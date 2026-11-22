package io.github.lenticualrstone.civilight.domain.library.importing

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.format.ImportedPackage
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import java.time.ZoneId

/** The `같은 프로젝트가 이미 있음` block of [OpenFileSheet] (LIB-52). */
data class SameProjectBlock(
    /** `서재 · 씬 <s> · <time> 수정` of the project already in the 서재. */
    val existingSub: String,
    val choices: SameProjectChoices,
) {
    val heading: String get() = SameProjectChoices.HEADING
}

/**
 * The `RHOpenFile` sheet (LIB-52): what the file holds and, when the same project (same project key)
 * already exists, the `새 프로젝트로` · `덮어쓰기` choice. Encrypted files show it after decryption.
 */
data class OpenFileSheet(
    val title: String,
    val ratingBadge: String,
    /** `씬 31 · 인물 11 · 1.2 MB · 11월 22일`. */
    val summary: String,
    val sameProject: SameProjectBlock?,
    /** `열지 못한 부분 · 1-7` when part of the file was damaged (LIB-55). */
    val failedNote: String?,
) {
    val header: String get() = HEADER
    val close: String get() = CLOSE
    val buttons: List<String> get() = listOf(CANCEL, OPEN)

    companion object {
        const val HEADER = "Civilight · 파일에서 열기"
        const val CLOSE = "닫기"
        const val CANCEL = "취소"
        const val OPEN = "열기"
    }
}

object OpenFileSheets {
    /**
     * `씬 <s> · 인물 <c> · <size> · <date>`: live scenes and live characters of the file, its size,
     * and the day it was written (`ED`: the manifest's `writtenAt`).
     */
    fun summary(doc: ProjectDoc, sizeBytes: Long, writtenAt: Timestamp, zone: ZoneId): String {
        val scenes = doc.manuscript.liveScenes.size
        val characters = doc.cast.characters.count { it.deletedAt == null }
        return "씬 $scenes · 인물 $characters · ${TimeLabels.size(sizeBytes)} · ${TimeLabels.monthDay(writtenAt, zone)}"
    }

    /**
     * The sheet for [pkg] under [decision]. [existingSceneCount] is the live scene count of the
     * project already in the 서재 (needed only for [OpenDecision.AskSameProject]). [sizeBytes]
     * defaults to the package size; pass the outer file size for an encrypted file.
     */
    fun of(
        pkg: ImportedPackage,
        decision: OpenDecision,
        existingSceneCount: Int,
        now: Timestamp,
        zone: ZoneId,
        sizeBytes: Long = pkg.sizeBytes,
    ): OpenFileSheet {
        val doc = pkg.doc
        val same = (decision as? OpenDecision.AskSameProject)?.let { ask ->
            SameProjectBlock(
                existingSub = SameProjectChoices.existingSub(existingSceneCount, ask.existing.updatedAt, now, zone),
                choices = ask.choices,
            )
        }
        return OpenFileSheet(
            title = doc.project.name,
            ratingBadge = RatingLabels.badge(doc.project.rating),
            summary = summary(doc, sizeBytes, pkg.manifest.writtenAt, zone),
            sameProject = same,
            failedNote = pkg.failedMessage,
        )
    }
}
