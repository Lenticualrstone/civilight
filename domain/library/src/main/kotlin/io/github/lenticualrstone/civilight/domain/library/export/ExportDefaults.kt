package io.github.lenticualrstone.civilight.domain.library.export

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.hiding.HiddenItems
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.game.GameTools
import java.time.ZoneId

/** `넣기` · `빼기` for hidden scenes in range (LIB-41, LIB-42). */
enum class HiddenSceneChoice(val label: String) { INCLUDE("넣기"), EXCLUDE("빼기") }

/** Display layers of the 대본 PDF (LIB-43); same ids as the script's 표시 레이어. */
enum class ExportLayer(val id: String, val label: String) {
    DIRECTION("direction", "연출"),
    PAREN("paren", "괄호 지문"),
    MEMO("memo", "작가 메모"),
    ;

    companion object {
        fun of(id: String): ExportLayer? = entries.firstOrNull { it.id == id }
    }
}

/** Defaults of the export sheet (LIB-41, LIB-42, E-01, E-08, E-09). */
object ExportDefaults {
    val FORMATS: List<ExportFormat> = listOf(ExportFormat.PDF, ExportFormat.CSV, ExportFormat.ENGINE_JSON, ExportFormat.PROJECT_FILE)

    /**
     * (0.2.0, LIB-91, G-7) The formats the sheet offers for [project]: all four with `게임 제작 도구` on,
     * else `대본 PDF` and `프로젝트 파일 .civilight` only. [FORMATS] keeps every format.
     */
    fun exportFormats(project: Project): List<ExportFormat> =
        if (GameTools.on(project)) FORMATS else FORMATS.filter { it == ExportFormat.PDF || it == ExportFormat.PROJECT_FILE }

    fun exportFormats(doc: ProjectDoc): List<ExportFormat> = exportFormats(doc.project)

    /** [format] when [doc] offers it, else the first offered format (LIB-91 `ED`: a hidden choice falls back). */
    fun visibleFormat(doc: ProjectDoc, format: ExportFormat): ExportFormat =
        exportFormats(doc).let { if (format in it) format else it.first() }

    /** Format radio cards: title · sub. */
    fun formatCard(format: ExportFormat): Pair<String, String> = when (format) {
        // (0.2.0, LIB-92) The rating fragments are dropped, nothing added.
        ExportFormat.PDF -> "대본 PDF" to "읽기용"
        ExportFormat.CSV -> "대사 표 CSV" to "화자 · 대사 · 보이스 ID · 블록 ID · 상태"
        ExportFormat.ENGINE_JSON -> "게임 엔진 JSON" to "씬 · 블록 · 화자 · 연출 · 선택지"
        ExportFormat.PROJECT_FILE -> "프로젝트 파일 .civilight" to "옮기기 · 보관 · 숨김 · 숨긴 항목 있으면 암호"
    }

    /** PDF `빼기`; CSV and JSON `넣기`; `.civilight` never asks (everything goes in). */
    fun hiddenSceneDefault(format: ExportFormat): HiddenSceneChoice? = when (format) {
        ExportFormat.PDF -> HiddenSceneChoice.EXCLUDE
        ExportFormat.CSV, ExportFormat.ENGINE_JSON -> HiddenSceneChoice.INCLUDE
        ExportFormat.PROJECT_FILE -> null
    }

    /** `.civilight` `암호` switch default: on for a hidden or R-18 project or one with any hidden item. */
    fun encryptDefault(doc: ProjectDoc): Boolean =
        doc.project.protections.hidden || doc.project.rating.known == Rating.R18 || doc.project.rating.isUnknown || HiddenItems(doc).any

    /** `PDF 암호` row: shown and on for a hidden or R-18 project. */
    fun pdfPasswordDefault(doc: ProjectDoc): Boolean =
        doc.project.protections.hidden || doc.project.rating.known == Rating.R18 || doc.project.rating.isUnknown

    /** `파일 이름`: the project name, or `civilight-내보내기-<MMDD>` for a hidden or R-18 project (§6 Q1). */
    fun defaultFileName(doc: ProjectDoc, now: Long, zone: ZoneId): String =
        if (pdfPasswordDefault(doc)) "civilight-내보내기-${TimeLabels.mmdd(now, zone)}" else doc.project.name

    /** File extension of each output. */
    fun extension(format: ExportFormat): String = when (format) {
        ExportFormat.PDF -> "pdf"
        ExportFormat.CSV -> "csv"
        ExportFormat.ENGINE_JSON -> "json"
        ExportFormat.PROJECT_FILE -> "civilight"
    }

    /** MIME type for the save dialog (`ED`). */
    fun mimeType(format: ExportFormat): String = when (format) {
        ExportFormat.PDF -> "application/pdf"
        ExportFormat.CSV -> "text/csv"
        ExportFormat.ENGINE_JSON -> "application/json"
        ExportFormat.PROJECT_FILE -> "application/vnd.civilight.project"
    }

    /**
     * Initial sheet state for [format]: range = the first live top-level unit (`1장 전체`), layers
     * 연출 · 괄호 지문, the format's hidden-scene default, secret fields off, file name per
     * [defaultFileName], PDF password row on for hidden/R-18, `.civilight` encryption per
     * [encryptDefault], audio and trash off.
     */
    fun initial(doc: ProjectDoc, format: ExportFormat, now: Long, zone: ZoneId): ExportOptions {
        val firstTop = doc.index.children(null).filter { doc.liveness.isLive(it) }.minByOrNull { it.order }
        return ExportOptions(
            format = format,
            rangeKeys = listOfNotNull(firstTop?.key),
            includeHiddenScenes = hiddenSceneDefault(format) == HiddenSceneChoice.INCLUDE,
            layers = setOf(ExportLayer.DIRECTION.id, ExportLayer.PAREN.id),
            includeSecretFields = false,
            fileName = defaultFileName(doc, now, zone),
            pdfPassword = if (format == ExportFormat.PDF && pdfPasswordDefault(doc)) "" else null,
            encrypt = format == ExportFormat.PROJECT_FILE && encryptDefault(doc),
            password = null,
            includeRecorderAudio = false,
            includeTrash = false,
        )
    }
}
