package io.github.lenticualrstone.civilight.feature.canvas.image

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageSlot

/**
 * What the picker hands to the crop editor beyond the route's arguments (CNV-45 `고르기 → 자르기`): the
 * tapped tile's bounds in the window and its decoded thumbnail (the image grows from there, from the first
 * frame, while the original still decodes) and the picker's holo toggle. It lives
 * in memory only and is taken once; after process death the editor opens without the motion and with
 * the slot's own holo value.
 */
data class CropHandOff(
    val ownerKey: Key,
    val slot: ImageSlot,
    val blobHash: String,
    val tileBounds: Rect?,
    val holo: Boolean,
    /** The picker tile's decoded thumbnail, so the crop screen can draw and grow it from its first frame (CNV-45). */
    val tileBitmap: ImageBitmap? = null,
    /** A line shown above the crop controls: `숨긴 이미지 · 초상에서는 보임` when a hidden gallery image becomes a main image (GAL-14). */
    val note: String? = null,
) {
    companion object {
        @Volatile private var pending: CropHandOff? = null

        fun put(value: CropHandOff) {
            pending = value
        }

        /** The hand-off for this crop, once; a hand-off for another image is dropped. */
        fun take(ownerKey: Key, slot: ImageSlot, blobHash: String): CropHandOff? {
            val p = pending ?: return null
            pending = null
            return p.takeIf { it.ownerKey == ownerKey && it.slot == slot && it.blobHash == blobHash }
        }
    }
}
