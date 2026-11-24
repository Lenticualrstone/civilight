package io.github.lenticualrstone.civilight.feature.library.shelf.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/*
 * 별자리 서고 (board `scr_u16_lib_c` · `U16LibCStars`, `U16LibCStarsStory`). A night sky (`radial-gradient(120%
 * 70% at 40% 38%, sky1, sky2 70%)`, 90 stars and two dashed figures, shifted −6 px by the tilt) and, in
 * `perspective: 1000px; perspective-origin: 50% 42%`, the projects as small 3D books (110 × 156 × 18 at
 * scale 0.5) on tilted orbits, one orbit per 형식 (장편 at (150, 318) r 100 · 110 s a turn, 단편 at (262, 540)
 * r 60 · 80 s; this port adds 설정만 at (96, 452) r 44 · 70 s). The lock star floats alone at (322, 176); the
 * new-project slot sits at (74, 540) (board 590, raised in 0.2.3 so the dock no longer cuts it). 앞으로: the book spins in (rotateY −372°) to (195, 272, z 90) at ×1.08
 * while the rest fall back 180 and fade to 0.22 (to 0 behind the lock). Drag turns the orbits (0.012 rad a px).
 */
internal object StarsGeo {
    const val PERSP = 1000f
    const val OX = 195f
    const val OY = 844f * .42f
    const val CX = 195f
    const val CY = 422f
    const val TOP = 130f
    const val BW = 110f
    const val BH = 156f
    const val NX = 74f

    /**
     * (0.2.3) The new-project slot's centre: 540 (board 590), so its 96-tall place ends at 604, inside the stage's
     * 500 above the dock's top edge ([newSlotBottom]); at 590 the bar cut its `+`. Still clear of 설정만's orbit.
     */
    const val NY = 540f

    /** The slot's foot in stage units (board y − [TOP]). */
    val newSlotBottom: Float get() = NY - 32f + 96f - TOP

    class Orbit(val cx: Float, val cy: Float, val r: Float, val amp: Float, val per: Float, val ph: Float)

    val SERIAL = Orbit(150f, 318f, 100f, 24f, 110f, .55f)
    val SHORT = Orbit(262f, 540f, 60f, 14f, 80f, 2.1f)
    val SETTING = Orbit(96f, 452f, 44f, 10f, 70f, 1.2f)
    val LOCK = Orbit(322f, 176f, 7f, 5f, 11f, 0f)

    fun orbit(group: StarGroup): Orbit = when (group) {
        StarGroup.SERIAL -> SERIAL
        StarGroup.SHORT -> SHORT
        StarGroup.SETTING_ONLY -> SETTING
        StarGroup.OTHER -> LOCK
    }

    /** `rotateX` of an orbit ring: `90 − atan(amp / r)`. */
    fun ringTilt(o: Orbit): Float = 90f - atan2(o.amp, o.r) * 180f / PI.toFloat()

    class Pose(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var ry: Float = 0f, var s: Float = 1f, var op: Float = 1f)

    /**
     * The board's `SPOSE`: member [k] of [n] on [o] at orbit time [t] (s) turned by [spin]; [lock] for the lock
     * star (no spin, scale 0.9). [focus] (0 … 1) brings it to the front; [behind] (0 … 1) sends it back while
     * another is in front ([lockInFront]: then it fades out entirely).
     */
    fun pose(o: Orbit, k: Int, n: Int, t: Float, spin: Float, lock: Boolean, focus: Float, behind: Float, lockInFront: Boolean, out: Pose): Pose {
        val a = o.ph + 2f * PI.toFloat() * k / n.coerceAtLeast(1) + 2f * PI.toFloat() * t / o.per + (if (lock) 0f else spin)
        var x = o.cx + o.r * cos(a)
        var z = o.r * sin(a)
        var y = o.cy + o.amp * sin(a)
        var s = if (lock) .9f else .5f
        var ry = if (lock) 0f else -16f + 18f * cos(a)
        var op = .55f + .45f * (z + o.r) / (2f * o.r)
        if (behind > 0f) {
            z -= 180f * behind
            op = Css.lerp(op, if (lockInFront) 0f else .22f, behind)
        }
        if (focus > 0f) {
            x = Css.lerp(x, 195f, focus); y = Css.lerp(y, 272f, focus); z = Css.lerp(z, 90f, focus)
            s = Css.lerp(s, if (lock) 1.5f else 1.08f, focus)
            ry = Css.lerp(ry, if (lock) 0f else -372f, focus)
            op = Css.lerp(op, 1f, focus)
        }
        out.x = x; out.y = y; out.z = z; out.ry = ry; out.s = s; out.op = op
        return out
    }

    /** `lcIdle` and `.sk`: `translate3d(tx · 10, ty · 8, 0) rotateY(tx · 6deg) rotateX(ty · -5deg)` about the centre. */
    fun sway(m: M4, idle: Float, tx: Float, ty: Float): M4 =
        m.translate(CX, CY).rotateY(2.2f * idle).rotateX(-.8f * idle).translate(tx * 10f, ty * 8f).rotateY(6f * tx).rotateX(-5f * ty).translate(-CX, -CY)
}

private class StarParts(val cover: GraphicsLayer, val label: GraphicsLayer) {
    val focus = Animatable(0f)
    var tone: Tone = Tones.palette[0]
    var group = StarGroup.OTHER
    var k = 0
    var n = 1
}

@Composable
internal fun BoxScope.StarsView(args: SceneArgs, onTap: (Int) -> Unit) {
    val look = LookC.current()
    val loops = CivilightTheme.motion.loops
    val planes = remember { PlaneBuffer(64) }
    val sm = remember { M4() }
    val bm = remember { M4() }
    val pose = remember { StarsGeo.Pose() }
    val parts = remember { HashMap<Any, StarParts>() }
    val dens = androidx.compose.ui.platform.LocalDensity.current.density
    val paints = remember(look, dens) { StarPaints(look, dens) }
    val items = args.items
    val groups = remember(items) { items.map(ShelfScene::starGroup) }
    // Member k of n in each orbit, over all items (the window never moves a star).
    val slots = remember(groups) {
        val counts = IntArray(StarGroup.entries.size)
        val ks = groups.map { g -> counts[g.ordinal]++ }
        ks to counts
    }
    val focusItem = if (args.picked) args.front else -1
    val lockInFront = focusItem >= 0 && items.getOrNull(focusItem) is ShelfCardItem.Lock
    val backAll = remember { Animatable(0f) }
    val returnSpec = remember { spring<Float>(CivilightMotion.libDrawerPullDamping, CivilightMotion.libStarReturnStiffness) }
    val focusSpec = remember { spring<Float>(CivilightMotion.libSpringDamping, CivilightMotion.libStarFocusStiffness) }
    LaunchedEffect(focusItem >= 0) { backAll.animateTo(if (focusItem >= 0) 1f else 0f, returnSpec) }
    // The orbit clock: runs while nothing is in front, resumes where it stopped (board: `_p0`).
    val orbitT = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(loops, focusItem >= 0) {
        if (!loops || focusItem >= 0) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            orbitT.floatValue += (now - last) / 1_000_000_000f
            last = now
        }
    }
    // Group labels (`장편 · 2`), one layer per drawn orbit.
    val groupKeys = remember { StarGroup.entries.filter { it != StarGroup.OTHER } }
    val groupLayers = groupKeys.map { rememberGraphicsLayer() }
    for ((gi, g) in groupKeys.withIndex()) {
        val layer = groupLayers[gi]
        val n = slots.second[g.ordinal]
        if (n > 0) FaceSource(layer, 120f, 16f) { GroupLabel(ShelfViewCopy.groupCount(ShelfScene.groupLabel(g) ?: "", n), look) }
    }
    val live = HashSet<Any>()
    for (i in args.window) {
        val item = items[i]
        live += item.key
        key(item.key) {
            val cover = rememberGraphicsLayer()
            val label = rememberGraphicsLayer()
            val p = remember { StarParts(cover, label) }
            parts[item.key] = p
            p.group = groups[i]
            p.k = slots.first[i]
            p.n = slots.second[groups[i].ordinal]
            if (item is ShelfCardItem.Project) p.tone = rememberTone(item.card.cover, item.key)
            LaunchedEffect(focusItem == i) { p.focus.animateTo(if (focusItem == i) 1f else 0f, if (focusItem == i) focusSpec else returnSpec) }
            when (item) {
                is ShelfCardItem.Project -> FaceSource(cover, StarsGeo.BW, StarsGeo.BH) { StarCoverFace(item, args.unhide) }
                is ShelfCardItem.Lock -> FaceSource(cover, 72f, 72f) { LockStarFace() }
                ShelfCardItem.NewProject -> FaceSource(cover, 120f, 96f) { NewSlot(i == args.front, look) }
            }
            if (item !is ShelfCardItem.NewProject) {
                FaceSource(label, 120f, 24f) { StarLabel(if (item is ShelfCardItem.Project) item.card.row.title else "", i == args.front && focusItem < 0, look) }
            }
        }
    }
    parts.keys.retainAll(live)

    SceneCanvas(args, planes, onTap) {
        persp.set(StarsGeo.PERSP, StarsGeo.OX, StarsGeo.OY)
        offX = 195f - args.frameW / 2f
        offY = StarsGeo.TOP
        val tx = args.sway.x
        val ty = args.sway.y
        val ext = (args.frameW - 390f).coerceAtLeast(0f) / 2f
        // The sky: flat, behind everything (z 0 projects 1:1).
        add(430f + 2 * ext, 884f).also { it.m.translate(-20f - ext, -20f).translate(tx * -6f, ty * -6f); it.paint = paints.sky; it.bias = -100000f }
        sm.reset()
        StarsGeo.sway(sm, args.sway.idle.floatValue, tx, ty)
        // Orbit rings and their labels (gone while a book is in front).
        val ringA = 1f - backAll.value
        for (gi in groupKeys.indices) {
            val g = groupKeys[gi]
            val layer = groupLayers[gi]
            val n = slots.second[g.ordinal]
            if (n == 0 || ringA <= 0f) continue
            val o = StarsGeo.orbit(g)
            add(o.r * 2f, o.r * 2f).also {
                it.m.set(sm).translate(o.cx - o.r, o.cy - o.r).translate(o.r, o.r).rotateX(StarsGeo.ringTilt(o)).translate(-o.r, -o.r)
                it.paint = paints.ring; it.alpha = ringA; it.bias = -5000f
            }
            add(120f, 16f).also {
                it.m.set(sm).translate(o.cx - 60f, o.cy - o.r * .62f - 34f)
                it.layer = layer; it.alpha = ringA; it.bias = -4000f
            }
        }
        val t = orbitT.floatValue
        val spin = args.spin.floatValue
        for (i in args.window) {
            val item = items[i]
            val p = parts[item.key] ?: continue
            if (item is ShelfCardItem.NewProject) {
                add(120f, 96f).also { it.m.translate(StarsGeo.NX - 60f, StarsGeo.NY - 32f); it.layer = p.cover; it.item = i; it.alpha = ringA; it.bias = -3000f }
                continue
            }
            val lock = item is ShelfCardItem.Lock
            val f = p.focus.value
            val behind = backAll.value * (1f - f)
            StarsGeo.pose(StarsGeo.orbit(p.group), p.k, p.n, t, spin, lock, f, behind, lockInFront, pose)
            if (pose.op <= 0.01f) continue
            val zb = f * 1000f
            if (lock) {
                bm.set(sm).translate(pose.x - 36f, pose.y - 36f, pose.z).translate(36f, 36f).scale(pose.s).translate(-36f, -36f)
                add(72f, 72f).also { it.m.set(bm); it.layer = p.cover; it.alpha = pose.op; it.item = i; it.bias = zb }
            } else {
                bm.set(sm).translate(pose.x - 55f, pose.y - 78f, pose.z).translate(55f, 78f).rotateY(pose.ry).scale(pose.s).translate(-55f, -78f)
                book(bm, p, pose.op, i, zb, args.sway.x, paints)
            }
            // `.olab`: the name under the star (hidden while one is in front).
            val lop = 1f - backAll.value
            if (lop > 0.01f) {
                add(120f, 24f).also {
                    it.m.set(sm).translate(pose.x - 60f, pose.y + (if (lock) 34f else 46f), pose.z)
                    it.layer = p.label; it.alpha = lop; it.bias = zb + 1f
                }
            }
        }
        // The scrim under the book in front (`transparent 44%, bg 58%` at 0.94).
        if (backAll.value > 0f) add(390f + 2 * ext, 844f).also { it.m.translate(-ext, 0f); it.paint = paints.scrim; it.alpha = .94f * backAll.value; it.bias = 500f }
    }
}

/** A star book (`.bk3`): cover at z 9, back, spine (rotateY −90 at 55), page edge, top and bottom. */
private fun PlaneBuffer.book(bm: M4, p: StarParts, op: Float, i: Int, zb: Float, sheen: Float, paints: StarPaints) {
    fun face(left: Float, top: Float, w: Float, h: Float, build: M4.() -> M4): Plane {
        val pl = add(w, h)
        pl.m.set(bm).translate(left, top).translate(w / 2f, h / 2f).build().translate(-w / 2f, -h / 2f)
        pl.alpha = op; pl.cull = true; pl.item = i; pl.bias = zb
        return pl
    }
    face(0f, 0f, 110f, 156f) { rotateY(180f).translate(0f, 0f, 9f) }.also { it.paint = paints.cloth; it.ref = p.tone }
    face(46f, 0f, 18f, 156f) { rotateY(-90f).translate(0f, 0f, 55f) }.also { it.paint = paints.cloth; it.ref = p.tone }
    face(46f, 3f, 18f, 150f) { rotateY(90f).translate(0f, 0f, 53f) }.also { it.paint = paints.pages }
    face(2f, 69f, 104f, 18f) { rotateX(90f).translate(0f, 0f, 76f) }.also { it.paint = paints.pagesH }
    face(2f, 69f, 104f, 18f) { rotateX(-90f).translate(0f, 0f, 76f) }.also { it.paint = paints.page }
    face(0f, 0f, 110f, 156f) { translate(0f, 0f, 9f) }.also { it.layer = p.cover; it.paint = paints.fx; it.i0 = 3; it.a1 = sheen; it.bias = zb + 0.5f }
}

@Composable
private fun GroupLabel(text: String, look: LookC) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(text, style = boardText(11f, 14f, FontWeight.SemiBold, look.ink2, .1f).merge(androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center)), maxLines = 1)
    }
}

@Composable
private fun StarLabel(name: String, selected: Boolean, look: LookC) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(name, style = boardText(12f, 16f, FontWeight.SemiBold, if (selected) look.ink else look.ink2), maxLines = 1)
        if (selected) Box(Modifier.padding(top = 4.dp).size(16.dp, 2.dp).clip(RoundedCornerShape(1.dp)).background(look.acc))
    }
}

@Composable
private fun NewSlot(selected: Boolean, look: LookC) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp)) { NewSlotFace(selected) }
        BasicText(ShelfCopyNew, Modifier.padding(top = 8.dp).width(120.dp).height(16.dp), style = boardText(12f, 16f, FontWeight.SemiBold, look.ink2).merge(androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center)))
    }
}

private val ShelfCopyNew = io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy.NEW_PROJECT

private class StarPaints(private val look: LookC, private val dens: Float) {
    val fx = SheenFx(look.sheen, look.shadow).paint
    val cloth = PlanePaint { p -> drawRect((p.ref as? Tone ?: Tones.palette[0]).cloth, alpha = p.alpha) }
    val page = PlanePaint { p -> drawRect(look.page, alpha = p.alpha) }

    /** `repeating-linear-gradient(90deg, page 0 2px, cline 2px 3px)` (and its 180deg twin on the top). */
    val pages = PlanePaint { p -> edge(p, true) }
    val pagesH = PlanePaint { p -> edge(p, false) }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.edge(p: Plane, vertical: Boolean) {
        val d = density
        drawRect(look.page, alpha = p.alpha)
        var x = 2f * d
        val end = if (vertical) size.width else size.height
        while (x < end) {
            if (vertical) drawRect(look.cline, Offset(x, 0f), androidx.compose.ui.geometry.Size(d, size.height), alpha = p.alpha)
            else drawRect(look.cline, Offset(0f, x), androidx.compose.ui.geometry.Size(size.width, d), alpha = p.alpha)
            x += 3f * d
        }
    }

    val ring = PlanePaint { p -> drawCircle(look.cst, size.minDimension / 2f - density / 2f, style = Stroke(density), alpha = p.alpha) }

    private val scrimB = Brush.verticalGradient(0f to Color.Transparent, .44f to Color.Transparent, .58f to look.bg, 1f to look.bg)
    val scrim = PlanePaint { p -> drawRect(scrimB, alpha = p.alpha) }
    private val skyB = SizedBrush { s ->
        val k = dens
        Brush.radialGradient(0f to look.sky1, .7f to look.sky2, center = Offset((20f + 390f * .4f) * k, (20f + 844f * .38f) * k), radius = 390f * 1.2f * k)
    }

    // The sky: 90 seeded stars and the two dashed figures behind the orbits (board `sky_svg`, its own seed).
    private val dots: FloatArray = run {
        val r = java.util.Random(7)
        val sizes = floatArrayOf(.6f, .7f, .8f, 1f, 1.2f)
        FloatArray(90 * 4).also { a ->
            for (k in 0 until 90) {
                a[k * 4] = r.nextFloat() * 430f
                a[k * 4 + 1] = r.nextFloat() * 884f
                a[k * 4 + 2] = sizes[r.nextInt(sizes.size)]
                a[k * 4 + 3] = .25f + r.nextFloat() * .65f
            }
        }
    }
    private val figA = floatArrayOf(40f, 250f, 96f, 222f, 170f, 236f, 238f, 214f, 262f, 262f, 220f, 300f)
    private val figB = floatArrayOf(212f, 500f, 262f, 476f, 318f, 498f, 330f, 548f)
    private var dash: PathEffect? = null

    val sky = PlanePaint { _ ->
        val d = density
        // `radial-gradient(120% 70% at 40% 38%, sky1 0, sky2 70%)` over the 390 × 844 board (20 px inside this plane).
        val c = Offset((20f + 390f * .4f) * d, (20f + 844f * .38f) * d)
        val rx = 390f * 1.2f * d
        val ry = 844f * .7f * d
        drawRect(look.sky2)
        withTransform({ scale(1f, ry / rx, c) }) {
            drawCircle(skyB[size], rx, c)
        }
        for (k in 0 until 90) drawCircle(look.star, dots[k * 4 + 2] * d, Offset(dots[k * 4] * d, dots[k * 4 + 1] * d), alpha = dots[k * 4 + 3])
        val pe = dash ?: PathEffect.dashPathEffect(floatArrayOf(2f * d, 4f * d)).also { dash = it }
        for (fig in arrayOf(figA, figB)) {
            var k = 0
            while (k + 3 < fig.size) {
                drawLine(look.cst, Offset((fig[k] + 20f) * d, (fig[k + 1] + 20f) * d), Offset((fig[k + 2] + 20f) * d, (fig[k + 3] + 20f) * d), d, pathEffect = pe)
                k += 2
            }
            k = 0
            while (k + 1 < fig.size) {
                drawCircle(look.star, 1.8f * d, Offset((fig[k] + 20f) * d, (fig[k + 1] + 20f) * d))
                k += 2
            }
        }
    }
}
