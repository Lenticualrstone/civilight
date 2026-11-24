package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.list.CastCardViews
import io.github.lenticualrstone.civilight.feature.cast.list.CastListModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListUi
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardBackModel
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardPoses
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardXform
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.Projector
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.Scene
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.TurnOrder
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
        // The ring draws its twelve places at most; the wall the four rows in view and one each side.
        assertEquals(12, CardPoses.ringWindow(100, 200).size)
        assertEquals(6, CardPoses.ringWindow(0, 6).size)
        assertTrue(CardPoses.wallWindow(10, 200).size <= (CardPoses.WALL_ROWS + 2) * CardPoses.WALL_COLS)
        assertTrue(30 in CardPoses.wallWindow(10, 200))
    }

    @Test
    fun `focus rests on the nearest card, clamps or loops, and the shortest way round is taken`() {
        assertEquals(2, CardPoses.indexAt(2.4f, 5, loop = false))
        assertEquals(4, CardPoses.indexAt(7.2f, 5, loop = false))
        assertEquals(2, CardPoses.indexAt(7.2f, 5, loop = true))
        assertEquals(4, CardPoses.indexAt(-1f, 5, loop = true))
        assertEquals(-1f, CardPoses.nearestPos(0f, 9, 10, loop = true), 0f)
        assertEquals(9f, CardPoses.nearestPos(0f, 9, 10, loop = false), 0f)
        assertEquals(-2f, CardPoses.wrap(8f, 10), 1e-4f)
        assertEquals(1.5f, CardPoses.offset(1, 9.5f, 10, loop = true), 1e-4f)
    }

    // --- board geometry (golden values from the generators' own deck_t, ring_t, sslot, tslot, tdeck, slot_xy) ---

    @Test
    fun `fan matches board deck_t`() {
        val c = CardPoses.fan(0f)!!
        assertEquals(0f, c.x, 1e-3f)
        assertEquals(-30f, c.y, 1e-3f)
        assertEquals(40f, c.z, 1e-3f)
        // deck_t(1): translate3d(81.3px, 6.4px, −36px) rotateZ(9deg) rotateY(−7deg) scale(.955), dim .16
        val r = CardPoses.fan(1f)!!
        assertEquals(81.3f, r.x, 0.05f)
        assertEquals(6.4f, r.y, 0.05f)
        assertEquals(-36f, r.z, 1e-3f)
        assertEquals(9f, r.rotZ, 1e-3f)
        assertEquals(-7f, r.rotY, 1e-3f)
        assertEquals(0.955f, r.scale, 1e-3f)
        assertEquals(0.16f, r.dim, 1e-3f)
        // deck_t(−2.5): x −199.0, y 39.6, z −90, scale .887, dim .4
        val l = CardPoses.fan(-2.5f)!!
        assertEquals(-199.0f, l.x, 0.05f)
        assertEquals(39.6f, l.y, 0.05f)
        assertEquals(-90f, l.z, 1e-3f)
        assertEquals(0.4f, l.dim, 1e-3f)
        assertNull(CardPoses.fan(3.6f))
        // deck_t(0, flip .5): the turning card lifts to z 170.
        assertEquals(170f, CardPoses.fan(0f, 0.5f)!!.z, 1e-3f)
        assertEquals(c.z, CardPoses.fan(0f, 1f)!!.z, 1e-3f)
    }

    @Test
    fun `ring matches board ring_t and shows every card`() {
        assertEquals(200f, CardPoses.ringRadius(6), 1e-3f)
        // ring_t(1, 60°, 200): translate3d(173.2px, 0, −100px) rotateY(60deg) scale(.875), dim .2
        val side = CardPoses.ring(1f, 6)
        assertEquals(173.2f, side.x, 0.05f)
        assertEquals(-100f, side.z, 0.05f)
        assertEquals(60f, side.rotY, 1e-3f)
        assertEquals(0.875f, side.scale, 1e-3f)
        assertEquals(0.2f, side.dim, 1e-3f)
        val front = CardPoses.ring(0f, 6)
        assertEquals(1.12f, front.scale, 1e-3f)
        // ring_t(3): straight behind, −400 deep, turned −180°.
        val behind = CardPoses.ring(3f, 6)
        assertEquals(-400f, behind.z, 0.05f)
        assertEquals(0.8f, behind.dim, 1e-3f)
        assertEquals(12, CardPoses.ringSlots(200))
        assertEquals(3, CardPoses.ringSlots(1))
        assertTrue(CardPoses.ringRadius(12) >= 200f)
    }

    @Test
    fun `pile matches board SLOT, throws along the arc and slides back under`() {
        assertEquals(0f, stackOffset(3, 3f, 10), 1e-4f)
        assertEquals(9f, stackOffset(2, 3f, 10), 1e-4f)
        assertEquals(-0.5f, stackOffset(3, 3.5f, 10), 1e-4f)
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8), stackWindow(3, 10))
        assertEquals(listOf(1, 0), stackWindow(0, 2))
        // sslot(2): y 39, z −60, rotZ 1.9 (deeper cards sit lower and peek out under the top card).
        val s2 = CardPoses.stack(2f)!!
        assertEquals(39f, s2.y, 1e-3f)
        assertEquals(-60f, s2.z, 1e-3f)
        assertEquals(1.9f, s2.rotZ, 1e-3f)
        assertEquals(0.144f, s2.dim, 1e-3f)
        // Thrown by the buttons (dir −1): off to the left, lifted, turning; at the end of the arc (dir·540, −50, 70).
        val mid = CardPoses.stack(-0.25f, -1f)!!
        assertTrue(mid.x < 0f && mid.z > 0f)
        val end = CardPoses.stack(-0.5f, 1f)!!
        assertEquals(540f, end.x, 1e-2f)
        assertEquals(-50f, end.y, 1e-2f)
        assertEquals(70f, end.z, 1e-2f)
        // Then back from behind (dir·340, 90, −330) into the bottom slot.
        val back = CardPoses.stack(-0.5001f, 1f, 6)!!
        assertEquals(340f, back.x, 0.5f)
        assertEquals(-330f, back.z, 0.5f)
        assertEquals(-150f, CardPoses.stack(-0.9999f, 1f, 6)!!.z, 0.5f)
        // A pile longer than six: the returning card fades as it goes under.
        assertTrue(CardPoses.stack(-0.9f, 1f, 10)!!.alpha < 0.3f)
        assertNull(CardPoses.stack(9f))
        // Drag: 110 px or a 0.55 px/ms flick in the drag's direction throws.
        assertTrue(CardPoses.pileThrows(120f, 0f))
        assertTrue(CardPoses.pileThrows(40f, 0.8f))
        assertFalse(CardPoses.pileThrows(40f, -0.8f))
        assertEquals(0.5f, CardPoses.pileRise(400f), 1e-4f)
    }

    @Test
    fun `tarot matches board tslot and tdeck`() {
        assertEquals(0 until 5, CardPoses.hand(0, 12))
        assertEquals(10 until 12, CardPoses.hand(2, 12))
        assertEquals(2, CardPoses.handOf(11))
        // tslot(0): (−110.25, 9.50, 1, −16°, 0)
        val s0 = CardPoses.tarot(0, 5, focus = false)
        assertEquals(-110.25f, s0.x, 0.01f)
        assertEquals(9.50f, s0.y, 0.01f)
        assertEquals(1f, s0.z, 1e-4f)
        assertEquals(-16f, s0.rotZ, 1e-4f)
        // tslot(2, focus): (0, −14, 16, 0, −12)
        val f2 = CardPoses.tarot(2, 5, focus = true)
        assertEquals(-14f, f2.y, 1e-3f)
        assertEquals(16f, f2.z, 1e-3f)
        assertEquals(-12f, f2.rotX, 1e-3f)
        // tdeck(3): (0, 84, 8.5, 2.5°)
        val d3 = CardPoses.tarotDeck(3)
        assertEquals(84f, d3.y, 1e-4f)
        assertEquals(8.5f, d3.z, 1e-4f)
        assertEquals(2.5f, d3.rotZ, 1e-4f)
        // A short hand stays centred on the same 8° steps.
        assertEquals(-CardPoses.tarot(0, 2, false).x, CardPoses.tarot(1, 2, false).x, 1e-3f)
    }

    @Test
    fun `wall matches board slot_xy and turns with the tilt`() {
        // slot_xy(4) = (103, 134).
        assertEquals(103f, CardPoses.wallSlotX(4), 1e-4f)
        assertEquals(134f, CardPoses.wallSlotY(CardPoses.wallRow(4).toFloat()), 1e-4f)
        assertEquals(12f, CardPoses.wallRotX(0f), 1e-4f)
        assertEquals(-24f, CardPoses.wallRotY(0f), 1e-4f)
        assertEquals(4f, CardPoses.wallRotX(1f), 1e-4f)
        assertEquals(-12f, CardPoses.wallRotY(1f), 1e-4f)
        assertEquals(0, CardPoses.wallTopRow(0, 10))
        assertEquals(4, CardPoses.wallTopRow(5, 10))
        assertEquals(7, CardPoses.wallTopRow(10, 10))
        assertEquals(0, CardPoses.wallTopRow(3, 2))
    }

    @Test
    fun `a wall drag moves by rows, a short flick still moves one, and lands on the same column`() {
        assertEquals(-CardPoses.WALL_DRAG_OVER, CardPoses.wallDragRow(-3f, 4), 1e-4f)
        assertEquals(4 + CardPoses.WALL_DRAG_OVER, CardPoses.wallDragRow(9f, 4), 1e-4f)
        assertEquals(1.3f, CardPoses.wallDragRow(1.3f, 4), 1e-4f)
        assertEquals(2, CardPoses.wallDragTarget(1.2f, 1f, 4))
        assertEquals(0, CardPoses.wallDragTarget(0.8f, -1f, 4))
        assertEquals(1, CardPoses.wallDragTarget(1.0f, 0f, 4))
        assertEquals(4, CardPoses.wallDragTarget(4.3f, 1f, 4))
        assertEquals(0, CardPoses.wallDragTarget(-0.4f, -1f, 4))
        assertEquals(7, CardPoses.wallIndex(2, 1, 12))
        assertEquals(7, CardPoses.wallIndex(2, 2, 8))
        assertEquals(0, CardPoses.wallIndex(0, 0, 0))
    }

    @Test
    fun `a drag moves the deck at once, rubber-bands past the ends and a fling goes at most two cards`() {
        val deck = Deck(loop = false, scope = kotlinx.coroutines.CoroutineScope(kotlin.coroutines.EmptyCoroutineContext), size = 5, start = 0)
        deck.dragBy(1.2f)
        assertEquals(1.2f, deck.pos, 1e-4f)
        assertEquals(1, deck.index)
        assertEquals(1, deck.targetIndex)
        // Board rubber band: a third of the pull past the end.
        deck.dragBy(-9f)
        assertEquals(-7.8f * CardPoses.RUBBER, deck.pos, 1e-4f)
        deck.snap(3)
        assertEquals(3f, deck.pos, 1e-4f)
        assertEquals(4, CardPoses.flingTarget(2.4f, 10f, 10, loop = false))
        assertEquals(4, CardPoses.flingTarget(2.4f, 300f, 10, loop = false))
        assertEquals(9, CardPoses.flingTarget(8.6f, 30f, 10, loop = false))
        assertEquals(11, CardPoses.flingTarget(8.6f, 30f, 10, loop = true))
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
            // A pile card in flight fades at the stage's edge but stays in the window until it lands.
            assertEquals("stack $p", CardPoses.stack(p) != null && (p > -1f && p < 6f), CardPoses.stackShows(p))
            assertEquals("rolodex $p", CardPoses.rolodex(p)?.let { it.alpha > 0f } == true, CardPoses.rolodexShows(p))
            p += 0.05f
        }
    }

    @Test
    fun `rolodex matches board d0 to d4 and a passed card falls towards the viewer`() {
        val front = CardPoses.rolodex(0f)!!
        assertEquals(0f, front.rotX, 1e-4f)
        assertEquals(1f, front.alpha, 1e-4f)
        // .d1: translate3d(0, −40px, −46px) rotateX(9deg); .d3: (0, −102, −138) rotateX(22deg) opacity .8
        val d1 = CardPoses.rolodex(1f)!!
        assertEquals(-40f, d1.y, 1e-4f)
        assertEquals(-46f, d1.z, 1e-4f)
        assertEquals(9f, d1.rotX, 1e-4f)
        val d3 = CardPoses.rolodex(3f)!!
        assertEquals(-102f, d3.y, 1e-4f)
        assertEquals(22f, d3.rotX, 1e-4f)
        assertEquals(0.8f, d3.alpha, 1e-4f)
        // .dp: (0, 70, 40) rotateX(−104deg), faded by the end.
        val passed = CardPoses.rolodex(-0.5f)!!
        assertEquals(35f, passed.y, 1e-3f)
        assertEquals(-52f, passed.rotX, 1e-3f)
        assertTrue(CardPoses.rolodex(-0.9f)!!.alpha < 0.5f)
        assertNull(CardPoses.rolodex(5f))
        // Behind the front card only paper and tab show; the next one shows its content as the front falls.
        assertEquals(1f, CardPoses.rolodexContent(0f), 1e-4f)
        assertEquals(0f, CardPoses.rolodexContent(1f), 1e-4f)
        assertEquals(0.5f, CardPoses.rolodexContent(0.5f), 1e-4f)
    }

    // --- shared perspective (Projector) ---

    private val flat = Scene(1000f, 195f, 230f, 195f, 250f)

    @Test
    fun `projection divides by depth from the perspective origin`() {
        val xf = CardXform()
        val pr = Projector()
        pr.solve(xf, flat, 0f, 0f, 195f, 250f, 0f, 0f, 0f)
        assertEquals(0f, xf.translationX, 1e-4f)
        assertEquals(0f, xf.translationY, 1e-4f)
        assertEquals(1f, xf.scale, 1e-5f)
        // Board deck story: the centre card lifted 30 px and 40 px nearer reads ×1.042.
        pr.solve(xf, flat, 0f, 0f, 195f, 250f, 0f, -30f, 40f)
        assertEquals(1000f / 960f, xf.scale, 1e-4f)
        assertEquals(230f + (220f - 230f) * 1000f / 960f - 250f, xf.translationY, 1e-3f)
        // A card to the right moves further right as it comes nearer.
        pr.solve(xf, flat, 0f, 0f, 195f, 250f, 100f, 0f, 100f)
        assertEquals(100f * 1000f / 900f, xf.translationX, 1e-3f)
        assertEquals(38.194f, Projector.cameraDistance(1000f, 2.75f), 1e-3f)
    }

    @Test
    fun `the rig turn moves positions and combines with the card turn`() {
        val xf = CardXform()
        val pr = Projector()
        val ring = Scene(900f, 195f, 150f, 195f, 250f)
        // Seen from above (rotateX −15°): the far side of the ring rises on screen.
        pr.solve(xf, ring, -15f, 0f, 195f, 280f, 0f, 0f, -400f)
        val far = xf.translationY
        pr.solve(xf, ring, -15f, 0f, 195f, 280f, 0f, 0f, 0f)
        assertTrue(far < xf.translationY)
        // Rig X then card Y stays the same two angles (the layer's own X·Y·Z order).
        pr.solve(xf, ring, -15f, 0f, 195f, 280f, 0f, 0f, 0f, rotY = 60f)
        assertEquals(-15f, xf.rotationX, 1e-3f)
        assertEquals(60f, xf.rotationY, 1e-3f)
        assertEquals(0f, xf.rotationZ, 1e-3f)
        assertFalse(xf.backFacing)
        // A deck card: rotateZ then rotateY, rebuilt exactly as X·Y·Z angles.
        pr.solve(xf, flat, 3f, -6f, 195f, 250f, 0f, 0f, 0f, rotY = 14f, rotZ = -18f, order = TurnOrder.ZY)
        assertMatrix(product(rx(3f), ry(-6f), rz(-18f), ry(14f)), product(rx(xf.rotationX), ry(xf.rotationY), rz(xf.rotationZ)))
        // Turned past 90° (behind the ring, or flipped) the back faces the viewer.
        pr.solve(xf, ring, -15f, 0f, 195f, 280f, 0f, 0f, -400f, rotY = -180f)
        assertTrue(xf.backFacing)
        pr.solve(xf, flat, 0f, 0f, 195f, 250f, 0f, 0f, 0f, order = TurnOrder.ZY, flipY = 180f)
        assertTrue(xf.backFacing)
        pr.solve(xf, flat, 0f, 0f, 195f, 250f, 0f, 0f, 0f, order = TurnOrder.ZY, flipY = 60f)
        assertFalse(xf.backFacing)
    }

    @Test
    fun `the tarot table turns Y before X and its face-up cards show their front`() {
        val xf = CardXform()
        val pr = Projector()
        val table = Scene(640f, 195f, 79.2f, 195f, 150f, rigFirstY = true)
        pr.solve(xf, table, 50f, 6f, 195f, 198f, 0f, 0f, 1f, rotZ = 8f, order = TurnOrder.ZX)
        assertMatrix(product(ry(6f), rx(50f), rz(8f)), product(rx(xf.rotationX), ry(xf.rotationY), rz(xf.rotationZ)))
        assertFalse(xf.backFacing)
        pr.solve(xf, table, 50f, 0f, 195f, 198f, 0f, 0f, 1f, order = TurnOrder.ZX, flipY = 180f)
        assertTrue(xf.backFacing)
    }

    private fun rx(d: Float): DoubleArray { val r = Math.toRadians(d.toDouble()); val c = Math.cos(r); val s = Math.sin(r); return doubleArrayOf(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c) }
    private fun ry(d: Float): DoubleArray { val r = Math.toRadians(d.toDouble()); val c = Math.cos(r); val s = Math.sin(r); return doubleArrayOf(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c) }
    private fun rz(d: Float): DoubleArray { val r = Math.toRadians(d.toDouble()); val c = Math.cos(r); val s = Math.sin(r); return doubleArrayOf(c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0) }
    private fun product(vararg m: DoubleArray): DoubleArray = m.reduce { a, b -> DoubleArray(9) { i -> (0 until 3).sumOf { k -> a[i / 3 * 3 + k] * b[k * 3 + i % 3] } } }
    private fun assertMatrix(expected: DoubleArray, actual: DoubleArray) {
        for (i in 0 until 9) assertEquals("m$i", expected[i], actual[i], 1e-4)
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
    fun `a card carries its live tag names in tag order for the chips`() {
        for (c in doc.cast.characters.filter { it.deletedAt == null }) {
            val card = CastList.card(doc, c, doc.castLens)
            assertEquals(CastList.groupTags(doc, c).map { it.name }, card.tags)
        }
    }

    @Test
    fun `card back has the one-liner and at most three public fields at the lens`() {
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
