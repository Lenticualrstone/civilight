package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.DirectionCue
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.text.MarkerGlyph
import io.github.lenticualrstone.civilight.designsystem.text.PartUnderline
import io.github.lenticualrstone.civilight.designsystem.text.ScriptRun
import io.github.lenticualrstone.civilight.designsystem.text.SpeakerMarker
import io.github.lenticualrstone.civilight.designsystem.text.directionLineText
import io.github.lenticualrstone.civilight.designsystem.text.manuscriptText
import io.github.lenticualrstone.civilight.designsystem.text.speakerLabelParts
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics

/** Speaker of a block as the speaker line shows it (display-name rule: CST). */
data class SpeakerLabel(
    val displayName: String,
    val realName: String? = null,
    val blockNo: String? = null,
    val markers: List<SpeakerMarker> = emptyList(),
    val deleted: Boolean = false,
)

/** Dialogue (`line` role) or narration (`body` role, `ink2`, no speaker line). */
enum class BlockTextKind { DIALOGUE, NARRATION }

/**
 * DS-42 speaker line (`.who`): `speaker` role row, gap 8, centre-aligned; parts from [speakerLabelParts];
 * markers at the far right. No expression tags.
 */
@Composable
fun SpeakerLine(
    speaker: SpeakerLabel,
    selected: Boolean,
    modifier: Modifier = Modifier,
    typing: Boolean = false,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val parts = remember(speaker, selected, typing) {
        speakerLabelParts(speaker.displayName, speaker.realName, selected, speaker.blockNo, speaker.markers, speaker.deleted, typing)
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        parts.filterNot { it.alignEnd }.forEach { part ->
            val marker = part.marker
            if (marker != null) {
                MarkerGlyph(marker.kind, line = marker.line, size = 11.dp)
            } else if (part.underline == PartUnderline.DOTTED) {
                DecoratedText(
                    AnnotatedString(part.text),
                    type.of(part.role),
                    color = colors.of(part.color),
                    decorations = listOf(DecorationRange(0, part.text.length, InlineDecoration.DELETED_REF)),
                    maxLines = 1,
                )
            } else {
                CivilightText(part.text, type.of(part.role), color = colors.of(part.color), maxLines = 1)
            }
        }
        val ends = parts.filter { it.alignEnd }
        if (ends.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            ends.forEach { part -> part.marker?.let { MarkerGlyph(it.kind, line = it.line, size = 11.dp) } }
        }
    }
}

/**
 * DS-42 manuscript block, typeset without a box: column gap 6, side padding 24; optional speaker line,
 * then dialogue (`line` 18 / 31 `ink`) or narration (`body` 16 / 28 `ink2`). Selected: `sel` fill + 2 dp
 * `acc` bar and 12 dp extra padding; the speaker name turns `ink`. While [typing], a caret sits at
 * [caretOffset] (end by default) and the line never moves. Blocks are separated only by 26 dp
 * (`space-7`), the caller's spacing.
 */
@Composable
fun ScriptBlock(
    runs: List<ScriptRun>,
    modifier: Modifier = Modifier,
    kind: BlockTextKind = BlockTextKind.DIALOGUE,
    speaker: SpeakerLabel? = null,
    selected: Boolean = false,
    typing: Boolean = false,
    caretOffset: Int? = null,
    narrationLabel: String? = null,
    showParens: Boolean = true,
    showCandidates: Boolean = false,
    gutter: Dp = LayoutMetrics.manuscriptGutter,
    onClick: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val text = remember(runs, showParens, showCandidates) { manuscriptText(runs, showParens, showCandidates) }
    val selection = selectionStyle(colors)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectedRowBackground(selected, selection.fill, selection.bar, outset = 0.dp)
            .then(if (onClick != null) Modifier.pressable(PressAction.Click(onClick), role = Role.Button, scaleOnPress = false) else Modifier)
            .padding(horizontal = gutter, vertical = if (selected) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (kind == BlockTextKind.DIALOGUE && speaker != null) {
            SpeakerLine(speaker, selected, typing = typing)
        } else if (kind == BlockTextKind.NARRATION && selected && narrationLabel != null) {
            CivilightText(narrationLabel, type.speakerName, color = colors.ink, maxLines = 1)
        }
        DecoratedText(
            AnnotatedString(text.text),
            if (kind == BlockTextKind.DIALOGUE) type.line else type.body,
            color = if (kind == BlockTextKind.DIALOGUE) colors.ink else colors.ink2,
            decorations = text.decorations,
            caretOffset = if (typing) caretOffset ?: text.text.length else null,
        )
    }
}

/**
 * DS-42 direction line (`.dir`): `dir` role `ink3`, side padding 24:
 * `▸ 배경  하층 거리(새벽)   ♪ 잔불`. Author memos use the same style with `⌁`.
 */
@Composable
fun DirectionLine(cues: List<DirectionCue>, modifier: Modifier = Modifier, gutter: Dp = LayoutMetrics.manuscriptGutter) {
    CivilightText(
        directionLineText(cues),
        CivilightTheme.type.dir,
        modifier.fillMaxWidth().padding(horizontal = gutter),
        color = CivilightTheme.colors.ink3,
    )
}
