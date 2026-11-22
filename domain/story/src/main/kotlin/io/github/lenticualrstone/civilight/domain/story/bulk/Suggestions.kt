package io.github.lenticualrstone.civilight.domain.story.bulk

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.text.BlockText
import io.github.lenticualrstone.civilight.domain.story.text.Sentences
import io.github.lenticualrstone.civilight.domain.story.text.WordHit
import io.github.lenticualrstone.civilight.domain.story.text.WordMatcher
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.domain.story.time.bandOfWord
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.domain.story.withUnits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.world.Place

/** Where a proposal was read from, in priority order (STO-34 `ED`). */
enum class SuggestionSource(val word: String) {
    BACKGROUND("배경 연출"),
    TRANSITION("전환 문구"),
    BODY("본문"),
}

/** The source sentence in full with the matched parts boxed (`mark.hit`). */
data class Evidence(val text: String, val hits: List<IntRange>) {
    val boxed: List<String> get() = hits.map { text.substring(it) }
}

/** A proposed place: a known place (or region), or a new place card [name] (`새 장소`). */
data class PlaceGuess(val ref: PlaceRef?, val name: String) {
    val isNew: Boolean get() = ref == null
}

/** A proposed 때 relative to the previous timed scene: day interval (a range for `며칠 뒤`) and band. */
data class TimeGuess(val days: Span, val band: Band?) {
    /** `같은 날 · 밤`, `+2일 · 새벽`, `+5~9일`. */
    val text: String
        get() {
            val day = when {
                !days.isPoint -> "+${days.min}~${days.max}일"
                days.min == 0 -> "같은 날"
                days.min > 0 -> "+${days.min}일"
                else -> "${days.min}일"
            }
            return listOfNotNull(day, band?.word).joinToString(" · ")
        }
}

/** One proposal for one scene. */
data class Suggestion(
    val sceneKey: Key,
    val source: SuggestionSource,
    val evidence: Evidence,
    val place: PlaceGuess?,
    val time: TimeGuess?,
) {
    /** `잿더미 시장 | 같은 날 · 밤`; `기록실 | 같은 날 · 밤 | · 장소 카드 없음 → 새 장소`. */
    val line: String
        get() = listOfNotNull(
            place?.name ?: "장소 없음",
            time?.text,
            if (place?.isNew == true) "· 장소 카드 없음 → 새 장소" else null,
        ).joinToString(" | ")
}

/**
 * One card of the `제안 받기` sheet: header `1-6 씬 2 · 노아의 심부름 | 배경 연출`, or for no result
 * `1-9 씬 1 · 미정 | 제안 없음` with [emptyLine].
 */
data class SuggestionCard(val sceneKey: Key, val header: String, val suggestion: Suggestion?) {
    val emptyLine: String? get() = if (suggestion == null) "배경 · 전환 문구 · 본문에서 찾지 못함 → 직접" else null
    val actions: List<String> get() = if (suggestion == null) emptyList() else listOf("넣기", "고치기", "건너뛰기")
}

/** The `제안 받기` sheet (`BulkSuggest`). */
data class SuggestSheet(val head: String, val title: String, val line: String, val cards: List<SuggestionCard>) {
    val count: Int get() = cards.count { it.suggestion != null }

    /** `제안 3개 모두 넣기`. */
    val applyAllLabel: String get() = "제안 ${count}개 모두 넣기"
}

/**
 * `제안 받기` (STO-34, R22 §6): the only place the app reads prose for place and time. Sources in
 * order: a `▸ 배경` cue whose asset is linked to a place (band = the parenthesised word of the asset
 * name), the scene's 직접 transition text (never the automatic one), the first body sentence that holds
 * a time phrase. Proposals are offered only for a scene that lacks the place or 때 they would fill.
 * Nothing changes before [apply].
 *
 * Places match by name and by their `Place.aliases` (SUG-02: 송전탑 폐허 · 송전탑).
 *
 * @param placeAliases extra match strings per place key, merged with each place's own `aliases`.
 */
class Suggestions(
    val doc: ProjectDoc,
    private val placeAliases: Map<Key, List<String>> = emptyMap(),
    private val codes: UnitCodes = UnitCodes(doc),
) {
    private val livePlaces: List<Place> = doc.world.places.filter { doc.liveness.isPlaceLive(it.key) }
    private val extraParticles = doc.project.termMatching.extraParticles
    private val placeMatcher = WordMatcher(
        livePlaces.flatMap { p ->
            (listOf(p.name) + p.aliases + placeAliases[p.key].orEmpty()).filter { it.isNotBlank() }.distinct().map { it to p }
        },
        extraParticles,
    )
    private val timeMatcher = WordMatcher(TIME_PHRASES + Band.entries.map { it.word to TimePhrase(null, it) }, extraParticles, minSyllables = 1)

    /** A day phrase (`이틀 뒤` → +2) or a band word. */
    private data class TimePhrase(val days: Span?, val band: Band?)

    /** The proposal for a scene, or null. */
    fun suggestionFor(sceneKey: Key): Suggestion? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val missingPlace = scene.place == null
        val missingTime = scene.time == SceneTime.None || scene.time is SceneTime.Unknown
        if (!missingPlace && !missingTime) return null
        val found = fromBackground(sceneKey) ?: fromTransition(sceneKey) ?: fromBody(sceneKey) ?: return null
        val useful = (missingPlace && found.place != null) || (missingTime && found.time != null)
        return found.takeIf { useful }
    }

    fun hasSuggestion(sceneKey: Key): Boolean = suggestionFor(sceneKey) != null

    fun card(sceneKey: Key): SuggestionCard? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val s = suggestionFor(sceneKey)
        val head = "${codes.sceneRef(scene)} · ${scene.title ?: "미정"}"
        return SuggestionCard(sceneKey, "$head | ${s?.source?.word ?: "제안 없음"}", s)
    }

    /** The sheet for the selected scenes (manuscript order). */
    fun sheet(selected: List<Key>): SuggestSheet {
        val ordered = doc.manuscript.liveScenes.map { it.key }.filter { it in selected }
        return SuggestSheet(
            head = "장소 · 때 한꺼번에 · 고른 씬 ${ordered.size}개",
            title = "제안 받기",
            line = "원고의 배경 연출 · 전환 문구 · 본문에서 읽음 · 넣기 전엔 바뀌지 않음",
            cards = ordered.mapNotNull(::card),
        )
    }

    // --- Sources ----------------------------------------------------------------------------------

    private fun fromBackground(sceneKey: Key): Suggestion? {
        for (block in doc.manuscript.liveBlocksOf(sceneKey)) {
            if (block.kind.known != BlockKind.DIRECTION) continue
            for (cue in block.cues) {
                if (cue.kind.known != CueKind.BACKGROUND) continue
                val asset = cue.assetKey?.let(doc.index::scriptAsset) ?: continue
                val placeKey = asset.placeKey?.takeIf(doc.liveness::isPlaceLive) ?: continue
                val place = doc.index.place(placeKey) ?: continue
                val text = "▸ 배경 ${asset.name}"
                val hits = mutableListOf<IntRange>()
                val nameAt = text.indexOf(place.name)
                if (nameAt >= 0) hits += nameAt until nameAt + place.name.length
                val paren = Regex("""\(([^()]*)\)""").find(text)
                val band = paren?.groupValues?.get(1)?.trim()?.let(::bandOfWord)
                if (band != null) hits += paren.groups[1]!!.range
                return Suggestion(
                    sceneKey, SuggestionSource.BACKGROUND, Evidence(text, hits.sortedBy { it.first }),
                    PlaceGuess(PlaceRef(PlaceRefKind.PLACE, placeKey), place.name),
                    band?.let { TimeGuess(Span.point(0), it) },
                )
            }
        }
        return null
    }

    private fun fromTransition(sceneKey: Key): Suggestion? {
        val scene = doc.index.unit(sceneKey) ?: return null
        if (scene.transitionModeOrNone != TransitionMode.DIRECT) return null
        val text = scene.transitionRuns?.plainText?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val places = placeMatcher.find(text)
        val times = timeMatcher.find(text)
        val hits = mutableListOf<IntRange>()
        val place: PlaceGuess? = places.firstOrNull()?.let { h ->
            hits += h.range
            PlaceGuess(PlaceRef(PlaceRefKind.PLACE, h.value.key), h.value.name)
        } ?: run {
            // An unknown place-like word: the first fragment before ` · `, when it is not a time phrase.
            val fragment = text.substringBefore(" · ").trim()
            val isTime = times.any { it.range.first == text.indexOf(fragment) }
            if (fragment.isEmpty() || isTime || " · " !in text) null else {
                val at = text.indexOf(fragment)
                hits += at until at + fragment.length
                PlaceGuess(null, fragment)
            }
        }
        val time = timeGuess(times)?.also { hits += times.map { it.range } }
        if (place == null && time == null) return null
        return Suggestion(sceneKey, SuggestionSource.TRANSITION, Evidence(text, hits.sortedBy { it.first }), place, time)
    }

    private fun fromBody(sceneKey: Key): Suggestion? {
        for (block in doc.manuscript.liveBlocksOf(sceneKey)) {
            val text = BlockText.gameText(block) ?: continue
            for (range in Sentences.split(text)) {
                val sentence = text.substring(range)
                val times = timeMatcher.find(sentence)
                val time = timeGuess(times) ?: continue
                val places = placeMatcher.find(sentence)
                val place = places.firstOrNull()?.let { PlaceGuess(PlaceRef(PlaceRefKind.PLACE, it.value.key), it.value.name) }
                val hits = (times.map { it.range } + places.take(1).map { it.range }).sortedBy { it.first }
                return Suggestion(sceneKey, SuggestionSource.BODY, Evidence(sentence, hits), place, time)
            }
        }
        return null
    }

    /** The first day phrase and the first band word; band only → `같은 날 · <band>`. */
    private fun timeGuess(hits: List<WordHit<TimePhrase>>): TimeGuess? {
        val days = hits.firstNotNullOfOrNull { it.value.days }
        val band = hits.firstNotNullOfOrNull { it.value.band }
        if (days == null && band == null) return null
        return TimeGuess(days ?: Span.point(0), band)
    }

    companion object {
        /** `ED` lexicon, extendable (STO-34). */
        private val TIME_PHRASES: List<Pair<String, TimePhrase>> = listOf(
            "같은 날" to TimePhrase(Span.point(0), null),
            "다음 날" to TimePhrase(Span.point(1), null),
            "이튿날" to TimePhrase(Span.point(1), null),
            "이틀 뒤" to TimePhrase(Span.point(2), null),
            "사흘 뒤" to TimePhrase(Span.point(3), null),
            "며칠 뒤" to TimePhrase(Span(5, 9), null),
        )

        /**
         * `넣기`: fills the scene's empty place and empty 때 from [suggestion] (`ED`: existing values are
         * never overwritten). A `새 장소` first creates a place card with that name and no pin. The
         * time is `<nearest earlier timed scene> 기준` with [precision] (`대략` default, chip range
         * as STO-33); without an earlier timed scene it becomes `직접` from story day 1.
         */
        fun apply(doc: ProjectDoc, suggestion: Suggestion, ctx: EditContext, precision: TimePrecision = TimePrecision.APPROX): ProjectDoc {
            val scene = doc.index.unit(suggestion.sceneKey) ?: return doc
            var d = doc
            var place = scene.place
            if (place == null && suggestion.place != null) {
                place = suggestion.place.ref ?: run {
                    val key = ctx.newKey()
                    val last = d.world.places.maxOfOrNull { it.order }
                    val card = Place(key = key, order = OrderKeys.after(last), name = suggestion.place.name, updatedAt = ctx.now, updatedBy = ctx.device)
                    d = d.copy(world = d.world.copy(places = d.world.places + card))
                    PlaceRef(PlaceRefKind.PLACE, key)
                }
            }
            var time = scene.time
            if (time == SceneTime.None && suggestion.time != null) {
                time = BulkWhen.relativeTo(SceneTimes(d), suggestion.sceneKey, suggestion.time.days, suggestion.time.band, null, precision)
            }
            return d.withUnits(mapOf(scene.key to scene.copy(place = place, time = time)), ctx)
        }

        /** `제안 n개 모두 넣기`: every proposal in manuscript order, one undo step. */
        fun applyAll(doc: ProjectDoc, suggestions: List<Suggestion>, ctx: EditContext, precision: TimePrecision = TimePrecision.APPROX): ProjectDoc {
            val order = doc.manuscript.liveScenes.withIndex().associate { (i, s) -> s.key to i }
            return suggestions.sortedBy { order[it.sceneKey] ?: Int.MAX_VALUE }.fold(doc) { acc, s -> apply(acc, s, ctx, precision) }
        }
    }
}
