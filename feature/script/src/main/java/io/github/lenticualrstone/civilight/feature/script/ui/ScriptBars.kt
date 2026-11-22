package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.KeyboardTool
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.LensChipState
import io.github.lenticualrstone.civilight.designsystem.component.Pill
import io.github.lenticualrstone.civilight.designsystem.component.SpeakerPill
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.TopGlassBar
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.MarkerGlyph
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.state.CommandRow

/**
 * Script header while reading (SCR-01, board `Main`): 118 dp glass, the mono position line, the
 * project title, the position label `이 씬 · 1-7` (a lens-shaped chip that is not a lens) and the
 * layers button.
 */
@Composable
fun ScriptTopBar(
    title: String,
    line: String,
    positionLabel: String?,
    wide: Boolean,
    onPositionLabel: () -> Unit,
    onLayers: (() -> Unit)?,
) {
    TopGlassBar(title = title, smallLine = line, wide = wide, barHeight = LayoutMetrics.topBarScript) {
        if (positionLabel != null) LensChip(positionLabel, LensChipState.SCRIPT_POSITION, onPositionLabel)
        LayersButton(onLayers)
    }
}

/** Script header while typing (SCR-14, board `EditKeyboard`): 76 dp, `1-7 · 씬 3 · 저장됨` and `완료`. */
@Composable
fun EditingTopBar(line: String, onDone: () -> Unit) {
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxWidth().heightIn(min = EDIT_BAR + inset)) {
        GlassBackground(GlassSurface.TOP_BAR, Modifier.matchParentSize())
        OnGlass {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = inset + 4.dp, start = 20.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CivilightText(line, CivilightTheme.type.numberHeader, Modifier.weight(1f), color = CivilightTheme.colors.ink3, maxLines = 1)
                TextAction(ScriptCopy.DONE, onDone)
            }
        }
    }
}

private val EDIT_BAR: Dp = 76.dp

/** The layers button (`연출 · 메모 표시`, SCR-26). */
@Composable
private fun LayersButton(onLayers: (() -> Unit)?) {
    CivilightIconButton(IconId.Layers, ScriptCopy.LAYERS_LABEL, onLayers ?: {}, enabled = onLayers != null)
}

/**
 * Bottom glass bar while reading (SCR-01): the command row ([row], see [CommandRow]): the speaker
 * pill, the layers button, the selected block's panel buttons `◆ 시간 · 참고 · 속성` (SCR-30 tap path)
 * and the `줄바꿈` pill; then the command field with the Life Recorder mic.
 */
@Composable
fun ScriptBottomBar(
    row: CommandRow,
    speakerName: String?,
    onSpeaker: () -> Unit,
    onLayers: (() -> Unit)?,
    onLineBreak: () -> Unit,
    lineBreakEnabled: Boolean,
    timeHasChange: Boolean = false,
    onPanelTab: (PanelTab) -> Unit = {},
    command: @Composable () -> Unit,
) {
    BottomGlassBar(
        extraRow = if (row.visible) {
            {
                if (row.speaker) {
                    SpeakerPill(name = speakerName ?: ScriptCopy.NO_SPEAKER, label = speakerName ?: ScriptCopy.NO_SPEAKER, onClick = onSpeaker)
                }
                if (row.layers) LayersButton(onLayers)
                if (row.panelTabs) PanelTabButtons(timeHasChange, onPanelTab)
                Spacer(Modifier.weight(1f))
                if (row.lineBreak) Pill(ScriptCopy.LINE_BREAK, onLineBreak, Modifier.alpha(if (lineBreakEnabled) 1f else 0.38f), primary = true)
            }
        } else null,
        commandBar = command,
    )
}

/**
 * The swipe row's three buttons drawn in the command row (SCR-30, Q-02 `ED`): `◆ 시간` (`◆` and label
 * `acc` when the block carries a `◆`), `참고`, `속성`, compact, each with a 44 dp touch area.
 */
@Composable
private fun PanelTabButtons(timeHasChange: Boolean, onTab: (PanelTab) -> Unit) {
    val colors = CivilightTheme.colors
    val style = CivilightTheme.type.chip
    PanelTab.SWIPE.forEach { tab ->
        Row(
            Modifier
                .pressable(PressAction.Click({ onTab(tab) }), role = Role.Button, hitTarget = HitTarget.SMALL, scaleOnPress = false)
                .padding(horizontal = 5.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (tab) {
                PanelTab.TIME -> if (timeHasChange) MarkerGlyph(MarkerKind.CHANGE, size = 11.dp)
                PanelTab.REF -> CivilightIcon(IconId.Paper, null, size = 16.dp, tint = colors.ink2)
                else -> CivilightIcon(IconId.Props, null, size = 16.dp, tint = colors.ink2)
            }
            val color = if (tab == PanelTab.TIME && timeHasChange) colors.acc else colors.ink
            CivilightText(tab.label, style, color = color, maxLines = 1)
        }
    }
}

/**
 * The command field (SCR-12, SCR-68): idle it is the DS command bar with the hint and the mic;
 * tapped, it takes typing. `/` alone opens `블록 추가`, `/대` … `/메` add a block, `/씬` routes to a
 * new scene ([onSlash]); other text goes to [onSubmit] (jumps and search).
 */
@Composable
fun ScriptCommandField(
    typing: Boolean,
    text: String,
    onStart: () -> Unit,
    onText: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onClose: () -> Unit,
    onMic: () -> Unit,
    focusRequester: FocusRequester,
    /** The field's hint: the script's (SCR-68) or another script screen's (`에셋`, SCR-61). */
    hint: String = ScriptCommands.HINT,
) {
    if (!typing) {
        CommandBar(hint = hint, onClick = onStart, onMic = onMic, micLabel = ScriptCopy.MIC_LABEL)
        return
    }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(colors.tint)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(start = 14.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText("/", type.number.merge(TextStyle(fontSize = 13.sp)), color = colors.ink2, maxLines = 1)
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) CivilightText(hint, type.command, color = colors.ink3, maxLines = 1)
            BasicTextField(
                value = text,
                onValueChange = onText,
                singleLine = true,
                textStyle = type.command.merge(TextStyle(color = colors.ink)),
                cursorBrush = SolidColor(colors.acc),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit(text) }, onDone = { onSubmit(text) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = hint },
            )
        }
        TextAction(ScriptCopy.CLOSE, onClose)
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}


/**
 * The keyboard toolbar (SCR-16, board `EditKeyboard`): a 56 dp glass strip on the keyboard with the
 * block kind `대사 ▾` (add sheet), the speaker (dialogue only), `( )`, `@`, `◆` and undo. Every item
 * is at least 44 × 44. `◆` (the change-record sheet, SCR-41) is shown disabled until that sheet exists.
 */
@Composable
fun KeyboardToolbar(
    kindLabel: String,
    speakerName: String?,
    showSpeaker: Boolean,
    onKind: () -> Unit,
    onSpeaker: () -> Unit,
    /** Null hides `( )` (kinds without `괄호 지문`, SCR-07). */
    onParen: (() -> Unit)?,
    parenOn: Boolean,
    onLink: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    /** `◆` (SCR-41): the change sheet for the block being typed; null while the caret has no block yet. */
    onChange: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val bottom = WindowInsets.ime.union(WindowInsets.navigationBars).asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(TOOLBAR).topHairline(colors.line)) {
            GlassBackground(GlassSurface.BOTTOM_BAR, Modifier.matchParentSize())
            OnGlass {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(TOOLBAR)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KindTool(kindLabel + ScriptCopy.KIND_CHEVRON, onKind)
                    if (showSpeaker) SpeakerTool(speakerName ?: ScriptCopy.NO_SPEAKER, onSpeaker)
                    if (onParen != null) {
                        KeyboardTool(onClick = onParen, text = ScriptCopy.PAREN_TOOL, on = parenOn, contentDescription = ScriptCopy.PAREN_LABEL)
                    }
                    KeyboardTool(onClick = onLink, text = ScriptCopy.LINK_TOOL)
                    Box(Modifier.alpha(if (onChange != null) 1f else 0.38f)) {
                        KeyboardTool(onClick = { onChange?.invoke() }, text = ScriptCopy.CHANGE_TOOL, contentDescription = ScriptCopy.CHANGE_LABEL)
                    }
                    CivilightIconButton(IconId.Undo, ScriptCopy.UNDO_LABEL, onUndo, enabled = canUndo)
                }
            }
        }
        Spacer(Modifier.fillMaxWidth().height(bottom).background(Color.Transparent))
    }
}

private val TOOLBAR: Dp = 56.dp

@Composable
private fun KindTool(label: String, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .pressable(PressAction.Click(onClick), role = Role.Button, hitTarget = HitTarget.BUTTON, shape = shape)
            .heightIn(min = 44.dp)
            .widthIn(min = 64.dp)
            .clip(shape)
            .background(colors.sel)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(label, CivilightTheme.type.button, color = colors.ink, maxLines = 1)
    }
}

@Composable
private fun SpeakerTool(name: String, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .pressable(PressAction.Click(onClick), role = Role.Button, hitTarget = HitTarget.BUTTON, shape = shape)
            .heightIn(min = 44.dp)
            .widthIn(min = 72.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name, size = 24.dp)
        CivilightText(name, CivilightTheme.type.button, color = colors.ink2, maxLines = 1)
    }
}

/** Remembers a focus requester (a helper for call sites). */
@Composable
fun rememberFocus(): FocusRequester = remember { FocusRequester() }
