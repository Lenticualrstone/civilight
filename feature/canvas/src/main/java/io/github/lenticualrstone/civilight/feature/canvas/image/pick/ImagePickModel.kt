package io.github.lenticualrstone.civilight.feature.canvas.image.pick

import io.github.lenticualrstone.civilight.feature.canvas.image.HoloRules
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwners
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImage
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** One tile of the picker grid; [source] is what the slot would store (`preset:<id>` or a hash). */
data class PickTile(val source: String, val name: String, val preset: PresetImage?, val blob: Blob?)

/** What `고르기` does with the selected tile. */
sealed interface PickAction {
    /** A preset fills the slot at once (presets are not cropped, Q-IM-03 default). */
    data class PlacePreset(val presetId: String) : PickAction

    /** An original opens the crop editor (new crop, or re-crop of the one in the slot). */
    data class Crop(val blobHash: String) : PickAction
}

/** The picker sheet as the board draws it (CNV-27, board `ImagePick`). */
data class ImagePickView(
    val title: String,
    val subtitle: String,
    val tabs: List<String>,
    val presets: List<PickTile>,
    val mine: List<PickTile>,
    val holoVisible: Boolean,
    val initialHolo: Boolean,
    /** The slot's current source, selected when the sheet opens. */
    val initialSource: String?,
    /** 0 = `기본 이미지`, 1 = `내 이미지`. */
    val initialTab: Int,
    val current: ImageUse?,
)

object ImagePickModel {
    const val TAB_PRESETS = 0
    const val TAB_MINE = 1

    /**
     * [fileExists] hides `내 이미지` rows whose file is no longer on this device (the blob store's garbage
     * collection removes originals no image use refers to, see Requests).
     */
    fun build(doc: ProjectDoc, owner: ImageOwner, holoDefaultForNewImages: Boolean, fileExists: (String) -> Boolean = { true }): ImagePickView {
        val spec = ImageSlots.spec(owner.slot)
        val current = ImageEdits.use(doc, owner)
        val presets = PresetImages.all.map { PickTile(it.source, it.name, it, null) }
        val mine = ImageOwners.myImages(doc).filter { fileExists(it.hash) }.map { b ->
            PickTile(b.hash, if (b.width != null && b.height != null) "${b.width} × ${b.height}" else "", null, b)
        }
        val currentSource = current?.source?.takeIf { s -> presets.any { it.source == s } || mine.any { it.source == s } }
        return ImagePickView(
            title = spec.pickTitle,
            subtitle = ImageCopy.pickSubtitle(ImageOwners.name(doc, owner.kind, owner.key), spec.chips),
            tabs = listOf(ImageCopy.tab(ImageCopy.TAB_PRESETS, presets.size), ImageCopy.tab(ImageCopy.TAB_MINE, mine.size)),
            presets = presets,
            mine = mine,
            holoVisible = HoloRules.showToggle(owner.slot),
            initialHolo = HoloRules.initial(owner.slot, current, holoDefaultForNewImages),
            initialSource = currentSource,
            initialTab = if (current != null && !current.isPreset && currentSource != null) TAB_MINE else TAB_PRESETS,
            current = current,
        )
    }

    /** `고르기` with [source] selected; null when nothing is selected. */
    fun action(source: String?): PickAction? = when {
        source == null -> null
        source.startsWith(ImageUse.PRESET_PREFIX) -> PickAction.PlacePreset(source.removePrefix(ImageUse.PRESET_PREFIX))
        else -> PickAction.Crop(source)
    }
}
