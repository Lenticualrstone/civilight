package io.github.lenticualrstone.civilight.feature.canvas.image.pick

import io.github.lenticualrstone.civilight.domain.actions.GalleryActions
import io.github.lenticualrstone.civilight.domain.actions.GalleryOwner
import io.github.lenticualrstone.civilight.domain.library.newproject.CoverSummary
import io.github.lenticualrstone.civilight.feature.canvas.gallery.MyImages
import io.github.lenticualrstone.civilight.feature.canvas.image.HoloRules
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwners
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImage
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.ui.image.CoverUi
import io.github.lenticualrstone.civilight.ui.image.ProjectCovers

/**
 * One tile of the picker grid; [source] is what the slot would store (`preset:<id>` or a hash), or, for a
 * gallery tile (U-08 LIB-85), `gallery:<item key>` so it never collides with the same original under
 * `내 이미지`. [item] and [chip] (`표지 · 홀로 · 숨김`) are set on gallery tiles only.
 */
data class PickTile(
    val source: String,
    val name: String,
    val preset: PresetImage?,
    val blob: Blob?,
    val item: GalleryItem? = null,
    val chip: String? = null,
)

/** The picker's tabs. Other slots have `기본 이미지 · 내 이미지`; the cover slot adds `갤러리` first (LIB-84). */
enum class PickTab { GALLERY, PRESETS, MINE }

/** What `고르기` does with the selected tile. */
sealed interface PickAction {
    /** A preset fills the slot at once (presets are not cropped, Q-IM-03 default). */
    data class PlacePreset(val presetId: String) : PickAction

    /** An original opens the crop editor (new crop, or re-crop of the one in the slot). */
    data class Crop(val blobHash: String) : PickAction

    /** A project gallery item (U-08 LIB-85): a preset item is placed at once, an original opens the crop editor. */
    data class FromGallery(val itemKey: Key) : PickAction
}

/**
 * The cover picker's current line (U-08 LIB-84): 48 dp thumbnail and `지금 표지 · <source word>`, or
 * `표지 없음 · 첫 글자 카드`. [cropHash] is set for an original that is on this device (`자르기`);
 * [canClear] whenever there is a row, also a stale one whose original is gone (LIB-88), so it can go.
 */
data class CoverLine(val text: String, val thumb: CoverUi, val cropHash: String?, val canClear: Boolean)

/** What `고르기` on a gallery tile hands on (U-08 LIB-85). */
sealed interface GalleryPick {
    /** A preset item: placed at once with the item's holo (GAL-14 preset rule). */
    data class Preset(val presetId: String, val holo: Boolean) : GalleryPick

    /** An original: the crop editor starts from the item's crops and holo; [hiddenNote] for a hidden item. */
    data class Original(val blobHash: String, val crops: Map<Ratio, Crop>, val holo: Boolean, val hiddenNote: String?) : GalleryPick
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
    /** Index into [tabs] / [tabKinds]: for other slots 0 = `기본 이미지`, 1 = `내 이미지`. */
    val initialTab: Int,
    val current: ImageUse?,
    /** What each of [tabs] shows, in order. */
    val tabKinds: List<PickTab> = listOf(PickTab.PRESETS, PickTab.MINE),
    /** `갤러리` tiles (cover slot only). */
    val gallery: List<PickTile> = emptyList(),
    /** The current-cover line (cover slot only). */
    val coverLine: CoverLine? = null,
) {
    /** The tiles of tab [index]. */
    fun tilesOf(index: Int): List<PickTile> = when (tabKinds.getOrNull(index)) {
        PickTab.GALLERY -> gallery
        PickTab.PRESETS -> presets
        PickTab.MINE, null -> mine
    }

    /** The empty line of tab [index]. */
    fun emptyOf(index: Int): String = if (tabKinds.getOrNull(index) == PickTab.GALLERY) ImageCopy.GALLERY_EMPTY else ImageCopy.MY_IMAGES_EMPTY
}

object ImagePickModel {
    const val TAB_PRESETS = 0
    const val TAB_MINE = 1

    /** Prefix of a gallery tile's [PickTile.source]. */
    const val GALLERY_PREFIX = "gallery:"

    /** The project cover slot: the one that gets the cover mode (U-08 LIB-84). */
    fun isCover(owner: ImageOwner): Boolean = owner.kind == ImageOwnerKind.PROJECT && owner.slot == ImageSlot.PROJECT_COVER

    /**
     * [locked] = the project's hidden items are locked: `내 이미지` then leaves out originals only hidden things use
     * (55 GAL-19), and the cover's `갤러리` tab leaves out hidden items, uncounted. [fileExists] hides rows whose
     * file is no longer on this device (the blob store's garbage collection removes originals no image use refers to).
     */
    fun build(
        doc: ProjectDoc,
        owner: ImageOwner,
        holoDefaultForNewImages: Boolean,
        fileExists: (String) -> Boolean = { true },
        locked: Boolean = false,
    ): ImagePickView {
        val spec = ImageSlots.spec(owner.slot)
        val current = ImageEdits.use(doc, owner)
        val presets = PresetImages.all.map { PickTile(it.source, it.name, it, null) }
        val mine = MyImages.visible(doc, locked).filter { fileExists(it.hash) }.map { b ->
            PickTile(b.hash, if (b.width != null && b.height != null) "${b.width} × ${b.height}" else "", null, b)
        }
        val currentSource = current?.source?.takeIf { s -> presets.any { it.source == s } || mine.any { it.source == s } }
        val base = ImagePickView(
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
        return if (isCover(owner)) coverMode(doc, base, fileExists, locked) else base
    }

    /**
     * The cover mode (U-08 LIB-84 … LIB-86): tabs `갤러리 <N> · 기본 이미지 8 · 내 이미지 <M>`, the current line,
     * and the first tab: the gallery tile showing the cover, else the preset tab for a preset, `내 이미지` for
     * another original, and with no cover `갤러리` when it has tiles, else `기본 이미지`.
     */
    private fun coverMode(doc: ProjectDoc, base: ImagePickView, fileExists: (String) -> Boolean, locked: Boolean): ImagePickView {
        val current = base.current
        val items = GalleryActions.visibleItems(doc, GalleryOwner.project(doc), locked)
            .filter { item -> item.isPreset || item.source.let(fileExists) }
        val gallery = items.map { item ->
            val chip = listOfNotNull(
                ImageCopy.CHIP_COVER.takeIf { current != null && current.source == item.source },
                ImageCopy.CHIP_HOLO.takeIf { item.holo },
                ImageCopy.CHIP_HIDDEN.takeIf { item.hidden },
            ).joinToString(" · ").ifEmpty { null }
            val preset = if (item.isPreset) PresetImages.ofSource(item.source) else null
            val blob = if (item.isPreset) null else doc.assets.blobs.firstOrNull { it.hash == item.source }
            PickTile(GALLERY_PREFIX + item.key, item.title.ifBlank { ImageCopy.GALLERY_UNTITLED }, preset, blob, item, chip)
        }
        val galleryTile = current?.let { c -> gallery.firstOrNull { it.item?.source == c.source } }
        val kinds = listOf(PickTab.GALLERY, PickTab.PRESETS, PickTab.MINE)
        val initialKind = when {
            galleryTile != null -> PickTab.GALLERY
            current == null -> if (gallery.isNotEmpty()) PickTab.GALLERY else PickTab.PRESETS
            current.isPreset -> PickTab.PRESETS
            base.initialSource != null -> PickTab.MINE
            else -> PickTab.PRESETS
        }
        return base.copy(
            tabs = listOf(ImageCopy.tab(ImageCopy.TAB_GALLERY, gallery.size)) + base.tabs,
            tabKinds = kinds,
            gallery = gallery,
            initialTab = kinds.indexOf(initialKind),
            initialSource = galleryTile?.source ?: base.initialSource,
            coverLine = coverLine(doc, current, fileExists),
        )
    }

    /** `지금 표지 · <source word>` with `자르기` (originals) and `빼기`, or `표지 없음 · 첫 글자 카드` (LIB-84, LIB-88). */
    fun coverLine(doc: ProjectDoc, current: ImageUse?, fileExists: (String) -> Boolean = { true }): CoverLine {
        val thumb = ProjectCovers.thumb(doc, doc.project.name, fileExists)
        if (current == null) return CoverLine(ImageCopy.COVER_NONE, thumb, cropHash = null, canClear = false)
        val word = CoverSummary.wordOf(doc, current, fileExists)
            ?: return CoverLine(ImageCopy.COVER_NONE, thumb, cropHash = null, canClear = true)
        return CoverLine(ImageCopy.coverNow(word), thumb, cropHash = current.blobHash, canClear = true)
    }

    /** `고르기` with [source] selected; null when nothing is selected. */
    fun action(source: String?): PickAction? = when {
        source == null -> null
        source.startsWith(GALLERY_PREFIX) -> PickAction.FromGallery(source.removePrefix(GALLERY_PREFIX))
        source.startsWith(ImageUse.PRESET_PREFIX) -> PickAction.PlacePreset(source.removePrefix(ImageUse.PRESET_PREFIX))
        else -> PickAction.Crop(source)
    }

    /** What `고르기` on gallery item [item] does (U-08 LIB-85); nothing is copied, the cover gets its own crops. */
    fun galleryPick(item: GalleryItem): GalleryPick {
        if (item.isPreset) return GalleryPick.Preset(item.source.removePrefix(ImageUse.PRESET_PREFIX), item.holo)
        val crops = listOf(Ratio.R3X4, Ratio.R1X1).mapNotNull { r -> item.cropFor(r)?.let { r to it } }.toMap()
        return GalleryPick.Original(item.source, crops, item.holo, ImageCopy.HIDDEN_SHOWN_IN_COVER.takeIf { item.hidden })
    }
}
