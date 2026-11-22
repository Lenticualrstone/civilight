package io.github.lenticualrstone.civilight.ui.security

/**
 * Authentication for every gate of rule 19 (LIB-32 hidden group, LIB-35 app lock, LIB-36 hidden
 * items, unhide, trash restore of hidden things, export of hidden scenes). Implemented by the app
 * with `androidx.biometric.BiometricPrompt`; device PIN / pattern / password is always the fallback.
 */
interface BiometricGate {
    /** Shows the system prompt and waits for the outcome. Must be called on the main thread. */
    suspend fun authenticate(request: AuthRequest): AuthResult

    /** Whether the device can authenticate at all (LIB-37, §6 Q19 when it cannot). */
    fun availability(): AuthAvailability
}

/**
 * One prompt. [title] is the system prompt's title (`숨긴 프로젝트 보기` for LIB-32). With
 * [credentialOnly] the device PIN / pattern / password is asked directly (`기기 PIN 사용`).
 */
data class AuthRequest(
    val title: String,
    val subtitle: String? = null,
    val credentialOnly: Boolean = false,
)

enum class AuthResult {
    SUCCESS,

    /** Cancelled by the user or the system: the action is aborted, nothing changes, no toast (LIB-37). */
    CANCELLED,

    /** Rejected after the system's own retries and lockout handling. Treated like [CANCELLED]. */
    FAILED,

    /** No secure lock screen; see [AuthAvailability.NONE]. */
    UNAVAILABLE,
}

enum class AuthAvailability {
    /** Biometrics enrolled (device credential also offered). */
    BIOMETRIC,

    /** No usable biometrics but a PIN / pattern / password exists: the credential prompt is used directly. */
    CREDENTIAL_ONLY,

    /** No secure lock screen (library spec §6 Q19). */
    NONE,
}

/** Prompt titles fixed by the specs. */
object AuthCopy {
    /** LIB-32 system prompt title. */
    const val HIDDEN_PROJECTS = "숨긴 프로젝트 보기"

    /** App lock prompt (LIB-35): the sources give no prompt title; the lock screen's own title is used (`ED`). */
    const val APP = "Civilight"
}

/** A gate that always reports [AuthResult.UNAVAILABLE] (previews, tests). */
object UnavailableBiometricGate : BiometricGate {
    override suspend fun authenticate(request: AuthRequest): AuthResult = AuthResult.UNAVAILABLE
    override fun availability(): AuthAvailability = AuthAvailability.NONE
}
