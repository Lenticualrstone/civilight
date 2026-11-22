package io.github.lenticualrstone.civilight.feature.canvas.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import io.github.lenticualrstone.civilight.designsystem.component.ImageTile
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.domain.actions.GalleryMove
import io.github.lenticualrstone.civilight.domain.actions.GalleryOwner
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.image.CropHandOff
import io.github.lenticualrstone.civilight.feature.canvas.image.MineThumb
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetArt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.GalleryAddSheet
import io.github.lenticualrstone.civilight.navigation.GalleryInfoSheet
import io.github.lenticualrstone.civilight.navigation.GalleryMenuSheet
import io.github.lenticualrstone.civilight.navigation.ImageCrop
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.richtext.RichTextEditor
import io.github.lenticualrstone.civilight.ui.richtext.rememberRichTextEditorState
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * What the gallery screen lends the sheets it opens (the frozen sheet keys carry only item keys): the view the
 * user sees (positions, sort, filter), the in-screen modes (`보기`, `자르기`, `순서 바꾸기`) and the pick. Null when
 * a sheet is opened from another screen; then a sheet works from the gallery's default view (`전체`, `직접 순서`)
 * and opens the gallery route for the in-screen modes.
 */
class GalleryHost(
    val view: GalleryView?,
    val openViewer: (Key) -> Unit,
    val openCrop: (Key) -> Unit,
    val openOrder: (Key) -> Unit,
    /** Arrive with [Key] picked (after adding, GAL-11). */
    val pick: (Key) -> Unit,
    /** The pick before an add, to put back on `되돌리기` (GAL-11). */
    val currentPick: () -> Key?,
    val restorePick: (Key?) -> Unit,
)

val LocalGalleryHost = compositionLocalOf<GalleryHost?> { null }

/** `다른 곳에도 넣기` (GAL-16): the owner picker for [itemKeys]. Feature-local. */
@Serializable
data class GalleryCopySheet(val projectKey: Key, val itemKeys: List<Key>) : SheetKey

/** `씬 고르기` · `바꾸기` of the info sheet's `연결` (GAL-13). Feature-local. */
@Serializable
data class GallerySceneSheet(val projectKey: Key, val itemKey: Key) : SheetKey

/** `여러 장 고르기 › 태그` (GAL-18): project tags on several items. Feature-local. */
@Serializable
data class GalleryTagsSheet(val projectKey: Key, val itemKeys: List<Key>) : SheetKey

/** The sheets' shared prelude: the edit view model, the document, the lock gate. */
private class SheetCtx(val deps: FeatureDeps, val vm: GalleryViewModel, val doc: ProjectDoc?, val readOnly: Boolean, val gate: HiddenGate) {
    val locked: Boolean get() = !gate.open

    fun sayReadOnly() = deps.toasts.show(GalleryUiCopy.READ_ONLY, actionLabel = null)

    /** The view a sheet counts positions in: the screen's, else the owner's default view. */
    fun viewOf(host: GalleryHost?, item: GalleryItem): GalleryView? {
        val d = doc ?: return null
        host?.view?.takeIf { it.entry(item.key) != null }?.let { return it }
        val owner = GalleryOwner.of(item)
        return GalleryRows.build(d, owner.kind, owner.key, locked, GalleryFilter.All, GallerySort.MANUAL)
    }
}

@Composable
private fun rememberSheetCtx(projectKey: Key): SheetCtx {
    val deps = LocalFeatureDeps.current
    val vm = entryViewModel(key = GalleryViewModel.sheetKey(projectKey)) {
        GalleryViewModel(projectKey, deps.projects, deps::canvasEditContext, deps.stores.blobs)
    }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready
    val gate = rememberHiddenGate(projectKey)
    return SheetCtx(deps, vm, ready?.doc, ready?.readOnly == true, gate)
}

/** Shows an add or copy toast with `되돌리기`, which also puts the previous pick back (GAL-11, T-GAL-33). */
private fun showAddToast(c: SheetCtx, outcome: GalleryAddOutcome, host: GalleryHost?, previous: Key?) {
    when {
        outcome.result == EditResult.READ_ONLY -> c.sayReadOnly()
        outcome.toast != null -> {
            val t = outcome.toast
            if (t.action != null) c.deps.toasts.show(t.message, actionLabel = t.action) {
                c.vm.undoLast()
                host?.restorePick(previous)
            } else c.deps.toasts.show(t.message, actionLabel = null)
        }
    }
}

// ---------------------------------------------------------------- 갤러리에 넣기 (GAL-11)

/** Three columns of tiles, 9 apart (the picker's grid, board `GalAdd`). */
private const val COLUMNS = 3
private const val TILE_ASPECT = 108f / 144f
private const val DIMMED = 0.38f

/**
 * `갤러리에 넣기` (GAL-11, board `GalAdd`): the CNV-27 picker in a several-at-once mode. Tiles toggle with a numbered
 * badge (the order they are added in); a source this gallery already holds reads `이미 있음` or `휴지통에 있음` and
 * cannot be chosen. `사진` · `파일` take several at once and add them right away in the order the system returns
 * them; `넣기 · <k>` adds the chosen tiles. Each add is one undo step; the sheet closes and the first new item is
 * picked.
 */
@Composable
fun SheetScope.GalleryAddSheetContent(sheet: GalleryAddSheet) {
    val c = rememberSheetCtx(sheet.projectKey)
    val host = LocalGalleryHost.current
    val settings by c.deps.settings.collectAsState()
    val doc = c.doc
    val owner = GalleryOwner(sheet.ownerKind, sheet.ownerKey)
    var chosen by rememberSaveable { mutableStateOf(listOf<String>()) }
    var tab by rememberSaveable { mutableIntStateOf(GalleryAddModel.TAB_PRESETS) }
    val model = remember(doc, c.locked, chosen) { doc?.let { GalleryAddModel.build(it, owner, c.locked, chosen, c.deps.stores.blobs::contains) } }
    if (doc == null || model == null) {
        SheetHeader(GalleryUiCopy.ADD_TITLE, onClose = ::close, closeLabel = GalleryUiCopy.BTN_CLOSE)
        return
    }
    val context = LocalContext.current
    val holo = settings.holoDefaultForNewImages
    val closeSheet: () -> Unit = { close() }

    fun finish(outcome: GalleryAddOutcome?) {
        if (outcome == null) {
            c.deps.toasts.show(io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy.IMAGE_IMPORT_FAILED, actionLabel = null)
            return
        }
        val previous = host?.currentPick?.invoke()
        showAddToast(c, outcome, host, previous)
        if (outcome.result == EditResult.APPLIED) {
            closeSheet()
            outcome.added.firstOrNull()?.let { first -> host?.pick?.invoke(first) }
        }
    }

    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) c.vm.importAndAdd(context.contentResolver, uris, BlobSource.GALLERY, owner, c.locked, holo, ::finish)
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) c.vm.importAndAdd(context.contentResolver, uris, BlobSource.FILE, owner, c.locked, holo, ::finish)
    }

    SheetHeader(model.title, onClose = ::close, subtitle = model.subtitle, closeLabel = GalleryUiCopy.BTN_CLOSE)
    CivilightTabs(model.tabs, tab, { tab = it }, Modifier.padding(top = 6.dp))
    SheetScrollBody(Modifier.padding(top = 12.dp)) {
        ContentSwap(tab, label = "galleryAddTab") { shown ->
            val tiles = if (shown == GalleryAddModel.TAB_PRESETS) model.presets else model.mine
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (tiles.isEmpty()) {
                    CivilightText(
                        io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy.MY_IMAGES_EMPTY,
                        CivilightTheme.type.small,
                        Modifier.padding(vertical = 24.dp),
                        color = CivilightTheme.colors.ink3,
                    )
                }
                tiles.chunked(COLUMNS).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        row.forEach { tile -> AddTileView(tile, { chosen = GalleryAddModel.toggle(model.chosen, tile) }, Modifier.weight(1f)) }
                        repeat(COLUMNS - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
        CivilightText(GalleryUiCopy.ADD_NOTE, CivilightTheme.type.small, Modifier.padding(top = 14.dp), color = CivilightTheme.colors.ink3)
    }
    SheetActions {
        CivilightButton(
            GalleryUiCopy.BTN_PHOTO,
            { if (c.readOnly) c.sayReadOnly() else photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            variant = ButtonVariant.GHOST,
            icon = IconId.Image,
        )
        CivilightButton(
            GalleryUiCopy.BTN_FILE,
            { if (c.readOnly) c.sayReadOnly() else files.launch(arrayOf("image/*")) },
            variant = ButtonVariant.GHOST,
            icon = IconId.File,
        )
        Box(Modifier.weight(1f))
        CivilightButton(
            model.button,
            {
                if (c.readOnly) c.sayReadOnly()
                else c.vm.add(owner, GalleryAddModel.sources(doc, model.chosen), c.locked, holo, ::finish)
            },
            variant = ButtonVariant.PRIMARY,
            enabled = model.canAdd,
        )
    }
}

/** A tile of the add sheet: ring and numbered badge when chosen; dimmed with its mark when not choosable. */
@Composable
private fun AddTileView(tile: AddTile, onClick: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val badge = animatedFraction(tile.number != null, label = "addBadge")
    ImageTile(
        name = tile.caption,
        selected = tile.number != null,
        onClick = { if (tile.choosable) onClick() },
        modifier = modifier,
        aspectRatio = TILE_ASPECT,
    ) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = if (tile.choosable) 1f else DIMMED }) {
            tile.preset?.let { PresetArt(it) }
            tile.blob?.let { blob -> MineThumb(blob.hash) {} }
        }
        tile.mark.label?.let { ImageStatusChip(it, Modifier.align(Alignment.BottomStart).padding(6.dp)) }
        // The badge stays composed while it fades out, showing its last number.
        val n = rememberLastNonNull(tile.number)
        if (n != null && (tile.number != null || badge.value > 0f)) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .graphicsLayer { alpha = badge.value }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.acc),
                contentAlignment = Alignment.Center,
            ) {
                CivilightText(n.toString(), CivilightTheme.type.number, color = colors.onpri, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- 이미지 정보 (GAL-13)

/**
 * `이미지 정보` (GAL-13, board `GalInfo`): head (thumbnail, `<W> × <H> · <size> · <source>`, `<owner> · 갤러리
 * <i>/<N> · 원본 그대로`), then `제목`, `설명` (shared rich editor with `@` links), `태그` (project tags; `+ 태그`
 * through `CharacterEdits.addTag`) and `연결` (one scene). Saves as typed; one undo step per field edit.
 */
@Composable
fun SheetScope.GalleryInfoSheetContent(sheet: GalleryInfoSheet) {
    val c = rememberSheetCtx(sheet.projectKey)
    val host = LocalGalleryHost.current
    val sheetScope = this
    val doc = c.doc
    val item = doc?.index?.galleryItem(sheet.itemKey)?.takeIf { doc.liveness.isLive(it) }
    if (doc == null || item == null) {
        SheetHeader(GalleryUiCopy.INFO_TITLE, onClose = ::close, closeLabel = GalleryUiCopy.BTN_CLOSE)
        if (doc != null) LaunchedEffect(Unit) { close() }
        return
    }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val view = c.viewOf(host, item)
    val head = remember(doc, item, view) { GalleryInfo.head(doc, item, view) }
    val editable = !c.readOnly

    SheetHeader(GalleryUiCopy.INFO_TITLE, onClose = ::close, closeLabel = GalleryUiCopy.BTN_CLOSE)
    SheetScrollBody(Modifier.padding(top = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))) { GalleryImage(item, Ratio.R1X1, GalleryImageSizes.THUMB) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CivilightText(head.facts, type.number, color = colors.ink2, maxLines = 1)
                    CivilightText(head.small, type.small, color = colors.ink3, maxLines = 1)
                }
            }
            // 제목: one line, written through as typed (one merged undo step).
            var title by rememberSaveable(sheet.itemKey) { mutableStateOf(item.title) }
            CivilightTextField(
                title,
                {
                    title = it
                    c.vm.setTitle(item.key, it)
                },
                Modifier.fillMaxWidth(),
                label = GalleryUiCopy.FIELD_TITLE,
                enabled = editable,
            )
            // 설명: the shared run contract; never re-seeded from the document while open.
            val caption = rememberRichTextEditorState(item.caption ?: emptyList(), key = sheet.itemKey)
            LaunchedEffect(caption) {
                snapshotFlow { caption.result }.drop(1).collect { c.vm.setCaption(item.key, it) }
            }
            val focus = remember { FocusRequester() }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CivilightText(GalleryUiCopy.FIELD_DESCRIPTION, type.label, Modifier.weight(1f), color = colors.ink2)
                    CivilightText(GalleryUiCopy.LINK_HINT, type.small, color = colors.ink3, maxLines = 1)
                }
                RichTextEditor(
                    state = caption,
                    projectKey = sheet.projectKey,
                    linkPickerTitle = GalleryUiCopy.LINK_PICKER_TITLE,
                    openSheet = sheetScope::open,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    enabled = editable,
                    minHeight = 120.dp,
                )
            }
            if (sheet.focusDescription && editable) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            InfoTags(c, item, editable)
            InfoLink(c, doc, item, editable) { sheetScope.open(GallerySceneSheet(sheet.projectKey, item.key)) }
        }
    }
}

/** `태그`: project tags as chips (tap toggles), `+ 태그` reveals a name line with `넣기`. */
@Composable
private fun InfoTags(c: SheetCtx, item: GalleryItem, editable: Boolean) {
    val doc = c.doc ?: return
    var adding by rememberSaveable(item.key) { mutableStateOf(false) }
    var name by rememberSaveable(item.key) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(GalleryUiCopy.FIELD_TAGS, CivilightTheme.type.label, color = CivilightTheme.colors.ink2)
        ChipFlow {
            CharacterEdits.tags(doc).forEach { tag ->
                FilterChip(tag.name, tag.key in item.tagKeys, { c.vm.setTags(listOf(item.key)) { GalleryInfo.toggleTag(it, tag.key) } }, enabled = editable)
            }
            if (editable) AddChip(GalleryUiCopy.FIELD_TAGS, { adding = !adding })
        }
        FoldReveal(adding && editable, label = "galleryNewTag") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                CivilightTextField(name, { name = it }, Modifier.weight(1f), label = GalleryUiCopy.FIELD_NAME)
                CivilightButton(GalleryUiCopy.BTN_ADD, {
                    c.vm.addTag(listOf(item.key), name)
                    name = ""
                    adding = false
                }, variant = ButtonVariant.GHOST, enabled = name.isNotBlank())
            }
        }
    }
}

/** `연결`: `씬` · `1-7 씬 3 · 2일째 새벽` with `빼기` and `바꾸기`; empty → `씬 고르기`. */
@Composable
private fun InfoLink(c: SheetCtx, doc: ProjectDoc, item: GalleryItem, editable: Boolean, pickScene: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val line = remember(doc, item.sceneKey, c.locked) { GalleryPanels(doc, c.locked).sceneLine(item.sceneKey) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(GalleryUiCopy.FIELD_LINK, type.label, color = colors.ink2)
        ContentSwap(line, contentKey = { it?.text }, label = "galleryLink") { shown ->
            if (shown == null) {
                CivilightButton(GalleryUiCopy.BTN_PICK_SCENE, pickScene, variant = ButtonVariant.GHOST, enabled = editable)
            } else {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.tint).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        CivilightText(GalleryUiCopy.SCENE, type.small, color = colors.ink3)
                        CivilightText(shown.text, type.number, color = colors.ink, maxLines = 1)
                    }
                    if (editable) {
                        TextAction(GalleryUiCopy.BTN_UNLINK, { c.vm.setScene(item.key, null) })
                        TextAction(GalleryUiCopy.BTN_CHANGE, pickScene, color = colors.acc)
                    }
                }
            }
        }
    }
}

/** `씬 고르기` (GAL-13): the story structure picker limited to scenes, search by code and title. */
@Composable
fun SheetScope.GallerySceneSheetContent(sheet: GallerySceneSheet) {
    val c = rememberSheetCtx(sheet.projectKey)
    val doc = c.doc ?: return
    val item = doc.index.galleryItem(sheet.itemKey)
    UnitPickerSheet(
        doc = doc,
        gate = c.gate,
        title = GalleryUiCopy.BTN_PICK_SCENE,
        onPick = { key ->
            c.vm.setScene(sheet.itemKey, key)
            close()
        },
        onClose = ::close,
        selectedKey = item?.sceneKey,
        levels = setOf(doc.project.structure.levels.lastIndex),
    )
}

// ---------------------------------------------------------------- 메뉴 (GAL-14 … GAL-20)

/**
 * The `⋯` menu (GAL-14, board `GalMore`): title, `<owner> · 갤러리 <i>/<N>`, `보기` · `고치기`, then `초상으로` /
 * `표지로` · `배경으로` · `자르기` · `다른 곳에도 넣기` · `순서` (`맨 앞` · `앞으로` · `뒤로`) · `순서 바꾸기` ·
 * `홀로그램 효과` · `숨기기` · `지우기`.
 */
@Composable
fun SheetScope.GalleryMenuSheetContent(sheet: GalleryMenuSheet) {
    val c = rememberSheetCtx(sheet.projectKey)
    val host = LocalGalleryHost.current
    val sheetScope = this
    val scope = rememberCoroutineScope()
    val doc = c.doc
    val item = doc?.index?.galleryItem(sheet.itemKey)
    val view = item?.let { c.viewOf(host, it) }
    val menu = remember(doc, view, sheet.itemKey, c.locked) { if (doc != null && view != null) GalleryMenus.menu(doc, view, sheet.itemKey, c.locked) else null }
    if (doc == null || item == null || menu == null) {
        SheetHeader(GalleryUiCopy.UNTITLED, onClose = ::close, closeLabel = GalleryUiCopy.BTN_CLOSE)
        // Deleted, hidden by a relock: nothing to act on.
        if (doc != null) LaunchedEffect(Unit) { close() }
        return
    }
    val deps = c.deps
    val editable = !c.readOnly
    val ownerRoute = GalleryOwner.of(item).let { o -> Gallery(sheet.projectKey, o.kind, o.key, item.key) }

    fun inScreen(action: ((Key) -> Unit)?) {
        close()
        if (action != null) action(item.key) else deps.navigator.push(ownerRoute)
    }

    fun guarded(block: () -> Unit) = if (c.readOnly) c.sayReadOnly() else block()

    SheetHeader(menu.title, onClose = ::close, subtitle = menu.small, closeLabel = GalleryUiCopy.BTN_CLOSE)
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightButton(GalleryUiCopy.BTN_VIEW, { inScreen(host?.openViewer) }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(GalleryUiCopy.BTN_EDIT, { sheetScope.open(GalleryInfoSheet(sheet.projectKey, item.key)) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = editable)
    }
    SheetScrollBody(Modifier.padding(top = 10.dp)) {
        menu.main.forEach { row ->
            ListRow(
                title = row.target.label,
                subtitle = row.sub,
                trailing = { MenuTrailing(if (row.current) row.target.now else null) },
                onClick = { guarded { runMain(c, item, row.target, sheetScope::close) } },
            )
        }
        ListRow(
            title = GalleryUiCopy.BTN_CROP,
            subtitle = menu.cropSub,
            modifier = Modifier.alpha(if (menu.cropEnabled) 1f else DIMMED),
            trailing = { MenuTrailing(null) },
            onClick = if (menu.cropEnabled) ({ guarded { inScreen(host?.openCrop) } }) else null,
        )
        ListRow(
            title = GalleryUiCopy.BTN_COPY_TO,
            subtitle = GalleryUiCopy.COPY_TO_SUB,
            trailing = { MenuTrailing(null) },
            onClick = { guarded { sheetScope.open(GalleryCopySheet(sheet.projectKey, listOf(item.key))) } },
        )
        OrderRow(menu.order, enabled = true) { move -> guarded { c.vm.move(item.key, move, c.locked) } }
        ListRow(
            title = GalleryUiCopy.BTN_REORDER,
            subtitle = GalleryUiCopy.REORDER_SUB,
            trailing = { MenuTrailing(null) },
            onClick = { guarded { inScreen(host?.openOrder) } },
        )
        SwitchRow(
            title = GalleryUiCopy.HOLO,
            checked = menu.holo,
            onCheckedChange = { on -> guarded { c.vm.setHolo(item.key, on) } },
            subtitle = GalleryUiCopy.HOLO_SUB,
            enabled = editable,
        )
        SwitchRow(
            title = GalleryUiCopy.BTN_HIDE,
            checked = menu.hidden,
            onCheckedChange = { on ->
                guarded {
                    if (on) {
                        // Locked, the image folds into the lock tile at once (GAL-19); nothing else is opened.
                        c.vm.setHidden(listOf(item.key), true)
                    } else {
                        // Unhiding needs authentication (LIB-32, GAL-19).
                        scope.launch {
                            if (deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS) {
                                c.vm.setHidden(listOf(item.key), false)
                            }
                        }
                    }
                }
            },
            subtitle = GalleryUiCopy.HIDE_SUB,
            enabled = editable,
        )
        ListRow(
            title = GalleryUiCopy.BTN_DELETE,
            subtitle = GalleryUiCopy.DELETE_SUB,
            onClick = {
                guarded {
                    sheetScope.close()
                    deleteWithToast(c, listOf(item.key))
                }
            },
        )
    }
}

/** `지우기` with `이미지 지움 · <title>` and `되돌리기` (GAL-20, T-GAL-14). */
private fun deleteWithToast(c: SheetCtx, keys: List<Key>) {
    c.vm.delete(keys) { r, toast ->
        when {
            r == EditResult.READ_ONLY -> c.sayReadOnly()
            toast != null -> c.deps.toasts.show(toast.message, actionLabel = toast.action) { c.vm.undoLast() }
        }
    }
}

/** A main-image row (GAL-14): a preset goes in at once; an original opens the slot's crop editor. */
private fun runMain(c: SheetCtx, item: GalleryItem, target: MainTarget, close: () -> Unit) {
    when (val plan = GalleryMenus.mainPlan(item, target)) {
        is MainPlan.PlacePreset -> c.vm.placePreset(plan.owner, plan.presetId, plan.holo, target.label) { r ->
            if (r == EditResult.READ_ONLY) c.sayReadOnly() else close()
        }
        is MainPlan.Crop -> {
            CropHandOff.put(CropHandOff(plan.owner.key, plan.owner.slot, plan.blobHash, null, plan.holo, null, plan.note))
            close()
            c.deps.navigator.push(ImageCrop(c.vm.projectKey, plan.owner.kind, plan.owner.key, plan.owner.slot, plan.blobHash))
        }
    }
}

/** `지금 초상` in place of the chevron, or the chevron. */
@Composable
private fun MenuTrailing(now: String?) {
    // `지금 초상` and the chevron swap with a crossfade when the slot changes elsewhere.
    ContentSwap(now, label = "menuTrailing") { shown ->
        if (shown != null) CivilightText(shown, CivilightTheme.type.small, color = CivilightTheme.colors.acc, maxLines = 1)
        else CivilightIcon(IconId.Next, null, size = 16.dp, tint = CivilightTheme.colors.ink3)
    }
}

/** `순서` · `3/8 · 직접 순서` with `맨 앞` · `앞으로` · `뒤로` (GAL-15); disabled with the reason otherwise. */
@Composable
private fun OrderRow(order: OrderRowModel, enabled: Boolean, onMove: (GalleryMove) -> Unit) {
    val colors = CivilightTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(1f)) {
            CivilightText(GalleryUiCopy.ORDER, CivilightTheme.type.itemTitle, color = colors.ink, maxLines = 1)
            CivilightText(order.text, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
        }
        CivilightButton(GalleryUiCopy.BTN_FIRST, { onMove(GalleryMove.FIRST) }, variant = ButtonVariant.COMPACT_GHOST, enabled = enabled && order.canFirst)
        CivilightButton(GalleryUiCopy.BTN_FORWARD, { onMove(GalleryMove.FORWARD) }, variant = ButtonVariant.COMPACT_GHOST, enabled = enabled && order.canForward)
        CivilightButton(GalleryUiCopy.BTN_BACKWARD, { onMove(GalleryMove.BACKWARD) }, variant = ButtonVariant.COMPACT_GHOST, enabled = enabled && order.canBackward)
    }
}

// ---------------------------------------------------------------- 다른 곳에도 넣기 (GAL-16)

/** The owner picker (GAL-16): the project and visible characters with search; `이미 있음` cannot be chosen. */
@Composable
fun SheetScope.GalleryCopySheetContent(sheet: GalleryCopySheet) {
    val sheetScope = this
    val c = rememberSheetCtx(sheet.projectKey)
    val doc = c.doc ?: return
    var query by rememberSaveable(stateSaver = TextValueSaver) { mutableStateOf(TextFieldValue("")) }
    val targets = remember(doc, sheet.itemKeys, c.locked, query.text) { GalleryCopyTargets.targets(doc, sheet.itemKeys, c.locked, query.text) }
    SheetHeader(GalleryUiCopy.BTN_COPY_TO, onClose = ::close, subtitle = GalleryUiCopy.COPY_TO_SUB, closeLabel = GalleryUiCopy.BTN_CLOSE)
    SearchField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 10.dp))
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        targets.forEach { t ->
            ListRow(
                title = t.name,
                subtitle = t.mark.label ?: t.sub,
                modifier = Modifier.alpha(if (t.choosable) 1f else DIMMED),
                onClick = if (t.choosable) ({
                    if (c.readOnly) c.sayReadOnly()
                    else c.vm.copyTo(sheet.itemKeys, t.owner, c.locked) { o -> showAddToast(c, o, null, null) }
                    sheetScope.close()
                }) else null,
            )
        }
    }
}

private val TextValueSaver = androidx.compose.runtime.saveable.Saver<TextFieldValue, String>(save = { it.text }, restore = { TextFieldValue(it) })

// ---------------------------------------------------------------- 여러 장 고르기 › 태그 (GAL-18)

/** Project tags over several items: a chip is on when every item has it; a tap puts it on all, or takes it off all. */
@Composable
fun SheetScope.GalleryTagsSheetContent(sheet: GalleryTagsSheet) {
    val c = rememberSheetCtx(sheet.projectKey)
    val doc = c.doc ?: return
    val items = sheet.itemKeys.mapNotNull(doc.index::galleryItem)
    var name by rememberSaveable { mutableStateOf("") }
    SheetHeader(GalleryUiCopy.BTN_TAGS, onClose = ::close, subtitle = GalleryUiCopy.pickedCount(items.size), closeLabel = GalleryUiCopy.BTN_CLOSE)
    SheetScrollBody(Modifier.padding(top = 12.dp)) {
        ChipFlow {
            CharacterEdits.tags(doc).forEach { tag ->
                val all = items.isNotEmpty() && items.all { tag.key in it.tagKeys }
                FilterChip(tag.name, all, {
                    if (c.readOnly) c.sayReadOnly()
                    else c.vm.setTags(sheet.itemKeys) { keys -> if (all) keys - tag.key else if (tag.key in keys) keys else keys + tag.key }
                }, enabled = !c.readOnly)
            }
        }
        if (!c.readOnly) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                CivilightTextField(name, { name = it }, Modifier.weight(1f), label = GalleryUiCopy.FIELD_NAME)
                CivilightButton(GalleryUiCopy.BTN_ADD, {
                    c.vm.addTag(sheet.itemKeys, name)
                    name = ""
                }, variant = ButtonVariant.GHOST, enabled = name.isNotBlank())
            }
        }
    }
}

/** Multi-select `지우기` and `숨기기` from the screen (GAL-18): the same paths as the menu, one undo step each. */
object GalleryBatch {
    @Composable
    fun rememberActions(projectKey: Key): GalleryBatchActions {
        val c = rememberSheetCtx(projectKey)
        val scope = rememberCoroutineScope()
        return remember(c.vm, c.doc, c.locked, c.readOnly) {
            GalleryBatchActions(
                delete = { keys -> if (c.readOnly) c.sayReadOnly() else deleteWithToast(c, keys) },
                hide = { keys ->
                    if (c.readOnly) c.sayReadOnly()
                    else c.vm.setHidden(keys, true)
                },
                unhide = { keys ->
                    if (c.readOnly) c.sayReadOnly()
                    else scope.launch {
                        if (c.deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS) c.vm.setHidden(keys, false)
                    }
                },
            )
        }
    }
}

class GalleryBatchActions(val delete: (List<Key>) -> Unit, val hide: (List<Key>) -> Unit, val unhide: (List<Key>) -> Unit)
