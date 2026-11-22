package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S1
import io.github.lenticualrstone.civilight.domain.story.Fx.S2
import io.github.lenticualrstone.civilight.domain.story.Fx.S3
import io.github.lenticualrstone.civilight.domain.story.Fx.S4
import io.github.lenticualrstone.civilight.domain.story.Fx.S5
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.rel
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.scene.DayChip
import io.github.lenticualrstone.civilight.domain.story.scene.SceneWhere
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The `씬 · 장소와 때` sheet values (STO-20) assembled from the engines. */
class SceneWhereTest {
    private val doc = Fx.doc()

    @Test
    fun sceneWhereForS9() {
        val s = SceneWhere.sheet(doc, S9)
        assertEquals("씬 3 · 장소와 때", s.title)
        assertEquals("결사의 문", s.place)
        assertEquals("지역으로 바꾸기", s.placeToggle)
        assertEquals("회상 | 연쇄 · 이동 점검에서 빠짐", s.flashbackRow)
        assertEquals("때 · 직전 씬 기준", s.baseLine?.label)
        assertEquals("씬 2 · 1-7 · 1일째 저녁", s.baseLine?.value)
        assertEquals(DayChip.PLUS_ONE, s.dayChip)
        assertEquals(Band.DAWN, s.band)
        assertEquals("05:40", s.clock.toString())
        assertEquals(TimePrecision.EXACT, s.precision)
        assertEquals("1-7 · 2일째 · 새벽 | 이야기 약 13일째 · 연표에서", s.resultLine)
        assertFalse(s.rebaseToPrevious)
        assertNull(s.orderLine)
        assertEquals("세라 · 오웬 · 리엔 · 하르트", s.speakers)
        assertEquals("마르타", s.silentCast)
        assertEquals(TransitionMode.NONE, s.transition.mode)
        // The fixture holds a `맞음` on 하르트's ▲ (STO-30, TRV-07), so the sheet shows no travel block.
        assertTrue(s.travel.isEmpty())
    }

    /** STO-20 travel block: 하르트's ▲ once the fixture's `맞음` is reverted (STO-41 `되돌리기`). */
    @Test
    fun sceneWhereTravelBlockForS9() {
        val open = Acks.revert(doc, ExampleKeys.ACK_TRAVEL_HART)
        val block = SceneWhere.sheet(open, S9).travel.single()
        assertEquals("▲ 이동 시간 | 하르트 · 씬 2 상층 관문에서", block.title)
        assertEquals("9시간 40분 – 12시간 40분 · 도보 2일 4시간", block.line)
        assertEquals(listOf("+2일로", "말 · 약 7시간 40분", "맞음"), block.chips)
        assertTrue(SceneWhere.sheet(open, S9, TravelSettings(checkTravel = false)).travel.isEmpty())
    }

    @Test
    fun flashbackSheet() {
        val s = SceneWhere.sheet(doc, S5)
        assertTrue(s.flashback)
        assertNull(s.baseLine)
        assertEquals("연도 1195 · 대정전 그날 | 이야기 시작 5년 전 · 연표에서", s.resultLine)
    }

    @Test
    fun firstSceneOnlyDirectAndBaseLineWhenNotPrevious() {
        val first = SceneWhere.sheet(doc, S1)
        assertTrue(first.onlyDirect)
        assertEquals(DayChip.DIRECT, first.dayChip)
        val s4 = SceneWhere.sheet(doc, S4)
        assertEquals("때 · 1-2 씬 1 기준", s4.baseLine?.label)
        assertFalse(s4.rebaseToPrevious)
        val withS3 = doc.withTime(S3, rel(S2, 0, Band.DAY))
        assertTrue(SceneWhere.sheet(withS3, S4).rebaseToPrevious)
    }
}
