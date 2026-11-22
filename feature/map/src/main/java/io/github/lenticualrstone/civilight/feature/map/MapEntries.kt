@file:JvmName("MapEntries")

package io.github.lenticualrstone.civilight.feature.map

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.map.edit.MapEditScreen
import io.github.lenticualrstone.civilight.feature.map.list.MapListScreen
import io.github.lenticualrstone.civilight.feature.map.pick.PickOnMapScreen
import io.github.lenticualrstone.civilight.feature.map.view.MapViewScreen
import io.github.lenticualrstone.civilight.navigation.MapEdit
import io.github.lenticualrstone.civilight.navigation.MapList
import io.github.lenticualrstone.civilight.navigation.MapView
import io.github.lenticualrstone.civilight.navigation.PickOnMap
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * The map area's entries (shell.md §1). Registered: `MapList` (WLD-36 … 39), `MapView` (WLD-37,
 * 38, 41), `MapEdit` (WLD-40 … 44) and `PickOnMap` (WLD-52). `MapScale`, `MapReveal`, `NodeMap`,
 * `MapFormatPicker`, `MapGenerate`, `HexPaint`, `PolygonRegions`, `DetailStamps`, `TextMap` and
 * `CharacterTrack` belong to later batches and keep the shell's placeholders until then. This
 * module owns no shared sheet (`Sheets.kt` names none with owner MAP).
 */
fun EntryProviderScope<NavKey>.mapEntries(deps: FeatureDeps) {
    entry<MapList> { route -> MapListScreen(route, deps) }
    entry<MapView> { route -> MapViewScreen(route, deps) }
    entry<MapEdit> { route -> MapEditScreen(route, deps) }
    entry<PickOnMap> { route -> PickOnMapScreen(route, deps) }
}
