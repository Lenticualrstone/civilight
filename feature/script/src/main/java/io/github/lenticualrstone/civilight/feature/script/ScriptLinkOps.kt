package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.world.common.EditContext as WorldEditContext
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.feature.script.links.LinkCopy
import io.github.lenticualrstone.civilight.feature.script.links.LinkFixes
import io.github.lenticualrstone.civilight.feature.script.panel.SubjectDeleted
import io.github.lenticualrstone.civilight.feature.script.terms.TermMarksModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.ui.picker.PickKind

/*
 * Link and term-candidate operations of the script (SCR-55, SCR-56). Each runs on the op queue
 * after typed text is written, inside one store update (one undo step).
 */

/** The world module's edit context (term links and dismissals). */
internal fun ScriptViewModel.worldCtx(): WorldEditContext =
    WorldEditContext(newKey = Keys::newKey, now = { featureDeps.clock.now() }, device = featureDeps.stores.deviceId.value)

/** `잇기` on a candidate (SCR-56): exactly that text range becomes a link to the term. */
fun ScriptViewModel.linkCandidate(candidate: TermCandidate) {
    if (readOnly) return
    enqueue {
        commitNow()
        update(LinkCopy.LINK) { d -> TermMarksModel.link(d, candidate, worldCtx()) ?: d }
    }
}

/** `아님` on a candidate (SCR-56): the dismissal is stored and the underline goes. */
fun ScriptViewModel.rejectCandidate(candidate: TermCandidate) {
    if (readOnly) return
    enqueue {
        commitNow()
        update(LinkCopy.REJECT) { d -> TermMarksModel.reject(d, candidate, worldCtx()) }
    }
}

/** `다른 항목에 잇기` (SCR-55): the link keeps its key and text and points at the picked item. */
fun ScriptViewModel.relinkLink(blockKey: Key, linkKey: Key, kind: PickKind, targetKey: Key) {
    if (readOnly) return
    val target = when (kind) {
        PickKind.CHARACTER -> LinkTargetKind.CHARACTER
        PickKind.TERM -> LinkTargetKind.TERM
        PickKind.PLACE -> LinkTargetKind.PLACE
        else -> return
    }
    enqueue {
        commitNow()
        update(LinkCopy.RELINK) { d -> LinkFixes.relink(d, blockKey, linkKey, target, targetKey, ctx()) }
    }
}

/** `글로 풀기` (SCR-55): the text stays, the link goes. */
fun ScriptViewModel.unlinkLink(blockKey: Key, linkKey: Key) {
    if (readOnly) return
    enqueue {
        commitNow()
        update(LinkCopy.UNLINK) { d -> LinkFixes.unlink(d, blockKey, linkKey, ctx()) }
    }
}

/** `이대로 두기` on a deleted link (SCR-55, rule 20 §3). */
fun ScriptViewModel.keepLink(targetKey: Key, blockKey: Key) {
    if (readOnly) return
    enqueue {
        commitNow()
        update(LinkCopy.KEEP) { d -> LinkFixes.keep(d, targetKey, blockKey, actionCtx().trash) }
    }
}

/** `되살리기` of a deleted link's target (SCR-55): the item comes back and every reference reconnects. */
fun ScriptViewModel.restoreLinkTarget(kind: LinkTargetKind?, targetKey: Key, authenticated: Boolean) {
    if (readOnly) return
    enqueue {
        commitNow()
        var toast: String? = null
        val context = actionCtx()
        update(LinkCopy.RESTORE) { d ->
            val restored = LinkFixes.restore(d, kind, targetKey, context, authenticated)
            toast = restored?.second
            restored?.first ?: d
        }
        toast?.let(::showUndoToast)
    }
}

/** `이대로 두기` on a record about a deleted item (SCR-57, `대상 지워짐`). */
fun ScriptViewModel.keepSubject(itemKey: Key, recordKey: Key) {
    if (readOnly) return
    enqueue {
        commitNow()
        update(LinkCopy.KEEP) { d -> SubjectDeleted.keep(d, itemKey, recordKey, actionCtx().trash) }
    }
}
