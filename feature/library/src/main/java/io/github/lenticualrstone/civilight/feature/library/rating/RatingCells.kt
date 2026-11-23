package io.github.lenticualrstone.civilight.feature.library.rating

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.animatedInsetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
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
 * What a cell draws, never null, so the fill, outline and text can cross over on a selection change
 * (motion S6, like the segmented cells). An absent fill is the selected fill at alpha 0 and an absent outline
 * the unselected outline at alpha 0, so the fade does not pass through grey.
 */
internal data class RatingCellPaint(val fill: Color, val outline: Color, val text: Color)

internal fun ratingCellPaint(rating: Rating, selected: Boolean, colors: CivilightColors): RatingCellPaint {
    val style = ratingCellStyle(rating, selected, colors)
    val on = ratingCellStyle(rating, true, colors)
    val off = ratingCellStyle(rating, false, colors)
    return RatingCellPaint(
        fill = style.fill ?: (on.fill ?: Color.Transparent).copy(alpha = 0f),
        outline = style.outline ?: (off.outline ?: Color.Transparent).copy(alpha = 0f),
        text = style.text,
    )
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
            val paint = ratingCellPaint(rating, isSelected, colors)
            val fill = animatedColor(paint.fill, label = "ratingFill")
            val outline = animatedColor(paint.outline, label = "ratingOutline")
            val text by animatedColor(paint.text, label = "ratingText")
            val shape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .pressable(PressAction.Select(isSelected) { onSelect(rating) }, role = Role.RadioButton, shape = shape, scaleOnPress = false)
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .drawBehind { drawRect(fill.value) }
                    .animatedInsetOutline(outline, { 1.dp }, shape),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CivilightText(
                    RatingLabels.badge(rating),
                    type.number.merge(TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)),
                    color = text,
                    maxLines = 1,
                )
                CivilightText(
                    RatingLabels.gloss(rating),
                    type.small.merge(TextStyle(fontSize = 11.sp, lineHeight = 15.sp)),
                    color = text,
                    maxLines = 1,
                )
            }
        }
    }
}
