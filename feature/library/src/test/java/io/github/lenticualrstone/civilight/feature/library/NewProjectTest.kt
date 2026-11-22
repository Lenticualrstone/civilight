package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.newproject.StartBlock
import io.github.lenticualrstone.civilight.feature.library.newproject.CreatorPorts
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectDraftHolder
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectForm
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectRules
import io.github.lenticualrstone.civilight.feature.library.newproject.ProjectCreator
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.navigation.NewProjectStep
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 새 프로젝트 1/4 … 4/4 (LIB-09 … LIB-12): step rules, the shared draft, and `만들기` (spec §4.9). */
class NewProjectTest {
    private val defaults = RatingDefaults()

    @Test
    fun `P-06 a blank name keeps 다음 off`() {
        assertFalse(NewProjectRules.canProceed(NewProjectForm(name = "  "), NewProjectStep.NAME))
        assertTrue(NewProjectRules.canProceed(NewProjectForm(name = "유리 정원"), NewProjectStep.NAME))
        // A later step entered by process restore still refuses to create without a name.
        assertFalse(NewProjectRules.canProceed(NewProjectForm(name = ""), NewProjectStep.START))
    }

    @Test
    fun `step 1 defaults - 연재 장편 and ALL`() {
        val form = NewProjectForm()
        assertEquals(ProjectFormat.SERIAL, form.format)
        assertEquals(Rating.ALL, form.rating)
        assertEquals(StructurePreset.CHAPTER_EPISODE_SCENE, form.structurePreset)
        assertEquals(WorldTimeMode.YEAR_ONLY, form.timeMode)
        assertEquals(FieldBundle.BASIC, form.bundle)
    }

    @Test
    fun `NewNameR18 tinted block only while R-18 is picked`() {
        assertNull(NewProjectRules.r18Block(NewProjectForm(rating = Rating.R15), defaults, recoverySet = false))
        val block = NewProjectRules.r18Block(NewProjectForm(rating = Rating.R18), defaults, recoverySet = false)!!
        assertEquals("R-18 기본 동작 · 설정에서 바꾸기", block.header)
        assertTrue(block.lines.contains("프로젝트 숨기기 켬"))
        assertTrue(block.lines.contains("화면 캡처 · 최근 앱 미리 보기 가림"))
        assertNotNull(block.warning)
        assertNull(NewProjectRules.r18Block(NewProjectForm(rating = Rating.R18), defaults, recoverySet = true)!!.warning)
    }

    @Test
    fun `step 2 preview trees follow the preset`() {
        assertEquals(
            listOf(0 to "장 1", 1 to "에피소드 1-1", 2 to "씬 1", 2 to "씬 2", 1 to "에피소드 1-2"),
            NewProjectRules.preview(NewProjectForm()),
        )
        val act = NewProjectRules.withPreset(NewProjectForm(), StructurePreset.ACT_SCENE)
        assertEquals(listOf(0 to "막 1", 1 to "장면 1", 1 to "장면 2"), NewProjectRules.preview(act))
    }

    @Test
    fun `직접 정하기 edits 1 to 4 named levels`() {
        var form = NewProjectRules.withPreset(NewProjectForm(name = "x"), StructurePreset.CUSTOM)
        assertEquals(listOf("장", "에피소드", "씬"), form.customLevels)
        form = NewProjectRules.addLevel(form)
        assertEquals(4, form.customLevels.size)
        assertFalse("an unnamed level blocks 다음", NewProjectRules.canProceed(form, NewProjectStep.STRUCTURE))
        assertTrue(NewProjectRules.preview(form).isEmpty())
        assertFalse(NewProjectRules.canAddLevel(form))
        assertEquals(form, NewProjectRules.addLevel(form))
        form = NewProjectRules.setLevel(form, 3, "컷")
        assertTrue(NewProjectRules.canProceed(form, NewProjectStep.STRUCTURE))
        repeat(5) { form = NewProjectRules.removeLevel(form, 0) }
        assertEquals(listOf("컷"), form.customLevels)
        assertFalse(NewProjectRules.canRemoveLevel(form))
        // Picking another preset and back keeps the edited levels.
        val back = NewProjectRules.withPreset(NewProjectRules.withPreset(form, StructurePreset.ACT_SCENE), StructurePreset.CUSTOM)
        assertEquals(listOf("컷"), back.customLevels)
    }

    @Test
    fun `step 3 year takes digits only and 시간 없이 drops it`() {
        val form = NewProjectRules.withYear(NewProjectForm(name = "x"), "12a00년")
        assertEquals("1200", form.startYear)
        assertEquals(1200, NewProjectRules.toDraft(form).startYear)
        assertNull(NewProjectRules.toDraft(form.copy(timeMode = WorldTimeMode.NONE)).startYear)
    }

    @Test
    fun `R19-11 to R19-13 step 4 hide switch follows the rating until touched`() {
        assertTrue(NewProjectRules.hideSwitch(NewProjectForm(rating = Rating.R18), defaults))
        assertFalse(NewProjectRules.hideSwitch(NewProjectForm(rating = Rating.R15), defaults))
        assertFalse(NewProjectRules.hideSwitch(NewProjectForm(rating = Rating.R18), defaults.copy(r18Hide = false)))
        assertFalse(NewProjectRules.hideSwitch(NewProjectForm(rating = Rating.R18, step4Hide = false), defaults))

        val created = LibFx.created(NewProjectForm(name = "검은 서약", rating = Rating.R18, step4Hide = false))
        val p = created.project.protections
        assertFalse(p.hidden)
        assertTrue(p.captureBlock && p.nameMask && p.recorderLinkMask)
        val r15 = LibFx.created(NewProjectForm(name = "외전", rating = Rating.R15)).project.protections
        assertFalse(r15.hidden || r15.captureBlock || r15.nameMask || r15.recorderLinkMask)
    }

    @Test
    fun `the draft survives a restart once and a new run starts empty`() {
        val holder = NewProjectDraftHolder()
        val saved = NewProjectDraftHolder.encode(NewProjectForm(name = "유리 정원", format = ProjectFormat.SHORT, linkedRecordKeys = listOf("R1")))
        holder.restore(saved)
        assertEquals("유리 정원", holder.form.value.name)
        holder.update { it.copy(name = "유리 정원 2") }
        holder.restore(saved)
        assertEquals("restore runs once per process", "유리 정원 2", holder.form.value.name)
        holder.reset()
        assertEquals(NewProjectForm(), holder.form.value)
        assertNull(NewProjectDraftHolder.decode("not json"))
    }

    @Test
    fun `P-01 a blank first scene title gives 장 1 › 1-1 › 씬 1`() {
        val doc = LibFx.created(NewProjectForm(name = "유리 정원"))
        assertEquals(3, doc.story.units.size)
        assertEquals("1장 · 에피소드 1-1 · 씬 1", StartBlock.quickWriteLine(doc))
        val titled = LibFx.created(NewProjectForm(name = "유리 정원", firstSceneTitle = "꺼진 등불"))
        assertTrue(StartBlock.quickWriteLine(titled)!!.contains("꺼진 등불"))
    }

    @Test
    fun `P-02 a protagonist becomes one character`() {
        val doc = LibFx.created(NewProjectForm(name = "잿빛", protagonist = "리엔"))
        assertEquals(listOf("리엔"), doc.cast.characters.map { it.name })
        assertTrue(LibFx.created(NewProjectForm(name = "잿빛")).cast.characters.isEmpty())
    }

    @Test
    fun `P-04 설정만 still gets the 기본 field set and no scene`() {
        val doc = LibFx.created(NewProjectForm(name = "검은 서약", format = ProjectFormat.SETTING_ONLY))
        assertEquals(FieldBundle.BASIC, NewProjectForm().bundle)
        assertTrue(doc.story.units.isEmpty())
        val basic = LibFx.created(NewProjectForm(name = "a", bundle = FieldBundle.BASIC))
        val game = LibFx.created(NewProjectForm(name = "b", bundle = FieldBundle.GAME_PROFILE))
        assertTrue(game.project.fieldDefs.size > basic.project.fieldDefs.size)
    }

    private class FakePorts(var recorder: RecorderLibrary) : CreatorPorts {
        val added = mutableListOf<ProjectDoc>()

        override suspend fun addProject(doc: ProjectDoc) {
            added += doc
        }

        override suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary) {
            recorder = transform(recorder)
        }
    }

    @Test
    fun `P-03 checked records gain a link to the new project and nothing else`() = runTest {
        val library = ExampleRecorder.library()
        val chosen = library.records.take(2).map { it.key }
        val ports = FakePorts(library)
        val result = ProjectCreator.create(ports, NewProjectForm(name = "새 이야기", linkedRecordKeys = chosen), defaults, LibFx.ctx(), null)
        val doc = ports.added.single()
        assertEquals(doc.key, result.projectKey)
        val newLinks = ports.recorder.links.filter { it.projectKey == doc.key }
        assertEquals(chosen.toSet(), newLinks.map { it.recordKey }.toSet())
        assertEquals(library.records, ports.recorder.records)
        assertTrue("record text is not inserted anywhere", doc.allBlocks.none { it.runs.plainText.isNotEmpty() })
        assertFalse(result.hidden)
        assertFalse(result.opensCalendar)
    }

    @Test
    fun `만들기 reports a hidden project and the calendar to open`() = runTest {
        val ports = FakePorts(RecorderLibrary())
        val result = ProjectCreator.create(ports, NewProjectForm(name = "검은 서약", rating = Rating.R18, timeMode = WorldTimeMode.CALENDAR), defaults, LibFx.ctx(), null)
        assertTrue(result.hidden)
        assertTrue(result.opensCalendar)
        assertEquals(RecorderLibrary(), ports.recorder)
    }
}
