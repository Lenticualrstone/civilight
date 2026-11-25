package io.github.lenticualrstone.civilight.feature.map.hex

import io.github.lenticualrstone.civilight.feature.map.common.MapAside
import io.github.lenticualrstone.civilight.feature.map.common.MapBars
import io.github.lenticualrstone.civilight.feature.map.common.MapAsideRules
import io.github.lenticualrstone.civilight.feature.map.common.rememberMapAsideState

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import io.github.lenticualrstone.civilight.designsystem.component.SidePanelHost
import io.github.lenticualrstone.civilight.feature.map.common.FoldSwitch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.MapTool
import io.github.lenticualrstone.civilight.designsystem.component.MapToolbar
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.ReadOnlyBanner
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TerrainSwatch
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.MapColors
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.CardRow
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.edit.NameContent
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.navigation.HexPaint as HexPaintRoute
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin

/** `칸 크기` (km). */
@Serializable
data object HexCellKmSheet : SheetKey

/** `좌표 표시`. */
@Serializable
data object HexCoordsSheet : SheetKey

/** `장소 만들기` at a cell: the new place's name. */
@Serializable
data class HexPlaceSheet(val row: Int, val col: Int) : SheetKey

/**
 * 칸 칠하기 (WLD-58, boards `MapHex`, `MMapHexFill`). Cover: the hex map with the info chip
 * `24 km · 뾰족 · 32×24`, and a bottom card with the tool bar `한 칸 · 줄 · 채우기 · 고르기 ·
 * 되돌리기`, the terrain palette, the hint, the `고르기` cell card (`장소 만들기`) and the settings
 * rows. Inner (not drawn, `[PROPOSED]`): the same card as a side panel.
 */
@Composable
fun HexPaintScreen(route: HexPaintRoute, deps: FeatureDeps) {
    val vm = entryViewModel { HexPaintViewModel(route.projectKey, route.mapKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val sheets = rememberSheetHost()
    val inner = LocalWindowLayout.current.isInner
    val aside = rememberMapAsideState()
    var stateText by rememberSaveable(route.mapKey) { mutableStateOf(HexPaintState().encode()) }
    var viewportArray by rememberSaveable(route.mapKey) { mutableStateOf(Viewport.UNSET.toArray()) }
    val state = remember(stateText) { HexPaintState.decode(stateText) }
    fun set(s: HexPaintState) { stateText = s.encode() }
    val d = doc
    val ui = remember(d, hiddenOpen, stateText) { d?.let { HexPaintUiBuilder.build(it, route.mapKey, MapScreenViewModel.lensOf(it), hiddenOpen, state) } }

    CivilightScreen(
        title = ui?.title ?: MapCopy.HEX_TITLE,
        smallLine = ui?.small,
        back = mapBack(deps),
        banner = if (readOnly) ({ ReadOnlyBanner(MapCopy.READ_ONLY) }) else null,
        sheets = sheets,
        localSheets = { key ->
            val grid = ui?.grid
            when {
                grid == null -> null
                key == HexCellKmSheet -> { { KmContent(grid, readOnly, { km -> close(); vm.setCellKm(km) }, ::close) } }
                key == HexCoordsSheet -> {
                    {
                        val sheet = this
                        SheetHeader(MapCopy.COORDS, onClose = ::close)
                        SheetScrollBody {
                            HexPaintState.COORD_CHOICES.forEach { n ->
                                RadioRow(HexPaintUiBuilder.coordsLabel(n), grid.coordEvery == n, { sheet.close(); vm.setCoordEvery(n) }, enabled = !readOnly)
                            }
                        }
                    }
                }
                key is HexPlaceSheet -> { { NameContent("", onDone = { name -> close(); if (name.isNotBlank()) vm.createPlace(io.github.lenticualrstone.civilight.domain.world.map.HexCell(key.row, key.col), name) }, onClose = ::close) } }
                else -> null
            }
        },
    ) { padding ->
        val u = ui ?: return@CivilightScreen
        val acc = CivilightTheme.colors.acc
        Box(Modifier.fillMaxSize()) {
            MapCanvas(
                base = u.base,
                scene = u.scene,
                viewport = Viewport.fromArray(viewportArray),
                onViewportChange = { viewportArray = it.toArray() },
                showRouteChips = false,
                onTap = { p, _ ->
                    val out = HexPaint.tap(state, u.grid, p)
                    set(out.state)
                    out.commit?.let { vm.apply(it) }
                },
                extraDraw = { scope ->
                    fun outline(row: Int, col: Int, width: Float) {
                        val (rx, ry) = MapBases.hexRadius(u.grid)
                        val c = HexMaps.center(u.grid, io.github.lenticualrstone.civilight.domain.world.map.HexCell(row, col))
                        val start = if (u.grid.orientation.known == HexOrientation.FLAT) 0.0 else Math.PI / 6
                        val path = Path()
                        for (k in 0 until 6) {
                            val a = start + k * Math.PI / 3
                            val s0 = scope.screen(io.github.lenticualrstone.civilight.model.Point(c.x + (rx * cos(a)).toFloat(), c.y + (ry * sin(a)).toFloat()))
                            if (k == 0) path.moveTo(s0.x, s0.y) else path.lineTo(s0.x, s0.y)
                        }
                        path.close()
                        drawPath(path, acc, style = Stroke(width))
                    }
                    state.selected?.let { outline(it.row, it.col, 2.5.dp.toPx()) }
                    state.lineFrom?.let { outline(it.row, it.col, 4.dp.toPx()) }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            MapValueChip(u.info, Modifier.align(Alignment.TopStart).padding(top = padding.calculateTopPadding() + 8.dp, start = 20.dp))
            val panel: @Composable ColumnScope.() -> Unit = {
                HexPanel(u, state, readOnly, ::set, onUndo = { vm.undoLast() }, onCreatePlace = { sheets.open(HexPlaceSheet(it.row, it.col)) },
                    onCellKm = { sheets.open(HexCellKmSheet) }, onCoords = { sheets.open(HexCoordsSheet) },
                    onOrientation = { vm.setOrientation(HexOrientation.entries[it]) })
            }
            SidePanelHost(
                inner,
                Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + MapAsideRules.TOP_GAP.dp, bottom = padding.calculateBottomPadding() + 12.dp),
            ) {
                MapAside(state = aside, tabs = emptyList(), selectedTab = 0, onSelectTab = {}) {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = panel)
                }
            }
            FoldSwitch(inner) { isInner ->
                // The cover card, and on the inner screen while the side panel is closed (MapBars.panelShown).
                if (!MapBars.panelShown(isInner, aside.closed)) {
                    GlassCard(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 12.dp).heightIn(max = 420.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = panel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.HexPanel(
    u: HexPaintUi,
    state: HexPaintState,
    readOnly: Boolean,
    set: (HexPaintState) -> Unit,
    onUndo: () -> Unit,
    onCreatePlace: (io.github.lenticualrstone.civilight.domain.world.map.HexCell) -> Unit,
    onCellKm: () -> Unit,
    onCoords: () -> Unit,
    onOrientation: (Int) -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val tools = remember { HexTool.entries.map { MapTool(it.label, it.icon) } + MapTool(MapCopy.HEX_TOOL_UNDO, IconId.UndoMap) }
    Box(Modifier.horizontalScroll(rememberScrollState())) {
        MapToolbar(state.tool.ordinal, { i -> if (i == HexTool.entries.size) onUndo() else set(HexPaint.chooseTool(state, HexTool.entries[i])) }, tools = tools, label = MapCopy.TOOLBAR)
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        HexPaintState.PALETTE.forEach { t ->
            TerrainSwatch(MapColors.dsTerrain(t), HexMaps.terrainName(t), t == state.terrain, { set(HexPaint.chooseTerrain(state, t)) })
        }
    }
    CivilightText(MapCopy.HEX_HINT, type.small, color = colors.ink3)
    // The chosen cell's line folds open and away with the selection (motion audit row 1).
    FoldRevealValue(u.cell) { c ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CivilightText(c.text, type.label, maxLines = 2)
            if (c.canCreatePlace) CivilightButton(MapCopy.BTN_CREATE_PLACE, { onCreatePlace(c.cell) }, Modifier.fillMaxWidth(), variant = ButtonVariant.GHOST, enabled = !readOnly)
        }
    }
    CardRow(MapCopy.CELL_SIZE, u.cellSize, onClick = if (readOnly) null else onCellKm)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CivilightText(MapCopy.ORIENTATION, type.label, color = colors.ink3)
        SegmentedControl(MapCopy.ORIENTATIONS, u.orientation, { if (!readOnly) onOrientation(it) }, Modifier.weight(1f))
    }
    CardRow(MapCopy.COORDS, u.coords, onClick = if (readOnly) null else onCoords)
}

/** `칸 크기`: the km of one cell (also the map scale). */
@Composable
private fun ColumnScope.KmContent(grid: HexGrid, readOnly: Boolean, onDone: (Double) -> Unit, onClose: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(io.github.lenticualrstone.civilight.domain.world.map.Travel.number(grid.cellKm)) }
    val km = HexPaintViewModel.parseKm(text)
    SheetHeader(MapCopy.CELL_SIZE, onClose = onClose)
    SheetScrollBody { CivilightTextField(text, { text = it }, Modifier.padding(top = 12.dp), label = MapCopy.UNIT_KM) }
    SheetActions { CivilightButton(MapCopy.BTN_DONE, { km?.let(onDone) }, Modifier.weight(1f), enabled = km != null && !readOnly) }
}
