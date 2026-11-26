package io.github.lenticualrstone.civilight.feature.cast.image

import io.github.lenticualrstone.civilight.ui.image.ImageCrops
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Where a character image is shown and which crop it uses (CST-13): the portrait as the `1:1`
 * avatar and the `4:5` card, the background as `9:16` on the cover screen and `4:3` inside.
 */
enum class ImagePlace(val slot: ImageSlot, val ratio: Ratio) {
    AVATAR(ImageSlot.CHARACTER_PORTRAIT, Ratio.R1X1),
    CARD(ImageSlot.CHARACTER_PORTRAIT, Ratio.R4X5),
    BACKGROUND_COVER(ImageSlot.CHARACTER_BACKGROUND, Ratio.R9X16),
    BACKGROUND_INNER(ImageSlot.CHARACTER_BACKGROUND, Ratio.R4X3),
}

/** What to draw in one place: a preset by id, or a blob with the crop of that place's ratio (none = centre fill). */
data class SlotImage(val presetId: String?, val blobHash: String?, val crop: Crop?, val holo: Boolean, val ratio: Ratio)

/** A slot row of the record's image list: `인물 초상 | 잿빛 새벽`. */
data class ImageSlotRow(val slot: ImageSlot, val label: String, val value: String, val chosen: Boolean)

/** The 8 built-in presets (CNV-26): name and the `preset:<id>` ids (spec 50 [PROPOSED] ids). */
data class Preset(val id: String, val name: String)

/**
 * Pure reading of a character's two image slots (CST-13). Picking, cropping, presets and holo are
 * the image area's (`ImagePickSheet`, `ImageCrop`); this defines the slots and where each crop is used.
 */
object CharacterImages {
    const val PORTRAIT = CastUiCopy.PORTRAIT
    const val BACKGROUND = CastUiCopy.BACKGROUND
    const val PICK = CastUiCopy.PICK_IMAGE
    const val OWN_IMAGE = CastUiCopy.OWN_IMAGE
    const val HOLO = CastUiCopy.HOLO

    /** Preset ids: `ash-dawn` from the fixture, the other seven spec 50 [PROPOSED] (Q-IM-06). */
    private val PRESET_IDS = listOf("ash-dawn", "lantern", "deep-sea", "mist-forest", "desert-ruin", "nebula", "red-citadel", "snowfield")

    val PRESETS: List<Preset> = PRESET_IDS.zip(CastUiCopy.PRESET_NAMES) { id, name -> Preset(id, name) }

    fun presetIndex(id: String): Int? = PRESETS.indexOfFirst { it.id == id }.takeIf { it >= 0 }

    fun image(doc: ProjectDoc, characterKey: Key, place: ImagePlace): SlotImage? {
        val use = doc.index.imageUse(ImageOwnerKind.CHARACTER, characterKey, place.slot) ?: return null
        if (doc.liveness.let { !it.isLive(use) }) return null
        return SlotImage(
            presetId = use.presetId,
            blobHash = use.blobHash,
            crop = ImageCrops.cropFor(use, place.ratio),
            holo = place.slot == ImageSlot.CHARACTER_PORTRAIT && use.holo,
            ratio = place.ratio,
        )
    }

    /** The background of the record: `9:16` on the cover screen, `4:3` inside. */
    fun background(doc: ProjectDoc, characterKey: Key, inner: Boolean): SlotImage? =
        image(doc, characterKey, if (inner) ImagePlace.BACKGROUND_INNER else ImagePlace.BACKGROUND_COVER)

    /** The two slot rows (`인물 초상 · 인물 배경`): the preset name, `내 이미지`, or `이미지 고르기`. */
    fun slotRows(doc: ProjectDoc, characterKey: Key): List<ImageSlotRow> =
        listOf(ImageSlot.CHARACTER_PORTRAIT to PORTRAIT, ImageSlot.CHARACTER_BACKGROUND to BACKGROUND).map { (slot, label) ->
            val use = doc.index.imageUse(ImageOwnerKind.CHARACTER, characterKey, slot)?.takeIf { doc.liveness.isLive(it) }
            val value = when {
                use == null -> PICK
                use.presetId != null -> PRESETS.firstOrNull { it.id == use.presetId }?.name ?: PICK
                else -> OWN_IMAGE
            }
            ImageSlotRow(slot, label, value, use != null)
        }

    /** Width ÷ height of a ratio. */
    fun aspect(ratio: Ratio): Float = when (ratio) {
        Ratio.R3X4 -> 3f / 4f
        Ratio.R1X1 -> 1f
        Ratio.R4X5 -> 4f / 5f
        Ratio.R9X16 -> 9f / 16f
        Ratio.R4X3 -> 4f / 3f
        Ratio.R16X9 -> 16f / 9f
        Ratio.R21X9 -> 21f / 9f
    }
}
