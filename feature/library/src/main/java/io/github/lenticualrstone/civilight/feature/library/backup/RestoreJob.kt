package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.backup.RestoreProgress
import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.feature.library.notices.LibraryNotices
import io.github.lenticualrstone.civilight.feature.library.openfile.StoreImportPorts
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.app.BackupEntry
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.Restoring
import io.github.lenticualrstone.civilight.navigation.resetTo
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The one restore of the process (LIB-60). It runs in the app scope, so leaving 되살리는 중 does not
 * stop it (`ED`: a foreground service would also survive the app being closed; the app module owns
 * the manifest, see Requests). Its status is process-wide, so a new activity shows the same run.
 */
class RestoreJob internal constructor(
    private val deps: FeatureDeps,
    private val drive: DriveGateway,
) {
    val status: StateFlow<RestoreStatus?> get() = shared.asStateFlow()

    val running: Boolean get() = shared.value?.finished == false

    /** Checks the 복구 암호 against the first locked file; false = wrong (the sheet shows the error). */
    suspend fun checkPassword(request: RestoreRequest, password: CharArray): Boolean = try {
        prefetched = withContext(Dispatchers.IO) { runner().checkPassword(request, password) }
        true
    } catch (_: WrongRecoveryPassword) {
        false
    }

    /**
     * Starts the restore and shows 되살리는 중. At the end: during the first run, onboarding is
     * finished (LIB-15: the example is never kept, [onFirstRunDone]); otherwise the 서재.
     */
    fun start(request: RestoreRequest, password: CharArray?, onFirstRunDone: () -> Unit) {
        if (running) return
        val zone = deps.clock.zone
        val dateLabel = TimeLabels.monthDay(request.run.startedAt, zone)
        val pw = password?.copyOf()
        val reuse = prefetched
        prefetched = emptyMap()
        // The new run's status before the screen shows, so neither an old run's end nor a second
        // start slips in.
        shared.value = RestoreStatus(
            RestoreProgress(0, request.totalBytes, request.projects.size, 0, 0, request.hiddenCount), step = 0, dateLabel = dateLabel, secondsLeft = null,
        )
        deps.navigator.push(Restoring)
        deps.appScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    runner().run(request, pw, deps.clock.now(), zone, dateLabel, reuse) { shared.value = it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Drive or the store failed half-way: what was imported stays; say so and stop.
                null
            } finally {
                pw?.fill('\u0000')
            }
            shared.value = shared.value?.copy(finished = true)
            if (result != null && result.imported.isNotEmpty()) {
                val profiles = withContext(Dispatchers.IO) {
                    result.imported.mapNotNull { outcome -> runCatching { ProjectProfile.of(deps.stores.library.readDoc(outcome.projectKey)) }.getOrNull() }
                }
                deps.toasts.show(LibraryNotices.restoreDone(profiles), actionLabel = null)
            }
            if (result == null || result.failed > 0) deps.toasts.show(RestoreCopy.TODO_COPY_LIB60_PARTLY_FAILED, actionLabel = null)
            deps.stores.appState.ensureLoaded()
            when (RestoreEnd.of(result, firstRun = !deps.stores.appState.onboarding.state.value.firstRunCompleted)) {
                RestoreEnd.FINISH_ONBOARDING -> onFirstRunDone()
                RestoreEnd.LIBRARY -> deps.navigator.resetTo(Library)
                RestoreEnd.BACK -> if (deps.navigator.current == Restoring) deps.navigator.pop()
            }
        }
    }

    private fun runner() = RestoreRunner(drive, StoreImportPorts(deps), Keys::newKey)

    private var prefetched: Map<BackupEntry, ByteArray> = emptyMap()

    private companion object {
        /** Process-wide: survives a new activity (a new `LibraryServices`). */
        val shared = MutableStateFlow<RestoreStatus?>(null)
    }
}

/** Where a restore ends (LIB-59, LIB-60). */
enum class RestoreEnd {
    /** First run and something came back: onboarding ends (the example is deleted, LIB-15). */
    FINISH_ONBOARDING,

    /** The 서재 with what came back. */
    LIBRARY,

    /** First run and nothing came back: back to 백업에서 되살리기, onboarding goes on. */
    BACK,
    ;

    companion object {
        fun of(result: RestoreResult?, firstRun: Boolean): RestoreEnd {
            val restored = result != null && (result.imported.isNotEmpty() || result.recorderMerged)
            return when {
                !firstRun -> LIBRARY
                restored -> FINISH_ONBOARDING
                else -> BACK
            }
        }
    }
}
