@file:JvmName("CastEntries")

package io.github.lenticualrstone.civilight.feature.cast

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.cast.field.FieldHistorySheetContent
import io.github.lenticualrstone.civilight.feature.cast.field.FieldValueSheetContent
import io.github.lenticualrstone.civilight.feature.cast.gametext.ArchiveDocScreen
import io.github.lenticualrstone.civilight.feature.cast.list.CastListScreen
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileScreen
import io.github.lenticualrstone.civilight.feature.cast.relation.RelationEditContent
import io.github.lenticualrstone.civilight.feature.cast.trash.DeleteImpactContent
import io.github.lenticualrstone.civilight.feature.cast.settings.AddFieldSheetContent
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldSettingsScreen
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.ArchiveDocEdit
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.DeleteImpactSheet
import io.github.lenticualrstone.civilight.navigation.MergeItemsSheet
import io.github.lenticualrstone.civilight.feature.cast.merge.MergeItemsContent
import io.github.lenticualrstone.civilight.navigation.RelationEditSheet
import io.github.lenticualrstone.civilight.navigation.FieldHistorySheet
import io.github.lenticualrstone.civilight.navigation.FieldDefEdit
import io.github.lenticualrstone.civilight.navigation.RelationKindSettings
import io.github.lenticualrstone.civilight.navigation.RelationMap
import io.github.lenticualrstone.civilight.navigation.TagSettings
import io.github.lenticualrstone.civilight.feature.cast.settings.TagSettingsScreen
import io.github.lenticualrstone.civilight.feature.cast.map.RelationMapScreen
import io.github.lenticualrstone.civilight.feature.cast.kinds.RelationKindSettingsScreen
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldDefEditScreen
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.FieldSettings as FieldSettingsRoute
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.navigation.TemplateEdit
import io.github.lenticualrstone.civilight.navigation.TemplateFieldEdit
import io.github.lenticualrstone.civilight.navigation.TemplateList
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateEditScreen
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateFieldEditScreen
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateListScreen

/**
 * Entries of `:feature:cast` (shell.md §1). Routes: the `cast` row of shell.md §3; shared sheets:
 * those owned by CAST in shell.md §5. Only registrations happen here.
 *
 * Registered: every route of the `cast` row (`CastList`, `CharacterProfile`, `FieldSettings`,
 * `ArchiveDocEdit`, `FieldDefEdit`, `RelationKindSettings`, `RelationMap`, `TagSettings`, and the app-level
 * 인물 템플릿 routes `TemplateList`, `TemplateEdit`, `TemplateFieldEdit`) and every
 * CAST sheet (`FieldValueEditSheet`, `FieldHistorySheet`, `AddFieldSheet`, `RelationEditSheet`,
 * `DeleteImpactSheet`, `MergeItemsSheet`).
 */
fun EntryProviderScope<NavKey>.castEntries(deps: FeatureDeps) {
    entry<CastList> { route -> CastListScreen(route, deps) }
    entry<CharacterProfile> { route -> ProfileScreen(route, deps) }
    entry<FieldSettingsRoute> { route -> FieldSettingsScreen(route, deps) }
    entry<ArchiveDocEdit> { route -> ArchiveDocScreen(route, deps) }
    entry<FieldDefEdit> { route -> FieldDefEditScreen(route, deps) }
    entry<RelationKindSettings> { route -> RelationKindSettingsScreen(route, deps) }
    entry<RelationMap> { route -> RelationMapScreen(route, deps) }
    entry<TagSettings> { route -> TagSettingsScreen(route, deps) }
    entry<TemplateList> { route -> TemplateListScreen(route, deps) }
    entry<TemplateEdit> { route -> TemplateEditScreen(route, deps) }
    entry<TemplateFieldEdit> { route -> TemplateFieldEditScreen(route, deps) }

    deps.sheets.register<FieldValueEditSheet> { sheet -> FieldValueSheetContent(sheet) }
    deps.sheets.register<FieldHistorySheet> { sheet -> FieldHistorySheetContent(sheet) }
    deps.sheets.register<AddFieldSheet> { sheet -> AddFieldSheetContent(sheet) }
    deps.sheets.register<RelationEditSheet> { sheet -> RelationEditContent(sheet) }
    deps.sheets.register<DeleteImpactSheet> { sheet -> DeleteImpactContent(sheet) }
    deps.sheets.register<MergeItemsSheet> { sheet -> MergeItemsContent(sheet) }
}
