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
    X("M6 6l12 12M18 6L6 18"),
    Check("M5 12l4 4 10-10"),
    Grip("M9 6h.01M15 6h.01M9 12h.01M15 12h.01M9 18h.01M15 18h.01"),
    Filter("M4 6h16M7 12h10M10 18h4"),
    Search("M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4-4"),
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
    File("M7 3h7l5 5v13H7zM14 3v5h5"),
    Folder("M3 7h7l2 2h9v10H3z"),
    Image("M4 5h16v14H4zM4 16l5-5 4 4 3-3 4 4"),
    Grid("M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z"),
    List("M4 6h16M4 12h16M4 18h16"),
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
    Merge("M6 6l6 6-6 6M18 6l-6 6 6 6"),
    Split("M12 4v16M6 8l-3 4 3 4M18 8l3 4-3 4"),
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
