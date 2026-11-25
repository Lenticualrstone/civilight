package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtons
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Callbacks of the input bar. Every write is the caller's; the bar only reports taps and text. */
class ScriptInputActions(
    val onEvent: (ScriptInputEvent) -> Unit,
    val onSend: () -> Unit,
    val onSpeakerChip: () -> Unit = {},
    val onKindChip: () -> Unit = {},
    val onNextScene: () -> Unit = {},
    val onSplit: () -> Unit = {},
    val onMerge: () -> Unit = {},
    val onParen: () -> Unit = {},
    val onLink: () -> Unit = {},
    val onChange: () -> Unit = {},
    val onUndo: () -> Unit = {},
)

/**
 * SCR-70 input bar (U-14), stateless: [state] comes from [ScriptInput.reduce] in the caller, every change goes
 * out through [actions]. Rows top to bottom, 6 dp apart (4 with the keyboard): the tool row `( ) · @ · ◆ · ↶`
 * (only with the keyboard up and the field focused), the chip row (speaker and kind chips, the place area at
 * its right) and the field row (field + send). The field grows from 1 to 6 lines, then scrolls inside itself
 * and keeps the caret line in view ([rememberCaretFollow], DS-69). Enter is a line break; only the send button
 * sends. [mergeOffReason] (`화자 다름` · `종류 다름` · scene start) dims `합치기`; null leaves it on.
 * [cues] draws the 연출 cue chips above the text (the caller passes it only with `게임 제작 도구` on).
 * [glass] false leaves out the bar's own glass and insets (a host that draws its own bar).
 * [nextSceneEnabled] false dims `다음 씬` (`ink3`, not tappable, name `다음 씬 만들기 · 꺼짐`): the caller's rule
 * (an empty scene would only be followed by another empty one).
 */
@Composable
fun ScriptInputBar(
    state: ScriptInputState,
    actions: ScriptInputActions,
    modifier: Modifier = Modifier,
    context: InputContext = InputContext(),
    mergeOffReason: String? = null,
    speakerMenuOpen: Boolean = false,
    kindMenuOpen: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    cues: (@Composable () -> Unit)? = null,
    glass: Boolean = true,
    nextSceneEnabled: Boolean = true,
) {
    val ime = keyboardUp()
    val currentEvent by rememberUpdatedState(actions.onEvent)
    LaunchedEffect(ime) { currentEvent(ScriptInputEvent.Ime(ime)) }
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gap = if (ime) 4.dp else 6.dp
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = if (glass) nav + (if (ime) 6.dp else 16.dp) else 0.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            if (ScriptInputRules.showsToolRow(state, context.readOnly)) InputToolRow(state, context, actions)
            InputChipRow(state, actions, context.readOnly, mergeOffReason, speakerMenuOpen, kindMenuOpen, nextSceneEnabled)
            InputFieldRow(state, actions, context, visualTransformation, cues)
        }
    }
    if (!glass) {
        Box(modifier.fillMaxWidth()) { body() }
        return
    }
    Box(modifier.windowInsetsPadding(KeyboardLift).fillMaxWidth()) {
        GlassBackground(GlassSurface.BOTTOM_BAR, Modifier.matchParentSize())
        OnGlass { body() }
    }
}

/** SCR-16 tool row: 44 × 44 items, radius 12, 4 apart, left-aligned. */
@Composable
private fun InputToolRow(state: ScriptInputState, context: InputContext, actions: ScriptInputActions) {
    val colors = CivilightTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        InputTool(ScriptInputCopy.TOOL_PAREN_NAME, ScriptInputRules.parenEnabled(state), actions.onParen) {
            CivilightText(ScriptInputCopy.TOOL_PAREN, TOOL_TEXT, color = colors.ink2, maxLines = 1)
        }
        InputTool(ScriptInputCopy.TOOL_LINK_NAME, true, actions.onLink) {
            CivilightText(ScriptInputCopy.TOOL_LINK, TOOL_TEXT, color = colors.ink2, maxLines = 1)
        }
        InputTool(ScriptInputCopy.TOOL_CHANGE_NAME, ScriptInputRules.changeEnabled(state, context), actions.onChange) {
            CivilightText(ScriptInputCopy.TOOL_CHANGE, TOOL_TEXT, color = colors.acc, maxLines = 1)
        }
        InputTool(ScriptInputCopy.TOOL_UNDO_NAME, context.canUndo, actions.onUndo) {
            CivilightIcon(IconId.Undo, contentDescription = null, size = 18.dp, tint = colors.ink2)
        }
    }
}

private val TOOL_TEXT: TextStyle
    @Composable get() = CivilightTheme.type.button.copy(fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun InputTool(name: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.BUTTON, shape = shape)
            .size(44.dp)
            .clip(shape)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The chip row: speaker and kind chips (class c), then the place area, flex and right-aligned. */
@Composable
private fun InputChipRow(
    state: ScriptInputState,
    actions: ScriptInputActions,
    readOnly: Boolean,
    mergeOffReason: String?,
    speakerMenuOpen: Boolean,
    kindMenuOpen: Boolean,
    nextSceneEnabled: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ValueChip(
            name = ScriptInputCopy.SPEAKER,
            value = state.speaker?.label ?: ScriptInputCopy.NO_SPEAKER,
            dimmed = ScriptInputRules.speakerDimmed(state),
            open = speakerMenuOpen,
            onClick = actions.onSpeakerChip,
        )
        ValueChip(
            name = ScriptInputCopy.KIND,
            value = state.kind.label,
            dimmed = false,
            open = kindMenuOpen,
            onClick = actions.onKindChip,
        )
        PlaceArea(state, actions, readOnly, mergeOffReason, nextSceneEnabled)
    }
}

/**
 * SCR-70 chip: 36 tall, radius 18, `chip` fill, 600 13 / 16 `ink`, padding 0 8 0 11, chevron 14. Dimmed: `tint`
 * fill, 1 dp `line` inset, 500 `ink3`, not tappable, value kept. Open (menu up): 1.5 dp `acc` inset.
 */
@Composable
private fun ValueChip(name: String, value: String, dimmed: Boolean, open: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val content = if (dimmed) colors.ink3 else colors.ink
    Row(
        Modifier
            .pressable(onClick = onClick, enabled = !dimmed, hitTarget = HitTarget.CHIP, shape = shape)
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(if (dimmed) colors.tint else colors.chip)
            .then(
                when {
                    dimmed -> Modifier.insetOutline(colors.line, 1.dp, shape)
                    open -> Modifier.insetOutline(colors.acc, 1.5.dp, shape)
                    else -> Modifier
                },
            )
            .padding(start = 11.dp, end = 8.dp)
            .semantics {
                contentDescription = name
                stateDescription = value
                role = Role.DropdownList
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CivilightText(
            value,
            CivilightTheme.type.chip.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = if (dimmed) FontWeight.Medium else FontWeight.SemiBold),
            Modifier.widthIn(max = 96.dp),
            color = content,
            maxLines = 1,
        )
        CivilightIcon(IconId.Down, contentDescription = null, size = 14.dp, tint = content)
    }
}

/** The place area: one of `씬 3 끝에 · 다음 씬`, `씬 없음 · 다음 씬`, `#044 뒤에 · ✕`, `#044 고치기 · 나누기 · 합치기 · ✕`. */
@Composable
private fun RowScope.PlaceArea(state: ScriptInputState, actions: ScriptInputActions, readOnly: Boolean, mergeOffReason: String?, nextSceneEnabled: Boolean) {
    val colors = CivilightTheme.colors
    val place = ScriptInputRules.place(state)
    Row(
        Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (place) {
            is InputPlace.SceneEnd -> {
                SceneEndLabel(place.number, state.sceneFlash)
                if (ScriptInputRules.showsNextScene(state, readOnly)) NextSceneButton(nextSceneEnabled, actions.onNextScene)
            }
            InputPlace.NoScene -> {
                CivilightText(ScriptInputCopy.NO_SCENE, PLACE_TEXT, color = colors.ink3, maxLines = 1)
                if (ScriptInputRules.showsNextScene(state, readOnly)) NextSceneButton(nextSceneEnabled, actions.onNextScene)
            }
            is InputPlace.After -> {
                CivilightText(ScriptInputCopy.after(place.number), PLACE_BOLD, Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1)
                MarkClose { actions.onEvent(ScriptInputEvent.CloseMark) }
            }
            is InputPlace.Editing -> {
                CivilightText(ScriptInputCopy.editing(place.number), PLACE_BOLD, Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1)
                MarkButton(IconId.Split, IconButtonCopy.SPLIT, enabled = ScriptInputRules.canSplit(state), reason = null, onClick = actions.onSplit)
                MarkButton(IconId.Merge, IconButtonCopy.MERGE, enabled = mergeOffReason == null, reason = mergeOffReason, onClick = actions.onMerge)
                MarkClose { actions.onEvent(ScriptInputEvent.CloseMark) }
            }
        }
    }
}

/** Place area text: 500 12 / 16 (bold parts 600). */
private val PLACE_TEXT: TextStyle
    @Composable get() = CivilightTheme.type.label.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

private val PLACE_BOLD: TextStyle
    @Composable get() = PLACE_TEXT.copy(fontWeight = FontWeight.SemiBold)

/**
 * `씬 3 끝에` (`씬 3` bold). When [flash] grows the label flashes once (U-14.21, SCR-71): a `sel` fill (radius 8)
 * fades in and out over `placeFlash` while a 2 dp `acc` bar runs out along its bottom edge. Reduced motion:
 * the fill only, as a 120 ms fade, and no bar.
 */
@Composable
private fun SceneEndLabel(number: Int, flash: Int) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val fill = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }
    var seen by remember { mutableStateOf(flash) }
    LaunchedEffect(flash) {
        if (flash == seen) return@LaunchedEffect
        seen = flash
        val third = CivilightMotion.placeFlash.durationMs / 3
        launch {
            bar.snapTo(0f)
            if (!motion.reduced) bar.animateTo(1f, motion.move(CivilightMotion.placeFlash))
        }
        fill.animateTo(1f, motion.spec(CivilightMotion.standard))
        delay(motion.delayMs(third))
        fill.animateTo(0f, motion.spec(CivilightMotion.standard))
        bar.snapTo(0f)
    }
    val text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.ink)) { append(ScriptInputCopy.scene(number)) }
        append(" ")
        append(ScriptInputCopy.END)
    }
    Box(
        Modifier
            .heightIn(min = 26.dp)
            .drawBehind {
                val a = fill.value
                if (a > 0f) drawRoundRect(colors.sel, alpha = a, cornerRadius = CornerRadius(8.dp.toPx()))
                val b = bar.value
                if (b > 0f && a > 0f) {
                    val h = 2.dp.toPx()
                    drawRect(colors.acc, topLeft = Offset(0f, size.height - h), size = Size(size.width * b, h), alpha = a)
                }
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.text.BasicText(text, style = PLACE_TEXT.copy(color = colors.ink3), maxLines = 1)
    }
}

/**
 * `다음 씬` (class b): icon `next_scene` 16 + text, `acc` 600, accessibility name `다음 씬 만들기`. Off: `ink3` 500,
 * not tappable, name `다음 씬 만들기 · 꺼짐`.
 */
@Composable
private fun NextSceneButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val content = if (enabled) colors.acc else colors.ink3
    IconLabelButton(
        icon = IconId.NextScene,
        label = IconButtonCopy.NEXT_SCENE,
        onClick = onClick,
        contentDescription = if (enabled) IconButtonCopy.NEXT_SCENE_NAME else IconButtons.offName(IconButtonCopy.NEXT_SCENE_NAME),
        tint = content,
        labelColor = content,
        labelStyle = if (enabled) PLACE_BOLD else PLACE_TEXT,
        iconSize = 16.dp,
        enabled = enabled,
    )
}

/**
 * `나누기` · `합치기` (class b, `split` · `merge`): on `acc` 600; off `ink3` 500, not tappable, with the
 * reason in 500 11 / 13 under the word and the name `합치기 · 꺼짐 · 종류 다름`.
 */
@Composable
private fun MarkButton(icon: IconId, label: String, enabled: Boolean, reason: String?, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val content = if (enabled) colors.acc else colors.ink3
    val name = if (enabled) label else IconButtons.offName(label, reason)
    Column(
        Modifier
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.BUTTON, shape = RoundedCornerShape(8.dp))
            .heightIn(min = 44.dp)
            .padding(horizontal = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = name },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CivilightIcon(icon, contentDescription = null, size = 16.dp, tint = content)
            CivilightText(
                label,
                if (enabled) PLACE_BOLD else PLACE_TEXT,
                color = content,
                maxLines = 1,
            )
        }
        if (!enabled && reason != null) {
            CivilightText(reason, CivilightTheme.type.small.copy(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium), color = colors.ink3, maxLines = 1)
        }
    }
}

/** `✕` of a mark: icon `close` 16, 28 drawn, 44 target, name `닫기`. */
@Composable
private fun MarkClose(onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Box(
        Modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.SMALL, shape = CircleShape)
            .size(28.dp)
            .semantics { contentDescription = IconButtonCopy.CLOSE; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(IconId.X, contentDescription = null, size = 16.dp, tint = colors.ink2)
    }
}

/**
 * The field row: radius 24, `bg`, 1 dp `edge` inset, padding 6 6 6 12, items bottom-aligned 6 apart. The
 * field is serif 15 / 24; it grows to 6 lines, then scrolls in its own box (3 dp bar, 18 dp top fade).
 */
@Composable
private fun InputFieldRow(
    state: ScriptInputState,
    actions: ScriptInputActions,
    context: InputContext,
    visualTransformation: VisualTransformation,
    cues: (@Composable () -> Unit)?,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.bg)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(start = 12.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            if (cues != null) cues()
            InputField(state, actions, visualTransformation)
        }
        SendButton(state, ScriptInputRules.canSend(state, context), actions.onSend)
    }
}

/** The field's text style: serif 15 / 24 (`line` family). */
@Composable
internal fun inputFieldTextStyle(): TextStyle =
    CivilightTheme.type.line.merge(TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = CivilightTheme.colors.ink))

@Composable
private fun InputField(state: ScriptInputState, actions: ScriptInputActions, visualTransformation: VisualTransformation) {
    val colors = CivilightTheme.colors
    val style = inputFieldTextStyle()
    val density = LocalDensity.current
    val lineHeight: Dp = with(density) { style.lineHeight.toDp() }
    // The held value keeps the IME's composing range (Hangul syllables are assembled in it); it is replaced
    // only when the state's text or selection moved without the field (고치기 start, send, restore).
    var held by remember { mutableStateOf(TextFieldValue(state.text, TextRange(state.selectionStart, state.selectionEnd))) }
    val stateSelection = TextRange(state.selectionStart, state.selectionEnd)
    val value = if (held.text == state.text && held.selection == stateSelection) held else TextFieldValue(state.text, stateSelection)
    val follow = rememberCaretFollow(value, state.focused) {
        if (visualTransformation === VisualTransformation.None) it.selection.end
        else visualTransformation.filter(androidx.compose.ui.text.AnnotatedString(it.text)).offsetMapping.originalToTransformed(it.selection.end)
    }
    val scroll = rememberScrollState()
    val fade = animatedAlpha(if (scroll.value > 0) 1f else 0f, label = "inputFade")
    BasicTextField(
        value = value,
        onValueChange = {
            held = it
            actions.onEvent(ScriptInputEvent.TextChanged(it.text, it.selection.start, it.selection.end))
        },
        textStyle = style,
        cursorBrush = SolidColor(colors.acc),
        // Enter is a line break (U-14.7): the default IME action of a multi-line field, never a send action.
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Default),
        singleLine = false,
        visualTransformation = visualTransformation,
        onTextLayout = follow::onTextLayout,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { actions.onEvent(ScriptInputEvent.Focus(it.isFocused)) },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp, max = lineHeight * ScriptInputRules.MAX_VISIBLE_LINES + 12.dp)
                    .drawWithContent {
                        drawContent()
                        // 3 dp scroll bar and an 18 dp fade at the top while the field scrolls inside itself.
                        val max = scroll.maxValue
                        if (max > 0 && max != Int.MAX_VALUE) {
                            val w = 3.dp.toPx()
                            val track = size.height
                            val thumb = (track * track / (track + max)).coerceAtLeast(12.dp.toPx())
                            val y = (track - thumb) * scroll.value / max
                            drawRect(colors.line, topLeft = Offset(size.width - w, 0f), size = Size(w, track))
                            drawRect(colors.ink3, topLeft = Offset(size.width - w, y), size = Size(w, thumb))
                        }
                        val f = fade.value
                        if (f > 0f) {
                            drawRect(
                                Brush.verticalGradient(listOf(colors.bg, Color.Transparent), endY = 18.dp.toPx()),
                                size = Size(size.width, 18.dp.toPx()),
                                alpha = f,
                            )
                        }
                    }
                    .verticalScroll(scroll)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val transformedText = if (visualTransformation === VisualTransformation.None) state.text
                else visualTransformation.filter(androidx.compose.ui.text.AnnotatedString(state.text)).text.text
                if (state.text.isEmpty() && transformedText.isEmpty()) {
                    CivilightText(ScriptInputCopy.PLACEHOLDER, style, color = colors.ink3, maxLines = 1)
                }
                Box(Modifier.caretFollow(follow)) { inner() }
            }
        },
    )
}

/**
 * Send: 36 tall, radius 18, `pri` / `onpri`, 44 target. Icon `send` 20 named `보내기` (a); during `고치기` the
 * text `고치기` (c). Off: `tint` / `ink3`, not tappable.
 */
@Composable
private fun SendButton(state: ScriptInputState, enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val slot = ScriptInputRules.sendSlot(state)
    val content = if (enabled) colors.onpri else colors.ink3
    val name = if (slot == SendSlot.ICON) IconButtonCopy.SEND else IconButtonCopy.EDIT
    Box(
        Modifier
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.ICON_BUTTON, shape = shape)
            .heightIn(min = 36.dp)
            .widthIn(min = if (slot == SendSlot.ICON) 44.dp else 0.dp)
            .clip(shape)
            .background(if (enabled) colors.pri else colors.tint)
            .padding(horizontal = if (slot == SendSlot.ICON) 0.dp else 13.dp)
            .semantics { contentDescription = name; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        if (slot == SendSlot.ICON) {
            CivilightIcon(IconId.Send, contentDescription = null, size = IconButtons.ICON_ONLY_DP.dp, tint = content)
        } else {
            CivilightText(IconButtonCopy.EDIT, CivilightTheme.type.button.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold), color = content, maxLines = 1)
        }
    }
}

/**
 * SCR-60 read-only row in place of the input bar: a 96 dp glass bar with a lock 16 (`acc`) and [label] at the
 * left, and [secondaryLabel] (ghost) · [primaryLabel] (primary) at the right. The wording is the caller's
 * (scope U-14.15 and SCR-60 word it differently, Q-41); the defaults follow SCR-60.
 */
@Composable
fun ScriptInputReadOnlyRow(
    onSecondary: () -> Unit,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = ScriptInputCopy.READ_ONLY_LABEL,
    secondaryLabel: String = ScriptInputCopy.READ_ONLY_HOME,
    primaryLabel: String = ScriptInputCopy.READ_ONLY_FINISH,
    glass: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val row: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .padding(start = 12.dp, end = 12.dp, bottom = if (glass) nav + 16.dp else 0.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CivilightIcon(IconId.Lock, contentDescription = null, size = 16.dp, tint = colors.acc)
            CivilightText(label, CivilightTheme.type.button.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold), Modifier.weight(1f), color = colors.ink, maxLines = 1)
            CivilightButton(secondaryLabel, onSecondary, variant = ButtonVariant.COMPACT_GHOST)
            CivilightButton(primaryLabel, onPrimary, variant = ButtonVariant.COMPACT_PRIMARY)
        }
    }
    if (!glass) {
        Box(modifier.fillMaxWidth()) { row() }
        return
    }
    Box(modifier.fillMaxWidth()) {
        GlassBackground(GlassSurface.BOTTOM_BAR, Modifier.matchParentSize())
        OnGlass { row() }
    }
}
