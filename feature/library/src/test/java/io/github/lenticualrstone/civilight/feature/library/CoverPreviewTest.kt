package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.feature.library.export.CoverPreview
import io.github.lenticualrstone.civilight.feature.library.export.ExportBuild
import io.github.lenticualrstone.civilight.feature.library.export.ExportFormRules
import io.github.lenticualrstone.civilight.feature.library.export.ExportRunner
import io.github.lenticualrstone.civilight.feature.library.export.PreviewRenderer
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileFlow
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenStage
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.AssetsIndexFile
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `preview.webp` (LIB-47, LIB-49): the cover picture goes into a `.civilight`, never for a hidden project. */
class CoverPreviewTest {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE

    private fun withCover(doc: ProjectDoc, source: String): ProjectDoc = doc.copy(
        assets = AssetsIndexFile(
            imageUses = listOf(
                ImageUse(
                    key = "01KCOVERUSE000000000000001",
                    ownerKind = ImageOwnerKind.PROJECT.open(),
                    ownerKey = doc.key,
                    slot = ImageSlot.PROJECT_COVER.open(),
                    source = source,
                ),
            ),
        ),
    )

    private fun hidden(doc: ProjectDoc): ProjectDoc =
        doc.copy(project = doc.project.copy(protections = doc.project.protections.copy(hidden = true)))

    @Test
    fun `the source is the picked cover, never a preset, a missing cover or a hidden project`() {
        val glass = LibFx.glass()
        assertNull(CoverPreview.source(glass))
        assertNull(CoverPreview.source(withCover(glass, "preset:ember")))
        assertEquals("ab12", CoverPreview.source(withCover(glass, "ab12"))!!.blobHash)
        assertNull(CoverPreview.source(hidden(withCover(glass, "ab12"))))
    }

    @Test
    fun `the exported package carries preview webp only for a visible project`() = runTest {
        val webp = "RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1)
        var renders = 0
        val renderer = PreviewRenderer { doc -> renders++; if (CoverPreview.source(doc) != null) webp else null }
        val doc = withCover(LibFx.glass(), "ab12")
        val form = ExportFormRules.initial(doc, now, zone, ExportFormat.PROJECT_FILE)
        val ready = ExportRunner.build(doc, form, RecorderLibrary(), BlobReader.NONE, "t", now, preview = renderer) { true } as ExportBuild.Ready
        assertTrue(ready.file.bytes.toString(Charsets.ISO_8859_1).contains(PackagePaths.PREVIEW))
        // The reader takes a package with the preview (LIB-50): it opens to the usual sheet.
        val reopened = OpenFileFlow.open(ready.file.bytes, ready.file.fileName, emptyList(), { 0 }, now, zone)
        assertTrue(reopened.toString(), reopened is OpenStage.Ask)

        val secret = hidden(doc)
        // Unlocked here only so the ZIP's names can be read; the rule does not depend on the lock.
        val hiddenForm = ExportFormRules.setEncrypt(ExportFormRules.initial(secret, now, zone, ExportFormat.PROJECT_FILE), false)
        val plain = ExportRunner.build(secret, hiddenForm, RecorderLibrary(), BlobReader.NONE, "t", now, preview = renderer) { true } as ExportBuild.Ready
        assertFalse(plain.file.bytes.toString(Charsets.ISO_8859_1).contains(PackagePaths.PREVIEW))
        assertEquals("the hidden project's cover is never rendered", 1, renders)
    }
}
