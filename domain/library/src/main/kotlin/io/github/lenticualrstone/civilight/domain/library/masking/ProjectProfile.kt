package io.github.lenticualrstone.civilight.domain.library.masking

import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating

/** What masking needs to know about a project (name, rating and the four protections). */
data class ProjectProfile(
    val key: Key,
    val name: String,
    val rating: Rating,
    val protections: Protections,
    val trashed: Boolean = false,
    val isExample: Boolean = false,
) {
    val hidden: Boolean get() = protections.hidden

    companion object {
        fun of(project: Project, trashed: Boolean = false, isExample: Boolean = false) = ProjectProfile(
            project.key, project.name, RatingLabels.effective(project.rating), project.protections, trashed, isExample,
        )

        fun of(doc: ProjectDoc, trashed: Boolean = false, isExample: Boolean = false) = of(doc.project, trashed, isExample)
    }
}

/**
 * Runtime unlock state of hidden things (LIB-32, LIB-33, LIB-36): the core [session] plus the
 * projects hidden during this session, which stay open until the next re-lock (R19 §2 "지금은 열린 채").
 * Never persisted.
 */
data class HiddenAccess(
    val session: UnlockSession = UnlockSession(),
    val keptOpen: Set<Key> = emptySet(),
) {
    val groupOpen: Boolean get() = session.hiddenGroupUnlockedAt != null

    /** Content of the hidden project [key] may be shown. */
    fun isProjectOpen(key: Key): Boolean = groupOpen || key in keptOpen

    /** Hidden items of project [key] are unlocked (separate per project, LIB-36). */
    fun itemsOpen(projectKey: Key): Boolean = projectKey in session.itemsUnlocked

    fun openGroup(now: Long): HiddenAccess = copy(session = session.copy(hiddenGroupUnlockedAt = now))

    fun openItems(projectKey: Key, now: Long): HiddenAccess =
        copy(session = session.copy(itemsUnlocked = session.itemsUnlocked + (projectKey to now)))

    /** A project just hidden stays open until the next re-lock. */
    fun keepOpen(projectKey: Key): HiddenAccess = copy(keptOpen = keptOpen + projectKey)

    /** Re-lock (LIB-33): closes the group, kept-open projects and the hidden items of every project. */
    fun relock(): HiddenAccess = HiddenAccess(session.copy(hiddenGroupUnlockedAt = null, itemsUnlocked = emptyMap()), emptySet())

    companion object {
        val LOCKED = HiddenAccess()
    }
}
