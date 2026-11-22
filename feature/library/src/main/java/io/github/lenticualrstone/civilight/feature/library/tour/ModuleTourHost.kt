package io.github.lenticualrstone.civilight.feature.library.tour

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.launch

/** The card words of every module tour (LIB-19). */
val ModuleTourButtons = TourButtons(ModuleTours.SKIP, ModuleTours.PREVIOUS, ModuleTours.NEXT, ModuleTours.FINISH)

/**
 * A module's first-entry tour (LIB-19) over [content]: runs when `ModuleTours.shouldStart` says so
 * (not seen, copy complete) and marks the module seen on `건너뛰기`, `마치기` or back. Targets are
 * marked in [content] with `Modifier.tourTarget(step)`.
 */
@Composable
fun ModuleTourHost(module: TourModule, deps: FeatureDeps, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val onboarding = deps.stores.appState.onboarding
    val state by onboarding.state.collectAsState()
    val steps = remember(module) { ModuleTours.steps(module).map { TourCardStep(it.title, it.sub) } }
    TourHost(
        steps = steps,
        label = module.label,
        running = ModuleTours.shouldStart(state, module),
        buttons = ModuleTourButtons,
        onDone = { deps.appScope.launch { onboarding.update { ModuleTours.markSeen(it, module) } } },
        modifier = modifier,
        content = content,
    )
}
