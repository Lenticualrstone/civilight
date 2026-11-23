package io.github.lenticualrstone.civilight.feature.recorder.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.domain.recorder.settings.RecorderSettingsScreen
import io.github.lenticualrstone.civilight.feature.recorder.RecorderUiCopy
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import kotlinx.coroutines.launch

/**
 * `설정 · 라이프 레코더` (LRC-44, board `LrSettings`): head small `설정`, title `라이프 레코더`, back;
 * section `받아쓰기` with the radios `기기 안` · `클라우드`, section `녹음` with the switches
 * `원본 음성 보관` · `녹음 중 빛 띠` · `보던 곳 연결` (`잠금 화면에서 녹음` hidden in 0.2.0, LRC-48). Cover: 24 dp margins;
 * inner screens: the same list in the 560 dp reading column (LIB settings layout). Writes go to the
 * settings store in the app scope; the rows follow `deps.settings`.
 */
@Composable
fun RecorderSettingsContent(deps: FeatureDeps) {
    val settings by deps.settings.collectAsState()
    val rows = remember(settings) { RecorderSettingsRows.of(settings) }
    fun set(id: RecorderSettingId, on: Boolean) {
        deps.appScope.launch { deps.stores.settings.update { RecorderSettingsRows.apply(it, id, on) } }
    }
    CivilightScreen(
        title = RecorderSettingsScreen.TITLE,
        smallLine = RecorderSettingsScreen.SMALL,
        back = IconAction(RecorderUiCopy.BACK) { deps.navigator.pop() },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                for ((index, view) in rows.withIndex()) {
                    val row = view.row
                    row.section?.let { SectionLabel(it, Modifier.padding(top = if (index == 0) 0.dp else 20.dp, bottom = 6.dp)) }
                    if (row.radio) {
                        RadioRow(title = row.title, selected = row.on, onClick = { set(view.id, true) }, subtitle = row.sub)
                    } else {
                        SwitchRow(title = row.title, checked = row.on, onCheckedChange = { set(view.id, it) }, subtitle = row.sub)
                    }
                }
            }
        }
    }
}
