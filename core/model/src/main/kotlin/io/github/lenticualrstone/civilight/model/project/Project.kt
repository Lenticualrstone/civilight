@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.project

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.RelationKind
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The project record and its settings groups: `project.json` (§2.1, §7.4). Field definitions, tags and
 * relation kinds are stored in the same file. The app-only columns (`isExample`, `readOnly`, trash
 * marks, last opened) live in the library index ([io.github.lenticualrstone.civilight.model.app.LibraryEntry]).
 */
@Serializable
data class Project(
    /** Portable project identity; same key = same project on open/restore (LIB-52). */
    override val key: Key,
    val name: String,
    val oneLiner: String? = null,
    @EncodeDefault val format: OpenEnum<ProjectFormat> = ProjectFormat.SERIAL.open(),
    @EncodeDefault val rating: OpenEnum<Rating> = Rating.ALL.open(),
    @EncodeDefault val protections: Protections = Protections(),
    @EncodeDefault val structure: StoryStructure = StoryStructure(),
    @EncodeDefault val worldTime: WorldTime = WorldTime(),
    @EncodeDefault val sceneTime: SceneTimeSettings = SceneTimeSettings(),
    @EncodeDefault val script: ScriptSettings = ScriptSettings(),
    @EncodeDefault val cast: CastSettings = CastSettings(),
    @EncodeDefault val travelModes: List<TravelMode> = emptyList(),
    /** Project default travel mode; null = 도보 (the first default mode). */
    val defaultTravelModeKey: Key? = null,
    @EncodeDefault val termMatching: TermMatching = TermMatching(),
    @EncodeDefault val fieldBundleAtCreation: OpenEnum<FieldBundle> = FieldBundle.BASIC.open(),
    @EncodeDefault val counters: ProjectCounters = ProjectCounters(),
    @EncodeDefault val fieldDefs: List<FieldDef> = emptyList(),
    @EncodeDefault val tags: List<Tag> = emptyList(),
    @EncodeDefault val relationKinds: List<RelationKind> = emptyList(),
    @EncodeDefault val createdAt: Timestamp = 0L,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity

/** `연재 장편 · 단편 · 설정만`. */
enum class ProjectFormat { SERIAL, SHORT, SETTING_ONLY }

/** 수위 등급. Display words (`R-15`) are never stored. */
enum class Rating { ALL, R15, R18 }

/** Starting field bundle chosen in the new-project wizard (informational, LIB-12). */
enum class FieldBundle { BASIC, GAME_PROFILE, EMPTY, FROM_PROJECT }

/** Rule 19 per-project protection values (LIB-23). */
@Serializable
data class Protections(
    @EncodeDefault val hidden: Boolean = false,
    @EncodeDefault val captureBlock: Boolean = false,
    @EncodeDefault val nameMask: Boolean = false,
    @EncodeDefault val recorderLinkMask: Boolean = false,
    override val ext: JsonObject? = null,
) : io.github.lenticualrstone.civilight.model.HasExt

/** Issue-only counters (`ED`): record numbers and, for 1-level structures, block numbers are never reused. */
@Serializable
data class ProjectCounters(
    @EncodeDefault val nextCharacterRecordNo: Int = 1,
    /** Only for 1-level structures, where the project is the numbering unit (§4.1). */
    val nextBlockNumber: Int? = null,
    override val ext: JsonObject? = null,
) : io.github.lenticualrstone.civilight.model.HasExt
