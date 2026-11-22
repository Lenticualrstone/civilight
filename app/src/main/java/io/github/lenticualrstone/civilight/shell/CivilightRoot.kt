package io.github.lenticualrstone.civilight.shell

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import io.github.lenticualrstone.civilight.ServiceLocator
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.navigation.BackStackNavigator
import io.github.lenticualrstone.civilight.navigation.HiddenProjectLock
import io.github.lenticualrstone.civilight.navigation.ProjectRoute
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.lens.ScriptLensEffect
import io.github.lenticualrstone.civilight.ui.nav.ListDetailSceneStrategy
import io.github.lenticualrstone.civilight.ui.nav.OverlayBackdrop
import io.github.lenticualrstone.civilight.ui.nav.OverlaySceneStrategy
import io.github.lenticualrstone.civilight.ui.nav.overlayBackdrop
import io.github.lenticualrstone.civilight.ui.nav.RouteMetadata
import io.github.lenticualrstone.civilight.ui.nav.rememberEntryViewModelDecorator
import io.github.lenticualrstone.civilight.ui.project.ProjectEntryFrame
import io.github.lenticualrstone.civilight.ui.scaffold.LocalSheetRegistry
import io.github.lenticualrstone.civilight.ui.scaffold.LocalToasts
import io.github.lenticualrstone.civilight.ui.scaffold.PlaceholderScreen
import io.github.lenticualrstone.civilight.ui.security.AppLockScreen
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.BiometricGate
import io.github.lenticualrstone.civilight.ui.security.CaptureGuard
import io.github.lenticualrstone.civilight.ui.security.LocalCaptureGuard
import io.github.lenticualrstone.civilight.ui.security.LockRules
import io.github.lenticualrstone.civilight.ui.security.LockScreenMode
import io.github.lenticualrstone.civilight.ui.security.ScreenExposure
import io.github.lenticualrstone.civilight.ui.security.lockBlurAvailable
import io.github.lenticualrstone.civilight.ui.security.lockedContent
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge
import io.github.lenticualrstone.civilight.ui.window.LocalPaneSplits
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.ui.window.PaneSplitSource
import io.github.lenticualrstone.civilight.ui.window.rememberWindowLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The composition root: theme from settings, the back stack (restored after process death by
 * Navigation3's saved state), feature entries, scene strategies (list | detail, glass overlays) and
 * the app lock overlay.
 */
@Composable
fun CivilightRoot(
    locator: ServiceLocator,
    activity: Activity,
    startRoutes: List<Route>,
    biometric: BiometricGate,
    capture: CaptureGuard,
    pendingRoute: Route?,
    onPendingRouteHandled: () -> Unit,
    splashDone: State<Boolean>? = null,
) {
    val settings by locator.settings.collectAsState()
    // S16: the theme reads the system animator scale live (ContentObserver) and folds scale 0 into
    // reduced motion; the app passes only the user's own choice.
    CivilightTheme(
        themeMode = ThemeBridge.themeMode(settings.theme),
        textScale = ThemeBridge.textScale(settings),
        glassMode = ThemeBridge.glassMode(settings),
        reducedMotion = ThemeBridge.reducedMotion(settings.animation, systemAnimationsOff = false),
    ) {
        val motion = LocalMotion.current
        val motionState = rememberUpdatedState(motion)
        val backStack = rememberNavBackStack(*startRoutes.toTypedArray())
        val navigator = remember(backStack) { BackStackNavigator(backStack) { activity.finish() } }
        val layout = rememberWindowLayout(settings.unfoldedLayout)
        val layoutState = rememberUpdatedState(layout)
        val deps = remember(navigator, biometric, capture) { ShellDeps(locator, navigator, biometric, capture, layoutState) }
        val provider = remember(deps) { shellEntryProvider(deps) }
        val backdrop = remember { OverlayBackdrop() }
        val overlays = remember(backdrop) { OverlaySceneStrategy(backdrop) { motionState.value } }
        // A new strategy when the pane mode changes, so Nav3 recomputes the scenes on fold / unfold.
        val listDetail = remember(layout.twoPanes) { ListDetailSceneStrategy { layoutState.value.twoPanes } }
        val paneSplits = remember(locator) {
            PaneSplitSource(locator.settings) { module, split -> locator.appScope.launch { locator.stores.settings.setPaneSplit(module, split) } }
        }

        CompositionLocalProvider(
            LocalFeatureDeps provides deps,
            LocalWindowLayout provides layout,
            LocalCaptureGuard provides capture,
            LocalToasts provides deps.toasts,
            LocalSheetRegistry provides deps.sheets,
            LocalBackHandler provides { enabled, onBack -> BackHandler(enabled, onBack) },
            LocalPaneSplits provides paneSplits,
        ) {
            // A file opened from outside waits until the app lock is passed (LIB-08).
            val unlock by deps.unlock.state.collectAsState()
            LaunchedEffect(pendingRoute, unlock.appLocked) {
                if (pendingRoute != null && !unlock.appLocked) {
                    navigator.push(pendingRoute)
                    onPendingRouteHandled()
                }
            }
            // Features' start-up hooks (LRC-09 recovery) run once per process after the app lock.
            LaunchedEffect(deps, unlock.appLocked) {
                if (!unlock.appLocked) {
                    deps.startup.runPending(deps) { id, e -> Log.w("Civilight", "start-up hook $id failed", e) }
                        .forEach { navigator.push(it) }
                }
            }
            // `연 지 5분 뒤` is checked on the clock while in the foreground (LIB-33).
            LaunchedEffect(deps) {
                while (true) {
                    delay(RELOCK_TICK_MS)
                    deps.unlock.tick()
                }
            }

            val top = backStack.lastOrNull() as? Route
            val lockMode = if (unlock.appLocked) {
                LockRules.lockScreens(exposureOf(top, deps), appLockDue = true, hiddenRelockDue = false).first()
            } else {
                null
            }
            // S9 / D5: the lock appears at once (protected content never lingers in a fade); only unlocking
            // animates: the lock fades out on `exit` while the content's blur and dim ramp back.
            val lockAmount = remember { Animatable(if (lockMode != null) 1f else 0f) }
            LaunchedEffect(lockMode != null) {
                if (lockMode != null) lockAmount.snapTo(1f) else lockAmount.animateTo(0f, motion.spec(CivilightMotion.exit))
            }
            val shownLock = rememberLastNonNull(lockMode)
            val lockLevel = { if (lockMode != null) 1f else lockAmount.value }
            val showContent = lockMode == null || (lockMode == LockScreenMode.BLURRED_CONTENT && lockBlurAvailable())
            val entryViewModels = rememberEntryViewModelDecorator<NavKey>()
            val saveable = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
            val rise = rememberSplashRise(splashDone, motion)
            Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg)) {
                if (showContent) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(rise)
                            .lockedContent(lockLevel, locked = lockMode != null),
                    ) {
                        NavDisplay(
                            backStack = backStack,
                            modifier = Modifier.overlayBackdrop(backdrop) { motionState.value.reduced },
                            onBack = { navigator.pop() },
                            entryDecorators = listOf(saveable, entryViewModels),
                            sceneStrategies = listOf(overlays, listDetail),
                            transitionSpec = { sceneTransition(motion, forward = true) },
                            popTransitionSpec = { sceneTransition(motion, forward = false) },
                            predictivePopTransitionSpec = { sceneTransition(motion, forward = false) },
                            entryProvider = provider,
                        )
                    }
                }
                if (lockMode != null) {
                    AppLock(lockMode, deps, onBack = { activity.moveTaskToBack(true) })
                } else if (shownLock != null && lockAmount.value > 0f) {
                    // The unlock fade: the same lock screen, no longer taking back or touches.
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = lockAmount.value }) {
                        AppLockScreen(shownLock, onBiometric = {}, onDevicePin = {}, interactive = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLock(mode: LockScreenMode, deps: FeatureDeps, onBack: () -> Unit) {
    // Back never reaches the screens under the lock; it leaves the app instead.
    BackHandler(enabled = true, onBack = onBack)
    val scope = rememberCoroutineScope()
    fun ask(credentialOnly: Boolean) {
        scope.launch {
            // Cancel keeps the lock up (LIB-35). A device without any secure lock screen cannot
            // authenticate at all (library spec §6 Q19): the lock lets the user in instead of locking
            // them out for good.
            when (deps.biometric.authenticate(AuthRequest(AuthCopy.APP, credentialOnly = credentialOnly))) {
                AuthResult.SUCCESS, AuthResult.UNAVAILABLE -> deps.unlock.appUnlocked()
                AuthResult.CANCELLED, AuthResult.FAILED -> Unit
            }
        }
    }
    AppLockScreen(mode, onBiometric = { ask(false) }, onDevicePin = { ask(true) })
}

/**
 * What the top screen exposes for the lock decision (LIB-34): a hidden project (from the 서재
 * index) or a capture-blocked one (from its open document). A project route whose document is not
 * open yet counts as capture-blocked, so no content is ever shown under the lock by mistake.
 */
private fun exposureOf(route: Route?, deps: FeatureDeps): ScreenExposure {
    val key = (route as? ProjectRoute)?.projectKey ?: return ScreenExposure.None
    val hidden = deps.stores.library.state.value.entry(key)?.hidden ?: false
    val doc = deps.projects.peekDoc(key) ?: return ScreenExposure(hidden = hidden, captureBlock = true)
    val p = doc.project.protections
    return ScreenExposure(hidden = hidden || p.hidden, captureBlock = p.captureBlock)
}

/**
 * Every route's entry: the feature's (or the placeholder), with the route in its metadata, project
 * routes wrapped in [ProjectEntryFrame] (open store, protection, hidden lock) and the script in the
 * STO-16 lens hook.
 */
private fun shellEntryProvider(deps: FeatureDeps): (NavKey) -> NavEntry<NavKey> {
    val features = entryProvider<NavKey>(fallback = { key -> placeholderEntry(key, deps) }) {
        FeatureRegistry.registerAll(this, deps)
    }
    return { key ->
        val entry = features(key)
        val projectKey = (key as? ProjectRoute)?.projectKey?.takeIf { key !is HiddenProjectLock }
        NavEntry(key, entry.contentKey, entry.metadata + RouteMetadata.of(key)) {
            if (projectKey == null) {
                entry.Content()
            } else {
                ProjectEntryFrame(projectKey, deps) {
                    if (key is Script) ScriptLensEffect()
                    entry.Content()
                }
            }
        }
    }
}

private fun placeholderEntry(key: NavKey, deps: FeatureDeps): NavEntry<NavKey> = NavEntry(key) {
    val back: () -> Unit = { deps.navigator.pop() }
    PlaceholderScreen(key, onBack = back.takeIf { deps.navigator.backStack.size > 1 })
}

private const val RELOCK_TICK_MS = 30_000L

/**
 * Scene transitions (S1, CNV-37) from the shared helpers: push slides an eighth in on `enter` with a fade
 * while the old screen fades on `exit`; pop reverses. Reduced: fades only. A fold or unfold (the same top
 * entry in a different scene) is not navigation: the new scene fades in on `foldOpen` over the old one.
 */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.sceneTransition(motion: Motion, forward: Boolean): ContentTransform {
    val sameTop = initialState.entries.lastOrNull()?.contentKey == targetState.entries.lastOrNull()?.contentKey
    if (sameTop && initialState::class != targetState::class) {
        return fadeIn(motion.spec(CivilightMotion.foldOpen)) togetherWith ExitTransition.KeepUntilTransitionsFinished
    }
    return motion.push(forward)
}

/**
 * CNV-38 app phase, the destination half: while the system splash is up the content waits, then it rises
 * 14 dp and fades in on `enter` as the splash mark leaves (`splashDone`). Reduced: a fade.
 */
@Composable
private fun rememberSplashRise(splashDone: State<Boolean>?, motion: Motion): Modifier {
    val progress = remember { Animatable(if (splashDone == null || splashDone.value) 1f else 0f) }
    val done = splashDone?.value ?: true
    LaunchedEffect(done) {
        if (done && progress.value < 1f) progress.animateTo(1f, motion.spec(CivilightMotion.enter))
    }
    val risePx = with(LocalDensity.current) { SPLASH_RISE.toPx() }
    val reduced = motion.reduced
    return Modifier.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = if (reduced) 0f else (1f - CivilightMotion.enter.easing.transform(p)) * risePx
    }
}

private val SPLASH_RISE = 14.dp
