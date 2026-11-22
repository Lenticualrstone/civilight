package io.github.lenticualrstone.civilight.feature.recorder.capture

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.security.WindowProtection

/**
 * Window protection of the capture surfaces (rule 19 §3, LIB-26): the capture shows record text
 * next to its links and the entry project, so the protections of those projects apply while it is
 * open (FLAG_SECURE for `화면 캡처 가리기`, the masked recents preview for hidden projects). The link
 * picker lists names of the projects it offers, so their protections apply while it is open.
 */
object CaptureProtection {
    /** Projects whose content a capture in [state] shows: the entry project and every link's project. */
    fun projectsShown(state: CaptureState): Set<Key> {
        val links = state.session?.pendingLinks ?: state.memo?.draft?.pendingLinks.orEmpty()
        return (links.map { it.projectKey } + listOfNotNull(state.entry?.projectKey)).toSet()
    }

    /** The strongest protection of [projects] (unknown or purged projects add nothing). */
    fun of(env: RecorderEnv, projects: Collection<Key>): WindowProtection =
        projects.mapNotNull(env::project).fold(WindowProtection()) { acc, p ->
            acc + WindowProtection.forProject(p.doc.project.protections.copy(hidden = p.hidden))
        }
}
