package io.github.lenticualrstone.civilight.domain.library.format

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * Which recorder records go into a project's `recorder.json` (§7.10, LIB-42, LIB-47, R23 §6):
 * records linked to the project (explicitly or through a placement), not in the trash; hidden
 * records only when the project is hidden and the file is encrypted.
 */
data class RecorderSlice(
    val records: List<RecorderRecord>,
    /** Linked hidden records left out (`숨긴 기록 <h> 제외`). */
    val excludedHidden: Int,
) {
    /** All linked records, written or not (`레코더 기록 <n>`). */
    val linkedCount: Int get() = records.size + excludedHidden

    /** The `recorder.json` of these records; `@` runs into other projects become plain text (LRC §3.5). */
    fun toFile(library: RecorderLibrary, projectKey: Key, includeAudio: Boolean): RecorderFile =
        RecorderFile.forProject(library, projectKey, records.map { it.plainOtherProjects(projectKey) }, includeAudio)

    companion object {
        fun select(library: RecorderLibrary, projectKey: Key, projectHidden: Boolean, encrypted: Boolean): RecorderSlice {
            val linked = library.recordsLinkedTo(projectKey).filter { it.deletedAt == null }
            val allowHidden = projectHidden && encrypted
            val written = if (allowHidden) linked else linked.filterNot { it.hidden }
            return RecorderSlice(written, linked.size - written.size)
        }

        private fun RecorderRecord.plainOtherProjects(projectKey: Key): RecorderRecord {
            if (runs.none { it is TextRun.Link && it.target.projectKey != null && it.target.projectKey != projectKey }) return this
            return copy(
                runs = runs.map { run ->
                    if (run is TextRun.Link && run.target.projectKey != null && run.target.projectKey != projectKey) {
                        TextRun.Plain(run.text)
                    } else {
                        run
                    }
                },
            )
        }
    }
}
