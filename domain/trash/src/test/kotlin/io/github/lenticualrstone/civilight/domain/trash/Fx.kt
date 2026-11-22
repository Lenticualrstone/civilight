package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.time.LocalDateTime
import java.util.Random

/** Test fixtures over the example project 「잿빛 새벽의 등불」 (docs/status/fixture.md). */
object Fx {
    val zone = ExampleClock.ZONE
    const val PROJECT_NAME = "잿빛 새벽의 등불"

    /** The fixtures' now: 2026-11-22 09:00 KST, deterministic keys. */
    fun ctx(now: Long = ExampleClock.NOW, seed: Long = 7): TrashContext {
        val generator = UlidGenerator(clock = { now }, random = Random(seed))
        return TrashContext(now, zone, "test-device", generator::next)
    }

    /** A context at a local date and time in Seoul. */
    fun ctxAt(iso: String, seed: Long = 11): TrashContext =
        ctx(LocalDateTime.parse(iso).atZone(zone).toInstant().toEpochMilli(), seed)

    fun doc(): ProjectDoc = ExampleProject.build()

    fun recorder() = ExampleRecorder.library()

    /** The fixture with 노아 live (fixture.md conflict #22 recipe: clear his mark, drop `TRASH_NOAH`). */
    fun noahLive(doc: ProjectDoc = doc()): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(deletedAt = null, trashEntryKey = null) else it }),
        trash = doc.trash.copy(entries = doc.trash.entries.filter { it.key != K.TRASH_NOAH }),
    )

    /** Adds a character at the end of the cast. */
    fun addCharacter(doc: ProjectDoc, key: Key, name: String, recordNo: Int, voiceCode: String? = null): ProjectDoc {
        val last = doc.cast.characters.maxOf { it.order }
        val c = Character(key = key, recordNo = recordNo, name = name, voiceCode = voiceCode, order = OrderKeys.after(last))
        return doc.copy(cast = doc.cast.copy(characters = doc.cast.characters + c))
    }

    /** Edits one block in place. */
    fun editBlock(doc: ProjectDoc, key: Key, f: (Block) -> Block): ProjectDoc = doc.copy(
        episodes = doc.episodes.mapValues { (_, file) -> file.copy(blocks = file.blocks.map { if (it.key == key) f(it) else it }) },
    )

    fun fixVoice(doc: ProjectDoc, key: Key, id: String): ProjectDoc =
        editBlock(doc, key) { it.copy(voice = VoiceState(mode = VoiceMode.FIXED.open(), fixedId = id, fixedAt = ExampleClock.VOICE_EXPORT)) }

    /** Live dialogue blocks spoken by [characterKey], in manuscript order. */
    fun linesOf(doc: ProjectDoc, characterKey: Key): List<Block> =
        doc.manuscript.liveBlocks.filter { it.speakerKey == characterKey }

    /** Keys of every block of [sceneKey] (live and trashed). */
    fun blocksOfScene(doc: ProjectDoc, sceneKey: Key): List<Block> = doc.index.blocksOfScene(sceneKey)

    fun text(runs: List<TextRun>): String = runs.joinToString("") { it.shownText }

    /** A fresh ULID-shaped test key: `01JZ0000TEST` + 14 digits. */
    fun key(family: String, n: Int): Key = "01JZ0000" + family + n.toString().padStart(14, '0')

    fun restored(outcome: RestoreOutcome): RestoreOutcome.Restored =
        outcome as? RestoreOutcome.Restored ?: error("expected Restored, got $outcome")
}
