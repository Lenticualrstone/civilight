package io.github.lenticualrstone.civilight.feature.world.list

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.ShardArt
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.common.CardImageUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.WorldTab
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyListState

/** One card of the card view: the list row's name and right-column label, and the item's image. */
data class WorldCardUi(val kind: WorldKind, val key: Key, val name: String, val label: String, val image: CardImageUi)

/** One horizontal row of cards: a list section (`공개됨 · 최신 · 1-7`, `아직 공개 전`, a `지역`), or none. */
data class WorldCardRowUi(val id: String, val title: String?, val cards: List<WorldCardUi>)

/**
 * `목록 | 카드` (WLD-05): the card view is the list's own items as horizontal rows of cards, one row
 * per list section `[PROPOSED]`; lock and empty lines stay as they are. The choice is remembered per
 * screen in `설정 › viewByScreen` (`world.terms`, `world.places`, `world.factions`).
 */
object WorldCardRows {
    fun screenId(tab: WorldTab): String = when (tab) {
        WorldTab.TERMS -> "world.terms"
        WorldTab.PLACES -> "world.places"
        WorldTab.FACTIONS -> "world.factions"
    }

    fun showCards(viewByScreen: Map<String, ViewMode>, tab: WorldTab): Boolean = viewByScreen[screenId(tab)] == ViewMode.CARD

    fun withView(viewByScreen: Map<String, ViewMode>, tab: WorldTab, cards: Boolean): Map<String, ViewMode> =
        viewByScreen + (screenId(tab) to if (cards) ViewMode.CARD else ViewMode.LIST)

    /** Groups the list's rows under their sections; lock and empty lines are left to the list. */
    fun build(doc: ProjectDoc, items: List<WorldListItem>): List<WorldCardRowUi> {
        val out = ArrayList<WorldCardRowUi>()
        var title: String? = null
        var cards = ArrayList<WorldCardUi>()
        fun flush() {
            if (cards.isNotEmpty()) out += WorldCardRowUi("cards:${out.size}:${title.orEmpty()}", title, cards)
            cards = ArrayList()
        }
        for (item in items) {
            when (item) {
                is WorldListItem.Section -> {
                    flush()
                    title = item.title
                }
                is WorldListItem.Row -> {
                    val label = listOfNotNull(item.right.takeIf { it.isNotEmpty() }, item.candidates).joinToString(" · ")
                    cards += WorldCardUi(item.kind, item.key, item.title, label, CardImages.of(doc, item.kind, item.key, item.title))
                }
                else -> Unit
            }
        }
        flush()
        return out
    }
}

/** A row of cards that snaps to the nearest card's left edge, with a light image parallax (WLD-05, board `MCarousel`). */
@Composable
fun WorldCardRow(row: WorldCardRowUi, selectedKey: Key?, onCard: (WorldCardUi) -> Unit) {
    val state = rememberLazyListState()
    val reduced = CivilightTheme.reducedMotion
    val haptics = rememberHaptics()
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().drop(1).collect { if (!reduced) haptics.perform(HapticToken.TICK) }
    }
    Column(Modifier.fillMaxWidth()) {
        row.title?.let { SectionLabel(it, Modifier.padding(top = 18.dp, bottom = 8.dp)) }
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 48.dp),
        ) {
            itemsIndexed(row.cards, key = { _, c -> c.key }) { index, card ->
                WorldCardTile(card, selected = card.key == selectedKey, parallax = if (reduced) 0f else parallaxOf(state, index), onClick = { onCard(card) })
            }
        }
    }
}

/** −1 … 1 as the card moves across the row (0 = at the left edge); drives the 8 % image shift. */
private fun parallaxOf(state: LazyListState, index: Int): Float {
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
    val width = state.layoutInfo.viewportSize.width.takeIf { it > 0 } ?: return 0f
    return (info.offset.toFloat() / width).coerceIn(-1f, 1f)
}

@Composable
private fun WorldCardTile(card: WorldCardUi, selected: Boolean, parallax: Float, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    val deps = LocalFeatureDeps.current
    Column(
        Modifier
            .width(220.dp)
            .pressable(PressAction.Select(selected, onClick), role = Role.Button, shape = shape),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(if (selected) colors.sel else colors.raise)) {
            val shift = Modifier.graphicsLayer { translationX = -parallax * size.width * 0.08f; scaleX = 1.1f; scaleY = 1.1f }
            when (val image = card.image) {
                is CardImageUi.Initial -> CivilightText(image.letter, type.hero, Modifier.align(Alignment.Center), color = colors.ink3)
                is CardImageUi.Preset -> ShardArt(listOf(colors.tint, colors.raise, colors.edge), shift)
                is CardImageUi.Picked -> {
                    val bitmap by produceState<ImageBitmap?>(null, image) {
                        value = withContext(Dispatchers.IO) {
                            deps.stores.blobs.fileOf(image.hash)?.let { runCatching { CardImages.decode(it, image.crop, 480) }.getOrNull() }?.asImageBitmap()
                        }
                    }
                    bitmap?.let { Image(it, contentDescription = card.name, modifier = shift.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
            }
        }
        CivilightText(card.name, type.itemTitle, Modifier.padding(top = 8.dp), color = colors.ink, maxLines = 1)
        if (card.label.isNotEmpty()) CivilightText(card.label, type.small, color = colors.ink3, maxLines = 1)
    }
}
