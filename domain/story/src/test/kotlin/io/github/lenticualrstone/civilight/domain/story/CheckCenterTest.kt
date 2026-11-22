package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.checks.CheckCenter
import io.github.lenticualrstone.civilight.domain.story.checks.CheckEntry
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.CheckRow
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 점검 센터 registry semantics (20-story-time §4.13 CHK-01 … CHK-03). */
class CheckCenterTest {
    private val doc = Fx.doc()

    /** CHK-01's open items (emitted by world, SCR and cast; here as plain entries). */
    private val chk01 = listOf(
        CheckEntry(CheckType.TERM_BEFORE_REVEAL, listOf(K.LINK_L42), "a", "▲ 공개 전 사용", "1-4 씬 2", storyOrder = 40),
        CheckEntry(CheckType.TERM_CANDIDATE_BEFORE_REVEAL, listOf(K.block(1, 2, 3)), "b", "◇ 공개 전 후보", "1-2 씬 1", storyOrder = 12),
        CheckEntry(CheckType.SPEAKER_MISSING, listOf(K.block(1, 7, 27)), "c", "▲ 화자 없음", "1-7 씬 2", storyOrder = 70),
        CheckEntry(CheckType.DELETED_ITEM, listOf(K.NOAH), "d", "◇ 지운 인물 · 노아", "휴지통"),
        CheckEntry(CheckType.VOICE_EMPTY, listOf(K.RIEN), "e", "▲ 보이스 비어 있음", "인물 · 리엔"),
    )

    @Test
    fun chk01_allCountsBothMarkers() {
        assertEquals(5, CheckCenter.count(doc, chk01, CheckFilter.ALL))
        val chips = CheckCenter.chips(doc, chk01)
        assertEquals("전체 5", chips.first())
        assertEquals("용어 공개 2", chips[1])
        assertEquals("이동", chips[3])
        val rows = CheckCenter.rows(doc, chk01).map { (it as CheckRow.Item).entry }
        assertEquals(listOf(Marker.WARNING, Marker.WARNING, Marker.WARNING, Marker.NOTICE, Marker.NOTICE), rows.map { it.marker })
        assertEquals("▲ 공개 전 사용", rows.first().title)
        assertEquals("◇ 공개 전 후보", rows[3].title)
        assertNull(CheckCenter.empty(doc, chk01, "1-1 ~ 1-7"))
    }

    @Test
    fun chk02_emptyState() {
        val empty = CheckCenter.empty(doc, emptyList(), StoryOverview(doc).writtenRange())!!
        assertEquals("어긋난 곳 없음", empty.title)
        assertEquals("마지막 확인 · 방금 · 1-1 ~ 1-7", empty.line)
        assertEquals(listOf("원고로", "맞다고 표시한 것 2"), empty.buttons)
        assertEquals("전체 0", CheckCenter.chips(doc, emptyList()).first())
    }

    @Test
    fun chk03_lockedHiddenItemsCollapse() {
        val hidden = listOf(
            CheckEntry(CheckType.SPEAKER_MISSING, listOf("h1"), "x", "▲ 화자 없음", "1-6 씬 5", lockedHidden = true),
            CheckEntry(CheckType.TIME_ORDER, listOf("h2", "h3"), "y", "◇ 순서와 때가 다름", "1-6 씬 5", lockedHidden = true),
        )
        val rows = CheckCenter.rows(doc, hidden + chk01.take(1))
        assertEquals("▲ 숨긴 항목 관련 2 · 지문으로 보기", (rows.first() as CheckRow.HiddenGroup).text)
        assertEquals(2, rows.size)
    }

    @Test
    fun acknowledgedItemsMoveToTheirFilter() {
        val acked = Acks.acknowledge(doc, chk01[3], Fx.ctx())
        assertEquals(4, CheckCenter.count(acked, chk01, CheckFilter.ALL))
        assertEquals(1, CheckCenter.count(acked, chk01, CheckFilter.ACKNOWLEDGED))
        assertEquals(chk01[3], (CheckCenter.rows(acked, chk01, CheckFilter.ACKNOWLEDGED).single() as CheckRow.Item).entry)
        assertEquals(CheckFilter.TRAVEL, CheckCenter.filterOf(CheckType.TIME_ORDER))
    }
}
