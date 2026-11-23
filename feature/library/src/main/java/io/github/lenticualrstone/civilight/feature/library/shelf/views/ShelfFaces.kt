package io.github.lenticualrstone.civilight.feature.library.shelf.views

import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ImageCard
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverArt
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverThumb
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectCardUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy

/*
 * What one item of a 서재 3D view looks like. The lock item never draws a project's name or cover (LIB-89,
 * V-04): only the lock mark and the group words of [HiddenGroupRow]. All faces are static: the views move
 * them with `graphicsLayer`, so a scroll frame redraws no face.
 */

/** The look of a face, per view. */
internal enum class FaceKind { COVER, SMALL_COVER, STAR, SHEET, INDEX }

internal fun faceKind(style: io.github.lenticualrstone.civilight.model.app.LibraryStyle, itemW: Float): FaceKind = when (style) {
    io.github.lenticualrstone.civilight.model.app.LibraryStyle.MANUSCRIPT_STACK -> FaceKind.SHEET
    io.github.lenticualrstone.civilight.model.app.LibraryStyle.INDEX_DRAWER -> FaceKind.INDEX
    io.github.lenticualrstone.civilight.model.app.LibraryStyle.CONSTELLATION -> FaceKind.STAR
    else -> if (itemW < 150f) FaceKind.SMALL_COVER else FaceKind.COVER
}

@Composable
internal fun ItemFace(item: ShelfCardItem, kind: FaceKind, itemW: Dp, modifier: Modifier = Modifier) {
    when (item) {
        is ShelfCardItem.Project -> when (kind) {
            FaceKind.COVER, FaceKind.SMALL_COVER -> CoverFace(item.card, itemW, compact = kind == FaceKind.SMALL_COVER, modifier)
            FaceKind.STAR -> StarFace(item.card, itemW, modifier)
            FaceKind.SHEET -> PaperFace(item.card, index = false, modifier)
            FaceKind.INDEX -> PaperFace(item.card, index = true, modifier)
        }
        is ShelfCardItem.Lock -> LockFace(item.group, small = kind == FaceKind.STAR, paper = kind == FaceKind.SHEET || kind == FaceKind.INDEX, modifier)
        ShelfCardItem.NewProject -> NewFace(small = kind == FaceKind.STAR, modifier)
    }
}

@Composable
private fun CoverFace(card: ProjectCardUi, itemW: Dp, compact: Boolean, modifier: Modifier) {
    val targetPx = with(LocalDensity.current) { itemW.roundToPx() }
    ImageCard(
        title = card.row.title,
        meta = card.row.meta.takeUnless { compact },
        statusChip = card.chip.takeUnless { compact },
        compactTitle = compact,
        modifier = modifier.fillMaxSize(),
    ) { CoverArt(card.cover, targetPx) }
}

/** 별자리: a small cover with no words; the caption under the view names the star in front. */
@Composable
private fun StarFace(card: ProjectCardUi, itemW: Dp, modifier: Modifier) {
    val targetPx = with(LocalDensity.current) { itemW.roundToPx() }
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.fillMaxSize().clip(shape).clearAndSetSemantics { }) { CoverArt(card.cover, targetPx) }
}

/** 원고 더미 sheet and 색인 카드 서랍 card: paper with the 48 dp thumbnail, the title, the meta line and ruled lines. */
@Composable
private fun PaperFace(card: ProjectCardUi, index: Boolean, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(if (index) 10.dp else 4.dp)
    val ruled = colors.line
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.edge, 1.dp, shape)
            .drawBehind {
                // Ruled lines under the text (the paper's own look; static).
                val gap = 18.dp.toPx()
                var y = if (index) 64.dp.toPx() else 96.dp.toPx()
                while (y < size.height - 8.dp.toPx()) {
                    drawLine(ruled, Offset(14.dp.toPx(), y), Offset(size.width - 14.dp.toPx(), y), 1.dp.toPx())
                    y += gap
                }
            },
    ) {
        if (index) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CoverThumb(card.row.thumb)
                Column(Modifier.weight(1f)) {
                    CivilightText(card.row.title, type.itemTitleCompact, color = colors.ink, maxLines = 1)
                    CivilightText(card.row.meta, type.small, color = colors.ink3, maxLines = 1)
                }
            }
        } else {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CoverThumb(card.row.thumb)
                CivilightText(card.row.title, type.itemTitleCompact, Modifier.padding(top = 4.dp), color = colors.ink, maxLines = 2)
                CivilightText(card.row.meta, type.small, color = colors.ink3, maxLines = 1)
                card.chip?.let { CivilightText(it, type.small, color = colors.acc, maxLines = 1) }
            }
        }
    }
}

/** The hidden group as one item: lock mark, `숨긴 프로젝트 <M>`, `이름 · 표지 가림` (no name, no cover). */
@Composable
private fun LockFace(group: HiddenGroupRow, small: Boolean, paper: Boolean, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(if (small) 8.dp else if (paper) 8.dp else 20.dp)
    Box(modifier.fillMaxSize().clip(shape).background(colors.tint).insetOutline(colors.edge, 1.dp, shape)) {
        if (small) {
            CivilightIcon(IconId.Lock, contentDescription = null, size = 18.dp, tint = colors.ink2, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                CivilightIcon(IconId.Lock, contentDescription = null, tint = colors.ink2)
                CivilightText(group.title, type.title.merge(TextStyle(fontSize = 18.sp, lineHeight = 24.sp)), Modifier.padding(top = 8.dp), color = colors.ink, maxLines = 1)
                CivilightText(group.cardSub, type.small, color = colors.ink3, maxLines = 1)
            }
        }
    }
}

@Composable
private fun NewFace(small: Boolean, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(if (small) 8.dp else 20.dp)
    Column(
        modifier.fillMaxSize().clip(shape).insetOutline(colors.edge, 1.5.dp, shape),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, tint = colors.ink2)
        if (!small) CivilightText(ShelfCopy.NEW_PROJECT, type.label, color = colors.ink2)
    }
}

/**
 * 3D 책장 spine: a slice of the cover (or the lock mark, or `+`) with the title running down it. Drawn in the
 * middle of the item box, [spineW] wide.
 */
@Composable
internal fun SpineFace(item: ShelfCardItem, spineW: Dp, itemH: Dp, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(4.dp)
    val targetPx = with(LocalDensity.current) { spineW.roundToPx() * 4 }
    Box(modifier.width(spineW).height(itemH).clip(shape).clearAndSetSemantics { }) {
        when (item) {
            is ShelfCardItem.Project -> {
                CoverArt(item.card.cover, targetPx)
                Box(Modifier.fillMaxSize().background(FixedColors.imageShadeBottom.copy(alpha = 0.45f)))
                Box(Modifier.align(Alignment.Center).requiredWidth(itemH - 20.dp).graphicsLayer { rotationZ = 90f }) {
                    CivilightText(item.card.row.title, type.label, color = FixedColors.onImage, maxLines = 1)
                }
            }
            is ShelfCardItem.Lock -> {
                Box(Modifier.fillMaxSize().background(colors.tint).insetOutline(colors.edge, 1.dp, shape))
                CivilightIcon(IconId.Lock, contentDescription = null, size = 16.dp, tint = colors.ink2, modifier = Modifier.align(Alignment.Center))
            }
            ShelfCardItem.NewProject -> {
                Box(Modifier.fillMaxSize().insetOutline(colors.edge, 1.5.dp, shape))
                CivilightIcon(IconId.Plus, contentDescription = null, size = 16.dp, tint = colors.ink2, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

/**
 * DS-74 light: a soft band across the face, shifted by the tilt. The brush is built once per size
 * (`drawWithCache`); a tilt change only redraws this layer.
 */
internal fun Modifier.sheen(tilt: DeviceTilt): Modifier = if (tilt === DeviceTilt.Still) this else drawWithCache {
    val band = size.width * 0.5f
    val brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent), startX = 0f, endX = band)
    onDrawWithContent {
        drawContent()
        val x = size.width * 0.25f + tilt.x * size.width * 0.3f
        drawContext.transform.translate(x, 0f)
        drawRect(brush, topLeft = Offset.Zero, size = Size(band, size.height))
        drawContext.transform.translate(-x, 0f)
    }
}
