package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.K
import io.github.lenticualrstone.civilight.domain.recorder.Fx.withRecord
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxFilters
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxOpening
import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordMenu
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.PageInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.Undo
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.StatusFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LRC §4.3 · States and inbox. */
class StatesAndInboxTest {
    private val env = Fx.env()
    private val all = InboxFilterState()

    @Test
    fun LRC_T09_threeBoardRecords() {
        val library = Fx.openLibrary()
        for (n in 1..3) assertEquals(RecordState.UNSORTED, RecordStates.state(library, library.record(Fx.r(n))!!, env))
        val rows = Inbox.screen(library, all, env, Fx.NOW).rows.associateBy { it.recordKey }
        assertEquals(listOf("1-7 씬 3", "하르트"), rows[Fx.R1]!!.linkTags.map { it.text })
        assertEquals(listOf("오웬", "세라"), rows[Fx.R2]!!.linkTags.map { it.text })
        assertTrue(rows[Fx.R3]!!.linkTags.isEmpty())
        assertEquals(listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기", "+ 연결"), rows[Fx.R3]!!.actions)
        assertTrue(rows.values.none { r -> r.actions.any { it.contains("2장 메모로") } })

        // A record linked only to P1 shows the project name.
        val onlyP1 = LinkEdits.add(library, Fx.R3, TargetRef.project(Fx.P1), LinkOrigin.MANUAL, Fx.ctx())
        val row = Inbox.row(onlyP1, onlyP1.record(Fx.R3)!!, env, Fx.NOW)
        assertEquals(listOf("잿빛 새벽의 등불"), row.linkTags.map { it.text })
        assertEquals(listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기"), row.actions)
    }

    @Test
    fun LRC_T10_filterCounts() {
        val open = Inbox.screen(Fx.openLibrary(), all, env, Fx.NOW)
        assertEquals(listOf("전체 12", "정리 안 됨 3", "넣음", "끝냄", "음성", "글", "프로젝트 ▾"), open.filters.map { it.label })
        assertNull(open.lockedGroup)

        val hidden = Fx.hiddenLibrary()
        val locked = Inbox.screen(hidden, all, env, Fx.NOW)
        assertEquals(listOf("전체 9", "정리 안 됨 2"), locked.filters.take(2).map { it.label })
        val group = locked.lockedGroup!!
        assertEquals(listOf("숨긴 기록 3", "내용 · 날짜 · 길이 가림 · 검색 제외", "지문으로 보기"), listOf(group.title, group.sub, group.button))
        assertTrue(locked.rows.none { it.recordKey in setOf(Fx.R1, Fx.R4, Fx.r(5)) })
        assertNull(locked.headerAction)

        val opened = Inbox.screen(hidden, all, Fx.env(unlock = UnlockSession(recordsUnlockedAt = Fx.NOW)), Fx.NOW)
        assertEquals(listOf("전체 12", "정리 안 됨 3"), opened.filters.take(2).map { it.label })
        val og = opened.openGroup!!
        assertEquals("숨긴 기록 3 · 열림", og.header)
        assertEquals("앱을 떠나면 잠김", og.relockNote)
        assertEquals(listOf(Fx.R1, Fx.R4, Fx.r(5)), og.rows.map { it.recordKey })
        assertTrue(og.rows.all { it.unhideAction == "숨김 풀기" })
        assertEquals("다시 잠그기", opened.headerAction)
        assertNull(opened.lockedGroup)
    }

    @Test
    fun LRC_T11_sort() {
        val rows = Inbox.screen(Fx.openLibrary(), all, env, Fx.NOW).rows.map { it.recordKey }
        assertEquals(listOf(Fx.R1, Fx.R2, Fx.R3, Fx.R4), rows.take(4))
        assertEquals((12 downTo 5).map { Fx.r(it) }, rows.drop(4))
    }

    @Test
    fun LRC_T12_labels() {
        val library = Fx.openLibrary()
        val rows = Inbox.screen(library, all, env, Fx.NOW).rows.associateBy { it.recordKey }
        assertEquals("오늘 08:41", rows[Fx.R1]!!.meta)
        assertEquals("어제 23:10", rows[Fx.R2]!!.meta)
        assertEquals("어제 08:15", rows[Fx.R3]!!.meta)
        assertEquals("11월 18일", rows[Fx.R4]!!.meta)
        assertEquals("음성 0:24", rows[Fx.R1]!!.kindLabel)
        assertEquals("음성 1:02", rows[Fx.R3]!!.kindLabel)
        assertEquals("글", rows[Fx.R2]!!.kindLabel)
        assertEquals("00:12", RecorderTimeLabels.timer(12_000))
    }

    @Test
    fun LRC_T13_doneAndBack() {
        val ctx = Fx.ctx()
        val library = Fx.openLibrary()
        val r3 = library.record(Fx.R3)!!
        assertTrue(RecordMenu.options(library, r3, env).any { it.action == MenuAction.MARK_DONE })
        val (done, toast) = RecordMenu.markDone(library, Fx.R3, env, ctx)
        assertEquals(RecordState.DONE, RecordStates.state(done, done.record(Fx.R3)!!, env))
        assertEquals(listOf("전체 12", "정리 안 됨 2"), Inbox.screen(done, all, env, Fx.NOW).filters.take(2).map { it.label })
        assertEquals(listOf("되돌리기"), toast.actions)

        val undone = RecordMenu.undoMarkDone(done, Fx.R3, ctx)
        assertEquals(RecordState.UNSORTED, RecordStates.state(undone, undone.record(Fx.R3)!!, env))

        val again = RecordMenu.markDone(undone, Fx.R3, env, ctx).first
        val menu = RecordMenu.options(again, again.record(Fx.R3)!!, env)
        assertTrue(menu.any { it.action == MenuAction.UNMARK_DONE && it.title == "다시 정리 안 됨" })
        assertTrue(menu.none { it.action == MenuAction.MARK_DONE })
        val back = RecordMenu.unmarkDone(again, Fx.R3, ctx)
        assertEquals(RecordState.UNSORTED, RecordStates.state(back, back.record(Fx.R3)!!, env))

        val r5 = library.record(Fx.r(5))!!
        assertEquals(RecordState.PUT, RecordStates.state(library, r5, env))
        assertTrue(RecordMenu.options(library, r5, env).none { it.action == MenuAction.MARK_DONE || it.title == "끝냄으로 표시" })
    }

    @Test
    fun LRC_T14_insertIntoDoneRecord() {
        val library = Fx.openLibrary()
        assertEquals(RecordState.DONE, RecordStates.state(library, library.record(Fx.R4)!!, env))
        val p1 = Fx.p1Doc()
        val ins = PageInsert.insert(p1, library, Fx.R4, "", Fx.ctx())
        val envAfter = env.withDoc(ins.doc)
        assertEquals(RecordState.PUT, RecordStates.state(ins.library, ins.library.record(Fx.R4)!!, envAfter))
        assertFalse(ins.library.record(Fx.R4)!!.doneMarked)

        val (doc, lib) = Undo.undo(ins.doc, ins.library, ins.undo)
        assertEquals(RecordState.DONE, RecordStates.state(lib, lib.record(Fx.R4)!!, env.withDoc(doc)))
        assertEquals(p1.pages, doc.pages)
    }

    @Test
    fun LRC_T15_targetLineCount() {
        val library = Fx.openLibrary()
        assertEquals(1, RecorderHooks.unsortedLinkedCount(library, Fx.sceneS, env))
        assertEquals("레코더 · 정리 안 됨 1 ›", RecorderHooks.targetLine(RecorderHooks.unsortedLinkedCount(library, Fx.sceneS, env)))
        assertEquals(1, RecorderHooks.unsortedLinkedCount(library, Fx.hart, env))
        assertEquals(1, RecorderHooks.unsortedLinkedCount(library, Fx.sera, env))
        val rien = Fx.target(RecorderTargetKind.CHARACTER, K.RIEN)
        assertEquals(0, RecorderHooks.unsortedLinkedCount(library, rien, env))
        assertNull(RecorderHooks.targetLine(0))

        val hidden = Fx.hiddenLibrary()
        assertEquals(0, RecorderHooks.unsortedLinkedCount(hidden, Fx.sceneS, env))
        assertNull(RecorderHooks.targetLine(RecorderHooks.unsortedLinkedCount(hidden, Fx.sceneS, env)))

        val preset = InboxFilters.preset(InboxOpening.FromTarget(Fx.sceneS))
        assertEquals(StatusFilter.UNSORTED, preset.status)
        val screen = Inbox.screen(library, preset, env, Fx.NOW)
        assertEquals(listOf(Fx.R1), screen.rows.map { it.recordKey })
        assertEquals("1-7 씬 3 ×", screen.filters.first().label)
        val cleared = InboxFilters.clearTarget(preset)
        assertNull(cleared.target)
        assertEquals(ProjectFilter.All, cleared.project)
    }

    @Test
    fun doneMarkIsClearedByAnInsertAndKeptByLinks() {
        val library = Fx.openLibrary().withRecord(Fx.R3) { it.copy(doneMarked = true) }
        val linked = LinkEdits.add(library, Fx.R3, Fx.hart, LinkOrigin.MANUAL, Fx.ctx())
        assertEquals(RecordState.DONE, RecordStates.state(linked, linked.record(Fx.R3)!!, env))
    }
}
