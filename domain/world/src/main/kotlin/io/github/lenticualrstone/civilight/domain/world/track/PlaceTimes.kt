package io.github.lenticualrstone.civilight.domain.world.track

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.time.Moment
import io.github.lenticualrstone.civilight.domain.world.time.Moments
import io.github.lenticualrstone.civilight.domain.world.time.SceneCastSource
import io.github.lenticualrstone.civilight.domain.world.time.SceneTimeSource
import io.github.lenticualrstone.civilight.domain.world.time.SceneWhen
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.TimelineEvent

/** A row of `이곳의 시간` (WLD-63). */
sealed interface PlaceTimeRow {
    /** `1195 · 대정전`, sub `세계 사건 · 하층 거리의 등이 …`. */
    data class EventRow(val eventKey: Key, val time: String, val title: String, val sub: String, val dimmed: Boolean) : PlaceTimeRow

    /** `1-2 씬 1 · 새벽 순찰`, sub `1일째 새벽 · 리엔`, `대정전 그날 · 세라`, `계획 · 하르트`. */
    data class SceneRow(val sceneKey: Key, val time: String, val title: String, val sub: String, val dimmed: Boolean) : PlaceTimeRow

    /** `시점 1-7 · 이 아래는 아직` (acc, dashed). */
    data class Divider(val text: String) : PlaceTimeRow
}

/**
 * `이곳의 시간` (WLD-63): world events whose place is this place (or a map region holding its pin)
 * and live scenes at this place, in world-time order. Flashbacks sit at their own era (rule 11 §4,
 * Q-24 default); at equal time a world event comes first (`[PROPOSED]` tie rule), then manuscript
 * order. Rows after the lens point (scenes starting after it, events not yet revealed) go below the
 * divider, dimmed. Scenes without 때 follow the timed rows of their side.
 */
class PlaceTimes(
    private val doc: ProjectDoc,
    times: SceneTimeSource,
    private val cast: SceneCastSource,
) {
    private val moments = Moments(doc, times)
    private val timesSource = times
    private val codes = StoryCodes(doc)

    private data class Item(val absolute: Long, val tier: Int, val manuscript: Int, val after: Boolean, val row: PlaceTimeRow)

    fun rows(placeKey: Key, lens: LensPoint = LensPoint.Latest): List<PlaceTimeRow> {
        if (doc.index.place(placeKey) == null) return emptyList()
        val regions = PlaceRefs.regionsHolding(doc, placeKey).map { it.second.key }.toSet()
        val lensRank = doc.manuscript.rankOf(lens)
        val writing = doc.manuscript.writingLineRank
        val items = ArrayList<Item>()

        for (event in doc.world.events.filter { it.deletedAt == null && it.kind.known != EventKind.STORY }) {
            if (!PlaceRefs.refersTo(doc, event.place, placeKey, regions)) continue
            val moment = moments.ofEvent(event.key)
            val after = !doc.manuscript.isVisible(event.revealPoint, lens)
            items += Item(moment?.absolute ?: Long.MAX_VALUE, 0, -1, after, eventRow(event, moment, after))
        }

        for ((i, scene) in doc.manuscript.liveScenes.withIndex()) {
            if (!PlaceRefs.refersTo(doc, scene.place, placeKey, emptySet())) continue
            val start = doc.manuscript.rankOf(Position.Start(scene.key)) ?: Rank.UNPLACED
            val after = start > lensRank
            val planned = writing != null && start > writing
            val moment = moments.ofScene(scene.key)
            val names = cast.castOf(scene.key).mapNotNull { doc.index.character(it)?.takeIf { c -> c.deletedAt == null }?.name }
            val timePart = when (val w = timesSource.timeOf(scene.key)) {
                is SceneWhen.Story -> w.episodeLabel
                is SceneWhen.Flashback -> w.label
                null -> null
            }
            val sub = listOfNotNull(if (planned) "계획" else timePart, names.joinToString(Copy.SEP).takeIf { it.isNotEmpty() })
                .joinToString(Copy.SEP)
            val row = PlaceTimeRow.SceneRow(
                sceneKey = scene.key,
                time = codes.sceneLabel(scene.key).orEmpty(),
                title = scene.title ?: "미정",
                sub = sub,
                dimmed = after,
            )
            items += Item(moment?.absolute ?: Long.MAX_VALUE, 1, i, after, row)
        }

        val order = compareBy<Item>({ it.absolute }, { it.tier }, { it.manuscript })
        val before = items.filterNot { it.after }.sortedWith(order).map { it.row }
        val later = items.filter { it.after }.sortedWith(order).map { it.row }
        if (later.isEmpty()) return before
        return before + PlaceTimeRow.Divider("시점 ${lensPoint(lens)}${Copy.SEP}이 아래는 아직") + later
    }

    private fun eventRow(event: TimelineEvent, moment: Moment?, after: Boolean): PlaceTimeRow.EventRow {
        val year = moment?.let(moments::yearOf)?.toString().orEmpty()
        val fragment = event.description.docText.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
        return PlaceTimeRow.EventRow(
            eventKey = event.key,
            time = year,
            title = event.title,
            sub = listOfNotNull("세계 사건", fragment).joinToString(Copy.SEP),
            dimmed = after,
        )
    }

    /** `1-7` for the lens point (at 최신 the 집필 선's episode). */
    private fun lensPoint(lens: LensPoint): String = when (lens) {
        LensPoint.Latest -> doc.manuscript.writingLineSceneKey?.let(codes::episodeCode) ?: "최신"
        is LensPoint.UnitEnd -> codes.unitLabel(lens.unitKey) ?: "최신"
    }
}
