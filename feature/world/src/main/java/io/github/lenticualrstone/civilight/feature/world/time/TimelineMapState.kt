package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.map.MapReveal
import io.github.lenticualrstone.civilight.domain.world.map.OverlayKind
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.domain.world.time.EventMasks
import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.domain.world.track.MapHighlight
import io.github.lenticualrstone.civilight.domain.world.track.RegionChange
import io.github.lenticualrstone.civilight.domain.world.track.TimelineMaps
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.lens.LensRules

/** A row of the left list: `1200 · 1-3 | 리엔, 결사 합류 | 씬 2 · 1일째 아침 | 결사의 문`. */
data class TimelineMapRowUi(
    val eventKey: Key,
    val time: String,
    val title: String,
    val sub: String,
    val place: String?,
    /** `세력 영역 변화 ◆`. */
    val regionChange: String?,
    val selected: Boolean,
    /** A story event on a hidden scene while locked: no detail, no highlight. */
    val locked: Boolean,
)

/** A pin on the schematic map (0–1 coordinates); [highlighted] = the selected event's place. */
data class MapPinUi(val key: Key, val name: String, val x: Float, val y: Float, val highlighted: Boolean)

/** A region outline; [added] = the part drawn dotted in the comparison (`점선 = 1-9부터 더해진 곳`). */
data class MapRegionUi(val key: Key, val name: String, val vertices: List<Point>, val highlighted: Boolean, val added: List<Point>? = null)

/** `1-7 · 1-9 ◆ · 비교` and the caption `상층 경비대 | 영역 · 1-9 ◆ 넓어짐`. */
data class RegionCompareUi(
    val chips: List<String>,
    val mode: CompareMode,
    val captionName: String,
    val captionLine: String,
    /** `점선 = 1-9부터 더해진 곳`, only while comparing. */
    val dottedNote: String?,
)

/** Which shape the map draws for a region with `◆` history (WLD-64, WLD-65). */
enum class CompareMode { NOW, CHANGE, BOTH }

/** The `연표 + 지도` screen (board `TimelineMap`). */
data class TimelineMapUi(
    val hint: String,
    val rows: List<TimelineMapRowUi>,
    /** Live maps to pick from (name), in map order. */
    val maps: List<Pair<Key, String>>,
    val mapKey: Key?,
    val mapName: String,
    /** Width / height of the map (base size, else 4:3). */
    val aspect: Float,
    val pins: List<MapPinUi>,
    val regions: List<MapRegionUi>,
    val compare: RegionCompareUi?,
)

/**
 * Builds `연표 + 지도` (WLD-64): every live event in time order on the left; on the right the map
 * holding the selected event's place, with that pin or region highlighted, overlays as revealed at
 * the lens (WLD-49) and hidden items left out while locked (WLD-13). A region with a `◆` shape on
 * this map gets the chips `<lens> · <point> ◆ · 비교` (WLD-65).
 */
object TimelineMapState {
    fun build(doc: ProjectDoc, selectedEvent: Key?, chosenMap: Key?, compareMode: CompareMode, hiddenOpen: Boolean): TimelineMapUi {
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val maps = TimelineMaps(doc, StoryEngine(doc))
        val rows = maps.rows().map { row ->
            val event = doc.index.event(row.eventKey)
            val sceneLocked = !hiddenOpen && event != null && EventMasks.sceneHidden(doc, event)
            val placeLocked = !hiddenOpen && event != null && EventMasks.placeHidden(doc, event.place)
            TimelineMapRowUi(
                eventKey = row.eventKey,
                time = row.time,
                title = if (sceneLocked) WorldCopy.HIDDEN_SCENE else row.title,
                sub = if (sceneLocked) "" else row.sub,
                place = if (placeLocked) WorldCopy.HIDDEN_ITEM else row.placeName,
                regionChange = if (row.regionChange && !sceneLocked) TimelineMaps.REGION_CHANGE else null,
                selected = row.eventKey == selectedEvent,
                locked = sceneLocked,
            )
        }
        val selectedRow = rows.firstOrNull { it.selected && !it.locked }
        val highlights: List<MapHighlight> = selectedRow
            ?.takeUnless { r -> doc.index.event(r.eventKey)?.let { !hiddenOpen && EventMasks.placeHidden(doc, it.place) } == true }
            ?.let { maps.highlight(it.eventKey) }.orEmpty()

        val live = doc.maps.values.filter { doc.liveness.isMapLive(it.key) }
            .sortedWith(compareBy<WorldMap>({ it.parent != null }, { it.order }, { it.key }))
        val map = highlights.firstOrNull()?.let { h -> live.firstOrNull { it.key == h.mapKey } }
            ?: live.firstOrNull { it.key == chosenMap }
            ?: live.firstOrNull()
        val highlight = highlights.firstOrNull { it.mapKey == map?.key }

        if (map == null) {
            return TimelineMapUi(TimelineMaps.HINT, rows, emptyList(), null, "", 4f / 3f, emptyList(), emptyList(), null)
        }
        val shown = MapReveal.at(doc, map, lens, hiddenOpen).items.filter { it.visible }
        val shownPins = shown.filter { it.kind == OverlayKind.PIN }.map { it.key }.toSet()
        val shownRegions = shown.filter { it.kind == OverlayKind.REGION }.map { it.key }.toSet()

        val change = maps.regionChanges().firstOrNull { it.mapKey == map.key && it.regionKey in shownRegions }
        val compare = change?.let { compareUi(doc, maps, it, lens, compareMode) }
        val pins = map.pins.filter { it.key in shownPins }.map { pin ->
            MapPinUi(pin.key, doc.index.place(pin.placeKey)?.name.orEmpty(), pin.x, pin.y, highlight?.pinKeys?.contains(pin.key) == true)
        }
        val regions = map.regions.filter { it.key in shownRegions }.map { region ->
            val isChanged = change != null && region.key == change.regionKey
            val to = change?.let { pointOf(doc, it) }
            val vertices = when {
                isChanged && compareMode == CompareMode.CHANGE && to != null -> RegionShapes.shapeAt(doc, map.key, region, to)
                else -> RegionShapes.shapeAt(doc, map.key, region, lens)
            }
            val added = if (isChanged && compareMode == CompareMode.BOTH && to != null) {
                RegionShapes.compare(doc, map.key, region, lens, to).takeIf { it.changed }?.after
            } else {
                null
            }
            MapRegionUi(region.key, region.name, vertices, highlight?.regionKey == region.key, added)
        }
        return TimelineMapUi(
            hint = TimelineMaps.HINT,
            rows = rows,
            maps = live.map { it.key to it.name },
            mapKey = map.key,
            mapName = map.name,
            aspect = map.baseSize?.takeIf { it.w > 0 && it.h > 0 }?.let { it.w.toFloat() / it.h } ?: (4f / 3f),
            pins = pins,
            regions = regions,
            compare = compare,
        )
    }

    /** The lens point at which a region record applies (the end of its unit; a world event = 최신). */
    fun pointOf(doc: ProjectDoc, change: RegionChange): LensPoint? = when (val a = change.record.anchor) {
        is Anchor.UnitStart -> LensPoint.UnitEnd(a.unitKey)
        is Anchor.UnitEnd -> LensPoint.UnitEnd(a.unitKey)
        is Anchor.AtBlock -> doc.index.block(a.blockKey)?.let { LensPoint.UnitEnd(it.sceneKey) }
        is Anchor.WorldEvent -> LensPoint.Latest
        else -> null
    }

    private fun compareUi(doc: ProjectDoc, maps: TimelineMaps, change: RegionChange, lens: LensPoint, mode: CompareMode): RegionCompareUi? {
        val to = pointOf(doc, change) ?: return null
        val compared = maps.compare(change.mapKey, change.regionKey, lens, to) ?: return null
        val (name, line) = compared.second
        return RegionCompareUi(
            chips = listOf(lensCode(doc, lens), "${change.pointLabel} ◆", WorldCopy.COMPARE),
            mode = mode,
            captionName = name,
            captionLine = line,
            dottedNote = if (mode == CompareMode.BOTH) maps.dottedNote(to) else null,
        )
    }

    /** `1-7` for the lens (at 최신 the 집필 선's episode). */
    fun lensCode(doc: ProjectDoc, lens: LensPoint): String {
        val codes = StoryCodes(doc)
        return when (lens) {
            LensPoint.Latest -> doc.manuscript.writingLineSceneKey?.let(codes::episodeCode) ?: WorldCopy.LATEST
            is LensPoint.UnitEnd -> codes.unitLabel(lens.unitKey) ?: WorldCopy.LATEST
        }
    }
}
