package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio

/** A ratio as width : height (`3:4` = 3 / 4). */
data class RatioParts(val w: Int, val h: Int) {
    val value: Float get() = w.toFloat() / h

    /** `3:4` (mono label of the `ImageSlots` board). */
    val label: String get() = "$w:$h"
}

/** One slot of the CNV-25 table: its ratios (first = the one the crop editor opens with), holo and copy. */
data class SlotSpec(
    val slot: ImageSlot,
    val name: String,
    val usedIn: String,
    val ratios: List<Ratio>,
    val chips: List<String>,
    val holoAllowed: Boolean,
    val holoNote: String,
    /** Title of the picker sheet (`표지 이미지`; other slots: the slot name, Q-IM-05). */
    val pickTitle: String,
    /** Short word of the crop header (`표지`; other slots: the slot name, Q-IM-05). */
    val shortName: String,
    /** Owners that can hold this slot. */
    val owners: Set<ImageOwnerKind>,
)

/**
 * The slot registry (CNV-25, T-CNV-32): every image place is a slot with fixed ratios chosen with buttons,
 * and only 표지, 초상 and 장소 · 용어 · 세력 may carry holo.
 */
object ImageSlots {
    private val specs: Map<ImageSlot, SlotSpec> = listOf(
        SlotSpec(
            ImageSlot.PROJECT_COVER, ImageCopy.SLOT_PROJECT_COVER, ImageCopy.USED_PROJECT_COVER,
            listOf(Ratio.R3X4, Ratio.R1X1), listOf(ImageCopy.CHIP_CARD_3_4, ImageCopy.CHIP_LIST_1_1),
            holoAllowed = true, holoNote = ImageCopy.HOLO_CHOICE,
            pickTitle = ImageCopy.PICK_TITLE_COVER, shortName = ImageCopy.SHORT_PROJECT_COVER,
            owners = setOf(ImageOwnerKind.PROJECT),
        ),
        SlotSpec(
            ImageSlot.CHARACTER_PORTRAIT, ImageCopy.SLOT_CHARACTER_PORTRAIT, ImageCopy.USED_CHARACTER_PORTRAIT,
            listOf(Ratio.R4X5, Ratio.R1X1), listOf(ImageCopy.CHIP_CARD_4_5, ImageCopy.CHIP_AVATAR_1_1),
            holoAllowed = true, holoNote = ImageCopy.HOLO_CHOICE,
            pickTitle = ImageCopy.SLOT_CHARACTER_PORTRAIT, shortName = ImageCopy.SLOT_CHARACTER_PORTRAIT,
            owners = setOf(ImageOwnerKind.CHARACTER),
        ),
        SlotSpec(
            ImageSlot.CHARACTER_BACKGROUND, ImageCopy.SLOT_CHARACTER_BACKGROUND, ImageCopy.USED_CHARACTER_BACKGROUND,
            listOf(Ratio.R9X16, Ratio.R4X3), listOf(ImageCopy.CHIP_COVER_9_16, ImageCopy.CHIP_INNER_4_3),
            holoAllowed = false, holoNote = ImageCopy.HOLO_NONE_BEHIND_TEXT,
            pickTitle = ImageCopy.SLOT_CHARACTER_BACKGROUND, shortName = ImageCopy.SLOT_CHARACTER_BACKGROUND,
            owners = setOf(ImageOwnerKind.CHARACTER),
        ),
        SlotSpec(
            ImageSlot.CARD_REFERENCE, ImageCopy.SLOT_CARD_REFERENCE, ImageCopy.USED_CARD_REFERENCE,
            listOf(Ratio.R16X9), listOf(ImageCopy.CHIP_16_9),
            holoAllowed = true, holoNote = ImageCopy.HOLO_CHOICE,
            pickTitle = ImageCopy.USED_CARD_REFERENCE, shortName = ImageCopy.USED_CARD_REFERENCE,
            owners = setOf(ImageOwnerKind.PLACE, ImageOwnerKind.TERM, ImageOwnerKind.FACTION),
        ),
        SlotSpec(
            ImageSlot.PAGE_HEADER, ImageCopy.SLOT_PAGE_HEADER, ImageCopy.USED_PAGE_HEADER,
            listOf(Ratio.R21X9), listOf(ImageCopy.CHIP_21_9),
            holoAllowed = false, holoNote = ImageCopy.HOLO_NONE,
            pickTitle = ImageCopy.SLOT_PAGE_HEADER, shortName = ImageCopy.SLOT_PAGE_HEADER,
            owners = setOf(ImageOwnerKind.PAGE),
        ),
    ).associateBy { it.slot }

    /** Every slot in table order. */
    val all: List<SlotSpec> = ImageSlot.entries.map { specs.getValue(it) }

    fun spec(slot: ImageSlot): SlotSpec = specs.getValue(slot)

    fun ratios(slot: ImageSlot): List<Ratio> = spec(slot).ratios

    fun holoAllowed(slot: ImageSlot): Boolean = spec(slot).holoAllowed

    /** Chip label of [ratio] in [slot] (the plain ratio when the slot does not use it). */
    fun chip(slot: ImageSlot, ratio: Ratio): String {
        val s = spec(slot)
        val i = s.ratios.indexOf(ratio)
        return if (i >= 0) s.chips[i] else parts(ratio).label
    }

    fun parts(ratio: Ratio): RatioParts = when (ratio) {
        Ratio.R3X4 -> RatioParts(3, 4)
        Ratio.R1X1 -> RatioParts(1, 1)
        Ratio.R4X5 -> RatioParts(4, 5)
        Ratio.R9X16 -> RatioParts(9, 16)
        Ratio.R4X3 -> RatioParts(4, 3)
        Ratio.R16X9 -> RatioParts(16, 9)
        Ratio.R21X9 -> RatioParts(21, 9)
    }
}
