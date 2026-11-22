package io.github.lenticualrstone.civilight.feature.story.common

/**
 * The chapter numeral (`Ⅰ 1장 · 등불지기`, STO-04). QA 2026-11-23 #13 saw the roman glyph `Ⅰ` in the sans face as a bare bar
 * (the font file was not inspected), so a leading `Ⅰ` … `Ⅻ` is drawn as its Latin letters in the serif face, where
 * `I` has its serifs. The text itself (and every test of it) stays the spec string.
 */
object ChapterGlyph {
    private const val FIRST = 0x2160
    private val LATIN = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")

    /** `Ⅱ 2장 · 제목` → (`II`, ` 2장 · 제목`); null when the text does not start with a roman glyph. */
    fun split(text: String): Pair<String, String>? {
        val head = text.firstOrNull() ?: return null
        val index = head.code - FIRST
        if (index !in LATIN.indices) return null
        return LATIN[index] to text.substring(1)
    }
}
