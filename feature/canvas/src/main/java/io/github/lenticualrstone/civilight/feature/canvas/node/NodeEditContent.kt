package io.github.lenticualrstone.civilight.feature.canvas.node

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/** Node edit as a cover bottom sheet (CNV-10: "커버 화면: … 노드 편집은 옆 팝오버 대신 아래 시트"). */
@Serializable
data class NodeEditSheet(val projectKey: Key, val canvasKey: Key, val nodeKey: Key) : SheetKey

/**
 * Rows of node edit, shared by the cover sheet and the inner popover: the memo text or 묶음 title
 * field, then the buttons — open the referenced item (ghost) and take the node off this canvas.
 */
@Composable
fun ColumnScope.NodeEditRows(
    view: NodeEditView,
    compact: Boolean,
    onText: (String) -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = CivilightTheme.colors
    if (view.text != null) {
        // Held here while the surface is open: the document value arrives only after the store write,
        // and feeding it back would snap the field between keystrokes (Hangul composition).
        var text by rememberSaveable(view.nodeKey) { mutableStateOf(view.text) }
        CivilightTextField(
            value = text,
            onValueChange = {
                text = it
                onText(it)
            },
            modifier = Modifier.padding(top = if (compact) 8.dp else 12.dp),
            singleLine = false,
            enabled = !view.readOnly,
        )
    }
    if (view.readOnly) CivilightText(CanvasCopy.READ_ONLY, CivilightTheme.type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
    SheetActions {
        view.openLabel?.let { label ->
            CivilightButton(
                text = label,
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                variant = if (compact) ButtonVariant.COMPACT_GHOST else ButtonVariant.GHOST,
            )
        }
        if (!view.readOnly) {
            CivilightButton(
                text = view.removeLabel,
                onClick = onRemove,
                modifier = Modifier.weight(1f),
                variant = if (compact) ButtonVariant.COMPACT_GHOST else ButtonVariant.GHOST,
            )
        }
    }
}
