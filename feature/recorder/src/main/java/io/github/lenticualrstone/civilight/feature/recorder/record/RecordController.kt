package io.github.lenticualrstone.civilight.feature.recorder.record

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.inbox.CaptureGuard
import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordScreen
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordScreens
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.DeletedTargetCard
import io.github.lenticualrstone.civilight.domain.recorder.link.DeletedTargets
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.target
import io.github.lenticualrstone.civilight.domain.recorder.link.recorderTarget
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkCandidate
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkPicker
import io.github.lenticualrstone.civilight.domain.recorder.picker.PickerMode
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementRow
import io.github.lenticualrstone.civilight.domain.recorder.text.AtMentions
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderPort
import io.github.lenticualrstone.civilight.feature.recorder.env.EnvSource
import io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuOutcome
import io.github.lenticualrstone.civilight.feature.recorder.inbox.MenuView
import io.github.lenticualrstone.civilight.feature.recorder.memo.AtMarks
import io.github.lenticualrstone.civilight.feature.recorder.memo.MemoEditing
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The record text as being edited: [runs] are the field's source of truth (never re-seeded while
 * the user types, so Hangul composition is safe); [reload] tells the field to take [runs] again
 * (an `@` pick, a change made elsewhere such as `글로 풀기`).
 */
data class RecordEditor(val runs: List<TextRun>, val caret: Int, val reload: Int)

/** One `넣은 곳` row and where a tap goes (null = not tappable). */
data class PlacementView(val row: PlacementRow, val route: Route?)

/**
 * The record screen as drawn (LRC-28): the domain screen (kind, date, chips, `넣은 곳` with
 * `◇ 넣은 뒤 고침` / `◇ 넣은 곳 지워짐`, actions), the editor, the open `@` candidates, which 넣기 can
 * run, the kept audio for the player, and the projects whose capture protection applies.
 */
data class RecordView(
    val screen: RecordScreen,
    val text: String,
    val marks: AtMarks,
    val caret: Int,
    val reload: Int,
    val atOpen: Boolean,
    val atCandidates: List<LinkCandidate>,
    val puts: Set<PutKind>,
    val audio: RecorderAudio?,
    val placements: List<PlacementView>,
    val protectedProjects: Set<Key>,
)

/** What the screen shows. */
sealed interface RecordStatus {
    data object Loading : RecordStatus

    /** A hidden record while hidden records are locked: the prompt `숨긴 기록 보기` comes first (LRC-41). */
    data object Locked : RecordStatus

    /** Unknown or in the trash: the screen leaves. */
    data object Gone : RecordStatus

    data class Ready(val view: RecordView) : RecordStatus
}

/** What a button of a deleted card's sheet (LRC-23) asks the screen to do next. */
sealed interface CardOutcome {
    data object Close : CardOutcome

    /** The link picker: replacing [replaceLinkKeys] (`칩 다시 고르기`), or re-pointing the `@` runs of [relinkFrom] (`다른 항목에 잇기`). */
    data class OpenPicker(val replaceLinkKeys: List<Key>, val relinkFrom: TargetRef?) : CardOutcome

    /** `되살리기` went through: show [text] (no undo; the card can be deleted again). */
    data class Restored(val text: String) : CardOutcome
}

/**
 * The record screen's state and actions (LRC-28, LRC-22, LRC-23), plain Kotlin over the ports so it
 * runs on the JVM. Text edits autosave [commitDelayMs] after the last keystroke (and at once on an
 * `@` pick or when the screen leaves, [flush]); every write goes through [writer].
 */
class RecordController(
    private val scope: CoroutineScope,
    private val recorder: RecorderPort,
    private val envSource: EnvSource,
    private val writer: RecordWriter,
    val recordKey: Key,
    private val now: () -> Timestamp,
    private val editContext: () -> RecorderEditContext,
    unlockChanges: Flow<Any?>,
    private val commitDelayMs: Long = COMMIT_DELAY_MS,
) : RecordMenuSource {
    private val _env = MutableStateFlow<RecorderEnv?>(null)
    val env: StateFlow<RecorderEnv?> = _env.asStateFlow()
    private val editor = MutableStateFlow<RecordEditor?>(null)

    /** The runs last written or taken from the library; a library change to anything else is adopted while no edit waits. */
    private var committed: List<TextRun>? = null
    private var pending: Job? = null

    val status: StateFlow<RecordStatus> = combine(recorder.observe(), _env, editor) { library, env, ed ->
        if (env == null) RecordStatus.Loading else status(library, env, ed)
    }.stateIn(scope, SharingStarted.Eagerly, RecordStatus.Loading)

    init {
        scope.launch { recorder.ensureLoaded() }
        scope.launch { unlockChanges.collectLatest { _env.value = envSource.load() } }
        scope.launch { writer.projectDocs.collect { doc -> _env.update { it?.withDoc(doc) } } }
        scope.launch { recorder.observe().collect(::adopt) }
    }

    /** Reloads the projects (coming back to the screen). */
    fun refresh() {
        scope.launch { _env.value = envSource.load() }
    }

    private fun status(library: RecorderLibrary, env: RecorderEnv, ed: RecordEditor?): RecordStatus {
        val record = library.record(recordKey)
        if (record == null || record.deletedAt != null) return RecordStatus.Gone
        if (RecordScreens.needsUnlock(library, recordKey, env)) return RecordStatus.Locked
        val screen = RecordScreens.of(library, recordKey, env, now()) ?: return RecordStatus.Gone
        val runs = ed?.runs ?: record.runs
        val caret = ed?.caret ?: Runs.text(runs).length
        val query = AtMentions.queryAt(runs, caret)
        val candidates = query?.let { LinkPicker.search(env, it.query, library.linksOf(recordKey), PickerMode.AT).take(AT_MAX) }.orEmpty()
        val placements = library.placementsOf(recordKey).associateBy { it.key }
        val text = Runs.text(runs)
        return RecordStatus.Ready(
            RecordView(
                screen = screen,
                text = text,
                marks = markedTargets(screen) { it.dotted }.let { dotted ->
                    val diamond = markedTargets(screen) { it.diamond }
                    AtMarks.of(runs, dotted = { run -> run.recorderTarget in dotted }, diamond = { run -> run.recorderTarget in diamond })
                },
                caret = caret,
                reload = ed?.reload ?: 0,
                atOpen = query != null,
                atCandidates = candidates,
                puts = PutAvailability.kinds(Runs.text(record.runs), PutAvailability.offered(env)),
                audio = record.audio.takeIf { screen.hasPlayer },
                placements = screen.placements.map { row -> PlacementView(row, placements[row.placementKey]?.let { PlacementRoutes.routeOf(it, env) }) },
                protectedProjects = CaptureGuard.projectsFor(library, listOf(recordKey), env),
            ),
        )
    }

    /** Targets of the chips matching [flag] (dotted = in the trash, diamond = a deleted card, LRC-23); their `@` runs look the same. */
    private fun markedTargets(screen: RecordScreen, flag: (Chip) -> Boolean): Set<TargetRef> =
        screen.chips.filter(flag).flatMap { chip -> chip.links.mapNotNull { it.target } }.toSet()

    /** The library changed: take the record's text when it differs from what this screen wrote and no edit waits. */
    private fun adopt(library: RecorderLibrary) {
        val record = library.record(recordKey) ?: return
        if (pending != null || record.runs == committed) return
        // This screen's own write coming back (after a flush): the field already shows it.
        editor.value?.let { if (Runs.normalize(it.runs) == record.runs) { committed = record.runs; return } }
        committed = record.runs
        val ed = editor.value
        val text = Runs.text(record.runs)
        editor.value = RecordEditor(record.runs, (ed?.caret ?: text.length).coerceIn(0, text.length), (ed?.reload ?: 0) + 1)
    }

    // ---- text (LRC-28, LRC-17) ----

    /** The field changed to [text] with the caret at [caret]: one replace over the runs; saved shortly after. */
    fun edit(text: String, caret: Int) {
        val ed = editor.value ?: return
        val runs = MemoEditing.apply(ed.runs, text, caret)
        val reload = if (Runs.text(runs) != text) ed.reload + 1 else ed.reload
        editor.value = RecordEditor(runs, caret, reload)
        pending?.cancel()
        schedule {
            delay(commitDelayMs)
            // Once the write starts it finishes, even when the screen leaves or [flush] runs meanwhile.
            withContext(NonCancellable) { commit() }
        }
    }

    /** The one waiting write; it forgets itself when done. */
    private fun schedule(block: suspend () -> Unit) {
        pending?.cancel()
        val job = scope.launch { block() }
        pending = job
        job.invokeOnCompletion { if (pending === job) pending = null }
    }

    fun caretMoved(caret: Int) {
        editor.update { ed -> if (ed == null || ed.caret == caret) ed else ed.copy(caret = caret) }
    }

    /** An `@` candidate chosen: the `@query` becomes a link run (and a link of origin `@`); saved at once. */
    fun pickAt(candidate: LinkCandidate) {
        val ed = editor.value ?: return
        val query = AtMentions.queryAt(ed.runs, ed.caret) ?: return
        val (runs, caret) = AtMentions.complete(ed.runs, query, candidate.target, candidate.runText, editContext())
        editor.value = RecordEditor(runs, caret, ed.reload + 1)
        schedule { withContext(NonCancellable) { commit() } }
    }

    /** Writes a waiting edit now (the screen leaves; a change made elsewhere is about to run). */
    suspend fun flush() {
        // A waiting edit is dropped from the timer and written here; a write already running is awaited.
        pending?.cancelAndJoin()
        commit()
    }

    /**
     * Writes the editor's runs when they differ from what was last written. [committed] is set only
     * after the write, so a write cut short is retried by the next commit instead of being skipped.
     */
    private suspend fun commit() {
        val ed = editor.value
        if (ed != null) {
            val runs = Runs.normalize(ed.runs)
            if (runs != committed) {
                writer.editText(recordKey, ed.runs)
                committed = runs
            }
        }
    }

    // ---- links (LRC-18 … LRC-20) ----

    suspend fun addLink(target: TargetRef) = writer.addLink(recordKey, target)

    suspend fun replaceLinks(linkKeys: List<Key>, target: TargetRef) = writer.replaceLinks(recordKey, linkKeys, target)

    suspend fun removeLinks(linkKeys: List<Key>) = writer.removeLinks(recordKey, linkKeys)

    /** `다른 항목에 잇기` (picked in the link picker): the `@` runs of [from] point at [to]. */
    suspend fun relinkRuns(from: TargetRef, to: TargetRef) {
        flush()
        writer.relinkRuns(recordKey, from, to)
    }

    // ---- deleted targets (LRC-23) ----

    /** The chip holding [linkKey] as the screen draws it now. */
    fun chipOf(linkKey: Key): Chip? {
        val env = _env.value ?: return null
        val library = recorder.current()
        val record = library.record(recordKey) ?: return null
        return Chips.captureRow(library.linksOf(recordKey), record.runs, env, record).firstOrNull { chip -> chip.links.any { it.key == linkKey } }
    }

    /** The card a tap on [chip] opens (`지운 인물 · 노아` and its buttons), or null for a chip of a live target. */
    fun cardOf(chip: Chip): DeletedTargetCard? = _env.value?.let { DeletedTargets.card(chip, it) }

    /** A button of the card of the chip holding [linkKey]. */
    suspend fun onCard(linkKey: Key, action: String): CardOutcome {
        val chip = chipOf(linkKey) ?: return CardOutcome.Close
        val target = chip.links.singleOrNull()?.target ?: return CardOutcome.Close
        val env = _env.value ?: return CardOutcome.Close
        return when (action) {
            io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy.RELINK_CHIP -> CardOutcome.OpenPicker(chip.links.map { it.key }, null)
            io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy.LINK_OTHER -> CardOutcome.OpenPicker(emptyList(), target)
            io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy.REMOVE_CHIP -> {
                writer.removeLinks(recordKey, chip.links.map { it.key })
                CardOutcome.Close
            }
            io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy.TO_PLAIN_TEXT -> {
                flush()
                writer.toPlainText(recordKey, target)
                CardOutcome.Close
            }
            io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy.RESTORE -> {
                // A hidden card shows its name only while its project's items (or the hidden group) are open: that is the authentication.
                val authenticated = env.hiddenGroupOpen || env.itemsOpen(target.projectKey)
                writer.restoreTarget(target, authenticated)?.let { CardOutcome.Restored(it) } ?: CardOutcome.Close
            }
            else -> CardOutcome.Close
        }
    }

    // ---- ⋯ menu (LRC-29) ----

    override fun menu(recordKey: Key): MenuView? {
        val env = _env.value ?: return null
        return RecordMenus.menu(recorder.current(), recordKey, env, now())
    }

    override suspend fun onMenu(recordKey: Key, action: MenuAction): MenuOutcome {
        val env = _env.value ?: return MenuOutcome.Done
        flush()
        return RecordMenus.run(writer, recordKey, action, env)
    }

    companion object {
        const val COMMIT_DELAY_MS = 600L
        const val AT_MAX = 6
    }
}
