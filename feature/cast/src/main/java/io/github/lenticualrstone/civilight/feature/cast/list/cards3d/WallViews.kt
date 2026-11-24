package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import io.github.lenticualrstone.civilight.designsystem.foundation.horizontalSwipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ==================================================================== 기울어진 벽 (cards-c (5))

/**
 * The wall board below the head (board y 150 → stage 0): the wall at (46, 36), 298 × 525, `perspective:
 * 1100px` at (195, 300); the pulled-out card centred at (195, 190) under `perspective: 1000px`.
 */
private const val WALL_STAGE_H = 600f
private const val WALL_X = 46f
private const val WALL_Y = 36f
private val WALL_W = CardPoses.WALL_COLS * CardPoses.WALL_CW + (CardPoses.WALL_COLS - 1) * CardPoses.WALL_GAP
private val WALL_H = CardPoses.WALL_ROWS * CardPoses.WALL_CH + (CardPoses.WALL_ROWS - 1) * CardPoses.WALL_GAP
private val WALL_SCENE = Scene(1100f, 195f, 300f, WALL_X + WALL_W / 2, WALL_Y + WALL_H / 2)
private const val PULL_CX = 195f
private const val PULL_CY = 190f
private const val PULL_PERSPECTIVE = 1000f

/** Board `.wplate`: 16 px round the wall, 18 px behind it. */
private const val PLATE_OUT = 16f
private const val PLATE_Z = -18f

/** Board `.fhint` under the pulled card. */
private const val HINT_Y = 376f

/** Where a pulled card flies from: its slot's projected pose on the wall (board `_fs`). */
private class PullFrom {
    var dx = 0f
    var dy = 0f
    var scale = 0.35f
    var rotX = 0f
    var rotY = 0f
    var rotZ = 0f
}

/**
 * Cards hung on a wall turned in 3D (board `기울어진 격자 벽`): a plate with a grid behind three columns of
 * cards set at slightly different depths; the tilt turns the wall (or it sways slowly) and slides a light
 * band across the faces. Tapping a card, or `이전 카드 · 다음 카드`, pulls it forward out of its slot under a
 * scrim, with the holo foil over it; a sideways drag on it moves to the next card, a tap or `열기` opens it and
 * `닫기` puts it back. A vertical drag on the wall scrolls a longer cast by rows.
 */
@Composable
internal fun TiltedWallView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    var pulled by rememberSaveable { mutableStateOf(false) }
    val focusItem = env.items.getOrNull(focus)
    val motion = CivilightTheme.motion
    val scope = rememberCoroutineScope()
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    val wallShadow = remember(mat) { Shadows.wall(mat) }
    val pullShadow = remember(mat) { Shadows.pulled(mat) }
    val projector = remember { Projector() }
    val tilt = env.tilt
    val lastRow = CardPoses.wallRow(max(0, n - 1))
    val maxTop = max(0, lastRow - CardPoses.WALL_ROWS + 1)
    // The first row in view (fractional while it moves): follows the focus, or a drag on the wall.
    val row = remember { Animatable(CardPoses.wallTopRow(CardPoses.wallRow(focus), lastRow).toFloat()) }
    LaunchedEffect(focus, env.reduced, lastRow) {
        // The focus row already in view stays where it is; otherwise the wall moves to show it.
        val fr = CardPoses.wallRow(focus)
        val top = row.value.roundToInt()
        if (row.value == top.toFloat() && fr in top until top + CardPoses.WALL_ROWS) return@LaunchedEffect
        val target = CardPoses.wallTopRow(fr, lastRow).toFloat()
        if (env.reduced) row.snapTo(target) else row.animateTo(target, motion.move(CivilightMotion.standard))
    }
    // The pull (0 in the wall … 1 out in front) and where it flies from.
    val pull = remember { Animatable(if (pulled) 1f else 0f) }
    val from = remember { PullFrom() }
    val dragX = remember { mutableFloatStateOf(0f) }
    val fromXf = remember { CardXform() }
    // The stage scale for the pulled card's drag (a plain holder: written while composing, read in input).
    val unitNow = remember { floatArrayOf(1f) }
    /** The pulled card's horizontal drag so far, in board px. */
    val pullRaw = remember { floatArrayOf(0f) }
    val measureFrom: (Int) -> Unit = { i ->
        val slotRow = CardPoses.wallRow(i) - row.value
        val cx = WALL_X + CardPoses.wallSlotX(i) + CardPoses.WALL_CW / 2
        val cy = WALL_Y + CardPoses.wallSlotY(slotRow) + CardPoses.WALL_CH / 2
        projector.solve(fromXf, WALL_SCENE, CardPoses.wallRotX(tilt.y), CardPoses.wallRotY(tilt.x), cx, cy, 0f, 0f, CardPoses.wallDepth(i))
        from.dx = cx + fromXf.translationX - PULL_CX
        from.dy = cy + fromXf.translationY - PULL_CY
        from.scale = fromXf.scale * CardPoses.WALL_CH / CardPoses.PULL_H
        from.rotX = fromXf.rotationX
        from.rotY = fromXf.rotationY
        from.rotZ = fromXf.rotationZ
    }
    val pullOut: (Int) -> Unit = { i ->
        measureFrom(i)
        pulled = true
        scope.launch {
            if (env.reduced) {
                pull.snapTo(1f)
            } else {
                pull.snapTo(0f)
                pull.animateTo(1f, spring(CivilightMotion.wallPullDamping, CivilightMotion.wallPullStiffness))
            }
        }
    }
    val putBack: () -> Unit = {
        measureFrom(deck.index)
        pulled = false
        scope.launch {
            if (env.reduced) pull.snapTo(0f) else pull.animateTo(0f, spring(CivilightMotion.wallPullDamping, CivilightMotion.wallPullStiffness))
        }
    }
    val pick: (Int) -> Unit = { i ->
        deck.goTo(i)
        deck.snap(i)
        pullOut(i)
    }
    val stepPulled: (Int) -> Unit = { d ->
        if (deck.canStep(d)) {
            val i = (deck.index + d).coerceIn(0, n - 1)
            pick(i)
        }
    }
    val pullShown by remember { derivedStateOf { pull.value > 0.001f } }

    Column(Modifier.fillMaxSize()) {
        BoardStage(WALL_STAGE_H, Modifier.weight(1f).fillMaxWidth()) { unit, bp ->
            unitNow[0] = unit
            ReducedFade(env.reduced, focus) { f ->
                // The wall: one turned plane (the cards' small depths read as scale), board `.wall`.
                Box(
                    Modifier.offset(bp.dp(WALL_X), bp.dp(WALL_Y)).size(bp.dp(WALL_W), bp.dp(WALL_H))
                        .graphicsLayer {
                            rotationX = CardPoses.wallRotX(tilt.y)
                            rotationY = CardPoses.wallRotY(tilt.x)
                            cameraDistance = Projector.cameraDistance(WALL_SCENE.perspective, unit)
                        }
                        .pointerInput(env.reduced, maxTop, unit) {
                            if (maxTop == 0) return@pointerInput
                            var moved = 0f
                            detectVerticalDragGestures(
                                onDragStart = { scope.launch { row.stop() } },
                                onDragEnd = {
                                    val t = CardPoses.wallDragTarget(row.value, moved, maxTop).toFloat()
                                    moved = 0f
                                    scope.launch { if (env.reduced) row.snapTo(t) else row.animateTo(t, motion.move(CivilightMotion.standard)) }
                                },
                                onDragCancel = { moved = 0f },
                            ) { change, d ->
                                change.consume()
                                val rows = -d / ((CardPoses.WALL_CH + CardPoses.WALL_GAP) * unit)
                                moved = rows
                                if (!env.reduced) scope.launch { row.snapTo(CardPoses.wallDragRow(row.value + rows, maxTop)) }
                            }
                        },
                ) {
                    // Board `.wplate`: the plate and its grid, behind the cards.
                    Box(
                        Modifier.offset(bp.dp(-PLATE_OUT), bp.dp(-PLATE_OUT))
                            .size(bp.dp(WALL_W + PLATE_OUT * 2), bp.dp(WALL_H + PLATE_OUT * 2))
                            .graphicsLayer {
                                val s = WALL_SCENE.perspective / (WALL_SCENE.perspective - PLATE_Z)
                                scaleX = s
                                scaleY = s
                            }
                            .then(rememberCardShadow(listOf(ShadowLayer(0f, 40f, 80f, 0f, mat.shadowC)), 26f, unit))
                            .clip(RoundedCornerShape(bp.dp(26f)))
                            .background(Brush.linearGradient(listOf(colors.raise, mat.wallBack), start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                            .border(1.dp, colors.line, RoundedCornerShape(bp.dp(26f)))
                            .drawBehind {
                                val k = bp.k * density
                                val inset = 10f * k
                                val step = 34f * k
                                val c = colors.line.copy(alpha = colors.line.alpha * 0.5f)
                                var x = inset
                                while (x < size.width - inset) {
                                    drawRect(c, topLeft = Offset(x, inset), size = Size(1.dp.toPx(), size.height - inset * 2))
                                    x += step
                                }
                                var y = inset
                                while (y < size.height - inset) {
                                    drawRect(c, topLeft = Offset(inset, y), size = Size(size.width - inset * 2, 1.dp.toPx()))
                                    y += step
                                }
                            },
                    )
                    val slotShadow = rememberCardShadow(wallShadow, 14f, unit)
                    for (i in CardPoses.wallWindow(row.value.roundToInt(), n)) {
                        val item = env.items[i]
                        // The slot stays empty while its card is out or still flying back.
                        val out = i == f && (pulled || pullShown)
                        key(item.key) {
                            val shape = RoundedCornerShape(bp.dp(14f))
                            Box(
                                Modifier.offset(bp.dp(CardPoses.wallSlotX(i)), 0.dp).size(bp.dp(CardPoses.WALL_CW), bp.dp(CardPoses.WALL_CH))
                                    .graphicsLayer {
                                        val r = CardPoses.wallRow(i) - row.value
                                        translationY = CardPoses.wallSlotY(r) * unit
                                        val s = WALL_SCENE.perspective / (WALL_SCENE.perspective - CardPoses.wallDepth(i))
                                        scaleX = s
                                        scaleY = s
                                        // Rows past the four in view fade at the plate's edge.
                                        alpha = (1f - max(max(-r, r - (CardPoses.WALL_ROWS - 1)), 0f)).coerceIn(0f, 1f)
                                    }
                                    .then(if (out) Modifier.border(1.5.dp, colors.edge.copy(alpha = 0.6f), shape) else slotShadow)
                                    .pressable(onClick = { if (out) putBack() else pick(i) }, scaleOnPress = false, onClickLabel = itemLabel(item), shape = shape)
                                    .cardLongPress(!out, LongPressGate.label(item), env.longPress(item))
                                    .clip(shape),
                            ) {
                                if (!out) {
                                    when (item) {
                                        is DeckItem.Lock -> LockCardFace(item.row, bp, compact = true, onUnlock = env.actions.unlock)
                                        is DeckItem.Character -> WallFace(
                                            item.card, env.input.imageOf(item.card.key), item.card.key in env.input.hiddenKeys, pulled = false, bp = bp,
                                            sheen = { Offset(tilt.x * 0.38f, tilt.y * 0.08f) },
                                            tilt = { Offset(tilt.x, tilt.y) },
                                            // Board `.focus .sl:not(.out) .tx`: with a card out, the wall keeps only its art.
                                            labels = { 1f - pull.value },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Board `.scrim` and the pulled card (`.front .fc`).
            if (pulled || pullShown) {
                val item = env.items.getOrNull(focus)
                Box(
                    Modifier.matchParentSize().zIndex(10f).graphicsLayer { alpha = pull.value.coerceIn(0f, 1f) }.background(mat.scrim)
                        .pressable(onClick = putBack, scaleOnPress = false, pressOverlay = false, onClickLabel = IconButtonCopy.CLOSE),
                )
                if (item != null) {
                    val shape = RoundedCornerShape(bp.dp(20f))
                    Box(
                        Modifier.zIndex(11f)
                            .offset(bp.dp(PULL_CX - CardPoses.PULL_W / 2), bp.dp(PULL_CY - CardPoses.PULL_H / 2))
                            .size(bp.dp(CardPoses.PULL_W), bp.dp(CardPoses.PULL_H))
                            .graphicsLayer {
                                val t = pull.value
                                val dx = dragX.floatValue
                                translationX = CardPoses.lerp(from.dx, dx, t) * unit
                                translationY = CardPoses.lerp(from.dy, 0f, t) * unit
                                val s = CardPoses.lerp(from.scale, 1f, t)
                                scaleX = s
                                scaleY = s
                                rotationX = CardPoses.lerp(from.rotX, tilt.y * -7f, t)
                                rotationY = CardPoses.lerp(from.rotY, tilt.x * 9f + dx * CardPoses.PULL_DRAG_TURN, t)
                                rotationZ = CardPoses.lerp(from.rotZ, 0f, t)
                                alpha = (t * 8f).coerceIn(0f, 1f)
                                cameraDistance = Projector.cameraDistance(PULL_PERSPECTIVE, unit)
                                transformOrigin = TransformOrigin.Center
                            }
                            .then(rememberCardShadow(pullShadow, 20f, unit))
                            // Under reduced motion the card does not follow; the release still steps (board `fup`). 0.2.3 gesture
                            // rules: undecided inside the slop (tap and long press stay), a vertical drag is let go.
                            .horizontalSwipe(
                                onStart = { pullRaw[0] = 0f },
                                onDelta = { d ->
                                    pullRaw[0] += d / unitNow[0]
                                    if (!env.reduced) dragX.floatValue = pullRaw[0]
                                },
                                onStop = {
                                    val raw = pullRaw[0]
                                    when {
                                        raw < -CardPoses.PULL_STEP_PX -> stepPulled(1)
                                        raw > CardPoses.PULL_STEP_PX -> stepPulled(-1)
                                    }
                                    pullRaw[0] = 0f
                                    scope.launch { animate(dragX.floatValue, 0f, 0f, spring(CivilightMotion.wallPullDamping, CivilightMotion.wallPullStiffness)) { v, _ -> dragX.floatValue = v } }
                                },
                            )
                            .pressable(onClick = { env.activate(item) }, scaleOnPress = false, onClickLabel = itemLabel(item), shape = shape)
                            .cardLongPress(true, LongPressGate.label(item), env.longPress(item))
                            .clip(shape),
                    ) {
                        when (item) {
                            is DeckItem.Lock -> LockCardFace(item.row, bp, compact = false, onUnlock = env.actions.unlock)
                            is DeckItem.Character -> WallFace(
                                item.card, env.input.imageOf(item.card.key), item.card.key in env.input.hiddenKeys, pulled = true, bp = bp,
                                sheen = { Offset(tilt.x * 0.38f, tilt.y * 0.08f) },
                                tilt = { Offset(tilt.x, tilt.y) },
                            )
                        }
                    }
                    // Board `.fhint`.
                    Box(Modifier.zIndex(11f).fillMaxWidth().offset(0.dp, bp.dp(HINT_Y)).graphicsLayer { alpha = pull.value.coerceIn(0f, 1f) }, contentAlignment = Alignment.TopCenter) {
                        CivilightText(
                            Cards3dCopy.WALL_HINT,
                            CivilightTheme.type.small,
                            Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(colors.raise).border(1.dp, colors.line, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            color = colors.ink2,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Readout(env, focus) { deck.pos.coerceIn(0f, (n - 1).toFloat()) }
        // `이전 카드 · 다음 카드` on the wall pull the next card forward (board), so a step always shows one card;
        // `열기` opens the focused card without pulling it first.
        ControlBar(env, onStep = { d -> if (pulled) stepPulled(d) else pick((deck.index + d).coerceIn(0, n - 1)) }) {
            if (pulled) CivilightIconButton(IconButtonSpec.CLOSE, putBack)
            OpenButton(env, focusItem)
        }
    }
}

// ==================================================================== 롤로덱스 (cards-c (6))

/**
 * The drum board below the head (board y 150 → stage 0): `perspective: 900px` at (195, 10); the drum turns
 * about (195, 410); the front card (342 × 216, its tab 24 above) rests with its bottom centre at (195, 416).
 */
private const val ROLO_STAGE_H = 480f
private val ROLO_SCENE = Scene(900f, 195f, 10f, 195f, 410f)
private const val ROLO_PIVOT_Y = 416f

/** Board `.rbase` under the drum: (36, 402), 318 × 46, radius 14. */
private const val BASE_X = 36f
private const val BASE_Y = 402f
private const val BASE_W = 318f
private const val BASE_H = 46f

/**
 * Index cards on a vertical drum (board `세로 롤로덱스`): the next cards wait above and behind (only their paper
 * and tab show), the front card falls down towards the viewer to show the next. Drag down for the next card
 * and up for the one before, `이전 카드 · 다음 카드`, or a letter of the 초성 index row; `열기` opens it.
 */
@Composable
internal fun RolodexView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    val index = remember(env.input.cards) { CardPoses.indexRow(env.input.cards.map { it.title }) }
    val focusLetter = (env.items.getOrNull(focus) as? DeckItem.Character)?.let { CardPoses.indexLetter(it.card.title) }
    val projector = remember { Projector() }
    val pose = remember { CardPose() }
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    val shadow = remember(mat) { Shadows.index(mat) }
    val tilt = env.tilt
    Column(Modifier.fillMaxSize()) {
        if (index.size > 1) {
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(index, key = { it.first }) { (letter, first) ->
                    FilterChip(letter, letter == focusLetter, { deck.goTo(first) })
                }
            }
        }
        BoardStage(ROLO_STAGE_H, Modifier.weight(1f).fillMaxWidth()) { unit, bp ->
            ReducedFade(env.reduced, focus) { f ->
                val pos: () -> Float = if (env.reduced) ({ f.toFloat() }) else ({ deck.pos })
                // Board `.rbase`: the drum's foot, turning with it.
                val baseXf = remember { CardXform() }
                Box(
                    Modifier.zIndex(BEHIND).offset(bp.dp(BASE_X), bp.dp(BASE_Y)).size(bp.dp(BASE_W), bp.dp(BASE_H))
                        .graphicsLayer {
                            projector.solve(baseXf, ROLO_SCENE, -tilt.y * 5f, tilt.x * 9f, 195f, BASE_Y + BASE_H / 2, 0f, 0f, 0f)
                            translationX = baseXf.translationX * unit
                            translationY = baseXf.translationY * unit
                            scaleX = baseXf.scale
                            scaleY = baseXf.scale
                            rotationX = baseXf.rotationX
                            rotationY = baseXf.rotationY
                            rotationZ = baseXf.rotationZ
                            cameraDistance = Projector.cameraDistance(ROLO_SCENE.perspective, unit)
                        }
                        .then(rememberCardShadow(listOf(ShadowLayer(0f, 16f, 30f, 0f, mat.shadowC)), 14f, unit))
                        .drawBehind {
                            val k = bp.k * density
                            drawRoundRect(Brush.verticalGradient(listOf(colors.chip, colors.raise)), cornerRadius = CornerRadius(14f * k))
                            drawRoundRect(
                                Brush.horizontalGradient(listOf(colors.ink3, colors.edge, colors.ink3)),
                                topLeft = Offset(-14f * k, 6f * k), size = Size(size.width + 28f * k, 8f * k), cornerRadius = CornerRadius(4f * k), alpha = 0.55f,
                            )
                        },
                )
                Box(
                    Modifier.matchParentSize().stageDrag(
                        listOf(deck, unit, env.reduced),
                        vertical = true,
                        onDrag = { d -> deck.dragBy(d / (CardPoses.ROLO_DRAG_PX * unit)) },
                        onEnd = { v -> deck.endDrag(v / (CardPoses.ROLO_DRAG_PX * unit), stepAt = CardPoses.ROLO_TURN_PX / CardPoses.ROLO_DRAG_PX) },
                    ),
                ) {
                    for (i in CardPoses.window(f, n, loop = false, before = 1, after = 4)) {
                        val item = env.items[i]
                        val front = i == f
                        key(item.key) {
                            PosedCard(
                                w = CardPoses.ROLO_W, h = CardPoses.ROLO_H + CardPoses.ROLO_TAB, pivotX = 195f, pivotY = ROLO_PIVOT_Y,
                                unit = unit, bp = bp, radius = 0f, shadow = emptyList(), camera = ROLO_SCENE.perspective,
                                originY = 1f,
                                place = { xf ->
                                    val p = CardPoses.rolodex(i - pos(), pose)
                                    if (p == null) {
                                        xf.alpha = 0f
                                    } else {
                                        projector.solve(xf, ROLO_SCENE, -tilt.y * 5f, tilt.x * 9f, 195f, ROLO_PIVOT_Y, p.x, p.y, p.z, p.rotX, order = TurnOrder.XYZ)
                                        // A falling card must stay over the next one even as its depth crosses.
                                        if (i - pos() < 0f) xf.depth += 400f
                                        // Past 90° the falling card would show mirrored: the board hides its back.
                                        xf.alpha = if (xf.backFacing) 0f else p.alpha
                                        xf.dim = 0f
                                    }
                                },
                                onTap = if (front) ({ env.activate(item) }) else ({ deck.goTo(i) }),
                                clickLabel = itemLabel(item),
                                onLongPress = env.longPress(item),
                                longPressLabel = LongPressGate.label(item),
                            ) { _ ->
                                Box(Modifier.fillMaxSize()) {
                                    val letter = (item as? DeckItem.Character)?.let { CardPoses.indexLetter(it.card.title) } ?: "#"
                                    IndexTab(
                                        letter,
                                        (item as? DeckItem.Character)?.card?.title.orEmpty(),
                                        bp,
                                        Modifier.offset(bp.dp(CardPoses.rolodexTabX(i)), 0.dp),
                                    )
                                    Box(
                                        Modifier.offset(0.dp, bp.dp(CardPoses.ROLO_TAB)).size(bp.dp(CardPoses.ROLO_W), bp.dp(CardPoses.ROLO_H))
                                            .then(rememberCardShadow(shadow, 16f, unit))
                                            .clip(RoundedCornerShape(bp.dp(16f))),
                                    ) {
                                        when (item) {
                                            is DeckItem.Lock -> LockCardFace(item.row, bp, compact = false, onUnlock = env.actions.unlock)
                                            is DeckItem.Character -> {
                                                val back = remember(item.card.key, env.input.backOf) { env.input.backOf(item.card.key) }
                                                IndexCardFace(
                                                    item.card, env.input.imageOf(item.card.key), item.card.key in env.input.hiddenKeys, back, bp,
                                                    content = { CardPoses.rolodexContent(i - pos()) },
                                                    sheen = { Offset(tilt.x * 0.38f, tilt.y * 0.08f) },
                                                    tilt = { Offset(tilt.x, tilt.y) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Board `.rhint`: the gesture and the position pips.
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(Cards3dCopy.ROLODEX_HINT, CivilightTheme.type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                val shownPips = n.coerceAtMost(MAX_PIPS)
                val first = (focus - shownPips / 2).coerceIn(0, max(0, n - shownPips))
                for (k in first until first + shownPips) {
                    Box(Modifier.width(if (k == focus) 18.dp else 6.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (k == focus) colors.acc else colors.edge))
                }
            }
        }
        ControlBar(env, vertical = true) { OpenButton(env, env.items.getOrNull(focus)) }
    }
}

/** At most this many position pips under the drum (a large cast shows the ones around the focus). */
private const val MAX_PIPS = 12
