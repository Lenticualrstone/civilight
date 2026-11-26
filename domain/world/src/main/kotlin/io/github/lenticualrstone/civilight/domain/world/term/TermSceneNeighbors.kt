package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.domain.world.time.EventMasks
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Kinds the `같은 씬에 나온 것` chips show, in chip order (board `ImmContent`: 인물 · 장소 · 사건 · 세력). */
enum class NeighborKind(val label: String) {
    CHARACTER("인물"),
    PLACE("장소"),
    EVENT("사건"),
    FACTION("세력"),
}

/** One chip: the item, its live name and the number of the term's scenes it is linked in. */
data class SceneNeighbor(val kind: NeighborKind, val key: Key, val name: String, val scenes: Int)

/**
 * (2026-11-26) `같은 씬에 나온 것` on the term card (`연결` tab, board `ImmTermWhy`): characters, places, events and
 * factions that appear as `@` links in the game-output text of the scenes where the term itself is `@`-linked.
 * Only links count (rule 21: never text matches, speakers or the scene's place property). Left out: other terms,
 * links to another project, trashed or purged items, and while hidden items are locked hidden scenes (their
 * text is masked on the card), hidden items and events on a hidden scene. Script text allows no faction links today
 * ([LinkTargetKind.SCRIPT]), so faction chips appear only if a file carries one. Pure.
 */
object TermSceneNeighbors {
    fun of(doc: ProjectDoc, termKey: Key, hiddenOpen: Boolean): List<SceneNeighbor> {
        // Game-output spots of live scenes, grouped by scene in manuscript order.
        val byScene = LinkedHashMap<Key, MutableList<SpotText>>()
        for (spot in TextSpots.script(doc)) {
            val sceneKey = sceneOf(doc, spot.spot) ?: continue
            byScene.getOrPut(sceneKey) { ArrayList() } += spot
        }
        val firstSeen = LinkedHashMap<Pair<NeighborKind, Key>, Int>()
        val sceneCount = HashMap<Pair<NeighborKind, Key>, Int>()
        var order = 0
        for ((sceneKey, spots) in byScene) {
            if (!hiddenOpen && sceneHidden(doc, sceneKey)) continue
            val hasTerm = spots.any { s -> TermLinks.inSpot(s).any { it.termKey == termKey } }
            if (!hasTerm) continue
            val here = LinkedHashSet<Pair<NeighborKind, Key>>()
            for (s in spots) for (run in s.runs) {
                if (run !is TextRun.Link || run.target.projectKey != null) continue
                val kind = kindOf(run.target.kind.known) ?: continue
                here += kind to run.target.key
            }
            for (id in here) {
                if (id !in firstSeen) firstSeen[id] = order++
                sceneCount[id] = (sceneCount[id] ?: 0) + 1
            }
        }
        return firstSeen.keys
            .mapNotNull { id -> nameIfShown(doc, id.first, id.second, hiddenOpen)?.let { SceneNeighbor(id.first, id.second, it, sceneCount[id] ?: 0) } }
            .sortedWith(compareBy<SceneNeighbor>({ it.kind.ordinal }, { firstSeen[it.kind to it.key] ?: 0 }))
    }

    private fun kindOf(kind: LinkTargetKind?): NeighborKind? = when (kind) {
        LinkTargetKind.CHARACTER -> NeighborKind.CHARACTER
        LinkTargetKind.PLACE -> NeighborKind.PLACE
        LinkTargetKind.EVENT -> NeighborKind.EVENT
        LinkTargetKind.FACTION -> NeighborKind.FACTION
        else -> null
    }

    private fun sceneOf(doc: ProjectDoc, spot: TextSpot): Key? = when (spot) {
        is ScriptSpot.BlockText -> doc.index.block(spot.blockKey)?.sceneKey
        is ScriptSpot.OptionText -> doc.index.block(spot.blockKey)?.sceneKey
        is ScriptSpot.TransitionText -> spot.sceneKey
        else -> null
    }

    private fun sceneHidden(doc: ProjectDoc, sceneKey: Key): Boolean {
        val scene = doc.index.unit(sceneKey) ?: return false
        return scene.hidden || doc.index.ancestors(scene).any { it.hidden }
    }

    /** The live name, or null when the item is gone or hidden while locked. */
    private fun nameIfShown(doc: ProjectDoc, kind: NeighborKind, key: Key, hiddenOpen: Boolean): String? = when (kind) {
        NeighborKind.CHARACTER -> doc.index.character(key)?.takeIf { it.deletedAt == null && (hiddenOpen || !it.hidden) }?.name
        NeighborKind.PLACE -> doc.index.place(key)?.takeIf { it.deletedAt == null && (hiddenOpen || !it.hidden) }?.name
        NeighborKind.FACTION -> doc.index.faction(key)?.takeIf { it.deletedAt == null && (hiddenOpen || !it.hidden) }?.name
        // An event whose scene is hidden is masked as `숨긴 씬` on the timeline while locked (EventMasks).
        NeighborKind.EVENT -> doc.index.event(key)?.takeIf { it.deletedAt == null && (hiddenOpen || !EventMasks.sceneHidden(doc, it)) }?.title
    }?.takeIf { it.isNotBlank() }
}
