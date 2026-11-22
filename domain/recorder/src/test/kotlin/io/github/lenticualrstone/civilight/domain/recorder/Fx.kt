package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.TravelMode
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.time.Instant
import java.time.ZoneId

/**
 * LRC §4.1 fixtures over the canonical example (P1 = `ExampleProject.build()` with a plain 서재 row,
 * not the onboarding example). P2 「유리 정원」 and P3 「검은 서약 (가제)」 and records R5 … R12 are
 * built here (the core fixture leaves them out). F-open = R1 … R12.
 */
object Fx {
    val K = ExampleKeys
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    val NOW: Long = ExampleClock.NOW
    const val DEVICE = "test-device"

    val P1 = ExampleKeys.PROJECT
    const val P2 = "01JZ000000PRJT000000000002"
    const val P3 = "01JZ000000PRJT000000000003"
    const val P4 = "01JZ000000PRJT000000000004"
    val S = ExampleKeys.SCENE_1_7_3
    val C2 = ExampleKeys.CHAPTER_2

    const val P2_SERA = "01JZ000000CHAR000000000102"
    const val P3_SERA = "01JZ000000CHAR000000000302"
    const val P3_SCENE = "01JZ000000SCNE000000990101"
    const val P3_CHAPTER = "01JZ000000CHAP000000000099"
    const val P3_EPISODE = "01JZ000000EPSD000000009901"

    val R1 = ExampleKeys.RECORDER_R1
    val R2 = ExampleKeys.RECORDER_R2
    val R3 = ExampleKeys.RECORDER_R3
    val R4 = ExampleKeys.RECORDER_R4
    fun r(n: Int): Key = if (n <= 4) listOf(R1, R2, R3, R4)[n - 1] else ExampleKeys.recorderRecord(100 + n)
    fun p2Page(n: Int): Key = ExampleKeys.page(200 + n)

    fun at(iso: String): Long = Instant.parse(iso).toEpochMilli()

    /** Deterministic ULID-shaped keys (`01JZ000000TSTK` + 12 digits). */
    fun keys(start: Int = 1): () -> Key {
        var n = start
        return { "01JZ000000TSTK" + (n++).toString().padStart(12, '0') }
    }

    fun ctx(now: Long = NOW, start: Int = 1) = RecorderEditContext(newKey = keys(start), now = now, device = DEVICE, zone = ZONE)

    // --- Projects ---

    fun p1Doc(): ProjectDoc = ExampleProject.build()

    fun entry(doc: ProjectDoc, hidden: Boolean = false, isExample: Boolean = false, trashedAt: Long? = null) = LibraryEntry(
        key = doc.key, name = doc.project.name, rating = doc.project.rating, hidden = hidden, isExample = isExample,
        readOnly = isExample, deletedAt = trashedAt,
    )

    private fun project(key: Key, name: String, rating: Rating, format: ProjectFormat, protections: Protections) = Project(
        key = key, name = name, format = format.open(), rating = rating.open(), protections = protections,
        travelModes = TravelMode.defaults(keys(900 + key.takeLast(1).toInt() * 10)),
    )

    private fun units(chapter: Key, episode: Key, scene: Key) = listOf(
        StoryUnit(chapter, null, 0, OrderKeys.FIRST, title = "1장"),
        StoryUnit(episode, chapter, 1, OrderKeys.FIRST, nextBlockNumber = 1),
        StoryUnit(scene, episode, 2, OrderKeys.FIRST, title = "첫 장면"),
    )

    /** 「유리 정원」: ALL, 단편, not hidden; the pages of R5 … R12; a character 세라 only for LRC-T05. */
    fun p2Doc(withSera: Boolean = false): ProjectDoc = ProjectDoc(
        project = project(P2, "유리 정원", Rating.ALL, ProjectFormat.SHORT, Protections()),
        story = StoryIndexFile(units("01JZ000000CHAP000000000021", "01JZ000000EPSD000000002101", "01JZ000000SCNE000000210101")),
        cast = CastFile(characters = if (withSera) listOf(Character(P2_SERA, 1, "세라", order = OrderKeys.FIRST)) else emptyList()),
        pages = (1..8).associate { n ->
            p2Page(n) to Page(key = p2Page(n), order = OrderKeys.evenlySpaced(8)[n - 1], name = "정원 메모 $n")
        },
    )

    /** 「검은 서약 (가제)」: R-18, hidden, `recorderLinkMask`; a scene and 세라. */
    fun p3Doc(): ProjectDoc = ProjectDoc(
        project = project(P3, "검은 서약 (가제)", Rating.R18, ProjectFormat.SERIAL, Protections(hidden = true, recorderLinkMask = true)),
        story = StoryIndexFile(units(P3_CHAPTER, P3_EPISODE, P3_SCENE)),
        cast = CastFile(characters = listOf(Character(P3_SERA, 1, "세라", order = OrderKeys.FIRST))),
    )

    fun env(
        p1: ProjectDoc = p1Doc(),
        unlock: UnlockSession = UnlockSession(),
        p1Entry: LibraryEntry = entry(p1),
        p2: ProjectDoc = p2Doc(),
        p3: ProjectDoc = p3Doc(),
        recent: Key? = null,
    ) = RecorderEnv(
        projects = listOf(ProjectSnapshot(p1Entry, p1), ProjectSnapshot(entry(p2), p2), ProjectSnapshot(entry(p3, hidden = true), p3)),
        unlock = unlock,
        zone = ZONE,
        recentProjectKey = recent,
    )

    // --- Libraries ---

    private val exampleRecords = ExampleRecorder.library()

    fun record(n: Int): RecorderRecord = exampleRecords.record(r(n))!!

    /** F-open: R1 … R12, none hidden. */
    fun openLibrary(): RecorderLibrary {
        val base = listOf(R1, R2, R3, R4)
        val records = base.map { exampleRecords.record(it)!! } + (5..12).map { n ->
            val created = at("2026-%02d-%02dT01:00:00Z".format(11, n - 4))
            RecorderRecord(
                key = r(n), kind = RecordKind.TEXT.open(), createdAt = created,
                runs = listOf(TextRun.Plain("유리 정원 메모 ${n - 4}")), updatedAt = created, updatedBy = DEVICE,
            )
        }
        val links = exampleRecords.links.filter { it.recordKey in base } + (5..12).map { n ->
            RecorderLink(
                key = ExampleKeys.recorderLink(100 + n), recordKey = r(n), projectKey = P2, targetKind = RecorderTargetKind.PAGE.open(),
                targetKey = p2Page(n - 4), origin = LinkOrigin.INSERT.open(), createdAt = records.first { it.key == r(n) }.createdAt,
            )
        }
        val placements = (5..12).map { n ->
            Placement(
                key = ExampleKeys.placement(100 + n), recordKey = r(n), projectKey = P2, kind = PlacementKind.PAGE.open(),
                targetKey = p2Page(n - 4), copiedRevision = 1, createdAt = records.first { it.key == r(n) }.createdAt,
                lastLabel = "정원 메모 ${n - 4}",
            )
        }
        return RecorderLibrary(records, links, placements)
    }

    /** F-hidden: F-open with R1, R4, R5 hidden. */
    fun hiddenLibrary(): RecorderLibrary = openLibrary().let { lib ->
        lib.copy(records = lib.records.map { if (it.key in setOf(R1, R4, r(5))) it.copy(hidden = true) else it })
    }

    fun RecorderLibrary.withRecord(key: Key, change: (RecorderRecord) -> RecorderRecord) =
        copy(records = records.map { if (it.key == key) change(it) else it })

    // --- Doc edits used by the cases ---

    /** fixture.md: to start 노아 live, clear his trash mark and drop `TRASH_NOAH`. */
    fun withNoahLive(doc: ProjectDoc, hidden: Boolean = false): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map {
            if (it.key == K.NOAH) it.copy(deletedAt = null, trashEntryKey = null, hidden = hidden) else it
        }),
        trash = doc.trash.copy(entries = doc.trash.entries.filter { it.key != K.TRASH_NOAH }),
    )

    fun trashCharacter(doc: ProjectDoc, key: Key, at: Long = NOW): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == key) it.copy(deletedAt = at) else it }),
    )

    fun purgeCharacter(doc: ProjectDoc, key: Key): ProjectDoc =
        doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.filter { it.key != key }))

    fun trashUnit(doc: ProjectDoc, key: Key, at: Long = NOW): ProjectDoc =
        doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == key) it.copy(deletedAt = at) else it }))

    fun restoreUnit(doc: ProjectDoc, key: Key): ProjectDoc =
        doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == key) it.copy(deletedAt = null) else it }))

    fun purgeUnit(doc: ProjectDoc, key: Key): ProjectDoc = doc.copy(
        story = doc.story.copy(units = doc.story.units.filter { it.key != key }),
        episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.filter { it.sceneKey != key }) },
    )

    fun trashBlock(doc: ProjectDoc, key: Key, at: Long = NOW): ProjectDoc = doc.copy(
        episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == key) it.copy(deletedAt = at) else it }) },
    )

    fun purgeBlock(doc: ProjectDoc, key: Key): ProjectDoc = doc.copy(
        episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.filter { it.key != key }) },
    )

    fun target(kind: RecorderTargetKind, key: Key, project: Key = P1) = TargetRef(project, kind, key)
    val sceneS get() = target(RecorderTargetKind.UNIT, S)
    val hart get() = target(RecorderTargetKind.CHARACTER, K.HART)
    val sera get() = target(RecorderTargetKind.CHARACTER, K.SERA)
    val noah get() = target(RecorderTargetKind.CHARACTER, K.NOAH)
}
