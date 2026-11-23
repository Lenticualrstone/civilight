package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.picker.PickerCopy
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerModelsTest {
    private val doc = ExampleProject.build()

    private fun hide(doc: ProjectDoc, characterKey: String) =
        doc.copy(cast = doc.cast.copy(characters = doc.characters.map { if (it.key == characterKey) it.copy(hidden = true) else it }))

    @Test
    fun `speaker picker sections - in scene, recent, the rest by name`() {
        val model = PickerModels.characters(
            doc,
            sceneCast = listOf(ExampleKeys.HART, ExampleKeys.RIEN),
            recent = listOf(ExampleKeys.RIEN, ExampleKeys.SERA, ExampleKeys.OWEN),
        )
        assertEquals(listOf(PickerCopy.IN_SCENE, PickerCopy.RECENT, PickerCopy.ALL), model.sections.map { it.title })
        assertEquals(listOf("하르트", "리엔"), model.sections[0].items.map { it.title })
        assertEquals(listOf("세라", "오웬"), model.sections[1].items.map { it.title })
        val rest = model.sections[2].items.map { it.title }
        assertEquals(rest.sorted(), rest)
        // 11 live characters; 노아 is in the trash and never listed.
        assertEquals(11, model.items.size)
        assertFalse(model.items.any { it.key == ExampleKeys.NOAH })
        assertTrue(model.items.all { it.avatar })
    }

    @Test
    fun `tags filter and form the sub line`() {
        val lead = PickerModels.characters(doc, tagKey = ExampleKeys.TAG_LEAD)
        assertEquals(setOf("리엔", "하르트", "세라"), lead.items.map { it.title }.toSet())
        val rien = PickerModels.characters(doc).items.single { it.key == ExampleKeys.RIEN }
        assertTrue(rien.subtitle!!.contains("주연"))
        assertTrue(rien.subtitle!!.contains(" · "))
    }

    @Test
    fun `initial-consonant search over names and tags`() {
        assertEquals(listOf("하르트"), PickerModels.characters(doc, query = "ㅎㄹㅌ").items.map { it.title })
        val byTag = PickerModels.characters(doc, query = "주연").items.map { it.title }.toSet()
        assertEquals(setOf("리엔", "하르트", "세라"), byTag)
    }

    @Test
    fun `locked hidden characters are counted in the last row - I-06`() {
        val hidden = hide(doc, ExampleKeys.HART)
        val locked = PickerModels.characters(hidden)
        assertFalse(locked.items.any { it.key == ExampleKeys.HART })
        assertEquals(1, locked.hiddenCount)
        assertEquals("숨긴 인물 1", PickerCopy.hiddenRow(locked.hiddenKindWord, locked.hiddenCount))
        val open = PickerModels.characters(hidden, hiddenOpen = true)
        assertTrue(open.items.any { it.key == ExampleKeys.HART })
        assertEquals(0, open.hiddenCount)
    }

    @Test
    fun `a deleted current speaker gets the top row - rule 20`() {
        val model = PickerModels.characters(doc, currentSpeakerKey = ExampleKeys.NOAH)
        assertEquals("노아", model.deleted?.name)
        assertNull(PickerModels.characters(doc, currentSpeakerKey = ExampleKeys.RIEN).deleted)
    }

    @Test
    fun `term aliases insert the alias - SCR-55`() {
        val hit = PickerModels.terms(doc, query = "빛돌").items.single()
        assertEquals(ExampleKeys.TERM_ORIGINIUM_LANTERN, hit.key)
        assertEquals("빛돌", hit.insertText)
        assertEquals("오리지늄 등불의 별칭", hit.subtitle)
        val byName = PickerModels.terms(doc, query = "오리지늄").items.single()
        assertEquals("오리지늄 등불", byName.insertText)
    }

    @Test
    fun `places and regions, links across kinds`() {
        val places = PickerModels.places(doc)
        assertTrue(places.items.any { it.kind == PickKind.PLACE })
        val first = places.items.first()
        assertEquals(PlaceRefKind.PLACE, PickerModels.placeRefKind(first))
        val links = PickerModels.links(doc, query = "리엔")
        assertEquals(PickerCopy.KIND_CHARACTER, links.sections.first().title)
        assertEquals(ExampleKeys.RIEN, links.items.first().key)
    }

    @Test
    fun `unit rows are indented and skip the trash`() {
        val all = PickerModels.units(doc)
        val titles = all.items.map { it.title }
        assertTrue("Ⅰ 1장 · 등불지기" in titles)
        assertTrue("1-7 씬 3 · 꺼진 등불" in titles)
        assertFalse(all.items.any { it.key == ExampleKeys.SCENE_1_6_5 })
        assertEquals(0, all.items.first().depth)
        val scenesOnly = PickerModels.units(doc, levels = setOf(doc.project.structure.sceneLevel))
        assertEquals(34, scenesOnly.items.size)
        assertTrue(scenesOnly.items.all { it.depth == 0 })
    }

    @Test
    fun `a locked hidden scene keeps its place with the title masked - I-02`() {
        val scene4 = ExampleKeys.scene(1, 7, 4)
        val hiddenDoc = doc.copy(story = doc.story.copy(units = doc.units.map { if (it.key == scene4) it.copy(hidden = true, title = "비밀") else it }))
        val locked = PickerModels.units(hiddenDoc).items.single { it.key == scene4 }
        assertEquals("1-7 씬 4 · 숨긴 씬", locked.title)
        val open = PickerModels.units(hiddenDoc, hiddenOpen = true).items.single { it.key == scene4 }
        assertEquals("1-7 씬 4 · 비밀", open.title)
    }

    @Test
    fun `event picker lists live events in timeline order with the scene of a story event`() {
        val events = PickerModels.events(doc)
        val keys = events.items.map { it.key }
        assertTrue(ExampleKeys.EVENT_BLACKOUT in keys)
        assertTrue(ExampleKeys.EVENT_JOINS_SOCIETY in keys)
        assertTrue(events.items.all { it.kind == PickKind.EVENT })
        assertEquals("1-3 씬 2", events.items.first { it.key == ExampleKeys.EVENT_JOINS_SOCIETY }.subtitle)
        assertNull(events.items.first { it.key == ExampleKeys.EVENT_BLACKOUT }.subtitle)
        val trashed = doc.copy(world = doc.world.copy(events = doc.world.events.map { if (it.key == ExampleKeys.EVENT_BLACKOUT) it.copy(deletedAt = 1L) else it }))
        assertFalse(PickerModels.events(trashed).items.any { it.key == ExampleKeys.EVENT_BLACKOUT })
        assertEquals(listOf(ExampleKeys.EVENT_BLACKOUT), PickerModels.events(doc, "대정").items.map { it.key })
    }

    @Test
    fun `document link picker adds factions and equals the script picker for the script kinds`() {
        assertEquals(PickerModels.links(doc, "", false), PickerModels.links(doc, "", false, PickerModels.SCRIPT_LINK_KINDS))
        val document = PickerModels.links(doc, "", false, PickerModels.DOCUMENT_LINK_KINDS)
        assertEquals(PickerCopy.KIND_FACTION, document.sections.last().title)
        assertTrue(document.items.any { it.kind == PickKind.FACTION })
        val onlyFactions = PickerModels.links(doc, "", false, setOf(PickKind.FACTION))
        assertTrue(onlyFactions.items.all { it.kind == PickKind.FACTION })
    }

    @Test
    fun `U-15_3 picker create row reads the query only when nothing matches`() {
        val all = PickerModels.characters(doc, sceneCast = emptyList(), recent = emptyList())
        val none = io.github.lenticualrstone.civilight.ui.picker.PickerModel(sections = emptyList())
        assertTrue(io.github.lenticualrstone.civilight.ui.picker.PickerCreate.matched("", none))
        assertTrue(io.github.lenticualrstone.civilight.ui.picker.PickerCreate.matched("리", all))
        assertFalse(io.github.lenticualrstone.civilight.ui.picker.PickerCreate.matched("세라라", none))
        val kind = io.github.lenticualrstone.civilight.designsystem.component.CreateKind.CHARACTER
        assertEquals("‘세라라’ 새 인물", io.github.lenticualrstone.civilight.designsystem.component.CreateCard.rowLabel(kind, "세라라", matched = false))
    }
}
