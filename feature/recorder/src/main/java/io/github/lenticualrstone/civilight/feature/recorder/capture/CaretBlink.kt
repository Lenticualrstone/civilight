package io.github.lenticualrstone.civilight.feature.recorder.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlinx.coroutines.delay

/**
 * Whether the transcript caret is drawn now (DS-14 / S12, the same step blink as the DS caret): on for half
 * of [CivilightMotion.caretPeriodMs], off for the other half, waits scaled by the system animator scale. With
 * decorative loops off (reduce motion, animator scale 0) it stays solid. Read the value in a draw or layer
 * block so the blink never recomposes the transcript.
 */
@Composable
fun rememberCaretOn(): State<Boolean> {
    val motion = CivilightTheme.motion
    return produceState(true, motion.loops) {
        value = true
        if (motion.loops) {
            while (true) {
                delay(motion.delayMs(CivilightMotion.caretPeriodMs / 2))
                value = !value
            }
        }
    }
}
