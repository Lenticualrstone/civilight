package io.github.lenticualrstone.civilight.ui.back

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** What one press of system back does (0.2.3 `뒤로가기`). */
enum class BackStep {
    /** Nothing here: the lock screen takes back itself. */
    NONE,

    /** The keyboard is up: it goes down first. */
    HIDE_KEYBOARD,

    /** A sheet, popup, dialog, side panel or an in-progress mode (`고치기`, draft-only) is open: it closes first. */
    CLOSE_LAYER,

    /** Back to the previous screen. */
    POP,

    /** On the bottom screen (서재): the first press only says `한 번 더 누르면 닫힘` for [BackPolicy.EXIT_WINDOW_MS]. */
    ARM_EXIT,

    /** The second press within the window (or while the hint is still shown) closes the app. */
    EXIT,
}

/** What the policy looks at when back is pressed. Times in ms on one monotonic clock. */
data class BackSnapshot(
    val imeVisible: Boolean,
    /** Open layers that close on back ([BackLayers]). */
    val openLayers: Int,
    val stackSize: Int,
    val locked: Boolean,
    /** When the exit hint was shown last, or null. */
    val exitArmedAtMs: Long?,
    val nowMs: Long,
    /** The exit hint toast is still on screen (an accessibility timeout can keep it past the window). */
    val hintShowing: Boolean = false,
)

/**
 * (0.2.3) The one back policy of the app, in order: lock screen → keyboard → the layer opened last → the previous
 * screen → on the bottom screen a two-press exit. Pure, JVM-tested; the shell runs it from the one root handler
 * and screens register their layers through `LocalBackHandler` (the shell's `LayeredBackHandler`).
 */
object BackPolicy {
    /** The second press must come within this time (decision 0.2.3: 2 s). */
    const val EXIT_WINDOW_MS: Long = 2_000L

    fun decide(s: BackSnapshot): BackStep = when {
        s.locked -> BackStep.NONE
        s.imeVisible -> BackStep.HIDE_KEYBOARD
        s.openLayers > 0 -> BackStep.CLOSE_LAYER
        s.stackSize > 1 -> BackStep.POP
        armed(s) -> BackStep.EXIT
        else -> BackStep.ARM_EXIT
    }

    /**
     * Whether the shell's root handler takes this press. Layers and the previous screen handle back with their own
     * handlers (and the screen's predictive back); the root handler only covers the keyboard and the bottom screen.
     */
    fun rootIntercepts(s: BackSnapshot): Boolean = when (decide(s)) {
        BackStep.HIDE_KEYBOARD, BackStep.ARM_EXIT, BackStep.EXIT -> true
        BackStep.NONE, BackStep.CLOSE_LAYER, BackStep.POP -> false
    }

    private fun armed(s: BackSnapshot): Boolean {
        if (s.hintShowing) return true
        val at = s.exitArmedAtMs ?: return false
        val since = s.nowMs - at
        return since in 0..EXIT_WINDOW_MS
    }
}

/** Copy of the back policy. */
object BackCopy {
    /** The bottom screen's first back (decision 0.2.3). */
    const val EXIT_HINT = "한 번 더 누르면 닫힘"

    val all: List<String> = listOf(EXIT_HINT)
}

/** How many layers that close on back are open now (sheets, side panels, in-progress modes). */
@Stable
class BackLayers {
    var open by mutableIntStateOf(0)
        private set

    fun add() {
        open += 1
    }

    fun remove() {
        open = (open - 1).coerceAtLeast(0)
    }
}
