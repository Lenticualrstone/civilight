@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.story

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A chapter, episode, scene or custom-level unit (§2.2, `story/index.json`). Scene-level fields
 * (place, time, cast, memo, transition) are on the same object and meaningful only when [level] is
 * the scene level.
 */
@Serializable
data class StoryUnit(
    override val key: Key,
    /** Null for top-level units. */
    val parentKey: Key? = null,
    /** Index into `Project.structure.levels`; the last index is the scene level. */
    val level: Int,
    override val order: OrderKey,
    /** Null → `미정`. */
    val title: String? = null,
    /** Chapter/episode plan and description line; receives recorder memo lines (LRC-37). */
    val synopsis: String? = null,
    @EncodeDefault val doneMarked: Boolean = false,
    /** Item hiding (rule 19); meaningful on scene-level units only. */
    @EncodeDefault val hidden: Boolean = false,
    /** Only on numbering units (§4.1); only grows. */
    val nextBlockNumber: Int? = null,
    // Scene-level fields.
    /** 장소 or 지역. */
    val place: PlaceRef? = null,
    /** Omitted = none. */
    val time: SceneTime = SceneTime.None,
    /** 말 없는 등장 (character keys); speakers are derived from blocks. */
    val silentCast: List<Key> = emptyList(),
    /** `씬 메모`, multi-line plain text (SCR). */
    val memo: String? = null,
    val transitionMode: OpenEnum<TransitionMode>? = null,
    /** Last 직접 text, kept while the mode is auto/none. */
    val transitionRuns: List<TextRun>? = null,
    val transitionSnapshot: TransitionSnapshot? = null,
    // Trash (§5): set only on the directly deleted unit.
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable {
    /** The transition mode, `none` when not set. */
    val transitionModeOrNone: TransitionMode get() = transitionMode?.known ?: TransitionMode.NONE
}

/** A scene's place: a `Place` or a map region (project-unique key). */
@Serializable
data class PlaceRef(
    val kind: OpenEnum<PlaceRefKind>,
    val key: Key,
    override val ext: JsonObject? = null,
) : HasExt {
    constructor(kind: PlaceRefKind, key: Key) : this(kind.open(), key)

    /** Equal kind and key, ignoring extension data. */
    fun sameAs(other: PlaceRef?): Boolean = other != null && other.kind == kind && other.key == key
}

enum class PlaceRefKind { PLACE, REGION }

/**
 * Place and resolved time when the direct transition text was saved: story-day range and
 * minutes-after-04:00 range. Input of `transition.stale` (SCR-62).
 */
@Serializable
data class TransitionSnapshot(
    val place: PlaceRef? = null,
    val dayRange: IntSpan? = null,
    val tRange: IntSpan? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** Travel mode chosen for one character's pair of scenes (STO-28), in `checks.json`. */
@Serializable
data class TravelSegmentOverride(
    override val key: Key,
    val characterKey: Key,
    val fromSceneKey: Key,
    val toSceneKey: Key,
    val modeKey: Key,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity
