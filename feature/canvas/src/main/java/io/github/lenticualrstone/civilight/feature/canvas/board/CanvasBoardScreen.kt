package io.github.lenticualrstone.civilight.feature.canvas.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.feature.canvas.edit.LineResult
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphGeometry
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphHit
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphScene
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphSelection
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphSurface
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphSurfaceListener
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphViewport
import io.github.lenticualrstone.civilight.feature.canvas.graph.Pt
import io.github.lenticualrstone.civilight.feature.canvas.graph.Sz
import io.github.lenticualrstone.civilight.feature.canvas.label.LineLabelPopoverBody
import io.github.lenticualrstone.civilight.feature.canvas.label.LineStartSheet
import io.github.lenticualrstone.civilight.feature.canvas.label.rememberLineLabelState
import io.github.lenticualrstone.civilight.feature.canvas.node.NodeEditModel
import io.github.lenticualrstone.civilight.feature.canvas.node.NodeEditRows
import io.github.lenticualrstone.civilight.feature.canvas.node.NodeEditSheet
import io.github.lenticualrstone.civilight.feature.canvas.node.NodeEditView
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.CanvasBoard
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.LineLabelSheet
import io.github.lenticualrstone.civilight.navigation.RelationKindSettings
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

private val BoardUiSaver = listSaver<BoardUi, String>(
    save = { ui -> ui.save().map { it.orEmpty() } },
    restore = { saved -> BoardUi.restore(saved.map { it.ifEmpty { null } }) },
)

/** Bottom space the tool bar takes (18 + 56 dp), used when fitting the canvas into view. */
private const val TOOLBAR_SPACE = 80f

/** 캔버스 (CNV-02; boards `CanvasC` cover, `Canvas` inner). */
@Composable
fun CanvasBoardScreen(route: CanvasBoard, deps: FeatureDeps) {
    val vm = entryViewModel { CanvasBoardViewModel(route.projectKey, deps.projects, deps::canvasEditContext) }
    val load by vm.state.collectAsState()
    val sheets = rememberSheetHost()
    val back = IconAction(CanvasCopy.BACK) { deps.navigator.pop() }
    val ready = load as? ProjectLoad.Ready
    if (ready == null) {
        CivilightScreen(title = "", smallLine = CanvasCopy.SMALL_LINE, back = back) { }
        return
    }
    val canvas = CanvasEdits.resolve(ready.doc, route.canvasKey)
    if (canvas == null) {
        NoCanvas(ready.readOnly, back, vm)
        return
    }
    CanvasBoard(route, deps, vm, ready.doc, ready.readOnly, canvas, sheets, back)
}

/** A project without a canvas: one is made (CNV-02 [PROPOSED]); a read-only project shows an empty state. */
@Composable
private fun NoCanvas(readOnly: Boolean, back: IconAction, vm: CanvasBoardViewModel) {
    if (!readOnly) LaunchedEffect(Unit) { vm.createFirstCanvas() }
    CivilightScreen(title = "", smallLine = CanvasCopy.SMALL_LINE, back = back) { padding ->
        if (readOnly) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                EmptyBlock(title = CanvasCopy.TODO_COPY_CANVAS_NONE)
            }
        }
    }
}

@Composable
private fun CanvasBoard(
    route: CanvasBoard,
    deps: FeatureDeps,
    vm: CanvasBoardViewModel,
    doc: ProjectDoc,
    readOnly: Boolean,
    canvas: Canvas,
    sheets: SheetHost,
    back: IconAction,
) {
    val projectKey = route.projectKey
    val cover = !LocalWindowLayout.current.isInner
    val gate = rememberHiddenGate(projectKey)
    val lens = LensRules.effective(doc, doc.app.view.lens.point)
    var ui by rememberSaveable(canvas.key, stateSaver = BoardUiSaver) { mutableStateOf(BoardUi()) }
    /** Inner screens: the popover of the selected node or line is open. */
    var popover by rememberSaveable(canvas.key) { mutableStateOf(false) }
    val moved = remember(canvas.key) { mutableStateMapOf<Key, Pt>() }
    val sizes = remember(canvas.key) { mutableStateMapOf<String, Sz>() }
    var viewport by remember(canvas.key) { mutableStateOf(doc.app.view.canvasViewports[canvas.key]?.toGraph()) }
    val relationLines = remember(doc, canvas, lens, gate.open) { BoardScene.relationLines(doc, canvas, lens, gate.open) }
    val scene = BoardScene.build(doc, canvas, lens, gate.open, moved, relationLines)

    // A dropped node keeps its on-screen position until the document has it (no flicker back).
    LaunchedEffect(canvas) { dropSettled(canvas, moved) }

    val readOnlyToast = { deps.toasts.show(CanvasCopy.READ_ONLY, actionLabel = null) }
    val openNode = { key: Key -> if (cover) sheets.open(NodeEditSheet(projectKey, canvas.key, key)) else popover = true }
    val openLine = { key: Key -> if (cover) sheets.open(LineLabelSheet(projectKey, canvas.key, key)) else popover = true }
    val clearSelection = {
        ui = BoardReducer.clear(ui)
        popover = false
    }

    CivilightScreen(
        title = canvas.name,
        smallLine = CanvasCopy.SMALL_LINE,
        back = back,
        lens = lensChipLabel(doc),
        onLens = { sheets.open(LensSheet(projectKey)) },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is NodeEditSheet -> {
                    {
                        val view = remember(doc, key, gate.open, readOnly) { NodeEditModel.build(doc, key.canvasKey, key.nodeKey, gate.open, readOnly) }
                        if (view == null) {
                            SheetHeader(title = "", onClose = ::close, closeLabel = CanvasCopy.BTN_CLOSE)
                        } else {
                            SheetHeader(title = view.title, subtitle = view.kindWord, onClose = { close(); clearSelection() }, closeLabel = CanvasCopy.BTN_CLOSE)
                            NodeEditRows(
                                view = view,
                                compact = false,
                                onText = { vm.setText(key.canvasKey, key.nodeKey, it) },
                                onOpen = { close(); view.openRoute?.let(deps.navigator::push) },
                                onRemove = { removeNode(vm, key.canvasKey, view, readOnlyToast); close(); clearSelection() },
                            )
                        }
                    }
                }
                is PlaceItemSheet -> {
                    {
                        PlaceItemContent(
                            sheet = key,
                            doc = doc,
                            gate = gate,
                            onPick = { kind, ref ->
                                vm.addItem(key.canvasKey, kind, ref, centred(Pt(key.x, key.y))) { r, _ -> if (r == EditResult.READ_ONLY) readOnlyToast() }
                                close()
                            },
                            onClose = ::close,
                        )
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val topInset = padding.calculateTopPadding().value
            val vp = viewport ?: GraphViewport.fit(
                GraphGeometry.bounds(scene, sizes), maxWidth.value, maxHeight.value, topInset, TOOLBAR_SPACE,
            )
            val center = Pt(maxWidth.value / 2f, topInset + (maxHeight.value - topInset) / 2f)
            val listener = object : GraphSurfaceListener {
                override fun onTap(hit: GraphHit, at: Pt) {
                    val (next, action) = BoardReducer.tap(ui, hit, at, readOnly)
                    ui = next
                    popover = false
                    when (action) {
                        BoardAction.None -> Unit
                        BoardAction.ReadOnly -> readOnlyToast()
                        is BoardAction.EditNode -> openNode(action.nodeKey)
                        is BoardAction.EditLine -> openLine(action.edgeKey)
                        is BoardAction.PickItem -> sheets.open(PlaceItemSheet(projectKey, canvas.key, action.tool, action.at.x, action.at.y))
                        is BoardAction.AddMemo -> vm.addMemo(canvas.key, centred(action.at)) { r, key ->
                            if (r == EditResult.READ_ONLY) readOnlyToast()
                            key?.let {
                                ui = ui.copy(selection = GraphSelection(nodeKey = it))
                                openNode(it)
                            }
                        }
                        is BoardAction.AddGroup -> vm.addGroup(canvas.key, centred(action.at, CanvasEdits.GROUP_SIZE)) { r, _ ->
                            if (r == EditResult.READ_ONLY) readOnlyToast()
                        }
                        is BoardAction.DrawLine -> vm.drawLine(canvas.key, action.from, action.to) { r, result ->
                            if (r == EditResult.READ_ONLY) readOnlyToast()
                            val edgeKey = when (result) {
                                is LineResult.Created -> result.edgeKey
                                is LineResult.Existing -> result.edgeKey
                                LineResult.Refused -> null
                            }
                            if (edgeKey != null) {
                                ui = BoardReducer.lineDrawn(ui, edgeKey)
                                openLine(edgeKey)
                            }
                        }
                    }
                }

                override fun canDrag(nodeKey: String): Boolean = !readOnly

                override fun onDrag(nodeKey: String, delta: Pt) {
                    val node = canvas.nodes.firstOrNull { it.key == nodeKey } ?: return
                    val base = moved[nodeKey] ?: Pt(node.x, node.y)
                    moved[nodeKey] = base + delta
                }

                override fun onDragEnd(nodeKey: String) {
                    val at = moved[nodeKey] ?: return
                    vm.move(canvas.key, nodeKey, at, sizes[nodeKey] ?: Sz.DEFAULT) { r ->
                        if (r != EditResult.APPLIED) moved.remove(nodeKey)
                        if (r == EditResult.READ_ONLY) readOnlyToast()
                    }
                }

                override fun onViewport(viewport: GraphViewport) {
                    popover = false
                    setViewport(viewport)
                }

                override fun onViewportSettled(viewport: GraphViewport) {
                    setViewport(viewport)
                    vm.saveViewport(canvas.key, viewport)
                }

                private fun setViewport(next: GraphViewport) {
                    viewport = next
                }
            }
            GraphSurface(scene = scene, viewport = vp, selection = ui.selection, sizes = sizes, listener = listener, cover = cover)

            ZoomColumn(
                percent = CanvasCopy.zoomPercent(vp.zoom),
                canIn = vp.canZoomIn,
                canOut = vp.canZoomOut,
                onIn = { vp.zoomIn(center).also { viewport = it; vm.saveViewport(canvas.key, it) } },
                onOut = { vp.zoomOut(center).also { viewport = it; vm.saveViewport(canvas.key, it) } },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = (topInset + 16f).dp, end = 12.dp),
            )
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (cover) CanvasHint(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 18.dp))
                CanvasToolBar(
                    current = ui.tool,
                    cover = cover,
                    onSelect = { tool ->
                        ui = BoardReducer.selectTool(ui, tool)
                        popover = false
                    },
                    modifier = if (cover) Modifier.fillMaxWidth().padding(horizontal = 12.dp) else Modifier,
                )
            }

            if (!cover && popover && sheets.top == null) {
                InnerPopover(projectKey, canvas, doc, readOnly, gate.open, scene, sizes, vp, ui.selection, deps, vm, sheets, readOnlyToast, clearSelection)
            }
        }
    }
}

/** The inner popover of the selected line (`선 라벨`) or node (node edit). */
@Composable
private fun InnerPopover(
    projectKey: Key,
    canvas: Canvas,
    doc: ProjectDoc,
    readOnly: Boolean,
    hiddenOpen: Boolean,
    scene: GraphScene,
    sizes: Map<String, Sz>,
    viewport: GraphViewport,
    selection: GraphSelection,
    deps: FeatureDeps,
    vm: CanvasBoardViewModel,
    sheets: SheetHost,
    readOnlyToast: () -> Unit,
    close: () -> Unit,
) {
    selection.lineKey?.let { lineKey ->
        val line = scene.lines.firstOrNull { it.key == lineKey } ?: return
        val a = scene.nodesByKey[line.from] ?: return
        val b = scene.nodesByKey[line.to] ?: return
        val visible = GraphGeometry.visibleSegment(GraphGeometry.rectOf(a, sizes), GraphGeometry.rectOf(b, sizes))
        val anchor = viewport.toScreen(GraphGeometry.middleAnchor(visible)) + Pt(0f, 16f)
        val state = rememberLineLabelState(projectKey, canvas.key, lineKey) ?: return
        CanvasPopover(anchor, onDismiss = { state.discard(); close() }) {
            LineLabelPopoverBody(
                state = state,
                onNewKind = { deps.navigator.push(RelationKindSettings(projectKey)) },
                onPickStart = { if (!state.view.readOnly) sheets.open(LineStartSheet(projectKey, canvas.key, lineKey)) },
                onDone = close,
            )
        }
        return
    }
    val nodeKey = selection.nodeKey ?: return
    val node = scene.nodesByKey[nodeKey] ?: return
    val view: NodeEditView = remember(doc, nodeKey, hiddenOpen, readOnly) { NodeEditModel.build(doc, canvas.key, nodeKey, hiddenOpen, readOnly) } ?: return
    val rect = GraphGeometry.rectOf(node, sizes)
    val anchor = viewport.toScreen(Pt(rect.center.x, rect.bottom)) + Pt(0f, 8f)
    CanvasPopover(anchor, onDismiss = close) {
        CivilightText(view.title, CivilightTheme.type.itemTitle, color = CivilightTheme.colors.ink, maxLines = 1)
        view.kindWord?.let { CivilightText(it, CivilightTheme.type.small, color = CivilightTheme.colors.ink3) }
        NodeEditRows(
            view = view,
            compact = true,
            onText = { vm.setText(canvas.key, nodeKey, it) },
            onOpen = { close(); view.openRoute?.let(deps.navigator::push) },
            onRemove = { removeNode(vm, canvas.key, view, readOnlyToast); close() },
        )
    }
}

private fun removeNode(vm: CanvasBoardViewModel, canvasKey: Key, view: NodeEditView, readOnlyToast: () -> Unit) {
    vm.removeNode(canvasKey, view.nodeKey) { r -> if (r == EditResult.READ_ONLY) readOnlyToast() }
}

/** Top-left that centres a card (or a 묶음 frame of [size]) on a tapped point. */
private fun centred(at: Pt, size: Sz = Sz.DEFAULT): Pt = Pt(at.x - size.w / 2f, at.y - size.h / 2f)

/** Drops on-screen positions the document now holds. */
private fun dropSettled(canvas: Canvas, moved: SnapshotStateMap<Key, Pt>) {
    for ((key, at) in moved.toMap()) {
        val node = canvas.nodes.firstOrNull { it.key == key }
        if (node == null || (node.x == at.x && node.y == at.y)) moved.remove(key)
    }
}
