package io.github.lenticualrstone.civilight.ui.image

import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.serialName
import kotlin.math.roundToInt

/**
 * One picture, crops by ratio (CST-13, CNV-28): a ratio with no crop of its own follows the slot's first ratio.
 * Only [follows] slots derive. Pure; shared by the crop editor (`feature:canvas`) and every reader (avatar, cover thumbnail, slot images),
 * so old rows with only one crop draw the same derived cut the editor would save.
 */
object ImageCrops {
    /** Width : height of [ratio]. */
    fun parts(ratio: Ratio): Pair<Int, Int> = when (ratio) {
        Ratio.R3X4 -> 3 to 4
        Ratio.R1X1 -> 1 to 1
        Ratio.R4X5 -> 4 to 5
        Ratio.R9X16 -> 9 to 16
        Ratio.R4X3 -> 4 to 3
        Ratio.R16X9 -> 16 to 9
        Ratio.R21X9 -> 21 to 9
    }

    /** The ratio the crop editor opens with in [slot] (CNV-25 table order); the others derive from it. */
    fun primary(slot: ImageSlot): Ratio = when (slot) {
        ImageSlot.PROJECT_COVER -> Ratio.R3X4
        ImageSlot.CHARACTER_PORTRAIT -> Ratio.R4X5
        ImageSlot.CHARACTER_BACKGROUND -> Ratio.R9X16
        ImageSlot.CARD_REFERENCE -> Ratio.R16X9
        ImageSlot.PAGE_HEADER -> Ratio.R21X9
    }

    /**
     * Slots whose second ratio follows the first: the portrait (`아바타 1:1` from `카드 4:5`) and the project cover
     * (`목록 1:1` from `카드 3:4`). The background's `4:3` is not cut from its `9:16` (it would be a thin band).
     */
    fun follows(slot: ImageSlot): Boolean = slot == ImageSlot.CHARACTER_PORTRAIT || slot == ImageSlot.PROJECT_COVER

    /** Derived crops keep the top of the parent (faces). */
    fun topAligned(slot: ImageSlot): Boolean = follows(slot)

    /**
     * The largest [to]-ratio rectangle inside [from], centred across and top-aligned ([top]) or centred down.
     * Same pixel space as [from] (rotated, mirrored original), so it is inside the original too; rotation and
     * mirror are inherited. [sourceW] × [sourceH] (the upright original) only sets `zoom` (1 = the largest frame
     * of [to] in the rotated original); without it the parent's zoom is kept.
     */
    fun derived(from: Crop, to: Ratio, top: Boolean = true, sourceW: Int? = null, sourceH: Int? = null): Crop {
        val (pw, ph) = parts(to)
        val fw = from.w.coerceAtLeast(1)
        val fh = from.h.coerceAtLeast(1)
        val wider = fw.toLong() * ph > fh.toLong() * pw
        val w: Int
        val h: Int
        if (wider) {
            h = fh
            w = (fh.toDouble() * pw / ph).roundToInt().coerceIn(1, fw)
        } else {
            w = fw
            h = (fw.toDouble() * ph / pw).roundToInt().coerceIn(1, fh)
        }
        val x = from.x + (fw - w) / 2
        val y = if (top) from.y else from.y + (fh - h) / 2
        val zoom = if (sourceW != null && sourceH != null && sourceW > 0 && sourceH > 0) {
            val turned = ((from.rotation % 360) + 360) % 360 % 180 == 90
            val iw = if (turned) sourceH else sourceW
            val ih = if (turned) sourceW else sourceH
            val largestW = if (iw.toLong() * ph > ih.toLong() * pw) ih.toDouble() * pw / ph else iw.toDouble()
            (largestW / w).toFloat().coerceAtLeast(1f)
        } else {
            from.zoom
        }
        return Crop(x = x, y = y, w = w, h = h, rotation = from.rotation, flipH = from.flipH, zoom = zoom)
    }

    /**
     * The crop a reader draws for [ratio]: the stored one, else (a [follows] slot, [ratio] not its first) the one
     * derived from the stored first ratio, else null (centre fill).
     */
    fun cropFor(use: ImageUse, ratio: Ratio): Crop? {
        use.cropFor(ratio)?.let { return it }
        val slot = ImageSlot.entries.firstOrNull { it.serialName == use.slot.raw } ?: return null
        if (!follows(slot) || ratio == primary(slot)) return null
        val parent = use.cropFor(primary(slot)) ?: return null
        return derived(parent, ratio, top = topAligned(slot))
    }
}
