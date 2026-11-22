package io.github.lenticualrstone.civilight.feature.story.bulk

import io.github.lenticualrstone.civilight.domain.story.bulk.BulkFilter

/**
 * Copy of 장소 · 때 한꺼번에 (STO-31 … STO-35; boards `BulkList`, `BulkPlace`, `BulkWhen`, `BulkSuggest`,
 * `BulkWide`), verbatim. Lines built from data (`고른 씬 4개 · 1-6 씬 2`, `씬 4개 → 잿더미 시장`, preview rows,
 * suggestion cards) come from `domain:story` `bulk` package or the functions here.
 */
object BulkCopy {
    const val SEP = " · "
    const val TITLE = "장소 · 때 한꺼번에"
    const val WIDE = "펼친 화면"
    const val PLACE = "장소"
    const val TIME = "때"
    const val SELECT_ALL = "모두 고르기"
    const val NO_PLACE = "장소 없음"
    const val NO_TIME = "때 없음"

    /** A hidden place while the project's hidden items are locked (R19 §6, as the `장소와 때` sheet). */
    const val HIDDEN_PLACE = "숨긴 항목"
    const val SET_PLACE = "장소 정하기"
    const val SET_TIME = "때 정하기"
    const val SUGGEST = "제안 받기"
    const val CLOSE = "닫기"
    const val BACK = "뒤로"

    // 장소 정하기 (BulkPlace)
    const val ALL_SAME_PLACE = "모두 같은 장소"
    const val ONE_BY_ONE = "씬마다 차례로"
    const val KIND_PLACE = "장소"
    const val KIND_REGION = "지역"
    const val CHOSEN = "고름"
    const val NEW_PLACE = "새 장소"
    const val NEW_PLACE_SUB = "지도에 핀을 꽂으며 만들기"

    // 때 정하기 (BulkWhen)
    const val CHAIN = "직전 씬 기준 차례로"
    const val ALL_SAME_TIME = "모두 같은 때"
    const val BETWEEN = "씬 사이"
    const val SAME_DAY = "같은 날"
    const val PLUS_ONE = "+1일"
    const val PLUS_TWO = "+2일"
    const val PER_SCENE = "씬마다 고르기"
    const val BAND = "시간대"
    const val KEEP = "그대로"
    const val CLEAR = "비워 둠"
    const val PRECISION = "정밀도"
    const val EXACT = "정확"
    const val APPROX = "대략"
    const val PREVIEW = "미리 보기"
    const val APPROX_NOTE = "대략으로 넣으면 점검은 넓게 · 나중에 씬에서 정확히"

    // 제안 받기 (BulkSuggest)
    const val PUT = "넣기"
    const val FIX = "고치기"
    const val SKIP = "건너뛰기"

    // 표로 한꺼번에 (BulkWide)
    const val COL_SCENE = "씬"
    const val COL_PLACE = "장소"
    const val COL_TIME = "때"
    const val COL_SUGGEST = "제안"
    const val ADD_PLACE = "+ 장소"
    const val ADD_TIME = "+ 때"
    const val NONE = "—"
    const val MAP_TITLE = "장소별 씬 수"
    const val MAP_NOTE = "핀으로 끌어다 놓기 대신 · 고른 씬 → 핀 탭 → 장소 정하기"

    /** `전체 31` · `장소 · 때 빈 것 12` · `제안 있음 3`. */
    fun chip(filter: BulkFilter, count: Int): String = when (filter) {
        BulkFilter.ALL -> "전체 $count"
        BulkFilter.EMPTY -> "장소 · 때 빈 것 $count"
        BulkFilter.SUGGESTED -> "제안 있음 $count"
    }

    /** `고른 씬 4개`. */
    fun chosenCount(n: Int): String = "고른 씬 ${n}개"

    /** `제안 받기 3`. */
    fun suggestButton(n: Int): String = "$SUGGEST $n"

    /** `씬 4개 → 잿더미 시장 · 결사의 문` (`씬마다 차례로` with several places: the names in scene order). */
    fun placeResult(count: Int, names: List<String>): String = "씬 ${count}개 → ${names.distinct().joinToString(SEP)}"

    /** `1/4` (the step of `씬마다 차례로`; the step line copy is not drawn, so only the figures show). */
    fun step(index: Int, count: Int): String = "${index + 1}/$count"
}
