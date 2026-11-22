package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.pointsAt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

/**
 * `레코더 · 정리 안 됨 n ›` on `개요` (CST-49, LRC-24): records linked to this character that are still
 * `정리 안 됨` (`domain:recorder` [RecorderHooks.unsortedLinkedCount]); locked hidden records are not
 * counted. A record's state depends on where its copies went, so the environment holds every project
 * those records were put into (a project missing from it would count as permanently deleted).
 */
object RecorderLinkModel {
    /** The `target` string of `RecorderInbox` / `Recording` for a character (recorder lane's names). */
    const val TARGET_KIND = "character"

    fun target(projectKey: Key, characterKey: Key): TargetRef = TargetRef(projectKey, RecorderTargetKind.CHARACTER, characterKey)

    /** Projects whose documents the count needs: this one, plus every project a linked record was put into. */
    fun projectsNeeded(library: RecorderLibrary, projectKey: Key, characterKey: Key): Set<Key> {
        val target = target(projectKey, characterKey)
        val records = library.links.filter { it.pointsAt(target) }.map { it.recordKey }.toSet()
        return setOf(projectKey) + library.placements.filter { it.recordKey in records }.map { it.projectKey }
    }

    fun env(entries: List<LibraryEntry>, docs: Map<Key, ProjectDoc>, unlock: UnlockState, zone: ZoneId): RecorderEnv = RecorderEnv(
        projects = entries.mapNotNull { e -> docs[e.key]?.let { ProjectSnapshot(e, it) } },
        unlock = UnlockSession(
            appUnlocked = !unlock.appLocked,
            hiddenGroupUnlockedAt = unlock.hiddenGroupUnlockedAt,
            itemsUnlocked = unlock.itemsUnlockedAt,
            recordsUnlockedAt = unlock.recordsUnlockedAt,
        ),
        zone = zone,
    )

    /** The link text, or null when nothing is `정리 안 됨` (nothing is shown). */
    fun line(library: RecorderLibrary, env: RecorderEnv, projectKey: Key, characterKey: Key): String? =
        Profile.recorderLink(RecorderHooks.unsortedLinkedCount(library, target(projectKey, characterKey), env))
}

/** [RecorderLinkModel.line] for the open record, recomputed when the document, records or locks change. */
@Composable
fun rememberRecorderLink(projectKey: Key, characterKey: Key, doc: ProjectDoc): String? {
    val deps = LocalFeatureDeps.current
    val recorder by deps.stores.recorder.state.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    val line by produceState<String?>(null, doc, recorder.library, recorder.loaded, unlock) {
        deps.stores.recorder.ensureLoaded()
        if (!recorder.loaded) return@produceState
        val library = recorder.library
        value = withContext(Dispatchers.Default) {
            deps.stores.library.ensureLoaded()
            val entries = deps.stores.library.state.value.entries
            val docs = RecorderLinkModel.projectsNeeded(library, projectKey, characterKey).mapNotNull { key ->
                val d = if (key == projectKey) doc else deps.projects.peekDoc(key) ?: readOrNull { deps.stores.library.readDoc(key) }
                d?.let { key to it }
            }.toMap()
            RecorderLinkModel.line(library, RecorderLinkModel.env(entries, docs, unlock, deps.clock.zone), projectKey, characterKey)
        }
    }
    return line
}

private suspend fun readOrNull(read: suspend () -> ProjectDoc): ProjectDoc? = try {
    read()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
