package io.github.lenticualrstone.civilight.feature.world.faction

import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.CardHeadUi
import io.github.lenticualrstone.civilight.feature.world.common.CardText
import io.github.lenticualrstone.civilight.feature.world.common.FieldRows
import io.github.lenticualrstone.civilight.feature.world.common.FieldSectionUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.MapView
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.ui.lens.LensRules

/** A row of a derived section of the faction card; [target] null rows do not open anything. */
data class FactionLinkRowUi(val id: String, val title: String, val target: Route?)

/** A titled derived section (`이 항목으로 잇는 인물`, map areas, events). */
data class FactionSectionUi(val id: String, val title: String, val rows: List<FactionLinkRowUi>)

/** The faction card (WLD-10 `[PROPOSED]`: the term card's layout without reveal and usage). */
data class FactionCardUi(
    val factionKey: Key,
    val access: CardAccess,
    val head: CardHeadUi?,
    val fields: FieldSectionUi,
    val sections: List<FactionSectionUi>,
    val descriptionLocked: Boolean,
) {
    val title: String
        get() = when (access) {
            CardAccess.Open -> head?.name.orEmpty()
            is CardAccess.Locked -> WorldCopy.HIDDEN_ITEM
            is CardAccess.Gone -> access.text
        }
}

/** Builds the faction card at the lens (fields and `연결` follow the lens, WLD-12). */
object FactionCardState {
    fun build(doc: ProjectDoc, projectKey: Key, factionKey: Key, hiddenOpen: Boolean): FactionCardUi {
        val access = CardAccess.of(doc, WorldKind.FACTION, factionKey, hiddenOpen)
        val faction = doc.index.faction(factionKey)
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val card = if (access == CardAccess.Open) WorldCards(doc, hiddenOpen).factionCard(factionKey, lens) else null
        if (card == null || faction == null) {
            return FactionCardUi(factionKey, access, null, FieldSectionUi(emptyList(), emptyList()), emptyList(), descriptionLocked = false)
        }
        val linked = card.linkedCharacters.map {
            FactionLinkRowUi("char:${it.characterKey}:${it.label}", it.label, CharacterProfile(projectKey, it.characterKey))
        }
        val areas = MapViews.liveMaps(doc).flatMap { map ->
            map.regions.filter { it.factionKey == factionKey }.map { region ->
                FactionLinkRowUi("region:${region.key}", "${region.name}${WorldCopy.SEP}${map.name}", MapView(projectKey, map.key))
            } + map.poly?.provinces.orEmpty().filter { it.factionKey == factionKey }.map { province ->
                FactionLinkRowUi("province:${map.key}:${province.key}", "${province.name}${WorldCopy.SEP}${map.name}", MapView(projectKey, map.key))
            }
        }
        val events = card.changeEvents.mapNotNull { key ->
            doc.index.event(key)?.takeIf { it.deletedAt == null }?.let { FactionLinkRowUi("event:$key", it.title, EventDetail(projectKey, key)) }
        }
        val sections = listOf(
            FactionSectionUi("linked", WorldCopy.LINKED_CHARACTERS, linked),
            FactionSectionUi("areas", WorldCopy.FACTION_AREAS, areas),
            FactionSectionUi("events", WorldCopy.FACTION_EVENTS, events),
        ).filter { it.rows.isNotEmpty() }
        return FactionCardUi(
            factionKey = factionKey,
            access = access,
            head = CardHeadUi(WorldCopy.EYEBROW_FACTION, card.name, CardText.paragraphs(doc, faction.description, hiddenOpen)),
            fields = FieldRows.build(doc, EntityKind.FACTION, factionKey, faction.fields, lens),
            sections = sections,
            descriptionLocked = CardText.hasLockedLinks(doc, faction.description, hiddenOpen),
        )
    }
}
