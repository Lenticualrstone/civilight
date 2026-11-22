package io.github.lenticualrstone.civilight.feature.map.common

/**
 * Which floating bar a map screen shows, as pure functions (the screens only draw the answer through [MapBar]). A bar kind is
 * also the `AnimatedContent` key, so two states of one kind update in place and only a change of kind animates.
 */
object MapBars {
    const val MOVE = "move"
    const val PLACE = "place"
    const val TOOLS = "tools"
    const val SHAPE = "shape"
    const val LAYERS = "layers"
    const val PROPS = "props"
    const val STAMPS = "stamps"
    const val STEP = "step"
    const val PICK = "pick"
    const val SPLIT = "split"
    const val PANEL = "panel"

    /** 지도 보기: `핀 옮기기` first, then the cover place sheet (inner draws the place as a side card), else the tools. */
    fun view(moving: Boolean, place: Boolean, inner: Boolean): String = when {
        moving -> MOVE
        place && !inner -> PLACE
        else -> TOOLS
    }

    /**
     * 지도 편집, the bar above the tool bar. Inner has only the two step bars (the panels sit in the side panel); cover also
     * has the layer card, the property card and the stamp palette.
     */
    fun edit(moving: Boolean, shape: Boolean, layerTool: Boolean, props: Boolean, stampPalette: Boolean, inner: Boolean): String? = when {
        moving -> MOVE
        shape -> SHAPE
        inner -> null
        layerTool -> LAYERS
        props -> PROPS
        stampPalette -> STAMPS
        else -> null
    }

    /** 노드 지도: the step bar (`이동` or `연결`), and on the cover the layer card or the property card. */
    fun node(step: Boolean, layerTool: Boolean, props: Boolean, inner: Boolean): String? = when {
        step -> STEP
        inner -> null
        layerTool -> LAYERS
        props -> PROPS
        else -> null
    }
}
