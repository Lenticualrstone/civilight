package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import java.text.NumberFormat
import java.util.Locale

/**
 * Words of the seven flat 서재 views and the view picker (0.2.3, r3 `scr_v_lib.py`, `scr_v_preview.py`). Terse
 * fragments joined with ` · `. Board words the copy lint rejects are replaced: `수정` (table head, picker
 * fragment) reads `고친 때`, `밀어서 넘기기` reads `밀어서 바꾸기`.
 */
object FlatViewCopy {
    // View names (r3 `VIEWS`).
    const val TIMELINE = "타임라인"
    const val PAPER = "원고지"
    const val MAIL = "편지함"
    const val CELLS = "진행 칸"
    const val WALL = "표지 벽"
    const val POSTER = "포스터"
    const val TABLE = "표"

    // Picker fragments (r3 `FRAG`, LIB_EXTRA).
    const val FRAG_TIMELINE = "고친 때 · 띠"
    const val FRAG_PAPER = "글자 수 · 원고지 칸"
    const val FRAG_MAIL = "봉투 · 최근 자리"
    const val FRAG_CELLS = "회차 칸 · 진행"
    const val FRAG_WALL = "표지 · 벽돌 쌓기"
    const val FRAG_POSTER = "한 장씩 · 크게"
    const val FRAG_TABLE = "정렬 · 한눈에"
    const val FRAG_SHELF = "책등 · 꺼내서 열기"
    const val FRAG_FLOW = "표지 돌리기 · 가운데 크게"
    const val FRAG_DESK = "책상 위 원고 묶음"
    const val FRAG_HALL = "양쪽 서가 사이 걷기"
    const val FRAG_DRAWER = "서랍 속 카드 꺼내기"
    const val FRAG_STARS = "형식별 궤도 · 별 하나씩"
    const val FRAG_LIST = "이름 · 형식 · 고친 때"

    // Picker chrome.
    const val PICK_TITLE = "보기 고르기"
    const val PICK_KIND = "서재"
    const val NOW = "지금"
    const val GROUP_NEW = "새 보기"
    const val GROUP_NEW_SHORT = "새"
    const val GROUP_3D = "3D"
    const val GROUP_BASIC = "기본"
    const val APPLY = "이 보기로"
    const val CANCEL = "취소"
    const val PREV_VIEW = "이전 보기"
    const val NEXT_VIEW = "다음 보기"
    const val SWIPE_HINT = "밀어서 바꾸기"
    const val PREVIEW = "미리 보기"
    const val BOTH_SCREENS = "커버 · 내부 같이 바뀜"
    const val VIEW = "보기"

    // Forms (NewProjectCopy words) and their seals (board `SEALC`).
    const val FORM_SERIAL = "연재 장편"
    const val FORM_SHORT = "단편"
    const val FORM_SETTING = "설정만"
    const val SEAL_SERIAL = "장"
    const val SEAL_SHORT = "단"
    const val SEAL_SETTING = "설"

    // Views.
    const val OPEN = "열기"
    const val UNLOCK = "잠금 풀기"
    const val LOCKED_PROJECT = "잠긴 프로젝트"
    const val LOCKED = "잠김"
    const val PREV = "이전"
    const val NEXT = "다음"
    const val CHARS = "글자"
    const val CAST = "인물"
    const val RECENT = "최근 위치"
    const val RECENT_SHORT = "최근"
    const val NO_PROGRESS = "진행 없음"
    const val SETTING_ONLY = "설정만"
    const val CHAR_UNIT = "자"
    const val SCENE = "씬"
    const val NEW_PAPER = "새 원고지"
    const val NAME = "이름"
    const val FORM = "형식"
    const val PROGRESS = "진행"
    const val EDITED = "고친 때"
    const val SUM = "합계"
    const val ALL_PROJECTS = "모든 프로젝트"
    const val LEGEND_DONE = "씀"
    const val LEGEND_CURRENT = "쓰는 중"
    const val LEGEND_PLANNED = "계획"
    const val LEGEND_NOTE = "한 칸 · 에피소드 또는 씬 · 고른 줄 다시 누르면 열기"
    const val TIMELINE_GROUP = "고친 때 순 타임라인"
    const val MAILBOX_GROUP = "프로젝트 편지함"
    const val POSTERS = "프로젝트 포스터"
    const val ALL_POSTERS = "모든 포스터"
    const val PICKED = "고른 프로젝트"
    const val SORT = "정렬"

    // Timeline segments (r3 `SEGS`).
    val SEGMENTS = listOf("오늘", "어제", "이번 주", "지난 주", "이번 달", "이전")

    fun number(n: Int): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(n)

    /** `에피소드 6 / 20`. */
    fun progress(unit: String, done: Int, total: Int): String = "$unit $done / $total"

    /** `프로젝트 7`. */
    fun projects(n: Int): String = "프로젝트 $n"

    /** `3 / 8`. */
    fun count(index: Int, n: Int): String = "${index + 1} / $n"

    /** `48,210자`. */
    fun chars(n: Int): String = "${number(n)}$CHAR_UNIT"

    /** `인물 12`. */
    fun cast(n: Int): String = "$CAST $n"

    /** `진행 없음 · 설정만`. */
    val noProgress: String get() = "$NO_PROGRESS · $SETTING_ONLY"

    /** `보기 · 표지 벽` (the head button's accessibility name). */
    fun viewButton(label: String): String = "$VIEW · $label"

    /** `지금 · 표지 벽`. */
    fun now(label: String): String = "$NOW · $label"

    /** `정렬 · 글자` (no particle to agree with the column word). */
    fun sortBy(column: String): String = "$SORT · $column"
}
