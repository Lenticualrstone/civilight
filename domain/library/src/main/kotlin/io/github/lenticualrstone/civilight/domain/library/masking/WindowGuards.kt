package io.github.lenticualrstone.civilight.domain.library.masking

import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels

/**
 * Something of a project currently on screen. [maskedOnly] = only a masked presentation such as the
 * `R-18 프로젝트` label, which shows no content.
 */
data class VisibleContent(val project: ProjectProfile, val maskedOnly: Boolean = false)

/** Window state derived from what is on screen (LIB-26, LIB-29). */
data class WindowGuard(
    /** `FLAG_SECURE`: screenshots, recording and the recents preview are blocked. */
    val flagSecure: Boolean,
    /** Recents preview masked even when screenshots are allowed (API 33+ `setRecentsScreenshotEnabled(false)`). */
    val recentsPreviewMasked: Boolean,
    /** Task label for the recents card; null = the default app label. */
    val taskLabel: String?,
)

object WindowGuards {
    /**
     * Capture blocking applies whenever any content of a capture-blocked project is on screen,
     * inside the project or not (open hidden group row, search results, 기록함 rows, recents cards,
     * the export sheet). Masked-only presentations do not trigger it (`ED`).
     */
    fun captureGuard(visible: List<VisibleContent>): Boolean =
        visible.any { !it.maskedOnly && it.project.protections.captureBlock }

    /**
     * Full guard (LIB-29): hidden-project content masks the recents preview and names the card
     * `Civilight`; content of a non-hidden name-masked project names it `R-18 프로젝트`.
     */
    fun of(visible: List<VisibleContent>): WindowGuard {
        val content = visible.filterNot { it.maskedOnly }
        val hiddenOnScreen = content.any { it.project.hidden }
        val label = when {
            hiddenOnScreen -> ProjectSurfaces.APP_LABEL
            content.any { it.project.protections.nameMask } -> RatingLabels.MASKED_PROJECT
            else -> null
        }
        val secure = captureGuard(visible)
        return WindowGuard(flagSecure = secure, recentsPreviewMasked = secure || hiddenOnScreen, taskLabel = label)
    }
}
