package io.github.lenticualrstone.civilight.feature.library.shelf.views

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.component.ReportBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.rememberSystemBottom
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.LocalGlassSource
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.glass.glassSource
import io.github.lenticualrstone.civilight.designsystem.glass.rememberGlassSource
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.card.rememberTilt
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.Unhide
import io.github.lenticualrstone.civilight.feature.library.shelf.COVER_PROTECTION
import io.github.lenticualrstone.civilight.feature.library.shelf.LibraryTourSteps
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfState
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import io.github.lenticualrstone.civilight.feature.library.tour.tourTarget
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/** The scroll position: the settle spring, or the finger while a drag runs. Read only in draw lambdas. */
@Stable
internal class ScenePosition(start: Float) {
    val anim = Animatable(start)
    val dragging = mutableStateOf(false)
    val drag = mutableFloatStateOf(start)
    val value: Float get() = if (dragging.value) drag.floatValue else anim.value
}

/**
 * The boards' `--tx` / `--ty` (−1 … 1: the device tilt plus the idle sway when nothing moves it) and board C's
 * keyframed idle ([idle] −1 … 1, `lcIdle 11s ease-in-out`). Written by one frame loop; read in draw only.
 */
@Stable
internal class Sway {
    val tx = mutableFloatStateOf(0f)
    val ty = mutableFloatStateOf(0f)
    val idle = mutableFloatStateOf(0f)
    val x: Float get() = tx.floatValue
    val y: Float get() = ty.floatValue
}

/** Which board's idle a view runs (A: `l3idle`, B: the slow sine pair, C: `lcIdle`). */
internal enum class Board { A, B, C }

internal fun boardOf(style: LibraryStyle): Board = when (style) {
    LibraryStyle.MANUSCRIPT_STACK, LibraryStyle.ARCHIVE_HALL -> Board.B
    LibraryStyle.INDEX_DRAWER, LibraryStyle.CONSTELLATION -> Board.C
    else -> Board.A
}

/** The idle sway of each board, pure (`t` in seconds), so the tests read the boards' numbers. */
internal object IdleSway {
    /** Board A script: `sin(t · 0.84) · 0.42`, `sin(t · 0.62 + 1.1) · 0.2`, followed at 0.07 per 60 Hz frame. */
    fun ax(t: Float): Float = sin(t * 0.84f) * 0.42f
    fun ay(t: Float): Float = sin(t * 0.62f + 1.1f) * 0.2f
    const val A_TAU_MS = 230f

    /** Board B script: `sin(t / 2900) · .55`, `sin(t / 4300) · .35` (t in ms), followed at 0.06 per frame. */
    fun bx(t: Float): Float = sin(t * 1000f / 2900f) * 0.55f
    fun by(t: Float): Float = sin(t * 1000f / 4300f) * 0.35f
    const val B_TAU_MS = 270f

    /** Board C `lcIdle`: −1 at 0 %, +1 at 50 %, −1 at 100 % of 11 s, each half on `ease-in-out`. */
    fun c(t: Float): Float {
        val period = CivilightMotion.libIdleSwayMs / 1000f
        val u = (t % period) / period
        val half = if (u < 0.5f) u * 2f else (1f - u) * 2f
        return -1f + 2f * Css.easeInOut(half)
    }

    fun follow(prev: Float, target: Float, dtMs: Float, tauMs: Float): Float =
        if (dtMs <= 0f) prev else prev + (target - prev) * (1f - exp(-dtMs / tauMs))
}

/** Everything a view reads to draw one frame. */
@Stable
internal class SceneArgs(
    val style: LibraryStyle,
    val items: List<ShelfCardItem>,
    val window: IntRange,
    val front: Int,
    /** The view's own pick (책장 꺼내기, 더미 들어 보기, 서랍 꺼내기, 별 앞으로). */
    val picked: Boolean,
    /** `열기` has started the board's open move. */
    val opening: Boolean,
    /** 원고 더미: spread (true) or piled. */
    val fanned: Boolean,
    val position: ScenePosition,
    val sway: Sway,
    /** 별자리: the orbits' turn under the finger (radians); read in draw only. */
    val spin: androidx.compose.runtime.MutableFloatState,
    val wide: Boolean,
    val frameW: Float,
    val bounds: SceneBounds,
    val unhide: Unhide?,
    /** (0.2.3) The frame's height in board units (≥ the board's 500): the scene runs on under the bottom bars. */
    val frameH: Float = ShelfScene.STAGE_H,
)

/**
 * One 서재 3D view (LIB-93, U-16 boards `scr_u16_lib_a` … `c`). (0.2.2) The scene is the whole screen: the
 * board's 390 × 500 stage between the top bar and the dock, its edges faded into `bg`, with the caption and the
 * buttons of the item in front (◀ ▶ · `열기` · the board's own buttons · ⋯) on a glass dock floating above the
 * search field, and the peek row `최근 연 파일 ⌃` under them. No `프로젝트 · N` label. Every gesture has a
 * button. Under reduced motion the item in front is drawn flat and a change is the DS-14 fade (the boards'
 * `rmc` card): no 3D, no tilt, no idle motion. Draws from the masked [ShelfViewInput.ui] only.
 */
@Composable
internal fun ShelfScene3D(style: LibraryStyle, input: ShelfViewInput) {
    val ui = input.ui
    val wide = input.wide
    val motion = CivilightTheme.motion
    val reduced = motion.reduced
    // Every board ends on the `새 프로젝트` place (책장 `nws`, 더미 dashed sheet, 회랑 door, 서랍 card, 별 slot).
    val items = remember(ui.cards, ui.lockedGroup) { ShelfCards.cardItems(ui, wide = true) }
    val keys = remember(items) { items.map { it.key } }
    val count = items.size
    val countState = rememberUpdatedState(count)
    val pair = ShelfScene.pickPair(style)
    val scope = rememberCoroutineScope()
    val settleSpec = remember(style) { settleSpec(style) }

    // The item in front and the picked item are kept by key, so a fold or a restart keeps them (CNV-34) and a
    // re-lock drops a hidden one (its key is no longer among the items).
    var focusKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var fanned by rememberSaveable { mutableStateOf(false) }
    var opening by remember { mutableStateOf(false) }
    val spin = remember { mutableFloatStateOf(0f) }
    val position = remember { ScenePosition(ShelfScene.indexOf(keys, focusKey, 0).toFloat()) }
    val front by remember { derivedStateOf { ShelfScene.nearest(position.value, countState.value) } }
    LaunchedEffect(keys) {
        val i = ShelfScene.indexOf(keys, focusKey, front)
        if (!position.dragging.value && position.anim.targetValue != i.toFloat()) position.anim.snapTo(i.toFloat())
    }
    LaunchedEffect(front, keys) {
        // No items yet (loading, a group unlocking): keep the restored item in front for when they come.
        if (keys.isEmpty()) return@LaunchedEffect
        val k = keys.getOrNull(front)?.toString()
        focusKey = k
        // A step over several items (or a spring's overshoot) passes others on the way: the pick is dropped
        // only once the front is where the move ends.
        val settledHere = !position.dragging.value && front == position.anim.targetValue.roundToInt()
        if (pickedKey != null && pickedKey != k && settledHere) pickedKey = null
    }
    val frontItem = items.getOrNull(front)
    val picked = pickedKey != null && pickedKey == frontItem?.key?.toString() && pair != null && !reduced &&
        (frontItem is ShelfCardItem.Project || style == LibraryStyle.CONSTELLATION || style == LibraryStyle.MANUSCRIPT_STACK)

    fun goTo(index: Int, keepPick: Boolean = false) {
        val i = ShelfScene.clampIndex(index, count)
        val k = items.getOrNull(i)
        pickedKey = if (keepPick && k is ShelfCardItem.Project) k.key.toString() else null
        if (style == LibraryStyle.MANUSCRIPT_STACK && !fanned && i != 0) fanned = true
        scope.launch { if (reduced) position.anim.snapTo(i.toFloat()) else position.anim.animateTo(i.toFloat(), settleSpec) }
    }

    fun openFront() {
        val project = frontItem as? ShelfCardItem.Project ?: return
        val key = project.card.row.key
        if (reduced || !opensWithMove(style)) return input.onOpen(key)
        if (opening) return
        if (style == LibraryStyle.BOOKSHELF || style == LibraryStyle.MANUSCRIPT_STACK) pickedKey = project.key.toString()
        if (style == LibraryStyle.MANUSCRIPT_STACK) fanned = true
        opening = true
        scope.launch {
            delay(CivilightMotion.libOpenHandoffMs)
            input.onOpen(key)
            // The project screen covers the 서재 by now: the board snaps back to rest behind it.
            delay(CivilightMotion.libOpenHandoffMs)
            opening = false
        }
    }

    fun togglePick() {
        val k = frontItem?.key?.toString() ?: return
        pickedKey = if (picked) null else k
        if (style == LibraryStyle.MANUSCRIPT_STACK) fanned = true
    }

    fun onTap(index: Int) {
        val item = items.getOrNull(index) ?: return
        if (opening) return
        if (style == LibraryStyle.MANUSCRIPT_STACK && !fanned) {
            // Board B: a tap on the pile spreads it, the newest sheet in front.
            fanned = true
            goTo(0)
            return
        }
        if (index != front) return goTo(index, keepPick = style == LibraryStyle.BOOKSHELF)
        when (item) {
            is ShelfCardItem.Project -> if (pair != null && !reduced && !picked) togglePick() else openFront()
            is ShelfCardItem.Lock -> input.onUnlock()
            ShelfCardItem.NewProject -> input.onNewProject()
        }
    }

    // LIB-06 on the inner display: the project the detail pane shows is brought to the front (the 3D view's
    // current item follows the selection; a hidden project is never among the items while locked).
    LaunchedEffect(input.selectedKey, keys) {
        val sel = input.selectedKey ?: return@LaunchedEffect
        val i = items.indexOfFirst { it is ShelfCardItem.Project && it.card.row.key == sel }
        if (i >= 0 && i != front && !position.dragging.value) goTo(i)
    }

    val density = LocalDensity.current.density
    val stepDp = dragStep(style)
    val dragState = rememberDraggableState { delta ->
        if (style == LibraryStyle.CONSTELLATION) {
            // Board C: `spin = spin0 + dx · 0.012` (radians), the orbit turns under the finger.
            spin.floatValue += delta / density * STAR_SPIN_PER_DP
        } else {
            val range = ShelfScene.dragRange(countState.value)
            position.drag.floatValue = (position.drag.floatValue - delta / (stepDp * density)).let { rubber(it, range) }
        }
    }
    val tilt = rememberTilt(input.tilt && !reduced && !input.preview)
    // (0.2.3) A picker preview draws a still board: no idle loop, no tilt.
    val sway = rememberSway(boardOf(style), tilt, run = !reduced && !input.preview, idle = motion.loops && !input.preview)
    val bounds = remember { SceneBounds() }
    val bg = CivilightTheme.colors.bg
    // (0.2.2) The dock blurs the stage it floats over: its own source, a sibling (never its ancestor).
    val sceneSource = rememberGlassSource()

    // (0.2.2) The scene is the whole screen: the content box is `bg`, the stage sits between the top bar and
    // the glass dock with its edges faded into `bg`, and the dock floats on top above the search field.
    Layout(
        contents = listOf(
            {
                BoxWithConstraints(
                    Modifier
                        .glassSource(sceneSource)
                        // Edges melt into `bg` (no hard top or bottom line); the stage itself stays clipped.
                        .drawWithContent {
                            clipRect { this@drawWithContent.drawContent() }
                            val hDp = size.height / density
                            val ft = ShelfScene.fadeTop(hDp) * density
                            val fb = ShelfScene.fadeBottom(hDp) * density
                            // (0.2.3) 원고 더미 and 서고 회랑 also melt into `bg` at the sides: no coloured box.
                            val fs = ShelfScene.sideFade(style, size.width / density) * density
                            if (fs > 0f) {
                                drawRect(Brush.horizontalGradient(0f to bg, 1f to bg.copy(alpha = 0f), startX = 0f, endX = fs), size = Size(fs, size.height))
                                drawRect(
                                    Brush.horizontalGradient(0f to bg.copy(alpha = 0f), 1f to bg, startX = size.width - fs, endX = size.width),
                                    topLeft = Offset(size.width - fs, 0f),
                                    size = Size(fs, size.height),
                                )
                            }
                            if (ft > 0f) drawRect(Brush.verticalGradient(0f to bg, 1f to bg.copy(alpha = 0f), startY = 0f, endY = ft), size = Size(size.width, ft))
                            if (fb > 0f) {
                                drawRect(
                                    Brush.verticalGradient(0f to bg.copy(alpha = 0f), 1f to bg, startY = size.height - fb, endY = size.height),
                                    topLeft = Offset(0f, size.height - fb),
                                    size = Size(size.width, fb),
                                )
                            }
                        }
                        // A short room shows the whole board scaled about its top centre (the plank and the
                        // corridor's floor stay in view).
                        // (0.2.3) The stage box runs from the fit's top to the viewport's foot (under the glass dock
                        // and the search field): `minHeight` carries the fit's height (the board's room), `maxHeight`
                        // the whole run. The board keeps its fit scale and the frame grows to the foot.
                        .layout { m, c ->
                            val w = c.maxWidth
                            val k = ShelfScene.stageScale(c.minHeight / density)
                            val iw = (w / k).roundToInt()
                            val ih = (ShelfScene.frameH(c.maxHeight / density, k) * density).roundToInt()
                            val p = m.measure(Constraints.fixed(iw, ih))
                            layout(w, c.maxHeight) {
                                p.placeWithLayer((w - iw) / 2, 0) {
                                    scaleX = k
                                    scaleY = k
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                }
                            }
                        }
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Horizontal,
                            enabled = !reduced && !opening && (count > 1 || style == LibraryStyle.CONSTELLATION),
                            onDragStarted = {
                                if (style != LibraryStyle.CONSTELLATION) {
                                    position.anim.stop()
                                    position.drag.floatValue = position.anim.value
                                    position.dragging.value = true
                                    if (style != LibraryStyle.BOOKSHELF) pickedKey = null
                                    if (style == LibraryStyle.MANUSCRIPT_STACK) fanned = true
                                }
                            },
                            onDragStopped = { velocity ->
                                if (style == LibraryStyle.CONSTELLATION) return@draggable
                                val from = position.drag.floatValue
                                val target = ShelfScene.settle(from, -velocity / (stepDp * density), countState.value)
                                position.anim.snapTo(from)
                                position.dragging.value = false
                                if (style == LibraryStyle.BOOKSHELF && pickedKey != null) {
                                    (items.getOrNull(target) as? ShelfCardItem.Project)?.let { pickedKey = it.key.toString() }
                                }
                                position.anim.animateTo(target.toFloat(), settleSpec)
                            },
                        ),
                ) {
                    val frameW = maxWidth.value
                    val frameH = maxHeight.value
                    // 별자리 places every star over all items (board C), so it composes them all, up to [STAR_CAP].
                    val window = if (style == LibraryStyle.CONSTELLATION) ShelfScene.window(front, count, SceneReach(STAR_CAP / 2, STAR_CAP - 1 - STAR_CAP / 2)) else ShelfScene.window(front, count, ShelfScene.reach(style, wide))
                    for (i in window) key(items[i].key) { ItemProtection(items[i]) }
                    if (reduced) {
                        // The board's own 500 at the top of the frame (the frame runs on under the bars).
                        Box(Modifier.fillMaxWidth().height(ShelfScene.STAGE_H.dp)) { FlatFront(style, frontItem, input) { onTap(front) } }
                    } else {
                        val args = SceneArgs(
                            style, items, window, front, picked, opening, fanned || style != LibraryStyle.MANUSCRIPT_STACK,
                            position, sway, spin, wide, frameW, bounds, input.unhide, frameH,
                        )
                        when (style) {
                            LibraryStyle.BOOKSHELF -> BookcaseView(args, ::onTap)
                            LibraryStyle.COVER_SPIN -> CoverFlowView(args, ::onTap)
                            LibraryStyle.MANUSCRIPT_STACK -> DeskView(args, ::onTap)
                            LibraryStyle.ARCHIVE_HALL -> HallView(args, ::onTap)
                            LibraryStyle.INDEX_DRAWER -> DrawerView(args, ::onTap)
                            LibraryStyle.CONSTELLATION -> StarsView(args, ::onTap)
                            else -> Unit
                        }
                        if (!input.preview) ItemNodes(args, pair, ::onTap)
                    }
                }
            },
            {
                CompositionLocalProvider(LocalGlassSource provides sceneSource) {
                    GlassDock(barBottom = input.barBottom, covered = input.covered || input.preview, onRecents = input.onRecents, preview = input.preview) {
                        Controls(
                            style, frontItem, front, count, picked, pair, reduced, fanned, input,
                            goTo = { goTo(it, keepPick = style == LibraryStyle.BOOKSHELF && picked) },
                            openFront = ::openFront,
                            togglePick = ::togglePick,
                            toggleFan = {
                                fanned = !fanned
                                pickedKey = null
                                if (!fanned) goTo(0)
                            },
                        )
                    }
                }
            },
        ),
        modifier = Modifier.fillMaxSize().background(bg).then(if (input.preview) Modifier.previewInert() else Modifier),
    ) { (stageM, dockM), c ->
        val w = c.maxWidth
        val margin = DOCK_MARGIN.roundToPx()
        val dockW = minOf(w - 2 * margin, DOCK_MAX_W.roundToPx()).coerceAtLeast(0)
        // The scene is the viewport, never taller: the glass bars float over the viewport, so a scrolled scene
        // would only slide the dock under them. A short window (landscape, a large font) caps the dock between
        // the bars instead; the dock scrolls its own caption (GlassDock) and the stage runs on under the glass.
        val h = c.maxHeight
        val viewport = h / density
        val dockMax = (ShelfScene.dockMaxH(viewport, input.barTop, input.barBottom) * density).roundToInt()
        val dock = dockM.first().measure(Constraints(minWidth = dockW, maxWidth = dockW, maxHeight = dockMax))
        val barBottom = input.barBottom.dp.roundToPx()
        val gap = ShelfScene.DOCK_GAP.dp.roundToPx()
        val dockH = (dock.height + gap) / density
        val fit = ShelfScene.stageFit(viewport, input.barTop, input.barBottom, dockH)
        // (0.2.3) The scene runs on under the dock and the search field to the viewport's foot (no plain band).
        val fitH = (fit.height * density).roundToInt()
        val runH = ((viewport - fit.top) * density).roundToInt().coerceAtLeast(fitH)
        val stage = stageM.first().measure(Constraints(minWidth = w, maxWidth = w, minHeight = fitH, maxHeight = runH))
        layout(w, h) {
            stage.place(0, (fit.top * density).roundToInt())
            dock.place((w - dock.width) / 2, h - barBottom - gap - dock.height)
        }
    }
}

/** (0.2.2) The glass dock's side margin and widest width (a wide pane keeps it a dock, not a bar). */
private val DOCK_MARGIN = 12.dp
private val DOCK_MAX_W = 560.dp
private val DOCK_SHAPE = RoundedCornerShape(22.dp)

/**
 * (0.2.2) The floating glass dock over the scene: the item's caption and buttons ([content]), then the peek row
 * `최근 연 파일 ⌃` that opens the 최근 연 파일 sheet (a tap; an upward pull does the same). The fill is the
 * [DockScrim] alpha so its text keeps 4.5:1 over any cover (the scrim and opaque modes keep their denser fills).
 * It reports itself as bottom chrome, so the toast appears above it. Touches on
 * its gaps stop here and never reach the stage under it.
 */
@Composable
private fun GlassDock(barBottom: Float, covered: Boolean, onRecents: (() -> Unit)?, preview: Boolean = false, content: @Composable () -> Unit) {
    val colors = CivilightTheme.colors
    // The window's toast and popovers keep above the dock (board B's toast sits over it), not on the peek row.
    // Not while a sheet covers the dock: the toast then keeps to the bars the sheet leaves.
    var dockH by remember { mutableIntStateOf(0) }
    val dockDp = with(LocalDensity.current) { dockH.toDp() }
    if (dockH > 0 && !covered) ReportBottomChrome(barBottom.dp + ShelfScene.DOCK_GAP.dp + dockDp - rememberSystemBottom())
    // The DockScrim fill only where the dock blurs (BLUR); the scrim and opaque modes keep their own, denser
    // fills. On API 31–32 (BLUR_COSTLY) the dock takes the scrim look: a third blur over a scene redrawn each frame.
    // A picker preview never blurs (several scenes at once): the scrim look.
    val mode = CivilightTheme.glassMode.let { if (it == GlassMode.BLUR_COSTLY || preview) GlassMode.SCRIM else it }
    val fill = if (mode == GlassMode.BLUR) colors.glass.copy(alpha = if (colors.isDark) DockScrim.dark else DockScrim.light) else null
    Box(
        Modifier
            .onSizeChanged { dockH = it.height }
            .clip(DOCK_SHAPE)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        // A floating bar, not a sheet: it never takes the one costly sheet blur of API 31–32 (GlassBudget).
        GlassBackground(GlassSurface.BANNER, Modifier.matchParentSize(), shape = DOCK_SHAPE, mode = mode, fill = fill)
        OnGlass {
            Column(Modifier.fillMaxWidth().insetOutline(colors.line, 1.dp, DOCK_SHAPE)) {
                // Capped between the bars (ShelfScene.dockMaxH): a too-tall dock scrolls its caption here, opening
                // on the buttons (reverse scrolling keeps the bottom in view); the peek row below always shows.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState(), reverseScrolling = true)
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (onRecents != null) 4.dp else 12.dp),
                ) { content() }
                if (onRecents != null) PeekRow(onRecents)
            }
        }
    }
}

/** `최근 연 파일 ⌃`: a visible button (label + `up`); an upward drag on it opens the sheet as well. */
@Composable
private fun PeekRow(onOpen: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val open by rememberUpdatedState(onOpen)
    val threshold = with(LocalDensity.current) { PEEK_DRAG.toPx() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(PEEK_H)
            .pointerInput(Unit) {
                // Claims only an upward pull (past [PEEK_DRAG]); a downward or sideways drag is left to whatever
                // is under or around the row, so the peek row never swallows a gesture it does not use.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var pulled = 0f
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        pulled += change.position.y - change.previousPosition.y
                        if (pulled > viewConfiguration.touchSlop) break
                        if (pulled < -threshold) {
                            change.consume()
                            open()
                            // Swallow the rest of this pull so the tap below does not fire as well.
                            while (true) {
                                val rest = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                rest.consume()
                                if (!rest.pressed) break
                            }
                            break
                        }
                    }
                }
            }
            .pressable(onClick = onOpen, role = Role.Button, scaleOnPress = false, onClickLabel = ShelfCopy.RECENTS)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        CivilightText(ShelfCopy.RECENTS, type.small, color = colors.ink2, maxLines = 1)
        CivilightIcon(IconId.Up, contentDescription = null, size = 16.dp, tint = colors.ink2)
    }
}

/** The peek row's height (a 44 dp touch target) and how far up a drag must pull to open the sheet. */
private val PEEK_H = 44.dp
private val PEEK_DRAG = 24.dp

/** 별자리: the stars composed at once (each is six small planes). */
private const val STAR_CAP = 16

/** Board C: one px of drag turns the orbit by 0.012 rad. */
private const val STAR_SPIN_PER_DP = 0.012f

/** A drag past either end gives at a third of the finger (board A flow: `raw * 0.35` beyond the ends). */
private fun rubber(v: Float, range: ClosedFloatingPointRange<Float>): Float {
    val lo = range.start + 0.35f
    val hi = range.endInclusive - 0.35f
    return when {
        v < lo -> lo + (v - lo) * 0.35f
        v > hi -> hi + (v - hi) * 0.35f
        else -> v
    }.coerceIn(range.start - 0.5f, range.endInclusive + 0.5f)
}

/** dp of finger travel per item, from each board's script. */
internal fun dragStep(style: LibraryStyle): Float = when (style) {
    LibraryStyle.BOOKSHELF -> 40f
    LibraryStyle.COVER_SPIN -> 118f
    LibraryStyle.MANUSCRIPT_STACK -> 150f
    // Board B corridor: `cam = c0 − dd · 2.2` and one stop is 560 of camera, so about 255 dp of finger.
    LibraryStyle.ARCHIVE_HALL -> HallGeo.PHONE.d / 2.2f
    LibraryStyle.INDEX_DRAWER -> 170f
    else -> 100f
}

private fun settleSpec(style: LibraryStyle): AnimationSpec<Float> = when (style) {
    LibraryStyle.BOOKSHELF, LibraryStyle.COVER_SPIN ->
        spring(dampingRatio = CivilightMotion.libSpringDamping, stiffness = CivilightMotion.libPullStiffness)
    LibraryStyle.MANUSCRIPT_STACK -> spring(dampingRatio = CivilightMotion.libBoardBDamping, stiffness = CivilightMotion.libSheetStiffness)
    LibraryStyle.ARCHIVE_HALL -> spring(dampingRatio = CivilightMotion.libWalkDamping, stiffness = CivilightMotion.libWalkStiffness)
    LibraryStyle.INDEX_DRAWER -> spring(dampingRatio = CivilightMotion.libSpringDamping, stiffness = CivilightMotion.libFlipStiffness)
    LibraryStyle.CONSTELLATION -> spring(dampingRatio = CivilightMotion.libSpringDamping, stiffness = CivilightMotion.libStarFocusStiffness)
    else -> spring(dampingRatio = CivilightMotion.libSpringDamping, stiffness = CivilightMotion.libPullStiffness)
}

/** The boards with an open move before the project screen (책장 펼치기, 표지 뒤집기, 원고 다가오기, 회랑 걸어 들어가기). */
private fun opensWithMove(style: LibraryStyle): Boolean =
    style == LibraryStyle.BOOKSHELF || style == LibraryStyle.COVER_SPIN || style == LibraryStyle.MANUSCRIPT_STACK || style == LibraryStyle.ARCHIVE_HALL

/** The frame loop behind [Sway]: one callback for the whole loop, no allocation per frame. */
@Composable
private fun rememberSway(board: Board, tilt: DeviceTilt, run: Boolean, idle: Boolean): Sway {
    val sway = remember { Sway() }
    LaunchedEffect(board, tilt, run, idle) {
        if (!run || (!idle && tilt === DeviceTilt.Still)) {
            sway.tx.floatValue = 0f
            sway.ty.floatValue = 0f
            sway.idle.floatValue = 0f
            return@LaunchedEffect
        }
        val start = withFrameNanos { it }
        var last = start
        val onFrame: (Long) -> Unit = { now ->
            val t = (now - start) / 1_000_000_000f
            val dt = (now - last) / 1_000_000f
            last = now
            val k = if (idle) 1f else 0f
            when (board) {
                Board.A -> {
                    sway.tx.floatValue = IdleSway.follow(sway.x, (IdleSway.ax(t) * k + tilt.x).coerceIn(-1f, 1f), dt, IdleSway.A_TAU_MS)
                    sway.ty.floatValue = IdleSway.follow(sway.y, (IdleSway.ay(t) * k + tilt.y).coerceIn(-1f, 1f), dt, IdleSway.A_TAU_MS)
                }
                Board.B -> {
                    sway.tx.floatValue = IdleSway.follow(sway.x, (IdleSway.bx(t) * k + tilt.x).coerceIn(-1f, 1f), dt, IdleSway.B_TAU_MS)
                    sway.ty.floatValue = IdleSway.follow(sway.y, (IdleSway.by(t) * k + tilt.y).coerceIn(-1f, 1f), dt, IdleSway.B_TAU_MS)
                }
                Board.C -> {
                    sway.tx.floatValue = tilt.x
                    sway.ty.floatValue = tilt.y
                    sway.idle.floatValue = IdleSway.c(t) * k
                }
            }
        }
        while (true) withInfiniteAnimationFrameNanos(onFrame)
    }
    return sway
}

/**
 * The stage canvas every view draws into: [build] fills the planes for this frame (reading the position, the
 * sway and the view's animations, so only the draw phase reruns), then the planes are projected and drawn.
 * A tap goes to the nearest drawn item under the finger (the boards' flat tap strips, from the live quads).
 */
@Composable
internal fun BoxScope.SceneCanvas(args: SceneArgs, planes: PlaneBuffer, onTap: (Int) -> Unit, build: PlaneBuffer.() -> Unit) {
    val tap by rememberUpdatedState(onTap)
    Canvas(
        Modifier
            .matchParentSize()
            .clearAndSetSemantics { }
            .pointerInput(planes) {
                detectTapGestures { o ->
                    val i = planes.hit(o.x / density, o.y / density)
                    if (i >= 0) tap(i)
                }
            },
    ) {
        planes.begin()
        planes.frameH = args.frameH
        planes.build()
        planes.project()
        planes.draw(this)
        args.bounds.publish(planes, args.window)
    }
}

/**
 * One accessibility node per composed item, where the item was last drawn: named by the item, labelled by what
 * a tap does (the canvas says nothing). The lock item's node is the tour's 숨긴 프로젝트 target.
 */
@Composable
private fun BoxScope.ItemNodes(args: SceneArgs, pair: PickPair?, onTap: (Int) -> Unit) {
    for (i in args.window) {
        val item = args.items[i]
        key(item.key) {
            val name = ShelfScene.itemName(item)
            val label = ShelfScene.tapLabel(item, inFront = i == args.front, pair = pair, picked = args.picked)
            val drawn by remember(i) { derivedStateOf { args.bounds.of(i).width > 0 } }
            Box(
                Modifier
                    .layout { m, _ ->
                        val r = args.bounds.of(i)
                        val w = (r.width * density).toInt().coerceAtLeast(0)
                        val h = (r.height * density).toInt().coerceAtLeast(0)
                        val p = m.measure(Constraints.fixed(w, h))
                        layout(0, 0) { p.place((r.left * density).toInt(), (r.top * density).toInt()) }
                    }
                    .then(if (item is ShelfCardItem.Lock && drawn) Modifier.tourTarget(LibraryTourSteps.HIDDEN_GROUP) else Modifier)
                    .then(
                        if (drawn) {
                            Modifier.semantics {
                                contentDescription = name
                                role = Role.Button
                                onClick(label) { onTap(i); true }
                            }
                        } else {
                            Modifier.clearAndSetSemantics { }
                        },
                    ),
            )
        }
    }
}

/** Reduced motion: the boards' flat card (`rmc`, 192 × 296) of the item in front; a change is the DS-14 fade. */
@Composable
private fun FlatFront(style: LibraryStyle, item: ShelfCardItem?, input: ShelfViewInput, onTap: () -> Unit) {
    ContentSwap(item, Modifier.fillMaxSize(), contentAlignment = Alignment.Center, contentKey = { it?.key }, label = "shelfFlat") { shown ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (shown != null) {
                val name = ShelfScene.itemName(shown)
                val (w, h) = when (style) {
                    LibraryStyle.MANUSCRIPT_STACK -> 176f to 236f
                    LibraryStyle.INDEX_DRAWER -> 318f to 315f
                    else -> 192f to 296f
                }
                Box(
                    Modifier
                        .size(w.dp, h.dp)
                        .pressable(onClick = onTap, role = Role.Button, scaleOnPress = false, onClickLabel = ShelfScene.tapLabel(shown, inFront = true, pair = null, picked = false))
                        .semantics { contentDescription = name },
                ) {
                    Box(Modifier.matchParentSize().clearAndSetSemantics { }) {
                        when (style) {
                            LibraryStyle.MANUSCRIPT_STACK -> SheetFace(shown, input.unhide)
                            LibraryStyle.INDEX_DRAWER -> IndexCardFace(shown, 0, input.unhide)
                            else -> BookCoverFace(shown, big = true, unhide = input.unhide)
                        }
                    }
                }
            }
        }
    }
}

/** LIB-89 in every view: an open hidden project, or a picked cover of a `captureBlock` project, protects the window while drawn. */
@Composable
internal fun ItemProtection(item: ShelfCardItem) {
    val card = (item as? ShelfCardItem.Project)?.card ?: return
    if (card.hiddenOpen) {
        ProtectWindow(WindowProtection.forProject(card.row.protections))
    } else if (ShelfState.coverProtects(card.row.protections, card.cover)) {
        ProtectWindow(COVER_PROTECTION)
    }
}

/** The board docks' button height (`.rail`, `.nav` buttons: 48 px). */
private val DOCK_BUTTON_H = 48.dp

/**
 * Each board's own dock (every gesture has a button), drawn on the glass dock over the scene (0.2.2); meta and
 * counter in `ink2` so they keep 4.5:1 on the [DockScrim] fill:
 * - board A (책장, 표지 회전) and C (서랍, 별자리): title and meta on the left, `n / N` on the right; then
 *   ◀ · the view's pair (`꺼내기` · `제자리` …) · a wide `열기` · ⋯ · ▶, all 48 dp tall;
 * - board B 원고 더미: the accent `n / N` above a left-aligned title; then two full-width pills
 *   (`펼치기` / `모으기` and `열기`), 48 dp; ◀ ▶ at the ends. 서고 회랑 (board B too) keeps the A row.
 */
@Composable
private fun Controls(
    style: LibraryStyle,
    item: ShelfCardItem?,
    front: Int,
    count: Int,
    picked: Boolean,
    pair: PickPair?,
    reduced: Boolean,
    fanned: Boolean,
    input: ShelfViewInput,
    goTo: (Int) -> Unit,
    openFront: () -> Unit,
    togglePick: () -> Unit,
    toggleFan: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val desk = style == LibraryStyle.MANUSCRIPT_STACK && !reduced
    Column(Modifier.fillMaxWidth()) {
        ContentSwap(item, Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart, contentKey = { it?.key }, label = "shelfCaption") { shown ->
            val (title, meta) = when (shown) {
                is ShelfCardItem.Project -> shown.card.row.title to shown.card.row.meta
                is ShelfCardItem.Lock -> shown.group.title to shown.group.cardSub
                ShelfCardItem.NewProject -> ShelfCopy.NEW_PROJECT to null
                null -> "" to null
            }
            val counter = if (count > 1) ShelfViewCopy.position(front, count) else null
            if (desk) {
                // Board B: the accent counter above a left-aligned title.
                Column(Modifier.fillMaxWidth()) {
                    if (counter != null) CivilightText(counter, type.number, color = colors.acc, maxLines = 1)
                    CivilightText(title, type.itemTitleCompact, color = colors.ink, maxLines = 1)
                    if (meta != null) CivilightText(meta, type.small, color = colors.ink2, maxLines = 1)
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        CivilightText(title, type.itemTitleCompact, color = colors.ink, maxLines = 1)
                        if (meta != null) CivilightText(meta, type.small, color = colors.ink2, maxLines = 1)
                    }
                    if (counter != null) CivilightText(counter, type.number, color = colors.ink2, maxLines = 1)
                }
            }
        }
        ContentSwap(Triple(item, picked, fanned), Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center, contentKey = { Triple(it.first?.key, it.second, it.third) }, label = "shelfActions") { (shown, isPicked, isFanned) ->
            Row(Modifier.fillMaxWidth().height(DOCK_BUTTON_H), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CivilightIconButton(IconId.Back, ShelfViewCopy.PREV, { goTo(front - 1) }, enabled = front > 0)
                // Board B dock: `펼치기` on the pile, `모으기` on the spread sheets, full width beside `열기`.
                if (desk && count > 1 && !isPicked) {
                    CivilightButton(if (isFanned) ShelfViewCopy.GATHER else ShelfViewCopy.SPREAD, toggleFan, Modifier.weight(1f).height(DOCK_BUTTON_H), variant = ButtonVariant.GHOST)
                }
                when (shown) {
                    is ShelfCardItem.Project -> {
                        if (pair != null && !reduced && (!desk || isFanned)) {
                            CivilightButton(if (isPicked) pair.back else pair.pick, togglePick, Modifier.weight(1f).height(DOCK_BUTTON_H), variant = ButtonVariant.GHOST)
                        }
                        CivilightButton(ShelfViewCopy.OPEN, openFront, Modifier.weight(if (desk) 1f else 1.6f).height(DOCK_BUTTON_H), variant = ButtonVariant.PRIMARY)
                        // LIB-02 from the item in front; the LIB-19 `프로젝트 숨기기` step points here.
                        // Only the incoming row registers the step (both rows are composed during the crossfade).
                        val current = shown.key == item?.key && isPicked == picked && isFanned == fanned && !input.preview
                        CivilightIconButton(
                            IconId.More,
                            ShelfCopy.MORE_LABEL,
                            { input.onMenu(shown.card.row.key) },
                            if (current) Modifier.tourTarget(LibraryTourSteps.HIDE) else Modifier,
                        )
                    }
                    is ShelfCardItem.Lock -> CivilightButton(shown.group.button, input.onUnlock, Modifier.weight(1f).height(DOCK_BUTTON_H), variant = ButtonVariant.PRIMARY, icon = IconId.Fingerprint)
                    ShelfCardItem.NewProject -> CivilightButton(ShelfCopy.NEW_PROJECT, input.onNewProject, Modifier.weight(1f).height(DOCK_BUTTON_H), variant = ButtonVariant.GHOST, icon = IconId.Plus)
                    null -> Box(Modifier.weight(1f))
                }
                CivilightIconButton(IconId.Next, ShelfViewCopy.NEXT, { goTo(front + 1) }, enabled = front < count - 1)
            }
        }
    }
}

/**
 * (0.2.3) A view drawn as a picker preview: nothing in it is focusable, read or touchable (every pointer event
 * is taken before the scene sees it), so the picker's own gestures and buttons are the only ones.
 */
internal fun Modifier.previewInert(): Modifier = this
    .clearAndSetSemantics { }
    .pointerInput(Unit) {
        awaitEachGesture {
            while (true) {
                val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                e.changes.forEach { it.consume() }
                if (e.changes.none { it.pressed }) break
            }
        }
    }
