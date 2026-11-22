package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.gen.ConditionReading
import io.github.lenticualrstone.civilight.domain.world.gen.GenResults
import io.github.lenticualrstone.civilight.domain.world.gen.GenSession
import io.github.lenticualrstone.civilight.domain.world.gen.KeywordConditionReader
import io.github.lenticualrstone.civilight.domain.world.gen.LanguageModelConditionReader
import io.github.lenticualrstone.civilight.domain.world.gen.MapGenerator
import io.github.lenticualrstone.civilight.domain.world.gen.ReadResult
import io.github.lenticualrstone.civilight.domain.world.gen.RerollMode
import io.github.lenticualrstone.civilight.domain.world.gen.RerollResult
import io.github.lenticualrstone.civilight.domain.world.gen.StepReroll
import io.github.lenticualrstone.civilight.domain.world.gen.TerrainGenerator
import io.github.lenticualrstone.civilight.domain.world.gen.TextConditions
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.map.AttachedLayer
import io.github.lenticualrstone.civilight.model.map.ConditionStatus
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.GenerationStep
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.map.TextCondition
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD §4.7 generator cases (WLD-T61 … WLD-T63) and generator edge cases. */
class GenSpecTest {
    private val doc = ExampleProject.build()

    private fun steps(vararg seeds: Int, choices: Map<GenerationStepKind, Map<String, String>> = emptyMap()) =
        listOf(GenerationStepKind.COAST, GenerationStepKind.MOUNTAIN, GenerationStepKind.RIVER, GenerationStepKind.FOREST_CLIMATE, GenerationStepKind.TOWN_ROAD)
            .zip(seeds.toList()) { k, s -> GenerationStep(k.open(), s, choices = choices[k].orEmpty()) }

    private val fixedChips = mapOf(
        GenerationStepKind.MOUNTAIN to mapOf("ranges" to "2", "height" to "보통"),
        GenerationStepKind.RIVER to mapOf("count" to "보통", "bends" to "보통", "lakes" to "on"),
    )

    private fun json(e: JsonElement): String = CivilightJson.json.encodeToString(JsonElement.serializer(), e)

    private fun layer(session: GenSession, step: GenerationStepKind): String =
        json(GenResults.layers(GenResults.encode(session.preview()), listOf(step)))

    @Test
    fun `WLD-T61 generator determinism and step locks`() {
        val session = GenSession(kind = GeneratedMapKind.CONTINENT, landRatio = LandRatio.HALF, steps = steps(4827, 112, 31, 907, 215, choices = fixedChips))
        val a = json(GenResults.encode(session.preview()))
        val b = json(GenResults.encode(session.preview()))
        assertEquals(a, b)
        assertEquals(TerrainGenerator.VERSION, session.preview().version)

        // Property over many seeds: re-rolling 강 with 해안선 and 산 locked changes only the river layer.
        for (coastSeed in listOf(4827, 1, 77, 2024, 9999, 31337)) {
            val base = GenSession(steps = steps(coastSeed, 112, 31, 907, 215, choices = fixedChips))
                .lock(GenerationStepKind.COAST, true).lock(GenerationStepKind.MOUNTAIN, true)
            for (riverSeed in listOf(32, 500, 4242)) {
                val rerolled = base.reroll(GenerationStepKind.RIVER, riverSeed)
                assertEquals(layer(base, GenerationStepKind.COAST), layer(rerolled, GenerationStepKind.COAST))
                assertEquals(layer(base, GenerationStepKind.MOUNTAIN), layer(rerolled, GenerationStepKind.MOUNTAIN))
                assertEquals(layer(base, GenerationStepKind.FOREST_CLIMATE), layer(rerolled, GenerationStepKind.FOREST_CLIMATE))
                assertEquals(layer(base, GenerationStepKind.TOWN_ROAD), layer(rerolled, GenerationStepKind.TOWN_ROAD))
                assertNotEquals("coast $coastSeed river $riverSeed", layer(base, GenerationStepKind.RIVER), layer(rerolled, GenerationStepKind.RIVER))
            }
            // A locked step never changes on `다시`.
            assertEquals(base, base.reroll(GenerationStepKind.COAST, 5))
        }
    }

    private fun startedDetail(c: io.github.lenticualrstone.civilight.domain.world.common.EditContext): Pair<ProjectDoc, WorldMap> {
        val session = GenSession(
            format = MapFormat.DETAIL,
            steps = steps(4827, 112, 31, 907, 215, choices = mapOf(GenerationStepKind.RIVER to mapOf("count" to "많이"))),
        )
        val started = MapGenerator.start(doc, session, c, name = "새 대륙") as MapEdit.Applied
        return started.doc to started.value
    }

    @Test
    fun `WLD-T62 re-roll guard`() {
        val c = ctx()
        val (started, map) = startedDetail(c)
        val rivers = map.drawnLines.filter { it.kind.known == DrawnLineKind.RIVER }
        assertTrue("rivers ${rivers.size}", rivers.size >= 3)
        // Two rivers edited by hand, and a bridge stamp attached to the river layer.
        val edited = rivers.take(2).map { it.copy(editedByAuthor = true, points = it.points.map { p -> Point(p.x, (p.y + 0.001f).coerceAtMost(1f)) }) }
        var given = MapEdits.updateMap(started, map.key, c) { m ->
            m.copy(drawnLines = m.drawnLines.map { l -> edited.firstOrNull { it.key == l.key } ?: l })
        }!!
        val bridge = MapEdits.placeStamp(given, map.key, StampKind.BRIDGE, rivers[2].points[1], c) as MapEdit.Applied
        given = MapEdits.updateStamp(bridge.doc, map.key, bridge.value.key, c) { it.copy(attachedLayer = AttachedLayer.RIVER.open()) }!!
        val before = given.maps.getValue(map.key)

        val sheet = StepReroll.sheet(before, GenerationStepKind.RIVER)!!
        assertEquals("강 다시 만들기", sheet.title)
        assertEquals("직접 고친 강 2 · 강에 붙은 도장 1(다리)", sheet.line)
        assertEquals(RerollMode.KEEP_EDITED, sheet.defaultMode)
        assertEquals("고친 강은 남기기 · 나머지만 새로", sheet.options.first().second)
        assertEquals("모두 새로 · 다리 도장 → 위치 확인 목록", sheet.options[1].second)
        assertEquals("이전 강 · 기록에 보관 · 되돌리기 가능", sheet.note)
        assertEquals(listOf("취소", "다시 만들기"), sheet.buttons)

        val applied = StepReroll.apply(given, map.key, GenerationStepKind.RIVER, 777, sheet.defaultMode, c) as RerollResult.Applied
        val after = applied.doc.maps.getValue(map.key)
        for (e in edited) assertEquals(e, after.drawnLines.single { it.key == e.key })
        val generatedBefore = before.drawnLines.filter { it.kind.known == DrawnLineKind.RIVER && !it.editedByAuthor }.map { it.key }.toSet()
        assertTrue(after.drawnLines.none { it.key in generatedBefore })
        assertEquals(777, after.generation!!.steps.single { it.step.known == GenerationStepKind.RIVER }.seed)
        val entry = after.generation!!.history.last()
        assertEquals("RIVER", (entry.resultRef as JsonObject)["step"].toString().trim('"'))
        assertEquals(before.generation!!.steps, entry.steps)
        // Roads and stamps stay.
        assertEquals(before.drawnLines.filter { it.kind.known == DrawnLineKind.ROAD }, after.drawnLines.filter { it.kind.known == DrawnLineKind.ROAD })
        assertEquals(before.stamps, after.stamps)

        val undone = StepReroll.undo(applied.doc, map.key, c)!!.maps.getValue(map.key)
        assertEquals(before.generation, undone.generation)
        assertEquals(before.drawnLines.toSet(), undone.drawnLines.toSet())

        // `모두 새로`: every river is new and the bridge goes to the 위치 확인 list.
        val allNew = StepReroll.apply(given, map.key, GenerationStepKind.RIVER, 778, RerollMode.ALL_NEW, c) as RerollResult.Applied
        assertTrue(allNew.doc.maps.getValue(map.key).drawnLines.none { it.editedByAuthor })
        assertEquals(listOf("다리"), allNew.checks.map { it.label })
        // A layer nobody edited needs no sheet.
        assertNull(StepReroll.sheet(map, GenerationStepKind.RIVER))
    }

    @Test
    fun `WLD-T63 text conditions never invent names`() {
        val readings = listOf(
            ConditionReading("숲", setOf(0, 3, 6), placeName = "결사의 문"),
            ConditionReading("항구 도시", setOf(6, 7, 8), traits = listOf("해안"), placeName = "은빛 항구"),
        )
        val conditions = TextConditions.resolve(doc, readings, ctx())
        assertEquals(K.PLACE_SOCIETY_GATE, conditions[0].placeKey)
        assertEquals("숲", conditions[0].what)
        assertNull(conditions[1].placeKey)
        assertEquals("항구 도시", conditions[1].what)
        assertEquals(setOf(6, 7, 8), conditions[1].where)
        assertEquals(listOf("해안"), conditions[1].traits)
        assertEquals("서쪽 · 장소 ‘결사의 문’", TextConditions.row(doc, conditions[0]).small)
        assertEquals("남쪽 해안", TextConditions.row(doc, conditions[1]).small)

        // Four conditions; the generator can act on three (an unknown 무엇 needs a check).
        val four = TextConditions.resolve(
            doc,
            listOf(
                ConditionReading("산맥", setOf(0, 1, 2), listOf("눈 덮임")),
                ConditionReading("사막", setOf(2, 5, 8), listOf("메마름")),
                ConditionReading("숲", setOf(0, 3, 6), placeName = "결사의 문"),
                ConditionReading("용의 둥지", setOf(4)),
            ),
            ctx(),
        )
        val session = GenSession(steps = steps(4827, 112, 31, 907, 215), conditions = four)
        val started = MapGenerator.start(doc, session, ctx(), name = "글로 만든 지도") as MapEdit.Applied
        val stored = started.value.generation!!.conditions!!
        assertEquals("조건 4 · 3 반영", TextConditions.header(stored))
        assertEquals(ConditionStatus.NEEDS_CHECK, stored.single { it.what == "용의 둥지" }.status.known)
        assertEquals("확인 필요", TextConditions.row(doc, stored.last()).status)
    }

    @Test
    fun `biased conditions are met across seeds`() {
        val conditions = listOf(
            TextCondition("01JZ000000TEST000000000001", "산맥", setOf(0, 1, 2), status = ConditionStatus.NEEDS_CHECK.open()),
            TextCondition("01JZ000000TEST000000000002", "사막", setOf(2, 5, 8), status = ConditionStatus.NEEDS_CHECK.open()),
            TextCondition("01JZ000000TEST000000000003", "숲", setOf(0, 3, 6), status = ConditionStatus.NEEDS_CHECK.open()),
        )
        for (kind in GeneratedMapKind.entries) for (seed in listOf(4827, 12, 900)) {
            val world = GenSession(kind = kind, steps = steps(seed, seed + 1, seed + 2, seed + 3, seed + 4), conditions = conditions).preview()
            val evaluated = TextConditions.evaluate(world, conditions)
            assertEquals("$kind $seed", "조건 3 · 3 반영", TextConditions.header(evaluated))
        }
    }

    @Test
    fun `keyword reader and language-model stub`() {
        val text = "북쪽은 눈 덮인 산맥이 가로막고, 남쪽 해안에 항구 도시가 있다. 동쪽은 메마른 사막, 서쪽 숲속에 결사의 문."
        val read = KeywordConditionReader.read(text, doc.world.places.map { it.name }) as ReadResult.Read
        assertEquals(listOf("산맥", "항구 도시", "사막", "숲"), read.readings.map { it.what })
        assertEquals(listOf(setOf(0, 1, 2), setOf(6, 7, 8), setOf(2, 5, 8), setOf(0, 3, 6)), read.readings.map { it.where })
        assertEquals(listOf("눈 덮임"), read.readings[0].traits)
        assertEquals("결사의 문", read.readings[3].placeName)
        assertEquals("북쪽은 눈 덮인 산맥이 가로막고", text.substring(read.readings[0].span!!))
        val conditions = TextConditions.resolve(doc, read.readings, ctx())
        assertEquals(listOf("산맥", "북쪽 · 눈 덮임"), TextConditions.row(doc, conditions[0]).let { listOf(it.name, it.small) })
        assertEquals("읽어 낸 조건 · 4 | 키워드", TextConditions.readLine(conditions, null))
        assertTrue(LanguageModelConditionReader(onDevice = true).read(text, emptyList()) is ReadResult.Unavailable)
    }

    @Test
    fun `keyword reader ignores words inside other words`() {
        // `에서` holds no direction `서`.
        val near = (KeywordConditionReader.read("강에서 가까운 마을", emptyList()) as ReadResult.Read).readings.single()
        assertEquals("강", near.what)
        assertEquals((0..8).toSet(), near.where)
        // `강한` is not a river.
        val windy = (KeywordConditionReader.read("동쪽은 강한 바람이 부는 사막", emptyList()) as ReadResult.Read).readings.single()
        assertEquals("사막", windy.what)
        assertEquals(setOf(2, 5, 8), windy.where)
        // A clause without any 무엇 word yields nothing.
        assertTrue((KeywordConditionReader.read("바람이 분다.", emptyList()) as ReadResult.Read).readings.isEmpty())
    }

    @Test
    fun `session words, candidates and history`() {
        val session = GenSession(steps = steps(4827, 112, 31, 907, 215)).lock(GenerationStepKind.COAST, true)
        assertEquals("상세 지형 · 색면 · 시드 4827 · 해안선 고정", session.header)
        assertEquals("같은 세계 · 시드 4827 · 양식만 다름 · 시작 전까지 바꾸기 가능", session.formatNote)
        assertEquals("강 · 시드 0031" to listOf("강만 다시", "강 고정"), session.stepCard(GenerationStepKind.RIVER))
        var n = 5000
        val (withHistory, candidates) = session.newCandidates(3, { n++ }, ExampleProjectClock.NOW)
        assertEquals("후보 기록 3", withHistory.historyLabel)
        assertTrue(candidates.all { it.step(GenerationStepKind.COAST).seed == 4827 })
        assertEquals(3, candidates.map { it.step(GenerationStepKind.RIVER).seed }.toSet().size)
        assertEquals(candidates[1].steps, withHistory.chooseFromHistory(1)!!.steps)
        // Format changes keep the same world.
        assertEquals(session.preview(), session.copy(format = MapFormat.HEX).preview())
    }

    @Test
    fun `formats from one world`() {
        val session = GenSession(format = MapFormat.HEX, steps = steps(4827, 112, 31, 907, 215))
        val hexMap = (MapGenerator.start(doc, session, ctx()) as MapEdit.Applied).value
        assertEquals(32 * 24, hexMap.hex!!.cells.size)
        assertEquals(24.0, MapScales.valueKm(doc, hexMap.scale!!)!!, 1e-9)
        assertEquals(session.preview(), MapGenerator.storedWorld(hexMap))
        val poly = (MapGenerator.start(doc, session.copy(format = MapFormat.POLY), ctx()) as MapEdit.Applied).value.poly!!
        assertEquals(96, poly.cells.size)
        assertTrue(poly.provinces.size in 1..8)
        assertTrue(poly.cellProvince.values.all { k -> poly.provinces.any { it.key == k } })
        assertTrue(poly.provinces.all { it.name.isEmpty() })
        val outline = (MapGenerator.start(doc, session.copy(format = MapFormat.OUTLINE), ctx()) as MapEdit.Applied).value
        assertTrue(outline.contours)
        assertTrue(outline.stamps.isEmpty() && outline.drawnLines.isEmpty())
        val node = (MapGenerator.start(doc, session.copy(format = MapFormat.NODE), ctx()) as MapEdit.Applied).value
        assertNull(node.generation)
    }

    @Test
    fun `a foreign stored result reads leniently`() {
        // The minimal shape the example maps stored before they held real gen-1 results
        // (`towns` a number, `coastline` a flat point list); the fixture's own results: FixtureGenResultsTest.
        val result = CivilightJson.json.parseToJsonElement(
            """{"coastline":[[0.05,0.5],[0.95,0.5]],"rivers":[],"towns":4,"roads":[],"stamps":[]}""",
        ) as JsonObject
        val stored = GenResults.decode(result)
        assertEquals(1, stored.coastline!!.size)
        assertEquals(emptyList<Any>(), stored.rivers)
        assertTrue("towns" in stored.unreadable)
        assertNull(stored.toWorld())
        val map = doc.maps.getValue(K.MAP_WORLD)
        assertNull(MapGenerator.storedWorld(map.copy(generation = map.generation!!.copy(result = result))))
    }
}

private object ExampleProjectClock {
    val NOW: Long = io.github.lenticualrstone.civilight.model.sample.ExampleClock.NOW
}
