package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.theme.Unhide
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ShelfScene
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ShelfScene3D
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
    /** DS-74: tilt follows the device (`Tilt.follows`: the LIB-94 switch on and motion not reduced). */
    val tilt: Boolean = false,
    /** CNV-43: the open hidden group's items come out of the blur with the list's unhide. */
    val unhide: Unhide? = null,
    /**
     * (0.2.2) The scene fills the whole content box, under the glass bars: the top bar's height (dp) and the
     * bottom bar's (the search field; in a pane the frame bar's). The stage sits between them and the glass dock.
     */
    val barTop: Float = 0f,
    val barBottom: Float = 0f,
    /** (0.2.2) A sheet covers the scene: its dock stops counting as bottom chrome (the toast keeps to the bars). */
    val covered: Boolean = false,
    /** (0.2.2) The dock's peek row `최근 연 파일 ⌃` opens the 최근 연 파일 sheet; null draws no peek row. */
    val onRecents: (() -> Unit)? = null,
    /** LIB-06: the project shown in the detail pane (inner display); the view brings it to the front. */
    val selectedKey: io.github.lenticualrstone.civilight.model.Key? = null,
    /**
     * (0.2.3) Drawn as a live preview in the view picker: inert (no gestures, no semantics, no tour targets, no
     * bottom-chrome report, no tilt or idle loop). Window protection still applies to what it draws.
     */
    val preview: Boolean = false,
    /** (0.2.3) LIB-19 `프로젝트 숨기기`: the project whose `⋯` the tour points at (flat views). */
    val tourMenuKey: io.github.lenticualrstone.civilight.model.Key? = null,
)

/** One 서재 3D view (U-16 boards `u16_lib_a` … `c`). Motion only through `CivilightMotion`. */
typealias ShelfViewRenderer = @Composable (ShelfViewInput) -> Unit

/**
 * The extension point of the 서재 views: one lookup `style → renderer`. A style with no renderer draws the
 * list. Wave 3 (U-16) registers the six views (`shelf/views`); a style left out draws the list and gets no
 * head toggle (a toggle between two identical lists would be a control that does nothing).
 */
object ShelfViewRenderers {
    private val threeD: Map<LibraryStyle, ShelfViewRenderer> = ShelfScene.styles.associateWith { style ->
        { input: ShelfViewInput -> ShelfScene3D(style, input) }
    }

    /** (0.2.3, r3 `libviews`) The seven flat views: one frame, one renderer each. */
    private val flat: Map<LibraryStyle, ShelfViewRenderer> = io.github.lenticualrstone.civilight.feature.library.shelf.flat.FlatViews.styles.associateWith { style ->
        { input: ShelfViewInput -> io.github.lenticualrstone.civilight.feature.library.shelf.flat.FlatScene(style, input) }
    }

    val renderers: Map<LibraryStyle, ShelfViewRenderer> = threeD + flat

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
    /** `AppSettings.viewTilt` (LIB-94); the screen adds the reduced-motion half of `Tilt.follows`. */
    val viewTilt: Boolean = true,
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
        return ShelfViewUi(chosen, shown, rendered, toggle, settings.viewTilt)
    }

    /** The 3D view is drawn only with projects to show: 0 projects shows the LIB-70 empty card (V-05). */
    fun drawsView(view: ShelfViewUi, ui: ShelfUi): Boolean = view.rendered != null && ui.empty == null
}
