package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.ImportedPackage
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.time.ZoneId
import kotlin.random.Random

/** Shared Givens of the §4 cases, built from the canonical example project. */
object Fixtures {
    val NOW: Long = ExampleClock.NOW
    val ZONE: ZoneId = ExampleClock.ZONE
    const val DEVICE = "test-device"

    fun at(iso: String): Long = ExampleClock.at(iso)

    /** Deterministic key source for tests. */
    fun keys(seed: Long = 7): () -> Key {
        val gen = UlidGenerator(clock = { 1_800_000_000_000L }, random = java.util.Random(seed))
        return gen::next
    }

    fun example(): ProjectDoc = ExampleProject.build()

    fun recorder(): RecorderLibrary = ExampleRecorder.library()

    /** 잿빛 with 노아 back from the trash (fixture recipe #22). */
    fun withNoahLive(doc: ProjectDoc = example()): ProjectDoc = doc.copy(
        cast = doc.cast.copy(
            characters = doc.cast.characters.map {
                if (it.key == ExampleKeys.NOAH) it.copy(deletedAt = null, trashEntryKey = null) else it
            },
        ),
        trash = doc.trash.copy(entries = doc.trash.entries.filterNot { it.key == ExampleKeys.TRASH_NOAH }),
    )

    /** 잿빛 with 노아 live and hidden. */
    fun withNoahHidden(doc: ProjectDoc = example()): ProjectDoc {
        val live = withNoahLive(doc)
        return live.copy(cast = live.cast.copy(characters = live.cast.characters.map { if (it.key == ExampleKeys.NOAH) it.copy(hidden = true) else it }))
    }

    /** [doc] with scene [sceneKey] hidden. */
    fun withSceneHidden(doc: ProjectDoc, sceneKey: Key): ProjectDoc =
        doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == sceneKey) it.copy(hidden = true) else it }))

    /** [doc] with 1-6 씬 5 back from the trash. */
    fun withScene165Live(doc: ProjectDoc): ProjectDoc = doc.copy(
        story = doc.story.copy(
            units = doc.story.units.map {
                if (it.key == ExampleKeys.SCENE_1_6_5) it.copy(deletedAt = null, trashEntryKey = null) else it
            },
        ),
        trash = doc.trash.copy(entries = doc.trash.entries.filterNot { it.key == ExampleKeys.TRASH_SCENE_1_6_5 }),
    )

    fun withProtections(doc: ProjectDoc, rating: Rating, protections: Protections): ProjectDoc =
        doc.copy(project = doc.project.copy(rating = rating.open(), protections = protections))

    /** Appends [blocks] (built by the caller) to their scenes and refiles them by numbering unit. */
    fun withBlocks(doc: ProjectDoc, blocks: List<Block>): ProjectDoc {
        val unitKey = doc.index.numberingUnitKeyOfScene(blocks.first().sceneKey)!!
        val file = doc.episodes.getValue(unitKey)
        return doc.copy(episodes = doc.episodes + (unitKey to file.copy(blocks = file.blocks + blocks)))
    }

    /** A narration block after the last block of [sceneKey]. */
    fun narration(doc: ProjectDoc, sceneKey: Key, number: Int, text: String, key: Key, after: String? = null): Block {
        val last = after ?: doc.index.blocksOfScene(sceneKey).maxOfOrNull { it.order }
        return Block(
            key = key, sceneKey = sceneKey, order = OrderKeys.between(last, null), number = number,
            kind = BlockKind.NARRATION.open(), runs = listOf(TextRun.Plain(text)),
        )
    }

    /** A minimal other project of the §4 sample data. */
    fun otherProject(
        name: String,
        rating: Rating,
        protections: Protections = Protections(),
        format: ProjectFormat = ProjectFormat.SERIAL,
        key: Key = keys(name.hashCode().toLong())(),
    ): ProjectDoc = ProjectDoc(
        project = Project(key = key, name = name, format = format.open(), rating = rating.open(), protections = protections),
    )

    /**
     * A project of the §4 sample data with [scenes] scenes (one chapter, one episode) and
     * [characters] characters, all live.
     */
    fun projectWith(
        name: String,
        rating: Rating,
        protections: Protections = Protections(),
        format: ProjectFormat = ProjectFormat.SERIAL,
        scenes: Int = 0,
        characters: Int = 0,
        seed: Long = name.hashCode().toLong(),
    ): ProjectDoc {
        val next = keys(seed)
        val base = otherProject(name, rating, protections, format, next())
        if (scenes == 0 && characters == 0) return base
        val chapter = next()
        val episode = next()
        val sceneOrders = OrderKeys.evenlySpaced(maxOf(scenes, 1))
        val units = buildList {
            if (scenes > 0) {
                add(StoryUnit(key = chapter, level = 0, order = OrderKeys.between(null, null)))
                add(StoryUnit(key = episode, parentKey = chapter, level = 1, order = OrderKeys.between(null, null), nextBlockNumber = 1))
                repeat(scenes) { add(StoryUnit(key = next(), parentKey = episode, level = 2, order = sceneOrders[it])) }
            }
        }
        val charOrders = OrderKeys.evenlySpaced(maxOf(characters, 1))
        val cast = List(characters) {
            io.github.lenticualrstone.civilight.model.cast.Character(key = next(), recordNo = it + 1, name = "인물${it + 1}", order = charOrders[it])
        }
        return base.copy(
            story = base.story.copy(units = units),
            episodes = if (scenes > 0) mapOf(episode to io.github.lenticualrstone.civilight.model.file.EpisodeFile(unitKey = episode)) else emptyMap(),
            cast = base.cast.copy(characters = cast),
        )
    }

    fun entry(doc: ProjectDoc, deletedAt: Long? = null): LibraryEntry = LibraryEntry(
        key = doc.key, name = doc.project.name, rating = doc.project.rating, hidden = doc.project.protections.hidden,
        deletedAt = deletedAt, createdAt = doc.project.createdAt, updatedAt = doc.project.updatedAt,
    )

    fun export(
        doc: ProjectDoc,
        recorder: RecorderLibrary = RecorderLibrary(),
        options: ProjectFileOptions = ProjectFileOptions(),
    ): ByteArray = ProjectPackageWriter.write(doc, recorder, options = options, writtenBy = "Civilight test", writtenAt = NOW)

    fun open(bytes: ByteArray): ImportedPackage {
        val result = ProjectPackageReader.open(bytes)
        check(result is PackageOpen.Ready) { "expected Ready, got $result" }
        return result.pkg
    }

    fun randomBytes(n: Int, seed: Int = 1): ByteArray = Random(seed).nextBytes(n)

    @Suppress("unused")
    fun scene(doc: ProjectDoc, key: Key): StoryUnit = doc.index.unit(key)!!
}
