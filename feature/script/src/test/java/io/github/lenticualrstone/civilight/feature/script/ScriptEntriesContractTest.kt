package io.github.lenticualrstone.civilight.feature.script

import androidx.navigation3.runtime.EntryProviderScope
import io.github.lenticualrstone.civilight.navigation.FeatureModule
import io.github.lenticualrstone.civilight.ui.feature.FeatureContract
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.lang.reflect.Modifier

/** shell.md §1: the app finds `scriptEntries` by reflection, by these exact names. */
class ScriptEntriesContractTest {
    @Test
    fun `the entries function has the JVM name and signature the loader looks for`() {
        val binding = FeatureContract.bindings.single { it.module == FeatureModule.SCRIPT }
        val type = Class.forName(binding.className)
        val method = type.methods.firstOrNull { m ->
            m.name == binding.functionName &&
                Modifier.isStatic(m.modifiers) &&
                m.parameterTypes.size == 2 &&
                m.parameterTypes[0].isAssignableFrom(EntryProviderScope::class.java) &&
                m.parameterTypes[1].isAssignableFrom(FeatureDeps::class.java)
        }
        assertNotNull("${binding.className}.${binding.functionName}(EntryProviderScope, FeatureDeps)", method)
    }
}
