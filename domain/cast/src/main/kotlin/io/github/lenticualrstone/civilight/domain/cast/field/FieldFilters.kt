package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.entityKind
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import java.math.BigDecimal

/** A choice in a field filter: an option, or a linked record. */
data class FilterOption(val key: Key, val label: String)

/** A filter chip of a `필터` field with its options (`성별 · 종족 · 출신지`). */
data class FieldFilterChip(val defKey: Key, val label: String, val options: List<FilterOption>)

/** `신장순 · 나이순 · 생일순`. */
data class SortOrder(val defKey: Key, val label: String)

/**
 * Field filters and sort orders of the character list (CST-03). Values follow the lens. `??` and
 * empty values sort last ([PROPOSED]).
 */
object FieldFilters {
    /** Live, visible, non-secret fields whose `필터` box is on (only 고르기 · 여러 개 고르기 · 연결). */
    fun filterDefs(doc: ProjectDoc, kind: EntityKind = EntityKind.CHARACTER): List<FieldDef> =
        FieldDefinitions.live(doc, kind).filter { d ->
            d.inFilter && !d.definitionHidden && !d.isSecret && d.scopeCharacterKey == null && d.type.known?.let(FieldTypes::canFilter) == true
        }

    fun chips(doc: ProjectDoc, kind: EntityKind = EntityKind.CHARACTER): List<FieldFilterChip> =
        filterDefs(doc, kind).map { FieldFilterChip(it.key, it.name, options(doc, it)) }

    /** Options of a chip: the field's options in order, or the live records of a `연결` target kind. */
    fun options(doc: ProjectDoc, def: FieldDef): List<FilterOption> = when (def.type.known) {
        FieldType.CHOICE, FieldType.MULTI_CHOICE -> def.options.orEmpty().sortedWith(compareBy({ it.order }, { it.key })).map { FilterOption(it.key, it.label) }
        FieldType.LINK -> when (def.linkTargetKind?.known?.entityKind) {
            EntityKind.FACTION -> doc.world.factions.filter { it.deletedAt == null && !it.hidden }.sortedWith(compareBy({ it.order }, { it.key })).map { FilterOption(it.key, it.name) }
            EntityKind.PLACE -> doc.world.places.filter { it.deletedAt == null && !it.hidden }.sortedWith(compareBy({ it.order }, { it.key })).map { FilterOption(it.key, it.name) }
            EntityKind.TERM -> doc.world.terms.filter { it.deletedAt == null && !it.hidden }.sortedWith(compareBy({ it.order }, { it.key })).map { FilterOption(it.key, it.name) }
            else -> emptyList()
        }
        else -> emptyList()
    }

    /** The character's value at [lens] holds any of [selected] (option or linked keys). */
    fun matches(
        doc: ProjectDoc,
        characterKey: Key,
        def: FieldDef,
        selected: Set<Key>,
        lens: LensPoint = LensPoint.Latest,
        dates: StoryDateSource = StoryDates.Default,
    ): Boolean {
        if (selected.isEmpty()) return true
        val value = FieldReader.read(doc, EntityKind.CHARACTER, characterKey, def, ReadPoint.AtLens(lens), dates).value
        return when (val p = value?.payload) {
            is FieldPayload.Choice -> p.optionKeys.any { it in selected }
            is FieldPayload.Link -> p.targetKeys.any { it in selected }
            else -> false
        }
    }

    /** `숫자` range filter: the value lies in [min, max] (either bound open when null). */
    fun inRange(
        doc: ProjectDoc,
        characterKey: Key,
        def: FieldDef,
        min: BigDecimal?,
        max: BigDecimal?,
        lens: LensPoint = LensPoint.Latest,
        dates: StoryDateSource = StoryDates.Default,
    ): Boolean {
        val n = number(doc, characterKey, def, lens, dates) ?: return false
        return (min == null || n >= min) && (max == null || n <= max)
    }

    /** Sort orders `<항목 이름>순` for 숫자 · 날짜 · 기념일 · 계산. */
    fun sortOrders(doc: ProjectDoc, kind: EntityKind = EntityKind.CHARACTER): List<SortOrder> =
        FieldDefinitions.live(doc, kind)
            .filter { !it.definitionHidden && !it.isSecret && it.scopeCharacterKey == null && it.type.known?.let(FieldTypes::canSort) == true }
            .map { SortOrder(it.key, "${it.name}순") }

    /** Sorted ascending by the field at [lens]; `??` and empty last, then list order. */
    fun sort(
        doc: ProjectDoc,
        characters: List<Character>,
        def: FieldDef,
        lens: LensPoint = LensPoint.Latest,
        dates: StoryDateSource = StoryDates.Default,
    ): List<Character> {
        val keyed = characters.map { it to sortKey(doc, it.key, def, lens, dates) }
        return keyed.sortedWith(
            compareBy<Pair<Character, List<BigDecimal>?>> { it.second == null }
                .thenComparator { a, b -> compareLists(a.second, b.second) }
                .thenBy { it.first.order }
                .thenBy { it.first.key },
        ).map { it.first }
    }

    private fun compareLists(a: List<BigDecimal>?, b: List<BigDecimal>?): Int {
        if (a == null || b == null) return 0
        for (i in 0 until minOf(a.size, b.size)) {
            val c = a[i].compareTo(b[i])
            if (c != 0) return c
        }
        return a.size.compareTo(b.size)
    }

    /** Comparable parts: number; date (year, month, day); anniversary (month, day); null for `??` or empty. */
    private fun sortKey(doc: ProjectDoc, key: Key, def: FieldDef, lens: LensPoint, dates: StoryDateSource): List<BigDecimal>? {
        val value = FieldReader.read(doc, EntityKind.CHARACTER, key, def, ReadPoint.AtLens(lens), dates).value
        if (value.valueState != ValueState.VALUE) return null
        return when (val p = value!!.payload) {
            is FieldPayload.Number -> p.value.trim().toBigDecimalOrNull()?.let { listOf(it) }
            is FieldPayload.Date -> listOf(p.year, p.month ?: 0, p.day ?: 0).map { BigDecimal(it) }
            is FieldPayload.Anniversary -> listOf(BigDecimal(p.month), BigDecimal(p.day))
            else -> null
        }
    }

    private fun number(doc: ProjectDoc, key: Key, def: FieldDef, lens: LensPoint, dates: StoryDateSource): BigDecimal? {
        val value = FieldReader.read(doc, EntityKind.CHARACTER, key, def, ReadPoint.AtLens(lens), dates).value
        return (value?.payload as? FieldPayload.Number)?.value?.trim()?.toBigDecimalOrNull()
    }
}
