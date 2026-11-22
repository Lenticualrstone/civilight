package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.recorder.inbox.FilterAction
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxOpening
import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxController
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxRoutes
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxView
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxViews
import io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuOutcome
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordWriter
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.KindFilter
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.StatusFilter
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A 기록함 controller over fakes: the example's recorder library and project, the spec's clock. */
class InboxFixture(
    scope: TestScope,
    library: RecorderLibrary = ExampleRecorder.library(),
    preset: InboxFilterState = InboxFilterState(),
    relock: RelockPolicy = RelockPolicy.ON_LEAVE,
) {
    val recorder = FakeRecorder(library, ExampleRecorder.trashEntries())
    val projects = FakeProjects(Fx.p1())
    val env = FakeEnv(Fx.env(projects.doc))
    val unlock = MutableStateFlow(0)
    private val keys = Fx.keys(100)
    val writer = RecordWriter(recorder, projects, env) { ActionContext(now = Fx.NOW, zone = Fx.ZONE, device = Fx.DEVICE, newKey = keys) }
    val controller = InboxController(
        scope = scope.backgroundScope,
        recorder = recorder,
        envSource = env,
        writer = writer,
        now = { Fx.NOW },
        relockPolicy = { relock },
        unlockChanges = unlock,
        preset = preset,
    )

    fun view(scope: TestScope): InboxView {
        scope.runCurrent()
        return controller.view.value!!
    }
}

class InboxTest {
    private val r1 = ExampleKeys.RECORDER_R1
    private val r2 = ExampleKeys.RECORDER_R2
    private val r3 = ExampleKeys.RECORDER_R3
    private val r4 = ExampleKeys.RECORDER_R4

    @Test
    fun LRC_T11_newestFirstOnly() = runTest {
        val v = InboxFixture(this).view(this)
        assertEquals(
            listOf(r1, r2, r3, r4, ExampleKeys.RECORDER_R0, ExampleKeys.RECORDER_NOAH),
            v.rows.map { it.row.recordKey },
        )
    }

    @Test
    fun LRC_T09_T12_boardRowsAndLabels() = runTest {
        val rows = InboxFixture(this).view(this).rows.associateBy { it.row.recordKey }
        val row1 = rows.getValue(r1).row
        assertEquals("음성 0:24", row1.kindLabel)
        assertEquals("오늘 08:41", row1.meta)
        assertEquals("정리 안 됨", row1.stateLabel)
        assertEquals(listOf("1-7 씬 3", "하르트"), row1.linkTags.map { it.label })
        assertEquals(listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기"), row1.actions)

        val row2 = rows.getValue(r2).row
        assertEquals("글", row2.kindLabel)
        assertEquals("어제 23:10", row2.meta)
        assertEquals(listOf("오웬", "세라"), row2.linkTags.map { it.label })

        val row3 = rows.getValue(r3).row
        assertEquals("음성 1:02", row3.kindLabel)
        assertEquals("어제 08:15", row3.meta)
        assertTrue(row3.linkTags.isEmpty())
        assertEquals(listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기", "+ 연결"), row3.actions)
        assertEquals("11월 18일", rows.getValue(r4).row.meta)
        assertTrue(rows.values.none { v -> v.row.actions.any { it.contains("메모로") } })
    }

    @Test
    fun LRC_T10_filterRowCountsAndStates() = runTest {
        val v = InboxFixture(this).view(this)
        // R0 is 넣음 (its page), R4 끝냄; the other four are 정리 안 됨.
        assertEquals(listOf("전체 6", "정리 안 됨 4", "넣음", "끝냄", "음성", "글", "프로젝트 ▾"), v.screen.filters.map { it.label })
        assertEquals(RecordState.PUT, v.rows.single { it.row.recordKey == ExampleKeys.RECORDER_R0 }.row.state)
        assertEquals(RecordState.DONE, v.rows.single { it.row.recordKey == r4 }.row.state)
        assertNull(v.screen.lockedGroup)
        assertEquals(listOf("글로 쓰기", "말로 기록"), v.screen.bottomButtons)
    }

    @Test
    fun LRC_27_statusAndKindChips() = runTest {
        val f = InboxFixture(this)
        f.controller.onFilter(FilterAction.Status(StatusFilter.DONE))
        assertEquals(listOf(r4), f.view(this).rows.map { it.row.recordKey })
        f.controller.onFilter(FilterAction.Status(StatusFilter.UNSORTED))
        f.controller.onFilter(FilterAction.Kind(KindFilter.VOICE))
        val voice = f.view(this)
        assertEquals(listOf(r1, r3), voice.rows.map { it.row.recordKey })
        assertEquals("전체 2", voice.screen.filters.first().label)
        // A second tap on the same kind turns it off (none on = both kinds).
        f.controller.onFilter(FilterAction.Kind(KindFilter.VOICE))
        assertEquals(KindFilter.NONE, f.view(this).filter.kind)
    }

    @Test
    fun LRC_T03_subLinkImpliesTheProject() = runTest {
        val f = InboxFixture(this)
        f.controller.setProject(ProjectFilter.Project(Fx.P1))
        val inP1 = f.view(this).rows.map { it.row.recordKey }
        assertTrue(r1 in inP1 && r2 in inP1)
        assertFalse(r3 in inP1)
        assertEquals("잿빛 새벽의 등불 ▾", f.view(this).screen.filters.last().label)
        f.controller.setProject(ProjectFilter.NoLink)
        val noLink = f.view(this).rows.map { it.row.recordKey }
        assertTrue(r3 in noLink)
        assertFalse(r1 in noLink || r2 in noLink)
        assertEquals(listOf("모든 프로젝트", "잿빛 새벽의 등불", "연결 없음"), f.controller.projectMenu().map { it.label })
    }

    @Test
    fun LRC_27_routePresets() {
        assertEquals(InboxOpening.FromLibrary, InboxRoutes.opening(RecorderInbox()))
        assertEquals(InboxOpening.FromProject(Fx.P1), InboxRoutes.opening(RecorderInbox(projectKey = Fx.P1)))
        assertEquals(
            InboxOpening.FromTarget(TargetRef(Fx.P1, RecorderTargetKind.UNIT, Fx.S)),
            InboxRoutes.opening(RecorderInbox(projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S)),
        )
        assertEquals(
            InboxOpening.FromTarget(TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.HART)),
            InboxRoutes.opening(RecorderInbox(projectKey = Fx.P1, targetKind = "CHARACTER", targetKey = ExampleKeys.HART)),
        )
        val fromProject = InboxRoutes.preset(RecorderInbox(projectKey = Fx.P1))
        assertEquals(StatusFilter.UNSORTED, fromProject.status)
        assertEquals(ProjectFilter.Project(Fx.P1), fromProject.project)
        assertEquals(InboxFilterState(), InboxRoutes.preset(RecorderInbox()))
        assertEquals(StatusFilter.DONE, InboxRoutes.preset(RecorderInbox(filter = "done")).status)
        assertEquals(ProjectFilter.NoLink, InboxRoutes.preset(RecorderInbox(filter = "noLink")).project)
    }

    @Test
    fun LRC_T15_targetOpeningListsThatTargetsUnsortedRecords() = runTest {
        val f = InboxFixture(this, preset = InboxRoutes.preset(RecorderInbox(projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S)))
        val v = f.view(this)
        assertEquals(listOf(r1), v.rows.map { it.row.recordKey })
        assertEquals("1-7 씬 3 ×", v.screen.filters.first().label)
        f.controller.onFilter(FilterAction.ClearTarget)
        val cleared = f.view(this)
        assertNull(cleared.filter.target)
        assertEquals(ProjectFilter.All, cleared.filter.project)
    }

    @Test
    fun LRC_19_captureFromTheInboxPrefillsTheFilterProject() = runTest {
        val f = InboxFixture(this)
        assertNull(f.view(this).captureEntry.projectKey)
        f.controller.setProject(ProjectFilter.Project(Fx.P1))
        assertEquals(Fx.P1, f.view(this).captureEntry.projectKey)
    }

    @Test
    fun LRC_T13_doneAndBack() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val menu = f.controller.menu(r3)!!
        assertTrue(menu.options.any { it.option.action == MenuAction.MARK_DONE })
        val outcome = f.controller.onMenu(r3, MenuAction.MARK_DONE)
        assertTrue(outcome is MenuOutcome.ShowToast)
        assertEquals("TODO_COPY_LRC_11", (outcome as MenuOutcome.ShowToast).toast.text)
        val marked = f.view(this)
        assertEquals(RecordState.DONE, marked.rows.single { it.row.recordKey == r3 }.row.state)
        assertEquals("정리 안 됨 3", marked.screen.filters[1].label)
        outcome.undo()
        assertEquals(RecordState.UNSORTED, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)

        f.controller.onMenu(r3, MenuAction.MARK_DONE)
        f.view(this)
        assertTrue(f.controller.menu(r3)!!.options.any { it.option.action == MenuAction.UNMARK_DONE })
        f.controller.onMenu(r3, MenuAction.UNMARK_DONE)
        assertEquals(RecordState.UNSORTED, f.view(this).rows.single { it.row.recordKey == r3 }.row.state)
        // Not offered on a 넣음 record (R0, its page).
        assertTrue(f.controller.menu(ExampleKeys.RECORDER_R0)!!.options.none { it.option.action == MenuAction.MARK_DONE })
    }

    @Test
    fun LRC_29_menuPreviewOrderAndWhatRunsInThisBuild() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val menu = f.controller.menu(r1)!!
        assertEquals("음성 0:24 · 오늘 08:41", menu.preview.meta)
        assertEquals(
            listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기", "메모 칸에 넣기", "인물로 만들기", "끝냄으로 표시", "숨기기", "지우기"),
            menu.options.map { it.option.title },
        )
        assertEquals(
            listOf(true, true, true, true, true, true, true, true),
            menu.options.map { it.enabled },
        )
        assertEquals("위치 고르기 · 연결된 씬 · 마지막으로 쓰던 곳", menu.options.first().option.sub)
        assertTrue(f.controller.onMenu(r1, MenuAction.PUT_MANUSCRIPT) is MenuOutcome.OpenPut)
    }

    @Test
    fun LRC_29_hideLeavesTheListIntoTheLockedGroup() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        f.controller.onMenu(r1, MenuAction.HIDE)
        val v = f.view(this)
        assertFalse(v.rows.any { it.row.recordKey == r1 })
        assertEquals("숨긴 기록 1", v.screen.lockedGroup!!.title)
        assertEquals("전체 5", v.screen.filters.first().label)
        assertNull(f.controller.menu(r1))
    }

    @Test
    fun LRC_29_deleteGoesToTheTrashAndUndoBringsItBack() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        val outcome = f.controller.onMenu(r2, MenuAction.DELETE) as MenuOutcome.ShowToast
        assertEquals(ShownCopy.DELETED_TOAST, outcome.toast.text)
        val record = f.recorder.library.record(r2)!!
        assertNotNull(record.deletedAt)
        assertTrue(f.recorder.trash.any { r2 in it.targetKeys })
        assertFalse(f.view(this).rows.any { it.row.recordKey == r2 })
        outcome.undo()
        assertNull(f.recorder.library.record(r2)!!.deletedAt)
        assertTrue(f.recorder.trash.none { r2 in it.targetKeys })
        assertEquals(3, f.recorder.library.linksOf(r2).size)
        assertTrue(f.view(this).rows.any { it.row.recordKey == r2 })
    }

    @Test
    fun LRC_20_rowAddLinkOnASavedRecord() = runTest {
        val f = InboxFixture(this)
        f.view(this)
        f.controller.addLink(r3, TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.HART))
        f.controller.addLink(r3, TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.HART))
        val row = f.view(this).rows.single { it.row.recordKey == r3 }.row
        assertEquals(listOf("하르트"), row.linkTags.map { it.label })
        assertEquals(listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기"), row.actions)
    }

    @Test
    fun LRC_33_putIsOffForEmptyTextAndWithoutAWritableProject() = runTest {
        val empty = RecorderRecord(key = "01JZ000000TSTK000000000900", kind = RecordKind.VOICE.open(), createdAt = Fx.NOW - 1000, durationMs = 5_000)
        val library = ExampleRecorder.library().let { it.copy(records = it.records + empty) }
        val f = InboxFixture(this, library)
        val rows = f.view(this).rows.associateBy { it.row.recordKey }
        assertFalse(rows.getValue(empty.key).actions.putManuscript)
        assertTrue(rows.getValue(r1).actions.putManuscript)
        // The read-only example is not a destination: nothing can be put anywhere.
        val readOnly = InboxViews.of(library, InboxFilterState(), Fx.env(Fx.p1()).let { e ->
            e.copy(projects = e.projects.map { it.copy(entry = it.entry.copy(readOnly = true, isExample = true)) })
        }, Fx.NOW)
        assertTrue(readOnly.rows.none { it.actions.putManuscript })
    }

    @Test
    fun LRC_25_searchNarrowsTheRows() = runTest {
        val f = InboxFixture(this)
        f.controller.search("등불")
        assertEquals(listOf(r1, r4, ExampleKeys.RECORDER_R0), f.view(this).rows.map { it.row.recordKey })
        f.controller.search("없는 말")
        assertTrue(f.view(this).filteredEmpty)
        f.controller.search("")
        assertEquals(6, f.view(this).rows.size)
    }

    @Test
    fun LRC_30_emptyLibraryShowsEmptyLr() = runTest {
        val v = InboxFixture(this, RecorderLibrary()).view(this)
        assertTrue(v.empty)
        assertEquals("전체 0", v.screen.empty!!.chip)
        assertEquals(listOf("말로 기록", "글로"), v.screen.empty!!.buttons)
        assertFalse(v.filteredEmpty)
    }
}
