package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.checks.Fx.types
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The example project 「잿빛 새벽의 등불」 at 최신 with default settings (fixture.md "Checks"):
 * every check of the registry run once, with expected counts.
 */
class FixtureChecksTest {
    private val report = Fx.run(Fx.doc())

    /**
     * Eight open items and `맞다고 표시한 것 2`. The fixture's acknowledgements store the real
     * `domain:story` fingerprints; if the story engine changes its fingerprint parts, the two
     * acknowledgements turn void here and the fixture's values must be recomputed.
     */
    @Test
    fun fixtureRaisesEightOpenItemsAndTwoAcknowledged() {
        assertEquals(
            listOf(
                CheckType.TERM_BEFORE_REVEAL,
                CheckType.TERM_CANDIDATE_BEFORE_REVEAL, CheckType.TERM_CANDIDATE_BEFORE_REVEAL, CheckType.TERM_CANDIDATE_BEFORE_REVEAL,
                CheckType.SPEAKER_MISSING,
                CheckType.VOICE_EMPTY,
                CheckType.DELETED_ITEM,
                CheckType.TRAVEL_NO_PLACE,
            ),
            report.types(),
        )
        assertEquals(8, report.count(CheckFilter.ALL))
        assertEquals(2, report.count(CheckFilter.ACKNOWLEDGED))
        assertFalse(report.scanning)
        assertEquals(
            listOf(CheckType.TRAVEL_SHORTAGE to K.ACK_TRAVEL_HART, CheckType.TIME_CLOCK_BAND_MISMATCH to K.ACK_CLOCK_BAND),
            report.acknowledged.map { it.checkType to CheckAcks.find(report.doc, it)!!.key },
        )
        assertTrue(CheckAcks.voided(report).isEmpty())
        val chips = CheckCenterView.header(report).chips.map { it.label }
        assertEquals(listOf("전체 8", "용어 공개 4", "화자 1", "이동 1", "보이스 1", "변화", "지운 항목 1", "맞다고 표시한 것 2"), chips)
    }

    /** Each registry row's check function on the fixture (open and acknowledged items). */
    @Test
    fun everyRegistryRowAgainstTheFixture() {
        val expected = mapOf(
            CheckType.TERM_BEFORE_REVEAL to 1,
            CheckType.TERM_CANDIDATE_BEFORE_REVEAL to 3,
            CheckType.TERM_NO_REVEAL to 0,
            CheckType.SPEAKER_MISSING to 1,
            CheckType.VOICE_EMPTY to 1,
            CheckType.VOICE_FORMAT_DIFFERS to 0,
            CheckType.VOICE_DUPLICATE to 0,
            CheckType.CHANGE_ANCHOR_DELETED to 0,
            CheckType.DELETED_ITEM to 1,
            CheckType.TRAVEL_SHORTAGE to 1,
            CheckType.TRAVEL_SAME_TIME_TWO_PLACES to 0,
            CheckType.TRAVEL_SIMULTANEOUS to 0,
            CheckType.TRAVEL_NO_PLACE to 1,
            CheckType.TRAVEL_NO_SCALE to 0,
            CheckType.TIME_CLOCK_BAND_MISMATCH to 1,
            CheckType.TIME_ORDER to 0,
            CheckType.TRANSITION_STALE to 0,
            CheckType.TRANSITION_PLACE_BEFORE_REVEAL to 0,
            CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE to 0,
            CheckType.HIDDEN_LOCKED_GROUP to 0,
        )
        val doc = Fx.doc()
        val inputs = Fx.inputs()
        assertEquals(CheckType.ALL, expected.keys.toList())
        for (definition in CheckRegistry.all) {
            val items = definition.compute(doc, inputs)
            assertEquals(definition.checkType, expected.getValue(definition.checkType), items.size)
            assertTrue(items.all { it.checkType == definition.checkType })
            assertEquals(items, report.ofType(definition.checkType))
        }
    }

    /** Open and acknowledged items alike. */
    @Test
    fun eachItemAtItsPlace() {
        val byType = report.results.groupBy { it.checkType }
        assertEquals("1-4 씬 2", byType.getValue(CheckType.TERM_BEFORE_REVEAL).single().location)
        assertEquals(listOf(K.LINK_L42), byType.getValue(CheckType.TERM_BEFORE_REVEAL).single().subjectKeys)
        assertEquals(listOf("1-2 씬 1", "1-3 씬 2", "1-6 씬 1"), byType.getValue(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).map { it.location })
        assertEquals("1-7 씬 2", byType.getValue(CheckType.SPEAKER_MISSING).single().location)
        assertEquals(listOf(K.BLOCK_1_7_027), byType.getValue(CheckType.SPEAKER_MISSING).single().subjectKeys)
        assertEquals("인물 · 리엔", byType.getValue(CheckType.VOICE_EMPTY).single().location)
        assertEquals("◇ 지운 인물 · 노아", byType.getValue(CheckType.DELETED_ITEM).single().title)
        assertEquals(listOf(K.HART, K.SCENE_1_7_2, K.SCENE_1_7_3), byType.getValue(CheckType.TRAVEL_SHORTAGE).single().subjectKeys)
        assertEquals("◇ 장소 없음 · 씬 2개", byType.getValue(CheckType.TRAVEL_NO_PLACE).single().title)
        assertEquals(listOf(K.SCENE_1_5_1), byType.getValue(CheckType.TIME_CLOCK_BAND_MISMATCH).single().subjectKeys)
    }

    @Test
    fun withoutNoPlaceNoticeTheFixtureRaisesSeven() {
        // fixture.md conflict 23: "Base raises 7 open items".
        val report = Fx.run(Fx.doc(), Fx.inputs(travel = TravelSettings(checkNoPlace = false)))
        assertEquals(7, report.count(CheckFilter.ALL))
        assertTrue(report.ofType(CheckType.TRAVEL_NO_PLACE).isEmpty())
    }

    /** STO CHK-01 / CNV T-CNV-23 with their Givens: term state A (no alias 빛돌) and no `◇ 장소 없음`. */
    @Test
    fun chk01_allCountsBothMarkers() {
        val doc = Fx.doc().let { d ->
            d.copy(world = d.world.copy(terms = d.world.terms.map { t ->
                if (t.key == K.TERM_ORIGINIUM_LANTERN) t.copy(aliases = t.aliases - "빛돌") else t
            }))
        }
        val report = Fx.run(doc, Fx.inputs(travel = TravelSettings(checkNoPlace = false)))
        assertEquals(
            listOf("▲ 공개 전 사용", "◇ 공개 전 후보", "▲ 화자 없음", "▲ 보이스 비어 있음", "◇ 지운 인물 · 노아"),
            report.open.map { it.title },
        )
        assertEquals("전체 5", CheckCenterView.header(report).chips.first().label)
    }

    @Test
    fun rowsPutWarningsFirstAndNestCandidatesUnderTheirTerm() {
        val rows = CheckCenterView.rows(report)
        val items = rows.filterIsInstance<CheckListRow.Item>()
        assertEquals(rows.size, items.size)
        assertEquals(
            listOf("▲ 공개 전 사용", "▲ 화자 없음", "▲ 보이스 비어 있음", "◇ 장소 없음 · 씬 2개", "◇ 지운 인물 · 노아"),
            items.map { it.result.title },
        )
        val term = items.first()
        assertEquals(listOf("1-2 씬 1", "1-3 씬 2", "1-6 씬 1"), term.subRows.map { it.location })
        assertEquals(listOf("원고로 가기", "공개 시점 옮기기", "맞음"), term.actions.map { it.label })
        assertEquals(listOf("원고로 가기", "잇기", "아님"), term.subRows.first().actions.map { it.label })
    }

    @Test
    fun rowCopyFollowsTheRegistry() {
        val term = report.ofType(CheckType.TERM_BEFORE_REVEAL).single()
        assertEquals("세라: “오리지늄은 타지 않아. 빛만 남지.”", term.body)
        assertEquals("오리지늄", term.body!!.substring(term.highlight!!.start, term.highlight!!.end))
        assertEquals("‘오리지늄 등불’ 첫 공개 1-7 · 사용 1-4", term.detail)

        val candidate = report.ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).first()
        assertEquals("◇ 공개 전 후보 · 리엔: “등불 속 그 돌, 오리지… 뭐라고 했죠?” · ‘오리지늄 등불’ 끊김 · 첫 공개 1-7", candidate.line)
        assertEquals("오리지", candidate.body!!.substring(candidate.highlight!!.start, candidate.highlight!!.end))

        val speaker = report.ofType(CheckType.SPEAKER_MISSING).single()
        assertEquals(listOf("화자 고르기", "나레이션으로 바꾸기"), speaker.actions.map { it.label })
        assertEquals("대사 블록 1개", speaker.detail)

        val voice = report.ofType(CheckType.VOICE_EMPTY).single()
        assertEquals("▲ 보이스 비어 있음 · 리엔 · 신뢰 상승 후 · 보이스 2/5 비어 있음", voice.line)
        assertEquals(listOf("보이스 쓰기"), voice.actions.map { it.label })
        assertEquals(K.VOICE_RIEN_AFTER_TRUST, voice.actions.single().target)

        val noPlace = report.ofType(CheckType.TRAVEL_NO_PLACE).single()
        assertEquals("1-5 씬 3 · 1-10 씬 1 · 동선 · 점검에서 빠짐", noPlace.body)
        assertEquals(listOf("한꺼번에 ›"), noPlace.actions.map { it.label })
    }

    /** STO CHK-02 / CNV T-CNV-29: nothing open, two acknowledgements stored. */
    @Test
    fun chk02_emptyState() {
        val doc = Fx.allResolved(Fx.doc())
        val resolved = Fx.run(doc, Fx.inputs(travel = TravelSettings(checkNoPlace = false)))
        assertTrue(resolved.open.isEmpty())
        val empty = CheckCenterView.empty(resolved)!!
        assertEquals("어긋난 곳 없음", empty.title)
        assertEquals("마지막 확인 · 방금 · 1-1 ~ 1-7", empty.line)
        assertEquals(listOf("원고로", "맞다고 표시한 것 2"), empty.buttons)
        assertEquals("전체 0", CheckCenterView.header(resolved).chips.first().label)
        assertTrue(CheckCenterView.rows(resolved).isEmpty())
        assertNull(CheckCenterView.empty(report))
    }
}
