package io.github.lenticualrstone.civilight.ui.feature

import io.github.lenticualrstone.civilight.navigation.Route

/**
 * Work a feature asks the shell to run once per process after start-up, e.g. LRC-09: the recorder
 * resumes a saved `session.json` into `정리 중` after a crash.
 *
 * A hook runs after the start routes are on the back stack and the app lock (if any) is passed, on
 * the main thread, at most once per process (a recreated activity registers again but does not run
 * it again). It may read stores (suspend) and returns a route to push on top of the start routes, or
 * null. A hook that throws is logged and skipped; it never blocks start-up.
 */
fun interface StartupHook {
    suspend fun run(deps: FeatureDeps): Route?
}

/**
 * The hooks registered by features in their entries function:
 * `deps.startup.register("recorder.resume") { d -> … }`. Registering is not running, so it obeys the
 * "no work in the entries function" rule. Ids are unique; registering an id again (activity
 * recreation) replaces the hook.
 */
class StartupHooks {
    private val hooks = LinkedHashMap<String, StartupHook>()
    private val ran = HashSet<String>()

    @Synchronized
    fun register(id: String, hook: StartupHook) {
        hooks[id] = hook
    }

    @Synchronized
    fun isRegistered(id: String): Boolean = id in hooks

    /** Hooks not yet run in this process, in registration order. */
    @Synchronized
    fun pending(): List<Pair<String, StartupHook>> = hooks.entries.filter { it.key !in ran }.map { it.key to it.value }

    @Synchronized
    private fun markRan(id: String) {
        ran += id
    }

    /**
     * Runs every pending hook and returns the routes they asked for, in order. A hook counts as run
     * once it returns or throws; one cancelled half-way (the activity went away) stays pending and runs
     * again in the next activity. Failures go to [onError] and do not stop the other hooks.
     */
    suspend fun runPending(deps: FeatureDeps, onError: (String, Throwable) -> Unit = { _, _ -> }): List<Route> {
        val routes = ArrayList<Route>()
        for ((id, hook) in pending()) {
            try {
                hook.run(deps)?.let(routes::add)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError(id, e)
            }
            markRan(id)
        }
        return routes
    }

    companion object {
        /** Hooks of a deps object made outside the shell (tests): kept, never run. */
        val Unused: StartupHooks get() = StartupHooks()
    }
}
