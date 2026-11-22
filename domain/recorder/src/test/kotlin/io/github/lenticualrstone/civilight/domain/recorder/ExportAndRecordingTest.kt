package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.K
import io.github.lenticualrstone.civilight.domain.recorder.Fx.withRecord
import io.github.lenticualrstone.civilight.domain.recorder.capture.MicAction
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.export.RecorderExport
import io.github.lenticualrstone.civilight.domain.recorder.export.RecorderMerge
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderTermUse
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.PageInsert
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LRC §4.7 · Export and §4.8 · Recording rules. */
class ExportAndRecordingTest {
    private val env = Fx.env()
    private val audio = RecorderAudio(hash = "a".repeat(64), mime = "audio/mp4", bytes = 96_000, durationMs = 24_000)

    private fun fourLinkedToP1(): RecorderLibrary {
        val ctx = Fx.ctx(start = 300)
        val withLinks = LinkEdits.add(LinkEdits.add(Fx.openLibrary(), Fx.R3, TargetRef.project(Fx.P1), LinkOrigin.MANUAL, ctx), Fx.R4, TargetRef.project(Fx.P1), LinkOrigin.MANUAL, ctx)
        return withLinks.withRecord(Fx.R4) { it.copy(hidden = true) }.withRecord(Fx.R1) { it.copy(audio = audio) }
    }

    @Test
    fun LRC_T43_exportRow() {
        val library = fourLinkedToP1()
        val row = RecorderExport.row(library, env, Fx.P1, encrypted = false)
        assertEquals(listOf("레코더 기록 4", "원본 음성 포함", "숨긴 기록 1 제외"), row.parts)
        val file = RecorderExport.projectFile(library, env, Fx.P1, encrypted = false, includeAudio = false)
        assertEquals(3, file.records.size)
        assertFalse(file.records.any { it.key == Fx.R4 })
        assertTrue(RecorderExport.audioHashes(file).isEmpty())
        val withAudio = RecorderExport.projectFile(library, env, Fx.P1, encrypted = false, includeAudio = true)
        assertEquals(listOf(audio.hash), RecorderExport.audioHashes(withAudio))
        // The file round-trips through the canonical Json.
        val json = CivilightJson.encode(RecorderFile.serializer(), file)
        assertEquals(file, CivilightJson.decode(RecorderFile.serializer(), json))
    }

    @Test
    fun LRC_T44_hiddenProjectFile() {
        val ctx = Fx.ctx(start = 400)
        val p3Scene = TargetRef(Fx.P3, RecorderTargetKind.UNIT, Fx.P3_SCENE)
        val library = LinkEdits.add(LinkEdits.add(Fx.openLibrary(), Fx.R3, p3Scene, LinkOrigin.MANUAL, ctx), Fx.R4, p3Scene, LinkOrigin.MANUAL, ctx)
            .withRecord(Fx.R4) { it.copy(hidden = true) }
        val file = RecorderExport.projectFile(library, env, Fx.P3, encrypted = true, includeAudio = false)
        assertEquals(setOf(Fx.R3, Fx.R4), file.records.map { it.key }.toSet())
        assertEquals(2, RecorderExport.row(library, env, Fx.P3, encrypted = true).linked)
        assertEquals("TODO_COPY_LRC_20", RecorderExport.row(library, env, Fx.P3, encrypted = true).hiddenPart)
        // Not encrypted: the hidden one stays out.
        assertEquals(listOf(Fx.R3), RecorderExport.projectFile(library, env, Fx.P3, encrypted = false, includeAudio = false).records.map { it.key })
    }

    @Test
    fun LRC_T45_otherFormats() {
        assertFalse(RecorderExport.appliesTo(ExportFormat.PDF))
        assertFalse(RecorderExport.appliesTo(ExportFormat.CSV))
        assertFalse(RecorderExport.appliesTo(ExportFormat.ENGINE_JSON))
        assertTrue(RecorderExport.appliesTo(ExportFormat.PROJECT_FILE))
    }

    @Test
    fun LRC_T46_multiProjectAndMerge() {
        val ins = PageInsert.insert(Fx.p2Doc(), Fx.openLibrary(), Fx.R1, "", Fx.ctx(start = 500))
        val library = ins.library
        val envAfter = env.withDoc(ins.doc)
        val p1File = RecorderExport.projectFile(library, envAfter, Fx.P1, encrypted = false, includeAudio = false)
        val p2File = RecorderExport.projectFile(library, envAfter, Fx.P2, encrypted = false, includeAudio = false)
        assertTrue(p1File.records.any { it.key == Fx.R1 })
        assertTrue(p2File.records.any { it.key == Fx.R1 })
        val r1InP1 = p1File.records.first { it.key == Fx.R1 }
        assertEquals(setOf(Fx.S, K.HART), r1InP1.links.map { it.targetKey }.toSet())
        assertTrue(r1InP1.placements.isEmpty())
        assertTrue(r1InP1.links.all { it.projectKey == null })

        // Import P2's file on a device that already has R1 (P1 links only, locally edited text).
        val local = Fx.openLibrary().withRecord(Fx.R1) { it.copy(runs = Runs.plain("로컬에서 고친 글"), textRevision = 2) }
        val merged = RecorderMerge.merge(local, p2File.toLibrary(impliedProjectKey = Fx.P2))
        assertEquals(1, merged.records.count { it.key == Fx.R1 })
        assertEquals("로컬에서 고친 글", Runs.text(merged.record(Fx.R1)!!.runs))
        assertTrue(merged.linksOf(Fx.R1).any { it.projectKey == Fx.P2 && it.targetKind.known == RecorderTargetKind.PAGE })
        assertTrue(merged.linksOf(Fx.R1).any { it.targetKey == Fx.S })
        assertEquals(listOf(Fx.P2), merged.placementsOf(Fx.R1).map { it.projectKey })
        // Merging twice adds nothing.
        assertEquals(merged, RecorderMerge.merge(merged, p2File.toLibrary(impliedProjectKey = Fx.P2)))
    }

    @Test
    fun LRC_T47_noAutoStop() {
        val ctx = Fx.ctx()
        var s = Recording.start("01JZ000000TSTK000000000900", EntryLinks.outside(EntrySurface.LIBRARY), RecorderSettings(), env, "a", ctx)
        s = Recording.final(Recording.elapse(s, 3_000), "생각 하나")
        s = Recording.silence(s, 10 * 60_000L)
        assertEquals(RecordingPhase.RECORDING, s.phase)
        assertEquals(603_000L, s.elapsedMs)
        assertEquals("10:03", Recording.recordingScreen(s, env).timer)
        val library = RecorderLibrary()
        assertNull(library.record(s.recordKey))
        val (stopped, saved) = Recording.stopAndSave(s, library, ctx)
        assertEquals(RecordingPhase.PROCESSING, stopped.phase)
        assertEquals(603_000L, saved.record(s.recordKey)!!.durationMs)
    }

    @Test
    fun LRC_T48_resumeContinuesTheSameRecord() {
        val ctx = Fx.ctx()
        var s = Recording.start("01JZ000000TSTK000000000901", EntryLinks.outside(EntrySurface.LIBRARY), RecorderSettings(), env, "a", ctx)
        s = Recording.final(Recording.elapse(s, 24_000), "하르트가 등불을 싫어하는 이유.")
        var (processing, library) = Recording.stopAndSave(s, RecorderLibrary(), ctx)
        assertEquals("00:24", Recording.processingScreen(processing, env, finalising = true, audioKept = true).duration)
        var again = Recording.continueSpeaking(processing, library)
        assertEquals(RecordingPhase.RECORDING, again.phase)
        again = Recording.partial(Recording.elapse(again, 10_000), "그게 세라의")
        val saved = Recording.stopAndSave(again, library, ctx)
        processing = saved.first
        library = saved.second
        assertEquals(1, library.records.size)
        val record = library.record(s.recordKey)!!
        assertEquals(34_000L, record.durationMs)
        assertEquals("하르트가 등불을 싫어하는 이유. 그게 세라의", Runs.text(record.runs))
        assertEquals(2, record.textRevision)
    }

    @Test
    fun LRC_T49_lockScreen() {
        val on = RecorderSettings(lockScreenRecording = true)
        assertEquals(MicAction.START_RECORDING, Recording.micTap(permissionGranted = true, running = null, onLockScreen = true, settings = on))
        val ctx = Fx.ctx()
        var s = Recording.start("01JZ000000TSTK000000000902", EntryLinks.outside(EntrySurface.LOCK_SCREEN), on, env, "a", ctx)
        assertTrue(s.pendingLinks.isEmpty())
        s = Recording.stopAndSave(Recording.final(s, "잠금 화면 메모"), RecorderLibrary(), ctx).first
        val screen = Recording.processingScreen(s, env, finalising = false, audioKept = true)
        assertEquals(listOf("이어서 말하기", "기록함에 두기"), screen.buttons)
        assertNull(screen.putHere)
        assertTrue(Recording.linkPickerNeedsUnlock(s))
        // Even a script entry on the lock screen offers no 여기 넣기.
        val scriptLocked = EntryLinks.script(Fx.P1, Fx.S, K.BLOCK_044).copy(onLockScreen = true)
        val s2 = Recording.stopAndSave(Recording.start("01JZ000000TSTK000000000903", scriptLocked, on, env, "a", ctx), RecorderLibrary(), ctx).first
        assertNull(Recording.processingScreen(s2, env, finalising = false, audioKept = true).putHere)

        val off = RecorderSettings(lockScreenRecording = false)
        assertEquals(MicAction.REQUEST_UNLOCK, Recording.micTap(permissionGranted = true, running = null, onLockScreen = true, settings = off))
    }

    @Test
    fun LRC_T50_rule21DoesNotApply() {
        val ctx = Fx.ctx()
        val withPlainTerm = Fx.openLibrary().withRecord(Fx.R3) { it.copy(runs = Runs.plain("오리지늄 등불이 꺼진다")) }
        assertFalse(RecorderTermUse.SCANNED_FOR_CANDIDATES)
        val term = TargetRef(Fx.P1, RecorderTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN)
        assertTrue(RecorderTermUse.recordsUsing(withPlainTerm, term).isEmpty())
        assertTrue(Runs.text(withPlainTerm.record(Fx.R1)!!.runs).contains("등불"))

        val linkRun = Runs.linkRun("01JZ000000TSTK000000000777", io.github.lenticualrstone.civilight.model.text.LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN, Fx.P1, "오리지늄 등불")
        val withAt = withPlainTerm.withRecord(Fx.R3) { it.copy(runs = listOf(linkRun, TextRun.Plain("이 꺼진다"))) }
        assertEquals(listOf(Fx.R3), RecorderTermUse.recordsUsing(withAt, term))
        assertTrue(ctx.now > 0)
    }

    @Test
    fun cancelFirstTakeDiscardsAndUndoRestores() {
        val ctx = Fx.ctx()
        var s = Recording.start("01JZ000000TSTK000000000904", EntryLinks.outside(EntrySurface.LIBRARY), RecorderSettings(), env, "a", ctx)
        s = Recording.final(Recording.elapse(s, 5_000), "버릴 생각")
        val (session, cancelled) = Recording.cancel(s, RecorderLibrary())
        assertNull(session)
        assertEquals(listOf("되돌리기"), cancelled!!.toast.actions)
        val (restored, library) = Recording.undoCancel(cancelled, RecorderLibrary(), ctx)
        assertEquals(RecordingPhase.PROCESSING, restored.phase)
        assertEquals("버릴 생각", Runs.text(library.record(s.recordKey)!!.runs))
    }

    @Test
    fun pausedTimeIsNotCountedAndBackNeverDiscards() {
        var s = Recording.start("01JZ000000TSTK000000000905", EntryLinks.outside(EntrySurface.QS_TILE), RecorderSettings(), env, "a", Fx.ctx())
        s = Recording.elapse(s, 2_000)
        s = Recording.pause(s)
        s = Recording.elapse(s, 60_000)
        assertEquals(2_000L, s.elapsedMs)
        assertFalse(Recording.recordingScreen(s, env).live)
        assertEquals("TODO_COPY_LRC_02", Recording.recordingScreen(s, env).pauseLabel)
        s = Recording.resume(s)
        assertEquals("일시정지", Recording.recordingScreen(s, env).pauseLabel)
        assertEquals(io.github.lenticualrstone.civilight.domain.recorder.capture.BackAction.MINIMISE, Recording.back(s))
        assertEquals(MicAction.OPEN_RUNNING, Recording.micTap(true, s, false, RecorderSettings()))
        assertEquals(MicAction.SHOW_MIC_ERROR, Recording.micTap(false, null, false, RecorderSettings()))
    }
}
