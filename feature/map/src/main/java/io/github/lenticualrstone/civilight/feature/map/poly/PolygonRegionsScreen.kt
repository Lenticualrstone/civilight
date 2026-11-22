package io.github.lenticualrstone.civilight.feature.map.poly

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.MapTool
import io.github.lenticualrstone.civilight.designsystem.component.MapToolbar
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.designsystem.component.ReadOnlyBanner
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SidePanel
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.CardHeader
import io.github.lenticualrstone.civilight.feature.map.common.CardRow
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.edit.ColorRow
import io.github.lenticualrstone.civilight.feature.map.edit.EditState
import io.github.lenticualrstone.civilight.feature.map.edit.FactionContent
import io.github.lenticualrstone.civilight.feature.map.edit.LayerActions
import io.github.lenticualrstone.civilight.feature.map.edit.LayerNameContent
import io.github.lenticualrstone.civilight.feature.map.edit.LayerNameSheet
import io.github.lenticualrstone.civilight.feature.map.edit.LayersPanel
import io.github.lenticualrstone.civilight.feature.map.edit.NameContent
import io.github.lenticualrstone.civilight.feature.map.edit.RevealContent
import io.github.lenticualrstone.civilight.feature.map.edit.RevealSheet
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.PolygonRegions as PolygonRegionsRoute
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.serialization.Serializable

/** `이름` of a province. */
@Serializable
data class ProvinceNameSheet(val provinceKey: Key) : SheetKey

/** `연결 세력` of a province. */
@Serializable
data class ProvinceFactionSheet(val provinceKey: Key) : SheetKey

/**
 * 다각형 · 지역 나누기 (WLD-59, board `MapPoly`). Inner: the map, the side panel `지역 · 레이어`
 * (province rows `<이름> | 칸 N`, the hint, `새 지역`, the selected province's 이름 · 색 · 연결 세력)
 * and the tool bar bottom-left. Cover (not drawn, `[PROPOSED]`): the same panel as a bottom card.
 */
@Composable
fun PolygonRegionsScreen(route: PolygonRegionsRoute, deps: FeatureDeps) {
    val vm = entryViewModel { PolyViewModel(route.projectKey, route.mapKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val inner = LocalWindowLayout.current.isInner
    var stateText by rememberSaveable(route.mapKey) { mutableStateOf(PolyState().encode()) }
    var viewportArray by rememberSaveable(route.mapKey) { mutableStateOf(Viewport.UNSET.toArray()) }
    val state = remember(stateText) { PolyState.decode(stateText) }
    fun set(s: PolyState) { stateText = s.encode() }
    val d = doc
    val ui = remember(d, hiddenOpen, stateText) { d?.let { PolyUiBuilder.build(it, route.mapKey, MapScreenViewModel.lensOf(it), hiddenOpen, state) } }
    val layerActions = remember(vm, sheets) {
        object : LayerActions {
            override fun setVisible(layerKey: Key, visible: Boolean) { vm.setLayerVisible(layerKey, visible) }
            override fun rename(layerKey: Key) = sheets.open(LayerNameSheet(layerKey))
            override fun reveal(layerKey: Key) = sheets.open(RevealSheet(layerKey))
            override fun move(layerKey: Key, up: Boolean) { vm.moveLayer(layerKey, up) }
            override fun add() = sheets.open(LayerNameSheet(null))
        }
    }

    CivilightScreen(
        title = ui?.title ?: MapCopy.POLY_TITLE,
        smallLine = ui?.small,
        back = mapBack(deps),
        lens = d?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        banner = if (readOnly) ({ ReadOnlyBanner(MapCopy.READ_ONLY) }) else null,
        sheets = sheets,
        localSheets = { key ->
            val doc0 = d
            val poly = ui?.poly
            val map0 = doc0?.maps?.get(route.mapKey)
            if (doc0 == null || poly == null || map0 == null) null else when (key) {
                is ProvinceNameSheet -> { { NameContent(PolyUiBuilder.nameOf(poly, key.provinceKey), onDone = { n -> close(); if (n.isNotBlank()) vm.updateProvince(key.provinceKey) { it.copy(name = n.trim()) } }, onClose = ::close) } }
                is ProvinceFactionSheet -> {
                    {
                        val current = poly.provinces.firstOrNull { it.key == key.provinceKey }?.factionKey
                        FactionContent(doc0, gate, current, onPick = { f -> close(); vm.updateProvince(key.provinceKey) { it.copy(factionKey = f) } }, onClose = ::close)
                    }
                }
                is LayerNameSheet -> {
                    {
                        val existing = key.layerKey?.let { k -> map0.layers.firstOrNull { it.key == k } }
                        LayerNameContent(existing?.name.orEmpty(), existing != null, onDone = { name ->
                            close()
                            if (existing != null) vm.renameLayer(existing.key, name) else vm.addLayer(name)
                        }, onClose = ::close)
                    }
                }
                is RevealSheet -> {
                    {
                        RevealContent(doc0, gate, onPick = { unit ->
                            close()
                            key.target?.let { vm.setReveal(it, unit, MapCopy.step(MapCopy.REVEAL_POINT, EditState.revealLabelOfUnit(doc0, unit))) }
                        }, onClose = ::close)
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        val u = ui ?: return@CivilightScreen
        val colors = CivilightTheme.colors
        Box(Modifier.fillMaxSize()) {
            MapCanvas(
                base = u.base,
                scene = u.scene,
                viewport = Viewport.fromArray(viewportArray),
                onViewportChange = { viewportArray = it.toArray() },
                showRouteChips = false,
                onTap = { p, _ ->
                    val out = PolyEditor.tap(state, u.poly, p)
                    set(out.state)
                    out.commit?.let { vm.apply(it) }
                },
                extraDraw = { scope ->
                    u.selectedSites.forEach { q -> val s0 = scope.screen(q); drawCircle(colors.acc, 3.dp.toPx(), Offset(s0.x, s0.y)) }
                    u.splitSites.forEach { q -> val s0 = scope.screen(q); drawCircle(colors.acc, 7.dp.toPx(), Offset(s0.x, s0.y)) }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            u.assignChip?.let { MapValueChip(it, Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding() + 12.dp)) }
            val tools = remember { PolyTool.entries.map { MapTool(it.label, it.icon) } }
            val toolbar: @Composable (Modifier) -> Unit = { mod ->
                Box(mod.horizontalScroll(rememberScrollState())) {
                    MapToolbar(state.tool.ordinal, { set(PolyEditor.chooseTool(state, PolyTool.entries[it])) }, tools = tools, label = MapCopy.TOOLBAR)
                }
            }
            val panel: @Composable ColumnScope.() -> Unit = {
                SegmentedControl(listOf(MapCopy.TAB_PROVINCES, MapCopy.TAB_LAYERS), state.panelTab, { set(state.copy(panelTab = it)) })
                if (state.panelTab == 1) LayersPanel(u.layers, readOnly, layerActions)
                else ProvincePanel(u, readOnly, state, ::set, vm, onName = { sheets.open(ProvinceNameSheet(it)) }, onFaction = { sheets.open(ProvinceFactionSheet(it)) })
            }
            if (inner) {
                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = padding.calculateBottomPadding() + 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    u.splitBar?.let { (text, ok) -> SplitBar(text, ok, readOnly, Modifier.widthIn(max = 360.dp)) { split(vm, state, ::set) } }
                    toolbar(Modifier)
                }
                SidePanel(
                    onClose = { set(PolyEditor.select(state, null)) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + 8.dp, end = 12.dp, bottom = 12.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp), content = panel)
                }
            } else {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    u.splitBar?.let { (text, ok) -> SplitBar(text, ok, readOnly, Modifier.fillMaxWidth()) { split(vm, state, ::set) } }
                    GlassCard(Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp), content = panel)
                    }
                    toolbar(Modifier)
                }
            }
        }
    }
}

private fun split(vm: PolyViewModel, state: PolyState, set: (PolyState) -> Unit) {
    val key = state.selected ?: return
    vm.split(key, state.split) { newKey -> set(PolyEditor.splitDone(state, newKey)) }
}

/** `칸 N · 나누기`. */
@Composable
private fun SplitBar(text: String, canSplit: Boolean, readOnly: Boolean, modifier: Modifier, onSplit: () -> Unit) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CivilightText(text, CivilightTheme.type.label, Modifier.weight(1f), maxLines = 1)
            CivilightButton(MapCopy.BTN_SPLIT, onSplit, variant = ButtonVariant.COMPACT_PRIMARY, enabled = canSplit && !readOnly)
        }
    }
}

/** `지역` tab: the selected province (이름 · 색 · 연결 세력), the rows `<이름> | 칸 N`, the hint and `새 지역`. */
@Composable
private fun ColumnScope.ProvincePanel(
    u: PolyUi,
    readOnly: Boolean,
    state: PolyState,
    set: (PolyState) -> Unit,
    vm: PolyViewModel,
    onName: (Key) -> Unit,
    onFaction: (Key) -> Unit,
) {
    val colors = CivilightTheme.colors
    u.props?.let { p ->
        CardHeader(p.name, { set(PolyEditor.select(state, null)) })
        CardRow(MapCopy.NAME, p.name, onClick = if (readOnly) null else ({ onName(p.key) }))
        ColorRow(p.color) { token -> if (!readOnly) vm.updateProvince(p.key) { it.copy(color = token) } }
        CardRow(MapCopy.FACTION_LINK, p.faction, onClick = if (readOnly) null else ({ onFaction(p.key) }))
    }
    u.rows.forEach { r -> ListRow(r.name, subtitle = r.cells, onClick = { set(PolyEditor.select(state, r.key)) }, selected = r.selected, compact = true) }
    CivilightText(MapCopy.POLY_HINT, CivilightTheme.type.small, color = colors.ink3)
    if (!readOnly) AddChip(MapCopy.BTN_NEW_PROVINCE, { vm.addProvince { key -> set(PolyEditor.select(state, key)) } })
}
