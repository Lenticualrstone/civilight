package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.game.GameTools

/**
 * (0.2.0, S-2, D33, G-2) The 점검 센터 limited to items that affect writing. Kept: the term kinds, speaker
 * missing, ◆ anchor deleted, deleted item, time band and order, the transition kinds, the hidden kinds.
 * `◇ 첫 공개 없음` stays registered (stored acknowledgements keep their type, §8) but no longer fires for a
 * live term: with S-5 every term used with `@` has an automatic reveal at its first use (D12).
 * Gated:
 * - the three voice kinds (and the `보이스` chip) follow the project's `게임 제작 도구` switch;
 * - the five travel kinds (and the `이동` chip) follow `이동 점검` ([CheckInputs.travel]), `◇ 장소 없음` included;
 * - `◇ 공개 전 후보` follows the project's `용어 후보` setting.
 *
 * Gated items are only left out of the list: nothing is stored or removed. Acknowledgements of gated kinds
 * stay in the project (a type that does not fire is never judged void by [CheckAcks.voided]), so switching
 * a gate on again shows them under `맞다고 표시한 것` as before (data safety §8).
 */
object WritingChecks {
    /** `▲ 보이스 비어 있음` · `◇ 형식과 다른 ID` · `▲ 보이스 ID 겹침` (G-2). */
    val VOICE_TYPES: Set<String> = setOf(CheckType.VOICE_EMPTY, CheckType.VOICE_FORMAT_DIFFERS, CheckType.VOICE_DUPLICATE)

    /** Rows 10–14 (D33 "the five travel kinds"). */
    val TRAVEL_TYPES: Set<String> = setOf(
        CheckType.TRAVEL_SHORTAGE,
        CheckType.TRAVEL_SAME_TIME_TWO_PLACES,
        CheckType.TRAVEL_SIMULTANEOUS,
        CheckType.TRAVEL_NO_PLACE,
        CheckType.TRAVEL_NO_SCALE,
    )

    /** Whether items of [checkType] are listed for [doc] with [inputs]. */
    fun shows(doc: ProjectDoc, inputs: CheckInputs, checkType: String): Boolean = when (checkType) {
        in VOICE_TYPES -> GameTools.on(doc)
        in TRAVEL_TYPES -> inputs.travel.checkTravel
        CheckType.TERM_CANDIDATE_BEFORE_REVEAL -> doc.project.termCandidates
        else -> true
    }

    /** Filter chips that are not drawn: `보이스` with the switch off, `이동` with the travel check off. */
    fun hiddenFilters(doc: ProjectDoc, inputs: CheckInputs): Set<CheckFilter> = buildSet {
        if (!GameTools.on(doc)) add(CheckFilter.VOICE)
        if (!inputs.travel.checkTravel) add(CheckFilter.TRAVEL)
    }

    /** [ProjectChecks.run] with the gated kinds left out; counts, rows and `warningCount` follow. */
    fun run(doc: ProjectDoc, inputs: CheckInputs): CheckReport {
        val full = ProjectChecks.run(doc, inputs)
        val kept = full.results.filter { shows(doc, inputs, it.checkType) }
        return if (kept.size == full.results.size) full else CheckReport(doc, inputs, kept, full.scanning)
    }

    /** [ProjectChecks.check] for one type, empty while that type is gated. */
    fun check(doc: ProjectDoc, inputs: CheckInputs, checkType: String): List<CheckResult> =
        if (shows(doc, inputs, checkType)) ProjectChecks.check(doc, inputs, checkType) else emptyList()

    /** [CheckCenterView.header] without the [hiddenFilters] chips. */
    fun header(report: CheckReport): CheckHeader {
        val hidden = hiddenFilters(report.doc, report.inputs)
        val header = CheckCenterView.header(report)
        return if (hidden.isEmpty()) header else header.copy(chips = header.chips.filterNot { it.filter in hidden })
    }

    /** [filter] when its chip is drawn, else `전체` (a route that names a hidden chip). */
    fun shownFilter(doc: ProjectDoc, inputs: CheckInputs, filter: CheckFilter): CheckFilter =
        if (filter in hiddenFilters(doc, inputs)) CheckFilter.ALL else filter
}
