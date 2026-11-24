package io.github.lenticualrstone.civilight.feature.library.shelf.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * 표지 회전 목록 (board `scr_u16_lib_a` · `U16LibFlow`, `U16LibFlowSteps`). Stage 390 × 500 with
 * `perspective: 900px; perspective-origin: 50% 36%`; the group tilts about 50% 40% by
 * `rotateX(ty · -6deg) rotateY(tx · 10deg)`. Covers are 200 × 280 at (95, 50); a cover d slots from the
 * middle sits at `ftf(d)` (x = 118 d, rotateY −60 d, z = 40 − 140 |d| within one slot; beyond, 46 more per
 * slot at ±60° and z −30 per slot). Each cover has its floor reflection (`scaleY(-1)` 6 px under it, veiled
 * by `rfA → rfB 42%`), side covers are dimmed 0.55. 열기: the middle cover flips (`rotateY(-180deg)`), moves to
 * `translate3d(0, 136px, 40px)` and grows ×3.2, its back (the home's paper) filling the stage.
 */
internal object FlowGeo {
    const val PERSP = 900f
    const val OX = 195f
    const val OY = 180f
    const val TILT_OX = 195f
    const val TILT_OY = 200f
    const val LEFT = 95f
    const val TOP = 50f
    const val W = 200f
    const val H = 280f
    const val SIDE_DIM = .55f

    class Pose(var x: Float = 0f, var r: Float = 0f, var z: Float = 0f)

    /** The board's `ftf(d)`. */
    fun ftf(d: Float, out: Pose): Pose {
        val a = abs(d)
        val s = if (d < 0f) -1f else 1f
        if (a <= 1f) {
            out.x = d * 118f; out.r = -d * 60f; out.z = 40f - a * 140f
        } else {
            out.x = s * (118f + (a - 1f) * 46f); out.r = -s * 60f; out.z = -100f - (a - 1f) * 30f
        }
        return out
    }

    fun tilt(m: M4, tx: Float, ty: Float): M4 =
        m.translate(TILT_OX, TILT_OY).rotateX(-6f * ty).rotateY(10f * tx).translate(-TILT_OX, -TILT_OY)

    /** The `.fi` box of a cover at slot offset [d], opened by [open] (0 … 1, middle cover only). */
    fun cover(m: M4, d: Float, open: Float, pose: Pose): M4 {
        ftf(d, pose)
        m.translate(LEFT, TOP).translate(W / 2f, H / 2f)
        val x = Css.lerp(pose.x, 0f, open)
        val y = 136f * open
        val z = Css.lerp(pose.z, 40f, open)
        val r = Css.lerp(pose.r, -180f, open)
        val s = 1f + 2.2f * open
        return m.translate(x, y, z).rotateY(r).scale(s).translate(-W / 2f, -H / 2f)
    }
}

private class FlowParts(val face: GraphicsLayer) {
    var isProject = false
}

@Composable
internal fun BoxScope.CoverFlowView(args: SceneArgs, onTap: (Int) -> Unit) {
    val look = LookA.current()
    val density = LocalDensity.current.density
    val planes = remember { PlaneBuffer(64) }
    val tm = remember { M4() }
    val cm = remember { M4() }
    val pose = remember { FlowGeo.Pose() }
    val parts = remember { HashMap<Any, FlowParts>() }
    val paints = remember(look, density) { FlowPaints(look, density) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(args.opening) {
        if (args.opening) open.animateTo(1f, spring(CivilightMotion.libSoftDamping, CivilightMotion.libOpenFlowStiffness)) else open.snapTo(0f)
    }
    val live = HashSet<Any>()
    for (i in args.window) {
        val item = args.items[i]
        live += item.key
        key(item.key) {
            val layer = rememberGraphicsLayer()
            val p = remember { FlowParts(layer) }
            p.isProject = item is ShelfCardItem.Project
            parts[item.key] = p
            FaceSource(layer, FlowGeo.W, FlowGeo.H) { BookCoverFace(item, big = true, unhide = args.unhide) }
        }
    }
    parts.keys.retainAll(live)

    SceneCanvas(args, planes, onTap) {
        persp.set(FlowGeo.PERSP, FlowGeo.OX, FlowGeo.OY)
        offX = 195f - args.frameW / 2f
        offY = 0f
        val ext = max(0f, (args.frameW - 390f) / 2f)
        val tx = args.sway.x
        backdrop(args.frameW, paints.wall)
        tm.reset()
        FlowGeo.tilt(tm, tx, args.sway.y)
        // `.fglow` behind everything; `.floor` the lit floor line.
        add(300f, 360f).also {
            it.m.set(tm).translate(45f, 10f).translate(150f, 180f).translate(tx * 24f, 0f, -180f).scale(1.4f).translate(-150f, -180f)
            it.paint = paints.glow
            it.bias = -1000f
        }
        add(510f + 2 * ext, 200f).also {
            it.m.set(tm).translate(-60f - ext, 334f).rotateX(90f)
            it.paint = paints.floor
            it.bias = -900f
        }
        val pos = args.position.value
        for (i in args.window) {
            val p = parts[args.items[i].key] ?: continue
            val d = i - pos
            val o = if (i == args.front) open.value else 0f
            cm.set(tm)
            FlowGeo.cover(cm, d, o, pose)
            val dim = FlowGeo.SIDE_DIM * min(1f, abs(d)) * (1f - o)
            val reflect = 1f - min(1f, o * 4f)
            // `.rf` reflection and `.rfo` its veil: under the cover, drawn before it.
            if (reflect > 0f) {
                add(FlowGeo.W, FlowGeo.H).also {
                    it.m.set(cm).translate(0f, FlowGeo.H + 6f).translate(FlowGeo.W / 2f, FlowGeo.H / 2f).scale3(1f, -1f, 1f).translate(-FlowGeo.W / 2f, -FlowGeo.H / 2f)
                    it.layer = p.face
                    it.alpha = reflect
                    it.bias = -0.6f
                }
                add(FlowGeo.W + 4f, 282f).also {
                    it.m.set(cm).translate(-2f, FlowGeo.H + 5f)
                    it.paint = paints.veil
                    it.alpha = reflect
                    it.bias = -0.5f
                }
            }
            // The card's own shadow (`0 20px 40px -18px`).
            add(FlowGeo.W + 2 * paints.shadow.margin, FlowGeo.H + 2 * paints.shadow.margin).also {
                it.m.set(cm).translate(-paints.shadow.margin, -paints.shadow.margin)
                it.paint = paints.shadow.paint
                it.bias = -0.4f
                it.cull = true
            }
            // `.ff` the cover (sheen, side dim) and `.fb` its back.
            add(FlowGeo.W, FlowGeo.H).also {
                it.m.set(cm)
                it.layer = p.face
                it.paint = paints.fx
                it.a0 = dim
                it.a1 = tx
                it.i0 = if (p.isProject) 2 else 0
                it.cull = true
                it.item = i
            }
            if (p.isProject) {
                add(FlowGeo.W, FlowGeo.H).also {
                    it.m.set(cm).translate(FlowGeo.W / 2f, 0f).rotateY(180f).translate(-FlowGeo.W / 2f, 0f)
                    it.paint = paints.back
                    it.cull = true
                    it.item = i
                }
            }
        }
    }
}

private class FlowPaints(private val look: LookA, density: Float) {
    val fx = SheenFx(look.sheen, look.dimc).paint
    val shadow = SoftShadow(look.shadow, density, 0f, 20f, 40f, -18f, 6f, 48f)

    /** `radial-gradient(closest-side, glow, transparent)`. */
    private val glowB = Brush.radialGradient(listOf(look.glow, Color.Transparent))
    val glow = PlanePaint { drawCircle(glowB, size.minDimension / 2f) }

    private val floorB = Brush.verticalGradient(listOf(look.line, Color.Transparent))
    private val wallB = Brush.verticalGradient(listOf(look.wallA, look.wallB))
    val wall = PlanePaint { drawRect(wallB) }
    val floor = PlanePaint { drawRect(floorB) }

    /** `.rfo`: `linear-gradient(180deg, rfA, rfB 42%)`. */
    private val veilB = Brush.verticalGradient(0f to look.rfA, .42f to look.rfB, 1f to look.rfB)
    val veil = PlanePaint { p -> drawRect(veilB, alpha = p.alpha) }

    /** `.fb`: the page side (`--page`, 28 / 22 padding, ruled 2 px lines 9 px apart, every third shorter). */
    val back = PlanePaint { p ->
        val d = density
        withTransform({ }) {
            drawRoundRect(look.page, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * d), alpha = p.alpha)
            var y = 28f * d
            var k = 1
            while (k <= 9) {
                val w = (p.w - 44f) * (if (k % 3 == 0) .7f else 1f)
                drawRect(look.pageln, Offset(22f * d, y), Size(w * d, 2f * d), alpha = p.alpha)
                y += 11f * d
                k++
            }
        }
    }
}
