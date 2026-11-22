package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.text.MarkerGlyph
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.text.MarkerLine
import io.github.lenticualrstone.civilight.designsystem.text.PartUnderline
import io.github.lenticualrstone.civilight.designsystem.text.SpeakerMarker
import io.github.lenticualrstone.civilight.designsystem.text.speakerLabelParts
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.state.MarkModel
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerLook
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerModel

/** A mark as the DS speaker-line marker. */
fun MarkModel.toSpeakerMarker(): SpeakerMarker? {
    val kind = MarkerKind.of(glyph) ?: return null
    return SpeakerMarker(kind, if (dotted) MarkerLine.DOTTED else MarkerLine.SOLID)
}

/**
 * The speaker line of a dialogue block (DS-42 `.who`, SCR-20): display name, the real name in `ink3`
 * when it differs, `· #043` when selected, `화자 없음` in `warn`, deleted speakers dotted with `◇`,
 * a small [extraLabel] (`입력 중`, `직전 화자`) after the name, and the marks at the far right.
 * Tapping it opens the speaker picker ([onClick], touch area ≥ 44 dp).
 */
@Composable
fun ScriptSpeakerLine(
    speaker: SpeakerModel,
    selected: Boolean,
    number: String?,
    marks: List<MarkModel>,
    modifier: Modifier = Modifier,
    typing: Boolean = false,
    extraLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val markers = remember(marks) { marks.mapNotNull { it.toSpeakerMarker() } }
    val parts = remember(speaker, selected, number, markers, typing) {
        speakerLabelParts(
            displayName = speaker.text,
            realName = speaker.realName,
            selected = selected,
            blockNo = number,
            markers = markers,
            deleted = speaker.look == SpeakerLook.DELETED,
            typing = typing,
        )
    }
    val click = onClick
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (click != null) {
                    Modifier.pressable(PressAction.Click(click), role = Role.Button, hitTarget = HitTarget.SMALL, scaleOnPress = false)
                } else Modifier,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.filterNot { it.alignEnd }.forEachIndexed { i, part ->
            val marker = part.marker
            val color = if (i == 0 && speaker.look == SpeakerLook.MISSING) colors.warn else colors.of(part.color)
            when {
                marker != null -> MarkerGlyph(marker.kind, line = marker.line, size = 11.dp)
                part.underline == PartUnderline.DOTTED -> DecoratedText(
                    AnnotatedString(part.text),
                    type.of(part.role),
                    color = color,
                    decorations = listOf(DecorationRange(0, part.text.length, InlineDecoration.DELETED_REF)),
                    maxLines = 1,
                )
                else -> CivilightText(part.text, type.of(part.role), color = color, maxLines = 1)
            }
        }
        if (extraLabel != null) {
            CivilightText(extraLabel, type.small.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3, maxLines = 1)
        }
        val ends = parts.filter { it.alignEnd }
        if (ends.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            ends.forEach { part -> part.marker?.let { MarkerGlyph(it.kind, line = it.line, size = 11.dp) } }
        }
    }
}
