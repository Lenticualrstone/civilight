package io.github.lenticualrstone.civilight

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.fragment.app.FragmentActivity
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.navigation.OpenFile
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.RouteCodec
import io.github.lenticualrstone.civilight.shell.AndroidBiometricGate
import io.github.lenticualrstone.civilight.shell.CivilightRoot
import io.github.lenticualrstone.civilight.ui.security.ActivityWindowProtector
import io.github.lenticualrstone.civilight.ui.security.CaptureGuard
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The single activity. A [FragmentActivity] because `BiometricPrompt` needs one. Splash (LIB-08,
 * CNV-38) stays up until the first back stack is known; stores are flushed on pause; lifecycle
 * events drive the app lock and the hidden re-lock (LIB-33, LIB-35).
 */
class MainActivity : FragmentActivity() {
    private val pendingRoute = mutableStateOf<Route?>(null)

    /** CNV-38: false while the system splash is up; the content rises in when it turns true. */
    private val splashDone = mutableStateOf(true)
    private var wentToBackground = false
    private lateinit var biometric: AndroidBiometricGate

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val locator = locator
        // The process may have started on an earlier activity (e.g. during onboarding); start from today's state.
        locator.refreshStartForNewActivity(restored = savedInstanceState != null)
        splash.setKeepOnScreenCondition { locator.start.value == null }
        splash.setOnExitAnimationListener(::animateSplashExit)
        // Only a fresh start shows the splash; a recreated activity draws at once. The content also shows if
        // the system skips the exit callback, after the mark's own exit time.
        if (savedInstanceState == null) {
            splashDone.value = false
            window.decorView.postDelayed({ splashDone.value = true }, SPLASH_FALLBACK_MS)
        }
        enableEdgeToEdge()

        val capture = CaptureGuard(ActivityWindowProtector(this))
        biometric = AndroidBiometricGate(this)
        if (savedInstanceState == null) pendingRoute.value = routeOf(intent)

        setContent {
            val start by locator.start.collectAsState()
            val routes = start
            if (routes != null) {
                CivilightRoot(
                    locator = locator,
                    activity = this,
                    startRoutes = routes,
                    biometric = biometric,
                    capture = capture,
                    pendingRoute = pendingRoute.value,
                    onPendingRouteHandled = { pendingRoute.value = null },
                    splashDone = splashDone,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeOf(intent)?.let { pendingRoute.value = it }
    }

    override fun onStart() {
        super.onStart()
        if (wentToBackground) {
            wentToBackground = false
            locator.unlock.onForeground()
        }
    }

    override fun onPause() {
        super.onPause()
        val locator = locator
        locator.appScope.launch { runCatching { locator.stores.flushAll() } }
    }

    override fun onStop() {
        super.onStop()
        // The device-credential prompt can stop this activity; that is not leaving the app.
        if (!isChangingConfigurations && !biometric.isPrompting) {
            wentToBackground = true
            locator.unlock.onBackground()
        }
    }

    /**
     * CNV-38 app phase: the mark moves up, shrinks to .42 and fades on `enterLong` with the `enter` curve
     * while the content rises 14 dp and fades in on `enter` (CivilightRoot); reduced motion: a 120 ms fade.
     * Durations and the curve come from the motion tokens (S15); the system animator scale applies to both.
     */
    private fun animateSplashExit(provider: SplashScreenViewProvider) {
        val settings = locator.settings.value
        val reduced = ThemeBridge.reducedMotion(settings.animation, ThemeBridge.systemAnimationsOff(this))
        val root: View = provider.view
        splashDone.value = true
        if (reduced) {
            root.animate().alpha(0f).setDuration(CivilightMotion.reducedFadeMs.toLong()).withEndAction { provider.remove() }.start()
            return
        }
        val curve = CivilightMotion.enterCurve
        val easing = PathInterpolator(curve.x1, curve.y1, curve.x2, curve.y2)
        val ms = CivilightMotion.enterLong.durationMs.toLong()
        runCatching { provider.iconView }.getOrNull()?.animate()
            ?.translationY(-MARK_RISE_DP * resources.displayMetrics.density)
            ?.scaleX(MARK_SCALE)?.scaleY(MARK_SCALE)?.alpha(0f)
            ?.setInterpolator(easing)?.setDuration(ms)?.start()
        root.animate().alpha(0f).setInterpolator(easing).setDuration(ms).withEndAction { provider.remove() }.start()
    }

    /**
     * Routes asked for from outside: a `.civilight` file to open (LIB-52), or a route from the
     * app's own surfaces (widget, tile, notification) in [EXTRA_ROUTE] as [RouteCodec] text. Only
     * the shared route classes are accepted.
     */
    private fun routeOf(intent: Intent?): Route? {
        intent ?: return null
        intent.getStringExtra(EXTRA_ROUTE)?.let { text ->
            val type = runCatching { Json.parseToJsonElement(text).jsonObject["type"]?.jsonPrimitive?.content }.getOrNull()
            if (type != null && type.startsWith(ROUTE_PACKAGE)) return RouteCodec.decodeOrNull(text) as? Route
            return null
        }
        if (intent.action == Intent.ACTION_VIEW) intent.data?.let { return OpenFile(it.toString()) }
        return null
    }

    companion object {
        /** Intent extra holding a route as [RouteCodec] text. */
        const val EXTRA_ROUTE = "io.github.lenticualrstone.civilight.ROUTE"

        private const val ROUTE_PACKAGE = "io.github.lenticualrstone.civilight.navigation."
        /** Content shows even when no exit callback comes (the CNV-38 total of 1 s). */
        private const val SPLASH_FALLBACK_MS = 1_000L
        private const val MARK_RISE_DP = 300f
        private const val MARK_SCALE = 0.42f
    }
}
