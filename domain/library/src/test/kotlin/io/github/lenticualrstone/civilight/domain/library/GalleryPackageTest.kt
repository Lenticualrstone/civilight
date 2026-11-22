package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.PackageWriter
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.domain.library.hiding.HiddenItems
import io.github.lenticualrstone.civilight.domain.library.hiding.HideableKind
import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_3
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_6
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_H1
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_H2
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.STORED
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gallery in the `.civilight` package (format 1.0) and on the hiding surfaces (55 GAL-19, GAL-22, §3.3). */
class GalleryPackageTest {
    private val g0: ProjectDoc = ExampleGallery.build()
    private val blobs = BlobReader.of(ExampleGallery.blobBytes)

    private fun trashed(doc: ProjectDoc, key: String) = doc.copy(
        gallery = doc.gallery.mapValues { (_, f) -> f.copy(items = f.items.map { if (it.key == key) it.copy(deletedAt = Fixtures.NOW, trashEntryKey = "TE") else it }) },
    )

    private fun content(doc: ProjectDoc, options: ProjectFileOptions = ProjectFileOptions()) =
        ProjectPackageWriter.content(doc, Fixtures.recorder(), blobs, options, "Civilight test", Fixtures.NOW)

    private fun open(bytes: ByteArray) = (ProjectPackageReader.open(bytes) as PackageOpen.Ready).pkg

    private fun galleryJson(files: Map<String, ByteArray>, path: String): GalleryFile =
        CivilightJson.decode(GalleryFile.serializer(), files.getValue(path).toString(Charsets.UTF_8))

    @Test
    fun `T-GAL-17 export writes every live item, hidden ones too, in format 1_0 with the gallery feature`() {
        val c = content(trashed(g0, ITEM_6))
        val file = galleryJson(c.files, PackagePaths.gallery(K.RIEN))
        assertEquals(9, file.items.size)
        assertEquals(2, file.items.count { it.hidden })
        assertFalse(ITEM_6 in file.items.map { it.key })
        assertEquals("1.0", c.manifest.formatVersion)
        assertEquals("1.0", c.manifest.minReaderVersion)
        assertTrue(Features.GALLERY in c.manifest.features.optional)
        assertFalse(PackagePaths.trash(PackagePaths.gallery(K.RIEN)) in c.files)
        // The originals of the written items, not of the trashed one.
        for (n in listOf(1, 2, 3, 4, 91, 5, 7, 8, 92)) {
            assertArrayEquals(ExampleGallery.bytesOf(n), c.files[PackagePaths.galleryOriginal(ExampleGallery.hashOf(n), "webp")])
        }
        assertFalse(PackagePaths.galleryOriginal(ExampleGallery.hashOf(6), "webp") in c.files)
        // Item 3 shares its source with the portrait: also under assets/ for the image use.
        assertTrue(PackagePaths.asset(ExampleGallery.hashOf(3), "webp") in c.files)
        assertFalse(PackagePaths.asset(ExampleGallery.hashOf(1), "webp") in c.files)
        assertTrue(c.missingBlobs.isEmpty())
        // Hidden images make the project count as having hidden items: encryption defaults to on.
        assertEquals(setOf(ITEM_H1, ITEM_H2), HiddenItems(g0).galleryImages)
        assertTrue(ExportDefaults.encryptDefault(g0))
    }

    @Test
    fun `with 휴지통도 포함 trashed images go to trash gallery`() {
        val c = content(trashed(g0, ITEM_6), ProjectFileOptions(includeTrash = true))
        val trash = galleryJson(c.files, PackagePaths.trash(PackagePaths.gallery(K.RIEN)))
        assertEquals(listOf(ITEM_6), trash.items.map { it.key })
        assertTrue(PackagePaths.galleryOriginal(ExampleGallery.hashOf(6), "webp") in c.files)
        val back = open(c.toZip()).doc
        assertEquals(STORED, back.index.galleryItemsOf(K.RIEN).map { it.key })
        assertTrue(back.index.galleryItem(ITEM_6)!!.deletedAt != null)
    }

    @Test
    fun `the package reads back with the same items and the same bytes`() {
        val pkg = open(content(g0).toZip())
        assertEquals(g0.index.galleryItemsOf(K.RIEN), pkg.doc.index.galleryItemsOf(K.RIEN))
        for (n in listOf(1, 3, 91)) assertArrayEquals(ExampleGallery.bytesOf(n), pkg.blobs[ExampleGallery.hashOf(n)])
        assertTrue(pkg.doc.unknownFiles.keys.none { it.startsWith("gallery/") })
        assertTrue(pkg.failed.isEmpty())
        // Every written blob keeps its metadata row.
        assertTrue(pkg.doc.assets.blobs.any { it.hash == ExampleGallery.hashOf(1) })
    }

    @Test
    fun `T-GAL-10 the same original in two galleries is one file`() {
        val photo = ExampleGallery.hashOf(1)
        val projectItem = GalleryItem(
            key = ExampleGallery.item(70), ownerKind = GalleryOwnerKind.PROJECT.open(), ownerKey = K.PROJECT, source = photo, order = "V",
        )
        val doc = g0.copy(gallery = g0.gallery + (K.PROJECT to GalleryFile(GalleryOwnerKind.PROJECT.open(), K.PROJECT, listOf(projectItem))))
        val c = content(doc)
        assertEquals(1, c.files.keys.count { it.startsWith(PackagePaths.GALLERY_FILES_DIR) && photo in it })
        assertEquals(photo, galleryJson(c.files, PackagePaths.gallery(K.PROJECT)).items.single().source)
        assertTrue(galleryJson(c.files, PackagePaths.gallery(K.RIEN)).items.any { it.source == photo })
    }

    @Test
    fun `re-keying a project moves its project gallery`() {
        val projectItem = GalleryItem(
            key = ExampleGallery.item(71), ownerKind = GalleryOwnerKind.PROJECT.open(), ownerKey = K.PROJECT, source = "preset:ash-dawn", order = "V",
        )
        val doc = g0.copy(gallery = g0.gallery + (K.PROJECT to GalleryFile(GalleryOwnerKind.PROJECT.open(), K.PROJECT, listOf(projectItem))))
        val newKey = "01JZ000000PRJT000000000077"
        val next = io.github.lenticualrstone.civilight.domain.library.importing.ProjectRekey.rekey(doc, newKey)
        assertNull(next.gallery[K.PROJECT])
        assertEquals(newKey, next.gallery.getValue(newKey).items.single().ownerKey)
        assertEquals(STORED, next.index.galleryItemsOf(K.RIEN).map { it.key })
    }

    @Test
    fun `T-GAL-32 a preset item writes no file`() {
        val f = g0.gallery.getValue(K.RIEN)
        val doc = g0.copy(gallery = g0.gallery + (K.RIEN to f.copy(items = f.items.map { if (it.key == ITEM_3) it.copy(source = "preset:fog-forest") else it })))
        val c = content(doc)
        assertTrue(c.files.keys.none { "fog-forest" in it })
        assertTrue(c.missingBlobs.isEmpty())
    }

    @Test
    fun `T-GAL-18 a file without gallery opens, and gallery files are read by path when the manifest does not list them`() {
        val plain = open(content(Fixtures.example()).toZip())
        assertTrue(plain.doc.gallery.isEmpty())

        val c = content(g0)
        val unlisted = c.manifest.copy(features = c.manifest.features.copy(optional = c.manifest.features.optional - Features.GALLERY))
        val pkg = open(PackageWriter.zip(PackagePaths.MIME_PROJECT, unlisted, c.files))
        assertEquals(STORED, pkg.doc.index.galleryItemsOf(K.RIEN).map { it.key })
        assertArrayEquals(ExampleGallery.bytesOf(92), pkg.blobs[ExampleGallery.hashOf(92)])
    }

    @Test
    fun `items whose owner is not in the file are dropped on read`() {
        val c = content(g0)
        val ghost = "01JZ000000CHAR000000000999"
        val orphan = GalleryFile(
            GalleryOwnerKind.CHARACTER.open(), ghost,
            listOf(GalleryItem(key = ExampleGallery.item(80), ownerKind = GalleryOwnerKind.CHARACTER.open(), ownerKey = ghost, source = "preset:fog", order = "V")),
        )
        val files = c.files + (PackagePaths.gallery(ghost) to CivilightJson.encode(GalleryFile.serializer(), orphan).toByteArray())
        val pkg = open(PackageWriter.zip(PackagePaths.MIME_PROJECT, c.manifest, files))
        assertNull(pkg.doc.gallery[ghost])
        assertEquals(10, pkg.doc.index.galleryItemsOf(K.RIEN).size)
    }

    @Test
    fun `T-GAL-06 T-GAL-34 a gallery masks hidden images while locked and tags them while open`() {
        val surfaces = ItemSurfaces(g0)
        val locked = surfaces.gallery(K.RIEN, locked = true)
        assertEquals(8, locked.items.size)
        assertEquals("숨긴 이미지 2", locked.lockedRow?.title)
        assertEquals(HideableKind.GALLERY_IMAGE, locked.lockedRow?.kind)
        val open = surfaces.gallery(K.RIEN, locked = false)
        assertEquals(STORED, open.items.map { it.key })
        assertEquals(setOf(ITEM_H1, ITEM_H2), open.taggedHidden)
        assertNull(open.lockedRow)
    }

    @Test
    fun `T-GAL-16 a hidden character is one row in the project gallery without an image count`() {
        val hart = (1..3).map {
            GalleryItem(key = ExampleGallery.item(40 + it), ownerKind = GalleryOwnerKind.CHARACTER.open(), ownerKey = K.HART, source = "preset:h$it", order = "V$it")
        }
        val doc = g0.copy(
            cast = g0.cast.copy(characters = g0.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }),
            gallery = g0.gallery + (K.HART to GalleryFile(GalleryOwnerKind.CHARACTER.open(), K.HART, hart)),
        )
        val groups = ItemSurfaces(doc).projectGallery(locked = true)
        assertTrue(groups.characters.none { it.character.key == K.HART })
        assertEquals(listOf(K.RIEN), groups.characters.map { it.character.key })
        assertEquals("숨긴 인물 1", groups.hiddenCharacters?.title)
        val open = ItemSurfaces(doc).projectGallery(locked = false)
        assertTrue(open.characters.any { it.character.key == K.HART })
        assertNull(open.hiddenCharacters)
        // His images are not hidden images of their own.
        assertFalse(HiddenItems(doc).galleryImages.any { it in hart.map { h -> h.key } })
    }

    @Test
    fun `T-GAL-19 search leaves out hidden images while locked`() {
        val locked = ItemSurfaces(g0).searchScope(locked = true)
        assertTrue(ITEM_3 in locked.galleryItemKeys)
        assertFalse(ITEM_H1 in locked.galleryItemKeys)
        assertEquals(ItemSurfaces.FOOTER_HIDDEN_ITEMS, locked.footer)
        assertTrue(ITEM_H1 in ItemSurfaces(g0).searchScope(locked = false).galleryItemKeys)
    }
}
