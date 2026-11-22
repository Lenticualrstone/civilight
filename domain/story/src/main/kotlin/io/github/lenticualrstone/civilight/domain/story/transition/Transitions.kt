package io.github.lenticualrstone.civilight.domain.story.transition

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.time.ResolvedTime
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.withUnits
import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.TransitionTemplate
import io.github.lenticualrstone.civilight.model.story.TransitionSnapshot
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText

/**
 * The `전환 문구 | 자동 · 직접 · 없음` row of the `장소와 때` sheet (STO-27).
 *
 * @property autoEnabled `자동` can be chosen only when the scene has a place or region.
 * @property preview the text the game shows in the current mode (null for `없음`).
 */
data class TransitionRow(val mode: TransitionMode, val autoEnabled: Boolean, val preview: String?, val directText: String?)

/**
 * 전환 문구 (STO-27, R22 §2, SCR-48): automatic text, the direct text and its snapshot, mode
 * switching. The direct text is never parsed; switching away from `직접` keeps it.
 */
class Transitions(val doc: ProjectDoc, val times: SceneTimes = SceneTimes(doc)) {
    private val places = PlaceNames(doc)

    val template: TransitionTemplate get() = doc.project.script.transitionTemplate.knownOr(TransitionTemplate.PLACE_TIME)

    fun mode(sceneKey: Key): TransitionMode = doc.index.unit(sceneKey)?.transitionModeOrNone ?: TransitionMode.NONE

    /** `자동` is selectable only for a scene with a place or region. */
    fun autoEnabled(sceneKey: Key): Boolean = doc.index.unit(sceneKey)?.place?.let(places::name) != null

    /**
     * Automatic text: `결사의 문 · 06:10` (place + clock), `결사의 문 · 아침` (band only), place only
     * for an approximate scene or with template `장소만`, `하층 구역 · 1195` for a 회상 with neither
     * clock nor band (`[PROPOSED]`). Null without a place. Ordinals and `N일째` never appear.
     * `대략` is the scene's own precision (an exact link after a `대략` one still shows its clock).
     */
    fun autoText(sceneKey: Key, template: TransitionTemplate = this.template): String? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val place = places.name(scene.place) ?: return null
        if (template == TransitionTemplate.PLACE_ONLY) return place
        val time = when (val r = times.resolve(sceneKey)) {
            is ResolvedTime.Timed -> when {
                r.ownApprox -> null
                r.clock != null -> r.clock.toString()
                else -> r.storedBand?.word
            }
            is ResolvedTime.Flashback -> r.clock?.toString() ?: r.band?.word ?: r.year?.toString()
            ResolvedTime.None -> null
        }
        return listOfNotNull(place, time).joinToString(" · ")
    }

    /** The stored direct text (kept while the mode is auto or none), or null. */
    fun directText(sceneKey: Key): String? = doc.index.unit(sceneKey)?.transitionRuns?.plainText?.takeIf { it.isNotEmpty() }

    /** What the game shows: auto text, direct text, or null for `없음`. */
    fun shownText(sceneKey: Key): String? = when (mode(sceneKey)) {
        TransitionMode.AUTO -> autoText(sceneKey)
        TransitionMode.DIRECT -> directText(sceneKey)
        TransitionMode.NONE -> null
    }

    /** Second line of the scene head: `전환 문구 · 결사의 문 앞 · 06:10 · 직접`, `전환 문구 · 없음`. */
    fun headLine(sceneKey: Key): String {
        val text = shownText(sceneKey) ?: return "전환 문구 · 없음"
        val word = if (mode(sceneKey) == TransitionMode.AUTO) "자동" else "직접"
        return "전환 문구 · $text · $word"
    }

    /** Scene-head ⋯ value: `직접 · 결사의 문 앞 · 06:10`, `자동 · 결사의 문 · 06:10`, `없음`. */
    fun menuValue(sceneKey: Key): String {
        val text = shownText(sceneKey) ?: return "없음"
        val word = if (mode(sceneKey) == TransitionMode.AUTO) "자동" else "직접"
        return "$word · $text"
    }

    fun row(sceneKey: Key): TransitionRow = TransitionRow(mode(sceneKey), autoEnabled(sceneKey), shownText(sceneKey), directText(sceneKey))

    /** The place and resolved time the direct text is checked against (`transition.stale`). */
    fun snapshot(sceneKey: Key): TransitionSnapshot {
        val scene = doc.index.unit(sceneKey)
        val t = times.timed(sceneKey)
        return TransitionSnapshot(
            place = scene?.place,
            dayRange = t?.dayRange?.let { IntSpan(it.min, it.max) },
            tRange = t?.t?.let { IntSpan(it.min, it.max) },
        )
    }

    companion object {
        /**
         * Switches the mode. The direct runs are kept (switching back to `직접` restores them,
         * TRN-05). `자동` without a place is refused (the doc is returned unchanged).
         */
        fun setMode(doc: ProjectDoc, sceneKey: Key, mode: TransitionMode, ctx: EditContext? = null): ProjectDoc {
            val scene = doc.index.unit(sceneKey) ?: return doc
            if (mode == TransitionMode.AUTO && !Transitions(doc).autoEnabled(sceneKey)) return doc
            return doc.withUnits(mapOf(sceneKey to scene.copy(transitionMode = mode.open())), ctx)
        }

        /** Saves a direct text (mode `직접`) with a fresh snapshot of place and resolved time. */
        fun writeDirect(doc: ProjectDoc, sceneKey: Key, runs: List<TextRun>, ctx: EditContext? = null): ProjectDoc {
            val scene = doc.index.unit(sceneKey) ?: return doc
            val snapshot = Transitions(doc).snapshot(sceneKey)
            return doc.withUnits(
                mapOf(sceneKey to scene.copy(transitionMode = TransitionMode.DIRECT.open(), transitionRuns = runs, transitionSnapshot = snapshot)),
                ctx,
            )
        }

        /** `자동으로` on a stale item: mode `자동`, the direct text stays stored. */
        fun toAuto(doc: ProjectDoc, sceneKey: Key, ctx: EditContext? = null): ProjectDoc = setMode(doc, sceneKey, TransitionMode.AUTO, ctx)
    }
}
