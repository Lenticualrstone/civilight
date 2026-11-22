@file:JvmName("WorldEntries")

package io.github.lenticualrstone.civilight.feature.world

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.world.faction.FactionCardScreen
import io.github.lenticualrstone.civilight.feature.world.list.WorldScreen
import io.github.lenticualrstone.civilight.feature.world.place.PlaceCardScreen
import io.github.lenticualrstone.civilight.feature.world.term.TermCardScreen
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * The world area's entries (shell.md §1). Registered: `World` (WLD-01 … WLD-04), `TermCard`
 * (WLD-08), `PlaceCard` (WLD-09), `FactionCard` (WLD-10). `Timeline`, `EventDetail`,
 * `TimelineMap`, `WorldCalendar`, `TravelModes` and the shared sheets `LinkCandidatesSheet`,
 * `EventCreateSheet` belong to later batches and keep the shell's placeholders until then.
 */
fun EntryProviderScope<NavKey>.worldEntries(deps: FeatureDeps) {
    entry<World> { route -> WorldScreen(route, deps) }
    entry<TermCard> { route -> TermCardScreen(route, deps) }
    entry<PlaceCard> { route -> PlaceCardScreen(route, deps) }
    entry<FactionCard> { route -> FactionCardScreen(route, deps) }
}
