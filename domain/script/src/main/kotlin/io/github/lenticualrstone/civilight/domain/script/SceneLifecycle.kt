package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind

/** `씬 지우기` (SCR-52), restoring a scene, and moving a scene with its preview (SCR-54). */
object SceneLifecycle {
    data class Deleted(val doc: ProjectDoc, val relocations: List<Anchors.Relocation>, val toast: String)

    /**
     * Deletes a scene (no confirmation): the scene goes to the trash (filter `씬`, 30 days) with its
     * blocks; its records move to the first block of the next scene (then next episode, then
     * `예정`), flagged `근거 지워짐`; scenes based on it switch to its base with the interval rewritten
     * so their absolute 때 stays (STO-10, `rebasedFromKey` remembered). Toast ordinals are the ones
     * before the delete.
     */
    fun delete(doc: ProjectDoc, sceneKey: Key, ctx: EditContext): Deleted? {
        val scene = doc.index.unit(sceneKey)?.takeIf { doc.manuscript.isLiveUnit(it.key) } ?: return null
        val ordinal = Labels.sceneOrdinal(doc, sceneKey) ?: return null
        val entry = TrashEntry(
            key = ctx.newKey(), kind = TrashKind.SCENE.open(), projectKey = doc.key, targetKeys = listOf(sceneKey),
            hiddenAtDelete = scene.hidden, deletedAt = ctx.now, purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        var out = doc.updateUnit(sceneKey) { it.copy(deletedAt = ctx.now, trashEntryKey = entry.key, updatedAt = ctx.now, updatedBy = ctx.device) }
            .upsertTrashEntries(listOf(entry))
        out = out.updateUnits(rebaseDependents(doc, sceneKey, ctx))
        val (relocated, moves) = Anchors.relocateOrphans(out, ctx)
        val target = moves.firstOrNull()?.to as? Anchor.AtBlock
        val next = target?.let { relocated.index.block(it.blockKey) }?.let { blk ->
            if (doc.numberingUnitOfScene(blk.sceneKey) == doc.numberingUnitOfScene(sceneKey)) "씬 ${Labels.sceneOrdinal(doc, blk.sceneKey)}"
            else Labels.sceneLocation(doc, blk.sceneKey)
        }
        val toast = Toasts.sceneDeleted(ordinal, moves.count { !it.isReveal }, moves.count { it.isReveal }, next)
        return Deleted(relocated, moves, toast)
    }

    private fun rebaseDependents(doc: ProjectDoc, sceneKey: Key, ctx: EditContext) =
        SceneMerge.dependentsOf(doc, sceneKey).associate { s ->
            val t = s.time as SceneTime.Relative
            val newTime = when (val own = doc.index.unit(sceneKey)?.time) {
                is SceneTime.Relative -> TimeChains.rebased(t, own.baseKey, TimeChains.offsetOf(own), sceneKey)
                is SceneTime.Direct -> own.storyDay?.let { d -> SceneTime.Direct(storyDay = d + t.dayMin, band = t.band, clock = t.clock) } ?: t
                else -> t
            }
            s.key to s.copy(time = newTime, updatedAt = ctx.now, updatedBy = ctx.device)
        }

    /**
     * Restores a trashed scene with its blocks: records still `orphaned` towards it re-attach, and
     * scenes rebased away from it return to it with their absolute 때 kept.
     */
    fun restore(doc: ProjectDoc, sceneKey: Key, ctx: EditContext): ProjectDoc {
        val scene = doc.index.unit(sceneKey) ?: return doc
        if (scene.deletedAt == null) return doc
        var out = doc.updateUnit(sceneKey) { it.copy(deletedAt = null, trashEntryKey = null, updatedAt = ctx.now, updatedBy = ctx.device) }
            .removeFromTrash(sceneKey)
        val own = scene.time as? SceneTime.Relative
        val back = out.story.units.filter { (it.time as? SceneTime.Relative)?.rebasedFromKey == sceneKey }.associate { s ->
            val t = s.time as SceneTime.Relative
            val time = if (own != null && t.baseKey == own.baseKey) {
                TimeChains.rebased(t, sceneKey, TimeChains.Offset.ZERO - TimeChains.offsetOf(own), null)
            } else t.copy(rebasedFromKey = null)
            s.key to s.copy(time = time, updatedAt = ctx.now, updatedBy = ctx.device)
        }
        out = out.updateUnits(back)
        return Anchors.reattachOrphans(out, ctx)
    }

    // --- Scene move (SCR-54) ---

    data class MovePreview(
        val title: String,
        val sub: String,
        /** `1-7 · 씬 3` / `꺼진 등불`. */
        val from: Pair<String, String>,
        /** `1-8` / `맨 앞 · 씬 1`. */
        val to: Pair<String, String>,
        val rows: List<String>,
    ) {
        val buttons: List<String> get() = listOf("취소", "옮기기")
    }

    /**
     * Preview of moving a scene under [destParentKey] after [afterSiblingKey] (null = `맨 앞`).
     * [timeOrderMismatches] comes from the place & time area (scenes whose time order no longer
     * matches the manuscript order after the move).
     */
    fun movePreview(doc: ProjectDoc, sceneKey: Key, destParentKey: Key, afterSiblingKey: Key?, timeOrderMismatches: Int): MovePreview? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val code = Labels.episodeCodeOfScene(doc, sceneKey)
        val destCode = Labels.unitCode(doc, destParentKey)
        val destScenes = doc.index.children(destParentKey).filter { doc.manuscript.isLiveUnit(it.key) && it.key != sceneKey }
        val newOrdinal = if (afterSiblingKey == null) 1 else destScenes.indexOfFirst { it.key == afterSiblingKey } + 2
        val position = if (afterSiblingKey == null) "맨 앞" else "씬 ${newOrdinal - 1} 뒤"
        val blocks = doc.manuscript.liveBlocksOf(sceneKey)
        val records = blocks.flatMap { doc.recordsAnchoredOn(it.key) }.filter { doc.liveness.isLive(it) }
        val sameUnit = doc.numberingUnitOfScene(sceneKey) == destParentKey
        val fixed = blocks.sumOf { b ->
            (if (b.voice?.mode?.known == VoiceMode.FIXED) 1 else 0) + b.options.count { it.voice?.mode?.known == VoiceMode.FIXED }
        }
        val rows = mutableListOf<String>()
        rows += "기록 · ◆ ${records.count { !it.isReveal }} · ◇ ${records.count { it.isReveal }} · 시점 " +
            if (sameUnit) "씬 ${Labels.sceneOrdinal(doc, sceneKey)} → 씬 $newOrdinal" else "$code → $destCode"
        rows += if (sameUnit) "블록 번호 · 그대로" else "블록 번호 · 블록 ${blocks.size}개 · $destCode 새 번호"
        rows += if (sameUnit) "보이스 ID · 그대로" else "보이스 ID · 고정 ${fixed}개 그대로 · ◇ 에피소드 부분 다름"
        rows += if (timeOrderMismatches > 0) "때 · 그대로 · ◇ $timeOrderMismatches 순서와 어긋남" else "때 · 그대로"
        return MovePreview(
            title = "씬 옮기기",
            sub = "미리 보기 · 옮기기 전에는 그대로",
            from = "$code · 씬 ${Labels.sceneOrdinal(doc, sceneKey)}" to Labels.titleOrUndecided(scene),
            to = destCode to "$position · 씬 $newOrdinal",
            rows = rows,
        )
    }

    /**
     * The same preview with `◇ {o} 순서와 어긋남` computed by the place & time engine on a simulated
     * move (notices that the move would add). Nothing is changed: the simulated doc is dropped.
     */
    fun movePreview(doc: ProjectDoc, sceneKey: Key, destParentKey: Key, afterSiblingKey: Key?, ctx: EditContext): MovePreview? {
        val after = move(doc, sceneKey, destParentKey, afterSiblingKey, ctx)
        return movePreview(doc, sceneKey, destParentKey, afterSiblingKey, StoryTimes.newOrderMismatches(doc, after))
    }

    /**
     * Moves a scene (`옮기기`): the scene keeps its key and its 때; within the episode numbers stay;
     * into another numbering unit every block of the scene gets the destination's next numbers
     * (live blocks in manuscript order, then trashed ones), automatic IDs follow, fixed IDs stay.
     */
    fun move(doc: ProjectDoc, sceneKey: Key, destParentKey: Key, afterSiblingKey: Key?, ctx: EditContext): ProjectDoc {
        val scene = doc.index.unit(sceneKey) ?: return doc
        val siblings = doc.index.children(destParentKey).filter { it.key != sceneKey }
        val i = if (afterSiblingKey == null) -1 else siblings.indexOfFirst { it.key == afterSiblingKey }
        val order = OrderKeys.between(siblings.getOrNull(i)?.order, siblings.getOrNull(i + 1)?.order)
        val fromUnit = doc.numberingUnitOfScene(sceneKey)
        var out = doc.updateUnit(sceneKey) { it.copy(parentKey = destParentKey, order = order, updatedAt = ctx.now, updatedBy = ctx.device) }
        val toUnit = out.numberingUnitOfScene(sceneKey)
        if (fromUnit == toUnit) return out
        val live = doc.manuscript.liveBlocksOf(sceneKey)
        val all = live + doc.index.blocksOfScene(sceneKey).filter { b -> live.none { it.key == b.key } }
        val (numbered, numbers) = Numbering.issue(out, toUnit, all.size)
        out = numbered.replaceBlocks(all.zip(numbers).associate { (b, n) -> b.key to b.copy(number = n, updatedAt = ctx.now, updatedBy = ctx.device) })
        return out.regroupBlocks()
    }

    /** True when the unit is at the scene level (for callers validating a destination). */
    fun isScene(doc: ProjectDoc, key: Key): Boolean = doc.index.unit(key)?.let { doc.index.isScene(it) } == true
}
