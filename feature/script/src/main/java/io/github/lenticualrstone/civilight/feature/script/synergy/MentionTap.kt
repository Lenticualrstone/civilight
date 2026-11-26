package io.github.lenticualrstone.civilight.feature.script.synergy

import io.github.lenticualrstone.civilight.domain.trash.HidingLocks
import io.github.lenticualrstone.civilight.domain.trash.RefState
import io.github.lenticualrstone.civilight.domain.trash.Resolution
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind

/**
 * What a tap on an `@` mention opens (rules 19 and 20). The preview sheet is only for a LIVE target of a
 * kind it can show; a locked hidden target asks for the hidden-items unlock and shows nothing; a trashed,
 * name-tagged or missing target opens the link card (`되살리기` · `다른 항목에 잇기` · `글로 풀기` · `이대로 두기`).
 */
object MentionTap {
    sealed interface Action {
        data class Preview(val kind: LinkTargetKind) : Action
        data object Unlock : Action
        data object Card : Action
    }

    private val PREVIEW_KINDS = setOf(
        LinkTargetKind.CHARACTER, LinkTargetKind.TERM, LinkTargetKind.PLACE, LinkTargetKind.FACTION, LinkTargetKind.EVENT,
    )

    fun decide(doc: ProjectDoc, target: LinkTarget, unlocked: Boolean): Action {
        val resolved = Resolution.resolve(doc, target, HidingLocks(itemsLocked = !unlocked)) ?: return Action.Card
        return when (resolved.state) {
            RefState.HIDDEN_LOCKED -> Action.Unlock
            RefState.LIVE -> target.kind.known?.takeIf { it in PREVIEW_KINDS }?.let { Action.Preview(it) } ?: Action.Card
            RefState.TRASHED, RefState.TAGGED, RefState.MISSING -> Action.Card
        }
    }

    /** The preview sheet may draw [target] now (re-checked while the sheet is open: the gate can relock). */
    fun previewable(doc: ProjectDoc, target: LinkTarget, unlocked: Boolean): Boolean = decide(doc, target, unlocked) is Action.Preview
}
