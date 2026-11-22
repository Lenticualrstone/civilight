package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.gallery.GalleryViewState
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.store.blob.BlobReferences
import io.github.lenticualrstone.civilight.store.project.ProjectRekey
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.store.settings.AppSettingsCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Gallery file units in the working store (55 §3.3, §12.5) and the gallery app settings (§12.4). */
@OptIn(ExperimentalCoroutinesApi::class)
class GalleryStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val hashA = "a".repeat(64)
    private val hashG = "c".repeat(64)

    private fun item(key: Key, owner: Key, kind: GalleryOwnerKind, order: String, source: String, deletedAt: Long? = null) = GalleryItem(
        key = key, ownerKind = kind.open(), ownerKey = owner, source = source, order = order, title = key,
        deletedAt = deletedAt, trashEntryKey = deletedAt?.let { "TE_$key" },
    )

    /** C1 live with one live and one trashed image; C2 (trashed character) with one; the project with one. */
    private fun docWithGallery(): ProjectDoc {
        val d = Fx.richDoc()
        val c = GalleryOwnerKind.CHARACTER
        return d.copy(
            gallery = mapOf(
                "C1" to GalleryFile(c.open(), "C1", listOf(item("G1", "C1", c, "V", hashG), item("G2", "C1", c, "k", "preset:fog", deletedAt = Fx.T0 + 3))),
                "C2" to GalleryFile(c.open(), "C2", listOf(item("G3", "C2", c, "V", hashA))),
                d.key to GalleryFile(GalleryOwnerKind.PROJECT.open(), d.key, listOf(item("G4", d.key, GalleryOwnerKind.PROJECT, "V", hashG))),
            ),
        )
    }

    private fun sorted(d: ProjectDoc) = d.gallery.mapValues { (_, f) -> f.copy(items = f.items.sortedBy { it.key }) }.toSortedMap()

    @Test
    fun `gallery units are written per owner and split by liveness`() = runTest {
        val doc = docWithGallery()
        val dir = File(tmp.root, "projects/${doc.key}")
        ProjectStore.writeNew(doc, dir)
        val files = dir.walk().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }.toSet()
        assertTrue("gallery/C1.json" in files)
        assertTrue("trash/gallery/C1.json" in files)
        // A trashed character's images live in the trash mirror only.
        assertTrue("trash/gallery/C2.json" in files)
        assertFalse("gallery/C2.json" in files)
        assertTrue("gallery/${doc.key}.json" in files)

        val store = ProjectStore.open(Fx.P, dir, testEnv())
        assertEquals(sorted(doc), sorted(store.doc.value))
        assertEquals(emptyMap<String, String>(), store.loadReport.damaged)
        assertTrue(store.doc.value.unknownFiles.keys.none { it.startsWith("gallery/") || it.startsWith("trash/gallery/") })
    }

    @Test
    fun `a caption edit rewrites only that owner's file`() = runTest {
        val doc = docWithGallery()
        val dir = File(tmp.root, "projects/${doc.key}")
        ProjectStore.writeNew(doc, dir)
        val store = ProjectStore.open(Fx.P, dir, testEnv())
        val log = WriteLog(dir)
        store.storeDirectory.faults = log
        store.update("제목") { d ->
            val f = d.gallery.getValue("C1")
            d.copy(gallery = d.gallery + ("C1" to f.copy(items = f.items.map { if (it.key == "G1") it.copy(title = "등불 고리") else it })))
        }
        store.flush()
        assertEquals(listOf("gallery/C1.json"), log.writes)
    }

    @Test
    fun `gallery view state is app state in the view file`() = runTest {
        val doc = docWithGallery()
        val dir = File(tmp.root, "projects/${doc.key}")
        ProjectStore.writeNew(doc, dir)
        val store = ProjectStore.open(Fx.P, dir, testEnv())
        val state = GalleryViewState(pickedKey = "G1", panelExpanded = true, scrollIndex = 3, scrollOffset = 40)
        store.updateApp { it.copy(view = it.view.withGallery("C1", state)) }
        store.flush()
        val reopened = ProjectStore.open(Fx.P, dir, testEnv())
        assertEquals(state, reopened.doc.value.app.view.galleryOf("C1"))
    }

    @Test
    fun `T-GAL-27 blob references count gallery items live and trashed`() {
        val doc = docWithGallery()
        val counts = BlobReferences.counts(doc)
        assertEquals(2, counts[hashG]) // G1 and G4
        assertTrue(hashA in BlobReferences.of(doc)) // G3 of a trashed character
        assertFalse(BlobReferences.of(doc).any { it.startsWith("preset:") })
    }

    @Test
    fun `rekey moves the project gallery with the project key`() {
        val doc = docWithGallery()
        val next = ProjectRekey.rekey(doc, "01JDPROJECT0000000000000BB")
        assertFalse(doc.key in next.gallery)
        val file = next.gallery.getValue("01JDPROJECT0000000000000BB")
        assertEquals("01JDPROJECT0000000000000BB", file.ownerKey)
        assertTrue(file.items.all { it.ownerKey == "01JDPROJECT0000000000000BB" })
        assertEquals(doc.gallery["C1"], next.gallery["C1"])
    }

    @Test
    fun `T-GAL-37 gallery view setting round-trips and an unknown view reads as list`() {
        val settings = AppSettings().let { it.copy(viewByScreen = it.viewByScreen + (AppSettings.GALLERY_CHARACTER to ViewMode.GRID)) }
        val back = AppSettingsCodec.decode(AppSettingsCodec.encode(settings))
        assertEquals(ViewMode.GRID, back.viewByScreen["characterGallery"])
        val unknown = AppSettingsCodec.decode("""{"viewByScreen":{"characterGallery":"mosaic","library":"card"}}""")
        assertEquals(ViewMode.LIST, unknown.viewByScreen["characterGallery"])
        assertEquals(ViewMode.CARD, unknown.viewByScreen["library"])
    }

    @Test
    fun `gallery text and pane ratios round-trip and are absent by default`() {
        val plain = AppSettingsCodec.encode(AppSettings())
        assertFalse("paneSplit" in plain)
        assertFalse("galleryTextOn" in plain)
        assertTrue(AppSettings().galleryTextOn)
        val custom = AppSettings(galleryTextOn = false)
            .withPaneSplit(PaneSplit.Modules.CAST, PaneSplit.TWO_ONE)
            .withPaneSplit(PaneSplit.Modules.SCRIPT_PANEL, PaneSplit.ONE_TWO)
        val back = AppSettingsCodec.decode(AppSettingsCodec.encode(custom))
        assertEquals(custom, back)
        assertEquals(PaneSplit.TWO_ONE, back.paneSplitOf("cast"))
        assertEquals(PaneSplit.DEFAULT, back.paneSplitOf("library"))
        // `기본` removes the entry.
        assertEquals(AppSettings(galleryTextOn = false).withPaneSplit("scriptPanel", PaneSplit.ONE_TWO), back.withPaneSplit("cast", PaneSplit.DEFAULT))
        // A value a newer version wrote falls back to the default layout.
        assertEquals(PaneSplit.DEFAULT, AppSettingsCodec.decode("""{"paneSplit":{"cast":"threeOne"}}""").paneSplitOf("cast"))
    }
}
