@file:JvmName("WorldEntries")

package io.github.lenticualrstone.civilight.feature.world

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.world.faction.FactionCardScreen
import io.github.lenticualrstone.civilight.feature.world.list.WorldScreen
import io.github.lenticualrstone.civilight.feature.world.place.PlaceCardScreen
import io.github.lenticualrstone.civilight.feature.world.term.LinkCandidatesSheetContent
import io.github.lenticualrstone.civilight.feature.world.term.TermCardScreen
import io.github.lenticualrstone.civilight.feature.world.term.registerRevealSheets
import io.github.lenticualrstone.civilight.feature.world.time.EventCreateSheetContent
import io.github.lenticualrstone.civilight.feature.world.time.EventDetailScreen
import io.github.lenticualrstone.civilight.feature.world.time.TimelineMapScreen
import io.github.lenticualrstone.civilight.feature.world.travel.TravelModesScreen
import io.github.lenticualrstone.civilight.feature.world.time.TimelineScreen
import io.github.lenticualrstone.civilight.feature.world.time.WorldTimeScreen
import io.github.lenticualrstone.civilight.navigation.EventCreateSheet
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.LinkCandidatesSheet
import io.github.lenticualrstone.civilight.navigation.Timeline
import io.github.lenticualrstone.civilight.navigation.TimelineMap
import io.github.lenticualrstone.civilight.navigation.TravelModes
import io.github.lenticualrstone.civilight.navigation.WorldCalendar
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * The world area's entries (shell.md §1). Every world route is registered: `World` (WLD-01 … 05),
 * `TermCard` (WLD-08), `PlaceCard` (WLD-09, 63), `FactionCard` (WLD-10), `Timeline` (WLD-31, 34),
 * `EventDetail` (WLD-32/33), `WorldCalendar` (WLD-29/30), `TimelineMap` (WLD-64/65) and
 * `TravelModes` (WLD-47); the WORLD shared sheets `EventCreateSheet` (WLD-32) and
 * `LinkCandidatesSheet` (WLD-23); and the reveal sheets (WLD-18/28).
 */
fun EntryProviderScope<NavKey>.worldEntries(deps: FeatureDeps) {
    entry<World> { route -> WorldScreen(route, deps) }
    entry<TermCard> { route -> TermCardScreen(route, deps) }
    entry<PlaceCard> { route -> PlaceCardScreen(route, deps) }
    entry<FactionCard> { route -> FactionCardScreen(route, deps) }
    entry<Timeline> { route -> TimelineScreen(route, deps) }
    entry<EventDetail> { route -> EventDetailScreen(route, deps) }
    entry<WorldCalendar> { route -> WorldTimeScreen(route, deps) }
    entry<TimelineMap> { route -> TimelineMapScreen(route, deps) }
    // (0.2.0, S-3, WLD-47, WLD-75) `이동 수단` is hidden with the distances (values kept). Its only entry,
    // `feature:library`'s `프로젝트 설정 › 이동 수단` row, is hidden in the same change.
    entry<TravelModes> { route -> if (SHOWS_TRAVEL_MODES) TravelModesScreen(route, deps) else HiddenTravelModes(deps) }
    deps.sheets.register<EventCreateSheet> { sheet -> EventCreateSheetContent(sheet) }
    deps.sheets.register<LinkCandidatesSheet> { sheet -> LinkCandidatesSheetContent(sheet) }
    // `공개 시점 옮기기` and its steps read the open project, so they also show over other areas'
    // screens once those can name the sheet (WLD-28; see Requests: a shared key in core:navigation).
    registerRevealSheets(deps)
}

/** Whether the `TravelModes` route opens its editor; false since the library row is hidden (0.2.0 integration). */
internal const val SHOWS_TRAVEL_MODES: Boolean = false

/** (0.2.0, WLD-47) `이동 수단` without a screen: the route turns back at once and draws nothing. */
@androidx.compose.runtime.Composable
private fun HiddenTravelModes(deps: FeatureDeps) {
    androidx.compose.runtime.LaunchedEffect(Unit) { deps.navigator.pop() }
}
