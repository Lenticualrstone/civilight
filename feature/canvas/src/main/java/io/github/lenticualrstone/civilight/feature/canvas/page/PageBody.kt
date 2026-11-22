package io.github.lenticualrstone.civilight.feature.canvas.page

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.text.DocBlockKind

/**
 * One block of a page as the board `Page` reads it: paragraphs Nanum Myeongjo 400 16 / 28 on `bg`,
 * sub-headings 700 17 / 24, the author memo in a `tint` box (radius 14, padding 12 × 14) with its
 * `⌁ 작가 메모` label. Links are underlined 1 dp `ink3`; a link to a deleted item is dotted with `◇`.
 * A tap on a link calls [onLink]; a tap elsewhere in the text calls [onText] (tap into text to edit). [modifier]
 * goes on the block (the list's item motion and the unhide).
 */
@Composable
fun PageBlockText(paragraph: PageParagraph, first: Boolean, onLink: (PageLink) -> Unit, onText: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    when (paragraph.kind) {
        DocBlockKind.MEMO -> Column(
            modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.tint)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            CivilightText(paragraph.memoLabel ?: PageCopy.MEMO_LABEL, type.small, color = colors.ink3)
            LinkedText(paragraph, type.command.merge(TextStyle(lineHeight = 22.sp)), colors.ink2, Modifier.padding(top = 4.dp), onLink, onText)
        }
        DocBlockKind.HEADING -> LinkedText(paragraph, type.heading, colors.ink, modifier.padding(top = if (first) 18.dp else 22.dp), onLink, onText)
        DocBlockKind.PARAGRAPH -> LinkedText(paragraph, bodyStyle(type.lead), colors.ink, modifier.padding(top = if (first) 18.dp else 6.dp), onLink, onText)
    }
}

private fun bodyStyle(lead: TextStyle): TextStyle = lead.merge(TextStyle(fontSize = 16.sp, lineHeight = 28.sp))

@Composable
private fun LinkedText(
    paragraph: PageParagraph,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onLink: (PageLink) -> Unit,
    onText: () -> Unit,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val decorations = remember(paragraph) {
        paragraph.links.map { span ->
            val kind = when (span.link.state) {
                LinkState.LIVE -> InlineDecoration.LINK
                LinkState.HIDDEN -> InlineDecoration.HIDDEN_LINK
                LinkState.TRASHED, LinkState.GONE -> InlineDecoration.DELETED_REF
                LinkState.OTHER -> null
            }
            kind?.let { DecorationRange(span.start, span.end, it) }
        }.filterNotNull() + paragraph.parens.map { DecorationRange(it.first, it.last + 1, InlineDecoration.PAREN) }
    }
    DecoratedText(
        text = AnnotatedString(paragraph.text),
        style = style,
        color = color,
        decorations = decorations,
        onTextLayout = { layout = it },
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                customActions = paragraph.links.filter { it.link.state != LinkState.OTHER }.map { span ->
                    CustomAccessibilityAction(span.link.inline) { onLink(span.link); true }
                }
            }
            .pointerInput(paragraph) {
                detectTapGestures { at ->
                    val offset = layout?.getOffsetForPosition(at)
                    val link = offset?.let(paragraph::linkAt)?.takeIf { it.state != LinkState.OTHER }
                    if (link != null) onLink(link) else onText()
                }
            },
    )
}
