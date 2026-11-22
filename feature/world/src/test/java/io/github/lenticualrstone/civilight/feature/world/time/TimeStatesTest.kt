package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.world.time.TimelineView
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.common.ItemLifecycle
import io.github.lenticualrstone.civilight.feature.world.common.Restored
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.WorldDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** 연표 (WLD-31), 사건 sheet and detail (WLD-32/33), 세계 시간 (WLD-29) as the screens draw them. */
class TimeStatesTest {
    private val doc = WorldTestFixture.stateB()
    private fun actx() = ActionContext(now = ExampleClock.NOW, zone = ZoneId.of("Asia/Seoul"), device = "test-device", newKey = WorldTestFixture.keys())

    private fun withMode(d: ProjectDoc, mode: WorldTimeMode, calendar: WorldCalendar? = null) =
        d.copy(project = d.project.copy(worldTime = d.project.worldTime.copy(mode = mode.open(), calendar = calendar ?: d.project.worldTime.calendar)))

    @Test
    fun `WLD-31 timeline rows in time order with the divider and the lens chapter`() {
        val ui = TimelineState.build(doc, TimelineView.EPISODE, null, hiddenOpen = false)
        assertTrue(ui.available)
        assertEquals("연표", ui.title)
        assertEquals("세계 시간과 이야기 시간", ui.smallLine)
        assertEquals("달력: 연도만 · 프로젝트 설정에서 월 · 일 달력 추가", ui.infoLine)
        assertEquals(listOf("에피소드 기준", "이야기 시작부터", "기준 사건"), ui.chips.map { it.label })
        assertEquals(TimelineView.EPISODE, ui.chips.single { it.selected }.view)
        assertEquals("사건 · 연도 찾기", ui.searchHint)
        assertNull(ui.footer)
        assertEquals(
            listOf("대정전", "이야기 시작 · 1장", "1장 · 등불지기", "리엔, 결사 합류", "상층 송전망 사고", "2장 · 잿불"),
            ui.rows.map { it.title },
        )
        val blackout = ui.rows.first()
        assertEquals(TimelineRowKind.WORLD_EVENT, blackout.kind)
        assertEquals("1195", blackout.time)
        assertTrue(blackout.sub.endsWith("1-4에서 회상"))
        assertEquals(K.EVENT_BLACKOUT, blackout.eventKey)
        // The lens (최신 · 1-7) lies in 1장: its dot is the current one; 2장 and the planned event are dimmed.
        assertEquals("1장 · 등불지기", ui.rows.single { it.current }.title)
        assertTrue(ui.rows.single { it.title == "상층 송전망 사고" }.sub.endsWith("2장에서 공개 예정"))
        assertTrue(ui.rows.single { it.title == "상층 송전망 사고" }.dimmed)
        assertNull(ui.rows.single { it.kind == TimelineRowKind.CHAPTER && it.title.startsWith("1장") }.eventKey)
    }

    @Test
    fun `WLD-34 story-start labels and the anchor view with its chip, hint and footer`() {
        val start = TimelineState.build(doc, TimelineView.STORY_START, null, hiddenOpen = false)
        assertEquals("3일째", start.rows.single { it.eventKey == K.EVENT_JOINS_SOCIETY }.time)
        val anchor = TimelineState.build(doc, TimelineView.ANCHOR, K.EVENT_JOINS_SOCIETY, hiddenOpen = false)
        assertEquals("기준 사건 · 리엔, 결사 합류", anchor.chips.single { it.selected }.label)
        assertEquals("기준", anchor.rows.single { it.eventKey == K.EVENT_JOINS_SOCIETY }.time)
        assertEquals("사건 · 때 찾기", anchor.searchHint)
        assertEquals("대략인 때는 범위(5~9일)로 · 점검도 범위로", anchor.footer)
        // Without a live anchor event the anchor view falls back to 에피소드 기준.
        val none = TimelineState.build(doc, TimelineView.ANCHOR, null, hiddenOpen = false)
        assertEquals(TimelineView.EPISODE, none.chips.single { it.selected }.view)
    }

    @Test
    fun `WLD-T51 시간 없이 turns the module off`() {
        assertFalse(TimelineState.build(withMode(doc, WorldTimeMode.NONE), TimelineView.EPISODE, null, false).available)
        assertFalse(timelineAvailable(withMode(doc, WorldTimeMode.NONE)))
        assertTrue(timelineAvailable(doc))
    }

    @Test
    fun `WLD-13 the locked timeline masks a hidden place`() {
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_LOWER_STREET) it.copy(hidden = true) else it }))
        val locked = TimelineState.build(hidden, TimelineView.EPISODE, null, hiddenOpen = false).rows.first()
        val open = TimelineState.build(hidden, TimelineView.EPISODE, null, hiddenOpen = true).rows.first()
        assertTrue(open.sub.contains("하층 거리"))
        assertFalse(locked.sub.contains("하층 거리"))
        assertTrue(locked.sub.contains("숨긴 항목"))
        val detail = EventDetailState.build(hidden, K.EVENT_BLACKOUT, hiddenOpen = false)
        assertEquals("숨긴 항목", detail.rows.toMap()["장소"])
    }

    @Test
    fun `WLD-33 event detail rows and changes as board TwoTimeline`() {
        val d = EventDetailState.build(doc, K.EVENT_BLACKOUT, hiddenOpen = false)
        assertEquals(EventAccess.Open, d.access)
        assertEquals("연표 · 세계 사건", d.paneTitle)
        assertEquals("대정전", d.name)
        assertEquals("1195", d.time)
        assertEquals(listOf("장소", "그때 나이", "원고", "관련 용어"), d.rows.map { it.first })
        assertEquals("1-4 씬 2 · 회상", d.rows.toMap()["원고"])
        assertTrue(d.rows.toMap()["그때 나이"]!!.startsWith("리엔 12세 · 세라 16세"))
        assertTrue(d.changes.any { it.label == "하층 거리 · 상태" && it.value == "빛 있음 → 대정전 이후" })
    }

    @Test
    fun `WLD-32 a new world event needs a name and a valid year`() {
        val ctx = WorldTestFixture.ctx()
        assertEquals(EventSave.Invalid, EventForms.save(doc, EventDraft(title = "  "), null, ctx))
        assertTrue(EventForms.ui(doc, EventDraft(title = "검은 비", year = "12x"), editing = false, hiddenOpen = false).dateInvalid)
        val draft = EventDraft(title = "검은 비", year = "1190", description = "하층에 비가 내림")
            .withPlace(PlaceRef(PlaceRefKind.PLACE, K.PLACE_LOWER_STREET))
            .toggleTerm(K.TERM_ORIGINIUM_LANTERN)
        val ui = EventForms.ui(doc, draft, editing = false, hiddenOpen = false)
        assertTrue(ui.canSave)
        assertTrue(ui.showYear)
        assertFalse(ui.showMonthDay)
        assertEquals("하층 거리", ui.placeLabel)
        assertEquals("오리지늄 등불", ui.termsLabel)
        val saved = EventForms.save(doc, draft, null, ctx) as EventSave.Saved
        val event = saved.doc.index.event(saved.eventKey)!!
        assertEquals(EventKind.WORLD, event.kind.known)
        assertEquals(WorldDate(1190), event.worldDate)
        assertEquals(listOf(K.TERM_ORIGINIUM_LANTERN), event.relatedTermKeys)
        assertTrue(TimelineState.build(saved.doc, TimelineView.EPISODE, null, false).rows.first().title == "검은 비")
    }

    @Test
    fun `WLD-32 a story event takes its time from a scene`() {
        val ctx = WorldTestFixture.ctx()
        val noScene = EventDraft(story = true, title = "하르트와 마주침")
        assertFalse(EventForms.ui(doc, noScene, editing = false, hiddenOpen = false).canSave)
        assertFalse(EventForms.ui(doc, noScene, editing = false, hiddenOpen = false).showReveal)
        val scene = doc.manuscript.liveScenes.last { it.deletedAt == null }.key
        val saved = EventForms.save(doc, noScene.copy(sceneKey = scene), null, ctx) as EventSave.Saved
        assertEquals(EventKind.STORY, saved.doc.index.event(saved.eventKey)!!.kind.known)
        assertEquals(scene, saved.doc.index.event(saved.eventKey)!!.sceneKey)
    }

    @Test
    fun `WLD-32 calendar mode checks month and day against the calendar`() {
        val cal = withMode(doc, WorldTimeMode.CALENDAR, WorldCalendar(months = listOf(CalendarMonth("가달", 30), CalendarMonth("나달", 31))))
        val ok = EventDraft(title = "축제", year = "1199", month = "2", day = "31")
        assertTrue(EventForms.ui(cal, ok, false, false).showMonthDay)
        assertEquals(WorldDate(1199, 2, 31), EventForms.parseDate(cal, ok))
        assertNull(EventForms.parseDate(cal, ok.copy(day = "32")))
        assertNull(EventForms.parseDate(cal, ok.copy(month = "3")))
        assertNull(EventForms.parseDate(cal, ok.copy(month = "", day = "3")))
    }

    @Test
    fun `WLD-32 editing keeps the kind, links and a date the mode cannot show`() {
        val ctx = WorldTestFixture.ctx()
        val event = doc.index.event(K.EVENT_BLACKOUT)!!
        val draft = EventDraft.of(event)
        assertEquals("1195", draft.year)
        assertFalse(EventForms.ui(doc, draft, editing = true, hiddenOpen = false).kindChoice)
        val renamed = EventForms.save(doc, draft.copy(title = "대정전의 밤"), K.EVENT_BLACKOUT, ctx) as EventSave.Saved
        val after = renamed.doc.index.event(K.EVENT_BLACKOUT)!!
        assertEquals("대정전의 밤", after.title)
        assertEquals(event.description, after.description)
        assertEquals(event.worldDate, after.worldDate)
        val none = withMode(doc, WorldTimeMode.NONE)
        val kept = EventForms.save(none, EventDraft.of(event).copy(year = ""), K.EVENT_BLACKOUT, ctx) as EventSave.Saved
        assertEquals(event.worldDate, kept.doc.index.event(K.EVENT_BLACKOUT)!!.worldDate)
    }

    @Test
    fun `WLD-32 delete goes to the trash with its toast and comes back`() {
        val deleted = ItemLifecycle.delete(doc, ItemId.event(K.EVENT_BLACKOUT), actx())!!
        assertTrue(deleted.toast.message.startsWith("사건 대정전 지움"))
        assertEquals("되돌리기", deleted.toast.action)
        val gone = EventDetailState.build(deleted.doc, K.EVENT_BLACKOUT, hiddenOpen = false)
        assertEquals(EventAccess.Gone("지운 사건 · 대정전", restorable = true), gone.access)
        assertFalse(TimelineState.build(deleted.doc, TimelineView.EPISODE, null, false).rows.any { it.eventKey == K.EVENT_BLACKOUT })
        val back = ItemLifecycle.restore(deleted.doc, ItemId.event(K.EVENT_BLACKOUT), actx(), authenticated = false) as Restored.Done
        assertEquals(EventAccess.Open, EventDetailState.build(back.doc, K.EVENT_BLACKOUT, false).access)
        assertNull(ItemLifecycle.delete(deleted.doc, ItemId.event(K.EVENT_BLACKOUT), actx()))
    }

    @Test
    fun `WLD-29 the three modes and the start year`() {
        val ui = WorldTimeState.build(doc, readOnly = false)
        assertEquals(listOf("연도만", "세계 달력 만들기", "시간 없이"), ui.options.map { it.title })
        assertEquals(listOf("예: 1200년", "달 이름 · 한 달의 날 수 · 요일을 직접", "연표 끔"), ui.options.map { it.subtitle })
        assertEquals(WorldTimeMode.YEAR_ONLY, ui.options.single { it.selected }.mode)
        assertEquals("1200", ui.startYear)
        assertTrue(ui.showStartYear)
        assertNull(ui.calendarLine)
        assertFalse(WorldTimeState.build(withMode(doc, WorldTimeMode.NONE), false).showStartYear)
        val cal = withMode(doc, WorldTimeMode.CALENDAR, WorldCalendar(months = listOf(CalendarMonth("가달", 30), CalendarMonth("나달", 31))))
        assertEquals("달력: 가달 · 나달", WorldTimeState.build(cal, false).calendarLine)
        // Calendar mode before the editor (WLD-30) made any month: renders like 연도만, no line.
        assertNull(WorldTimeState.build(withMode(doc, WorldTimeMode.CALENDAR), false).calendarLine)
        assertTrue(TimelineState.build(withMode(doc, WorldTimeMode.CALENDAR), TimelineView.EPISODE, null, false).available)
    }

    @Test
    fun `WLD-29 editing in 연도만 keeps a stored month and day`() {
        val ctx = WorldTestFixture.ctx()
        val dated = doc.copy(world = doc.world.copy(events = doc.world.events.map {
            if (it.key == K.EVENT_BLACKOUT) it.copy(worldDate = WorldDate(1195, 2, 3)) else it
        }))
        val draft = EventDraft.of(dated.index.event(K.EVENT_BLACKOUT)!!)
        assertFalse(EventForms.ui(dated, draft, editing = true, hiddenOpen = false).dateInvalid)
        assertFalse(EventForms.ui(dated, draft.copy(year = ""), editing = true, hiddenOpen = false).dateInvalid)
        val saved = EventForms.save(dated, draft.copy(title = "대정전의 밤"), K.EVENT_BLACKOUT, ctx) as EventSave.Saved
        assertEquals(WorldDate(1195, 2, 3), saved.doc.index.event(K.EVENT_BLACKOUT)!!.worldDate)
        val moved = EventForms.save(dated, draft.copy(year = "1196"), K.EVENT_BLACKOUT, ctx) as EventSave.Saved
        assertEquals(WorldDate(1196, 2, 3), moved.doc.index.event(K.EVENT_BLACKOUT)!!.worldDate)
    }

    @Test
    fun `WLD-13 a story event on a hidden scene is locked, and hidden places read 숨긴 항목 in the form`() {
        val event = doc.index.event(K.EVENT_JOINS_SOCIETY)!!
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == event.sceneKey) it.copy(hidden = true) else it }))
        assertEquals(EventAccess.Locked, EventDetailState.build(hidden, K.EVENT_JOINS_SOCIETY, hiddenOpen = false).access)
        assertEquals(EventAccess.Open, EventDetailState.build(hidden, K.EVENT_JOINS_SOCIETY, hiddenOpen = true).access)
        assertEquals("숨긴 씬", TimelineState.build(hidden, TimelineView.EPISODE, null, false).rows.single { it.eventKey == K.EVENT_JOINS_SOCIETY }.title)
        val hiddenPlace = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_LOWER_STREET) it.copy(hidden = true) else it }))
        val ref = PlaceRef(PlaceRefKind.PLACE, K.PLACE_LOWER_STREET)
        assertEquals("숨긴 항목", EventForms.placeLabel(hiddenPlace, ref, hiddenOpen = false))
        assertEquals("하층 거리", EventForms.placeLabel(hiddenPlace, ref, hiddenOpen = true))
    }
}
