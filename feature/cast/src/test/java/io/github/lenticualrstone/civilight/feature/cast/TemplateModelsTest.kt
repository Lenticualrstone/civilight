package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.domain.cast.field.NewField
import io.github.lenticualrstone.civilight.feature.cast.settings.BundleModel
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldDefEditModel
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldOrderCopy
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldSettingsModel
import io.github.lenticualrstone.civilight.feature.cast.templates.SaveTarget
import io.github.lenticualrstone.civilight.feature.cast.templates.SaveTemplateModel
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateCopy
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateListModel
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateStart
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.TemplateField
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier
import java.time.ZoneId

/** U-09 in feature:cast (30-cast.md §7): order mode, delete lines, template save, list, apply, copy. */
class TemplateModelsTest {
    private val doc = CastFx.doc()
    private val ctx = CastFx.ctx()

    private fun tf(key: String, name: String, kind: EntityKind = EntityKind.CHARACTER) =
        TemplateField(key = key, origin = "tpl:$key", entityKind = kind.open(), name = name, type = FieldType.SHORT_TEXT.open(), order = "a$key")

    private fun basicFive(): ProjectDoc {
        var d = ProjectDoc(Project(key = "P5", name = "P5"))
        for (n in listOf("출생", "나이", "생일", "소속", "성격")) d = FieldDefinitions.add(d, NewField(n, FieldType.SHORT_TEXT), ctx).first
        return d
    }

    private fun names(d: ProjectDoc) = FieldSettingsModel.screen(d, EntityKind.CHARACTER).rows.map { it.name }

    @Test
    fun `TC-F-27 order buttons move one place and stop at the ends`() {
        val d = basicFive()
        val key = FieldSettingsModel.screen(d, EntityKind.CHARACTER).rows.single { it.name == "성격" }.key
        val moved = FieldSettingsModel.forward(d, EntityKind.CHARACTER, key, ctx)
        assertEquals(listOf("출생", "나이", "생일", "성격", "소속"), names(moved))
        val rows = FieldSettingsModel.orderRows(FieldSettingsModel.screen(moved, EntityKind.CHARACTER))
        assertEquals("4/5", rows.single { it.name == "성격" }.position)
        assertFalse(rows.first().canForward)
        assertTrue(rows.first().canBackward)
        assertFalse(rows.last().canBackward)
        // At the ends the buttons change nothing.
        val first = rows.first().key
        assertTrue(FieldSettingsModel.forward(moved, EntityKind.CHARACTER, first, ctx) === moved)
        assertTrue(FieldSettingsModel.backward(moved, EntityKind.CHARACTER, rows.last().key, ctx) === moved)
        assertEquals(listOf("출생", "나이", "생일", "소속", "성격"), names(FieldSettingsModel.backward(moved, EntityKind.CHARACTER, key, ctx)))
    }

    @Test
    fun `TC-F-28 order mode is an explicit button`() {
        val screen = FieldSettingsModel.screen(doc, EntityKind.CHARACTER)
        assertEquals("순서 바꾸기", screen.reorderAction)
        assertEquals("완료", FieldOrderCopy.BTN_DONE)
        assertEquals("끌기 · 앞으로 · 뒤로", screen.orderModeNote)
        assertEquals(listOf("앞으로", "뒤로"), listOf(FieldOrderCopy.BTN_FORWARD, FieldOrderCopy.BTN_BACKWARD))
    }

    @Test
    fun `TC-F-29 hidden definitions count in the order`() {
        var d = basicFive()
        d = FieldDefinitions.add(d, NewField("신장", FieldType.NUMBER), ctx).first
        d = FieldDefinitions.add(d, NewField("특기", FieldType.SHORT_TEXT), ctx).first
        val height = FieldSettingsModel.screen(d, EntityKind.CHARACTER).rows.single { it.name == "신장" }.key
        // Move 신장 to position 3 of 7 and hide it.
        d = FieldSettingsModel.move(d, EntityKind.CHARACTER, height, 5, 2, ctx)
        d = FieldDefinitions.setHidden(d, height, true, ctx)
        val rows = FieldSettingsModel.orderRows(FieldSettingsModel.screen(d, EntityKind.CHARACTER))
        val row = rows.single { it.key == height }
        assertTrue(row.hidden)
        assertEquals("3/7", row.position)
        assertEquals("4/7", rows[3].position)
    }

    @Test
    fun `TC-F-30 the field delete confirm lists every fact`() {
        val birth = FieldSettingsModel.screen(doc, EntityKind.CHARACTER).rows.single { it.name == "출생" }.key
        val view = FieldDefEditModel.view(doc, birth)!!
        assertEquals(view.deleteSummary, view.deleteLines.first())
        assertTrue(view.deleteLines.contains("계산 항목 나이 · 직접 넣기로"))
    }

    @Test
    fun `TC-F-32 and 33 the save sheet groups, defaults and calc notice`() {
        var d = FieldDefinitions.setHidden(doc, FieldSettingsModel.screen(doc, EntityKind.CHARACTER).rows.single { it.name == "신장" }.key, true, ctx)
        d = FieldDefinitions.add(d, NewField("기후", FieldType.SHORT_TEXT, entityKind = EntityKind.PLACE), ctx).first
        val picks = SaveTemplateModel.initialPicks(d)
        val view = SaveTemplateModel.view(d, picks, "", hidden = false, visibleTemplates = emptyList())
        val people = view.groups.first()
        assertEquals("인물 · 11", people.label)
        assertEquals(10, people.rows.count { it.checked })
        assertFalse(people.rows.single { it.name == "신장" }.checked)
        assertTrue(people.rows.single { it.name == "신장" }.hidden)
        assertTrue(view.groups.any { it.label.startsWith("장소 · ") })
        assertFalse(view.canSave)
        assertEquals("항목 정의만 · 값 없음", TemplateCopy.DEFS_ONLY)

        val birth = people.rows.single { it.name == "출생" }.key
        val age = people.rows.single { it.name == "나이" }.key
        val without = SaveTemplateModel.view(d, picks - birth, "내 기본", false, emptyList())
        assertEquals("◇ 나이 · 출생 없이 · 직접 넣기", without.calcNotices[age])
        assertTrue(without.canSave)
        val saved = SaveTemplateModel.save(d, picks - birth, "내 기본", false, emptyList(), ctx)
        assertNull(saved.fields.single { it.name == "나이" }.calcSourceKey)
    }

    @Test
    fun `TC-F-41 the hidden switch starts on in a hidden project`() {
        val hiddenDoc = doc.copy(project = doc.project.copy(protections = doc.project.protections.copy(hidden = true)))
        assertTrue(SaveTemplateModel.initialHidden(hiddenDoc))
        assertEquals("◇ 잠가도 보임", SaveTemplateModel.view(hiddenDoc, emptySet(), "a", hidden = false, visibleTemplates = emptyList()).visibleNotice)
        assertNull(SaveTemplateModel.view(hiddenDoc, emptySet(), "a", hidden = true, visibleTemplates = emptyList()).visibleNotice)
        assertFalse(SaveTemplateModel.initialHidden(doc))
        assertNull(SaveTemplateModel.view(doc, emptySet(), "a", hidden = false, visibleTemplates = emptyList()).visibleNotice)
    }

    @Test
    fun `TC-F-40 nothing on the list names a hidden template while locked`() {
        val visible = FieldTemplate(key = "V", name = "내 기본", fields = listOf(tf("a", "특기")), order = "a")
        val hidden = FieldTemplate(key = "H", name = "유리 정원 인물", hidden = true, fields = listOf(tf("b", "유리 꽃")), order = "b")
        val all = listOf(visible, hidden, FieldTemplates.delete(hidden.copy(key = "H2"), 1L))
        val locked = TemplateListModel.view(all, hiddenUnlocked = false)
        assertEquals("템플릿 · 1", locked.sectionTitle)
        assertEquals(listOf("V"), locked.rows.map { it.key })
        assertTrue(locked.lockRow)
        assertNull(locked.trashRow)
        val strings = listOf(locked.sectionTitle) + locked.rows.flatMap { listOf(it.title, it.sub) } +
            BundleModel.templateRows(all, false).flatMap { (t, sub) -> listOf(t.name, sub) } +
            SaveTemplateModel.view(doc, emptySet(), "유리 정원 인물", false, FieldTemplates.visible(all, false)).let { listOfNotNull(it.sameName) }
        assertTrue(strings.none { it.contains("유리") })

        val open = TemplateListModel.view(all, hiddenUnlocked = true)
        assertEquals(listOf("V", "H"), open.rows.map { it.key })
        assertTrue(open.rows.single { it.key == "H" }.lockMark)
        assertFalse(open.lockRow)
        assertEquals("지운 템플릿 · 1", open.trashRow)
    }

    @Test
    fun `TC-F-43 template trash and restore`() {
        val t = FieldTemplate(key = "T", name = "내 기본", fields = listOf(tf("a", "특기")), order = "a")
        val day = java.time.ZonedDateTime.of(2026, 11, 23, 10, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
        val gone = FieldTemplates.delete(t, day)
        val view = TemplateListModel.view(listOf(gone), false)
        assertTrue(view.empty)
        assertEquals("지운 템플릿 · 1", view.trashRow)
        assertEquals("템플릿 내 기본 지움", TemplateCopy.deleted("내 기본"))
        val row = TemplateListModel.trashRows(listOf(gone), false, ZoneId.of("Asia/Seoul")).single()
        assertEquals("11월 23일 지움 · 30일 뒤 삭제", row.sub)
        val back = FieldTemplates.restore(gone, day + 1)
        assertEquals(t.fields, back.fields)
        assertEquals(listOf("T"), TemplateListModel.view(listOf(back), false).rows.map { it.key })
        assertEquals(listOf("항목 1", "프로젝트에 넣은 항목은 그대로", "지운 템플릿 · 30일 뒤 삭제"), TemplateCopy.deleteLines(1))
    }

    @Test
    fun `TC-F-36 and 38 apply from the sheet model`() {
        val t = FieldTemplate(key = "T", name = "T", fields = listOf(tf("a", "특기"), tf("c", "기후", EntityKind.PLACE)), order = "a")
        val empty = ProjectDoc(Project(key = "B", name = "B"))
        assertEquals("장소 · 용어 · 세력도" to "장소 1", BundleModel.otherKinds(empty, t, EntityKind.CHARACTER))
        assertEquals(1, BundleModel.templatePreview(empty, t, EntityKind.CHARACTER, others = false).addedCount)
        val once = BundleModel.applyTemplate(empty, t, EntityKind.CHARACTER, others = true, ctx = ctx)
        assertEquals(0, BundleModel.templatePreview(once, t, EntityKind.CHARACTER, others = true).addedCount)
        assertNull(BundleModel.otherKinds(once, t, EntityKind.CHARACTER))
    }

    @Test
    fun `template make sheet sources`() {
        assertFalse(TemplateListModel.canMake("", TemplateStart.EMPTY, null))
        assertTrue(TemplateListModel.canMake("a", TemplateStart.EMPTY, null))
        assertFalse(TemplateListModel.canMake("a", TemplateStart.FROM_TEMPLATE, null))
        val made = TemplateListModel.make("내 기본", false, TemplateStart.BASIC, emptyList(), ctx)
        assertEquals(5, made.fields.size)
        val fromProject = TemplateListModel.make("예시", false, TemplateStart.FROM_PROJECT, listOf(made), ctx, fromProject = doc)
        assertTrue(fromProject.order > made.order)
        assertEquals(11, fromProject.fields.count { it.entityKind.known == EntityKind.CHARACTER })
        assertEquals(SaveTarget.REPLACE, SaveTarget.valueOf("REPLACE"))
    }

    @Test
    fun `TC-F-47 template and order copy pass the lint`() {
        val objects = listOf(TemplateCopy, FieldOrderCopy)
        val constants = objects.flatMap { obj ->
            obj::class.java.declaredFields.filter { Modifier.isStatic(it.modifiers) && it.name != "INSTANCE" }.mapNotNull { f ->
                f.isAccessible = true
                (f.get(null) as? String)?.let { f.name to it }
            }
        }
        assertTrue(constants.size > 30)
        val formatted = listOf(
            TemplateCopy.picks(11), TemplateCopy.kindCount(EntityKind.CHARACTER, 11), TemplateCopy.calcWithoutSource("나이", "출생"),
            TemplateCopy.scopedLeftOut(1), TemplateCopy.saved("내 기본"), TemplateCopy.deleted("내 기본"), TemplateCopy.section(3),
            TemplateCopy.fields(7), TemplateCopy.list(3), TemplateCopy.trashRow(2), TemplateCopy.trashSub(11, 23),
            TemplateCopy.previewAdd(1), TemplateCopy.previewHave(2), TemplateCopy.previewClash(1), TemplateCopy.clashRow("소속", "연결"),
            TemplateCopy.otherKindsSub(listOf(EntityKind.PLACE to 1, EntityKind.FACTION to 2)),
        ) + TemplateCopy.deleteLines(7)
        val failures = constants.mapNotNull { (name, text) ->
            CopyLint.lint(text, isButton = name.startsWith("BTN_")).takeIf { it.isNotEmpty() }?.let { "$name = $text: $it" }
        } + formatted.mapNotNull { text -> CopyLint.lint(text).takeIf { it.isNotEmpty() }?.let { "$text: $it" } }
        assertEquals(emptyList<String>(), failures)
    }
}
