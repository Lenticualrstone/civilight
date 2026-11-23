package io.github.lenticualrstone.civilight.feature.cast.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.IconLabelButton
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * `목록 · 관계도` of the character list and the relationship map (CST-01, CST-33). Icon list row `인물 목록`
 * (class b, `B10`): icon `list` + `목록`, icon `graph` + `관계도`, each a toggle that reads its own name.
 * [selected] 0 is the list, 1 the map; tapping the other one calls [onSelect].
 */
@Composable
fun CastViewModeRow(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        MODES.forEachIndexed { i, spec ->
            val on = i == selected
            IconLabelButton(
                icon = spec.icon!!,
                label = spec.text!!,
                onClick = { if (!on) onSelect(i) },
                contentDescription = spec.accessibilityName,
                tint = if (on) colors.acc else colors.ink3,
                labelColor = if (on) colors.ink else colors.ink3,
                labelStyle = CivilightTheme.type.label,
                on = on,
            )
        }
    }
}

/** The two cells in order: `목록`, `관계도`. */
private val MODES: List<IconButtonSpec> = listOf(IconButtonSpec.CAST_LIST, IconButtonSpec.CAST_GRAPH)
