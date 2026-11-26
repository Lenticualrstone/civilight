package io.github.lenticualrstone.civilight.feature.world.place

import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.domain.world.map.PinStatus
import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.domain.world.track.PlaceTimeRow
import io.github.lenticualrstone.civilight.domain.world.track.PlaceTimes
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.CardHeadUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImageUi
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.feature.world.common.CardText
import io.github.lenticualrstone.civilight.feature.world.common.FieldRows
import io.github.lenticualrstone.civilight.feature.world.common.FieldSectionUi
import io.github.lenticualrstone.civilight.feature.world.common.FixedRowUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.MapView
import io.github.lenticualrstone.civilight.navigation.PlaceTab
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.lens.LensRules

/** A row of `이곳의 시간` (board `PlaceTime`): time label, title, sub; dimmed after the lens. */
sealed interface PlaceTimeItemUi {
    val id: String

    data class Row(
        override val id: String,
        val time: String,
        val title: String,
        val sub: String,
        val dimmed: Boolean,
        val target: Route?,
    ) : PlaceTimeItemUi

    /** `시점 1-7 · 이 아래는 아직` (`acc`, dashed). */
    data class Divider(override val id: String, val text: String) : PlaceTimeItemUi
}

/** A map holding a pin of the place (`지도` tab): name, breadcrumb or `◇ 지도 없음`. */
data class PlaceMapRowUi(val id: String, val title: String, val sub: String?, val target: Route?)

/** A `하위 지역` chip: a direct live sub-place (locked hidden ones are only counted). */
data class SubPlaceUi(val key: Key, val name: String)

/**
 * The immersive place card (WLD-09; boards `Immersive` kind place, `ImmContent` place branch): hero image or a
 * colour field, title = name, [subtitle] = structure facts, bottom tabs `개요 · 소속 · 등장`. No tags on places.
 */
data class PlaceCardUi(
    val placeKey: Key,
    val access: CardAccess,
    /** Name and description (the eyebrow `장소 · 하층 구역` is kept for the summary shapes). */
    val head: CardHeadUi?,
    /** `상위 지역 오리지아 · 하위 지역 3`, or null. */
    val subtitle: String?,
    val image: CardImageUi?,
    val tabs: List<String>,
    /** `등장 | 1-2 · 1-7 외 4` (등장 tab). */
    val appearance: FixedRowUi?,
    /** `지역 | 하층 구역` (tap → `지역`). */
    val parent: FixedRowUi?,
    /** `배경 에셋 | 2` (등장 tab, `게임 제작 도구` on). */
    val assets: FixedRowUi?,
    val fields: FieldSectionUi,
    val subPlaces: List<SubPlaceUi>,
    /** Locked hidden sub-places (`숨긴 장소 N`). */
    val hiddenSubPlaces: Int,
    val affiliation: AffiliationUi?,
    val timeRows: List<PlaceTimeItemUi>,
    val maps: List<PlaceMapRowUi>,
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
        const val TAB_AFFILIATION = 1
        const val TAB_APPEARANCE = 2

        /**
         * The tab of the route as an index into [tabs] (`개요 · 소속 · 등장`): `이곳의 시간` and the map rows now
         * live under `등장`.
         */
        fun tabIndex(tab: PlaceTab): Int = when (tab) {
            PlaceTab.OVERVIEW -> TAB_OVERVIEW
            PlaceTab.TIME_HERE, PlaceTab.MAP -> TAB_APPEARANCE
        }
    }
}

/** Structure facts of a place: direct sub-places and the subtitle (no kind field exists; nothing is guessed). */
object PlaceStructure {
    /** Direct live sub-places in creation order; locked hidden ones are left out (see [hiddenSubPlaces]). */
    fun subPlaces(doc: ProjectDoc, placeKey: Key, hiddenOpen: Boolean): List<SubPlaceUi> =
        children(doc, placeKey).filter { hiddenOpen || !it.hidden }.map { SubPlaceUi(it.key, it.name) }

    fun hiddenSubPlaces(doc: ProjectDoc, placeKey: Key, hiddenOpen: Boolean): Int =
        if (hiddenOpen) 0 else children(doc, placeKey).count { it.hidden }

    private fun children(doc: ProjectDoc, placeKey: Key) = doc.world.places
        .filter { it.parentPlaceKey == placeKey && it.key != placeKey && it.deletedAt == null }
        .sortedWith(compareBy({ it.order }, { it.key }))

    /** `상위 지역 X · 하위 지역 N`, either part alone, or null. */
    fun subtitle(parentName: String?, subCount: Int): String? = listOfNotNull(
        parentName?.let(PlaceCopy::parent),
        subCount.takeIf { it > 0 }?.let(PlaceCopy::subPlaces),
    ).joinToString(PlaceCopy.SEP).ifEmpty { null }
}

/** Builds the place card at the lens (WLD-09; `이곳의 시간` rows from WLD-63's domain; `소속` from `Affiliations`). */
object PlaceCardState {
    fun build(
        doc: ProjectDoc,
        projectKey: Key,
        placeKey: Key,
        hiddenOpen: Boolean,
        readOnly: Boolean,
        includeSubPlaces: Boolean = true,
    ): PlaceCardUi {
        val access = CardAccess.of(doc, WorldKind.PLACE, placeKey, hiddenOpen)
        val place = doc.index.place(placeKey)
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val card = if (access == CardAccess.Open) WorldCards(doc, hiddenOpen).placeCard(placeKey, lens) else null
        if (card == null || place == null) {
            return PlaceCardUi(
                placeKey, access, null, null, null, emptyList(), null, null, null, FieldSectionUi(emptyList(), emptyList()),
                emptyList(), 0, null, emptyList(), emptyList(), descriptionLocked = false,
            )
        }
        val parentHidden = place.parentPlaceKey?.let { doc.index.place(it) }?.let { it.hidden && it.deletedAt == null && !hiddenOpen } == true
        val eyebrow = if (parentHidden) "${WorldCopy.TAB_PLACES}${WorldCopy.SEP}${WorldCopy.HIDDEN_ITEM}" else card.eyebrow
        val parentName = if (parentHidden) WorldCopy.HIDDEN_ITEM else card.parentName
        val subPlaces = PlaceStructure.subPlaces(doc, placeKey, hiddenOpen)
        val hiddenSubs = PlaceStructure.hiddenSubPlaces(doc, placeKey, hiddenOpen)
        return PlaceCardUi(
            placeKey = placeKey,
            access = access,
            head = CardHeadUi(eyebrow, card.name, CardText.paragraphs(doc, place.description, hiddenOpen)),
            subtitle = PlaceStructure.subtitle(parentName, subPlaces.size),
            image = CardImages.of(doc, WorldKind.PLACE, placeKey, card.name),
            tabs = PlaceCopy.TABS,
            appearance = FixedRowUi(WorldCopy.ROW_APPEARANCE, card.appearance, editable = false),
            parent = FixedRowUi(WorldCopy.ROW_PARENT, parentName ?: WorldCopy.EMPTY_VALUE, editable = !readOnly),
            // (0.2.0, WLD-76) `배경 에셋` only with `게임 제작 도구` on.
            assets = if (GameTools.on(doc)) FixedRowUi(WorldCopy.ROW_ASSETS, card.backgroundAssets.toString(), editable = false) else null,
            fields = FieldRows.build(doc, EntityKind.PLACE, placeKey, place.fields, lens),
            subPlaces = subPlaces,
            hiddenSubPlaces = hiddenSubs,
            affiliation = AffiliationState.place(doc, placeKey, lens, includeSubPlaces, hiddenOpen, readOnly),
            timeRows = timeRows(doc, projectKey, placeKey, hiddenOpen),
            maps = card.pins.map { mapRow(doc, projectKey, it) },
            descriptionLocked = CardText.hasLockedLinks(doc, place.description, hiddenOpen),
        )
    }

    private fun timeRows(doc: ProjectDoc, projectKey: Key, placeKey: Key, hiddenOpen: Boolean): List<PlaceTimeItemUi> {
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val engine = StoryEngine(doc)
        return PlaceTimes(doc, engine, engine).rows(placeKey, lens, hiddenUnlocked = hiddenOpen).mapIndexed { i, row ->
            when (row) {
                is PlaceTimeRow.EventRow -> PlaceTimeItemUi.Row(
                    "event:${row.eventKey}", row.time, row.title, row.sub, row.dimmed, EventDetail(projectKey, row.eventKey),
                )
                is PlaceTimeRow.SceneRow -> {
                    val scene = doc.index.unit(row.sceneKey)
                    val locked = !hiddenOpen && scene != null && (scene.hidden || doc.index.ancestors(scene).any { it.hidden })
                    if (locked) {
                        PlaceTimeItemUi.Row("scene:${row.sceneKey}", row.time, WorldCopy.HIDDEN_SCENE, "", row.dimmed, null)
                    } else {
                        PlaceTimeItemUi.Row("scene:${row.sceneKey}", row.time, row.title, row.sub, row.dimmed, Script(projectKey, sceneKey = row.sceneKey))
                    }
                }
                is PlaceTimeRow.Divider -> PlaceTimeItemUi.Divider("divider:$i", row.text)
            }
        }
    }

    private fun mapRow(doc: ProjectDoc, projectKey: Key, pin: PinStatus): PlaceMapRowUi = when (pin) {
        is PinStatus.OnMap -> {
            val map = doc.maps[pin.mapKey]
            PlaceMapRowUi(
                id = "pin:${pin.pin.key}",
                title = map?.name.orEmpty(),
                sub = map?.let { MapViews.breadcrumbText(doc, it) }?.takeIf { it != map.name },
                target = MapView(projectKey, pin.mapKey, focusPinKey = pin.pin.key),
            )
        }
        is PinStatus.MapMissing -> PlaceMapRowUi(
            id = "pin:${pin.pin.key}",
            title = doc.maps[pin.mapKey]?.name.orEmpty(),
            sub = pin.text,
            target = null,
        )
    }
}
