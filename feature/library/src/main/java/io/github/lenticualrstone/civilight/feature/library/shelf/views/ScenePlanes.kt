package io.github.lenticualrstone.civilight.feature.library.shelf.views

import android.graphics.Matrix
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverUi
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.image.CoverBitmaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * The draw side of the boards' CSS 3D: every DOM box of a board becomes a [Plane] (its full matrix to the
 * perspective container, its size, what it shows). Each frame the view fills a [PlaneBuffer], which projects
 * the planes, culls back faces (`backface-visibility: hidden`), orders them far to near, and draws each one
 * through the homography that maps its rectangle onto its projected quad (`Matrix.setPolyToPoly`). Faces
 * built from composables (cover art, titles) are recorded once into a [GraphicsLayer] and replayed in their
 * quad; walls, wood and paper are drawn by [PlanePaint]s from brushes built once. All per-frame work runs in
 * the draw phase: a frame recomposes nothing and allocates nothing.
 */

/** Draws a plane's own content in its local px rectangle (`0 … w · density`, `0 … h · density`). */
internal fun interface PlanePaint {
    fun DrawScope.paint(plane: Plane)
}

/** One board box for one frame. */
internal class Plane {
    val m = M4()
    var w = 0f
    var h = 0f
    var alpha = 1f

    /** `backface-visibility: hidden`. */
    var cull = false

    /** The item a tap on this plane picks, or −1. */
    var item = -1

    var layer: GraphicsLayer? = null
    var paint: PlanePaint? = null

    /** Free per-plane values for its [paint] (an item index, a dim amount, a sheen offset). */
    var a0 = 0f
    var a1 = 0f
    var i0 = 0

    /** A per-plane object for its [paint] (an item's tone). */
    var ref: Any? = null

    /** Added to the depth when ordering (a decal over its own face, an item pulled in front). */
    var bias = 0f

    val q = FloatArray(8)
    var depth = 0f
    var shown = false

    fun reset(w: Float, h: Float) {
        m.reset()
        this.w = w
        this.h = h
        alpha = 1f
        cull = false
        item = -1
        layer = null
        paint = null
        a0 = 0f; a1 = 0f; i0 = 0
        ref = null
        bias = 0f
        shown = false
    }
}

/** The frame's planes; reused every frame (grows only when a board needs more planes than before). */
internal class PlaneBuffer(capacity: Int = 96) {
    private val pool = ArrayList<Plane>(capacity).apply { repeat(capacity) { add(Plane()) } }
    private var order = IntArray(capacity)
    var count = 0
        private set
    val persp = Persp(1000f, 195f, 250f)

    /** Board px → frame dp: the frame shows the board from ([offX], [offY]). */
    var offX = 0f
    var offY = 0f

    /** (0.2.3) The frame's height in board units: the board's 500, or more where the scene runs on under the bars. */
    var frameH = ShelfScene.STAGE_H

    /** The near limit (fraction of the perspective) past which a plane is not drawn. */
    var nearFrac = 0.92f

    private val mat = Matrix()
    private val src = FloatArray(8)
    private val dst = FloatArray(8)

    fun begin() {
        count = 0
    }

    /** A fresh plane `w × h`, its matrix the identity: the caller sets [Plane.m] to the board transform. */
    fun add(w: Float, h: Float): Plane {
        if (count == pool.size) {
            pool.add(Plane())
            order = order.copyOf(pool.size + 16)
        }
        val p = pool[count++]
        p.reset(w, h)
        return p
    }

    operator fun get(i: Int): Plane = pool[order[i]]

    /** Projects, culls and orders the planes (stable: equal depth keeps the order they were added in). */
    fun project() {
        var n = 0
        for (i in 0 until count) {
            val p = pool[i]
            if (p.alpha <= 0.004f || p.w <= 0f || p.h <= 0f) continue
            val d = Quad.project(p.m, p.w, p.h, persp, p.q, nearFrac)
            if (d.isNaN()) continue
            if (p.cull && Quad.signedArea(p.q) <= 0f) continue
            p.depth = d + p.bias
            p.shown = true
            // Insertion sort, far (small z) first.
            var j = n
            while (j > 0 && pool[order[j - 1]].depth > p.depth) {
                order[j] = order[j - 1]
                j--
            }
            order[j] = i
            n++
        }
        shownCount = n
    }

    var shownCount = 0
        private set

    fun draw(scope: DrawScope) = with(scope) {
        val d = density
        for (k in 0 until shownCount) {
            val p = pool[order[k]]
            src[0] = 0f; src[1] = 0f
            src[2] = p.w * d; src[3] = 0f
            src[4] = p.w * d; src[5] = p.h * d
            src[6] = 0f; src[7] = p.h * d
            for (i in 0 until 4) {
                dst[i * 2] = (p.q[i * 2] - offX) * d
                dst[i * 2 + 1] = (p.q[i * 2 + 1] - offY) * d
            }
            if (!mat.setPolyToPoly(src, 0, dst, 0, 4)) continue
            drawIntoCanvas { c ->
                c.save()
                c.nativeCanvas.concat(mat)
                p.layer?.let { layer ->
                    // A layer can be drawn twice in a frame (a cover and its reflection), so its own alpha stays
                    // 1 and a faded plane goes through a save layer instead.
                    layer.alpha = 1f
                    if (p.alpha < 0.995f) {
                        c.nativeCanvas.saveLayerAlpha(0f, 0f, p.w * d, p.h * d, (p.alpha * 255f).toInt())
                        drawLayer(layer)
                        c.nativeCanvas.restore()
                    } else {
                        drawLayer(layer)
                    }
                }
                // A paint draws in its plane's own box: `size` is the plane, not the stage.
                p.paint?.let { pp ->
                    val stage = drawContext.size
                    drawContext.size = androidx.compose.ui.geometry.Size(p.w * d, p.h * d)
                    with(pp) { paint(p) }
                    drawContext.size = stage
                }
                c.restore()
            }
        }
    }

    /** The item under ([x], [y]) (frame dp), nearest first; −1 when none. */
    fun hit(x: Float, y: Float): Int {
        val bx = x + offX
        val by = y + offY
        for (k in shownCount - 1 downTo 0) {
            val p = pool[order[k]]
            if (p.item >= 0 && p.alpha > 0.3f && Quad.contains(p.q, bx, by)) return p.item
        }
        return -1
    }

    /**
     * A flat backdrop over the board's 500 (z 0 projects 1:1), behind everything. (0.2.3) Kept at 500 so no board
     * gradient stretches; below it, where the frame runs on under the bars, every backdrop's foot is `bg`, which
     * the scene's own background already is.
     */
    fun backdrop(frameW: Float, paint: PlanePaint) {
        val p = add(frameW, ShelfScene.STAGE_H)
        p.m.translate(offX, offY)
        p.paint = paint
        p.bias = -1_000_000f
    }

    /** The frame-dp bounds of every shown plane of [item] (left, top, right, bottom), or false. */
    fun bounds(item: Int, out: FloatArray): Boolean {
        var any = false
        var l = 0f; var t = 0f; var r = 0f; var b = 0f
        for (k in 0 until shownCount) {
            val p = pool[order[k]]
            if (p.item != item || p.alpha <= 0.3f) continue
            val pl = Quad.minX(p.q) - offX
            val pt = Quad.minY(p.q) - offY
            val pr = Quad.maxX(p.q) - offX
            val pb = Quad.maxY(p.q) - offY
            if (!any) { l = pl; t = pt; r = pr; b = pb; any = true } else {
                l = minOf(l, pl); t = minOf(t, pt); r = max(r, pr); b = max(b, pb)
            }
        }
        if (any) { out[0] = l; out[1] = t; out[2] = r; out[3] = b }
        return any
    }
}

/**
 * Where each item was drawn, for its accessibility node and the tour target: written from the draw phase
 * only when an item moved by more than [STEP] dp, so a settled view writes nothing.
 */
@Stable
internal class SceneBounds {
    private val slots: Array<MutableState<IntRect>> = Array(SLOTS) { mutableStateOf(IntRect.Zero) }
    private val scratch = FloatArray(4)

    fun of(item: Int): IntRect = slots[item.mod(SLOTS)].value

    fun publish(planes: PlaneBuffer, items: IntRange) {
        for (i in items) {
            val s = slots[i.mod(SLOTS)]
            val r = if (planes.bounds(i, scratch)) {
                IntRect(scratch[0].roundToInt(), scratch[1].roundToInt(), scratch[2].roundToInt(), scratch[3].roundToInt())
            } else {
                IntRect.Zero
            }
            val o = s.value
            if (abs(o.left - r.left) > STEP || abs(o.top - r.top) > STEP || abs(o.right - r.right) > STEP || abs(o.bottom - r.bottom) > STEP) s.value = r
        }
    }

    private companion object {
        const val SLOTS = 16
        const val STEP = 6
    }
}

/**
 * Records [content] (`w × h` dp) into [layer] without drawing it here: the scene replays the layer in the
 * face's projected quad. The node stays in the tree (it measures, loads its image, takes effects such as
 * the unhide blur) but paints nothing on its own.
 */
@Composable
internal fun FaceSource(layer: GraphicsLayer, w: Float, h: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.size(0.dp)) {
        Box(
            modifier
                .requiredSize(w.dp, h.dp)
                .drawWithContent { layer.record { this@drawWithContent.drawContent() } },
            content = content,
        )
    }
}

/** A project's cloth and accent, sampled from its cover picture or taken from its palette slot. */
internal data class Tone(val cloth: Color, val band: Color)

internal object Tones {
    /** The boards' three sample books first (잿빛 새벽의 등불 · 별의 무게 · 안개 항구), then five more in the same key. */
    val palette = listOf(
        Tone(Color(0xFF353A46), Color(0xFFC8913F)),
        Tone(Color(0xFF2A2550), Color(0xFFB9A8F0)),
        Tone(Color(0xFF4B626A), Color(0xFFDDE5E6)),
        Tone(Color(0xFF4A3426), Color(0xFFE0B07A)),
        Tone(Color(0xFF2E3A4D), Color(0xFF9FC1E6)),
        Tone(Color(0xFF5C4330), Color(0xFFF0D2A0)),
        Tone(Color(0xFF2C3529), Color(0xFFB9D09A)),
        Tone(Color(0xFF3B2A3F), Color(0xFFE3A9C9)),
    )

    /** The palette slot of a cover: its picture or preset, else the project key (stable across launches). */
    fun slot(cover: CoverUi, key: Any): Int = slotOf(seed(cover, key))

    fun seed(cover: CoverUi, key: Any): String = cover.hash ?: cover.presetId ?: key.toString()

    fun slotOf(seed: String, size: Int = palette.size): Int {
        var h = 0
        for (ch in seed) h = h * 31 + ch.code
        return h.mod(size)
    }

    /** The 책장 board's own three cloths (잿빛 새벽의 등불 · 별의 무게 · 안개 항구): slate, indigo, sea-grey. */
    const val BOARD_A = 3

    /**
     * Palette slots of a row of books ([seeds]: null for the lock and the new place): each its seed's slot,
     * moved on to the next free one when it would repeat either of the two books before it, so a shelf never
     * shows two cloths alike side by side (QA 0.2.1: every spine read the same brown).
     */
    fun slots(seeds: List<String?>, size: Int = palette.size): IntArray {
        val out = IntArray(seeds.size) { -1 }
        for (i in seeds.indices) {
            val seed = seeds[i] ?: continue
            var s = slotOf(seed, size)
            var guard = 0
            while (guard++ < size && ((i >= 1 && out[i - 1] == s) || (i >= 2 && out[i - 2] == s))) s = (s + 1) % size
            out[i] = s
        }
        return out
    }

    /**
     * A cloth from the picture's mean colour: kept in its hue, pressed to the boards' cloth lightness
     * (about 0.22), and an accent lifted to about 0.72.
     */
    fun fromMean(r: Float, g: Float, b: Float): Tone {
        val l = 0.2126f * r + 0.7152f * g + 0.0722f * b
        fun at(target: Float, sat: Float): Color {
            val k = if (l <= 0.001f) 0f else target / l
            fun ch(c: Float): Float = Css.clamp(target + (c * k - target) * sat, 0f, 1f)
            return Color(ch(r), ch(g), ch(b))
        }
        return Tone(at(0.22f, 0.9f), at(0.72f, 1.2f))
    }
}

/** The tone of a project's cover: the palette slot at once, the picture's mean once it is decoded. */
@Composable
internal fun rememberTone(cover: CoverUi, key: Any, slot: Int = -1): Tone {
    val deps = LocalFeatureDeps.current
    val start = Tones.palette[if (slot >= 0) slot % Tones.palette.size else Tones.slot(cover, key)]
    val tone by produceState(start, cover.hash, cover.crop, start) {
        val hash = cover.hash ?: return@produceState
        value = withContext(Dispatchers.IO) {
            val bmp = deps.stores.blobs.fileOf(hash)?.let { runCatching { CoverBitmaps.decode(it, cover.crop, 16) }.getOrNull() } ?: return@withContext start
            var r = 0f; var g = 0f; var b = 0f
            val w = bmp.width
            val h = bmp.height
            val px = IntArray(w * h)
            bmp.getPixels(px, 0, w, 0, 0, w, h)
            for (c in px) {
                r += ((c shr 16) and 0xFF) / 255f
                g += ((c shr 8) and 0xFF) / 255f
                b += (c and 0xFF) / 255f
            }
            val n = max(px.size, 1).toFloat()
            Tones.fromMean(r / n, g / n, b / n)
        }
    }
    return tone
}

/**
 * The boards' light band over a face (`.gl` / `.sheen`: `linear-gradient(100deg, transparent 40%, sheen 50%,
 * transparent 60%)` on a box 3× the face's width, shifted by `tx · 26%`) and the dim veil (`::after`). The
 * plane's [Plane.i0] picks the blend (1 soft-light, 2 overlay 0.9, 3 plain), [Plane.a1] is the shift (−1 … 1
 * of 26 %), [Plane.a0] the veil's amount.
 */
internal class SheenFx(sheen: Color, private val dim: Color) {
    private val unit = androidx.compose.ui.graphics.Brush.linearGradient(
        0f to Color.Transparent, .4f to Color.Transparent, .5f to sheen, .6f to Color.Transparent, 1f to Color.Transparent,
        start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(1f, 0.18f),
    )

    val paint = PlanePaint { p ->
        val w = p.w * density
        val h = p.h * density
        if (p.i0 != 0) {
            val blend = when (p.i0) {
                1 -> androidx.compose.ui.graphics.BlendMode.Softlight
                2 -> androidx.compose.ui.graphics.BlendMode.Overlay
                else -> androidx.compose.ui.graphics.BlendMode.SrcOver
            }
            val a = if (p.i0 == 2) .9f * p.alpha else p.alpha
            drawContext.canvas.save()
            // `overflow: hidden`: the band never leaves its face.
            drawContext.canvas.clipRect(0f, 0f, w, h)
            drawContext.transform.translate(-w + p.a1 * 0.26f * 3f * w, -0.1f * h)
            drawContext.transform.scale(3f * w, 1.2f * h, androidx.compose.ui.geometry.Offset.Zero)
            drawRect(unit, size = androidx.compose.ui.geometry.Size(1f, 1f), alpha = a, blendMode = blend)
            drawContext.canvas.restore()
        }
        if (p.a0 > 0f) drawRect(dim, alpha = p.a0 * p.alpha)
    }
}

/** `Color.toArgb()` once, for paints that take an int. */
internal fun Color.argb(): Int = toArgb()

/**
 * A CSS `box-shadow: x y blur spread color` under a box, as its own plane: the plane is the box grown by
 * [margin] on every side, and the shadow is a rounded rect blurred by a mask filter (hardware since API 28).
 */
internal class SoftShadow(color: Color, density: Float, private val dx: Float, private val dy: Float, blur: Float, private val spread: Float, private val radius: Float, val margin: Float) {
    private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.argb()
        if (blur > 0f) maskFilter = android.graphics.BlurMaskFilter(blur / 2f * density, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    private val baseAlpha = p.alpha

    val paint = PlanePaint { pl ->
        val d = density
        val m = margin * d
        p.alpha = (baseAlpha * pl.alpha * (1f - pl.a0)).toInt().coerceIn(0, 255)
        drawIntoCanvas {
            it.nativeCanvas.drawRoundRect(
                m + (dx - spread) * d, m + (dy - spread) * d,
                size.width - m + (dx + spread) * d, size.height - m + (dy + spread) * d,
                radius * d, radius * d, p,
            )
        }
    }
}

/** A brush built for one draw size and kept until the size changes (a frame allocates nothing). */
internal class SizedBrush(private val build: (androidx.compose.ui.geometry.Size) -> androidx.compose.ui.graphics.Brush) {
    private var size = androidx.compose.ui.geometry.Size.Unspecified
    private var brush: androidx.compose.ui.graphics.Brush? = null

    operator fun get(s: androidx.compose.ui.geometry.Size): androidx.compose.ui.graphics.Brush {
        val b = brush
        if (b != null && s == size) return b
        size = s
        return build(s).also { brush = it }
    }
}
