package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeChange
import io.github.lenticualrstone.civilight.domain.story.time.keepAbsoluteOn
import io.github.lenticualrstone.civilight.domain.story.withTimes
import io.github.lenticualrstone.civilight.domain.story.withUnits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind

/**
 * `씬 지우기` and restoring a scene: the 때 and trash-mark side (STO-10). No other scene's absolute
 * time changes; the 집필 선 is derived and moves by itself. Record relocation (SCR-43/§5.5), the
 * toast and acknowledgement pruning are the script, trash and checks areas'.
 */
object SceneTrash {
    /**
     * Moves the scene (with its blocks, by containment) to the trash for 30 days, without asking.
     * Every scene based on it is rebased to its own base (else the nearest earlier timed scene) with
     * the interval recomputed and `rebasedFromKey` = the deleted scene.
     */
    fun delete(doc: ProjectDoc, sceneKey: Key, ctx: EditContext): ProjectDoc {
        val scene = requireNotNull(doc.index.unit(sceneKey)) { "unknown scene $sceneKey" }
        require(doc.liveness.isUnitLive(sceneKey)) { "scene $sceneKey is not live" }
        val before = SceneTimes(doc)
        val entryKey = ctx.newKey()
        val marked = doc.withUnits(mapOf(sceneKey to scene.copy(deletedAt = ctx.now, trashEntryKey = entryKey)), ctx)
        val entry = TrashEntry(
            key = entryKey,
            kind = TrashKind.SCENE.open(),
            projectKey = doc.project.key,
            targetKeys = listOf(sceneKey),
            hiddenAtDelete = scene.hidden,
            deletedAt = ctx.now,
            purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        val withEntry = marked.copy(trash = marked.trash.copy(entries = marked.trash.entries + entry))
        return TimeChange.rebaseDirectDependents(withEntry, before, sceneKey, ctx, rebasedFrom = sceneKey)
    }

    /**
     * Restores a trashed scene (undo or trash). It returns with its own stored 때 (re-resolved; if
     * its base is gone it is rebased like a delete). Scenes still marked `rebasedFromKey` = this
     * scene (not edited meanwhile) return to it as base, absolute unchanged (`ED`, Q7).
     */
    fun restore(doc: ProjectDoc, sceneKey: Key, ctx: EditContext): ProjectDoc {
        val scene = requireNotNull(doc.index.unit(sceneKey)) { "unknown scene $sceneKey" }
        val before = SceneTimes(doc)
        val cleared = doc.withUnits(mapOf(sceneKey to scene.copy(deletedAt = null, trashEntryKey = null)), ctx)
        val restored = cleared.copy(trash = cleared.trash.copy(entries = cleared.trash.entries.filterNot { it.key == scene.trashEntryKey }))
        val times = SceneTimes(restored)
        val changes = LinkedHashMap<Key, SceneTime>()
        // Its own base may be gone: rebase onto the nearest earlier timed scene, keeping its time.
        val own = scene.time as? SceneTime.Relative
        if (own != null && (!times.isLive(own.baseKey) || times.timed(own.baseKey) == null)) {
            keepAbsoluteOnStored(before, times, sceneKey)?.let { changes[sceneKey] = it }
        }
        val partial = restored.withTimes(changes, ctx)
        val partialTimes = SceneTimes(partial)
        val returning = partialTimes.liveScenes.filter { (it.time as? SceneTime.Relative)?.rebasedFromKey == sceneKey }
        val back = returning.mapNotNull { d ->
            keepAbsoluteOn(partialTimes, d.key, sceneKey, rebasedFromKey = null)?.let { d.key to it }
        }.toMap()
        return partial.withTimes(back, ctx)
    }

    /** The restored scene keeps the absolute time it resolved to while in the trash. */
    private fun keepAbsoluteOnStored(before: SceneTimes, after: SceneTimes, sceneKey: Key): SceneTime? {
        if (before.timed(sceneKey) == null) return null
        val previous = after.previousTimed(sceneKey)?.key
        return keepAbsoluteOn(before, sceneKey, previous)
    }
}
