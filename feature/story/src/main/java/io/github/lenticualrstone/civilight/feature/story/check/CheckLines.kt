package io.github.lenticualrstone.civilight.feature.story.check

import io.github.lenticualrstone.civilight.domain.checks.CheckListRow
import io.github.lenticualrstone.civilight.domain.checks.CheckResult

/**
 * One lazy item of 점검 센터. A row with its `◇` sub-rows, and a group with its members, used to be one
 * item each, so a fold could not animate its members (motion audit, feature:story 2). Every line is
 * now its own keyed item; [key] is unique within one list.
 */
sealed interface CheckLine {
    val key: String

    /** A top-level row: an item, a folded group head (with its buttons) or the locked hidden row. */
    data class Top(val row: CheckListRow, override val key: String) : CheckLine

    /** A `◇ 공개 전 후보` sub-row under [parent]. */
    data class Sub(val parent: CheckListRow.Item, val result: CheckResult, override val key: String) : CheckLine

    /** A member of an open [group]. */
    data class Member(val group: CheckListRow.Group, val result: CheckResult, override val key: String) : CheckLine
}

object CheckLines {
    /**
     * The flat lines for [rows], with the members of the groups in [expanded] (group ids). Keys are
     * prefixed by kind and made unique, since a duplicate lazy key crashes at runtime.
     */
    fun flatten(rows: List<CheckListRow>, expanded: Collection<String>): List<CheckLine> {
        val seen = HashSet<String>()
        fun unique(base: String): String {
            var key = base
            var n = 1
            while (!seen.add(key)) key = "$base#${n++}"
            return key
        }
        val out = ArrayList<CheckLine>()
        rows.forEach { row ->
            when (row) {
                is CheckListRow.HiddenGroup -> out += CheckLine.Top(row, unique("hidden"))
                is CheckListRow.Item -> {
                    out += CheckLine.Top(row, unique("row:${row.result.id}"))
                    row.subRows.forEach { sub -> out += CheckLine.Sub(row, sub, unique("sub:${row.result.id}:${sub.id}")) }
                }
                is CheckListRow.Group -> {
                    val id = row.group.id
                    out += CheckLine.Top(row, unique("group:$id"))
                    if (id in expanded) row.members.forEach { member -> out += CheckLine.Member(row, member, unique("member:$id:${member.id}")) }
                }
            }
        }
        return out
    }
}
