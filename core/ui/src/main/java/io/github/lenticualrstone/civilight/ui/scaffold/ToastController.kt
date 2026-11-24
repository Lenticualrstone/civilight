package io.github.lenticualrstone.civilight.ui.scaffold

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What a toast says (DS-67): a fact, one action (normally `되돌리기`), or a failure that needs nothing now. */
enum class NoticeKind { INFO, ACTION, ERROR }

/**
 * One toast; [actionLabel] null = no action (first-use tips, LIB-20). [projectKey] is the project it was
 * raised in (or acts on): the toast leaves at once when that project leaves the back stack (DS-67).
 */
data class ToastMessage(
    val id: Long,
    val text: String,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
    val kind: NoticeKind = if (onAction != null) NoticeKind.ACTION else NoticeKind.INFO,
    val projectKey: Key? = null,
)

/** Why a shown toast's timer stands still (DS-67). */
enum class ToastHold {
    /** A finger is down on the toast. */
    PRESSED,

    /** Accessibility focus is on the toast. */
    ACCESSIBILITY_FOCUS,

    /** The app is not resumed (`ON_STOP`). */
    APP_STOPPED,
}

/**
 * DS-67 notice rules: durations per kind and what dismisses a toast. Pure, so the timing is tested on the JVM.
 */
object NoticeRules {
    const val INFO_MS: Long = 2_500L
    const val ACTION_MS: Long = 4_000L
    const val ERROR_MS: Long = 4_000L

    /** Base duration of [kind]; no kind stays until dismissed. */
    fun durationMs(kind: NoticeKind): Long = when (kind) {
        NoticeKind.INFO -> INFO_MS
        NoticeKind.ACTION -> ACTION_MS
        NoticeKind.ERROR -> ERROR_MS
    }

    /** Events around a shown toast. */
    enum class Event {
        /** Its action ran. */
        ACTION,

        /** Its time ran out. */
        TIMER,

        /** A new toast replaced it. */
        REPLACED,

        /** The app lock appeared. */
        APP_LOCK,

        /** A hidden-project lock or `다시 잠그기` relocked something. */
        RELOCK,

        /** The project it belongs to left the back stack. */
        PROJECT_RELEASED,

        /** The screen that raised it was popped (page delete then `pop`): the undo must stay. */
        SCREEN_POPPED,

        /** A sheet opened, a pane's scope changed, or the screen behind changed. */
        SCREEN_CHANGED,
    }

    /** True when [toast] acts on a project that left the back stack ([liveProjects]): only an `ACTION` toast. */
    fun releasedFrom(toast: ToastMessage, liveProjects: Set<Key>): Boolean =
        toast.kind == NoticeKind.ACTION && toast.projectKey != null && toast.projectKey !in liveProjects

    /** Whether [event] dismisses the shown toast. */
    fun dismissOn(event: Event): Boolean = when (event) {
        Event.SCREEN_POPPED, Event.SCREEN_CHANGED -> false
        else -> true
    }

    /** Whether [event] removes the toast in the same frame with no exit motion (nothing may show while locked). */
    fun instant(event: Event): Boolean = event == Event.APP_LOCK || event == Event.RELOCK || event == Event.PROJECT_RELEASED

    /**
     * True when something that was open is locked again between [before] and [after]: the app lock came up, the
     * hidden group or a kept-open project relocked, a project's hidden items or the hidden records relocked.
     */
    fun locksTightened(before: UnlockState, after: UnlockState): Boolean =
        (!before.appLocked && after.appLocked) ||
            (before.hiddenGroupUnlockedAt != null && after.hiddenGroupUnlockedAt == null) ||
            (before.recordsUnlockedAt != null && after.recordsUnlockedAt == null) ||
            !after.itemsUnlockedAt.keys.containsAll(before.itemsUnlockedAt.keys) ||
            !after.keptOpenSince.keys.containsAll(before.keptOpenSince.keys)
}

/**
 * The app's single toast (LIB-40, DS-67): one toast at a time, a new one replaces the old and starts its own full
 * time; the action runs and dismisses. Time per [NoticeKind] ([NoticeRules.durationMs]: 2.5 s info, 4 s with an
 * action or an error), lengthened only by [recommendedTimeout] (the system accessibility timeout, set by the
 * shell). The timer stands still while any [ToastHold] is held and resumes with the time left.
 *
 * The shell draws [current] once per window (`WindowToastHost`); screens draw no toast of their own.
 * [durationMs] overrides every kind's time (previews and tests); [now] is the clock in ms for the time left.
 */
class ToastController(
    private val scope: CoroutineScope,
    private val durationMs: Long? = null,
    private val now: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val _current = MutableStateFlow<ToastMessage?>(null)
    val current: StateFlow<ToastMessage?> = _current.asStateFlow()

    private val _instantDismissals = MutableStateFlow(0)

    /** Counts [dismissNow] calls: the window host drops its node (no exit motion) when this changes. */
    val instantDismissals: StateFlow<Int> = _instantDismissals.asStateFlow()

    /**
     * The system accessibility timeout for a base time (`AccessibilityManager.getRecommendedTimeoutMillis`); the
     * shell sets it. Its result only ever lengthens the time.
     */
    var recommendedTimeout: (baseMs: Long, hasAction: Boolean) -> Long = { base, _ -> base }

    /** The project of the screen on top (set by the shell): a toast without its own key is tagged with it. */
    var contextKey: () -> Key? = { null }

    private var nextId = 0L
    private var timer: Job? = null
    private val holds = mutableSetOf<ToastHold>()
    private var remainingMs = 0L
    private var startedAt = 0L

    /**
     * Shows [text] with `되돌리기` by default; pass `actionLabel = null` for a plain toast. The kind follows the
     * action: [NoticeKind.ACTION] with [onAction], else [NoticeKind.INFO]. It belongs to the project on top
     * ([contextKey]).
     */
    fun show(text: String, actionLabel: String? = UNDO, onAction: (() -> Unit)? = null) {
        val kind = if (onAction != null) NoticeKind.ACTION else NoticeKind.INFO
        post(text, actionLabel.takeIf { onAction != null }, onAction, kind, null)
    }

    /** [show] for a toast that acts on [projectKey] (it leaves when that project leaves the back stack). */
    fun showFor(projectKey: Key, text: String, actionLabel: String? = UNDO, onAction: (() -> Unit)? = null) {
        val kind = if (onAction != null) NoticeKind.ACTION else NoticeKind.INFO
        post(text, actionLabel.takeIf { onAction != null }, onAction, kind, projectKey)
    }

    /**
     * (0.2.3) A plain info toast shown for [baseMs] instead of the kind's time (the accessibility timeout can still
     * lengthen it). Returns its id, so the caller can tell whether it is still the one shown ([isShowing]).
     */
    fun showBrief(text: String, baseMs: Long): Long {
        post(text, null, null, NoticeKind.INFO, null, baseMs)
        return nextId
    }

    /** Whether the toast [id] is the one on screen now. */
    fun isShowing(id: Long): Boolean = _current.value?.id == id

    /** A failure that needs nothing from the user now (`IMPORT_FAILED`, `SAVE_FAILED`): 4 s, no action. */
    fun showError(text: String) = post(text, null, null, NoticeKind.ERROR, null)

    /** The time [kind] is shown for, after the accessibility timeout. */
    fun durationOf(kind: NoticeKind, hasAction: Boolean): Long {
        val base = durationMs ?: NoticeRules.durationMs(kind)
        return maxOf(base, recommendedTimeout(base, hasAction))
    }

    private fun post(text: String, actionLabel: String?, onAction: (() -> Unit)?, kind: NoticeKind, projectKey: Key?, baseMs: Long? = null) {
        val message = ToastMessage(++nextId, text, actionLabel, onAction, kind, projectKey ?: contextKey())
        timer?.cancel()
        timer = null
        _current.value = message
        remainingMs = if (baseMs != null) maxOf(baseMs, recommendedTimeout(baseMs, onAction != null)) else durationOf(kind, onAction != null)
        startTimer()
    }

    private fun startTimer() {
        val message = _current.value ?: return
        if (holds.isNotEmpty() || timer != null) return
        startedAt = now()
        val wait = remainingMs
        timer = scope.launch {
            delay(wait)
            if (_current.value?.id == message.id) {
                _current.value = null
                timer = null
            }
        }
    }

    /** Stops the clock for [hold] (finger down, accessibility focus, app stopped); the time left is kept. */
    fun pause(hold: ToastHold) {
        if (!holds.add(hold)) return
        val running = timer ?: return
        running.cancel()
        timer = null
        remainingMs = (remainingMs - (now() - startedAt)).coerceAtLeast(0L)
    }

    /** Releases [hold]; when nothing holds the toast any more its clock runs on with the time left. */
    fun resume(hold: ToastHold) {
        if (!holds.remove(hold)) return
        if (holds.isEmpty()) startTimer()
    }

    fun dismiss() {
        timer?.cancel()
        timer = null
        _current.value = null
    }

    /** Dismisses in the same frame with no exit motion (app lock, relock, project released; DS-67). */
    fun dismissNow() {
        val shown = _current.value != null
        dismiss()
        if (shown) _instantDismissals.value += 1
    }

    /**
     * [dismissNow] when the shown toast acts on a project ([NoticeKind.ACTION], e.g. `되돌리기`) that is not in
     * [liveProjects] any more: its undo cannot apply. An info or error toast stays its time (DS-67).
     */
    fun dismissIfReleased(liveProjects: Set<Key>) {
        val shown = _current.value ?: return
        if (NoticeRules.releasedFrom(shown, liveProjects)) dismissNow()
    }

    /** The action button was tapped. */
    fun act() {
        val message = _current.value ?: return
        dismiss()
        message.onAction?.invoke()
    }

    companion object {
        const val UNDO = "되돌리기"
    }
}
