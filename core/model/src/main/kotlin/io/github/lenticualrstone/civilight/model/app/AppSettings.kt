package io.github.lenticualrstone.civilight.model.app

import io.github.lenticualrstone.civilight.model.json.LowerCamelEnumSerializer
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import kotlinx.serialization.Serializable

/** The one app-settings record (class S, DataStore; §2.13). Never in `.civilight`. */
@Serializable
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Text size step; 0 = `보통` (18 sp · 1.7). */
    val textSizeStep: Int = 0,
    val unfoldedLayout: UnfoldedLayout = UnfoldedLayout.LIST_DETAIL,
    val animation: AnimationPref = AnimationPref.FOLLOW_SYSTEM,
    val reduceTransparency: Boolean = false,
    val recorderGlow: Boolean = true,
    val holoDefaultForNewImages: Boolean = false,
    /** Screen id → list or card (`library` → list). */
    val viewByScreen: Map<String, ViewMode> = mapOf("library" to ViewMode.LIST),
    val appLock: Boolean = false,
    val relockPolicy: RelockPolicy = RelockPolicy.ON_LEAVE,
    val hiddenInSearchWhileOpen: Boolean = true,
    val hiddenOutOfWidgetsRecents: Boolean = true,
    val backupHidden: Boolean = true,
    val driveBackup: Boolean = false,
    val driveAccountId: String? = null,
    val firstUseTips: Boolean = true,
    val defaultSceneNotation: SceneNotationPref = SceneNotationPref.EPISODE,
    val checkTravel: Boolean = true,
    val checkNoPlace: Boolean = true,
    val defaultMapStyle: MapStylePref = MapStylePref.SOFT,
    val textMapEngine: TextMapEngine = TextMapEngine.ON_DEVICE,
    val largeMapExport: LargeMapExport = LargeMapExport.K4,
    val recorder: RecorderSettings = RecorderSettings(),
    val ratingDefaults: RatingDefaults = RatingDefaults(),
)

/** Rating protection defaults (LIB-23). Fixed: R-15 hide/name/recorder off; ALL all off. */
@Serializable
data class RatingDefaults(
    val r18Hide: Boolean = true,
    val r18Capture: Boolean = true,
    val r18NameMask: Boolean = true,
    val r18RecorderMask: Boolean = true,
    val r15Capture: Boolean = false,
    val exportCoverRating: Boolean = true,
    /** Rating serial names whose defaults changed; drives `이미 있는 … 같이 바꾸기`. */
    val changedGroups: Set<String> = emptySet(),
) {
    fun changed(rating: Rating): Boolean = io.github.lenticualrstone.civilight.model.OpenEnum.serialNameOf(rating.name) in changedGroups
}

@Serializable(with = ThemeMode.Codec::class)
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    object Codec : LowerCamelEnumSerializer<ThemeMode>("ThemeMode", entries, SYSTEM)
}

@Serializable(with = UnfoldedLayout.Codec::class)
enum class UnfoldedLayout {
    LIST_DETAIL, CENTER_COLUMN;

    object Codec : LowerCamelEnumSerializer<UnfoldedLayout>("UnfoldedLayout", entries, LIST_DETAIL)
}

@Serializable(with = AnimationPref.Codec::class)
enum class AnimationPref {
    FOLLOW_SYSTEM, ALWAYS_REDUCE, NEVER_REDUCE;

    object Codec : LowerCamelEnumSerializer<AnimationPref>("AnimationPref", entries, FOLLOW_SYSTEM)
}

@Serializable(with = ViewMode.Codec::class)
enum class ViewMode {
    LIST, CARD;

    object Codec : LowerCamelEnumSerializer<ViewMode>("ViewMode", entries, LIST)
}

@Serializable(with = RelockPolicy.Codec::class)
enum class RelockPolicy {
    ON_LEAVE, SCREEN_OFF_ONLY, FIVE_MINUTES;

    object Codec : LowerCamelEnumSerializer<RelockPolicy>("RelockPolicy", entries, ON_LEAVE)
}

/** App default of [SceneNotation] for new projects. */
@Serializable(with = SceneNotationPref.Codec::class)
enum class SceneNotationPref {
    EPISODE, STORY_START;

    val notation: SceneNotation get() = SceneNotation.valueOf(name)

    object Codec : LowerCamelEnumSerializer<SceneNotationPref>("SceneNotationPref", entries, EPISODE)
}

@Serializable(with = MapStylePref.Codec::class)
enum class MapStylePref {
    SOFT, INK, COLOR, PARCHMENT;

    object Codec : LowerCamelEnumSerializer<MapStylePref>("MapStylePref", entries, SOFT)
}

@Serializable(with = TextMapEngine.Codec::class)
enum class TextMapEngine {
    ON_DEVICE, CLOUD;

    object Codec : LowerCamelEnumSerializer<TextMapEngine>("TextMapEngine", entries, ON_DEVICE)
}

@Serializable(with = LargeMapExport.Codec::class)
enum class LargeMapExport {
    K4, K8;

    object Codec : LowerCamelEnumSerializer<LargeMapExport>("LargeMapExport", entries, K4)
}
