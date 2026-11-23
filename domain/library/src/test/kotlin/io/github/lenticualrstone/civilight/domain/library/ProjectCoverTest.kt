package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.domain.library.newproject.CoverSummary
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenuAction
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenus
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Presets
import io.github.lenticualrstone.civilight.model.doc.BlobRefs
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.FileManifest
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.domain.library.format.Sha256
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * U-08 프로젝트 표지 (60 §7): the ⋯ row and its source word (CV-01 … CV-05), the cover in a `.civilight`
 * (CV-28, CV-29, a file without a cover row) and the blob it keeps (CV-16). No new field and no format
 * change: the cover is the existing `ImageUse(project, <key>, projectCover)` row (LIB-90).
 */
class ProjectCoverTest {
    private val glass: ProjectDoc = Fixtures.projectWith("유리 정원", Rating.ALL, scenes = 2)
    private val photo = Fixtures.randomBytes(240, seed = 21)
    private val photoHash = Sha256.hex(photo)

    private fun blob(hash: String, source: BlobSource) =
        Blob(hash, "webp", "image/webp", 240, width = 600, height = 800, source = source.open(), importedAt = Fixtures.NOW)

    private fun cover(doc: ProjectDoc, source: String, crops: Map<String, Crop> = emptyMap(), holo: Boolean = false) = ImageUse(
        key = Fixtures.keys(90)(), ownerKind = ImageOwnerKind.PROJECT.open(), ownerKey = doc.key,
        slot = ImageSlot.PROJECT_COVER.open(), source = source, crops = crops, holo = holo,
    )

    private fun withCover(doc: ProjectDoc, use: ImageUse, vararg blobs: Blob): ProjectDoc =
        doc.copy(assets = doc.assets.copy(blobs = doc.assets.blobs + blobs, imageUses = doc.assets.imageUses.filterNot { it.slot.known == ImageSlot.PROJECT_COVER } + use))

    @Test
    fun `CV-01 the cover row follows 이름 바꾸기, CV-02 the example shows only 열기`() {
        val menu = ProjectMenus.of(glass)
        assertEquals(
            listOf(
                ProjectMenuAction.OPEN, ProjectMenuAction.RENAME, ProjectMenuAction.COVER, ProjectMenuAction.RATING,
                ProjectMenuAction.HIDE, ProjectMenuAction.BACKUP_EXPORT, ProjectMenuAction.DELETE,
            ),
            menu.rows.map { it.action },
        )
        val row = menu.rows.single { it.action == ProjectMenuAction.COVER }
        assertEquals("표지 이미지", row.title)
        assertEquals("없음 · 첫 글자 카드", row.sub)
        assertEquals(listOf(ProjectMenuAction.OPEN), ProjectMenus.of(Fixtures.example(), isExample = true).rows.map { it.action })
    }

    @Test
    fun `CV-03 a preset cover is named, CV-04 a photo is 사진 and a file is 파일`() {
        val lantern = withCover(glass, cover(glass, ImageUse.PRESET_PREFIX + "lantern"))
        assertEquals("지금 등불", CoverSummary.menuSub(lantern))
        assertEquals("등불", CoverSummary.settingsValue(lantern))

        val byPhoto = withCover(glass, cover(glass, photoHash), blob(photoHash, BlobSource.GALLERY))
        assertEquals("지금 사진", ProjectMenus.of(byPhoto).rows.single { it.action == ProjectMenuAction.COVER }.sub)
        val byFile = withCover(glass, cover(glass, photoHash), blob(photoHash, BlobSource.FILE))
        assertEquals("지금 파일", CoverSummary.menuSub(byFile))
        assertEquals("없음", CoverSummary.settingsValue(glass))
        // The registry other modules read keeps the picker's eight names in file-format order.
        assertEquals(8, Presets.IDS.size)
        assertEquals("잿빛 새벽", Presets.nameOf("ash-dawn"))
        assertNull(Presets.nameOf("nope"))
    }

    @Test
    fun `CV-05 the source word never names the gallery item, CV-19 a missing original reads as none`() {
        val doc = withCover(glass, cover(glass, photoHash), blob(photoHash, BlobSource.GALLERY))
        val sub = CoverSummary.menuSub(doc)
        assertEquals("지금 사진", sub)
        assertEquals("없음 · 첫 글자 카드", CoverSummary.menuSub(doc, fileExists = { it != photoHash }))
        assertEquals("없음", CoverSummary.settingsValue(doc, fileExists = { false }))
        // An unknown preset id draws the initial-letter card, so it reads as none too.
        assertNull(CoverSummary.word(withCover(glass, cover(glass, ImageUse.PRESET_PREFIX + "gone"))))
    }

    @Test
    fun `CV-28 the cover travels in a 1_0 file with its original and preview, and comes back equal`() {
        val crops = mapOf("r3x4" to Crop(0, 0, 600, 800), "r1x1" to Crop(0, 100, 600, 600))
        val use = cover(glass, photoHash, crops, holo = true)
        val doc = withCover(glass, use, blob(photoHash, BlobSource.GALLERY))
        val preview = byteArrayOf(1, 2, 3)
        val bytes = ProjectPackageWriter.write(
            doc, options = io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions(preview = preview),
            blobs = BlobReader.of(mapOf(photoHash to photo)), writtenBy = "t", writtenAt = Fixtures.NOW,
        )
        val pkg = Fixtures.open(bytes)
        assertEquals(FileManifest.FORMAT_VERSION, pkg.manifest.formatVersion)
        assertEquals("1.0", pkg.manifest.formatVersion)
        assertEquals("1.0", pkg.manifest.minReaderVersion)
        // No new feature id: the manifest lists what a file without a cover lists.
        assertEquals(Fixtures.open(Fixtures.export(glass)).manifest.features, pkg.manifest.features)
        assertEquals(use, CoverSummary.use(pkg.doc))
        assertArrayEquals(photo, pkg.blobs[photoHash])
        assertArrayEquals(preview, pkg.preview)
        assertTrue(photoHash in BlobRefs.of(pkg.doc))
    }

    @Test
    fun `a file without a cover row reads as no cover`() {
        val pkg = Fixtures.open(Fixtures.export(glass))
        assertNull(CoverSummary.use(pkg.doc))
        assertNull(pkg.preview)
        assertEquals("없음 · 첫 글자 카드", CoverSummary.menuSub(pkg.doc))
    }

    @Test
    fun `CV-29 a row with an unknown slot is kept and written back unchanged`() {
        val odd = ImageUse(
            key = Fixtures.keys(91)(), ownerKind = ImageOwnerKind.PROJECT.open(), ownerKey = glass.key,
            slot = OpenEnum.unknown("projectBanner"), source = ImageUse.PRESET_PREFIX + "nebula",
        )
        val doc = glass.copy(assets = glass.assets.copy(imageUses = glass.assets.imageUses + odd))
        val once = Fixtures.open(Fixtures.export(doc)).doc
        val kept = once.assets.imageUses.single { it.key == odd.key }
        assertEquals("projectBanner", kept.slot.raw)
        assertTrue(kept.slot.isUnknown)
        val twice = Fixtures.open(Fixtures.export(once)).doc
        assertEquals(kept, twice.assets.imageUses.single { it.key == odd.key })
        // It is not the cover.
        assertNull(CoverSummary.use(twice))
    }

    @Test
    fun `CV-16 BlobRefs keeps a cover original and lets it go once the row is removed`() {
        val doc = withCover(glass, cover(glass, photoHash), blob(photoHash, BlobSource.GALLERY))
        assertTrue(photoHash in BlobRefs.of(doc))
        val removed = doc.copy(assets = doc.assets.copy(imageUses = doc.assets.imageUses.filterNot { it.slot.known == ImageSlot.PROJECT_COVER }))
        assertFalse(photoHash in BlobRefs.of(removed))
    }
}
