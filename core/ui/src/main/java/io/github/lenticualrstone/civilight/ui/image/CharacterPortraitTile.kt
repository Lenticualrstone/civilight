package io.github.lenticualrstone.civilight.ui.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProfileShape
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * (0.4.0) Shared 1:1 character portrait tile usable across `:feature:script` and `:feature:cast`.
 * Loads `CHARACTER_PORTRAIT` (`Ratio.R1X1`) via shared [ImageCache] / [ImageBitmaps] with zero main-thread I/O,
 * renders as a 1:1 rounded square or circle per [shape], and falls back to a tinted initial monogram.
 */
@Composable
fun CharacterPortraitTile(
    doc: ProjectDoc?,
    characterKey: Key?,
    displayName: String,
    shape: ProfileShape = ProfileShape.SQUARE,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val tileShape = if (shape == ProfileShape.CIRCLE) CircleShape else RoundedCornerShape(10.dp)
    val deps = LocalFeatureDeps.current
    val density = LocalDensity.current
    val targetPx = with(density) { (size * 2).roundToPx().coerceAtLeast(96) }

    val imageUse = remember(doc, characterKey) {
        if (doc != null && characterKey != null) {
            doc.index.imageUse(ImageOwnerKind.CHARACTER, characterKey, ImageSlot.CHARACTER_PORTRAIT)
        } else null
    }
    val blobHash = imageUse?.blobHash
    val crop = imageUse?.cropFor(Ratio.R1X1)

    val bitmap by produceState<ImageBitmap?>(
        initialValue = blobHash?.let { ImageCache.get(ImageCache.key(it, crop, targetPx)) },
        key1 = blobHash,
        key2 = crop,
        key3 = targetPx,
    ) {
        if (blobHash == null || deps == null) {
            value = null
            return@produceState
        }
        val cached = ImageCache.get(ImageCache.key(blobHash, crop, targetPx))
        if (cached != null) {
            value = cached
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = deps.stores.blobs.fileOf(blobHash) ?: return@runCatching null
                if (file.exists()) ImageBitmaps.getOrDecode(blobHash, file, crop, targetPx) else null
            }.getOrNull()
        }
    }

    val tone = remember(characterKey, displayName, colors.isDark) {
        portraitTone(characterKey ?: displayName, colors.isDark)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(tileShape)
            .background(tone)
            .insetOutline(colors.line, 1.dp, tileShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val img = bitmap
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (characterKey == null || displayName.isBlank()) {
            CivilightIcon(IconId.User, contentDescription = null, size = size * 0.44f, tint = colors.ink3)
        } else {
            val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            CivilightText(
                initial,
                TextStyle(fontSize = (size.value * 0.38f).sp, lineHeight = (size.value * 0.44f).sp, fontWeight = FontWeight.Bold),
                color = colors.ink2,
                maxLines = 1,
            )
        }
    }
}

private fun portraitTone(seed: String, isDark: Boolean): Color {
    val palette = if (isDark) {
        listOf(
            Color(0xFF222D3D),
            Color(0xFF33262A),
            Color(0xFF213128),
            Color(0xFF2C273A),
            Color(0xFF312E22),
            Color(0xFF1F2F35),
        )
    } else {
        listOf(
            Color(0xFFDEE7F2),
            Color(0xFFEEE0DB),
            Color(0xFFDCEAE0),
            Color(0xFFE6E1F2),
            Color(0xFFEEE8D8),
            Color(0xFFD9EAEF),
        )
    }
    return palette[abs(seed.hashCode()) % palette.size]
}
