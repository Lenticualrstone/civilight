package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.map.MapCopy

/**
 * The map tour's targets in the map editor (LIB-19, board `TourMap`; copy in `ModuleTours`): which
 * element each step's hole goes around. `보이는 시점` is a property of a picked pin or region (its
 * panel shows only after a pick), so its hole is the header's `⋯`, whose `시점별 공개` row opens the
 * map's 보이는 시점 (emulator QA 2026-11-23: the card floated with nothing marked).
 *
 * (0.2.0, S-9, WLD-75) The `경로` tool is gone from the tool bar, so the editor has no `경로` step.
 * The step copy lives in `domain:library` (`ModuleTours`), so the ids are read from its titles: while
 * that copy still lists a `경로` step, the editor declares it absent ([absent]) and the tour skips it;
 * once the copy drops it, [REVEAL] moves to 1 by itself.
 */
object MapTourSteps {
    private val titles: List<String> get() = ModuleTours.steps(TourModule.MAP).map { it.title }

    /** `도구 줄`: the whole tool bar. */
    const val TOOLBAR: Int = 0

    /** `보이는 시점`: the header's `⋯` (see above), always on screen. -1 if the copy has no such step. */
    val REVEAL: Int get() = titles.indexOf(MapCopy.REVEAL_POINT)

    /** Steps the editor has no target for: every step but [TOOLBAR] and [REVEAL] (the old `경로`). */
    fun absent(): List<Int> = titles.indices.filter { it != TOOLBAR && it != REVEAL }
}
