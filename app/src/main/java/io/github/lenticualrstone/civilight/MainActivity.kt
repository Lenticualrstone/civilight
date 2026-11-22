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
    private var wentToBackground = false
    private lateinit var biometric: AndroidBiometricGate

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val locator = locator
        splash.setKeepOnScreenCondition { locator.start.value == null }
        splash.setOnExitAnimationListener(::animateSplashExit)
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
     * CNV-38 app phase: the mark moves up, shrinks to .42 and fades while 서재 shows (≤ 1 s,
     * `cubic-bezier(.05,.7,.1,1)`); reduced motion: a 120 ms fade.
     */
    private fun animateSplashExit(provider: SplashScreenViewProvider) {
        val settings = locator.settings.value
        val reduced = ThemeBridge.reducedMotion(settings.animation, ThemeBridge.systemAnimationsOff(this))
        val root: View = provider.view
        if (reduced) {
            root.animate().alpha(0f).setDuration(REDUCED_MS).withEndAction { provider.remove() }.start()
            return
        }
        val easing = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
        runCatching { provider.iconView }.getOrNull()?.animate()
            ?.translationY(-MARK_RISE_DP * resources.displayMetrics.density)
            ?.scaleX(MARK_SCALE)?.scaleY(MARK_SCALE)?.alpha(0f)
            ?.setInterpolator(easing)?.setDuration(EXIT_MS)?.start()
        root.animate().alpha(0f).setInterpolator(easing).setDuration(EXIT_MS).withEndAction { provider.remove() }.start()
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
        private const val EXIT_MS = 450L
        private const val REDUCED_MS = 120L
        private const val MARK_RISE_DP = 300f
        private const val MARK_SCALE = 0.42f
    }
}
