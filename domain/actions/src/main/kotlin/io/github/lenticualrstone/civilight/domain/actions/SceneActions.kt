package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.story.scene.MergeOptions
import io.github.lenticualrstone.civilight.domain.story.scene.MergePlan
import io.github.lenticualrstone.civilight.domain.story.scene.MovePreview
import io.github.lenticualrstone.civilight.domain.story.scene.NewScene
import io.github.lenticualrstone.civilight.domain.story.scene.NewScenePrefill
import io.github.lenticualrstone.civilight.domain.story.scene.SceneMerge
import io.github.lenticualrstone.civilight.domain.story.scene.SceneMove
import io.github.lenticualrstone.civilight.domain.story.scene.SceneSplit
import io.github.lenticualrstone.civilight.domain.story.scene.SceneTrash
import io.github.lenticualrstone.civilight.domain.story.scene.SilentCastChoice
import io.github.lenticualrstone.civilight.domain.story.scene.SplitPreview
import io.github.lenticualrstone.civilight.domain.trash.DocChange
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RefKind
import io.github.lenticualrstone.civilight.domain.trash.ReferenceScanner
import io.github.lenticualrstone.civilight.domain.trash.Relocation
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.RestoreReport
import io.github.lenticualrstone.civilight.domain.trash.SceneChoice
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.domain.trash.TrashGates
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** What `씬 지우기` did: the project after it, its trash entry, relocated records, the toast and the undo pair. */
data class SceneDeleteResult(
    val doc: ProjectDoc,
    val entry: TrashEntry,
    /** Records whose anchor moved off the scene's blocks (§5.5). */
    val relocations: List<Relocation>,
    /** Acknowledgements dropped because a subject stopped being live (STO-41). */
    val removedAcks: List<Acknowledgement>,
    /** `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로` with `되돌리기`. */
    val toast: Toast,
    /** Before / after of the whole action (mark, entry, 때 rebase, anchors, acks): `되돌리기` reverts it. */
    val change: DocChange,
)

/**
 * The one entry point for scene structure and scene trash (DECISIONS §1): new, split, merge, move
 * and reorder pass through to `domain:story` `scene.*`; delete and restore compose `domain:story`
 * [SceneTrash] (mark, entry, 때 rebase, STO-10) with `domain:trash` (anchors, acknowledgements,
 * reference snapshot, toast). Every function is pure: it takes a [ProjectDoc] and returns the next one.
 */
object SceneActions {
    // --- Structure (pass-through to domain:story) ---

    /** Prefill of `새 씬` after [previousSceneKey] (STO-05). */
    fun prefillAfter(doc: ProjectDoc, previousSceneKey: Key): NewScenePrefill = NewScene.prefillAfter(doc, previousSceneKey)

    /** Prefill of `새 씬` before [nextSceneKey] (STO-05). */
    fun prefillBefore(doc: ProjectDoc, nextSceneKey: Key): NewScenePrefill = NewScene.prefillBefore(doc, nextSceneKey)

    /**
     * `새 씬` under [parentKey] after [afterSceneKey] (null = first): the new doc and the new scene key.
     * [parentKey] null = a top-level scene, only in 1-level structures (scenes have no parent there;
     * elsewhere it throws); no other scene's 때 changes. The prefills and [move] still take a parent.
     */
    fun create(
        doc: ProjectDoc,
        parentKey: Key?,
        afterSceneKey: Key?,
        ctx: ActionContext,
        title: String? = null,
        place: PlaceRef? = null,
        time: SceneTime = SceneTime.None,
        transitionMode: TransitionMode = NewScene.defaultTransition(doc),
    ): Pair<ProjectDoc, Key> =
        if (parentKey != null) {
            NewScene.create(doc, parentKey, afterSceneKey, ctx.edit, title, place, time, transitionMode)
        } else {
            createTopLevel(doc, afterSceneKey, ctx, title, place, time, transitionMode)
        }

    private fun createTopLevel(
        doc: ProjectDoc,
        afterSceneKey: Key?,
        ctx: ActionContext,
        title: String?,
        place: PlaceRef?,
        time: SceneTime,
        transitionMode: TransitionMode,
    ): Pair<ProjectDoc, Key> {
        require(doc.project.structure.sceneLevel == 0) { "a scene without a parent needs a 1-level structure" }
        val siblings = doc.index.children(null)
        val order = if (afterSceneKey == null) {
            OrderKeys.before(siblings.firstOrNull()?.order)
        } else {
            val i = siblings.indexOfFirst { it.key == afterSceneKey }
            require(i >= 0) { "$afterSceneKey is not a top-level unit" }
            OrderKeys.between(siblings[i].order, siblings.getOrNull(i + 1)?.order)
        }
        val key = ctx.newKey()
        val unit = StoryUnit(
            key = key,
            parentKey = null,
            level = doc.project.structure.sceneLevel,
            order = order,
            title = title,
            place = place,
            time = time,
            transitionMode = OpenEnum.of(transitionMode),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return doc.copy(story = doc.story.copy(units = doc.story.units + unit)) to key
    }

    /** `씬 나누기` sheet and result (`SplitPreview.result`, STO-09). */
    fun split(
        doc: ProjectDoc,
        sceneKey: Key,
        afterBlockKey: Key,
        ctx: ActionContext,
        silentCast: Map<Key, SilentCastChoice> = emptyMap(),
        keepFlashback: Boolean = true,
        keepHidden: Boolean = true,
    ): SplitPreview = SceneSplit.plan(doc, sceneKey, afterBlockKey, ctx.edit, silentCast, keepFlashback, keepHidden)

    /** `앞 씬과 합치기` on [secondKey] (STO-09): not offered, blocked, or the sheet with its result. */
    fun merge(doc: ProjectDoc, secondKey: Key, ctx: ActionContext, options: MergeOptions = MergeOptions()): MergePlan =
        SceneMerge.plan(doc, secondKey, ctx.edit, options)

    /** Scene move sheet and result (`MovePreview.result`, STO-08). */
    fun move(doc: ProjectDoc, sceneKey: Key, parentKey: Key, afterSceneKey: Key?, ctx: ActionContext): MovePreview =
        SceneMove.plan(doc, sceneKey, parentKey, afterSceneKey, ctx.edit)

    /** Reorders an episode, chapter or other non-scene unit (STO-08, no preview). */
    fun reorderUnit(doc: ProjectDoc, unitKey: Key, parentKey: Key?, afterKey: Key?, ctx: ActionContext): ProjectDoc =
        SceneMove.reorderUnit(doc, unitKey, parentKey, afterKey, ctx.edit)

    // --- Trash ---

    /**
     * `씬 지우기` (STO-10, SCR-43): [SceneTrash.delete] sets the mark, writes the entry and rebases
     * 때; [TrashOps.settleDelete] relocates records anchored in the scene and drops acknowledgements
     * of subjects that stopped being live; the entry keeps the reference sites for the restore report
     * (as [TrashOps.delete] does for every other item); the toast is [Messages.sceneDeleteToast].
     */
    fun delete(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext): SceneDeleteResult {
        val marked = SceneTrash.delete(doc, sceneKey, ctx.edit)
        val settled = TrashOps.settleDelete(doc, marked, ctx.trash)
        var d = settled.doc
        val entryKey = requireNotNull(d.index.unit(sceneKey)?.trashEntryKey) { "scene $sceneKey has no trash entry" }
        // References at delete time: read before the 때 rebase moves the dependents' bases away.
        val sites = ReferenceScanner(doc).referencesTo(ItemId.unit(sceneKey)).map { it.id }
        if (sites.isNotEmpty()) {
            d = d.copy(
                trash = d.trash.copy(
                    entries = d.trash.entries.map { e ->
                        if (e.key != entryKey) e
                        else e.copy(ext = JsonObject((e.ext ?: JsonObject(emptyMap())) + (TrashOps.REF_SITES to JsonArray(sites.map(::JsonPrimitive)))))
                    },
                ),
            )
        }
        val entry = requireNotNull(d.index.trashEntry(entryKey))
        return SceneDeleteResult(
            doc = d,
            entry = entry,
            relocations = settled.relocations,
            removedAcks = settled.removedAcks,
            toast = Messages.sceneDeleteToast(doc, d, sceneKey, settled.relocations),
            change = DocChange(doc, d),
        )
    }

    /** `되돌리기` right after [delete]: row-level revert, later edits of other rows survive. */
    fun undo(current: ProjectDoc, result: SceneDeleteResult): ProjectDoc = result.change.revert(current)

    /**
     * `되살리기` of a trashed scene (from the trash list or the check center): the rule-19 gate
     * ([TrashGates.needsAuth]) first, then [SceneTrash.restore] (mark, entry, 때 back onto the
     * scene) and [RestoreOps.settleRestore] (records re-attach, `deleted.item` acknowledgements go).
     * The report counts references that point at the scene again and the ones changed meanwhile.
     */
    fun restore(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext, authenticated: Boolean = false): RestoreOutcome {
        val scene = doc.index.unit(sceneKey) ?: return RestoreOutcome.NotFound
        if (scene.deletedAt == null) return RestoreOutcome.NotFound
        val entry = scene.trashEntryKey?.let(doc.index::trashEntry)
        if (entry != null && TrashGates.needsAuth(doc, entry) && !authenticated) return RestoreOutcome.NeedsAuth
        val unmarked = SceneTrash.restore(doc, sceneKey, ctx.edit)
        val settled = RestoreOps.settleRestore(doc, unmarked, ctx.trash)
        val report = report(settled.doc, entry, sceneKey, settled.reattached)
        return RestoreOutcome.Restored(settled.doc, report, DocChange(doc, settled.doc))
    }

    /**
     * Restores the entry [entryKey] from the trash list: a scene entry goes through [restore]; a
     * block group through [restoreBlocks] with all its blocks; anything else is not a scene action.
     */
    fun restoreEntry(doc: ProjectDoc, entryKey: Key, ctx: ActionContext, authenticated: Boolean = false, sceneChoice: SceneChoice? = null): RestoreOutcome {
        val entry = doc.index.trashEntry(entryKey) ?: return RestoreOutcome.NotFound
        return when (entry.kind.known) {
            io.github.lenticualrstone.civilight.model.trash.TrashKind.SCENE -> {
                val sceneKey = entry.targetKeys.singleOrNull() ?: return RestoreOutcome.NotFound
                restore(doc, sceneKey, ctx, authenticated)
            }
            io.github.lenticualrstone.civilight.model.trash.TrashKind.BLOCK_GROUP ->
                restoreBlocks(doc, entryKey, entry.targetKeys, ctx, authenticated, sceneChoice)
            else -> RestoreOutcome.NotFound
        }
    }

    /**
     * Restores blocks of a block group (SCR-39). When their scene is in the trash the caller picks a
     * [SceneChoice]: `씬도 되살리기` restores the scene through [restore] first (so its 때 comes back
     * with the rebase undone), then the blocks at their places; `다른 씬 끝에` appends them to another
     * live scene. Without a choice the outcome asks for one ([RestoreOutcome.NeedsSceneChoice]).
     */
    fun restoreBlocks(
        doc: ProjectDoc,
        entryKey: Key,
        blockKeys: List<Key>,
        ctx: ActionContext,
        authenticated: Boolean = false,
        sceneChoice: SceneChoice? = null,
    ): RestoreOutcome {
        val entry = doc.index.trashEntry(entryKey) ?: return RestoreOutcome.NotFound
        if (TrashGates.needsAuth(doc, entry) && !authenticated) return RestoreOutcome.NeedsAuth
        val sceneKey = blockKeys.firstNotNullOfOrNull { doc.index.block(it)?.sceneKey } ?: return RestoreOutcome.NotFound
        var d = doc
        var reattached = emptyList<Key>()
        val scene = doc.index.unit(sceneKey)
        if (sceneChoice == SceneChoice.RestoreScene && scene != null && scene.deletedAt != null) {
            when (val sceneBack = restore(d, sceneKey, ctx, authenticated)) {
                is RestoreOutcome.Restored -> {
                    d = sceneBack.doc
                    reattached = sceneBack.report.reattached
                }
                RestoreOutcome.NeedsAuth -> return RestoreOutcome.NeedsAuth
                else -> return sceneBack
            }
        }
        // A trashed ancestor (episode) is still cleared by RestoreOps' own `씬도 되살리기` step.
        val outcome = RestoreOps.restoreBlocks(d, entryKey, blockKeys, ctx.trash, authenticated = true, sceneChoice = sceneChoice)
        return when (outcome) {
            is RestoreOutcome.Restored -> RestoreOutcome.Restored(
                outcome.doc,
                outcome.report.copy(reattached = (reattached + outcome.report.reattached).distinct()),
                DocChange(doc, outcome.doc),
            )
            else -> outcome
        }
    }

    /** The restore report of a scene, from the reference sites stored at delete time (as `RestoreOps` builds it for other items). */
    private fun report(doc: ProjectDoc, entry: TrashEntry?, sceneKey: Key, reattached: List<Key>): RestoreReport {
        val item = ItemId.unit(sceneKey)
        val now = ReferenceScanner(doc).referencesTo(item)
        val stored = entry?.let(TrashOps::refSitesOf).orEmpty().toSet()
        val nowIds = now.map { it.id }.toSet()
        val kept = stored.filter { it !in nowIds }.mapNotNull { id -> RefKind.entries.firstOrNull { id.startsWith(it.name + ":") } }
        return RestoreReport(
            item = item,
            name = doc.index.unit(sceneKey)?.title,
            relinked = now.groupingBy { it.kind }.eachCount(),
            keptChanged = kept.groupingBy { it }.eachCount(),
            reattached = reattached,
        )
    }
}
