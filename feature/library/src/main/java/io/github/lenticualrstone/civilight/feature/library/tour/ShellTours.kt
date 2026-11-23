package io.github.lenticualrstone.civilight.feature.library.tour

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.CharacterTrack
import io.github.lenticualrstone.civilight.navigation.MapEdit
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * Where the shell runs each module's first-entry tour (LIB-19): one route per module, so a list and
 * its detail pane never show the same tour twice. Each is the screen the module's board draws the
 * tour on (`Tour1` script, `TourCast` 인물 기록, `TourWorld` 세계, `TourLr` 녹음 중, `TourMap` the map
 * editor's tool row). `인물 동선 · 장소와 때` has no route of its own (its board is the `장소와 때`
 * sheet over the script), so it runs on 인물 동선. The 서재 hosts its own `서재 · 숨기기` tour
 * (`LibraryScreen`), so [moduleOf] leaves it out.
 */
object ShellTours {
    fun moduleOf(route: Any?): TourModule? = when (route) {
        is Script -> TourModule.SCRIPT
        is CharacterProfile -> TourModule.CAST
        is World -> TourModule.WORLD_TIMELINE
        is Recording -> TourModule.RECORDER
        is MapEdit -> TourModule.MAP
        is CharacterTrack -> TourModule.ROUTE_PLACE_TIME
        else -> null
    }
}

/**
 * The shell's wrapper of one route's content: the module tour of [route] ([ShellTours.moduleOf])
 * over [content], or [content] as is. Each area marks its own holes with `Modifier.tourTarget(step)`
 * (스크립트 `ScriptTourSteps`, 인물 `CastTourSteps`, 지도 `MapTourSteps`); a step with no marked,
 * on-screen target or fallback (세계 · 레코더 · 장소와 때 so far) shows its card in the middle without
 * a hole (`TourHost`'s last resort).
 */
@Composable
fun ModuleTourFrame(route: Any?, deps: FeatureDeps, content: @Composable () -> Unit) {
    val module = ShellTours.moduleOf(route)
    if (module == null) {
        content()
    } else {
        ModuleTourHost(module, deps, Modifier.fillMaxSize()) { content() }
    }
}
