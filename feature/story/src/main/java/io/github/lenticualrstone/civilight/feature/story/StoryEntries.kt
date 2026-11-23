@file:JvmName("StoryEntries")

package io.github.lenticualrstone.civilight.feature.story

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.story.check.CheckCenterScreen
import io.github.lenticualrstone.civilight.feature.story.home.HomeScreen
import io.github.lenticualrstone.civilight.feature.story.search.SearchScreen
import io.github.lenticualrstone.civilight.feature.story.move.MoveTargetContent
import io.github.lenticualrstone.civilight.feature.story.move.MoveTargetSheet
import io.github.lenticualrstone.civilight.feature.story.move.SceneMoveContent
import io.github.lenticualrstone.civilight.feature.story.menu.UnitMenuContent
import io.github.lenticualrstone.civilight.feature.story.newunit.NewUnitContent
import io.github.lenticualrstone.civilight.feature.story.settings.DayBandScreen
import io.github.lenticualrstone.civilight.feature.story.settings.NotationScreen
import io.github.lenticualrstone.civilight.feature.story.structure.StructureScreen
import io.github.lenticualrstone.civilight.feature.story.where.CascadeSheet
import io.github.lenticualrstone.civilight.feature.story.where.CascadeSheetContent
import io.github.lenticualrstone.civilight.feature.story.where.SceneWhereContent
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkScreen
import io.github.lenticualrstone.civilight.feature.story.search.LibrarySearchScreen
import io.github.lenticualrstone.civilight.navigation.BulkPlaceTime
import io.github.lenticualrstone.civilight.navigation.LibrarySearch
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.DayBandSettings
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.SceneMoveSheet
import io.github.lenticualrstone.civilight.navigation.SceneNotationSettings
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.UnitMenuSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * feature:story entries (shell.md §1). Routes: `ProjectHome`, `StoryStructure`, `SceneNotationSettings`,
 * `CheckCenter`, `ProjectSearch`, `DayBandSettings`, `BulkPlaceTime`, `LibrarySearch`. Shared sheets (owner STORY): `UnitMenuSheet`,
 * `NewUnitSheet`, `SceneWhereSheet`, `SceneMoveSheet`. Feature-local sheets registered so they open over
 * any host: `MoveTargetSheet`, `CascadeSheet`. The bulk screen's `BulkPlaceSheet` / `BulkWhenSheet` are
 * local to it.
 */
fun EntryProviderScope<NavKey>.storyEntries(deps: FeatureDeps) {
    entry<ProjectHome> { route -> HomeScreen(route, deps) }
    entry<StoryStructure> { route -> StructureScreen(route, deps) }
    entry<SceneNotationSettings> { route -> NotationScreen(route, deps) }
    entry<CheckCenter> { route -> CheckCenterScreen(route, deps) }
    entry<ProjectSearch> { route -> SearchScreen(route, deps) }
    entry<DayBandSettings> { route -> DayBandScreen(route, deps) }
    entry<BulkPlaceTime> { route -> BulkScreen(route, deps) }
    entry<LibrarySearch> { route -> LibrarySearchScreen(route, deps) }
    entry<io.github.lenticualrstone.civilight.navigation.MarksList> { route -> io.github.lenticualrstone.civilight.feature.story.home.MarksListScreen(route, deps) }
    deps.sheets.register<UnitMenuSheet> { sheet -> UnitMenuContent(sheet, deps) }
    deps.sheets.register<NewUnitSheet> { sheet -> NewUnitContent(sheet, deps) }
    deps.sheets.register<SceneWhereSheet> { sheet -> SceneWhereContent(sheet, deps) }
    deps.sheets.register<SceneMoveSheet> { sheet -> SceneMoveContent(sheet, deps) }
    // Feature-local: the `옮기기` destination picker, opened from the ⋯ on any host screen.
    deps.sheets.register<MoveTargetSheet> { sheet -> MoveTargetContent(sheet, deps) }
    // Feature-local: the cascade preview started outside the `장소와 때` sheet (점검 센터 time chip).
    deps.sheets.register<CascadeSheet> { sheet -> CascadeSheetContent(sheet, deps) }
    // `RenumberSheet` (SCR-22) is SCRIPT's shared sheet: opened here, registered by feature:script only.
}
