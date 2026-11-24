package io.github.lenticualrstone.civilight.feature.library.shelf.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * 책상 위 원고 더미 (board `scr_u16_lib_b` · `U16LibBDesk`, `U16LibBDeskStory`). A desk seen three-quarters
 * from above: `perspective: 1000px; perspective-origin: 50% 34%`, the camera at (50%, 46%) of the 844 board
 * tilting by `rotateX(ty · 3.5deg) rotateY(tx · 6deg)`, the desk `rotateX(54deg) rotateZ(-24deg)` with its
 * wood (135° desk1 → desk2, grain lines at 22 and 61 of every 62 px) under the lamp's pool. Sheets are
 * 176 × 236 paper with a binder clip. 더미: stacked at `PILE` (x, y, z, rz), the newest on top, the new-project
 * place off to the side. 펼치기: each sheet slides out 45 ms after the one before to x = 116.9 d, y = 52.1 d +
 * 26 |d|, z = 26 − 8 |d|, rz = 20 + 6 d (the one in front at z 48 × 1.06, ringed). 들어 보기: it rises off the
 * desk to face the viewer (x −80, y 180, z 230, rz 24, rx −54) and the rest dim to 0.5. 열기: it comes on to
 * z 520 × 1.1 and fades as the project screen takes over.
 */
internal object DeskGeo {
    const val PERSP = 1000f
    const val OX = 195f
    const val OY = 844f * .34f
    const val CAM_X = 195f
    const val CAM_Y = 844f * .46f
    const val SW = 176f
    const val SH = 236f
    const val TOP = 96f

    /** One fan step along the screen's horizontal (the desk is turned −24°): 128 · cos 24°, 128 · sin 24°. */
    val FX = (128.0 * cos(Math.toRadians(24.0))).toFloat()
    val FY = (128.0 * sin(Math.toRadians(24.0))).toFloat()

    /** `PILE`: x, y, z, rz per sheet from the top (z + 2 on the board); `NEW_AT` the new-project place. */
    val PILE = arrayOf(floatArrayOf(-7f, -24f, 18f, -6f), floatArrayOf(5f, -36f, 12f, 5f), floatArrayOf(-19f, -12f, 6f, -11f), floatArrayOf(-1f, -20f, 0f, 14f))
    val NEW_AT = floatArrayOf(196f, -262f, 0f, 18f)

    class Pose(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var rz: Float = 0f, var rx: Float = 0f, var s: Float = 1f)

    /** A sheet on the pile: the board's four places, then the same four a layer lower for longer piles. */
    fun pile(i: Int, isNew: Boolean, out: Pose): Pose {
        if (isNew) {
            out.x = NEW_AT[0]; out.y = NEW_AT[1]; out.z = NEW_AT[2]; out.rz = NEW_AT[3]
        } else {
            val p = PILE[i % PILE.size]
            out.x = p[0]; out.y = p[1]; out.z = p[2] + 2f - (i / PILE.size) * 24f; out.rz = p[3]
        }
        out.rx = 0f; out.s = 1f
        return out
    }

    /** A sheet spread [d] places from the one in front (fractional while dragging). */
    fun fan(d: Float, front: Float, out: Pose): Pose {
        val a = abs(d)
        out.x = d * FX; out.y = d * FY + a * 26f
        out.z = 26f - a * 8f + 22f * front
        out.rz = 20f + d * 6f
        out.rx = 0f; out.s = 1f + .06f * front
        return out
    }

    /** `들어 보기` (and on to `열기` by [open]). */
    fun lift(out: Pose, open: Float): Pose {
        out.x = -80f; out.y = 180f; out.z = 230f + 290f * open; out.rz = 24f; out.rx = -54f; out.s = 1f + .1f * open
        return out
    }

    fun mix(a: Pose, b: Pose, t: Float): Pose {
        a.x = Css.lerp(a.x, b.x, t); a.y = Css.lerp(a.y, b.y, t); a.z = Css.lerp(a.z, b.z, t)
        a.rz = Css.lerp(a.rz, b.rz, t); a.rx = Css.lerp(a.rx, b.rx, t); a.s = Css.lerp(a.s, b.s, t)
        return a
    }

    /** The desk's frame: camera, its tilt, then `rotateX(54deg) rotateZ(-24deg)` about the desk's centre. */
    fun desk(m: M4, tx: Float, ty: Float): M4 =
        m.translate(CAM_X, CAM_Y).rotateX(3.5f * ty).rotateY(6f * tx).rotateX(54f).rotateZ(-24f).translate(-800f, -800f)

    /** A sheet's box on the desk (`left: 50%; top: 50%; margin: -118px 0 0 -88px`). */
    fun sheet(m: M4, p: Pose): M4 =
        m.translate(800f - SW / 2f, 800f - SH / 2f).translate(SW / 2f, SH / 2f)
            .translate(p.x, p.y, p.z).rotateZ(p.rz).rotateX(p.rx).scale(p.s).translate(-SW / 2f, -SH / 2f)
}

private class SheetParts(val face: GraphicsLayer) {
    val fan = Animatable(0f)
    var isNew = false
}

@Composable
internal fun BoxScope.DeskView(args: SceneArgs, onTap: (Int) -> Unit) {
    val look = LookB.current()
    val density = LocalDensity.current.density
    val planes = remember { PlaneBuffer(40) }
    val dm = remember { M4() }
    val sm = remember { M4() }
    val a = remember { DeskGeo.Pose() }
    val b = remember { DeskGeo.Pose() }
    val parts = remember { HashMap<Any, SheetParts>() }
    val acc = CivilightTheme.colors.acc
    val paints = remember(look, density, acc) { DeskPaints(look, density, acc) }
    val spec = remember { spring<Float>(CivilightMotion.libBoardBDamping, CivilightMotion.libSheetStiffness) }
    val lift = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(args.picked) { lift.animateTo(if (args.picked) 1f else 0f, spec) }
    LaunchedEffect(args.opening) { if (args.opening) open.animateTo(1f, spec) else open.snapTo(0f) }
    val n = args.items.size
    val live = HashSet<Any>()
    for (i in args.window) {
        val item = args.items[i]
        live += item.key
        key(item.key) {
            val layer = rememberGraphicsLayer()
            val p = remember { SheetParts(layer) }
            parts[item.key] = p
            p.isNew = item is ShelfCardItem.NewProject
            // 펼치기 one after another (45 ms), 모으기 from the bottom of the pile up (30 ms).
            LaunchedEffect(args.fanned) {
                if (args.fanned) delay(CivilightMotion.libFanStaggerMs * i) else delay(CivilightMotion.libPileStaggerMs * (n - 1 - i).coerceAtLeast(0))
                p.fan.animateTo(if (args.fanned) 1f else 0f, spec)
            }
            FaceSource(layer, DeskGeo.SW, DeskGeo.SH) { SheetFace(item, args.unhide) }
        }
    }
    parts.keys.retainAll(live)

    SceneCanvas(args, planes, onTap) {
        persp.set(DeskGeo.PERSP, DeskGeo.OX, DeskGeo.OY)
        offX = 195f - args.frameW / 2f
        offY = DeskGeo.TOP
        val tx = args.sway.x
        val ty = args.sway.y
        backdrop(args.frameW, paints.stage)
        // (0.2.3) The backdrop now runs to the frame's foot (under the dock and the search field).
        dm.reset()
        DeskGeo.desk(dm, tx, ty)
        // `.surf` (cut to the part that can reach the stage) and `.lampx`, then `.dim` while a sheet is up.
        add(2200f, 2450f).also { it.m.set(dm).translate(0f, -1200f); it.paint = paints.surface; it.bias = -10000f }
        add(560f, 560f).also { it.m.set(dm).translate(800f - 280f, 800f - 280f).translate(tx * -60f, ty * -50f); it.paint = paints.lamp; it.alpha = .75f; it.bias = -9000f }
        val up = lift.value
        if (up > 0f) add(2200f, 2450f).also { it.m.set(dm).translate(0f, -1200f); it.paint = paints.dim; it.alpha = .45f * up; it.bias = -8000f }
        val pos = args.position.value
        val sheen = tx
        for (i in args.window) {
            val p = parts[args.items[i].key] ?: continue
            val isFront = i == args.front
            DeskGeo.pile(i, p.isNew, a)
            val front = if (isFront) 1f - abs(i - pos).coerceAtMost(1f) else 0f
            DeskGeo.fan(i - pos, front, b)
            DeskGeo.mix(a, b, p.fan.value)
            if (isFront && (up > 0f || open.value > 0f)) DeskGeo.mix(a, DeskGeo.lift(b, open.value), up)
            sm.set(dm)
            DeskGeo.sheet(sm, a)
            val dim = if (!isFront) .5f * up else 0f
            val alpha = if (isFront) 1f - open.value else 1f
            // Parallel sheets on a tilted desk: the higher sheet is drawn later (mean depth would not do).
            val zb = a.z * 50f + if (isFront) up * 100_000f else 0f
            // Drop shadow (`7px 9px 18px drop`), the ring of the sheet in front, the sheet with its sheen.
            if (!p.isNew) add(DeskGeo.SW + 2 * paints.shadow.margin, DeskGeo.SH + 2 * paints.shadow.margin).also {
                it.m.set(sm).translate(-paints.shadow.margin, -paints.shadow.margin)
                it.paint = paints.shadow.paint; it.alpha = alpha * (1f - up * .6f); it.bias = zb - 0.5f
            }
            val ring = if (isFront) p.fan.value * (1f - up) else 0f
            if (ring > 0.01f) add(DeskGeo.SW + 12f, DeskGeo.SH + 12f).also {
                it.m.set(sm).translate(-6f, -6f); it.paint = paints.ring; it.alpha = ring; it.bias = zb - 0.4f
            }
            add(DeskGeo.SW, DeskGeo.SH).also {
                it.m.set(sm); it.layer = p.face; it.alpha = alpha; it.item = i; it.bias = zb
                it.paint = paints.fx; it.i0 = if (p.isNew) 0 else 3; it.a1 = sheen; it.a0 = dim
            }
        }
    }
}

private class DeskPaints(private val look: LookB, private val dens: Float, private val acc: Color) {
    /** `.sh`: `linear-gradient(100deg, transparent 30%, rgba(255,255,255,.42) 50%, transparent 70%)` at 0.55. */
    val fx = SheenFx(Color.White.copy(alpha = .42f * .55f), look.wall2).paint
    val shadow = SoftShadow(look.drop, dens, 7f, 9f, 18f, 0f, 3f, 36f)

    val ring = PlanePaint { p -> drawRoundRect(acc, cornerRadius = CornerRadius(8f * density), style = Stroke(2f * density), alpha = p.alpha) }

    val dim = PlanePaint { p -> drawRect(look.wall2, alpha = p.alpha) }

    /** `.stage`: `linear-gradient(180deg, wall2 0%, wall 30%, wall 100%)` over the board (the frame shows 96 … 596). */
    private val stageB = SizedBrush { s -> Brush.verticalGradient(0f to look.wall2, .3f to look.wall, 1f to look.wall, startY = -96f * dens, endY = (844f - 96f) * dens) }
    val stage = PlanePaint { drawRect(stageB[size]) }

    /** `.lampx`: `radial-gradient(circle, lamp, transparent 66%)`. */
    private val lampB = Brush.radialGradient(0f to look.lamp, .66f to Color.Transparent)
    val lamp = PlanePaint { p -> drawCircle(lampB, size.minDimension / 2f, alpha = p.alpha) }

    private val woodB = SizedBrush { s -> Brush.linearGradient(listOf(look.desk1, look.desk2), start = Offset.Zero, end = Offset(s.width, s.width)) }
    private val poolB = SizedBrush { _ -> Brush.radialGradient(0f to look.lamp, .7f to Color.Transparent, center = Offset(800f * dens, 1968f * dens), radius = 544f * dens) }

    /**
     * `.surf`: the lamp's ellipse (544 × 480 at (800, 1968) of the surface), the grain, the 135° wood. The
     * plane starts at the surface's top (desk y −1200).
     */
    val surface = PlanePaint {
        val d = density
        drawRoundRect(woodB[size], cornerRadius = CornerRadius(18f * d))
        var x = 0f
        while (x < size.width) {
            drawRect(look.deskln, Offset(x + 22f * d, 0f), Size(d, size.height))
            drawRect(look.deskln, Offset(x + 61f * d, 0f), Size(d, size.height))
            x += 62f * d
        }
        val c = Offset(800f * d, 1968f * d)
        val rx = 544f * d
        withTransform({ scale(1f, 480f / 544f, c) }) {
            drawCircle(poolB[size], rx, c)
        }
    }
}
