package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 00-domain-model §5.4 walk-throughs not already covered by the TC-20 cases, and §5.5 edges. */
class WalkThroughTest {
    private val sceneBlocks = (36..48).map { K.block(1, 7, it) }

    @Test
    fun `SCR-39 block 043 deleted alone, later its scene deleted`() {
        val ctx = Fx.ctx()
        val block = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        val blockEntry = block.entries.single()
        assertEquals(TrashKind.BLOCK_GROUP, blockEntry.kind.known)
        val scene = TrashOps.delete(block.doc, ItemId.unit(K.SCENE_1_7_3), ctx)
        val sceneEntry = scene.entries.single()
        assertEquals(TrashKind.SCENE, sceneEntry.kind.known)
        val d = scene.doc
        // #043 carries its own mark; the scene carries one; the other blocks carry none.
        assertEquals(blockEntry.key, d.index.block(K.BLOCK_043)!!.trashEntryKey)
        assertEquals(sceneEntry.key, d.index.unit(K.SCENE_1_7_3)!!.trashEntryKey)
        assertTrue((sceneBlocks - K.BLOCK_043).all { d.index.block(it)!!.deletedAt == null })
        assertTrue(sceneBlocks.none { d.liveness.isBlockLive(it) })
        // Records relocated twice keep their first original anchor.
        val r4 = d.index.record(K.R4)!!
        assertEquals(AnchorState.ORPHANED, r4.anchorState.known)
        assertEquals(Anchor.AtBlock(K.BLOCK_043), r4.originalAnchor)
        assertEquals(Anchor.AtBlock(K.BLOCK_1_8_001), r4.anchor)
        assertEquals(Anchor.AtBlock(K.BLOCK_044), d.index.record(K.R9)!!.originalAnchor)

        // Restoring the scene brings back the other blocks; #043 stays in its block group.
        val sceneBack = Fx.restored(RestoreOps.restore(d, sceneEntry.key, ctx)).doc
        assertTrue((sceneBlocks - K.BLOCK_043).all { sceneBack.liveness.isBlockLive(it) })
        assertFalse(sceneBack.liveness.isBlockLive(K.BLOCK_043))
        assertEquals(listOf(K.BLOCK_043), sceneBack.index.trashEntry(blockEntry.key)!!.targetKeys)
        // R9 goes back to #044; R4 waits for #043.
        assertEquals(Anchor.AtBlock(K.BLOCK_044), sceneBack.index.record(K.R9)!!.anchor)
        assertEquals(AnchorState.NORMAL, sceneBack.index.record(K.R9)!!.anchorState.known)
        assertEquals(AnchorState.ORPHANED, sceneBack.index.record(K.R4)!!.anchorState.known)

        // Restoring #043 now puts it back between #042 and #044 and re-attaches R4, R5, R5A.
        val blockBack = Fx.restored(RestoreOps.restore(sceneBack, blockEntry.key, ctx))
        val live = blockBack.doc.manuscript.liveBlocks.map { it.key }
        assertEquals(live.indexOf(K.BLOCK_042) + 1, live.indexOf(K.BLOCK_043))
        assertEquals(live.indexOf(K.BLOCK_043) + 1, live.indexOf(K.BLOCK_044))
        assertEquals(setOf(K.R4, K.R5, K.R5A), blockBack.report.reattached.toSet())
        assertTrue(blockBack.doc.trash.entries.none { it.key == blockEntry.key || it.key == sceneEntry.key })
    }

    @Test
    fun `scene marks set elsewhere settle like a delete here`() {
        // domain:story's SceneTrash sets the mark, writes the entry and rebases 때; this module settles anchors and acks.
        val ctx = Fx.ctx()
        val doc = Fx.doc()
        val entryKey = Fx.key("TRSH", 71)
        val marked = doc.setMark(ItemId.unit(K.SCENE_1_7_3), ctx.now, entryKey, ctx.now, ctx.deviceId)
        val settled = TrashOps.settleDelete(doc, marked, ctx)
        val own = TrashOps.delete(doc, ItemId.unit(K.SCENE_1_7_3), ctx)
        assertEquals(own.relocations, settled.relocations)
        assertEquals(own.doc.changes, settled.doc.changes)
        assertEquals(
            Messages.sceneDeleteToast(own)!!.text,
            Messages.sceneDeleteToast(doc, settled.doc, K.SCENE_1_7_3, settled.relocations).text,
        )
        val unmarked = settled.doc.setMark(ItemId.unit(K.SCENE_1_7_3), null, null, ctx.now, ctx.deviceId)
        val restored = RestoreOps.settleRestore(settled.doc, unmarked, ctx)
        assertEquals(setOf(K.R4, K.R5, K.R5A, K.R9), restored.reattached.toSet())
        assertTrue(listOf(K.R4, K.R5, K.R5A, K.R9).all { restored.doc.index.record(it)!!.anchorState.known == AnchorState.NORMAL })
    }

    @Test
    fun `SCR-39 restoring 043 while its scene is in the trash offers the scene choice`() {
        val ctx = Fx.ctx()
        val block = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        val scene = TrashOps.delete(block.doc, ItemId.unit(K.SCENE_1_7_3), ctx)
        val outcome = RestoreOps.restore(scene.doc, block.entries.single().key, ctx)
        assertEquals(RestoreOutcome.NeedsSceneChoice(K.SCENE_1_7_3, listOf(K.BLOCK_043)), outcome)
    }

    @Test
    fun `기록 지우기 on an orphaned record, then restore`() {
        val ctx = Fx.ctx()
        val blockGone = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        val orphan = blockGone.doc.index.record(K.R4)!!
        assertEquals(AnchorState.ORPHANED, orphan.anchorState.known)
        val removed = TrashOps.delete(blockGone.doc, ItemId.change(K.R4), ctx)
        val entry = removed.entries.single()
        assertEquals(TrashKind.CHANGE, entry.kind.known)
        assertEquals(entry.key, removed.doc.index.record(K.R4)!!.trashEntryKey)
        val row = TrashListing.projectItemRows(removed.doc, ctx).single { it.entryKey == entry.key }
        assertEquals("◆ 리엔 · 하르트 · 마음", row.title)
        assertEquals("변화 · 잿빛 새벽의 등불", row.sub)
        assertTrue(TrashListing.list(listOf(row), TrashFilter.CHANGE).isNotEmpty())
        // Restoring it brings it back still orphaned with its original anchor.
        val back = Fx.restored(RestoreOps.restore(removed.doc, entry.key, ctx)).doc
        val record = back.index.record(K.R4)!!
        assertTrue(back.liveness.isLive(record))
        assertEquals(AnchorState.ORPHANED, record.anchorState.known)
        assertEquals(Anchor.AtBlock(K.BLOCK_043), record.originalAnchor)
        assertEquals(orphan.anchor, record.anchor)
    }

    @Test
    fun `a record restored after its original block came back re-attaches`() {
        val ctx = Fx.ctx()
        val blockGone = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        val removed = TrashOps.delete(blockGone.doc, ItemId.change(K.R4), ctx)
        val blockBack = Fx.restored(RestoreOps.restore(removed.doc, blockGone.entries.single().key, ctx))
        // R4 was in the trash: it is not re-attached with the others.
        assertEquals(setOf(K.R5, K.R5A), blockBack.report.reattached.toSet())
        assertEquals(AnchorState.ORPHANED, blockBack.doc.index.record(K.R4)!!.anchorState.known)
        val recordBack = Fx.restored(RestoreOps.restore(blockBack.doc, removed.entries.single().key, ctx))
        val r4 = recordBack.doc.index.record(K.R4)!!
        assertEquals(Anchor.AtBlock(K.BLOCK_043), r4.anchor)
        assertEquals(AnchorState.NORMAL, r4.anchorState.known)
        assertNull(r4.originalAnchor)
        assertEquals(listOf(K.R4), recordBack.report.reattached)
    }

    @Test
    fun `a record restored after its anchor block was deleted relocates`() {
        val ctx = Fx.ctx()
        val removed = TrashOps.delete(Fx.doc(), ItemId.change(K.R4), ctx)
        val blockGone = TrashOps.delete(removed.doc, ItemId.block(K.BLOCK_043), ctx)
        // The trashed record did not move with R5 and R5A.
        assertEquals(setOf(K.R5, K.R5A), blockGone.relocations.map { it.recordKey }.toSet())
        val back = Fx.restored(RestoreOps.restore(blockGone.doc, removed.entries.single().key, ctx)).doc
        val r4 = back.index.record(K.R4)!!
        assertEquals(Anchor.AtBlock(K.BLOCK_044), r4.anchor)
        assertEquals(AnchorState.ORPHANED, r4.anchorState.known)
        assertEquals(Anchor.AtBlock(K.BLOCK_043), r4.originalAnchor)
    }

    @Test
    fun `TC-20-23 이대로 두기 on a 대상 지워짐 record and 기록 지우기`() {
        val ctx = Fx.ctx()
        val doc = Fx.doc() // 노아 in the trash; N1 anchored at 1-6 #007 is 대상 지워짐
        assertEquals(listOf(K.N1), DerivedMarks.targetDeleted(doc, K.BLOCK_1_6_007).map { it.key })
        // 이대로 두기 on that one reference stores [itemKey, recordKey]; the item row is not kept by it.
        val kept = Fixes.keep(doc, K.NOAH, ctx, referencingKey = K.N1).doc
        val ack = kept.checks.acknowledgements.single { it.checkType == CheckType.DELETED_ITEM }
        assertEquals(listOf(K.NOAH, K.N1), ack.subjectKeys)
        assertFalse(Fixes.isKept(kept, K.NOAH))
        // 기록 지우기 gives the record its own mark; restoring 노아 does not bring it back.
        val removed = TrashOps.delete(kept, ItemId.change(K.N1), ctx)
        assertEquals(TrashKind.CHANGE, removed.entries.single().kind.known)
        val noahBack = Fx.restored(RestoreOps.restore(removed.doc, K.TRASH_NOAH, ctx)).doc
        assertFalse(noahBack.liveness.isLive(noahBack.index.record(K.N1)!!))
        // The deleted.item acks of 노아 go when he is restored (§2.9).
        assertTrue(noahBack.checks.acknowledgements.none { it.checkType == CheckType.DELETED_ITEM })
        val recordBack = Fx.restored(RestoreOps.restore(noahBack, removed.entries.single().key, ctx)).doc
        assertTrue(recordBack.liveness.isLive(recordBack.index.record(K.N1)!!))
        assertEquals(Anchor.AtBlock(K.BLOCK_1_6_007), recordBack.index.record(K.N1)!!.anchor)
    }

    @Test
    fun `export without 휴지통도 포함 while 노아 is in the trash`() {
        val doc = Fx.doc()
        val tags = DerivedMarks.exportNameTags(doc)
        val tag = tags.single()
        assertEquals(K.NOAH, tag.key)
        assertEquals(NameTagKind.CHARACTER, tag.kind.known)
        assertEquals("노아", tag.name)
        assertEquals("NOA", tag.voiceCode)
        assertFalse(tag.hidden)
        assertEquals(NameTagState.NOT_EXPORTED, tag.state.known)
        // The imported copy has no 노아 row, only the tag: lines resolve TAGGED without 되살리기.
        val imported = doc.copy(
            cast = doc.cast.copy(
                characters = doc.cast.characters.filter { it.key != K.NOAH },
                relations = doc.cast.relations.filter { it.key != K.REL_OWEN_NOAH },
            ),
            tombstones = doc.tombstones.copy(nameTags = tags),
            trash = doc.trash.copy(entries = emptyList()),
        )
        val ref = Resolution.resolve(imported, RefTargetKind.CHARACTER, K.NOAH)
        assertEquals(RefState.TAGGED, ref.state)
        assertEquals("지운 인물 · 노아", ref.label)
        assertFalse(ref.canRestore)
        assertTrue(ref.dotted)
        // JSON/CSV exports from the imported copy still print 노아 and use NOA.
        assertEquals("노아", ref.exportName)
        assertEquals("NOA", ref.voiceCode)
        assertEquals(14, Fx.linesOf(imported, K.NOAH).size)
    }

    @Test
    fun `export name tags keep purged tags and skip unreferenced trashed items`() {
        val ctx = Fx.ctx()
        val gordon = TrashOps.delete(Fx.doc(), ItemId.character(K.GORDON), ctx).doc
        // 고든 still speaks in live blocks: tagged. A trashed character nobody references is not.
        val loner = Fx.key("CHAR", 41)
        val withLoner = TrashOps.delete(Fx.addCharacter(gordon, loner, "하린", 13), ItemId.character(loner), ctx).doc
        val purged = PurgeOps.purge(withLoner, K.TRASH_NOAH, ctx).doc
        val tags = DerivedMarks.exportNameTags(purged)
        assertEquals(NameTagState.PURGED, tags.single { it.key == K.NOAH }.state.known)
        assertEquals(NameTagState.NOT_EXPORTED, tags.single { it.key == K.GORDON }.state.known)
        assertNull(tags.firstOrNull { it.key == loner })
    }

    @Test
    fun `TC-20-24 restore the map brings the pin back at the same coordinates`() {
        val ctx = Fx.ctx()
        val before = Fx.doc().index.pin(K.PIN_ASH_MARKET)!!.second
        val place = TrashOps.delete(Fx.doc(), ItemId.place(K.PLACE_ASH_MARKET), ctx)
        val map = TrashOps.delete(place.doc, ItemId.map(K.MAP_LOWER_DISTRICT), ctx)
        // Map first: the pin stays not live while the place is in the trash.
        val mapBack = Fx.restored(RestoreOps.restore(map.doc, map.entries.single().key, ctx)).doc
        assertFalse(mapBack.liveness.isOverlayLive(K.MAP_LOWER_DISTRICT, before))
        val both = Fx.restored(RestoreOps.restore(mapBack, place.entries.single().key, ctx)).doc
        val pin = both.index.pin(K.PIN_ASH_MARKET)!!.second
        assertTrue(both.liveness.isOverlayLive(K.MAP_LOWER_DISTRICT, pin))
        assertEquals(before, pin)
        assertNotNull(DerivedMarks.pinsOf(both, K.PLACE_ASH_MARKET).single().takeIf { it.mapLive && it.mark == null })
    }
}
