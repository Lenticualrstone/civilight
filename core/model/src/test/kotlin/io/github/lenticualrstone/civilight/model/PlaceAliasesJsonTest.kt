package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.world.Place
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** `Place.aliases` (`world.json › places`, SUG-02): written only when present, read back as written. */
class PlaceAliasesJsonTest {
    private fun place(aliases: List<String> = emptyList()) =
        Place(key = ExampleKeys.PLACE_PYLON_RUINS, order = "V", name = "송전탑 폐허", aliases = aliases)

    @Test
    fun aliasesRoundTrip() {
        val place = place(listOf("송전탑", "폐허 탑"))
        val element = CivilightJson.encodeElement(Place.serializer(), place).jsonObject
        assertEquals(JsonArray(listOf(JsonPrimitive("송전탑"), JsonPrimitive("폐허 탑"))), element["aliases"])
        assertEquals(place, CivilightJson.decode(Place.serializer(), CivilightJson.encode(Place.serializer(), place)))
    }

    @Test
    fun absentAliasesReadAsEmptyAndAreNotWritten() {
        val text = """{"key":"${ExampleKeys.PLACE_PYLON_RUINS}","order":"V","name":"송전탑 폐허"}"""
        val read = CivilightJson.decode(Place.serializer(), text)
        assertEquals(emptyList<String>(), read.aliases)
        val written = CivilightJson.encodeElement(Place.serializer(), read).jsonObject
        assertFalse("aliases" in written)
        assertFalse("ext" in written)
    }

    @Test
    fun aliasesAreNfcOnRead() {
        val nfd = "송전탑".let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
        val text = """{"key":"${ExampleKeys.PLACE_PYLON_RUINS}","order":"V","name":"송전탑 폐허","aliases":["$nfd"]}"""
        assertEquals(listOf("송전탑"), CivilightJson.decode(Place.serializer(), text).aliases)
    }

    @Test
    fun fixtureWorldFileHasNoAliasesKey() {
        val world = ExampleProject.build().world
        val places = CivilightJson.encodeElement(WorldFile.serializer(), world).jsonObject.getValue("places").jsonArray
        assertEquals(world.places.size, places.size)
        assertFalse(places.any { "aliases" in (it as JsonObject) })
    }
}
