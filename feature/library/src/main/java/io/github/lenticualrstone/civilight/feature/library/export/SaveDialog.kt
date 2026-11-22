package io.github.lenticualrstone.civilight.feature.library.export

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The system save dialog (`ACTION_CREATE_DOCUMENT`, LIB-41 `ED`). Registered on the activity's
 * result registry without a lifecycle, so the answer still arrives when the sheet that asked has
 * left composition meanwhile (a re-lock or the app lock may dispose it while the dialog is open).
 */
internal object SaveDialog {
    private const val KEY = "civilight.library.export.save"

    fun open(registry: ActivityResultRegistry, fileName: String, mimeType: String, onAnswer: (Uri?) -> Unit) {
        var launcher: ActivityResultLauncher<String>? = null
        launcher = registry.register(KEY, ActivityResultContracts.CreateDocument(mimeType)) { uri ->
            launcher?.unregister()
            onAnswer(uri)
        }
        launcher.launch(fileName)
    }

    /** Writes [bytes] to [uri], replacing what is there. False when the place cannot be written. */
    suspend fun write(context: Context, uri: Uri, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val stream = runCatching { context.contentResolver.openOutputStream(uri, "wt") }.getOrNull()
                ?: context.contentResolver.openOutputStream(uri)
            requireNotNull(stream).use { it.write(bytes) }
        }.isSuccess
    }

    /** `Civilight <version> (Android)` for the manifest's `writtenBy` (LIB-50). */
    fun writtenBy(context: Context): String {
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"
        return "Civilight $version (Android)"
    }
}
