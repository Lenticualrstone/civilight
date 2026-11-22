package io.github.lenticualrstone.civilight.designsystem.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** The DS-11 expansion rule of a tappable element. */
enum class HitTarget {
    /** `.tag`: width `max(w + 8, 44)`, height `max(h, 44)`. */
    CHIP,

    /** `.lens`: as [CHIP]. */
    LENS_CHIP,

    /** `.sw`: as [CHIP]. */
    SWITCH,

    /** `.hit` (32 dp mic, inline search boxes): as [CHIP]. */
    SMALL,

    /** Fixed 44 × 44 centred. */
    RADIO,
    CHECKBOX,
    VERTEX,

    /** `.lk` 36 × 36: 4 dp on every side. */
    LOCK_TOGGLE,

    /** Already ≥ 44: `max(w, 44) × max(h, 44)`. */
    ICON_BUTTON,
    BUTTON,
}

/** A touch area in dp. */
data class HitSize(val width: Float, val height: Float)

/** DS-11: the touch area (dp) of an element drawn [width] × [height] dp. */
fun hitArea(target: HitTarget, width: Float, height: Float): HitSize = when (target) {
    HitTarget.CHIP, HitTarget.LENS_CHIP, HitTarget.SWITCH, HitTarget.SMALL ->
        HitSize(max(width + 8f, MIN_HIT), max(height, MIN_HIT))
    HitTarget.RADIO, HitTarget.CHECKBOX, HitTarget.VERTEX -> HitSize(MIN_HIT, MIN_HIT)
    HitTarget.LOCK_TOGGLE -> HitSize(max(width + 8f, MIN_HIT), max(height + 8f, MIN_HIT))
    HitTarget.ICON_BUTTON, HitTarget.BUTTON -> HitSize(max(width, MIN_HIT), max(height, MIN_HIT))
}

/** `hit-min`. */
const val MIN_HIT: Float = 44f

private class HitHolder {
    var constraints: Constraints = Constraints()
    var padX: Int = 0
    var padY: Int = 0
}

/**
 * Extends the touch area of [interactive] (a clickable / toggleable modifier) to [hitArea] without changing
 * layout: the element reports its drawn size to its parent while the interactive node covers the larger,
 * centred area. Touches beyond a clipping ancestor are lost, as with any clipped content.
 */
fun Modifier.expandedHitArea(target: HitTarget, interactive: Modifier): Modifier {
    val holder = HitHolder()
    return this
        .layout { measurable, constraints ->
            holder.constraints = constraints
            val extra = 2 * MAX_EXPANSION.roundToPx()
            val loose = Constraints(
                maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else Constraints.Infinity,
                maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight + extra else Constraints.Infinity,
            )
            val placeable = measurable.measure(loose)
            val w = (placeable.width - 2 * holder.padX).coerceAtLeast(0)
            val h = (placeable.height - 2 * holder.padY).coerceAtLeast(0)
            layout(w, h) { placeable.place(-holder.padX, -holder.padY) }
        }
        .then(interactive)
        .layout { measurable, _ ->
            val content = measurable.measure(holder.constraints)
            val area = hitArea(target, content.width.toDp().value, content.height.toDp().value)
            holder.padX = ((area.width.dp.roundToPx() - content.width).coerceAtLeast(0)) / 2
            holder.padY = ((area.height.dp.roundToPx() - content.height).coerceAtLeast(0)) / 2
            layout(content.width + 2 * holder.padX, content.height + 2 * holder.padY) {
                content.place(holder.padX, holder.padY)
            }
        }
}

private val MAX_EXPANSION = 44.dp
