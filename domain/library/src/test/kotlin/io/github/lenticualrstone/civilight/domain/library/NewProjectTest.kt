package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.newproject.ChecklistItem
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectDraft
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectFlow
import io.github.lenticualrstone.civilight.domain.library.newproject.Onboarding
import io.github.lenticualrstone.civilight.domain.library.newproject.ReadOnly
import io.github.lenticualrstone.civilight.domain.library.newproject.RowMeta
import io.github.lenticualrstone.civilight.domain.library.newproject.StartBlock
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 60-library §4.9 new project and onboarding cases P-01 … P-12. */
class NewProjectTest {
    private val defaults = RatingDefaults()

    private fun create(draft: NewProjectDraft, seed: Long = 1) =
        NewProjectFlow.create(draft, defaults, Fixtures.NOW, Fixtures.DEVICE, Fixtures.keys(seed))

    private fun record(n: Int, text: String, createdAt: String, done: Boolean = false) = RecorderRecord(
        key = Fixtures.keys(100L + n)(), kind = RecordKind.TEXT.open(), createdAt = Fixtures.at(createdAt),
        runs = listOf(TextRun.Plain(text)), doneMarked = done,
    )

    @Test
    fun `P-01 a blank first scene title gives 1장 · 에피소드 1-1 · 씬 1`() {
        val created = create(NewProjectDraft(name = "새 이야기", firstSceneTitle = "  "))
        val doc = created.doc
        val units = doc.story.units
        assertEquals(listOf(0, 1, 2), units.map { it.level })
        val (chapter, episode, scene) = units
        assertEquals(chapter.key, episode.parentKey)
        assertEquals(episode.key, scene.parentKey)
        val codes = UnitCodes(doc)
        assertEquals("1", codes.code(chapter.key))
        assertEquals("1-1", codes.code(episode.key))
        assertEquals("1-1 씬 1", codes.sceneLabel(scene.key))
        // No typed title: the scene shows its label `씬 1` (the stored title stays empty, see status file).
        assertNull(scene.title)
        assertEquals("1장 · 에피소드 1-1 · 씬 1", StartBlock.quickWriteLine(doc))
        assertEquals("1장 · 에피소드 1-1 · 씬 1", StartBlock.of(doc, RecorderLibrary()).quickWrite)
        // Numbering unit, counter and travel modes are ready.
        assertEquals(1, episode.nextBlockNumber)
        assertTrue(episode.key in doc.episodes)
        assertEquals(3, doc.project.travelModes.size)
        assertEquals(StructurePreset.CHAPTER_EPISODE_SCENE, doc.project.structure.preset.known)
        // A typed title is used as is.
        val titled = create(NewProjectDraft(name = "새 이야기", firstSceneTitle = "꺼진 등불"), seed = 2)
        assertEquals("1장 · 에피소드 1-1 · 꺼진 등불", StartBlock.quickWriteLine(titled.doc))
    }

    @Test
    fun `P-02 a protagonist is created and checks the first item`() {
        val created = create(NewProjectDraft(name = "새 이야기", protagonist = " 리엔 "))
        val cast = created.doc.cast.characters
        assertEquals(listOf("리엔"), cast.map { it.name })
        assertEquals(1, cast.single().recordNo)
        assertEquals(2, created.doc.project.counters.nextCharacterRecordNo)
        val block = StartBlock.of(created.doc, RecorderLibrary())
        val first = block.items.first()
        assertEquals(ChecklistItem.Id.PROTAGONIST, first.id)
        assertEquals("주인공 만들기", first.title)
        assertEquals("리엔 · 완료", first.sub)
        assertTrue(first.done)
        assertEquals("시작해 볼 것 · 1/4", block.header)
        assertEquals("목록 닫기", block.dismiss)
        // Without a protagonist: no character, item open.
        val none = create(NewProjectDraft(name = "새 이야기"), seed = 3)
        assertTrue(none.doc.cast.characters.isEmpty())
        assertFalse(StartBlock.of(none.doc, RecorderLibrary()).items.first().done)
        assertEquals("시작해 볼 것 · 0/4", StartBlock.of(none.doc, RecorderLibrary()).header)
    }

    @Test
    fun `P-03 checked records get a link and stay where they are`() {
        val records = listOf(
            record(1, "첫 기록 · 비 오는 관문", "2026-11-20T01:00:00Z"),
            record(2, "둘째 기록 · 리엔의 등불", "2026-11-21T01:00:00Z"),
            record(3, "셋째 기록 · 시장의 밤", "2026-11-22T00:30:00Z"),
            record(4, "끝낸 기록", "2026-11-22T00:40:00Z", done = true),
        )
        val recorder = RecorderLibrary(records)
        val candidates = NewProjectFlow.startRecordCandidates(recorder)
        assertEquals(listOf(records[2], records[1], records[0]).map { it.key }, candidates.map { it.key })

        val checked = listOf(records[2].key, records[0].key)
        val created = create(NewProjectDraft(name = "새 이야기", linkedRecordKeys = checked))
        assertEquals(checked, created.recorderLinks.map { it.recordKey })
        for (link in created.recorderLinks) {
            assertEquals(created.doc.key, link.projectKey)
            assertEquals(RecorderTargetKind.PROJECT, link.targetKind.known)
            assertEquals(created.doc.key, link.targetKey)
            assertEquals(LinkOrigin.START_MATERIAL, link.origin.known)
        }
        // The record text is not inserted anywhere in the project.
        assertTrue(created.doc.allBlocks.isEmpty())
        val encoded = ProjectFileSet.split(created.doc).encode().values.joinToString("") { it.toString() }
        assertFalse(encoded.contains("첫 기록"))
        assertFalse(encoded.contains("셋째 기록"))

        val after = recorder.copy(links = recorder.links + created.recorderLinks)
        val item = StartBlock.of(created.doc, after).items.single { it.id == ChecklistItem.Id.LINKED_RECORDS }
        assertEquals("연결된 기록 2개 정리하기", item.title)
        assertEquals("기록함", item.sub)
        assertFalse(item.done)
        // With no linked record the item is not shown.
        assertTrue(StartBlock.of(created.doc, recorder).items.none { it.id == ChecklistItem.Id.LINKED_RECORDS })
    }

    @Test
    fun `P-04 the bundle is 기본 whatever the format`() {
        for (format in ProjectFormat.entries) {
            assertEquals(FieldBundle.BASIC, NewProjectDraft(name = "x", format = format).bundle)
        }
        val created = create(NewProjectDraft(name = "검은 서약", format = ProjectFormat.SETTING_ONLY))
        assertEquals(FieldBundle.BASIC, created.doc.project.fieldBundleAtCreation.known)
        // 설정만 creates no manuscript.
        assertTrue(created.doc.story.units.isEmpty())
        assertTrue(created.doc.episodes.isEmpty())
        assertNull(StartBlock.quickWriteLine(created.doc))
        assertEquals("기본", NewProjectFlow.bundleLabel(FieldBundle.BASIC))
        assertEquals("다른 프로젝트에서", NewProjectFlow.bundleLabel(FieldBundle.FROM_PROJECT))
    }

    @Test
    fun `P-05 다른 프로젝트에서 lists no hidden project while the group is locked`() {
        val ash = ProjectProfile.of(Fixtures.example())
        val glass = ProjectProfile.of(Fixtures.otherProject("유리 정원", Rating.ALL))
        val black = ProjectProfile.of(Fixtures.otherProject("검은 서약 (가제)", Rating.R18, Protections(true, true, true, true)))
        val side = ProjectProfile.of(Fixtures.otherProject("외전 · 오웬의 시장", Rating.R15, Protections(hidden = true)))
        val all = listOf(ash, glass, black, side)
        assertEquals(listOf(ash, glass), NewProjectFlow.bundleSources(all, HiddenAccess()))
        assertEquals(all, NewProjectFlow.bundleSources(all, HiddenAccess().openGroup(Fixtures.NOW)))
        // Trashed and example projects are never sources.
        assertEquals(listOf(glass), NewProjectFlow.bundleSources(listOf(ash.copy(isExample = true), glass, glass.copy(key = "x", trashed = true)), HiddenAccess()))
    }

    @Test
    fun `P-06 a blank name keeps 다음 disabled`() {
        assertFalse(NewProjectFlow.canProceedFromName("  "))
        assertFalse(NewProjectFlow.canProceedFromName(""))
        assertTrue(NewProjectFlow.canProceedFromName(" 유리 정원 "))
        val failed = runCatching { create(NewProjectDraft(name = "  ")) }
        assertTrue(failed.isFailure)
        // Names are trimmed and NFC-normalised on create.
        val nfd = java.text.Normalizer.normalize("잿빛", java.text.Normalizer.Form.NFD)
        assertEquals("잿빛", create(NewProjectDraft(name = " $nfd ")).doc.project.name)
    }

    @Test
    fun `P-07 온보딩 끝내기 deletes the example for good`() {
        val example = ExampleProject.libraryEntry()
        val state = OnboardingState(exampleState = ExampleState.INSTALLED, resumePage = "example")
        val recorder = Fixtures.recorder()
        assertTrue(recorder.links.any { it.projectKey == example.key })
        val finish = Onboarding.finish(state, listOf(example), recorder)
        assertTrue(finish.state.firstRunCompleted)
        assertEquals(ExampleState.DELETED, finish.state.exampleState)
        assertNull(finish.state.resumePage)
        assertTrue(finish.library.isEmpty())
        assertEquals(example, finish.purgedExample)
        // Purged, not trashed: nothing in the library carries a deletion mark; recorder links into it go.
        assertTrue(finish.recorder.links.none { it.projectKey == example.key })
        assertTrue(finish.recorder.placements.none { it.projectKey == example.key })
        assertEquals(recorder.records, finish.recorder.records)

        val empty = Onboarding.emptyLibrary(finish.state, finish.library)!!
        assertEquals("프로젝트 · 0", empty.section)
        assertEquals("프로젝트 없음", empty.title)
        assertEquals("프로젝트 = 원고 · 인물 · 세계 · 페이지 한 묶음", empty.sub)
        assertEquals(listOf("새 프로젝트", "백업에서 되살리기"), empty.buttons)
        assertEquals("예시 프로젝트 삭제됨", empty.notice)
        // Shown once: gone after the notice was seen, and no empty state once a project exists.
        assertNull(Onboarding.emptyLibrary(finish.state.copy(emptyLibraryNoticeShown = true), finish.library)!!.notice)
        assertNull(Onboarding.emptyLibrary(finish.state, listOf(Fixtures.entry(Fixtures.otherProject("유리 정원", Rating.ALL)))))
    }

    @Test
    fun `P-08 the example project rejects every write`() {
        var ran = false
        val result = ReadOnly.guard(ExampleProject.libraryEntry()) { ran = true }
        assertEquals(ReadOnly.Write.Rejected, result)
        assertFalse(ran)
        assertFalse(ReadOnly.canWrite(ExampleProject.libraryEntry()))
        val own = Fixtures.entry(Fixtures.otherProject("유리 정원", Rating.ALL))
        assertEquals(ReadOnly.Write.Done(3), ReadOnly.guard(own) { 3 })
    }

    @Test
    fun `P-09 row meta of 잿빛`() {
        // 12 characters: 노아 back from the trash (fixture.md conflict 7: the base has 11 live).
        val doc = Fixtures.withNoahLive()
        assertEquals("1장 7/10 · 인물 12 · 오늘 08:12", RowMeta.of(doc, ExampleClock.LAST_EDIT, Fixtures.NOW, Fixtures.ZONE))
        assertEquals("1장 7/10", RowMeta.position(doc))
    }

    @Test
    fun `P-10 row meta of 유리 정원`() {
        val glass = Fixtures.projectWith("유리 정원", Rating.ALL, format = ProjectFormat.SHORT, scenes = 6, characters = 4)
        assertEquals("단편 · 씬 6개 · 3일 전", RowMeta.of(glass, Fixtures.at("2026-11-19T03:00:00Z"), Fixtures.NOW, Fixtures.ZONE))
    }

    @Test
    fun `P-11 row meta of 검은 서약`() {
        val black = Fixtures.projectWith(
            "검은 서약 (가제)", Rating.R18, Protections(true, true, true, true), format = ProjectFormat.SETTING_ONLY, characters = 5,
        )
        assertEquals("설정만 · 인물 5 · 11월 2일", RowMeta.of(black, Fixtures.at("2026-11-02T03:00:00Z"), Fixtures.NOW, Fixtures.ZONE))
    }

    @Test
    fun `P-12 relative time words`() {
        fun rel(iso: String) = TimeLabels.relative(Fixtures.at(iso), Fixtures.NOW, Fixtures.ZONE)
        assertEquals("어제", rel("2026-11-21T03:00:00Z"))
        assertEquals("4일 전", rel("2026-11-18T03:00:00Z"))
        assertEquals("11월 15일", rel("2026-11-15T03:00:00Z"))
        assertEquals("오늘 08:12", rel("2026-11-21T23:12:00Z"))
        assertEquals("6일 전", rel("2026-11-16T03:00:00Z"))
        assertEquals("2025년 12월 30일", rel("2025-12-30T03:00:00Z"))
    }

    @Test
    fun `step 2 preview trees`() {
        assertEquals(
            listOf(0 to "장 1", 1 to "에피소드 1-1", 2 to "씬 1", 2 to "씬 2", 1 to "에피소드 1-2"),
            NewProjectFlow.previewTree(listOf("장", "에피소드", "씬")),
        )
        assertEquals(listOf(0 to "막 1", 1 to "장면 1", 1 to "장면 2"), NewProjectFlow.previewTree(listOf("막", "장면")))
        assertTrue(NewProjectFlow.levelsValid(listOf("권", "장", "절", "씬")))
        assertFalse(NewProjectFlow.levelsValid(listOf("권", "장", "절", "씬", "줄")))
        assertFalse(NewProjectFlow.levelsValid(listOf("장", " ")))
    }

    @Test
    fun `a 2-level and a 1-level structure`() {
        val act = create(NewProjectDraft(name = "막극", structurePreset = StructurePreset.ACT_SCENE))
        assertEquals(listOf("막", "장면"), act.doc.project.structure.levels)
        assertEquals("1막 · 장면 1", StartBlock.quickWriteLine(act.doc))
        val oneLevel = create(NewProjectDraft(name = "짧은 것", structurePreset = StructurePreset.CUSTOM, customLevels = listOf("씬")), seed = 4)
        assertEquals(1, oneLevel.doc.story.units.size)
        // 1-level projects number blocks on the project (§4.1).
        assertTrue(oneLevel.doc.key in oneLevel.doc.episodes)
        assertEquals(1, oneLevel.doc.project.counters.nextBlockNumber)
    }

    @Test
    fun `step 2 default follows the format - 단편 is scenes only (Q10)`() {
        assertEquals(StructurePreset.CUSTOM to listOf("씬"), NewProjectFlow.defaultStructure(ProjectFormat.SHORT))
        assertEquals(StructurePreset.CHAPTER_EPISODE_SCENE to listOf("장", "에피소드", "씬"), NewProjectFlow.defaultStructure(ProjectFormat.SERIAL))
        assertEquals(StructurePreset.CHAPTER_EPISODE_SCENE to listOf("장", "에피소드", "씬"), NewProjectFlow.defaultStructure(ProjectFormat.SETTING_ONLY))
        val (preset, levels) = NewProjectFlow.defaultStructure(ProjectFormat.SHORT)
        val short = create(
            NewProjectDraft(name = "유리 정원", format = ProjectFormat.SHORT, structurePreset = preset, customLevels = levels, startYear = 1200), seed = 5,
        )
        assertEquals(listOf("씬"), short.doc.project.structure.levels)
        assertEquals(listOf(0), short.doc.story.units.map { it.level })
        assertNull(short.doc.project.structure.episodeLevel)
        // LIB-13: no chapter or episode left in the summary or the quick-write line.
        assertEquals("단편 · 씬 · 1200년", StartBlock.of(short.doc, RecorderLibrary()).summary)
        assertEquals("씬 1", StartBlock.quickWriteLine(short.doc))
    }

    @Test
    fun `summary line of the 만든 직후 block`() {
        val serial = create(NewProjectDraft(name = "새 이야기", startYear = 1200))
        assertEquals("연재 장편 · 장 › 에피소드 › 씬 · 1200년", StartBlock.of(serial.doc, RecorderLibrary()).summary)
        val r18 = NewProjectFlow.create(
            NewProjectDraft(name = "검은 서약", rating = Rating.R18, startYear = 1200, step4Hide = false), defaults, Fixtures.NOW, Fixtures.DEVICE, Fixtures.keys(5),
        )
        assertEquals("연재 장편 · 장 › 에피소드 › 씬 · 1200년 · 숨김 · 끔", StartBlock.of(r18.doc, RecorderLibrary(), r18.protections).summary)
        assertFalse(r18.doc.project.protections.hidden)
        assertTrue(r18.doc.project.protections.captureBlock)
        val hidden = NewProjectFlow.create(NewProjectDraft(name = "검은 서약", rating = Rating.R18), defaults, Fixtures.NOW, Fixtures.DEVICE, Fixtures.keys(6))
        assertTrue(hidden.keepOpenUntilRelock)
        assertTrue(hidden.entry.hidden)
        val noTime = create(NewProjectDraft(name = "시간 없는", timeMode = WorldTimeMode.NONE, startYear = 1200), seed = 7)
        assertNull(noTime.doc.project.worldTime.storyStartYear)
        assertEquals("연재 장편 · 장 › 에피소드 › 씬", StartBlock.of(noTime.doc, RecorderLibrary()).summary)
    }

    @Test
    fun `checklist items complete by fact`() {
        val created = create(NewProjectDraft(name = "새 이야기"))
        val doc = created.doc
        val scene = doc.story.units.last()
        val line = Fixtures.narration(doc, scene.key, 1, "대사", Fixtures.keys(40)()).copy(
            kind = io.github.lenticualrstone.civilight.model.script.BlockKind.DIALOGUE.open(),
        )
        val withLine = Fixtures.withBlocks(doc, listOf(line))
        val term = io.github.lenticualrstone.civilight.model.world.Term(key = Fixtures.keys(41)(), order = OrderKeys.between(null, null), name = "오리지늄")
        val withTerm = withLine.copy(world = withLine.world.copy(terms = listOf(term)))
        val items = StartBlock.of(withTerm, RecorderLibrary()).items.associateBy { it.id }
        assertTrue(items.getValue(ChecklistItem.Id.FIRST_LINE).done)
        assertTrue(items.getValue(ChecklistItem.Id.TERM).done)
        assertNotNull(items[ChecklistItem.Id.PROTAGONIST])
    }

    @Test
    fun `the bundled example installs read-only and a file never does`() {
        val dir = java.io.File(requireNotNull(javaClass.classLoader.getResource("format/1.0")).toURI())
        val asset = dir.listFiles().orEmpty().single { it.name.endsWith(".civilight") }.readBytes()
        val plan = io.github.lenticualrstone.civilight.domain.library.importing.ProjectImports.installExample(
            Fixtures.open(asset), RecorderLibrary(), Fixtures.NOW, Fixtures.keys(70),
        )
        assertTrue(plan.entry.isExample)
        assertTrue(plan.entry.readOnly)
        assertEquals(ReadOnly.Write.Rejected, ReadOnly.guard(plan.entry) { 1 })
        val opened = io.github.lenticualrstone.civilight.domain.library.importing.ProjectImports.direct(
            Fixtures.open(asset), RecorderLibrary(), Fixtures.NOW, Fixtures.keys(71),
        )
        assertFalse(opened.entry.isExample)
        assertFalse(opened.entry.readOnly)
    }

    @Test
    fun `project menu and delete confirm`() {
        val glass = Fixtures.projectWith("유리 정원", Rating.ALL, format = ProjectFormat.SHORT, scenes = 6, characters = 4).let { d ->
            d.copy(pages = (1..3).associate { n ->
                val key = Fixtures.keys(80L + n)()
                key to io.github.lenticualrstone.civilight.model.canvas.Page(key = key, order = OrderKeys.evenlySpaced(3)[n - 1], name = "페이지 $n")
            })
        }
        val menu = io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenus.of(glass)
        assertEquals("유리 정원", menu.title)
        assertEquals("ALL", menu.ratingBadge)
        assertEquals("단편 · 씬 6개 · 인물 4 · 페이지 3", menu.sub)
        assertEquals(
            listOf(
                "열기" to "마지막 위치", "이름 바꾸기" to null, "수위 등급" to "ALL(R-12) · R-15 · R-18 · 지금 ALL",
                "프로젝트 숨기기" to "프로젝트 전체 · 지문 · PIN으로 열림", "백업 · 내보내기" to "Google Drive · 파일", "지우기" to "휴지통 · 30일 뒤 삭제",
            ),
            menu.rows.map { it.title to it.sub },
        )
        val hidden = Fixtures.withProtections(glass, Rating.R15, Protections(hidden = true))
        assertTrue(io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenus.of(hidden).rows.any { it.title == "숨김 풀기" })
        assertEquals(listOf("열기"), io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenus.of(Fixtures.example(), isExample = true).rows.map { it.title })
        assertNull(io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenus.rename("   "))
        assertEquals("유리 정원 2", io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenus.rename(" 유리 정원 2 "))

        val dialog = io.github.lenticualrstone.civilight.domain.library.newproject.ProjectDeleteDialog.of(glass)
        assertEquals("‘유리 정원’ 지우기", dialog.title)
        assertEquals(listOf("씬 · 인물 · 페이지" to "6 · 4 · 3", "이동" to "휴지통 · 30일 보관"), dialog.rows)
        assertEquals("확인: 프로젝트 이름 입력", dialog.fieldLabel)
        assertEquals(listOf("취소", "휴지통으로"), dialog.buttons)
        assertTrue(dialog.canConfirm(" 유리 정원 "))
        assertTrue(dialog.canConfirm(java.text.Normalizer.normalize("유리 정원", java.text.Normalizer.Form.NFD)))
        assertFalse(dialog.canConfirm("유리정원"))
        assertFalse(dialog.canConfirm("유리 정"))
    }
}
