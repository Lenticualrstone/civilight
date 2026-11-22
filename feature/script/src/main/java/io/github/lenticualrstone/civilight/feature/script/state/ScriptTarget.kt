package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.plainText

/**
 * Where the script opens (SCR-01 entry): one numbering unit (an episode), the scene or block to
 * scroll to, and whether the caret goes into the block.
 */
data class OpenTarget(
    /** The numbering unit shown (an episode; the project key in a 1-level project). */
    val unitKey: Key,
    /** Scene whose head is scrolled to the top when no block is given. */
    val sceneKey: Key?,
    /** Block scrolled to and selected. */
    val blockKey: Key?,
    /** Raw caret offset in [blockKey] when the caret goes into it (`이어 쓰기`), else null. */
    val caretOffset: Int?,
)

object ScriptTarget {
    /**
     * Resolves the `Script` route: the live [blockKey] (caret at its end with [caretAtEnd]), else
     * the head of the live [sceneKey], else the last writing position (selected, no caret), else the
     * first live scene, else the first live episode with no scene (empty episode state).
     */
    fun resolve(doc: ProjectDoc, sceneKey: Key?, blockKey: Key?, caretAtEnd: Boolean): OpenTarget {
        val m = doc.manuscript
        blockKey?.let(doc.index::block)?.takeIf { m.isLiveBlock(it.key) }?.let { block ->
            val caret = if (caretAtEnd) block.runs.plainText.length else null
            return OpenTarget(unitOf(doc, block.sceneKey), block.sceneKey, block.key, caret)
        }
        sceneKey?.takeIf { m.isLiveUnit(it) && doc.index.unit(it)?.let(doc.index::isScene) == true }?.let {
            return OpenTarget(unitOf(doc, it), it, null, null)
        }
        val activity = doc.app.activity.activity
        activity.lastEditedBlockKey?.let(doc.index::block)?.takeIf { m.isLiveBlock(it.key) }?.let { block ->
            return OpenTarget(unitOf(doc, block.sceneKey), block.sceneKey, block.key, null)
        }
        m.liveScenes.firstOrNull()?.let { return OpenTarget(unitOf(doc, it.key), it.key, null, null) }
        return OpenTarget(firstLiveNumberingUnit(doc), null, null, null)
    }

    /** The numbering unit of a scene (§4.1), or the project key. */
    fun unitOf(doc: ProjectDoc, sceneKey: Key): Key = doc.index.numberingUnitKeyOfScene(sceneKey) ?: doc.project.key

    /** First live unit at the episode level in tree order; the project key for a 1-level project. */
    fun firstLiveNumberingUnit(doc: ProjectDoc): Key {
        val level = doc.project.structure.episodeLevel ?: return doc.project.key
        fun visit(parent: Key?): Key? {
            for (unit in doc.index.children(parent)) {
                if (!doc.manuscript.isLiveUnit(unit.key)) continue
                if (unit.level == level) return unit.key
                if (unit.level < level) visit(unit.key)?.let { return it }
            }
            return null
        }
        return visit(null) ?: doc.project.key
    }
}
