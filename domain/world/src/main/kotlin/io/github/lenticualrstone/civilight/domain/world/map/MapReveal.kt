package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapLayer
import io.github.lenticualrstone.civilight.model.map.MapOverlay
import io.github.lenticualrstone.civilight.model.map.MapRegion
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.PointRef

enum class OverlayKind { PIN, REGION, ROUTE, LABEL, STAMP, NODE, NODE_LINK }

/** One overlay as the map view draws it at a lens. */
data class MapItem(
    val key: Key,
    val kind: OverlayKind,
    val name: String,
    val revealPoint: PointRef?,
    /** Revealed at the lens (its own point and its layer's point). */
    val visible: Boolean,
    /** `상층 관문 · 1-7` for items not yet revealed. */
    val pendingTag: String?,
)

/** `시점별 공개` (WLD-49): what a map shows at a lens. */
data class MapRevealView(
    val items: List<MapItem>,
    /** `보임 · 핀 4 · 영역 1 | 아직 공개 전 · 2`. */
    val summary: String,
    val pendingTags: List<String>,
)

/**
 * Visibility of map overlays (WLD-49, WLD-13): an item is visible at lens L iff its `보이는 시점`
 * and its layer's are ≤ L. Items of hidden places and factions are left out entirely while the
 * hidden lock is closed; layers switched off are left out of the drawing.
 */
object MapReveal {
    private fun layerOf(map: WorldMap, overlay: MapOverlay): MapLayer? {
        val key = when (overlay) {
            is Pin -> overlay.layerKey
            is MapRegion -> overlay.layerKey
            is io.github.lenticualrstone.civilight.model.map.Route -> overlay.layerKey
            is io.github.lenticualrstone.civilight.model.map.MapLabel -> overlay.layerKey
            is io.github.lenticualrstone.civilight.model.map.Stamp -> overlay.layerKey
            else -> null
        }
        return key?.let { k -> map.layers.firstOrNull { it.key == k } }
    }

    /** Revealed at [lens] (point and layer point). */
    fun isRevealed(doc: ProjectDoc, map: WorldMap, overlay: MapOverlay, lens: LensPoint): Boolean {
        val layer = layerOf(map, overlay)
        return doc.manuscript.isVisible(overlay.revealPoint, lens) && doc.manuscript.isVisible(layer?.revealPoint, lens)
    }

    /** False when the overlay's layer is switched off. */
    fun isLayerShown(map: WorldMap, overlay: MapOverlay): Boolean = layerOf(map, overlay)?.visible ?: true

    /** True when the overlay belongs to a hidden place or faction and the lock is closed. */
    fun isMasked(doc: ProjectDoc, overlay: MapOverlay, hiddenUnlocked: Boolean): Boolean {
        if (hiddenUnlocked) return false
        val place = when (overlay) {
            is Pin -> overlay.placeKey
            is MapRegion -> overlay.placeKey
            is io.github.lenticualrstone.civilight.model.map.Stamp -> overlay.placeKey
            is io.github.lenticualrstone.civilight.model.map.NodeMapNode -> overlay.placeKey
            else -> null
        }
        val faction = (overlay as? MapRegion)?.factionKey
        return place?.let { doc.index.place(it)?.hidden } == true || faction?.let { doc.index.faction(it)?.hidden } == true
    }

    private fun overlays(map: WorldMap): List<Pair<OverlayKind, MapOverlay>> =
        map.pins.map { OverlayKind.PIN to it } + map.regions.map { OverlayKind.REGION to it } +
            map.routes.map { OverlayKind.ROUTE to it } + map.labels.map { OverlayKind.LABEL to it } +
            map.stamps.map { OverlayKind.STAMP to it } + map.nodes.map { OverlayKind.NODE to it } +
            map.nodeLinks.map { OverlayKind.NODE_LINK to it }

    private fun nameOf(doc: ProjectDoc, overlay: MapOverlay): String = when (overlay) {
        is Pin -> doc.index.place(overlay.placeKey)?.name.orEmpty()
        is MapRegion -> overlay.name
        is io.github.lenticualrstone.civilight.model.map.MapLabel -> overlay.text
        is io.github.lenticualrstone.civilight.model.map.NodeMapNode -> overlay.name
        is io.github.lenticualrstone.civilight.model.map.Stamp -> overlay.placeKey?.let { doc.index.place(it)?.name }.orEmpty()
        else -> ""
    }

    /** The map at [lens]: live overlays only (a pin also needs its place), hidden items masked while locked. */
    fun at(doc: ProjectDoc, map: WorldMap, lens: LensPoint, hiddenUnlocked: Boolean = false): MapRevealView {
        val codes = StoryCodes(doc)
        val items = overlays(map)
            .filter { (_, o) -> doc.liveness.isOverlayLive(map.key, o) && !isMasked(doc, o, hiddenUnlocked) && isLayerShown(map, o) }
            .map { (kind, o) ->
                val visible = isRevealed(doc, map, o, lens)
                val name = nameOf(doc, o)
                val point = o.revealPoint ?: layerOf(map, o)?.revealPoint
                MapItem(
                    key = o.key, kind = kind, name = name, revealPoint = o.revealPoint, visible = visible,
                    pendingTag = if (visible) null else "$name${Copy.SEP}${point?.let { codes.pointLabel(it) }.orEmpty()}",
                )
            }
        val pins = items.filter { it.kind == OverlayKind.PIN }
        val regions = items.filter { it.kind == OverlayKind.REGION }
        val pending = (pins + regions).filterNot { it.visible }
        val summary = "보임${Copy.SEP}핀 ${pins.count { it.visible }}${Copy.SEP}영역 ${regions.count { it.visible }} | " +
            "아직 공개 전${Copy.SEP}${pending.size}"
        return MapRevealView(items, summary, pending.mapNotNull { it.pendingTag })
    }
}
