package io.github.lenticualrstone.civilight.feature.recorder.inbox

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxRow
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxScreen
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordSearch
import io.github.lenticualrstone.civilight.feature.recorder.record.PutAvailability
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/**
 * Which row actions can run now ([PutAvailability]): each 넣기 needs text (an empty record cannot
 * be inserted, LRC-33 `[PROPOSED]`; a voice record has empty text until a speech engine lands or
 * the user types it on the record screen) and a project that can take it.
 */
data class RowActions(val putManuscript: Boolean, val putCard: Boolean = false, val makePage: Boolean = false) {
    companion object {
        fun of(kinds: Set<PutKind>) = RowActions(PutKind.MANUSCRIPT in kinds, PutKind.CARD in kinds, PutKind.PAGE in kinds)
    }
}

/** One drawn row: the domain row, what its buttons can do, and its kept audio for the player (LRC-14). */
data class RowView(val row: InboxRow, val actions: RowActions, val audio: RecorderAudio? = null)

/**
 * The 기록함 as drawn (LRC-25 … LRC-27): the domain screen, the rows narrowed by the header search
 * (§6 Q11 `ED`: a text search over the visible records; hidden records only while open), and the
 * entry context of `말로 기록` / `글로 쓰기` (the project filter pre-fills its link, LRC-19).
 */
data class InboxView(
    val screen: InboxScreen,
    val rows: List<RowView>,
    val openRows: List<RowView>,
    val filter: InboxFilterState,
    val query: String,
    val captureEntry: EntryContext,
) {
    /** The library has no records at all (`EmptyLr`, LRC-30). */
    val empty: Boolean get() = screen.empty != null

    /** Nothing passes the filters or the search (TODO_COPY_LRC_12); the filter row stays. */
    val filteredEmpty: Boolean get() = !empty && rows.isEmpty() && openRows.isEmpty()
}

object InboxViews {
    fun of(
        library: RecorderLibrary,
        filter: InboxFilterState,
        env: RecorderEnv,
        now: Timestamp,
        relockPolicy: RelockPolicy = RelockPolicy.ON_LEAVE,
        query: String = "",
    ): InboxView {
        val screen = Inbox.screen(library, filter, env, now, relockPolicy)
        val hits: Set<Key>? = query.takeIf { it.isNotBlank() }?.let { q -> RecordSearch.search(library, q, env).map { it.key }.toSet() }
        val offered = PutAvailability.offered(env)
        fun view(row: InboxRow): RowView {
            val record = library.record(row.recordKey)
            val text = record?.runs?.let(Runs::text).orEmpty()
            return RowView(row, RowActions.of(PutAvailability.kinds(text, offered)), record?.audio?.takeIf { row.hasPlayer })
        }
        fun keep(row: InboxRow) = hits == null || row.recordKey in hits
        return InboxView(
            screen = screen,
            rows = screen.rows.filter(::keep).map(::view),
            openRows = screen.openGroup?.rows.orEmpty().filter(::keep).map(::view),
            filter = filter,
            query = query,
            captureEntry = EntryContext(surface = EntrySurface.INBOX, projectKey = (filter.project as? ProjectFilter.Project)?.projectKey),
        )
    }
}
