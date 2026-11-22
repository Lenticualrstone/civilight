package io.github.lenticualrstone.civilight.feature.canvas.board

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.feature.canvas.edit.LineResult
import io.github.lenticualrstone.civilight.ui.graph.GraphGeometry
import io.github.lenticualrstone.civilight.ui.graph.NodeLook
import io.github.lenticualrstone.civilight.ui.graph.Rect
import io.github.lenticualrstone.civilight.ui.graph.GraphHit
import io.github.lenticualrstone.civilight.ui.graph.GraphScene
import io.github.lenticualrstone.civilight.ui.graph.GraphSelection
import io.github.lenticualrstone.civilight.ui.graph.GraphSurface
import io.github.lenticualrstone.civilight.ui.graph.GraphSurfaceListener
import io.github.lenticualrstone.civilight.ui.graph.GraphViewport
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Sz
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
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.delay

private val BoardUiSaver = listSaver<BoardUi, String>(
    save = { ui -> ui.save().map { it.orEmpty() } },
    restore = { saved -> BoardUi.restore(saved.map { it.ifEmpty { null } }) },
)

/** How long the canvas waits for its nodes to be measured before it shows anyway (a wait, not a motion). */
private const val MEASURE_WAIT_MS = 600L

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
    val canvas = ready?.let { CanvasEdits.resolveForRoute(it.doc, route.canvasKey) }
    val state = when {
        ready == null -> BoardState.LOADING
        canvas == null -> BoardState.NO_CANVAS
        else -> BoardState.BOARD
    }
    // Loading, no canvas and the board crossfade (audit §4 canvas 10); an exiting state keeps its last data.
    val shownReady = rememberLastNonNull(ready)
    val shownCanvas = rememberLastNonNull(canvas)
    ContentSwap(state, Modifier.fillMaxSize(), label = "canvasBoardState") { s ->
        when (s) {
            BoardState.LOADING -> CivilightScreen(title = "", smallLine = CanvasCopy.SMALL_LINE, back = back) { }
            BoardState.NO_CANVAS -> NoCanvas(shownReady?.readOnly ?: true, back, vm)
            BoardState.BOARD -> if (shownReady != null && shownCanvas != null) {
                CanvasBoard(route, deps, vm, shownReady.doc, shownReady.readOnly, shownCanvas, sheets, back)
            }
        }
    }
}

private enum class BoardState { LOADING, NO_CANVAS, BOARD }

/** A project without a canvas: one is made (CNV-02 [PROPOSED]); a read-only project shows an empty state. */
@Composable
private fun NoCanvas(readOnly: Boolean, back: IconAction, vm: CanvasBoardViewModel) {
    if (!readOnly) LaunchedEffect(Unit) { vm.createFirstCanvas() }
    CivilightScreen(title = "", smallLine = CanvasCopy.SMALL_LINE, back = back) { padding ->
        if (readOnly) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                EmptyBlock(title = CanvasCopy.NO_CANVAS)
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
    /** Once the nodes are measured the view is fitted (or restored) and latched: it never refits by itself again. */
    var revealed by remember(canvas.key) { mutableStateOf(false) }
    val reveal = remember(canvas.key) { Animatable(0f) }
    /** The last zoom change came from `+` / `−` (the percentage then swaps like a lens value; pinching shows it as is). */
    var zoomByButton by remember(canvas.key) { mutableStateOf(false) }
    val motion = CivilightTheme.motion
    LaunchedEffect(revealed) { if (revealed) reveal.animateTo(1f, motion.spec(CivilightMotion.enter)) }
    val relationLines = remember(doc, canvas, lens, gate.open) { BoardScene.relationLines(doc, canvas, lens, gate.open) }
    val scene = BoardScene.build(doc, canvas, lens, gate.open, moved, relationLines)

    // A dropped node keeps its on-screen position until the document has it (no flicker back).
    LaunchedEffect(canvas) { dropSettled(canvas, moved) }
    LaunchedEffect(canvas.key) { vm.markOpened(canvas.key) }

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
                                onRestore = { restoreItem(vm, deps, view, gate, readOnlyToast) { close() } },
                                onKeep = { keepItem(vm, view, readOnlyToast); close() },
                                onUnlock = gate.unlock,
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
                                vm.addItem(key.canvasKey, kind, ref, placeCard(Pt(key.x, key.y), scene, sizes)) { r, _ -> if (r == EditResult.READ_ONLY) readOnlyToast() }
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
            // The middle of what the author sees, between the top bar and the tool bar (screen dp).
            val center = Pt(maxWidth.value / 2f, topInset + (maxHeight.value - topInset - TOOLBAR_SPACE) / 2f)
            // New nodes land here (content dp), then step aside from any node already there (QA #10).
            val viewCentre = vp.toContent(center)
            val measured = scene.nodes.all { it.look == NodeLook.FRAME || sizes.containsKey(it.key) }
            LaunchedEffect(measured) {
                if (!measured || revealed) return@LaunchedEffect
                if (viewport == null) viewport = vp
                revealed = true
            }
            // Safety net: a node that never reports its size must not leave the canvas invisible.
            val latestVp by rememberUpdatedState(vp)
            LaunchedEffect(canvas.key) {
                delay(MEASURE_WAIT_MS)
                if (!revealed) {
                    if (viewport == null) viewport = latestVp
                    revealed = true
                }
            }
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
                        is BoardAction.PickItem -> sheets.open(PlaceItemSheet(projectKey, canvas.key, action.tool, viewCentre.x, viewCentre.y))
                        is BoardAction.AddMemo -> vm.addMemo(canvas.key, placeCard(viewCentre, scene, sizes)) { r, key ->
                            if (r == EditResult.READ_ONLY) readOnlyToast()
                            key?.let {
                                ui = ui.copy(selection = GraphSelection(nodeKey = it))
                                openNode(it)
                            }
                        }
                        is BoardAction.AddGroup -> vm.addGroup(canvas.key, placeGroup(viewCentre, scene, sizes)) { r, _ ->
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
                    zoomByButton = false
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
            // Held at opacity 0 until the node sizes are known, then it fades in (audit §4 canvas 1); the surface is
            // re-created at that moment so the fitted view is its first view and nothing glides into place.
            key(revealed) {
                GraphSurface(
                    scene = scene,
                    viewport = vp,
                    selection = ui.selection,
                    sizes = sizes,
                    listener = listener,
                    cover = cover,
                    modifier = Modifier.graphicsLayer { alpha = reveal.value },
                )
            }

            ZoomColumn(
                percent = CanvasCopy.zoomPercent(vp.zoom),
                swapPercent = zoomByButton,
                canIn = vp.canZoomIn,
                canOut = vp.canZoomOut,
                onIn = { zoomByButton = true; vp.zoomIn(center).also { viewport = it; vm.saveViewport(canvas.key, it) } },
                onOut = { zoomByButton = true; vp.zoomOut(center).also { viewport = it; vm.saveViewport(canvas.key, it) } },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = (topInset + 16f).dp, end = 12.dp),
            )
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FoldReveal(cover) { CanvasHint(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 18.dp)) }
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
                InnerPopover(projectKey, canvas, doc, readOnly, gate, scene, sizes, vp, ui.selection, deps, vm, sheets, readOnlyToast, clearSelection)
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
    gate: HiddenGate,
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
    val view: NodeEditView = remember(doc, nodeKey, gate.open, readOnly) { NodeEditModel.build(doc, canvas.key, nodeKey, gate.open, readOnly) } ?: return
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
            onRestore = { restoreItem(vm, deps, view, gate, readOnlyToast, close) },
            onKeep = { keepItem(vm, view, readOnlyToast); close() },
            onUnlock = gate.unlock,
        )
    }
}

/** `되살리기` of the item behind a deleted node: a hidden item asks for `지문으로 보기` first; the toast can undo it. */
private fun restoreItem(vm: CanvasBoardViewModel, deps: FeatureDeps, view: NodeEditView, gate: HiddenGate, readOnlyToast: () -> Unit, done: () -> Unit) {
    val item = view.item ?: return
    vm.restoreItem(item, gate.open) { r, result ->
        if (r == EditResult.READ_ONLY) readOnlyToast()
        if (result.needsAuth) {
            gate.unlock()
        } else {
            done()
            result.toast?.let { t -> deps.toasts.show(t.message, actionLabel = t.action) { vm.undoLast() } }
        }
    }
}

private fun keepItem(vm: CanvasBoardViewModel, view: NodeEditView, readOnlyToast: () -> Unit) {
    val item = view.item ?: return
    vm.keepItem(item) { r -> if (r == EditResult.READ_ONLY) readOnlyToast() }
}

private fun removeNode(vm: CanvasBoardViewModel, canvasKey: Key, view: NodeEditView, readOnlyToast: () -> Unit) {
    vm.removeNode(canvasKey, view.nodeKey) { r -> if (r == EditResult.READ_ONLY) readOnlyToast() }
}

/** Top-left of a new card or memo centred on [centre] (content dp), moved off the cards already there. */
private fun placeCard(centre: Pt, scene: GraphScene, sizes: Map<String, Sz>): Pt =
    NodePlacement.place(centre, NodePlacement.CARD, occupiedBy(scene, sizes, frames = false))

/** Top-left of a new 묶음 frame centred on [centre], moved off the frames already there. */
private fun placeGroup(centre: Pt, scene: GraphScene, sizes: Map<String, Sz>): Pt =
    NodePlacement.place(centre, CanvasEdits.GROUP_SIZE, occupiedBy(scene, sizes, frames = true))

private fun occupiedBy(scene: GraphScene, sizes: Map<String, Sz>, frames: Boolean): List<Rect> =
    scene.nodes.filter { (it.look == NodeLook.FRAME) == frames }.map { GraphGeometry.rectOf(it, sizes) }

/** Drops on-screen positions the document now holds. */
private fun dropSettled(canvas: Canvas, moved: SnapshotStateMap<Key, Pt>) {
    for ((key, at) in moved.toMap()) {
        val node = canvas.nodes.firstOrNull { it.key == key }
        if (node == null || (node.x == at.x && node.y == at.y)) moved.remove(key)
    }
}
