package io.github.lenticualrstone.civilight.feature.library.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import android.content.Context
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.export.ExportSheets
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.backup.PasswordInput
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** The 서재's 여러 프로젝트 내보내기 sheet (LIB-72, feature-local). */
@Serializable
data object MultiExportSheet : SheetKey

/**
 * 여러 프로젝트 한꺼번에 내보내기 (LIB-72 `ED`, undrawn): a checklist of the projects the 서재 shows
 * (`프로젝트 파일 .civilight` only), then one `암호` step per locked file, then the system folder
 * picker; one `.civilight` per project is written into the folder. Passwords live in memory only.
 */
@Composable
internal fun SheetScope.MultiExportContent(docs: List<ProjectDoc>, services: LibraryServices) {
    val deps = services.deps
    val sheet = this
    val context = LocalContext.current.applicationContext
    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    var picked by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var step by remember { mutableIntStateOf(0) }
    val passwords = remember { HashMap<Key, CharArray>() }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val pickedSet = picked.toSet()
    val files = MultiExport.files(docs, pickedSet, deps.clock.now(), deps.clock.zone)
    val locked = files.filter { it.encrypt }
    docs.filter { it.key in pickedSet || step == 0 }.forEach { ProtectWindow(WindowProtection.forProject(it.project.protections)) }

    fun finish() {
        val reg = registry ?: return
        val chosen = files
        val keys = passwords.toMap()
        passwords.clear()
        sheet.close()
        FolderDialog.open(reg) { tree ->
            if (tree == null) {
                keys.values.forEach { it.fill('\u0000') }
                return@open
            }
            deps.appScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    deps.stores.recorder.ensureLoaded()
                    val recorder = deps.stores.recorder.state.value.library
                    val blobs = BlobReader { blob -> deps.stores.blobs.fileOf(blob)?.readBytes() }
                    val preview = AndroidCoverPreview(deps.stores.blobs)
                    // Every file is tried; one that fails does not stop the others.
                    chosen.map { file ->
                        val doc = docs.firstOrNull { it.key == file.key } ?: return@map false
                        runCatching {
                            val bytes = MultiExport.build(doc, file, keys[file.key], recorder, blobs, services.writtenBy(), deps.clock.now(), deps.clock.zone, preview)
                            FolderDialog.write(context, tree, file.fileName, ExportDefaults.mimeType(ExportFormat.PROJECT_FILE), bytes)
                        }.getOrDefault(false)
                    }.all { it }
                }
                keys.values.forEach { it.fill('\u0000') }
                deps.toasts.show(if (ok) ExportCopy.DONE else ExportCopy.SAVE_FAILED, actionLabel = null)
            }
        }
    }

    // Pick → one password page per locked file: the page swaps in place (audit row 5, P3).
    ContentSwap(step, Modifier.fillMaxWidth(), label = "multiExportStep") { page ->
    Column {
    if (page == 0) {
        val (title, sub) = ExportDefaults.formatCard(ExportFormat.PROJECT_FILE)
        SheetHeader(ExportCopy.HEADER, onClose = sheet::close, subtitle = title)
        SheetScrollBody(Modifier.padding(top = 4.dp)) {
            CivilightText(sub, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
            MultiExport.rows(docs, pickedSet).forEach { row ->
                CheckboxRow(
                    label = row.title,
                    checked = row.checked,
                    onCheckedChange = { picked = MultiExport.toggle(pickedSet, row.key).toList() },
                    subtitle = if (row.locked) "${row.badge} · ${ExportCopy.PASSWORD}" else row.badge,
                )
            }
        }
        SheetActions(Modifier.padding(bottom = 12.dp)) {
            CivilightButton(ExportCopy.CANCEL, sheet::close, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            CivilightButton(ExportSheets.CREATE_FILE, { if (locked.isEmpty()) finish() else step = 1 }, Modifier.weight(1f), enabled = files.isNotEmpty() && registry != null)
        }
    } else {
        val file = locked.getOrNull(page - 1) ?: return@ContentSwap
        val doc = docs.firstOrNull { it.key == file.key } ?: return@ContentSwap
        SheetHeader(doc.project.name, onClose = sheet::close, subtitle = "${ExportCopy.PASSWORD} · $page/${locked.size}")
        val mismatch = repeat.isNotEmpty() && password != repeat
        PasswordInput(ExportCopy.PASSWORD, password, { password = it })
        PasswordInput(ExportCopy.PASSWORD_REPEAT, repeat, { repeat = it }, Modifier.padding(top = 12.dp), error = if (mismatch) ExportCopy.PASSWORD_MISMATCH else null)
        CivilightText(ExportRow.PASSWORD_WARNING, CivilightTheme.type.small, Modifier.padding(top = 8.dp), color = CivilightTheme.colors.ink3)
        SheetActions(Modifier.padding(bottom = 12.dp)) {
            CivilightButton(ExportCopy.CANCEL, sheet::close, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            CivilightButton(
                ExportSheets.CREATE_FILE,
                {
                    passwords[file.key] = password.toCharArray()
                    password = ""
                    repeat = ""
                    if (step >= locked.size) finish() else step++
                },
                Modifier.weight(1f),
                enabled = password.isNotEmpty() && password == repeat,
            )
        }
    }
    }
    }
}

/** The system folder picker (`ACTION_OPEN_DOCUMENT_TREE`) and the writes into the chosen folder. */
internal object FolderDialog {
    private const val KEY = "civilight.library.export.folder"

    fun open(registry: ActivityResultRegistry, onAnswer: (Uri?) -> Unit) {
        var launcher: ActivityResultLauncher<Uri?>? = null
        launcher = registry.register(KEY, ActivityResultContracts.OpenDocumentTree()) { uri ->
            launcher?.unregister()
            onAnswer(uri)
        }
        launcher.launch(null)
    }

    /** One new file in [tree]; false when the folder cannot take it. */
    fun write(context: Context, tree: Uri, name: String, mime: String, bytes: ByteArray): Boolean {
        val folder = DocumentFile.fromTreeUri(context, tree) ?: return false
        val target = folder.createFile(mime, name) ?: return false
        return runCatching { context.contentResolver.openOutputStream(target.uri)?.use { it.write(bytes) } != null }.getOrDefault(false)
    }
}
