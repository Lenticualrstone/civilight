@file:JvmName("MapEntries")

package io.github.lenticualrstone.civilight.feature.map

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.map.edit.MapEditScreen
import io.github.lenticualrstone.civilight.feature.map.edit.DetailStampsScreen
import io.github.lenticualrstone.civilight.navigation.DetailStamps
import io.github.lenticualrstone.civilight.navigation.HexPaint
import io.github.lenticualrstone.civilight.navigation.PolygonRegions
import io.github.lenticualrstone.civilight.navigation.TextMap
import io.github.lenticualrstone.civilight.navigation.CharacterTrack
import io.github.lenticualrstone.civilight.feature.map.track.CharacterTrackScreen
import io.github.lenticualrstone.civilight.feature.map.text.TextMapScreen
import io.github.lenticualrstone.civilight.feature.map.poly.PolygonRegionsScreen
import io.github.lenticualrstone.civilight.feature.map.hex.HexPaintScreen
import io.github.lenticualrstone.civilight.feature.map.list.MapListScreen
import io.github.lenticualrstone.civilight.feature.map.node.NodeMapScreen
import io.github.lenticualrstone.civilight.feature.map.gen.MapFormatPickerScreen
import io.github.lenticualrstone.civilight.feature.map.gen.MapGenerateScreen
import io.github.lenticualrstone.civilight.feature.map.pick.PickOnMapScreen
import io.github.lenticualrstone.civilight.feature.map.reveal.MapRevealScreen
import io.github.lenticualrstone.civilight.feature.map.scale.MapScaleScreen
import io.github.lenticualrstone.civilight.feature.map.sketch.SketchStraightenScreen
import io.github.lenticualrstone.civilight.feature.map.view.MapViewScreen
import io.github.lenticualrstone.civilight.navigation.MapEdit
import io.github.lenticualrstone.civilight.navigation.MapList
import io.github.lenticualrstone.civilight.navigation.MapView
import io.github.lenticualrstone.civilight.navigation.NodeMap
import io.github.lenticualrstone.civilight.navigation.PickOnMap
import io.github.lenticualrstone.civilight.navigation.MapFormatPicker
import io.github.lenticualrstone.civilight.navigation.MapGenerate
import io.github.lenticualrstone.civilight.navigation.MapReveal
import io.github.lenticualrstone.civilight.navigation.MapScale
import io.github.lenticualrstone.civilight.navigation.SketchStraighten
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.feature.map.common.HiddenRoute

/**
 * The map area's entries (shell.md §1): every map route. `MapList` (WLD-36 … 39), `MapView`
 * (WLD-37, 38, 41, 48, 51, 57), `MapEdit` (WLD-40 … 45, 47, 48, 65), `PickOnMap` (WLD-52),
 * `MapScale` (WLD-46), `MapReveal` (WLD-49), `NodeMap` (WLD-50), `MapFormatPicker` and
 * `MapGenerate` (WLD-53 … 55), `SketchStraighten` (WLD-39), `DetailStamps` (WLD-60), `HexPaint`
 * (WLD-58), `PolygonRegions` (WLD-59), `TextMap` (WLD-61) and `CharacterTrack` (WLD-62). This
 * module owns no shared sheet (`Sheets.kt` names none with owner MAP); its sheets are feature-local.
 */
fun EntryProviderScope<NavKey>.mapEntries(deps: FeatureDeps) {
    entry<MapList> { route -> MapListScreen(route, deps) }
    entry<MapView> { route -> MapViewScreen(route, deps) }
    entry<MapEdit> { route -> MapEditScreen(route, deps) }
    entry<PickOnMap> { route -> PickOnMapScreen(route, deps) }
    entry<MapReveal> { route -> MapRevealScreen(route, deps) }
    // (0.2.0, S-3, WLD-75) Hidden, data kept: distances (scale), generation, sketch, node / terrain / hex /
    // polygon editors, text → terrain and 인물 동선 have no entry in 0.2.0. A route pushed from elsewhere
    // (a stale back stack, another area) is turned back: a node map opens view-only in 지도 보기, the rest
    // leave at once, so no hidden tool can be reached.
    entry<MapScale> { HiddenRoute(deps) }
    entry<MapFormatPicker> { HiddenRoute(deps) }
    entry<MapGenerate> { HiddenRoute(deps) }
    entry<SketchStraighten> { HiddenRoute(deps) }
    entry<NodeMap> { route -> HiddenRoute(deps, MapView(route.projectKey, route.mapKey)) }
    entry<DetailStamps> { HiddenRoute(deps) }
    entry<HexPaint> { HiddenRoute(deps) }
    entry<PolygonRegions> { HiddenRoute(deps) }
    entry<TextMap> { HiddenRoute(deps) }
    entry<CharacterTrack> { HiddenRoute(deps) }
}
