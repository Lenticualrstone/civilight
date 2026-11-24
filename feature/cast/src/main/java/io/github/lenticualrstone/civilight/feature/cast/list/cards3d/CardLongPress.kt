package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics

/**
 * The pure decision of a card's long press (JVM-tested): while the finger is down and the press timeout has
 * not passed, a press goes on [waiting][Verdict.WAIT] until it lifts, moves past the touch slop, is taken by
 * another gesture (a drag of the stage), or a second finger lands; any of those [cancels][Verdict.CANCEL] it.
 * Once the timeout passes with the press still waiting, it opens.
 */
internal object LongPressGate {
    enum class Verdict { WAIT, CANCEL }

    /** One pointer event of a press that went down at (0, 0) and is now at ([dx], [dy]) px. */
    fun verdict(dx: Float, dy: Float, slop: Float, lifted: Boolean, takenByOther: Boolean, pointers: Int): Verdict = when {
        lifted || takenByOther || pointers > 1 -> Verdict.CANCEL
        movedPastSlop(dx, dy, slop) -> Verdict.CANCEL
        else -> Verdict.WAIT
    }

    /** The finger left a circle of [slop] px around where it went down. */
    fun movedPastSlop(dx: Float, dy: Float, slop: Float): Boolean = dx * dx + dy * dy > slop * slop

    /** The long-press label: `열기` for a character, `지문으로 보기` for the lock card (the tap's unlock path). */
    fun label(item: DeckItem): String = when (item) {
        is DeckItem.Character -> Cards3dCopy.LONG_PRESS_OPEN
        is DeckItem.Lock -> Cards3dCopy.LONG_PRESS_UNLOCK
    }
}

/**
 * Long press on a card: held still for the platform's long-press timeout (within the touch slop), it gives the
 * long-press haptic and calls [onLongPress]; the rest of that press is consumed, so neither the card's tap, a
 * button on its face nor the stage's drag acts on it. Moving past the slop, lifting, or a stage drag taking
 * the finger first cancels it. TalkBack gets the same action as a long click labelled [label].
 */
@Composable
internal fun Modifier.cardLongPress(enabled: Boolean, label: String, onLongPress: () -> Unit): Modifier {
    if (!enabled) return this
    val haptic = LocalHapticFeedback.current
    val action by rememberUpdatedState(onLongPress)
    return this
        .semantics {
            onLongClick(label) {
                action()
                true
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                // The final pass throughout: the card's tap (and a button on its face) consume the down, a stage drag
                // the moves past its slop; the press only counts as taken when a move was consumed.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                val slop = viewConfiguration.touchSlop
                val timeout = viewConfiguration.longPressTimeoutMillis
                val cancelled = withTimeoutOrNull(timeout) {
                    var verdict = LongPressGate.Verdict.WAIT
                    while (verdict == LongPressGate.Verdict.WAIT) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        verdict = if (change == null) {
                            LongPressGate.Verdict.CANCEL
                        } else {
                            val d = change.position - down.position
                            LongPressGate.verdict(
                                d.x, d.y, slop,
                                lifted = !change.pressed,
                                takenByOther = change.isConsumed,
                                pointers = event.changes.count { it.pressed },
                            )
                        }
                    }
                    true
                }
                if (cancelled != null) return@awaitEachGesture
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                action()
                // Swallow the rest of this press before anyone else sees it: no tap, no button click, no drag.
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                    if (event.changes.none { it.pressed }) break
                }
            }
        }
}
