package io.github.lenticualrstone.civilight.domain.recorder.export

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * The export sheet row (LIB-41 draws it): `레코더 기록 <n>` · `원본 음성 포함` (checkbox, default off)
 * · `숨긴 기록 <h> 제외`. [hidden] = hidden ones among the linked records; [hiddenIncluded] = they go
 * into the file (hidden project, encrypted), whose part is TODO_COPY_LRC_20.
 */
data class RecorderExportRow(val linked: Int, val hidden: Int, val hiddenIncluded: Boolean) {
    val recordsPart: String get() = "${RecorderCopy.EXPORT_RECORDS} $linked"
    val audioPart: String get() = RecorderCopy.EXPORT_AUDIO

    /** Omitted when there is no hidden record ([PROPOSED]). */
    val hiddenPart: String?
        get() = when {
            hidden == 0 -> null
            hiddenIncluded -> RecorderCopy.TODO_EXPORT_HIDDEN_INCLUDED
            else -> "${RecorderCopy.HIDDEN_RECORDS} $hidden ${RecorderCopy.EXCLUDED}"
        }

    val parts: List<String> get() = listOfNotNull(recordsPart, audioPart, hiddenPart)
}

/**
 * Recorder records in exports (LRC-45, §7.10): a project's `.civilight` holds exactly the records
 * linked to it (explicitly or through a sub-link) that are not in the trash, hidden ones only when
 * the project is hidden and the file encrypted. Text always; audio only with `원본 음성 포함`.
 * Links and placements into this project only; `@` runs into other projects become plain text.
 * `대본 PDF`, `대사 표 CSV` and `게임 엔진 JSON` never contain records.
 */
object RecorderExport {
    fun appliesTo(format: ExportFormat): Boolean = format == ExportFormat.PROJECT_FILE

    /** Records linked to [projectKey], not in the trash (hidden included). */
    fun linkedRecords(library: RecorderLibrary, projectKey: Key): List<RecorderRecord> =
        library.records.filter { r -> r.deletedAt == null && library.linksOf(r.key).any { it.projectKey == projectKey } }

    fun includesHidden(env: RecorderEnv, projectKey: Key, encrypted: Boolean): Boolean =
        encrypted && env.project(projectKey)?.hidden == true

    fun row(library: RecorderLibrary, env: RecorderEnv, projectKey: Key, encrypted: Boolean): RecorderExportRow {
        val linked = linkedRecords(library, projectKey)
        return RecorderExportRow(linked.size, linked.count { it.hidden }, includesHidden(env, projectKey, encrypted))
    }

    /** The records written to the file. */
    fun exportedRecords(library: RecorderLibrary, env: RecorderEnv, projectKey: Key, encrypted: Boolean): List<RecorderRecord> {
        val allowHidden = includesHidden(env, projectKey, encrypted)
        return linkedRecords(library, projectKey).filter { allowHidden || !it.hidden }
    }

    /** The project's `recorder.json`. */
    fun projectFile(library: RecorderLibrary, env: RecorderEnv, projectKey: Key, encrypted: Boolean, includeAudio: Boolean): RecorderFile {
        val records = exportedRecords(library, env, projectKey, encrypted).map { plainOtherProjects(it, projectKey) }
        return RecorderFile.forProject(library, projectKey, records, includeAudio)
    }

    /** Audio blob hashes the file references (`assets/<hash>.<ext>`). */
    fun audioHashes(file: RecorderFile): List<String> = file.records.mapNotNull { it.audio?.hash }.distinct()

    /**
     * The library file `.civilight-recorder` (LRC-46): every record not in the trash with all links
     * and placements; audio per `원본 음성 보관`; hidden records only when [includeHidden] (encrypted
     * with the 복구 암호, `ED` Q16).
     */
    fun libraryFile(library: RecorderLibrary, includeHidden: Boolean, keepAudio: Boolean): RecorderFile {
        val records = library.records.filter { it.deletedAt == null && (includeHidden || !it.hidden) }
            .map { if (keepAudio) it else it.copy(audio = null) }
        val keys = records.map { it.key }.toSet()
        return RecorderFile.library(
            RecorderLibrary(records, library.links.filter { it.recordKey in keys }, library.placements.filter { it.recordKey in keys }),
        )
    }

    fun plainOtherProjects(record: RecorderRecord, projectKey: Key): RecorderRecord {
        if (record.runs.none { it is TextRun.Link && it.target.projectKey != projectKey }) return record
        return record.copy(runs = record.runs.map { run ->
            if (run is TextRun.Link && run.target.projectKey != projectKey) TextRun.Plain(run.text) else run
        })
    }
}

/**
 * Import and restore merge (LRC-45, LRC-46, §3.5): the same record key gives one record; links and
 * placements are united by key (and links by target); the local record — text included — is kept;
 * nothing is removed or overwritten.
 */
object RecorderMerge {
    fun merge(local: RecorderLibrary, incoming: RecorderLibrary): RecorderLibrary {
        val localKeys = local.records.map { it.key }.toSet()
        val records = local.records + incoming.records.filter { it.key !in localKeys }
        val links = mutableListOf<RecorderLink>().apply { addAll(local.links) }
        for (link in incoming.links) {
            val duplicate = links.any {
                it.key == link.key || (it.recordKey == link.recordKey && it.projectKey == link.projectKey &&
                    it.targetKind == link.targetKind && it.targetKey == link.targetKey)
            }
            if (!duplicate) links += link
        }
        val placementKeys = local.placements.map { it.key }.toSet()
        val placements: List<Placement> = local.placements + incoming.placements.filter { it.key !in placementKeys }
        return RecorderLibrary(records, links, placements)
    }

    /** A project imported under a new key (`새 프로젝트로`): its records' rows point at [newKey] (F-18). */
    fun rekeyProject(incoming: RecorderLibrary, oldKey: Key, newKey: Key): RecorderLibrary = incoming.copy(
        records = incoming.records.map { r ->
            r.copy(runs = r.runs.map { run ->
                if (run is TextRun.Link && run.target.projectKey == oldKey) run.copy(target = run.target.copy(projectKey = newKey)) else run
            })
        },
        links = incoming.links.map {
            if (it.projectKey != oldKey) it
            else it.copy(projectKey = newKey, targetKey = if (it.targetKey == oldKey) newKey else it.targetKey)
        },
        placements = incoming.placements.map { if (it.projectKey == oldKey) it.copy(projectKey = newKey) else it },
    )
}
