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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
internal class Deck(
    val loop: Boolean,
    private val scope: CoroutineScope,
    size: Int,
    start: Int,
    private val stiffness: Float = CivilightMotion.cardDeckStiffness,
    private val damping: Float = CivilightMotion.cardDeckDamping,
) {
    private val posState = mutableFloatStateOf(start.toFloat())
    private val targetState = mutableFloatStateOf(start.toFloat())
    var size by mutableIntStateOf(size)
    var reduced by mutableStateOf(false)
    private var dragged = 0f
    private var dragRaw = Float.NaN
    private var dragStart = 0f
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

    /**
     * Moves by [cards] while a finger drags; a clamped view follows at a third past its ends (board rubber
     * band). Under reduced motion nothing moves until the release, which steps one card.
     */
    fun dragBy(cards: Float) {
        if (size == 0) return
        if (reduced) {
            dragged += cards
            return
        }
        if (dragRaw.isNaN()) {
            stopSettle()
            velocity = 0f
            dragRaw = pos
            dragStart = pos
        }
        dragRaw += cards
        val p = if (loop) dragRaw else CardPoses.rubber(dragRaw, size)
        posState.floatValue = p
        targetState.floatValue = p
    }

    /**
     * Releases a drag moving at [velocity] cards/s: settles on `round(pos + v · 0.16)` within two cards (board
     * `up`), carrying the finger's speed into the spring. With [stepAt] (롤로덱스) a drag of at least that many
     * cards turns exactly one card, and a shorter one goes back.
     */
    fun endDrag(velocity: Float, stepAt: Float? = null) {
        if (size == 0) return
        val start = dragStart
        dragRaw = Float.NaN
        if (reduced) {
            val d = dragged
            dragged = 0f
            if (abs(d) > (stepAt ?: DRAG_STEP)) step(d.sign.toInt())
            return
        }
        val t = if (stepAt != null) {
            val moved = pos - start
            val base = start.roundToInt()
            if (abs(moved) >= stepAt) base + moved.sign.toInt() else base
        } else {
            CardPoses.flingTarget(pos, velocity, size, loop)
        }
        this.velocity = velocity
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
        dragRaw = Float.NaN
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
            animate(from, target, v0, spring(damping, stiffness)) { value, v ->
                posState.floatValue = value
                velocity = v
            }
            velocity = 0f
        }
    }

    companion object {
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
internal fun rememberDeck(
    items: List<DeckItem>,
    loop: Boolean,
    reduced: Boolean,
    focusKey: Key?,
    stiffness: Float = CivilightMotion.cardDeckStiffness,
    damping: Float = CivilightMotion.cardDeckDamping,
): Deck {
    val scope = rememberCoroutineScope()
    var savedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val deck = remember(loop, stiffness, damping) {
        val start = savedKey?.let { k -> items.indexOfFirst { it.key == k } }?.takeIf { it >= 0 } ?: 0
        Deck(loop, scope, items.size, start, stiffness, damping)
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
