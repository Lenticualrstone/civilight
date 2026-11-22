package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.feature.cast.field.FieldDraft
import io.github.lenticualrstone.civilight.feature.cast.field.FieldDrafts
import io.github.lenticualrstone.civilight.feature.cast.field.LongTextEdit
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The field sheet's inputs by type (CST-09 three states, CST-20 nine types). */
class FieldDraftsTest {
    private val doc = CastFx.doc()
    private val ctx = CastFx.ctx()

    private fun def(key: Key): FieldDef = doc.index.fieldDef(key)!!
    private fun valueOf(defKey: Key): FieldValue? = doc.index.character(K.RIEN)!!.fields[defKey]

    @Test
    fun `CST-09 an untouched draft keeps the stored value`() {
        for (key in listOf(K.FIELD_AFFILIATION, K.FIELD_HEIGHT, K.FIELD_SPECIES, K.FIELD_BIRTH, K.FIELD_PERSONALITY, K.FIELD_SPECIALTY, K.FIELD_ORIGIN)) {
            val value = valueOf(key) ?: continue
            assertSame(key, value, FieldDrafts.toValue(def(key), FieldDrafts.from(def(key), value), value, ctx))
        }
    }

    @Test
    fun `CST-09 three states - empty is never stored, unknown is ??, a value is a payload`() {
        val height = def(K.FIELD_HEIGHT)
        assertNull(FieldDrafts.toValue(height, FieldDraft(number = "  "), valueOf(K.FIELD_HEIGHT), ctx))
        assertEquals(FieldValue.UNKNOWN, FieldDrafts.toValue(height, FieldDraft(unknown = true), valueOf(K.FIELD_HEIGHT), ctx))
        assertEquals(FieldValue.of(FieldPayload.Number("160")), FieldDrafts.toValue(height, FieldDraft(number = "160"), valueOf(K.FIELD_HEIGHT), ctx))
        assertTrue(FieldDrafts.from(height, FieldValue.UNKNOWN).unknown)
        assertEquals(FieldDraft(), FieldDrafts.from(height, null))
    }

    @Test
    fun `CST-20 numbers and dates must read as numbers`() {
        val height = def(K.FIELD_HEIGHT)
        assertTrue(FieldDrafts.isValid(height, FieldDraft(number = "154.5")))
        assertFalse(FieldDrafts.isValid(height, FieldDraft(number = "1.2.3")))
        val birth = def(K.FIELD_BIRTH)
        assertTrue(FieldDrafts.isValid(birth, FieldDraft(year = "1183")))
        assertFalse(FieldDrafts.isValid(birth, FieldDraft(year = "천백")))
        assertEquals(FieldValue.of(FieldPayload.Date(1183)), FieldDrafts.toValue(birth, FieldDraft(year = "1183"), null, ctx))
    }

    @Test
    fun `CST-20 기념일 needs a month of the project's year and a day`() {
        val birthday = def(K.FIELD_BIRTHDAY)
        val months = FieldDrafts.monthCount(doc)
        assertTrue(FieldDrafts.isValid(birthday, FieldDraft(month = "3", day = "14"), months))
        assertFalse(FieldDrafts.isValid(birthday, FieldDraft(month = "${months + 1}", day = "1"), months))
        assertFalse(FieldDrafts.isValid(birthday, FieldDraft(month = "3"), months))
        assertTrue("both blank = empty", FieldDrafts.isValid(birthday, FieldDraft(), months))
        assertEquals(FieldValue.of(FieldPayload.Anniversary(3, 14)), FieldDrafts.toValue(birthday, FieldDraft(month = "3", day = "14"), null, ctx))
    }

    @Test
    fun `CST-20 고르기 keeps one option and 여러 개 고르기 keeps several in option order`() {
        val species = def(K.FIELD_SPECIES)
        val (a, b) = species.options!!.sortedBy { it.order }.take(2).map { it.key }
        val one = FieldDrafts.toggleOption(species, FieldDrafts.toggleOption(species, FieldDraft(), a), b)
        assertEquals(listOf(b), one.optionKeys)
        val specialty = def(K.FIELD_SPECIALTY)
        val opts = specialty.options!!.sortedBy { it.order }.map { it.key }
        val many = FieldDrafts.toggleOption(specialty, FieldDrafts.toggleOption(specialty, FieldDraft(), opts[1]), opts[0])
        assertEquals(setOf(opts[0], opts[1]), many.optionKeys.toSet())
        assertEquals(FieldPayload.Choice(listOf(opts[0], opts[1])), FieldDrafts.toValue(specialty, many, null, ctx)!!.payload)
        assertEquals(listOf(opts[0]), FieldDrafts.toggleOption(specialty, many, opts[1]).optionKeys)
    }

    @Test
    fun `CST-20 짧은 글 keeps one line unless 여러 개 is on`() {
        val affiliation = def(K.FIELD_AFFILIATION)
        val single = FieldDrafts.toValue(affiliation.copy(shortTextMultiple = false), FieldDraft(texts = listOf(" 등불 결사 ", "남는 줄")), null, ctx)
        assertEquals(FieldPayload.Texts(listOf("등불 결사")), single!!.payload)
        val multiple = FieldDrafts.toValue(affiliation.copy(shortTextMultiple = true), FieldDraft(texts = listOf("하나", "", "둘")), null, ctx)
        assertEquals(FieldPayload.Texts(listOf("하나", "둘")), multiple!!.payload)
    }

    @Test
    fun `CST-20 연결 keeps each target once`() {
        val origin = def(K.FIELD_ORIGIN)
        val value = FieldDrafts.toValue(origin, FieldDraft(linkKeys = listOf("a", "b", "a")), null, ctx)
        assertEquals(FieldPayload.Link(listOf("a", "b")), value!!.payload)
    }

    @Test
    fun `긴 글 with links is not edited as plain text and unchanged paragraphs keep their keys`() {
        val link = TextRun.Link("lnk", LinkTarget(LinkTargetKind.CHARACTER, K.SERA), "세라")
        assertFalse(LongTextEdit.editable(listOf(DocBlock("p1", "a", runs = listOf(TextRun.Plain("앞 "), link)))))
        val before = listOf(DocBlock("p1", "a", runs = listOf(TextRun.Plain("첫 문단"))), DocBlock("p2", "b", runs = listOf(TextRun.Plain("둘째 문단"))))
        assertTrue(LongTextEdit.editable(before))
        val after = LongTextEdit.apply(before, "첫 문단\n\n새 문단\n둘째 문단", ctx)
        assertEquals(3, after.size)
        assertEquals("p1", after[0].key)
        assertEquals("p2", after[2].key)
        assertEquals(after.sortedBy { it.order }.map { it.key }, after.map { it.key })
        assertTrue(LongTextEdit.apply(before, "  \n ", ctx).isEmpty())
    }

    @Test
    fun `CST-09 a 긴 글 from the rich editor keeps its @ link, unknown wins, empty is no value`() {
        val link = TextRun.Link("l1", LinkTarget(LinkTargetKind.CHARACTER, K.SERA), "세라")
        val rich = listOf(DocBlock(key = "p1", order = "a0", runs = listOf(TextRun.Plain("선배 "), link)))
        val value = FieldDrafts.richValue(FieldDraft(), rich)!!
        assertEquals(rich, (value.payload as FieldPayload.Rich).doc)
        assertEquals(FieldValue.UNKNOWN, FieldDrafts.richValue(FieldDraft(unknown = true), rich))
        assertNull(FieldDrafts.richValue(FieldDraft(), emptyList()))
    }
}
