package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureSheetKind
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureSheets
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureSlot
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureState
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerRequest
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerTarget
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxKind
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxKinds
import io.github.lenticualrstone.civilight.feature.recorder.link.Present
import io.github.lenticualrstone.civilight.feature.recorder.link.Presence
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

/** Motion audit §4 `feature:recorder` (the pure parts and the lint) and the copy that reaches the screen. */
class MotionAndCopyTest {
    private fun constants(type: Class<*>): Map<String, String> = type.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
        .associate { field ->
            field.isAccessible = true
            field.name to field.get(null) as String
        }

    // ---- copy ----

    @Test
    fun everyTodoIdOfTheDomainResolvesToCopyThatPassesTheLint() {
        val todos = constants(RecorderCopy::class.java).filterKeys { it.startsWith("TODO_") }.values.toSet()
        assertEquals(todos, ShownCopy.ids)
        for (id in todos) {
            val shown = ShownCopy.of(id)
            assertFalse("$id still raw", shown.contains("TODO_COPY"))
            // `TODO_COPY_LRC_10` is the other-year date format, not copy.
            if (id != RecorderCopy.TODO_OTHER_YEAR) assertEquals("$id = \"$shown\"", emptyList<String>(), CopyLint.lint(shown, isButton = false))
        }
        assertEquals("메뉴", ShownCopy.of(RecorderCopy.TODO_MENU_ARIA))
        assertEquals("글", ShownCopy.of("글"))
    }

    @Test
    fun aToastShowsItsCopyAndKeepsItsActions() {
        val toast = Toast(RecorderCopy.TODO_CARD_TOAST, listOf(RecorderCopy.UNDO))
        assertEquals("카드에 넣음", ShownCopy.of(toast))
        assertEquals(listOf(RecorderCopy.UNDO), toast.actions)
    }

    @Test
    fun theFeatureCopyAndItsSourcesHoldNoRawTodoId() {
        assertTrue(constants(RecorderUiCopy::class.java).values.none { it.contains("TODO_COPY") })
        val leaks = File("src/main").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file -> file.readLines().mapIndexed { i, line -> Triple(file, i + 1, line) } }
            .filter { (_, _, line) -> line.contains("\"TODO_COPY") || line.contains("RecorderCopy.TODO_") }
            .map { (file, i, line) -> "${file.path}:$i ${line.trim()}" }
            .toList()
        assertEquals(leaks.joinToString("\n"), 0, leaks.size)
    }

    @Test
    fun theReLockNoteFollowsThePolicyInTheLibrarysWords() = runTest {
        assertEquals(RecorderCopy.LOCKS_ON_LEAVE, ShownCopy.relockNote(RelockPolicy.ON_LEAVE.name))
        assertEquals("화면이 꺼지면 잠김", ShownCopy.relockNote(RelockPolicy.SCREEN_OFF_ONLY.name))
        assertEquals("5분 뒤 잠김", ShownCopy.relockNote(RelockPolicy.FIVE_MINUTES.name))
        val f = InboxFixture(this, relock = RelockPolicy.FIVE_MINUTES)
        f.view(this)
        f.writer.hide(ExampleKeys.RECORDER_R1)
        f.env.env = f.env.env.copy(unlock = f.env.env.unlock.copy(recordsUnlockedAt = Fx.NOW))
        f.unlock.value++
        assertEquals("5분 뒤 잠김", f.view(this).screen.openGroup!!.relockNote)
    }

    // ---- presence (chips that come and go) ----

    private fun merge(previous: List<Present<String>>, next: List<String>) = Presence.merge(previous, next) { it }

    @Test
    fun aNewMemberIsAliveAndALeavingOneStaysWhereItWas() {
        val first = merge(emptyList(), listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), first.map { it.item })
        assertTrue(first.all { it.alive })
        val second = merge(first, listOf("a", "c"))
        assertEquals(listOf("a", "b", "c"), second.map { it.item })
        assertEquals(listOf(true, false, true), second.map { it.alive })
        val third = merge(second, listOf("a", "c", "d"))
        assertEquals(listOf("a", "b", "c", "d"), third.map { it.item })
        assertEquals(listOf(true, false, true, true), third.map { it.alive })
    }

    @Test
    fun aMemberThatComesBackWhileLeavingIsAliveAgain() {
        val leaving = merge(merge(emptyList(), listOf("a", "b")), listOf("a"))
        assertFalse(leaving[1].alive)
        val back = merge(leaving, listOf("a", "b"))
        assertEquals(listOf(true, true), back.map { it.alive })
    }

    // ---- inbox body and capture stage keys ----

    @Test
    fun theInboxBodyKindFollowsTheView() = runTest {
        assertEquals(InboxKind.LOADING, InboxKinds.of(null))
        assertEquals(InboxKind.ROWS, InboxKinds.of(InboxFixture(this).view(this)))
        val empty = InboxFixture(this, library = io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary()).view(this)
        assertEquals(InboxKind.EMPTY, InboxKinds.of(empty))
    }

    @Test
    fun theCaptureSheetIsOneSheetWhoseContentSwaps() {
        assertNull(CaptureSheets.of(CaptureState(stage = CaptureStage.RECORDING)))
        assertEquals(CaptureSheetKind.MEMO, CaptureSheets.of(CaptureState(stage = CaptureStage.TYPING)))
        assertEquals(CaptureSheetKind.MIC_REASON, CaptureSheets.of(CaptureState(stage = CaptureStage.MIC_REASON)))
        // The picker wins over the memo it opened from, and over a capture pane (`+ 연결` on `녹음 중`).
        val picker = PickerRequest(PickerTarget.MEMO)
        assertEquals(CaptureSheetKind.PICKER, CaptureSheets.of(CaptureState(stage = CaptureStage.TYPING, picker = picker)))
        assertEquals(CaptureSheetKind.PICKER, CaptureSheets.of(CaptureState(stage = CaptureStage.RECORDING, picker = picker)))
    }

    @Test
    fun aStageSwapKeysOnTheStageNotOnTheView() {
        assertNull(CaptureSlot.of(CaptureState(stage = CaptureStage.TYPING)).kind)
        assertNull(CaptureSlot.of(CaptureState(stage = CaptureStage.IDLE)).kind)
        assertEquals(CaptureStage.MIC_OFF, CaptureSlot.of(CaptureState(stage = CaptureStage.MIC_OFF)).kind)
        // A stage whose view is not there yet draws nothing instead of a blank pane.
        assertEquals(CaptureSlot.None, CaptureSlot.of(CaptureState(stage = CaptureStage.RECORDING)))
    }
}
