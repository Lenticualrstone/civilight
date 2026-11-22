package io.github.lenticualrstone.civilight.domain.story.text

import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.text.BreakIterator

/** Game-output text of blocks: what search, suggestions and char counts read (STO-06, STO-34, STO-37). */
object BlockText {
    /** True for blocks whose text reaches the player (대사 · 나레이션 · 선택지 · 시스템 텍스트). */
    fun isGameOutput(block: Block): Boolean = when (block.kind.known) {
        BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM, BlockKind.CHOICE -> true
        BlockKind.DIRECTION, BlockKind.MEMO -> false
        null -> true
    }

    /** Runs as drawn: `괄호 지문` in parentheses followed by a space, breaks as new lines. */
    fun display(runs: List<TextRun>): String = buildString {
        for ((i, run) in runs.withIndex()) {
            when (run) {
                is TextRun.Paren -> {
                    append('(').append(run.text).append(')')
                    val next = runs.getOrNull(i + 1)?.shownText
                    if (next != null && next.isNotEmpty() && !next.first().isWhitespace()) append(' ')
                }
                else -> append(run.shownText)
            }
        }
    }

    /** The drawn game-output text of a block (choice options one per line), or null for a non-output block. */
    fun gameText(block: Block): String? {
        if (!isGameOutput(block)) return null
        return if (block.kind.known == BlockKind.CHOICE) {
            block.options.sortedBy { it.order }.joinToString("\n") { display(it.runs) }
        } else {
            display(block.runs)
        }
    }

    /**
     * `N자` of a block (STO-06 `ED`): Unicode grapheme clusters of its game-output text, spaces
     * included, 괄호 지문 text included without the brackets, breaks and non-output blocks excluded.
     */
    fun charCount(block: Block): Int {
        if (!isGameOutput(block)) return 0
        val runs = if (block.kind.known == BlockKind.CHOICE) block.options.flatMap { it.runs } else block.runs
        return runs.filter { it !is TextRun.Break }.sumOf { graphemes(it.shownText) }
    }

    /** Number of grapheme clusters in [text]. */
    fun graphemes(text: String): Int {
        if (text.isEmpty()) return 0
        val it = BreakIterator.getCharacterInstance()
        it.setText(text)
        var n = 0
        while (it.next() != BreakIterator.DONE) n++
        return n
    }
}
