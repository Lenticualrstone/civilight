package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.model.asset.Crop
import kotlin.math.abs

/**
 * The picture's transform at the start of a crop step (rotate, mirror, reset, zoom step): the new picture
 * is already drawn, and this layer makes it look like the old one, then eases to identity on `standard`
 * (audit §4 canvas 6). All about the frame's centre, in screen px.
 */
data class CropLayerStart(
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    /** Degrees, clockwise. */
    val rotation: Float = 0f,
    /** −1 when the mirror just flipped (the picture squashes through 0 and opens the other way), else 1. */
    val flipX: Float = 1f,
) {
    val isIdentity: Boolean get() = scale == 1f && translationX == 0f && translationY == 0f && rotation == 0f && flipX == 1f

    /** The layer at [progress] 0 (old look) … 1 (new picture as drawn). */
    fun at(progress: Float): CropLayerStart {
        val p = progress.coerceIn(0f, 1f)
        fun lerp(a: Float, b: Float) = a + (b - a) * p
        return CropLayerStart(lerp(scale, 1f), lerp(translationX, 0f), lerp(translationY, 0f), lerp(rotation, 0f), lerp(flipX, 1f))
    }
}

/** Pure geometry of the crop-step animation; JVM-tested. */
object CropTransition {
    /**
     * The start layer between the crop just drawn ([old]) and the new one ([new]) for the same frame width
     * [frameWidthPx]. A turn or mirror animates rotation / flip only; zoom and position changes animate scale
     * and translation so the old picture's centre stays under the frame's centre.
     */
    fun start(old: Crop, new: Crop, frameWidthPx: Float): CropLayerStart {
        if (old.rotation != new.rotation || old.flipH != new.flipH) {
            val turn = normalize(old.rotation - new.rotation)
            // A mirrored picture turns the other way on screen.
            val rotation = if (old.flipH) -turn else turn
            // The largest frame differs after a turn when the frame is not square: the picture also changes size.
            val sizeScale = new.w.coerceAtLeast(1).toFloat() / old.w.coerceAtLeast(1)
            return CropLayerStart(scale = sizeScale, rotation = rotation, flipX = if (old.flipH != new.flipH) -1f else 1f)
        }
        if (same(old, new)) return CropLayerStart()
        val sOld = frameWidthPx / old.w.coerceAtLeast(1)
        val sNew = frameWidthPx / new.w.coerceAtLeast(1)
        val cOldX = old.x + old.w / 2f
        val cOldY = old.y + old.h / 2f
        val cNewX = new.x + new.w / 2f
        val cNewY = new.y + new.h / 2f
        return CropLayerStart(scale = sOld / sNew, translationX = (cNewX - cOldX) * sOld, translationY = (cNewY - cOldY) * sOld)
    }

    /** Degrees folded into −180 … 180 (a quarter turn stays a quarter turn). */
    fun normalize(degrees: Int): Float {
        var d = degrees % 360
        if (d > 180) d -= 360
        if (d <= -180) d += 360
        return d.toFloat()
    }

    private fun same(a: Crop, b: Crop): Boolean =
        a.x == b.x && a.y == b.y && a.w == b.w && a.h == b.h && abs(a.zoom - b.zoom) < 0.0001f
}
