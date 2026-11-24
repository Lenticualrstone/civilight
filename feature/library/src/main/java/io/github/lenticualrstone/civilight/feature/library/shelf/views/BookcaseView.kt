package io.github.lenticualrstone.civilight.feature.library.shelf.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * 3D 책장 (board `scr_u16_lib_a` · `U16LibShelf`, `U16LibShelfSteps`). The stage is 390 × 500 with
 * `perspective: 1100px; perspective-origin: 50% 30%`; the shelf group tilts about 50% 72% by
 * `rotateX(-3deg + ty · -5deg) rotateY(tx · 9deg)`. Each book is a box (thickness T, height H, depth D)
 * standing on the plank line SY = 400, its spine to the viewer (`.bk rotateY(90deg)`). 꺼내기: the slot comes
 * to the middle (`translate3d(195 − cx, 210 − (400 − H/2), 40px) scale(1.24)`, about 84 px forward on the
 * way), the book turns its cover to the viewer 160 ms later, the others step back 60 px and dim. 열기: the
 * cover opens 160° and the inside zooms ×3.2 to fill the stage, then the project screen takes over.
 */
internal object BookcaseGeo {
    const val PERSP = 1100f
    const val OX = 195f
    const val OY = 150f
    const val TILT_OX = 195f
    const val TILT_OY = 360f
    const val SY = 400f
    const val POSE_CY = 210f
    const val POSE_S = 1.24f
    const val POSE_Z = 40f
    const val OUT_Z = 44f
    const val GAP = 9f
    const val X0 = 30f

    /** Books stay left of the lamp (`left: 298px`); a longer shelf slides so the book in front is in view. */
    const val RIGHT = 290f
    const val ANCHOR = 160f
    const val OTHERS_Z = -60f
    const val OPEN_SCALE = 3.2f
    const val OPEN_FC = -160f

    /** `.zm` open: `translate3d(0, (422 − 210) / 1.24, 0) scale(3.2)`. */
    const val OPEN_Y = (422f - POSE_CY) / POSE_S

    /** The rise in (`l3rise 900ms` spr, 120 ms + 70 ms per book). */
    const val RISE_MS = 900f
    const val RISE_DELAY_MS = 120f
    const val RISE_STEP_MS = 70f
    const val RISE_FROM = -84f

    class Dims(val t: Float, val h: Float, val d: Float)

    /** The sample books' T · H · D in order, then the lock spine and the new place. */
    val PROJECT = listOf(Dims(46f, 238f, 154f), Dims(34f, 196f, 130f), Dims(40f, 222f, 148f))
    val LOCK = Dims(36f, 212f, 144f)
    val NEW = Dims(40f, 204f, 140f)

    fun dims(items: List<ShelfCardItem>): List<Dims> {
        var p = 0
        return items.map {
            when (it) {
                // The boards' three books first, then a stable size per project within the boards' ranges.
                is ShelfCardItem.Project -> if (p < PROJECT.size) PROJECT[p++] else { p++; hashed(it.key.toString()) }
                is ShelfCardItem.Lock -> LOCK
                ShelfCardItem.NewProject -> NEW
            }
        }
    }

    /** A book size from a stable [seed]: T 34 … 46, H 196 … 238 (the boards' ranges), D following H as the samples do. */
    fun hashed(seed: String): Dims {
        var h = 0
        for (ch in seed) h = h * 31 + ch.code
        val a = (h ushr 3).mod(13) / 12f
        val b = (h ushr 11).mod(15) / 14f
        val hh = 196f + 42f * b
        return Dims(34f + 12f * a, hh, 130f + (hh - 196f) * (24f / 42f))
    }

    /** Spine centres: from 30, each book T wide with 9 between (`cx = x + T / 2`). */
    fun centres(dims: List<Dims>): FloatArray {
        val out = FloatArray(dims.size)
        var x = X0
        for (i in dims.indices) {
            out[i] = x + dims[i].t / 2f
            x += dims[i].t + GAP
        }
        return out
    }

    /** How far the row slides left for the (fractional) [position]: none while the row fits left of the lamp. */
    fun shift(cx: FloatArray, dims: List<Dims>, position: Float): Float {
        if (cx.isEmpty()) return 0f
        val last = cx.size - 1
        val right = cx[last] + dims[last].t / 2f
        if (right <= RIGHT) return 0f
        val p = Css.clamp(position, 0f, last.toFloat())
        val i = min(p.toInt(), last)
        val j = min(i + 1, last)
        val c = Css.lerp(cx[i], cx[j], p - i)
        return -Css.clamp(c - ANCHOR, 0f, right - RIGHT)
    }

    /** `.tilt`: `rotateX(-3deg + ty · -5deg) rotateY(tx · 9deg)` about (195, 360). */
    fun tilt(m: M4, tx: Float, ty: Float): M4 =
        m.translate(TILT_OX, TILT_OY).rotateX(-3f - 5f * ty).rotateY(9f * tx).translate(-TILT_OX, -TILT_OY)

    /**
     * A book's `.bk` box (D × H) in stage space: the slot at its place on the shelf, pulled by [pull]
     * (0 rest … 1 posed), pushed back by [back] (another book is out), risen by [rise], zoomed open by
     * [open], turned by [turn] (0 spine … 1 cover).
     */
    fun book(m: M4, cx: Float, shift: Float, d: Dims, pull: Float, back: Float, rise: Float, open: Float, turn: Float): M4 {
        val left = cx + shift - d.d / 2f
        val top = SY - d.h
        m.translate(left, top)
        // `.slot`: rest → pose, with the storyboard's straight-out move on the way.
        val tx = pull * (195f - (cx + shift))
        val ty = pull * (POSE_CY - (SY - d.h / 2f))
        val tz = pull * POSE_Z + sin(pull.coerceIn(0f, 1f) * PI.toFloat()) * OUT_Z + back * OTHERS_Z
        val s = 1f + (POSE_S - 1f) * pull
        m.translate(d.d / 2f, d.h / 2f).translate(tx, ty, tz).scale(s).translate(-d.d / 2f, -d.h / 2f)
        // `.rise`: about the foot.
        if (rise < 1f) m.translate(d.d / 2f, d.h).rotateX(RISE_FROM * (1f - rise)).translate(-d.d / 2f, -d.h)
        // `.zm`.
        if (open > 0f) m.translate(d.d / 2f, d.h / 2f).translate(0f, OPEN_Y * open).scale(1f + (OPEN_SCALE - 1f) * open).translate(-d.d / 2f, -d.h / 2f)
        // `.bk`: rotateY(90deg) shows the spine; 0 the cover.
        return m.translate(d.d / 2f, d.h / 2f).rotateY(90f * (1f - turn)).translate(-d.d / 2f, -d.h / 2f)
    }

    /** The board's sampled spring (`spring(.62)`, settling at t = 1) at [t] (0 … 1 of the duration). */
    fun springStep(t: Float, zeta: Float = 0.62f): Float {
        if (t <= 0f) return 0f
        val w = 6.9f / zeta
        val wd = w * sqrt(1f - zeta * zeta)
        return 1f - exp(-zeta * w * t) * (cos(wd * t) + (zeta * w / wd) * sin(wd * t))
    }

    /** Book [i]'s rise at [ms] after the view appeared. */
    fun rise(i: Int, ms: Float): Float = springStep((ms - RISE_DELAY_MS - i * RISE_STEP_MS) / RISE_MS)
}

/** One book's parts: its two recorded faces, its own pull and turn, its tone. */
private class BookParts(val spine: GraphicsLayer, val cover: GraphicsLayer) {
    val pull = Animatable(0f)
    val turn = Animatable(0f)
    val fc = Animatable(0f)
    var tone: Tone = Tones.palette[0]
    var dims: BookcaseGeo.Dims = BookcaseGeo.NEW
    var isNew = false
}

@Composable
internal fun BoxScope.BookcaseView(args: SceneArgs, onTap: (Int) -> Unit) {
    val look = LookA.current()
    val items = args.items
    val dims = remember(items) { BookcaseGeo.dims(items) }
    // Each book its own cloth: the cover's picture or preset, else the project, never the same as its neighbour.
    val toneSlots = remember(items) { Tones.slots(items.map { (it as? ShelfCardItem.Project)?.let { p -> Tones.seed(p.card.cover, p.key) } }, Tones.BOARD_A) }
    val cx = remember(dims) { BookcaseGeo.centres(dims) }
    val parts = remember { HashMap<Any, BookParts>() }
    val planes = remember { PlaneBuffer(80) }
    val bm = remember { M4() }
    val tm = remember { M4() }
    val dens = androidx.compose.ui.platform.LocalDensity.current.density
    val paints = remember(look, dens) { BookPaints(look, dens) }
    val pullSpec = remember { spring<Float>(CivilightMotion.libSpringDamping, CivilightMotion.libPullStiffness) }
    val turnSpec = remember { spring<Float>(CivilightMotion.libSpringDamping, CivilightMotion.libTurnStiffness) }
    val openSpec = remember { spring<Float>(CivilightMotion.libSoftDamping, CivilightMotion.libOpenStiffness) }

    // Another book is out: the rest step back 60 px and dim (`.face::after` → `--dimc`).
    val back = remember { Animatable(0f) }
    LaunchedEffect(args.picked) { back.animateTo(if (args.picked) 1f else 0f, pullSpec) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(args.opening) {
        if (args.opening) {
            delay(CivilightMotion.libTurnDelayMs + 40L)
            open.animateTo(1f, openSpec)
        } else {
            open.snapTo(0f)
        }
    }
    // The rise in: once, when the view appears.
    val introMs = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        val end = BookcaseGeo.RISE_DELAY_MS + BookcaseGeo.RISE_STEP_MS * items.size + BookcaseGeo.RISE_MS
        while (introMs.floatValue < end) introMs.floatValue = (withFrameNanos { it } - start) / 1_000_000f
    }

    val live = HashSet<Any>()
    for (i in args.window) {
        val item = items[i]
        live += item.key
        key(item.key) {
            val spine = rememberGraphicsLayer()
            val cover = rememberGraphicsLayer()
            val p = remember { BookParts(spine, cover) }
            parts[item.key] = p
            p.dims = dims[i]
            p.isNew = item is ShelfCardItem.NewProject
            p.tone = if (item is ShelfCardItem.Project) rememberTone(item.card.cover, item.key, toneSlots[i]) else Tones.palette[0]
            val out = args.picked && i == args.front
            LaunchedEffect(out) {
                if (out) {
                    launchPair({ p.pull.animateTo(1f, pullSpec) }, { delay(CivilightMotion.libTurnDelayMs); p.turn.animateTo(1f, turnSpec) })
                } else {
                    launchPair({ p.turn.animateTo(0f, turnSpec) }, { delay(CivilightMotion.libReturnDelayMs); p.pull.animateTo(0f, pullSpec) })
                }
            }
            val opening = out && args.opening
            LaunchedEffect(opening) { if (opening) p.fc.animateTo(1f, turnSpec) else p.fc.snapTo(0f) }
            FaceSource(spine, p.dims.t, p.dims.h) { SpineFace(item, p.tone, p.dims.t, p.dims.h, args.unhide) }
            FaceSource(cover, p.dims.d, p.dims.h) { BookCoverFace(item, big = false, unhide = args.unhide) }
        }
    }
    parts.keys.retainAll(live)

    SceneCanvas(args, planes, onTap) {
        persp.set(BookcaseGeo.PERSP, BookcaseGeo.OX, BookcaseGeo.OY)
        offX = 195f - args.frameW / 2f
        offY = 0f
        val ext = max(0f, (args.frameW - 390f) / 2f)
        val tx = args.sway.x
        val ty = args.sway.y
        backdrop(args.frameW, paints.wall)
        tm.reset()
        BookcaseGeo.tilt(tm, tx, ty)
        // The room: back wall, the lamp's glow, the plank's top (all behind the books).
        env(tm, -30f - ext, 60f, 450f + 2 * ext, 360f, 0f, 0f, -110f, paints.back, -1000f)
        env(tm, -30f - ext, 60f, 450f + 2 * ext, 360f, tx * 22f, 0f, -109f, paints.glow, -999f)
        add(402f + 2 * ext, 200f).apply {
            this.m.set(tm).translate(-6f - ext, BookcaseGeo.SY).translate(0f, 0f, -100f).rotateX(90f)
            paint = paints.plankTop
            bias = -500f
        }
        env(tm, 298f - 67f, BookcaseGeo.SY - 132f - 57f, 190f, 190f, 0f, 0f, 10f, paints.lampGlow, -400f)
        env(tm, 298f, BookcaseGeo.SY - 132f, 56f, 132f, 0f, 0f, 10f, paints.lamp, -390f)
        val position = args.position.value
        val shift = BookcaseGeo.shift(cx, dims, position)
        val sheenX = tx
        for (i in args.window) {
            val p = parts[items[i].key] ?: continue
            val d = p.dims
            val pull = p.pull.value
            val rise = BookcaseGeo.rise(i - args.window.first, introMs.floatValue)
            val o = if (args.picked && i == args.front) open.value else 0f
            val dim = back.value * (1f - min(1f, pull * 2f))
            bm.set(tm)
            BookcaseGeo.book(bm, cx[i], shift, d, pull, back.value * (1f - pull), rise, o, p.turn.value)
            val zb = pull * 400f
            if (!p.isNew) {
                // `.bc` back cover, `.pg` page block, `.pe` fore-edge, `.tp` top.
                face(bm, 0f, 0f, d.d, d.h, i, zb) { translate(0f, 0f, -d.t / 2f).rotateY(180f) }.apply { paint = paints.backCover; a0 = dim; cull = true; ref = p.tone }
                face(bm, 0f, 3f, d.d - 5f, d.h - 6f, i, zb) { translate(0f, 0f, d.t / 2f - 1.5f) }.apply { paint = paints.page; cull = true }
                face(bm, d.d - d.t / 2f - 5f, 3f, d.t, d.h - 6f, i, zb) { rotateY(90f) }.apply { paint = paints.foreEdge; a0 = dim; cull = true }
                face(bm, 0f, -d.t / 2f, d.d - 5f, d.t, i, zb) { rotateX(90f) }.apply { paint = paints.top; a0 = dim; cull = true }
            }
            // `.sp` spine.
            face(bm, -d.t / 2f, 0f, d.t, d.h, i, zb) { rotateY(-90f) }.apply {
                layer = p.spine; paint = paints.fx; a0 = dim; a1 = sheenX; i0 = 1; cull = true
            }
            // `.fc` front cover on its hinge (`transform-origin: 0 50%`), `.fco` outside, `.fci` inside.
            val fcDeg = BookcaseGeo.OPEN_FC * p.fc.value
            add(d.d, d.h).apply {
                this.m.set(bm).translate(0f, d.h / 2f).translate(0f, 0f, d.t / 2f).rotateY(fcDeg).translate(0f, -d.h / 2f)
                layer = p.cover; paint = paints.fx; a0 = dim; a1 = sheenX; i0 = 2; cull = true; item = i; this.bias = zb + 0.5f
            }
            if (!p.isNew) {
                add(d.d, d.h).apply {
                    this.m.set(bm).translate(0f, d.h / 2f).translate(0f, 0f, d.t / 2f).rotateY(fcDeg).translate(0f, -d.h / 2f)
                        .translate(d.d / 2f, d.h / 2f).rotateY(180f).translate(-d.d / 2f, -d.h / 2f)
                    paint = paints.inside; cull = true; item = i; this.bias = zb + 0.5f; ref = p.tone
                }
            }
        }
        // `.plf` the plank's front edge and its shadow, in front of every standing book.
        env(tm, -6f - ext, BookcaseGeo.SY, 402f + 2 * ext, 18f, 0f, 0f, 100f, paints.plankFront, 0f)
        env(tm, -6f - ext, BookcaseGeo.SY + 18f, 402f + 2 * ext, 40f, 0f, 0f, 100f, paints.plankShadow, 0f)
    }
}

/** Runs two suspending blocks side by side (the slot and the turn keep their own springs and delays). */
internal suspend fun launchPair(a: suspend () -> Unit, b: suspend () -> Unit) {
    coroutineScope {
        launch { a() }
        launch { b() }
    }
}

/** The cloth tone a paint reads from its plane. */
private val Plane.tone: Tone get() = ref as? Tone ?: Tones.palette[0]

/** An environment plane at ([left], [top]) of the stage, moved by ([dx], [dy], [dz]) inside the tilt group. */
private fun PlaneBuffer.env(tilt: M4, left: Float, top: Float, w: Float, h: Float, dx: Float, dy: Float, dz: Float, pp: PlanePaint, zBias: Float) {
    val pl = add(w, h)
    pl.m.set(tilt).translate(left, top).translate(dx, dy, dz)
    pl.paint = pp
    pl.bias = zBias
}

/** A face of a box: at ([left], [top]) of the box, its own [transform] about its centre. */
private inline fun PlaneBuffer.face(box: M4, left: Float, top: Float, w: Float, h: Float, idx: Int, zBias: Float, transform: M4.() -> M4): Plane {
    val pl = add(w, h)
    pl.m.set(box).translate(left, top).translate(w / 2f, h / 2f).transform().translate(-w / 2f, -h / 2f)
    pl.item = idx
    pl.bias = zBias
    return pl
}

/** Board A's paints, built once per theme. */
private class BookPaints(private val look: LookA, private val dens: Float) {
    val fx = SheenFx(look.sheen, look.dimc).paint

    private val backShade = Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Black.copy(alpha = .1f)))
    val backCover = PlanePaint { p ->
        drawRect(p.tone.cloth, alpha = p.alpha)
        drawRect(backShade, alpha = p.alpha)
        if (p.a0 > 0f) drawRect(look.dimc, alpha = p.a0)
    }

    val page = PlanePaint { p ->
        val d = density
        drawRect(look.page, alpha = p.alpha)
        var y = 49f * d
        var k = 1
        while (k <= 7 && y < size.height) {
            val right = if (k % 3 == 0) (p.w - 16f) * .7f else p.w - 16f
            drawRect(look.pageln, Offset(16f * d, y), Size((right - 16f) * d, 2f * d), alpha = p.alpha)
            y += 9f * d
            k++
        }
    }

    val foreEdge = PlanePaint { p -> stripes(p, vertical = true) }
    val top = PlanePaint { p -> stripes(p, vertical = false) }

    private fun DrawScope.stripes(p: Plane, vertical: Boolean) {
        val d = density
        drawRect(look.paperE, alpha = p.alpha)
        var x = 1f * d
        val end = if (vertical) size.width else size.height
        while (x < end) {
            if (vertical) drawRect(Color.Black.copy(alpha = .08f), Offset(x, 0f), Size(d, size.height), alpha = p.alpha)
            else drawRect(Color.Black.copy(alpha = .07f), Offset(0f, x), Size(size.width, d), alpha = p.alpha)
            x += 2f * d
        }
        if (p.a0 > 0f) drawRect(look.dimc, alpha = p.a0)
    }

    /** `.fci`: the inside of the cover, `repeating-linear-gradient(45deg, rgba(255,255,255,.06) 0 6px, transparent 6px 12px)`. */
    val inside = PlanePaint { p ->
        val d = density
        drawRect(p.tone.cloth, alpha = p.alpha)
        val step = 12f * d * 1.4142f
        var c = -size.height
        while (c < size.width) {
            drawLine(Color.White.copy(alpha = .06f), Offset(c, size.height), Offset(c + size.height, 0f), 6f * d * 0.7071f * 1.4142f, alpha = p.alpha)
            c += step
        }
    }

    private val backB = Brush.verticalGradient(listOf(look.wallA, look.back))

    /** `.wall`: the stage's own `linear-gradient(180deg, wallA, wallB)`. */
    private val wallB = Brush.verticalGradient(listOf(look.wallA, look.wallB))
    val wall = PlanePaint { drawRect(wallB) }
    val back = PlanePaint { drawRect(backB) }

    /** `radial-gradient(60% 55% at 82% 62%, glow, transparent 70%)`. */
    private val glowB = SizedBrush { s -> Brush.radialGradient(0f to look.glow, .7f to Color.Transparent, center = Offset(.82f * s.width, .62f * s.height), radius = .6f * s.width) }
    val glow = PlanePaint { _ ->
        val rx = .6f * size.width
        val ry = .55f * size.height
        val c = Offset(.82f * size.width, .62f * size.height)
        withTransform({ scale(1f, ry / rx, c) }) { drawCircle(glowB[size], rx, c) }
    }

    private val plankTopB = Brush.verticalGradient(0f to look.plankF, .4f to look.plankT, 1f to look.plankT)
    val plankTop = PlanePaint { drawRect(plankTopB) }

    private val plankEdgeB = SizedBrush { _ -> Brush.verticalGradient(listOf(look.plankE, Color.Transparent), endY = 2f * dens) }
    val plankFront = PlanePaint {
        drawRect(look.plankF)
        drawRect(plankEdgeB[size])
    }

    /** `box-shadow: 0 22px 30px -12px shadow`. */
    private val plankShadowB = Brush.verticalGradient(0f to look.shadow.copy(alpha = 0f), .25f to look.shadow.copy(alpha = look.shadow.alpha * .6f), 1f to Color.Transparent)
    val plankShadow = PlanePaint { drawRect(plankShadowB) }

    /** `.lamp .lg`: `radial-gradient(closest-side, glow, transparent)` × 1.4. */
    private val lampGlowB = Brush.radialGradient(listOf(look.glow, Color.Transparent))
    val lampGlow = PlanePaint { drawCircle(lampGlowB, size.minDimension / 2f) }

    private val brass = Color(0xFFB88A45)
    private val stem = Color(0xFF8A6A3A)
    private val flame = Color(0xFFFFE2A6)
    private val glass = Color(255, 214, 150).copy(alpha = .35f)

    /** The lamp drawing (`viewBox 0 0 56 132`). */
    val lamp = PlanePaint {
        val d = density
        fun path(vararg xy: Float): Path = Path().apply {
            moveTo(xy[0] * d, xy[1] * d)
            var k = 2
            while (k < xy.size) { lineTo(xy[k] * d, xy[k + 1] * d); k += 2 }
            close()
        }
        drawLine(brass, Offset(28f * d, 14f * d), Offset(28f * d, 30f * d), 2f * d)
        drawPath(lampCap ?: path(16f, 30f, 40f, 30f, 36f, 38f, 20f, 38f).also { lampCap = it }, brass)
        drawRoundRect(glass, Offset(19f * d, 38f * d), Size(18f * d, 30f * d), androidx.compose.ui.geometry.CornerRadius(3f * d))
        drawRoundRect(brass, Offset(19f * d, 38f * d), Size(18f * d, 30f * d), androidx.compose.ui.geometry.CornerRadius(3f * d), style = Stroke(2f * d))
        drawPath(lampFlame ?: Path().apply {
            moveTo(28f * d, 44f * d)
            cubicTo(32f * d, 50f * d, 32f * d, 56f * d, 28f * d, 60f * d)
            cubicTo(24f * d, 56f * d, 24f * d, 50f * d, 28f * d, 44f * d)
            close()
        }.also { lampFlame = it }, flame)
        drawPath(lampFoot ?: path(17f, 68f, 39f, 68f, 36f, 74f, 20f, 74f).also { lampFoot = it }, brass)
        drawLine(stem, Offset(28f * d, 74f * d), Offset(28f * d, 124f * d), 2.5f * d)
        drawPath(lampBase ?: path(14f, 124f, 42f, 124f, 40f, 132f, 16f, 132f).also { lampBase = it }, stem)
    }
    private var lampCap: Path? = null
    private var lampFlame: Path? = null
    private var lampFoot: Path? = null
    private var lampBase: Path? = null
}
