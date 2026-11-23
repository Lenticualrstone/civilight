package io.github.lenticualrstone.civilight.store

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.store.app.AppStateStore
import io.github.lenticualrstone.civilight.store.blob.BitmapImageSizeReader
import io.github.lenticualrstone.civilight.store.blob.BlobReferences
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import io.github.lenticualrstone.civilight.store.blob.ImageSizeReader
import io.github.lenticualrstone.civilight.store.library.LibraryStore
import io.github.lenticualrstone.civilight.store.recorder.RecorderStore
import io.github.lenticualrstone.civilight.store.settings.SettingsStore
import io.github.lenticualrstone.civilight.store.templates.FieldTemplateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Every store of the app, made once per process ([get]) from the app's `filesDir`:
 *
 * - [library]: 서재 index, project directories, the open [io.github.lenticualrstone.civilight.store.project.ProjectStore]s
 * - [recorder]: the Life Recorder library
 * - [blobs]: content-addressed files
 * - [settings]: [io.github.lenticualrstone.civilight.model.app.AppSettings] in DataStore
 * - [appState]: onboarding and recent items
 * - [fieldTemplates]: the user's 인물 템플릿 (CST-63, `files/app/fieldTemplates.json`)
 * - [deviceId], [clock]: what edits are stamped with
 *
 * Everything except the DataStore wiring depends only on `java.io.File`, so tests build a
 * container on a temp folder with [StoreContainer] directly.
 */
class StoreContainer(
    val filesDir: File,
    settingsDataStore: DataStore<Preferences>,
    val env: StoreEnvironment,
    imageSizeReader: ImageSizeReader = ImageSizeReader.None,
) {
    val clock: AppClock get() = env.clock
    val deviceId: DeviceId get() = env.deviceId

    val blobs = BlobStore(File(filesDir, BLOBS_DIR), env.clock, env.io, imageSizeReader)
    val library = LibraryStore(filesDir, env)
    val recorder = RecorderStore(File(filesDir, RECORDER_DIR), env, blobs)
    val settings = SettingsStore(settingsDataStore)
    val appState = AppStateStore(File(filesDir, APP_DIR), env)
    val fieldTemplates = FieldTemplateStore(File(filesDir, APP_DIR), env)

    /** Loads the library, recorder and app state (call once at start; every store also loads on first use). */
    suspend fun start() {
        library.ensureLoaded()
        recorder.ensureLoaded()
        appState.ensureLoaded()
        fieldTemplates.ensureLoaded()
    }

    /** Writes every pending change of every store (`onPause` / `onStop`). */
    suspend fun flushAll() {
        for (key in library.state.value.entries.map { it.key }) library.openStore(key)?.flush()
        library.flush()
        recorder.flush()
        appState.flush()
        fieldTemplates.flush()
    }

    /** [flushAll] for callers that cannot suspend; blocks the calling thread. */
    fun flushAllBlocking() = runBlocking { flushAll() }

    /**
     * Deletes a trashed project for good (LIB-39): its directory and index row go, recorder links
     * into it keep their last label with `purgedAt`, its recents go, then unused blobs are collected.
     */
    suspend fun purgeProject(key: Key): LibraryEntry {
        val entry = library.purge(key)
        recorder.markProjectPurged(key, entry.name, entry.hidden)
        appState.removeRecentsOf(key)
        collectBlobGarbage()
        return entry
    }

    /** Removes the example project without the trash and drops recorder links into it (LIB-18, LRC-23). */
    suspend fun deleteExampleProject(key: Key): LibraryEntry {
        val entry = library.deleteWithoutTrash(key)
        recorder.removeProjectLinks(key)
        appState.removeRecentsOf(key)
        return entry
    }

    /**
     * Daily job and app start (LIB-39 `ED`): purges trashed projects and recorder records whose
     * date has come, then collects unused blobs. Rows trashed inside a project are purged by the
     * app right after this (`domain:actions` `PurgeActions.runDue`, then [deleteUnreferencedBlobs]).
     */
    suspend fun runMaintenance() {
        for (entry in library.purgeExpired()) {
            recorder.markProjectPurged(entry.key, entry.name, entry.hidden)
            appState.removeRecentsOf(entry.key)
        }
        recorder.purgeExpired()
        // 지운 템플릿 (CST-63): 30 days after `지우기`.
        fieldTemplates.purgeDeleted(clock.now())
        collectBlobGarbage()
    }

    /**
     * Deletes blobs that no row of any project (live or trashed, open or closed) and no recorder
     * record references, keeping recent imports ([StoreTiming.blobGraceMs]). Nothing is deleted
     * while any project cannot be read completely (a damaged or set-aside file, an unreadable
     * project directory, damaged recorder files): its missing rows may still name blobs.
     */
    suspend fun collectBlobGarbage(): List<String> {
        val referenced = referencedBlobs() ?: return emptyList()
        return blobs.collectGarbage(referenced, env.timing.blobGraceMs)
    }

    /**
     * Deletes exactly [hashes] (the daily purge's `PurgeRun.blobsToDelete`, `domain:actions`) once the
     * same full scan as [collectBlobGarbage] confirms that no project row (live or trashed, any
     * project, not only the ones the purge was given) and no recorder record still references them.
     * Same safeguards: nothing while any project cannot be read completely; recent imports are kept.
     */
    suspend fun deleteUnreferencedBlobs(hashes: Collection<String>): List<String> {
        if (hashes.isEmpty()) return emptyList()
        val referenced = referencedBlobs() ?: return emptyList()
        return blobs.collectGarbage(referenced, env.timing.blobGraceMs, only = hashes.toSet())
    }

    /** Every blob hash a project row or a record references, or null when a project cannot be read completely. */
    private suspend fun referencedBlobs(): Set<String>? {
        library.ensureLoaded()
        recorder.ensureLoaded()
        if (library.reconcileReport.unreadable.isNotEmpty() || recorder.hasSetAsideFiles()) return null
        val referenced = HashSet<String>()
        for (entry in library.state.value.entries) {
            val scan = runCatching { library.scan(entry.key) }.getOrNull() ?: return null
            if (scan.incomplete) return null
            referenced += BlobReferences.of(scan.doc)
        }
        referenced += BlobReferences.of(recorder.state.value.library)
        return referenced
    }

    companion object {
        const val BLOBS_DIR = "blobs"
        const val RECORDER_DIR = "recorder"
        const val APP_DIR = "app"
        const val DEVICE_ID_FILE = "device-id"
        const val SETTINGS_NAME = "settings"

        @Volatile private var instance: StoreContainer? = null

        /** The one container of this process. */
        fun get(context: Context): StoreContainer =
            instance ?: synchronized(this) { instance ?: create(context.applicationContext).also { instance = it } }

        private fun create(context: Context): StoreContainer {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val env = StoreEnvironment(
                scope = scope,
                io = Dispatchers.IO,
                clock = AppClock.System,
                deviceId = DeviceId(File(context.filesDir, DEVICE_ID_FILE)),
            )
            val dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                produceFile = { context.preferencesDataStoreFile(SETTINGS_NAME) },
            )
            val container = StoreContainer(context.filesDir, dataStore, env, BitmapImageSizeReader)
            // Safety net: the app going to the background saves everything (the app also flushes in onStop).
            context.registerComponentCallbacks(object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) {
                    @Suppress("DEPRECATION")
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) scope.launch { container.flushAll() }
                }

                override fun onConfigurationChanged(newConfig: Configuration) = Unit

                @Deprecated("Deprecated in Java")
                override fun onLowMemory() = Unit
            })
            scope.launch { runCatching { container.start() } }
            return container
        }
    }
}
