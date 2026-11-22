package io.github.lenticualrstone.civilight.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.component.TopGlassBar
import io.github.lenticualrstone.civilight.ui.lens.LensChipLabel
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.MainScope

/** The toast host screens draw; provided by the shell. */
val LocalToasts = staticCompositionLocalOf { ToastController(MainScope()) }

/**
 * The common screen frame: glass top bar (small line, title, back, lens chip, actions), optional
 * glass bottom bar with the command bar, the `되돌리기` toast above it, an optional [banner] under the
 * top bar (read-only example, lens edit banner), and the screen's sheets.
 *
 * [content] scrolls under both bars and receives their heights as padding. The bottom padding is at
 * least the keyboard height, so a field near the end of the content can be scrolled above the keyboard;
 * the command bar itself rides on the keyboard (DS `BottomGlassBar`).
 *
 * [topBar] / [bottomBar] replace the DS bars when a screen needs its own (the script's 76 dp editing
 * header and keyboard toolbar); the toast host, banner and sheets stay. With [bottomBar] set,
 * [commandBar] and [bottomExtraRow] are ignored.
 */
@Composable
fun CivilightScreen(
    title: String,
    modifier: Modifier = Modifier,
    smallLine: String? = null,
    back: IconAction? = null,
    lens: LensChipLabel? = null,
    onLens: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    commandBar: (@Composable () -> Unit)? = null,
    bottomExtraRow: (@Composable RowScope.() -> Unit)? = null,
    banner: (@Composable () -> Unit)? = null,
    sheets: SheetHost? = null,
    localSheets: LocalSheets = { null },
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val frame: @Composable () -> Unit = {
        ScreenFrame(title, modifier, smallLine, back, lens, onLens, actions, commandBar, bottomExtraRow, banner, topBar, bottomBar, content)
    }
    if (sheets != null) SheetHostLayout(sheets, localSheets = localSheets) { frame() } else frame()
}

@Composable
private fun ScreenFrame(
    title: String,
    modifier: Modifier,
    smallLine: String?,
    back: IconAction?,
    lens: LensChipLabel?,
    onLens: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    commandBar: (@Composable () -> Unit)?,
    bottomExtraRow: (@Composable RowScope.() -> Unit)?,
    banner: (@Composable () -> Unit)?,
    topBar: (@Composable () -> Unit)?,
    bottomBar: (@Composable () -> Unit)?,
    content: @Composable (PaddingValues) -> Unit,
) {
    val wide = LocalWindowLayout.current.isInner
    val keyboard = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    GlassScaffold(
        modifier = modifier,
        topBar = {
            if (topBar != null) {
                topBar()
            } else {
                TopGlassBar(title = title, smallLine = smallLine, back = back, wide = wide) {
                    if (lens != null) LensChip(lens.text, lens.state, onLens)
                    actions()
                }
            }
        },
        bottomBar = {
            when {
                bottomBar != null -> bottomBar()
                commandBar != null -> BottomGlassBar(extraRow = bottomExtraRow) { commandBar() }
            }
        },
        overlay = { padding ->
            if (banner != null) {
                Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentAlignment = Alignment.TopCenter) { banner() }
            }
            ToastOverlay(bottom = maxOf(padding.calculateBottomPadding(), keyboard))
        },
        content = { padding ->
            content(PaddingValues(top = padding.calculateTopPadding(), bottom = maxOf(padding.calculateBottomPadding(), keyboard)))
        },
    )
}

/**
 * The shell's `되돌리기` toast (LIB-40, DS-27), 6 dp above [bottom] (the bottom bar's height). For a
 * screen that draws its own frame (e.g. on `GlassScaffold` directly) inside a full-size box; screens on
 * [CivilightScreen] get it already.
 */
@Composable
fun ToastOverlay(bottom: Dp, modifier: Modifier = Modifier) {
    val toasts = LocalToasts.current
    val toast by toasts.current.collectAsState()
    Box(modifier.fillMaxSize().padding(bottom = bottom + TOAST_GAP), contentAlignment = Alignment.BottomCenter) {
        ToastHost(message = toast?.text, actionLabel = toast?.actionLabel, onAction = toast?.onAction?.let { { toasts.act() } })
    }
}

/** A toast sits 6 dp above the bottom bar (DS-27). */
private val TOAST_GAP = 6.dp
