package io.github.lenticualrstone.civilight.feature.map.track

/**
 * The `인물 동선 · 장소와 때` tour's targets on 인물 동선 (LIB-19, board `TourWhen`; copy in `ModuleTours`):
 * which element each step's hole goes around (emulator QA 2026-11-23: every card floated).
 * - `장소와 때`: the first `씬 순서` row (`1-2 | 하층 거리 | 씬 1 · 1일째 새벽`: a scene's place and 때);
 *   while no row is shown (no character, another panel tab) its fallback is the panel's `동선 · 장소 ·
 *   레이어` tabs, always on screen.
 * - `인물 동선`: the episode scrubber with `재생`, which walks the stops in scene order; same fallback.
 * The tour waits while there is no map to draw ([hold]): the screen then shows nothing to point at.
 */
object TrackTourSteps {
    /** `장소와 때`. */
    const val PLACE_TIME: Int = 0

    /** `인물 동선`. */
    const val TRACK: Int = 1

    const val COUNT: Int = 2

    /**
     * The steps the panel's tabs back up: every step. At cover width the rows and the scrubber share one
     * scrolling card, so either can sit below its visible part; the tabs stay at its top.
     */
    val TABS_FALLBACK: List<Int> = listOf(PLACE_TIME, TRACK)

    /** Whether the tour waits: the track is still loading or the project has no map ([TrackUi] null). */
    fun hold(ui: TrackUi?): Boolean = ui == null
}
