package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType

/** The nine field types (CST-20): labels, filter/sort abilities and allowed conversions (CST-23). */
object FieldTypes {
    /** Table order of CST-20 (also the `종류` radio grid order). */
    val ALL: List<FieldType> = listOf(
        FieldType.SHORT_TEXT, FieldType.LONG_TEXT, FieldType.NUMBER, FieldType.CHOICE, FieldType.MULTI_CHOICE,
        FieldType.DATE, FieldType.ANNIVERSARY, FieldType.LINK, FieldType.CALC,
    )

    fun label(type: FieldType): String = when (type) {
        FieldType.SHORT_TEXT -> "짧은 글"
        FieldType.LONG_TEXT -> "긴 글"
        FieldType.NUMBER -> "숫자"
        FieldType.CHOICE -> "고르기"
        FieldType.MULTI_CHOICE -> "여러 개 고르기"
        FieldType.DATE -> "날짜"
        FieldType.ANNIVERSARY -> "기념일"
        FieldType.LINK -> "연결"
        FieldType.CALC -> "계산"
    }

    /** `필터` may be checked only for 고르기 · 여러 개 고르기 · 연결 (TC-F-20). */
    fun canFilter(type: FieldType): Boolean =
        type == FieldType.CHOICE || type == FieldType.MULTI_CHOICE || type == FieldType.LINK

    /** Sort orders `<항목 이름>순` exist for 숫자 · 날짜 · 기념일 · 계산 (CST-03). */
    fun canSort(type: FieldType): Boolean =
        type == FieldType.NUMBER || type == FieldType.DATE || type == FieldType.ANNIVERSARY || type == FieldType.CALC

    /** `숫자` allows a range filter. */
    fun canRangeFilter(type: FieldType): Boolean = type == FieldType.NUMBER

    /** Values hit by the command-bar search as `<항목> · <값>` (CST-27); long text is searched like body text. */
    fun isValueSearchable(type: FieldType): Boolean =
        type == FieldType.SHORT_TEXT || type == FieldType.CHOICE || type == FieldType.MULTI_CHOICE || type == FieldType.LINK

    /** Has options (option editor). */
    fun hasOptions(type: FieldType): Boolean = type == FieldType.CHOICE || type == FieldType.MULTI_CHOICE

    /**
     * Types a field may be changed to (CST-23 table); anything else is not offered. Long text
     * converts to nothing (TC-F-07).
     */
    fun conversionTargets(from: FieldType): List<FieldType> = when (from) {
        FieldType.SHORT_TEXT -> listOf(FieldType.CHOICE, FieldType.MULTI_CHOICE, FieldType.NUMBER, FieldType.LINK)
        FieldType.CHOICE -> listOf(FieldType.MULTI_CHOICE, FieldType.SHORT_TEXT)
        FieldType.NUMBER, FieldType.LINK, FieldType.DATE -> listOf(FieldType.SHORT_TEXT)
        FieldType.LONG_TEXT, FieldType.MULTI_CHOICE, FieldType.ANNIVERSARY, FieldType.CALC -> emptyList()
    }

    /** `연결` targets: exactly 세력 · 장소 · 용어, never 인물 (TC-13-16). */
    val LINK_TARGETS: List<FieldLinkTargetKind> = listOf(FieldLinkTargetKind.FACTION, FieldLinkTargetKind.PLACE, FieldLinkTargetKind.TERM)

    fun precisionLabel(precision: DatePrecision): String = when (precision) {
        DatePrecision.YEAR -> "연도"
        DatePrecision.YMD -> "연 · 월 · 일"
    }
}
