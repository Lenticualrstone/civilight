package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** `/씬` routing and new scenes from the script (SCR-49, SCR-53). Nothing changes before a sheet button. */
object SceneCreate {
    sealed interface Route {
        /** `다음 씬 만들기` after [sceneKey]. */
        data class NextScene(val sceneKey: Key) : Route

        /** `여기서 씬 나누기` below [blockKey]. */
        data class SplitBelow(val blockKey: Key) : Route

        /** Only `앞에 새 씬` before [sceneKey]. */
        data class NewSceneBefore(val sceneKey: Key) : Route

        /** Read-only project: the row is dimmed with `읽기 전용`. */
        data object ReadOnly : Route {
            const val LABEL = "읽기 전용"
        }
    }

    /**
     * Where `/씬` goes. [checkedKeys] = blocks checked in select mode; [onHead] = the position is the
     * scene head (above the first block). Splitting below the last block of a scene would leave an
     * empty new scene, so that case is `다음 씬 만들기` (`ED`).
     */
    fun route(
        doc: ProjectDoc,
        sceneKey: Key,
        currentBlockKey: Key?,
        checkedKeys: Collection<Key> = emptyList(),
        readOnly: Boolean = false,
        onHead: Boolean = false,
    ): Route {
        if (readOnly) return Route.ReadOnly
        val blocks = doc.manuscript.liveBlocksOf(sceneKey)
        val checked = blocks.lastOrNull { it.key in checkedKeys.toSet() }
        val below = checked ?: blocks.firstOrNull { it.key == currentBlockKey }
        return when {
            onHead -> Route.NewSceneBefore(sceneKey)
            below == null -> Route.NextScene(sceneKey)
            below.key == blocks.last().key -> Route.NextScene(sceneKey)
            else -> Route.SplitBelow(below.key)
        }
    }

    /** The scene whose 때 a new scene after [sceneKey] copies: itself, or the nearest earlier timed non-flashback scene. */
    fun timeBaseFor(doc: ProjectDoc, sceneKey: Key): Key? {
        val scenes = doc.manuscript.liveScenes
        val i = scenes.indexOfFirst { it.key == sceneKey }
        if (i < 0) return null
        return scenes.subList(0, i + 1).lastOrNull { it.time is SceneTime.Relative || it.time is SceneTime.Direct }?.key
    }

    /** Prefilled rows of `다음 씬 만들기` (SCR-49, layout `[PROPOSED]`). */
    data class NextScenePrefill(
        val title: String,
        val sub: String,
        val place: PlaceRef?,
        /** `장소 · 결사의 문 · 씬 3과 같음`, or `장소 · 장소 없음`. */
        val placeRow: String,
        val timeBaseKey: Key?,
        /** `때 · 씬 3 기준 · 같은 때`, or `때 · 때 없음`. */
        val timeRow: String,
        val transitionDefault: TransitionMode,
    ) {
        val titleRow: String get() = "제목 · 미정"
        val rowButtons: List<String> get() = listOf("비우기", "바꾸기 ›")
        val transitionChips: List<String> get() = listOf("자동", "직접", "없음")
        val primary: String get() = "만들기"
    }

    fun nextScenePrefill(doc: ProjectDoc, sceneKey: Key): NextScenePrefill? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val n = Labels.sceneOrdinal(doc, sceneKey) ?: return null
        val code = Labels.episodeCodeOfScene(doc, sceneKey)
        val placeName = SceneText.placeName(doc, scene.place)
        val base = timeBaseFor(doc, sceneKey)
        val baseLabel = base?.let { b ->
            if (doc.numberingUnitOfScene(b) == doc.numberingUnitOfScene(sceneKey)) "씬 ${Labels.sceneOrdinal(doc, b)}"
            else Labels.sceneLocation(doc, b)
        }
        return NextScenePrefill(
            title = "다음 씬 만들기",
            sub = "${code.ifEmpty { "" }} · 씬 $n 뒤".trimStart(' ', '·'),
            place = scene.place,
            placeRow = placeName?.let { "장소 · $it · 씬 $n${Labels.and(n)} 같음" } ?: "장소 · 장소 없음",
            timeBaseKey = base,
            timeRow = baseLabel?.let { "때 · $it 기준 · 같은 때" } ?: "때 · 때 없음",
            transitionDefault = doc.project.script.transitionDefault.known ?: TransitionMode.NONE,
        )
    }

    /** Choices of the create sheets; all fields may be cleared (`비우기`). */
    data class NewScene(
        val place: PlaceRef? = null,
        /** Base scene for `같은 때`; null = no 때. */
        val sameTimeAs: Key? = null,
        val title: String? = null,
        val transition: TransitionMode? = null,
        /** `[PROPOSED]`: an empty dialogue block with the inherited speaker receives the caret. */
        val withEmptyBlock: Boolean = true,
    )

    data class Created(val doc: ProjectDoc, val sceneKey: Key, val blockKey: Key?)

    /** Defaults of `다음 씬 만들기` as prefilled. */
    fun prefillChoices(doc: ProjectDoc, sceneKey: Key): NewScene {
        val prefill = nextScenePrefill(doc, sceneKey) ?: return NewScene()
        return NewScene(place = prefill.place, sameTimeAs = prefill.timeBaseKey, transition = prefill.transitionDefault)
    }

    /**
     * `다음 씬 만들기`: a new scene directly after [afterSceneKey] in the same parent (the last scene
     * of an episode stays in that episode). Other scenes' times do not change.
     */
    fun createAfter(doc: ProjectDoc, afterSceneKey: Key, choices: NewScene, ctx: EditContext): Created? {
        val after = doc.index.unit(afterSceneKey) ?: return null
        val siblings = doc.index.children(after.parentKey)
        val next = siblings.getOrNull(siblings.indexOfFirst { it.key == afterSceneKey } + 1)
        return create(doc, after, OrderKeys.between(after.order, next?.order), choices, ctx)
    }

    /** `앞에 새 씬` (SCR-53): an empty scene directly before [beforeSceneKey]; nothing prefilled. */
    fun createBefore(doc: ProjectDoc, beforeSceneKey: Key, ctx: EditContext, choices: NewScene = NewScene()): Created? {
        val before = doc.index.unit(beforeSceneKey) ?: return null
        val siblings = doc.index.children(before.parentKey)
        val prev = siblings.getOrNull(siblings.indexOfFirst { it.key == beforeSceneKey } - 1)
        return create(doc, before, OrderKeys.between(prev?.order, before.order), choices, ctx)
    }

    private fun create(doc: ProjectDoc, sibling: StoryUnit, order: String, choices: NewScene, ctx: EditContext): Created {
        val transition = choices.transition?.takeUnless { it == TransitionMode.AUTO && choices.place == null }
            ?: TransitionMode.NONE
        val scene = StoryUnit(
            key = ctx.newKey(), parentKey = sibling.parentKey, level = sibling.level, order = order, title = choices.title,
            place = choices.place, time = choices.sameTimeAs?.let { TimeChains.sameTimeAs(doc, it) } ?: SceneTime.None,
            transitionMode = transition.open(), updatedAt = ctx.now, updatedBy = ctx.device,
        )
        val out = doc.copy(story = doc.story.copy(units = doc.story.units + scene))
        if (!choices.withEmptyBlock) return Created(out, scene.key, null)
        val ins = BlockOps.insert(out, BlockPosition.SceneStart(scene.key), BlockKind.DIALOGUE, ctx)
        return Created(ins.doc, scene.key, ins.blockKey)
    }
}
