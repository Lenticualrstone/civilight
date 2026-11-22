package io.github.lenticualrstone.civilight.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.runtime.NavBackStack
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(InternalSerializationApi::class)
class RouteSerializationTest {

    @Test
    fun `the samples cover every route and every shared sheet`() {
        val routeNames = Route.serializer().descriptor.getElementDescriptor(1).elementNames.toSet()
        val sampled = RouteSamples.routes.map { it::class.serializer().descriptor.serialName }.toSet()
        assertEquals(routeNames, sampled)
        val sheetNames = SharedSheet.serializer().descriptor.getElementDescriptor(1).elementNames.toSet()
        val sampledSheets = RouteSamples.sheets.map { it::class.serializer().descriptor.serialName }.toSet()
        assertEquals(sheetNames, sampledSheets)
    }

    @Test
    fun `every route survives the back-stack serializer used for process death`() {
        val json = Json
        val serializer = NavKeySerializer<NavKey>()
        for (route in RouteSamples.routes + RouteSamples.sheets) {
            val text = json.encodeToString(serializer, route)
            assertEquals(route, json.decodeFromString(serializer, text))
        }
    }

    @Test
    fun `a whole back stack round-trips`() {
        val stack = NavBackStack<NavKey>(*RouteSamples.routes.toTypedArray())
        val serializer = NavBackStackSerializer(NavKeySerializer<NavKey>())
        val text = Json.encodeToString(serializer, stack)
        assertEquals(RouteSamples.routes, Json.decodeFromString(serializer, text).toList())
    }

    @Test
    fun `route codec reads what it writes and drops what it cannot read`() {
        val keys: List<NavKey> = listOf(Library, Script(RouteSamples.routes.filterIsInstance<ProjectRoute>().first().projectKey), LensSheet("P"))
        assertEquals(keys, RouteCodec.decodeAll(RouteCodec.encodeAll(keys)))
        assertEquals(emptyList<NavKey>(), RouteCodec.decodeAll("""[{"type":"gone.Route","value":{}}]"""))
        assertNull(RouteCodec.decodeOrNull("not json"))
    }

    @Test
    fun `every route has an owner and lens screens follow R11 §3`() {
        for (route in RouteSamples.routes) route.owner
        assertTrue(ProjectHome("P").hasLens)
        assertTrue(CastList("P").hasLens)
        assertTrue(!Script("P").hasLens)
        assertTrue(!StoryStructure("P").hasLens)
        assertTrue(!CheckCenter("P").hasLens)
        assertEquals(FeatureModule.SCRIPT, Script("P").owner)
        assertEquals(FeatureModule.SHELL, LensSheet("P").owner)
        assertEquals("P", CharacterProfile("P", "C").projectKeyOrNull)
        assertNull(Library.projectKeyOrNull)
    }

    @Test
    fun `nav results wait until consumed`() {
        val results = NavResults()
        results.send("pick", "place-1")
        assertEquals("place-1", results.consume("pick"))
        assertNull(results.consume("pick"))
    }
}
