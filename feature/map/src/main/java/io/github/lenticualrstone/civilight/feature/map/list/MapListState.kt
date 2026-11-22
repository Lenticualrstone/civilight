package io.github.lenticualrstone.civilight.feature.map.list

import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** A map card (200 × 150 thumbnail, name, small line `생성 · 핀 9 · 영역 4`). */
data class MapCardUi(val key: Key, val name: String, val line: String, val base: MapBase, val scene: MapScene)

/** 지도 목록 (WLD-36, board `MapNew`). */
data class MapListUi(
    /** `잿빛 새벽의 등불 · 세계`. */
    val small: String,
    /** `지도 · 2`. */
    val section: String,
    val cards: List<MapCardUi>,
)

/** The `상위` line of the new-map sheet started from a pin: `세계 지도 · 핀 ‘상층’`. */
data class ParentUi(val parent: MapParent, val tag: String)

object MapListState {
    fun build(doc: ProjectDoc, lens: LensPoint, hiddenOpen: Boolean): MapListUi = MapListUi(
        small = MapCopy.listSmall(doc.project.name),
        section = MapViews.sectionTitle(doc),
        cards = MapViews.liveMaps(doc).map { map ->
            MapCardUi(
                key = map.key,
                name = map.name,
                line = MapViews.cardLine(doc, map),
                base = MapBases.of(map),
                scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.VIEW),
            )
        },
    )

    /** The parent line for a child map of [pinKey] on [mapKey], or null when either is gone. */
    fun parent(doc: ProjectDoc, mapKey: Key?, pinKey: Key?): ParentUi? {
        if (mapKey == null || pinKey == null) return null
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        val pin = map.pins.firstOrNull { it.key == pinKey } ?: return null
        val place = doc.index.place(pin.placeKey)?.name.orEmpty()
        return ParentUi(MapParent(mapKey, pinKey), MapCopy.parentTag(map.name, place))
    }
}
