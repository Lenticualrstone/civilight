package io.github.lenticualrstone.civilight.feature.map.export

import io.github.lenticualrstone.civilight.feature.map.canvas.TilePlan
import io.github.lenticualrstone.civilight.model.app.LargeMapExport
import kotlin.math.max
import kotlin.math.roundToInt

/** A horizontal band of the source image decoded at once: rows [top, bottom) at [sample]. */
data class Band(val top: Int, val bottom: Int, val sample: Int)

/**
 * The numbers of `지도 내보내기` (WLD-51): the output size for `4K` (long edge 3840 px, default) or
 * `8K` (7680 px), and how a large base image is read without decoding it whole (bands of
 * subsampled region decoding; full decoding above `TilePlan.FULL_DECODE_MAX` never happens).
 */
object ExportPlan {
    const val LONG_4K = 3840
    const val LONG_8K = 7680

    /** Source rows decoded per band (before subsampling); keeps one band at a few tens of MB. */
    const val BAND_ROWS = 1024

    fun longEdge(size: LargeMapExport): Int = if (size == LargeMapExport.K8) LONG_8K else LONG_4K

    /** Output width and height for a base of [aspect] (width / height). */
    fun size(aspect: Float, longEdge: Int): Pair<Int, Int> {
        val a = if (aspect.isFinite() && aspect > 0f) aspect else 1f
        return if (a >= 1f) longEdge to max(1, (longEdge / a).roundToInt()) else max(1, (longEdge * a).roundToInt()) to longEdge
    }

    /**
     * Bands of an image [imageW] × [imageH] drawn [outW] wide: the subsample is the largest power of
     * two that still gives at least the output's detail; the image is never decoded whole when it
     * is larger than the full-decode threshold.
     */
    fun bands(imageW: Int, imageH: Int, outW: Int): List<Band> {
        if (imageW <= 0 || imageH <= 0) return emptyList()
        val sample = TilePlan.sampleForScale(outW.toFloat() / imageW)
        if (!TilePlan.needsTiling(imageW, imageH)) return listOf(Band(0, imageH, sample))
        val rows = BAND_ROWS * sample
        return (0 until imageH step rows).map { top -> Band(top, minOf(imageH, top + rows), sample) }
    }

    /** Pixels per layout unit of the overlay drawing (1 unit ≈ 1 dp on a 1000 px wide map). */
    fun unit(outW: Int, outH: Int): Float = max(outW, outH) / 1000f

    /** `<map name>-4K.png`, safe for file systems. */
    fun fileName(mapName: String, size: LargeMapExport): String {
        val clean = mapName.trim().replace(Regex("""[\\/:*?"<>|\s]+"""), "_").ifEmpty { "map" }
        return "$clean-${if (size == LargeMapExport.K8) "8K" else "4K"}.png"
    }
}
