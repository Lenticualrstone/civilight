package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtons
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    /** (0.7.0) Redo callback. */
    val onRedo: () -> Unit = {},
    /** (0.7.0) 3-mode composer tab selection (`대사 | 지문 | 장면`). */
    val onSelectTab: (ComposerTab) -> Unit = {},
    /** (0.7.0) Contextual leading slot tap (character popup / attachment popup / scene preset popup). */
    val onLeadingSlot: () -> Unit = {},
    /** (0.7.0) Fired when tapping send on an empty draft to trigger shake feedback. */
    val onEmptySend: () -> Unit = {},
)

private val DOCK_RADIUS = 26.dp
private val DOCK_SHAPE = RoundedCornerShape(topStart = DOCK_RADIUS, topEnd = DOCK_RADIUS, bottomStart = 0.dp, bottomEnd = 0.dp)
private val DOCK_MAX_WIDTH = 640.dp
private val LEADING_SLOT_SIZE = 42.dp

/**
 * (0.7.0) Docked Glass Arch script input bar:
 * - Upper corners rounded (26 dp) with specular glass rim highlight, hugging the side bezels and flowing cleanly
 *   down into the navigation inset / keyboard while frost-blurring the manuscript behind it.
 * - Streamlined 3-mode segmented tabs (`대사 | 지문 | 장면`) with 1-tap switching and hardware Tab cycling.
 * - Fixed 42 dp contextual leading slot (character portrait in Dialogue, attach/cues in Narration, scene preset in Scene)
 *   so the input field never shifts horizontally across modes.
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
    /** (0.7.0) Wide/unfolded screen centers the arched glass dock to the manuscript column width. */
    wide: Boolean = false,
    /** (0.7.0) Caller-supplied 42 dp contextual leading slot (portrait cluster / attach icon / scene icon). */
    leadingSlot: (@Composable () -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val haptics = rememberHaptics()
    val ime = keyboardUp()
    val currentEvent by rememberUpdatedState(actions.onEvent)
    LaunchedEffect(ime) { currentEvent(ScriptInputEvent.Ime(ime)) }

    // Subtle horizontal shake when trying to send an empty draft.
    val shakeX = remember { Animatable(0f) }
    var lastShake by remember { mutableStateOf(state.shakeCount) }
    LaunchedEffect(state.shakeCount) {
        if (state.shakeCount != lastShake) {
            lastShake = state.shakeCount
            haptics.perform(HapticToken.TICK)
            if (!motion.reduced) {
                val spec = motion.spec<Float>(CivilightMotion.press)
                shakeX.animateTo(-7f, spec)
                shakeX.animateTo(6f, spec)
                shakeX.animateTo(-3f, spec)
                shakeX.animateTo(0f, spec)
            }
        }
    }

    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gap = if (ime) 6.dp else 8.dp
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeX.value.roundToInt(), 0) }
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 12.dp,
                    bottom = if (glass) nav + (if (ime) 8.dp else 14.dp) else 4.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            if (ScriptInputRules.showsToolRow(state, context.readOnly)) {
                InputToolRow(state, context, actions)
            }
            ModeAndPlaceRow(state, context, actions, mergeOffReason)
            InputFieldRow(state, actions, context, visualTransformation, cues, leadingSlot, speakerMenuOpen)
        }
    }

    if (!glass) {
        Box(modifier.fillMaxWidth()) { body() }
        return
    }

    val dockModifier = if (wide) {
        Modifier.widthIn(max = DOCK_MAX_WIDTH).fillMaxWidth()
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        modifier
            .windowInsetsPadding(KeyboardLift)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // (0.7.3) Sonnet-Create floating glass capsule banner while editing a block.
        if (state.editing) {
            val editPlace = ScriptInputRules.place(state) as? InputPlace.Editing
            val bannerShape = CircleShape
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .tokenShadow(bannerShape, CivilightTheme.elevation.popover(colors))
                    .clip(bannerShape),
            ) {
                GlassBackground(
                    surface = GlassSurface.POPOVER,
                    modifier = Modifier.matchParentSize(),
                    shape = bannerShape,
                    fill = colors.glass.copy(alpha = if (colors.isDark) 0.72f else 0.84f),
                )
                OnGlass {
                    Row(
                        Modifier
                            .insetOutline(colors.edge.copy(alpha = if (colors.isDark) 0.35f else 0.18f), 1.dp, bannerShape)
                            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CivilightIcon(IconId.Edit, contentDescription = null, size = 13.dp, tint = colors.acc)
                        CivilightText(
                            editPlace?.let { ScriptInputCopy.editing(it.number) } ?: IconButtonCopy.EDIT,
                            PLACE_BOLD,
                            color = colors.ink,
                            maxLines = 1,
                        )
                        MarkButton(IconId.Split, IconButtonCopy.SPLIT, enabled = ScriptInputRules.canSplit(state), reason = null, onClick = actions.onSplit)
                        MarkButton(IconId.Merge, IconButtonCopy.MERGE, enabled = mergeOffReason == null, reason = mergeOffReason, onClick = actions.onMerge)
                        MarkClose { actions.onEvent(ScriptInputEvent.CloseMark) }
                    }
                }
            }
        }
        Box(
            dockModifier
                .tokenShadow(DOCK_SHAPE, CivilightTheme.elevation.popover(colors))
                .clip(DOCK_SHAPE)
                .drawBehind {
                    // Subtle ambient tint behind glass in opaque/scrim fallback
                    drawRect(colors.glass.copy(alpha = 0.08f))
                },
        ) {
            GlassBackground(
                surface = GlassSurface.DOCK,
                modifier = Modifier.matchParentSize(),
                shape = DOCK_SHAPE,
                fill = colors.glass.copy(alpha = if (colors.isDark) 0.62f else 0.72f),
            )
            // Specular arched top rim highlight: luminous on the rounded crown, fading down the bezel sides.
            Box(
                Modifier
                    .matchParentSize()
                    .drawWithContent {
                        drawContent()
                        val strokePx = 1.dp.toPx()
                        val r = DOCK_RADIUS.toPx()
                        val w = size.width
                        val h = size.height
                        val archPath = Path().apply {
                            moveTo(strokePx / 2f, h)
                            lineTo(strokePx / 2f, r)
                            arcTo(
                                rect = Rect(strokePx / 2f, strokePx / 2f, r * 2f, r * 2f),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false,
                            )
                            lineTo(w - r, strokePx / 2f)
                            arcTo(
                                rect = Rect(w - r * 2f, strokePx / 2f, w - strokePx / 2f, r * 2f),
                                startAngleDegrees = 270f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false,
                            )
                            lineTo(w - strokePx / 2f, h)
                        }
                        val rimBrush = Brush.verticalGradient(
                            0f to if (colors.isDark) Color.White.copy(alpha = 0.24f) else colors.edge.copy(alpha = 0.20f),
                            0.45f to if (colors.isDark) Color.White.copy(alpha = 0.08f) else colors.edge.copy(alpha = 0.07f),
                            1f to Color.Transparent,
                        )
                        drawPath(archPath, brush = rimBrush, style = Stroke(width = strokePx))
                    },
            )
            OnGlass { body() }
        }
    }
}

/** SCR-16 tool row: `( ) · @ · ◆ · ↶ · ↷` shown while keyboard is up and field focused. */
@Composable
private fun InputToolRow(state: ScriptInputState, context: InputContext, actions: ScriptInputActions) {
    val colors = CivilightTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InputTool(ScriptInputCopy.TOOL_PAREN_NAME, ScriptInputRules.parenEnabled(state) && !state.sceneMode, actions.onParen) {
            CivilightText(ScriptInputCopy.TOOL_PAREN, TOOL_TEXT, color = colors.ink2, maxLines = 1)
        }
        InputTool(ScriptInputCopy.TOOL_LINK_NAME, !state.sceneMode, actions.onLink) {
            CivilightText(ScriptInputCopy.TOOL_LINK, TOOL_TEXT, color = colors.ink2, maxLines = 1)
        }
        InputTool(ScriptInputCopy.TOOL_CHANGE_NAME, ScriptInputRules.changeEnabled(state, context), actions.onChange) {
            CivilightText(ScriptInputCopy.TOOL_CHANGE, TOOL_TEXT, color = colors.acc, maxLines = 1)
        }
        Box(Modifier.weight(1f))
        InputTool(ScriptInputCopy.TOOL_UNDO_NAME, context.canUndo, actions.onUndo) {
            CivilightIcon(IconId.Undo, contentDescription = null, size = 18.dp, tint = colors.ink2)
        }
        InputTool(ScriptInputCopy.TOOL_REDO_NAME, context.canRedo, actions.onRedo) {
            CivilightIcon(IconId.Redo, contentDescription = null, size = 18.dp, tint = colors.ink2)
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
            .size(38.dp)
            .clip(shape)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * (0.7.0) Row 1 inside the docked glass arch:
 * Left = streamlined 3-mode sliding tabs (`대사 | 지문 | 장면`),
 * Right = compact place indicator (`씬 N` / `#044 뒤에 ✕` / `#044 고치기 · 나누기 · 합치기 ✕`) + quick Undo/Redo when IME is down.
 */
@Composable
private fun ModeAndPlaceRow(
    state: ScriptInputState,
    context: InputContext,
    actions: ScriptInputActions,
    mergeOffReason: String?,
) {
    val colors = CivilightTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ComposerModeTabs(
            active = state.activeTab,
            editing = state.editing,
            onSelect = actions.onSelectTab,
        )
        PlaceArea(state, context, actions, mergeOffReason)
    }
}

@Composable
private fun ComposerModeTabs(
    active: ComposerTab,
    editing: Boolean,
    onSelect: (ComposerTab) -> Unit,
) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val tabs = if (editing) listOf(ComposerTab.DIALOGUE, ComposerTab.NARRATION) else ComposerTab.entries
    val pillShape = RoundedCornerShape(16.dp)
    val tabShape = RoundedCornerShape(14.dp)
    val tabWidth = 48.dp
    val tabGap = 2.dp
    val activeIdx = tabs.indexOf(active).coerceAtLeast(0)
    // (0.7.3) Sonnet-Create sliding capsule highlight gliding smoothly between `대사 | 지문 | 장면`.
    val indicatorOffset by animateDpAsState(
        targetValue = (tabWidth + tabGap) * activeIdx,
        animationSpec = motion.move(CivilightMotion.standard),
        label = "modeTabSlide",
    )
    Box(
        Modifier
            .clip(pillShape)
            .background(colors.tint)
            .insetOutline(colors.line, 1.dp, pillShape)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(28.dp)
                .clip(tabShape)
                .background(colors.hl)
                .insetOutline(colors.acc.copy(alpha = 0.35f), 1.dp, tabShape),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(tabGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == active
                Box(
                    Modifier
                        .width(tabWidth)
                        .height(28.dp)
                        .clip(tabShape)
                        .pressable(onClick = { onSelect(tab) }, hitTarget = HitTarget.CHIP, shape = tabShape, scaleOnPress = false)
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    CivilightText(
                        tab.label,
                        CivilightTheme.type.chip.copy(
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (isSelected) colors.acc else colors.ink2,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** The place area: `씬 3`, `씬 없음`, `#044 뒤에 · ✕`, `#044 고치기 · 나누기 · 합치기 · ✕`. */
@Composable
private fun RowScope.PlaceArea(
    state: ScriptInputState,
    context: InputContext,
    actions: ScriptInputActions,
    mergeOffReason: String?,
) {
    val colors = CivilightTheme.colors
    val place = ScriptInputRules.place(state)
    Row(
        Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (place) {
            is InputPlace.SceneEnd -> {
                // Keep normal writing clean (top bar already displays the current scene); show compact Undo/Redo when available.
                if (!ScriptInputRules.showsToolRow(state, context.readOnly) && (context.canUndo || context.canRedo)) {
                    CompactHistoryIcon(IconId.Undo, ScriptInputCopy.TOOL_UNDO_NAME, context.canUndo, actions.onUndo)
                    CompactHistoryIcon(IconId.Redo, ScriptInputCopy.TOOL_REDO_NAME, context.canRedo, actions.onRedo)
                }
            }
            InputPlace.NoScene -> {
                CivilightText(ScriptInputCopy.NO_SCENE, PLACE_TEXT, color = colors.ink3, maxLines = 1)
            }
            is InputPlace.After -> {
                CivilightText(ScriptInputCopy.after(place.number), PLACE_BOLD, Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1)
                MarkClose { actions.onEvent(ScriptInputEvent.CloseMark) }
            }
            is InputPlace.Editing -> {
                // Edit controls are elevated into the floating glass edit banner above the dock.
                if (!ScriptInputRules.showsToolRow(state, context.readOnly) && (context.canUndo || context.canRedo)) {
                    CompactHistoryIcon(IconId.Undo, ScriptInputCopy.TOOL_UNDO_NAME, context.canUndo, actions.onUndo)
                    CompactHistoryIcon(IconId.Redo, ScriptInputCopy.TOOL_REDO_NAME, context.canRedo, actions.onRedo)
                }
            }
        }
    }
}

@Composable
private fun CompactHistoryIcon(icon: IconId, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Box(
        Modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.SMALL, shape = CircleShape)
            .size(28.dp)
            .semantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(icon, contentDescription = null, size = 16.dp, tint = colors.ink2)
    }
}

/** Place area text: 500 12 / 16 (bold parts 600). */
private val PLACE_TEXT: TextStyle
    @Composable get() = CivilightTheme.type.label.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

private val PLACE_BOLD: TextStyle
    @Composable get() = PLACE_TEXT.copy(fontWeight = FontWeight.SemiBold)

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
    }
    Box(
        Modifier
            .heightIn(min = 24.dp)
            .drawBehind {
                val a = fill.value
                if (a > 0f) drawRoundRect(colors.sel, alpha = a, cornerRadius = CornerRadius(8.dp.toPx()))
                val b = bar.value
                if (b > 0f && a > 0f) {
                    val h = 2.dp.toPx()
                    drawRect(colors.acc, topLeft = Offset(0f, size.height - h), size = Size(size.width * b, h), alpha = a)
                }
            }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.text.BasicText(text, style = PLACE_TEXT.copy(color = colors.ink3), maxLines = 1)
    }
}

@Composable
private fun MarkButton(icon: IconId, label: String, enabled: Boolean, reason: String?, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val content = if (enabled) colors.acc else colors.ink3
    val name = if (enabled) label else IconButtons.offName(label, reason)
    Column(
        Modifier
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.BUTTON, shape = RoundedCornerShape(8.dp))
            .heightIn(min = 36.dp)
            .padding(horizontal = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = name },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            CivilightIcon(icon, contentDescription = null, size = 15.dp, tint = content)
            CivilightText(label, if (enabled) PLACE_BOLD else PLACE_TEXT, color = content, maxLines = 1)
        }
        if (!enabled && reason != null) {
            CivilightText(reason, CivilightTheme.type.small.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium), color = colors.ink3, maxLines = 1)
        }
    }
}

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
 * (0.7.0) Row 2 inside the docked glass arch:
 * `[Leading 42dp slot]` + `[Open airy text field on glass]` + `[Send button]`.
 */
@Composable
private fun InputFieldRow(
    state: ScriptInputState,
    actions: ScriptInputActions,
    context: InputContext,
    visualTransformation: VisualTransformation,
    cues: (@Composable () -> Unit)?,
    leadingSlot: (@Composable () -> Unit)?,
    popupOpen: Boolean,
) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val canSend = ScriptInputRules.canSend(state, context)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Fixed-size contextual leading slot — never shifts across modes!
        val slotShape = RoundedCornerShape(14.dp)
        val slotDesc = when (state.activeTab) {
            ComposerTab.DIALOGUE -> state.speaker?.label ?: ScriptInputCopy.LEADING_SPEAKER
            ComposerTab.NARRATION -> ScriptInputCopy.LEADING_ATTACH
            ComposerTab.SCENE -> ScriptInputCopy.LEADING_SCENE
        }
        Box(
            Modifier
                .size(LEADING_SLOT_SIZE)
                .clip(slotShape)
                .then(if (popupOpen) Modifier.insetOutline(colors.acc, 1.5.dp, slotShape) else Modifier)
                .pressable(onClick = actions.onLeadingSlot, hitTarget = HitTarget.BUTTON, shape = slotShape)
                .semantics { contentDescription = slotDesc; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            if (leadingSlot != null) {
                leadingSlot()
            } else {
                AnimatedContent(
                    targetState = state.activeTab,
                    transitionSpec = { motion.enter(CivilightMotion.standard) togetherWith motion.exit(CivilightMotion.standard) },
                    label = "leadingSlotIcon",
                ) { tab ->
                    Box(
                        Modifier
                            .size(LEADING_SLOT_SIZE)
                            .clip(slotShape)
                            .background(colors.tint)
                            .insetOutline(colors.line, 1.dp, slotShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val icon = when (tab) {
                            ComposerTab.DIALOGUE -> IconId.User
                            ComposerTab.NARRATION -> IconId.Attach
                            ComposerTab.SCENE -> IconId.Pin
                        }
                        CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink2)
                    }
                }
            }
        }

        // Open transparent input area sitting directly on the frosted glass dock (no heavy double box).
        Column(
            Modifier
                .weight(1f)
                .heightIn(min = LEADING_SLOT_SIZE),
            verticalArrangement = Arrangement.Center,
        ) {
            if (cues != null) cues()
            InputField(state, actions, canSend, visualTransformation)
        }

        SendButton(
            state = state,
            enabled = canSend,
            onClick = { if (canSend) actions.onSend() else actions.onEmptySend() },
        )
    }
}

/** The field's text style: serif 15 / 24 (`line` family). */
@Composable
internal fun inputFieldTextStyle(): TextStyle =
    CivilightTheme.type.line.merge(TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = CivilightTheme.colors.ink))

@Composable
private fun InputField(
    state: ScriptInputState,
    actions: ScriptInputActions,
    canSend: Boolean,
    visualTransformation: VisualTransformation,
) {
    val colors = CivilightTheme.colors
    val style = inputFieldTextStyle()
    val density = LocalDensity.current
    val lineHeight: Dp = with(density) { style.lineHeight.toDp() }
    var held by remember { mutableStateOf(TextFieldValue(state.text, TextRange(state.selectionStart, state.selectionEnd))) }
    val stateSelection = TextRange(state.selectionStart, state.selectionEnd)
    val value = if (held.text == state.text && held.selection == stateSelection) held else TextFieldValue(state.text, stateSelection)
    val follow = rememberCaretFollow(value, state.focused) {
        if (visualTransformation === VisualTransformation.None) it.selection.end
        else visualTransformation.filter(androidx.compose.ui.text.AnnotatedString(it.text)).offsetMapping.originalToTransformed(it.selection.end)
    }
    val scroll = rememberScrollState()
    val fade = animatedAlpha(if (scroll.value > 0) 1f else 0f, label = "inputFade")
    val placeholder = when {
        state.activeTab == ComposerTab.SCENE -> ScriptInputCopy.PLACEHOLDER_SCENE
        state.activeTab == ComposerTab.DIALOGUE -> {
            val who = state.speaker?.label
            val extra = state.coSpeakers.size
            when {
                who == null -> ScriptInputCopy.PLACEHOLDER_DIALOGUE
                extra > 0 -> "$who 외 ${extra}명 · ${ScriptInputCopy.PLACEHOLDER_DIALOGUE}"
                else -> "$who · ${ScriptInputCopy.PLACEHOLDER_DIALOGUE}"
            }
        }
        else -> ScriptInputCopy.PLACEHOLDER_NARRATION
    }
    BasicTextField(
        value = value,
        onValueChange = {
            held = it
            actions.onEvent(ScriptInputEvent.TextChanged(it.text, it.selection.start, it.selection.end))
        },
        textStyle = style,
        cursorBrush = SolidColor(colors.acc),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Default),
        singleLine = false,
        visualTransformation = visualTransformation,
        onTextLayout = follow::onTextLayout,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { actions.onEvent(ScriptInputEvent.Focus(it.isFocused)) }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    // Hardware keyboard Tab cycles `[대사 → 지문 → 장면]`
                    keyEvent.key == Key.Tab && !keyEvent.isCtrlPressed && !keyEvent.isMetaPressed && !keyEvent.isShiftPressed -> {
                        val tabs = if (state.editing) listOf(ComposerTab.DIALOGUE, ComposerTab.NARRATION) else ComposerTab.entries
                        val next = tabs[(tabs.indexOf(state.activeTab) + 1) % tabs.size]
                        actions.onSelectTab(next)
                        true
                    }
                    // Hardware Ctrl+Enter / Cmd+Enter sends immediately
                    keyEvent.key == Key.Enter && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) -> {
                        if (canSend) actions.onSend() else actions.onEmptySend()
                        true
                    }
                    else -> false
                }
            },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 38.dp, max = lineHeight * ScriptInputRules.MAX_VISIBLE_LINES + 12.dp)
                    .drawWithContent {
                        drawContent()
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
                                Brush.verticalGradient(listOf(colors.glass, Color.Transparent), endY = 18.dp.toPx()),
                                size = Size(size.width, 18.dp.toPx()),
                                alpha = f,
                            )
                        }
                    }
                    .verticalScroll(scroll)
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val transformedText = if (visualTransformation === VisualTransformation.None) state.text
                else visualTransformation.filter(androidx.compose.ui.text.AnnotatedString(state.text)).text.text
                if (state.text.isEmpty() && transformedText.isEmpty()) {
                    CivilightText(placeholder, style, color = colors.ink3, maxLines = 1)
                }
                Box(Modifier.caretFollow(follow)) { inner() }
            }
        },
    )
}

/**
 * Send button: 38 dp height circle (`pri` / `onpri`), or `고치기` pill during edit.
 * Tapping while empty calls [onClick] too so empty-send shake feedback can fire cleanly.
 */
@Composable
private fun SendButton(state: ScriptInputState, enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(19.dp)
    val slot = ScriptInputRules.sendSlot(state)
    val content = if (enabled) colors.onpri else colors.ink3
    val name = if (slot == SendSlot.ICON) IconButtonCopy.SEND else IconButtonCopy.EDIT
    Box(
        Modifier
            .pressable(onClick = onClick, enabled = true, hitTarget = HitTarget.ICON_BUTTON, shape = shape)
            .heightIn(min = 38.dp)
            .widthIn(min = if (slot == SendSlot.ICON) 38.dp else 0.dp)
            .clip(shape)
            .background(if (enabled) colors.pri else colors.tint)
            .padding(horizontal = if (slot == SendSlot.ICON) 0.dp else 13.dp)
            .semantics { contentDescription = name; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        if (slot == SendSlot.ICON) {
            CivilightIcon(IconId.Send, contentDescription = null, size = IconButtons.ICON_ONLY_DP.dp, tint = content)
        } else {
            CivilightText(
                IconButtonCopy.EDIT,
                CivilightTheme.type.button.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
                color = content,
                maxLines = 1,
            )
        }
    }
}

/** SCR-60 read-only row in place of the input bar. */
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
                .heightIn(min = 84.dp)
                .padding(start = 14.dp, end = 14.dp, bottom = if (glass) nav + 14.dp else 0.dp, top = 12.dp),
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
    Box(modifier.fillMaxWidth().clip(DOCK_SHAPE)) {
        GlassBackground(GlassSurface.DOCK, Modifier.matchParentSize(), shape = DOCK_SHAPE)
        OnGlass { row() }
    }
}
