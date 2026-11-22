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
    /**
     * Screen id → list, grid or card (`library` → list). Gallery screens use the ids
     * [GALLERY_CHARACTER] and [GALLERY_PROJECT] (55 GAL-04); only they ever store [ViewMode.GRID].
     */
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
    /** Gallery viewer `설명` toggle (55 GAL-09), remembered app-wide. */
    val galleryTextOn: Boolean = true,
    /**
     * Inner-screen two-pane ratio per module (CNV-47 `펼친 화면 비율`): module id (see [PaneSplit])
     * → chosen ratio. Absent = [PaneSplit.DEFAULT] (today's layout); not written while empty.
     */
    val paneSplit: Map<String, PaneSplit> = emptyMap(),
) {
    /** The remembered pane ratio of [module] (CNV-47); [PaneSplit.DEFAULT] when none was chosen. */
    fun paneSplitOf(module: String): PaneSplit = paneSplit[module] ?: PaneSplit.DEFAULT

    /** [module] set to [split]; choosing [PaneSplit.DEFAULT] removes the entry. */
    fun withPaneSplit(module: String, split: PaneSplit): AppSettings =
        copy(paneSplit = if (split == PaneSplit.DEFAULT) paneSplit - module else paneSplit + (module to split))

    companion object {
        /** [viewByScreen] id of every character gallery (not per character, 55 GAL-04). */
        const val GALLERY_CHARACTER = "characterGallery"

        /** [viewByScreen] id of the project gallery. */
        const val GALLERY_PROJECT = "projectGallery"
    }
}

/**
 * CNV-47: the left : right width ratio of a two-pane layout on the inner screen (list : detail, or
 * manuscript : side panel). [DEFAULT] is the layout the module draws without a choice (the `기본`
 * button). Module ids are the keys of [AppSettings.paneSplit]; every lane uses [Modules].
 */
@Serializable(with = PaneSplit.Codec::class)
enum class PaneSplit(
    /** Left pane share of the two panes' width, or null for [DEFAULT]. */
    val listFraction: Float?,
) {
    ONE_TWO(1f / 3f), ONE_ONE(0.5f), TWO_ONE(2f / 3f), DEFAULT(null);

    object Codec : LowerCamelEnumSerializer<PaneSplit>("PaneSplit", entries, DEFAULT)

    /**
     * Module ids of [AppSettings.paneSplit]: the two-pane layouts of CNV-47, one per `ListDetailPairs`
     * list module (서재, 홈, 인물, 세계, 연표, 레코더 기록함, 점검 센터), the gallery (GAL-23) and the
     * script's side panel.
     */
    object Modules {
        const val LIBRARY = "library"
        const val HOME = "home"
        const val CAST = "cast"
        const val WORLD = "world"
        const val TIMELINE = "timeline"
        const val RECORDER = "recorder"
        const val CHECKS = "checks"
        const val GALLERY = "gallery"
        const val SCRIPT_PANEL = "scriptPanel"

        val ALL: List<String> = listOf(LIBRARY, HOME, CAST, WORLD, TIMELINE, RECORDER, CHECKS, GALLERY, SCRIPT_PANEL)
    }

    companion object {
        /** The three ratios a released drag settles to, in on-screen order (`1:2 · 1:1 · 2:1`). */
        val SNAP: List<PaneSplit> = listOf(ONE_TWO, ONE_ONE, TWO_ONE)

        /** The ratio of [SNAP] nearest to a dragged list share [fraction] (0…1). */
        fun nearest(fraction: Float): PaneSplit = SNAP.minBy { kotlin.math.abs(it.listFraction!! - fraction) }
    }
}

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
    /** List rows. */
    LIST,

    /** Card shelf. */
    CARD,

    /** Three-column tile grid; gallery screens only (55 GAL-06). */
    GRID;

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
