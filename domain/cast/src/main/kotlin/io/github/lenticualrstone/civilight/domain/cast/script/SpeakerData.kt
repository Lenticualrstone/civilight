package io.github.lenticualrstone.civilight.domain.cast.script

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.field.FieldFilters
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.relation.PanelRelationRow
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationPanel
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.text.docText

/** A `필터` field chip of the speaker picker (CST-37): `소속 · 등불 결사`. */
data class PickerFieldChip(val defKey: Key, val optionKey: Key, val label: String)

/**
 * What the script's `참고 · 속성` panel shows for a selected block (CST-38; `CSheetRef`, `WideRef`):
 * the speaker's state just before the block, plus that block's own changes as `여기서`.
 */
data class SpeakerPanel(
    /** `화자 · 리엔 · 17세 · 등불 결사` (the first two `머리` values before the block, TC-F-26). */
    val headLine: String,
    /** `말투` in one line: the first sentence of the 말투 prose, or null when it is empty (§6 Q9). */
    val speech: String?,
    /** `??? (1-9 공개)` while a display name stands in for the name; null otherwise. */
    val displayName: String?,
    /** `리엔을 | ‘꼬마’`: the speaker's 호칭 toward partners before the block. */
    val addresses: List<Pair<String, String>>,
    /** `관계 · 지금` rows from the speaker's side (`→ 보호`, `← 경계` on the smaller line, `◆ 여기서`). */
    val relations: List<PanelRelationRow>,
    /** `숨긴 인물과의 관계 1` while hidden partners are locked (rule 19 §6). */
    val hiddenRelations: String?,
)

/**
 * The cast data the script area draws (CST-37 speaker picker, CST-38 panel). The picker's search,
 * tag chips, groups, deleted top row and hidden bottom row are `core:ui` `PickerModels`; this adds
 * the `필터` field chips. The panel combines `Profile.panelLine`, `DisplayNames.referenceValue` and
 * `RelationPanel` at one block.
 */
object SpeakerData {
    /** `소속 · 등불 결사` for every option of every `필터` field (hidden, secret and `이 인물만` fields excluded). */
    fun fieldChips(doc: ProjectDoc): List<PickerFieldChip> = FieldFilters.chips(doc).flatMap { chip ->
        chip.options.map { PickerFieldChip(chip.defKey, it.key, CastCopy.join(chip.label, it.label)) }
    }

    /** Characters of [keys] whose value at [lens] holds the chip's option (order kept). */
    fun filter(doc: ProjectDoc, keys: List<Key>, chip: PickerFieldChip, lens: LensPoint = LensPoint.Latest): List<Key> {
        val def = doc.index.fieldDef(chip.defKey) ?: return keys
        return keys.filter { FieldFilters.matches(doc, it, def, setOf(chip.optionKey), lens) }
    }

    /**
     * The panel of [speakerKey] at [blockKey]; null when the speaker is not a live, visible
     * character (a deleted speaker's panel is the script's `지운 인물 · 노아` row, rule 20).
     */
    fun panel(doc: ProjectDoc, speakerKey: Key, blockKey: Key, hiddenUnlocked: Boolean = false): SpeakerPanel? {
        if (ItemRefs.character(doc, speakerKey, hiddenUnlocked) !is ResolvedItem.Live) return null
        val c = doc.index.character(speakerKey) ?: return null
        val point = ReadPoint.BeforeBlock(blockKey)
        val name = DisplayNames.read(doc, speakerKey, point)
        val hidden = RelationViews.hiddenPartnerCount(doc, speakerKey, hiddenUnlocked)
        return SpeakerPanel(
            headLine = Profile.panelLine(doc, speakerKey, blockKey),
            speech = c.speechBody.docText.trim().takeIf { it.isNotEmpty() }?.let(CharacterEdits::firstSentence),
            displayName = if (name.override != null) DisplayNames.referenceValue(doc, speakerKey, point) else null,
            addresses = RelationPanel.addressRows(doc, speakerKey, blockKey, hiddenUnlocked),
            relations = RelationPanel.rows(doc, speakerKey, blockKey, hiddenUnlocked),
            hiddenRelations = hidden.takeIf { it > 0 }?.let(RelationViews::hiddenRow),
        )
    }
}
