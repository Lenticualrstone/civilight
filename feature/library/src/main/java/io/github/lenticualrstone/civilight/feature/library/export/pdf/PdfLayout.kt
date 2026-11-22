package io.github.lenticualrstone.civilight.feature.library.export.pdf

/** Text styles of the 대본 PDF (`ED`: A4, body 11 pt; LIB-43). */
enum class PdfStyle(val sizePt: Float, val leading: Float, val serif: Boolean, val bold: Boolean = false, val grey: Boolean = false) {
    COVER_TITLE(28f, 1.3f, serif = true, bold = true),
    COVER_RATING(12f, 1.4f, serif = false, bold = true),
    COVER_RANGE(11f, 1.5f, serif = false, grey = true),
    SCENE_CODE(9f, 1.5f, serif = false, grey = true),
    SCENE_HEAD(14f, 1.4f, serif = true, bold = true),
    SCENE_TITLE(11f, 1.5f, serif = false, grey = true),
    SPEAKER(9.5f, 1.4f, serif = false, bold = true, grey = true),
    BODY(11f, 1.7f, serif = true),
    DIRECTION(9f, 1.6f, serif = false, grey = true),
    MEMO(10f, 1.6f, serif = false, grey = true),
    PAGE_NUMBER(8f, 1.2f, serif = false, grey = true),
    ;

    val lineHeight: Float get() = sizePt * leading
}

/** Measures text width in points for a style (Android: `Paint.measureText`; tests: a fixed width). */
fun interface TextMeasure {
    fun width(text: String, style: PdfStyle): Float
}

/** One line placed on a page: [x] from the left edge, [baseline] from the top (points). */
data class PlacedLine(val text: String, val style: PdfStyle, val x: Float, val baseline: Float)

data class PdfPage(val number: Int, val lines: List<PlacedLine>)

/** A4 and the margins (`ED`). */
data class PageSpec(
    val width: Float = 595f,
    val height: Float = 842f,
    val marginX: Float = 64f,
    val marginTop: Float = 72f,
    val marginBottom: Float = 72f,
) {
    val textWidth: Float get() = width - 2 * marginX
}

/**
 * Pagination of the 대본 PDF, pure so it is tested on the JVM: the cover alone on page 1, then the
 * blocks flowing over A4 pages; lines break at spaces where possible (Korean breaks between
 * syllables otherwise), a speaker name never ends a page without its first line, and every page but
 * the cover carries its number at the foot.
 */
class PdfLayout(private val measure: TextMeasure, private val spec: PageSpec = PageSpec()) {
    fun pages(blocks: List<PdfBlock>): List<PdfPage> {
        val pages = ArrayList<PdfPage>()
        val cover = blocks.filterIsInstance<PdfBlock.Cover>().firstOrNull()
        if (cover != null) pages += coverPage(cover)
        var current = ArrayList<PlacedLine>()
        var y = spec.marginTop
        val bottom = spec.height - spec.marginBottom

        fun newPage() {
            pages += PdfPage(pages.size + 1, current + footer(pages.size + 1))
            current = ArrayList()
            y = spec.marginTop
        }

        /**
         * Places [lines]; the first [keep] of them, plus [reserve] points of what follows, must fit on
         * the page or the block starts on the next one.
         */
        fun place(lines: List<Pair<String, PdfStyle>>, indent: Float = 0f, spaceBefore: Float = 0f, keep: Int = 1, reserve: Float = 0f) {
            if (lines.isEmpty()) return
            val keepHeight = lines.take(keep).sumOf { it.second.lineHeight.toDouble() }.toFloat() + reserve
            if (current.isNotEmpty() && y + spaceBefore + keepHeight > bottom) newPage()
            if (current.isNotEmpty()) y += spaceBefore
            for ((text, style) in lines) {
                if (y + style.lineHeight > bottom) newPage()
                y += style.lineHeight
                current += PlacedLine(text, style, spec.marginX + indent, y - (style.lineHeight - style.sizePt) / 2)
            }
        }

        fun wrapped(text: String, style: PdfStyle, indent: Float = 0f): List<Pair<String, PdfStyle>> =
            text.split('\n').flatMap { wrap(it, style, spec.textWidth - indent) }.map { it to style }

        for (block in blocks) {
            when (block) {
                is PdfBlock.Cover -> Unit
                is PdfBlock.SceneTitle -> {
                    val lines = listOf(block.code to PdfStyle.SCENE_CODE) + wrapped(block.head, PdfStyle.SCENE_HEAD) +
                        (block.title?.let { wrapped(it, PdfStyle.SCENE_TITLE) } ?: emptyList())
                    // A scene head never sits alone at the foot of a page: room for a speaker and two lines.
                    place(lines, spaceBefore = 28f, keep = lines.size, reserve = 10f + PdfStyle.SPEAKER.lineHeight + 2 * PdfStyle.BODY.lineHeight)
                }
                is PdfBlock.Dialogue -> {
                    val body = wrapped(block.text, PdfStyle.BODY)
                    val head = block.speaker?.let { listOf(it to PdfStyle.SPEAKER) } ?: emptyList()
                    place(head + body, spaceBefore = 10f, keep = head.size + 1)
                }
                is PdfBlock.Narration -> place(wrapped(block.text, PdfStyle.BODY), spaceBefore = 10f)
                is PdfBlock.System -> place(wrapped(block.text, PdfStyle.BODY, INDENT), indent = INDENT, spaceBefore = 10f)
                is PdfBlock.Direction -> place(wrapped("▸ " + block.text, PdfStyle.DIRECTION), spaceBefore = 8f)
                is PdfBlock.Memo -> place(wrapped("⌁ " + block.text, PdfStyle.MEMO, INDENT), indent = INDENT, spaceBefore = 8f)
                is PdfBlock.Choice -> block.options.forEachIndexed { i, option ->
                    val head = option.speaker?.let { listOf(it to PdfStyle.SPEAKER) } ?: emptyList()
                    place(head + wrapped("▸ " + option.text, PdfStyle.BODY, INDENT), indent = INDENT, spaceBefore = if (i == 0) 10f else 4f, keep = head.size + 1)
                }
            }
        }
        if (current.isNotEmpty() || pages.size <= 1) newPage()
        return pages
    }

    private fun coverPage(cover: PdfBlock.Cover): PdfPage {
        val lines = ArrayList<PlacedLine>()
        var y = spec.height * 0.38f
        for (line in wrap(cover.title, PdfStyle.COVER_TITLE, spec.textWidth)) {
            y += PdfStyle.COVER_TITLE.lineHeight
            lines += PlacedLine(line, PdfStyle.COVER_TITLE, spec.marginX, y)
        }
        cover.rating?.let {
            y += PdfStyle.COVER_RATING.lineHeight + 8f
            lines += PlacedLine(it, PdfStyle.COVER_RATING, spec.marginX, y)
        }
        if (cover.range.isNotBlank()) {
            y += PdfStyle.COVER_RANGE.lineHeight + 4f
            lines += PlacedLine(cover.range, PdfStyle.COVER_RANGE, spec.marginX, y)
        }
        return PdfPage(1, lines)
    }

    private fun footer(number: Int): List<PlacedLine> {
        val text = number.toString()
        val x = (spec.width - measure.width(text, PdfStyle.PAGE_NUMBER)) / 2
        return listOf(PlacedLine(text, PdfStyle.PAGE_NUMBER, x, spec.height - spec.marginBottom / 2))
    }

    /** Greedy breaking: whole words while they fit, else as many characters as fit (Korean). */
    fun wrap(text: String, style: PdfStyle, width: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = ArrayList<String>()
        var rest = text
        while (rest.isNotEmpty()) {
            if (measure.width(rest, style) <= width) {
                lines += rest
                break
            }
            var fit = 1
            while (fit < rest.length && measure.width(rest.substring(0, fit + 1), style) <= width) fit++
            val space = rest.lastIndexOf(' ', fit).takeIf { it > 0 && it >= fit / 2 }
            val cut = if (space != null && measure.width(rest.substring(0, space), style) <= width) space else fit
            lines += rest.substring(0, cut).trimEnd()
            rest = rest.substring(cut).trimStart()
        }
        return lines
    }

    private companion object {
        const val INDENT = 14f
    }
}
