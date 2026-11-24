package io.github.lenticualrstone.civilight.feature.library.export

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.ExportActions
import io.github.lenticualrstone.civilight.domain.actions.VersionActions
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
import io.github.lenticualrstone.civilight.model.script.Block
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
    /**
     * (0.2.0, N-29, D21) PDF, CSV and engine JSON: what was written, so each exported numbering unit gets one
     * automatic `내보낸 판` once the file is saved. Null for `.civilight` (it adds none, LIB-97).
     */
    val versionPlan: ExportPlan? = null,
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
                    // A PDF never fixes voice IDs (LIB-46): no fixation plan goes with it; the version plan does (D21).
                    ExportBuild.Ready(ExportReady(ExportRun.fileName(options.fileName, ExportFormat.PDF), mimeOf(ExportFormat.PDF), bytes, null, versionPlan = plan))
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
                    is ExportRunOutcome.Done -> ExportBuild.Ready(ExportReady(run.file.fileName, run.file.mimeType, run.file.bytes, run.plan, versionPlan = run.plan))
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

    /**
     * What a saved file writes back to the project as it is now, as one step: the voice IDs of exactly the
     * lines written (CSV, JSON; LIB-46), then one automatic `내보낸 판` per exported numbering unit (PDF, CSV,
     * JSON; N-29, D21) through `domain:actions` `VersionActions.captureForExport`. Null when nothing changes.
     */
    fun afterSave(current: ProjectDoc, ready: ExportReady, ctx: ActionContext): ProjectDoc? {
        if (ready.plan == null && ready.versionPlan == null) return null
        val fixed = ready.plan?.let { fixVoiceIds(current, it, ctx.now) } ?: current
        return ready.versionPlan?.let { VersionActions.captureForExport(fixed, it, ctx) } ?: fixed
    }

    /**
     * (0.2.0) What [afterSave] changed, carried onto a kept undo step [step] (the write is no undo step, so undo
     * never takes it back): the `내보낸 판` rows it added (they hold the text as exported, never the step's) and
     * the voice states it fixed, by block and option key. Blocks the step does not have are left alone.
     */
    fun carryAfterSave(step: ProjectDoc, before: ProjectDoc, after: ProjectDoc): ProjectDoc =
        carry(step, before, after, changedVoices(before, after))

    /**
     * [carryAfterSave] for one save: the store runs the carry once per kept undo step (up to 100) with
     * the same `before`/`after`, so what changed is worked out once per save instead of once per step.
     */
    fun carrier(): (ProjectDoc, ProjectDoc, ProjectDoc) -> ProjectDoc {
        var seenBefore: ProjectDoc? = null
        var seenAfter: ProjectDoc? = null
        var memo: Map<Key, Block> = emptyMap()
        return { step, before, after ->
            if (seenBefore !== before || seenAfter !== after) {
                memo = changedVoices(before, after)
                seenBefore = before
                seenAfter = after
            }
            carry(step, before, after, memo)
        }
    }

    /** Blocks of [after] whose voice state differs from [before] (or that [before] does not have), by key. */
    private fun changedVoices(before: ProjectDoc, after: ProjectDoc): Map<Key, Block> {
        val beforeBlocks = before.episodes.values.flatMap { it.blocks }.associateBy { it.key }
        return after.episodes.values.flatMap { it.blocks }.filter { b ->
            val old = beforeBlocks[b.key]
            old == null || (old !== b && (old.voice != b.voice || old.options.map { it.voice } != b.options.map { it.voice }))
        }.associateBy { it.key }
    }

    private fun carry(step: ProjectDoc, before: ProjectDoc, after: ProjectDoc, changed: Map<Key, Block>): ProjectDoc {
        var out = step
        for ((unit, file) in after.versions) {
            val had = before.versions[unit]?.versions?.map { it.key }?.toSet().orEmpty()
            val added = file.versions.filter { it.key !in had }
            if (added.isEmpty()) continue
            val target = out.versions[unit] ?: file.copy(versions = emptyList())
            val known = target.versions.map { it.key }.toSet()
            out = out.copy(versions = out.versions + (unit to target.copy(versions = target.versions + added.filter { it.key !in known })))
        }
        if (changed.isEmpty()) return out
        // Only episode files holding a changed block are copied, and a block already carrying the voices
        // stays the same object, so undo snapshots keep sharing what the save did not touch.
        return out.copy(
            episodes = out.episodes.mapValues { (_, file) ->
                if (file.blocks.none { it.key in changed }) return@mapValues file
                file.copy(
                    blocks = file.blocks.map { b ->
                        val a = changed[b.key] ?: return@map b
                        val optionVoice = a.options.associate { it.key to it.voice }
                        val next = b.copy(voice = a.voice, options = b.options.map { o -> if (o.key in optionVoice) o.copy(voice = optionVoice.getValue(o.key)) else o })
                        if (next == b) b else next
                    },
                )
            },
        )
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
