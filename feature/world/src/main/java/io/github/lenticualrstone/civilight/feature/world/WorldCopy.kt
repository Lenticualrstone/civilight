package io.github.lenticualrstone.civilight.feature.world

import io.github.lenticualrstone.civilight.domain.world.item.WorldKind

/**
 * UI copy of the world area (DECISIONS §3), exactly as `docs/spec/40-world.md` and the boards give
 * it. Strings the domain already builds (`공개됨 · 최신 · 1-7`, `숨긴 장소 N`, `쓰인 곳 · 2 · 후보 1` …)
 * are not repeated here. `WorldCopyLintTest` runs the DS copy lint over every value.
 */
object WorldCopy {
    const val SEP = " · "

    // --- 세계 화면 틀 (WLD-01) ---
    const val WORLD_TITLE = "세계"
    const val TAB_PLACES = "장소"
    const val TAB_TERMS = "용어"
    const val TAB_FACTIONS = "세력"
    const val TAB_MAPS = "지도"
    const val TERM_HINT = "공개 시점 전 사용 → 점검 센터"
    const val SEARCH_WORLD = "장소 · 용어 · 세력 찾기"
    const val SEARCH_CARD = "명령 또는 검색"
    const val BACK = "뒤로"
    const val READ_ONLY = "읽기 전용"
    const val UNLOCK = "지문으로 보기"

    // --- 만들기 (WLD-11) ---
    const val CREATE_TERM = "용어 만들기"
    const val CREATE_PLACE = "장소 만들기"
    const val CREATE_FACTION = "세력 만들기"
    const val NAME = "이름"
    const val DESCRIPTION = "설명"
    const val BTN_CREATE = "만들기"

    // --- 카드 공통 (WLD-06, WLD-08 … WLD-10) ---
    const val SMALL_TERM = "세계 · 용어"
    const val SMALL_PLACE = "세계 · 장소"
    const val SMALL_FACTION = "세계 · 세력"
    const val EYEBROW_TERM = "용어"
    const val EYEBROW_FACTION = "세력"
    const val IMAGE_LABEL = "참고 이미지"
    const val ROW_REVEAL = "첫 공개"
    const val ROW_ALIASES = "별칭"
    const val ROW_RELATED = "관련"
    const val ROW_APPEARANCE = "등장"
    const val ROW_PARENT = "지역"
    const val ROW_ASSETS = "배경 에셋"
    const val ADD_FIELD = "+ 항목"
    const val EMPTY_VALUE = "—"
    const val HIDDEN_ITEM = "숨긴 항목"
    const val HIDDEN_SCENE = "숨긴 씬"
    const val TAG_INTENDED = "맞음"

    // --- 용어 카드 (WLD-08, WLD-19, WLD-25 notices) ---
    const val NOTICE_SEARCH_OFF = "글자 찾기 꺼짐"
    const val BTN_SEARCH_ON = "켜기"

    // --- 세력 카드 (WLD-10) ---
    const val LINKED_CHARACTERS = "이 항목으로 잇는 인물"

    /** Section of map regions and provinces linked to the faction: no source gives the title. */
    const val TODO_COPY_WORLD_FACTION_AREAS = "TODO_COPY_WORLD_FACTION_AREAS"

    /** Section of events at which a `◆` of the faction changes: no source gives the title. */
    const val TODO_COPY_WORLD_FACTION_EVENTS = "TODO_COPY_WORLD_FACTION_EVENTS"

    // --- ⋯ 메뉴 · 이름 · 설명 ---
    const val RENAME = "이름 바꾸기"
    const val BTN_RENAME = "바꾸기"
    const val EDIT_DESCRIPTION = "설명 고치기"
    const val BTN_EDIT = "고치기"

    // --- 첫 공개 (WLD-18) ---
    const val REVEAL_TITLE = "공개 시점 옮기기"
    const val REVEAL_LINKS = "쓰인 곳"
    const val REVEAL_FROM_SCRIPT = "원고에서 고르기"
    const val REVEAL_TO_UNIT = "에피소드 · 장으로"
    const val REVEAL_NOW = "◇ 첫 공개"

    // --- 지역 · 관련 ---
    const val BTN_CLEAR = "비우기"

    // --- 후보 (WLD-08 marker line `◇ 후보 · 끊김 · 잇기 · 아님`, WLD-22) ---
    const val BTN_LINK = "잇기"
    const val BTN_REJECT = "아님"

    /** Toast after `잇기` on a card candidate (WLD-22 gives `되돌리기` but no text). */
    const val TODO_COPY_WORLD_LINKED_TOAST = "TODO_COPY_WORLD_LINKED_TOAST"

    /** Toast after `아님` on a card candidate (WLD-22 gives `되돌리기` but no text). */
    const val TODO_COPY_WORLD_REJECTED_TOAST = "TODO_COPY_WORLD_REJECTED_TOAST"

    // --- 모두 잇기 · 모두 아님 (WLD-23, board `RTLinkAll`), 맞음 (WLD-24) ---
    const val BULK_LINK_ALL = "모두 잇기"
    const val BULK_REJECT_ALL = "모두 아님"

    /** `점검 중`: the first full scan is running (WLD-T72, check center wording). */
    const val SCANNING = "점검 중"

    /** The bulk sheet has no candidate left (opened from another area, or all decided meanwhile): no source gives the line. */
    const val TODO_COPY_WORLD_NO_CANDIDATES = "TODO_COPY_WORLD_NO_CANDIDATES"

    /** Toasts after `넣기` in the bulk sheet (WLD-23 gives `되돌리기` only). */
    const val TODO_COPY_WORLD_BULK_LINKED_TOAST = "TODO_COPY_WORLD_BULK_LINKED_TOAST"
    const val TODO_COPY_WORLD_BULK_REJECTED_TOAST = "TODO_COPY_WORLD_BULK_REJECTED_TOAST"

    /** Toast after `맞음` on a card `▲` row (WLD-24 gives the undo, not the text). */
    const val TODO_COPY_WORLD_INTENDED_TOAST = "TODO_COPY_WORLD_INTENDED_TOAST"

    // --- `@` 연결 in card text (WLD-17) ---
    /** The keyboard tool (`EditKeyboard`: `@`). */
    const val LINK_TOOL = "@"

    /** Title of the `@` picker: no board names it (feature:script has the same request). */
    const val TODO_COPY_WORLD_LINK_PICKER_TITLE = "TODO_COPY_WORLD_LINK_PICKER_TITLE"

    // --- 과거 시점에서 고치기 (WLD-12, board `LensEdit`, cast spec §CST lens banner) ---
    const val BTN_TO_LATEST = "최신으로"

    // --- Undo labels (shown by the shell's undo toast) ---
    const val UNDO_TEXT_SEARCH = "글자 찾기"

    // --- ⋯ 메뉴 · 숨기기 · 지우기 · 합치기 (WLD-13 … WLD-15, WLD-22; cast spec record menu) ---
    const val MENU_HIDE = "숨기기"
    const val MENU_UNHIDE = "숨김 풀기"
    const val MENU_DELETE = "지우기"
    const val MENU_MERGE = "합치기"
    const val MENU_REJECTIONS = "아님으로 둔 것"
    const val BTN_RESTORE = "되살리기"
    const val BTN_UNDO = "되돌리기"
    const val BTN_CANCEL = "취소"

    /** Merge sheet rows (board `RCMerge`): `옮길 참조 · 5`, `다른 칸 · 3`, `<A> · 합쳐짐`, `<B> · 남음`. */
    const val MERGE_REFS = "옮길 참조"
    const val MERGE_CONFLICTS = "다른 칸"
    const val MERGE_MERGED = "합쳐짐"
    const val MERGE_KEPT = "남음"

    /** The `아님` list is empty: no source gives the line. */
    const val TODO_COPY_WORLD_NO_REJECTIONS = "TODO_COPY_WORLD_NO_REJECTIONS"

    // --- 별칭 (WLD-16) ---
    /** Field of the alias editor: the row label (`별칭`) is its label; the button is `+`. */
    const val ALIAS_ADD = "+"

    /** An alias that is empty, the name itself, or already listed: no source gives the lines. */
    const val TODO_COPY_WORLD_ALIAS_EMPTY = "TODO_COPY_WORLD_ALIAS_EMPTY"
    const val TODO_COPY_WORLD_ALIAS_SAME_AS_NAME = "TODO_COPY_WORLD_ALIAS_SAME_AS_NAME"
    const val TODO_COPY_WORLD_ALIAS_DUPLICATE = "TODO_COPY_WORLD_ALIAS_DUPLICATE"

    // --- 이미지 (WLD-07, board `ImageSlots`) ---
    const val PICK_IMAGE = "이미지 고르기"

    // --- 목록 | 카드 (WLD-05) ---
    const val VIEW_GROUP = "보기"
    const val VIEW_LIST = "목록"
    const val VIEW_CARDS = "카드"

    // --- 연표 (WLD-31, boards `Timeline`, `TimelineAnchor`) ---
    const val TIMELINE_TITLE = "연표"
    const val AGES_AUTO = "나이 자동 계산"
    const val SEARCH_TIMELINE = "사건 · 연도 찾기"
    const val SEARCH_TIMELINE_ANCHOR = "사건 · 때 찾기"
    const val ANCHOR_RANGE_NOTE = "대략인 때는 범위(5~9일)로 · 점검도 범위로"
    const val ANCHOR_PICK = "기준 사건"
    const val TIMELINE_OFF = "시간 없이 · 연표 끔"

    // --- 연표 + 지도 (WLD-64, board `TimelineMap`) ---
    const val TIMELINE_MAP = "연표 + 지도"
    const val SMALL_TIMELINE_MAP = "세계 · 연표 + 지도"
    const val COMPARE = "비교"
    const val LATEST = "최신"

    /** The project has no live map (WLD-09 `지도` tab wording). */
    const val NO_MAP = "◇ 지도 없음"

    // --- 사건 만들기 · 고치기 (WLD-32), 사건 상세 (WLD-33, board `TwoTimeline`) ---
    const val CREATE_EVENT = "사건 만들기"
    const val EVENT_KIND = "종류"
    const val EVENT_WORLD = "세계 사건"
    const val EVENT_STORY = "이야기 사건"
    const val EVENT_YEAR = "연도"
    const val EVENT_MONTH = "월"
    const val EVENT_DAY = "일"
    const val EVENT_SCENE = "씬"
    const val PICK_SCENE = "씬 고르기"
    const val EVENT_PLACE = "장소"
    const val PICK_PLACE = "장소 고르기"
    const val EVENT_TERMS = "관련 용어"
    const val EVENT_REVEAL = "공개 시점"
    const val BTN_PUT = "넣기"
    const val EVENT_CHANGES = "이 사건에서 바뀐 것"

    /** `+ 변화` under `이 사건에서 바뀐 것` (WLD-33 `[PROPOSED]`) and the first step's title. */
    const val CHANGE_ADD = "+ 변화"
    const val CHANGE_TITLE = "변화"

    /** An empty value in the field step (board `TwoTimeline`: `없음 → 하층 구역의 고아`). */
    const val CHANGE_NONE = "없음"

    /** Toast after `+ 변화 › 넣기` (only `되돌리기` is implied). */
    const val TODO_COPY_WORLD_CHANGE_ADDED_TOAST = "TODO_COPY_WORLD_CHANGE_ADDED_TOAST"

    /** The item has no project field that can change: no source gives the line. */
    const val TODO_COPY_WORLD_NO_FIELDS = "TODO_COPY_WORLD_NO_FIELDS"

    /** The date typed is not a number in the calendar: no source gives the line. */
    const val TODO_COPY_WORLD_EVENT_DATE_INVALID = "TODO_COPY_WORLD_EVENT_DATE_INVALID"

    // --- 세계 시간 (WLD-29, boards `NewTime`, `ProjSettings`) ---
    const val PROJECT_SETTINGS = "프로젝트 설정"
    const val WORLD_TIME = "세계 시간"
    const val TIME_YEAR_ONLY = "연도만"
    const val TIME_YEAR_ONLY_SUB = "예: 1200년"
    const val TIME_CALENDAR = "세계 달력 만들기"
    const val TIME_CALENDAR_SUB = "달 이름 · 한 달의 날 수 · 요일을 직접"
    const val TIME_NONE = "시간 없이"
    const val TIME_NONE_SUB = "연표 끔"
    const val STORY_START_YEAR = "이야기가 시작되는 해"
    const val AGES_NOTE = "생일이 있으면 나이 자동 계산"

    // --- 이동 수단 (WLD-47, board `ProjSettings` `이동 하루 거리`) ---
    const val TRAVEL_MODES = "이동 수단"
    const val TRAVEL_ADD = "직접"
    const val TRAVEL_DISTANCE = "하루 거리"
    const val UNIT_KM = "km"
    const val UNIT_HOURS = "시간"

    /** Label of the daily moving hours field (rule 18 `하루 이동 시간` is not drawn as a label). */
    const val TODO_COPY_WORLD_TRAVEL_HOURS = "TODO_COPY_WORLD_TRAVEL_HOURS"

    /** Refusals of the mode form: no name, distance not > 0, hours not in (0, 24], name taken. */
    const val TODO_COPY_WORLD_TRAVEL_NAME_EMPTY = "TODO_COPY_WORLD_TRAVEL_NAME_EMPTY"
    const val TODO_COPY_WORLD_TRAVEL_DISTANCE_BAD = "TODO_COPY_WORLD_TRAVEL_DISTANCE_BAD"
    const val TODO_COPY_WORLD_TRAVEL_HOURS_BAD = "TODO_COPY_WORLD_TRAVEL_HOURS_BAD"
    const val TODO_COPY_WORLD_TRAVEL_NAME_TAKEN = "TODO_COPY_WORLD_TRAVEL_NAME_TAKEN"

    // --- 세계 달력 (WLD-30 editor `[PROPOSED]`, words of the `세계 달력 만들기` option) ---
    const val CALENDAR_TITLE = "세계 달력"
    const val MONTH_NAME = "달 이름"
    const val MONTH_DAYS = "한 달의 날 수"
    const val WEEKDAY = "요일"
    const val ADD_MONTH = "+ 달"
    const val ADD_WEEKDAY = "+ 요일"
    const val BTN_SAVE = "저장"

    /** Label of the story start date (month chips + day) and of its weekday: not drawn (Q-13, Q-21). */
    const val TODO_COPY_WORLD_CALENDAR_START_LABEL = "TODO_COPY_WORLD_CALENDAR_START_LABEL"
    const val TODO_COPY_WORLD_CALENDAR_START_WEEKDAY_LABEL = "TODO_COPY_WORLD_CALENDAR_START_WEEKDAY_LABEL"

    /** Lines that stop `저장`: no month, a month without name, days not ≥ 1, an empty weekday, a start date outside its month. */
    const val TODO_COPY_WORLD_CALENDAR_NO_MONTH = "TODO_COPY_WORLD_CALENDAR_NO_MONTH"
    const val TODO_COPY_WORLD_CALENDAR_MONTH_NAME = "TODO_COPY_WORLD_CALENDAR_MONTH_NAME"
    const val TODO_COPY_WORLD_CALENDAR_MONTH_DAYS = "TODO_COPY_WORLD_CALENDAR_MONTH_DAYS"
    const val TODO_COPY_WORLD_CALENDAR_WEEKDAY_NAME = "TODO_COPY_WORLD_CALENDAR_WEEKDAY_NAME"
    const val TODO_COPY_WORLD_CALENDAR_START = "TODO_COPY_WORLD_CALENDAR_START"

    /** Toast after `저장` of the calendar (only `되돌리기` is implied). */
    const val TODO_COPY_WORLD_CALENDAR_SAVED_TOAST = "TODO_COPY_WORLD_CALENDAR_SAVED_TOAST"

    /** Accessibility label of a row's remove button: `가달 빼기` (pattern of `<별칭> 빼기`). */
    fun removeLabel(name: String): String = "$name 빼기"

    /** The segmented control of the bulk sheet. */
    fun bulkMode(mode: io.github.lenticualrstone.civilight.domain.world.term.BulkMode): String = when (mode) {
        io.github.lenticualrstone.civilight.domain.world.term.BulkMode.LINK_ALL -> BULK_LINK_ALL
        io.github.lenticualrstone.civilight.domain.world.term.BulkMode.REJECT_ALL -> BULK_REJECT_ALL
    }

    /** The toast after the bulk sheet's `넣기` (copy pending). */
    @Suppress("UNUSED_PARAMETER")
    fun bulkDone(mode: io.github.lenticualrstone.civilight.domain.world.term.BulkMode, count: Int): String = when (mode) {
        io.github.lenticualrstone.civilight.domain.world.term.BulkMode.LINK_ALL -> TODO_COPY_WORLD_BULK_LINKED_TOAST
        io.github.lenticualrstone.civilight.domain.world.term.BulkMode.REJECT_ALL -> TODO_COPY_WORLD_BULK_REJECTED_TOAST
    }

    /** Value step title: `리엔 · 소속` (the row label of `이 사건에서 바뀐 것`). */
    fun changeTitle(item: String, field: String): String = "$item$SEP$field"

    /** `장소 고르기`, `용어 고르기`, `세력 고르기` (pattern of `씬 고르기 · 장소 고르기`). */
    fun pickLabel(kind: io.github.lenticualrstone.civilight.ui.picker.PickKind): String = when (kind) {
        io.github.lenticualrstone.civilight.ui.picker.PickKind.PLACE -> PICK_PLACE
        io.github.lenticualrstone.civilight.ui.picker.PickKind.TERM -> "용어 고르기"
        else -> "세력 고르기"
    }

    /** `기준 사건 · 결사 합류` (board `TimelineAnchor`). */
    fun anchorChip(chip: String, eventTitle: String): String = "$chip$SEP$eventTitle"

    /** `달력: 가달 · 나달` (the 연표 info line in calendar mode). */
    fun calendarLine(months: List<String>): String = "달력: ${months.joinToString(SEP)}"

    /** `옮길 참조 · 5`, `다른 칸 · 3`. */
    fun countLine(label: String, count: Int): String = "$label$SEP$count"

    /** `노아(시장) · 합쳐짐`. */
    fun withWord(name: String, word: String): String = "$name$SEP$word"

    /** `노아(시장) · ‘합침 → 노아’로 휴지통` (board `RCMerge`). */
    fun mergedToTrash(merged: String, target: String): String = "$merged$SEP‘합침 → $target’로 휴지통"

    /** Accessibility label of an alias chip's remove button: `빛돌 빼기`. */
    fun removeAlias(alias: String): String = "$alias 빼기"

    /** `후보 2 새로 찾음` next to `별칭` (board `RTLinkAll`). */
    fun newlyFound(count: Int): String = "후보 $count 새로 찾음"

    /** `+` and empty-state button of a tab: `용어 만들기` (WLD-02), `장소 만들기` (MapHex), `세력 만들기` (same pattern). */
    fun createLabel(kind: WorldKind): String = when (kind) {
        WorldKind.TERM -> CREATE_TERM
        WorldKind.PLACE -> CREATE_PLACE
        WorldKind.FACTION -> CREATE_FACTION
    }

    /** The card's small header line `세계 · 용어`. */
    fun smallLine(kind: WorldKind): String = when (kind) {
        WorldKind.TERM -> SMALL_TERM
        WorldKind.PLACE -> SMALL_PLACE
        WorldKind.FACTION -> SMALL_FACTION
    }

    /** Accessibility label of a card's `⋯` (pattern of the scene head's `씬 4 메뉴`). */
    fun menuLabel(name: String): String = "$name 메뉴"

    /** The locked card or row: `숨긴 용어 1`. */
    fun hiddenRow(kind: WorldKind, count: Int): String = "숨긴 ${kind.label} $count"

    /** `빈 항목 3 · 보기` (WLD-06). */
    fun emptyFields(count: Int): String = "빈 항목 $count${SEP}보기"

    /** Banner title at a past lens: `1-5 시점 값 고치는 중` (board `LensEdit`). */
    fun lensEditTitle(code: String): String = "$code 시점 값 고치는 중"

    /** Banner line at a past lens: `고치면 1-5부터 적용` (board `LensEdit`). */
    fun lensEditSub(code: String): String = "고치면 ${code}부터 적용"

    /** The toast after a first `첫 공개` was set: `◇ 첫 공개 1-2`. */
    fun revealSet(label: String): String = "$REVEAL_NOW $label"
}
