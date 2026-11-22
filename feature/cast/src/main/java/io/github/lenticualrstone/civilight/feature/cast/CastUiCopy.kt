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

    /** Search footer while hidden characters are locked (CST-27, rule 19 §6). */
    const val HIDDEN_EXCLUDED = "숨긴 항목 제외"

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

    /** CST-22: an `이 인물만` field becomes a project field (`모든 인물에 넣기`). */
    const val BTN_PROMOTE = "모든 인물에 넣기"

    /** `인물 · 모든 인물에` (`RCFieldAdd` sub), the same pattern for the other kinds. */
    fun addFieldSub(kind: EntityKind): String = "${kind.label} · 모든 ${kind.label}에"

    /** `리엔 · 인물 기록 007` (`RCProfileAdd` sub). */
    fun recordSub(name: String, recordNo: Int): String = "$name · 인물 기록 ${recordNo.toString().padStart(3, '0')}"

    /** `짧은 글 ›`: the chosen type on the record sheet's `종류` row (`RCProfileAdd`). */
    fun chosenType(label: String): String = "$label ›"

    /** `리엔에게만`. */
    fun onlyFor(name: String): String = "${name}에게만"

    /** `출생에서` (calc source chip, as the settings rows write it). */
    fun fromSource(name: String): String = "${name}에서"

    // --- 항목 정의 바꾸기 (CST-23) ---
    /** `숨기기` of a field definition (values kept, CST-23 table). */
    const val HIDE = "숨기기"

    /**
     * Applies a previewed change (field type, relation kind switch). No source names this button;
     * `바꾸기` is the verb of the spec's `관계 종류 · 바꾸기 ›` row [PROPOSED reuse].
     */
    const val BTN_CHANGE = "바꾸기"

    /** A definition change as the spec writes it (`키 → 신장`, `짧은 글 → 숫자`). */
    fun change(from: String, to: String): String = "$from → $to"

    // --- 관계 종류 (CST-32, `RCRelTypes`) ---
    const val KIND_TITLE = "관계 종류"
    const val KIND_NEW = "새 종류"
    const val KIND_BETWEEN = "사이"
    const val KIND_MIND = "마음"
    const val KIND_SYMMETRIC = "대칭"
    const val KIND_PAIR_NAME = "짝 이름"

    /** `쓰임 1`. */
    fun usage(count: Int): String = "쓰임 $count"

    // --- 전체 관계도 (CST-33, CST-34; `CastMap`, `CastMapC`) ---
    const val ALL = "전체"
    const val MAP_SUB = "관계도로 보기"
    const val SELECTED_RELATION = "고른 관계"

    /** The cover sheet's short `지금 시점보다 뒤` (`CastMapC`: `· 뒤`). */
    const val AFTER_LENS_SHORT = "뒤"

    /** The line tool (the canvas tool word `선`): tap one character, then another. */
    const val LINE_TOOL = "선"

    /** Undo label of a node drag (the canvas hint `노드는 끌어서 이동`). */
    const val UNDO_MOVE = "노드는 끌어서 이동"

    // --- 시작 묶음 (CST-24) ---
    /** The bundle picker title and its button (`RCFields` row `시작 묶음 · …` / `항목 더하기 ›`). */
    const val BUNDLE_TITLE = "시작 묶음"
    const val BUNDLE_ACTION = "항목 더하기"

    // --- 프로젝트 설정 › 인물 항목 (CST-19) ---
    const val REORDER = "순서 바꾸기"
    const val ADD_FIELD_ROW = "항목"

    // --- 관계 만들기 · 고치기 (CST-29) ---
    /** `무엇으로 적을지` (the slot chips; `CanvasCLabel` line-label sheet). */
    const val WHAT_TO_WRITE = "무엇으로 적을지"

    /** Partner picker title (rule 13 §7 `상대 고르기`). */
    const val PICK_PARTNER = "상대 고르기"
    const val DESCRIPTION_OPTIONAL = "설명 (선택)"
    const val BTN_MAKE_RELATION = "관계로 만들기"

    // --- 지우기 · 되살리기 (CST-40, CST-41, CST-43) ---
    const val BTN_DELETE = "지우기"
    const val BTN_RESTORE = "되살리기"
    const val BTN_KEEP = "이대로 두기"

    // --- 게임 텍스트 · 말투 (CST-15, CST-16, CST-17) ---
    /** Label of the record's `+` on `게임 텍스트` (a voice line at the end, CST-16) and the new line's sheet title [PROPOSED]. */
    const val NEW_VOICE_LINE = "보이스 대사 추가"
    const val ARCHIVE = "아카이브 문서"

    /** The archive document's unlock point (planning `열리는 시점`). */
    const val UNLOCK = "열리는 시점"

    /**
     * Title of the `@` picker of cast text (voice lines, 말투, 비밀, archive documents). No source names it,
     * so it is the nearest existing copy, the `연결` of a link field ([LINK_TARGET]).
     */
    const val LINK_PICKER_TITLE = LINK_TARGET

    // --- 항목 필터 · 정렬 (CST-03) ---
    /** The header button and its sheet (the feature's own name; §6 Q13 default placement). */
    const val FILTER_TITLE = "항목 필터 · 정렬"

    /** Section of the `<항목>순` orders in that sheet [PROPOSED]. */
    const val SORT_TITLE = "정렬"

    // --- 이미지 (CST-13) ---
    const val PORTRAIT = "인물 초상"
    const val BACKGROUND = "인물 배경"
    const val PICK_IMAGE = "이미지 고르기"
    const val OWN_IMAGE = "내 이미지"
    const val HOLO = "홀로"

    /** The 8 built-in presets (CNV-26), in the order of `CharacterImages` preset ids. */
    val PRESET_NAMES = listOf("잿빛 새벽", "등불", "심해", "안개 숲", "사막 유적", "성운", "붉은 성채", "설원")

    /** A `숫자` range on its active filter chip: `150 ~ 170`, or one open bound (`150 ~`). */
    fun range(min: String, max: String): String = "${min.trim()} ~ ${max.trim()}".trim()

    /** `+` beside a section title (`아카이브 문서 · 2`: a new document). */
    const val ADD_MARK = "+"

    /** The computed fixed fields' header labels (`Profile.header`: `첫 등장`, `보이스`). */
    const val FIRST_APPEARANCE = "첫 등장"
    const val VOICE = "보이스"

    /** The record `⋯` label: `리엔 메뉴`. */
    fun menuLabel(name: String): String = "$name 메뉴"

    /** The separator of joined fragments. */
    const val SEP = " · "

    // --- 갤러리 (GAL-01, GAL-02; 55-gallery.md) ---
    /** `갤러리 · 8`: the header of the profile section, N = live visible images. */
    fun gallerySection(count: Int): String = "갤러리 · $count"

    /** `갤러리 8`: the chip on a card of the card view (GAL-02). */
    fun galleryChip(count: Int): String = "갤러리 $count"

    /** `전체 보기 ›`: opens the character's gallery screen (GAL-04). */
    const val BTN_GALLERY_ALL = "전체 보기 ›"

    /** The outlined empty card of the section (GAL-01, same look as the empty gallery). */
    const val GALLERY_EMPTY_ADD = "이미지 넣기"

    // --- 숨기기 (CST-39) ---
    /** Record `⋯` row (CST-39) and its undo label. */
    const val BTN_HIDE = "숨기기"

    /** Record `⋯` row of a hidden character (CST-39 `숨김 풀기` [PROPOSED]). */
    const val BTN_UNHIDE = "숨김 풀기"

    // --- 합치기 (CST-45, CST-46; `RCMerge`) ---
    const val BTN_MERGE = "합치기"
    const val BTN_CANCEL = "취소"

    /** The term `첫 공개` comparison row (TC-20-27). */
    const val FIRST_REVEAL = "첫 공개"

    /** `같은 항목 | 6` (the engine counts equal fields of world items). */
    const val SAME_FIELDS = "같은 항목"

    /** `◇ 같은 이름 2 · 노아` (CST-46; the `domain:trash` notice, counted over visible characters while locked). */
    fun sameName(count: Int, name: String): String = "◇ 같은 이름 $count · $name"

    /** `옮길 참조 · 5`. */
    fun movedTitle(count: Int): String = "옮길 참조 · $count"

    /** `다른 칸 · 3`. */
    fun differingTitle(count: Int): String = "다른 칸 · $count"

    /** `빛돌 · 합쳐짐` (column head of the merged item). */
    fun mergedColumn(name: String): String = "$name · 합쳐짐"

    /** `오리지늄 등불 · 남음` (column head of the item that stays). */
    fun keptColumn(name: String): String = "$name · 남음"

    /** Preview of `@ 한꺼번에 바꾸기` (CST-42), after the speaker one `대사 14 · 노아 → 오웬` [PROPOSED]. */
    fun linkChange(count: Int, from: String, to: String): String = "@ $count · $from → $to"

    /** `빛돌 · ‘합침 → 오리지늄 등불’로 휴지통`. */
    fun mergedFootnote(merged: String, target: String): String = "$merged · ‘합침 → $target’로 휴지통"
}
