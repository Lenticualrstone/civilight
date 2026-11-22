package io.github.lenticualrstone.civilight.feature.cast.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.SnapCardRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.list.CardGroup
import io.github.lenticualrstone.civilight.domain.cast.list.CastCard
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.image.CharacterImages
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.image.rememberSlotPainter
import io.github.lenticualrstone.civilight.model.Key

/** The side padding of the list screen's own content; card rows run past it to the screen edge (QA 7). */
internal val CAST_SIDE = 24.dp

/** Card sizes of `CastCards`: the first group 196 × 245, later groups 132 × 165 (portrait 4:5). */
fun cardWidth(groupIndex: Int): Dp = if (groupIndex == 0) 196.dp else 132.dp

/** A card row shows pager marks only while they stay readable (CNV-22: 12 cards or fewer). */
private const val PAGER_MAX = 12

/**
 * One snapping row of cards (`CastCards`, CST-04) on the shared `SnapCardRow`: `<tag> · <count>`, then
 * cards with the portrait's 4:5 crop (or the first letter and `이미지 고르기`), title, first appearance and
 * subtitle. `홀로` marks a portrait with the holo effect; `갤러리 <N>` (GAL-02) opens that character's
 * gallery. The row is full-bleed (QA 7): it starts at the content inset and the next card peeks out at the
 * screen edge. [modifier] is the lazy item's (`civilightItem()`).
 */
@Composable
internal fun CastCardRow(
    index: Int,
    group: CardGroup,
    inner: Boolean,
    imageOf: (Key) -> SlotImage?,
    galleryCounts: Map<Key, Int>,
    onOpen: (Key) -> Unit,
    onGallery: (Key) -> Unit,
    onPickImage: ((Key) -> Unit)?,
    modifier: Modifier = Modifier,
    shared: CastShared = CastShared.None,
    unhidden: (Key) -> Modifier = { Modifier },
) {
    // The row's own inset is 20 (cover) or 32 (inner); the cast screens keep 24, so cover rows start 4 dp in.
    val start = (CAST_SIDE.value - CardMetrics.inset(inner)).coerceAtLeast(0f).dp
    SnapCardRow(
        items = group.cards,
        key = { it.key },
        kind = if (index == 0) CardKind.PORTRAIT else CardKind.PORTRAIT_SMALL,
        inner = inner,
        modifier = modifier.padding(start = start, top = if (index == 0) 18.dp else 14.dp),
        title = group.header,
        showPager = group.cards.size in 2..PAGER_MAX,
    ) { card, imageModifier ->
        CastCardView(
            card = card,
            width = cardWidth(index),
            image = imageOf(card.key),
            galleryCount = galleryCounts[card.key] ?: 0,
            imageModifier = imageModifier,
            portraitShare = shared.portrait(group.tagKey, card.key, radiusDp = 14f),
            modifier = unhidden(card.key),
            onClick = { onOpen(card.key) },
            onGallery = { onGallery(card.key) },
            onPickImage = onPickImage?.let { pick -> { pick(card.key) } },
        )
    }
}

@Composable
private fun CastCardView(
    card: CastCard,
    width: Dp,
    image: SlotImage?,
    galleryCount: Int,
    imageModifier: Modifier,
    portraitShare: Modifier,
    modifier: Modifier,
    onClick: () -> Unit,
    onGallery: () -> Unit,
    onPickImage: (() -> Unit)?,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    val painter = rememberSlotPainter(image)
    Column(modifier.width(width).pressable(onClick = onClick, scaleOnPress = false, onClickLabel = card.title)) {
        Box(Modifier.size(width, width * 5 / 4).then(portraitShare).clip(shape).background(colors.tint)) {
            if (painter != null) {
                Image(painter, contentDescription = null, modifier = imageModifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                CivilightText(card.initial, type.display, Modifier.align(Alignment.Center), color = colors.ink3)
                if (onPickImage != null) {
                    TextAction(CharacterImages.PICK, onPickImage, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), color = colors.ink2, style = type.small)
                }
            }
            if (card.holo && painter != null) ImageStatusChip(CharacterImages.HOLO, Modifier.align(Alignment.TopStart).padding(10.dp))
            if (galleryCount > 0) GalleryCountChip(galleryCount, onGallery, Modifier.align(Alignment.BottomEnd).padding(6.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(card.title, type.itemTitle, Modifier.weight(1f), color = colors.ink, maxLines = 1)
            card.firstAppearance?.let { CivilightText(it, type.number.merge(TextStyle(fontSize = 12.sp)), color = colors.ink3, maxLines = 1) }
        }
        if (card.subtitle.isNotEmpty()) CivilightText(card.subtitle, type.small, color = colors.ink3, maxLines = 2)
    }
}

/**
 * `갤러리 <N>` at the bottom-right of a portrait card (GAL-02; board `GalCastCards`): the `홀로` chip's look
 * (24 tall, radius 12, light text on `imageChipFill` with a 12 dp blur) with a 44 dp hit area. Tapping it
 * opens that character's gallery; the rest of the card still opens the record.
 */
@Composable
fun GalleryCountChip(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = CastUiCopy.galleryChip(count)
    Box(
        modifier.pressable(
            onClick = onClick,
            role = Role.Button,
            hitTarget = HitTarget.CHIP,
            shape = RoundedCornerShape(12.dp),
            scaleOnPress = false,
            onClickLabel = label,
        ),
    ) {
        ImageStatusChip(label)
    }
}
