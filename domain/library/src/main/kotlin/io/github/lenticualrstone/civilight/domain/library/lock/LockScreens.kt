package io.github.lenticualrstone.civilight.domain.library.lock

import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile

/** Lock screen forms drawn by the app (LIB-34, R19 §4). */
enum class LockMode {
    /** App lock over the blurred last screen (`AppLock`). */
    BLURRED_CONTENT,

    /** App lock without content and without a project name. */
    MARK_ONLY,

    /** Hidden project lock: plain background, mark only (`RHLockMark`). */
    MARK_ONLY_HIDDEN,
}

/** One lock screen in the order shown; each needs its own authentication. */
data class LockScreen(val mode: LockMode, val title: String, val status: String, val buttons: List<String>) {
    companion object {
        const val STATUS = "잠김 · 지문으로 열기"

        fun appLock(blurred: Boolean) = LockScreen(
            if (blurred) LockMode.BLURRED_CONTENT else LockMode.MARK_ONLY, "Civilight", STATUS, listOf("기기 PIN 사용"),
        )

        val HIDDEN = LockScreen(LockMode.MARK_ONLY_HIDDEN, "숨긴 프로젝트", STATUS, listOf("PIN 사용", "서재로"))
    }
}

/** After a lock screen's authentication. */
sealed interface LockOutcome {
    /** Show the next lock screen, or the content when none is left. */
    data object Continue : LockOutcome

    /** Cancel or failure: go to 서재 with the hidden group locked (LIB-34, LIB-37). */
    data object GoToLibraryLocked : LockOutcome
}

/** Which lock screens to show on return (LIB-34, LIB-35). */
object LockScreens {
    /**
     * [last] = the project whose content was on screen (null for 서재 and app screens);
     * [appLock] = 앱 잠금 on; [relockDue] = the hidden re-lock is due ([Relock.isDue]).
     * App lock and hidden lock stay separate: a hidden project asks twice (L-03).
     */
    fun sequence(last: ProjectProfile?, appLock: Boolean, relockDue: Boolean): List<LockScreen> = buildList {
        val hidden = last?.hidden == true
        if (appLock) add(LockScreen.appLock(blurred = takeBlurSnapshot(last)))
        if (hidden && relockDue) add(LockScreen.HIDDEN)
    }

    /** Never pre-capture the screen for a hidden or capture-blocked project (L-06). */
    fun takeBlurSnapshot(project: ProjectProfile?): Boolean =
        project == null || (!project.hidden && !project.protections.captureBlock)

    fun afterAuth(success: Boolean): LockOutcome = if (success) LockOutcome.Continue else LockOutcome.GoToLibraryLocked
}
