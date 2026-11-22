package io.github.lenticualrstone.civilight.ui.window

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.Popover
import io.github.lenticualrstone.civilight.designsystem.component.PopoverCard
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightHaptics
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Where the CNV-47 ratios come from and go to: [settings] (`AppSettings.paneSplit`) and [write], which the
 * shell sends to `SettingsStore.setPaneSplit` (DECISIONS §1). Provided by the shell as [LocalPaneSplits].
 */
class PaneSplitSource(
    val settings: StateFlow<AppSettings>,
    private val write: (module: String, split: PaneSplit) -> Unit,
) {
    fun set(module: String, split: PaneSplit) = write(module, split)

    companion object {
        /** In memory only: previews and tests outside the shell. */
        fun inMemory(): PaneSplitSource {
            val flow = MutableStateFlow(AppSettings())
            return PaneSplitSource(flow) { m, s -> flow.value = flow.value.withPaneSplit(m, s) }
        }
    }
}

/** The pane-split source of the app (CNV-47); the shell provides the stored one. */
val LocalPaneSplits = staticCompositionLocalOf { PaneSplitSource.inMemory() }

/**
 * The live split of one two-pane frame (CNV-47). [leftDp] is the left pane's width as drawn: it follows the
 * finger while dragging and settles (motion `standard`, a snap when motion is reduced) to the stored ratio.
 * Make it with [rememberPaneSplitState]; draw the handle with [PaneSplitHandle]. `feature:script` uses the
 * pair for its side panel; [SplitPanes] is the whole frame for list + detail.
 */
@Stable
class PaneSplitState internal constructor(
    val module: String,
    private val scope: CoroutineScope,
    private val source: PaneSplitSource,
) {
    private val anim: Animatable<Float, AnimationVector1D> = Animatable(0f)
    private var initialized by mutableStateOf(false)
    internal var totalDp by mutableFloatStateOf(0f)
    internal var defaultLeftDp by mutableFloatStateOf(0f)
    internal var inner by mutableStateOf(false)
    internal var stored by mutableStateOf(PaneSplit.DEFAULT)
    private var pending by mutableStateOf<PaneSplit?>(null)
    private var settleNext = false
    internal var motion: Motion = Motion(reduced = false)
    internal var haptics: CivilightHaptics? = null

    /** True while the handle is being dragged. */
    var dragging by mutableStateOf(false)
        private set

    /** The chosen ratio (the one just picked, until the store has it). */
    val split: PaneSplit get() = pending ?: stored

    /** Whether the handle shows: an inner screen where both panes keep their minimum. */
    val resizable: Boolean get() = PaneSplitRules.canResize(totalDp, inner)

    /** Where the left pane settles for [split] in this window. */
    val targetLeftDp: Float get() = PaneSplitRules.leftWidth(totalDp, split, defaultLeftDp, inner)

    /** The left pane's width as drawn now. */
    val leftDp: Float get() = if (initialized && resizable) anim.value else targetLeftDp

    internal fun onStored(value: PaneSplit) {
        stored = value
        if (pending == value) pending = null
    }

    internal suspend fun follow() {
        snapshotFlow { targetLeftDp to dragging }
            .distinctUntilChanged()
            .collectLatest { (target, drag) ->
                if (drag) return@collectLatest
                if (settleNext && initialized) {
                    settleNext = false
                    anim.animateTo(target, motion.move(CivilightMotion.standard))
                    // The settle tick (CNV-47, as the snapping card row); it stays when motion is reduced.
                    haptics?.perform(HapticToken.TICK)
                } else {
                    anim.snapTo(target)
                    initialized = true
                }
            }
    }

    internal fun startDrag() {
        if (!resizable) return
        scope.launch { anim.snapTo(leftDp) }
        initialized = true
        dragging = true
    }

    internal fun dragBy(deltaDp: Float) {
        if (!dragging) return
        scope.launch { anim.snapTo(PaneSplitRules.clampLeft(anim.value + deltaDp, totalDp)) }
    }

    internal fun endDrag() {
        if (!dragging) return
        choose(PaneSplitRules.settle(anim.value, totalDp))
    }

    /** Applies [value] at once (popover button or released drag) with the settle motion, and stores it. */
    fun choose(value: PaneSplit) {
        settleNext = true
        pending = value
        dragging = false
        source.set(module, value)
    }
}

/**
 * The split state of the frame [totalDp] wide for [module] (`PaneSplit.Modules`). [defaultLeftDp] is the
 * module's own left width for that total (`기본`; CNV-33 390 | rest, script its own).
 */
@Composable
fun rememberPaneSplitState(module: String, totalDp: Float, defaultLeftDp: Float): PaneSplitState {
    val source = LocalPaneSplits.current
    val scope = rememberCoroutineScope()
    val state = remember(module, source) { PaneSplitState(module, scope, source) }
    val settings by source.settings.collectAsState()
    state.onStored(settings.paneSplitOf(module))
    state.totalDp = totalDp
    state.defaultLeftDp = defaultLeftDp
    state.inner = LocalWindowLayout.current.isInner
    state.motion = LocalMotion.current
    state.haptics = rememberHaptics()
    LaunchedEffect(state) { state.follow() }
    return state
}

/**
 * The CNV-47 handle: a 4 × 32 grip in `ink3` (`acc` while pressed, dragged or open) in a 44 dp hit target,
 * to be centred on the 1 dp divider. Dragging resizes the panes; a tap opens a popover with the real buttons
 * `1:2 · 1:1 · 2:1 · 기본`. Nothing is drawn when the frame cannot be resized (cover, narrow window).
 */
@Composable
fun PaneSplitHandle(state: PaneSplitState, modifier: Modifier = Modifier) {
    if (!state.resizable) return
    val colors = CivilightTheme.colors
    val density = LocalDensity.current.density
    var open by remember { mutableStateOf(false) }
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val grip by animatedColor(if (pressed || state.dragging || open) colors.acc else colors.ink3, label = "paneGrip")
    val label = PaneSplitRules.label(state.split)
    Box(
        modifier
            .size(HANDLE_HIT)
            .draggable(
                state = rememberDraggableState { px -> state.dragBy(px / density) },
                orientation = Orientation.Horizontal,
                onDragStarted = { state.startDrag() },
                onDragStopped = { state.endDrag() },
            )
            .pressable(onClick = { open = true }, scaleOnPress = false, pressOverlay = false, interactionSource = press)
            .semantics {
                contentDescription = PaneSplitCopy.HANDLE
                stateDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(GRIP_WIDTH, GRIP_HEIGHT).clip(RoundedCornerShape(2.dp)).drawBehind { drawRect(grip) })
        if (open) {
            Popover(onDismissRequest = { open = false }) {
                PopoverCard(title = PaneSplitCopy.HANDLE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (choice in PaneSplitRules.CHOICES) {
                            FilterChip(PaneSplitRules.label(choice), selected = choice == state.split, onClick = {
                                state.choose(choice)
                                open = false
                            })
                        }
                    }
                }
            }
        }
    }
}

/**
 * A two-pane frame with the CNV-47 handle: [left] | 1 dp `line` | [right]. The split comes from [module]'s
 * stored ratio, [defaultLeft] gives the module's own left width for a total width (`기본`). On the cover
 * screen, or when two 280 dp panes do not fit, the frame keeps [defaultLeft] and shows no handle.
 */
@Composable
fun SplitPanes(
    module: String,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    defaultLeft: (totalDp: Float) -> Float = PaneSplitRules::listDetailDefaultLeft,
) {
    val colors = CivilightTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(colors.bg)) {
        val total = maxWidth.value
        val state = rememberPaneSplitState(module, total, defaultLeft(total))
        SplitLayout(leftDp = { state.leftDp }, left = left, right = right, divider = {
            Box(Modifier.fillMaxSize().background(colors.line))
        })
        PaneSplitHandle(
            state,
            Modifier
                .align(Alignment.CenterStart)
                .layoutOffset { (state.leftDp + PaneSplitRules.DIVIDER_DP / 2f - HANDLE_HIT.value / 2f) },
        )
    }
}

/** Places the node [x] dp from the start, read in the layout phase (no recomposition while dragging). */
private fun Modifier.layoutOffset(x: () -> Float): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    layout(p.width, p.height) { p.place(IntOffset((x() * density).roundToInt(), 0)) }
}

/** left | divider | right, with the left width read in the layout phase. */
@Composable
private fun SplitLayout(
    leftDp: () -> Float,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    divider: @Composable () -> Unit,
) {
    Layout(
        contents = listOf({ Box { left() } }, divider, { Box { right() } }),
        modifier = Modifier.fillMaxSize(),
    ) { (l, d, r), constraints ->
        val total = constraints.maxWidth
        val height = constraints.maxHeight
        val dividerPx = (PaneSplitRules.DIVIDER_DP * density).roundToInt().coerceAtLeast(1)
        val leftPx = (leftDp() * density).roundToInt().coerceIn(0, (total - dividerPx).coerceAtLeast(0))
        val rightPx = (total - leftPx - dividerPx).coerceAtLeast(0)
        val lp = l.first().measure(Constraints.fixed(leftPx, height))
        val dp = d.first().measure(Constraints.fixed(dividerPx, height))
        val rp = r.first().measure(Constraints.fixed(rightPx, height))
        layout(total, height) {
            lp.place(0, 0)
            dp.place(leftPx, 0)
            rp.place(leftPx + dividerPx, 0)
        }
    }
}

private val HANDLE_HIT = 44.dp
private val GRIP_WIDTH = 4.dp
private val GRIP_HEIGHT = 32.dp
