package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.startHairline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler

/**
 * DS-24 side panel (inner screens): glass aside on the right edge, full height, [width] 290 at the 800
 * reference; padding 20 top, 22 sides; 1 dp `line` inset on its left edge. Header: [tabs] (gap 16) at the
 * left, `닫기` (`label` 12 sp) at the right. System back closes it (via [LocalBackHandler]).
 * Place it as a sibling above the glass source (e.g. in [GlassScaffold]'s overlay).
 */
@Composable
fun SidePanel(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onSelectTab: (Int) -> Unit = {},
    closeLabel: String = "닫기",
    width: Dp = CivilightTheme.sizes.sidePanel,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    LocalBackHandler.current(true, onClose)
    Box(modifier.width(width).fillMaxHeight()) {
        GlassBackground(GlassSurface.PANEL, Modifier.matchParentSize())
        OnGlass {
            Column(
                Modifier
                    .fillMaxHeight()
                    .startHairline(colors.line)
                    .padding(top = 20.dp, start = 22.dp, end = 22.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (tabs.isNotEmpty()) {
                        CivilightTabs(tabs, selectedTab, onSelectTab, Modifier.weight(1f), gap = TabGap.Panel)
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    TextAction(closeLabel, onClose, style = CivilightTheme.type.label.merge(TextStyle(fontSize = 12.sp)))
                }
                content()
            }
        }
    }
}

/** A card inside a panel: `tint` fill, radius 14, padding 14. */
@Composable
fun PanelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CivilightTheme.colors.tint)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
