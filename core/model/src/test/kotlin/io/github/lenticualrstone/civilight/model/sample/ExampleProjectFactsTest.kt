package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.doc.liveBlocksOf
import io.github.lenticualrstone.civilight.model.doc.liveCharacters
import io.github.lenticualrstone.civilight.model.doc.liveScenes
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.doc.recordBeforeBlock
import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The headline facts listed in docs/status/fixture.md; keep the two in step. */
class ExampleProjectFactsTest {
    private val doc = ExampleProject.build()
    private val latest = LensPoint.Latest
    private fun end(unitKey: String) = LensPoint.UnitEnd(unitKey)

    private val rienMind = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.MIND_A)
    private val hartMind = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.MIND_B)
    private val rienCalls = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.ADDRESS_A)
    private val hartCalls = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.ADDRESS_B)
    private val between = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.BETWEEN)
    private val hartName = ChangeTarget.DisplayName(K.HART)
    private val affiliation = ChangeTarget.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION)

    @Test
    fun writingLineIsTheEndOf173() {
        assertEquals(K.SCENE_1_7_3, doc.manuscript.writingLineSceneKey)
        assertEquals(K.BLOCK_048, doc.liveBlocksOf(K.SCENE_1_7_3).last().key)
        // 1-7 씬 4 has no blocks; 1-8 씬 1 has one empty block that is not content.
        assertTrue(doc.liveBlocksOf(K.SCENE_1_7_4).isEmpty())
        val empty = doc.liveBlocksOf(K.SCENE_1_8_1).single()
        assertEquals(K.BLOCK_1_8_001, empty.key)
        assertFalse(empty.hasContent())
    }

    @Test
    fun structureCounts() {
        val chapter1 = doc.liveScenes.filter { doc.index.ancestors(it).any { a -> a.key == K.CHAPTER_1 } }
        assertEquals(31, chapter1.size)
        assertEquals(22, chapter1.count { it.place != null })
        assertEquals(19, chapter1.count { it.time !is SceneTime.None })
        assertEquals(34, doc.liveScenes.size)
        assertEquals(10, doc.index.children(K.CHAPTER_1).size)
        assertEquals(8, doc.index.children(K.CHAPTER_2).size)
        assertEquals(4, doc.index.children(K.EPISODE_1_7).count { doc.liveness.isLive(it) })
        assertEquals(listOf(true, true, true, true, false), doc.index.children(K.EPISODE_1_6).map { doc.liveness.isLive(it) })
        assertEquals(58, doc.index.unit(K.EPISODE_1_7)!!.nextBlockNumber)
        assertEquals(61, doc.index.unit(K.EPISODE_1_8)!!.nextBlockNumber)
        assertEquals("잿빛 새벽의 등불", doc.index.unit(K.EPISODE_1_7)!!.title)
        assertNull(doc.index.unit(K.EPISODE_1_8)!!.title)
        assertEquals("등불지기", doc.index.unit(K.CHAPTER_1)!!.title)
        assertEquals("잿불", doc.index.unit(K.CHAPTER_2)!!.title)
    }

    @Test
    fun scene173Blocks() {
        val blocks = doc.liveBlocksOf(K.SCENE_1_7_3)
        assertEquals((36..48).toList(), blocks.map { it.number })
        assertEquals(7, blocks.indexOfFirst { it.key == K.BLOCK_043 })
        assertEquals(K.HART, blocks[7].speakerKey)
        assertEquals("멈추지 마. 저건 등불이 아니야.", blocks[7].runs.plainText)
        assertEquals(listOf(TextRun.Paren("작게"), TextRun.Plain("저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?")), blocks[6].runs)
        val l44 = blocks[8].runs.filterIsInstance<TextRun.Link>().single()
        assertEquals(K.LINK_L44 to K.TERM_ORIGINIUM_LANTERN, l44.linkKey to l44.target.key)
        assertEquals(BlockKind.MEMO, doc.index.block(K.BLOCK_046)!!.kind.known)
        assertEquals(listOf(K.MARTHA), doc.index.unit(K.SCENE_1_7_3)!!.silentCast)
        assertEquals(
            SceneTime.Relative(baseKey = K.SCENE_1_7_2, dayMin = 1, dayMax = 1, clock = ClockTime.of(5, 40)),
            doc.index.unit(K.SCENE_1_7_3)!!.time,
        )
        // Voice IDs of 1-7 are automatic; 1-1 … 1-3 were fixed by the 2026-11-10 export.
        assertTrue(blocks.all { it.voice == null })
        assertEquals("SRA_0101_002", doc.index.block(K.block(1, 1, 2))!!.voice!!.fixedId)
        assertEquals("NOA_0106_007", doc.index.block(K.BLOCK_1_6_007)!!.voice!!.fixedId)
        // 1-7 씬 2 #027 has no speaker.
        val missing = doc.index.block(K.BLOCK_1_7_027)!!
        assertEquals(BlockKind.DIALOGUE to null, missing.kind.known to missing.speakerKey)
    }

    @Test
    fun relationAndNameByLens() {
        assertNull(doc.recordAt(rienMind, end(K.EPISODE_1_6)))
        assertEquals(K.R4, doc.recordAt(rienMind, end(K.EPISODE_1_7))?.key)
        assertEquals(K.R4, doc.recordAt(rienMind, end(K.EPISODE_1_8))?.key)
        assertEquals(K.R4, doc.recordAt(rienMind, latest)?.key)
        assertEquals(K.R7, doc.recordAt(rienMind, end(K.EPISODE_1_9))?.key)
        assertEquals(K.R5, doc.recordAt(hartMind, latest)?.key)
        assertEquals(K.R5, doc.recordAt(hartMind, end(K.EPISODE_1_9))?.key)
        assertEquals(K.R5A, doc.recordAt(hartCalls, latest)?.key)
        assertNull(doc.recordAt(rienCalls, latest))
        assertEquals(K.R7A, doc.recordAt(rienCalls, end(K.EPISODE_1_9))?.key)
        assertNull(doc.recordAt(between, latest))
        assertEquals(K.R6, doc.recordAt(between, end(K.EPISODE_1_9))?.key)
        // 표시 이름: base `???` until 1-8, cleared (show 이름) from 1-9.
        assertEquals("???", doc.index.character(K.HART)!!.displayNameBase)
        assertNull(doc.recordAt(hartName, latest))
        assertNull(doc.recordAt(hartName, end(K.EPISODE_1_8)))
        val reveal = doc.recordAt(hartName, end(K.EPISODE_1_9))!!
        assertEquals(K.R8 to null, reveal.key to reveal.value)
    }

    @Test
    fun affiliationHistory() {
        assertEquals(listOf(K.R1, K.R2, K.R3), doc.history(affiliation).map { it.key })
        assertEquals(K.R2, doc.recordAt(affiliation, end(K.EPISODE_1_5))?.key)
        assertEquals(K.R2, doc.recordAt(affiliation, latest)?.key)
        assertEquals(K.R3, doc.recordAt(affiliation, end(K.EPISODE_2_5))?.key)
        val value = doc.recordAt(affiliation, latest)!!.valueAs(FieldValue.serializer())!!
        assertEquals(FieldPayload.Texts(listOf("등불 결사")), value.payload)
    }

    @Test
    fun plannedRecords() {
        val planned = doc.changes.records.filter { doc.liveness.isLive(it) && doc.isPlanned(it) }.map { it.key }.toSet()
        assertEquals(
            setOf(K.R3, K.R6, K.R7, K.R7A, K.R8, K.REGION_SHAPE_1_9, K.REVEAL_EMBER_OATH, K.REVEAL_UPPER_GRID),
            planned,
        )
    }

    @Test
    fun scriptPanelAtBlocks() {
        assertNull(doc.recordBeforeBlock(rienMind, K.BLOCK_042))
        assertNull(doc.recordBeforeBlock(rienMind, K.BLOCK_043))
        assertEquals(setOf(K.R4, K.R5, K.R5A), doc.recordsHere(K.BLOCK_043).map { it.key }.toSet())
        assertEquals(K.R4, doc.recordBeforeBlock(rienMind, K.BLOCK_044)?.key)
        assertEquals(setOf(K.R9), doc.recordsHere(K.BLOCK_044).map { it.key }.toSet())
    }

    @Test
    fun termRevealsAndPreRevealUse() {
        assertEquals(listOf("오리지늄", "빛돌"), doc.index.term(K.TERM_ORIGINIUM_LANTERN)!!.aliases)
        assertTrue(doc.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, K.BLOCK_B42))
        assertFalse(doc.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, K.BLOCK_044))
        assertFalse(doc.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, K.BLOCK_045))
        assertFalse(doc.isLinkBeforeReveal(K.TERM_BLACKOUT, K.BLOCK_B41))
        assertFalse(doc.isLinkBeforeReveal(K.TERM_LANTERN_SOCIETY, K.BLOCK_1_1_001))
        assertEquals("등불 속 그 돌, 오리지… 뭐라고 했죠?", doc.index.block(K.BLOCK_B12)!!.runs.plainText)
        assertEquals("빛돌 하나면 한 달은 버텨.", doc.index.block(K.BLOCK_B32)!!.runs.plainText)
        assertEquals("하층에서 빛돌을 판다고?", doc.index.block(K.BLOCK_B61)!!.runs.plainText)
        assertTrue(doc.index.block(K.BLOCK_B12)!!.runs.none { it is TextRun.Link })
    }

    @Test
    fun noahInTheTrash() {
        assertFalse(doc.liveness.isCharacterLive(K.NOAH))
        assertEquals(11, doc.liveCharacters.size)
        assertEquals(12, doc.cast.characters.size)
        val liveLines = doc.manuscript.liveBlocks.filter { it.speakerKey == K.NOAH }
        assertEquals(14, liveLines.size)
        assertEquals(mapOf(K.SCENE_1_6_2 to 6, K.SCENE_1_6_4 to 8), liveLines.groupingBy { it.sceneKey }.eachCount())
        val trashedScene = doc.index.blocksOfScene(K.SCENE_1_6_5)
        assertTrue(trashedScene.none { doc.liveness.isLive(it) })
        assertTrue(trashedScene.any { it.runs.plainText == "노아가 웃었다." })
        assertEquals(2, doc.liveScenes.count { K.NOAH in it.silentCast })
        val liveLinks = ExampleWalk.links(doc).filter { (surface, link) ->
            link.target.key == K.NOAH && surface.kind == ExampleWalk.Kind.DOCUMENT ||
                link.target.key == K.NOAH && doc.manuscript.liveBlocks.any { b -> b.runs.contains(link) }
        }
        assertEquals(3, liveLinks.size)
        assertEquals(1, ExampleRecorder.links().count { it.targetKey == K.NOAH })
        val relations = doc.cast.relations.filter { it.endA.key == K.NOAH || it.endB.key == K.NOAH }
        assertEquals(listOf(K.REL_OWEN_NOAH), relations.map { it.key })
        assertFalse(doc.liveness.isLive(relations.single()))
        val records = doc.changes.records.filter { r ->
            (r.target as? ChangeTarget.Field)?.entityKey == K.NOAH || (r.target as? ChangeTarget.RelationSlot)?.relationKey == K.REL_OWEN_NOAH
        }
        assertEquals(listOf(K.N1, K.N2), records.map { it.key })
        assertTrue(records.none { doc.liveness.isLive(it) })
        assertEquals(ExampleClock.localDate(ExampleClock.NOW).plusDays(3), doc.index.trashEntry(K.TRASH_NOAH)!!.purgeOn)
    }

    @Test
    fun castFacts() {
        val rien = doc.index.character(K.RIEN)!!
        assertEquals(7, rien.recordNo)
        assertEquals("막내 등불지기", rien.oneLiner)
        assertEquals(13, doc.project.counters.nextCharacterRecordNo)
        val female = doc.cast.characters.filter { c ->
            (c.fields[K.FIELD_GENDER]?.payload as? FieldPayload.Choice)?.optionKeys == listOf(K.OPTION_GENDER_FEMALE)
        }
        assertEquals(setOf(K.RIEN, K.SERA, K.MARTHA, K.IRENA), female.map { it.key }.toSet())
        assertTrue(doc.cast.characters.all { K.FIELD_GENDER in it.fields })
        assertEquals(FieldValue.UNKNOWN, doc.index.character(K.HART)!!.fields[K.FIELD_AGE])
        assertEquals(listOf("나이", "소속", "신장", "종족"), doc.index.fieldDefs(EntityKind.CHARACTER).filter { it.inHeader }.map { it.name })
        assertEquals(listOf("나이", "소속"), doc.index.fieldDefs(EntityKind.CHARACTER).filter { it.inList }.map { it.name })
        assertEquals(11, doc.index.fieldDefs(EntityKind.CHARACTER).size)
        // Relations live at lens 1-7: 리엔·하르트 (records), 리엔·세라, 리엔·오웬, 세라·하르트.
        assertEquals(4, doc.cast.relations.count { doc.liveness.isLive(it) })
        // 리엔's voice lines: 5, two of them empty (`▲ 보이스 비어 있음`).
        val lines = doc.index.voiceLines(K.RIEN)
        assertEquals(5, lines.size)
        assertEquals(listOf("신뢰 상승 후", "전투 시작"), lines.filter { it.text.isEmpty() }.map { it.title })
    }

    @Test
    fun timeInputsFollowFixtureT() {
        fun time(key: String) = doc.index.unit(key)!!.time
        assertEquals(SceneTime.Direct(storyDay = 1), time(K.SCENE_1_1_1))
        assertEquals(SceneTime.None, time(K.SCENE_1_3_1))
        val s6 = time(K.SCENE_1_6_1) as SceneTime.Relative
        assertEquals(listOf(K.SCENE_1_3_2, 5, 9), listOf(s6.baseKey, s6.dayMin, s6.dayMax))
        assertEquals(TimePrecision.APPROX, s6.precision.known)
        assertEquals(Band.DAY, s6.band?.known)
        assertTrue(time(K.SCENE_1_4_2) is SceneTime.Flashback)
        assertEquals(SceneTime.Direct(storyDay = 20, band = Band.DAY.open()), time(K.SCENE_1_9_2))
        val s10 = doc.index.unit(K.SCENE_1_7_4)!!
        assertEquals("결사의 문 앞 · 06:10", s10.transitionRuns!!.plainText)
    }

    @Test
    fun gateRouteIs38Km() {
        val map = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        val pins = map.pins.associateBy { it.key }
        val points = map.routes.single { it.key == K.ROUTE_GATE_TO_UPPER_GATE }.points.map { p ->
            p.pinKey?.let { pins.getValue(it).let { pin -> Point(pin.x, pin.y) } } ?: Point(p.x!!, p.y!!)
        }
        val length = points.zipWithNext { a, b -> hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()) }.sum()
        val scale = map.scale!!
        val kmPerUnit = scale.value * 15.0 / hypot((scale.b.x - scale.a.x).toDouble(), (scale.b.y - scale.a.y).toDouble())
        assertEquals(ExampleMaps.LOWER_DISTRICT_KM_PER_UNIT, kmPerUnit, 0.001)
        assertEquals(38.0, length * kmPerUnit, 0.01)
    }
}
