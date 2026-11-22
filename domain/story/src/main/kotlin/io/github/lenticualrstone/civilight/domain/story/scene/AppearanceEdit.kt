package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Edits of a scene's silent cast (STO-26, `등장 | … | + 세라`). Speakers are a fact of the blocks and
 * are never stored here. Every edit is a no-op (the same doc) when its precondition no longer holds,
 * so a stale sheet never changes anything.
 */
object AppearanceEdit {
    /**
     * `+` in the 등장 row: [characterKey] joins the silent cast of the live scene [sceneKey]. No-op
     * for an unknown or trashed character, one who already speaks in the scene, or one already listed.
     */
    fun addSilent(doc: ProjectDoc, sceneKey: Key, characterKey: Key, ctx: EditContext): ProjectDoc {
        val scene = liveScene(doc, sceneKey) ?: return doc
        val character = doc.index.character(characterKey) ?: return doc
        if (character.deletedAt != null) return doc
        if (characterKey in scene.silentCast) return doc
        if (characterKey in Appearance.of(doc, sceneKey).speakers) return doc
        return doc.withSilent(sceneKey, scene.silentCast + characterKey, ctx)
    }

    /**
     * `빼기` on a silent member (also on a deleted one, R20 §2): removes [characterKey] from the silent
     * cast. No-op when the scene is gone or the character is not in its silent cast.
     */
    fun removeSilent(doc: ProjectDoc, sceneKey: Key, characterKey: Key, ctx: EditContext): ProjectDoc {
        val scene = liveScene(doc, sceneKey) ?: return doc
        if (characterKey !in scene.silentCast) return doc
        return doc.withSilent(sceneKey, scene.silentCast - characterKey, ctx)
    }

    private fun liveScene(doc: ProjectDoc, sceneKey: Key) = doc.index.unit(sceneKey)
        ?.takeIf { it.level == doc.project.structure.sceneLevel && doc.liveness.isUnitLive(it.key) }

    private fun ProjectDoc.withSilent(sceneKey: Key, cast: List<Key>, ctx: EditContext): ProjectDoc = copy(
        story = story.copy(
            units = story.units.map { if (it.key == sceneKey) it.copy(silentCast = cast, updatedAt = ctx.now, updatedBy = ctx.device) else it },
        ),
    )
}
