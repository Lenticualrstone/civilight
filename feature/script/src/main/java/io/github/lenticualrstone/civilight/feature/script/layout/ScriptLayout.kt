package io.github.lenticualrstone.civilight.feature.script.layout

/**
 * Where the script's parts sit (SCR-01, SCR-03), from the width the screen actually gets (not the
 * window class: in the `점검 센터 | Script` pair the script is a narrow detail pane). All values in dp.
 *
 * - Cover (narrower than [WIDE_MIN]): the manuscript fills the width, no side panel.
 * - Inner: a reading column of at most 560 from x 40; the side panel (290, right edge, full height)
 *   when open. Header and bottom bar span the manuscript area only (board `Wide`: header `right:
 *   312`, bottom bar `right: 314`). Board `Wide` lets the 560 column run 90 under the panel on an
 *   800 screen; here the column narrows instead, so no line sits under the panel (`ED`).
 *   Closing the panel gives the manuscript the full width and keeps the column (SCR-03).
 */
data class ScriptLayout(
    val wide: Boolean,
    /** The side panel is drawn. */
    val panel: Boolean,
    /** Width of the manuscript area (bars and list), left of the panel. */
    val areaWidth: Float,
    /** Start of the reading column inside the area; 0 on cover. */
    val columnStart: Float,
    /** Width of the reading column (the whole area on cover). */
    val columnWidth: Float,
    /** Width of the side panel while [panel] (CNV-47: the writer's own ratio; [PANEL_WIDTH] by default). */
    val panelWidth: Float = PANEL_WIDTH,
) {
    /**
     * The same layout with the manuscript kept at the width of [other] (the panel is still open): used
     * while a panel is sliding in, so the text re-wraps once, when the panel has arrived (SCR-66).
     */
    fun keepingAreaOf(other: ScriptLayout): ScriptLayout = copy(areaWidth = other.areaWidth, columnStart = other.columnStart, columnWidth = other.columnWidth)

    companion object {
        const val WIDE_MIN = 600f
        const val PANEL_WIDTH = 290f
        const val COLUMN_START = 40f
        const val COLUMN_MAX = 560f

        /** Space kept between the column and the panel's hairline. */
        const val PANEL_GAP = 16f

        /** Narrowest column worth keeping the panel for; below it the panel is not drawn. */
        const val COLUMN_MIN = 320f

        /**
         * [area] (CNV-47) is the manuscript width the writer chose with the pane split handle; it only
         * counts while the panel is drawn. Without it the area is what is left of the 290 dp panel.
         */
        fun of(width: Float, panelOpen: Boolean, area: Float? = null): ScriptLayout {
            if (width < WIDE_MIN) return ScriptLayout(false, false, width, 0f, width)
            val fitsPanel = width - PANEL_WIDTH - COLUMN_START - PANEL_GAP >= COLUMN_MIN
            val panel = panelOpen && fitsPanel
            val chosen = if (panel && area != null) area.coerceIn(COLUMN_START + PANEL_GAP, width - 1f) else null
            val areaWidth = chosen ?: if (panel) width - PANEL_WIDTH else width
            val room = areaWidth - COLUMN_START - (if (panel) PANEL_GAP else 0f)
            return ScriptLayout(true, panel, areaWidth, COLUMN_START, room.coerceIn(0f, COLUMN_MAX), if (panel) width - areaWidth else PANEL_WIDTH)
        }
    }
}
