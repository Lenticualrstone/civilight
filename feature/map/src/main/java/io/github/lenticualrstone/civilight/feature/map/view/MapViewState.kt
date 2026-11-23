package io.github.lenticualrstone.civilight.feature.map.view

import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.feature.map.common.MapOffers
import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.domain.world.map.Crumb
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** The place sheet (cover) / side card (inner) of a selected pin (WLD-09 summary, WLD-37). */
data class PlacePanelUi(
    val pinKey: Key,
    val placeKey: Key,
    val name: String,
    /** `장소 · 하층 구역` or `장소`. */
    val eyebrow: String,
    val description: String,
    /** `1-2 · 1-7 외 4`. */
    val appearance: String,
    /** The parent place (`지역 | 하층 구역`, Q-03 default), or null. */
    val parentName: String?,
    /** `배경 에셋 | 2`. */
    val backgroundAssets: Int,
    /** Live child maps of the pin (`하위 지도`, WLD-48 entry by a visible control, Q-02). */
    val childMaps: List<Pair<Key, String>>,
)

/** A layer chip under the map: `전체 핀` (null key) or a layer's name. */
data class LayerChipUi(val layerKey: Key?, val label: String, val selected: Boolean)

/** 지도 보기 (WLD-37, boards `Map`, `MapC`, `MapSoft`, `MapSoftC`). */
data class MapViewUi(
    val mapKey: Key,
    val title: String,
    /** `세계 · 지도 · 색면`. */
    val small: String,
    /** `세계 지도 › 하층 구역` when nested, else empty. */
    val crumbs: List<Crumb>,
    val base: MapBase,
    val scene: MapScene,
    /** `1일 · 15 km`, or null without a scale. */
    val scale: String?,
    val chips: List<LayerChipUi>,
    /** `지도 이미지 바꾸기` is offered for image and sketch bases. */
    val canReplaceBase: Boolean,
    val place: PlacePanelUi?,
    /** The pin being moved and its new spot (`핀 옮기기 · <name>`), drawn there before `완료`. */
    val moving: MovingPinUi?,
    /** `그림 · 색면` chip of a generated detailed-terrain or polygon map (WLD-57), the style's name. */
    val styleChip: String? = null,
    /** (0.2.0, WLD-75) `편집`: image maps only; every other map is view-only. */
    val canEdit: Boolean = true,
    /** (0.2.0, WLD-75) `핀 옮기기`: image maps only. */
    val canMovePin: Boolean = true,
    /** (0.2.0, WLD-76) The `배경 에셋` row: only with `게임 제작 도구` on. */
    val showsAssets: Boolean = true,
)

data class MovingPinUi(val pinKey: Key, val title: String, val to: Point?)

/** Pure state of the map view; the screen only draws it. */
object MapViewState {
    fun styleName(map: WorldMap): String? = map.style?.known?.let { MapCopy.STYLE_NAMES.getOrNull(it.ordinal) }

    fun build(
        doc: ProjectDoc,
        mapKey: Key,
        lens: LensPoint,
        hiddenOpen: Boolean,
        layerFilter: Key?,
        selectedPin: Key?,
        moving: Pair<Key, Point?>?,
    ): MapViewUi? {
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        val filter = layerFilter?.takeIf { k -> map.layers.any { it.key == k } }
        var scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.VIEW, filter)
        val movingPin = moving?.let { (key, to) -> scene.pin(key)?.let { MovingPinUi(key, MapCopy.movingPin(it.name), to) } }
        if (movingPin?.to != null) {
            scene = scene.copy(pins = scene.pins.map { if (it.key == movingPin.pinKey) it.copy(at = movingPin.to) else it })
        }
        val crumbs = MapViews.breadcrumb(doc, map)
        return MapViewUi(
            mapKey = map.key,
            title = map.name,
            small = MapCopy.viewSmall(styleName(map)),
            crumbs = if (crumbs.size > 1) crumbs else emptyList(),
            base = MapBases.of(map),
            scene = scene,
            scale = if (MapOffers.showsScale(map)) MapScales.barLabel(doc, map) else null,
            chips = listOf(LayerChipUi(null, MapCopy.ALL_PINS, filter == null)) +
                MapEdits.layers(map).map { LayerChipUi(it.key, it.name, it.key == filter) },
            canReplaceBase = MapOffers.canReplaceBase(map),
            place = selectedPin?.let { placePanel(doc, map, scene, it, lens, hiddenOpen) },
            moving = movingPin,
            styleChip = if (MapOffers.showsStyleChip(map)) styleChip(map) else null,
            canEdit = MapOffers.canEdit(map),
            canMovePin = MapOffers.canMovePin(map),
            showsAssets = GameTools.on(doc),
        )
    }

    /** The style chip is offered where styles apply: generated detailed-terrain and polygon maps (WLD-57). */
    fun styleChip(map: WorldMap): String? {
        if (map.baseKind.known != MapBaseKind.GENERATED) return null
        val format = map.format?.known
        if (format != MapFormat.DETAIL && format != MapFormat.POLY) return null
        return MapCopy.STYLE_NAMES[(map.style?.known ?: MapStyle.SOFT).ordinal]
    }

    /** The panel of a drawn pin, or null when the pin is not drawn (hidden, not yet revealed, filtered). */
    fun placePanel(doc: ProjectDoc, map: WorldMap, scene: MapScene, pinKey: Key, lens: LensPoint, hiddenOpen: Boolean): PlacePanelUi? {
        val pin = scene.pin(pinKey) ?: return null
        val card = WorldCards(doc, hiddenOpen).placeCard(pin.placeKey, lens) ?: return null
        return PlacePanelUi(
            pinKey = pinKey,
            placeKey = pin.placeKey,
            name = card.name,
            eyebrow = card.eyebrow,
            description = card.description,
            appearance = card.appearance,
            parentName = card.parentName,
            backgroundAssets = card.backgroundAssets,
            childMaps = MapViews.childMaps(doc, pinKey).map { it.key to it.name },
        )
    }
}
