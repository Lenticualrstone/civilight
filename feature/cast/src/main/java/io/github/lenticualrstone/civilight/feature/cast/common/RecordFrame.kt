package io.github.lenticualrstone.civilight.feature.cast.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.GlassRoundButton
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.component.TopGlassBar
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.SCRIM_ALPHA
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.ui.lens.LensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope
import io.github.lenticualrstone.civilight.ui.scaffold.LocalCommandBarHost
import io.github.lenticualrstone.civilight.ui.scaffold.LocalSheets
import io.github.lenticualrstone.civilight.ui.scaffold.LocalToasts
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/** Top of the record content: the floating glass buttons sit above it (status bar + 12 + 44 + 12). */
@Composable
fun recordTopInset(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 68.dp

/**
 * The character record's frame (`Profile`, `RTEditTwo`, `Relations`). Unlike `CivilightScreen` it
 * has no glass top bar while the record's head is on screen: a glass round back button and the lens
 * as a glass pill float over the page (DS-28 `FadingHeaderArt`: "no glass bar on top"). Once the
 * name has scrolled away ([collapsed]) the usual glass top bar shows the record number and name, as
 * the `Relations` board draws it. [add] is the tab's `+` (`게임 텍스트`: a voice line, CST-16) and [menu] the record `⋯` (CST-40); both sit next to the lens in either state. The bottom bar holds the five tabs above the command bar; toasts
 * and sheets work as in `CivilightScreen`.
 *
 * [lensModifier] goes on the lens in either state (the cast tour marks it as its `시점 렌즈` hole, LIB-19).
 * [content] gets the bottom bar's height as padding; its top is [recordTopInset] (fixed, so the
 * content does not jump when the top bar changes).
 */
@Composable
fun RecordFrame(
    back: IconAction?,
    lens: LensChipLabel,
    onLens: () -> Unit,
    add: IconAction?,
    menu: IconAction?,
    collapsed: Boolean,
    collapsedTitle: String,
    collapsedSmallLine: String,
    tabs: @Composable RowScope.() -> Unit,
    commandBar: @Composable () -> Unit,
    sheets: SheetHost,
    localSheets: LocalSheets,
    lensModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    SheetHostLayout(sheets, localSheets = localSheets) {
        val toasts = LocalToasts.current
        val toast by toasts.current.collectAsState()
        val wide = LocalWindowLayout.current.isInner
        val paneHost = if (CommandScope.of(LocalPaneRole.current) != null) LocalCommandBarHost.current else null
        GlassScaffold(
            topBar = {
                // The floating buttons and the glass top bar swap in place (P3); both draw the lens as the shared `LensChip`.
                ContentSwap(collapsed, label = "recordHeader") { bar ->
                    if (bar) {
                        TopGlassBar(collapsedTitle, smallLine = collapsedSmallLine, back = back, wide = wide) {
                            LensChip(lens.text, lens.state, onLens, lensModifier, small = true)
                            if (add != null) CivilightIconButton(IconId.Plus, add.label, add.onClick, tint = CivilightTheme.colors.ink)
                            if (menu != null) CivilightIconButton(IconId.More, menu.label, menu.onClick, tint = CivilightTheme.colors.ink)
                        }
                    } else {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(WindowInsets.statusBars.asPaddingValues())
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (back != null) GlassRoundButton(IconId.Back, back.label, back.onClick)
                            Spacer(Modifier.weight(1f))
                            GlassLens(lens, onLens, lensModifier)
                            if (add != null) GlassRoundButton(IconId.Plus, add.label, add.onClick, Modifier.padding(start = 8.dp))
                            if (menu != null) GlassRoundButton(IconId.More, menu.label, menu.onClick, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            // QA 13: the tabs sit in the see-through top of the bar's glass (the bar is opaque only over its lower
            // 70 %), so page text scrolling under them showed through as a faint ghost behind a label. The tab row
            // gets its own scrim until the bar's opaque part covers an extra row (see Requests).
            bottomBar = {
                if (paneHost != null) {
                    // Two-pane inner screen: the frame draws the shared command bar (DS-65); draw only the tabs pill above it.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = paneHost.panePadding + 8.dp, start = 16.dp, end = 16.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(CivilightTheme.colors.raise.copy(alpha = SCRIM_ALPHA))
                                .padding(horizontal = TAB_ROW_INSET),
                            verticalAlignment = Alignment.CenterVertically,
                            content = tabs,
                        )
                    }
                } else {
                    BottomGlassBar(
                        extraRow = {
                            Row(
                                // The pill is fully round (radius 22 on a 44 row): without an inset `개요` and its underline
                                // started under the left corner and were cut off. The inset clears the corner at the underline.
                                Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(CivilightTheme.colors.raise.copy(alpha = SCRIM_ALPHA))
                                    .padding(horizontal = TAB_ROW_INSET),
                                verticalAlignment = Alignment.CenterVertically,
                                content = tabs,
                            )
                        },
                    ) { commandBar() }
                }
            },
            overlay = { padding ->
                Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding() + 6.dp), contentAlignment = Alignment.BottomCenter) {
                    ToastHost(message = toast?.text, actionLabel = toast?.actionLabel, onAction = toast?.onAction?.let { { toasts.act() } })
                }
            },
            content = { padding -> content(PaddingValues(bottom = padding.calculateBottomPadding())) },
        )
    }
}

/**
 * The lens in the expanded header: the shared [LensChip] (value swap, sweep and outline, CNV-42) on a glass
 * pill, so the glass top bar and the floating header animate the lens the same way.
 */
@Composable
private fun GlassLens(lens: LensChipLabel, onLens: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape)
        LensChip(lens.text, lens.state, onLens, Modifier.padding(horizontal = 6.dp), small = true)
    }
}

/** Side inset of the tab row inside its round scrim: clears the 22 dp corner at the 2 dp underline (≈ 13 dp in). */
internal val TAB_ROW_INSET: Dp = 16.dp
