package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.ui.picker.LinkCreate
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.picker.PickerCopy
import io.github.lenticualrstone.civilight.ui.picker.PickerCreateCards
import io.github.lenticualrstone.civilight.ui.picker.PickerCreateCodec
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0, U-15.3) The picker create card and the `@` picker's kind in view. */
class PickerCreateTest {
    private val doc = ExampleProject.build()

    @Test
    fun `the card opens with the query as the name only when it matched nothing`() {
        assertEquals("세라2", PickerCreateCards.open(CreateKind.CHARACTER, " 세라2 ", matched = false).name)
        assertEquals("", PickerCreateCards.open(CreateKind.CHARACTER, "세라", matched = true).name)
        assertEquals("", PickerCreateCards.open(CreateKind.PLACE, "  ", matched = false).name)
        assertEquals(CreateCardMode.PICKER, PickerCreateCards.open(CreateKind.PLACE, "", true).mode)
    }

    @Test
    fun `fields have a working control each - no image`() {
        assertEquals(listOf("name", "displayName", "oneLine", "tags"), PickerCreateCards.fields(CreateKind.CHARACTER).map { it.key })
        assertEquals(listOf("name", "parent", "oneLine"), PickerCreateCards.fields(CreateKind.PLACE).map { it.key })
        assertEquals(listOf("name", "aliases", "oneLine"), PickerCreateCards.fields(CreateKind.TERM).map { it.key })
        assertEquals(listOf("name", "oneLine"), PickerCreateCards.fields(CreateKind.FACTION).map { it.key })
    }

    @Test
    fun `values are trimmed, tags toggle, and the open card survives the codec`() {
        var card = PickerCreateCards.open(CreateKind.CHARACTER, "하르트2", matched = false)
            .with(CreateFields.displayName.key, "  ")
            .with(CreateFields.oneLineIntro.key, " 검사 ")
        card = PickerCreateCards.toggleTag(PickerCreateCards.toggleTag(card, "a"), "b")
        card = PickerCreateCards.toggleTag(card, "a")
        val v = PickerCreateCards.values(card)
        assertEquals("하르트2", v.name)
        assertNull(v.displayName)
        assertEquals("검사", v.note)
        assertEquals(listOf("b"), v.tagKeys)
        assertEquals(card, PickerCreateCodec.decode(PickerCreateCodec.encode(card)))
        assertNull(PickerCreateCodec.decode(""))
        val place = PickerCreateCards.withParent(PickerCreateCards.open(CreateKind.PLACE, "", true), "P1")
        assertEquals("P1", PickerCreateCards.values(place).parentKey)
        assertFalse(PickerCreateCards.canCreate(PickerCreateCards.open(CreateKind.PLACE, "", true)))
    }

    @Test
    fun `the @ picker makes the kind of its chip, people under 전체`() {
        val kinds = PickerModels.SCRIPT_LINK_KINDS
        assertEquals(listOf(PickerCopy.ALL, PickerCopy.KIND_CHARACTER, PickerCopy.KIND_TERM, PickerCopy.KIND_PLACE, PickerCopy.KIND_EVENT), LinkCreate.chips(kinds).map { it.label })
        assertEquals(CreateKind.CHARACTER, LinkCreate.kindInView(null, kinds))
        assertEquals(CreateKind.TERM, LinkCreate.kindInView(PickKind.TERM.name, kinds))
        assertEquals(CreateKind.PLACE, LinkCreate.kindInView(PickKind.PLACE.name, kinds))
        assertEquals(PickKind.PLACE, LinkCreate.pickKind(CreateKind.PLACE))
        assertNull(LinkCreate.pickKind(CreateKind.TAG))
    }

    @Test
    fun `a trashed tag makes no subtitle, no match and no card chip`() {
        val rien = doc.index.character(ExampleKeys.RIEN)!!
        val tagKey = rien.tagKeys.first()
        val tagName = doc.index.tag(tagKey)!!.name
        val trashed = doc.copy(project = doc.project.copy(tags = doc.project.tags.map { if (it.key == tagKey) it.copy(deletedAt = 1L) else it }))
        assertTrue(PickerModels.liveTagNames(doc, rien).contains(tagName))
        assertFalse(PickerModels.liveTagNames(trashed, rien).contains(tagName))
        assertFalse(PickerCreateCards.liveTags(trashed).any { it.key == tagKey })
    }
}
