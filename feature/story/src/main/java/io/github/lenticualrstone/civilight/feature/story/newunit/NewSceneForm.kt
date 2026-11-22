package io.github.lenticualrstone.civilight.feature.story.newunit

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.story.scene.NewScene
import io.github.lenticualrstone.civilight.domain.story.scene.NewScenePrefill
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
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
    /** Null = the top level of a 1-level structure (`SceneActions.create(parentKey = null)`). */
    val parentKey: Key?,
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
    /** `같은 날 · +1일 · +2일` picked in the sheet (days after the base); null = the pre-filled 때. */
    val dayChip: Int? = null,
    /** A band picked in the sheet (`Band` name); null = the pre-filled band and clock. */
    val band: String? = null,
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
     * moved or deleted). In a 1-level structure the sheet has no parent and scenes are top-level.
     * - `afterKey`: after that scene, pre-filled from it (`SceneActions.prefillAfter`).
     * - `beforeKey`: before that scene, pre-filled from the scene before it (`prefillBefore`, STO-05 `ED`).
     * - neither: after the episode's last scene, pre-filled from it; an empty episode starts with
     *   nothing pre-filled (`ED`).
     */
    fun setup(doc: ProjectDoc, sheet: NewUnitSheet): NewSceneSetup? {
        val structure = doc.project.structure
        val topLevel = structure.levels.size == 1
        val parent = if (topLevel) null else sheet.parentKey?.let(doc.index::unit) ?: return null
        if (topLevel && sheet.parentKey != null) return null
        if (parent != null && (!doc.liveness.isUnitLive(parent.key) || parent.level != structure.sceneLevel - 1)) return null
        val codes = UnitCodes(doc)
        val scenes = doc.index.children(parent?.key).filter { it.level == structure.sceneLevel && doc.liveness.isUnitLive(it.key) }
        val parentCode = parent?.let(codes::unitLabel)
        fun place(scene: io.github.lenticualrstone.civilight.model.story.StoryUnit, word: String) =
            listOfNotNull(parentCode, "${codes.sceneLabel(scene)} $word").joinToString(" · ")
        fun isChild(key: Key) = scenes.any { it.key == key }
        return when {
            sheet.beforeKey != null -> {
                val before = sheet.beforeKey!!.takeIf(::isChild) ?: return null
                val index = scenes.indexOfFirst { it.key == before }
                NewSceneSetup(
                    parentKey = parent?.key,
                    afterSceneKey = scenes.getOrNull(index - 1)?.key,
                    title = NewUnitCopy.TITLE_BEFORE,
                    subtitle = place(scenes[index], NewUnitCopy.BEFORE),
                    prefill = SceneActions.prefillBefore(doc, before),
                )
            }
            else -> {
                val after = sheet.afterKey?.let { it.takeIf(::isChild) ?: return null } ?: scenes.lastOrNull()?.key
                val afterScene = after?.let { k -> scenes.first { it.key == k } }
                NewSceneSetup(
                    parentKey = parent?.key,
                    afterSceneKey = after,
                    title = NewUnitCopy.TITLE_AFTER,
                    subtitle = afterScene?.let { place(it, NewUnitCopy.AFTER) } ?: parentCode.orEmpty(),
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

    /**
     * The base the sheet's 때 chips count from (R11 §4 `기준`): the scene the new one follows when it is in
     * the time chain, else the nearest earlier timed scene; null when there is none (the chips are not
     * offered; a first scene gets its 때 in the `장소와 때` sheet, `직접`).
     */
    fun baseKey(doc: ProjectDoc, setup: NewSceneSetup): Key? {
        val after = setup.afterSceneKey ?: return null
        val times = SceneTimes(doc)
        return if (times.timed(after) != null) after else times.previousTimed(after)?.key
    }

    /**
     * The 때 the scene is created with: cleared, the pre-filled one, or `<base> 기준 · +n일` with the picked
     * band (a picked band drops the pre-filled clock; the precision stays `정확`, as the sheet's chips).
     */
    fun time(doc: ProjectDoc, setup: NewSceneSetup, form: NewSceneForm): SceneTime {
        if (form.timeCleared) return SceneTime.None
        if (form.dayChip == null && form.band == null) return setup.prefill.time
        val base = baseKey(doc, setup) ?: return setup.prefill.time
        val pre = setup.prefill.time as? SceneTime.Relative
        val days = form.dayChip ?: pre?.takeIf { it.dayMin == it.dayMax }?.dayMin ?: 0
        val band = form.band?.let { b -> Band.entries.firstOrNull { it.name == b } } ?: pre?.band?.known
        return SceneTime.Relative(baseKey = base, dayMin = days, dayMax = days, band = band?.open(), clock = if (form.band == null) pre?.clock else null)
    }

    /** The day chip shown as selected: the picked one, else the pre-filled interval when it is one of the three. */
    fun dayChipOf(doc: ProjectDoc, setup: NewSceneSetup, form: NewSceneForm): Int? =
        (time(doc, setup, form) as? SceneTime.Relative)?.takeIf { it.dayMin == it.dayMax && it.dayMin in 0..2 }?.dayMin

    /** The band chip shown as selected. */
    fun bandOf(doc: ProjectDoc, setup: NewSceneSetup, form: NewSceneForm): Band? = when (val t = time(doc, setup, form)) {
        is SceneTime.Relative -> t.band?.known
        is SceneTime.Direct -> t.band?.known
        else -> null
    }

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
        val time = time(doc, setup, form)
        val edited = form.dayChip != null || form.band != null
        val timeLine = when {
            time == SceneTime.None -> null
            edited && time is SceneTime.Relative -> editedLine(doc, setup, time)
            else -> setup.prefill.timeLine?.removePrefix("${NewUnitCopy.FIELD_TIME} · ")
        }
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

    /** `씬 2 기준 · +1일 · 새벽` (the base, short inside the episode as the pre-filled line,, the chip word and the band word as the chips read). */
    private fun editedLine(doc: ProjectDoc, setup: NewSceneSetup, time: SceneTime.Relative): String {
        val day = when (time.dayMin) {
            0 -> NewUnitCopy.SAME_DAY
            1 -> NewUnitCopy.PLUS_ONE
            2 -> NewUnitCopy.PLUS_TWO
            else -> "+${time.dayMin}일"
        }
        val codes = UnitCodes(doc)
        val baseUnit = doc.index.unit(time.baseKey)
        val near = setup.afterSceneKey?.let(doc.index::unit)
        val base = when {
            baseUnit == null -> ""
            near != null -> codes.sceneRefRelativeTo(baseUnit, near)
            else -> codes.sceneRef(baseUnit)
        }
        return listOfNotNull("$base ${NewUnitCopy.BASE}", day, time.band?.known?.word).joinToString(" · ")
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
            time = time(doc, setup, form),
            transitionMode = mode,
        )
        val withText = if (mode == TransitionMode.DIRECT) Transitions.writeDirect(created, key, listOf(TextRun.Plain(text)), ctx.edit) else created
        return withText to key
    }
}
