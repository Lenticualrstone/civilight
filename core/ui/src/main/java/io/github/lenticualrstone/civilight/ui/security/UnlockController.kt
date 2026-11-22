package io.github.lenticualrstone.civilight.ui.security

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What is unlocked right now. Memory only: a new process starts fully locked (never saved).
 *
 * - [appLocked]: the app lock (LIB-35) is up; due at start and after every return from background.
 * - [hiddenGroupUnlockedAt]: the hidden-projects group is open since then (LIB-32); null = locked.
 * - [itemsUnlockedAt]: hidden items of a project are open since then (LIB-36), by project key.
 * - [recordsUnlockedAt]: hidden recorder records are open since then (LRC-41).
 */
data class UnlockState(
    val appLocked: Boolean = false,
    val hiddenGroupUnlockedAt: Long? = null,
    val itemsUnlockedAt: Map<Key, Long> = emptyMap(),
    val recordsUnlockedAt: Long? = null,
    /** Events since the earliest open unlock; cleared when everything is locked again. */
    val events: Set<RelockEvent> = emptySet(),
) {
    val hiddenGroupOpen: Boolean get() = hiddenGroupUnlockedAt != null

    fun itemsOpen(projectKey: Key): Boolean = projectKey in itemsUnlockedAt

    val recordsOpen: Boolean get() = recordsUnlockedAt != null

    val anythingOpen: Boolean get() = hiddenGroupUnlockedAt != null || itemsUnlockedAt.isNotEmpty() || recordsUnlockedAt != null
}

/**
 * Holds [UnlockState] and applies the re-lock policy (LIB-33) to lifecycle events. The shell calls
 * [onStart], [onBackground], [onScreenOff], [onForeground] and [tick]; features call the unlock
 * functions after a successful [BiometricGate] prompt and [relockNow] for `다시 잠그기`.
 */
class UnlockController(
    private val now: () -> Long,
    private val policy: () -> RelockPolicy,
    private val appLockEnabled: () -> Boolean,
) {
    private val _state = MutableStateFlow(UnlockState())
    val state: StateFlow<UnlockState> = _state.asStateFlow()

    /** Process start: the app lock is up when the setting is on ([appLockOn] once settings are read). */
    fun onStart(appLockOn: Boolean = appLockEnabled()) = _state.update { it.copy(appLocked = appLockOn) }

    /** The app went to the background (another app, home, recents). */
    fun onBackground() {
        _state.update { s -> s.copy(appLocked = s.appLocked || appLockEnabled()) }
        record(RelockEvent.BACKGROUNDED)
    }

    fun onScreenOff() {
        _state.update { s -> s.copy(appLocked = s.appLocked || appLockEnabled()) }
        record(RelockEvent.SCREEN_OFF)
    }

    /** Back in the foreground: time-based re-lock is checked. */
    fun onForeground() = apply()

    /** Periodic check while in the foreground (`연 지 5분 뒤`). */
    fun tick() = apply()

    /** `다시 잠그기` (LIB-33, LRC-41): everything locks at once. */
    fun relockNow() = record(RelockEvent.RELOCK_PRESSED)

    /** The app lock was passed. Never opens hidden projects (LIB-35). */
    fun appUnlocked() = _state.update { it.copy(appLocked = false) }

    /** Turning the app lock setting off (after authentication) drops a pending lock. */
    fun appLockDisabled() = appUnlocked()

    fun unlockHiddenGroup() = open { it.copy(hiddenGroupUnlockedAt = now()) }

    fun unlockItems(projectKey: Key) = open { it.copy(itemsUnlockedAt = it.itemsUnlockedAt + (projectKey to now())) }

    fun unlockRecords() = open { it.copy(recordsUnlockedAt = now()) }

    /** Locks the hidden group right away (auth cancelled on the hidden lock, L-05). */
    fun lockHiddenGroup() = _state.update { tidy(it.copy(hiddenGroupUnlockedAt = null)) }

    private fun open(change: (UnlockState) -> UnlockState) = _state.update { s ->
        change(if (s.anythingOpen) s else s.copy(events = emptySet()))
    }

    private fun record(event: RelockEvent) {
        _state.update { s -> if (s.anythingOpen) s.copy(events = s.events + event) else s }
        apply()
    }

    private fun apply() = _state.update { s ->
        val p = policy()
        val t = now()
        fun due(at: Long?) = LockRules.relockDue(p, at, s.events, t)
        tidy(
            s.copy(
                hiddenGroupUnlockedAt = s.hiddenGroupUnlockedAt.takeUnless { due(it) },
                itemsUnlockedAt = s.itemsUnlockedAt.filterValues { !due(it) },
                recordsUnlockedAt = s.recordsUnlockedAt.takeUnless { due(it) },
            ),
        )
    }

    private fun tidy(s: UnlockState): UnlockState = if (s.anythingOpen) s else s.copy(events = emptySet())
}
