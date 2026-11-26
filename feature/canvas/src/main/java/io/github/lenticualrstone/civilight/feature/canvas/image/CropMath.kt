package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.ui.image.ImageCrops
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

/** A size in pixels. */
data class PixelSize(val w: Int, val h: Int)

/** A rectangle in float units (screen px). */
data class FrameRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

/**
 * One ratio's edit in the crop editor (CNV-28): zoom (1 = the largest frame of the ratio inside the
 * rotated image), the frame centre as fractions of the rotated, mirrored image, rotation and mirror.
 */
@Serializable
data class CropEdit(
    val zoom: Float = 1f,
    val cx: Float = 0.5f,
    val cy: Float = 0.5f,
    val rotation: Int = 0,
    val flipH: Boolean = false,
)

/** Readout under the crop frame (T-CNV-30). */
data class CropReadout(val crop: Crop, val label: String)

/**
 * Crop geometry (CNV-28, CNV-29). Same convention as the decoder ([SlotImages.decode]): the original is
 * rotated, then mirrored, then cut by `{x, y, w, h}` in those pixels. The original is never changed.
 */
object CropMath {
    const val MIN_ZOOM = 1f

    /** Q-IM-04 default: 1.0× … 4.0× in steps of 0.1. */
    const val MAX_ZOOM = 4f
    const val ZOOM_STEP = 0.1f
    val zoomSteps: Int = ((MAX_ZOOM - MIN_ZOOM) / ZOOM_STEP).roundToInt() + 1

    fun zoomOfStep(step: Int): Float = (MIN_ZOOM + step.coerceIn(0, zoomSteps - 1) * ZOOM_STEP).let { (it * 10f).roundToInt() / 10f }

    fun stepOfZoom(zoom: Float): Int = ((zoom - MIN_ZOOM) / ZOOM_STEP).roundToInt().coerceIn(0, zoomSteps - 1)

    fun normalizeRotation(degrees: Int): Int = ((degrees % 360) + 360) % 360 / 90 * 90

    /** Size of the original after [rotation]. */
    fun rotated(source: PixelSize, rotation: Int): PixelSize =
        if (normalizeRotation(rotation) % 180 == 90) PixelSize(source.h, source.w) else source

    /** The largest frame of [ratio] inside [image] (zoom 1.0, T-CNV-31). */
    fun largestFrame(image: PixelSize, ratio: Ratio): PixelSize {
        val p = ImageSlots.parts(ratio)
        return if (image.w.toLong() * p.h > image.h.toLong() * p.w) {
            PixelSize((image.h * p.value).roundToInt().coerceAtMost(image.w), image.h)
        } else {
            PixelSize(image.w, (image.w / p.value).roundToInt().coerceAtMost(image.h))
        }
    }

    /** Crop size of [ratio] at [zoom] inside [image]; rounded, never truncated (3024 / 1.4 = 2160). */
    fun cropSize(image: PixelSize, ratio: Ratio, zoom: Float): PixelSize {
        val frame = largestFrame(image, ratio)
        val z = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val w = (frame.w / z.toDouble()).roundToInt().coerceIn(1, image.w)
        val h = (frame.h / z.toDouble()).roundToInt().coerceIn(1, image.h)
        return PixelSize(w, h)
    }

    /** The stored crop of [edit] for [ratio] on an original of [source] pixels. */
    fun crop(source: PixelSize, ratio: Ratio, edit: CropEdit): Crop {
        val image = rotated(source, edit.rotation)
        val size = cropSize(image, ratio, edit.zoom)
        val x = (edit.cx * image.w - size.w / 2f).roundToInt().coerceIn(0, image.w - size.w)
        val y = (edit.cy * image.h - size.h / 2f).roundToInt().coerceIn(0, image.h - size.h)
        return Crop(x = x, y = y, w = size.w, h = size.h, rotation = normalizeRotation(edit.rotation), flipH = edit.flipH, zoom = edit.zoom)
    }

    /** `cropReadout` (T-CNV-30): the crop and `<cropW> × <cropH> · 원본 <W> × <H>` (the file's own size). */
    fun cropReadout(source: PixelSize, ratio: Ratio, edit: CropEdit): CropReadout {
        val crop = crop(source, ratio, edit)
        return CropReadout(crop, ImageCopy.readout(crop.w, crop.h, source.w, source.h))
    }

    /** The edit that reproduces a stored [crop] (re-cropping from the slot). */
    fun editOf(source: PixelSize, crop: Crop): CropEdit {
        val image = rotated(source, crop.rotation)
        return CropEdit(
            zoom = crop.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM),
            cx = (crop.x + crop.w / 2f) / image.w,
            cy = (crop.y + crop.h / 2f) / image.h,
            rotation = normalizeRotation(crop.rotation),
            flipH = crop.flipH,
        )
    }

    /** Keeps the frame inside the image: the centre may move only as far as the crop allows. */
    fun clamp(source: PixelSize, ratio: Ratio, edit: CropEdit): CropEdit {
        val image = rotated(source, edit.rotation)
        val size = cropSize(image, ratio, edit.zoom)
        val hx = size.w / 2f / image.w
        val hy = size.h / 2f / image.h
        return edit.copy(cx = edit.cx.coerceIn(hx, 1f - hx), cy = edit.cy.coerceIn(hy, 1f - hy))
    }

    /** Dragging the image by ([dx], [dy]) source pixels moves the frame centre the other way. */
    fun pan(source: PixelSize, ratio: Ratio, edit: CropEdit, dx: Float, dy: Float): CropEdit {
        val image = rotated(source, edit.rotation)
        return clamp(source, ratio, edit.copy(cx = edit.cx - dx / image.w, cy = edit.cy - dy / image.h))
    }

    /**
     * 90° clockwise on the original (the decoder's `postRotate(90)`): the centre turns with the picture.
     * With the mirror on, the shown picture is mirrored after the turn, so the centre maps to (cy, 1 − cx).
     */
    fun rotate(source: PixelSize, ratio: Ratio, edit: CropEdit): CropEdit {
        val (cx, cy) = if (edit.flipH) edit.cy to 1f - edit.cx else 1f - edit.cy to edit.cx
        val turned = edit.copy(rotation = normalizeRotation(edit.rotation + 90), cx = cx, cy = cy)
        return clamp(source, ratio, turned)
    }

    /** `좌우 반전`: the picture mirrors under the frame, which stays where it is. */
    fun flip(edit: CropEdit): CropEdit = edit.copy(flipH = !edit.flipH)

    /** The frame of [ratio] inside a stage of [stageW] × [stageH], inset by [margin] (DS `CropFrame`). */
    fun frameIn(stageW: Float, stageH: Float, ratio: Float, margin: Float): FrameRect {
        val availW = (stageW - margin * 2).coerceAtLeast(1f)
        val availH = (stageH - margin * 2).coerceAtLeast(1f)
        val (fw, fh) = if (availW / availH > ratio) (availH * ratio) to availH else availW to (availW / ratio)
        return FrameRect((stageW - fw) / 2, (stageH - fh) / 2, fw, fh)
    }

    /**
     * The [to]-ratio crop cut from the [from] crop of an original of [sourceW] × [sourceH] (CST-13: one picture,
     * the avatar follows the card): inside [from], top-aligned when [top], rotation and mirror inherited.
     */
    fun derived(sourceW: Int, sourceH: Int, from: Crop, to: Ratio, top: Boolean = true): Crop =
        ImageCrops.derived(from, to, top, sourceW, sourceH)

    /** True when two crops cut the same pixels. */
    fun same(a: Crop?, b: Crop?): Boolean =
        a != null && b != null && a.x == b.x && a.y == b.y && a.w == b.w && a.h == b.h &&
            a.rotation == b.rotation && a.flipH == b.flipH && abs(a.zoom - b.zoom) < 0.001f
}
