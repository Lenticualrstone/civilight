package io.github.lenticualrstone.civilight.feature.map.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.TabGap
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.ui.window.PanelCollapseState
import io.github.lenticualrstone.civilight.ui.window.PanelCopy
import io.github.lenticualrstone.civilight.ui.window.collapsedPanel
import io.github.lenticualrstone.civilight.ui.window.rememberPanelCollapseState

/**
 * The map's floating side panel on the inner screen (r3 boards `MapEdit`, `MapNode`, `MapDetail`): a glass card
 * [WIDTH] dp wide, radius [RADIUS], [EDGE] dp in from the right under the top bar, with its tabs and an icon
 * `패널 닫기` on the first row. Closed and reopened by a visible button (0.2.3 decision, `PanelCollapse`): the
 * closed panel leaves a 44 dp glass `패널 열기` button in its corner. The choice is kept per app in
 * `AppSettings.panelClosed[mapPanel]`. Full motion slides it out to the right on `panelSlide`; reduced motion
 * fades it (120 ms). JVM-tested numbers in [MapAsideRules].
 */
object MapAsideRules {
    const val WIDTH: Float = 252f

    /** The wider aside of `인물 동선` and the generated-map view (boards `MapTrack`, `MapSoft`: 280). */
    const val WIDE: Float = 280f
    const val RADIUS: Float = 20f
    const val EDGE: Float = 16f

    /** Under the top bar: the board's `top: 120` is the 104 dp bar plus this gap. */
    const val TOP_GAP: Float = 16f

    /** Board padding `6 14 16 18` (top, end, bottom, start). */
    const val PAD_TOP: Float = 6f
    const val PAD_END: Float = 14f
    const val PAD_BOTTOM: Float = 16f
    const val PAD_START: Float = 18f

    /** The reopen button (`패널 열기`), 44 dp: the touch-target minimum. */
    const val REOPEN: Float = 44f

    /** How far the panel has slid out to the right (0 open … width + edge closed). */
    fun shift(open: Float, width: Float = WIDTH): Float = (1f - open.coerceIn(0f, 1f)) * (width + EDGE)

    const val MODULE: String = PaneSplit.Modules.MAP_PANEL
}

/** The collapse state of the map's side panel (one for every map screen). */
@Composable
fun rememberMapAsideState(): PanelCollapseState = rememberPanelCollapseState(MapAsideRules.MODULE)

/**
 * Draws the panel (or, closed, its reopen button) at the top end of a full-size box. [onBack] (when not null) is
 * what the system back does while something is picked, open or closed (a closed panel's card floats); closing is the button's.
 */
@Composable
fun MapAside(
    state: PanelCollapseState,
    tabs: List<String>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    width: Float = MapAsideRules.WIDTH,
    /** Stretch to the height it is given (the `인물 동선` aside runs from the top bar to the bottom). */
    fillHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(MapAsideRules.RADIUS.dp)
    // Back deselects whether the panel is open or closed (closed, the picked item's card floats over the tool bar),
    // so the editor is never left mid-selection.
    LocalBackHandler.current(onBack != null) { onBack?.invoke() }
    Box(modifier.padding(end = MapAsideRules.EDGE.dp)) {
        if (!state.gone) {
            Box(
                Modifier
                    .width(width.dp)
                    .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                    .graphicsLayer {
                        translationX = MapAsideRules.shift(state.panelIn, width) * density
                        alpha = state.panelAlpha
                    }
                    .collapsedPanel(state.closed)
                    .blockMapTaps(),
            ) {
                GlassBackground(GlassSurface.PANEL, Modifier.matchParentSize(), shape = shape)
                Box(Modifier.matchParentSize().insetOutline(colors.line, 1.dp, shape))
                OnGlass {
                    Column(
                        (if (fillHeight) Modifier.fillMaxHeight() else Modifier).padding(
                            start = MapAsideRules.PAD_START.dp,
                            end = MapAsideRules.PAD_END.dp,
                            top = MapAsideRules.PAD_TOP.dp,
                            bottom = MapAsideRules.PAD_BOTTOM.dp,
                        ),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (tabs.isNotEmpty()) CivilightTabs(tabs, selectedTab, onSelectTab, Modifier.weight(1f), gap = TabGap.Panel)
                            else Box(Modifier.weight(1f))
                            CivilightIconButton(IconId.X, PanelCopy.CLOSE, { state.setClosed(true) }, Modifier.offset(x = 8.dp))
                        }
                        content()
                    }
                }
            }
        }
        if (state.closed) {
            val reopenShape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(MapAsideRules.REOPEN.dp)
                    .graphicsLayer { alpha = (1f - state.open).coerceIn(0f, 1f) }
                    .blockMapTaps(),
                contentAlignment = Alignment.Center,
            ) {
                GlassBackground(GlassSurface.PANEL, Modifier.matchParentSize(), shape = reopenShape)
                Box(Modifier.matchParentSize().insetOutline(colors.line, 1.dp, reopenShape))
                CivilightIconButton(IconId.Panel, PanelCopy.OPEN, { state.setClosed(false) }, tint = colors.ink)
            }
        }
    }
}
