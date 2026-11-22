package io.github.lenticualrstone.civilight.shell

import androidx.compose.runtime.State
import io.github.lenticualrstone.civilight.ServiceLocator
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.navigation.NavResults
import io.github.lenticualrstone.civilight.navigation.Navigator
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetRegistry
import io.github.lenticualrstone.civilight.ui.feature.StartupHooks
import io.github.lenticualrstone.civilight.ui.feature.registerShellSheets
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.security.BiometricGate
import io.github.lenticualrstone.civilight.ui.security.CaptureGuard
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import io.github.lenticualrstone.civilight.ui.window.WindowLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * The shell's [FeatureDeps]: process-wide parts from [ServiceLocator], activity-bound parts
 * (navigator over the back stack, biometric prompt, window protection, window size) from the
 * activity. The sheet registry is per activity so a recreated activity can register again.
 */
class ShellDeps(
    locator: ServiceLocator,
    override val navigator: Navigator,
    override val biometric: BiometricGate,
    override val capture: CaptureGuard,
    override val windowLayout: State<WindowLayout>,
) : FeatureDeps {
    override val stores: StoreContainer = locator.stores
    override val settings: StateFlow<AppSettings> = locator.settings
    override val clock: AppClock = locator.stores.clock
    override val projects: ProjectSessions = locator.projects
    override val unlock: UnlockController = locator.unlock
    override val toasts: ToastController = locator.toasts
    override val results: NavResults = locator.results
    override val appScope: CoroutineScope = locator.appScope
    override val sheets: SheetRegistry = SheetRegistry().apply { registerShellSheets() }
    override val startup: StartupHooks = locator.startup
}
