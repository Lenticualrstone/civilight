package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.export.ExportLayer
import io.github.lenticualrstone.civilight.domain.library.export.ExportPlanner
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.feature.library.export.ExportBuild
import io.github.lenticualrstone.civilight.feature.library.export.ExportFormRules
import io.github.lenticualrstone.civilight.feature.library.export.ExportRunner
import io.github.lenticualrstone.civilight.feature.library.export.pdf.PdfBlock
import io.github.lenticualrstone.civilight.feature.library.export.pdf.PdfLayout
import io.github.lenticualrstone.civilight.feature.library.export.pdf.PdfStyle
import io.github.lenticualrstone.civilight.feature.library.export.pdf.PdfWriter
import io.github.lenticualrstone.civilight.feature.library.export.pdf.ScriptPdf
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.script.BlockKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 대본 PDF (LIB-43): the printed blocks, the layers, pagination, the gate and `PDF 암호`. */
class PdfExportTest {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE
    private val ashen = LibFx.ashen()

    /** A tiny valid PDF standing in for Android's `PdfDocument` output. */
    private val fakeWriter = PdfWriter { blocks ->
        val body = blocks.size.toString()
        val objs = listOf(
            "1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n",
            "2 0 obj\n<</Type /Pages /Count 0 /Kids []>>\nendobj\n",
            "3 0 obj\n<</Producer (test $body)>>\nendobj\n",
        )
        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = objs.map { o -> sb.length.also { sb.append(o) } }
        val xref = sb.length
        sb.append("xref\n0 4\n0000000000 65535 f \n")
        offsets.forEach { sb.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        sb.append("trailer\n<</Size 4 /Root 1 0 R /Info 3 0 R>>\nstartxref\n$xref\n%%EOF\n")
        sb.toString().toByteArray(Charsets.ISO_8859_1)
    }

    private fun blocks(doc: ProjectDoc, layers: Set<String>): List<PdfBlock> {
        var form = ExportFormRules.initial(doc, now, zone, ExportFormat.PDF)
        ExportLayer.entries.forEach { form = ExportFormRules.toggleLayer(form, it, it.id in layers) }
        val options = ExportFormRules.runOptions(form)
        return ScriptPdf.build(doc, ExportPlanner.plan(doc, options), options.rangeKeys, coverRating = true)
    }

    @Test
    fun `the cover carries the name, the rating and the range, then scene heads as title lines`() {
        val out = blocks(ashen, setOf("direction", "paren"))
        val cover = out.first() as PdfBlock.Cover
        assertEquals("잿빛 새벽의 등불", cover.title)
        assertEquals("R-15", cover.rating)
        assertEquals("1장 전체", cover.range)
        val heads = out.filterIsInstance<PdfBlock.SceneTitle>()
        assertTrue(heads.isNotEmpty())
        assertTrue(heads.first().head, heads.first().head.startsWith("씬 "))
        assertTrue(out.filterIsInstance<PdfBlock.Dialogue>().any { it.speaker == "리엔" })
    }

    @Test
    fun `display layers decide 연출, 괄호 지문 and 작가 메모`() {
        val all = blocks(ashen, setOf("direction", "paren", "memo"))
        val none = blocks(ashen, emptySet())
        val hasDirections = ashen.manuscript.liveBlocks.any { it.kind.known == BlockKind.DIRECTION && it.cues.isNotEmpty() }
        assertEquals(hasDirections, all.any { it is PdfBlock.Direction })
        assertFalse(none.any { it is PdfBlock.Direction })
        assertFalse(none.any { it is PdfBlock.Memo })
        val hasMemo = ashen.manuscript.liveBlocks.any { it.kind.known == BlockKind.MEMO }
        assertEquals(hasMemo, all.any { it is PdfBlock.Memo })
        // 리엔's `(작게)` paren prints only with the layer.
        assertTrue(all.filterIsInstance<PdfBlock.Dialogue>().any { "(" in it.text })
        assertFalse(none.filterIsInstance<PdfBlock.Dialogue>().any { "(작게)" in it.text })
    }

    @Test
    fun `빼기 leaves a hidden scene out of the PDF`() {
        val hidden = ashen.copy(story = ashen.story.copy(units = ashen.story.units.map { if (it.key == ExampleKeys.SCENE_1_7_4) it.copy(hidden = true) else it }))
        val form = ExportFormRules.initial(hidden, now, zone, ExportFormat.PDF)
        val plan = ExportPlanner.plan(hidden, ExportFormRules.runOptions(form))
        assertTrue(ExampleKeys.SCENE_1_7_4 in plan.leftOutScenes)
        assertFalse(plan.scenes.any { it.sceneKey == ExampleKeys.SCENE_1_7_4 })
    }

    @Test
    fun `pages break inside the text area, the cover stands alone and pages are numbered`() {
        val layout = PdfLayout({ text, style -> text.length * style.sizePt * 0.9f })
        val pages = layout.pages(blocks(ashen, setOf("direction", "paren")))
        assertTrue(pages.size > 2)
        assertTrue(pages.first().lines.none { it.style == PdfStyle.PAGE_NUMBER })
        pages.drop(1).forEachIndexed { i, page ->
            assertEquals((i + 2).toString(), page.lines.single { it.style == PdfStyle.PAGE_NUMBER }.text)
            page.lines.filter { it.style != PdfStyle.PAGE_NUMBER }.forEach { line ->
                assertTrue(line.baseline <= 842f - 72f + 1f)
                assertTrue(line.text, line.text.length * line.style.sizePt * 0.9f <= 595f - 128f + 0.01f)
            }
            assertFalse("a speaker never ends a page", page.lines.lastOrNull { it.style != PdfStyle.PAGE_NUMBER }?.style == PdfStyle.SPEAKER)
        }
        val wrapped = layout.wrap("하르트 씨, 방금 저 불빛은 결사의 신호였어요. 뒤돌아보지 말고 따라와.", PdfStyle.BODY, 200f)
        assertTrue(wrapped.size > 1)
        assertEquals("하르트 씨, 방금 저 불빛은 결사의 신호였어요. 뒤돌아보지 말고 따라와.".replace(" ", ""), wrapped.joinToString("").replace(" ", ""))
    }

    @Test
    fun `LIB-43 LIB-46 the PDF is written without fixing voice IDs, and PDF 암호 locks it`() = runTest {
        val plain = ExportRunner.build(ashen, ExportFormRules.initial(ashen, now, zone, ExportFormat.PDF), RecorderLibrary(), BlobReader.NONE, "t", now, pdf = fakeWriter) { true }
        plain as ExportBuild.Ready
        assertEquals("잿빛 새벽의 등불.pdf", plain.file.fileName)
        assertEquals("application/pdf", plain.file.mimeType)
        assertNull(plain.file.plan)

        val oath = LibFx.oath()
        var form = ExportFormRules.initial(oath, now, zone, ExportFormat.PDF)
        assertFalse("PDF 암호 on and empty", ExportFormRules.canCreate(form))
        form = ExportFormRules.setPdfPassword(form, "등불지기1234")
        assertTrue(ExportFormRules.canCreate(form))
        val locked = ExportRunner.build(oath, form, RecorderLibrary(), BlobReader.NONE, "t", now, pdf = fakeWriter) { true } as ExportBuild.Ready
        val text = locked.file.bytes.toString(Charsets.ISO_8859_1)
        assertTrue(text.startsWith("%PDF-1.7"))
        assertTrue(text.contains("/Filter /Standard /V 5 /R 6"))
        assertFalse(text.contains("(test "))
    }

    @Test
    fun `넣기 of hidden scenes asks once and a cancel writes nothing`() = runTest {
        val hidden = ashen.copy(story = ashen.story.copy(units = ashen.story.units.map { if (it.key == ExampleKeys.SCENE_1_7_4) it.copy(hidden = true) else it }))
        val form = ExportFormRules.setHiddenScenes(ExportFormRules.initial(hidden, now, zone, ExportFormat.PDF), include = true)
        var asked = 0
        assertEquals(ExportBuild.Cancelled, ExportRunner.build(hidden, form, RecorderLibrary(), BlobReader.NONE, "t", now, pdf = fakeWriter) { asked++; false })
        assertEquals(1, asked)
    }
}
