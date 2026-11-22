package io.github.lenticualrstone.civilight.feature.library.export

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.ChoiceCard
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.feature.library.backup.PasswordInput
import io.github.lenticualrstone.civilight.designsystem.component.OutlinedSegmented
import io.github.lenticualrstone.civilight.designsystem.component.RatingBadge
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentItem
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.export.ExportLayer
import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.export.HiddenSceneChoice
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.feature.library.export.pdf.AndroidPdfWriter
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.rememberProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.ExportSheet
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge
import kotlinx.coroutines.launch

/**
 * 내보내기 (LIB-41, board `Export`), the shared sheet other areas open with [ExportSheet]: `내보내기` +
 * `닫기`, `<name> · <badge>`, `형식` radio cards, the option rows of the chosen format, warnings, and
 * the primary `<형식> 만들기`. Create: one prompt when hidden scenes go in, the file is built off the
 * main thread, the system save dialog asks where, and only a saved CSV or JSON fixes voice IDs.
 * The example, trashed projects and a hidden project whose group is locked never show it.
 */
@Composable
internal fun SheetScope.ExportSheetContent(sheet: ExportSheet, services: LibraryServices) {
    val scope = this
    val deps = services.deps
    val key = sheet.projectKey
    val doc by rememberProjectDoc(deps.projects, key)
    val library by deps.stores.library.state.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    val recorder by deps.stores.recorder.state.collectAsState()
    val draftFlow = remember(key) { services.exports.of(key) }
    val draft by draftFlow.collectAsState()
    val entry = library.entry(key)
    val blocked = library.loaded && (entry == null || entry.isExample || entry.deletedAt != null)
    val current = doc
    val lockedAway = current != null && current.project.protections.hidden && !unlock.projectOpen(key)

    fun closeSheet() {
        services.exports.clear(key)
        scope.close()
    }

    LaunchedEffect(blocked, lockedAway) { if (blocked || lockedAway) closeSheet() }
    // Typed passwords never outlive the sheet, however it leaves (scrim, back, a re-lock, the dialog).
    DisposableEffect(key) { onDispose { draftFlow.value = draftFlow.value?.let(ExportFormRules::withoutPasswords) } }
    LaunchedEffect(current?.key) {
        val d = current ?: return@LaunchedEffect
        if (draftFlow.value == null) draftFlow.value = ExportFormRules.initial(d, deps.clock.now(), deps.clock.zone)
    }
    if (current == null || blocked || lockedAway) return
    val form = draft ?: return
    ProtectWindow(WindowProtection.forProject(current.project.protections))

    fun update(change: (ExportForm) -> ExportForm) {
        draftFlow.value = draftFlow.value?.let(change)
    }

    val model = remember(current, form, recorder.library) { ExportFormRules.sheet(current, form, recorder.library) }
    var working by remember { mutableStateOf(false) }
    // The gate and the build belong to this sheet: leaving it cancels them, so no dialog opens for
    // an export the author dismissed. Only the write after the dialog's answer outlives it.
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext
    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry

    fun create() {
        if (working || registry == null) return
        working = true
        sheetScope.launch {
            val build = try {
                ExportRunner.build(
                    doc = current,
                    form = form,
                    recorder = recorder.library,
                    blobs = BlobReader { blob -> deps.stores.blobs.fileOf(blob)?.readBytes() },
                    writtenBy = SaveDialog.writtenBy(context),
                    now = deps.clock.now(),
                    pdf = AndroidPdfWriter(context),
                    coverRating = deps.settings.value.ratingDefaults.exportCoverRating,
                    preview = AndroidCoverPreview(deps.stores.blobs),
                    authenticate = { deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS },
                )
            } finally {
                working = false
            }
            if (build == ExportBuild.Unavailable) deps.toasts.show(ExportCopy.TODO_COPY_LIB41_SAVE_FAILED, actionLabel = null)
            val ready = (build as? ExportBuild.Ready)?.file ?: return@launch
            // The sheet steps aside for the system dialog; its draft stays until the file is saved.
            scope.close()
            SaveDialog.open(registry, ready.fileName, ready.mimeType) { uri ->
                if (uri == null) return@open
                deps.appScope.launch {
                    if (!SaveDialog.write(context, uri, ready.bytes)) {
                        deps.toasts.show(ExportCopy.TODO_COPY_LIB41_SAVE_FAILED, actionLabel = null)
                        return@launch
                    }
                    ready.plan?.let { plan ->
                        val at = deps.clock.now()
                        services.writer.edit(key, model.header) { doc, _ -> ExportRunner.fixVoiceIds(doc, plan, at) }
                    }
                    services.exports.clear(key)
                    deps.toasts.show(ExportCopy.TODO_COPY_LIB41_DONE, actionLabel = null)
                }
            }
        }
    }

    ExportHeader(model.header, model.close, current, ::closeSheet)
    SheetScrollBody(Modifier.padding(top = 4.dp)) {
        SectionLabel(ExportCopy.SECTION_FORMAT, Modifier.padding(top = 10.dp))
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportDefaults.FORMATS.forEach { format ->
                val (title, sub) = ExportDefaults.formatCard(format)
                ChoiceCard(
                    title = title,
                    subtitle = sub,
                    selected = form.format == format,
                    onClick = { update { ExportFormRules.selectFormat(it, current, format, deps.clock.now(), deps.clock.zone) } },
                    compact = true,
                )
            }
        }
        Column(Modifier.padding(top = 6.dp)) {
            model.rows.forEach { row -> OptionRow(row, form, current, ::update) }
            model.warnings.forEach { warning ->
                CivilightText(warning.text, CivilightTheme.type.small, Modifier.padding(top = 10.dp), color = CivilightTheme.colors.warn)
            }
        }
    }
    SheetActions {
        CivilightButton(
            model.primaryButton,
            onClick = ::create,
            modifier = Modifier.weight(1f),
            enabled = ExportFormRules.canCreate(form) && !working && registry != null,
        )
    }
}

/** `내보내기` + `닫기`, then `<name> · <badge>` (board `Export`). */
@Composable
private fun ExportHeader(title: String, close: String, doc: ProjectDoc, onClose: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(title, type.title, Modifier.weight(1f), color = colors.ink, maxLines = 1)
        TextAction(close, onClose)
    }
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CivilightText(doc.project.name + ExportCopy.SEP.trimEnd(), type.small, color = colors.ink3, maxLines = 1)
        RatingBadge(ThemeBridge.rating(doc.project.rating))
    }
}

/** A board `.row`: label left in `ink3`, the value or control right; 44 min; `line` above. */
@Composable
private fun RowFrame(label: String, trailing: @Composable () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CivilightText(label, CivilightTheme.type.command, Modifier.weight(1f), color = colors.ink3)
        trailing()
    }
}

@Composable
private fun OptionRow(row: ExportRow, form: ExportForm, doc: ProjectDoc, update: ((ExportForm) -> ExportForm) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    when (row) {
        is ExportRow.Range -> {
            var open by rememberSaveable { mutableStateOf(false) }
            val choices = remember(doc) { ExportRanges.choices(doc) }
            RowFrame(row.label) {
                TextAction(row.value + " ›", { open = !open }, color = colors.ink)
            }
            if (open) {
                CheckboxRow(ExportCopy.RANGE_ALL, form.options.rangeKeys.isEmpty(), { if (it) update { f -> ExportFormRules.setRange(f, emptyList()) } }, dense = true)
                choices.forEach { choice ->
                    val checked = choice.key in form.options.rangeKeys
                    CheckboxRow(
                        label = choice.label,
                        checked = checked,
                        onCheckedChange = { on ->
                            update { f -> ExportFormRules.setRange(f, if (on) f.options.rangeKeys + choice.key else f.options.rangeKeys - choice.key) }
                        },
                        modifier = Modifier.padding(start = (choice.depth * 16).dp),
                        dense = true,
                    )
                }
            }
        }
        is ExportRow.HiddenScenes -> RowFrame(row.label) {
            OutlinedSegmented(
                items = row.options.map { SegmentItem(it) },
                selectedIndex = if (row.choice == HiddenSceneChoice.INCLUDE) 0 else 1,
                onSelect = { i -> update { ExportFormRules.setHiddenScenes(it, i == 0) } },
                height = 32.dp,
            )
        }
        is ExportRow.HiddenSpeakers -> RowFrame(row.label) {
            CivilightText(row.info, type.small, color = colors.ink2, maxLines = 1)
        }
        is ExportRow.Layers -> {
            var open by rememberSaveable { mutableStateOf(false) }
            RowFrame(row.label) {
                TextAction(row.value + " ›", { open = !open }, color = colors.ink)
            }
            if (open) {
                ExportLayer.entries.forEach { layer ->
                    CheckboxRow(layer.label, layer.id in form.options.layers, { on -> update { ExportFormRules.toggleLayer(it, layer, on) } }, dense = true)
                }
            }
        }
        is ExportRow.SecretFields -> RowFrame(row.label) {
            CivilightSwitch(row.on, { on -> update { ExportFormRules.setSecretFields(it, on) } })
        }
        is ExportRow.FileName -> Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 10.dp)) {
            CivilightTextField(
                value = row.value,
                onValueChange = { text -> update { ExportFormRules.setFileName(it, text) } },
                label = row.label,
            )
        }
        is ExportRow.PdfPassword -> {
            RowFrame(row.label) { CivilightSwitch(row.on, { on -> update { ExportFormRules.setPdfPasswordOn(it, on) } }) }
            if (row.on) {
                PasswordField(form.pdfPassword, null) { text -> update { ExportFormRules.setPdfPassword(it, text) } }
            }
        }
        is ExportRow.Password -> {
            RowFrame(row.label) { CivilightSwitch(row.on, { on -> update { ExportFormRules.setEncrypt(it, on) } }) }
            if (row.on) {
                PasswordField(form.password, null) { text -> update { ExportFormRules.setPassword(it, text) } }
                PasswordField(
                    form.passwordRepeat,
                    ExportCopy.PASSWORD_REPEAT,
                    error = ExportCopy.TODO_COPY_LIB41_PASSWORD_MISMATCH.takeIf { form.passwordRepeat.isNotEmpty() && !ExportFormRules.passwordsMatch(form) },
                ) { text -> update { ExportFormRules.setPasswordRepeat(it, text) } }
                CivilightText(row.warning, type.small, Modifier.padding(top = 6.dp, bottom = 4.dp), color = colors.ink3)
            }
        }
        is ExportRow.RecorderRecords -> RowFrame(row.label) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CheckboxMark(row.includeAudio, onCheckedChange = { on -> update { ExportFormRules.setRecorderAudio(it, on) } }, dense = true)
                CivilightText(listOfNotNull(row.audioOption, row.hiddenNote).joinToString(ExportCopy.SEP), type.small, color = colors.ink2, maxLines = 1)
            }
        }
        is ExportRow.IncludeTrash -> RowFrame(row.label) {
            CivilightSwitch(row.on, { on -> update { ExportFormRules.setIncludeTrash(it, on) } })
        }
    }
}

/** A password field (`RHRecoveryPw` `pw_field`): dots, no suggestions, and the `보이기` eye button. */
@Composable
private fun PasswordField(value: String, label: String?, error: String? = null, onChange: (String) -> Unit) {
    PasswordInput(label, value, onChange, Modifier.padding(top = 8.dp), error = error)
}
