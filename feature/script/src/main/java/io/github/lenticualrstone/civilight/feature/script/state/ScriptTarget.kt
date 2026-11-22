package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.BlockTrash
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
    /**
     * An empty scene whose virtual first line takes the caret on open (SCR-01 `[PROPOSED]`: "the
     * scene head followed by one empty dialogue block … and the caret in it").
     */
    val emptyScene: Key? = null,
)

object ScriptTarget {
    /**
     * Resolves the `Script` route:
     * - the live [blockKey] (caret at its end with [caretAtEnd], `이어 쓰기`);
     * - else the live [sceneKey] ([sceneTarget]);
     * - else the last writing position (caret at its end with [caretAtEnd], else selected);
     * - else the first live scene ([sceneTarget]);
     * - else the first live episode with no scene (empty episode state).
     *
     * Whether the caret is actually placed is the caller's call: a read-only project never gets one.
     */
    fun resolve(doc: ProjectDoc, sceneKey: Key?, blockKey: Key?, caretAtEnd: Boolean): OpenTarget {
        val m = doc.manuscript
        blockKey?.let(doc.index::block)?.takeIf { m.isLiveBlock(it.key) }?.let { block ->
            return blockTarget(doc, block.key, caretAtEnd)
        }
        sceneKey?.takeIf { m.isLiveUnit(it) && doc.index.unit(it)?.let(doc.index::isScene) == true }?.let {
            return sceneTarget(doc, it, caretAtEnd)
        }
        val activity = doc.app.activity.activity
        activity.lastEditedBlockKey?.let(doc.index::block)?.takeIf { m.isLiveBlock(it.key) }?.let { block ->
            return blockTarget(doc, block.key, caretAtEnd)
        }
        m.liveScenes.firstOrNull()?.let { return sceneTarget(doc, it.key, caretAtEnd) }
        return OpenTarget(firstLiveNumberingUnit(doc), null, null, null)
    }

    private fun blockTarget(doc: ProjectDoc, blockKey: Key, caretAtEnd: Boolean): OpenTarget {
        val block = doc.index.block(blockKey)!!
        val caret = if (caretAtEnd && block.kind.known in BlockOps.TEXT_KINDS) block.runs.plainText.length else null
        return OpenTarget(unitOf(doc, block.sceneKey), block.sceneKey, block.key, caret)
    }

    /**
     * A scene target. An empty scene opens with the caret in its virtual first line, and a scene that
     * holds only empty text blocks (a new scene's first line, SCR-49) with the caret in the first of
     * them: the writer can type at once (SCR-01). With [caretAtEnd] the caret goes to the end of the
     * scene's last text block. Otherwise the scene head is scrolled to the top, nothing selected.
     */
    fun sceneTarget(doc: ProjectDoc, sceneKey: Key, caretAtEnd: Boolean): OpenTarget {
        val unit = unitOf(doc, sceneKey)
        val blocks = doc.manuscript.liveBlocksOf(sceneKey)
        if (blocks.isEmpty()) return OpenTarget(unit, sceneKey, null, null, emptyScene = sceneKey)
        val first = blocks.first()
        if (blocks.all { it.kind.known in BlockOps.TEXT_KINDS && BlockTrash.isEmpty(doc, it) }) {
            return OpenTarget(unit, sceneKey, first.key, 0)
        }
        if (caretAtEnd) {
            val last = blocks.last()
            if (last.kind.known in BlockOps.TEXT_KINDS) return OpenTarget(unit, sceneKey, last.key, last.runs.plainText.length)
        }
        return OpenTarget(unit, sceneKey, null, null)
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
