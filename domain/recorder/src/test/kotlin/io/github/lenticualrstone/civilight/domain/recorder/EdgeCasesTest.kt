package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.K
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.capture.TextMemo
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxFilters
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordMenu
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkViews
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkPicker
import io.github.lenticualrstone.civilight.domain.recorder.picker.PickerMode
import io.github.lenticualrstone.civilight.domain.recorder.put.MemoFieldInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.Undo
import io.github.lenticualrstone.civilight.domain.recorder.settings.RecorderSettingsScreen
import io.github.lenticualrstone.civilight.domain.recorder.text.AtMentions
import io.github.lenticualrstone.civilight.domain.recorder.text.Hangul
import io.github.lenticualrstone.civilight.domain.recorder.text.RecordText
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.KindFilter
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Own edge cases beyond the §4 rule cases. */
class EdgeCasesTest {
    private val env = Fx.env()
    private val link = Runs.linkRun("01JZ000000TSTK000000000001", LinkTargetKind.CHARACTER, K.SERA, Fx.P1, "세라")

    @Test
    fun runsReplaceKeepsLinksAtomic() {
        val runs = listOf(TextRun.Plain("탈퇴 이유가 "), link, TextRun.Plain("와 관련"))
        assertEquals("탈퇴 이유가 세라와 관련", Runs.text(runs))
        // Insert before and after the link: the link survives.
        val before = Runs.insertText(runs, 7, "X")
        assertTrue(before.any { it is TextRun.Link })
        assertEquals("탈퇴 이유가 X세라와 관련", Runs.text(before))
        val after = Runs.insertText(runs, 9, "Y")
        assertTrue(after.any { it is TextRun.Link })
        assertEquals("탈퇴 이유가 세라Y와 관련", Runs.text(after))
        // Typing inside the link breaks it into plain text.
        val inside = Runs.insertText(runs, 8, "Z")
        assertTrue(inside.none { it is TextRun.Link })
        assertEquals("탈퇴 이유가 세Z라와 관련", Runs.text(inside))
        // Deleting the whole link removes it; adjacent plain text merges.
        val deleted = Runs.delete(runs, 7, 9)
        assertEquals(listOf<TextRun>(TextRun.Plain("탈퇴 이유가 와 관련")), deleted)
    }

    @Test
    fun atQueryRules() {
        val runs = Runs.plain("메일a@b 그리고 @세")
        assertNull(AtMentions.queryAt(runs, 6)) // `@` after a letter is not a mention
        val q = AtMentions.queryAt(runs, Runs.text(runs).length)!!
        assertEquals("세", q.query)
        assertNull(AtMentions.queryAt(Runs.plain("@세\n라"), 4))
        assertNull(AtMentions.queryAt(Runs.plain("@" + "가".repeat(25)), 26))
        assertEquals("", AtMentions.queryAt(Runs.plain("@"), 1)!!.query)
    }

    @Test
    fun hangulInitialsMatch() {
        assertEquals("ㅅㄹ", Hangul.initials("세라"))
        assertTrue(Hangul.isInitialsQuery("ㅎㄹㅌ"))
        assertFalse(Hangul.isInitialsQuery("하르"))
        val hits = LinkPicker.search(env, "ㅎㄹㅌ", emptyList(), PickerMode.AT)
        assertEquals(Fx.hart, hits.first().target)
    }

    @Test
    fun aliasMatchUsesTheAliasAsRunText() {
        val hit = LinkPicker.search(env, "빛돌", emptyList(), PickerMode.AT).first()
        assertEquals(K.TERM_ORIGINIUM_LANTERN, hit.target.key)
        assertEquals("빛돌", hit.runText)
        assertEquals("오리지늄 등불", hit.label)
    }

    @Test
    fun sentenceCuts() {
        assertEquals("하르트가 등불을 싫어하는 이유", RecordText.pageNamePrefill(ExampleText.R1))
        assertEquals("하르트가 등불을 싫어하는 이유.", RecordText.firstSentence(ExampleText.R1))
        assertEquals("가".repeat(30), RecordText.pageNamePrefill("가".repeat(40)))
        assertEquals("첫 줄", RecordText.firstSentence("첫 줄\n둘째 줄"))
        assertEquals("a b c", RecordText.oneLine(" a\n b\t\tc "))
    }

    @Test
    fun timeLabels() {
        val now = Fx.NOW
        assertEquals("2025년 3월 4일", RecorderTimeLabels.meta(Fx.at("2025-03-04T03:00:00Z"), now, Fx.ZONE))
        assertEquals("1:00:05", RecorderTimeLabels.duration(3_605_000))
        assertEquals("1:00:05", RecorderTimeLabels.timer(3_605_000))
        assertEquals("59:59", RecorderTimeLabels.timer(3_599_000))
        assertEquals("11월 22일 08:41", RecorderTimeLabels.sourceDate(Fx.at("2026-11-21T23:41:00Z"), Fx.ZONE))
    }

    @Test
    fun linkAddIsIdempotentAndAtMarksAnExistingLink() {
        val ctx = Fx.ctx()
        val once = LinkEdits.add(emptyList(), Fx.R1, Fx.sera, LinkOrigin.MANUAL, ctx)
        assertEquals(once, LinkEdits.add(once, Fx.R1, Fx.sera, LinkOrigin.ENTRY, ctx))
        // An `@` to an already linked target: the chip turns into the `@` form and cannot be ×-ed.
        val runs = listOf(link)
        assertTrue(LinkEdits.isAtForm(once.single(), runs))
        assertEquals(once, LinkEdits.remove(once, once.single().key, runs))
        // When the run goes, the manual link stays (it was not created by the `@`).
        assertEquals(once, LinkEdits.syncAt(once, Fx.R1, emptyList(), ctx))
        assertTrue(LinkEdits.remove(once, once.single().key, emptyList()).isEmpty())
    }

    @Test
    fun replaceKeepsChipPlaceAndBecomesManual() {
        val ctx = Fx.ctx()
        val library = Fx.openLibrary()
        val entryLink = library.linksOf(Fx.R1).first { it.origin.known == LinkOrigin.ENTRY }
        val replaced = LinkEdits.replace(library, Fx.R1, entryLink.key, Fx.sera, ctx)
        val first = LinkEdits.ordered(replaced.linksOf(Fx.R1)).first()
        assertEquals(K.SERA, first.targetKey)
        assertEquals(LinkOrigin.MANUAL, first.origin.known)
        assertEquals(entryLink.createdAt, first.createdAt)
    }

    @Test
    fun kindFilterAndProjectChip() {
        val library = Fx.openLibrary()
        val voice = InboxFilters.toggleKind(InboxFilterState(), KindFilter.VOICE)
        val rows = Inbox.screen(library, voice, env, Fx.NOW).rows
        assertTrue(rows.all { library.record(it.recordKey)!!.kind.known == RecordKind.VOICE })
        assertEquals(listOf(Fx.R1, Fx.R3), rows.map { it.recordKey })
        assertEquals(KindFilter.NONE, InboxFilters.toggleKind(voice, KindFilter.VOICE).kind)
        assertEquals(KindFilter.TEXT, InboxFilters.toggleKind(voice, KindFilter.TEXT).kind)
        assertEquals("잿빛 새벽의 등불 ▾", Inbox.projectChipLabel(ProjectFilter.Project(Fx.P1), env))
        assertEquals("연결 없음 ▾", Inbox.projectChipLabel(ProjectFilter.NoLink, env))
        // Counts do not follow the kind filter (emulator round 1): `전체` stays the status total.
        val noKind = Inbox.screen(library, InboxFilterState(), env, Fx.NOW).filters.take(2).map { it.label }
        assertEquals(noKind, Inbox.screen(library, voice, env, Fx.NOW).filters.take(2).map { it.label })
    }

    @Test
    fun kindChipsLeaveTheStatusCountsAlone() {
        val library = Fx.openLibrary()
        fun labels(lib: RecorderLibrary, f: InboxFilterState) = Inbox.screen(lib, f, env, Fx.NOW).filters.take(2).map { it.label }
        val base = InboxFilterState()
        val all = labels(library, base)
        val voice = InboxFilters.toggleKind(base, KindFilter.VOICE)
        val text = InboxFilters.toggleKind(base, KindFilter.TEXT)
        assertEquals(all, labels(library, voice))
        assertEquals(all, labels(library, text))
        // The rows still narrow, and both 전체 (status) and 음성 (kind) stay selected.
        val voiceScreen = Inbox.screen(library, voice, env, Fx.NOW)
        assertTrue(voiceScreen.rows.size < Inbox.screen(library, base, env, Fx.NOW).rows.size)
        assertEquals(listOf("전체", "음성"), voiceScreen.filters.filter { it.selected }.map { it.label.substringBefore(' ') })
        // No voice record at all: the list is filtered-empty but `전체 <n>` still counts the text records.
        val textOnly = library.copy(records = library.records.filter { it.kind.known == RecordKind.TEXT })
        val n = textOnly.records.count { it.deletedAt == null && !it.hidden }
        assertTrue(n > 0)
        val voiceOnText = Inbox.screen(textOnly, voice, env, Fx.NOW)
        assertTrue(voiceOnText.filteredEmpty)
        assertEquals("전체 $n", voiceOnText.filters.first().label)
        assertEquals(labels(textOnly, base), labels(textOnly, voice))
        // The project filter still scopes the counts (the `정리 안 됨 n ›` line matches the inbox).
        val inP3 = labels(library, InboxFilterState(project = ProjectFilter.Project(Fx.P3)))
        assertEquals(listOf("전체 0", "정리 안 됨 0"), inP3)
    }

    @Test
    fun emptyAndFilteredEmptyInbox() {
        val empty = Inbox.screen(RecorderLibrary(), InboxFilterState(), env, Fx.NOW)
        assertNotNull(empty.empty)
        assertEquals("전체 0", empty.empty!!.chip)
        assertEquals(listOf("말로 기록", "글로"), empty.empty.buttons)
        assertTrue(empty.bottomButtons.isEmpty())
        val filtered = Inbox.screen(Fx.openLibrary(), InboxFilterState(project = ProjectFilter.Project(Fx.P3)), env, Fx.NOW)
        assertNull(filtered.empty)
        assertTrue(filtered.filteredEmpty)
        assertEquals(listOf("글로 쓰기", "말로 기록"), filtered.bottomButtons)
    }

    @Test
    fun textMemoDraftIsKeptOnClose() {
        val ctx = Fx.ctx()
        val sheet = TextMemo.open(EntryLinks.outside(EntrySurface.LIBRARY), RecorderSettings(), env, null, ctx)
        assertNull(TextMemo.close(sheet))
        assertFalse(TextMemo.view(sheet, env).canSave)
        val typed = TextMemo.type(sheet, 0, "  ", env, ctx)
        assertFalse(TextMemo.canSave(typed))
        val real = TextMemo.type(typed, 2, "생각", env, ctx)
        val kept = TextMemo.close(real)!!
        val reopened = TextMemo.open(EntryLinks.project(EntrySurface.PROJECT_HOME, Fx.P1), RecorderSettings(), env, kept, ctx)
        assertEquals("  생각", Runs.text(reopened.draft.runs))
        assertTrue(reopened.draft.pendingLinks.isEmpty())
    }

    @Test
    fun switchingBetweenVoiceAndText() {
        val ctx = Fx.ctx()
        var s = Recording.start("01JZ000000TSTK000000000950", EntryLinks.script(Fx.P1, Fx.S, K.BLOCK_044), RecorderSettings(), env, "a", ctx)
        s = Recording.partial(Recording.final(Recording.elapse(s, 7_000), "첫 문장."), "둘째")
        val sheet = Recording.switchToText(s, env, ctx.now)
        assertEquals("첫 문장. 둘째", Runs.text(sheet.draft.runs))
        assertEquals(s.pendingLinks.map { it.targetKey }, sheet.draft.pendingLinks.map { it.targetKey })
        val (library, key) = TextMemo.save(sheet, RecorderLibrary(), ctx)
        assertEquals(s.recordKey, key)
        assertEquals(RecordKind.VOICE, library.record(key)!!.kind.known)
        assertEquals(7_000L, library.record(key)!!.durationMs)

        val back = TextMemo.toVoice(sheet, "01JZ000000TSTK000000000951", "b")
        assertEquals("첫 문장. 둘째", Runs.text(back.confirmedRuns))
        assertEquals(s.entry, back.entry)
        assertEquals(listOf(Fx.S), back.pendingLinks.map { it.targetKey })
    }

    @Test
    fun switchToTextNeverDuplicatesALink() {
        val ctx = Fx.ctx()
        var s = Recording.start("01JZ000000TSTK000000000960", EntryLinks.script(Fx.P1, Fx.S, K.BLOCK_044), RecorderSettings(), env, "a", ctx)
        s = Recording.final(Recording.elapse(s, 3_000), "장면 생각")
        var sheet = Recording.switchToText(s, env, ctx.now)
        sheet = TextMemo.addLink(sheet, Fx.sceneS, env, ctx)
        val sceneRun = Runs.linkRun("01JZ000000TSTK000000000961", LinkTargetKind.UNIT, Fx.S, Fx.P1, "1-7 씬 3")
        sheet = TextMemo.edit(sheet, sheet.draft.runs + TextRun.Plain(" ") + sceneRun, env, ctx)
        assertEquals(1, sheet.draft.pendingLinks.count { it.targetKey == Fx.S })
        val (library, key) = TextMemo.save(sheet, RecorderLibrary(), ctx)
        val links = library.linksOf(key)
        assertEquals(1, links.count { it.targetKey == Fx.S })
        assertEquals(links.size, links.map { Triple(it.projectKey, it.targetKind, it.targetKey) }.toSet().size)
        // The chip of the 보던 곳 link turns into the `@` form while the run exists.
        val chips = io.github.lenticualrstone.civilight.domain.recorder.link.Chips.captureRow(links, library.record(key)!!.runs, env)
        assertEquals(listOf("연결 · 1-7 씬 3 @"), chips.map { it.text })
    }

    @Test
    fun sceneMemoGetsANewLineAndUndoRestoresIt() {
        val doc = Fx.p1Doc()
        val before = doc.index.unit(Fx.S)!!.memo
        val library = Fx.openLibrary().let { lib ->
            lib.copy(records = lib.records.map { if (it.key == Fx.R3) it.copy(runs = Runs.plain("한 줄\n두 줄")) else it })
        }
        val ins = MemoFieldInsert.insert(doc, library, Fx.R3, Fx.S, Fx.ctx())
        assertEquals("$before\n한 줄 두 줄", ins.doc.index.unit(Fx.S)!!.memo)
        assertEquals("1-7 씬 3 · 씬 메모", ins.placement.lastLabel)
        assertEquals(before, Undo.undo(ins.doc, ins.library, ins.undo).first.index.unit(Fx.S)!!.memo)
    }

    @Test
    fun goneUnitLabelsWithoutASceneWordStayPlain() {
        assertEquals("1-7 씬 3", LinkViews.sceneLabelIn("1-7 씬 3 · 꺼진 등불", "씬"))
        assertEquals("1-7 씬 3", LinkViews.sceneLabelIn("1-7 씬 3", "씬"))
        assertNull(LinkViews.sceneLabelIn("1-6 · 잿더미 시장", "씬"))
        assertNull(LinkViews.sceneLabelIn("2장", "씬"))
    }

    @Test
    fun menuOptionsAndPreview() {
        val library = Fx.openLibrary()
        val r1 = library.record(Fx.R1)!!
        val titles = RecordMenu.options(library, r1, env).map { it.title }
        assertEquals(
            listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기", "메모 칸에 넣기", "인물로 만들기", "끝냄으로 표시", "숨기기", "지우기"),
            titles,
        )
        val subs = RecordMenu.options(library, r1, env).associate { it.title to it.sub }
        assertEquals("위치 고르기 · 연결된 씬 · 마지막으로 쓰던 곳", subs["원고에 넣기"])
        assertEquals("고른 탭 끝에 작가 메모", subs["카드에 넣기"])
        assertEquals("지문 · PIN으로 열림", subs["숨기기"])
        assertEquals("휴지통 · 30일 뒤 삭제", subs["지우기"])
        val preview = RecordMenu.preview(r1, env, Fx.NOW)
        assertEquals("음성 0:24 · 오늘 08:41", preview.meta)
        assertTrue(preview.excerpt.startsWith("하르트가 등불을 싫어하는 이유. 대정전 때"))
        assertTrue(preview.excerpt.endsWith("…"))
    }

    @Test
    fun settingsScreenRows() {
        val rows = RecorderSettingsScreen.rows(RecorderSettings(), recorderGlow = true)
        assertEquals(listOf("기기 안", "클라우드", "잠금 화면에서 녹음", "원본 음성 보관", "녹음 중 빛 띠", "보던 곳 연결"), rows.map { it.title })
        assertEquals(listOf(true, false, true, true, true, true), rows.map { it.on })
    }

    @Test
    fun projectPurgedButSceneLinkStillImpliesNothingForAMissingProject() {
        val library = LinkEdits.add(Fx.openLibrary(), Fx.R3, TargetRef(Fx.P4, RecorderTargetKind.PROJECT, Fx.P4), LinkOrigin.MANUAL, Fx.ctx())
        // P4 is not in the 서재: permanently deleted.
        val rows = Inbox.screen(library, InboxFilterState(project = ProjectFilter.NoLink), env, Fx.NOW).rows
        assertTrue(rows.any { it.recordKey == Fx.R3 })
    }
}
