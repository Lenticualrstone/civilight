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

    /** The stored (hand-set) reveal of a term, or null. (0.2.0) Readers of "the reveal" use [effective] / [effectiveInfo]. */
    fun of(
        doc: ProjectDoc,
        termKey: Key,
        codes: StoryCodes = StoryCodes(doc),
        isLive: (ChangeRecord) -> Boolean = { doc.liveness.isLive(it) },
    ): RevealInfo? {
        val record = record(doc, termKey, isLive) ?: return null
        return info(doc, termKey, record, codes)
    }

    /**
     * (0.2.0, WLD-72, D12) The effective reveal of a term: the stored `◇ 첫 공개` record when there is one
     * (it always wins), else the automatic one, the first `@` link of the term in the game-output
     * manuscript text in manuscript order (links in memos, directions, cards, pages, profiles and the
     * recorder never count; trashed blocks and scenes do not count; hidden scenes do). Null: `첫 공개 없음`.
     */
    fun effective(
        doc: ProjectDoc,
        termKey: Key,
        isLive: (ChangeRecord) -> Boolean = { doc.liveness.isLive(it) },
    ): EffectiveReveal? {
        record(doc, termKey, isLive)?.let { return EffectiveReveal(RevealKind.STORED, it.anchor) }
        return autoAnchor(doc, termKey)?.let { EffectiveReveal(RevealKind.AUTO, it) }
    }

    /** [effective] as the [RevealInfo] every list, card and check row reads; [RevealInfo.isAuto] tells the two apart. */
    fun effectiveInfo(
        doc: ProjectDoc,
        termKey: Key,
        codes: StoryCodes = StoryCodes(doc),
        isLive: (ChangeRecord) -> Boolean = { doc.liveness.isLive(it) },
    ): RevealInfo? = of(doc, termKey, codes, isLive) ?: autoAnchor(doc, termKey)?.let { autoInfo(doc, termKey, it, codes) }

    /** The automatic reveal's anchor: the first link of [termKey] in [TextSpots.script] order, or null. */
    fun autoAnchor(doc: ProjectDoc, termKey: Key): Anchor? {
        // A trashed term has no reveal (its stored record is not live either, WLD-14).
        if (doc.index.term(termKey)?.deletedAt != null) return null
        for (spot in TextSpots.script(doc)) {
            val at = spot.spot as? ScriptSpot ?: continue
            if (TermLinks.inSpot(spot).any { it.termKey == termKey }) return anchorOf(at)
        }
        return null
    }

    /** The anchor of a manuscript spot: its block (an option line counts as its choice block), or a scene's start. */
    fun anchorOf(spot: ScriptSpot): Anchor = when (spot) {
        is ScriptSpot.BlockText -> Anchor.AtBlock(spot.blockKey)
        is ScriptSpot.OptionText -> Anchor.AtBlock(spot.blockKey)
        is ScriptSpot.TransitionText -> Anchor.UnitStart(spot.sceneKey)
    }

    /**
     * The [RevealInfo] of an automatic reveal at [anchor]. Its record is synthetic: it is never written
     * (`changes.json` holds hand-set reveals only, scope §8), and its key starts with [AUTO_KEY_PREFIX].
     */
    fun autoInfo(doc: ProjectDoc, termKey: Key, anchor: Anchor, codes: StoryCodes = StoryCodes(doc)): RevealInfo =
        info(doc, termKey, ChangeRecord(key = AUTO_KEY_PREFIX + termKey, target = ChangeTarget.TermReveal(termKey), anchor = anchor), codes)

    /** Prefix of the synthetic record key of an automatic reveal. */
    const val AUTO_KEY_PREFIX: String = "auto-reveal:"

    private fun info(doc: ProjectDoc, termKey: Key, record: ChangeRecord, codes: StoryCodes): RevealInfo {
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
            // Stored `근거 지워짐`, or read-time: the anchor block is not live and reads at the next one.
            orphaned = record.anchorState.known == AnchorState.ORPHANED ||
                (record.anchor as? Anchor.AtBlock)?.let { !manuscript.isLiveBlock(it.blockKey) } == true,
            episodeLabel = episode,
            cardValue = cardValue,
        )
    }
}

/** (0.2.0, WLD-72) Where an effective reveal comes from: a stored (hand-set) record, or the first manuscript link. */
enum class RevealKind { STORED, AUTO }

/** (0.2.0, WLD-72) `TermReveals.effective`: the kind and the anchor. */
data class EffectiveReveal(val kind: RevealKind, val anchor: Anchor)

/** (0.2.0, WLD-72) True for an automatic reveal (derived, never stored). */
val RevealInfo.isAuto: Boolean get() = record.key.startsWith(TermReveals.AUTO_KEY_PREFIX)

/** Fingerprints of term checks (the `맞음` of `▲ 공개 전 사용` keys on the link and its term only, so moving the reveal keeps it, WLD-T41). */
object TermFingerprints {
    fun beforeReveal(termKey: Key, linkKey: Key): String = "term.beforeReveal:$termKey:$linkKey"
}
