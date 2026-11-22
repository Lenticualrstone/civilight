package io.github.lenticualrstone.civilight.feature.map.reveal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Color
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.feature.map.common.FoldSwitch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.designsystem.component.TimelineLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.MapReveal
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/** 시점별 공개 (WLD-49): `숨은 것 흐리게` is stored per map (`revealViewDim`), one undo step. */
class MapRevealViewModel(projectKey: Key, val mapKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    fun setDim(on: Boolean) {
        val ctx = editContext()
        commit(MapCopy.REVEAL_DIM) { d -> MapEdits.updateMap(d, mapKey, ctx) { it.copy(revealViewDim = on) } }
    }
}

/**
 * 시점별 공개 (WLD-49, board `MapReveal`): the map at the lens with hidden items left out, or dimmed
 * with their point in the tag (`상층 관문 · 1-7`); a card with `1-5 시점 · 보이는 것만` and the
 * `숨은 것 흐리게` switch, the point scrubber (one stop per episode; a tap moves the project lens
 * there, so the lens chip follows); and the summary chips `보임 · 핀 4 · 영역 1 · 아직 공개 전 · 2`.
 */
@Composable
fun MapRevealScreen(route: MapReveal, deps: FeatureDeps) {
    val vm = entryViewModel { MapRevealViewModel(route.projectKey, route.mapKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val sheets = rememberSheetHost()
    val lens = deps.lens(currentProjectStore)
    val inner = LocalWindowLayout.current.isInner
    var viewportArray by rememberSaveable(route.mapKey) { mutableStateOf(Viewport.UNSET.toArray()) }
    val d = doc
    val ui = remember(d, hiddenOpen) { d?.let { RevealState.build(it, route.mapKey, MapScreenViewModel.lensOf(it), hiddenOpen) } }

    CivilightScreen(
        title = MapCopy.REVEAL_TITLE,
        smallLine = ui?.small,
        back = mapBack(deps),
        lens = d?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        sheets = sheets,
    ) { padding ->
        val state = ui ?: return@CivilightScreen
        Box(Modifier.fillMaxSize()) {
            MapCanvas(
                base = state.base,
                scene = state.scene,
                viewport = Viewport.fromArray(viewportArray),
                onViewportChange = { viewportArray = it.toArray() },
                unhideFresh = true,
            )
            TopBarTapGuard(padding.calculateTopPadding())
            FoldSwitch(inner) { isInner ->
                Column(
                    Modifier
                        .align(if (isInner) Alignment.BottomStart else Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 16.dp)
                        .then(if (isInner) Modifier.widthIn(max = 480.dp) else Modifier.fillMaxWidth()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ValueSwap(state.modeLine, CivilightTheme.type.label, Modifier.weight(1f))
                            CivilightText(MapCopy.REVEAL_DIM, CivilightTheme.type.small, Modifier.padding(end = 8.dp), color = CivilightTheme.colors.ink3)
                            CivilightSwitch(state.dim, if (readOnly) null else ({ vm.setDim(it) }), enabled = !readOnly)
                        }
                        Scrubber(state.stops) { key -> lens.set(LensPoint.UnitEnd(key)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // The counts change with the lens: the value swap (CNV-42), the pending chip folds in and out.
                        ChipValue(state.visibleChip, CivilightTheme.colors.ink2)
                        FoldReveal(state.pendingChip.isNotEmpty(), horizontal = true) { ChipValue(state.pendingChip, CivilightTheme.colors.ink3) }
                    }
                }
            }
        }
    }
}

/** A summary chip (the `MapValueChip` pill) whose text swaps like a lens value. */
@Composable
private fun ChipValue(text: String, color: Color) {
    val shown = rememberLastNonNull(text.takeIf { it.isNotEmpty() }).orEmpty()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.heightIn(min = 32.dp).clip(shape).background(CivilightTheme.colors.tint).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ValueSwap(shown, CivilightTheme.type.chip.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = color)
    }
}

/**
 * The point scrubber: a track filled up to the current stop and one labelled stop per episode
 * (`1-1 · 1-3 · 1-5 …`). Each stop is a 44 dp button; the row scrolls when the episodes do not fit.
 */
@Composable
internal fun Scrubber(stops: List<RevealStop>, onStop: (Key) -> Unit) {
    val colors = CivilightTheme.colors
    val current = stops.indexOfFirst { it.current }
    val fraction = if (stops.size <= 1 || current < 0) (if (current >= 0) 1f else 0f) else current.toFloat() / (stops.size - 1)
    // The filled part of the track follows the lens on `standard` (it snaps when motion is reduced).
    val filled by animateFloatAsState(fraction, CivilightTheme.motion.move(CivilightMotion.standard), label = "revealFill")
    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.line)) {
        Box(Modifier.fillMaxWidth(filled).height(4.dp).background(colors.acc))
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        stops.forEach { stop ->
            Box(
                Modifier
                    .pressable(onClick = { onStop(stop.unitKey) }, role = Role.Button, onClickLabel = stop.code)
                    .widthIn(min = 44.dp)
                    .height(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                TimelineLabel(stop.code, stop.current)
            }
        }
    }
}
