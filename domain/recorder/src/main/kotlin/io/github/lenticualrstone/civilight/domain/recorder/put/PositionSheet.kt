package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.Targets
import io.github.lenticualrstone.civilight.domain.recorder.link.UnitLabels
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementLabels
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementStatus
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/** One row of the `원고에 넣기` position sheet: facts only, none preselected (rule 23 §4). */
sealed interface PositionRow {
    val label: String

    /** `연결된 씬 · 1-7 씬 3 · 맨 끝` → [ManuscriptSpot.SceneEnd]. */
    data class LinkedScene(val sceneKey: Key, override val label: String) : PositionRow

    /** `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤` → [ManuscriptSpot.AfterBlock]. */
    data class LastWriting(val blockKey: Key, override val label: String) : PositionRow

    /** `씬 고르기 ›`: the structure (장 › 에피소드 › 씬); a chosen scene gets the memo at its end. */
    data object PickScene : PositionRow {
        override val label: String = RecorderCopy.PICK_SCENE
    }

    /** `이미 넣음 · 1-7 씬 3 · #062`: informational; tap opens the script at that block. */
    data class AlreadyPut(val blockKey: Key, override val label: String) : PositionRow

    /** Where tapping the row inserts, or null for rows that do not insert. */
    val spot: ManuscriptSpot?
        get() = when (this) {
            is LinkedScene -> ManuscriptSpot.SceneEnd(sceneKey)
            is LastWriting -> ManuscriptSpot.AfterBlock(blockKey)
            else -> null
        }
}

/** The sheet: title `원고에 넣기 · <project> ▾` and its rows. Nothing is inserted until a row is tapped. */
data class PositionSheet(val title: String, val rows: List<PositionRow>)

/** `원고에 넣기` position sheet (LRC-33) and `여기 넣기` (LRC-34). */
object PositionSheets {
    /**
     * The initial destination: the record's only linked project (explicit or implied), else null
     * and the project menu opens first ([PROPOSED]).
     */
    fun initialProject(library: RecorderLibrary, recordKey: Key, env: RecorderEnv): Key? =
        RecordStates.impliedProjects(library, recordKey, env).singleOrNull()

    fun of(library: RecorderLibrary, recordKey: Key, projectKey: Key, env: RecorderEnv): PositionSheet {
        val project = env.project(projectKey) ?: return PositionSheet(RecorderCopy.PUT_MANUSCRIPT, emptyList())
        val doc = project.doc
        val labels = UnitLabels(doc)
        val sep = RecorderCopy.SEP
        val rows = mutableListOf<PositionRow>()

        val linkedScenes = library.linksOf(recordKey)
            .filter { it.projectKey == projectKey && it.targetKind.known == RecorderTargetKind.UNIT }
            .mapNotNull { doc.index.unit(it.targetKey) }
            .filter { labels.isScene(it) && Targets.isOfferable(env, TargetRef(projectKey, RecorderTargetKind.UNIT, it.key)) }
        val sceneOrder = doc.index.allScenesInOrder.map { it.key }
        for (scene in linkedScenes.sortedBy { sceneOrder.indexOf(it.key) }) {
            rows += PositionRow.LinkedScene(
                scene.key, "${RecorderCopy.LINKED_SCENE}$sep${labels.sceneRef(scene)}$sep${RecorderCopy.SCENE_END}",
            )
        }

        lastWritingBlock(doc)?.let { block ->
            val scene = doc.index.unit(block.sceneKey)!!
            rows += PositionRow.LastWriting(
                block.key,
                "${RecorderCopy.LAST_WRITING}$sep${labels.sceneRef(scene)}$sep${PlacementLabels.number(block.number)} ${RecorderCopy.AFTER}",
            )
        }

        rows += PositionRow.PickScene

        library.placementsOf(recordKey)
            .filter { it.projectKey == projectKey && it.kind.known == PlacementKind.SCRIPT_BLOCK }
            .filter { RecordStates.placementStatus(it, env) == PlacementStatus.ACTIVE }
            .sortedBy { it.createdAt }
            .forEach { p ->
                val label = PlacementLabels.current(p, doc) ?: p.lastLabel
                rows += PositionRow.AlreadyPut(p.targetKey, "${RecorderCopy.ALREADY_PUT}$sep$label")
            }
        return PositionSheet("${RecorderCopy.PUT_MANUSCRIPT}$sep${Destinations.label(project, env)}${RecorderCopy.DROPDOWN}", rows)
    }

    /** The project's last caret block (SCR-59), when it still exists and is live. */
    fun lastWritingBlock(doc: ProjectDoc) = doc.app.activity.activity.lastEditedBlockKey
        ?.let(doc.index::block)
        ?.takeIf { doc.liveness.isLive(it) && doc.index.unit(it.sceneKey) != null }

    /**
     * `여기 넣기 · #044 뒤`, only for a recording started from the script's command bar with a caret
     * block that is still live, never on the lock screen; else null.
     */
    fun putHereLabel(entry: EntryContext, env: RecorderEnv): String? {
        val block = putHereBlock(entry, env) ?: return null
        return "${RecorderCopy.PUT_HERE}${RecorderCopy.SEP}${PlacementLabels.number(block.number)} ${RecorderCopy.AFTER}"
    }

    fun putHereSpot(entry: EntryContext, env: RecorderEnv): ManuscriptSpot? =
        putHereBlock(entry, env)?.let { ManuscriptSpot.AfterBlock(it.key) }

    private fun putHereBlock(entry: EntryContext, env: RecorderEnv) = run {
        if (entry.surface != EntrySurface.SCRIPT || entry.onLockScreen) return@run null
        val projectKey = entry.projectKey ?: return@run null
        val blockKey = entry.caretBlockKey ?: return@run null
        val doc = env.project(projectKey)?.doc ?: return@run null
        doc.index.block(blockKey)?.takeIf { doc.liveness.isLive(it) }
    }
}
