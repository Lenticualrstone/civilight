package io.github.lenticualrstone.civilight.feature.story.check

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.checks.Highlight
import io.github.lenticualrstone.civilight.domain.story.checks.Marker

/**
 * One item row (STO-39 anatomy, board `Check`): marker + kind in `warn` (▲) or `ink3` (◇) with the
 * tag, the location right-aligned (tap = the script there), the evidence line in the serif with the
 * hit underlined, a small detail line, and the buttons in one row (first `ink`, then `ink2`, last
 * `ink3`). [nested] rows (`◇ 공개 전 후보` under its `▲`) are indented with a dashed top line.
 */
@Composable
fun CheckItemRow(
    result: CheckResult,
    buttons: List<FixAction>,
    onLocation: (() -> Unit)?,
    onButton: (FixAction) -> Unit,
    nested: Boolean = false,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val markerColor = if (result.marker == Marker.WARNING) colors.warn else colors.ink3
    Column(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line, dashed = nested)
            .selectedRow(selected)
            .padding(start = if (nested) 16.dp else 0.dp, top = 14.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightText(result.title, type.label, color = markerColor, maxLines = 1)
            result.tag?.let { CivilightText(it, type.label.merge(TextStyle(fontWeight = FontWeight.Bold)), Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1) }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                result.location?.let { location ->
                    if (onLocation != null) {
                        TextAction(location, onLocation, color = colors.ink2, style = type.small)
                    } else {
                        CivilightText(location, type.small, color = colors.ink3, maxLines = 1)
                    }
                }
            }
        }
        result.body?.let { BodyLine(it, result.highlight) }
        result.detail?.let { CivilightText(it, type.small, color = colors.ink3) }
        result.flag?.let { CivilightText(it, type.small, color = colors.ink3) }
        if (buttons.isNotEmpty()) ButtonsRow(buttons, onButton)
    }
}

/**
 * The row whose position the right pane shows (`TwoCheck`): `sel` fill with the 2 dp `acc` bar
 * (DS-40 selection), drawn 10 dp past the row's sides.
 */
@Composable
fun Modifier.selectedRow(selected: Boolean): Modifier {
    val colors = CivilightTheme.colors
    return selectedRowBackground(selected, colors.sel, colors.acc)
}

/** A folded group (`◇ 근거 지워진 변화 2`, `잿불의 서약 · ▲ 3`): title, count, and its open / closed mark. */
@Composable
fun CheckGroupRow(title: String, warning: Boolean, expanded: Boolean, onToggle: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = onToggle, scaleOnPress = false)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(title, CivilightTheme.type.label, Modifier.weight(1f), color = if (warning) colors.warn else colors.ink3, maxLines = 1)
        // Plus <-> Minus cross-fade on `standard` (motion audit, feature:story 2).
        val spec = LocalMotion.current.spec<Float>(CivilightMotion.standard)
        Crossfade(expanded, animationSpec = spec, label = "groupMark") { open ->
            CivilightIcon(if (open) IconId.Minus else IconId.Plus, contentDescription = null, size = 16.dp, tint = colors.ink3)
        }
    }
}

@Composable
fun ButtonsRow(buttons: List<FixAction>, onButton: (FixAction) -> Unit) {
    val colors = CivilightTheme.colors
    Row(Modifier.fillMaxWidth().padding(start = 0.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        buttons.forEachIndexed { i, action ->
            val color = when {
                i == 0 -> colors.ink
                i == buttons.lastIndex -> colors.ink3
                else -> colors.ink2
            }
            TextAction(action.label, { onButton(action) }, color = color)
        }
    }
}

/** The evidence line in the reading serif with the hit underlined (`세라: “[오리지늄]은 타지 않아.”`). */
@Composable
private fun BodyLine(text: String, highlight: Highlight?) {
    val colors = CivilightTheme.colors
    val style = CivilightTheme.type.line.merge(TextStyle(color = colors.ink))
    BasicText(annotated(text, highlight, colors.ink), style = style)
}

private fun annotated(text: String, highlight: Highlight?, ink: androidx.compose.ui.graphics.Color): AnnotatedString = buildAnnotatedString {
    append(text)
    val h = highlight ?: return@buildAnnotatedString
    if (h.start in 0..text.length && h.end in h.start..text.length) {
        addStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = ink), h.start, h.end)
    }
}
