package io.github.lenticualrstone.civilight.feature.cast.field

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.ordered
import io.github.lenticualrstone.civilight.model.text.plainText

/**
 * Plain-text editing of a `긴 글` value (CST-20) that never loses what it cannot show: only a
 * document whose runs are all plain text is [editable] here (a paragraph with an `@` link, a paren
 * run or a soft break needs the rich editor of a later batch). Paragraphs whose text is unchanged
 * keep their block (key, memo kind, recorder source); a changed paragraph keeps its block's identity
 * when the paragraph count is unchanged.
 */
object LongTextEdit {
    /** Every run of every paragraph is plain text (no `@` link, paren run, break or unknown run). */
    fun editable(doc: RichDoc): Boolean = doc.all { block -> block.runs.all { it is TextRun.Plain } }

    /** The paragraphs of [text], trimmed, NFC, blank lines dropped. */
    fun paragraphs(text: String): List<String> = text.split('\n').map { it.trim().nfc() }.filter { it.isNotEmpty() }

    fun apply(original: RichDoc, text: String, ctx: EditContext): RichDoc {
        val paragraphs = paragraphs(text)
        if (paragraphs.isEmpty()) return emptyList()
        val before = original.ordered()
        val blocks: List<DocBlock> = if (before.size == paragraphs.size) {
            before.zip(paragraphs).map { (block, p) -> if (block.runs.plainText == p) block else block.copy(runs = listOf(TextRun.Plain(p))) }
        } else {
            var next = 0
            paragraphs.map { p ->
                val found = (next until before.size).firstOrNull { before[it].runs.plainText == p }
                if (found != null) {
                    next = found + 1
                    before[found]
                } else {
                    DocBlock(key = ctx.newKey(), order = "", runs = listOf(TextRun.Plain(p)))
                }
            }
        }
        val orders = OrderKeys.evenlySpaced(blocks.size)
        return blocks.mapIndexed { i, b -> b.copy(order = orders[i]) }
    }
}
