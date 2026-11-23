package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.model.game.GameData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import io.github.lenticualrstone.civilight.domain.library.format.FormatVersion
import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.domain.library.format.UpgradeChain
import io.github.lenticualrstone.civilight.domain.library.format.ZipCodec
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * LIB-56 / §8.1: one 「잿빛 새벽의 등불」 file per released format version under
 * `src/test/resources/format/<version>/`, opened with the current reader. The current-version file is
 * the one the onboarding example ships (LIB-17).
 *
 * Files are committed once and never regenerated: a reader change that stops them opening with the
 * same content is a broken promise 1 of LIB-50.
 */
class FormatRegressionTest {
    private val root: File = File(requireNotNull(javaClass.classLoader.getResource("format")) { "format/ resources missing" }.toURI())

    private fun versionFiles(): List<Pair<FormatVersion, File>> = root.listFiles().orEmpty()
        .filter { it.isDirectory }
        .mapNotNull { dir -> FormatVersion.parseOrNull(dir.name)?.let { v -> dir.listFiles().orEmpty().filter { it.name.endsWith(".civilight") }.map { v to it } } }
        .flatten()
        .sortedBy { it.first }

    /** What every regression file holds: the example project with its trash and its linked records. */
    private fun expected(): ProjectDoc = ExampleProject.build()

    @Test
    fun `there is a file for the current format version`() {
        val versions = versionFiles().map { it.first }
        assertTrue("no file for ${FormatVersion.CURRENT}: $versions", FormatVersion.CURRENT in versions)
        assertTrue(versions.all { it <= FormatVersion.CURRENT })
    }

    @Test
    fun `every released format file opens and writes back unchanged`() {
        for ((version, file) in versionFiles()) {
            val bytes = file.readBytes()
            val before = bytes.copyOf()
            val result = ProjectPackageReader.open(bytes, UpgradeChain.CURRENT)
            assertTrue("$file: $result", result is PackageOpen.Ready)
            val pkg = (result as PackageOpen.Ready).pkg
            assertEquals("$file", version.toString(), pkg.manifest.formatVersion)
            assertEquals("$file", emptyList<Any>(), pkg.failed)
            assertEquals("$file", emptyList<String>(), pkg.skippedOptionalFeatures)
            // Facts of the file itself (independent of later changes to the sample project).
            assertEquals("잿빛 새벽의 등불", pkg.doc.project.name)
            assertEquals(34, pkg.doc.manuscript.liveScenes.size)
            assertEquals(12, pkg.doc.cast.characters.size)
            // Read → write in the current format gives the same file units (nothing lost or altered).
            val again = ZipCodec.read(
                ProjectPackageWriter.write(
                    pkg.doc, pkg.recorder?.toLibrary(pkg.doc.key) ?: RecorderLibrary(),
                    options = ProjectFileOptions(includeTrash = Features.TRASH in pkg.manifest.features.optional),
                    writtenBy = "Civilight test", writtenAt = pkg.manifest.writtenAt,
                ),
            )
            val stored = ZipCodec.read(bytes)
            if (version == FormatVersion.CURRENT) {
                assertEquals("$file", stored.paths, again.paths)
                for (path in stored.paths.filter { it != PackagePaths.MANIFEST && it != PackagePaths.PROJECT }) assertArrayEquals("$file $path", stored[path], again[path])
                // (0.2.0, G-3) A file without `gameTools` gets it decided on read; nothing else of project.json changes.
                val storedProject = Json.parseToJsonElement(stored[PackagePaths.PROJECT]!!.toString(Charsets.UTF_8)).jsonObject
                val againProject = Json.parseToJsonElement(again[PackagePaths.PROJECT]!!.toString(Charsets.UTF_8)).jsonObject
                if ("gameTools" !in storedProject) {
                    assertEquals("$file", JsonPrimitive(GameData.present(pkg.doc, emptyList())), againProject["gameTools"])
                    assertEquals("$file", storedProject, JsonObject(againProject - "gameTools"))
                } else {
                    assertEquals("$file", storedProject, againProject)
                }
            }
            // The source file is never modified (promise 1).
            assertArrayEquals(before, bytes)
        }
    }

    @Test
    fun `the current-version file holds the example project`() {
        val file = versionFiles().last { it.first == FormatVersion.CURRENT }.second
        val pkg = Fixtures.open(file.readBytes())
        assertEquals(expected().copy(app = pkg.doc.app), pkg.doc)
        assertEquals(ExampleRecorder.projectFile().records.map { it.key }.toSet(), pkg.recorder!!.records.map { it.key }.toSet())
    }
}
