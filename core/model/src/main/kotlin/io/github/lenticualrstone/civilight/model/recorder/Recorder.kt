@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.recorder

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.Base64

/**
 * A Life Recorder record (§2.12), in the global recorder library outside projects. Its state
 * (`정리 안 됨 · 넣음 · 끝냄`) is derived from placements and [doneMarked].
 */
@Serializable
data class RecorderRecord(
    override val key: Key,
    val kind: OpenEnum<RecordKind>,
    /** Recording start; copied into source marks. */
    val createdAt: Timestamp,
    /** Text; link targets carry `projectKey`. */
    @EncodeDefault val runs: List<TextRun> = emptyList(),
    /** +1 on every committed edit. */
    @EncodeDefault val textRevision: Int = 1,
    /** Voice only, pauses excluded. */
    val durationMs: Long? = null,
    /** Null when the audio is not kept. */
    val audio: RecorderAudio? = null,
    val transcribedBy: OpenEnum<TranscriptionMode>? = null,
    /** `끝냄`. */
    @EncodeDefault val doneMarked: Boolean = false,
    /** Hidden record (own gate, LRC-41). */
    @EncodeDefault val hidden: Boolean = false,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Trashable

enum class RecordKind { VOICE, TEXT }

enum class TranscriptionMode { ON_DEVICE, CLOUD }

/**
 * Kept audio of a voice record. [envelope] (base64 of the downsampled loudness bytes) is stored in
 * the library only; package files leave it out (§7.10).
 */
@Serializable
data class RecorderAudio(
    /** → Blob. */
    val hash: String,
    val mime: String,
    val bytes: Long,
    val durationMs: Long,
    val envelope: String? = null,
    override val ext: JsonObject? = null,
) : HasExt {
    fun envelopeBytes(): ByteArray? = envelope?.let { Base64.getDecoder().decode(it) }

    companion object {
        fun encodeEnvelope(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    }
}

/** `연결` of a record to a project item (§2.12); unique (record, project, target kind, target). */
@Serializable
data class RecorderLink(
    override val key: Key,
    val recordKey: Key,
    val projectKey: Key,
    val targetKind: OpenEnum<RecorderTargetKind>,
    val targetKey: Key,
    val origin: OpenEnum<LinkOrigin>,
    val createdAt: Timestamp,
    /** Last known label, written when the target is purged. */
    val lastLabel: String? = null,
    val purgedAt: Timestamp? = null,
    /** The target was hidden when purged: never print its name. */
    @EncodeDefault val lastLabelHidden: Boolean = false,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity

/** Link targets; `unit` covers every structure level. */
enum class RecorderTargetKind { PROJECT, UNIT, CHARACTER, PLACE, TERM, FACTION, PAGE }

/** `보던 곳 · + 연결 · @ · 넣기 · 시작 재료`. */
enum class LinkOrigin { ENTRY, MANUAL, AT, INSERT, START_MATERIAL }

/** `넣은 곳`: a copy of a record placed into a project (§2.12). */
@Serializable
data class Placement(
    override val key: Key,
    val recordKey: Key,
    val projectKey: Key,
    val kind: OpenEnum<PlacementKind>,
    /** Block key · DocBlock key · page key · unit key · character key. */
    val targetKey: Key,
    /** Scene key for a block; entity key for a paragraph. */
    val containerKey: Key? = null,
    /** Field def key or `speech` · `secret` · `overview` · `description`. */
    val tabId: String? = null,
    /** Record text revision at copy time (`◇ 넣은 뒤 고침` when lower). */
    val copiedRevision: Int,
    val createdAt: Timestamp,
    val lastLabel: String,
    val purgedAt: Timestamp? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity

enum class PlacementKind { SCRIPT_BLOCK, CARD_PARAGRAPH, PAGE, MEMO_FIELD, CHARACTER }

/**
 * The global recorder library (class R, `files/recorder/records.json`): records plus all links and
 * placements of every project.
 */
data class RecorderLibrary(
    val records: List<RecorderRecord> = emptyList(),
    val links: List<RecorderLink> = emptyList(),
    val placements: List<Placement> = emptyList(),
) {
    private val linksByRecord by lazy { links.groupBy { it.recordKey } }
    private val placementsByRecord by lazy { placements.groupBy { it.recordKey } }
    private val recordsByKey by lazy { records.associateBy { it.key } }

    fun record(key: Key): RecorderRecord? = recordsByKey[key]
    fun linksOf(recordKey: Key): List<RecorderLink> = linksByRecord[recordKey].orEmpty()
    fun placementsOf(recordKey: Key): List<Placement> = placementsByRecord[recordKey].orEmpty()

    /** Records linked to [projectKey] explicitly (implied links derive from these). */
    fun recordsLinkedTo(projectKey: Key): List<RecorderRecord> {
        val keys = (links.filter { it.projectKey == projectKey }.map { it.recordKey } +
            placements.filter { it.projectKey == projectKey }.map { it.recordKey }).toSet()
        return records.filter { it.key in keys }
    }
}
