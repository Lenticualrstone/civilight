package io.github.lenticualrstone.civilight.domain.recorder.capture

import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * The live transcript (LRC-07, LRC-11): [confirmed] text the recognizer finalised (drawn `ink`)
 * and the unconfirmed [tail] (drawn `ink3`), followed by the caret.
 */
data class TranscriptView(val label: String, val confirmed: String, val tail: String) {
    val text: String get() = joined(confirmed, tail)

    companion object {
        /** [a] and [b] with one space between them when both are non-empty and [a] does not end in whitespace. */
        fun joined(a: String, b: String): String = when {
            a.isEmpty() -> b
            b.isEmpty() -> a
            a.last().isWhitespace() || b.first().isWhitespace() -> a + b
            else -> "$a $b"
        }
    }
}

/**
 * Dictation into runs. Dictated text is always plain: it never creates `@` runs or links by itself
 * (rule 23 §5). New text is appended after the existing text with a space.
 */
object Transcripts {
    fun appendFinal(runs: List<TextRun>, text: String): List<TextRun> {
        val t = text.nfc().trim()
        if (t.isEmpty()) return runs
        val existing = Runs.text(runs)
        val sep = if (existing.isEmpty() || existing.last().isWhitespace()) "" else " "
        return Runs.normalize(runs + TextRun.Plain(sep + t))
    }
}
