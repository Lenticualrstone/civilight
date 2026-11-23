@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.file

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.cast.ArchiveDoc
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.VoiceLine
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.marks.Foreshadow
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.ScriptAsset
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.story.TravelSegmentOverride
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry
import io.github.lenticualrstone.civilight.model.trash.MergeRecord
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.world.Term
import io.github.lenticualrstone.civilight.model.world.TimelineEvent
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// File units of §7 (and of the working store, §8.2). `project.json` is
// [io.github.lenticualrstone.civilight.model.project.Project]; `maps/<key>.json` is
// [io.github.lenticualrstone.civilight.model.map.WorldMap]; `canvas/<key>.json` is
// [io.github.lenticualrstone.civilight.model.canvas.Canvas]; `pages/<key>.json` is
// [io.github.lenticualrstone.civilight.model.canvas.Page]. The same shapes hold the non-live rows
// under `trash/` (§7.9).

/**
 * `story/index.json`: the unit tree with scene fields. [discardedVoiceIds] is read for tolerance only
 * (§9 places them here, §7.4 in the episode file; see the status file's spec conflicts).
 */
@Serializable
data class StoryIndexFile(
    @EncodeDefault val units: List<StoryUnit> = emptyList(),
    val discardedVoiceIds: List<DiscardedVoiceId> = emptyList(),
    /**
     * (0.2.0, N-41) Foreshadowings (`복선`); additive, not written while empty. Live rows here,
     * trashed rows in `trash/story/index.json`.
     */
    val foreshadows: List<Foreshadow> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `story/<numbering unit key>.json`: blocks of one numbering unit and its discarded voice IDs. */
@Serializable
data class EpisodeFile(
    /** The episode-level unit, or the project key for 1-level projects. */
    val unitKey: Key,
    @EncodeDefault val blocks: List<Block> = emptyList(),
    @EncodeDefault val discardedVoiceIds: List<DiscardedVoiceId> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `cast.json`. */
@Serializable
data class CastFile(
    @EncodeDefault val characters: List<Character> = emptyList(),
    @EncodeDefault val relations: List<Relation> = emptyList(),
    @EncodeDefault val voiceLines: List<VoiceLine> = emptyList(),
    @EncodeDefault val archiveDocs: List<ArchiveDoc> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `world.json`. */
@Serializable
data class WorldFile(
    @EncodeDefault val places: List<Place> = emptyList(),
    @EncodeDefault val terms: List<Term> = emptyList(),
    @EncodeDefault val factions: List<Faction> = emptyList(),
    @EncodeDefault val events: List<TimelineEvent> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `changes.json`: `◆` records and `◇` term reveals. */
@Serializable
data class ChangesFile(
    @EncodeDefault val records: List<ChangeRecord> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `checks.json` (feature `checks.ack`). */
@Serializable
data class ChecksFile(
    @EncodeDefault val acknowledgements: List<Acknowledgement> = emptyList(),
    @EncodeDefault val travelOverrides: List<TravelSegmentOverride> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `tombstones.json` (feature `tombstones`). */
@Serializable
data class TombstonesFile(
    @EncodeDefault val nameTags: List<NameTag> = emptyList(),
    @EncodeDefault val mergeRecords: List<MergeRecord> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `deletions.json` (feature `deletions`). */
@Serializable
data class DeletionsFile(
    @EncodeDefault val entries: List<DeletionLogEntry> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `assets/index.json`: blobs, image uses and script assets. */
@Serializable
data class AssetsIndexFile(
    @EncodeDefault val blobs: List<Blob> = emptyList(),
    @EncodeDefault val imageUses: List<ImageUse> = emptyList(),
    @EncodeDefault val scriptAssets: List<ScriptAsset> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `trash/index.json` (feature `trash`). */
@Serializable
data class TrashIndexFile(
    @EncodeDefault val entries: List<TrashEntry> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt
