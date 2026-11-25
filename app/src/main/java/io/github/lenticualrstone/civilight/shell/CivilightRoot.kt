package io.github.lenticualrstone.civilight.shell

import android.app.Activity
import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.util.Log
import android.os.SystemClock
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import io.github.lenticualrstone.civilight.ui.back.BackCopy
import io.github.lenticualrstone.civilight.ui.back.BackLayers
import io.github.lenticualrstone.civilight.ui.back.BackPolicy
import io.github.lenticualrstone.civilight.ui.back.BackSnapshot
import io.github.lenticualrstone.civilight.ui.back.BackStep
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import io.github.lenticualrstone.civilight.designsystem.component.BottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.LocalBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.LocalWindowToasts
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.feature.library.tour.ModuleTourFrame
import io.github.lenticualrstone.civilight.navigation.BackStackNavigator
import io.github.lenticualrstone.civilight.navigation.HiddenProjectLock
import io.github.lenticualrstone.civilight.navigation.ProjectRoute
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.openFromOutside
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
import io.github.lenticualrstone.civilight.ui.scaffold.LocalRelocked
import io.github.lenticualrstone.civilight.ui.scaffold.NoticeRules
import io.github.lenticualrstone.civilight.ui.scaffold.PlaceholderScreen
import io.github.lenticualrstone.civilight.ui.scaffold.WindowToastHost
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
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import io.github.lenticualrstone.civilight.ui.window.FoldPosture
import io.github.lenticualrstone.civilight.ui.window.LocalPaneSplits
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.ui.window.PaneSplitSource
import io.github.lenticualrstone.civilight.ui.window.rememberWindowLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
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
        val posture by remember(activity) {
            WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).map { info ->
                val fold = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                when {
                    fold == null -> FoldPosture.FLAT
                    fold.state == FoldingFeature.State.HALF_OPENED && fold.orientation == FoldingFeature.Orientation.HORIZONTAL -> FoldPosture.TABLETOP
                    fold.state == FoldingFeature.State.HALF_OPENED && fold.orientation == FoldingFeature.Orientation.VERTICAL -> FoldPosture.BOOK
                    else -> FoldPosture.FLAT
                }
            }
        }.collectAsState(initial = FoldPosture.UNKNOWN)
        val layout = rememberWindowLayout(posture)
        val layoutState = rememberUpdatedState(layout)
        val deps = remember(navigator, biometric, capture) { ShellDeps(locator, navigator, biometric, capture, layoutState) }
        val provider = remember(deps) { shellEntryProvider(deps) }
        val backdrop = remember { OverlayBackdrop() }
        val overlays = remember(backdrop) { OverlaySceneStrategy(backdrop) { motionState.value } }
        // A new strategy when the pane mode changes, so Nav3 recomputes the scenes on fold / unfold.
        val listDetail = remember(layout.twoPanes) { ListDetailSceneStrategy { layoutState.value.twoPanes } }
        // DS-66 / DS-67: the window's bottom chrome (toast slot, popover limit) and its one toast host.
        val bottomChrome = remember { BottomChrome() }
        val toasts = deps.toasts
        val context = LocalContext.current
        remember(toasts, context) {
            // A toast without its own project is tagged with the project on top, so it leaves with that project.
            toasts.contextKey = { (backStack.lastOrNull() as? ProjectRoute)?.projectKey }
            // The system accessibility timeout only lengthens a toast (DS-67).
            val a11y = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            toasts.recommendedTimeout = { base, hasAction ->
                val flags = AccessibilityManager.FLAG_CONTENT_TEXT or (if (hasAction) AccessibilityManager.FLAG_CONTENT_CONTROLS else 0)
                a11y?.getRecommendedTimeoutMillis(base.toInt(), flags)?.toLong() ?: base
            }
            true
        }
        val paneSplits = remember(locator) {
            PaneSplitSource(
                locator.settings,
                { module, split -> locator.appScope.launch { locator.stores.settings.setPaneSplit(module, split) } },
                // 0.2.3 (r3): each screen's side panel remembers closed / open (AppSettings.panelClosed).
                { module, closed -> locator.appScope.launch { locator.stores.settings.update { it.withPanelClosed(module, closed) } } },
            )
        }
        // 0.2.3 `뒤로가기`: every layer that closes on back registers here (LocalBackHandler), so the root handler
        // knows when to stay out of the way.
        val backLayers = remember { BackLayers() }

        CompositionLocalProvider(
            LocalFeatureDeps provides deps,
            LocalWindowLayout provides layout,
            LocalCaptureGuard provides capture,
            LocalToasts provides deps.toasts,
            LocalSheetRegistry provides deps.sheets,
            LocalBackHandler provides { enabled, onBack -> LayeredBackHandler(backLayers, enabled, onBack) },
            LocalPaneSplits provides paneSplits,
            LocalBottomChrome provides bottomChrome,
            LocalWindowToasts provides true,
        ) {
            // A file opened from outside waits until the app lock is passed (LIB-08).
            val unlock by deps.unlock.state.collectAsState()
            LaunchedEffect(pendingRoute, unlock.appLocked) {
                if (pendingRoute != null && !unlock.appLocked) {
                    navigator.openFromOutside(pendingRoute)
                    onPendingRouteHandled()
                }
            }
            // Features' start-up hooks (LRC-09 recovery) run once per process after the app lock.
            LaunchedEffect(deps, unlock.appLocked) {
                if (!unlock.appLocked) {
                    deps.startup.runPending(deps) { id, e -> Log.w("Civilight", "start-up hook $id failed", e) }
                        .forEach { navigator.openFromOutside(it) }
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
            // DS-67: a toast never shows over a lock. The app lock, a hidden-project relock or `다시 잠그기` drops it in
            // the same frame (not composed, then dismissed); so does its project leaving the back stack.
            // Snapshot state, so the frame after a relock recomposes with `relocked` false again.
            val lastUnlock = remember { mutableStateOf(unlock) }
            val relocked = NoticeRules.locksTightened(lastUnlock.value, unlock)
            val liveProjects = backStack.mapNotNullTo(HashSet()) { (it as? ProjectRoute)?.projectKey }
            SideEffect {
                if (lastUnlock.value != unlock) lastUnlock.value = unlock
                if (lockMode != null || relocked) toasts.dismissNow()
                toasts.dismissIfReleased(liveProjects)
            }
            val lockLevel = { if (lockMode != null) 1f else lockAmount.value }
            val showContent = lockMode == null || (lockMode == LockScreenMode.BLURRED_CONTENT && lockBlurAvailable())
            // 0.2.3: the one root back handler, composed before NavDisplay so every screen, sheet and mode handler
            // outranks it. It acts only for the keyboard and the bottom screen's two-press exit (BackPolicy).
            RootBackHandler(
                stackSize = backStack.size,
                locked = lockMode != null,
                layers = backLayers,
                toasts = toasts,
                onExit = { activity.finish() },
            )
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
                        CompositionLocalProvider(LocalRelocked provides relocked) {
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
                }
                WindowToastHost(toasts = toasts, hidden = lockMode != null || relocked, liveProjects = liveProjects)
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

/**
 * A layer's back handler (the shell's `LocalBackHandler`). It is registered only while [enabled] and anew each time
 * it turns on, so the layer opened last is the one back closes first (a sheet opened during `고치기` closes before
 * `고치기` ends). While on it counts in [layers], which keeps the root handler out of the way.
 */
@Composable
private fun LayeredBackHandler(layers: BackLayers, enabled: Boolean, onBack: () -> Unit) {
    val current by rememberUpdatedState(onBack)
    if (!enabled) return
    DisposableEffect(layers) {
        layers.add()
        onDispose { layers.remove() }
    }
    BackHandler(enabled = true) { current() }
}

/**
 * 0.2.3 `뒤로가기` at the root ([BackPolicy]): with the keyboard up, back lowers it; on the bottom screen the first
 * press shows `한 번 더 누르면 닫힘` for 2 s and a second press within that time (or while that toast is still shown)
 * closes the app. Enabled only for those cases, so screens keep their own (predictive) back.
 */
@Composable
private fun RootBackHandler(stackSize: Int, locked: Boolean, layers: BackLayers, toasts: ToastController, onExit: () -> Unit) {
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val keyboard = LocalSoftwareKeyboardController.current
    val armedAt = remember { mutableStateOf<Long?>(null) }
    val hintId = remember { mutableStateOf<Long?>(null) }
    fun snapshot() = BackSnapshot(
        imeVisible = imeVisible,
        openLayers = layers.open,
        stackSize = stackSize,
        locked = locked,
        exitArmedAtMs = armedAt.value,
        nowMs = SystemClock.uptimeMillis(),
        hintShowing = hintId.value?.let(toasts::isShowing) ?: false,
    )
    BackHandler(enabled = BackPolicy.rootIntercepts(snapshot())) {
        when (BackPolicy.decide(snapshot())) {
            BackStep.HIDE_KEYBOARD -> keyboard?.hide()
            BackStep.ARM_EXIT -> {
                armedAt.value = SystemClock.uptimeMillis()
                hintId.value = toasts.showBrief(BackCopy.EXIT_HINT, BackPolicy.EXIT_WINDOW_MS)
            }
            BackStep.EXIT -> {
                armedAt.value = null
                hintId.value = null
                toasts.dismissNow()
                onExit()
            }
            BackStep.NONE, BackStep.CLOSE_LAYER, BackStep.POP -> Unit
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
 * routes wrapped in [ProjectEntryFrame] (open store, protection, hidden lock), the script in the
 * STO-16 lens hook, and each module's entry route in its first-entry tour (LIB-19, [ModuleTourFrame]).
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
                ModuleTourFrame(key, deps) { entry.Content() }
            } else {
                ProjectEntryFrame(projectKey, deps) {
                    if (key is Script) ScriptLensEffect()
                    ModuleTourFrame(key, deps) { entry.Content() }
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
