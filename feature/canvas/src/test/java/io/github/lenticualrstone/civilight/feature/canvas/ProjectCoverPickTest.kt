package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.ui.image.ProjectCovers
import io.github.lenticualrstone.civilight.feature.canvas.common.actions
import io.github.lenticualrstone.civilight.domain.actions.PurgeActions
import io.github.lenticualrstone.civilight.domain.actions.GalleryActions
import io.github.lenticualrstone.civilight.feature.canvas.image.CropEdit
import io.github.lenticualrstone.civilight.feature.canvas.image.CropMath
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.PixelSize
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.feature.canvas.image.pick.GalleryPick
import io.github.lenticualrstone.civilight.feature.canvas.image.pick.ImagePickModel
import io.github.lenticualrstone.civilight.feature.canvas.image.pick.PickAction
import io.github.lenticualrstone.civilight.feature.canvas.image.pick.PickTab
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Presets
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.BlobRefs
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.ui.gallery.PresetPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * U-08 프로젝트 표지, the picker's cover mode (60 §7.3 CV-05 … CV-16, CV-19): the current line, the `갤러리`
 * tab, a gallery item becoming the cover with its own crops, `빼기`, and the blob the cover keeps.
 * Fixture: the example project with three project-owned gallery items P1 (`h1`, photo, crops, holo), P2
 * (preset `lantern`) and PH (hidden, `h2`).
 */
class ProjectCoverPickTest {
    private val h1 = "1".repeat(64)
    private val h2 = "2".repeat(64)
    private val source = PixelSize(1600, 1600)

    /** Crops as the crop editor stores them (with their zoom), so they survive a round trip through an edit. */
    private val p1Crops = mapOf(
        Ratio.R3X4 to CropMath.crop(source, Ratio.R3X4, CropEdit(zoom = 1f, cx = 0.45f, cy = 0.5f)),
        Ratio.R1X1 to CropMath.crop(source, Ratio.R1X1, CropEdit(zoom = 2f, cx = 0.4f, cy = 0.6f)),
    )

    private val base: ProjectDoc = CanvasFx.s0()
    private val cover = ImageOwner(ImageOwnerKind.PROJECT, base.key, ImageSlot.PROJECT_COVER)
    private val orders = OrderKeys.evenlySpaced(3)

    private fun blob(hash: String) = Blob(hash, "jpg", "image/jpeg", 10, width = 1600, height = 1600, source = BlobSource.GALLERY.open(), importedAt = 7L)

    private fun item(key: String, source: String, order: Int, title: String, holo: Boolean = false, hidden: Boolean = false, crops: Map<Ratio, Crop> = emptyMap()) =
        GalleryItem(
            key = key, ownerKind = GalleryOwnerKind.PROJECT.open(), ownerKey = base.key, source = source, order = orders[order],
            title = title, crops = crops.mapKeys { (r, _) -> if (r == Ratio.R3X4) "r3x4" else "r1x1" }, holo = holo, hidden = hidden,
        )

    private val p1 = item("P1", h1, 0, "표지 시안", holo = true, crops = p1Crops)
    private val p2 = item("P2", "preset:lantern", 1, "")
    private val ph = item("PH", h2, 2, "비밀 원화", hidden = true)

    /** The example with P1, P2, PH and no cover row. */
    private val g: ProjectDoc = base.copy(
        assets = base.assets.copy(
            blobs = base.assets.blobs + blob(h1) + blob(h2),
            imageUses = base.assets.imageUses.filterNot { cover.matches(it) },
        ),
        gallery = base.gallery + (base.key to GalleryFile(GalleryOwnerKind.PROJECT.open(), base.key, listOf(p1, p2, ph))),
    )

    private fun withCoverOf(doc: ProjectDoc, source: String, crops: Map<Ratio, Crop> = emptyMap(), holo: Boolean = false): ProjectDoc =
        if (source.startsWith(ImageUse.PRESET_PREFIX)) ImageEdits.placePreset(doc, cover, source.removePrefix(ImageUse.PRESET_PREFIX), holo, CanvasFx.ctx())
        else ImageEdits.placeBlob(doc, cover, source, crops, holo, CanvasFx.ctx())

    @Test
    fun `CV-05 the source word never names a hidden item`() {
        val doc = withCoverOf(g, h2)
        val v = ImagePickModel.build(doc, cover, false, locked = true)
        assertEquals("지금 표지 · 사진", v.coverLine!!.text)
        assertFalse(v.coverLine!!.text.contains("비밀"))
        assertTrue(v.gallery.none { it.name.contains("비밀") })
    }

    @Test
    fun `CV-06 locked, CV-07 unlocked`() {
        val lockedView = ImagePickModel.build(g, cover, false, locked = true)
        assertEquals(listOf(PickTab.GALLERY, PickTab.PRESETS, PickTab.MINE), lockedView.tabKinds)
        assertEquals("갤러리 2", lockedView.tabs[0])
        assertEquals("기본 이미지 8", lockedView.tabs[1])
        assertTrue(lockedView.gallery.none { it.item?.key == "PH" })
        assertTrue("h2 only PH uses is not offered under 내 이미지", lockedView.mine.none { it.source == h2 })
        assertEquals("내 이미지 ${lockedView.mine.size}", lockedView.tabs[2])

        val openView = ImagePickModel.build(g, cover, false, locked = false)
        assertEquals("갤러리 3", openView.tabs[0])
        assertEquals("숨김", openView.gallery.single { it.item?.key == "PH" }.chip)
        assertEquals("표지 시안", openView.gallery.single { it.item?.key == "P1" }.name)
        assertEquals("제목 없음", openView.gallery.single { it.item?.key == "P2" }.name)
        assertEquals("홀로", openView.gallery.single { it.item?.key == "P1" }.chip)
    }

    @Test
    fun `CV-08 no cover with a gallery opens on 갤러리, CV-09 an empty gallery on 기본 이미지`() {
        val v = ImagePickModel.build(g, cover, false, locked = true)
        assertEquals(PickTab.GALLERY, v.tabKinds[v.initialTab])
        assertEquals("표지 없음 · 첫 글자 카드", v.coverLine!!.text)
        assertNull(v.coverLine!!.cropHash)
        assertFalse(v.coverLine!!.canClear)
        assertEquals("잿", v.coverLine!!.thumb.initial)

        val empty = ImagePickModel.build(base.copy(assets = base.assets.copy(imageUses = base.assets.imageUses.filterNot { cover.matches(it) })), cover, false, locked = true)
        assertEquals(PickTab.PRESETS, empty.tabKinds[empty.initialTab])
        assertEquals("갤러리 0", empty.tabs[0])
        assertEquals("이미지 없음", empty.emptyOf(0))
        assertTrue(empty.tilesOf(0).isEmpty())
    }

    @Test
    fun `the first tab follows the cover, the gallery tile showing it is selected`() {
        val fromP1 = ImagePickModel.build(withCoverOf(g, h1), cover, false)
        assertEquals(PickTab.GALLERY, fromP1.tabKinds[fromP1.initialTab])
        assertEquals("gallery:P1", fromP1.initialSource)
        assertEquals("표지 · 홀로", fromP1.gallery.single { it.item?.key == "P1" }.chip)
        assertEquals("지금 표지 · 사진", fromP1.coverLine!!.text)
        assertEquals(h1, fromP1.coverLine!!.cropHash)

        val preset = ImagePickModel.build(withCoverOf(g, "preset:nebula"), cover, false)
        assertEquals(PickTab.PRESETS, preset.tabKinds[preset.initialTab])
        assertEquals("preset:nebula", preset.initialSource)
        assertEquals("지금 표지 · 성운", preset.coverLine!!.text)
        assertNull("presets are not cropped", preset.coverLine!!.cropHash)
        assertTrue(preset.coverLine!!.canClear)

        val other = "3".repeat(64)
        val mine = ImagePickModel.build(withCoverOf(ImageEdits.addBlob(g, blob(other).copy(source = BlobSource.FILE.open())), other), cover, false)
        assertEquals(PickTab.MINE, mine.tabKinds[mine.initialTab])
        assertEquals("지금 표지 · 파일", mine.coverLine!!.text)
    }

    @Test
    fun `other slots keep their two tabs`() {
        val header = ImageOwner(ImageOwnerKind.PAGE, "page", ImageSlot.PAGE_HEADER)
        val v = ImagePickModel.build(g, header, false)
        assertEquals(listOf(PickTab.PRESETS, PickTab.MINE), v.tabKinds)
        assertEquals(2, v.tabs.size)
        assertNull(v.coverLine)
        assertTrue(v.gallery.isEmpty())
        assertEquals(ImagePickModel.TAB_PRESETS, v.initialTab)
    }

    @Test
    fun `CV-10 고르기 on P1 hands its crops and holo to the crop editor and writes nothing`() {
        assertEquals(PickAction.FromGallery("P1"), ImagePickModel.action("gallery:P1"))
        val pick = ImagePickModel.galleryPick(p1) as GalleryPick.Original
        assertEquals(h1, pick.blobHash)
        assertEquals(p1Crops, pick.crops)
        assertTrue(pick.holo)
        assertNull(pick.hiddenNote)
        assertEquals("숨긴 이미지 · 표지에서는 보임", (ImagePickModel.galleryPick(ph) as GalleryPick.Original).hiddenNote)
        assertNull(ImageEdits.use(g, cover))
    }

    @Test
    fun `CV-11 적용 unchanged writes P1's crops and holo, CV-12 취소 writes nothing`() {
        val session = CropSession.start(ImageSlot.PROJECT_COVER, PixelSize(1600, 1600), ImageEdits.use(g, cover), h1, holo = true, initialCrops = p1Crops)
        assertEquals(setOf(Ratio.R3X4, Ratio.R1X1), session.touched)
        val applied = ImageEdits.placeBlob(g, cover, h1, session.crops(), session.holo, CanvasFx.ctx())
        val use = ImageEdits.use(applied, cover)!!
        assertEquals(h1, use.source)
        assertEquals(p1Crops[Ratio.R3X4], use.cropFor(Ratio.R3X4))
        assertEquals(p1Crops[Ratio.R1X1], use.cropFor(Ratio.R1X1))
        assertTrue(use.holo)
        assertEquals("P1 unchanged", p1, applied.galleryItems.single { it.key == "P1" })
        // 취소: the session is dropped, the document is the one before.
        assertNull(ImageEdits.use(g, cover))
    }

    @Test
    fun `a ratio the item has not cropped starts centred, re-cropping the same original keeps its crops`() {
        val only34 = mapOf(Ratio.R3X4 to p1Crops.getValue(Ratio.R3X4))
        val s = CropSession.start(ImageSlot.PROJECT_COVER, PixelSize(1600, 1600), null, h1, holo = false, initialCrops = only34)
        assertEquals(CropEdit(), s.edits[Ratio.R1X1])
        assertEquals(setOf(Ratio.R3X4), s.touched)
        // The cover already shows h1: its own crops win over a hand-off.
        val doc = withCoverOf(g, h1, mapOf(Ratio.R1X1 to Crop(0, 0, 800, 800)))
        val re = CropSession.start(ImageSlot.PROJECT_COVER, PixelSize(1600, 1600), ImageEdits.use(doc, cover), h1, holo = false, initialCrops = p1Crops)
        assertEquals(setOf(Ratio.R3X4), re.touched)
    }

    @Test
    fun `CV-13 a preset item is placed at once with its holo`() {
        val pick = ImagePickModel.galleryPick(p2) as GalleryPick.Preset
        assertEquals("lantern", pick.presetId)
        val doc = ImageEdits.placePreset(g, cover, pick.presetId, pick.holo, CanvasFx.ctx())
        assertEquals("preset:lantern", ImageEdits.use(doc, cover)!!.source)
    }

    @Test
    fun `CV-14 re-cropping the cover leaves P1's crops alone`() {
        val doc = withCoverOf(g, h1, p1Crops, holo = true)
        val recropped = ImageEdits.placeBlob(doc, cover, h1, mapOf(Ratio.R1X1 to Crop(0, 0, 900, 900)), holo = true, CanvasFx.ctx())
        assertEquals(Crop(0, 0, 900, 900), ImageEdits.use(recropped, cover)!!.cropFor(Ratio.R1X1))
        assertEquals(p1Crops[Ratio.R3X4], ImageEdits.use(recropped, cover)!!.cropFor(Ratio.R3X4))
        assertEquals(p1, recropped.galleryItems.single { it.key == "P1" })
    }

    @Test
    fun `CV-15 빼기 removes the row only and the undo puts the same row back`() {
        val doc = withCoverOf(g, h1, p1Crops, holo = true)
        val before = ImageEdits.use(doc, cover)!!
        val cleared = ImageEdits.clear(doc, cover, CanvasFx.ctx())
        assertNull(ImageEdits.use(cleared, cover))
        assertEquals(doc.assets.blobs, cleared.assets.blobs)
        assertEquals(doc.galleryItems, cleared.galleryItems)
        // The undo is the store's revert to the document before: the same row, crops and holo.
        assertEquals(before, ImageEdits.use(doc, cover))
        assertEquals("표지 뺌", ImageCopy.TOAST_COVER_CLEARED)
        assertEquals("되돌리기", ImageCopy.UNDO)
        assertEquals("nothing to clear: unchanged", cleared, ImageEdits.clear(cleared, cover, CanvasFx.ctx()))
    }

    @Test
    fun `CV-15 the toast's 되돌리기 puts back exactly the removed row, and only into an empty slot`() {
        val doc = withCoverOf(g, h1, p1Crops, holo = true)
        val index = doc.assets.imageUses.indexOfFirst(cover::matches)
        val removed = doc.assets.imageUses[index]
        val cleared = ImageEdits.clear(doc, cover, CanvasFx.ctx())
        // A later, unrelated edit must survive the restore (no generic undo of the last step).
        val later = cleared.copy(project = cleared.project.copy(name = "다른 이름"))
        val restored = ImageEdits.restoreUse(later, removed, index)
        assertEquals(removed, ImageEdits.use(restored, cover))
        assertEquals(doc.assets.imageUses, restored.assets.imageUses)
        assertEquals("다른 이름", restored.project.name)
        // Pressed twice, or after the row came back another way: unchanged.
        assertEquals(restored, ImageEdits.restoreUse(restored, removed, index))
        // The user placed another image in the slot since: that one stays.
        val replaced = ImageEdits.placePreset(cleared, cover, "lantern", false, CanvasFx.ctx())
        assertEquals(replaced, ImageEdits.restoreUse(replaced, removed, index))
        // An index past the end (rows removed meanwhile) is clamped.
        assertEquals(removed, ImageEdits.use(ImageEdits.restoreUse(cleared, removed, 99), cover))
    }

    @Test
    fun `CV-16 a cover-only original leaves BlobRefs after 빼기, CV-17 deleting P1 keeps the cover`() {
        val only = "4".repeat(64)
        val doc = withCoverOf(ImageEdits.addBlob(g, blob(only)), only)
        assertTrue(only in BlobRefs.of(doc))
        assertFalse(only in BlobRefs.of(ImageEdits.clear(doc, cover, CanvasFx.ctx())))

        val fromP1 = withCoverOf(g, h1, p1Crops)
        val withoutP1 = fromP1.copy(gallery = fromP1.gallery + (g.key to GalleryFile(GalleryOwnerKind.PROJECT.open(), g.key, listOf(p2, ph))))
        assertEquals(h1, ImageEdits.use(withoutP1, cover)!!.source)
        assertTrue(h1 in BlobRefs.of(withoutP1))
    }

    @Test
    fun `CV-17 deleting P1 through the gallery keeps the cover, CV-18 purging it keeps the original held`() {
        val ctx = CanvasFx.ctx().actions()
        val fromP1 = withCoverOf(g, h1, p1Crops, holo = true)
        val coverRow = ImageEdits.use(fromP1, cover)!!
        val deleted = GalleryActions.delete(fromP1, listOf("P1"), ctx)!!
        assertEquals(coverRow, ImageEdits.use(deleted.doc, cover))
        assertTrue(h1 in BlobRefs.of(deleted.doc))
        assertEquals(h1, ProjectCovers.cover(deleted.doc, "잿빛 새벽의 등불").hash)

        val entry = deleted.result.entries.single().key
        val purged = GalleryActions.purge(deleted.doc, entry, ctx).doc
        assertTrue(purged.galleryItems.none { it.key == "P1" })
        assertEquals(coverRow, ImageEdits.use(purged, cover))
        assertTrue(h1 in BlobRefs.of(purged))
        assertTrue("the daily sweep keeps it", h1 in PurgeActions.heldBlobs(purged))
    }

    @Test
    fun `CV-19 a stale cover row reads as none but keeps 빼기`() {
        val doc = withCoverOf(g, h1, p1Crops)
        val v = ImagePickModel.build(doc, cover, false, fileExists = { it != h1 })
        assertEquals("표지 없음 · 첫 글자 카드", v.coverLine!!.text)
        assertTrue(v.coverLine!!.canClear)
        assertNull(v.coverLine!!.cropHash)
        assertNull(v.coverLine!!.thumb.hash)
        assertTrue("its gallery tile is not offered either", v.gallery.none { it.item?.key == "P1" })
    }

    @Test
    fun `the three preset tables agree`() {
        assertEquals(PresetImages.all.map { it.id }, Presets.IDS)
        assertEquals(PresetImages.all.map { it.name }, Presets.NAMES)
        assertEquals(Presets.IDS, PresetPictures.IDS)
    }
}
