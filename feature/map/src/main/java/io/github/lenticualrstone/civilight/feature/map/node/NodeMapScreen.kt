package io.github.lenticualrstone.civilight.feature.map.node

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
import io.github.lenticualrstone.civilight.designsystem.component.DelayedProgress
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.MapTool
import io.github.lenticualrstone.civilight.designsystem.component.MapToolbar
import io.github.lenticualrstone.civilight.designsystem.component.ReadOnlyBanner
import io.github.lenticualrstone.civilight.designsystem.component.SidePanel
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.FoldSwitch
import io.github.lenticualrstone.civilight.feature.map.common.MapBar
import io.github.lenticualrstone.civilight.feature.map.common.MapBarSlot
import io.github.lenticualrstone.civilight.feature.map.common.MapBars
import io.github.lenticualrstone.civilight.feature.map.canvas.rememberViewportMover
import androidx.compose.animation.animateContentSize
import io.github.lenticualrstone.civilight.designsystem.component.SidePanelHost
import io.github.lenticualrstone.civilight.designsystem.component.TabContent
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.edit.EditState
import io.github.lenticualrstone.civilight.feature.map.edit.ItemsPanel
import io.github.lenticualrstone.civilight.feature.map.edit.LabelTextContent
import io.github.lenticualrstone.civilight.feature.map.edit.LabelTextSheet
import io.github.lenticualrstone.civilight.feature.map.edit.LayerActions
import io.github.lenticualrstone.civilight.feature.map.edit.LayerNameContent
import io.github.lenticualrstone.civilight.feature.map.edit.LayerNameSheet
import io.github.lenticualrstone.civilight.feature.map.edit.LayerPickContent
import io.github.lenticualrstone.civilight.feature.map.edit.LayerSheet
import io.github.lenticualrstone.civilight.feature.map.edit.LayersPanel
import io.github.lenticualrstone.civilight.feature.map.edit.RevealContent
import io.github.lenticualrstone.civilight.feature.map.edit.RevealSheet
import io.github.lenticualrstone.civilight.feature.map.edit.point
import io.github.lenticualrstone.civilight.feature.map.export.rememberMapExporter
import io.github.lenticualrstone.civilight.feature.map.view.MapMoreContent
import io.github.lenticualrstone.civilight.feature.map.view.MapMoreSheet
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.MapReveal
import io.github.lenticualrstone.civilight.navigation.NodeMap
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * 노드 지도 (WLD-50, board `MapNode`): header `<부모> › <이름>` and `노드 지도`, lens chip, `⋯` (시점별
 * 공개, 지도 내보내기); the node cards and their connections on the base-less surface; the tool bar
 * `이동 · 노드 · 연결 · 라벨 · 레이어` (only the chosen tool acts; a connection is two taps); the command
 * bar `/ 장소 · 노드 찾기` with the 라이프 레코더 button. Inner: the side panel `속성 · 레이어 · 목록`;
 * cover: a bottom card. The editor state is saveable, so a fold keeps a half-made connection.
 */
@Composable
fun NodeMapScreen(route: NodeMap, deps: FeatureDeps) {
    val vm = entryViewModel { NodeMapViewModel(route.projectKey, route.mapKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val settings by deps.settings.collectAsState()
    val sheets = rememberSheetHost()
    val menu = io.github.lenticualrstone.civilight.feature.map.common.rememberMapMenuHost(route.projectKey, route.mapKey, deps, vm, sheets)
    val gate = rememberHiddenGate(route.projectKey)
    val inner = LocalWindowLayout.current.isInner
    val exporter = rememberMapExporter(deps)

    var editorText by rememberSaveable(route.mapKey) { mutableStateOf(NodeEditorState().encode()) }
    var viewportArray by rememberSaveable(route.mapKey) { mutableStateOf(Viewport.UNSET.toArray()) }
    val mover = rememberViewportMover({ Viewport.fromArray(viewportArray) }) { viewportArray = it.toArray() }
    val editor = remember(editorText) { NodeEditorState.decode(editorText) }
    fun set(s: NodeEditorState) { editorText = s.encode() }
    fun current() = NodeEditorState.decode(editorText)

    val d = doc
    val ui = remember(d, hiddenOpen, editorText) { d?.let { NodeMapState.build(it, route.mapKey, MapScreenViewModel.lensOf(it), hiddenOpen, editor) } }

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
        object : NodeActions {
            override fun deselect() = set(NodeEditor.select(current(), null))
            override fun editNode(nodeKey: Key) = sheets.open(NodeNameSheet(nodeKey = nodeKey))
            override fun openPlace(nodeKey: Key) = sheets.open(NodePlaceSheet(nodeKey))
            override fun openPlaceCard(placeKey: Key) = deps.navigator.push(PlaceCard(route.projectKey, placeKey))
            override fun startMove(nodeKey: Key) = set(NodeEditor.startMove(current(), nodeKey))
            override fun startLink(nodeKey: Key) = set(NodeEditor.startLink(current(), nodeKey))
            override fun openReveal(key: Key) = sheets.open(RevealSheet(key))
            override fun openLinkLabel(linkKey: Key) = sheets.open(LinkLabelSheet(linkKey))
            override fun openLabelText(labelKey: Key) = sheets.open(LabelTextSheet(labelKey = labelKey))
            override fun openLayer(key: Key) = sheets.open(LayerSheet(key))
        }
    }

    CivilightScreen(
        title = MapCopy.NODE_TITLE,
        smallLine = ui?.small,
        back = mapBack(deps),
        lens = d?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        actions = { CivilightIconButton(IconId.More, MapCopy.MORE, { sheets.open(MapMoreSheet) }) },
        commandBar = {
            CommandBar(
                hint = MapCopy.NODE_SEARCH,
                onClick = { sheets.open(NodeSearchSheet) },
                onMic = { deps.navigator.push(Recording(projectKey = route.projectKey)) },
                micLabel = MapCopy.RECORDER,
            )
        },
        banner = if (readOnly) ({ ReadOnlyBanner(MapCopy.READ_ONLY) }) else null,
        sheets = sheets,
        localSheets = { key ->
            val doc0 = doc
            val map0 = doc0?.maps?.get(route.mapKey)
            if (doc0 == null || map0 == null) null else menu.content(key, doc0, readOnly) ?: when (key) {
                is NodeNameSheet -> {
                    {
                        val existing = key.nodeKey?.let { k -> map0.nodes.firstOrNull { it.key == k } }
                        NodeNameContent(existing?.name.orEmpty(), existing?.sub.orEmpty(), existing != null, onDone = { name, sub ->
                            close()
                            if (existing != null) {
                                vm.editNode(existing.key, name, sub)
                            } else {
                                vm.addNode(name, sub, key.point()) { node -> set(NodeEditor.placed(current(), node)) }
                            }
                        }, onClose = ::close)
                    }
                }
                is LinkLabelSheet -> {
                    {
                        val link = map0.nodeLinks.firstOrNull { it.key == key.linkKey }
                        LinkLabelContent(link?.label.orEmpty(), onDone = { text -> close(); vm.setLinkLabel(key.linkKey, text) }, onClose = ::close)
                    }
                }
                is NodePlaceSheet -> {
                    {
                        val current = map0.nodes.firstOrNull { it.key == key.nodeKey }?.placeKey
                        NodePlaceContent(doc0, gate, current, onPick = { place -> close(); vm.linkPlace(key.nodeKey, place) }, onClose = ::close)
                    }
                }
                is NodeSearchSheet -> {
                    {
                        val scene = ui?.scene
                        NodeSearchContent(
                            search = { q -> if (scene == null) emptyList() else NodeMapState.search(doc0, route.mapKey, scene, q) },
                            onPick = { nodeKey ->
                                close()
                                set(NodeEditor.select(current(), nodeKey))
                                scene?.nodes?.firstOrNull { it.key == nodeKey }?.let { n ->
                                    val now = Viewport.fromArray(viewportArray)
                                    mover.moveTo(Viewport(zoom = now.zoom.coerceAtLeast(Viewport.MIN_ZOOM)).centeredOn(n.at))
                                }
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
                                vm.addLabel(text, key.point()) { label -> set(NodeEditor.placed(current(), label)) }
                            }
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
                is LayerSheet -> {
                    {
                        val target = key.target
                        val currentLayer = target?.let { t -> map0.labels.firstOrNull { it.key == t }?.layerKey }
                        LayerPickContent(ui?.layers.orEmpty(), currentLayer, onPick = { layer ->
                            close()
                            if (target != null) vm.setLayer(target, layer, MapCopy.step(MapCopy.LAYER, EditState.layerLabel(map0, layer)))
                        }, onClose = ::close)
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
                is MapMoreSheet -> {
                    {
                        MapMoreContent(
                            rows = io.github.lenticualrstone.civilight.feature.map.view.MapMenu.rows(map0, io.github.lenticualrstone.civilight.feature.map.view.MapMenuScreen.NODE),
                            onRow = { action -> close(); menu.onRow(action, doc0) },
                            canScale = false,
                            defaultSize = settings.largeMapExport,
                            exporting = exporter.exporting,
                            onScale = {},
                            onReveal = { close(); deps.navigator.push(MapReveal(route.projectKey, route.mapKey)) },
                            onExport = { size ->
                                close()
                                val state = ui
                                if (state != null) exporter.export(state.base, state.scene, map0.name, size)
                            },
                            onClose = ::close,
                        )
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        val state = ui ?: return@CivilightScreen
        Box(Modifier.fillMaxSize()) {
            MapCanvas(
                base = state.base,
                scene = state.scene,
                viewport = Viewport.fromArray(viewportArray),
                onViewportChange = { mover.cancel(); viewportArray = it.toArray() },
                selectedKey = editor.movingNode ?: editor.linkFrom ?: editor.selected,
                onTap = { p, hit ->
                    val linkKey = if (hit == null) NodeEditor.linkAt(state.scene, p) else null
                    val out = NodeEditor.tap(editor, p, hit, linkKey)
                    if (readOnly && (out.effect != null || out.commit != null)) {
                        // Read-only (the example): taps only select; nothing opens or writes.
                        set(NodeEditor.select(editor.copy(linkFrom = null, movingNode = null), (hit as? io.github.lenticualrstone.civilight.feature.map.canvas.MapHit.Node)?.key))
                        deps.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
                        return@MapCanvas
                    }
                    set(out.state)
                    when (out.effect) {
                        NodeEffect.NEW_NODE -> sheets.open(NodeNameSheet(p.x, p.y))
                        NodeEffect.LABEL_TEXT -> sheets.open(LabelTextSheet(p.x, p.y))
                        null -> Unit
                    }
                    when (val c = out.commit) {
                        is NodeCommit.Link -> vm.link(c.a, c.b)
                        is NodeCommit.MoveNode -> vm.moveNode(c.nodeKey, c.to)
                        null -> Unit
                    }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            DelayedProgress(exporter.exporting, Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding() + 12.dp)) { MapValueChip(MapCopy.EXPORT) }
            val tools = remember { NodeTool.entries.map { MapTool(it.label, it.icon) } }
            val motion = CivilightTheme.motion
            val toolbar: @Composable (Modifier) -> Unit = { mod ->
                MapToolbar(editor.tool.ordinal, { set(NodeEditor.chooseTool(editor, NodeTool.entries[it])) }, mod.animateContentSize(motion.move(CivilightMotion.standard)), tools = tools, label = MapCopy.TOOLBAR)
            }
            // The bar above the tool bar: the `이동` / `연결` step, and on the cover the layer card or the property card.
            val barContent: @Composable (NodeBar, String, Boolean) -> Unit = { b, kind, isInner ->
                val mod = Modifier.padding(bottom = 10.dp)
                when (kind) {
                    MapBars.STEP -> {
                        val text = b.state.moving ?: b.state.linking
                        if (text != null) {
                            GlassCard(if (isInner) mod.widthIn(max = 360.dp) else mod.fillMaxWidth()) {
                                NodeStepBar(text, { set(if (b.state.moving != null) NodeEditor.cancelMove(b.editor) else NodeEditor.cancelLink(b.editor)) })
                            }
                        }
                    }
                    MapBars.LAYERS -> GlassCard(mod.fillMaxWidth().heightIn(max = 320.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState())) { LayersPanel(b.state.layers, readOnly, layerActions) }
                    }
                    MapBars.PROPS -> b.state.props?.let { props ->
                        GlassCard(mod.fillMaxWidth().heightIn(max = 340.dp)) {
                            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                NodePropsPanel(props, readOnly, actions)
                            }
                        }
                    }
                }
            }
            SidePanelHost(
                inner,
                Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + 8.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 12.dp),
            ) {
                SidePanel(
                    onClose = actions::deselect,
                    tabs = listOf(MapCopy.TAB_PROPS, MapCopy.TAB_LAYERS, MapCopy.TAB_LIST),
                    selectedTab = editor.panelTab,
                    onSelectTab = { set(editor.copy(panelTab = it)) },
                ) {
                    TabContent(editor.panelTab) { tab ->
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (tab) {
                                1 -> LayersPanel(state.layers, readOnly, layerActions)
                                2 -> ItemsPanel(state.items) { set(NodeEditor.select(editor, it)) }
                                else -> state.props?.let { NodePropsPanel(it, readOnly, actions) }
                            }
                        }
                    }
                }
            }
            FoldSwitch(inner) { isInner ->
                val kind = MapBars.node(state.moving != null || state.linking != null, editor.tool == NodeTool.LAYER, state.props != null, isInner)
                val slot = kind?.let { MapBarSlot(it, NodeBar(state, editor)) }
                if (isInner) {
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = padding.calculateBottomPadding() + 20.dp)) {
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

/** What the bar above the tool bar draws: a snapshot, so the bar that leaves keeps its last content. */
private data class NodeBar(val state: NodeMapUi, val editor: NodeEditorState)
