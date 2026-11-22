package io.github.lenticualrstone.civilight.ui.richtext

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.Pill
import io.github.lenticualrstone.civilight.designsystem.component.fieldOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.RichLinkPickerSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.picker.PickerModels

/** Copy of the shared editor's tools (SCR-16 / SCR-55 words, as the script's keyboard toolbar). */
object RichTextCopy {
    const val PAREN_TOOL = "( )"
    const val PAREN_LABEL = "괄호 지문"
    const val LINK_TOOL = "@"

    val all: List<String> = listOf(PAREN_TOOL, PAREN_LABEL, LINK_TOOL)
}

/**
 * The shared rich text editor for [io.github.lenticualrstone.civilight.model.text.RichDoc] values
 * (`긴 글` fields, 말투 / 비밀 prose, archive documents, world text, pages): a DS-35 multi-line field
 * with `@` links (link shape), `괄호 지문` drawn in parentheses (`ink3`) and `⌁` memo paragraphs
 * (`⌁ ` mark, `ink3`), and a tool row under it with two visible buttons: `( )` (paren mode, a toggle)
 * and `@` (opens the shared `@` picker [RichLinkPickerSheet] through [openSheet]; the pick comes back
 * through `NavResults` and is inserted at the caret where `@` was pressed).
 *
 * [openSheet] is the host's `sheets.open` (a screen) or the sheet scope's `open` (inside another
 * sheet, where the picker stacks on top). [linkLook] styles links to hidden or deleted items; a caller
 * whose text links to items hidden while locked should ask for `지문으로 보기` before editing (the
 * stored link text is shown here). Save with [RichTextEditorState.result].
 */
@Composable
fun RichTextEditor(
    state: RichTextEditorState,
    projectKey: Key,
    linkPickerTitle: String,
    openSheet: (SheetKey) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    linkKinds: Set<PickKind> = PickerModels.DOCUMENT_LINK_KINDS,
    enabled: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 120.dp,
    linkLook: (TextRun.Link) -> LinkLook = { LinkLook.LIVE },
) {
    val deps = LocalFeatureDeps.current
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val requestKey = rememberSaveable { "richtext-" + Keys.newKey() }
    LaunchedEffect(requestKey, state) {
        deps.results.results(requestKey).collect {
            (deps.results.consume(requestKey) as? LinkPick)?.let(state::insertLink)
        }
    }
    val layout = remember(state.doc) { RichDocEdit.layout(state.doc) }
    val transformation = remember(layout, colors, linkLook) { RichTextTransformation(layout, colors, linkLook) }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val outline = fieldOutline(focused, error = false, colors = colors)
    val shape = RoundedCornerShape(12.dp)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) CivilightText(label, type.label, color = colors.ink2)
        BasicTextField(
            value = state.field,
            onValueChange = state::onFieldChange,
            enabled = enabled,
            singleLine = false,
            textStyle = type.field.merge(TextStyle(color = colors.ink)),
            cursorBrush = SolidColor(colors.acc),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            visualTransformation = transformation,
            interactionSource = source,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .clip(shape)
                        .background(colors.tint)
                        .insetOutline(outline.color, outline.width, shape)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    if (state.field.text.isEmpty() && placeholder != null) {
                        CivilightText(placeholder, type.field, color = colors.ink3)
                    }
                    inner()
                }
            },
        )
        if (enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    text = RichTextCopy.PAREN_TOOL,
                    onClick = state::toggleParen,
                    primary = state.parenMode,
                    modifier = Modifier.semantics {
                        contentDescription = RichTextCopy.PAREN_LABEL
                        selected = state.parenMode
                    },
                )
                Pill(
                    text = RichTextCopy.LINK_TOOL,
                    onClick = {
                        state.beginLink()
                        openSheet(RichLinkPickerSheet(projectKey, requestKey, linkPickerTitle, linkKinds.map { it.name }))
                    },
                )
            }
        }
    }
}

/** Draws the editor text with parentheses, memo marks and link shapes (offsets mapped both ways). */
private class RichTextTransformation(
    private val layout: TextLayout,
    private val colors: CivilightColors,
    private val linkLook: (TextRun.Link) -> LinkLook,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = ShownText.of(text.text, layout.takeIf { matches(it, text.text) } ?: EMPTY, linkLook)
        val styled = AnnotatedString.Builder(shown.text).apply {
            for (span in shown.spans) addStyle(styleOf(span.kind), span.start, span.end)
        }.toAnnotatedString()
        return TransformedText(
            styled,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = shown.originalToShown(offset)
                override fun transformedToOriginal(offset: Int): Int = shown.shownToOriginal(offset)
            },
        )
    }

    /** The layout belongs to this text (a frame can draw the new text before the layout follows). */
    private fun matches(layout: TextLayout, text: String): Boolean =
        (layout.paragraphs.lastOrNull()?.end ?: 0) == text.length

    private fun styleOf(kind: SpanKind): SpanStyle = when (kind) {
        SpanKind.LINK, SpanKind.LINK_HIDDEN -> SpanStyle(color = colors.ink, textDecoration = TextDecoration.Underline)
        SpanKind.LINK_DELETED -> SpanStyle(color = colors.ink2, textDecoration = TextDecoration.Underline)
        SpanKind.PAREN, SpanKind.MEMO -> SpanStyle(color = colors.ink3)
        SpanKind.HEADING -> SpanStyle(fontWeight = FontWeight.Bold)
    }

    override fun equals(other: Any?): Boolean =
        other is RichTextTransformation && other.layout == layout && other.colors == colors && other.linkLook === linkLook

    override fun hashCode(): Int = layout.hashCode()

    companion object {
        private val EMPTY = TextLayout(emptyList(), emptyList())
    }
}
