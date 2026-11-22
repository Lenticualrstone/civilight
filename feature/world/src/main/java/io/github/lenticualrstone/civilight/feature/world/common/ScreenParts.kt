package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.serialName
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.popTo
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.navigation.FactionCard as FactionCardRoute
import io.github.lenticualrstone.civilight.navigation.PlaceCard as PlaceCardRoute
import io.github.lenticualrstone.civilight.navigation.TermCard as TermCardRoute

/**
 * The `‹` of a world screen: none in the detail pane (CNV-33); in the list pane it leaves the list
 * together with the card beside it; alone it pops.
 */
@Composable
fun worldBack(route: Route, deps: FeatureDeps): IconAction? = when (LocalPaneRole.current) {
    PaneRole.DETAIL -> null
    PaneRole.LIST -> IconAction(WorldCopy.BACK) { if (!deps.navigator.popTo(route, inclusive = true)) deps.navigator.pop() }
    PaneRole.SINGLE -> IconAction(WorldCopy.BACK) { deps.navigator.pop() }
}

/**
 * The bottom command bar: tapping opens project search (`장소 · 용어 · 세력 찾기`, `명령 또는 검색`);
 * the mic starts the recorder with this screen as `보던 곳` (rule 23: a card links that card).
 */
@Composable
fun WorldCommandBar(
    deps: FeatureDeps,
    projectKey: Key,
    hint: String,
    surface: EntrySurface = EntrySurface.OTHER_IN_PROJECT,
    targetKind: RecorderTargetKind? = null,
    targetKey: Key? = null,
) {
    CommandBar(
        hint = hint,
        onClick = { deps.navigator.push(ProjectSearch(projectKey)) },
        onMic = {
            deps.navigator.push(
                Recording(
                    surface = surface.serialName,
                    projectKey = projectKey,
                    targetKind = targetKind?.serialName,
                    targetKey = targetKey,
                ),
            )
        },
    )
}

/** The key of the world card open beside the list (two panes), for the list's selected row. */
fun selectedCardKey(deps: FeatureDeps): Key? = when (val top = deps.navigator.backStack.lastOrNull()) {
    is TermCardRoute -> top.termKey
    is PlaceCardRoute -> top.placeKey
    is FactionCardRoute -> top.factionKey
    else -> null
}
