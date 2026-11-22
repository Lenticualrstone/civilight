package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * A row's sentence with the linked or matched span boxed inline (search-highlight rule: the whole
 * sentence, never the span on its own line). [boxStart]/[boxEnd] index into [text].
 */
data class Snippet(val speaker: String?, val text: String, val boxStart: Int, val boxEnd: Int) {
    /** `세라: “오리지늄은 타지 않아. 빛만 남지.”` (dialogue) or the sentence itself. */
    fun render(): String = speaker?.let { "$it: “$text”" } ?: text

    /** [render] with the box drawn as `[…]` (the spec's notation; for tests and accessibility text). */
    fun renderBoxed(): String {
        val boxed = text.substring(0, boxStart) + "[" + text.substring(boxStart, boxEnd) + "]" + text.substring(boxEnd)
        return speaker?.let { "$it: “$boxed”" } ?: boxed
    }

    val boxed: String get() = text.substring(boxStart, boxEnd)
}

/** Location labels and sentences of text spots. */
class SpotDescriber(private val doc: ProjectDoc, private val codes: StoryCodes = StoryCodes(doc)) {

    /** `1-4 씬 2` for manuscript text; `페이지 · 하층 구역의 빛`, `인물 · 세라` … outside it. */
    fun location(spot: TextSpot): String = when (spot) {
        is ScriptSpot.BlockText -> codes.sceneOfBlock(spot.blockKey).orEmpty()
        is ScriptSpot.OptionText -> codes.sceneOfBlock(spot.blockKey).orEmpty()
        is ScriptSpot.TransitionText -> codes.sceneLabel(spot.sceneKey).orEmpty()
        is OutsideSpot.MemoBlock -> codes.sceneOfBlock(spot.blockKey).orEmpty()
        is OutsideSpot.PageText -> "페이지${Copy.SEP}${doc.pages[spot.pageKey]?.name.orEmpty()}"
        is OutsideSpot.CharacterBody -> "인물${Copy.SEP}${doc.index.character(spot.characterKey)?.name.orEmpty()}"
        is OutsideSpot.VoiceLineText -> "인물${Copy.SEP}${doc.index.character(spot.characterKey)?.name.orEmpty()}"
        is OutsideSpot.ArchiveText -> "인물${Copy.SEP}${doc.index.character(spot.characterKey)?.name.orEmpty()}"
        is OutsideSpot.Description -> when (spot.kind) {
            DescribedKind.TERM -> "용어${Copy.SEP}${doc.index.term(spot.key)?.name.orEmpty()}"
            DescribedKind.PLACE -> "장소${Copy.SEP}${doc.index.place(spot.key)?.name.orEmpty()}"
            DescribedKind.FACTION -> "세력${Copy.SEP}${doc.index.faction(spot.key)?.name.orEmpty()}"
            DescribedKind.EVENT -> "사건${Copy.SEP}${doc.index.event(spot.key)?.title.orEmpty()}"
        }
        is OutsideSpot.FieldText -> when (spot.entityKind) {
            EntityKind.CHARACTER -> "인물"
            EntityKind.PLACE -> "장소"
            EntityKind.TERM -> "용어"
            EntityKind.FACTION -> "세력"
        } + Copy.SEP + doc.index.nameOf(spot.entityKind, spot.entityKey).orEmpty()
    }

    /** Speaker name of a dialogue block or a spoken option line. */
    fun speaker(spot: TextSpot): String? {
        val key = when (spot) {
            is ScriptSpot.BlockText -> doc.index.block(spot.blockKey)?.takeIf { it.kind.known == BlockKind.DIALOGUE }?.speakerKey
            is ScriptSpot.OptionText -> doc.index.block(spot.blockKey)?.options?.firstOrNull { it.key == spot.optionKey }?.speakerKey
            else -> null
        } ?: return null
        return doc.index.character(key)?.name
    }

    /** True for text shown whole as one line (dialogue and option lines). */
    private fun isLine(spot: TextSpot): Boolean = speaker(spot) != null || spot is ScriptSpot.OptionText

    /**
     * The sentence around [start, end) of [runs] (offsets in run coordinates). Dialogue and option
     * lines are shown whole with their speaker; other text shows the sentence holding the span.
     * `괄호 지문` runs are drawn in parentheses.
     */
    fun snippet(spot: TextSpot, runs: List<TextRun>, start: Int, end: Int): Snippet {
        val display = StringBuilder()
        var cursor = 0
        var boxStart = 0
        var boxEnd = 0
        // Half-open on the side that matters: a span starting at a run boundary belongs to the next
        // run, a span ending at a boundary to the previous one.
        fun mapInto(runStart: Int, displayStart: Int, len: Int) {
            if (start >= runStart && start < runStart + len) boxStart = displayStart + (start - runStart)
            if (end > runStart && end <= runStart + len) boxEnd = displayStart + (end - runStart)
        }
        for (run in runs) {
            val shown = run.shownText
            when (run) {
                is TextRun.Paren -> {
                    display.append('(')
                    mapInto(cursor, display.length, shown.length)
                    display.append(shown).append(") ")
                }
                is TextRun.Break -> {
                    mapInto(cursor, display.length, shown.length)
                    display.append(' ')
                }
                else -> {
                    mapInto(cursor, display.length, shown.length)
                    display.append(shown)
                }
            }
            cursor += shown.length
        }
        val text = display.toString()
        if (isLine(spot)) return trimmed(speaker(spot), text, boxStart, boxEnd)
        val (s, e) = sentenceAround(text, boxStart, boxEnd)
        return trimmed(null, text.substring(s, e), boxStart - s, boxEnd - s)
    }

    private fun trimmed(speaker: String?, text: String, boxStart: Int, boxEnd: Int): Snippet {
        val lead = text.length - text.trimStart().length
        val body = text.trim()
        return Snippet(speaker, body, (boxStart - lead).coerceIn(0, body.length), (boxEnd - lead).coerceIn(0, body.length))
    }

    /** Sentence bounds: a sentence ends after `.`, `?` or `!` followed by whitespace or the end. */
    private fun sentenceAround(text: String, boxStart: Int, boxEnd: Int): Pair<Int, Int> {
        fun endsAt(i: Int) = text[i] in ".?!" && (i + 1 >= text.length || text[i + 1].isWhitespace())
        var s = 0
        for (i in 0 until boxStart.coerceAtMost(text.length)) if (endsAt(i)) s = i + 1
        var e = text.length
        for (i in boxEnd.coerceAtLeast(0) until text.length) if (endsAt(i)) { e = i + 1; break }
        return s to e
    }
}
