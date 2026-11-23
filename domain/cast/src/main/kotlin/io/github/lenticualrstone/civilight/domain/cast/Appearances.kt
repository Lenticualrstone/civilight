package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** One bar of CST-71: an episode ([label] `1-4`) and the number of its scenes the character appeared in. */
data class EpisodeBar(val episodeKey: Key, val label: String, val scenes: Int)

/** `마지막 등장 | 1-4`: the last scene in manuscript order where the character appeared, and its episode. */
data class LastAppearance(val sceneKey: Key, val episodeLabel: String)

/** The appearance section of a character (CST-71). [bars] is empty when the project has no episode. */
data class AppearanceBars(val bars: List<EpisodeBar>, val last: LastAppearance?) {
    /** The largest bar (heights are proportional to it); 0 when the character never appeared. */
    val max: Int get() = bars.maxOfOrNull { it.scenes } ?: 0

    /** A bar's height share in 0..1. */
    fun share(bar: EpisodeBar): Float = if (max == 0) 0f else bar.scenes.toFloat() / max
}

/**
 * (0.2.0, N-47, CST-71, D23) Where characters appeared, derived and never stored. A character appeared
 * in a live scene when a live block of it has the character as its speaker (a choice option's speaker
 * too; a per-block display name keeps the original speaker), or holds an `@` link to it in game-output
 * text: dialogue, narration, system text or a choice option line. Memos, direction blocks, the silent
 * `등장` list, pages and profiles do not count. Trashed scenes and blocks never count; hidden scenes
 * count only while hidden items are open, so no number reveals them. Independent of the lens.
 */
object Appearances {
    private val GAME_TEXT = setOf(BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM, BlockKind.CHOICE)

    /** Characters a block makes appear. */
    fun charactersOf(block: Block): Set<Key> {
        val out = HashSet<Key>()
        block.speakerKey?.let(out::add)
        block.options.forEach { o -> o.speakerKey?.let(out::add) }
        if (block.kind.known in GAME_TEXT) {
            linkedCharacters(block.runs, out)
            block.options.forEach { linkedCharacters(it.runs, out) }
        }
        return out
    }

    private fun linkedCharacters(runs: List<TextRun>, into: MutableSet<Key>) {
        for (run in runs) if (run is TextRun.Link && run.target.kind.known == LinkTargetKind.CHARACTER) into += run.target.key
    }

    /** Live scenes counted under [hiddenUnlocked], in manuscript order. */
    private fun countedScenes(doc: ProjectDoc, hiddenUnlocked: Boolean): List<StoryUnit> =
        doc.manuscript.liveScenes.filter { hiddenUnlocked || !(it.hidden || doc.index.ancestors(it).any { a -> a.hidden }) }

    /** Character → the counted scenes (manuscript order) it appeared in. */
    fun scenesByCharacter(doc: ProjectDoc, hiddenUnlocked: Boolean): Map<Key, List<StoryUnit>> {
        val out = LinkedHashMap<Key, MutableList<StoryUnit>>()
        for (scene in countedScenes(doc, hiddenUnlocked)) {
            val here = HashSet<Key>()
            for (block in doc.manuscript.liveBlocksOf(scene.key)) here += charactersOf(block)
            for (key in here) out.getOrPut(key) { ArrayList() } += scene
        }
        return out
    }

    /** Live episodes in manuscript order (episodes after the 집필 선 included, with no content). */
    fun episodes(doc: ProjectDoc): List<StoryUnit> {
        val level = doc.project.structure.episodeLevel ?: return emptyList()
        val manuscript = doc.manuscript
        return doc.story.units
            .filter { it.level == level && doc.liveness.isUnitLive(it.key) }
            .sortedBy { manuscript.rankOf(Position.Start(it.key)) ?: Rank.UNPLACED }
    }

    /** The bars and `마지막 등장` of [characterKey]. */
    fun of(doc: ProjectDoc, characterKey: Key, hiddenUnlocked: Boolean): AppearanceBars {
        val scenes = scenesByCharacter(doc, hiddenUnlocked)[characterKey].orEmpty()
        val codes = UnitCodes(doc)
        val perEpisode = scenes.groupingBy { codes.episodeOf(it)?.key }.eachCount()
        val bars = episodes(doc).map { e -> EpisodeBar(e.key, codes.episodeCode(e), perEpisode[e.key] ?: 0) }
        val last = scenes.lastOrNull()?.let { s ->
            LastAppearance(s.key, codes.episodeOf(s)?.let(codes::episodeCode) ?: StoryLabels.episodeCodeOf(doc, s.key).orEmpty())
        }
        return AppearanceBars(bars, last)
    }

    /**
     * (CST-69) Each character's last appearance as a manuscript scene index (0-based among live scenes);
     * absent when the character never appeared.
     */
    fun lastPositions(doc: ProjectDoc, hiddenUnlocked: Boolean): Map<Key, Int> =
        scenesByCharacter(doc, hiddenUnlocked).mapNotNull { (key, scenes) ->
            scenes.lastOrNull()?.let { doc.manuscript.sceneIndex(it.key) }?.let { key to it }
        }.toMap()

    /**
     * `오래 안 나옴` (CST-69): characters with an appearance first, the earliest last appearance first;
     * ties keep [characters]' order; characters that never appeared come after all others, in order.
     * The stored order never changes.
     */
    fun sortAbsent(characters: List<Character>, last: Map<Key, Int>): List<Character> {
        val (seen, never) = characters.withIndex().partition { last.containsKey(it.value.key) }
        return seen.sortedWith(compareBy({ last.getValue(it.value.key) }, { it.index })).map { it.value } + never.map { it.value }
    }

    /** [sortAbsent] over keys. */
    fun sortAbsentKeys(keys: List<Key>, last: Map<Key, Int>): List<Key> {
        val (seen, never) = keys.withIndex().partition { last.containsKey(it.value) }
        return seen.sortedWith(compareBy({ last.getValue(it.value) }, { it.index })).map { it.value } + never.map { it.value }
    }
}
