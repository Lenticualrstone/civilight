package io.github.lenticualrstone.civilight.feature.recorder.outside

import android.app.Activity
import android.app.KeyguardManager

/**
 * Device unlock for the lock-screen capture (LRC-05, `[PROPOSED]` §6 Q7): the link picker lists
 * project and item names, so on the lock screen it asks for the device unlock first, and with
 * `잠금 화면에서 녹음` off the tile asks before recording. Cancel keeps the recording screen.
 */
object DeviceUnlock {
    /** Asks the keyguard to dismiss; [onResult] is true when the device is (or already was) unlocked. */
    fun request(activity: Activity, onResult: (Boolean) -> Unit) {
        val keyguard = activity.getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardLocked) return onResult(true)
        keyguard.requestDismissKeyguard(
            activity,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = onResult(true)

                override fun onDismissCancelled() = onResult(false)

                override fun onDismissError() = onResult(false)
            },
        )
    }
}
