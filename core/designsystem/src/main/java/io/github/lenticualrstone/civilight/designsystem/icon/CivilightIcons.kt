package io.github.lenticualrstone.civilight.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Stroke width of every icon in 24-unit grid units (DS-12). */
const val ICON_STROKE_UNITS: Float = 1.6f

/** DS-12: the drawn stroke for an icon of [sizeDp] (1.6 grid units scale with the size). */
fun iconStrokeDp(sizeDp: Float): Float = ICON_STROKE_UNITS * sizeDp / 24f

/**
 * Every icon of DS-12 with its 24 × 24 path data (outline, stroke 1.6, round caps and joins, no fill).
 * Material icons-core glyphs are filled, so none of them is used.
 */
enum class IconId(val pathData: String) {
    Back("M15 5l-7 7 7 7"),
    Next("M9 5l7 7-7 7"),
    More("M5 12h.01M12 12h.01M19 12h.01"),
    Plus("M12 5v14M5 12h14"),
    X("M6.5 6.5l11 11M17.5 6.5l-11 11"),
    Check("M5 12.5l4.5 4.5L19 7.5"),
    Grip("M9 6h.01M15 6h.01M9 12h.01M15 12h.01M9 18h.01M15 18h.01"),
    Filter("M4 6h16M7 12h10M10 18h4"),
    Search("M10.5 17a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13zM20 20l-4.5-4.5"),
    Mic("M12 4a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V7a3 3 0 0 1 3-3zM6 11a6 6 0 0 0 12 0M12 17v3"),
    Text("M5 6h14M12 6v12M9 18h6"),
    Pause("M9 6v12M15 6v12"),
    Layers("M12 4l9 5-9 5-9-5zM3 14l9 5 9-5"),
    Link("M10 14l4-4M8.5 15.5l-1 1a3 3 0 0 1-4-4l3-3a3 3 0 0 1 4 0M15.5 8.5l1-1a3 3 0 0 1 4 4l-3 3a3 3 0 0 1-4 0"),
    Lock("M7 11V8a5 5 0 0 1 10 0v3M6 11h12v9H6zM12 15v2"),
    Unlock("M7 11V8a5 5 0 0 1 9.6-2M6 11h12v9H6zM12 15v2"),
    Fingerprint(
        "M7 18c1-2 1.5-4 1.5-6a3.5 3.5 0 0 1 7 0c0 1.2-.1 2.3-.3 3.4M12 12c0 3-.6 5.6-1.8 8" +
            "M4.5 14c.3-.8.5-1.6.5-2.5a7 7 0 0 1 14 0c0 2-.2 3.9-.7 5.7M14.8 19.5c.3-.8.5-1.6.6-2.5",
    ),
    EyeOff(
        "M3 3l18 18M10.6 6.1A9.8 9.8 0 0 1 12 6c6 0 9.5 6 9.5 6a16 16 0 0 1-2.7 3.4" +
            "M6.4 7.6A16 16 0 0 0 2.5 12s3.5 6 9.5 6a9.6 9.6 0 0 0 4.2-1M10 10a3 3 0 0 0 4 4",
    ),
    Warn("M12 4l9 16H3zM12 10v4M12 17h.01"),
    Trash("M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12"),
    Undo("M9 14L4 9l5-5M4 9h10a6 6 0 0 1 0 12h-3"),
    UndoMap("M9 7L4 12l5 5M4 12h11a5 5 0 0 1 0 10h-2"),
    Cloud("M7 18h10a4 4 0 0 0 .5-8A6 6 0 0 0 6 11a3.5 3.5 0 0 0 1 7z"),
    Drive("M8 4h8l5 9-4 7H7l-4-7zM8 4l9 16M16 4L7 20M3 13h18"),
    File("M6 3h8l4 4v14H6zM14 3v4h4"),
    Folder("M3 7h7l2 2h9v10H3z"),
    Image("M4 5h16v14H4zM4 16l5-5 4 4 3-3 4 4"),
    Grid("M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z"),

    /** Gallery `격자` (GAL, board `scr_gallery.py` I_GRID9): three columns of tall 3:4 tiles, two rows. */
    GridTall("M3.5 3.5h4.5v7.5H3.5zM9.75 3.5h4.5v7.5h-4.5zM16 3.5h4.5v7.5H16zM3.5 13h4.5v7.5H3.5zM9.75 13h4.5v7.5h-4.5zM16 13h4.5v7.5H16z"),
    List("M4 7h16M4 12h16M4 17h16"),
    Rotate("M4 12a8 8 0 1 0 3-6.2M4 4v4h4"),
    Flip("M12 3v18M8 7l-4 5 4 5M16 7l4 5-4 5"),
    Reset("M4 4v6h6M4.5 15a8 8 0 1 0 2-8.5L4 9"),
    Pin("M12 21s-6-5.5-6-10a6 6 0 0 1 12 0c0 4.5-6 10-6 10z"),
    Move("M12 3v18M3 12h18M8 7l4-4 4 4M8 17l4 4 4-4M7 8l-4 4 4 4M17 8l4 4-4 4"),
    Area("M5 7l7-3 7 4-1 9-8 3-6-6z"),
    Route("M5 19c4 0 3-7 7-7s3-7 7-7"),
    Label("M5 6h14M12 6v13"),
    Stamp("M9 4h6l-1 7h4v4H6v-4h4zM5 20h14"),
    Brush("M4 20c3 0 4-2 4-4l9-9 3 3-9 9c-2 0-4 1-4 4z"),
    Fill("M5 12l7-7 7 7-7 7zM19 15c0 2 1 3 1 4a1 1 0 0 1-2 0c0-1 1-2 1-4"),
    Select("M4 4h6M14 4h6M4 20h6M14 20h6M4 4v6M4 14v6M20 4v6M20 14v6"),
    Node("M4 6h6v5H4zM14 13h6v5h-6zM10 8.5h2v7h2"),
    Line("M4 18L20 6"),
    Dice("M4 4h16v16H4zM9 9h.01M15 15h.01M15 9h.01M9 15h.01"),
    Camera("M4 8h4l2-3h4l2 3h4v11H4zM12 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"),
    Wave("M3 12c3-4 6 4 9 0s6 4 9 0"),
    Book("M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3zM5 17a3 3 0 0 1 3-3h11"),
    Merge("M12 4v16M3 12h5M5.5 9.5L8 12l-2.5 2.5M21 12h-5M18.5 9.5L16 12l2.5 2.5"),
    Split("M12 4v16M4 12h5M6.5 9.5L4 12l2.5 2.5M20 12h-5M17.5 9.5L20 12l-2.5 2.5"),
    Paper("M5 5h14v10h-8l-4 3.5V15H5z"),
    Props("M4 7h16M4 12h16M4 17h10"),
    StepLock("M6 11h12v9H6zM8 11V8a4 4 0 0 1 8 0v3"),
    StepOpen("M6 11h12v9H6zM8 11V8a4 4 0 0 1 7.5-2"),
    Exclaim("M12 5v9M12 18h.01"),

    /** `이름 바꾸기` (서재 ⋯ menu, LIB-02). */
    Pencil("M4 20h4l10-10-4-4L4 16z"),

    /** `수위 등급` (서재 ⋯ menu, LIB-02). */
    Shield("M12 3l8 4v5c0 4.5-3.4 8.2-8 9-4.6-.8-8-4.5-8-9V7z"),

    /** `백업 · 내보내기`: download tray (서재 ⋯ menu, LIB-02). */
    Download("M12 4v11M7 10l5 5 5-5M5 20h14"),

    /** Zoom out `−` (canvas zoom column, CNV-01); the pair of [Plus]. */
    Minus("M5 12h14"),

    // ---- DS-71 (0.2.0): ids added by the r2 icon set. Existing ids are kept; seven of them took the r2 path above.

    /** `보내기` (input bar send, A1). */
    Send("M12 19V6M6.5 11.5L12 6l5.5 5.5"),

    /** `다음 씬` (B2). */
    NextScene("M4 6h12M4 10h12M4 14h7M17 13v7M13.5 16.5h7"),

    /** `고치기` pencil (script block left swipe, caption panels). */
    Edit("M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4"),

    /** `이 뒤에` (script block left swipe). */
    InsertAfter("M5 6h14M5 11h14M12 14v6M9 17h6"),

    /** `종류` (script block left swipe). */
    Kind("M4 7h10M4 12h16M4 17h7M17 4l3 3-3 3"),

    /** `씬 나누기` (script block left swipe). */
    SceneCut("M4 5h16M4 19h16M4 12h3M10.5 12h3M17 12h3"),

    /** `복사` (block selection tools). */
    Copy("M8 8h11v11H8zM5 16V5h11"),

    /** `잘라내기` (block selection tools). */
    Cut("M6 7a2.5 2.5 0 1 0 0 .01M6 17a2.5 2.5 0 1 0 0 .01M8 8.5L19 18M8 15.5L19 6"),

    /** `옮기기` (block selection tools); [Move] stays the map pan tool. */
    MoveBlock("M12 4v16M8 8l4-4 4 4M8 16l4 4 4-4"),

    /** `관계도` (cast list view switch). */
    Graph(
        "M8.5 6.5a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0zM20.5 9a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0z" +
            "M12.5 18a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0zM8.4 7.1l7.2 1.4M6.6 8.9l2.6 6.8M16.4 10.8l-4.6 5.4",
    ),

    /** `이전` in bottom rows and card controls; same shape as [Back], which sits at a head's left (D31). */
    Prev("M15 5l-7 7 7 7"),

    /** `이전 카드` (롤로덱스). */
    Up("M5 15l7-7 7 7"),

    /** `다음 카드` (롤로덱스). */
    Down("M5 9l7 7 7-7"),

    /** `맨 앞` (gallery reorder). */
    ToTop("M5 4.5h14M12 20V9M7 14l5-5 5 5"),

    /** `맨 뒤` (gallery reorder). */
    ToBottom("M5 19.5h14M12 4v11M7 10l5 5 5-5"),

    /** `여러 장 고르기`. */
    SelectBox("M5 5h14v14H5zM8.5 12l2.5 2.5 4.5-5"),

    /** `설명` (gallery viewer, on and off). */
    Caption("M5 6h14M5 11h14M5 16h9"),

    /** `보기` (full screen). */
    Expand("M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"),

    /** Card `뒤집기`; not the crop mirror, which is [Flip]. */
    CardFlip("M4 12a8 8 0 0 1 14-5.3M20 12a8 8 0 0 1-14 5.3M18 3v4h-4M6 21v-4h4"),

    /** `다시 펼치기` (타로 펼침). */
    Redeal("M4 12a8 8 0 1 0 2.4-5.7M4 4.5v4h4"),

    /** `열기` (card page). */
    OpenOut("M14 5h5v5M19 5l-8 8M10 6H6a1 1 0 0 0-1 1v11a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1v-4"),

    /** `펼치기` (원고 더미). */
    Fan("M12 20L5 7M12 20l7-13M12 20V5"),

    /** `모으기` (원고 더미). */
    Gather("M5 9l7-4 7 4-7 4zM5 13l7 4 7-4M5 17l7 4 7-4"),

    /** `들어 보기` (원고 더미). */
    Lift("M4 17h16M4 20.5h16M7 6.5h10v6H7zM9.5 4L12 2l2.5 2"),

    /** `내려놓기` (원고 더미). */
    Drop("M4 17h16M4 20.5h16M7 8h10v5.5H7zM9.5 3L12 5l2.5-2"),

    /** `패널 열기` on the closed side panel's rail (r3 `L*Shut` / `Two*Shut`). */
    Panel("M4 5h16v14H4zM9 5v14"),

    /** (0.4.0) `설정` gear icon (replaces Props in library top bar). */
    Settings(
        "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" +
            "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33" +
            "a1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51a1.65 1.65 0 0 0-1.82.33l-.06.06" +
            "a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09" +
            "a1.65 1.65 0 0 0 1.51-1a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06" +
            "a1.65 1.65 0 0 0 1.82.33h.08a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51" +
            "a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v.08" +
            "a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
    ),

    /** (0.4.0) Clock icon for time tab, timeline, and estimated play time. */
    Clock("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2"),

    /** (0.4.0) Character / speaker silhouette icon. */
    User("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM5 20a7 7 0 0 1 14 0"),

    /** (0.4.0) Folded map icon for world/map shortcuts. */
    Map("M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2zM9 4v14M15 6v14"),

    /** (0.4.0) Scene outline / jump list icon. */
    Outline("M4 6h3M10 6h10M4 12h3M10 12h10M4 18h3M10 18h10"),

    /** (0.4.0) Sliders icon for motion/resistance/tuning settings. */
    Sliders("M4 7h16M4 17h16M9 4v6M15 14v6"),

    /** (0.4.0) Sparkles / visual cue / direction icon. */
    Spark("M12 3l2.2 6.8L21 12l-6.8 2.2L12 21l-2.2-6.8L3 12l6.8-2.2z"),
    ;

    companion object {
        /**
         * DS-71: the r2 icon-set key (`send`, `close`, `mirror` …) → its id. Every one of the 51 keys maps to one
         * id; null for an unknown key.
         */
        fun forKey(key: String): IconId? = R2_KEYS[key]

        /** The 51 r2 keys in the set's order. */
        val r2Keys: List<String> get() = R2_KEYS.keys.toList()

        private val R2_KEYS: Map<String, IconId> by lazy {
            linkedMapOf(
                "send" to Send, "check" to Check, "search" to Search, "layers" to Layers, "close" to X,
                "split" to Split, "merge" to Merge, "next_scene" to NextScene,
                "edit" to Edit, "insert_after" to InsertAfter, "kind" to Kind, "scene_cut" to SceneCut, "trash" to Trash,
                "note_ref" to Paper, "props" to Props, "copy" to Copy, "cut" to Cut, "move" to MoveBlock, "undo" to Undo,
                "plus" to Plus, "list" to List, "graph" to Graph, "back" to Back, "prev" to Prev, "next" to Next,
                "up" to Up, "down" to Down, "to_top" to ToTop, "to_bottom" to ToBottom, "select_box" to SelectBox,
                "more" to More,
                "caption" to Caption, "expand" to Expand, "image" to Image, "file" to File, "rotate" to Rotate,
                "mirror" to Flip, "reset" to Reset, "lock" to Lock, "fingerprint" to Fingerprint,
                "mic" to Mic, "write" to Text, "pause" to Pause,
                "dice" to Dice,
                "flip" to CardFlip, "redeal" to Redeal, "open_out" to OpenOut, "fan" to Fan, "gather" to Gather,
                "lift" to Lift, "drop" to Drop,
            )
        }
    }
}

/** [ImageVector]s for [IconId]; built once and cached. */
object CivilightIcons {
    private val cache = HashMap<IconId, ImageVector>()

    fun get(id: IconId): ImageVector = synchronized(cache) {
        cache.getOrPut(id) { build(id.name, id.pathData, ICON_STROKE_UNITS) }
    }

    /** The checkbox tick: `Check` with stroke 3 grid units (DS-32). */
    val checkBold: ImageVector by lazy { build("CheckBold", IconId.Check.pathData, 3f) }

    private fun build(name: String, d: String, stroke: Float): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
}

val IconId.vector: ImageVector get() = CivilightIcons.get(this)
