package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.gen.GenInput
import io.github.lenticualrstone.civilight.domain.world.gen.GenResults
import io.github.lenticualrstone.civilight.domain.world.gen.GenSession
import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.domain.world.gen.MapGenerator
import io.github.lenticualrstone.civilight.domain.world.gen.RerollMode
import io.github.lenticualrstone.civilight.domain.world.gen.RerollResult
import io.github.lenticualrstone.civilight.domain.world.gen.StepReroll
import io.github.lenticualrstone.civilight.domain.world.gen.TerrainGenerator
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.map.GeneratedMapRecord
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The example maps' stored `generation.result` is real `gen-1` output for their stored input
 * (domain-world Requests, fixture owner): it equals a fresh generation and reads back as a whole world.
 */
class FixtureGenResultsTest {
    private val doc = ExampleProject.build()
    private val generated: List<WorldMap> = doc.maps.values.filter { it.generation != null }

    /** The world a fresh generation gives for [gen]'s stored kind, land ratio, steps and conditions. */
    private fun fresh(gen: GeneratedMapRecord): GeneratedWorld =
        TerrainGenerator.generate(GenInput(gen.kind.known!!, gen.landRatio.known!!, gen.steps, gen.conditions.orEmpty()))

    @Test
    fun theTwoGeneratedExampleMaps() {
        assertEquals(setOf(K.MAP_WORLD, K.MAP_LOWER_DISTRICT), generated.map { it.key }.toSet())
    }

    @Test
    fun storedResultEqualsAFreshGeneration() {
        for (map in generated) {
            val gen = map.generation!!
            assertEquals(map.name, TerrainGenerator.VERSION, gen.generatorVersion)
            val world = fresh(gen)
            assertEquals(map.name, GenResults.encode(world), gen.result)
            assertTrue(map.name, GenResults.decode(gen.result).unreadable.isEmpty())
            val stored = MapGenerator.storedWorld(map)
            assertNotNull(map.name, stored)
            assertEquals(map.name, world, stored)
        }
    }

    @Test
    fun startingTheStoredSessionStoresTheSameResult() {
        for (map in generated) {
            val gen = map.generation!!
            val session = GenSession(
                kind = gen.kind.known!!, landRatio = gen.landRatio.known!!, format = map.format!!.known!!, style = map.style!!.known!!,
                steps = gen.steps, conditions = gen.conditions.orEmpty(),
            )
            val started = (MapGenerator.start(doc, session, ctx()) as MapEdit.Applied).value
            assertEquals(map.name, gen.result, started.generation!!.result)
        }
    }

    @Test
    fun storedResultSurvivesTheFileRoundTrip() {
        for (map in generated) {
            val back = CivilightJson.decode(WorldMap.serializer(), CivilightJson.encode(WorldMap.serializer(), map))
            assertEquals(map.name, map.generation!!.result, back.generation!!.result)
            assertEquals(map.name, fresh(map.generation!!), MapGenerator.storedWorld(back))
        }
    }

    @Test
    fun rerollWithTheSameSeedKeepsTheStoredResult() {
        val map = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        val gen = map.generation!!
        val riverSeed = gen.steps.single { it.step.known == GenerationStepKind.RIVER }.seed
        val applied = StepReroll.apply(doc, map.key, GenerationStepKind.RIVER, riverSeed, RerollMode.KEEP_EDITED, ctx()) as RerollResult.Applied
        assertEquals(gen.result, applied.doc.maps.getValue(map.key).generation!!.result)
    }
}
