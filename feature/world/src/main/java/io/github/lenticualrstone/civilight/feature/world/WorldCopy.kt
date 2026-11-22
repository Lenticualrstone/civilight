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

    // --- `@` 연결 in card text (WLD-17) ---
    /** The keyboard tool (`EditKeyboard`: `@`). */
    const val LINK_TOOL = "@"

    /** Title of the `@` picker: no board names it (feature:script has the same request). */
    const val TODO_COPY_WORLD_LINK_PICKER_TITLE = "TODO_COPY_WORLD_LINK_PICKER_TITLE"

    // --- 과거 시점에서 고치기 (WLD-12, board `LensEdit`, cast spec §CST lens banner) ---
    const val BTN_TO_LATEST = "최신으로"

    // --- Undo labels (shown by the shell's undo toast) ---
    const val UNDO_TEXT_SEARCH = "글자 찾기"

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
