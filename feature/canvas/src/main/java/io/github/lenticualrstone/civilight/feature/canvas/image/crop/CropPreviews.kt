package io.github.lenticualrstone.civilight.feature.canvas.image.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.canvas.image.CropMath
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.model.asset.Crop

/** Height of a ratio preview (the width follows the ratio). */
private val PREVIEW_H = 56.dp

/**
 * The other ratios of the slot as small previews (CST-13): each shows the crop `적용` will save, labelled
 * `아바타 1:1 · 자동` (follows the first ratio) or `· 직접` (its own crop). Tapping one opens it in the stage,
 * the same step as its ratio chip. Nothing for a one-ratio slot or a gallery item.
 */
@Composable
internal fun CropPreviews(session: CropSession, bitmap: ImageBitmap?, onSelect: (CropSession) -> Unit) {
    val others = session.ratios.filter { it != session.current }
    if (session.target != io.github.lenticualrstone.civilight.feature.canvas.image.CropTarget.SLOT || others.isEmpty()) return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        others.forEach { ratio ->
            val crop = session.cropOf(ratio)
            val label = ImageCopy.cropPreview(session.chip(ratio), session.isAuto(ratio))
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClickLabel = label) { onSelect(session.select(ratio)) },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .height(PREVIEW_H)
                        .aspectRatio(crop.w.toFloat() / crop.h.coerceAtLeast(1))
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.chip),
                ) {
                    if (bitmap != null) CropThumb(bitmap, session.sourceW, crop)
                }
                CivilightText(label, type.small, color = colors.ink2, maxLines = 1)
            }
        }
    }
}

/** [crop] of the original drawn from the stage [bitmap]: same order as the stage (rotate, mirror, cut). */
@Composable
private fun CropThumb(bitmap: ImageBitmap, sourceW: Int, crop: Crop) {
    Canvas(Modifier.fillMaxWidth().aspectRatio(crop.w.toFloat() / crop.h.coerceAtLeast(1))) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0f || bh <= 0f || crop.w <= 0) return@Canvas
        val scale = size.width / crop.w
        val k = sourceW / bw
        val rotation = CropMath.normalizeRotation(crop.rotation)
        val rbw = if (rotation % 180 == 90) bh else bw
        withTransform({
            translate(-crop.x * scale, -crop.y * scale)
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
