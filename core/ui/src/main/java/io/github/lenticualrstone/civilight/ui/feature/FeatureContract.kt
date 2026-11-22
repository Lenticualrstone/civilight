package io.github.lenticualrstone.civilight.ui.feature

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.navigation.FeatureModule

/**
 * The entry point every feature module exposes, found by the shell by its fixed JVM name (the app
 * cannot reference feature code at compile time while features are being written):
 *
 * ```kotlin
 * // feature/script/src/main/java/io/github/lenticualrstone/civilight/feature/script/ScriptEntries.kt
 * @file:JvmName("ScriptEntries")
 * package io.github.lenticualrstone.civilight.feature.script
 *
 * fun EntryProviderScope<NavKey>.scriptEntries(deps: FeatureDeps) {
 *     entry<Script> { route -> ScriptScreen(route, deps) }
 *     deps.sheets.register<SceneSplitSheet> { sheet -> SceneSplitSheetContent(sheet) }
 * }
 * ```
 *
 * Each route class may be registered by exactly one feature (Navigation3 rejects duplicates).
 * Routes nobody registered show the shell's placeholder screen.
 */
data class FeatureBinding(val module: FeatureModule, val className: String, val functionName: String)

typealias FeatureEntries = EntryProviderScope<NavKey>.(FeatureDeps) -> Unit

object FeatureContract {
    private const val ROOT = "io.github.lenticualrstone.civilight.feature"

    val bindings: List<FeatureBinding> = listOf(
        binding(FeatureModule.LIBRARY, "library", "Library"),
        binding(FeatureModule.STORY, "story", "Story"),
        binding(FeatureModule.SCRIPT, "script", "Script"),
        binding(FeatureModule.CAST, "cast", "Cast"),
        binding(FeatureModule.WORLD, "world", "World"),
        binding(FeatureModule.MAP, "map", "Map"),
        binding(FeatureModule.CANVAS, "canvas", "Canvas"),
        binding(FeatureModule.RECORDER, "recorder", "Recorder"),
    )

    private fun binding(module: FeatureModule, pkg: String, name: String) =
        FeatureBinding(module, "$ROOT.$pkg.${name}Entries", "${pkg}Entries")
}
