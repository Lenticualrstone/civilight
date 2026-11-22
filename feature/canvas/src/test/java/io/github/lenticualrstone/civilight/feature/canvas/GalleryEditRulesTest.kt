package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.domain.actions.GalleryActions
import io.github.lenticualrstone.civilight.domain.actions.GalleryAddSource
import io.github.lenticualrstone.civilight.domain.actions.GalleryMove
import io.github.lenticualrstone.civilight.domain.actions.GalleryOwner
import io.github.lenticualrstone.civilight.domain.actions.GallerySourceMark
import io.github.lenticualrstone.civilight.feature.canvas.common.actions
import io.github.lenticualrstone.civilight.feature.canvas.gallery.EntryLabels
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryAddModel
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryCardSizes
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryCopyTargets
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryFilter
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryInfo
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryMenus
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryOrder
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryRows
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GallerySort
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryUiCopy
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryView
import io.github.lenticualrstone.civilight.feature.canvas.gallery.MainPlan
import io.github.lenticualrstone.civilight.feature.canvas.gallery.MainTarget
import io.github.lenticualrstone.civilight.feature.canvas.gallery.MultiPick
import io.github.lenticualrstone.civilight.feature.canvas.gallery.MyImages
import io.github.lenticualrstone.civilight.feature.canvas.gallery.OrderState
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.CropTarget
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.PixelSize
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_1
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_2
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_3
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_4
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_5
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_6
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_H1
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gallery part 2 rules (55 GAL-10 … GAL-20) on fixture G0 (`ExampleGallery`). */
class GalleryEditRulesTest {
    private val g0: ProjectDoc = ExampleGallery.build()
    private val rien = ExampleKeys.RIEN
    private val owner = GalleryOwner.character(rien)
    private fun act() = CanvasFx.ctx().actions()

    private fun view(doc: ProjectDoc = g0, locked: Boolean = true, filter: GalleryFilter = GalleryFilter.All, sort: GallerySort = GallerySort.MANUAL): GalleryView =
        GalleryRows.build(doc, GalleryOwnerKind.CHARACTER, rien, locked, filter, sort)

    private fun positions(doc: ProjectDoc, locked: Boolean = true): List<Key> = view(doc, locked).keys

    @Test
    fun `T-GAL-07 main image is derived from the slot`() {
        val item3 = g0.index.galleryItem(ITEM_3)!!
        val labels = EntryLabels(g0, locked = true)
        assertEquals("초상 · 홀로", labels.chip(item3))
        val rows3 = GalleryMenus.mainRows(g0, item3)
        assertEquals(listOf(MainTarget.PORTRAIT, MainTarget.BACKGROUND), rows3.map { it.target })
        assertTrue(rows3.first().current)
        assertEquals("지금 초상", rows3.first().target.now)
        // `초상으로` on item 1, applied in the crop editor: the portrait use takes item 1's source; no gallery row changes.
        val item1 = g0.index.galleryItem(ITEM_1)!!
        val plan = GalleryMenus.mainPlan(item1, MainTarget.PORTRAIT) as MainPlan.Crop
        val after = ImageEdits.placeBlob(g0, plan.owner, plan.blobHash, emptyMap(), plan.holo, CanvasFx.ctx())
        val labelsAfter = EntryLabels(after, locked = true)
        assertEquals("초상", labelsAfter.chip(after.index.galleryItem(ITEM_1)!!))
        assertEquals("홀로", labelsAfter.chip(after.index.galleryItem(ITEM_3)!!))
        assertTrue(GalleryMenus.mainRows(after, after.index.galleryItem(ITEM_1)!!).first().current)
        assertEquals(g0.gallery, after.gallery)
    }

    @Test
    fun `T-GAL-08 the main-image plan writes nothing before the crop editor applies`() {
        val item3 = g0.index.galleryItem(ITEM_3)!!
        val plan = GalleryMenus.mainPlan(item3, MainTarget.BACKGROUND)
        assertTrue(plan is MainPlan.Crop)
        plan as MainPlan.Crop
        assertEquals(ImageSlot.CHARACTER_BACKGROUND, plan.owner.slot)
        assertEquals(item3.source, plan.blobHash)
        assertNull(plan.note)
        // Cancelling the editor = never calling `placeBlob`: the background use stays as it was.
        assertNull(ImageEdits.use(g0, plan.owner))
        // Applied, the background never carries holo (CNV-25), whatever the item's flag.
        val applied = ImageEdits.placeBlob(g0, plan.owner, plan.blobHash, emptyMap(), plan.holo, CanvasFx.ctx())
        assertFalse(ImageEdits.use(applied, plan.owner)!!.holo)
        // A preset item goes in at once (presets are not cropped).
        val preset = item3.copy(source = PresetImages.all.first().source)
        assertTrue(GalleryMenus.mainPlan(preset, MainTarget.PORTRAIT) is MainPlan.PlacePreset)
    }

    @Test
    fun `T-GAL-09 add order and duplicates`() {
        val presets = PresetImages.all
        val b = presets[1].source
        val a = presets[0].source
        val c = presets[2].source
        var chosen = emptyList<String>()
        val model0 = GalleryAddModel.build(g0, owner, locked = true, chosen = chosen)
        for (s in listOf(b, a, c)) chosen = GalleryAddModel.toggle(chosen, model0.presets.first { it.source == s })
        // Item 3's source is a `내 이미지` tile marked `이미 있음`; tapping it changes nothing.
        val mine3 = model0.mine.first { it.source == ExampleGallery.hashOf(3) }
        assertEquals(GallerySourceMark.ALREADY_THERE, mine3.mark)
        assertEquals("이미 있음", mine3.mark.label)
        assertEquals(chosen, GalleryAddModel.toggle(chosen, mine3))
        val model = GalleryAddModel.build(g0, owner, true, chosen)
        assertEquals(listOf(1, 2, 3), listOf(b, a, c).map { s -> model.presets.first { it.source == s }.number })
        assertEquals("넣기 · 3", model.button)
        assertEquals("리엔 · 여러 장 · 고른 순서대로", model.subtitle)
        val result = GalleryActions.add(g0, owner, GalleryAddModel.sources(g0, model.chosen), act(), locked = true)
        assertEquals("3장 넣음", result.toast.message)
        val keys = positions(result.doc)
        assertEquals(11, keys.size)
        assertEquals(result.added, keys.subList(8, 11))
        assertEquals(listOf(b, a, c), result.added.map { result.doc.index.galleryItem(it)!!.source })
        assertEquals(1, result.doc.galleryItems.count { it.source == ExampleGallery.hashOf(3) && it.ownerKey == rien })
    }

    @Test
    fun `T-GAL-11 order buttons`() {
        val v = view()
        val item3 = g0.index.galleryItem(ITEM_3)!!
        val row = GalleryMenus.orderRow(g0, v, item3, "3/8", locked = true)
        assertTrue(row.editable && row.canForward && row.canBackward && row.canFirst)
        assertEquals("3/8 · 직접 순서", row.text)
        val forward = GalleryActions.move(g0, ITEM_3, GalleryMove.FORWARD, act(), locked = true)!!.doc
        assertEquals(listOf(ITEM_1, ITEM_3, ITEM_2), positions(forward).take(3))
        val first = GalleryActions.move(forward, ITEM_3, GalleryMove.FIRST, act(), locked = true)!!.doc
        assertEquals(ITEM_3, positions(first).first())
        val row1 = GalleryMenus.orderRow(first, view(first), first.index.galleryItem(ITEM_3)!!, "1/8", true)
        assertFalse(row1.canForward)
        assertFalse(row1.canFirst)
        // One undo step each: reverting the last change brings back the state before it.
        val change = GalleryActions.move(forward, ITEM_3, GalleryMove.FIRST, act(), locked = true)!!.change
        assertEquals(positions(forward), positions(GalleryActions.undo(first, change)))
    }

    @Test
    fun `T-GAL-12 and T-GAL-24 order controls need 직접 순서 and 전체`() {
        val scene = view(sort = GallerySort.SCENE)
        val item = g0.index.galleryItem(ITEM_3)!!
        val sceneRow = GalleryMenus.menu(g0, scene, ITEM_3, true)!!.order
        assertFalse(sceneRow.editable || sceneRow.canForward || sceneRow.canBackward || sceneRow.canFirst)
        assertTrue(sceneRow.text.endsWith(" · 씬 순서"))
        val added = GalleryMenus.orderRow(g0, view(sort = GallerySort.ADDED), item, "3/8", true)
        assertEquals("3/8 · 넣은 날", added.text)
        val tag = view(filter = GalleryFilter.Tag(ExampleGallery.TAG_ILLUSTRATION))
        val tagRow = GalleryMenus.menu(g0, tag, ITEM_3, true)!!.order
        assertFalse(tagRow.editable)
        assertTrue(tagRow.text.endsWith(" · 전체에서만"))
    }

    @Test
    fun `T-GAL-24 앞으로 skips a hidden neighbour while locked`() {
        val moved = GalleryActions.move(g0, ITEM_5, GalleryMove.FORWARD, act(), locked = true)!!.doc
        val keys = positions(moved)
        assertEquals(listOf(ITEM_3, ITEM_5, ITEM_4), keys.subList(2, 5))
        assertEquals("4/8", view(moved).position(ITEM_5))
    }

    @Test
    fun `T-GAL-14 delete toast and chips`() {
        val result = GalleryActions.delete(g0, listOf(ITEM_6), act())!!
        assertEquals("이미지 지움 · 등불 고리", result.toast.message)
        val v = view(result.doc)
        assertEquals("전체 7", v.chips.first().text)
        assertTrue(v.chips.none { it.text.startsWith("소품") })
        val back = GalleryActions.undo(result.doc, result.change)
        assertEquals(ITEM_6, positions(back)[5])
    }

    @Test
    fun `T-GAL-20 a hidden source never shows in 내 이미지 and is skipped silently`() {
        val h1 = ExampleGallery.hashOf(91)
        assertTrue(MyImages.visible(g0, locked = true).none { it.hash == h1 })
        assertTrue(MyImages.visible(g0, locked = false).any { it.hash == h1 })
        assertTrue(GalleryAddModel.build(g0, owner, true, emptyList()).mine.none { it.source == h1 })
        // The same file chosen through `사진` with one new photo: one added, nothing named.
        val result = GalleryActions.add(g0, owner, listOf(GalleryAddSource(h1, g0.index.blob(h1)), GalleryAddSource("f".repeat(64))), act(), locked = true)
        assertEquals(1, result.added.size)
        assertEquals("1장 넣음", result.toast.message)
        // An original nothing uses yet stays listed.
        assertTrue(MyImages.maskedHashes(g0).none { it == ExampleGallery.hashOf(1) })
    }

    @Test
    fun `T-GAL-21 a trashed item still holds its source`() {
        val deleted = GalleryActions.delete(g0, listOf(ITEM_6), act())!!.doc
        val h6 = ExampleGallery.hashOf(6)
        val tile = GalleryAddModel.build(deleted, owner, true, emptyList()).mine.first { it.source == h6 }
        assertEquals(GallerySourceMark.IN_TRASH, tile.mark)
        assertEquals("휴지통에 있음", tile.mark.label)
        assertFalse(tile.choosable)
        val again = GalleryActions.add(deleted, owner, listOf(GalleryAddSource(h6)), act(), locked = true)
        assertEquals("0장 넣음 · 이미 있음 1", again.toast.message)
    }

    @Test
    fun `T-GAL-22 reuse copies every field and marks owners that hold the source`() {
        val targets = GalleryCopyTargets.targets(g0, listOf(ITEM_3), locked = true)
        assertEquals(GalleryOwnerKind.PROJECT, targets.first().owner.kind)
        assertEquals(GallerySourceMark.ALREADY_THERE, targets.first { it.owner.key == rien }.mark)
        assertEquals(GallerySourceMark.NONE, targets.first().mark)
        val project = GalleryOwner.project(g0)
        val copied = GalleryActions.copyTo(g0, listOf(ITEM_H1), project, act(), locked = false)
        val from = g0.index.galleryItem(ITEM_H1)!!
        val to = copied.doc.index.galleryItem(copied.added.single())!!
        assertEquals(listOf(from.source, from.title, from.caption, from.tagKeys, from.sceneKey, from.crops, from.holo, true), listOf(to.source, to.title, to.caption, to.tagKeys, to.sceneKey, to.crops, to.holo, to.hidden))
        val locked = GalleryRows.build(copied.doc, GalleryOwnerKind.PROJECT, g0.key, true, GalleryFilter.All, GallerySort.MANUAL)
        assertTrue(to.key !in locked.keys)
    }

    @Test
    fun `T-GAL-23 a hidden item as main image says what becomes visible`() {
        val unlocked = view(locked = false)
        val menu = GalleryMenus.menu(g0, unlocked, ITEM_H1, locked = false)!!
        assertEquals("숨긴 이미지 · 초상에서는 보임", menu.main.first().sub)
        assertTrue(menu.hidden)
        val plan = GalleryMenus.mainPlan(g0.index.galleryItem(ITEM_H1)!!, MainTarget.PORTRAIT) as MainPlan.Crop
        assertEquals("숨긴 이미지 · 초상에서는 보임", plan.note)
        val applied = ImageEdits.placeBlob(g0, plan.owner, plan.blobHash, emptyMap(), plan.holo, CanvasFx.ctx())
        assertEquals(ExampleGallery.hashOf(91), ImageEdits.use(applied, plan.owner)!!.source)
        assertTrue(applied.index.galleryItem(ITEM_H1)!!.hidden)
        // Locked, the hidden item has no menu at all.
        assertNull(GalleryMenus.menu(g0, view(), ITEM_H1, locked = true))
    }

    @Test
    fun `T-GAL-25 옮기기 for 128 items`() {
        val sources = (9..128).map { GalleryAddSource("h" + it.toString().padStart(63, '0')) }
        val big = GalleryActions.add(g0, owner, sources, act(), locked = true).doc
        val rows = GalleryOrder.rows(big, rien, locked = true)
        assertEquals(128, rows.size)
        var state = GalleryOrder.tap(OrderState(), ITEM_3)
        state = GalleryOrder.startMove(state)
        assertEquals(ITEM_3, state.movingKey)
        // Taps on rows do nothing while moving; nothing moves until a target is chosen.
        assertEquals(state, GalleryOrder.tap(state, ITEM_1))
        val target = rows[79].key
        val edit = GalleryActions.moveAfter(big, state.movingKey!!, target, act(), locked = true)!!
        state = GalleryOrder.placed(state)
        assertEquals(OrderState(ITEM_3, null), state)
        val after = GalleryOrder.rows(edit.doc, rien, locked = true)
        assertEquals(80, after.first { it.key == ITEM_3 }.position)
        assertEquals(target, after[78].key)
        assertEquals(rows.map { it.key }, GalleryOrder.rows(GalleryActions.undo(edit.doc, edit.change), rien, true).map { it.key })
        // `취소` keeps the pick and drops the move.
        assertEquals(OrderState(ITEM_3, null), GalleryOrder.cancel(OrderState(ITEM_3, ITEM_3)))
    }

    @Test
    fun `T-GAL-32 presets read 기본 이미지 and cannot be cropped`() {
        val preset = PresetImages.all.first { it.name == "안개 숲" }
        val added = GalleryActions.add(g0, owner, listOf(GalleryAddSource(preset.source)), act(), locked = true)
        val item = added.doc.index.galleryItem(added.added.single())!!
        val v = view(added.doc)
        assertEquals("기본 이미지", GalleryInfo.head(added.doc, item, v).facts)
        assertEquals("리엔 · 갤러리 9/9 · 원본 그대로", GalleryInfo.head(added.doc, item, v).small)
        val tile = GalleryAddModel.build(g0, owner, true, emptyList()).presets.first { it.source == preset.source }
        assertEquals("안개 숲", tile.caption)
        val menu = GalleryMenus.menu(added.doc, v, item.key, true)!!
        assertFalse(menu.cropEnabled)
        assertEquals("기본 이미지 · 자르기 없음", menu.cropSub)
    }

    @Test
    fun `info head of an original`() {
        val item3 = g0.index.galleryItem(ITEM_3)!!
        val head = GalleryInfo.head(g0, item3, view())
        assertTrue(head.facts.startsWith("1200 × 1600 · "))
        assertTrue(head.facts.endsWith(" · 사진"))
        assertEquals("리엔 · 갤러리 3/8 · 원본 그대로", head.small)
        assertEquals(listOf("a", "b"), GalleryInfo.toggleTag(listOf("a"), "b"))
        assertEquals(listOf("a"), GalleryInfo.toggleTag(listOf("a", "b"), "b"))
    }

    @Test
    fun `T-GAL-33 add is one undo step`() {
        val presets = PresetImages.all.take(3).map { GalleryAddSource(it.source) }
        val result = GalleryActions.add(g0, owner, presets, act(), locked = true)
        assertEquals(11, positions(result.doc).size)
        assertEquals(positions(g0), positions(GalleryActions.undo(result.doc, result.change)))
    }

    @Test
    fun `GAL-10 an ungrouped project filter is one run and grouped rows use smaller cards`() {
        val project = GalleryOwner.project(g0)
        val withOwn = GalleryActions.copyTo(g0, listOf(ITEM_1, ITEM_2), project, act(), locked = true).doc
        fun pv(filter: GalleryFilter) = GalleryRows.build(withOwn, GalleryOwnerKind.PROJECT, g0.key, true, filter, GallerySort.MANUAL)
        val tag = pv(GalleryFilter.Tag(ExampleGallery.TAG_ILLUSTRATION))
        assertEquals(1, tag.sections.size)
        assertNull(tag.sections.single().header)
        assertEquals((1..tag.count).toList(), tag.entries.map { it.position })
        val own = pv(GalleryFilter.Project)
        assertEquals(1, own.sections.size)
        assertEquals(2, own.count)
        val all = pv(GalleryFilter.All)
        assertTrue(all.grouped)
        assertEquals(162f, GalleryCardSizes.of(all.sections.first(), true, false).width)
        assertEquals(132f, GalleryCardSizes.of(all.sections.last(), true, false).width)
        assertEquals(222f, GalleryCardSizes.of(own.sections.single(), false, false).width)
        // The hidden images of 리엔 collapse into one locked row in the grouped view.
        assertEquals(2, all.lockedRow?.count)
    }

    @Test
    fun `T-GAL-16 a hidden character in the project gallery`() {
        val hart = ExampleKeys.HART
        val sources = (1..3).map { GalleryAddSource("a" + it.toString().padStart(63, '0')) }
        val withHart = GalleryActions.add(g0, GalleryOwner.character(hart), sources, act(), locked = true).doc
        val hidden = withHart.copy(cast = withHart.cast.copy(characters = withHart.cast.characters.map { if (it.key == hart) it.copy(hidden = true) else it }))
        val v = GalleryRows.build(hidden, GalleryOwnerKind.PROJECT, g0.key, true, GalleryFilter.All, GallerySort.MANUAL)
        assertTrue(v.sections.none { it.ownerLink == hart })
        assertEquals("숨긴 인물 1", v.hiddenCharacters?.title)
        assertEquals("전체 8", v.chips.first().text)
        assertTrue(GalleryCopyTargets.targets(hidden, listOf(ITEM_1), locked = true).none { it.owner.key == hart })
    }

    @Test
    fun `GAL-18 several at once`() {
        var m = MultiPick.OFF
        assertEquals(m, m.toggle(ITEM_1))
        m = MultiPick.start().toggle(ITEM_1).toggle(ITEM_4).toggle(ITEM_2).toggle(ITEM_4)
        assertEquals(listOf(ITEM_1, ITEM_2), m.keys)
        assertEquals("2장 고름", m.title)
        assertEquals(listOf(ITEM_2), m.reconcile(listOf(ITEM_2, ITEM_3)).keys)
        // Hide and delete of several are one step each.
        val hidden = listOf(ITEM_1, ITEM_2).fold(g0) { d, k -> GalleryActions.setHidden(d, k, true, act())!!.doc }
        assertEquals(6, positions(hidden).size)
        val deleted = GalleryActions.delete(g0, m.keys, act())!!
        assertEquals("이미지 2장 지움", deleted.toast.message)
    }

    @Test
    fun `GAL-12 the gallery crop uses the gallery ratios and the item's crops`() {
        val stored = Crop(x = 0, y = 100, w = 1200, h = 1200)
        val item = g0.index.galleryItem(ITEM_2)!!.withCrop(Ratio.R1X1, stored)
        val session = CropSession.startGallery(PixelSize(1200, 1600), item)
        assertEquals(CropTarget.GALLERY, session.target)
        assertEquals(listOf(Ratio.R3X4, Ratio.R1X1), session.ratios)
        assertEquals(listOf("갤러리 카드 3:4", "목록 1:1"), session.ratios.map(session::chip))
        assertTrue(session.holoAllowed)
        assertEquals(setOf(Ratio.R3X4), session.crops().keys)
        val square = session.select(Ratio.R1X1)
        assertEquals(stored, square.crops()[Ratio.R1X1])
        // The session survives process death.
        assertEquals(square, CropSession.decode(square.encode()))
    }

    @Test
    fun `GAL-04 the order line copy`() {
        assertEquals("직접 순서 · 8", GalleryUiCopy.orderSection(8))
        assertEquals("고른 이미지 · 등불 앞의 리엔 · 3/8", GalleryUiCopy.pickedBar("등불 앞의 리엔", "3/8"))
        assertEquals("옮기는 중 · 등불 앞의 리엔", GalleryUiCopy.movingBar("등불 앞의 리엔"))
        val menu = GalleryMenus.menu(g0, view(), ITEM_4, true)!!
        assertEquals("제목 없음", menu.title)
        assertEquals("리엔 · 갤러리 4/8", menu.small)
        assertEquals(ImageOwner(ImageOwnerKind.CHARACTER, rien, ImageSlot.CHARACTER_PORTRAIT), MainTarget.PORTRAIT.owner(g0.index.galleryItem(ITEM_4)!!))
        assertEquals(listOf(MainTarget.COVER), MainTarget.of(GalleryOwnerKind.PROJECT))
    }
}
