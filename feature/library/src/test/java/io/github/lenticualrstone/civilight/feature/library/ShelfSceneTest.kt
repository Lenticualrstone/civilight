package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectCardUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRowUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi
import io.github.lenticualrstone.civilight.feature.library.shelf.views.Pose
import io.github.lenticualrstone.civilight.feature.library.shelf.views.SceneFrame
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ScenePoses
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
import kotlin.math.abs
import kotlin.math.cos

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
    fun `each view's window is its reach, and nothing pops at the window edge`() {
        val pose = Pose()
        for (wide in listOf(false, true)) {
            val cap = ShelfScene.cap(wide)
            for (style in ShelfScene.styles) {
                val reach = ShelfScene.reach(style, wide)
                assertEquals("$style wide=$wide", cap, reach.behind + 1 + reach.ahead)
                assertEquals(reach.behind, ShelfScene.behind(style, wide))
                assertEquals(reach.ahead, ShelfScene.ahead(style, wide))
                // An item enters or leaves the window at |rel| = reach + 0.5 (front flips there): fully faded.
                for (rel in listOf(-(reach.behind + 0.5f), reach.ahead + 0.5f)) {
                    if (style != LibraryStyle.CONSTELLATION) assertEquals("$style wide=$wide rel=$rel", 0f, ScenePoses.edgeAlpha(style, rel, wide), 0f)
                    val size = ShelfScene.size(style, wide)
                    val frame = SceneFrame(if (wide) 560f else 360f, size.frameH, size.itemW, size.itemH)
                    if (style != LibraryStyle.CONSTELLATION) {
                        ScenePoses.place(style, rel, 20, 0f, wide, frame, pose)
                        assertEquals("$style wide=$wide rel=$rel", 0f, pose.alpha, 0f)
                    }
                }
                // 별자리 fades against the window itself: every composed star is lit at rest (also at the list
                // ends and when all items fit); the star leaving and the one entering are 0 at the flip.
                val behind = reach.behind
                for (count in listOf(3, cap, 20)) for (f in 0 until count) {
                    for (i in ShelfScene.window(f, count, reach)) {
                        assertEquals("count=$count f=$f i=$i", 1f, ScenePoses.windowAlpha(i, f.toFloat(), count, behind, cap), 0f)
                    }
                    if (count > cap && f + 1 < count) {
                        val a = ShelfScene.window(f, count, reach)
                        val b = ShelfScene.window(f + 1, count, reach)
                        for (i in (a + b).toSet() - (a.toSet() intersect b.toSet())) {
                            assertEquals("leave/enter count=$count f=$f i=$i", 0f, ScenePoses.windowAlpha(i, f + 0.5f, count, behind, cap), 0f)
                        }
                    }
                }
                // The last composed item at rest is not dimmed by the edge.
                assertEquals(1f, ScenePoses.edgeAlpha(style, reach.ahead.toFloat(), wide), 0f)
                assertEquals(1f, ScenePoses.edgeAlpha(style, -reach.behind.toFloat(), wide), 0f)
            }
        }
        // The pile, the corridor and the drawer keep one item behind and spend the rest ahead.
        val pile = ShelfScene.reach(LibraryStyle.MANUSCRIPT_STACK, false)
        assertEquals(1, pile.behind)
        assertEquals(5, pile.ahead)
        assertEquals(19..25, ShelfScene.window(20, 40, pile))
        assertEquals(0..6, ShelfScene.window(0, 40, pile))
        assertEquals(33..39, ShelfScene.window(39, 40, pile))
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
    fun `each view's picked-item pair is the DS-70 words`() {
        assertEquals("꺼내기" to "제자리", ShelfScene.pickPair(LibraryStyle.BOOKSHELF)!!.let { it.pick to it.back })
        assertEquals("꺼내기" to "돌려놓기", ShelfScene.pickPair(LibraryStyle.MANUSCRIPT_STACK)!!.let { it.pick to it.back })
        assertEquals("꺼내기" to "돌려놓기", ShelfScene.pickPair(LibraryStyle.INDEX_DRAWER)!!.let { it.pick to it.back })
        assertEquals("앞으로" to "제자리", ShelfScene.pickPair(LibraryStyle.CONSTELLATION)!!.let { it.pick to it.back })
        assertNull(ShelfScene.pickPair(LibraryStyle.COVER_SPIN))
        assertNull(ShelfScene.pickPair(LibraryStyle.ARCHIVE_HALL))
        assertEquals("이전", ShelfViewCopy.PREV)
        assertEquals("다음", ShelfViewCopy.NEXT)
        assertEquals("3 / 8", ShelfViewCopy.position(2, 8))
        assertEquals(6, ShelfScene.styles.size)
        assertFalse(LibraryStyle.LIST in ShelfScene.styles)
    }

    /** Horizontal half extent of a placed item (rotation about Y narrows it; a small margin for perspective). */
    private fun halfWidth(style: LibraryStyle, wide: Boolean, frame: SceneFrame, pose: Pose): Float {
        val w = if (style == LibraryStyle.BOOKSHELF && pose.turn < 0.5f) ShelfScene.spineWidth(wide) else frame.itemW
        return w * pose.scale * abs(cos(Math.toRadians(pose.rotY.toDouble()))).toFloat() / 2f * 1.1f
    }

    @Test
    fun `DS-74 frame rule - every visible item stays inside the frame at rest and at full tilt`() {
        val pose = Pose()
        for (style in ShelfScene.styles.filter { it != LibraryStyle.CONSTELLATION }) for ((wide, width) in widths) {
            val size = ShelfScene.size(style, wide)
            val frame = SceneFrame(width, size.frameH, size.itemW, size.itemH)
            val reach = ShelfScene.reach(style, wide)
            var r = -reach.behind - 0.5f
            while (r <= reach.ahead + 0.5f) {
                for (lift in listOf(0f, 1f)) {
                    ScenePoses.place(style, r, (r + 20f).toInt(), if (abs(r) < 0.01f) lift else 0f, wide, frame, pose)
                    if (pose.alpha > 0.02f) {
                        val right = abs(pose.tx) + halfWidth(style, wide, frame, pose) + TiltMath.DEPTH_DP
                        assertTrue("$style wide=$wide rel=$r lift=$lift: x $right > ${width / 2f}", right <= width / 2f)
                        val bottom = abs(pose.ty) + frame.itemH * pose.scale / 2f + TiltMath.DEPTH_DP
                        assertTrue("$style wide=$wide rel=$r: y $bottom > ${size.frameH / 2f}", bottom <= size.frameH / 2f + frame.itemH * 0.1f)
                    }
                }
                r += 0.25f
            }
        }
    }

    @Test
    fun `the item in front faces the viewer and items further away fade`() {
        val pose = Pose()
        for (style in ShelfScene.styles.filter { it != LibraryStyle.CONSTELLATION }) {
            val size = ShelfScene.size(style, false)
            val frame = SceneFrame(360f, size.frameH, size.itemW, size.itemH)
            ScenePoses.place(style, 0f, 0, 0f, false, frame, pose)
            assertEquals("$style front centred", 0f, pose.tx, 0.01f)
            assertEquals("$style front faces", 0f, pose.rotY, 0.01f)
            assertEquals("$style front opaque", 1f, pose.alpha, 0.01f)
            val front = pose.z
            for (rel in -3..3) {
                if (rel == 0) continue
                ScenePoses.place(style, rel.toFloat(), rel + 10, 0f, false, frame, pose)
                if (pose.alpha > 0.02f) assertTrue("$style rel=$rel: the front item is drawn over the others", front > pose.z)
            }
            ScenePoses.place(style, 8f, 8, 0f, false, frame, pose)
            assertEquals("$style: far items are not drawn", 0f, pose.alpha, 0.01f)
        }
    }

    @Test
    fun `taps land only on what is visible`() {
        val scratch = Pose()
        for ((wide, width) in widths) {
            val size = ShelfScene.size(LibraryStyle.BOOKSHELF, wide)
            val frame = SceneFrame(width, size.frameH, size.itemW, size.itemH)
            // A spine's target is no wider than its pitch, so a tap on a neighbour never reaches the front book.
            assertTrue(ScenePoses.hitWidth(LibraryStyle.BOOKSHELF, wide, frame, pulled = false) <= ShelfScene.spineStep(wide))
            assertEquals(frame.itemW, ScenePoses.hitWidth(LibraryStyle.BOOKSHELF, wide, frame, pulled = true), 0f)
        }
        val hall = ShelfScene.size(LibraryStyle.ARCHIVE_HALL, false).let { SceneFrame(360f, it.frameH, it.itemW, it.itemH) }
        // 서고 회랑: a passed item is gone and takes no tap; the ones ahead do.
        assertFalse(ScenePoses.visibleAtRest(LibraryStyle.ARCHIVE_HALL, 2, 3, false, hall, scratch))
        assertTrue(ScenePoses.visibleAtRest(LibraryStyle.ARCHIVE_HALL, 4, 3, false, hall, scratch))
        val stack = ShelfScene.size(LibraryStyle.MANUSCRIPT_STACK, false).let { SceneFrame(360f, it.frameH, it.itemW, it.itemH) }
        assertFalse(ScenePoses.visibleAtRest(LibraryStyle.MANUSCRIPT_STACK, 1, 3, false, stack, scratch))
        assertTrue(ScenePoses.visibleAtRest(LibraryStyle.MANUSCRIPT_STACK, 2, 3, false, stack, scratch))
        assertTrue(ScenePoses.visibleAtRest(LibraryStyle.CONSTELLATION, 0, 5, false, stack, scratch))
    }

    @Test
    fun `3D 책장 - the pulled book turns from spine to cover and the neighbours step aside`() {
        val pose = Pose()
        val size = ShelfScene.size(LibraryStyle.BOOKSHELF, false)
        val frame = SceneFrame(360f, size.frameH, size.itemW, size.itemH)
        ScenePoses.place(LibraryStyle.BOOKSHELF, 0f, 0, 0f, false, frame, pose)
        assertEquals(0f, pose.turn, 0f)
        ScenePoses.place(LibraryStyle.BOOKSHELF, 0f, 0, 1f, false, frame, pose)
        assertEquals(1f, pose.turn, 0f)
        ScenePoses.place(LibraryStyle.BOOKSHELF, 1f, 1, 0f, false, frame, pose)
        val restX = pose.tx
        ScenePoses.place(LibraryStyle.BOOKSHELF, 1f, 1, 1f, false, frame, pose)
        assertTrue(pose.tx > restX)
        assertEquals(0f, pose.turn, 0f)
        assertTrue(ScenePoses.spineShown(0.2f))
        assertFalse(ScenePoses.spineShown(0.8f))
        assertEquals(90f, ScenePoses.spineRotY(0.5f), 0.01f)
        assertEquals(-90f, ScenePoses.coverRotY(0.5f), 0.01f)
        assertEquals(0f, ScenePoses.coverRotY(1f), 0.01f)
        assertEquals(0f, ScenePoses.spineRotY(0f), 0.01f)
    }

    @Test
    fun `별자리 - clusters by 형식 in order, lock and new apart, stars inside the frame`() {
        val items = listOf(
            ShelfCardItem.Project(card("a", ProjectFormat.SHORT)),
            ShelfCardItem.Project(card("b", ProjectFormat.SERIAL)),
            ShelfCardItem.Project(card("c", null)),
            ShelfCardItem.Project(card("d", ProjectFormat.SETTING_ONLY)),
            ShelfCardItem.Project(card("e", ProjectFormat.SHORT)),
            ShelfCardItem.Lock(HiddenGroupRow(2)),
            ShelfCardItem.NewProject,
        )
        val layout = ShelfScene.stars(items.map(ShelfScene::starGroup))
        assertEquals(listOf(StarGroup.SERIAL, StarGroup.SHORT, StarGroup.SETTING_ONLY, StarGroup.OTHER), layout.groups)
        // A loading project (no format yet) sits with 연재 장편, the model default.
        assertEquals(listOf(1, 0, 0, 2, 1, 3, 3), layout.slots.map { it.group })
        assertEquals(listOf(0, 0, 1, 0, 1, 0, 1), layout.slots.map { it.slot })
        assertEquals(listOf(2, 2, 2, 1, 2, 2, 2), layout.slots.map { it.slotCount })
        assertEquals(listOf("연재 장편", "단편", "설정만", null), layout.groups.map(ShelfScene::groupLabel))
        val pose = Pose()
        for ((wide, width) in widths) {
            val size = ShelfScene.size(LibraryStyle.CONSTELLATION, wide)
            val frame = SceneFrame(width, size.frameH, size.itemW, size.itemH)
            for (orbit in listOf(0f, 0.13f, 0.25f, 0.5f, 0.77f)) for (slot in layout.slots) for ((focus, lift) in listOf(0f to 0f, 1f to 0f, 1f to 1f)) {
                ScenePoses.star(slot, layout.groups.size, orbit, focus, lift, frame, pose)
                val right = abs(pose.tx) + frame.itemW * pose.scale / 2f + TiltMath.DEPTH_DP
                assertTrue("star $slot wide=$wide orbit=$orbit: $right", right <= width / 2f)
                val bottom = abs(pose.ty) + frame.itemH * pose.scale / 2f + TiltMath.DEPTH_DP
                assertTrue("star $slot wide=$wide: $bottom", bottom <= size.frameH / 2f)
            }
        }
        // The layout is over all items (`Scene` keys it on the items and indexes it by item), so it has one slot per item.
        assertEquals(items.size, layout.slots.size)
        // `앞으로` brings the star to the middle, in front of every other.
        val frame = SceneFrame(360f, 290f, 44f, 59f)
        ScenePoses.star(layout.slots[0], 4, 0.3f, 1f, 1f, frame, pose)
        assertEquals(0f, pose.tx, 0.01f)
        assertTrue(pose.z > 3f)
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
