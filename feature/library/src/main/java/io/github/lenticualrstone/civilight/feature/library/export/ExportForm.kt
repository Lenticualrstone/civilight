package io.github.lenticualrstone.civilight.feature.library.export

import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.export.ExportLayer
import io.github.lenticualrstone.civilight.domain.library.export.ExportSheet
import io.github.lenticualrstone.civilight.domain.library.export.ExportSheets
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import java.time.ZoneId

/**
 * The export sheet's draft (LIB-41): the options `domain:library` decides from, plus the typed
 * passwords. Held in memory only ([ExportDrafts]); passwords never go into saved state.
 */
data class ExportForm(
    val options: ExportOptions,
    /** `.civilight` `암호` and its repeat (`한 번 더`). */
    val password: String = "",
    val passwordRepeat: String = "",
    /** `PDF 암호` (hidden or R-18 project). */
    val pdfPassword: String = "",
    /** The author typed a file name: switching the format keeps it. */
    val fileNameEdited: Boolean = false,
) {
    val format: ExportFormat get() = options.format
}

/** The sheet's rules as plain functions (tests: E-01, E-02, E-08 … E-11). */
object ExportFormRules {
    /** Every format is written here (the 대본 PDF since batch 5, LIB-43). */
    @Suppress("UNUSED_PARAMETER")
    fun available(format: ExportFormat): Boolean = true

    /** The sheet as first opened: `대본 PDF` selected (board `Export`; §3 `ExportOptions` default). */
    fun initial(doc: ProjectDoc, now: Timestamp, zone: ZoneId, format: ExportFormat = ExportFormat.PDF): ExportForm =
        ExportForm(ExportDefaults.initial(doc, ExportDefaults.visibleFormat(doc, format), now, zone))

    /** Another format card: its own defaults; a typed file name stays, passwords are dropped. */
    fun selectFormat(form: ExportForm, doc: ProjectDoc, format: ExportFormat, now: Timestamp, zone: ZoneId): ExportForm {
        if (format == form.format) return form
        val fresh = ExportDefaults.initial(doc, format, now, zone)
        val options = if (form.fileNameEdited) fresh.copy(fileName = form.options.fileName, rangeKeys = form.options.rangeKeys) else fresh.copy(rangeKeys = form.options.rangeKeys)
        return ExportForm(options, fileNameEdited = form.fileNameEdited)
    }

    fun setHiddenScenes(form: ExportForm, include: Boolean) = form.copy(options = form.options.copy(includeHiddenScenes = include))

    fun setRange(form: ExportForm, keys: List<Key>) = form.copy(options = form.options.copy(rangeKeys = keys))

    fun toggleLayer(form: ExportForm, layer: ExportLayer, on: Boolean): ExportForm {
        val layers = if (on) form.options.layers + layer.id else form.options.layers - layer.id
        return form.copy(options = form.options.copy(layers = layers))
    }

    fun setSecretFields(form: ExportForm, on: Boolean) = form.copy(options = form.options.copy(includeSecretFields = on))

    fun setFileName(form: ExportForm, name: String) = form.copy(options = form.options.copy(fileName = name), fileNameEdited = true)

    fun setPdfPasswordOn(form: ExportForm, on: Boolean) =
        form.copy(options = form.options.copy(pdfPassword = if (on) form.pdfPassword else null), pdfPassword = if (on) form.pdfPassword else "")

    fun setPdfPassword(form: ExportForm, text: String) =
        form.copy(pdfPassword = text, options = form.options.copy(pdfPassword = text))

    fun setEncrypt(form: ExportForm, on: Boolean) =
        form.copy(options = form.options.copy(encrypt = on), password = if (on) form.password else "", passwordRepeat = if (on) form.passwordRepeat else "")

    fun setPassword(form: ExportForm, text: String) = form.copy(password = text)

    /** The draft kept after its sheet left: every choice but the typed passwords. */
    fun withoutPasswords(form: ExportForm): ExportForm =
        form.copy(password = "", passwordRepeat = "", pdfPassword = "", options = form.options.copy(password = null, pdfPassword = form.options.pdfPassword?.let { "" }))

    fun setPasswordRepeat(form: ExportForm, text: String) = form.copy(passwordRepeat = text)

    fun setRecorderAudio(form: ExportForm, on: Boolean) = form.copy(options = form.options.copy(includeRecorderAudio = on))

    fun setIncludeTrash(form: ExportForm, on: Boolean) = form.copy(options = form.options.copy(includeTrash = on))

    /** (0.2.0, LIB-97) `지난 판도 포함`. */
    fun setIncludeVersions(form: ExportForm, on: Boolean) = form.copy(options = form.options.copy(includeVersions = on))

    /**
     * (0.2.0, LIB-91) [form] when its format is offered for [doc], else switched to the first offered format
     * (a remembered CSV or JSON draft while `게임 제작 도구` is off). Nothing is written to the project.
     */
    fun visible(form: ExportForm, doc: ProjectDoc, now: Timestamp, zone: ZoneId): ExportForm {
        val format = ExportDefaults.visibleFormat(doc, form.format)
        return if (format == form.format) form else selectFormat(form, doc, format, now, zone)
    }

    /** The rows `domain:library` draws for the current choices. */
    fun sheet(doc: ProjectDoc, form: ExportForm, recorder: RecorderLibrary): ExportSheet = ExportSheets.build(doc, form.options, recorder)

    /** Whether the two password fields agree (shown under `한 번 더` only once something was typed there). */
    fun passwordsMatch(form: ExportForm): Boolean = form.password == form.passwordRepeat

    /**
     * The primary button is on when the format can be written here, the file name is not blank, an
     * `암호` that is on has both fields equal and not empty, and a `PDF 암호` that is on is not empty.
     */
    fun canCreate(form: ExportForm): Boolean {
        if (!available(form.format)) return false
        if (form.options.fileName.isBlank()) return false
        if (form.format == ExportFormat.PROJECT_FILE && form.options.encrypt && (form.password.isEmpty() || !passwordsMatch(form))) return false
        if (form.format == ExportFormat.PDF && form.options.pdfPassword != null && form.pdfPassword.isEmpty()) return false
        return true
    }

    /** The options a run gets (passwords included). */
    fun runOptions(form: ExportForm): ExportOptions = form.options.copy(
        fileName = form.options.fileName.trim(),
        password = form.password.takeIf { form.options.encrypt },
        pdfPassword = form.pdfPassword.takeIf { form.options.pdfPassword != null },
    )
}

/** One choice of `범위`: a unit above 씬 (`1장 전체`, `에피소드 1-7`), indented by its level. */
data class RangeChoice(val key: Key, val label: String, val depth: Int)

/** The `범위` picker's rows (LIB-41: units = the project's levels above 씬, MAIN). */
object ExportRanges {
    fun choices(doc: ProjectDoc): List<RangeChoice> {
        val sceneLevel = doc.project.structure.levels.lastIndex
        if (sceneLevel <= 0) return emptyList()
        val rows = ArrayList<RangeChoice>()
        fun visit(parent: Key?) {
            for (unit in doc.index.children(parent).sortedBy { it.order }) {
                if (!doc.liveness.isLive(unit) || unit.level >= sceneLevel) continue
                rows += RangeChoice(unit.key, ExportSheets.rangeLabel(doc, listOf(unit.key)), unit.level)
                visit(unit.key)
            }
        }
        visit(null)
        return rows
    }
}
