package io.github.lenticualrstone.civilight.domain.library.onboarding

import io.github.lenticualrstone.civilight.model.app.OnboardingState

/**
 * The modules that run a tooltip tour on first entry (LIB-19, `OnboardingState.moduleTourSeen`).
 * [id] is the key in `moduleTourSeen` (spec §3.2 ModuleId); [rowTitle] the `도움말 · 안내 다시 보기`
 * row (board `HelpReplay`); [label] the mono label before `k/n` on the tooltip card (null = `k/n`
 * only, as the script tour draws it). Listed in the `HelpReplay` row order.
 */
enum class TourModule(val id: String, val rowTitle: String, val label: String?) {
    SCRIPT("SCRIPT", "스크립트", null),
    CAST("CAST", "인물", "인물 안내"),
    WORLD_TIMELINE("WORLD_TIMELINE", "세계 · 연표", "세계 안내"),
    RECORDER("RECORDER", "라이프 레코더", "레코더 안내"),
    LIBRARY_HIDING("LIBRARY_HIDING", "서재 · 숨기기", "서재 안내"),
    MAP("MAP", "지도 만들기", "지도 안내"),
    ROUTE_PLACE_TIME("ROUTE_PLACE_TIME", "인물 동선 · 장소와 때", "장소와 때 안내"),
}

/** One tooltip step: title and sub line (DS `TourCard`). */
data class TourStep(val title: String, val sub: String) {
    /** False while any of its copy is still a `TODO_COPY_` constant. */
    val hasCopy: Boolean get() = !title.startsWith(TODO_PREFIX) && !sub.startsWith(TODO_PREFIX)
}

private const val TODO_PREFIX = "TODO_COPY_"

/**
 * Tooltip tours (LIB-19): the step copy, the seen flags and the card buttons. Each module area
 * supplies the targets (where the hole goes) and calls [shouldStart] on its first entry; the shade,
 * hole and card are DS `TourDim` + `TourCard`. Steps the boards draw are verbatim; the undrawn steps
 * and the whole 서재 · 숨기기 tour (§6 Q15) were worded by the orchestrator on 2026-11-23 from each
 * area's own copy. A tour whose copy is incomplete would not start on its own ([shouldStart]).
 */
object ModuleTours {
    const val SKIP = "건너뛰기"
    const val PREVIOUS = "이전"
    const val NEXT = "다음"
    const val FINISH = "마치기"


    /** Steps per module, in order (spec LIB-19 table; boards `Tour1`, `Tour2`, `Tour4`, `TourCast`, `TourWorld`, `TourLr`, `TourMap`, `TourWhen`). */
    fun steps(module: TourModule): List<TourStep> = when (module) {
        TourModule.SCRIPT -> listOf(
            TourStep("명령 칸", "이동 · 검색 · 블록 추가 · 마이크"),
            TourStep("블록을 왼쪽으로", "시간 · 참고 · 속성 → 옆 패널 탭"),
            TourStep("씬 머리", "장소와 때 · 전환 문구 · ⋯ 메뉴"),
            TourStep("시점 렌즈", "고른 시점까지의 인물 · 관계 · 용어"),
        )
        TourModule.CAST -> listOf(
            TourStep("시점 렌즈", "이 시점까지의 필드 · 관계만"),
            TourStep("값이 바뀌는 때", "이 값 고치기 · 여기서부터 바뀜"),
            TourStep("관계", "사이 · 양쪽 마음 · 호칭"),
        )
        TourModule.WORLD_TIMELINE -> listOf(
            TourStep("공개 시점", "앞에서 쓰이면 ▲ 점검 센터"),
            TourStep("용어 잇기", "원고 속 후보 · 잇기 · 아님"),
            TourStep("연표", "사건을 때 순서로 · 쓰인 곳"),
        )
        TourModule.RECORDER -> listOf(
            TourStep("기록 남기기", "녹음 · 글로 쓰기 · 멈추고 저장"),
            TourStep("연결", "보던 곳 · 넣기 전까지 내용은 그대로"),
            TourStep("기록함", "원고에 넣기 · 카드에 넣기 · 페이지로 만들기"),
        )
        TourModule.LIBRARY_HIDING -> listOf(
            TourStep("프로젝트 숨기기", "⋯ 메뉴 · 지문 · PIN으로 열림"),
            TourStep("숨긴 프로젝트", "한 묶음 · 연 뒤 다시 잠그기"),
            TourStep("설정 › 숨기기", "위젯 · 최근 항목 · 검색에서 빠짐"),
        )
        TourModule.MAP -> listOf(
            TourStep("도구 줄", "이동 · 핀 · 영역 · 경로 · 라벨 · 레이어 · 고른 것만 동작"),
            TourStep("경로", "거리 · 이동 수단 → 걸리는 날"),
            TourStep("보이는 시점", "이 시점부터 지도에 나옴 · 쓰인 곳"),
        )
        TourModule.ROUTE_PLACE_TIME -> listOf(
            TourStep("장소와 때", "직전 씬 기준 · 시간대 · 정확/대략 → 이동 점검"),
            TourStep("▲ 이동 시간", "못 닿으면 표시 · +일 · 다른 수단 · 맞음"),
            TourStep("인물 동선", "씬 순서대로 지도 위에"),
        )
    }

    fun stepCount(module: TourModule): Int = steps(module).size

    /** Every step and the label have real copy. */
    fun copyComplete(module: TourModule): Boolean =
        steps(module).all { it.hasCopy } && module.label?.startsWith(TODO_PREFIX) != true

    fun seen(state: OnboardingState, module: TourModule): Boolean = state.moduleTourSeen[module.id] == true

    /**
     * Whether the tour runs on this entry into [module]: not seen yet, and its copy is complete (a
     * tour whose steps would print `TODO_COPY_` stays dormant until the copy exists).
     */
    fun shouldStart(state: OnboardingState, module: TourModule): Boolean = !seen(state, module) && copyComplete(module)

    /** `건너뛰기` and `마치기` both mark the whole module tour as seen (LIB-19). */
    fun markSeen(state: OnboardingState, module: TourModule): OnboardingState =
        state.copy(moduleTourSeen = state.moduleTourSeen + (module.id to true))

    /** `다시 보기` (LIB-21): the tour runs again on the next entry (`ED`). */
    fun reset(state: OnboardingState, module: TourModule): OnboardingState =
        state.copy(moduleTourSeen = state.moduleTourSeen + (module.id to false))
}

/**
 * A running tour on one screen (pure reducer): the step shown, the card's buttons and what each
 * does. [index] is 0-based. `이전` from step 2 on; the last step's primary is `마치기`.
 */
data class TourRun(val module: TourModule, val index: Int = 0) {
    val steps: List<TourStep> get() = ModuleTours.steps(module)
    val count: Int get() = steps.size
    val step: TourStep get() = steps[index.coerceIn(0, count - 1)]
    val isLast: Boolean get() = index >= count - 1
    val hasPrevious: Boolean get() = index > 0
    val primaryLabel: String get() = if (isLast) ModuleTours.FINISH else ModuleTours.NEXT

    /** Null = the tour ends (mark it seen). */
    fun next(): TourRun? = if (isLast) null else copy(index = index + 1)

    fun previous(): TourRun = copy(index = (index - 1).coerceAtLeast(0))
}
