package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.feature.map.canvas.StampGlyphs
import io.github.lenticualrstone.civilight.model.Point
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * The handles of a selected stamp (WLD-60 `모서리 → 크기 · 위 손잡이 → 회전`), used tap-to-place like
 * vertex handles: tap the handle, then tap the map where it should go. Rotation snaps to 15°
 * (release → nearest 15°); size snaps to the steps `1 · 2 · 3`.
 */
object StampHandles {
    /** Handle indices (the editor's `handle` while a stamp is selected). */
    const val ROTATE = 0
    const val SIZE = 1

    /** Degrees of the direction from [center] to [to], 0° = up, clockwise, snapped to 15°. */
    fun rotationToward(center: Point, to: Point): Int {
        val deg = Math.toDegrees(atan2((to.x - center.x).toDouble(), -(to.y - center.y).toDouble())).toInt()
        return MapEdits.snapRotation(deg)
    }

    /** The size step whose corner (radius × √2) lies nearest to [to]. */
    fun sizeToward(center: Point, to: Point): Int {
        val d = hypot((to.x - center.x).toDouble(), (to.y - center.y).toDouble())
        return (1..3).minByOrNull { kotlin.math.abs(StampGlyphs.radius(it) * 1.41421356 - d) } ?: 2
    }
}
