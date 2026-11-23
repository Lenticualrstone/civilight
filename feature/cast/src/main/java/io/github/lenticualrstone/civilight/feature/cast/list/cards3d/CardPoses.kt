package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import io.github.lenticualrstone.civilight.domain.cast.list.Hangul
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tan

/**
 * Where one card sits in a 3D card view (U-16, CST-67), in board pixels of a 390-wide stage (the boards
 * `scr_u16_cards_a/b/c.py`), with its centre at the stage centre. [z] is depth towards the viewer; Compose
 * has no shared perspective, so the renderer turns it into scale ([projected]) and draw order.
 */
internal data class CardPose(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    val rotZ: Float = 0f,
    val scale: Float = 1f,
    /** 0 = full brightness, 1 = fully shaded. */
    val dim: Float = 0f,
    val alpha: Float = 1f,
) {
    /** [scale] with the board's perspective applied to [z]. */
    val projected: Float get() = scale * CardPoses.PERSPECTIVE / (CardPoses.PERSPECTIVE - z.coerceAtMost(CardPoses.PERSPECTIVE / 2))
}

/**
 * Pure layout math of the six card views, mirrored from the boards (JVM-tested). Positions are fractional:
 * `p = index - focus`, so a drag or a spring between two cards moves every card continuously.
 */
internal object CardPoses {
    /** Board perspective, px. */
    const val PERSPECTIVE = 900f

    /** Board stage width, px; the renderer scales board px by `stage width / STAGE_W`. */
    const val STAGE_W = 390f

    /** Cards drawn on each side of the focus: 7 cards in all, whatever the cast size. */
    const val WINDOW = 3

    /** Cards dealt per 타로 hand (board `TA`). */
    const val HAND = 5

    /** Wall columns (board `slot_xy`). */
    const val WALL_COLS = 3

    // ---------------------------------------------------------------- focus and window

    /** [d] wrapped into `[-n/2, n/2)`, for views that loop. */
    fun wrap(d: Float, n: Int): Float {
        if (n <= 0) return 0f
        val r = ((d + n / 2f) % n + n) % n
        return r - n / 2f
    }

    /** The card a continuous position rests on. */
    fun indexAt(pos: Float, n: Int, loop: Boolean): Int {
        if (n <= 0) return 0
        val i = pos.roundToInt()
        return if (loop) Math.floorMod(i, n) else i.coerceIn(0, n - 1)
    }

    /** The shortest position a looping view moves to reach [target] from [pos]. */
    fun nearestPos(pos: Float, target: Int, n: Int, loop: Boolean): Float {
        if (n <= 0) return 0f
        if (!loop) return target.coerceIn(0, n - 1).toFloat()
        val here = pos.roundToInt()
        val delta = wrap((target - Math.floorMod(here, n)).toFloat(), n)
        return here + delta
    }

    /** Offset of card [index] from [pos]; looping views wrap it. */
    fun offset(index: Int, pos: Float, n: Int, loop: Boolean): Float =
        if (loop) wrap(index - pos, n) else index - pos

    /**
     * Indices drawn around [focus]: [before] cards before and [after] after, each once. A looping view takes
     * them round the end; a clamped view stops at the ends.
     */
    fun window(focus: Int, n: Int, loop: Boolean, before: Int = WINDOW, after: Int = WINDOW): List<Int> {
        if (n <= 0) return emptyList()
        if (loop) {
            if (n <= before + after + 1) return (0 until n).toList()
            return (-before..after).map { Math.floorMod(focus + it, n) }
        }
        return (max(0, focus - before)..min(n - 1, focus + after)).toList()
    }

    // ---------------------------------------------------------------- 부채꼴 덱 (board deck_t)

    /** The fan card at offset [p]; [flip] 0..1 lifts the centre card while it turns. Null when too far out. */
    fun fan(p: Float, flip: Float = 0f): CardPose? {
        val ap = abs(p)
        if (!fanShows(p)) return null
        val a = Math.toRadians(p * 9.0)
        val near = max(0f, 1 - ap)
        return CardPose(
            x = (sin(a) * 520).toFloat(),
            y = ((1 - cos(a)) * 520).toFloat() - 30 * near,
            z = -ap * 36 + 40 * near + (sin(PI * flip) * 130).toFloat() * near,
            rotZ = p * 9,
            rotY = -p * 7,
            scale = 1 - min(ap, 3f) * 0.045f,
            dim = min(1f, ap * 0.16f + max(0f, ap - 2.6f) * 1.2f),
        )
    }

    /** The fan card at offset [p] is drawn ([fan] is not null). Allocation-free, for the per-frame checks. */
    fun fanShows(p: Float): Boolean = abs(p) <= FAN_REACH

    private const val FAN_REACH = 3.5f

    // ---------------------------------------------------------------- 회전 목마 (board ring_r, ring_t)

    /** Ring slots: the cast size, at least 3 and at most 12, so a large cast keeps a readable ring. */
    fun ringSlots(n: Int): Int = n.coerceIn(3, 12)

    fun ringRadius(slots: Int): Float {
        val step = 360.0 / max(slots, 3)
        return max(200.0, 84 / tan(Math.toRadians(step / 2)) + 24).toFloat()
    }

    /** The ring card at offset [d] (in cards) on a ring of [slots]. Null when it is behind the ring. */
    fun ring(d: Float, slots: Int): CardPose? {
        val th = ringAngle(d, slots)
        // No backface culling in Compose: a card turned past 90° would read mirrored, so it fades out before.
        if (abs(th) >= RING_CUT) return null
        val t = Math.toRadians(th.toDouble())
        val c = cos(t).toFloat()
        val r = ringRadius(slots)
        return CardPose(
            x = sin(t).toFloat() * r,
            z = (c - 1) * r,
            rotY = th,
            scale = 0.84f + 0.28f * max(0f, c).pow(3),
            dim = min(1f, (1 - c) / 2 * 0.8f),
            alpha = ((RING_CUT - abs(th)) / RING_FADE).coerceIn(0f, 1f),
        )
    }

    /** The ring card at offset [d] can be seen ([ring] is not null and not fully faded). Allocation-free. */
    fun ringShows(d: Float, slots: Int): Boolean = abs(ringAngle(d, slots)) < RING_CUT

    /** Angle of offset [d] on a ring of [slots], degrees in `[-180, 180)`. */
    private fun ringAngle(d: Float, slots: Int): Float {
        val step = 360f / max(slots, 3)
        return ((d * step + 180) % 360 + 360) % 360 - 180
    }

    private const val RING_CUT = 90f
    private const val RING_FADE = 25f

    // ---------------------------------------------------------------- 겹친 더미 (board SLOT, dimv)

    /** Board `SLOT`: (y, z, rotZ) of the top card and the five under it. */
    private val STACK = listOf(
        Triple(0f, 0f, 0f), Triple(20f, -30f, -2.4f), Triple(39f, -60f, 1.9f),
        Triple(57f, -90f, -1.3f), Triple(74f, -120f, 1.6f), Triple(90f, -150f, -1.0f),
    )

    /**
     * The pile card at offset [p] (0 = top, 1 = under it …). Between −1 and 0 the top card is flicked off in
     * an arc to the right and fades; it comes back at the bottom of the pile. Null below the sixth card.
     */
    fun stack(p: Float): CardPose? {
        if (p < -1f || p > STACK.size - 1f) return null
        if (p < 0f) {
            val t = -p
            return CardPose(x = t * 360, y = -t * (1 - t) * 120, z = t * 20, rotZ = t * 18, alpha = 1 - t)
        }
        val k = floor(p).toInt().coerceAtMost(STACK.size - 1)
        val f = p - k
        val a = STACK[k]
        val b = STACK[min(k + 1, STACK.size - 1)]
        val z = lerp(a.second, b.second, f)
        return CardPose(
            y = -lerp(a.first, b.first, f),
            z = z,
            rotZ = lerp(a.third, b.third, f),
            dim = (-z / 150 * 0.36f).coerceIn(0f, 0.4f),
            alpha = if (p > STACK.size - 2f) STACK.size - 1f - p else 1f,
        )
    }

    /** The pile card at offset [p] can be seen ([stack] is not null and not fully faded). Allocation-free. */
    fun stackShows(p: Float): Boolean = p > -1f && p < STACK.size - 1f

    // ---------------------------------------------------------------- 타로 펼침 (board TA, TR, tslot, tdeck)

    private val TAROT_ANGLES = listOf(-16f, -8f, 0f, 8f, 16f)
    private const val TAROT_R = 400f
    private const val TAROT_STEP = 8f

    /** Board `tdeck`: each slot's small turn while it waits in the deck. */
    private val TAROT_DECK_ROT = floatArrayOf(-2f, 1.5f, -1f, 2.5f, 0f)

    /** The hand (of [HAND] cards) that card [index] is dealt in. */
    fun handOf(index: Int): Int = index / HAND

    /** The card indices of hand [hand]. */
    fun hand(hand: Int, n: Int): IntRange = (hand * HAND) until min(n, (hand + 1) * HAND)

    /**
     * Slot [slot] of a hand of [count] cards on the arc, raised when [focus]. [deal] 0..1 moves it from the
     * face-down deck below the arc to its slot.
     */
    fun tarot(slot: Int, count: Int, focus: Boolean, deal: Float = 1f): CardPose {
        // A full hand uses the board angles; a short last hand stays centred on the same 8° steps.
        val angle = if (count >= HAND) TAROT_ANGLES.getOrElse(slot) { 0f } else (slot - (count - 1) / 2f) * TAROT_STEP
        val a = Math.toRadians(angle.toDouble())
        var y = -6 + TAROT_R * (1 - cos(a)).toFloat()
        var z = 1f
        var rx = 0f
        if (focus) {
            y -= 8
            z = 16f
            rx = -12f
        }
        val dealt = CardPose(x = TAROT_R * sin(a).toFloat(), y = y, z = z, rotZ = angle, rotX = rx)
        if (deal >= 1f) return dealt
        val deck = CardPose(y = 84f + 120f, z = 1 + slot * 2.5f, rotZ = TAROT_DECK_ROT.getOrElse(slot) { 0f })
        return mix(deck, dealt, deal.coerceIn(0f, 1f))
    }

    // ---------------------------------------------------------------- 기울어진 벽 (board ZS, RX0, RY0, KX, KY)

    /** Board `ZS`: each slot's depth in the wall, so the wall reads as relief. */
    private val WALL_Z = listOf(0f, 12f, 4f, 16f, 2f, 10f, 14f, 6f, 0f, 10f, 4f, 16f)

    fun wallColumn(index: Int): Int = index % WALL_COLS

    fun wallRow(index: Int): Int = index / WALL_COLS

    fun wallDepth(index: Int): Float = WALL_Z[index % WALL_Z.size]

    /** The wall's turn with the tilt (board `rotateX(12 - ty*8)`), degrees; two floats, no pair per sample. */
    fun wallRotX(tiltY: Float): Float = 12 - tiltY * 8

    /** The wall's turn with the tilt (board `rotateY(-24 + tx*12)`), degrees. */
    fun wallRotY(tiltX: Float): Float = -24 + tiltX * 12

    /** A wall drag past the first or last row follows this far, in rows. */
    const val WALL_DRAG_OVER = 0.4f

    /** A released wall drag carries on this far in its direction before rounding, so a short flick moves a row. */
    const val WALL_DRAG_BIAS = 0.35f

    /** The wall row under a dragging finger ([row]: fractional), held near the ends. */
    fun wallDragRow(row: Float, lastRow: Int): Float = row.coerceIn(-WALL_DRAG_OVER, lastRow + WALL_DRAG_OVER)

    /** The row a released wall drag settles on: [row] carried on in [direction], clamped to the wall. */
    fun wallDragTarget(row: Float, direction: Float, lastRow: Int): Int =
        (row + WALL_DRAG_BIAS * sign(direction)).roundToInt().coerceIn(0, max(0, lastRow))

    /** The card at [column] of [row], or the last card when that row is short. */
    fun wallIndex(row: Int, column: Int, n: Int): Int = (row * WALL_COLS + column).coerceIn(0, max(0, n - 1))

    /** Rows of the wall drawn around the focus row: two above and two below. */
    fun wallWindow(focus: Int, n: Int): List<Int> {
        if (n <= 0) return emptyList()
        val row = wallRow(focus)
        val first = max(0, row - 2) * WALL_COLS
        val last = min(n - 1, (row + 3) * WALL_COLS - 1)
        return (first..last).toList()
    }

    /** Light band position over a card, 0..1 across it, moving with the tilt (holo sheen). */
    fun sheen(tiltX: Float, column: Int = 1): Float = (0.5f + tiltX * 0.45f + (column - 1) * 0.12f).coerceIn(-0.2f, 1.2f)

    // ---------------------------------------------------------------- 롤로덱스 (vertical drum)

    private const val DRUM_STEP = 24f
    private const val DRUM_R = 260f

    /**
     * The drum card at offset [p]: the front card at 0; the next cards wait above and behind; a passed card
     * flips down towards the viewer and fades. Null beyond the window.
     */
    fun rolodex(p: Float): CardPose? {
        if (p < -1.2f || p > WINDOW + 0.5f) return null
        val th = p * DRUM_STEP
        val t = Math.toRadians(th.toDouble())
        val c = cos(t).toFloat()
        return CardPose(
            y = -sin(t).toFloat() * DRUM_R * 0.55f,
            z = (c - 1) * DRUM_R,
            rotX = if (p < 0) p * 80f else th,
            dim = min(0.6f, max(0f, p) * 0.16f),
            alpha = if (p < 0) (1 + p).coerceIn(0f, 1f) else if (p > WINDOW) (WINDOW + 0.5f - p) * 2 else 1f,
        )
    }

    /** The drum card at offset [p] can be seen ([rolodex] is not null and not fully faded). Allocation-free. */
    fun rolodexShows(p: Float): Boolean = p > -1f && p < WINDOW + 0.5f

    /** The index letter of a title: its first Hangul initial (`ㄹ` for 리엔), a Latin capital, or `#`. */
    fun indexLetter(title: String): String {
        val c = title.trim().firstOrNull() ?: return "#"
        Hangul.initialOf(c)?.let { return it.toString() }
        if (Hangul.isInitialJamo(c)) return c.toString()
        if (c in 'a'..'z' || c in 'A'..'Z') return c.uppercaseChar().toString()
        return "#"
    }

    /** The 초성 index row: each letter once, in card order, with the first card that has it. */
    fun indexRow(titles: List<String>): List<Pair<String, Int>> {
        val seen = LinkedHashMap<String, Int>()
        titles.forEachIndexed { i, t -> seen.getOrPut(indexLetter(t)) { i } }
        return seen.entries.map { it.key to it.value }
    }

    // ---------------------------------------------------------------- helpers

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun mix(a: CardPose, b: CardPose, t: Float): CardPose = CardPose(
        x = lerp(a.x, b.x, t), y = lerp(a.y, b.y, t), z = lerp(a.z, b.z, t),
        rotX = lerp(a.rotX, b.rotX, t), rotY = lerp(a.rotY, b.rotY, t), rotZ = lerp(a.rotZ, b.rotZ, t),
        scale = lerp(a.scale, b.scale, t), dim = lerp(a.dim, b.dim, t), alpha = lerp(a.alpha, b.alpha, t),
    )

    /** Draw order: nearer cards over farther ones. */
    fun drawOrder(pose: CardPose?): Float = pose?.z ?: -10_000f
}
