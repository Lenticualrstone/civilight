package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import java.text.BreakIterator
import kotlin.math.floor

/** DS-30 button variants. */
enum class ButtonVariant {
    PRIMARY, GHOST, DANGER, PRIMARY_LARGE, COMPACT_PRIMARY, COMPACT_GHOST, TWO_LINE, TWO_LINE_GHOST, ON_TOAST, ON_IMAGE,
}

/** Colours and geometry of a button variant. [outline] is a 1 dp inset when not null. */
data class ButtonStyle(
    val fill: Color,
    val content: Color,
    val outline: Color?,
    val height: Dp,
    val horizontalPadding: Dp,
    val radius: Dp,
    val large: Boolean = false,
    val compact: Boolean = false,
)

/** DS-30 (T-DS-29). */
fun buttonStyle(variant: ButtonVariant, colors: CivilightColors): ButtonStyle = when (variant) {
    ButtonVariant.PRIMARY -> ButtonStyle(colors.pri, colors.onpri, null, 44.dp, 18.dp, 22.dp)
    ButtonVariant.GHOST -> ButtonStyle(colors.tint, colors.ink, null, 44.dp, 18.dp, 22.dp)
    ButtonVariant.DANGER -> ButtonStyle(colors.r18, colors.onr18, null, 44.dp, 18.dp, 22.dp)
    ButtonVariant.PRIMARY_LARGE -> ButtonStyle(colors.pri, colors.onpri, null, 52.dp, 18.dp, 26.dp, large = true)
    ButtonVariant.COMPACT_PRIMARY -> ButtonStyle(colors.pri, colors.onpri, null, 36.dp, 12.dp, 18.dp, compact = true)
    ButtonVariant.COMPACT_GHOST -> ButtonStyle(colors.tint, colors.ink, null, 36.dp, 12.dp, 18.dp, compact = true)
    ButtonVariant.TWO_LINE -> ButtonStyle(colors.pri, colors.onpri, null, 56.dp, 18.dp, 16.dp)
    ButtonVariant.TWO_LINE_GHOST -> ButtonStyle(colors.tint, colors.ink, colors.edge, 56.dp, 18.dp, 16.dp)
    ButtonVariant.ON_TOAST -> ButtonStyle(colors.bg, colors.ink, null, 44.dp, 14.dp, 22.dp)
    ButtonVariant.ON_IMAGE -> ButtonStyle(Color.Transparent, FixedColors.onImage, null, 44.dp, 0.dp, 22.dp)
}

/** Whether the variant is the screen's / sheet's one main action. */
val ButtonVariant.isPrimary: Boolean
    get() = this == ButtonVariant.PRIMARY || this == ButtonVariant.PRIMARY_LARGE ||
        this == ButtonVariant.COMPACT_PRIMARY || this == ButtonVariant.TWO_LINE

/** DS-30: at most one primary per screen or sheet. */
fun hasAtMostOnePrimary(variants: List<ButtonVariant>): Boolean = variants.count { it.isPrimary } <= 1

/**
 * DS-25: a hard-to-undo confirm (project to trash, permanent delete) uses [ButtonVariant.DANGER]; every
 * other confirm, including the R-18 rating raise, uses [ButtonVariant.PRIMARY].
 */
fun confirmButtonVariant(hardToUndo: Boolean): ButtonVariant = if (hardToUndo) ButtonVariant.DANGER else ButtonVariant.PRIMARY

/** Disabled opacity of every control (DS-30 / DS-32 proposed). */
const val DISABLED_ALPHA: Float = 0.38f

/** DS-32 switch: track 46 × 28, thumb 22 at x 3 (off) or 21 (on). */
data class SwitchStyle(val track: Color, val trackOutline: Color?, val thumb: Color, val thumbX: Dp)

fun switchStyle(checked: Boolean, colors: CivilightColors): SwitchStyle =
    if (checked) SwitchStyle(colors.acc, null, colors.bg, 21.dp) else SwitchStyle(colors.tint, colors.line, colors.ink3, 3.dp)

/** DS-32 radio (22 dp circle, 1.5 dp ring, 12 dp dot when on) and checkbox (radius 6, filled `acc` when on). */
data class MarkControlStyle(val ring: Color?, val ringWidth: Dp, val fill: Color?, val dot: Color?, val tick: Color?)

fun radioStyle(selected: Boolean, colors: CivilightColors): MarkControlStyle =
    if (selected) MarkControlStyle(colors.acc, 1.5.dp, null, colors.acc, null)
    else MarkControlStyle(colors.ink3, 1.5.dp, null, null, null)

fun checkboxStyle(checked: Boolean, colors: CivilightColors): MarkControlStyle =
    if (checked) MarkControlStyle(null, 0.dp, colors.acc, null, colors.bg)
    else MarkControlStyle(colors.ink3, 1.5.dp, null, null, null)

/** DS-33 segment cell colours; [outlined] = `.seg`, else `.sg`. */
data class SegmentStyle(val fill: Color, val text: Color)

fun segmentStyle(selected: Boolean, outlined: Boolean, colors: CivilightColors): SegmentStyle = when {
    selected -> SegmentStyle(colors.pri, colors.onpri)
    outlined -> SegmentStyle(Color.Transparent, colors.ink3)
    else -> SegmentStyle(Color.Transparent, colors.ink2)
}

/** DS-37 tab colours. */
data class TabStyle(val text: Color, val underline: Color?)

fun tabStyle(selected: Boolean, colors: CivilightColors): TabStyle =
    if (selected) TabStyle(colors.ink, colors.acc) else TabStyle(colors.ink3, null)

/** DS-38 lens chip states. */
enum class LensChipState { LATEST, PAST, SCRIPT_POSITION }

data class LensChipStyle(val outline: Color?, val text: Color, val endPadding: Dp)

/** DS-38 (T-DS-16). */
fun lensChipStyle(state: LensChipState, colors: CivilightColors): LensChipStyle = when (state) {
    LensChipState.LATEST -> LensChipStyle(colors.line, colors.ink2, 12.dp)
    LensChipState.PAST -> LensChipStyle(colors.acc, colors.acc, 12.dp)
    LensChipState.SCRIPT_POSITION -> LensChipStyle(null, colors.ink2, 0.dp)
}

/** Project rating (rule 19); the stored value is LIB's. */
enum class Rating(val label: String) { ALL("ALL"), R15("R-15"), R18("R-18") }

/** Where a badge sits. */
enum class BadgeSurface { PLAIN, ON_IMAGE }

data class BadgeStyle(val outline: Color, val text: Color)

/** DS-20 (T-DS-15). Never filled. */
fun badgeStyle(rating: Rating, surface: BadgeSurface, colors: CivilightColors): BadgeStyle = when (surface) {
    BadgeSurface.PLAIN -> if (rating == Rating.R18) BadgeStyle(colors.r18, colors.r18) else BadgeStyle(colors.ink3, colors.ink2)
    BadgeSurface.ON_IMAGE ->
        if (rating == Rating.R18) BadgeStyle(FixedColors.onImageR18, FixedColors.onImageR18)
        else BadgeStyle(FixedColors.onImageBadge, FixedColors.onImageBadge)
}

/** DS-27 toast colours (T-DS-32). */
data class ToastStyle(val fill: Color, val message: Color, val actionFill: Color, val actionText: Color)

fun toastStyle(colors: CivilightColors): ToastStyle = ToastStyle(colors.ink, colors.bg, colors.bg, colors.ink)

/** DS-40 / DS-42 selection (T-DS-35): `sel` fill and a 2 dp `acc` bar; search boxes use `hl`. */
data class SelectionStyle(val fill: Color, val bar: Color, val barWidth: Dp)

fun selectionStyle(colors: CivilightColors): SelectionStyle = SelectionStyle(colors.sel, colors.acc, 2.dp)

/** DS-60 / T-DS-21: `min(1, available ÷ required)`, both in minutes. */
fun travelBarFraction(available: Long, required: Long): Float = when {
    required <= 0L -> 1f
    available <= 0L -> 0f
    else -> (available.toDouble() / required.toDouble()).coerceAtMost(1.0).toFloat()
}

/** DS-46: the first grapheme of a name. */
fun avatarGlyph(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return ""
    val it = BreakIterator.getCharacterInstance()
    it.setText(trimmed)
    val end = it.next()
    return if (end == BreakIterator.DONE) trimmed else trimmed.substring(0, end)
}

/** DS-46: glyph size `floor(size × 0.38)` sp (32 → 12, 36 → 13, 38 → 14). */
fun avatarGlyphSp(sizeDp: Int): Int = floor(sizeDp * 0.38).toInt()
