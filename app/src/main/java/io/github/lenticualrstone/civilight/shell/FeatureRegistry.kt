package io.github.lenticualrstone.civilight.shell

import android.util.Log
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.navigation.FeatureModule
import io.github.lenticualrstone.civilight.ui.feature.FeatureContract
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.FeatureLoader

/**
 * The eight feature modules' entries, called by their fixed JVM names ([FeatureContract]) through
 * [FeatureLoader]: an absent feature keeps the placeholders, a feature with a wrong signature fails
 * at startup. Once every feature exists this can become eight direct calls; the names stay the same.
 */
object FeatureRegistry {
    fun registerAll(scope: EntryProviderScope<NavKey>, deps: FeatureDeps): Map<FeatureModule, Boolean> =
        FeatureLoader.registerAll(scope, deps, FeatureContract.bindings) { Log.i("Civilight", it) }
}
