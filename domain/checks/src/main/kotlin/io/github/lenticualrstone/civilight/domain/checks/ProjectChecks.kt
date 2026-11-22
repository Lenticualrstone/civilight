package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.checks.sources.CheckScope
import io.github.lenticualrstone.civilight.domain.checks.sources.Emitters
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.checks.CheckCenter
import io.github.lenticualrstone.civilight.domain.story.checks.CheckEntry
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Every check item of one project state, open and acknowledged (results are never stored).
 *
 * @property scanning `점검 중`: the term candidate scan has not finished (R21 §7); candidate rows are
 *   missing or provisional and the chip counts are dimmed.
 */
class CheckReport(
    val doc: ProjectDoc,
    val inputs: CheckInputs,
    val results: List<CheckResult>,
    val scanning: Boolean,
) {
    /** Items without a valid acknowledgement. */
    val open: List<CheckResult> by lazy { Acks.open(doc, results) }

    /** Items listed under `맞다고 표시한 것`. */
    val acknowledged: List<CheckResult> by lazy { Acks.acknowledged(doc, results) }

    /** The registry entries `domain:story`'s [CheckCenter] counts and orders. */
    val entries: List<CheckEntry> by lazy { results.map { it.entry() } }

    fun isAcknowledged(result: CheckResult): Boolean = Acks.isAcknowledged(doc, result)

    /** Items of one check type (open and acknowledged). */
    fun ofType(checkType: String): List<CheckResult> = results.filter { it.checkType == checkType }

    /** Open items of one filter chip (`ACKNOWLEDGED`: acknowledged items). */
    fun inFilter(filter: CheckFilter): List<CheckResult> = when (filter) {
        CheckFilter.ALL -> open
        CheckFilter.ACKNOWLEDGED -> acknowledged
        else -> open.filter { it.filter == filter }
    }

    /** Count of a filter chip: open items, `◇` included (R21 §8); locked hidden items count too. */
    fun count(filter: CheckFilter): Int = CheckCenter.count(doc, entries, filter)

    /** Open `▲` items (script module badge and the `▲` marks elsewhere in the app). */
    val warningCount: Int get() = open.count { it.marker == Marker.WARNING }
}

/** Runs the registry's check functions over one project state (STO-40). */
object ProjectChecks {
    /** Every check type in registry order; engines shared by several types run once. */
    fun run(doc: ProjectDoc, inputs: CheckInputs): CheckReport {
        val scope = CheckScope(doc, inputs)
        val results = CheckRegistry.all.flatMap { Emitters.emit(scope, it.checkType) }
        return CheckReport(doc, inputs, results, scanning = scope.terms.scanning)
    }

    /** The items of one check type (open and acknowledged). */
    fun check(doc: ProjectDoc, inputs: CheckInputs, checkType: String): List<CheckResult> =
        Emitters.emit(CheckScope(doc, inputs), checkType)
}
