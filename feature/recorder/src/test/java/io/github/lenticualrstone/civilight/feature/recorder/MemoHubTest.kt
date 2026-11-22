package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerRequest
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerTarget
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The text memo (LRC-15 … LRC-17) through the hub: typing as field changes, `@` picks, chips, save, draft, voice ↔ text. */
class MemoHubTest {
    private val home = EntryContext(surface = EntrySurface.PROJECT_HOME, projectKey = Fx.P1)

    private fun HubFixture.text(): String = Runs.text(hub.state.value.memo!!.draft.runs)

    /** Types [added] at the end, as the field reports it. */
    private fun HubFixture.type(added: String) {
        val next = text() + added
        hub.memoTextChanged(next, next.length)
    }

    private fun HubFixture.chips(): List<String> = CaptureViews.memo(hub.state.value)!!.sheet.chips.map { it.text }

    @Test
    fun LRC_T04_atChipLine() = runTest {
        val f = HubFixture(this)
        f.hub.openMemo(home)
        assertEquals(listOf("연결 · 잿빛 새벽의 등불"), f.chips())
        f.type("오웬은 사실 결사 출신? 탈퇴 이유가 ")
        f.hub.openPicker(PickerRequest(PickerTarget.MEMO))
        f.hub.pickTarget(TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.OWEN))
        assertEquals(listOf("연결 · 잿빛 새벽의 등불", "오웬"), f.chips())

        f.type("@세")
        val view = CaptureViews.memo(f.hub.state.value)!!
        assertTrue(view.atOpen)
        val sera = view.atCandidates.first { it.target.key == ExampleKeys.SERA }
        f.hub.memoPickAt(sera)
        f.type("와 관련")
        assertEquals("오웬은 사실 결사 출신? 탈퇴 이유가 세라와 관련", f.text())
        assertEquals(listOf("연결 · 잿빛 새벽의 등불", "오웬", "세라 @"), f.chips())
        val atChip = CaptureViews.memo(f.hub.state.value)!!.sheet.chips.last()
        assertFalse("an @ chip has no ×", atChip.removable)

        // Deleting the `@세라` text removes its link and chip.
        val t = f.text()
        val start = t.indexOf("세라")
        f.hub.memoTextChanged(t.removeRange(start, start + 2), start)
        assertEquals(listOf("연결 · 잿빛 새벽의 등불", "오웬"), f.chips())

        val owen = f.hub.state.value.memo!!.draft.pendingLinks.first { it.targetKey == ExampleKeys.OWEN }
        f.hub.removeLinks(PickerTarget.MEMO, listOf(owen.key))
        assertEquals(listOf("연결 · 잿빛 새벽의 등불"), f.chips())
    }

    @Test
    fun LRC_T06_crossProjectAtImpliesThatProject() = runTest {
        val f = HubFixture(this)
        f.hub.openMemo(EntryContext(surface = EntrySurface.LIBRARY))
        assertTrue(f.chips().isEmpty())
        f.type("@하르")
        val hart = CaptureViews.memo(f.hub.state.value)!!.atCandidates.first { it.target.key == ExampleKeys.HART }
        f.hub.memoPickAt(hart)
        assertEquals(listOf("연결 · 하르트 @"), f.chips())
        assertTrue(f.hub.memoSave())
        val record = f.recorder.library.records.single()
        assertEquals(RecordKind.TEXT.open(), record.kind)
        val link = f.recorder.library.linksOf(record.key).single()
        assertEquals(LinkOrigin.AT, link.origin.known)
        assertEquals(Fx.P1, link.projectKey)
        assertTrue(record.runs.single() is TextRun.Link)
        assertEquals(CaptureStage.IDLE, f.hub.state.value.stage)
    }

    @Test
    fun LRC_15_saveIsOffForBlankTextAndADraftIsKeptOnClose() = runTest {
        val f = HubFixture(this)
        f.hub.begin(EntryRoutes.entryOf(Recording(surface = "projectHome", projectKey = Fx.P1, startTyped = true)), startTyped = true, micGranted = true)
        assertEquals(CaptureStage.TYPING, f.hub.state.value.stage)
        f.type("   ")
        assertFalse(CaptureViews.memo(f.hub.state.value)!!.sheet.canSave)
        assertFalse(f.hub.memoSave())
        f.type("등불 결사 규약")
        assertTrue(CaptureViews.memo(f.hub.state.value)!!.sheet.canSave)
        f.hub.memoClose()
        assertEquals(CaptureStage.IDLE, f.hub.state.value.stage)
        assertNotNull(f.recorder.draft)

        // The draft returns from any entry.
        f.hub.openMemo(EntryContext(surface = EntrySurface.LIBRARY))
        assertEquals("   등불 결사 규약", f.text())
        assertEquals(listOf("연결 · 잿빛 새벽의 등불"), f.chips())
        f.hub.memoSave()
        assertNull(f.recorder.draft)
        assertTrue(f.recorder.library.records.single().runs.isNotEmpty())
    }

    @Test
    fun LRC_16_toTextKeepsTheRecordingAsAVoiceRecord() = runTest {
        val f = HubFixture(this)
        f.hub.begin(EntryContext(surface = EntrySurface.LIBRARY), startTyped = false, micGranted = true)
        val key = f.hub.state.value.session!!.recordKey
        f.engine.confirm("하르트가 등불을")
        f.engine.say("싫어하는 이유")
        testScheduler.advanceTimeBy(9_000)
        f.hub.toText()
        assertEquals(CaptureStage.TYPING, f.hub.state.value.stage)
        assertEquals("하르트가 등불을 싫어하는 이유", f.text())
        f.type(". 대정전")
        f.hub.memoSave()
        val record = f.recorder.library.records.single()
        assertEquals(key, record.key)
        assertEquals(RecordKind.VOICE.open(), record.kind)
        assertEquals(9_000L, record.durationMs)
        assertEquals("하르트가 등불을 싫어하는 이유. 대정전", Runs.text(record.runs))
    }

    @Test
    fun LRC_16_toVoiceStartsWithTheTypedTextAfterThePermission() = runTest {
        val f = HubFixture(this)
        f.hub.openMemo(home)
        f.type("오웬은 사실")
        f.hub.memoToVoice(micGranted = false)
        assertEquals(CaptureStage.MIC_REASON, f.hub.state.value.stage)
        assertTrue(f.hub.state.value.voiceFromMemo)
        f.hub.permissionResult(true)
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        assertEquals("오웬은 사실", CaptureViews.recording(f.hub.state.value)!!.screen.transcript.confirmed)
        assertEquals(listOf("연결 · 잿빛 새벽의 등불"), CaptureViews.recording(f.hub.state.value)!!.screen.chips.map { it.text })
        f.engine.confirm("결사 출신?")
        f.hub.stopAndSave()
        assertEquals("오웬은 사실 결사 출신?", Runs.text(f.recorder.library.records.single().runs))
        assertNull(f.recorder.draft)
    }

    @Test
    fun aMemoInAnotherScreensSheetHandsOverToTheRecordingRoute() = runTest {
        val f = HubFixture(this)
        f.hub.openMemo(home)
        f.type("메모")
        f.hub.requestVoiceFromMemo()
        f.hub.begin(EntryRoutes.entryOf(EntryRoutes.routeOf(home)), startTyped = false, micGranted = true)
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        assertEquals("메모", CaptureViews.recording(f.hub.state.value)!!.screen.transcript.confirmed)
    }
    @Test
    fun LRC_19_aMemoDismissedByItsHostOpensFreshForTheNextEntry() = runTest {
        val f = HubFixture(this)
        f.hub.openMemo(home)
        assertEquals(listOf("연결 · 잿빛 새벽의 등불"), f.chips())
        f.hub.memoSheetGone()
        assertTrue(f.hub.showMemo(EntryContext(surface = EntrySurface.LIBRARY)))
        assertEquals("an empty memo is not carried over", EntrySurface.LIBRARY, f.hub.state.value.memo!!.draft.entry.surface)
        assertEquals(emptyList<String>(), f.chips())
    }

    @Test
    fun LRC_15_aDismissedMemoWithTextComesBackAsItsDraft() = runTest {
        val f = HubFixture(this)
        f.hub.openMemo(home)
        f.type("하층 구역")
        f.hub.memoSheetGone()
        assertTrue(f.hub.showMemo(EntryContext(surface = EntrySurface.LIBRARY)))
        assertEquals("하층 구역", f.text())
        assertEquals(listOf("연결 · 잿빛 새벽의 등불"), f.chips())
    }

    @Test
    fun LRC_20_theStackedPickerKeepsTheMemo() = runTest {
        val f = HubFixture(this)
        f.hub.openMemo(home)
        f.type("메모")
        f.hub.openPicker(PickerRequest(PickerTarget.MEMO))
        f.hub.memoSheetGone()
        f.hub.pickTarget(TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.HART))
        assertTrue(f.hub.showMemo(EntryContext(surface = EntrySurface.LIBRARY)))
        assertEquals("메모", f.text())
        assertEquals(listOf("연결 · 잿빛 새벽의 등불", "하르트"), f.chips())
    }

    @Test
    fun LRC_19_aMemoInsideTheExampleProjectIsMarkedAsExample() = runTest {
        val doc = Fx.p1()
        val f = HubFixture(this, env = FakeEnv(RecorderEnv(listOf(ProjectSnapshot(Fx.entry(doc).copy(isExample = true), doc)), zone = Fx.ZONE)))
        f.hub.openMemo(home)
        assertTrue(f.hub.state.value.memo!!.draft.entry.isExample)
        assertEquals(listOf("연결 · 예시 · 끝나면 연결 없어짐"), f.chips())
    }
}
