package io.github.lenticualrstone.civilight.feature.cast.list

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldFilterChip
import io.github.lenticualrstone.civilight.domain.cast.field.FieldFilters
import io.github.lenticualrstone.civilight.domain.cast.field.SortOrder
import io.github.lenticualrstone.civilight.domain.cast.list.CardGroup
import io.github.lenticualrstone.civilight.domain.cast.list.CastGroup
import io.github.lenticualrstone.civilight.domain.cast.list.CastListView
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/** A typed `숫자` range; either bound may be empty (open). */
@Serializable
data class RangeInput(val min: String = "", val max: String = "") {
    val minValue: BigDecimal? get() = min.trim().toBigDecimalOrNull()
    val maxValue: BigDecimal? get() = max.trim().toBigDecimalOrNull()
    val isOpen: Boolean get() = minValue == null && maxValue == null
}

/** The field filters and sort order of the list (CST-03). Saved with the screen as JSON. */
@Serializable
data class FieldFilterState(
    /** Field → picked options or linked keys (any of them matches). */
    val selected: Map<Key, Set<Key>> = emptyMap(),
    val ranges: Map<Key, RangeInput> = emptyMap(),
    val sortDefKey: Key? = null,
) {
    val active: Boolean get() = selected.values.any { it.isNotEmpty() } || ranges.values.any { !it.isOpen } || sortDefKey != null

    fun toggle(defKey: Key, option: Key): FieldFilterState {
        val now = selected[defKey].orEmpty()
        val next = if (option in now) now - option else now + option
        return copy(selected = (selected + (defKey to next)).filterValues { it.isNotEmpty() })
    }

    fun range(defKey: Key, input: RangeInput): FieldFilterState =
        copy(ranges = if (input.min.isBlank() && input.max.isBlank()) ranges - defKey else ranges + (defKey to input))

    /** Tapping the chosen order again goes back to the list order. */
    fun sort(defKey: Key): FieldFilterState = copy(sortDefKey = if (sortDefKey == defKey) null else defKey)
}

/** A range control of the sheet: a `숫자` field. */
data class RangeField(val defKey: Key, val label: String, val unit: String?)

/** The filter and sort sheet (CST-03; placement §6 Q13 default: a header button opens a sheet). */
data class FilterSheetView(
    val title: String,
    val chips: List<FieldFilterChip>,
    val ranges: List<RangeField>,
    val sorts: List<SortOrder>,
) {
    val isEmpty: Boolean get() = chips.isEmpty() && ranges.isEmpty() && sorts.isEmpty()
}

/** An active filter shown under the tag chips, with the state after removing it. */
data class ActiveFilter(val label: String, val without: FieldFilterState)

/**
 * Field filters and sort orders of the character list (CST-03) over `domain:cast` [FieldFilters]:
 * `고르기 · 여러 개 고르기 · 연결` fields with `필터` on, `숫자` ranges, and `<항목>순` sorting
 * (`??` and empty last). Several fields narrow together; options of one field widen.
 */
object CastFilterModel {
    const val TITLE = CastUiCopy.FILTER_TITLE
    const val SORT_TITLE = CastUiCopy.SORT_TITLE

    /** `숫자` fields that can take a range: visible, public, for every character. */
    fun rangeFields(doc: ProjectDoc): List<RangeField> = FieldDefinitions.live(doc, EntityKind.CHARACTER)
        .filter { it.type.known == FieldType.NUMBER && !it.definitionHidden && !it.isSecret && it.scopeCharacterKey == null }
        .map { RangeField(it.key, it.name, it.unit) }

    fun sheet(doc: ProjectDoc): FilterSheetView =
        FilterSheetView(TITLE, FieldFilters.chips(doc), rangeFields(doc), FieldFilters.sortOrders(doc))

    /** The state without fields that no longer exist or no longer filter. */
    fun clean(doc: ProjectDoc, state: FieldFilterState): FieldFilterState {
        val chips = FieldFilters.chips(doc).associate { c -> c.defKey to c.options.map { it.key }.toSet() }
        val ranges = rangeFields(doc).map { it.defKey }.toSet()
        val sorts = FieldFilters.sortOrders(doc).map { it.defKey }.toSet()
        return FieldFilterState(
            selected = state.selected.mapNotNull { (k, v) -> chips[k]?.let { opts -> (v intersect opts).takeIf { it.isNotEmpty() }?.let { k to it } } }.toMap(),
            ranges = state.ranges.filterKeys { it in ranges },
            sortDefKey = state.sortDefKey?.takeIf { it in sorts },
        )
    }

    /** Does [characterKey] pass every active filter at [lens]? */
    fun passes(doc: ProjectDoc, characterKey: Key, state: FieldFilterState, lens: LensPoint): Boolean {
        for ((defKey, options) in state.selected) {
            val def = doc.index.fieldDef(defKey) ?: continue
            if (!FieldFilters.matches(doc, characterKey, def, options, lens)) return false
        }
        for ((defKey, range) in state.ranges) {
            if (range.isOpen) continue
            val def = doc.index.fieldDef(defKey) ?: continue
            if (!FieldFilters.inRange(doc, characterKey, def, range.minValue, range.maxValue, lens)) return false
        }
        return true
    }

    /**
     * The list with the field filters applied. A sort order turns the groups into one section named
     * after it (`나이순`), sorted with `??` and empty last [PROPOSED layout].
     */
    fun apply(doc: ProjectDoc, view: CastListView, state: FieldFilterState, lens: LensPoint): CastListView {
        if (!state.active) return view
        val groups = view.groups.map { g -> g.copy(rows = g.rows.filter { passes(doc, it.key, state, lens) }) }.filter { it.rows.isNotEmpty() }
        val sortDef = state.sortDefKey?.let(doc.index::fieldDef)
        val sorted = if (sortDef == null) {
            groups
        } else {
            val rows = groups.flatMap { it.rows }.distinctBy { it.key }.associateBy { it.key }
            val characters = rows.keys.mapNotNull(doc.index::character)
            val order = FieldFilters.sort(doc, characters, sortDef, lens).mapNotNull { rows[it.key] }
            listOf(CastGroup(null, sortLabel(doc, state).orEmpty(), order)).filter { it.rows.isNotEmpty() }
        }
        val empty = sorted.isEmpty() && view.empty == null
        return view.copy(groups = sorted, noResults = view.noResults || empty)
    }

    /** The card rows with the same narrowing as the list (tag chip, search, field filters, sort). */
    fun cards(doc: ProjectDoc, groups: List<CardGroup>, list: CastListView, state: FieldFilterState, lens: LensPoint): List<CardGroup> {
        val allowed = list.rows.map { it.key }
        val keep = allowed.toSet()
        val sortDef = state.sortDefKey?.let(doc.index::fieldDef)
        if (sortDef != null) {
            val byKey = groups.flatMap { it.cards }.associateBy { it.key }
            val cards = allowed.mapNotNull(byKey::get)
            return listOf(CardGroup(null, CastCopy.join(sortLabel(doc, state), cards.size.toString()), cards)).filter { it.cards.isNotEmpty() }
        }
        return groups.map { g ->
            val cards = g.cards.filter { it.key in keep }
            g.copy(header = CastCopy.join(g.header.substringBefore(CastCopy.SEP), cards.size.toString()), cards = cards)
        }.filter { it.cards.isNotEmpty() }
    }

    fun sortLabel(doc: ProjectDoc, state: FieldFilterState): String? =
        state.sortDefKey?.let { k -> FieldFilters.sortOrders(doc).firstOrNull { it.defKey == k }?.label }

    /** Chips of the active filters (`소속 · 등불 결사`, `신장 · 150 ~ 170`, `나이순`); tapping one removes it. */
    fun activeFilters(doc: ProjectDoc, state: FieldFilterState): List<ActiveFilter> = buildList {
        val chips = FieldFilters.chips(doc).associateBy { it.defKey }
        for ((defKey, options) in state.selected) {
            val chip = chips[defKey] ?: continue
            for (o in chip.options.filter { it.key in options }) add(ActiveFilter(CastCopy.join(chip.label, o.label), state.toggle(defKey, o.key)))
        }
        val ranges = rangeFields(doc).associateBy { it.defKey }
        for ((defKey, range) in state.ranges) {
            val field = ranges[defKey] ?: continue
            if (range.isOpen) continue
            add(ActiveFilter(CastCopy.join(field.label, CastUiCopy.range(range.min, range.max)), state.range(defKey, RangeInput())))
        }
        sortLabel(doc, state)?.let { add(ActiveFilter(it, state.copy(sortDefKey = null))) }
    }
}

/** Saves [FieldFilterState] with the screen (`rememberSaveable` of a string). */
object FilterStateCodec {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    fun encode(state: FieldFilterState): String = json.encodeToString(FieldFilterState.serializer(), state)

    fun decode(text: String): FieldFilterState =
        if (text.isBlank()) FieldFilterState() else runCatching { json.decodeFromString(FieldFilterState.serializer(), text) }.getOrElse { FieldFilterState() }
}
