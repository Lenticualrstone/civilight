package io.github.lenticualrstone.civilight.designsystem.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction

/**
 * DS-64 focus ring: a 2 dp outline [gap] outside the shape while keyboard / D-pad focused.
 */
fun Modifier.focusRing(focused: Boolean, shape: Shape, color: Color, width: Dp = 2.dp, gap: Dp = 2.dp): Modifier =
    drawWithContent {
        drawContent()
        if (focused) {
            val inset = gap.toPx() + width.toPx() / 2f
            val ringSize = Size(size.width + inset * 2, size.height + inset * 2)
            val outline = shape.createOutline(ringSize, layoutDirection, this)
            translate(-inset, -inset) { drawOutline(outline, color, style = Stroke(width.toPx())) }
        }
    }

/** Interaction kinds for [pressable]. */
sealed interface PressAction {
    /**
     * A tap, and (0.2.3) an optional long press with its accessibility label. The long press fires only while the
     * finger stays inside the touch slop (`GestureRules.cancelsLongPress`), so it never fights a swipe row, a
     * pager or a scroll that starts on the same finger.
     */
    data class Click(
        val onClick: () -> Unit,
        val label: String? = null,
        val onLongClick: (() -> Unit)? = null,
        val longClickLabel: String? = null,
    ) : PressAction
    data class Toggle(val value: Boolean, val onValueChange: (Boolean) -> Unit) : PressAction
    data class Select(val selected: Boolean, val onClick: () -> Unit) : PressAction
}

/**
 * The shared interaction of every component: click / toggle / select without ripple, the proposed
 * pressed scale 0.94 over `pressMs` on the `standard` curve, the focus ring, and the DS-11 touch area.
 * Where nothing scales ([scaleOnPress] false, or reduced motion) a pressed overlay of `ink` at 6 % fades in
 * and out inside [shape] instead (S7), unless [pressOverlay] is false (the caller draws its own state layer).
 */
@Composable
fun Modifier.pressable(
    action: PressAction,
    enabled: Boolean = true,
    role: Role? = null,
    hitTarget: HitTarget? = null,
    shape: Shape = RectangleShape,
    scaleOnPress: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    pressOverlay: Boolean = true,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val motion = LocalMotion.current
    val scaling = scaleOnPress && !motion.reduced
    val scale by animateFloatAsState(
        targetValue = if (pressed && scaling && enabled) CivilightMotion.pressScale else 1f,
        animationSpec = motion.spec(CivilightMotion.press),
        label = "press",
    )
    val overlay = animatedFraction(pressed && enabled && !scaling && pressOverlay, CivilightMotion.press, label = "pressOverlay")
    val ink = CivilightTheme.colors.ink
    val interactive = when (action) {
        is PressAction.Click -> if (action.onLongClick != null) {
            Modifier.combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClickLabel = action.label,
                role = role,
                onLongClickLabel = action.longClickLabel,
                onLongClick = action.onLongClick,
                onClick = action.onClick,
            )
        } else {
            Modifier.clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClickLabel = action.label,
                role = role,
                onClick = action.onClick,
            )
        }
        is PressAction.Toggle -> Modifier.toggleable(
            value = action.value,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onValueChange = action.onValueChange,
        )
        is PressAction.Select -> Modifier.selectable(
            selected = action.selected,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = action.onClick,
        )
    }
    val withHit = if (hitTarget != null) expandedHitArea(hitTarget, interactive) else then(interactive)
    return withHit
        .focusRing(focused, shape, CivilightTheme.colors.acc)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            val a = overlay.value
            if (a > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), ink, alpha = PRESS_OVERLAY_ALPHA * a)
        }
}

/** Pressed overlay strength (`ink` at 6 %, S7). */
const val PRESS_OVERLAY_ALPHA: Float = 0.06f

/** Convenience for the common click case. */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    hitTarget: HitTarget? = null,
    shape: Shape = RectangleShape,
    scaleOnPress: Boolean = true,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    pressOverlay: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
): Modifier = pressable(
    action = PressAction.Click(onClick, onClickLabel, onLongClick, onLongClickLabel),
    enabled = enabled,
    role = role,
    hitTarget = hitTarget,
    shape = shape,
    scaleOnPress = scaleOnPress,
    interactionSource = interactionSource,
    pressOverlay = pressOverlay,
)

/** Draws a 2 dp `acc` bar at the start edge and the `sel` fill (DS-40 selected row). */
fun Modifier.selectionFill(selected: Boolean, fill: Color, bar: Color, barWidth: Dp = 2.dp): Modifier =
    if (!selected) this else drawWithContent {
        drawRect(fill)
        drawRect(bar, Offset.Zero, Size(barWidth.toPx(), size.height))
        drawContent()
    }
