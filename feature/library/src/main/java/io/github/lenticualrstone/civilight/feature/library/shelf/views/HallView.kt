package io.github.lenticualrstone.civilight.feature.library.shelf.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverArt
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * 서고 회랑 (board `scr_u16_lib_b` · `U16LibBHall`, `U16LibBHallInner`, `U16LibBHallStory`). A one-point
 * corridor walked by a camera (`perspective: 560px; perspective-origin: 50% 48%`): book walls left and right
 * (spines in the board's 106 px rhythm, a shelf board every 122), a tiled floor, a beamed ceiling, cut in
 * 280 px segments that fade with distance (fog from 900, gone at 1800) and are hidden once passed (the
 * board's `near + cam > persp · .72`). Each project is a bay standing flat on a wall every 560 (left, right, …)
 * that turns 36° towards the walker when the walk stops there (`rotateY(54deg)`), its lamp, light cone and
 * floor pool lit. The corridor ends in a wall with the new-project door. The camera eases between stops
 * (smoothstep) sideways (±64) and in yaw (±7°), bobbing while it walks; the walk is the board's spring
 * (`a = 140 · (target − cam) − 21 · v`). 열기 walks 230 on into the bay.
 */
internal object HallGeo {
    class G(
        val persp: Float, val w: Float, val top: Float, val bot: Float, val d: Float, val z0: Float, val bw: Float, val bh: Float,
        val view: Float, val side: Float, val yaw: Float, val fog0: Float, val fogr: Float, val pqh: Float, val pqt: Float, val pqm: Float,
        val doorW: Float, val doorH: Float, val doorT: Float, val sub: Float,
    ) {
        val h: Float get() = bot - top
    }

    val PHONE = G(560f, 168f, -262f, 232f, 560f, 520f, 280f, 388f, 370f, 64f, 7f, 900f, 900f, 78f, 22f, 12f, 190f, 300f, 18f, 280f)
    val INNER = G(600f, 200f, -250f, 224f, 580f, 540f, 300f, 380f, 390f, 76f, 7f, 950f, 950f, 80f, 22f, 12f, 200f, 300f, 18f, 290f)

    /** The world origin (`.w0` at 50% 48% of the 844 board) and the board px at the frame's top. */
    const val OX = 195f
    const val OY = 844f * .48f
    const val TOP = 96f
    const val START_Z = 400f
    const val OPEN_WALK = 230f

    /** Bay [i]'s centre depth (`zc = −(Z0 + i · D)`). */
    fun zc(g: G, i: Int): Float = -(g.z0 + i * g.d)

    /** The end wall's depth for [bays] bays (`−(Z0 + (bays − 1) · D + 0.95 D)`). */
    fun zEnd(g: G, bays: Int): Float = -(g.z0 + (max(bays, 1) - 1) * g.d + g.d * .95f)

    /** The stops: per bay (c, x, yaw), left bays seen from the right; then the door. Packed as triples. */
    fun stops(g: G, bays: Int, door: Boolean): FloatArray {
        val n = bays + if (door) 1 else 0
        val out = FloatArray(n * 3)
        for (i in 0 until bays) {
            val s = if (i % 2 == 0) -1f else 1f
            out[i * 3] = -zc(g, i) - g.view
            out[i * 3 + 1] = -s * g.side
            out[i * 3 + 2] = s * g.yaw
        }
        if (door) {
            val k = bays
            out[k * 3] = -zEnd(g, bays) - g.view - 40f
            out[k * 3 + 1] = 0f
            out[k * 3 + 2] = 0f
        }
        return out
    }

    /** The camera depth for a (fractional) stop [position]; beyond the ends it carries on at the end gaps. */
    fun cam(stops: FloatArray, position: Float): Float {
        val n = stops.size / 3
        if (n == 0) return 0f
        if (n == 1) return stops[0] + position * 560f
        val i = floor(position).toInt().coerceIn(0, n - 2)
        val u = position - i
        return Css.lerp(stops[i * 3], stops[(i + 1) * 3], u)
    }

    /** The board's `interp(cam)`: sideways offset and yaw between the stops (smoothstep), into [out] (x, yaw). */
    fun interp(stops: FloatArray, cam: Float, out: FloatArray) {
        val n = stops.size / 3
        if (n == 0) { out[0] = 0f; out[1] = 0f; return }
        if (cam <= stops[0]) { out[0] = stops[1]; out[1] = stops[2]; return }
        for (i in 0 until n - 1) {
            val a = i * 3
            val b = (i + 1) * 3
            if (cam <= stops[b]) {
                val u = Css.smooth((cam - stops[a]) / (stops[b] - stops[a]))
                out[0] = Css.lerp(stops[a + 1], stops[b + 1], u)
                out[1] = Css.lerp(stops[a + 2], stops[b + 2], u)
                return
            }
        }
        out[0] = stops[(n - 1) * 3 + 1]; out[1] = stops[(n - 1) * 3 + 2]
    }

    /** `fog_style`: −1 when hidden (passed), else the opacity for a piece from [near] to [far]. */
    fun fog(g: G, near: Float, far: Float, cam: Float): Float {
        if (near + cam > g.persp * .72f) return -1f
        val dist = -((near + far) / 2f + cam)
        return Css.clamp(1f - (dist - g.fog0) / g.fogr, 0f, 1f)
    }

    /** The world: `rotateX(−ty · 2.2deg) rotateY(yaw + tx · 3.2deg) translate3d(x, bob, cam)` at the origin. */
    fun world(m: M4, x: Float, yaw: Float, bob: Float, cam: Float, tx: Float, ty: Float): M4 =
        m.translate(OX, OY).rotateX(-ty * 2.2f).rotateY(yaw + tx * 3.2f).translate(x, bob, cam)

    /** A bay's box: on its wall, turned `rotateY(±90deg)` flat, `±54deg` when [cur] (0 … 1). */
    fun bay(m: M4, g: G, i: Int, cur: Float): M4 {
        val left = i % 2 == 0
        val zn = zc(g, i) + g.bw / 2f
        val bx = if (left) -g.w + 2f else g.w - 2f - g.bw
        val by = g.bot - g.bh - 14f
        m.translate(bx, by, zn)
        return if (left) m.rotateY(Css.lerp(90f, 54f, cur)) else m.translate(g.bw, 0f).rotateY(Css.lerp(-90f, -54f, cur)).translate(-g.bw, 0f)
    }

    /** The board's spine rows: (width, height, colour slot) runs from its LCG, as in `spines()`. */
    fun spines(seed: Long, width: Float, h0: Int, h1: Int, colours: Int): List<Triple<Int, Int, Int>> {
        val out = ArrayList<Triple<Int, Int, Int>>()
        var x = 0
        var k = seed
        while (x < width - 4) {
            k = Css.lcg(k)
            val w = 9 + (k % 9).toInt()
            val h = h0 + ((k shr 5) % (h1 - h0)).toInt()
            val c = ((k shr 9) % colours).toInt()
            if (x + w > width) break
            out += Triple(w, h, c)
            x += w + 2
        }
        return out
    }
}

private class BayParts(val face: GraphicsLayer) {
    val cur = Animatable(0f)
    var isDoor = false
}

@Composable
internal fun BoxScope.HallView(args: SceneArgs, onTap: (Int) -> Unit) {
    val look = LookB.current()
    val density = LocalDensity.current
    val g = if (args.wide) HallGeo.INNER else HallGeo.PHONE
    val items = args.items
    val door = items.lastOrNull() is ShelfCardItem.NewProject
    val bays = items.size - if (door) 1 else 0
    val stops = remember(g, bays, door) { HallGeo.stops(g, bays, door) }
    val zEnd = HallGeo.zEnd(g, bays)
    val planes = remember { PlaneBuffer(96) }
    val wm = remember { M4() }
    val bm = remember { M4() }
    val xy = remember { FloatArray(2) }
    val parts = remember { HashMap<Any, BayParts>() }
    val paints = remember(look, density, g) { HallPaints(look, density, g) }
    val baySpec = remember { spring<Float>(CivilightMotion.libBoardBDamping, CivilightMotion.libBayStiffness) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(args.opening) {
        if (args.opening) open.animateTo(1f, spring(CivilightMotion.libWalkDamping, CivilightMotion.libWalkStiffness)) else open.snapTo(0f)
    }
    // The bay the walk has stopped at (the board's `setCur`: within 18 of the stop and slow).
    val front by rememberUpdatedState(args.front)
    val position = args.position
    val settled by remember { derivedStateOf { if (!position.dragging.value && abs(position.value - front) < .04f) front else -1 } }
    val live = HashSet<Any>()
    for (i in args.window) {
        val item = items[i]
        live += item.key
        key(item.key) {
            val layer = rememberGraphicsLayer()
            val p = remember { BayParts(layer) }
            parts[item.key] = p
            p.isDoor = item is ShelfCardItem.NewProject
            LaunchedEffect(settled == i) { p.cur.animateTo(if (settled == i) 1f else 0f, baySpec) }
            val tone = if (item is ShelfCardItem.Project) rememberTone(item.card.cover, item.key) else null
            if (p.isDoor) FaceSource(layer, g.doorW, g.doorH) { Door(g, look) } else FaceSource(layer, g.bw, g.bh) { BayFace(item, i, g, look, tone, args.unhide) }
        }
    }
    parts.keys.retainAll(live)

    SceneCanvas(args, planes, onTap) {
        persp.set(g.persp, HallGeo.OX, HallGeo.OY)
        offX = 195f - args.frameW / 2f
        offY = HallGeo.TOP
        nearFrac = .9f
        val pos = position.value
        val cam = HallGeo.cam(stops, pos) + HallGeo.OPEN_WALK * open.value
        HallGeo.interp(stops, cam, xy)
        val speed = min(1f, abs(position.anim.velocity * g.d) / 900f)
        val bob = sin(cam / 70f) * 3f * speed
        val tx = args.sway.x
        backdrop(args.frameW, paints.far)
        wm.reset()
        HallGeo.world(wm, xy[0], xy[1], bob, cam, tx, args.sway.y)
        // The corridor, 280 at a time from z 400 to the end wall.
        var z = HallGeo.START_Z
        while (z > zEnd + 1f) {
            val z2 = max(zEnd, z - g.sub)
            val ln = z - z2
            val op = HallGeo.fog(g, z, z2, cam)
            if (op > 0f) {
                val along = HallGeo.START_Z - z
                add(ln, g.h).also { it.m.set(wm).translate(-g.w, g.top, z).rotateY(90f); it.paint = paints.wall; it.alpha = op; it.a0 = along; it.bias = -3000f }
                add(ln, g.h).also { it.m.set(wm).translate(g.w, g.top, z).rotateY(90f); it.paint = paints.wall; it.alpha = op; it.a0 = along; it.bias = -3000f }
                add(2 * g.w, ln).also { it.m.set(wm).translate(-g.w, g.bot, z).rotateX(-90f); it.paint = paints.floor; it.alpha = op; it.a0 = along; it.bias = -3000f }
                add(2 * g.w, ln).also { it.m.set(wm).translate(-g.w, g.top, z).rotateX(-90f); it.paint = paints.ceiling; it.alpha = op; it.a0 = along; it.bias = -3000f }
            }
            z = z2
        }
        // The end wall and its door.
        val endOp = HallGeo.fog(g, zEnd + 1f, zEnd, cam)
        if (endOp > 0f) {
            add(2 * g.w, g.h).also { it.m.set(wm).translate(-g.w, g.top, zEnd); it.paint = paints.wall; it.alpha = endOp; it.bias = -2900f }
            if (door) {
                val i = items.size - 1
                val p = parts[items[i].key]
                if (p != null) {
                    val glow = p.cur.value
                    if (glow > 0f) add(g.doorW * 2f, g.doorH).also { it.m.set(wm).translate(-g.doorW, g.bot - g.doorH, zEnd); it.paint = paints.doorGlow; it.alpha = endOp * glow; it.bias = -2800f }
                    add(g.doorW, g.doorH).also { it.m.set(wm).translate(-g.doorW / 2f, g.bot - g.doorH, zEnd); it.layer = p.face; it.alpha = endOp; it.item = i; it.bias = -2700f }
                }
            }
        }
        // Bays with their lights.
        for (i in args.window) {
            if (i >= bays) continue
            val p = parts[items[i].key] ?: continue
            val zc = HallGeo.zc(g, i)
            val cur = p.cur.value
            val s = if (i % 2 == 0) -1f else 1f
            val xc = s * g.w * .42f
            val bulbOp = HallGeo.fog(g, zc + 30f, zc - 30f, cam)
            if (bulbOp > 0f) {
                add(60f, 60f).also { it.m.set(wm).translate(xc - 30f, g.top + 2f, zc + 30f).rotateX(-90f); it.paint = paints.bulb; it.alpha = bulbOp; it.bias = -2000f }
                if (cur > 0f) add(140f, 140f).also { it.m.set(wm).translate(xc - 70f, g.top + 2f, zc + 70f).rotateX(-90f); it.paint = paints.bulbOn; it.alpha = bulbOp * cur; it.bias = -1990f }
            }
            if (cur > 0f) {
                add(g.w * 1.3f, g.h).also { it.m.set(wm).translate(xc - g.w * .65f, g.top, zc); it.paint = paints.cone; it.alpha = cur; it.bias = -1500f }
                add(g.w * 1.9f, g.w * 1.6f).also { it.m.set(wm).translate(xc - g.w * .95f, g.bot - 1f, zc + g.w * .8f).rotateX(-90f); it.paint = paints.pool; it.alpha = cur; it.bias = -1900f }
            }
            val zn = zc + g.bw / 2f
            val op = HallGeo.fog(g, zn, zn - g.bw, cam)
            if (op <= 0f) continue
            bm.set(wm)
            HallGeo.bay(bm, g, i, cur)
            add(g.bw, g.bh).also { it.m.set(bm); it.layer = p.face; it.alpha = op; it.item = i }
            if (cur > 0f) add(g.bw, g.bh).also { it.m.set(bm); it.paint = paints.bayGlow; it.alpha = op * .85f * cur; it.bias = .5f }
            add(g.bw - 48f, g.pqh).also { it.m.set(bm).translate(24f, 18f); it.paint = paints.plaqueSheen; it.i0 = 3; it.a1 = tx; it.alpha = op; it.bias = 1f }
        }
    }
}

/** A bay (`.bay`): the frame, the brass plaque (name · meta), three shelf rows of spines with the cover in the middle row. Lock: grille and the brass lock only. */
@Composable
private fun BayFace(item: ShelfCardItem, index: Int, g: HallGeo.G, look: LookB, tone: Tone?, unhide: io.github.lenticualrstone.civilight.designsystem.theme.Unhide?) {
    val rowsTop = 18f + g.pqh + 14f
    val rh = floor((g.bh - rowsTop - 14f) / 3f)
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(look.shelf2)
            .drawBehind {
                drawRoundRect(look.shelf, cornerRadius = CornerRadius(4f * density), style = Stroke(16f * density))
                drawRoundRect(look.wsh, cornerRadius = CornerRadius(4f * density), style = Stroke(density))
            },
    ) {
        val plaqueShape = RoundedCornerShape(4.dp)
        val plaque = Modifier
            .offset(24.dp, 18.dp)
            .size((g.bw - 48f).dp, g.pqh.dp)
            .clip(plaqueShape)
            .background(Brush.verticalGradient(listOf(look.brass, lerp(look.brass, Color.Black, .3f))))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        when (item) {
            is ShelfCardItem.Project -> {
                Column(plaque) {
                    BasicText(item.card.row.title, style = boardText(g.pqt, g.pqt * 1.25f, FontWeight.ExtraBold, look.brassink, -.02f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    BasicText(item.card.row.meta, Modifier.padding(top = 2.dp), style = boardText(g.pqm, g.pqm * 1.3f, FontWeight.SemiBold, look.brassink), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                val t = tone ?: Tones.palette[0]
                val cols = listOf(t.cloth, t.band, lerp(t.cloth, t.band, .5f), look.sp[index % look.sp.size], look.sp[(index + 2) % look.sp.size], look.sp[(index + 3) % look.sp.size])
                val inner = g.bw - 28f
                val fw = (rh * .74f).toInt()
                val fh = rh - 10f
                for (k in 0 until 3) {
                    val (seed, width, h0, h1, nc) = when (k) {
                        0 -> Row5(index * 7L + 1, inner, (rh * .62f).toInt(), (rh - 6f).toInt(), 4)
                        1 -> Row5(index * 7L + 3, inner - fw - 4f, (rh * .55f).toInt(), (rh - 8f).toInt(), 4)
                        else -> Row5(index * 7L + 5, inner, (rh * .5f).toInt(), (rh - 10f).toInt(), 6)
                    }
                    val runs = remember(seed, width, h0, h1, nc) { HallGeo.spines(seed, width, h0, h1, nc) }
                    Row(
                        Modifier
                            .offset(14.dp, (rowsTop + k * rh).dp)
                            .size(inner.dp, rh.dp)
                            .drawBehind { drawRect(look.shelf, Offset(0f, size.height - 8f * density), Size(size.width, 8f * density)) }
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (k == 1) {
                            Box(Modifier.size(fw.dp, fh.dp).clip(RoundedCornerShape(2.dp))) {
                                CoverArt(item.card.cover, with(LocalDensity.current) { 80.dp.roundToPx() })
                            }
                            Box(Modifier.size(2.dp, 1.dp))
                        }
                        for ((w, h, c) in runs) {
                            Box(Modifier.size(w.dp, h.dp).clip(RoundedCornerShape(2.dp, 2.dp, 0.dp, 0.dp)).background(cols[c % cols.size]))
                        }
                    }
                }
            }
            is ShelfCardItem.Lock -> {
                Box(plaque, contentAlignment = Alignment.Center) { CivilightIcon(IconId.Lock, contentDescription = null, size = 22.dp, tint = look.brassink) }
                Box(
                    Modifier
                        .offset(14.dp, rowsTop.dp)
                        .size((g.bw - 28f).dp, (g.bh - rowsTop - 14f).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(look.gap)
                        .drawBehind {
                            var x = 0f
                            while (x < size.width) {
                                drawRect(look.shelf, Offset(x, 0f), Size(4f * density, size.height))
                                x += 22f * density
                            }
                        },
                )
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = (g.bh * .55f - 36f).dp).size(72.dp).clip(CircleShape).background(look.brass),
                    contentAlignment = Alignment.Center,
                ) { CivilightIcon(IconId.Lock, contentDescription = null, size = 30.dp, tint = look.brassink) }
            }
            ShelfCardItem.NewProject -> Unit
        }
    }
}

private data class Row5(val seed: Long, val width: Float, val h0: Int, val h1: Int, val n: Int)

/** The end wall's door (`.endw .door`): dashed, open at the foot, `+` in its disc, `새 프로젝트`. */
@Composable
private fun Door(g: HallGeo.G, look: LookB) {
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp, 10.dp, 0.dp, 0.dp))
            .background(look.far.copy(alpha = .88f))
            .drawBehind {
                val d = density
                val r = 10f * d
                val p = Path().apply {
                    moveTo(d, size.height)
                    lineTo(d, r)
                    quadraticTo(d, d, r, d)
                    lineTo(size.width - r, d)
                    quadraticTo(size.width - d, d, size.width - d, r)
                    lineTo(size.width - d, size.height)
                }
                drawPath(p, look.edge, style = Stroke(2f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * d, 4f * d))))
            },
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(look.tint).drawBehind { drawCircle(look.edge, size.minDimension / 2f - density / 2f, style = Stroke(density)) }, contentAlignment = Alignment.Center) {
            CivilightIcon(IconId.Plus, contentDescription = null, size = 24.dp, tint = look.ink)
        }
        BasicText(ShelfCopy.NEW_PROJECT, style = boardText(g.doorT, g.doorT * 1.2f, FontWeight.Bold, look.ink))
    }
}

/** Board B corridor paints: the textures are tiles drawn once per theme and repeated by a shader. */
private class HallPaints(private val look: LookB, private val dens: Density, private val g: HallGeo.G) {
    private val d = dens.density

    /** One 106 × 122 tile of the book wall: the spine stripes, the gaps, the shelf board at 112 … 122. */
    private val wallTile: ImageBitmap = tile(106f, 122f) {
        drawRect(look.hw2)
        val sp = look.sp
        // (start, end, colour) along the 106 px rhythm; −1 = gap.
        val runs = floatArrayOf(0f, 13f, 0f, 13f, 15f, -1f, 15f, 27f, 1f, 27f, 45f, 2f, 45f, 47f, -1f, 47f, 58f, 3f, 58f, 74f, 4f, 74f, 78f, -1f, 78f, 90f, 2f, 90f, 103f, 0f, 103f, 106f, -1f)
        var k = 0
        while (k < runs.size) {
            val c = runs[k + 2].toInt()
            drawRect(if (c < 0) look.gap else sp[c], Offset(runs[k] * d, 0f), Size((runs[k + 1] - runs[k]) * d, size.height))
            k += 3
        }
        drawRect(look.board, Offset(0f, 112f * d), Size(size.width, 10f * d))
    }

    /** One 70 × 70 floor tile: `fl1` with a `fl2` joint, a `wsh` joint across. */
    private val floorTile: ImageBitmap = tile(70f, 70f) {
        drawRect(look.fl1)
        drawRect(look.fl2, Offset(0f, 68f * d), Size(size.width, 2f * d))
        drawRect(look.wsh, Offset(68f * d, 0f), Size(2f * d, size.height))
    }

    private val wallBrush = ShaderBrush(ImageShader(wallTile, TileMode.Repeated, TileMode.Repeated))
    private val floorBrush = ShaderBrush(ImageShader(floorTile, TileMode.Repeated, TileMode.Repeated))
    private val shade = Brush.verticalGradient(0f to look.wsh, .3f to Color.Transparent, .8f to Color.Transparent, 1f to look.wsh)

    private fun tile(w: Float, h: Float, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit): ImageBitmap {
        val bmp = ImageBitmap((w * d).toInt().coerceAtLeast(1), (h * d).toInt().coerceAtLeast(1))
        CanvasDrawScope().draw(dens, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bmp), Size(bmp.width.toFloat(), bmp.height.toFloat()), block)
        return bmp
    }

    /** `.wl`: the book wall, continuous across segments ([Plane.a0] is the segment's distance along). */
    val wall = PlanePaint { p ->
        val shift = (p.a0 % 106f) * d
        withTransform({ translate(-shift, 0f) }) { drawRect(wallBrush, Offset(0f, 0f), Size(size.width + shift, size.height), alpha = p.alpha) }
        drawRect(shade, alpha = p.alpha)
    }

    val floor = PlanePaint { p ->
        val shift = (p.a0 % 70f) * d
        withTransform({ translate(0f, -shift) }) { drawRect(floorBrush, Offset(0f, 0f), Size(size.width, size.height + shift), alpha = p.alpha) }
    }

    /** `.ce`: `repeating-linear-gradient(180deg, ceil 0 120px, beam 120px 140px)` along the corridor. */
    val ceiling = PlanePaint { p ->
        drawRect(look.ceil, alpha = p.alpha)
        var y = 120f - (p.a0 % 140f)
        while (y < p.h) {
            drawRect(look.beam, Offset(0f, y * d), Size(size.width, 20f * d), alpha = p.alpha)
            y += 140f
        }
    }

    private val bulbB = Brush.radialGradient(0f to look.bulb, .08f to look.bulb, .2f to look.glow, .7f to Color.Transparent)
    private val bulbOnB = Brush.radialGradient(0f to look.glow, .65f to Color.Transparent)
    private val coneB = Brush.verticalGradient(0f to look.cone, .92f to Color.Transparent)
    private val poolB = SizedBrush { s -> Brush.radialGradient(0f to look.glow, .68f to Color.Transparent, center = Offset(s.width / 2f, s.height / 2f), radius = s.width / 2f * 1.4142f) }
    private val bayGlowB = SizedBrush { s -> Brush.radialGradient(0f to look.glow, .75f to Color.Transparent, center = Offset(s.width / 2f, s.height * .4f), radius = s.width * .7f) }
    private val doorGlowB = SizedBrush { s -> Brush.radialGradient(0f to look.glow, .75f to Color.Transparent, center = Offset(s.width / 2f, s.height * .6f), radius = s.width * .5f) }

    val bulb = PlanePaint { p -> drawCircle(bulbB, size.minDimension / 2f, alpha = p.alpha) }

    val bulbOn = PlanePaint { p -> drawCircle(bulbOnB, size.minDimension / 2f, alpha = p.alpha) }

    /** `.cone`: `clip-path: polygon(42% 0, 58% 0, 100% 100%, 0 100%)` over `cone → transparent 92%`. */
    private var conePath: Path? = null
    val cone = PlanePaint { p ->
        val path = conePath ?: Path().apply {
            moveTo(.42f * size.width, 0f); lineTo(.58f * size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }.also { conePath = it }
        drawPath(path, coneB, alpha = p.alpha)
    }

    /** `.pool`: `radial-gradient(ellipse at 50% 50%, glow 0, glow0 68%)` on the floor. */
    val pool = PlanePaint { p ->
        val rx = size.width / 2f
        val ry = size.height / 2f
        withTransform({ scale(1f, ry / rx, center) }) {
            drawCircle(poolB[size], rx * 1.4142f, center, alpha = p.alpha)
        }
    }

    /** `.bay .bg`: `radial-gradient(ellipse 70% 60% at 50% 40%, glow, transparent 75%)`. */
    val bayGlow = PlanePaint { p ->
        val c = Offset(size.width / 2f, size.height * .4f)
        val rx = size.width * .7f
        withTransform({ scale(1f, size.height * .6f / rx, c) }) {
            drawCircle(bayGlowB[size], rx, c, alpha = p.alpha)
        }
    }

    val doorGlow = PlanePaint { p ->
        val c = Offset(size.width / 2f, size.height * .6f)
        val rx = size.width * .5f
        withTransform({ scale(1f, size.height * .6f / rx, c) }) {
            drawCircle(doorGlowB[size], rx, c, alpha = p.alpha)
        }
    }

    /** `.hall`: `radial-gradient(ellipse 60% 46% at 50% 44%, far 0%, bg 78%)` over the board. */
    private val farB = SizedBrush { s -> Brush.radialGradient(0f to look.far, .78f to look.bg, center = Offset(s.width / 2f, (844f * .44f - HallGeo.TOP) * d), radius = 390f * .6f * d) }
    val far = PlanePaint {
        val c = Offset(size.width / 2f, (844f * .44f - HallGeo.TOP) * d)
        drawRect(look.bg)
        withTransform({ scale(1f, (844f * .46f) / (390f * .6f), c) }) { drawCircle(farB[size], 390f * .6f * d, c) }
    }

    /** `.plq::after`: a white band (0.5 at 0.55) crossing the plaque with the tilt. */
    val plaqueSheen = SheenFx(Color.White.copy(alpha = .5f * .55f), look.wall2).paint
}
