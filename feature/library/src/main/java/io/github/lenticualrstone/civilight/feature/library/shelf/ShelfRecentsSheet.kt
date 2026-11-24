package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/**
 * (0.2.2) `최근 연 파일` pulled up from the 3D views' peek row: the recent files (LIB-05, from the masked
 * [ShelfUi.recents]) and, since the 3D views no longer scroll down to it, `라이브러리 · 프로젝트 밖` with the
 * 라이프 레코더 row. About no project (each card protects the window itself), so it never closes on a re-lock.
 */
@Serializable
data object RecentsSheet : SheetKey

@Composable
internal fun SheetScope.RecentsSheetContent(
    recents: List<RecentCardUi>,
    wide: Boolean,
    recorderSub: String,
    onOpen: (RecentItem) -> Unit,
    onRecorder: () -> Unit,
) {
    val scope = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(title = ShelfCopy.RECENTS, onClose = scope::close, closeLabel = ShelfCopy.CLOSE)
    if (recents.isEmpty()) {
        CivilightText(ShelfCopy.RECENTS_NONE, type.small, Modifier.padding(top = 12.dp), color = colors.ink3)
    } else {
        Box(Modifier.padding(top = 12.dp)) {
            RecentsBlock(recents, wide, 0.dp, label = false) { item ->
                scope.close()
                onOpen(item)
            }
        }
    }
    SectionLabel(ShelfCopy.LIBRARY_SECTION, Modifier.padding(top = 24.dp))
    ListRow(
        title = ShelfCopy.RECORDER,
        subtitle = recorderSub,
        leading = { RecorderTile() },
        trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) },
        onClick = {
            scope.close()
            onRecorder()
        },
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

/** The mic tile of the 라이프 레코더 row: 44 × 44, radius 14, `pri` fill. */
@Composable
internal fun RecorderTile() {
    val colors = CivilightTheme.colors
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.pri), contentAlignment = Alignment.Center) {
        CivilightIcon(IconId.Mic, contentDescription = null, tint = colors.onpri)
    }
}
