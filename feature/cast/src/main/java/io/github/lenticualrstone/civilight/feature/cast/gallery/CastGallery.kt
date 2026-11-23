package io.github.lenticualrstone.civilight.feature.cast.gallery

import io.github.lenticualrstone.civilight.domain.actions.GalleryActions
import io.github.lenticualrstone.civilight.domain.actions.GalleryOwner
import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.domain.library.hiding.HideableKind
import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCardRules

/** One small card of the profile's gallery row (GAL-01): the shared card anatomy without `⋯`. */
data class GalleryRowCard(
    val itemKey: Key,
    val title: String?,
    /** Scene code, else the tag names. */
    val meta: String?,
    /** `초상 · 홀로` (the shared chip rule). */
    val chip: String?,
    val image: SlotImage,
)

/** The `갤러리 · 8` section of `개요` (GAL-01). [cards] is empty exactly when [count] is 0. */
data class GallerySection(
    val header: String,
    val count: Int,
    val cards: List<GalleryRowCard>,
) {
    val empty: Boolean get() = count == 0

    /** `전체 보기 ›` only while there is something to see (an empty gallery has none). */
    val showAll: Boolean get() = count > 0
}

/**
 * Pure reading of a character's gallery for the cast screens (GAL-01 profile section, GAL-02 card
 * chip, 55 §3.4 tag counts). Reads only: adding, editing and ordering are `GalleryActions` called by
 * the gallery screen. N is always the live items that are visible now: hidden ones are left out
 * while hidden items are locked (and so is a hidden character's whole gallery).
 */
object CastGallery {
    /** At most this many cards in the profile row `[PROPOSED]`; the rest is reached with `전체 보기 ›`. */
    const val MAX_CARDS = 12

    fun visible(doc: ProjectDoc, characterKey: Key, hiddenUnlocked: Boolean): List<GalleryItem> =
        GalleryActions.visibleItems(doc, GalleryOwner.character(characterKey), locked = !hiddenUnlocked)

    /** `갤러리 <N>` of a card's chip (GAL-02), 0 = no chip. */
    fun count(doc: ProjectDoc, characterKey: Key, hiddenUnlocked: Boolean): Int = visible(doc, characterKey, hiddenUnlocked).size

    /** The chip counts of every character in [keys] that has any; the card view reads this once. */
    fun counts(doc: ProjectDoc, keys: Collection<Key>, hiddenUnlocked: Boolean): Map<Key, Int> {
        val wanted = keys.toSet()
        val surfaces = ItemSurfaces(doc)
        return doc.galleryItems
            .filter { it.ownerKind.known == GalleryOwnerKind.CHARACTER && it.ownerKey in wanted && surfaces.isGalleryItemVisible(it, !hiddenUnlocked) }
            .groupingBy { it.ownerKey }
            .eachCount()
    }

    fun section(doc: ProjectDoc, characterKey: Key, hiddenUnlocked: Boolean): GallerySection {
        val items = visible(doc, characterKey, hiddenUnlocked)
        val codes = UnitCodes(doc)
        val surfaces = ItemSurfaces(doc)
        val portrait = useSource(doc, characterKey, ImageSlot.CHARACTER_PORTRAIT)
        val background = useSource(doc, characterKey, ImageSlot.CHARACTER_BACKGROUND)
        val cards = items.take(MAX_CARDS).map { item ->
            GalleryRowCard(
                itemKey = item.key,
                title = GalleryCardRules.shownTitle(item.title),
                meta = meta(doc, codes, surfaces, item, hiddenUnlocked),
                chip = GalleryCardRules.chip(
                    portrait = portrait != null && item.source == portrait,
                    background = background != null && item.source == background,
                    cover = false,
                    holo = item.holo,
                ),
                image = SlotImage(item.presetId, item.blobHash, item.cropFor(Ratio.R3X4), item.holo, Ratio.R3X4),
            )
        }
        return GallerySection(CastUiCopy.gallerySection(items.size), items.size, cards)
    }

    /** The meta line of a card: the scene code, else the tag names (GAL-07). */
    private fun meta(doc: ProjectDoc, codes: UnitCodes, surfaces: ItemSurfaces, item: GalleryItem, hiddenUnlocked: Boolean): String? {
        // A scene hidden while locked is never named here (rule 19 §6), and a trashed scene has no live code.
        val sceneShown = item.sceneKey?.takeIf { scene ->
            doc.liveness.isUnitLive(scene) && (hiddenUnlocked || !surfaces.hidden.isHidden(HideableKind.SCENE, scene))
        }
        sceneShown?.let { scene -> codes.sceneLabel(scene)?.let { return it } }
        val tags = item.tagKeys.mapNotNull { io.github.lenticualrstone.civilight.domain.cast.CharacterEdits.liveTag(doc, it)?.name }
        return tags.joinToString(CastUiCopy.SEP).ifEmpty { null }
    }

    /** The source a live image use of the character's [slot] holds (the main image is derived from it, T-GAL-07). */
    private fun useSource(doc: ProjectDoc, characterKey: Key, slot: ImageSlot): String? =
        doc.index.imageUse(ImageOwnerKind.CHARACTER, characterKey, slot)?.takeIf { doc.liveness.isLive(it) }?.source
}
