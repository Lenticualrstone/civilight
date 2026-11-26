package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import io.github.lenticualrstone.civilight.designsystem.foundation.GestureRules
import io.github.lenticualrstone.civilight.designsystem.foundation.horizontalSwipe
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.startHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.MarkerGlyph
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * DS-44 revealed button row: 192 wide (3 × 64), `raise` fill; three buttons, each a centred column (gap 4,
 * `chip` role `ink`, 1 dp `line` left border): `시간` (with `◆` 11 sp above and the label in `acc` when the
 * block has a change), `참고` (`Paper` 18), `속성` (`Props` 18). Only the buttons act.
 */
@Composable
fun SwipeTabsRow(
    onTime: () -> Unit,
    onReference: () -> Unit,
    onProperties: () -> Unit,
    modifier: Modifier = Modifier,
    timeHasChange: Boolean = false,
    timeLabel: String = "시간",
    referenceLabel: String = "참고",
    propertiesLabel: String = "속성",
) {
    val colors = CivilightTheme.colors
    Row(modifier.width(CivilightTheme.sizes.swipeRow).fillMaxHeight().background(colors.raise)) {
        SwipeButton(timeLabel, onTime, labelColor = if (timeHasChange) colors.acc else colors.ink) {
            if (timeHasChange) MarkerGlyph(MarkerKind.CHANGE, size = 11.dp)
        }
        SwipeButton(referenceLabel, onReference) { CivilightIcon(IconId.Paper, null, size = 18.dp, tint = colors.ink2) }
        SwipeButton(propertiesLabel, onProperties) { CivilightIcon(IconId.Props, null, size = 18.dp, tint = colors.ink2) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SwipeButton(
    label: String,
    onClick: () -> Unit,
    labelColor: androidx.compose.ui.graphics.Color = CivilightTheme.colors.ink,
    top: @Composable () -> Unit,
) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .startHairline(CivilightTheme.colors.line)
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        top()
        CivilightText(label, CivilightTheme.type.chip.merge(TextStyle(fontSize = 12.sp)), color = labelColor, maxLines = 1)
    }
}

/**
 * DS-44 swipe-left reveal: the block slides left by 192 dp over its own `bg` fill to show [actions]
 * (usually [SwipeTabsRow]). Only left swipes exist; nothing executes on swipe. Motion: `Swipe` 260 ms
 * (reduced: 120 ms fade timing). Disabled while the block is being typed ([enabled] false).
 */
@Composable
fun SwipeRevealBox(
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val reduced = CivilightTheme.reducedMotion
    val widthPx = with(LocalDensity.current) { CivilightTheme.sizes.swipeRow.toPx() }
    val offset = remember { Animatable(if (revealed) -widthPx else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(revealed, widthPx) {
        offset.animateTo(if (revealed) -widthPx else 0f, CivilightMotion.spec(CivilightMotion.swipe, reduced))
    }
    // Hidden buttons are not composed, so a tap on the closed block never falls through to them.
    val showActions by remember { derivedStateOf { offset.value < -SWIPE_EDGE_PX } }
    // The swipe sits on this stationary outer box, not on the sliding block: deltas are read in coordinates that
    // never move, and the revealed buttons (its children) see their taps first. On the block before `.offset`,
    // its hit area stayed over the whole row and swallowed the buttons' taps.
    Box(
        modifier
            .clipToBounds()
            .horizontalSwipe(
                enabled = enabled,
                canStart = { dx -> GestureRules.canMove(dx, offset.value, -widthPx, 0f) },
                onDelta = { delta -> scope.launch { offset.snapTo((offset.value + delta).coerceIn(-widthPx, 0f)) } },
                onStop = {
                    scope.launch {
                        val open = offset.value < -widthPx / 2
                        offset.animateTo(if (open) -widthPx else 0f, CivilightMotion.spec(CivilightMotion.swipe, reduced))
                        if (open != revealed) onRevealedChange(open)
                    }
                },
            ),
    ) {
        if (showActions) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.graphicsLayer { alpha = (-offset.value / widthPx).coerceIn(0f, 1f) }) { actions() }
            }
        }
        // The block only slides; its hit area moves with it, so it never covers the revealed buttons.
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(CivilightTheme.colors.bg),
        ) { content() }
    }
}

/** Offset (px) past which a slid block counts as off rest (its buttons are composed). */
private const val SWIPE_EDGE_PX: Float = 0.5f

/** Which side of a two-sided swipe row is open (SCR-30). */
enum class SwipeSide {
    CLOSED,

    /** Swiped left: the block slid left and `◆ 시간 · 참고 · 속성` show at its right end. */
    REFERENCE,

    /** Swiped right: the block slid right and `고치기 · 이 뒤에 · 종류 · 씬 나누기 · 지우기` show at its left end. */
    EDIT,
}

/**
 * Pure rules of the two-sided swipe row (SCR-30, JVM tested). Offsets are px, negative = slid left. A swipe
 * only reveals: nothing here acts on the document.
 */
object SwipeReveal {
    /** Left reveal width (3 × 64), dp. */
    const val REFERENCE_WIDTH_DP: Float = 192f

    /** Right reveal width (5 × 60), dp. */
    const val EDIT_WIDTH_DP: Float = 300f

    /** Where the block rests for [side]. */
    fun restOffset(side: SwipeSide, referenceWidth: Float, editWidth: Float): Float = when (side) {
        SwipeSide.CLOSED -> 0f
        SwipeSide.REFERENCE -> -referenceWidth
        SwipeSide.EDIT -> editWidth
    }

    /** A drag's offset kept inside the sides that may open ([editEnabled] false: no right swipe). */
    fun clamp(offset: Float, referenceWidth: Float, editWidth: Float, referenceEnabled: Boolean = true, editEnabled: Boolean = true): Float =
        offset.coerceIn(if (referenceEnabled) -referenceWidth else 0f, if (editEnabled) editWidth else 0f)

    /** On release: past half a side's width the row stays open on that side; otherwise it closes. */
    fun settle(offset: Float, referenceWidth: Float, editWidth: Float, referenceEnabled: Boolean = true, editEnabled: Boolean = true): SwipeSide = when {
        referenceEnabled && offset < -referenceWidth / 2f -> SwipeSide.REFERENCE
        editEnabled && offset > editWidth / 2f -> SwipeSide.EDIT
        else -> SwipeSide.CLOSED
    }

    /**
     * Whether the `◆ 시간 · 참고 · 속성` layer is composed: only while the block sits left of rest and the side
     * is enabled. The two layers overlap on a cover screen (192 + 300 > 390), so a hidden layer must not exist
     * at all, or it would take the taps meant for the shown one.
     */
    fun showsReference(offset: Float, referenceEnabled: Boolean = true): Boolean = referenceEnabled && offset < -EDGE_PX

    /** Whether the five-button layer is composed: only while the block sits right of rest and the side is enabled. */
    fun showsEdit(offset: Float, editEnabled: Boolean = true): Boolean = editEnabled && offset > EDGE_PX

    private const val EDGE_PX = 0.5f

    /** The open side after a button tap: the button ran (the caller's), and the row closes. */
    fun afterTap(): SwipeSide = SwipeSide.CLOSED
}

/** The five right-swipe buttons (SCR-30), each 60 dp: icon 18 over its label, class b. */
@Composable
fun EditSwipeButtonsRow(
    onEdit: () -> Unit,
    onInsertAfter: () -> Unit,
    onKind: () -> Unit,
    onSceneCut: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    open: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val items = listOf(
        Triple(IconId.Edit, io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy.EDIT, onEdit),
        Triple(IconId.InsertAfter, io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy.INSERT_AFTER, onInsertAfter),
        Triple(IconId.Kind, io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy.KIND, onKind),
        Triple(IconId.SceneCut, io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy.SCENE_CUT, onSceneCut),
        Triple(IconId.Trash, io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy.DELETE, onDelete),
    )
    Row(modifier.width(SwipeReveal.EDIT_WIDTH_DP.dp).fillMaxHeight().background(colors.raise)) {
        items.forEachIndexed { i, (icon, label, onClick) ->
            StaggeredSwipeButton(i, open, label, onClick) { CivilightIcon(icon, null, size = 18.dp, tint = colors.ink2) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StaggeredSwipeButton(
    index: Int,
    open: Boolean,
    label: String,
    onClick: () -> Unit,
    top: @Composable () -> Unit,
) {
    val motion = io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion.current
    val shown = remember { Animatable(if (open) 1f else 0f) }
    LaunchedEffect(open) {
        // Buttons appear 60 ms apart (SCR-30, SCR-67); they leave together.
        if (open) shown.animateTo(1f, motion.spec(CivilightMotion.swipe, delayMs = index * CivilightMotion.swipeStaggerMs))
        else shown.snapTo(0f)
    }
    Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer { alpha = shown.value }) {
        Row(Modifier.fillMaxSize()) {
            SwipeButton(label, onClick, top = top)
        }
    }
}

/**
 * SCR-30 two-sided swipe row: dragging left reveals [referenceActions] (`◆ 시간 · 참고 · 속성`, 192 dp,
 * usually [SwipeTabsRow]) at the right; dragging right reveals [editActions] (300 dp, usually
 * [EditSwipeButtonsRow]) at the left. Released past half a width the row stays open, otherwise it returns.
 * A swipe only reveals buttons: the only callback a drag fires is [onSideChange]; actions run on a button tap
 * (the caller closes the row then). [editEnabled] false (`고치기` active, read-only) turns the right swipe off.
 * Motion: `swipe` 260 ms (reduced: snap).
 */
@Composable
fun TwoSidedSwipeRow(
    side: SwipeSide,
    onSideChange: (SwipeSide) -> Unit,
    referenceActions: @Composable () -> Unit,
    editActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    referenceEnabled: Boolean = true,
    editEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val motion = io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion.current
    val density = LocalDensity.current
    val refPx = with(density) { SwipeReveal.REFERENCE_WIDTH_DP.dp.toPx() }
    val editPx = with(density) { SwipeReveal.EDIT_WIDTH_DP.dp.toPx() }
    val offset = remember { Animatable(SwipeReveal.restOffset(side, refPx, editPx)) }
    val scope = rememberCoroutineScope()
    val effectiveSide = when {
        side == SwipeSide.EDIT && !editEnabled -> SwipeSide.CLOSED
        side == SwipeSide.REFERENCE && !referenceEnabled -> SwipeSide.CLOSED
        else -> side
    }
    LaunchedEffect(effectiveSide, refPx, editPx) {
        offset.animateTo(SwipeReveal.restOffset(effectiveSide, refPx, editPx), motion.move(CivilightMotion.swipe))
    }
    val refOn by rememberUpdatedState(referenceEnabled)
    val editOn by rememberUpdatedState(editEnabled)
    val showRef by remember { derivedStateOf { SwipeReveal.showsReference(offset.value, refOn) } }
    val showEdit by remember { derivedStateOf { SwipeReveal.showsEdit(offset.value, editOn) } }
    // As in [SwipeRevealBox]: the swipe on the stationary outer box, only the block slides.
    Box(
        modifier
            .clipToBounds()
            .horizontalSwipe(
                enabled = referenceEnabled || editEnabled,
                canStart = { dx ->
                    GestureRules.canMove(
                        dx,
                        offset.value,
                        if (refOn) -refPx else minOf(offset.value, 0f),
                        if (editOn) editPx else maxOf(offset.value, 0f),
                    )
                },
                onDelta = { delta ->
                    scope.launch {
                        offset.snapTo(SwipeReveal.clamp(offset.value + delta, refPx, editPx, referenceEnabled, editEnabled))
                    }
                },
                onStop = {
                    scope.launch {
                        val next = SwipeReveal.settle(offset.value, refPx, editPx, referenceEnabled, editEnabled)
                        offset.animateTo(SwipeReveal.restOffset(next, refPx, editPx), motion.move(CivilightMotion.swipe))
                        if (next != side) onSideChange(next)
                    }
                },
            ),
    ) {
        if (showRef) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.graphicsLayer { alpha = (-offset.value / refPx).coerceIn(0f, 1f) }) { referenceActions() }
            }
        }
        if (showEdit) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.graphicsLayer { alpha = (offset.value / editPx).coerceIn(0f, 1f) }) { editActions() }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(CivilightTheme.colors.bg),
        ) { content() }
    }
}
