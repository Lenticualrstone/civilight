package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

class ExampleProjectRoundTripTest {
    private val doc = ExampleProject.build()

    /** JSON element → text → JSON element, as a file on disk would go. */
    private fun throughText(files: Map<String, JsonElement>): Map<String, JsonElement> = files.mapValues { (_, element) ->
        CivilightJson.json.parseToJsonElement(CivilightJson.json.encodeToString(JsonElement.serializer(), element))
    }

    @Test
    fun wholeProjectSurvivesSplitEncodeDecode() {
        val files = throughText(ProjectFileSet.split(doc).encode())
        val decoded = ProjectFileSet.decode(files)
        assertTrue("failed: ${decoded.failedPaths}", decoded.failedPaths.isEmpty())
        assertTrue("unknown: ${decoded.unknownPaths}", decoded.unknownPaths.isEmpty())
        val back = decoded.files.toDoc(app = doc.app)
        assertEquals(doc.project, back.project)
        assertEquals(doc.story, back.story)
        assertEquals(doc.episodes, back.episodes)
        assertEquals(doc.cast, back.cast)
        assertEquals(doc.world, back.world)
        assertEquals(doc.maps, back.maps)
        assertEquals(doc.changes, back.changes)
        assertEquals(doc.checks, back.checks)
        assertEquals(doc.tombstones, back.tombstones)
        assertEquals(doc.deletions, back.deletions)
        assertEquals(doc.canvases, back.canvases)
        assertEquals(doc.pages, back.pages)
        assertEquals(doc.assets, back.assets)
        assertEquals(doc.trash, back.trash)
        assertEquals(doc, back)
    }

    @Test
    fun liveFilesHoldOnlyLiveRowsAndTheTrashMirrorTheRest() {
        val set = ProjectFileSet.split(doc)
        val paths = set.encode().keys
        assertTrue(PackagePaths.TRASH_INDEX in paths)
        assertTrue(PackagePaths.trash(PackagePaths.CAST) in paths)
        assertTrue(PackagePaths.trash(PackagePaths.episode(K.EPISODE_1_6)) in paths)
        assertTrue(PackagePaths.trash(PackagePaths.episode(K.EPISODE_1_5)) in paths)
        // 노아, his relation and records, the trashed scene and its blocks are only in the mirror.
        val trash = set.trash!!.units
        assertTrue(set.live.cast.characters.none { it.key == K.NOAH })
        assertEquals(listOf(K.NOAH), trash.cast.characters.map { it.key })
        assertEquals(listOf(K.REL_OWEN_NOAH), trash.cast.relations.map { it.key })
        assertEquals(setOf(K.N1, K.N2, K.RECORD_TRASHED), trash.changes.records.map { it.key }.toSet())
        assertEquals(listOf(K.SCENE_1_6_5), trash.storyIndex.units.map { it.key })
        assertEquals(listOf(30, 31, 32), trash.episodes.getValue(K.EPISODE_1_6).blocks.map { it.number })
        assertEquals(listOf(8, 9), trash.episodes.getValue(K.EPISODE_1_5).blocks.map { it.number })
        // Every numbering unit with a live scene has its file, even when empty.
        val episodeFiles = paths.filter { it.startsWith("story/") && it != PackagePaths.STORY_INDEX }.toSet()
        val expected = listOf(1 to 1, 1 to 2, 1 to 3, 1 to 4, 1 to 5, 1 to 6, 1 to 7, 1 to 8, 1 to 9, 1 to 10, 2 to 3, 2 to 5)
            .map { (c, e) -> PackagePaths.episode(K.episode(c, e)) }.toSet()
        assertEquals(expected, episodeFiles)
    }

    @Test
    fun eachFileUnitIsCanonicalJson() {
        // Encoding the decoded files again gives the same JSON (no lossy fields).
        val files = throughText(ProjectFileSet.split(doc).encode())
        val again = ProjectFileSet.split(ProjectFileSet.decode(files).files.toDoc(app = doc.app)).encode()
        assertEquals(files.keys, again.keys)
        for ((path, element) in files) assertEquals(path, element, again.getValue(path))
    }

    @Test
    fun recorderSliceRoundTrips() {
        val library = ExampleRecorder.library()
        val file = RecorderFile.library(library, includeTrashed = true)
        val text = CivilightJson.encode(RecorderFile.serializer(), file)
        val back = CivilightJson.decode(RecorderFile.serializer(), text).toLibrary()
        assertEquals(library.records, back.records)
        assertEquals(library.links.toSet(), back.links.toSet())
        assertEquals(library.placements, back.placements)

        val project = ExampleRecorder.projectFile()
        assertEquals(
            listOf(K.RECORDER_R0, K.RECORDER_R1, K.RECORDER_R2, K.RECORDER_NOAH),
            project.records.map { it.key },
        )
        assertTrue(project.records.all { r -> r.links.all { it.projectKey == null } && r.placements.all { it.projectKey == null } })
        val projectBack = CivilightJson.decode(RecorderFile.serializer(), CivilightJson.encode(RecorderFile.serializer(), project))
        assertEquals(project, projectBack)
        assertEquals(
            library.links.filter { it.recordKey in project.records.map { r -> r.key } }.toSet(),
            projectBack.toLibrary(impliedProjectKey = K.PROJECT).links.toSet(),
        )
    }
}
