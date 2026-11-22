package io.github.lenticualrstone.civilight

import android.content.Context
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.navigation.NavResults
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.shell.Startup
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide objects, made once by [CivilightApp]. Activity-bound pieces (navigator, biometric
 * prompt, window protection, sheet registry) are made per activity in the shell.
 */
class ServiceLocator(context: Context) {
    /** Main-thread scope for the whole process (never cancelled). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val stores: StoreContainer = StoreContainer.get(context)

    private val _settings = MutableStateFlow(AppSettings())

    /** Settings as state; holds the stored values before the first frame (startup reads them first). */
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val projects = ProjectSessions(appScope, stores.library)

    val unlock = UnlockController(
        now = { stores.clock.now() },
        policy = { settings.value.relockPolicy },
        appLockEnabled = { settings.value.appLock },
    )

    val toasts = ToastController(appScope)

    val results = NavResults()

    private val _start = MutableStateFlow<List<Route>?>(null)

    /** The first back stack (LIB-08 routing), null until settings, onboarding and the seed are ready. */
    val start: StateFlow<List<Route>?> = _start.asStateFlow()

    init {
        appScope.launch {
            val result = Startup(stores).prepare()
            _settings.value = result.settings
            unlock.onStart(appLockOn = result.settings.appLock)
            _start.value = result.routes
            stores.settings.settings.collect { _settings.value = it }
        }
        appScope.launch(Dispatchers.IO) { runCatching { stores.runMaintenance() } }
    }
}
