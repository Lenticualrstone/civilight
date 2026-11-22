@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.checks

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * `맞음` or `이대로 두기` on a check item (§2.9, `checks.json › acknowledgements`). A [fingerprint]
 * that no longer matches the current inputs voids it.
 */
@Serializable
data class Acknowledgement(
    override val key: Key,
    /** Registry id, see [CheckType]. */
    val checkType: String,
    /** Keys the item is about (block, scene, character, record, linkKey …). */
    val subjectKeys: List<Key>,
    val fingerprint: String,
    val kind: OpenEnum<AckKind>,
    @EncodeDefault val createdAt: Timestamp = 0L,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity

/** `맞음` · `이대로 두기`. */
enum class AckKind { MATCH, KEEP }

/** Check type ids of the STO-40 registry (§2.9). Results are never stored. */
object CheckType {
    const val TERM_BEFORE_REVEAL = "term.beforeReveal"
    const val TERM_CANDIDATE_BEFORE_REVEAL = "term.candidateBeforeReveal"
    const val TERM_NO_REVEAL = "term.noReveal"
    const val SPEAKER_MISSING = "speaker.missing"
    const val VOICE_EMPTY = "voice.empty"
    const val VOICE_FORMAT_DIFFERS = "voice.formatDiffers"
    const val VOICE_DUPLICATE = "voice.duplicate"
    const val CHANGE_ANCHOR_DELETED = "change.anchorDeleted"
    const val DELETED_ITEM = "deleted.item"
    const val TRAVEL_SHORTAGE = "travel.shortage"
    const val TRAVEL_SAME_TIME_TWO_PLACES = "travel.sameTimeTwoPlaces"
    const val TRAVEL_SIMULTANEOUS = "travel.simultaneous"
    const val TRAVEL_NO_PLACE = "travel.noPlace"
    const val TRAVEL_NO_SCALE = "travel.noScale"
    const val TIME_CLOCK_BAND_MISMATCH = "time.clockBandMismatch"
    const val TIME_ORDER = "time.order"
    const val TRANSITION_STALE = "transition.stale"
    const val TRANSITION_PLACE_BEFORE_REVEAL = "transition.placeBeforeReveal"
    const val HIDDEN_CHARACTER_IN_VISIBLE_SCENE = "hidden.characterInVisibleScene"
    const val HIDDEN_LOCKED_GROUP = "hidden.lockedGroup"

    /** All ids in registry order (#1 … #20). */
    val ALL: List<String> = listOf(
        TERM_BEFORE_REVEAL, TERM_CANDIDATE_BEFORE_REVEAL, TERM_NO_REVEAL, SPEAKER_MISSING, VOICE_EMPTY,
        VOICE_FORMAT_DIFFERS, VOICE_DUPLICATE, CHANGE_ANCHOR_DELETED, DELETED_ITEM, TRAVEL_SHORTAGE,
        TRAVEL_SAME_TIME_TWO_PLACES, TRAVEL_SIMULTANEOUS, TRAVEL_NO_PLACE, TRAVEL_NO_SCALE,
        TIME_CLOCK_BAND_MISMATCH, TIME_ORDER, TRANSITION_STALE, TRANSITION_PLACE_BEFORE_REVEAL,
        HIDDEN_CHARACTER_IN_VISIBLE_SCENE, HIDDEN_LOCKED_GROUP,
    )

    /** How each ackable type can be acknowledged; types not listed cannot be. */
    val ACKABLE_AS: Map<String, AckKind> = mapOf(
        TERM_BEFORE_REVEAL to AckKind.MATCH,
        VOICE_FORMAT_DIFFERS to AckKind.MATCH,
        CHANGE_ANCHOR_DELETED to AckKind.KEEP,
        DELETED_ITEM to AckKind.KEEP,
        TRAVEL_SHORTAGE to AckKind.MATCH,
        TRAVEL_SAME_TIME_TWO_PLACES to AckKind.MATCH,
        TRAVEL_SIMULTANEOUS to AckKind.MATCH,
        TIME_CLOCK_BAND_MISMATCH to AckKind.MATCH,
        TIME_ORDER to AckKind.MATCH,
        TRANSITION_STALE to AckKind.MATCH,
        TRANSITION_PLACE_BEFORE_REVEAL to AckKind.MATCH,
        HIDDEN_CHARACTER_IN_VISIBLE_SCENE to AckKind.MATCH,
    )
}
