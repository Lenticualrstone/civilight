package io.github.lenticualrstone.civilight.ui.window

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.theme.ShadowSpec
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Which edge a collapsible side panel sits on: the list of `목록 + 상세` (left) or the script panel (right). */
enum class PanelSide { START, END }

/**
 * (0.2.3, r3 `L*` / `P*` / `Two*`) A side panel that closes and reopens by a visible button on its seam, as plain
 * numbers (dp). Open: `panel | 1 dp line | main`. Closed: the panel slides out, a [RAIL] dp rail (with its 1 dp
 * hairline) keeps the reopen buttons off the back-gesture edge, and the main pane takes the rest. JVM-tested.
 */
object PanelRules {
    /** The closed panel's rail (board `.rail` 56 px). */
    const val RAIL: Float = 56f

    /** Rail plus its hairline: where the main pane starts (or ends) while closed (board `left: 57px`). */
    const val RAIL_SPACE: Float = RAIL + PaneSplitRules.DIVIDER_DP

    /** The seam tab as drawn: 20 × 64 open, 24 × 64 closed (board `.tgb`). */
    const val TAB_WIDTH_OPEN: Float = 20f
    const val TAB_WIDTH_CLOSED: Float = 24f
    const val TAB_HEIGHT: Float = 64f

    /** The tab's touch area (board `.tgb::after` 44 × 72): at least 44 dp each way. */
    const val TAB_HIT_WIDTH: Float = 44f
    const val TAB_HIT_HEIGHT: Float = 72f

    /** Open: the tab straddles the seam, starting this far inside the panel (board `left: calc(var(--pw) - 9px)`). */
    const val TAB_SEAM_INSET: Float = 9f

    /** The same for an end-side panel, whose hairline sits on its inner edge (board `right: calc(var(--pw) - 11px)`). */
    const val TAB_SEAM_INSET_END: Float = 11f

    /** Closed: the tab sits this far from the panel's edge, on the rail (board `left: 16px`). */
    const val TAB_CLOSED_INSET: Float = 16f

    /** The rail's `패널 열기` button: 44 dp, 22 down and 6 in (board `.tpb`). */
    const val REOPEN_SIZE: Float = 44f
    const val REOPEN_TOP: Float = 22f
    const val REOPEN_INSET: Float = 6f

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    /**
     * Where the main pane begins, measured from the panel's edge, for a panel [panelDp] wide that is [open]
     * (0 closed … 1 open): `panel + divider` open, [RAIL_SPACE] closed.
     */
    fun mainInset(panelDp: Float, open: Float): Float = lerp(RAIL_SPACE, panelDp + PaneSplitRules.DIVIDER_DP, open)

    /** How far the panel has slid out past its edge (0 open … [panelDp] closed). */
    fun panelShift(panelDp: Float, open: Float): Float = (1f - open.coerceIn(0f, 1f)) * panelDp

    /** The drawn tab's start, measured from the panel's edge (on the seam open, on the rail closed). */
    fun tabInset(panelDp: Float, open: Float, side: PanelSide = PanelSide.START): Float =
        lerp(TAB_CLOSED_INSET, panelDp - (if (side == PanelSide.START) TAB_SEAM_INSET else TAB_SEAM_INSET_END), open)

    /** The drawn tab's width. */
    fun tabWidth(open: Float): Float = lerp(TAB_WIDTH_CLOSED, TAB_WIDTH_OPEN, open)

    /** The drawn tab's corner radius: 10 on the seam, 12 on the rail (board `.tgb` / `.pc .tgb`). */
    fun tabRadius(open: Float): Float = lerp(12f, 10f, open)

    /** The chevron's turn: it points toward the panel's edge while open (close), away from it while closed (open). */
    fun chevronDegrees(side: PanelSide, open: Float): Float {
        val openDeg = if (side == PanelSide.START) 0f else 180f
        return lerp(openDeg + 180f, openDeg, open)
    }

    /** The seam button's label: what a tap does now. */
    fun label(closed: Boolean): String = if (closed) PanelCopy.OPEN else PanelCopy.CLOSE

    /** Whether a frame can offer the button: two panes are drawn (an inner screen). */
    fun canCollapse(inner: Boolean): Boolean = inner
}

/** Copy of the side panel's seam and rail buttons (r3 `aria-label`). */
object PanelCopy {
    const val CLOSE = "패널 닫기"
    const val OPEN = "패널 열기"

    val all: List<String> = listOf(CLOSE, OPEN)
}

/**
 * One screen's collapsible side panel. [closed] is the stored choice (`AppSettings.panelClosed`, per [module]; the
 * one just made until the store has it); [open] is the drawn amount (0 closed … 1 open), sliding on `panelSlide`.
 * Reduced motion: [open] snaps and [panelAlpha] fades the panel 120 ms instead (r3 boards).
 */
@Stable
class PanelCollapseState internal constructor(
    val module: String,
    private val source: PaneSplitSource,
    initialClosed: Boolean,
) {
    private val amount: Animatable<Float, AnimationVector1D> = Animatable(if (initialClosed) 0f else 1f)
    private val fade: Animatable<Float, AnimationVector1D> = Animatable(if (initialClosed) 0f else 1f)
    internal var stored by mutableStateOf(initialClosed)
    private var pending by mutableStateOf<Boolean?>(null)
    internal var motion: Motion = Motion(reduced = false)

    /**
     * False until [follow] has snapped to the effective state: the seed above is the stored choice, but [available]
     * is set by the frame after construction, so until then the drawn amounts come from [closed] itself (no slide
     * in from the edge when a stored `closed` cannot apply, e.g. no detail beside the list).
     */
    private var settled by mutableStateOf(false)
    private val seed: Float get() = if (closed) 0f else 1f
    private val amountNow: Float get() = if (settled) amount.value else seed
    private val fadeNow: Float get() = if (settled) fade.value else seed

    /** The writer's choice for this screen: closed or open (kept while it cannot apply). */
    val chosenClosed: Boolean get() = pending ?: stored

    /** Whether the panel can be closed here now (an inner screen, and something beside it); set by the frame. */
    var available by mutableStateOf(true)

    /** Closed as drawn: the choice, where it can apply. */
    val closed: Boolean get() = chosenClosed && available

    /** The drawn open amount, 0 … 1 (read it in layout or draw). */
    val open: Float get() = amountNow

    /**
     * How far the panel itself is in (0 out … 1 in): [open] in full motion (it slides); with reduced motion it stays
     * in place while it fades ([panelAlpha]) and is out once the fade ends.
     */
    val panelIn: Float get() = if (motion.reduced) (if (fadeNow > 0f) 1f else 0f) else amountNow

    /** The panel's opacity: 1 in full motion (it slides), the 120 ms fade when motion is reduced. */
    val panelAlpha: Float get() = if (motion.reduced) fadeNow else 1f

    /** True once the panel is fully out: it is then not drawn and takes no touches. */
    val gone: Boolean get() = closed && amountNow <= 0f && (!motion.reduced || fadeNow <= 0f)

    internal fun onStored(value: Boolean) {
        stored = value
        if (pending == value) pending = null
    }

    /** Closes or opens the panel and remembers it for this screen. */
    fun setClosed(value: Boolean) {
        if (value == chosenClosed) return
        pending = value
        source.setPanelClosed(module, value)
    }

    fun toggle() = setClosed(!closed)

    internal suspend fun follow() = kotlinx.coroutines.coroutineScope {
        snapshotFlow { closed }.distinctUntilChanged().collectLatest { c ->
            val target = if (c) 0f else 1f
            if (!settled) {
                // First emission: the effective state as it is now, without a slide.
                fade.snapTo(target)
                amount.snapTo(target)
                settled = true
                return@collectLatest
            }
            launch { fade.animateTo(target, motion.spec(CivilightMotion.panelSlide)) }
            amount.animateTo(target, motion.move(CivilightMotion.panelSlide))
        }
    }
}

/** The collapsible panel of [module] (`PaneSplit.Modules`), stored through [LocalPaneSplits]. */
@Composable
fun rememberPanelCollapseState(module: String): PanelCollapseState {
    val source = LocalPaneSplits.current
    val settings by source.settings.collectAsState()
    val closed = settings.panelClosedOf(module)
    val state = remember(module, source) { PanelCollapseState(module, source, closed) }
    state.onStored(closed)
    state.motion = LocalMotion.current
    LaunchedEffect(state) { state.follow() }
    return state
}

/**
 * Keeps a closed panel out of reach: no semantics and no focus once it is chosen closed (it may still be sliding
 * out), so a screen reader or keyboard never lands in a pane that is not there.
 */
fun Modifier.collapsedPanel(closed: Boolean): Modifier =
    if (!closed) this else clearAndSetSemantics { }.focusProperties { onEnter = { cancelFocusChange() } }.focusGroup()

/** (0.4.0) One icon shortcut displayed on the collapsed mini icon rail. */
data class MiniRailAction(
    val icon: IconId,
    val label: String,
    val active: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * (0.4.0) Unobtrusive seam control:
 * - While open: renders an invisible full-height resize drag seam + a compact collapse button tucked cleanly
 *   at the top of the panel edge (never a giant shadowed pill blocking the center of the viewport).
 * - While closed: renders nothing (the collapsed [PanelRail] owns the reopen button + mini icon shortcuts,
 *   eliminating the old duplicate reopen buttons).
 */
@Composable
fun PanelSeamButton(
    state: PanelCollapseState,
    side: PanelSide,
    panelDp: () -> Float,
    modifier: Modifier = Modifier,
    extraActions: List<CustomAccessibilityAction> = emptyList(),
    dragModifier: Modifier = Modifier,
) {
    if (state.closed) return
    val colors = CivilightTheme.colors
    val label = PanelRules.label(state.closed)
    val shape = RoundedCornerShape(10.dp)
    val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier
            .fillMaxHeight()
            .layout { measurable, constraints ->
                val hitW = (32f * density).roundToInt()
                val p = measurable.measure(constraints.copy(minWidth = hitW, maxWidth = hitW))
                val seamX = ((panelDp() - 16f) * density).roundToInt().coerceAtLeast(0)
                layout(constraints.maxWidth, p.height) {
                    val px = if (side == PanelSide.START) seamX else constraints.maxWidth - seamX - p.width
                    p.place(IntOffset(px, 0))
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        // Full-height invisible resize seam drag target
        Box(
            Modifier
                .fillMaxHeight()
                .width(24.dp)
                .then(dragModifier),
        )
        // Compact, unobtrusive collapse button tucked near the top header corner
        Box(
            Modifier
                .padding(top = statusTop + 14.dp)
                .size(28.dp)
                .clip(shape)
                .background(colors.bg.copy(alpha = 0.88f))
                .insetOutline(colors.line, 1.dp, shape)
                .pressable(onClick = state::toggle, scaleOnPress = false, shape = shape)
                .semantics {
                    contentDescription = label
                    if (extraActions.isNotEmpty()) customActions = extraActions
                },
            contentAlignment = Alignment.Center,
        ) {
            CivilightIcon(
                IconId.Back,
                contentDescription = null,
                size = 14.dp,
                tint = colors.ink2,
                modifier = Modifier.graphicsLayer { rotationZ = PanelRules.chevronDegrees(side, state.open) },
            )
        }
    }
}

/**
 * (0.4.0) Customizable mini icon rail shown when a foldable side panel is collapsed:
 * Top reopen button (`IconId.Panel`) + hairline + user-configured [actions] icons.
 */
@Composable
fun PanelRail(
    state: PanelCollapseState,
    side: PanelSide,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    actions: List<MiniRailAction> = emptyList(),
) {
    if (!state.closed && state.open >= 1f) return
    val colors = CivilightTheme.colors
    val alpha = { (1f - state.open).coerceIn(0f, 1f) }
    Box(
        modifier
            .fillMaxHeight()
            .width(PanelRules.RAIL_SPACE.dp)
            .graphicsLayer { this.alpha = alpha() }
            .background(colors.bg)
            .drawBehind {
                val w = PaneSplitRules.DIVIDER_DP * density
                val x = if (side == PanelSide.START) size.width - w else 0f
                drawRect(colors.line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Size(w, size.height))
            }
            .then(if (state.closed) Modifier else Modifier.clearAndSetSemantics { }),
    ) {
        if (state.closed) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(PanelRules.RAIL.dp)
                    .align(if (side == PanelSide.START) Alignment.TopStart else Alignment.TopEnd)
                    .padding(top = topInset + 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(PanelRules.REOPEN_SIZE.dp)
                        .pressable(onClick = { state.setClosed(false) }, scaleOnPress = false, shape = RoundedCornerShape(12.dp))
                        .semantics { contentDescription = PanelCopy.OPEN },
                    contentAlignment = Alignment.Center,
                ) {
                    CivilightIcon(IconId.Panel, contentDescription = null, size = 20.dp, tint = colors.acc)
                }
                if (actions.isNotEmpty()) {
                    Box(
                        Modifier
                            .width(24.dp)
                            .height(1.dp)
                            .background(colors.line),
                    )
                    actions.forEach { action ->
                        val itemShape = RoundedCornerShape(12.dp)
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(itemShape)
                                .background(if (action.active) colors.sel else androidx.compose.ui.graphics.Color.Transparent)
                                .pressable(onClick = action.onClick, scaleOnPress = false, shape = itemShape)
                                .semantics { contentDescription = action.label },
                            contentAlignment = Alignment.Center,
                        ) {
                            CivilightIcon(
                                action.icon,
                                contentDescription = null,
                                size = 19.dp,
                                tint = if (action.active) colors.acc else colors.ink2,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The tab's lift (board `0 4px 14px rgba(0,0,0,.18)`). */
private val TabShadow = ShadowSpec(4.dp, 14.dp, androidx.compose.ui.graphics.Color(0f, 0f, 0f, 0.18f))
