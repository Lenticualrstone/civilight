package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.field.StoryDateSource
import io.github.lenticualrstone.civilight.domain.cast.field.StoryDates
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** One `항목 · 값` line on a card's back. */
data class CardBackField(val label: String, val value: String)

/**
 * The back of a flipped card (부채꼴 덱, CST-67): the title as on the front, then the `한 줄 소개` (or, while a
 * display name hides the name, the front's `하르트 · 이름 공개 1-9` line), then up to three public fields (the turned deck card shows two).
 */
data class CardBack(val title: String, val line: String?, val fields: List<CardBackField>)

internal object CardBackModel {
    /** The 롤로덱스 index card shows three rows (board `rcard`); the turned 부채꼴 덱 card two ([DECK_FIELDS]). */
    const val FIELDS = 3
    const val DECK_FIELDS = 2

    /**
     * The back of [characterKey] at [lens]. Only public fields (never a secret field, so the 비밀 tab lock
     * has nothing to guard here); the list-visible ones first, then the rest in field order, empty skipped.
     * Linked values name no hidden item (`FieldText.format` with hidden items closed), as on the front.
     */
    fun of(doc: ProjectDoc, characterKey: Key, lens: LensPoint, dates: StoryDateSource = StoryDates.Default): CardBack? {
        val c = doc.index.character(characterKey)?.takeIf { it.deletedAt == null } ?: return null
        val (title, revealSub) = DisplayNames.card(doc, characterKey, lens)
        val defs = Profile.publicDefs(doc, characterKey).sortedBy { if (it.inList) 0 else 1 }
        val fields = defs.asSequence()
            .map { def -> def to FieldText.format(doc, def, Profile.reading(doc, characterKey, def, ReadPoint.AtLens(lens), dates).value) }
            .filterNot { it.second.isEmpty }
            .take(FIELDS)
            .map { (def, shown) -> CardBackField(def.name, shown.text) }
            .toList()
        val line = revealSub ?: c.oneLiner?.takeIf { it.isNotBlank() }
        return CardBack(title, line, fields)
    }
}
