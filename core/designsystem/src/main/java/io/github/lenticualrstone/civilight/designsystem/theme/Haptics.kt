package io.github.lenticualrstone.civilight.designsystem.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** DS-15: the only two haptics (`틱`, `확인`). */
enum class HapticToken { TICK, CONFIRM }

/** The platform constant for [token] on [apiLevel] (DS-15 proposed mapping). */
fun hapticConstant(token: HapticToken, apiLevel: Int): Int = when (token) {
    HapticToken.TICK -> if (apiLevel >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
    HapticToken.CONFIRM -> if (apiLevel >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
}

/** Plays haptic tokens through the view, respecting the system haptic setting (default flags). */
@Stable
class CivilightHaptics internal constructor(private val view: View) {
    fun perform(token: HapticToken) {
        view.performHapticFeedback(hapticConstant(token, Build.VERSION.SDK_INT))
    }
}

@Composable
fun rememberHaptics(): CivilightHaptics {
    val view = LocalView.current
    return remember(view) { CivilightHaptics(view) }
}
