package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.checks.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.domain.story.travel.TravelCheck
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rows 10–14 (`이동`, rule 18) against small edits of the example project. */
class TravelChecksTest {
    private val doc = Fx.doc()

    /** STO TRV-04 … TRV-06 as a check-center row (the fixture holds it under `맞다고 표시한 것`; the row is the same). */
    @Test
    fun trv04_shortageRowWithItsChips() {
        val item = Fx.run(doc).ofType(CheckType.TRAVEL_SHORTAGE).single()
        assertEquals("▲ 이동 시간 부족", item.title)
        assertEquals("하르트", item.tag)
        assertEquals(Marker.WARNING, item.marker)
        assertEquals(CheckFilter.TRAVEL, item.filter)
        assertEquals("1-7 씬 2 상층 관문 → 씬 3 결사의 문", item.body)
        assertEquals("경로 38 km · 도보 15 km · 하루 8시간 · 1일째 저녁(17–20) → 2일째 05:40", item.detail)
        val card = assertNotNullAndGet(item.travel)
        assertEquals("도보 2일 4시간", card.needText)
        assertEquals("9시간 40분 – 12시간 40분", card.haveText)
        assertEquals(listOf("씬 3 · +2일로", "말 · 약 7시간 40분", "맞음"), item.actions.map { it.label })
        assertEquals(listOf(FixKind.DELAY_SCENE, FixKind.USE_MODE, FixKind.ACKNOWLEDGE), item.actions.map { it.kind })
        assertEquals(2, item.actions.first().days)
    }

    /** STO TRV-07: the `맞음` holds until the pair's inputs change; then it is void and pruned. */
    @Test
    fun trv07_matchIsVoidedByATimeChange() {
        val report = Fx.run(doc)
        assertTrue(report.isAcknowledged(report.ofType(CheckType.TRAVEL_SHORTAGE).single()))
        assertEquals(1, report.count(CheckFilter.TRAVEL)) // only `◇ 장소 없음`

        // S8 (1-7 씬 2) moves from 저녁 to 낮: still short, but another fingerprint.
        val s8 = doc.index.unit(K.SCENE_1_7_2)!!.time as SceneTime.Relative
        val changed = doc.withTime(K.SCENE_1_7_2, s8.copy(band = Band.DAY.open()))
        val after = Fx.run(changed)
        val item = after.ofType(CheckType.TRAVEL_SHORTAGE).single()
        assertTrue(!after.isAcknowledged(item))
        assertEquals(listOf(K.ACK_TRAVEL_HART), CheckAcks.voided(after).map { it.key })
        val pruned = CheckAcks.prune(after)
        assertTrue(pruned.checks.acknowledgements.none { it.key == K.ACK_TRAVEL_HART })
        assertEquals(after.count(CheckFilter.ALL), Fx.run(pruned).count(CheckFilter.ALL))
    }

    @Test
    fun trv06_modeChipClearsTheWarning() {
        val report = Fx.run(doc)
        val item = report.ofType(CheckType.TRAVEL_SHORTAGE).single()
        val chip = item.travel!!.modeChips.single()
        val horse = TravelCheck.applyModeChip(doc, item.travel!!, chip, Fx.ctx())
        assertTrue(Fx.run(horse).ofType(CheckType.TRAVEL_SHORTAGE).isEmpty())
    }

    /** STO CHK-06: the settings switch rows 10–12 and 13 off; nothing is voided by a switched-off run. */
    @Test
    fun chk06_settingsSwitchTravelRowsOff() {
        val off = Fx.run(doc, Fx.inputs(travel = TravelSettings(checkTravel = false, checkNoPlace = false)))
        assertTrue(off.results.none { it.filter == CheckFilter.TRAVEL && it.checkType.startsWith("travel.") })
        assertTrue(CheckAcks.voided(off).none { it.checkType == CheckType.TRAVEL_SHORTAGE })
        val noPlaceOff = Fx.run(doc, Fx.inputs(travel = TravelSettings(checkNoPlace = false)))
        assertTrue(noPlaceOff.ofType(CheckType.TRAVEL_NO_PLACE).isEmpty())
        assertEquals(1, noPlaceOff.ofType(CheckType.TRAVEL_SHORTAGE).size)
    }

    /** STO TRV-12: places on a map without scale cannot be measured (`◇ 축척 없음 · 점검 못 함`), no ▲. */
    @Test
    fun trv12_noScaleCannotCheck() {
        val noScale = doc.copy(maps = doc.maps.mapValues { (k, m) -> if (k == K.MAP_LOWER_DISTRICT) m.copy(scale = null) else m })
        val report = Fx.run(noScale)
        assertTrue(report.ofType(CheckType.TRAVEL_SHORTAGE).isEmpty())
        val rows = report.ofType(CheckType.TRAVEL_NO_SCALE)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.cannotCheck && it.title == "◇ 축척 없음 · 점검 못 함" && it.marker == Marker.NOTICE })
        assertTrue(rows.all { it.actions.single().label == "축척 맞추기" })
        // One row per scene pair.
        assertEquals(rows.size, rows.map { it.subjectKeys }.distinct().size)
        val pair = rows.single { it.subjectKeys == listOf(K.SCENE_1_7_2, K.SCENE_1_7_3) }
        assertEquals("하르트", pair.tag)
    }

    /** Rule 19: the no-scale row names both places, so a hidden place keeps it out while locked. */
    @Test
    fun noScaleRowWithAHiddenPlaceStaysOutWhileLocked() {
        val noScale = doc.copy(maps = doc.maps.mapValues { (k, m) -> if (k == K.MAP_LOWER_DISTRICT) m.copy(scale = null) else m })
        val hidden = noScale.copy(world = noScale.world.copy(places = noScale.world.places.map { if (it.key == K.PLACE_UPPER_GATE) it.copy(hidden = true) else it }))
        val pair = { unlocked: Boolean ->
            Fx.run(hidden, Fx.inputs(hiddenUnlocked = unlocked)).ofType(CheckType.TRAVEL_NO_SCALE)
                .single { it.subjectKeys == listOf(K.SCENE_1_7_2, K.SCENE_1_7_3) }
        }
        assertTrue(pair(false).lockedHidden)
        assertTrue(!pair(true).lockedHidden)
    }

    /** STO-30 compact rows: `▲ 같은 때 두 장소 · 하르트 | … · 둘 다 …` (row 11), then `동시에 두 장소` (row 12). */
    @Test
    fun trv09_trv10_sameTimeAndSimultaneousRows() {
        val s8 = doc.index.unit(K.SCENE_1_7_2)!!.time as SceneTime.Relative
        // 1-7 씬 3 on the same day and band as 씬 2.
        val sameBand = doc.withTime(K.SCENE_1_7_3, SceneTime.Relative(baseKey = K.SCENE_1_7_2, dayMin = 0, dayMax = 0, band = s8.band))
        val same = Fx.run(sameBand).ofType(CheckType.TRAVEL_SAME_TIME_TWO_PLACES).single { it.tag == "하르트" }
        assertEquals("▲ 같은 때 두 장소", same.title)
        assertEquals(listOf(K.HART, K.SCENE_1_7_2, K.SCENE_1_7_3), same.subjectKeys)
        assertTrue(same.body, same.body!!.startsWith("1-7 씬 2 상층 관문 · 씬 3 결사의 문 · 둘 다 "))
        assertEquals(same.travel!!.compactLine, "같은 때 두 장소 · 하르트 | " + same.body)
        assertEquals("맞음", same.actions.last().label)
        assertTrue(Fx.run(sameBand).ofType(CheckType.TRAVEL_SHORTAGE).none { it.tag == "하르트" })

        // Both at 18:00 the same day: `동시에 두 장소`.
        val clock = io.github.lenticualrstone.civilight.model.ClockTime.of(18, 0)
        val exact = doc
            .withTime(K.SCENE_1_7_2, s8.copy(band = null, clock = clock))
            .withTime(K.SCENE_1_7_3, SceneTime.Relative(baseKey = K.SCENE_1_7_2, dayMin = 0, dayMax = 0, clock = clock))
        val simultaneous = Fx.run(exact).ofType(CheckType.TRAVEL_SIMULTANEOUS).single { it.tag == "하르트" }
        assertEquals("▲ 동시에 두 장소", simultaneous.title)
        assertTrue(simultaneous.body, simultaneous.body!!.startsWith("1-7 씬 2 상층 관문 · 씬 3 결사의 문 · 둘 다 ") && simultaneous.body!!.endsWith("18:00"))
        assertEquals(Marker.WARNING, simultaneous.marker)
        assertEquals(CheckFilter.TRAVEL, simultaneous.filter)
    }

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
