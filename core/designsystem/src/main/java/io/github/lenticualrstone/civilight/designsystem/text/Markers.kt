package io.github.lenticualrstone.civilight.designsystem.text

import androidx.compose.ui.graphics.Color
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors

/** The six allowed markers (DS-16, DS-17). Copy keeps the Unicode characters; renderers draw vectors. */
enum class MarkerKind(val symbol: Char, val spokenName: String?) {
    /** `◆` a change record, `acc`. */
    CHANGE('◆', "변화"),

    /** `◇` first reveal / soft notice, `ink3`. */
    NOTICE('◇', "첫 공개"),

    /** `▲` check warning, always `warn`. */
    WARNING('▲', "경고"),

    /** `▸` direction cue, `ink3`. */
    DIRECTION('▸', null),

    /** `♪` BGM, `ink3`. */
    BGM('♪', null),

    /** `⌁` author memo, `ink3`. */
    MEMO('⌁', null);

    companion object {
        fun of(c: Char): MarkerKind? = entries.firstOrNull { it.symbol == c }
    }
}

/** Solid, or dotted = attached to something deleted (DS-18). */
enum class MarkerLine { SOLID, DOTTED }

/** Accessibility label of a mark whose anchor block was deleted (rule 12 / rule 20). */
const val LABEL_ANCHOR_DELETED: String = "근거 지워짐"

/** Accessibility label of a mark whose target was deleted (rule 20). */
const val LABEL_TARGET_DELETED: String = "대상 지워짐"

/**
 * How a marker is drawn in its 14-unit box (`scr_new_scene.py` `DM`).
 * Dotted marks: no fill, stroke 1.5, dash 2 on / 1.8 off, colour unchanged.
 */
data class MarkerStyle(
    val color: Color,
    val filled: Boolean,
    val dashed: Boolean,
    val strokeUnits: Float,
    val dashOnUnits: Float,
    val dashOffUnits: Float,
    val accessibilityLabel: String?,
)

/** Size of the marker's drawing box in units. */
const val MARKER_BOX_UNITS: Float = 14f

fun markerColor(kind: MarkerKind, colors: CivilightColors): Color = when (kind) {
    MarkerKind.CHANGE -> colors.acc
    MarkerKind.WARNING -> colors.warn
    MarkerKind.NOTICE, MarkerKind.DIRECTION, MarkerKind.BGM, MarkerKind.MEMO -> colors.ink3
}

/** DS-16 / DS-18 marker style (T-DS-12, T-DS-14). */
fun markerStyle(
    kind: MarkerKind,
    line: MarkerLine = MarkerLine.SOLID,
    colors: CivilightColors,
    deletedLabel: String = LABEL_ANCHOR_DELETED,
): MarkerStyle {
    val color = markerColor(kind, colors)
    if (line == MarkerLine.DOTTED) {
        return MarkerStyle(color, filled = false, dashed = true, strokeUnits = 1.5f, dashOnUnits = 2f, dashOffUnits = 1.8f, accessibilityLabel = deletedLabel)
    }
    val filled = when (kind) {
        MarkerKind.CHANGE, MarkerKind.WARNING, MarkerKind.DIRECTION, MarkerKind.BGM -> true
        MarkerKind.NOTICE, MarkerKind.MEMO -> false
    }
    return MarkerStyle(color, filled, dashed = false, strokeUnits = 1.5f, dashOnUnits = 0f, dashOffUnits = 0f, accessibilityLabel = kind.spokenName)
}

/**
 * Text for screen readers: `◆` `◇` `▲` are spoken by their names (DS-16); `▸ ♪ ⌁` are dropped,
 * since the label after them already says what the line is.
 */
fun markerSpokenText(text: String): String {
    val sb = StringBuilder()
    for (c in text) {
        val kind = MarkerKind.of(c)
        when {
            kind == null -> sb.append(c)
            kind.spokenName != null -> sb.append(kind.spokenName)
            else -> Unit
        }
    }
    return sb.toString().replace(Regex(" {2,}"), " ").trim()
}

/** Whether [text] contains a marker character. */
fun hasMarker(text: CharSequence): Boolean = text.any { MarkerKind.of(it) != null }
