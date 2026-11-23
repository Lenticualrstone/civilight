package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.domain.world.common.DocRuns
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.Term

/** Where a `◇ 첫 공개` may be set (WLD-18). */
sealed interface RevealTarget {
    /** `X부터`, the block inclusive. */
    data class Block(val blockKey: Key) : RevealTarget

    /** A choice option line: always refused. */
    data class OptionLine(val blockKey: Key, val optionKey: Key) : RevealTarget

    /** From the start of a chapter, episode or scene (`공개 예정 2-5`). */
    data class Unit(val unitKey: Key) : RevealTarget
}

/** Result of setting a reveal. */
sealed interface RevealEdit {
    data class Applied(val doc: ProjectDoc, val record: ChangeRecord) : RevealEdit
    data class Refused(val reason: String) : RevealEdit
}

/** Result of editing aliases (WLD-16). */
sealed interface AliasEdit {
    data class Applied(val doc: ProjectDoc) : AliasEdit
    data class Invalid(val alias: String, val problem: AliasProblem) : AliasEdit
}

enum class AliasProblem { EMPTY, SAME_AS_NAME, DUPLICATE }

/** Links made by one `잇기` / `모두 잇기` (one undo step: keep the doc from before). */
data class Linked(val doc: ProjectDoc, val linkKeys: List<Key>)

/**
 * Pure rule-21 edits (WLD-16 … WLD-25). The app never creates links by itself: links come only from
 * these explicit author actions.
 */
object TermEdits {
    const val OPTION_REVEAL_REFUSED = "선택지 보기 줄에는 첫 공개를 걸 수 없음"

    private fun touchTerm(doc: ProjectDoc, termKey: Key, ctx: EditContext, f: (Term) -> Term): ProjectDoc =
        doc.copy(world = doc.world.copy(terms = doc.world.terms.map {
            if (it.key == termKey) f(it).copy(updatedAt = ctx.now(), updatedBy = ctx.device) else it
        }))

    /** Rewrites the runs of one game-output spot; null when the container is gone or [f] refuses. */
    fun updateSpotRuns(doc: ProjectDoc, spot: ScriptSpot, ctx: EditContext, f: (List<TextRun>) -> List<TextRun>?): ProjectDoc? {
        val now = ctx.now()
        return when (spot) {
            is ScriptSpot.BlockText -> {
                val block = doc.index.block(spot.blockKey) ?: return null
                val runs = f(block.runs) ?: return null
                DocRuns.updateBlock(doc, spot.blockKey) { it.copy(runs = runs, updatedAt = now, updatedBy = ctx.device) }
            }
            is ScriptSpot.OptionText -> {
                val block = doc.index.block(spot.blockKey) ?: return null
                val option = block.options.firstOrNull { it.key == spot.optionKey } ?: return null
                val runs = f(option.runs) ?: return null
                DocRuns.updateBlock(doc, spot.blockKey) { b ->
                    b.copy(
                        options = b.options.map { if (it.key == spot.optionKey) it.copy(runs = runs) else it },
                        updatedAt = now, updatedBy = ctx.device,
                    )
                }
            }
            is ScriptSpot.TransitionText -> {
                val scene = doc.index.unit(spot.sceneKey) ?: return null
                val runs = f(scene.transitionRuns ?: return null) ?: return null
                DocRuns.updateUnit(doc, spot.sceneKey) { it.copy(transitionRuns = runs, updatedAt = now, updatedBy = ctx.device) }
            }
        }
    }

    // --- 잇기 · 아님 -------------------------------------------------------------------------------

    /**
     * `잇기`: wraps exactly the candidate span in a new link run to the term (even when the span is
     * not the name, e.g. `오리지`). Refused (null) when the text no longer holds the candidate.
     */
    fun link(doc: ProjectDoc, candidate: TermCandidate, ctx: EditContext): Linked? = linkAll(doc, listOf(candidate), ctx)

    /** `모두 잇기 · 넣기`: all [candidates] in one transaction; null if any of them is stale. */
    fun linkAll(doc: ProjectDoc, candidates: List<TermCandidate>, ctx: EditContext): Linked? {
        var current = doc
        val keys = ArrayList<Key>()
        // Several spans in one container: apply from the last offset backwards so offsets stay valid.
        for ((spot, list) in candidates.groupBy { it.spot }) {
            for (c in list.sortedByDescending { it.start }) {
                val key = ctx.newKey()
                val link = TextRun.Link(key, LinkTarget(LinkTargetKind.TERM, c.termKey), c.matchedText)
                current = updateSpotRuns(current, spot, ctx) { runs ->
                    val text = TextSpots.textOf(runs)
                    if (c.end > text.length || text.substring(c.start, c.end).normalizeNfc() != c.matchedText) null
                    else RunEdits.wrap(runs, c.start, c.end, link)
                } ?: return null
                keys += key
            }
        }
        return Linked(current, keys)
    }

    private fun String.normalizeNfc(): String = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFC)

    /** `아님`: stores a rejection; the candidate disappears everywhere. */
    fun reject(doc: ProjectDoc, candidate: TermCandidate, ctx: EditContext): ProjectDoc = rejectAll(doc, listOf(candidate), ctx)

    /** `모두 아님 · 넣기`. Candidates already rejected are skipped. */
    fun rejectAll(doc: ProjectDoc, candidates: List<TermCandidate>, ctx: EditContext): ProjectDoc {
        var current = doc
        for ((termKey, list) in candidates.groupBy { it.termKey }) {
            val now = ctx.now()
            current = touchTerm(current, termKey, ctx) { term ->
                val added = list.filterNot { c -> term.rejections.any { c.isRejectedBy(it) } }
                    .map { TermRejections.of(it, ctx.newKey(), now) }
                term.copy(rejections = term.rejections + added)
            }
        }
        return current
    }

    /** `아님으로 둔 것 › 되돌리기`. */
    fun restoreRejection(doc: ProjectDoc, termKey: Key, rejectionKey: Key, ctx: EditContext): ProjectDoc =
        touchTerm(doc, termKey, ctx) { t -> t.copy(rejections = t.rejections.filterNot { it.key == rejectionKey }) }

    /**
     * Releases `아님` records whose candidate is gone (WLD-22 lifetime): for searchable terms, a
     * rejection in a live container of [spots] (all when null) is deleted when [index] (fresh for
     * those spots) no longer holds the same `(term, matchedText, occurrence)`; a rejection whose
     * container no longer exists is deleted too. Trashed containers and terms with `글자 찾기` off
     * keep theirs.
     */
    fun releaseStaleRejections(
        doc: ProjectDoc,
        index: CandidateIndex,
        access: TermAccess = TermAccess(),
        spots: Set<ScriptSpot>? = null,
    ): ProjectDoc {
        // (0.2.0, WLD-73) While `용어 후보` is off, stored `아님` records are kept as they are.
        if (!doc.project.termCandidates) return doc
        var changed = false
        val terms = doc.world.terms.map { term ->
            if (!access.canSearch(term) || term.rejections.isEmpty()) return@map term
            val kept = term.rejections.filter { r ->
                val spot = TermRejections.spotOf(r) ?: return@filter true
                if (!TextSpots.exists(doc, spot)) return@filter false
                if (!TextSpots.isLive(doc, spot)) return@filter true
                if (spots != null && spot !in spots) return@filter true
                index.at(spot).any { it.termKey == term.key && it.isRejectedBy(r) }
            }
            if (kept.size == term.rejections.size) term else term.copy(rejections = kept).also { changed = true }
        }
        return if (changed) doc.copy(world = doc.world.copy(terms = terms)) else doc
    }

    // --- 글자 찾기 · 별칭 · 이름 ---------------------------------------------------------------------

    /** `글자 찾기` on/off; `아님` records are kept (WLD-25). */
    fun setTextSearch(doc: ProjectDoc, termKey: Key, on: Boolean, ctx: EditContext): ProjectDoc =
        touchTerm(doc, termKey, ctx) { it.copy(textSearch = on) }

    /**
     * Replaces the aliases (WLD-16): each NFC, trimmed, non-empty, different from the name and from
     * the others (Latin case-insensitive). Existing text is never converted to links.
     */
    fun setAliases(doc: ProjectDoc, termKey: Key, aliases: List<String>, ctx: EditContext): AliasEdit {
        val term = doc.index.term(termKey) ?: return AliasEdit.Applied(doc)
        val name = TermText.fold(TermText.clean(term.name))
        val seen = HashSet<String>()
        val cleaned = ArrayList<String>()
        for (raw in aliases) {
            val alias = TermText.clean(raw)
            val folded = TermText.fold(alias)
            when {
                alias.isEmpty() -> return AliasEdit.Invalid(raw, AliasProblem.EMPTY)
                folded == name -> return AliasEdit.Invalid(raw, AliasProblem.SAME_AS_NAME)
                !seen.add(folded) -> return AliasEdit.Invalid(raw, AliasProblem.DUPLICATE)
            }
            cleaned += alias
        }
        return AliasEdit.Applied(touchTerm(doc, termKey, ctx) { it.copy(aliases = cleaned) })
    }

    /** Renames a term (NFC, trimmed, non-empty); link runs keep their written text. Null when the name is empty. */
    fun rename(doc: ProjectDoc, termKey: Key, name: String, ctx: EditContext): ProjectDoc? {
        val clean = TermText.clean(name)
        if (clean.isEmpty()) return null
        return touchTerm(doc, termKey, ctx) { it.copy(name = clean) }
    }

    /** `후보 N 새로 찾음`: candidates of [termKey] in [after] that [before] did not have. */
    fun newlyFound(before: CandidateIndex, after: CandidateIndex, termKey: Key): Int {
        fun id(c: TermCandidate) = Triple(c.spot, c.matchedText, c.occurrence)
        val old = before.of(termKey).map(::id).toSet()
        return after.of(termKey).count { id(it) !in old }
    }

    // --- 첫 공개 ---------------------------------------------------------------------------------

    /**
     * Sets or moves the term's `◇ 첫 공개` (one live record per term). An option line is refused
     * (`선택지 보기 줄에는 첫 공개를 걸 수 없음`); a whole choice block is allowed (Q-06 default).
     */
    fun setReveal(doc: ProjectDoc, termKey: Key, target: RevealTarget, ctx: EditContext): RevealEdit {
        val anchor = when (target) {
            is RevealTarget.OptionLine -> return RevealEdit.Refused(OPTION_REVEAL_REFUSED)
            is RevealTarget.Block -> Anchor.AtBlock(target.blockKey)
            is RevealTarget.Unit -> Anchor.UnitStart(target.unitKey)
        }
        val now = ctx.now()
        val existing = TermReveals.record(doc, termKey)
        val record = existing?.copy(
            anchor = anchor, anchorState = AnchorState.NORMAL.open(), originalAnchor = null, updatedAt = now, updatedBy = ctx.device,
        ) ?: ChangeRecord(
            key = ctx.newKey(), target = ChangeTarget.TermReveal(termKey), value = null, anchor = anchor,
            createdAt = now, updatedAt = now, updatedBy = ctx.device,
        )
        val records = if (existing == null) doc.changes.records + record
        else doc.changes.records.map { if (it.key == existing.key) record else it }
        return RevealEdit.Applied(doc.copy(changes = doc.changes.copy(records = records)), record)
    }

    // --- 맞음 · 연결 끊기 --------------------------------------------------------------------------

    /** `맞음` on a `▲ 공개 전 사용` link (WLD-24): an acknowledgement keyed by the link. */
    fun markIntended(doc: ProjectDoc, link: TermLink, ctx: EditContext): ProjectDoc {
        val fingerprint = TermFingerprints.beforeReveal(link.termKey, link.linkKey)
        if (doc.checks.acknowledgements.any { it.checkType == CheckType.TERM_BEFORE_REVEAL && it.subjectKeys == listOf(link.linkKey) && it.fingerprint == fingerprint }) {
            return doc
        }
        val now = ctx.now()
        val ack = Acknowledgement(
            key = ctx.newKey(), checkType = CheckType.TERM_BEFORE_REVEAL, subjectKeys = listOf(link.linkKey),
            fingerprint = fingerprint, kind = AckKind.MATCH.open(), createdAt = now, updatedAt = now, updatedBy = ctx.device,
        )
        return doc.copy(checks = doc.checks.copy(acknowledgements = doc.checks.acknowledgements + ack))
    }

    /** `맞다고 표시한 것 › 되돌리기`. */
    fun unmarkIntended(doc: ProjectDoc, linkKey: Key): ProjectDoc = doc.copy(
        checks = doc.checks.copy(acknowledgements = doc.checks.acknowledgements.filterNot {
            it.checkType == CheckType.TERM_BEFORE_REVEAL && it.subjectKeys == listOf(linkKey)
        }),
    )

    /** Removes a link (text stays) wherever it is; its `맞음` goes with it (WLD-17). */
    fun unlink(doc: ProjectDoc, linkKey: Key): ProjectDoc =
        unmarkIntended(DocRuns.mapAll(doc) { runs -> RunEdits.unwrap(runs, linkKey) ?: runs }, linkKey)
}
