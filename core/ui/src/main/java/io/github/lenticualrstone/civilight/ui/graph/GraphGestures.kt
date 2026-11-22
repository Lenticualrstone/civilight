package io.github.lenticualrstone.civilight.ui.graph

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged

/** Zoom factor of one mouse-wheel notch (desktop windows and Chromebooks). */
private const val WHEEL_STEP = 1.1f

private enum class GestureMode { PENDING, DRAG_NODE, TRANSFORM, IGNORED }

/**
 * The graph's gestures (CNV-01, hint `두 손가락 이동 · 확대 · 노드는 끌어서 이동`): two fingers pan and
 * pinch, one finger on a node drags it, a press that does not move is a tap. One finger on empty
 * space does nothing, so a stray touch never moves the view. A mouse wheel zooms at the pointer.
 */
fun Modifier.graphGestures(
    density: Float,
    sceneOf: () -> GraphScene,
    viewportOf: () -> GraphViewport,
    sizes: Map<String, Sz>,
    listenerOf: () -> GraphSurfaceListener,
    onPinch: (GraphViewport) -> Unit = {},
): Modifier = this
    .pointerInput(Unit) {
        val slop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = viewportOf()
            val startDp = Pt(down.position.x / density, down.position.y / density)
            val at = start.toContent(startDp)
            val hit = GraphGeometry.hit(sceneOf(), sizes, at, start.zoom)
            val dragKey = when (hit) {
                is GraphHit.Node -> hit.key
                is GraphHit.Frame -> hit.key
                else -> null
            }?.takeIf { listenerOf().canDrag(it) }
            var mode = GestureMode.PENDING
            var current = start
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                if (pressed.size >= 2) {
                    if (mode == GestureMode.DRAG_NODE && dragKey != null) listenerOf().onDragEnd(dragKey)
                    mode = GestureMode.TRANSFORM
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    val centroid = event.calculateCentroid(useCurrent = true)
                    current = current
                        .panBy(pan.x / density, pan.y / density)
                        .zoomAround(Pt(centroid.x / density, centroid.y / density), zoom)
                    onPinch(current)
                    listenerOf().onViewport(current)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                } else if (mode == GestureMode.PENDING || mode == GestureMode.DRAG_NODE) {
                    val change = pressed.first()
                    if (mode == GestureMode.PENDING && (change.position - down.position).getDistance() > slop) {
                        mode = if (dragKey != null) GestureMode.DRAG_NODE else GestureMode.IGNORED
                    }
                    if (mode == GestureMode.DRAG_NODE && dragKey != null) {
                        val d = change.positionChange()
                        listenerOf().onDrag(dragKey, Pt(d.x / density / current.zoom, d.y / density / current.zoom))
                        change.consume()
                    }
                }
            }
            when (mode) {
                GestureMode.PENDING -> listenerOf().onTap(hit, at)
                GestureMode.DRAG_NODE -> dragKey?.let { listenerOf().onDragEnd(it) }
                GestureMode.TRANSFORM -> listenerOf().onViewportSettled(current)
                GestureMode.IGNORED -> Unit
            }
        }
    }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                val dy = change.scrollDelta.y
                if (dy == 0f) continue
                val focus = Pt(change.position.x / density, change.position.y / density)
                val next = viewportOf().zoomAround(focus, if (dy < 0f) WHEEL_STEP else 1f / WHEEL_STEP)
                listenerOf().onViewport(next)
                listenerOf().onViewportSettled(next)
                change.consume()
            }
        }
    }
