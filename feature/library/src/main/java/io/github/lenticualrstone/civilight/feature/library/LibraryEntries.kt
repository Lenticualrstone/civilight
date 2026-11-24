@file:JvmName("LibraryEntries")

package io.github.lenticualrstone.civilight.feature.library

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.library.backup.BackupSettingsScreen
import io.github.lenticualrstone.civilight.feature.library.backup.RecoveryPasswordScreen
import io.github.lenticualrstone.civilight.feature.library.backup.RestoreBackupScreen
import io.github.lenticualrstone.civilight.feature.library.backup.RestoringScreen
import io.github.lenticualrstone.civilight.feature.library.export.ExportSheetContent
import io.github.lenticualrstone.civilight.feature.library.help.HelpReplayScreen
import io.github.lenticualrstone.civilight.feature.library.onboarding.ExampleTourScreen
import io.github.lenticualrstone.civilight.feature.library.onboarding.OnboardingScreen
import io.github.lenticualrstone.civilight.feature.library.onboarding.WalkthroughScreen
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectScreen
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileScreen
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsScreen
import io.github.lenticualrstone.civilight.feature.library.projectsettings.StructureLevelsScreen
import io.github.lenticualrstone.civilight.feature.library.rating.RatingChangeContent
import io.github.lenticualrstone.civilight.feature.library.rating.RatingDefaultsApplyScreen
import io.github.lenticualrstone.civilight.feature.library.rating.RatingSettingsScreen
import io.github.lenticualrstone.civilight.feature.library.settings.HidingSettingsScreen
import io.github.lenticualrstone.civilight.feature.library.settings.MoreSettingsScreen
import io.github.lenticualrstone.civilight.feature.library.settings.MotionSettingsScreen
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsScreen
import io.github.lenticualrstone.civilight.feature.library.settings.TextSizeScreen
import io.github.lenticualrstone.civilight.feature.library.shelf.LibraryScreen
import io.github.lenticualrstone.civilight.feature.library.trash.TrashScreen
import io.github.lenticualrstone.civilight.navigation.ExampleTour
import io.github.lenticualrstone.civilight.navigation.ExportSheet
import io.github.lenticualrstone.civilight.navigation.HelpReplay
import io.github.lenticualrstone.civilight.navigation.Onboarding
import io.github.lenticualrstone.civilight.navigation.Walkthrough
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.NewProject
import io.github.lenticualrstone.civilight.navigation.OpenFile
import io.github.lenticualrstone.civilight.navigation.ProjectSettings
import io.github.lenticualrstone.civilight.navigation.StructureLevels
import io.github.lenticualrstone.civilight.navigation.RatingDefaultsApply
import io.github.lenticualrstone.civilight.navigation.Settings
import io.github.lenticualrstone.civilight.navigation.SettingsHiding
import io.github.lenticualrstone.civilight.navigation.SettingsLayout
import io.github.lenticualrstone.civilight.navigation.SettingsMore
import io.github.lenticualrstone.civilight.navigation.SettingsMotion
import io.github.lenticualrstone.civilight.navigation.SettingsRating
import io.github.lenticualrstone.civilight.navigation.SettingsTextSize
import io.github.lenticualrstone.civilight.navigation.RatingChangeSheet
import io.github.lenticualrstone.civilight.navigation.RecoveryPassword
import io.github.lenticualrstone.civilight.navigation.RestoreBackup
import io.github.lenticualrstone.civilight.navigation.Restoring
import io.github.lenticualrstone.civilight.navigation.SettingsBackup
import io.github.lenticualrstone.civilight.navigation.Trash
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.launch

/**
 * feature:library's routes and shared sheets (shell.md §1). Runs once per activity inside
 * `entryProvider { }`: registers only, no I/O. Routes of this lane not registered yet keep the
 * shell's placeholder.
 */
fun EntryProviderScope<NavKey>.libraryEntries(deps: FeatureDeps) {
    val services = LibraryServices(deps)
    entry<Library> { LibraryScreen(services) }
    entry<NewProject> { route -> NewProjectScreen(route, services) }
    entry<Settings> { SettingsScreen(services) }
    entry<io.github.lenticualrstone.civilight.navigation.SettingsLibraryView> {
        io.github.lenticualrstone.civilight.feature.library.settings.ViewStyleScreen(io.github.lenticualrstone.civilight.feature.library.settings.ViewKind.LIBRARY, services)
    }
    entry<io.github.lenticualrstone.civilight.navigation.SettingsCardView> {
        io.github.lenticualrstone.civilight.feature.library.settings.ViewStyleScreen(io.github.lenticualrstone.civilight.feature.library.settings.ViewKind.CARD, services)
    }
    entry<SettingsHiding> { HidingSettingsScreen(services) }
    entry<SettingsTextSize> { TextSizeScreen(services) }
    // (0.2.3, r3) `펼친 화면 배치` is gone. The route stays so a saved back stack still restores; it steps back at once.
    entry<SettingsLayout> { androidx.compose.runtime.LaunchedEffect(Unit) { deps.navigator.pop() } }
    entry<SettingsMotion> { MotionSettingsScreen(services) }
    entry<SettingsMore> { MoreSettingsScreen(services) }
    entry<SettingsRating> { RatingSettingsScreen(services) }
    entry<RatingDefaultsApply> { route -> RatingDefaultsApplyScreen(route, services) }
    entry<ProjectSettings> { route -> ProjectSettingsScreen(route, services) }
    entry<StructureLevels> { route -> StructureLevelsScreen(route, services) }
    entry<Trash> { route -> TrashScreen(route, services) }
    entry<OpenFile> { route -> OpenFileScreen(route, services) }
    entry<Onboarding> { route -> OnboardingScreen(route, services) }
    entry<Walkthrough> { route -> WalkthroughScreen(route, services) }
    entry<ExampleTour> { route -> ExampleTourScreen(route, services) }
    entry<HelpReplay> { HelpReplayScreen(services) }
    entry<SettingsBackup> { BackupSettingsScreen(services) }
    entry<RecoveryPassword> { RecoveryPasswordScreen(services) }
    entry<RestoreBackup> { RestoreBackupScreen(services) }
    entry<Restoring> { RestoringScreen(services) }
    deps.sheets.register<RatingChangeSheet> { sheet -> RatingChangeContent(sheet, services) }
    deps.sheets.register<ExportSheet> { sheet -> ExportSheetContent(sheet, services) }
    // LIB-57 `ED` until WorkManager is a dependency: the daily backup runs at start when it is due.
    deps.startup.register(BACKUP_HOOK) { d ->
        d.appScope.launch { runCatching { services.backups.runIfDue(services.writtenBy()) } }
        null
    }
}

private const val BACKUP_HOOK = "library.backup"
