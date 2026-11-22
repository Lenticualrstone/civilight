package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SceneHead
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.editor.LinkLook
import io.github.lenticualrstone.civilight.feature.script.editor.RunLayout
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.EmptySceneRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.HiddenSceneRow
import io.github.lenticualrstone.civilight.feature.script.state.MarkModel
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerLook
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerModel
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** DS decoration of a display span. */
fun SpanKind.decoration(): InlineDecoration = when (this) {
    SpanKind.PAREN -> InlineDecoration.PAREN
    SpanKind.LINK -> InlineDecoration.LINK
    SpanKind.HIDDEN_LINK -> InlineDecoration.HIDDEN_LINK
    SpanKind.DELETED_LINK -> InlineDecoration.DELETED_REF
}

/** The run layout a block row draws (parens per the `괄호 지문` layer). */
fun BlockRow.layout(showParens: Boolean, editing: Boolean = false): RunLayout =
    RunLayout.of(runs, showParens = showParens || editing, linkLook = { linkLooks[it.linkKey] ?: LinkLook(SpanKind.LINK) })

/** Text style of a text block by kind (DS-42; system text mono 13 / 22, memo `dir`). */
@Composable
fun blockTextStyle(kind: BlockKind?): Pair<TextStyle, androidx.compose.ui.graphics.Color> {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    return when (kind) {
        BlockKind.DIALOGUE -> type.line to colors.ink
        BlockKind.SYSTEM -> type.number.merge(TextStyle(fontSize = 13.sp, lineHeight = 22.sp)) to colors.ink2
        BlockKind.MEMO -> type.dir to colors.ink3
        else -> type.body to colors.ink2
    }
}

/**
 * A block as the manuscript draws it when the caret is not in it (SCR-04 … SCR-11): typeset on `bg`
 * with no card; selected = `sel` fill + 2 dp `acc` bar and 12 dp padding. Tapping the text gives the
 * raw offset under the finger ([onTapText]); the speaker line opens the picker ([onSpeaker]).
 */
@Composable
fun BlockView(
    row: BlockRow,
    selected: Boolean,
    showParens: Boolean,
    inheritedHint: Boolean,
    onTapText: (rawOffset: Int?) -> Unit,
    onSpeaker: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    BlockFrame(selected, modifier.pressable(PressAction.Click({ onTapText(null) }), role = Role.Button, scaleOnPress = false)) {
        AddressHints(row.addressHints)
        when (row.kind) {
            BlockKind.DIALOGUE -> row.speaker?.let { speaker ->
                ScriptSpeakerLine(
                    speaker, selected, row.number.takeIf { selected }, row.speakerMarks,
                    extraLabel = ScriptCopy.INHERITED_SPEAKER.takeIf { inheritedHint },
                    onClick = onSpeaker,
                )
            }
            BlockKind.CHOICE -> SlotLine(ScriptCopy.CHOICE_LABEL, ScriptCopy.CHOICE_REJOIN, row.number.takeIf { selected })
            BlockKind.SYSTEM -> SlotLine(ScriptCopy.SYSTEM_LABEL, ScriptCopy.SYSTEM_SUB, row.number.takeIf { selected })
            BlockKind.NARRATION -> if (selected) SlotLine(io.github.lenticualrstone.civilight.domain.script.Labels.kindLabel(BlockKind.NARRATION), null, row.number)
            else -> Unit
        }
        when (row.kind) {
            BlockKind.DIRECTION -> CivilightText(row.text, type.dir, color = colors.ink3)
            BlockKind.CHOICE -> row.options.forEach { option ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    CivilightText(option.index, type.number.merge(TextStyle(fontSize = 12.sp)), color = colors.ink3)
                    CivilightText(option.text, type.body.merge(TextStyle(lineHeight = 26.sp)), color = colors.ink)
                }
            }
            null -> CivilightText(row.text, type.body, color = colors.ink2)
            else -> RunsText(row, showParens, onTapText)
        }
    }
}

/** Shared block frame: column gap 6, side padding 24 (the caller's column), selection look. */
@Composable
fun BlockFrame(selected: Boolean, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = CivilightTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectedRowBackground(selected, colors.sel, colors.acc, outset = 0.dp)
            .padding(horizontal = 24.dp, vertical = if (selected) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** `선택지 · 다음 블록에서 합류` / `시스템 · 화면 문구` speaker slot (boards `Blocks`). */
@Composable
fun SlotLine(label: String, sub: String?, number: String?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(label, type.speakerName, color = colors.ink2, maxLines = 1)
        if (sub != null) CivilightText(sub, type.speaker, color = colors.ink3, maxLines = 1)
        if (number != null) CivilightText("· $number", type.speaker, color = colors.ink3, maxLines = 1)
    }
}

@Composable
private fun AddressHints(hints: List<String>) {
    if (hints.isEmpty()) return
    CivilightText(hints.joinToString(" · "), CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
}

/** Text runs with their decorations, the memo prefix, and the end-of-line marks; taps give a raw offset. */
@Composable
private fun RunsText(row: BlockRow, showParens: Boolean, onTapText: (Int?) -> Unit) {
    val (style, color) = blockTextStyle(row.kind)
    val layout = remember(row, showParens) { row.layout(showParens) }
    val prefix = row.memoPrefix?.let { "$it  " }.orEmpty()
    val suffix = row.textMarks.joinToString("") { " ${it.glyph}" }
    val text = prefix + layout.text + suffix
    val decorations = remember(layout, prefix) {
        layout.spans.map { DecorationRange(it.start + prefix.length, it.end + prefix.length, it.kind.decoration()) }
    }
    val dotted = remember(row.textMarks, text) { dottedIndices(text.length - suffix.length, row.textMarks) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    DecoratedText(
        AnnotatedString(text),
        style,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp)
            .pointerInput(layout, prefix) {
                detectTapGestures { position ->
                    val at = textLayout?.getOffsetForPosition(position)
                    onTapText(at?.let { layout.toRaw((it - prefix.length).coerceAtLeast(0)) })
                }
            },
        color = color,
        decorations = decorations,
        dottedMarkers = dotted,
        onTextLayout = { textLayout = it },
    )
}

/** Indices of dotted marks appended as ` ◆` / ` ◇` after [start]. */
private fun dottedIndices(start: Int, marks: List<MarkModel>): Set<Int> =
    marks.mapIndexedNotNull { i, m -> if (m.dotted) start + i * 2 + 1 else null }.toSet()

/**
 * Scene head (SCR-46, DS-43): tap → `씬 · 장소와 때`; `⋯` → the scene menu (SCR-47) when
 * [onMenu] is given.
 */
@Composable
fun HeadView(row: HeadRow, onOpen: () -> Unit, onMenu: (() -> Unit)?) {
    SceneHead(
        title = row.title,
        modifier = Modifier.pressable(PressAction.Click(onOpen), role = Role.Button, scaleOnPress = false),
        subtitle = row.transitionLine,
        menu = onMenu?.let { IconAction(ScriptCopy.sceneMenuLabel(row.ordinal), it) },
    )
}

/** A locked hidden scene: one row; tapping starts the unlock (rule 19). */
@Composable
fun HiddenSceneView(row: HiddenSceneRow, onUnlock: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(PressAction.Click(onUnlock), role = Role.Button, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(row.text, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
    }
}

/** The virtual first line of an empty scene: speaker line (inherited or `화자 없음`) and an empty line. */
@Composable
fun EmptySceneView(row: EmptySceneRow, speaker: SpeakerModel?, onTap: () -> Unit) {
    BlockFrame(false, Modifier.pressable(PressAction.Click(onTap), role = Role.Button, scaleOnPress = false)) {
        ScriptSpeakerLine(
            speaker ?: SpeakerModel(ScriptCopy.NO_SPEAKER, null, SpeakerLook.UNSET, null),
            selected = false,
            number = null,
            marks = emptyList(),
            extraLabel = ScriptCopy.INHERITED_SPEAKER.takeIf { speaker != null && speaker == row.speaker },
        )
        Spacer(Modifier.height(CivilightTheme.type.line.lineHeight.value.dp))
    }
}
