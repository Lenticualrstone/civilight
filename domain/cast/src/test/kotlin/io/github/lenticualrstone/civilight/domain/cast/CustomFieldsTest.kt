package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.export.CharacterExport
import io.github.lenticualrstone.civilight.domain.cast.field.Derivation
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
import io.github.lenticualrstone.civilight.domain.cast.field.FieldConversion
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldFilters
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldScope
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSearch
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTypes
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.field.NewField
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.field.ValueState
import io.github.lenticualrstone.civilight.domain.cast.field.valueState
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FallbackReason
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.MonthDay
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.story.CalendarDate
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDate
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Custom fields `인물 항목` (30-cast.md §4.3). */
class CustomFieldsTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    private fun read(d: ProjectDoc, character: Key, def: FieldDef, point: ReadPoint) =
        FieldReader.read(d, EntityKind.CHARACTER, character, def, point)

    private fun shown(d: ProjectDoc, character: Key, name: String, point: ReadPoint = ReadPoint.LATEST): String {
        val def = Fx.def(d, name)
        return FieldText.format(d, def, read(d, character, def, point).value).text
    }

    private fun value(d: ProjectDoc, character: Key, def: FieldDef): FieldValue? = d.index.character(character)!!.fields[def.key]

    /** A 세계 달력 of 12 months of 30 days, story start 1200 · month 1 · day 1. */
    private fun calendarProject(d: ProjectDoc): ProjectDoc = d.copy(
        project = d.project.copy(
            worldTime = WorldTime(
                mode = WorldTimeMode.CALENDAR.open(),
                storyStartYear = 1200,
                calendar = WorldCalendar(
                    months = (1..12).map { CalendarMonth("${it}의 달", 30) },
                    storyStart = MonthDay(1, 1),
                ),
            ),
        ),
    )

    private fun withSceneTime(d: ProjectDoc, sceneKey: Key, time: SceneTime): ProjectDoc =
        d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == sceneKey) it.copy(time = time) else it }))

    @Test
    fun `TC-F-01 age in 연도만 mode`() {
        assertEquals("17세", shown(doc, K.RIEN, "나이", ReadPoint.AtLens(Fx.L1_4)))
        assertEquals("12세", shown(doc, K.RIEN, "나이", ReadPoint.AtLens(Fx.lens(K.SCENE_1_4_2))))
        assertEquals("12세", shown(doc, K.RIEN, "나이", ReadPoint.BeforeBlock(K.BLOCK_B42)))
        assertEquals("12세", shown(doc, K.RIEN, "나이", ReadPoint.AtEvent(K.EVENT_BLACKOUT)))
        val age = read(doc, K.RIEN, Fx.def(doc, "나이"), ReadPoint.AtLens(Fx.L1_4))
        assertEquals(Derivation.AGE_FROM_BIRTH, age.derivation)
        assertFalse(age.hasHistory)
    }

    @Test
    fun `TC-F-02 unknown age`() {
        val header = Profile.header(doc, K.HART, Fx.LATEST, cover = false)
        assertEquals("??세", header.rows.single { it.label == "나이" }.value)
        assertEquals(ValueState.UNKNOWN, read(doc, K.HART, Fx.def(doc, "나이"), ReadPoint.LATEST).state)
        // 생일 · 소속 · 코드네임 · 출신지 are empty; 출생 ?? and 나이 ?? are not.
        assertEquals(4, Profile.overview(doc, K.HART, Fx.LATEST).emptyCount)
    }

    @Test
    fun `TC-F-03 birth with month and day fills the birthday`() {
        val d = Fx.setField(calendarProject(doc), K.RIEN, K.FIELD_BIRTH, FieldValue.of(FieldPayload.Date(1183, 5, 10)))
        val birthday = read(d, K.RIEN, Fx.def(d, "생일"), ReadPoint.LATEST)
        assertEquals(Derivation.BIRTHDAY_FROM_BIRTH, birthday.derivation)
        assertTrue(birthday.readOnly)
        assertEquals(FieldPayload.Anniversary(5, 10), birthday.value?.payload)
        assertEquals("5의 달 10일", shown(d, K.RIEN, "생일"))
    }

    @Test
    fun `TC-F-04 direct age with history`() {
        val ageDef = Fx.def(doc, "나이")
        var d = Fx.setField(doc, K.RIEN, K.FIELD_BIRTH, null)
        d = FieldValueEdits.addPointRecord(d, EntityKind.CHARACTER, K.RIEN, ageDef, Fx.number("16"), K.EPISODE_1_1, ctx).first
        d = FieldValueEdits.addPointRecord(d, EntityKind.CHARACTER, K.RIEN, ageDef, Fx.number("17"), K.EPISODE_1_6, ctx).first
        assertEquals("16세", shown(d, K.RIEN, "나이", ReadPoint.AtLens(Fx.L1_5)))
        assertEquals("17세", shown(d, K.RIEN, "나이", ReadPoint.AtLens(Fx.L1_6)))
        assertTrue(read(d, K.RIEN, ageDef, ReadPoint.AtLens(Fx.L1_6)).hasHistory)
    }

    @Test
    fun `TC-F-05 short text to number`() {
        val (withDef, def) = FieldDefinitions.add(doc, NewField("키", FieldType.SHORT_TEXT), ctx)
        var d = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.RIEN, def.key, Fx.text("163cm"), ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.SERA, def.key, Fx.text("154"), ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.MARTHA, def.key, Fx.text("키 작음"), ctx)
        val result = FieldConversion.convert(d, def.key, FieldType.NUMBER, ctx)
        val after = result.doc
        assertEquals(Fx.number("163"), value(after, K.RIEN, def))
        assertEquals(Fx.number("154"), value(after, K.SERA, def))
        assertEquals(FieldValue.fallback("키 작음", FallbackReason.CONVERSION_FAILED), value(after, K.MARTHA, def))
        assertEquals("◇ 바꾸지 못한 값 1", result.notice)
        val display = FieldText.format(after, after.index.fieldDef(def.key)!!, value(after, K.MARTHA, def))
        assertEquals("키 작음", display.text)
        assertEquals("◇ 바꾸지 못한 값", display.notice)
    }

    @Test
    fun `TC-F-06 short text to choice`() {
        val (withDef, def) = FieldDefinitions.add(doc, NewField("종족", FieldType.SHORT_TEXT), ctx)
        var d = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.RIEN, def.key, Fx.text("인간"), ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.SERA, def.key, Fx.text("인간"), ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.OWEN, def.key, Fx.text("수인"), ctx)
        val result = FieldConversion.convert(d, def.key, FieldType.CHOICE, ctx)
        val converted = result.doc.index.fieldDef(def.key)!!
        assertEquals(listOf("인간", "수인"), converted.options!!.map { it.label })
        assertEquals(0, result.failedCount)
        for ((who, label) in listOf(K.RIEN to "인간", K.SERA to "인간", K.OWEN to "수인")) {
            val v = value(result.doc, who, converted)!!
            assertEquals(ValueState.VALUE, v.valueState)
            assertEquals(label, FieldText.format(result.doc, converted, v).text)
        }
    }

    @Test
    fun `TC-F-07 disallowed conversion`() {
        assertTrue(FieldConversion.offered(Fx.def(doc, "성격")).isEmpty())
        assertTrue(FieldTypes.conversionTargets(FieldType.LONG_TEXT).isEmpty())
    }

    @Test
    fun `TC-F-08 option delete`() {
        val gender = Fx.def(doc, "성별")
        val users = FieldDefinitions.usersOfOption(doc, gender.key, K.OPTION_GENDER_FEMALE)
        assertEquals(listOf("리엔", "세라", "마르타", "이레나"), users)
        assertEquals("리엔 외 3명", FieldDefinitions.namesSummary(users))
        val after = FieldDefinitions.removeOption(doc, gender.key, K.OPTION_GENDER_FEMALE, ctx)
        val def = after.index.fieldDef(gender.key)!!
        assertTrue(def.options!!.none { it.key == K.OPTION_GENDER_FEMALE })
        for (who in listOf(K.RIEN, K.SERA, K.MARTHA, K.IRENA)) {
            val display = FieldText.format(after, def, value(after, who, def))
            assertEquals("여", display.text)
            assertEquals("◇ 보기에 없는 값", display.notice)
        }
        assertEquals("남", shown(after, K.HART, "성별"))
    }

    @Test
    fun `TC-F-09 option rename`() {
        val gender = Fx.def(doc, "성별")
        val after = FieldDefinitions.renameOption(doc, gender.key, K.OPTION_GENDER_FEMALE, "여성", ctx)
        for (who in listOf(K.RIEN, K.SERA, K.MARTHA, K.IRENA)) assertEquals("여성", shown(after, who, "성별"))
    }

    @Test
    fun `TC-F-10 unit change never converts`() {
        val after = FieldDefinitions.setUnit(doc, K.FIELD_HEIGHT, "척", ctx)
        assertEquals("154 척", shown(after, K.RIEN, "신장"))
        assertEquals(Fx.number("154"), value(after, K.RIEN, Fx.def(after, "신장")))
        assertEquals("환산하지 않음", FieldDefinitions.UNIT_NOTE)
    }

    @Test
    fun `TC-F-11 rename keeps values and export name`() {
        var d = FieldDefinitions.rename(doc, K.FIELD_HEIGHT, "키", ctx)
        d = FieldValueEdits.addPointRecord(d, EntityKind.CHARACTER, K.RIEN, Fx.def(d, "키"), Fx.number("158"), K.CHAPTER_2, ctx).first
        val before = d
        val renamed = FieldDefinitions.rename(before, K.FIELD_HEIGHT, "신장", ctx)
        assertEquals("신장", renamed.index.fieldDef(K.FIELD_HEIGHT)!!.name)
        assertEquals("height", renamed.index.fieldDef(K.FIELD_HEIGHT)!!.exportName)
        assertEquals(before.cast.characters, renamed.cast.characters)
        assertEquals(before.changes, renamed.changes)
        val fields = CharacterExport.fields(renamed, K.RIEN)
        assertFalse("신장" in fields)
        val height = fields.getValue("height") as JsonArray
        assertEquals(JsonNull, height[0].jsonObject["from"])
        assertEquals(JsonPrimitive(154.toBigDecimal()), height[0].jsonObject["value"])
        assertEquals(JsonPrimitive("2장"), height[1].jsonObject["from"])
    }

    @Test
    fun `TC-F-12 field delete and restore`() {
        val live = Fx.noahLive(doc)
        val (withDef, def) = FieldDefinitions.add(live, NewField("좋아하는 색", FieldType.SHORT_TEXT), ctx)
        var d = withDef
        for (c in CharacterEdits.live(d)) d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, c.key, def.key, Fx.text("잿빛"), ctx)
        d = FieldValueEdits.addPointRecord(d, EntityKind.CHARACTER, K.RIEN, def, Fx.text("등불색"), K.EPISODE_1_7, ctx).first
        assertEquals(12, CharacterEdits.live(d).size)
        assertEquals("값이 있는 인물 12명", FieldDefinitions.deleteSummary(d, def.key))

        val deleted = FieldDefinitions.delete(d, def.key, ctx)
        val entry = deleted.trash.entries.single { def.key in it.targetKeys }
        assertEquals(TrashKind.FIELD_DEF, entry.kind.known)
        assertEquals(LocalDate.of(2026, 12, 22), entry.purgeOn)
        assertTrue(FieldDefinitions.visibleFor(deleted, EntityKind.CHARACTER, K.RIEN).none { it.key == def.key })
        assertFalse(deleted.liveness.isFieldValueLive(EntityKind.CHARACTER, K.RIEN, def.key))
        val target = ChangeTarget.Field(EntityKind.CHARACTER, K.RIEN, def.key)
        assertTrue(deleted.history(target).isEmpty())

        val restored = FieldDefinitions.restore(deleted, def.key, ctx)
        assertTrue(restored.trash.entries.none { it.key == entry.key })
        assertEquals(12, CharacterEdits.live(restored).count { it.fields.containsKey(def.key) })
        assertEquals(1, restored.history(target).size)
        assertEquals("등불색", FieldText.format(restored, def, read(restored, K.RIEN, def, ReadPoint.LATEST).value).text)
    }

    @Test
    fun `TC-F-13 empty fields hidden`() {
        var d = Fx.setField(doc, K.RIEN, K.FIELD_CODENAME, null)
        d = Fx.setField(d, K.RIEN, K.FIELD_ORIGIN, null)
        d = Fx.setField(d, K.RIEN, K.FIELD_SPECIALTY, null)
        val view = Profile.overview(d, K.RIEN, Fx.LATEST)
        assertEquals("빈 항목 3 · 보기", view.emptyRow)
        assertTrue(view.items.none { it.label in setOf("코드네임", "출신지", "특기") })
        val edit = Profile.overview(d, K.RIEN, Fx.LATEST, showEmpty = true)
        assertTrue(edit.items.map { it.label }.containsAll(listOf("코드네임", "출신지", "특기")))
        assertEquals(listOf("성격"), view.sections.map { it.title })
        assertEquals("Ⅰ", view.sections.single().numeral)
    }

    @Test
    fun `TC-F-14 add a field from the record`() {
        val (all, shared) = FieldDefinitions.addFromRecord(doc, K.RIEN, "좋아하는 것", FieldType.SHORT_TEXT, FieldScope.ALL_CHARACTERS, ctx)
        assertNull(shared.scopeCharacterKey)
        assertTrue(FieldDefinitions.visibleFor(all, EntityKind.CHARACTER, K.HART).any { it.key == shared.key })
        assertTrue(read(all, K.HART, shared, ReadPoint.LATEST).isEmpty)

        val (only, own) = FieldDefinitions.addFromRecord(doc, K.RIEN, "좋아하는 것", FieldType.SHORT_TEXT, FieldScope.THIS_CHARACTER, ctx)
        assertEquals(K.RIEN, own.scopeCharacterKey)
        assertTrue(FieldDefinitions.visibleFor(only, EntityKind.CHARACTER, K.HART).none { it.key == own.key })
        val withValue = FieldValueEdits.setBase(only, EntityKind.CHARACTER, K.RIEN, own.key, Fx.text("별 보기"), ctx)
        assertTrue(Profile.row(withValue, K.RIEN, own, Fx.LATEST).characterOnly)

        val promoted = FieldDefinitions.promoteToAll(withValue, own.key, ctx)
        assertNull(promoted.index.fieldDef(own.key)!!.scopeCharacterKey)
        assertEquals(Fx.text("별 보기"), value(promoted, K.RIEN, own))
        assertTrue(FieldDefinitions.visibleFor(promoted, EntityKind.CHARACTER, K.HART).any { it.key == own.key })
    }

    @Test
    fun `TC-F-15 bundle default and 항목 더하기`() {
        assertEquals(Triple("인물 항목", "출생 · 나이 · 생일 · 소속 · 성격", "기본 ›"), FieldBundles.newProjectRow())
        assertEquals(FieldBundle.BASIC, FieldBundles.DEFAULT)

        val fresh = ProjectDoc(project = Project(key = Fx.key("PRJT", 1), name = "새 프로젝트", format = ProjectFormat.SETTING_ONLY.open()))
        val basic = FieldBundles.apply(fresh, FieldBundle.BASIC, ctx).doc
        val birth = Fx.def(basic, "출생")
        val withRien = Fx.addCharacter(
            basic, Fx.key("CHAR", 1), "리엔", 1,
            fields = mapOf(birth.key to Fx.year(1183)),
        )
        val before = FieldDefinitions.live(withRien, EntityKind.CHARACTER)
        val result = FieldBundles.apply(withRien, FieldBundle.GAME_PROFILE, ctx)
        assertEquals(listOf("코드네임", "성별", "종족", "출신지", "신장", "특기"), result.added.map { it.name })
        val after = FieldDefinitions.live(result.doc, EntityKind.CHARACTER)
        assertEquals(11, after.size)
        assertEquals(before, after.take(5))
        assertEquals(withRien.cast.characters, result.doc.cast.characters)
        assertEquals("게임 프로필", result.doc.project.cast.bundleName.character)
        // Applying again adds nothing.
        assertTrue(FieldBundles.apply(result.doc, FieldBundle.GAME_PROFILE, ctx).added.isEmpty())
    }

    @Test
    fun `TC-F-16 bundle from another project`() {
        val current = LibraryEntry(key = K.PROJECT, name = Fx.PROJECT_NAME)
        val glass = LibraryEntry(key = Fx.key("PRJT", 2), name = "유리 정원", hidden = true)
        assertEquals(emptyList<LibraryEntry>(), FieldBundles.sourceProjects(listOf(current, glass), K.PROJECT, hiddenUnlocked = false))
        assertEquals(listOf(glass), FieldBundles.sourceProjects(listOf(current, glass), K.PROJECT, hiddenUnlocked = true))

        val target = Fx.addCharacter(ProjectDoc(project = Project(key = glass.key, name = "유리 정원")), Fx.key("CHAR", 1), "유리", 1)
        val result = FieldBundles.applyFromProject(target, doc, ctx)
        assertEquals(FieldDefinitions.live(doc, EntityKind.CHARACTER).map { it.name }, result.added.map { it.name })
        assertTrue(result.doc.cast.characters.single().fields.isEmpty())
        val age = Fx.def(result.doc, "나이")
        assertEquals(Fx.def(result.doc, "출생").key, age.calc?.sourceFieldDefKey)
    }

    @Test
    fun `TC-F-17 list row composition`() {
        assertEquals("17세 · 등불 결사 · 막내 등불지기", Profile.listSubtitle(doc, K.RIEN, Fx.LATEST))
        val row = CastList.build(doc, Fx.LATEST).rows.first { it.key == K.RIEN }
        assertEquals("1-2", row.firstAppearance)
        assertEquals("리", row.initial)
    }

    @Test
    fun `TC-F-18 header limit on cover`() {
        var d = doc
        for (name in listOf("성별", "코드네임", "출신지", "특기")) {
            val def = Fx.def(d, name)
            d = FieldDefinitions.setPlaces(d, def.key, inHeader = true, inList = def.inList, inFilter = def.inFilter, ctx = ctx)
        }
        assertEquals(8, FieldDefinitions.live(d, EntityKind.CHARACTER).count { it.inHeader })
        val cover = Profile.header(d, K.RIEN, Fx.LATEST, cover = true)
        assertEquals(6, cover.rows.size)
        assertEquals("항목 더 보기", cover.moreLabel)
        val inner = Profile.header(d, K.RIEN, Fx.LATEST, cover = false)
        assertEquals(10, inner.rows.size)
        assertNull(inner.moreLabel)
        assertEquals("인물 기록 007 · 최신 1-7", cover.title)
    }

    @Test
    fun `TC-F-19 field search`() {
        val hits = FieldSearch.search(doc, "성직자")
        assertEquals(1, hits.size)
        assertEquals(K.MARTHA, hits.single().characterKey)
        assertEquals("특기 · 성직자", hits.single().label)
        assertEquals(0 until 3, hits.single().match)
    }

    @Test
    fun `TC-F-20 filter only on allowed types`() {
        val allowed = FieldTypes.ALL.filter(FieldTypes::canFilter)
        assertEquals(listOf(FieldType.CHOICE, FieldType.MULTI_CHOICE, FieldType.LINK), allowed)
        for (type in FieldTypes.ALL - allowed.toSet()) {
            val (_, def) = FieldDefinitions.add(doc, NewField("시험", type, inFilter = true), ctx)
            assertFalse(def.inFilter)
        }
        val (_, choice) = FieldDefinitions.add(doc, NewField("시험", FieldType.CHOICE, inFilter = true), ctx)
        assertTrue(choice.inFilter)
    }

    @Test
    fun `TC-F-21 hide and unhide a field definition`() {
        val hidden = FieldDefinitions.setHidden(doc, K.FIELD_HEIGHT, true, ctx)
        assertTrue(Profile.header(hidden, K.RIEN, Fx.LATEST, cover = false).rows.none { it.label == "신장" })
        assertTrue(Profile.overview(hidden, K.RIEN, Fx.LATEST, showEmpty = true).items.none { it.label == "신장" })
        assertFalse("height" in CharacterExport.fields(hidden, K.RIEN))
        val row = FieldSettings.screen(hidden).rows.single { it.key == K.FIELD_HEIGHT }
        assertTrue(row.hidden)
        assertTrue(row.sub.endsWith("숨김"))
        assertEquals(Fx.number("154"), value(hidden, K.RIEN, Fx.def(hidden, "신장")))

        val shownAgain = FieldDefinitions.setHidden(hidden, K.FIELD_HEIGHT, false, ctx)
        assertEquals("154 cm", Profile.header(shownAgain, K.RIEN, Fx.LATEST, cover = false).rows.single { it.label == "신장" }.value)
    }

    @Test
    fun `TC-F-22 short text to link`() {
        val (withDef, def) = FieldDefinitions.add(doc, NewField("소속", FieldType.SHORT_TEXT), ctx)
        var d = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.RIEN, def.key, Fx.text("등불 결사"), ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.SERA, def.key, Fx.text("결사"), ctx)
        val result = FieldConversion.convert(d, def.key, FieldType.LINK, ctx, FieldLinkTargetKind.FACTION)
        assertEquals(FieldValue.of(FieldPayload.Link(listOf(K.FACTION_LANTERN_SOCIETY))), value(result.doc, K.RIEN, def))
        assertEquals(FieldValue.fallback("결사", FallbackReason.CONVERSION_FAILED), value(result.doc, K.SERA, def))
        assertEquals("◇ 바꾸지 못한 값 1", result.notice)
        assertEquals(FieldLinkTargetKind.FACTION, result.doc.index.fieldDef(def.key)!!.linkTargetKind?.known)
    }

    @Test
    fun `TC-F-23 choice to multi-choice`() {
        val result = FieldConversion.convert(doc, K.FIELD_SPECIES, FieldType.MULTI_CHOICE, ctx)
        val def = result.doc.index.fieldDef(K.FIELD_SPECIES)!!
        assertEquals(FieldType.MULTI_CHOICE, def.type.known)
        assertEquals(FieldValue.of(FieldPayload.Choice(listOf(K.OPTION_SPECIES_HUMAN))), value(result.doc, K.RIEN, def))
        assertEquals(0, result.failedCount)
        assertNull(result.notice)
    }

    @Ignore("spec conflict: TC-F-24 types 소속 as `연결` (CST-19, CST-24) but the fixture's 소속 is `짧은 글` (fixture.md conflict #10)")
    @Test
    fun `TC-F-24 typed to short text as stated`() {
        assertEquals(FieldType.LINK, Fx.def(doc, "소속").type.known)
        val result = FieldConversion.convert(doc, K.FIELD_AFFILIATION, FieldType.SHORT_TEXT, ctx)
        assertEquals("등불 결사", shown(result.doc, K.SERA, "소속"))
    }

    @Test
    fun `TC-F-24 typed to short text`() {
        val height = FieldConversion.convert(doc, K.FIELD_HEIGHT, FieldType.SHORT_TEXT, ctx)
        assertEquals(Fx.text("154 cm"), value(height.doc, K.RIEN, Fx.def(height.doc, "신장")))
        assertEquals(0, height.failedCount)
        val birth = FieldConversion.convert(doc, K.FIELD_BIRTH, FieldType.SHORT_TEXT, ctx)
        assertEquals(Fx.text("1183"), value(birth.doc, K.RIEN, Fx.def(birth.doc, "출생")))
        assertEquals(0, birth.failedCount)
        // 연결 `소속` (synthetic field, see the ignored case) → the visible name.
        val (withDef, link) = FieldDefinitions.add(doc, NewField("소속", FieldType.LINK, linkTargetKind = FieldLinkTargetKind.FACTION), ctx)
        val d = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.SERA, link.key, FieldValue.of(FieldPayload.Link(listOf(K.FACTION_LANTERN_SOCIETY))), ctx)
        val converted = FieldConversion.convert(d, link.key, FieldType.SHORT_TEXT, ctx)
        assertEquals(Fx.text("등불 결사"), value(converted.doc, K.SERA, link))
        assertEquals(0, converted.failedCount)
    }

    @Test
    fun `TC-F-25 world-calendar age`() {
        val base = Fx.setField(calendarProject(doc), K.RIEN, K.FIELD_BIRTH, FieldValue.of(FieldPayload.Date(1183, 5, 10)))
        val march = withSceneTime(base, K.SCENE_1_7_3, SceneTime.Direct(calendarDate = CalendarDate(3, 1)))
        assertEquals("16세", shown(march, K.RIEN, "나이", ReadPoint.BeforeBlock(K.BLOCK_042)))
        assertEquals("16세", shown(march, K.RIEN, "나이", ReadPoint.AtLens(Fx.lens(K.SCENE_1_7_3))))
        val june = withSceneTime(base, K.SCENE_1_7_3, SceneTime.Direct(calendarDate = CalendarDate(6, 1)))
        assertEquals("17세", shown(june, K.RIEN, "나이", ReadPoint.BeforeBlock(K.BLOCK_042)))
        val yearOnly = withSceneTime(base, K.SCENE_1_7_3, SceneTime.Flashback(mode = FlashbackMode.DIRECT.open(), year = 1200))
        assertEquals("17세", shown(yearOnly, K.RIEN, "나이", ReadPoint.BeforeBlock(K.BLOCK_042)))
    }

    @Test
    fun `TC-F-26 panel uses the first two header fields`() {
        assertEquals("화자 · 리엔 · 17세 · 등불 결사", Profile.panelLine(doc, K.RIEN, K.BLOCK_042))
    }

    // --- Own edge cases ---

    @Test
    fun `secret fields show only on the 비밀 tab`() {
        val (withDef, secret) = FieldDefinitions.add(doc, NewField("진짜 이름", FieldType.SHORT_TEXT, inHeader = true, inList = true, isSecret = true), ctx)
        val d = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.RIEN, secret.key, Fx.text("리에나"), ctx)
        assertTrue(Profile.header(d, K.RIEN, Fx.LATEST, cover = false).rows.none { it.label == "진짜 이름" })
        assertFalse("리에나" in Profile.listSubtitle(d, K.RIEN, Fx.LATEST))
        assertTrue(FieldSearch.search(d, "리에나").isEmpty())
        assertEquals(listOf("리에나"), Profile.secretTab(d, K.RIEN, Fx.LATEST).rows.map { it.value })
        assertFalse("진짜 이름" in CharacterExport.fields(d, K.RIEN))
        assertEquals(JsonPrimitive("리에나"), CharacterExport.fields(d, K.RIEN, includeSecret = true)["진짜 이름"])
    }

    @Test
    fun `new field sheet checks the name`() {
        assertFalse(FieldDefinitions.check(doc, NewField("  ", FieldType.SHORT_TEXT)).canSave)
        val same = FieldDefinitions.check(doc, NewField("신장", FieldType.NUMBER))
        assertTrue(same.canSave)
        assertEquals("◇ 같은 이름 항목 있음", same.sameNameNotice)
        assertNull(FieldDefinitions.check(doc, NewField("체중", FieldType.NUMBER)).sameNameNotice)
    }

    @Test
    fun `settings screen rows`() {
        val screen = FieldSettings.screen(doc)
        assertEquals("고정 항목 · 7", screen.fixedTitle)
        assertEquals("프로젝트 항목 · 11", screen.projectTitle)
        val subs = screen.rows.associate { it.name to it.sub }
        assertEquals("날짜 · 연도", subs["출생"])
        assertEquals("계산 · 출생에서 · 머리 · 목록", subs["나이"])
        assertEquals("숫자 · cm · 머리", subs["신장"])
        assertEquals("고르기 · 보기 3 · 필터", subs["성별"])
        assertEquals("고르기 · 보기 4 · 머리 · 필터", subs["종족"])
        assertEquals("긴 글 · 개요", subs["성격"])
        assertEquals("연결 · 장소 · 필터", subs["출신지"])
        assertEquals("시작 묶음 · 게임 프로필", screen.bundleTitle)
        assertEquals("게임 프로필 · 고정 7 · 프로젝트 11 · 장소 · 용어 · 세력도", FieldSettings.entryRow(doc).second)
    }

    @Test
    fun `filters and sort orders`() {
        val chips = FieldFilters.chips(doc)
        assertEquals(listOf("성별", "종족", "출신지", "특기"), chips.map { it.label })
        assertEquals(listOf("신장순", "출생순", "나이순", "생일순").sorted(), FieldFilters.sortOrders(doc).map { it.label }.sorted())
        val gender = Fx.def(doc, "성별")
        val women = CharacterEdits.live(doc).filter { FieldFilters.matches(doc, it.key, gender, setOf(K.OPTION_GENDER_FEMALE)) }
        assertEquals(listOf(K.RIEN, K.SERA, K.MARTHA, K.IRENA), women.map { it.key })
        val byHeight = FieldFilters.sort(doc, CharacterEdits.live(doc), Fx.def(doc, "신장")).map { it.name }
        assertEquals(listOf("리엔", "세라", "마르타", "오웬", "하르트", "카일"), byHeight.take(6))
        // ?? and empty last: 이레나 has no height.
        assertTrue(byHeight.indexOf("이레나") > byHeight.indexOf("카일"))
        val byAge = FieldFilters.sort(doc, CharacterEdits.live(doc), Fx.def(doc, "나이")).map { it.name }
        assertEquals("하르트", byAge.last())
        assertTrue(FieldFilters.inRange(doc, K.RIEN, Fx.def(doc, "신장"), 150.toBigDecimal(), 160.toBigDecimal()))
        assertFalse(FieldFilters.inRange(doc, K.HART, Fx.def(doc, "신장"), 150.toBigDecimal(), 160.toBigDecimal()))
    }

    @Test
    fun `long text is searched by sentence`() {
        val hits = FieldSearch.search(doc, "물러서지")
        assertEquals(1, hits.size)
        assertEquals("겁이 많지만 한 번 정한 일에서는 물러서지 않는다.", hits.single().text)
        assertNotNull(hits.single().match)
    }

    @Test
    fun `empty is never stored`() {
        val cleared = FieldValueEdits.setBase(doc, EntityKind.CHARACTER, K.RIEN, K.FIELD_CODENAME, Fx.text("  "), ctx)
        assertFalse(cleared.index.character(K.RIEN)!!.fields.containsKey(K.FIELD_CODENAME))
        val unknown = FieldValueEdits.setUnknown(doc, EntityKind.CHARACTER, K.RIEN, K.FIELD_CODENAME, ctx)
        assertEquals("??", shown(unknown, K.RIEN, "코드네임"))
        assertEquals("?? cm", FieldText.unknownText(Fx.def(doc, "신장")))
    }
}
