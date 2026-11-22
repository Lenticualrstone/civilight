package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.Fx.withUnit
import io.github.lenticualrstone.civilight.feature.story.home.HomeModel
import io.github.lenticualrstone.civilight.feature.story.home.HomeShortcut
import io.github.lenticualrstone.civilight.feature.story.search.SearchModel
import io.github.lenticualrstone.civilight.feature.story.search.SearchTap
import io.github.lenticualrstone.civilight.feature.story.structure.SceneItem
import io.github.lenticualrstone.civilight.feature.story.structure.StructureModel
import io.github.lenticualrstone.civilight.feature.story.where.CalendarLabels
import io.github.lenticualrstone.civilight.feature.story.where.WhenEdit
import io.github.lenticualrstone.civilight.feature.story.where.WhereModel
import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.MonthDay
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.navigation.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The remaining sheet rows of this batch (STO-18 calendar result, STO-25 order line, STO-27
 * transition row, STO-28 mode chip), STO-16 in search, and the STO-11 marks on 스토리 and 홈.
 */
class SheetRowsTest {
    private val doc = Fx.doc()
    private val s9 = K.SCENE_1_7_3

    private fun ProjectDoc.withTime(key: String, time: SceneTime) = withUnit(key) { it.copy(time = time) }

    private fun calendarDoc(d: ProjectDoc = doc): ProjectDoc {
        val cal = WorldCalendar(months = listOf(CalendarMonth("서리달", 30), CalendarMonth("잿달", 30)), storyStart = MonthDay(1, 25))
        return d.copy(project = d.project.copy(worldTime = WorldTime(WorldTimeMode.CALENDAR.open(), 1200, cal)))
    }

    @Test
    fun sto18_theResultShowsTheCalendarDateWithNIljjaeSecond() {
        val cal = calendarDoc()
        assertEquals("1-2 · 서리달 26일 · 새벽 | 1일째 · 이야기 2일째 · 연표에서", WhereModel.build(cal, K.SCENE_1_2_1, false)!!.resultLine)
        // 대략: a date range (S9 is story day 11~15).
        assertEquals("1-7 · 잿달 5일 ~ 9일 · 새벽 | 2일째 · 이야기 약 13일째 · 연표에서", WhereModel.build(cal, s9, false)!!.resultLine)
        // Calendar off: the plain line; a flashback keeps its own line.
        assertEquals("1-7 · 2일째 · 새벽 | 이야기 약 13일째 · 연표에서", WhereModel.build(doc, s9, false)!!.resultLine)
        assertEquals("연도 1195 · 대정전 그날 | 이야기 시작 5년 전 · 연표에서", WhereModel.build(cal, K.SCENE_1_4_2, false)!!.resultLine)
        assertNull(CalendarLabels.resultLine(cal, s9, null))
    }

    @Test
    fun sto18_theCalendarNeverChangesStoredTimeOrControls() {
        val cal = calendarDoc()
        assertEquals(doc.story.units.map { it.time }, cal.story.units.map { it.time })
        val times = SceneTimes(doc)
        val calTimes = SceneTimes(cal)
        doc.manuscript.liveScenes.forEach { s ->
            assertEquals(times.timed(s.key)?.absolute, calTimes.timed(s.key)?.absolute)
            assertEquals(WhenEdit.controls(doc, s.key), WhenEdit.controls(cal, s.key))
        }
    }

    @Test
    fun ord01_theSheetShowsAnEarlierTimeAndNeverAWarning() {
        val p = doc.withTime(K.SCENE_1_7_2, SceneTime.Relative(K.SCENE_1_7_1, 0, 0, clock = ClockTime.of(20, 0)))
            .withTime(s9, SceneTime.Relative(K.SCENE_1_7_2, 0, 0, clock = ClockTime.of(11, 0)))
        val sheet = WhereModel.build(p, s9, false)!!.sheet
        assertEquals("직전 씬보다 앞 · 9시간 전", sheet.orderLine)
        assertTrue(sheet.travel.none { it.title.startsWith("▲ 순서") })
    }

    @Test
    fun ord02_bandsOnlyOnTheSameDayStraddleZero() {
        val p = doc.withTime(K.SCENE_1_7_2, SceneTime.Relative(K.SCENE_1_7_1, 1, 1, band = Band.MORNING.open()))
            .withTime(s9, SceneTime.Relative(K.SCENE_1_7_2, 0, 0, band = Band.MORNING.open()))
        assertNull(WhereModel.build(p, s9, false)!!.sheet.orderLine)
    }

    @Test
    fun trn01_theRowPreviewsTheAutoTextAndNeedsAPlace() {
        val row = WhereModel.build(doc, K.SCENE_1_3_2, false)!!.sheet.transition
        assertEquals(TransitionMode.AUTO, row.mode)
        assertEquals("결사의 문 · 아침", row.preview)
        assertFalse("no place, no 자동", WhereModel.build(doc, K.SCENE_1_3_1, false)!!.sheet.transition.autoEnabled)
    }

    @Test
    fun trn05_switchingAwayFromDirectKeepsTheText() {
        val s10 = K.SCENE_1_7_4
        val none = WhereModel.setTransitionMode(doc, s10, TransitionMode.NONE, Fx.ctx())
        assertEquals(TransitionMode.NONE, WhereModel.build(none, s10, false)!!.sheet.transition.mode)
        val back = WhereModel.setTransitionMode(none, s10, TransitionMode.DIRECT, Fx.ctx())
        assertEquals("결사의 문 앞 · 06:10", WhereModel.build(back, s10, false)!!.sheet.transition.directText)
    }

    @Test
    fun trv06_aModeChipStoresThePairsModeAndTheWarningGoes_STO28() {
        val open = Acks.revert(doc, K.ACK_TRAVEL_HART)
        val block = WhereModel.build(open, s9, false)!!.sheet.travel.single()
        val horse = block.warning.modeChips.single()
        assertEquals("말 · 약 7시간 40분", horse.label)
        val after = WhereModel.applyModeChip(open, block.warning, horse.modeKey, Fx.ctx())
        assertTrue(WhereModel.build(after, s9, false)!!.sheet.travel.isEmpty())
        // Stale chip on the new doc: nothing more changes.
        assertEquals(after, WhereModel.applyModeChip(after, block.warning, horse.modeKey, Fx.ctx()))
    }

    @Test
    fun sto16_insideTheScriptTheLensCommandIsLensToScene() {
        val p = doc.project.key
        val script = SearchModel.context(doc, Script(p, sceneKey = s9), locked = true, openChecks = 0)
        val items = ProjectSearch(doc, script).search("렌즈", SearchFilter.COMMAND).section(SearchSectionKind.COMMAND)!!.items
        val taps = items.map { SearchModel.tap(p, it, script) }
        assertTrue(SearchTap.LensToScene(s9) in taps)
        assertFalse("the script has no lens sheet (R11 §3)", SearchTap.Lens in taps)
    }

    @Test
    fun sto11_hiddenScenesAreMaskedWhileLockedAndTaggedWhenOpen() {
        val hidden = doc.withUnit(K.SCENE_1_7_4) { it.copy(hidden = true) }
        fun item(locked: Boolean) = StructureModel.build(hidden, listOf(K.EPISODE_1_7), locked, readOnly = false).items
            .filterIsInstance<SceneItem>().single { it.row.key == K.SCENE_1_7_4 }
        val locked = item(locked = true)
        assertTrue(locked.masked)
        assertEquals("씬 4 · 숨긴 씬", locked.row.label)
        assertFalse(locked.row.hiddenTag)
        val open = item(locked = false)
        assertFalse(open.masked)
        assertTrue(open.row.hiddenTag)
        // 1-level home rows carry the same tag.
        val mini = Fx.mini(listOf("씬"), Fx.node("S1", "첫 씬", "본문"), Fx.node("S2", "둘째"))
            .withUnit("S2") { it.copy(hidden = true) }
        val rows = HomeModel.build(mini, readOnly = false, locked = false, today = Fx.today, zone = Fx.zone).rows
        assertTrue(rows.single { it.key == "S2" }.hiddenTag)
        assertFalse(rows.single { it.key == "S1" }.hiddenTag)
    }

    @Test
    fun theTimelineShortcutIsLeftOutWithoutTime_WLD29() {
        assertTrue(HomeShortcut.TIMELINE in HomeModel.shortcuts(doc))
        val none = doc.copy(project = doc.project.copy(worldTime = doc.project.worldTime.copy(mode = WorldTimeMode.NONE.open())))
        assertFalse(HomeShortcut.TIMELINE in HomeModel.shortcuts(none))
        assertEquals(HomeShortcut.entries.size - 1, HomeModel.shortcuts(none).size)
    }
}
