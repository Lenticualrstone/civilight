package io.github.lenticualrstone.civilight.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.navigation.NavResults
import io.github.lenticualrstone.civilight.navigation.Navigator
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.ui.lens.LensController
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.security.BiometricGate
import io.github.lenticualrstone.civilight.ui.security.CaptureGuard
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import io.github.lenticualrstone.civilight.ui.window.WindowLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Everything a feature module gets from the shell. One instance per activity, handed to every
 * feature's entries function and available in composition as [LocalFeatureDeps].
 */
interface FeatureDeps {
    /** Every store (library, recorder, blobs, settings, app state, clock, device id). */
    val stores: StoreContainer

    /** The only way to move between screens. */
    val navigator: Navigator

    /** App settings, always current (theme, layout, lock, rating defaults …). */
    val settings: StateFlow<AppSettings>

    /** System biometric / device-credential prompt (rule 19 gates). */
    val biometric: BiometricGate

    /** Injected clock: all date logic uses it (LIB conventions). */
    val clock: AppClock

    /** Window size class, fold posture and the `펼친 화면 배치` choice; also [io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout]. */
    val windowLayout: State<WindowLayout>

    /** Open projects, shared by every screen; use through [io.github.lenticualrstone.civilight.ui.project.ProjectViewModel]. */
    val projects: ProjectSessions

    /** What is unlocked (app lock, hidden group, hidden items, hidden records). */
    val unlock: UnlockController

    /** FLAG_SECURE / recents masking requests (prefer the `ProtectWindow` composable). */
    val capture: CaptureGuard

    /** The one `되돌리기` toast host (LIB-40). */
    val toasts: ToastController

    /** Results between screens (e.g. `지도에서 고르기`). */
    val results: NavResults

    /** Content of shared sheets; register yours inside your entries function. */
    val sheets: SheetRegistry

    /** Scope that outlives screens (saving after a screen closed, toasts). Main dispatcher. */
    val appScope: CoroutineScope

    /** The lens of an open project (STO-13); one value per project, shared by all lens screens. */
    fun lens(store: ProjectStore): LensController = LensController(store, appScope)
}

/** The shell's [FeatureDeps] in composition (inside every entry and sheet). */
val LocalFeatureDeps = staticCompositionLocalOf<FeatureDeps> { error("FeatureDeps not provided") }

/**
 * A view model of this entry made by [create] (entries get their own store, see
 * `rememberEntryViewModelDecorator`). [key] separates several view models of one class in one entry.
 */
@Composable
inline fun <reified VM : ViewModel> entryViewModel(key: String? = null, noinline create: () -> VM): VM =
    viewModel(key = key, factory = viewModelFactory { initializer { create() } })
