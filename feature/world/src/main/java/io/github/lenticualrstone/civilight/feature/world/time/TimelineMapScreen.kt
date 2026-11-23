package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.key
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.ui.window.SplitPanes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.MapPin
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldCommandBar
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.TimelineMap
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.ui.window.WidthClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** `연표 + 지도` (WLD-64): the selection, map and comparison come from the screen's saved state. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineMapViewModel(projectKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    private data class Input(val selected: Key?, val map: Key?, val mode: CompareMode)

    private val input = MutableStateFlow(Input(null, null, CompareMode.NOW))

    val ui: StateFlow<TimelineMapUi?> = combine(doc.filterNotNull(), hiddenOpen, input) { d, h, i -> Triple(d, h, i) }
        .mapLatest { (d, h, i) -> TimelineMapState.build(d, i.selected, i.map, i.mode, h) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun set(selected: Key?, map: Key?, mode: CompareMode) {
        input.value = Input(selected, map, mode)
    }
}

/**
 * `연표 + 지도` (board `TimelineMap`, inner screen): header small `세계 · 연표 + 지도`, title `연표`,
 * lens chip; left the event list with the hint `사건마다 장소 · 고르면 지도에서 강조`, right the map
 * with the selected event's pin or region in `acc`, map chips when there are several maps, and for a
 * region with `◆` history the chips `1-7 · 1-9 ◆ · 비교` with its caption. The cover version is not
 * drawn (Q-21): there the map sits above the list. Selecting a row highlights; its `›` opens the event.
 *
 * The map here is a schematic of the domain data (region outlines, pins) on a plain ground, not the
 * map lane's rendered base (see Requests).
 */
@Composable
fun TimelineMapScreen(route: TimelineMap, deps: FeatureDeps) {
    val vm = entryViewModel { TimelineMapViewModel(route.projectKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val sheets = rememberSheetHost()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var mapKey by rememberSaveable { mutableStateOf<String?>(null) }
    var modeName by rememberSaveable { mutableStateOf(CompareMode.NOW.name) }
    LaunchedEffect(selected, mapKey, modeName) { vm.set(selected, mapKey, CompareMode.valueOf(modeName)) }
    val wide = LocalWindowLayout.current.widthClass != WidthClass.COMPACT
    val motion = CivilightTheme.motion
    val state = ui

    CivilightScreen(
        title = WorldCopy.TIMELINE_TITLE,
        smallLine = WorldCopy.SMALL_TIMELINE_MAP,
        back = worldBack(route, deps),
        commandBar = { WorldCommandBar(deps, route.projectKey, WorldCopy.SEARCH_TIMELINE) },
        sheets = sheets,
    ) { padding ->
        if (state == null) return@CivilightScreen
        val onSelect: (TimelineMapRowUi) -> Unit = { row ->
            if (!row.locked) {
                selected = if (selected == row.eventKey) null else row.eventKey
                mapKey = null
            }
        }
        val onOpen: (TimelineMapRowUi) -> Unit = { row -> deps.navigator.push(EventDetail(route.projectKey, row.eventKey)) }
        val mapPanel: @Composable (Modifier) -> Unit = { modifier ->
            MapPanel(
                ui = state,
                modifier = modifier,
                onMap = { key -> mapKey = key; selected = null },
                onMode = { modeName = it.name },
            )
        }
        // A fold or unfold swaps the two layouts on the fold tokens (CNV-34): fade in on `foldOpen` / `foldClose`.
        AnimatedContent(
            targetState = wide,
            transitionSpec = {
                val t = if (targetState) CivilightMotion.foldOpen else CivilightMotion.foldClose
                ContentTransform(fadeIn(motion.spec(t)), fadeOut(motion.spec(CivilightMotion.exit)), sizeTransform = null)
            },
            label = "timelineMapLayout",
        ) { isWide ->
            if (isWide) {
                // The event list | map frame is the shared CNV-47 split (390 dp list by default; the 연표 ratio).
                SplitPanes(
                    module = PaneSplit.Modules.TIMELINE,
                    left = { EventList(state, Modifier.fillMaxSize(), padding, onSelect, onOpen) },
                    right = {
                        mapPanel(
                            Modifier.fillMaxSize()
                                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding(), end = 16.dp),
                        )
                    },
                )
            } else {
                EventList(state, Modifier.fillMaxSize(), padding, onSelect, onOpen, header = { mapPanel(Modifier.fillMaxWidth()) })
            }
        }
    }
}

@Composable
private fun EventList(
    ui: TimelineMapUi,
    modifier: Modifier,
    padding: PaddingValues,
    onSelect: (TimelineMapRowUi) -> Unit,
    onOpen: (TimelineMapRowUi) -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 12.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        if (header != null) item(key = "map") { header() }
        item(key = "hint") { CivilightText(ui.hint, type.small, Modifier.padding(top = 8.dp, bottom = 6.dp), color = colors.ink3) }
        items(ui.rows, key = { it.eventKey }) { row ->
            EventRow(row, civilightItem(), onSelect, onOpen)
        }
    }
}

/** One event of the list; the selection fades its highlight in and the title to `acc` (`standard`). */
@Composable
private fun EventRow(row: TimelineMapRowUi, modifier: Modifier, onSelect: (TimelineMapRowUi) -> Unit, onOpen: (TimelineMapRowUi) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val hl by animatedFraction(row.selected)
    val titleColor by animatedColor(if (row.selected) colors.acc else colors.ink)
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .drawBehind { drawRect(colors.hl.copy(alpha = colors.hl.alpha * hl)) }
            .pressable(PressAction.Select(row.selected) { onSelect(row) }, role = Role.Button, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CivilightText(row.time, type.number, Modifier.width(72.dp), color = colors.ink3)
        Column(Modifier.weight(1f)) {
            CivilightText(row.title, type.itemTitle, color = titleColor)
            if (row.sub.isNotEmpty()) CivilightText(row.sub, type.small, color = colors.ink3)
            row.regionChange?.let { CivilightText(it, type.small, color = colors.acc) }
        }
        row.place?.let { CivilightText(it, type.small, Modifier.padding(start = 8.dp, top = 2.dp), color = colors.ink2) }
        if (!row.locked) {
            CivilightIconButton(icon = IconId.Next, contentDescription = row.title, onClick = { onOpen(row) })
        }
    }
}

@Composable
private fun MapPanel(ui: TimelineMapUi, modifier: Modifier, onMap: (Key) -> Unit, onMode: (CompareMode) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.padding(top = 8.dp)) {
        if (ui.maps.size > 1) {
            ChipFlow(Modifier.padding(bottom = 8.dp)) {
                ui.maps.forEach { (key, name) -> FilterChip(label = name, selected = key == ui.mapKey, onClick = { onMap(key) }) }
            }
        }
        if (ui.mapKey == null) {
            CivilightText(WorldCopy.NO_MAP, type.small, color = colors.ink3)
            return@Column
        }
        // Another map crossfades on `standard` (WLD-68, 200 ms); the same map only moves its highlight.
        ContentSwap(ui, Modifier.fillMaxWidth(), contentKey = { it.mapKey }, label = "timelineMap") { shown ->
            SchematicMap(shown, Modifier.fillMaxWidth())
        }
        FoldRevealValue(ui.compare) { compare ->
          Column {
            ChipFlow(Modifier.padding(top = 10.dp)) {
                compare.chips.forEachIndexed { i, label ->
                    val mode = CompareMode.entries[i]
                    FilterChip(label = label, selected = compare.mode == mode, onClick = { onMode(mode) })
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightText(compare.captionName, type.small, color = colors.ink)
                CivilightText(compare.captionLine, type.small, color = colors.acc)
            }
            FoldRevealValue(compare.dottedNote) { note -> CivilightText(note, type.small, Modifier.padding(top = 2.dp), color = colors.ink3) }
          }
        }
    }
}

/** Region outlines and pins on a plain ground in the map's aspect; the highlight is `acc`. */
@Composable
private fun SchematicMap(ui: TimelineMapUi, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    BoxWithConstraints(
        modifier
            .aspectRatio(ui.aspect.coerceIn(0.5f, 2.5f))
            .clip(shape)
            .background(colors.tint),
    ) {
        val w = maxWidth
        val h = maxHeight
        // Each region's highlight moves on `standard` (per-key fraction, read in the draw phase).
        val fractions = ui.regions.map { r -> key(r.key) { animatedFraction(r.highlighted, label = "region") } }
        Canvas(Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            ui.regions.forEachIndexed { index, region ->
                val f = fractions[index].value
                if (region.vertices.size < 3) return@forEachIndexed
                val path = Path().apply {
                    region.vertices.forEachIndexed { i, p ->
                        val o = Offset(p.x * size.width, p.y * size.height)
                        if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                    }
                    close()
                }
                val tone = lerp(colors.ink3, colors.acc, f)
                drawPath(path, tone.copy(alpha = lerp(0.08f, 0.22f, f)))
                drawPath(path, tone, style = Stroke(width = lerp(1.5f, 3f, f)))
                region.added?.takeIf { it.size >= 3 }?.let { added ->
                    val addedPath = Path().apply {
                        added.forEachIndexed { i, p ->
                            val o = Offset(p.x * size.width, p.y * size.height)
                            if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                        }
                        close()
                    }
                    drawPath(addedPath, colors.acc, style = Stroke(width = 2f, pathEffect = dash))
                }
            }
        }
        ui.pins.forEach { pin -> key(pin.key) {
            // The pin's tip sits on its point; the 24 dp icon is centred over x.
            Box(Modifier.offset(x = w * pin.x - 12.dp, y = h * pin.y - 24.dp)) {
                MapPin(pin.name, selected = pin.highlighted, dim = !pin.highlighted && ui.pins.any { it.highlighted })
            }
        } }
    }
}
