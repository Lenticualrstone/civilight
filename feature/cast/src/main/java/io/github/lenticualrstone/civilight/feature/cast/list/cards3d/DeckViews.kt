package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ==================================================================== 부채꼴 덱 (cards-a U16CastDeck)

/** Board `.stage` 390 × 500, `perspective: 1000px` at 50 % · 46 %; the card centre at the stage centre. */
private const val FAN_H = 500f
private val FAN_SCENE = Scene(1000f, 195f, 230f, 195f, 250f)

/** Board `.stage.ring`: `perspective: 900px` at 50 % · 30 %, cards at `top: 56%`. */
private const val RING_H = 500f
private val RING_SCENE = Scene(900f, 195f, 150f, 195f, 250f)
private const val RING_PIVOT_Y = 280f

/** Board `.ring .rig`: the ring seen from above (`rotateX(−15deg)`). */
private const val RING_LOOK = -15f

/** Board `.pool`: 360 × 120 floor light, 150 px under the stage centre, shifted `tx × 14px`. */
private const val POOL_W = 360f
private const val POOL_H = 120f
private const val POOL_TOP = 400f

/** Board `.stk` 390 × 496, `perspective: 1100px` at 50 % · 30 %; card at (60, 44), pivot 50 % · 60 %. */
private const val PILE_H = 496f
private val PILE_SCENE = Scene(1100f, 195f, 148.8f, 195f, 248f)
private const val PILE_PIVOT_Y = 44f + CardPoses.PILE_H * CardPoses.PILE_PIVOT_Y

/** Reduced motion: a pile drag of this many board px still turns the top card (board `rm && |dx| > 60`). */
private const val PILE_RM_STEP = 60f

/**
 * Fan like a hand of cards (board `U16CastDeck`): drag or `이전 카드 · 다음 카드` brings a card to the centre;
 * tapping the centre card (or `뒤집기`) turns it in 3D to its back with the `한 줄 소개`, the fields and `열기`.
 * The ends clamp with a rubber band; the rig follows the tilt (or sways slowly while nothing tilts it).
 */
@Composable
internal fun FanDeckView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    var flippedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val focusItem = env.items.getOrNull(focus)
    val flipped = focusItem != null && flippedKey == focusItem.key
    LaunchedEffect(focusItem?.key) {
        if (flippedKey != null && flippedKey != focusItem?.key) flippedKey = null
    }
    val toggle = {
        if (focusItem is DeckItem.Character) flippedKey = if (flipped) null else focusItem.key
    }
    val projector = remember { Projector() }
    val pose = remember { CardPose() }
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    val shadow = remember(mat) { Shadows.deck(mat) }
    val tilt = env.tilt
    Column(Modifier.fillMaxSize()) {
        BoardStage(FAN_H, Modifier.weight(1f).fillMaxWidth()) { unit, bp ->
            ReducedFade(env.reduced, FanFrame(focus, flipped)) { frame ->
                // One turn per centre card: a new centre starts face-up instead of un-turning the last one's back.
                val flip = remember(frame.focus) { Animatable(if (frame.flipped) 1f else 0f) }
                LaunchedEffect(flip, frame.flipped) {
                    val t = if (frame.flipped) 1f else 0f
                    if (env.reduced) flip.snapTo(t) else flip.animateTo(t, spring(CivilightMotion.cardFlipDamping, CivilightMotion.cardFlipStiffness))
                }
                val pos: () -> Float = if (env.reduced) ({ frame.focus.toFloat() }) else ({ deck.pos })
                Box(
                    Modifier.matchParentSize().stageDrag(
                        listOf(deck, unit, env.reduced),
                        vertical = false,
                        onStart = { flippedKey = null },
                        onDrag = { d -> deck.dragBy(-d / (CardPoses.FAN_DRAG_PX * unit)) },
                        onEnd = { v -> deck.endDrag(-v / (CardPoses.FAN_DRAG_PX * unit)) },
                    ),
                ) {
                    for (i in CardPoses.window(frame.focus, env.items.size, loop = false)) {
                        val item = env.items[i]
                        val centre = i == frame.focus
                        key(item.key) {
                            PosedCard(
                                w = CardPoses.DECK_W, h = CardPoses.DECK_H, pivotX = 195f, pivotY = 250f,
                                unit = unit, bp = bp, radius = 18f, shadow = shadow, camera = FAN_SCENE.perspective,
                                place = { xf ->
                                    val f = if (centre) flip.value else 0f
                                    val p = CardPoses.fan(i - pos(), f, pose)
                                    if (p == null) {
                                        xf.alpha = 0f
                                    } else {
                                        projector.solve(
                                            xf, FAN_SCENE, -tilt.y * 5f, tilt.x * 8f, 195f, 250f,
                                            p.x, p.y, p.z, p.rotX, p.rotY, p.rotZ, TurnOrder.ZY,
                                            flipY = if (env.reduced) 0f else 180f * f, scale = p.scale,
                                        )
                                        xf.alpha = 1f
                                        xf.dim = p.dim
                                    }
                                },
                                onTap = { if (centre) (if (item is DeckItem.Lock) env.actions.unlock() else toggle()) else deck.goTo(i) },
                                clickLabel = itemLabel(item),
                                onLongPress = env.longPress(item),
                                longPressLabel = LongPressGate.label(item),
                            ) { back ->
                                val showBack = item is DeckItem.Character && centre && (back || (env.reduced && frame.flipped))
                                when {
                                    item is DeckItem.Lock -> LockCardFace(item.row, bp, compact = false, onUnlock = env.actions.unlock)
                                    showBack && item is DeckItem.Character -> {
                                        val b = remember(item.card.key, env.input.backOf) { env.input.backOf(item.card.key) }
                                        DeckBackFace(item.card, b, bp, onOpen = { env.actions.open(item.card.key) })
                                    }
                                    item is DeckItem.Character -> PortraitFace(
                                        item.card, env.input.imageOf(item.card.key), item.card.key in env.input.hiddenKeys, PortraitStyle.DECK, bp,
                                        sheen = { Offset((tilt.x * 30f - (i - pos()) * 22f) / 100f, 0f) },
                                        tilt = { Offset(tilt.x, tilt.y) },
                                        parallax = if (centre) ({ tilt.x }) else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Readout(env, focus) { deck.pos.coerceIn(-0.3f, env.items.size - 0.7f) }
        ControlBar(env) {
            when (focusItem) {
                is DeckItem.Character -> {
                    CivilightIconButton(IconButtonSpec.CARD_FLIP, toggle, on = flipped)
                    OpenButton(env, focusItem)
                }
                else -> OpenButton(env, focusItem)
            }
        }
    }
}

private data class FanFrame(val focus: Int, val flipped: Boolean)

// ==================================================================== 회전 목마 (cards-a U16CastRing)

/**
 * Cards on a ring turning in depth (board `U16CastRing`), seen a little from above, with a floor light under
 * the front card. The front card is the largest; a card turned away shows its patterned back. Drag or the
 * buttons turn it, it loops, and tapping the front card (or `열기`) opens it. A large cast keeps a ring of
 * at most twelve places.
 */
@Composable
internal fun CarouselView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    val slots = CardPoses.ringSlots(n)
    val projector = remember { Projector() }
    val pose = remember { CardPose() }
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    val shadow = remember(mat) { Shadows.deck(mat) }
    val tilt = env.tilt
    Column(Modifier.fillMaxSize()) {
        BoardStage(RING_H, Modifier.weight(1f).fillMaxWidth()) { unit, bp ->
            // Board `.pool`: an elliptic floor light under the ring.
            Box(
                Modifier.zIndex(BEHIND).offset(bp.dp(195f - POOL_W / 2), bp.dp(POOL_TOP)).size(bp.dp(POOL_W), bp.dp(POOL_H))
                    .graphicsLayer { translationX = tilt.x * 14f * unit }
                    .drawWithCache {
                        val r = size.width / 2
                        val c = Offset(size.width / 2, size.height / 2)
                        val brush = Brush.radialGradient(listOf(mat.pool, Color.Transparent), center = c, radius = r)
                        onDrawBehind { scale(1f, size.height / size.width, c) { drawCircle(brush, r, c) } }
                    },
            )
            ReducedFade(env.reduced, focus) { f ->
                val pos: () -> Float = if (env.reduced) ({ f.toFloat() }) else ({ deck.pos })
                Box(
                    Modifier.matchParentSize().stageDrag(
                        listOf(deck, unit, env.reduced),
                        vertical = false,
                        onDrag = { d -> deck.dragBy(-d / (CardPoses.RING_DRAG_PX * unit)) },
                        onEnd = { v -> deck.endDrag(-v / (CardPoses.RING_DRAG_PX * unit)) },
                    ),
                ) {
                    for (i in CardPoses.ringWindow(f, n)) {
                        val item = env.items[i]
                        val front = i == f
                        key(item.key) {
                            PosedCard(
                                w = CardPoses.RING_W, h = CardPoses.RING_H, pivotX = 195f, pivotY = RING_PIVOT_Y,
                                unit = unit, bp = bp, radius = 16f, shadow = shadow, camera = RING_SCENE.perspective,
                                place = { xf ->
                                    val p = CardPoses.ring(CardPoses.offset(i, pos(), n, true), slots, pose)
                                    projector.solve(
                                        xf, RING_SCENE, RING_LOOK - tilt.y * 5f, tilt.x * 9f, 195f, RING_PIVOT_Y,
                                        p.x, p.y, p.z, p.rotX, p.rotY, p.rotZ, TurnOrder.XYZ, scale = p.scale,
                                    )
                                    xf.alpha = 1f
                                    xf.dim = p.dim
                                },
                                onTap = { if (front) env.activate(item) else deck.goTo(i) },
                                clickLabel = itemLabel(item),
                                onLongPress = env.longPress(item),
                                longPressLabel = LongPressGate.label(item),
                            ) { back ->
                                when {
                                    back -> PatternBackFace(bp, 16f)
                                    item is DeckItem.Lock -> LockCardFace(item.row, bp, compact = true, onUnlock = env.actions.unlock)
                                    item is DeckItem.Character -> PortraitFace(
                                        item.card, env.input.imageOf(item.card.key), item.card.key in env.input.hiddenKeys, PortraitStyle.RING, bp,
                                        sheen = { Offset((tilt.x * 30f - CardPoses.offset(i, pos(), n, true) * 22f) / 100f, 0f) },
                                        tilt = { Offset(tilt.x, tilt.y) },
                                        parallax = if (front) ({ tilt.x }) else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Readout(env, focus) { ((deck.pos % n) + n) % n }
        ControlBar(env) { OpenButton(env, env.items.getOrNull(focus)) }
    }
}

// ==================================================================== 겹친 더미 (cards-b 겹친 더미 넘기기)

/**
 * A pile (board `겹친 더미 넘기기`): drag the top card and it follows the finger while the next one rises;
 * let go past 110 px (or flicked) and it is thrown in an arc and slides back in under the pile. `다음 카드`
 * throws it to the left, `이전 카드` brings the bottom card back on top. Tapping the top card (or `열기`)
 * opens it. Six cards of the pile are drawn.
 */
@Composable
internal fun PileView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    val projector = remember { Projector() }
    val pose = remember { CardPose() }
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    val shadow = remember(mat) { Shadows.pile(mat) }
    val tilt = env.tilt
    val scope = rememberCoroutineScope()
    // The top card under the finger (board px), and which way the last throw went (−1 left, the buttons).
    val dragX = remember { mutableFloatStateOf(0f) }
    // The card the finger took (the top card when the drag began): only it carries the drag offset.
    val dragIndex = remember { mutableIntStateOf(-1) }
    val dir = remember { mutableFloatStateOf(-1f) }
    var dragging by remember { mutableStateOf(false) }
    var backJob by remember { mutableStateOf<Job?>(null) }
    val focusItem = env.items.getOrNull(focus)
    Column(Modifier.fillMaxSize()) {
        BoardStage(PILE_H, Modifier.weight(1f).fillMaxWidth()) { unit, bp ->
            ReducedFade(env.reduced, focus) { f ->
                val pos: () -> Float = if (env.reduced) ({ f.toFloat() }) else ({ deck.pos })
                Box(
                    Modifier.matchParentSize().stageDrag(
                        listOf(deck, unit, env.reduced),
                        vertical = false,
                        onStart = {
                            backJob?.cancel()
                            dragIndex.intValue = deck.index
                            dragging = true
                        },
                        onDrag = { d -> dragX.floatValue += d / unit },
                        onEnd = { v ->
                            dragging = false
                            val dx = dragX.floatValue
                            val vx = v / unit / 1000f
                            val throws = if (env.reduced) abs(dx) > PILE_RM_STEP else CardPoses.pileThrows(dx, vx)
                            if (throws) {
                                dir.floatValue = sign(if (dx != 0f) dx else vx)
                                deck.step(1)
                            }
                            val fromX = dragX.floatValue
                            backJob = scope.launch {
                                val s = if (throws) {
                                    spring<Float>(CivilightMotion.cardPileDamping, CivilightMotion.cardPileStiffness)
                                } else {
                                    spring(CivilightMotion.cardPileBackDamping, CivilightMotion.cardPileBackStiffness)
                                }
                                animate(fromX, 0f, if (throws) 0f else v / unit, s) { value, _ -> dragX.floatValue = value }
                            }
                        },
                    ),
                ) {
                    for (i in stackWindow(f, n)) {
                        val item = env.items[i]
                        val top = i == f
                        key(item.key) {
                            PosedCard(
                                w = CardPoses.PILE_W, h = CardPoses.PILE_H, pivotX = 195f, pivotY = PILE_PIVOT_Y,
                                unit = unit, bp = bp, radius = 22f, shadow = shadow, camera = PILE_SCENE.perspective,
                                originY = CardPoses.PILE_PIVOT_Y,
                                place = { xf ->
                                    var o = stackOffset(i, pos(), n)
                                    // The card under a dragged top card rises halfway (board `p`).
                                    if (dragging && !env.reduced && o in 0.5f..1.5f) o -= CardPoses.pileRise(dragX.floatValue)
                                    val p = CardPoses.stack(o, dir.floatValue, n, pose)
                                    if (p == null) {
                                        xf.alpha = 0f
                                    } else {
                                        // The finger's card: its drag offset fades out over the throw.
                                        val held = if (i == dragIndex.intValue && o >= -0.5f && o <= 0.5f && !env.reduced) (1f - abs(o) / 0.5f).coerceIn(0f, 1f) else 0f
                                        val dx = dragX.floatValue * held
                                        projector.solve(
                                            xf, PILE_SCENE, -tilt.y * 5f, tilt.x * 7f, 195f, PILE_PIVOT_Y,
                                            p.x + dx, p.y, p.z + (if (dx != 0f) 24f * held else 0f),
                                            p.rotX, p.rotY + dx * 0.06f, p.rotZ + dx * 0.07f, TurnOrder.XYZ,
                                        )
                                        xf.alpha = p.alpha
                                        xf.dim = p.dim
                                    }
                                },
                                onTap = if (top) ({ env.activate(item) }) else null,
                                clickLabel = itemLabel(item),
                                onLongPress = env.longPress(item),
                                longPressLabel = LongPressGate.label(item),
                            ) { _ ->
                                when (item) {
                                    is DeckItem.Lock -> LockCardFace(item.row, bp, compact = false, onUnlock = env.actions.unlock)
                                    is DeckItem.Character -> PileFace(
                                        item.card, env.input.imageOf(item.card.key), item.card.key in env.input.hiddenKeys, i + 1, bp,
                                        sheen = if (top) ({ Offset(tilt.x * 0.26f, tilt.y * 0.14f) }) else ({ Offset(-0.12f, 0f) }),
                                        glyphShift = if (top) ({ Offset(tilt.x * -18f, tilt.y * -12f) }) else ({ Offset.Zero }),
                                        tilt = { Offset(tilt.x, tilt.y) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Board `.cnt`: `01 / 06`, the name, and the gesture hint.
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val type = CivilightTheme.type
            if (focusItem is DeckItem.Character) {
                CivilightText("${Cards3dCopy.twoDigits(focus + 1)} / ${Cards3dCopy.twoDigits(env.cardCount)}", type.number, color = colors.ink3, maxLines = 1)
                CivilightText(focusItem.card.title, type.body.merge(TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)), color = colors.ink, maxLines = 1)
            } else if (focusItem is DeckItem.Lock) {
                CivilightText(focusItem.row.title, type.body.merge(TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)), color = colors.ink, maxLines = 1)
            }
            Box(Modifier.weight(1f))
            CivilightText(Cards3dCopy.PILE_HINT, type.small, color = colors.ink3, maxLines = 1)
        }
        ControlBar(env, onStep = { d ->
            if (d > 0) dir.floatValue = -1f
            deck.step(d)
        }) { OpenButton(env, focusItem) }
    }
}

/** Pile offset of card [i]: 0 = top, up to n − 1 at the bottom; the card just thrown is between −1 and 0. */
internal fun stackOffset(i: Int, pos: Float, n: Int): Float {
    if (n <= 0) return 0f
    val d = ((i - pos) % n + n) % n
    return if (d > n - 1) d - n else d
}

/** The thrown card and the six of the pile. */
internal fun stackWindow(focus: Int, n: Int): List<Int> =
    if (n <= 0) emptyList() else (-1..5).map { Math.floorMod(focus + it, n) }.distinct()
