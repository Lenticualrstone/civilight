package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureHub
import io.github.lenticualrstone.civilight.feature.recorder.capture.ProjectPort
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderPort
import io.github.lenticualrstone.civilight.feature.recorder.env.EnvSource
import io.github.lenticualrstone.civilight.feature.recorder.speech.SpeechEngine
import io.github.lenticualrstone.civilight.feature.recorder.speech.SpeechListener
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.recorder.TextMemoDraft
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.feature.recorder.audio.AudioTakes
import io.github.lenticualrstone.civilight.feature.recorder.audio.NoAudioTakes
import io.github.lenticualrstone.civilight.feature.recorder.audio.Playback
import io.github.lenticualrstone.civilight.feature.recorder.audio.TakeFile
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.TranscriptionMode
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import java.time.Instant
import java.time.ZoneId

/** The example project and clock of the spec's fixtures (2026-11-22 09:00 Asia/Seoul). */
object Fx {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    val NOW: Long = Instant.parse("2026-11-22T00:00:00Z").toEpochMilli()
    const val DEVICE = "TESTDEVICE"
    val P1: Key = ExampleKeys.PROJECT
    val S: Key = ExampleKeys.SCENE_1_7_3

    const val R1_TEXT = ExampleRecorder.R1_TEXT

    fun p1(): ProjectDoc = ExampleProject.build()

    fun entry(doc: ProjectDoc, readOnly: Boolean = false, hidden: Boolean = false, updatedAt: Long = 0, lastOpenedAt: Long? = null, deletedAt: Long? = null) =
        LibraryEntry(
            key = doc.key, name = doc.project.name, rating = doc.project.rating, hidden = hidden, readOnly = readOnly,
            updatedAt = updatedAt, lastOpenedAt = lastOpenedAt, deletedAt = deletedAt,
        )

    fun env(vararg docs: ProjectDoc = arrayOf(p1())): RecorderEnv =
        RecorderEnv(projects = docs.map { ProjectSnapshot(entry(it), it) }, zone = ZONE)

    /** Deterministic ULID-shaped keys. */
    fun keys(start: Int = 1): () -> Key {
        var n = start
        return { "01JZ000000TSTK" + (n++).toString().padStart(12, '0') }
    }
}

class FakeRecorder(library: RecorderLibrary = RecorderLibrary(), var trash: List<TrashEntry> = emptyList()) : RecorderPort {
    private val flow = MutableStateFlow(library)
    var library: RecorderLibrary
        get() = flow.value
        set(value) {
            flow.value = value
        }
    var session: RecordingSession? = null
    var draft: TextMemoDraft? = null
    var sessionWrites = 0

    override fun current(): RecorderLibrary = library

    override suspend fun ensureLoaded() = Unit

    override suspend fun update(transform: (RecorderLibrary) -> RecorderLibrary): RecorderLibrary = transform(library).also { library = it }

    override fun observe(): Flow<RecorderLibrary> = flow

    override suspend fun updateTrash(transform: (RecorderTrashState) -> RecorderTrashState) {
        val next = transform(RecorderTrashState(library, trash))
        library = next.library
        trash = next.entries
    }

    override suspend fun readSession(): RecordingSession? = session

    override suspend fun saveSession(session: RecordingSession?) {
        this.session = session
        sessionWrites++
    }

    override suspend fun saveDraft(draft: TextMemoDraft?) {
        this.draft = draft
    }

    override suspend fun readDraft(): TextMemoDraft? = draft

    /** Audio kept in the blob store: hash → path (content-addressed by the path in tests). */
    val blobs = mutableMapOf<String, String>()

    override suspend fun importAudio(path: String, mime: String, durationMs: Long): Blob? {
        val hash = "h-" + path.substringAfterLast('/')
        blobs[hash] = path
        return Blob(hash = hash, fileExt = "aac", mime = mime, bytes = 1000, durationMs = durationMs, source = BlobSource.RECORDER.open(), importedAt = Fx.NOW)
    }

    override fun audioFile(hash: String): File? = blobs[hash]?.let(::File)
}

/**
 * Microphone takes the test drives: a started run counts as captured when [finish]ed (unless
 * [captures] is false); [level] is what [sample] reports. Takes are kept per record by run count.
 * The microphone is modelled as the platform takes hold it (U-13): open while a run records, closed
 * by [pause], [finish] and [drop]; [opens] and [closes] count it.
 */
class FakeTakes(var captures: Boolean = true, var level: Float = 0.5f) : AudioTakes {
    private var segmentKey: Key? = null
    val segments = mutableMapOf<Key, Int>()
    val discarded = mutableListOf<Key>()
    val recovered = mutableListOf<Key>()
    var pruneKept: Set<Key>? = null
    var paused = false
    var micOpen = false
        private set
    var opens = 0
        private set
    var closes = 0
        private set

    override val capturing: Boolean get() = micOpen

    private fun openMic() {
        micOpen = true
        opens++
    }

    private fun closeMic() {
        if (!micOpen) return
        micOpen = false
        closes++
    }

    override fun start(recordKey: Key): Boolean {
        if (!captures) return false
        closeMic()
        segmentKey = recordKey
        paused = false
        openMic()
        return true
    }

    override fun pause() {
        paused = true
        closeMic()
    }

    override fun resume() {
        paused = false
        if (segmentKey != null && !micOpen) openMic()
    }

    override fun sample(): Float = if (capturing) level else 0f

    override fun finish(recordKey: Key): Boolean {
        if (segmentKey != recordKey) return false
        closeMic()
        segmentKey = null
        segments[recordKey] = (segments[recordKey] ?: 0) + 1
        return true
    }

    override fun drop(recordKey: Key) {
        if (segmentKey == recordKey) {
            closeMic()
            segmentKey = null
        }
    }

    override fun take(recordKey: Key): TakeFile? {
        val n = segments[recordKey] ?: return null
        return TakeFile("takes/$recordKey.aac", "audio/aac", 1000L * n, ByteArray(64) { 128.toByte() })
    }

    override fun discard(recordKey: Key) {
        drop(recordKey)
        segments.remove(recordKey)
        discarded += recordKey
    }

    override fun recoverSegment(recordKey: Key) {
        recovered += recordKey
        segments[recordKey] = (segments[recordKey] ?: 0) + 1
    }

    override fun prune(keep: Set<Key>) {
        pruneKept = keep
        segments.keys.retainAll(keep)
    }
}

/** Playback the test drives: [finish] ends the file; [at] is the position reported. */
class FakePlayback(var playable: Boolean = true) : Playback {
    var path: String? = null
    var at: Long = 0
    var starts = 0
    private var onComplete: (() -> Unit)? = null

    override fun start(path: String, fromMs: Long, onComplete: () -> Unit): Boolean {
        if (!playable) return false
        this.path = path
        at = fromMs
        starts++
        this.onComplete = onComplete
        return true
    }

    override fun pause(): Long = at

    override fun position(): Long = at

    override fun stop() {
        path = null
        onComplete = null
    }

    fun finish() {
        val done = onComplete
        stop()
        done?.invoke()
    }
}

class FakeProjects(var doc: ProjectDoc, var readOnly: Boolean = false) : ProjectPort {
    val labels = mutableListOf<String>()

    override suspend fun <T> edit(projectKey: Key, label: String, transform: (ProjectDoc) -> Pair<ProjectDoc, T>): T? {
        if (readOnly || projectKey != doc.key) return null
        val (next, value) = transform(doc)
        doc = next
        labels += label
        return value
    }
}

class FakeEnv(var env: RecorderEnv) : EnvSource {
    val forgotten = mutableListOf<Key>()

    override suspend fun load(): RecorderEnv = env

    override fun forget(projectKey: Key) {
        forgotten += projectKey
    }

    override fun quick(): RecorderEnv = env
}

/** A speech engine the test drives: [say] partial, [confirm] final; [stop] delivers [lastWords] then finishes. */
class FakeEngine(override val available: Boolean = true) : SpeechEngine {
    var listener: SpeechListener? = null
    var running = false
    var paused = false
    var lastWords: String? = null
    var starts = 0

    override fun start(listener: SpeechListener) {
        this.listener = listener
        running = true
        paused = false
        starts++
    }

    override fun pause() {
        paused = true
    }

    override fun resume() {
        paused = false
    }

    override fun stop() {
        running = false
        val l = listener ?: return
        lastWords?.let { l.onFinal(it) }
        lastWords = null
        l.onFinished()
    }

    fun say(text: String) = listener!!.onPartial(text)

    fun confirm(text: String) = listener!!.onFinal(text)
}

/** A hub over fakes, on the test's virtual clock. */
class HubFixture(
    scope: TestScope,
    val recorder: FakeRecorder = FakeRecorder(),
    val projects: FakeProjects = FakeProjects(Fx.p1()),
    val env: FakeEnv = FakeEnv(Fx.env(projects.doc)),
    val engine: FakeEngine = FakeEngine(),
    var settings: AppSettings = AppSettings(),
    val takes: AudioTakes = NoAudioTakes,
    var online: Boolean = false,
    val engines: ((TranscriptionMode) -> SpeechEngine)? = null,
) {
    private val keys = Fx.keys()
    var now: Long = Fx.NOW

    val hub = CaptureHub(
        scope = scope.backgroundScope,
        recorder = recorder,
        envSource = env,
        projects = projects,
        settings = { settings },
        context = { ActionContext(now = now, zone = Fx.ZONE, device = Fx.DEVICE, newKey = keys) },
        monotonicMs = { scope.testScheduler.currentTime },
        audioTempPath = { "takes/$it.m4a" },
        engineFactory = engines ?: { engine },
        takes = takes,
        online = { online },
    )
}
