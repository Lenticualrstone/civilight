package io.github.lenticualrstone.civilight.domain.world.track

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.map.MapReveal
import io.github.lenticualrstone.civilight.domain.world.time.SceneCastSource
import io.github.lenticualrstone.civilight.domain.world.time.SceneTimeSource
import io.github.lenticualrstone.civilight.domain.world.time.SceneWhen
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** One stop of a character's track: a scene with a live place where the character is present. */
data class TrackStop(
    val sceneKey: Key,
    val place: PlaceRef,
    val placeName: String,
    /** `1-2` (the stop chip). */
    val episodeLabel: String,
    /** `씬 1`. */
    val sceneLabel: String,
    /** `1일째 새벽`, or null without 때. */
    val dayLabel: String?,
) {
    /** Side list row `1-2 | 하층 거리 | 씬 1 · 1일째 새벽`. */
    val row: Triple<String, String, String>
        get() = Triple(episodeLabel, placeName, listOfNotNull(sceneLabel, dayLabel).joinToString(Copy.SEP))
}

/** A line between two consecutive stops: along a route that joins both places, else straight `추정`. */
data class TrackLeg(
    val from: TrackStop,
    val to: TrackStop,
    /** The route followed, with its map; null for a straight line. */
    val routeKey: Key?,
    val mapKey: Key?,
    /** `추정` on a straight line; `숨긴 씬` when the leg passes a hidden scene (dotted). */
    val label: String?,
    val dotted: Boolean,
) {
    val estimated: Boolean get() = routeKey == null
}

/** Why a scene the character is in is not a stop (`◇` rows of the UI). */
enum class SkipReason { FLASHBACK, NO_PLACE, DELETED_PLACE, HIDDEN_SCENE }

/** `인물 동선` of one character (WLD-62). */
data class CharacterTrack(
    val characterKey: Key,
    /** `리엔 · 씬 순서`. */
    val listTitle: String,
    val stops: List<TrackStop>,
    val legs: List<TrackLeg>,
    val skipped: Map<Key, SkipReason>,
) {
    companion object {
        const val TITLE = "인물 동선"
        const val NOTE = "장소 · 때는 씬 속성에서 · 선은 지도 경로를 따라 · 경로가 없으면 직선 ‘추정’"
        const val ESTIMATED = "추정"
        const val HIDDEN_SCENE = "숨긴 씬"
    }
}

/**
 * Character tracks (WLD-62). Stops are live scenes where the character is in the cast ([cast]) and
 * that have a live place or region, ordered by resolved time ([times]; rule 11 §5), not by
 * manuscript order. Flashbacks, scenes without place, scenes whose place was deleted and (while
 * locked) hidden scenes are left out; a leg that passes a hidden scene is dotted `숨긴 씬`. Hidden
 * characters have no track while locked. Scenes without 때 keep the time of the nearest earlier
 * timed scene in manuscript order (`ED`).
 */
class CharacterTracks(
    private val doc: ProjectDoc,
    private val times: SceneTimeSource,
    private val cast: SceneCastSource,
    private val hiddenUnlocked: Boolean = false,
) {
    private val codes = StoryCodes(doc)

    /** Sort minute of each live scene: its own time, else the previous timed scene's (manuscript order). */
    private val sortMinute: Map<Key, Long> by lazy {
        val out = HashMap<Key, Long>()
        var last = Long.MIN_VALUE
        for (scene in doc.manuscript.liveScenes) {
            val w = times.timeOf(scene.key) as? SceneWhen.Story
            if (w != null) last = (w.dayMin - 1L) * DAY_MINUTES + (w.minuteMin ?: 0)
            out[scene.key] = last
        }
        out
    }

    private val sceneIndex: Map<Key, Int> by lazy { doc.manuscript.liveScenes.withIndex().associate { (i, s) -> s.key to i } }

    /** Live scenes in time order (ties in manuscript order). */
    fun inTimeOrder(scenes: List<StoryUnit>): List<StoryUnit> =
        scenes.sortedWith(compareBy<StoryUnit> { sortMinute[it.key] ?: Long.MIN_VALUE }.thenBy { sceneIndex[it.key] ?: Int.MAX_VALUE })

    fun track(characterKey: Key): CharacterTrack? {
        val character = doc.index.character(characterKey) ?: return null
        if (character.deletedAt != null || (character.hidden && !hiddenUnlocked)) return null
        val present = doc.manuscript.liveScenes.filter { characterKey in cast.castOf(it.key) }
        val skipped = LinkedHashMap<Key, SkipReason>()
        val stops = ArrayList<TrackStop>()
        val dottedBefore = HashSet<Int>()
        var pendingHidden = false
        for (scene in inTimeOrder(present)) {
            val reason = when {
                times.timeOf(scene.key) is SceneWhen.Flashback -> SkipReason.FLASHBACK
                scene.place == null -> SkipReason.NO_PLACE
                !PlaceRefs.isLive(doc, scene.place) -> SkipReason.DELETED_PLACE
                scene.hidden && !hiddenUnlocked -> SkipReason.HIDDEN_SCENE
                else -> null
            }
            if (reason != null) {
                skipped[scene.key] = reason
                if (reason == SkipReason.HIDDEN_SCENE) pendingHidden = true
                continue
            }
            if (pendingHidden && stops.isNotEmpty()) dottedBefore += stops.size
            pendingHidden = false
            stops += stopOf(scene, scene.place!!)
        }
        val legs = stops.zipWithNext().mapIndexed { i, (a, b) -> legOf(a, b, dotted = (i + 1) in dottedBefore) }
        return CharacterTrack(characterKey, "${character.name}${Copy.SEP}씬 순서", stops, legs, skipped)
    }

    private fun stopOf(scene: StoryUnit, place: PlaceRef): TrackStop {
        val w = times.timeOf(scene.key) as? SceneWhen.Story
        return TrackStop(
            sceneKey = scene.key,
            place = place,
            placeName = PlaceRefs.name(doc, place).orEmpty(),
            episodeLabel = codes.episodeCode(scene.key).orEmpty(),
            sceneLabel = "씬 ${codes.ordinal(scene)}",
            dayLabel = w?.episodeLabel,
        )
    }

    private fun legOf(a: TrackStop, b: TrackStop, dotted: Boolean): TrackLeg {
        val route = routeBetween(a.place, b.place)
        val label = when {
            dotted -> CharacterTrack.HIDDEN_SCENE
            route == null -> CharacterTrack.ESTIMATED
            else -> null
        }
        return TrackLeg(a, b, route?.second, route?.first, label, dotted)
    }

    /** A live route on one map whose points include a pin of each place: (mapKey, routeKey). */
    private fun routeBetween(a: PlaceRef, b: PlaceRef): Pair<Key, Key>? {
        if (a.kind.known != PlaceRefKind.PLACE || b.kind.known != PlaceRefKind.PLACE || a.key == b.key) return null
        for ((map, pinA) in PlaceRefs.livePins(doc, a.key)) {
            val pinB = map.pins.firstOrNull { it.placeKey == b.key && doc.liveness.isOverlayLive(map.key, it) } ?: continue
            // Rule 19: while locked, a route that passes a pin of a hidden place is not followed (the line would show
            // where that place is); the leg falls back to another route or the straight `추정` line.
            val masked = if (hiddenUnlocked) emptySet() else map.pins.filter { MapReveal.isMasked(doc, it, false) }.mapTo(HashSet()) { it.key }
            val route = map.routes.firstOrNull { r ->
                r.points.any { it.pinKey == pinA.key } && r.points.any { it.pinKey == pinB.key } && r.points.none { it.pinKey in masked }
            }
            if (route != null) return map.key to route.key
        }
        return null
    }

    /**
     * The character's place at [lens] (the scrubber point): the latest stop in time order among the
     * stops whose scene starts at or before the lens point.
     */
    fun positionAt(track: CharacterTrack, lens: LensPoint): TrackStop? {
        val lensRank = doc.manuscript.rankOf(lens)
        return track.stops.lastOrNull { (doc.manuscript.rankOf(Position.Start(it.sceneKey)) ?: Rank.UNPLACED) <= lensRank }
    }

    /** Scrubber points: live episodes up to the 집필 선's episode, as `1-1 … 1-9` with their lens. */
    fun scrubber(): List<Pair<String, LensPoint>> {
        val episodeLevel = doc.project.structure.episodeLevel ?: return emptyList()
        return doc.index.allScenesInOrder.asSequence()
            .mapNotNull { s -> doc.index.ancestors(s).firstOrNull { it.level == episodeLevel } }
            .distinctBy { it.key }
            .filter { doc.liveness.isLive(it) }
            .map { codes.episodeCode(it.key).orEmpty() to LensPoint.UnitEnd(it.key) as LensPoint }
            .toList()
    }

    private companion object {
        const val DAY_MINUTES = 1440L
    }
}
