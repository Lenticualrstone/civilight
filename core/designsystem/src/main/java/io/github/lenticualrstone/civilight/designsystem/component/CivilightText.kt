package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.hasMarker
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * Text in a type role and colour. Strings containing markers (`◆ ◇ ▲ ▸ ♪ ⌁`) are drawn with vector
 * glyphs (DS-16); single-line text truncates with `…` (DS-08).
 */
@Composable
fun CivilightText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = CivilightTheme.colors.ink,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
) {
    val merged = if (textAlign != null) style.merge(TextStyle(textAlign = textAlign)) else style
    if (hasMarker(text)) {
        DecoratedText(
            text = AnnotatedString(text),
            style = merged,
            modifier = modifier,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
        )
    } else {
        BasicText(
            text = text,
            modifier = modifier,
            style = merged.merge(TextStyle(color = color)),
            maxLines = maxLines,
            overflow = overflow,
            softWrap = maxLines != 1,
        )
    }
}
