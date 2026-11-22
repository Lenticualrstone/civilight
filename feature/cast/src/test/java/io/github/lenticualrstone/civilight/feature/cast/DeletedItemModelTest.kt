package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.field.NewField
import io.github.lenticualrstone.civilight.feature.cast.field.LinkFixes
import io.github.lenticualrstone.civilight.feature.cast.trash.BulkKind
import io.github.lenticualrstone.civilight.feature.cast.trash.DeletedItemModel
import io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** A deleted item's check row in the shared delete sheet (CST-42, CST-44) and dotted `연결` values (CST-41, CST-47). */
class DeletedItemModelTest {
    private val doc = CastFx.doc()
    private val trash = CastFx.action().trash
    private val recorder = ExampleRecorder.library()

    private fun noahLines(d: ProjectDoc) = d.manuscript.liveBlocks.count { it.speakerKey == K.NOAH }

    @Test
    fun `TC-20-04 노아's row in the trash has its counts, the speaker change, 되살리기 and 이대로 두기`() {
        val row = DeletedItemModel.row(doc, EntityKind.CHARACTER, K.NOAH, trash, recorder, itemsLocked = true)!!
        val view = DeletedItemModel.view(row, doc)
        assertEquals("◇ 지운 인물 · 노아", view.title)
        assertTrue(view.sub, view.sub.startsWith("휴지통 · ") && view.sub.contains("일 남음") && view.sub.contains("대사 "))
        assertEquals(listOf(BulkKind.SPEAKER), view.bulk.map { it.kind }.filter { it == BulkKind.SPEAKER })
        assertEquals("화자 한꺼번에 바꾸기", view.bulk.first { it.kind == BulkKind.SPEAKER }.label)
        assertTrue(view.canRestore)
        assertFalse(view.kept)
        // A live item has no check row.
        assertNull(DeletedItemModel.row(doc, EntityKind.CHARACTER, K.RIEN, trash, recorder, itemsLocked = true))
    }

    @Test
    fun `TC-20-04 화자 한꺼번에 바꾸기 → 오웬 previews, changes every live line in one step, and reverts`() {
        val n = noahLines(doc)
        assertTrue(n > 0)
        assertEquals("대사 $n · 노아 → 오웬", DeletedItemModel.preview(doc, EntityKind.CHARACTER, K.NOAH, K.OWEN, BulkKind.SPEAKER))
        val changed = DeletedItemModel.apply(doc, EntityKind.CHARACTER, K.NOAH, K.OWEN, BulkKind.SPEAKER, trash)!!
        assertEquals(0, noahLines(changed.doc))
        assertEquals("대사 $n · 노아 → 오웬", changed.line)
        assertEquals(n, noahLines(changed.change.revert(changed.doc)))
        // Never onto itself or onto an item that is not live.
        assertNull(DeletedItemModel.apply(doc, EntityKind.CHARACTER, K.NOAH, K.NOAH, BulkKind.SPEAKER, trash))
    }

    @Test
    fun `@ 한꺼번에 바꾸기 points the links at the new item and keeps their text`() {
        val before = DeletedItemModel.count(doc, ItemTrash.itemId(EntityKind.CHARACTER, K.NOAH), BulkKind.LINK)
        assertTrue("노아 has `@` links in the manuscript (TC-20-01 `@연결 3`)", before > 0)
        val changed = DeletedItemModel.apply(doc, EntityKind.CHARACTER, K.NOAH, K.OWEN, BulkKind.LINK, trash)!!
        assertEquals(0, DeletedItemModel.count(changed.doc, ItemTrash.itemId(EntityKind.CHARACTER, K.NOAH), BulkKind.LINK))
        assertTrue(changed.line.startsWith("@ $before · 노아 → 오웬"))
    }

    @Test
    fun `the @ button shows only while the project holds links it can change`() {
        val item = ItemTrash.itemId(EntityKind.CHARACTER, K.NOAH)
        val retargeted = DeletedItemModel.apply(doc, EntityKind.CHARACTER, K.NOAH, K.OWEN, BulkKind.LINK, trash)!!.doc
        assertEquals(0, DeletedItemModel.count(retargeted, item, BulkKind.LINK))
        val row = DeletedItemModel.row(retargeted, EntityKind.CHARACTER, K.NOAH, trash, recorder, itemsLocked = true)!!
        assertTrue(DeletedItemModel.view(row, retargeted).bulk.none { it.kind == BulkKind.LINK })
    }

    @Test
    fun `TC-20-28 이대로 두기 moves the row to 맞다고 표시한 것 and the button goes`() {
        val kept = ItemTrash.keepItem(doc, K.NOAH, CastFx.action())
        val view = DeletedItemModel.view(DeletedItemModel.row(kept, EntityKind.CHARACTER, K.NOAH, trash, recorder, itemsLocked = true)!!, kept)
        assertTrue(view.kept)
    }

    @Test
    fun `TC-20-06 after a permanent delete the row reads 영구 삭제 and loses 되살리기`() {
        val purged = CharacterActions.purge(doc, recorder, K.NOAH, CastFx.action())!!
        val row = DeletedItemModel.row(purged.doc, EntityKind.CHARACTER, K.NOAH, trash, purged.recorder, itemsLocked = true)
        assertNotNull(row)
        val view = DeletedItemModel.view(row!!, purged.doc)
        assertEquals("영구 삭제 · 노아", view.title)
        assertFalse(view.canRestore)
    }

    @Test
    fun `TC-20-13 variant a deleted faction in a 연결 value is dotted with 되살리기 and 이대로 두기`() {
        val ctx = CastFx.ctx()
        val (withDef, def) = FieldDefinitions.add(doc, NewField("소속 세력", FieldType.LINK, linkTargetKind = FieldLinkTargetKind.FACTION), ctx)
        val linked = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.RIEN, def.key, FieldValue.of(FieldPayload.Link(listOf(K.FACTION_UPPER_GUARD))), ctx)
        assertEquals(emptyList<Any>(), LinkFixes.rows(linked, EntityKind.FACTION, listOf(K.FACTION_UPPER_GUARD), K.RIEN, hiddenUnlocked = false))

        val deleted = ItemTrash.delete(linked, EntityKind.FACTION, K.FACTION_UPPER_GUARD, CastFx.action(), recorder)!!.doc
        val row = LinkFixes.rows(deleted, EntityKind.FACTION, listOf(K.FACTION_UPPER_GUARD), K.RIEN, hiddenUnlocked = false).single()
        assertEquals("지운 세력 · 상층 경비대", row.label)
        assertTrue(row.canRestore)
        assertFalse(row.kept)

        val kept = ItemTrash.keep(deleted, K.FACTION_UPPER_GUARD, K.RIEN, CastFx.action())
        assertTrue(LinkFixes.rows(kept, EntityKind.FACTION, listOf(K.FACTION_UPPER_GUARD), K.RIEN, hiddenUnlocked = false).single().kept)
    }
}
