package io.github.lenticualrstone.civilight.domain.recorder.inbox

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.text.Hangul
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord

/**
 * Text search over the inbox (the header icon, `ED` Q11) and what the capture guard must know
 * (LRC-22). Hidden records are found only while hidden records are open; records linked to a
 * locked hidden project are never found; trashed records never.
 */
object RecordSearch {
    fun searchable(library: RecorderLibrary, record: RecorderRecord, env: RecorderEnv): Boolean {
        if (record.deletedAt != null) return false
        if (record.hidden && !env.recordsOpen) return false
        return library.linksOf(record.key).none { link -> env.project(link.projectKey)?.let(env::isLockedHidden) == true }
    }

    fun search(library: RecorderLibrary, query: String, env: RecorderEnv): List<RecorderRecord> {
        val q = Hangul.fold(query)
        if (q.isEmpty()) return emptyList()
        return Inbox.ordered(library).filter { searchable(library, it, env) && Hangul.fold(Runs.text(it.runs)).contains(q) }
    }
}

/**
 * The capture guard (LRC-22, LIB-26): the projects with `화면 캡처 가리기` on whose linked records
 * show their content on screen right now. A non-empty set means FLAG_SECURE. A masked label alone
 * does not count, but every visible row shows its text, so a visible linked record counts; a
 * hidden record while locked shows nothing and does not count.
 */
object CaptureGuard {
    fun projectsFor(library: RecorderLibrary, visibleRecordKeys: Collection<Key>, env: RecorderEnv): Set<Key> =
        visibleRecordKeys.mapNotNull(library::record)
            .filter { it.deletedAt == null && (!it.hidden || env.recordsOpen) }
            .flatMap { library.linksOf(it.key).map { link -> link.projectKey } }
            .filter { env.project(it)?.captureBlock == true }
            .toSet()

    fun isSecure(library: RecorderLibrary, visibleRecordKeys: Collection<Key>, env: RecorderEnv): Boolean =
        projectsFor(library, visibleRecordKeys, env).isNotEmpty()
}
