package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.gallery.CardRowRules
import io.github.lenticualrstone.civilight.feature.canvas.gallery.EntryLabels
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryColumn
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryFacts
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryAccess
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryFilter
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryGrid
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryLayout
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryPanels
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryPick
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryPlacement
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryRows
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GallerySort
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryView
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryViewChoice
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryViewKind
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GridLine
import io.github.lenticualrstone.civilight.feature.canvas.gallery.HintPlace
import io.github.lenticualrstone.civilight.feature.canvas.gallery.OrderSlot
import io.github.lenticualrstone.civilight.feature.canvas.gallery.PanelButton
import io.github.lenticualrstone.civilight.feature.canvas.gallery.PanelModel
import io.github.lenticualrstone.civilight.feature.canvas.gallery.PanelPlace
import io.github.lenticualrstone.civilight.feature.canvas.gallery.PickState
import io.github.lenticualrstone.civilight.feature.canvas.gallery.ViewerNav
import io.github.lenticualrstone.civilight.feature.canvas.gallery.ViewerZoom
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.gallery.GalleryViewState
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_1
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_2
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_3
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_4
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_5
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_6
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_7
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_8
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_H1
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gallery screen rules (55 GAL-04 … GAL-09, GAL-23, GAL-25) on fixture G0 (`ExampleGallery`). */
class GalleryScreenRulesTest {
    private val g0: ProjectDoc = ExampleGallery.build()
    private val rien = ExampleKeys.RIEN

    private fun view(
        doc: ProjectDoc = g0,
        locked: Boolean = true,
        filter: GalleryFilter = GalleryFilter.All,
        sort: GallerySort = GallerySort.MANUAL,
    ): GalleryView = GalleryRows.build(doc, GalleryOwnerKind.CHARACTER, rien, locked, filter, sort)

    private fun panel(key: Key, doc: ProjectDoc = g0, locked: Boolean = true) =
        GalleryPanels(doc, locked).panel(view(doc, locked), key)!!

    private fun withScene(doc: ProjectDoc, key: Key, change: (io.github.lenticualrstone.civilight.model.story.StoryUnit) -> io.github.lenticualrstone.civilight.model.story.StoryUnit): ProjectDoc =
        doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == key) change(it) else it }))

    @Test
    fun `T-GAL-01 tap picks, scroll does not`() {
        val v = view()
        var pick = PickState()
        // Nothing picked in 카드: the hint sits under the row; a fling is not an input of the reducer at all.
        assertEquals(HintPlace.UNDER_ROW, GalleryPlacement.hint(GalleryViewKind.CARD, GalleryLayout.COVER, picked = false, grouped = false, empty = false))
        pick = GalleryPick.tap(pick, ITEM_3)
        val p = GalleryPanels(g0, true).panel(v, pick.pickedKey)!!
        assertEquals("등불 앞의 리엔", p.title)
        assertEquals("3/8", p.position)
        // Flung to card 7: card 3 is no longer laid out, so the panel has no arrow and shows `고른 카드로`;
        // card 3 stays picked (a fling is not an input of the reducer at all).
        assertNull(CardRowRules.arrowX(cardStart = null, cardWidth = 222f, rowWidth = 350f))
        // Card 3 scrolled two pitches left of the row start: its centre is off the row, still away.
        assertNull(CardRowRules.arrowX(cardStart = -2 * 236f, cardWidth = 222f, rowWidth = 350f))
        // Back at the row start: the arrow points at its centre.
        assertEquals(111f, CardRowRules.arrowX(cardStart = 0f, cardWidth = 222f, rowWidth = 350f))
        assertEquals(ITEM_3, pick.pickedKey)
    }

    @Test
    fun `T-GAL-02 tap again unpicks, another moves the pick`() {
        val picked = PickState(ITEM_3)
        assertEquals(PickState(), GalleryPick.tap(picked, ITEM_3))
        val moved = GalleryPick.tap(picked, ITEM_5)
        assertEquals(ITEM_5, moved.pickedKey)
        val p = GalleryPanels(g0, true).panel(view(), moved.pickedKey)!!
        assertEquals("대정전의 밤", p.title)
        assertEquals("5/8", p.position)
    }

    @Test
    fun `T-GAL-03 empty texts and the write button`() {
        val p = panel(ITEM_4)
        assertEquals("제목 없음", p.title)
        assertTrue(p.untitled)
        assertTrue(p.noLink)
        assertNull(p.caption)
        assertEquals(listOf(PanelButton.WRITE, PanelButton.VIEW, PanelButton.MENU), p.buttons)
        assertEquals(listOf("설명 쓰기", "보기", "메뉴"), p.buttons.map { it.label })
        assertEquals(listOf(PanelButton.VIEW, PanelButton.EDIT, PanelButton.MENU), panel(ITEM_3).buttons)
    }

    @Test
    fun `T-GAL-04 clamp per view and 더 보기 toggles`() {
        assertEquals(3, PanelModel.clampLines(GalleryViewKind.CARD))
        assertEquals(2, PanelModel.clampLines(GalleryViewKind.GRID))
        assertNull(PanelModel.clampLines(GalleryViewKind.LIST))
        assertNull(PanelModel.clampLines(GalleryViewKind.CARD, fullSurface = true))
        val open = GalleryPick.toggleExpanded(PickState(ITEM_3))
        assertTrue(open.expanded)
        assertFalse(GalleryPick.toggleExpanded(open).expanded)
        assertFalse("nothing picked: nothing to expand", GalleryPick.toggleExpanded(PickState()).expanded)
        // The full description of item 3 holds the T-GAL-19 sentence.
        assertTrue(panel(ITEM_3).captionText!!.contains(ExampleGallery.SOOT_SENTENCE))
        // A new pick starts clamped.
        assertFalse(GalleryPick.tap(open, ITEM_5).expanded)
    }

    @Test
    fun `T-GAL-05 panel under the grid row`() {
        val v = view()
        val lines = GalleryGrid.lines(v.entries, ITEM_4, lockTile = v.lockedRow != null)
        val shape = lines.map { line ->
            when (line) {
                is GridLine.Tiles -> line.entries.map { v.keys.indexOf(it.key) + 1 }.joinToString(",") + if (line.lockTile) "+lock" else ""
                is GridLine.Panel -> "panel"
            }
        }
        assertEquals(listOf("1,2,3", "4,5,6", "panel", "7,8+lock"), shape)
        val none = GalleryGrid.lines(v.entries, null, lockTile = true)
        assertTrue(none.none { it is GridLine.Panel })
        // A full last row puts the lock tile on a row of its own.
        val six = GalleryGrid.lines(v.entries.take(6), null, lockTile = true)
        assertEquals(3, six.size)
        assertTrue((six.last() as GridLine.Tiles).let { it.lockTile && it.entries.isEmpty() })
    }

    @Test
    fun `T-GAL-06 counts and hidden while locked`() {
        val v = view()
        assertEquals(listOf("전체 8", "삽화 4", "설정화 2", "소품 1"), v.chips.map { it.text })
        assertEquals("8장 · 숨김 2", v.countLine)
        assertEquals("숨긴 이미지 2", v.lockedRow?.title)
        assertEquals(OrderSlot.Count("8장 · 숨김 2"), GalleryPlacement.orderSlot(v, GalleryViewKind.GRID, HintPlace.NONE))
        assertEquals(listOf(ITEM_1, ITEM_2, ITEM_3, ITEM_4, ITEM_5, ITEM_6, ITEM_7, ITEM_8), v.keys)
        assertNull(v.unlockedBar)
    }

    @Test
    fun `T-GAL-12 scene order sorts the view, not the storage`() {
        val stored = g0.gallery.getValue(rien).items.map { it.key to it.order }
        val v = view(sort = GallerySort.SCENE)
        assertEquals(listOf(ITEM_1, ITEM_5, ITEM_7, ITEM_3, ITEM_2, ITEM_4, ITEM_6, ITEM_8), v.keys)
        assertEquals(stored, g0.gallery.getValue(rien).items.map { it.key to it.order })
        assertFalse(v.orderEditable)
        assertTrue(view().orderEditable)
        assertFalse(view(filter = GalleryFilter.Tag(ExampleGallery.TAG_ILLUSTRATION)).orderEditable)
        // 넣은 날 = newest first.
        assertEquals(ITEM_8, view(sort = GallerySort.ADDED).keys.first())
    }

    @Test
    fun `T-GAL-13 viewer ends`() {
        val keys = view().keys
        val last = ViewerNav(keys, ITEM_8)
        assertFalse(last.hasNext)
        assertTrue(last.hasPrev)
        assertEquals(ITEM_8, last.next())
        assertEquals("8/8", last.position)
        val first = ViewerNav(keys, ITEM_1)
        assertFalse(first.hasPrev)
        assertEquals(ITEM_2, first.next())
        // The item left the list (deleted while viewing): the nearest one is shown.
        assertEquals(ITEM_7, ViewerNav.settle(keys - ITEM_8, ITEM_8, 7))
        assertNull(ViewerNav.settle(emptyList(), ITEM_8, 0))
    }

    @Test
    fun `T-GAL-26 arriving with an item picks it in the remembered view`() {
        val v = view()
        val arrived = GalleryPick.arrive(PickState(), ITEM_3, v.keys)
        assertEquals(ITEM_3, arrived.pickedKey)
        assertEquals(PanelPlace.ROW_EXPANDS, GalleryPlacement.panel(GalleryViewKind.LIST, GalleryLayout.COVER))
        assertEquals(PanelPlace.UNDER_ROW, GalleryPlacement.panel(GalleryViewKind.GRID, GalleryLayout.COVER))
        assertEquals(PanelPlace.UNDER_ROW, GalleryPlacement.panel(GalleryViewKind.CARD, GalleryLayout.INNER_CARDS))
        // A key that is not visible (hidden while locked) is ignored.
        assertEquals(PickState(), GalleryPick.arrive(PickState(), ITEM_H1, v.keys))
    }

    @Test
    fun `T-GAL-30 scene link states`() {
        val withStates = withScene(withScene(g0, ExampleKeys.SCENE_1_4_2) { it.copy(hidden = true) }, ExampleKeys.SCENE_1_6_1) { it.copy(deletedAt = 1L) }
        assertEquals("숨긴 씬", panel(ITEM_5, withStates).scene?.text)
        val trashed = panel(ITEM_7, withStates).scene!!
        assertEquals("지운 씬 · 1-6 씬 1", trashed.text)
        assertTrue(trashed.dotted)
        assertFalse(trashed.tappable)
        val sorted = view(withStates, sort = GallerySort.SCENE).keys
        assertEquals(listOf(ITEM_1, ITEM_3, ITEM_2, ITEM_4, ITEM_5, ITEM_6, ITEM_7, ITEM_8), sorted)
        // Unlocked, the hidden scene reads as a live link again.
        assertEquals(EntryLabels.SceneState.LIVE, EntryLabels(withStates, locked = false).sceneState(ExampleKeys.SCENE_1_4_2))
    }

    @Test
    fun `scene line of a live scene is code and short 때`() {
        val line = panel(ITEM_3).scene!!
        assertTrue(line.tappable)
        assertEquals(ExampleKeys.SCENE_1_7_3, line.sceneKey)
        assertEquals("1-7 씬 3 · 2일째 새벽", line.text)
        assertEquals(listOf("삽화"), panel(ITEM_3).tags)
    }

    @Test
    fun `T-GAL-34 unlock shows hidden items, relock unpicks`() {
        val open = view(locked = false)
        assertEquals("전체 10", open.chips.first().text)
        assertTrue(open.entries.first { it.key == ITEM_H1 }.hiddenTag)
        assertEquals("숨긴 이미지 2 · 열림 · 앱을 떠나면 잠김", open.unlockedBar)
        assertEquals("5/10", open.position(ITEM_H1))
        val picked = GalleryPick.tap(PickState(), ITEM_H1)
        val relocked = view(locked = true)
        assertEquals(PickState(), GalleryPick.reconcile(picked, relocked.keys))
        assertEquals("전체 8", relocked.chips.first().text)
        // A visible pick survives.
        assertEquals(PickState(ITEM_3), GalleryPick.reconcile(PickState(ITEM_3), relocked.keys))
    }

    @Test
    fun `filter change that hides the pick unpicks it`() {
        val props = view(filter = GalleryFilter.Tag(ExampleGallery.TAG_PROP))
        assertEquals(listOf(ITEM_6), props.keys)
        assertEquals("1/1", props.position(ITEM_6))
        assertEquals(PickState(), GalleryPick.reconcile(PickState(ITEM_3), props.keys))
    }

    @Test
    fun `T-GAL-36 pick, panel and scroll survive through the saved state`() {
        val saved = GalleryViewState(scrollIndex = 4, scrollOffset = 12)
        val stored = GalleryPick.save(saved, PickState(ITEM_3, expanded = true))
        assertEquals(GalleryViewState(ITEM_3, true, 4, 12), stored)
        assertEquals(PickState(ITEM_3, true), PickState.of(stored))
        assertEquals(GalleryViewState(scrollIndex = 4, scrollOffset = 12), GalleryPick.save(stored, PickState()))
    }

    @Test
    fun `T-GAL-37 view per screen, default 카드`() {
        assertEquals(AppSettings.GALLERY_CHARACTER, GalleryViewChoice.screenOf(GalleryOwnerKind.CHARACTER))
        assertEquals(AppSettings.GALLERY_PROJECT, GalleryViewChoice.screenOf(GalleryOwnerKind.PROJECT))
        assertEquals(GalleryViewKind.CARD, GalleryViewChoice.of(emptyMap(), GalleryOwnerKind.CHARACTER))
        assertEquals(GalleryViewKind.GRID, GalleryViewChoice.of(mapOf(AppSettings.GALLERY_CHARACTER to ViewMode.GRID), GalleryOwnerKind.CHARACTER))
        assertEquals(GalleryViewKind.CARD, GalleryViewChoice.of(mapOf(AppSettings.GALLERY_CHARACTER to ViewMode.GRID), GalleryOwnerKind.PROJECT))
        GalleryViewKind.entries.forEach { kind ->
            val mode = GalleryViewChoice.modeOf(kind)
            assertEquals(kind, GalleryViewChoice.of(mapOf(AppSettings.GALLERY_PROJECT to mode), GalleryOwnerKind.PROJECT))
        }
    }

    @Test
    fun `T-GAL-39 hint only in 카드`() {
        val v = view()
        assertEquals(HintPlace.UNDER_ROW, GalleryPlacement.hint(GalleryViewKind.CARD, GalleryLayout.COVER, false, false, false))
        assertEquals(OrderSlot.Pager(8), GalleryPlacement.orderSlot(v, GalleryViewKind.CARD, HintPlace.UNDER_ROW))
        assertEquals(HintPlace.NONE, GalleryPlacement.hint(GalleryViewKind.GRID, GalleryLayout.COVER, false, false, false))
        assertEquals(HintPlace.NONE, GalleryPlacement.hint(GalleryViewKind.LIST, GalleryLayout.COVER, false, false, false))
        assertEquals(HintPlace.RIGHT_PANE, GalleryPlacement.hint(GalleryViewKind.LIST, GalleryLayout.INNER_SPLIT, false, false, false))
        assertEquals(HintPlace.UNDER_ROW, GalleryPlacement.hint(GalleryViewKind.CARD, GalleryLayout.INNER_CARDS, false, false, false))
        assertEquals(HintPlace.NONE, GalleryPlacement.hint(GalleryViewKind.CARD, GalleryLayout.COVER, true, false, false))
        assertEquals(HintPlace.ORDER_LINE, GalleryPlacement.hint(GalleryViewKind.CARD, GalleryLayout.COVER, false, true, false))
        assertEquals(GalleryLayout.INNER_SPLIT, GalleryLayout.of(inner = true, listDetail = true, view = GalleryViewKind.GRID))
        assertEquals(GalleryLayout.INNER_COLUMN, GalleryLayout.of(inner = true, listDetail = false, view = GalleryViewKind.LIST))
        assertEquals(GalleryLayout.INNER_CARDS, GalleryLayout.of(inner = true, listDetail = true, view = GalleryViewKind.CARD))
        assertEquals(PanelPlace.RIGHT_PANE, GalleryPlacement.panel(GalleryViewKind.LIST, GalleryLayout.INNER_SPLIT))
    }

    @Test
    fun `main image chips are derived from the portrait`() {
        val v = view()
        assertEquals("초상 · 홀로", v.entry(ITEM_3)?.chip)
        assertNull(v.entry(ITEM_1)?.chip)
        assertEquals("1-3 씬 2", v.entry(ITEM_1)?.meta)
        assertEquals("설정화", v.entry(ITEM_2)?.meta)
        assertNull(v.entry(ITEM_4)?.meta)
    }

    @Test
    fun `GAL-25 many cards use the readout and an empty gallery is empty`() {
        val v = view()
        assertEquals("41 · 128", CardRowRules.readout(40, 128))
        assertEquals(OrderSlot.Pager(8), GalleryPlacement.orderSlot(v, GalleryViewKind.CARD, HintPlace.NONE))
        val bare = ExampleGallery.build().let { it.copy(gallery = it.gallery - rien) }
        val empty = view(bare)
        assertTrue(empty.isEmpty)
        assertFalse(empty.filteredEmpty)
        assertEquals(HintPlace.NONE, GalleryPlacement.hint(GalleryViewKind.CARD, GalleryLayout.COVER, false, false, empty = true))
        // The last item of a chosen tag gone: the chip stays at 0 and the filtered-empty line shows.
        val noProps = g0.copy(gallery = g0.gallery + (rien to g0.gallery.getValue(rien).let { f -> f.copy(items = f.items.filterNot { it.key == ITEM_6 }) }))
        val props = view(noProps, filter = GalleryFilter.Tag(ExampleGallery.TAG_PROP))
        assertTrue(props.filteredEmpty)
        assertTrue(props.chips.any { it.text == "소품 0" })
    }

    @Test
    fun `owner access is decided before anything is drawn`() {
        val hiddenRien = g0.copy(cast = g0.cast.copy(characters = g0.cast.characters.map { if (it.key == rien) it.copy(hidden = true) else it }))
        val locked = view(hiddenRien)
        assertEquals(GalleryAccess.OWNER_LOCKED, locked.access)
        assertEquals("", locked.ownerName)
        assertTrue(locked.keys.isEmpty())
        assertEquals(GalleryAccess.OPEN, view(hiddenRien, locked = false).access)
        val trashed = g0.copy(cast = g0.cast.copy(characters = g0.cast.characters.map { if (it.key == rien) it.copy(deletedAt = 1L) else it }))
        assertEquals(GalleryAccess.OWNER_GONE, view(trashed).access)
    }

    @Test
    fun `project gallery groups characters`() {
        val v = GalleryRows.build(g0, GalleryOwnerKind.PROJECT, g0.key, true, GalleryFilter.All, GallerySort.MANUAL)
        assertTrue(v.grouped)
        val rienSection = v.sections.single { it.ownerLink == rien }
        assertEquals("리엔 · 8", rienSection.header)
        assertEquals(listOf("전체 8", "프로젝트 0", "인물 8"), v.chips.take(3).map { it.text })
        assertEquals(HintPlace.ORDER_LINE, GalleryPlacement.hint(GalleryViewKind.CARD, GalleryLayout.COVER, false, v.grouped, false))
    }

    @Test
    fun `viewer zoom stays in bounds`() {
        assertEquals(4f, ViewerZoom.scale(3f, 2f))
        assertEquals(1f, ViewerZoom.scale(1f, 0.5f))
        assertEquals(0f, ViewerZoom.pan(0f, 50f, 300f, 400f, 1f))
        assertEquals(100f, ViewerZoom.pan(0f, 500f, 400f, 400f, 1.5f))
        assertEquals(-100f, ViewerZoom.pan(0f, -500f, 400f, 400f, 1.5f))
    }

    @Test
    fun `panel arrow follows the card and hides when away`() {
        assertEquals(111f, CardRowRules.arrowX(0f, 222f, 390f))
        assertNull(CardRowRules.arrowX(null, 222f, 390f))
        assertNull(CardRowRules.arrowX(-300f, 222f, 390f))
        assertEquals(24f, CardRowRules.arrowX(-100f, 222f, 390f))
    }

    @Test
    fun `menu on another card picks it`() {
        assertEquals(PickState(ITEM_5), GalleryPick.menu(PickState(ITEM_3, true), ITEM_5))
        assertEquals(PickState(ITEM_3, true), GalleryPick.menu(PickState(ITEM_3, true), ITEM_3))
    }

    @Test
    fun `T-GAL-26 the picked row or grid line is found in the column`() {
        val v = view()
        // Chips and order line come first: row 3 of 목록 is column item 4.
        assertEquals(4, GalleryColumn.indexOfItem(ITEM_3, v, GalleryViewKind.LIST, GalleryLayout.COVER))
        // 격자: items 1…3 are line 0 (column 2), items 4…6 line 1 (column 3), items 7, 8 line 2 (column 4); the
        // scrolled-to item is the picked one, so its panel line comes after its own row and never shifts it.
        assertEquals(2, GalleryColumn.indexOfItem(ITEM_3, v, GalleryViewKind.GRID, GalleryLayout.COVER))
        assertEquals(3, GalleryColumn.indexOfItem(ITEM_4, v, GalleryViewKind.GRID, GalleryLayout.COVER))
        assertEquals(4, GalleryColumn.indexOfItem(ITEM_8, v, GalleryViewKind.GRID, GalleryLayout.COVER))
        assertEquals(4, GalleryColumn.indexOfItem(ITEM_8, v, GalleryViewKind.GRID, GalleryLayout.INNER_SPLIT))
        assertNull(GalleryColumn.indexOfItem(ITEM_3, v, GalleryViewKind.CARD, GalleryLayout.COVER))
        assertNull(GalleryColumn.indexOfItem(ITEM_H1, v, GalleryViewKind.LIST, GalleryLayout.COVER))
        // The unlocked bar adds one line before the rows.
        val open = view(locked = false)
        assertEquals(3 + 4, GalleryColumn.indexOfItem(ITEM_H1, open, GalleryViewKind.LIST, GalleryLayout.COVER))
    }

    @Test
    fun `original facts read as the picker shows them`() {
        assertEquals("4.2 MB", GalleryFacts.size(4_200_000L))
        assertEquals("820 KB", GalleryFacts.size(820_000L))
        assertEquals("1 KB", GalleryFacts.size(14L))
        assertEquals("11-20", GalleryFacts.monthDay(1_763_629_200_000L, java.time.ZoneId.of("Asia/Seoul")))
    }
}
