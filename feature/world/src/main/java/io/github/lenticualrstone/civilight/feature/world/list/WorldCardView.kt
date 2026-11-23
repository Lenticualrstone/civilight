package io.github.lenticualrstone.civilight.feature.world.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.SnapCardRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.common.CardImageContent
import io.github.lenticualrstone.civilight.feature.world.common.CardImageUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.WorldTab

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

    /**
     * The lines card view keeps as list lines: the lock and empty lines, and (0.2.0, WLD-73) the `용어 후보 · N`
     * section with its rows, which stays a list.
     */
    fun cardViewLines(items: List<WorldListItem>): List<WorldListItem> {
        val firstCandidate = items.indexOfFirst { it is WorldListItem.CandidateRow }
        val tail = if (firstCandidate <= 0) emptyList() else items.subList(firstCandidate - 1, items.size)
            .filter { it is WorldListItem.CandidateRow || it is WorldListItem.Section }
        return items.filter { it is WorldListItem.Lock || it is WorldListItem.Empty } + tail
    }

    /** Groups the list's rows under their sections; lock and empty lines are left to the list. */
    fun build(doc: ProjectDoc, items: List<WorldListItem>): List<WorldCardRowUi> {
        val out = ArrayList<WorldCardRowUi>()
        var title: String? = null
        var cards = ArrayList<WorldCardUi>()
        val rows = ArrayList<Pair<String?, List<WorldCardUi>>>()
        fun flush() {
            if (cards.isNotEmpty()) rows += title to cards
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
        // A row is keyed by its section, not by its place in the list, so a row keeps its key (and its
        // scroll position) when the rows above it come and go.
        val ids = WorldViewSwitch.uniqueKeys(rows.map { "cards:${it.first.orEmpty()}" })
        rows.forEachIndexed { i, (rowTitle, rowCards) -> out += WorldCardRowUi(ids[i], rowTitle, rowCards) }
        return out
    }
}

/**
 * A row of cards that snaps to the nearest card's left edge, with a light image parallax (WLD-05, board
 * `MCarousel`): the shared [SnapCardRow] (snap on `enter`, tick, 8 % parallax and the constant 1.1 scale in
 * the draw phase, cards entering and leaving with `civilightItem`).
 */
@Composable
fun WorldCardRow(row: WorldCardRowUi, selectedKey: Key?, inner: Boolean, onCard: (WorldCardUi) -> Unit, modifier: Modifier = Modifier) {
    SnapCardRow(
        items = row.cards,
        key = { it.key },
        kind = CardKind.RECENT,
        inner = inner,
        modifier = modifier.padding(top = if (row.title != null) 18.dp else 8.dp),
        title = row.title,
    ) { card, imageModifier ->
        WorldCardTile(card, selected = card.key == selectedKey, imageModifier = imageModifier, onClick = { onCard(card) })
    }
}

@Composable
private fun WorldCardTile(card: WorldCardUi, selected: Boolean, imageModifier: Modifier, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    val fill by animatedColor(if (selected) colors.sel else colors.raise)
    Column(
        Modifier
            .width(220.dp)
            .pressable(PressAction.Select(selected, onClick), role = Role.Button, shape = shape),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).drawBehind { drawRect(fill) }) {
            CardImageContent(card.image, card.name, targetWidth = 480, modifier = imageModifier)
        }
        CivilightText(card.name, type.itemTitle, Modifier.padding(top = 8.dp), color = colors.ink, maxLines = 1)
        if (card.label.isNotEmpty()) CivilightText(card.label, type.small, color = colors.ink3, maxLines = 1)
    }
}
