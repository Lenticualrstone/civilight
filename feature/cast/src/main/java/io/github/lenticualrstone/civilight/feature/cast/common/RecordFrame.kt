package io.github.lenticualrstone.civilight.feature.cast.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.GlassPill
import io.github.lenticualrstone.civilight.designsystem.component.GlassRoundButton
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.LensChipState
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.component.TopGlassBar
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.ui.lens.LensChipLabel
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
 * the `Relations` board draws it. The bottom bar holds the five tabs above the command bar; toasts
 * and sheets work as in `CivilightScreen`.
 *
 * [content] gets the bottom bar's height as padding; its top is [recordTopInset] (fixed, so the
 * content does not jump when the top bar changes).
 */
@Composable
fun RecordFrame(
    back: IconAction?,
    lens: LensChipLabel,
    onLens: () -> Unit,
    collapsed: Boolean,
    collapsedTitle: String,
    collapsedSmallLine: String,
    tabs: @Composable RowScope.() -> Unit,
    commandBar: @Composable () -> Unit,
    sheets: SheetHost,
    localSheets: LocalSheets,
    content: @Composable (PaddingValues) -> Unit,
) {
    SheetHostLayout(sheets, localSheets = localSheets) {
        val toasts = LocalToasts.current
        val toast by toasts.current.collectAsState()
        val wide = LocalWindowLayout.current.isInner
        GlassScaffold(
            topBar = {
                if (collapsed) {
                    TopGlassBar(collapsedTitle, smallLine = collapsedSmallLine, back = back, wide = wide) {
                        LensChip(lens.text, lens.state, onLens)
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
                        val colors = CivilightTheme.colors
                        GlassPill(lens.text, onLens, textColor = if (lens.state == LensChipState.PAST) colors.acc else colors.ink)
                    }
                }
            },
            bottomBar = { BottomGlassBar(extraRow = tabs) { commandBar() } },
            overlay = { padding ->
                Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding() + 6.dp), contentAlignment = Alignment.BottomCenter) {
                    ToastHost(message = toast?.text, actionLabel = toast?.actionLabel, onAction = toast?.onAction?.let { { toasts.act() } })
                }
            },
            content = { padding -> content(PaddingValues(bottom = padding.calculateBottomPadding())) },
        )
    }
}
