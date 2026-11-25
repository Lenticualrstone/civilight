package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.feature.map.common.MapOffers
import io.github.lenticualrstone.civilight.feature.map.common.MapAside
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics
import io.github.lenticualrstone.civilight.feature.map.common.MapAsideRules
import io.github.lenticualrstone.civilight.feature.map.common.rememberMapAsideState
import io.github.lenticualrstone.civilight.ui.tour.TourStepAbsent
import io.github.lenticualrstone.civilight.ui.tour.tourTarget
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
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
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.CurveGeometry
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.canvas.mapAt
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.FoldSwitch
import io.github.lenticualrstone.civilight.feature.map.common.MapBar
import io.github.lenticualrstone.civilight.feature.map.common.MapBarSlot
import io.github.lenticualrstone.civilight.feature.map.common.MapBars
import androidx.compose.animation.animateContentSize
import io.github.lenticualrstone.civilight.designsystem.component.SidePanelHost
import io.github.lenticualrstone.civilight.designsystem.component.TabContent
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapNav
import io.github.lenticualrstone.civilight.feature.map.scale.ScaleBars
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalDensity
import io.github.lenticualrstone.civilight.feature.map.list.NewMapSheet
import io.github.lenticualrstone.civilight.feature.map.list.rememberNewMapFlow
import io.github.lenticualrstone.civilight.feature.map.travel.TravelModeContent
import io.github.lenticualrstone.civilight.feature.map.travel.TravelModeSheet
import io.github.lenticualrstone.civilight.domain.world.map.MapNesting
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.view.MovePinBar
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.MapEdit as MapEditRoute
import io.github.lenticualrstone.civilight.navigation.DetailStamps as DetailStampsRoute
import io.github.lenticualrstone.civilight.navigation.MapReveal as MapRevealRoute
import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.feature.map.canvas.StampGlyphs
import io.github.lenticualrstone.civilight.feature.map.common.rememberMapMenuHost
import io.github.lenticualrstone.civilight.feature.map.view.MapMenu
import io.github.lenticualrstone.civilight.feature.map.view.MapMenuScreen
import io.github.lenticualrstone.civilight.feature.map.view.MapMoreContent
import io.github.lenticualrstone.civilight.feature.map.view.MapMoreSheet
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import androidx.compose.ui.graphics.PathOperation
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
fun MapEditScreen(route: MapEditRoute, deps: FeatureDeps) = MapEditorScreen(route.projectKey, route.mapKey, deps, detail = false)

/**
 * 상세 지형 · 편집 (WLD-60, boards `MapDetail`, `MMapStamp`): the same editor with the tool bar
 * `이동 · 도장 · 강 · 길 · 라벨 · 영역 · 레이어`, the side panel `도장 · 범례 · 레이어` (inner) or the
 * bottom card (cover, `[PROPOSED]`), stamp handles and the stamp chip.
 */
@Composable
fun DetailStampsScreen(route: DetailStampsRoute, deps: FeatureDeps) = MapEditorScreen(route.projectKey, route.mapKey, deps, detail = true)

private data class EditorRoute(val projectKey: String, val mapKey: String)

@Composable
private fun MapEditorScreen(projectKey: String, mapKey: String, deps: FeatureDeps, detail: Boolean) {
    val route = EditorRoute(projectKey, mapKey)
    val vm = entryViewModel { MapEditViewModel(route.projectKey, route.mapKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    // A node map is edited in the node editor; a stale MapEdit for one is sent there.
    val nodeMap = doc?.maps?.get(route.mapKey)?.let { !MapOffers.canEdit(it) }
    if (nodeMap == true) {
        LaunchedEffect(route.mapKey) { deps.navigator.replace(io.github.lenticualrstone.civilight.navigation.NodeMap(route.projectKey, route.mapKey)) }
        return
    }
    val readOnly by vm.readOnly.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val inner = LocalWindowLayout.current.isInner
    val aside = rememberMapAsideState()
    val haptics = rememberHaptics()

    var editorText by rememberSaveable(route.mapKey) { mutableStateOf(EditorState().encode()) }
    var viewportArray by rememberSaveable(route.mapKey) { mutableStateOf(Viewport.UNSET.toArray()) }
    var pinQuery by rememberSaveable { mutableStateOf("") }
    val editor = remember(editorText) { EditorState.decode(editorText) }
    fun set(s: EditorState) { editorText = s.encode() }
    val viewport = Viewport.fromArray(viewportArray)
    var contentWidth by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val barPx = with(density) { 96.dp.toPx() }

    val d = doc
    val ui = remember(d, hiddenOpen, editorText) { d?.let { EditState.build(it, route.mapKey, MapScreenViewModel.lensOf(it), hiddenOpen, editor) } }
    val map = d?.maps?.get(route.mapKey)
    val newMap = rememberNewMapFlow(route.projectKey, deps, vm, sheets)
    val menu = rememberMapMenuHost(route.projectKey, route.mapKey, deps, vm, sheets)
    val settings by deps.settings.collectAsState()
    val lens = d?.let { MapScreenViewModel.lensOf(it) } ?: LensPoint.Latest
    val tools = if (detail) EditTool.DETAIL else EditTool.BASE
    val layerActions = remember(vm, sheets) {
        object : LayerActions {
            override fun setVisible(layerKey: Key, visible: Boolean) { vm.setLayerVisible(layerKey, visible) }
            override fun rename(layerKey: Key) = sheets.open(LayerNameSheet(layerKey))
            override fun reveal(layerKey: Key) = sheets.open(RevealSheet(layerKey))
            override fun move(layerKey: Key, up: Boolean) { vm.moveLayer(layerKey, up) }
            override fun add() = sheets.open(LayerNameSheet(null))
        }
    }

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
            override fun setRouteMode(routeKey: Key, index: Int?) {
                if (index == null) sheets.open(TravelModeSheet(routeKey = routeKey)) else vm.setRouteMode(routeKey, index)
            }
            override fun openChildMap(mapKey: Key) = MapNav.open(deps.navigator, vm.doc.value, route.projectKey, mapKey)
            override fun createChildMap(pinKey: Key) = newMap.open(MapParent(route.mapKey, pinKey))
            override fun pickChildMap(pinKey: Key) = sheets.open(ChildMapSheet(pinKey))
            override fun setScale() = deps.navigator.push(MapScaleRoute(route.projectKey, route.mapKey))
            override fun movePin(pinKey: Key) = set(Editor.startMovePin(s(), pinKey))
            override fun openPlaceCard(placeKey: Key) = deps.navigator.push(PlaceCard(route.projectKey, placeKey))
            override fun setStampSize(stampKey: Key, size: Int) {
                val old = vm.doc.value?.maps?.get(route.mapKey)?.stamps?.firstOrNull { it.key == stampKey }?.size
                if (old != null && EditHaptics.sizePicked(old, size)) haptics.perform(HapticToken.TICK)
                vm.updateStamp(stampKey) { it.copy(size = size) }
            }
            override fun openStampPlace(stampKey: Key) = sheets.open(StampPlaceSheet(stampKey))
            override fun moveStamp() = set(Editor.startMoveStamp(s()))
            override fun undoLinePoint() = set(Editor.undoLinePoint(s()))
            override fun finishLine() {
                val st = s()
                val points = st.line ?: return
                val kind = if (st.tool == EditTool.ROAD) DrawnLineKind.ROAD else DrawnLineKind.RIVER
                vm.finishLine(kind, points) { key -> set(Editor.lineFinished(EditorState.decode(editorText), key)) }
            }
            override fun toggleCompare(index: Int) = set(Editor.toggleCompare(s(), index))
            override fun setCurve(labelKey: Key, on: Boolean) {
                val label = vm.doc.value?.maps?.get(route.mapKey)?.labels?.firstOrNull { it.key == labelKey } ?: return
                vm.setLabelCurve(labelKey, if (on) CurveGeometry.defaultCurve(Point(label.x, label.y)) else null)
            }
        }
    }

    // 상세 지형 · 편집 has no `경로` cell: the tour skips that step there.
    if (detail) TourStepAbsent(MapTourSteps.ROUTE)
    CivilightScreen(
        title = if (detail) MapCopy.DETAIL_TITLE else ui?.title.orEmpty(),
        smallLine = if (detail) map?.name else ui?.small,
        back = mapBack(deps),
        lens = doc?.let { io.github.lenticualrstone.civilight.ui.lens.lensChipLabel(it) },
        onLens = { sheets.open(io.github.lenticualrstone.civilight.navigation.LensSheet(route.projectKey)) },
        actions = {
            // LIB-19 map tour: `⋯` (its `시점별 공개` row) is the `보이는 시점` hole (MapTourSteps).
            CivilightIconButton(IconId.More, MapCopy.MORE, { sheets.open(MapMoreSheet) }, Modifier.tourTarget(MapTourSteps.REVEAL))
            CivilightIconButton(IconButtonSpec.MAP_UNDO, { vm.undoLast() }, enabled = !readOnly)
            TextAction(MapCopy.BTN_DONE, { deps.navigator.pop() })
        },
        banner = if (readOnly) ({ ReadOnlyBanner(MapCopy.READ_ONLY) }) else null,
        sheets = sheets,
        localSheets = { key ->
            val doc0 = doc
            val map0 = doc0?.maps?.get(route.mapKey)
            if (doc0 == null || map0 == null) null else menu.content(key, doc0, readOnly) ?: when (key) {
                MapMoreSheet -> {
                    {
                        MapMoreContent(
                            rows = MapMenu.rows(map0, MapMenuScreen.EDIT),
                            onRow = { action -> close(); menu.onRow(action, doc0) },
                            canScale = MapOffers.showsScale(map0),
                            defaultSize = settings.largeMapExport,
                            exporting = false,
                            onScale = { close(); deps.navigator.push(MapScaleRoute(route.projectKey, route.mapKey)) },
                            onReveal = { close(); deps.navigator.push(MapRevealRoute(route.projectKey, route.mapKey)) },
                            onExport = {},
                            onClose = ::close,
                            canExport = false,
                        )
                    }
                }
                is StampPlaceSheet -> {
                    {
                        PlacePickerSheet(doc0, gate, MapCopy.PLACE_LINK, onPick = { ref ->
                            close()
                            vm.updateStamp(key.stampKey) { it.copy(placeKey = ref.key) }
                        }, onClose = ::close, includeRegions = false)
                    }
                }
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
                is NewMapSheet -> { { newMap.content(this, key) } }
                is LayerNameSheet -> {
                    {
                        val existing = key.layerKey?.let { k -> map0.layers.firstOrNull { it.key == k } }
                        LayerNameContent(existing?.name.orEmpty(), existing != null, onDone = { name ->
                            close()
                            if (existing != null) vm.renameLayer(existing.key, name) else vm.addLayer(name)
                        }, onClose = ::close)
                    }
                }
                is ChildMapSheet -> {
                    {
                        val maps = MapNesting.candidates(doc0, route.mapKey, key.pinKey).map { it.key to it.name }
                        ChildMapContent(maps, onPick = { child ->
                            close()
                            vm.nestMap(child, key.pinKey)
                        }, onClose = ::close)
                    }
                }
                is TravelModeSheet -> {
                    {
                        val routeKey = key.routeKey
                        val shown = routeKey?.let { k -> map0.routes.firstOrNull { it.key == k }?.shownModeKey }
                        TravelModeContent(doc0, shown, readOnly, onPick = null, onSave = { modeKey, name, km, hours ->
                            vm.saveTravelMode(modeKey, name, km, hours) { saved ->
                                if (modeKey == null && routeKey != null) vm.setRouteModeKey(routeKey, saved)
                            }
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
                onContentWidth = { contentWidth = it },
                hitTerrain = detail,
                selectedKey = editor.movingPin ?: editor.selected,
                onTap = { p, hit ->
                    val m = map ?: return@MapCanvas
                    val out = Editor.tap(editor, m, p, hit)
                    if (EditHaptics.placedPoint(editor, out.state) || out.commit is EditorCommit.PlaceStamp) haptics.perform(HapticToken.TICK)
                    set(out.state)
                    when (out.effect) {
                        EditorEffect.PICK_PLACE -> { pinQuery = ""; sheets.open(PinPlaceSheet(p.x, p.y)) }
                        EditorEffect.LABEL_TEXT -> sheets.open(LabelTextSheet(p.x, p.y))
                        null -> Unit
                    }
                    when (val c = out.commit) {
                        is EditorCommit.MoveVertex -> when {
                            m.drawnLines.any { it.key == c.regionKey } -> vm.moveLinePoint(c.regionKey, c.index, c.to)
                            else -> {
                                // WLD-65: under the lens, or with `◆` history, the author chooses first.
                                val d0 = vm.doc.value
                                if (d0 != null && ShapeEdits.choice(d0, route.mapKey, c.regionKey, lens) is EditChoice.TwoButtons) {
                                    set(Editor.askShapeEdit(out.state, c.regionKey, c.index, c.to))
                                } else {
                                    vm.overwriteShape(c.regionKey, lens, c.index, c.to)
                                }
                            }
                        }
                        is EditorCommit.MoveCurveHandle -> when {
                            m.stamps.any { it.key == c.labelKey } -> {
                                val st = m.stamps.first { it.key == c.labelKey }
                                val rotate = c.index == StampHandles.ROTATE
                                if (if (rotate) EditHaptics.rotationDetent(st, c.to) else EditHaptics.sizeStep(st, c.to)) haptics.perform(HapticToken.TICK)
                                if (rotate) vm.rotateStampToward(c.labelKey, c.to) else vm.resizeStampToward(c.labelKey, c.to)
                            }
                            else -> {
                                val curve = m.labels.firstOrNull { it.key == c.labelKey }?.curve
                                if (curve != null && c.index in curve.indices) {
                                    vm.setLabelCurve(c.labelKey, curve.mapIndexed { i, q -> if (i == c.index) c.to else q })
                                }
                            }
                        }
                        is EditorCommit.PlaceStamp -> vm.placeStamp(c.at, c.kind, c.size) { key -> set(Editor.select(EditorState.decode(editorText), key)) }
                        is EditorCommit.MoveStamp -> vm.moveStamp(c.stampKey, c.to)
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
                    line(state.lineDraft, closed = false)
                    state.lineDraft.forEach { q -> val s0 = scope.screen(q); drawCircle(acc, 4.dp.toPx(), Offset(s0.x, s0.y)) }
                    state.compare?.let { (other, current) ->
                        // WLD-65 `비교`: the other shape dashed; the part of it outside the current shape dotted.
                        fun path(points: List<Point>) = Path().apply {
                            points.forEachIndexed { i, q -> val s0 = scope.screen(q); if (i == 0) moveTo(s0.x, s0.y) else lineTo(s0.x, s0.y) }
                            close()
                        }
                        val added = Path().apply { op(path(other), path(current), PathOperation.Difference) }
                        drawPath(added, acc.copy(alpha = 0.18f))
                        drawPath(added, acc, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()))))
                        drawPath(path(other), acc, style = Stroke(1.5.dp.toPx(), pathEffect = dash))
                    }
                    state.routeDraft.forEach { q -> val s = scope.screen(q); drawCircle(acc, 4.dp.toPx(), Offset(s.x, s.y)) }
                },
                marks = { scope ->
                    state.regionDraft.forEachIndexed { i, q ->
                        val s = scope.screen(q)
                        MapVertex(Modifier.mapAt(s.x, s.y), last = i == state.regionDraft.lastIndex)
                    }
                    val props = state.props
                    if (props is PropsUi.StampProps && (editor.tool == EditTool.MOVE || editor.tool == EditTool.STAMP)) {
                        val c = scope.screen(props.data.at)
                        val r = maxOf(StampGlyphs.radius(props.data.size) * scope.math.contentW(scope.viewport), with(density) { (4 + 3 * props.data.size).dp.toPx() })
                        MapValueChip(props.data.chip, Modifier.mapAt(c.x, c.y + r + with(density) { 22.dp.toPx() }))
                        MapVertex(Modifier.mapAt(c.x, c.y - r * 1.6f), last = editor.handle == StampHandles.ROTATE, onClick = { set(Editor.chooseHandle(editor, StampHandles.ROTATE)) }, popOnAppear = false)
                        MapVertex(Modifier.mapAt(c.x + r * 1.1f, c.y + r * 1.1f), last = editor.handle == StampHandles.SIZE, onClick = { set(Editor.chooseHandle(editor, StampHandles.SIZE)) }, popOnAppear = false)
                    }
                    if (props is PropsUi.LineProps && editor.tool == EditTool.MOVE) {
                        props.points.forEachIndexed { i, q ->
                            val s0 = scope.screen(q)
                            MapVertex(Modifier.mapAt(s0.x, s0.y), last = editor.vertex == i, onClick = { set(Editor.chooseVertex(editor, i)) }, popOnAppear = false)
                        }
                    }
                    if (editor.tool == EditTool.MOVE || editor.tool == EditTool.LAYER) {
                        if (props is PropsUi.RegionProps) {
                            props.vertices.forEachIndexed { i, q ->
                                val s = scope.screen(q)
                                MapVertex(Modifier.mapAt(s.x, s.y), last = editor.vertex == i, onClick = { set(Editor.chooseVertex(editor, i)) }, popOnAppear = false)
                            }
                        }
                        if (props is PropsUi.LabelProps) {
                            props.curve?.forEachIndexed { i, q ->
                                val s = scope.screen(q)
                                MapVertex(Modifier.mapAt(s.x, s.y), last = editor.handle == i, onClick = { set(Editor.chooseHandle(editor, i)) }, popOnAppear = false)
                            }
                        }
                    }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            val toolCells = remember(detail) { tools.map { MapTool(it.label, it.icon) } }
            val motion = CivilightTheme.motion
            val toolbar: @Composable (Modifier) -> Unit = { mod ->
                // LIB-19 map tour: the tool bar and its `경로` cell are the holes of `도구 줄` and `경로` (MapTourSteps).
                MapToolbar(
                    tools.indexOf(editor.tool).coerceAtLeast(0),
                    { set(Editor.chooseTool(editor, tools[it], detail)) },
                    mod.tourTarget(MapTourSteps.TOOLBAR).animateContentSize(motion.move(CivilightMotion.standard)),
                    tools = toolCells,
                    label = MapCopy.TOOLBAR,
                ) { i -> MapTourSteps.stepOfTool(tools[i])?.let { Modifier.tourTarget(it) } ?: Modifier }
            }
            val shapeEdit = editor.shapeEdit
            val shapeChoice = shapeEdit?.let { pending -> d?.let { ShapeEdits.choice(it, route.mapKey, pending.regionKey, lens) } as? EditChoice.TwoButtons }
            val moving = state.moving
            val scaleLabel = map?.takeIf(MapOffers::showsScale)?.let { m -> d?.let { ScaleBars.label(it, m, contentWidth, barPx) } }
            // The bar above the tool bar (move, shape choice, and on the cover the layer, property and stamp cards).
            val barContent: @Composable (EditBar, String, Boolean) -> Unit = { b, kind, isInner ->
                val st = b.state
                val ed = b.editor
                val mod = Modifier.padding(bottom = 10.dp)
                when (kind) {
                    MapBars.MOVE -> {
                        val mv = st.moving
                        if (mv != null) {
                            MovePinBar(mv.first, mv.second != null && !b.readOnly, { set(Editor.endMovePin(ed)) }, {
                                val to = mv.second ?: return@MovePinBar
                                ed.movingPin?.let { k -> vm.movePin(k, to) { set(Editor.endMovePin(EditorState.decode(editorText))) } }
                            }, if (isInner) mod.widthIn(max = 360.dp) else mod.fillMaxWidth())
                        }
                    }
                    MapBars.SHAPE -> {
                        val pending = b.pending
                        val choice = b.choice
                        if (pending != null && choice != null) {
                            ShapeChoiceBar(
                                choice, b.readOnly,
                                onOverwrite = { vm.overwriteShape(pending.regionKey, lens, pending.index, pending.to); set(Editor.endShapeEdit(ed)) },
                                onFromHere = { vm.changeShapeFromHere(pending.regionKey, lens, pending.index, pending.to); set(Editor.endShapeEdit(ed)) },
                                onCancel = { set(Editor.endShapeEdit(ed)) },
                                modifier = if (isInner) mod.widthIn(max = 360.dp) else mod.fillMaxWidth(),
                            )
                        }
                    }
                    MapBars.LAYERS -> GlassCard((if (isInner) mod.width(360.dp) else mod.fillMaxWidth()).heightIn(max = 320.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState())) { LayersPanel(st.layers, b.readOnly, layerActions) }
                    }
                    MapBars.PROPS -> st.props?.let { props ->
                        GlassCard((if (isInner) mod.width(360.dp) else mod.fillMaxWidth()).heightIn(max = 360.dp)) {
                            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { PropsPanel(props, b.readOnly, actions, ed.compare) }
                        }
                    }
                    MapBars.STAMPS -> GlassCard((if (isInner) mod.width(360.dp) else mod.fillMaxWidth()).heightIn(max = 320.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StampPalette(ed.stampKind, ed.stampSize, { set(Editor.chooseStamp(ed, it)) }, { set(Editor.chooseStampSize(ed, it)) })
                        }
                    }
                }
            }
            SidePanelHost(
                inner,
                Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + MapAsideRules.TOP_GAP.dp, bottom = padding.calculateBottomPadding() + 12.dp),
            ) {
                MapAside(
                    state = aside,
                    tabs = if (detail) listOf(MapCopy.TAB_STAMPS, MapCopy.TAB_LEGEND, MapCopy.TAB_LAYERS) else listOf(MapCopy.TAB_PROPS, MapCopy.TAB_LAYERS, MapCopy.TAB_LIST),
                    selectedTab = editor.panelTab,
                    onSelectTab = { set(editor.copy(panelTab = it)) },
                    onBack = if (editor.selected != null) actions::deselect else null,
                ) {
                    TabContent(editor.panelTab) { tab ->
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (detail) {
                                when (tab) {
                                    1 -> LegendPanel(state.legend)
                                    2 -> LayersPanel(state.layers, readOnly, layerActions)
                                    else -> {
                                        state.props?.let { PropsPanel(it, readOnly, actions, editor.compare) }
                                        if (state.props == null || editor.tool == EditTool.STAMP) {
                                            StampPalette(editor.stampKind, editor.stampSize, { set(Editor.chooseStamp(editor, it)) }, { set(Editor.chooseStampSize(editor, it)) })
                                        }
                                    }
                                }
                            } else {
                                when (tab) {
                                    1 -> LayersPanel(state.layers, readOnly, layerActions)
                                    2 -> ItemsPanel(state.items) { set(Editor.select(editor, it)) }
                                    else -> state.props?.let { PropsPanel(it, readOnly, actions, editor.compare) }
                                }
                            }
                        }
                    }
                }
            }
            FoldSwitch(inner) { isInner ->
                val kind = MapBars.edit(
                    moving != null, shapeChoice != null, editor.tool == EditTool.LAYER, state.props != null,
                    detail && editor.tool == EditTool.STAMP, MapBars.panelShown(isInner, aside.closed),
                )
                val slot = kind?.let { MapBarSlot(it, EditBar(state, editor, shapeChoice, shapeEdit, readOnly)) }
                if (isInner) {
                    val asideInset = if (aside.closed) 20.dp else 276.dp
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = asideInset, bottom = padding.calculateBottomPadding() + 20.dp)) {
                        scaleLabel?.let { ScaleBar(it, Modifier.padding(bottom = 10.dp)) }
                        MapBar(slot) { k, b -> barContent(b, k, true) }
                        toolbar(Modifier)
                    }
                } else {
                    Column(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MapBar(slot, Modifier.fillMaxWidth()) { k, b -> barContent(b, k, false) }
                        toolbar(Modifier)
                    }
                }
            }
        }
    }
}

/** What the bar above the tool bar draws: a snapshot of everything it needs, so the bar that leaves keeps its last content. */
private data class EditBar(val state: EditUi, val editor: EditorState, val choice: EditChoice.TwoButtons?, val pending: ShapeEditState?, val readOnly: Boolean)
