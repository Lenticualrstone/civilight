package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.domain.library.format.Sha256
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The example project survives export and import (LIB-47, LIB-49, §5.6, §7). */
class ExampleRoundTripTest {
    private val doc = Fixtures.example()

    @Test
    fun withTrashTheWholeProjectComesBackUnchanged() {
        val bytes = Fixtures.export(doc, Fixtures.recorder(), ProjectFileOptions(includeTrash = true))
        val pkg = Fixtures.open(bytes)
        assertEquals(emptyList<Any>(), pkg.failed)
        assertEquals(doc.copy(app = pkg.doc.app), pkg.doc)
        assertEquals(ExampleRecorder.projectFile().records.map { it.key }.toSet(), pkg.recorder!!.records.map { it.key }.toSet())
    }

    @Test
    fun withoutTrashOnlyLiveRowsAndNotExportedNameTags() {
        val pkg = Fixtures.open(Fixtures.export(doc, Fixtures.recorder()))
        val expected = ProjectPackageWriter.withNotExportedNameTags(doc)
        val liveOnly = ProjectFileSet.split(expected).copy(trash = null).toDoc()
        assertEquals(liveOnly.copy(trash = pkg.doc.trash), pkg.doc)
        assertTrue(pkg.doc.trash.entries.isEmpty())
        val noah = pkg.doc.tombstones.nameTags.single { it.key == ExampleKeys.NOAH }
        assertEquals(NameTagKind.CHARACTER.open(), noah.kind)
        assertEquals("노아", noah.name)
        assertEquals("NOA", noah.voiceCode)
        assertEquals(NameTagState.NOT_EXPORTED.open(), noah.state)
        assertNotNull(pkg.doc.index.unit(ExampleKeys.SCENE_1_7_3))
    }

    @Test
    fun textHashMatchesTheFixturesVoiceExport() {
        val block = doc.allBlocks.first { it.voice?.lastExport != null && it.key.startsWith("01JZ000000BKNM0101") }
        val text = block.runs.joinToString("") { it.shownText }
        assertEquals(block.voice!!.lastExport!!.textHash, Sha256.ofText(text))
    }
}
