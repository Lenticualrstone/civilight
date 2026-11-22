package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.backup.BackupFailure
import io.github.lenticualrstone.civilight.feature.library.onboarding.DriveConnect
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.BackupRun
import kotlinx.serialization.Serializable

/** One stored version of a Drive file. */
data class DriveVersion(val fileId: String, val versionId: String, val sizeBytes: Long)

/**
 * The run index this app keeps next to its backup files, so a new device can list the runs before it
 * downloads anything (LIB-59). Names are kept only for projects that are not hidden: a hidden
 * project's name never leaves the device unencrypted (R19 §5); it is listed as `숨긴 프로젝트 <h>`.
 */
@Serializable
data class DriveRunIndex(
    val runs: List<BackupRun> = emptyList(),
    val projectNames: Map<Key, String> = emptyMap(),
)

/** A Drive call failed; [reason] picks the LIB-61 banner text. */
class DriveException(val reason: BackupFailure, message: String? = null) : Exception(message ?: reason.code)

/**
 * Google Drive in the app-data folder (`drive.appdata`, LIB-57 `ED`): sign-in, upload of a new
 * version, download, delete and the run index. Everything above this port (planning, files,
 * encryption, pruning, restore) is plain code with tests; only this port talks to the network.
 */
interface DriveGateway {
    /** The connected Google account, or null. */
    suspend fun account(): String?

    /** Google sign-in + Drive authorization (onboarding `연결`, 설정 › 보관 switch). */
    suspend fun connect(): DriveConnect

    /** Uploads [bytes] as a new version of the file [name]. */
    suspend fun upload(name: String, mimeType: String, bytes: ByteArray): DriveVersion

    /** The bytes of one version, reporting progress in bytes; null when Drive no longer has it. */
    suspend fun download(fileId: String, versionId: String, onBytes: (Long) -> Unit = {}): ByteArray?

    suspend fun delete(fileId: String, versionId: String)

    suspend fun readIndex(): DriveRunIndex?

    suspend fun writeIndex(index: DriveRunIndex)
}

/**
 * STUB (needsExternal, LIB-57 / LIB-59 / LIB-60): this build has no Google Play services or Drive REST
 * client, so nothing is ever uploaded or downloaded. No account is connected and `연결` returns
 * [DriveConnect.Unavailable]; every transfer fails with [BackupFailure.NETWORK]. Replace this object
 * with the real Drive client (same port); nothing else changes.
 */
object UnavailableDrive : DriveGateway {
    override suspend fun account(): String? = null

    override suspend fun connect(): DriveConnect = DriveConnect.Unavailable

    override suspend fun upload(name: String, mimeType: String, bytes: ByteArray): DriveVersion =
        throw DriveException(BackupFailure.NETWORK, "Drive is not available in this build")

    override suspend fun download(fileId: String, versionId: String, onBytes: (Long) -> Unit): ByteArray? =
        throw DriveException(BackupFailure.NETWORK, "Drive is not available in this build")

    override suspend fun delete(fileId: String, versionId: String) = Unit

    override suspend fun readIndex(): DriveRunIndex? = null

    override suspend fun writeIndex(index: DriveRunIndex) = Unit
}
