package io.github.lenticualrstone.civilight.ui.scaffold

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.remember
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
import io.github.lenticualrstone.civilight.designsystem.component.ReportBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.rememberSystemBottom
import io.github.lenticualrstone.civilight.designsystem.component.systemBottomWithoutKeyboard
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.component.TopGlassBar
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.ui.lens.LensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.MainScope

/** The toast host screens draw; provided by the shell. */
val LocalToasts = staticCompositionLocalOf { ToastController(MainScope()) }

/**
 * The common screen frame: glass top bar (small line, title, back, lens chip, actions), optional
 * glass bottom bar with the command bar, an optional [banner] under the top bar (read-only example, lens
 * edit banner), and the screen's sheets. The toast is the window's (DS-67), not the screen's.
 *
 * In a pane of `목록 + 상세` ([LocalPaneRole] `LIST` / `DETAIL` under a [LocalCommandBarHost]) the screen draws
 * no bottom glass bar: it hands [commandBar] and [bottomExtraRow] to the frame, which draws one bar for both
 * panes (DS-65), and [content] gets the frame bar's height as bottom padding.
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
    // DS-65: in `목록 + 상세` the pane hands its command bar to the frame's host and draws none itself.
    val host = LocalCommandBarHost.current
    val scope = CommandScope.of(LocalPaneRole.current)
    val paneHost = if (scope != null) host else null
    if (paneHost != null && scope != null) {
        val token = remember { Any() }
        val locals = currentCompositionLocalContext
        val declared = commandBar.takeIf { bottomBar == null }
        val sheetShown = sheets?.top != null
        SideEffect {
            paneHost.declare(CommandBarHost.Registration(token, scope, title, declared, bottomExtraRow.takeIf { declared != null }, locals))
            paneHost.sheetOpen(token, sheetShown)
        }
        DisposableEffect(paneHost, token) { onDispose { paneHost.withdraw(token) } }
    }
    val frame: @Composable () -> Unit = {
        ScreenFrame(title, modifier, smallLine, back, lens, onLens, actions, commandBar, bottomExtraRow, banner, topBar, bottomBar, paneHost, content)
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
    paneHost: CommandBarHost?,
    content: @Composable (PaddingValues) -> Unit,
) {
    val wide = LocalWindowLayout.current.isInner
    val keyboard = rememberKeyboardBottom()
    val motion = LocalMotion.current
    val shownBanner = rememberLastNonNull(banner)
    GlassScaffold(
        modifier = modifier,
        topBar = {
            if (topBar != null) {
                topBar()
            } else {
                TopGlassBar(title = title, smallLine = smallLine, back = back, wide = wide) {
                    // S-1, CST-70, WLD-71: the lens chip left on the cast and world heads is the small one.
                    if (lens != null) LensChip(lens.text, lens.state, onLens, small = true)
                    actions()
                }
            }
        },
        bottomBar = {
            when {
                // A pane's own action bar (wizard buttons, record buttons) sits on the frame bar's top (DS-65 §6).
                // One call site in a pane whatever the padding: the bar keeps its state, focus and animations
                // when the frame bar is measured or comes and goes (a padding of 0 dp until then).
                bottomBar != null && paneHost != null -> {
                    val lift = if (paneHost.panePadding > 0.dp) {
                        (paneHost.panePadding - systemBottomWithoutKeyboard()).coerceAtLeast(0.dp)
                    } else {
                        0.dp
                    }
                    Box(Modifier.padding(bottom = lift)) { bottomBar() }
                }
                bottomBar != null -> bottomBar()
                // In a pane: no bar of its own, only the frame bar's height, so both panes get the same padding.
                // Until the frame bar is measured, its standard height (no reflow when it first appears).
                paneHost != null -> Spacer(
                    Modifier.fillMaxWidth().height(
                        if (paneHost.hasBar && paneHost.barHeight == 0.dp) LayoutMetrics.bottomBar + systemBottomWithoutKeyboard() else paneHost.panePadding,
                    ),
                )
                commandBar != null -> BottomGlassBar(extraRow = bottomExtraRow, inner = wide) { commandBar() }
            }
        },
        overlay = { padding ->
            // S8: the banner drops in 8 dp on `enter` and leaves on `exit`, drawn with its last content.
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentAlignment = Alignment.TopCenter) {
                AnimatedVisibility(
                    visible = banner != null,
                    enter = motion.enter(CivilightMotion.enter, rise = -BANNER_RISE),
                    exit = motion.exit(CivilightMotion.exit, rise = -BANNER_RISE),
                    label = "banner",
                ) {
                    shownBanner?.invoke()
                }
            }
            // DS-67: the toast belongs to the window (the shell's `WindowToastHost`); a screen only reports its
            // bottom bar, which sets the toast slot and the popover limit (DS-66).
            if (paneHost == null && (bottomBar != null || commandBar != null)) {
                ReportBottomChrome(padding.calculateBottomPadding() - rememberSystemBottom())
            }
        },
        content = { padding ->
            content(PaddingValues(top = padding.calculateTopPadding(), bottom = maxOf(padding.calculateBottomPadding(), keyboard)))
        },
    )
}

/**
 * A toast host 6 dp above [bottom], kept for screens that draw their own frame. Inside the app the shell's
 * `WindowToastHost` draws the one toast of the window (DS-67) and this draws nothing (`LocalWindowToasts`);
 * outside it (previews) it still shows the toast.
 */
@Composable
fun ToastOverlay(bottom: Dp, modifier: Modifier = Modifier) {
    val toasts = LocalToasts.current
    val toast by toasts.current.collectAsState()
    Box(modifier.fillMaxSize().padding(bottom = bottom + TOAST_GAP), contentAlignment = Alignment.BottomCenter) {
        // S10: the id lets a new toast replace one with the same text (LIB-40).
        ToastHost(message = toast?.text, actionLabel = toast?.actionLabel, onAction = toast?.onAction?.let { { toasts.act() } }, id = toast?.id)
    }
}

/**
 * The keyboard's height under the content (S18). From API 30 the system animates the inset itself; on
 * API 29 it arrives in one step, so it moves on `standard` there (a snap when motion is reduced).
 */
@Composable
fun rememberKeyboardBottom(): Dp {
    val raw = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    if (Build.VERSION.SDK_INT >= 30) return raw
    val animated by animateDpAsState(raw, LocalMotion.current.move(CivilightMotion.standard), label = "keyboard")
    return animated
}

private val BANNER_RISE = 8.dp

/** A toast sits 6 dp above the bottom bar (DS-27). */
private val TOAST_GAP = 6.dp
