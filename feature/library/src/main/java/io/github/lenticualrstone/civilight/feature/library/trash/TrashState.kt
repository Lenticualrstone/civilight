package io.github.lenticualrstone.civilight.feature.library.trash

import io.github.lenticualrstone.civilight.domain.trash.HidingLocks
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.domain.trash.TrashFilter
import io.github.lenticualrstone.civilight.domain.trash.TrashListing
import io.github.lenticualrstone.civilight.domain.trash.TrashRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import io.github.lenticualrstone.civilight.navigation.TrashFilter as RouteFilter

/** Where a trash row lives, so its actions go to the right store. */
enum class TrashSource {
    /** A whole project (`files/trash/index.json`). */
    PROJECT,

    /** An item inside a live project (its own `trash/index.json`). */
    ITEM,

    /** A Life Recorder record (`files/recorder/trash.json`). */
    RECORDER,
}

/** One row of the 휴지통 as drawn: the domain row plus where it lives and how to protect the window. */
data class TrashRowUi(
    val row: TrashRow,
    val source: TrashSource,
    /** The project the row belongs to (null for recorder rows). */
    val projectKey: Key?,
    /**
     * The project's protections while the row shows its content (unmasked), so the window is
     * protected as the project's own screens are (LIB-26, LIB-29). Null for masked rows and records.
     */
    val protections: Protections?,
) {
    val key: Key get() = row.entryKey
    val isBlockGroup: Boolean get() = row.kind.known == TrashKind.BLOCK_GROUP
}

/** One filter chip (`전체 <n>` · `프로젝트` · …, T-10). */
data class TrashChip(val filter: TrashFilter, val label: String)

/** Everything the 휴지통 draws (LIB-38). */
data class TrashUi(
    val loaded: Boolean = false,
    val chips: List<TrashChip> = emptyList(),
    /** Every row of every source, newest deletion first. */
    val rows: List<TrashRowUi> = emptyList(),
) {
    /** The rows of [filter] (T-09). */
    fun visible(filter: TrashFilter): List<TrashRowUi> {
        val order = TrashListing.list(rows.map { it.row }, filter).map { it.entryKey }
        val byKey = rows.associateBy { it.key }
        return order.mapNotNull(byKey::get)
    }

    fun row(entryKey: Key): TrashRowUi? = rows.firstOrNull { it.key == entryKey }

    /**
     * `비우기` deletes every entry regardless of the filter, except hidden things that are locked now
     * (LIB-39 `ED`): exactly the rows shown in their masked form stay.
     */
    val purgeable: List<TrashRowUi> get() = rows.filter { !it.row.masked }
}

/** The stores' trash rows of every source, read for one build. */
data class TrashInputs(
    val index: LibraryIndexFile,
    /** Project-level trash rows (kind `project`). */
    val projectEntries: List<TrashEntry>,
    /** Documents of trashed projects (row counts `씬 <s>개 · 인물 <c>명`), when read. */
    val trashedDocs: Map<Key, ProjectDoc>,
    /** Documents of live projects (their own trash rows); the read-only example is left out. */
    val liveDocs: List<ProjectDoc>,
    val recorder: RecorderLibrary,
    val recorderEntries: List<TrashEntry>,
)

/** Builds [TrashUi] from the stores and the unlock state; pure, so LIB-38's rules run on the JVM. */
object TrashState {
    fun filterOf(route: RouteFilter): TrashFilter = TrashFilter.valueOf(route.name)

    fun routeFilterOf(filter: TrashFilter): RouteFilter = RouteFilter.valueOf(filter.name)

    fun build(inputs: TrashInputs, unlock: UnlockState, ctx: TrashContext, loaded: Boolean = true): TrashUi {
        val projectRows = TrashListing.projectRows(
            entries = inputs.projectEntries,
            library = inputs.index,
            docs = inputs.trashedDocs,
            ctx = ctx,
            hiddenGroupLocked = !unlock.hiddenGroupOpen,
        ).map { row ->
            val protections = row.projectKey?.let { key -> inputs.trashedDocs[key]?.project?.protections ?: inputs.index.projects.firstOrNull { it.key == key }?.let { Protections(hidden = it.hidden) } }
            TrashRowUi(row, TrashSource.PROJECT, row.projectKey, protections.takeUnless { row.masked })
        }
        val itemRows = inputs.liveDocs.flatMap { doc ->
            val locks = HidingLocks(
                itemsLocked = !unlock.itemsOpen(doc.key),
                projectLocked = doc.project.protections.hidden && !unlock.projectOpen(doc.key),
            )
            TrashListing.projectItemRows(doc, ctx, locks, inputs.recorder).map { row ->
                // A masked row shows only `숨긴 …` words; its project's window rules still apply while
                // the location (the project name) is visible.
                val nameShown = !(locks.projectLocked)
                TrashRowUi(row, TrashSource.ITEM, doc.key, doc.project.protections.takeIf { nameShown })
            }
        }
        val recorderRows = TrashListing.recorderRows(inputs.recorder, inputs.recorderEntries, ctx, recordsLocked = !unlock.recordsOpen)
            .map { TrashRowUi(it, TrashSource.RECORDER, null, null) }
        val all = projectRows + itemRows + recorderRows
        val ordered = TrashListing.list(all.map { it.row }, TrashFilter.ALL).map { it.entryKey }
        val byKey = all.associateBy { it.key }
        val rows = ordered.mapNotNull(byKey::get)
        val labels = TrashListing.chips(rows.map { it.row })
        return TrashUi(
            loaded = loaded,
            chips = TrashFilter.entries.mapIndexed { i, f -> TrashChip(f, labels[i]) },
            rows = rows,
        )
    }
}
