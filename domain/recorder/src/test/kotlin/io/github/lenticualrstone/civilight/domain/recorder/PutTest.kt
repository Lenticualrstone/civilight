package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.K
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxFilters
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxOpening
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordScreens
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.target
import io.github.lenticualrstone.civilight.domain.recorder.put.CardDocs
import io.github.lenticualrstone.civilight.domain.recorder.put.CharacterInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.DestinationStep
import io.github.lenticualrstone.civilight.domain.recorder.put.Destinations
import io.github.lenticualrstone.civilight.domain.recorder.put.Insertion
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.MemoFieldInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.PageInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.PositionRow
import io.github.lenticualrstone.civilight.domain.recorder.put.PositionSheets
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.domain.recorder.put.SourceMarks
import io.github.lenticualrstone.civilight.domain.recorder.put.Undo
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.docText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/** LRC §4.4 · 넣기. */
class PutTest {
    private val env = Fx.env()

    private fun sheetRows(library: RecorderLibrary, env: RecorderEnv = this.env) =
        PositionSheets.of(library, Fx.R1, Fx.P1, env).rows.map { it.label }

    /** LRC-T17's action: `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤` tapped. */
    private fun insertAfter044(doc: ProjectDoc = Fx.p1Doc(), library: RecorderLibrary = Fx.openLibrary()): Insertion {
        val row = PositionSheets.of(library, Fx.R1, Fx.P1, env.withDoc(doc)).rows.filterIsInstance<PositionRow.LastWriting>().single()
        return ManuscriptInsert.insert(doc, library, Fx.R1, row.spot!!, Fx.ctx())
    }

    @Test
    fun LRC_T16_positionSheetRows() {
        val library = Fx.openLibrary()
        assertEquals(Fx.P1, PositionSheets.initialProject(library, Fx.R1, env))
        val sheet = PositionSheets.of(library, Fx.R1, Fx.P1, env)
        assertEquals("원고에 넣기 · 잿빛 새벽의 등불 ▾", sheet.title)
        assertEquals(listOf("연결된 씬 · 1-7 씬 3 · 맨 끝", "마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤", "씬 고르기 ›"), sheet.rows.map { it.label })
        assertEquals(library, Fx.openLibrary())
    }

    // LRC-T17 … T20 as stated, with the number as a parameter: the LRC fixture's 1-7 counter had
    // issued #061 (next #062); the example fixture's counter is 58 (fixture.md conflict 3).

    private fun num(n: Int) = "#%03d".format(n)

    private fun t17(expected: Int) {
        val library = Fx.openLibrary()
        val ins = insertAfter044(library = library)
        val doc = ins.doc
        val block = doc.index.block(ins.placement.targetKey)!!
        assertEquals(BlockKind.MEMO, block.kind.known)
        assertEquals(expected, block.number)
        assertEquals(Fx.S, block.sceneKey)
        val sceneBlocks = doc.index.blocksOfScene(Fx.S).filter { doc.liveness.isLive(it) }.map { it.key }
        assertEquals(K.BLOCK_044, sceneBlocks[sceneBlocks.indexOf(block.key) - 1])
        assertEquals(K.BLOCK_045, sceneBlocks[sceneBlocks.indexOf(block.key) + 1])
        assertEquals(ExampleText.R1, Runs.text(block.runs))
        val r1 = library.record(Fx.R1)!!
        assertEquals(RecorderSource(Fx.R1, r1.createdAt), block.recorderSource)
        assertEquals(Fx.at("2026-11-21T23:41:00Z"), block.recorderSource!!.recordedAt)
        assertEquals("⌁ 작가 메모 · 레코더 11월 22일 08:41", SourceMarks.blockMemoPrefix(block.recorderSource!!, ins.library, Fx.ZONE))
        assertEquals(PlacementKind.SCRIPT_BLOCK, ins.placement.kind.known)
        assertEquals("1-7 씬 3 · ${num(expected)}", ins.placement.lastLabel)
        assertEquals(1, ins.placement.copiedRevision)
        assertEquals(library.linksOf(Fx.R1), ins.library.linksOf(Fx.R1))
        assertEquals(RecordState.PUT, RecordStates.state(ins.library, ins.library.record(Fx.R1)!!, env.withDoc(doc)))
        assertEquals("1-7 씬 3 · ${num(expected)} 작가 메모 넣음", ins.toast.text)
        assertEquals(listOf("되돌리기", "가 보기"), ins.toast.actions)
        assertEquals(expected + 1, doc.index.unit(K.EPISODE_1_7)!!.nextBlockNumber)
    }

    @Test
    @Ignore("spec conflict: LRC fixture's 1-7 counter gives #062; the example fixture's counter gives #058 (fixture.md conflict 3)")
    fun LRC_T17_blockNumberAndToast() = t17(62)

    @Test
    fun LRC_T17_fixture_blockNumberAndToast() = t17(58)

    private fun t18(expected: Int) {
        assertTrue(sheetRows(Fx.openLibrary()).none { it.startsWith("이미 넣음") })
        val ins = insertAfter044()
        assertEquals(
            listOf("연결된 씬 · 1-7 씬 3 · 맨 끝", "마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤", "씬 고르기 ›", "이미 넣음 · 1-7 씬 3 · ${num(expected)}"),
            sheetRows(ins.library, env.withDoc(ins.doc)),
        )
    }

    @Test
    @Ignore("spec conflict: expects #062; the example fixture's 1-7 counter gives #058 (fixture.md conflict 3)")
    fun LRC_T18_alreadyPutRow() = t18(62)

    @Test
    fun LRC_T18_fixture_alreadyPutRow() = t18(58)

    private fun t19(expectedNext: Int) {
        val ins = insertAfter044()
        val (doc, library) = Undo.undo(ins.doc, ins.library, ins.undo)
        assertNull(doc.index.block(ins.placement.targetKey))
        assertTrue(doc.trash.entries.none { ins.placement.targetKey in it.targetKeys })
        assertTrue(library.placementsOf(Fx.R1).isEmpty())
        assertEquals(RecordState.UNSORTED, RecordStates.state(library, library.record(Fx.R1)!!, env.withDoc(doc)))
        assertEquals(expectedNext, ManuscriptInsert.nextNumber(doc, K.EPISODE_1_7))
        assertEquals(Fx.openLibrary().linksOf(Fx.R1), library.linksOf(Fx.R1))
    }

    @Test
    @Ignore("spec conflict: expects #063 next after undoing #062; the example fixture gives #059 after #058 (fixture.md conflict 3)")
    fun LRC_T19_undo() = t19(63)

    @Test
    fun LRC_T19_fixture_undo() = t19(59)

    private fun putHere(): Insertion {
        val ctx = Fx.ctx(start = 500)
        val doc = Fx.p1Doc()
        val entry = EntryLinks.script(Fx.P1, Fx.S, K.BLOCK_044)
        var s = Recording.start("01JZ000000TSTK000000000400", entry, RecorderSettings(), env, "tmp/a.m4a", ctx)
        s = Recording.final(Recording.elapse(s, 24_000), ExampleText.R1)
        val (processing, library) = Recording.stopAndSave(s, RecorderLibrary(), ctx, startedAt = Fx.at("2026-11-21T23:41:00Z"))
        val screen = Recording.processingScreen(processing, env, finalising = false, audioKept = true)
        assertEquals("여기 넣기 · #044 뒤", screen.putHere)
        assertEquals(listOf("이어서 말하기", "기록함에 두기"), screen.buttons)
        assertEquals(doc, env.project(Fx.P1)!!.doc)
        return Recording.putHere(processing, library, env, ctx)
    }

    private fun t20(expected: Int) {
        val ins = putHere()
        val block = ins.doc.index.block(ins.placement.targetKey)!!
        val expectedIns = insertAfter044()
        val expectedBlock = expectedIns.doc.index.block(expectedIns.placement.targetKey)!!
        assertEquals(expected, block.number)
        assertEquals(expectedBlock.number, block.number)
        assertEquals(expectedBlock.order, block.order)
        assertEquals(expectedBlock.kind, block.kind)
        assertEquals(Runs.text(expectedBlock.runs), Runs.text(block.runs))
        assertEquals("1-7 씬 3 · ${num(expected)}", ins.placement.lastLabel)
        assertEquals(expectedIns.toast, ins.toast)
        assertEquals(RecordState.PUT, RecordStates.state(ins.library, ins.library.record(ins.placement.recordKey)!!, env.withDoc(ins.doc)))

        for (entry in listOf(
            EntryLinks.project(EntrySurface.PROJECT_HOME, Fx.P1),
            EntryLinks.inbox(ProjectFilter.All),
            EntryLinks.outside(EntrySurface.HOME_WIDGET),
            EntryLinks.outside(EntrySurface.QS_TILE),
        )) {
            var s = Recording.start("01JZ000000TSTK000000000401", entry, RecorderSettings(), env, "tmp/b.m4a", Fx.ctx())
            s = Recording.stopAndSave(Recording.final(s, "메모"), RecorderLibrary(), Fx.ctx()).first
            assertNull(Recording.processingScreen(s, env, finalising = false, audioKept = true).putHere)
        }
    }

    @Test
    @Ignore("spec conflict: the result 'equals LRC-T17' (#062); the example fixture gives #058 (fixture.md conflict 3)")
    fun LRC_T20_putHereOnlyFromScript() = t20(62)

    @Test
    fun LRC_T20_fixture_putHereOnlyFromScript() = t20(58)

    @Test
    fun LRC_T21_insertAddsLinkWhenMissing() {
        val library = Fx.openLibrary()
        val r3 = library.record(Fx.R3)!!
        assertEquals("2장 첫 장면은 비 오는 상층 관문에서 시작", PageInsert.prefillName(r3))
        val ins = PageInsert.insert(Fx.p1Doc(), library, Fx.R3, PageInsert.prefillName(r3), Fx.ctx())
        val page = ins.doc.pages[ins.placement.targetKey]!!
        assertEquals("2장 첫 장면은 비 오는 상층 관문에서 시작", page.name)
        assertEquals(ExampleText.R3, page.blocks.docText)
        assertEquals(RecorderSource(Fx.R3, r3.createdAt), page.sourceRecord)
        assertEquals(PlacementKind.PAGE, ins.placement.kind.known)
        val pageTarget = TargetRef(Fx.P1, RecorderTargetKind.PAGE, page.key)
        assertEquals(listOf(pageTarget to LinkOrigin.INSERT), ins.library.linksOf(Fx.R3).map { it.target to it.origin.known })
        val after = env.withDoc(ins.doc)
        assertEquals(setOf(Fx.P1), RecordStates.impliedProjects(ins.library, Fx.R3, after))
        assertEquals(RecordState.PUT, RecordStates.state(ins.library, ins.library.record(Fx.R3)!!, after))
    }

    @Test
    fun LRC_T22_memoFieldIntoChapter2() {
        val doc = Fx.p1Doc()
        val before = doc.index.unit(Fx.C2)!!.synopsis
        val ins = MemoFieldInsert.insert(doc, Fx.openLibrary(), Fx.R3, Fx.C2, Fx.ctx())
        val after = ins.doc.index.unit(Fx.C2)!!.synopsis!!
        assertTrue(after.endsWith(ExampleText.R3))
        assertFalse(after.contains('\n'))
        if (!before.isNullOrBlank()) assertTrue(after.startsWith(before.trimEnd()))
        assertEquals(PlacementKind.MEMO_FIELD, ins.placement.kind.known)
        assertEquals(Fx.C2, ins.placement.targetKey)
        assertEquals(
            listOf(Fx.target(RecorderTargetKind.UNIT, Fx.C2) to LinkOrigin.INSERT),
            ins.library.linksOf(Fx.R3).map { it.target to it.origin.known },
        )
        assertEquals(RecordState.PUT, RecordStates.state(ins.library, ins.library.record(Fx.R3)!!, env.withDoc(ins.doc)))
        val (undone, _) = Undo.undo(ins.doc, ins.library, ins.undo)
        assertEquals(before, undone.index.unit(Fx.C2)!!.synopsis)
    }

    @Test
    fun LRC_T23_twoProjects() {
        val first = insertAfter044()
        val envAfter = env.withDoc(first.doc)
        val second = PageInsert.insert(envAfter.project(Fx.P2)!!.doc, first.library, Fx.R1, "", Fx.ctx(start = 300))
        val lib = second.library
        val finalEnv = envAfter.withDoc(second.doc)
        assertEquals(listOf(Fx.P1, Fx.P2), lib.placementsOf(Fx.R1).map { it.projectKey })
        val pageTarget = TargetRef(Fx.P2, RecorderTargetKind.PAGE, second.placement.targetKey)
        assertEquals(
            setOf(Fx.sceneS to LinkOrigin.ENTRY, Fx.hart to LinkOrigin.MANUAL, pageTarget to LinkOrigin.INSERT),
            lib.linksOf(Fx.R1).map { it.target to it.origin.known }.toSet(),
        )
        assertEquals(setOf(Fx.P1, Fx.P2), RecordStates.impliedProjects(lib, Fx.R1, finalEnv))
        assertEquals(RecordState.PUT, RecordStates.state(lib, lib.record(Fx.R1)!!, finalEnv))
    }

    @Test
    fun LRC_T24_atRunsInCopies() {
        val library = Fx.openLibrary()
        val inP1 = PageInsert.insert(Fx.p1Doc(), library, Fx.R2, "", Fx.ctx())
        val runsP1 = inP1.doc.pages[inP1.placement.targetKey]!!.blocks.single().runs
        val link = runsP1.filterIsInstance<TextRun.Link>().single()
        assertEquals(K.SERA, link.target.key)
        assertNull(link.target.projectKey)
        assertTrue(link.linkKey != library.record(Fx.R2)!!.runs.filterIsInstance<TextRun.Link>().single().linkKey)

        val inP2 = PageInsert.insert(Fx.p2Doc(), library, Fx.R2, "", Fx.ctx())
        val runsP2 = inP2.doc.pages[inP2.placement.targetKey]!!.blocks.single().runs
        assertTrue(runsP2.none { it is TextRun.Link })
        assertEquals("오웬은 사실 결사 출신? 탈퇴 이유가 세라와 관련", Runs.text(runsP2))
    }

    @Test
    fun LRC_T25_editsAfterInsertion() {
        val ins = insertAfter044()
        val envAfter = env.withDoc(ins.doc)
        val edited = RecordScreens.editText(ins.library, Fx.R1, Runs.plain(ExampleText.R1 + " 다시 생각해 보기."), Fx.ctx(start = 700))
        assertEquals(2, edited.record(Fx.R1)!!.textRevision)
        assertEquals(ExampleText.R1, Runs.text(ins.doc.index.block(ins.placement.targetKey)!!.runs))
        val screen = RecordScreens.of(edited, Fx.R1, envAfter, Fx.NOW)!!
        val row = screen.placements.single()
        assertEquals("1-7 씬 3 · #058", row.text)
        assertEquals("◇ 넣은 뒤 고침", row.mark)
        val inboxRow = Inbox.row(edited, edited.record(Fx.R1)!!, envAfter, Fx.NOW)
        assertTrue(listOf(inboxRow.kindLabel, inboxRow.meta, inboxRow.stateLabel, inboxRow.text).none { it.contains("◇") })

        // The copy edited instead: the record is unchanged and shows no mark.
        val blockKey = ins.placement.targetKey
        val copyEdited = ins.doc.copy(episodes = ins.doc.episodes.mapValues { (_, f) ->
            f.copy(blocks = f.blocks.map { if (it.key == blockKey) it.copy(runs = Runs.plain("고친 사본")) else it })
        })
        val screen2 = RecordScreens.of(ins.library, Fx.R1, env.withDoc(copyEdited), Fx.NOW)!!
        assertEquals(1, ins.library.record(Fx.R1)!!.textRevision)
        assertNull(screen2.placements.single().mark)
    }

    @Test
    fun LRC_T26_protectedDestination() {
        val library = Fx.openLibrary()
        for (kind in PutKind.values()) {
            val step = Destinations.choose(env, Fx.P3, kind)
            assertEquals(DestinationStep.AwaitingAuth(Fx.P3), step)
            assertEquals(DestinationStep.ChoosingProject, Destinations.authResult(step, success = false))
            assertEquals(DestinationStep.Chosen(Fx.P3), Destinations.authResult(step, success = true))
        }
        assertEquals(Fx.openLibrary(), library)
        assertTrue(Destinations.options(env, PutKind.PAGE).first { it.projectKey == Fx.P3 }.let { it.needsAuth && it.label == "숨긴 프로젝트" })

        val openR18 = Fx.p3Doc().let { it.copy(project = it.project.copy(protections = Protections(recorderLinkMask = true))) }
        val unhiddenEnv = Fx.env(p3 = openR18).let { e ->
            e.withProject(ProjectSnapshot(Fx.entry(openR18), openR18))
        }
        assertEquals(DestinationStep.AwaitingAuth(Fx.P3), Destinations.choose(unhiddenEnv, Fx.P3, PutKind.MANUSCRIPT))
        assertEquals(DestinationStep.Chosen(Fx.P1), Destinations.choose(env, Fx.P1, PutKind.MANUSCRIPT))
    }

    @Test
    fun LRC_T27_makeCharacter() {
        val doc = Fx.p1Doc()
        val ins = CharacterInsert.insert(doc, Fx.openLibrary(), Fx.R3, "시장 꼬마", Fx.ctx())
        val character = ins.doc.index.character(ins.placement.targetKey)!!
        assertEquals("시장 꼬마", character.name)
        assertEquals("2장 첫 장면은 비 오는 상층 관문에서 시작.", character.oneLiner)
        assertEquals(doc.project.counters.nextCharacterRecordNo, character.recordNo)
        val overview = CardDocs.overviewField(ins.doc, character.key)!!
        assertEquals(K.FIELD_PERSONALITY, overview.key)
        val paragraph = (character.fields[overview.key]!!.payload as FieldPayload.Rich).doc.last()
        assertEquals(DocBlockKind.MEMO, paragraph.kind.known)
        assertEquals(ExampleText.R3, Runs.text(paragraph.runs))
        assertEquals(PlacementKind.CHARACTER, ins.placement.kind.known)
        val target = Fx.target(RecorderTargetKind.CHARACTER, character.key)
        assertEquals(listOf(target to LinkOrigin.INSERT), ins.library.linksOf(Fx.R3).map { it.target to it.origin.known })
        assertEquals(RecordState.PUT, RecordStates.state(ins.library, ins.library.record(Fx.R3)!!, env.withDoc(ins.doc)))
    }

    @Test
    fun LRC_T28_startMaterialAndChecklist() {
        val library = Fx.openLibrary()
        val candidates = RecorderHooks.startMaterialCandidates(library, env, Fx.NOW)
        assertEquals(listOf(Fx.R1, Fx.R2, Fx.R3), candidates.map { it.recordKey })
        assertEquals("음성 0:24 · 오늘", candidates[0].meta)
        assertEquals("라이프 레코더 기록 연결 · 3개 중 2개 고름", RecorderHooks.startMaterialHeader(candidates.size, 2))

        val p4 = ProjectDoc(project = Fx.p2Doc().project.copy(key = Fx.P4, name = "새 프로젝트"))
        val env4 = env.withDoc(p4)
        val linked = RecorderHooks.linkRecords(library, listOf(Fx.R1, Fx.R3), Fx.P4, Fx.ctx())
        for (key in listOf(Fx.R1, Fx.R3)) {
            assertTrue(linked.linksOf(key).any { it.target == TargetRef.project(Fx.P4) && it.origin.known == LinkOrigin.START_MATERIAL })
            assertTrue(linked.placementsOf(key).isEmpty())
            assertEquals(RecordState.UNSORTED, RecordStates.state(linked, linked.record(key)!!, env4))
        }
        assertEquals("연결된 기록 2개 정리하기", RecorderHooks.checklistLine(linked, Fx.P4, env4))
        val preset = InboxFilters.preset(InboxOpening.FromProject(Fx.P4))
        assertEquals(ProjectFilter.Project(Fx.P4), preset.project)
        assertEquals(listOf(Fx.R1, Fx.R3), Inbox.screen(linked, preset, env4, Fx.NOW).rows.map { it.recordKey })
    }

    @Test
    fun cardInsertIntoSpeechTabAndUndo() {
        val doc = Fx.p1Doc()
        val ins = io.github.lenticualrstone.civilight.domain.recorder.put.CardInsert.insert(doc, Fx.openLibrary(), Fx.R1, Fx.hart, CardDocs.SPEECH, Fx.ctx())
        val hart = ins.doc.index.character(K.HART)!!
        assertEquals(ins.placement.targetKey, hart.speechBody.last().key)
        assertEquals(DocBlockKind.MEMO, hart.speechBody.last().kind.known)
        assertEquals("하르트 · 말투", ins.placement.lastLabel)
        assertEquals(K.HART, ins.placement.containerKey)
        assertEquals("speech", ins.placement.tabId)
        assertNull(ins.undo.addedLinkKey) // 하르트 was already linked
        assertEquals("TODO_COPY_LRC_13", ins.toast.text)
        val (undone, lib) = Undo.undo(ins.doc, ins.library, ins.undo)
        assertEquals(doc.index.character(K.HART)!!.speechBody, undone.index.character(K.HART)!!.speechBody)
        assertTrue(lib.placementsOf(Fx.R1).isEmpty())
    }

    @Test
    fun characterTabsAndWorldTabs() {
        val doc = Fx.p1Doc()
        assertEquals(listOf("개요", "말투", "비밀", "성격"), CardDocs.tabs(doc, RecorderTargetKind.CHARACTER, K.HART).map { it.label })
        assertEquals(listOf("설명"), CardDocs.tabs(doc, RecorderTargetKind.TERM, K.TERM_BLACKOUT).map { it.label })
    }

    @Test
    fun undoOfACharacterKeepsTheRecordNumberIssued() {
        val doc = Fx.p1Doc()
        val ins = CharacterInsert.insert(doc, Fx.openLibrary(), Fx.R3, "시장 꼬마", Fx.ctx())
        val (undone, _) = Undo.undo(ins.doc, ins.library, ins.undo)
        assertNull(undone.index.character(ins.placement.targetKey))
        assertEquals(doc.project.counters.nextCharacterRecordNo + 1, undone.project.counters.nextCharacterRecordNo)
    }

    @Test
    fun linkedScenesListOnlyLiveScenesOfThisProject() {
        val library = io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits.add(
            Fx.openLibrary(), Fx.R1, Fx.target(RecorderTargetKind.UNIT, K.SCENE_1_6_5), LinkOrigin.MANUAL, Fx.ctx(),
        )
        // 1-6 씬 5 is in the trash: no row for it.
        assertEquals(1, PositionSheets.of(library, Fx.R1, Fx.P1, env).rows.count { it is PositionRow.LinkedScene })
    }

    @Test
    fun emptyRecordCannotBeInserted() {
        val library = Fx.openLibrary().let { lib -> lib.copy(records = lib.records.map { if (it.key == Fx.R3) it.copy(runs = emptyList()) else it }) }
        val failed = runCatching { PageInsert.insert(Fx.p1Doc(), library, Fx.R3, "x", Fx.ctx()) }
        assertTrue(failed.isFailure)
    }

    @Test
    fun manuscriptDestinationsExcludeSettingOnlyAndExample() {
        val settingOnly = Fx.p2Doc().let { it.copy(project = it.project.copy(format = io.github.lenticualrstone.civilight.model.project.ProjectFormat.SETTING_ONLY.open())) }
        val e = Fx.env(p2 = settingOnly, p1Entry = Fx.entry(Fx.p1Doc(), isExample = true))
        val keys = Destinations.options(e, PutKind.MANUSCRIPT).map { it.projectKey }
        assertFalse(Fx.P1 in keys)
        assertFalse(Fx.P2 in keys)
        assertTrue(Fx.P2 in Destinations.options(e, PutKind.PAGE).map { it.projectKey })
    }
}
