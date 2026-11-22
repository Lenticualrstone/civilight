package io.github.lenticualrstone.civilight.domain.story.structure

import io.github.lenticualrstone.civilight.domain.story.text.BlockText
import io.github.lenticualrstone.civilight.model.Key

/**
 * `이어 쓰기` (STO-01) with the block it opens: [where] `1-7 씬 3 · 꺼진 등불`, [quote] the block's
 * first line in quotes. The same block [StoryOverview.resume] labels, so the label and the opened
 * position never disagree.
 */
data class ResumeTarget(val blockKey: Key, val sceneKey: Key, val where: String, val quote: String)

/**
 * The `이어 쓰기` block: the last edited block when it is live and not in a locked hidden scene,
 * else the last visible block with content (`ED`); null for a project without content.
 */
fun StoryOverview.resumeTarget(): ResumeTarget? {
    val visible = { key: Key -> doc.index.unit(key)?.let { !(it.hidden && locked) } ?: false }
    val block = lastEditedBlockKey?.let(doc.index::block)
        ?.takeIf { doc.liveness.isLive(it) && visible(it.sceneKey) }
        ?: doc.manuscript.liveBlocks.lastOrNull { it.hasContent() && visible(it.sceneKey) }
        ?: return null
    val scene = doc.index.unit(block.sceneKey) ?: return null
    val firstLine = (BlockText.gameText(block) ?: BlockText.display(block.runs)).lineSequence().firstOrNull().orEmpty()
    return ResumeTarget(block.key, scene.key, codes.sceneRefWithTitle(scene), "“$firstLine”")
}
