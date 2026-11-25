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
import androidx.compose.runtime.State
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onFind: (() -> Unit)? = null,
    back: io.github.lenticualrstone.civilight.designsystem.component.IconAction? = null,
) {
    // (0.2.0, SCR-01, U-14.13) `이 씬 · 1-7` · `찾기` (icon `search`, a) · `표시` (icon `layers`, a).
    TopGlassBar(title = title, smallLine = line, back = back, wide = wide, barHeight = LayoutMetrics.topBarScript) {
        if (positionLabel != null) LensChip(positionLabel, LensChipState.SCRIPT_POSITION, onPositionLabel)
        if (onFind != null) io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton(
            io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec.SEARCH, onFind,
        )
        if (onLayers != null) io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton(
            io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec.DISPLAY_LAYERS, onLayers,
        )
    }
}

/**
 * The command field of the `에셋` screen (SCR-61): idle it is the DS command bar with the hint (no mic, S-8);
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
    focusRequester: FocusRequester,
    /** The field's hint: the script's (SCR-68) or another script screen's (`에셋`, SCR-61). */
    hint: String = ScriptCommands.HINT,
) {
    if (!typing) {
        CommandBar(hint = hint, onClick = onStart)
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
                // A narrow column (1:2 split, 280 dp) cannot hold all six tools: `◆` and undo stay pinned at
                // the end, the rest scroll, and a faded edge shows that more lies that way (emulator QA).
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(TOOLBAR)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val scroll = rememberScrollState()
                    val fadeStart = animatedFraction(scroll.canScrollBackward, label = "toolsFadeStart")
                    val fadeEnd = animatedFraction(scroll.canScrollForward, label = "toolsFadeEnd")
                    Row(
                        Modifier
                            .weight(1f, fill = false)
                            .scrollEdgeFade(fadeStart, fadeEnd, EDGE_FADE)
                            .horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KindTool(kindLabel + ScriptCopy.KIND_CHEVRON, onKind)
                        if (showSpeaker) SpeakerTool(speakerName ?: ScriptCopy.NO_SPEAKER, onSpeaker)
                        if (onParen != null) {
                            KeyboardTool(onClick = onParen, text = ScriptCopy.PAREN_TOOL, on = parenOn, contentDescription = ScriptCopy.PAREN_LABEL)
                        }
                        KeyboardTool(onClick = onLink, text = ScriptCopy.LINK_TOOL)
                    }
                    Box(Modifier.dimmedWhen(onChange == null)) {
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

/** Width of the faded edge on the scrolling part of the toolbar. */
private val EDGE_FADE: Dp = 24.dp

/**
 * Fades the viewport's start and end edges ([start] and [end] are 0..1, 1 = fully faded) so tools cut
 * by the edge read as "more this way". Goes before `horizontalScroll`, so it draws on the viewport.
 */
private fun Modifier.scrollEdgeFade(start: State<Float>, end: State<Float>, width: Dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val w = width.toPx().coerceAtMost(size.width / 2f)
            val s = start.value
            val e = end.value
            if (s > 0f) {
                drawRect(
                    Brush.horizontalGradient(0f to Color.Black.copy(alpha = 1f - s), 1f to Color.Black, startX = 0f, endX = w),
                    size = Size(w, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (e > 0f) {
                drawRect(
                    Brush.horizontalGradient(0f to Color.Black, 1f to Color.Black.copy(alpha = 1f - e), startX = size.width - w, endX = size.width),
                    topLeft = Offset(size.width - w, 0f),
                    size = Size(w, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
        }

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
