package io.github.lenticualrstone.civilight.feature.cast.templates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.launch

/** What a template screen reads: every template (live and trashed) and whether the hidden group is open. */
data class TemplatesNow(val all: List<FieldTemplate>, val hiddenUnlocked: Boolean, val loaded: Boolean = true)

/** The app-level templates (CST-63) and the hidden group state, loaded on first use. */
@Composable
internal fun rememberTemplatesNow(deps: FeatureDeps = LocalFeatureDeps.current): TemplatesNow {
    val file by deps.stores.fieldTemplates.state.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        deps.stores.fieldTemplates.ensureLoaded()
        loaded = true
    }
    return remember(file, unlock.hiddenGroupOpen, loaded) { TemplatesNow(file.templates, unlock.hiddenGroupOpen, loaded) }
}

/**
 * Writes to the template store. Each runs in the app scope, so it lands even when the screen
 * closes; nothing goes into a project's undo stack (CST-57, CST-59).
 */
internal class TemplateWrites(private val deps: FeatureDeps) {
    private val store get() = deps.stores.fieldTemplates

    fun put(template: FieldTemplate, then: () -> Unit = {}) {
        deps.appScope.launch {
            store.ensureLoaded()
            store.put(template)
            then()
        }
    }

    fun edit(key: Key, change: (FieldTemplate) -> FieldTemplate) {
        deps.appScope.launch {
            store.ensureLoaded()
            store.edit(key, change)
        }
    }
}
