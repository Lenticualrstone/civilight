@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.change

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * A `◆` change record or a `◇ 첫 공개` (target `termReveal`), in `changes.json` (§2.4). [value] is
 * the value from the [anchor] on; its schema depends on the target kind. An absent value and a JSON
 * `null` mean the same (`displayName`: show `이름`; `termReveal`: no value).
 */
@Serializable
data class ChangeRecord(
    override val key: Key,
    val target: ChangeTarget,
    val value: JsonElement? = null,
    /** History note (`처음 만남`). */
    val note: String? = null,
    val anchor: Anchor,
    @EncodeDefault val anchorState: OpenEnum<AnchorState> = AnchorState.NORMAL.open(),
    /** Non-null iff [anchorState] is orphaned: the anchor before relocation. */
    val originalAnchor: Anchor? = null,
    /** Tie-break at equal positions. */
    @EncodeDefault val createdAt: Timestamp = 0L,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Trashable {
    /** [value] decoded with [deserializer]; null when there is no value. */
    fun <T> valueAs(deserializer: DeserializationStrategy<T>): T? =
        value?.takeUnless { it is JsonNull }?.let { CivilightJson.decodeElement(deserializer, it) }

    companion object {
        /** Encodes a typed value for [ChangeRecord.value]. */
        fun <T> encodeValue(serializer: SerializationStrategy<T>, value: T): JsonElement =
            CivilightJson.encodeElement(serializer, value)
    }
}

/**
 * `normal`; `orphaned` = `근거 지워짐` (moved because its referent was deleted, re-attaches on
 * restore); `kept` = the author chose `이대로 두기`.
 */
enum class AnchorState { NORMAL, ORPHANED, KEPT }
