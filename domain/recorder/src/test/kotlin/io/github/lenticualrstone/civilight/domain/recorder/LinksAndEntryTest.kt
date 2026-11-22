package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.K
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.capture.TextMemo
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.target
import io.github.lenticualrstone.civilight.domain.recorder.picker.CandidateGroup
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkPicker
import io.github.lenticualrstone.civilight.domain.recorder.picker.PickerMode
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/** LRC §4.2 · Links and entry. */
class LinksAndEntryTest {
    private val on = RecorderSettings(presetLink = true)

    private fun startedLinks(entry: EntryContext, settings: RecorderSettings = on): List<Pair<TargetRef?, LinkOrigin?>> {
        val s = Recording.start("01JZ000000TSTK999999999999", entry, settings, Fx.env(), "tmp/a.m4a", Fx.ctx())
        return s.pendingLinks.map { it.target to it.origin.known }
    }

    private fun firstChip(entry: EntryContext): String {
        val env = Fx.env()
        val s = Recording.start("01JZ000000TSTK999999999999", entry, on, env, "tmp/a.m4a", Fx.ctx())
        return Chips.captureRow(s.pendingLinks, s.confirmedRuns, env).first().text
    }

    @Test
    fun LRC_T01_prefilledLinkByEntry() {
        val script = EntryLinks.script(Fx.P1, Fx.S, K.BLOCK_044)
        assertEquals(listOf(Fx.sceneS to LinkOrigin.ENTRY), startedLinks(script))
        assertEquals("연결 · 1-7 씬 3", firstChip(script))

        val profile = EntryLinks.card(EntrySurface.CHARACTER, Fx.P1, RecorderTargetKind.CHARACTER, K.HART)
        assertEquals(listOf(Fx.hart to LinkOrigin.ENTRY), startedLinks(profile))

        val page = EntryLinks.page(Fx.P1, K.PAGE_LOWER_LIGHT)
        assertEquals(listOf(Fx.target(RecorderTargetKind.PAGE, K.PAGE_LOWER_LIGHT) to LinkOrigin.ENTRY), startedLinks(page))

        for (surface in listOf(EntrySurface.PROJECT_HOME, EntrySurface.STORY, EntrySurface.TIMELINE, EntrySurface.MAP)) {
            val e = EntryLinks.project(surface, Fx.P1)
            assertEquals(listOf(TargetRef.project(Fx.P1) to LinkOrigin.ENTRY), startedLinks(e))
            assertEquals("연결 · 잿빛 새벽의 등불", firstChip(e))
        }
        for (surface in listOf(EntrySurface.LIBRARY, EntrySurface.HOME_WIDGET, EntrySurface.QS_TILE, EntrySurface.LOCK_SCREEN, EntrySurface.COVER_WIDGET)) {
            assertEquals(surface.name, emptyList<Any>(), startedLinks(EntryLinks.outside(surface)))
        }
        assertEquals(listOf(TargetRef.project(Fx.P1) to LinkOrigin.ENTRY), startedLinks(EntryLinks.inbox(ProjectFilter.Project(Fx.P1))))
        assertEquals(emptyList<Any>(), startedLinks(EntryLinks.inbox(ProjectFilter.All)))

        val off = RecorderSettings(presetLink = false)
        for (e in listOf(script, profile, page, EntryLinks.project(EntrySurface.PROJECT_HOME, Fx.P1), EntryLinks.inbox(ProjectFilter.Project(Fx.P1)))) {
            assertEquals(emptyList<Any>(), startedLinks(e, off))
        }
    }

    @Test
    fun LRC_T02_rule23Example() {
        val env = Fx.env()
        val ctx = Fx.ctx(now = Fx.at("2026-11-21T23:41:24Z"))
        var s = Recording.start("01JZ000000TSTK000000000500", EntryLinks.script(Fx.P1, Fx.S, K.BLOCK_044), on, env, "tmp/r.m4a", ctx)
        s = Recording.final(Recording.elapse(s, 24_000), ExampleText.R1)
        val (processing, saved) = Recording.stopAndSave(s, RecorderLibrary(), ctx, startedAt = Fx.at("2026-11-21T23:41:00Z"))
        val withHart = Recording.addLink(processing, Fx.hart, env, ctx)
        val library = Recording.keepInInbox(withHart, saved, ctx)

        val record = library.record(s.recordKey)!!
        assertEquals(RecordKind.VOICE, record.kind.known)
        assertEquals(24_000L, record.durationMs)
        assertEquals(
            listOf(Fx.sceneS to LinkOrigin.ENTRY, Fx.hart to LinkOrigin.MANUAL),
            library.linksOf(record.key).sortedBy { it.createdAt }.map { it.target to it.origin.known },
        )
        assertEquals(RecordState.UNSORTED, RecordStates.state(library, record, env))

        val row = Inbox.row(library, record, env, Fx.NOW)
        assertEquals("음성 0:24", row.kindLabel)
        assertEquals("오늘 08:41", row.meta)
        assertEquals("정리 안 됨", row.stateLabel)
        assertEquals(ExampleText.R1, row.text)
        assertEquals(listOf("1-7 씬 3", "하르트"), row.linkTags.map { it.text })
        assertEquals(listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기"), row.actions)
        val all = listOf(row.kindLabel, row.meta, row.stateLabel, row.text) + row.linkTags.map { it.text } + row.actions
        assertTrue(all.none { it.contains("에서") })
    }

    private fun listed(filter: ProjectFilter) =
        Inbox.screen(Fx.openLibrary(), InboxFilterState(project = filter), Fx.env(), Fx.NOW).rows.map { it.recordKey }

    @Test
    fun LRC_T03_subLinkImpliesProject() {
        val p1 = listed(ProjectFilter.Project(Fx.P1))
        assertTrue(Fx.R1 in p1 && Fx.R2 in p1)
        assertFalse(Fx.R3 in p1)
        val p2 = listed(ProjectFilter.Project(Fx.P2))
        assertTrue(listOf(Fx.R1, Fx.R2, Fx.R3).none { it in p2 })
        val none = listed(ProjectFilter.NoLink)
        assertTrue(Fx.R3 in none)
        assertFalse(Fx.R1 in none || Fx.R2 in none)
    }

    @Test
    fun LRC_T04_atChipLine() {
        val env = Fx.env()
        val ctx = Fx.ctx()
        var sheet = TextMemo.open(EntryLinks.project(EntrySurface.PROJECT_HOME, Fx.P1), on, env, null, ctx)
        sheet = TextMemo.type(sheet, 0, "오웬은 사실 결사 출신? 탈퇴 이유가 ", env, ctx)
        sheet = TextMemo.addLink(sheet, Fx.target(RecorderTargetKind.CHARACTER, K.OWEN), env, ctx)
        val atPos = Runs.text(sheet.draft.runs).length
        sheet = TextMemo.type(sheet, atPos, "@세라", env, ctx)
        val query = TextMemo.atQuery(sheet, atPos + 3)
        assertNotNull(query)
        assertEquals("세라", query!!.query)
        val candidate = LinkPicker.search(env, query.query, sheet.draft.pendingLinks, PickerMode.AT).first()
        assertEquals(Fx.sera, candidate.target)
        val (picked, caret) = TextMemo.pick(sheet, query, candidate, env, ctx)
        sheet = TextMemo.type(picked, caret, "와 관련", env, ctx)
        assertEquals("오웬은 사실 결사 출신? 탈퇴 이유가 세라와 관련", Runs.text(sheet.draft.runs))

        val chips = TextMemo.view(sheet, env).chips
        assertEquals(listOf("연결 · 잿빛 새벽의 등불", "오웬", "세라 @"), chips.map { it.text })
        assertEquals(listOf(true, true, false), chips.map { it.removable })
        assertEquals("+ 연결", TextMemo.view(sheet, env).addLink)

        val range = Runs.linkRanges(sheet.draft.runs).single().second
        sheet = TextMemo.delete(sheet, range.first, range.last + 1, env, ctx)
        assertEquals(listOf("연결 · 잿빛 새벽의 등불", "오웬"), TextMemo.view(sheet, env).chips.map { it.text })
        assertTrue(sheet.draft.pendingLinks.none { it.target == Fx.sera })

        val owen = sheet.draft.pendingLinks.first { it.targetKey == K.OWEN }
        sheet = TextMemo.removeLink(sheet, owen.key, env)
        assertEquals(listOf("연결 · 잿빛 새벽의 등불"), TextMemo.view(sheet, env).chips.map { it.text })
    }

    @Test
    fun LRC_T05_atSearchOrder() {
        val library = Fx.openLibrary()
        val r5Links = library.linksOf(Fx.r(5))
        val env = Fx.env(p2 = Fx.p2Doc(withSera = true), recent = Fx.P1)
        val results = LinkPicker.search(env, "세라", r5Links, PickerMode.AT)
        val seras = results.filter { it.label == "세라" }
        assertEquals(TargetRef(Fx.P2, RecorderTargetKind.CHARACTER, Fx.P2_SERA), seras[0].target)
        assertEquals(Fx.sera, seras[1].target)
        assertEquals(CandidateGroup.LINKED_PROJECTS, results[0].group)
        assertEquals(Fx.sera, results[1].target)
        assertTrue(results.none { it.target.projectKey == Fx.P3 })
    }

    @Test
    fun LRC_T06_crossProjectAtImpliesProject() {
        val env = Fx.env()
        val ctx = Fx.ctx()
        var sheet = TextMemo.open(EntryLinks.outside(EntrySurface.LIBRARY), on, env, null, ctx)
        sheet = TextMemo.type(sheet, 0, "@하르트", env, ctx)
        val query = TextMemo.atQuery(sheet, 4)!!
        val candidate = LinkPicker.search(env, query.query, sheet.draft.pendingLinks, PickerMode.AT).first { it.target == Fx.hart }
        sheet = TextMemo.pick(sheet, query, candidate, env, ctx).first
        val (library, key) = TextMemo.save(sheet, RecorderLibrary(), ctx)
        assertEquals(listOf(Fx.hart to LinkOrigin.AT), library.linksOf(key).map { it.target to it.origin.known })
        assertEquals(setOf(Fx.P1), RecordStates.impliedProjects(library, key, env))
        val rows = Inbox.screen(library, InboxFilterState(project = ProjectFilter.Project(Fx.P1)), env, Fx.NOW).rows
        assertEquals(listOf(key), rows.map { it.recordKey })
    }

    private fun inTextGroup(): List<String> {
        val library = Fx.openLibrary()
        val r1 = library.record(Fx.R1)!!
        return LinkPicker.search(Fx.env(), "", library.linksOf(Fx.R1), PickerMode.PICKER, Runs.text(r1.runs))
            .filter { it.group == CandidateGroup.IN_TEXT }.map { it.label }
    }

    @Test
    @Ignore("spec conflict: LRC fixture says 대정전 is only an event; the example fixture also has the term 대정전 (fixture.md conflict 9), whose name is in R1's text")
    fun LRC_T07_dictationCreatesNoLinks_inTextGroup() {
        assertEquals(listOf("하르트", "세라"), inTextGroup())
    }

    @Test
    fun LRC_T07_fixture_dictationCreatesNoLinks_inTextGroup() {
        val library = Fx.openLibrary()
        val r1 = library.record(Fx.R1)!!
        assertTrue(library.linksOf(Fx.R1).none { it.origin.known == LinkOrigin.AT })
        assertTrue(r1.runs.none { it is TextRun.Link })
        val results = LinkPicker.search(Fx.env(), "", library.linksOf(Fx.R1), PickerMode.PICKER, Runs.text(r1.runs))
        val group = results.filter { it.group == CandidateGroup.IN_TEXT }
        // Order of first occurrence; 대정전 is a term in the example fixture.
        // Order of first occurrence; 대정전 is a term in the example fixture.
        assertEquals(listOf("하르트", "대정전", "세라"), group.map { it.label })
        assertTrue(group.all { it.target.projectKey == Fx.P1 })
        assertTrue(group.first { it.label == "하르트" }.linked)
        assertFalse(group.first { it.label == "세라" }.linked)
        assertTrue(listOf("오리지늄 등불", "리엔", "오웬", "노아").none { name -> group.any { it.label == name } })
        assertEquals("글에 나온 이름", CandidateGroup.IN_TEXT.label)
    }

    @Test
    fun LRC_T08_lockedHiddenTargetsAreNotOffered() {
        val p1 = Fx.withNoahLive(Fx.p1Doc(), hidden = true)
        val locked = Fx.env(p1 = p1)
        for (mode in PickerMode.values()) {
            assertTrue(LinkPicker.search(locked, "노아", emptyList(), mode).isEmpty())
            assertTrue(LinkPicker.search(locked, "세라", emptyList(), mode).none { it.target.projectKey == Fx.P3 })
        }
        assertTrue(LinkPicker.search(locked, "", emptyList(), PickerMode.PICKER).none { it.target.projectKey == Fx.P3 })
        assertTrue(LinkPicker.search(locked, "검은 서약", emptyList(), PickerMode.AT).isEmpty())

        val unlocked = Fx.env(p1 = p1, unlock = UnlockSession(itemsUnlocked = mapOf(Fx.P1 to Fx.NOW)))
        assertEquals(listOf(Fx.noah), LinkPicker.search(unlocked, "노아", emptyList(), PickerMode.PICKER).map { it.target })
    }
}

/** Text of the fixture records. */
object ExampleText {
    const val R1 = "하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다는 설정은 어떨까. 그게 세라의 오빠였다면?"
    const val R3 = "2장 첫 장면은 비 오는 상층 관문에서 시작. 리엔이 처음으로 위를 올려다보는…"
}
