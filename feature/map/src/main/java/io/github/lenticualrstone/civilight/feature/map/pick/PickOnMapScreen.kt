package io.github.lenticualrstone.civilight.feature.map.pick

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import kotlinx.serialization.Serializable
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.navigation.PickOnMap
import io.github.lenticualrstone.civilight.domain.world.item.PlacePicker as PlacePickerCopy
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen

/** Reads the map; its one write is `새 장소 · 지도에 핀을 꽂으며 만들기` (a place and its pin, one step). */
class PickOnMapViewModel(projectKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    fun createPlaceWithPin(mapKey: Key, name: String, at: Point, onDone: (Key) -> Unit) {
        val ctx = editContext()
        commitValue(MapCopy.step(MapCopy.PIN, name.trim()), onDone) { d ->
            (MapEdits.createPlaceWithPin(d, mapKey, name, at, ctx) as? MapEdit.Applied)?.let { it.doc to it.value.placeKey }
        }
    }
}

/** `새 장소` name for a spot tapped in pick mode. */
@Serializable
data class NewPlaceSheet(val x: Float, val y: Float) : SheetKey

/**
 * `지도에서 고르기 ›` (WLD-52 map mode): the map in pick mode with a map switcher and the
 * `장소 | 지역` toggle; tapping a pin picks its place, in `지역` mode tapping inside a region picks
 * it; the bottom bar confirms (`씬 1개 → 잿더미 시장` · `고르기`). The result is sent as a
 * `model.story.PlaceRef` through `deps.results` under the route's request key, then the screen pops.
 */
@Composable
fun PickOnMapScreen(route: PickOnMap, deps: FeatureDeps) {
    val vm = entryViewModel { PickOnMapViewModel(route.projectKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    var mapKey by rememberSaveable { mutableStateOf<String?>(null) }
    var regions by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var pickedKind by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var viewportArray by rememberSaveable { mutableStateOf(Viewport.UNSET.toArray()) }
    var emptySpot by rememberSaveable { mutableStateOf<FloatArray?>(null) }
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val d = doc
    val activeMap = mapKey ?: d?.let { PickState.initialMap(it, route.sceneKey) }
    val regionMode = regions ?: (d?.let { PickState.initialRegions(it, route.sceneKey) } ?: false)
    val picked = pickedKey?.let { k -> PlaceRefKind.entries.firstOrNull { it.name == pickedKind }?.let { PlaceRef(it, k) } }
    val ui = remember(d, hiddenOpen, activeMap, regionMode, pickedKind, pickedKey) {
        if (d == null || activeMap == null) null else PickState.build(d, activeMap, MapScreenViewModel.lensOf(d), hiddenOpen, regionMode, picked, route.sceneKey, route.sceneCount)
    }

    CivilightScreen(
        title = MapCopy.PICK_TITLE,
        smallLine = d?.project?.name,
        back = mapBack(deps),
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is NewPlaceSheet -> {
                    {
                        val sheet = this
                        var name by rememberSaveable { mutableStateOf("") }
                        SheetHeader(MapCopy.NEW_PLACE, onClose = sheet::close)
                        SheetScrollBody { CivilightTextField(name, { name = it }, Modifier.padding(top = 12.dp), label = MapCopy.PLACE_NAME) }
                        SheetActions {
                            CivilightButton(MapCopy.BTN_CANCEL, sheet::close, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                            CivilightButton(MapCopy.BTN_CREATE, {
                                sheet.close()
                                val target = activeMap ?: return@CivilightButton
                                vm.createPlaceWithPin(target, name, Point(key.x, key.y)) { placeKey ->
                                    emptySpot = null
                                    pickedKind = PlaceRefKind.PLACE.name
                                    pickedKey = placeKey
                                }
                            }, Modifier.weight(1f), enabled = name.isNotBlank())
                        }
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            val state = ui
            if (state != null) {
                MapCanvas(
                    base = state.base,
                    scene = state.scene,
                    viewport = Viewport.fromArray(viewportArray),
                    onViewportChange = { viewportArray = it.toArray() },
                    selectedKey = state.selectedKey,
                    showRouteChips = false,
                    onTap = { p, hit ->
                        val doc0 = d ?: return@MapCanvas
                        val ref = PickState.pick(doc0, state.mapKey, p, hit, state.regions, MapScreenViewModel.lensOf(doc0), hiddenOpen)
                        pickedKind = ref?.kind?.known?.name
                        pickedKey = ref?.key
                        emptySpot = if (ref == null && !state.regions) floatArrayOf(p.x, p.y) else null
                    },
                )
            }
            TopBarTapGuard(padding.calculateTopPadding())
            Column(
                Modifier.fillMaxWidth().padding(top = padding.calculateTopPadding() + 4.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SegmentedControl(
                    options = listOf(MapCopy.PICK_PLACE, MapCopy.PICK_REGION),
                    selectedIndex = if (regionMode) 1 else 0,
                    onSelect = { regions = it == 1; pickedKey = null; pickedKind = null },
                    modifier = Modifier.widthIn(max = 280.dp),
                )
                if ((state?.maps?.size ?: 0) > 1) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state?.maps?.forEach { chip ->
                            FilterChip(chip.name, chip.selected, {
                                mapKey = chip.key
                                viewportArray = Viewport.UNSET.toArray()
                            })
                        }
                    }
                }
            }
            if (state == null && d != null) {
                CivilightText(MapCopy.NONE, CivilightTheme.type.small, Modifier.align(Alignment.Center), color = CivilightTheme.colors.ink3)
            }
            val bar = state?.bar
            val ref = state?.picked
            val spot = emptySpot
            if (ref == null && spot != null && state != null) {
                GlassCard(Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CivilightText(PlacePickerCopy.NEW_PLACE_ROW, CivilightTheme.type.label, Modifier.weight(1f), maxLines = 2)
                        CivilightButton(MapCopy.NEW_PLACE, { sheets.open(NewPlaceSheet(spot[0], spot[1])) }, enabled = !readOnly)
                    }
                }
            }
            if (bar != null && ref != null) {
                GlassCard(Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CivilightText(bar, CivilightTheme.type.label, Modifier.weight(1f), maxLines = 1)
                        CivilightButton(MapCopy.BTN_PICK, {
                            deps.results.send(route.requestKey, ref)
                            deps.navigator.pop()
                        })
                    }
                }
            }
        }
    }
}
