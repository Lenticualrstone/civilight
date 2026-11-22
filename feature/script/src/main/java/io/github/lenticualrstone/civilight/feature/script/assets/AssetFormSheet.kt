package io.github.lenticualrstone.civilight.feature.script.assets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.store.blob.import
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val draftSaver = Saver<AssetFormDraft, String>(save = { AssetForm.encode(it) }, restore = { AssetForm.decode(it) })

/** The edit context of a sheet that writes through the project's store (no screen view model behind it). */
internal fun FeatureDeps.scriptEditContext(): EditContext =
    EditContext(now = clock.now(), zone = clock.zone, device = stores.deviceId.value)

/**
 * `에셋 추가` / an asset's own form (SCR-61 `[PROPOSED]`, not drawn): `종류` (adding only), `이름`,
 * `파일 이름`, then per kind `이미지` (배경 · CG, picked from the gallery and stored once in the blob
 * store), `길이` in seconds with `반복` (BGM · 효과음) and `장소` (배경, the shared place picker in
 * place). The primary `추가` / `저장` writes one undo step through the project's store. Read-only
 * projects see the values and a disabled button.
 */
@Composable
fun SheetScope.AssetFormSheet(key: AssetFormSheetKey) {
    val host = this
    val deps = LocalFeatureDeps.current
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val readOnly = store.readOnly
    var draft by rememberSaveable(stateSaver = draftSaver) { mutableStateOf(AssetForm.initial(doc, key)) }
    var pickingPlace by rememberSaveable { mutableStateOf(false) }
    val ui = remember(doc, key, draft, readOnly) { AssetForm.ui(doc, key, draft, readOnly) }
    val gate = rememberHiddenGate(key.projectKey)
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        deps.appScope.launch {
            val blob = try {
                deps.stores.blobs.import(context.contentResolver, uri, BlobSource.GALLERY)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (blob == null || !blob.mime.startsWith("image/")) return@launch
            // The blob row joins the project's index with `추가` / `저장` (one undo step, none on cancel).
            draft = AssetForm.withPicked(draft, blob)
        }
    }
    if (pickingPlace) {
        PlacePickerSheet(
            doc = doc,
            gate = gate,
            title = AssetsCopy.PICK_PLACE,
            onPick = { ref ->
                draft = draft.copy(placeKey = ref.key)
                pickingPlace = false
            },
            onClose = { pickingPlace = false },
            includeRegions = false,
        )
        return
    }
    SheetHeader(title = ui.title, onClose = ::close, subtitle = if (ui.readOnly) AssetsCopy.READ_ONLY else null)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        SectionLabel(AssetsCopy.FIELD_KIND, Modifier.padding(bottom = 8.dp))
        SegmentedControl(
            options = ui.kinds.map { it.second },
            selectedIndex = ui.kindIndex,
            onSelect = { i -> if (ui.adding && !ui.readOnly) draft = draft.copy(kind = ui.kinds[i].first) },
        )
        CivilightTextField(
            value = draft.name,
            onValueChange = { draft = draft.copy(name = it) },
            modifier = Modifier.padding(top = 16.dp),
            label = AssetsCopy.FIELD_NAME,
            enabled = !ui.readOnly,
        )
        CivilightTextField(
            value = draft.fileName,
            onValueChange = { draft = draft.copy(fileName = it) },
            modifier = Modifier.padding(top = 12.dp),
            label = AssetsCopy.FIELD_FILE,
            enabled = !ui.readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        )
        if (ui.showImage) {
            FormRow(AssetsCopy.FIELD_IMAGE, value = ui.imageLine, modifier = Modifier.padding(top = 16.dp)) {
                AssetThumbnail(AssetThumb.Image(draft.imageHash))
            }
            if (!ui.readOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextAction(AssetsCopy.PICK_IMAGE, onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                    if (draft.imageHash != null) TextAction(AssetsCopy.REMOVE_IMAGE, onClick = { draft = draft.copy(imageHash = null) })
                }
            }
        }
        if (ui.showLength) {
            CivilightTextField(
                value = draft.lengthText,
                onValueChange = { draft = draft.copy(lengthText = it) },
                modifier = Modifier.padding(top = 16.dp),
                label = AssetsCopy.FIELD_LENGTH,
                unit = AssetsCopy.SECONDS,
                hint = ui.lengthPreview,
                enabled = !ui.readOnly,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            SwitchRow(AssetsCopy.FIELD_LOOP, draft.loop, { draft = draft.copy(loop = it) }, enabled = !ui.readOnly)
        }
        if (ui.showPlace) {
            FormRow(
                AssetsCopy.FIELD_PLACE,
                value = ui.placeLine,
                action = AssetsCopy.CHANGE.takeIf { !ui.readOnly },
                onAction = { pickingPlace = true },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
    SheetActions {
        CivilightButton(AssetsCopy.CANCEL, onClick = { host.close() }, modifier = Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            text = ui.primary,
            onClick = {
                val chosen = draft
                deps.appScope.launch {
                    try {
                        store.update(ui.primary) { d -> AssetForm.apply(d, key, chosen, deps.scriptEditContext()) }
                    } catch (e: ReadOnlyProjectException) {
                        // The example project: nothing changes.
                    }
                }
                host.close()
            },
            modifier = Modifier.weight(1f),
            enabled = ui.canSave,
        )
    }
}

/** A labelled value row with an optional leading thumbnail and a `바꾸기 ›` action. */
@Composable
private fun FormRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 52.dp).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(label, type.label, color = colors.ink2, maxLines = 1)
        leading?.invoke()
        CivilightText(value.orEmpty(), type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
        if (action != null) TextAction(action, onAction)
    }
}
