package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.list.CastCardViews
import io.github.lenticualrstone.civilight.feature.cast.list.CastListModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListUi
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardBackModel
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardPoses
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.Deck
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.DeckItem
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.stackOffset
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.stackWindow
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** U-16 / CST-67 card views: layout math, focus window, card back, hidden rules and wiring (JVM). */
class Cards3dTest {
    private val doc = CastFx.doc()

    // --- focus and window ---

    @Test
    fun `the window draws seven cards at most whatever the cast size`() {
        assertEquals((0..3).toList(), CardPoses.window(0, 200, loop = false))
        assertEquals((97..103).toList(), CardPoses.window(100, 200, loop = false))
        assertEquals(7, CardPoses.window(100, 200, loop = true).size)
        assertEquals(listOf(197, 198, 199, 0, 1, 2, 3), CardPoses.window(0, 200, loop = true))
        assertEquals((0 until 5).toList(), CardPoses.window(2, 5, loop = true))
        assertTrue(CardPoses.window(0, 0, loop = true).isEmpty())
        // The wall draws five rows around the focus row, never the whole cast.
        assertTrue(CardPoses.wallWindow(100, 200).size <= 5 * CardPoses.WALL_COLS)
        assertTrue(100 in CardPoses.wallWindow(100, 200))
    }

    @Test
    fun `focus rests on the nearest card, clamps or loops, and the shortest way round is taken`() {
        assertEquals(2, CardPoses.indexAt(2.4f, 5, loop = false))
        assertEquals(4, CardPoses.indexAt(7.2f, 5, loop = false))
        assertEquals(2, CardPoses.indexAt(7.2f, 5, loop = true))
        assertEquals(4, CardPoses.indexAt(-1f, 5, loop = true))
        // From card 0 of 10 in a loop, card 9 is one step back, not nine forward.
        assertEquals(-1f, CardPoses.nearestPos(0f, 9, 10, loop = true), 0f)
        assertEquals(9f, CardPoses.nearestPos(0f, 9, 10, loop = false), 0f)
        assertEquals(-2f, CardPoses.wrap(8f, 10), 1e-4f)
        assertEquals(1.5f, CardPoses.offset(1, 9.5f, 10, loop = true), 1e-4f)
    }

    // --- poses (board math) ---

    @Test
    fun `fan centre card is front and lifted, neighbours mirror, far cards are not drawn`() {
        val c = CardPoses.fan(0f)!!
        val l = CardPoses.fan(-1f)!!
        val r = CardPoses.fan(1f)!!
        assertTrue(c.z > l.z && c.z > r.z)
        assertEquals(-l.x, r.x, 1e-3f)
        assertEquals(-l.rotZ, r.rotZ, 1e-3f)
        assertEquals(0f, c.dim, 1e-4f)
        assertNull(CardPoses.fan(3.6f))
        // Turning lifts the centre card towards the viewer mid-way.
        assertTrue(CardPoses.fan(0f, 0.5f)!!.z > c.z)
        assertEquals(c.z, CardPoses.fan(0f, 1f)!!.z, 1e-3f)
    }

    @Test
    fun `ring front card is the largest and a card behind the ring is not drawn`() {
        val slots = CardPoses.ringSlots(7)
        val front = CardPoses.ring(0f, slots)!!
        val side = CardPoses.ring(1f, slots)!!
        assertTrue(front.projected > side.projected)
        assertNull(CardPoses.ring(3.5f, slots))
        // Past 90° a card would read mirrored (no backface culling): it is gone by then and fades before.
        assertNull(CardPoses.ring(2f, slots))
        assertTrue(CardPoses.ring(1.6f, slots)!!.alpha < 1f)
        assertEquals(12, CardPoses.ringSlots(200))
        assertEquals(3, CardPoses.ringSlots(1))
        assertTrue(CardPoses.ringRadius(12) >= 200f)
    }

    @Test
    fun `pile keeps six cards, the flicked one arcs off and returns under the pile`() {
        assertEquals(0f, stackOffset(3, 3f, 10), 1e-4f)
        assertEquals(9f, stackOffset(2, 3f, 10), 1e-4f)
        assertEquals(-0.5f, stackOffset(3, 3.5f, 10), 1e-4f)
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8), stackWindow(3, 10))
        assertEquals(listOf(1, 0), stackWindow(0, 2))
        val top = CardPoses.stack(0f)!!
        val under = CardPoses.stack(1f)!!
        assertTrue(top.z > under.z && under.dim > 0f)
        val flying = CardPoses.stack(-0.5f)!!
        assertTrue(flying.x > 0f && flying.alpha < 1f)
        assertNull(CardPoses.stack(9f))
    }

    @Test
    fun `tarot deals five to a hand, centres a short hand and raises the focus`() {
        assertEquals(0 until 5, CardPoses.hand(0, 12))
        assertEquals(10 until 12, CardPoses.hand(2, 12))
        assertEquals(2, CardPoses.handOf(11))
        assertEquals(0f, CardPoses.tarot(2, 5, focus = false).x, 1e-3f)
        assertEquals(-CardPoses.tarot(0, 2, false).x, CardPoses.tarot(1, 2, false).x, 1e-3f)
        assertTrue(CardPoses.tarot(1, 5, focus = true).z > CardPoses.tarot(1, 5, focus = false).z)
        // Before the deal the card waits in the deck below the arc.
        assertTrue(CardPoses.tarot(0, 5, false, deal = 0f).y > CardPoses.tarot(0, 5, false).y)
    }

    @Test
    fun `wall turns with the tilt and the light band follows it`() {
        assertEquals(12f, CardPoses.wallRotX(0f), 1e-4f)
        assertEquals(-24f, CardPoses.wallRotY(0f), 1e-4f)
        assertEquals(4f, CardPoses.wallRotX(1f), 1e-4f)
        assertEquals(-12f, CardPoses.wallRotY(1f), 1e-4f)
        assertTrue(CardPoses.sheen(1f) > CardPoses.sheen(-1f))
    }

    @Test
    fun `a wall drag moves by rows, a short flick still moves one, and lands on the same column`() {
        // Held near the ends while dragging.
        assertEquals(-CardPoses.WALL_DRAG_OVER, CardPoses.wallDragRow(-3f, 4), 1e-4f)
        assertEquals(4 + CardPoses.WALL_DRAG_OVER, CardPoses.wallDragRow(9f, 4), 1e-4f)
        assertEquals(1.3f, CardPoses.wallDragRow(1.3f, 4), 1e-4f)
        // A short flick up from row 1 moves one row; a drag that came back stays.
        assertEquals(2, CardPoses.wallDragTarget(1.2f, 1f, 4))
        assertEquals(0, CardPoses.wallDragTarget(0.8f, -1f, 4))
        assertEquals(1, CardPoses.wallDragTarget(1.0f, 0f, 4))
        assertEquals(4, CardPoses.wallDragTarget(4.3f, 1f, 4))
        assertEquals(0, CardPoses.wallDragTarget(-0.4f, -1f, 4))
        // Same column in the new row, or the last card of a short row.
        assertEquals(7, CardPoses.wallIndex(2, 1, 12))
        assertEquals(7, CardPoses.wallIndex(2, 2, 8))
        assertEquals(0, CardPoses.wallIndex(0, 0, 0))
    }

    @Test
    fun `a drag moves the deck at once without a coroutine and clamps past the ends`() {
        val deck = Deck(loop = false, scope = kotlinx.coroutines.CoroutineScope(kotlin.coroutines.EmptyCoroutineContext), size = 5, start = 0)
        deck.dragBy(1.2f)
        assertEquals(1.2f, deck.pos, 1e-4f)
        assertEquals(1, deck.index)
        assertEquals(1, deck.targetIndex)
        deck.dragBy(-9f)
        assertEquals(-Deck.DRAG_OVER, deck.pos, 1e-4f)
        deck.snap(3)
        assertEquals(3f, deck.pos, 1e-4f)
        // Reduced motion: a drag moves nothing until the release, which steps one card.
        deck.reduced = true
        deck.dragBy(0.5f)
        assertEquals(3f, deck.pos, 1e-4f)
        deck.endDrag(1f)
        assertEquals(4, deck.index)
        assertFalse(deck.settling)
    }

    @Test
    fun `a narrowed cast moves a clamped deck onto the kept card`() {
        val scope = kotlinx.coroutines.CoroutineScope(kotlin.coroutines.EmptyCoroutineContext)
        val fan = Deck(loop = false, scope = scope, size = 8, start = 7)
        fan.resize(5, 4)
        assertEquals(4f, fan.pos, 1e-4f)
        assertEquals(4, fan.targetIndex)
        // Already on the kept card: nothing moves.
        fan.resize(5, 4)
        assertEquals(4f, fan.pos, 1e-4f)
        val ring = Deck(loop = true, scope = scope, size = 10, start = 13)
        ring.resize(8, 3)
        assertEquals(3, ring.index)
    }

    @Test
    fun `a card is tappable and read out only while it can be seen`() {
        var p = -5f
        while (p <= 7f) {
            assertEquals("fan $p", CardPoses.fan(p) != null, CardPoses.fanShows(p))
            assertEquals("stack $p", CardPoses.stack(p)?.let { it.alpha > 0f } == true, CardPoses.stackShows(p))
            assertEquals("rolodex $p", CardPoses.rolodex(p)?.let { it.alpha > 0f } == true, CardPoses.rolodexShows(p))
            for (slots in listOf(3, 4, 7, 12)) {
                assertEquals("ring $p/$slots", CardPoses.ring(p, slots)?.let { it.alpha > 0f } == true, CardPoses.ringShows(p, slots))
            }
            p += 0.05f
        }
        // A small ring: the neighbour behind at rest still comes round during a drag.
        assertFalse(CardPoses.ringShows(1f, 4))
        assertTrue(CardPoses.ringShows(0.6f, 4))
    }

    @Test
    fun `rolodex front card is level, the next ones wait above, a passed one flips down and fades`() {
        val front = CardPoses.rolodex(0f)!!
        assertEquals(0f, front.rotX, 1e-4f)
        assertTrue(CardPoses.rolodex(1f)!!.y < 0f)
        val passed = CardPoses.rolodex(-0.5f)!!
        assertTrue(passed.rotX < 0f && passed.alpha < 1f)
        assertNull(CardPoses.rolodex(5f))
    }

    @Test
    fun `초성 index row lists each letter once with its first card`() {
        assertEquals("ㄹ", CardPoses.indexLetter("리엔"))
        assertEquals("#", CardPoses.indexLetter("???"))
        assertEquals("A", CardPoses.indexLetter("ash"))
        assertEquals(listOf("ㄹ" to 0, "ㅎ" to 1, "#" to 3), CardPoses.indexRow(listOf("리엔", "하르트", "로아", "???")))
    }

    // --- card back ---

    @Test
    fun `card back has the one-liner and at most two public fields at the lens`() {
        val back = CardBackModel.of(doc, K.RIEN, doc.castLens)!!
        val card = CastList.card(doc, doc.index.character(K.RIEN)!!, doc.castLens)
        assertEquals(card.title, back.title)
        assertTrue(back.fields.size <= CardBackModel.FIELDS)
        val publicNames = Profile.publicDefs(doc, K.RIEN).map { it.name }.toSet()
        assertTrue(back.fields.all { it.label in publicNames })
        assertTrue(back.fields.all { it.value.isNotBlank() })
        val rien = doc.index.character(K.RIEN)!!
        rien.oneLiner?.takeIf { it.isNotBlank() }?.let { assertEquals(it, back.line) }
    }

    @Test
    fun `card back keeps the display-name rule of the front`() {
        val card = CastList.card(doc, doc.index.character(K.HART)!!, doc.castLens)
        val back = CardBackModel.of(doc, K.HART, doc.castLens)!!
        assertEquals(card.title, back.title)
        if (card.title != doc.index.character(K.HART)!!.name) assertTrue(back.line.orEmpty().contains("이름 공개"))
        assertNull(CardBackModel.of(doc, "missing", doc.castLens))
    }

    // --- hidden rules ---

    @Test
    fun `locked hidden characters are never cards, the lock card comes last and names nobody`() {
        val live = CastFx.noahLive(doc)
        val hidden = live.copy(cast = live.cast.copy(characters = live.cast.characters.map { if (it.key == K.NOAH) it.copy(hidden = true) else it }))
        val locked = CastListModel.build(hidden, CastListUi(), hiddenUnlocked = false, readOnly = false)
        val cards = locked.view.rows.distinctBy { it.key }
        assertFalse(cards.any { it.key == K.NOAH })
        assertNotNull(locked.view.lockRow)
        val lock = locked.view.lockRow!!
        val items = DeckItem.of(cards.map { CastList.card(hidden, hidden.index.character(it.key)!!, hidden.castLens) }, lock)
        assertTrue(items.last() is DeckItem.Lock)
        val noah = hidden.index.character(K.NOAH)!!.name
        assertFalse(lock.title.contains(noah) || lock.sub.contains(noah))
        // Each character once, even under several tags.
        assertEquals(items.size, items.map { it.key }.toSet().size)
        // Unlocked: Noah is a card and no lock card is left.
        val open = CastListModel.build(hidden, CastListUi(), hiddenUnlocked = true, readOnly = false)
        assertTrue(open.view.rows.any { it.key == K.NOAH })
        assertNull(open.view.lockRow)
    }

    // --- wiring ---

    @Test
    fun `every 3D style is drawn, the toggle switches to the list, LIST has no toggle`() {
        val s = AppSettings()
        for (style in CardStyle.entries.filterNot { it.isList }) {
            val chosen = s.copy(castCardStyle = style)
            assertEquals(style, CastCardViews.shown(chosen))
            assertTrue(CastCardViews.showsToggle(chosen))
            assertEquals(CardStyle.LIST, CastCardViews.shown(chosen.copy(castToggle = ViewMode.LIST)))
            assertEquals(style, CastCardViews.shown(chosen.copy(castToggle = ViewMode.CARD)))
        }
        assertFalse(CastCardViews.showsToggle(s.copy(castCardStyle = CardStyle.LIST)))
    }

    @Test
    fun `card views use motion tokens only and every icon button is named`() {
        val dir = File("src/main/java/io/github/lenticualrstone/civilight/feature/cast/list/cards3d")
        assertTrue(dir.isDirectory)
        val motion = io.github.lenticualrstone.civilight.designsystem.theme.MotionLint.lintModule(File("."))
        assertEquals(io.github.lenticualrstone.civilight.designsystem.theme.MotionLint.report(motion), 0, motion.size)
        val icons = io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint.lintDir(dir)
        assertEquals(io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint.report(icons), 0, icons.size)
        // No literal spring outside the token constants.
        val springs = dir.walkTopDown().filter { it.extension == "kt" }.flatMap { f -> f.readLines().filter { Regex("""\bspring\s*\(\s*[0-9]""").containsMatchIn(it) } }.toList()
        assertTrue(springs.toString(), springs.isEmpty())
    }
}
