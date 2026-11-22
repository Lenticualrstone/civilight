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

class FakeRecorder(var library: RecorderLibrary = RecorderLibrary()) : RecorderPort {
    var session: RecordingSession? = null
    var draft: TextMemoDraft? = null
    var sessionWrites = 0

    override fun current(): RecorderLibrary = library

    override suspend fun ensureLoaded() = Unit

    override suspend fun update(transform: (RecorderLibrary) -> RecorderLibrary): RecorderLibrary = transform(library).also { library = it }

    override suspend fun saveSession(session: RecordingSession?) {
        this.session = session
        sessionWrites++
    }

    override suspend fun saveDraft(draft: TextMemoDraft?) {
        this.draft = draft
    }

    override suspend fun readDraft(): TextMemoDraft? = draft
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
    override suspend fun load(): RecorderEnv = env

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
        engineFactory = { engine },
    )
}
