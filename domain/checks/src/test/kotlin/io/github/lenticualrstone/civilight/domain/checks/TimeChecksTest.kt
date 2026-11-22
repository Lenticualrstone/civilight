package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.checks.Fx.withTime
import io.github.lenticualrstone.civilight.domain.checks.Fx.withUnit
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rows 15–18 (time and transition notices, filter `이동` `ED`) against small edits. */
class TimeChecksTest {
    private val doc = Fx.doc()

    private fun clockRel(base: String, h: Int, m: Int) =
        SceneTime.Relative(baseKey = base, dayMin = 0, dayMax = 0, clock = ClockTime.of(h, m))

    /** STO ORD-01 and ORD-03 as check-center rows. */
    @Test
    fun ord01_ord03_orderNoticeAndItsMatch() {
        // P = 1-7 씬 2 at 같은 날 · 20:00, X = 1-7 씬 3 at 같은 날 · 11:00 after it.
        val edited = doc.withTime(K.SCENE_1_7_2, clockRel(K.SCENE_1_7_1, 20, 0)).withTime(K.SCENE_1_7_3, clockRel(K.SCENE_1_7_2, 11, 0))
        val report = Fx.run(edited)
        val notice = report.ofType(CheckType.TIME_ORDER).single { it.subjectKeys.first() == K.SCENE_1_7_3 }
        assertEquals(listOf(K.SCENE_1_7_3, K.SCENE_1_7_2), notice.subjectKeys)
        assertEquals("◇ 순서와 때가 다름", notice.title)
        assertEquals("1-7 씬 3 · 직전 씬보다 앞 · 9시간 전", notice.body)
        assertEquals(Marker.NOTICE, notice.marker)
        assertEquals(CheckFilter.TRAVEL, notice.filter)
        assertEquals(listOf("원고로 가기", "맞음"), notice.actions.map { it.label })

        val acked = CheckAcks.acknowledge(edited, notice, Fx.ctx())
        val after = Fx.run(acked)
        assertTrue(after.isAcknowledged(after.ofType(CheckType.TIME_ORDER).single { it.subjectKeys.first() == K.SCENE_1_7_3 }))

        // X moves to 10:00: the difference changes, the acknowledgement is void.
        val moved = acked.withTime(K.SCENE_1_7_3, clockRel(K.SCENE_1_7_2, 10, 0))
        val reopened = Fx.run(moved)
        val again = reopened.ofType(CheckType.TIME_ORDER).single { it.subjectKeys.first() == K.SCENE_1_7_3 }
        assertFalse(reopened.isAcknowledged(again))
        assertEquals("1-7 씬 3 · 직전 씬보다 앞 · 10시간 전", again.body)
        assertEquals(1, CheckAcks.voided(reopened).count { it.checkType == CheckType.TIME_ORDER })
    }

    @Test
    fun ord02_sameBandsRaiseNothing() {
        val sameMorning = doc.withTime(K.SCENE_1_7_3, SceneTime.Relative(baseKey = K.SCENE_1_7_2, dayMin = 0, dayMax = 0, band = (doc.index.unit(K.SCENE_1_7_2)!!.time as SceneTime.Relative).band))
        assertTrue(Fx.run(sameMorning).ofType(CheckType.TIME_ORDER).none { it.subjectKeys.first() == K.SCENE_1_7_3 })
    }

    /** STO-19 row 15: the fixture's 1-5 씬 1 (`새벽` at 06:20) is acknowledged until the clock moves. */
    @Test
    fun clockBandMismatch() {
        val report = Fx.run(doc)
        val item = report.ofType(CheckType.TIME_CLOCK_BAND_MISMATCH).single()
        assertEquals("1-5 씬 1 · 새벽 06:20", item.body)
        assertTrue(report.isAcknowledged(item))

        val s = doc.index.unit(K.SCENE_1_5_1)!!.time as SceneTime.Relative
        val later = Fx.run(doc.withTime(K.SCENE_1_5_1, s.copy(clock = ClockTime.of(6, 40))))
        val moved = later.ofType(CheckType.TIME_CLOCK_BAND_MISMATCH).single()
        assertFalse(later.isAcknowledged(moved))
        assertEquals("1-5 씬 1 · 새벽 06:40", moved.body)

        val inBand = Fx.run(doc.withTime(K.SCENE_1_5_1, s.copy(clock = ClockTime.of(5, 20))))
        assertTrue(inBand.ofType(CheckType.TIME_CLOCK_BAND_MISMATCH).isEmpty())
    }

    /** R22 §2 row 17: 1-7 씬 4's direct text was written for 06:10. */
    @Test
    fun staleTransitionAfterATimeChange() {
        assertTrue(Fx.run(doc).ofType(CheckType.TRANSITION_STALE).isEmpty())
        val edited = doc.withTime(K.SCENE_1_7_4, clockRel(K.SCENE_1_7_3, 7, 0))
        val item = Fx.run(edited).ofType(CheckType.TRANSITION_STALE).single()
        assertEquals("◇ 전환 문구 · 쓴 뒤 때 바뀜", item.title)
        assertEquals("1-7 씬 4 · 06:10 → 07:00", item.body)
        assertEquals(listOf("자동으로", "문구 고치기", "맞음"), item.actions.map { it.label })
        assertEquals(CheckFilter.TRAVEL, item.filter)
        val acked = CheckAcks.acknowledge(edited, item, Fx.ctx())
        assertEquals(1, Fx.run(acked).acknowledged.count { it.checkType == CheckType.TRANSITION_STALE })
    }

    /** R22 §2 row 18: an automatic text naming a place shown on the map only from 1-7. */
    @Test
    fun placeBeforeRevealInAutomaticText() {
        val edited = doc.withUnit(K.SCENE_1_3_2) { it.copy(place = PlaceRef(PlaceRefKind.PLACE, K.PLACE_UPPER_GATE)) }
        val item = Fx.run(edited).ofType(CheckType.TRANSITION_PLACE_BEFORE_REVEAL).single()
        assertEquals("◇ 공개 전 장소 · 전환 문구", item.title)
        assertEquals("1-3 씬 2 · 상층 관문", item.body)
        assertEquals(listOf("직접 쓰기", "맞음"), item.actions.map { it.label })
    }
}
