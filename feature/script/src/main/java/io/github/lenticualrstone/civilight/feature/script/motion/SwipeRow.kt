package io.github.lenticualrstone.civilight.feature.script.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.startHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.MarkerGlyph
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The swipe-left reveal of a block (SCR-30, CNV-40), the DS-44 look with the staggered buttons the
 * DS row cannot draw: the block follows the finger and settles at −192 dp or back over 260 ms
 * (released past half stays open); each of `시간 · 참고 · 속성` fades in from 16 dp to the right,
 * 60 ms after the one before ([ScriptMotion.buttonProgress]). Nothing runs on swipe; only the buttons
 * act. Off while typing ([enabled] false). Reduced motion: the buttons fade with the row, no slide.
 */
@Composable
fun StaggeredSwipeRow(
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onTime: () -> Unit,
    onReference: () -> Unit,
    onProperties: () -> Unit,
    timeHasChange: Boolean,
    labels: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val reduced = CivilightTheme.reducedMotion
    val colors = CivilightTheme.colors
    val width = CivilightTheme.sizes.swipeRow
    val widthPx = with(LocalDensity.current) { width.toPx() }
    val density = LocalDensity.current
    val offset = remember { Animatable(if (revealed) -widthPx else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(revealed, widthPx) {
        offset.animateTo(if (revealed) -widthPx else 0f, CivilightMotion.spec(CivilightMotion.swipe, reduced))
    }
    val progress = { (-offset.value / widthPx).coerceIn(0f, 1f) }
    Box(modifier) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            // Closed, the buttons are not read by TalkBack (the row's custom actions offer the same three).
            val hidden = if (revealed) Modifier else Modifier.clearAndSetSemantics { }
            Row(Modifier.width(width).fillMaxHeight().then(hidden).graphicsLayer { alpha = if (progress() > 0f) 1f else 0f }.background(colors.raise)) {
                val actions = listOf(onTime, onReference, onProperties)
                actions.forEachIndexed { i, action ->
                    SwipeButton(
                        label = labels[i],
                        onClick = action,
                        labelColor = if (i == 0 && timeHasChange) colors.acc else colors.ink,
                        modifier = Modifier.graphicsLayer {
                            val p = ScriptMotion.buttonProgress(progress(), i, reduced)
                            alpha = p
                            translationX = with(density) { ScriptMotion.buttonShiftDp(p, reduced).dp.toPx() }
                        },
                    ) {
                        when (i) {
                            0 -> if (timeHasChange) MarkerGlyph(MarkerKind.CHANGE, size = 11.dp)
                            1 -> CivilightIcon(IconId.Paper, null, size = 18.dp, tint = colors.ink2)
                            else -> CivilightIcon(IconId.Props, null, size = 18.dp, tint = colors.ink2)
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(colors.bg)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-widthPx, 0f)) }
                    },
                    onDragStopped = {
                        val open = offset.value < -widthPx / 2
                        offset.animateTo(if (open) -widthPx else 0f, CivilightMotion.spec(CivilightMotion.swipe, reduced))
                        if (open != revealed) onRevealedChange(open)
                    },
                ),
        ) { content() }
    }
}

@Composable
private fun RowScope.SwipeButton(
    label: String,
    onClick: () -> Unit,
    labelColor: Color,
    modifier: Modifier,
    top: @Composable () -> Unit,
) {
    Column(
        modifier
            .weight(1f)
            .fillMaxHeight()
            .startHairline(CivilightTheme.colors.line)
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        top()
        CivilightText(label, CivilightTheme.type.chip.merge(TextStyle(fontSize = 12.sp)), color = labelColor, maxLines = 1)
    }
}
