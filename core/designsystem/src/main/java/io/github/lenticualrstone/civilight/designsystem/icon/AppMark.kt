package io.github.lenticualrstone.civilight.designsystem.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** A rounded rectangle in mark units. */
data class MarkBar(val x: Float, val y: Float, val width: Float, val height: Float, val radius: Float)

/** Geometry of the manuscript-lines mark (DS-13): three bars and a caret in a square box of [box] units. */
data class MarkGeometry(val box: Float, val bars: List<MarkBar>, val caret: MarkBar) {
    companion object {
        /** 96-unit mark (`scr_motion.py` `mark`): splash, launcher foreground. */
        val Mark96 = MarkGeometry(
            box = 96f,
            bars = listOf(MarkBar(12f, 22f, 72f, 6f, 3f), MarkBar(12f, 44f, 60f, 6f, 3f), MarkBar(12f, 66f, 40f, 6f, 3f)),
            caret = MarkBar(58f, 62f, 4f, 16f, 2f),
        )

        /** 64-unit static mark (app lock 64 dp, lock-screen mark 72 dp). */
        val Static64 = MarkGeometry(
            box = 64f,
            bars = listOf(MarkBar(8f, 14f, 48f, 5f, 3f), MarkBar(8f, 28f, 40f, 5f, 3f), MarkBar(8f, 42f, 26f, 5f, 3f)),
            caret = MarkBar(38f, 40f, 3f, 12f, 1.5f),
        )
    }
}

/**
 * DS-13 app mark: three manuscript lines (`ink`) and a typing caret (`acc`); no plate, no circle.
 * [barProgress] (0…1) draws the bars left → right for the splash (motion CNV-38). Decorative.
 */
@Composable
fun AppMark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    geometry: MarkGeometry = MarkGeometry.Mark96,
    barColor: Color = CivilightTheme.colors.ink,
    caretColor: Color = CivilightTheme.colors.acc,
    barProgress: Float = 1f,
    showCaret: Boolean = true,
) {
    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val k = this.size.minDimension / geometry.box
        val p = barProgress.coerceIn(0f, 1f)
        geometry.bars.forEach { b ->
            drawRoundRect(barColor, Offset(b.x * k, b.y * k), Size(b.width * k * p, b.height * k), CornerRadius(b.radius * k))
        }
        if (showCaret) {
            val c = geometry.caret
            drawRoundRect(caretColor, Offset(c.x * k, c.y * k), Size(c.width * k, c.height * k), CornerRadius(c.radius * k))
        }
    }
}

/** The `CIVILIGHT` wordmark (`wordmark` role; [welcome] uses 12 / 16 with 0.12 em). */
@Composable
fun Wordmark(modifier: Modifier = Modifier, welcome: Boolean = false, text: String = "CIVILIGHT") {
    val type = CivilightTheme.type
    CivilightText(text, if (welcome) type.wordmarkWelcome else type.wordmark, modifier, color = CivilightTheme.colors.ink3, maxLines = 1, textAlign = TextAlign.Center)
}

/** DS-13 biometric button next to the mark: 64 × 64, radius 20, `pri` fill, `Fingerprint` 30 in `onpri`. */
@Composable
fun BiometricButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .pressable(onClick = onClick, role = Role.Button, shape = shape)
            .size(64.dp)
            .clip(shape)
            .background(colors.pri)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(IconId.Fingerprint, null, size = 30.dp, tint = colors.onpri)
    }
}
