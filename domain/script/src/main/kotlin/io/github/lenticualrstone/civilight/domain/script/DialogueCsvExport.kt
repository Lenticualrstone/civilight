package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.script.VoiceState

/**
 * `대사 표 CSV` (SCR-64): columns `화자 · 대사 · 보이스 ID · 블록 ID · 상태`. Per scene in manuscript
 * order: its transition row (when it has text), then one row per dialogue block and spoken option;
 * every discarded ID of the covered episodes follows as a `버림` row. Writing it fixes the written IDs.
 */
object DialogueCsvExport {
    val HEADER: List<String> = listOf("화자", "대사", "보이스 ID", "블록 ID", "상태")
    const val NEW = "새"
    const val CHANGED = "바뀜"
    const val DISCARDED = "버림"
    const val TRANSITION_SPEAKER = "전환 문구"

    data class Row(val speaker: String, val line: String, val voiceId: String, val id: String, val state: String) {
        val cells: List<String> get() = listOf(speaker, line, voiceId, id, state)
    }

    data class Output(val rows: List<Row>, val doc: ProjectDoc) {
        /** CSV text (RFC 4180 quoting, CRLF, UTF-8 BOM by default so spreadsheet apps read Korean). */
        fun text(bom: Boolean = true): String = toCsv(rows, bom)
    }

    /**
     * Rows of the export and the doc with the written voice IDs fixed at [at]. [transitionLedger]
     * holds the text hash of each scene's transition at its previous export (the model stores no
     * ledger for transition text; empty = every transition row is `새`).
     */
    fun export(doc: ProjectDoc, scope: ExportScope, time: SceneTimeSource, at: Timestamp, transitionLedger: Map<Key, String> = emptyMap()): Output {
        val rows = rows(doc, scope, time, transitionLedger)
        val written = scope.scenes(doc).flatMap { doc.manuscript.liveBlocksOf(it.key) }.filter {
            it.kind.known == BlockKind.DIALOGUE || it.kind.known == BlockKind.CHOICE
        }
        return Output(rows, ExportVoices.fix(doc, written, at))
    }

    fun rows(doc: ProjectDoc, scope: ExportScope, time: SceneTimeSource, transitionLedger: Map<Key, String> = emptyMap()): List<Row> {
        val voices = ExportVoices.enabled(doc)
        val out = mutableListOf<Row>()
        val scenes = scope.scenes(doc)
        for (scene in scenes) {
            SceneText.transitionText(doc, scene, time.timeOf(scene.key))?.let { text ->
                val previous = transitionLedger[scene.key]
                val state = if (previous == null) NEW else if (previous != VoiceIds.textHash(text)) CHANGED else ""
                out += Row(TRANSITION_SPEAKER, text, "", SceneText.transitionId(scene.key), state)
            }
            for (b in doc.manuscript.liveBlocksOf(scene.key)) {
                when (b.kind.known) {
                    BlockKind.DIALOGUE -> out += Row(
                        speaker = b.displayNameOverride?.takeIf { it.isNotBlank() }
                            ?: b.speakerKey?.let { Manuscript.displayNameAt(doc, it, b.key) ?: Labels.speakerName(doc, it) }.orEmpty(),
                        line = Runs.exportText(b.runs, scope.includeParens),
                        voiceId = if (voices) VoiceIds.currentId(doc, b).orEmpty() else "",
                        id = b.key,
                        state = state(b.voice, VoiceIds.spokenText(b), b.speakerKey),
                    )
                    BlockKind.CHOICE -> for (o in BlockOps.orderedOptions(b).filter { it.speakerKey != null }) {
                        out += optionRow(doc, b, o, voices, scope)
                    }
                    else -> Unit
                }
            }
        }
        if (voices) {
            val units = scenes.map { doc.numberingUnitOfScene(it.key) }.toSet()
            for (unit in units) for (d in doc.index.discardedVoiceIds(unit)) {
                val block = doc.index.block(d.blockKey)
                out += Row(
                    speaker = block?.speakerKey?.let { Labels.speakerName(doc, it) }.orEmpty(),
                    line = block?.let { Runs.exportText(it.runs, scope.includeParens) }.orEmpty(),
                    voiceId = d.id, id = d.blockKey, state = DISCARDED,
                )
            }
        }
        return out
    }

    private fun optionRow(doc: ProjectDoc, b: Block, o: ChoiceOption, voices: Boolean, scope: ExportScope): Row = Row(
        speaker = o.speakerKey?.let { Labels.speakerName(doc, it) }.orEmpty(),
        line = Runs.exportText(o.runs, scope.includeParens),
        voiceId = if (voices) VoiceIds.currentIdOfOption(doc, b, o).orEmpty() else "",
        id = b.key,
        state = state(o.voice, VoiceIds.spokenText(b, o), o.speakerKey),
    )

    /**
     * `새` before the first export; `바뀜` when the text or the speaker changed since the last export
     * (Q-27 default); empty when unchanged.
     */
    fun state(voice: VoiceState?, text: String, speakerKey: Key?): String {
        val last = voice?.lastExport ?: return NEW
        return if (last.textHash != VoiceIds.textHash(text) || last.speakerKey != speakerKey) CHANGED else ""
    }

    /** RFC 4180: fields with a comma, quote or line break are quoted, quotes doubled; CRLF rows. */
    fun toCsv(rows: List<Row>, bom: Boolean = true): String = buildString {
        if (bom) append('﻿')
        for (r in listOf(HEADER) + rows.map { it.cells }) {
            append(r.joinToString(",") { escape(it) })
            append("\r\n")
        }
    }

    fun escape(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + cell.replace("\"", "\"\"") + "\"" else cell
}
