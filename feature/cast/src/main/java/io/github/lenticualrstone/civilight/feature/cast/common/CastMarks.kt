package io.github.lenticualrstone.civilight.feature.cast.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy

/** The small `숨김` tag on an unlocked hidden character (rule 19 §6). */
@Composable
fun HiddenMark(modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    CivilightText(
        CastUiCopy.HIDDEN_TAG,
        CivilightTheme.type.small,
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.tint)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        color = colors.ink2,
        maxLines = 1,
    )
}
