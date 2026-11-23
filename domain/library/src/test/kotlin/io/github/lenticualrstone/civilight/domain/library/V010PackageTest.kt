package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.domain.library.format.ZipCodec
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectDraft
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectFlow
import io.github.lenticualrstone.civilight.domain.library.export.ProjectFileExport
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.marks.Foreshadow
import io.github.lenticualrstone.civilight.model.marks.MarkAnchor
import io.github.lenticualrstone.civilight.model.version.EpisodeVersion
import io.github.lenticualrstone.civilight.model.version.EpisodeVersionsFile
import io.github.lenticualrstone.civilight.model.version.VersionBlock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scope §8 acceptance 2 and 00 §13.8 DM-0.2-03, DM-0.2-08: a `.civilight` written by the **v0.1.0
 * tag's code** (`src/test/resources/v010/rich.civilight`, with the trash) opens in 0.2.0 with the
 * switch decided by D6 and writes back unchanged; game data and versions travel as specified.
 */
class V010PackageTest {
    private fun v010(): ByteArray =
        requireNotNull(javaClass.classLoader!!.getResource("v010/rich.civilight")) { "fixture" }.readBytes()

    private fun projectJson(bytes: ByteArray): JsonObject = Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject

    @Test
    fun `a 0_1_0 file opens with the switch on and writes back unchanged`() {
        val bytes = v010()
        val pkg = Fixtures.open(bytes)
        assertEquals(emptyList<Any>(), pkg.failed)
        assertEquals(emptyList<String>(), pkg.skippedOptionalFeatures)
        assertEquals(true, pkg.doc.project.gameTools)
        assertEquals("r15", pkg.doc.project.rating.raw)
        assertTrue(pkg.doc.project.protections.captureBlock)

        val again = ZipCodec.read(
            ProjectPackageWriter.write(
                pkg.doc, pkg.recorder?.toLibrary(pkg.doc.key) ?: RecorderLibrary(), BlobReader.of(pkg.blobs),
                ProjectFileOptions(includeTrash = true), writtenBy = "Civilight test", writtenAt = pkg.manifest.writtenAt,
            ),
        )
        val stored = ZipCodec.read(bytes)
        assertEquals(stored.paths.toSet(), again.paths.toSet())
        for (path in stored.paths.filter { it != PackagePaths.MANIFEST && it != PackagePaths.PROJECT }) {
            assertArrayEquals(path, stored[path], again[path])
        }
        val old = projectJson(stored[PackagePaths.PROJECT]!!)
        val new = projectJson(again[PackagePaths.PROJECT]!!)
        assertFalse("gameTools" in old)
        assertEquals(JsonPrimitive(true), new["gameTools"])
        assertEquals(old, JsonObject(new - "gameTools"))
    }

    @Test
    fun `game data travels with the switch off`() {
        val pkg = Fixtures.open(v010())
        val off = pkg.doc.copy(project = pkg.doc.project.copy(gameTools = false))
        val back = Fixtures.open(Fixtures.export(off, options = ProjectFileOptions(includeTrash = true)))
        assertEquals(false, back.doc.project.gameTools)
        assertEquals(off.copy(app = back.doc.app, unknownFiles = back.doc.unknownFiles), back.doc)
        assertTrue(back.doc.allBlocks.any { it.voice?.fixedId != null })
        assertTrue(back.doc.allBlocks.any { it.cues.isNotEmpty() })
        assertTrue(back.doc.assets.scriptAssets.isNotEmpty())
        assertTrue(back.doc.cast.voiceLines.isNotEmpty())
    }

    private fun withVersion(doc: ProjectDoc): ProjectDoc {
        val block = doc.manuscript.liveBlocks.first { it.kind.known == BlockKind.DIALOGUE }
        val unit = doc.index.numberingUnitKeyOfScene(block.sceneKey)!!
        val v = EpisodeVersion(
            key = "01JZ000000VERS000000000001", name = "11월 23일 · 초고", createdAt = 1L,
            blocks = listOf(VersionBlock(block.key, block.sceneKey, block.number, block.kind, block.speakerKey, listOf(TextRun.Plain("옛 글")))),
        )
        return doc.copy(versions = mapOf(unit to EpisodeVersionsFile(unit, listOf(v))))
    }

    @Test
    fun `versions are written by default, left out on request and always in backups`() {
        val doc = withVersion(Fixtures.example())
        val unit = doc.versions.keys.single()
        val on = Fixtures.export(doc)
        val onZip = ZipCodec.read(on)
        assertTrue(PackagePaths.versions(unit) in onZip.paths)
        val pkg = Fixtures.open(on)
        assertTrue(Features.VERSIONS in pkg.manifest.features.optional)
        assertEquals(doc.versions, pkg.doc.versions)

        val off = ZipCodec.read(Fixtures.export(doc, options = ProjectFileOptions(includeVersions = false)))
        assertTrue(off.paths.none { it.startsWith(PackagePaths.VERSIONS_DIR) })
        assertTrue(Fixtures.open(Fixtures.export(doc, options = ProjectFileOptions(includeVersions = false))).doc.versions.isEmpty())

        val backup = ProjectFileExport.backup(doc, RecorderLibrary(), BlobReader.NONE, null, "Civilight test", Fixtures.NOW)
        assertTrue(PackagePaths.versions(unit) in ZipCodec.read(backup.bytes).paths)

        // A file without versions (every 0.1.0 file) opens with none.
        assertTrue(Fixtures.open(v010()).doc.versions.isEmpty())
    }

    @Test
    fun `without the trash, trashed tags, versions of trashed episodes and marks on trashed blocks are not written`() {
        val base = Fixtures.example()
        val live = base.manuscript.liveBlocks
        val gone = live[10]
        val next = live[11]
        val goneUnit = base.index.numberingUnitKeyOfScene(gone.sceneKey)!!
        val tag = base.project.tags.first()
        val withRows = withVersion(base).let { d ->
            val trashedUnit = d.story.units.first { u -> !d.liveness.isLive(u) && d.versions.keys.none { it == u.key } }
            d.copy(
                project = d.project.copy(tags = d.project.tags.map { if (it.key == tag.key) it.copy(deletedAt = 1L, trashEntryKey = "TE_TAG") else it }),
                versions = d.versions + (trashedUnit.key to EpisodeVersionsFile(trashedUnit.key)),
                story = d.story.copy(foreshadows = listOf(Foreshadow("01JZ000000FRSH000000000001", "등불", MarkAnchor(gone.key)))),
                episodes = d.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == gone.key) it.copy(deletedAt = 2L, trashEntryKey = "TE_B") else it }) },
            ) to trashedUnit.key
        }
        val (doc, deadUnit) = withRows
        assertTrue(goneUnit.isNotEmpty())
        val pkg = Fixtures.open(Fixtures.export(doc))
        assertTrue(pkg.doc.project.tags.none { it.key == tag.key })
        assertFalse(deadUnit in pkg.doc.versions)
        assertEquals(doc.versions.keys - deadUnit, pkg.doc.versions.keys)
        assertEquals(next.key, pkg.doc.story.foreshadows.single().plant.blockKey)
        // With the trash everything is kept as it is.
        val all = Fixtures.open(Fixtures.export(doc, options = ProjectFileOptions(includeTrash = true)))
        assertEquals(doc.project.tags, all.doc.project.tags)
        assertEquals(gone.key, all.doc.story.foreshadows.single().plant.blockKey)
    }

    @Test
    fun `a new key keeps versions and marks`() {
        val doc = withVersion(Fixtures.example()).let { d ->
            val one = d.versions.values.single()
            // A 1-level project's numbering unit is the project key: that file follows the new key.
            d.copy(
                versions = d.versions + (d.key to EpisodeVersionsFile(d.key, one.versions)),
                story = d.story.copy(foreshadows = listOf(Foreshadow("01JZ000000FRSH000000000001", "등불", MarkAnchor(one.versions.single().blocks.single().key)))),
            )
        }
        val rekeyed = io.github.lenticualrstone.civilight.domain.library.importing.ProjectRekey.rekey(doc, "01JZ000000PRJT0000000000NEW")
        assertEquals(doc.versions.keys - doc.key + "01JZ000000PRJT0000000000NEW", rekeyed.versions.keys)
        assertEquals("01JZ000000PRJT0000000000NEW", rekeyed.versions.getValue("01JZ000000PRJT0000000000NEW").unitKey)
        assertEquals(doc.story.foreshadows, rekeyed.story.foreshadows)
    }

    @Test
    fun `a new project has the switch off`() {
        val created = NewProjectFlow.create(NewProjectDraft(name = "새 이야기"), RatingDefaults(), Fixtures.NOW, Fixtures.DEVICE, Fixtures.keys(3))
        assertEquals(false, created.doc.project.gameTools)
        assertEquals(false, Fixtures.open(Fixtures.export(created.doc)).doc.project.gameTools)
    }
}
