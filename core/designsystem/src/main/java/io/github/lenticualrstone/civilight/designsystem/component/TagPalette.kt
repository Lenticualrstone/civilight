package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Rgba

/**
 * (0.2.0, CST-75) The tag palette. A tag stores a token [name] (`Tag.color`); the names are stored in projects
 * and files, so they never change. An unknown or null name means no colour (no dot). Each token has a light
 * and a dark tone, muted to sit beside `ink` text in both themes.
 */
enum class TagColor(val token: String, private val light: Int, private val dark: Int) {
    RED("red", 0xC2544B, 0xE0857C),
    ORANGE("orange", 0xC27A3A, 0xE3A46A),
    YELLOW("yellow", 0xA88B22, 0xD9C060),
    GREEN("green", 0x4E8A55, 0x86C08C),
    TEAL("teal", 0x2F8580, 0x6FBFB8),
    BLUE("blue", 0x3F6FB0, 0x82A9E0),
    VIOLET("violet", 0x7A5BB0, 0xB29AE0),
    GRAY("gray", 0x7A7F87, 0xA9AEB5),
    ;

    fun color(dark: Boolean): Color = Rgba.hex(if (dark) this.dark else light).toColor()

    companion object {
        /** The token of a stored name, or null (no colour). */
        fun of(token: String?): TagColor? = token?.let { t -> entries.firstOrNull { it.token == t } }
    }
}

/**
 * Accessibility names of the swatches. No planning source names the colours: interim words, to confirm
 * (`TODO_COPY_DS_TAG_COLOR_*`).
 */
object TagColorCopy {
    const val TODO_COPY_DS_TAG_COLOR_RED = "빨강"
    const val TODO_COPY_DS_TAG_COLOR_ORANGE = "주황"
    const val TODO_COPY_DS_TAG_COLOR_YELLOW = "노랑"
    const val TODO_COPY_DS_TAG_COLOR_GREEN = "초록"
    const val TODO_COPY_DS_TAG_COLOR_TEAL = "청록"
    const val TODO_COPY_DS_TAG_COLOR_BLUE = "파랑"
    const val TODO_COPY_DS_TAG_COLOR_VIOLET = "보라"
    const val TODO_COPY_DS_TAG_COLOR_GRAY = "회색"

    /** `없음` (CST-75: the default, no colour). */
    const val NONE = "없음"

    fun name(color: TagColor): String = when (color) {
        TagColor.RED -> TODO_COPY_DS_TAG_COLOR_RED
        TagColor.ORANGE -> TODO_COPY_DS_TAG_COLOR_ORANGE
        TagColor.YELLOW -> TODO_COPY_DS_TAG_COLOR_YELLOW
        TagColor.GREEN -> TODO_COPY_DS_TAG_COLOR_GREEN
        TagColor.TEAL -> TODO_COPY_DS_TAG_COLOR_TEAL
        TagColor.BLUE -> TODO_COPY_DS_TAG_COLOR_BLUE
        TagColor.VIOLET -> TODO_COPY_DS_TAG_COLOR_VIOLET
        TagColor.GRAY -> TODO_COPY_DS_TAG_COLOR_GRAY
    }

    val all: List<String> = TagColor.entries.map(::name) + NONE
}

/** The colour dot before a tag name (8 dp); nothing for no colour. */
@Composable
fun TagDot(token: String?, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val color = TagColor.of(token) ?: return
    Box(modifier.size(size).clip(CircleShape).background(color.color(CivilightTheme.colors.isDark)))
}

/**
 * CST-75 `색`: `없음` then one 28 dp swatch per palette token (44 dp touch), the picked one ringed in `acc`.
 * Each swatch reads its colour name; [onPick] gets the token, or null for `없음`.
 */
@Composable
fun TagColorRow(selected: String?, onPick: (String?) -> Unit, modifier: Modifier = Modifier) {
    val picked = TagColor.of(selected)
    ChipFlow(modifier) {
        FilterChip(TagColorCopy.NONE, picked == null, { onPick(null) }, Modifier.align(Alignment.CenterVertically))
        TagColor.entries.forEach { c ->
            val on = c == picked
            Box(
                Modifier
                    .align(Alignment.CenterVertically)
                    .pressable(onClick = { onPick(c.token) }, hitTarget = HitTarget.BUTTON, shape = CircleShape)
                    .semantics {
                        contentDescription = TagColorCopy.name(c)
                        this.selected = on
                    }
                    .size(28.dp)
                    .clip(CircleShape)
                    .then(if (on) Modifier.insetOutline(CivilightTheme.colors.acc, 2.dp, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(if (on) 18.dp else 22.dp).clip(CircleShape).background(c.color(CivilightTheme.colors.isDark)))
            }
        }
    }
}
