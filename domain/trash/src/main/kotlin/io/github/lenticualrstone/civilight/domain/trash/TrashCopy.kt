package io.github.lenticualrstone.civilight.domain.trash

/**
 * UI copy of rule 20 and the trash (CST-40…48, LIB-38…40), as terse fragments joined with ` · `.
 * Strings marked `ED` in the status file are defaults where the sources give no copy.
 */
object TrashCopy {
    const val SEP = " · "
    const val UNTITLED = "미정"
    const val UNDO = "되돌리기"
    const val RESTORE = "되살리기"
    const val PURGE = "영구 삭제"
    const val EMPTY_TRASH = "비우기"
    const val TRASH = "휴지통"
    const val CHECK_CENTER = "점검 센터"
    const val TO_TRASH = "휴지통으로"
    const val CANCEL = "취소"
    const val KEEP = "이대로 두기"
    const val AUTO_PURGE_HEAD = "30일 뒤 자동 삭제"
    const val SHEET_OWNED = "같이 휴지통으로"
    const val SHEET_FOOTNOTE = "휴지통 · 30일 · 되살리면 다시 이어짐"
    const val SHEET_CHECK_ROW = "점검 센터"
    const val SHEET_CHECK_SUB = "◇ 1줄로 모임 · 한꺼번에 바꾸기"
    const val SHEET_DUPLICATE = "중복이면"
    const val SHEET_DUPLICATE_SUB = "지우지 않고 합치기"
    const val RESTORE_NEEDS_AUTH = "되살리기는 지문 확인"
    const val SCENE_TOO = "씬도 되살리기"
    const val OTHER_SCENE_END = "다른 씬 끝에"
    const val MAP_MISSING = "◇ 지도 없음"
    const val RESTORE_MAP = "지도 되살리기"
    const val RECORDER = "라이프 레코더"
    const val HIDDEN_PROJECT = "숨긴 프로젝트"
    const val HIDDEN_ITEM = "숨긴 항목"

    /** (0.2.0, CST-75) A trashed tag whose name a live tag has: `되살리기` refused (sub line and toast). */
    const val TAG_NAME_TAKEN = "◇ 같은 이름 태그 있음"

    /** Gallery (55): the area word, an untitled image, and the hidden count form of the order line. */
    const val GALLERY = "갤러리"
    const val UNTITLED_IMAGE = "제목 없음"
    const val HIDDEN_TAG = "숨김"

    /** Copy id of a reference whose target is absent and has no name tag (§3 case 5; no source copy yet). */
    const val MISSING_REFERENCE_COPY_ID = "TODO_COPY_DM_01"

    /** Korean kind word of an item (`인물`, `장소` …). */
    fun kindWord(kind: ItemKind): String = when (kind) {
        ItemKind.CHARACTER -> "인물"
        ItemKind.PLACE -> "장소"
        ItemKind.TERM -> "용어"
        ItemKind.FACTION -> "세력"
        ItemKind.FIELD_DEF -> "항목"
        ItemKind.CHANGE -> "변화"
        ItemKind.MAP -> "지도"
        ItemKind.EVENT -> "사건"
        ItemKind.PAGE -> "페이지"
        ItemKind.CANVAS -> "캔버스"
        ItemKind.UNIT -> "씬"
        ItemKind.BLOCK -> "블록"
        ItemKind.GALLERY_ITEM -> "이미지"
    }

    /** `숨긴 인물` / `숨긴 장소` … (trash rows and lists of a hidden item). */
    fun hiddenKind(kind: ItemKind): String = "숨긴 ${kindWord(kind)}"

    /** `지운 인물 · 노아`. */
    fun deletedLabel(kind: ItemKind, name: String): String = "지운 ${kindWord(kind)}$SEP$name"

    /** `영구 삭제 · 노아`. */
    fun purgedLabel(name: String): String = "$PURGE$SEP$name"

    /** `영구 삭제 · 숨긴 항목` (CST-44, LIB T-13). */
    val purgedHiddenLabel: String get() = "$PURGE$SEP$HIDDEN_ITEM"

    /** `<d>일 남음`. */
    fun daysLeft(days: Int): String = "${days.coerceAtLeast(0)}일 남음"

    /** Joins the non-blank parts with ` · `. */
    fun join(vararg parts: String?): String = parts.filterNot { it.isNullOrBlank() }.joinToString(SEP)

    fun join(parts: List<String?>): String = parts.filterNot { it.isNullOrBlank() }.joinToString(SEP)
}
