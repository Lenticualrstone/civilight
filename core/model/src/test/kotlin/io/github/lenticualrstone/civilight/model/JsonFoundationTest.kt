package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonFoundationTest {
    enum class Mood { CALM, SCENE_TRANSITION, DAWN_0400 }

    @Serializable
    data class Holder(
        val mood: OpenEnum<Mood>,
        val at: Timestamp,
        val maybe: Timestamp? = null,
        val clock: ClockTime? = null,
        val p: Point? = null,
        val runs: List<TextRun> = emptyList(),
        override val ext: JsonObject? = null,
    ) : HasExt

    @Test
    fun roundTripsHolderWithUnknowns() {
        val text = """{"mood":"sceneTransition","at":"2026-11-21T23:41:00Z","clock":"05:40","p":[0.5,0.25],""" +
            """"runs":[{"t":"a"},{"k":"paren","t":"b"},{"k":"link","id":"01JD0000000000000000000000","to":{"kind":"term","key":"K"},"t":"c"},{"k":"break"},{"k":"ruby","t":"d","r":"x"}],""" +
            """"future":1}"""
        val h = CivilightJson.decode(Holder.serializer(), text)
        assertEquals(Mood.SCENE_TRANSITION, h.mood.known)
        assertEquals(TimestampLiteral, h.at)
        assertNull(h.maybe)
        assertEquals(ClockTime.of(5, 40), h.clock)
        assertEquals(Point(0.5f, 0.25f), h.p)
        assertEquals(5, h.runs.size)
        assertTrue(h.runs[4] is TextRun.Unknown)
        assertEquals(LinkTargetKind.TERM, (h.runs[2] as TextRun.Link).target.kind.known)
        assertEquals(JsonPrimitive(1), (h.ext!!["_unknown"] as JsonObject)["future"])
        val back = CivilightJson.encodeElement(Holder.serializer(), h)
        assertEquals(CivilightJson.json.parseToJsonElement(text), back)
    }

    @Test
    fun unknownEnumIsPreserved() {
        val h = CivilightJson.decode(Holder.serializer(), """{"mood":"stormy","at":"2026-11-21T23:41:00Z"}""")
        assertTrue(h.mood.isUnknown)
        assertEquals("""{"mood":"stormy","at":"2026-11-21T23:41:00Z"}""", CivilightJson.encode(Holder.serializer(), h))
    }

    @Test
    fun serialNames() {
        assertEquals("dawn0400", Mood.DAWN_0400.serialName)
        assertEquals("r15", OpenEnum.serialNameOf("R15"))
        assertEquals("chapterEpisodeScene", OpenEnum.serialNameOf("CHAPTER_EPISODE_SCENE"))
        assertEquals("r3x4", OpenEnum.serialNameOf("R3X4"))
    }

    @Test
    fun nfcOnRead() {
        val nfd = "가" // 가 in NFD
        val runs = CivilightJson.decode(ListSerializer(TextRun.serializer()), """[{"t":"$nfd"}]""")
        assertEquals("가", runs[0].shownText)
    }

    private companion object {
        const val TimestampLiteral = 1_795_304_460_000L
    }
}
