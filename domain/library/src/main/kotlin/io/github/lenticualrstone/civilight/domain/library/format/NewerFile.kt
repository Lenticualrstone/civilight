package io.github.lenticualrstone.civilight.domain.library.format

import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.FileManifest

/**
 * Newer-file detection (LIB-50 promise 4, LIB-53). A file is refused when its `minReaderVersion` is
 * above what this app reads, or its `features.required` names an id this app does not know; the
 * feature list beats the version number. Unknown optional ids never refuse.
 */
data class NewerFileCheck(
    val refused: Boolean,
    /** `minReaderVersion` above the app (or unreadable). */
    val versionTooNew: Boolean,
    val unknownRequired: List<String>,
    /** Parts skipped silently on import. */
    val unknownOptional: List<String>,
) {
    companion object {
        fun of(
            manifest: FileManifest,
            appMax: FormatVersion = FormatVersion.CURRENT,
            knownFeatures: Set<String> = Features.KNOWN,
        ): NewerFileCheck {
            val minReader = FormatVersion.parseOrNull(manifest.minReaderVersion)
            val tooNew = minReader == null || minReader > appMax
            val unknownRequired = manifest.features.required.filter { it !in knownFeatures }
            val unknownOptional = manifest.features.optional.filter { it !in knownFeatures }
            return NewerFileCheck(tooNew || unknownRequired.isNotEmpty(), tooNew, unknownRequired, unknownOptional)
        }
    }
}

/** The `RHNewerFile` sheet (LIB-53). Nothing is imported, not even read-only. */
data class NewerFileSheet(
    /** Project name from the manifest (the file is not decoded). */
    val title: String,
    val ratingBadge: String?,
    /** Label · value rows. */
    val rows: List<Pair<String, String>>,
) {
    val heading: String get() = HEADING
    val note: String get() = NOTE
    val buttons: List<String> get() = listOf(CLOSE, UPDATE)

    companion object {
        const val HEADING = "새 버전 앱에서 만든 파일"
        const val NOTE = "가져오지 않음 · 파일은 그대로 · 업데이트 뒤 다시 열기"
        const val CLOSE = "닫기"
        const val UPDATE = "Civilight 업데이트"

        fun of(manifest: FileManifest, check: NewerFileCheck, appMax: FormatVersion = FormatVersion.CURRENT): NewerFileSheet {
            val rows = buildList {
                add("이 파일" to "양식 ${manifest.formatVersion}")
                add("지금 앱" to "${appMax}까지")
                if (check.unknownRequired.isNotEmpty()) {
                    add("필요한 기능" to check.unknownRequired.joinToString(" · ") { FeatureNames.korean(it) })
                }
            }
            return NewerFileSheet(
                title = manifest.project?.title.orEmpty(),
                ratingBadge = manifest.project?.rating?.let { RatingLabels.badge(it) },
                rows = rows,
            )
        }
    }
}
