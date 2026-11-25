package io.github.lenticualrstone.civilight.feature.script.layout

/**
 * Where the script's parts sit (SCR-01, SCR-03), from the width the screen actually gets (not the
 * window class: in the `점검 센터 | Script` pair the script is a narrow detail pane). All values in dp.
 *
 * - Cover (narrower than [WIDE_MIN]): the manuscript fills the width, no side panel.
 * - Inner (r3, decision 0.2.3 · r3, boards `LScript` / `PScript`): a reading column of at most 560 from x 40;
 *   the side panel at the right edge, full height, [PANEL_LANDSCAPE] wide when the frame is wider than tall
 *   and [PANEL_PORTRAIT] otherwise. Header and bottom bar span the manuscript area only; the column narrows
 *   beside the panel, so no line sits under it.
 * - Panel closed by the writer (`LScriptShut` / `PScriptShut`): a [RAIL] dp rail with its 1 dp hairline stays
 *   at the right edge for the reopen buttons ([rail]), and the manuscript takes the rest. A panel hidden for a
 *   while (select mode, tabletop) leaves no rail: the manuscript takes the full width.
 */
data class ScriptLayout(
    val wide: Boolean,
    /** The side panel is drawn. */
    val panel: Boolean,
    /** Width of the manuscript area (bars and list), left of the panel or the rail. */
    val areaWidth: Float,
    /** Start of the reading column inside the area; 0 on cover. */
    val columnStart: Float,
    /** Width of the reading column (the whole area on cover). */
    val columnWidth: Float,
    /** Width of the side panel while [panel] (CNV-47: the writer's own ratio; the r3 width by default). */
    val panelWidth: Float = PANEL_PORTRAIT,
    /** The closed panel's rail is drawn at the right edge ([RAIL] + 1 dp hairline). */
    val rail: Boolean = false,
) {
    /**
     * The same layout with the manuscript kept at the width of [other] (the panel is still open): used
     * while a panel is sliding in, so the text re-wraps once, when the panel has arrived (SCR-66).
     */
    fun keepingAreaOf(other: ScriptLayout): ScriptLayout = copy(areaWidth = other.areaWidth, columnStart = other.columnStart, columnWidth = other.columnWidth)

    companion object {
        const val WIDE_MIN = 600f

        /** r3 `LScript`: the panel beside a landscape frame (933 × 704: editor 613 · panel 320). */
        const val PANEL_LANDSCAPE = 320f

        /** r3 `PScript`: the panel beside a portrait frame (704 × 933: editor 432 · panel 272). */
        const val PANEL_PORTRAIT = 272f

        /** r3 closed panel: the rail that keeps the reopen buttons off the back-gesture edge. */
        const val RAIL = 56f

        /** Rail plus its hairline: what the manuscript gives up while the panel is closed (board `right: 57px`). */
        const val RAIL_SPACE = RAIL + 1f

        const val COLUMN_START = 40f
        const val COLUMN_MAX = 560f

        /** Space kept between the column and the panel's hairline. */
        const val PANEL_GAP = 16f

        /** Narrowest column worth keeping the panel for (280 dp supports Z Fold 8 inner portrait at 480 dpi / 616 dp). */
        const val COLUMN_MIN = 280f

        /**
         * Where the manuscript stays while the panel slides in (SCR-66, one re-wrap): the last layout without the panel
         * ([last], drawn [lastWidth] wide) when it was for this [width] (beside the rail, or full width after select
         * mode or tabletop); otherwise (an unfold, a resize) the full width.
         */
        fun beforePanel(last: ScriptLayout?, lastWidth: Float?, width: Float, height: Float): ScriptLayout =
            if (last != null && !last.panel && lastWidth == width) last else of(width, false, height = height, hidden = true)

        /**
         * The panel slides fully out of the frame (r3 toggle motion) when it goes to or comes from the rail: [other]
         * is the layout before it arrives (enter) or the one it leaves for (exit).
         */
        fun slidesFromRail(other: ScriptLayout): Boolean = other.rail

        /** The r3 panel width of a [width] × [height] frame: landscape 320, portrait (or square) 272. */
        fun panelWidthFor(width: Float, height: Float): Float = if (width > height) PANEL_LANDSCAPE else PANEL_PORTRAIT

        /** The panel fits beside a column of at least [COLUMN_MIN] (otherwise neither panel nor rail is drawn). */
        fun fitsPanel(width: Float, height: Float): Boolean =
            width >= WIDE_MIN && width - panelWidthFor(width, height) - COLUMN_START - PANEL_GAP >= COLUMN_MIN

        /**
         * [panelOpen]: the writer's choice (the seam tab and the rail button). [hidden]: the panel is out of the way
         * for a while (select mode, tabletop), so no rail either. [area] (CNV-47) is the manuscript width the writer
         * chose with the pane split handle; it only counts while the panel is drawn. [height] picks the r3 width
         * ([panelWidthFor]); without it the frame counts as portrait.
         */
        fun of(width: Float, panelOpen: Boolean, area: Float? = null, height: Float = width, hidden: Boolean = false): ScriptLayout {
            val base = panelWidthFor(width, height)
            if (width < WIDE_MIN) return ScriptLayout(false, false, width, 0f, width, base)
            val fits = fitsPanel(width, height)
            val panel = panelOpen && !hidden && fits
            val rail = !panelOpen && !hidden && fits
            val chosen = if (panel && area != null) area.coerceIn(COLUMN_START + PANEL_GAP, width - 1f) else null
            val areaWidth = chosen ?: when {
                panel -> width - base
                rail -> width - RAIL_SPACE
                else -> width
            }
            val room = areaWidth - COLUMN_START - (if (panel) PANEL_GAP else 0f)
            return ScriptLayout(true, panel, areaWidth, COLUMN_START, room.coerceIn(0f, COLUMN_MAX), if (panel) width - areaWidth else base, rail)
        }
    }
}
