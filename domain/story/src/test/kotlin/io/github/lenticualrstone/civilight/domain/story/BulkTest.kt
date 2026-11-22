package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S10
import io.github.lenticualrstone.civilight.domain.story.Fx.S11
import io.github.lenticualrstone.civilight.domain.story.Fx.S6
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.Fx.withUnit
import io.github.lenticualrstone.civilight.domain.story.bulk.BandChoice
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkFilter
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkList
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkPlace
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkWhen
import io.github.lenticualrstone.civilight.domain.story.bulk.SuggestionSource
import io.github.lenticualrstone.civilight.domain.story.bulk.Suggestions
import io.github.lenticualrstone.civilight.domain.story.bulk.WhenMode
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/** 장소 · 때 한꺼번에 and 제안 받기 (20-story-time §4.11). */
class BulkTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    private val s162 = K.SCENE_1_6_2
    private val s182 = K.SCENE_1_8_2
    private val s191 = K.SCENE_1_9_1
    private val selection = listOf(s162, S11, s182, s191)

    /** BLK-02's Given: 1-7 씬 4 and 1-8 씬 1 untimed. */
    private fun untimed(): ProjectDoc = doc.withTime(S10, SceneTime.None).withTime(S11, SceneTime.None)

    /** SUG-02's Given on top of [untimed]: 1-8 #001 holds the narration. */
    private fun withPylonLine(d: ProjectDoc = untimed()): ProjectDoc {
        val b001 = K.block(1, 8, 1)
        return d.copy(
            episodes = d.episodes.mapValues { (_, f) ->
                f.copy(blocks = f.blocks.map { if (it.key == b001) it.copy(runs = listOf(TextRun.Plain("이틀 뒤, 송전탑 그림자가 길게 누운 새벽."))) else it })
            },
        )
    }

    private val pylonAlias = mapOf(K.PLACE_PYLON_RUINS to listOf("송전탑"))

    // --- BLK -----------------------------------------------------------------------------------------

    @Test
    fun blk01_progressAndChips() {
        val list = BulkList(doc, K.CHAPTER_1)
        assertEquals("잿빛 새벽의 등불 · 1장", list.head)
        assertEquals("장소 | 22/31", list.placeProgress.text)
        assertEquals("때 | 19/31", list.timeProgress.text)
        assertEquals("전체 31", list.chip(BulkFilter.ALL))
        assertEquals("제안 있음 3", list.chip(BulkFilter.SUGGESTED))
        assertEquals(
            listOf(K.SCENE_1_4_1, s162, s182),
            list.filtered(BulkFilter.SUGGESTED).map { it.key },
        )
        assertEquals("장소 22/31 · 때 19/31", list.progressLine)
    }

    @Test
    fun blk01_emptyCountOfTheFixture() {
        // 12 scenes lack 때; 1-5 씬 3 and 1-10 씬 1 have 때 but no place, so either-empty is 14.
        val list = BulkList(doc, K.CHAPTER_1)
        assertEquals("장소 · 때 빈 것 14", list.chip(BulkFilter.EMPTY))
        assertEquals(12, list.scenes.count { !list.hasTime(it) })
    }

    @Ignore("spec conflict: BLK-01 `장소 · 때 빈 것 12` vs the fixture (22 placed, 19 timed of 31; 1-5 씬 3 and 1-10 씬 1 are timed without a place) = 14 scenes lacking a place or a 때")
    @Test
    fun blk01_emptyChipAsDrawn() {
        assertEquals("장소 · 때 빈 것 12", BulkList(doc, K.CHAPTER_1).chip(BulkFilter.EMPTY))
    }

    @Test
    fun bulkRowsAndFooter() {
        val list = BulkList(untimed(), K.CHAPTER_1)
        val g16 = list.groups().single { it.code == "1-6" }
        val r1 = g16.rows.first()
        assertEquals("씬 1 · 잿더미 시장", r1.label)
        assertEquals("잿더미 시장", r1.place)
        assertEquals("1일째 낮", r1.time)
        val r2 = g16.rows[1]
        assertEquals("장소 없음", r2.place)
        assertEquals("때 없음", r2.time)
        assertEquals(listOf("1-6 씬 2", "+ 장소", "+ 때", "배경"), r2.tableCells)
        assertEquals("고른 씬 4개 · 1-6 씬 2 · 1-8 씬 1 · 1-8 씬 2 · 1-9 씬 1", list.footer(selection))
        assertEquals(listOf("장소 정하기", "때 정하기", "제안 받기 2"), list.buttons(selection))
        assertEquals("고른 씬 4개 | 장소 정하기 | 때 정하기 | 제안 받기 3", BulkList(withPylonLine(), K.CHAPTER_1).wideFooter(selection))
    }

    @Test
    fun blk02_chainThroughTheSelection() {
        val d = untimed()
        val plan = BulkWhen.plan(d, selection, WhenMode.CHAIN, dayChip = 1, band = BandChoice.Keep, precision = TimePrecision.APPROX, ctx = ctx)
        assertEquals("때 정하기 · 씬 4개", plan.title)
        assertEquals(
            listOf(
                "1-6 씬 2 | 1-6 · 2일째 · 대략 | 직전 1-6 씬 1 · 1일째 낮",
                "1-8 씬 1 | 1-8 · 1일째 · 대략 | 1-7 씬 3 기준 · 직전 씬 때 없음",
                "1-8 씬 2 | 1-8 · 2일째 · 대략 | 직전 1-8 씬 1",
                "1-9 씬 1 | 1-9 · 1일째 · 대략 | 직전 1-8 씬 2",
            ),
            plan.rows.map { it.line },
        )
        assertEquals("씬 4개에 넣기", plan.button)
        // Nothing stored before the button; the result is one step (the previous doc is the undo).
        for (k in selection) assertEquals(SceneTime.None, d.unit(k).time)
        val r = plan.result
        fun rel(k: String) = r.unit(k).time as SceneTime.Relative
        assertEquals(S6, rel(s162).baseKey)
        assertEquals(S9, rel(S11).baseKey)
        assertEquals(S11, rel(s182).baseKey)
        assertEquals(s182, rel(s191).baseKey)
        for (k in selection) {
            assertEquals(0, rel(k).dayMin)
            assertEquals(2, rel(k).dayMax)
            assertEquals(TimePrecision.APPROX, rel(k).precision.known)
        }
        val changed = r.story.units.zip(d.story.units).filter { (a, b) -> a != b }.map { it.first.key }.toSet()
        assertEquals(selection.toSet(), changed)
    }

    @Test
    fun blk02_exactAndAllSame() {
        val d = untimed()
        val exact = BulkWhen.plan(d, selection, dayChip = 1, band = BandChoice.Set(Band.NIGHT), precision = TimePrecision.EXACT, ctx = ctx)
        assertEquals("1-6 씬 2 | 1-6 · 2일째 · 밤 | 직전 1-6 씬 1 · 1일째 낮", exact.rows.first().line)
        val same = BulkWhen.plan(d, selection, WhenMode.ALL_SAME, dayChip = 2, precision = TimePrecision.EXACT, ctx = ctx).result
        for (k in selection.drop(1)) assertEquals(s162, (same.unit(k).time as SceneTime.Relative).baseKey)
        val times = SceneTimes(same)
        for (k in selection.drop(1)) assertEquals(times.timed(s162)!!.day, times.timed(k)!!.day)
    }

    @Test
    fun approxRanges() {
        assertEquals(Span(0, 1), BulkWhen.approxRange(0))
        assertEquals(Span(0, 2), BulkWhen.approxRange(1))
        assertEquals(Span(1, 3), BulkWhen.approxRange(2))
    }

    @Test
    fun blk03_samePlaceForAll() {
        val scope = BulkList(doc, K.CHAPTER_1).scenes.map { it.key }
        val options = BulkPlace.options(doc, scope)
        val market = options.single { it.name == "잿더미 시장" }
        assertEquals("씬 4개", market.countText)
        // Count desc, ties in place order: 하층 거리 6, 결사의 문 6, 잿더미 시장 4 …
        assertEquals(listOf("하층 거리" to 6, "결사의 문" to 6, "잿더미 시장" to 4), options.take(3).map { it.name to it.sceneCount })
        assertEquals("장소 정하기 · 씬 4개", BulkPlace.title(selection.size))
        assertEquals("씬 4개 → 잿더미 시장", BulkPlace.footer(selection.size, market.name))
        val after = BulkPlace.applySame(doc, selection, market.ref, ctx)
        for (k in selection) assertTrue(market.ref.sameAs(after.unit(k).place))
        val changed = after.story.units.zip(doc.story.units).filter { (a, b) -> a != b }.map { it.first.key }.toSet()
        assertEquals(selection.toSet(), changed)
    }

    // --- SUG -----------------------------------------------------------------------------------------

    @Test
    fun sug01_backgroundCue() {
        val card = Suggestions(doc).card(s162)!!
        assertEquals("1-6 씬 2 · 노아의 심부름 | 배경 연출", card.header)
        val s = card.suggestion!!
        assertEquals(SuggestionSource.BACKGROUND, s.source)
        assertEquals("▸ 배경 잿더미 시장(밤)", s.evidence.text)
        assertEquals(listOf("잿더미 시장", "밤"), s.evidence.boxed)
        assertEquals("잿더미 시장 | 같은 날 · 밤", s.line)
        assertEquals(listOf("넣기", "고치기", "건너뛰기"), card.actions)
    }

    @Ignore("spec conflict: SUG-02 needs place 송전탑 폐허 to have the alias 송전탑, but Place has no aliases in 00-domain-model (only Term); see Requests")
    @Test
    fun sug02_bodySentenceAsDrawn() {
        val s = Suggestions(withPylonLine()).suggestionFor(S11)!!
        assertEquals(listOf("이틀 뒤", "송전탑", "새벽"), s.evidence.boxed)
        assertEquals("송전탑 폐허 | +2일 · 새벽", s.line)
    }

    @Test
    fun sug02_bodySentenceWithAnAlias() {
        val s = Suggestions(withPylonLine(), pylonAlias).suggestionFor(S11)!!
        assertEquals(SuggestionSource.BODY, s.source)
        assertEquals("이틀 뒤, 송전탑 그림자가 길게 누운 새벽.", s.evidence.text)
        assertEquals(listOf("이틀 뒤", "송전탑", "새벽"), s.evidence.boxed)
        assertEquals("송전탑 폐허 | +2일 · 새벽", s.line)
        // Without the alias only the time is read.
        assertEquals("장소 없음 | +2일 · 새벽", Suggestions(withPylonLine()).suggestionFor(S11)!!.line)
    }

    @Test
    fun sug03_directTransitionTextWithANewPlace() {
        val noArchive = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_ARCHIVE_ROOM) it.copy(deletedAt = 1L) else it }))
        val s = Suggestions(noArchive).suggestionFor(s182)!!
        assertEquals(SuggestionSource.TRANSITION, s.source)
        assertEquals("기록실 · 같은 날 밤", s.evidence.text)
        assertEquals(listOf("기록실", "같은 날", "밤"), s.evidence.boxed)
        assertEquals("기록실 | 같은 날 · 밤 | · 장소 카드 없음 → 새 장소", s.line)
        // With the fixture's 기록실 card the place is matched.
        assertEquals("기록실 | 같은 날 · 밤", Suggestions(doc).suggestionFor(s182)!!.line)

        val after = Suggestions.apply(noArchive, s, ctx)
        val place = after.unit(s182).place!!
        val card = after.world.places.single { it.key == place.key }
        assertEquals("기록실", card.name)
        assertTrue(after.maps.values.none { m -> m.pins.any { it.placeKey == card.key } })
        // The direct transition text is left unchanged.
        assertEquals(doc.unit(s182).transitionRuns, after.unit(s182).transitionRuns)
    }

    @Test
    fun sug04_noResultAndFooter() {
        val sheet = Suggestions(withPylonLine()).sheet(selection)
        assertEquals("장소 · 때 한꺼번에 · 고른 씬 4개", sheet.head)
        assertEquals("원고의 배경 연출 · 전환 문구 · 본문에서 읽음 · 넣기 전엔 바뀌지 않음", sheet.line)
        val none = sheet.cards.single { it.sceneKey == s191 }
        assertEquals("1-9 씬 1 · 미정 | 제안 없음", none.header)
        assertEquals("배경 · 전환 문구 · 본문에서 찾지 못함 → 직접", none.emptyLine)
        assertEquals("제안 3개 모두 넣기", sheet.applyAllLabel)
    }

    @Test
    fun sug05_nothingChangesBeforeInsertAndAutoTextIsNoSource() {
        val d = withPylonLine()
        val sheet = Suggestions(d).sheet(selection)
        assertEquals(d, withPylonLine())
        // A scene whose stored runs are not in 직접 mode is not read.
        val auto = d.withUnit(s182) { it.copy(transitionMode = TransitionMode.AUTO.open()) }
        assertNull(Suggestions(auto).suggestionFor(s182))

        val all = Suggestions.applyAll(d, sheet.cards.mapNotNull { it.suggestion }, ctx)
        val market = all.unit(s162)
        assertTrue(PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET).sameAs(market.place))
        val t = market.time as SceneTime.Relative
        assertEquals(S6, t.baseKey)
        assertEquals(0, t.dayMin)
        assertEquals(1, t.dayMax)
        assertEquals(Band.NIGHT, t.band?.known)
        val pylon = all.unit(S11).time as SceneTime.Relative
        assertEquals(1, pylon.dayMin)
        assertEquals(3, pylon.dayMax)
        assertEquals(Band.DAWN, pylon.band?.known)
        assertEquals(SceneTime.None, all.unit(s191).time)
        assertFalse(Suggestions(all).hasSuggestion(s162))
    }
}
