package io.github.lenticualrstone.civilight.ui.security

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.AppMark
import io.github.lenticualrstone.civilight.designsystem.icon.BiometricButton
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** Lock screen copy (LIB-34, LIB-35; boards `AppLock`, `RHLockMark`). */
object LockCopy {
    const val APP_TITLE = "Civilight"
    const val HIDDEN_TITLE = "숨긴 프로젝트"
    const val LOCKED = "잠김 · 지문으로 열기"
    const val DEVICE_PIN = "기기 PIN 사용"
    const val PIN = "PIN 사용"
    const val TO_LIBRARY = "서재로"
    const val FINGERPRINT = "지문으로 열기"
}

/**
 * App lock (LIB-35) in the form [mode] gives. In [LockScreenMode.BLURRED_CONTENT] on API 31+ this
 * draws no background: the shell keeps the last screen composed underneath, blurred 14 dp at 60 %
 * (`AppLock`). In [LockScreenMode.MARK_ONLY] (and below API 31, where there is no blur) it is opaque
 * and nothing is composed underneath. It always takes every touch.
 */
@Composable
fun AppLockScreen(mode: LockScreenMode, onBiometric: () -> Unit, onDevicePin: () -> Unit, interactive: Boolean = true) {
    val colors = CivilightTheme.colors
    val see = mode == LockScreenMode.BLURRED_CONTENT && lockBlurAvailable()
    Box(
        Modifier
            .fillMaxSize()
            .then(if (see) Modifier.background(colors.shade) else Modifier.background(colors.bg))
            .then(
                if (interactive) {
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                } else {
                    Modifier.clearAndSetSemantics { }
                },
            ),
    ) {
        LockBody(
            title = LockCopy.APP_TITLE,
            markSize = 96,
            onBiometric = onBiometric,
        ) {
            TextAction(LockCopy.DEVICE_PIN, onDevicePin)
        }
    }
}

/** Whether the last screen can be shown blurred under the app lock (API 31+ `RenderEffect`). */
fun lockBlurAvailable(): Boolean = Build.VERSION.SDK_INT >= 31

/** The blur the shell applies to the screen under [AppLockScreen] (`AppLock`: 14 px, 60 %). */
fun Modifier.lockedContent(): Modifier = this.blur(14.dp).graphicsLayer { alpha = 0.6f }.clearAndSetSemantics { }

/**
 * [lockedContent] at a [level] read in the draw phase (1 = locked look, 0 = clear): the unlock ramps the
 * blur 14 → 0 dp and the dim .6 → 1 (S9). Content is hidden from accessibility while [locked].
 */
fun Modifier.lockedContent(level: () -> Float, locked: Boolean): Modifier = this
    .graphicsLayer {
        val l = level().coerceIn(0f, 1f)
        alpha = 1f - (1f - LOCKED_ALPHA) * l
        val r = LOCKED_BLUR.toPx() * l
        renderEffect = if (r > 0f && Build.VERSION.SDK_INT >= 31) BlurEffect(r, r, TileMode.Decal) else null
    }
    .then(if (locked) Modifier.clearAndSetSemantics { } else Modifier)

private const val LOCKED_ALPHA = 0.6f
private val LOCKED_BLUR = 14.dp

/** Hidden-project lock (LIB-34 `RHLockMark`): plain `bg`, mark 72, `PIN 사용` · `서재로`. */
@Composable
fun HiddenProjectLockScreen(onBiometric: () -> Unit, onPin: () -> Unit, onLibrary: () -> Unit, interactive: Boolean = true) {
    Box(
        Modifier
            .fillMaxSize()
            .background(CivilightTheme.colors.bg)
            .then(
                if (interactive) {
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                } else {
                    Modifier.clearAndSetSemantics { }
                },
            ),
    ) {
        LockBody(title = LockCopy.HIDDEN_TITLE, markSize = 72, onBiometric = onBiometric) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TextAction(LockCopy.PIN, onPin)
                TextAction(LockCopy.TO_LIBRARY, onLibrary)
            }
        }
    }
}

@Composable
private fun LockBody(title: String, markSize: Int, onBiometric: () -> Unit, actions: @Composable () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppMark(size = markSize.dp)
        Spacer(Modifier.height(20.dp))
        CivilightText(title, type.stateTitle, color = colors.ink, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        CivilightText(LockCopy.LOCKED, type.small, color = colors.ink2, maxLines = 1)
        Spacer(Modifier.height(28.dp))
        BiometricButton(onClick = onBiometric, contentDescription = LockCopy.FINGERPRINT)
        Spacer(Modifier.height(20.dp))
        actions()
    }
}
