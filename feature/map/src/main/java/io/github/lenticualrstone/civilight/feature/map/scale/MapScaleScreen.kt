package io.github.lenticualrstone.civilight.feature.map.scale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.feature.map.common.FoldSwitch
import io.github.lenticualrstone.civilight.feature.map.common.appear
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.canvas.mapAt
import io.github.lenticualrstone.civilight.feature.map.common.CardHeader
import io.github.lenticualrstone.civilight.feature.map.common.CardRow
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.feature.map.common.TopBarTapGuard
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.feature.map.travel.TravelModeContent
import io.github.lenticualrstone.civilight.feature.map.travel.TravelModeSheet
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.MapScale
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/** 축척 맞추기 (WLD-46): `축척 저장` is one undo step `축척 · <map>`. */
class MapScaleViewModel(projectKey: Key, val mapKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    fun save(draft: ScaleDraft, onDone: () -> Unit) {
        val ctx = editContext()
        val name = doc.value?.maps?.get(mapKey)?.name
        commit(MapCopy.step(MapCopy.SCALE_TITLE, name), onDone) { d -> ScaleEdit.save(d, mapKey, draft, ctx) }
    }
}

/**
 * 축척 맞추기 (WLD-46, board `MapScale`): the map with the marks `A` and `B`; a card `축척 맞추기 ·
 * 닫기` with the hint, the number and unit chips `일 · km · 리 · 직접`, `하루 이동 | 도보 15 km ›`
 * for `일`, the rows `A | 결사의 문` and `B | 잿더미 시장` (the chosen row is placed by the next tap
 * on the map), and `축척 저장`. Cover: the card at the bottom; inner: at the right (`[PROPOSED]`).
 */
@Composable
fun MapScaleScreen(route: MapScale, deps: FeatureDeps) {
    val vm = entryViewModel { MapScaleViewModel(route.projectKey, route.mapKey, MapEnv.of(deps)) }
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val hiddenOpen by vm.hiddenOpen.collectAsState()
    val sheets = rememberSheetHost()
    val inner = LocalWindowLayout.current.isInner
    var draftText by rememberSaveable(route.mapKey) { mutableStateOf<String?>(null) }
    var viewportArray by rememberSaveable(route.mapKey) { mutableStateOf(Viewport.UNSET.toArray()) }
    val d = doc
    val map = d?.maps?.get(route.mapKey)
    val draft = remember(draftText, d != null) { ScaleDraft.decode(draftText) ?: if (d != null && map != null) ScaleDraft.of(d, map) else null }
    val scene = remember(d, hiddenOpen) { if (d != null && map != null) MapScenes.build(d, map, MapScreenViewModel.lensOf(d), hiddenOpen, SceneMode.VIEW) else null }
    fun set(next: ScaleDraft) { draftText = next.encode() }

    CivilightScreen(
        title = MapCopy.SCALE_TITLE,
        smallLine = map?.name,
        back = mapBack(deps),
        sheets = sheets,
        localSheets = { key ->
            val doc0 = doc
            if (key !is TravelModeSheet || doc0 == null) null else {
                {
                    val current = ScaleDraft.decode(draftText) ?: draft
                    val selected = current?.let { Travel.modes(doc0).getOrNull(it.modeIndex)?.key }
                    TravelModeContent(
                        doc0, selected, readOnly,
                        onPick = { i -> current?.let { set(it.copy(modeIndex = i)) }; close() },
                        onSave = { modeKey, name, km, hours ->
                            val newIndex = Travel.modes(doc0).size
                            vm.saveTravelMode(modeKey, name, km, hours) {
                                if (modeKey == null) (ScaleDraft.decode(draftText) ?: draft)?.let { set(it.copy(modeIndex = newIndex)) }
                            }
                        },
                        onClose = ::close,
                    )
                }
            }
        },
    ) { padding ->
        if (d == null || map == null || draft == null || scene == null) return@CivilightScreen
        val ui = ScaleEdit.ui(d, scene, draft)
        val accent = CivilightTheme.colors.acc
        Box(Modifier.fillMaxSize()) {
            MapCanvas(
                base = MapBases.of(map),
                scene = scene,
                viewport = Viewport.fromArray(viewportArray),
                onViewportChange = { viewportArray = it.toArray() },
                showRouteChips = false,
                onTap = { p, _ -> if (!readOnly) set(ScaleEdit.tap(draft, scene, p)) },
                // The measured line A → B (board `MapScale`: 2 dp in `acc`).
                extraDraw = { scope ->
                    val a = draft.a
                    val b = draft.b
                    if (a != null && b != null) {
                        val sa = scope.screen(a)
                        val sb = scope.screen(b)
                        drawLine(accent, Offset(sa.x, sa.y), Offset(sb.x, sb.y), 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                },
                marks = { scope ->
                    draft.a?.let { p -> val s = scope.screen(p); MapValueChip(MapCopy.SCALE_A, Modifier.mapAt(s.x, s.y).appear()) }
                    draft.b?.let { p -> val s = scope.screen(p); MapValueChip(MapCopy.SCALE_B, Modifier.mapAt(s.x, s.y).appear()) }
                },
            )
            TopBarTapGuard(padding.calculateTopPadding())
            FoldSwitch(inner) { isInner ->
            val cardModifier = if (isInner) {
                Modifier.align(Alignment.TopEnd).padding(top = padding.calculateTopPadding() + 8.dp, end = 16.dp).width(320.dp)
            } else {
                Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth()
            }
            GlassCard(cardModifier) {
                CardHeader(MapCopy.SCALE_SHEET, onClose = { deps.navigator.pop() })
                CivilightText(MapCopy.SCALE_HINT, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CivilightTextField(
                        ui.value, { set(draft.copy(value = it)) }, Modifier.width(96.dp),
                        label = MapCopy.SCALE_TITLE, enabled = !readOnly,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = CivilightTheme.type.number,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MapCopy.SCALE_UNITS.forEachIndexed { i, unit ->
                            FilterChip(unit, ui.unitIndex == i, { set(ScaleEdit.unit(draft, i)) }, enabled = !readOnly)
                        }
                    }
                }
                FoldRevealValue(ui.dayTravel) { text -> CardRow(MapCopy.DAY_TRAVEL, text, onClick = { sheets.open(TravelModeSheet(forScale = true)) }) }
                ListRow(MapCopy.SCALE_A, subtitle = ui.aLabel, selected = ui.active == 0, compact = true, onClick = { set(ScaleEdit.choose(draft, 0)) })
                ListRow(MapCopy.SCALE_B, subtitle = ui.bLabel, selected = ui.active == 1, compact = true, onClick = { set(ScaleEdit.choose(draft, 1)) })
                CivilightButton(MapCopy.BTN_SAVE_SCALE, { vm.save(draft) { deps.navigator.pop() } }, Modifier.fillMaxWidth(), enabled = !readOnly && ui.canSave)
            }
            }
        }
    }
}
