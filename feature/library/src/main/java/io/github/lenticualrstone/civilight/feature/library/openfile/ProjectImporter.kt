package io.github.lenticualrstone.civilight.feature.library.openfile

import io.github.lenticualrstone.civilight.domain.library.format.ImportedPackage
import io.github.lenticualrstone.civilight.domain.library.importing.ImportPlan
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecision
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecisions
import io.github.lenticualrstone.civilight.domain.library.importing.ProjectImports
import io.github.lenticualrstone.civilight.domain.library.importing.RecorderMerge
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import java.time.ZoneId

/** What an import needs from the stores; the app implements it over `StoreContainer` (tests: a fake). */
interface ImportPorts {
    /** Every 서재 row, live and trashed, as it is now. */
    fun libraryEntries(): List<LibraryEntry>

    /** Stores one blob of the file (content-addressed: the same bytes give the same hash). */
    suspend fun importBlob(hash: String, bytes: ByteArray, mime: String, fileName: String)

    /**
     * Before the project [key] is re-keyed and moved aside: its screens leave the stack and cached
     * stores close, so nothing keeps the old copy open under the key the import will use.
     */
    suspend fun releaseProject(key: Key)

    /** `LibraryStore.moveAsideForImport`: re-keys [key] (live → trash `replacedByOverwrite`; trashed → stays trashed). */
    suspend fun moveAside(key: Key)

    suspend fun addProject(doc: ProjectDoc)

    /** Applies [transform] to the recorder library as it is when written (never a stale snapshot). */
    suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary)

    suspend fun removeRecentsOf(key: Key)

    /** Applies [transform] to the app-level 인물 템플릿 (restore merge, CST-63); fakes may ignore it. */
    suspend fun updateTemplates(transform: (List<FieldTemplate>) -> List<FieldTemplate>) = Unit
}

/** The result of `열기`. */
data class ImportOutcome(
    val projectKey: Key,
    val hidden: Boolean,
    /** `열지 못한 부분 · 1-7` when part of the file was damaged (LIB-55). */
    val failedMessage: String?,
)

/**
 * Runs `열기` of 파일에서 열기 (LIB-52, LIB-54): the plan comes from `domain:library` `ProjectImports`,
 * built against the 서재 as it is at this moment (names `<name> (n)` recomputed). Order: blobs, then
 * the old copy moved aside (`덮어쓰기`, or a same-key project only in the trash), then the project, then
 * the recorder records merged by key into the library as it is now.
 *
 * `LibraryStore.moveAsideForImport` makes its own key and trash row, so [ImportPlan.replaced] only
 * says which key to move. Files locked with an export `암호` land hidden (`ProjectImports`).
 */
class ProjectImporter(private val ports: ImportPorts) {
    suspend fun import(
        pkg: ImportedPackage,
        decision: OpenDecision,
        choice: SameProjectChoice?,
        now: Timestamp,
        zone: ZoneId,
        newKey: () -> Key,
    ): ImportOutcome {
        val library = ports.libraryEntries()
        // The decision is taken again on the current 서재: the same project may have been deleted
        // or another `(2)` made while the sheet was open.
        require(decision !is OpenDecision.RefuseNewer) { "a newer file is never imported" }
        val current = OpenDecisions.decide(pkg, library)
        val plan: ImportPlan = when (current) {
            is OpenDecision.RefuseNewer -> error("a newer file is never imported")
            is OpenDecision.Import -> ProjectImports.direct(pkg, RecorderLibrary(), now, newKey, current.trashedSameKey)
            is OpenDecision.AskSameProject -> when (choice ?: current.choices.default) {
                SameProjectChoice.AS_NEW_PROJECT -> ProjectImports.asNewProject(pkg, library, RecorderLibrary(), now, newKey)
                SameProjectChoice.OVERWRITE ->
                    if (current.existing.isExample || current.existing.readOnly) {
                        ProjectImports.asNewProject(pkg, library, RecorderLibrary(), now, newKey)
                    } else {
                        ProjectImports.overwrite(pkg, current.existing, RecorderLibrary(), now, zone, newKey)
                    }
            }
        }

        for ((hash, bytes) in plan.blobs) {
            val row = plan.doc.assets.blobs.firstOrNull { it.hash == hash }
            ports.importBlob(hash, bytes, row?.mime ?: GENERIC_MIME, row?.fileName ?: hash)
        }
        plan.replaced?.let { replaced ->
            ports.releaseProject(replaced.oldKey)
            ports.moveAside(replaced.oldKey)
            ports.removeRecentsOf(replaced.oldKey)
        }
        ports.addProject(plan.doc)
        pkg.recorder?.let { file ->
            ports.updateRecorder { library -> RecorderMerge.mergeProjectFile(library, file, pkg.doc.key, plan.doc.key, newKey) }
        }
        return ImportOutcome(plan.doc.key, plan.doc.project.protections.hidden, plan.failedMessage)
    }

    private companion object {
        const val GENERIC_MIME = "application/octet-stream"
    }
}
