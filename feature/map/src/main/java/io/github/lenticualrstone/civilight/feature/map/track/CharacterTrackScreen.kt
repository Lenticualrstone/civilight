package io.github.lenticualrstone.civilight.feature.map.track

import io.github.lenticualrstone.civilight.feature.map.common.rememberMapAsideState
import io.github.lenticualrstone.civilight.feature.map.common.MapAsideRules
import io.github.lenticualrstone.civilight.feature.map.common.MapAside
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.component.TabGap
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.TabContent
import io.github.lenticualrstone.civilight.feature.map.common.MapMotion
import io.github.lenticualrstone.civilight.feature.map.common.FoldSwitch
import io.github.lenticualrstone.civilight.feature.map.canvas.MapMarkScope
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
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
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.designsystem.component.PersonChip
import io.github.lenticualrstone.civilight.designsystem.component.RouteStop
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
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.ui.tour.TourHold
import io.github.lenticualrstone.civilight.ui.tour.tourTarget
import kotlinx.coroutines.delay

/** 인물 동선 reads the project only. */
class TrackViewModel(projectKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env)

/**
 * 인물 동선 (WLD-62, board `MapTrack`). Inner: the map with the stops (`1-2` chips), the lines (route-following,
 * else straight `추정`, dashed for the characters not in focus) and the position chips; the floating aside (280,
 * `동선 · 장소 · 레이어` tabs and `패널 닫기`, 0.2.3 `PanelCollapse`) with the character chips, `리엔 · 씬 순서`
 * rows, the `▲ 이동 시간` card (only while 설정 `이동 점검` is on) and the note; the episode scrubber at the bottom
 * with a filled `재생`. Cover (not drawn, Q-21): the same parts in a bottom card.
 *
 * (0.3.0, WLD-71) The scrubber is this screen's own point, as on 시점별 공개: it starts at 최신 and never moves the
 * project lens (map screens read at 최신). `재생` from 최신 starts at the first episode.
 */
@Composable
fun CharacterTrackScreen(route: CharacterTrackRoute, deps: FeatureDeps) {
    val vm = entryViewModel { TrackViewModel(route.projectKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val settings by deps.settings.collectAsState()
    val sheets = rememberSheetHost()
    val inner = LocalWindowLayout.current.isInner
    var selected by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val mapKey = route.mapKey
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var layerFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var playing by rememberSaveable { mutableStateOf(false) }
    var pointUnit by rememberSaveable { mutableStateOf<String?>(null) }
    var viewportArray by rememberSaveable { mutableStateOf(Viewport.UNSET.toArray()) }
    val aside = rememberMapAsideState()
    val d = doc
    LaunchedEffect(d != null) {
        if (selected == null && d != null) selected = TrackState.initial(d, hiddenOpen, route.characterKey)
    }
    val checkTravel = settings.checkTravel
    val ui = remember(d, hiddenOpen, selected, mapKey, layerFilter, pointUnit) {
        d?.let {
            val point = pointUnit?.let { u -> LensPoint.UnitEnd(u) } ?: MapScreenViewModel.lensOf(it)
            TrackState.build(it, mapKey, selected.orEmpty(), point, hiddenOpen, layerFilter)
        }
    }
    // The travel card does not depend on the point: it is not recomputed on every `재생` step.
    val warning = remember(d, hiddenOpen, selected, checkTravel) {
        if (checkTravel && d != null) TrackState.warningOf(d, selected.orEmpty(), hiddenOpen) else null
    }
    val stopsNow by rememberUpdatedState(ui?.scrubber.orEmpty())
    // LIB-19 `장소와 때` tour: it waits until the map is drawn, so its holes exist (TrackTourSteps).
    if (TrackTourSteps.hold(ui)) TourHold()
    val motion = CivilightTheme.motion
    LaunchedEffect(playing) {
        while (playing) {
            // The pause between two stops is a named step (no token yet, see Requests), scaled by the animator scale.
            delay(motion.delayMs(MapMotion.PLAY_STEP_MS))
            val next = TrackState.nextStop(stopsNow)
            if (next == null) playing = false else pointUnit = next
        }
    }
    val togglePlay: () -> Unit = {
        if (!playing) TrackState.playStart(stopsNow)?.let { pointUnit = it }
        playing = !playing
    }

    CivilightScreen(
        title = MapCopy.TRACK_TITLE,
        smallLine = ui?.small,
        back = mapBack(deps),
        sheets = sheets,
    ) { padding ->
        val u = ui ?: return@CivilightScreen
        val colors = CivilightTheme.colors
        val type = CivilightTheme.type
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
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
                    for ((n, l) in u.lines.withIndex()) {
                        val sel = focus.getOrNull(n)?.value ?: 0f
                        val path = Path()
                        l.points.forEachIndexed { i, q -> val s0 = scope.screen(q); if (i == 0) path.moveTo(s0.x, s0.y) else path.lineTo(s0.x, s0.y) }
                        // Board `MapTrack`: the focused track 2.5 in `acc`, the others 2 dashed in `ink2`.
                        drawPath(
                            path, lerp(colors.ink2, colors.acc, sel),
                            style = Stroke((2f + 0.5f * sel).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = if (l.dashed) dash else null),
                        )
                    }
                },
                marks = { scope ->
                    u.lines.filter { it.label != null && it.focused }.forEach { l ->
                        midpoint(l.points.map { scope.screen(it) })?.let { m -> MapValueChip(l.label!!, Modifier.mapAt(m.x, m.y, ay = 1.2f)) }
                    }
                    u.stops.forEach { s -> val p = scope.screen(s.at); RouteStop(s.label, s.focused, Modifier.mapAt(p.x, p.y)) }
                    u.positions.forEach { pos -> key(pos.name) { WalkingChip(doc, pos, u.lines, scope) } }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            val scrubber: @Composable ColumnScope.() -> Unit = {
                Row(Modifier.tourTarget(TrackTourSteps.TRACK), verticalAlignment = Alignment.CenterVertically) {
                    PlayButton(playing, togglePlay)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) { Scrubber(u.scrubber) { key -> playing = false; pointUnit = key } }
                }
            }
            val chips: @Composable () -> Unit = {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    u.chips.forEach { c -> FilterChip(c.name, c.selected, { selected = TrackState.tapChip(selected.orEmpty(), c.key) }) }
                }
            }
            val body: @Composable ColumnScope.(Int) -> Unit = { t ->
                when (t) {
                    1 -> u.places.forEach { CivilightText(it, type.label, maxLines = 1) }
                    2 -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        u.layerChips.forEach { chip -> FilterChip(chip.label, chip.selected, { layerFilter = chip.layerKey }) }
                    }
                    else -> {
                        if (u.listTitle == null) CivilightText(MapCopy.TRACK_EMPTY, type.small, color = colors.ink3)
                        u.listTitle?.let { CivilightText(it, type.sec, color = colors.ink3) }
                        u.rows.forEachIndexed { i, r ->
                            ListRow(
                                "${r.episode} | ${r.place}", subtitle = r.detail, compact = true, titleDotted = !r.onMap,
                                selected = r.current,
                                onClick = { playing = false; pointUnit = r.sceneKey },
                                modifier = if (i == 0) Modifier.tourTarget(TrackTourSteps.PLACE_TIME) else Modifier,
                            )
                        }
                    }
                }
            }
            val foot: @Composable ColumnScope.() -> Unit = {
                warning?.let { w -> WarningCard(w) { playing = false; pointUnit = w.sceneKey } }
                CivilightText(DomainTrack.NOTE, type.small, color = colors.ink3)
            }
            val tabs = listOf(MapCopy.TAB_TRACK, MapCopy.TAB_TRACK_PLACES, MapCopy.TAB_LAYERS)
            val tabsFallback = TrackTourSteps.TABS_FALLBACK.fold<Int, Modifier>(Modifier) { m, step -> m.tourTarget(step, fallback = true) }
            FoldSwitch(inner) { isInner ->
                if (isInner) {
                    // Board `MapTrack`: aside 280 wide, from 8 under the top bar to 16 above the bottom.
                    MapAside(
                        aside, tabs, tab, { tab = it },
                        Modifier.align(Alignment.TopEnd)
                            .padding(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 16.dp)
                            .then(tabsFallback),
                        width = MapAsideRules.WIDE,
                        fillHeight = true,
                    ) {
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
                        Column(
                            Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) { TabContent(tab) { t -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { body(t) } } }
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = foot)
                    }
                    val endGap = if (aside.closed) MapAsideRules.REOPEN + 2 * MapAsideRules.EDGE else MapAsideRules.WIDE + 2 * MapAsideRules.EDGE
                    GlassCard(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .padding(start = 16.dp, end = endGap.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                        radius = 18,
                        content = scrubber,
                    )
                } else {
                    GlassCard(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 12.dp).heightIn(max = 420.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chips()
                            CivilightTabs(tabs, tab, { tab = it }, tabsFallback, gap = TabGap.Panel)
                            TabContent(tab) { t -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { body(t) } }
                            foot()
                            scrubber()
                        }
                    }
                }
            }
        }
    }
}

/** `재생` / `멈춤` (board `MapTrack`): a filled 36 dp `pri` circle with a solid glyph in `onpri`, 44 dp to touch. */
@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val label = if (playing) MapCopy.TRACK_PAUSE else MapCopy.TRACK_PLAY
    Box(
        Modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape)
            .size(44.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(36.dp)) {
            drawCircle(colors.pri)
            val u = size.minDimension / 36f
            val c = center
            if (playing) {
                val w = 3.2f * u
                drawRect(colors.onpri, Offset(c.x - 5f * u, c.y - 6f * u), Size(w, 12f * u))
                drawRect(colors.onpri, Offset(c.x + 5f * u - w, c.y - 6f * u), Size(w, 12f * u))
            } else {
                // The board's `M8 5v14l11-7z` at 14 dp, centred on its optical middle.
                val k = 14f * u / 24f
                val ox = c.x - 13.5f * k
                val oy = c.y - 12f * k
                val tri = Path().apply {
                    moveTo(ox + 8f * k, oy + 5f * k)
                    lineTo(ox + 8f * k, oy + 19f * k)
                    lineTo(ox + 19f * k, oy + 12f * k)
                    close()
                }
                drawPath(tri, colors.onpri)
            }
        }
    }
}

/** `▲ 이동 시간 · 하르트 · 1-5 → 1-7` over `도보 2일 4시간 · 있음 9시간 40분` (board `MapTrack`, `tint` card, radius 14). */
@Composable
private fun WarningCard(w: TrackWarningUi, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.tint).insetOutline(colors.edge, 1.dp, shape)
            .pressable(onClick = onClick, shape = shape).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CivilightText(MapCopy.TRACK_WARN, type.label, color = colors.warn, maxLines = 1)
            CivilightText(w.who, type.small, Modifier.padding(start = 8.dp), color = colors.ink3, maxLines = 1)
        }
        CivilightText(w.detail, type.small, Modifier.padding(top = 4.dp), color = colors.ink2)
    }
}

/** The position chip of a character: when the lens moves it walks along the leg to the new stop on `enter` (it jumps when motion is reduced). */
@Composable
private fun WalkingChip(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc?, pos: TrackPosition, lines: List<TrackLine>, scope: MapMarkScope) {
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
    val portrait = io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, pos.characterKey.takeIf { it.isNotEmpty() })
    PersonChip(pos.name, Modifier.mapAt(p.x, p.y, ay = 1.6f), image = portrait)
}
