package io.github.lenticualrstone.civilight.domain.library.export

import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Turns an [ExportPlan] into file bytes. Implemented by the script area (PDF, 대사 표 CSV, engine
 * JSON); this module never serialises those formats (it must not depend on domain:script).
 */
fun interface ExportSerializer {
    fun write(doc: ProjectDoc, plan: ExportPlan): ByteArray
}

/** A written reading/engine file, ready for the save dialog (`ACTION_CREATE_DOCUMENT`, `ED`). */
class ExportedFile(val fileName: String, val mimeType: String, val bytes: ByteArray)

sealed interface ExportRunOutcome {
    /** Authentication cancelled or failed: nothing written, voice IDs untouched (E-03). */
    data object Cancelled : ExportRunOutcome

    /** [doc] carries the voice IDs fixed by this export (LIB-46); store it. */
    class Done(val file: ExportedFile, val doc: ProjectDoc, val plan: ExportPlan) : ExportRunOutcome
}

/**
 * `<형식> 만들기` for PDF, CSV and engine JSON (LIB-41…46): the one authentication gate, the plan,
 * the script area's serialiser, then voice-ID fixation of exactly the lines written. Recorder records
 * never go into these files (E-13). `.civilight` goes through `ProjectFileExport` instead.
 */
object ExportRun {
    fun run(
        doc: ProjectDoc,
        options: ExportOptions,
        authenticated: Boolean,
        serializer: ExportSerializer,
        voiceIdOf: (WrittenLine) -> String?,
        now: Timestamp,
    ): ExportRunOutcome {
        require(options.format != ExportFormat.PROJECT_FILE) { "use ProjectFileExport for .civilight" }
        val plan = when (val start = ExportStart.of(doc, options, authenticated)) {
            ExportStart.Cancelled -> return ExportRunOutcome.Cancelled
            is ExportStart.Write -> start.plan
        }
        val bytes = serializer.write(doc, plan)
        val fixed = VoiceFixation.apply(doc, plan, now, voiceIdOf)
        val file = ExportedFile(fileName(options.fileName, options.format), ExportDefaults.mimeType(options.format), bytes)
        return ExportRunOutcome.Done(file, fixed, plan)
    }

    /** `<name>.<ext>`; an extension already typed is kept. */
    fun fileName(base: String, format: ExportFormat): String {
        val ext = ExportDefaults.extension(format)
        return if (base.endsWith(".$ext", ignoreCase = true)) base else "$base.$ext"
    }
}
