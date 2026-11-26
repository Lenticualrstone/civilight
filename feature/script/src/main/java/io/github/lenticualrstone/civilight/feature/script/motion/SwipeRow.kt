package io.github.lenticualrstone.civilight.feature.script.motion

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

// (0.5) `StaggeredSwipeRow` (the old one-sided block swipe) was removed: no caller since `TwoSidedSwipeRow`.

/**
 * The swipe row's finger state (CNV-40), plain so it is JVM-tested: [offset] in px (0 = closed,
 * −[widthPx] = open), written synchronously by each drag delta; [release] decides open or closed
 * (past half stays open) and bumps [releases] so the settle runs even when the state did not change.
 */
class SwipeDrag(open: Boolean, var widthPx: Float) {
    private val offsetState = mutableFloatStateOf(anchor(open, widthPx))
    private val fadeState = mutableFloatStateOf(if (open) 1f else 0f)
    private val draggingState = mutableStateOf(false)
    private val releasesState = mutableIntStateOf(0)

    /** Where the block is drawn, in px. */
    val offset: Float get() = offsetState.floatValue

    /** The buttons' fade under reduced motion (follows the finger, then its own run). */
    val fade: Float get() = fadeState.floatValue

    val dragging: Boolean get() = draggingState.value
    val releases: Int get() = releasesState.intValue

    /** 0 = closed, 1 = fully open. */
    val progress: Float get() = if (widthPx <= 0f) 0f else (-offset / widthPx).coerceIn(0f, 1f)

    /** One drag delta (px, negative = left), applied at once and kept between open and closed. */
    fun onDelta(delta: Float) {
        draggingState.value = true
        offsetState.floatValue = (offset + delta).coerceIn(-widthPx, 0f)
        fadeState.floatValue = progress
    }

    /** The finger lifted: true = settle open. */
    fun release(): Boolean {
        draggingState.value = false
        releasesState.intValue++
        return opensOnRelease(offset, widthPx)
    }

    /** A drag that ends without a release (the row turned off): settles back to the open state as it was. */
    fun cancel() {
        if (!draggingState.value) return
        draggingState.value = false
        releasesState.intValue++
    }

    /** The settle animation's frame; ignored while a finger holds the row (the drag wins at once). */
    fun place(value: Float) {
        if (draggingState.value) return
        offsetState.floatValue = value.coerceIn(-widthPx, 0f)
    }

    /** The reduced-motion fade's frame; ignored while a finger holds the row. */
    fun placeFade(value: Float) {
        if (draggingState.value) return
        fadeState.floatValue = value.coerceIn(0f, 1f)
    }

    companion object {
        fun anchor(open: Boolean, widthPx: Float): Float = if (open) -widthPx else 0f

        /** Released past half the buttons' width stays open. */
        fun opensOnRelease(offset: Float, widthPx: Float): Boolean = offset < -widthPx / 2
    }
}
