package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Delete, restore and keep (CST-40, CST-41, CST-43) through `domain:actions` and `domain:trash`. */
class ItemTrashTest {
    private val recorder = ExampleRecorder.library()

    /** The fixture with 노아 live (fixture conflict #22). */
    private fun noahLive(doc: ProjectDoc = CastFx.doc()): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(deletedAt = null, trashEntryKey = null) else it }),
        trash = doc.trash.copy(entries = doc.trash.entries.filter { it.key != K.TRASH_NOAH }),
    )

    @Test
    fun `CST-40 노아's sheet splits counts and keeps the board's rows`() {
        val doc = noahLive()
        val impact = ItemTrash.impact(doc, EntityKind.CHARACTER, K.NOAH, recorder)!!
        assertTrue(impact.needsSheet)
        val sheet = ItemTrash.sheet(EntityKind.CHARACTER, impact)
        val trash = Messages.impactSheet(impact)
        assertEquals("‘노아’ 지우기", sheet.title)
        assertEquals(trash.sub, sheet.sub)
        assertTrue(sheet.sub.startsWith("인물 · 참조 "))
        assertEquals("같이 휴지통으로", sheet.ownedTitle)
        val relation = sheet.owned.single { it.label == "관계" }
        assertEquals("1", relation.count)
        assertEquals("오웬", relation.sub)
        assertEquals("제자리에 남음 · 지운 인물로 보임", sheet.stayingTitle)
        assertEquals(trash.stayingRows.map { it.label }, sheet.staying.map { "${it.label} ${it.count}" })
        assertTrue(sheet.staying.any { it.label == "레코더 칩" })
        assertEquals("노아", sheet.dottedName)
        assertEquals("휴지통 · 30일 · 되살리면 다시 이어짐", sheet.footnote)
        assertEquals("취소" to "휴지통으로", sheet.cancelLabel to sheet.confirmLabel)
    }

    @Test
    fun `the staying title takes the right particle for every kind`() {
        val doc = CastFx.doc()
        val impact = ItemTrash.impact(doc, EntityKind.FACTION, K.FACTION_UPPER_GUARD, null)!!
        assertEquals("제자리에 남음 · 지운 세력으로 보임", ItemTrash.sheet(EntityKind.FACTION, impact).stayingTitle)
    }

    @Test
    fun `a trashed or unknown item has no impact`() {
        val doc = CastFx.doc()
        assertNull(ItemTrash.impact(doc, EntityKind.CHARACTER, K.NOAH, recorder))
        assertNull(ItemTrash.impact(doc, EntityKind.CHARACTER, "nobody", recorder))
        assertNull(ItemTrash.delete(doc, EntityKind.CHARACTER, K.NOAH, CastFx.ctx().toAction(), recorder))
    }

    @Test
    fun `TC-20-03 no references and nothing owned means no sheet, then the short toast`() {
        val (doc, created) = CharacterEdits.create(CastFx.doc(), "유령", CastFx.ctx())
        val impact = ItemTrash.impact(doc, EntityKind.CHARACTER, created.key, recorder)!!
        assertFalse(impact.needsSheet)
        val done = ItemTrash.delete(doc, EntityKind.CHARACTER, created.key, CastFx.ctx(3).toAction(), recorder)!!
        assertEquals("인물 유령 지움", done.toast.message)
        assertEquals("되돌리기", done.toast.action)
        assertFalse(ItemTrash.isLive(done.doc, EntityKind.CHARACTER, created.key))
    }

    @Test
    fun `CST-40 delete with references, then 되돌리기 brings back exactly the document`() {
        val doc = noahLive()
        val done = ItemTrash.delete(doc, EntityKind.CHARACTER, K.NOAH, CastFx.ctx(5).toAction(), recorder)!!
        assertTrue(done.toast.message.startsWith("인물 노아 지움 · 참조 "))
        assertTrue(done.toast.message.endsWith(" · 점검 센터"))
        assertFalse(ItemTrash.isLive(done.doc, EntityKind.CHARACTER, K.NOAH))
        assertEquals(doc, done.change.revert(done.doc))
    }

    @Test
    fun `CST-43 되살리기 brings the fixture's 노아 back with a report and no undo action`() {
        val doc = CastFx.doc()
        val r = ItemTrash.restore(doc, EntityKind.CHARACTER, K.NOAH, CastFx.ctx().toAction(), authenticated = false)
        assertFalse(r.needsAuth)
        val restored = assertNotNull(r.doc).let { r.doc!! }
        assertTrue(ItemTrash.isLive(restored, EntityKind.CHARACTER, K.NOAH))
        assertTrue(r.toast!!.message.startsWith("노아 되살림"))
        assertNull(r.toast!!.action)
    }

    @Test
    fun `CST-43 a hidden item needs the biometric prompt first`() {
        val doc = CastFx.doc().let { d -> d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == K.NOAH) it.copy(hidden = true) else it })) }
        val r = ItemTrash.restore(doc, EntityKind.CHARACTER, K.NOAH, CastFx.ctx().toAction(), authenticated = false)
        assertTrue(r.needsAuth)
        assertNull(r.doc)
        val ok = ItemTrash.restore(doc, EntityKind.CHARACTER, K.NOAH, CastFx.ctx().toAction(), authenticated = true)
        assertNotNull(ok.doc)
    }

    @Test
    fun `CST-41 이대로 두기 on 오웬's dotted 노아 row keeps that row only`() {
        val doc = CastFx.doc()
        assertFalse(ItemTrash.isKept(doc, K.NOAH, K.REL_OWEN_NOAH))
        val kept = ItemTrash.keep(doc, K.NOAH, K.REL_OWEN_NOAH, CastFx.ctx().toAction())
        assertTrue(ItemTrash.isKept(kept, K.NOAH, K.REL_OWEN_NOAH))
        // Keeping twice adds nothing.
        assertEquals(kept, ItemTrash.keep(kept, K.NOAH, K.REL_OWEN_NOAH, CastFx.ctx(9).toAction()))
    }
}
