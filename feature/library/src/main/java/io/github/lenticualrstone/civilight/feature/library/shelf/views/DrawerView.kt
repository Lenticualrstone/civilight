package io.github.lenticualrstone.civilight.feature.library.shelf.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import kotlin.math.max
import kotlin.math.min

/*
 * 색인 카드 서랍 (board `scr_u16_lib_c` · `U16LibCDrawer`, `U16LibCDrawerStory`). The drawer is seen from above
 * (`perspective: 900px; perspective-origin: 50% 2%`): floor, two side walls and the back wall, cards 318 × 290
 * standing on their foot (`transform-origin: 50% 100%`) with tabs 14 · 100 · 186, and the drawer front
 * (362 × 150 at z 44, its brass label and handle) hiding their feet. A card d places behind the front leans
 * back (`dpose`: z −36 d, y −8 d, rotateX 9° then 1.5° a place); a passed card falls forward over the front's
 * edge (rotateX −58°, up by 60) and fades. 꺼내기: the card rises 150 and comes 104 forward, the rest step back
 * 40 and the room dims. The whole drawer sways (`lcIdle`: rotateY ±2.2°, rotateX ∓0.8°, 11 s) and follows the
 * tilt (`rotateY(tx · 7deg) rotateX(ty · -4deg)`).
 */
internal object DrawerGeo {
    const val PERSP = 900f
    const val OX = 195f
    const val OY = 844f * .02f
    const val CX = 195f
    const val CY = 422f
    const val CH = 290f
    const val CT = 270f
    const val BASE = CT + CH
    const val WT = BASE - 150f
    const val FT = 500f
    const val CARD_L = 36f
    const val CARD_W = 318f
    const val TAB_H = 25f

    /** Board px shown at the top of the 500 dp frame. */
    const val TOP = 120f

    class Pose(var y: Float = 0f, var z: Float = 0f, var rx: Float = 0f, var op: Float = 1f)

    /** The board's `dpose(d)` (the pulled card is [pull] on top of it). */
    fun dpose(d: Float, out: Pose): Pose {
        if (d >= 0f) {
            out.z = -d * 36f; out.y = -d * 8f
            out.rx = min(d, 1f) * 9f + max(d - 1f, 0f) * 1.5f
            out.op = if (d > 4.4f) 0f else 1f
        } else {
            val e = min(-d, 1f)
            val r = max(-d - 1f, 0f)
            out.rx = -e * 58f - r * 6f
            out.z = e * 46f + r * 8f
            out.y = -e * (BASE - FT)
            out.op = if (r == 0f) Css.clamp(1f - (e - .55f) / .45f, 0f, 1f) else 0f
        }
        return out
    }

    /** `lcIdle` and the tilt, about the board's centre. */
    fun sway(m: M4, idle: Float, tx: Float, ty: Float): M4 =
        m.translate(CX, CY).rotateY(2.2f * idle).rotateX(-.8f * idle).rotateY(7f * tx).rotateX(-4f * ty).translate(-CX, -CY)

    /** A card's box (318 × 290 at (36, 270)), placed by [pose], pulled by [pull], pushed back by [back]. */
    fun card(m: M4, pose: Pose, pull: Float, back: Float): M4 {
        val y = Css.lerp(pose.y, -150f, pull)
        val z = Css.lerp(pose.z - 40f * back, 104f, pull)
        val rx = Css.lerp(pose.rx, 0f, pull)
        return m.translate(CARD_L, CT).translate(CARD_W / 2f, CH).translate(0f, y, z).rotateX(rx).translate(-CARD_W / 2f, -CH)
    }
}

private class CardParts(val face: GraphicsLayer) {
    val pull = Animatable(0f)
}

@Composable
internal fun BoxScope.DrawerView(args: SceneArgs, onTap: (Int) -> Unit) {
    val look = LookC.current()
    val density = LocalDensity.current.density
    val planes = remember { PlaneBuffer(48) }
    val sm = remember { M4() }
    val cm = remember { M4() }
    val pose = remember { DrawerGeo.Pose() }
    val parts = remember { HashMap<Any, CardParts>() }
    val paints = remember(look, density) { DrawerPaints(look, density) }
    val frontLayer = rememberGraphicsLayer()
    val pullSpec = remember { spring<Float>(CivilightMotion.libDrawerPullDamping, CivilightMotion.libDrawerPullStiffness) }
    val back = remember { Animatable(0f) }
    LaunchedEffect(args.picked) { back.animateTo(if (args.picked) 1f else 0f, pullSpec) }
    FaceSource(frontLayer, 362f, 150f) { DrawerFront(look, args.picked) }
    val live = HashSet<Any>()
    for (i in args.window) {
        val item = args.items[i]
        live += item.key
        key(item.key) {
            val layer = rememberGraphicsLayer()
            val p = remember { CardParts(layer) }
            parts[item.key] = p
            val tone = if (item is ShelfCardItem.Project) rememberTone(item.card.cover, item.key) else null
            val out = args.picked && i == args.front
            LaunchedEffect(out) { p.pull.animateTo(if (out) 1f else 0f, pullSpec) }
            FaceSource(layer, DrawerGeo.CARD_W, DrawerGeo.CH + DrawerGeo.TAB_H) { IndexCardFace(item, i, args.unhide, tone) }
        }
    }
    parts.keys.retainAll(live)

    SceneCanvas(args, planes, onTap) {
        persp.set(DrawerGeo.PERSP, DrawerGeo.OX, DrawerGeo.OY)
        offX = 195f - args.frameW / 2f
        offY = DrawerGeo.TOP
        backdrop(args.frameW, paints.bg)
        sm.reset()
        DrawerGeo.sway(sm, args.sway.idle.floatValue, args.sway.x, args.sway.y)
        // The drawer box: floor, side walls, back wall.
        add(342f, 290f).also { it.m.set(sm).translate(24f, DrawerGeo.BASE).translate(171f, 0f).translate(0f, 0f, 40f).rotateX(-90f).translate(-171f, 0f); it.paint = paints.floor; it.bias = -1000f }
        add(290f, 150f).also { it.m.set(sm).translate(24f, DrawerGeo.WT).translate(0f, 75f).translate(0f, 0f, 40f).rotateY(90f).translate(0f, -75f); it.paint = paints.wall; it.bias = -900f }
        add(290f, 150f).also { it.m.set(sm).translate(366f, DrawerGeo.WT).translate(0f, 75f).translate(0f, 0f, 40f).rotateY(90f).translate(0f, -75f); it.paint = paints.wall; it.bias = -900f }
        add(342f, 150f).also { it.m.set(sm).translate(24f, DrawerGeo.WT).translate(0f, 0f, -250f); it.paint = paints.backWall; it.bias = -1100f }
        val pos = args.position.value
        val sheen = args.sway.x + args.sway.idle.floatValue * (18f / 26f)
        for (i in args.window) {
            val p = parts[args.items[i].key] ?: continue
            DrawerGeo.dpose(i - pos, pose)
            val pull = p.pull.value
            val op = Css.lerp(pose.op, 1f, pull)
            if (op <= 0f) continue
            cm.set(sm)
            DrawerGeo.card(cm, pose, pull, back.value * (1f - pull))
            val cur = if (i == args.front) 1f else 0f
            add(DrawerGeo.CARD_W + 2 * paints.shadow.margin, DrawerGeo.CH + 2 * paints.shadow.margin).also {
                it.m.set(cm).translate(-paints.shadow.margin, -paints.shadow.margin)
                it.paint = paints.shadow.paint; it.alpha = op * (.8f + .2f * cur); it.cull = true; it.bias = -0.5f
            }
            // `.fc` with its tab above the card (the face starts 25 above the card's top) and `.bk`.
            add(DrawerGeo.CARD_W, DrawerGeo.CH + DrawerGeo.TAB_H).also {
                it.m.set(cm).translate(0f, -DrawerGeo.TAB_H)
                it.layer = p.face; it.paint = paints.fx; it.i0 = 3; it.a1 = sheen; it.alpha = op; it.cull = true; it.item = i
            }
            add(DrawerGeo.CARD_W, DrawerGeo.CH).also {
                it.m.set(cm).translate(0f, DrawerGeo.CH / 2f).rotateX(180f).translate(0f, -DrawerGeo.CH / 2f)
                it.paint = paints.back; it.alpha = op; it.cull = true; it.item = i
            }
        }
        // The drawer front, then the room's veil while a card is out (`.dimp`, z 100).
        add(362f, 150f).also { it.m.set(sm).translate(14f, DrawerGeo.FT).translate(0f, 0f, 44f); it.layer = frontLayer; it.bias = 0f }
        if (back.value > 0f) add(510f, 844f).also { it.m.set(sm).translate(-60f, 0f).translate(0f, 0f, 100f); it.paint = paints.dim; it.alpha = .55f * back.value }
    }
}

/** `.front`: the drawer face, its brass-edged `서재` label, the handle and the hint (hidden while a card is out). */
@Composable
private fun DrawerFront(look: LookC, pulled: Boolean) {
    val shape = RoundedCornerShape(10.dp, 10.dp, 4.dp, 4.dp)
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(look.dwf, look.dwf2)))
            .drawBehind {
                drawRect(look.edge, size = androidx.compose.ui.geometry.Size(size.width, density))
                val h = (if (look === LookC.dark) 18f else 14f) * density
                drawRect(Brush.verticalGradient(listOf(look.shadow.copy(alpha = 0f), look.shadow.copy(alpha = if (look === LookC.dark) look.shadow.alpha else .13f)), startY = size.height - h * 1.6f, endY = size.height), Offset(0f, size.height - h * 1.6f))
            },
    ) {
        val labShape = RoundedCornerShape(4.dp)
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = 26.dp).size(112.dp, 40.dp).clip(labShape).background(look.card).insetOutline(look.brass, 2.dp, labShape),
            contentAlignment = Alignment.Center,
        ) { BasicText(ShelfViewCopy.DRAWER_LABEL, style = boardText(13f, 16f, FontWeight.SemiBold, look.ink, .06f)) }
        Box(Modifier.align(Alignment.TopCenter).offset(y = 80.dp).size(132.dp, 14.dp).clip(RoundedCornerShape(7.dp)).background(Brush.verticalGradient(listOf(look.brass, look.dw2))))
        if (!pulled) {
            Box(Modifier.fillMaxWidth().offset(y = 112.dp), contentAlignment = Alignment.TopCenter) {
                BasicText(ShelfViewCopy.DRAWER_HINT, style = boardText(11f, 14f, FontWeight.Medium, if (look === LookC.dark) look.ink3 else look.ink2, .04f))
            }
        }
    }
}

private class DrawerPaints(private val look: LookC, density: Float) {
    val fx = SheenFx(look.sheen, look.shadow).paint
    val shadow = SoftShadow(look.shadow, density, 0f, 10f, 24f, 0f, 6f, 40f)
    private val floorB = Brush.verticalGradient(listOf(look.dwin, look.dw2))
    private val wallB = Brush.horizontalGradient(listOf(look.dw1, look.dw2))
    val floor = PlanePaint { drawRect(floorB) }
    val wall = PlanePaint { drawRect(wallB) }
    val backWall = PlanePaint { drawRect(look.dwin) }
    val dim = PlanePaint { p -> drawRect(look.bg, alpha = p.alpha) }
    val bg = PlanePaint { drawRect(look.bg) }

    /** `.bk`: the card's back, ruled every 22 on `--cardb`. */
    val back = PlanePaint { p ->
        val d = density
        drawRoundRect(look.cardb, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * d), alpha = p.alpha)
        var y = 21f * d
        while (y < size.height) {
            drawRect(look.cline, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, d), alpha = p.alpha)
            y += 22f * d
        }
    }
}
