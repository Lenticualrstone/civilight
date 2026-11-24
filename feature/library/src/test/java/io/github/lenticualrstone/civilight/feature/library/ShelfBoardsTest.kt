package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectCardUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRowUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.views.BookcaseGeo
import io.github.lenticualrstone.civilight.feature.library.shelf.views.Css
import io.github.lenticualrstone.civilight.feature.library.shelf.views.DeskGeo
import io.github.lenticualrstone.civilight.feature.library.shelf.views.DrawerGeo
import io.github.lenticualrstone.civilight.feature.library.shelf.views.FlowGeo
import io.github.lenticualrstone.civilight.feature.library.shelf.views.HallGeo
import io.github.lenticualrstone.civilight.feature.library.shelf.views.IdleSway
import io.github.lenticualrstone.civilight.feature.library.shelf.views.M4
import io.github.lenticualrstone.civilight.feature.library.shelf.views.Persp
import io.github.lenticualrstone.civilight.feature.library.shelf.views.Quad
import io.github.lenticualrstone.civilight.feature.library.shelf.views.StarGroup
import io.github.lenticualrstone.civilight.feature.library.shelf.views.StarsGeo
import io.github.lenticualrstone.civilight.feature.library.shelf.views.Tones
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * U-16 boards ported (0.2.1): every number here is read from the generators (`scr_u16_lib_a` … `c`, run with
 * python3), so a view that drifts from its board fails here.
 */
class ShelfBoardsTest {
    private val eps = 0.06f

    private fun card(key: String) = ProjectCardUi(
        ProjectRowUi(key, "제목 $key", Rating.ALL, "장편 · 1-7", false, Protections(), false),
        null, CoverUi(null, null, null, "가", holo = false), false,
    )

    @Test
    fun `CSS transforms - rotations, origins and perspective match the CSS model`() {
        val m = M4()
        // rotateX(90): a point below the origin comes towards the viewer (the shelf's plank top).
        m.rotateX(90f)
        assertEquals(0f, m.mapY(0f, 200f, 0f), 1e-3f)
        assertEquals(200f, m.mapZ(0f, 200f, 0f), 1e-3f)
        // rotateY(90): +x goes away from the viewer (the corridor walls run down the corridor).
        m.reset().rotateY(90f)
        assertEquals(-100f, m.mapZ(100f, 0f, 0f), 1e-3f)
        // rotateZ is clockwise on screen (y down).
        m.reset().rotateZ(90f)
        assertEquals(100f, m.mapY(100f, 0f, 0f), 1e-3f)
        // A transform list applies left to right: translateZ(-100) rotateX(90) puts the plank top at z −100 … 100.
        m.reset().translate(0f, 0f, -100f).rotateX(90f)
        assertEquals(-100f, m.mapZ(0f, 0f, 0f), 1e-3f)
        assertEquals(100f, m.mapZ(0f, 200f, 0f), 1e-3f)
        // Perspective: `o + (p − o) · P / (P − z)`.
        val p = Persp(1100f, 195f, 150f)
        assertEquals(195f + (30f - 195f) * 1100f / 1023f, p.x(30f, 77f), 1e-3f)
        val q = FloatArray(8)
        assertTrue(Quad.project(M4(), 100f, 50f, p, q).let { abs(it) < 1e-3f })
        assertTrue(Quad.signedArea(q) > 0f)
        assertTrue(Quad.contains(q, 50f, 25f))
        assertFalse(Quad.contains(q, 250f, 25f))
        // A plane past the eye is not drawn.
        assertTrue(Quad.project(M4().translate(0f, 0f, 1050f), 10f, 10f, p, q).isNaN())
        // A face turned away winds the other way (backface-visibility: hidden).
        Quad.project(M4().translate(50f, 0f).rotateY(180f).translate(-50f, 0f), 100f, 50f, p, q)
        assertTrue(Quad.signedArea(q) < 0f)
    }

    @Test
    fun `3D 책장 - spines stand where the board's tap strips are`() {
        // Board A sample: T · H · D and the centres 53 · 102 · 148 · 195 · 242.
        val items = listOf(ShelfCardItem.Project(card("a")), ShelfCardItem.Project(card("b")), ShelfCardItem.Project(card("c")), ShelfCardItem.Lock(HiddenGroupRow(1)), ShelfCardItem.NewProject)
        val dims = BookcaseGeo.dims(items)
        assertEquals(listOf(46f, 34f, 40f, 36f, 40f), dims.map { it.t })
        assertEquals(listOf(238f, 196f, 222f, 212f, 204f), dims.map { it.h })
        assertEquals(listOf(154f, 130f, 148f, 144f, 140f), dims.map { it.d })
        val cx = BookcaseGeo.centres(dims)
        assertEquals(listOf(53f, 102f, 148f, 195f, 242f), cx.toList())
        assertEquals(0f, BookcaseGeo.shift(cx, dims, 2f), 0f)
        // `strips()`: the spine of book 0 projects to x 17.6 … 67.0, y 162.9 … 418.8 (perspective 1100 at 50% 30%).
        val p = Persp(BookcaseGeo.PERSP, BookcaseGeo.OX, BookcaseGeo.OY)
        val box = BookcaseGeo.book(M4(), cx[0], 0f, dims[0], 0f, 0f, 1f, 0f, 0f)
        val spine = M4().set(box).translate(-dims[0].t / 2f, 0f).translate(dims[0].t / 2f, dims[0].h / 2f).rotateY(-90f).translate(-dims[0].t / 2f, -dims[0].h / 2f)
        val q = FloatArray(8)
        Quad.project(spine, dims[0].t, dims[0].h, p, q)
        assertEquals(17.58f, Quad.minX(q), eps)
        assertEquals(67.04f, Quad.maxX(q), eps)
        assertEquals(162.9f, Quad.minY(q), eps)
        assertEquals(418.8f, Quad.maxY(q), eps)
        assertTrue("the spine faces the viewer", Quad.signedArea(q) > 0f)
        // Pulled and turned: the cover faces the viewer in the middle of the stage at ×1.24, z 40.
        val posed = BookcaseGeo.book(M4(), cx[0], 0f, dims[0], 1f, 0f, 1f, 0f, 1f)
        val cover = M4().set(posed).translate(0f, 0f, dims[0].t / 2f)
        assertEquals(195f, cover.mapX(dims[0].d / 2f, dims[0].h / 2f, 0f), eps)
        assertEquals(BookcaseGeo.POSE_CY, cover.mapY(dims[0].d / 2f, dims[0].h / 2f, 0f), eps)
        assertEquals(40f + 23f, cover.mapZ(dims[0].d / 2f, dims[0].h / 2f, 0f), eps)
        assertEquals(dims[0].d * 1.24f, cover.mapX(dims[0].d, 0f, 0f) - cover.mapX(0f, 0f, 0f), eps)
        // Open: `.zm` moves (422 − 210) / 1.24 down and grows ×3.2.
        assertEquals(171f, BookcaseGeo.OPEN_Y, 0.1f)
        // A longer shelf slides so the book in front stays left of the lamp.
        val many = BookcaseGeo.dims(List(12) { ShelfCardItem.Project(card("p$it")) })
        val mc = BookcaseGeo.centres(many)
        assertEquals(0f, BookcaseGeo.shift(mc, many, 0f), 0f)
        val s = BookcaseGeo.shift(mc, many, 11f)
        assertTrue(mc[11] + s + many[11].t / 2f <= BookcaseGeo.RIGHT + 0.01f)
        // The rise in: board A's sampled spring (`spring(.62)`) at 0.1 · 0.25 · 0.5 · 1.
        assertEquals(0.3740f, BookcaseGeo.springStep(.1f), 1e-3f)
        assertEquals(0.9872f, BookcaseGeo.springStep(.25f), 1e-3f)
        assertEquals(1.0344f, BookcaseGeo.springStep(.5f), 1e-3f)
        assertEquals(1.0003f, BookcaseGeo.springStep(1f), 1e-3f)
        assertEquals(0f, BookcaseGeo.rise(0, 0f), 0f)
    }

    @Test
    fun `표지 회전 - ftf is the board's`() {
        val pose = FlowGeo.Pose()
        val golden = listOf(
            -2.5f to Triple(-187f, 60f, -145f), -1f to Triple(-118f, 60f, -100f), -.4f to Triple(-47.2f, 24f, -16f), 0f to Triple(0f, 0f, 40f),
            .6f to Triple(70.8f, -36f, -44f), 1f to Triple(118f, -60f, -100f), 1.7f to Triple(150.2f, -60f, -121f), 3f to Triple(210f, -60f, -160f),
        )
        for ((d, g) in golden) {
            FlowGeo.ftf(d, pose)
            assertEquals("x $d", g.first, pose.x, eps)
            assertEquals("r $d", g.second, pose.r, eps)
            assertEquals("z $d", g.third, pose.z, eps)
        }
        // Open: the middle cover ends at (0, 136, 40), flipped, ×3.2.
        val m = FlowGeo.cover(M4(), 0f, 1f, pose)
        assertEquals(FlowGeo.LEFT + FlowGeo.W / 2f, m.mapX(FlowGeo.W / 2f, FlowGeo.H / 2f, 0f), eps)
        assertEquals(FlowGeo.TOP + FlowGeo.H / 2f + 136f, m.mapY(FlowGeo.W / 2f, FlowGeo.H / 2f, 0f), eps)
        assertEquals(-FlowGeo.W * 3.2f, m.mapX(FlowGeo.W, 0f, 0f) - m.mapX(0f, 0f, 0f), eps)
    }

    @Test
    fun `색인 카드 서랍 - dpose is the board's`() {
        val p = DrawerGeo.Pose()
        // d → (y, z, rx, opacity), from `dpose()`.
        val golden = listOf(
            -1.5f to floatArrayOf(-60f, 50f, -61f, 0f), -1f to floatArrayOf(-60f, 46f, -58f, 0f), -.7f to floatArrayOf(-42f, 32.2f, -40.6f, .6667f),
            -.3f to floatArrayOf(-18f, 13.8f, -17.4f, 1f), 0f to floatArrayOf(0f, 0f, 0f, 1f), .5f to floatArrayOf(-4f, -18f, 4.5f, 1f),
            1f to floatArrayOf(-8f, -36f, 9f, 1f), 2.5f to floatArrayOf(-20f, -90f, 11.25f, 1f), 4.5f to floatArrayOf(-36f, -162f, 14.25f, 0f),
        )
        for ((d, g) in golden) {
            DrawerGeo.dpose(d, p)
            assertEquals("y $d", g[0], p.y, eps)
            assertEquals("z $d", g[1], p.z, eps)
            assertEquals("rx $d", g[2], p.rx, eps)
            assertEquals("op $d", g[3], p.op, 1e-3f)
        }
        // A card behind the pulled one steps back 40 (`dpose(1, pulled)`: z −76); the pulled one rises 150, z 104.
        DrawerGeo.dpose(1f, p)
        val back = DrawerGeo.card(M4(), p, 0f, 1f)
        assertEquals(-76f, back.mapZ(DrawerGeo.CARD_W / 2f, DrawerGeo.CH, 0f), eps)
        DrawerGeo.dpose(0f, p)
        val pulled = DrawerGeo.card(M4(), p, 1f, 0f)
        assertEquals(DrawerGeo.CT + DrawerGeo.CH - 150f, pulled.mapY(DrawerGeo.CARD_W / 2f, DrawerGeo.CH, 0f), eps)
        assertEquals(104f, pulled.mapZ(DrawerGeo.CARD_W / 2f, DrawerGeo.CH, 0f), eps)
    }

    @Test
    fun `별자리 - spose is the board's`() {
        val p = StarsGeo.Pose()
        // Board items 0 · 2 on 장편 (k 0, 1 of 2), item 1 on 단편, item 3 the lock star; translate3d(x − hw, y − hh, z).
        fun check(o: StarsGeo.Orbit, k: Int, n: Int, t: Float, spin: Float, lock: Boolean, focus: Float, behind: Float, x: Float, y: Float, z: Float, ry: Float, s: Float, op: Float) {
            StarsGeo.pose(o, k, n, t, spin, lock, focus, behind, false, p)
            val hw = if (lock) 36f else 55f
            val hh = if (lock) 36f else 78f
            assertEquals(x, p.x - hw, .06f); assertEquals(y, p.y - hh, .06f); assertEquals(z, p.z, .06f)
            assertEquals(ry, p.ry, .06f); assertEquals(s, p.s, 1e-3f); assertEquals(op, p.op, 1e-3f)
        }
        check(StarsGeo.SERIAL, 0, 2, 0f, 0f, false, 0f, 0f, 180.3f, 252.5f, 52.3f, -.7f, .5f, .893f)
        check(StarsGeo.SERIAL, 0, 2, 13.7f, .8f, false, 0f, 0f, 41.7f, 260.3f, 84.6f, -25.6f, .5f, .965f)
        check(StarsGeo.SERIAL, 0, 2, 0f, 0f, false, 1f, 0f, 140f, 194f, 90f, -372f, 1.08f, 1f)
        check(StarsGeo.SHORT, 0, 1, 0f, 0f, false, 0f, 0f, 176.7f, 474.1f, 51.8f, -25.1f, .5f, .969f)
        check(StarsGeo.SHORT, 0, 1, 13.7f, .8f, false, 0f, 0f, 166.7f, 451.6f, -44.5f, -28.1f, .5f, .608f)
        check(StarsGeo.SHORT, 0, 1, 0f, 0f, false, 0f, 1f, 176.7f, 474.1f, -128.2f, -25.1f, .5f, .22f)
        check(StarsGeo.SERIAL, 1, 2, 0f, 0f, false, 0f, 0f, 9.7f, 227.5f, -52.3f, -31.3f, .5f, .657f)
        check(StarsGeo.SERIAL, 1, 2, 13.7f, .8f, false, 0f, 0f, 148.3f, 219.7f, -84.6f, -6.4f, .5f, .585f)
        check(StarsGeo.LOCK, 0, 1, 0f, 0f, true, 0f, 0f, 293f, 140f, 0f, 0f, .9f, .775f)
        check(StarsGeo.LOCK, 0, 1, 13.7f, .8f, true, 0f, 0f, 286.2f, 145f, 7f, 0f, .9f, 1f)
        // Rings tilt by the orbit's own amplitude: 76.5° and 76.9°.
        assertEquals(76.504f, StarsGeo.ringTilt(StarsGeo.SERIAL), 1e-3f)
        assertEquals(76.866f, StarsGeo.ringTilt(StarsGeo.SHORT), 1e-3f)
        assertEquals(StarsGeo.SETTING, StarsGeo.orbit(StarGroup.SETTING_ONLY))
        // Behind the lock nothing shows.
        StarsGeo.pose(StarsGeo.SERIAL, 0, 2, 0f, 0f, false, 0f, 1f, true, p)
        assertEquals(0f, p.op, 0f)
    }

    @Test
    fun `서고 회랑 - stops, interp and fog are the board's`() {
        val g = HallGeo.PHONE
        val st = HallGeo.stops(g, 4, door = true)
        assertEquals(listOf(150f, 64f, -7f, 710f, -64f, 7f, 1270f, 64f, -7f, 1830f, -64f, 7f, 2322f, 0f, 0f), st.toList())
        assertEquals(-2732f, HallGeo.zEnd(g, 4), 0f)
        val xy = FloatArray(2)
        val golden = listOf(100f to (64f to -7f), 150f to (64f to -7f), 430f to (0f to 0f), 710f to (-64f to 7f), 1000f to (3.4271f to -.37484f), 2322f to (0f to 0f), 2500f to (0f to 0f))
        for ((cam, e) in golden) {
            HallGeo.interp(st, cam, xy)
            assertEquals("x $cam", e.first, xy[0], 1e-3f)
            assertEquals("yaw $cam", e.second, xy[1], 1e-3f)
        }
        // The camera at a stop is the stop's own depth; half-way it is half-way.
        assertEquals(710f, HallGeo.cam(st, 1f), 0f)
        assertEquals(430f, HallGeo.cam(st, .5f), 0f)
        assertEquals(2322f, HallGeo.cam(st, 4f), 0f)
        // `fog_style`: passed (hidden), near (1), far (0.01), and the near-plane rule.
        assertEquals(-1f, HallGeo.fog(g, 400f, 120f, 150f), 0f)
        assertEquals(1f, HallGeo.fog(g, -120f, -400f, 150f), 0f)
        assertEquals(.01f, HallGeo.fog(g, -1800f, -2080f, 150f), .005f)
        assertEquals(-1f, HallGeo.fog(g, 300f, 20f, 150f), 0f)
        // `spines(1, 0, 252, 54, 82, 4 colours)`: the board's first runs.
        val runs = HallGeo.spines(1L, 252f, 54, 82, 4)
        assertEquals(16, runs.size)
        assertEquals(listOf(15 to 71, 12 to 57, 13 to 54, 13 to 67, 13 to 71, 15 to 58), runs.take(6).map { it.first to it.second })
        assertEquals(listOf(3, 0, 2, 1, 3, 2), runs.take(6).map { it.third })
        // A bay stands flat on its wall (±90°) and turns to 54° when the walk stops at it.
        val left = HallGeo.bay(M4(), g, 0, 0f)
        assertEquals(left.mapX(0f, 0f, 0f), left.mapX(g.bw, 0f, 0f), 1e-3f)
        val cur = HallGeo.bay(M4(), g, 0, 1f)
        assertTrue(cur.mapX(g.bw, 0f, 0f) > cur.mapX(0f, 0f, 0f))
    }

    @Test
    fun `원고 더미 - fan, pile and lift are the board's`() {
        assertEquals(116.9f, DeskGeo.FX, .05f)
        assertEquals(52.1f, DeskGeo.FY, .05f)
        val p = DeskGeo.Pose()
        DeskGeo.pile(0, false, p)
        assertEquals(listOf(-7f, -24f, 20f, -6f), listOf(p.x, p.y, p.z, p.rz))
        DeskGeo.pile(3, false, p)
        assertEquals(listOf(-1f, -20f, 2f, 14f), listOf(p.x, p.y, p.z, p.rz))
        DeskGeo.pile(4, true, p)
        assertEquals(listOf(196f, -262f, 0f, 18f), listOf(p.x, p.y, p.z, p.rz))
        // Fan: d = 2 → (233.8, 156.2, 10, 32); the sheet in front z 48 × 1.06.
        DeskGeo.fan(2f, 0f, p)
        assertEquals(233.8f, p.x, .1f); assertEquals(156.2f, p.y, .1f); assertEquals(10f, p.z, 0f); assertEquals(32f, p.rz, 0f)
        DeskGeo.fan(0f, 1f, p)
        assertEquals(48f, p.z, 0f); assertEquals(1.06f, p.s, 1e-4f); assertEquals(20f, p.rz, 0f)
        DeskGeo.lift(p, 0f)
        assertEquals(listOf(-80f, 180f, 230f, 24f, -54f, 1f), listOf(p.x, p.y, p.z, p.rz, p.rx, p.s))
        DeskGeo.lift(p, 1f)
        assertEquals(520f, p.z, 0f); assertEquals(1.1f, p.s, 1e-4f)
        // The desk's surface stays in front of the eye at full tilt (the plane is cut to what can reach the stage).
        val m = DeskGeo.desk(M4(), 1f, 1f).translate(0f, -1200f)
        for ((x, y) in listOf(0f to 0f, 2200f to 0f, 2200f to 2450f, 0f to 2450f)) assertTrue(m.mapZ(x, y, 0f) < DeskGeo.PERSP * .92f)
    }

    @Test
    fun `idle sway is the boards' own`() {
        assertEquals(0f, IdleSway.ax(0f), 1e-6f)
        assertEquals(.42f, IdleSway.ax((Math.PI / 2 / .84).toFloat()), 1e-4f)
        assertEquals(.2f * kotlin.math.sin(1.1f), IdleSway.ay(0f), 1e-6f)
        assertEquals(.55f, IdleSway.bx(2.9f * (Math.PI / 2).toFloat()), 1e-3f)
        // Board C keyframes: −1 at 0 %, +1 at 50 %, back at 100 % of 11 s, eased.
        assertEquals(-1f, IdleSway.c(0f), 1e-3f)
        assertEquals(1f, IdleSway.c(5.5f), 1e-3f)
        assertEquals(0f, IdleSway.c(2.75f), 1e-3f)
        assertEquals(-1f, IdleSway.c(11f), 1e-3f)
        assertEquals(.5f, Css.easeInOut(.5f), 1e-3f)
        assertTrue(Css.easeInOut(.25f) < .25f)
    }

    @Test
    fun `tones - the palette starts with the board's three books and a picture keeps its hue`() {
        assertEquals(0xFF353A46.toInt(), Tones.palette[0].cloth.value.shr(32).toInt())
        assertEquals(8, Tones.palette.size)
        val t = Tones.fromMean(.8f, .3f, .2f)
        assertTrue("cloth is dark", t.cloth.red < .5f)
        assertTrue("hue kept: red over blue", t.cloth.red > t.cloth.blue && t.band.red > t.band.blue)
        assertTrue("accent is light", t.band.red > t.cloth.red)
        val cover = CoverUi(null, null, null, "가", holo = false)
        assertEquals(Tones.slot(cover, "k1"), Tones.slot(cover, "k1"))
    }
}
