package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.feature.world.common.FixedRowUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 별칭 (WLD-16) and `아님으로 둔 것` (WLD-22) on the term card. */
class AliasRejectionTest {
    private val ctx = WorldTestFixture.ctx()
    private val access = TermAccess()

    @Test
    fun `WLD-T10 adding an alias counts the new candidates next to the row`() {
        val a = WorldTestFixture.stateA()
        val index = WorldTestFixture.index(a)
        val saved = AliasEditor.add(a, T1, "빛돌", index, access, ctx) as AliasChange.Saved
        assertEquals(2, saved.newlyFound)
        assertEquals(listOf("오리지늄", "빛돌"), saved.doc.index.term(T1)!!.aliases)
        val card = TermCardState.build(saved.doc, PROJECT, T1, WorldTestFixture.index(saved.doc), false, readOnly = false, aliasFound = saved.newlyFound)
        assertEquals(FixedRowUi("별칭", "오리지늄 · 빛돌", editable = true, note = "후보 2 새로 찾음"), card.aliases)
        assertEquals("쓰인 곳 · 2 · 후보 3", card.header)
    }

    @Test
    fun `no count while the first scan is pending, and removing gives zero`() {
        val a = WorldTestFixture.stateA()
        val pending = AliasEditor.add(a, T1, "빛돌", CandidateIndex.pending(), access, ctx) as AliasChange.Saved
        assertNull(pending.newlyFound)
        val b = WorldTestFixture.stateB()
        val removed = AliasEditor.remove(b, T1, "빛돌", WorldTestFixture.index(b), access, ctx) as AliasChange.Saved
        assertEquals(0, removed.newlyFound)
        assertEquals(listOf("오리지늄"), removed.doc.index.term(T1)!!.aliases)
    }

    @Test
    fun `invalid aliases are refused with their line`() {
        val a = WorldTestFixture.stateA()
        val index = WorldTestFixture.index(a)
        assertEquals(AliasChange.Refused(WorldCopy.ALIAS_SAME_AS_NAME), AliasEditor.add(a, T1, "오리지늄 등불", index, access, ctx))
        assertEquals(AliasChange.Refused(WorldCopy.ALIAS_DUPLICATE), AliasEditor.add(a, T1, " 오리지늄", index, access, ctx))
        assertEquals(AliasChange.Refused(WorldCopy.ALIAS_EMPTY), AliasEditor.add(a, T1, "  ", index, access, ctx))
    }

    @Test
    fun `the alias row is editable and shows a dash when empty, hidden when read-only and empty`() {
        val none = WorldTestFixture.withTerm(WorldTestFixture.stateA(), T1) { it.copy(aliases = emptyList()) }
        val index = WorldTestFixture.index(none)
        assertEquals(FixedRowUi("별칭", "—", editable = true), TermCardState.build(none, PROJECT, T1, index, false, readOnly = false).aliases)
        assertNull(TermCardState.build(none, PROJECT, T1, index, false, readOnly = true).aliases)
    }

    @Test
    fun `WLD-T06 the rejection is listed and its undo brings the candidate back`() {
        val a = WorldTestFixture.stateA()
        val candidate = WorldTestFixture.index(a).of(T1).single()
        val rejected = TermEdits.reject(a, candidate, ctx)
        val rows = RejectionRows.build(rejected, T1, hiddenOpen = false)
        assertEquals(1, rows.size)
        assertEquals("“오리지”", rows.single().text)
        assertTrue(rows.single().location, rows.single().location.startsWith("1-2 씬 1"))
        assertEquals("쓰인 곳 · 2", TermCardState.build(rejected, PROJECT, T1, WorldTestFixture.index(rejected), false, false).header)
        val back = RejectionRows.restore(rejected, T1, rows.single().rejectionKey, ctx)
        assertTrue(RejectionRows.build(back, T1, false).isEmpty())
        assertEquals("쓰인 곳 · 2 · 후보 1", TermCardState.build(back, PROJECT, T1, WorldTestFixture.index(back), false, false).header)
    }

    @Test
    fun `WLD-T37 the orphaned reveal marker is dotted and the warning opens the check center`() {
        assertEquals(MarkerUi("◇ 근거 지워짐", warn = false, dotted = true), MarkerUi.of("◇ 근거 지워짐"))
        assertEquals(MarkerUi("▲ 공개(1-7) 전 사용", warn = true, dotted = false), MarkerUi.of("▲ 공개(1-7) 전 사용"))
        val a = WorldTestFixture.stateA()
        val trashCtx = io.github.lenticualrstone.civilight.domain.trash.TrashContext(
            now = io.github.lenticualrstone.civilight.model.sample.ExampleClock.NOW,
            zone = java.time.ZoneId.of("Asia/Seoul"),
            newKey = WorldTestFixture.keys(),
        )
        val trashed = io.github.lenticualrstone.civilight.domain.trash.TrashOps
            .delete(a, io.github.lenticualrstone.civilight.domain.trash.ItemId.block(K.BLOCK_044), trashCtx).doc
        val card = TermCardState.build(trashed, PROJECT, T1, WorldTestFixture.index(trashed), false, false)
        val b42 = card.groups.first().rows.first { it.markers.any(MarkerUi::warn) }
        assertTrue(b42.markers.toString(), b42.markers.any { it.dotted && it.text == MarkerUi.ORPHANED })
    }
}
