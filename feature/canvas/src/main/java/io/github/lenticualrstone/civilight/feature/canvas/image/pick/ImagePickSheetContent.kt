package io.github.lenticualrstone.civilight.feature.canvas.image.pick

import io.github.lenticualrstone.civilight.ui.image.CoverThumb
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.feature.canvas.image.BlobImage
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.heightIn
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
import io.github.lenticualrstone.civilight.feature.canvas.common.CanvasWrites
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
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

    fun toCrop(
        hash: String,
        tile: Rect?,
        thumb: ImageBitmap?,
        holoStart: Boolean = holo,
        crops: Map<Ratio, Crop> = emptyMap(),
        note: String? = null,
    ) {
        CropHandOff.put(CropHandOff(owner.key, owner.slot, hash, tile, holoStart, thumb, note = note, crops = crops))
        close()
        deps.navigator.push(ImageCrop(sheet.projectKey, owner.kind, owner.key, owner.slot, hash))
    }

    /** `빼기` (U-08 LIB-87): no confirm, the sheet closes, `표지 뺌` · `되돌리기` follows. */
    fun clearSlot() {
        if (readOnly) return sayReadOnly()
        vm.clear(owner) { r, removed, index ->
            if (r == EditResult.READ_ONLY) {
                sayReadOnly()
            } else {
                closeSheet()
                // `되돌리기` runs on the app scope and puts back exactly the removed row: the sheet's entry (for
                // example 프로젝트 설정) may be popped while the toast is still up, which cancels the view model.
                val projectKey = sheet.projectKey
                deps.toasts.show(
                    ImageCopy.TOAST_COVER_CLEARED,
                    actionLabel = ImageCopy.UNDO,
                    onAction = {
                        removed?.let { use ->
                            CanvasWrites.launch(deps, projectKey, ImageCopy.UNDO) { d, _ -> ImageEdits.restoreUse(d, use, index) }
                        }
                    },
                )
            }
        }
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
    view.coverLine?.let { line ->
        CoverCurrentLine(
            line = line,
            onCrop = line.cropHash?.let { hash -> { if (readOnly) sayReadOnly() else toCrop(hash, null, null) } },
            onClear = if (line.canClear) ({ clearSlot() }) else null,
        )
    }
    CivilightTabs(view.tabs, tab, { tab = it }, Modifier.padding(top = 6.dp))
    SheetScrollBody(Modifier.padding(top = 12.dp)) {
        // The tabs swap their tile grids with a crossfade (audit §4 canvas 9, P3).
        ContentSwap(tab, label = "imagePickTab") { shownTab ->
            val tiles = view.tilesOf(shownTab)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (tiles.isEmpty()) {
                    CivilightText(
                        view.emptyOf(shownTab),
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
                                // The holo row previews on the selected tile; switching it on sweeps once (CNV-45). A gallery
                                // tile whose item has holo (its chip says `홀로`) shows the foil too, so the label never
                                // stands without the effect.
                                val isSelected = selected == tile.source
                                val foilOn = if (isSelected) holo else tile.item?.holo == true
                                // This tile's own sweep count: it rises only when the switch is turned on while this
                                // tile is the selected one, so picking another tile never sweeps.
                                val sweepCount = remember(tile.source) { intArrayOf(0, holoSweep) }
                                if (holoSweep != sweepCount[1]) {
                                    if (isSelected) sweepCount[0]++
                                    sweepCount[1] = holoSweep
                                }
                                Box(Modifier.matchParentSize().holoSurface(foilOn, sweepTrigger = sweepCount[0], consumeDrag = false)) {
                                    tile.preset?.let { PresetArt(it) }
                                    val item = tile.item
                                    if (item != null) {
                                        // A gallery tile shows the item's own `카드 3:4` crop (LIB-85).
                                        tile.blob?.let { blob -> BlobImage(blob.hash, item.cropFor(Ratio.R3X4), GALLERY_TILE_SIDE, null) { } }
                                    } else {
                                        tile.blob?.let { blob -> MineThumb(blob.hash) { thumbs[tile.source] = it } }
                                    }
                                }
                                if (tile.item != null) tile.chip?.let { ImageStatusChip(it, Modifier.align(Alignment.TopStart).padding(6.dp)) }
                            }
                        }
                        repeat(COLUMNS - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
    // (0.4.1) Keep hologram toggle pinned outside the scroll body right above actions so it is never hidden below tiles.
    FoldReveal(view.holoVisible) {
        SwitchRow(
            title = ImageCopy.HOLO,
            checked = holo,
            onCheckedChange = {
                holoChoice = it
                if (it) holoSweep++
                // Cover kept, no new choice: the switch writes at once (U-08 LIB-84 `[PROPOSED]`).
                if (view.coverLine != null && view.current != null && selected == view.initialSource && !readOnly) vm.setHolo(owner, it)
            },
            subtitle = ImageCopy.HOLO_SUB,
            modifier = Modifier.padding(top = 8.dp),
        )
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
                    is PickAction.FromGallery -> {
                        val item = view.gallery.firstOrNull { it.item?.key == action.itemKey }?.item
                        when {
                            item == null -> Unit
                            readOnly -> sayReadOnly()
                            else -> when (val pick = ImagePickModel.galleryPick(item)) {
                                is GalleryPick.Preset -> vm.placePreset(owner, pick.presetId, holoChoice ?: pick.holo) { r ->
                                    if (r == EditResult.READ_ONLY) sayReadOnly() else closeSheet()
                                }
                                // Nothing is copied: the cover points at the same original with its own crops (LIB-85).
                                is GalleryPick.Original -> toCrop(pick.blobHash, null, null, holoChoice ?: pick.holo, pick.crops, pick.hiddenNote)
                            }
                        }
                    }
                }
            },
            variant = ButtonVariant.PRIMARY,
            enabled = selected != null,
        )
    }
}

/** Decode size of a gallery tile's 3:4 crop in the cover picker. */
private const val GALLERY_TILE_SIDE = 360

/**
 * The cover picker's current line (U-08 LIB-84, height 64): the 48 dp 1:1 thumbnail, `지금 표지 · <word>`
 * (or `표지 없음 · 첫 글자 카드`), then `자르기` (originals) and `빼기` as visible text buttons.
 */
@Composable
private fun CoverCurrentLine(line: CoverLine, onCrop: (() -> Unit)?, onClear: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumb(line.thumb)
        CivilightText(line.text, CivilightTheme.type.small, Modifier.weight(1f), color = colors.ink2, maxLines = 1)
        onCrop?.let { TextAction(ImageCopy.BTN_CROP, it) }
        onClear?.let { TextAction(ImageCopy.BTN_CLEAR, it) }
    }
}
