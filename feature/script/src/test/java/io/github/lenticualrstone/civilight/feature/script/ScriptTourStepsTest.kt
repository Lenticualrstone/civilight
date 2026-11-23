package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.ScriptTourSteps.Item
import io.github.lenticualrstone.civilight.ui.tour.TourStepFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** LIB-19 script tour: the holes go around rows on screen (emulator QA: the first step pointed at nothing). */
class ScriptTourStepsTest {
    private val blocks = setOf("block:a", "block:b", "block:c")
    private val heads = setOf("head:1", "head:2")

    @Test
    fun `steps in board order`() {
        assertEquals(0, ScriptTourSteps.COMMAND)
        assertEquals(1, ScriptTourSteps.BLOCK)
        assertEquals(2, ScriptTourSteps.HEAD)
    }

    @Test
    fun `the command step is left out everywhere in 0_2_0`() {
        // Emulator QA round 2: the example draws `예시 홈으로 · 온보딩 끝내기` where `명령 칸` would be; (0.2.0 LIB-96)
        // U-14 removed the command bar and its mic, so a writable script leaves the step out too.
        assertEquals(setOf(ScriptTourSteps.COMMAND), ScriptTourSteps.absent(readOnly = true))
        assertEquals(setOf(ScriptTourSteps.COMMAND), ScriptTourSteps.absent(readOnly = false))
        assertEquals(
            listOf(ScriptTourSteps.BLOCK, ScriptTourSteps.HEAD),
            TourStepFilter.shown(ScriptTourSteps.COUNT, ScriptTourSteps.absent(readOnly = true)),
        )
        assertEquals(listOf(1, 2), TourStepFilter.shown(ScriptTourSteps.COUNT, ScriptTourSteps.absent(readOnly = false)))
    }

    @Test
    fun `the tour ends at the scene head - no lens step`() {
        // The script has no lens (STO-16): `시점 렌즈` had no hole and floated (emulator QA f-04).
        assertEquals(3, ScriptTourSteps.COUNT)
        assertEquals(ScriptTourSteps.COUNT - 1, ScriptTourSteps.HEAD)
    }

    @Test
    fun `a row step with no row on screen leaves the tour`() {
        assertEquals(emptySet<Int>(), ScriptTourSteps.missing("block:a", "head:1", readOnly = false))
        assertEquals(setOf(ScriptTourSteps.HEAD), ScriptTourSteps.missing("block:a", null, readOnly = true))
        // (0.2.0) With `명령 칸` always absent, no rows on screen would leave nothing: the guard keeps both.
        assertEquals(emptySet<Int>(), ScriptTourSteps.missing(null, null, readOnly = false))
        // Never every step: the read-only script with no rows keeps them (guard against a tour that blinks).
        assertEquals(emptySet<Int>(), ScriptTourSteps.missing(null, null, readOnly = true))
    }

    @Test
    fun `a row cut by the top bar gives way to the first whole one`() {
        val items = listOf(
            Item("block:a", -80, 40),
            Item("head:2", 66, 200),
            Item("block:b", 226, 400),
            Item("block:c", 426, 1300),
        )
        assertEquals("block:b", ScriptTourSteps.firstVisible(items, 0, 1200) { it in blocks })
        assertEquals("head:2", ScriptTourSteps.firstVisible(items, 0, 1200) { it in heads })
    }

    @Test
    fun `only partly visible rows - the first one that shows at all`() {
        val items = listOf(Item("block:a", -80, 40), Item("block:c", 100, 2000))
        assertEquals("block:a", ScriptTourSteps.firstVisible(items, 0, 1200) { it in blocks })
    }

    @Test
    fun `no such row on screen - no hole`() {
        val items = listOf(Item("block:a", 0, 100))
        assertNull(ScriptTourSteps.firstVisible(items, 0, 1200) { it in heads })
        assertNull(ScriptTourSteps.firstVisible(emptyList(), 0, 1200) { it in blocks })
    }
}
