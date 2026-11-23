package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.github.lenticualrstone.civilight.model.Key

/**
 * View model stores of the cover picker opened over the 서재 (U-08 LIB-81), one per project.
 *
 * The picker (`feature:canvas` `ImagePickSheet`) keeps its view model, and with it the project's open
 * session, in the hosting entry, so its `표지 뺌 · 되돌리기` toast still works after the sheet closed. The
 * 서재 entry lives as long as the app, so the picker's view model would hold the session for good (and a
 * project moved to the trash would keep a closed store). Here each project gets its own store, cleared as
 * soon as the 서재 no longer shows that project (moved to the trash, its hidden group locked) and when
 * the 서재's view model is cleared. Not thread-safe: called from the main thread only.
 */
internal class CoverPickerStores {
    private val owners = HashMap<Key, Owner>()

    private class Owner : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }

    /** The store of [projectKey]'s picker, made on first use. */
    fun ownerFor(projectKey: Key): ViewModelStoreOwner = owners.getOrPut(projectKey) { Owner() }

    /** The projects that currently hold a store (for tests). */
    val held: Set<Key> get() = owners.keys.toSet()

    /**
     * Lets [projectKey]'s picker go (its view model and session). Called before each opening from the
     * 서재, so a picker never reuses a session the library closed meanwhile (rename, restore, file replace).
     */
    fun release(projectKey: Key) {
        owners.remove(projectKey)?.viewModelStore?.clear()
    }

    /** Clears the stores of every project not in [shown]. */
    fun retain(shown: Set<Key>) {
        val gone = owners.keys.filterNot { it in shown }
        gone.forEach { key -> owners.remove(key)?.viewModelStore?.clear() }
    }

    fun clearAll() {
        owners.values.forEach { it.viewModelStore.clear() }
        owners.clear()
    }
}
