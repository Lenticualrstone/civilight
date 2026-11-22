package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * Matching script commands over the command field while typing (SCR-68, board `Search` `명령` rows):
 * `› 에셋`, 44 dp each; a tap runs the command as if it had been submitted.
 */
@Composable
fun CommandSuggestionRows(commands: List<String>, onPick: (String) -> Unit) {
    if (commands.isEmpty()) return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        commands.forEach { command ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .topHairline(colors.line)
                    .pressable(PressAction.Click({ onPick(command) }), role = Role.Button, hitTarget = HitTarget.BUTTON, scaleOnPress = false)
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CivilightText(CHEVRON, type.command, color = colors.ink3, maxLines = 1)
                CivilightText(command, type.command, Modifier.weight(1f), color = colors.ink, maxLines = 1)
            }
        }
    }
}

/** The board's row mark (`› 점검 센터 열기`). */
private const val CHEVRON = "›"
