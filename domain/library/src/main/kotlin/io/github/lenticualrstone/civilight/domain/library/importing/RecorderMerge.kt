package io.github.lenticualrstone.civilight.domain.library.importing

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * Recorder records merge by their permanent key (R23 §6, LRC-45, F-18/F-19): a record already in
 * the library is never duplicated and keeps its local text; it only gains links and placements.
 * Links are unique per (record, project, target kind, target); placements per (record, project,
 * kind, target).
 */
object RecorderMerge {
    /**
     * Merges a project's `recorder.json` into [library]. [fileProjectKey] is the key the file was
     * written under, [projectKey] the key the project gets on this device (they differ for
     * `새 프로젝트로`). Rows from the file get a fresh key from [newKey] when their key is already used
     * by another row of the library (always the case for `새 프로젝트로`).
     */
    fun mergeProjectFile(
        library: RecorderLibrary,
        file: RecorderFile,
        fileProjectKey: Key,
        projectKey: Key,
        newKey: () -> Key,
    ): RecorderLibrary {
        val incoming = file.toLibrary(impliedProjectKey = fileProjectKey)
        val moved = if (fileProjectKey == projectKey) incoming else incoming.moveProject(fileProjectKey, projectKey)
        return merge(library, moved, newKey)
    }

    /** Merges two libraries (backup restore of several projects, `.civilight-recorder` open). */
    fun merge(library: RecorderLibrary, incoming: RecorderLibrary, newKey: () -> Key): RecorderLibrary {
        val records = LinkedHashMap<Key, RecorderRecord>()
        library.records.forEach { records[it.key] = it }
        incoming.records.forEach { if (it.key !in records) records[it.key] = it }

        val linkKeys = library.links.mapTo(HashSet()) { it.key }
        val links = ArrayList(library.links)
        val linkIds = library.links.mapTo(HashSet()) { it.identity() }
        for (link in incoming.links) {
            if (!linkIds.add(link.identity())) continue
            val key = if (linkKeys.add(link.key)) link.key else newKey().also { linkKeys += it }
            links += link.copy(key = key)
        }

        val placementKeys = library.placements.mapTo(HashSet()) { it.key }
        val placements = ArrayList(library.placements)
        val placementIds = library.placements.mapTo(HashSet()) { it.identity() }
        for (placement in incoming.placements) {
            if (!placementIds.add(placement.identity())) continue
            val key = if (placementKeys.add(placement.key)) placement.key else newKey().also { placementKeys += it }
            placements += placement.copy(key = key)
        }
        return RecorderLibrary(records.values.toList(), links, placements)
    }

    private fun RecorderLink.identity() = listOf(recordKey, projectKey, targetKind.raw, targetKey)

    private fun Placement.identity() = listOf(recordKey, projectKey, kind.raw, targetKey)

    /** Links, placements and `@` runs pointing at project [from] now point at [to] (`ED` for runs). */
    private fun RecorderLibrary.moveProject(from: Key, to: Key) = RecorderLibrary(
        records = records.map { r ->
            r.copy(
                runs = r.runs.map { run ->
                    if (run is TextRun.Link && run.target.projectKey == from) run.copy(target = run.target.copy(projectKey = to)) else run
                },
            )
        },
        links = links.map { l ->
            l.copy(
                projectKey = if (l.projectKey == from) to else l.projectKey,
                targetKey = if (l.targetKind.known == RecorderTargetKind.PROJECT && l.targetKey == from) to else l.targetKey,
            )
        },
        placements = placements.map { p -> if (p.projectKey == from) p.copy(projectKey = to) else p },
    )
}
