package io.github.lenticualrstone.civilight.feature.world.time

/**
 * (U-15.6, WLD-70) Where the 연표 list scrolls after `만들기`: the lazy-list index of the row of
 * [eventKey], counting the items above the rows (`info`, `chips`, and `＋ 새 사건` when [hasCreate]).
 * Null when the event has no row. The list scrolls there only if that row is out of view.
 */
object TimelineScroll {
    /** Items above the first row: `info`, `chips` and, unless read-only, the create row. */
    fun headerItems(hasCreate: Boolean): Int = if (hasCreate) 3 else 2

    fun rowIndex(rows: List<TimelineRowUi>, eventKey: String?, hasCreate: Boolean): Int? {
        if (eventKey == null) return null
        val i = rows.indexOfFirst { it.eventKey == eventKey }
        return if (i < 0) null else i + headerItems(hasCreate)
    }
}
