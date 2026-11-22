package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * `첫 등장` (CST-14): the earliest live scene in manuscript order where the character speaks (a
 * dialogue block or a spoken choice option) or is in the scene's silent cast. Trashed scenes and
 * blocks do not count. Never stored.
 */
object FirstAppearance {
    /** Speakers of a block: its own and its options'. */
    fun speakersOf(block: Block): List<Key> = listOfNotNull(block.speakerKey) + block.options.mapNotNull { it.speakerKey }

    /** First scene of every character that appears at all. */
    fun firstScenes(doc: ProjectDoc): Map<Key, StoryUnit> {
        val out = LinkedHashMap<Key, StoryUnit>()
        for (scene in doc.manuscript.liveScenes) {
            for (key in scene.silentCast) out.putIfAbsent(key, scene)
            for (block in doc.manuscript.liveBlocksOf(scene.key)) for (key in speakersOf(block)) out.putIfAbsent(key, scene)
        }
        return out
    }

    fun scene(doc: ProjectDoc, characterKey: Key): StoryUnit? = firstScenes(doc)[characterKey]

    /** `1-2`, or null when the character never appears. */
    fun label(doc: ProjectDoc, characterKey: Key): String? = scene(doc, characterKey)?.let { StoryLabels.episodeCodeOf(doc, it.key) }

    /** `아직 등장 전`: the first appearance starts after the lens point (or there is none yet). */
    fun isAfterLens(doc: ProjectDoc, characterKey: Key, lens: LensPoint): Boolean {
        val scene = scene(doc, characterKey) ?: return false
        val start = doc.manuscript.rankOf(Position.Start(scene.key)) ?: Rank.UNPLACED
        return start > doc.manuscript.rankOf(lens)
    }
}
