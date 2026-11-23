package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.card.rememberTilt
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/** Camera distance of a card layer, in card-independent density units (a calm, not fish-eye, 3D turn). */
private const val CAMERA = 14f

/** Tilt turn of a whole stage, degrees at full tilt (parallax). */
private const val STAGE_TILT_Y = 6f
private const val STAGE_TILT_X = 4f

/** What every view reads: the items, the focus, the callbacks and the motion gates. */
internal class CardEnv(
    val items: List<DeckItem>,
    val deck: Deck,
    val input: CastCardViewInput,
    val actions: CastCardViewActions,
    val tilt: DeviceTilt,
    /** Tilt follows the device (`Tilt.follows`: `viewTilt` on, motion not reduced). */
    val tiltOn: Boolean,
    val reduced: Boolean,
) {
    val cardCount: Int get() = input.cards.size

    /**
     * Lean right as the card views were built (the old feature-local listener read `getOrientation` roll,
     * `atan2(−R[6], R[8])`); the DS-74 source reads `asin(R[6])`, the opposite sign. Read in layer and draw
     * blocks only. The y axis has the same sign in both.
     */
    val tiltX: Float get() = -tilt.x

    fun activate(item: DeckItem) {
        when (item) {
            is DeckItem.Lock -> actions.unlock()
            is DeckItem.Character -> actions.open(item.card.key)
        }
    }
}

/**
 * One 3D card view (U-16, CST-67) of [style]. Every view shares the [Deck] focus, the window of about seven
 * drawn cards, the lock card at the end, the `이전 카드 · 다음 카드` buttons and the reduce-motion rule: no 3D
 * motion, every change a 120 ms fade.
 */
@Composable
internal fun CardStage(style: CardStyle, input: CastCardViewInput, actions: CastCardViewActions) {
    val motion = CivilightTheme.motion
    val reduced = motion.reduced
    val items = remember(input.cards, input.lock) { DeckItem.of(input.cards, input.lock) }
    val loop = style == CardStyle.CAROUSEL || style == CardStyle.STACK
    val deck = rememberDeck(items, loop, reduced, input.focusKey)
    val tiltOn = input.viewTilt && motion.loops
    val tilt = rememberTilt(tiltOn)
    val env = CardEnv(items, deck, input, actions, tilt, tiltOn, reduced)
    if (items.isEmpty()) return
    when (style) {
        CardStyle.FAN_DECK -> FanDeckView(env)
        CardStyle.CAROUSEL -> CarouselView(env)
        CardStyle.STACK -> StackView(env)
        CardStyle.TAROT -> TarotView(env)
        CardStyle.TILTED_WALL -> TiltedWallView(env)
        CardStyle.ROLODEX -> RolodexView(env)
        CardStyle.LIST -> Unit
    }
}

// ==================================================================== shared pieces

/** Under reduced motion the stage is drawn per [frame] and crossfades between frames (120 ms); otherwise live. */
@Composable
private fun <T> ReducedFade(reduced: Boolean, frame: T, content: @Composable (T) -> Unit) {
    if (reduced) {
        Crossfade(frame, animationSpec = CivilightTheme.motion.spec(CivilightMotion.standard), label = "cards3d") { content(it) }
    } else {
        content(frame)
    }
}

/** Board px → px for a stage of this size ([boardH]: the board height the view needs). */
private fun BoxWithConstraintsScope.unitPx(density: Float, boardH: Float): Float =
    min(constraints.maxWidth / CardPoses.STAGE_W, constraints.maxHeight / boardH).coerceAtLeast(0.1f * density)

private fun Modifier.cardDrag(deck: Deck, vertical: Boolean, pxPerCard: Float, sign: Float = 1f): Modifier =
    pointerInput(deck, vertical, pxPerCard, sign) {
        var last = 0f
        if (vertical) {
            detectVerticalDragGestures(onDragEnd = { deck.endDrag(last) }, onDragCancel = { deck.endDrag(0f) }) { change, d ->
                change.consume()
                last = -d * sign
                deck.dragBy(-d * sign / pxPerCard)
            }
        } else {
            detectHorizontalDragGestures(onDragEnd = { deck.endDrag(last) }, onDragCancel = { deck.endDrag(0f) }) { change, d ->
                change.consume()
                last = -d * sign
                deck.dragBy(-d * sign / pxPerCard)
            }
        }
    }

/** The whole stage turns a little with the tilt (parallax); nothing moves while tilt is off. */
private fun Modifier.stageTilt(env: CardEnv): Modifier = if (!env.tiltOn) this else graphicsLayer {
    rotationY = env.tiltX * STAGE_TILT_Y
    rotationX = -env.tilt.y * STAGE_TILT_X
    cameraDistance = CAMERA * 2 * density
}

/** The click label of a card: its title, or the lock card's `숨긴 인물 n`. */
private fun itemLabel(item: DeckItem): String = when (item) {
    is DeckItem.Character -> item.card.title
    is DeckItem.Lock -> item.row.title
}

/**
 * A card drawn at [pose] (read in the layer, so a moving focus redraws without recomposing). [turnY] adds a
 * 3D turn (a flip) without a new pose per frame. While [shown] is false (behind the ring, past the fan, faded
 * out) the card can be neither tapped nor focused by TalkBack; it recomposes only when that flips.
 */
@Composable
private fun PosedCard(
    env: CardEnv,
    item: DeckItem,
    order: Float,
    width: Dp,
    height: Dp,
    unitPx: Float,
    pose: () -> CardPose?,
    onTap: () -> Unit,
    sheen: Boolean = false,
    shown: () -> Boolean = { true },
    turnY: (() -> Float)? = null,
    clickLabel: String = itemLabel(item),
    content: @Composable () -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(CARD_RADIUS)
    val shownNow by rememberUpdatedState(shown)
    val visible by remember { derivedStateOf { shownNow() } }
    Box(
        Modifier
            .zIndex(order)
            .size(width, height)
            .graphicsLayer {
                val p = pose()
                if (p == null) {
                    alpha = 0f
                    return@graphicsLayer
                }
                translationX = p.x * unitPx
                translationY = p.y * unitPx
                rotationX = p.rotX
                rotationY = p.rotY + (turnY?.invoke() ?: 0f)
                rotationZ = p.rotZ
                val s = p.projected
                scaleX = s
                scaleY = s
                alpha = p.alpha
                cameraDistance = CAMERA * density
            }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {})
            .pressable(onClick = onTap, enabled = visible, scaleOnPress = false, onClickLabel = clickLabel, shape = shape)
            .clip(shape)
            .cardLight(
                shade = colors.shade,
                band = colors.hl,
                dim = { pose()?.dim ?: 0f },
                sheen = if (sheen && env.tiltOn) ({ CardPoses.sheen(env.tiltX) }) else null,
            ),
    ) { content() }
}

/** The front of [item] at a card size: a character card or the lock card. */
@Composable
private fun ItemFront(env: CardEnv, item: DeckItem, width: Dp, height: Dp, parallax: Boolean = false) {
    when (item) {
        is DeckItem.Character -> CardFront(
            item.card,
            env.input.imageOf(item.card.key),
            item.card.key in env.input.hiddenKeys,
            width,
            height,
            parallax = if (parallax && env.tiltOn) ({ env.tiltX }) else null,
        )
        is DeckItem.Lock -> LockCardFace(item.row, width, height, env.actions.unlock)
    }
}

/** `03 / 11` over the buttons while a character card has the focus. */
@Composable
private fun Counter(env: CardEnv, index: Int) {
    val shown = if (env.items.getOrNull(index) is DeckItem.Character) "${index + 1} / ${env.cardCount}" else ""
    CivilightText(shown, CivilightTheme.type.number, Modifier.fillMaxWidth().padding(top = 4.dp), color = CivilightTheme.colors.ink3, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

/** `이전 카드` · the view's main buttons · `다음 카드` (every gesture has a visible button). */
@Composable
private fun ControlBar(env: CardEnv, vertical: Boolean = false, onStep: ((Int) -> Unit)? = null, middle: @Composable RowScope.() -> Unit) {
    val deck = env.deck
    val step: (Int) -> Unit = onStep ?: { deck.step(it) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (vertical) {
            CivilightIconButton(IconButtonSpec.ROLODEX_PREV, { step(-1) }, enabled = deck.canStep(-1))
        } else {
            IconOnlyButton(IconId.Prev, IconButtonCopy.PREV_CARD, { step(-1) }, enabled = deck.canStep(-1))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) { middle() }
        if (vertical) {
            CivilightIconButton(IconButtonSpec.ROLODEX_NEXT, { step(1) }, enabled = deck.canStep(1))
        } else {
            IconOnlyButton(IconId.Next, IconButtonCopy.NEXT_CARD, { step(1) }, enabled = deck.canStep(1))
        }
    }
}

/** `열기` for a character, `지문으로 보기` for the lock card. */
@Composable
private fun OpenButton(env: CardEnv, item: DeckItem?) {
    when (item) {
        is DeckItem.Lock -> CivilightIconButton(IconButtonSpec.SHOW_WITH_FINGERPRINT, env.actions.unlock)
        is DeckItem.Character -> CivilightIconButton(IconButtonSpec.CARD_OPEN, { env.actions.open(item.card.key) })
        null -> Unit
    }
}

// ==================================================================== 부채꼴 덱

/**
 * Fan like a hand of cards (board `U16CastDeck`): drag or `이전 카드 · 다음 카드` brings a card to the centre;
 * tapping the centre card (or `뒤집기`) turns it in 3D to its back with the `한 줄 소개`, two fields and `열기`.
 * The ends clamp.
 */
@Composable
private fun FanDeckView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    // The card turned to its back; moving to another card shows every front again.
    var flippedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val focusItem = env.items.getOrNull(focus)
    val flipped = focusItem != null && flippedKey == focusItem.key
    // Clear the turn once another card is at the centre (by key: a filter change can move the same card's index).
    LaunchedEffect(focusItem?.key) {
        if (flippedKey != null && flippedKey != focusItem?.key) flippedKey = null
    }
    val toggle = {
        if (focusItem is DeckItem.Character) flippedKey = if (flipped) null else focusItem.key
    }
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val density = LocalDensity.current.density
            val unit = unitPx(density, FAN_BOARD_H)
            val w = (220 * unit / density).dp
            val h = w * CARD_RATIO
            ReducedFade(env.reduced, FanFrame(focus, flipped)) { frame ->
                // One turn per centre card: a new centre starts face-up instead of un-turning the last one's back.
                val motion = CivilightTheme.motion
                val flip = remember(frame.focus) { Animatable(if (frame.flipped) 1f else 0f) }
                LaunchedEffect(flip, frame.flipped) { flip.animateTo(if (frame.flipped) 1f else 0f, motion.move(CivilightMotion.enter)) }
                val showBack by remember(flip) { derivedStateOf { flip.value >= 0.5f } }
                val pos: () -> Float = if (env.reduced) ({ frame.focus.toFloat() }) else ({ deck.pos })
                Box(
                    Modifier.fillMaxSize().cardDrag(deck, vertical = false, pxPerCard = FAN_DRAG * unit).stageTilt(env),
                    contentAlignment = Alignment.Center,
                ) {
                    for (i in CardPoses.window(frame.focus, env.items.size, loop = false)) {
                        val item = env.items[i]
                        val centre = i == frame.focus
                        key(item.key) {
                            PosedCard(
                                env, item,
                                order = CardPoses.drawOrder(CardPoses.fan((i - frame.focus).toFloat())),
                                width = w, height = h, unitPx = unit,
                                pose = { CardPoses.fan(i - pos(), if (centre) flip.value else 0f) },
                                onTap = { if (centre) (if (item is DeckItem.Lock) env.actions.unlock() else toggle()) else deck.goTo(i) },
                                sheen = centre,
                                shown = { CardPoses.fanShows(i - pos()) },
                                turnY = if (centre && !env.reduced) ({ 180f * flip.value }) else null,
                            ) {
                                if (centre && showBack && item is DeckItem.Character) {
                                    val back = remember(item.card.key, env.input.backOf) { env.input.backOf(item.card.key) }
                                    Box(Modifier.graphicsLayer { if (!env.reduced) rotationY = 180f }) {
                                        CardBackFace(back, w, h, onOpen = { env.actions.open(item.card.key) })
                                    }
                                } else {
                                    ItemFront(env, item, w, h, parallax = centre)
                                }
                            }
                        }
                    }
                }
            }
        }
        Counter(env, focus)
        ControlBar(env) {
            when (focusItem) {
                is DeckItem.Character -> {
                    CivilightIconButton(IconButtonSpec.CARD_FLIP, toggle, on = flipped)
                    if (flipped) OpenButton(env, focusItem)
                }
                else -> OpenButton(env, focusItem)
            }
        }
    }
}

private data class FanFrame(val focus: Int, val flipped: Boolean)

// ==================================================================== 회전 목마

/**
 * Cards on a ring turning in depth (board `U16CastRing`); the front card is the largest. Drag or the buttons
 * turn it, it loops, and tapping the front card (or `열기`) opens it. A large cast keeps a ring of at most
 * twelve places and draws only the seven nearest cards.
 */
@Composable
private fun CarouselView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    val slots = CardPoses.ringSlots(n)
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val density = LocalDensity.current.density
            val unit = unitPx(density, RING_BOARD_H)
            val w = (168 * unit / density).dp
            val h = w * CARD_RATIO
            ReducedFade(env.reduced, focus) { f ->
                val pos: () -> Float = if (env.reduced) ({ f.toFloat() }) else ({ deck.pos })
                Box(Modifier.fillMaxSize().cardDrag(deck, vertical = false, pxPerCard = RING_DRAG * unit).stageTilt(env), contentAlignment = Alignment.Center) {
                    for (i in CardPoses.window(f, n, loop = true)) {
                        val item = env.items[i]
                        val front = i == f
                        key(item.key) {
                            PosedCard(
                                env, item,
                                order = CardPoses.drawOrder(CardPoses.ring(CardPoses.offset(i, f.toFloat(), n, true), slots)),
                                width = w, height = h, unitPx = unit,
                                pose = { CardPoses.ring(CardPoses.offset(i, pos(), n, true), slots) },
                                onTap = { if (front) env.activate(item) else deck.goTo(i) },
                                sheen = front,
                                shown = { CardPoses.ringShows(CardPoses.offset(i, pos(), n, true), slots) },
                            ) { ItemFront(env, item, w, h, parallax = front) }
                        }
                    }
                }
            }
        }
        Counter(env, focus)
        ControlBar(env) { OpenButton(env, env.items.getOrNull(focus)) }
    }
}

// ==================================================================== 겹친 더미

/**
 * A pile (board `겹친 더미 넘기기`): flick the top card and it arcs off to the right and returns under the pile;
 * `다음 카드` does the same, `이전 카드` brings the bottom card back on top. Tapping the top card (or `열기`)
 * opens it. Six cards of the pile are drawn.
 */
@Composable
private fun StackView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val density = LocalDensity.current.density
            val unit = unitPx(density, STACK_BOARD_H)
            val w = (220 * unit / density).dp
            val h = w * CARD_RATIO
            ReducedFade(env.reduced, focus) { f ->
                val pos: () -> Float = if (env.reduced) ({ f.toFloat() }) else ({ deck.pos })
                Box(
                    Modifier.fillMaxSize().cardDrag(deck, vertical = false, pxPerCard = STACK_DRAG * unit, sign = -1f).stageTilt(env),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.offset { IntOffset(0, (STACK_DROP * unit).roundToInt()) }, contentAlignment = Alignment.Center) {
                        for (i in stackWindow(f, n)) {
                            val item = env.items[i]
                            val top = i == f
                            key(item.key) {
                                PosedCard(
                                    env, item,
                                    order = CardPoses.drawOrder(CardPoses.stack(stackOffset(i, f.toFloat(), n))),
                                    width = w, height = h, unitPx = unit,
                                    pose = { CardPoses.stack(stackOffset(i, pos(), n)) },
                                    onTap = { if (top) env.activate(item) else deck.goTo(i) },
                                    sheen = top,
                                    shown = { CardPoses.stackShows(stackOffset(i, pos(), n)) },
                                ) { ItemFront(env, item, w, h, parallax = top) }
                            }
                        }
                    }
                }
            }
        }
        Counter(env, focus)
        ControlBar(env) { OpenButton(env, env.items.getOrNull(focus)) }
    }
}

/** Pile offset of card [i]: 0 = top, up to n − 1 at the bottom; the card just flicked is between −1 and 0. */
internal fun stackOffset(i: Int, pos: Float, n: Int): Float {
    if (n <= 0) return 0f
    val d = ((i - pos) % n + n) % n
    return if (d > n - 1) d - n else d
}

/** The flicked card and the six of the pile. */
internal fun stackWindow(focus: Int, n: Int): List<Int> =
    if (n <= 0) emptyList() else (-1..5).map { Math.floorMod(focus + it, n) }.distinct()

// ==================================================================== 타로 펼침

/**
 * Cards dealt face-down in an arc (board `타로 펼침`, characters in 0.2.0, D2), five to a hand. Tapping a card
 * or `뒤집기` turns it over; a turned card's `열기` opens it. `모두 뒤집기` turns the whole hand,
 * `다시 펼치기` deals it again face-down, and stepping past the hand deals the next one. The face-down back
 * shows nothing about the character.
 */
@Composable
private fun TarotView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    val handNo = CardPoses.handOf(focus)
    val range = CardPoses.hand(handNo, n)
    // Face-up cards belong to the hand they were turned in: a new hand is dealt face-down.
    var turned by remember { mutableStateOf(-1 to emptySet<String>()) }
    var redeals by remember { mutableIntStateOf(0) }
    val faceUp = if (turned.first == handNo) turned.second else emptySet()
    val setFaceUp: (Set<String>) -> Unit = { turned = handNo to it }
    val dealSeq = handNo * 10_000 + redeals
    val isUp: (DeckItem) -> Boolean = { it is DeckItem.Lock || it.key in faceUp }
    val focusItem = env.items.getOrNull(focus)
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val density = LocalDensity.current.density
            val unit = unitPx(density, TAROT_BOARD_H)
            val w = (100 * unit / density).dp
            val h = w * CARD_RATIO
            ReducedFade(env.reduced, TarotFrame(focus, faceUp, dealSeq)) { frame ->
                Box(Modifier.fillMaxSize().stageTilt(env), contentAlignment = Alignment.Center) {
                    Box(Modifier.offset { IntOffset(0, (-TAROT_LIFT * unit).roundToInt()) }, contentAlignment = Alignment.Center) {
                        val count = range.count()
                        for (i in range) {
                            val item = env.items[i]
                            val slot = i - range.first
                            key(item.key) {
                                TarotCard(env, item, slot, count, i == frame.focus, item is DeckItem.Lock || item.key in frame.faceUp, frame.deal, w, h, unit) {
                                    when {
                                        !isUp(item) -> {
                                            setFaceUp(faceUp + item.key)
                                            deck.goTo(i)
                                        }
                                        i == frame.focus -> env.activate(item)
                                        else -> deck.goTo(i)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Counter(env, focus)
        ControlBar(env) {
            when {
                focusItem == null -> Unit
                !isUp(focusItem) -> CivilightIconButton(IconButtonSpec.CARD_FLIP, { setFaceUp(faceUp + focusItem.key) }, on = false)
                else -> OpenButton(env, focusItem)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            val downInHand = range.map { env.items[it] }.filterNot(isUp)
            if (downInHand.isNotEmpty()) {
                CivilightIconButton(IconButtonSpec.TAROT_FLIP_ALL, { setFaceUp(faceUp + downInHand.map { it.key }) })
            }
            CivilightIconButton(IconButtonSpec.TAROT_REDEAL, {
                setFaceUp(emptySet())
                redeals++
            })
        }
    }
}

private data class TarotFrame(val focus: Int, val faceUp: Set<String>, val deal: Int)

@Composable
private fun TarotCard(
    env: CardEnv,
    item: DeckItem,
    slot: Int,
    count: Int,
    focused: Boolean,
    up: Boolean,
    dealSeq: Int,
    w: Dp,
    h: Dp,
    unit: Float,
    onTap: () -> Unit,
) {
    val motion = CivilightTheme.motion
    val deal = remember { Animatable(0f) }
    LaunchedEffect(dealSeq) {
        deal.snapTo(0f)
        deal.animateTo(1f, motion.move(CivilightMotion.enter, delayMs = slot * CivilightMotion.swipeStaggerMs))
    }
    val raise = animateFloatAsState(if (focused) 1f else 0f, motion.move(CivilightMotion.standard), label = "tarotRaise")
    val turn = animateFloatAsState(if (up) 1f else 0f, motion.move(CivilightMotion.enter), label = "tarotTurn")
    val showFront by remember { derivedStateOf { turn.value >= 0.5f } }
    PosedCard(
        env, item,
        order = slot.toFloat() + if (focused) 10f else 0f,
        width = w, height = h, unitPx = unit,
        pose = {
            // Two poses are mixed only while the raise moves; at rest one pose is built.
            val r = raise.value
            when {
                r <= 0f -> CardPoses.tarot(slot, count, false, deal.value)
                r >= 1f -> CardPoses.tarot(slot, count, true, deal.value)
                else -> CardPoses.mix(CardPoses.tarot(slot, count, false, deal.value), CardPoses.tarot(slot, count, true, deal.value), r)
            }
        },
        onTap = onTap,
        sheen = focused && up,
        turnY = if (env.reduced) null else ({ 180f * (1 - turn.value) }),
        // Face-down, a card says only what tapping does: who is under it stays a surprise until it turns.
        clickLabel = if (up) itemLabel(item) else IconButtonCopy.FLIP,
    ) {
        if (showFront) {
            ItemFront(env, item, w, h)
        } else {
            Box(Modifier.graphicsLayer { if (!env.reduced) rotationY = 180f }) { TarotBackFace(w, h) }
        }
    }
}

// ==================================================================== 기울어진 벽

/**
 * Cards hung on a wall turned in 3D (board `기울어진 격자 벽`): the tilt turns the wall with parallax depth and a
 * holo light band. A vertical drag moves the wall by rows under the finger and settles on the nearest row (the
 * same column, or the row's last card). Tapping a card, or `이전 카드 · 다음 카드`, pulls it forward out of the
 * wall; `열기` opens the focused card (pulled or not) and `닫기` puts a pulled card back. Five rows around the
 * focus are drawn.
 */
@Composable
private fun TiltedWallView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    var pulled by rememberSaveable { mutableStateOf(false) }
    val focusItem = env.items.getOrNull(focus)
    val motion = CivilightTheme.motion
    val scope = rememberCoroutineScope()
    // The wall's row (fractional while it moves) and the finger's share of it while a drag is down.
    val row = remember { Animatable(CardPoses.wallRow(focus).toFloat()) }
    var dragRows by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(focus, env.reduced) {
        if (dragging) return@LaunchedEffect
        val target = CardPoses.wallRow(focus).toFloat()
        if (env.reduced) row.snapTo(target) else row.animateTo(target, motion.move(CivilightMotion.standard))
    }
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val density = LocalDensity.current.density
            val unit = unitPx(density, WALL_BOARD_H)
            val stageWdp = maxWidth
            val gap = 10.dp
            val cw = ((stageWdp - 48.dp - gap * 2) / 3).coerceAtLeast(60.dp)
            val ch = cw * CARD_RATIO
            val rowPx = ch.toPx(density) + gap.toPx(density)
            val colPx = cw.toPx(density) + gap.toPx(density)
            ReducedFade(env.reduced, WallFrame(focus, pulled)) { frame ->
                val pull = animateFloatAsState(if (frame.pulled) 1f else 0f, motion.move(CivilightMotion.enterLong), label = "wallPull")
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        rotationX = CardPoses.wallRotX(if (env.tiltOn) env.tilt.y else 0f)
                        rotationY = CardPoses.wallRotY(if (env.tiltOn) env.tiltX else 0f)
                        cameraDistance = CAMERA * 2 * density
                    }.pointerInput(deck, rowPx, env.reduced) {
                        var moved = 0f
                        var last = 0f
                        // The column the drag started in: kept through a short last row.
                        var column = 0
                        val release: (Boolean) -> Unit = { done ->
                            val lastRow = CardPoses.wallRow((deck.size - 1).coerceAtLeast(0))
                            val from = row.value + dragRows
                            val targetRow = when {
                                !done -> CardPoses.wallRow(deck.index)
                                env.reduced -> if (abs(moved) > Deck.DRAG_STEP) (CardPoses.wallRow(deck.index) + moved.sign.toInt()).coerceIn(0, lastRow) else CardPoses.wallRow(deck.index)
                                else -> CardPoses.wallDragTarget(from, last, lastRow)
                            }
                            moved = 0f
                            last = 0f
                            scope.launch {
                                row.snapTo(from)
                                dragRows = 0f
                                dragging = false
                                deck.snap(CardPoses.wallIndex(targetRow, column, deck.size))
                                if (env.reduced) row.snapTo(targetRow.toFloat()) else row.animateTo(targetRow.toFloat(), motion.move(CivilightMotion.standard))
                            }
                        }
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                column = CardPoses.wallColumn(deck.index)
                                scope.launch { row.stop() }
                            },
                            onDragEnd = { release(true) },
                            onDragCancel = { release(false) },
                        ) { change, d ->
                            change.consume()
                            val rows = -d / rowPx
                            moved += rows
                            last = rows
                            // Under reduced motion the wall does not follow; the release steps one row.
                            if (!env.reduced) {
                                val lastRow = CardPoses.wallRow((deck.size - 1).coerceAtLeast(0))
                                dragRows = CardPoses.wallDragRow(row.value + dragRows + rows, lastRow) - row.value
                                // The focus follows the row under the finger, so the drawn rows (two each side) move along.
                                val under = (row.value + dragRows).roundToInt().coerceIn(0, lastRow)
                                if (under != CardPoses.wallRow(deck.index)) deck.snap(CardPoses.wallIndex(under, column, deck.size))
                            }
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    for (i in CardPoses.wallWindow(frame.focus, n)) {
                        val item = env.items[i]
                        val col = CardPoses.wallColumn(i)
                        val r = CardPoses.wallRow(i)
                        val focused = i == frame.focus
                        key(item.key) {
                            PosedCard(
                                env, item,
                                order = CardPoses.wallDepth(i),
                                width = cw, height = ch, unitPx = 1f,
                                pose = {
                                    CardPose(
                                        x = (col - 1) * colPx,
                                        y = (r - row.value - dragRows) * rowPx,
                                        // Board px: the projection's perspective is in board px too.
                                        z = CardPoses.wallDepth(i),
                                        alpha = if (focused) 1f - pull.value else 1f,
                                    )
                                },
                                onTap = {
                                    deck.goTo(i)
                                    pulled = true
                                },
                                sheen = true,
                            ) {
                                Box(if (focused && !frame.pulled) Modifier.border(2.dp, CivilightTheme.colors.acc, RoundedCornerShape(CARD_RADIUS)) else Modifier) {
                                    ItemFront(env, item, cw, ch, parallax = true)
                                }
                            }
                        }
                    }
                }
                val pullShown by remember(pull) { derivedStateOf { pull.value > 0f } }
                if (frame.pulled || pullShown) {
                    val item = env.items.getOrNull(frame.focus)
                    if (item != null) {
                        val fw = min(264f * unit / density, (constraints.maxHeight * 0.86f / density) / CARD_RATIO).dp
                        Box(
                            Modifier.fillMaxSize().zIndex(10f).graphicsLayer { alpha = pull.value }.background(CivilightTheme.colors.shade.copy(alpha = 0.5f))
                                .pressable(onClick = { pulled = false }, scaleOnPress = false, pressOverlay = false, onClickLabel = IconButtonCopy.CLOSE),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.graphicsLayer {
                                    val s = CardPoses.lerp(PULL_FROM, 1f, pull.value)
                                    scaleX = s
                                    scaleY = s
                                    translationY = (1 - pull.value) * PULL_RISE * unit
                                }.clip(RoundedCornerShape(CARD_RADIUS)).cardLight(
                                    shade = CivilightTheme.colors.shade,
                                    band = CivilightTheme.colors.hl,
                                    dim = { 0f },
                                    sheen = if (env.tiltOn) ({ CardPoses.sheen(env.tiltX) }) else null,
                                ),
                            ) { ItemFront(env, item, fw, fw * CARD_RATIO, parallax = true) }
                        }
                    }
                }
            }
        }
        Counter(env, focus)
        // `이전 카드 · 다음 카드` on the wall pull the next card forward (board), so a step always shows one card;
        // `열기` opens the focused card without pulling it first.
        ControlBar(env, onStep = { d ->
            deck.step(d)
            pulled = true
        }) {
            OpenButton(env, focusItem)
            if (pulled) CivilightIconButton(IconButtonSpec.CLOSE, { pulled = false })
        }
    }
}

private fun Dp.toPx(density: Float): Float = value * density

private data class WallFrame(val focus: Int, val pulled: Boolean)

// ==================================================================== 롤로덱스

/**
 * Index cards on a vertical drum (board `세로 롤로덱스`): the front card flips down to show the next. Drag up
 * or down, `이전 카드 · 다음 카드` (up · down), or a letter of the 초성 index row; `열기` opens the front card.
 */
@Composable
private fun RolodexView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    val index = remember(env.input.cards) { CardPoses.indexRow(env.input.cards.map { it.title }) }
    val focusLetter = (env.items.getOrNull(focus) as? DeckItem.Character)?.let { CardPoses.indexLetter(it.card.title) }
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val density = LocalDensity.current.density
            val unit = unitPx(density, ROLO_BOARD_H)
            val w = (320 * unit / density).dp
            val h = w * INDEX_RATIO
            ReducedFade(env.reduced, focus) { f ->
                val pos: () -> Float = if (env.reduced) ({ f.toFloat() }) else ({ deck.pos })
                Box(
                    Modifier.fillMaxSize().cardDrag(deck, vertical = true, pxPerCard = ROLO_DRAG * unit).then(
                        if (!env.tiltOn) Modifier else Modifier.graphicsLayer {
                            rotationX = -env.tilt.y * 5f
                            rotationY = env.tiltX * 9f
                            cameraDistance = CAMERA * 2 * density
                        },
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.offset { IntOffset(0, (ROLO_DROP * unit).roundToInt()) }, contentAlignment = Alignment.Center) {
                        for (i in CardPoses.window(f, n, loop = false, before = 1, after = CardPoses.WINDOW)) {
                            val item = env.items[i]
                            val front = i == f
                            key(item.key) {
                                PosedCard(
                                    env, item,
                                    order = CardPoses.drawOrder(CardPoses.rolodex((i - f).toFloat())) + if (i < f) 100f else 0f,
                                    width = w, height = h, unitPx = unit,
                                    pose = { CardPoses.rolodex(i - pos()) },
                                    onTap = { if (front) env.activate(item) else deck.goTo(i) },
                                    sheen = front,
                                    shown = { CardPoses.rolodexShows(i - pos()) },
                                ) {
                                    when (item) {
                                        is DeckItem.Character -> IndexCardFront(item.card, env.input.imageOf(item.card.key), item.card.key in env.input.hiddenKeys, w, h)
                                        is DeckItem.Lock -> LockCardFace(item.row, w, h, env.actions.unlock)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (index.size > 1) {
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(index, key = { it.first }) { (letter, first) ->
                    FilterChip(letter, letter == focusLetter, { deck.goTo(first) })
                }
            }
        }
        Counter(env, focus)
        ControlBar(env, vertical = true) { OpenButton(env, env.items.getOrNull(focus)) }
    }
}

// ==================================================================== sizes (board px)

/** Card height / width of the portrait cards (portrait 4:5 plus the title lines). */
internal const val CARD_RATIO = 1.55f

/** Index card height / width (롤로덱스). */
private const val INDEX_RATIO = 0.6f

private const val FAN_BOARD_H = 620f
private const val RING_BOARD_H = 520f
private const val STACK_BOARD_H = 560f
private const val TAROT_BOARD_H = 520f
private const val WALL_BOARD_H = 600f
private const val ROLO_BOARD_H = 520f

/** Drag distance that moves one card, board px. */
private const val FAN_DRAG = 90f
private const val RING_DRAG = 120f
private const val STACK_DRAG = 260f
private const val ROLO_DRAG = 110f

/** The pile sits a little low so the cards behind can peek out above. */
private const val STACK_DROP = 50f

/** The arc sits above the centre (the deal comes from below it). */
private const val TAROT_LIFT = 60f

/** The drum's front card sits below the centre; the waiting cards rise above it. */
private const val ROLO_DROP = 70f

/** A pulled wall card grows from this scale and rises from this far below, board px. */
private const val PULL_FROM = 0.7f
private const val PULL_RISE = 40f
