package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S10
import io.github.lenticualrstone.civilight.domain.story.Fx.S2
import io.github.lenticualrstone.civilight.domain.story.Fx.S3
import io.github.lenticualrstone.civilight.domain.story.Fx.S5
import io.github.lenticualrstone.civilight.domain.story.Fx.S6
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.clock
import io.github.lenticualrstone.civilight.domain.story.Fx.rel
import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.Fx.withUnit
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.scene.HeadMenuRow
import io.github.lenticualrstone.civilight.domain.story.scene.SceneHeads
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.time.TimeChange
import io.github.lenticualrstone.civilight.domain.story.transition.StaleKind
import io.github.lenticualrstone.civilight.domain.story.transition.TransitionChecks
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.TransitionTemplate
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/** Rule 22: scene head and 전환 문구 (20-story-time §4.10 TRN-01 … 05; 10-script T-22-01 … 06). */
class TransitionTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    private fun tr(d: ProjectDoc = doc) = Transitions(d)
    private fun checks(d: ProjectDoc) = TransitionChecks(Transitions(d))
    private fun head(d: ProjectDoc, key: String) = SceneHeads(d).head(key)!!

    private val lowerDistrict = PlaceRef(PlaceRefKind.REGION, K.REGION_LOWER_DISTRICT)

    /** S10 at 07:00 (no dependent moves, stored directly). */
    private fun at0700(): ProjectDoc = TimeChange.apply(doc, S10, rel(S9, 0, clock = clock(7, 0)), CascadeMode.WITH_FOLLOWING, ctx)

    private fun gatePinFrom18(d: ProjectDoc): ProjectDoc = d.copy(
        maps = d.maps.mapValues { (_, m) ->
            m.copy(pins = m.pins.map { if (it.placeKey == K.PLACE_SOCIETY_GATE) it.copy(revealPoint = PointRef.unitStart(K.EPISODE_1_8)) else it })
        },
    )

    @Test
    fun trn01_autoTextPerProperties() {
        assertEquals("결사의 문 · 06:10", tr().autoText(S10))
        assertEquals("결사의 문 · 아침", tr(doc.withTime(S10, rel(S9, 0, Band.MORNING))).autoText(S10))
        assertEquals("잿더미 시장", tr().autoText(S6))
        val region = doc.withUnit(S2) { it.copy(place = lowerDistrict) }
        assertEquals("하층 구역 · 새벽", tr(region).autoText(S2))
        assertNull(tr().autoText(S3))
        assertFalse(tr().autoEnabled(S3))
        assertTrue(tr().autoEnabled(S10))
        assertEquals(doc, Transitions.setMode(doc, S3, TransitionMode.AUTO, ctx))
    }

    @Test
    fun trn02_placeOnlyTemplate() {
        assertEquals("결사의 문", tr().autoText(S10, TransitionTemplate.PLACE_ONLY))
        val setting = doc.copy(project = doc.project.copy(script = doc.project.script.copy(transitionTemplate = TransitionTemplate.PLACE_ONLY.open())))
        assertEquals("결사의 문", tr(setting).autoText(S10))
    }

    @Test
    fun trn03_directTextGoesStaleAfterATimeChange() {
        assertNull(checks(doc).stale(S10))
        val changed = at0700()
        assertEquals("결사의 문 앞 · 06:10", tr(changed).shownText(S10))
        val item = checks(changed).stale(S10)!!
        assertEquals(StaleKind.TIME, item.kind)
        assertEquals("◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00", item.line)
        assertEquals(listOf("자동으로", "문구 고치기", "맞음"), item.actions)

        val auto = Transitions.toAuto(changed, S10, ctx)
        assertEquals(TransitionMode.AUTO, tr(auto).mode(S10))
        assertEquals("결사의 문 · 07:00", tr(auto).shownText(S10))
        assertNull(checks(auto).stale(S10))
        assertEquals("결사의 문 앞 · 06:10", auto.unit(S10).transitionRuns?.plainText)
    }

    @Test
    fun trn03_rewriteTakesANewSnapshotAndAcknowledgementHolds() {
        val changed = at0700()
        val rewritten = Transitions.writeDirect(changed, S10, listOf(TextRun.Plain("결사의 문 앞 · 07:00")), ctx)
        assertNull(checks(rewritten).stale(S10))
        val acked = Acks.acknowledge(changed, checks(changed).stale(S10)!!, ctx)
        assertTrue(Acks.isAcknowledged(acked, checks(acked).stale(S10)!!))
        val later = acked.withTime(S10, rel(S9, 0, clock = clock(7, 30)))
        assertFalse(Acks.isAcknowledged(later, checks(later).stale(S10)!!))
    }

    @Test
    fun placeChangeMakesItStaleToo() {
        val moved = doc.withUnit(S10) { it.copy(place = PlaceRef(PlaceRefKind.PLACE, K.PLACE_ARCHIVE_ROOM)) }
        val item = checks(moved).stale(S10)!!
        assertEquals("◇ 전환 문구 · 쓴 뒤 장소 바뀜 · 1-7 씬 4 · 결사의 문 → 기록실", item.line)
    }

    @Test
    fun trn04_placeNotYetVisible() {
        val d = gatePinFrom18(Transitions.setMode(doc, S10, TransitionMode.AUTO, ctx))
        assertEquals("결사의 문 · 06:10", tr(d).shownText(S10))
        val item = checks(d).placeBeforeReveal(S10)!!
        assertEquals("◇ 공개 전 장소 · 전환 문구", item.title)
        assertEquals("1-7 씬 4 · 결사의 문", item.detail)
        assertEquals(listOf("직접 쓰기", "맞음"), item.actions)
        // A direct text is never read: no item. Visible place: no item.
        assertNull(checks(gatePinFrom18(doc)).placeBeforeReveal(S10))
        assertNull(checks(Transitions.setMode(doc, S10, TransitionMode.AUTO, ctx)).placeBeforeReveal(S10))
    }

    @Test
    fun trn05_directTextKeptWhileNotInUse() {
        val none = Transitions.setMode(doc, S10, TransitionMode.NONE, ctx)
        assertNull(tr(none).shownText(S10))
        assertEquals("전환 문구 · 없음", tr(none).headLine(S10))
        val back = Transitions.setMode(none, S10, TransitionMode.DIRECT, ctx)
        assertEquals("결사의 문 앞 · 06:10", tr(back).shownText(S10))
    }

    @Test
    fun fixtureRaisesNoTransitionItems() {
        assertTrue(checks(doc).stale().isEmpty())
        assertTrue(checks(doc).placesBeforeReveal().isEmpty())
    }

    // --- 10-script T-22-01 … 06 (rule 22 as seen in the script) ------------------------------------

    @Test
    fun t2201_headAndDirectTransition() {
        val s10 = head(doc, S10)
        assertEquals("씬 4 · 결사의 문 · 2일째 아침 06:10", s10.text)
        assertEquals("전환 문구 · 결사의 문 앞 · 06:10 · 직접", s10.transitionLine)
        assertEquals("전환 문구 · 없음", head(doc, S9).transitionLine)
    }

    @Test
    fun t2202_directTextSurvivesATimeChange() {
        val changed = at0700()
        assertEquals("씬 4 · 결사의 문 · 2일째 아침 07:00", head(changed, S10).text)
        assertEquals("전환 문구 · 결사의 문 앞 · 06:10 · 직접", head(changed, S10).transitionLine)
        val auto = Transitions.toAuto(changed, S10, ctx)
        assertEquals("전환 문구 · 결사의 문 · 07:00 · 자동", head(auto, S10).transitionLine)
    }

    @Test
    fun t2204_headPerProperties() {
        assertEquals("씬 4 · 결사의 문 · 2일째 아침 06:10", head(doc, S10).text)
        assertEquals("씬 4 · 결사의 문 · 2일째 아침", head(doc.withTime(S10, rel(S9, 0, Band.MORNING)), S10).text)
        val region = doc.withUnit(S2) { it.copy(place = lowerDistrict) }
        assertEquals("씬 1 · 하층 구역 · 1일째 새벽", head(region, S2).text)
        assertEquals("씬 2 · 장소 없음 · 때 없음", head(doc, K.SCENE_1_8_2).text)
        // F4(c) is blocked in the spec (Q-22); this engine follows rule 11 §6 (episode's first timed scene = 1일째).
        assertEquals("씬 1 · 잿더미 시장 · 1일째 낮", head(doc, S6).text)
    }

    @Test
    fun flashbackHeadFollowsSto24() {
        assertEquals("씬 2 · 하층 거리 · 회상 · 1195", head(doc, S5).text)
    }

    @Ignore("spec conflict: T-22-04(f) (rule 22 table) wants `씬 2 · 회상 · 하층 구역 · 1195`; STO-24 / RTFlashback put the place first (`씬 2 · 하층 거리 · 회상 · 1195`), SCR Q-22(b)")
    @Test
    fun t2204f_flashbackHeadRule22Order() {
        val region = doc.withUnit(S5) { it.copy(place = lowerDistrict) }
        assertEquals("씬 2 · 회상 · 하층 구역 · 1195", head(region, S5).text)
    }

    @Test
    fun deletedPlaceInTheHead() {
        val d = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_SOCIETY_GATE) it.copy(deletedAt = 1L) else it }))
        val h = head(d, S10)
        assertEquals("씬 4 · 지운 장소 · 결사의 문 · 2일째 아침 06:10", h.text)
        assertTrue(h.placeDeleted)
    }

    @Test
    fun headMenuRows() {
        val menu = SceneHeads(doc).menu(S10)!!
        assertEquals("씬 4 · 1-7", menu.title)
        assertEquals(
            listOf(
                HeadMenuRow("장소와 때", "결사의 문 · 2일째 아침 06:10"),
                HeadMenuRow("전환 문구", "직접 · 결사의 문 앞 · 06:10"),
                HeadMenuRow("앞 씬과 합치기", "씬 3 · 꺼진 등불"),
                HeadMenuRow("앞에 새 씬", "빈 씬 · 씬 4 앞"),
                HeadMenuRow("씬 지우기", "휴지통 · 30일"),
            ),
            menu.rows,
        )
        assertTrue(SceneHeads(doc).menu(K.SCENE_1_8_1)!!.rows.none { it.label == "앞 씬과 합치기" })
        assertTrue(SceneHeads(doc).menu(S10, readOnly = true)!!.rows.all { !it.enabled && it.value == "읽기 전용" })
    }

    @Test
    fun sheetRow() {
        val row = tr().row(S10)
        assertEquals(TransitionMode.DIRECT, row.mode)
        assertTrue(row.autoEnabled)
        assertEquals("결사의 문 앞 · 06:10", row.preview)
        assertFalse(tr().row(S3).autoEnabled)
    }

    @Test
    fun flashbackAutoTextHasNoWord() {
        val d = Transitions.setMode(doc, S5, TransitionMode.AUTO, ctx)
        assertEquals("하층 거리 · 1195", tr(d).shownText(S5))
    }
}
