package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.checks.CheckCenter
import io.github.lenticualrstone.civilight.domain.story.checks.CheckEntry
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.CheckRow
import io.github.lenticualrstone.civilight.domain.story.checks.EmptyCheck
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.model.checks.CheckType

/** One filter chip: `전체 5` · `이동 4` · `용어 공개`; [dimmed] while `점검 중`. */
data class CheckChip(val filter: CheckFilter, val label: String, val count: Int, val dimmed: Boolean)

/** Header of 점검 센터 (CNV-17): project name, title, intro, chips and the `점검 중` label. No lens chip. */
data class CheckHeader(
    val projectName: String,
    val chips: List<CheckChip>,
    /** `점검 중` while the scan runs, else null. */
    val scanningLabel: String?,
) {
    val title: String get() = CheckCopy.SCREEN_TITLE
    val intro: String get() = CheckCopy.INTRO
}

/** A row of the list. */
sealed interface CheckListRow {
    /**
     * One item with the buttons shown for it ([actions]: its own, or `되돌리기` under
     * `맞다고 표시한 것`) and its `◇ 공개 전 후보` sub-rows (dashed, under a `▲ 공개 전 사용` row).
     */
    data class Item(val result: CheckResult, val actions: List<FixAction>, val subRows: List<CheckResult> = emptyList()) : CheckListRow

    /** `◇ 근거 지워진 변화 2`, `◇ 형식과 다른 ID 3`, `잿불의 서약 · ▲ 3`: expands to [members]. */
    data class Group(val group: CheckGroup, val members: List<CheckResult>, val actions: List<FixAction>) : CheckListRow {
        val title: String get() = group.title(members.size)
    }

    /** `▲ 숨긴 항목 관련 2 · 지문으로 보기` (locked hidden items, R19 §6). */
    data class HiddenGroup(val count: Int) : CheckListRow {
        val text: String get() = CheckCopy.hiddenGroup(count)
        val action: FixAction get() = FixAction(CheckCopy.UNLOCK, FixKind.UNLOCK)
    }
}

/**
 * The 점검 센터 list (STO-39, CNV-17) over a [CheckReport]: chips with counts, rows per filter (▲
 * before ◇, then story order, locked hidden items collapsed at the top, `domain:story`'s
 * [CheckCenter]), groups and sub-rows, and the empty state.
 */
object CheckCenterView {
    fun header(report: CheckReport): CheckHeader {
        val labels = CheckCenter.chips(report.doc, report.entries)
        val chips = CheckFilter.entries.mapIndexed { i, f -> CheckChip(f, labels[i], report.count(f), dimmed = report.scanning) }
        return CheckHeader(report.doc.project.name, chips, if (report.scanning) CheckCopy.SCANNING else null)
    }

    fun rows(report: CheckReport, filter: CheckFilter = CheckFilter.ALL): List<CheckListRow> {
        val byEntry = HashMap<CheckEntry, ArrayDeque<CheckResult>>()
        report.results.forEach { byEntry.getOrPut(it.entry()) { ArrayDeque() }.addLast(it) }
        val base = CheckCenter.rows(report.doc, report.entries, filter)
        val hidden = base.filterIsInstance<CheckRow.HiddenGroup>().map { CheckListRow.HiddenGroup(it.count) }
        val ordered = base.filterIsInstance<CheckRow.Item>().mapNotNull { byEntry[it.entry]?.removeFirstOrNull() }
        if (filter == CheckFilter.ACKNOWLEDGED) return hidden + ordered.map { CheckListRow.Item(it, listOfNotNull(revertAction(report, it))) }
        return hidden + fold(ordered)
    }

    /** Nests `◇ 공개 전 후보` under the first `▲` row of their term, and folds grouped rows at the first member. */
    private fun fold(ordered: List<CheckResult>): List<CheckListRow> {
        val parents = ordered.filter { it.nestKey != null && it.group == null }.groupBy { it.nestKey!! }.mapValues { it.value.first() }
        val children = ordered.filter { it.nestUnder != null && it.nestUnder in parents }.groupBy { it.nestUnder!! }
        val nested = children.values.flatten().toSet()
        val groups = ordered.filter { it.group != null }.groupBy { it.group!! }
        val out = ArrayList<CheckListRow>()
        val doneGroups = HashSet<CheckGroup>()
        for (r in ordered) {
            if (r in nested) continue
            val g = r.group
            if (g != null) {
                if (doneGroups.add(g)) {
                    val members = groups.getValue(g)
                    out += CheckListRow.Group(g, members, groupActions(g, members))
                }
                continue
            }
            val subRows = if (r.nestKey != null && parents[r.nestKey] === r) children[r.nestKey].orEmpty() else emptyList()
            out += CheckListRow.Item(r, r.actions, subRows)
        }
        return out
    }

    /** `모두 지우기` on a `◇ 근거 지워진 변화` group of several records (R12 §2). */
    private fun groupActions(group: CheckGroup, members: List<CheckResult>): List<FixAction> =
        if (members.first().checkType == CheckType.CHANGE_ANCHOR_DELETED && members.size > 1) {
            listOf(FixAction(CheckCopy.TRASH_ALL_RECORDS, FixKind.TRASH_ALL_RECORDS))
        } else {
            emptyList()
        }

    private fun revertAction(report: CheckReport, result: CheckResult): FixAction? =
        Acks.find(report.doc, result)?.let { FixAction(CheckCopy.REVERT, FixKind.REVERT, it.key) }

    /**
     * `어긋난 곳 없음` with `마지막 확인 · 방금 · 1-1 ~ 1-7` and `원고로` · `맞다고 표시한 것 N`, or null
     * while anything is open or the scan has not finished (`ED`).
     */
    fun empty(report: CheckReport, lastChecked: String = "방금"): EmptyCheck? =
        if (report.scanning) null else CheckCenter.empty(report.doc, report.entries, StoryOverview(report.doc).writtenRange(), lastChecked)
}
