package io.github.lenticualrstone.civilight.feature.script.panel

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * The lower half in tabletop posture (SCR-69): the panel tabs `시간 · 참고 · 속성 · 모듈` with the
 * shown tab's body (same bodies and per-tab scroll as the side panel and the sheet), then the
 * script's command area ([commandArea]) at the bottom, where the keyboard comes up.
 */
@Composable
fun TabletopPanel(
    tab: PanelTab,
    onSelectTab: (PanelTab) -> Unit,
    scrollOf: (PanelTab) -> ScrollState,
    modifier: Modifier = Modifier,
    commandArea: @Composable () -> Unit,
    content: @Composable ColumnScope.(PanelTab) -> Unit,
) {
    val colors = CivilightTheme.colors
    val tabs = PanelTab.BUILT
    val shown = tab.takeIf { it in tabs } ?: tabs.first()
    val motion = CivilightTheme.motion
    Column(modifier.fillMaxSize().background(colors.bg).topHairline(colors.line)) {
        CivilightTabs(
            tabs = tabs.map { it.label },
            selectedIndex = tabs.indexOf(shown),
            onSelect = { onSelectTab(tabs[it]) },
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(shown, animationSpec = motion.spec(CivilightMotion.standard), label = "tabletopTab") { t ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(scrollOf(t)).padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { content(t) }
            }
        }
        commandArea()
    }
}
