package io.github.lenticualrstone.civilight.feature.map.gen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.domain.world.gen.GenInput
import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapNav
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.MapStylePref
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.navigation.MapFormatPicker
import io.github.lenticualrstone.civilight.navigation.MapGenerate
import io.github.lenticualrstone.civilight.navigation.NodeMap
import io.github.lenticualrstone.civilight.navigation.TextMap
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import kotlin.random.Random

/** `양식` (WLD-54 1/2): `새 지도 › 지형 만들기`, keeping the pin parent of a `하위 지도` (WLD-48). */
@Composable
fun MapFormatPickerScreen(route: MapFormatPicker, deps: FeatureDeps) {
    val parent = route.parentMapKey?.let { m -> route.parentPinKey?.let { MapParent(m, it) } }
    GenerateFlow(route.projectKey, parent, startFormat = null, deps)
}

/** `단계별 만들기` (WLD-54 2/2, WLD-55) entered directly for one format. */
@Composable
fun MapGenerateScreen(route: MapGenerate, deps: FeatureDeps) {
    GenerateFlow(route.projectKey, parent = null, startFormat = GenFlowState.formatOf(route.format) ?: MapFormat.DETAIL, deps)
}

/** A fresh 4-digit seed (`0031`, `4827`). */
internal fun nextSeed(): Int = Random.nextInt(0, 10_000)

private fun styleOf(pref: MapStylePref): MapStyle = when (pref) {
    MapStylePref.SOFT -> MapStyle.SOFT
    MapStylePref.INK -> MapStyle.INK
    MapStylePref.COLOR -> MapStyle.COLOR
    MapStylePref.PARCHMENT -> MapStyle.PARCHMENT
}

/**
 * Both pages of `지형 만들기` in one back-stack entry, so the session (seeds, locks, candidates)
 * carries from `양식` to the steps and back (`같은 세계 · 양식만 다름`). The state is saveable JSON.
 * The new style starts at the app setting `새 지도 그림`.
 */
@Composable
private fun GenerateFlow(projectKey: Key, parent: MapParent?, startFormat: MapFormat?, deps: FeatureDeps) {
    val vm = entryViewModel { MapGenerateViewModel(projectKey, MapEnv.of(deps)) }
    val readOnly by vm.readOnly.collectAsState()
    val settings by deps.settings.collectAsState()
    var text by rememberSaveable { mutableStateOf<String?>(null) }
    val state = remember(text) {
        GenFlowState.decode(text) ?: GenFlowState.start(::nextSeed, startFormat).copy(style = styleOf(settings.defaultMapStyle)).also { text = it.encode() }
    }
    fun set(next: GenFlowState) { text = next.encode() }
    val back = IconAction(MapCopy.BACK) {
        if (state.page == 2 && state.fromFormats) set(GenFlow.toFormats(state)) else deps.navigator.pop()
    }
    fun start() {
        vm.start(state, parent) { key -> deps.navigator.replace(MapNav.routeOf(vm.doc.value, projectKey, key)) }
    }
    // The two pages push and pop like a route (S1 shape, in-route); only the page changes, the session carries over.
    val motion = CivilightTheme.motion
    AnimatedContent(state.page, transitionSpec = { motion.push(forward = targetState > initialState) }, label = "gen-page") { page ->
        if (page == 1) {
            FormatPage(
                state = state,
                vm = vm,
                back = back,
                readOnly = readOnly,
                onPick = { set(GenFlow.pickFormat(state, it)) },
                onText = { deps.navigator.push(TextMap(projectKey)) },
                onPrevious = { deps.navigator.pop() },
                onNext = {
                    if (state.format == MapFormat.NODE) {
                        vm.start(state, parent) { key -> deps.navigator.replace(NodeMap(projectKey, key)) }
                    } else {
                        set(GenFlow.toSteps(state))
                    }
                },
            )
        } else {
            StepsPage(state = state, vm = vm, back = back, readOnly = readOnly, defaultName = vm.doc.value?.let { MapCopy.defaultMapName(it.maps.size + 1) }, set = ::set, onStart = ::start)
        }
    }
}

/** The generated world of [input] drawn as [format]'s preview (non-interactive). */
@Composable
internal fun WorldPreview(
    vm: MapGenerateViewModel,
    input: GenInput,
    format: MapFormat,
    modifier: Modifier = Modifier,
    /** How the preview changes when the world or the format does: `standard`; the steps page passes `mapReroll` (400 ms). */
    token: MotionToken = CivilightMotion.standard,
) {
    val world by produceState<GeneratedWorld?>(null, input) { value = vm.world(input) }
    val motion = CivilightTheme.motion
    Box(modifier.fillMaxSize()) {
        Crossfade(world to format, animationSpec = motion.spec(token), label = "gen-preview") { (current, shownFormat) ->
            val base: MapBase = if (current == null) MapBase.Blank else vm.base(current, shownFormat)
            MapCanvas(base = base, scene = MapScene(), viewport = Viewport(), onViewportChange = {}, interactive = false, showRouteChips = false)
        }
    }
}
