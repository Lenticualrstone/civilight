package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.cast.PairHolder
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.doc.liveBlocksOf
import io.github.lenticualrstone.civilight.model.doc.liveScenes
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Values behind the fixture.md facts that the key-level tests do not pin. */
class ExampleProjectContentTest {
    private val doc = ExampleProject.build()

    private fun kindName(slot: SlotValue?): String? = slot?.kindKey?.let { doc.index.relationKind(it)?.name }
    private fun slotOf(recordKey: String): SlotValue = doc.index.record(recordKey)!!.valueAs(SlotValue.serializer())!!

    @Test
    fun relationValuesByKindName() {
        val r4 = slotOf(K.R4)
        assertEquals("경계" to "불빛을 아는 낯선 사람", kindName(r4) to r4.description)
        assertEquals("보호", kindName(slotOf(K.R5)))
        assertEquals("동료", kindName(slotOf(K.R6)))
        assertEquals("신뢰", kindName(slotOf(K.R7)))
        assertEquals("꼬마", doc.index.record(K.R5A)!!.valueAs(String.serializer()))
        assertEquals("하르트", doc.index.record(K.R7A)!!.valueAs(String.serializer()))

        val sera = doc.index.relation(K.REL_RIEN_SERA)!!
        val seniorJunior = doc.index.relationKind(sera.between!!.kindKey!!)!!
        assertEquals("선배" to "후배", seniorJunior.name to seniorJunior.pairName)
        assertEquals(PairHolder.B, sera.between.pairFirstHolder?.known)
        assertEquals(K.SERA, sera.endB.key)
        assertEquals("의지" to "의지하는 선배", kindName(sera.mindA) to sera.mindA!!.description)
        assertEquals("거래 상대", kindName(doc.index.relation(K.REL_RIEN_OWEN)!!.between))
        assertEquals("동료", kindName(doc.index.relation(K.REL_SERA_HART)!!.between))
        assertEquals(11 to 11, doc.project.relationKinds.partition { it.category.raw == "between" }.let { it.first.size to it.second.size })
    }

    @Test
    fun voiceIdOfHartsLine() {
        assertEquals("HRT", doc.index.character(K.HART)!!.voiceCode)
        assertEquals("HRT_0107_043", ExampleScript.voiceId(K.HART, 1, 7, 43))
        assertEquals("RIE_0102_007", doc.episodes.getValue(K.EPISODE_1_2).discardedVoiceIds.single().id)
    }

    @Test
    fun everySceneUpToTheWritingLineHasContentAndNoneAfter() {
        val scenes = doc.liveScenes
        val cut = scenes.indexOfFirst { it.key == K.SCENE_1_7_3 }
        for ((i, scene) in scenes.withIndex()) {
            val written = doc.liveBlocksOf(scene.key).any { it.hasContent() }
            assertEquals("content of ${scene.key}", i <= cut, written)
        }
    }

    @Test
    fun rienFirstAppearsIn12() {
        val inEpisode11 = doc.index.children(K.EPISODE_1_1)
        for (scene in inEpisode11) {
            assertFalse(K.RIEN in scene.silentCast)
            assertTrue(doc.liveBlocksOf(scene.key).none { b -> b.speakerKey == K.RIEN || b.options.any { it.speakerKey == K.RIEN } })
        }
        assertTrue(doc.liveBlocksOf(K.SCENE_1_2_1).any { it.speakerKey == K.RIEN })
    }

    @Test
    fun termWordsAppearAsPlainTextOnlyInTheCandidateBlocks() {
        val needles = listOf("오리지", "빛돌", "대정전", "잿불", "송전망", "상층 송전망")
        val hits = mutableMapOf<String, MutableSet<String>>()
        val texts = doc.manuscript.liveBlocks.map { b ->
            b.key to (b.runs + b.options.flatMap { it.runs }).filter { it !is TextRun.Link }.joinToString("") { it.shownText }
        } + doc.liveScenes.mapNotNull { s -> s.transitionRuns?.let { runs -> s.key to runs.filter { it !is TextRun.Link }.joinToString("") { it.shownText } } }
        for ((key, text) in texts) for (n in needles) if (n in text) hits.getOrPut(key) { mutableSetOf() } += n
        assertEquals(
            mapOf(K.BLOCK_B12 to setOf("오리지"), K.BLOCK_B32 to setOf("빛돌"), K.BLOCK_B61 to setOf("빛돌")),
            hits,
        )
    }

    @Test
    fun everyNamedKeyConstantIsUsed() {
        val constants = K::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java && it.name != "TIME" }
            .associate { it.name to it.get(null) as String }
        assertTrue(constants.size > 200)
        val used = ExampleWalk.identityKeys(doc).toSet()
        val unused = constants.filterValues { it !in used }
        assertTrue("constants not in the fixture: ${unused.keys}", unused.isEmpty())
    }
}
