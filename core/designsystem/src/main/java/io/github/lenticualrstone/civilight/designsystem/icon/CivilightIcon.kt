package io.github.lenticualrstone.civilight.designsystem.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** An outline icon of DS-12, default 20 dp in `ink2`. A null [contentDescription] marks it decorative. */
@Composable
fun CivilightIcon(
    id: IconId,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = CivilightTheme.colors.ink2,
) {
    CivilightIcon(id.vector, contentDescription, modifier, size, tint)
}

@Composable
fun CivilightIcon(
    vector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = CivilightTheme.colors.ink2,
) {
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
