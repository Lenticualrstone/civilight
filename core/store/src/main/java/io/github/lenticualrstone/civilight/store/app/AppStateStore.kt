package io.github.lenticualrstone.civilight.store.app

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.store.StoreEnvironment
import io.github.lenticualrstone.civilight.store.io.JsonFile
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import kotlinx.serialization.Serializable
import java.io.File

/** `files/app/recents.json`. */
@Serializable
data class RecentsFile(val items: List<RecentItem> = emptyList())

/**
 * App-level state that is neither settings nor project content (class A, never exported):
 * onboarding progress (LIB-15…18) and recently opened items (LIB-05), in `files/app/`.
 */
class AppStateStore(dir: File, env: StoreEnvironment) {
    private val directory = StoreDirectory(dir)

    val onboarding = JsonFile(directory, "onboarding.json", OnboardingState.serializer(), OnboardingState(), env)
    val recents = JsonFile(directory, "recents.json", RecentsFile.serializer(), RecentsFile(), env)

    suspend fun ensureLoaded() {
        onboarding.ensureLoaded()
        recents.ensureLoaded()
    }

    /** Puts [item] first; the same target opened before moves up; at most [RecentItem.MAX] kept. */
    suspend fun recordRecent(item: RecentItem) {
        recents.update { file ->
            val rest = file.items.filterNot { it.projectKey == item.projectKey && it.kind == item.kind && it.targetKey == item.targetKey }
            file.copy(items = (listOf(item) + rest).take(RecentItem.MAX))
        }
    }

    /** Drops recents of a project (trashed, hidden, purged: LIB-05 masking). */
    suspend fun removeRecentsOf(projectKey: Key) {
        recents.update { file -> file.copy(items = file.items.filterNot { it.projectKey == projectKey }) }
    }

    suspend fun flush() {
        onboarding.flush()
        recents.flush()
    }
}
