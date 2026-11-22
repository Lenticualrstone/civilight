package io.github.lenticualrstone.civilight.feature.map.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.MapTool
import io.github.lenticualrstone.civilight.designsystem.component.MapToolbar
import io.github.lenticualrstone.civilight.designsystem.component.MapVertex
import io.github.lenticualrstone.civilight.designsystem.component.ReadOnlyBanner
import io.github.lenticualrstone.civilight.designsystem.component.ScaleBar
import io.github.lenticualrstone.civilight.designsystem.component.SidePanel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.CurveGeometry
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.canvas.mapAt
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.view.MovePinBar
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.TravelModes
import io.github.lenticualrstone.civilight.navigation.MapEdit as MapEditRoute
import io.github.lenticualrstone.civilight.navigation.MapScale as MapScaleRoute
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * 지도 편집 (WLD-40 … 44, boards `MapEdit`, `MapRouteC`, `TourMap`): header `<지도> · 편집` with the
 * breadcrumb, lens chip, `되돌리기` and `완료`; the visible tool bar `이동 · 핀 · 영역 · 경로 · 라벨 ·
 * 레이어` (bottom-left inner, bottom centre cover; only the chosen tool acts); properties in the
 * inner side panel `속성 · 레이어 · 목록` or the cover bottom card; property pickers as sheets.
 */
@Composable
fun MapEditScreen(route: MapEditRoute, deps: FeatureDeps) {
    val vm = entryViewModel { MapEditViewModel(route.projectKey, route.mapKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val inner = LocalWindowLayout.current.isInner

    var editorText by rememberSaveable(route.mapKey) { mutableStateOf(EditorState().encode()) }
    var viewportArray by rememberSaveable(route.mapKey) { mutableStateOf(Viewport.UNSET.toArray()) }
    var pinQuery by rememberSaveable { mutableStateOf("") }
    val editor = remember(editorText) { EditorState.decode(editorText) }
    fun set(s: EditorState) { editorText = s.encode() }
    val viewport = Viewport.fromArray(viewportArray)

    val d = doc
    val ui = remember(d, hiddenOpen, editorText) { d?.let { EditState.build(it, route.mapKey, MapScreenViewModel.lensOf(it), hiddenOpen, editor) } }
    val map = d?.maps?.get(route.mapKey)

    val actions = remember(vm, sheets) {
        object : PanelActions {
            fun s() = EditorState.decode(editorText)
            override fun deselect() = set(Editor.select(s(), null))
            override fun openReveal(target: Key?) = sheets.open(RevealSheet(target))
            override fun openLayer(target: Key?) = sheets.open(LayerSheet(target))
            override fun openFaction(target: Key?) = sheets.open(FactionSheet(target))
            override fun openKind(target: Key?) = sheets.open(KindSheet(target))
            override fun openName(target: Key?) = sheets.open(RegionNameSheet(target))
            override fun openLabelText(labelKey: Key) = sheets.open(LabelTextSheet(labelKey = labelKey))
            override fun setColor(target: Key?, token: String) {
                if (target == null) set(Editor.editRegionDraft(s()) { it.copy(color = token) }) else vm.updateRegion(target) { it.copy(color = token) }
            }
            override fun undoVertex() = set(Editor.undoVertex(s()))
            override fun closeRegion() {
                val draft = s().region ?: return
                vm.closeRegion(draft) { key -> set(Editor.regionClosed(EditorState.decode(editorText), key)) }
            }
            override fun undoRoutePoint() = set(Editor.undoRoutePoint(s()))
            override fun finishRoute() {
                val points = s().routeDraft.points
                vm.finishRoute(points) { key -> set(Editor.routeFinished(EditorState.decode(editorText), key)) }
            }
            override fun setRouteMode(routeKey: Key, modeKey: Key?) {
                if (modeKey == null) deps.navigator.push(TravelModes(route.projectKey)) else vm.updateRoute(routeKey) { it.copy(shownModeKey = modeKey) }
            }
            override fun setScale() = deps.navigator.push(MapScaleRoute(route.projectKey, route.mapKey))
            override fun movePin(pinKey: Key) = set(Editor.startMovePin(s(), pinKey))
            override fun openPlaceCard(placeKey: Key) = deps.navigator.push(PlaceCard(route.projectKey, placeKey))
            override fun setCurve(labelKey: Key, on: Boolean) {
                val label = vm.doc.value?.maps?.get(route.mapKey)?.labels?.firstOrNull { it.key == labelKey } ?: return
                vm.setLabelCurve(labelKey, if (on) CurveGeometry.defaultCurve(Point(label.x, label.y)) else null)
            }
        }
    }

    CivilightScreen(
        title = ui?.title.orEmpty(),
        smallLine = ui?.small,
        back = mapBack(deps),
        lens = d?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        actions = {
            CivilightIconButton(IconId.Undo, MapCopy.BTN_UNDO, { vm.undoLast() }, enabled = !readOnly)
            TextAction(MapCopy.BTN_DONE, { deps.navigator.pop() })
        },
        banner = if (readOnly) ({ ReadOnlyBanner(MapCopy.READ_ONLY) }) else null,
        sheets = sheets,
        localSheets = { key ->
            val doc0 = doc
            val map0 = doc0?.maps?.get(route.mapKey)
            if (doc0 == null || map0 == null) null else when (key) {
                is PinPlaceSheet -> {
                    {
                        val model = PinPick.model(doc0, route.mapKey, pinQuery, hiddenOpen)
                        PinPlaceContent(
                            model, pinQuery, { pinQuery = it }, gate,
                            onPick = { placeKey ->
                                close()
                                vm.addPin(placeKey, key.point()) { pin -> set(Editor.pinPlaced(EditorState.decode(editorText), pin)) }
                            },
                            onCreate = { name ->
                                close()
                                vm.createPlaceWithPin(name, key.point()) { pin -> set(Editor.pinPlaced(EditorState.decode(editorText), pin)) }
                            },
                            onClose = ::close,
                        )
                    }
                }
                is LabelTextSheet -> {
                    {
                        val existing = key.labelKey?.let { k -> map0.labels.firstOrNull { it.key == k } }
                        LabelTextContent(existing?.text.orEmpty(), existing != null, onDone = { text ->
                            close()
                            if (existing != null) {
                                vm.updateLabel(existing.key) { it.copy(text = text.trim()) }
                            } else {
                                vm.addLabel(text, key.point()) { label -> set(Editor.labelPlaced(EditorState.decode(editorText), label)) }
                            }
                        }, onClose = ::close)
                    }
                }
                is RevealSheet -> {
                    {
                        RevealContent(doc0, gate, onPick = { unit ->
                            close()
                            val target = key.target
                            if (target == null) set(Editor.editRegionDraft(EditorState.decode(editorText)) { it.copy(revealUnitKey = unit) })
                            else vm.setReveal(target, unit, MapCopy.step(MapCopy.REVEAL_POINT, EditState.revealLabelOfUnit(doc0, unit)))
                        }, onClose = ::close)
                    }
                }
                is LayerSheet -> {
                    {
                        val current = key.target?.let { io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes.layerOf(map0, it) }
                            ?: if (key.target == null) EditorState.decode(editorText).region?.layerKey else null
                        LayerPickContent(ui?.layers.orEmpty(), current, onPick = { layer ->
                            close()
                            val target = key.target
                            if (target == null) set(Editor.editRegionDraft(EditorState.decode(editorText)) { it.copy(layerKey = layer) })
                            else vm.setLayer(target, layer, MapCopy.step(MapCopy.LAYER, EditState.layerLabel(map0, layer)))
                        }, onClose = ::close)
                    }
                }
                is FactionSheet -> {
                    {
                        val target = key.target
                        val current = if (target == null) EditorState.decode(editorText).region?.factionKey else map0.regions.firstOrNull { it.key == target }?.factionKey
                        FactionContent(doc0, gate, current, onPick = { faction ->
                            close()
                            if (target == null) set(Editor.editRegionDraft(EditorState.decode(editorText)) { it.copy(factionKey = faction) })
                            else vm.updateRegion(target) { it.copy(factionKey = faction) }
                        }, onClose = ::close)
                    }
                }
                is KindSheet -> {
                    {
                        val target = key.target
                        val current = if (target == null) EditorState.decode(editorText).region?.kind.orEmpty() else map0.regions.firstOrNull { it.key == target }?.kind.orEmpty()
                        KindContent(current, onPick = { kind ->
                            close()
                            if (target == null) set(Editor.editRegionDraft(EditorState.decode(editorText)) { it.copy(kind = kind) })
                            else vm.updateRegion(target) { it.copy(kind = kind) }
                        }, onClose = ::close)
                    }
                }
                is RegionNameSheet -> {
                    {
                        val target = key.target
                        val current = if (target == null) EditorState.decode(editorText).region?.name.orEmpty() else map0.regions.firstOrNull { it.key == target }?.name.orEmpty()
                        NameContent(current, onDone = { name ->
                            close()
                            if (target == null) set(Editor.editRegionDraft(EditorState.decode(editorText)) { it.copy(name = name.trim()) })
                            else if (name.isNotBlank()) vm.updateRegion(target) { it.copy(name = name.trim()) }
                        }, onClose = ::close)
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        val state = ui ?: return@CivilightScreen
        val acc = CivilightTheme.colors.acc
        Box(Modifier.fillMaxSize()) {
            MapCanvas(
                base = state.base,
                scene = state.scene,
                viewport = viewport,
                onViewportChange = { viewportArray = it.toArray() },
                selectedKey = editor.movingPin ?: editor.selected,
                onTap = { p, hit ->
                    val m = map ?: return@MapCanvas
                    val out = Editor.tap(editor, m, p, hit)
                    set(out.state)
                    when (out.effect) {
                        EditorEffect.PICK_PLACE -> { pinQuery = ""; sheets.open(PinPlaceSheet(p.x, p.y)) }
                        EditorEffect.LABEL_TEXT -> sheets.open(LabelTextSheet(p.x, p.y))
                        null -> Unit
                    }
                    when (val c = out.commit) {
                        is EditorCommit.MoveVertex -> vm.moveVertex(c.regionKey, c.index, c.to)
                        is EditorCommit.MoveCurveHandle -> {
                            val curve = m.labels.firstOrNull { it.key == c.labelKey }?.curve
                            if (curve != null && c.index in curve.indices) {
                                vm.setLabelCurve(c.labelKey, curve.mapIndexed { i, q -> if (i == c.index) c.to else q })
                            }
                        }
                        null -> Unit
                    }
                },
                extraDraw = { scope ->
                    val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                    fun line(points: List<Point>, closed: Boolean) {
                        if (points.size < 2) return
                        val path = Path()
                        points.forEachIndexed { i, q -> val s = scope.screen(q); if (i == 0) path.moveTo(s.x, s.y) else path.lineTo(s.x, s.y) }
                        if (closed) path.close()
                        drawPath(path, acc, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash))
                    }
                    line(state.regionDraft, closed = false)
                    line(state.routeDraft, closed = false)
                    state.routeDraft.forEach { q -> val s = scope.screen(q); drawCircle(acc, 4.dp.toPx(), Offset(s.x, s.y)) }
                },
                marks = { scope ->
                    state.regionDraft.forEachIndexed { i, q ->
                        val s = scope.screen(q)
                        MapVertex(Modifier.mapAt(s.x, s.y), last = i == state.regionDraft.lastIndex)
                    }
                    val props = state.props
                    if (editor.tool == EditTool.MOVE || editor.tool == EditTool.LAYER) {
                        if (props is PropsUi.RegionProps) {
                            props.vertices.forEachIndexed { i, q ->
                                val s = scope.screen(q)
                                MapVertex(Modifier.mapAt(s.x, s.y), last = editor.vertex == i, onClick = { set(Editor.chooseVertex(editor, i)) })
                            }
                        }
                        if (props is PropsUi.LabelProps) {
                            props.curve?.forEachIndexed { i, q ->
                                val s = scope.screen(q)
                                MapVertex(Modifier.mapAt(s.x, s.y), last = editor.handle == i, onClick = { set(Editor.chooseHandle(editor, i)) })
                            }
                        }
                    }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            val tools = remember { EditTool.entries.map { MapTool(it.label, it.icon) } }
            val toolbar: @Composable (Modifier) -> Unit = { mod ->
                MapToolbar(editor.tool.ordinal, { set(Editor.chooseTool(editor, EditTool.entries[it])) }, mod, tools = tools, label = MapCopy.TOOLBAR)
            }
            val moving = state.moving
            if (inner) {
                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = padding.calculateBottomPadding() + 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.scale?.let { ScaleBar(it) }
                    if (moving != null) {
                        MovePinBar(moving.first, moving.second != null && !readOnly, { set(Editor.endMovePin(editor)) }, {
                            val to = moving.second ?: return@MovePinBar
                            editor.movingPin?.let { k -> vm.movePin(k, to) { set(Editor.endMovePin(EditorState.decode(editorText))) } }
                        }, Modifier.widthIn(max = 360.dp))
                    }
                    toolbar(Modifier)
                }
                SidePanel(
                    onClose = actions::deselect,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + 8.dp, end = 12.dp, bottom = 12.dp),
                    tabs = listOf(MapCopy.TAB_PROPS, MapCopy.TAB_LAYERS, MapCopy.TAB_LIST),
                    selectedTab = editor.panelTab,
                    onSelectTab = { set(editor.copy(panelTab = it)) },
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (editor.panelTab) {
                            1 -> LayersPanel(state.layers, readOnly) { k, v -> vm.setLayerVisible(k, v) }
                            2 -> ItemsPanel(state.items) { set(Editor.select(editor, it)) }
                            else -> state.props?.let { PropsPanel(it, readOnly, actions) }
                        }
                    }
                }
            } else {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when {
                        moving != null -> MovePinBar(moving.first, moving.second != null && !readOnly, { set(Editor.endMovePin(editor)) }, {
                            val to = moving.second ?: return@MovePinBar
                            editor.movingPin?.let { k -> vm.movePin(k, to) { set(Editor.endMovePin(EditorState.decode(editorText))) } }
                        }, Modifier.fillMaxWidth())
                        editor.tool == EditTool.LAYER -> GlassCard(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                            Column(Modifier.verticalScroll(rememberScrollState())) { LayersPanel(state.layers, readOnly) { k, v -> vm.setLayerVisible(k, v) } }
                        }
                        state.props != null -> GlassCard(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { PropsPanel(state.props, readOnly, actions) }
                        }
                    }
                    toolbar(Modifier)
                }
            }
        }
    }
}
