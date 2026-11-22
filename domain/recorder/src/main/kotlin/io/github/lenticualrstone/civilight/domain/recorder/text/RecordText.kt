package io.github.lenticualrstone.civilight.domain.recorder.text

import io.github.lenticualrstone.civilight.model.nfc

/** Text cuts used by 넣기 (LRC-36, LRC-37, LRC-38). */
object RecordText {
    /** Longest page-name prefill (`ED` LRC-36). */
    const val PAGE_NAME_MAX = 30

    private val TERMINATOR = Regex("""[.?!]""")

    /**
     * The first sentence with its terminator (`한 줄 소개` of `인물로 만들기`, LRC-T27): up to the first
     * `.`, `?` or `!`, else the first line; trimmed.
     */
    fun firstSentence(text: String): String {
        val t = text.trim().nfc()
        val line = t.lineSequence().firstOrNull().orEmpty()
        val end = TERMINATOR.find(line)?.range?.last
        return (if (end != null) line.substring(0, end + 1) else line).trim()
    }

    /**
     * The `이름` prefill of `페이지로 만들기` (`ED` LRC-36): the first sentence without its
     * terminator, trimmed, cut to [PAGE_NAME_MAX] characters.
     */
    fun pageNamePrefill(text: String): String {
        val line = text.trim().nfc().lineSequence().firstOrNull().orEmpty()
        val end = TERMINATOR.find(line)?.range?.first
        val sentence = (if (end != null) line.substring(0, end) else line).trim()
        return if (sentence.length <= PAGE_NAME_MAX) sentence else sentence.substring(0, PAGE_NAME_MAX).trim()
    }

    /** Record text as one line (`메모 칸에 넣기`, [PROPOSED] LRC-37): line breaks become spaces, runs of spaces collapse. */
    fun oneLine(text: String): String = text.nfc().replace(Regex("""\s+"""), " ").trim()

    /** The first line of the text (start-material rows, LIB-12). */
    fun firstLine(text: String): String = text.trim().lineSequence().firstOrNull().orEmpty().trim()
}
