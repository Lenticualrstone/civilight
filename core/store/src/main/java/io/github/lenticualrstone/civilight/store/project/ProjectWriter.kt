package io.github.lenticualrstone.civilight.store.project

import io.github.lenticualrstone.civilight.model.app.UnknownFile
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import java.util.Base64

/** One save of a project: files to write, files to remove, and the state on disk afterwards. */
internal class ProjectCommit(
    val writes: Map<String, ByteArray>,
    val deletes: Set<String>,
    val next: WrittenState,
) {
    val isEmpty: Boolean get() = writes.isEmpty() && deletes.isEmpty()
}

/**
 * Decides which files of a project change (§8.2: only file units whose objects changed are
 * written). Each unit's value is compared with the value last written (or read) at its path; data
 * classes compare by reference first, so unchanged units cost almost nothing and are never encoded.
 * Paths that no longer hold a unit are removed (a stale `trash/…` file would bring purged rows back).
 */
internal object ProjectWriter {
    fun plan(doc: ProjectDoc, previous: WrittenState, storeVersion: Int): ProjectCommit {
        val units = ProjectLayout.units(doc, storeVersion)
        val writes = LinkedHashMap<String, ByteArray>()
        for ((path, unit) in units) {
            if (previous.units[path] != unit.value) writes[path] = unit.encode()
        }
        val deletes = LinkedHashSet<String>()
        for (path in previous.units.keys) if (path !in units) deletes += path

        val unknown = LinkedHashMap<String, UnknownFile>()
        for ((path, file) in doc.unknownFiles) {
            if (ProjectLayout.isFreeForUnknown(path)) unknown[path] = file
        }
        for ((path, file) in unknown) {
            if (previous.unknown[path] != file) writes[path] = Base64.getDecoder().decode(file.bytesBase64)
        }
        for (path in previous.unknown.keys) if (path !in unknown && path !in units) deletes += path
        deletes.removeAll(writes.keys)

        return ProjectCommit(writes, deletes, WrittenState(units.mapValues { it.value.value }, unknown))
    }

    /** Every file of [doc] (a new project directory). */
    fun all(doc: ProjectDoc, storeVersion: Int): ProjectCommit = plan(doc, WrittenState(), storeVersion)
}
