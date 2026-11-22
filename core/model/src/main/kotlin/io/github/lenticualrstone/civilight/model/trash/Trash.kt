@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.trash

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.IsoDate
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate

/**
 * One trash row (§2.11): an index over directly trashed rows, whose content stays with its owner
 * carrying `deletedAt` + `trashEntryKey`. Written to `trash/index.json` only with `휴지통도 포함`.
 */
@Serializable
data class TrashEntry(
    val key: Key,
    val kind: OpenEnum<TrashKind>,
    /** Null for recorder records (outside projects). */
    val projectKey: Key? = null,
    val targetKeys: List<Key>,
    /** Block groups: one per scene per local day. */
    val groupSceneKey: Key? = null,
    val groupDate: IsoDate? = null,
    @EncodeDefault val reason: OpenEnum<TrashReason> = TrashReason.DELETED.open(),
    /** `합침 → <target>`. */
    val mergedIntoKey: Key? = null,
    /** Render masked and authenticate to restore. */
    @EncodeDefault val hiddenAtDelete: Boolean = false,
    val deletedAt: Timestamp,
    /** Deleted local date + 30 days. */
    val purgeOn: IsoDate,
    override val ext: JsonObject? = null,
) : HasExt {
    companion object {
        const val RETENTION_DAYS = 30L

        fun purgeDateFor(deletedOn: LocalDate): LocalDate = deletedOn.plusDays(RETENTION_DAYS)
    }
}

enum class TrashKind {
    PROJECT, SCENE, BLOCK_GROUP, CHARACTER, PLACE, TERM, FACTION, CHANGE, RECORDER, FIELD_DEF,
    PAGE, MAP, CANVAS, EVENT,

    /** One gallery image (55 GAL-20, serial `galleryItem`). */
    GALLERY_ITEM,
}

enum class TrashReason { DELETED, MERGED, REPLACED_BY_OVERWRITE }

/**
 * `이름표` (§2.11, `tombstones.json › nameTags`): what a reference to a gone item prints. Its [key] is
 * the deleted item's key. `notExported` (`ED`) marks items in the trash on the exporting device that
 * live exported data still references (§5.6).
 */
@Serializable
data class NameTag(
    val key: Key,
    val kind: OpenEnum<NameTagKind>,
    val name: String,
    val voiceCode: String? = null,
    @EncodeDefault val hidden: Boolean = false,
    val state: OpenEnum<NameTagState>,
    val purgedAt: Timestamp? = null,
    override val ext: JsonObject? = null,
) : HasExt

enum class NameTagKind { CHARACTER, PLACE, TERM, FACTION, UNIT, PAGE }

enum class NameTagState { PURGED, NOT_EXPORTED }

/** A merge (CST-45), used for undo and for restoring the merged item. */
@Serializable
data class MergeRecord(
    val key: Key,
    /** Kind of the merged item (character, place, term, faction). */
    val kind: OpenEnum<NameTagKind>,
    val mergedKey: Key,
    val targetKey: Key,
    val at: Timestamp,
    @EncodeDefault val movedRefs: List<MovedRef> = emptyList(),
    @EncodeDefault val filledFieldDefKeys: List<Key> = emptyList(),
    @EncodeDefault val chosenFromMerged: List<MergeChoice> = emptyList(),
    val deletedMutualRelationKey: Key? = null,
    /** Snapshot of the merged item as it was (owner-defined shape). */
    val mergedOriginal: JsonElement,
    override val ext: JsonObject? = null,
) : HasExt

/** A reference rewritten to the target key: owner kind and key plus the field that held it. */
@Serializable
data class MovedRef(
    val ownerKind: String,
    val ownerKey: Key,
    val field: String,
    override val ext: JsonObject? = null,
) : HasExt

/** A value taken from the merged item: `{fieldDefKey}` or `{relationKey, slot}`. */
@Serializable
data class MergeChoice(
    val fieldDefKey: Key? = null,
    val relationKey: Key? = null,
    val slot: OpenEnum<RelationSlotName>? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** Deletion log row (`deletions.json`), kept 30 days for future sync. */
@Serializable
data class DeletionLogEntry(
    val key: Key,
    val kind: String,
    val deletedAt: Timestamp,
    override val ext: JsonObject? = null,
) : HasExt
