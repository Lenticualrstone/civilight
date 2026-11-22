package io.github.lenticualrstone.civilight.feature.world.place

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

/** The full place card (WLD-09): tabs `개요 · 이곳의 시간 · 지도`. */
data class PlaceCardUi(
    val placeKey: Key,
    val access: CardAccess,
    /** `장소 · 하층 구역` or `장소`. */
    val head: CardHeadUi?,
    val tabs: List<String>,
    /** `등장 | 1-2 · 1-7 외 4`. */
    val appearance: FixedRowUi?,
    /** `지역 | 하층 구역` (tap → `지역`). */
    val parent: FixedRowUi?,
    /** `배경 에셋 | 2`. */
    val assets: FixedRowUi?,
    val fields: FieldSectionUi,
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
        /** The tab of the route as an index into [tabs] (`개요 · 이곳의 시간 · 지도`). */
        fun tabIndex(tab: PlaceTab): Int = when (tab) {
            PlaceTab.OVERVIEW -> 0
            PlaceTab.TIME_HERE -> 1
            PlaceTab.MAP -> 2
        }

        fun tabOf(index: Int): PlaceTab = PlaceTab.entries.getOrElse(index) { PlaceTab.OVERVIEW }
    }
}

/** Builds the place card at the lens (WLD-09; `이곳의 시간` rows from WLD-63's domain). */
object PlaceCardState {
    fun build(doc: ProjectDoc, projectKey: Key, placeKey: Key, hiddenOpen: Boolean, readOnly: Boolean): PlaceCardUi {
        val access = CardAccess.of(doc, WorldKind.PLACE, placeKey, hiddenOpen)
        val place = doc.index.place(placeKey)
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val card = if (access == CardAccess.Open) WorldCards(doc, hiddenOpen).placeCard(placeKey, lens) else null
        if (card == null || place == null) {
            return PlaceCardUi(
                placeKey, access, null, emptyList(), null, null, null, FieldSectionUi(emptyList(), emptyList()),
                emptyList(), emptyList(), descriptionLocked = false,
            )
        }
        val parentHidden = place.parentPlaceKey?.let { doc.index.place(it) }?.let { it.hidden && it.deletedAt == null && !hiddenOpen } == true
        val eyebrow = if (parentHidden) "${WorldCopy.TAB_PLACES}${WorldCopy.SEP}${WorldCopy.HIDDEN_ITEM}" else card.eyebrow
        return PlaceCardUi(
            placeKey = placeKey,
            access = access,
            head = CardHeadUi(eyebrow, card.name, CardText.paragraphs(doc, place.description, hiddenOpen)),
            tabs = card.tabs,
            appearance = FixedRowUi(WorldCopy.ROW_APPEARANCE, card.appearance, editable = false),
            parent = FixedRowUi(
                WorldCopy.ROW_PARENT,
                if (parentHidden) WorldCopy.HIDDEN_ITEM else card.parentName ?: WorldCopy.EMPTY_VALUE,
                editable = !readOnly,
            ),
            assets = FixedRowUi(WorldCopy.ROW_ASSETS, card.backgroundAssets.toString(), editable = false),
            fields = FieldRows.build(doc, EntityKind.PLACE, placeKey, place.fields, lens),
            timeRows = timeRows(doc, projectKey, placeKey, hiddenOpen),
            maps = card.pins.map { mapRow(doc, projectKey, it) },
            descriptionLocked = CardText.hasLockedLinks(doc, place.description, hiddenOpen),
        )
    }

    private fun timeRows(doc: ProjectDoc, projectKey: Key, placeKey: Key, hiddenOpen: Boolean): List<PlaceTimeItemUi> {
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val engine = StoryEngine(doc)
        return PlaceTimes(doc, engine, engine).rows(placeKey, lens).mapIndexed { i, row ->
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
