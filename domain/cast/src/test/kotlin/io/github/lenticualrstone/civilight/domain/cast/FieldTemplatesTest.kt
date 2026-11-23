package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldScope
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.domain.cast.field.NewField
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.app.FieldTemplatesFile
import io.github.lenticualrstone.civilight.model.app.TemplateField
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 인물 템플릿 (30-cast.md §7: CST-56 … CST-64, TC-F-32 … 38, 40, 44, 45, 48). */
class FieldTemplatesTest {
    private val ctx = Fx.ctx()

    private fun tf(
        key: String,
        name: String,
        type: FieldType,
        origin: String = "tpl:$key",
        kind: EntityKind = EntityKind.CHARACTER,
        options: List<String> = emptyList(),
        calcSourceKey: Key? = null,
        link: FieldLinkTargetKind? = null,
        order: String = "a$key",
    ) = TemplateField(
        key = key, origin = origin, entityKind = kind.open(), name = name, type = type.open(), order = order,
        options = options, calcSourceKey = calcSourceKey, linkTargetKind = link?.open(),
        datePrecision = if (type == FieldType.DATE) DatePrecision.YEAR.open() else null,
    )

    private fun template(vararg fields: TemplateField, hidden: Boolean = false, key: String = "T", name: String = "T") =
        FieldTemplate(key = key, name = name, hidden = hidden, fields = fields.toList(), order = "a$key")

    private fun empty(key: String = "B") = ProjectDoc(Project(key = key, name = key))

    private fun add(doc: ProjectDoc, field: NewField): ProjectDoc = FieldDefinitions.add(doc, field, ctx).first

    private fun def(doc: ProjectDoc, name: String, kind: EntityKind = EntityKind.CHARACTER) =
        FieldDefinitions.live(doc, kind).single { it.name == name }

    /** The example project (게임 프로필, 11 인물 fields) with `신장` hidden, an `이 인물만` field and 장소 `기후`. */
    private fun exampleLike(): ProjectDoc {
        var d = Fx.doc()
        d = FieldDefinitions.setHidden(d, def(d, "신장").key, true, ctx)
        d = FieldDefinitions.addFromRecord(d, K.RIEN, "좋아하는 것", FieldType.SHORT_TEXT, FieldScope.THIS_CHARACTER, ctx).first
        d = add(d, NewField("기후", FieldType.SHORT_TEXT, entityKind = EntityKind.PLACE))
        return d
    }

    @Test
    fun `TC-F-32 save picks and what a template holds`() {
        val d = exampleLike()
        val people = FieldTemplates.candidates(d, EntityKind.CHARACTER)
        assertEquals(11, people.size)
        val picks = FieldTemplates.defaultPicks(d)
        assertEquals(10, people.count { it.key in picks })
        assertFalse(def(d, "신장").key in picks)
        assertTrue(def(d, "기후", EntityKind.PLACE).key in picks)
        assertTrue(FieldTemplates.scopedCount(d) >= 1)
        assertTrue(FieldTemplates.candidates(d, EntityKind.CHARACTER).none { it.name == "좋아하는 것" })

        val t = FieldTemplates.fromProject(d, picks, " 내 기본 ", hidden = false, ctx = ctx)
        assertEquals("내 기본", t.name)
        assertEquals(picks.size, t.fields.size)
        assertEquals("basic.birth", t.fields.single { it.name == "출생" }.origin)
        val climate = t.fields.single { it.name == "기후" }
        assertEquals("tpl:${climate.key}", climate.origin)
        assertEquals(listOf(def(d, "기후", EntityKind.PLACE).key), climate.sourceDefKeys)
        // The age keeps its source inside the template.
        assertEquals(t.fields.single { it.name == "출생" }.key, t.fields.single { it.name == "나이" }.calcSourceKey)
        // No values, no project name or key: the JSON never names the project.
        val json = CivilightJson.encode(FieldTemplatesFile.serializer(), FieldTemplatesFile(templates = listOf(t)))
        assertFalse(json.contains(d.project.key))
        assertFalse(json.contains(d.project.name))
        assertFalse(json.contains("좋아하는 것"))
    }

    @Test
    fun `TC-F-33 a calc without its source is saved as typed age`() {
        val d = exampleLike()
        val picks = FieldTemplates.defaultPicks(d) - def(d, "출생").key
        val warn = FieldTemplates.calcWithoutSource(d, picks).single()
        assertEquals("나이" to "출생", warn.calcName to warn.sourceName)
        val t = FieldTemplates.fromProject(d, picks, "x", false, ctx)
        assertNull(t.fields.single { it.name == "나이" }.calcSourceKey)
    }

    private fun projectB(): ProjectDoc {
        var b = empty()
        b = add(b, NewField("출생", FieldType.DATE, bundleOrigin = "basic.birth"))
        b = add(b, NewField("소속", FieldType.SHORT_TEXT))
        b = add(b, NewField("키", FieldType.NUMBER, bundleOrigin = "tpl:h1"))
        return b
    }

    private val t34 = template(
        tf("b", "출생", FieldType.DATE, origin = "basic.birth", order = "a1"),
        tf("s", "소속", FieldType.LINK, link = FieldLinkTargetKind.FACTION, order = "a2"),
        tf("h1", "신장", FieldType.NUMBER, order = "a3"),
        tf("s1", "특기", FieldType.MULTI_CHOICE, options = listOf("검술", "기록"), order = "a4"),
    )

    @Test
    fun `TC-F-34 apply preview and matching`() {
        val b = projectB()
        val p = FieldTemplates.preview(b, t34, setOf(EntityKind.CHARACTER)).of(EntityKind.CHARACTER)!!
        assertEquals(listOf("특기"), p.added.map { it.name })
        assertEquals(listOf("출생", "신장"), p.present.map { it.name })
        assertEquals(listOf("소속" to "연결"), p.clashes.map { it.name to it.typeLabel })

        val applied = FieldTemplates.apply(b, t34, setOf(EntityKind.CHARACTER), ctx)
        assertEquals(listOf("특기"), applied.added.map { it.name })
        val names = FieldDefinitions.live(applied.doc, EntityKind.CHARACTER).map { it.name }
        assertEquals(listOf("출생", "소속", "키", "특기"), names)
        assertEquals("tpl:s1", def(applied.doc, "특기").bundleOrigin)
        assertEquals(FieldType.SHORT_TEXT, def(applied.doc, "소속").type.known)
        assertEquals("T", FieldSettings.bundleName(applied.doc, EntityKind.CHARACTER))
    }

    @Test
    fun `TC-F-35 apply never changes existing options or values`() {
        var b = empty()
        b = add(b, NewField("종족", FieldType.CHOICE, options = listOf("인간", "수인"), bundleOrigin = "gameProfile.species"))
        val t = template(tf("sp", "종족", FieldType.CHOICE, origin = "gameProfile.species", options = listOf("인간", "수인", "용족")))
        val applied = FieldTemplates.apply(b, t, setOf(EntityKind.CHARACTER), ctx)
        assertTrue(applied.added.isEmpty())
        assertEquals(listOf("인간", "수인"), def(applied.doc, "종족").options!!.map { it.label })
    }

    @Test
    fun `TC-F-36 applying twice adds nothing the second time`() {
        val once = FieldTemplates.apply(projectB(), t34, setOf(EntityKind.CHARACTER), ctx).doc
        assertEquals(0, FieldTemplates.preview(once, t34, setOf(EntityKind.CHARACTER)).addedCount)
        val twice = FieldTemplates.apply(once, t34, setOf(EntityKind.CHARACTER), ctx)
        assertTrue(twice.added.isEmpty())
        assertSame(once, twice.doc)
    }

    @Test
    fun `TC-F-37 calc source on apply uses the matched field`() {
        val b = projectB()
        val t = template(
            tf("b", "출생", FieldType.DATE, origin = "basic.birth", order = "a1"),
            tf("g", "나이", FieldType.CALC, origin = "basic.age", calcSourceKey = "b", order = "a2"),
        )
        val applied = FieldTemplates.apply(b, t, setOf(EntityKind.CHARACTER), ctx).doc
        assertEquals(def(b, "출생").key, def(applied, "나이").calc?.sourceFieldDefKey)
    }

    @Test
    fun `TC-F-38 other kinds are opt-in`() {
        val t = template(
            tf("s1", "특기", FieldType.SHORT_TEXT),
            tf("c1", "기후", FieldType.SHORT_TEXT, kind = EntityKind.PLACE),
        )
        val b = empty()
        val others = FieldTemplates.otherKindsWithAdds(b, t, EntityKind.CHARACTER)
        assertEquals(listOf(EntityKind.PLACE to 1), others.map { it.kind to it.added.size })
        val onlyPeople = FieldTemplates.apply(b, t, setOf(EntityKind.CHARACTER), ctx)
        assertEquals(listOf("특기"), onlyPeople.added.map { it.name })
        val both = FieldTemplates.apply(b, t, setOf(EntityKind.CHARACTER, EntityKind.PLACE), ctx)
        assertEquals(listOf("특기", "기후"), both.added.map { it.name })
        assertEquals("T", FieldSettings.bundleName(both.doc, EntityKind.PLACE))
    }

    @Test
    fun `TC-F-39 at creation every kind is applied and the bundle is TEMPLATE`() {
        val t = template(
            tf("s1", "특기", FieldType.SHORT_TEXT),
            tf("c1", "기후", FieldType.SHORT_TEXT, kind = EntityKind.PLACE),
            name = "내 기본",
        )
        val created = FieldTemplates.forProjectCreation(empty(), t, ctx)
        assertEquals(FieldBundle.TEMPLATE, created.project.fieldBundleAtCreation.known)
        assertEquals(1, FieldTemplates.candidates(created, EntityKind.PLACE).size)
        assertTrue(FieldSettings.entryRow(created).second.startsWith("내 기본 · 고정 7 · 프로젝트 1"))
        assertEquals("인물 1 · 장소 1", FieldTemplates.kindCounts(t))
        assertEquals("항목 없음", FieldTemplates.kindCounts(template()))
    }

    @Test
    fun `TC-F-40 hidden templates stay hidden while locked`() {
        val visible = template(tf("a", "특기", FieldType.SHORT_TEXT), key = "V", name = "내 기본")
        val hidden = template(tf("b", "유리 꽃", FieldType.SHORT_TEXT), hidden = true, key = "H", name = "유리 정원 인물")
        val all = listOf(visible, hidden)
        assertEquals(listOf("V"), FieldTemplates.visible(all, hiddenUnlocked = false).map { it.key })
        assertTrue(FieldTemplates.showsLockRow(all, hiddenUnlocked = false))
        assertNull(FieldTemplates.visibleByKey(all, "H", hiddenUnlocked = false))
        assertFalse(FieldTemplates.hasSameName(FieldTemplates.visible(all, false), "유리 정원 인물"))
        assertEquals(listOf("H", "V"), FieldTemplates.visible(all, hiddenUnlocked = true).map { it.key })
        assertFalse(FieldTemplates.showsLockRow(all, hiddenUnlocked = true))
        assertEquals("숨긴 템플릿", FieldTemplates.shownName(hidden, hiddenUnlocked = false))
        // Trashed hidden templates are left out of `지운 템플릿` while locked.
        val trashed = listOf(FieldTemplates.delete(hidden, 1L), FieldTemplates.delete(visible, 2L))
        assertEquals(listOf("V"), FieldTemplates.trashed(trashed, false).map { it.key })
        // A visible project that applies it shows `시작 묶음 · 숨긴 템플릿`.
        val applied = FieldTemplates.apply(empty(), hidden, setOf(EntityKind.CHARACTER), ctx).doc
        assertEquals("숨긴 템플릿", FieldSettings.bundleName(applied, EntityKind.CHARACTER))
        assertFalse(FieldSettings.screen(applied).bundleTitle.contains("유리 정원"))
    }

    @Test
    fun `TC-F-44 template editor over the scratch document`() {
        val t = template(
            tf("b", "출생", FieldType.DATE, origin = "basic.birth", order = "a1").copy(sourceDefKeys = listOf("D1")),
            tf("g", "나이", FieldType.CALC, origin = "basic.age", calcSourceKey = "b", order = "a2"),
            tf("s", "소속", FieldType.LINK, link = FieldLinkTargetKind.FACTION, order = "a3"),
            tf("o", "종족", FieldType.CHOICE, options = listOf("인간", "수인"), order = "a4"),
        )
        val scratch = FieldTemplates.toScratchDoc(t)
        // Round trip without edits keeps the template as it is.
        assertSame(t, FieldTemplates.fromScratchDoc(scratch, t, 5L))
        // Any type, no preview: 연결 → 짧은 글.
        assertEquals(9, FieldTemplates.ALL_TYPES.size)
        var d = FieldTemplates.setType(scratch, "s", FieldType.SHORT_TEXT, ctx)
        // `빼기` on 출생: 나이 loses its source.
        d = FieldTemplates.removeField(d, "b", ctx)
        d = FieldTemplates.removeOption(d, "o", FieldTemplates.optionKey("o", 1, 2), ctx)
        d = FieldDefinitions.add(d, NewField("특기", FieldType.SHORT_TEXT), ctx).first
        val after = FieldTemplates.fromScratchDoc(d, t, 5L)
        assertEquals(listOf("나이", "소속", "종족", "특기"), after.fields.map { it.name })
        assertNull(after.fields.single { it.name == "나이" }.calcSourceKey)
        assertEquals("basic.age", after.fields.single { it.name == "나이" }.origin)
        assertEquals(FieldType.SHORT_TEXT, after.fields.single { it.name == "소속" }.type.known)
        assertEquals(listOf("인간"), after.fields.single { it.name == "종족" }.options)
        val skill = after.fields.single { it.name == "특기" }
        assertEquals("tpl:${skill.key}", skill.origin)
        assertEquals(5L, after.updatedAt)
    }

    @Test
    fun `scratch round trip keeps keys, origins, source keys, calc and order`() {
        val t = FieldTemplates.fromProject(exampleLike(), FieldTemplates.defaultPicks(exampleLike()), "x", false, ctx)
        val back = FieldTemplates.fromScratchDoc(FieldTemplates.toScratchDoc(t), t, 9L)
        assertEquals(t.fields, back.fields)
    }

    @Test
    fun `TC-F-45 replacing a template keeps renamed fields by source key`() {
        var a = empty("A")
        a = add(a, NewField("특기", FieldType.SHORT_TEXT))
        val saved = FieldTemplates.fromProject(a, FieldTemplates.defaultPicks(a), "내 기본", false, ctx)
        val skill = saved.fields.single()
        a = FieldDefinitions.rename(a, def(a, "특기").key, "장기", ctx)
        a = add(a, NewField("기후", FieldType.SHORT_TEXT, entityKind = EntityKind.PLACE))
        val replaced = FieldTemplates.replaceFields(saved, a, FieldTemplates.defaultPicks(a), ctx)
        val kept = replaced.fields.single { it.name == "장기" }
        assertEquals(skill.key, kept.key)
        assertEquals(skill.origin, kept.origin)
        val climate = replaced.fields.single { it.name == "기후" }
        assertNotEquals(skill.key, climate.key)
        assertEquals("tpl:${climate.key}", climate.origin)
    }

    @Test
    fun `TC-F-48 round trip to the source project matches by source keys`() {
        var a = Fx.doc()
        val t = FieldTemplates.fromProject(a, FieldTemplates.defaultPicks(a), "내 기본", false, ctx)
        a = FieldDefinitions.rename(a, def(a, "신장").key, "키", ctx)
        val p = FieldTemplates.preview(a, t, FieldTemplates.KINDS.toSet())
        assertEquals(0, p.addedCount)
        assertTrue(p.of(EntityKind.CHARACTER)!!.present.any { it.name == "신장" })
    }

    @Test
    fun `new templates from bundles and other templates`() {
        val basic = FieldTemplates.fieldsFromBundle(FieldBundle.BASIC, ctx)
        assertEquals(listOf("출생", "나이", "생일", "소속", "성격"), basic.map { it.name })
        assertEquals("basic.birth", basic.first().origin)
        assertEquals(basic.first().key, basic[1].calcSourceKey)
        val copy = FieldTemplates.copyFields(template(*basic.toTypedArray()), ctx)
        assertEquals(basic.map { it.origin }, copy.map { it.origin })
        assertEquals(copy.first().key, copy[1].calcSourceKey)
        assertTrue(copy.none { c -> basic.any { it.key == c.key } })
    }

    @Test
    fun `restore merge keeps newer and never deletes local`() {
        val local = listOf(template(key = "A").copy(updatedAt = 5), template(key = "B").copy(updatedAt = 5))
        val incoming = listOf(template(key = "A", name = "new").copy(updatedAt = 9), template(key = "B", name = "old").copy(updatedAt = 1), template(key = "C"))
        val merged = FieldTemplates.mergeByKey(local, incoming)
        assertEquals(listOf("A", "B", "C"), merged.map { it.key })
        assertEquals("new", merged[0].name)
        assertEquals("T", merged[1].name)
    }
}
