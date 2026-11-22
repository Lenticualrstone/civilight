package io.github.lenticualrstone.civilight.feature.cast.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.map.MapPositions
import io.github.lenticualrstone.civilight.domain.cast.map.SelectedRelationCard
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.RelationEditSheet
import io.github.lenticualrstone.civilight.navigation.RelationMap
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.graph.GraphGeometry
import io.github.lenticualrstone.civilight.ui.graph.GraphHit
import io.github.lenticualrstone.civilight.ui.graph.GraphSelection
import io.github.lenticualrstone.civilight.ui.graph.GraphSurface
import io.github.lenticualrstone.civilight.ui.graph.GraphSurfaceListener
import io.github.lenticualrstone.civilight.ui.graph.GraphViewport
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Sz
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.serialization.json.Json

/** The map's view model: node positions are project data (one undo step per drag). */
class RelationMapViewModel(projectKey: Key, sessions: ProjectSessions, contextOf: () -> EditContext) : CastViewModel(projectKey, sessions, contextOf)

private fun encode(ui: MapUi): String = Json.encodeToString(MapUi.serializer(), ui)

private fun decode(s: String): MapUi = runCatching { Json.decodeFromString(MapUi.serializer(), s) }.getOrNull() ?: MapUi()

/**
 * 전체 관계도 (CST-33; `CastMapC` cover, `CastMap` inner): the `목록 | 관계도` view of the cast (cover),
 * the graph on the shared engine (cards, solid `사이` / dashed mind lines with their labels, never
 * coloured by kind), tag chips bottom left, the filter (`사이 · 마음 · 둘 다`, kinds), and the
 * `고른 관계` card (inner: glass aside 260 bottom right; cover: bottom sheet). A node opens its record;
 * the `선` tool draws a line between two characters and opens the line-label sheet (CST-34). One
 * finger drags a node (saved per project; kept on screen only in a read-only project).
 */
@Composable
fun RelationMapScreen(route: RelationMap, deps: FeatureDeps) {
    val vm = entryViewModel { RelationMapViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val doc = ready.doc
    val p = route.projectKey
    val gate = rememberHiddenGate(p)
    val inner = LocalWindowLayout.current.isInner
    var uiText by rememberSaveable { mutableStateOf(encode(MapUi(selectedNode = route.focusKey))) }
    val ui = decode(uiText)
    fun setUi(next: MapUi) {
        uiText = encode(next)
    }
    val moved = remember { mutableStateMapOf<Key, Pt>() }
    val state = remember(doc, ui, gate.open, inner, moved.toMap()) { RelationMapModel.build(doc, ui, gate.open, inner, moved.toMap()) }
    val sizes = remember { mutableStateMapOf<String, Sz>() }
    var viewport by remember { mutableStateOf<GraphViewport?>(null) }
    var latestFit by remember { mutableStateOf(GraphViewport()) }
    val sheets = rememberSheetHost()
    val editable = !ready.readOnly

    fun openRelationEdit(relationKey: Key) {
        val (viewer, partner) = RelationMapModel.viewerOf(doc, relationKey, route.focusKey) ?: return
        sheets.open(RelationEditSheet(p, endAKey = viewer, endBKey = partner))
    }
    fun openRecord(relationKey: Key) {
        val (viewer, _) = RelationMapModel.viewerOf(doc, relationKey, route.focusKey) ?: return
        deps.navigator.push(CharacterProfile(p, viewer))
    }

    val listener = object : GraphSurfaceListener {
        override fun onTap(hit: GraphHit, at: Pt) {
            when (hit) {
                is GraphHit.Node -> {
                    val (next, tap) = RelationMapModel.tapNode(ui, hit.key)
                    setUi(next)
                    when (tap) {
                        is NodeTap.OpenRecord -> deps.navigator.push(CharacterProfile(p, tap.characterKey))
                        is NodeTap.DrawLine -> if (editable) sheets.open(MapLineSheet(p, tap.fromKey, tap.toKey))
                        NodeTap.LineStarted -> Unit
                    }
                }
                is GraphHit.Line -> {
                    val next = RelationMapModel.tapLine(ui, hit.key, inner)
                    setUi(next)
                    if (!inner && next.selectedRelation != null) sheets.open(SelectedRelationSheet(p, hit.key))
                }
                else -> setUi(RelationMapModel.tapEmpty(ui))
            }
        }

        override fun canDrag(nodeKey: String): Boolean = true

        override fun onDrag(nodeKey: String, delta: Pt) {
            // Latch the fitted view so it does not refit under the finger while a node moves.
            if (viewport == null) viewport = latestFit
            val base = moved[nodeKey] ?: state.scene.nodesByKey[nodeKey]?.topLeft ?: return
            moved[nodeKey] = base + delta
        }

        override fun onDragEnd(nodeKey: String) {
            val at = moved[nodeKey] ?: return
            if (!editable) return
            val all = RelationMapModel.positionsToSave(state.scene, moved.toMap())
            vm.act(CastUiCopy.UNDO_MOVE, then = { moved.remove(nodeKey) }) { d, ctx ->
                all.entries.fold(d) { acc, (key, point) -> MapPositions.place(acc, key, point, ctx) }
            }
        }

        override fun onViewport(viewport: GraphViewport) {
            viewportState(viewport)
        }

        override fun onViewportSettled(viewport: GraphViewport) {
            viewportState(viewport)
        }

        private fun viewportState(next: GraphViewport) {
            viewport = next
        }
    }

    CivilightScreen(
        title = state.title,
        smallLine = state.smallLine,
        back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
        lens = lensChipLabel(doc),
        onLens = { sheets.open(LensSheet(p)) },
        actions = {
            if (editable) {
                CivilightIconButton(
                    IconId.Line, CastUiCopy.LINE_TOOL, { setUi(RelationMapModel.toggleLineTool(ui)) },
                    tint = if (ui.lineTool) CivilightTheme.colors.acc else CivilightTheme.colors.ink2,
                )
            }
            CivilightIconButton(
                IconId.Filter, CastUiCopy.PLACE_FILTER, { sheets.open(MapFilterSheet(p)) },
                tint = if (state.filtered) CivilightTheme.colors.acc else CivilightTheme.colors.ink2,
            )
        },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is MapFilterSheet -> { { MapFilterContent(ui, state) { setUi(it) } } }
                is MapLineSheet -> { { MapLineContent(key) } }
                is SelectedRelationSheet -> {
                    {
                        val card = state.selected
                        if (card == null) {
                            LaunchedEffect(Unit) { close() }
                        } else {
                            val ends = RelationMapModel.viewerOf(doc, key.relationKey, route.focusKey)
                            SheetHeader(card.title, onClose = { close(); setUi(ui.copy(selectedRelation = null)) }, closeLabel = card.closeLabel)
                            SelectedRelationBody(
                                card,
                                onRecord = { close(); openRecord(key.relationKey) },
                                onEdit = if (editable && ends != null) ({ open(RelationEditSheet(p, endAKey = ends.first, endBKey = ends.second)) }) else null,
                                cover = true,
                            )
                        }
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val top = padding.calculateTopPadding().value
            val bottom = padding.calculateBottomPadding().value
            val viewW = maxWidth.value
            val viewH = maxHeight.value
            val fitted = viewport ?: GraphViewport.fit(GraphGeometry.bounds(state.scene, sizes), viewW, viewH, top + if (inner) 0f else 56f, bottom + 56f)
            latestFit = fitted
            GraphSurface(
                scene = state.scene,
                viewport = fitted,
                selection = GraphSelection(nodeKey = ui.selectedNode, lineKey = ui.selectedRelation),
                sizes = sizes,
                listener = listener,
                cover = !inner,
            )
            if (!inner) {
                // `목록 | 관계도` under the head (cover); `목록` returns to the list.
                SegmentedControl(
                    CastUiCopy.VIEW_MODES, 1, { if (it == 0) deps.navigator.push(CastList(p)) },
                    Modifier.padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding()),
                )
            }
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (chip in state.tags) FilterChip(chip.label, chip.selected, { setUi(ui.copy(tagKey = chip.tagKey)) })
            }
            val card = state.selected
            if (inner && card != null && ui.selectedRelation != null) {
                val relationKey = ui.selectedRelation
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp)
                        .width(260.dp),
                ) {
                    GlassAside {
                        SelectedRelationBody(
                            card,
                            onRecord = { openRecord(relationKey) },
                            onEdit = if (editable) ({ openRelationEdit(relationKey) }) else null,
                            cover = false,
                        )
                    }
                }
            }
        }
    }
}

/** The inner `고른 관계` aside: glass, radius 20, padding 16 × 18, `line` outline. */
@Composable
private fun GlassAside(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.insetOutline(CivilightTheme.colors.line, 1.dp, shape)) {
        // The aside sits inside the screen's glass source, so it must not sample it (SCRIM fill instead).
        GlassBackground(GlassSurface.PANEL, Modifier.matchParentSize(), shape = shape, source = null)
        Box(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) { content() }
    }
}

/**
 * `고른 관계`: (inner) `고른 관계` and the pair, then per-point steps (mono point · text; steps after
 * the lens in `ink3`) and `인물 기록` (ghost) · `관계 고치기`. The cover sheet puts the pair in its header.
 */
@Composable
fun SelectedRelationBody(card: SelectedRelationCard, onRecord: () -> Unit, onEdit: (() -> Unit)?, cover: Boolean) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth()) {
        if (!cover) {
            SectionLabel(CastUiCopy.SELECTED_RELATION)
            CivilightText(card.title, type.itemTitle, Modifier.padding(top = 8.dp), color = colors.ink, maxLines = 1)
        }
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for ((point, text) in card.steps) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CivilightText(point, type.number.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3, maxLines = 1)
                    CivilightText(text, type.small.merge(TextStyle(fontSize = 13.sp, lineHeight = 19.sp)), color = if (RelationMapModel.isAfterLens(text)) colors.ink3 else colors.ink)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightButton(card.recordLabel, onRecord, if (cover) Modifier.weight(1f) else Modifier, variant = if (cover) ButtonVariant.GHOST else ButtonVariant.COMPACT_GHOST)
            onEdit?.let { CivilightButton(card.editLabel, it, if (cover) Modifier.weight(1f) else Modifier, variant = if (cover) ButtonVariant.PRIMARY else ButtonVariant.COMPACT_PRIMARY) }
        }
    }
}
