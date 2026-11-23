package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.merge.MergeChoices
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** (0.2.0, U-15.3) Create from a picker: the item exists and its key is returned to be picked. */
class CreateActionsTest {
    @Test
    fun `a character made in a picker exists with its card values and live tags only`() {
        val doc = Fx.doc()
        val live = TagActions.liveTags(doc).first().key
        val (next, key) = CreateActions.inPicker(doc, "CHARACTER", " 세라2 ", Fx.ctx(), displayName = "???", note = "기록관", tagKeys = listOf(live, "gone"))!!
        val c = next.index.character(key)!!
        assertEquals("세라2", c.name)
        assertEquals("기록관", c.oneLiner)
        assertEquals(listOf(live), c.tagKeys)
        assertEquals(doc.cast.characters.size + 1, next.cast.characters.size)
    }

    @Test
    fun `places, terms and factions made in a picker exist, a blank name or an unknown kind makes nothing`() {
        val doc = Fx.doc()
        val parent = doc.world.places.first { it.deletedAt == null }.key
        val (d1, place) = CreateActions.inPicker(doc, "PLACE", "새 골목", Fx.ctx(), note = "좁은 길", parentKey = parent)!!
        assertEquals(parent, d1.index.place(place)!!.parentPlaceKey)
        val (d2, term) = CreateActions.inPicker(d1, "TERM", "빛돌", Fx.ctx(seed = 3), alias = "등석")!!
        assertEquals(listOf("등석"), d2.index.term(term)!!.aliases)
        val (d3, faction) = CreateActions.inPicker(d2, "FACTION", "잿빛 회", Fx.ctx(seed = 5))!!
        assertNotNull(d3.index.faction(faction))
        assertNull(CreateActions.inPicker(doc, "PLACE", "  ", Fx.ctx()))
        assertNull(CreateActions.inPicker(doc, "TAG", "주연", Fx.ctx()))
        assertEquals(PlaceRefKind.PLACE.name, "PLACE")
    }

    @Test
    fun `an event's related character follows a character merge`() {
        val given = Fx.noahLive()
        val (made, kid) = CreateActions.inPicker(given, "CHARACTER", "시장 꼬마", Fx.ctx(seed = 9))!!
        val event = made.world.events.first { it.deletedAt == null }
        val withRef = made.copy(world = made.world.copy(events = made.world.events.map {
            if (it.key == event.key) it.copy(relatedCharacterKeys = listOf(kid, K.NOAH)) else it
        }))
        val result = MergeActions.mergeCharacters(withRef, kid, K.NOAH, MergeChoices(), Fx.ctx(seed = 13))
        val merged = result.doc
        assertEquals(listOf(K.NOAH), merged.index.event(event.key)!!.relatedCharacterKeys)
        assertTrue(merged.index.character(kid)!!.deletedAt != null)
        // `되돌리기` brings the event's characters back as they were.
        val undone = MergeActions.undo(merged, null, result).first
        assertEquals(listOf(kid, K.NOAH), undone.index.event(event.key)!!.relatedCharacterKeys)
    }
}
