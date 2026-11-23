@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.script

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.isBlankText
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A script block (§2.3), stored in `story/<numbering unit key>.json`. [number] is unique per
 * numbering unit and shown `#043`; identity is [key].
 */
@Serializable
data class Block(
    override val key: Key,
    val sceneKey: Key,
    override val order: OrderKey,
    val number: Int,
    val kind: OpenEnum<BlockKind>,
    /** Dialogue only; null = `화자 없음`. */
    val speakerKey: Key? = null,
    /** Text of dialogue, narration, system and memo blocks. */
    @EncodeDefault val runs: List<TextRun> = emptyList(),
    /** Dialogue only, used when `stagePositionEnabled`. */
    val stage: OpenEnum<StagePosition>? = null,
    /** Direction only. */
    val cues: List<DirectionCue> = emptyList(),
    /** Choice only, at least 2. */
    val options: List<ChoiceOption> = emptyList(),
    /** Dialogue only. */
    val voice: VoiceState? = null,
    /** Memo made by `원고에 넣기` / `여기 넣기`. */
    val recorderSource: RecorderSource? = null,
    @EncodeDefault val createdAt: Timestamp = 0L,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    /** Restore position while trashed (SCR-39). */
    val trashPlacement: TrashPlacement? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    /**
     * (0.2.0, N-43, SCR-78) `이 블록만 · 표시 이름`; trimmed, blank stored as null. Kept when the kind
     * changes away from dialogue (not drawn then).
     */
    val displayNameOverride: String? = null,
    /** (0.2.0, N-41, SCR-77) Memo only; non-null = `할 일` on. */
    val todo: BlockTodo? = null,
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable {
    /**
     * True when the block shows anything: text in its runs, a direction cue, or text in a choice
     * option. A block of an unknown kind counts by its runs. Used for the 집필 선 (§4.4).
     * (0.2.0, SCR-04) A direction block also counts by its text line.
     */
    fun hasContent(): Boolean = when (kind.known) {
        BlockKind.DIRECTION -> cues.isNotEmpty() || !runs.isBlankText()
        BlockKind.CHOICE -> options.any { !it.runs.isBlankText() }
        else -> !runs.isBlankText()
    }
}

/** (0.2.0, N-41) `할 일` of a memo block: `열림` until [done], then `끝냄`. */
@Serializable
data class BlockTodo(
    @EncodeDefault val done: Boolean = false,
    val doneAt: Timestamp? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** Block kinds; an unknown kind from a newer file is kept read-only (§8.3). */
enum class BlockKind { DIALOGUE, NARRATION, DIRECTION, CHOICE, SYSTEM, MEMO }

enum class StagePosition { LEFT, CENTER, RIGHT }

/** One cue of a direction block. */
@Serializable
data class DirectionCue(
    val key: Key,
    val kind: OpenEnum<CueKind>,
    /** → [ScriptAsset]; required except for a screen effect. */
    val assetKey: Key? = null,
    /** Screen effect name (`fade`, `blackout`, `shake`). */
    val effect: String? = null,
    override val ext: JsonObject? = null,
) : HasExt

enum class CueKind { BACKGROUND, BGM, SFX, CG, SCREEN_EFFECT }

/** One option line of a choice block; display index `#045-1` is derived. */
@Serializable
data class ChoiceOption(
    val key: Key,
    override val order: OrderKey,
    @EncodeDefault val runs: List<TextRun> = emptyList(),
    val speakerKey: Key? = null,
    val voice: VoiceState? = null,
    override val ext: JsonObject? = null,
) : HasExt, Ordered

/** Voice-ID state of a dialogue block or spoken option (SCR-23; `lastExport` is `ED`). */
@Serializable
data class VoiceState(
    @EncodeDefault val mode: OpenEnum<VoiceMode> = VoiceMode.AUTO.open(),
    /** The exported ID; required when fixed. */
    val fixedId: String? = null,
    /** First export time. */
    val fixedAt: Timestamp? = null,
    /** ID typed by the author when the format is `직접`. */
    val manualId: String? = null,
    /** Inputs of the CSV `바뀜` state (SCR Q-27). */
    val lastExport: VoiceExport? = null,
    override val ext: JsonObject? = null,
) : HasExt

enum class VoiceMode { AUTO, FIXED }

@Serializable
data class VoiceExport(
    val at: Timestamp,
    /** `sha256-<hex>` of the exported text. */
    val textHash: String,
    val speakerKey: Key? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** `버린 ID`: never reissued; exported as CSV `버림` rows. */
@Serializable
data class DiscardedVoiceId(
    val id: String,
    val blockKey: Key,
    val optionKey: Key? = null,
    val discardedAt: Timestamp,
    val reason: OpenEnum<DiscardReason>,
    override val ext: JsonObject? = null,
) : HasExt

enum class DiscardReason { DELETED, KIND_CHANGED, MERGED, REPLACED, FORMAT_REFIXED }

/** Where a trashed block was (SCR-39). */
@Serializable
data class TrashPlacement(
    val prevKey: Key? = null,
    val nextKey: Key? = null,
    /** 0-based position in the scene at deletion time. */
    val ordinal: Int,
    override val ext: JsonObject? = null,
) : HasExt
