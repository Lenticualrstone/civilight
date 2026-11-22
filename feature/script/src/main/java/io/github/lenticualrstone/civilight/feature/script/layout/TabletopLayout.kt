package io.github.lenticualrstone.civilight.feature.script.layout

import io.github.lenticualrstone.civilight.ui.window.FoldPosture

/**
 * Half-folded postures (SCR-69, P2; board `FoldSpec` rules only, no drawing yet). All values in dp.
 * - Tabletop: the manuscript (with its header) on the upper half; the panel tabs and the command
 *   field on the lower half, where the keyboard also comes up; nothing tappable within
 *   [HINGE_CLEARANCE] of the fold.
 * - Book: the two panes of SCR-03 with the fold as their border; the normal inner layout already
 *   draws them, so no split is returned.
 * Without hinge bounds from the window the fold is taken at half the height.
 */
data class TabletopLayout(
    /** Height of the upper half (manuscript and header). */
    val upper: Float,
    /** Empty band around the fold. */
    val gap: Float,
    /** Height of the lower half (panel tabs and the command field). */
    val lower: Float,
) {
    companion object {
        /** Distance kept free of controls on each side of the fold. */
        const val HINGE_CLEARANCE = 12f

        /** Below this height a half is too small to split (a landscape cover screen). */
        const val MIN_HEIGHT = 480f

        fun of(posture: FoldPosture, height: Float, foldAt: Float? = null): TabletopLayout? {
            if (posture != FoldPosture.TABLETOP || height < MIN_HEIGHT) return null
            val fold = (foldAt ?: height / 2f).coerceIn(HINGE_CLEARANCE, height - HINGE_CLEARANCE)
            val upper = fold - HINGE_CLEARANCE
            val lower = height - fold - HINGE_CLEARANCE
            return TabletopLayout(upper, HINGE_CLEARANCE * 2, lower)
        }
    }
}
