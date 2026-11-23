package io.github.lenticualrstone.civilight.designsystem.component

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.LocalGlassSource
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.glass.glassSource
import io.github.lenticualrstone.civilight.designsystem.glass.rememberGlassSource
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics
import io.github.lenticualrstone.civilight.designsystem.theme.OverlayGaps
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import kotlinx.coroutines.launch

/**
 * DS-23 modal bottom sheet over [content]. While open: `shade` scrim, the screen behind scales to 96.5 % and
 * blurs 2 dp (CNV-41; reduced motion: fade only), the sheet rises with `Enter` and leaves with `Exit`.
 * Dismiss: `닫기` (in [sheet]), scrim tap, system back (via [LocalBackHandler]), or dragging down past
 * 40 % of its height. The sheet is glass (blurring [content]), top corners 28, 1 dp `line` inset, grab
 * handle, padding 10 top / [sidePadding] sides / `systemBottom + 24` bottom (16 above the keyboard, DS-66); its
 * top never rises above the
 * status bar + 56 dp, and longer content scrolls inside it.
 * [centered] draws the inner-screen centred sheet (max width 560, all corners rounded); it scales .96 → 1
 * with its fade. The back screen scales from 50 % / 30 % (CNV-41); while the sheet is dragged down, the
 * scrim and the back screen's scale and blur follow the drag (S5). The sheet's height changes animate on
 * `standard` (S4; snap when reduced).
 * With the keyboard up both kinds sit above it ([KeyboardLift], consumption-aware): the bottom sheet's
 * bottom edge rides on the keyboard and its maximum height shrinks by the keyboard; the centred sheet
 * centres in the space above the keyboard.
 */
@Composable
fun CivilightSheetLayout(
    sheetVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sidePadding: Dp = LayoutMetrics.sheetSide,
    centered: Boolean = false,
    sheet: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val reduced = motion.reduced
    val source = rememberGlassSource()
    val progress = remember { Animatable(if (sheetVisible) 1f else 0f) }
    val drag = remember { Animatable(0f) }
    val sheetHeight = remember { mutableFloatStateOf(0f) }
    var composed by remember { mutableStateOf(sheetVisible) }
    LaunchedEffect(sheetVisible) {
        if (sheetVisible) {
            composed = true
            drag.snapTo(0f)
            progress.animateTo(1f, motion.spec(CivilightMotion.enter))
        } else {
            progress.animateTo(0f, motion.spec(CivilightMotion.exit))
            composed = false
        }
    }
    // Open amount as the back screen and scrim see it: the drag-to-dismiss gesture takes it back (S5).
    val shown = { sheetOpenAmount(progress.value, drag.value, sheetHeight.floatValue) }
    LocalBackHandler.current(sheetVisible, onDismissRequest)

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .glassSource(source)
                .graphicsLayer {
                    val p = shown()
                    if (!reduced && p > 0f) {
                        val s = 1f - (1f - CivilightMotion.sheetBackScale) * p
                        transformOrigin = SheetBackOrigin
                        scaleX = s
                        scaleY = s
                        if (Build.VERSION.SDK_INT >= 31) {
                            val r = CivilightMotion.sheetBackBlur.toPx() * p
                            renderEffect = if (r > 0f) BlurEffect(r, r, TileMode.Clamp) else null
                        }
                    } else {
                        scaleX = 1f
                        scaleY = 1f
                        renderEffect = null
                    }
                },
        ) { content() }

        if (composed) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shown() }
                    .background(colors.shade)
                    .clearAndSetSemantics { }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest),
            )
            CompositionLocalProvider(LocalGlassSource provides source) {
                SheetSurface(progress, drag, sheetHeight, centered, sidePadding, onDismissRequest, sheet)
            }
        }
    }
}

@Composable
private fun SheetSurface(
    progress: Animatable<Float, *>,
    drag: Animatable<Float, AnimationVector1D>,
    sheetHeightState: MutableFloatState,
    centered: Boolean,
    sidePadding: Dp,
    onDismissRequest: () -> Unit,
    sheet: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val reduced = motion.reduced
    val scope = rememberCoroutineScope()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // DS-66: the box below rides on the keyboard by `ime − navigation`, so with the keyboard up the navigation
    // inset is still under the sheet and 16 dp more puts the last content 16 above the keyboard top; with the
    // keyboard hidden the content ends `systemBottom + 24` above the window bottom.
    val imeUp = keyboardUp()
    val systemBelow = if (imeUp) navBottom else systemBottomWithoutKeyboard()
    val sheetBottomPadding = if (imeUp) navBottom + OverlayGaps.sheetBottomIme else systemBelow + OverlayGaps.sheetBottom
    var sheetHeight by sheetHeightState
    val shape: Shape = if (centered) RoundedCornerShape(28.dp) else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    BoxWithConstraints(
        Modifier.fillMaxSize().windowInsetsPadding(KeyboardLift),
        contentAlignment = if (centered) Alignment.Center else Alignment.BottomCenter,
    ) {
        val maxSheet = maxHeight - statusTop - LayoutMetrics.sheetMaxTopBelowStatusBar
        Box(
            modifier = Modifier
                .then(
                    // DS-66: the centred sheet sits in the band `[statusTop + 24, H − systemBottom − 24]`, 24 dp sides.
                    if (centered) {
                        Modifier
                            .padding(
                                start = OverlayGaps.dialogBand,
                                end = OverlayGaps.dialogBand,
                                top = statusTop + OverlayGaps.dialogBand,
                                bottom = systemBelow + OverlayGaps.dialogBand,
                            )
                            .widthIn(max = LayoutMetrics.centredSheetMaxWidth)
                    } else Modifier,
                )
                .fillMaxWidth()
                .heightIn(max = maxSheet)
                .onSizeChanged { sheetHeight = it.height.toFloat() }
                .graphicsLayer {
                    val p = progress.value
                    if (reduced) {
                        alpha = p
                        translationY = drag.value
                    } else if (centered) {
                        alpha = p
                        val s = CENTRED_START_SCALE + (1f - CENTRED_START_SCALE) * p
                        scaleX = s
                        scaleY = s
                        translationY = drag.value
                    } else {
                        translationY = (1f - p) * sheetHeight + drag.value
                    }
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) }
                    },
                    onDragStopped = {
                        if (sheetHeight > 0f && drag.value > sheetHeight * DISMISS_FRACTION) {
                            onDismissRequest()
                            drag.animateTo(0f, motion.move(CivilightMotion.exit))
                        } else {
                            drag.animateTo(0f, motion.move(CivilightMotion.standard))
                        }
                    },
                ),
        ) {
            GlassBackground(GlassSurface.SHEET, Modifier.matchParentSize(), shape = shape)
            OnGlass {
                Column(
                    Modifier
                        .animateContentSize(motion.move(CivilightMotion.standard))
                        .clip(shape)
                        .insetOutline(colors.line, 1.dp, shape)
                        .padding(top = 10.dp, start = sidePadding, end = sidePadding, bottom = if (centered) 20.dp else sheetBottomPadding),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.ink3.copy(alpha = 0.5f))
                            .clearAndSetSemantics { },
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(content = sheet)
                }
            }
        }
    }
}

/** Drag distance (fraction of the sheet height) past which a drag closes the sheet (proposed). */
private const val DISMISS_FRACTION = 0.4f

/** CNV-41: the back screen scales towards 50 % / 30 %. */
private val SheetBackOrigin = TransformOrigin(0.5f, 0.3f)

/** Start scale of the centred sheet (S5). */
private const val CENTRED_START_SCALE = 0.96f

/**
 * How open the sheet looks to the back screen and the scrim: the open [progress] times the part of the
 * sheet still up while it is dragged [drag] px of [height] (S5).
 */
fun sheetOpenAmount(progress: Float, drag: Float, height: Float): Float {
    val held = if (height > 0f) (1f - drag / height).coerceIn(0f, 1f) else 1f
    return progress.coerceIn(0f, 1f) * held
}

/**
 * DS-23 title row: title in the `title` role at the left, `닫기` (`label`, `ink2`) at the right,
 * baseline-aligned; optional `small` sub-line 2 dp below the title.
 */
@Composable
fun SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    closeLabel: String = "닫기",
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CivilightText(title, type.title, color = colors.ink, maxLines = 1)
            if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3)
        }
        TextAction(closeLabel, onClose)
    }
}

/** A sheet title row made of tabs (`시간 · 참고 · 속성 · 모듈`) plus `닫기`. */
@Composable
fun SheetTabsHeader(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeLabel: String = "닫기",
    gap: Dp = TabGap.Sheet,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CivilightTabs(tabs, selectedIndex, onSelect, Modifier.weight(1f), gap = gap)
        TextAction(closeLabel, onClose)
    }
}

/**
 * Sheet actions at the bottom, 14 dp below the content: buttons 8 dp apart (ghost cancel flex 1 +
 * primary flex 1, or primary flex 2 when a cancel exists).
 */
@Composable
fun SheetActions(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    ButtonRow(modifier.fillMaxWidth().padding(top = 14.dp), content = content)
}

/** Scrollable body for long sheet content; the title row above it stays pinned. */
@Composable
fun ColumnScope.SheetScrollBody(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
}
