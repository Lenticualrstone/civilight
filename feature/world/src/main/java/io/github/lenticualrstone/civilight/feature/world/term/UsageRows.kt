package io.github.lenticualrstone.civilight.feature.world.term

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy

/** Width of the location column (board `Term`: mono 11 in 64 px, then 10 px gap). */
private val LOCATION_WIDTH = 64.dp
private val GAP = 10.dp

/**
 * A `쓰인 곳` row (WLD-19, board `Term`): the location in mono `ink3`, the whole sentence with the
 * span boxed inline (search-highlight rule, never on its own line), marker lines and tags under the
 * sentence. Rows after the lens point are dimmed; tapping opens the place the text lives.
 */
@Composable
fun UsageRowView(
    row: UsageRowUi,
    onOpen: (() -> Unit)?,
    readOnly: Boolean = true,
    onLink: () -> Unit = {},
    onReject: () -> Unit = {},
    onWarning: (() -> Unit)? = null,
    onIntended: () -> Unit = {},
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (onOpen != null) Modifier.pressable(PressAction.Click(onOpen), role = Role.Button, scaleOnPress = false) else Modifier)
            .heightIn(min = 44.dp)
            .alpha(if (row.dimmed) 0.5f else 1f)
            .padding(vertical = 11.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            CivilightText(row.location, type.number, Modifier.width(LOCATION_WIDTH), color = colors.ink3)
            DecoratedText(
                text = AnnotatedString(row.text),
                style = type.lead,
                modifier = Modifier.weight(1f),
                color = colors.ink,
                decorations = if (row.boxed) listOf(DecorationRange(row.boxStart, row.boxEnd, InlineDecoration.SEARCH_HIT)) else emptyList(),
            )
        }
        val indent = Modifier.padding(start = LOCATION_WIDTH + GAP, top = 4.dp)
        row.placeLine?.let { CivilightText(it, type.small, indent, color = colors.ink3) }
        row.markers.forEachIndexed { i, marker ->
            when {
                i == 0 && row.candidate != null -> CandidateMarker(marker.text, readOnly, onLink, onReject, Modifier.padding(start = LOCATION_WIDTH + GAP))
                // `▲` opens the check center (WLD-20); `맞음` next to it keeps this use (WLD-24).
                // Both are text buttons with 44 dp targets.
                marker.warn && onWarning != null -> Row(
                    Modifier.padding(start = LOCATION_WIDTH + GAP - 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextAction(marker.text, onWarning, color = colors.warn, style = type.small)
                    if (row.canMarkIntended && !readOnly) {
                        CivilightText(WorldCopy.SEP, type.small, color = colors.ink3)
                        TextAction(WorldCopy.TAG_INTENDED, onIntended, color = colors.acc, style = type.small)
                    }
                }
                marker.dotted -> DecoratedText(
                    text = AnnotatedString(marker.text),
                    style = type.small,
                    modifier = indent,
                    color = colors.ink3,
                    decorations = listOf(DecorationRange(0, marker.text.length, InlineDecoration.DELETED_REF)),
                )
                else -> CivilightText(marker.text, type.small, indent, color = if (marker.warn) colors.warn else colors.ink3)
            }
        }
        if (row.tags.isNotEmpty()) {
            CivilightText(row.tags.joinToString(WorldCopy.SEP), type.small, indent, color = colors.ink2)
        }
    }
}

/** `◇ 후보 · 끊김 · 잇기 · 아님`: the marker in `ink3`, then `잇기` and `아님` as text buttons (44 dp). */
@Composable
private fun CandidateMarker(text: String, readOnly: Boolean, onLink: () -> Unit, onReject: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CivilightText("$text${WorldCopy.SEP}", type.small, color = colors.ink3)
        if (readOnly) {
            CivilightText("${WorldCopy.BTN_LINK}${WorldCopy.SEP}${WorldCopy.BTN_REJECT}", type.small, color = colors.ink3)
        } else {
            TextAction(WorldCopy.BTN_LINK, onLink, color = colors.acc, style = type.small)
            CivilightText(WorldCopy.SEP, type.small, color = colors.ink3)
            TextAction(WorldCopy.BTN_REJECT, onReject, color = colors.acc, style = type.small)
        }
    }
}
