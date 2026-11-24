package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.feature.library.shelf.views.BookcaseGeo
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ShelfScene
import io.github.lenticualrstone.civilight.feature.library.shelf.views.SpineText
import io.github.lenticualrstone.civilight.feature.library.shelf.views.Tones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.1 QA fixes of the 서재 3D views: the stage fits the pane, spines differ, spine text keeps its spaces. */
class Shelf021Test {
    @Test
    fun stageKeepsTheBoardWhenItFits() {
        assertEquals(500f, ShelfScene.stageHeight(null), 0f)
        assertEquals(500f, ShelfScene.stageHeight(900f), 0f)
        assertEquals(1f, ShelfScene.stageScale(500f), 0f)
    }

    @Test
    fun aShortPaneScalesTheWholeBoardSoTheDockStaysOnScreen() {
        // A pane 560 dp tall under the header and above the command bar.
        val h = ShelfScene.stageHeight(560f)
        assertEquals(560f - ShelfScene.LABEL_H - ShelfScene.DOCK_H, h, 0f)
        assertTrue(h + ShelfScene.LABEL_H + ShelfScene.DOCK_H <= 560f)
        // The inner display's list pane (about 440 dp): the minimum stage, the board at 0.6.
        assertEquals(ShelfScene.STAGE_MIN_H, ShelfScene.stageHeight(440f), 0f)
        assertEquals(0.6f, ShelfScene.stageScale(ShelfScene.stageHeight(440f)), 1e-6f)
        assertEquals(h / 500f, ShelfScene.stageScale(h), 1e-6f)
        // Never below the minimum (the list scrolls instead).
        assertEquals(ShelfScene.STAGE_MIN_H, ShelfScene.stageHeight(100f), 0f)
    }

    @Test
    fun neighbouringSpinesNeverShareACloth() {
        val seeds = List(12) { "project-$it" } + listOf(null) + List(4) { "same" }
        val slots = Tones.slots(seeds)
        for (i in 1 until slots.size) {
            if (slots[i] < 0 || slots[i - 1] < 0) continue
            assertNotEquals("books $i and ${i - 1}", slots[i - 1], slots[i])
            if (i >= 2 && slots[i - 2] >= 0) assertNotEquals(slots[i - 2], slots[i])
        }
        assertEquals(-1, slots[12])
        // The bookcase uses the board's three cloths: any three books in a row all differ.
        val a = Tones.slots(List(9) { "book-$it" }, Tones.BOARD_A)
        for (i in 2 until a.size) assertEquals(3, setOf(a[i], a[i - 1], a[i - 2]).size)
        assertTrue(a.all { it in 0 until Tones.BOARD_A })
        // Stable: the same seeds give the same cloths.
        assertEquals(slots.toList(), Tones.slots(seeds).toList())
    }

    @Test
    fun spineTextKeepsSpacesAndEndsInAnEllipsisWhenTooLong() {
        val short = SpineText.fit("Fog Harbor", 12)
        assertEquals("Fog Harbor".toList(), short.glyphs)
        assertEquals(1f, short.scale, 0f)
        // 9.5 steps in 8: tightened, nothing cut.
        val tight = SpineText.fit("Fog Harbor", 9)
        assertEquals("Fog Harbor".toList(), tight.glyphs)
        assertTrue(tight.scale < 1f && tight.scale >= SpineText.MIN_SCALE)
        // Far too long: cut at a glyph, never ending in a space, with `︙`.
        val long = SpineText.fit("Ashen Dawn Lamp", 7)
        assertEquals(SpineText.ELLIPSIS, long.glyphs.last())
        assertTrue(long.glyphs[long.glyphs.size - 2] != ' ')
        assertTrue(SpineText.steps(long.glyphs) * long.scale <= 7f + 1e-4f)
        assertTrue(long.glyphs.contains(' '))
    }

    @Test
    fun booksPastTheBoardsThreeGetStableSizesInTheBoardsRanges() {
        for (k in 0 until 50) {
            val d = BookcaseGeo.hashed("p$k")
            assertTrue(d.t in 34f..46f)
            assertTrue(d.h in 196f..238f)
            assertTrue(d.d in 130f..154f)
            assertEquals(d.t, BookcaseGeo.hashed("p$k").t, 0f)
        }
    }
}
