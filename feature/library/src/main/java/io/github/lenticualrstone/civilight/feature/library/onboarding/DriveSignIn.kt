package io.github.lenticualrstone.civilight.feature.library.onboarding

/** The outcome of `연결` (Google sign-in + Drive authorization, LIB-15 → LIB-57). */
sealed interface DriveConnect {
    data class Connected(val accountId: String) : DriveConnect

    /** The author backed out of the system sign-in: nothing changes, nothing is shown (LIB-37). */
    data object Cancelled : DriveConnect

    /** Sign-in is not available in this build. */
    data object Unavailable : DriveConnect
}
