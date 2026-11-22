package io.github.lenticualrstone.civilight.model.doc

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.app.UnknownFile
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.file.AssetsIndexFile
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.ChangesFile
import io.github.lenticualrstone.civilight.model.file.ChecksFile
import io.github.lenticualrstone.civilight.model.file.DeletionsFile
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.file.TombstonesFile
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.ManuscriptIndex
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * The whole in-memory project (§8.2): one immutable value; edits are pure functions
 * `ProjectDoc -> ProjectDoc` using `copy()`.
 *
 * Each property is one file unit (or a map of them), so a store can write only the units whose
 * value changed. Live and trashed rows are held together (trashed ones carry `deletedAt` on the
 * directly deleted row, §5.1); [ProjectFileSet.split] separates them into the normal files and the
 * `trash/` mirror. Blocks are grouped by numbering unit key ([episodes]).
 *
 * Lookups, liveness and the manuscript index are computed lazily per instance ([index],
 * [liveness], [manuscript]) and are never part of equality.
 */
data class ProjectDoc(
    val project: Project,
    val story: StoryIndexFile = StoryIndexFile(),
    /** Numbering unit key → that unit's blocks and discarded voice IDs. */
    val episodes: Map<Key, EpisodeFile> = emptyMap(),
    val cast: CastFile = CastFile(),
    val world: WorldFile = WorldFile(),
    val maps: Map<Key, WorldMap> = emptyMap(),
    val changes: ChangesFile = ChangesFile(),
    val checks: ChecksFile = ChecksFile(),
    val tombstones: TombstonesFile = TombstonesFile(),
    val deletions: DeletionsFile = DeletionsFile(),
    val canvases: Map<Key, Canvas> = emptyMap(),
    val pages: Map<Key, Page> = emptyMap(),
    val assets: AssetsIndexFile = AssetsIndexFile(),
    /** Trash rows of this project (`TrashEntry`). */
    val trash: TrashIndexFile = TrashIndexFile(),
    /** App-only state (`_app/…`). */
    val app: ProjectAppState = ProjectAppState(),
    /** Files of an imported package this version did not understand (§8.3), by path. */
    val unknownFiles: Map<String, UnknownFile> = emptyMap(),
) {
    /** Keyed lookups and tree navigation. */
    val index: ProjectIndex by lazy { ProjectIndex(this) }

    /** Derived liveness (§5.1). */
    val liveness: Liveness by lazy { Liveness(this) }

    /** Manuscript order and position ranks (§4.2–§4.6). */
    val manuscript: ManuscriptIndex by lazy {
        ManuscriptIndex.build(project.structure, story.units, allBlocks, world.events)
    }

    val key: Key get() = project.key
    val units: List<StoryUnit> get() = story.units
    val characters: List<Character> get() = cast.characters
    val records: List<ChangeRecord> get() = changes.records

    /** Every block of every numbering unit (live and trashed). */
    val allBlocks: List<Block> by lazy { episodes.values.flatMap { it.blocks } }

    /**
     * The same project with every block filed under the numbering unit of its scene (§4.1), e.g.
     * after a block or scene moved to another episode. Discarded voice IDs stay where they are.
     * Blocks whose scene is unknown keep their current group.
     */
    fun regroupBlocks(): ProjectDoc {
        val target = LinkedHashMap<Key, MutableList<Block>>()
        for ((groupKey, file) in episodes) {
            for (block in file.blocks) {
                val unitKey = index.numberingUnitKeyOfScene(block.sceneKey) ?: groupKey
                target.getOrPut(unitKey) { mutableListOf() } += block
            }
        }
        val keys = (episodes.keys + target.keys).toSet()
        val regrouped = keys.associateWith { key ->
            val old = episodes[key] ?: EpisodeFile(unitKey = key)
            old.copy(blocks = target[key].orEmpty())
        }
        return copy(episodes = regrouped)
    }
}
