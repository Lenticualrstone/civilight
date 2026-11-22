package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Injected sources for edits that mint keys or stamp rows (`createdAt`, `updatedAt`, `updatedBy`).
 * Mirrors `domain:story`'s `EditContext`, which this module cannot depend on.
 */
data class RecorderEditContext(
    val newKey: () -> Key = Keys::newKey,
    val now: Timestamp,
    val device: String,
    val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) {
    val today: LocalDate get() = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
}

/** One project as the recorder sees it: its 서재 row and its document. */
data class ProjectSnapshot(val entry: LibraryEntry, val doc: ProjectDoc) {
    val key: Key get() = doc.project.key
    val name: String get() = doc.project.name

    /** An unknown stored rating reads as the strictest known one. */
    val rating: Rating get() = doc.project.rating.known ?: Rating.R18
    val hidden: Boolean get() = doc.project.protections.hidden || entry.hidden
    val linkMask: Boolean get() = doc.project.protections.recorderLinkMask
    val captureBlock: Boolean get() = doc.project.protections.captureBlock
    val trashed: Boolean get() = entry.deletedAt != null
    val isExample: Boolean get() = entry.isExample
    val readOnly: Boolean get() = entry.readOnly
    val format: ProjectFormat? get() = doc.project.format.known

    companion object {
        /** A snapshot whose 서재 row is derived from the document (not the example, not trashed). */
        fun of(doc: ProjectDoc): ProjectSnapshot = ProjectSnapshot(
            LibraryEntry(
                key = doc.key,
                name = doc.project.name,
                rating = doc.project.rating,
                hidden = doc.project.protections.hidden,
                createdAt = doc.project.createdAt,
                updatedAt = doc.project.updatedAt,
            ),
            doc,
        )
    }
}

/**
 * What recorder derivations need besides the library: the projects of the 서재 (live and trashed,
 * in 서재 order), the unlock state, the zone for date labels and the most recently opened project
 * (search order). A project that is not listed is permanently deleted (§6: resolution = the
 * project row with that key).
 */
data class RecorderEnv(
    val projects: List<ProjectSnapshot>,
    val unlock: UnlockSession = UnlockSession(),
    val zone: ZoneId = ZoneId.of("Asia/Seoul"),
    val recentProjectKey: Key? = null,
) {
    private val byKey: Map<Key, ProjectSnapshot> by lazy { projects.associateBy { it.key } }

    fun project(key: Key): ProjectSnapshot? = byKey[key]

    /** Hidden records are open (`지문으로 보기` succeeded and no re-lock since). */
    val recordsOpen: Boolean get() = unlock.recordsUnlockedAt != null

    /** The hidden-project group is open. */
    val hiddenGroupOpen: Boolean get() = unlock.hiddenGroupUnlockedAt != null

    /** Hidden items of [projectKey] are open (a separate gate per project). */
    fun itemsOpen(projectKey: Key): Boolean = projectKey in unlock.itemsUnlocked

    /** A hidden project whose gate is locked: its name and contents are never shown. */
    fun isLockedHidden(project: ProjectSnapshot): Boolean = project.hidden && !hiddenGroupOpen

    /** The same env with [snapshot] replacing the project of the same key (or appended). */
    fun withProject(snapshot: ProjectSnapshot): RecorderEnv =
        if (byKey.containsKey(snapshot.key)) copy(projects = projects.map { if (it.key == snapshot.key) snapshot else it })
        else copy(projects = projects + snapshot)

    /** The same env with [doc] as the document of its project. */
    fun withDoc(doc: ProjectDoc): RecorderEnv =
        withProject(project(doc.key)?.copy(doc = doc) ?: ProjectSnapshot.of(doc))

    /** The same env without the project [key] (it was permanently deleted). */
    fun withoutProject(key: Key): RecorderEnv = copy(projects = projects.filter { it.key != key })
}
