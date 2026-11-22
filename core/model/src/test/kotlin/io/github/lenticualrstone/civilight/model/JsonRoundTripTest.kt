package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValueState
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RegionShapeValue
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.file.AssetsIndexFile
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.ChangesFile
import io.github.lenticualrstone.civilight.model.file.ChecksFile
import io.github.lenticualrstone.civilight.model.file.DeletionsFile
import io.github.lenticualrstone.civilight.model.file.EncryptionHeader
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.FileManifest
import io.github.lenticualrstone.civilight.model.file.PackageLockType
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.file.TombstonesFile
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.PointRefKind
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Hand-written §7-shaped JSON → decode → encode must give the same JSON tree, including unknown
 * properties, unknown enum values and unknown run, anchor, target, block, payload and time kinds.
 */
class JsonRoundTripTest {
    private fun text(name: String): String =
        requireNotNull(javaClass.getResource("/json/$name")) { "missing resource $name" }.readText()

    private fun <T> roundTrip(name: String, serializer: KSerializer<T>): T {
        val source = text(name)
        val value = CivilightJson.decode(serializer, source)
        val expected = CivilightJson.json.parseToJsonElement(source)
        val actual = CivilightJson.encodeElement(serializer, value)
        assertEquals("round trip of $name", expected, actual)
        // A second pass from the encoded text is stable too.
        assertEquals(value, CivilightJson.decode(serializer, CivilightJson.encode(serializer, value)))
        return value
    }

    private fun unknownOf(ext: JsonObject?): JsonObject = ext!!["_unknown"]!!.jsonObject

    @Test
    fun projectJson() {
        val p = roundTrip("project.json", Project.serializer())
        assertTrue(p.format.isUnknown)
        assertEquals("anthology", p.format.raw)
        assertEquals(Rating.R15, p.rating.known)
        assertEquals(DayBoundary.MIDNIGHT, p.sceneTime.dayBoundary.known)
        assertEquals(ClockTime.of(6, 30), p.sceneTime.bandStarts.startOf(Band.MORNING))
        assertEquals(40.5, p.travelModes[1].kmPerDay, 0.0)
        assertEquals(1_795_304_460_000L, p.updatedAt)
        assertEquals(JsonPrimitive(3), p.ext!!["vendor"]!!.jsonObject["theme"])
        assertTrue("futureSetting" in unknownOf(p.ext))
        assertTrue("futureFlag" in unknownOf(p.fieldDefs[3].ext))
        assertTrue("moon" in unknownOf(p.worldTime.calendar!!.months[1].ext))
        assertEquals(8, p.counters.nextCharacterRecordNo)
    }

    @Test
    fun projectDefaultsAreWrittenForRequiredProperties() {
        val minimal = Project(key = "K", name = "새 프로젝트")
        val json = CivilightJson.encodeElement(Project.serializer(), minimal).jsonObject
        for (name in listOf("format", "rating", "protections", "structure", "worldTime", "sceneTime", "script", "cast",
            "travelModes", "termMatching", "fieldBundleAtCreation", "counters", "fieldDefs", "tags", "relationKinds",
            "createdAt", "updatedAt", "updatedBy")) {
            assertTrue("$name is written", name in json)
        }
        assertEquals(JsonPrimitive("serial"), json["format"])
        assertTrue("oneLiner" !in json && "ext" !in json && "defaultTravelModeKey" !in json)
        assertEquals(minimal, CivilightJson.decodeElement(Project.serializer(), json))
    }

    @Test
    fun storyIndexJson() {
        val f = roundTrip("story-index.json", StoryIndexFile.serializer())
        val s9 = f.units[2]
        val time = s9.time as SceneTime.Relative
        assertEquals("01JDUN000000000000000000S8", time.baseKey)
        assertEquals(ClockTime.of(5, 40), time.clock)
        assertEquals(IntSpan(11, 15), s9.transitionSnapshot!!.dayRange)
        assertTrue(s9.transitionRuns!![1] is TextRun.Link)
        assertTrue(f.units[3].time is SceneTime.Direct)
        assertTrue(f.units[4].time is SceneTime.Flashback)
        assertEquals("dream", (f.units[5].time as SceneTime.Unknown).type)
        assertTrue(f.units[5].place!!.kind.isUnknown)
        assertEquals(SceneTime.None, f.units[0].time)
        assertEquals(58, f.units[1].nextBlockNumber)
    }

    @Test
    fun episodeJson() {
        val f = roundTrip("episode.json", EpisodeFile.serializer())
        val dialogue = f.blocks[0]
        assertEquals(BlockKind.DIALOGUE, dialogue.kind.known)
        assertEquals(VoiceMode.FIXED, dialogue.voice!!.mode.known)
        assertTrue(dialogue.runs[0] is TextRun.Paren)
        assertEquals(TextRun.Break, dialogue.runs[2])
        assertEquals(LinkTargetKind.TERM, (dialogue.runs[3] as TextRun.Link).target.kind.known)
        assertEquals("ruby", (dialogue.runs[4] as TextRun.Unknown).kind)
        assertEquals("등불", dialogue.runs[4].shownText)
        assertEquals(CueKind.SCREEN_EFFECT, f.blocks[1].cues[1].kind.known)
        assertTrue(f.blocks[1].cues[2].kind.isUnknown)
        assertEquals(2, f.blocks[2].options.size)
        val unknownBlock = f.blocks[4]
        assertTrue(unknownBlock.kind.isUnknown)
        assertTrue(unknownBlock.hasContent())
        assertTrue("puzzle" in unknownOf(unknownBlock.ext))
        assertTrue(f.discardedVoiceIds[1].reason.isUnknown)
    }

    @Test
    fun castJson() {
        val f = roundTrip("cast.json", CastFile.serializer())
        val hart = f.characters[0]
        val fields = hart.fields
        assertEquals(FieldPayload.Texts(listOf("등불 결사")), fields["01JDFD0000000000000000000A"]!!.payload)
        assertEquals("1.5", (fields["01JDFD0000000000000000000C"]!!.payload as FieldPayload.Number).value)
        assertTrue(fields["01JDFD0000000000000000000G"]!!.payload is FieldPayload.Rich)
        assertEquals(FieldValueState.UNKNOWN, fields["01JDFD0000000000000000000J"]!!.state.known)
        assertTrue(fields["01JDFD0000000000000000000M"]!!.payload is FieldPayload.Unknown)
        assertTrue(fields["01JDFD0000000000000000000N"]!!.state.isUnknown)
        assertEquals(DocBlockKind.MEMO, hart.speechBody[1].kind.known)
        assertTrue(hart.speechBody[2].kind.isUnknown)
        assertTrue("pronouns" in unknownOf(f.characters[1].ext))
        assertEquals(RelationEndKind.CHARACTER, f.relations[0].endA.kind.known)
        assertTrue(f.relations[1].endB.kind.isUnknown)
        assertEquals(PointRefKind.UNIT_END, f.archiveDocs[0].unlockPoint!!.kind.known)
        assertTrue(f.archiveDocs[1].unlockPoint!!.kind.isUnknown)
    }

    @Test
    fun worldJson() {
        val f = roundTrip("world.json", WorldFile.serializer())
        assertEquals(listOf("오리지늄", "빛돌"), f.terms[0].aliases)
        assertTrue(f.terms[0].related[1].kind.isUnknown)
        assertTrue("confidence" in unknownOf(f.terms[0].rejections[1].ext))
        assertTrue(f.events[2].kind.isUnknown)
        assertTrue("regions" in unknownOf(f.ext))
    }

    @Test
    fun changesJson() {
        val f = roundTrip("changes.json", ChangesFile.serializer())
        val r = f.records
        assertTrue(r[0].target is ChangeTarget.Field)
        assertEquals(RelationSlotName.MIND_A, (r[1].target as ChangeTarget.RelationSlot).slot.known)
        assertEquals(AnchorState.ORPHANED, r[1].anchorState.known)
        assertEquals(Anchor.AtBlock("01JDBK00000000000000000043"), r[1].originalAnchor)
        assertEquals(JsonPrimitive("꼬마"), r[2].value)
        assertTrue(r[3].anchor is Anchor.WorldEvent)
        assertEquals(Anchor.Unplaced, r[4].anchor)
        assertNull(r[4].value)
        assertEquals(3, r[5].valueAs(RegionShapeValue.serializer())!!.vertices.size)
        assertEquals("sceneMiddle", (r[5].anchor as Anchor.Unknown).kind)
        assertEquals("mood", (r[6].target as ChangeTarget.Unknown).kind)
        assertNull(r[6].target.targetId)
        assertTrue("offset" in unknownOf((r[6].anchor as Anchor.AtBlock).ext))
        assertTrue(r[6].anchorState.isUnknown)
    }

    @Test
    fun mapJson() {
        val m = roundTrip("map.json", WorldMap.serializer())
        assertTrue(m.style!!.isUnknown)
        assertEquals(Point(0.25f, 0.75f).x, m.pins[0].x)
        assertTrue(m.routes[0].points[0].isPin)
        assertTrue(m.stamps[1].kind.isUnknown)
        assertEquals(Terrain.FOREST, m.hex!!.overrides[1]!!.known)
        assertTrue(m.hex.cells[1].isUnknown)
        assertTrue(m.generation!!.steps[1].step.isUnknown)
        assertTrue("weather" in unknownOf(m.ext))
    }

    @Test
    fun canvasAndPageJson() {
        val c = roundTrip("canvas.json", Canvas.serializer())
        assertTrue(c.nodes[2].kind.isUnknown)
        assertTrue("style" in unknownOf(c.edges[0].ext))
        val p = roundTrip("page.json", Page.serializer())
        assertEquals(LinkTargetKind.PAGE, (p.blocks[1].runs[0] as TextRun.Link).target.kind.known)
        assertEquals(JsonPrimitive(true), p.ext!!["pinned"])
    }

    @Test
    fun assetsIndexJson() {
        val f = roundTrip("assets-index.json", AssetsIndexFile.serializer())
        assertEquals("webp", f.blobs[0].fileExt)
        assertEquals("aa11.webp", f.blobs[0].fileName)
        assertTrue(f.blobs[1].source.isUnknown)
        assertEquals(1.5f, f.imageUses[0].cropFor(io.github.lenticualrstone.civilight.model.asset.Ratio.R4X5)!!.zoom)
        assertEquals("ash-dawn", f.imageUses[1].presetId)
        assertTrue(f.imageUses[2].slot.isUnknown)
        assertTrue(f.scriptAssets[2].kind.isUnknown)
    }

    @Test
    fun checksTombstonesDeletionsTrashJson() {
        val checks = roundTrip("checks.json", ChecksFile.serializer())
        assertTrue(checks.acknowledgements[1].kind.isUnknown)
        val tomb = roundTrip("tombstones.json", TombstonesFile.serializer())
        assertTrue(tomb.nameTags[2].state.isUnknown)
        assertEquals("시장 꼬마", tomb.mergeRecords[0].mergedOriginal.jsonObject["name"]!!.let { (it as JsonPrimitive).content })
        val deletions = roundTrip("deletions.json", DeletionsFile.serializer())
        assertTrue("reason" in unknownOf(deletions.entries[1].ext))
        val trash = roundTrip("trash-index.json", TrashIndexFile.serializer())
        assertEquals(TrashKind.BLOCK_GROUP, trash.entries[0].kind.known)
        assertEquals(LocalDate.of(2026, 12, 22), trash.entries[0].purgeOn)
        assertTrue(trash.entries[2].kind.isUnknown)
    }

    @Test
    fun recorderJson() {
        val f = roundTrip("recorder.json", RecorderFile.serializer())
        assertEquals(PlacementKind.CARD_PARAGRAPH, f.records[0].placements[1].kind.known)
        assertTrue(f.records[1].kind.isUnknown)
        val library = f.toLibrary(impliedProjectKey = "01JDPROJ00000000000000000A")
        assertEquals(2, library.links.size)
        assertTrue(library.links.all { it.projectKey == "01JDPROJ00000000000000000A" && it.recordKey == "01JDRR0000000000000000000A" })
        // Writing the project file again leaves project keys out and gives the same JSON.
        val again = RecorderFile.forProject(library, "01JDPROJ00000000000000000A", library.records, includeAudio = true)
        assertEquals(
            CivilightJson.json.parseToJsonElement(text("recorder.json")),
            CivilightJson.encodeElement(RecorderFile.serializer(), again),
        )
        // The library file keeps project keys.
        val libraryFile = RecorderFile.library(library)
        val decoded = CivilightJson.decode(RecorderFile.serializer(), CivilightJson.encode(RecorderFile.serializer(), libraryFile))
        assertEquals(library.links, decoded.toLibrary().links)
    }

    @Test
    fun manifestAndEncryptionJson() {
        val m = roundTrip("manifest.json", FileManifest.serializer())
        assertEquals("01JDPROJ00000000000000000A", m.project!!.key)
        assertEquals(Rating.R15, m.project.rating.known)
        assertTrue("signature" in unknownOf(m.ext))
        val e = roundTrip("encryption.json", EncryptionHeader.serializer())
        assertEquals(PackageLockType.RECOVERY, e.lockType.known)
    }

    @Test
    fun unknownEnumValueOfAProjectFormatIsKeptVerbatim() {
        val p = CivilightJson.decode(Project.serializer(), text("project.json"))
        val changed = p.copy(format = ProjectFormat.SHORT.open())
        val json = CivilightJson.encodeElement(Project.serializer(), changed).jsonObject
        assertEquals(JsonPrimitive("short"), json["format"])
    }
}
