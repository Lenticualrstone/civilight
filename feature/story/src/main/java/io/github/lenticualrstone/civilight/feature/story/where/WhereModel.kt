package io.github.lenticualrstone.civilight.feature.story.where

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkPlace
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.scene.SceneWhere
import io.github.lenticualrstone.civilight.domain.story.scene.SceneWhereSheet
import io.github.lenticualrstone.civilight.domain.story.time.BaseRules
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.time.TimeChange
import io.github.lenticualrstone.civilight.domain.story.time.TimeChangePreview
import io.github.lenticualrstone.civilight.domain.story.time.shiftedByDays
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.domain.story.travel.TravelCheck
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.domain.story.travel.TravelWarning
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.FlashbackRelation
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Everything the `씬 · 장소와 때` sheet draws for one live scene (STO-20). */
data class WhereUi(
    val sheet: SceneWhereSheet,
    val controls: WhenControls,
    /** The scene's place is a region (`장소로 바꾸기` shown instead of `지역으로 바꾸기`). */
    val isRegion: Boolean,
    val place: PlaceRef?,
    val readOnly: Boolean,
    /** Hidden-place mask and the deleted-place buttons (STO-11). */
    val placeState: PlaceState = PlaceState(masked = false, restorable = false, keepable = false),
    /** The 등장 row (STO-26). */
    val cast: CastRow = CastRow(emptyList(), emptyList()),
    /** The 회상 form, for a flashback scene (STO-24). */
    val flashback: FlashbackUi? = null,
    /** The result line, with the world-calendar date when the calendar is on (STO-18). */
    val resultLine: String? = sheet.resultLine,
)

/** What a 때 change does: stored at once, or the cascade preview first (STO-23). */
sealed interface TimeStep {
    data object NoChange : TimeStep

    data class Direct(val time: SceneTime) : TimeStep

    data class Preview(val time: SceneTime, val preview: TimeChangePreview) : TimeStep
}

/**
 * The `장소와 때` sheet as plain data and pure edits (STO-20 … STO-23, STO-27 row). Every edit is
 * recomputed on the doc it is applied to, and is a no-op when the scene is no longer a live scene
 * (a sheet restored after process death, or left open over a changed project).
 */
object WhereModel {
    fun isLiveScene(doc: ProjectDoc, sceneKey: Key): Boolean {
        val unit = doc.index.unit(sceneKey) ?: return false
        return unit.level == doc.project.structure.sceneLevel && doc.liveness.isUnitLive(sceneKey)
    }

    /** [locked]: the project's hidden items are locked (`숨긴 항목`, `숨긴 인물`, R19 §6). */
    fun build(doc: ProjectDoc, sceneKey: Key, readOnly: Boolean, travel: TravelSettings = TravelSettings(), locked: Boolean = false): WhereUi? {
        if (!isLiveScene(doc, sceneKey)) return null
        val scene = doc.index.unit(sceneKey)!!
        val sheet = SceneWhere.sheet(doc, sceneKey, travel)
        return WhereUi(
            sheet = sheet,
            controls = WhenEdit.controls(doc, sceneKey),
            isRegion = scene.place?.kind?.known == PlaceRefKind.REGION,
            place = scene.place,
            readOnly = readOnly,
            placeState = PlaceFix.state(doc, sceneKey, locked),
            cast = CastModel.row(doc, sceneKey, locked),
            flashback = FlashbackModel.ui(doc, sceneKey),
            resultLine = CalendarLabels.resultLine(doc, sceneKey, sheet.resultLine),
        )
    }

    /** A control change: no change, store directly, or show the cascade preview first. */
    fun step(doc: ProjectDoc, sceneKey: Key, newTime: SceneTime?): TimeStep {
        if (newTime == null || !isLiveScene(doc, sceneKey)) return TimeStep.NoChange
        if (doc.index.unit(sceneKey)!!.time == newTime) return TimeStep.NoChange
        val preview = TimeChange.preview(doc, sceneKey, newTime)
        return if (preview.needsPreview) TimeStep.Preview(newTime, preview) else TimeStep.Direct(newTime)
    }

    /** Stores [newTime] (`뒤도 같이` unless the author picked `이 씬만`), recomputed on [doc]. */
    fun setTime(doc: ProjectDoc, sceneKey: Key, newTime: SceneTime, mode: CascadeMode, ctx: ActionContext): ProjectDoc =
        if (isLiveScene(doc, sceneKey)) TimeChange.apply(doc, sceneKey, newTime, mode, ctx.edit) else doc

    /**
     * `회상` on: a flashback outside the chain, starting on `기준 사건` · `그날` as `RTFlashback` draws it
     * (the event is picked in the form, STO-24); off: no 때. Dependents rebase (STO-10 rule).
     */
    fun flashbackTime(on: Boolean): SceneTime =
        if (on) SceneTime.Flashback(mode = FlashbackMode.ANCHOR.open(), relation = FlashbackRelation.ON.open()) else SceneTime.None

    fun setPlace(doc: ProjectDoc, sceneKey: Key, place: PlaceRef, ctx: ActionContext): ProjectDoc =
        if (isLiveScene(doc, sceneKey)) BulkPlace.applySame(doc, listOf(sceneKey), place, ctx.edit) else doc

    /** `기준을 직전 씬으로` (STO-21): base = the nearest earlier timed scene, absolute kept. */
    fun rebaseToPrevious(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext): ProjectDoc =
        if (isLiveScene(doc, sceneKey)) BaseRules.rebaseToPrevious(doc, sceneKey, ctx.edit) else doc

    /** `전환 문구 | 자동 · 직접 · 없음` (STO-27 row, SCR-48 storage); `자동` needs a place. */
    fun setTransitionMode(doc: ProjectDoc, sceneKey: Key, mode: TransitionMode, ctx: ActionContext): ProjectDoc {
        if (!isLiveScene(doc, sceneKey)) return doc
        if (mode == TransitionMode.AUTO && !Transitions(doc).autoEnabled(sceneKey)) return doc
        return Transitions.setMode(doc, sceneKey, mode, ctx.edit)
    }

    /** The `직접` text (plain runs; the direct text is never parsed). */
    fun writeDirect(doc: ProjectDoc, sceneKey: Key, text: String, ctx: ActionContext): ProjectDoc {
        if (!isLiveScene(doc, sceneKey)) return doc
        if (Transitions(doc).directText(sceneKey).orEmpty() == text) return doc
        return Transitions.writeDirect(doc, sceneKey, if (text.isEmpty()) emptyList() else listOf(TextRun.Plain(text)), ctx.edit)
    }

    /** The open travel `▲` on the same pair in [doc], or null once it no longer fires (or is already accepted). */
    fun currentWarning(doc: ProjectDoc, like: TravelWarning, travel: TravelSettings = TravelSettings()): TravelWarning? =
        TravelCheck(doc, travel).warningFor(like.characterKey, like.fromSceneKey, like.toSceneKey)
            ?.takeIf { it.verdict == like.verdict && !Acks.isAcknowledged(doc, it) }

    /** Travel `맞음` (STO-30, STO-41): accepted with the fingerprint of the pair as it is now. */
    fun acknowledgeTravel(doc: ProjectDoc, like: TravelWarning, ctx: ActionContext): ProjectDoc {
        val w = currentWarning(doc, like) ?: return doc
        return Acks.acknowledge(doc, w, ctx.edit)
    }

    /** Travel mode chip (`말 · 약 7시간 40분`): stores the pair's override at once (STO-30). */
    fun applyModeChip(doc: ProjectDoc, like: TravelWarning, modeKey: Key, ctx: ActionContext): ProjectDoc {
        val w = currentWarning(doc, like) ?: return doc
        val chip = w.modeChips.firstOrNull { it.modeKey == modeKey } ?: return doc
        return TravelCheck.applyModeChip(doc, w, chip, ctx.edit)
    }

    /** Travel time chip (`+2일로`): the later scene's 때 delayed by the chip's days (STO-30 → STO-23). */
    fun delayedTime(doc: ProjectDoc, sceneKey: Key, days: Int): SceneTime? {
        if (!isLiveScene(doc, sceneKey)) return null
        val time = doc.index.unit(sceneKey)!!.time
        val shifted = time.shiftedByDays(days)
        return shifted.takeIf { it != time }
    }

    /** A pending 때 kept as saveable text while the cascade preview is shown. */
    fun encode(time: SceneTime): String = CivilightJson.json.encodeToString(SceneTime.serializer(), time)

    fun decode(text: String?): SceneTime? =
        text?.let { runCatching { CivilightJson.json.decodeFromString(SceneTime.serializer(), it) }.getOrNull() }
}
