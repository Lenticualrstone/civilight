package io.github.lenticualrstone.civilight.designsystem.text

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * Draws a marker into a square of side `size.minDimension` at the top-left of this scope, in the 14-unit box.
 * Vector shapes (DS-16 / DS-17 proposed): diamond `M7 1.5L12.5 7 7 12.5 1.5 7z`, triangle, a right-pointing
 * triangle for `▸`, an eighth note for `♪`, a zig-zag bolt for `⌁`.
 */
fun DrawScope.drawMarker(kind: MarkerKind, style: MarkerStyle) {
    val side = size.minDimension
    val k = side / MARKER_BOX_UNITS
    scale(k, k, pivot = Offset.Zero) {
        val strokeEffect = if (style.dashed) PathEffect.dashPathEffect(floatArrayOf(style.dashOnUnits, style.dashOffUnits)) else null
        val stroke = Stroke(width = style.strokeUnits, cap = if (style.dashed) StrokeCap.Butt else StrokeCap.Round, join = StrokeJoin.Round, pathEffect = strokeEffect)
        fun Path.paint() = if (style.filled) drawPath(this, style.color, style = Fill) else drawPath(this, style.color, style = stroke)
        when (kind) {
            MarkerKind.CHANGE, MarkerKind.NOTICE -> Path().apply {
                moveTo(7f, 1.5f); lineTo(12.5f, 7f); lineTo(7f, 12.5f); lineTo(1.5f, 7f); close()
            }.paint()
            MarkerKind.WARNING -> Path().apply {
                moveTo(7f, 1.6f); lineTo(13f, 12.4f); lineTo(1f, 12.4f); close()
            }.paint()
            MarkerKind.DIRECTION -> Path().apply {
                moveTo(4.2f, 2.8f); lineTo(11.4f, 7f); lineTo(4.2f, 11.2f); close()
            }.paint()
            MarkerKind.BGM -> {
                val note = Stroke(width = 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = strokeEffect)
                drawPath(Path().apply { moveTo(9.2f, 10.6f); lineTo(9.2f, 1.8f) }, style.color, style = note)
                drawPath(
                    Path().apply {
                        moveTo(9.2f, 1.8f)
                        cubicTo(10.2f, 3.4f, 12.6f, 4.0f, 12.2f, 6.8f)
                    },
                    style.color,
                    style = note,
                )
                rotate(-20f, pivot = Offset(7f, 10.8f)) {
                    if (style.filled) {
                        drawOval(style.color, topLeft = Offset(4.6f, 9.1f), size = Size(4.8f, 3.4f))
                    } else {
                        drawOval(style.color, topLeft = Offset(4.6f, 9.1f), size = Size(4.8f, 3.4f), style = note)
                    }
                }
            }
            MarkerKind.MEMO -> drawPath(
                Path().apply { moveTo(1.2f, 9f); lineTo(5f, 5f); lineTo(9f, 9f); lineTo(12.8f, 5f) },
                style.color,
                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = strokeEffect),
            )
        }
    }
}

/** A stand-alone marker glyph (e.g. at the right end of a speaker line). */
@Composable
fun MarkerGlyph(
    kind: MarkerKind,
    modifier: Modifier = Modifier,
    line: MarkerLine = MarkerLine.SOLID,
    size: Dp = 12.dp,
    colors: CivilightColors = CivilightTheme.colors,
    deletedLabel: String = LABEL_ANCHOR_DELETED,
) {
    val style = markerStyle(kind, line, colors, deletedLabel)
    val label = style.accessibilityLabel
    Canvas(
        modifier
            .size(size)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
    ) { drawMarker(kind, style) }
}

private fun inlineId(kind: MarkerKind, line: MarkerLine) = "civilight.marker.${kind.name}.${line.name}"

/**
 * Replaces every marker character of [text] with an inline vector placeholder (size 0.75 em, advance
 * 1 em), keeping the characters as alternate text and every span. [dotted] holds character indices drawn
 * with the dotted line.
 */
fun withMarkerGlyphs(text: AnnotatedString, dotted: Set<Int> = emptySet()): AnnotatedString {
    if (!hasMarker(text)) return text
    val raw = text.text
    return buildAnnotatedString {
        var segmentStart = 0
        raw.forEachIndexed { i, c ->
            val kind = MarkerKind.of(c) ?: return@forEachIndexed
            if (i > segmentStart) append(raw.substring(segmentStart, i))
            val line = if (i in dotted) MarkerLine.DOTTED else MarkerLine.SOLID
            appendInlineContent(inlineId(kind, line), c.toString())
            segmentStart = i + 1
        }
        if (segmentStart < raw.length) append(raw.substring(segmentStart))
        // Offsets are unchanged (alternate text is the one marker character), so spans carry over as is.
        text.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
        text.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
        text.getStringAnnotations(0, raw.length).forEach { addStringAnnotation(it.tag, it.item, it.start, it.end) }
    }
}

/** Inline content for every marker × line, drawn in the given colours. */
fun markerInlineContent(colors: CivilightColors): Map<String, InlineTextContent> =
    MarkerKind.entries.flatMap { kind -> MarkerLine.entries.map { kind to it } }.associate { (kind, line) ->
        inlineId(kind, line) to InlineTextContent(
            Placeholder(width = 1.em, height = 0.75.em, placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline),
        ) {
            val style = markerStyle(kind, line, colors)
            Canvas(Modifier.fillMaxSize()) {
                val side = size.height
                translate(left = (size.width - side) / 2f) { drawMarker(kind, style) }
            }
        }
    }
