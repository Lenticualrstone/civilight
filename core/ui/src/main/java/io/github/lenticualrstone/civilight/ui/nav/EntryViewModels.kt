package io.github.lenticualrstone.civilight.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntryDecorator

/** One [ViewModelStore] per back-stack entry, kept across configuration changes in the activity's store. */
class EntryViewModelStores : ViewModel() {
    private val stores = HashMap<Any, ViewModelStore>()

    fun storeFor(contentKey: Any): ViewModelStore = stores.getOrPut(contentKey) { ViewModelStore() }

    /** The entry left the back stack: its view models are cleared. */
    fun clear(contentKey: Any) {
        stores.remove(contentKey)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}

/**
 * Gives every entry its own [ViewModelStoreOwner], so `viewModel()` inside an entry is scoped to that
 * entry (two screens of one route type never share a view model) and cleared when it is popped.
 * Navigation3 1.1 has no such decorator without `lifecycle-viewmodel-navigation3`. View models get no
 * `SavedStateHandle`: restorable screen state goes into the route and `rememberSaveable`.
 */
@Composable
fun <T : Any> rememberEntryViewModelDecorator(): NavEntryDecorator<T> {
    val activityOwner = checkNotNull(LocalViewModelStoreOwner.current) { "no ViewModelStoreOwner" }
    val holder = viewModel<EntryViewModelStores>(viewModelStoreOwner = activityOwner)
    return remember(holder) {
        NavEntryDecorator<T>(
            onPop = { contentKey -> holder.clear(contentKey) },
            decorate = { entry ->
                val store = holder.storeFor(entry.contentKey)
                val owner = remember(store) {
                    object : ViewModelStoreOwner {
                        override val viewModelStore: ViewModelStore = store
                    }
                }
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { entry.Content() }
            },
        )
    }
}
