package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S10
import io.github.lenticualrstone.civilight.domain.story.Fx.S11
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.bulk.SuggestionSource
import io.github.lenticualrstone.civilight.domain.story.bulk.Suggestions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SUG-02 (20-story-time §4.11) as drawn, with the alias stored on the place (`Place.aliases`):
 * `BulkTest.sug02_bodySentenceAsDrawn` stays ignored because the example fixture's 송전탑 폐허 has no
 * alias; this is its Given plus that alias in the model.
 */
class PlaceAliasSuggestionTest {
    /** SUG-02's Given: 1-7 씬 4 and 1-8 씬 1 untimed, 1-8 #001 holds the narration, 송전탑 폐허 has the alias 송전탑. */
    private fun given(aliases: List<String> = listOf("송전탑")): ProjectDoc {
        val d = Fx.doc().withTime(S10, SceneTime.None).withTime(S11, SceneTime.None)
        val b001 = K.block(1, 8, 1)
        return d.copy(
            episodes = d.episodes.mapValues { (_, f) ->
                f.copy(blocks = f.blocks.map { if (it.key == b001) it.copy(runs = listOf(TextRun.Plain("이틀 뒤, 송전탑 그림자가 길게 누운 새벽."))) else it })
            },
            world = d.world.copy(places = d.world.places.map { if (it.key == K.PLACE_PYLON_RUINS) it.copy(aliases = aliases) else it }),
        )
    }

    @Test
    fun sug02_bodySentenceAsDrawnWithTheAliasInTheModel() {
        val s = Suggestions(given()).suggestionFor(S11)!!
        assertEquals(SuggestionSource.BODY, s.source)
        assertEquals("이틀 뒤, 송전탑 그림자가 길게 누운 새벽.", s.evidence.text)
        assertEquals(listOf("이틀 뒤", "송전탑", "새벽"), s.evidence.boxed)
        assertEquals(PlaceRef(PlaceRefKind.PLACE, K.PLACE_PYLON_RUINS), s.place!!.ref)
        assertEquals("송전탑 폐허 | +2일 · 새벽", s.line)
    }

    @Test
    fun modelAliasesAndPassedAliasesMerge() {
        val withBoth = Suggestions(given(), mapOf(K.PLACE_PYLON_RUINS to listOf("송전탑"))).suggestionFor(S11)!!
        assertEquals(listOf("이틀 뒤", "송전탑", "새벽"), withBoth.evidence.boxed)
        assertEquals("송전탑 폐허 | +2일 · 새벽", withBoth.line)
        // A passed alias still works next to the place's own (other) alias.
        val passedOnly = Suggestions(given(listOf("철탑")), mapOf(K.PLACE_PYLON_RUINS to listOf("송전탑"))).suggestionFor(S11)!!
        assertEquals("송전탑 폐허 | +2일 · 새벽", passedOnly.line)
        // Without any alias only the time is read.
        assertEquals("장소 없음 | +2일 · 새벽", Suggestions(given(emptyList())).suggestionFor(S11)!!.line)
    }
}
