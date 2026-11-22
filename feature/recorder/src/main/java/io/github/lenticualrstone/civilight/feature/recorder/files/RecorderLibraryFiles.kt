package io.github.lenticualrstone.civilight.feature.recorder.files

import io.github.lenticualrstone.civilight.domain.library.crypto.DecryptResult
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKey
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackage
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackageOpen
import io.github.lenticualrstone.civilight.domain.library.importing.RecorderMerge
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.store.recorder.RecorderStore
import java.io.ByteArrayInputStream
import java.security.SecureRandom

/**
 * What one `.civilight-recorder` file holds (LRC-46, §6 Q16 `ED`): every record not in the trash
 * with all its links and placements; hidden records only when the file is locked with the 복구 암호
 * (and `숨긴 것도 백업` is on), otherwise they are left out; audio per `원본 음성 보관`.
 */
data class RecorderBackupPlan(val includeHidden: Boolean, val includeAudio: Boolean) {
    /** The file is encrypted exactly when it carries hidden records. */
    val encrypted: Boolean get() = includeHidden

    companion object {
        fun of(settings: AppSettings, recoverySet: Boolean): RecorderBackupPlan =
            RecorderBackupPlan(includeHidden = recoverySet && settings.backupHidden, includeAudio = settings.recorder.keepAudio)
    }
}

/** How a restore went. */
sealed interface RecorderRestore {
    /** Merged: [added] records new on this device, [audio] audio files stored. */
    data class Merged(val added: Int, val audio: Int) : RecorderRestore

    /** Locked with the 복구 암호 and no key was given, or the key did not open it. */
    data object NeedsRecovery : RecorderRestore

    /** Written by a newer app (refused, never half-read). */
    data object Newer : RecorderRestore

    /** Not a recorder library file, or damaged; nothing was changed. */
    data class Unreadable(val reason: String) : RecorderRestore
}

/** The library and the audio store behind the file (fakes in tests; [StoreRecorderFiles] in the app). */
interface RecorderFilesPort {
    /** Loads the library from disk once (before it loads, [current] is empty). */
    suspend fun ensureLoaded()

    fun current(): RecorderLibrary

    suspend fun update(transform: (RecorderLibrary) -> RecorderLibrary): RecorderLibrary

    /** The bytes of the kept audio [hash], or null when it is not stored. */
    fun audioBytes(hash: String): ByteArray?

    /** Stores audio bytes (content-addressed, deduplicated). */
    suspend fun importAudio(bytes: ByteArray, mime: String, durationMs: Long?)
}

/** [RecorderFilesPort] over core:store's `RecorderStore` (and its blob store). */
class StoreRecorderFiles(private val store: RecorderStore) : RecorderFilesPort {
    override suspend fun ensureLoaded() = store.ensureLoaded()

    override fun current(): RecorderLibrary = store.state.value.library

    override suspend fun update(transform: (RecorderLibrary) -> RecorderLibrary): RecorderLibrary = store.updateLibrary(transform)

    override fun audioBytes(hash: String): ByteArray? = store.audioFile(hash)?.takeIf { it.isFile }?.readBytes()

    override suspend fun importAudio(bytes: ByteArray, mime: String, durationMs: Long?) {
        store.importAudio(ByteArrayInputStream(bytes), mime, durationMs)
    }
}

/**
 * The Life Recorder library file `.civilight-recorder` (LRC-46): write for the Drive backup (LIB-57
 * transport) and restore by merging (LRC-45 rule: a record already here keeps its text and only gains
 * links and placements; nothing is removed). Packaging, versions and encryption are `domain:library`
 * (`RecorderPackage`, `PackageCrypto`, `RecorderMerge`). There is no manual export entry in v1
 * (§6 Q16 `ED`); the backup service calls [write] and the restore flow calls [restore].
 */
class RecorderLibraryFiles(
    private val port: RecorderFilesPort,
    private val now: () -> Timestamp,
    private val writtenBy: String,
    private val newKey: () -> Key,
    private val random: SecureRandom = SecureRandom(),
) {
    /**
     * The file for [plan]. With hidden records it is locked with [recovery] (lock type `recovery`);
     * without a key hidden records are left out whatever the plan says. The library is loaded first
     * (a backup of a not-yet-loaded store would be empty). Reads audio files: call it off the main thread.
     */
    suspend fun write(plan: RecorderBackupPlan, recovery: RecoveryKey?): ByteArray {
        port.ensureLoaded()
        val hidden = plan.includeHidden && recovery != null
        val inner = RecorderPackage.write(
            library = port.current(),
            includeHidden = hidden,
            includeAudio = plan.includeAudio,
            audio = port::audioBytes,
            writtenBy = writtenBy,
            writtenAt = now(),
        )
        if (!hidden || recovery == null) return inner
        return PackageCrypto.encryptWithRecoveryKey(inner, recovery, random, now(), PackagePaths.MIME_RECORDER)
    }

    /**
     * Merges [bytes] into the library. A file locked with the 복구 암호 opens with [dataKey] (the backup
     * data key, from the Keystore or unlocked by the 복구 암호); without it the result is
     * [RecorderRestore.NeedsRecovery] and nothing changes. Audio in the file is stored for records that
     * carry it.
     */
    suspend fun restore(bytes: ByteArray, dataKey: ByteArray? = null): RecorderRestore {
        port.ensureLoaded()
        val content = when (val open = RecorderPackage.read(bytes)) {
            is RecorderPackageOpen.Ready -> open.content
            is RecorderPackageOpen.Encrypted -> {
                val key = dataKey ?: return RecorderRestore.NeedsRecovery
                when (val d = PackageCrypto.decryptWithDataKey(bytes, key)) {
                    is DecryptResult.Ok -> when (val inner = RecorderPackage.read(d.inner)) {
                        is RecorderPackageOpen.Ready -> inner.content
                        is RecorderPackageOpen.Newer -> return RecorderRestore.Newer
                        else -> return RecorderRestore.Unreadable("inner package")
                    }
                    DecryptResult.WrongPassword -> return RecorderRestore.NeedsRecovery
                    else -> return RecorderRestore.Unreadable("encryption")
                }
            }
            is RecorderPackageOpen.Newer -> return RecorderRestore.Newer
            is RecorderPackageOpen.NotAPackage -> return RecorderRestore.Unreadable(open.reason)
            is RecorderPackageOpen.Damaged -> return RecorderRestore.Unreadable(open.reason)
        }
        var stored = 0
        for (record in content.library.records) {
            val audio = record.audio ?: continue
            val data = content.audio[audio.hash] ?: continue
            if (port.audioBytes(audio.hash) == null) {
                port.importAudio(data, audio.mime, audio.durationMs)
                stored++
            }
        }
        var added = 0
        port.update { library ->
            val known = library.records.mapTo(HashSet()) { it.key }
            added = content.library.records.count { it.key !in known }
            RecorderMerge.merge(library, content.library, newKey)
        }
        return RecorderRestore.Merged(added, stored)
    }
}
