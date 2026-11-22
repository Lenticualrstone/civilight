@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.file

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.TranscriptionMode
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * `recorder.json` of a project package (§7.10) and `records.json` of the recorder library file or
 * store (§7.11): records with their links and placements nested. In a project's `recorder.json`
 * link and placement `projectKey` is omitted (implied: that project).
 */
@Serializable
data class RecorderFile(
    @EncodeDefault val records: List<RecorderFileRecord> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt {
    /** The library rows; links and placements without a project key get [impliedProjectKey]. */
    fun toLibrary(impliedProjectKey: Key? = null): RecorderLibrary {
        fun projectOf(key: Key?): Key = requireNotNull(key ?: impliedProjectKey) { "recorder row without projectKey" }
        val records = records.map { it.toRecord() }
        val links = this.records.flatMap { r -> r.links.map { it.toLink(r.key, projectOf(it.projectKey)) } }
        val placements = this.records.flatMap { r -> r.placements.map { it.toPlacement(r.key, projectOf(it.projectKey)) } }
        return RecorderLibrary(records, links, placements)
    }

    companion object {
        /**
         * The whole library (`records.json`): every record (trashed ones only with
         * [includeTrashed], as the working store needs), all links and placements with their
         * project keys, audio envelopes kept.
         */
        fun library(library: RecorderLibrary, includeTrashed: Boolean = false): RecorderFile = RecorderFile(
            library.records.filter { includeTrashed || it.deletedAt == null }.map { record ->
                RecorderFileRecord.of(
                    record,
                    library.linksOf(record.key).map { RecorderFileLink.of(it, withProjectKey = true) },
                    library.placementsOf(record.key).map { RecorderFilePlacement.of(it, withProjectKey = true) },
                    keepEnvelope = true,
                )
            },
        )

        /**
         * A project's `recorder.json`: [records] with only their links and placements into
         * [projectKey] (no project key written), audio only when [includeAudio]. Choosing which
         * records belong (linked, not trashed, hidden only when allowed) is the exporter's job.
         */
        fun forProject(
            library: RecorderLibrary,
            projectKey: Key,
            records: List<RecorderRecord>,
            includeAudio: Boolean,
        ): RecorderFile = RecorderFile(
            records.map { record ->
                RecorderFileRecord.of(
                    if (includeAudio) record else record.copy(audio = null),
                    library.linksOf(record.key).filter { it.projectKey == projectKey }
                        .map { RecorderFileLink.of(it, withProjectKey = false) },
                    library.placementsOf(record.key).filter { it.projectKey == projectKey }
                        .map { RecorderFilePlacement.of(it, withProjectKey = false) },
                    keepEnvelope = false,
                )
            },
        )
    }
}

@Serializable
data class RecorderFileRecord(
    val key: Key,
    val kind: OpenEnum<RecordKind>,
    val createdAt: Timestamp,
    @EncodeDefault val updatedAt: Timestamp = 0L,
    @EncodeDefault val updatedBy: String = "",
    @EncodeDefault val runs: List<TextRun> = emptyList(),
    @EncodeDefault val textRevision: Int = 1,
    val durationMs: Long? = null,
    val transcribedBy: OpenEnum<TranscriptionMode>? = null,
    @EncodeDefault val doneMarked: Boolean = false,
    @EncodeDefault val hidden: Boolean = false,
    val audio: RecorderAudio? = null,
    @EncodeDefault val links: List<RecorderFileLink> = emptyList(),
    @EncodeDefault val placements: List<RecorderFilePlacement> = emptyList(),
    /** Working store only (the library trash); never in a package. */
    val deletedAt: Timestamp? = null,
    val trashEntryKey: Key? = null,
    override val ext: JsonObject? = null,
) : HasExt {
    fun toRecord(): RecorderRecord = RecorderRecord(
        key = key, kind = kind, createdAt = createdAt, runs = runs, textRevision = textRevision,
        durationMs = durationMs, audio = audio, transcribedBy = transcribedBy, doneMarked = doneMarked,
        hidden = hidden, deletedAt = deletedAt, trashEntryKey = trashEntryKey, updatedAt = updatedAt,
        updatedBy = updatedBy, ext = ext,
    )

    companion object {
        fun of(
            record: RecorderRecord,
            links: List<RecorderFileLink>,
            placements: List<RecorderFilePlacement>,
            keepEnvelope: Boolean,
        ) = RecorderFileRecord(
            key = record.key, kind = record.kind, createdAt = record.createdAt, updatedAt = record.updatedAt,
            updatedBy = record.updatedBy, runs = record.runs, textRevision = record.textRevision,
            durationMs = record.durationMs, transcribedBy = record.transcribedBy, doneMarked = record.doneMarked,
            hidden = record.hidden,
            audio = if (keepEnvelope) record.audio else record.audio?.copy(envelope = null),
            links = links, placements = placements, deletedAt = record.deletedAt,
            trashEntryKey = record.trashEntryKey, ext = record.ext,
        )
    }
}

@Serializable
data class RecorderFileLink(
    val key: Key,
    val projectKey: Key? = null,
    val targetKind: OpenEnum<RecorderTargetKind>,
    val targetKey: Key,
    val origin: OpenEnum<LinkOrigin>,
    val createdAt: Timestamp,
    val lastLabel: String? = null,
    val purgedAt: Timestamp? = null,
    @EncodeDefault val lastLabelHidden: Boolean = false,
    @EncodeDefault val updatedAt: Timestamp = 0L,
    @EncodeDefault val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : HasExt {
    fun toLink(recordKey: Key, projectKey: Key) = RecorderLink(
        key = key, recordKey = recordKey, projectKey = projectKey, targetKind = targetKind, targetKey = targetKey,
        origin = origin, createdAt = createdAt, lastLabel = lastLabel, purgedAt = purgedAt,
        lastLabelHidden = lastLabelHidden, updatedAt = updatedAt, updatedBy = updatedBy, ext = ext,
    )

    companion object {
        fun of(link: RecorderLink, withProjectKey: Boolean) = RecorderFileLink(
            key = link.key, projectKey = if (withProjectKey) link.projectKey else null, targetKind = link.targetKind,
            targetKey = link.targetKey, origin = link.origin, createdAt = link.createdAt, lastLabel = link.lastLabel,
            purgedAt = link.purgedAt, lastLabelHidden = link.lastLabelHidden, updatedAt = link.updatedAt,
            updatedBy = link.updatedBy, ext = link.ext,
        )
    }
}

@Serializable
data class RecorderFilePlacement(
    val key: Key,
    val projectKey: Key? = null,
    val kind: OpenEnum<PlacementKind>,
    val targetKey: Key,
    val containerKey: Key? = null,
    val tabId: String? = null,
    val copiedRevision: Int,
    val createdAt: Timestamp,
    val lastLabel: String,
    val purgedAt: Timestamp? = null,
    @EncodeDefault val updatedAt: Timestamp = 0L,
    @EncodeDefault val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : HasExt {
    fun toPlacement(recordKey: Key, projectKey: Key) = Placement(
        key = key, recordKey = recordKey, projectKey = projectKey, kind = kind, targetKey = targetKey,
        containerKey = containerKey, tabId = tabId, copiedRevision = copiedRevision, createdAt = createdAt,
        lastLabel = lastLabel, purgedAt = purgedAt, updatedAt = updatedAt, updatedBy = updatedBy, ext = ext,
    )

    companion object {
        fun of(placement: Placement, withProjectKey: Boolean) = RecorderFilePlacement(
            key = placement.key, projectKey = if (withProjectKey) placement.projectKey else null,
            kind = placement.kind, targetKey = placement.targetKey, containerKey = placement.containerKey,
            tabId = placement.tabId, copiedRevision = placement.copiedRevision, createdAt = placement.createdAt,
            lastLabel = placement.lastLabel, purgedAt = placement.purgedAt, updatedAt = placement.updatedAt,
            updatedBy = placement.updatedBy, ext = placement.ext,
        )
    }
}
