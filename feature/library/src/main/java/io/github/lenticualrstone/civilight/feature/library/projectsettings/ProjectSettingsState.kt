package io.github.lenticualrstone.civilight.feature.library.projectsettings

import io.github.lenticualrstone.civilight.domain.library.newproject.CoverSummary
import io.github.lenticualrstone.civilight.domain.library.rating.ProjectProtectionSettings
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * 프로젝트 설정 as sections (LIB-69, board `ProjSettings`): `구조`, `인물`, `등급 · 숨기기` (LIB-25)
 * and `스크립트 추가 칸`. Rows other areas specify open their routes; this screen only shows them.
 */
data class ProjectSettingsUi(
    val projectName: String,
    /** Read-only example project: every row disabled (LIB-69). */
    val readOnly: Boolean,
    val structure: List<ProjectLinkRow>,
    val cast: List<ProjectLinkRow>,
    val protection: ProjectProtectionSettings,
    val script: ScriptSectionUi,
    /** `표지` (U-08 LIB-82), the first section. */
    val cover: CoverSettingsRow = CoverSettingsRow(ProjectSettingsCopy.COVER, ProjectSettingsCopy.COVER_SUB, CoverSummary.NONE),
)

object ProjectSettingsState {
    fun build(
        doc: ProjectDoc,
        readOnly: Boolean,
        defaults: RatingDefaults,
        fileExists: (String) -> Boolean = { true },
    ): ProjectSettingsUi = ProjectSettingsUi(
        projectName = doc.project.name,
        readOnly = readOnly,
        structure = ProjectSettingsRows.structure(doc),
        cast = ProjectSettingsRows.cast(doc),
        protection = ProjectProtectionSettings.of(RatingLabels.effective(doc.project.rating), doc.project.protections, defaults),
        script = ProjectSettingsRows.script(doc),
        cover = ProjectSettingsRows.cover(doc, fileExists),
    )
}
