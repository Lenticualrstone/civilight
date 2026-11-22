package io.github.lenticualrstone.civilight.feature.library.notices

import io.github.lenticualrstone.civilight.domain.library.masking.Notifications
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectSurfaces
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels

/**
 * Texts of this area's system notifications (LIB-30): backup done, restore done, export done. They
 * never name a hidden project: a run with hidden projects counts them (`숨긴 프로젝트 N`), and a
 * name-masked project reads `R-18 프로젝트`. The backup wording is `domain:library`
 * `Notifications.backupDone` (§6 Q17); restore and export have no source copy beyond the masked
 * tokens, so their lead word is the one-word result (`되살림`, `내보냄`). Posted by the backup and restore work of
 * LIB-57 / LIB-60 (needs `POST_NOTIFICATIONS`, Request).
 */
object LibraryNotices {
    const val RESTORE_DONE = "되살림"
    const val EXPORT_DONE = "내보냄"
    private const val SEP = " · "
    private const val PROJECTS = "프로젝트"

    fun backupDone(projects: List<ProjectProfile>): String = Notifications.backupDone(projects)

    /** `<lead> · 프로젝트 n · 숨긴 프로젝트 h` with any hidden project, else `<lead> · <names>`. */
    fun restoreDone(projects: List<ProjectProfile>): String = listOf(RESTORE_DONE, tokens(projects)).joinToString(SEP)

    /** `<lead> · <name | R-18 프로젝트 | 숨긴 프로젝트 1>`. */
    fun exportDone(project: ProjectProfile): String = listOf(EXPORT_DONE, Notifications.projectToken(project)).joinToString(SEP)

    private fun tokens(projects: List<ProjectProfile>): String {
        val hidden = projects.count { it.hidden }
        if (hidden > 0) return "$PROJECTS ${projects.size - hidden}$SEP${ProjectSurfaces.HIDDEN_PROJECT} $hidden"
        return projects.joinToString(SEP) { if (it.protections.nameMask) RatingLabels.MASKED_PROJECT else it.name }
    }
}
