package io.github.lenticualrstone.civilight.feature.map.pick

import io.github.lenticualrstone.civilight.domain.world.item.PlacePicker
import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapHit
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/** A map chip of the pick screen's map switcher. */
data class PickMapChip(val key: Key, val name: String, val selected: Boolean)

/** 지도에서 고르기 (WLD-52 map mode). */
data class PickUi(
    val maps: List<PickMapChip>,
    val mapKey: Key,
    val base: MapBase,
    val scene: MapScene,
    /** `지역` mode: taps pick the region holding the point. */
    val regions: Boolean,
    val picked: PlaceRef?,
    /** The pin or region drawn as chosen. */
    val selectedKey: Key?,
    /** `씬 1개 → 잿더미 시장` for a scene (`씬 4개 → …` for a bulk pick), or the name alone; null before a pick. */
    val bar: String?,
)

/**
 * Pure state of `지도에서 고르기`. Maps that can hold a pick are the live maps with a base (node
 * maps carry no pins). The first map shown holds the scene's current place or region, else the
 * first top-level map. A pick is a `PlaceRef(PLACE)` from a pin (44 dp target) or a
 * `PlaceRef(REGION)` from inside a drawn region at the lens.
 */
object PickState {
    fun pickableMaps(doc: ProjectDoc) = MapViews.liveMaps(doc).filterNot { MapScales.isNodeMap(it) }

    fun scenePlace(doc: ProjectDoc, sceneKey: Key?): PlaceRef? = sceneKey?.let { k -> doc.manuscript.liveScenes.firstOrNull { it.key == k }?.place }

    fun initialMap(doc: ProjectDoc, sceneKey: Key?): Key? {
        val maps = pickableMaps(doc)
        val current = scenePlace(doc, sceneKey)
        val holding = current?.let { ref ->
            maps.firstOrNull { m ->
                when (ref.kind.known) {
                    PlaceRefKind.PLACE -> m.pins.any { it.placeKey == ref.key }
                    PlaceRefKind.REGION -> m.regions.any { it.key == ref.key }
                    null -> false
                }
            }
        }
        return (holding ?: maps.firstOrNull { it.parent == null } ?: maps.firstOrNull())?.key
    }

    fun initialRegions(doc: ProjectDoc, sceneKey: Key?): Boolean = scenePlace(doc, sceneKey)?.kind?.known == PlaceRefKind.REGION

    /**
     * Only what the pick scene draws can be picked: hidden places and factions stay masked while
     * locked, and items not yet revealed at the lens or on a switched-off layer are not drawn.
     */
    fun pick(doc: ProjectDoc, mapKey: Key, at: Point, hit: MapHit?, regions: Boolean, lens: LensPoint, hiddenOpen: Boolean): PlaceRef? {
        val map = pickableMaps(doc).firstOrNull { it.key == mapKey } ?: return null
        val scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.PICK)
        if (regions) {
            return scene.regions.lastOrNull { MapGeometry.contains(it.shape, at) }?.let { PlaceRef(PlaceRefKind.REGION, it.key) }
        }
        val pin = (hit as? MapHit.Pin)?.let { scene.pin(it.key) } ?: return null
        return PlaceRef(PlaceRefKind.PLACE, pin.placeKey)
    }

    fun build(
        doc: ProjectDoc,
        mapKey: Key,
        lens: LensPoint,
        hiddenOpen: Boolean,
        regions: Boolean,
        picked: PlaceRef?,
        sceneKey: Key?,
        /** Scenes placed at once (`PickOnMap.sceneCount`, bulk `장소 정하기`). */
        sceneCount: Int = 1,
    ): PickUi? {
        val maps = pickableMaps(doc)
        val map = maps.firstOrNull { it.key == mapKey } ?: return null
        val scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.PICK)
        val selected = when (picked?.kind?.known) {
            PlaceRefKind.PLACE -> scene.pins.firstOrNull { it.placeKey == picked.key }?.key
            PlaceRefKind.REGION -> picked.key.takeIf { scene.region(it) != null }
            else -> null
        }
        val bar = picked?.let { ref ->
            if (sceneKey != null || sceneCount > 1) PlacePicker(doc, hiddenOpen).confirmBar(sceneCount.coerceAtLeast(1), ref) else nameOf(doc, ref)
        }
        return PickUi(
            maps = maps.map { PickMapChip(it.key, it.name, it.key == mapKey) },
            mapKey = map.key,
            base = MapBases.of(map),
            scene = scene,
            regions = regions,
            picked = picked,
            selectedKey = selected,
            bar = bar,
        )
    }

    fun nameOf(doc: ProjectDoc, ref: PlaceRef): String = when (ref.kind.known) {
        PlaceRefKind.PLACE -> doc.index.place(ref.key)?.name
        PlaceRefKind.REGION -> doc.index.region(ref.key)?.second?.name
        null -> null
    }.orEmpty()
}
