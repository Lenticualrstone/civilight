package io.github.lenticualrstone.civilight.feature.library.shelf

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
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

/** One card of the card view's project row (LIB-04). */
sealed interface ShelfCardItem {
    /** The row's lazy key. */
    val key: Any

    data class Project(val card: ProjectCardUi) : ShelfCardItem {
        override val key: Any get() = card.row.key
    }

    data class Lock(val group: HiddenGroupRow) : ShelfCardItem {
        override val key: Any get() = "lockCard"
    }

    data object NewProject : ShelfCardItem {
        override val key: Any get() = "newCard"
    }
}

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

    fun recentLimit(wide: Boolean): Int = if (wide) RecentsModel.WIDE_COUNT else RecentsModel.COVER_COUNT

    /**
     * The cards of the project row in order: the projects, the lock card while the hidden group is locked,
     * and the outlined `새 프로젝트` card on the wide row. The pager dots count exactly these.
     */
    fun cardItems(ui: ShelfUi, wide: Boolean): List<ShelfCardItem> =
        ui.cards.map { ShelfCardItem.Project(it) } +
            listOfNotNull(ui.lockedGroup?.let { ShelfCardItem.Lock(it) }) +
            listOfNotNull(ShelfCardItem.NewProject.takeIf { wide })

    fun cover(doc: ProjectDoc?, name: String): CoverUi {
        val initial = name.trim().take(1)
        val use = doc?.index?.imageUse(ImageOwnerKind.PROJECT, doc.key, ImageSlot.PROJECT_COVER)
            ?: return CoverUi(null, null, null, initial, holo = false)
        return CoverUi(use.blobHash, use.cropFor(Ratio.R3X4), use.presetId, initial, use.holo)
    }

    /** The 48 dp list thumbnail (D6): the project cover's 1:1 crop ("목록 썸네일 · 목록 1:1", 55 GAL-14), a preset, or the initial. */
    fun thumb(doc: ProjectDoc?, name: String): CoverUi {
        val initial = name.trim().take(1)
        val use = doc?.index?.imageUse(ImageOwnerKind.PROJECT, doc.key, ImageSlot.PROJECT_COVER)
            ?: return CoverUi(null, null, null, initial, holo = false)
        return CoverUi(use.blobHash, use.cropFor(Ratio.R1X1), use.presetId, initial, use.holo)
    }

    fun chip(writing: Boolean, holo: Boolean): String? =
        listOfNotNull(ShelfCopy.WRITING.takeIf { writing }, ShelfCopy.HOLO.takeIf { holo }).joinToString(" · ").ifEmpty { null }

    fun card(row: ProjectRowUi, doc: ProjectDoc?, hiddenOpen: Boolean): ProjectCardUi {
        val cover = cover(doc, row.title)
        return ProjectCardUi(row, chip(row.writing, cover.holo), cover, hiddenOpen)
    }
}
