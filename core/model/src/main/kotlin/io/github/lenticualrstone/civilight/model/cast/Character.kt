@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.cast

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.text.RichDoc
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A character (§2.5, `cast.json › characters`). Portrait and background are `ImageUse` rows; age,
 * `첫 등장`, `보이스 진행` and display-name ranges are derived.
 */
@Serializable
data class Character(
    override val key: Key,
    /** `인물 기록 007`; from `Project.counters.nextCharacterRecordNo`, never reused. */
    val recordNo: Int,
    /** `이름`, non-empty. */
    val name: String,
    /** `표시 이름` before any record; null = show [name]. */
    val displayNameBase: String? = null,
    val oneLiner: String? = null,
    @EncodeDefault val tagKeys: List<Key> = emptyList(),
    @EncodeDefault val speechBody: RichDoc = emptyList(),
    @EncodeDefault val secretBody: RichDoc = emptyList(),
    /** Speaker part of voice IDs (`HRT`). */
    val voiceCode: String? = null,
    @EncodeDefault val hidden: Boolean = false,
    override val order: OrderKey,
    /** Base values (before any record), by field definition key. */
    @EncodeDefault val fields: Map<Key, FieldValue> = emptyMap(),
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable

/** Kinds of items that carry custom fields (and change-record `field` targets). */
enum class EntityKind { CHARACTER, PLACE, TERM, FACTION }

/** A tag (`project.json › tags`); names are unique per project. */
@Serializable
data class Tag(
    override val key: Key,
    val name: String,
    override val order: OrderKey,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered

/** An in-game voice line of a character (`cast.json › voiceLines`); `비어 있음` is derived. */
@Serializable
data class VoiceLine(
    override val key: Key,
    val characterKey: Key,
    override val order: OrderKey,
    val title: String,
    @EncodeDefault val text: RichDoc = emptyList(),
    @EncodeDefault val status: OpenEnum<VoiceLineStatus> = OpenEnum.of(VoiceLineStatus.WRITING),
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered

enum class VoiceLineStatus { WRITING, DONE }

/** An archive document of a character (`cast.json › archiveDocs`); `1-7 이후 열림` = `unitEnd(1-7)`. */
@Serializable
data class ArchiveDoc(
    override val key: Key,
    val characterKey: Key,
    override val order: OrderKey,
    val title: String,
    @EncodeDefault val body: RichDoc = emptyList(),
    val unlockPoint: PointRef? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered
