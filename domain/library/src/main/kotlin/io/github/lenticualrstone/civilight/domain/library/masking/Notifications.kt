package io.github.lenticualrstone.civilight.domain.library.masking

import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels

/**
 * Notification texts of this area (LIB-30): never a hidden project's name; hidden projects are
 * counted as `숨긴 프로젝트 N`; name-masked ones appear as `R-18 프로젝트`.
 */
object Notifications {
    /**
     * Backup done (`ED` rule reproducing both R19 §3 examples, §6 Q17): with any hidden project in
     * the run → `백업 끝 · 프로젝트 <n> · 숨긴 프로젝트 <h>`; otherwise the names joined by ` · `.
     */
    fun backupDone(projects: List<ProjectProfile>): String {
        val hidden = projects.count { it.hidden }
        if (hidden > 0) return "백업 끝 · 프로젝트 ${projects.size - hidden} · 숨긴 프로젝트 $hidden"
        val names = projects.map { if (it.protections.nameMask) RatingLabels.MASKED_PROJECT else it.name }
        return (listOf("백업 끝") + names).joinToString(" · ")
    }

    /** The masked token of one project for restore/export done messages (copy beyond the token: not in sources). */
    fun projectToken(project: ProjectProfile): String = when {
        project.hidden -> "${ProjectSurfaces.HIDDEN_PROJECT} 1"
        project.protections.nameMask -> RatingLabels.MASKED_PROJECT
        else -> project.name
    }
}
