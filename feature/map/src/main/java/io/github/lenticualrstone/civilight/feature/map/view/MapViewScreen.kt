package io.github.lenticualrstone.civilight.feature.map.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
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
import io.github.lenticualrstone.civilight.designsystem.component.Breadcrumb
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ScaleBar
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.MapHit
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.CardHeader
import io.github.lenticualrstone.civilight.feature.map.common.CardRow
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapNav
import io.github.lenticualrstone.civilight.feature.map.export.rememberMapExporter
import io.github.lenticualrstone.civilight.feature.map.scale.ScaleBars
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.model.app.LargeMapExport
import io.github.lenticualrstone.civilight.navigation.MapScale as MapScaleRoute
import io.github.lenticualrstone.civilight.navigation.MapReveal as MapRevealRoute
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalDensity
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.common.MapStyleSheet
import io.github.lenticualrstone.civilight.feature.map.common.rememberMapMenuHost
import io.github.lenticualrstone.civilight.designsystem.component.LayerFlagChip
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.MapEdit as MapEditRoute
import io.github.lenticualrstone.civilight.navigation.MapView as MapViewRoute
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.launch

/**
 * 지도 보기 (WLD-37). Cover: the map under the glass bars, breadcrumb and zoom label at the top,
 * layer chips and the scale bar at the bottom, and the place sheet of a tapped pin (`핀 옮기기 ·
 * 장소 카드 열기`). Inner: the same map with the place as a 270 dp side card. Both are one element
 * driven by the saved selection, so folding keeps it; centre and zoom are saved too.
 */
@Composable
fun MapViewScreen(route: MapViewRoute, deps: FeatureDeps) {
    val vm = entryViewModel { MapViewViewModel(route.projectKey, route.mapKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val sheets = rememberSheetHost()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val inner = LocalWindowLayout.current.isInner

    var viewportArray by rememberSaveable(route.mapKey) { mutableStateOf(Viewport.UNSET.toArray()) }
    var selectedPin by rememberSaveable(route.mapKey) { mutableStateOf(route.focusPinKey) }
    var layerFilter by rememberSaveable(route.mapKey) { mutableStateOf<String?>(null) }
    var movingPin by rememberSaveable(route.mapKey) { mutableStateOf<String?>(null) }
    var movingTo by rememberSaveable(route.mapKey) { mutableStateOf<FloatArray?>(null) }
    var focused by rememberSaveable(route.mapKey) { mutableStateOf(route.focusPinKey == null) }
    val viewport = Viewport.fromArray(viewportArray)
    var contentWidth by remember { mutableFloatStateOf(0f) }
    val exporter = rememberMapExporter(deps)
    val barPx = with(LocalDensity.current) { 96.dp.toPx() }
    val colors = CivilightTheme.colors
    val settings by deps.settings.collectAsState()
    val menu = rememberMapMenuHost(route.projectKey, route.mapKey, deps, vm, sheets)

    val d = doc
    val ui = remember(d, hiddenOpen, layerFilter, selectedPin, movingPin, movingTo?.toList()) {
        d?.let {
            MapViewState.build(
                it, route.mapKey, MapScreenViewModel.lensOf(it), hiddenOpen, layerFilter, selectedPin,
                movingPin?.let { k -> k to movingTo?.let { a -> Point(a[0], a[1]) } },
            )
        }
    }
    LaunchedEffect(ui != null) {
        val pin = route.focusPinKey?.let { ui?.scene?.pin(it) }
        if (!focused && pin != null) {
            viewportArray = Viewport(zoom = 2f).centeredOn(pin.at).toArray()
            focused = true
        }
    }
    val replaceBase = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { vm.replaceBase(context.contentResolver, uri) }
    }

    fun export(size: LargeMapExport) {
        val state = ui ?: return
        val map = d?.maps?.get(route.mapKey) ?: return
        exporter.export(state.base, state.scene, map.name, size)
    }

    CivilightScreen(
        title = ui?.title.orEmpty(),
        smallLine = ui?.small,
        back = mapBack(deps),
        lens = d?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        actions = { CivilightIconButton(IconId.More, MapCopy.MORE, { sheets.open(MapMoreSheet) }) },
        sheets = sheets,
        localSheets = { key ->
            menu.content(key, d, readOnly) ?: if (key != MapMoreSheet) null else {
                {
                    val map = d?.maps?.get(route.mapKey)
                    MapMoreContent(
                        rows = map?.let { MapMenu.rows(it, MapMenuScreen.VIEW) }.orEmpty(),
                        onRow = { action -> close(); menu.onRow(action, d) },
                        canScale = map != null && !MapScales.isNodeMap(map),
                        defaultSize = settings.largeMapExport,
                        exporting = exporter.exporting,
                        onScale = { close(); deps.navigator.push(MapScaleRoute(route.projectKey, route.mapKey)) },
                        onReveal = { close(); deps.navigator.push(MapRevealRoute(route.projectKey, route.mapKey)) },
                        onExport = { size -> close(); export(size) },
                        onClose = ::close,
                    )
                }
            }
        },
    ) { padding ->
        val state = ui ?: return@CivilightScreen
        Box(Modifier.fillMaxSize()) {
            MapCanvas(
                base = state.base,
                scene = state.scene,
                viewport = viewport,
                onViewportChange = { viewportArray = it.toArray() },
                onContentWidth = { contentWidth = it },
                selectedKey = state.moving?.pinKey ?: state.place?.pinKey,
                onTap = { p, hit ->
                    when {
                        state.moving != null -> movingTo = floatArrayOf(p.x, p.y)
                        hit is MapHit.Pin -> selectedPin = hit.key
                        else -> selectedPin = null
                    }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            Row(
                Modifier.fillMaxWidth().padding(top = padding.calculateTopPadding() + 4.dp, start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.crumbs.isNotEmpty()) {
                    Breadcrumb(state.crumbs.map { it.text }, onSegment = { i ->
                        state.crumbs[i].mapKey?.takeIf { it != route.mapKey }?.let { MapNav.crumb(deps.navigator, d, route.projectKey, it) }
                    })
                }
                Spacer(Modifier.weight(1f))
                if (viewport.zoom > 0f) CivilightText(MapCopy.zoomLabel(viewport.percent), CivilightTheme.type.number, color = CivilightTheme.colors.ink2)
            }
            val moving = state.moving
            val place = state.place
            when {
                moving != null -> MovePinBar(
                    title = moving.title,
                    canCommit = moving.to != null && !readOnly,
                    onCancel = { movingPin = null; movingTo = null },
                    onDone = {
                        val to = moving.to ?: return@MovePinBar
                        vm.movePin(moving.pinKey, to) { movingPin = null; movingTo = null }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                )
                place != null && !inner -> PlacePanel(
                    place = place, inner = false, readOnly = readOnly,
                    onClose = { selectedPin = null },
                    onMove = { movingPin = place.pinKey; movingTo = null },
                    onOpenCard = { deps.navigator.push(PlaceCard(route.projectKey, place.placeKey)) },
                    onChildMap = { MapNav.open(deps.navigator, d, route.projectKey, it) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth(),
                )
                else -> BottomTools(
                    state = state, readOnly = readOnly,
                    scale = d?.maps?.get(route.mapKey)?.let { ScaleBars.label(d, it, contentWidth, barPx) } ?: state.scale,
                    onChip = { layerFilter = it },
                    onReplaceBase = { replaceBase.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onEdit = { deps.navigator.push(MapEditRoute(route.projectKey, route.mapKey)) },
                    onStyle = { sheets.open(MapStyleSheet) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = padding.calculateBottomPadding() + 20.dp),
                )
            }
            if (place != null && inner && moving == null) {
                PlacePanel(
                    place = place, inner = true, readOnly = readOnly,
                    onClose = { selectedPin = null },
                    onMove = { movingPin = place.pinKey; movingTo = null },
                    onOpenCard = { deps.navigator.push(PlaceCard(route.projectKey, place.placeKey)) },
                    onChildMap = { MapNav.open(deps.navigator, d, route.projectKey, it) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + 40.dp, end = 16.dp).width(270.dp),
                )
            }
        }
    }
}

/** Layer chips (`전체 핀 · 정치 …`), `지도 이미지 바꾸기`, `편집`, and the scale bar. */
@Composable
private fun BottomTools(
    state: MapViewUi,
    readOnly: Boolean,
    scale: String?,
    onChip: (String?) -> Unit,
    onReplaceBase: () -> Unit,
    onEdit: () -> Unit,
    onStyle: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        scale?.let { ScaleBar(it) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            state.chips.forEach { chip -> FilterChip(chip.label, chip.selected, { onChip(chip.layerKey) }) }
            state.styleChip?.let { LayerFlagChip(MapCopy.STYLE, it, onStyle) }
            if (state.canReplaceBase) FilterChip(MapCopy.BTN_REPLACE_BASE, false, onReplaceBase, enabled = !readOnly)
            FilterChip(MapCopy.BTN_EDIT, false, onEdit, enabled = !readOnly)
        }
    }
}

/** The place of a tapped pin: cover sheet (`핀 옮기기 · 장소 카드 열기`) or inner side card (`장소 카드 · 핀 옮기기`). */
@Composable
private fun PlacePanel(
    place: PlacePanelUi,
    inner: Boolean,
    readOnly: Boolean,
    onClose: () -> Unit,
    onMove: () -> Unit,
    onOpenCard: () -> Unit,
    onChildMap: (String) -> Unit,
    modifier: Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    GlassCard(modifier) {
        if (inner) {
            CivilightText(MapCopy.PLACE, type.sec, color = colors.ink3)
            CardHeader(place.name, onClose)
        } else {
            CardHeader(place.name, onClose)
            CivilightText(place.eyebrow, type.small, color = colors.ink3, maxLines = 1)
        }
        if (place.description.isNotBlank()) CivilightText(place.description, type.body, maxLines = 4)
        CardRow(MapCopy.APPEARANCE, place.appearance)
        if (inner && place.parentName != null) CardRow(MapCopy.REGION_OF_PLACE, place.parentName)
        CardRow(MapCopy.BACKGROUND_ASSETS, place.backgroundAssets.toString())
        place.childMaps.forEach { (key, name) -> CardRow(MapCopy.CHILD_MAP, name, onClick = { onChildMap(key) }) }
        ButtonRow(Modifier.padding(top = 4.dp)) {
            if (inner) {
                CivilightButton(MapCopy.BTN_PLACE_CARD, onOpenCard, Modifier.weight(1f))
                CivilightButton(MapCopy.BTN_MOVE_PIN, onMove, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = !readOnly)
            } else {
                CivilightButton(MapCopy.BTN_MOVE_PIN, onMove, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = !readOnly)
                CivilightButton(MapCopy.BTN_OPEN_PLACE_CARD, onOpenCard, Modifier.weight(1f))
            }
        }
    }
}

/** `핀 옮기기 · <name>`: the next tap on the map sets the spot; `완료` commits, `취소` leaves it. */
@Composable
internal fun MovePinBar(title: String, canCommit: Boolean, onCancel: () -> Unit, onDone: () -> Unit, modifier: Modifier) {
    GlassCard(modifier) {
        CivilightText(title, CivilightTheme.type.label, maxLines = 1)
        ButtonRow {
            CivilightButton(MapCopy.BTN_CANCEL, onCancel, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            CivilightButton(MapCopy.BTN_DONE, onDone, Modifier.weight(1f), enabled = canCommit)
        }
    }
}

