package io.github.lenticualrstone.civilight.feature.recorder.record

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/**
 * `되살리기` on a deleted card's chip (LRC-23 `@` card, rule 20 §2) through the binding paths
 * (DECISIONS §1): characters through `domain:actions` `CharacterActions.restore` (its toast from the
 * cast snapshot), places, terms and factions through `domain:trash` `RestoreOps` (the trash report
 * toast). Other kinds are never offered `되살리기` by the card.
 */
object TargetRestore {
    /** The restored document and its toast text, or null when nothing came back. */
    fun restore(doc: ProjectDoc, target: TargetRef, ctx: ActionContext, authenticated: Boolean): Pair<ProjectDoc, String>? {
        if (target.projectKey != doc.key) return null
        return when (target.kind) {
            RecorderTargetKind.CHARACTER -> {
                val result = CharacterActions.restore(doc, target.key, ctx, authenticated)
                (result.outcome as? RestoreOutcome.Restored)?.let { it.doc to result.toast?.message.orEmpty() }
            }
            RecorderTargetKind.PLACE, RecorderTargetKind.TERM, RecorderTargetKind.FACTION -> {
                val entryKey = trashEntryOf(doc, target) ?: return null
                (RestoreOps.restore(doc, entryKey, ctx.trash, authenticated) as? RestoreOutcome.Restored)
                    ?.let { it.doc to Messages.restoreToast(it.report).message }
            }
            else -> null
        }
    }

    /** The trash entry holding the card, or null when it is not in the trash. */
    fun trashEntryOf(doc: ProjectDoc, target: TargetRef): Key? = when (target.kind) {
        RecorderTargetKind.CHARACTER -> doc.index.character(target.key)?.trashEntryKey
        RecorderTargetKind.PLACE -> doc.index.place(target.key)?.trashEntryKey
        RecorderTargetKind.TERM -> doc.index.term(target.key)?.trashEntryKey
        RecorderTargetKind.FACTION -> doc.index.faction(target.key)?.trashEntryKey
        else -> null
    }
}
