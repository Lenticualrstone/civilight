package io.github.lenticualrstone.civilight.ui.feature

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.navigation.FeatureModule
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Calls each feature module's entries function by its fixed JVM name ([FeatureContract]).
 *
 * - Class absent (the feature is not written yet): skipped; its routes fall back to the placeholder.
 * - Class present but no `static <name>(EntryProviderScope, FeatureDeps)`: throws, so a feature
 *   never silently stays on placeholders.
 * - The function itself throws: the exception is rethrown unwrapped.
 */
object FeatureLoader {
    /** Result per module: true when its entries function was called. */
    fun registerAll(
        scope: EntryProviderScope<NavKey>,
        deps: FeatureDeps,
        bindings: List<FeatureBinding> = FeatureContract.bindings,
        log: (String) -> Unit = {},
    ): Map<FeatureModule, Boolean> = bindings.associate { it.module to register(it, scope, deps, log) }

    private fun register(binding: FeatureBinding, scope: EntryProviderScope<NavKey>, deps: FeatureDeps, log: (String) -> Unit): Boolean {
        val type = try {
            Class.forName(binding.className)
        } catch (e: ClassNotFoundException) {
            log("${binding.module.gradlePath}: ${binding.className} not found, placeholders are shown")
            return false
        }
        val method = entriesMethod(type, binding.functionName) ?: throw IllegalStateException(
            "${binding.className} must declare `fun EntryProviderScope<NavKey>.${binding.functionName}(deps: FeatureDeps)` " +
                "in a file annotated @file:JvmName(\"${binding.className.substringAfterLast('.')}\")",
        )
        try {
            method.invoke(null, scope, deps)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
        return true
    }

    private fun entriesMethod(type: Class<*>, name: String): Method? = type.methods.firstOrNull { m ->
        m.name == name &&
            Modifier.isStatic(m.modifiers) &&
            m.parameterTypes.size == 2 &&
            m.parameterTypes[0].isAssignableFrom(EntryProviderScope::class.java) &&
            m.parameterTypes[1].isAssignableFrom(FeatureDeps::class.java)
    }
}
