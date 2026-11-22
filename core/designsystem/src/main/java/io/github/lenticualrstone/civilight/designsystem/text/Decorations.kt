package io.github.lenticualrstone.civilight.designsystem.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors

/** Inline decorations of DS-18, DS-19 and DS-45. */
enum class InlineDecoration { LINK, CANDIDATE, CANDIDATE_SELECTED, HIDDEN_LINK, DELETED_REF, SEARCH_HIT, PAREN }

/** A decoration over the UTF-16 range [start, end) of the displayed text. */
data class DecorationRange(val start: Int, val end: Int, val kind: InlineDecoration) {
    init {
        require(start in 0..end) { "bad range $start..$end" }
    }
}

/** Underline drawn under a range: [offset] below the baseline. Dotted underlines use [dashOn] / [dashOff]. */
data class UnderlineSpec(
    val color: Color,
    val thickness: Dp,
    val offset: Dp,
    val dotted: Boolean,
    val dashOn: Dp = 2.dp,
    val dashOff: Dp = 2.dp,
)

/** A rounded box behind a range (search hit, tapped candidate). */
data class BoxSpec(
    val fill: Color,
    val outline: Color?,
    val outlineWidth: Dp,
    val radius: Dp,
    val padHorizontal: Dp,
    val padVertical: Dp,
)

/** What a decoration draws; [textColor] null keeps the surrounding text colour. */
data class DecorationStyle(val textColor: Color?, val underline: UnderlineSpec?, val box: BoxSpec?)

/** The search-hit box of DS-45 (T-DS-20): `hl` fill, `ink` text, 1 dp `acc` inset, radius 4, padding 1 × 3. */
fun searchHitBoxStyle(colors: CivilightColors): BoxSpec =
    BoxSpec(fill = colors.hl, outline = colors.acc, outlineWidth = 1.dp, radius = 4.dp, padHorizontal = 3.dp, padVertical = 1.dp)

/**
 * DS-19 / DS-18 decoration styles. [textColor] is the colour the text would have without decoration
 * (dotted underlines keep it).
 */
fun decorationStyle(kind: InlineDecoration, colors: CivilightColors, textColor: Color): DecorationStyle = when (kind) {
    InlineDecoration.LINK, InlineDecoration.HIDDEN_LINK -> DecorationStyle(
        textColor = colors.ink,
        underline = UnderlineSpec(colors.ink3, thickness = 1.dp, offset = 2.dp, dotted = false),
        box = null,
    )
    InlineDecoration.CANDIDATE -> DecorationStyle(
        textColor = null,
        underline = UnderlineSpec(colors.ink3, thickness = 2.dp, offset = 6.dp, dotted = true),
        box = null,
    )
    InlineDecoration.CANDIDATE_SELECTED -> DecorationStyle(
        textColor = null,
        underline = UnderlineSpec(colors.acc, thickness = 2.dp, offset = 6.dp, dotted = true),
        box = BoxSpec(colors.sel, outline = null, outlineWidth = 0.dp, radius = 4.dp, padHorizontal = 2.dp, padVertical = 2.dp),
    )
    InlineDecoration.DELETED_REF -> DecorationStyle(
        textColor = null,
        underline = UnderlineSpec(textColor, thickness = 1.dp, offset = 4.dp, dotted = true),
        box = null,
    )
    InlineDecoration.SEARCH_HIT -> DecorationStyle(textColor = colors.ink, underline = null, box = searchHitBoxStyle(colors))
    InlineDecoration.PAREN -> DecorationStyle(textColor = colors.ink3, underline = null, box = null)
}
