package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldFilters
import io.github.lenticualrstone.civilight.feature.cast.list.CastFilterModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListUi
import io.github.lenticualrstone.civilight.feature.cast.list.FieldFilterState
import io.github.lenticualrstone.civilight.feature.cast.list.FilterStateCodec
import io.github.lenticualrstone.civilight.feature.cast.list.RangeInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Field filters, ranges and `<항목>순` (CST-03) and the card rows (CST-04). */
class CastFilterModelTest {
    private val doc = CastFx.doc()

    private fun build(ui: CastListUi) = CastListModel.build(doc, ui, hiddenUnlocked = false, readOnly = false)

    @Test
    fun `CST-03 the sheet offers filter fields, number ranges and sort orders`() {
        val sheet = CastFilterModel.sheet(doc)
        assertEquals("항목 필터 · 정렬", sheet.title)
        assertEquals(FieldFilters.chips(doc).map { it.label }, sheet.chips.map { it.label })
        assertTrue(sheet.ranges.any { it.defKey == K.FIELD_HEIGHT && it.unit == "cm" })
        assertTrue(sheet.sorts.any { it.label == "신장순" })
        assertTrue(build(CastListUi()).canFilter)
    }

    @Test
    fun `CST-03 picking an option keeps the characters holding it`() {
        val chip = FieldFilters.chips(doc).first { it.defKey == K.FIELD_GENDER }
        val female = chip.options.first { it.label == "여" }
        val state = FieldFilterState().toggle(K.FIELD_GENDER, female.key)
        val list = build(CastListUi(filters = state))
        assertEquals(setOf(K.RIEN, K.SERA, K.MARTHA, K.IRENA), list.view.rows.map { it.key }.toSet())
        assertEquals(listOf("성별 · 여"), list.activeFilters.map { it.label })
        // Removing the chip gives the whole list back.
        assertFalse(list.activeFilters.single().without.active)
    }

    @Test
    fun `CST-03 a 숫자 range and 신장순 with empty values last`() {
        val ranged = build(CastListUi(filters = FieldFilterState().range(K.FIELD_HEIGHT, RangeInput("150", "160"))))
        val keys = ranged.view.rows.map { it.key }
        assertTrue(K.RIEN in keys)
        assertFalse(K.HART in keys)

        val sorted = build(CastListUi(filters = FieldFilterState().sort(K.FIELD_HEIGHT)))
        val group = sorted.view.groups.single()
        assertEquals("신장순", group.title)
        val order = group.rows.map { it.key }
        assertTrue(order.indexOf(K.RIEN) < order.indexOf(K.HART))
        assertEquals(listOf("신장순"), sorted.activeFilters.map { it.label })
        // Tapping the chosen order again goes back to the list order.
        assertFalse(FieldFilterState().sort(K.FIELD_HEIGHT).sort(K.FIELD_HEIGHT).active)
    }

    @Test
    fun `filters of a field that no longer filters are dropped`() {
        val stale = FieldFilterState(selected = mapOf("gone" to setOf("x")), sortDefKey = "gone")
        assertFalse(CastFilterModel.clean(doc, stale).active)
        assertEquals(stale, FilterStateCodec.decode(FilterStateCodec.encode(stale)))
        assertEquals(FieldFilterState(), FilterStateCodec.decode("not json"))
    }

    @Test
    fun `CST-04 card rows per tag with counts, narrowed like the list`() {
        val cards = build(CastListUi(cards = true)).cardGroups
        val lead = cards.first()
        assertEquals("주연 · 3", lead.header)
        assertEquals(listOf("리엔", "???", "세라"), lead.cards.map { it.title }.take(3).let { titles -> titles })
        val hart = lead.cards.single { it.key == K.HART }
        assertEquals("하르트 · 이름 공개 1-9", hart.subtitle)
        // The list view has no card rows.
        assertTrue(build(CastListUi()).cardGroups.isEmpty())
        // A search narrows the cards too.
        val found = build(CastListUi(cards = true, query = "리엔", searching = true)).cardGroups
        assertEquals(listOf(K.RIEN), found.flatMap { it.cards }.map { it.key })
        assertEquals("주연 · 1", found.single().header)
    }
}
