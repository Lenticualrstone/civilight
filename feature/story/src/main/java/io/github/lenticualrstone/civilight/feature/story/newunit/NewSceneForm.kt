package io.github.lenticualrstone.civilight.feature.story.newunit

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.story.scene.NewScene
import io.github.lenticualrstone.civilight.domain.story.scene.NewScenePrefill
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet

/**
 * Where the new scene goes and what is pre-filled (STO-05): under [parentKey] after [afterSceneKey]
 * (null = first in the episode). [title] `다음 씬 만들기` / `앞에 새 씬`, [subtitle] `1-7 · 씬 3 뒤`.
 */
data class NewSceneSetup(
    val parentKey: Key,
    val afterSceneKey: Key?,
    val title: String,
    val subtitle: String,
    val prefill: NewScenePrefill,
)

/** The 장소 field: the pre-filled value, emptied with `비우기`, or a place picked with `바꾸기 ›`. */
sealed interface PlaceChoice {
    data object Prefilled : PlaceChoice

    data object Cleared : PlaceChoice

    data class Picked(val ref: PlaceRef) : PlaceChoice
}

/** What the user set in the sheet. Every field may be skipped; nothing is created before `만들기`. */
data class NewSceneForm(
    val title: String = "",
    val place: PlaceChoice = PlaceChoice.Prefilled,
    val timeCleared: Boolean = false,
    /** Null = the pre-filled mode (the project's default for new scenes). */
    val transition: TransitionMode? = null,
    val directText: String = "",
)

/** The sheet's value lines and which buttons show. */
data class NewSceneLines(
    val title: String,
    val place: String,
    val placeClearable: Boolean,
    val time: String,
    val timeClearable: Boolean,
    val transition: TransitionMode,
    val autoEnabled: Boolean,
)

/** `다음 씬 만들기` · `앞에 새 씬` as plain data (STO-05, R22 §4, R11 §4). */
object NewSceneForms {
    /**
     * The sheet's setup, or null when it no longer fits the project (parent gone, neighbour scene
     * moved or deleted, or a 1-level structure where scenes have no parent; see Requests).
     * - `afterKey`: after that scene, pre-filled from it (`SceneActions.prefillAfter`).
     * - `beforeKey`: before that scene, pre-filled from the scene before it (`prefillBefore`, STO-05 `ED`).
     * - neither: after the episode's last scene, pre-filled from it; an empty episode starts with
     *   nothing pre-filled (`ED`).
     */
    fun setup(doc: ProjectDoc, sheet: NewUnitSheet): NewSceneSetup? {
        val structure = doc.project.structure
        val parent = sheet.parentKey?.let(doc.index::unit) ?: return null
        if (!doc.liveness.isUnitLive(parent.key) || parent.level != structure.sceneLevel - 1) return null
        val codes = UnitCodes(doc)
        val scenes = doc.index.children(parent.key).filter { it.level == structure.sceneLevel && doc.liveness.isUnitLive(it.key) }
        val parentCode = codes.unitLabel(parent)
        fun isChild(key: Key) = scenes.any { it.key == key }
        return when {
            sheet.beforeKey != null -> {
                val before = sheet.beforeKey!!.takeIf(::isChild) ?: return null
                val index = scenes.indexOfFirst { it.key == before }
                NewSceneSetup(
                    parentKey = parent.key,
                    afterSceneKey = scenes.getOrNull(index - 1)?.key,
                    title = NewUnitCopy.TITLE_BEFORE,
                    subtitle = "$parentCode · ${codes.sceneLabel(scenes[index])} ${NewUnitCopy.BEFORE}",
                    prefill = SceneActions.prefillBefore(doc, before),
                )
            }
            else -> {
                val after = sheet.afterKey?.let { it.takeIf(::isChild) ?: return null } ?: scenes.lastOrNull()?.key
                val afterScene = after?.let { k -> scenes.first { it.key == k } }
                NewSceneSetup(
                    parentKey = parent.key,
                    afterSceneKey = after,
                    title = NewUnitCopy.TITLE_AFTER,
                    subtitle = afterScene?.let { "$parentCode · ${codes.sceneLabel(it)} ${NewUnitCopy.AFTER}" } ?: parentCode,
                    prefill = after?.let { SceneActions.prefillAfter(doc, it) } ?: emptyPrefill(doc),
                )
            }
        }
    }

    private fun emptyPrefill(doc: ProjectDoc) = NewScenePrefill(null, SceneTime.None, null, null, NewScene.defaultTransition(doc))

    fun place(setup: NewSceneSetup, form: NewSceneForm): PlaceRef? = when (val p = form.place) {
        PlaceChoice.Prefilled -> setup.prefill.place
        PlaceChoice.Cleared -> null
        is PlaceChoice.Picked -> p.ref
    }

    fun time(setup: NewSceneSetup, form: NewSceneForm): SceneTime = if (form.timeCleared) SceneTime.None else setup.prefill.time

    /** `자동` needs a place with a name (SCR-48); without one the pre-filled `자동` reads `없음`. */
    fun transition(doc: ProjectDoc, setup: NewSceneSetup, form: NewSceneForm): TransitionMode {
        val wanted = form.transition ?: setup.prefill.transitionMode
        return if (wanted == TransitionMode.AUTO && !autoEnabled(doc, setup, form)) TransitionMode.NONE else wanted
    }

    fun autoEnabled(doc: ProjectDoc, setup: NewSceneSetup, form: NewSceneForm): Boolean = PlaceNames(doc).name(place(setup, form)) != null

    fun lines(doc: ProjectDoc, setup: NewSceneSetup, form: NewSceneForm): NewSceneLines {
        val place = place(setup, form)
        val placeLine = when (form.place) {
            PlaceChoice.Prefilled -> setup.prefill.placeLine?.removePrefix("${NewUnitCopy.FIELD_PLACE} · ")
            else -> PlaceNames(doc).name(place)
        }
        val time = time(setup, form)
        val timeLine = if (time == SceneTime.None) null else setup.prefill.timeLine?.removePrefix("${NewUnitCopy.FIELD_TIME} · ")
        return NewSceneLines(
            title = form.title.trim().ifEmpty { StoryCopy.UNTITLED },
            place = placeLine ?: NewUnitCopy.NO_PLACE,
            placeClearable = place != null,
            time = timeLine ?: NewUnitCopy.NO_TIME,
            timeClearable = time != SceneTime.None,
            transition = transition(doc, setup, form),
            autoEnabled = autoEnabled(doc, setup, form),
        )
    }

    /**
     * `만들기`: the scene through `SceneActions.create` (no other scene's 때 changes, R11 §4), then the
     * direct transition text when `직접` has text (`ED`: `직접` with no text is created as `없음`).
     * Recomputes the setup on [doc], so a sheet left open over a changed project creates nothing
     * (null) rather than failing.
     */
    fun create(doc: ProjectDoc, sheet: NewUnitSheet, form: NewSceneForm, ctx: ActionContext): Pair<ProjectDoc, Key>? {
        val setup = setup(doc, sheet) ?: return null
        val text = form.directText.trim()
        val mode = transition(doc, setup, form).let { if (it == TransitionMode.DIRECT && text.isEmpty()) TransitionMode.NONE else it }
        val (created, key) = SceneActions.create(
            doc = doc,
            parentKey = setup.parentKey,
            afterSceneKey = setup.afterSceneKey,
            ctx = ctx,
            title = form.title.trim().ifEmpty { null },
            place = place(setup, form),
            time = time(setup, form),
            transitionMode = mode,
        )
        val withText = if (mode == TransitionMode.DIRECT) Transitions.writeDirect(created, key, listOf(TextRun.Plain(text)), ctx.edit) else created
        return withText to key
    }
}
