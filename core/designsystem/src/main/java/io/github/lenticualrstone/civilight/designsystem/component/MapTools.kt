package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.Terrain

/** A map editor tool: Korean label (spec copy) and icon. */
data class MapTool(val label: String, val icon: IconId)

object MapToolDefaults {
    /** DS-55 default tool set `이동 · 핀 · 영역 · 경로 · 라벨 · 레이어`. */
    val tools: List<MapTool> = listOf(
        MapTool("이동", IconId.Move),
        MapTool("핀", IconId.Pin),
        MapTool("영역", IconId.Area),
        MapTool("경로", IconId.Route),
        MapTool("라벨", IconId.Label),
        MapTool("레이어", IconId.Layers),
    )
}

private val Micro500 = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)

/**
 * DS-55 map tool bar (`.mbar`): glass container, padding 4, gap 2, radius 18, 1 dp `line` inset; cells
 * 52 × 50, radius 14, icon 18 + label 500 11 / 14 `ink2`; the selected tool `pri` / `onpri`. Visible mode
 * switching instead of hidden gestures. Labelled [label] (`지도 도구`).
 */
@Composable
fun MapToolbar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tools: List<MapTool> = MapToolDefaults.tools,
    label: String = "지도 도구",
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.semantics { contentDescription = label }) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape)
        Row(
            Modifier
                .insetOutline(colors.line, 1.dp, shape)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            tools.forEachIndexed { i, tool ->
                val selected = i == selectedIndex
                val content by animatedColor(if (selected) colors.onpri else colors.ink2, label = "toolContent")
                val fill = animatedFraction(selected, label = "toolFill")
                val cellShape = RoundedCornerShape(14.dp)
                Column(
                    Modifier
                        .pressable(PressAction.Select(selected) { onSelect(i) }, role = Role.RadioButton, shape = cellShape)
                        .size(52.dp, 50.dp)
                        .clip(cellShape)
                        .drawBehind { val a = fill.value; if (a > 0f) drawRect(colors.pri, alpha = a) },
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CivilightIcon(tool.icon, contentDescription = null, size = 18.dp, tint = content)
                    CivilightText(tool.label, CivilightTheme.type.micro.merge(Micro500), color = content, maxLines = 1)
                }
            }
        }
    }
}

/**
 * DS-55 lock toggle (`.lk`): 36 × 36, radius 10, icon `ink3`; on: `acc` icon on `tint`; touch area 44.
 * Icons `StepLock` (고정) / `StepOpen`; [label] is the accessibility label.
 */
@Composable
fun LockToggle(locked: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier, label: String = "고정") {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val fill = animatedFraction(locked, label = "lockFill")
    val tint by animatedColor(if (locked) colors.acc else colors.ink3, label = "lockTint")
    Box(
        modifier
            .pressable(PressAction.Toggle(locked, onToggle), role = Role.Switch, hitTarget = HitTarget.LOCK_TOGGLE, shape = shape)
            .size(36.dp)
            .clip(shape)
            .drawBehind { val a = fill.value; if (a > 0f) drawRect(colors.tint, alpha = a) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        ContentSwap(locked, contentAlignment = Alignment.Center, label = "lockIcon") { on ->
            CivilightIcon(if (on) IconId.StepLock else IconId.StepOpen, null, size = 18.dp, tint = tint)
        }
    }
}

/** The `.lk`-shaped re-roll button (`Dice`, label `다시`). */
@Composable
fun RerollButton(onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "다시") {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.LOCK_TOGGLE, shape = shape)
            .size(36.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(IconId.Dice, null, size = 18.dp, tint = CivilightTheme.colors.ink3)
    }
}

/**
 * DS-55 generator step row (`.step`): height 46, padding 0 6 0 10, 1 dp `line` top, gap 8, 600 13 / 18 `ink`;
 * [seed] in `number` 11 / 14 `ink3`; trailing lock toggle and re-roll. Selected: `sel` + 2 dp `acc` bar.
 */
@Composable
fun GeneratorStepRow(
    title: String,
    locked: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onReroll: () -> Unit,
    modifier: Modifier = Modifier,
    seed: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .selectedRowBackground(selected, colors.sel, colors.acc, outset = 0.dp)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, scaleOnPress = false) else Modifier)
            .height(46.dp)
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CivilightText(title, type.label.merge(TextStyle(fontWeight = FontWeight.SemiBold)), Modifier.weight(1f), color = colors.ink, maxLines = 1)
        if (seed != null) CivilightText(seed, type.number.merge(TextStyle(fontSize = 11.sp, lineHeight = 14.sp)), color = colors.ink3, maxLines = 1)
        LockToggle(locked, onToggleLock)
        RerollButton(onReroll)
    }
}

/**
 * DS-55 step chip (`.stp`): column, gap 4, padding 8 · 0, radius 12, 500 11 / 14 `ink2`; on: `sel` fill +
 * 1.5 dp `acc` inset, `ink`. Used for steps, hex tools and the stamp palette.
 */
@Composable
fun StepChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: IconId? = null) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val on = animatedFraction(selected, label = "stepChip")
    val content by animatedColor(if (selected) colors.ink else colors.ink2, label = "stepChipContent")
    Column(
        modifier
            .pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, hitTarget = HitTarget.BUTTON, shape = shape)
            .clip(shape)
            .drawWithContent {
                val a = on.value
                if (a > 0f) drawRect(colors.sel, alpha = a)
                drawContent()
                if (a > 0f) {
                    val w = 1.5.dp.toPx()
                    val outline = shape.createOutline(Size(size.width - w, size.height - w), layoutDirection, this)
                    translate(w / 2, w / 2) { drawOutline(outline, colors.acc, alpha = a, style = Stroke(w)) }
                }
            }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) CivilightIcon(icon, null, size = 18.dp, tint = content)
        CivilightText(label, CivilightTheme.type.micro.merge(Micro500), color = content, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Hexagon clip polygon of `.pal` (25 % 0, 75 % 0, 100 % 50 %, 75 % 100 %, 25 % 100 %, 0 50 %). */
private fun hexagon(w: Float, h: Float, inset: Float = 0f) = Path().apply {
    moveTo(w * 0.25f, inset)
    lineTo(w * 0.75f, inset)
    lineTo(w - inset, h / 2)
    lineTo(w * 0.75f, h - inset)
    lineTo(w * 0.25f, h - inset)
    lineTo(inset, h / 2)
    close()
}

/**
 * DS-55 terrain swatch (`.pal`): 52 wide column, hexagon 34 × 30 in the `ter-*` colour, label 500 11 / 14
 * `ink2`; selected: label `ink` and a 2 dp `acc` outline 2 dp off the hexagon.
 */
@Composable
fun TerrainSwatch(terrain: Terrain, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val fill = FixedColors.terrain.getValue(terrain)
    val ring = animatedFraction(selected, label = "terrainRing")
    val labelColor by animatedColor(if (selected) colors.ink else colors.ink2, label = "terrainLabel")
    Column(
        modifier
            .width(52.dp)
            .pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, hitTarget = HitTarget.BUTTON),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.size(42.dp, 38.dp)) {
            val off = 4.dp.toPx()
            val w = size.width - off * 2
            val h = size.height - off * 2
            val path = hexagon(w, h).apply { translate(Offset(off, off)) }
            drawPath(path, fill)
            val a = ring.value
            if (a > 0f) {
                val g = 2.dp.toPx()
                val path = hexagon(size.width, size.height, inset = g / 2)
                drawPath(path, colors.acc, style = Stroke(g), alpha = a)
            }
        }
        CivilightText(label, CivilightTheme.type.micro.merge(Micro500), color = labelColor, maxLines = 1)
    }
}

/**
 * DS-55 format tile (`.fmt`): 166 wide; preview 166 × 118, radius 14, 1 dp `line` inset; selected: double
 * ring (2 dp `bg`, then 2 dp `acc`). [label] below in `small`.
 */
@Composable
fun FormatTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    preview: @Composable BoxScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val ring = animatedFraction(selected, label = "formatRing")
    val labelColor by animatedColor(if (selected) colors.ink else colors.ink3, label = "formatLabel")
    Column(
        modifier
            .width(166.dp)
            .pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, shape = shape),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(166.dp, 118.dp)
                .drawWithContent {
                    drawContent()
                    val a = ring.value
                    if (a > 0f) {
                        val g = 2.dp.toPx()
                        val r = 14.dp.toPx()
                        drawRoundRect(colors.bg, Offset(-g / 2, -g / 2), Size(size.width + g, size.height + g), CornerRadius(r + g / 2), style = Stroke(g), alpha = a)
                        drawRoundRect(colors.acc, Offset(-g * 1.5f, -g * 1.5f), Size(size.width + g * 3, size.height + g * 3), CornerRadius(r + g * 1.5f), style = Stroke(g), alpha = a)
                    }
                },
        ) {
            Box(Modifier.matchParentSize().clip(shape).insetOutline(colors.line, 1.dp, shape), content = preview)
        }
        if (label != null) CivilightText(label, CivilightTheme.type.small, color = labelColor, maxLines = 1)
    }
}
