package io.github.lenticualrstone.civilight.domain.recorder.inbox

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.RecorderTimeLabels
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementLabels
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementRow
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * The record screen (LRC-28): editable text, player, every link chip with `+ 연결`, the `넣은 곳`
 * list with `◇ 넣은 뒤 고침` per row and `◇ 넣은 곳 지워짐` when every copy is gone, and the actions.
 * Those two marks appear only here, never in the inbox row or the check center.
 */
data class RecordScreen(
    val recordKey: Key,
    val kindLabel: String,
    val meta: String,
    val runs: List<TextRun>,
    val hasPlayer: Boolean,
    val chips: List<Chip>,
    val addLink: String,
    val state: RecordState,
    val placementsLabel: String,
    val placements: List<PlacementRow>,
    /** `◇ 넣은 곳 지워짐`, or null. */
    val placementsGoneMark: String?,
    val actions: List<String>,
)

/** The record screen and in-place text editing. */
object RecordScreens {
    /** Null for an unknown, trashed, or hidden-and-locked record (a deep link first asks `숨긴 기록 보기`). */
    fun of(library: RecorderLibrary, recordKey: Key, env: RecorderEnv, now: Timestamp): RecordScreen? {
        val record = library.record(recordKey) ?: return null
        if (record.deletedAt != null) return null
        if (record.hidden && !env.recordsOpen) return null
        val placements = library.placementsOf(recordKey)
        return RecordScreen(
            recordKey = recordKey,
            kindLabel = RecorderTimeLabels.kindLabel(record),
            meta = RecorderTimeLabels.meta(record.createdAt, now, env.zone),
            runs = record.runs,
            hasPlayer = record.kind.known == RecordKind.VOICE && record.audio != null,
            chips = Chips.captureRow(library.linksOf(recordKey), record.runs, env, record),
            addLink = RecorderCopy.ADD_LINK,
            state = RecordStates.state(record, placements, env),
            placementsLabel = RecorderCopy.PLACEMENTS,
            placements = PlacementLabels.rows(record, placements, env),
            placementsGoneMark = if (RecordStates.placementsAllGone(placements, env)) RecorderCopy.PLACEMENTS_GONE else null,
            actions = listOf(RecorderCopy.PUT_MANUSCRIPT, RecorderCopy.PUT_CARD, RecorderCopy.MAKE_PAGE),
        )
    }

    /** True when the record screen needs the biometric prompt `숨긴 기록 보기` first. */
    fun needsUnlock(library: RecorderLibrary, recordKey: Key, env: RecorderEnv): Boolean =
        library.record(recordKey)?.hidden == true && !env.recordsOpen

    /**
     * A committed edit of the record text (autosave): NFC, `@` links follow their runs, and the text
     * revision grows by one when the text changed (used by `◇ 넣은 뒤 고침`). Copies never change.
     */
    fun editText(library: RecorderLibrary, recordKey: Key, runs: List<TextRun>, ctx: RecorderEditContext): RecorderLibrary {
        val record = library.record(recordKey) ?: return library
        val next = Runs.normalize(runs)
        if (next == record.runs) return library
        val updated = record.copy(runs = next, textRevision = record.textRevision + 1, updatedAt = ctx.now, updatedBy = ctx.device)
        val withRecord = library.copy(records = library.records.map { if (it.key == recordKey) updated else it })
        return LinkEdits.replaceLinksOf(withRecord, recordKey, LinkEdits.syncAt(library.linksOf(recordKey), recordKey, next, ctx))
    }
}
