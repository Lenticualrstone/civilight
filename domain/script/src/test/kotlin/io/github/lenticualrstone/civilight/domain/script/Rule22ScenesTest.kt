package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.domain.story.travel.TravelCheck
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.TransitionTemplate
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 10-script.md §4.4 · rule 22 (scene head, transition text, split, merge, export), on the F1 overlay. */
class Rule22ScenesTest {
    private val ctx = testCtx()
    private fun b(n: Int) = F1.b(n)

    private fun head(doc: ProjectDoc, scene: Key): String = SceneText.headText(doc, scene, StoryTimes.source(doc).timeOf(scene))
    private fun headTime(doc: ProjectDoc, scene: Key): String = SceneText.headTime(StoryTimes.source(doc).timeOf(scene))
    private fun line(doc: ProjectDoc, scene: Key): String =
        SceneText.transitionLine(doc, doc.unit(scene), StoryTimes.source(doc).timeOf(scene))
    private fun auto(doc: ProjectDoc, scene: Key): String? =
        SceneText.autoTransitionText(doc, doc.unit(scene), StoryTimes.source(doc).timeOf(scene))
    private fun relative(doc: ProjectDoc, scene: Key) = doc.unit(scene).time as SceneTime.Relative

    // --- F4 scene-head variants ---

    private val region = PlaceRef(PlaceRefKind.REGION, K.REGION_LOWER_DISTRICT)

    private fun f4a() = F1.build()
    private fun f4b() = F1.build().updateUnit(F1.S4) { it.copy(time = (it.time as SceneTime.Relative).copy(band = Band.MORNING.open(), clock = null)) }
    private fun f4c() = F1.build().updateUnit(K.SCENE_1_6_1) { it.copy(time = (it.time as SceneTime.Relative).copy(band = null, clock = null)) }
    private fun f4d() = F1.build().updateUnit(K.SCENE_1_2_1) { it.copy(place = region) }
    private fun f4f() = F1.build().updateUnit(K.SCENE_1_4_2) { it.copy(place = region) }

    @Test
    fun `T-22-01 head and direct transition`() {
        val doc = F1.build()
        assertEquals("씬 4 · 결사의 문 · 2일째 아침 06:10", head(doc, F1.S4))
        assertEquals("전환 문구 · 결사의 문 앞 · 06:10 · 직접", line(doc, F1.S4))
        assertEquals("전환 문구 · 없음", line(doc, F1.S3))
    }

    @Test
    fun `T-22-02 direct text survives a time change and raises the stale notice`() {
        val doc = F1.build().updateUnit(F1.S4) { it.copy(time = (it.time as SceneTime.Relative).copy(clock = ClockTime.of(7, 0))) }
        assertEquals("씬 4 · 결사의 문 · 2일째 아침 07:00", head(doc, F1.S4))
        assertEquals("결사의 문 앞 · 06:10", SceneText.transitionText(doc, doc.unit(F1.S4), StoryTimes.source(doc).timeOf(F1.S4)))
        val item = ScriptChecks.transitionStale(doc, StoryTimes.source(doc)).single()
        assertEquals("◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00", item.title)
        assertEquals(listOf("자동으로", "문구 고치기", "맞음"), item.buttons)

        val auto = SceneText.setTransitionMode(doc, F1.S4, TransitionMode.AUTO, ctx)!!
        assertEquals(TransitionMode.AUTO, auto.unit(F1.S4).transitionModeOrNone)
        assertEquals("전환 문구 · 결사의 문 · 07:00 · 자동", line(auto, F1.S4))
        assertEquals("결사의 문 앞 · 06:10", auto.unit(F1.S4).transitionRuns!!.plainText)
        assertTrue(ScriptChecks.transitionStale(auto, StoryTimes.source(auto)).isEmpty())
    }

    @Test
    fun `T-22-03 automatic text per properties`() {
        assertEquals("결사의 문 · 06:10", auto(f4a(), F1.S4))
        assertEquals("결사의 문 · 아침", auto(f4b(), F1.S4))
        assertEquals("잿더미 시장", auto(f4c(), K.SCENE_1_6_1))
        assertEquals("하층 구역 · 새벽", auto(f4d(), K.SCENE_1_2_1))
        val e = F1.build()
        assertNull(auto(e, K.SCENE_1_6_2))
        assertFalse(SceneText.canUseAuto(e.unit(K.SCENE_1_6_2)))
        assertNull(SceneText.setTransitionMode(e, K.SCENE_1_6_2, TransitionMode.AUTO, ctx))
        assertEquals("하층 구역 · 1195", auto(f4f(), K.SCENE_1_4_2))

        val placeOnly = f4a().let { it.copy(project = it.project.copy(script = it.project.script.copy(transitionTemplate = TransitionTemplate.PLACE_ONLY.open()))) }
        assertEquals("결사의 문", auto(placeOnly, F1.S4))
    }

    @Test
    fun `T-22-04 head per properties`() {
        assertEquals("씬 4 · 결사의 문 · 2일째 아침 06:10", head(f4a(), F1.S4))
        assertEquals("씬 4 · 결사의 문 · 2일째 아침", head(f4b(), F1.S4))
        assertEquals("씬 1 · 하층 구역 · 1일째 새벽", head(f4d(), K.SCENE_1_2_1))
        assertEquals("씬 2 · 장소 없음 · 때 없음", head(F1.build(), K.SCENE_1_6_2))
        assertEquals("씬 2 · 회상 · 하층 구역 · 1195", head(f4f(), K.SCENE_1_4_2))
    }

    @Ignore("spec conflict (Q-22, blocked): rule 22 gives `씬 1 · 잿더미 시장 · 약 7일째` for F4(c); rule 11 §6 makes an episode's first timed scene `1일째` even when approximate. Implemented: rule 11.")
    @Test
    fun `T-22-04 F4(c) approximate first scene as rule 22 draws it`() {
        assertEquals("씬 1 · 잿더미 시장 · 약 7일째", head(f4c(), K.SCENE_1_6_1))
    }

    @Test
    fun `T-22-04 F4(c) approximate first scene follows rule 11 (implemented default)`() {
        assertEquals("씬 1 · 잿더미 시장 · 1일째", head(f4c(), K.SCENE_1_6_1))
    }

    @Test
    fun `T-22-05 direct text is kept while not in use`() {
        val none = SceneText.setTransitionMode(F1.build(), F1.S4, TransitionMode.NONE, ctx)!!
        assertEquals("전환 문구 · 없음", line(none, F1.S4))
        val back = SceneText.setTransitionMode(none, F1.S4, TransitionMode.DIRECT, ctx)!!
        assertEquals("결사의 문 앞 · 06:10", SceneText.transitionText(back, back.unit(F1.S4), StoryTimes.source(back).timeOf(F1.S4)))
    }

    @Test
    fun `T-22-06 pre-reveal place`() {
        val base = SceneText.setTransitionMode(F1.build(), F1.S4, TransitionMode.AUTO, ctx)!!
        val map = base.maps.getValue(K.MAP_LOWER_DISTRICT)
        val doc = base.copy(
            maps = base.maps + (map.key to map.copy(pins = map.pins.map { if (it.placeKey == K.PLACE_SOCIETY_GATE) it.copy(revealPoint = PointRef.unitStart(F1.E18)) else it })),
        )
        assertEquals("결사의 문 · 06:10", auto(doc, F1.S4))
        val item = ScriptChecks.transitionPlaceBeforeReveal(doc).single { F1.S4 in it.subjectKeys }
        assertEquals("◇ 공개 전 장소 · 전환 문구", item.title)
        assertEquals(listOf("직접 쓰기", "맞음"), item.buttons)
        assertTrue(ScriptChecks.transitionPlaceBeforeReveal(base).isEmpty())

        val direct = SceneText.directFromAuto(doc, F1.S4, StoryTimes.source(doc).timeOf(F1.S4), ctx)
        assertEquals("전환 문구 · 결사의 문 · 06:10 · 직접", line(direct, F1.S4))
    }

    @Test
    fun `T-22-07 backspace never crosses a head`() {
        val doc = F1.build()
        val r = BlockOps.backspace(doc, b(49), atStart = true, ctx)
        assertTrue(r is BlockOps.BackspaceResult.Nothing)
        assertSame(doc, r.doc)
    }

    @Test
    fun `T-22-08 slash scene routing`() {
        val doc = F1.build()
        val split = SceneCreate.route(doc, F1.S3, currentBlockKey = b(44))
        assertEquals(SceneCreate.Route.SplitBelow(b(44)), split)
        assertEquals("1-7 씬 3 · #044 아래", SceneSplit.preview(doc, b(44), StoryTimes.source(doc))!!.sub)
        assertEquals(SceneCreate.Route.NextScene(F1.S3), SceneCreate.route(doc, F1.S3, currentBlockKey = b(48)))
        assertEquals(
            SceneCreate.Route.SplitBelow(b(44)),
            SceneCreate.route(doc, F1.S3, currentBlockKey = null, checkedKeys = listOf(b(42), b(43), b(44))),
        )
        assertEquals(SceneCreate.Route.ReadOnly, SceneCreate.route(doc, F1.S3, b(44), readOnly = true))
        assertEquals("읽기 전용", SceneCreate.Route.ReadOnly.LABEL)
        assertEquals(SceneCreate.Route.NewSceneBefore(F1.S3), SceneCreate.route(doc, F1.S3, null, onHead = true))
        assertEquals(F1.build(), doc)
    }

    @Test
    fun `T-22-09 split`() {
        val doc = F1.build()
        val p = SceneSplit.preview(doc, b(44), StoryTimes.source(doc))!!
        assertEquals("씬 나누기", p.title)
        assertEquals(SceneSplit.PartCard("씬 3", "블록 9개", "‘세라 · 불은 꺼졌어…’ ~ ‘…한 번 깜박였다.’"), p.partA)
        assertEquals(SceneSplit.PartCard("새 씬 4", "블록 4개", "‘리엔 · 하르트 씨, 방금…’ ~ ‘…결사의 신호였어요.’"), p.partB)
        assertEquals("장소 · 결사의 문 · 씬 3과 같음", p.placeRow)
        assertEquals("때 · 씬 3 기준 · 같은 때", p.timeRow)
        assertEquals("제목 · 미정", p.titleRow)
        val silent = p.silentRows.single()
        assertEquals("말 없는 등장 · 마르타", silent.label)
        assertEquals(listOf("씬 3", "새 씬 4", "둘 다"), silent.chips)
        assertEquals(SceneSplit.Side.BOTH, silent.selected)
        val expected = listOf(
            "순번 · 씬 4 · 미정 → 씬 5",
            "순번 · 씬 5 · 미정 → 씬 6",
            "첫 공개 · 오리지늄 등불 · 씬 3 #044 그대로",
            "◆ 관계 · 리엔 → 하르트 · #043 그대로",
            "씬 5 때 · 기준 씬 3 · 2일째 아침 06:10 그대로",
        )
        // T-22-09 lists the reveal before the ◆ row and T-22-10 the other way round: order is not asserted.
        assertTrue(p.changes.toString(), p.changes.containsAll(expected))
        assertEquals("나누기", p.primary)

        val r = SceneSplit.apply(doc, b(44), ctx)!!
        val out = r.doc
        val newScene = r.newSceneKey
        assertEquals((36..44).toList(), out.liveNumbers(F1.S3))
        assertEquals((45..48).toList(), out.liveNumbers(newScene))
        assertEquals(4, Labels.sceneOrdinal(out, newScene))
        val bUnit = out.unit(newScene)
        assertEquals(K.PLACE_SOCIETY_GATE, bUnit.place?.key)
        val bTime = bUnit.time as SceneTime.Relative
        assertEquals(F1.S3, bTime.baseKey)
        assertEquals(0 to 0, bTime.dayMin to bTime.dayMax)
        assertEquals("2일째 새벽 05:40", headTime(out, newScene))
        assertEquals(TransitionMode.NONE, bUnit.transitionModeOrNone)
        assertNull(bUnit.title)
        assertEquals(listOf(K.MARTHA), bUnit.silentCast)
        assertEquals(5, Labels.sceneOrdinal(out, F1.S4))
        assertEquals(F1.S3, relative(out, F1.S4).baseKey)
        assertEquals("2일째 아침 06:10", headTime(out, F1.S4))
        assertEquals(6, Labels.sceneOrdinal(out, F1.S5))
        for (n in 36..48) {
            val before = doc.block(b(n))
            val after = out.block(b(n))
            assertEquals(before.number, after.number)
            assertEquals(VoiceIds.currentId(doc, before), VoiceIds.currentId(out, after))
        }
        assertEquals(doc.changes, out.changes)
        assertEquals(F1.build(), doc)
    }

    @Test
    fun `T-22-10 split above records`() {
        val doc = F1.build()
        val p = SceneSplit.preview(doc, b(42), StoryTimes.source(doc))!!
        assertTrue(p.changes.contains("◆ 관계 · 1-7 씬 3 #043 → 1-7 씬 4 #043"))
        assertTrue(p.changes.contains("첫 공개 · 1-7 씬 3 #044 → 1-7 씬 4 #044"))
    }

    @Test
    fun `T-22-11 split inheritance`() {
        val base = F1.build()
        // A has no time → B has none.
        val untimed = SceneSplit.apply(base, K.block(1, 3, 1), ctx)!!
        assertEquals(SceneTime.None, untimed.doc.unit(untimed.newSceneKey).time)
        // A is a fixed point → B is `A 기준 · 같은 때` and A stays the only fixed point.
        val fixed = SceneSplit.apply(base, K.block(1, 1, 2), ctx)!!
        val fb = fixed.doc.unit(fixed.newSceneKey).time as SceneTime.Relative
        assertEquals(K.SCENE_1_1_1, fb.baseKey)
        assertEquals(0 to 0, fb.dayMin to fb.dayMax)
        assertTrue(fixed.doc.unit(K.SCENE_1_1_1).time is SceneTime.Direct)
        assertEquals(1, fixed.doc.units.count { it.time is SceneTime.Direct && it.parentKey == K.EPISODE_1_1 })
        // A is a flashback → the flashback row is on; A is hidden → the hidden row is on.
        val flash = SceneSplit.preview(base, K.BLOCK_B41, StoryTimes.source(base))!!
        assertEquals("회상", flash.flashbackRow)
        val hiddenDoc = base.updateUnit(F1.S3) { it.copy(hidden = true) }
        assertEquals("숨김", SceneSplit.preview(hiddenDoc, b(44), StoryTimes.source(hiddenDoc))!!.hiddenRow)
        val hiddenSplit = SceneSplit.apply(hiddenDoc, b(44), ctx)!!
        assertTrue(hiddenSplit.doc.unit(hiddenSplit.newSceneKey).hidden)
        // A is the first timed scene of its episode → A stays day 1.
        val first = SceneSplit.apply(base, b(5), ctx)!!
        assertEquals(1, StoryTimes.source(first.doc).timeOf(F1.S1)!!.episodeDay)
        assertEquals(1, StoryTimes.source(first.doc).timeOf(first.newSceneKey)!!.episodeDay)
    }

    @Test
    fun `T-22-12 next scene prefill`() {
        val doc = F1.build()
        assertEquals(SceneCreate.Route.NextScene(F1.S3), SceneCreate.route(doc, F1.S3, b(48)))
        val prefill = SceneCreate.nextScenePrefill(doc, F1.S3)!!
        assertEquals("다음 씬 만들기", prefill.title)
        assertEquals("1-7 · 씬 3 뒤", prefill.sub)
        assertEquals("장소 · 결사의 문 · 씬 3과 같음", prefill.placeRow)
        assertEquals("때 · 씬 3 기준 · 같은 때", prefill.timeRow)
        assertEquals(listOf("비우기", "바꾸기 ›"), prefill.rowButtons)

        val created = SceneCreate.createAfter(doc, F1.S3, SceneCreate.NewScene(place = null, sameTimeAs = null), ctx)!!
        val scene = created.doc.unit(created.sceneKey)
        assertNull(scene.place)
        assertEquals(SceneTime.None, scene.time)
        assertEquals(4, Labels.sceneOrdinal(created.doc, created.sceneKey))
        val before = StoryTimes.source(doc)
        val after = StoryTimes.source(created.doc)
        assertEquals(before.timeOf(F1.S4), after.timeOf(F1.S4))
        assertEquals(before.timeOf(F1.S5), after.timeOf(F1.S5))
    }

    private fun mergePreview(doc: ProjectDoc) = SceneMerge.preview(doc, F1.S4, StoryTimes.source(doc))!!

    private fun merge(doc: ProjectDoc, choices: SceneMerge.Choices = SceneMerge.Choices()) =
        SceneMerge.apply(doc, F1.S4, choices, StoryTimes.source(doc), ctx)!!

    @Test
    fun `T-22-13 merge with default choices`() {
        val doc = F1.build()
        val p = mergePreview(doc)
        assertEquals("씬 합치기", p.title)
        assertEquals("1-7 씬 3 + 씬 4 → 씬 3", p.sub)
        assertEquals("장소 · 결사의 문 · 씬 3 · 씬 4 같음", p.placeRow)
        assertNull(p.placeChoice)
        assertEquals(SceneMerge.Choice("때 · 하나 고르기", "씬 3 것 · 2일째 새벽 05:40", "씬 4 것 · 2일째 아침 06:10"), p.timeChoice)
        assertEquals("고르지 않은 때 → 씬 메모 한 줄", p.timeNote)
        assertEquals("전환 문구 · 씬 4 직접" to "‘결사의 문 앞 · 06:10’", p.transitionRow)
        assertEquals("시스템 텍스트로 남기기", p.transitionChoices.first())
        val expected = listOf(
            "씬 5 때 · 기준 씬 4 → 씬 3 · 간격 다시 적음 · 절대 때 그대로",
            "제목 · 꺼진 등불 · 씬 4 제목 없음",
            "기록 · 블록에 그대로 · 위치 이름만 씬 4 → 씬 3",
        )
        assertEquals(expected, p.changes.filter { it in expected })
        // The example also bases 1-8 씬 1 on 1-7 씬 4 (Fixture T S11), which F1 does not state.
        assertEquals(expected.size + 1, p.changes.size)
        assertTrue(p.changes.contains("1-8 씬 1 때 · 기준 씬 4 → 씬 3 · 간격 다시 적음 · 절대 때 그대로"))
        assertEquals("합치기", p.primary)

        val r = merge(doc)
        val out = r.doc
        assertEquals((36..48).toList() + 58 + (49..52).toList(), out.liveNumbers(F1.S3))
        val system = out.block(r.systemBlockKey!!)
        assertEquals(BlockKind.SYSTEM, system.kind.known)
        assertEquals("결사의 문 앞 · 06:10", system.runs.plainText)
        assertTrue(out.unit(F1.S3).memo!!.lines().contains("합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10"))
        assertEquals(4, Labels.sceneOrdinal(out, F1.S5))
        assertEquals(F1.S3, relative(out, F1.S5).baseKey)
        assertEquals("2일째 아침 08:00", headTime(out, F1.S5))
        assertNull(out.index.unit(F1.S4))
        assertEquals(F1.S3, relative(out, F1.S18_1).baseKey)
        assertEquals(StoryTimes.source(doc).timeOf(F1.S18_1), StoryTimes.source(out).timeOf(F1.S18_1))
        assertEquals(F1.build(), doc)
    }

    @Test
    fun `T-22-14 merge choosing the later scene's time`() {
        val out = merge(F1.build(), SceneMerge.Choices(timeFromB = true)).doc
        assertEquals("2일째 아침 06:10", headTime(out, F1.S3))
        assertEquals("2일째 아침 08:00", headTime(out, F1.S5))
        assertTrue(out.unit(F1.S3).memo!!.lines().contains("합치기 전 · 씬 3 · 결사의 문 · 2일째 새벽 05:40"))
    }

    @Test
    fun `T-22-15 merge with one side empty`() {
        val doc = F1.build().updateUnit(F1.S3) { it.copy(place = null) }
        val p = mergePreview(doc)
        assertNull(p.placeChoice)
        assertEquals("장소 · 결사의 문", p.placeRow)
        assertEquals(K.PLACE_SOCIETY_GATE, merge(doc).doc.unit(F1.S3).place?.key)
    }

    @Test
    fun `T-22-16 merge blocked`() {
        val flash = F1.build().updateUnit(F1.S4) { it.copy(time = SceneTime.Flashback(mode = FlashbackMode.DIRECT.open(), year = 1195)) }
        assertEquals(SceneMerge.Check.Blocked("회상이 섞임 · 먼저 회상 끄기"), SceneMerge.check(flash, F1.S4))
        assertNull(SceneMerge.preview(flash, F1.S4, StoryTimes.source(flash)))
        val hidden = F1.build().updateUnit(F1.S4) { it.copy(hidden = true) }
        assertEquals(SceneMerge.Check.Blocked("숨긴 씬이 섞임 · 먼저 숨김 풀기"), SceneMerge.check(hidden, F1.S4))
        assertEquals(SceneMerge.Check.NotOffered, SceneMerge.check(F1.build(), F1.S18_1))
    }

    @Test
    fun `T-22-17 merge discarding the transition text`() {
        val r = merge(F1.build(), SceneMerge.Choices(keepTransitionAsSystemText = false))
        assertNull(r.systemBlockKey)
        assertEquals((36..52).toList(), r.doc.liveNumbers(F1.S3))
        assertEquals(58, r.doc.unit(F1.E17).nextBlockNumber)
    }

    @Test
    fun `T-22-18 scene delete keeps other times`() {
        val doc = F1.build()
        val deleted = SceneLifecycle.delete(doc, F1.S4, ctx)!!.doc
        assertEquals(F1.S3, relative(deleted, F1.S5).baseKey)
        assertEquals("2일째 아침 08:00", headTime(deleted, F1.S5))
        val restored = SceneLifecycle.restore(deleted, F1.S4, ctx)
        assertEquals("2일째 아침 06:10", headTime(restored, F1.S4))
        assertEquals("2일째 아침 08:00", headTime(restored, F1.S5))
        assertEquals(F1.S4, relative(restored, F1.S5).baseKey)
        assertEquals(StoryTimes.source(doc).timeOf(F1.S5), StoryTimes.source(restored).timeOf(F1.S5))
    }

    @Test
    fun `T-22-19 text that looks like a head`() {
        val doc = F1.build()
        val r = BlockMove.pastePlainText(doc, "— 씬 2 · 기록실 —", BlockPosition.After(b(48)), ctx)
        val pasted = r.doc.block(r.blockKeys.single())
        assertEquals(BlockKind.NARRATION, pasted.kind.known)
        assertEquals(58, pasted.number)
        assertEquals("— 씬 2 · 기록실 —", pasted.runs.plainText)
        assertEquals(BlockMove.sceneCount(doc), BlockMove.sceneCount(r.doc))
        assertEquals(doc.index.scenesOfNumberingUnit(F1.E17).size, r.doc.index.scenesOfNumberingUnit(F1.E17).size)
    }

    @Test
    fun `T-22-20 in-scene time skip`() {
        val doc = F1.build()
        val ins = BlockOps.insert(doc, BlockPosition.After(b(44)), BlockKind.SYSTEM, ctx, runs = listOf(TextRun.Plain("30분 뒤")))
        assertEquals("2일째 새벽 05:40", headTime(ins.doc, F1.S3))
        assertEquals(StoryTimes.source(doc).timeOf(F1.S3), StoryTimes.source(ins.doc).timeOf(F1.S3))
        assertEquals(TravelCheck(doc).warnings.map { it.fingerprint }, TravelCheck(ins.doc).warnings.map { it.fingerprint })
    }

    @Test
    fun `T-22-21 JSON scene value`() {
        val doc = F1.build()
        val json = GameJsonExport.build(doc, ExportScope.json(listOf(F1.E17)), StoryTimes.source(doc))
        val s4 = json["scenes"]!!.jsonArray.map { it.jsonObject }.single { it["id"]!!.jsonPrimitive.content == F1.S4 }
        assertEquals(4, s4["index"]!!.jsonPrimitive.int)
        assertEquals(JsonNull, s4["title"])
        val place = s4["place"]!!.jsonObject
        assertEquals(K.PLACE_SOCIETY_GATE, place["id"]!!.jsonPrimitive.content)
        assertEquals("결사의 문", place["name"]!!.jsonPrimitive.content)
        assertEquals("place", place["kind"]!!.jsonPrimitive.content)
        val time = s4["time"]!!.jsonObject
        assertEquals(11, time["storyDay"]!!.jsonObject["min"]!!.jsonPrimitive.int)
        assertEquals(15, time["storyDay"]!!.jsonObject["max"]!!.jsonPrimitive.int)
        assertEquals(2, time["episodeDay"]!!.jsonPrimitive.int)
        assertEquals("아침", time["band"]!!.jsonPrimitive.content)
        assertEquals("06:10", time["clock"]!!.jsonPrimitive.content)
        assertEquals("exact", time["precision"]!!.jsonPrimitive.content)
        assertFalse(s4["flashback"]!!.jsonPrimitive.boolean)
        val transition = s4["transition"]!!.jsonObject
        assertEquals("direct", transition["mode"]!!.jsonPrimitive.content)
        assertEquals("결사의 문 앞 · 06:10", transition["text"]!!.jsonPrimitive.content)
        assertEquals(SceneText.transitionId(F1.S4), transition["id"]!!.jsonPrimitive.content)
        assertTrue(transition["id"]!!.jsonPrimitive.content.contains(F1.S4))
        val blocks = s4["blocks"]!!.jsonArray.map { it.jsonObject }
        assertEquals((49..52).map(::b), blocks.map { it["id"]!!.jsonPrimitive.content })
        assertTrue(blocks.none { (it["type"]?.jsonPrimitive?.content ?: "").contains("transition") })
    }

    @Test
    fun `T-22-22 CSV transition row`() {
        val doc = F1.build()
        val rows = DialogueCsvExport.rows(doc, ExportScope.csv(listOf(F1.E17)), StoryTimes.source(doc))
        val transitions = rows.filter { it.speaker == "전환 문구" }
        assertEquals(
            listOf(DialogueCsvExport.Row("전환 문구", "결사의 문 앞 · 06:10", "", SceneText.transitionId(F1.S4), "새")),
            transitions,
        )
    }

    @Test
    fun `T-22-23 the head ignores layers`() {
        val doc = F1.build()
        val times = StoryTimes.source(doc)
        fun s4Head(layers: ScriptLayers) =
            Manuscript.rows(doc, F1.E17, times, layers).filterIsInstance<Manuscript.Row.Head>().single { it.sceneKey == F1.S4 }
        val off = s4Head(ScriptLayers(system = false))
        assertEquals("씬 4 · 결사의 문 · 2일째 아침 06:10", off.text)
        assertNull(off.transitionLine)
        assertEquals("전환 문구 · 결사의 문 앞 · 06:10 · 직접", s4Head(ScriptLayers()).transitionLine)
    }

    private fun fixedPointF1() = F1.build().updateUnit(F1.S4) {
        it.copy(time = SceneTime.Direct(storyDay = 13, band = Band.MORNING.open(), clock = ClockTime.of(6, 10)))
    }

    @Ignore(
        "spec conflict: T-22-24 wants S5 at exactly `2일째 아침 08:00` after re-basing on S3, but S3's day is a range " +
            "(11~15, from 1-6 씬 1's 대략 link) and SCR-51 says the new interval is `a range when approximate`, so S5 resolves " +
            "to `약 2일째`. The rest of the case is tested in the next test.",
    )
    @Test
    fun `T-22-24 merge where B is a fixed point`() {
        val out = merge(fixedPointF1()).doc
        assertEquals("2일째 아침 08:00", headTime(out, F1.S5))
    }

    @Test
    fun `T-22-24 merge where B is a fixed point (note, base and fixed point)`() {
        val doc = fixedPointF1()
        assertEquals("씬 4 고정점 사라짐 · 씬 4 것을 고르면 직접 유지", mergePreview(doc).fixedPointNote)
        val keepA = merge(doc).doc
        val s3 = relative(keepA, F1.S3)
        assertEquals(F1.S2, s3.baseKey)
        assertEquals(1 to 1, s3.dayMin to s3.dayMax)
        assertEquals(ClockTime.of(5, 40), s3.clock)
        assertEquals(F1.S3, relative(keepA, F1.S5).baseKey)
        val s5 = StoryTimes.source(keepA).timeOf(F1.S5)!!
        assertEquals(ClockTime.of(8, 0), s5.clock)
        val s5Days = s5.storyDay!!
        assertTrue(13 in s5Days.first..s5Days.last)
        assertEquals("약 2일째 아침", headTime(keepA, F1.S5))
        assertTrue(keepA.units.none { it.parentKey == F1.E17 && it.time is SceneTime.Direct })

        val takeB = merge(doc, SceneMerge.Choices(timeFromB = true)).doc
        assertEquals(SceneTime.Direct(storyDay = 13, band = Band.MORNING.open(), clock = ClockTime.of(6, 10)), takeB.unit(F1.S3).time)
        assertTrue(headTime(takeB, F1.S5).endsWith("2일째 아침 08:00"))
    }

    @Test
    fun `T-22-25 merge where B has a title`() {
        val doc = F1.build().updateUnit(F1.S4) { it.copy(title = "문 앞") }
        assertTrue(mergePreview(doc).changes.contains("제목 · 꺼진 등불 · 씬 4 제목 문 앞 → 씬 메모"))
        val out = merge(doc).doc
        assertEquals("꺼진 등불", out.unit(F1.S3).title)
        assertEquals(
            listOf("문 앞", "등불이 두 번째로 깜박이는 타이밍", "합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10"),
            out.unit(F1.S3).memo!!.lines(),
        )
    }

    @Test
    fun `T-22-26 merge with time on one side only`() {
        val doc = F1.build()
            .updateUnit(F1.S3) { it.copy(time = SceneTime.None) }
            .updateUnit(F1.S4) { it.copy(time = SceneTime.Relative(baseKey = F1.S2, dayMin = 1, dayMax = 1, clock = ClockTime.of(6, 10))) }
        val p = mergePreview(doc)
        assertNull(p.timeChoice)
        assertNull(p.timeNote)
        val out = merge(doc).doc
        assertEquals("2일째 아침 06:10", headTime(out, F1.S3))
        assertEquals(F1.S3, relative(out, F1.S5).baseKey)
        assertEquals("2일째 아침 08:00", headTime(out, F1.S5))
    }

    @Test
    fun `merge moves every reference to the removed scene onto the merged one`() {
        val base = F1.build()
        val map = base.maps.getValue(K.MAP_LOWER_DISTRICT)
        val pointRecord = record("01JZ000000CHGR000000000094", io.github.lenticualrstone.civilight.model.change.Anchor.UnitStart(F1.S4))
        val ack = io.github.lenticualrstone.civilight.model.checks.Acknowledgement(
            key = "01JZ000000ACKS000000000098", checkType = io.github.lenticualrstone.civilight.model.checks.CheckType.TRANSITION_STALE,
            subjectKeys = listOf(F1.S4), fingerprint = "x", kind = io.github.lenticualrstone.civilight.model.checks.AckKind.MATCH.open(),
        )
        val override = { k: String, from: Key, to: Key ->
            io.github.lenticualrstone.civilight.model.story.TravelSegmentOverride(k, K.HART, from, to, K.TRAVEL_HORSE)
        }
        val doc = base.withRecords(pointRecord).copy(
            maps = base.maps + (map.key to map.copy(pins = map.pins.map { if (it.placeKey == K.PLACE_SOCIETY_GATE) it.copy(revealPoint = PointRef.unitStart(F1.S4)) else it })),
            world = base.world.copy(events = base.world.events.map { if (it.key == K.EVENT_JOINS_SOCIETY) it.copy(sceneKey = F1.S4) else it }),
            app = base.app.copy(view = base.app.view.copy(lens = io.github.lenticualrstone.civilight.model.app.LensState(io.github.lenticualrstone.civilight.model.position.LensPoint.UnitEnd(F1.S4)))),
            checks = base.checks.copy(
                acknowledgements = base.checks.acknowledgements + ack,
                travelOverrides = listOf(override("01JZ000000TOVR000000000001", F1.S3, F1.S4), override("01JZ000000TOVR000000000002", F1.S2, F1.S4)),
            ),
        )
        val out = merge(doc).doc
        assertEquals(io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock(b(49)), out.record(pointRecord.key).anchor)
        val pin = out.maps.getValue(K.MAP_LOWER_DISTRICT).pins.single { it.placeKey == K.PLACE_SOCIETY_GATE }
        assertEquals(PointRef.unitStart(F1.S3), pin.revealPoint)
        assertEquals(F1.S3, out.index.event(K.EVENT_JOINS_SOCIETY)!!.sceneKey)
        assertEquals(io.github.lenticualrstone.civilight.model.position.LensPoint.UnitEnd(F1.S3), out.app.view.lens.point)
        assertTrue(out.checks.acknowledgements.none { F1.S4 in it.subjectKeys })
        assertEquals(listOf(F1.S2 to F1.S3), out.checks.travelOverrides.map { it.fromSceneKey to it.toSceneKey })
        val dangling = out.story.units.any { (it.time as? SceneTime.Relative)?.let { t -> t.baseKey == F1.S4 || t.rebasedFromKey == F1.S4 } == true }
        assertFalse(dangling)
    }

    @Test
    fun `merge memo puts B's memo after A's and unions silent cast`() {
        val doc = F1.build().updateUnit(F1.S4) { it.copy(memo = "문 닫히기 전", silentCast = listOf(K.TEO)) }
        val out = merge(doc).doc
        val s3 = out.unit(F1.S3)
        assertEquals(listOf("등불이 두 번째로 깜박이는 타이밍", "문 닫히기 전", "합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10"), s3.memo!!.lines())
        assertEquals(listOf(K.MARTHA, K.TEO), s3.silentCast)
        assertNotNull(out.unit(F1.S5))
    }
}
