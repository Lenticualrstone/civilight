package io.github.lenticualrstone.civilight.feature.canvas.image.crop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CropFrame
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.canvas.image.CropMath
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.image.holoSurface
import kotlin.math.roundToInt

/** Inset of the crop frame inside the stage (DS `CropFrame` default). */
private val FRAME_MARGIN = 24.dp

/** Corner marks: L shapes 22 × 22, 4 dp, white (board `ImageCrop`). */
private val CORNER = 22.dp
private val CORNER_STROKE = 4.dp

/** Readout under the frame: mono 11, `#E9EAEE`. */
private val ReadoutColor = Color(0xFFE9EAEE)

/**
 * The crop stage (CNV-28): the original drawn under a fixed frame of the current ratio (dragging moves the
 * picture, pinching zooms it), the outside darkened, thirds, corner marks and the readout under the frame.
 * Switching the ratio animates only the frame's aspect (200 ms). [origin] is the picker tile the picture
 * grows from (CNV-45, 360 ms decelerate); under reduced motion the stage fades in over 120 ms.
 */
@Composable
fun CropStage(
    bitmap: ImageBitmap,
    session: CropSession,
    origin: Rect?,
    holoSweep: Boolean,
    onGesture: (dx: Float, dy: Float, zoom: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = CivilightTheme.reducedMotion
    val ratioTarget = ImageSlots.parts(session.current).value
    val ratio by animateFloatAsState(
        ratioTarget,
        if (reduced) snap() else tween(CivilightMotion.standard.durationMs, easing = CivilightMotion.standard.easing),
        label = "cropRatio",
    )
    val readout = session.readout()
    val crop = readout.crop
    val density = LocalDensity.current
    val marginPx = with(density) { FRAME_MARGIN.toPx() }
    var stageInWindow by remember { mutableStateOf<Rect?>(null) }
    val grow = remember { Animatable(if (origin == null) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (grow.value < 1f) {
            val token = CivilightMotion.enterLong
            grow.animateTo(1f, CivilightMotion.spec(token, reduced))
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { stageInWindow = it.boundsInWindow() }) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val frame = CropMath.frameIn(w, h, ratio, marginPx)
        val scale = frame.width / crop.w
        val currentScale by rememberUpdatedState(scale)
        val gesture by rememberUpdatedState(onGesture)
        // CNV-45: the tapped tile grows into the frame (scale and move about the frame's centre).
        val from = origin?.let { tile -> stageInWindow?.let { stage -> tile.translate(-stage.left, -stage.top) } }
        val p = grow.value
        val growLayer = Modifier.graphicsLayer {
            if (reduced || from == null) {
                alpha = if (origin == null) 1f else p
            } else {
                val cx = frame.left + frame.width / 2
                val cy = frame.top + frame.height / 2
                transformOrigin = TransformOrigin(cx / w, cy / h)
                val s0 = (from.width / frame.width).coerceIn(0.05f, 1f)
                val sc = s0 + (1f - s0) * p
                scaleX = sc
                scaleY = sc
                translationX = (from.center.x - cx) * (1f - p)
                translationY = (from.center.y - cy) * (1f - p)
            }
        }
        Box(Modifier.fillMaxSize().then(growLayer)) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .holoSurface(session.holo, sweepOnStart = holoSweep)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val s = currentScale
                            if (s > 0f) gesture(pan.x / s, pan.y / s, zoom)
                        }
                    },
            ) {
                val bw = bitmap.width.toFloat()
                val bh = bitmap.height.toFloat()
                val rotation = CropMath.normalizeRotation(crop.rotation)
                val rbw = if (rotation % 180 == 90) bh else bw
                val k = session.sourceW / bw
                withTransform({
                    translate(frame.left - crop.x * scale, frame.top - crop.y * scale)
                    scale(scale * k, scale * k, pivot = Offset.Zero)
                    if (crop.flipH) {
                        translate(rbw, 0f)
                        scale(-1f, 1f, pivot = Offset.Zero)
                    }
                    when (rotation) {
                        90 -> {
                            translate(bh, 0f)
                            rotate(90f, pivot = Offset.Zero)
                        }
                        180 -> {
                            translate(bw, bh)
                            rotate(180f, pivot = Offset.Zero)
                        }
                        270 -> {
                            translate(0f, bw)
                            rotate(270f, pivot = Offset.Zero)
                        }
                    }
                }) { drawImage(bitmap) }
            }
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (origin == null) 1f else p }) {
            CropFrame(ratio, margin = FRAME_MARGIN)
            Canvas(Modifier.fillMaxSize()) {
                val len = CORNER.toPx()
                val sw = CORNER_STROKE.toPx()
                val white = Color.White
                fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                    drawLine(white, Offset(x, y), Offset(x + dx * len, y), sw, StrokeCap.Square)
                    drawLine(white, Offset(x, y), Offset(x, y + dy * len), sw, StrokeCap.Square)
                }
                corner(frame.left, frame.top, 1f, 1f)
                corner(frame.right, frame.top, -1f, 1f)
                corner(frame.left, frame.bottom, 1f, -1f)
                corner(frame.right, frame.bottom, -1f, -1f)
            }
            CivilightText(
                readout.label,
                CivilightTheme.type.number.merge(TextStyle(fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center)),
                Modifier.fillMaxWidth().offset { IntOffset(0, (frame.bottom + 10.dp.toPx()).roundToInt()) },
                color = ReadoutColor,
                maxLines = 1,
            )
        }
    }
}
