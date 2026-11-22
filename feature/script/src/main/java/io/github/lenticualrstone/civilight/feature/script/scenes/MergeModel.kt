package io.github.lenticualrstone.civilight.feature.script.scenes

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.story.scene.MergeOptions
import io.github.lenticualrstone.civilight.domain.story.scene.MergePlan
import io.github.lenticualrstone.civilight.domain.story.scene.MergeSide
import io.github.lenticualrstone.civilight.domain.story.scene.TransitionOnMerge
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.serialization.Serializable

/** The author's choices in `씬 합치기` (SCR-51), saveable with the sheet; defaults = A's values and `시스템 텍스트로 남기기`. */
@Serializable
data class MergeChoices(
    val place: MergeSide = MergeSide.FIRST,
    val time: MergeSide = MergeSide.FIRST,
    val transition: TransitionOnMerge = TransitionOnMerge.KEEP_AS_SYSTEM_TEXT,
) {
    val options: MergeOptions get() = MergeOptions(place, time, transition)
}

/** One radio row: `씬 3 것` over `2일째 새벽 05:40`. */
data class MergeRadio(val title: String, val sub: String, val selected: Boolean)

/** What the `씬 합치기` sheet shows. */
sealed interface MergeSheetModel {
    val title: String

    /** The first scene of an episode: nothing to merge with (`앞 씬과 합치기` is not offered). */
    data object NotOffered : MergeSheetModel {
        override val title: String get() = SceneCopy.MERGE_TITLE
    }

    /** `회상이 섞임 · 먼저 회상 끄기` / `숨긴 씬이 섞임 · 먼저 숨김 풀기`: the reason and no merge button. */
    data class Blocked(override val title: String, val reason: String) : MergeSheetModel

    /** Board `RSMerge`. */
    data class Ready(
        override val title: String,
        val sub: String,
        /** `결사의 문` + `· 씬 3 · 씬 4 같음` when nothing is asked about the place. */
        val place: FieldLine?,
        val placeRadios: List<MergeRadio>,
        /** `때 · 하나 고르기` above [timeRadios], or the one 때 kept (`2일째 아침 06:10`) as a plain row. */
        val timeHeader: String?,
        val time: FieldLine?,
        val timeRadios: List<MergeRadio>,
        val timeNote: String?,
        val fixedPointNote: String?,
        /** `전환 문구 · 씬 4 직접` + `‘결사의 문 앞 · 06:10’` with `시스템 텍스트로 남기기 | 지우기`. */
        val transitionLine: String?,
        val transitionText: String?,
        val transitionIndex: Int,
        val changes: List<ChangeLine>,
        val canApply: Boolean,
    ) : MergeSheetModel
}

/**
 * `씬 합치기` (SCR-51) as plain data over `domain:actions` `SceneActions.merge` (STO-09 values).
 * [apply] recomputes on the document it writes; a stale or blocked sheet writes nothing.
 */
object MergeModel {
    fun of(doc: ProjectDoc, sceneKey: Key, choices: MergeChoices, readOnly: Boolean, ctx: ActionContext): MergeSheetModel {
        if (doc.index.unit(sceneKey)?.let { doc.manuscript.isLiveUnit(it.key) } != true) return MergeSheetModel.NotOffered
        return when (val plan = plan(doc, sceneKey, choices, ctx)) {
            null, MergePlan.NotOffered -> MergeSheetModel.NotOffered
            is MergePlan.Blocked -> MergeSheetModel.Blocked(SceneCopy.MERGE_TITLE, plan.reason)
            is MergePlan.Ready -> {
                val p = plan.preview
                val placeLine = p.placeLine?.removePrefix("${SceneCopy.FIELD_PLACE} · ")
                val timeAsked = p.timeChoices.isNotEmpty()
                MergeSheetModel.Ready(
                    title = p.title,
                    sub = p.sub,
                    place = placeLine?.let(::placeValue),
                    placeRadios = radios(p.placeChoices, choices.place),
                    timeHeader = if (timeAsked) p.timeLine else null,
                    time = if (!timeAsked) p.timeLine?.removePrefix("${SceneCopy.FIELD_TIME} · ")?.let { FieldLine(it) } else null,
                    timeRadios = radios(p.timeChoices, choices.time),
                    timeNote = p.timeNote,
                    fixedPointNote = p.fixedPointNote,
                    transitionLine = p.transitionLine,
                    transitionText = p.transitionText,
                    transitionIndex = if (choices.transition == TransitionOnMerge.KEEP_AS_SYSTEM_TEXT) 0 else 1,
                    changes = p.changeRows.map(ChangeLine::of),
                    canApply = !readOnly,
                )
            }
        }
    }

    /** `합치기`: the merged document, or [doc] unchanged when the merge is no longer ready. One undo step. */
    fun apply(doc: ProjectDoc, sceneKey: Key, choices: MergeChoices, ctx: ActionContext): ProjectDoc =
        (plan(doc, sceneKey, choices, ctx) as? MergePlan.Ready)?.preview?.result ?: doc

    /** The scene the merge keeps (A), for the script to follow after `합치기`. */
    fun firstScene(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext): Key? =
        (plan(doc, sceneKey, MergeChoices(), ctx) as? MergePlan.Ready)?.preview?.firstKey

    private fun plan(doc: ProjectDoc, sceneKey: Key, choices: MergeChoices, ctx: ActionContext): MergePlan? =
        runCatching { SceneActions.merge(doc, sceneKey, ctx, choices.options) }.getOrNull()

    /** `씬 3 것 · 2일째 새벽 05:40` → radio `씬 3 것` over `2일째 새벽 05:40`. */
    private fun radios(lines: List<String>, side: MergeSide): List<MergeRadio> = lines.mapIndexed { i, line ->
        val cut = line.indexOf(" · ")
        val (title, sub) = if (cut < 0) line to "" else line.substring(0, cut) to line.substring(cut + 3)
        MergeRadio(title, sub, selected = (i == 0) == (side == MergeSide.FIRST))
    }

    /** `결사의 문 · 씬 3 · 씬 4 같음` → `결사의 문` + `· 씬 3 · 씬 4 같음`; a single value stays whole. */
    private fun placeValue(line: String): FieldLine {
        val i = line.indexOf(" · ")
        return if (i < 0) FieldLine(line) else FieldLine(line.substring(0, i), line.substring(i + 1))
    }
}
