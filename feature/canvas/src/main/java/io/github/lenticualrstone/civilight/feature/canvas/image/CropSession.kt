package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
) {
    val source: PixelSize get() = PixelSize(sourceW, sourceH)
    val ratios: List<Ratio> get() = ImageSlots.ratios(slot)
    val edit: CropEdit get() = edits[current] ?: CropEdit()
    val holoAllowed: Boolean get() = ImageSlots.holoAllowed(slot)

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
    }
}
