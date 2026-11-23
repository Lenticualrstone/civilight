package io.github.lenticualrstone.civilight.feature.library.shelf.views

import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.card.rememberTilt
import io.github.lenticualrstone.civilight.designsystem.card.TiltMath
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The scroll position: the settle animation, or the finger while a drag runs. Read only in layers and derived states. */
@Stable
private class ScenePosition(start: Float) {
    val anim = Animatable(start)
    val dragging = mutableStateOf(false)
    val drag = mutableFloatStateOf(start)
    val value: Float get() = if (dragging.value) drag.floatValue else anim.value
}

/**
 * One 서재 3D view (LIB-93, U-16): the frame with the items of [style], then the caption and the buttons of the
 * item in front (◀ ▶ · `열기` · the view's pair · ⋯, or `지문으로 보기` / `새 프로젝트`). Every gesture has a
 * button. Under reduced motion the item in front is drawn flat and a change is the DS-14 fade; no 3D, no tilt,
 * no idle motion. Draws from the masked [ShelfViewInput.ui] only.
 */
@Composable
internal fun ShelfScene3D(style: LibraryStyle, input: ShelfViewInput) {
    val ui = input.ui
    val wide = input.wide
    val motion = CivilightTheme.motion
    val reduced = motion.reduced
    val items = remember(ui.cards, ui.lockedGroup, wide) { ShelfCards.cardItems(ui, wide) }
    val keys = remember(items) { items.map { it.key } }
    val count = items.size
    val countState = rememberUpdatedState(count)
    val size = ShelfScene.size(style, wide)
    val pair = ShelfScene.pickPair(style)
    val scope = rememberCoroutineScope()
    val settleSpec = remember(reduced) { spring<Float>(dampingRatio = CivilightMotion.viewSettleDamping, stiffness = CivilightMotion.viewSettleStiffness) }

    // The item in front and the picked item are kept by key, so a fold or a restart keeps them (CNV-34) and a
    // re-lock drops a hidden one (its key is no longer among the items).
    var focusKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }
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
        if (pickedKey != null && pickedKey != k) pickedKey = null
    }
    val frontItem = items.getOrNull(front)
    val picked = pickedKey != null && frontItem is ShelfCardItem.Project && pickedKey == frontItem.key.toString() && pair != null && !reduced
    val lift = remember { Animatable(0f) }
    LaunchedEffect(picked, reduced) {
        val target = if (picked) 1f else 0f
        if (reduced) lift.snapTo(target) else lift.animateTo(target, settleSpec)
    }

    fun goTo(index: Int) {
        val i = ShelfScene.clampIndex(index, count)
        pickedKey = null
        scope.launch { if (reduced) position.anim.snapTo(i.toFloat()) else position.anim.animateTo(i.toFloat(), settleSpec) }
    }

    fun openFront() {
        (frontItem as? ShelfCardItem.Project)?.let { input.onOpen(it.card.row.key) }
    }

    fun onTap(index: Int) {
        val item = items.getOrNull(index) ?: return
        if (index != front) return goTo(index)
        when (item) {
            is ShelfCardItem.Project -> if (pair != null && !reduced) {
                pickedKey = if (picked) null else item.key.toString()
            } else {
                openFront()
            }
            is ShelfCardItem.Lock -> input.onUnlock()
            ShelfCardItem.NewProject -> input.onNewProject()
        }
    }

    val density = LocalDensity.current.density
    val stepDp = when (style) {
        LibraryStyle.BOOKSHELF -> ShelfScene.spineStep(wide)
        LibraryStyle.INDEX_DRAWER -> size.itemH * 0.6f
        LibraryStyle.CONSTELLATION -> 64f
        else -> size.itemW * 0.5f
    }
    val dragState = rememberDraggableState { delta ->
        val range = ShelfScene.dragRange(countState.value)
        position.drag.floatValue = (position.drag.floatValue - delta / (stepDp * density)).coerceIn(range.start, range.endInclusive)
    }
    val tilt = rememberTilt(input.tilt && !reduced)
    val side = CardMetrics.inset(wide).dp

    Column(Modifier.fillMaxWidth()) {
        SectionLabel(ui.cardSectionLabel, Modifier.padding(horizontal = side))
        BoxWithConstraints(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(size.frameH.dp)
                .clipToBounds()
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = count > 1,
                    onDragStarted = {
                        position.anim.stop()
                        position.drag.floatValue = position.anim.value
                        position.dragging.value = true
                        pickedKey = null
                    },
                    onDragStopped = { velocity ->
                        val from = position.drag.floatValue
                        val target = ShelfScene.settle(from, -velocity / (stepDp * density), countState.value)
                        position.anim.snapTo(from)
                        position.dragging.value = false
                        if (reduced) position.anim.snapTo(target.toFloat()) else position.anim.animateTo(target.toFloat(), settleSpec)
                    },
                ),
        ) {
            val frame = SceneFrame(maxWidth.value, size.frameH, size.itemW, size.itemH)
            if (reduced) {
                FlatFront(style, frontItem, wide) { onTap(front) }
            } else {
                Scene(style, items, front, position, lift, frame, wide, tilt, input, motion.loops, picked, pair, ::onTap)
            }
        }
        Controls(frontItem, front, count, picked, pair, reduced, side, input, ::goTo, ::openFront) { pickedKey = if (picked) null else frontItem?.key?.toString() }
    }
}

/** Reduced motion: the item in front, flat and centred; a change is the DS-14 fade (no 3D, no tilt). */
@Composable
private fun FlatFront(style: LibraryStyle, item: ShelfCardItem?, wide: Boolean, onTap: () -> Unit) {
    val kind = when (style) {
        LibraryStyle.MANUSCRIPT_STACK -> FaceKind.SHEET
        LibraryStyle.INDEX_DRAWER -> FaceKind.INDEX
        else -> FaceKind.COVER
    }
    val flat = if (kind == FaceKind.COVER) ShelfScene.size(LibraryStyle.COVER_SPIN, wide) else ShelfScene.size(style, wide)
    ContentSwap(item, Modifier.fillMaxSize(), contentAlignment = Alignment.Center, contentKey = { it?.key }, label = "shelfFlat") { shown ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (shown != null) {
                ItemProtection(shown)
                // Named by the item, labelled by what a tap does (the face's own words drop out of the tree).
                val name = ShelfScene.itemName(shown)
                Box(
                    Modifier
                        .size(flat.itemW.dp, flat.itemH.dp)
                        .pressable(onClick = onTap, role = Role.Button, scaleOnPress = false, onClickLabel = ShelfScene.tapLabel(shown, inFront = true, pair = null, picked = false))
                        .semantics { contentDescription = name },
                ) { ItemFace(shown, kind, flat.itemW.dp, Modifier.clearAndSetSemantics { }) }
            }
        }
    }
}

/** LIB-89 in every view: an open hidden project, or a picked cover of a `captureBlock` project, protects the window while drawn. */
@Composable
private fun ItemProtection(item: ShelfCardItem) {
    val card = (item as? ShelfCardItem.Project)?.card ?: return
    if (card.hiddenOpen) {
        ProtectWindow(WindowProtection.forProject(card.row.protections))
    } else if (ShelfState.coverProtects(card.row.protections, card.cover)) {
        ProtectWindow(COVER_PROTECTION)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Scene(
    style: LibraryStyle,
    items: List<ShelfCardItem>,
    front: Int,
    position: ScenePosition,
    lift: Animatable<Float, AnimationVector1D>,
    frame: SceneFrame,
    wide: Boolean,
    tilt: DeviceTilt,
    input: ShelfViewInput,
    loops: Boolean,
    picked: Boolean,
    pair: PickPair?,
    onTap: (Int) -> Unit,
) {
    val window = ShelfScene.window(front, items.size, ShelfScene.reach(style, wide))
    // 별자리: one slow turn; stopped (fixed at 0) when loops do not run (reduced motion, animator scale 0).
    val orbit = remember { mutableFloatStateOf(0f) }
    if (style == LibraryStyle.CONSTELLATION && loops) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            val offset = orbit.floatValue
            val period = ShelfScene.ORBIT_PERIOD_MS * 1_000_000L
            // One frame callback for the whole loop: no lambda allocated per frame.
            val onFrame: (Long) -> Unit = { now ->
                orbit.floatValue = (offset + ((now - start) % period).toFloat() / period) % 1f
            }
            while (true) withInfiniteAnimationFrameNanos(onFrame)
        }
    }
    // 별자리: one layout over all items, so a step (which moves the window) never moves a star or a label.
    val stars = remember(items, style) {
        if (style == LibraryStyle.CONSTELLATION) ShelfScene.stars(items.map(ShelfScene::starGroup)) else null
    }
    SceneBackdrop(style, frame, wide, tilt, stars, window, items.size, position, lift, orbit)
    val kind = faceKind(style, frame.itemW)
    for (index in window) {
        val item = items[index]
        key(item.key) {
            SceneItem(
                style = style,
                index = index,
                item = item,
                kind = kind,
                frame = frame,
                wide = wide,
                position = position,
                lift = lift,
                star = stars?.slots?.getOrNull(index),
                groupCount = stars?.groups?.size ?: 1,
                orbit = orbit,
                tilt = tilt,
                input = input,
                tourGroup = item is ShelfCardItem.Lock,
                front = front,
                pulled = picked && index == front,
                tapLabel = ShelfScene.tapLabel(item, inFront = index == front, pair = pair, picked = picked),
                count = items.size,
                onTap = { onTap(index) },
            )
        }
    }
    if (style == LibraryStyle.INDEX_DRAWER) DrawerFront(frame)
    if (stars != null) StarLabels(stars, frame)
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.SceneItem(
    style: LibraryStyle,
    index: Int,
    item: ShelfCardItem,
    kind: FaceKind,
    frame: SceneFrame,
    wide: Boolean,
    position: ScenePosition,
    lift: Animatable<Float, AnimationVector1D>,
    star: StarSlot?,
    groupCount: Int,
    orbit: State<Float>,
    tilt: DeviceTilt,
    input: ShelfViewInput,
    tourGroup: Boolean,
    front: Int,
    pulled: Boolean,
    tapLabel: String?,
    count: Int,
    onTap: () -> Unit,
) {
    ItemProtection(item)
    val layerPose = remember { Pose() }
    val orderPose = remember { Pose() }
    val restPose = remember { Pose() }
    fun place(out: Pose) {
        val rel = index - position.value
        if (star != null) {
            ScenePoses.star(star, groupCount, orbit.value, max(0f, 1f - abs(rel)), lift.value * max(0f, 1f - abs(rel)), frame, out)
            out.alpha *= ScenePoses.windowAlpha(index, position.value, count, ShelfScene.behind(style, wide), ShelfScene.cap(wide))
        } else {
            ScenePoses.place(style, rel, index, lift.value, wide, frame, out)
        }
    }
    // Draw order changes in steps only, so a scroll frame recomposes nothing.
    val order by remember(star, index, style, groupCount, frame) { derivedStateOf { place(orderPose); (orderPose.z * 2f).roundToInt() / 2f } }
    val hiddenOpen = (item as? ShelfCardItem.Project)?.card?.hiddenOpen == true
    val unhide = input.unhide
    // The tap target is what is visible: a spine on the shelf, nothing for a passed or faded item. The tour's
    // 숨긴 프로젝트 step points at the lock item only while it is drawn (otherwise: no target).
    val visible = ScenePoses.visibleAtRest(style, index, front, wide, frame, restPose)
    Box(
        Modifier
            .align(Alignment.Center)
            .zIndex(order)
            .size(frame.itemW.dp, frame.itemH.dp)
            .graphicsLayer {
                place(layerPose)
                val near = max(0f, 1f - min(abs(index - position.value), 1f))
                val depth = TiltMath.DEPTH_DP * (0.5f + 0.5f * near)
                translationX = (layerPose.tx + TiltMath.offset(tilt.x, depth)) * density
                translationY = (layerPose.ty + TiltMath.offset(tilt.y, depth)) * density
                rotationX = layerPose.rotX - tilt.y * 3f
                rotationY = layerPose.rotY + tilt.x * 3f
                rotationZ = layerPose.rotZ
                scaleX = layerPose.scale
                scaleY = layerPose.scale
                alpha = layerPose.alpha
                transformOrigin = TransformOrigin(0.5f, layerPose.pivotY)
                cameraDistance = ShelfScene.CAMERA_DISTANCE_DP * density
            }
            .then(if (hiddenOpen && unhide != null) Modifier.unhide(unhide) else Modifier)
            .then(if (tourGroup && visible) Modifier.tourTarget(LibraryTourSteps.HIDDEN_GROUP) else Modifier),
    ) {
        if (style == LibraryStyle.BOOKSHELF) {
            // The spine turns away over the first half of the pull, the cover turns in over the second.
            val spineW = ShelfScene.spineWidth(wide).dp
            fun turn(): Float = lift.value * max(0f, 1f - abs(index - position.value))
            SpineFace(
                item,
                spineW,
                frame.itemH.dp,
                Modifier.align(Alignment.Center).clearAndSetSemantics { }.graphicsLayer {
                    val t = turn()
                    rotationY = ScenePoses.spineRotY(t)
                    alpha = if (ScenePoses.spineShown(t)) 1f else 0f
                    cameraDistance = ShelfScene.CAMERA_DISTANCE_DP * density
                },
            )
            ItemFace(
                item,
                FaceKind.COVER,
                frame.itemW.dp,
                Modifier.clearAndSetSemantics { }.graphicsLayer {
                    val t = turn()
                    rotationY = ScenePoses.coverRotY(t)
                    alpha = if (ScenePoses.spineShown(t)) 0f else 1f
                    cameraDistance = ShelfScene.CAMERA_DISTANCE_DP * density
                },
            )
        } else {
            ItemFace(item, kind, frame.itemW.dp, Modifier.clearAndSetSemantics { })
        }
        // DS-74 light in its own layer: a tilt change re-records only the band, never the face.
        if (tilt !== DeviceTilt.Still && style != LibraryStyle.BOOKSHELF) Box(Modifier.matchParentSize().graphicsLayer().sheen(tilt))
        // The faces say nothing (cleared above); the target carries the item's name and what a tap does.
        Box(
            Modifier
                .align(Alignment.Center)
                .size(ScenePoses.hitWidth(style, wide, frame, pulled).dp, frame.itemH.dp)
                .then(
                    if (visible) {
                        val name = ShelfScene.itemName(item)
                        Modifier
                            .pressable(onClick = onTap, role = Role.Button, scaleOnPress = false, onClickLabel = tapLabel, pressOverlay = false)
                            .semantics { contentDescription = name }
                    } else {
                        Modifier.clearAndSetSemantics { }
                    },
                ),
        )
    }
}

/**
 * What stands behind the items: the shelf board, the floor line, the desk, the corridor's walls converging on
 * the vanishing point, or the constellation's lines. Drawn in the draw phase; the tilt shifts it slightly.
 */
@Composable
private fun SceneBackdrop(
    style: LibraryStyle,
    frame: SceneFrame,
    wide: Boolean,
    tilt: DeviceTilt,
    stars: StarLayout?,
    window: IntRange,
    count: Int,
    position: ScenePosition,
    lift: Animatable<Float, AnimationVector1D>,
    orbit: State<Float>,
) {
    val colors = CivilightTheme.colors
    val line = colors.line
    val edge = colors.edge
    val tint = colors.tint
    val scratch = remember { Pose() }
    // 별자리 lines: the last star drawn per cluster (x, y, alpha), so each cluster is joined in slot order.
    val last = remember { FloatArray(StarGroup.entries.size * 3) }
    val seen = remember { BooleanArray(StarGroup.entries.size) }
    // Own layer: a scroll or tilt frame re-records only the backdrop.
    Canvas(Modifier.fillMaxSize().graphicsLayer()) {
        val dp = density
        val cx = size.width / 2f - tilt.x * 4f * dp
        val cy = size.height / 2f - tilt.y * 4f * dp
        when (style) {
            LibraryStyle.BOOKSHELF -> {
                val y = cy + (frame.itemH / 2f + frame.height * 0.02f) * dp
                drawRect(edge, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, 8f * dp))
                drawLine(line, Offset(0f, y + 10f * dp), Offset(size.width, y + 10f * dp), 1f * dp)
            }
            LibraryStyle.COVER_SPIN -> {
                val y = cy + (frame.itemH / 2f + 10f) * dp
                drawLine(line, Offset(size.width * 0.1f, y), Offset(size.width * 0.9f, y), 1f * dp)
            }
            LibraryStyle.MANUSCRIPT_STACK -> {
                val top = cy + frame.itemH * 0.2f * dp
                drawRect(tint, Offset(0f, top), androidx.compose.ui.geometry.Size(size.width, size.height - top))
                drawLine(edge, Offset(0f, top), Offset(size.width, top), 1f * dp)
            }
            LibraryStyle.ARCHIVE_HALL -> {
                val vx = cx
                val vy = cy - frame.height * 0.16f * dp
                drawLine(line, Offset(0f, 0f), Offset(vx, vy), 1f * dp)
                drawLine(line, Offset(size.width, 0f), Offset(vx, vy), 1f * dp)
                drawLine(line, Offset(0f, size.height), Offset(vx, vy), 1f * dp)
                drawLine(line, Offset(size.width, size.height), Offset(vx, vy), 1f * dp)
            }
            LibraryStyle.INDEX_DRAWER -> Unit
            LibraryStyle.CONSTELLATION -> if (stars != null && !window.isEmpty()) {
                // Each cluster's stars joined in slot order (a constellation), skipping the other clusters' stars
                // between them; positions follow the orbit, and a line fades with its fainter star.
                seen.fill(false)
                for (index in window.first..window.last) {
                    val slot = stars.slots.getOrNull(index) ?: continue
                    val rel = index - position.value
                    val f = max(0f, 1f - abs(rel))
                    ScenePoses.star(slot, stars.groups.size, orbit.value, f, lift.value * f, frame, scratch)
                    val a = ScenePoses.windowAlpha(index, position.value, count, ShelfScene.behind(style, wide), ShelfScene.cap(wide))
                    val x = size.width / 2f + scratch.tx * dp
                    val y = size.height / 2f + scratch.ty * dp
                    val g = slot.group
                    if (seen[g]) {
                        val la = min(a, last[g * 3 + 2])
                        if (la > 0f) drawLine(line, Offset(last[g * 3], last[g * 3 + 1]), Offset(x, y), 1f * dp, alpha = la)
                    }
                    seen[g] = true
                    last[g * 3] = x
                    last[g * 3 + 1] = y
                    last[g * 3 + 2] = a
                }
            }
            LibraryStyle.LIST -> Unit
        }
    }
}

/** 색인 카드 서랍: the drawer's front over the cards' feet. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.DrawerFront(frame: SceneFrame) {
    val colors = CivilightTheme.colors
    val h = (frame.height / 2f - frame.itemH / 2f + frame.itemH * 0.22f).dp
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .zIndex(50f)
            .fillMaxWidth(0.9f)
            .height(h)
            .background(colors.raise, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
    ) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(56.dp, 6.dp).background(colors.edge, RoundedCornerShape(3.dp)))
    }
}

/** 별자리: the 형식 word under each cluster (static; the lock and `새 프로젝트` cluster has none). */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.StarLabels(stars: StarLayout, frame: SceneFrame) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    stars.groups.forEachIndexed { g, group ->
        val label = ShelfScene.groupLabel(group) ?: return@forEachIndexed
        val x = ScenePoses.clusterCentre(g, stars.groups.size, frame)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .graphicsLayer { translationX = x * density },
        ) { CivilightText(label, type.small, color = colors.ink3, maxLines = 1) }
    }
}

/** Under the frame: the item in front named, its buttons, ◀ ▶ (every gesture has a button). */
@Composable
private fun Controls(
    item: ShelfCardItem?,
    front: Int,
    count: Int,
    picked: Boolean,
    pair: PickPair?,
    reduced: Boolean,
    side: androidx.compose.ui.unit.Dp,
    input: ShelfViewInput,
    goTo: (Int) -> Unit,
    openFront: () -> Unit,
    togglePick: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(horizontal = side).padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CivilightIconButton(IconId.Back, ShelfViewCopy.PREV, { goTo(front - 1) }, enabled = front > 0)
            ContentSwap(item, Modifier.weight(1f), contentAlignment = Alignment.Center, contentKey = { it?.key }, label = "shelfCaption") { shown ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (shown) {
                        is ShelfCardItem.Project -> {
                            CivilightText(shown.card.row.title, type.itemTitleCompact, color = colors.ink, maxLines = 1)
                            CivilightText(shown.card.row.meta, type.small, color = colors.ink3, maxLines = 1)
                        }
                        is ShelfCardItem.Lock -> {
                            CivilightText(shown.group.title, type.itemTitleCompact, color = colors.ink, maxLines = 1)
                            CivilightText(shown.group.cardSub, type.small, color = colors.ink3, maxLines = 1)
                        }
                        ShelfCardItem.NewProject -> CivilightText(ShelfCopy.NEW_PROJECT, type.itemTitleCompact, color = colors.ink2, maxLines = 1)
                        null -> Unit
                    }
                    if (count > 1) CivilightText(ShelfViewCopy.position(front, count), type.small, color = colors.ink3, maxLines = 1)
                }
            }
            CivilightIconButton(IconId.Next, ShelfViewCopy.NEXT, { goTo(front + 1) }, enabled = front < count - 1)
        }
        ContentSwap(item to picked, Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center, contentKey = { (it.first?.key) to it.second }, label = "shelfActions") { (shown, isPicked) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                when (shown) {
                    is ShelfCardItem.Project -> {
                        CivilightButton(ShelfViewCopy.OPEN, openFront, variant = ButtonVariant.COMPACT_PRIMARY)
                        if (pair != null && !reduced) {
                            CivilightButton(if (isPicked) pair.back else pair.pick, togglePick, variant = ButtonVariant.COMPACT_GHOST)
                        }
                        // LIB-02 from the item in front; the LIB-19 `프로젝트 숨기기` step points here.
                        // Only the incoming row registers the step (both rows are composed during the crossfade).
                        val current = shown.key == item?.key && isPicked == picked
                        CivilightIconButton(
                            IconId.More,
                            ShelfCopy.MORE_LABEL,
                            { input.onMenu(shown.card.row.key) },
                            if (current) Modifier.tourTarget(LibraryTourSteps.HIDE) else Modifier,
                        )
                    }
                    is ShelfCardItem.Lock -> CivilightButton(shown.group.button, input.onUnlock, variant = ButtonVariant.COMPACT_PRIMARY, icon = IconId.Fingerprint)
                    ShelfCardItem.NewProject -> CivilightButton(ShelfCopy.NEW_PROJECT, input.onNewProject, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Plus)
                    null -> Unit
                }
            }
        }
    }
}
