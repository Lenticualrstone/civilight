package io.github.lenticualrstone.civilight.feature.library.rating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.project.Rating

/** Fill, outline and text of one rating cell (LIB-09): selected ALL · R-15 `pri`, selected R-18 `r18`. */
internal data class RatingCellStyle(val fill: Color?, val outline: Color?, val text: Color)

internal fun ratingCellStyle(rating: Rating, selected: Boolean, colors: CivilightColors): RatingCellStyle = when {
    selected && rating == Rating.R18 -> RatingCellStyle(colors.r18, null, colors.onr18)
    selected -> RatingCellStyle(colors.pri, null, colors.onpri)
    rating == Rating.R18 -> RatingCellStyle(null, colors.r18, colors.r18)
    else -> RatingCellStyle(null, colors.line, colors.ink)
}

/**
 * The 3-cell 수위 등급 picker (`NewName`, `NewNameR18`), also used by 서재 ⋯ › 수위 등급 and 프로젝트
 * 설정: `ALL` / `12세 이하`, `R-15` / `15세 이상`, `R-18` / `성인`, each 56 tall, radius 14.
 */
@Composable
internal fun RatingCells(selected: Rating?, onSelect: (Rating) -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rating.entries.forEach { rating ->
            val isSelected = rating == selected
            val style = ratingCellStyle(rating, isSelected, colors)
            val shape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .pressable(PressAction.Select(isSelected) { onSelect(rating) }, role = Role.RadioButton, shape = shape, scaleOnPress = false)
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .then(if (style.fill != null) Modifier.background(style.fill) else Modifier)
                    .then(if (style.outline != null) Modifier.insetOutline(style.outline, 1.dp, shape) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CivilightText(
                    RatingLabels.badge(rating),
                    type.number.merge(TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)),
                    color = style.text,
                    maxLines = 1,
                )
                CivilightText(
                    RatingLabels.gloss(rating),
                    type.small.merge(TextStyle(fontSize = 11.sp, lineHeight = 15.sp)),
                    color = style.text,
                    maxLines = 1,
                )
            }
        }
    }
}
