package io.github.lenticualrstone.civilight.feature.world

import io.github.lenticualrstone.civilight.domain.world.time.EventDetails
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateCards
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateDraft
import io.github.lenticualrstone.civilight.feature.world.time.EventDraft
import io.github.lenticualrstone.civilight.feature.world.time.EventForms
import io.github.lenticualrstone.civilight.feature.world.time.EventSave
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0, U-15.7, WLD-74) An event's `관련 인물`: create card, event sheet, detail row, masking. */
class EventPeopleTest {
    private fun live(doc: ProjectDoc) = doc.characters.filter { it.deletedAt == null && !it.hidden }

    @Test
    fun `the create card writes the event's characters and the edit sheet keeps them`() {
        val doc = WorldTestFixture.stateA()
        val (a, b) = live(doc).take(2)
        val draft = WorldCreateDraft.event().copy(name = "결사 창립", characterKeys = listOf(a.key, b.key))
        assertTrue(draft.hasContent)
        val saved = EventForms.save(doc, WorldCreateCards.eventDraft(draft), null, WorldTestFixture.ctx()) as EventSave.Saved
        val event = saved.doc.index.event(saved.eventKey)!!
        assertEquals(listOf(a.key, b.key), event.relatedCharacterKeys)
        val edit = EventDraft.of(event).toggleCharacter(a.key)
        val again = EventForms.save(saved.doc, edit, event.key, WorldTestFixture.ctx(seed = 9)) as EventSave.Saved
        assertEquals(listOf(b.key), again.doc.index.event(event.key)!!.relatedCharacterKeys)
    }

    @Test
    fun `a hidden character reads 숨긴 인물 while locked and a trashed one is skipped`() {
        val doc = WorldTestFixture.stateA()
        val (a, b, c) = live(doc).take(3)
        val masked = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map {
            when (it.key) {
                b.key -> it.copy(hidden = true)
                c.key -> it.copy(deletedAt = 1L)
                else -> it
            }
        }))
        val keys = listOf(a.key, b.key, c.key, "gone")
        assertEquals("${a.name} · 숨긴 인물", EventForms.peopleLabel(masked, keys, hiddenOpen = false))
        assertEquals("${a.name} · ${b.name}", EventForms.peopleLabel(masked, keys, hiddenOpen = true))
        assertEquals("—", EventForms.peopleLabel(masked, emptyList(), hiddenOpen = true))
        val event = masked.world.events.first { it.deletedAt == null }.copy(relatedCharacterKeys = keys)
        assertEquals(listOf(a.name, EventDetails.HIDDEN_PERSON), EventDetails.relatedPeopleOf(masked, event, hiddenUnlocked = false))
    }
}
