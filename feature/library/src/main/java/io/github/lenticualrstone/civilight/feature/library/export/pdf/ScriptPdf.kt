package io.github.lenticualrstone.civilight.feature.library.export.pdf

import io.github.lenticualrstone.civilight.domain.library.export.ExportLayer
import io.github.lenticualrstone.civilight.domain.library.export.ExportPlan
import io.github.lenticualrstone.civilight.domain.library.export.ExportSheets
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.domain.script.DirectionCues
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.SceneText
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** One printed piece of the 대본 PDF, in manuscript order. */
sealed interface PdfBlock {
    /** The cover: project name, the rating (when `내보내기 표지에 등급 표시` is on) and the range. */
    data class Cover(val title: String, val rating: String?, val range: String) : PdfBlock

    /** Scene head as a title line (R22 §6): `1-7 씬 3` over `씬 3 · 하층 거리 · 2일째 새벽`. */
    data class SceneTitle(val code: String, val head: String, val title: String?) : PdfBlock

    /** Speaker name above the line (LIB-43). */
    data class Dialogue(val speaker: String?, val text: String) : PdfBlock

    data class Narration(val text: String) : PdfBlock

    /** 연출 지시 (layer `연출`). */
    data class Direction(val text: String) : PdfBlock

    /** 작가 메모, recorder memos included (layer `작가 메모`, R23 §6). */
    data class Memo(val text: String) : PdfBlock

    data class System(val text: String) : PdfBlock

    /** A choice: one `▸` line per option, with its speaker when it is spoken. */
    data class Choice(val options: List<Dialogue>) : PdfBlock
}

/**
 * The 대본 PDF as data (LIB-43): built from the same [ExportPlan] the CSV and engine JSON use, so the
 * range and the hidden-scene `빼기` are decided once (DECISIONS §1). Display layers decide whether
 * 연출 지시, 괄호 지문 and 작가 메모 print. Voice IDs are never touched (LIB-46).
 */
object ScriptPdf {
    fun build(doc: ProjectDoc, plan: ExportPlan, rangeKeys: List<Key>, coverRating: Boolean): List<PdfBlock> {
        val layers = plan.layers.mapNotNull(ExportLayer::of).toSet()
        val parens = ExportLayer.PAREN in layers
        val names = plan.characters.associate { it.key to it.name }
        fun speaker(key: Key?): String? = key?.let { names[it] ?: Labels.speakerName(doc, it) }
        fun text(runs: List<TextRun>): String = Runs.exportText(runs, includeParens = parens)

        val times = StoryTimes.source(doc)
        val out = ArrayList<PdfBlock>()
        out += PdfBlock.Cover(
            title = doc.project.name,
            rating = if (coverRating) RatingLabels.badge(doc.project.rating) else null,
            range = ExportSheets.rangeLabel(doc, rangeKeys),
        )
        for (scene in plan.scenes) {
            out += PdfBlock.SceneTitle(scene.label, SceneText.headText(doc, scene.sceneKey, times.timeOf(scene.sceneKey)), scene.title?.takeIf { it.isNotBlank() })
            for (block in scene.blocks) {
                when (block.kind.known) {
                    BlockKind.DIALOGUE -> out += PdfBlock.Dialogue(speaker(block.speakerKey), text(block.runs))
                    BlockKind.NARRATION -> out += PdfBlock.Narration(text(block.runs))
                    BlockKind.DIRECTION -> if (ExportLayer.DIRECTION in layers && block.cues.isNotEmpty()) {
                        out += PdfBlock.Direction(DirectionCues.blockLine(doc, block.cues))
                    }
                    BlockKind.MEMO -> if (ExportLayer.MEMO in layers) out += PdfBlock.Memo(text(block.runs))
                    BlockKind.SYSTEM -> out += PdfBlock.System(text(block.runs))
                    BlockKind.CHOICE -> out += PdfBlock.Choice(
                        block.options.sortedBy { it.order }.map { PdfBlock.Dialogue(speaker(it.speakerKey), text(it.runs)) },
                    )
                    // A block of a kind this app does not know prints its text (§8.3: kept read-only).
                    null -> if (block.runs.isNotEmpty()) out += PdfBlock.Narration(text(block.runs))
                }
            }
        }
        return out.filterNot { it is PdfBlock.Narration && it.text.isBlank() || it is PdfBlock.Dialogue && it.text.isBlank() }
    }
}
