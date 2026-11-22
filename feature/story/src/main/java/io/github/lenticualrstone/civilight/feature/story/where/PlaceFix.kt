package io.github.lenticualrstone.civilight.feature.story.where

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.feature.story.common.RestoreStep
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashGates
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/**
 * The scene's place row beyond the domain sheet values (STO-11, STO-20): hidden-place masking and the
 * R20 §2 buttons of a deleted place.
 *
 * @property masked a hidden place while the project's hidden items are locked: `숨긴 항목`, no name.
 * @property restorable `되살리기` is shown: a deleted place with its own trash entry.
 * @property keepable `이대로 두기` is shown: deleted and not kept yet for this scene.
 */
data class PlaceState(val masked: Boolean, val restorable: Boolean, val keepable: Boolean)

/** A deleted place: `장소 고르기 · 되살리기 · 이대로 두기` (R20 §2); every edit is a no-op once it is not deleted. */
object PlaceFix {
    fun state(doc: ProjectDoc, sceneKey: Key, locked: Boolean): PlaceState {
        val ref = doc.index.unit(sceneKey)?.place ?: return PlaceState(masked = false, restorable = false, keepable = false)
        val place = if (ref.kind.known == PlaceRefKind.PLACE) doc.index.place(ref.key) else null
        val masked = locked && place?.hidden == true
        val deleted = deletedPlaceKey(doc, sceneKey) != null
        return PlaceState(
            masked = masked,
            restorable = !masked && deleted && place?.trashEntryKey?.let(doc.index::trashEntry) != null,
            keepable = !masked && deleted && !isKept(doc, ref.key, sceneKey),
        )
    }

    /** The key of the scene's place (or region) when it is not live. */
    fun deletedPlaceKey(doc: ProjectDoc, sceneKey: Key): Key? {
        val ref = doc.index.unit(sceneKey)?.place ?: return null
        val live = when (ref.kind.known) {
            PlaceRefKind.PLACE -> doc.liveness.isPlaceLive(ref.key)
            PlaceRefKind.REGION -> doc.index.region(ref.key)?.let { (map, _) -> doc.liveness.isMapLive(map.key) } ?: false
            null -> true
        }
        return if (live) null else ref.key
    }

    /** `되살리기`: the place's own trash entry through `domain:trash` `RestoreOps.restore`; null = nothing to restore. */
    fun restore(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext, authenticated: Boolean): RestoreOutcome? {
        val key = deletedPlaceKey(doc, sceneKey) ?: return null
        val entry = doc.index.place(key)?.trashEntryKey ?: return null
        return RestoreOps.restore(doc, entry, ctx.trash, authenticated)
    }

    /** [restore] as a [RestoreStep] with the trash toast (`결사의 문 되살림 · …`), or null when nothing came back. */
    fun restoreStep(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext, authenticated: Boolean): RestoreStep? {
        val restored = restore(doc, sceneKey, ctx, authenticated) as? RestoreOutcome.Restored ?: return null
        return RestoreStep(restored.doc, restored.change, Messages.restoreToast(restored.report))
    }

    /** `되살리기` needs `지문으로 보기` first (rule 19 gate on the place's trash entry). */
    fun needsAuth(doc: ProjectDoc, sceneKey: Key): Boolean {
        val key = deletedPlaceKey(doc, sceneKey) ?: return false
        val entry = doc.index.place(key)?.trashEntryKey?.let(doc.index::trashEntry) ?: return false
        return TrashGates.needsAuth(doc, entry)
    }

    /** `이대로 두기`: the `deleted.item` acknowledgement for this reference (`[place, scene]`); the dotted name stays. */
    fun keep(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext): ProjectDoc {
        val key = deletedPlaceKey(doc, sceneKey) ?: return doc
        if (isKept(doc, key, sceneKey)) return doc
        return Fixes.keep(doc, key, ctx.trash, referencingKey = sceneKey).doc
    }

    private fun isKept(doc: ProjectDoc, placeKey: Key, sceneKey: Key): Boolean =
        doc.checks.acknowledgements.any {
            it.checkType == io.github.lenticualrstone.civilight.model.checks.CheckType.DELETED_ITEM &&
                (it.subjectKeys == listOf(placeKey) || it.subjectKeys == listOf(placeKey, sceneKey))
        }
}
