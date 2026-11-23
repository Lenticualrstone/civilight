package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.domain.cast.list.CastCard
import io.github.lenticualrstone.civilight.domain.cast.list.LockRow
import io.github.lenticualrstone.civilight.model.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** One item of a card view: a character card, or the lock card that ends the view. */
internal sealed interface DeckItem {
    val key: String

    data class Character(val card: CastCard) : DeckItem {
        override val key: String get() = card.key
    }

    data class Lock(val row: LockRow) : DeckItem {
        override val key: String get() = LOCK_KEY
    }

    companion object {
        const val LOCK_KEY = "cards3d-lock"

        /** The shown characters in list order, then the lock card while hidden characters are locked. */
        fun of(cards: List<CastCard>, lock: LockRow?): List<DeckItem> = cards.map { Character(it) } + listOfNotNull(lock?.let { Lock(it) })
    }
}

/**
 * The focus of a card view as one continuous position ([pos]: 2.4 = between the third and fourth card), so a
 * drag or a spring moves every card together. Looping views ([loop]) let [pos] run past the ends; the others
 * clamp. With reduced motion every change snaps (the stage fades instead).
 *
 * A drag writes [pos] straight from the pointer callback (no coroutine per event); a settle runs as one job
 * that the next drag, settle or snap cancels, carrying the spring's velocity into the next settle.
 */
@Stable
internal class Deck(val loop: Boolean, private val scope: CoroutineScope, size: Int, start: Int) {
    private val posState = mutableFloatStateOf(start.toFloat())
    private val targetState = mutableFloatStateOf(start.toFloat())
    var size by mutableIntStateOf(size)
    var reduced by mutableStateOf(false)
    private var dragged = 0f
    private var settleJob: Job? = null
    private var velocity = 0f

    /** A settle spring is running. */
    val settling: Boolean get() = settleJob?.isActive == true

    /** The continuous focus position (read it in layer and draw blocks). */
    val pos: Float get() = posState.floatValue

    /** The card the view rests on (or is passing). */
    val index: Int by derivedStateOf { CardPoses.indexAt(posState.floatValue, this.size, loop) }

    /** The card the view is heading for (the settle's target, or under the finger while dragging). */
    val targetIndex: Int by derivedStateOf { CardPoses.indexAt(targetState.floatValue, this.size, loop) }

    fun canStep(delta: Int): Boolean = if (loop) size > 1 else (index + delta) in 0 until size

    fun step(delta: Int) {
        if (!canStep(delta)) return
        val base = targetState.floatValue.roundToInt()
        val t = if (loop) base + delta else (base + delta).coerceIn(0, size - 1)
        settle(t.toFloat())
    }

    fun goTo(i: Int) {
        if (size == 0) return
        settle(CardPoses.nearestPos(pos, i, size, loop))
    }

    /** Moves by [cards] while a finger drags (snaps nothing under reduced motion; the release steps once). */
    fun dragBy(cards: Float) {
        if (size == 0) return
        if (reduced) {
            dragged += cards
            return
        }
        stopSettle()
        velocity = 0f
        val v = pos + cards
        val p = if (loop) v else v.coerceIn(-DRAG_OVER, size - 1 + DRAG_OVER)
        posState.floatValue = p
        targetState.floatValue = p
    }

    /** Releases a drag that last moved in [direction]: settles on the card it was heading for. */
    fun endDrag(direction: Float) {
        if (size == 0) return
        if (reduced) {
            val d = dragged
            dragged = 0f
            if (abs(d) > DRAG_STEP) step(d.sign.toInt())
            return
        }
        val t = (pos + DRAG_BIAS * direction.sign).roundToInt()
        settle(if (loop) t.toFloat() else t.coerceIn(0, size - 1).toFloat())
    }

    /**
     * The items changed to [n], with the focused card now at [kept]: a spring already heading for it carries
     * on; anything else lands on it at once. A clamped deck compares raw positions (indices clamp, so a deck
     * left past the new end would read as resting on the last card).
     */
    fun resize(n: Int, kept: Int) {
        size = n
        val stale = if (loop) {
            targetIndex != kept || (!settling && index != kept)
        } else {
            targetState.floatValue != kept.toFloat() || (!settling && pos != kept.toFloat())
        }
        if (stale) snap(kept)
    }

    fun snap(i: Int) {
        stopSettle()
        velocity = 0f
        posState.floatValue = i.toFloat()
        targetState.floatValue = i.toFloat()
    }

    private fun stopSettle() {
        settleJob?.cancel()
        settleJob = null
    }

    private fun settle(target: Float) {
        stopSettle()
        targetState.floatValue = target
        if (reduced || pos == target) {
            velocity = 0f
            posState.floatValue = target
            return
        }
        val from = pos
        val v0 = velocity
        settleJob = scope.launch {
            animate(from, target, v0, spring(CivilightMotion.viewSettleDamping, CivilightMotion.viewSettleStiffness)) { value, v ->
                posState.floatValue = value
                velocity = v
            }
            velocity = 0f
        }
    }

    companion object {
        /** How far a clamped view follows a drag past its end, in cards. */
        const val DRAG_OVER = 0.4f

        /** A released drag carries on this far in its direction before rounding, so a short flick still turns a card. */
        const val DRAG_BIAS = 0.35f

        /** Under reduced motion a drag of this many cards steps one. */
        const val DRAG_STEP = 0.25f
    }
}

/**
 * The deck of [items]: the focus is kept by key across filter changes and restores (a card that left keeps
 * its place), and [focusKey] (a card just made) is brought to the front once: a later filter or tag change
 * while it is still highlighted leaves the focus where the reader put it.
 */
@Composable
internal fun rememberDeck(items: List<DeckItem>, loop: Boolean, reduced: Boolean, focusKey: Key?): Deck {
    val scope = rememberCoroutineScope()
    var savedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val deck = remember(loop) {
        val start = savedKey?.let { k -> items.indexOfFirst { it.key == k } }?.takeIf { it >= 0 } ?: 0
        Deck(loop, scope, items.size, start)
    }
    deck.reduced = reduced
    LaunchedEffect(deck, items) {
        val kept = savedKey?.let { k -> items.indexOfFirst { it.key == k } }?.takeIf { it >= 0 }
            ?: deck.index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        deck.resize(items.size, kept)
        snapshotFlow { deck.targetIndex }.collect { savedKey = items.getOrNull(it)?.key }
    }
    // The card just made: brought forward once it is among the items (a filter that hid it may clear first).
    var handledFocus by remember { mutableStateOf<Key?>(null) }
    LaunchedEffect(deck, focusKey, items) {
        val key = focusKey
        if (key == null) {
            handledFocus = null
            return@LaunchedEffect
        }
        if (key == handledFocus) return@LaunchedEffect
        val i = items.indexOfFirst { it.key == key }
        if (i < 0) return@LaunchedEffect
        handledFocus = key
        savedKey = key
        deck.goTo(i)
    }
    return deck
}
