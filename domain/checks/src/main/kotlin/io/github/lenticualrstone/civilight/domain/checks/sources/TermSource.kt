package io.github.lenticualrstone.civilight.domain.checks.sources

import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.domain.checks.CheckGroup
import io.github.lenticualrstone.civilight.domain.checks.CheckRegistry
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.checks.Highlight
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.Snippet
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermCheckRow
import io.github.lenticualrstone.civilight.domain.world.term.TermFingerprints
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.domain.world.term.TextSpot
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType

/**
 * Rows 1–3 (`용어 공개`) from the world area's rule-21 readings (WLD-28). The query runs with hidden
 * terms open so their rows exist; hiding is applied afterwards as [CheckResult.lockedHidden].
 */
internal object TermSource {
    data class Output(val results: List<CheckResult>, val scanning: Boolean) {
        fun ofType(checkType: String): List<CheckResult> = results.filter { it.checkType == checkType }
    }

    fun compute(scope: CheckScope): Output {
        val doc = scope.doc
        val index = scope.inputs.candidates ?: CandidateIndex.build(doc, TermAccess(hiddenUnlocked = true))
        val queries = TermQueries(doc, index, TermAccess(hiddenUnlocked = true))
        val report = queries.checks()
        val out = ArrayList<CheckResult>()
        for (row in report.rows) {
            when (row) {
                is TermCheckRow.BeforeReveal -> out += beforeReveal(scope, queries, row, group = null)
                is TermCheckRow.BeforeRevealGroup -> {
                    val name = doc.index.term(row.termKey)?.name.orEmpty()
                    val group = CheckGroup("term:${row.termKey}", "$name${CheckCopy.SEP}▲")
                    row.rows.forEach { out += beforeReveal(scope, queries, it, group) }
                }
                is TermCheckRow.CandidateBeforeReveal -> out += candidate(scope, queries, row)
                is TermCheckRow.NoReveal -> out += noReveal(scope, queries, row)
                // Never produced with hidden terms open; deleted terms are the trash area's rows (row 9).
                is TermCheckRow.HiddenLocked, is TermCheckRow.DeletedTerm -> Unit
            }
        }
        // `맞음`-marked links: listed under `맞다고 표시한 것` (their fingerprint matches the stored one).
        report.intended.forEach { out += beforeReveal(scope, queries, it, group = null) }
        return Output(out, scanning = !index.complete)
    }

    private fun beforeReveal(scope: CheckScope, q: TermQueries, row: TermCheckRow.BeforeReveal, group: CheckGroup?): CheckResult {
        val link = row.link
        val (blockKey, sceneKey) = keysOf(scope, link.spot)
        val target = blockKey ?: sceneKey
        return CheckResult(
            checkType = CheckType.TERM_BEFORE_REVEAL,
            subjectKeys = listOf(link.linkKey),
            fingerprint = TermFingerprints.beforeReveal(link.termKey, link.linkKey),
            title = CheckCopy.TERM_BEFORE_REVEAL,
            location = q.describer.location(link.spot),
            body = row.snippet.render(),
            highlight = highlightOf(row.snippet),
            detail = row.detail,
            flag = row.marker,
            actions = listOf(
                FixAction(CheckCopy.GO_TO_SCRIPT, FixKind.GO_TO_SCRIPT, target),
                FixAction(CheckCopy.MOVE_REVEAL, FixKind.MOVE_REVEAL, link.termKey),
                FixAction(CheckRegistry.ackLabel(CheckType.TERM_BEFORE_REVEAL)!!, FixKind.ACKNOWLEDGE, link.linkKey),
            ),
            storyOrder = link.rank?.value,
            lockedHidden = scope.hiding.term(link.termKey) || hiddenSpot(scope, link.spot),
            group = group,
            nestKey = "term:${link.termKey}",
        )
    }

    private fun candidate(scope: CheckScope, q: TermQueries, row: TermCheckRow.CandidateBeforeReveal): CheckResult {
        val c = row.candidate
        val term = scope.doc.index.term(c.termKey)
        val reveal = q.reveal(c.termKey)
        val tag = q.candidateTag(c)?.let { " $it" }.orEmpty()
        val (blockKey, sceneKey) = keysOf(scope, c.spot)
        val optionKey = (c.spot as? ScriptSpot.OptionText)?.optionKey
        val subjects = listOfNotNull(c.termKey, blockKey ?: sceneKey, optionKey)
        return CheckResult(
            checkType = CheckType.TERM_CANDIDATE_BEFORE_REVEAL,
            subjectKeys = subjects,
            fingerprint = Fingerprints.of(CheckType.TERM_CANDIDATE_BEFORE_REVEAL, subjects, c.start, c.end, c.matchedText),
            title = CheckCopy.TERM_CANDIDATE,
            location = q.describer.location(c.spot),
            body = row.snippet.render(),
            highlight = highlightOf(row.snippet),
            detail = CheckCopy.join("‘${term?.name.orEmpty()}’$tag", reveal?.listLabel),
            actions = listOf(
                FixAction(CheckCopy.GO_TO_SCRIPT, FixKind.GO_TO_SCRIPT, blockKey ?: sceneKey),
                FixAction(CheckCopy.LINK, FixKind.LINK_CANDIDATE, c.termKey),
                FixAction(CheckCopy.REJECT, FixKind.REJECT_CANDIDATE, c.termKey),
            ),
            storyOrder = q.rankOf(c)?.value,
            lockedHidden = scope.hiding.term(c.termKey) || hiddenSpot(scope, c.spot),
            nestUnder = "term:${c.termKey}",
        )
    }

    private fun noReveal(scope: CheckScope, q: TermQueries, row: TermCheckRow.NoReveal): CheckResult {
        val term = scope.doc.index.term(row.termKey)
        val name = term?.name.orEmpty()
        val usage = q.usage(row.termKey)
        val first = usage.firstOrNull()?.let { keysOf(scope, it.spot) }
        val chip = row.chip?.let { FixAction(it.label, FixKind.SET_REVEAL, row.termKey) }
        return CheckResult(
            checkType = CheckType.TERM_NO_REVEAL,
            subjectKeys = listOf(row.termKey),
            fingerprint = Fingerprints.of(CheckType.TERM_NO_REVEAL, row.termKey, usage.size),
            title = CheckCopy.TERM_NO_REVEAL,
            location = CheckCopy.termLocation(name),
            body = CheckCopy.join(name, CheckCopy.usedIn(usage.size)),
            actions = listOfNotNull(chip, FixAction(CheckCopy.GO_TO_SCRIPT, FixKind.GO_TO_SCRIPT, first?.first ?: first?.second)),
            storyOrder = usage.firstOrNull()?.rank?.value,
            lockedHidden = scope.hiding.term(row.termKey),
        )
    }

    /** Block and scene of a manuscript spot (null for text outside the manuscript). */
    private fun keysOf(scope: CheckScope, spot: TextSpot): Pair<Key?, Key?> = when (spot) {
        is ScriptSpot.BlockText -> spot.blockKey to scope.doc.index.block(spot.blockKey)?.sceneKey
        is ScriptSpot.OptionText -> spot.blockKey to scope.doc.index.block(spot.blockKey)?.sceneKey
        is ScriptSpot.TransitionText -> null to spot.sceneKey
        else -> null to null
    }

    private fun hiddenSpot(scope: CheckScope, spot: TextSpot): Boolean {
        val (blockKey, sceneKey) = keysOf(scope, spot)
        return blockKey?.let(scope.hiding::block) == true || sceneKey?.let(scope.hiding::scene) == true
    }

    /** The box of a snippet inside its rendered line (`세라: “` comes first for dialogue). */
    internal fun highlightOf(snippet: Snippet): Highlight {
        val offset = snippet.speaker?.let { it.length + 3 } ?: 0
        return Highlight(offset + snippet.boxStart, offset + snippet.boxEnd)
    }
}
