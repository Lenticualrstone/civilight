package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * DS-44 revealed button row: 192 wide (3 × 64), `raise` fill; three buttons, each a centred column (gap 4,
 * `chip` role `ink`, 1 dp `line` left border): `시간` (with `◆` 11 sp above and the label in `acc` when the
 * block has a change), `참고` (`Paper` 18), `속성` (`Props` 18). Only the buttons act.
 */
@Composable
fun SwipeTabsRow(
    onTime: () -> Unit,
    onReference: () -> Unit,
    onProperties: () -> Unit,
    modifier: Modifier = Modifier,
    timeHasChange: Boolean = false,
    timeLabel: String = "시간",
    referenceLabel: String = "참고",
    propertiesLabel: String = "속성",
) {
    val colors = CivilightTheme.colors
    Row(modifier.width(CivilightTheme.sizes.swipeRow).fillMaxHeight().background(colors.raise)) {
        SwipeButton(timeLabel, onTime, labelColor = if (timeHasChange) colors.acc else colors.ink) {
            if (timeHasChange) MarkerGlyph(MarkerKind.CHANGE, size = 11.dp)
        }
        SwipeButton(referenceLabel, onReference) { CivilightIcon(IconId.Paper, null, size = 18.dp, tint = colors.ink2) }
        SwipeButton(propertiesLabel, onProperties) { CivilightIcon(IconId.Props, null, size = 18.dp, tint = colors.ink2) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SwipeButton(
    label: String,
    onClick: () -> Unit,
    labelColor: androidx.compose.ui.graphics.Color = CivilightTheme.colors.ink,
    top: @Composable () -> Unit,
) {
    Column(
        Modifier
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

/**
 * DS-44 swipe-left reveal: the block slides left by 192 dp over its own `bg` fill to show [actions]
 * (usually [SwipeTabsRow]). Only left swipes exist; nothing executes on swipe. Motion: `Swipe` 260 ms
 * (reduced: 120 ms fade timing). Disabled while the block is being typed ([enabled] false).
 */
@Composable
fun SwipeRevealBox(
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val reduced = CivilightTheme.reducedMotion
    val widthPx = with(LocalDensity.current) { CivilightTheme.sizes.swipeRow.toPx() }
    val offset = remember { Animatable(if (revealed) -widthPx else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(revealed, widthPx) {
        offset.animateTo(if (revealed) -widthPx else 0f, CivilightMotion.spec(CivilightMotion.swipe, reduced))
    }
    Box(modifier) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.graphicsLayer { alpha = (-offset.value / widthPx).coerceIn(0f, 1f) }) { actions() }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(CivilightTheme.colors.bg)
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
