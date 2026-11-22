@file:JvmName("StoryEntries")

package io.github.lenticualrstone.civilight.feature.story

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.story.home.HomeScreen
import io.github.lenticualrstone.civilight.feature.story.menu.RenumberContent
import io.github.lenticualrstone.civilight.feature.story.menu.RenumberSheet
import io.github.lenticualrstone.civilight.feature.story.menu.UnitMenuContent
import io.github.lenticualrstone.civilight.feature.story.newunit.NewUnitContent
import io.github.lenticualrstone.civilight.feature.story.settings.NotationScreen
import io.github.lenticualrstone.civilight.feature.story.structure.StructureScreen
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.SceneNotationSettings
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.UnitMenuSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * feature:story entries (shell.md §1). Routes built so far: `ProjectHome`, `StoryStructure`,
 * `SceneNotationSettings`. Shared sheets (owner STORY) built so far: `UnitMenuSheet`, `NewUnitSheet`.
 * The other story routes (`CheckCenter`, `ProjectSearch`, `LibrarySearch`, `BulkPlaceTime`,
 * `DayBandSettings`) and sheets (`SceneMoveSheet`, `SceneWhereSheet`) are registered here by the
 * batches that build them; until then the shell shows its placeholder.
 */
fun EntryProviderScope<NavKey>.storyEntries(deps: FeatureDeps) {
    entry<ProjectHome> { route -> HomeScreen(route, deps) }
    entry<StoryStructure> { route -> StructureScreen(route, deps) }
    entry<SceneNotationSettings> { route -> NotationScreen(route, deps) }
    deps.sheets.register<UnitMenuSheet> { sheet -> UnitMenuContent(sheet, deps) }
    deps.sheets.register<NewUnitSheet> { sheet -> NewUnitContent(sheet, deps) }
    // Feature-local (no shared key yet, see Requests); registered so it opens over the script too.
    deps.sheets.register<RenumberSheet> { sheet -> RenumberContent(sheet, deps) }
}
