package io.github.lenticualrstone.civilight.feature.canvas.page

import java.time.Instant
import java.time.ZoneId

/**
 * UI copy of pages (CNV-13 … CNV-16; board `Page`, rule 19 §6, rule 20 §2, rule 23 §6). Keys starting
 * `BTN_` are buttons; copy no board gives is the nearest existing wording, listed under Requests in
 * `docs/status/feature-canvas.md`. `CanvasCopyLintTest` lints every value.
 */
object PageCopy {
    const val SEP = " · "

    // --- Header (board `Page`) ---
    /** Small mono line of the header and the area word of the section line. */
    const val SMALL_LINE = "페이지"
    const val BACK = "뒤로"

    /** Label of the `⋯` button (pattern `구조 메뉴`, `<name> 메뉴`). */
    const val MENU = "페이지 메뉴"
    const val READ_ONLY = "읽기 전용"

    /** `페이지 · 세계관 메모`; a page without a section label reads `페이지`. */
    fun sectionLine(label: String?): String = if (label.isNullOrBlank()) SMALL_LINE else "$SMALL_LINE$SEP${label.trim()}"

    const val LAST_EDITED = "마지막 수정"
    const val LINKS = "연결"

    /** `마지막 수정 11월 20일 · 연결 4`. */
    fun meta(date: String, links: Int): String = "$LAST_EDITED $date$SEP$LINKS $links"

    /** `11월 20일` in [zone]. */
    fun monthDay(at: Long, zone: ZoneId): String {
        val d = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        return "${d.monthValue}월 ${d.dayOfMonth}일"
    }

    /** The author-memo block label (`⌁ 작가 메모`). */
    const val MEMO_LABEL = "⌁ 작가 메모"

    /** Command bar hint, verbatim from the board (Q-PG-07: the comma stays until the source changes). */
    const val HINT = "@로 인물 · 용어 연결, /로 블록 추가"
    const val MIC = "라이프 레코더"

    /**
     * Name shown for a page whose name is still empty (a page just made from the list). The trash
     * area's `ED` word for an unnamed thing; no page source names it.
     */
    const val UNNAMED = "미정"

    // --- Block kinds (`/`, spec §3.1 PageBlock: 본문 · 소제목 · ⌁ 작가 메모) ---
    /** Title of the `/` sheet, from the hint `/로 블록 추가`. */
    const val BLOCK_ADD_TITLE = "블록 추가"
    const val BLOCK_PARAGRAPH = "본문"
    const val BLOCK_HEADING = "소제목"
    const val BLOCK_MEMO = MEMO_LABEL

    // --- Page menu (`⋯`, Q-PG-02: contents not drawn; vocabulary words) ---
    const val BTN_EDIT = "고치기"
    const val BTN_HIDE = "숨기기"
    const val BTN_UNHIDE = "숨김 풀기"
    const val BTN_DELETE = "지우기"

    /** The page header image slot (CNV-25 table). */
    const val BTN_HEADER_IMAGE = "페이지 머리 이미지"
    const val BTN_CLOSE = "닫기"

    // --- Hidden things (rule 19 §6) ---
    const val HIDDEN_CHARACTER = "숨긴 인물"
    const val HIDDEN_ITEM = "숨긴 항목"
    const val HIDDEN_TAG = "숨김"
    const val BTN_UNLOCK = "지문으로 보기"

    /** `숨긴 페이지 1`: the one locked row of a page list (rule 19 §6). */
    fun hiddenPages(count: Int): String = "숨긴 $SMALL_LINE $count"

    // --- A link to a deleted item (rule 20 §2 row `@연결`) ---
    const val DELETED_MARK = "◇"
    const val BTN_RELINK = "다른 항목에 잇기"
    const val BTN_UNLINK = "글로 풀기"
    const val BTN_RESTORE = "되살리기"
    const val BTN_KEEP = "이대로 두기"

    /** A link to a page that is gone without a name tag (pattern `지운 인물`, rule 20 §2). */
    const val DELETED_PAGE = "지운 페이지"

    // --- Page list (CNV-14) ---
    /** `페이지 · 2` (pattern `프로젝트 · N`). */
    fun listSection(count: Int): String = "$SMALL_LINE$SEP$count"

    /** The row line under a page name: `설정 노트 · 7월 2일`. */
    fun rowSub(sectionLabel: String?, date: String): String =
        listOfNotNull(sectionLabel?.trim()?.takeIf { it.isNotEmpty() }, date).joinToString(SEP)

    // --- Copy no board gives (Q-EM-01, Q-PG-05): the nearest existing words, listed under Requests ---
    /** The `+` of the page list (accessibility label) and the undo step of a new page. */
    const val NEW_PAGE = "새 페이지"
    const val PAGE_LIST_EMPTY = "페이지 없음"
    const val LINK_PICKER_TITLE = "항목 고르기"

    /** Undo-step names (the button copy). */
    const val UNDO_TEXT = BTN_EDIT
    const val UNDO_NEW_PAGE = NEW_PAGE
}
