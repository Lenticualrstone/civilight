@file:JvmName("ScriptEntries")

package io.github.lenticualrstone.civilight.feature.script

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/**
 * The script area's entries (shell.md §1). Registered now: `Script` (SCR-01 …). Not yet registered
 * (the shell shows its placeholder): `ScriptAssets` (SCR-61), `ScriptFieldSettings` (SCR-24/25),
 * `TransitionTextSettings` (SCR-48), and the shared sheets `SceneSplitSheet` (SCR-50) and
 * `SceneMergeSheet` (SCR-51).
 */
fun EntryProviderScope<NavKey>.scriptEntries(deps: FeatureDeps) {
    entry<Script> { route -> ScriptScreen(route, deps) }
}
