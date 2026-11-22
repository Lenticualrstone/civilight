package io.github.lenticualrstone.civilight.domain.library.importing

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.format.ImportedPackage
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.PackageLockType
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import java.time.ZoneId

/**
 * A project that must change key before the import is stored (§6): the live project replaced by
 * `덮어쓰기` (it also goes to the trash with [trashEntry]) or a same-key project already in the trash
 * ([trashEntry] null). The store re-keys its files with [ProjectRekey] to [newKey].
 */
data class ReplacedProject(val oldKey: Key, val newKey: Key, val trashEntry: TrashEntry?)

/** A pure import plan; the store executes it (write project files, blobs, library row, recorder). */
class ImportPlan(
    val doc: ProjectDoc,
    val entry: LibraryEntry,
    /** The recorder library after merging the file's records. */
    val recorder: RecorderLibrary,
    val blobs: Map<String, ByteArray>,
    val replaced: ReplacedProject?,
    /** `열지 못한 부분 · …` when part of the file was damaged (LIB-55). */
    val failedMessage: String?,
)

/** Import as a new project or over the same project (LIB-52, LIB-54, LIB-59, §6). */
object ProjectImports {
    /** No live project with this key: import with the file's key. */
    fun direct(
        pkg: ImportedPackage,
        recorder: RecorderLibrary,
        now: Timestamp,
        newKey: () -> Key,
        trashedSameKey: LibraryEntry? = null,
    ): ImportPlan {
        val replaced = trashedSameKey?.let { ReplacedProject(it.key, newKey(), trashEntry = null) }
        return plan(pkg, pkg.doc, recorder, newKey, now, replaced)
    }

    /**
     * First-run install of the bundled example (LIB-17, §6): the current-version `.civilight` asset,
     * imported with `isExample = readOnly = true`. The flag lives only in the 서재 row; a file never
     * carries it, so a project opened from a file is never read-only.
     */
    fun installExample(pkg: ImportedPackage, recorder: RecorderLibrary, now: Timestamp, newKey: () -> Key): ImportPlan {
        val plan = direct(pkg, recorder, now, newKey)
        return ImportPlan(plan.doc, plan.entry.copy(isExample = true, readOnly = true), plan.recorder, plan.blobs, null, plan.failedMessage)
    }

    /** `새 프로젝트로`: a new project key, the name `<name> (n)`, item keys kept. */
    fun asNewProject(
        pkg: ImportedPackage,
        library: List<LibraryEntry>,
        recorder: RecorderLibrary,
        now: Timestamp,
        newKey: () -> Key,
    ): ImportPlan {
        val rekeyed = ProjectRekey.rekey(pkg.doc, newKey())
        val name = ProjectNames.nextFree(pkg.doc.project.name, library)
        val doc = rekeyed.copy(project = rekeyed.project.copy(name = name))
        return plan(pkg, doc, recorder, newKey, now, replaced = null)
    }

    /**
     * `덮어쓰기`: the existing project [existing] is re-keyed to a fresh key and moved to the trash
     * (`replacedByOverwrite`, 30 days); the file is imported with its own key. Recorder links to the
     * key now resolve to the imported project (§6 `ED`).
     */
    fun overwrite(
        pkg: ImportedPackage,
        existing: LibraryEntry,
        recorder: RecorderLibrary,
        now: Timestamp,
        zone: ZoneId,
        newKey: () -> Key,
    ): ImportPlan {
        val movedKey = newKey()
        val entry = TrashEntry(
            key = newKey(),
            kind = TrashKind.PROJECT.open(),
            projectKey = movedKey,
            targetKeys = listOf(movedKey),
            reason = TrashReason.REPLACED_BY_OVERWRITE.open(),
            hiddenAtDelete = existing.hidden,
            deletedAt = now,
            purgeOn = TrashEntry.purgeDateFor(TimeLabels.localDate(now, zone)),
        )
        return plan(pkg, pkg.doc, recorder, newKey, now, ReplacedProject(existing.key, movedKey, entry))
    }

    private fun plan(
        pkg: ImportedPackage,
        doc: ProjectDoc,
        recorder: RecorderLibrary,
        newKey: () -> Key,
        now: Timestamp,
        replaced: ReplacedProject?,
    ): ImportPlan {
        // A file locked with an export `암호` lands hidden on this device and is re-locked with its
        // biometrics (LIB-51, LIB-54). A 복구 암호 backup keeps its own flag: a visible project locked
        // only for its hidden items (§6 Q5) comes back visible (`ED`). An unknown lock counts as export.
        val forceHidden = pkg.decrypted && pkg.lockType?.known != PackageLockType.RECOVERY
        val finalDoc = if (forceHidden && !doc.project.protections.hidden) {
            doc.copy(project = doc.project.copy(protections = doc.project.protections.copy(hidden = true)))
        } else {
            doc
        }
        val merged = pkg.recorder?.let { RecorderMerge.mergeProjectFile(recorder, it, pkg.doc.key, finalDoc.key, newKey) } ?: recorder
        val project = finalDoc.project
        val entry = LibraryEntry(
            key = project.key,
            name = project.name,
            rating = project.rating,
            hidden = project.protections.hidden,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt.takeIf { it > 0 } ?: now,
        )
        return ImportPlan(finalDoc, entry, merged, pkg.blobs, replaced, pkg.failedMessage)
    }
}
