package io.github.lenticualrstone.civilight.feature.library.projectsettings

import io.github.lenticualrstone.civilight.domain.library.rating.ProjectProtectionSettings
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * 프로젝트 설정 as sections (LIB-69). Batch 1 fills `등급 · 숨기기` (LIB-25); later batches add
 * `구조`, `인물`, `스크립트 추가 칸` as further fields here and composables in [ProjectSettingsScreen].
 */
data class ProjectSettingsUi(
    val projectName: String,
    /** Read-only example project: every row disabled (LIB-69). */
    val readOnly: Boolean,
    val protection: ProjectProtectionSettings,
)

object ProjectSettingsState {
    fun build(doc: ProjectDoc, readOnly: Boolean, defaults: RatingDefaults): ProjectSettingsUi = ProjectSettingsUi(
        projectName = doc.project.name,
        readOnly = readOnly,
        protection = ProjectProtectionSettings.of(RatingLabels.effective(doc.project.rating), doc.project.protections, defaults),
    )
}
