package io.github.lenticualrstone.civilight.feature.canvas.image.crop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
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
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.feature.canvas.image.CropLayerStart
import io.github.lenticualrstone.civilight.feature.canvas.image.CropTransition
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
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

/** What the stage remembers between two compositions to tell a crop step from a finger gesture. */
private class CropMemo(var crop: Crop, var ratio: Ratio?, var controlStep: Int) {
    var step = 0
    var start = CropLayerStart()
    var animate = false
}

/**
 * The crop stage (CNV-28): the original drawn under a fixed frame of the current ratio (dragging moves the
 * picture, pinching zooms it), the outside darkened, thirds, corner marks and the readout under the frame.
 *
 * Motion (audit §4 canvas 2, 6):
 * - Switching the ratio animates only the frame's aspect on `standard`; reduced: the frame crossfades.
 * - A crop step from the controls (turn, mirror, reset, zoom step) eases the new picture from its old look on
 *   `standard` ([CropTransition]); the screen raises [controlStep] for each such step, so a drag or pinch (which
 *   never raises it) follows the finger. Reduced: snap.
 * - [origin] is the picker tile the picture grows from (CNV-45, `enterLong`). [tile] is that tile's decoded
 *   thumbnail: it is drawn and grows from the first frame while the original ([bitmap], [session]) still
 *   decodes; the stage then takes over in place. Without an [origin] the stage fades in once it is ready.
 *   Reduced: the stage fades in over 120 ms.
 *
 * [slot] gives the frame's ratio before the [session] exists. [holoSweep] is the sweep trigger of the holo
 * foil (raise it when the user switches holo on).
 */
@Composable
fun CropStage(
    bitmap: ImageBitmap?,
    session: CropSession?,
    slot: ImageSlot,
    origin: Rect?,
    tile: ImageBitmap?,
    holoSweep: Int,
    controlStep: Int,
    onGesture: (dx: Float, dy: Float, zoom: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = CivilightTheme.motion
    val ratioTarget = ImageSlots.parts(session?.current ?: ImageSlots.ratios(slot).first()).value
    val ratio by animateFloatAsState(ratioTarget, motion.move(CivilightMotion.standard), label = "cropRatio")
    val ready = bitmap != null && session != null
    val appear by animatedFraction(ready, label = "cropReady")
    val density = LocalDensity.current
    val marginPx = with(density) { FRAME_MARGIN.toPx() }
    var stageInWindow by remember { mutableStateOf<Rect?>(null) }
    val grow = remember { Animatable(if (origin == null) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (grow.value < 1f) grow.animateTo(1f, motion.spec(CivilightMotion.enterLong))
    }

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { stageInWindow = it.boundsInWindow() }) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val frame = CropMath.frameIn(w, h, ratio, marginPx)
        val readout = session?.readout()
        val crop = readout?.crop
        val scale = if (crop != null) frame.width / crop.w else 1f
        val currentScale by rememberUpdatedState(scale)
        val gesture by rememberUpdatedState(onGesture)

        // A crop step from the controls eases the new picture from its old look; a gesture, a ratio change or the
        // first crop snaps.
        val memo = remember { CropMemo(crop ?: Crop(0, 0, 1, 1, 0, false, 1f), session?.current, controlStep) }
        if (crop != null && (memo.crop != crop || memo.ratio != session?.current)) {
            val discrete = memo.ratio == session?.current && controlStep != memo.controlStep
            memo.start = if (discrete) CropTransition.start(memo.crop, crop, frame.width) else CropLayerStart()
            memo.animate = discrete && !memo.start.isIdentity
            memo.crop = crop
            memo.ratio = session?.current
            memo.step++
        }
        memo.controlStep = controlStep
        val stepAnim = animateFloatAsState(memo.step.toFloat(), if (memo.animate) motion.move(CivilightMotion.standard) else snap(), label = "cropStep")

        // CNV-45: the tapped tile grows into the frame (scale and move about the frame's centre).
        val from = origin?.let { t -> stageInWindow?.let { stage -> t.translate(-stage.left, -stage.top) } }
        val p = grow.value
        val growLayer = Modifier.graphicsLayer {
            if (motion.reduced || from == null) {
                alpha = if (origin == null) appear else p
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
            if (ready && crop != null) {
                val shown = bitmap!!
                val s = session!!
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .holoSurface(s.holo, sweepTrigger = holoSweep)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val sc = currentScale
                                if (sc > 0f) gesture(pan.x / sc, pan.y / sc, zoom)
                            }
                        }
                        .graphicsLayer {
                            val l = memo.start.at(stepAnim.value - (memo.step - 1))
                            transformOrigin = TransformOrigin((frame.left + frame.width / 2) / w, (frame.top + frame.height / 2) / h)
                            scaleX = l.scale * l.flipX
                            scaleY = l.scale
                            rotationZ = l.rotation
                            translationX = l.translationX
                            translationY = l.translationY
                        },
                ) {
                    val bw = shown.width.toFloat()
                    val bh = shown.height.toFloat()
                    val rotation = CropMath.normalizeRotation(crop.rotation)
                    val rbw = if (rotation % 180 == 90) bh else bw
                    val k = s.sourceW / bw
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
                    }) { drawImage(shown) }
                }
            } else if (tile != null) {
                // The picker's own thumbnail, in the frame, until the original is ready.
                Image(
                    tile,
                    contentDescription = null,
                    modifier = Modifier
                        .offset { IntOffset(frame.left.roundToInt(), frame.top.roundToInt()) }
                        .size(with(density) { frame.width.toDp() }, with(density) { frame.height.toDp() }),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (origin == null) appear else p }) {
            // Reduced motion: the frame's aspect does not move, the old frame crossfades into the new one.
            if (motion.reduced) {
                AnimatedContent(ratioTarget, transitionSpec = { motion.swap(clip = false) }, label = "cropFrame") { r ->
                    FrameOverlay(r, w, h, marginPx, readout?.label)
                }
            } else {
                FrameOverlay(ratio, w, h, marginPx, readout?.label)
            }
        }
    }
}

/** The frame, thirds, corner marks and readout for a frame of [ratio] in a stage of [w] × [h] px. */
@Composable
private fun FrameOverlay(ratio: Float, w: Float, h: Float, marginPx: Float, label: String?) {
    val frame = CropMath.frameIn(w, h, ratio, marginPx)
    Box(Modifier.fillMaxSize()) {
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
        if (label != null) {
            CivilightText(
                label,
                CivilightTheme.type.number.merge(TextStyle(fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center)),
                Modifier.fillMaxWidth().offset { IntOffset(0, (frame.bottom + 10.dp.toPx()).roundToInt()) },
                color = ReadoutColor,
                maxLines = 1,
            )
        }
    }
}
