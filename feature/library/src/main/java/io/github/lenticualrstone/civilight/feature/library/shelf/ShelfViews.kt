package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsCopy
import io.github.lenticualrstone.civilight.feature.library.settings.ViewStyles
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.app.ShownView

/**
 * What a 서재 3D view draws (LIB-93). It gets only the already-masked [ShelfUi]: visible rows, the open
 * group's rows while it is open, the lock item ([ShelfUi.lockedGroup]) and the masked cards. It never
 * reads a project document, so no view can draw a locked hidden project (LIB-89, V-04).
 */
data class ShelfViewInput(
    val ui: ShelfUi,
    val wide: Boolean,
    val onOpen: (Key) -> Unit,
    val onMenu: (Key) -> Unit,
    val onUnlock: () -> Unit,
    val onNewProject: () -> Unit,
)

/** One 서재 3D view (U-16 boards `u16_lib_a` … `c`). Motion only through `CivilightMotion`. */
typealias ShelfViewRenderer = @Composable (ShelfViewInput) -> Unit

/**
 * The extension point of the 서재 views: one lookup `style → renderer`. A style with no renderer draws the
 * list. Wave 3 (U-16) fills [renderers]; until then every style draws the list and the head toggle is not
 * drawn (a toggle between two identical lists would be a control that does nothing).
 */
object ShelfViewRenderers {
    val renderers: Map<LibraryStyle, ShelfViewRenderer> = emptyMap()

    fun of(style: LibraryStyle): ShelfViewRenderer? = renderers[style]

    val available: Set<LibraryStyle> get() = renderers.keys
}

/** The head toggle (DS-39 shape): `목록` and the chosen 3D view, named by its label (`3D 책장`). */
data class ShelfToggleUi(val listLabel: String, val styleLabel: String, val styleOn: Boolean)

/**
 * The 서재 view as chosen (LIB-93): [chosen] is `서재 보기`, [shown] is `ShownView.library` (the 0.1.0
 * `viewByScreen["library"]` is not read, V-08), [rendered] is the 3D style actually drawn (null = the
 * list), and [toggle] is drawn only when the chosen style is 3D **and** has a renderer.
 */
data class ShelfViewUi(
    val chosen: LibraryStyle,
    val shown: LibraryStyle,
    val rendered: LibraryStyle?,
    val toggle: ShelfToggleUi?,
)

object ShelfViews {
    fun of(settings: AppSettings, available: Set<LibraryStyle> = ShelfViewRenderers.available): ShelfViewUi {
        val chosen = settings.libraryStyle
        val shown = ShownView.library(settings)
        val toggle = if (ShownView.showsToggle(chosen) && chosen in available) {
            ShelfToggleUi(SettingsCopy.VIEW_LIST, ViewStyles.label(chosen), styleOn = !shown.isList)
        } else {
            null
        }
        val rendered = shown.takeIf { !it.isList && it in available }
        return ShelfViewUi(chosen, shown, rendered, toggle)
    }

    /** The 3D view is drawn only with projects to show: 0 projects shows the LIB-70 empty card (V-05). */
    fun drawsView(view: ShelfViewUi, ui: ShelfUi): Boolean = view.rendered != null && ui.empty == null
}
