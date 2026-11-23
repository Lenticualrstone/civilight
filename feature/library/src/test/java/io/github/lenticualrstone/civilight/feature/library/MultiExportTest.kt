package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.hiding.HiddenItems
import io.github.lenticualrstone.civilight.feature.library.backup.RecoveryPasswordState
import io.github.lenticualrstone.civilight.feature.library.export.ExportFormRules
import io.github.lenticualrstone.civilight.feature.library.export.MultiExport
import io.github.lenticualrstone.civilight.feature.library.export.ExportCopy
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileFlow
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenStage
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-72 여러 프로젝트 내보내기, and LIB-36 on this module's own surfaces. */
class MultiExportTest {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE

    @Test
    fun `LIB-72 one file per checked project, own defaults, no two names alike`() {
        val ashen = LibFx.ashen()
        val oath = LibFx.oath()
        val oath2 = LibFx.oath()
        val docs = listOf(ashen, oath, oath2)
        val picked = MultiExport.toggle(MultiExport.toggle(MultiExport.toggle(emptySet(), ashen.key), oath.key), oath2.key)
        val files = MultiExport.files(docs, picked, now, zone)
        assertEquals(3, files.size)
        assertEquals("잿빛 새벽의 등불.civilight", files[0].fileName)
        assertFalse(files[0].encrypt)
        assertTrue("R-18 locks by default", files[1].encrypt)
        assertTrue(files[1].fileName.startsWith("civilight-내보내기-"))
        assertEquals(files[1].fileName.removeSuffix(".civilight") + " (2).civilight", files[2].fileName)
        assertEquals(listOf(true, true, true), MultiExport.rows(docs, picked).map { it.checked })
        assertEquals(2, MultiExport.files(docs, MultiExport.toggle(picked, ashen.key), now, zone).size)
    }

    @Test
    fun `LIB-92 S-4 no rating badge on the checklist rows`() {
        val ashen = LibFx.ashen()
        val oath = LibFx.oath()
        val rows = MultiExport.rows(listOf(ashen, oath), setOf(ashen.key, oath.key))
        val badges = Rating.entries.map { RatingLabels.badge(it) }
        rows.forEach { row ->
            val text = listOfNotNull(row.title, row.subtitle).joinToString(" ")
            badges.forEach { assertFalse("$text shows $it", text.contains(it)) }
            assertFalse(text, text.contains("R-1"))
        }
        // The R-18 project still locks by default (D34); a plain one has no sub-line.
        assertEquals(listOf(null, ExportCopy.PASSWORD), rows.map { it.subtitle })
    }

    @Test
    fun `LIB-72 a locked file opens with its own password`() {
        val oath = LibFx.oath()
        val file = MultiExport.files(listOf(oath), setOf(oath.key), now, zone).single()
        val bytes = MultiExport.build(oath, file, "등불지기1234".toCharArray(), RecorderLibrary(), BlobReader.NONE, "t", now, zone)
        val ask = OpenFileFlow.open(bytes, file.fileName, emptyList(), { 0 }, now, zone) as OpenStage.Password
        assertTrue(OpenFileFlow.unlock(bytes, "등불지기1234".toCharArray(), ask.sheet, emptyList(), { 0 }, now, zone) is OpenStage.Ask)
    }

    @Test
    fun `LIB-36 a hidden speaker goes into exports by name only, and its items lock with the 복구 암호`() {
        val ashen = LibFx.ashen()
        val rien = ashen.characters.first { it.name == "리엔" }
        val hidden = ashen.copy(cast = ashen.cast.copy(characters = ashen.cast.characters.map { if (it.key == rien.key) it.copy(hidden = true) else it }))
        val row = ExportFormRules.sheet(hidden, ExportFormRules.initial(hidden, now, zone, ExportFormat.ENGINE_JSON), RecorderLibrary())
            .rows.filterIsInstance<ExportRow.HiddenSpeakers>().single()
        assertEquals("숨긴 인물 1 · 리엔", row.label)
        assertEquals("화자 이름만 들어감", row.info)
        assertTrue("`.civilight` locks by default when an item is hidden", ExportFormRules.initial(hidden, now, zone, ExportFormat.PROJECT_FILE).options.encrypt)
        val items = HiddenItems(hidden).count
        val ui = RecoveryPasswordState.build(CharArray(0), CharArray(0), 0, listOf(hidden))
        assertEquals("숨긴 항목 $items", ui.rows.first().label)
        assertEquals("잿빛 새벽의 등불", ui.rows.first().value)
        assertTrue(ExampleKeys.SCENE_1_7_4.isNotEmpty())
    }
}
