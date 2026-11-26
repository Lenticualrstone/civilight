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
import io.github.lenticualrstone.civilight.feature.world.common.dimmed
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.domain.world.term.FlowMark
import io.github.lenticualrstone.civilight.domain.world.term.TermFlow
import io.github.lenticualrstone.civilight.domain.world.term.TermFlowStrip

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
    modifier: Modifier = Modifier,
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
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (onOpen != null) Modifier.pressable(PressAction.Click(onOpen), role = Role.Button, scaleOnPress = false) else Modifier)
            .heightIn(min = 44.dp)
            .dimmed(row.dimmed)
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

/**
 * (2026-11-26) A `작중에서` row (board `ImmContent` term branch): avatar letter, who says it, the location in mono,
 * then the whole sentence in the serif 17 / 30 with the linked or matched span boxed inline (search-highlight rule).
 * Under it: `▲ 공개(1-7) 전 사용` (→ check center) with `맞음`, other marker lines and tags, and on a candidate
 * `◇ 후보 · 끊김` with the visible 44 dp buttons `잇기` · `아님`. Tapping the row opens the place the text lives.
 */
@Composable
fun QuoteRowView(
    row: UsageRowUi,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)?,
    readOnly: Boolean,
    onLink: () -> Unit,
    onReject: () -> Unit,
    onWarning: () -> Unit,
    onIntended: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (onOpen != null) Modifier.pressable(PressAction.Click(onOpen), role = Role.Button, scaleOnPress = false) else Modifier)
            .heightIn(min = 44.dp)
            .dimmed(row.dimmed)
            .padding(top = 14.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(colors.tint),
                contentAlignment = Alignment.Center,
            ) { CivilightText(row.initial.orEmpty(), type.speakerName, color = colors.ink, maxLines = 1) }
            CivilightText(row.who.orEmpty(), type.label, Modifier.weight(1f), color = colors.ink2, maxLines = 1)
            CivilightText(row.location, type.number, color = colors.ink3, maxLines = 1)
        }
        DecoratedText(
            text = AnnotatedString(row.text),
            style = type.lead.merge(TextStyle(fontSize = 17.sp, lineHeight = 30.sp)),
            modifier = Modifier.padding(top = 8.dp),
            color = colors.ink,
            decorations = if (row.boxed) listOf(DecorationRange(row.boxStart, row.boxEnd, InlineDecoration.SEARCH_HIT)) else emptyList(),
        )
        row.markers.forEachIndexed { i, marker ->
            when {
                i == 0 && row.candidate != null -> CandidateLine(marker.text, readOnly, onLink, onReject)
                marker.warn -> Row(Modifier.padding(top = 2.dp).offset(x = (-10).dp), verticalAlignment = Alignment.CenterVertically) {
                    TextAction(marker.text, onWarning, color = colors.warn, style = type.label)
                    if (row.canMarkIntended && !readOnly) {
                        CivilightText(WorldCopy.SEP, type.small, color = colors.ink3)
                        TextAction(WorldCopy.TAG_INTENDED, onIntended, color = colors.acc, style = type.label)
                    }
                }
                marker.dotted -> DecoratedText(
                    text = AnnotatedString(marker.text),
                    style = type.small,
                    modifier = Modifier.padding(top = 6.dp),
                    color = colors.ink3,
                    decorations = listOf(DecorationRange(0, marker.text.length, InlineDecoration.DELETED_REF)),
                )
                else -> CivilightText(marker.text, type.small, Modifier.padding(top = 6.dp), color = if (marker.warn) colors.warn else colors.ink3)
            }
        }
        if (row.tags.isNotEmpty()) {
            CivilightText(row.tags.joinToString(WorldCopy.SEP), type.small, Modifier.padding(top = 4.dp), color = colors.ink2)
        }
    }
}

/** `◇ 후보 · 끊김` then the two visible pill buttons (44 dp): `잇기` filled in `ink`, `아님` outlined. */
@Composable
private fun CandidateLine(text: String, readOnly: Boolean, onLink: () -> Unit, onReject: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(text, type.label.merge(TextStyle(fontSize = 12.sp)), Modifier.weight(1f), color = colors.ink3)
        PillButton(WorldCopy.BTN_LINK, filled = true, enabled = !readOnly, onClick = onLink)
        PillButton(WorldCopy.BTN_REJECT, filled = false, enabled = !readOnly, onClick = onReject)
    }
}

/** A 44 dp pill (min width 56): filled `ink` on `bg`, or a 1 dp `line` outline with `ink2` text. */
@Composable
fun PillButton(text: String, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .pressable(PressAction.Click(onClick), enabled = enabled, role = Role.Button, shape = shape)
            .alpha(if (enabled) 1f else 0.38f)
            .heightIn(min = 44.dp)
            .widthIn(min = 56.dp)
            .clip(shape)
            .then(if (filled) Modifier.background(colors.ink) else Modifier.border(1.dp, colors.line, shape))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(text, type.button, color = if (filled) colors.bg else colors.ink2, maxLines = 1)
    }
}

/** One flow mark: the used mark as a filled 8 dp `acc` dot (a shape, not `●`), `▲` in `warn`, `◇` in `ink3`. */
@Composable
fun FlowMarkView(mark: FlowMark, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    when (mark) {
        FlowMark.NONE -> Unit
        FlowMark.USED -> Box(modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.acc))
        else -> CivilightText(
            mark.symbol,
            CivilightTheme.type.label.merge(TextStyle(fontSize = 14.sp)),
            modifier,
            color = if (mark == FlowMark.BEFORE_REVEAL) colors.warn else colors.ink3,
            maxLines = 1,
        )
    }
}

/** The legend under the strip: each mark followed by its word (`쓴 곳 · 후보 · 공개 전`). */
@Composable
fun FlowLegendView(modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TermFlow.LEGEND_MARKS.forEach { (mark, word) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowMarkView(mark)
                CivilightText(word, type.small, color = colors.ink3, maxLines = 1)
            }
        }
    }
}

/**
 * (2026-11-26) `쓰인 흐름` (board `ImmContent`): one cell per episode, 40 dp tall on `tint` with the mark (a filled dot in
 * `acc`, `▲` in `warn`, `◇` in `ink3`) and a 1.5 dp `acc` ring on the reveal's episode, the code under it in mono.
 * Up to [TermFlow.MAX_VISIBLE] cells share the width; more keep that cell width and scroll sideways.
 */
@Composable
fun FlowStripView(strip: TermFlowStrip, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val gap = 6.dp
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val visible = strip.cells.size.coerceIn(1, TermFlow.MAX_VISIBLE)
        val cellW = (maxWidth - gap * (visible - 1)) / visible
        Row(
            Modifier.then(if (strip.scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth()),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            strip.cells.forEach { cell ->
                Column(
                    Modifier.width(cellW).semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val shape = RoundedCornerShape(8.dp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(shape)
                            .background(colors.tint)
                            .then(if (cell.reveal) Modifier.border(1.5.dp, colors.acc, shape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        FlowMarkView(cell.mark)
                    }
                    CivilightText(cell.code, type.numberHeader.merge(TextStyle(fontSize = 10.sp, lineHeight = 14.sp)), color = colors.ink3, maxLines = 1)
                }
            }
        }
    }
}
