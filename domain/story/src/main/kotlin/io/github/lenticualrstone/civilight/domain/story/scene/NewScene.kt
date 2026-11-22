package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.sameTimeAs
import io.github.lenticualrstone.civilight.domain.story.units.Particles
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Pre-filled values of `다음 씬 만들기` / `앞에 새 씬` (STO-05, R22 §4). Every field may be emptied
 * with `비우기` (then the new scene has no place / no 때, R11 §4).
 *
 * @property placeLine `장소 · 결사의 문 · 씬 3과 같음`, or null without a place.
 * @property timeLine `때 · 씬 3 기준 · 같은 때` (`1-3 씬 2 기준` when the base is not the scene before).
 */
data class NewScenePrefill(
    val place: PlaceRef?,
    val time: SceneTime,
    val placeLine: String?,
    val timeLine: String?,
    val transitionMode: TransitionMode,
)

/** Creating scenes (STO-05; the sheets are SCR-49 / SCR-53). */
object NewScene {
    /**
     * Prefill for a scene created right after [previousSceneKey]: its place, and `<P> 기준 · 같은 때`
     * where P is that scene, or (when it has no 때 or is 회상) the nearest earlier timed scene.
     */
    fun prefillAfter(doc: ProjectDoc, previousSceneKey: Key): NewScenePrefill {
        val times = SceneTimes(doc)
        val codes = UnitCodes(doc)
        val previous = requireNotNull(doc.index.unit(previousSceneKey)) { "unknown scene $previousSceneKey" }
        val base = if (times.timed(previousSceneKey) != null) previous else times.previousTimed(previousSceneKey)
        val placeName = PlaceNames(doc).name(previous.place)
        val n = codes.ordinal(previous)
        return NewScenePrefill(
            place = previous.place,
            time = base?.let { sameTimeAs(times, it.key) } ?: SceneTime.None,
            placeLine = placeName?.let { "장소 · $it · ${codes.sceneLabel(previous).substringBeforeLast(' ')} ${Particles.gwa(n)} 같음" },
            timeLine = base?.let {
                val ref = if (it.key == previousSceneKey) codes.sceneLabel(it) else codes.sceneRef(it)
                "때 · $ref 기준 · 같은 때"
            },
            transitionMode = defaultTransition(doc),
        )
    }

    /**
     * Prefill for `앞에 새 씬` before [nextSceneKey] (`ED`, STO-05): as [prefillAfter] from the scene
     * before it; nothing when it is the first scene of the story.
     */
    fun prefillBefore(doc: ProjectDoc, nextSceneKey: Key): NewScenePrefill {
        val previous = SceneTimes(doc).previousScene(nextSceneKey)
        return if (previous != null) prefillAfter(doc, previous.key) else
            NewScenePrefill(null, SceneTime.None, null, null, defaultTransition(doc))
    }

    /** The project's transition default for new scenes (factory `없음`). */
    fun defaultTransition(doc: ProjectDoc): TransitionMode = doc.project.script.transitionDefault.knownOr(TransitionMode.NONE)

    /**
     * Inserts a new scene under [parentKey] directly after [afterSceneKey] (null = first). No other
     * scene's 때 changes (later scenes keep their base). Returns the new doc and the new key.
     */
    fun create(
        doc: ProjectDoc,
        parentKey: Key,
        afterSceneKey: Key?,
        ctx: EditContext,
        title: String? = null,
        place: PlaceRef? = null,
        time: SceneTime = SceneTime.None,
        transitionMode: TransitionMode = defaultTransition(doc),
    ): Pair<ProjectDoc, Key> {
        val key = ctx.newKey()
        val unit = StoryUnit(
            key = key,
            parentKey = parentKey,
            level = doc.project.structure.sceneLevel,
            order = orderAfter(doc, parentKey, afterSceneKey),
            title = title,
            place = place,
            time = time,
            transitionMode = OpenEnum.of(transitionMode),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return doc.copy(story = doc.story.copy(units = doc.story.units + unit)) to key
    }

    /** An order key under [parentKey] right after [afterKey] (null = before every child). */
    internal fun orderAfter(doc: ProjectDoc, parentKey: Key?, afterKey: Key?, excluding: Key? = null): String {
        val siblings = doc.index.children(parentKey).filter { it.key != excluding }
        if (afterKey == null) return OrderKeys.before(siblings.firstOrNull()?.order)
        val i = siblings.indexOfFirst { it.key == afterKey }
        require(i >= 0) { "$afterKey is not a child of $parentKey" }
        return OrderKeys.between(siblings[i].order, siblings.getOrNull(i + 1)?.order)
    }
}
