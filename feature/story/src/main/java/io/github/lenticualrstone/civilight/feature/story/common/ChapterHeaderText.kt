package io.github.lenticualrstone.civilight.feature.story.common

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** A chapter header line with its numeral in the serif face ([ChapterGlyph]). */
@Composable
fun ChapterHeaderText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = CivilightTheme.colors.ink,
    maxLines: Int = 1,
) {
    val annotated = remember(text) {
        val split = ChapterGlyph.split(text)
        buildAnnotatedString {
            if (split == null) {
                append(text)
            } else {
                append(split.first)
                addStyle(SpanStyle(fontFamily = FontFamily.Serif), 0, split.first.length)
                append(split.second)
            }
        }
    }
    BasicText(
        text = annotated,
        modifier = modifier,
        style = style.merge(TextStyle(color = color)),
        maxLines = maxLines,
        overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}
