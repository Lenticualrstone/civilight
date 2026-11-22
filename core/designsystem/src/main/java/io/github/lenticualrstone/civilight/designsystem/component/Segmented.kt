package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor

/**
 * DS-33 text segmented (`.sg`): equal cells in a 44 dp `tint` container (padding 3, radius 12); cells
 * radius 9, 500 13 sp; selected `pri` / `onpri`, others `ink2`. Radio-group semantics. The selected pill
 * slides on `standard` and the text colours cross over (S6); reduced motion: the pill crossfades in place.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val indicator = rememberSlidingIndicator(selectedIndex)
    val pill = segmentStyle(selected = true, outlined = false, colors).fill
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.tint)
            .padding(3.dp)
            .selectableGroup()
            .drawBehind {
                val r = CornerRadius(9.dp.toPx())
                with(indicator) {
                    drawIndicator { x, w, a -> drawRoundRect(pill, Offset(x, 0f), Size(w, size.height), r, alpha = a) }
                }
            },
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            val style = segmentStyle(selected, outlined = false, colors)
            val text by animatedColor(style.text, label = "segText")
            val shape = RoundedCornerShape(9.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onPlaced { indicator.bounds[i] = it.positionInParent().x to it.size.width.toFloat() }
                    .pressable(PressAction.Select(selected) { onSelect(i) }, role = Role.RadioButton, shape = shape, scaleOnPress = false),
                contentAlignment = Alignment.Center,
            ) {
                CivilightText(
                    label,
                    type.label.merge(TextStyle(lineHeight = 38.sp)),
                    color = text,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** One cell of [OutlinedSegmented]: text, or an icon with its label as content description. */
data class SegmentItem(val label: String, val icon: IconId? = null)

/**
 * DS-33 outlined segmented (`.seg`): inline row, height 36 (32 in the WhenPicker), radius 12, 1 dp `edge`
 * inset, clipped; icon cells 44 wide (icon 18), text cells padding 0 × 12 in 600 12 / 16; selected
 * `pri` / `onpri`, others `ink3`; touch height 44. [groupLabel] names the group (`보기`). The selected fill
 * slides between cells on `standard` and content colours cross over (S6); reduced motion: crossfade.
 */
@Composable
fun OutlinedSegmented(
    items: List<SegmentItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    groupLabel: String? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(12.dp)
    val indicator = rememberSlidingIndicator(selectedIndex)
    val pill = segmentStyle(selected = true, outlined = true, colors).fill
    // Not clipped, so each cell's 44 dp touch height reaches past the drawn 36 dp row; the sliding fill is
    // clipped to the row's outline instead, which also rounds the first and last cells.
    Row(
        modifier = modifier
            .then(if (groupLabel != null) Modifier.semantics { contentDescription = groupLabel } else Modifier)
            .heightIn(min = height)
            .height(IntrinsicSize.Min)
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                val path = Path().apply { addOutline(outline) }
                clipPath(path) {
                    with(indicator) {
                        drawIndicator { x, w, a -> drawRect(pill, Offset(x, 0f), Size(w, size.height), alpha = a) }
                    }
                }
            }
            .insetOutline(colors.edge, 1.dp, shape)
            .selectableGroup(),
    ) {
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            val style = segmentStyle(selected, outlined = true, colors)
            val content by animatedColor(style.text, label = "segContent")
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .onPlaced { indicator.bounds[i] = it.positionInParent().x to it.size.width.toFloat() }
                    .pressable(
                        PressAction.Select(selected) { onSelect(i) },
                        role = Role.RadioButton,
                        hitTarget = HitTarget.BUTTON,
                        scaleOnPress = false,
                    )
                    .then(if (item.icon != null) Modifier.width(44.dp) else Modifier.padding(horizontal = 12.dp))
                    .then(if (item.icon != null) Modifier.semantics { contentDescription = item.label } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (item.icon != null) {
                    CivilightIcon(item.icon, contentDescription = null, size = 18.dp, tint = content)
                } else {
                    CivilightText(item.label, type.chip.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = content, maxLines = 1)
                }
            }
        }
    }
}

/**
 * DS-39 list | card toggle: outlined segmented, two 44-wide cells with `List` and `Grid` 18 dp;
 * group `보기`, cells `목록`, `카드`.
 */
@Composable
fun ViewToggle(
    showCards: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    groupLabel: String = "보기",
    listLabel: String = "목록",
    cardLabel: String = "카드",
) {
    OutlinedSegmented(
        items = listOf(SegmentItem(listLabel, IconId.List), SegmentItem(cardLabel, IconId.Grid)),
        selectedIndex = if (showCards) 1 else 0,
        onSelect = { onChange(it == 1) },
        modifier = modifier,
        groupLabel = groupLabel,
    )
}

/** The views of the three-cell [ViewToggle] (gallery, GAL-04): `목록` · `격자` · `카드`. */
enum class ViewToggleMode { LIST, GRID, CARD }

/**
 * DS-39 three-cell variant for the gallery (spec 55 GAL-04, board `scr_gallery.py`): `목록` (three lines) ·
 * `격자` (`GridTall`: three columns of tall 3:4 tiles, two rows, so it does not read as the square `카드`
 * glyph) · `카드` (`Grid`, the 2 × 2 icon of 서재 and 인물). Height 36, radius 12, cells 44 wide; group `보기`.
 */
@Composable
fun ViewToggle(
    mode: ViewToggleMode,
    onChange: (ViewToggleMode) -> Unit,
    modifier: Modifier = Modifier,
    groupLabel: String = "보기",
    listLabel: String = "목록",
    gridLabel: String = "격자",
    cardLabel: String = "카드",
) {
    val modes = ViewToggleMode.entries
    OutlinedSegmented(
        items = listOf(
            SegmentItem(listLabel, IconId.List),
            SegmentItem(gridLabel, IconId.GridTall),
            SegmentItem(cardLabel, IconId.Grid),
        ),
        selectedIndex = modes.indexOf(mode),
        onSelect = { onChange(modes[it]) },
        modifier = modifier,
        groupLabel = groupLabel,
    )
}
