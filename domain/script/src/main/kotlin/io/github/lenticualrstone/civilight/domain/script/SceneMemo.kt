package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/**
 * `씬 메모` (SCR-35 `모듈` card, editable in place `[PROPOSED]`): the scene's free-text memo
 * (`StoryUnit.memo`). Merging scenes appends lines (SCR-51) and the recorder's `메모 칸에 넣기`
 * appends one line; this is the direct edit of the whole text.
 */
object SceneMemo {
    /** The memo shown on the card (null without one). */
    fun of(doc: ProjectDoc, sceneKey: Key): String? = doc.index.unit(sceneKey)?.memo?.takeIf { it.isNotBlank() }

    /**
     * Writes [text] (NFC, trailing blank lines dropped; blank = no memo) as the memo of a live scene.
     * Unchanged text returns [doc] itself, so callers write nothing.
     */
    fun set(doc: ProjectDoc, sceneKey: Key, text: String, ctx: EditContext): ProjectDoc {
        val scene = doc.index.unit(sceneKey)?.takeIf { doc.manuscript.isLiveUnit(it.key) && it.level == doc.project.structure.sceneLevel } ?: return doc
        val clean = text.nfc().trimEnd().takeIf { it.isNotBlank() }
        if (clean == scene.memo?.takeIf { it.isNotBlank() }) return doc
        return doc.updateUnit(sceneKey) { it.copy(memo = clean, updatedAt = ctx.now, updatedBy = ctx.device) }
    }
}
