@file:JvmName("LibraryEntries")

package io.github.lenticualrstone.civilight.feature.library

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectScreen
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsScreen
import io.github.lenticualrstone.civilight.feature.library.rating.RatingChangeContent
import io.github.lenticualrstone.civilight.feature.library.rating.RatingDefaultsApplyScreen
import io.github.lenticualrstone.civilight.feature.library.rating.RatingSettingsScreen
import io.github.lenticualrstone.civilight.feature.library.shelf.LibraryScreen
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.NewProject
import io.github.lenticualrstone.civilight.navigation.ProjectSettings
import io.github.lenticualrstone.civilight.navigation.RatingDefaultsApply
import io.github.lenticualrstone.civilight.navigation.SettingsRating
import io.github.lenticualrstone.civilight.navigation.RatingChangeSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * feature:library's routes and shared sheets (shell.md §1). Runs once per activity inside
 * `entryProvider { }`: registers only, no I/O. Routes of this lane not registered yet keep the
 * shell's placeholder.
 */
fun EntryProviderScope<NavKey>.libraryEntries(deps: FeatureDeps) {
    val services = LibraryServices(deps)
    entry<Library> { LibraryScreen(services) }
    entry<NewProject> { route -> NewProjectScreen(route, services) }
    entry<SettingsRating> { RatingSettingsScreen(services) }
    entry<RatingDefaultsApply> { route -> RatingDefaultsApplyScreen(route, services) }
    entry<ProjectSettings> { route -> ProjectSettingsScreen(route, services) }
    deps.sheets.register<RatingChangeSheet> { sheet -> RatingChangeContent(sheet, services) }
}
