package io.github.lenticualrstone.civilight.feature.world.faction

import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.CardHeadUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImageUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.feature.world.common.CardText
import io.github.lenticualrstone.civilight.feature.world.common.FieldRows
import io.github.lenticualrstone.civilight.feature.world.common.FieldSectionUi
import io.github.lenticualrstone.civilight.feature.world.common.FixedRowUi
import io.github.lenticualrstone.civilight.feature.world.place.AffiliationState
import io.github.lenticualrstone.civilight.feature.world.place.FactionMembersUi
import io.github.lenticualrstone.civilight.feature.world.place.PlaceCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.MapView
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.ui.lens.LensRules

/** A row of a derived section of the faction card; [target] null rows do not open anything. */
data class FactionLinkRowUi(val id: String, val title: String, val target: Route?)

/** A titled derived section (map areas, events). */
data class FactionSectionUi(val id: String, val title: String, val rows: List<FactionLinkRowUi>)

/**
 * The faction card (WLD-10) on the immersive scaffold: hero image, title, subtitle `거점 <place>`; tabs
 * `개요 · 소속`: `개요` = 설명, `거점` (fixed row, place picker), 세부 항목, map areas and events; `소속` = `소속 인물`
 * (board `ImmPlaceRules` row `세력 카드`).
 */
data class FactionCardUi(
    val factionKey: Key,
    val access: CardAccess,
    val head: CardHeadUi?,
    val subtitle: String?,
    val image: CardImageUi?,
    val tabs: List<String>,
    /** `거점 | 하층 거리` (tap → place picker); dotted when the place is deleted (rule 20). */
    val base: FixedRowUi?,
    val baseKey: Key?,
    val baseDeleted: Boolean,
    val fields: FieldSectionUi,
    val members: FactionMembersUi?,
    val sections: List<FactionSectionUi>,
    val descriptionLocked: Boolean,
) {
    val title: String
        get() = when (access) {
            CardAccess.Open -> head?.name.orEmpty()
            is CardAccess.Locked -> WorldCopy.HIDDEN_ITEM
            is CardAccess.Gone -> access.text
        }

    companion object {
        const val TAB_OVERVIEW = 0
        const val TAB_MEMBERS = 1
        val TABS: List<String> = listOf(PlaceCopy.TAB_OVERVIEW, PlaceCopy.TAB_AFFILIATION)
    }
}

/** Builds the faction card at the lens (fields and `소속` follow the lens, WLD-12). */
object FactionCardState {
    fun build(doc: ProjectDoc, projectKey: Key, factionKey: Key, hiddenOpen: Boolean, readOnly: Boolean = false): FactionCardUi {
        val access = CardAccess.of(doc, WorldKind.FACTION, factionKey, hiddenOpen)
        val faction = doc.index.faction(factionKey)
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val card = if (access == CardAccess.Open) WorldCards(doc, hiddenOpen).factionCard(factionKey, lens) else null
        if (card == null || faction == null) {
            return FactionCardUi(
                factionKey, access, null, null, null, emptyList(), null, null, false,
                FieldSectionUi(emptyList(), emptyList()), null, emptyList(), descriptionLocked = false,
            )
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
            FactionSectionUi("areas", WorldCopy.FACTION_AREAS, areas),
            FactionSectionUi("events", WorldCopy.FACTION_EVENTS, events),
        ).filter { it.rows.isNotEmpty() }
        val members = AffiliationState.faction(doc, factionKey, lens, hiddenOpen, readOnly)
        return FactionCardUi(
            factionKey = factionKey,
            access = access,
            head = CardHeadUi(WorldCopy.EYEBROW_FACTION, card.name, CardText.paragraphs(doc, faction.description, hiddenOpen)),
            subtitle = members?.base?.let(PlaceCopy::base),
            image = CardImages.of(doc, WorldKind.FACTION, factionKey, card.name),
            tabs = FactionCardUi.TABS,
            base = FixedRowUi(PlaceCopy.BASE, members?.base ?: WorldCopy.EMPTY_VALUE, editable = !readOnly),
            baseKey = members?.baseKey,
            baseDeleted = members?.baseDeleted == true,
            fields = FieldRows.build(doc, EntityKind.FACTION, factionKey, faction.fields, lens),
            members = members,
            sections = sections,
            descriptionLocked = CardText.hasLockedLinks(doc, faction.description, hiddenOpen),
        )
    }
}
