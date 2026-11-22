package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.K
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordScreens
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.DeletedTargets
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkDisplay
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.Insertion
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptSpot
import io.github.lenticualrstone.civilight.domain.recorder.put.SourceMarks
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementStatus
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.domain.recorder.trash.RecordTrash
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDate

/** LRC §4.6 · Deletion. */
class DeletionTest {
    private fun chipsOf(library: RecorderLibrary, key: String, env: RecorderEnv): List<Chip> =
        Chips.captureRow(library.linksOf(key), library.record(key)!!.runs, env)

    private fun tagsOf(library: RecorderLibrary, key: String, env: RecorderEnv) =
        Inbox.row(library, library.record(key)!!, env, Fx.NOW).linkTags

    private fun markPurged(library: RecorderLibrary, target: TargetRef, lastLabel: String, hidden: Boolean = false) = library.copy(
        links = library.links.map {
            if (it.projectKey == target.projectKey && it.targetKey == target.key) it.copy(purgedAt = Fx.NOW, lastLabel = lastLabel, lastLabelHidden = hidden) else it
        },
    )

    @Test
    fun LRC_T34_linkedSceneDeleted() {
        val library = Fx.openLibrary()
        val trashed = Fx.trashUnit(Fx.p1Doc(), Fx.S)
        val inTrash = tagsOf(library, Fx.R1, Fx.env(p1 = trashed)).first()
        assertEquals("1-7 씬 3 · 휴지통", inTrash.text)
        assertTrue(inTrash.dotted)
        assertEquals(LinkDisplay.TRASHED, inTrash.display)

        val restored = Fx.restoreUnit(trashed, Fx.S)
        assertEquals("1-7 씬 3", tagsOf(library, Fx.R1, Fx.env(p1 = restored)).first().text)

        // Purge as domain:trash writes it: `purgedAt` + last label `<code> · <title>`.
        val purgedDoc = Fx.purgeUnit(trashed, Fx.S)
        val purgedLib = markPurged(library, Fx.sceneS, "1-7 씬 3 · 꺼진 등불")
        val env = Fx.env(p1 = purgedDoc)
        val gone = tagsOf(purgedLib, Fx.R1, env).first()
        assertEquals("지워진 씬 · 1-7 씬 3", gone.text)
        assertTrue(gone.plain)
        assertFalse(gone.removable)
        val rows = Inbox.screen(purgedLib, InboxFilterState(project = ProjectFilter.Project(Fx.P1)), env, Fx.NOW).rows
        assertTrue(rows.any { it.recordKey == Fx.R1 })
    }

    @Test
    fun LRC_T35_linkedProjectDeleted() {
        val library = Fx.openLibrary()
        val p1 = Fx.p1Doc()
        val trashedEnv = Fx.env(p1Entry = Fx.entry(p1, trashedAt = Fx.NOW))
        val projectChip = chipsOf(library, Fx.R2, trashedEnv).first { it.links.single().targetKind.known == RecorderTargetKind.PROJECT }
        assertEquals("연결 · 잿빛 새벽의 등불 · 휴지통", projectChip.text)
        assertTrue(projectChip.dotted)

        // Purged: the project leaves the 서재; its links keep the name (applyProjectGone).
        val gone = library.copy(links = library.links.map {
            if (it.projectKey == Fx.P1) it.copy(purgedAt = Fx.NOW, lastLabel = it.lastLabel ?: "잿빛 새벽의 등불") else it
        })
        val env = trashedEnv.withoutProject(Fx.P1)
        val chip = chipsOf(gone, Fx.R2, env).first { it.links.single().targetKind.known == RecorderTargetKind.PROJECT }
        assertEquals("연결 · 잿빛 새벽의 등불", chip.text)
        assertTrue(chip.plain)
        val rows = Inbox.screen(gone, InboxFilterState(project = ProjectFilter.NoLink), env, Fx.NOW).rows
        assertTrue(rows.any { it.recordKey == Fx.R2 })
    }

    private fun inserted(): Insertion =
        ManuscriptInsert.insert(Fx.p1Doc(), Fx.openLibrary(), Fx.R1, ManuscriptSpot.AfterBlock(K.BLOCK_044), Fx.ctx())

    // LRC-T36/T37 as stated with the block number as a parameter (#062 in the LRC fixture, #058 in
    // the example fixture: fixture.md conflict 3).

    private fun num(n: Int) = "#%03d".format(n)

    private fun t36(expected: Int) {
        val ins = inserted()
        val trashed = Fx.trashBlock(ins.doc, ins.placement.targetKey)
        val envTrash = Fx.env(p1 = trashed)
        val row = RecordScreens.of(ins.library, Fx.R1, envTrash, Fx.NOW)!!.placements.single()
        assertEquals("1-7 씬 3 · ${num(expected)} · 휴지통", row.text)
        assertEquals(PlacementStatus.IN_TRASH, row.status)
        assertEquals(RecordState.PUT, RecordStates.state(ins.library, ins.library.record(Fx.R1)!!, envTrash))

        val envPurged = Fx.env(p1 = Fx.purgeBlock(trashed, ins.placement.targetKey))
        val screen = RecordScreens.of(ins.library, Fx.R1, envPurged, Fx.NOW)!!
        val gone = screen.placements.single()
        assertEquals("1-7 씬 3 · ${num(expected)}", gone.text)
        assertEquals(PlacementStatus.GONE, gone.status)
        assertFalse(gone.tappable)
        assertEquals(RecordState.UNSORTED, screen.state)
        assertEquals("◇ 넣은 곳 지워짐", screen.placementsGoneMark)
        // The stored purge mark gives the same.
        val marked = ins.library.copy(placements = ins.library.placements.map { it.copy(purgedAt = Fx.NOW) })
        assertEquals(RecordState.UNSORTED, RecordStates.state(marked, marked.record(Fx.R1)!!, null))
    }

    @Test
    @Ignore("spec conflict: the placement label is `1-7 씬 3 · #062`; the example fixture's counter gives #058 (fixture.md conflict 3)")
    fun LRC_T36_placementsDeleted() = t36(62)

    @Test
    fun LRC_T36_fixture_placementsDeleted() = t36(58)

    private fun t37(expected: Int) {
        val ins = inserted()
        val env = Fx.env(p1 = ins.doc)
        val block = ins.doc.index.block(ins.placement.targetKey)!!
        assertEquals(expected, block.number)
        val source = block.recorderSource!!
        val ctx = Fx.ctx(start = 900)
        val (trashedLib, entry, toast) = RecordTrash.delete(ins.library, Fx.R1, ctx)!!
        assertEquals("레코더 · 휴지통", SourceMarks.mark(source, trashedLib, Fx.ZONE))
        assertEquals(ExampleText.R1, Runs.text(ins.doc.index.block(block.key)!!.runs))
        assertTrue(Inbox.screen(trashedLib, InboxFilterState(), env, Fx.NOW).rows.none { it.recordKey == Fx.R1 })
        assertEquals(0, RecorderHooks.unsortedLinkedCount(trashedLib, Fx.sceneS, env))
        assertEquals(listOf("되돌리기"), toast.actions)

        val restored = RecordTrash.restore(trashedLib, entry, ctx)!!
        assertEquals("레코더 11월 22일 08:41", SourceMarks.mark(source, restored, Fx.ZONE))
        assertEquals(ins.library.linksOf(Fx.R1), restored.linksOf(Fx.R1))
        assertEquals(ins.library.placementsOf(Fx.R1), restored.placementsOf(Fx.R1))
        assertEquals(RecordState.PUT, RecordStates.state(restored, restored.record(Fx.R1)!!, env))

        val purged = RecordTrash.purge(trashedLib, entry)
        assertEquals("지워진 기록", SourceMarks.mark(source, purged, Fx.ZONE))
        assertTrue(purged.linksOf(Fx.R1).isEmpty() && purged.placementsOf(Fx.R1).isEmpty())
        assertEquals(ExampleText.R1, Runs.text(ins.doc.index.block(block.key)!!.runs))
    }

    @Test
    @Ignore("spec conflict: refers to block #062; the example fixture's counter gives #058 (fixture.md conflict 3)")
    fun LRC_T37_recordDeleted() = t37(62)

    @Test
    fun LRC_T37_fixture_recordDeleted() = t37(58)

    private fun withNoahLink(): RecorderLibrary = LinkEdits.add(Fx.openLibrary(), Fx.R1, Fx.noah, LinkOrigin.MANUAL, Fx.ctx(start = 600))

    private fun noahChip(library: RecorderLibrary, env: RecorderEnv) =
        chipsOf(library, Fx.R1, env).first { it.links.single().targetKey == K.NOAH }

    @Test
    fun LRC_T38_deletedCharacter() {
        val library = withNoahLink()
        val live = Fx.withNoahLive(Fx.p1Doc())
        assertEquals("노아", noahChip(library, Fx.env(p1 = live)).text)

        val deleted = Fx.trashCharacter(live, K.NOAH)
        val env = Fx.env(p1 = deleted)
        assertEquals(1, RecorderHooks.recorderLinkCount(library, Fx.noah))
        val chip = noahChip(library, env)
        assertEquals("노아", chip.text)
        assertTrue(chip.dotted && chip.diamond)
        assertEquals(LinkDisplay.DELETED_ITEM_TRASH, chip.display)
        val card = DeletedTargets.card(chip, env)!!
        assertEquals("지운 인물 · 노아", card.title)
        assertEquals(listOf("칩 다시 고르기", "칩 빼기", "이대로 두기"), card.actions)

        assertEquals(LinkDisplay.NORMAL, noahChip(library, Fx.env(p1 = live)).display)

        val purged = markPurged(library, Fx.noah, "노아")
        val purgedChip = noahChip(purged, Fx.env(p1 = Fx.purgeCharacter(deleted, K.NOAH)))
        assertEquals("영구 삭제 · 노아", purgedChip.text)
    }

    @Test
    fun LRC_T38b_deletedCharacterInAtRun() {
        val library = Fx.openLibrary()
        val deleted = Fx.trashCharacter(Fx.p1Doc(), K.SERA)
        val env = Fx.env(p1 = deleted)
        assertEquals(0, RecorderHooks.recorderLinkCount(library, Fx.sera))
        assertEquals(1, RecorderHooks.recorderAtLinkCount(library, Fx.sera))
        val chip = chipsOf(library, Fx.R2, env).first { it.links.single().targetKey == K.SERA }
        assertEquals("세라 @", chip.text)
        assertTrue(chip.dotted && chip.diamond && chip.atForm)
        assertFalse(chip.removable)
        val card = DeletedTargets.card(chip, env)!!
        assertEquals("지운 인물 · 세라", card.title)
        assertEquals(listOf("다른 항목에 잇기", "글로 풀기", "되살리기", "이대로 두기"), card.actions)
        assertFalse("칩 빼기" in card.actions)

        val plain = DeletedTargets.toPlainText(library, Fx.R2, Fx.sera, Fx.ctx())
        val r2 = plain.record(Fx.R2)!!
        assertEquals("오웬은 사실 결사 출신? 탈퇴 이유가 세라와 관련", Runs.text(r2.runs))
        assertTrue(r2.runs.none { it is TextRun.Link })
        assertTrue(plain.linksOf(Fx.R2).none { it.targetKey == K.SERA })
    }

    @Test
    fun LRC_T38c_hiddenWinsOverDeletion() {
        val library = withNoahLink()
        val hidden = Fx.withNoahLive(Fx.p1Doc(), hidden = true)
        val trashed = Fx.trashCharacter(hidden, K.NOAH)
        val chip = noahChip(library, Fx.env(p1 = trashed))
        assertEquals("숨긴 항목", chip.text)
        assertFalse(chip.dotted)

        val purged = markPurged(library, Fx.noah, "노아", hidden = true)
        val purgedChip = noahChip(purged, Fx.env(p1 = Fx.purgeCharacter(trashed, K.NOAH)))
        assertEquals("영구 삭제 · 숨긴 항목", purgedChip.text)
        assertFalse(purgedChip.text.contains("노아"))
    }

    @Test
    fun LRC_T39_sceneMoved() {
        val doc = Fx.p1Doc()
        val firstOf18 = doc.index.children(K.EPISODE_1_8).first()
        val moved = doc.copy(story = doc.story.copy(units = doc.story.units.map {
            if (it.key == Fx.S) it.copy(parentKey = K.EPISODE_1_8, order = OrderKeys.before(firstOf18.order)) else it
        })).regroupBlocks()
        val library = Fx.openLibrary()
        val link = library.linksOf(Fx.R1).first { it.targetKind.known == RecorderTargetKind.UNIT }
        assertEquals(Fx.S, link.targetKey)
        assertEquals("1-8 씬 1", tagsOf(library, Fx.R1, Fx.env(p1 = moved)).first().text)
    }

    @Test
    fun LRC_T40_exampleProject() {
        val p1 = Fx.p1Doc()
        val env = Fx.env(p1Entry = Fx.entry(p1, isExample = true))
        val ctx = Fx.ctx()
        val entry = EntryLinks.script(Fx.P1, Fx.S, K.BLOCK_044, isExample = true)
        var s = Recording.start("01JZ000000TSTK000000000700", entry, RecorderSettings(), env, "a", ctx)
        assertEquals(listOf("연결 · 예시 · 끝나면 연결 없어짐"), Chips.captureRow(s.pendingLinks, emptyList(), env).map { it.text })
        s = Recording.final(s, "예시에서 녹음")
        val (done, lib) = Recording.stopAndSave(s, RecorderLibrary(), ctx)
        val saved = Recording.keepInInbox(done, lib, ctx)

        val after = RecorderHooks.exampleProjectDeleted(saved, Fx.P1)
        val envAfter = env.withoutProject(Fx.P1)
        assertNotNull(after.record(s.recordKey))
        assertTrue(after.linksOf(s.recordKey).isEmpty())
        val rows = Inbox.screen(after, InboxFilterState(project = ProjectFilter.NoLink), envAfter, Fx.NOW).rows
        assertEquals(listOf(s.recordKey), rows.map { it.recordKey })
    }

    @Test
    fun LRC_T41_trashRowAndFilter() {
        val trashedRecord = ExampleRecorder.records().first { it.key == K.RECORDER_TRASHED }
        val entry = ExampleRecorder.trashEntries().single()
        val today = LocalDate.of(2026, 11, 22)
        val row = RecordTrash.row(trashedRecord, entry, today)
        assertEquals(listOf("음성 0:12", "라이프 레코더", "1일 남음"), listOf(row.title, row.sub, row.daysLeft))
        assertEquals(listOf("되살리기", "영구 삭제"), row.actions)
        assertFalse(RecordTrash.isDue(entry, today))
        assertTrue(RecordTrash.isDue(entry, LocalDate.of(2026, 11, 23)))
        assertEquals(listOf(K.RECORDER_TRASHED), entry.targetKeys)
        val library = ExampleRecorder.library()
        assertTrue(RecordTrash.purge(library, entry).record(K.RECORDER_TRASHED) == null)
    }

    @Test
    fun LRC_T42_deletingAProjectNeverTrashesRecords() {
        val library = Fx.openLibrary()
        val env = Fx.env(p1Entry = Fx.entry(Fx.p1Doc(), trashedAt = Fx.NOW))
        assertNull(library.record(Fx.R2)!!.deletedAt)
        assertTrue(Inbox.screen(library, InboxFilterState(), env, Fx.NOW).rows.any { it.recordKey == Fx.R2 })
    }

    @Test
    fun hiddenRecordInTrashIsMaskedAndNeedsAuthToRestore() {
        val ctx = Fx.ctx()
        val library = Fx.hiddenLibrary()
        val (trashed, entry, _) = RecordTrash.delete(library, Fx.R1, ctx)!!
        assertEquals("숨긴 기록", RecordTrash.row(trashed.record(Fx.R1)!!, entry, ctx.today).title)
        assertNull(RecordTrash.restore(trashed, entry, ctx))
        assertNotNull(RecordTrash.restore(trashed, entry, ctx, authenticated = true))
    }

    @Test
    fun relinkRunPointsTheRunElsewhere() {
        val library = Fx.openLibrary()
        val next = DeletedTargets.relinkRuns(library, Fx.R2, Fx.sera, Fx.hart, Fx.ctx())
        val run = next.record(Fx.R2)!!.runs.filterIsInstance<TextRun.Link>().single()
        assertEquals(K.HART, run.target.key)
        assertEquals("세라", run.text)
        assertTrue(next.linksOf(Fx.R2).none { it.targetKey == K.SERA })
        assertTrue(next.linksOf(Fx.R2).any { it.targetKey == K.HART && it.origin.known == LinkOrigin.AT })
    }
}
