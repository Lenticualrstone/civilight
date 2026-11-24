package io.github.lenticualrstone.civilight.domain.library.export

import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.domain.library.format.RecorderSlice
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** Option rows of the export sheet, per format (LIB-41 table). */
sealed interface ExportRow {
    val label: String

    /** `범위` · `1장 전체`. */
    data class Range(val value: String) : ExportRow {
        override val label: String get() = "범위"
    }

    /** `숨긴 씬 1 · 1-7 씬 4` with `넣기` · `빼기`; `넣기` asks for one authentication on create. */
    data class HiddenScenes(val sceneLabels: List<String>, val choice: HiddenSceneChoice) : ExportRow {
        override val label: String get() = (listOf("숨긴 씬 ${sceneLabels.size}") + sceneLabels).joinToString(" · ")
        val options: List<String> get() = HiddenSceneChoice.entries.map { it.label }
    }

    /** `숨긴 인물 <n> · <names>` · `화자 이름만 들어감` (information only). */
    data class HiddenSpeakers(val names: List<String>) : ExportRow {
        override val label: String get() = (listOf("숨긴 인물 ${names.size}") + names).joinToString(" · ")
        val info: String get() = "화자 이름만 들어감"
    }

    /** `표시 레이어` · `연출 · 괄호 지문 포함`. */
    data class Layers(val layers: List<ExportLayer>) : ExportRow {
        override val label: String get() = "표시 레이어"
        val value: String get() = if (layers.isEmpty()) "" else layers.joinToString(" · ") { it.label } + " 포함"
    }

    /** `비밀 항목 포함` switch (JSON). */
    data class SecretFields(val on: Boolean) : ExportRow {
        override val label: String get() = "비밀 항목 포함"
    }

    data class FileName(val value: String) : ExportRow {
        override val label: String get() = "파일 이름"
    }

    /** `PDF 암호` switch + password field (hidden or R-18 project). */
    data class PdfPassword(val on: Boolean) : ExportRow {
        override val label: String get() = "PDF 암호"
    }

    /** `.civilight` `암호` switch + password and repeat fields + one warning line (the drawn pattern of the RHRecoveryPw warning). */
    data class Password(val on: Boolean) : ExportRow {
        override val label: String get() = "암호"
        val warning: String get() = PASSWORD_WARNING
    }

    /** `레코더 기록 4` · `원본 음성 포함` checkbox · `숨긴 기록 1 제외`. */
    data class RecorderRecords(val linked: Int, val includeAudio: Boolean, val hiddenExcluded: Int) : ExportRow {
        override val label: String get() = "레코더 기록 $linked"
        val audioOption: String get() = "원본 음성 포함"
        val hiddenNote: String? get() = if (hiddenExcluded > 0) "숨긴 기록 $hiddenExcluded 제외" else null

        /** The drawn line: `레코더 기록 4 · 원본 음성 포함 · 숨긴 기록 1 제외`. */
        val text: String get() = listOfNotNull(label, audioOption, hiddenNote).joinToString(" · ")
    }

    data class IncludeTrash(val on: Boolean) : ExportRow {
        override val label: String get() = "휴지통도 포함"
    }

    /**
     * (0.2.0, LIB-97, N-29) `.civilight` only: `TODO_COPY_LIB_EXPORT_VERSIONS` switch, shown only when the
     * project has at least one episode version; on → `versions/` is written.
     */
    data class IncludeVersions(val on: Boolean) : ExportRow {
        override val label: String get() = TODO_COPY_LIB_EXPORT_VERSIONS
    }

    companion object {
        /** LIB-97 row label, interim `지난 판도 포함` (pattern of `휴지통도 포함`; `지난 판` is N-29 copy). */
        const val TODO_COPY_LIB_EXPORT_VERSIONS = "지난 판도 포함"

        /** The warning line under the password fields (`RHRecoveryPw` pattern `잊으면 … 열 수 없음`). */
        const val PASSWORD_WARNING = "잊으면 파일을 열 수 없음"

        @Deprecated("The copy exists now.", ReplaceWith("PASSWORD_WARNING"))
        const val TODO_COPY_LIB41_PASSWORD_WARNING = PASSWORD_WARNING
    }
}

/** The export sheet as drawn for the current options (LIB-41, board `Export`). */
data class ExportSheet(
    val sub: String,
    val format: ExportFormat,
    val rows: List<ExportRow>,
    val warnings: List<ExportWarning>,
    val primaryButton: String,
) {
    val header: String get() = "내보내기"
    val close: String get() = "닫기"

    inline fun <reified T : ExportRow> row(): T? = rows.filterIsInstance<T>().firstOrNull()
}

object ExportSheets {
    /** Primary buttons of the other formats follow the drawn `PDF 만들기` (LIB-41: `<형식> 만들기`). */
    const val CREATE_CSV = "CSV 만들기"
    const val CREATE_JSON = "JSON 만들기"
    const val CREATE_FILE = ".civilight 만들기"

    @Deprecated("The copy exists now.", ReplaceWith("CREATE_CSV"))
    const val TODO_COPY_LIB41_CREATE_CSV = CREATE_CSV

    @Deprecated("The copy exists now.", ReplaceWith("CREATE_JSON"))
    const val TODO_COPY_LIB41_CREATE_JSON = CREATE_JSON

    @Deprecated("The copy exists now.", ReplaceWith("CREATE_FILE"))
    const val TODO_COPY_LIB41_CREATE_FILE = CREATE_FILE

    /**
     * Rows for [options]. The example project and trashed projects cannot be exported (the caller
     * does not open the sheet for them).
     */
    fun build(doc: ProjectDoc, options: ExportOptions, recorder: RecorderLibrary = RecorderLibrary()): ExportSheet {
        val format = options.format
        val reading = format != ExportFormat.PROJECT_FILE
        val rows = ArrayList<ExportRow>()
        var warnings = emptyList<ExportWarning>()
        if (reading) {
            val scope = ExportScope(doc, options.rangeKeys)
            rows += ExportRow.Range(rangeLabel(doc, options.rangeKeys))
            if (scope.hiddenScenes.isNotEmpty()) {
                val codes = UnitCodes(doc)
                rows += ExportRow.HiddenScenes(
                    scope.hiddenScenes.map { codes.sceneLabel(it.key).orEmpty() },
                    if (options.includeHiddenScenes) HiddenSceneChoice.INCLUDE else HiddenSceneChoice.EXCLUDE,
                )
            }
            val hiddenSpeakers = scope.written(options.includeHiddenScenes)
                .flatMap { doc.manuscript.liveBlocksOf(it.key) }
                .flatMap { b -> listOfNotNull(b.speakerKey.takeIf { b.kind.known == BlockKind.DIALOGUE }) + b.options.mapNotNull { it.speakerKey } }
                .distinct()
                .mapNotNull { doc.index.character(it) }
                .filter { it.hidden }
            if (hiddenSpeakers.isNotEmpty()) rows += ExportRow.HiddenSpeakers(hiddenSpeakers.map { it.name })
            if (format == ExportFormat.PDF) rows += ExportRow.Layers(options.layers.mapNotNull(ExportLayer::of).sortedBy { it.ordinal })
            if (format == ExportFormat.ENGINE_JSON) rows += ExportRow.SecretFields(options.includeSecretFields)
            if (format == ExportFormat.ENGINE_JSON) warnings = ExportPlanner.plan(doc, options).warnings
        }
        rows += ExportRow.FileName(options.fileName)
        if (format == ExportFormat.PDF && ExportDefaults.pdfPasswordDefault(doc)) rows += ExportRow.PdfPassword(options.pdfPassword != null)
        if (format == ExportFormat.PROJECT_FILE) {
            rows += ExportRow.Password(options.encrypt)
            val slice = RecorderSlice.select(recorder, doc.key, doc.project.protections.hidden, options.encrypt)
            if (slice.linkedCount > 0) rows += ExportRow.RecorderRecords(slice.linkedCount, options.includeRecorderAudio, slice.excludedHidden)
            rows += ExportRow.IncludeTrash(options.includeTrash)
            if (hasVersions(doc, options.includeTrash)) rows += ExportRow.IncludeVersions(options.includeVersions)
        }
        return ExportSheet(
            // (0.2.0, LIB-92) No rating badge in the sub.
            sub = doc.project.name,
            format = format,
            rows = rows,
            warnings = warnings,
            primaryButton = when (format) {
                ExportFormat.PDF -> "PDF 만들기"
                ExportFormat.CSV -> CREATE_CSV
                ExportFormat.ENGINE_JSON -> CREATE_JSON
                ExportFormat.PROJECT_FILE -> CREATE_FILE
            },
        )
    }

    /**
     * (0.2.0, LIB-97) Whether [doc] holds at least one episode version the file would carry (the versions row
     * shows only then). Without `휴지통도 포함` the versions of trashed units are left out of the file
     * (ProjectPackageWriter.withoutTrashed020), so they do not count.
     */
    fun hasVersions(doc: ProjectDoc, includeTrash: Boolean = true): Boolean {
        val l = doc.liveness
        return doc.versions.any { (key, file) ->
            file.versions.isNotEmpty() && (includeTrash || key == doc.key || l.isUnitLive(key))
        }
    }

    /**
     * `범위` value: a top-level unit reads `<n><level> 전체` (`1장 전체`); a deeper one
     * `<level> <code>` (`에피소드 1-7`, `ED`); several are joined by ` · `; empty = all (`ED`: `전체`).
     */
    fun rangeLabel(doc: ProjectDoc, rangeKeys: List<Key>): String {
        if (rangeKeys.isEmpty()) return "전체"
        val codes = UnitCodes(doc)
        val levels = doc.project.structure.levels
        return rangeKeys.mapNotNull { key ->
            val unit = doc.index.unit(key) ?: return@mapNotNull null
            val name = levels.getOrElse(unit.level) { "" }
            val code = codes.code(key) ?: return@mapNotNull null
            if (unit.level == 0) "$code$name 전체" else "$name $code"
        }.joinToString(" · ")
    }
}

/** The authentication gate on create (LIB-41, R19 §5, E-03/E-04). */
sealed interface ExportGate {
    /** No authentication needed. */
    data object Open : ExportGate

    /** Exactly one authentication before anything is written (`넣기` of hidden scenes). */
    data object NeedsAuth : ExportGate

    companion object {
        fun of(doc: ProjectDoc, options: ExportOptions): ExportGate {
            if (options.format == ExportFormat.PROJECT_FILE || !options.includeHiddenScenes) return Open
            return if (ExportScope(doc, options.rangeKeys).hiddenScenes.isNotEmpty()) NeedsAuth else Open
        }
    }
}

/** What create does after the gate. */
sealed interface ExportStart {
    /** Cancelled or failed authentication: nothing written, no voice IDs fixed. */
    data object Cancelled : ExportStart

    data class Write(val plan: ExportPlan) : ExportStart

    companion object {
        /** [authenticated] is the result of the one prompt, ignored when the gate is open. */
        fun of(doc: ProjectDoc, options: ExportOptions, authenticated: Boolean): ExportStart = when (ExportGate.of(doc, options)) {
            ExportGate.Open -> Write(ExportPlanner.plan(doc, options))
            ExportGate.NeedsAuth -> if (authenticated) Write(ExportPlanner.plan(doc, options)) else Cancelled
        }
    }
}
