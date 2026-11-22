package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.model.app.ProjectViewFile
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.doc.BlobRefs
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryViewState
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_6
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gallery model, file units, app state and the one blob reference function (55 §3, §12; CNV-47). */
class GalleryModelTest {
    private val g0 = ExampleGallery.build()

    private fun trashed(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc, key: String) = doc.copy(
        gallery = doc.gallery.mapValues { (_, f) -> f.copy(items = f.items.map { if (it.key == key) it.copy(deletedAt = 1L, trashEntryKey = "TE") else it }) },
    )

    @Test
    fun `G0 holds ten items of 리엔 in stored order with two hidden`() {
        val items = g0.index.galleryItemsOf(ExampleKeys.RIEN)
        assertEquals(ExampleGallery.STORED, items.map { it.key })
        assertEquals(2, items.count { it.hidden })
        assertEquals(items[2].source, g0.index.imageUse(
            io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind.CHARACTER, ExampleKeys.RIEN,
            io.github.lenticualrstone.civilight.model.asset.ImageSlot.CHARACTER_PORTRAIT,
        )?.source)
        assertTrue(items.all { g0.liveness.isLive(it) })
        // The plain example project has no gallery: G0 is opt-in.
        assertTrue(ExampleProject.build().gallery.isEmpty())
    }

    @Test
    fun `gallery files round-trip and trashed items go to the trash mirror`() {
        val doc = trashed(g0, ITEM_6)
        val set = ProjectFileSet.split(doc)
        val files = set.encode()
        val live = PackagePaths.gallery(ExampleKeys.RIEN)
        assertTrue(live in files)
        assertTrue(PackagePaths.trash(live) in files)
        assertEquals(9, set.live.gallery.getValue(ExampleKeys.RIEN).items.size)
        assertEquals(listOf(ITEM_6), set.trash!!.units.gallery.getValue(ExampleKeys.RIEN).items.map { it.key })

        val decoded = ProjectFileSet.decode(files)
        assertTrue(decoded.unknownPaths.none { it.startsWith("gallery/") || it.startsWith("trash/gallery/") })
        val back = decoded.files.toDoc(app = doc.app, unknownFiles = doc.unknownFiles)
        assertEquals(
            doc.index.galleryItemsOf(ExampleKeys.RIEN),
            back.index.galleryItemsOf(ExampleKeys.RIEN),
        )
    }

    @Test
    fun `a deleted owner takes its items out of the live files`() {
        val doc = g0.copy(cast = g0.cast.copy(characters = g0.cast.characters.map { if (it.key == ExampleKeys.RIEN) it.copy(deletedAt = 5L, trashEntryKey = "TE") else it }))
        assertTrue(doc.galleryItems.none { doc.liveness.isLive(it) })
        val set = ProjectFileSet.split(doc)
        assertFalse(ExampleKeys.RIEN in set.live.gallery)
        assertEquals(10, set.trash!!.units.gallery.getValue(ExampleKeys.RIEN).items.size)
    }

    @Test
    fun `a file without gallery units reads as an empty gallery`() {
        val files = ProjectFileSet.split(ExampleProject.build()).encode()
        assertTrue(files.keys.none { it.startsWith("gallery/") })
        assertTrue(ProjectFileSet.decode(files).files.toDoc().gallery.isEmpty())
    }

    @Test
    fun `T-GAL-27 one blob reference function counts live and trashed gallery items`() {
        val doc = trashed(g0, ITEM_6)
        val refs = BlobRefs.of(doc)
        assertTrue(ExampleGallery.hashOf(6) in refs)
        assertTrue(ExampleGallery.hashOf(91) in refs)
        // Item 3 and the portrait share one source: two references.
        assertEquals(2, BlobRefs.counts(doc)[ExampleGallery.hashOf(3)])
        assertFalse(refs.any { it.startsWith("preset:") })
    }

    @Test
    fun `gallery item json keeps defaults small and unknown properties`() {
        val item = GalleryItem(key = "01JZ000000GXMG000000000099", ownerKind = io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind.PROJECT.open(),
            ownerKey = ExampleKeys.PROJECT, source = "preset:ash-dawn", order = "V")
        val json = CivilightJson.encode(GalleryItem.serializer(), item)
        assertTrue("\"title\":\"\"" in json)
        assertFalse("caption" in json)
        val withUnknown = json.dropLast(1) + ",\"future\":1}"
        val read = CivilightJson.decode(GalleryItem.serializer(), withUnknown)
        assertEquals(json.dropLast(1) + ",\"future\":1}", CivilightJson.encode(GalleryItem.serializer(), read))
        assertTrue(read.isPreset)
        assertNull(read.blobHash)
    }

    @Test
    fun `trash kind, feature id and view mode`() {
        assertEquals("galleryItem", TrashKind.GALLERY_ITEM.serialName)
        assertTrue(Features.GALLERY in Features.OPTIONAL)
        assertTrue(Features.GALLERY in Features.KNOWN)
        assertEquals("grid", ViewMode.GRID.serialName)
    }

    @Test
    fun `T-GAL-37 view by screen round-trips GRID and reads unknown values as LIST`() {
        val s = AppSettings(viewByScreen = mapOf(AppSettings.GALLERY_CHARACTER to ViewMode.GRID, AppSettings.GALLERY_PROJECT to ViewMode.CARD))
        assertEquals(s, CivilightJson.decode(AppSettings.serializer(), CivilightJson.encode(AppSettings.serializer(), s)))
        val unknown = CivilightJson.decode(AppSettings.serializer(), """{"viewByScreen":{"characterGallery":"shelf"}}""")
        assertEquals(ViewMode.LIST, unknown.viewByScreen["characterGallery"])
    }

    @Test
    fun `CNV-47 pane split is optional, round-trips and snaps to the nearest ratio`() {
        val plain = CivilightJson.encode(AppSettings.serializer(), AppSettings())
        assertFalse("paneSplit" in plain)
        val s = AppSettings().withPaneSplit(PaneSplit.Modules.LIBRARY, PaneSplit.ONE_ONE).withPaneSplit(PaneSplit.Modules.WORLD, PaneSplit.TWO_ONE)
        val back = CivilightJson.decode(AppSettings.serializer(), CivilightJson.encode(AppSettings.serializer(), s))
        assertEquals(s, back)
        assertEquals(PaneSplit.ONE_ONE, back.paneSplitOf("library"))
        assertEquals(PaneSplit.DEFAULT, back.paneSplitOf("home"))
        assertEquals(AppSettings().withPaneSplit("world", PaneSplit.TWO_ONE), back.withPaneSplit("library", PaneSplit.DEFAULT))
        assertEquals(PaneSplit.ONE_TWO, PaneSplit.nearest(0.2f))
        assertEquals(PaneSplit.ONE_ONE, PaneSplit.nearest(0.45f))
        assertEquals(PaneSplit.TWO_ONE, PaneSplit.nearest(0.9f))
        assertEquals(listOf(PaneSplit.ONE_TWO, PaneSplit.ONE_ONE, PaneSplit.TWO_ONE), PaneSplit.SNAP)
    }

    @Test
    fun `T-GAL-36 gallery view state round-trips in the view file and is absent by default`() {
        assertFalse("galleries" in CivilightJson.encode(ProjectViewFile.serializer(), ProjectViewFile()))
        val state = GalleryViewState(pickedKey = ExampleGallery.ITEM_3, panelExpanded = true, scrollIndex = 2, scrollOffset = 18)
        val view = ProjectViewFile().withGallery(ExampleKeys.RIEN, state)
        val back = CivilightJson.decode(ProjectViewFile.serializer(), CivilightJson.encode(ProjectViewFile.serializer(), view))
        assertEquals(state, back.galleryOf(ExampleKeys.RIEN))
        assertEquals(GalleryViewState(), back.galleryOf(ExampleKeys.PROJECT))
        assertEquals(ProjectViewFile(), back.withGallery(ExampleKeys.RIEN, GalleryViewState()))
    }
}
