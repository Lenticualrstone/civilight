package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.ProjectRoute
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import kotlinx.coroutines.delay

/**
 * Records 최근 연 파일 (LIB-05) when a scene, character, page, term, place or faction screen comes on
 * top of the stack. The project's open document is preferred (a just-created item is in it); while
 * the screen's store is still opening it waits briefly, then falls back to the 서재's stored copy.
 * What is never remembered (example, hidden project, hidden or trashed item) is [RecentsModel.itemOf]'s.
 */
class RecentsRecorder(
    private val ports: Ports,
    private val now: () -> Timestamp,
    private val newKey: () -> Key,
    private val waitTries: Int = WAIT_TRIES,
    private val waitMs: Long = WAIT_MS,
) {
    interface Ports {
        /** The document of an open project store, or null while none is open. */
        fun openDoc(projectKey: Key): ProjectDoc?

        /** The 서재's stored copy of the project (read without opening a store). */
        fun storedDoc(projectKey: Key): ProjectDoc?

        /** True for the read-only example, and for a project the 서재 does not know. */
        fun isExample(projectKey: Key): Boolean

        suspend fun record(item: RecentItem)
    }

    /** Returns the item it recorded, or null. */
    suspend fun onTop(route: NavKey): RecentItem? {
        if (!isTarget(route)) return null
        val projectKey = (route as ProjectRoute).projectKey
        if (ports.isExample(projectKey)) return null
        var doc = ports.openDoc(projectKey)
        var tries = 0
        while (doc == null && tries < waitTries) {
            delay(waitMs)
            doc = ports.openDoc(projectKey)
            tries++
        }
        val item = RecentsModel.itemOf(route, doc ?: ports.storedDoc(projectKey), isExample = false, now = now(), newKey = newKey) ?: return null
        ports.record(item)
        return item
    }

    companion object {
        const val WAIT_TRIES = 20
        const val WAIT_MS = 100L

        fun isTarget(route: NavKey): Boolean =
            route is Script || route is CharacterProfile || route is PageDoc || route is TermCard || route is PlaceCard || route is FactionCard
    }
}
