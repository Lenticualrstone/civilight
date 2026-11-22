@file:JvmName("CastEntries")

package io.github.lenticualrstone.civilight.feature.cast

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.cast.field.FieldHistorySheetContent
import io.github.lenticualrstone.civilight.feature.cast.field.FieldValueSheetContent
import io.github.lenticualrstone.civilight.feature.cast.list.CastListScreen
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileScreen
import io.github.lenticualrstone.civilight.feature.cast.settings.AddFieldSheetContent
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldSettingsScreen
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.FieldHistorySheet
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.FieldSettings as FieldSettingsRoute
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * Entries of `:feature:cast` (shell.md §1). Routes: the `cast` row of shell.md §3; shared sheets:
 * those owned by CAST in shell.md §5. Only registrations happen here.
 *
 * Registered so far (batch 1): `CastList`, `CharacterProfile`, `FieldSettings`; sheets
 * `FieldValueEditSheet`, `FieldHistorySheet`, `AddFieldSheet`. Not yet (later batches, they show the
 * shell's placeholder): `RelationMap`, `FieldDefEdit`, `RelationKindSettings`, `TagSettings`,
 * `ArchiveDocEdit`, `RelationEditSheet`, `DeleteImpactSheet`, `MergeItemsSheet`.
 */
fun EntryProviderScope<NavKey>.castEntries(deps: FeatureDeps) {
    entry<CastList> { route -> CastListScreen(route, deps) }
    entry<CharacterProfile> { route -> ProfileScreen(route, deps) }
    entry<FieldSettingsRoute> { route -> FieldSettingsScreen(route, deps) }

    deps.sheets.register<FieldValueEditSheet> { sheet -> FieldValueSheetContent(sheet) }
    deps.sheets.register<FieldHistorySheet> { sheet -> FieldHistorySheetContent(sheet) }
    deps.sheets.register<AddFieldSheet> { sheet -> AddFieldSheetContent(sheet) }
}
