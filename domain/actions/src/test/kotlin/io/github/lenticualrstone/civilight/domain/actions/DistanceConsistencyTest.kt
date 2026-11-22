package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.story.travel.Distance
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.domain.story.travel.Distances as StoryDistances
import io.github.lenticualrstone.civilight.domain.world.map.Distances as WorldDistances

/**
 * DECISIONS §1 (WLD-66): `domain:story` travel keeps its own distance code until the review round;
 * on the example project both must give the same kilometres. Tolerance 1 m: the story module rounds
 * to metres, the world module does not.
 */
class DistanceConsistencyTest {
    private val doc = Fx.doc()
    private val story = StoryDistances(doc)
    private val world = WorldDistances(doc)

    private fun place(key: Key) = PlaceRef(PlaceRefKind.PLACE, key)

    private fun storyKm(a: PlaceRef, b: PlaceRef): Double? = when (val d = story.between(a, b)) {
        Distance.SamePlace -> 0.0
        is Distance.Route -> d.km
        is Distance.Straight -> d.km
        is Distance.NoScale -> null
    }

    /** Place pairs joined by a route on a live map (both ends on pins). */
    private fun routePairs(): List<Pair<Key, Key>> = doc.maps.values.filter { doc.liveness.isMapLive(it.key) }.flatMap { map ->
        val placeOfPin = map.pins.associate { it.key to it.placeKey }
        map.routes.mapNotNull { r ->
            val a = r.points.firstOrNull()?.pinKey?.let(placeOfPin::get)
            val b = r.points.lastOrNull()?.pinKey?.let(placeOfPin::get)
            if (a != null && b != null && a != b) a to b else null
        }
    }

    /** Every pair of live places with a pin somewhere (straight lines, lifted pins, no-scale maps). */
    private fun allPairs(): List<Pair<Key, Key>> {
        val pinned = doc.maps.values.filter { doc.liveness.isMapLive(it.key) }.flatMap { m -> m.pins.map { it.placeKey } }
            .filter { doc.liveness.isPlaceLive(it) }.distinct()
        return pinned.flatMapIndexed { i, a -> pinned.drop(i + 1).map { b -> a to b } }
    }

    private fun compare(pairs: List<Pair<Key, Key>>, requireDistance: Boolean = false) {
        for ((a, b) in pairs) {
            val s = storyKm(place(a), place(b))
            val w = world.distanceKm(place(a), place(b)).km
            val name = "${doc.index.place(a)?.name} → ${doc.index.place(b)?.name}"
            if (requireDistance) assertTrue("$name: a distance in both modules", s != null && w != null)
            if (s == null || w == null) {
                assertEquals("$name: both have no distance", s == null, w == null)
            } else {
                assertEquals(name, w, s, 0.001)
            }
        }
    }

    @Test
    fun `the example project's routes have the same length in both modules`() {
        val pairs = routePairs()
        assertEquals(2, pairs.size)
        compare(pairs, requireDistance = true)
    }

    @Test
    fun `every pinned place pair of the example project has the same distance in both modules`() {
        val pairs = allPairs()
        assertTrue(pairs.count { (a, b) -> world.distanceKm(place(a), place(b)).km != null } >= 2)
        compare(pairs)
    }
}
