package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.ui.image.CoverBlobImage
import io.github.lenticualrstone.civilight.ui.image.CoverInitialStyle
import io.github.lenticualrstone.civilight.ui.image.CoverThumb as SharedCoverThumb
import io.github.lenticualrstone.civilight.ui.image.CoverArt as SharedCoverArt

/*
 * The cover drawing moved to `core:ui` `image/ProjectCover.kt` (U-08 §7.6), so the 서재 and the project
 * home head draw the same cover. These keep the 서재's call sites unchanged.
 */

/** A picked image from the blob store, faded in on `standard` once decoded (audit row 9). */
@Composable
internal fun BlobImage(hash: String, crop: Crop?, targetWidth: Int, modifier: Modifier = Modifier) =
    CoverBlobImage(hash, crop, targetWidth, modifier)

/** The 48 dp thumbnail of a 서재 row (D6; CNV-45 `MCardView`): radius 10, the cover's 1:1 crop, a preset, or the initial. */
@Composable
internal fun CoverThumb(cover: CoverUi, modifier: Modifier = Modifier) = SharedCoverThumb(cover, modifier)

/**
 * A project card's cover (LIB-04): the picked image, the preset's own picture (CNV-26, U-08 LIB-88), or
 * the initial-letter card; the holo foil when on.
 */
@Composable
internal fun BoxScope.CoverArt(cover: CoverUi, targetWidth: Int) = SharedCoverArt(cover, targetWidth, CoverInitialStyle.CARD)
