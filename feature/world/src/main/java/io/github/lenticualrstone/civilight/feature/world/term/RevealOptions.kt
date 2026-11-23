package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.RevealEdit
import io.github.lenticualrstone.civilight.domain.world.term.RevealMoves
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldTargets
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** One choice of the `공개 시점 옮기기` sheet; [refusal] rows are shown disabled with the reason. */
data class RevealOptionUi(
    val id: String,
    val label: String,
    val target: RevealTarget,
    val current: Boolean,
    val refusal: String? = null,
)

/** Outcome of choosing a `첫 공개`. */
sealed interface RevealApply {
    /** [toast] = `◇ 첫 공개 1-7 → 1-8 · ▲ 새로 1` (moved) or `◇ 첫 공개 1-2` (first set). */
    data class Applied(val doc: ProjectDoc, val toast: String?) : RevealApply

    /** `선택지 보기 줄에는 첫 공개를 걸 수 없음`. */
    data class Refused(val reason: String) : RevealApply
}

/**
 * The `공개 시점 옮기기` sheet (WLD-18): (a) the term's manuscript links in order
 * (`1-2 씬 1 · 리엔 · “…”`), (b) `원고에서 고르기` (a scene, then one of its blocks; the script has no
 * block-pick mode yet), (c) `에피소드 · 장으로` (a chapter or episode point). Choosing applies at once.
 */
object RevealOptions {
    /** Block kinds a reveal may start at: game-output blocks (a whole choice block is allowed, Q-06). */
    val BLOCK_KINDS = setOf(BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM, BlockKind.CHOICE)

    /** (a): one row per manuscript link of the term, in manuscript order. */
    fun links(doc: ProjectDoc, termKey: Key, hiddenOpen: Boolean): List<RevealOptionUi> {
        val queries = TermQueries(doc, CandidateIndex.pending(), TermAccess(hiddenOpen))
        // (0.2.0, WLD-72) Only a stored (hand-set) reveal is marked current: the automatic one stays pickable.
        val current = queries.storedReveal(termKey)
        return queries.usage(termKey).map { link ->
            val spot = link.spot as ScriptSpot
            val target = when (spot) {
                is ScriptSpot.BlockText -> RevealTarget.Block(spot.blockKey)
                is ScriptSpot.OptionText -> RevealTarget.OptionLine(spot.blockKey, spot.optionKey)
                is ScriptSpot.TransitionText -> RevealTarget.Unit(spot.sceneKey)
            }
            val (text, _, _) = TermCardState.shown(doc, spot, queries.snippetOf(link), hiddenOpen)
            val speaker = queries.describer.speaker(spot).takeUnless { WorldTargets.isLocked(doc, spot, hiddenOpen) }
            RevealOptionUi(
                id = "link:${link.linkKey}",
                label = listOfNotNull(queries.describer.location(spot), speaker, quoted(text, speaker != null)).joinToString(WorldCopy.SEP),
                target = target,
                current = isCurrent(current?.record?.anchor, target),
                refusal = if (target is RevealTarget.OptionLine) TermEdits.OPTION_REVEAL_REFUSED else null,
            )
        }
    }

    /** (b), second step: the scene's game-output blocks (`#044 · 리엔 · “…”`). */
    fun blocks(doc: ProjectDoc, termKey: Key, sceneKey: Key): List<RevealOptionUi> {
        val current = TermQueries(doc, CandidateIndex.pending()).storedReveal(termKey)
        return doc.manuscript.liveBlocksOf(sceneKey)
            .filter { it.kind.known in BLOCK_KINDS }
            .map { block ->
                val text = Runs.displayText(block.runs).ifBlank { block.options.firstOrNull()?.let { Runs.displayText(it.runs) }.orEmpty() }
                val speaker = block.speakerKey?.let { doc.index.character(it)?.name }
                val target = RevealTarget.Block(block.key)
                RevealOptionUi(
                    id = "block:${block.key}",
                    label = listOfNotNull(StoryCodes.blockNumber(block.number), speaker, quoted(text.lineSequence().first(), speaker != null))
                        .joinToString(WorldCopy.SEP),
                    target = target,
                    current = isCurrent(current?.record?.anchor, target),
                )
            }
    }

    /** Levels the point picker offers: chapters down to episodes (`에피소드 · 장으로`). */
    fun pointLevels(doc: ProjectDoc): Set<Int> {
        val structure = doc.project.structure
        val last = structure.episodeLevel ?: (structure.sceneLevel - 1)
        return if (last < 0) setOf(structure.sceneLevel) else (0..last).toSet()
    }

    /** Levels of the scene picker of `원고에서 고르기`. */
    fun sceneLevels(doc: ProjectDoc): Set<Int> = setOf(doc.project.structure.sceneLevel)

    /** Sets or moves the term's `첫 공개` to [target]; the toast names the move and new `▲`. */
    fun apply(doc: ProjectDoc, termKey: Key, target: RevealTarget, ctx: EditContext): RevealApply {
        val before = TermQueries(doc, CandidateIndex.pending())
        return when (val edit = TermEdits.setReveal(doc, termKey, target, ctx)) {
            is RevealEdit.Refused -> RevealApply.Refused(edit.reason)
            is RevealEdit.Applied -> {
                val after = TermQueries(edit.doc, CandidateIndex.pending())
                val toast = RevealMoves.toast(before, after, termKey)
                    ?: after.reveal(termKey)?.episodeLabel?.let(WorldCopy::revealSet)
                RevealApply.Applied(edit.doc, toast)
            }
        }
    }

    private fun quoted(text: String, dialogue: Boolean): String? = when {
        text.isBlank() -> null
        dialogue && !text.startsWith("“") -> "“$text”"
        else -> text
    }

    private fun isCurrent(anchor: Anchor?, target: RevealTarget): Boolean = when (target) {
        is RevealTarget.Block -> (anchor as? Anchor.AtBlock)?.blockKey == target.blockKey
        is RevealTarget.Unit -> (anchor as? Anchor.UnitStart)?.unitKey == target.unitKey
        is RevealTarget.OptionLine -> false
    }
}
