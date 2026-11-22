package io.github.lenticualrstone.civilight.ui.security

import io.github.lenticualrstone.civilight.model.app.RelockPolicy

/** The app-drawn lock screens (LIB-34). */
enum class LockScreenMode {
    /** `AppLock`: the last screen blurred under glass, mark, `Civilight`, `잠김 · 지문으로 열기`, `기기 PIN 사용`. */
    BLURRED_CONTENT,

    /** App lock without content behind it (hidden or capture-blocked project on screen): mark and `Civilight` only. */
    MARK_ONLY,

    /** `RHLockMark`: `숨긴 프로젝트`, `잠김 · 지문으로 열기`, `PIN 사용` · `서재로`. */
    MARK_ONLY_HIDDEN,
}

/** What is known about the project on screen when a lock is due. */
data class ScreenExposure(val hidden: Boolean = false, val captureBlock: Boolean = false) {
    companion object {
        /** No project content on screen (서재, settings …). */
        val None = ScreenExposure()
    }
}

/** Something that happened since the hidden group was opened (LIB-33). */
enum class RelockEvent { BACKGROUNDED, SCREEN_OFF, RELOCK_PRESSED }

/**
 * Lock decisions of rule 19 §4 (LIB-33, LIB-34, LIB-35) as pure functions; the shell feeds them the
 * policy, the clock and the lifecycle events. Test cases L-01 … L-06 and RL-01 … RL-07 of the
 * library spec §4.3.
 */
object LockRules {
    const val FIVE_MINUTES_MS: Long = 5 * 60 * 1000L

    /**
     * The lock screens to show, in order, for the screen [onScreen] when the app lock is due
     * ([appLockDue]) and/or the hidden group's re-lock is due ([hiddenRelockDue]). App lock and
     * hidden lock are separate authentications (L-03).
     */
    fun lockScreens(onScreen: ScreenExposure, appLockDue: Boolean, hiddenRelockDue: Boolean): List<LockScreenMode> = buildList {
        if (appLockDue) add(if (takeBlurSnapshot(onScreen)) LockScreenMode.BLURRED_CONTENT else LockScreenMode.MARK_ONLY)
        if (onScreen.hidden && hiddenRelockDue) add(LockScreenMode.MARK_ONLY_HIDDEN)
    }

    /** Whether the lock may show the blurred last screen (L-06: never for hidden or capture-blocked projects). */
    fun takeBlurSnapshot(onScreen: ScreenExposure): Boolean = !onScreen.hidden && !onScreen.captureBlock

    /**
     * Whether the hidden group (and hidden items) must be locked again (RL-01 … RL-07). [unlockedAt]
     * null = already locked. `연 지 5분 뒤` counts on the clock even in the foreground.
     */
    fun relockDue(policy: RelockPolicy, unlockedAt: Long?, events: Collection<RelockEvent>, now: Long): Boolean {
        if (unlockedAt == null) return false
        if (RelockEvent.RELOCK_PRESSED in events) return true
        return when (policy) {
            RelockPolicy.ON_LEAVE -> RelockEvent.BACKGROUNDED in events || RelockEvent.SCREEN_OFF in events
            RelockPolicy.SCREEN_OFF_ONLY -> RelockEvent.SCREEN_OFF in events
            RelockPolicy.FIVE_MINUTES -> now - unlockedAt >= FIVE_MINUTES_MS
        }
    }
}
