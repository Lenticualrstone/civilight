package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.ProjectTrash
import io.github.lenticualrstone.civilight.domain.trash.ProjectTrashState
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeResult
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrash
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.BlobRefs
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry
import java.time.ZoneId

/**
 * What one daily purge (or the start-up check) did. Nothing here touches files: the store writes
 * [projects], drops the stores of [purgedProjects], writes [projectTrash] and [recorderTrash], and
 * deletes the blob files in [blobsToDelete].
 */
data class PurgeRun(
    /** Every project document after the run (whole projects purged today are absent). */
    val projects: Map<Key, ProjectDoc>,
    /** Per project, the item purge when at least one of its entries was due. */
    val itemPurges: Map<Key, PurgeResult>,
    /** The 서재 index and the project-level entries after the run. */
    val projectTrash: ProjectTrashState,
    /** Whole projects purged (their stores are deleted by the caller). */
    val purgedProjects: List<Key>,
    /** The recorder library (links to purged things marked) and its trash entries. */
    val recorderTrash: RecorderTrashState,
    /** Deletion-log rows of purged recorder records. */
    val recorderLog: List<DeletionLogEntry>,
    /**
     * Blob hashes no surviving project row and no remaining recorder record references any more:
     * the global blob store deletes exactly these files (§5.3).
     */
    val blobsToDelete: List<String>,
) {
    val changedProjects: Set<Key> get() = itemPurges.keys
    val recorder: RecorderLibrary get() = recorderTrash.library
}

/**
 * The daily purge and the start-up maintenance (DECISIONS §1, `domain:trash` Requests to core:store):
 * whole projects due ([ProjectTrash.purgeDue]), entries due inside every other project
 * ([PurgeOps.purgeDue]) with the recorder links into them updated ([RecorderTrash.applyProjectPurge],
 * plain unit labels), recorder records due ([RecorderTrash.purgeDue]), and the blob files nobody
 * references any more. Pure: inputs in, a [PurgeRun] out.
 */
object PurgeActions {
    /**
     * Runs every purge due at [now] (local dates in [zone]). [projects] are the documents of every
     * project of the 서재, live and trashed (a project whose document is not given only takes part
     * through its 서재 row); [projectTrash] the 서재 index with the project entries; [recorderTrash]
     * the recorder library with its entries.
     */
    fun runDue(
        now: Timestamp,
        projects: List<ProjectDoc>,
        projectTrash: ProjectTrashState,
        recorderTrash: RecorderTrashState,
        zone: ZoneId = ZoneId.of("Asia/Seoul"),
        device: String = "",
        newKey: () -> Key = Keys::newKey,
    ): PurgeRun = runDue(ActionContext(now, zone, device, newKey), projects, projectTrash, recorderTrash)

    fun runDue(
        ctx: ActionContext,
        projects: List<ProjectDoc>,
        projectTrash: ProjectTrashState,
        recorderTrash: RecorderTrashState,
    ): PurgeRun {
        val docs = LinkedHashMap<Key, ProjectDoc>().apply { projects.forEach { put(it.key, it) } }
        var library = recorderTrash.library
        val candidates = LinkedHashSet<String>()

        // 1. Whole projects.
        val rows = projectTrash.library.projects.associateBy { it.key }
        val (projectState, gone) = ProjectTrash.purgeDue(projectTrash, ctx.trash)
        for (key in gone) {
            val doc = docs.remove(key)
            val row = rows[key]
            val name = row?.name ?: doc?.project?.name.orEmpty()
            val hidden = (row?.hidden ?: false) || (doc?.project?.protections?.hidden ?: false)
            library = RecorderTrash.applyProjectGone(library, key, name, hidden, ctx.trash)
            if (doc != null) candidates += heldBlobs(doc)
        }

        // 2. Entries inside the remaining projects.
        val itemPurges = LinkedHashMap<Key, PurgeResult>()
        for ((key, doc) in docs.entries.toList()) {
            val result = PurgeOps.purgeDue(doc, ctx.trash)
            if (result.purgedEntries.isEmpty()) continue
            val labelled = withPlainUnitLabels(doc, result)
            itemPurges[key] = labelled
            docs[key] = labelled.doc
            library = applyProjectPurge(library, key, labelled, ctx)
            candidates += labelled.unreferencedBlobs
        }

        // 3. Recorder records.
        val (recorderState, log) = RecorderTrash.purgeDue(RecorderTrashState(library, recorderTrash.entries), ctx.trash)
        val remainingRecords = recorderState.library.records.map { it.key }.toSet()
        library.records.filter { it.key !in remainingRecords }.mapNotNullTo(candidates) { it.audio?.hash }

        // 4. Blob files: candidates no surviving project or record still holds.
        val kept = HashSet<String>()
        docs.values.forEach { kept += heldBlobs(it) }
        recorderState.library.records.mapNotNullTo(kept) { it.audio?.hash }
        val blobs = candidates.filter { it !in kept }

        return PurgeRun(
            projects = docs,
            itemPurges = itemPurges,
            projectTrash = projectState,
            purgedProjects = gone,
            recorderTrash = recorderState,
            recorderLog = log,
            blobsToDelete = blobs,
        )
    }

    /**
     * [result] with the label of every purged unit replaced by its plain unit label as
     * `domain:story` [UnitCodes] prints it before the purge (`1-7 씬 3`, `1-7`, `2장`; the
     * `domain:recorder` request): recorder links keep it as `lastLabel`.
     */
    fun withPlainUnitLabels(before: ProjectDoc, result: PurgeResult): PurgeResult {
        if (result.purgedTargets.none { it.kind == ItemKind.UNIT }) return result
        val codes = UnitCodes(before)
        return result.copy(
            purgedTargets = result.purgedTargets.map { t ->
                if (t.kind != ItemKind.UNIT) t else t.copy(label = codes.unitLabel(t.key) ?: t.label)
            },
        )
    }

    /** The recorder library after a purge inside project [projectKey] (links get `purgedAt` and `lastLabel`). */
    fun applyProjectPurge(library: RecorderLibrary, projectKey: Key, result: PurgeResult, ctx: ActionContext): RecorderLibrary =
        RecorderTrash.applyProjectPurge(library, projectKey, result, ctx.trash)

    /**
     * Blob hashes a project holds: every row that points at a blob, live or trashed (the one function
     * `core:model` [BlobRefs], gallery items included, T-GAL-27), plus its asset index rows (so a
     * purged project's catalogue is a candidate too).
     */
    fun heldBlobs(doc: ProjectDoc): Set<String> = BlobRefs.of(doc) + doc.assets.blobs.map { it.hash }
}
