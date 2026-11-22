package io.github.lenticualrstone.civilight.feature.world.list

import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.feature.world.common.CardImageUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.navigation.WorldTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** `목록 | 카드` (WLD-05) and the card image slot (WLD-07). */
class WorldCardRowsTest {
    @Test
    fun `WLD-05 the card view keeps the list's sections, one row each`() {
        val doc = WorldTestFixture.stateA()
        val ui = WorldListState.build(doc, WorldTab.TERMS, WorldTestFixture.index(doc), hiddenOpen = false)
        val rows = WorldCardRows.build(doc, ui.items)
        assertEquals(listOf("공개됨 · 최신 · 1-7", "아직 공개 전"), rows.map { it.title })
        val t1 = rows.first().cards.single { it.key == T1 }
        assertEquals("오리지늄 등불", t1.name)
        assertEquals("첫 공개 1-7 · ◇ 후보 1", t1.label)
        assertEquals(CardImageUi.Initial("오"), t1.image)
        assertEquals(ui.items.count { it is WorldListItem.Row }, rows.sumOf { it.cards.size })
    }

    @Test
    fun `WLD-05 the view is remembered per screen`() {
        val start = mapOf("library" to ViewMode.LIST)
        assertFalse(WorldCardRows.showCards(start, WorldTab.TERMS))
        val cards = WorldCardRows.withView(start, WorldTab.PLACES, cards = true)
        assertTrue(WorldCardRows.showCards(cards, WorldTab.PLACES))
        assertFalse(WorldCardRows.showCards(cards, WorldTab.TERMS))
        assertEquals(ViewMode.LIST, cards["library"])
        assertEquals(ViewMode.LIST, WorldCardRows.withView(cards, WorldTab.PLACES, cards = false)["world.places"])
    }

    @Test
    fun `WLD-07 the slot reads the item's card reference image`() {
        val doc = WorldTestFixture.stateA()
        assertEquals(CardImageUi.Initial("오"), CardImages.of(doc, WorldKind.TERM, T1, "오리지늄 등불"))
        fun withUse(source: String) = doc.copy(
            assets = doc.assets.copy(
                imageUses = doc.assets.imageUses + ImageUse(
                    key = Keys.newKey(), ownerKind = ImageOwnerKind.TERM.open(), ownerKey = T1,
                    slot = ImageSlot.CARD_REFERENCE.open(), source = source, holo = true,
                ),
            ),
        )
        assertEquals(CardImageUi.Preset("lantern"), CardImages.of(withUse("preset:lantern"), WorldKind.TERM, T1, "오리지늄 등불"))
        assertEquals(CardImageUi.Picked("abc123", null, holo = true), CardImages.of(withUse("abc123"), WorldKind.TERM, T1, "오리지늄 등불"))
        val sheet = CardImages.pickSheet(WorldTestFixture.PROJECT, WorldKind.TERM, T1)
        assertEquals(ImageOwnerKind.TERM, sheet.ownerKind)
        assertEquals(ImageSlot.CARD_REFERENCE, sheet.slot)
    }
}
