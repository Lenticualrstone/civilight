package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.domain.story.time.OrderChecks
import io.github.lenticualrstone.civilight.domain.story.time.OrderNotice
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.domain.story.withUnits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.VoiceMode

/**
 * Preview of `씬 옮기기` (STO-08; sheet SCR-54). Nothing changes before [result] is taken.
 *
 * @property fromLine `1-7 · 씬 3 | 꺼진 등불`.
 * @property toLine `1-8 | 맨 앞 · 씬 1`.
 * @property recordsRow `◆ 3 · ◇ 1 · 시점 1-7 → 1-8`.
 * @property blocksRow `블록 13개 · 1-8 새 번호` or `그대로`.
 * @property voiceRow `고정 4개 그대로 · ◇ 에피소드 부분 다름` or `그대로`.
 * @property timeRow `그대로 · ◇ 1 순서와 어긋남` (this area's row).
 */
data class MovePreview(
    val title: String,
    val sub: String,
    val fromLine: String,
    val toLine: String,
    val recordsRow: String,
    val blocksRow: String,
    val voiceRow: String,
    val timeRow: String,
    val newOrderNotices: List<OrderNotice>,
    val result: ProjectDoc,
)

/** Moving scenes and reordering units (STO-08). */
object SceneMove {
    /**
     * Plans moving [sceneKey] under [parentKey] right after [afterSceneKey] (null = `맨 앞`). The scene
     * keeps its key, base + interval and absolute time; no other scene's 때 changes. Across numbering
     * units its blocks get new numbers from the destination counter in order (SCR-44): live blocks
     * first, then trashed ones; fixed voice IDs stay.
     */
    fun plan(doc: ProjectDoc, sceneKey: Key, parentKey: Key, afterSceneKey: Key?, ctx: EditContext): MovePreview {
        val codes = UnitCodes(doc)
        val scene = requireNotNull(doc.index.unit(sceneKey)) { "unknown scene $sceneKey" }
        val fromUnit = doc.index.numberingUnitKeyOfScene(sceneKey)
        val order = NewScene.orderAfter(doc, parentKey, afterSceneKey, excluding = sceneKey)
        var moved = doc.withUnits(mapOf(sceneKey to scene.copy(parentKey = parentKey, order = order)), ctx)
        val toUnit = moved.index.numberingUnitKeyOfScene(sceneKey)
        val blocks = doc.index.blocksOfScene(sceneKey).sortedWith(compareBy<Block> { it.deletedAt != null }.thenBy { it.order }.thenBy { it.key })
        val crosses = fromUnit != toUnit
        if (crosses && toUnit != null) moved = renumber(moved, blocks, toUnit, ctx)
        moved = moved.regroupBlocks()

        val liveBlockKeys = doc.manuscript.liveBlocksOf(sceneKey).map { it.key }.toSet()
        val records = doc.changes.records.filter { r ->
            doc.liveness.isLive(r) && when (val a = r.anchor) {
                is Anchor.AtBlock -> a.blockKey in liveBlockKeys
                is Anchor.UnitStart -> a.unitKey == sceneKey
                is Anchor.UnitEnd -> a.unitKey == sceneKey
                else -> false
            }
        }
        val changes = records.count { it.target !is ChangeTarget.TermReveal }
        val reveals = records.size - changes
        val fromCode = codes.episodeOf(scene)?.let(codes::episodeCode)
        val movedCodes = UnitCodes(moved)
        val movedScene = moved.index.unit(sceneKey)!!
        val toCode = movedCodes.episodeOf(movedScene)?.let(movedCodes::episodeCode)
        val point = if (fromCode != null && toCode != null && fromCode != toCode) " · 시점 $fromCode → $toCode" else ""
        val fixed = blocks.count { b -> b.deletedAt == null && (b.voice?.mode?.known == VoiceMode.FIXED || b.options.any { it.voice?.mode?.known == VoiceMode.FIXED }) }

        val notices = OrderChecks.newNotices(SceneTimes(doc), SceneTimes(moved))
        val siblingsAfter = moved.index.children(parentKey).filter { moved.liveness.isUnitLive(it.key) }
        val position = if (siblingsAfter.firstOrNull()?.key == sceneKey) "맨 앞" else
            siblingsAfter.getOrNull(siblingsAfter.indexOfFirst { it.key == sceneKey } - 1)?.let { "${movedCodes.sceneLabel(it)} 뒤" } ?: "맨 앞"
        val toEpisode = moved.index.unit(parentKey)?.let(movedCodes::unitLabel) ?: ""
        return MovePreview(
            title = "씬 옮기기",
            sub = "미리 보기 · 옮기기 전에는 그대로",
            fromLine = "${listOfNotNull(fromCode, codes.sceneLabel(scene)).joinToString(" · ")} | ${scene.title ?: "미정"}",
            toLine = "$toEpisode | $position · ${movedCodes.sceneLabel(movedScene)}",
            recordsRow = "◆ $changes · ◇ $reveals$point",
            blocksRow = if (crosses) "블록 ${liveBlockKeys.size}개 · ${toCode ?: toEpisode} 새 번호" else "그대로",
            voiceRow = if (crosses && fixed > 0) "고정 ${fixed}개 그대로 · ◇ 에피소드 부분 다름" else "그대로",
            timeRow = "그대로" + if (notices.isEmpty()) "" else " · ◇ ${notices.size} 순서와 어긋남",
            newOrderNotices = notices,
            result = moved,
        )
    }

    /**
     * Reorders an episode, chapter or any non-scene unit (STO-08): codes renumber, no 때 changes, no
     * preview (`ED`, Q6). Scenes moved to another numbering unit go through [plan].
     */
    fun reorderUnit(doc: ProjectDoc, unitKey: Key, parentKey: Key?, afterKey: Key?, ctx: EditContext): ProjectDoc {
        val unit = requireNotNull(doc.index.unit(unitKey)) { "unknown unit $unitKey" }
        val order = NewScene.orderAfter(doc, parentKey, afterKey, excluding = unitKey)
        return doc.withUnits(mapOf(unitKey to unit.copy(parentKey = parentKey, order = order)), ctx).regroupBlocks()
    }

    /** New numbers from the destination counter (it only grows). */
    private fun renumber(doc: ProjectDoc, blocks: List<Block>, unitKey: Key, ctx: EditContext): ProjectDoc {
        if (blocks.isEmpty()) return doc
        val counterOwner = doc.index.unit(unitKey)
        val start = counterOwner?.nextBlockNumber ?: doc.project.counters.nextBlockNumber ?: 1
        val numbers = blocks.withIndex().associate { (i, b) -> b.key to start + i }
        val episodes = doc.episodes.mapValues { (_, file) ->
            file.copy(blocks = file.blocks.map { b -> numbers[b.key]?.let { b.copy(number = it, updatedAt = ctx.now, updatedBy = ctx.device) } ?: b })
        }
        val next = start + blocks.size
        return if (counterOwner != null) {
            doc.copy(episodes = episodes).withUnits(mapOf(unitKey to counterOwner.copy(nextBlockNumber = next)), ctx)
        } else {
            doc.copy(episodes = episodes, project = doc.project.copy(counters = doc.project.counters.copy(nextBlockNumber = next)))
        }
    }
}
