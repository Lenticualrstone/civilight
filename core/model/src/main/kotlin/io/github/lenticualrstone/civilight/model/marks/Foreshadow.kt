@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.marks

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * (0.2.0, N-41, SCR-77) One foreshadowing (`복선`), stored in `story/index.json › foreshadows`
 * (additive; a 0.1.0 file has none). Live rows sit in the normal file, trashed ones in the `trash/`
 * mirror.
 *
 * Its anchors name blocks by key. When an anchored block stops being live, the mark is drawn on the
 * next live block (rule 12 §2) by derivation ([Marks.resolve]); the stored key never changes, so a
 * restore or undo re-attaches it.
 */
@Serializable
data class Foreshadow(
    override val key: Key,
    /** `이름`, one line, required. */
    val name: String,
    /** `복선 심기` block. */
    val plant: MarkAnchor,
    /** `거둘 곳` (an episode-level unit, shown `2-5`); optional. */
    val payoffUnitKey: Key? = null,
    /** `다시 언급` blocks, oldest first. */
    @EncodeDefault val mentions: List<MarkAnchor> = emptyList(),
    /** `거두기` block; non-null = `거둠`. */
    val payoff: MarkAnchor? = null,
    @EncodeDefault val createdAt: Timestamp = 0L,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Trashable {
    /** Every anchor of this item: plant, mentions, then the payoff. */
    val anchors: List<MarkAnchor> get() = listOf(plant) + mentions + listOfNotNull(payoff)
}

/** A block a mark sits on (SCR-77). */
@Serializable
data class MarkAnchor(
    val blockKey: Key,
    override val ext: JsonObject? = null,
) : HasExt
