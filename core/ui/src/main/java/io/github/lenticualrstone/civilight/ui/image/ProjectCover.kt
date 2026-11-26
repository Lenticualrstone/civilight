package io.github.lenticualrstone.civilight.ui.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.holoImage
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.gallery.PresetPicture
import io.github.lenticualrstone.civilight.ui.gallery.PresetPictures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.BreakIterator

/**
 * A project's cover as drawn (LIB-04, U-08 LIB-80 · LIB-88): the picked original ([hash] + the crop of
 * the surface's ratio), a preset ([presetId]), or neither: the initial-letter card ([initial]). [holo]
 * adds the foil. Shared by the 서재 (`feature:library`) and the project home head (`feature:story`).
 */
data class CoverUi(val hash: String?, val crop: Crop?, val presetId: String?, val initial: String, val holo: Boolean) {
    /** A picked original is drawn (not a preset, not the initial-letter card): the LIB-89 capture rule keys on it. */
    val isImage: Boolean get() = hash != null
}

/**
 * The pure reading of the cover row `ImageUse(project, <project key>, projectCover)` (C-10; no project
 * column). A preset whose id this version does not know, and an original whose file is not on this device
 * ([fileExists], LIB-88 "missing original"), draw the initial-letter card.
 *
 * Masking is the caller's: a surface that does not show the real project name must not build a
 * [CoverUi] at all (LIB-89).
 */
object ProjectCovers {
    /** The cover row of [doc], or null. */
    fun use(doc: ProjectDoc?): ImageUse? = doc?.index?.imageUse(ImageOwnerKind.PROJECT, doc.key, ImageSlot.PROJECT_COVER)

    /** The 3:4 cover (서재 card, home head, `preview.webp`). */
    fun cover(doc: ProjectDoc?, name: String, fileExists: (String) -> Boolean = { true }): CoverUi = of(doc, name, Ratio.R3X4, fileExists)

    /** The 1:1 cover (48 dp list thumbnail, the picker's current line). */
    fun thumb(doc: ProjectDoc?, name: String, fileExists: (String) -> Boolean = { true }): CoverUi = of(doc, name, Ratio.R1X1, fileExists)

    /** The first grapheme of the trimmed [name] (the initial-letter card, CNV-24). */
    fun initialOf(name: String): String {
        val text = name.trim()
        if (text.isEmpty()) return ""
        val it = BreakIterator.getCharacterInstance().apply { setText(text) }
        val end = it.next().takeIf { e -> e != BreakIterator.DONE } ?: text.length
        return text.substring(0, end)
    }

    private fun of(doc: ProjectDoc?, name: String, ratio: Ratio, fileExists: (String) -> Boolean): CoverUi {
        val initial = initialOf(name)
        val none = CoverUi(null, null, null, initial, holo = false)
        val use = use(doc) ?: return none
        use.presetId?.let { id ->
            return if (PresetPictures.IDS.contains(id)) CoverUi(null, null, id, initial, use.holo) else none
        }
        val hash = use.blobHash ?: return none
        if (!fileExists(hash)) return none
        return CoverUi(hash, ImageCrops.cropFor(use, ratio), null, initial, use.holo)
    }
}

/** Decodes a cover image off the main thread, EXIF-upright, sampled safely to [targetWidth] px and cropped. */
object CoverBitmaps {
    fun decode(file: File, crop: Crop?, targetWidth: Int): Bitmap? =
        ImageBitmaps.decodeUprightSampled(file, crop, targetWidth)
}

/**
 * A picked original from the blob store. Uses the shared [ImageCache] so scrolling never re-decodes
 * from disk on re-entry; decoded pictures stay in memory only (LIB-90).
 */
@Composable
fun CoverBlobImage(hash: String, crop: Crop?, targetWidth: Int, modifier: Modifier = Modifier) {
    val deps = LocalFeatureDeps.current
    val motion = CivilightTheme.motion
    val safeWidth = targetWidth.coerceAtLeast(1)
    val initial = remember(hash, crop, safeWidth) { ImageCache.get(ImageCache.key(hash, crop, safeWidth)) }
    val bitmap by produceState(initial, hash, crop, safeWidth) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            deps.stores.blobs.fileOf(hash)?.let { runCatching { ImageBitmaps.getOrDecode(hash, it, crop, safeWidth) }.getOrNull() }
        }
    }
    Crossfade(bitmap, modifier.fillMaxSize(), animationSpec = motion.spec(CivilightMotion.standard), label = "coverImage") { image ->
        if (image != null) Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/** The preset picture of [presetId] (CNV-26), or nothing for an unknown id. */
@Composable
fun CoverPreset(presetId: String, modifier: Modifier = Modifier) {
    val index = PresetPictures.IDS.indexOf(presetId)
    if (index >= 0) PresetPicture(index, modifier)
}

/**
 * The 48 dp 1:1 thumbnail (서재 row D6, the cover picker's current line): radius 10, the picture, a preset,
 * or the initial in 20 sp `ink3` on `chip`. No foil and no badge at this size (LIB-88).
 */
@Composable
fun CoverThumb(cover: CoverUi, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val px = with(LocalDensity.current) { CardViewMotion.THUMB_DP.dp.roundToPx() }
    Box(modifier.size(CardViewMotion.THUMB_DP.dp).clip(RoundedCornerShape(CardViewMotion.THUMB_RADIUS_DP.dp)).background(colors.chip)) {
        when {
            cover.hash != null -> CoverBlobImage(cover.hash, cover.crop, px)
            cover.presetId != null -> CoverPreset(cover.presetId)
            else -> CivilightText(
                cover.initial,
                CivilightTheme.type.title.merge(TextStyle(fontSize = 20.sp, lineHeight = 24.sp)),
                Modifier.align(Alignment.Center),
                color = colors.ink3,
                maxLines = 1,
            )
        }
    }
}

/** How the initial-letter card is drawn on a larger cover. */
enum class CoverInitialStyle {
    /** 서재 card (LIB-04): 64 sp at the top left, light ink for the bottom shade. */
    CARD,

    /** Project home head (LIB-83): 40 sp centred, `ink3` on `tint`. */
    HEAD,
}

/**
 * A 3:4 cover filling its box (서재 card LIB-04, home head LIB-83): the picture, the preset's real
 * picture, or the initial-letter card; the holo foil when on. No text over a picture.
 */
@Composable
fun BoxScope.CoverArt(cover: CoverUi, targetWidth: Int, initialStyle: CoverInitialStyle = CoverInitialStyle.CARD) {
    val colors = CivilightTheme.colors
    // The live foil (tilt, drag, idle sway; still under reduced motion), never on the initial-letter card.
    val foil = Modifier.holoImage(cover.holo && (cover.hash != null || cover.presetId != null))
    val fill = if (cover.hash == null && cover.presetId == null && initialStyle == CoverInitialStyle.HEAD) colors.tint else colors.chip
    Box(Modifier.matchParentSize().then(foil).background(fill)) {
        when {
            cover.hash != null -> CoverBlobImage(cover.hash, cover.crop, targetWidth)
            cover.presetId != null -> CoverPreset(cover.presetId)
            initialStyle == CoverInitialStyle.HEAD -> CivilightText(
                cover.initial,
                CivilightTheme.type.title.merge(TextStyle(fontSize = 40.sp, lineHeight = 48.sp)),
                Modifier.align(Alignment.Center),
                color = colors.ink3,
                maxLines = 1,
            )
            else -> CivilightText(
                cover.initial,
                CivilightTheme.type.title.merge(TextStyle(fontSize = 64.sp, lineHeight = 72.sp)),
                Modifier.align(Alignment.TopStart).padding(18.dp),
                color = FixedColors.onImage2,
            )
        }
    }
}
