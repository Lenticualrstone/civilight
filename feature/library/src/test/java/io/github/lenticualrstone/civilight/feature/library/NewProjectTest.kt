package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.domain.library.newproject.StartBlock
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.library.newproject.CreateResult
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
import io.github.lenticualrstone.civilight.navigation.BackStackNavigator
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.NewProject
import io.github.lenticualrstone.civilight.navigation.NewProjectStep
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.WorldCalendar
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
    fun `단편 picked in step 1 makes step 2 start on 씬 only`() {
        val short = NewProjectRules.withFormat(NewProjectForm(name = "유리 정원"), ProjectFormat.SHORT)
        assertEquals(ProjectFormat.SHORT, short.format)
        assertEquals(StructurePreset.CUSTOM, short.structurePreset)
        assertEquals(listOf("씬"), NewProjectRules.levels(short))
        assertEquals(listOf(0 to "씬 1", 0 to "씬 2"), NewProjectRules.preview(short))
        assertTrue(NewProjectRules.canProceed(short, NewProjectStep.STRUCTURE))
        assertEquals(listOf("씬"), NewProjectRules.toDraft(short).levels)
        // Back to 연재 장편 / 설정만 while untouched: their own default again, nothing left over.
        val serial = NewProjectRules.withFormat(short, ProjectFormat.SERIAL)
        assertEquals(NewProjectForm(name = "유리 정원"), serial)
        val setting = NewProjectRules.withFormat(short, ProjectFormat.SETTING_ONLY)
        assertEquals(StructurePreset.CHAPTER_EPISODE_SCENE, setting.structurePreset)
        assertEquals(listOf("장", "에피소드", "씬"), NewProjectRules.levels(setting))
        // Re-tapping the selected 장 › 에피소드 › 씬 card is not a manual change.
        val retapped = NewProjectRules.withPreset(NewProjectForm(name = "x"), StructurePreset.CHAPTER_EPISODE_SCENE)
        assertEquals(listOf("씬"), NewProjectRules.levels(NewProjectRules.withFormat(retapped, ProjectFormat.SHORT)))
        // Step 2 on 단편: 직접 정하기 with the one level, which cannot be removed.
        assertFalse(NewProjectRules.canRemoveLevel(short))
    }

    @Test
    fun `a structure picked by hand survives a format change`() {
        val act = NewProjectRules.withPreset(NewProjectForm(name = "x"), StructurePreset.ACT_SCENE)
        val short = NewProjectRules.withFormat(act, ProjectFormat.SHORT)
        assertEquals(ProjectFormat.SHORT, short.format)
        assertEquals(StructurePreset.ACT_SCENE, short.structurePreset)
        // Edited 단편 levels stay when the format changes again.
        val edited = NewProjectRules.addLevel(NewProjectRules.withFormat(NewProjectForm(name = "x"), ProjectFormat.SHORT))
        val named = NewProjectRules.setLevel(edited, 1, "컷")
        val serial = NewProjectRules.withFormat(named, ProjectFormat.SERIAL)
        assertEquals(StructurePreset.CUSTOM, serial.structurePreset)
        assertEquals(listOf("씬", "컷"), serial.customLevels)
        // The flag survives process death with the draft.
        assertEquals(serial, NewProjectDraftHolder.decode(NewProjectDraftHolder.encode(serial)))
    }

    @Test
    fun `만들기 after 단편 creates a scene-only project`() {
        val doc = LibFx.created(NewProjectRules.withFormat(NewProjectForm(name = "유리 정원"), ProjectFormat.SHORT))
        assertEquals(ProjectFormat.SHORT, doc.project.format.known)
        assertEquals(StructurePreset.CUSTOM, doc.project.structure.preset.known)
        assertEquals(listOf("씬"), doc.project.structure.levels)
        assertEquals(1, doc.story.units.size)
    }

    @Test
    fun `step 3 year takes digits only and 시간 없이 drops it`() {
        val form = NewProjectRules.withYear(NewProjectForm(name = "x"), "12a00년")
        assertEquals("1200", form.startYear)
        assertEquals(1200, NewProjectRules.toDraft(form).startYear)
        assertNull(NewProjectRules.toDraft(form.copy(timeMode = WorldTimeMode.NONE)).startYear)
    }

    @Test
    fun `R-06 step 4 hide switch starts off and the author's choice is final (R19-11 to R19-13 rating parts no longer apply)`() {
        // (0.2.0, LIB-92) No rating to derive it from: off, whatever the stored defaults say.
        assertFalse(NewProjectRules.hideSwitch(NewProjectForm()))
        assertFalse(NewProjectRules.hideSwitch(NewProjectForm(rating = Rating.R18)))
        assertTrue(NewProjectRules.hideSwitch(NewProjectForm(step4Hide = true)))
        assertFalse(NewProjectRules.hideSwitch(NewProjectForm(step4Hide = false)))
        // G-01: a new project is `all` with every protection off.
        val plain = LibFx.created(NewProjectForm(name = "새 글")).project
        assertEquals(Rating.ALL, plain.rating.known)
        assertEquals(Protections(), plain.protections)
        // A project built from a stored R-18 form (the creator's own rule, kept while the code exists):

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

    private class FakePorts(var recorder: RecorderLibrary, val secure: Boolean = true) : CreatorPorts {
        val added = mutableListOf<ProjectDoc>()
        val keptOpen = mutableListOf<String>()

        override fun deviceSecure(): Boolean = secure

        override fun keepOpen(projectKey: String) {
            keptOpen += projectKey
        }

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
        assertEquals("a project created hidden stays open until the next re-lock (R19 §2)", listOf(result.projectKey), ports.keptOpen)
    }

    @Test
    fun `Q19 a device without a lock creates the project visible and says so`() = runTest {
        val ports = FakePorts(RecorderLibrary(), secure = false)
        val result = ProjectCreator.create(ports, NewProjectForm(name = "검은 서약", rating = Rating.R18), defaults, LibFx.ctx(), null)
        assertFalse(result.hidden)
        assertTrue(result.hidingRefused)
        assertFalse(ports.added.single().project.protections.hidden)
        assertTrue(ports.keptOpen.isEmpty())
    }

    @Test
    fun `after 만들기 back from the new project's home goes to 서재 - no step stays below`() {
        val start = listOf(Library, NewProject(NewProjectStep.NAME), NewProject(NewProjectStep.STRUCTURE), NewProject(NewProjectStep.TIME), NewProject(NewProjectStep.START))
        val stack = start.toMutableList<NavKey>()
        val navigator = BackStackNavigator(stack)
        navigator.resetTo(ProjectCreator.stackAfter(CreateResult("01KTESTPROJECT00000000000001", hidden = false, opensCalendar = false)))
        assertEquals(listOf(Library, ProjectHome("01KTESTPROJECT00000000000001")), stack)
        assertTrue(navigator.pop())
        assertEquals(listOf<NavKey>(Library), stack)

        val withCalendar = start.toMutableList<NavKey>()
        BackStackNavigator(withCalendar).resetTo(ProjectCreator.stackAfter(CreateResult("01KTESTPROJECT00000000000002", hidden = true, opensCalendar = true)))
        assertEquals(listOf(Library, ProjectHome("01KTESTPROJECT00000000000002"), WorldCalendar("01KTESTPROJECT00000000000002")), withCalendar)
    }
}
