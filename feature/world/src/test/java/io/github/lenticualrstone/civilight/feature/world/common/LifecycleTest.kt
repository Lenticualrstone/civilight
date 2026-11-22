package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.MergeActions
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.MergeChoices
import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.RevealEdit
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.feature.world.list.WorldListItem
import io.github.lenticualrstone.civilight.feature.world.list.WorldListState
import io.github.lenticualrstone.civilight.feature.world.term.TermCardState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import io.github.lenticualrstone.civilight.navigation.WorldTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** 숨기기 (WLD-13), 지우기 · 되살리기 (WLD-14), 합치기 (WLD-15) of world items as the cards drive them. */
class LifecycleTest {
    private val doc = WorldTestFixture.stateA()
    private fun actx() = ActionContext(now = ExampleClock.NOW, zone = ZoneId.of("Asia/Seoul"), device = "test-device", newKey = WorldTestFixture.keys())
    private fun card(d: ProjectDoc, hiddenOpen: Boolean = false) =
        TermCardState.build(d, PROJECT, T1, WorldTestFixture.index(d, hiddenOpen), hiddenOpen, readOnly = false)

    private fun createPlace(d: ProjectDoc, name: String): Pair<ProjectDoc, Key> =
        (WorldItems.createPlace(d, name, WorldTestFixture.ctx()) as CreateResult.Created).let { it.doc to it.key }

    @Test
    fun `WLD-13 hiding a term locks its card and folds it into the list's lock row`() {
        val hidden = ItemLifecycle.setHidden(doc, WorldKind.TERM, T1, true, WorldTestFixture.ctx())
        assertTrue(ItemLifecycle.isHidden(hidden, WorldKind.TERM, T1))
        assertEquals(CardAccess.Locked("숨긴 용어 1"), card(hidden).access)
        assertEquals(CardAccess.Open, card(hidden, hiddenOpen = true).access)
        val list = WorldListState.build(hidden, WorldTab.TERMS, WorldTestFixture.index(hidden), hiddenOpen = false).items
        assertTrue(list.any { it is WorldListItem.Lock && it.text.startsWith("숨긴 용어 1") })
        assertFalse(list.any { it is WorldListItem.Row && it.key == T1 })
        val shown = ItemLifecycle.setHidden(hidden, WorldKind.TERM, T1, false, WorldTestFixture.ctx())
        assertEquals(CardAccess.Open, card(shown).access)
    }

    @Test
    fun `WLD-T43 deleting a term shows the impact, trashes it, and the card restores it`() {
        val impact = ItemLifecycle.impact(doc, ItemId.term(T1))
        assertTrue(impact.needsSheet)
        val sheet = MergePlan.deleteSheet(impact)
        assertEquals(sheet.owned, MergePlan.deleteSheet(impact, hiddenLocked = false).owned)
        assertEquals("‘오리지늄 등불’ 지우기", sheet.title)
        assertTrue(sheet.sub, sheet.sub.startsWith("용어 · 참조 "))
        assertTrue(sheet.owned.any { it.label == "◇ 첫 공개 1" })
        assertEquals("다른 용어와 합치기 ›", sheet.mergeAction)
        assertEquals(listOf("취소", "휴지통으로"), listOf(sheet.cancel, sheet.confirm))

        val deleted = ItemLifecycle.delete(doc, ItemId.term(T1), actx())!!
        assertTrue(deleted.toast.message, deleted.toast.message.startsWith("용어 오리지늄 등불 지움"))
        assertTrue(deleted.toast.message, deleted.toast.message.contains("▲ 공개 전 사용 1"))
        assertEquals("되돌리기", deleted.toast.action)
        val gone = card(deleted.doc).access as CardAccess.Gone
        assertEquals("지운 용어 · 오리지늄 등불", gone.text)
        assertTrue(ItemLifecycle.restorable(deleted.doc, ItemId.term(T1)))
        assertNull(ItemLifecycle.delete(deleted.doc, ItemId.term(T1), actx()))

        val back = ItemLifecycle.restore(deleted.doc, ItemId.term(T1), actx(), authenticated = false) as Restored.Done
        assertTrue(back.toast.message.startsWith("오리지늄 등불 되살림"))
        assertEquals(CardAccess.Open, card(back.doc).access)
        assertEquals("쓰인 곳 · 2 · 후보 1", card(back.doc).header)
    }

    @Test
    fun `a hidden trashed item needs authentication to come back`() {
        val hidden = ItemLifecycle.setHidden(doc, WorldKind.TERM, T1, true, WorldTestFixture.ctx())
        val deleted = ItemLifecycle.delete(hidden, ItemId.term(T1), actx())!!
        assertEquals(Restored.NeedsAuth, ItemLifecycle.restore(deleted.doc, ItemId.term(T1), actx(), authenticated = false))
        assertTrue(ItemLifecycle.restore(deleted.doc, ItemId.term(T1), actx(), authenticated = true) is Restored.Done)
    }

    @Test
    fun `TC-20-03 an item nothing refers to needs no impact sheet`() {
        val (d, key) = createPlace(doc, "빈 골목")
        assertFalse(ItemLifecycle.impact(d, ItemId.place(key)).needsSheet)
        val deleted = ItemLifecycle.delete(d, ItemId.place(key), actx())!!
        assertEquals("장소 빈 골목 지움", deleted.toast.message)
    }

    @Test
    fun `WLD-15 same name notice and the place merge sheet`() {
        val (d, key) = createPlace(doc, "잿더미 시장")
        assertEquals("◇ 같은 이름 2 · 잿더미 시장", ItemLifecycle.sameName(d, WorldKind.PLACE, key))
        assertNull(ItemLifecycle.sameName(doc, WorldKind.PLACE, K.PLACE_LOWER_STREET))
        assertFalse(MergePlan.canMerge(d, WorldKind.PLACE, key, key))
        val ui = MergePlan.mergeSheet(d, WorldKind.PLACE, key, K.PLACE_ASH_MARKET, MergeChoices())!!
        assertEquals("장소 합치기", ui.title)
        assertEquals("잿더미 시장 → 잿더미 시장 · 기본은 잿더미 시장 값", ui.sub)
        assertEquals("옮길 참조 · 0", ui.refsLine)
        assertEquals("잿더미 시장 · ‘합침 → 잿더미 시장’로 휴지통", ui.footer)
        assertEquals(listOf("취소", "합치기"), listOf(ui.cancel, ui.confirm))

        val result = MergeActions.mergeItems(d, ItemKind.PLACE, key, K.PLACE_ASH_MARKET, actx())
        val merged = result.doc.index.place(key)!!
        assertNotNull(merged.deletedAt)
        assertEquals(TrashReason.MERGED, result.doc.index.trashEntry(merged.trashEntryKey!!)!!.reason.known)
        assertEquals(CardAccess.Open, CardAccess.of(result.doc, WorldKind.PLACE, K.PLACE_ASH_MARKET, false))
    }

    @Test
    fun `WLD-T70 two first reveals give a comparison row whose choice is kept`() {
        val created = WorldItems.createTerm(doc, "빛돌", WorldTestFixture.ctx()) as CreateResult.Created
        val withReveal = (TermEdits.setReveal(created.doc, created.key, RevealTarget.Block(K.BLOCK_B32), WorldTestFixture.ctx()) as RevealEdit.Applied).doc
        val ui = MergePlan.mergeSheet(withReveal, WorldKind.TERM, created.key, T1, MergeChoices())!!
        val reveal = ui.reveal!!
        assertEquals("첫 공개", reveal.field)
        assertEquals("1-7 씬 3 · #044", reveal.targetValue)
        assertFalse(reveal.fromMerged)
        assertEquals("다른 칸 · 1", ui.conflictsLine)
        val chosen = MergePlan.toggle(MergeChoices(), reveal, fromMerged = true)
        assertTrue(chosen.revealFromMerged)
        assertTrue(MergePlan.mergeSheet(withReveal, WorldKind.TERM, created.key, T1, chosen)!!.reveal!!.fromMerged)
    }

    @Test
    fun `reference parts read like the trash rows`() {
        assertEquals(
            "대사 3 · @ 1 · 씬 등장 1".split(" · ").toSet(),
            MergePlan.refParts(
                mapOf(
                    io.github.lenticualrstone.civilight.domain.trash.RefKind.SPEAKER to 3,
                    io.github.lenticualrstone.civilight.domain.trash.RefKind.LINK to 1,
                    io.github.lenticualrstone.civilight.domain.trash.RefKind.SILENT_CAST to 1,
                ),
            ).split(" · ").toSet(),
        )
    }
}
