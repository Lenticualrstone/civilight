package io.github.lenticualrstone.civilight.domain.story.transition

import io.github.lenticualrstone.civilight.domain.story.checks.AckSubject
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.domain.story.time.BandTable
import io.github.lenticualrstone.civilight.domain.story.time.DAY_MINUTES
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.domain.story.time.clockAtMinutesAfterDayStart
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/** What changed since a direct transition text was written. */
enum class StaleKind(val word: String) { TIME("때"), PLACE("장소") }

/**
 * `◇ 전환 문구 · 쓴 뒤 때 바뀜 · 1-7 씬 4 · 06:10 → 07:00` (STO-40 row 17; or `장소 바뀜`), with
 * `자동으로 · 문구 고치기 · 맞음`.
 */
data class StaleTransition(
    val sceneKey: Key,
    val kind: StaleKind,
    /** `1-7 씬 4 · 06:10 → 07:00`. */
    val detail: String,
    override val fingerprint: String,
) : AckSubject {
    val title: String get() = "◇ 전환 문구 · 쓴 뒤 ${kind.word} 바뀜"
    val line: String get() = "$title · $detail"
    val actions: List<String> get() = listOf("자동으로", "문구 고치기", "맞음")
    override val checkType: String get() = CheckType.TRANSITION_STALE
    override val subjectKeys: List<Key> get() = listOf(sceneKey)
}

/** `◇ 공개 전 장소 · 전환 문구` (STO-40 row 18) with `직접 쓰기 · 맞음`; [detail] = `1-7 씬 4 · 결사의 문`. */
data class PlaceBeforeReveal(val sceneKey: Key, val placeKey: Key, val detail: String, override val fingerprint: String) : AckSubject {
    val title: String get() = "◇ 공개 전 장소 · 전환 문구"
    val actions: List<String> get() = listOf("직접 쓰기", "맞음")
    override val checkType: String get() = CheckType.TRANSITION_PLACE_BEFORE_REVEAL
    override val subjectKeys: List<Key> get() = listOf(sceneKey)
}

/** The two transition checks (R22 §2; emitted for SCR-62, rows 17–18). */
class TransitionChecks(private val transitions: Transitions) {
    private val doc = transitions.doc
    private val times = transitions.times
    private val codes = UnitCodes(doc)
    private val places = PlaceNames(doc)
    private val bands = BandTable(doc.project.sceneTime.bandStarts)

    /** Every stale direct text, manuscript order. */
    fun stale(): List<StaleTransition> = times.liveScenes.mapNotNull { stale(it.key) }

    /**
     * A direct text whose snapshot differs from the scene's place or resolved time now. A text
     * written before snapshots existed (no snapshot) is never stale (`ED`).
     */
    fun stale(sceneKey: Key): StaleTransition? {
        val scene = doc.index.unit(sceneKey) ?: return null
        if (transitions.mode(sceneKey) != TransitionMode.DIRECT) return null
        val then = scene.transitionSnapshot ?: return null
        val now = transitions.snapshot(sceneKey)
        val ref = codes.sceneRef(scene)
        val placeChanged = !samePlace(then.place, now.place)
        val dayChanged = then.dayRange != now.dayRange
        val todChanged = then.tRange != now.tRange
        if (!placeChanged && !dayChanged && !todChanged) return null
        val fingerprint = Fingerprints.of(CheckType.TRANSITION_STALE, sceneKey, then, now)
        if (placeChanged) {
            val from = then.place?.let(places::name) ?: "장소 없음"
            val to = now.place?.let(places::name) ?: "장소 없음"
            return StaleTransition(sceneKey, StaleKind.PLACE, "$ref · $from → $to", fingerprint)
        }
        fun side(day: IntSpan?, t: IntSpan?): String {
            if (day == null || t == null) return "때 없음"
            return listOfNotNull(
                dayText(day).takeIf { dayChanged },
                todText(t).takeIf { todChanged },
            ).joinToString(" ")
        }
        return StaleTransition(sceneKey, StaleKind.TIME, "$ref · ${side(then.dayRange, then.tRange)} → ${side(now.dayRange, now.tRange)}", fingerprint)
    }

    /**
     * An automatic text that names a place not yet visible on the map at this scene: no pin (or
     * region) of the place is visible at the scene's transition position (`ED`: a place without pins
     * is never flagged).
     */
    fun placeBeforeReveal(sceneKey: Key): PlaceBeforeReveal? {
        val scene = doc.index.unit(sceneKey) ?: return null
        if (transitions.mode(sceneKey) != TransitionMode.AUTO) return null
        val ref = scene.place ?: return null
        val points = revealPoints(ref) ?: return null
        val at = doc.manuscript.rankOf(Position.Transition(sceneKey)) ?: return null
        val visible = points.any { p -> p == null || doc.manuscript.rankOf(p) <= at }
        if (visible) return null
        val name = places.name(ref) ?: return null
        return PlaceBeforeReveal(
            sceneKey, ref.key, "${codes.sceneRef(scene)} · $name",
            Fingerprints.of(CheckType.TRANSITION_PLACE_BEFORE_REVEAL, sceneKey, ref.key, points.joinToString()),
        )
    }

    fun placesBeforeReveal(): List<PlaceBeforeReveal> = times.liveScenes.mapNotNull { placeBeforeReveal(it.key) }

    /** Reveal points of the live pins of a place, or of a region; null when it has none on a live map. */
    private fun revealPoints(ref: PlaceRef): List<PointRef?>? = when (ref.kind.known) {
        PlaceRefKind.PLACE -> doc.maps.values
            .filter { doc.liveness.isMapLive(it.key) }
            .flatMap { m -> m.pins.filter { it.placeKey == ref.key } }
            .map { it.revealPoint }
            .takeIf { it.isNotEmpty() }
        PlaceRefKind.REGION -> doc.index.region(ref.key)?.takeIf { doc.liveness.isMapLive(it.first.key) }?.let { listOf(it.second.revealPoint) }
        null -> null
    }

    private fun samePlace(a: PlaceRef?, b: PlaceRef?): Boolean = if (a == null) b == null else a.sameAs(b)

    private fun dayText(day: IntSpan): String =
        if (day.first == day.last) "${day.first}일째" else "약 ${Span(day.first, day.last).midpoint}일째"

    /** `06:10` for a point, the band word for a band range, else `HH:mm–HH:mm`. */
    private fun todText(t: IntSpan): String {
        if (t.first == t.last) return clockAtMinutesAfterDayStart(t.first).toString()
        if (t.first == 0 && t.last == DAY_MINUTES) return "하루"
        Band.entries.firstOrNull { bands.range(it) == Span(t.first, t.last) }?.let { return it.word }
        return "${clockAtMinutesAfterDayStart(t.first)}–${clockAtMinutesAfterDayStart(t.last)}"
    }
}
