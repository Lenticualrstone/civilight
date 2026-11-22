package io.github.lenticualrstone.civilight.feature.canvas.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.Popover
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.glass.SCRIM_ALPHA
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.canvas.graph.Pt

/** Width of the inner popovers (`Canvas`: ≈300 dp). */
const val POPOVER_WIDTH = 300f

/**
 * An inner-screen popover next to a line or node (CNV-08, CNV-10; board `Canvas`): ≈300 dp wide,
 * radius 18, 1 dp `line`, padding 14 × 16. [anchor] is a screen point (dp) the card hangs below,
 * centred on it. A popover is its own window and cannot blur what is under it, so it uses the SCRIM
 * fill (DS-26).
 */
@Composable
fun CanvasPopover(anchor: Pt, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val fill = if (CivilightTheme.glassMode == GlassMode.OPAQUE) colors.raise else colors.raise.copy(alpha = SCRIM_ALPHA)
    Box(Modifier.offset((anchor.x - POPOVER_WIDTH / 2f).dp, anchor.y.dp).size(0.dp)) {
        Popover(onDismissRequest = onDismiss) {
            Column(
                Modifier
                    .width(POPOVER_WIDTH.dp)
                    .tokenShadow(shape, CivilightTheme.elevation.popover(colors))
                    .clip(shape)
                    .background(fill)
                    .insetOutline(colors.line, 1.dp, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                content = content,
            )
        }
    }
}
