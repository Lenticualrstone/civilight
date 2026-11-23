package io.github.lenticualrstone.civilight.feature.canvas.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction

/**
 * U-15.6: the `hl` flash behind a row or card just made with `만들기`, faded in and out on `standard`
 * (120 ms when reduced). Behind a row (with [outset] matching the selected row's side margins), or [over] a card
 * whose image would hide it. A drawing only, not a selection: TalkBack does not read the new row as selected,
 * and the open canvas stays the only `current` one. Feature-local until `ListRow` has a highlight state of its
 * own (Requests).
 */
@Composable
internal fun Modifier.createdFlash(on: Boolean, radius: Dp = 0.dp, outset: Dp = 0.dp, over: Boolean = false): Modifier {
    val hl = CivilightTheme.colors.hl
    val amount by animatedFraction(on, CivilightMotion.standard, label = "createdFlash")
    return drawWithContent {
        if (over) drawContent()
        if (amount > 0f) {
            val color = hl.copy(alpha = hl.alpha * amount)
            val side = outset.toPx()
            val topLeft = Offset(-side, 0f)
            val area = Size(size.width + side * 2, size.height)
            if (radius > 0.dp) {
                drawRoundRect(color, topLeft, area, CornerRadius(radius.toPx()))
            } else {
                drawRect(color, topLeft, area)
            }
        }
        if (!over) drawContent()
    }
}

/** Where a list scrolls to show a row just made (JVM-tested). */
object CreatedScroll {
    /**
     * The lazy item index of [key] in [rows] with [leading] items before the rows (the create row, a section
     * label), or null while the row is not there yet.
     */
    fun <K> index(rows: List<K>, key: K?, leading: Int): Int? {
        if (key == null) return null
        val i = rows.indexOf(key)
        return if (i < 0) null else i + leading
    }
}
