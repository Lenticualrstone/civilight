package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldScope
import io.github.lenticualrstone.civilight.feature.cast.settings.AddFieldDraft
import io.github.lenticualrstone.civilight.feature.cast.settings.AddFieldModel
import io.github.lenticualrstone.civilight.feature.cast.settings.FIELD_KINDS
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldSettingsModel
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `프로젝트 설정 › 인물 항목` (CST-19) and `항목 넣기` (CST-20, CST-21, CST-22). */
class FieldSettingsTest {
    private val doc = CastFx.doc()
    private val ctx = CastFx.ctx()

    @Test
    fun `CST-19 the screen has the kind tabs, the fixed box and the project rows in order`() {
        val screen = FieldSettingsModel.screen(doc, EntityKind.CHARACTER)
        assertEquals("인물 항목", screen.title)
        assertEquals("프로젝트 설정", screen.head)
        assertEquals(listOf("인물", "장소", "용어", "세력"), screen.tabs)
        assertEquals(listOf(EntityKind.CHARACTER, EntityKind.PLACE, EntityKind.TERM, EntityKind.FACTION), FIELD_KINDS)
        assertEquals("고정 항목 · 7", screen.fixedTitle)
        assertEquals(listOf("이름", "표시 이름", "한 줄 소개", "이미지", "태그", "첫 등장", "보이스 진행"), screen.fixedFields)
        assertEquals("끌어서 순서", screen.reorderNote)
        assertEquals("출생", screen.rows.first().name)
        // Fixture conflict #13: 출생 is not a `머리` field in the example (the board draws `· 머리`).
        assertEquals("날짜 · 연도", screen.rows.first().sub)
        assertEquals("프로젝트 항목 · ${screen.rows.size}", screen.projectTitle)
    }

    @Test
    fun `CST-19 the grip moves a row and the header order follows`() {
        assertEquals(listOf("b", "c", "a"), FieldSettingsModel.reorder(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("c", "a", "b"), FieldSettingsModel.reorder(listOf("a", "b", "c"), 2, -5))
        assertEquals(listOf("a", "b"), FieldSettingsModel.reorder(listOf("a", "b"), 5, 0))
        assertEquals(2, FieldSettingsModel.dropIndex(0, 110f, 56f, 5))
        assertEquals(0, FieldSettingsModel.dropIndex(1, -300f, 56f, 5))
        assertEquals(1, FieldSettingsModel.dropIndex(1, 20f, 0f, 5))

        val keys = FieldSettingsModel.screen(doc, EntityKind.CHARACTER).rows.map { it.key }
        val moved = FieldSettingsModel.move(doc, EntityKind.CHARACTER, keys[0], 0, 3, ctx)
        val after = FieldSettingsModel.screen(moved, EntityKind.CHARACTER).rows.map { it.key }
        assertEquals(FieldSettingsModel.reorder(keys, 0, 3), after)
        assertSame(doc, FieldSettingsModel.move(doc, EntityKind.CHARACTER, keys[1], 0, 3, ctx))
        assertSame(doc, FieldSettingsModel.move(doc, EntityKind.CHARACTER, keys[0], 0, 0, ctx))
    }

    @Test
    fun `TC-F-20 필터 only for 고르기, 여러 개 고르기 and 연결`() {
        val allowed = AddFieldModel.TYPES.filter(AddFieldModel::filterEnabled)
        assertEquals(listOf(FieldType.CHOICE, FieldType.MULTI_CHOICE, FieldType.LINK), allowed)
        assertEquals(9, AddFieldModel.TYPES.size)
        val draft = AddFieldModel.withType(doc, EntityKind.CHARACTER, AddFieldDraft(type = FieldType.CHOICE, inFilter = true), FieldType.NUMBER)
        assertFalse(draft.inFilter)
    }

    @Test
    fun `TC-13-16 연결 targets are exactly 세력, 장소 and 용어`() {
        assertEquals(listOf(FieldLinkTargetKind.FACTION, FieldLinkTargetKind.PLACE, FieldLinkTargetKind.TERM), AddFieldModel.LINK_TARGETS)
    }

    @Test
    fun `CST-21 넣기 needs a name, a 계산 needs a source, a same name shows the notice`() {
        assertFalse(AddFieldModel.check(doc, AddFieldDraft(name = "  "), EntityKind.CHARACTER).canSave)
        val fresh = AddFieldModel.check(doc, AddFieldDraft(name = "좋아하는 것"), EntityKind.CHARACTER)
        assertTrue(fresh.canSave)
        assertNull(fresh.sameNameNotice)
        assertEquals("◇ 같은 이름 항목 있음", AddFieldModel.check(doc, AddFieldDraft(name = "신장"), EntityKind.CHARACTER).sameNameNotice)

        val calc = AddFieldModel.withType(doc, EntityKind.CHARACTER, AddFieldDraft(name = "나이 2"), FieldType.CALC)
        assertEquals(K.FIELD_BIRTH, calc.calcSourceKey)
        assertTrue(AddFieldModel.check(doc, calc, EntityKind.CHARACTER).canSave)
        assertFalse(AddFieldModel.check(doc, calc.copy(calcSourceKey = null), EntityKind.CHARACTER).canSave)
        assertFalse("places have no date field", AddFieldModel.check(doc, calc.copy(calcSourceKey = null), EntityKind.PLACE).canSave)
    }

    @Test
    fun `CST-21 a field from settings is added for every character with its places and options`() {
        val draft = AddFieldDraft(name = "혈액형", type = FieldType.CHOICE, options = listOf("A", " ", "B"), inList = true, inFilter = true, exportName = "blood")
        val next = AddFieldModel.add(doc, draft, EntityKind.CHARACTER, null, ctx)
        val def = next.project.fieldDefs.single { it.name == "혈액형" }
        assertEquals(listOf("A", "B"), def.options!!.map { it.label })
        assertTrue(def.inList && def.inFilter && !def.inHeader)
        assertEquals("blood", def.exportName)
        assertNull(def.scopeCharacterKey)
        assertEquals("혈액형", FieldSettingsModel.screen(next, EntityKind.CHARACTER).rows.last().name)
        assertSame(doc, AddFieldModel.add(doc, AddFieldDraft(name = ""), EntityKind.CHARACTER, null, ctx))
    }

    @Test
    fun `TC-F-14 from a record, 이 인물만 scopes the field and 모든 인물에 does not`() {
        val only = AddFieldModel.add(doc, AddFieldDraft(name = "좋아하는 것", scope = FieldScope.THIS_CHARACTER), EntityKind.CHARACTER, K.RIEN, ctx)
        assertEquals(K.RIEN, only.project.fieldDefs.single { it.name == "좋아하는 것" }.scopeCharacterKey)
        val all = AddFieldModel.add(doc, AddFieldDraft(name = "좋아하는 것"), EntityKind.CHARACTER, K.RIEN, ctx)
        assertNull(all.project.fieldDefs.single { it.name == "좋아하는 것" }.scopeCharacterKey)
    }

    @Test
    fun `CST-18 비밀 항목 only exists for characters`() {
        assertTrue(AddFieldModel.toNewField(AddFieldDraft(name = "x", secret = true), EntityKind.CHARACTER, null).isSecret)
        assertFalse(AddFieldModel.toNewField(AddFieldDraft(name = "x", secret = true), EntityKind.PLACE, null).isSecret)
    }
}
