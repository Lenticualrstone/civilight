package io.github.lenticualrstone.civilight.designsystem.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.ShadowSpec

/** Draws a DS-10 shadow (`shadow-float`, popover shadow) behind [shape]. Glass and manuscript never get one. */
fun Modifier.tokenShadow(shape: Shape, spec: ShadowSpec): Modifier =
    dropShadow(shape, Shadow(radius = spec.blur, color = spec.color, offset = DpOffset(0.dp, spec.offsetY)))
