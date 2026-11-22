package io.github.lenticualrstone.civilight.feature.canvas.card

import io.github.lenticualrstone.civilight.designsystem.component.avatarGlyph
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken
import io.github.lenticualrstone.civilight.model.app.ViewMode
import kotlin.math.roundToInt

/**
 * Card kinds of the card views (CNV-23) and their reference sizes in dp (CNV-22 "card pitch").
 * Project-type-free: the 서재 (LIB-04), 인물 (CST-04) and page rows can share it once it moves to a
 * shared module (Requests).
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

/** One haptic tick per snap: a tick when a scroll in progress comes to rest (CNV-22). */
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
 * The image of a card moves less than the card (parallax 8 %, CNV-22, T-CNV-42); reduce motion: none.
 */
object CardParallax {
    const val FACTOR = 0.08f

    /** The image's shift relative to its card when the card is displaced by [cardDisplacement] dp. */
    fun imageShift(cardDisplacement: Float, reduced: Boolean): Float = if (reduced) 0f else -cardDisplacement * FACTOR

    /** The image's displacement relative to the screen. */
    fun imageOnScreen(cardDisplacement: Float, reduced: Boolean): Float = cardDisplacement + imageShift(cardDisplacement, reduced)
}

/**
 * `목록 | 카드` (CNV-21): one choice per screen, kept in app preferences (`AppSettings.viewByScreen`),
 * never in `.civilight`. Default `목록` (Q-IM-02 [PROPOSED]: every list board shows the list first).
 */
object ViewChoice {
    /** Screen id of the page list. */
    const val PAGES = "pages"

    fun showCards(viewByScreen: Map<String, ViewMode>, screen: String): Boolean = viewByScreen[screen] == ViewMode.CARD

    fun with(viewByScreen: Map<String, ViewMode>, screen: String, cards: Boolean): Map<String, ViewMode> =
        viewByScreen + (screen to if (cards) ViewMode.CARD else ViewMode.LIST)
}

/** The initial-letter placeholder (CNV-24): the first grapheme of the owner's name, as the DS avatar reads it. */
object CardInitial {
    fun of(name: String): String = avatarGlyph(name)
}
