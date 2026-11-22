package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** `씬 나누기` (SCR-50): preview sheet and apply. */
object SceneSplit {
    /** One part card: `씬 3` · `블록 9개` · `‘세라 · 불은 꺼졌어…’ ~ ‘…한 번 깜박였다.’`. */
    data class PartCard(val title: String, val count: String, val range: String)

    /** Where a silent cast member goes: `씬 3 | 새 씬 4 | 둘 다`. */
    enum class Side { A, B, BOTH }

    data class SilentRow(val characterKey: Key, val label: String, val chips: List<String>, val selected: Side)

    data class Preview(
        val title: String,
        val sub: String,
        val partA: PartCard,
        val partB: PartCard,
        val placeRow: String,
        val timeRow: String,
        val titleRow: String,
        val silentRows: List<SilentRow>,
        /** `회상` toggle row (on) when A is a flashback. */
        val flashbackRow: String?,
        /** `숨김` toggle row (on) when A is hidden. */
        val hiddenRow: String?,
        val changes: List<String>,
    ) {
        val primary: String get() = "나누기"
        val close: String get() = "닫기"
    }

    /** The author's choices for the new scene B (defaults = the preview's prefills). */
    data class Choices(
        val place: PlaceRef? = null,
        val keepPlace: Boolean = true,
        val keepTime: Boolean = true,
        val title: String? = null,
        val silentCast: Map<Key, Side> = emptyMap(),
        val flashback: Boolean = true,
        val hidden: Boolean = true,
    )

    /**
     * Excerpt rule (T-22-09 `[PROPOSED]`): parentheticals removed; the leading excerpt is the longest
     * run of whole words from the start whose length (with spaces) is ≤ 10, trailing `.,?!…⋯`
     * removed, then `…`; dialogue gets `{speaker} · ` in front.
     */
    fun leadingExcerpt(doc: ProjectDoc, block: Block): String {
        val words = textForExcerpt(doc, block).split(' ').filter { it.isNotEmpty() }
        var taken = ""
        for (w in words) {
            val next = if (taken.isEmpty()) w else "$taken $w"
            if (next.length > 10) break
            taken = next
        }
        if (taken.isEmpty()) taken = words.firstOrNull()?.take(10).orEmpty()
        val body = taken.trimEnd('.', ',', '?', '!', '…', '⋯') + "…"
        val speaker = block.speakerKey?.takeIf { block.kind.known == BlockKind.DIALOGUE }?.let { Labels.speakerName(doc, it) }
        return if (speaker != null) "$speaker · $body" else body
    }

    /** Trailing excerpt: `…` + the longest run of whole words at the end with length ≤ 10. */
    fun trailingExcerpt(doc: ProjectDoc, block: Block): String {
        val words = textForExcerpt(doc, block).split(' ').filter { it.isNotEmpty() }
        var taken = ""
        for (w in words.asReversed()) {
            val next = if (taken.isEmpty()) w else "$w $taken"
            if (next.length > 10) break
            taken = next
        }
        if (taken.isEmpty()) taken = words.lastOrNull()?.takeLast(10).orEmpty()
        return "…$taken"
    }

    private fun textForExcerpt(doc: ProjectDoc, block: Block): String = when (block.kind.known) {
        BlockKind.DIRECTION -> Manuscript.cueText(doc, block, " ")
        BlockKind.CHOICE -> BlockOps.orderedOptions(block).joinToString(" ") { Runs.withoutParens(it.runs) }
        else -> Runs.withoutParens(block.runs)
    }

    private fun card(doc: ProjectDoc, title: String, blocks: List<Block>): PartCard = PartCard(
        title = title,
        count = "블록 ${blocks.size}개",
        range = if (blocks.isEmpty()) "" else "‘${leadingExcerpt(doc, blocks.first())}’ ~ ‘${trailingExcerpt(doc, blocks.last())}’",
    )

    /** Preview of a split below [blockKey]; null when the block is not live. */
    fun preview(doc: ProjectDoc, blockKey: Key, time: SceneTimeSource): Preview? {
        val block = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return null
        val scene = doc.index.unit(block.sceneKey) ?: return null
        val n = Labels.sceneOrdinal(doc, scene.key) ?: return null
        val code = Labels.episodeCodeOfScene(doc, scene.key)
        val all = doc.manuscript.liveBlocksOf(scene.key)
        val cut = all.indexOfFirst { it.key == blockKey } + 1
        val partA = all.subList(0, cut)
        val partB = all.subList(cut, all.size)
        val placeName = SceneText.placeName(doc, scene.place)
        val changes = mutableListOf<String>()
        val unit = doc.numberingUnitOfScene(scene.key)
        val following = doc.manuscript.liveScenes.dropWhile { it.key != scene.key }.drop(1)
            .filter { doc.numberingUnitOfScene(it.key) == unit }
        for (s in following) {
            val k = Labels.sceneOrdinal(doc, s.key)!!
            changes += "순번 · 씬 $k · ${Labels.titleOrUndecided(s)} → 씬 ${k + 1}"
        }
        val bKeys = partB.map { it.key }.toSet()
        for (b in all) {
            for (r in doc.recordsAnchoredOn(b.key).filter { doc.liveness.isLive(it) }) {
                changes += recordRow(doc, r, b, code, n, moves = b.key in bKeys)
            }
        }
        for (s in following) {
            val t = s.time as? SceneTime.Relative ?: continue
            if (t.baseKey != scene.key) continue
            changes += "씬 ${Labels.sceneOrdinal(doc, s.key)!! + 1} 때 · 기준 씬 $n · ${SceneText.headTime(time.timeOf(s.key))} 그대로"
        }
        return Preview(
            title = "씬 나누기",
            sub = "${code} 씬 $n · ${Labels.number(block.number)} 아래".trim(),
            partA = card(doc, "씬 $n", partA),
            partB = card(doc, "새 씬 ${n + 1}", partB),
            placeRow = placeName?.let { "장소 · $it · 씬 $n${Labels.and(n)} 같음" } ?: "장소 · 장소 없음",
            timeRow = if (TimeChains.isTimed(scene.time)) "때 · 씬 $n 기준 · 같은 때" else "때 · 때 없음",
            titleRow = "제목 · 미정",
            silentRows = scene.silentCast.map { c ->
                SilentRow(c, "말 없는 등장 · ${Labels.speakerName(doc, c) ?: "?"}", listOf("씬 $n", "새 씬 ${n + 1}", "둘 다"), Side.BOTH)
            },
            flashbackRow = if (scene.time is SceneTime.Flashback) "회상" else null,
            hiddenRow = if (scene.hidden) "숨김" else null,
            changes = changes,
        )
    }

    private fun recordRow(doc: ProjectDoc, r: ChangeRecord, block: Block, code: String, n: Int, moves: Boolean): String {
        val number = Labels.number(block.number)
        return if (moves) {
            val head = if (r.isReveal) "첫 공개" else "◆ ${Records.kindLabel(doc, r)}"
            "$head · $code 씬 $n $number → $code 씬 ${n + 1} $number"
        } else if (r.isReveal) {
            "첫 공개 · ${Records.subject(doc, r)} · 씬 $n $number 그대로"
        } else {
            "◆ ${Records.kindLabel(doc, r)} · ${Records.subject(doc, r)} · $number 그대로"
        }
    }

    data class Result(val doc: ProjectDoc, val newSceneKey: Key)

    /**
     * Applies the split: blocks after [blockKey] move to the new scene B right after A (keys, numbers,
     * voice IDs and records unchanged). B: `A 기준 · 같은 때` (none when A has none; a flashback is
     * copied), transition `없음`, no title, flashback and hidden inherited. Scenes based on A keep base A.
     */
    fun apply(doc: ProjectDoc, blockKey: Key, ctx: EditContext, choices: Choices = Choices()): Result? {
        val block = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return null
        val a = doc.index.unit(block.sceneKey) ?: return null
        val all = doc.index.blocksOfScene(a.key)
        val moving = all.filter { it.order > block.order || (it.order == block.order && it.key > block.key) }
        val siblings = doc.index.children(a.parentKey)
        val next = siblings.getOrNull(siblings.indexOfFirst { it.key == a.key } + 1)
        val time = when {
            !choices.keepTime -> SceneTime.None
            a.time is SceneTime.Flashback -> if (choices.flashback) a.time else SceneTime.None
            else -> TimeChains.sameTimeAs(doc, a.key)
        }
        val side = { c: Key -> choices.silentCast[c] ?: Side.BOTH }
        val b = StoryUnit(
            key = ctx.newKey(), parentKey = a.parentKey, level = a.level, order = OrderKeys.between(a.order, next?.order),
            title = choices.title, hidden = a.hidden && choices.hidden,
            place = if (choices.keepPlace) choices.place ?: a.place else choices.place,
            time = time, silentCast = a.silentCast.filter { side(it) != Side.A },
            transitionMode = TransitionMode.NONE.open(), updatedAt = ctx.now, updatedBy = ctx.device,
        )
        val newA = a.copy(silentCast = a.silentCast.filter { side(it) != Side.B }, updatedAt = ctx.now, updatedBy = ctx.device)
        val out = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == a.key) newA else it } + b))
            .replaceBlocks(moving.associate { it.key to it.copy(sceneKey = b.key, updatedAt = ctx.now, updatedBy = ctx.device) })
            .mapRecords { r ->
                // A scene-end point of A now sits after B's blocks: it follows them to B.
                if ((r.anchor as? Anchor.UnitEnd)?.unitKey == a.key) r.copy(anchor = Anchor.UnitEnd(b.key)) else r
            }
        return Result(out, b.key)
    }
}
