package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-33 `+ 변화`: a `◆` anchored on the event (rule 12 `세계 사건`). */
class EventChangesTest {
    private val doc = WorldTestFixture.stateB()
    private val ctx = EditContext(newKey = WorldTestFixture.keys(), now = ExampleClock.NOW, device = "test-device")
    private val blackout = K.EVENT_BLACKOUT

    @Test
    fun `offered on a live world event, not on a story event`() {
        assertTrue(EventChanges.offered(doc, blackout))
        assertFalse(EventChanges.offered(doc, K.EVENT_JOINS_SOCIETY))
        assertTrue(EventDetailState.build(doc, blackout, hiddenOpen = false).changeOffered)
    }

    @Test
    fun `the field step lists the item's fields without calculated ones`() {
        val fields = EventChanges.fields(doc, EntityKind.CHARACTER, K.RIEN)
        assertTrue(fields.any { it.key == K.FIELD_AFFILIATION })
        assertTrue(fields.none { it.type.known == FieldType.CALC })
    }

    @Test
    fun `WLD-33 넣기 adds a record on the event and the detail lists it`() {
        val draft = ChangeDraft(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION, text = "등불 결사의 막내")
        val next = EventChanges.add(doc, blackout, draft, ctx)!!
        val added = next.changes.records - doc.changes.records.toSet()
        assertEquals(1, added.size)
        assertEquals(Anchor.WorldEvent(blackout), added.single().anchor)
        val detail = EventDetailState.build(next, blackout, hiddenOpen = false)
        val row = detail.changes.single { it.id == added.single().key }
        assertEquals("리엔 · 소속", row.label)
        assertTrue(row.value, row.value.endsWith("→ 등불 결사의 막내"))
    }

    @Test
    fun `an empty or invalid value writes nothing and missing parts fall back a step`() {
        assertNull(EventChanges.add(doc, blackout, ChangeDraft(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION, text = "  "), ctx))
        val height = ChangeDraft(EntityKind.CHARACTER, K.RIEN, K.FIELD_HEIGHT, text = "열")
        assertNull(EventChanges.value(EventChanges.def(doc, height)!!, height) { "k" })
        assertEquals(ChangeStep.ITEM, EventChanges.effectiveStep(doc, ChangeStep.VALUE, ChangeDraft()))
        assertEquals(ChangeStep.FIELD, EventChanges.effectiveStep(doc, ChangeStep.VALUE, ChangeDraft(EntityKind.CHARACTER, K.RIEN)))
        assertEquals(ChangeStep.VALUE, EventChanges.effectiveStep(doc, ChangeStep.VALUE, height))
        assertNull(EventChanges.add(doc, K.EVENT_JOINS_SOCIETY, ChangeDraft(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION, text = "x"), ctx))
    }
}
