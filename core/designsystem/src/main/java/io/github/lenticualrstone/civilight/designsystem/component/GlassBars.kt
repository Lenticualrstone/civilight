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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
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
 * In a narrow pane the actions move to a second row under the title ([TopBarFit]); the bar grows to fit.
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
    val minBarHeight = (barHeight - 16.dp).coerceAtLeast(64.dp) + inset
    Box(modifier.fillMaxWidth().heightIn(min = minBarHeight)) {
        GlassBackground(GlassSurface.TOP_BAR, Modifier.matchParentSize())
        OnGlass {
            TopBarRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = inset + top, start = startPadding, end = 12.dp),
                back = {
                    FoldReveal(back != null, horizontal = true, label = "barBack") {
                        if (lastBack != null) CivilightIconButton(IconId.Back, lastBack.label, lastBack.onClick, tint = colors.ink)
                    }
                },
                title = {
                    Column(Modifier.padding(top = if (back != null) 2.dp else 0.dp)) {
                        if (smallLine != null) CivilightText(smallLine, type.numberHeader, color = colors.ink3, maxLines = 1)
                        CivilightText(title, if (wide) type.headerTitleInner else type.headerTitle, color = colors.ink, maxLines = 1)
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
                },
            )
        }
    }
}

/**
 * Where the top bar's actions go (펼친 화면 비율, CNV-47): beside the title while the title keeps
 * `min(its own width, MIN_TITLE_DP)`; in a narrower bar (a 280 dp list pane at 1:2, where the actions left
 * the title 0 to 24 dp) they move to their own row under the title, end-aligned and [ROW_GAP_DP] below it.
 * A short title (`인물`) that already fits beside the actions at cover width keeps the one-row bar.
 */
object TopBarFit {
    /** The narrowest title the actions may leave beside them (a longer title may be ellipsised to it). */
    const val MIN_TITLE_DP: Float = 64f

    /** Gap between the title row and the wrapped actions row. */
    const val ROW_GAP_DP: Float = 8f

    /**
     * True when [actions] (wide) next to [leading] (the back button) would leave the title less than
     * `min(titleNatural, minTitle)` of [width].
     */
    fun wraps(width: Int, leading: Int, actions: Int, minTitle: Int, titleNatural: Int = Int.MAX_VALUE): Boolean =
        actions > 0 && width - leading - actions < minOf(minTitle, titleNatural)

    /** The title's width: everything after [leading], less [actions] unless they [wrap]. */
    fun titleWidth(width: Int, leading: Int, actions: Int, wrap: Boolean): Int =
        (width - leading - if (wrap) 0 else actions).coerceAtLeast(0)

    /**
     * Start of the actions: end-aligned; when they are wider than the bar (wrapped row) they start at 0 and
     * run into the bar's end padding instead of squeezing the last action.
     */
    fun actionsX(width: Int, actions: Int): Int = (width - actions).coerceAtLeast(0)
}

/** back · title (fills) · actions, with the actions on a second row when [TopBarFit.wraps]. */
@Composable
private fun TopBarRow(
    modifier: Modifier,
    back: @Composable () -> Unit,
    title: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Layout(contents = listOf(back, title, actions), modifier = modifier) { (backM, titleM, actionsM), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val backP = backM.map { it.measure(loose) }
        // Actions keep their natural width (never squeezed); the decision below gives them room.
        val actionsP = actionsM.map { it.measure(loose.copy(maxWidth = Constraints.Infinity)) }
        val leading = backP.maxOfOrNull { it.width } ?: 0
        val actionsW = actionsP.maxOfOrNull { it.width } ?: 0
        val minTitle = TopBarFit.MIN_TITLE_DP.dp.roundToPx()
        val titleNatural = titleM.maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else leading + actionsW + minTitle
        val wrap = TopBarFit.wraps(width, leading, actionsW, minTitle, titleNatural)
        val titleW = TopBarFit.titleWidth(width, leading, actionsW, wrap)
        val titleP = titleM.map { it.measure(loose.copy(minWidth = titleW, maxWidth = titleW)) }
        val topH = maxOf(backP.maxOfOrNull { it.height } ?: 0, titleP.maxOfOrNull { it.height } ?: 0)
        val actionsH = actionsP.maxOfOrNull { it.height } ?: 0
        val actionsY = if (wrap) topH + TopBarFit.ROW_GAP_DP.dp.roundToPx() else 0
        val height = if (wrap) actionsY + actionsH else maxOf(topH, actionsH)
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            backP.forEach { it.placeRelative(0, 0) }
            titleP.forEach { it.placeRelative(leading, 0) }
            actionsP.forEach { it.placeRelative(TopBarFit.actionsX(width, it.width), actionsY) }
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
 *
 * [inner] (DS-65, inner screen): the bar spans the frame; the **command row** ([leading] + [commandBar]) is
 * centred, 24 dp from the sides, at most 720 dp wide, 20 dp above the navigation inset. [leading] (the scope
 * switch of `목록 + 상세`) sits 8 dp before the command field. [paneRows] is drawn full width above the command
 * row with no side padding, for rows confined to their pane's column (the frame places them itself).
 */
@Composable
fun BottomGlassBar(
    modifier: Modifier = Modifier,
    barHeight: Dp? = null,
    extraRow: (@Composable RowScope.() -> Unit)? = null,
    aboveKeyboard: Boolean = true,
    inner: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    paneRows: (@Composable () -> Unit)? = null,
    commandBar: @Composable () -> Unit,
) {
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val height = barHeight ?: if (extraRow != null || paneRows != null) LayoutMetrics.bottomBarWithRow else LayoutMetrics.bottomBar
    val side = if (inner) LayoutMetrics.commandBarSideInner else LayoutMetrics.commandBarSide
    val bottom = if (inner) LayoutMetrics.commandBarBottomInner else LayoutMetrics.commandBarBottom
    val minBottomHeight = (height - 20.dp).coerceAtLeast(72.dp) + inset
    Box(
        modifier
            .then(if (aboveKeyboard) Modifier.windowInsetsPadding(KeyboardLift) else Modifier)
            .fillMaxWidth()
            .heightIn(min = minBottomHeight),
        contentAlignment = Alignment.BottomCenter,
    ) {
        GlassBackground(GlassSurface.BOTTOM_BAR, Modifier.matchParentSize())
        OnGlass {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottom + inset, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (extraRow != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = side),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        content = extraRow,
                    )
                }
                if (paneRows != null) Box(Modifier.fillMaxWidth()) { paneRows() }
                val row = Modifier
                    .padding(horizontal = side)
                    .then(if (inner) Modifier.widthIn(max = LayoutMetrics.commandRowMaxInner) else Modifier)
                    .fillMaxWidth()
                if (leading == null) {
                    // A column, as before, so a caller that draws more than one child keeps them stacked 8 dp apart.
                    Column(row, verticalArrangement = Arrangement.spacedBy(8.dp)) { commandBar() }
                } else {
                    Row(row, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        leading()
                        Box(Modifier.weight(1f)) { commandBar() }
                    }
                }
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
    val glassMode = io.github.lenticualrstone.civilight.designsystem.theme.LocalGlassMode.current
    val recordBackdrop = glassMode == io.github.lenticualrstone.civilight.designsystem.glass.GlassMode.BLUR ||
        glassMode == io.github.lenticualrstone.civilight.designsystem.glass.GlassMode.BLUR_COSTLY
    // U-02: fields in the content keep their caret line clear of the bars (and the keyboard) while typing.
    val caretFrame = remember { CaretFrame() }
    SubcomposeLayout(modifier.fillMaxSize().onPlaced { caretFrame.coords = it }) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val top = subcompose(Slot.Top) { CompositionLocalProvider(LocalGlassSource provides source) { topBar() } }.map { it.measure(loose) }
        val bottom = subcompose(Slot.Bottom) { CompositionLocalProvider(LocalGlassSource provides source) { bottomBar() } }.map { it.measure(loose) }
        val topH = top.maxOfOrNull { it.height } ?: 0
        val bottomH = bottom.maxOfOrNull { it.height } ?: 0
        val padding = PaddingValues(top = topH.toDp(), bottom = bottomH.toDp())
        caretFrame.topBar = topH
        caretFrame.bottomBar = bottomH
        val body = subcompose(Slot.Content) {
            CompositionLocalProvider(LocalCaretFrame provides caretFrame) {
                Box(Modifier.glassSource(source, enabled = recordBackdrop).background(bg)) { content(padding) }
            }
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
