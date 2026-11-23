package io.github.lenticualrstone.civilight.feature.cast.templates

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.model.cast.EntityKind

/**
 * Copy of 인물 템플릿 (30-cast.md §7.6; CST-56 … CST-62). Terse fragments joined with ` · `; the new
 * noun is `템플릿` (user-made sets), built-in sets stay `시작 묶음`, applying either is `항목 더하기`.
 */
object TemplateCopy {
    const val SAVE_ROW = "템플릿으로 저장"
    const val SAVE_SUB = "프로젝트 밖에 보관 · 새 프로젝트 · 항목 더하기에서 사용"
    const val SAME_NAME = "◇ 같은 이름 템플릿 있음"
    const val WHERE = "어디에"
    const val NEW = "새 템플릿"
    const val REPLACE = "있는 템플릿 바꾸기"
    const val REPLACE_SUB = "그 템플릿 항목을 이 목록으로"
    const val DEFS_ONLY = "항목 정의만 · 값 없음"
    const val HIDDEN = FieldTemplates.HIDDEN_NAME
    const val HIDDEN_SUB = "지문으로 열 때만 보임"
    const val VISIBLE_NOTICE = "◇ 잠가도 보임"
    const val HIDDEN_MARK = "숨김"
    const val NAME = "이름"
    const val BACK = "뒤로"
    const val BTN_SAVE = "저장"
    const val BTN_CLOSE = "닫기"
    const val BTN_CANCEL = "취소"
    const val BTN_MAKE = "만들기"
    const val BTN_DELETE = "지우기"
    const val BTN_REMOVE = "빼기"
    const val BTN_RESTORE = "되살리기"
    const val BTN_UNDO = "되돌리기"
    const val BTN_EDIT = "고치기 ›"
    const val BTN_UNLOCK = "지문으로 보기"

    const val TITLE = "인물 템플릿"
    const val HEAD = "설정"
    const val ADD_ROW = "템플릿"
    const val MAKE = "템플릿 만들기"
    const val START = "시작"
    const val FROM_TEMPLATE = "템플릿에서"
    const val FROM_PROJECT = "프로젝트에서"
    const val START_EMPTY = "비어 있음"
    const val EMPTY = "템플릿 없음"
    const val EMPTY_SUB = "프로젝트 설정 › 인물 항목 › 템플릿으로 저장"
    const val TRASH_TITLE = "지운 템플릿"
    const val REMOVE_ONLY = "이 템플릿에서만 · 프로젝트 그대로"
    const val KEPT_IN_PROJECTS = "프로젝트에 넣은 항목은 그대로"
    const val TRASH_LINE = "지운 템플릿 · 30일 뒤 삭제"
    const val CALC_SOURCE = "계산 원본"
    const val CALC_NONE = "없음"

    /** `템플릿 · 3` (`항목 더하기` and step-4 pickers). */
    const val SECTION = "템플릿"
    const val OTHER_KINDS = "장소 · 용어 · 세력도"

    fun picks(n: Int): String = "담을 항목 · $n"

    fun kindCount(kind: EntityKind, n: Int): String = "${kind.label} · $n"

    fun calcWithoutSource(calc: String, source: String): String = "◇ $calc · $source 없이 · 직접 넣기"

    fun scopedLeftOut(n: Int): String = "이 인물만 항목 $n · 빠짐"

    fun saved(name: String): String = "템플릿 $name 저장"

    fun deleted(name: String): String = "템플릿 $name 지움"

    fun section(n: Int): String = "$SECTION · $n"

    fun fields(n: Int): String = "템플릿 항목 · $n"

    fun list(n: Int): String = "템플릿 · $n"

    fun trashRow(n: Int): String = "$TRASH_TITLE · $n"

    /** `11월 23일 지움 · 30일 뒤 삭제`. */
    fun trashSub(month: Int, day: Int): String = "${month}월 ${day}일 지움 · 30일 뒤 삭제"

    fun deleteLines(n: Int): List<String> = listOf("항목 $n", KEPT_IN_PROJECTS, TRASH_LINE)

    fun previewAdd(n: Int): String = "더할 항목 · $n"

    fun previewHave(n: Int): String = "이미 있음 · $n"

    fun previewClash(n: Int): String = "◇ 같은 이름 · 다른 종류 · $n"

    fun clashRow(name: String, type: String): String = CastCopy.join(name, type, "건너뜀")

    /** `장소 1 · 세력 2`. */
    fun otherKindsSub(counts: List<Pair<EntityKind, Int>>): String = CastCopy.joinAll(counts.map { (k, n) -> "${k.label} $n" })
}
