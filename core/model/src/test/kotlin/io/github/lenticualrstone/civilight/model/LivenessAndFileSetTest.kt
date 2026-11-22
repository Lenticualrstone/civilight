package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.FixtureR.HART
import io.github.lenticualrstone.civilight.model.FixtureR.RIEN
import io.github.lenticualrstone.civilight.model.FixtureR.blk
import io.github.lenticualrstone.civilight.model.FixtureR.ep
import io.github.lenticualrstone.civilight.model.FixtureR.sc
import io.github.lenticualrstone.civilight.model.FixtureR.withBlockChanged
import io.github.lenticualrstone.civilight.model.FixtureR.withUnit
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.doc.Liveness
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.doc.toFileSet
import io.github.lenticualrstone.civilight.model.file.AssetsIndexFile
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.json.UnknownProps
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.world.Place
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LivenessAndFileSetTest {
    private val base = FixtureR.doc()

    private fun ProjectDoc.trashCharacter(key: String, mark: Long? = 1L) =
        copy(cast = cast.copy(characters = cast.characters.map { if (it.key == key) it.copy(deletedAt = mark) else it }))

    private fun withOwen(doc: ProjectDoc) = doc.copy(
        cast = doc.cast.copy(
            characters = doc.cast.characters + Character(key = "C_OWEN", recordNo = 3, name = "오웬", order = "s"),
            relations = doc.cast.relations + Relation(
                key = "REL_NOA_OWEN",
                endA = EntityRef(RelationEndKind.CHARACTER, RIEN),
                endB = EntityRef(RelationEndKind.CHARACTER, "C_OWEN"),
            ),
        ),
    )

    @Test
    fun tc2008RelationComesBackWhenBothEndsAreBack() {
        val both = withOwen(base).trashCharacter(RIEN).trashCharacter("C_OWEN")
        assertFalse(both.liveness.isRelationLive("REL_NOA_OWEN"))
        val oneBack = both.trashCharacter(RIEN, mark = null)
        assertFalse(oneBack.liveness.isRelationLive("REL_NOA_OWEN"))
        val allBack = oneBack.trashCharacter("C_OWEN", mark = null)
        assertTrue(allBack.liveness.isRelationLive("REL_NOA_OWEN"))
    }

    @Test
    fun tc2009BlocksFollowTheirScene() {
        val d = base.trashCharacter(RIEN).withUnit(sc(1, 7, 3)) { it.copy(deletedAt = 1L) }
        val block = d.index.block(blk(42))!!
        assertFalse(d.liveness.isLive(block))
        assertNull("only the scene carries a mark", block.deletedAt)
        val sceneBack = d.withUnit(sc(1, 7, 3)) { it.copy(deletedAt = null) }
        assertTrue(sceneBack.liveness.isLive(sceneBack.index.block(blk(42))!!))
        // Episode trashed: its scenes and blocks are not live by containment.
        val epTrashed = base.withUnit(ep(1, 7)) { it.copy(deletedAt = 1L) }
        assertFalse(epTrashed.liveness.isUnitLive(sc(1, 7, 3)))
        assertFalse(epTrashed.liveness.isBlockLive(blk(43)))
    }

    @Test
    fun tc2024PinNeedsPlaceAndMap() {
        val doc = base.copy(
            world = WorldFile(places = listOf(Place(key = "PL", order = "V", name = "결사의 문"))),
            maps = mapOf("MAP" to WorldMap(key = "MAP", order = "V", name = "지도", baseKind = MapBaseKind.IMAGE.open(),
                pins = listOf(Pin(key = "PIN", placeKey = "PL", x = 0.3f, y = 0.4f)))),
        )
        fun pinLive(d: ProjectDoc) = d.liveness.isOverlayLive("MAP", d.maps.getValue("MAP").pins.single())
        assertTrue(pinLive(doc))
        val placeGone = doc.copy(world = doc.world.copy(places = doc.world.places.map { it.copy(deletedAt = 1L) }))
        assertFalse(pinLive(placeGone))
        val mapGoneToo = placeGone.copy(maps = placeGone.maps.mapValues { it.value.copy(deletedAt = 2L) })
        val placeBack = mapGoneToo.copy(world = doc.world)
        assertFalse(pinLive(placeBack))
        val mapBack = placeBack.copy(maps = doc.maps)
        assertTrue(pinLive(mapBack))
        assertEquals(0.3f, mapBack.maps.getValue("MAP").pins.single().x)
    }

    @Test
    fun tc2023RecordOfATrashedTargetIsNotLiveWithoutAMark() {
        val d = base.trashCharacter(HART)
        val r8 = d.changes.records.single { it.key == "R8" }
        assertNull(r8.deletedAt)
        assertFalse(d.liveness.isLive(r8))
        assertTrue(base.liveness.isLive(r8))
    }

    @Test
    fun imageUseFollowsItsOwner() {
        val use = ImageUse(key = "IU", ownerKind = ImageOwnerKind.CHARACTER.open(), ownerKey = HART,
            slot = ImageSlot.CHARACTER_PORTRAIT.open(), source = "preset:ash-dawn")
        val doc = base.copy(assets = AssetsIndexFile(imageUses = listOf(use)))
        assertTrue(doc.liveness.isLive(use))
        assertFalse(doc.trashCharacter(HART).liveness.isLive(use))
        assertEquals(use, doc.index.imageUse(ImageOwnerKind.CHARACTER, HART, ImageSlot.CHARACTER_PORTRAIT))
    }

    @Test
    fun ruleHelper() {
        assertTrue(Liveness.isLive(null, true, true))
        assertFalse(Liveness.isLive(1L, true))
        assertFalse(Liveness.isLive(null, true, false))
    }

    @Test
    fun splitPutsNonLiveRowsInTheTrashMirrorAndReassemblesExactly() {
        val doc = withOwen(base)
            .trashCharacter("C_OWEN")
            .withUnit(sc(1, 7, 2)) { it.copy(deletedAt = 3L, trashEntryKey = "TE_SCENE") }
            .withBlockChanged(blk(44)) { it.copy(deletedAt = 4L, trashEntryKey = "TE_BLOCKS") }
            .copy(trash = TrashIndexFile(listOf(
                TrashEntry(key = "TE_SCENE", kind = TrashKind.SCENE.open(), projectKey = "P", targetKeys = listOf(sc(1, 7, 2)),
                    deletedAt = 3L, purgeOn = LocalDate.of(2026, 12, 22)),
            )))
        val set = doc.toFileSet()
        val live = set.live
        assertTrue(live.storyIndex.units.none { it.key == sc(1, 7, 2) })
        assertTrue(live.episodes.getValue(ep(1, 7)).blocks.none { it.key == blk(44) || it.key == "B1-7-2" })
        assertTrue(live.cast.relations.none { it.key == "REL_NOA_OWEN" })
        val trash = set.trash!!.units
        assertEquals(listOf(sc(1, 7, 2)), trash.storyIndex.units.map { it.key })
        assertEquals(setOf(blk(44), "B1-7-2"), trash.episodes.getValue(ep(1, 7)).blocks.map { it.key }.toSet())
        assertEquals(listOf("REL_NOA_OWEN"), trash.cast.relations.map { it.key })
        assertEquals(listOf("C_OWEN"), trash.cast.characters.map { it.key })
        // Empty numbering units with a live scene still get a file (1-8 has none of its own blocks).
        assertTrue(ep(1, 8) in live.episodes)

        val files = set.encode()
        assertTrue(PackagePaths.PROJECT in files)
        assertTrue(PackagePaths.episode(ep(1, 8)) in files)
        assertTrue(PackagePaths.trash(PackagePaths.CAST) in files)
        assertTrue(PackagePaths.TRASH_INDEX in files)

        val decoded = ProjectFileSet.decode(files + ("extras/unknown.bin" to JsonPrimitive("x")))
        assertEquals(listOf("extras/unknown.bin"), decoded.unknownPaths)
        val back = decoded.files.toDoc()
        assertEquals(sortedDoc(doc), sortedDoc(back))
    }

    @Test
    fun trashedFieldDefinitionsLeaveProjectJson() {
        val doc = base.copy(project = base.project.copy(fieldDefs = base.project.fieldDefs.map { it.copy(deletedAt = 1L, trashEntryKey = "TE_FD") }))
        val set = doc.toFileSet()
        assertTrue(set.live.project.fieldDefs.isEmpty())
        val files = set.encode()
        assertTrue(PackagePaths.trash(PackagePaths.PROJECT) in files)
        val back = ProjectFileSet.decode(files).files.toDoc()
        assertEquals(doc.project.fieldDefs, back.project.fieldDefs)
        // Without trashed definitions no trash/project.json is written.
        assertFalse(PackagePaths.trash(PackagePaths.PROJECT) in base.toFileSet().encode())
    }

    @Test
    fun aDamagedFileLosesOnlyItsOwnContent() {
        val files = base.toFileSet().encode().toMutableMap()
        val damaged = PackagePaths.episode(ep(1, 6))
        files[damaged] = buildJsonObject { put("unitKey", JsonPrimitive(ep(1, 6))); put("blocks", JsonPrimitive("not a list")) }
        files[PackagePaths.CAST] = buildJsonObject { put("characters", buildJsonObject { }) }
        val result = ProjectFileSet.decode(files)
        assertEquals(setOf(damaged, PackagePaths.CAST), result.failedPaths.keys)
        val back = result.files.toDoc()
        assertTrue(back.cast.characters.isEmpty())
        assertTrue(ep(1, 6) !in back.episodes)
        assertEquals(base.episodes.getValue(ep(1, 7)), back.episodes.getValue(ep(1, 7)))
        // The counter lives in story/index.json, so it survives (C-32).
        assertEquals(base.story, back.story)
    }

    @Test(expected = IllegalArgumentException::class)
    fun projectJsonIsRequired() {
        ProjectFileSet.decode(base.toFileSet().encode() - PackagePaths.PROJECT)
    }

    /** Equality up to the order of rows in lists (files are sets of keyed rows, §1.5). */
    private fun sortedDoc(d: ProjectDoc) = d.copy(
        story = d.story.copy(units = d.story.units.sortedBy { it.key }),
        episodes = d.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.sortedBy { it.key }) }.toSortedMap(),
        cast = d.cast.copy(
            characters = d.cast.characters.sortedBy { it.key },
            relations = d.cast.relations.sortedBy { it.key },
        ),
        changes = d.changes.copy(records = d.changes.records.sortedBy { it.key }),
    )

    @Test
    fun everyFileModelObjectHasAnExtensionSlot() {
        val roots: List<SerialDescriptor> = listOf(
            io.github.lenticualrstone.civilight.model.project.Project.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.StoryIndexFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.EpisodeFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.CastFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.WorldFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.ChangesFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.ChecksFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.TombstonesFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.DeletionsFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.AssetsIndexFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.TrashIndexFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.RecorderFile.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.FileManifest.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.file.EncryptionHeader.serializer().descriptor,
            WorldMap.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.canvas.Canvas.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.canvas.Page.serializer().descriptor,
            // Variants behind tagged unions.
            io.github.lenticualrstone.civilight.model.text.TextRun.Plain.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.text.TextRun.Paren.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.text.TextRun.Link.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.Anchor.UnitStart.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.Anchor.UnitEnd.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.Anchor.WorldEvent.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.ChangeTarget.Field.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.ChangeTarget.DisplayName.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.ChangeTarget.RelationSlot.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.ChangeTarget.RegionShape.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.ChangeTarget.TermReveal.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.story.SceneTime.Relative.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.story.SceneTime.Direct.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.story.SceneTime.Flashback.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.cast.FieldPayload.Choice.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.cast.FieldPayload.Date.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.cast.FieldPayload.Anniversary.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.cast.FieldPayload.Link.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.text.DocBlock.serializer().descriptor,
            io.github.lenticualrstone.civilight.model.change.RegionShapeValue.serializer().descriptor,
        )
        val missing = mutableSetOf<String>()
        val seen = mutableSetOf<String>()
        fun walk(d: SerialDescriptor) {
            val name = d.serialName.removeSuffix("?")
            when (d.kind) {
                StructureKind.CLASS -> {
                    if (!seen.add(name)) return
                    if (!UnknownProps.hasExtSlot(d) && !name.endsWith(".Blob")) missing += name
                    for (i in 0 until d.elementsCount) walk(d.getElementDescriptor(i))
                }
                StructureKind.LIST -> walk(d.getElementDescriptor(0))
                StructureKind.MAP -> if (!name.endsWith("JsonObject")) walk(d.getElementDescriptor(1))
                else -> Unit
            }
        }
        roots.forEach(::walk)
        assertTrue("classes without ext: $missing", missing.isEmpty())
        assertTrue(seen.size > 50)
    }

    @Test
    fun unknownPropertiesInNestedObjectsAreKept() {
        val text = """{"key":"K","name":"n","format":"serial","protections":{"hidden":true,"newGate":1}}"""
        val p = CivilightJson.decode(io.github.lenticualrstone.civilight.model.project.Project.serializer(), text)
        assertEquals(JsonPrimitive(1), p.protections.ext!!["_unknown"]!!.jsonObject["newGate"])
        val out = CivilightJson.encodeElement(io.github.lenticualrstone.civilight.model.project.Project.serializer(), p).jsonObject
        assertEquals(buildJsonObject {
            put("hidden", JsonPrimitive(true)); put("captureBlock", JsonPrimitive(false))
            put("nameMask", JsonPrimitive(false)); put("recorderLinkMask", JsonPrimitive(false)); put("newGate", JsonPrimitive(1))
        }, out["protections"])
    }
}
