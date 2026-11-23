package io.github.lenticualrstone.civilight.feature.library.projectsettings

import io.github.lenticualrstone.civilight.domain.library.newproject.CoverSummary
import io.github.lenticualrstone.civilight.domain.library.rating.ProjectProtectionSettings
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * 프로젝트 설정 as sections (LIB-69, board `ProjSettings`): `표지`, `구조`, `인물`, (0.2.0) `세계`, `숨기기`
 * (LIB-25, LIB-92) and `스크립트 추가 칸` (LIB-91). Rows other areas specify open their routes; this screen only shows them.
 */
data class ProjectSettingsUi(
    val projectName: String,
    /** Read-only example project: every row disabled (LIB-69). */
    val readOnly: Boolean,
    val structure: List<ProjectLinkRow>,
    val cast: List<ProjectLinkRow>,
    val protection: ProjectProtectionSettings,
    val script: ScriptSectionUi,
    /** (0.2.0, WLD-73) Section `세계`: `용어 후보`. */
    val world: List<ProjectSwitchRow> = emptyList(),
    /** `표지` (U-08 LIB-82), the first section. */
    val cover: CoverSettingsRow = CoverSettingsRow(ProjectSettingsCopy.COVER, ProjectSettingsCopy.COVER_SUB, CoverSummary.NONE),
)

object ProjectSettingsState {
    fun build(
        doc: ProjectDoc,
        readOnly: Boolean,
        @Suppress("UNUSED_PARAMETER") defaults: RatingDefaults = RatingDefaults(),
        fileExists: (String) -> Boolean = { true },
    ): ProjectSettingsUi = ProjectSettingsUi(
        projectName = doc.project.name,
        readOnly = readOnly,
        structure = ProjectSettingsRows.structure(doc),
        cast = ProjectSettingsRows.cast(doc),
        // (0.2.0, LIB-92) No rating row and no `<rating> 기본과 다름`; the stored rating is kept (D34).
        protection = ProjectProtectionSettings.withoutRating(RatingLabels.effective(doc.project.rating), doc.project.protections),
        script = ProjectSettingsRows.script(doc),
        world = ProjectSettingsRows.world(doc),
        cover = ProjectSettingsRows.cover(doc, fileExists),
    )
}
