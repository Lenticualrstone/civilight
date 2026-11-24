package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Board `.tbl` 390 × 330, `perspective: 640px` at 50 % · 24 %. */
private const val TAROT_H = 330f

/** Board `.plane`: 14 px in from each side, 10 down, 280 tall, `rotateY(tx·6) rotateX(50 + ty·4)` about its centre. */
private val TAROT_SCENE = Scene(640f, 195f, 79.2f, 195f, 150f, rigFirstY = true)
private const val PLANE_X = 14f
private const val PLANE_Y = 10f
private const val PLANE_W = 362f
private const val PLANE_H = 280f
private const val TABLE_TILT = 50f

/** Board `.tc` at (139, 62) of the plane, turning about its bottom centre: pivot (195, 198) of the stage. */
private const val TAROT_PIVOT_Y = PLANE_Y + 62f + CardPoses.TAROT_H

/** Board deal: the first card leaves the deck after 160 ms; a re-deal gathers the cards 70 ms apart, then waits 450 ms. */
private const val DEAL_START_MS = 160
private const val GATHER_STEP_MS = 70
private const val GATHER_REST_MS = 450

/** One 타로 card's body: its own springs, as the board's `Body` (x, y, z, rotZ, rotX and the turn f in degrees). */
@Stable
private class TarotBody(start: CardPose) {
    val x = Animatable(start.x)
    val y = Animatable(start.y)
    val z = Animatable(start.z)
    val rz = Animatable(start.rotZ)
    val rx = Animatable(start.rotX)
    val f = Animatable(0f)

    suspend fun jump(p: CardPose) {
        x.snapTo(p.x)
        y.snapTo(p.y)
        z.snapTo(p.z)
        rz.snapTo(p.rotZ)
        rx.snapTo(p.rotX)
    }

    /** Moves x, y, rotZ, rotX (and z unless [keepZ]) on one spring; returns at once (the springs run in [scope]). */
    fun to(scope: CoroutineScope, p: CardPose, stiffness: Float, damping: Float, keepZ: Boolean = false) {
        val s = spring<Float>(damping, stiffness)
        scope.launch { x.animateTo(p.x, s) }
        scope.launch { y.animateTo(p.y, s) }
        scope.launch { rz.animateTo(p.rotZ, s) }
        scope.launch { rx.animateTo(p.rotX, s) }
        if (!keepZ) scope.launch { z.animateTo(p.z, s) }
    }

    fun lift(scope: CoroutineScope, height: Float, stiffness: Float, damping: Float) {
        scope.launch { z.animateTo(height, spring(damping, stiffness)) }
    }
}

/**
 * Cards dealt face-down in an arc on a tilted table (board `타로 펼침`, characters in 0.2.0, D2), five to a hand.
 * Tapping a card or `뒤집기` turns it over (it lifts off the table while it turns); a turned card's `열기`
 * opens it. `모두 뒤집기` turns the whole hand, `다시 펼치기` gathers it and deals it again face-down, and
 * stepping past the hand deals the next one. The panel under the table names the focus card once it is face-up;
 * the face-down back shows nothing about the character.
 */
@Composable
internal fun TarotView(env: CardEnv) {
    val deck = env.deck
    val focus = deck.index
    val n = env.items.size
    val handNo = CardPoses.handOf(focus)
    val range = CardPoses.hand(handNo, n)
    val count = range.count()
    // Face-up cards belong to the hand they were turned in: a new hand is dealt face-down.
    var turned by remember { mutableStateOf(-1 to emptySet<String>()) }
    var redeals by remember { mutableIntStateOf(0) }
    var dealt by remember { mutableStateOf(false) }
    val faceUp = if (turned.first == handNo) turned.second else emptySet()
    val setFaceUp: (Set<String>) -> Unit = { turned = handNo to it }
    val isUp: (DeckItem) -> Boolean = { it is DeckItem.Lock || it.key in faceUp }
    val focusItem = env.items.getOrNull(focus)
    val projector = remember { Projector() }
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    val shadow = remember(mat) { Shadows.tarot(mat) }
    val tilt = env.tilt
    val motion = CivilightTheme.motion
    // One body per slot of the hand, rebuilt for each hand.
    val bodies = remember(handNo, count) { List(count) { TarotBody(CardPoses.tarotDeck(it)) } }
    val slotOf: (Int) -> Int = { it - range.first }
    val place: (Int, Boolean) -> CardPose = { slot, raised -> CardPoses.tarot(slot, count, dealt && raised) }

    // The deal (board `deal`): each card leaves the deck 110 ms after the last, lifts and lays down in its place.
    LaunchedEffect(bodies, redeals) {
        dealt = false
        if (env.reduced) {
            bodies.forEachIndexed { i, b ->
                b.jump(CardPoses.tarot(i, count, false))
                b.f.snapTo(if (range.first + i < n && isUp(env.items[range.first + i])) 180f else 0f)
            }
            dealt = true
            bodies.getOrNull(slotOf(deck.index))?.jump(CardPoses.tarot(slotOf(deck.index), count, true))
            return@LaunchedEffect
        }
        coroutineScope {
            if (redeals > 0) {
                // Gather (board `redeal`): turn everything down, back to the deck from the right end.
                bodies.forEach { b -> launch { b.f.animateTo(0f, spring(CivilightMotion.tarotLiftDamping, CivilightMotion.tarotLiftStiffness)) } }
                for (k in bodies.indices) {
                    val i = bodies.size - 1 - k
                    delay(motion.delayMs(GATHER_STEP_MS))
                    bodies[i].to(this, CardPoses.tarotDeck(i), CivilightMotion.tarotDropStiffness, CivilightMotion.tarotDealDamping, keepZ = true)
                    bodies[i].lift(this, 60f, CivilightMotion.tarotLiftStiffness, CivilightMotion.tarotLiftDamping)
                }
                delay(motion.delayMs(GATHER_REST_MS))
                bodies.forEachIndexed { i, b -> b.lift(this, CardPoses.tarotDeck(i).z, CivilightMotion.tarotDropStiffness, CivilightMotion.tarotDropDamping) }
            }
            delay(motion.delayMs(DEAL_START_MS))
            bodies.forEachIndexed { i, b ->
                launch {
                    b.to(this, CardPoses.tarot(i, count, false), CivilightMotion.tarotDealStiffness, CivilightMotion.tarotDealDamping, keepZ = true)
                    b.lift(this, CardPoses.TAROT_DEAL_LIFT, CivilightMotion.tarotLiftStiffness, CivilightMotion.tarotLiftDamping)
                    delay(motion.delayMs(CivilightMotion.tarotDropDelayMs))
                    b.lift(this, 1f, CivilightMotion.tarotDropStiffness, CivilightMotion.tarotDropDamping)
                }
                delay(motion.delayMs(CivilightMotion.tarotDealStaggerMs))
            }
        }
        dealt = true
    }
    // The focus card rises off the table; the last one lies back down (board `setFocus`).
    LaunchedEffect(bodies, focus, dealt) {
        if (!dealt) return@LaunchedEffect
        coroutineScope {
            bodies.forEachIndexed { i, b ->
                val raised = range.first + i == focus
                val p = place(i, raised)
                if (env.reduced) {
                    b.jump(p)
                } else if (raised) {
                    b.to(this, p, CivilightMotion.tarotFocusStiffness, CivilightMotion.tarotFocusDamping)
                } else {
                    b.to(this, p, CivilightMotion.tarotFocusStiffness, CivilightMotion.tarotDropDamping)
                }
            }
        }
    }
    // A card turning over lifts to 74 px and lays down again (board `flip`).
    LaunchedEffect(bodies, faceUp) {
        coroutineScope {
            bodies.forEachIndexed { i, b ->
                val idx = range.first + i
                if (idx >= n) return@forEachIndexed
                val target = if (isUp(env.items[idx])) 180f else 0f
                if (b.f.targetValue == target) return@forEachIndexed
                if (env.reduced) {
                    b.f.snapTo(target)
                    return@forEachIndexed
                }
                launch { b.f.animateTo(target, spring(CivilightMotion.tarotTurnDamping, CivilightMotion.tarotTurnStiffness)) }
                launch {
                    val rest = place(i, idx == focus).z
                    b.z.animateTo(CardPoses.TAROT_TURN_LIFT, spring(CivilightMotion.tarotLiftDamping, CivilightMotion.tarotLiftStiffness))
                    b.z.animateTo(rest, spring(CivilightMotion.tarotDropDamping, CivilightMotion.tarotDropStiffness))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        BoardStage(TAROT_H, Modifier.weight(1f).fillMaxWidth()) { unit, bp ->
            ReducedFade(env.reduced, TarotFrame(focus, faceUp)) { frame ->
                // The table (board `.plane`): its own turn, under the cards.
                val planeXf = remember { CardXform() }
                Box(
                    Modifier.zIndex(BEHIND).offset(bp.dp(PLANE_X), bp.dp(PLANE_Y)).size(bp.dp(PLANE_W), bp.dp(PLANE_H))
                        .graphicsLayer {
                            projector.solve(planeXf, TAROT_SCENE, TABLE_TILT + tilt.y * 4f, tilt.x * 6f, 195f, 150f, 0f, 0f, 0f)
                            translationX = planeXf.translationX * unit
                            translationY = planeXf.translationY * unit
                            scaleX = planeXf.scale
                            scaleY = planeXf.scale
                            rotationX = planeXf.rotationX
                            rotationY = planeXf.rotationY
                            rotationZ = planeXf.rotationZ
                            cameraDistance = Projector.cameraDistance(TAROT_SCENE.perspective, unit)
                        }
                        .drawBehind {
                            val r = CornerRadius(bp.k * 30f * density)
                            drawRoundRect(Brush.verticalGradient(listOf(mat.table1, mat.table2)), cornerRadius = r)
                            val line = 1.dp.toPx()
                            drawRoundRect(mat.tableLine, topLeft = Offset(line / 2, line / 2), size = Size(size.width - line, size.height - line), cornerRadius = r, style = Stroke(line))
                            val inset = bp.k * 14f * density
                            drawRoundRect(
                                mat.tableLine, topLeft = Offset(inset, inset), size = Size(size.width - inset * 2, size.height - inset * 2),
                                cornerRadius = CornerRadius(bp.k * 20f * density), style = Stroke(line),
                            )
                        },
                )
                for (i in range) {
                    val item = env.items[i]
                    val slot = i - range.first
                    val body = bodies.getOrNull(slot) ?: continue
                    val focused = i == frame.focus
                    val up = isUp(item)
                    key(item.key) {
                        PosedCard(
                            w = CardPoses.TAROT_W, h = CardPoses.TAROT_H, pivotX = 195f, pivotY = TAROT_PIVOT_Y,
                            unit = unit, bp = bp, radius = 10f, shadow = shadow, camera = TAROT_SCENE.perspective,
                            originY = 1f,
                            orderBias = slot * 0.01f,
                            place = { xf ->
                                projector.solve(
                                    xf, TAROT_SCENE, TABLE_TILT + tilt.y * 4f, tilt.x * 6f, 195f, TAROT_PIVOT_Y,
                                    body.x.value, body.y.value, body.z.value, rotX = body.rx.value, rotZ = body.rz.value,
                                    order = TurnOrder.ZX, flipY = body.f.value,
                                )
                                xf.alpha = 1f
                                xf.dim = 0f
                            },
                            onTap = {
                                when {
                                    !isUp(item) -> {
                                        setFaceUp(faceUp + item.key)
                                        deck.goTo(i)
                                    }
                                    i == frame.focus -> env.activate(item)
                                    else -> deck.goTo(i)
                                }
                            },
                            // Face-down, a card says only what tapping does: who is under it stays a surprise until it turns.
                            clickLabel = if (up) itemLabel(item) else IconButtonCopy.FLIP,
                        ) { back ->
                            Box(Modifier.fillMaxSize().then(if (focused && dealt) Modifier.border(2.dp, colors.acc, RoundedCornerShape(bp.dp(10f))) else Modifier)) {
                                // The board's front sits turned 180° on the flip layer: it shows once the card's back faces us.
                                when {
                                    !back -> TarotBackFace(bp)
                                    item is DeckItem.Lock -> LockCardFace(item.row, bp, compact = true, onUnlock = env.actions.unlock)
                                    item is DeckItem.Character -> TarotFrontFace(
                                        item.card, env.input.imageOf(item.card.key), bp,
                                        sheen = { Offset(tilt.x * 0.3f - 0.12f, 0f) },
                                        tilt = { Offset(tilt.x, tilt.y) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        TarotPanel(env, focusItem, focusItem != null && isUp(focusItem), focus, handNo, CardPoses.hands(n))
        ControlBar(env) {
            when {
                focusItem == null -> Unit
                !isUp(focusItem) -> CivilightIconButton(IconButtonSpec.CARD_FLIP, { setFaceUp(faceUp + focusItem.key) }, on = false)
                else -> OpenButton(env, focusItem)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            CivilightIconButton(IconButtonSpec.TAROT_REDEAL, {
                setFaceUp(emptySet())
                redeals++
            })
            // Past five cards the deal is paged: `다음 펼침` deals the next five (each card is reachable).
            val hands = CardPoses.hands(n)
            if (hands > 1) {
                if (handNo > 0) {
                    CivilightButton(Cards3dCopy.PREV_SPREAD, { deck.goTo(CardPoses.hand(handNo - 1, n).first) }, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Prev)
                }
                if (handNo < hands - 1) {
                    CivilightButton(Cards3dCopy.NEXT_SPREAD, { deck.goTo(CardPoses.hand(handNo + 1, n).first) }, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Next)
                }
            }
            val downInHand = range.map { env.items[it] }.filterNot(isUp)
            if (downInHand.isNotEmpty()) {
                CivilightIconButton(IconButtonSpec.TAROT_FLIP_ALL, { setFaceUp(faceUp + downInHand.map { it.key }) })
            }
        }
    }
}

private data class TarotFrame(val focus: Int, val faceUp: Set<String>)

/**
 * Board `.pnl` under the table: the kind chip and `n / N`, then the name, the subtitle and the first appearance
 * of the focus card once it is face-up; face-down it reads `· · ·` and how to turn it.
 */
@Composable
private fun TarotPanel(env: CardEnv, item: DeckItem?, up: Boolean, index: Int, hand: Int, hands: Int) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightText(
                Cards3dCopy.KIND,
                type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
                Modifier.height(22.dp).clip(RoundedCornerShape(7.dp)).border(1.dp, colors.edge, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp).wrapContentSize(Alignment.Center),
                color = colors.ink2,
                maxLines = 1,
            )
            CivilightText(if (item is DeckItem.Character) Cards3dCopy.position(index, env.cardCount) else env.cardCount.toString(), type.small, color = colors.ink3, maxLines = 1)
            if (hands > 1) CivilightText(Cards3dCopy.spread(hand, hands), type.small, color = colors.ink3, maxLines = 1)
        }
        val name = when {
            item == null -> ""
            !up -> Cards3dCopy.FACE_DOWN
            item is DeckItem.Character -> item.card.title
            item is DeckItem.Lock -> item.row.title
            else -> ""
        }
        CivilightText(name, type.title.merge(TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)), Modifier.padding(top = 6.dp), color = colors.ink, maxLines = 1)
        val desc = when {
            !up -> Cards3dCopy.FACE_DOWN_HINT
            item is DeckItem.Character -> item.card.subtitle
            item is DeckItem.Lock -> item.row.sub
            else -> ""
        }
        CivilightText(desc, type.body, Modifier.padding(top = 4.dp), color = colors.ink2, maxLines = 2)
        val meta = if (up && item is DeckItem.Character) item.card.firstAppearance?.let { "${CastUiCopy.FIRST_APPEARANCE} $it" } else null
        CivilightText(meta.orEmpty(), type.small, Modifier.padding(top = 4.dp), color = colors.ink3, maxLines = 1)
    }
}
