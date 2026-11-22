@file:JvmName("ScriptEntries")

package io.github.lenticualrstone.civilight.feature.script

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.script.renumber.RenumberSheetContent
import io.github.lenticualrstone.civilight.feature.script.scenes.SceneMergeSheetContent
import io.github.lenticualrstone.civilight.feature.script.scenes.SceneSplitSheetContent
import io.github.lenticualrstone.civilight.feature.script.settings.ScriptFieldsScreen
import io.github.lenticualrstone.civilight.navigation.RenumberSheet
import io.github.lenticualrstone.civilight.navigation.SceneMergeSheet
import io.github.lenticualrstone.civilight.navigation.SceneSplitSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.ScriptAssets
import io.github.lenticualrstone.civilight.feature.script.assets.AssetsScreen
import io.github.lenticualrstone.civilight.navigation.ScriptFieldSettings
import io.github.lenticualrstone.civilight.navigation.TransitionTextSettings
import io.github.lenticualrstone.civilight.feature.script.settings.TransitionSettingsScreen
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * The script area's entries (shell.md §1). Registered: `Script` (SCR-01 …), `ScriptFieldSettings`
 * (SCR-24/25), `TransitionTextSettings` (SCR-48), `ScriptAssets` (SCR-61) and the shared sheets
 * `RenumberSheet` (SCR-22), `SceneSplitSheet` (SCR-50) and `SceneMergeSheet` (SCR-51). Every script
 * route is registered.
 */
fun EntryProviderScope<NavKey>.scriptEntries(deps: FeatureDeps) {
    entry<Script> { route -> ScriptScreen(route, deps) }
    entry<ScriptFieldSettings> { route -> ScriptFieldsScreen(route, deps) }
    entry<TransitionTextSettings> { route -> TransitionSettingsScreen(route, deps) }
    entry<ScriptAssets> { route -> AssetsScreen(route, deps) }
    deps.sheets.register<RenumberSheet> { sheet -> RenumberSheetContent(sheet, deps) }
    deps.sheets.register<SceneSplitSheet> { sheet -> SceneSplitSheetContent(sheet, deps) }
    deps.sheets.register<SceneMergeSheet> { sheet -> SceneMergeSheetContent(sheet, deps) }
}
