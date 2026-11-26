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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import kotlin.math.cos
import kotlin.math.sin
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
 * Rule 19: a hidden character's portrait is never drawn while the project's hidden items are locked.
 * Hidden = the row's `숨기기`, or the name tag's for a trashed or purged speaker. Pure, for JVM tests.
 */
object PortraitGate {
    fun isHidden(doc: ProjectDoc, characterKey: Key): Boolean =
        doc.index.character(characterKey)?.hidden == true || doc.index.nameTag(characterKey)?.hidden == true

    /** The portrait (and anything that opens the card from it) is held back. */
    fun isLocked(doc: ProjectDoc?, characterKey: Key?, itemsOpen: Boolean): Boolean =
        doc != null && characterKey != null && !itemsOpen && isHidden(doc, characterKey)
}

/** [PortraitGate.isLocked] with the shell's unlock state; no shell (previews) counts as locked. */
@Composable
fun rememberPortraitLocked(doc: ProjectDoc?, characterKey: Key?): Boolean {
    val deps = LocalFeatureDeps.current
    val open = deps?.unlock?.state?.collectAsState()?.value?.let { s -> doc?.key?.let(s::itemsOpen) } ?: false
    return PortraitGate.isLocked(doc, characterKey, open)
}

/**
 * (0.5.3) Shared 1:1 character portrait painter usable across `:core:ui`, `:feature:cast`, `:feature:script`, and `:feature:map`.
 * Resolves both built-in preset portraits (`presetId`) and uploaded blob portraits (`blobHash` with `Ratio.R1X1` crop)
 * via [ImageCache] / [ImageBitmaps] with zero main-thread I/O. Returns null when the character has no live portrait.
 */
@Composable
fun rememberCharacterAvatarPainter(
    doc: ProjectDoc?,
    characterKey: Key?,
    targetPx: Int = 192,
): Painter? {
    val deps = LocalFeatureDeps.current
    // Rule 19: a locked hidden character has no portrait at the source, so every caller is covered.
    val locked = rememberPortraitLocked(doc, characterKey)
    val imageUse = remember(doc, characterKey, locked) {
        if (doc != null && characterKey != null && !locked) {
            doc.index.imageUse(ImageOwnerKind.CHARACTER, characterKey, ImageSlot.CHARACTER_PORTRAIT)
                ?.takeIf { doc.liveness.isLive(it) }
        } else null
    }
    val presetId = imageUse?.presetId
    if (presetId != null) {
        val idx = remember(presetId) { PRESET_IDS.indexOf(presetId).takeIf { it >= 0 } ?: 0 }
        return remember(idx) { PresetShardPainter(idx) }
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
    return bitmap?.let { remember(it) { BitmapPainter(it) } }
}

/**
 * (0.4.0) Shared 1:1 character portrait tile usable across `:feature:script` and `:feature:cast`.
 * Loads `CHARACTER_PORTRAIT` (`Ratio.R1X1`) — both built-in presets and uploaded blobs — via
 * [rememberCharacterAvatarPainter] with zero main-thread I/O, renders as a 1:1 rounded square or
 * circle per [shape], and falls back to a tinted initial monogram.
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
    val density = LocalDensity.current
    val targetPx = with(density) { (size * 2).roundToPx().coerceAtLeast(96) }
    val painter = rememberCharacterAvatarPainter(doc, characterKey, targetPx)
    // Rule 19: a locked hidden character shows the plain icon on a neutral tone and opens nothing.
    val locked = rememberPortraitLocked(doc, characterKey)
    val click = onClick.takeUnless { locked }

    val tone = remember(characterKey, displayName, colors.isDark, locked) {
        if (locked) colors.tint else portraitTone(characterKey ?: displayName, colors.isDark)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(tileShape)
            .background(tone)
            .insetOutline(colors.line, 1.dp, tileShape)
            .then(
                if (click != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = click,
                    )
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (locked || characterKey == null || displayName.isBlank()) {
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

private val PRESET_IDS = listOf(
    "ash-dawn", "lantern", "deep-sea", "mist-forest",
    "desert-ruin", "nebula", "red-citadel", "snowfield",
)

private class PresetShardPainter(index: Int) : Painter() {
    private val palette = PALETTES[index.mod(PALETTES.size)]
    private val shards = SHARDS[index.mod(SHARDS.size)]

    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val angle = Math.toRadians(165.0 - 90.0)
        val dx = (cos(angle) * w / 2).toFloat()
        val dy = (sin(angle) * h / 2).toFloat()
        val c = Offset(w / 2, h / 2)
        drawRect(Brush.linearGradient(listOf(palette[0], palette[1]), start = c - Offset(dx, dy), end = c + Offset(dx, dy)))
        shards.forEachIndexed { i, poly ->
            val path = Path().apply {
                poly.forEachIndexed { j, (x, y) -> if (j == 0) moveTo(x * w, y * h) else lineTo(x * w, y * h) }
                close()
            }
            drawPath(path, palette[2 + i], alpha = if (i == 1) 1f else 0.9f)
        }
    }

    private companion object {
        fun hex(v: Long) = Color(0xFF000000 or v)

        val PALETTES = listOf(
            listOf(hex(0x2B2F3A), hex(0x6B6F7C), hex(0xA3A7B3), hex(0x4A4F5C), hex(0xD9C7A3)),
            listOf(hex(0x1D1A17), hex(0x4A3A28), hex(0xC8913F), hex(0x6E5236), hex(0x2C241C)),
            listOf(hex(0x06131F), hex(0x123B56), hex(0x2F6F8F), hex(0x0B2536), hex(0x7FB3C8)),
            listOf(hex(0x1E2A24), hex(0x4E6656), hex(0x8BA593), hex(0x2E3E34), hex(0xC9D4C3)),
            listOf(hex(0x3A2A1E), hex(0xA57A52), hex(0xD8B98C), hex(0x6B4A30), hex(0xF0DEC0)),
            listOf(hex(0x120F24), hex(0x3B2E5C), hex(0x7A64A8), hex(0x241B3D), hex(0xC7B6E6)),
            listOf(hex(0x240E0E), hex(0x6A2420), hex(0xA9483C), hex(0x3D1715), hex(0xE0A08A)),
            listOf(hex(0x8C98A4), hex(0xE4E9EE), hex(0xB7C2CC), hex(0x6E7D8A), hex(0xF6F8FA)),
        )

        val SHARDS: List<List<List<Pair<Float, Float>>>> = listOf(
            listOf(
                listOf(0f to .58f, .55f to .30f, 1f to .46f, 1f to .72f, 0f to .86f),
                listOf(0f to .74f, .40f to .56f, 1f to .68f, 1f to 1f, 0f to 1f),
                listOf(.58f to .08f, .78f to 0f, .70f to .44f, .52f to .52f),
            ),
            listOf(
                listOf(0f to .40f, 1f to .18f, 1f to .60f, 0f to .70f),
                listOf(0f to .66f, .70f to .50f, 1f to .80f, 1f to 1f, 0f to 1f),
                listOf(.12f to .10f, .30f to .06f, .26f to .34f, .08f to .30f),
            ),
            listOf(
                listOf(.20f to 0f, .60f to 0f, .90f to 1f, .40f to 1f),
                listOf(0f to .60f, 1f to .42f, 1f to 1f, 0f to 1f),
                listOf(.62f to .20f, .84f to .16f, .80f to .40f, .60f to .42f),
            ),
            listOf(
                listOf(0f to .30f, .45f to .12f, 1f to .36f, 1f to .64f, .50f to .54f, 0f to .70f),
                listOf(0f to .78f, 1f to .58f, 1f to 1f, 0f to 1f),
                listOf(.40f to .60f, .56f to .52f, .60f to .72f, .44f to .78f),
            ),
        )
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
