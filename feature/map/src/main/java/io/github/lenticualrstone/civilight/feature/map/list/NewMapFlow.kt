package io.github.lenticualrstone.civilight.feature.map.list

import io.github.lenticualrstone.civilight.feature.map.common.MapOffers
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.MapNav
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.sketch.SketchImages
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.navigation.MapFormatPicker
import io.github.lenticualrstone.civilight.navigation.NodeMap
import io.github.lenticualrstone.civilight.navigation.SketchStraighten
import io.github.lenticualrstone.civilight.store.blob.import
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import kotlinx.coroutines.launch
import java.io.File

/** The `새 지도` sheet and its system pickers, usable from any map screen (WLD-36, WLD-48). */
class NewMapFlow internal constructor(
    private val sheets: SheetHost,
    val content: @Composable SheetScope.(NewMapSheet) -> Unit,
    private val pickImage: (MapParent?) -> Unit,
) {
    /**
     * (0.2.0, WLD-75) A new map is one image plus pins: this opens the system image picker at once, and
     * a picked image becomes an `IMAGE` map (`지도 N`, a child map of [parent] when given) that opens in
     * 지도 보기. Cancelling the picker creates nothing. The old `새 지도` sheet is no longer opened.
     */
    fun open(parent: MapParent? = null) {
        if (MapOffers.NEW_MAP_IMAGE_ONLY) pickImage(parent) else sheets.open(NewMapSheet(parent?.mapKey, parent?.pinKey))
    }
}

/**
 * The sheet plus the gallery, file and camera launchers of a new map, owned by the screen that
 * shows it. The parent chosen in the sheet is kept (saveable) across the picker round trip. A sketch
 * photo goes on to `펴기` (`SketchStraighten`); generated maps go to `양식` (`MapFormatPicker`) with
 * the parent; image and node maps open at once.
 */
@Composable
fun rememberNewMapFlow(projectKey: Key, deps: FeatureDeps, vm: MapScreenViewModel, sheets: SheetHost): NewMapFlow {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    var parentMap by rememberSaveable { mutableStateOf<String?>(null) }
    var parentPin by rememberSaveable { mutableStateOf<String?>(null) }
    var captureFile by rememberSaveable { mutableStateOf<String?>(null) }
    fun parent(): MapParent? {
        val m = parentMap
        val p = parentPin
        return if (m != null && p != null) MapParent(m, p) else null
    }
    fun opened(key: Key?) {
        if (key != null) MapNav.open(deps.navigator, vm.doc.value, projectKey, key)
    }
    fun straighten(photo: Blob) = deps.navigator.push(SketchStraighten(projectKey, photo.hash, parentMap, parentPin))

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { opened(vm.importImage(context.contentResolver, uri, BlobSource.GALLERY, parent())) }
    }
    val openImageFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { opened(vm.importImage(context.contentResolver, uri, BlobSource.FILE, parent())) }
    }
    val pickSketch = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val blob = runCatching { deps.stores.blobs.import(context.contentResolver, uri, BlobSource.SKETCH) }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
                .getOrNull()
            if (blob == null) deps.toasts.show(MapCopy.IMPORT_FAILED, actionLabel = null) else straighten(blob)
        }
    }
    val shoot = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = captureFile
        captureFile = null
        if (path != null) scope.launch {
            val blob = if (ok) {
                runCatching { SketchImages.importCapture(deps.stores.blobs, File(path)) }
                    .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
                    .getOrNull()
            } else {
                File(path).delete()
                null
            }
            if (blob != null) straighten(blob) else if (ok) deps.toasts.show(MapCopy.IMPORT_FAILED, actionLabel = null)
        }
    }

    val content: @Composable SheetScope.(NewMapSheet) -> Unit = { key ->
        val parentUi = doc?.let { MapListState.parent(it, key.parentMapKey, key.parentPinKey) }
        NewMapSheetContent(
            parent = parentUi,
            readOnly = readOnly,
            onClose = ::close,
            onAction = { action ->
                parentMap = parentUi?.parent?.mapKey
                parentPin = parentUi?.parent?.pinKey
                close()
                when (action) {
                    NewMapAction.Generate -> deps.navigator.push(MapFormatPicker(projectKey, parentMap, parentPin))
                    NewMapAction.Node -> scope.launch {
                        vm.createNodeMap(parent())?.let { deps.navigator.push(NodeMap(projectKey, it)) }
                    }
                    NewMapAction.ImageFromGallery -> pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    NewMapAction.ImageFromFile -> openImageFile.launch(arrayOf("image/*"))
                    NewMapAction.SketchFromGallery -> pickSketch.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    NewMapAction.SketchShoot -> {
                        val (file, uri) = SketchImages.newCaptureTarget(context)
                        captureFile = file.path
                        runCatching { shoot.launch(uri) }.onFailure {
                            captureFile = null
                            file.delete()
                            deps.toasts.show(MapCopy.IMPORT_FAILED, actionLabel = null)
                        }
                    }
                }
            },
        )
    }
    val pickNewImage: (MapParent?) -> Unit = { parent ->
        if (readOnly) {
            deps.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
        } else {
            parentMap = parent?.mapKey
            parentPin = parent?.pinKey
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
    return NewMapFlow(sheets, content, pickNewImage)
}
