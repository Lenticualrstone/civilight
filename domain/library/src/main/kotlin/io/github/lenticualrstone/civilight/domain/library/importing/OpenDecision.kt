package io.github.lenticualrstone.civilight.domain.library.importing

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.format.FormatVersion
import io.github.lenticualrstone.civilight.domain.library.format.NewerFileCheck
import io.github.lenticualrstone.civilight.domain.library.format.NewerFileSheet
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.FileManifest
import java.time.ZoneId

/** What to do with an opened file (LIB-52, LIB-53, §6). */
sealed interface OpenDecision {
    /** Newer file: sheet only, nothing imported. */
    data class RefuseNewer(val sheet: NewerFileSheet) : OpenDecision

    /** A live project with the same key exists: ask `새 프로젝트로` (default) or `덮어쓰기`. */
    data class AskSameProject(val existing: LibraryEntry, val choices: SameProjectChoices) : OpenDecision

    /**
     * Import with the file's key. [trashedSameKey] is a project with the same key that is only in
     * the trash; it is re-keyed first and no question is asked (§6 `ED`).
     */
    data class Import(val trashedSameKey: LibraryEntry?) : OpenDecision
}

/** The two choices when the same project exists (LIB-52 `RHOpenFile`). */
enum class SameProjectChoice { AS_NEW_PROJECT, OVERWRITE }

/** The `같은 프로젝트가 이미 있음` block: radio titles and subs, default first. */
data class SameProjectChoices(val newName: String) {
    val heading: String get() = HEADING
    val default: SameProjectChoice get() = SameProjectChoice.AS_NEW_PROJECT

    fun title(choice: SameProjectChoice): String = when (choice) {
        SameProjectChoice.AS_NEW_PROJECT -> "새 프로젝트로"
        SameProjectChoice.OVERWRITE -> "덮어쓰기"
    }

    fun sub(choice: SameProjectChoice): String = when (choice) {
        SameProjectChoice.AS_NEW_PROJECT -> "‘$newName’로 추가"
        SameProjectChoice.OVERWRITE -> "지금 것은 휴지통 · 30일"
    }

    val buttons: List<String> get() = listOf("취소", "열기")

    companion object {
        const val HEADING = "같은 프로젝트가 이미 있음"

        /** `서재 · 씬 <s> · <time> 수정` for the existing project. */
        fun existingSub(sceneCount: Int, updatedAt: Long, now: Long, zone: ZoneId): String =
            "서재 · 씬 $sceneCount · ${TimeLabels.relative(updatedAt, now, zone)} 수정"
    }
}

object OpenDecisions {
    /**
     * Decides from the manifest alone (the file is decoded only when it will be imported).
     * Same project = same project key (memory 2026-11-22), never the same name.
     */
    fun decide(
        manifest: FileManifest,
        library: List<LibraryEntry>,
        appMax: FormatVersion = FormatVersion.CURRENT,
        knownFeatures: Set<String> = Features.KNOWN,
    ): OpenDecision {
        val check = NewerFileCheck.of(manifest, appMax, knownFeatures)
        if (check.refused) return OpenDecision.RefuseNewer(NewerFileSheet.of(manifest, check, appMax))
        val key = manifest.project?.key
        val sameKey = library.filter { it.key == key }
        val live = sameKey.firstOrNull { it.deletedAt == null }
        if (live != null) {
            val name = manifest.project?.title ?: live.name
            return OpenDecision.AskSameProject(live, SameProjectChoices(ProjectNames.nextFree(name, library)))
        }
        return OpenDecision.Import(sameKey.firstOrNull())
    }
}

/** Project naming on import (LIB-52, F-11). */
object ProjectNames {
    /**
     * `<name> (n)` with the smallest n ≥ 2 not used by a project in the 서재 (projects in the trash
     * do not block a name, `ED`).
     */
    fun nextFree(name: String, library: List<LibraryEntry>): String {
        val taken = library.filter { it.deletedAt == null }.map { it.name }.toSet()
        var n = 2
        while ("$name ($n)" in taken) n++
        return "$name ($n)"
    }
}
