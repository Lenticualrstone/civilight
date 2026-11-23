package io.github.lenticualrstone.civilight.feature.recorder.capture

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.RecorderActions
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.capture.CancelledRecording
import io.github.lenticualrstone.civilight.domain.recorder.capture.MemoSheet
import io.github.lenticualrstone.civilight.domain.recorder.capture.MicAction
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.capture.TextMemo
import io.github.lenticualrstone.civilight.domain.recorder.capture.Transcripts
import io.github.lenticualrstone.civilight.domain.recorder.hide.RecordHiding
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkCandidate
import io.github.lenticualrstone.civilight.domain.recorder.put.InsertionException
import io.github.lenticualrstone.civilight.domain.recorder.put.PositionSheets
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.env.EnvSource
import io.github.lenticualrstone.civilight.feature.recorder.memo.MemoEditing
import io.github.lenticualrstone.civilight.feature.recorder.outside.AudioFocusGate
import io.github.lenticualrstone.civilight.feature.recorder.audio.AudioTakes
import io.github.lenticualrstone.civilight.feature.recorder.audio.NoAudioTakes
import io.github.lenticualrstone.civilight.feature.recorder.speech.ChosenEngine
import io.github.lenticualrstone.civilight.feature.recorder.speech.EngineChoice
import io.github.lenticualrstone.civilight.feature.recorder.speech.SpeechEngine
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.feature.recorder.speech.SpeechListener
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.recorder.TranscriptionMode
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The one capture of the app (LRC-01 … LRC-20): recording, `정리 중`, the text memo and the link
 * picker. It lives for the process, not for a screen: system back only minimises a recording
 * (LRC-07), and a second mic tap reopens the running one (LRC-01). Rules are `domain:recorder`
 * (`Recording`, `TextMemo`, `LinkPicker`); this class sequences them, runs the timer and the
 * speech engine, and writes the library through [recorder].
 *
 * Call from the main thread. Suspend functions finish their writes before returning.
 */
class CaptureHub(
    private val scope: CoroutineScope,
    private val recorder: RecorderPort,
    private val envSource: EnvSource,
    private val projects: ProjectPort,
    private val settings: () -> AppSettings,
    private val context: () -> ActionContext,
    private val monotonicMs: () -> Long,
    private val audioTempPath: (Key) -> String,
    private val engineFactory: (TranscriptionMode) -> SpeechEngine,
    private var focus: AudioFocusGate = AudioFocusGate.None,
    private val takes: AudioTakes = NoAudioTakes,
    private var online: () -> Boolean = { false },
) {
    /** The engine of the current take (LRC-11, LRC-12), chosen when a take starts. */
    private var chosen: ChosenEngine? = null
    private val engine: SpeechEngine get() = (chosen ?: chooseEngine()).engine
    private val _state = MutableStateFlow(CaptureState())
    private val _level = MutableStateFlow(0f)

    val state: StateFlow<CaptureState> = _state.asStateFlow()

    /** Input level 0…1 for the glow (LRC-10); not part of [state] so the screen does not recompose per sample. */
    val level: StateFlow<Float> = _level.asStateFlow()

    private var tickerJob: Job? = null
    private var lastTick = 0L
    private var sincePersist = 0L
    private var engineToken = 0
    private var micDenied = false

    /** The take was paused by [holdForTour] (not by the user or a call), so [releaseTour] resumes it. */
    private var pausedForTour = false

    /** The shared memo sheet was dismissed by its host (scrim, back) while the hub still holds the memo. */
    private var memoDetached = false

    /** Confirmed runs while `정리 중` refines the last take (late final results replace the tail taken at stop). */
    private var finalBase: List<TextRun>? = null
    private var persistPending = false
    private var draftPending = false

    private val s: CaptureState get() = _state.value

    /** The platform audio focus, installed once by the first capture screen (it needs a context). */
    fun usePlatformFocus(gate: AudioFocusGate) {
        if (focus === AudioFocusGate.None) focus = gate
    }

    /** The platform network check (LRC-12), installed by the first capture screen (it needs a context). */
    fun useNetwork(network: () -> Boolean) {
        online = network
    }

    /** Start-up: takes nobody brought back (a `취소` never undone) are deleted, except [keep]'s. */
    fun pruneTakes(keep: Key?) = takes.prune(setOfNotNull(keep))

    /** Picks the engine for a new take from the setting and the network (LRC-12). */
    private fun chooseEngine(): ChosenEngine =
        EngineChoice.choose(settings().recorder.transcription, online(), engineFactory).also { chosen = it }

    /**
     * The take's audio kept in the blob store (LRC-14) when `원본 음성 보관` is on, with its envelope;
     * null when it is off or nothing was captured.
     */
    private suspend fun keptAudio(recordKey: Key, durationMs: Long): RecorderAudio? {
        if (!settings().recorder.keepAudio) return null
        val take = takes.take(recordKey) ?: return null
        val blob = recorder.importAudio(take.path, take.mime, durationMs) ?: return null
        val envelope = take.envelope.takeIf { it.isNotEmpty() }?.let(RecorderAudio::encodeEnvelope)
        return RecorderAudio(hash = blob.hash, mime = blob.mime, bytes = blob.bytes, durationMs = durationMs, envelope = envelope)
    }

    /** Runs [block] in the hub's own scope, so writes finish even after the screen left. */
    fun launch(block: suspend CaptureHub.() -> Unit): Job = scope.launch { block() }

    // ---- start (LRC-01, LRC-06) ----

    /**
     * A mic tap or the `Recording` route opened. A running capture is shown again as it is (LRC-01);
     * [startTyped] opens the text memo (LRC-15); otherwise recording starts at once, or the mic
     * permission is asked for first (LRC-06).
     */
    suspend fun begin(entry: EntryContext, startTyped: Boolean, micGranted: Boolean) {
        val now = s
        when {
            now.stage == CaptureStage.RECORDING || now.stage == CaptureStage.PROCESSING -> return
            now.stage == CaptureStage.TYPING && now.voiceFromMemo -> memoToVoice(micGranted)
            now.stage == CaptureStage.TYPING && startTyped -> showMemo(entry)
            startTyped -> openMemo(entry)
            else -> when (Recording.micTap(micGranted, null, entry.onLockScreen, settings().recorder)) {
                MicAction.SHOW_MIC_ERROR -> _state.update {
                    it.copy(stage = if (micDenied) CaptureStage.MIC_OFF else CaptureStage.MIC_REASON, entry = entry, memo = null, voiceFromMemo = false)
                }
                // `잠금 화면에서 녹음` off: the device unlock comes first (LRC-05); the screen asks for it.
                MicAction.REQUEST_UNLOCK -> _state.value = CaptureState(entry = entry, unlock = UnlockRequest.Start(entry), env = s.env)
                MicAction.START_RECORDING, MicAction.OPEN_RUNNING -> start(entry)
            }
        }
    }

    /** The system prompt answered (after the reason line). */
    suspend fun permissionResult(granted: Boolean) {
        if (!granted) {
            micDenied = true
            _state.update { it.copy(stage = CaptureStage.MIC_OFF) }
            return
        }
        micDenied = false
        val now = s
        when {
            now.voiceFromMemo -> memoToVoice(true)
            now.entry != null -> start(now.entry)
        }
    }

    /**
     * The device unlock asked for by [CaptureState.unlock] answered (LRC-05). Unlocked: the start
     * goes on, or the link picker opens; the capture now counts as off the lock screen for links.
     * Cancelled: the recording screen stays as it was; a start that never began closes.
     */
    fun unlockResult(granted: Boolean) {
        val request = s.unlock ?: return
        _state.update { it.copy(unlock = null, deviceUnlocked = it.deviceUnlocked || granted) }
        when (request) {
            is UnlockRequest.Start -> if (granted) start(request.entry.copy(onLockScreen = false)) else close()
            is UnlockRequest.Picker -> if (granted) openPicker(request.picker)
        }
    }

    /** `글로 쓰기` on `마이크 꺼짐`: the text memo with the same entry context (the memo itself after a memo's mic). */
    suspend fun micOffToText() {
        val now = s
        if (now.memo != null) {
            _state.update { it.copy(stage = CaptureStage.TYPING, voiceFromMemo = false) }
        } else {
            openMemo(now.entry ?: return)
        }
    }

    /** `닫기` on the reason line or on `마이크 꺼짐`; back to the memo when it was the memo's mic. */
    fun dismissMic() {
        if (s.memo != null) _state.update { it.copy(stage = CaptureStage.TYPING, voiceFromMemo = false) }
        else _state.value = CaptureState(env = s.env)
    }

    // ---- recording (LRC-07, LRC-08) ----

    private fun start(entry: EntryContext) {
        pausedForTour = false
        val ctx = context()
        val env = envSource.quick()
        val fullEntry = withExample(entry, env)
        val key = ctx.newKey()
        val session = Recording.start(key, fullEntry, settings().recorder, env, audioTempPath(key), ctx.recorder)
        val engineNow = chooseEngine().engine
        _state.value = CaptureState(stage = CaptureStage.RECORDING, entry = fullEntry, session = session, env = env, engineUnavailable = !engineNow.available)
        takes.start(key)
        startListening()
        refreshEnv()
    }

    /**
     * Start-up recovery (LRC-09, `[PROPOSED]`): the recording that was running when the process
     * ended. A take still recording or paused is saved with what was captured and opens in
     * `정리 중`; a `정리 중` comes back with its unsaved link and hide edits. A take with nothing in it
     * (no time, no text, no record yet) is dropped. Returns the entry to show, or null.
     */
    suspend fun recover(saved: RecordingSession): EntryContext? {
        if (s.active) return null
        recorder.ensureLoaded()
        val env = envSource.load()
        val existing = recorder.current().record(saved.recordKey)
        val empty = saved.elapsedMs <= 0 && Runs.text(saved.confirmedRuns).isBlank() && saved.partialText.isBlank()
        if (existing == null && (saved.phase == RecordingPhase.PROCESSING || empty)) {
            recorder.saveSession(null)
            return null
        }
        var session = saved
        if (saved.phase != RecordingPhase.PROCESSING) {
            val ctx = context()
            takes.recoverSegment(saved.recordKey)
            val audio = keptAudio(saved.recordKey, saved.elapsedMs)
            recorder.update { lib ->
                val (next, library) = Recording.stopAndSave(saved.copy(phase = RecordingPhase.PAUSED), lib, ctx.recorder, audio = audio, transcribedBy = transcription())
                session = next
                library
            }
        }
        _state.value = CaptureState(
            stage = CaptureStage.PROCESSING, entry = session.entry, session = session.copy(hideSwitch = RecordHiding.hideSwitch(session.hideSwitch, session.pendingLinks, env)),
            env = env, audioKept = recorder.current().record(session.recordKey)?.audio != null,
        )
        recorder.saveSession(_state.value.session)
        return session.entry
    }

    /** The engine that transcribed this take (the record's `transcribedBy`); on-device when none ran. */
    private fun transcription(): TranscriptionMode = chosen?.mode ?: TranscriptionMode.ON_DEVICE

    fun togglePause() {
        val session = s.session ?: return
        when (session.phase) {
            RecordingPhase.RECORDING -> {
                stopTicker()
                setSession(Recording.pause(s.session!!))
                engine.pause()
                takes.pause()
                focus.abandon()
                _level.value = 0f
            }
            RecordingPhase.PAUSED -> {
                setSession(Recording.resume(session))
                focus.request(::focusLost)
                engine.resume()
                takes.resume()
                startTicker()
            }
            RecordingPhase.PROCESSING -> Unit
        }
        persistSoon()
    }

    /**
     * LIB-19: the recorder tour's card is shown over `녹음 중`. A running take pauses, so the time spent
     * reading the cards is not recorded; [releaseTour] resumes it. A take already paused stays as it is.
     */
    fun holdForTour() {
        if (s.stage != CaptureStage.RECORDING || s.session?.phase != RecordingPhase.RECORDING) return
        togglePause()
        pausedForTour = true
    }

    /** The tour's card closed: the take [holdForTour] paused resumes (one the user paused stays paused). */
    fun releaseTour() {
        val resume = pausedForTour && s.stage == CaptureStage.RECORDING && s.session?.phase == RecordingPhase.PAUSED
        pausedForTour = false
        if (resume) togglePause()
    }

    /** Audio focus lost (a call): paused until the user resumes (LRC-09). */
    fun focusLost() {
        if (s.session?.phase == RecordingPhase.RECORDING) togglePause()
    }

    /**
     * `멈추고 저장`: the record is written now (LRC-13: it exists whatever happens next), then
     * `정리 중` shows while the engine delivers its last result.
     */
    suspend fun stopAndSave() {
        if (s.session == null || s.stage != CaptureStage.RECORDING) return
        stopTicker()
        val session = s.session ?: return
        finalBase = Runs.normalize(session.confirmedRuns)
        _state.update { it.copy(stage = CaptureStage.PROCESSING, finalising = true) }
        _level.value = 0f
        focus.abandon()
        engine.stop()
        takes.finish(session.recordKey)
        val ctx = context()
        val mode = transcription()
        val audio = keptAudio(session.recordKey, (s.session ?: session).elapsedMs)
        var next: RecordingSession = session
        recorder.update { lib ->
            val (saved, library) = Recording.stopAndSave(s.session ?: session, lib, ctx.recorder, audio = audio, transcribedBy = mode)
            next = saved
            library
        }
        setSession(next)
        _state.update { it.copy(audioKept = recorder.current().record(next.recordKey)?.audio != null) }
        recorder.saveSession(next)
    }

    /** `이어서 말하기`: the same record continues; the timer resumes; new text is appended. */
    fun continueSpeaking() {
        val session = s.session ?: return
        if (s.stage != CaptureStage.PROCESSING) return
        finalBase = null
        setSession(Recording.continueSpeaking(session, recorder.current()))
        _state.update { it.copy(stage = CaptureStage.RECORDING, finalising = false) }
        takes.start(session.recordKey)
        startListening()
    }

    /** `기록함에 두기` (and back on `정리 중`): links and the hide switch are saved; the capture closes. */
    suspend fun keepInInbox() {
        val session = s.session ?: return
        if (s.stage != CaptureStage.PROCESSING) return
        val ctx = context()
        close()
        recorder.update { Recording.keepInInbox(session, it, ctx.recorder) }
        recorder.saveSession(null)
        takes.discard(session.recordKey)
    }

    /** `취소` (LRC-07, `ED` §6 Q8). */
    suspend fun cancel(): CancelOutcome {
        if (s.session == null || s.stage != CaptureStage.RECORDING) return CancelOutcome.Nothing
        stopTicker()
        val session = s.session ?: return CancelOutcome.Nothing
        engineToken++
        engine.stop()
        focus.abandon()
        _level.value = 0f
        val (back, cancelled) = Recording.cancel(session, recorder.current())
        // A continued take drops only its new segment; a first take keeps its file for the toast's undo (pruned at start-up otherwise).
        if (back != null) takes.drop(session.recordKey) else takes.finish(session.recordKey)
        return if (back != null) {
            setSession(back)
            _state.update { it.copy(stage = CaptureStage.PROCESSING, finalising = false) }
            recorder.saveSession(back)
            CancelOutcome.BackToProcessing
        } else {
            close()
            recorder.saveSession(null)
            CancelOutcome.Discarded(cancelled!!)
        }
    }

    /** The `취소` toast's `되돌리기`: the discarded recording comes back into `정리 중` (and is saved). */
    suspend fun undoCancel(cancelled: CancelledRecording) {
        if (s.active) return
        val ctx = context()
        var next: RecordingSession? = null
        recorder.update { lib ->
            val (session, library) = Recording.undoCancel(cancelled, lib, ctx.recorder)
            next = session
            library
        }
        val audio = keptAudio(cancelled.session.recordKey, cancelled.session.elapsedMs)
        if (audio != null) recorder.update { CaptureRules.withAudio(it, cancelled.session.recordKey, audio) }
        val env = envSource.quick()
        _state.value = CaptureState(stage = CaptureStage.PROCESSING, entry = cancelled.session.entry, session = next, env = env, audioKept = audio != null)
        recorder.saveSession(next)
        refreshEnv()
    }

    /** System back: a recording only minimises (never discards); `정리 중` keeps the record in the inbox. */
    suspend fun back() {
        when (s.stage) {
            CaptureStage.PROCESSING -> keepInInbox()
            CaptureStage.RECORDING -> Unit
            CaptureStage.TYPING -> memoClose()
            CaptureStage.MIC_REASON, CaptureStage.MIC_OFF -> dismissMic()
            CaptureStage.IDLE -> Unit
        }
    }

    /** `여기 넣기 · #044 뒤` (LRC-34): saved like `기록함에 두기`, then one `⌁` memo block after the caret block. */
    suspend fun putHere(): PutHereOutcome? {
        val session = s.session ?: return null
        if (s.stage != CaptureStage.PROCESSING) return null
        val env = s.env
        val spot = PositionSheets.putHereSpot(session.entry, env) ?: return null
        val projectKey = session.entry.projectKey ?: return null
        val ctx = context()
        val saved = Recording.keepInInbox(session, recorder.current(), ctx.recorder)
        val insertion = (
            try {
                projects.edit(projectKey, RecorderCopy.PUT_HERE) { doc ->
                    val ins = RecorderActions.putInManuscript(doc, saved, session.recordKey, spot, ctx)
                    ins.doc to ins
                }
            } catch (e: InsertionException) {
                null
            }
            ) ?: return null
        close()
        envSource.forget(projectKey)
        recorder.update { insertion.library }
        recorder.saveSession(null)
        takes.discard(session.recordKey)
        return PutHereOutcome(projectKey, insertion)
    }

    /** The `여기 넣기` toast's `되돌리기`: the block, the placement and an added link go (no trash entry). */
    suspend fun undoPut(outcome: PutHereOutcome) {
        var library: RecorderLibrary? = null
        projects.edit(outcome.projectKey, RecorderCopy.UNDO) { doc ->
            val (d, l) = RecorderActions.undoPut(doc, recorder.current(), outcome.insertion.undo)
            library = l
            d to Unit
        } ?: return
        envSource.forget(outcome.projectKey)
        library?.let { l -> recorder.update { l } }
    }

    // ---- links (LRC-18 … LRC-20) ----

    /** `+ 연결` or a chip tap. On the lock screen the device unlock comes first (LRC-05, `[PROPOSED]` §6 Q7). */
    fun openPicker(request: PickerRequest) {
        val session = s.session
        if (request.target == PickerTarget.SESSION && session != null && Recording.linkPickerNeedsUnlock(session) && !s.deviceUnlocked) {
            _state.update { it.copy(unlock = UnlockRequest.Picker(request)) }
            return
        }
        _state.update { it.copy(picker = request) }
        refreshEnv()
    }

    fun closePicker() = _state.update { it.copy(picker = null) }

    /** A target chosen in the picker: added, or replacing the tapped chip's link. */
    fun pick(candidate: LinkCandidate) = pickTarget(candidate.target)

    fun pickTarget(target: TargetRef) {
        val request = s.picker ?: return
        val ctx = context().recorder
        val env = s.env
        when (request.target) {
            PickerTarget.SESSION -> {
                val session = s.session ?: return
                val next = if (request.replaceLinkKeys.isEmpty()) Recording.addLink(session, target, env, ctx)
                else Recording.replaceLink(
                    request.replaceLinkKeys.drop(1).fold(session) { acc, k -> Recording.removeLink(acc, k, env) },
                    request.replaceLinkKeys.first(), target, env, ctx,
                )
                setSession(next)
                persistSoon()
            }
            PickerTarget.MEMO -> {
                val memo = s.memo ?: return
                val next = if (request.replaceLinkKeys.isEmpty()) TextMemo.addLink(memo, target, env, ctx)
                else TextMemo.replaceLink(
                    request.replaceLinkKeys.drop(1).fold(memo) { acc, k -> TextMemo.removeLink(acc, k, env) },
                    request.replaceLinkKeys.first(), target, env, ctx,
                )
                setMemo(next)
            }
        }
        closePicker()
    }

    /** A chip's × (all links of a collapsed masked chip). */
    fun removeLinks(target: PickerTarget, linkKeys: List<Key>) {
        val env = s.env
        when (target) {
            PickerTarget.SESSION -> {
                val session = s.session ?: return
                setSession(linkKeys.fold(session) { acc, k -> Recording.removeLink(acc, k, env) })
                persistSoon()
            }
            PickerTarget.MEMO -> {
                val memo = s.memo ?: return
                setMemo(linkKeys.fold(memo) { acc, k -> TextMemo.removeLink(acc, k, env) })
            }
        }
    }

    /** The `숨기기` switch (LRC-42) on `정리 중` or the memo. */
    fun setHide(on: Boolean) {
        when (s.stage) {
            CaptureStage.TYPING -> s.memo?.let { setMemo(TextMemo.setHideSwitch(it, on)) }
            else -> s.session?.let {
                setSession(Recording.setHideSwitch(it, on))
                persistSoon()
            }
        }
    }

    // ---- text memo (LRC-15 … LRC-17) ----

    /**
     * Opens the memo sheet; a kept draft returns as it was. Returns false and changes nothing while a
     * recording or `정리 중` is running (one capture at a time): the caller shows that capture instead
     * (LRC-01), so a recording left with back is never dropped by a memo opened elsewhere.
     */
    suspend fun openMemo(entry: EntryContext): Boolean {
        if (s.stage == CaptureStage.RECORDING || s.stage == CaptureStage.PROCESSING) return false
        memoDetached = false
        val kept = recorder.readDraft()
        val env = envSource.quick()
        val ctx = context().recorder
        val fullEntry = withExample(entry, env)
        val memo = TextMemo.open(fullEntry, settings().recorder, env, kept, ctx)
        _state.value = CaptureState(
            stage = CaptureStage.TYPING, entry = fullEntry, memo = memo, env = env,
            memoCaret = Runs.text(memo.draft.runs).length, memoReload = s.memoReload + 1,
        )
        refreshEnv()
        return true
    }

    /** `글로 쓰기` while recording (LRC-16): the transcript so far and the links move to the memo; the record stays voice. */
    fun toText() {
        if (s.session == null || s.stage != CaptureStage.RECORDING) return
        stopTicker()
        val session = s.session ?: return
        if (session.entry.onLockScreen) return
        engineToken++
        engine.stop()
        takes.finish(session.recordKey)
        focus.abandon()
        _level.value = 0f
        val memo = Recording.switchToText(session, s.env, context().now)
        _state.update {
            it.copy(
                stage = CaptureStage.TYPING, session = null, memo = memo, finalising = false,
                memoCaret = Runs.text(memo.draft.runs).length, memoReload = it.memoReload + 1,
            )
        }
        launch { recorder.saveSession(null) }
        draftSoon()
    }

    /** The field changed to [text] with the caret at [caret]: one replace over the runs; `@` links follow their runs. */
    fun memoTextChanged(text: String, caret: Int) {
        val memo = s.memo ?: return
        val runs = MemoEditing.apply(memo.draft.runs, text, caret)
        val next = TextMemo.edit(memo, runs, s.env, context().recorder)
        val reload = Runs.text(next.draft.runs) != text
        _state.update { it.copy(memo = next, memoCaret = caret, memoReload = if (reload) it.memoReload + 1 else it.memoReload) }
        draftSoon()
    }

    fun memoCaretMoved(caret: Int) {
        if (s.memo != null && s.memoCaret != caret) _state.update { it.copy(memoCaret = caret) }
    }

    /** An `@` candidate chosen: the `@query` becomes a link run and a link of origin `@`. */
    fun memoPickAt(candidate: LinkCandidate) {
        val memo = s.memo ?: return
        val query = TextMemo.atQuery(memo, s.memoCaret) ?: return
        val (next, caret) = TextMemo.pick(memo, query, candidate, s.env, context().recorder)
        _state.update { it.copy(memo = next, memoCaret = caret, memoReload = it.memoReload + 1) }
        draftSoon()
    }

    /** `저장`: the record (text, or voice after a switch) with its links; the draft goes. */
    suspend fun memoSave(): Boolean {
        val memo = s.memo ?: return false
        if (!TextMemo.canSave(memo)) return false
        val ctx = context().recorder
        close()
        recorder.update { TextMemo.save(memo, it, ctx).first }
        recorder.saveDraft(null)
        // LRC-16: the audio of a recording switched to text stays attached to the voice record.
        memo.carriedVoice?.let { voice ->
            keptAudio(voice.recordKey, voice.durationMs)?.let { audio -> recorder.update { CaptureRules.withAudio(it, voice.recordKey, audio) } }
            takes.discard(voice.recordKey)
        }
        return true
    }

    /** `닫기` (or the sheet dismissed): a non-empty draft is kept for the next opening; an empty one goes. */
    suspend fun memoClose() {
        val memo = s.memo ?: return
        if (s.stage != CaptureStage.TYPING) return
        close()
        recorder.saveDraft(TextMemo.close(memo))
        // The kept draft holds text and links only; the switched recording's audio has nothing left to attach to.
        memo.carriedVoice?.let { takes.discard(it.recordKey) }
    }

    /** `말로 기록으로 바꾸기` (LRC-16): recording starts with the typed text as the start of the transcript. */
    suspend fun memoToVoice(micGranted: Boolean) {
        val memo = s.memo ?: return
        if (!micGranted) {
            _state.update {
                it.copy(stage = if (micDenied) CaptureStage.MIC_OFF else CaptureStage.MIC_REASON, voiceFromMemo = true)
            }
            return
        }
        val carried = memo.carriedVoice
        val key = carried?.recordKey ?: context().newKey()
        val session = TextMemo.toVoice(memo, key, audioTempPath(key)).let { if (carried != null) it.copy(elapsedMs = carried.durationMs) else it }
        val engineNow = chooseEngine().engine
        _state.update {
            it.copy(
                stage = CaptureStage.RECORDING, entry = memo.draft.entry, session = session, memo = null, voiceFromMemo = false,
                picker = null, finalising = false, audioKept = false, engineUnavailable = !engineNow.available,
            )
        }
        recorder.saveDraft(null)
        takes.start(key)
        startListening()
    }

    /**
     * The shared memo sheet left the screen without `닫기` or `저장` (scrim, drag, back, the host
     * screen went away): the memo closes as with `닫기`, so a non-empty draft is kept and the next
     * opening starts from its own entry. Not while the stacked link picker or the switch to voice
     * took the sheet's place.
     */
    fun memoSheetGone() {
        val now = s
        if (now.stage == CaptureStage.TYPING && now.picker == null && !now.voiceFromMemo) memoDetached = true
    }

    /**
     * The memo sheet is shown (again). Coming back from the stacked picker keeps the memo; a memo
     * whose sheet was dismissed is closed first (draft kept) and a new one opens for [entry].
     * False while a recording runs (see [openMemo]).
     */
    suspend fun showMemo(entry: EntryContext): Boolean {
        if (s.stage == CaptureStage.TYPING) {
            if (!memoDetached) return true
            memoClose()
        }
        return openMemo(entry)
    }

    /** The memo will turn into a recording on the `Recording` route (a memo shown in another screen's sheet). */
    fun requestVoiceFromMemo() {
        if (s.memo != null) _state.update { it.copy(voiceFromMemo = true) }
    }

    // ---- environment ----

    /** Reloads every project (links into closed projects need their documents) and re-derives the hide switch. */
    fun refreshEnv(): Job = launch {
        val env = envSource.load()
        _state.update { st ->
            st.copy(
                env = env,
                session = st.session?.let { it.copy(hideSwitch = RecordHiding.hideSwitch(it.hideSwitch, it.pendingLinks, env)) },
                memo = st.memo?.let { it.copy(hideSwitch = RecordHiding.hideSwitch(it.hideSwitch, it.draft.pendingLinks, env)) },
            )
        }
    }

    /** The env as the screens should use it now. */
    val env: RecorderEnv get() = s.env

    // ---- internals ----

    /**
     * The entry completed from the documents: marked as inside the onboarding example (its chip reads
     * `예시 · 끝나면 연결 없어짐`, LRC-19), and a script entry that names only its caret block gets that
     * block's scene as the pre-filled target (rule 23 §2: script → that scene).
     */
    private fun withExample(entry: EntryContext, env: RecorderEnv): EntryContext {
        val project = entry.projectKey?.let(env::project)
        val caretScene = if (entry.surface == EntrySurface.SCRIPT && entry.targetKey == null) {
            entry.caretBlockKey?.let { project?.doc?.index?.block(it)?.sceneKey }
        } else {
            null
        }
        return entry.copy(
            isExample = project?.isExample == true,
            targetKind = if (caretScene != null) RecorderTargetKind.UNIT else entry.targetKind,
            targetKey = caretScene ?: entry.targetKey,
        )
    }

    private fun close() {
        memoDetached = false
        pausedForTour = false
        focus.abandon()
        stopTicker()
        finalBase = null
        _level.value = 0f
        _state.value = CaptureState(env = s.env)
    }

    private fun setSession(session: RecordingSession) = _state.update { it.copy(session = session) }

    private fun setMemo(memo: MemoSheet) {
        _state.update { it.copy(memo = memo) }
        draftSoon()
    }

    private fun startListening() {
        val token = ++engineToken
        focus.request(::focusLost)
        engine.start(listenerFor(token))
        startTicker()
        persistSoon()
    }

    private fun listenerFor(token: Int) = object : SpeechListener {
        override fun onPartial(text: String) {
            if (token != engineToken) return
            s.session?.let { setSession(Recording.partial(it, text)) }
        }

        override fun onFinal(text: String) {
            if (token != engineToken) return
            val session = s.session ?: return
            when {
                s.stage == CaptureStage.RECORDING -> {
                    setSession(Recording.final(session, text))
                    persistSoon()
                }
                s.stage == CaptureStage.PROCESSING && s.finalising -> refine(text)
            }
        }

        override fun onLevel(level: Float) {
            if (token == engineToken && s.session?.phase == RecordingPhase.RECORDING) _level.value = level.coerceIn(0f, 1f)
        }

        override fun onFinished() {
            if (token == engineToken) _state.update { it.copy(finalising = false) }
        }
    }

    /** A late final result while `정리 중`: it replaces the tail taken as final at the stop. */
    private fun refine(text: String) {
        val session = s.session ?: return
        val base = Transcripts.appendFinal(finalBase ?: return, text)
        finalBase = base
        setSession(session.copy(confirmedRuns = base, partialText = ""))
        launch { recorder.update { lib -> CaptureRules.withRuns(lib, session.recordKey, base) } }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        lastTick = monotonicMs()
        tickerJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    private fun tick() {
        val now = monotonicMs()
        val delta = now - lastTick
        lastTick = now
        val session = s.session ?: return
        if (session.phase != RecordingPhase.RECORDING || delta <= 0) return
        // The microphone's own level drives the glow (LRC-10) and the waveform envelope (LRC-14).
        val level = takes.sample()
        if (takes.capturing) _level.value = level
        setSession(Recording.elapse(session, delta))
        sincePersist += delta
        if (sincePersist >= PERSIST_EVERY_MS) {
            sincePersist = 0
            persistSoon()
        }
    }

    private fun stopTicker() {
        if (tickerJob != null) tick()
        tickerJob?.cancel()
        tickerJob = null
    }

    /** Writes the running session for crash recovery, coalesced. */
    private fun persistSoon() {
        if (persistPending) return
        persistPending = true
        launch {
            delay(PERSIST_DELAY_MS)
            persistPending = false
            val now = s
            if (now.stage == CaptureStage.RECORDING || now.stage == CaptureStage.PROCESSING) recorder.saveSession(now.session)
        }
    }

    /** Writes the memo draft, coalesced (scrim, back or drag can close the sheet without `닫기`). */
    private fun draftSoon() {
        if (draftPending) return
        draftPending = true
        launch {
            delay(PERSIST_DELAY_MS)
            draftPending = false
            val now = s
            if (now.stage == CaptureStage.TYPING || now.voiceFromMemo) now.memo?.let { recorder.saveDraft(TextMemo.close(it)) }
        }
    }

    companion object {
        const val TICK_MS = 200L
        const val PERSIST_DELAY_MS = 300L
        const val PERSIST_EVERY_MS = 5_000L
    }
}

/** Small library edits the domain has no function for. */
object CaptureRules {
    /** [recordKey]'s text replaced by [runs] without a revision step (the engine refining its own transcript). */
    fun withRuns(library: RecorderLibrary, recordKey: Key, runs: List<TextRun>): RecorderLibrary = library.copy(
        records = library.records.map { if (it.key == recordKey) it.copy(runs = Runs.normalize(runs)) else it },
    )

    /** [recordKey]'s kept audio set (LRC-14: after `되돌리기` of `취소`, and for a recording switched to text). */
    fun withAudio(library: RecorderLibrary, recordKey: Key, audio: RecorderAudio): RecorderLibrary = library.copy(
        records = library.records.map { if (it.key == recordKey) it.copy(audio = audio) else it },
    )
}
