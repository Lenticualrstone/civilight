package io.github.lenticualrstone.civilight.feature.map.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * (0.2.0, S-3, WLD-75) The entry of a route whose screen is hidden in 0.2.0 (its data is kept). It draws
 * nothing and turns the route back at once: to [instead] when given (a node map opens view-only in
 * 지도 보기), else back to where it came from. So a hidden tool is never reachable, even from a stale
 * back stack or another area's button.
 */
@Composable
fun HiddenRoute(deps: FeatureDeps, instead: Route? = null) {
    LaunchedEffect(instead) {
        if (instead != null) deps.navigator.replace(instead) else deps.navigator.pop()
    }
}
