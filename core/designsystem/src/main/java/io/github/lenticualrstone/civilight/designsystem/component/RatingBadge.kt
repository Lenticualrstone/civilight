package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-20 rating badge: `ALL` · `R-15` · `R-18` in the `badge` role, height 18, padding 0 × 6, radius 5, 1 dp
 * inside outline, raised 1 dp. Never filled. The 6 dp right margin is the caller's spacing.
 */
@Composable
fun RatingBadge(rating: Rating, modifier: Modifier = Modifier, surface: BadgeSurface = BadgeSurface.PLAIN) {
    val style = badgeStyle(rating, surface, CivilightTheme.colors)
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .offset(y = (-1).dp)
            .heightIn(min = 18.dp)
            .insetOutline(style.outline, 1.dp, shape)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(rating.label, CivilightTheme.type.badge, color = style.text, maxLines = 1)
    }
}
