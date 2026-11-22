package io.github.lenticualrstone.civilight.feature.map.gen

import org.junit.Assert.assertEquals
import org.junit.Test

class GridFillTest {
    @Test
    fun `two format tiles fill the cover width to both margins`() {
        // 390 dp screen, 24 dp margins: 342 dp. Two 166 dp tiles need 332 + 10.
        assertEquals(2, GridFill.columns(342f, 166f))
        assertEquals(10f, GridFill.gap(342f, 166f, 2), 1e-4f)
        // A wider cover screen (363 dp of content): the leftover goes between the tiles, not after them.
        assertEquals(2, GridFill.columns(363f, 166f))
        assertEquals(31f, GridFill.gap(363f, 166f, 2), 1e-4f)
        assertEquals(363f, 2 * 166f + GridFill.gap(363f, 166f, 2), 1e-3f)
    }

    @Test
    fun `an inner window gets more columns, spread to the edges`() {
        val w = 752f
        val n = GridFill.columns(w, 166f)
        assertEquals(4, n)
        assertEquals(w, n * 166f + (n - 1) * GridFill.gap(w, 166f, n), 1e-3f)
    }

    @Test
    fun `narrow or empty space still gives one column`() {
        assertEquals(1, GridFill.columns(100f, 166f))
        assertEquals(1, GridFill.columns(0f, 166f))
        assertEquals(GridFill.MIN_GAP, GridFill.gap(100f, 166f, 1), 0f)
    }

    @Test
    fun `rows split the tiles by the column count`() {
        assertEquals(listOf(listOf(1, 2, 3), listOf(4, 5)), GridFill.rows(listOf(1, 2, 3, 4, 5), 3))
        assertEquals(listOf(listOf(1), listOf(2)), GridFill.rows(listOf(1, 2), 0))
    }
}
