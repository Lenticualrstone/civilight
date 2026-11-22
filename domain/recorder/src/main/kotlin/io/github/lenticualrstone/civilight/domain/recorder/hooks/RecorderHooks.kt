package io.github.lenticualrstone.civilight.domain.recorder.hooks

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.RecorderTimeLabels
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.pointsAt
import io.github.lenticualrstone.civilight.domain.recorder.link.recorderTarget
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.domain.recorder.text.RecordText
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.text.TextRun

/** 서재 row numbers (LIB-01): `기록 <n> · 정리 안 됨 <u>`. */
data class LibraryCounts(val records: Int, val unsorted: Int) {
    val text: String get() = "${RecorderCopy.RECORDS} $records${RecorderCopy.SEP}${RecorderCopy.UNSORTED} $unsorted"
}

/** A row of the new-project step `라이프 레코더 기록 연결` (LIB-12): `음성 0:24 · 오늘` / first text line. */
data class StartMaterialRow(val recordKey: Key, val meta: String, val firstLine: String)

/**
 * The functions other areas call (LRC-47). None of the numbers is stored. Locked hidden records
 * are excluded only where the spec says so.
 */
object RecorderHooks {
    private fun live(library: RecorderLibrary) = library.records.filter { it.deletedAt == null }

    /** 서재 row: records not in the trash and those `정리 안 됨`; hidden records included (`ED` Q14). */
    fun libraryCounts(library: RecorderLibrary, env: RecorderEnv): LibraryCounts {
        val records = live(library)
        return LibraryCounts(records.size, records.count { RecordStates.state(library, it, env) == RecordState.UNSORTED })
    }

    /**
     * `레코더 · 정리 안 됨 n ›` on a scene memo card or a profile (LRC-24): records `정리 안 됨`, not in
     * the trash, linked (any origin) to exactly [target]; locked hidden records not counted. Records
     * linked only to the project do not count for a scene or a character.
     */
    fun unsortedLinkedCount(library: RecorderLibrary, target: TargetRef, env: RecorderEnv): Int = live(library).count { record ->
        (!record.hidden || env.recordsOpen) &&
            library.linksOf(record.key).any { it.pointsAt(target) } &&
            RecordStates.state(library, record, env) == RecordState.UNSORTED
    }

    /** The line text, or null when the count is 0 (nothing is shown). */
    fun targetLine(count: Int): String? = if (count >= 1) "${RecorderCopy.TARGET_LINE} $count ›" else null

    /**
     * `레코더 칩 <n>` (rule 20 impact sheet, check row `레코더 <n>`): records not in the trash with a
     * non-`@` link to [target]. Hidden records count: a number reveals no content.
     */
    fun recorderLinkCount(library: RecorderLibrary, target: TargetRef): Int = live(library).count { record ->
        library.linksOf(record.key).any { it.pointsAt(target) && it.origin.known != LinkOrigin.AT }
    }

    /** `@` runs in records not in the trash pointing at [target] (the cast area adds them to `@연결`). */
    fun recorderAtLinkCount(library: RecorderLibrary, target: TargetRef): Int =
        live(library).sumOf { record -> record.runs.count { it is TextRun.Link && it.recorderTarget == target } }

    /** Records linked to [projectKey] explicitly or through a sub-link, not in the trash, hidden included (LIB-24 `기록 <n> 바로 적용`). */
    fun linkedRecordCount(library: RecorderLibrary, projectKey: Key): Int =
        live(library).count { record -> library.linksOf(record.key).any { it.projectKey == projectKey } }

    /** Checklist `연결된 기록 <n>개 정리하기` (LIB-13): linked records still `정리 안 됨`; null when none. */
    fun checklistLine(library: RecorderLibrary, projectKey: Key, env: RecorderEnv): String? {
        val n = live(library).count { record ->
            library.linksOf(record.key).any { it.projectKey == projectKey } && RecordStates.state(library, record, env) == RecordState.UNSORTED
        }
        return if (n == 0) null else "${RecorderCopy.CHECKLIST_LINKED} ${n}개 ${RecorderCopy.CHECKLIST_TAIL}"
    }

    /** New-project step 4 candidates: records `정리 안 됨`, not locked hidden, newest first. */
    fun startMaterialCandidates(library: RecorderLibrary, env: RecorderEnv, now: Timestamp): List<StartMaterialRow> =
        live(library)
            .filter { (!it.hidden || env.recordsOpen) && RecordStates.state(library, it, env) == RecordState.UNSORTED }
            .sortedWith(compareByDescending<RecorderRecord> { it.createdAt }.thenByDescending { it.key })
            .map {
                StartMaterialRow(
                    it.key,
                    "${RecorderTimeLabels.kindLabel(it)}${RecorderCopy.SEP}${RecorderTimeLabels.day(it.createdAt, now, env.zone)}",
                    RecordText.firstLine(Runs.text(it.runs)),
                )
            }

    /** `라이프 레코더 기록 연결 · <t>개 중 <k>개 고름`. */
    fun startMaterialHeader(total: Int, chosen: Int): String =
        "${RecorderCopy.START_MATERIAL}${RecorderCopy.SEP}${total}개 중 ${chosen}개 고름"

    /** On `만들기`: each chosen record gains a project link (origin `시작 재료`) and nothing else. */
    fun linkRecords(library: RecorderLibrary, recordKeys: Collection<Key>, projectKey: Key, ctx: RecorderEditContext): RecorderLibrary =
        recordKeys.fold(library) { lib, key ->
            if (lib.record(key) == null) lib else LinkEdits.add(lib, key, TargetRef.project(projectKey), LinkOrigin.START_MATERIAL, ctx)
        }

    /**
     * `온보딩 끝내기` purged the example project: records stay in the library; their links (and any
     * placements) into it are removed (LRC-23).
     */
    fun exampleProjectDeleted(library: RecorderLibrary, projectKey: Key): RecorderLibrary = library.copy(
        links = library.links.filter { it.projectKey != projectKey },
        placements = library.placements.filter { it.projectKey != projectKey },
    )
}
