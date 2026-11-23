package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.common.TypingWriter
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryDetailSizes
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryLayout
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryPick
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryReveal
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryViewKind
import io.github.lenticualrstone.civilight.feature.canvas.gallery.PickState
import io.github.lenticualrstone.civilight.feature.canvas.page.PageBack
import io.github.lenticualrstone.civilight.feature.canvas.page.PageBackRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Emulator round-2 fixes: typing saves, arrival scroll, opened-row reveal, detail pane card size, page back. */
@OptIn(ExperimentalCoroutinesApi::class)
class GalleryRound2FixesTest {
    private val typed = "abcdefghijklmnopqrstuvwxyz0123456789"

    @Test
    fun `a fast burst of 36 keys writes the document once, with every letter`() = runTest {
        val writes = mutableListOf<String>()
        val writer = TypingWriter<String>(this) { writes += it }
        for (i in 1..typed.length) {
            writer.offer(typed.substring(0, i))
            advanceTimeBy(20)
        }
        assertTrue(writes.isEmpty())
        assertTrue(writer.hasPending)
        advanceTimeBy(TypingWriter.QUIET_MILLIS + 1)
        runCurrent()
        assertEquals(listOf(typed), writes)
        assertFalse(writer.hasPending)
    }

    @Test
    fun `typing in words writes once per pause, the last value each time`() = runTest {
        val writes = mutableListOf<String>()
        val writer = TypingWriter<String>(this) { writes += it }
        writer.offer("The")
        writer.offer("The harbor")
        advanceUntilIdle()
        writer.offer("The harbor lights")
        advanceUntilIdle()
        assertEquals(listOf("The harbor", "The harbor lights"), writes)
    }

    @Test
    fun `closing the sheet flushes what waits at once, and only once`() = runTest {
        val writes = mutableListOf<String>()
        val writer = TypingWriter<String>(this) { writes += it }
        writer.offer(typed.take(10))
        writer.offer(typed)
        writer.flush()
        assertEquals(listOf(typed), writes)
        advanceUntilIdle()
        writer.flush()
        assertEquals(listOf(typed), writes)
    }

    @Test
    fun `a focused field never writes on a pause, even a long stall in the middle of a burst`() = runTest {
        val writes = mutableListOf<String>()
        val writer = TypingWriter<String>(this) { writes += it }
        writer.hold()
        for (i in 1..typed.length) {
            writer.offer(typed.substring(0, i))
            // A main thread busy for over a second looks like a pause to a wall-clock timer.
            advanceTimeBy(if (i % 5 == 0) 1_500 else 20)
        }
        advanceUntilIdle()
        assertTrue(writes.isEmpty())
        assertTrue(writer.hasPending)
        writer.release()
        assertEquals(listOf(typed), writes)
        assertFalse(writer.isHeld)
        advanceUntilIdle()
        assertEquals(listOf(typed), writes)
    }

    @Test
    fun `focus taken while a pause runs cancels that pause write until focus leaves`() = runTest {
        val writes = mutableListOf<String>()
        val writer = TypingWriter<String>(this) { writes += it }
        writer.offer("The")
        writer.hold()
        advanceUntilIdle()
        assertTrue(writes.isEmpty())
        writer.offer("The harbor")
        writer.flush()
        assertEquals(listOf("The harbor"), writes)
        writer.release()
        assertEquals(listOf("The harbor"), writes)
    }

    @Test
    fun `catching up writes the last key that had not reached the writer, and nothing when nothing changed`() = runTest {
        val writes = mutableListOf<String>()
        val writer = TypingWriter<String>(this) { writes += it }
        writer.hold()
        writer.catchUp("", isStart = true)
        writer.flush()
        assertTrue(writes.isEmpty())
        writer.offer(typed.dropLast(1))
        writer.catchUp(typed.dropLast(1), isStart = false)
        writer.flush()
        assertEquals(listOf(typed.dropLast(1)), writes)
        writer.catchUp(typed, isStart = false)
        writer.flush()
        assertEquals(listOf(typed.dropLast(1), typed), writes)
        writer.catchUp(typed, isStart = false)
        writer.flush()
        assertEquals(2, writes.size)
    }

    @Test
    fun `arriving with the saved pick still scrolls to it`() {
        val keys = listOf("a", "b", "c", "d", "e", "f", "g")
        val saved = PickState("f", expanded = false)
        // The pick does not change (it already is `f`), which used to skip the scroll.
        assertEquals(saved, GalleryPick.arrive(saved, "f", keys))
        assertTrue(GalleryReveal.scrollsOnArrival("f", keys))
        assertTrue(GalleryReveal.scrollsOnArrival("b", keys))
        assertFalse(GalleryReveal.scrollsOnArrival(null, keys))
        assertFalse(GalleryReveal.scrollsOnArrival("x", keys))
    }

    @Test
    fun `an opened row below the bars is scrolled up just enough, never past its top`() {
        // Fits: nothing.
        assertEquals(0, GalleryReveal.delta(top = 100, bottom = 400, viewStart = 0, viewEnd = 800))
        // Ends 150 below the clear area, plenty of room above: 150.
        assertEquals(150, GalleryReveal.delta(top = 600, bottom = 950, viewStart = 0, viewEnd = 800))
        // Taller than the clear area: stops with the row's top at the start.
        assertEquals(700, GalleryReveal.delta(top = 700, bottom = 1700, viewStart = 0, viewEnd = 800))
        // Top already above the start (partly scrolled away): no scroll forward.
        assertEquals(0, GalleryReveal.delta(top = -40, bottom = 900, viewStart = 0, viewEnd = 800))
    }

    @Test
    fun `reveal follows list and grid on the cover and the centre column, not cards or the right pane`() {
        assertTrue(GalleryReveal.applies(GalleryViewKind.LIST, GalleryLayout.COVER))
        assertTrue(GalleryReveal.applies(GalleryViewKind.GRID, GalleryLayout.COVER))
        assertTrue(GalleryReveal.applies(GalleryViewKind.LIST, GalleryLayout.INNER_COLUMN))
        assertFalse(GalleryReveal.applies(GalleryViewKind.CARD, GalleryLayout.COVER))
        assertFalse(GalleryReveal.applies(GalleryViewKind.LIST, GalleryLayout.INNER_SPLIT))
        assertFalse(GalleryReveal.applies(GalleryViewKind.GRID, GalleryLayout.INNER_SPLIT))
    }

    @Test
    fun `detail pane card shrinks so the buttons stay on screen, keeping 3 to 4`() {
        // Tall pane: full 261 x 348.
        assertEquals(348f, GalleryDetailSizes.cardHeight(paneHeight = 1000f, top = 136f, bottom = 48f), 0f)
        // The folded inner screen of the report (about 700 dp, header 136, gesture bar 24): smaller card.
        val h = GalleryDetailSizes.cardHeight(paneHeight = 700f, top = 136f, bottom = 24f)
        assertEquals(700f - 136f - 16f - 24f - 24f - GalleryDetailSizes.OTHER_ROWS, h, 0.01f)
        assertTrue(h < 348f)
        assertEquals(h * 3f / 4f, GalleryDetailSizes.cardWidth(h), 0.01f)
        // Very short pane: never below the floor (the rest scrolls).
        assertEquals(GalleryDetailSizes.MIN_CARD_H, GalleryDetailSizes.cardHeight(paneHeight = 400f, top = 136f, bottom = 24f), 0f)
    }

    @Test
    fun `page back - a blank page that opened into editing leaves at once, user editing ends first`() {
        // New page: editing began by itself, so the first back leaves (it used to only end editing).
        assertEquals(PageBack.LEAVE, PageBackRules.onBack(editing = true, autoStarted = true))
        // Tap into the text of an existing page: back ends editing and shows the links again.
        assertEquals(PageBack.STOP_EDITING, PageBackRules.onBack(editing = true, autoStarted = false))
        assertEquals(PageBack.LEAVE, PageBackRules.onBack(editing = false, autoStarted = false))
        assertEquals(PageBack.LEAVE, PageBackRules.onBack(editing = false, autoStarted = true))
    }
}
