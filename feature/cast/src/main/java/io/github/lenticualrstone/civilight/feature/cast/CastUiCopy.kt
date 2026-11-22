package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.model.cast.EntityKind

/**
 * UI copy of the cast screens and sheets that the `domain:cast` builders do not already produce
 * (DECISIONS §3). Verbatim from `docs/spec/30-cast.md` and the boards; terse fragments joined with
 * ` · `. Strings the domain returns (`CastList`, `Profile`, `TimedValues`, `RelationViews`,
 * `FieldSettings`) are shown as they come and are not repeated here.
 *
 * Constants named `BTN_…` are button labels (the copy lint checks them as buttons). `TODO_COPY_…`
 * values are placeholders for copy no source gives; they are listed under Requests in
 * `docs/status/feature-cast.md`.
 */
object CastUiCopy {
    // --- Shared ---
    const val BACK = "뒤로"
    const val BTN_CLOSE = "닫기"
    const val BTN_INSERT = "넣기"
    const val MIC = "라이프 레코더"
    const val NO_RESULTS = "결과 없음"
    const val HIDDEN_TAG = "숨김"

    // --- 인물 목록 (CST-01, CST-02, CST-05) ---
    const val NEW_CHARACTER = "인물 추가"

    /** The first row's segmented control `목록 | 관계도` (`Cast`). */
    val VIEW_MODES = listOf("목록", "관계도")
    const val NAME = "이름"

    // --- 인물 기록 (CST-07 … CST-12) ---
    val TABS = listOf("개요", "말투", "관계", "게임 텍스트", "비밀")
    const val CURRENT_VALUE = "지금 값"
    const val UNKNOWN_TOGGLE = "알 수 없음"
    const val EMPTY_VALUE = "비어 있음"
    const val DISPLAY_NAME = "표시 이름"
    const val ONE_LINER = "한 줄 소개"
    const val TAGS = "태그"
    const val BTN_CORRECT = "이 값 고치기"
    const val BTN_ADD_POINT = "시점 추가"
    const val HISTORY_OPEN = "변화 기록"
    const val SPEECH = "말투"

    /** `1-3부터`: the start of the record in effect beside a value (`Profile`, `RTEditTwo`). */
    fun since(point: String): String = "${point}부터"

    /** A month as a plain number (`3월`), to tell it from a world-calendar month name. */
    fun monthNumber(month: Int): String = "${month}월"

    /** `빈 항목 3 · 보기` (CST-08, TC-F-13); also with 0 so the fixed rows stay reachable. */
    fun emptyRow(count: Int): String = "빈 항목 $count · 보기"

    /** Status words of a relation step after the lens, as the `Relations` board writes them (`◆ 지금 시점보다 뒤`). */
    fun stepStatus(label: String): String = "${CastCopy.CHANGE_MARK} $label"

    /** No source gives copy for a long text holding `@` links or paren runs that cannot be edited as plain text yet. */
    const val TODO_COPY_CAST_LONG_TEXT_RICH = "TODO_COPY_CAST_LONG_TEXT_RICH"

    // --- 항목 값 넣기 (CST-09, CST-20) ---
    const val OPTIONS = "보기"
    const val FREE_TEXT = "직접 쓰기"
    const val UNIT = "단위"
    const val MULTIPLE = "여러 개"
    const val YEAR = "연도"
    const val MONTH = "월"
    const val DAY = "일"
    const val BTN_REMOVE = "빼기"

    /** `세력 고르기` (link picker title and add chip). */
    fun pickTitle(kind: EntityKind): String = "${kind.label} 고르기"

    // --- 항목 넣기 (CST-21, CST-22) ---
    const val ADD_FIELD_TITLE = "항목 넣기"
    const val EXPORT_NAME = "내보내기 이름"
    const val TYPE = "종류"
    const val NUMBER_NOTE = "값은 바꿔 계산 안 함"
    const val PLACES = "보이는 곳"
    const val PLACE_HEADER = "머리"
    const val PLACE_LIST = "목록"
    const val PLACE_FILTER = "필터"
    const val SECRET = "비밀 항목"
    const val SECRET_SUB = "비밀 탭에만 보임"
    const val LINK_TARGET = "연결"
    const val DATE_PRECISION = "날짜"
    const val CALC_SOURCE = "계산"
    const val WHERE = "어디에"
    const val SCOPE_ALL = "모든 인물에"
    const val SCOPE_ALL_SUB = "인물 항목에 추가 · 다른 인물은 빈 칸"
    const val SCOPE_ONE = "이 인물만"

    /** `인물 · 모든 인물에` (`RCFieldAdd` sub), the same pattern for the other kinds. */
    fun addFieldSub(kind: EntityKind): String = "${kind.label} · 모든 ${kind.label}에"

    /** `리엔 · 인물 기록 007` (`RCProfileAdd` sub). */
    fun recordSub(name: String, recordNo: Int): String = "$name · 인물 기록 ${recordNo.toString().padStart(3, '0')}"

    /** `리엔에게만`. */
    fun onlyFor(name: String): String = "${name}에게만"

    /** `출생에서` (calc source chip, as the settings rows write it). */
    fun fromSource(name: String): String = "${name}에서"

    // --- 프로젝트 설정 › 인물 항목 (CST-19) ---
    const val REORDER = "순서 바꾸기"
    const val ADD_FIELD_ROW = "항목"
}
