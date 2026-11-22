package io.github.lenticualrstone.civilight.feature.map.view

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.app.LargeMapExport
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/** `⋯` of a map screen (feature-local). */
@Serializable
data object MapMoreSheet : SheetKey

/**
 * The menu (WLD-46, WLD-49, WLD-51, and the [MapMenu] rows: `이름`, the format editor, `단계 다시
 * 만들기`, `인물 동선`). The spec names only `⋯ › 지도 내보내기`; the others need a visible way in
 * as well, so they sit in the same menu. `큰 지도 내보내기 | 4K · 8K` starts at the app setting and
 * applies to this export only. [canExport] false leaves the export out (the editor).
 */
@Composable
fun ColumnScope.MapMoreContent(
    canScale: Boolean,
    defaultSize: LargeMapExport,
    exporting: Boolean,
    onScale: () -> Unit,
    onReveal: () -> Unit,
    onExport: (LargeMapExport) -> Unit,
    onClose: () -> Unit,
    rows: List<MapMenuRow> = emptyList(),
    onRow: (MapMenuAction) -> Unit = {},
    canExport: Boolean = true,
) {
    var size by rememberSaveable { mutableIntStateOf(defaultSize.ordinal) }
    SheetHeader(MapCopy.MORE, onClose = onClose)
    SheetScrollBody {
        rows.forEach { row -> ListRow(row.title, subtitle = row.subtitle, onClick = if (row.enabled) ({ onRow(row.action) }) else null) }
        if (canScale) ListRow(MapCopy.SCALE_SHEET, onClick = onScale)
        ListRow(MapCopy.REVEAL_TITLE, onClick = onReveal)
        if (canExport) {
            ListRow(MapCopy.EXPORT, subtitle = MapCopy.EXPORT_SIZE, onClick = if (exporting) null else ({ onExport(LargeMapExport.entries[size]) }))
            SegmentedControl(MapCopy.EXPORT_SIZES, size, { size = it }, Modifier.padding(vertical = 8.dp))
        }
    }
}
