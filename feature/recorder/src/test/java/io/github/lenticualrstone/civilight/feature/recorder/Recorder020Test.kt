package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.designsystem.icon.IconClass
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.inbox.EmptyInbox
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.capture.LeaveRules
import io.github.lenticualrstone.civilight.feature.recorder.capture.NotificationAsk
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderEntryPoints
import io.github.lenticualrstone.civilight.feature.recorder.inbox.EmptyInboxView
import io.github.lenticualrstone.civilight.feature.recorder.settings.RecorderSettingId
import io.github.lenticualrstone.civilight.feature.recorder.settings.RecorderSettingsRows
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.navigation.Recording
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 0.2.0 rows of this lane: LRC-48 (entry points), LRC-49 (leaving, task removal), LRC-50 (masked label), icon list B24. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class Recorder020Test {
    private val fromHome = Recording(projectKey = Fx.P1)
    private val fromInbox = EntryRoutes.routeOf(null).copy(surface = "inbox")

    // ---- LRC-48 · entry points ----

    @Test
    fun LRC_0_2_01_onlyHomePassesOnMic() {
        // S-8 on the screens: every feature's main sources, only story's HomeScreen passes a command-bar mic.
        val features = java.io.File("..").canonicalFile
        val sites = features.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.path.contains("/src/main/") }
            .filter { f -> f.readLines().any { it.trimStart().startsWith("onMic =") } }
            .map { it.name }
            .toList()
        assertEquals(listOf("HomeScreen.kt"), sites)
    }

    @Test
    fun LRC_0_2_01_theMicIsOnlyOnHome() {
        // Reference rule only: no command bar reads RecorderEntryPoints yet, so this does NOT cover what the
        // screens draw. S-8's "the mic appears only on home" is met only when every other lane drops onMic.
        val screens = RecorderEntryPoints.Screen.entries
        assertEquals(listOf(RecorderEntryPoints.Screen.HOME), screens.filter(RecorderEntryPoints::commandBarMic))
        assertEquals(
            listOf(RecorderEntryPoints.Screen.HOME, RecorderEntryPoints.Screen.INBOX),
            screens.filter(RecorderEntryPoints::opensRecorder),
        )
        assertTrue(RecorderEntryPoints.Outside.entries.none(RecorderEntryPoints::offered))
        // The home mic's route (HomeScreen pushes `Recording(projectKey)`) pre-fills that project only (LRC-19 row `Project home`).
        val home = EntryRoutes.entryOf(fromHome)
        assertEquals(Fx.P1, home.projectKey)
        assertNull(home.targetKey)
        assertFalse(home.onLockScreen)
        assertEquals(EntrySurface.INBOX, EntryRoutes.entryOf(fromInbox).surface)
    }

    @Test
    fun LRC_48_noTileWidgetOrLockScreenSurfaceIsDeclared() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val declared = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL).replace(manifest, "")
        assertFalse("tile", "RecorderTileService" in declared)
        assertFalse("tile action", "QS_TILE" in declared)
        assertFalse("widget", "RecorderWidgetProvider" in declared)
        assertFalse("widget action", "APPWIDGET" in declared)
        assertFalse("show-when-locked", "showWhenLocked" in declared)
        // The background notification (LRC-09, D32) stays.
        assertTrue("RecordingService" in declared)
        assertTrue("foregroundServiceType=\"microphone\"" in declared)
    }

    @Test
    fun LRC_0_2_02_noLockScreenRowAndItsValueIsKept() {
        val stored = AppSettings().let { it.copy(recorder = it.recorder.copy(lockScreenRecording = false)) }
        val rows = RecorderSettingsRows.of(stored)
        assertTrue(rows.none { it.id == RecorderSettingId.LOCK_SCREEN })
        assertTrue(rows.none { it.row.title == "잠금 화면에서 녹음" })
        assertTrue(rows.none { "위젯" in it.row.title || "타일" in it.row.title })
        // A write through any drawn row keeps the hidden value.
        var s = stored
        for (row in rows) s = RecorderSettingsRows.apply(s, row.id, !row.row.on)
        assertFalse(s.recorder.lockScreenRecording)
    }

    @Test
    fun LRC_48_homeAndInboxStartAtOnceWhateverTheHiddenLockScreenValue() = runTest {
        for (route in listOf(fromHome, fromInbox)) {
            val f = HubFixture(this, takes = FakeTakes())
            f.settings = f.settings.copy(recorder = f.settings.recorder.copy(lockScreenRecording = false))
            f.hub.begin(EntryRoutes.entryOf(route), startTyped = false, micGranted = true)
            assertNull("no device unlock asked", f.hub.state.value.unlock)
            assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
            f.hub.leave()!!.join()
        }
    }

    // ---- LRC-49 · leaving the recorder ----

    @Test
    fun LRC_49_whenTheRouteLeavingSavesTheTake() {
        val home = io.github.lenticualrstone.civilight.navigation.ProjectHome(Fx.P1)
        // Popped back to home inside the app.
        assertTrue(LeaveRules.leaveOnDispose(false, false, true, listOf(home)))
        // Another screen pushed over the recorder (the recorder stays below it).
        assertTrue(LeaveRules.leaveOnDispose(false, false, true, listOf(home, fromHome, RecorderRecordDetail("R"))))
        assertTrue(LeaveRules.leaveOnDispose(false, false, true, listOf(home, fromHome, RecorderInbox())))
        // The notification opened the recorder again: the same capture is on top.
        assertFalse(LeaveRules.leaveOnDispose(false, false, true, listOf(home, fromHome, Recording(surface = "projectHome", projectKey = Fx.P1))))
        // Fold or rotation.
        assertFalse(LeaveRules.leaveOnDispose(true, false, true, listOf(home)))
        assertFalse(LeaveRules.leaveOnDispose(true, true, true, listOf(home)))
        // Background (D32): the system tore the screen down while the app was not started.
        assertFalse(LeaveRules.leaveOnDispose(false, false, false, listOf(home)))
        assertFalse(LeaveRules.leaveOnDispose(false, false, false, listOf(home, fromHome)))
        // The activity finishes.
        assertTrue(LeaveRules.leaveOnDispose(false, true, false, listOf(home, fromHome)))
    }

    @Test
    fun LRC_49_removingTheTaskSavesARecordingOrPausedTakeOnce() = runTest {
        for (pause in listOf(false, true)) {
            val takes = FakeTakes()
            val f = HubFixture(this, takes = takes)
            f.hub.begin(EntryRoutes.entryOf(fromHome), startTyped = false, micGranted = true)
            f.engine.confirm("밤에 든 생각")
            testScheduler.advanceTimeBy(2_000)
            if (pause) {
                f.hub.togglePause()
                assertEquals(RecordingPhase.PAUSED, f.hub.state.value.session!!.phase)
            }
            val save = LeaveRules.onTaskRemoved(f.hub)
            assertNotNull(save)
            assertFalse("released before the save is awaited", takes.micOpen)
            // The route's own dispose runs too (the activity finishing): nothing more is saved.
            assertNull(f.hub.leave())
            assertNotNull("set before the save starts, so the service waits for it", f.hub.leaving)
            save!!.join()
            assertTrue("written: the service may stop", f.hub.leaving!!.isCompleted)
            assertEquals(CaptureStage.IDLE, f.hub.state.value.stage)
            assertEquals(1, f.recorder.library.records.size)
            assertEquals("밤에 든 생각", io.github.lenticualrstone.civilight.domain.recorder.text.Runs.text(f.recorder.library.records.single().runs))
            assertEquals(takes.opens, takes.closes)
            assertNull("idle: nothing to save", LeaveRules.onTaskRemoved(f.hub))
        }
    }

    // ---- LRC-50 · masked link label ----

    @Test
    fun LRC_0_2_03_theMaskedLabelNamesNoRating() {
        assertEquals("이름 가림", LinkViews.maskedLabel())
        assertEquals(RecorderCopy.MASKED_NAME, LinkViews.maskedLabel())
        assertFalse("R-18" in LinkViews.maskedLabel())
    }

    // ---- icon list B24 · EmptyLr ----

    @Test
    fun B24_theEmptyInboxHasIconButtonsAndNoOutsideEntrySub() {
        val view = EmptyInboxView.of(EmptyInbox())
        assertNull("TODO_COPY_LRC_EMPTY_SUB: nothing drawn", view.sub)
        assertEquals("기록 없음", view.title)
        assertEquals("전체 0", view.chip)
        assertEquals(listOf("말로 기록", "글로 쓰기"), listOf(view.voice.text, view.text.text))
        assertEquals(listOf(IconClass.B, IconClass.B), listOf(view.voice.cls, view.text.cls))
        assertEquals(listOf(IconId.Mic, IconId.Text), listOf(view.voice.icon, view.text.icon))
        assertEquals(listOf("말로 기록", "글로 쓰기"), listOf(view.voice.accessibilityName, view.text.accessibilityName))
    }

    // ---- U-13 · D32 · LRC-09: the recording notification's permission ----

    @Test
    fun U13_notificationPermissionIsAskedOnceWhenATakeFirstRecordsOnApi33Up() {
        fun ask(sdk: Int = 34, granted: Boolean = false, stage: CaptureStage = CaptureStage.RECORDING, asked: Boolean = false, touring: Boolean = false) =
            NotificationAsk.shouldAsk(sdk, granted, stage, asked, touring)
        assertTrue(ask())
        assertTrue(ask(sdk = 33))
        assertFalse("below API 33 there is nothing to ask", ask(sdk = 32))
        assertFalse("already granted", ask(granted = true))
        assertFalse("asked once this run", ask(asked = true))
        assertFalse("not over a tour card", ask(touring = true))
        CaptureStage.entries.filter { it != CaptureStage.RECORDING }.forEach { assertFalse(it.name, ask(stage = it)) }
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val declared = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL).replace(manifest, "")
        assertTrue("android.permission.POST_NOTIFICATIONS" in declared)
    }

    // ---- S-4 · LRC-50: no rating in the 넣기 prompt title; icon list: 기록함 search is the search glyph ----

    @Test
    fun S4_theProtectedDestinationPromptNamesNoRating() {
        assertFalse("R-18" in RecorderUiCopy.PROTECTED_PROMPT)
        assertEquals(RecorderCopy.MASKED_NAME, RecorderUiCopy.PROTECTED_PROMPT)
        val inbox = File("src/main/java/io/github/lenticualrstone/civilight/feature/recorder/inbox/InboxScreen.kt").readText()
        assertTrue(Regex("""CivilightIconButton\(IconId\.Search, RecorderUiCopy\.SEARCH_ICON""").containsMatchIn(inbox))
        assertFalse("IconId.Filter" in inbox)
    }
}
