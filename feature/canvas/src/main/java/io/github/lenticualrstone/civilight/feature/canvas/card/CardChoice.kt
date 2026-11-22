package io.github.lenticualrstone.civilight.feature.canvas.card

import io.github.lenticualrstone.civilight.designsystem.component.avatarGlyph
import io.github.lenticualrstone.civilight.model.app.ViewMode

/*
 * The model-bound rest of the card rules. Card kinds, metrics, snap, ticker and parallax moved to the shared
 * `designsystem.card` package (motion audit wave 1).
 */

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
