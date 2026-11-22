package io.github.lenticualrstone.civilight.ui.scaffold

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One toast; [actionLabel] null = no action (first-use tips, LIB-20). */
data class ToastMessage(
    val id: Long,
    val text: String,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

/**
 * The app's single toast host (LIB-40): one toast at a time, a new one replaces the old, shown for
 * [durationMs] (`ED` 6 s); tapping the action runs it and dismisses. Screens draw [current] above
 * their bottom bar (`CivilightScreen` does this).
 */
class ToastController(private val scope: CoroutineScope, private val durationMs: Long = DEFAULT_DURATION_MS) {
    private val _current = MutableStateFlow<ToastMessage?>(null)
    val current: StateFlow<ToastMessage?> = _current.asStateFlow()
    private var nextId = 0L
    private var timer: Job? = null

    /** Shows [text] with `되돌리기` by default; pass `actionLabel = null` for a plain toast. */
    fun show(text: String, actionLabel: String? = UNDO, onAction: (() -> Unit)? = null) {
        val message = ToastMessage(++nextId, text, actionLabel.takeIf { onAction != null }, onAction)
        _current.value = message
        timer?.cancel()
        timer = scope.launch {
            delay(durationMs)
            if (_current.value?.id == message.id) _current.value = null
        }
    }

    fun dismiss() {
        timer?.cancel()
        _current.value = null
    }

    /** The action button was tapped. */
    fun act() {
        val message = _current.value ?: return
        dismiss()
        message.onAction?.invoke()
    }

    companion object {
        const val UNDO = "되돌리기"
        const val DEFAULT_DURATION_MS = 6_000L
    }
}
