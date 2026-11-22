package io.github.lenticualrstone.civilight.feature.cast.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.list.CardGroup
import io.github.lenticualrstone.civilight.domain.cast.list.CastCard
import io.github.lenticualrstone.civilight.feature.cast.image.CharacterImages
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.image.rememberSlotPainter
import io.github.lenticualrstone.civilight.model.Key

/** Card sizes of `CastCards`: the first group 196 × 245, later groups 132 × 165 (portrait 4:5). */
fun cardWidth(groupIndex: Int): Dp = if (groupIndex == 0) 196.dp else 132.dp

/**
 * One horizontally scrolling row of cards (`CastCards`, CST-04): `<tag> · <count>`, then cards with
 * the portrait's 4:5 crop (or the first letter and `이미지 고르기`), title, first appearance and
 * subtitle. `홀로` marks a portrait with the holo effect.
 */
@Composable
fun CardRow(
    index: Int,
    group: CardGroup,
    imageOf: (Key) -> SlotImage?,
    onOpen: (Key) -> Unit,
    onPickImage: ((Key) -> Unit)?,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(group.header, Modifier.padding(top = if (index == 0) 18.dp else 14.dp, bottom = 10.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 24.dp),
        ) {
            items(group.cards, key = { it.key }) { card ->
                CastCardView(card, cardWidth(index), imageOf(card.key), { onOpen(card.key) }, onPickImage?.let { pick -> { pick(card.key) } })
            }
        }
    }
}

@Composable
private fun CastCardView(card: CastCard, width: Dp, image: SlotImage?, onClick: () -> Unit, onPickImage: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    val painter = rememberSlotPainter(image)
    Column(Modifier.width(width).pressable(onClick = onClick, scaleOnPress = false, onClickLabel = card.title)) {
        Box(Modifier.size(width, width * 5 / 4).clip(shape).background(colors.tint)) {
            if (painter != null) {
                Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                CivilightText(card.initial, type.display, Modifier.align(Alignment.Center), color = colors.ink3)
                if (onPickImage != null) {
                    TextAction(CharacterImages.PICK, onPickImage, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), color = colors.ink2, style = type.small)
                }
            }
            if (card.holo && painter != null) ImageStatusChip(CharacterImages.HOLO, Modifier.align(Alignment.TopStart).padding(10.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(card.title, type.itemTitle, Modifier.weight(1f), color = colors.ink, maxLines = 1)
            card.firstAppearance?.let { CivilightText(it, type.number.merge(TextStyle(fontSize = 12.sp)), color = colors.ink3, maxLines = 1) }
        }
        if (card.subtitle.isNotEmpty()) CivilightText(card.subtitle, type.small, color = colors.ink3, maxLines = 2)
    }
}
