package io.github.lenticualrstone.civilight.feature.map.track

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.TabContent
import io.github.lenticualrstone.civilight.feature.map.common.MapMotion
import io.github.lenticualrstone.civilight.feature.map.common.FoldSwitch
import io.github.lenticualrstone.civilight.feature.map.canvas.MapMarkScope
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.component.SidePanelHost
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.key
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.designsystem.component.PersonChip
import io.github.lenticualrstone.civilight.designsystem.component.RouteStop
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SidePanel
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.track.CharacterTrack as DomainTrack
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.canvas.mapAt
import io.github.lenticualrstone.civilight.feature.map.canvas.midpoint
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.reveal.Scrubber
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.CharacterTrack as CharacterTrackRoute
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.delay

/** 인물 동선 reads the project only; the lens moves through the project's lens controller. */
class TrackViewModel(projectKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env)

/**
 * 인물 동선 (WLD-62, board `MapTrack`). Inner: the map with the stops (`1-2` chips), the lines
 * (route-following, else straight `추정`) and the position chip at the lens; the side panel `동선 ·
 * 장소 · 레이어` with the character chips, `리엔 · 씬 순서` rows and the note; the episode scrubber
 * at the bottom with play. Cover (not drawn, Q-21): the same parts in a bottom card.
 */
@Composable
fun CharacterTrackScreen(route: CharacterTrackRoute, deps: FeatureDeps) {
    val vm = entryViewModel { TrackViewModel(route.projectKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val sheets = rememberSheetHost()
    val inner = LocalWindowLayout.current.isInner
    val lens = deps.lens(currentProjectStore)
    var selected by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val mapKey = route.mapKey
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var layerFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var playing by rememberSaveable { mutableStateOf(false) }
    var viewportArray by rememberSaveable { mutableStateOf(Viewport.UNSET.toArray()) }
    val d = doc
    LaunchedEffect(d != null) {
        if (selected == null && d != null) selected = TrackState.initial(d, hiddenOpen, route.characterKey)
    }
    val ui = remember(d, hiddenOpen, selected, mapKey, layerFilter) {
        d?.let { TrackState.build(it, mapKey, selected.orEmpty(), MapScreenViewModel.lensOf(it), hiddenOpen, layerFilter) }
    }
    val stopsNow by rememberUpdatedState(ui?.scrubber.orEmpty())
    val motion = CivilightTheme.motion
    LaunchedEffect(playing) {
        while (playing) {
            // The pause between two stops is a named step (no token yet, see Requests), scaled by the animator scale.
            delay(motion.delayMs(MapMotion.PLAY_STEP_MS))
            val next = TrackState.nextStop(stopsNow)
            if (next == null) playing = false else lens.set(LensPoint.UnitEnd(next))
        }
    }

    CivilightScreen(
        title = MapCopy.TRACK_TITLE,
        smallLine = ui?.small,
        back = mapBack(deps),
        lens = d?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        sheets = sheets,
    ) { padding ->
        val u = ui ?: return@CivilightScreen
        val colors = CivilightTheme.colors
        // The focused leg's stroke eases in and out when the focus moves (per line).
        val focus = u.lines.mapIndexed { i, l -> key(i) { animatedFraction(l.focused, label = "trackFocus") } }
        Box(Modifier.fillMaxSize()) {
            MapCanvas(
                base = u.base,
                scene = u.scene,
                viewport = Viewport.fromArray(viewportArray),
                onViewportChange = { viewportArray = it.toArray() },
                showRouteChips = false,
                extraDraw = { scope ->
                    val dash = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))
                    for ((n, l) in u.lines.withIndex()) {
                        val sel = focus.getOrNull(n)?.value ?: 0f
                        val path = Path()
                        l.points.forEachIndexed { i, q -> val s0 = scope.screen(q); if (i == 0) path.moveTo(s0.x, s0.y) else path.lineTo(s0.x, s0.y) }
                        drawPath(
                            path, lerp(colors.ink2, colors.acc, sel),
                            style = Stroke((1.5 + sel).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = if (l.dashed) dash else null),
                        )
                    }
                },
                marks = { scope ->
                    u.lines.filter { it.label != null && it.focused }.forEach { l ->
                        midpoint(l.points.map { scope.screen(it) })?.let { m -> MapValueChip(l.label!!, Modifier.mapAt(m.x, m.y, ay = 1.2f)) }
                    }
                    u.stops.forEach { s -> val p = scope.screen(s.at); RouteStop(s.label, s.focused, Modifier.mapAt(p.x, p.y)) }
                    u.positions.forEach { pos -> key(pos.name) { WalkingChip(pos, u.lines, scope) } }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            val scrubber: @Composable ColumnScope.() -> Unit = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CivilightIconButton(if (playing) IconId.Pause else IconId.Next, if (playing) MapCopy.TRACK_PAUSE else MapCopy.TRACK_PLAY, { playing = !playing })
                    Column(Modifier.weight(1f)) { Scrubber(u.scrubber) { key -> playing = false; lens.set(LensPoint.UnitEnd(key)) } }
                }
            }
            val panel: @Composable ColumnScope.() -> Unit = {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    u.chips.forEach { c -> FilterChip(c.name, c.selected, { selected = TrackState.tapChip(selected.orEmpty(), c.key) }) }
                }
                SegmentedControl(listOf(MapCopy.TAB_TRACK, MapCopy.TAB_TRACK_PLACES, MapCopy.TAB_LAYERS), tab, { tab = it })
                TabContent(tab) { t ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (t) {
                    1 -> u.places.forEach { CivilightText(it, CivilightTheme.type.label, maxLines = 1) }
                    2 -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        u.layerChips.forEach { chip -> FilterChip(chip.label, chip.selected, { layerFilter = chip.layerKey }) }
                    }
                    else -> {
                        if (u.listTitle == null) CivilightText(MapCopy.TRACK_EMPTY, CivilightTheme.type.small, color = colors.ink3)
                        u.listTitle?.let { CivilightText(it, CivilightTheme.type.sec, color = colors.ink3) }
                        u.rows.forEach { r ->
                            ListRow(
                                "${r.episode} | ${r.place}", subtitle = r.detail, compact = true, titleDotted = !r.onMap,
                                onClick = { lens.set(LensPoint.UnitEnd(r.sceneKey)) },
                            )
                        }
                        CivilightText(DomainTrack.NOTE, CivilightTheme.type.small, color = colors.ink3)
                    }
                }
                    }
                }
            }
            SidePanelHost(
                inner,
                Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + 8.dp, end = 12.dp, bottom = 12.dp),
            ) {
                SidePanel(onClose = { deps.navigator.pop() }) {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp), content = panel)
                }
            }
            FoldSwitch(inner) { isInner ->
                if (isInner) {
                    GlassCard(
                        Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = padding.calculateBottomPadding() + 16.dp)
                            .width(LocalWindowLayout.current.widthDp.dp - CivilightTheme.sizes.sidePanel - 56.dp),
                        content = scrubber,
                    )
                } else {
                    GlassCard(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 12.dp).heightIn(max = 420.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            panel()
                            scrubber()
                        }
                    }
                }
            }
        }
    }
}

/** The position chip of a character: when the lens moves it walks along the leg to the new stop on `enter` (it jumps when motion is reduced). */
@Composable
private fun WalkingChip(pos: TrackPosition, lines: List<TrackLine>, scope: MapMarkScope) {
    val motion = CivilightTheme.motion
    val progress = remember { Animatable(1f) }
    var path by remember { mutableStateOf(listOf(pos.at)) }
    var target by remember { mutableStateOf(pos.at) }
    LaunchedEffect(pos.at) {
        if (pos.at != target) {
            // An interrupted walk goes on from where the chip is.
            val here = TrackPath.pointAt(path, progress.value)
            path = TrackPath.between(lines, here, pos.at)
            target = pos.at
            progress.snapTo(0f)
            progress.animateTo(1f, motion.move(CivilightMotion.enter))
        }
    }
    val p = scope.screen(TrackPath.pointAt(path, progress.value))
    PersonChip(pos.name, Modifier.mapAt(p.x, p.y, ay = 1.6f))
}
