package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.K
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxFilters
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxOpening
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.InboxTarget
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.StatusFilter
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The inbox target filter carries the project key: after a `새 프로젝트로` import, 「유리 정원」 (P2)
 * holds a 하르트 with the same item key as P1's, and each target filter lists only its own project's
 * records (LRC-27, domain-recorder Requests).
 */
class InboxTargetProjectTest {
    private val p2Hart = TargetRef(Fx.P2, RecorderTargetKind.CHARACTER, K.HART)
    private val p2Record = Fx.r(40)

    /** P2 with an imported 하르트 (same key as P1's). */
    private val env = Fx.env(
        p2 = Fx.p2Doc().copy(cast = CastFile(characters = listOf(Character(K.HART, 1, "하르트", order = OrderKeys.FIRST)))),
    )

    /** F-open plus one unsorted text record linked to P2's 하르트. */
    private fun library(): RecorderLibrary {
        val open = Fx.openLibrary()
        val at = Fx.at("2026-11-21T01:00:00Z")
        val record = RecorderRecord(
            key = p2Record, kind = RecordKind.TEXT.open(), createdAt = at,
            runs = listOf(TextRun.Plain("유리 정원의 하르트 메모")), updatedAt = at, updatedBy = Fx.DEVICE,
        )
        val link = RecorderLink(
            key = K.recorderLink(140), recordKey = p2Record, projectKey = Fx.P2, targetKind = RecorderTargetKind.CHARACTER.open(),
            targetKey = K.HART, origin = LinkOrigin.MANUAL.open(), createdAt = at,
        )
        return open.copy(records = open.records + record, links = open.links + link)
    }

    @Test
    fun targetFilterMatchesTheProjectKeyToo() {
        val library = library()
        val p1 = InboxFilters.preset(InboxOpening.FromTarget(Fx.hart))
        assertEquals(InboxTarget(Fx.P1, RecorderTargetKind.CHARACTER, K.HART), p1.target)
        assertEquals(StatusFilter.UNSORTED, p1.status)
        val p1Rows = Inbox.screen(library, p1, env, Fx.NOW).rows.map { it.recordKey }
        assertEquals(listOf(Fx.R1), p1Rows)
        assertEquals(Fx.hart, InboxFilters.targetRef(library, p1))

        val p2 = InboxFilters.preset(InboxOpening.FromTarget(p2Hart))
        assertEquals(InboxTarget(Fx.P2, RecorderTargetKind.CHARACTER, K.HART), p2.target)
        val p2Rows = Inbox.screen(library, p2, env, Fx.NOW).rows.map { it.recordKey }
        assertEquals(listOf(p2Record), p2Rows)
        assertEquals(p2Hart, InboxFilters.targetRef(library, p2))

        // The `레코더 · 정리 안 됨 n ›` line on each profile agrees with the rows its preset lists.
        assertEquals(p1Rows.size, RecorderHooks.unsortedLinkedCount(library, Fx.hart, env))
        assertEquals(p2Rows.size, RecorderHooks.unsortedLinkedCount(library, p2Hart, env))
    }

    @Test
    fun noChipForATargetNothingLinksTo() {
        val library = library()
        val p3Hart = InboxFilterState(target = InboxTarget(Fx.P3, RecorderTargetKind.CHARACTER, K.HART))
        assertNull(InboxFilters.targetRef(library, p3Hart))
        assertEquals(emptyList<Any>(), Inbox.screen(library, p3Hart, env, Fx.NOW).rows)
        // A project target always has its chip.
        val project = InboxFilterState(target = InboxTarget(Fx.P3, RecorderTargetKind.PROJECT, Fx.P3))
        assertEquals(TargetRef.project(Fx.P3), InboxFilters.targetRef(library, project))
    }
}
