package io.github.lenticualrstone.civilight.feature.script.select

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.records.BlockLineRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** Select-mode header (board `RSSelect`, 104 dp): the position line, `블록 고르기`, `모두` and primary `완료`. */
@Composable
fun SelectTopBar(line: String, onAll: () -> Unit, onDone: () -> Unit) {
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Box(Modifier.fillMaxWidth().heightIn(min = SELECT_BAR + inset)) {
        GlassBackground(GlassSurface.TOP_BAR, Modifier.matchParentSize())
        OnGlass {
            Row(
                Modifier.fillMaxWidth().padding(top = inset + 16.dp, start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    CivilightText(line, type.numberHeader, color = colors.ink3, maxLines = 1)
                    CivilightText(MoveCopy.TITLE, type.title, color = colors.ink, maxLines = 1)
                }
                TextAction(MoveCopy.ALL, onAll)
                CivilightButton(ScriptCopy.DONE, onDone, variant = ButtonVariant.PRIMARY)
            }
        }
    }
}

private val SELECT_BAR: Dp = 104.dp

/** Drag of one row's grip (SCR-37): the screen moves the row with the finger and maps the drop. */
class GripDrag(
    val onStart: (rowKey: String) -> Unit,
    val onDrag: (dy: Float) -> Unit,
    val onEnd: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * A block row (board `RSSelect`): a 52 × 44 checkbox (`고르기`), the label line `리엔 #042` with
 * `◆` / `◇`, the text, and the grip (`끌어서 옮기기`). Checked rows have the `sel` background.
 */
@Composable
fun SelectBlockRowView(row: SelectRowModel.Block, onToggle: () -> Unit, grip: GripDrag, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val drag by rememberUpdatedState(grip)
    Row(
        modifier
            .fillMaxWidth()
            .selectedRowBackground(row.checked, colors.sel, colors.acc)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.width(52.dp).heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
            CheckboxMark(
                checked = row.checked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics { contentDescription = MoveCopy.CHECK },
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(top = 2.dp)
                .pressable(PressAction.Click(onToggle), role = Role.Checkbox, scaleOnPress = false),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val label = buildAnnotatedString {
                val name = row.label.substringBeforeLast(" #")
                val number = row.label.removePrefix(name)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = if (row.checked) colors.ink else colors.ink2)) { append(name) }
                withStyle(SpanStyle(color = colors.ink3)) { append(number) }
                row.marks.forEach { mark ->
                    withStyle(SpanStyle(color = if (mark.glyph == "◆" && !mark.dotted) colors.acc else colors.ink3)) { append("  ${mark.glyph}") }
                }
            }
            androidx.compose.foundation.text.BasicText(label, style = type.small)
            CivilightText(row.text, type.line.merge(TextStyle(fontSize = 15.sp, lineHeight = 24.sp)), color = colors.ink, maxLines = 3)
        }
        Box(
            Modifier
                .size(44.dp)
                .semantics { contentDescription = MoveCopy.GRIP }
                .pointerInput(row.blockKey) {
                    detectDragGestures(
                        onDragStart = { drag.onStart(row.key) },
                        onDrag = { change, amount ->
                            change.consume()
                            drag.onDrag(amount.y)
                        },
                        onDragEnd = { drag.onEnd() },
                        onDragCancel = { drag.onCancel() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            CivilightIcon(IconId.Grip, contentDescription = null, size = 20.dp, tint = if (row.checked) colors.ink else colors.ink3)
        }
    }
}

/** A scene head as a divider in select mode (cannot be checked, SCR-36 `[PROPOSED]`). */
@Composable
fun SelectHeadView(row: SelectRowModel.Head, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    Box(modifier.fillMaxWidth().topHairline(colors.line).padding(start = 24.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
        CivilightText(row.text, CivilightTheme.type.line.merge(TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)), color = colors.ink2, maxLines = 1)
    }
}

/**
 * Select-mode bottom bar (124 dp): `고른 블록 3개` and `#042 ~ #044 · ◆ 1 · ◇ 1 같이 감`, then
 * `옮기기 · 지우기 · 잘라내기 · 복사` (56 dp each).
 */
@Composable
fun SelectBottomBar(
    bar: SelectBar,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onNewScene: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxWidth().heightIn(min = 124.dp + inset).topHairline(colors.line)) {
        GlassBackground(GlassSurface.BOTTOM_BAR, Modifier.matchParentSize())
        OnGlass {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = inset)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    CivilightText(bar.count, type.command.merge(TextStyle(fontWeight = FontWeight.SemiBold)), Modifier.weight(1f), color = colors.ink, maxLines = 1)
                    bar.range?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    val labels = SelectRows.BUTTONS
                    val calls = listOf(onMove, onDelete, onCut, onCopy)
                    val icons = listOf(IconId.Move, IconId.Trash, null, null)
                    labels.forEachIndexed { i, label -> BarTool(label, icons[i], bar.enabled, calls[i], Modifier.weight(1f)) }
                    // SCR-49: `/씬` with checked blocks splits below the last one; select mode has no
                    // command field, so the same routing sits here as a visible button (`ED`).
                    if (onNewScene != null) BarTool(ScriptCopy.NEW_SCENE, null, bar.enabled, onNewScene, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BarTool(label: String, icon: IconId?, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    Column(
        modifier
            .alpha(if (enabled) 1f else 0.38f)
            .pressable(PressAction.Click(onClick), enabled = enabled, role = Role.Button, hitTarget = HitTarget.BUTTON, scaleOnPress = false)
            .heightIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        if (icon != null) CivilightIcon(icon, contentDescription = null, size = 20.dp, tint = colors.ink2)
        CivilightText(label, CivilightTheme.type.chip, color = colors.ink, maxLines = 1)
    }
}

/**
 * The clipboard strip over the reading command row (SCR-40 `ED`; paste is not drawn): `블록 3개 ·
 * 잘라내기` and the paste button, enabled while a block is selected (paste goes below it).
 */
@Composable
fun ClipboardStrip(entry: ScriptClipboard.Entry, canPaste: Boolean, onPaste: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(MoveCopy.clipboard(entry.count, entry.clipboard.cut), CivilightTheme.type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
        Box(Modifier.alpha(if (canPaste) 1f else 0.38f)) {
            TextAction(MoveCopy.PASTE, { if (canPaste) onPaste() }, color = colors.acc)
        }
    }
}

/** `옮기기` destination picker of the checked blocks (SCR-37, Q-17 `[PROPOSED]`). */
@Serializable
data class MoveSheetKey(val projectKey: Key, val blockKeys: List<Key>) : SheetKey

/**
 * Two steps in one sheet: the scene list (`1-7 씬 3 · 꺼진 등불`, the blocks' own scene marked
 * `sel`), then `맨 앞` and `#044 아래` rows of that scene with `‹ 뒤로`. A row moves at once.
 */
@Composable
fun SheetScope.MoveSheet(key: MoveSheetKey, doc: ProjectDoc, unlocked: Boolean, onMove: (io.github.lenticualrstone.civilight.domain.script.BlockPosition) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    var scene by rememberSaveable { mutableStateOf<String?>(null) }
    val picked = scene?.takeIf { doc.manuscript.isLiveUnit(it) }
    val count = SelectRows.bar(doc, SelectState(true, key.blockKeys.toSet())).count
    SheetHeader(title = MoveCopy.MOVE, onClose = ::close, subtitle = count)
    SheetScrollBody(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        if (picked == null) {
            MoveTargets.scenes(doc, key.blockKeys, unlocked).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .topHairline(colors.line)
                        .selectedRowBackground(row.current, colors.sel, colors.acc)
                        .pressable(PressAction.Click({ scene = row.sceneKey }), role = Role.Button, scaleOnPress = false)
                        .heightIn(min = 48.dp)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CivilightText(row.location, type.number.merge(TextStyle(fontSize = 13.sp)), color = colors.ink3, maxLines = 1)
                    CivilightText(row.title, type.command, Modifier.weight(1f), color = colors.ink, maxLines = 1)
                    CivilightIcon(IconId.Next, contentDescription = null, size = 16.dp, tint = colors.ink3)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightIconButton(IconId.Back, MoveCopy.BACK, { scene = null })
                CivilightText(MoveTargets.scenes(doc, key.blockKeys, unlocked).firstOrNull { it.sceneKey == picked }?.location.orEmpty(), type.command, color = colors.ink2)
            }
            MoveTargets.positions(doc, picked, key.blockKeys, unlocked).forEach { row ->
                val line = row.line
                val go = {
                    this@MoveSheet.close()
                    onMove(row.position)
                }
                if (line == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .topHairline(colors.line)
                            .pressable(PressAction.Click(go), role = Role.Button, scaleOnPress = false)
                            .heightIn(min = 48.dp)
                            .padding(vertical = 12.dp),
                    ) { CivilightText(row.label, type.command, color = colors.acc) }
                } else {
                    BlockLineRow(line.copy(label = "${row.label} · ${line.label}"), onClick = go)
                }
            }
        }
    }
}
