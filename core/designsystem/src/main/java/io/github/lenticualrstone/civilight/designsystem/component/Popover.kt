package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.AnimatedVisibility
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.glass.SCRIM_ALPHA
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.OverlayGaps
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Colours the `▲` part of [text] in [warn]: from the `▲` to the next ` · ` separator or the end
 * (`▲` and its row text are always `warn`, DS-02).
 */
fun warningSpan(text: String, warn: androidx.compose.ui.graphics.Color): AnnotatedString {
    val i = text.indexOf(MarkerKind.WARNING.symbol)
    if (i < 0) return AnnotatedString(text)
    val sep = text.indexOf(" · ", i)
    val end = if (sep < 0) text.length else sep
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = warn), i, end)
    }
}

/**
 * Places a popup [gapPx] below its anchor, or above when there is no room; kept inside the window (DS-66):
 * [sidePx] from both sides, never above [topPx], and its bottom never below `H − bottomPx` ([bottomPx] =
 * `max(systemBottom + edge, bar height above the window bottom + 6)`). The extra limits default to [marginPx].
 */
class AnchorBelowPositionProvider(
    private val gapPx: Int,
    private val marginPx: Int,
    private val sidePx: Int = marginPx,
    private val topPx: Int = marginPx,
    private val bottomPx: Int = marginPx,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - sidePx).coerceAtLeast(sidePx)
        val x = anchorBounds.left.coerceIn(sidePx, maxX)
        val below = anchorBounds.bottom + gapPx
        val y = if (below + popupContentSize.height <= windowSize.height - bottomPx) below
        else (anchorBounds.top - gapPx - popupContentSize.height).coerceAtLeast(topPx)
        return IntOffset(x, y)
    }
}

/**
 * DS-26 popover anchored 6 dp below (or above) the layout it is placed in. Dismissed by tapping outside or
 * back. It renders in its own window, so it cannot sample the backdrop: the card uses the SCRIM fill
 * (`raise` 92 %) in every translucent glass mode and `raise` when transparency is reduced. On cover screens a
 * popover with more than one action row should be a bottom sheet instead (CNV-32).
 */
@Composable
fun Popover(onDismissRequest: () -> Unit, gap: Dp = 6.dp, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val motion = LocalMotion.current
    // DS-66 limits: sides 12 (cover) / 24 (inner), top = status bar + 8, bottom above `systemBottom + edge` and
    // 6 dp above the bottom glass bar.
    val inner = LocalConfiguration.current.screenWidthDp >= INNER_MIN_WIDTH_DP
    val systemBottom = rememberSystemBottom()
    val bar = LocalBottomChrome.current.tallest
    val top = statusTop() + OverlayGaps.top
    val bottom = maxOf(
        systemBottom + OverlayGaps.edge(inner),
        // The bar sits on `systemBottom` (it rides on the keyboard), so its top is `systemBottom + bar`.
        if (bar > 0.dp) systemBottom + bar + OverlayGaps.aboveBar else 0.dp,
    )
    val provider = remember(density, gap, inner, top, bottom) {
        with(density) {
            AnchorBelowPositionProvider(
                gap.roundToPx(),
                OverlayGaps.sideCompact.roundToPx(),
                sidePx = OverlayGaps.side(inner).roundToPx(),
                topPx = top.roundToPx(),
                bottomPx = bottom.roundToPx(),
            )
        }
    }
    // S8: the card rises 6 dp and fades in on `enter`; an outside tap or back fades it out on `exit` first and
    // only then calls [onDismissRequest]. Removing the popover from composition is still an instant close.
    val visible = remember { MutableTransitionState(false) }
    val closing = remember { mutableStateOf(false) }
    val dismiss by rememberUpdatedState(onDismissRequest)
    LaunchedEffect(Unit) { visible.targetState = true }
    LaunchedEffect(visible) {
        snapshotFlow { visible.isIdle && !visible.currentState && !visible.targetState }
            .collect { gone ->
                if (gone && closing.value) {
                    dismiss()
                    // Self-healing: if the caller keeps the popover, show it again instead of an invisible popup.
                    withFrameNanos { }
                    withFrameNanos { }
                    closing.value = false
                    visible.targetState = true
                }
            }
    }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = {
            if (!closing.value) {
                closing.value = true
                visible.targetState = false
            }
        },
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(visible, enter = motion.enter(CivilightMotion.enter, rise = 6.dp), exit = motion.exit(CivilightMotion.exit), label = "popover") {
            content()
        }
    }
}

/** Window width from which the popover keeps the inner-screen side gap. */
private const val INNER_MIN_WIDTH_DP = 600

/**
 * DS-26 popover card: radius 16, padding 12 · 12 · 12 · 14, 1 dp `edge` inset, shadow `0 10 28 shade`,
 * width 268 (max 320). Title row: [title] in `itemTitle` + `small` [tag] (`후보 · 끊김`); [line]
 * (markers drawn as vectors, `▲` in `warn`); [actions] row (buttons 8 dp apart, flex 1).
 */
@Composable
fun PopoverCard(
    title: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    line: String? = null,
    actions: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    val fill = if (CivilightTheme.glassMode == GlassMode.OPAQUE) colors.raise else colors.raise.copy(alpha = SCRIM_ALPHA)
    Column(
        modifier = modifier
            .widthIn(min = 268.dp, max = 320.dp)
            .tokenShadow(shape, CivilightTheme.elevation.popover(colors))
            .clip(shape)
            .background(fill)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightText(title, type.itemTitle, Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1)
            if (tag != null) CivilightText(tag, type.small, color = colors.ink3, maxLines = 1)
        }
        if (line != null) DecoratedText(warningSpan(line, colors.warn), type.small, color = colors.ink3)
        content?.invoke(this)
        if (actions != null) ButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp), content = actions)
    }
}
