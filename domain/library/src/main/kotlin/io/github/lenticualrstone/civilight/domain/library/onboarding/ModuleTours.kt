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
    LIBRARY_HIDING("LIBRARY_HIDING", "서재 · 숨기기", TODO_COPY_LIB19_LIBRARY_LABEL),
    MAP("MAP", "지도 만들기", "지도 안내"),
    ROUTE_PLACE_TIME("ROUTE_PLACE_TIME", "인물 동선 · 장소와 때", "장소와 때 안내"),
}

/** One tooltip step: title and sub line (DS `TourCard`). */
data class TourStep(val title: String, val sub: String) {
    /** False while any of its copy is still a `TODO_COPY_` constant. */
    val hasCopy: Boolean get() = !title.startsWith(TODO_PREFIX) && !sub.startsWith(TODO_PREFIX)
}

/** The 서재 · 숨기기 tour has no board (§6 Q15). */
const val TODO_COPY_LIB19_LIBRARY_LABEL = "TODO_COPY_LIB19_LIBRARY_LABEL"
private const val TODO_PREFIX = "TODO_COPY_"

/**
 * Tooltip tours (LIB-19): the step copy drawn on the boards, the seen flags and the card buttons.
 * Each module area supplies the targets (where the hole goes) and calls [shouldStart] on its first
 * entry; the shade, hole and card are DS `TourDim` + `TourCard`. Undrawn steps are `TODO_COPY_`
 * constants; a tour whose copy is incomplete does not start on its own ([shouldStart]).
 */
object ModuleTours {
    const val SKIP = "건너뛰기"
    const val PREVIOUS = "이전"
    const val NEXT = "다음"
    const val FINISH = "마치기"

    const val TODO_COPY_LIB19_SCRIPT_3_TITLE = "TODO_COPY_LIB19_SCRIPT_3_TITLE"
    const val TODO_COPY_LIB19_SCRIPT_3_SUB = "TODO_COPY_LIB19_SCRIPT_3_SUB"
    const val TODO_COPY_LIB19_CAST_2_TITLE = "TODO_COPY_LIB19_CAST_2_TITLE"
    const val TODO_COPY_LIB19_CAST_2_SUB = "TODO_COPY_LIB19_CAST_2_SUB"
    const val TODO_COPY_LIB19_CAST_3_TITLE = "TODO_COPY_LIB19_CAST_3_TITLE"
    const val TODO_COPY_LIB19_CAST_3_SUB = "TODO_COPY_LIB19_CAST_3_SUB"
    const val TODO_COPY_LIB19_WORLD_2_TITLE = "TODO_COPY_LIB19_WORLD_2_TITLE"
    const val TODO_COPY_LIB19_WORLD_2_SUB = "TODO_COPY_LIB19_WORLD_2_SUB"
    const val TODO_COPY_LIB19_WORLD_3_TITLE = "TODO_COPY_LIB19_WORLD_3_TITLE"
    const val TODO_COPY_LIB19_WORLD_3_SUB = "TODO_COPY_LIB19_WORLD_3_SUB"
    const val TODO_COPY_LIB19_RECORDER_1_TITLE = "TODO_COPY_LIB19_RECORDER_1_TITLE"
    const val TODO_COPY_LIB19_RECORDER_1_SUB = "TODO_COPY_LIB19_RECORDER_1_SUB"
    const val TODO_COPY_LIB19_RECORDER_3_TITLE = "TODO_COPY_LIB19_RECORDER_3_TITLE"
    const val TODO_COPY_LIB19_RECORDER_3_SUB = "TODO_COPY_LIB19_RECORDER_3_SUB"
    const val TODO_COPY_LIB19_LIBRARY_1_TITLE = "TODO_COPY_LIB19_LIBRARY_1_TITLE"
    const val TODO_COPY_LIB19_LIBRARY_1_SUB = "TODO_COPY_LIB19_LIBRARY_1_SUB"
    const val TODO_COPY_LIB19_LIBRARY_2_TITLE = "TODO_COPY_LIB19_LIBRARY_2_TITLE"
    const val TODO_COPY_LIB19_LIBRARY_2_SUB = "TODO_COPY_LIB19_LIBRARY_2_SUB"
    const val TODO_COPY_LIB19_LIBRARY_3_TITLE = "TODO_COPY_LIB19_LIBRARY_3_TITLE"
    const val TODO_COPY_LIB19_LIBRARY_3_SUB = "TODO_COPY_LIB19_LIBRARY_3_SUB"
    const val TODO_COPY_LIB19_MAP_2_TITLE = "TODO_COPY_LIB19_MAP_2_TITLE"
    const val TODO_COPY_LIB19_MAP_2_SUB = "TODO_COPY_LIB19_MAP_2_SUB"
    const val TODO_COPY_LIB19_MAP_3_TITLE = "TODO_COPY_LIB19_MAP_3_TITLE"
    const val TODO_COPY_LIB19_MAP_3_SUB = "TODO_COPY_LIB19_MAP_3_SUB"
    const val TODO_COPY_LIB19_WHEN_2_TITLE = "TODO_COPY_LIB19_WHEN_2_TITLE"
    const val TODO_COPY_LIB19_WHEN_2_SUB = "TODO_COPY_LIB19_WHEN_2_SUB"
    const val TODO_COPY_LIB19_WHEN_3_TITLE = "TODO_COPY_LIB19_WHEN_3_TITLE"
    const val TODO_COPY_LIB19_WHEN_3_SUB = "TODO_COPY_LIB19_WHEN_3_SUB"

    /** Steps per module, in order (spec LIB-19 table; boards `Tour1`, `Tour2`, `Tour4`, `TourCast`, `TourWorld`, `TourLr`, `TourMap`, `TourWhen`). */
    fun steps(module: TourModule): List<TourStep> = when (module) {
        TourModule.SCRIPT -> listOf(
            TourStep("명령 칸", "이동 · 검색 · 블록 추가 · 마이크"),
            TourStep("블록을 왼쪽으로", "시간 · 참고 · 속성 → 옆 패널 탭"),
            TourStep(TODO_COPY_LIB19_SCRIPT_3_TITLE, TODO_COPY_LIB19_SCRIPT_3_SUB),
            TourStep("시점 렌즈", "고른 시점까지의 인물 · 관계 · 용어"),
        )
        TourModule.CAST -> listOf(
            TourStep("시점 렌즈", "이 시점까지의 필드 · 관계만"),
            TourStep(TODO_COPY_LIB19_CAST_2_TITLE, TODO_COPY_LIB19_CAST_2_SUB),
            TourStep(TODO_COPY_LIB19_CAST_3_TITLE, TODO_COPY_LIB19_CAST_3_SUB),
        )
        TourModule.WORLD_TIMELINE -> listOf(
            TourStep("공개 시점", "앞에서 쓰이면 ▲ 점검 센터"),
            TourStep(TODO_COPY_LIB19_WORLD_2_TITLE, TODO_COPY_LIB19_WORLD_2_SUB),
            TourStep(TODO_COPY_LIB19_WORLD_3_TITLE, TODO_COPY_LIB19_WORLD_3_SUB),
        )
        TourModule.RECORDER -> listOf(
            TourStep(TODO_COPY_LIB19_RECORDER_1_TITLE, TODO_COPY_LIB19_RECORDER_1_SUB),
            TourStep("연결", "보던 곳 · 넣기 전까지 내용은 그대로"),
            TourStep(TODO_COPY_LIB19_RECORDER_3_TITLE, TODO_COPY_LIB19_RECORDER_3_SUB),
        )
        TourModule.LIBRARY_HIDING -> listOf(
            TourStep(TODO_COPY_LIB19_LIBRARY_1_TITLE, TODO_COPY_LIB19_LIBRARY_1_SUB),
            TourStep(TODO_COPY_LIB19_LIBRARY_2_TITLE, TODO_COPY_LIB19_LIBRARY_2_SUB),
            TourStep(TODO_COPY_LIB19_LIBRARY_3_TITLE, TODO_COPY_LIB19_LIBRARY_3_SUB),
        )
        TourModule.MAP -> listOf(
            TourStep("도구 줄", "이동 · 핀 · 영역 · 경로 · 라벨 · 레이어 · 고른 것만 동작"),
            TourStep(TODO_COPY_LIB19_MAP_2_TITLE, TODO_COPY_LIB19_MAP_2_SUB),
            TourStep(TODO_COPY_LIB19_MAP_3_TITLE, TODO_COPY_LIB19_MAP_3_SUB),
        )
        TourModule.ROUTE_PLACE_TIME -> listOf(
            TourStep("장소와 때", "직전 씬 기준 · 시간대 · 정확/대략 → 이동 점검"),
            TourStep(TODO_COPY_LIB19_WHEN_2_TITLE, TODO_COPY_LIB19_WHEN_2_SUB),
            TourStep(TODO_COPY_LIB19_WHEN_3_TITLE, TODO_COPY_LIB19_WHEN_3_SUB),
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
