package io.github.lenticualrstone.civilight.feature.script.input

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightType
import io.github.lenticualrstone.civilight.feature.script.editor.RunLayout
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind

/**
 * Draws the draft's runs in the input field (SCR-70): paren runs as `(text)` in the paren role, links
 * underlined, an open empty `()` at the caret. The field's own text is the raw text; the mapping keeps the
 * caret where the raw offset says. A text that no longer matches [layout] is shown as it is.
 */
class DraftTransformation(
    private val layout: RunLayout,
    private val colors: CivilightColors,
    private val type: CivilightType,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.length != layout.rawLength) return TransformedText(text, OffsetMapping.Identity)
        val b = AnnotatedString.Builder()
        b.append(layout.text)
        layout.spans.forEach { span ->
            val style = when (span.kind) {
                SpanKind.PAREN -> SpanStyle(color = colors.ink3, fontSize = type.paren.fontSize, fontFamily = type.paren.fontFamily)
                SpanKind.LINK, SpanKind.HIDDEN_LINK -> SpanStyle(color = colors.ink, textDecoration = TextDecoration.Underline)
                SpanKind.DELETED_LINK -> SpanStyle(textDecoration = TextDecoration.Underline)
            }
            b.addStyle(style, span.start, span.end)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = layout.toDisplay(offset)
            override fun transformedToOriginal(offset: Int): Int = layout.toRaw(offset)
        }
        return TransformedText(b.toAnnotatedString(), mapping)
    }
}
