package io.github.lenticualrstone.civilight.designsystem.text

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/** One drawn piece of a range on one line. */
private data class LineSegment(val left: Float, val right: Float, val baseline: Float)

private fun TextLayoutResult.segments(start: Int, end: Int): List<LineSegment> {
    val len = layoutInput.text.length
    val s = start.coerceIn(0, len)
    val e = end.coerceIn(0, len)
    if (e <= s) return emptyList()
    val first = getLineForOffset(s)
    val last = getLineForOffset(e - 1)
    return (first..last).mapNotNull { line ->
        val segStart = max(s, getLineStart(line))
        val segEnd = min(e, getLineEnd(line, visibleEnd = true))
        if (segEnd <= segStart) return@mapNotNull null
        val a = getBoundingBox(segStart)
        val b = getBoundingBox(segEnd - 1)
        LineSegment(min(a.left, b.left), max(a.right, b.right), getLineBaseline(line))
    }
}

/**
 * The shared decorated-text primitive: link / candidate / dotted underlines and search boxes drawn from the
 * text layout per line (so the sentence wraps as normal text), marker characters drawn as vectors,
 * parentheticals in the `paren` role, and an optional blinking caret (DS-18, DS-19, DS-45).
 * Screen readers hear markers by name ([markerSpokenText]).
 */
@Composable
fun DecoratedText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = CivilightTheme.colors.ink,
    decorations: List<DecorationRange> = emptyList(),
    dottedMarkers: Set<Int> = emptySet(),
    caretOffset: Int? = null,
    caretHeight: Dp = 20.dp,
    revealBoxes: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val reduced = CivilightTheme.reducedMotion
    val styled = remember(text, decorations, colors, type, color) {
        buildAnnotatedString {
            append(text)
            decorations.forEach { d ->
                val ds = decorationStyle(d.kind, colors, color)
                val span = when (d.kind) {
                    InlineDecoration.PAREN -> SpanStyle(color = colors.ink3, fontSize = type.paren.fontSize)
                    else -> ds.textColor?.let { SpanStyle(color = it) }
                }
                val end = d.end.coerceAtMost(text.length)
                if (span != null && d.start < end) addStyle(span, d.start, end)
            }
        }
    }
    val display = remember(styled, dottedMarkers) { withMarkerGlyphs(styled, dottedMarkers) }
    val inline = remember(colors) { markerInlineContent(colors) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val boxes = decorations.filter { decorationStyle(it.kind, colors, color).box != null }
    val reveal = remember { Animatable(if (revealBoxes) 0f else Float.MAX_VALUE) }
    LaunchedEffect(revealBoxes, boxes.size) {
        if (revealBoxes) {
            reveal.snapTo(0f)
            val total = (boxes.size.coerceAtLeast(1)) * CivilightMotion.searchBoxStaggerMs +
                if (reduced) CivilightMotion.reducedFadeMs else CivilightMotion.standard.durationMs
            reveal.animateTo(boxes.size.toFloat() + 1f, tween(total, easing = LinearEasing))
        }
    }
    val caretVisible by produceState(true, caretOffset) {
        value = true
        if (caretOffset != null) {
            while (true) {
                delay(CivilightMotion.caretPeriodMs / 2L)
                value = !value
            }
        }
    }

    val spoken = remember(text) { if (hasMarker(text)) markerSpokenText(text.text) else null }
    BasicText(
        text = display,
        style = style.merge(TextStyle(color = color)),
        inlineContent = inline,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = {
            layout = it
            onTextLayout(it)
        },
        modifier = modifier
            .then(if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier)
            .drawBehind {
                val l = layout ?: return@drawBehind
                val fontPx = style.fontSize.toPx()
                boxes.forEachIndexed { i, d ->
                    val spec = decorationStyle(d.kind, colors, color).box ?: return@forEachIndexed
                    val alpha = (reveal.value - i).coerceIn(0f, 1f)
                    if (alpha <= 0f) return@forEachIndexed
                    l.segments(d.start, d.end).forEach { seg -> drawBox(seg, spec, fontPx, alpha) }
                }
            }
            .drawWithContent {
                drawContent()
                val l = layout ?: return@drawWithContent
                decorations.forEach { d ->
                    val u = decorationStyle(d.kind, colors, color).underline ?: return@forEach
                    l.segments(d.start, d.end).forEach { seg -> drawUnderline(seg, u) }
                }
                if (caretOffset != null && caretVisible) {
                    val offset = caretOffset.coerceIn(0, l.layoutInput.text.length)
                    val rect = l.getCursorRect(offset)
                    val line = l.getLineForOffset(offset)
                    val bottom = l.getLineBaseline(line) + 3.dp.toPx()
                    drawRect(colors.acc, Offset(rect.left, bottom - caretHeight.toPx()), Size(2.dp.toPx(), caretHeight.toPx()))
                }
            },
    )
}

private fun DrawScope.drawBox(seg: LineSegment, spec: BoxSpec, fontPx: Float, alpha: Float) {
    val padH = spec.padHorizontal.toPx()
    val padV = spec.padVertical.toPx()
    val top = seg.baseline - fontPx * ASCENT - padV
    val bottom = seg.baseline + fontPx * DESCENT + padV
    val topLeft = Offset(seg.left - padH, top)
    val size = Size(seg.right - seg.left + padH * 2, bottom - top)
    val r = CornerRadius(spec.radius.toPx())
    drawRoundRect(spec.fill, topLeft, size, r, alpha = alpha)
    spec.outline?.let {
        val w = spec.outlineWidth.toPx()
        drawRoundRect(
            it,
            Offset(topLeft.x + w / 2, topLeft.y + w / 2),
            Size(size.width - w, size.height - w),
            CornerRadius((spec.radius.toPx() - w / 2).coerceAtLeast(0f)),
            style = Stroke(w),
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawUnderline(seg: LineSegment, u: UnderlineSpec) {
    val t = u.thickness.toPx()
    val y = seg.baseline + u.offset.toPx() + t / 2
    drawLine(
        color = u.color,
        start = Offset(seg.left, y),
        end = Offset(seg.right, y),
        strokeWidth = t,
        cap = StrokeCap.Butt,
        pathEffect = if (u.dotted) PathEffect.dashPathEffect(floatArrayOf(u.dashOn.toPx(), u.dashOff.toPx())) else null,
    )
}

/** Pretendard ascent / descent as a fraction of the font size, for boxes tighter than the line height. */
private const val ASCENT = 0.92f
private const val DESCENT = 0.24f
