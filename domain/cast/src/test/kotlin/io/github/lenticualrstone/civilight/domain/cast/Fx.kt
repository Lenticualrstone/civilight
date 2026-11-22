package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.RelationKind
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.util.Random
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Test fixtures over the example project 「잿빛 새벽의 등불」 (docs/status/fixture.md). */
object Fx {
    const val PROJECT_NAME = "잿빛 새벽의 등불"

    /** 2026-11-22 09:00 KST, deterministic keys. */
    fun ctx(now: Long = ExampleClock.NOW, seed: Long = 7): EditContext {
        val generator = UlidGenerator(clock = { now }, random = Random(seed))
        return EditContext(newKey = generator::next, now = now, device = "test-device", zone = ExampleClock.ZONE)
    }

    fun doc(): ProjectDoc = ExampleProject.build()

    /** Recorder links of the example project (노아's `레코더 칩 1` among them). */
    fun recorderLinks() = ExampleRecorder.library().links

    /** The fixture with 노아 live (fixture.md conflict #22: clear his mark, drop `TRASH_NOAH`). */
    fun noahLive(doc: ProjectDoc = doc()): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(deletedAt = null, trashEntryKey = null) else it }),
        trash = doc.trash.copy(entries = doc.trash.entries.filter { it.key != K.TRASH_NOAH }),
    )

    /** Sets a trash mark on a character (a stand-in for the trash domain's delete). */
    fun trash(doc: ProjectDoc, key: Key, at: Long = ExampleClock.NOW): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == key) it.copy(deletedAt = at) else it }),
    )

    fun restore(doc: ProjectDoc, key: Key): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == key) it.copy(deletedAt = null, trashEntryKey = null) else it }),
    )

    fun hide(doc: ProjectDoc, key: Key): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == key) it.copy(hidden = true) else it }),
    )

    fun lens(unitKey: Key): LensPoint = LensPoint.UnitEnd(unitKey)

    val L1_4 = lens(K.EPISODE_1_4)
    val L1_5 = lens(K.EPISODE_1_5)
    val L1_6 = lens(K.EPISODE_1_6)
    val L1_7 = lens(K.EPISODE_1_7)
    val L1_8 = lens(K.EPISODE_1_8)
    val L1_9 = lens(K.EPISODE_1_9)
    val LATEST: LensPoint = LensPoint.Latest

    fun kind(doc: ProjectDoc, name: String): RelationKind = doc.project.relationKinds.single { it.name == name }

    fun def(doc: ProjectDoc, name: String, kind: EntityKind = EntityKind.CHARACTER): FieldDef =
        doc.project.fieldDefs.first { it.name == name && it.entityKind.known == kind && it.deletedAt == null }

    fun character(doc: ProjectDoc, key: Key): Character = requireNotNull(doc.index.character(key))

    /** Removes every record of a relation (a "fresh" relation). */
    fun withoutRelationRecords(doc: ProjectDoc, relationKey: Key): ProjectDoc = doc.copy(
        changes = doc.changes.copy(records = doc.changes.records.filterNot { (it.target as? ChangeTarget.RelationSlot)?.relationKey == relationKey }),
    )

    /** Removes a relation and its records. */
    fun withoutRelation(doc: ProjectDoc, relationKey: Key): ProjectDoc = withoutRelationRecords(doc, relationKey).let {
        it.copy(cast = it.cast.copy(relations = it.cast.relations.filterNot { r -> r.key == relationKey }))
    }

    /** Adds a character at the end of the cast. */
    fun addCharacter(
        doc: ProjectDoc,
        key: Key,
        name: String,
        recordNo: Int,
        fields: Map<Key, FieldValue> = emptyMap(),
        oneLiner: String? = null,
        voiceCode: String? = null,
        tags: List<Key> = emptyList(),
    ): ProjectDoc {
        val last = doc.cast.characters.maxOfOrNull { it.order }
        val c = Character(
            key = key, recordNo = recordNo, name = name, oneLiner = oneLiner, voiceCode = voiceCode, tagKeys = tags,
            order = OrderKeys.after(last), fields = fields,
        )
        return doc.copy(cast = doc.cast.copy(characters = doc.cast.characters + c))
    }

    /** Sets one base field value of a character directly (test setup). */
    fun setField(doc: ProjectDoc, characterKey: Key, defKey: Key, value: FieldValue?): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { c ->
            if (c.key != characterKey) c else c.copy(fields = if (value == null) c.fields - defKey else c.fields + (defKey to value))
        }),
    )

    fun text(value: String) = FieldValue.of(FieldPayload.Texts(listOf(value)))
    fun number(value: String) = FieldValue.of(FieldPayload.Number(value))
    fun year(value: Int) = FieldValue.of(FieldPayload.Date(value))

    /** Edits one block in place. */
    fun editBlock(doc: ProjectDoc, key: Key, f: (Block) -> Block): ProjectDoc = doc.copy(
        episodes = doc.episodes.mapValues { (_, file) -> file.copy(blocks = file.blocks.map { if (it.key == key) f(it) else it }) },
    )

    /** Appends a block with text to a scene and files it under the scene's numbering unit. */
    fun addBlock(
        doc: ProjectDoc,
        key: Key,
        sceneKey: Key,
        number: Int,
        text: String,
        kind: BlockKind = BlockKind.NARRATION,
        speaker: Key? = null,
    ): ProjectDoc {
        val unitKey = requireNotNull(doc.index.numberingUnitKeyOfScene(sceneKey))
        val last = doc.index.blocksOfScene(sceneKey).maxOfOrNull { it.order }
        val block = Block(
            key = key, sceneKey = sceneKey, order = OrderKeys.after(last), number = number, kind = kind.open(),
            speakerKey = speaker, runs = listOf(TextRun.Plain(text)),
        )
        val file = doc.episodes[unitKey] ?: io.github.lenticualrstone.civilight.model.file.EpisodeFile(unitKey = unitKey)
        return doc.copy(episodes = doc.episodes + (unitKey to file.copy(blocks = file.blocks + block)))
    }

    /** Live dialogue blocks spoken by [characterKey], in manuscript order. */
    fun linesOf(doc: ProjectDoc, characterKey: Key): List<Block> = doc.manuscript.liveBlocks.filter { it.speakerKey == characterKey }

    /** A fresh ULID-shaped test key: `01JZ0000` + family + 14 digits. */
    fun key(family: String, n: Int): Key = "01JZ0000" + family + n.toString().padStart(14, '0')
}
