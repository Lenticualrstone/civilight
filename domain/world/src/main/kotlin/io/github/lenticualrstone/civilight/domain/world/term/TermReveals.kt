package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.Rank

/**
 * A term's `◇ 첫 공개` as read now (WLD-18). [blockKey] is the block the reveal resolves to (the
 * anchor block, the first block of a point, or where a deleted anchor reads); [episodeLabel] is the
 * `1-7` / `2-5` / `2장` the list and markers print.
 */
data class RevealInfo(
    val termKey: Key,
    val record: ChangeRecord,
    val rank: Rank,
    val blockKey: Key?,
    /** Beyond the 집필 선 (`공개 예정`). */
    val planned: Boolean,
    /** `근거 지워짐`: the anchor moved because its block was deleted. */
    val orphaned: Boolean,
    val episodeLabel: String?,
    /** The card's `첫 공개` value: `1-7 씬 3 · #044`, `공개 예정 2-5`. */
    val cardValue: String,
) {
    /** The list row's first words: `첫 공개 1-7` or `공개 예정 2-5`. */
    val listLabel: String get() = if (planned) "공개 예정 ${episodeLabel.orEmpty()}" else "첫 공개 ${episodeLabel.orEmpty()}"
}

/** Reading `termReveal` records. */
object TermReveals {
    /** The live reveal record of a term (the earliest if the invariant is broken), with [isLive] deciding liveness. */
    fun record(doc: ProjectDoc, termKey: Key, isLive: (ChangeRecord) -> Boolean = { doc.liveness.isLive(it) }): ChangeRecord? {
        val target = ChangeTarget.TermReveal(termKey)
        return doc.index.recordsOf(target).filter(isLive)
            .minWithOrNull(compareBy<ChangeRecord> { doc.manuscript.rankOf(it.anchor) }.thenBy { it.createdAt }.thenBy { it.key })
    }

    /** The reveal of a term, or null (`첫 공개 없음`). */
    fun of(
        doc: ProjectDoc,
        termKey: Key,
        codes: StoryCodes = StoryCodes(doc),
        isLive: (ChangeRecord) -> Boolean = { doc.liveness.isLive(it) },
    ): RevealInfo? {
        val record = record(doc, termKey, isLive) ?: return null
        val manuscript = doc.manuscript
        val rank = manuscript.rankOf(record.anchor)
        val block = if (rank.isUnplaced) null else manuscript.firstLiveBlockAfter(Rank(rank.value - 1))
        val planned = manuscript.isPlanned(record.anchor)
        val episode = when (val anchor = record.anchor) {
            is Anchor.UnitStart -> codes.episodeOrChapterLabel(anchor.unitKey)
            is Anchor.UnitEnd -> codes.episodeOrChapterLabel(anchor.unitKey)
            else -> block?.let { codes.episodeCode(it.sceneKey) }
        }
        val cardValue = when {
            planned -> "공개 예정 ${episode.orEmpty()}"
            record.anchor is Anchor.AtBlock && block != null ->
                "${codes.sceneLabel(block.sceneKey).orEmpty()}${Copy.SEP}${StoryCodes.blockNumber(block.number)}"
            else -> (record.anchor as? Anchor.UnitStart)?.let { codes.unitLabel(it.unitKey) } ?: episode.orEmpty()
        }
        return RevealInfo(
            termKey = termKey,
            record = record,
            rank = rank,
            blockKey = block?.key,
            planned = planned,
            orphaned = record.anchorState.known == AnchorState.ORPHANED,
            episodeLabel = episode,
            cardValue = cardValue,
        )
    }
}

/** Fingerprints of term checks (the `맞음` of `▲ 공개 전 사용` keys on the link and its term only, so moving the reveal keeps it, WLD-T41). */
object TermFingerprints {
    fun beforeReveal(termKey: Key, linkKey: Key): String = "term.beforeReveal:$termKey:$linkKey"
}
