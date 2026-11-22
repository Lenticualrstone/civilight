package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** Avatar sizes of DS-46. */
object AvatarSize {
    val Pill: Dp = 32.dp
    val List: Dp = 36.dp
    val Picker: Dp = 38.dp
    val MapChip: Dp = 18.dp
}

/**
 * DS-46 avatar: a `chip` circle with the first grapheme of [name] in 700 `ink` at `floor(size × 0.38)` sp
 * (10 sp for the 18 dp map-chip avatar), or the portrait's 1:1 crop when [image] is given. Decorative for
 * accessibility (the name is always next to it).
 */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = AvatarSize.List,
    image: Painter? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val glyphSp = if (size <= AvatarSize.MapChip) 10 else avatarGlyphSp(size.value.toInt())
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.chip)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CivilightText(
                avatarGlyph(name),
                type.itemTitle.merge(TextStyle(fontSize = glyphSp.sp, lineHeight = glyphSp.sp, fontWeight = FontWeight.Bold)),
                color = colors.ink,
                maxLines = 1,
            )
        }
    }
}
