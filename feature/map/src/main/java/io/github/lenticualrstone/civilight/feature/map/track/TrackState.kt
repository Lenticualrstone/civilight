package io.github.lenticualrstone.civilight.feature.map.track

import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.domain.world.map.Routes
import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.travel.TravelCheck
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.domain.world.track.CharacterTrack
import io.github.lenticualrstone.civilight.domain.world.track.CharacterTracks
import io.github.lenticualrstone.civilight.domain.world.track.TrackStop
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.feature.map.reveal.RevealState
import io.github.lenticualrstone.civilight.feature.map.reveal.RevealStop
import io.github.lenticualrstone.civilight.feature.map.view.LayerChipUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/** A character chip `리엔` (multi-select; the focused one in `acc`). */
data class TrackChipUi(val key: Key, val name: String, val selected: Boolean, val focused: Boolean)

/** A stop drawn on the map: the episode chip (`1-2`) at the place. */
data class TrackStopMark(val label: String, val at: Point, val focused: Boolean)

/** A line between two stops: along the route, or straight `추정`; dashed for characters not in focus or legs past a hidden scene. */
data class TrackLine(val points: List<Point>, val label: String?, val focused: Boolean, val dashed: Boolean)

/** The position chip at the lens point: initial + name at the latest stop at or before it. */
data class TrackPosition(val name: String, val at: Point, val focused: Boolean, val characterKey: Key = "")

/** A row of `리엔 · 씬 순서`: `1-2 | 하층 거리 | 씬 1 · 1일째 새벽`. */
data class TrackRowUi(
    val sceneKey: Key,
    val episode: String,
    val place: String,
    val detail: String,
    val onMap: Boolean,
    /** The stop the focused character stands at for the scrubber's point (board: `sel` with the `acc` edge). */
    val current: Boolean = false,
)

/**
 * The `▲ 이동 시간` card of the panel (board `MapTrack`): `하르트 · 1-5 → 1-7`, `도보 2일 4시간 · 있음 9시간 40분`.
 * Only while the travel check is on (설정 `이동 점검`, off by default).
 */
data class TrackWarningUi(val who: String, val detail: String, val sceneKey: Key)

/** 인물 동선 (WLD-62, board `MapTrack`). */
data class TrackUi(
    val mapKey: Key,
    /** `세계 지도 › 하층 구역`. */
    val small: String,
    val base: MapBase,
    val scene: MapScene,
    val chips: List<TrackChipUi>,
    /** `리엔 · 씬 순서`, or null with no character. */
    val listTitle: String?,
    val rows: List<TrackRowUi>,
    /** `장소` tab: `하층 거리 · 2`. */
    val places: List<String>,
    val layerChips: List<LayerChipUi>,
    val stops: List<TrackStopMark>,
    val lines: List<TrackLine>,
    val positions: List<TrackPosition>,
    val scrubber: List<RevealStop>,
    val warning: TrackWarningUi? = null,
)

/**
 * 인물 동선 state (WLD-62) over `domain:world` `CharacterTracks`: stops in time order (flashbacks,
 * scenes without place or with a deleted place and, while locked, hidden scenes left out), legs
 * along a route when one joins both places else straight `추정`, the position at the lens.
 * A stop whose place has no pin on the shown map stands at its ancestor pin there (the pin whose
 * child map holds it, as WLD-66 does for distances); a region stands at the middle of its shape.
 */
object TrackState {
    fun tracks(doc: ProjectDoc, hiddenOpen: Boolean): CharacterTracks {
        val engine = StoryEngine(doc)
        return CharacterTracks(doc, engine, engine, hiddenUnlocked = hiddenOpen)
    }

    /** Characters with at least one stop, in cast order (hidden ones only while open). */
    fun candidates(doc: ProjectDoc, tracks: CharacterTracks): List<CharacterTrack> =
        doc.cast.characters.filter { it.deletedAt == null }.mapNotNull { c -> tracks.track(c.key)?.takeIf { it.stops.isNotEmpty() } }

    /** The point of a place or region on [map], directly or through an ancestor pin. */
    fun pointOn(doc: ProjectDoc, map: WorldMap, place: PlaceRef, lens: LensPoint): Point? {
        when (place.kind.known) {
            PlaceRefKind.PLACE -> {
                map.pins.firstOrNull { it.placeKey == place.key && doc.liveness.isOverlayLive(map.key, it) }?.let { return Point(it.x, it.y) }
                for (other in doc.maps.values) {
                    if (other.key == map.key || !doc.liveness.isMapLive(other.key)) continue
                    if (other.pins.none { it.placeKey == place.key && doc.liveness.isOverlayLive(other.key, it) }) continue
                    ancestorPin(doc, other, map)?.let { return it }
                }
            }
            PlaceRefKind.REGION -> {
                map.regions.firstOrNull { it.key == place.key }?.let { r -> return middle(RegionShapes.shapeAt(doc, map.key, r, lens)) }
                for (other in doc.maps.values) {
                    if (other.key == map.key || other.regions.none { it.key == place.key }) continue
                    ancestorPin(doc, other, map)?.let { return it }
                }
            }
            null -> Unit
        }
        return null
    }

    private fun direct(doc: ProjectDoc, map: WorldMap, place: PlaceRef): Boolean = when (place.kind.known) {
        PlaceRefKind.PLACE -> map.pins.any { it.placeKey == place.key && doc.liveness.isOverlayLive(map.key, it) }
        PlaceRefKind.REGION -> map.regions.any { it.key == place.key }
        null -> false
    }

    private fun ancestorPin(doc: ProjectDoc, from: WorldMap, target: WorldMap): Point? {
        var current = from
        val seen = HashSet<Key>()
        while (seen.add(current.key)) {
            val parent = current.parent ?: return null
            val parentMap = doc.maps[parent.mapKey] ?: return null
            if (parentMap.key == target.key) return parentMap.pins.firstOrNull { it.key == parent.pinKey }?.let { Point(it.x, it.y) }
            current = parentMap
        }
        return null
    }

    private fun middle(shape: List<Point>): Point? =
        if (shape.isEmpty()) null else Point(shape.map { it.x }.average().toFloat(), shape.map { it.y }.average().toFloat())

    /** The map to show: the route's, else the one where the focused character's stops stand most, else the first top-level map. */
    fun chooseMap(doc: ProjectDoc, mapKey: Key?, focus: CharacterTrack?, lens: LensPoint): WorldMap? {
        val live = doc.maps.values.filter { doc.liveness.isMapLive(it.key) && !MapScales.isNodeMap(it) }.sortedBy { it.order }
        mapKey?.let { k -> live.firstOrNull { it.key == k } }?.let { return it }
        // The map where most stops have their own pin or region wins; the ancestor-pin fallback breaks ties.
        val best = focus?.let { t ->
            live.maxByOrNull { m -> t.stops.count { direct(doc, m, it.place) } * 1000 + t.stops.count { pointOn(doc, m, it.place, lens) != null } }
        }
        return best ?: live.firstOrNull { it.parent == null } ?: live.firstOrNull()
    }

    fun build(
        doc: ProjectDoc,
        mapKey: Key?,
        selected: List<Key>,
        lens: LensPoint,
        hiddenOpen: Boolean,
        layerFilter: Key?,
        checkTravel: Boolean = false,
    ): TrackUi? {
        val tracks = tracks(doc, hiddenOpen)
        val all = candidates(doc, tracks)
        val chosen = selected.filter { k -> all.any { it.characterKey == k } }
        val focusKey = chosen.lastOrNull()
        val focus = all.firstOrNull { it.characterKey == focusKey }?.let { masked(doc, it, hiddenOpen) }
        val map = chooseMap(doc, mapKey, focus, lens) ?: return null
        val filter = layerFilter?.takeIf { k -> map.layers.any { it.key == k } }
        val shown = chosen.mapNotNull { k -> all.firstOrNull { it.characterKey == k } }.map { masked(doc, it, hiddenOpen) }

        val stops = ArrayList<TrackStopMark>()
        val lines = ArrayList<TrackLine>()
        val positions = ArrayList<TrackPosition>()
        for (t in shown) {
            val focused = t.characterKey == focusKey
            for (s in t.stops) pointOn(doc, map, s.place, lens)?.let { stops += TrackStopMark(s.episodeLabel, it, focused) }
            for (leg in t.legs) {
                val a = pointOn(doc, map, leg.from.place, lens) ?: continue
                val b = pointOn(doc, map, leg.to.place, lens) ?: continue
                val route = if (leg.mapKey == map.key) map.routes.firstOrNull { it.key == leg.routeKey } else null
                val along = route?.let { r ->
                    val pts = Routes.points(map, r)
                    if (pts.size >= 2 && dist(pts.first(), a) > dist(pts.last(), a)) pts.reversed() else pts
                }
                val points = along?.takeIf { it.size >= 2 } ?: listOf(a, b)
                lines += TrackLine(points, leg.label.takeIf { route == null || leg.dotted }, focused, dashed = !focused || leg.dotted || route == null)
            }
            tracks.positionAt(t, lens)?.let { stop -> pointOn(doc, map, stop.place, lens)?.let { positions += TrackPosition(nameOf(doc, t), it, focused, t.characterKey) } }
        }
        val episodes = RevealState.episodes(doc)
        val at = RevealState.lensEpisode(doc, lens, episodes)
        val codes = io.github.lenticualrstone.civilight.domain.world.common.StoryCodes(doc)
        return TrackUi(
            mapKey = map.key,
            small = MapCopy.crumbs(MapViews.breadcrumb(doc, map).map { it.text }),
            base = MapBases.of(map),
            scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.VIEW, filter),
            chips = all.map { TrackChipUi(it.characterKey, nameOf(doc, it), it.characterKey in chosen, it.characterKey == focusKey) },
            listTitle = focus?.listTitle,
            rows = focus?.let { t ->
                val here = tracks.positionAt(t, lens)?.sceneKey
                t.stops.map { row(doc, map, it, lens).copy(current = it.sceneKey == here) }
            }.orEmpty(),
            places = focus?.stops.orEmpty().groupingBy { it.placeName }.eachCount().map { (name, n) -> "$name${MapCopy.SEP}$n" },
            layerChips = listOf(LayerChipUi(null, MapCopy.ALL_PINS, filter == null)) + MapEdits.layers(map).map { LayerChipUi(it.key, it.name, it.key == filter) },
            stops = stops,
            lines = lines,
            positions = positions,
            scrubber = episodes.map { RevealStop(it.key, codes.unitLabel(it.key).orEmpty(), it.key == at) },
            warning = if (checkTravel) warning(doc, shown, focusKey) else null,
        )
    }

    /** The `▲ 이동 시간` card alone (the screen keys it apart from the scrubber point, so `재생` does not re-run the check). */
    fun warningOf(doc: ProjectDoc, selected: List<Key>, hiddenOpen: Boolean): TrackWarningUi? {
        val tracks = tracks(doc, hiddenOpen)
        val all = candidates(doc, tracks)
        val chosen = selected.filter { k -> all.any { it.characterKey == k } }
        val shown = chosen.mapNotNull { k -> all.firstOrNull { it.characterKey == k } }.map { masked(doc, it, hiddenOpen) }
        return warning(doc, shown, chosen.lastOrNull())
    }

    /**
     * The first open `▲` of the travel check between two stops of a shown track (the focused character's first),
     * not accepted (`맞음`). Rule 19: only pairs whose both scenes are stops of the masked track, so a hidden
     * scene, place or character never shows here while locked.
     */
    fun warning(doc: ProjectDoc, shown: List<CharacterTrack>, focusKey: Key?): TrackWarningUi? {
        if (shown.isEmpty()) return null
        val warnings = TravelCheck(doc, TravelSettings(checkTravel = true, checkNoPlace = false)).warnings
            .filterNot { Acks.isAcknowledged(doc, it) }
        for (t in shown.sortedByDescending { it.characterKey == focusKey }) {
            val stops = t.stops.associateBy { it.sceneKey }
            val w = warnings.firstOrNull { it.characterKey == t.characterKey && it.fromSceneKey in stops && it.toSceneKey in stops } ?: continue
            val from = stops.getValue(w.fromSceneKey).episodeLabel
            val to = stops.getValue(w.toSceneKey).episodeLabel
            return TrackWarningUi(
                who = "${nameOf(doc, t)}${MapCopy.SEP}$from${MapCopy.ARROW}$to",
                detail = "${w.needText}${MapCopy.SEP}${MapCopy.TRACK_HAVE} ${w.haveText}",
                sceneKey = w.toSceneKey,
            )
        }
        return null
    }

    /** Rule 19: while locked, a stop at a hidden place (or a region of a hidden place or faction) and its legs stay out. */
    fun isMasked(doc: ProjectDoc, ref: PlaceRef, hiddenOpen: Boolean): Boolean {
        if (hiddenOpen) return false
        return when (ref.kind.known) {
            PlaceRefKind.PLACE -> doc.index.place(ref.key)?.hidden == true
            PlaceRefKind.REGION -> doc.maps.values.flatMap { it.regions }.firstOrNull { it.key == ref.key }
                ?.let { io.github.lenticualrstone.civilight.domain.world.map.MapReveal.isMasked(doc, it, false) } == true
            null -> false
        }
    }

    private fun masked(doc: ProjectDoc, t: CharacterTrack, hiddenOpen: Boolean): CharacterTrack {
        if (hiddenOpen) return t
        val stops = t.stops.filterNot { isMasked(doc, it.place, false) }
        if (stops.size == t.stops.size) return t
        val keys = stops.map { it.sceneKey }.toSet()
        return t.copy(stops = stops, legs = t.legs.filter { it.from.sceneKey in keys && it.to.sceneKey in keys })
    }

    private fun dist(a: Point, b: Point): Float = kotlin.math.hypot(a.x - b.x, a.y - b.y)

    private fun nameOf(doc: ProjectDoc, t: CharacterTrack): String = doc.index.character(t.characterKey)?.name.orEmpty()

    private fun row(doc: ProjectDoc, map: WorldMap, s: TrackStop, lens: LensPoint): TrackRowUi {
        val (episode, place, detail) = s.row
        return TrackRowUi(s.sceneKey, episode, place, detail, pointOn(doc, map, s.place, lens) != null)
    }

    /**
     * Chip taps: an unselected character is added and focused; tapping the focused one removes
     * it; tapping another selected one focuses it (the focus is the last in the list).
     */
    fun tapChip(selected: List<Key>, key: Key): List<Key> = when {
        key !in selected -> selected + key
        selected.lastOrNull() == key -> selected - key
        else -> selected - key + key
    }

    /** The first chip when nothing is chosen yet (or the route's character). */
    fun initial(doc: ProjectDoc, hiddenOpen: Boolean, characterKey: Key?): List<Key> {
        val all = candidates(doc, tracks(doc, hiddenOpen))
        val start = characterKey?.takeIf { k -> all.any { it.characterKey == k } } ?: all.firstOrNull()?.characterKey
        return listOfNotNull(start)
    }

    /**
     * Where `▶` starts: the first episode when the scrubber stands at 최신 (no episode marked) or on the last one,
     * else null (it plays on from where it stands).
     */
    fun playStart(stops: List<RevealStop>): Key? =
        if (stops.none { it.current } || nextStop(stops) == null) stops.firstOrNull()?.unitKey else null

    /** `▶` steps through the scrubber: the next episode after the point, or null at the end. */
    fun nextStop(stops: List<RevealStop>): Key? {
        val i = stops.indexOfFirst { it.current }
        return stops.getOrNull(i + 1)?.unitKey
    }
}

/**
 * The way a position chip takes between two stops (motion audit row 11): along the leg that joins them (a route, or the
 * straight `추정` line), in whichever direction it is stored, else straight. Pure, so it is tested on the JVM.
 */
object TrackPath {
    private const val SAME = 1e-4f

    private fun same(a: Point, b: Point) = kotlin.math.abs(a.x - b.x) < SAME && kotlin.math.abs(a.y - b.y) < SAME

    /** The polyline from [from] to [to]: a line of [lines] that starts at one and ends at the other, else the straight way. */
    fun between(lines: List<TrackLine>, from: Point, to: Point): List<Point> {
        for (l in lines) {
            val pts = l.points
            if (pts.size < 2) continue
            if (same(pts.first(), from) && same(pts.last(), to)) return pts
            if (same(pts.first(), to) && same(pts.last(), from)) return pts.reversed()
        }
        return listOf(from, to)
    }

    /** The point [t] (0–1) of the way along [path] by length. */
    fun pointAt(path: List<Point>, t: Float): Point {
        if (path.isEmpty()) return Point(0f, 0f)
        if (path.size == 1 || t <= 0f) return path.first()
        if (t >= 1f) return path.last()
        val lengths = path.zipWithNext { a, b -> kotlin.math.hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat() }
        val total = lengths.sum()
        if (total <= 0f) return path.last()
        var left = total * t
        for (i in lengths.indices) {
            if (left <= lengths[i] || i == lengths.lastIndex) {
                val k = if (lengths[i] <= 0f) 1f else (left / lengths[i]).coerceIn(0f, 1f)
                val a = path[i]
                val b = path[i + 1]
                return Point(a.x + (b.x - a.x) * k, a.y + (b.y - a.y) * k)
            }
            left -= lengths[i]
        }
        return path.last()
    }
}
