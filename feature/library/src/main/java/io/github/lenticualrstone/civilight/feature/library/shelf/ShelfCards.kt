package io.github.lenticualrstone.civilight.feature.library.shelf

import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * A project card's 3:4 cover (LIB-04): the picked image ([hash] + its 3:4 [crop]), a preset
 * ([presetId]), or neither: the initial-letter card ([initial]). [holo] adds the foil and `홀로`.
 */
data class CoverUi(val hash: String?, val crop: Crop?, val presetId: String?, val initial: String, val holo: Boolean)

/** One `ProjectCard` of the card view (LIB-04): the row's data, the status chip and the cover. */
data class ProjectCardUi(
    val row: ProjectRowUi,
    /** `쓰는 중`, `홀로` or `쓰는 중 · 홀로`; null without either. */
    val chip: String?,
    val cover: CoverUi,
    /** A hidden project shown because the group is open (`ED`: after the visible cards, `숨김 풀기` in its ⋯). */
    val hiddenOpen: Boolean,
)

/** The card view's pure parts (LIB-04 `LibCards` / `LibCardsWide`). */
object ShelfCards {
    /** The 서재's own key in `AppSettings.viewByScreen` (LIB-04: remembered per screen). */
    const val SCREEN = "library"

    /** Cover 222 × 296 with 14 between (board `LibCards`), inner 236 × 336 with 16 (`LibCardsWide`). */
    const val COVER_CARD_W = 222
    const val COVER_CARD_H = 296
    const val COVER_GAP = 14
    const val WIDE_CARD_W = 236
    const val WIDE_CARD_H = 336
    const val WIDE_GAP = 16

    /** 최근 연 파일 cards: 150 × 100 (gap 14) and 176 × 110 (gap 16). */
    const val COVER_RECENT_W = 150
    const val COVER_RECENT_H = 100
    const val WIDE_RECENT_W = 176
    const val WIDE_RECENT_H = 110

    /**
     * `ED`: the wide card row (`LibCardsWide`: bigger cards, the `새 프로젝트` card, 4 recents) is used
     * when the 서재 itself is at least this wide: the inner screen in `가운데 한 칸` (560) or a single pane.
     * In `목록 + 상세` the 서재 is the 390 list pane, so it keeps the cover row.
     */
    const val WIDE_MIN_DP = 560

    fun showCards(settings: AppSettings): Boolean = settings.viewByScreen[SCREEN] == ViewMode.CARD

    fun withView(settings: AppSettings, cards: Boolean): AppSettings =
        settings.copy(viewByScreen = settings.viewByScreen + (SCREEN to if (cards) ViewMode.CARD else ViewMode.LIST))

    fun isWide(widthDp: Int): Boolean = widthDp >= WIDE_MIN_DP

    /** `MCarousel` image parallax: 8 % of the card width (WLD-68 card carousel). */
    const val PARALLAX = 0.08f

    /**
     * How far a card's picture slides against its card (`MCarousel`): the card's distance from its
     * resting place [startPx] in strides (card + gap), times [PARALLAX] of the card width, opposite to
     * the motion, capped at one stride either way. 0 for the settled card.
     */
    fun parallaxShift(offsetPx: Int, startPx: Int, stridePx: Int, cardWidthPx: Int): Float {
        if (stridePx <= 0) return 0f
        val strides = ((offsetPx - startPx).toFloat() / stridePx).coerceIn(-1f, 1f)
        return -strides * PARALLAX * cardWidthPx
    }

    /** The scale that keeps a picture shifted by [shiftPx] covering its card: 1 at rest, so the crop shows whole. */
    fun parallaxScale(shiftPx: Float, cardWidthPx: Int): Float =
        if (cardWidthPx <= 0) 1f else 1f + 2f * kotlin.math.abs(shiftPx) / cardWidthPx

    fun recentLimit(wide: Boolean): Int = if (wide) RecentsModel.WIDE_COUNT else RecentsModel.COVER_COUNT

    /** Dots under the section label: one per card of the row (projects, the lock card, `새 프로젝트`). */
    fun pageCount(ui: ShelfUi, wide: Boolean): Int =
        ui.cards.size + (if (ui.lockedGroup != null) 1 else 0) + (if (wide) 1 else 0)

    fun cover(doc: ProjectDoc?, name: String): CoverUi {
        val initial = name.trim().take(1)
        val use = doc?.index?.imageUse(ImageOwnerKind.PROJECT, doc.key, ImageSlot.PROJECT_COVER)
            ?: return CoverUi(null, null, null, initial, holo = false)
        return CoverUi(use.blobHash, use.cropFor(Ratio.R3X4), use.presetId, initial, use.holo)
    }

    fun chip(writing: Boolean, holo: Boolean): String? =
        listOfNotNull(ShelfCopy.WRITING.takeIf { writing }, ShelfCopy.HOLO.takeIf { holo }).joinToString(" · ").ifEmpty { null }

    fun card(row: ProjectRowUi, doc: ProjectDoc?, hiddenOpen: Boolean): ProjectCardUi {
        val cover = cover(doc, row.title)
        return ProjectCardUi(row, chip(row.writing, cover.holo), cover, hiddenOpen)
    }
}
