package io.github.lenticualrstone.civilight.feature.library.export

import io.github.lenticualrstone.civilight.domain.actions.ExportActions
import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.export.ExportGate
import io.github.lenticualrstone.civilight.domain.library.export.ExportPlan
import io.github.lenticualrstone.civilight.domain.library.export.ExportRun
import io.github.lenticualrstone.civilight.domain.library.export.ExportRunOutcome
import io.github.lenticualrstone.civilight.domain.library.export.ExportStart
import io.github.lenticualrstone.civilight.domain.library.export.PdfEncryption
import io.github.lenticualrstone.civilight.domain.library.export.PdfEncryptionException
import io.github.lenticualrstone.civilight.domain.library.export.ProjectFileExport
import io.github.lenticualrstone.civilight.domain.library.export.VoiceFixation
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.feature.library.export.pdf.PdfWriter
import io.github.lenticualrstone.civilight.feature.library.export.pdf.ScriptPdf
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/** A file ready for the save dialog, and the plan whose voice IDs are fixed once it is saved. */
class ExportReady(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    /** CSV and engine JSON: the lines written, fixed after the save (LIB-46). Null for `.civilight`. */
    val plan: ExportPlan?,
)

/** What `<형식> 만들기` came to before the save dialog. */
sealed interface ExportBuild {
    /** The prompt for `넣기` of hidden scenes was cancelled: nothing written, nothing fixed (E-03). */
    data object Cancelled : ExportBuild

    /** No PDF writer was given (JVM tests without Android), or the PDF could not be locked. */
    data object Unavailable : ExportBuild

    class Ready(val file: ExportReady) : ExportBuild
}

/**
 * Builds the bytes of one export (LIB-41, LIB-43, LIB-45, LIB-47, LIB-49): one authentication when hidden
 * scenes go in ([ExportGate]), then `domain:actions` [ExportActions] (대사 표 CSV, engine JSON) or
 * `domain:library` [ProjectFileExport] (`.civilight`, latest format, encrypted with the `암호`).
 * Nothing is stored here: voice IDs are fixed by [fixVoiceIds] only after the file was saved.
 */
object ExportRunner {
    suspend fun build(
        doc: ProjectDoc,
        form: ExportForm,
        recorder: RecorderLibrary,
        blobs: BlobReader,
        writtenBy: String,
        now: Timestamp,
        pdf: PdfWriter? = null,
        coverRating: Boolean = true,
        preview: PreviewRenderer? = null,
        authenticate: suspend () -> Boolean,
    ): ExportBuild {
        val options = ExportFormRules.runOptions(form)
        return when (options.format) {
            ExportFormat.PDF -> {
                val writer = pdf ?: return ExportBuild.Unavailable
                val plan = when (val start = ExportStart.of(doc, options, gateAnswer(doc, options, authenticate) ?: return ExportBuild.Cancelled)) {
                    ExportStart.Cancelled -> return ExportBuild.Cancelled
                    is ExportStart.Write -> start.plan
                }
                val password = options.pdfPassword?.toCharArray()
                try {
                    val bytes = withContext(Dispatchers.Default) {
                        val plain = writer.write(ScriptPdf.build(doc, plan, options.rangeKeys, coverRating))
                        if (password != null) PdfEncryption.encrypt(plain, password) else plain
                    }
                    // A PDF never fixes voice IDs (LIB-46): no plan goes with it.
                    ExportBuild.Ready(ExportReady(ExportRun.fileName(options.fileName, ExportFormat.PDF), mimeOf(ExportFormat.PDF), bytes, null))
                } catch (_: PdfEncryptionException) {
                    // Never write the file unlocked when `PDF 암호` is on.
                    ExportBuild.Unavailable
                } finally {
                    password?.fill('\u0000')
                }
            }
            ExportFormat.CSV, ExportFormat.ENGINE_JSON -> {
                val authenticated = when (ExportGate.of(doc, options)) {
                    ExportGate.Open -> false
                    ExportGate.NeedsAuth -> authenticate().also { if (!it) return ExportBuild.Cancelled }
                }
                when (val run = withContext(Dispatchers.Default) { ExportActions.run(doc, options, authenticated, now) }) {
                    ExportRunOutcome.Cancelled -> ExportBuild.Cancelled
                    is ExportRunOutcome.Done -> ExportBuild.Ready(ExportReady(run.file.fileName, run.file.mimeType, run.file.bytes, run.plan))
                }
            }
            ExportFormat.PROJECT_FILE -> {
                val password = options.password?.toCharArray()
                try {
                    // PBKDF2 (600 000 rounds) and the ZIP run off the main thread.
                    val file = withContext(Dispatchers.Default) {
                        // `preview.webp`: the cover picture, never for a hidden project (LIB-49).
                        val cover = if (doc.project.protections.hidden) null else preview?.let { runCatching { it.render(doc) }.getOrNull() }
                        ProjectFileExport.export(doc, options, password, recorder, blobs, writtenBy, now, preview = cover)
                    }
                    ExportBuild.Ready(ExportReady(file.fileName, mimeOf(ExportFormat.PROJECT_FILE), file.bytes, null))
                } finally {
                    password?.fill('\u0000')
                }
            }
        }
    }

    /** One authentication when hidden scenes go in (E-03); null = the prompt was cancelled. */
    private suspend fun gateAnswer(doc: ProjectDoc, options: ExportOptions, authenticate: suspend () -> Boolean): Boolean? =
        when (ExportGate.of(doc, options)) {
            ExportGate.Open -> false
            ExportGate.NeedsAuth -> if (authenticate()) true else null
        }

    /** The voice IDs of exactly the lines written (LIB-46), applied to the project as it is now. */
    fun fixVoiceIds(current: ProjectDoc, plan: ExportPlan, at: Timestamp): ProjectDoc =
        VoiceFixation.apply(current, plan, at, ExportActions.voiceIdOf(current))

    fun mimeOf(format: ExportFormat): String = ExportDefaults.mimeType(format)
}

/**
 * The 내보내기 sheets' drafts, one per project, in memory for the activity (never in saved state).
 * A draft goes on `닫기` and after its file was saved; when the sheet leaves any other way (scrim,
 * back, a re-lock, the save dialog) its choices stay for the next opening but the typed passwords
 * are blanked ([ExportFormRules.withoutPasswords]).
 */
class ExportDrafts {
    private val drafts = HashMap<Key, MutableStateFlow<ExportForm?>>()

    fun of(projectKey: Key): MutableStateFlow<ExportForm?> = drafts.getOrPut(projectKey) { MutableStateFlow(null) }

    fun clear(projectKey: Key) {
        drafts[projectKey]?.value = null
    }
}
