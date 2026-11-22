package io.github.lenticualrstone.civilight.feature.recorder.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureState
import io.github.lenticualrstone.civilight.feature.recorder.capture.MemoView
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerRequest
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerTarget
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkChipRow
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkPickerRows
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkCandidate
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip

/** What the memo sheet asks of its host. */
class MemoActions(
    val onTextChange: (text: String, caret: Int) -> Unit,
    val onCaret: (Int) -> Unit,
    val onPickAt: (LinkCandidate) -> Unit,
    val onOpenPicker: (PickerRequest) -> Unit,
    val onRemoveLinks: (Chip) -> Unit,
    val onHide: (Boolean) -> Unit,
    val onToVoice: () -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * The text memo sheet (LRC-15 … LRC-17, board `LrText`): `라이프 레코더 · 글` · `닫기`; the field
 * (manuscript body 18 / 30, `tint`, `@` links underlined 2 dp `acc`); while an `@` query is open its
 * candidates right under the field (the field keeps the keyboard); the link chips; the `숨기기` row
 * for hidden / R-18 targets (LRC-42); the mic (`말로 기록으로 바꾸기`), the hint and `저장`.
 */
@Composable
fun ColumnScope.TextMemoSheetContent(state: CaptureState, actions: MemoActions) {
    // Keep the last content while the sheet animates out after save or close.
    var last by remember { mutableStateOf<MemoView?>(null) }
    CaptureViews.memo(state)?.let { last = it }
    val view = last ?: return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(view.sheet.title, type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), Modifier.weight(1f), color = colors.ink, maxLines = 1)
        TextAction(view.sheet.close, actions.onClose)
    }
    MemoField(
        text = view.sheet.text,
        caret = state.memoCaret,
        reload = state.memoReload,
        marks = view.marks,
        onChange = actions.onTextChange,
        onCaret = actions.onCaret,
        modifier = Modifier.padding(top = 12.dp),
    )
    if (view.atOpen && view.atCandidates.isNotEmpty()) {
        Column(Modifier.padding(top = 6.dp)) {
            for (candidate in view.atCandidates) {
                val row = LinkPickerRows.row(candidate)
                ListRow(title = row.title, subtitle = row.sub.takeIf { it.isNotEmpty() }, compact = true, onClick = { actions.onPickAt(candidate) })
            }
        }
    }
    LinkChipRow(
        chips = view.sheet.chips,
        onAdd = { actions.onOpenPicker(PickerRequest(PickerTarget.MEMO)) },
        onReplace = { chip -> actions.onOpenPicker(PickerRequest(PickerTarget.MEMO, chip.links.map { it.key })) },
        onRemove = actions.onRemoveLinks,
        modifier = Modifier.padding(top = 10.dp),
    )
    view.sheet.hideRow?.let { row ->
        SwitchRow(title = row.title, subtitle = row.sub, checked = row.on, onCheckedChange = actions.onHide, modifier = Modifier.padding(top = 6.dp))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .pressable(onClick = actions.onToVoice, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape)
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.tint)
                .semantics { contentDescription = view.sheet.toVoiceAria },
            contentAlignment = Alignment.Center,
        ) {
            CivilightIcon(IconId.Mic, contentDescription = null, size = 18.dp, tint = colors.ink)
        }
        CivilightText(view.sheet.hint, type.small, Modifier.weight(1f), color = colors.ink3)
        CivilightButton(view.sheet.save, actions.onSave, enabled = view.sheet.canSave, variant = ButtonVariant.PRIMARY)
    }
}

/**
 * The memo's text field. It edits plain text; the host turns each change into one run replace.
 * The field reloads from the runs only when [reload] changes (an `@` pick), so typing and Hangul
 * composition are never interrupted. Link runs show a leading `@` and a 2 dp `acc` underline.
 */
@Composable
fun MemoField(
    text: String,
    caret: Int,
    reload: Int,
    marks: AtMarks,
    onChange: (String, Int) -> Unit,
    onCaret: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    var field by remember { mutableStateOf(TextFieldValue(text, TextRange(caret.coerceIn(0, text.length)))) }
    LaunchedEffect(reload) {
        if (field.text != text) field = TextFieldValue(text, TextRange(caret.coerceIn(0, text.length)))
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // The marks belong to the runs' text; while the field is a keystroke ahead, show none.
    val shownMarks = if (field.text == text && marks.fits(text.length)) marks else AtMarks.NONE
    val underline = colors.acc
    val style = CivilightTheme.type.line.merge(TextStyle(fontSize = 18.sp, lineHeight = 30.sp, color = colors.ink))
    BasicTextField(
        value = field,
        onValueChange = { next ->
            val textChanged = next.text != field.text
            field = next
            if (textChanged) onChange(next.text, next.selection.end) else onCaret(next.selection.end)
        },
        textStyle = style,
        cursorBrush = SolidColor(colors.acc),
        visualTransformation = AtMarkTransformation(shownMarks),
        onTextLayout = { layout = it },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(shape)
            .background(colors.tint)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(14.dp)
            .drawBehind {
                val result = layout ?: return@drawBehind
                val thickness = 2.dp.toPx()
                for (range in shownMarks.shownRanges()) {
                    if (range.first >= result.layoutInput.text.length) continue
                    val end = (range.last + 1).coerceAtMost(result.layoutInput.text.length)
                    val firstLine = result.getLineForOffset(range.first)
                    val lastLine = result.getLineForOffset((end - 1).coerceAtLeast(range.first))
                    for (line in firstLine..lastLine) {
                        val from = maxOf(range.first, result.getLineStart(line))
                        val to = minOf(end, result.getLineEnd(line))
                        if (to <= from) continue
                        val left = result.getHorizontalPosition(from, usePrimaryDirection = true)
                        val right = result.getHorizontalPosition(to, usePrimaryDirection = true)
                        val y = result.getLineBaseline(line) + 4.dp.toPx()
                        drawRect(underline, Offset(minOf(left, right), y), Size(kotlin.math.abs(right - left), thickness))
                    }
                }
            },
    )
}

/** Shows the `@` of every link run without storing it ([AtMarks]). */
private class AtMarkTransformation(private val marks: AtMarks) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (marks.count == 0 || !marks.fits(text.length)) return TransformedText(text, OffsetMapping.Identity)
        val shown = marks.show(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = marks.originalToShown(offset).coerceIn(0, shown.length)
            override fun transformedToOriginal(offset: Int): Int = marks.shownToOriginal(offset).coerceIn(0, text.length)
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }

    override fun equals(other: Any?): Boolean = other is AtMarkTransformation && other.marks == marks

    override fun hashCode(): Int = marks.hashCode()
}
