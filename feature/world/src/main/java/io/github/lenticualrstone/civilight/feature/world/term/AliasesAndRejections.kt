package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.term.AliasEdit
import io.github.lenticualrstone.civilight.domain.world.term.AliasProblem
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.StaleRejections
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.domain.world.term.TermRejections
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldTargets
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** What adding or removing an alias did (WLD-16). */
sealed interface AliasChange {
    /** Saved; [newlyFound] = `후보 N 새로 찾음` (null while the first scan has not finished). */
    data class Saved(val doc: ProjectDoc, val newlyFound: Int?) : AliasChange

    /** Refused with the field's error line (the name of the field to fix). */
    data class Refused(val message: String) : AliasChange
}

/**
 * The `별칭` editor (WLD-16, board `RTLinkAll`): the list with per-alias remove and one field with
 * `+`. Saving rebuilds the matcher for that term only and counts the candidates that were not there
 * before (`후보 2 새로 찾음`). Existing text never turns into links.
 */
object AliasEditor {
    fun add(doc: ProjectDoc, termKey: Key, alias: String, index: CandidateIndex, access: TermAccess, ctx: EditContext): AliasChange {
        val term = doc.index.term(termKey) ?: return AliasChange.Refused(WorldCopy.ALIAS_EMPTY)
        return set(doc, termKey, term.aliases + alias, index, access, ctx)
    }

    fun remove(doc: ProjectDoc, termKey: Key, alias: String, index: CandidateIndex, access: TermAccess, ctx: EditContext): AliasChange {
        val term = doc.index.term(termKey) ?: return AliasChange.Refused(WorldCopy.ALIAS_EMPTY)
        return set(doc, termKey, term.aliases - alias, index, access, ctx)
    }

    /**
     * Saves [aliases]. The count compares [index] with a rescan of this term on the new document
     * (WLD-26 row 2), never with a later emission of the shared index, which could still be an older
     * scan. While [index] is pending there is nothing to compare with, so the count is null.
     */
    fun set(doc: ProjectDoc, termKey: Key, aliases: List<String>, index: CandidateIndex, access: TermAccess, ctx: EditContext): AliasChange =
        when (val edit = TermEdits.setAliases(doc, termKey, aliases, ctx)) {
            is AliasEdit.Invalid -> AliasChange.Refused(message(edit.problem))
            is AliasEdit.Applied -> {
                val found = if (index.complete) {
                    val (after, _) = index.rescanTerm(edit.doc, termKey, access)
                    TermEdits.newlyFound(index, after, termKey)
                } else {
                    null
                }
                AliasChange.Saved(edit.doc, found)
            }
        }

    fun message(problem: AliasProblem): String = when (problem) {
        AliasProblem.EMPTY -> WorldCopy.ALIAS_EMPTY
        AliasProblem.SAME_AS_NAME -> WorldCopy.ALIAS_SAME_AS_NAME
        AliasProblem.DUPLICATE -> WorldCopy.ALIAS_DUPLICATE
    }
}

/** One `아님으로 둔 것` row: `1-2 씬 1 · “오리지”` with `되돌리기`. */
data class RejectionRowUi(val rejectionKey: Key, val location: String, val text: String)

/**
 * `⋯ › 아님으로 둔 것` (WLD-22): the term's `아님` records, each with `되돌리기`. Records whose matched
 * text is gone from their block are released (WLD-T08); until a write releases them they are not
 * listed ([StaleRejections.stale] reads a fresh scan of their blocks).
 */
object RejectionRows {
    fun build(doc: ProjectDoc, termKey: Key, hiddenOpen: Boolean): List<RejectionRowUi> {
        val term = doc.index.term(termKey) ?: return emptyList()
        val access = TermAccess(hiddenOpen)
        val describer = TermQueries(doc, CandidateIndex.pending(), access).describer
        val stale = StaleRejections.stale(doc, access)
        return term.rejections.filterNot { it.key in stale }.map { r ->
            val spot = TermRejections.spotOf(r)
            val locked = spot != null && WorldTargets.isLocked(doc, spot, hiddenOpen)
            RejectionRowUi(
                rejectionKey = r.key,
                location = spot?.let { describer.location(it) }.orEmpty(),
                text = if (locked) WorldCopy.HIDDEN_SCENE else "“${r.matchedText}”",
            )
        }
    }

    /** `되돌리기` on a row: the candidate comes back (if its text is still there). */
    fun restore(doc: ProjectDoc, termKey: Key, rejectionKey: Key, ctx: EditContext): ProjectDoc =
        TermEdits.restoreRejection(doc, termKey, rejectionKey, ctx)
}
