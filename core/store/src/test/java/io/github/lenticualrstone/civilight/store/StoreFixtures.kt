package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.app.ProjectActivity
import io.github.lenticualrstone.civilight.model.app.ProjectActivityFile
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.app.ProjectViewFile
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.cast.ArchiveDoc
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.cast.VoiceLine
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.AssetsIndexFile
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.ChangesFile
import io.github.lenticualrstone.civilight.model.file.ChecksFile
import io.github.lenticualrstone.civilight.model.file.DeletionsFile
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.file.TombstonesFile
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapSketch
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.TravelMode
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.ScriptAsset
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.story.TravelSegmentOverride
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.world.RejectionContainerKind
import io.github.lenticualrstone.civilight.model.world.Term
import io.github.lenticualrstone.civilight.model.world.TermRejection
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.TimelineEvent
import io.github.lenticualrstone.civilight.store.io.WriteFaults
import io.github.lenticualrstone.civilight.store.project.ProjectLoader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.io.OutputStream
import java.time.LocalDate

/** Test data: one project that fills every file unit, live and trashed rows mixed. */
object Fx {
    const val P = "01JDPROJECT0000000000000AA"
    const val T0 = 1_763_769_600_000L // 2025-11-22T00:00:00Z

    fun richDoc(key: String = P): ProjectDoc {
        val trashScene = TrashEntry(key = "TE_S3", kind = TrashKind.SCENE.open(), projectKey = key, targetKeys = listOf("S3"),
            deletedAt = T0 + 5, purgeOn = LocalDate.of(2025, 12, 22))
        val trashChar = TrashEntry(key = "TE_C2", kind = TrashKind.CHARACTER.open(), projectKey = key, targetKeys = listOf("C2"),
            deletedAt = T0 + 6, purgeOn = LocalDate.of(2025, 12, 22))
        return ProjectDoc(
            project = Project(
                key = key, name = "잿빛 새벽의 등불", oneLiner = "꺼져 가는 하층 구역",
                // A 0.2.0 project always carries the switch (G-1); 0.1.0 upgrades are V010UpgradeTest.
                gameTools = false,
                travelModes = TravelMode.defaults(newKey = sequence().iterator()::next),
                fieldDefs = listOf(
                    FieldDef(key = "FD_AFF", entityKind = EntityKind.CHARACTER.open(), name = "소속", type = FieldType.SHORT_TEXT.open(), order = "V"),
                    FieldDef(key = "FD_OLD", entityKind = EntityKind.CHARACTER.open(), name = "옛 항목", type = FieldType.SHORT_TEXT.open(), order = "k",
                        deletedAt = T0 + 1, trashEntryKey = "TE_FD"),
                ),
                tags = listOf(Tag(key = "TAG1", name = "주연", order = "V")),
                createdAt = T0, updatedAt = T0, updatedBy = "DEV-A",
            ),
            story = StoryIndexFile(
                units = listOf(
                    StoryUnit(key = "CH1", level = 0, order = "V"),
                    StoryUnit(key = "E1", parentKey = "CH1", level = 1, order = "V"),
                    StoryUnit(key = "S1", parentKey = "E1", level = 2, order = "V", title = "꺼진 등불"),
                    StoryUnit(key = "S2", parentKey = "E1", level = 2, order = "k"),
                    StoryUnit(key = "E2", parentKey = "CH1", level = 1, order = "k"),
                    StoryUnit(key = "S3", parentKey = "E2", level = 2, order = "V", deletedAt = T0 + 5, trashEntryKey = "TE_S3"),
                ),
            ),
            episodes = mapOf(
                "E1" to EpisodeFile(
                    unitKey = "E1",
                    blocks = listOf(
                        block("B1", "S1", "V", 1, "저기… 불빛이 보여요.", speaker = "C1"),
                        block("B2", "S1", "k", 2, "지운 줄").copy(deletedAt = T0 + 3, trashEntryKey = "TE_BLK"),
                        block("B3", "S2", "V", 3, "…오리지늄 등불이 한 번 깜박였다."),
                    ),
                    discardedVoiceIds = listOf(DiscardedVoiceId(id = "RIE_0101_002", blockKey = "B2", discardedAt = T0 + 3, reason = DiscardReason.DELETED.open())),
                ),
                "E2" to EpisodeFile(unitKey = "E2", blocks = listOf(block("B4", "S3", "V", 4, "지운 씬의 줄"))),
            ),
            cast = CastFile(
                characters = listOf(
                    Character(key = "C1", recordNo = 1, name = "리엔", order = "V", voiceCode = "RIE"),
                    Character(key = "C2", recordNo = 2, name = "노아", order = "k", deletedAt = T0 + 6, trashEntryKey = "TE_C2"),
                ),
                relations = listOf(Relation(key = "REL", endA = EntityRef(RelationEndKind.CHARACTER, "C1"), endB = EntityRef(RelationEndKind.CHARACTER, "C2"))),
                voiceLines = listOf(VoiceLine(key = "VL1", characterKey = "C1", order = "V", title = "인사")),
                archiveDocs = listOf(ArchiveDoc(key = "AD1", characterKey = "C2", order = "V", title = "일지")),
            ),
            world = WorldFile(
                places = listOf(
                    Place(key = "PL1", order = "V", name = "결사의 문"),
                    Place(key = "PL2", order = "k", name = "잿더미 시장", deletedAt = T0 + 7, trashEntryKey = "TE_PL2"),
                ),
                terms = listOf(Term(key = "T1", order = "V", name = "오리지늄 등불", aliases = listOf("빛돌"),
                    rejections = listOf(TermRejection(key = "REJ1", containerKind = RejectionContainerKind.BLOCK.open(), containerKey = "B3", matchedText = "등불", occurrence = 1)))),
                factions = listOf(Faction(key = "F1", order = "V", name = "등불 결사")),
                events = listOf(TimelineEvent(key = "EV1", order = "V", kind = EventKind.WORLD.open(), title = "대정전")),
            ),
            maps = mapOf(
                "MAP1" to WorldMap(
                    key = "MAP1", order = "V", name = "하층 구역", baseKind = MapBaseKind.IMAGE.open(), baseImageHash = HASH_MAP,
                    sketch = MapSketch(photoHash = HASH_SKETCH, corners = listOf(Point(0f, 0f), Point(1f, 0f), Point(1f, 1f), Point(0f, 1f))),
                    pins = listOf(Pin(key = "PIN1", placeKey = "PL1", x = 0.25f, y = 0.5f), Pin(key = "PIN2", placeKey = "PL2", x = 0.75f, y = 0.5f)),
                ),
            ),
            changes = ChangesFile(
                records = listOf(
                    ChangeRecord(key = "R1", target = ChangeTarget.Field(EntityKind.CHARACTER, "C1", "FD_AFF"), value = JsonPrimitive("등불 결사"),
                        anchor = Anchor.AtBlock("B1"), createdAt = T0),
                    ChangeRecord(key = "R2", target = ChangeTarget.DisplayName("C2"), value = JsonPrimitive("???"), anchor = Anchor.UnitStart("E1"), createdAt = T0),
                ),
            ),
            checks = ChecksFile(
                acknowledgements = listOf(Acknowledgement(key = "ACK1", checkType = "term.beforeReveal", subjectKeys = listOf("LK1"), fingerprint = "f1", kind = AckKind.MATCH.open())),
                travelOverrides = listOf(TravelSegmentOverride(key = "TO1", characterKey = "C1", fromSceneKey = "S1", toSceneKey = "S2", modeKey = "TM1")),
            ),
            tombstones = TombstonesFile(nameTags = listOf(NameTag(key = "C_OLD", kind = NameTagKind.CHARACTER.open(), name = "시장 꼬마", voiceCode = "KID", state = NameTagState.PURGED.open(), purgedAt = T0 + 9))),
            deletions = DeletionsFile(entries = listOf(DeletionLogEntry(key = "C_OLD", kind = "character", deletedAt = T0 + 9))),
            canvases = mapOf("CV1" to Canvas(key = "CV1", order = "V", name = "관계도",
                nodes = listOf(CanvasNode(key = "N1", kind = CanvasNodeKind.CHARACTER.open(), refKey = "C1", x = 10f, y = 20f, z = "V")))),
            pages = mapOf(
                "PG1" to Page(key = "PG1", order = "V", name = "세계관 메모"),
                "PG2" to Page(key = "PG2", order = "k", name = "지운 페이지", deletedAt = T0 + 8, trashEntryKey = "TE_PG2"),
            ),
            assets = AssetsIndexFile(
                blobs = listOf(Blob(hash = HASH_PORTRAIT, fileExt = "png", mime = "image/png", bytes = 3, source = BlobSource.GALLERY.open(), importedAt = T0)),
                imageUses = listOf(
                    ImageUse(key = "IU1", ownerKind = ImageOwnerKind.CHARACTER.open(), ownerKey = "C1", slot = ImageSlot.CHARACTER_PORTRAIT.open(), source = HASH_PORTRAIT),
                    ImageUse(key = "IU2", ownerKind = ImageOwnerKind.CHARACTER.open(), ownerKey = "C2", slot = ImageSlot.CHARACTER_PORTRAIT.open(), source = "preset:ash-dawn"),
                ),
                scriptAssets = listOf(ScriptAsset(key = "SA1", kind = ScriptAssetKind.BGM.open(), name = "새벽", fileName = "bgm_dawn", fileHash = HASH_BGM)),
            ),
            trash = TrashIndexFile(entries = listOf(trashScene, trashChar)),
            app = ProjectAppState(
                activity = ProjectActivityFile(activity = ProjectActivity(lastEditedBlockKey = "B1", lastEditedOffset = 3, lastOpenedAt = T0)),
                view = ProjectViewFile(worldTab = "maps"),
            ),
            unknownFiles = mapOf(
                "labs/notes.json" to ProjectLoader.unknownFile("labs/notes.json", """{"future":true,"n":[1,2]}""".toByteArray()),
                "extras/sketch.bin" to ProjectLoader.unknownFile("extras/sketch.bin", byteArrayOf(0, 1, 2, -1, -128, 127)),
            ),
        )
    }

    const val HASH_PORTRAIT = "1111111111111111111111111111111111111111111111111111111111111111"
    const val HASH_MAP = "2222222222222222222222222222222222222222222222222222222222222222"
    const val HASH_SKETCH = "3333333333333333333333333333333333333333333333333333333333333333"
    const val HASH_BGM = "4444444444444444444444444444444444444444444444444444444444444444"

    fun block(key: String, scene: String, order: String, number: Int, text: String, speaker: String? = null) = Block(
        key = key, sceneKey = scene, order = order, number = number,
        kind = (if (speaker != null) BlockKind.DIALOGUE else BlockKind.NARRATION).open(),
        speakerKey = speaker, runs = listOf(TextRun.Plain(text)),
    )

    private fun sequence() = generateSequence(1) { it + 1 }.map { "TM$it" }

    /** Equality up to row order inside lists (files are sets of keyed rows, §1.5). */
    fun normalized(d: ProjectDoc): ProjectDoc = d.copy(
        project = d.project.copy(fieldDefs = d.project.fieldDefs.sortedBy { it.key }),
        story = d.story.copy(units = d.story.units.sortedBy { it.key }),
        episodes = d.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.sortedBy { it.key }) }.toSortedMap(),
        cast = d.cast.copy(
            characters = d.cast.characters.sortedBy { it.key },
            relations = d.cast.relations.sortedBy { it.key },
            voiceLines = d.cast.voiceLines.sortedBy { it.key },
            archiveDocs = d.cast.archiveDocs.sortedBy { it.key },
        ),
        world = d.world.copy(
            places = d.world.places.sortedBy { it.key },
            terms = d.world.terms.sortedBy { it.key },
            factions = d.world.factions.sortedBy { it.key },
            events = d.world.events.sortedBy { it.key },
        ),
        maps = d.maps.mapValues { (_, m) -> m.copy(pins = m.pins.sortedBy { it.key }) }.toSortedMap(),
        changes = d.changes.copy(records = d.changes.records.sortedBy { it.key }),
        assets = d.assets.copy(
            blobs = d.assets.blobs.sortedBy { it.hash },
            imageUses = d.assets.imageUses.sortedBy { it.key },
            scriptAssets = d.assets.scriptAssets.sortedBy { it.key },
        ),
        canvases = d.canvases.toSortedMap(),
        pages = d.pages.toSortedMap(),
        unknownFiles = d.unknownFiles.toSortedMap(),
    )
}

/** A test environment: timers on the test scheduler, file work run at once, a fixed device id. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.testEnv(
    clock: AppClock = ManualClock(Fx.T0),
    timing: StoreTiming = StoreTiming(),
): StoreEnvironment = StoreEnvironment(
    scope = backgroundScope,
    io = UnconfinedTestDispatcher(testScheduler),
    clock = clock,
    deviceId = DeviceId.fixed("DEV-TEST"),
    timing = timing,
)

/** Records every committed write and delete, relative to [root]. */
class WriteLog(private val root: File) : WriteFaults() {
    val writes = mutableListOf<String>()
    val deletes = mutableListOf<String>()

    override fun committed(target: File, deleted: Boolean) {
        val rel = target.relativeTo(root).invariantSeparatorsPath
        if (deleted) deletes += rel else writes += rel
    }

    fun clear() {
        writes.clear()
        deletes.clear()
    }
}

/** Fails at a chosen point, as a process kill would. */
class CrashAt(
    private val beforeJournal: Boolean = false,
    private val renameNumber: Int = -1,
    private val halfWriteOf: String? = null,
) : WriteFaults() {
    private var renames = 0

    override fun write(target: File, out: OutputStream, bytes: ByteArray) {
        if (halfWriteOf != null && target.name == halfWriteOf) {
            out.write(bytes, 0, bytes.size / 2)
            throw io.github.lenticualrstone.civilight.store.io.SimulatedCrash("half write of ${target.name}")
        }
        out.write(bytes)
    }

    override fun beforeJournal() {
        if (beforeJournal) throw io.github.lenticualrstone.civilight.store.io.SimulatedCrash("before journal")
    }

    override fun beforeRename(target: File) {
        if (target.name.startsWith(".commit-journal")) return
        renames++
        if (renames == renameNumber) throw io.github.lenticualrstone.civilight.store.io.SimulatedCrash("before rename $renames")
    }
}

/** Builds a JSON object quickly in tests. */
fun jsonOf(vararg pairs: Pair<String, Any?>) = buildJsonObject {
    for ((k, v) in pairs) put(k, when (v) {
        null -> kotlinx.serialization.json.JsonNull
        is Number -> JsonPrimitive(v)
        is Boolean -> JsonPrimitive(v)
        else -> JsonPrimitive(v.toString())
    })
}
