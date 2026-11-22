package io.github.lenticualrstone.civilight.domain.library.lock

import io.github.lenticualrstone.civilight.model.app.RelockPolicy

/** Session events that can end an unlock (LIB-33). */
sealed interface SessionEvent {
    val at: Long

    /** The app went to the background (other app, home, recents). */
    data class Backgrounded(override val at: Long) : SessionEvent

    data class Foregrounded(override val at: Long) : SessionEvent

    data class ScreenOff(override val at: Long) : SessionEvent

    /** `다시 잠그기` pressed. */
    data class RelockPressed(override val at: Long) : SessionEvent
}

/** When hidden projects and items lock again (LIB-33, R19 §6 edge). */
object Relock {
    const val FIVE_MINUTES_MS = 5 * 60 * 1000L

    /** Whether the unlock made at [unlockedAt] has ended by [now] under [policy]. */
    fun isDue(policy: RelockPolicy, unlockedAt: Long, events: List<SessionEvent>, now: Long): Boolean {
        val since = events.filter { it.at >= unlockedAt && it.at <= now }
        if (since.any { it is SessionEvent.RelockPressed }) return true
        return when (policy) {
            RelockPolicy.ON_LEAVE -> since.any { it is SessionEvent.Backgrounded || it is SessionEvent.ScreenOff }
            RelockPolicy.SCREEN_OFF_ONLY -> since.any { it is SessionEvent.ScreenOff }
            RelockPolicy.FIVE_MINUTES -> now - unlockedAt >= FIVE_MINUTES_MS
        }
    }

    /** Right text of the open group row; only the default policy's copy is in the sources. */
    fun openGroupNote(policy: RelockPolicy): String = when (policy) {
        RelockPolicy.ON_LEAVE -> "앱을 떠나면 잠김"
        RelockPolicy.SCREEN_OFF_ONLY -> "TODO_COPY_LIB32_RELOCK_SCREEN_OFF"
        RelockPolicy.FIVE_MINUTES -> "TODO_COPY_LIB32_RELOCK_FIVE_MINUTES"
    }

    /** Radio title · sub of 설정 · 숨기기 `다시 잠그는 때` (LIB-64). */
    fun policyLabel(policy: RelockPolicy): Pair<String, String?> = when (policy) {
        RelockPolicy.ON_LEAVE -> "앱을 떠날 때" to "다른 앱 · 화면 꺼짐"
        RelockPolicy.SCREEN_OFF_ONLY -> "화면이 꺼질 때만" to "다른 앱 다녀와도 유지"
        RelockPolicy.FIVE_MINUTES -> "연 지 5분 뒤" to null
    }
}
