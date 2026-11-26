package io.github.lenticualrstone.civilight.designsystem.icon

/** DS-70 button classes: [A] icon only, [B] icon + short text, [C] text as now. */
enum class IconClass { A, B, C }

/**
 * One button of the DS-70 tables (0.2.0). [row] is the table row (`A1`, `B11` …); [text] is the visible label
 * (null for an a-class button); [iconKey] is the r2 key of DS-71 (null when no r2 icon is drawn, e.g. the
 * `고치기` send kept as text); [accessibilityName] is what TalkBack reads, which for b can be longer than the
 * visible text (`시점` → `시점 추가`). [toggle] marks a b button that also exposes its on / off state.
 *
 * Rows whose surfaces 0.2.0 hides and that have no r2 key (B25 `조건`, B26's terrain palette) are left out.
 */
enum class IconButtonSpec(
    val row: String,
    val cls: IconClass,
    val text: String?,
    val iconKey: String?,
    val accessibilityName: String,
    val toggle: Boolean = false,
) {
    // ---- a · icon only (A1 … A15)
    SEND("A1", IconClass.A, null, "send", IconButtonCopy.SEND),
    EDIT_SEND("A2", IconClass.C, IconButtonCopy.EDIT, null, IconButtonCopy.EDIT),
    SEARCH("A3", IconClass.A, null, "search", IconButtonCopy.SEARCH_COMMAND),
    DISPLAY_LAYERS("A4", IconClass.A, null, "layers", IconButtonCopy.DISPLAY_LAYERS),
    SEARCH_CLOSE("A5", IconClass.A, null, "close", IconButtonCopy.SEARCH_CLOSE),
    CLOSE("A6", IconClass.A, null, "close", IconButtonCopy.CLOSE),
    NEW_PROJECT_HEAD("A7", IconClass.A, null, "plus", IconButtonCopy.NEW_PROJECT),
    VIEWER_PREV("A8", IconClass.A, null, "prev", IconButtonCopy.PREV),
    VIEWER_NEXT("A8", IconClass.A, null, "next", IconButtonCopy.NEXT),
    CAPTION_EDIT("A9", IconClass.A, null, "edit", IconButtonCopy.EDIT),
    CARD_VIEW("A10", IconClass.A, null, "expand", IconButtonCopy.VIEW),
    CARD_CAPTION_EDIT("A10", IconClass.A, null, "edit", IconButtonCopy.EDIT),
    MAP_UNDO("A11", IconClass.A, null, "undo", IconButtonCopy.UNDO),
    MOTION_UNDO("A12", IconClass.A, null, "undo", IconButtonCopy.UNDO),
    HOME_BACK_TO_LIBRARY("A13", IconClass.A, null, "back", IconButtonCopy.TO_LIBRARY),
    ROLODEX_PREV("A14", IconClass.A, null, "up", IconButtonCopy.PREV_CARD),
    ROLODEX_NEXT("A14", IconClass.A, null, "down", IconButtonCopy.NEXT_CARD),
    ROLODEX_CLOSE("A15", IconClass.A, null, "close", IconButtonCopy.CLOSE),

    // ---- b · icon + text (B1 … B33)
    SPLIT("B1", IconClass.B, IconButtonCopy.SPLIT, "split", IconButtonCopy.SPLIT),
    MERGE("B1", IconClass.B, IconButtonCopy.MERGE, "merge", IconButtonCopy.MERGE),
    NEXT_SCENE("B2", IconClass.B, IconButtonCopy.NEXT_SCENE, "next_scene", IconButtonCopy.NEXT_SCENE_NAME),
    SWIPE_EDIT("B3", IconClass.B, IconButtonCopy.EDIT, "edit", IconButtonCopy.EDIT),
    SWIPE_INSERT_AFTER("B3", IconClass.B, IconButtonCopy.INSERT_AFTER, "insert_after", IconButtonCopy.INSERT_AFTER),
    SWIPE_KIND("B3", IconClass.B, IconButtonCopy.KIND, "kind", IconButtonCopy.KIND),
    SWIPE_SCENE_CUT("B3", IconClass.B, IconButtonCopy.SCENE_CUT, "scene_cut", IconButtonCopy.SCENE_CUT),
    SWIPE_DELETE("B3", IconClass.B, IconButtonCopy.DELETE, "trash", IconButtonCopy.DELETE),

    /** The `◆` glyph over `시간`; the glyph is drawn by the swipe row, not an r2 icon. */
    SWIPE_TIME("B4", IconClass.B, IconButtonCopy.TIME, null, IconButtonCopy.TIME),
    SWIPE_REFERENCE("B4", IconClass.B, IconButtonCopy.REFERENCE, "note_ref", IconButtonCopy.REFERENCE),
    SWIPE_PROPERTIES("B4", IconClass.B, IconButtonCopy.PROPERTIES, "props", IconButtonCopy.PROPERTIES),
    TOAST_UNDO("B5", IconClass.B, IconButtonCopy.UNDO, "undo", IconButtonCopy.UNDO),
    OLD_SWIPE_REFERENCE("B6", IconClass.B, IconButtonCopy.REFERENCE, "note_ref", IconButtonCopy.REFERENCE),
    OLD_SWIPE_PROPERTIES("B6", IconClass.B, IconButtonCopy.PROPERTIES, "props", IconButtonCopy.PROPERTIES),
    SELECT_COPY("B7", IconClass.B, IconButtonCopy.COPY, "copy", IconButtonCopy.COPY),
    SELECT_CUT("B7", IconClass.B, IconButtonCopy.CUT, "cut", IconButtonCopy.CUT),
    SELECT_MOVE("B7", IconClass.B, IconButtonCopy.MOVE, "move", IconButtonCopy.MOVE),
    SELECT_DELETE("B7", IconClass.B, IconButtonCopy.DELETE, "trash", IconButtonCopy.DELETE),

    /** The list head create row; the text follows the list (`새 장소` …), see `CreateCard.rowLabel`. */
    LIST_CREATE("B8", IconClass.B, IconButtonCopy.NEW_CHARACTER, "plus", IconButtonCopy.NEW_CHARACTER),
    NEW_SCENE("B9", IconClass.B, IconButtonCopy.NEW_SCENE, "plus", IconButtonCopy.NEW_SCENE),
    CAST_LIST("B10", IconClass.B, IconButtonCopy.LIST, "list", IconButtonCopy.LIST),
    CAST_GRAPH("B10", IconClass.B, IconButtonCopy.GRAPH, "graph", IconButtonCopy.GRAPH),
    ADD_POINT("B11", IconClass.B, IconButtonCopy.POINT, "plus", IconButtonCopy.ADD_POINT),
    ADD_FIELD("B12", IconClass.B, IconButtonCopy.FIELD, "plus", IconButtonCopy.ADD_FIELD),
    CAPTION_TOGGLE("B13", IconClass.B, IconButtonCopy.CAPTION, "caption", IconButtonCopy.CAPTION, toggle = true),
    MULTI_SELECT("B14", IconClass.B, IconButtonCopy.PICK, "select_box", IconButtonCopy.MULTI_SELECT),
    TO_TOP("B15", IconClass.B, IconButtonCopy.TO_TOP, "to_top", IconButtonCopy.TO_TOP),
    TO_BOTTOM("B15", IconClass.B, IconButtonCopy.TO_BOTTOM, "to_bottom", IconButtonCopy.TO_BOTTOM),
    ADD_PHOTO("B16", IconClass.B, IconButtonCopy.PHOTO, "image", IconButtonCopy.PHOTO),
    ADD_FILE("B16", IconClass.B, IconButtonCopy.FILE, "file", IconButtonCopy.FILE),
    RELOCK("B16", IconClass.B, IconButtonCopy.RELOCK, "lock", IconButtonCopy.RELOCK),
    PICK_GALLERY("B17", IconClass.B, IconButtonCopy.GALLERY, "image", IconButtonCopy.GALLERY),
    PICK_FILE("B17", IconClass.B, IconButtonCopy.FILE, "file", IconButtonCopy.FILE),
    TO_PICKED_CARD("B18", IconClass.B, IconButtonCopy.TO_PICKED_CARD, "prev", IconButtonCopy.TO_PICKED_CARD),
    PROFILE_GALLERY_ADD("B19", IconClass.B, IconButtonCopy.PUT_IN, "plus", IconButtonCopy.PUT_IN),
    CROP_ROTATE("B20", IconClass.B, IconButtonCopy.ROTATE, "rotate", IconButtonCopy.ROTATE),
    CROP_MIRROR("B20", IconClass.B, IconButtonCopy.MIRROR, "mirror", IconButtonCopy.MIRROR),
    CROP_RESET("B20", IconClass.B, IconButtonCopy.RESET, "reset", IconButtonCopy.RESET),
    SHOW_WITH_FINGERPRINT("B21", IconClass.B, IconButtonCopy.SHOW_WITH_FINGERPRINT, "fingerprint", IconButtonCopy.SHOW_WITH_FINGERPRINT),
    UNLOCK("B21", IconClass.B, IconButtonCopy.UNLOCK, "fingerprint", IconButtonCopy.UNLOCK),
    RECORD_VOICE("B22", IconClass.B, IconButtonCopy.RECORD_VOICE, "mic", IconButtonCopy.RECORD_VOICE),
    RECORD_TEXT("B22", IconClass.B, IconButtonCopy.RECORD_TEXT, "write", IconButtonCopy.RECORD_TEXT),
    MAP_OTHER_CANDIDATES("B23", IconClass.B, IconButtonCopy.OTHER_CANDIDATES, "dice", IconButtonCopy.OTHER_CANDIDATES),
    INBOX_RECORD_VOICE("B24", IconClass.B, IconButtonCopy.RECORD_VOICE, "mic", IconButtonCopy.RECORD_VOICE),
    INBOX_RECORD_TEXT("B24", IconClass.B, IconButtonCopy.RECORD_TEXT, "write", IconButtonCopy.RECORD_TEXT),
    MAPGEN_NEW_CANDIDATE("B25", IconClass.B, IconButtonCopy.NEW_CANDIDATE, "dice", IconButtonCopy.NEW_CANDIDATE),
    MAPGEN_RIVER_AGAIN("B25", IconClass.B, IconButtonCopy.RIVER_AGAIN, "dice", IconButtonCopy.RIVER_AGAIN),
    MAPGEN_RIVER_LOCK("B25", IconClass.B, IconButtonCopy.RIVER_LOCK, "lock", IconButtonCopy.RIVER_LOCK),
    MAPGEN_NEW_REGION("B25", IconClass.B, IconButtonCopy.NEW_REGION, "plus", IconButtonCopy.NEW_REGION),
    PILE_LIFT("B27", IconClass.B, IconButtonCopy.LIFT, "lift", IconButtonCopy.LIFT),
    PILE_DROP("B27", IconClass.B, IconButtonCopy.DROP, "drop", IconButtonCopy.DROP),
    PILE_GATHER("B27", IconClass.B, IconButtonCopy.GATHER, "gather", IconButtonCopy.GATHER),
    PILE_FAN("B27", IconClass.B, IconButtonCopy.FAN, "fan", IconButtonCopy.FAN),
    NEW_PROJECT_CELL("B28", IconClass.B, IconButtonCopy.NEW_PROJECT, "plus", IconButtonCopy.NEW_PROJECT),
    CARD_FLIP("B29", IconClass.B, IconButtonCopy.FLIP, "flip", IconButtonCopy.FLIP, toggle = true),
    CARD_OPEN("B30", IconClass.B, IconButtonCopy.OPEN, "open_out", IconButtonCopy.OPEN),
    TAROT_FLIP_ALL("B31", IconClass.B, IconButtonCopy.FLIP_ALL, "flip", IconButtonCopy.FLIP_ALL),
    TAROT_REDEAL("B31", IconClass.B, IconButtonCopy.REDEAL, "redeal", IconButtonCopy.REDEAL),
    ROLODEX_CREATE("B32", IconClass.B, IconButtonCopy.NEW_CHARACTER, "plus", IconButtonCopy.NEW_CHARACTER),
    WALL_NEW_CARD("B33", IconClass.B, IconButtonCopy.NEW_CARD, "plus", IconButtonCopy.NEW_CARD),
    ;

    /** The drawn icon, or null when this button shows no r2 icon. */
    val icon: IconId? get() = iconKey?.let { IconId.forKey(it) }
}

/** DS-70 pure rules (T-DS-46, T-DS-48). */
object IconButtons {
    /** Touch area of every icon button (DS-11, DS-70), dp. */
    const val TOUCH_DP: Int = 44

    /** Icon size of an a-class button, dp. */
    const val ICON_ONLY_DP: Int = 20

    /** Icon size range of a b-class button (before its label), dp. */
    val ICON_WITH_TEXT_DP: IntRange = 16..18

    /** Default b-class icon size, dp. */
    const val ICON_WITH_TEXT_DEFAULT_DP: Int = 18

    /** Gap between a b-class icon and its label, dp. */
    const val ICON_TEXT_GAP_DP: Int = 6

    fun classOf(button: IconButtonSpec): IconClass = button.cls

    fun nameOf(button: IconButtonSpec): String = button.accessibilityName

    /** Every button of [row] (`A8` has two). */
    fun row(row: String): List<IconButtonSpec> = IconButtonSpec.entries.filter { it.row == row }

    /** Drawn icon size for [cls] (C draws none). */
    fun iconSizeDp(cls: IconClass): Int? = when (cls) {
        IconClass.A -> ICON_ONLY_DP
        IconClass.B -> ICON_WITH_TEXT_DEFAULT_DP
        IconClass.C -> null
    }

    /** `합치기 · 꺼짐 · 종류 다름` (B1 disabled), or `나누기 · 꺼짐` without a reason. */
    fun offName(name: String, reason: String? = null): String =
        if (reason.isNullOrBlank()) "$name · ${IconButtonCopy.OFF}" else "$name · ${IconButtonCopy.OFF} · $reason"
}

/** Visible texts and accessibility names of the DS-70 tables. */
object IconButtonCopy {
    const val SEND = "보내기"
    const val EDIT = "고치기"
    const val SEARCH_COMMAND = "찾기 · 명령"
    const val DISPLAY_LAYERS = "연출 · 메모 표시"
    const val SEARCH_CLOSE = "찾기 닫기"
    const val CLOSE = "닫기"
    const val NEW_PROJECT = "새 프로젝트"
    const val PREV = "이전"
    const val NEXT = "다음"
    const val VIEW = "보기"
    const val UNDO = "되돌리기"
    const val TO_LIBRARY = "서재로"
    const val PREV_CARD = "이전 카드"
    const val NEXT_CARD = "다음 카드"
    const val SPLIT = "나누기"
    const val MERGE = "합치기"
    const val NEXT_SCENE = "다음 씬"
    const val NEXT_SCENE_NAME = "다음 씬 만들기"
    const val INSERT_AFTER = "이 뒤에"
    const val KIND = "종류"
    const val SCENE_CUT = "씬 나누기"
    const val DELETE = "지우기"
    const val TIME = "시간"
    const val REFERENCE = "참고"
    const val PROPERTIES = "속성"
    const val COPY = "복사"
    const val CUT = "잘라내기"
    const val MOVE = "옮기기"
    const val NEW_CHARACTER = "새 인물"
    const val NEW_SCENE = "새 씬"
    const val LIST = "목록"
    const val GRAPH = "관계도"
    const val POINT = "시점"
    const val ADD_POINT = "시점 더하기"
    const val FIELD = "항목"
    const val ADD_FIELD = "항목 넣기"
    const val CAPTION = "설명"
    const val PICK = "고르기"
    const val MULTI_SELECT = "여러 장 고르기"
    const val TO_TOP = "맨 앞"
    const val TO_BOTTOM = "맨 뒤"
    const val PHOTO = "사진"
    const val FILE = "파일"
    const val RELOCK = "다시 잠그기"
    const val GALLERY = "갤러리"
    const val TO_PICKED_CARD = "고른 카드로"
    const val PUT_IN = "넣기"
    const val ROTATE = "90° 회전"
    const val MIRROR = "좌우 반전"
    const val RESET = "처음대로"
    const val SHOW_WITH_FINGERPRINT = "지문으로 보기"
    const val UNLOCK = "잠금 풀기"
    const val RECORD_VOICE = "말로 기록"
    const val RECORD_TEXT = "글로 쓰기"
    const val OTHER_CANDIDATES = "다른 후보 3"
    const val NEW_CANDIDATE = "새 후보"
    const val RIVER_AGAIN = "강만 다시"
    const val RIVER_LOCK = "강 고정"
    const val NEW_REGION = "새 지역"
    const val LIFT = "들어 보기"
    const val DROP = "내려놓기"
    const val GATHER = "모으기"
    const val FAN = "펼치기"
    const val FLIP = "뒤집기"
    const val OPEN = "열기"
    const val FLIP_ALL = "모두 뒤집기"
    const val REDEAL = "다시 펼치기"
    const val NEW_CARD = "새 카드"

    /** The state word in a disabled button's accessibility name (`합치기 · 꺼짐 · 종류 다름`, DS-70 B1). */
    const val OFF = "꺼짐"

    val all: List<String> = listOf(
        SEND, EDIT, SEARCH_COMMAND, DISPLAY_LAYERS, SEARCH_CLOSE, CLOSE, NEW_PROJECT, PREV, NEXT, VIEW, UNDO, TO_LIBRARY,
        PREV_CARD, NEXT_CARD, SPLIT, MERGE, NEXT_SCENE, NEXT_SCENE_NAME, INSERT_AFTER, KIND, SCENE_CUT, DELETE, TIME,
        REFERENCE, PROPERTIES, COPY, CUT, MOVE, NEW_CHARACTER, NEW_SCENE, LIST, GRAPH, POINT, ADD_POINT, FIELD, ADD_FIELD,
        CAPTION, PICK, MULTI_SELECT, TO_TOP, TO_BOTTOM, PHOTO, FILE, RELOCK, GALLERY, TO_PICKED_CARD, PUT_IN, ROTATE,
        MIRROR, RESET, SHOW_WITH_FINGERPRINT, UNLOCK, RECORD_VOICE, RECORD_TEXT, OTHER_CANDIDATES, NEW_CANDIDATE,
        RIVER_AGAIN, RIVER_LOCK, NEW_REGION, LIFT, DROP, GATHER, FAN, FLIP, OPEN, FLIP_ALL, REDEAL, NEW_CARD, OFF,
    )
}
