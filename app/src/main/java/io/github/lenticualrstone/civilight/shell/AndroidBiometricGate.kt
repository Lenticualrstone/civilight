package io.github.lenticualrstone.civilight.shell

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.github.lenticualrstone.civilight.ui.security.AuthAvailability
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.BiometricGate
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [BiometricGate] over `androidx.biometric` (LIB-32 `ED`): API 30+ `BIOMETRIC_STRONG or
 * DEVICE_CREDENTIAL`; API 29 `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` (STRONG + credential is not
 * supported there). With the device credential allowed the system supplies the PIN option, so no
 * negative button is set. `기기 PIN 사용` asks for the credential alone where the platform allows it.
 */
class AndroidBiometricGate(private val activity: FragmentActivity) : BiometricGate {
    private val manager = BiometricManager.from(activity)

    /**
     * True while a prompt is up. The device-credential screen can be a separate activity, which
     * stops ours; that must not count as leaving the app (no app lock, no re-lock of what the
     * prompt is opening).
     */
    @Volatile
    var isPrompting: Boolean = false
        private set

    private val biometricWithCredential: Int =
        if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** API 30+ can ask for the credential alone; API 29 cannot, so the combined prompt is used. */
    private val credentialOnly: Int =
        if (Build.VERSION.SDK_INT >= 30) DEVICE_CREDENTIAL else biometricWithCredential

    override fun availability(): AuthAvailability {
        val biometric = if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_STRONG else BIOMETRIC_WEAK
        return when {
            manager.canAuthenticate(biometric) == BiometricManager.BIOMETRIC_SUCCESS -> AuthAvailability.BIOMETRIC
            manager.canAuthenticate(biometricWithCredential) == BiometricManager.BIOMETRIC_SUCCESS -> AuthAvailability.CREDENTIAL_ONLY
            else -> AuthAvailability.NONE
        }
    }

    override suspend fun authenticate(request: AuthRequest): AuthResult {
        if (availability() == AuthAvailability.NONE) return AuthResult.UNAVAILABLE
        val authenticators = if (request.credentialOnly) credentialOnly else biometricWithCredential
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(request.title)
            .apply { request.subtitle?.let(::setSubtitle) }
            .setAllowedAuthenticators(authenticators)
            .build()
        isPrompting = true
        try {
            return prompt(info)
        } finally {
            isPrompting = false
        }
    }

    private suspend fun prompt(info: BiometricPrompt.PromptInfo): AuthResult =
        suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(AuthResult.SUCCESS)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val outcome = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED,
                            -> AuthResult.CANCELLED
                            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                            BiometricPrompt.ERROR_HW_NOT_PRESENT,
                            BiometricPrompt.ERROR_HW_UNAVAILABLE,
                            -> AuthResult.UNAVAILABLE
                            else -> AuthResult.FAILED
                        }
                        if (continuation.isActive) continuation.resume(outcome)
                    }

                    // A single failed try: the system prompt stays up and retries by itself.
                    override fun onAuthenticationFailed() = Unit
                },
            )
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(info)
        }
}
