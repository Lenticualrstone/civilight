package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.checks.CheckInputs
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.checks.ProjectChecks
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.feature.story.check.CheckFixes
import io.github.lenticualrstone.civilight.feature.story.check.CheckModel
import io.github.lenticualrstone.civilight.feature.story.check.FixEffect
import io.github.lenticualrstone.civilight.feature.story.check.TravelLook
import io.github.lenticualrstone.civilight.feature.story.check.TravelLooks
import io.github.lenticualrstone.civilight.feature.story.common.MiniMaps
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** STO-30 card vs compact rows, the card's mini map, `한꺼번에 ›` and the STO-43 selected row. */
class TravelLookTest {
    private val inputs = CheckInputs(now = Fx.ctx().now, zone = Fx.zone, hiddenUnlocked = true)

    /** TRV-04: 하르트's ▲ is acknowledged in the fixture (`ACK_TRAVEL_HART`); reverted it is open. */
    private val open = Acks.revert(Fx.doc(), K.ACK_TRAVEL_HART)
    private val report = ProjectChecks.run(open, inputs)
    private val shortage = report.open.single { it.checkType == CheckType.TRAVEL_SHORTAGE }

    @Test
    fun trv04_aShortageIsTheCardWithBarsDetailAndChips_STO30() {
        val look = TravelLooks.of(shortage, CheckFilter.TRAVEL) as TravelLook.Card
        assertEquals("▲ 이동 시간 부족", shortage.title)
        assertEquals("하르트", shortage.tag)
        assertEquals("1-7 씬 2 상층 관문 → 씬 3 결사의 문", look.warning.routeLine)
        assertEquals("도보 2일 4시간", look.warning.needText)
        assertEquals("9시간 40분 – 12시간 40분", look.warning.haveText)
        assertEquals("경로 38 km · 도보 15 km · 하루 8시간 · 1일째 저녁(17–20) → 2일째 05:40", look.detail)
        assertEquals(listOf("씬 3 · +2일로", "말 · 약 7시간 40분", "맞음"), shortage.actions.map { it.label })
        // Same card under 전체.
        assertTrue(TravelLooks.of(shortage, CheckFilter.ALL) is TravelLook.Card)
    }

    @Test
    fun underAcknowledgedATravelRowIsCompact_STO41() {
        val acked = ProjectChecks.run(Fx.doc(), inputs).acknowledged.single { it.checkType == CheckType.TRAVEL_SHORTAGE }
        val look = TravelLooks.of(acked, CheckFilter.ACKNOWLEDGED) as TravelLook.Compact
        assertEquals(Marker.WARNING, look.marker)
        assertEquals("이동 시간 부족 · 하르트", look.title)
        assertEquals("1-7 씬 2 상층 관문 → 씬 3 결사의 문", look.line)
        assertFalse(look.trailingAction)
    }

    @Test
    fun noPlaceIsCompactWithBulkAtTheRight_andOpensBulkWithItsScenes() {
        val row = report.open.single { it.checkType == CheckType.TRAVEL_NO_PLACE }
        val look = TravelLooks.of(row, CheckFilter.TRAVEL) as TravelLook.Compact
        assertEquals(Marker.NOTICE, look.marker)
        assertEquals("장소 없음 · 씬 2개", look.title)
        assertEquals("1-5 씬 3 · 1-10 씬 1 · 동선 · 점검에서 빠짐", look.line)
        assertTrue(look.trailingAction)
        val bulk = row.actions.single { it.kind == FixKind.OPEN_BULK }
        assertEquals("한꺼번에 ›", bulk.label)
        val effect = CheckFixes.effect(open, K.PROJECT, row, bulk, inputs) as FixEffect.OpenBulk
        assertEquals(row.subjectKeys, effect.sceneKeys)
    }

    @Test
    fun otherRowsArePlain() {
        val speaker = report.open.first { it.checkType == CheckType.SPEAKER_MISSING }
        assertEquals(TravelLook.Plain, TravelLooks.of(speaker, CheckFilter.ALL))
    }

    @Test
    fun theCardMapHasBothPinsAndTheRoute() {
        val warning = shortage.travel!!
        val found = MiniMaps.travel(open, warning)
        assertNotNull(found)
        val map = found!!
        assertEquals(2, map.pins.size)
        assertTrue(map.lines.isNotEmpty())
        assertTrue(map.lines.none { it.dashed })
        val b = map.bounds
        assertTrue(map.pins.all { it.at.x in b.left..b.right && it.at.y in b.top..b.bottom })
    }

    @Test
    fun sto43_theRowWhosePositionTheRightPaneShowsIsSelected() {
        val p = K.PROJECT
        val doc = Fx.doc()
        val speaker = ProjectChecks.run(doc, inputs).open.first { it.checkType == CheckType.SPEAKER_MISSING }
        val target = CheckModel.locationTarget(doc, speaker)!!
        val stack = listOf(ProjectHome(p), CheckCenter(p), CheckFixes.scriptAt(doc, p, target))
        val selected = CheckModel.selectedKey(stack, p)
        assertEquals(target, selected)
        assertTrue(CheckModel.isSelected(doc, speaker, selected))
        // No script beside the list (cover, or the list alone): nothing is selected.
        assertNull(CheckModel.selectedKey(listOf(ProjectHome(p), CheckCenter(p)), p))
        assertNull(CheckModel.selectedKey(listOf(CheckCenter(p), ProjectHome(p), Script(p, sceneKey = target)), p))
    }
}
