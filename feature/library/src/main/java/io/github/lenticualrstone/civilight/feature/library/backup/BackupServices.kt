package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.feature.library.export.AndroidCoverPreview
import io.github.lenticualrstone.civilight.feature.library.onboarding.DriveConnect
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * When the daily backup is due (LIB-57 `ED`, standing in for WorkManager's 24 h periodic work): a day
 * after the last success (or never), and not again within an hour of a failed try.
 */
object BackupSchedule {
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val RETRY_MS = 60L * 60 * 1000

    fun due(book: BackupBook, now: Timestamp): Boolean {
        val success = book.state.lastSuccessAt
        if (success != null && now - success < DAY_MS) return false
        val last = book.lastRun ?: return true
        return last.ok || now - last.startedAt >= RETRY_MS
    }
}

/** [BackupSources] over the app's stores (the open session's document first, else the stored copy). */
internal class StoreBackupSources(private val deps: FeatureDeps) : BackupSources {
    private val stores = deps.stores

    override suspend fun projects(): List<BackupProject> {
        stores.library.ensureLoaded()
        return stores.library.state.value.live.filter { !it.isExample }.mapNotNull { entry ->
            val doc = deps.projects.peekDoc(entry.key) ?: withContext(Dispatchers.IO) { runCatching { stores.library.readDoc(entry.key) }.getOrNull() }
            doc?.let { BackupProject(it, entry.updatedAt) }
        }
    }

    override fun blobs(): BlobReader = BlobReader { blob -> stores.blobs.fileOf(blob)?.readBytes() }

    override suspend fun recorder(): RecorderLibrary {
        stores.recorder.ensureLoaded()
        return stores.recorder.state.value.library
    }

    override fun audio(hash: String): ByteArray? = stores.blobs.fileOf(hash)?.readBytes()

    override suspend fun settings(): AppSettings = deps.settings.value
}

/**
 * The backup side of this lane, one per activity in `LibraryServices` (no I/O at creation): the
 * book, the 복구 암호 keyring, the Drive port (the labelled stub [UnavailableDrive] in this build) and
 * the one restore that may be running.
 */
class BackupServices(private val deps: FeatureDeps) {
    val book: BackupBooks = BackupBookStore(deps.stores)
    val facts = BackupFacts(book.state)
    val drive: DriveGateway = UnavailableDrive
    val keyring = RecoveryKeyring(book, AndroidKeystoreWrap())
    val restore = RestoreJob(deps, drive)
    val storage: DeviceStorage = StoreDeviceStorage(deps.stores)

    /** One run at a time: the start-up run and `지금 다시 시도` (LIB-61) never overlap. */
    private val runLock = Mutex()
    private val _running = MutableStateFlow(false)

    /** A run is in progress (설정 › 보관 disables `다시 로그인` · `지금 다시 시도` meanwhile). */
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Loads the book in the background (the 서재, 설정 and 새 프로젝트 call it when they open). */
    fun load() {
        deps.appScope.launch { book.ensureLoaded() }
    }

    /** `연결` (onboarding `OnbSafe`, 설정 › 보관): sign-in, then the switch on with the account. */
    suspend fun connect(): DriveConnect {
        val result = drive.connect()
        if (result is DriveConnect.Connected) deps.stores.settings.setDriveBackup(true, result.accountId)
        return result
    }

    /** One backup run now, when due (the start-up hook; `ED` until WorkManager is a dependency). */
    suspend fun runIfDue(writtenBy: String) {
        book.ensureLoaded()
        if (!deps.settings.value.driveBackup || !BackupSchedule.due(book.state.value, deps.clock.now())) return
        run(writtenBy)
    }

    /**
     * `지금 다시 시도` (LIB-61): one run now, whatever the schedule says. [BackupOutcome.NotRunning]
     * when the switch is off or no account is connected (always, with the [UnavailableDrive] stub).
     */
    suspend fun runNow(writtenBy: String): BackupOutcome {
        book.ensureLoaded()
        return run(writtenBy)
    }

    private suspend fun run(writtenBy: String): BackupOutcome = runLock.withLock {
        _running.value = true
        try {
            val runner = BackupRunner(StoreBackupSources(deps), drive, book, keyring::current, writtenBy, preview = AndroidCoverPreview(deps.stores.blobs))
            withContext(Dispatchers.Default) { runner.run(deps.clock.now(), Keys.newKey()) }
        } finally {
            _running.value = false
        }
    }

    /**
     * LIB-24 ◇ `이전 백업 n · 암호 없음 · 지우기 ›`: deletes the project's unencrypted Drive versions.
     * Versions Drive refuses to delete stay listed (they still exist).
     */
    suspend fun deleteUnencrypted(projectKey: Key) {
        book.ensureLoaded()
        val gone = book.state.value.unencryptedEntries(projectKey).filter { runCatching { drive.delete(it.driveFileId, it.versionId) }.isSuccess }
        if (gone.size == book.state.value.unencryptedEntries(projectKey).size) book.update { it.withoutUnencrypted(projectKey) }
    }
}
