package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.TransitionTemplate
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.story.TransitionSnapshot
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText

/**
 * Derived scene text (SCR-02, SCR-46, SCR-48): head line, transition text and line, transition ID,
 * header position line. Resolved 때 comes from a [SceneTimeSource].
 */
object SceneText {
    /** Name of a scene's place or region; null when the scene has none. */
    fun placeName(doc: ProjectDoc, place: PlaceRef?): String? = when (place?.kind?.known) {
        PlaceRefKind.PLACE -> doc.index.place(place.key)?.name
        PlaceRefKind.REGION -> doc.index.region(place.key)?.second?.name
        null -> null
    }

    /** `2일째 새벽` (short form); `약 13일째` when uncertain; null without an episode day. */
    fun shortDay(time: ResolvedSceneTime?): String? {
        if (time == null || time.flashback) return null
        val day = time.episodeDay?.let { if (time.episodeDayApprox) "약 ${it}일째" else "${it}일째" } ?: return null
        return listOfNotNull(day, time.band?.let(Labels::bandWord)).joinToString(" ")
    }

    /** Head form: short form + the exact clock (`2일째 아침 06:10`), or `회상 · 1195`, or `때 없음`. */
    fun headTime(time: ResolvedSceneTime?): String {
        if (time == null) return "때 없음"
        if (time.flashback) return listOfNotNull("회상", time.flashbackYear?.toString()).joinToString(" · ")
        val short = shortDay(time) ?: return time.clock?.toString() ?: "때 없음"
        val clock = time.clock?.takeIf { !time.approx }
        return if (clock != null) "$short $clock" else short
    }

    /**
     * Scene head (SCR-46): `씬 {ordinal} · {place, region or 장소 없음} · {time part}`; a flashback
     * reads `씬 2 · 회상 · 하층 구역 · 1195` (rule 22 order, Q-22).
     */
    fun headText(doc: ProjectDoc, sceneKey: Key, time: ResolvedSceneTime?): String {
        val scene = doc.index.unit(sceneKey)
        val ordinal = Labels.sceneOrdinal(doc, sceneKey) ?: "?"
        val place = placeName(doc, scene?.place) ?: "장소 없음"
        if (time?.flashback == true) {
            return listOfNotNull("씬 $ordinal", "회상", place, time.flashbackYear?.toString()).joinToString(" · ")
        }
        return "씬 $ordinal · $place · ${headTime(time)}"
    }

    /**
     * `자동` transition text (SCR-48): template `장소 · 시각` gives `P · 06:10` with an exact clock,
     * `P · 아침` with a band, `P` for an approximate or missing time; a flashback uses its own
     * place and year; `장소만` gives `P`. Null when the scene has no place (`자동` cannot be chosen).
     */
    fun autoTransitionText(doc: ProjectDoc, scene: StoryUnit, time: ResolvedSceneTime?): String? {
        val place = placeName(doc, scene.place) ?: return null
        if (doc.project.script.transitionTemplate.known == TransitionTemplate.PLACE_ONLY || time == null) return place
        if (time.flashback) return time.flashbackYear?.let { "$place · $it" } ?: place
        if (time.approx) return place
        time.clock?.let { return "$place · $it" }
        time.band?.let { return "$place · ${Labels.bandWord(it)}" }
        return place
    }

    /** Whether `자동` can be chosen for the scene. */
    fun canUseAuto(scene: StoryUnit): Boolean = scene.place != null

    /** The transition text the game shows, or null for `없음` (and for `자동` without a place). */
    fun transitionText(doc: ProjectDoc, scene: StoryUnit, time: ResolvedSceneTime?): String? = when (scene.transitionModeOrNone) {
        TransitionMode.NONE -> null
        TransitionMode.AUTO -> autoTransitionText(doc, scene, time)
        TransitionMode.DIRECT -> scene.transitionRuns?.plainText?.takeIf { it.isNotBlank() }
    }

    /** Second head line: `전환 문구 · {text} · 자동|직접` or `전환 문구 · 없음`. */
    fun transitionLine(doc: ProjectDoc, scene: StoryUnit, time: ResolvedSceneTime?): String {
        val text = transitionText(doc, scene, time) ?: return "전환 문구 · 없음"
        val mode = if (scene.transitionModeOrNone == TransitionMode.AUTO) "자동" else "직접"
        return "전환 문구 · $text · $mode"
    }

    /** `시간` tab state word: `전환 문구 없음 · 전환 문구 자동 · 전환 문구 직접`. */
    fun transitionState(scene: StoryUnit): String = when (scene.transitionModeOrNone) {
        TransitionMode.NONE -> "전환 문구 없음"
        TransitionMode.AUTO -> "전환 문구 자동"
        TransitionMode.DIRECT -> "전환 문구 직접"
    }

    /** Permanent transition ID derived from the scene key (Q-25 default `tr-{sceneKey}`). */
    fun transitionId(sceneKey: Key): String = "tr-$sceneKey"

    /**
     * Sets the mode. The last direct text is kept while the mode is `자동`/`없음` and comes back with
     * `직접`. `자동` is refused (null) for a scene without a place.
     */
    fun setTransitionMode(doc: ProjectDoc, sceneKey: Key, mode: TransitionMode, ctx: EditContext): ProjectDoc? {
        val scene = doc.index.unit(sceneKey) ?: return null
        if (mode == TransitionMode.AUTO && !canUseAuto(scene)) return null
        return doc.updateUnit(sceneKey) { it.copy(transitionMode = mode.open(), updatedAt = ctx.now, updatedBy = ctx.device) }
    }

    /** Snapshot of the scene's place and resolved time (stored with direct text, SCR-48). */
    fun snapshotOf(scene: StoryUnit, time: ResolvedSceneTime?): TransitionSnapshot =
        TransitionSnapshot(place = scene.place, dayRange = time?.storyDay, tRange = time?.tRange)

    /** Saves direct text (`직접`) with a fresh snapshot. */
    fun setDirectText(doc: ProjectDoc, sceneKey: Key, runs: List<TextRun>, time: ResolvedSceneTime?, ctx: EditContext): ProjectDoc {
        val scene = doc.index.unit(sceneKey) ?: return doc
        return doc.updateUnit(sceneKey) {
            it.copy(
                transitionMode = TransitionMode.DIRECT.open(), transitionRuns = Runs.normalize(runs),
                transitionSnapshot = snapshotOf(scene, time), updatedAt = ctx.now, updatedBy = ctx.device,
            )
        }
    }

    /** `직접 쓰기` (SCR-62): switches to `직접` prefilled with the automatic text (`[PROPOSED]`). */
    fun directFromAuto(doc: ProjectDoc, sceneKey: Key, time: ResolvedSceneTime?, ctx: EditContext): ProjectDoc {
        val scene = doc.index.unit(sceneKey) ?: return doc
        val text = autoTransitionText(doc, scene, time) ?: return doc
        return setDirectText(doc, sceneKey, listOf(TextRun.Plain(text)), time, ctx)
    }

    // --- Header (SCR-02) ---

    /** Header position line `1-7 · 씬 3 · 2일째 새벽` (ends after the ordinal without 때). */
    fun positionLine(doc: ProjectDoc, sceneKey: Key, time: ResolvedSceneTime?): String {
        val parts = mutableListOf<String>()
        Labels.episodeCodeOfScene(doc, sceneKey).takeIf { it.isNotEmpty() }?.let(parts::add)
        parts += "씬 ${Labels.sceneOrdinal(doc, sceneKey) ?: "?"}"
        if (time?.flashback == true) parts += listOfNotNull("회상", time.flashbackYear?.toString()).joinToString(" · ")
        else shortDay(time)?.let(parts::add)
        return parts.joinToString(" · ")
    }

    /** While editing: `1-7 · 씬 3 · 저장됨` (`저장 중` while a write is pending). */
    fun editingLine(doc: ProjectDoc, sceneKey: Key, saved: Boolean = true): String =
        listOfNotNull(
            Labels.episodeCodeOfScene(doc, sceneKey).takeIf { it.isNotEmpty() },
            "씬 ${Labels.sceneOrdinal(doc, sceneKey) ?: "?"}",
            if (saved) "저장됨" else "저장 중",
        ).joinToString(" · ")

    /**
     * Which scene the header describes (rule 22 §6): the scene of the caret or selected block, else
     * the scene of the block directly under the top bar.
     */
    fun headerSceneKey(doc: ProjectDoc, caretOrSelectedBlockKey: Key?, blockUnderTopBarKey: Key?): Key? =
        (caretOrSelectedBlockKey ?: blockUnderTopBarKey)?.let { doc.index.block(it)?.sceneKey }

    /** Position label `이 씬 · 1-7`. */
    fun positionLabel(doc: ProjectDoc, sceneKey: Key): String = "이 씬 · ${Labels.episodeCodeOfScene(doc, sceneKey)}"

    /** `렌즈를 이 씬으로 맞추기`: the lens point "end of this scene". */
    fun lensForScene(sceneKey: Key): LensPoint = LensPoint.UnitEnd(sceneKey)

    /** Toast `렌즈 · 1-7 씬 3` (`[PROPOSED]`). */
    fun lensToast(doc: ProjectDoc, sceneKey: Key): String = "렌즈 · ${Labels.sceneLocation(doc, sceneKey)}"

    /** Scene-head ⋯ sheet title `씬 4 · 1-7`. */
    fun menuTitle(doc: ProjectDoc, sceneKey: Key): String =
        "씬 ${Labels.sceneOrdinal(doc, sceneKey) ?: "?"} · ${Labels.episodeCodeOfScene(doc, sceneKey)}"
}
