package io.github.lenticualrstone.civilight.model.game

import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportRecord
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState

/** (0.2.0, G-1 … G-7) `게임 제작 도구` read helper for every surface. */
object GameTools {
    /**
     * True when the project's switch is on. An absent value (a 0.1.0 project not yet loaded through
     * [GameToolsUpgrade]) reads off; UI code never sees one, because every load path resolves it.
     */
    fun on(project: Project): Boolean = project.gameTools == true

    fun on(doc: ProjectDoc): Boolean = on(doc.project)
}

/**
 * (0.2.0, D6, 00 §13.2) Whether a project holds game data, live **or in the trash**. Auto voice IDs
 * alone never count (every 0.1.0 project has them).
 */
object GameData {
    fun present(doc: ProjectDoc, exportRecords: List<ExportRecord> = doc.app.activity.exportRecords): Boolean {
        // 1, 3, 6: block voice state, direction cues, stage values (live and trashed blocks).
        if (doc.allBlocks.any(::blockHasGameData)) return true
        // 2: discarded voice IDs (episode files and the tolerated story-index copy).
        if (doc.story.discardedVoiceIds.isNotEmpty()) return true
        if (doc.episodes.values.any { it.discardedVoiceIds.isNotEmpty() }) return true
        // 4: script assets.
        if (doc.assets.scriptAssets.isNotEmpty()) return true
        // 5: 게임 텍스트 rows.
        if (doc.cast.voiceLines.isNotEmpty() || doc.cast.archiveDocs.isNotEmpty()) return true
        // 7: a CSV or engine JSON export on this device.
        return exportRecords.any { it.format == ExportFormat.CSV || it.format == ExportFormat.ENGINE_JSON }
    }

    private fun blockHasGameData(block: Block): Boolean {
        if (voiceCounts(block.voice)) return true
        if (block.options.any { voiceCounts(it.voice) }) return true
        if (block.cues.isNotEmpty()) return true
        return block.kind.known == BlockKind.DIALOGUE && block.stage != null
    }

    /** Fixed, hand-typed or ever exported; an automatic state alone does not count. */
    private fun voiceCounts(voice: VoiceState?): Boolean =
        voice != null && (voice.mode.known == VoiceMode.FIXED || voice.manualId != null || voice.lastExport != null)
}

/**
 * (0.2.0, G-3) The one-time upgrade step: while `project.gameTools` is absent, it becomes
 * [GameData.present]. Runs on a working-store load and on every `.civilight` read (import, open,
 * restore); after that the stored value is the user's and this returns the document unchanged.
 */
object GameToolsUpgrade {
    /** True when [doc] still needs the step. */
    fun needed(doc: ProjectDoc): Boolean = doc.project.gameTools == null

    /** [doc] with the switch decided, or [doc] itself when it is already decided. */
    fun resolve(doc: ProjectDoc, exportRecords: List<ExportRecord> = doc.app.activity.exportRecords): ProjectDoc {
        if (!needed(doc)) return doc
        return doc.copy(project = doc.project.copy(gameTools = GameData.present(doc, exportRecords)))
    }
}
