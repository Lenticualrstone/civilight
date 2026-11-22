package io.github.lenticualrstone.civilight.designsystem.glass

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalGlassMode

/**
 * The content that glass surfaces blur (DS-22). The content is recorded into a graphics layer
 * ([glassSource]); surfaces drawn as **siblings above it** redraw that layer blurred. A glass surface
 * must never be a descendant of its own source.
 */
@Stable
class GlassSource internal constructor(internal val layer: GraphicsLayer) {
    internal var positionInRoot by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassSource(): GlassSource {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassSource(layer) }
}

/**
 * The nearest glass source, provided by [io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold]
 * and [io.github.lenticualrstone.civilight.designsystem.component.CivilightSheetLayout]; provide your own with
 * `CompositionLocalProvider(LocalGlassSource provides source)` around surfaces drawn over `Modifier.glassSource(source)`.
 */
val LocalGlassSource = staticCompositionLocalOf<GlassSource?> { null }

/** Records this node's content as the backdrop of [source]. */
fun Modifier.glassSource(source: GlassSource): Modifier = this
    .onGloballyPositioned { source.positionInRoot = it.positionInRoot() }
    .drawWithContent {
        source.layer.record { this@drawWithContent.drawContent() }
        drawLayer(source.layer)
    }

/**
 * Limits simultaneous sheet blurs in [GlassMode.BLUR_COSTLY] to one (DS-22 proposed); later sheets draw
 * the SCRIM look.
 */
@Stable
class GlassBudget {
    private val sheets = mutableStateListOf<Any>()

    internal fun register(token: Any) {
        if (token !in sheets) sheets.add(token)
    }

    internal fun release(token: Any) {
        sheets.remove(token)
    }

    internal fun mayBlur(token: Any): Boolean = sheets.firstOrNull() == token
}

val LocalGlassBudget = staticCompositionLocalOf { GlassBudget() }

/**
 * Draws the glass material of [surface] in the current [GlassMode]: backdrop blur (API 31+) with
 * saturation, the fill, the bars' edge masks and the opaque-mode borders. Hidden from accessibility.
 * Use as the first child of a surface, sized with `matchParentSize()`.
 */
@Composable
fun GlassBackground(
    surface: GlassSurface,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    source: GlassSource? = LocalGlassSource.current,
    mode: GlassMode = LocalGlassMode.current,
    fill: Color? = null,
) {
    val colors = CivilightTheme.colors
    val blurTokens = CivilightTheme.blur
    val budget = LocalGlassBudget.current
    val token = remember { Any() }
    val isSheet = surface == GlassSurface.SHEET || surface == GlassSurface.PANEL
    if (isSheet && mode == GlassMode.BLUR_COSTLY) {
        DisposableEffect(budget, token) {
            budget.register(token)
            onDispose { budget.release(token) }
        }
    }
    val canBlur = Build.VERSION.SDK_INT >= 31 && source != null
    val effectiveMode = when {
        (mode == GlassMode.BLUR || mode == GlassMode.BLUR_COSTLY) && !canBlur -> GlassMode.SCRIM
        mode == GlassMode.BLUR_COSTLY && isSheet && !budget.mayBlur(token) -> GlassMode.SCRIM
        else -> mode
    }
    val style = glassStyle(effectiveMode, surface, colors, blurTokens)
    val blurLayer = rememberGraphicsLayer()
    var selfPosition by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .clearAndSetSemantics { }
            .onGloballyPositioned { selfPosition = it.positionInRoot() }
            .graphicsLayer {
                this.shape = shape
                clip = true
                compositingStrategy = if (style.mask != null) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            .drawBehind {
                if (style.blur > 0.dp && source != null && Build.VERSION.SDK_INT >= 31) {
                    val radius = style.blur.toPx()
                    blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                    blurLayer.colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(style.saturation) },
                    )
                    val dx = source.positionInRoot.x - selfPosition.x
                    val dy = source.positionInRoot.y - selfPosition.y
                    blurLayer.record { translate(dx, dy) { drawLayer(source.layer) } }
                    drawLayer(blurLayer)
                }
                drawRect(fill ?: style.fill)
                style.mask?.let { m ->
                    val brush = if (m.anchoredAtTop) {
                        Brush.verticalGradient(0f to Color.Black, m.opaqueFraction to Color.Black, 1f to Color.Transparent)
                    } else {
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f - m.opaqueFraction to Color.Black,
                            1f to Color.Black,
                        )
                    }
                    drawRect(brush, blendMode = BlendMode.DstIn)
                }
                val hair = 1.dp.toPx()
                style.bottomBorder?.let { drawRect(it, Offset(0f, size.height - hair), size.copy(height = hair)) }
                style.topBorder?.let { drawRect(it, Offset.Zero, size.copy(height = hair)) }
            },
    )
}

/**
 * Content drawn on a glass surface. Nested glass surfaces inside it read no source and fall back to the
 * SCRIM fill, so glass never samples through other glass (DS-22: no glass-on-glass).
 */
@Composable
fun OnGlass(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassSource provides null, content = content)
}
