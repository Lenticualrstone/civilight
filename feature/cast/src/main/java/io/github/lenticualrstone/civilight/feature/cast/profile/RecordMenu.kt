package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** The record `⋯` (CST-39, CST-40, CST-45; spec §6 Q24 default: the record menu). Feature-local. */
@Serializable
data class RecordMenuSheet(val projectKey: Key, val characterKey: Key) : SheetKey

/** The rows of the record `⋯`, in order. [destructive] rows go last, under their own divider. */
enum class RecordMenuRow(val destructive: Boolean = false) {
    HIDE, MERGE, DELETE(destructive = true);

    companion object {
        /** `숨기기` / `숨김 풀기`, `합치기`, then `지우기` (always last). */
        val order: List<RecordMenuRow> = entries.sortedBy { it.destructive }

        fun label(row: RecordMenuRow, hidden: Boolean): String = when (row) {
            HIDE -> if (hidden) CastUiCopy.BTN_UNHIDE else CastUiCopy.BTN_HIDE
            MERGE -> CastUiCopy.BTN_MERGE
            DELETE -> CastUiCopy.BTN_DELETE
        }
    }
}

/**
 * The record `⋯` menu (§6 Q24 default): `숨기기` / `숨김 풀기` (CST-39), `합치기` (CST-45), and `지우기` (CST-40)
 * last under its own divider in `warn` with the trash icon, as the story `⋯` draws `씬 지우기` (MenuSheets).
 */
@Composable
fun SheetScope.RecordMenuContent(name: String, hidden: Boolean, onHide: () -> Unit, onDelete: () -> Unit, onMerge: () -> Unit) {
    SheetHeader(name, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        RecordMenuRow.order.forEach { row ->
            RecordMenuRowView(
                label = RecordMenuRow.label(row, hidden),
                destructive = row.destructive,
                onClick = when (row) {
                    RecordMenuRow.HIDE -> onHide
                    RecordMenuRow.MERGE -> onMerge
                    RecordMenuRow.DELETE -> onDelete
                },
            )
        }
    }
}

/** A ⋯ row as the story menu draws it: min 52, `line` top, label medium; destructive: 12 dp gap + line, trash, `warn`. */
@Composable
private fun RecordMenuRowView(label: String, destructive: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    if (destructive) {
        // The shared ⋯ `지우기` row (designsystem `MenuDeleteRow`).
        io.github.lenticualrstone.civilight.designsystem.component.MenuDeleteRow(label, onClick)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = onClick, role = Role.Button, scaleOnPress = false)
            .heightIn(min = 52.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (destructive) CivilightIcon(IconId.Trash, contentDescription = null, tint = colors.warn)
        CivilightText(
            label,
            type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Medium)),
            color = if (destructive) colors.warn else colors.ink,
            maxLines = 1,
        )
    }
}

private val DIVIDER_GAP = 12.dp
