package io.github.lenticualrstone.civilight.feature.world

import io.github.lenticualrstone.civilight.feature.world.term.TermCardScroll
import io.github.lenticualrstone.civilight.feature.world.term.UsageGroupUi
import io.github.lenticualrstone.civilight.feature.world.time.TimelineRowKind
import io.github.lenticualrstone.civilight.feature.world.time.TimelineRowUi
import io.github.lenticualrstone.civilight.feature.world.time.TimelineScroll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0) WLD-73 card scroll to `후보`, U-15.6 / WLD-70 연표 scroll to a new event. */
class World020ScrollTest {
    @Test
    fun `WLD-73 a 후보 row request is taken once, by that card only`() {
        TermCardScroll.request("p", "t1")
        assertFalse(TermCardScroll.take("p", "t2"))
        assertTrue(TermCardScroll.take("p", "t1"))
        assertFalse(TermCardScroll.take("p", "t1"))
        TermCardScroll.request("p", "t1")
        TermCardScroll.request("p", "t2")
        assertFalse(TermCardScroll.take("p", "t1"))
        assertTrue(TermCardScroll.take("p", "t2"))
    }

    @Test
    fun `WLD-73 the 후보 group index follows the card body's items`() {
        val one = listOf(UsageGroupUi(TermCardScroll.CANDIDATES, "후보", emptyList()))
        assertEquals(3, TermCardScroll.candidatesIndex(hasNotices = false, groups = one))
        assertEquals(4, TermCardScroll.candidatesIndex(hasNotices = true, groups = one))
        assertNull(TermCardScroll.candidatesIndex(false, listOf(UsageGroupUi("links", "쓰인 곳", emptyList()))))
        val two = listOf(UsageGroupUi("links", "쓰인 곳", emptyList()), UsageGroupUi(TermCardScroll.CANDIDATES, "후보", emptyList()))
        // art, head, fixed, header, group:links (0 rows), group:candidates
        assertEquals(5, TermCardScroll.candidatesIndex(false, two))
    }

    private fun row(key: String?) = TimelineRowUi(key ?: "d", TimelineRowKind.entries.first(), "", null, "", "", false, false, key)

    @Test
    fun `U-15-6 the 연표 scrolls to the new event's row, below info, chips and the create row`() {
        val rows = listOf(row("e1"), row(null), row("e2"))
        assertEquals(5, TimelineScroll.rowIndex(rows, "e2", hasCreate = true))
        assertEquals(4, TimelineScroll.rowIndex(rows, "e2", hasCreate = false))
        assertNull(TimelineScroll.rowIndex(rows, "e9", hasCreate = true))
        assertNull(TimelineScroll.rowIndex(rows, null, hasCreate = true))
    }
}
