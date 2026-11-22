package io.github.lenticualrstone.civilight.feature.map.list

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.sketch.SketchImages
import io.github.lenticualrstone.civilight.feature.map.sketch.StraightenScreen
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.navigation.MapEdit as MapEditRoute
import io.github.lenticualrstone.civilight.navigation.MapFormatPicker
import io.github.lenticualrstone.civilight.navigation.MapList
import io.github.lenticualrstone.civilight.navigation.MapView as MapViewRoute
import io.github.lenticualrstone.civilight.navigation.NodeMap
import io.github.lenticualrstone.civilight.store.blob.import
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 세계 › 지도 (WLD-36, board `MapNew`): header `<project> · 세계` / `지도` / `+`, section `지도 · N`,
 * a row of map cards (200 × 150 thumbnail, name, small line), and the `새 지도` sheet. Image and
 * sketch sources go through the system pickers and camera (WLD-38, WLD-39); `펴기` replaces the
 * list until the sketch map is made.
 */
@Composable
fun MapListScreen(route: MapList, deps: FeatureDeps) {
    val vm = entryViewModel { MapListViewModel(route.projectKey, MapEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // The parent of the map being made (a pin's `하위 지도`), kept across the picker round trip.
    var parentMap by rememberSaveable { mutableStateOf<String?>(null) }
    var parentPin by rememberSaveable { mutableStateOf<String?>(null) }
    var captureFile by rememberSaveable { mutableStateOf<String?>(null) }
    var sketchPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    fun parent(): MapParent? = if (parentMap != null && parentPin != null) MapParent(parentMap!!, parentPin!!) else null
    fun openMap(key: Key?) { if (key != null) deps.navigator.push(MapViewRoute(route.projectKey, key)) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { openMap(vm.importImage(context.contentResolver, uri, BlobSource.GALLERY, parent())) }
    }
    val openImageFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { openMap(vm.importImage(context.contentResolver, uri, BlobSource.FILE, parent())) }
    }
    val pickSketch = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val blob = runCatching { deps.stores.blobs.import(context.contentResolver, uri, BlobSource.SKETCH) }.getOrNull()
            if (blob == null) deps.toasts.show(MapCopy.TODO_COPY_MAP_IMPORT_FAILED, actionLabel = null) else sketchPhoto = Json.encodeToString(Blob.serializer(), blob)
        }
    }
    val shoot = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = captureFile
        captureFile = null
        if (path != null) scope.launch {
            val blob = if (ok) runCatching { SketchImages.importCapture(deps.stores.blobs, File(path)) }.getOrNull() else {
                File(path).delete()
                null
            }
            if (blob != null) sketchPhoto = Json.encodeToString(Blob.serializer(), blob)
        }
    }

    val photo = sketchPhoto?.let { runCatching { Json.decodeFromString(Blob.serializer(), it) }.getOrNull() }
    if (photo != null) {
        StraightenScreen(
            photo = photo,
            blobs = deps.stores.blobs,
            working = working,
            onStraighten = { corners ->
                working = true
                scope.launch {
                    try {
                        val result = runCatching { SketchImages.straightenToBlob(deps.stores.blobs, photo, corners) }
                            .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
                            .getOrNull()
                        val key = result?.let { runCatching { vm.createSketchMap(it, parent()) }.onFailure { e -> if (e is kotlinx.coroutines.CancellationException) throw e }.getOrNull() }
                        if (result == null) deps.toasts.show(MapCopy.TODO_COPY_MAP_IMPORT_FAILED, actionLabel = null)
                        if (key != null) {
                            sketchPhoto = null
                            deps.navigator.push(MapEditRoute(route.projectKey, key))
                        }
                    } finally {
                        working = false
                    }
                }
            },
            onCancel = { sketchPhoto = null },
        )
        return
    }

    CivilightScreen(
        title = MapCopy.LIST_TITLE,
        smallLine = ui?.small,
        back = mapBack(deps),
        actions = {
            CivilightIconButton(IconId.Plus, MapCopy.NEW_MAP, { sheets.open(NewMapSheet()) }, enabled = !readOnly)
        },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is NewMapSheet -> {
                    {
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
                                    NewMapAction.Generate -> deps.navigator.push(MapFormatPicker(route.projectKey))
                                    NewMapAction.Node -> scope.launch {
                                        vm.createNodeMap(parent())?.let { deps.navigator.push(NodeMap(route.projectKey, it)) }
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
                                            deps.toasts.show(MapCopy.TODO_COPY_MAP_IMPORT_FAILED, actionLabel = null)
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
            val list = ui ?: return@Column
            SectionLabel(list.section, Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp))
            if (list.cards.isEmpty()) {
                EmptyBlock(
                    title = list.section,
                    modifier = Modifier.padding(top = 48.dp),
                    primaryLabel = if (readOnly) null else MapCopy.NEW_MAP,
                    onPrimary = if (readOnly) null else { { sheets.open(NewMapSheet()) } },
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(list.cards, key = { it.key }) { card ->
                        MapCard(card) { deps.navigator.push(MapViewRoute(route.projectKey, card.key)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapCard(card: MapCardUi, onOpen: () -> Unit) {
    val type = CivilightTheme.type
    Column(Modifier.width(200.dp).pressable(onClick = onOpen, role = Role.Button)) {
        Box(Modifier.size(200.dp, 150.dp).clip(RoundedCornerShape(16.dp))) {
            MapCanvas(base = card.base, scene = card.scene, viewport = Viewport(), onViewportChange = {}, interactive = false, showRouteChips = false)
        }
        CivilightText(card.name, type.itemTitle, Modifier.padding(top = 8.dp), maxLines = 1)
        CivilightText(card.line, type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
    }
}
