package io.github.lenticualrstone.civilight.designsystem.card

import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken
import kotlin.math.roundToInt

/*
 * Shared copy of feature:canvas `card/CardRules.kt` (motion audit §3.2 promotion; the gallery spec §3.5
 * asks for one shared `SnapCardRow`). Model-free parts only: `ViewChoice` stays in the lanes because it
 * needs core:model. Lanes switch to this package and delete their copies in wave 1.
 */

/**
 * Card kinds of the card views (CNV-23) and their reference sizes in dp (CNV-22 "card pitch").
 */
enum class CardKind {
    /** `ProjectCard` 3:4: cover 222 × 296, inner 236 × 336. */
    PROJECT,

    /** 최근 연 파일 card: image cover 150 × 100, inner 176 × 110, text below. */
    RECENT,

    /** Portrait card 4:5 of the primary group: 196 × 245. */
    PORTRAIT,

    /** Portrait card 4:5 of other groups: 132 × 165. */
    PORTRAIT_SMALL,
}

/** A size in dp. */
data class CardSize(val width: Float, val height: Float)

object CardMetrics {
    /** Image size of a card (for [CardKind.RECENT] the image only; its text sits below). */
    fun size(kind: CardKind, inner: Boolean): CardSize = when (kind) {
        CardKind.PROJECT -> if (inner) CardSize(236f, 336f) else CardSize(222f, 296f)
        CardKind.RECENT -> if (inner) CardSize(176f, 110f) else CardSize(150f, 100f)
        CardKind.PORTRAIT -> CardSize(196f, 245f)
        CardKind.PORTRAIT_SMALL -> CardSize(132f, 165f)
    }

    /** Gap between cards: 14 cover, 16 inner. */
    fun gap(inner: Boolean): Float = if (inner) 16f else 14f

    /** Card width + gap: the distance between two card starts (`236` for project cards on the cover). */
    fun pitch(kind: CardKind, inner: Boolean): Float = size(kind, inner).width + gap(inner)

    /** Where a row starts: the content inset, 20 cover, 32 inner. */
    fun inset(inner: Boolean): Float = if (inner) 32f else 20f

    /** Radius of the card image: 20 for project and portrait cards, 14 for recent-file images. */
    fun radius(kind: CardKind): Float = if (kind == CardKind.RECENT) 14f else 20f
}

/**
 * Snapping of a horizontal card row (CNV-22, T-CNV-41): a fling or drag ends with the nearest card's
 * left edge at the row start (deceleration 320 ms), and one haptic tick per snap.
 */
object CardSnap {
    const val DECELERATE_MS = 320

    /** The settle motion: the DS decelerate (`enter`) curve over [DECELERATE_MS]. */
    val MOTION: MotionToken = CivilightMotion.enter

    /** The scroll offset a row settles at after ending at [offset]: the nearest multiple of [pitch]. */
    fun target(offset: Float, pitch: Float, count: Int = Int.MAX_VALUE): Float {
        if (pitch <= 0f) return 0f
        val last = (count - 1).coerceAtLeast(0)
        val index = (offset / pitch).roundToInt().coerceIn(0, last)
        return index * pitch
    }

    /** Index of the card at the row start for a settled [offset]. */
    fun indexAt(offset: Float, pitch: Float): Int = if (pitch <= 0f) 0 else (offset / pitch).roundToInt().coerceAtLeast(0)
}

/** One haptic tick per snap: a tick when a scroll in progress comes to rest (CNV-22). Kept under reduced motion. */
class SnapTicker {
    private var moving = false

    /** Feed every change of "scroll in progress"; true when this change is the end of a snap. */
    fun onScrolling(inProgress: Boolean): Boolean {
        val tick = moving && !inProgress
        moving = inProgress
        return tick
    }
}

/**
 * The image of a card moves less than the card (parallax 8 %, CNV-22, T-CNV-42), under a constant scale of
 * 1.1 so the shifted image never shows its edge; reduce motion: no shift and scale 1.
 *
 * The 1.1 scale (about the image centre) leaves only `(scale - 1) / 2` of the image width beyond each card
 * edge, 5 %. The raw shift `-displacement × 8 %` passes that after about 0.6 of a pitch (the second card at
 * rest is displaced a whole pitch: 18.9 dp against an 11.1 dp margin), which bared a strip at the card's end.
 * With the image width given, the shift is clamped to that margin, so the image always covers its card.
 */
object CardParallax {
    const val FACTOR = CivilightMotion.cardParallax

    /**
     * The room the scaled image has on each side of its card: `(scale - 1) / 2 × imageWidth`, less [guard]
     * (a pixel against an anti-aliased seam at the clip), never negative. Same unit as [imageWidth].
     */
    fun maxShift(imageWidth: Float, reduced: Boolean, guard: Float = 0f): Float =
        ((imageScale(reduced) - 1f) / 2f * imageWidth - guard).coerceAtLeast(0f)

    /**
     * The image's shift relative to its card when the card is displaced by [cardDisplacement] (any unit),
     * clamped to [maxShift] of [imageWidth] (same unit; unbounded when not given).
     */
    fun imageShift(
        cardDisplacement: Float,
        reduced: Boolean,
        imageWidth: Float = Float.POSITIVE_INFINITY,
        guard: Float = 0f,
    ): Float {
        if (reduced) return 0f
        val raw = -cardDisplacement * FACTOR
        if (imageWidth == Float.POSITIVE_INFINITY) return raw
        val limit = maxShift(imageWidth, reduced, guard)
        return raw.coerceIn(-limit, limit)
    }

    /** The image's displacement relative to the screen. */
    fun imageOnScreen(
        cardDisplacement: Float,
        reduced: Boolean,
        imageWidth: Float = Float.POSITIVE_INFINITY,
        guard: Float = 0f,
    ): Float = cardDisplacement + imageShift(cardDisplacement, reduced, imageWidth, guard)

    /** The constant image scale: [CivilightMotion.cardImageScale], or 1 when reduced. */
    fun imageScale(reduced: Boolean): Float = if (reduced) 1f else CivilightMotion.cardImageScale
}

/**
 * Fitting a card row's card to its screen (GAL-07, CNV-22): the card keeps its ratio and never grows past its
 * reference size, but shrinks so that the whole card stands between the top bar and the bottom bar and the
 * next card still peeks at the right. Without it the inner 236 × 315 card ran under the command bar on an
 * unfolded screen held landscape (about 700 dp tall), and the 설명 판 under it was out of sight.
 */
object CardFit {
    /** The least card height (dp): the small group card (132 × 176); below it a row scrolls instead. */
    const val MIN_HEIGHT = 176f

    /** How much of the next card shows at least (dp). */
    const val MIN_PEEK = 40f

    /**
     * The height (dp) a card may take: the viewport between the bars ([viewportHeight] less [barsTop] and
     * [barsBottom]) less the rows above it ([head]: filter chips, order line) and the row's own room
     * ([rowExtra]: room for the picked card's lift above, the gap below). When the rows above leave less than
     * [minHeight], they are left out (they scroll away and the row alone fits).
     */
    fun room(
        viewportHeight: Float,
        barsTop: Float,
        barsBottom: Float,
        head: Float,
        rowExtra: Float,
        minHeight: Float = MIN_HEIGHT,
    ): Float {
        val between = viewportHeight - barsTop - barsBottom - rowExtra
        val withHead = between - head
        return if (withHead >= minHeight) withHead else between
    }

    /**
     * The card size for [spec] in a row [rowWidth] wide that starts at [inset] with [gap] between cards and
     * [roomHeight] of height ([room]): [spec] scaled down (ratio kept) to fit the height and to leave [minPeek]
     * of the next card; never larger than [spec], never lower than [minHeight] (or [spec]'s own height when
     * that is lower).
     */
    fun fit(
        spec: CardSize,
        roomHeight: Float,
        rowWidth: Float,
        inset: Float,
        gap: Float,
        minHeight: Float = MIN_HEIGHT,
        minPeek: Float = MIN_PEEK,
    ): CardSize {
        if (spec.width <= 0f || spec.height <= 0f) return spec
        val ratio = spec.width / spec.height
        val widthRoom = rowWidth - inset - gap - minPeek
        val byWidth = if (widthRoom > 0f) widthRoom / ratio else 0f
        val floor = minOf(minHeight, spec.height)
        val height = minOf(spec.height, roomHeight, byWidth).coerceAtLeast(floor)
        if (height >= spec.height) return spec
        return CardSize(height * ratio, height)
    }
}
