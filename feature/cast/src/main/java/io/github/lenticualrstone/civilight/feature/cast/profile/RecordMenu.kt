package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** The record `⋯` (CST-39, CST-40, CST-45; spec §6 Q24 default: the record menu). Feature-local. */
@Serializable
data class RecordMenuSheet(val projectKey: Key, val characterKey: Key) : SheetKey

/**
 * The record `⋯` menu (§6 Q24 default; no board draws it): `숨기기` / `숨김 풀기` (CST-39), `지우기`
 * (CST-40), `합치기` (CST-45). Rows only for actions that exist.
 */
@Composable
fun SheetScope.RecordMenuContent(name: String, hidden: Boolean, onHide: () -> Unit, onDelete: () -> Unit, onMerge: () -> Unit) {
    SheetHeader(name, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        ListRow(if (hidden) CastUiCopy.BTN_UNHIDE else CastUiCopy.BTN_HIDE, onClick = onHide, compact = true)
        ListRow(CastUiCopy.BTN_DELETE, onClick = onDelete, compact = true)
        ListRow(CastUiCopy.BTN_MERGE, onClick = onMerge, compact = true)
    }
}
