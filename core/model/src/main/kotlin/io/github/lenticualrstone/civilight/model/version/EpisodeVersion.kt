@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.version

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * (0.2.0, N-29, 00 §13.5) `versions/<unitKey>.json`: the episode versions (`판`) of one numbering
 * unit, oldest first. Working store and package path are the same; in a manual `.civilight` export
 * it is written only while the export row is on (default on), and always in backups.
 */
@Serializable
data class EpisodeVersionsFile(
    /** The numbering unit (episode level; the project key for 1-level projects). */
    val unitKey: Key,
    @EncodeDefault val versions: List<EpisodeVersion> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/**
 * One version: text, kind, speaker, number and order of the unit's live blocks at that moment.
 * `◆` records, voice IDs, cues, stage values and acknowledgements are never copied (N-29).
 */
@Serializable
data class EpisodeVersion(
    val key: Key,
    /** `11월 23일 · 초고` (manual default) or `11월 24일 · 내보낸 판` (D21). */
    val name: String,
    @EncodeDefault val origin: OpenEnum<VersionOrigin> = VersionOrigin.MANUAL.open(),
    val createdAt: Timestamp,
    @EncodeDefault val createdBy: String = "",
    /** Scene order of the unit at that moment. */
    @EncodeDefault val scenes: List<VersionScene> = emptyList(),
    /** Every live block of the unit's scenes, in manuscript order. */
    @EncodeDefault val blocks: List<VersionBlock> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

/** `판 남기기` (manual) or the automatic version of a PDF, CSV or engine JSON export. */
enum class VersionOrigin { MANUAL, EXPORT }

@Serializable
data class VersionScene(
    val key: Key,
    val title: String? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** A block as it was; `이 판 글로` matches the live block by [key]. */
@Serializable
data class VersionBlock(
    val key: Key,
    val sceneKey: Key,
    val number: Int,
    val kind: OpenEnum<BlockKind>,
    val speakerKey: Key? = null,
    @EncodeDefault val runs: List<TextRun> = emptyList(),
    /** Choice lines by option key. */
    val options: List<VersionOption> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class VersionOption(
    val key: Key,
    @EncodeDefault val runs: List<TextRun> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt
