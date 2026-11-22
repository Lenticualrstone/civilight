package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightType
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.editor.CaretEvent
import io.github.lenticualrstone.civilight.feature.script.editor.FieldDiff
import io.github.lenticualrstone.civilight.feature.script.editor.LinkLook
import io.github.lenticualrstone.civilight.feature.script.editor.RunLayout
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind
import io.github.lenticualrstone.civilight.feature.script.state.MarkModel
import io.github.lenticualrstone.civilight.feature.script.state.OptionLine
import io.github.lenticualrstone.civilight.feature.script.state.SlotModel
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * Draws the raw field text (sentinel + raw) with parentheses around paren runs, the pending `()` of
 * paren mode, and link styling, mapping offsets through [layout]. A [prefix] (the memo's `⌁ 작가
 * 메모  `, SCR-11) is drawn before the text and cannot hold the caret, so the memo line reads the
 * same while typing as while reading. Falls back to no transformation when the field and the runs
 * disagree for a frame.
 */
class RunsTransformation(
    private val layout: RunLayout,
    private val colors: CivilightColors,
    private val type: CivilightType,
    private val prefix: String = "",
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val field = text.text
        if (!field.startsWith(FieldDiff.SENTINEL) || field.length - 1 != layout.rawLength) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val shown = buildAnnotated(layout)
        val p = prefix.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = if (offset <= 0) 0 else layout.toDisplay(offset - 1) + 1 + p
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 0 -> 0
                offset <= 1 + p -> 1
                else -> layout.toRaw(offset - 1 - p) + 1
            }
        }
        return TransformedText(shown, mapping)
    }

    private fun buildAnnotated(layout: RunLayout): AnnotatedString {
        val b = AnnotatedString.Builder()
        b.append(FieldDiff.SENTINEL)
        b.append(prefix)
        b.append(layout.text)
        val p = prefix.length
        layout.spans.forEach { span ->
            b.addStyle(styleOf(span.kind), span.start + 1 + p, span.end + 1 + p)
        }
        return b.toAnnotatedString()
    }

    private fun styleOf(kind: SpanKind): SpanStyle = when (kind) {
        SpanKind.PAREN -> SpanStyle(color = colors.ink3, fontSize = type.paren.fontSize, fontFamily = type.paren.fontFamily)
        SpanKind.LINK, SpanKind.HIDDEN_LINK -> SpanStyle(color = colors.ink, textDecoration = TextDecoration.Underline)
        SpanKind.DELETED_LINK -> SpanStyle(textDecoration = TextDecoration.Underline)
    }

}

/**
 * The block with the caret (SCR-14): the selection look, the speaker line with `입력 중` (or `직전
 * 화자` until the first character) or the kind's [slot] line with the number (SCR-21), and the text
 * field. A choice block draws its option lines with the field in the [caretOption] line (SCR-09);
 * tapping another line moves the caret there. The field is the source of truth while typing;
 * [onCaret] reports the caret line in root coordinates for the fixed writing line.
 */
@Composable
fun EditableBlock(
    kind: BlockKind,
    speaker: SpeakerModel?,
    number: String?,
    speakerMarks: List<MarkModel>,
    memoPrefix: String?,
    value: TextFieldValue,
    runs: List<TextRun>,
    linkLooks: Map<Key, LinkLook>,
    parenMode: Boolean,
    inheritedHint: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (TextFieldValue) -> Unit,
    /** The field lost focus; it passes the option line it was drawn in (null for block text). */
    onFocusLost: (optionKey: Key?) -> Unit,
    onSpeaker: (() -> Unit)?,
    onCaret: (top: Float, bottom: Float, event: CaretEvent) -> Unit,
    modifier: Modifier = Modifier,
    slot: SlotModel? = null,
    options: List<OptionLine> = emptyList(),
    caretOption: Key? = null,
    onTapOption: (OptionLine) -> Unit = {},
    /** `호칭 힌트` lines (SCR-29), drawn as in reading so the text does not move when typing starts. */
    addressHints: List<String> = emptyList(),
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val (blockStyle, blockColor) = blockTextStyle(kind)
    val inOption = options.isNotEmpty()
    val style = if (inOption) optionTextStyle() else blockStyle
    val color = if (inOption) colors.ink else blockColor
    val prefix = memoPrefix?.let { "$it  " }.orEmpty()
    val rawCaret = FieldDiff.rawOffset(value.selection.end)
    val layout = remember(runs, parenMode, rawCaret, linkLooks) {
        val touchesParen = parenMode && parenEndsOrContains(runs, rawCaret)
        RunLayout.of(
            runs,
            showParens = true,
            pendingParenAt = rawCaret.takeIf { parenMode && !touchesParen },
            openParenEndAt = rawCaret.takeIf { parenMode && touchesParen },
            linkLook = { linkLooks[it.linkKey] ?: LinkLook(SpanKind.LINK) },
        )
    }
    val transformation = remember(layout, colors, type, prefix) { RunsTransformation(layout, colors, type, prefix) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var hadFocus by remember { mutableStateOf(false) }

    fun reportCaret(event: CaretEvent) {
        val c = coords ?: return
        val l = textLayout ?: return
        if (!c.isAttached) return
        val display = transformation.filter(AnnotatedString(value.text)).offsetMapping.originalToTransformed(value.selection.end)
        val rect = l.getCursorRect(display.coerceIn(0, l.layoutInput.text.length))
        val origin = c.positionInRoot().y
        onCaret(origin + rect.top, origin + rect.bottom, event)
    }

    val field: @Composable () -> Unit = {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) hadFocus = true
                    else if (hadFocus) {
                        hadFocus = false
                        onFocusLost(caretOption.takeIf { inOption })
                    }
                }
                .onGloballyPositioned {
                    coords = it
                    reportCaret(CaretEvent.LAYOUT)
                },
            textStyle = style.merge(TextStyle(color = color)),
            cursorBrush = SolidColor(colors.acc),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Default),
            visualTransformation = transformation,
            onTextLayout = {
                textLayout = it
                reportCaret(CaretEvent.EDIT)
            },
        )
    }

    BlockFrame(selected = true, modifier = modifier) {
        AddressHints(addressHints)
        if (kind == BlockKind.DIALOGUE && speaker != null) {
            ScriptSpeakerLine(
                speaker, selected = true, number = number, marks = speakerMarks, typing = true,
                extraLabel = if (inheritedHint) ScriptCopy.INHERITED_SPEAKER else ScriptCopy.TYPING,
                onClick = onSpeaker,
            )
        } else if (slot != null) {
            SlotLine(slot)
        }
        if (inOption) {
            options.forEach { option ->
                if (option.optionKey == caretOption) {
                    OptionView(option, selected = true, onTap = null, text = field)
                } else {
                    OptionView(option, selected = true, onTap = { onTapOption(option) })
                }
            }
        } else {
            field()
        }
    }
    LaunchedEffect(value.selection, value.text) { reportCaret(CaretEvent.EDIT) }
}

/** Whether a paren run ends at or contains [at] (paren mode then types into it). */
fun parenEndsOrContains(runs: List<TextRun>, at: Int): Boolean {
    var pos = 0
    for (run in runs) {
        val len = run.shownText.length
        if (run is TextRun.Paren && at in pos..pos + len && at != pos) return true
        pos += len
    }
    return false
}
