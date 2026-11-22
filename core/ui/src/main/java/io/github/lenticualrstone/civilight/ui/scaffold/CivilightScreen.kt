package io.github.lenticualrstone.civilight.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * [content] scrolls under both bars and receives their heights as padding.
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
    content: @Composable (PaddingValues) -> Unit,
) {
    val frame: @Composable () -> Unit = {
        ScreenFrame(title, modifier, smallLine, back, lens, onLens, actions, commandBar, bottomExtraRow, banner, content)
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
    content: @Composable (PaddingValues) -> Unit,
) {
    val wide = LocalWindowLayout.current.isInner
    val toasts = LocalToasts.current
    val toast by toasts.current.collectAsState()
    GlassScaffold(
        modifier = modifier,
        topBar = {
            TopGlassBar(title = title, smallLine = smallLine, back = back, wide = wide) {
                if (lens != null) LensChip(lens.text, lens.state, onLens)
                actions()
            }
        },
        bottomBar = {
            if (commandBar != null) BottomGlassBar(extraRow = bottomExtraRow) { commandBar() }
        },
        overlay = { padding ->
            if (banner != null) {
                Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentAlignment = Alignment.TopCenter) { banner() }
            }
            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding() + TOAST_GAP), contentAlignment = Alignment.BottomCenter) {
                ToastHost(message = toast?.text, actionLabel = toast?.actionLabel, onAction = toast?.onAction?.let { { toasts.act() } })
            }
        },
        content = content,
    )
}

/** A toast sits 6 dp above the bottom bar (DS-27). */
private val TOAST_GAP = 6.dp
