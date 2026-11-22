package io.github.lenticualrstone.civilight.feature.script.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/**
 * `다른 블록에 잇기` (SCR-43) / `블록에 잇기` (SCR-42) for [recordKey]. Feature-local for now; story's
 * check center and world's `원고에서 고르기` need a shared key (see the status file's Requests).
 */
@Serializable
data class RelinkSheetKey(val projectKey: Key, val recordKey: Key) : SheetKey

/**
 * The candidate list (not drawn by the boards; layout `[PROPOSED]`): the record's title, then one
 * row per block, `하르트 #043` over the block's first line. A tap anchors the record there.
 */
@Composable
fun SheetScope.RelinkSheet(model: RelinkModel?, readOnly: Boolean, onPick: (blockKey: Key) -> Unit) {
    SheetHeader(title = model?.title.orEmpty(), onClose = ::close, subtitle = model?.subject)
    model ?: return
    SheetScrollBody(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        model.rows.forEach { row ->
            BlockLineRow(row.line, enabled = !readOnly) {
                this@RelinkSheet.close()
                onPick(row.blockKey)
            }
        }
    }
}

/** A block as a list row: label `small` `ink3` over its text, 44 dp or more, `line` top. */
@Composable
fun BlockLineRow(line: BlockLine, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val tap = onClick?.let { Modifier.pressable(PressAction.Click(it), enabled = enabled, role = Role.Button, scaleOnPress = false) } ?: Modifier
    Column(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(tap)
            .heightIn(min = 52.dp)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CivilightText(line.label, type.small, color = colors.ink3, maxLines = 1)
        CivilightText(line.text, type.command, color = colors.ink, maxLines = 2)
    }
}
