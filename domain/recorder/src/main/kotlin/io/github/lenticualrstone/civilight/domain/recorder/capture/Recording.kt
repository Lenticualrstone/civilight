package io.github.lenticualrstone.civilight.domain.recorder.capture

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.RecorderTimeLabels
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.hide.HideRow
import io.github.lenticualrstone.civilight.domain.recorder.hide.RecordHiding
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.Insertion
import io.github.lenticualrstone.civilight.domain.recorder.put.InsertionException
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.PositionSheets
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.recorder.TextMemoDraft
import io.github.lenticualrstone.civilight.model.recorder.TranscriptionMode
import io.github.lenticualrstone.civilight.model.text.TextRun

/** What `녹음 중` shows (LRC-07). [live] = the live mark pulses (not while paused). */
data class RecordingScreen(
    val title: String,
    val timer: String,
    val live: Boolean,
    val cancel: String,
    val chips: List<Chip>,
    val addLink: String,
    val transcript: TranscriptView,
    val toText: String,
    val stopAndSave: String,
    /** `일시정지`, or the resume label id while paused (TODO_COPY_LRC_02). */
    val pauseLabel: String,
)

/** What `정리 중` shows (LRC-13). [finalising] = the label `받아쓰는 중 · 문장 나누기` and the glow sweep. */
data class ProcessingScreen(
    val title: String,
    val duration: String,
    val finalising: Boolean,
    val sectionLabel: String?,
    val text: String,
    val chips: List<Chip>,
    val addLink: String,
    val hideRow: HideRow?,
    val note: String,
    /** `여기 넣기 · #044 뒤`, only from the script (LRC-34). */
    val putHere: String?,
    /** `이어서 말하기` · `기록함에 두기`. */
    val buttons: List<String>,
)

/** The mic of a command bar, the tile or a widget was tapped (LRC-01 … LRC-06). */
enum class MicAction { START_RECORDING, OPEN_RUNNING, SHOW_MIC_ERROR, REQUEST_UNLOCK }

/** System back during a capture: never discards ([PROPOSED] LRC-07, LRC-13). */
enum class BackAction { MINIMISE, KEEP_IN_INBOX }

/** A recording discarded by `취소`, kept for the toast's `되돌리기` (`ED` LRC-07). */
data class CancelledRecording(val session: RecordingSession, val toast: Toast)

/**
 * The recording session as a reducer (LRC-07 … LRC-13). Silence never stops, pauses or saves:
 * only `멈추고 저장` creates the record (LRC-08). The record is written at `멈추고 저장`, so it exists
 * from then on whatever happens on `정리 중`; link and hide changes made on `정리 중` are saved by
 * `기록함에 두기` (or `여기 넣기`, or back).
 */
object Recording {
    fun micTap(permissionGranted: Boolean, running: RecordingSession?, onLockScreen: Boolean, settings: RecorderSettings): MicAction = when {
        running != null -> MicAction.OPEN_RUNNING
        !permissionGranted -> MicAction.SHOW_MIC_ERROR
        onLockScreen && !settings.lockScreenRecording -> MicAction.REQUEST_UNLOCK
        else -> MicAction.START_RECORDING
    }

    /** The link picker on the lock screen first asks for device unlock ([PROPOSED] Q7). */
    fun linkPickerNeedsUnlock(session: RecordingSession): Boolean = session.entry.onLockScreen

    /** Starts at once with the pre-filled `보던 곳` link (LRC-19) and the LRC-42 switch. */
    fun start(
        recordKey: Key,
        entry: EntryContext,
        settings: RecorderSettings,
        env: RecorderEnv,
        audioTempPath: String,
        ctx: RecorderEditContext,
    ): RecordingSession {
        val links = EntryLinks.initialLinks(recordKey, entry, settings, ctx)
        return RecordingSession(
            recordKey = recordKey, entry = entry, phase = RecordingPhase.RECORDING, pendingLinks = links,
            hideSwitch = RecordHiding.hideSwitch(null, links, env), audioTempPath = audioTempPath,
        )
    }

    fun pause(s: RecordingSession) = if (s.phase == RecordingPhase.RECORDING) s.copy(phase = RecordingPhase.PAUSED) else s

    fun resume(s: RecordingSession) = if (s.phase == RecordingPhase.PAUSED) s.copy(phase = RecordingPhase.RECORDING) else s

    /** Audio focus lost (incoming call): pauses until the user resumes ([PROPOSED] LRC-09). */
    fun focusLost(s: RecordingSession) = pause(s)

    /** Recorded time passes; paused time is not counted. */
    fun elapse(s: RecordingSession, ms: Long) = if (s.phase == RecordingPhase.RECORDING) s.copy(elapsedMs = s.elapsedMs + ms) else s

    /** Silence of any length is just recorded time: no auto-stop (LRC-08). */
    fun silence(s: RecordingSession, ms: Long) = elapse(s, ms)

    /** A partial result replaces the unconfirmed tail. */
    fun partial(s: RecordingSession, text: String) = if (s.phase == RecordingPhase.RECORDING) s.copy(partialText = text) else s

    /** A final result joins the confirmed text; the tail clears. */
    fun final(s: RecordingSession, text: String) =
        if (s.phase == RecordingPhase.PROCESSING) s else s.copy(confirmedRuns = Transcripts.appendFinal(s.confirmedRuns, text), partialText = "")

    fun transcript(s: RecordingSession): TranscriptView =
        TranscriptView(stateLabel(s.phase), Runs.text(s.confirmedRuns), s.partialText)

    /**
     * (0.2.0) The capture state label: `듣는 중` only while the microphone is open; a paused take
     * (the microphone closed and released) reads `일시정지`, never `듣는 중`.
     */
    fun stateLabel(phase: RecordingPhase): String =
        if (phase == RecordingPhase.PAUSED) RecorderCopy.PAUSED else RecorderCopy.LISTENING

    // --- Links on the capture screens ---

    fun addLink(s: RecordingSession, target: TargetRef, env: RecorderEnv, ctx: RecorderEditContext): RecordingSession =
        withLinks(s, LinkEdits.add(s.pendingLinks, s.recordKey, target, LinkOrigin.MANUAL, ctx), env)

    fun removeLink(s: RecordingSession, linkKey: Key, env: RecorderEnv): RecordingSession =
        withLinks(s, LinkEdits.remove(s.pendingLinks, linkKey, s.confirmedRuns), env)

    fun replaceLink(s: RecordingSession, linkKey: Key, target: TargetRef, env: RecorderEnv, ctx: RecorderEditContext): RecordingSession =
        withLinks(s, LinkEdits.replace(s.pendingLinks, linkKey, target, s.confirmedRuns, ctx), env)

    /** The `숨기기` switch on `정리 중`. */
    fun setHideSwitch(s: RecordingSession, on: Boolean): RecordingSession = if (s.hideSwitch == null) s else s.copy(hideSwitch = on)

    private fun withLinks(s: RecordingSession, links: List<RecorderLink>, env: RecorderEnv) =
        s.copy(pendingLinks = links, hideSwitch = RecordHiding.hideSwitch(s.hideSwitch, links, env))

    // --- Stop, save, continue, cancel ---

    /**
     * `멈추고 저장`: capture stops; the record is created (kind voice, state `정리 안 됨`, duration =
     * recorded time) or, after `이어서 말하기`, updated (text appended, duration grown, text revision
     * +1). The unconfirmed tail is taken as final. [startedAt] defaults to the time in the ULID
     * minted at start.
     */
    fun stopAndSave(
        s: RecordingSession,
        library: RecorderLibrary,
        ctx: RecorderEditContext,
        startedAt: Timestamp? = null,
        audio: RecorderAudio? = null,
        transcribedBy: TranscriptionMode = TranscriptionMode.ON_DEVICE,
    ): Pair<RecordingSession, RecorderLibrary> {
        if (s.phase == RecordingPhase.PROCESSING) return s to library
        val runs = Runs.normalize(Transcripts.appendFinal(s.confirmedRuns, s.partialText))
        val existing = library.record(s.recordKey)
        val record = if (existing == null) {
            RecorderRecord(
                key = s.recordKey, kind = RecordKind.VOICE.open(),
                createdAt = startedAt ?: runCatching { Keys.timestampOf(s.recordKey) }.getOrDefault(ctx.now),
                runs = runs, durationMs = s.elapsedMs, audio = audio, transcribedBy = transcribedBy.open(),
                updatedAt = ctx.now, updatedBy = ctx.device,
            )
        } else {
            existing.copy(
                runs = runs, durationMs = s.elapsedMs, audio = audio ?: existing.audio,
                textRevision = if (runs != existing.runs) existing.textRevision + 1 else existing.textRevision,
                updatedAt = ctx.now, updatedBy = ctx.device,
            )
        }
        val records = if (existing == null) library.records + record else library.records.map { if (it.key == record.key) record else it }
        val next = LinkEdits.replaceLinksOf(library.copy(records = records), s.recordKey, s.pendingLinks)
        return s.copy(phase = RecordingPhase.PROCESSING, confirmedRuns = runs, partialText = "") to next
    }

    /** `이어서 말하기`: back to recording the same record; timer resumes; new text is appended. */
    fun continueSpeaking(s: RecordingSession, library: RecorderLibrary): RecordingSession {
        if (s.phase != RecordingPhase.PROCESSING) return s
        val record = library.record(s.recordKey)
        return s.copy(
            phase = RecordingPhase.RECORDING,
            confirmedRuns = record?.runs ?: s.confirmedRuns,
            elapsedMs = record?.durationMs ?: s.elapsedMs,
            partialText = "",
        )
    }

    /**
     * `기록함에 두기` (and system back on `정리 중`): the link and hide changes are saved; the recorder
     * surface closes. No toast ([PROPOSED]).
     */
    fun keepInInbox(s: RecordingSession, library: RecorderLibrary, ctx: RecorderEditContext): RecorderLibrary {
        require(library.record(s.recordKey) != null) { "the record is written by stopAndSave first" }
        val withLinks = LinkEdits.replaceLinksOf(library, s.recordKey, s.pendingLinks)
        return RecordHiding.setHidden(withLinks, s.recordKey, s.hideSwitch == true, ctx)
    }

    /** `여기 넣기 · #044 뒤`: saves like [keepInInbox], then inserts right after the caret block. */
    fun putHere(s: RecordingSession, library: RecorderLibrary, env: RecorderEnv, ctx: RecorderEditContext): Insertion {
        val spot = PositionSheets.putHereSpot(s.entry, env) ?: throw InsertionException("put-here is not available for this entry")
        val saved = keepInInbox(s, library, ctx)
        val doc = env.project(s.entry.projectKey!!)!!.doc
        return ManuscriptInsert.insert(doc, saved, s.recordKey, spot, ctx)
    }

    /**
     * `취소` while recording. A first take discards audio and transcript (no record) with an undo
     * toast; during a continued take only the new take is dropped and `정리 중` returns (`ED`).
     */
    fun cancel(s: RecordingSession, library: RecorderLibrary): Pair<RecordingSession?, CancelledRecording?> {
        val record = library.record(s.recordKey)
        if (record != null) {
            return s.copy(phase = RecordingPhase.PROCESSING, confirmedRuns = record.runs, partialText = "", elapsedMs = record.durationMs ?: 0) to null
        }
        return null to CancelledRecording(s, Toast(RecorderCopy.TODO_CANCELLED, listOf(RecorderCopy.UNDO)))
    }

    /** The cancel toast's `되돌리기`: the discarded recording comes back into `정리 중`. */
    fun undoCancel(cancelled: CancelledRecording, library: RecorderLibrary, ctx: RecorderEditContext, startedAt: Timestamp? = null) =
        stopAndSave(cancelled.session.copy(phase = RecordingPhase.PAUSED), library, ctx, startedAt)

    fun back(s: RecordingSession): BackAction = if (s.phase == RecordingPhase.PROCESSING) BackAction.KEEP_IN_INBOX else BackAction.MINIMISE

    // --- Screens ---

    fun recordingScreen(s: RecordingSession, env: RecorderEnv): RecordingScreen = RecordingScreen(
        title = RecorderCopy.APP_NAME,
        timer = RecorderTimeLabels.timer(s.elapsedMs),
        live = s.phase == RecordingPhase.RECORDING,
        cancel = RecorderCopy.CANCEL,
        chips = Chips.captureRow(s.pendingLinks, s.confirmedRuns, env),
        addLink = RecorderCopy.ADD_LINK,
        transcript = transcript(s),
        toText = RecorderCopy.TO_TEXT,
        stopAndSave = RecorderCopy.STOP_AND_SAVE,
        pauseLabel = if (s.phase == RecordingPhase.PAUSED) RecorderCopy.TODO_RESUME else RecorderCopy.PAUSE,
    )

    /**
     * `정리 중`. On the lock screen only `이어서 말하기` · `기록함에 두기` (no `여기 넣기`, LRC-05).
     * [audioKept] picks the note (`원본 음성 함께 보관 …` or TODO_COPY_LRC_06).
     */
    fun processingScreen(s: RecordingSession, env: RecorderEnv, finalising: Boolean, audioKept: Boolean): ProcessingScreen = ProcessingScreen(
        title = RecorderCopy.APP_NAME,
        duration = RecorderTimeLabels.timer(s.elapsedMs),
        finalising = finalising,
        sectionLabel = if (finalising) RecorderCopy.FINALISING else null,
        text = Runs.text(s.confirmedRuns),
        chips = Chips.captureRow(s.pendingLinks, s.confirmedRuns, env),
        addLink = RecorderCopy.ADD_LINK,
        hideRow = RecordHiding.row(s.hideSwitch),
        note = if (audioKept) RecorderCopy.AUDIO_KEPT_NOTE else RecorderCopy.TODO_AUDIO_NOT_KEPT,
        putHere = PositionSheets.putHereLabel(s.entry, env),
        buttons = listOf(RecorderCopy.CONTINUE_SPEAKING, RecorderCopy.KEEP_IN_INBOX),
    )

    /** `글로 쓰기` during recording (LRC-16, `ED` Q10): the text sheet with the transcript so far and the same links; the record stays voice. */
    fun switchToText(s: RecordingSession, env: RecorderEnv, now: Timestamp): MemoSheet {
        val runs: List<TextRun> = Runs.normalize(Transcripts.appendFinal(s.confirmedRuns, s.partialText))
        return MemoSheet(
            draft = TextMemoDraft(runs, s.pendingLinks, s.entry, now),
            hideSwitch = RecordHiding.hideSwitch(s.hideSwitch, s.pendingLinks, env),
            carriedVoice = CarriedVoice(s.recordKey, s.elapsedMs, s.audioTempPath),
        )
    }
}
