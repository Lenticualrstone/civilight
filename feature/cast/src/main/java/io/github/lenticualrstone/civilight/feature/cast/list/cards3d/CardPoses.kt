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
 * Where one card sits in a 3D card view (U-16, CST-67), in board px of the board stage (`scr_u16_cards_a/b/c.py`):
 * its `translate3d` from its resting pivot, its own turns, `scale`, [dim] (0 = full light) and [alpha]. The
 * [Projector] turns it into a layer's transform under the board's shared perspective. Mutable and reused per
 * card, so the per-frame pose functions allocate nothing (pass `into`).
 */
internal class CardPose(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var rotX: Float = 0f,
    var rotY: Float = 0f,
    var rotZ: Float = 0f,
    var scale: Float = 1f,
    /** 0 = full brightness, 1 = fully shaded. */
    var dim: Float = 0f,
    var alpha: Float = 1f,
) {
    fun set(x: Float = 0f, y: Float = 0f, z: Float = 0f, rotX: Float = 0f, rotY: Float = 0f, rotZ: Float = 0f, scale: Float = 1f, dim: Float = 0f, alpha: Float = 1f): CardPose {
        this.x = x
        this.y = y
        this.z = z
        this.rotX = rotX
        this.rotY = rotY
        this.rotZ = rotZ
        this.scale = scale
        this.dim = dim
        this.alpha = alpha
        return this
    }

    /** [scale] with the board perspective applied to [z] alone (no rig turn): for ordering and tests. */
    val projected: Float get() = scale * CardPoses.PERSPECTIVE / (CardPoses.PERSPECTIVE - z.coerceAtMost(CardPoses.PERSPECTIVE / 2))
}

/**
 * Pure layout math of the six card views, ported from the boards (JVM-tested). Positions are fractional:
 * `p = index - focus`, so a drag or a spring between two cards moves every card continuously.
 */
internal object CardPoses {
    /** 부채꼴 덱 board perspective, px. */
    const val PERSPECTIVE = 1000f

    /** Board stage width, px; the renderer scales board px by `stage width / STAGE_W`. */
    const val STAGE_W = 390f

    /**
     * Px per board px: the density itself. The card views never scale the rig to fit (QA 0.2.1: a 46 % card on
     * the inner display); the card is the board's size in dp everywhere.
     */
    fun boardUnit(density: Float): Float = density

    /** The cast list's stage area (stage + readout + buttons) never gets shorter than this; the list scrolls. */
    const val STAGE_MIN_H = 470f

    /**
     * The stage area a view needs at 1:1 (the part of its board the cards reach, plus that view's own dock),
     * so a short pane (the inner display) scrolls instead of letting the cards run over the chips or the dock.
     */
    fun stageMin(style: io.github.lenticualrstone.civilight.model.app.CardStyle): Float = when (style) {
        io.github.lenticualrstone.civilight.model.app.CardStyle.FAN_DECK -> 380f + 100f
        io.github.lenticualrstone.civilight.model.app.CardStyle.CAROUSEL -> 340f + 100f
        io.github.lenticualrstone.civilight.model.app.CardStyle.STACK -> 470f + 90f
        io.github.lenticualrstone.civilight.model.app.CardStyle.TAROT -> 330f + 210f
        io.github.lenticualrstone.civilight.model.app.CardStyle.TILTED_WALL -> 525f + 100f
        io.github.lenticualrstone.civilight.model.app.CardStyle.ROLODEX -> 440f + 110f
        io.github.lenticualrstone.civilight.model.app.CardStyle.LIST -> STAGE_MIN_H
    }

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

    /** Board `.deck .cd`: 220 × 308, radius 18. */
    const val DECK_W = 220f
    const val DECK_H = 308f

    /** The fan card at offset [p] (board `deck_t`); [flip] 0..1 lifts the centre card while it turns. Null past 3.5 cards. */
    fun fan(p: Float, flip: Float = 0f, into: CardPose = CardPose()): CardPose? {
        val ap = abs(p)
        if (!fanShows(p)) return null
        val a = Math.toRadians(p * 9.0)
        val near = max(0f, 1 - ap)
        return into.set(
            x = (sin(a) * 520).toFloat(),
            y = ((1 - cos(a)) * 520).toFloat() - 30 * near,
            z = -ap * 36 + 40 * near + (sin(PI * flip) * 130).toFloat() * near,
            rotZ = p * 9,
            rotY = -p * 7,
            scale = 1 - min(ap, 3f) * 0.045f,
            dim = min(1f, ap * 0.16f + max(0f, ap - 2.6f) * 1.2f),
        )
    }

    /** The fan card at offset [p] is drawn ([fan] is not null). */
    fun fanShows(p: Float): Boolean = abs(p) <= FAN_REACH

    private const val FAN_REACH = 3.5f

    /** Board drag: px of finger travel per card, and the pull past either end (`c *= 0.3`). */
    const val FAN_DRAG_PX = 120f
    const val RING_DRAG_PX = 140f
    const val RUBBER = 0.3f

    /** A released drag: `round(cur + v × 0.16)`, at most two cards from where it was let go (board `up`). */
    fun flingTarget(pos: Float, velocityCards: Float, n: Int, loop: Boolean): Int {
        val here = pos.roundToInt()
        var t = (pos + velocityCards * FLING_TIME).roundToInt()
        if (!loop) t = t.coerceIn(0, max(0, n - 1))
        return t.coerceIn(here - 2, here + 2)
    }

    private const val FLING_TIME = 0.16f

    /** A clamped deck dragged to [raw] (cards) follows at a third past its ends (board rubber band). */
    fun rubber(raw: Float, n: Int): Float {
        val mx = max(0, n - 1).toFloat()
        return when {
            raw < 0f -> raw * RUBBER
            raw > mx -> mx + (raw - mx) * RUBBER
            else -> raw
        }
    }

    // ---------------------------------------------------------------- 회전 목마 (board ring_r, ring_t)

    /** Board `.ring .cd`: 168 × 236, radius 16. */
    const val RING_W = 168f
    const val RING_H = 236f

    /** Ring slots: the cast size, at least 3 and at most 12, so a large cast keeps a readable ring. */
    fun ringSlots(n: Int): Int = n.coerceIn(3, 12)

    /** Board `ring_r`. */
    fun ringRadius(slots: Int): Float {
        val step = 360.0 / max(slots, 3)
        return max(200.0, 84 / tan(Math.toRadians(step / 2)) + 24).toFloat()
    }

    /**
     * The ring card at offset [d] (in cards) on a ring of [slots] (board `ring_t`). Every card is drawn: one
     * turned past 90° shows its patterned back, as the board's `backface-visibility` does.
     */
    fun ring(d: Float, slots: Int, into: CardPose = CardPose()): CardPose {
        val th = ringAngle(d, slots)
        val t = Math.toRadians(th.toDouble())
        val c = cos(t).toFloat()
        val r = ringRadius(slots)
        return into.set(
            x = sin(t).toFloat() * r,
            z = (c - 1) * r,
            rotY = th,
            scale = 0.84f + 0.28f * max(0f, c).pow(3),
            dim = min(1f, (1 - c) / 2 * 0.8f),
        )
    }

    /** Angle of offset [d] on a ring of [slots], degrees in `[-180, 180)`. */
    fun ringAngle(d: Float, slots: Int): Float {
        val step = 360f / max(slots, 3)
        return ((d * step + 180) % 360 + 360) % 360 - 180
    }

    /** The ring window: every slot of the ring, centred on the focus. */
    fun ringWindow(focus: Int, n: Int): List<Int> {
        val slots = min(n, ringSlots(n))
        return window(focus, n, loop = true, before = (slots - 1) / 2, after = slots / 2)
    }

    // ---------------------------------------------------------------- 겹친 더미 (board SLOT, dimv, next/prev)

    /** Board `.sk`: 270 × 384, radius 22, turning about 50 % · 60 %. */
    const val PILE_W = 270f
    const val PILE_H = 384f
    const val PILE_PIVOT_Y = 0.6f

    /** Board `SLOT`: (y, z, rotZ) of the top card and the five under it. */
    private val STACK_Y = floatArrayOf(0f, 20f, 39f, 57f, 74f, 90f)
    private val STACK_Z = floatArrayOf(0f, -30f, -60f, -90f, -120f, -150f)
    private val STACK_RZ = floatArrayOf(0f, -2.4f, 1.9f, -1.3f, 1.6f, -1.0f)
    private const val STACK_LAST = 5

    /** Board `dimv`: a card deeper in the pile is shaded, at most 0.4. */
    fun pileDim(z: Float): Float = (-z / 150 * 0.36f).coerceIn(0f, 0.4f)

    /**
     * The pile card at offset [p] (0 = top, 1 = under it …) of a pile of [n]. Between −0.5 and 0 the top card
     * is thrown along the board's arc towards [dir] (−1 left, the buttons; +1 right); between −1 and −0.5 it
     * comes back from behind the pile and settles at the bottom (board `next`: tween 380 ms then a spring
     * from `(dir·340, 90, −330)`). A card at the bottom past the sixth stays out of sight.
     */
    fun stack(p: Float, dir: Float = -1f, n: Int = STACK_LAST + 1, into: CardPose = CardPose()): CardPose? {
        if (p <= -1f || p > STACK_LAST + 1f) return null
        val bottom = min(max(n - 1, 0), STACK_LAST).toFloat()
        if (p < -0.5f) {
            // Back from behind the pile into the bottom slot: t runs 0 → 1 as p goes −0.5 → −1.
            val t = (-0.5f - p) / 0.5f
            slotAt(bottom, into)
            val by = into.y
            val bz = into.z
            val brz = into.rotZ
            val z = lerp(-330f, bz, t)
            return into.set(
                x = lerp(dir * 340f, 0f, t),
                y = lerp(90f, by, t),
                z = z,
                rotZ = lerp(dir * 10f, brz, t),
                dim = pileDim(z),
                // It comes in from beyond the stage (faded in, for screens wider than the board); a pile longer
                // than six takes it on under the sixth, out of sight.
                alpha = (t / EDGE_FADE).coerceIn(0f, 1f) * (if (n - 1 > STACK_LAST) 1f - t else 1f),
            )
        }
        if (p < 0f) {
            // Thrown: a quadratic arc from the top slot through (dir·210, −190) to (dir·540, −50).
            val q = -p / 0.5f
            val e = 1 - (1 - q).pow(2.4f)
            val u = 1 - e
            return into.set(
                x = 2 * u * e * dir * 210f + e * e * dir * 540f,
                y = 2 * u * e * -190f + e * e * -50f,
                z = 70f * e,
                rotX = 10f * e,
                rotY = -dir * 22f * e,
                rotZ = dir * 34f * e,
                // The arc ends beyond the stage; on a screen wider than the board it fades out there.
                alpha = ((1f - q) / EDGE_FADE).coerceIn(0f, 1f),
            )
        }
        slotAt(min(p, STACK_LAST.toFloat()), into)
        into.dim = pileDim(into.z)
        // Past the sixth card a card fades out at the back of the pile.
        into.alpha = if (p > STACK_LAST) (STACK_LAST + 1f - p).coerceIn(0f, 1f) else 1f
        return into
    }

    /** Share of a throw (or of the way back) faded at the stage's edge. */
    private const val EDGE_FADE = 0.3f

    /** Board `SLOT` at a fractional slot. */
    private fun slotAt(k: Float, into: CardPose): CardPose {
        val i = floor(k).toInt().coerceIn(0, STACK_LAST)
        val j = min(i + 1, STACK_LAST)
        val f = k - i
        return into.set(y = lerp(STACK_Y[i], STACK_Y[j], f), z = lerp(STACK_Z[i], STACK_Z[j], f), rotZ = lerp(STACK_RZ[i], STACK_RZ[j], f))
    }

    /** The pile card at offset [p] can be seen. */
    fun stackShows(p: Float): Boolean = p > -1f && p < STACK_LAST + 1f

    /** Board drag of the top card: it follows the finger (`x = dx, y = dy·0.35, z 24, rz dx·0.07, ry dx·0.06`). */
    fun pileDrag(dx: Float, dy: Float, into: CardPose): CardPose =
        into.set(x = dx, y = dy * 0.35f, z = 24f, rotZ = dx * 0.07f, rotY = dx * 0.06f)

    /** While the top card is dragged the next one rises halfway: offset 1 − this (board `p`). */
    fun pileRise(dx: Float): Float = min(1f, abs(dx) / 160f) * 0.5f

    /** A released drag throws the card past 110 px or 0.55 px/ms in its direction (board `up`). */
    fun pileThrows(dx: Float, velocityPxPerMs: Float): Boolean =
        abs(dx) > 110f || (abs(velocityPxPerMs) > 0.55f && sign(velocityPxPerMs) == sign(dx))

    // ---------------------------------------------------------------- 타로 펼침 (board TA, TR, tslot, tdeck)

    /** Board `.tc`: 84 × 126, radius 10, turning about its bottom centre. */
    const val TAROT_W = 84f
    const val TAROT_H = 126f

    private val TAROT_ANGLES = floatArrayOf(-16f, -8f, 0f, 8f, 16f)
    private const val TAROT_R = 400f
    private const val TAROT_STEP = 8f

    /** Board `tdeck`: each slot's small turn while it waits in the deck. */
    private val TAROT_DECK_ROT = floatArrayOf(-2f, 1.5f, -1f, 2.5f, 0f)

    /** The hand (of [HAND] cards) that card [index] is dealt in. */
    fun handOf(index: Int): Int = index / HAND

    /** How many hands of [HAND] deal [n] cards (at least one). */
    fun hands(n: Int): Int = max(1, (n + HAND - 1) / HAND)

    /** The card indices of hand [hand]. */
    fun hand(hand: Int, n: Int): IntRange = (hand * HAND) until min(n, (hand + 1) * HAND)

    /** Arc angle of [slot] in a hand of [count]: the board angles, or 8° steps centred for a short hand. */
    fun tarotAngle(slot: Int, count: Int): Float =
        if (count >= HAND) TAROT_ANGLES.getOrElse(slot) { 0f } else (slot - (count - 1) / 2f) * TAROT_STEP

    /** Board `tslot` (raised by `place` when [focus]): x, y, z, rotZ, rotX on the table. */
    fun tarot(slot: Int, count: Int, focus: Boolean, into: CardPose = CardPose()): CardPose {
        val angle = tarotAngle(slot, count)
        val a = Math.toRadians(angle.toDouble())
        var y = -6 + TAROT_R * (1 - cos(a)).toFloat()
        var z = 1f
        var rx = 0f
        if (focus) {
            y -= 8
            z = 16f
            rx = -12f
        }
        return into.set(x = TAROT_R * sin(a).toFloat(), y = y, z = z, rotZ = angle, rotX = rx)
    }

    /** Board `tdeck`: the face-down deck below the arc. */
    fun tarotDeck(slot: Int, into: CardPose = CardPose()): CardPose =
        into.set(y = 84f, z = 1 + slot * 2.5f, rotZ = TAROT_DECK_ROT.getOrElse(slot) { 0f })

    /** Height a card rises to mid-deal (board `z: 90`) and mid-turn (`z: 74`). */
    const val TAROT_DEAL_LIFT = 90f
    const val TAROT_TURN_LIFT = 74f

    // ---------------------------------------------------------------- 기울어진 벽 (board CW, CH, GAP, ZS, RX0, RY0, KX, KY)

    /** Board wall slot: 92 × 123, gap 11, radius 14. */
    const val WALL_CW = 92f
    const val WALL_CH = 123f
    const val WALL_GAP = 11f

    /** Rows of the wall in view at once (board: 4 rows, 525 px). */
    const val WALL_ROWS = 4

    /** Board pulled-out card: 264 × 352, radius 20. */
    const val PULL_W = 264f
    const val PULL_H = 352f

    /** Board `ZS`: each slot's depth in the wall, so the wall reads as relief. */
    private val WALL_Z = floatArrayOf(0f, 12f, 4f, 16f, 2f, 10f, 14f, 6f, 0f, 10f, 4f, 16f)

    fun wallColumn(index: Int): Int = index % WALL_COLS

    fun wallRow(index: Int): Int = index / WALL_COLS

    fun wallDepth(index: Int): Float = WALL_Z[index % WALL_Z.size]

    /** Board `slot_xy`: a slot's top-left in the wall. */
    fun wallSlotX(index: Int): Float = wallColumn(index) * (WALL_CW + WALL_GAP)

    fun wallSlotY(row: Float): Float = row * (WALL_CH + WALL_GAP)

    /** The wall's turn with the tilt (board `rotateX(12 - ty*8)`), degrees. */
    fun wallRotX(tiltY: Float): Float = 12 - tiltY * 8

    /** The wall's turn with the tilt (board `rotateY(-24 + tx*12)`), degrees. */
    fun wallRotY(tiltX: Float): Float = -24 + tiltX * 12

    /** The first row in view: the focus row kept inside the four rows shown. */
    fun wallTopRow(focusRow: Int, lastRow: Int): Int = (focusRow - 1).coerceIn(0, max(0, lastRow - WALL_ROWS + 1))

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

    /** Cards of the rows drawn around the top row in view: one row more each side, for a moving wall. */
    fun wallWindow(topRow: Int, n: Int): List<Int> {
        if (n <= 0) return emptyList()
        val first = max(0, topRow - 1) * WALL_COLS
        val last = min(n - 1, (topRow + WALL_ROWS + 1) * WALL_COLS - 1)
        return (first..last).toList()
    }

    /** A pulled card dragged sideways past this many px steps to the next one (board `fup`: 56). */
    const val PULL_STEP_PX = 56f

    /** A pulled card turns with a sideways drag (board `rotateY(drag · 0.09)`). */
    const val PULL_DRAG_TURN = 0.09f

    // ---------------------------------------------------------------- 롤로덱스 (board .rc.d0 … d4, .dp)

    /** Board `.rc`: 342 × 216, radius 16, turning about its bottom centre; the tab rises 24 above it. */
    const val ROLO_W = 342f
    const val ROLO_H = 216f
    const val ROLO_TAB = 24f

    /** Board slots `d0 … d4`: (y, z, rotX, opacity). */
    private val ROLO_Y = floatArrayOf(0f, -40f, -74f, -102f, -124f)
    private val ROLO_Z = floatArrayOf(0f, -46f, -92f, -138f, -184f)
    private val ROLO_RX = floatArrayOf(0f, 9f, 16f, 22f, 27f)
    private val ROLO_A = floatArrayOf(1f, 1f, 0.92f, 0.8f, 0f)
    private const val ROLO_LAST = 4

    /** Board `.dp`: a passed card falls towards the viewer to `(0, 70, 40) rotateX(−104°)` and fades late. */
    private const val PASS_Y = 70f
    private const val PASS_Z = 40f
    private const val PASS_RX = -104f

    /**
     * The drum card at offset [p] (0 = the front card): the next ones wait above and behind in the board slots;
     * a passed card ([p] between −1 and 0) falls down towards the viewer and fades. Null outside.
     */
    fun rolodex(p: Float, into: CardPose = CardPose()): CardPose? {
        if (p <= -1f || p >= ROLO_LAST) return null
        if (p < 0f) {
            val t = -p
            // The board fades a falling card in its last 140 ms of 460 ms.
            val fade = ((1f - t) / 0.3f).coerceIn(0f, 1f)
            return into.set(y = PASS_Y * t, z = PASS_Z * t, rotX = PASS_RX * t, alpha = fade)
        }
        val i = floor(p).toInt().coerceIn(0, ROLO_LAST)
        val j = min(i + 1, ROLO_LAST)
        val f = p - i
        return into.set(
            y = lerp(ROLO_Y[i], ROLO_Y[j], f),
            z = lerp(ROLO_Z[i], ROLO_Z[j], f),
            rotX = lerp(ROLO_RX[i], ROLO_RX[j], f),
            alpha = lerp(ROLO_A[i], ROLO_A[j], f),
        )
    }

    /** The drum card at offset [p] can be seen ([rolodex] is not null and not fully faded). */
    fun rolodexShows(p: Float): Boolean = p > -1f && p < ROLO_LAST.toFloat()

    /** Behind the front card a card shows only its paper and tab (board `.d1:not(.rv) .pp > * { opacity: 0 }`). */
    fun rolodexContent(p: Float): Float = (1f - p).coerceIn(0f, 1f)

    /** Board drag: px per card (`FIX(p)`: `p = |dy| / 190`), and the 64 px a release needs to turn a card. */
    const val ROLO_DRAG_PX = 190f
    const val ROLO_TURN_PX = 64f

    /** Board tab x by card position (`TABX[pos % 3]`). */
    fun rolodexTabX(index: Int): Float = ROLO_TABS[index % ROLO_TABS.size]

    private val ROLO_TABS = floatArrayOf(16f, 122f, 228f)

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

    // ---------------------------------------------------------------- tilt

    /** Idle sway of a stage while nothing tilts it (boards: `0.45 sin(2πt/9 s)`, `0.3 sin(2πt/6.5 s)`). */
    fun swayX(ms: Long, periodMs: Int, amp: Float): Float = (amp * sin(2 * PI * (ms % periodMs) / periodMs)).toFloat()

    // ---------------------------------------------------------------- helpers

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
