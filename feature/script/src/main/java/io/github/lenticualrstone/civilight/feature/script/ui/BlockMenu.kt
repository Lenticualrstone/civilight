package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.rememberSystemBottom
import io.github.lenticualrstone.civilight.designsystem.component.statusTop
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.glass.SCRIM_ALPHA
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.MarkerGlyph
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.input.WriteCopy
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.script.BlockKind
import kotlinx.coroutines.launch

/** (0.2.4) What a row of the block menu does; its group draws a line between groups. */
enum class BlockMenuItem(val label: String, val group: Group) {
    EDIT(IconButtonCopy.EDIT, Group.EDIT),
    INSERT_AFTER(IconButtonCopy.INSERT_AFTER, Group.EDIT),
    KIND(IconButtonCopy.KIND, Group.EDIT),
    SCENE_CUT(IconButtonCopy.SCENE_CUT, Group.EDIT),
    BRANCH("다른 전개 쓰기", Group.EDIT),
    PLANT(WriteCopy.PLANT, Group.MARKS),
    MENTION_OR_PAYOFF(WriteCopy.MENTION_OR_PAYOFF, Group.MARKS),
    DISPLAY_NAME(WriteCopy.BLOCK_DISPLAY_NAME, Group.MARKS),
    TIME(PanelTab.TIME.label, Group.REFERENCE),
    REF(PanelTab.REF.label, Group.REFERENCE),
    PROPS(PanelTab.PROPS.label, Group.REFERENCE),
    DELETE(IconButtonCopy.DELETE, Group.DELETE),
    ;

    enum class Group { EDIT, MARKS, REFERENCE, DELETE }

    /** `지우기` is drawn in `warn` (icon and label), apart from the rest. */
    val warning: Boolean get() = this == DELETE

    /** The icon of the row; null = the `◆` marker glyph (`시간`). */
    val icon: IconId?
        get() = when (this) {
            EDIT -> IconId.Edit
            INSERT_AFTER -> IconId.InsertAfter
            KIND -> IconId.Kind
            SCENE_CUT -> IconId.SceneCut
            BRANCH -> IconId.Branch
            PLANT -> IconId.Pin
            MENTION_OR_PAYOFF -> IconId.Check
            DISPLAY_NAME -> IconId.Label
            TIME -> null
            REF -> IconId.Paper
            PROPS -> IconId.Props
            DELETE -> IconId.Trash
        }
}

/** One row of the block menu as the screen wires it. [accent]: `시간` of a block with a change (`acc`, as the swipe). */
class BlockMenuEntry(val item: BlockMenuItem, val onClick: () -> Unit, val accent: Boolean = false) {
    val label: String get() = item.label
}

/** The open block menu: which block, and the row's bounds in window px when it was pressed. */
data class BlockMenuOpen(val rowKey: String, val blockKey: Key, val bounds: Rect, val closing: Boolean = false) {
    /** The row that stays lifted: none once the menu starts closing, so the block settles while the scrim fades. */
    val liftedRow: String? get() = rowKey.takeUnless { closing }
}

/** Where the menu card goes: top-left in the overlay (px) and the height it may take before it scrolls. */
data class BlockMenuPlace(val x: Float, val y: Float, val maxHeight: Float)

/**
 * (0.2.4) Pure rules of a block's long-press menu (JVM-tested). The menu holds every action of both swipe sides
 * plus the 속성 actions, so a long press reaches them all in one place:
 * `고치기 · 이 뒤에 · 종류 · 씬 나누기` | `복선 심기 · 다시 언급 · 거두기 · 이 블록만 · 표시 이름` | `◆ 시간 · 참고 · 속성` | `지우기`.
 * The same rules as the swipe: no edit row when [edit] is off (read-only, `대사만`, during `고치기`, a block of an
 * unknown kind), no `고치기` on a block whose text would show a locked hidden item ([editLocked]).
 */
object BlockMenu {
    /** Row height (a 48 dp touch target), the line between groups and the card's padding, dp. */
    const val ROW_DP: Float = 48f
    const val DIVIDER_DP: Float = 9f
    const val PAD_DP: Float = 6f
    const val WIDTH_DP: Float = 248f

    /** Gap between the block and the card, the least room worth placing it in, and the hole's padding, dp. */
    const val GAP_DP: Float = 8f
    const val MIN_ROOM_DP: Float = 160f
    const val HOLE_PAD_DP: Float = 6f
    const val SIDE_DP: Float = 12f

    fun items(edit: Boolean, editLocked: Boolean, kind: BlockKind?): List<BlockMenuItem> {
        val on = edit && kind != null
        return buildList {
            if (on) {
                if (!editLocked) add(BlockMenuItem.EDIT)
                add(BlockMenuItem.INSERT_AFTER)
                add(BlockMenuItem.KIND)
                add(BlockMenuItem.SCENE_CUT)
                add(BlockMenuItem.BRANCH)
                // `복선 심기` on every kind but `작가 메모` (as in 속성); `다시 언급 · 거두기` whenever 속성 offers it.
                if (kind != BlockKind.MEMO) add(BlockMenuItem.PLANT)
                add(BlockMenuItem.MENTION_OR_PAYOFF)
                // `이 블록만 · 표시 이름` is a dialogue block's.
                if (kind == BlockKind.DIALOGUE) add(BlockMenuItem.DISPLAY_NAME)
            }
            add(BlockMenuItem.TIME)
            add(BlockMenuItem.REF)
            add(BlockMenuItem.PROPS)
            if (on) add(BlockMenuItem.DELETE)
        }
    }

    fun items(row: BlockRow, off: Boolean): List<BlockMenuItem> = items(edit = !off, editLocked = row.editLocked, kind = row.kind)

    /** The left swipe's buttons (right end): the edit rows the swipe has room for, in order. */
    fun swipeEdit(items: List<BlockMenuItem>): List<BlockMenuItem> = items.filter { it in SWIPE_EDIT }

    private val SWIPE_EDIT = setOf(BlockMenuItem.EDIT, BlockMenuItem.INSERT_AFTER, BlockMenuItem.KIND, BlockMenuItem.SCENE_CUT, BlockMenuItem.DELETE)

    /** A line goes above row [i] when it starts a new group. */
    fun dividerBefore(items: List<BlockMenuItem>, i: Int): Boolean = i > 0 && items[i].group != items[i - 1].group

    /** The card's full height for [items], dp. */
    fun heightDp(items: List<BlockMenuItem>): Float =
        PAD_DP * 2 + ROW_DP * items.size + DIVIDER_DP * items.indices.count { dividerBefore(items, it) }

    /**
     * Places the card next to the block (all px, in the overlay): below it when it fits, else above, else on the
     * side with more room when that is at least [minRoom] (scrolling inside), else over the lower part of the
     * area. Its right edge lines up with the block's (the thumb side), kept [side] from both edges.
     */
    fun place(
        block: Rect,
        menuWidth: Float,
        menuHeight: Float,
        areaWidth: Float,
        areaHeight: Float,
        top: Float,
        bottom: Float,
        side: Float,
        gap: Float,
        minRoom: Float,
    ): BlockMenuPlace {
        val maxX = (areaWidth - side - menuWidth).coerceAtLeast(side)
        val x = (block.right - menuWidth).coerceIn(side, maxX)
        val below = areaHeight - bottom - (block.bottom + gap)
        val above = block.top - gap - top
        return when {
            menuHeight <= below -> BlockMenuPlace(x, block.bottom + gap, below)
            menuHeight <= above -> BlockMenuPlace(x, block.top - gap - menuHeight, above)
            maxOf(below, above) >= minRoom ->
                if (below >= above) BlockMenuPlace(x, block.bottom + gap, below)
                else BlockMenuPlace(x, block.top - gap - above, above)
            else -> {
                val room = (areaHeight - top - bottom).coerceAtLeast(0f)
                BlockMenuPlace(x, areaHeight - bottom - minOf(menuHeight, room), room)
            }
        }
    }

    /** The scrim's hole: the lifted block ([scale] about its centre) plus [pad] all round. */
    fun hole(block: Rect, scale: Float, pad: Float): Rect {
        val c = block.center
        val hw = block.width * scale / 2f + pad
        val hh = block.height * scale / 2f + pad
        return Rect(c.x - hw, c.y - hh, c.x + hw, c.y + hh)
    }
}

/**
 * (0.2.4) The long-press menu of a block: the screen dims (`shade` scrim) except for the lifted block, and a
 * glass card lists [entries] (each ≥ 48 dp, icon + label; `지우기` in `warn`, after a line) next to the block.
 * A tap on the scrim or back closes it on `exit`; a row closes it at once and runs. Opens on `enter` (the card
 * rises 6 dp and fades in; reduced motion: fade only).
 */
@Composable
fun BlockMenuOverlay(open: BlockMenuOpen, entries: List<BlockMenuEntry>, onClosing: () -> Unit = {}, onDismiss: () -> Unit) {
    val motion = CivilightTheme.motion
    val colors = CivilightTheme.colors
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    val closingStarted by rememberUpdatedState(onClosing)
    var closing by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(Unit) { progress.animateTo(1f, motion.spec(CivilightMotion.enter)) }
    fun close() {
        if (closing) return
        closing = true
        // The block settles on `exit` together with the scrim's fade, not after it.
        closingStarted()
        scope.launch {
            progress.animateTo(0f, motion.spec(CivilightMotion.exit))
            dismiss()
        }
    }
    LocalBackHandler.current(true) { close() }
    val items = entries.map { it.item }
    val statusTop = statusTop()
    val systemBottom = rememberSystemBottom()
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .semantics { paneTitle = ScriptCopy.BLOCK_MENU },
    ) {
        val at = origin ?: return@BoxWithConstraints
        val block = open.bounds.translate(-at.x, -at.y)
        val lift = if (motion.reduced) 1f else CivilightMotion.liftScale
        val px = with(density) { 1.dp.toPx() }
        val hole = BlockMenu.hole(block, lift, BlockMenu.HOLE_PAD_DP * px)
        // Scrim: everything but the lifted block; a tap on it closes the menu.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .drawBehind {
                    val path = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addRoundRect(RoundRect(hole, CornerRadius(HOLE_RADIUS_DP * px)))
                    }
                    drawPath(path, colors.shade)
                }
                .pointerInput(Unit) { detectTapGestures { close() } }
                .clearAndSetSemantics {
                    contentDescription = ScriptCopy.CLOSE
                    onClick(ScriptCopy.CLOSE) { close(); true }
                },
        )
        val place = BlockMenu.place(
            block = block,
            menuWidth = BlockMenu.WIDTH_DP * px,
            menuHeight = BlockMenu.heightDp(items) * px,
            areaWidth = constraints.maxWidth.toFloat(),
            areaHeight = constraints.maxHeight.toFloat(),
            top = with(density) { (statusTop + BlockMenu.GAP_DP.dp).toPx() },
            bottom = with(density) { (systemBottom + BlockMenu.GAP_DP.dp).toPx() },
            side = BlockMenu.SIDE_DP * px,
            gap = BlockMenu.GAP_DP * px,
            minRoom = BlockMenu.MIN_ROOM_DP * px,
        )
        val shape = RoundedCornerShape(CARD_RADIUS_DP.dp)
        val rise = 6.dp
        Box(
            Modifier
                .offset { IntOffset(place.x.toInt(), place.y.toInt()) }
                .width(BlockMenu.WIDTH_DP.dp)
                .heightIn(max = with(density) { place.maxHeight.toDp() })
                .graphicsLayer {
                    alpha = progress.value
                    translationY = if (motion.reduced) 0f else (1f - progress.value) * rise.toPx()
                }
                .tokenShadow(shape, CivilightTheme.elevation.popover(colors))
                .clip(shape)
                // The popover glass (DS-26) as `Popover` draws it: this card sits over the screen that a sheet layout
                // records as its glass source, so it uses the SCRIM fill (`raise` 92 %), plain `raise` when opaque.
                .background(if (CivilightTheme.glassMode == GlassMode.OPAQUE) colors.raise else colors.raise.copy(alpha = SCRIM_ALPHA))
                // The card's own padding is not the scrim: a tap there does nothing.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(
                Modifier
                    .insetOutline(colors.edge, 1.dp, shape)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = BlockMenu.PAD_DP.dp),
            ) {
                entries.forEachIndexed { i, entry ->
                    if (BlockMenu.dividerBefore(items, i)) {
                        Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).drawBehind { drawRect(colors.line) })
                    }
                    MenuRow(entry) {
                        // A row runs at once: the menu goes without its fade, the action's own sheet or panel comes next.
                        if (!closing) {
                            closing = true
                            dismiss()
                            entry.onClick()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(entry: BlockMenuEntry, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val tint = when {
        entry.item.warning -> colors.warn
        entry.accent -> colors.acc
        else -> colors.ink
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BlockMenu.ROW_DP.dp)
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON, scaleOnPress = false)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) {
            val icon = entry.item.icon
            if (icon == null) MarkerGlyph(MarkerKind.CHANGE, size = 11.dp)
            else CivilightIcon(icon, null, size = 18.dp, tint = if (entry.item.warning) colors.warn else colors.ink2)
        }
        CivilightText(entry.label, CivilightTheme.type.line, color = tint, maxLines = 1)
    }
}

private const val CARD_RADIUS_DP = 16
private const val HOLE_RADIUS_DP = 12f
