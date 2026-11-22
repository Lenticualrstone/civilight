package io.github.lenticualrstone.civilight.domain.library.export

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.VoiceExport
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState

/**
 * Voice-ID fixation on export (LIB-46, R12 §6). After a CSV or JSON file is written, every line in
 * it whose voice ID was `자동` becomes `고정` with that ID and the export time; the app never changes
 * it afterwards. A CSV export also records the text hash and speaker it wrote (`lastExport`, the
 * input of the next CSV's `바뀜`). Lines not written (out of range, `빼기`) are not touched; PDF and
 * `.civilight` change nothing.
 */
object VoiceFixation {
    /**
     * [doc] after writing [plan] at [at]. [voiceIdOf] gives the ID each line was written with (the
     * same function the CSV/JSON writer used); lines without an ID are skipped. Nothing happens when
     * the project has voice IDs turned off.
     */
    fun apply(doc: ProjectDoc, plan: ExportPlan, at: Timestamp, voiceIdOf: (WrittenLine) -> String?): ProjectDoc {
        if (!plan.fixesVoiceIds || !doc.project.script.voiceIdEnabled) return doc
        val csv = plan.format == ExportFormat.CSV
        val byBlock: Map<Key, List<WrittenLine>> = plan.writtenLines.groupBy { it.blockKey }
        if (byBlock.isEmpty()) return doc

        fun fixed(line: WrittenLine): VoiceState? {
            val id = CsvRows.voiceIdFor(line, voiceIdOf) ?: return null
            val current = line.voice ?: VoiceState()
            val isFixed = current.mode.known == VoiceMode.FIXED && current.fixedId != null
            val base = if (isFixed) current else current.copy(mode = VoiceMode.FIXED.open(), fixedId = id, fixedAt = at)
            return if (csv) base.copy(lastExport = VoiceExport(at, CsvRows.textHash(line.runs), line.speakerKey)) else base
        }

        fun update(block: Block): Block {
            val lines = byBlock[block.key] ?: return block
            var out = block
            for (line in lines) {
                val voice = fixed(line) ?: continue
                out = if (line.optionKey == null) {
                    out.copy(voice = voice)
                } else {
                    out.copy(options = out.options.map { if (it.key == line.optionKey) it.copy(voice = voice) else it })
                }
            }
            return out
        }

        return doc.copy(episodes = doc.episodes.mapValues { (_, file) -> file.copy(blocks = file.blocks.map(::update)) })
    }
}
