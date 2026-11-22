package io.github.lenticualrstone.civilight.feature.script.change

/**
 * Copy of the `◆ 변화 기록` sheet (SCR-41, board `ChangeCreate`) that `domain:cast` `RelationDrafts`
 * does not already give (its title, `취소`, `바꾸는 것`, `시작 시점`, `반영`, `기록`, `+ 새 종류` are used
 * as they come). Linted by `ScriptCopyLintTest`.
 */
object ChangeCopy {
    /** Segments `관계 | 필드 | 용어 공개`. */
    const val SEG_RELATION = "관계"
    const val SEG_FIELD = "필드"
    const val SEG_TERM = "용어 공개"

    /** An unset character chip (SCR-41 `[PROPOSED]`). */
    const val PICK_CHARACTER = "인물 고르기"

    /** The unset term chip of `용어 공개`, and the term picker's title (spec words: `용어` + `고르기`). */
    const val PICK_TERM = "용어 고르기"

    /** `시작 시점` (domain `Records.startRow` prefix). */
    const val START = "시작 시점"

    /** `바꾸는 것` section label (as `RelationDrafts.CHANGE_ITEMS`). */
    const val CHANGE_ITEMS = "바꾸는 것"

    /** `호칭` text field label (SCR-41 `[PROPOSED]`: a text field for 호칭). */
    const val ADDRESS_FIELD = "호칭"

    /** The field chip of the character's display name (CST-11). */
    const val DISPLAY_NAME = "표시 이름"

    /** `값 · 만나기 전 →` for a field: `값 · ??? →`. */
    fun valueLabel(now: String): String = "값 · $now →"

    /** A field with no value yet. */
    const val EMPTY_VALUE = "비어 있음"

    /** `+ 새 종류` text field label and its add button. */
    const val NEW_KIND_FIELD = "새 종류"
    const val ADD = "추가"

    /** A locked hidden character / item named in a chip (rule 19, SCR-58; the picker's words). */
    const val HIDDEN_CHARACTER = "숨긴 인물"
    const val HIDDEN_ITEM = "숨긴 항목"

    val all: List<String> = listOf(
        SEG_RELATION, SEG_FIELD, SEG_TERM, PICK_CHARACTER, PICK_TERM, START, CHANGE_ITEMS, ADDRESS_FIELD, DISPLAY_NAME,
        valueLabel("???"), EMPTY_VALUE, NEW_KIND_FIELD, ADD, HIDDEN_CHARACTER, HIDDEN_ITEM,
    )

    val buttons: List<String> = listOf(PICK_CHARACTER, PICK_TERM, ADD)
}
