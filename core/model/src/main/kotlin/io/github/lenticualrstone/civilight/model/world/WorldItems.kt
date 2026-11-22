@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.world

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.text.RichDoc
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A term (§2.6, `world.json › terms`). Its `◇ 첫 공개` is a `termReveal` change record. */
@Serializable
data class Term(
    override val key: Key,
    override val order: OrderKey,
    val name: String,
    @EncodeDefault val aliases: List<String> = emptyList(),
    @EncodeDefault val description: RichDoc = emptyList(),
    @EncodeDefault val related: List<ItemRef> = emptyList(),
    /** `글자 찾기`. */
    @EncodeDefault val textSearch: Boolean = true,
    @EncodeDefault val hidden: Boolean = false,
    @EncodeDefault val fields: Map<Key, FieldValue> = emptyMap(),
    /** `아님` decisions on candidates of this term. */
    @EncodeDefault val rejections: List<TermRejection> = emptyList(),
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable

/** `관련`: a term, place or faction. */
@Serializable
data class ItemRef(
    val kind: OpenEnum<ItemRefKind>,
    val key: Key,
    override val ext: JsonObject? = null,
) : HasExt {
    constructor(kind: ItemRefKind, key: Key) : this(kind.open(), key)
}

enum class ItemRefKind { TERM, PLACE, FACTION }

/** `아님` on one term candidate (WLD-22), embedded in its term. */
@Serializable
data class TermRejection(
    val key: Key,
    val containerKind: OpenEnum<RejectionContainerKind>,
    /** Block key, or scene key for transition text. */
    val containerKey: Key,
    val optionKey: Key? = null,
    /** NFC text of the candidate span. */
    val matchedText: String,
    /** 0-based among same-text candidates of this term in the container. */
    val occurrence: Int,
    @EncodeDefault val createdAt: Timestamp = 0L,
    override val ext: JsonObject? = null,
) : HasExt

enum class RejectionContainerKind { BLOCK, SCENE_TRANSITION }

/** A place (`world.json › places`). */
@Serializable
data class Place(
    override val key: Key,
    override val order: OrderKey,
    val name: String,
    /**
     * Other names of the place, matched like the name (STO-34 `제안 받기`, SUG-02: 송전탑 폐허 · 송전탑).
     * Written only when not empty (no `@EncodeDefault`), so files without it stay byte-identical.
     */
    val aliases: List<String> = emptyList(),
    @EncodeDefault val description: RichDoc = emptyList(),
    /** 지역 / 소속 (WLD Q-03). */
    val parentPlaceKey: Key? = null,
    @EncodeDefault val hidden: Boolean = false,
    @EncodeDefault val fields: Map<Key, FieldValue> = emptyMap(),
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable

/** A faction (`world.json › factions`). */
@Serializable
data class Faction(
    override val key: Key,
    override val order: OrderKey,
    val name: String,
    @EncodeDefault val description: RichDoc = emptyList(),
    @EncodeDefault val hidden: Boolean = false,
    @EncodeDefault val fields: Map<Key, FieldValue> = emptyMap(),
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable

/**
 * A timeline event (`world.json › events`). No stored `hidden`: masking derives from hidden linked
 * items. [order] breaks ties at equal time.
 */
@Serializable
data class TimelineEvent(
    override val key: Key,
    override val order: OrderKey,
    val kind: OpenEnum<EventKind>,
    val title: String,
    @EncodeDefault val description: RichDoc = emptyList(),
    /** World events only. */
    val worldDate: WorldDate? = null,
    /** Story events only; time = the scene's resolved time. */
    val sceneKey: Key? = null,
    val place: PlaceRef? = null,
    @EncodeDefault val relatedTermKeys: List<Key> = emptyList(),
    val revealPoint: PointRef? = null,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable

/** `세계 사건 · 이야기 사건`. */
enum class EventKind { WORLD, STORY }

/** A world date; month and day optional. */
@Serializable
data class WorldDate(
    val year: Int,
    val month: Int? = null,
    val day: Int? = null,
    override val ext: JsonObject? = null,
) : HasExt
