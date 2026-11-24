package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectCardUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRowUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ShelfScene
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ShelfViewCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.views.StarGroup
import io.github.lenticualrstone.civilight.designsystem.card.TiltMath
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-93 / DS-74 (lane lib3d): the pure parts of the six 서재 3D views. */
class ShelfSceneTest {
    private val cover = CoverUi(null, null, null, "가", holo = false)

    private fun card(key: String, format: ProjectFormat? = ProjectFormat.SERIAL, hiddenOpen: Boolean = false) = ProjectCardUi(
        row = ProjectRowUi(key, "제목 $key", Rating.ALL, "단편 · 씬 6개 · 3일 전", false, Protections(), false, format = format),
        chip = null,
        cover = cover,
        hiddenOpen = hiddenOpen,
    )

    /** The widths the 서재 column has: a cover screen and the 560 column of the inner screen. */
    private val widths = listOf(false to 360f, true to 560f)

    @Test
    fun `window caps the composed items around the item in front`() {
        assertEquals(7, ShelfScene.COVER_CAP)
        assertEquals(9, ShelfScene.WIDE_CAP)
        assertTrue(ShelfScene.window(0, 0, 7).isEmpty())
        assertEquals(0..0, ShelfScene.window(0, 1, 7))
        assertEquals(0..2, ShelfScene.window(2, 3, 7))
        assertEquals(0..6, ShelfScene.window(0, 40, 7))
        assertEquals(17..23, ShelfScene.window(20, 40, 7))
        assertEquals(33..39, ShelfScene.window(39, 40, 7))
        for (count in 0..30) for (focus in 0 until maxOf(count, 1)) {
            val w = ShelfScene.window(focus, count, 9)
            assertTrue(w.count() <= 9)
            if (count > 0) assertTrue("$focus in $w", focus in w)
        }
    }

    @Test
    fun `each view's window is its reach`() {
        for (wide in listOf(false, true)) {
            val cap = ShelfScene.cap(wide)
            for (style in ShelfScene.styles) {
                val reach = ShelfScene.reach(style, wide)
                assertEquals("$style wide=$wide", cap, reach.behind + 1 + reach.ahead)
                assertEquals(reach.behind, ShelfScene.behind(style, wide))
                assertEquals(reach.ahead, ShelfScene.ahead(style, wide))
            }
        }
        // The corridor and the drawer keep one item behind (a passed bay is hidden, a passed card falls and
        // fades); the pile spreads both ways (board B fan), so it is even like the others.
        val hall = ShelfScene.reach(LibraryStyle.ARCHIVE_HALL, false)
        assertEquals(1, hall.behind)
        assertEquals(5, hall.ahead)
        assertEquals(19..25, ShelfScene.window(20, 40, hall))
        assertEquals(0..6, ShelfScene.window(0, 40, hall))
        assertEquals(33..39, ShelfScene.window(39, 40, hall))
        assertEquals(3, ShelfScene.reach(LibraryStyle.MANUSCRIPT_STACK, false).behind)
        // Stepping the front one item moves the window by at most one item at either end.
        for (style in ShelfScene.styles) for (wide in listOf(false, true)) {
            val reach = ShelfScene.reach(style, wide)
            for (f in 0 until 39) {
                val a = ShelfScene.window(f, 40, reach)
                val b = ShelfScene.window(f + 1, 40, reach)
                assertTrue(f in a && f + 1 in b)
                assertTrue(b.first - a.first in 0..1)
            }
        }
    }

    @Test
    fun `accessibility - an item is named by its title and labelled by what a tap does`() {
        val project = ShelfCardItem.Project(card("a"))
        val lock = ShelfCardItem.Lock(HiddenGroupRow(3))
        assertEquals("제목 a", ShelfScene.itemName(project))
        assertEquals(HiddenGroupRow(3).title, ShelfScene.itemName(lock))
        assertEquals(ShelfCopy.NEW_PROJECT, ShelfScene.itemName(ShelfCardItem.NewProject))
        val pair = ShelfScene.pickPair(LibraryStyle.BOOKSHELF)!!
        assertEquals(pair.pick, ShelfScene.tapLabel(project, inFront = true, pair = pair, picked = false))
        assertEquals(pair.back, ShelfScene.tapLabel(project, inFront = true, pair = pair, picked = true))
        assertEquals(ShelfViewCopy.OPEN, ShelfScene.tapLabel(project, inFront = true, pair = null, picked = false))
        assertEquals(HiddenGroupRow(3).button, ShelfScene.tapLabel(lock, inFront = true, pair = pair, picked = false))
        assertEquals(ShelfCopy.NEW_PROJECT, ShelfScene.tapLabel(ShelfCardItem.NewProject, inFront = true, pair = pair, picked = false))
        // Away from the front a tap only brings the item forward: the default action is read.
        assertNull(ShelfScene.tapLabel(project, inFront = false, pair = pair, picked = false))
    }

    @Test
    fun `position settles on an item and a key survives a re-lock by falling back`() {
        assertEquals(0, ShelfScene.nearest(-0.3f, 5))
        assertEquals(4, ShelfScene.nearest(4.4f, 5))
        assertEquals(2, ShelfScene.nearest(1.6f, 5))
        assertEquals(0, ShelfScene.nearest(3f, 0))
        // A fling carries on; the ends hold.
        assertEquals(3, ShelfScene.settle(2.2f, 8f, 10))
        assertEquals(9, ShelfScene.settle(8.8f, 60f, 10))
        assertEquals(0, ShelfScene.settle(0.2f, -60f, 10))
        val keys = listOf<Any>("a", "b", "lockCard")
        assertEquals(1, ShelfScene.indexOf(keys, "b", 0))
        // The hidden project left with the re-lock: the item in front stays where it was, clamped.
        assertEquals(2, ShelfScene.indexOf(keys, "hidden", 5))
        assertEquals(0, ShelfScene.indexOf(emptyList(), "a", 3))
        assertTrue(ShelfScene.dragRange(1).let { it.start < 0f && it.endInclusive > 0f })
    }

    @Test
    fun `the lock item is one item at the end and never a project`() {
        val ui = ShelfUi(loaded = true, cards = listOf(card("a"), card("b")), lockedGroup = HiddenGroupRow(3))
        val items = ShelfCards.cardItems(ui, wide = false)
        assertEquals(3, items.size)
        assertTrue(items.last() is ShelfCardItem.Lock)
        assertEquals(StarGroup.OTHER, ShelfScene.starGroup(items.last()))
        // Only hidden projects: the lock item alone.
        assertEquals(listOf<Any>("lockCard"), ShelfCards.cardItems(ShelfUi(loaded = true, lockedGroup = HiddenGroupRow(2)), wide = false).map { it.key })
    }

    @Test
    fun `each view's picked-item pair is the board's words`() {
        assertEquals("꺼내기" to "제자리", ShelfScene.pickPair(LibraryStyle.BOOKSHELF)!!.let { it.pick to it.back })
        assertEquals("들어 보기" to "내려놓기", ShelfScene.pickPair(LibraryStyle.MANUSCRIPT_STACK)!!.let { it.pick to it.back })
        assertEquals("꺼내기" to "돌려놓기", ShelfScene.pickPair(LibraryStyle.INDEX_DRAWER)!!.let { it.pick to it.back })
        assertEquals("앞으로" to "돌려놓기", ShelfScene.pickPair(LibraryStyle.CONSTELLATION)!!.let { it.pick to it.back })
        assertEquals("펼치기" to "모으기", ShelfViewCopy.SPREAD to ShelfViewCopy.GATHER)
        assertNull(ShelfScene.pickPair(LibraryStyle.COVER_SPIN))
        assertNull(ShelfScene.pickPair(LibraryStyle.ARCHIVE_HALL))
        assertEquals("이전", ShelfViewCopy.PREV)
        assertEquals("다음", ShelfViewCopy.NEXT)
        assertEquals("3 / 8", ShelfViewCopy.position(2, 8))
        assertEquals(6, ShelfScene.styles.size)
        assertFalse(LibraryStyle.LIST in ShelfScene.styles)
    }

    @Test
    fun `DS-74 tilt maths - relative, clamped, smoothed, at most 8 dp`() {
        // Upright portrait: no lean, pitched 90°.
        assertEquals(0f, TiltMath.lean(TiltMath.screenX(0f, 1f, 0)), 0.01f)
        assertEquals(90f, TiltMath.pitch(TiltMath.screenY(0f, 1f, 0), 0f), 0.01f)
        // Landscape: the device's y axis is the screen's x axis.
        assertEquals(-1f, TiltMath.screenX(0f, 1f, 1), 0f)
        assertEquals(0f, TiltMath.normalized(30f, 30f), 0f)
        assertEquals(0.5f, TiltMath.normalized(36f, 30f), 0.001f)
        assertEquals(1f, TiltMath.normalized(80f, 30f), 0f)
        // Across ±180° the difference wraps: −178 against 176 is 6° on.
        assertEquals(1f, TiltMath.normalized(-170f, 175f), 0f)
        assertEquals(0.5f, TiltMath.normalized(-178f, 176f), 0.001f)
        assertEquals(0.2f, TiltMath.lowPass(0.2f, 1f, 0f), 0f)
        val half = TiltMath.lowPass(0f, 1f, TiltMath.TAU_MS)
        assertEquals(1f - kotlin.math.exp(-1f), half, 0.001f)
        var v = 0f
        repeat(60) { v = TiltMath.lowPass(v, 1f, 16f) }
        assertEquals(1f, v, 0.01f)
        assertEquals(8f, TiltMath.offset(1f, 20f), 0f)
        assertEquals(-4f, TiltMath.offset(-0.5f, 8f), 0f)
        assertEquals(-8f, TiltMath.offset(-3f, 8f), 0f)
    }
}
