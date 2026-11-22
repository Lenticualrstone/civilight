package io.github.lenticualrstone.civilight.domain.recorder.capture

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.hide.HideRow
import io.github.lenticualrstone.civilight.domain.recorder.hide.RecordHiding
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkCandidate
import io.github.lenticualrstone.civilight.domain.recorder.text.AtMentions
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.recorder.TextMemoDraft
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.isBlankText

/** The audio of a recording switched to text (`ED` Q10): it stays attached; the record stays voice. */
data class CarriedVoice(val recordKey: Key, val durationMs: Long, val audioTempPath: String)

/**
 * The text memo sheet (LRC-15): the draft (runs, pending links, entry), the `숨기기` switch (`ED` Q9:
 * same row as `정리 중`) and, after a switch from voice, the carried recording. Draft link keys
 * use an empty record key until saved.
 */
data class MemoSheet(
    val draft: TextMemoDraft,
    val hideSwitch: Boolean? = null,
    val carriedVoice: CarriedVoice? = null,
)

/** What the text sheet shows. [canSave] = `저장` enabled (not empty or whitespace, [PROPOSED]). */
data class MemoSheetView(
    val title: String,
    val close: String,
    val text: String,
    val chips: List<Chip>,
    val addLink: String,
    val hideRow: HideRow?,
    val toVoiceAria: String,
    val hint: String,
    val save: String,
    val canSave: Boolean,
)

/** The text memo and its `@` links (LRC-15 … LRC-17). */
object TextMemo {
    private const val DRAFT_RECORD = ""

    /**
     * Opens the sheet. A kept draft returns as it was ([PROPOSED] LRC-15); otherwise a new draft
     * starts with the pre-filled link of [entry].
     */
    fun open(entry: EntryContext, settings: RecorderSettings, env: RecorderEnv, kept: TextMemoDraft?, ctx: RecorderEditContext): MemoSheet {
        val draft = kept ?: TextMemoDraft(emptyList(), EntryLinks.initialLinks(DRAFT_RECORD, entry, settings, ctx), entry, ctx.now)
        return MemoSheet(draft, RecordHiding.hideSwitch(null, draft.pendingLinks, env))
    }

    /** New text from the field: `@` links follow their runs (deleting `@세라` removes its link and chip). */
    fun edit(sheet: MemoSheet, runs: List<TextRun>, env: RecorderEnv, ctx: RecorderEditContext): MemoSheet {
        val normalized = Runs.normalize(runs)
        val links = LinkEdits.syncAt(sheet.draft.pendingLinks, DRAFT_RECORD, normalized, ctx)
        return withLinks(sheet.copy(draft = sheet.draft.copy(runs = normalized, updatedAt = ctx.now)), links, env)
    }

    /** Types plain [text] at shown offset [at]. */
    fun type(sheet: MemoSheet, at: Int, text: String, env: RecorderEnv, ctx: RecorderEditContext): MemoSheet =
        edit(sheet, Runs.insertText(sheet.draft.runs, at, text), env, ctx)

    /** Deletes the shown range [start, end) (a link run inside it goes, with its link). */
    fun delete(sheet: MemoSheet, start: Int, end: Int, env: RecorderEnv, ctx: RecorderEditContext): MemoSheet =
        edit(sheet, Runs.delete(sheet.draft.runs, start, end), env, ctx)

    /** The `@` query open at [caret], for the picker in `PickerMode.AT`. */
    fun atQuery(sheet: MemoSheet, caret: Int) = AtMentions.queryAt(sheet.draft.runs, caret)

    /** Picks [candidate] for the open `@` query: a link run, and a link of origin `@`. Returns the sheet and the caret. */
    fun pick(sheet: MemoSheet, query: AtMentions.AtQuery, candidate: LinkCandidate, env: RecorderEnv, ctx: RecorderEditContext): Pair<MemoSheet, Int> {
        val (runs, caret) = AtMentions.complete(sheet.draft.runs, query, candidate.target, candidate.runText, ctx)
        return edit(sheet, runs, env, ctx) to caret
    }

    fun addLink(sheet: MemoSheet, target: TargetRef, env: RecorderEnv, ctx: RecorderEditContext): MemoSheet =
        withLinks(sheet, LinkEdits.add(sheet.draft.pendingLinks, DRAFT_RECORD, target, LinkOrigin.MANUAL, ctx), env)

    fun removeLink(sheet: MemoSheet, linkKey: Key, env: RecorderEnv): MemoSheet =
        withLinks(sheet, LinkEdits.remove(sheet.draft.pendingLinks, linkKey, sheet.draft.runs), env)

    fun replaceLink(sheet: MemoSheet, linkKey: Key, target: TargetRef, env: RecorderEnv, ctx: RecorderEditContext): MemoSheet =
        withLinks(sheet, LinkEdits.replace(sheet.draft.pendingLinks, linkKey, target, sheet.draft.runs, ctx), env)

    fun setHideSwitch(sheet: MemoSheet, on: Boolean): MemoSheet = if (sheet.hideSwitch == null) sheet else sheet.copy(hideSwitch = on)

    fun canSave(sheet: MemoSheet): Boolean = !sheet.draft.runs.isBlankText()

    fun view(sheet: MemoSheet, env: RecorderEnv): MemoSheetView = MemoSheetView(
        title = RecorderCopy.TEXT_SHEET_TITLE,
        close = RecorderCopy.CLOSE,
        text = Runs.text(sheet.draft.runs),
        chips = Chips.captureRow(sheet.draft.pendingLinks, sheet.draft.runs, env),
        addLink = RecorderCopy.ADD_LINK,
        hideRow = RecordHiding.row(sheet.hideSwitch),
        toVoiceAria = RecorderCopy.TO_VOICE,
        hint = RecorderCopy.AT_HINT,
        save = RecorderCopy.SAVE,
        canSave = canSave(sheet),
    )

    /**
     * `저장`: a text record (state `정리 안 됨`, time = now) with its links; hidden when the switch is
     * on. After a switch from voice the record keeps the recording's key, duration and kind voice.
     */
    fun save(sheet: MemoSheet, library: RecorderLibrary, ctx: RecorderEditContext): Pair<RecorderLibrary, Key> {
        require(canSave(sheet)) { "empty text memo" }
        val voice = sheet.carriedVoice
        val key = voice?.recordKey ?: ctx.newKey()
        val existing = library.record(key)
        val runs = Runs.normalize(sheet.draft.runs)
        val record = existing?.copy(
            runs = runs, textRevision = if (runs != existing.runs) existing.textRevision + 1 else existing.textRevision,
            hidden = sheet.hideSwitch == true, updatedAt = ctx.now, updatedBy = ctx.device,
        ) ?: RecorderRecord(
            key = key,
            kind = (if (voice != null) RecordKind.VOICE else RecordKind.TEXT).open(),
            createdAt = if (voice != null) runCatching { Keys.timestampOf(key) }.getOrDefault(ctx.now) else ctx.now,
            runs = runs,
            durationMs = voice?.durationMs,
            hidden = sheet.hideSwitch == true,
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        val records = if (existing == null) library.records + record else library.records.map { if (it.key == key) record else it }
        val links = LinkEdits.syncAt(sheet.draft.pendingLinks.map { it.copy(recordKey = key) }, key, runs, ctx)
        return LinkEdits.replaceLinksOf(library.copy(records = records), key, links) to key
    }

    /** `닫기`: a non-empty draft is kept for the next opening ([PROPOSED]); an empty one is dropped. */
    fun close(sheet: MemoSheet): TextMemoDraft? = if (canSave(sheet)) sheet.draft else null

    /**
     * `말로 기록으로 바꾸기` (LRC-16): recording starts; the typed text becomes the start of the
     * transcript and the links carry over, as does the entry context.
     */
    fun toVoice(sheet: MemoSheet, recordKey: Key, audioTempPath: String): RecordingSession = RecordingSession(
        recordKey = recordKey,
        entry = sheet.draft.entry,
        phase = RecordingPhase.RECORDING,
        confirmedRuns = sheet.draft.runs,
        pendingLinks = sheet.draft.pendingLinks.map { it.copy(recordKey = recordKey) },
        hideSwitch = sheet.hideSwitch,
        audioTempPath = audioTempPath,
    )

    private fun withLinks(sheet: MemoSheet, links: List<RecorderLink>, env: RecorderEnv) =
        sheet.copy(draft = sheet.draft.copy(pendingLinks = links), hideSwitch = RecordHiding.hideSwitch(sheet.hideSwitch, links, env))
}
