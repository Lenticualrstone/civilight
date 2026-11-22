package io.github.lenticualrstone.civilight.feature.library.export.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import io.github.lenticualrstone.civilight.designsystem.R
import java.io.ByteArrayOutputStream

/** Writes laid-out pages to PDF bytes (the app: Android's `PdfDocument`). */
fun interface PdfWriter {
    fun write(blocks: List<PdfBlock>): ByteArray
}

/**
 * The 대본 PDF on Android (LIB-43): [PdfLayout] pages drawn with `PdfDocument`, which embeds the
 * glyphs it uses. Fonts (`ED`, see Spec conflicts): the spec names Nanum Myeongjo and Noto Sans KR,
 * which this app does not ship; the body uses the system serif (Noto Serif CJK on most devices) and
 * labels the bundled Pretendard. Run it off the main thread.
 */
class AndroidPdfWriter(private val context: Context) : PdfWriter {
    private val sans: Typeface by lazy { runCatching { ResourcesCompat.getFont(context, R.font.pretendard_regular) }.getOrNull() ?: Typeface.SANS_SERIF }
    private val sansBold: Typeface by lazy { runCatching { ResourcesCompat.getFont(context, R.font.pretendard_semibold) }.getOrNull() ?: Typeface.DEFAULT_BOLD }

    private fun paint(style: PdfStyle): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = when {
            style.serif && style.bold -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            style.serif -> Typeface.SERIF
            style.bold -> sansBold
            else -> sans
        }
        textSize = style.sizePt
        color = if (style.grey) GREY else INK
    }

    override fun write(blocks: List<PdfBlock>): ByteArray {
        val paints = PdfStyle.entries.associateWith(::paint)
        val layout = PdfLayout({ text, style -> paints.getValue(style).measureText(text) })
        val spec = PageSpec()
        val document = PdfDocument()
        try {
            for (page in layout.pages(blocks)) {
                val info = PdfDocument.PageInfo.Builder(spec.width.toInt(), spec.height.toInt(), page.number).create()
                val pdfPage = document.startPage(info)
                for (line in page.lines) pdfPage.canvas.drawText(line.text, line.x, line.baseline, paints.getValue(line.style))
                document.finishPage(pdfPage)
            }
            return ByteArrayOutputStream().also { document.writeTo(it) }.toByteArray()
        } finally {
            document.close()
        }
    }

    private companion object {
        val INK = Color.rgb(0x16, 0x18, 0x1C)
        val GREY = Color.rgb(0x6B, 0x6E, 0x76)
    }
}
