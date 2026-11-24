package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import io.github.lenticualrstone.civilight.designsystem.foundation.horizontalSwipe
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.card.rememberTilt
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconOnlyButton
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.cast.list.CastCardViewActions
import io.github.lenticualrstone.civilight.feature.cast.list.CastCardViewInput
import io.github.lenticualrstone.civilight.model.app.CardStyle
import kotlin.math.roundToInt

/**
 * The tilt a stage turns with, each axis −1..1 (board `tx`, `ty`): the device while `viewTilt` follows it,
 * else (the switch on but no sensor) the boards' slow idle sway, else still. Read [x] and [y] only in layer and draw
 * blocks, so a sensor event or a sway frame redraws and never recomposes.
 */
@Stable
internal class StageTilt(private val device: DeviceTilt, private val follows: Boolean) {
    internal val swayX = mutableFloatStateOf(0f)
    internal val swayY = mutableFloatStateOf(0f)

    /**
     * Lean right as the card views were built (the old feature-local listener read `getOrientation` roll,
     * `atan2(−R[6], R[8])`); the DS-74 source reads `asin(R[6])`, the opposite sign. The y axis has the same
     * sign in both.
     */
    val x: Float get() = if (follows) -device.x else swayX.floatValue
    val y: Float get() = if (follows) device.y else swayY.floatValue
}

@Composable
private fun rememberStageTilt(device: DeviceTilt, follows: Boolean, sway: Boolean): StageTilt {
    val tilt = remember(device, follows) { StageTilt(device, follows) }
    if (!follows && sway) {
        LaunchedEffect(tilt) {
            while (true) {
                withFrameMillis { ms ->
                    tilt.swayX.floatValue = CardPoses.swayX(ms, CivilightMotion.cardSwayPeriodXMs, CivilightMotion.cardSwayX)
                    tilt.swayY.floatValue = CardPoses.swayX(ms, CivilightMotion.cardSwayPeriodYMs, CivilightMotion.cardSwayY)
                }
            }
        }
    }
    return tilt
}

/** The rotation-vector sensor [rememberTilt] listens to is there. */
private fun hasTiltSensor(context: Context): Boolean {
    val m = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
    return m.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null || m.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
}

/** What every view reads: the items, the focus, the callbacks and the motion gates. */
internal class CardEnv(
    val items: List<DeckItem>,
    val deck: Deck,
    val input: CastCardViewInput,
    val actions: CastCardViewActions,
    val tilt: StageTilt,
    val reduced: Boolean,
) {
    val cardCount: Int get() = input.cards.size

    /** A long press on any card: opens that character, or (the lock card) asks for the fingerprint, as a tap does. */
    fun longPress(item: DeckItem): () -> Unit = { activate(item) }

    fun activate(item: DeckItem) {
        when (item) {
            is DeckItem.Lock -> actions.unlock()
            is DeckItem.Character -> actions.open(item.card.key)
        }
    }
}

/**
 * One 3D card view (U-16, CST-67) of [style], as the cards boards draw it (`scr_u16_cards_a/b/c.py`): their
 * card sizes, perspectives, rigs, faces and springs. Every view shares the [Deck] focus, the lock card at the
 * end, the visible `이전 카드 · 다음 카드` buttons for every gesture, and the reduce-motion rule: no 3D motion,
 * every change a 120 ms fade.
 */
@Composable
internal fun CardStage(style: CardStyle, input: CastCardViewInput, actions: CastCardViewActions) {
    val motion = CivilightTheme.motion
    val reduced = motion.reduced
    val items = remember(input.cards, input.lock) { DeckItem.of(input.cards, input.lock) }
    val loop = style == CardStyle.CAROUSEL || style == CardStyle.STACK
    val (stiffness, damping) = when (style) {
        CardStyle.STACK -> CivilightMotion.cardPileStiffness to CivilightMotion.cardPileDamping
        CardStyle.ROLODEX -> CivilightMotion.rolodexStiffness to CivilightMotion.rolodexDamping
        else -> CivilightMotion.cardDeckStiffness to CivilightMotion.cardDeckDamping
    }
    val deck = rememberDeck(items, loop, reduced, input.focusKey, stiffness, damping)
    // `기울이면 따라옴` on: the device leans the rig; on a device without the sensor the boards' idle sway stands in
    // for it (board: sway only while no gyro answers). Off, or motion reduced: the rig stays still.
    val wants = input.viewTilt && motion.loops
    val context = LocalContext.current
    val sensor = remember(context) { hasTiltSensor(context) }
    val follows = wants && sensor
    val device = rememberTilt(follows)
    val tilt = rememberStageTilt(device, follows, sway = wants && !sensor)
    val env = CardEnv(items, deck, input, actions, tilt, reduced)
    if (items.isEmpty()) return
    CompositionLocalProvider(LocalCardTilt provides (if (follows) device else null)) {
        when (style) {
            CardStyle.FAN_DECK -> FanDeckView(env)
            CardStyle.CAROUSEL -> CarouselView(env)
            CardStyle.STACK -> PileView(env)
            CardStyle.TAROT -> TarotView(env)
            CardStyle.TILTED_WALL -> TiltedWallView(env)
            CardStyle.ROLODEX -> RolodexView(env)
            CardStyle.LIST -> Unit
        }
    }
}

// ==================================================================== shared pieces

/** Under reduced motion the stage is drawn per [frame] and crossfades between frames (120 ms); otherwise live. */
@Composable
internal fun <T> ReducedFade(reduced: Boolean, frame: T, content: @Composable (T) -> Unit) {
    if (reduced) {
        Crossfade(frame, Modifier.fillMaxSize(), animationSpec = CivilightTheme.motion.spec(CivilightMotion.standard), label = "cards3d") { content(it) }
    } else {
        content(frame)
    }
}

/**
 * A board stage of 390 × [boardH] board px, drawn 1:1 (one board px is one dp, as the boards are 390 wide):
 * the cards keep the boards' sizes (부채꼴 220 × 308, 목마 168 × 236, 더미 270 × 384 …) on every screen, and
 * only the stage area around them grows; never scaled by the pane's width or a fit factor. The board is
 * centred in the space; a space shorter than the board lets the board's empty margins overflow (the cast
 * list scrolls when the pane is shorter than [CardPoses.STAGE_MIN_H]).
 */
@Composable
internal fun BoardStage(boardH: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.(unit: Float, bp: Bp) -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current.density
        val unit = CardPoses.boardUnit(density)
        val bp = remember(unit, density) { Bp(unit / density) }
        Box(Modifier.requiredSize(bp.dp(CardPoses.STAGE_W), bp.dp(boardH))) { content(unit, bp) }
    }
}

/**
 * A horizontal or vertical drag of a stage, reporting px moved and, on release, the velocity in px/s. [key]
 * restarts the gesture when what the callbacks capture changes (the deck, the stage scale, reduced motion).
 *
 * 0.2.3 gesture rules (core `GestureRules`, shared `horizontalSwipe`): a horizontal stage stays undecided inside
 * the touch slop (a card's tap and long press still work), and lets a drag that reads vertical go unconsumed, so the
 * list column around a short stage scrolls instead of the deck jumping on a diagonal flick.
 */
@Composable
internal fun Modifier.stageDrag(
    key: Any,
    vertical: Boolean,
    onStart: () -> Unit = {},
    onDrag: (Float) -> Unit,
    onEnd: (velocity: Float) -> Unit,
): Modifier {
    if (!vertical) {
        return horizontalSwipe(onStart = onStart, onDelta = onDrag, onStop = onEnd)
    }
    return pointerInput(key, vertical) {
        val tracker = VelocityTracker()
        val start: (androidx.compose.ui.geometry.Offset) -> Unit = {
            tracker.resetTracking()
            onStart()
        }
        val end = { onEnd(tracker.calculateVelocity().y) }
        detectVerticalDragGestures(onDragStart = start, onDragEnd = end, onDragCancel = { onEnd(0f) }) { change, d ->
            change.consume()
            tracker.addPosition(change.uptimeMillis, change.position)
            onDrag(d)
        }
    }
}

/** The click label of a card: its title, or the lock card's `숨긴 인물 n`. */
internal fun itemLabel(item: DeckItem): String = when (item) {
    is DeckItem.Character -> item.card.title
    is DeckItem.Lock -> item.row.title
}

/** A card shadow as one board `box-shadow` list. */
internal object Shadows {
    /** cards-a `--cshadow`: `0 22px 44px` + `0 2px 6px`. */
    fun deck(m: CardMaterial) = listOf(ShadowLayer(0f, 22f, 44f, 0f, m.shadowWide), ShadowLayer(0f, 2f, 6f, 0f, m.shadowTight))

    /** cards-b `.sk .face`: `0 28px 46px −20px`. */
    fun pile(m: CardMaterial) = listOf(ShadowLayer(0f, 28f, 46f, -20f, m.shadowB))

    /** cards-b `.tc .face`: `0 10px 18px −8px`. */
    fun tarot(m: CardMaterial) = listOf(ShadowLayer(0f, 10f, 18f, -8f, m.shadowB))

    /** cards-c `.sl .fa`: `0 14px 26px`; pulled `.fc .fa`: `0 30px 60px`; `.rc .pp`: `0 18px 34px`. */
    fun wall(m: CardMaterial) = listOf(ShadowLayer(0f, 14f, 26f, 0f, m.shadowC))
    fun pulled(m: CardMaterial) = listOf(ShadowLayer(0f, 30f, 60f, 0f, m.shadowC))
    fun index(m: CardMaterial) = listOf(ShadowLayer(0f, 18f, 34f, 0f, m.shadowC))
}

/**
 * A card drawn at the pose [place] writes (board px, read in the layer and draw blocks, so a moving focus
 * redraws without recomposing). It rests with its pivot at ([pivotX], [pivotY]) of the stage ([originX],
 * [originY]: the pivot as a fraction of the card). The draw order follows the solved depth; [content] gets
 * whether the card's back faces the viewer (drawn mirrored back). [onLongPress] (held still, see [cardLongPress])
 * opens the card directly, read by TalkBack as [longPressLabel]. While the card is faded out it can be neither
 * tapped nor read by TalkBack; it recomposes only when such a flag flips.
 */
@Composable
internal fun BoxScope.PosedCard(
    w: Float,
    h: Float,
    pivotX: Float,
    pivotY: Float,
    unit: Float,
    bp: Bp,
    radius: Float,
    shadow: List<ShadowLayer>,
    camera: Float,
    place: (CardXform) -> Unit,
    onTap: (() -> Unit)?,
    clickLabel: String,
    onLongPress: (() -> Unit)? = null,
    longPressLabel: String = "",
    originX: Float = 0.5f,
    originY: Float = 0.5f,
    dimColor: Color = CivilightTheme.colors.bg,
    orderBias: Float = 0f,
    content: @Composable (back: Boolean) -> Unit,
) {
    val placeNow by rememberUpdatedState(place)
    // One pose each for the layer, the draw block and the flags: no allocation per frame.
    val xt = remember { CardXform() }
    val xf = remember { CardXform() }
    val xd = remember { CardXform() }
    val xs = remember { CardXform() }
    // Draw order in 10 px depth steps: a moving card recomposes (cheaply) only when it passes another.
    val order by remember { derivedStateOf { placeNow(xs); (xs.depth / ORDER_STEP).roundToInt() } }
    val shown by remember { derivedStateOf { placeNow(xs); xs.alpha > 0.02f } }
    val back by remember { derivedStateOf { placeNow(xs); xs.backFacing } }
    val shape = RoundedCornerShape(bp.dp(radius))
    val cam = Projector.cameraDistance(camera, unit)
    val left = ((pivotX - w * originX) * unit).roundToInt()
    val top = ((pivotY - h * originY) * unit).roundToInt()
    val origin = TransformOrigin(originX, originY)
    // Kept across recompositions, so the shadow's blurred bitmap is built once per card size.
    val shadowMod = rememberCardShadow(shadow, radius, unit)
    Box(
        Modifier
            .zIndex(order + orderBias)
            .offset { IntOffset(left, top) }
            .size(bp.dp(w), bp.dp(h))
            // The move on a layer of its own, the turn on the next one. Compose hit-tests a layer with its own
            // matrix (`Matrix.resetToPivotedTransform`), which applies the translation before the turns: on a card
            // turned past 90° (the fan's turned centre card, a face-up 타로 card: rotationX ≈ 180) the translation
            // comes out mirrored, so taps landed tens of dp away from what was drawn and the back's `열기` could
            // not be hit. A pure translation layer maps the same way as it draws.
            .graphicsLayer {
                placeNow(xt)
                translationX = xt.translationX * unit
                translationY = xt.translationY * unit
            }
            .graphicsLayer {
                placeNow(xf)
                if (xf.alpha <= 0f) {
                    alpha = 0f
                    return@graphicsLayer
                }
                transformOrigin = origin
                scaleX = xf.scale
                scaleY = xf.scale
                rotationX = xf.rotationX
                rotationY = xf.rotationY
                rotationZ = xf.rotationZ
                alpha = xf.alpha.coerceAtMost(1f)
                cameraDistance = cam
            }
            .then(shadowMod)
            .then(if (shown) Modifier else Modifier.clearAndSetSemantics {})
            .then(
                if (onTap != null) {
                    Modifier.pressable(onClick = onTap, enabled = shown, scaleOnPress = false, onClickLabel = clickLabel, shape = shape)
                } else {
                    Modifier
                },
            )
            .then(if (onLongPress != null) Modifier.cardLongPress(shown, longPressLabel, onLongPress) else Modifier)
            .clip(shape)
            .drawWithContent {
                drawContent()
                placeNow(xd)
                val d = xd.dim
                if (d > 0.01f) drawRect(dimColor, alpha = d.coerceIn(0f, 1f))
            },
    ) {
        if (back) {
            Box(Modifier.graphicsLayer { scaleX = -1f }) { content(true) }
        } else {
            content(false)
        }
    }
}

/**
 * Board `.ro`: a progress track with the focus as its thumb (moving with the deck, read in the layer) and
 * `n · N` (cards-a).
 */
@Composable
internal fun Readout(env: CardEnv, index: Int, track: () -> Float) {
    val colors = CivilightTheme.colors
    val n = env.items.size.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(colors.line)) {
            Box(
                Modifier.fillMaxWidth(1f / n).height(3.dp).graphicsLayer { translationX = track() * size.width }.clip(RoundedCornerShape(2.dp)).background(colors.acc),
            )
        }
        val shown = if (env.items.getOrNull(index) is DeckItem.Character) "${index + 1} · ${env.cardCount}" else env.cardCount.toString()
        CivilightText(shown, CivilightTheme.type.number, color = colors.ink3, maxLines = 1, textAlign = TextAlign.End)
    }
}

/** `이전 카드` · the view's main buttons · `다음 카드` (every gesture has a visible button). */
@Composable
internal fun ControlBar(env: CardEnv, vertical: Boolean = false, onStep: ((Int) -> Unit)? = null, canStep: ((Int) -> Boolean)? = null, middle: @Composable RowScope.() -> Unit) {
    val deck = env.deck
    val step: (Int) -> Unit = onStep ?: { deck.step(it) }
    val can: (Int) -> Boolean = canStep ?: { deck.canStep(it) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (vertical) {
            CivilightIconButton(IconButtonSpec.ROLODEX_PREV, { step(-1) }, enabled = can(-1))
        } else {
            IconOnlyButton(IconId.Prev, IconButtonCopy.PREV_CARD, { step(-1) }, enabled = can(-1))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) { middle() }
        if (vertical) {
            CivilightIconButton(IconButtonSpec.ROLODEX_NEXT, { step(1) }, enabled = can(1))
        } else {
            IconOnlyButton(IconId.Next, IconButtonCopy.NEXT_CARD, { step(1) }, enabled = can(1))
        }
    }
}

/** `열기` for a character, `지문으로 보기` for the lock card. */
@Composable
internal fun OpenButton(env: CardEnv, item: DeckItem?) {
    when (item) {
        is DeckItem.Lock -> CivilightIconButton(IconButtonSpec.SHOW_WITH_FINGERPRINT, env.actions.unlock)
        is DeckItem.Character -> CivilightIconButton(IconButtonSpec.CARD_OPEN, { env.actions.open(item.card.key) })
        null -> Unit
    }
}

/** Depth step of the cards' draw order, board px. */
private const val ORDER_STEP = 10f

/** Draw order of what lies under every card of a stage (the table, the floor light, the wall plate). */
internal const val BEHIND = -10_000f

/** Px of [dp] for a density. */
internal fun Dp.px(density: Float): Float = value * density
