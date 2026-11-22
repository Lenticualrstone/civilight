package io.github.lenticualrstone.civilight.feature.story.check

import io.github.lenticualrstone.civilight.domain.checks.CheckCenterView
import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.domain.checks.CheckHeader
import io.github.lenticualrstone.civilight.domain.checks.CheckListRow
import io.github.lenticualrstone.civilight.domain.checks.CheckReport
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.EmptyCheck
import io.github.lenticualrstone.civilight.domain.story.search.CommandBar
import io.github.lenticualrstone.civilight.domain.story.search.CommandScreen
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.domain.story.structure.resumeTarget
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.Script
import androidx.navigation3.runtime.NavKey

/** Everything 점검 센터 draws for one filter (STO-39, CNV-17; boards `Check`, `TravelCheck`, `EmptyCheck`). */
data class CheckUi(
    val header: CheckHeader,
    val filter: CheckFilter,
    val rows: List<CheckListRow>,
    /** `어긋난 곳 없음` (only under `전체` with nothing open, and never while `점검 중`). */
    val empty: EmptyCheck?,
    /** `점검 중`: the report is being computed (or the term scan is not finished); chips are dimmed. */
    val scanning: Boolean,
    val commandHint: String,
)

/** 점검 센터 as plain data over a `domain:checks` [CheckReport]. */
object CheckModel {
    /** The route's filter string (the `CheckFilter` name); null or unknown = `전체`. */
    fun filterOf(name: String?): CheckFilter = name?.let { n -> CheckFilter.entries.firstOrNull { it.name == n } } ?: CheckFilter.ALL

    /**
     * The screen for [filter]. [stale] = the doc changed since [report] was computed: the old rows
     * stay on screen while the new report runs, with `점검 중` and dimmed chips.
     */
    fun build(report: CheckReport, filter: CheckFilter, stale: Boolean): CheckUi {
        val scanning = stale || report.scanning
        val header = CheckCenterView.header(report).let { h ->
            if (!scanning) h else h.copy(chips = h.chips.map { it.copy(dimmed = true) }, scanningLabel = h.scanningLabel ?: CheckCopy.SCANNING)
        }
        val rows = CheckCenterView.rows(report, filter)
        val empty = if (filter == CheckFilter.ALL && rows.isEmpty() && !scanning) CheckCenterView.empty(report) else null
        return CheckUi(header, filter, rows, empty, scanning, CommandBar.placeholder(CommandScreen.CHECKS))
    }

    /**
     * Where tapping a row's location goes (STO-39 "Tap the location → open the script"): the row's
     * `원고로 가기` target, else its scene or block subject; null for locations outside the manuscript
     * (`인물 · 리엔`, `휴지통`).
     */
    fun locationTarget(doc: ProjectDoc, result: CheckResult): Key? {
        result.actions.firstOrNull { it.kind == FixKind.GO_TO_SCRIPT }?.target?.let { return it }
        return result.subjectKeys.firstOrNull { k ->
            doc.index.block(k) != null || doc.index.unit(k)?.level == doc.project.structure.sceneLevel
        }
    }

    /**
     * The position the right pane shows (`TwoCheck`, STO-43): the `Script` route right above this
     * 점검 센터 on the back stack (its block, else its scene), or null when no script is beside the list.
     */
    fun selectedKey(backStack: List<NavKey>, projectKey: Key): Key? {
        val top = backStack.lastOrNull() as? Script ?: return null
        val below = backStack.getOrNull(backStack.lastIndex - 1) as? CheckCenter ?: return null
        if (below.projectKey != projectKey || top.projectKey != projectKey) return null
        return top.blockKey ?: top.sceneKey
    }

    /** The row drawn as selected: its location is the position the right pane shows. */
    fun isSelected(doc: ProjectDoc, result: CheckResult, selectedKey: Key?): Boolean =
        selectedKey != null && locationTarget(doc, result) == selectedKey

    /** `원고로` on the empty state: the `이어 쓰기` block, else the first scene. */
    fun manuscriptRoute(doc: ProjectDoc): Script {
        val resume = StoryOverview(doc).resumeTarget()
        if (resume != null) return Script(doc.project.key, blockKey = resume.blockKey, caretAtEnd = true)
        val first = doc.manuscript.liveScenes.firstOrNull()?.key
        return Script(doc.project.key, sceneKey = first)
    }

}
