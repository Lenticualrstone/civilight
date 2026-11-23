package io.github.lenticualrstone.civilight.feature.library.settings

import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import kotlinx.serialization.Serializable
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.navigation.SheetKey

/** Which of the two view lists a picker shows (LIB-94). */
@Serializable
enum class ViewKind { LIBRARY, CARD }

/**
 * (0.2.0, LIB-94) The picker of `서재 보기` or `카드 보기`. The spec draws a screen (head `설정` / `서재 보기`);
 * `core:navigation` routes are sealed and frozen in this wave, so it opens as a local sheet with the same
 * radio list (Request: routes `SettingsLibraryView`, `SettingsCardView`).
 */
@Serializable
data class ViewStyleSheet(val kind: ViewKind) : SheetKey

/** One radio of the picker; [id] is the enum name of [LibraryStyle] or [CardStyle]. */
data class ViewStyleOption(val id: String, val title: String, val selected: Boolean)

/**
 * (0.2.0, LIB-94) Mirror of `feature:cast` `CastCardViews.renderers.keys`: `feature:library` does not depend
 * on `feature:cast`, so the card styles that can be drawn are listed here. Wave 3 fills both this set and
 * `CastCardViews.renderers` together (Request: one shared registry in a common module). While it is empty
 * `카드 보기` is not drawn, since picking a style would change nothing.
 */
object CardViewMirror {
    val available: Set<CardStyle> = emptySet()
}

/** Labels and picker rows of the 0.2.0 view settings (U-16, LIB-93, LIB-94, CST-67). */
object ViewStyles {
    fun label(style: LibraryStyle): String = when (style) {
        LibraryStyle.BOOKSHELF -> SettingsCopy.LIB_BOOKSHELF
        LibraryStyle.COVER_SPIN -> SettingsCopy.LIB_COVER_SPIN
        LibraryStyle.MANUSCRIPT_STACK -> SettingsCopy.LIB_MANUSCRIPT_STACK
        LibraryStyle.ARCHIVE_HALL -> SettingsCopy.LIB_ARCHIVE_HALL
        LibraryStyle.INDEX_DRAWER -> SettingsCopy.LIB_INDEX_DRAWER
        LibraryStyle.CONSTELLATION -> SettingsCopy.LIB_CONSTELLATION
        LibraryStyle.LIST -> SettingsCopy.VIEW_LIST
    }

    fun label(style: CardStyle): String = when (style) {
        CardStyle.FAN_DECK -> SettingsCopy.CARD_FAN_DECK
        CardStyle.CAROUSEL -> SettingsCopy.CARD_CAROUSEL
        CardStyle.STACK -> SettingsCopy.CARD_STACK
        CardStyle.TAROT -> SettingsCopy.CARD_TAROT
        CardStyle.TILTED_WALL -> SettingsCopy.CARD_TILTED_WALL
        CardStyle.ROLODEX -> SettingsCopy.CARD_ROLODEX
        CardStyle.LIST -> SettingsCopy.VIEW_LIST
    }

    fun title(kind: ViewKind): String = when (kind) {
        ViewKind.LIBRARY -> SettingsCopy.LIBRARY_VIEW
        ViewKind.CARD -> SettingsCopy.CARD_VIEW
    }

    /**
     * The values in settings order (00 §13.4) that can take effect: `목록` plus the styles with a renderer
     * ([library], [cards]). The stored one is checked; a stored style with no renderer draws the list, so
     * `목록` is checked then.
     */
    fun options(
        kind: ViewKind,
        settings: AppSettings,
        library: Set<LibraryStyle> = ShelfViewRenderers.available,
        cards: Set<CardStyle> = CardViewMirror.available,
    ): List<ViewStyleOption> = when (kind) {
        ViewKind.LIBRARY -> {
            val shown = settings.libraryStyle.takeIf { it == LibraryStyle.LIST || it in library } ?: LibraryStyle.LIST
            LibraryStyle.entries.filter { it == LibraryStyle.LIST || it in library }
                .map { ViewStyleOption(it.name, label(it), it == shown) }
        }
        ViewKind.CARD -> {
            val shown = settings.castCardStyle.takeIf { it == CardStyle.LIST || it in cards } ?: CardStyle.LIST
            CardStyle.entries.filter { it == CardStyle.LIST || it in cards }
                .map { ViewStyleOption(it.name, label(it), it == shown) }
        }
    }

    /**
     * `서재 보기` · `<label> ›` and `카드 보기` · `<label> ›`: no sub-line (LIB-94). Null (row not drawn) while
     * no 3D view of that kind has a renderer: the pick would change nothing (wave rule: no control that does
     * nothing). The value names what is drawn, so a stored style without a renderer reads `목록`.
     */
    fun libraryRow(settings: AppSettings, available: Set<LibraryStyle> = ShelfViewRenderers.available): SettingsLinkRow? {
        if (available.isEmpty()) return null
        val shown = settings.libraryStyle.takeIf { it == LibraryStyle.LIST || it in available } ?: LibraryStyle.LIST
        return SettingsLinkRow(SettingsCopy.LIBRARY_VIEW, null, label(shown))
    }

    fun cardRow(settings: AppSettings, available: Set<CardStyle> = CardViewMirror.available): SettingsLinkRow? {
        if (available.isEmpty()) return null
        val shown = settings.castCardStyle.takeIf { it == CardStyle.LIST || it in available } ?: CardStyle.LIST
        return SettingsLinkRow(SettingsCopy.CARD_VIEW, null, label(shown))
    }

    /**
     * `TODO_COPY_LIB_VIEW_TILT` (default on). The row stays enabled under reduced motion (`ED`); its effect
     * is nil then (`Tilt.follows`). Null (not drawn) while neither kind has a renderer: tilt only moves 3D views.
     */
    fun tiltRow(
        settings: AppSettings,
        library: Set<LibraryStyle> = ShelfViewRenderers.available,
        cards: Set<CardStyle> = CardViewMirror.available,
    ): SettingsSwitchRow? =
        if (library.isEmpty() && cards.isEmpty()) null else SettingsSwitchRow(SettingsCopy.VIEW_TILT, null, settings.viewTilt)

    fun libraryStyleOf(id: String): LibraryStyle? = LibraryStyle.entries.firstOrNull { it.name == id }

    fun cardStyleOf(id: String): CardStyle? = CardStyle.entries.firstOrNull { it.name == id }
}
