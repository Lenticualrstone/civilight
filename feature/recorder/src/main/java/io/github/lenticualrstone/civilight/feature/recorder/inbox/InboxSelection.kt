package io.github.lenticualrstone.civilight.feature.recorder.inbox

import io.github.lenticualrstone.civilight.model.Key

/** LRC-31 `목록 + 상세`: which record the right pane shows when the inbox opens with nothing beside it. */
object InboxSelection {
    /**
     * The newest visible record (`[PROPOSED]` "the newest visible record is selected"), only the first
     * time this inbox entry is shown in two panes ([alreadyDone]) and only while nothing else is on
     * top of it ([inboxOnTop]); null otherwise or when no row is visible (locked hidden records are
     * never rows, so they are never opened this way).
     */
    fun autoSelect(alreadyDone: Boolean, inboxOnTop: Boolean, rows: List<RowView>): Key? =
        if (alreadyDone || !inboxOnTop) null else rows.firstOrNull()?.row?.recordKey
}
