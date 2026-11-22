package io.github.lenticualrstone.civilight.feature.canvas.image.pick

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ImageTile
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.image.CropHandOff
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageSlots
import io.github.lenticualrstone.civilight.feature.canvas.image.MineThumb
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetArt
import io.github.lenticualrstone.civilight.feature.canvas.image.holoSurface
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.navigation.ImageCrop
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad

/** Image picker tiles: three columns, 9 dp apart (board `ImagePick`: 108 × 144, pitch 117). */
private const val COLUMNS = 3
private const val TILE_ASPECT = 108f / 144f

/**
 * `이미지 고르기` (CNV-27, board `ImagePick`), the shared [ImagePickSheet] other areas open for their slots:
 * title and `닫기`, `<owner> · <ratio chips>`, tabs `기본 이미지 8 · 내 이미지 N`, a three-column grid,
 * the holo row for holo-capable slots, and `사진 · 파일 · 고르기` (`사진` = the system photo picker, 55 §6 Q1). A preset is placed at once; an
 * original (picked now or from `내 이미지`) opens the crop editor.
 */
@Composable
fun SheetScope.ImagePickSheetContent(sheet: ImagePickSheet) {
    val deps = LocalFeatureDeps.current
    val owner = ImageOwner(sheet.ownerKind, sheet.ownerKey, sheet.slot)
    val vm = entryViewModel(key = ImagePickViewModel.key(owner)) {
        ImagePickViewModel(sheet.projectKey, deps.projects, deps::canvasEditContext, deps.stores.blobs)
    }
    val load by vm.state.collectAsState()
    val settings by deps.settings.collectAsState()
    val ready = load as? ProjectLoad.Ready
    // `내 이미지` leaves out originals only hidden things use while locked (55 GAL-19, §3.4).
    val locked = !rememberHiddenGate(sheet.projectKey).open
    val view = remember(ready?.doc, owner, settings.holoDefaultForNewImages, locked) {
        ready?.let { ImagePickModel.build(it.doc, owner, settings.holoDefaultForNewImages, deps.stores.blobs::contains, locked) }
    }
    if (view == null) {
        SheetHeader(ImageSlots.spec(sheet.slot).pickTitle, onClose = ::close, closeLabel = ImageCopy.BTN_CLOSE)
        return
    }
    val readOnly = ready?.readOnly == true
    val closeSheet: () -> Unit = { close() }
    var tab by rememberSaveable { mutableIntStateOf(view.initialTab) }
    var selected by rememberSaveable { mutableStateOf(view.initialSource) }
    var holoChoice by rememberSaveable { mutableStateOf<Boolean?>(null) }
    /** Raised when the user switches holo on: the preview sweeps once then, not when another tile is picked. */
    var holoSweep by rememberSaveable { mutableIntStateOf(0) }
    val holo = holoChoice ?: view.initialHolo
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    val thumbs = remember { mutableStateMapOf<String, ImageBitmap>() }
    val context = LocalContext.current

    fun sayReadOnly() = deps.toasts.show(ImageCopy.READ_ONLY, actionLabel = null)

    fun toCrop(hash: String, tile: Rect?, thumb: ImageBitmap?) {
        CropHandOff.put(CropHandOff(owner.key, owner.slot, hash, tile, holo, thumb))
        close()
        deps.navigator.push(ImageCrop(sheet.projectKey, owner.kind, owner.key, owner.slot, hash))
    }

    val onImported: (ImportResult) -> Unit = { r ->
        when (r) {
            is ImportResult.Imported -> toCrop(r.blobHash, null, null)
            ImportResult.ReadOnly -> sayReadOnly()
            ImportResult.Failed -> deps.toasts.show(ImageCopy.IMAGE_IMPORT_FAILED, actionLabel = null)
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { vm.import(context.contentResolver, it, BlobSource.GALLERY, onImported) }
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { vm.import(context.contentResolver, it, BlobSource.FILE, onImported) }
    }

    SheetHeader(view.title, onClose = ::close, subtitle = view.subtitle, closeLabel = ImageCopy.BTN_CLOSE)
    CivilightTabs(view.tabs, tab, { tab = it }, Modifier.padding(top = 6.dp))
    SheetScrollBody(Modifier.padding(top = 12.dp)) {
        // The tabs swap their tile grids with a crossfade (audit §4 canvas 9, P3).
        ContentSwap(tab, label = "imagePickTab") { shownTab ->
            val tiles = if (shownTab == ImagePickModel.TAB_PRESETS) view.presets else view.mine
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (tiles.isEmpty()) {
                    CivilightText(
                        ImageCopy.MY_IMAGES_EMPTY,
                        CivilightTheme.type.small,
                        Modifier.padding(vertical = 24.dp),
                        color = CivilightTheme.colors.ink3,
                    )
                }
                tiles.chunked(COLUMNS).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        row.forEach { tile ->
                            ImageTile(
                                name = tile.name,
                                selected = selected == tile.source,
                                onClick = { selected = tile.source },
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { bounds[tile.source] = it.boundsInWindow() },
                                aspectRatio = TILE_ASPECT,
                            ) {
                                // The holo row previews on the selected tile; switching it on sweeps once (CNV-45).
                                Box(Modifier.matchParentSize().holoSurface(holo && selected == tile.source, sweepTrigger = holoSweep)) {
                                    tile.preset?.let { PresetArt(it) }
                                    tile.blob?.let { blob -> MineThumb(blob.hash) { thumbs[tile.source] = it } }
                                }
                            }
                        }
                        repeat(COLUMNS - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
        FoldReveal(view.holoVisible) {
            SwitchRow(
                title = ImageCopy.HOLO,
                checked = holo,
                onCheckedChange = {
                    holoChoice = it
                    if (it) holoSweep++
                },
                subtitle = ImageCopy.HOLO_SUB,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
    SheetActions {
        CivilightButton(
            ImageCopy.BTN_PHOTO,
            { if (readOnly) sayReadOnly() else gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            variant = ButtonVariant.GHOST,
            icon = IconId.Image,
        )
        CivilightButton(
            ImageCopy.BTN_FILE,
            { if (readOnly) sayReadOnly() else files.launch(arrayOf("image/*")) },
            variant = ButtonVariant.GHOST,
            icon = IconId.File,
        )
        Box(Modifier.weight(1f))
        CivilightButton(
            ImageCopy.BTN_PICK,
            {
                when (val action = ImagePickModel.action(selected)) {
                    null -> Unit
                    is PickAction.PlacePreset -> if (readOnly) sayReadOnly() else vm.placePreset(owner, action.presetId, holo) { r ->
                        if (r == EditResult.READ_ONLY) sayReadOnly() else closeSheet()
                    }
                    is PickAction.Crop -> if (readOnly) sayReadOnly() else toCrop(action.blobHash, bounds[action.blobHash], thumbs[action.blobHash])
                }
            },
            variant = ButtonVariant.PRIMARY,
            enabled = selected != null,
        )
    }
}
