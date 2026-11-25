package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** Row separator: decorative `line`, or dashed for a deleted counterpart (DS-18). */
enum class RowDivider { NONE, SOLID, DASHED }

/**
 * DS-40 selected row: `sel` fill and a 2 dp `acc` bar drawn edge-to-edge with −[outset] side margins, so
 * the text does not move. [radius] rounds the fill (picker rows use 12). A change of [selected] fades the
 * fill and bar in or out on `standard` (S6; 120 ms when reduced); the first draw never animates. Not
 * composable, so it can be built anywhere a `Modifier` is.
 */
fun Modifier.selectedRowBackground(selected: Boolean, fill: androidx.compose.ui.graphics.Color, bar: androidx.compose.ui.graphics.Color, outset: Dp = 10.dp, radius: Dp = 0.dp): Modifier =
    this then SelectedRowElement(selected, fill, bar, outset, radius)

private data class SelectedRowElement(
    val selected: Boolean,
    val fill: Color,
    val bar: Color,
    val outset: Dp,
    val radius: Dp,
) : ModifierNodeElement<SelectedRowNode>() {
    override fun create(): SelectedRowNode = SelectedRowNode(selected, fill, bar, outset, radius)

    override fun update(node: SelectedRowNode) {
        node.fill = fill
        node.bar = bar
        node.outset = outset
        node.radius = radius
        node.select(selected)
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "selectedRowBackground"
        properties["selected"] = selected
    }
}

private class SelectedRowNode(
    private var selected: Boolean,
    var fill: Color,
    var bar: Color,
    var outset: Dp,
    var radius: Dp,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val amount = Animatable(if (selected) 1f else 0f)
    private var job: Job? = null

    fun select(value: Boolean) {
        if (value == selected) return
        selected = value
        val target = if (value) 1f else 0f
        if (!isAttached) return
        val spec = currentValueOf(LocalMotion).spec<Float>(CivilightMotion.standard)
        job?.cancel()
        job = coroutineScope.launch { amount.animateTo(target, spec) { invalidateDraw() } }
    }

    override fun onAttach() {
        val target = if (selected) 1f else 0f
        if (amount.value != target && !amount.isRunning) coroutineScope.launch { amount.snapTo(target) }
    }

    override fun ContentDrawScope.draw() {
        val a = amount.value
        if (a > 0f) {
            val o = outset.toPx()
            drawRoundRect(fill, Offset(-o, 0f), Size(size.width + o * 2, size.height), CornerRadius(radius.toPx()), alpha = a)
            drawRect(bar, Offset(-o, 0f), Size(2.dp.toPx(), size.height), alpha = a)
        }
        drawContent()
    }
}

/**
 * DS-40 list row (`.li`): gap 12, padding 12 · 0, 1 dp `line` top border; optional [leading] (avatar 36–38,
 * checkbox, grip); title `itemTitle` (`itemTitleCompact` when [compact]) and a one-line `small` sub-line;
 * [trailing] value + chevron or a control. Min height 44. Rows are set on `bg`, never in cards.
 * [titleDotted] draws the deleted-reference dotted underline (DS-18).
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    compact: Boolean = false,
    divider: RowDivider = RowDivider.SOLID,
    titleDotted: Boolean = false,
    subtitleLines: Int = 1,
    highlighted: Boolean = false,
    onHighlightDone: () -> Unit = {},
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val titleStyle = if (compact) type.itemTitleCompact else type.itemTitle
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (divider != RowDivider.NONE) Modifier.topHairline(colors.line, dashed = divider == RowDivider.DASHED) else Modifier)
            // U-15.6: a row just made with `만들기` is briefly highlighted (not selected).
            .createdHighlight(highlighted, onHighlightDone)
            .selectedRowBackground(selected, colors.sel, colors.acc)
            .then(if (onClick != null) Modifier.pressable(PressAction.Click(onClick), role = Role.Button, scaleOnPress = false) else Modifier)
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (titleDotted) {
                DecoratedText(
                    AnnotatedString(title),
                    titleStyle,
                    color = colors.ink,
                    decorations = listOf(DecorationRange(0, title.length, InlineDecoration.DELETED_REF)),
                    maxLines = 1,
                )
            } else {
                CivilightText(title, titleStyle, color = colors.ink, maxLines = 1)
            }
            if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3, maxLines = subtitleLines)
        }
        trailing?.invoke()
    }
}

/** DS-40 settings row: optional leading category icon + trailing `small` value in `ink2` + `IconId.Next`. */
@Composable
fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    divider: RowDivider = RowDivider.SOLID,
    leadingIcon: IconId? = null,
) {
    val colors = CivilightTheme.colors
    ListRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        leading = leadingIcon?.let { icon ->
            { CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink2) }
        },
        onClick = onClick,
        compact = true,
        divider = divider,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value != null) {
                    CivilightText(value, CivilightTheme.type.small, color = colors.ink2, maxLines = 1)
                }
                CivilightIcon(IconId.Next, contentDescription = null, size = 14.dp, tint = colors.ink3)
            }
        },
    )
}

/**
 * DS-40 picker row (speaker picker): padding 8 × 10, radius 12, avatar 38, name `itemTitle`, sub `small`;
 * current choice `sel` + 2 dp `acc` bar.
 */
@Composable
fun PickerRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    avatarName: String = name,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectedRowBackground(selected, colors.sel, colors.acc, outset = 0.dp, radius = 12.dp)
            .pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, shape = RoundedCornerShape(12.dp), scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(avatarName, size = AvatarSize.Picker)
        Column(Modifier.weight(1f)) {
            CivilightText(name, type.itemTitle, color = colors.ink, maxLines = 1)
            if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3, maxLines = 1)
        }
    }
}

/**
 * DS-40 field table row (`.row`): baseline row, space-between, padding 11 · 0, 1 dp `line` top, `command`
 * role; [label] `ink3`, [value] `ink`; after the value an optional `small` `ink3` [point] (`1-3부터`) and a
 * [marker] (`◆` in `acc`). [numeric] values use the `number` role. A changed [value] swaps with the
 * lens value motion (CNV-42, S18f).
 */
@Composable
fun FieldTableRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    point: String? = null,
    marker: MarkerKind? = null,
    numeric: Boolean = false,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (divider) Modifier.topHairline(colors.line) else Modifier)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, scaleOnPress = false) else Modifier)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CivilightText(label, type.command, Modifier.alignByBaseline(), color = colors.ink3, maxLines = 1)
        Row(
            Modifier
                .weight(1f)
                .alignByBaseline(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            ValueSwap(
                value,
                if (numeric) type.number.merge(TextStyle(fontSize = 14.sp, lineHeight = 20.sp)) else type.command,
                Modifier.alignByBaseline(),
                color = colors.ink,
                maxLines = Int.MAX_VALUE,
                contentAlignment = Alignment.CenterEnd,
            )
            if (point != null) CivilightText(point, type.small, Modifier.alignByBaseline(), color = colors.ink3, maxLines = 1)
            if (marker != null) CivilightText(marker.symbol.toString(), type.command, Modifier.alignByBaseline())
        }
    }
}

/** DS-40 key–value line (`.rk`, sheets): gap 10, padding 7 · 0, `line` top, 400 13 / 19 `ink`; key 64 wide, 12 sp `ink3`. */
@Composable
fun KeyValueLine(key: String, value: String, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CivilightText(key, type.small, Modifier.width(64.dp).alignByBaseline(), color = colors.ink3)
        CivilightText(value, type.command.merge(TextStyle(fontSize = 13.sp, lineHeight = 19.sp)), Modifier.weight(1f).alignByBaseline(), color = colors.ink)
    }
}

/** DS-40 field action row (`fld`): min 44, `line` top, gap 10; key `small` 40 wide; value 14 / 20; action `label` 12 sp + ` ›`. */
@Composable
fun FieldActionRow(
    key: String,
    value: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(key, type.small, Modifier.width(40.dp), color = colors.ink3, maxLines = 1)
        CivilightText(value, type.command, Modifier.weight(1f), color = colors.ink, maxLines = 1)
        TextAction("$actionLabel ›", onAction, style = type.label.merge(TextStyle(fontSize = 12.sp)))
    }
}

/**
 * DS-40 section label (`.sec`): `sec` role (11 / 14, 500, 0.08 em, `ink3`, `tnum`); optional right-side
 * link on the same baseline (`레코더 · 정리 안 됨 1 ›` in `acc`). Spacing (22–28 above, 6–10 below) is
 * the caller's.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailing: (() -> Unit)? = null,
    trailingAccent: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(text, type.sec, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
        if (trailing != null) {
            val color = if (trailingAccent) colors.acc else colors.ink3
            if (onTrailing != null) TextAction(trailing, onTrailing, color = color, style = type.small)
            else CivilightText(trailing, type.small, color = color, maxLines = 1)
        }
    }
}

/**
 * DS-35 value-under-edit: a field-table row whose value is being corrected under a past lens
 * (`LensEdit`): [label] with the row's [meta] right-aligned above (`1-5부터 · 다음 변화 2-5`), then the
 * focused field.
 */
@Composable
fun ValueUnderEditRow(
    label: String,
    meta: String?,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .padding(vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            CivilightText(label, type.command, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            if (meta != null) CivilightText(meta, type.small, color = colors.ink3, maxLines = 1)
        }
        CivilightTextField(value = value, onValueChange = onValueChange)
    }
}
