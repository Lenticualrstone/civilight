package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.ui.image.ImageCrops
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What a crop session edits: an image slot (CNV-28), or a gallery item (GAL-12), which has no `ImageSlot` of its
 * own (core:model is frozen; see Requests) and uses the gallery ratios `3:4` · `1:1` with holo allowed.
 */
enum class CropTarget { SLOT, GALLERY }

/**
 * The crop editor's session (CNV-28): one [CropEdit] per ratio of the slot, the ratio being edited and
 * the holo flag. `취소` drops the session. Saved as JSON in the screen's saveable state (CNV-35).
 *
 * Portrait and cover slots ([ImageCrops.follows], CST-13): one picture, crops by ratio. The first ratio ([primary]) is always saved; every other
 * ratio has its own crop once edited or stored before ([touched]), else it follows the primary
 * ([isAuto], [CropMath.derived]) and `적용` saves that derived crop. Other slots and the gallery target: `적용`
 * saves the ratios that were opened or edited ([touched]), as before.
 */
@Serializable
data class CropSession(
    val slot: ImageSlot,
    val sourceW: Int,
    val sourceH: Int,
    val current: Ratio,
    val edits: Map<Ratio, CropEdit>,
    val touched: Set<Ratio>,
    val holo: Boolean,
    /** [CropTarget.GALLERY]: [slot] is only a stand-in with the same ratios (the stage's first frame). */
    val target: CropTarget = CropTarget.SLOT,
) {
    val source: PixelSize get() = PixelSize(sourceW, sourceH)
    val ratios: List<Ratio> get() = if (target == CropTarget.GALLERY) GalleryItem.RATIOS else ImageSlots.ratios(slot)
    val edit: CropEdit get() = edits[current] ?: CropEdit()
    val holoAllowed: Boolean get() = target == CropTarget.GALLERY || ImageSlots.holoAllowed(slot)

    /** The ratio chip's label (`갤러리 카드 3:4` · `목록 1:1` for a gallery item, GAL-12). */
    fun chip(ratio: Ratio): String = if (target == CropTarget.GALLERY) GalleryCropChips.chip(ratio) else ImageSlots.chip(slot, ratio)

    /** The ratio the others follow (slot target). */
    val primary: Ratio get() = ratios.first()

    private val derives: Boolean get() = target == CropTarget.SLOT && ratios.size > 1 && ImageCrops.follows(slot)

    /** [ratio] has no crop of its own and follows [primary] (`자동`). */
    fun isAuto(ratio: Ratio): Boolean = derives && ratio != primary && ratio in ratios && ratio !in touched

    /** The crop [ratio] gets on `적용`: its own edit, or the one derived from [primary]. */
    fun cropOf(ratio: Ratio): Crop = if (isAuto(ratio)) derivedOf(ratio) else CropMath.crop(source, ratio, edits[ratio] ?: CropEdit())

    fun readout(): CropReadout {
        val crop = cropOf(current)
        return CropReadout(crop, ImageCopy.readout(crop.w, crop.h, sourceW, sourceH))
    }

    /**
     * A ratio chip or preview: switches the edited crop (the frame's aspect animates, the image stays). An
     * automatic ratio opens on its derived crop and stays automatic until it is edited.
     */
    fun select(ratio: Ratio): CropSession = when {
        ratio !in ratios -> this
        !derives -> copy(current = ratio, touched = touched + ratio)
        isAuto(ratio) -> copy(current = ratio, edits = edits + (ratio to autoEdit(ratio)))
        else -> copy(current = ratio)
    }

    fun zoom(value: Float): CropSession =
        change { CropMath.clamp(source, current, it.copy(zoom = value.coerceIn(CropMath.MIN_ZOOM, CropMath.MAX_ZOOM))) }

    fun zoomStep(step: Int): CropSession = zoom(CropMath.zoomOfStep(step))

    fun pan(dx: Float, dy: Float): CropSession = change { CropMath.pan(source, current, it, dx, dy) }

    fun rotate(): CropSession = change { CropMath.rotate(source, current, it) }

    fun flip(): CropSession = change { CropMath.flip(it) }

    /**
     * `처음대로`: zoom, position, rotation and mirror of the current ratio only. A ratio that can follow
     * [primary] goes back to following it (`자동`).
     */
    fun reset(): CropSession =
        if (derives && current != primary) copy(touched = touched - current, edits = edits + (current to autoEdit(current)))
        else change { CropEdit() }

    fun holo(on: Boolean): CropSession = copy(holo = on && holoAllowed)

    /**
     * The crops `적용` writes, in source pixels of the rotated image: slot target, every ratio (automatic ones
     * derived, written as such, never through a [CropEdit]); gallery target, the touched ratios.
     */
    fun crops(): Map<Ratio, Crop> =
        if (derives) ratios.associateWith(::cropOf)
        else ratios.filter { it in touched }.associateWith { CropMath.crop(source, it, edits[it] ?: CropEdit()) }

    fun encode(): String = JSON.encodeToString(serializer(), this)

    private fun change(transform: (CropEdit) -> CropEdit): CropSession =
        copy(edits = edits + (current to transform(edit)), touched = touched + current)

    private fun derivedOf(ratio: Ratio): Crop =
        CropMath.derived(sourceW, sourceH, CropMath.crop(source, primary, edits[primary] ?: CropEdit()), ratio, ImageCrops.topAligned(slot))

    /** The edit that shows [ratio]'s derived crop when it is opened (zoom clamped to the editor's range). */
    private fun autoEdit(ratio: Ratio): CropEdit = CropMath.clamp(source, ratio, CropMath.editOf(source, derivedOf(ratio)))

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String): CropSession? = runCatching { JSON.decodeFromString(serializer(), text) }.getOrNull()

        /**
         * A new session for an original of [source] pixels in [slot]. When [existing] already shows this
         * original, its crops are the starting edits (re-cropping); otherwise [initialCrops] are (U-08 LIB-85:
         * a gallery item's own crops, which then count as edited so `적용` keeps them), and every other ratio
         * starts at the largest centred frame. The first ratio is the one edited first and counts as edited.
         * A stored crop of another ratio keeps its own place, unless it is exactly the one derived from the
         * stored first ratio (saved automatically before): then it stays automatic and follows again (CST-13).
         */
        fun start(
            slot: ImageSlot,
            source: PixelSize,
            existing: ImageUse?,
            blobHash: String,
            holo: Boolean,
            initialCrops: Map<Ratio, Crop> = emptyMap(),
        ): CropSession {
            val ratios = ImageSlots.ratios(slot)
            val same = existing?.blobHash == blobHash
            val stored: Map<Ratio, Crop> = if (same) {
                ratios.mapNotNull { r -> existing?.cropFor(r)?.let { r to it } }.toMap()
            } else {
                initialCrops.filterKeys { it in ratios }
            }
            val edits = ratios.associateWith { r -> stored[r]?.let { CropMath.editOf(source, it) } ?: CropEdit() }
            val first = ratios.first()
            val top = ImageCrops.topAligned(slot)
            val own = if (!ImageCrops.follows(slot)) {
                if (same) emptySet() else stored.keys
            } else {
                stored.filter { (r, c) ->
                    val parent = stored[first]
                    r == first || parent == null || !CropMath.same(c, CropMath.derived(source.w, source.h, parent, r, top))
                }.keys
            }
            return CropSession(slot, source.w, source.h, first, edits, setOf(first) + own, holo && ImageSlots.holoAllowed(slot))
        }

        /**
         * A session for a gallery item's original (GAL-12): its stored `r3x4` · `r1x1` crops are the starting
         * edits, else the largest centred frame. Holo starts as the item's flag.
         */
        fun startGallery(source: PixelSize, item: GalleryItem): CropSession {
            val edits = GalleryItem.RATIOS.associateWith { r -> item.cropFor(r)?.let { CropMath.editOf(source, it) } ?: CropEdit() }
            val first = GalleryItem.RATIOS.first()
            return CropSession(GALLERY_STAND_IN, source.w, source.h, first, edits, setOf(first), item.holo, CropTarget.GALLERY)
        }

        /** The slot with the gallery's ratios (3:4 first, then 1:1), used where a slot is needed before the session exists. */
        val GALLERY_STAND_IN: ImageSlot = ImageSlot.PROJECT_COVER
    }
}

/** Gallery crop chips (board `GalCrop`). Kept here so the crop code does not depend on the gallery package. */
object GalleryCropChips {
    const val CARD = "갤러리 카드 3:4"
    const val LIST = "목록 1:1"

    fun chip(ratio: Ratio): String = when (ratio) {
        Ratio.R3X4 -> CARD
        Ratio.R1X1 -> LIST
        else -> ImageSlots.parts(ratio).label
    }
}
