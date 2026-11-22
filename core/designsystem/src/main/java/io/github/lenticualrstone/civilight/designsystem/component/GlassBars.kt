package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateDpAsState
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.LocalGlassSource
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.glass.glassSource
import io.github.lenticualrstone.civilight.designsystem.glass.rememberGlassSource
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics

/**
 * DS-21 top glass bar: full width from the window top (behind the status bar) to [barHeight] below the
 * status-bar inset; glass with the 72 % edge mask. Contents 16 dp (cover) / 18 dp ([wide]) below the inset:
 * optional back button (`Back`, 44, shifted −12 dp onto the 20 dp gutter), a column with the small line
 * (`number` 11 / 15 `ink3`) and the title (`headerTitle`, 21 / 28 when [wide]), then [actions]
 * (lens chip, `Layers`, ViewToggle, `⋯`). [back] carries the back button's accessibility label (owner copy).
 */
@Composable
fun TopGlassBar(
    title: String,
    modifier: Modifier = Modifier,
    smallLine: String? = null,
    back: IconAction? = null,
    wide: Boolean = false,
    barHeight: Dp = LayoutMetrics.topBarCover,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val start = if (wide) LayoutMetrics.topBarStartInner else LayoutMetrics.topBarStartCover
    val top = if (wide) LayoutMetrics.topBarContentTopInner else LayoutMetrics.topBarContentTopCover
    val motion = LocalMotion.current
    // S18a: the back button folds in from the start edge and the row's start padding follows on `standard`.
    val startPadding by animateDpAsState(if (back != null) start - 12.dp else start, motion.move(CivilightMotion.standard), label = "barStart")
    val lastBack = rememberLastNonNull(back)
    Box(modifier.fillMaxWidth().heightIn(min = barHeight + inset)) {
        GlassBackground(GlassSurface.TOP_BAR, Modifier.matchParentSize())
        OnGlass {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = inset + top, start = startPadding, end = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                FoldReveal(back != null, horizontal = true, label = "barBack") {
                    if (lastBack != null) CivilightIconButton(IconId.Back, lastBack.label, lastBack.onClick, tint = colors.ink)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(top = if (back != null) 2.dp else 0.dp),
                ) {
                    if (smallLine != null) CivilightText(smallLine, type.numberHeader, color = colors.ink3, maxLines = 1)
                    CivilightText(title, if (wide) type.headerTitleInner else type.headerTitle, color = colors.ink, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
            }
        }
    }
}

/**
 * DS-21 bottom glass bar: full width at the window bottom (behind the navigation bar), at least
 * [barHeight] (130; 150 with [extraRow]; 120 on onboarding) above the navigation inset, glass with the 70 %
 * edge mask. Contents: a column 8 dp apart, 12 dp sides, 18 dp above the navigation inset: the optional
 * row (speaker pill · `Layers` · spacer · `줄바꿈`), then [commandBar].
 *
 * The bar rides above the keyboard: it is lifted by the IME inset minus the navigation inset (its own
 * padding already covers the navigation bar), with the consumption-aware `windowInsetsPadding`, so a
 * caller that already lifts it the same way (the script) is not lifted twice. [aboveKeyboard] = false
 * keeps it at the window bottom (a screen that hides the bar while typing).
 */
@Composable
fun BottomGlassBar(
    modifier: Modifier = Modifier,
    barHeight: Dp? = null,
    extraRow: (@Composable RowScope.() -> Unit)? = null,
    aboveKeyboard: Boolean = true,
    commandBar: @Composable () -> Unit,
) {
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val height = barHeight ?: if (extraRow != null) LayoutMetrics.bottomBarWithRow else LayoutMetrics.bottomBar
    Box(
        modifier
            .then(if (aboveKeyboard) Modifier.windowInsetsPadding(KeyboardLift) else Modifier)
            .fillMaxWidth()
            .heightIn(min = height + inset),
        contentAlignment = Alignment.BottomCenter,
    ) {
        GlassBackground(GlassSurface.BOTTOM_BAR, Modifier.matchParentSize())
        OnGlass {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = LayoutMetrics.commandBarSide, end = LayoutMetrics.commandBarSide, bottom = LayoutMetrics.commandBarBottom + inset, top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (extraRow != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = extraRow)
                }
                commandBar()
            }
        }
    }
}

/**
 * Screen frame with glass bars: [content] is drawn on `bg` and recorded as the glass source; the bars are
 * drawn over it (siblings, never descendants) and blur it. [content] receives the bar heights as padding
 * so it can scroll under both bars. [overlay] (toasts, popover hosts) receives the same padding; a toast
 * sits 6 dp above the bottom bar.
 */
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    overlay: @Composable BoxScope.(PaddingValues) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val source = rememberGlassSource()
    val bg = CivilightTheme.colors.bg
    SubcomposeLayout(modifier.fillMaxSize()) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val top = subcompose(Slot.Top) { CompositionLocalProvider(LocalGlassSource provides source) { topBar() } }.map { it.measure(loose) }
        val bottom = subcompose(Slot.Bottom) { CompositionLocalProvider(LocalGlassSource provides source) { bottomBar() } }.map { it.measure(loose) }
        val topH = top.maxOfOrNull { it.height } ?: 0
        val bottomH = bottom.maxOfOrNull { it.height } ?: 0
        val padding = PaddingValues(top = topH.toDp(), bottom = bottomH.toDp())
        val body = subcompose(Slot.Content) {
            Box(Modifier.glassSource(source).background(bg)) { content(padding) }
        }.map {
            // Fill the frame; in an unbounded parent (e.g. a scrolling preview) take the content's own height.
            it.measure(
                if (constraints.hasBoundedHeight) Constraints.fixed(constraints.maxWidth, constraints.maxHeight)
                else Constraints(minWidth = constraints.maxWidth, maxWidth = constraints.maxWidth),
            )
        }
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else maxOf(body.maxOfOrNull { it.height } ?: 0, topH + bottomH)
        val over = subcompose(Slot.Overlay) {
            CompositionLocalProvider(LocalGlassSource provides source) {
                Box(Modifier.fillMaxSize()) { overlay(padding) }
            }
        }.map { it.measure(Constraints.fixed(constraints.maxWidth, height)) }
        layout(constraints.maxWidth, height) {
            body.forEach { it.place(0, 0) }
            top.forEach { it.place(0, 0) }
            bottom.forEach { it.place(0, height - it.height) }
            over.forEach { it.place(0, 0) }
        }
    }
}

private enum class Slot { Top, Bottom, Content, Overlay }

/**
 * The keyboard height above the navigation bar: what a bar or sheet that already pads for the
 * navigation bar must add to sit on the keyboard. Apply it with `Modifier.windowInsetsPadding`, which
 * consumes it, so nested users (a bar inside a lifted box) add nothing.
 */
val KeyboardLift: WindowInsets
    @Composable get() = WindowInsets.ime.exclude(WindowInsets.navigationBars)
