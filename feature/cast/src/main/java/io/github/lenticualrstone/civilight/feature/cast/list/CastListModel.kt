package io.github.lenticualrstone.civilight.feature.cast.list

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.cast.list.CastListView
import io.github.lenticualrstone.civilight.domain.cast.list.CardGroup
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSearch
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/**
 * What the user chose on the list screen: the tag chip (CST-02), the typed query, and whether the
 * command bar is in its typing state, the `목록 | 카드` view (CST-04) and the field filters (CST-03).
 * `목록 | 관계도` pushes the `RelationMap` route (CST-33), which has its own state (`map/MapUi`).
 */
data class CastListUi(
    val selectedTag: Key? = null,
    val query: String = "",
    val searching: Boolean = false,
    /** `목록 | 카드` (CST-04): the card view. */
    val cards: Boolean = false,
    /** Field filters and sort order (CST-03). */
    val filters: FieldFilterState = FieldFilterState(),
)

/** The list screen at one moment: the domain view plus what the screen needs around it. */
data class CastListState(
    val view: CastListView,
    val readOnly: Boolean,
    /** The tag chip in effect (a chip whose tag was removed falls back to `전체`). */
    val selectedTag: Key?,
    /** Card rows (CST-04), built in both views so the switch can animate between them; the screen draws them when `ui.cards`. */
    val cardGroups: List<CardGroup> = emptyList(),
    /** The field filters in effect (fields that are gone dropped) and their chips. */
    val filters: FieldFilterState = FieldFilterState(),
    val activeFilters: List<ActiveFilter> = emptyList(),
    /** Whether the project has any field filter or sort order to offer (the header button shows only then). */
    val canFilter: Boolean = false,
    /** Field-value hits of the typed query (CST-27): `특기 · 성직자`, the match boxed. */
    val fieldHits: List<FieldHitRow> = emptyList(),
    /** `숨긴 항목 제외` under search results while hidden characters are locked (rule 19 §6). */
    val hiddenExcluded: Boolean = false,
) {
    /** `결과 없음` only when neither a name nor a field value matched. */
    val noResults: Boolean get() = view.noResults && fieldHits.isEmpty()
}

/** One field-value hit: [location] is the character, [sentence] `<항목> · <값>` with [match] inside it. */
data class FieldHitRow(val characterKey: Key, val location: String, val sentence: String, val match: IntRange)

/** Pure builder of the list screen (CST-01, CST-02) over `domain:cast` [CastList]. */
object CastListModel {
    fun build(doc: ProjectDoc, ui: CastListUi, hiddenUnlocked: Boolean, readOnly: Boolean): CastListState {
        val tag = ui.selectedTag?.takeIf { key -> CharacterEdits.tags(doc).any { it.key == key } }
        val query = if (ui.searching) ui.query.nfc() else ""
        val lens = doc.castLens
        val filters = CastFilterModel.clean(doc, ui.filters)
        val view = CastFilterModel.apply(doc, CastList.build(doc, lens, tag, query, hiddenUnlocked), filters, lens)
        // Both views are built whatever `ui.cards` says: the `목록 ↔ 카드` switch animates between them (CNV-45).
        val cards = if (view.empty == null) CastFilterModel.cards(doc, CastList.cards(doc, lens, hiddenUnlocked), view, filters, lens) else emptyList()
        return CastListState(
            view = view,
            readOnly = readOnly,
            selectedTag = tag,
            cardGroups = cards,
            filters = filters,
            activeFilters = CastFilterModel.activeFilters(doc, filters),
            canFilter = !CastFilterModel.sheet(doc).isEmpty,
            fieldHits = fieldHits(doc, query, hiddenUnlocked),
            hiddenExcluded = query.isNotBlank() && !hiddenUnlocked && doc.cast.characters.any { it.hidden && it.deletedAt == null },
        )
    }

    /**
     * Field values hit by the query (CST-27, TC-F-19): `짧은 글 · 고르기 · 여러 개 고르기 · 연결`
     * as whole values and `긴 글` by sentence, at the lens; secret and hidden fields and locked hidden
     * characters never match (`domain:cast` `FieldSearch`). The row reads `<항목> · <값>` with the
     * match boxed inside it.
     */
    fun fieldHits(doc: ProjectDoc, query: String, hiddenUnlocked: Boolean): List<FieldHitRow> {
        if (query.isBlank()) return emptyList()
        return FieldSearch.search(doc, query, doc.castLens, hiddenUnlocked).map { hit ->
            val shift = hit.label.length - hit.text.length
            FieldHitRow(hit.characterKey, hit.characterName, hit.label, (hit.match.first + shift)..(hit.match.last + shift))
        }
    }

    /**
     * Stable lazy-list keys of the hit rows: character and sentence, with `#n` appended to the n-th repeat so
     * two equal hits (the same sentence twice in one `긴 글`) never share a key, which a lazy list rejects.
     */
    fun hitKeys(hits: List<FieldHitRow>): List<String> {
        val seen = HashMap<String, Int>()
        return hits.map { hit ->
            val base = "hit-${hit.characterKey}-${hit.sentence}"
            val n = seen.merge(base, 1, Int::plus)!!
            if (n == 1) base else "$base#$n"
        }
    }

    /** Lazy key of a list section header and of its rows: the group is part of it, a character can sit in several groups. */
    fun sectionKey(tagKey: Key?): String = "sec-$tagKey"

    fun rowKey(tagKey: Key?, characterKey: Key): String = "row-$tagKey-$characterKey"

    /** Lazy key of one card row (one per group; the sort order's single group has no tag). */
    fun cardsKey(tagKey: Key?): String = "cards-$tagKey"

    /** Tapping a chip: the tag, or `전체` (null); one chip at a time (CST-02). */
    fun selectTag(ui: CastListUi, tagKey: Key?): CastListUi = ui.copy(selectedTag = tagKey)

    /** Leaving the typing state clears the query, so the full list returns. */
    fun stopSearch(ui: CastListUi): CastListUi = ui.copy(searching = false, query = "")
}
