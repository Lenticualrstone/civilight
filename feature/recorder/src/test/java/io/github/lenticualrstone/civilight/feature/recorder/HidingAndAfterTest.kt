package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptSpot
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementStatus
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.feature.recorder.audio.RecordPlayer
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.inbox.RevealLook
import io.github.lenticualrstone.civilight.feature.recorder.inbox.RevealMotion
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.navigation.Recording
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** After inserting (LRC-39), hiding and opening (LRC-40 … LRC-42) and deleting (LRC-43) records. */
class HidingAndAfterTest {
    private val r1 = ExampleKeys.RECORDER_R1

    // ---- LRC-39 넣은 뒤 ----

    private fun ProjectDoc.withBlock(blockKey: String, transform: (io.github.lenticualrstone.civilight.model.script.Block) -> io.github.lenticualrstone.civilight.model.script.Block?): ProjectDoc =
        copy(episodes = episodes.mapValues { (_, ep) -> ep.copy(blocks = ep.blocks.mapNotNull { if (it.key == blockKey) transform(it) else it }) })

    private fun RecordFixture.setDoc(scope: TestScope, doc: ProjectDoc) {
        projects.doc = doc
        env.env = Fx.env(doc)
        unlock.value++
        status(scope)
    }

    @Test
    fun LRC_39_copyTrashedRestoredThenPurged() = runTest {
        val f = RecordFixture(this, r1)
        f.view(this)
        val done = f.writer.putInManuscript(r1, Fx.P1, ManuscriptSpot.AfterBlock(ExampleKeys.BLOCK_044))!!
        val block = done.insertion.placement.targetKey
        val live = f.projects.doc

        // Copy to the trash: the row reads `· 휴지통`, the state stays 넣음.
        f.setDoc(this, live.withBlock(block) { it.copy(deletedAt = Fx.NOW) })
        var view = f.view(this)
        assertEquals(PlacementStatus.IN_TRASH, view.placements.single().row.status)
        assertEquals("1-7 씬 3 · #058 · 휴지통", view.placements.single().row.text)
        assertEquals(RecordState.PUT, view.screen.state)
        assertNull(view.screen.placementsGoneMark)

        // Restored: back to normal, tappable.
        f.setDoc(this, live)
        view = f.view(this)
        assertEquals(PlacementStatus.ACTIVE, view.placements.single().row.status)
        assertEquals("1-7 씬 3 · #058", view.placements.single().row.text)
        assertNotNull(view.placements.single().route)

        // Purged: the row stays as plain text; its only placement gone → 정리 안 됨 + `◇ 넣은 곳 지워짐`.
        f.setDoc(this, live.withBlock(block) { null })
        view = f.view(this)
        val row = view.placements.single()
        assertEquals(PlacementStatus.GONE, row.row.status)
        assertEquals("1-7 씬 3 · #058", row.row.text)
        assertFalse(row.row.tappable)
        assertNull(row.route)
        assertEquals(RecordState.UNSORTED, view.screen.state)
        assertEquals(RecorderCopy.PLACEMENTS_GONE, view.screen.placementsGoneMark)
    }

    @Test
    fun LRC_39_undoIsNotADeletionAndTheInboxRowCarriesNoMark() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val done = f.writer.putInManuscript(r1, Fx.P1, ManuscriptSpot.AfterBlock(ExampleKeys.BLOCK_044))!!
        f.writer.undoPut(done)
        assertTrue(f.recorder.library.placementsOf(r1).isEmpty())
        val row = f.view(this).rows.single { it.row.recordKey == r1 }
        assertEquals(RecordState.UNSORTED, row.row.state)
        // Never a `◇` in the inbox (LRC-39 table: record screen only).
        assertFalse(row.row.stateLabel.contains(RecorderCopy.DIAMOND))
        assertFalse(row.row.text.contains(RecorderCopy.DIAMOND))

        val g = RecordFixture(this, r1)
        assertNull(g.view(this).screen.placementsGoneMark)
    }

    @Test
    fun LRC_T38b_anAtRunToADeletedCharacterShowsTheDiamondInTheText() = runTest {
        val f = RecordFixture(this, ExampleKeys.RECORDER_R2)
        val live = f.view(this)
        assertEquals(1, live.marks.count)
        assertFalse(live.marks.isDotted(0))
        assertTrue(live.marks.show(live.text).contains("@세라"))
        val trashed = f.projects.doc.let { doc ->
            doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == ExampleKeys.SERA) it.copy(deletedAt = Fx.NOW) else it }))
        }
        f.setDoc(this, trashed)
        val view = f.view(this)
        assertTrue(view.marks.isDotted(0))
        assertTrue(view.marks.hasDiamond(0))
        // The stored text is unchanged; only the shown mark turns into `◇`.
        assertEquals("오웬은 사실 결사 출신? 탈퇴 이유가 세라와 관련", view.text)
        assertTrue(view.marks.show(view.text).contains("◇세라"))
        assertEquals(view.text.length + 1, view.marks.show(view.text).length)
    }

    @Test
    fun LRC_T34_anAtRunToATrashedSceneIsDottedWithoutTheDiamond() = runTest {
        val sceneRun = io.github.lenticualrstone.civilight.model.text.TextRun.Link(
            linkKey = "01JZ000000TSTK000000000901",
            target = io.github.lenticualrstone.civilight.model.text.LinkTarget(io.github.lenticualrstone.civilight.model.text.LinkTargetKind.UNIT, Fx.S, Fx.P1),
            text = "1-7 씬 3",
        )
        val base = ExampleRecorder.library()
        val lib = base.copy(records = base.records.map { if (it.key == ExampleKeys.RECORDER_R3) it.copy(runs = it.runs + sceneRun) else it })
        val f = RecordFixture(this, ExampleKeys.RECORDER_R3, lib)
        f.view(this)
        f.writer.addLink(ExampleKeys.RECORDER_R3, io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef(Fx.P1, io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind.UNIT, Fx.S))
        val trashed = f.projects.doc.let { d -> d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == Fx.S) it.copy(deletedAt = Fx.NOW) else it })) }
        f.setDoc(this, trashed)
        val view = f.view(this)
        val index = view.marks.count - 1
        assertTrue(view.marks.isDotted(index))
        // A trashed scene's chip reads `1-7 씬 3 · 휴지통` with no `◇`, so its run keeps the `@`.
        assertFalse(view.marks.hasDiamond(index))
        assertTrue(view.marks.show(view.text).endsWith("@1-7 씬 3"))
    }

    // ---- LRC-40 / 41 ----

    @Test
    fun LRC_41_revealBlursSevenToZeroIn320msAndFadesWhenReduced() {
        assertEquals(RevealLook.BLUR, RevealMotion.look(reducedMotion = false, sdk = 34))
        assertEquals(RevealLook.FADE, RevealMotion.look(reducedMotion = false, sdk = 29))
        assertEquals(RevealLook.QUICK_FADE, RevealMotion.look(reducedMotion = true, sdk = 34))
        assertEquals(320, RevealMotion.durationMs(RevealLook.BLUR))
        assertEquals(320, RevealMotion.durationMs(RevealLook.FADE))
        assertEquals(120, RevealMotion.durationMs(RevealLook.QUICK_FADE))
        // Hidden look → clear.
        assertEquals(7f, RevealMotion.blurDp(RevealLook.BLUR, 0f), 0f)
        assertEquals(0f, RevealMotion.blurDp(RevealLook.BLUR, 1f), 0f)
        assertEquals(0.55f, RevealMotion.alpha(RevealLook.BLUR, 0f), 1e-6f)
        assertEquals(1f, RevealMotion.alpha(RevealLook.BLUR, 1f), 1e-6f)
        // The fades never blur.
        assertEquals(0f, RevealMotion.blurDp(RevealLook.QUICK_FADE, 0f), 0f)
        assertEquals(0f, RevealMotion.alpha(RevealLook.FADE, 0f), 0f)
        assertEquals(0.5f, RevealMotion.alpha(RevealLook.QUICK_FADE, 0.5f), 1e-6f)
    }

    @Test
    fun LRC_40_41_hideThenOpenThenUnhide() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        f.writer.hide(r1)
        var v = f.view(this)
        assertFalse(v.rows.any { it.row.recordKey == r1 })
        val locked = v.screen.lockedGroup!!
        assertEquals("숨긴 기록 1", locked.title)
        assertEquals(RecorderCopy.HIDDEN_GROUP_SUB, locked.sub)
        assertEquals(RecorderCopy.OPEN_WITH_FINGERPRINT, locked.button)
        // Locked: excluded from the inbox text search.
        f.controller.search("등불")
        assertFalse(f.view(this).rows.any { it.row.recordKey == r1 })
        f.controller.search("")

        // Open (the prompt passed): the group box lists it with `숨김 풀기`; the header offers `다시 잠그기`.
        f.env.env = f.env.env.copy(unlock = f.env.env.unlock.copy(recordsUnlockedAt = Fx.NOW))
        f.unlock.value++
        v = f.view(this)
        assertEquals("숨긴 기록 1 · 열림", v.screen.openGroup!!.header)
        assertEquals(RecorderCopy.LOCKS_ON_LEAVE, v.screen.openGroup!!.relockNote)
        assertEquals(RecorderCopy.RELOCK, v.screen.headerAction)
        assertEquals(RecorderCopy.UNHIDE, v.openRows.single().row.unhideAction)

        f.writer.unhide(r1)
        v = f.view(this)
        assertTrue(v.rows.any { it.row.recordKey == r1 })
        assertNull(v.screen.lockedGroup)
    }

    @Test
    fun LRC_42_putHereWithTheSwitchOnSavesTheRecordHidden() = runTest {
        val r18 = Fx.p1().let { it.copy(project = it.project.copy(rating = Rating.R18.open())) }
        val f = HubFixture(this, projects = FakeProjects(r18))
        f.hub.begin(EntryRoutes.entryOf(Recording(surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044)), startTyped = false, micGranted = true)
        f.engine.confirm("숨길 생각")
        testScheduler.advanceTimeBy(2_000)
        f.hub.stopAndSave()
        assertTrue(CaptureViews.processing(f.hub.state.value)!!.screen.hideRow!!.on)
        assertNotNull(f.hub.putHere())
        assertTrue(f.recorder.library.records.single().hidden)
    }

    // ---- LRC-43 ----

    @Test
    fun LRC_43_deleteWithUndoAndThePlayerStopsForADeletedRecord() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val done = f.writer.delete(r1)!!
        assertEquals("TODO_COPY_LRC_19", done.toast.text)
        assertFalse(f.view(this).rows.any { it.row.recordKey == r1 })
        assertNotNull(f.recorder.library.record(r1)!!.deletedAt)
        f.writer.undoDelete(done)
        assertTrue(f.view(this).rows.any { it.row.recordKey == r1 })

        val playback = FakePlayback()
        val player = RecordPlayer(backgroundScope, playback) { "/a.aac" }
        player.toggle(r1, RecorderAudio(hash = "a", mime = "audio/aac", bytes = 1, durationMs = 24_000))
        assertTrue(player.state.value.playing(r1))
        // The library still has R1: it keeps playing.
        val library = f.recorder.library
        player.follow(library)
        assertTrue(player.state.value.playing(r1))
        // In the trash, or gone altogether (purged): it stops.
        assertFalse(RecordPlayer.playable(library.copy(records = library.records.map { if (it.key == r1) it.copy(deletedAt = Fx.NOW) else it }), r1))
        assertFalse(RecordPlayer.playable(library.copy(records = library.records.filter { it.key != r1 }), r1))
        player.follow(library.copy(records = library.records.filter { it.key != r1 }))
        assertNull(player.state.value.recordKey)
        assertFalse(player.state.value.playing(r1))
    }
}
