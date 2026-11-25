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
    /**
     * `이동 점검` (STO-42). (0.2.0, S-2) Off by default. 0.1.0 never stored `true` (the old default),
     * so upgraded installs read off as well; a user who turns it on stores `true`.
     */
    val checkTravel: Boolean = false,
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
    /** (0.3.3, Atelier) `서재 보기`. The old `viewByScreen["library"]` is kept and not read. */
    val libraryStyle: LibraryStyle = LibraryStyle.CD_JEWEL_CASE,
    /** (0.2.0, U-16, CST-67) `카드 보기` of the character list. The old `viewByScreen["cast"]` is kept and not read. */
    val castCardStyle: CardStyle = CardStyle.FAN_DECK,
    /** (0.2.0) 서재 head toggle: `LIST` = the list, `CARD` = the chosen 3D view; absent = `CARD` (00 §13.4). */
    val libraryToggle: ViewMode? = null,
    /** (0.2.0) Character list head toggle, same meaning as [libraryToggle]. */
    val castToggle: ViewMode? = null,
    /** (0.2.0, LIB-94) Tilt follow switch; see [Tilt.follows]. */
    val viewTilt: Boolean = true,
    /**
     * (0.2.3, r3 `패널 닫기`) Side panel closed per two-pane screen: module id (see [PaneSplit.Modules]) → closed.
     * Absent reads [panelClosedOf]'s migrated default; not written while empty.
     */
    val panelClosed: Map<String, Boolean> = emptyMap(),
    /**
     * (0.2.3, r3 `galviews`) The r3 gallery view per gallery screen: [GALLERY_CHARACTER] / [GALLERY_PROJECT] → view id
     * (feature:canvas `GalleryStyle`). Only the seven new views are stored here; `목록` · `격자` · `카드 진열` stay in
     * [viewByScreen], which an r3 view also sets to [ViewMode.CARD] so an older build shows `카드`. Absent or an
     * unknown id reads [viewByScreen]; not written while empty.
     */
    val galleryStyle: Map<String, String> = emptyMap(),
    /** (0.4.0) Mechanical detent drag resistance for Atelier shelves. */
    val dragResistance: DragResistance = DragResistance.FIRM,
    /** (0.4.0) Character portrait shape in script & lists (1:1 rounded square or circle). */
    val profileShape: ProfileShape = ProfileShape.SQUARE,
    /** (0.4.0) Breathing spacing between script blocks (`좁음 / 중간 / 넓음`). */
    val blockSpacing: BlockSpacing = BlockSpacing.NORMAL,
    /** (0.4.0) Subtle floating outline + wiggle on block/card long press. */
    val longPressWiggle: Boolean = true,
    /** (0.4.0) Typewriter scrolling keeps active editing block centered vertically. */
    val typewriterScroll: Boolean = false,
    /** (0.4.0) Smart scenario punctuation (`--` → `──`, `...` → `…`). */
    val smartPunctuation: Boolean = true,
    /** (0.4.0, Experimental) Suggest previous partner speaker on next dialogue block. */
    val smartSpeakerSwap: Boolean = false,
    /** (0.4.0, Experimental) Show Life Recorder entry points (off by default for pure writing focus). */
    val lifeRecorderEnabled: Boolean = false,
    /** (0.4.0) Customized icon slots on collapsed mini rail in Script Editor. */
    val miniRailScript: List<MiniRailScriptItem> = MiniRailScriptItem.DEFAULT,
    /** (0.4.0) Customized icon slots on collapsed mini rail in Project workspace. */
    val miniRailProject: List<MiniRailProjectItem> = MiniRailProjectItem.DEFAULT,
    /** (0.5.0) Subtle portrait-based ambient blur backdrop behind PortraitHero (default ON). */
    val portraitAmbientBlur: Boolean = true,
    /** (0.5.0) Side placement of the collapsed mini icon rail & script side panel (default LEFT). */
    val miniRailSide: MiniRailSide = MiniRailSide.LEFT,
    /** (0.5.0) Active character profile tab preset (`표준 창작`, `현대극`, `게임/VN`, `미니멀`). */
    val profileTabPreset: ProfileTabPreset = ProfileTabPreset.STANDARD,
    /** (0.5.0) User-customized character profile tabs (empty = use [profileTabPreset] defaults). */
    val customProfileTabs: List<ProfileTabConfig> = emptyList(),
) {
    /**
     * Whether [module]'s side panel (list or script panel) is closed (0.2.3, r3). Without a choice of its own,
     * the retired `가운데 한 칸` ([UnfoldedLayout.CENTER_COLUMN], kept stored and no longer offered) reads as
     * closed: one wide main pane, the nearest thing to the old single column. Nothing is rewritten.
     */
    fun panelClosedOf(module: String): Boolean = panelClosed[module] ?: (unfoldedLayout == UnfoldedLayout.CENTER_COLUMN)

    /** [module]'s side panel set to [closed] (stored explicitly, so it no longer follows the migrated default). */
    fun withPanelClosed(module: String, closed: Boolean): AppSettings = copy(panelClosed = panelClosed + (module to closed))

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
 * (0.4.0, Atelier) 6 handcrafted `서재 보기` styles: 1 hyper-detailed 3D physical object view
 * (CD Jewel Case) + 5 tactile kinetic workspace views.
 * Unknown legacy stored values (including retired `floppyCrt`) gracefully fall back to [CD_JEWEL_CASE].
 */
@Serializable(with = LibraryStyle.Codec::class)
enum class LibraryStyle {
    /** `CD 케이스` — 3D 플라스틱 주얼 케이스 · 힌지 개폐 · 홀로그램 CD (기본값). */
    CD_JEWEL_CASE,

    /** `탄성 덱` — 기계식 힌지 텐션 · 부채꼴 덱. */
    KINETIC_DECK,

    /** `모자이크` — 에디토리얼 격자 · 포커스. */
    BENTO_MOSAIC,

    /** `필름 릴` — 35mm 필름 · 내부 패럴랙스. */
    FILM_REEL,

    /** `시간 축` — 맥동 노드 · 작업 연대기. */
    PULSE_TIMELINE,

    /** `스튜디오 목록` — 모션 목록 · 빠른 작업. */
    LIST;

    val isList: Boolean get() = this == LIST

    val isSpatial: Boolean get() = this == CD_JEWEL_CASE

    object Codec : LowerCamelEnumSerializer<LibraryStyle>("LibraryStyle", entries, CD_JEWEL_CASE)
}

/** (0.2.0, U-16) Character `카드 보기` values in settings order (00 §13.4). Unknown stored values read [FAN_DECK]. */
@Serializable(with = CardStyle.Codec::class)
enum class CardStyle {
    /** `부채꼴 덱` (D1). */
    FAN_DECK,

    /** `회전 목마`. */
    CAROUSEL,

    /** `겹친 더미`. */
    STACK,

    /** `타로 펼침`. */
    TAROT,

    /** `기울어진 벽`. */
    TILTED_WALL,

    /** `롤로덱스`. */
    ROLODEX,

    /** `목록`. */
    LIST;

    val isList: Boolean get() = this == LIST

    object Codec : LowerCamelEnumSerializer<CardStyle>("CardStyle", entries, FAN_DECK)
}

/**
 * (0.2.0, 00 §13.4) Which view a 3D-capable list shows. `style == LIST` → the list and no toggle;
 * a `LIST` toggle → the list; otherwise (absent, `CARD`, or a `GRID` read here) → the chosen style.
 */
object ShownView {
    fun of(style: LibraryStyle, toggle: ViewMode?): LibraryStyle =
        if (style.isList || toggle == ViewMode.LIST) LibraryStyle.LIST else style

    fun of(style: CardStyle, toggle: ViewMode?): CardStyle =
        if (style.isList || toggle == ViewMode.LIST) CardStyle.LIST else style

    /** The head toggle is drawn only when a 3D style is chosen. */
    fun showsToggle(style: LibraryStyle): Boolean = !style.isList

    fun showsToggle(style: CardStyle): Boolean = !style.isList

    /** 서재 of [settings]. */
    fun library(settings: AppSettings): LibraryStyle = of(settings.libraryStyle, settings.libraryToggle)

    /** Character list of [settings]. */
    fun cast(settings: AppSettings): CardStyle = of(settings.castCardStyle, settings.castToggle)
}

/**
 * (0.2.0, LIB-94, 00 §13.4) Tilt (parallax and light) follows the device only while [viewTilt] is on
 * and motion is not reduced (CNV-46: `ALWAYS_REDUCE`, or `FOLLOW_SYSTEM` with the system flag on).
 */
object Tilt {
    fun follows(viewTilt: Boolean, animation: AnimationPref, systemReduceMotion: Boolean): Boolean =
        viewTilt && !motionReduced(animation, systemReduceMotion)

    fun motionReduced(animation: AnimationPref, systemReduceMotion: Boolean): Boolean = when (animation) {
        AnimationPref.ALWAYS_REDUCE -> true
        AnimationPref.NEVER_REDUCE -> false
        AnimationPref.FOLLOW_SYSTEM -> systemReduceMotion
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

        /** (0.3.0) The map screens' floating side panel (editor, node map, 상세 지형): closed or open only. */
        const val MAP_PANEL = "mapPanel"

        val ALL: List<String> = listOf(LIBRARY, HOME, CAST, WORLD, TIMELINE, RECORDER, CHECKS, GALLERY, SCRIPT_PANEL, MAP_PANEL)
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

/**
 * `펼친 화면 배치` (CNV-33). (0.2.3, r3) No longer offered or read for layout: two panes are always drawn and
 * each screen's side panel closes by its own button ([AppSettings.panelClosed]). The stored value is kept and
 * only seeds [AppSettings.panelClosedOf].
 */
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

/** (0.4.0) Mechanical detent drag resistance presets for Atelier shelves. */
@Serializable(with = DragResistance.Codec::class)
enum class DragResistance(
    /** Resistance damping before breakaway threshold (lower = firmer tension). */
    val preBreakawayGain: Float,
    /** Distance multiplier per item step (higher = heavier travel). */
    val stepDistanceScale: Float,
    /** Normalized position threshold past which the detent releases to the next card. */
    val breakawayThreshold: Float,
    /** Spring stiffness for mechanical snap settle. */
    val springStiffness: Float,
    /** Spring damping ratio (near-critically damped, zero cheap bounce). */
    val springDamping: Float,
) {
    SOFT(0.72f, 0.92f, 0.30f, 640f, 0.80f),
    NORMAL(0.52f, 1.15f, 0.36f, 560f, 0.83f),
    FIRM(0.36f, 1.38f, 0.42f, 490f, 0.86f),
    HEAVY(0.24f, 1.62f, 0.47f, 420f, 0.90f);

    object Codec : LowerCamelEnumSerializer<DragResistance>("DragResistance", entries, FIRM)
}

/** (0.4.0) Character portrait frame shape in script & profile views. */
@Serializable(with = ProfileShape.Codec::class)
enum class ProfileShape {
    SQUARE, CIRCLE;

    object Codec : LowerCamelEnumSerializer<ProfileShape>("ProfileShape", entries, SQUARE)
}

/** (0.4.0) Vertical breathing gap preset between script blocks. */
@Serializable(with = BlockSpacing.Codec::class)
enum class BlockSpacing(val sameSpeakerDp: Int, val shiftDp: Int) {
    COMPACT(8, 18),
    NORMAL(14, 26),
    RELAXED(20, 34);

    object Codec : LowerCamelEnumSerializer<BlockSpacing>("BlockSpacing", entries, NORMAL)
}

/** (0.4.0) Customizable icon shortcuts on the collapsed mini rail in Script Editor. */
@Serializable(with = MiniRailScriptItem.Codec::class)
enum class MiniRailScriptItem {
    TIME, REF, PROPS, MODS, OUTLINE, SPEAKER_FOCUS;

    companion object {
        val DEFAULT: List<MiniRailScriptItem> = listOf(TIME, REF, PROPS, MODS, OUTLINE, SPEAKER_FOCUS)
    }

    object Codec : LowerCamelEnumSerializer<MiniRailScriptItem>("MiniRailScriptItem", entries, TIME)
}

/** (0.4.0) Customizable icon shortcuts on the collapsed mini rail in Project workspace. */
@Serializable(with = MiniRailProjectItem.Codec::class)
enum class MiniRailProjectItem {
    SCRIPT, CAST, WORLD, MAP, TIMELINE, CHECKS, GALLERY, SETTINGS;

    companion object {
        val DEFAULT: List<MiniRailProjectItem> = listOf(SCRIPT, CAST, WORLD, MAP, TIMELINE, CHECKS)
    }

    object Codec : LowerCamelEnumSerializer<MiniRailProjectItem>("MiniRailProjectItem", entries, SCRIPT)
}

/** (0.5.0) Placement side for the collapsed mini icon rail & script side panel. */
@Serializable(with = MiniRailSide.Codec::class)
enum class MiniRailSide {
    LEFT, RIGHT;

    object Codec : LowerCamelEnumSerializer<MiniRailSide>("MiniRailSide", entries, LEFT)
}

/** (0.5.0) Built-in & modular character profile tab kinds. */
@Serializable(with = ProfileTabKind.Codec::class)
enum class ProfileTabKind {
    OVERVIEW,
    EQUIPMENT,
    ABILITIES,
    RELATIONS,
    PAST,
    TRIVIA,
    APPEARANCES,
    SPEECH,
    GAME_TEXT,
    SECRET,
    CUSTOM;

    object Codec : LowerCamelEnumSerializer<ProfileTabKind>("ProfileTabKind", entries, CUSTOM)
}

/** (0.5.0) One modular tab slot in a character profile template/preset. */
@Serializable
data class ProfileTabConfig(
    val id: String,
    val label: String,
    val kind: ProfileTabKind,
    val enabled: Boolean = true,
)

/** (0.5.0) Ready-made character profile tab presets. */
@Serializable(with = ProfileTabPreset.Codec::class)
enum class ProfileTabPreset {
    STANDARD,
    MODERN_DRAMA,
    GAME_VN,
    MINIMAL;

    fun defaultTabs(): List<ProfileTabConfig> = when (this) {
        STANDARD -> listOf(
            ProfileTabConfig("overview", "개요", ProfileTabKind.OVERVIEW),
            ProfileTabConfig("equipment", "장비", ProfileTabKind.EQUIPMENT),
            ProfileTabConfig("abilities", "능력", ProfileTabKind.ABILITIES),
            ProfileTabConfig("relations", "관계", ProfileTabKind.RELATIONS),
            ProfileTabConfig("past", "과거", ProfileTabKind.PAST),
            ProfileTabConfig("trivia", "여담", ProfileTabKind.TRIVIA),
            ProfileTabConfig("appearances", "작중 행적", ProfileTabKind.APPEARANCES),
        )
        MODERN_DRAMA -> listOf(
            ProfileTabConfig("overview", "개요", ProfileTabKind.OVERVIEW),
            ProfileTabConfig("relations", "관계", ProfileTabKind.RELATIONS),
            ProfileTabConfig("speech", "말투", ProfileTabKind.SPEECH),
            ProfileTabConfig("past", "과거", ProfileTabKind.PAST),
            ProfileTabConfig("trivia", "여담", ProfileTabKind.TRIVIA),
            ProfileTabConfig("appearances", "작중 행적", ProfileTabKind.APPEARANCES),
        )
        GAME_VN -> listOf(
            ProfileTabConfig("overview", "개요", ProfileTabKind.OVERVIEW),
            ProfileTabConfig("equipment", "장비", ProfileTabKind.EQUIPMENT),
            ProfileTabConfig("abilities", "능력", ProfileTabKind.ABILITIES),
            ProfileTabConfig("relations", "관계", ProfileTabKind.RELATIONS),
            ProfileTabConfig("speech", "말투", ProfileTabKind.SPEECH),
            ProfileTabConfig("game_text", "게임 텍스트", ProfileTabKind.GAME_TEXT),
            ProfileTabConfig("appearances", "작중 행적", ProfileTabKind.APPEARANCES),
            ProfileTabConfig("secret", "비밀", ProfileTabKind.SECRET),
        )
        MINIMAL -> listOf(
            ProfileTabConfig("overview", "개요", ProfileTabKind.OVERVIEW),
            ProfileTabConfig("relations", "관계", ProfileTabKind.RELATIONS),
            ProfileTabConfig("appearances", "작중 행적", ProfileTabKind.APPEARANCES),
        )
    }

    object Codec : LowerCamelEnumSerializer<ProfileTabPreset>("ProfileTabPreset", entries, STANDARD)
}
