package io.github.lenticualrstone.civilight.feature.map.sketch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.MapVertex
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.mapAt
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot

/**
 * `스케치 촬영 → 펴기` (WLD-39): the photo with four corner handles. A handle can be dragged, or
 * tapped (it turns `acc`) and then placed by tapping the photo, so dragging is never the only way.
 * `펴기` straightens; the original photo is kept with the map.
 */
@Composable
fun StraightenScreen(
    photo: Blob,
    blobs: BlobStore,
    working: Boolean,
    onStraighten: (List<Point>) -> Unit,
    onCancel: () -> Unit,
) {
    var cornersArray by rememberSaveable(photo.hash) { mutableStateOf(Straighten.toArray(Straighten.defaultCorners())) }
    var chosen by rememberSaveable(photo.hash) { mutableStateOf(-1) }
    val corners = Straighten.fromArray(cornersArray)
    var image by remember(photo.hash) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photo.hash) {
        image = withContext(Dispatchers.IO) { blobs.fileOf(photo)?.let { SketchImages.decodeUpright(it, 2048) }?.asImageBitmap() }
    }
    CivilightScreen(
        title = MapCopy.STRAIGHTEN,
        smallLine = MapCopy.SKETCH_TITLE,
        back = IconAction(MapCopy.BTN_CANCEL, onCancel),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)) {
            CivilightText(
                "${MapCopy.STRAIGHTEN}${MapCopy.SEP}${MapCopy.TRACE}",
                CivilightTheme.type.small,
                Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = CivilightTheme.colors.ink3,
            )
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                val bmp = image
                if (bmp != null) {
                    PhotoWithHandles(
                        image = bmp,
                        corners = corners,
                        chosen = chosen,
                        onChoose = { chosen = it },
                        onMove = { i, p -> cornersArray = Straighten.toArray(Straighten.move(corners, i, p)) },
                    )
                }
            }
            ButtonRow(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                CivilightButton(MapCopy.BTN_CANCEL, onCancel, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                CivilightButton(MapCopy.BTN_STRAIGHTEN, { onStraighten(corners) }, Modifier.weight(1f), enabled = image != null && !working)
            }
        }
    }
}

@Composable
private fun PhotoWithHandles(
    image: ImageBitmap,
    corners: List<Point>,
    chosen: Int,
    onChoose: (Int) -> Unit,
    onMove: (Int, Point) -> Unit,
) {
    val aspect = image.width.toFloat() / image.height
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val (w, h) = if (maxW / maxH > aspect) maxH * aspect to maxH else maxW to maxW / aspect
        val wDp = with(density) { w.toDp() }
        val hDp = with(density) { h.toDp() }
        val touch = with(density) { 44.dp.toPx() }
        val acc = CivilightTheme.colors.acc
        val now by rememberUpdatedState(corners)
        val chosenNow by rememberUpdatedState(chosen)
        Box(
            Modifier
                .size(wDp, hDp)
                .pointerInput(w, h) {
                    detectTapGestures { o ->
                        val hit = now.indices.firstOrNull { hypot(now[it].x * w - o.x, now[it].y * h - o.y) <= touch / 2 }
                        when {
                            hit != null -> onChoose(if (hit == chosenNow) -1 else hit)
                            chosenNow >= 0 -> onMove(chosenNow, Point(o.x / w, o.y / h))
                        }
                    }
                }
                .pointerInput(w, h) {
                    var dragging = -1
                    detectDragGestures(
                        onDragStart = { o -> dragging = now.indices.firstOrNull { hypot(now[it].x * w - o.x, now[it].y * h - o.y) <= touch / 2 } ?: -1 },
                        onDragEnd = { dragging = -1 },
                    ) { change, _ ->
                        if (dragging >= 0) {
                            change.consume()
                            onMove(dragging, Point(change.position.x / w, change.position.y / h))
                        }
                    }
                },
        ) {
            Image(image, contentDescription = MapCopy.SKETCH_TITLE, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Canvas(Modifier.fillMaxSize()) {
                val path = Path()
                corners.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x * w, p.y * h) else path.lineTo(p.x * w, p.y * h) }
                path.close()
                drawPath(path, acc, style = Stroke(2.dp.toPx()))
            }
            corners.forEachIndexed { i, p ->
                MapVertex(Modifier.mapAt(p.x * w, p.y * h), last = i == chosen)
            }
        }
    }
}
