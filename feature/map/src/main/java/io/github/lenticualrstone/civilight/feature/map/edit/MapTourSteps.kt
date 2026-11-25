package io.github.lenticualrstone.civilight.feature.map.edit

/**
 * The map tour's targets in the map editor (LIB-19, board `TourMap`; copy in `ModuleTours`): which
 * element each step's hole goes around. `보이는 시점` is a property of a picked pin or region (its
 * panel shows only after a pick), so its hole is the header's `⋯`, whose `시점별 공개` row opens the
 * map's 보이는 시점 (emulator QA 2026-11-23: the card floated with nothing marked).
 */
object MapTourSteps {
    /** `도구 줄`: the whole tool bar. */
    const val TOOLBAR: Int = 0

    /** `경로`: the `경로` cell of the tool bar. */
    const val ROUTE: Int = 1

    /** `보이는 시점`: the header's `⋯` (see above), always on screen. */
    const val REVEAL: Int = 2

    /** The tour step the tool bar cell of [tool] is the hole of, or null. */
    fun stepOfTool(tool: EditTool): Int? = if (tool == EditTool.ROUTE) ROUTE else null
}
