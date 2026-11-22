package io.github.lenticualrstone.civilight.domain.story.text

/**
 * Sentence spans (STO-37): a sentence ends at `. ? ! … ⋯` (a run of them) followed by whitespace,
 * a closing quote or the end; closing quotes stay with the sentence. Texts of at most
 * [SHORT_BLOCK] characters are one sentence (`ED`: short blocks show whole).
 */
object Sentences {
    const val SHORT_BLOCK = 40

    private const val TERMINATORS = ".?!…⋯"
    private const val CLOSING = "”\"’'」』)"

    /** Every sentence of [text] as trimmed ranges, in order. */
    fun split(text: String): List<IntRange> {
        if (text.isBlank()) return emptyList()
        if (text.length <= SHORT_BLOCK) return listOfNotNull(trim(text, 0, text.length))
        val out = mutableListOf<IntRange>()
        var start = 0
        var i = 0
        while (i < text.length) {
            if (text[i] in TERMINATORS) {
                var j = i + 1
                while (j < text.length && text[j] in TERMINATORS) j++
                while (j < text.length && text[j] in CLOSING) j++
                if (j == text.length || text[j].isWhitespace()) {
                    trim(text, start, j)?.let(out::add)
                    start = j
                }
                i = j
                continue
            }
            if (text[i] == '\n') {
                trim(text, start, i)?.let(out::add)
                start = i + 1
            }
            i++
        }
        trim(text, start, text.length)?.let(out::add)
        return out
    }

    /** The sentence containing [at] (a character index), or the whole text. */
    fun around(text: String, at: Int): IntRange =
        split(text).firstOrNull { at in it } ?: split(text).lastOrNull { it.first <= at } ?: text.indices

    private fun trim(text: String, from: Int, to: Int): IntRange? {
        var a = from
        var b = to
        while (a < b && text[a].isWhitespace()) a++
        while (b > a && text[b - 1].isWhitespace()) b--
        return if (a < b) a until b else null
    }
}
