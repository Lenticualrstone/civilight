package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * (0.7.3) Sonnet-Create typography, speaker editorial palette, inline markdown styling,
 * dashed capsule outlines, and jitter-free `TypewriterText` with blinking accent caret.
 */
internal object SonnetScriptStyle {
    private val LIGHT_SPEAKER_PALETTE = listOf(
        Color(0xFF1F5590), // Civilight deep blue
        Color(0xFF3E5C50), // Pine
        Color(0xFF9E5A3C), // Terracotta
        Color(0xFF7F642A), // Warm gold
        Color(0xFF6B4C7A), // Muted plum
        Color(0xFF2D6B73), // Deep teal
    )

    private val DARK_SPEAKER_PALETTE = listOf(
        Color(0xFF8FB8E6), // Civilight sky blue
        Color(0xFF8FB3A2), // Soft pine
        Color(0xFFE09878), // Warm terracotta
        Color(0xFFD4A854), // Soft gold
        Color(0xFFC29CD6), // Soft lavender
        Color(0xFF7EC8CF), // Soft teal
    )

    /** Deterministic per-speaker editorial color (mirrors Sonnet-Create's per-cast accentHex). */
    fun speakerAccent(keyOrName: String?, isDark: Boolean, fallback: Color): Color {
        if (keyOrName.isNullOrBlank()) return fallback
        val palette = if (isDark) DARK_SPEAKER_PALETTE else LIGHT_SPEAKER_PALETTE
        return palette[abs(keyOrName.hashCode()) % palette.size]
    }

    /** True Serif + Italic stage direction style (`FontSynthesis.All` ensures italic renders even on non-italic serif fonts). */
    @Composable
    fun narrationStyle(): TextStyle {
        val base = CivilightTheme.type.body
        return base.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.All,
            textAlign = TextAlign.Center,
            lineHeight = (base.fontSize.value * 1.65f).sp,
        )
    }

    /** True Monospace style with wide tracking for scene header pills and PLOT timeline badges. */
    @Composable
    fun monoChipStyle(sizeSp: Float = 10.5f): TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = sizeSp.sp,
        lineHeight = (sizeSp + 4.5f).sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.1.sp,
    )

    /** True Monospace style for `//` programming-comment memos. */
    @Composable
    fun memoStyle(): TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal,
    )

    private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*")
    private val ITALIC_REGEX = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    private val STRIKE_REGEX = Regex("~~(.+?)~~")
    private val CODE_REGEX = Regex("`([^`]+)`")

    /**
     * Applies Sonnet-Create inline markdown (`**bold**`, `*italic*`, `~~strike~~`, `` `code` ``)
     * in-place without changing character offsets so tap hit-testing and `@` link spans stay exact.
     */
    fun styledAnnotated(
        text: String,
        colors: CivilightColors,
        linkRanges: List<IntRange> = emptyList(),
    ): AnnotatedString = buildAnnotatedString {
        append(text)
        val dimMarker = SpanStyle(color = colors.ink3.copy(alpha = 0.45f), fontSize = 11.sp)

        for (m in BOLD_REGEX.findAll(text)) {
            addStyle(dimMarker, m.range.first, m.range.first + 2)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSynthesis = FontSynthesis.All), m.range.first + 2, m.range.last - 1)
            addStyle(dimMarker, m.range.last - 1, m.range.last + 1)
        }
        for (m in ITALIC_REGEX.findAll(text)) {
            addStyle(dimMarker, m.range.first, m.range.first + 1)
            addStyle(SpanStyle(fontStyle = FontStyle.Italic, fontSynthesis = FontSynthesis.All), m.range.first + 1, m.range.last)
            addStyle(dimMarker, m.range.last, m.range.last + 1)
        }
        for (m in STRIKE_REGEX.findAll(text)) {
            addStyle(dimMarker, m.range.first, m.range.first + 2)
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = colors.ink3), m.range.first + 2, m.range.last - 1)
            addStyle(dimMarker, m.range.last - 1, m.range.last + 1)
        }
        for (m in CODE_REGEX.findAll(text)) {
            addStyle(dimMarker, m.range.first, m.range.first + 1)
            addStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = colors.tint,
                    fontSize = 13.sp,
                ),
                m.range.first + 1,
                m.range.last,
            )
            addStyle(dimMarker, m.range.last, m.range.last + 1)
        }
        // Sonnet-Create `@mention` inline chip wash: accent ink + soft highlight background.
        for (range in linkRanges) {
            val s = range.first.coerceIn(0, text.length)
            val e = (range.last + 1).coerceIn(s, text.length)
            if (e > s) {
                addStyle(
                    SpanStyle(
                        color = colors.acc,
                        background = colors.hl,
                        fontWeight = FontWeight.Medium,
                    ),
                    s,
                    e,
                )
            }
        }
    }
}

/** Dashed rounded border matching Sonnet-Create's branch pills and `＋ 장면` card. */
internal fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.dp,
    dashDp: Dp = 4.dp,
    gapDp: Dp = 3.dp,
): Modifier = drawBehind {
    val sw = strokeWidth.toPx()
    val half = sw / 2f
    val r = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(half, half),
        size = Size((size.width - sw).coerceAtLeast(0f), (size.height - sw).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(
            width = sw,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashDp.toPx(), gapDp.toPx()), 0f),
        ),
    )
}

/**
 * (0.7.3) Sonnet-Create `TypewriterText`:
 * Always lays out the full string with unrevealed suffix set to `Color.Transparent`,
 * preventing any multi-line wrap jitter while ink reveals character-by-character.
 * Supports external progress (TTS sync) or internal timer (`speed`, capped at 4s total).
 */
@Composable
internal fun TypewriterText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    progress: Int? = null,
    speed: Float = 1f,
    showsCaret: Boolean = true,
    caretHeight: Dp = 15.dp,
) {
    val motion = LocalMotion.current
    val accent = CivilightTheme.colors.acc
    var visibleCount by remember(text) {
        mutableIntStateOf(if (motion.reduced) text.length else (progress ?: 0))
    }
    var finishedAtMs by remember(text) {
        mutableStateOf<Long?>(if (motion.reduced) 0L else null)
    }

    LaunchedEffect(text, progress == null, speed, motion.reduced) {
        if (motion.reduced) {
            visibleCount = text.length
            finishedAtMs = 0L
            return@LaunchedEffect
        }
        if (progress != null) {
            visibleCount = progress.coerceIn(0, text.length)
            return@LaunchedEffect
        }
        visibleCount = 0
        finishedAtMs = null
        if (text.isEmpty()) {
            finishedAtMs = System.currentTimeMillis()
            return@LaunchedEffect
        }
        val stepSec = minOf(0.085, 4.0 / text.length.toDouble()) / speed.coerceAtLeast(0.1f)
        val stepMs = (stepSec * 1000.0).toLong().coerceAtLeast(14L)
        for (i in 1..text.length) {
            delay(stepMs)
            visibleCount = i
        }
        finishedAtMs = System.currentTimeMillis()
    }

    LaunchedEffect(progress, motion.reduced) {
        if (motion.reduced) return@LaunchedEffect
        if (progress != null) {
            visibleCount = maxOf(visibleCount, progress.coerceIn(0, text.length))
            if (visibleCount >= text.length && finishedAtMs == null) {
                finishedAtMs = System.currentTimeMillis()
            }
        }
    }

    val caretClockMs = remember { mutableLongStateOf(0L) }
    val showBlinkingCaret = showsCaret && !motion.reduced && motion.loops
    if (showBlinkingCaret) {
        LaunchedEffect(text) {
            while (true) {
                withInfiniteAnimationFrameMillis { caretClockMs.longValue = it }
            }
        }
    }

    val revealed = visibleCount.coerceIn(0, text.length)
    val annotated = remember(text, revealed, color) {
        buildAnnotatedString {
            append(text)
            if (revealed > 0) addStyle(SpanStyle(color = color), 0, revealed)
            if (revealed < text.length) addStyle(SpanStyle(color = Color.Transparent), revealed, text.length)
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        BasicText(
            text = annotated,
            style = style.copy(color = color),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (showBlinkingCaret) {
            val doneAt = finishedAtMs
            val holding = doneAt == null || (System.currentTimeMillis() - doneAt) < 2200L
            val blinkOn = (caretClockMs.longValue % 1000L) < 500L
            if (holding) {
                Box(
                    Modifier
                        .padding(start = 2.dp, bottom = 2.dp)
                        .width(2.dp)
                        .height(caretHeight)
                        .alpha(if (blinkOn) 1f else 0f)
                        .background(accent, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
}
