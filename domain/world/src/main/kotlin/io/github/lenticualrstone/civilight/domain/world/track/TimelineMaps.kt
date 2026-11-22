package io.github.lenticualrstone.civilight.domain.world.track

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.domain.world.map.ShapeComparison
import io.github.lenticualrstone.civilight.domain.world.time.Moments
import io.github.lenticualrstone.civilight.domain.world.time.SceneTimeSource
import io.github.lenticualrstone.civilight.domain.world.time.SceneWhen
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.TimelineEvent

/** What the map lights up for a selected event: the pins of its place, or its region (WLD-64). */
data class MapHighlight(val mapKey: Key, val pinKeys: List<Key>, val regionKey: Key?)

/** A row of the `연표 + 지도` list: `1200 · 1-3 | 리엔, 결사 합류 | 씬 2 · 1일째 아침 | 결사의 문`. */
data class TimelineMapRow(
    val eventKey: Key,
    val time: String,
    val title: String,
    val sub: String,
    val placeName: String?,
    /** `세력 영역 변화 ◆`: a region shape changes at this event. */
    val regionChange: Boolean,
    val highlights: List<MapHighlight>,
)

/** One `◆` on a region's shape, for the chips `1-7 · 1-9 ◆ · 비교`. */
data class RegionChange(val mapKey: Key, val regionKey: Key, val record: ChangeRecord, val pointLabel: String)

/**
 * `연표 + 지도` (WLD-64) and the faction-region readings it uses (WLD-65): each event's place on the
 * map, rows of the list, region shape changes and the two-point comparison.
 */
class TimelineMaps(private val doc: ProjectDoc, private val times: SceneTimeSource) {
    private val moments = Moments(doc, times)
    private val codes = StoryCodes(doc)

    companion object {
        const val HINT = "사건마다 장소 · 고르면 지도에서 강조"
        const val REGION_CHANGE = "세력 영역 변화 ◆"
        const val DOTTED_NOTE_PREFIX = "점선 = "
    }

    /** Where an event is on the maps: every live pin of its place, or its region. */
    fun highlight(eventKey: Key): List<MapHighlight> {
        val ref = doc.index.event(eventKey)?.place ?: return emptyList()
        return when (ref.kind.known) {
            PlaceRefKind.PLACE -> PlaceRefs.livePins(doc, ref.key).groupBy({ it.first.key }, { it.second.key })
                .map { (mapKey, pins) -> MapHighlight(mapKey, pins, null) }
            PlaceRefKind.REGION -> doc.index.region(ref.key)?.takeIf { doc.liveness.isMapLive(it.first.key) }
                ?.let { listOf(MapHighlight(it.first.key, emptyList(), it.second.key)) }.orEmpty()
            null -> emptyList()
        }
    }

    /** Live events in time order (world events first at equal time). */
    fun rows(): List<TimelineMapRow> = doc.world.events.filter { it.deletedAt == null }
        .sortedWith(compareBy<TimelineEvent>({ moments.ofEvent(it.key)?.absolute ?: Long.MAX_VALUE }, { if (it.kind.known == EventKind.STORY) 1 else 0 }, { it.order }))
        .map { row(it) }

    private fun row(event: TimelineEvent): TimelineMapRow {
        val moment = moments.ofEvent(event.key)
        val year = moment?.let(moments::yearOf)?.toString().orEmpty()
        val scene = event.sceneKey?.takeIf { event.kind.known == EventKind.STORY }
        val time = listOfNotNull(year.takeIf { it.isNotEmpty() }, scene?.let(codes::episodeCode)).joinToString(Copy.SEP)
        val sub = if (scene != null) {
            val unit = doc.index.unit(scene)
            listOfNotNull(unit?.let { "씬 ${codes.ordinal(it)}" }, (times.timeOf(scene) as? SceneWhen.Story)?.episodeLabel).joinToString(Copy.SEP)
        } else {
            "세계 사건"
        }
        return TimelineMapRow(
            eventKey = event.key,
            time = time,
            title = event.title,
            sub = sub,
            placeName = PlaceRefs.name(doc, event.place),
            regionChange = regionChangesAt(event).isNotEmpty(),
            highlights = highlight(event.key),
        )
    }

    /** Region-shape records anchored on the event, on its scene, or on a block of its scene. */
    private fun regionChangesAt(event: TimelineEvent): List<ChangeRecord> = shapeRecords().filter { r ->
        when (val a = r.anchor) {
            is Anchor.WorldEvent -> a.eventKey == event.key
            is Anchor.UnitStart -> event.sceneKey != null && a.unitKey == event.sceneKey
            is Anchor.AtBlock -> event.sceneKey != null && doc.index.block(a.blockKey)?.sceneKey == event.sceneKey
            else -> false
        }
    }

    private fun shapeRecords(): List<ChangeRecord> =
        doc.changes.records.filter { it.target is ChangeTarget.RegionShape && doc.liveness.isLive(it) }

    /** Every `◆` on a region shape, in manuscript order of its anchor. */
    fun regionChanges(): List<RegionChange> = shapeRecords()
        .sortedWith(compareBy({ doc.manuscript.rankOf(it.anchor) }, { it.createdAt }, { it.key }))
        .map { r ->
            val target = r.target as ChangeTarget.RegionShape
            val label = when (val a = r.anchor) {
                is Anchor.UnitStart -> codes.unitLabel(a.unitKey)
                is Anchor.UnitEnd -> codes.unitLabel(a.unitKey)
                is Anchor.AtBlock -> codes.episodeOfBlock(a.blockKey)
                is Anchor.WorldEvent -> doc.index.event(a.eventKey)?.title
                else -> null
            }.orEmpty()
            RegionChange(target.mapKey, target.regionKey, r, label)
        }

    /** The comparison of a region at [from] and [to] (`1-7 · 1-9 ◆ · 비교`) and its caption. */
    fun compare(mapKey: Key, regionKey: Key, from: LensPoint, to: LensPoint): Pair<ShapeComparison, Pair<String, String>>? {
        val region = doc.maps[mapKey]?.regions?.firstOrNull { it.key == regionKey } ?: return null
        val comparison = RegionShapes.compare(doc, mapKey, region, from, to)
        return comparison to RegionShapes.caption(doc, region, comparison, to)
    }

    /** `점선 = 1-9부터 더해진 곳`. */
    fun dottedNote(to: LensPoint): String {
        val point = when (to) {
            LensPoint.Latest -> "최신"
            is LensPoint.UnitEnd -> codes.unitLabel(to.unitKey).orEmpty()
        }
        return "${DOTTED_NOTE_PREFIX}${point}부터 더해진 곳"
    }
}
