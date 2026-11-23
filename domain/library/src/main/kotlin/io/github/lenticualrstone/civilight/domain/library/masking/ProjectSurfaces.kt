package io.github.lenticualrstone.civilight.domain.library.masking

import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.app.AppSettings

/** Surfaces outside the project itself (spec §3.1 `Surface`). */
enum class Surface {
    LIBRARY_ROW, RECENTS_LIST, WIDGET_LIST, CROSS_SEARCH, NOTIFICATION, SYSTEM_RECENTS_PREVIEW, SYSTEM_RECENTS_LABEL,
    SCREENSHOT, TRASH_LOCATION, RECORDER_LOCATION,
}

/** How a project appears on a surface (LIB-27 table, R19 §3). */
sealed interface Presentation {
    /** Not listed at all. */
    data object Omitted : Presentation

    /** Not shown by itself; counted (`숨긴 프로젝트 <n>` row or notification count). */
    data object CountedHidden : Presentation

    /** Real name with the rating badge (red for R-18). */
    data class Shown(val name: String, val badge: String, val redBadge: Boolean) : Presentation

    /** A fixed label instead of the name; [needsAuth] when acting on it needs authentication. */
    data class Label(val text: String, val needsAuth: Boolean = false) : Presentation

    /** System recents preview; [recentsOnly] when screenshots are still allowed. */
    data class Preview(val masked: Boolean, val recentsOnly: Boolean = false) : Presentation

    /** Screenshots and screen recording of the project's content. */
    data class Screenshot(val blocked: Boolean) : Presentation
}

/**
 * The single masking function every surface outside the project uses (`displayProjectName`,
 * LIB-27…29). Presentation is computed, never stored, so a protection change shows at once.
 */
object ProjectSurfaces {
    const val HIDDEN_PROJECT = "숨긴 프로젝트"
    const val APP_LABEL = "Civilight"

    fun present(
        surface: Surface,
        project: ProjectProfile,
        access: HiddenAccess = HiddenAccess.LOCKED,
        settings: AppSettings = AppSettings(),
    ): Presentation {
        val p = project.protections
        val shown = Presentation.Shown(project.name, RatingLabels.badge(project.rating), RatingLabels.isRed(project.rating))
        val masked = Presentation.Label(RatingLabels.MASKED_PROJECT)
        return when (surface) {
            Surface.LIBRARY_ROW -> if (p.hidden && !access.groupOpen) Presentation.CountedHidden else shown
            Surface.RECENTS_LIST, Surface.WIDGET_LIST -> when {
                p.hidden -> Presentation.Omitted
                p.nameMask -> masked
                else -> shown
            }
            Surface.CROSS_SEARCH ->
                if (p.hidden && !(access.groupOpen && settings.hiddenInSearchWhileOpen)) Presentation.Omitted else shown
            Surface.NOTIFICATION -> when {
                p.hidden -> Presentation.CountedHidden
                p.nameMask -> masked
                else -> shown
            }
            Surface.SYSTEM_RECENTS_PREVIEW -> when {
                p.captureBlock -> Presentation.Preview(masked = true)
                p.hidden -> Presentation.Preview(masked = true, recentsOnly = true)
                else -> Presentation.Preview(masked = false)
            }
            Surface.SYSTEM_RECENTS_LABEL -> when {
                p.hidden -> Presentation.Label(APP_LABEL)
                p.nameMask -> masked
                else -> Presentation.Label(APP_LABEL)
            }
            Surface.SCREENSHOT -> Presentation.Screenshot(blocked = p.captureBlock)
            Surface.TRASH_LOCATION, Surface.RECORDER_LOCATION ->
                if (p.hidden && !access.isProjectOpen(project.key)) Presentation.Label(HIDDEN_PROJECT, needsAuth = true) else shown
        }
    }

    /** The name text a surface prints, or null when the project is omitted or only counted. */
    fun displayProjectName(surface: Surface, project: ProjectProfile, access: HiddenAccess = HiddenAccess.LOCKED): String? =
        when (val p = present(surface, project, access)) {
            is Presentation.Shown -> p.name
            is Presentation.Label -> p.text
            else -> null
        }

    /**
     * What a Life Recorder link to [project] shows instead of the project or scene name (LIB-28):
     * `숨긴 프로젝트` while the hidden project is locked, the masked label [RatingLabels.MASKED_PROJECT]
     * (`TODO_COPY_LIB_MASKED_NAME`, never a rating word; 0.2.0 LIB-92, S-4) when 라이프 레코더 연결 가리기 is
     * on (also while open), else null (show the name).
     */
    fun recorderLinkLabel(project: ProjectProfile, access: HiddenAccess = HiddenAccess.LOCKED): String? = when {
        project.hidden && !access.isProjectOpen(project.key) -> HIDDEN_PROJECT
        project.protections.recorderLinkMask -> RatingLabels.MASKED_PROJECT
        else -> null
    }
}
