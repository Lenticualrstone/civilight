package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What a crop session edits: an image slot (CNV-28), or a gallery item (GAL-12), which has no `ImageSlot` of its
 * own (core:model is frozen; see Requests) and uses the gallery ratios `3:4` · `1:1` with holo allowed.
 */
enum class CropTarget { SLOT, GALLERY }

/**
 * The crop editor's session (CNV-28): one [CropEdit] per ratio of the slot, the ratio being edited and
 * the holo flag. `적용` saves the crops of the ratios that were edited ([touched]); `취소` drops the
 * session. Saved as JSON in the screen's saveable state (CNV-35).
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

    fun readout(): CropReadout = CropMath.cropReadout(source, current, edit)

    /** A ratio chip: switches the edited crop (the frame's aspect animates, the image stays). */
    fun select(ratio: Ratio): CropSession = if (ratio in ratios) copy(current = ratio, touched = touched + ratio) else this

    fun zoom(value: Float): CropSession =
        change { CropMath.clamp(source, current, it.copy(zoom = value.coerceIn(CropMath.MIN_ZOOM, CropMath.MAX_ZOOM))) }

    fun zoomStep(step: Int): CropSession = zoom(CropMath.zoomOfStep(step))

    fun pan(dx: Float, dy: Float): CropSession = change { CropMath.pan(source, current, it, dx, dy) }

    fun rotate(): CropSession = change { CropMath.rotate(source, current, it) }

    fun flip(): CropSession = change { CropMath.flip(it) }

    /** `처음대로`: zoom, position, rotation and mirror of the current ratio only. */
    fun reset(): CropSession = change { CropEdit() }

    fun holo(on: Boolean): CropSession = copy(holo = on && holoAllowed)

    /** The crops `적용` writes: one per edited ratio, in source pixels of the rotated image. */
    fun crops(): Map<Ratio, Crop> = ratios.filter { it in touched }.associateWith { CropMath.crop(source, it, edits[it] ?: CropEdit()) }

    fun encode(): String = JSON.encodeToString(serializer(), this)

    private fun change(transform: (CropEdit) -> CropEdit): CropSession =
        copy(edits = edits + (current to transform(edit)), touched = touched + current)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String): CropSession? = runCatching { JSON.decodeFromString(serializer(), text) }.getOrNull()

        /**
         * A new session for an original of [source] pixels in [slot]. When [existing] already shows this
         * original, its crops are the starting edits (re-cropping); otherwise every ratio starts at the
         * largest centred frame. The first ratio is the one edited first and counts as edited.
         */
        fun start(slot: ImageSlot, source: PixelSize, existing: ImageUse?, blobHash: String, holo: Boolean): CropSession {
            val ratios = ImageSlots.ratios(slot)
            val same = existing?.blobHash == blobHash
            val edits = ratios.associateWith { r ->
                val stored = if (same) existing?.cropFor(r) else null
                stored?.let { CropMath.editOf(source, it) } ?: CropEdit()
            }
            val first = ratios.first()
            return CropSession(slot, source.w, source.h, first, edits, setOf(first), holo && ImageSlots.holoAllowed(slot))
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
