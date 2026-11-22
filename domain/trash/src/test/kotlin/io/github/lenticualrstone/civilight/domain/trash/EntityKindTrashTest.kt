package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Delete, restore and purge of every §5.2 kind beyond the TC-20 cases, and undo. */
class EntityKindTrashTest {
    private val ctx = Fx.ctx()

    private fun row(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc, entryKey: String) =
        TrashListing.projectItemRows(doc, ctx).single { it.entryKey == entryKey }

    private fun filtersOf(r: TrashRow) = TrashFilter.entries.filter { it.matches(r.kind) }

    @Test
    fun `field definition delete restore purge`() {
        val doc = Fx.doc()
        val def = ItemId.fieldDef(K.FIELD_AFFILIATION)
        val holders = doc.cast.characters.filter { it.deletedAt == null && K.FIELD_AFFILIATION in it.fields }.map { it.key }
        val impact = Impact.of(doc, def)
        // 소속 records of 리엔 (R1–R3) and 마르타 (W1) go with it; 노아's N1 was already not live.
        assertEquals(4, impact.owned(OwnedKind.CHANGE_RECORD))
        assertEquals(holders.size, impact.owned(OwnedKind.FIELD_VALUE))
        val result = TrashOps.delete(doc, def, ctx)
        val after = result.doc
        val entry = result.entries.single()
        assertEquals(TrashKind.FIELD_DEF, entry.kind.known)
        assertTrue(holders.none { after.liveness.isFieldValueLive(EntityKind.CHARACTER, it, K.FIELD_AFFILIATION) })
        assertTrue(listOf(K.R1, K.R2, K.R3, K.W1).none { after.liveness.isLive(after.index.record(it)!!) })
        // Values stay on the rows (owned rows are not copied or marked).
        assertTrue(holders.all { K.FIELD_AFFILIATION in after.index.character(it)!!.fields })
        val r = row(after, entry.key)
        assertEquals("소속", r.title)
        assertEquals("항목 · 잿빛 새벽의 등불", r.sub)
        assertEquals(listOf(TrashFilter.ALL, TrashFilter.CHARACTER), filtersOf(r))

        val back = Fx.restored(RestoreOps.restore(after, entry.key, ctx)).doc
        assertTrue(listOf(K.R1, K.R2, K.R3, K.W1).all { back.liveness.isLive(back.index.record(it)!!) })

        val purged = PurgeOps.purge(after, entry.key, ctx)
        val p = purged.doc
        assertNull(p.index.fieldDef(K.FIELD_AFFILIATION))
        assertTrue(p.cast.characters.none { K.FIELD_AFFILIATION in it.fields })
        assertTrue(listOf(K.R1, K.R2, K.R3, K.W1, K.N1).all { p.index.record(it) == null })
        assertTrue(p.deletions.entries.any { it.key == K.FIELD_AFFILIATION && it.kind == "fieldDef" })
        assertTrue(purged.nameTags.isEmpty())
    }

    @Test
    fun `map delete restore purge`() {
        val doc = Fx.doc()
        val map = ItemId.map(K.MAP_LOWER_DISTRICT)
        val impact = Impact.of(doc, map)
        assertEquals(5, impact.owned(OwnedKind.PIN))
        assertEquals(1, impact.count(RefKind.SUB_MAP))
        assertTrue(impact.references.any { it.kind == RefKind.SCENE_PLACE && it.ownerKey == K.SCENE_1_5_1 })
        val result = TrashOps.delete(doc, map, ctx)
        val after = result.doc
        val entry = result.entries.single()
        // Regions belong to the map: the scene 지역 is dotted, the scene keeps it.
        val region = after.index.unit(K.SCENE_1_5_1)!!.place!!
        assertEquals(PlaceRefKind.REGION, region.kind.known)
        val resolved = Resolution.resolve(after, region)!!
        assertEquals(RefState.TRASHED, resolved.state)
        assertTrue(resolved.dotted)
        // The child map stays with its parent (breadcrumb on the deleted map).
        assertTrue(after.liveness.isMapLive(K.MAP_ASH_MARKET))
        assertFalse(DerivedMarks.isTopLevelMap(after, K.MAP_ASH_MARKET))
        val r = row(after, entry.key)
        assertEquals("하층 구역 지도", r.title)
        assertEquals(listOf(TrashFilter.ALL, TrashFilter.WORLD), filtersOf(r))

        val back = Fx.restored(RestoreOps.restore(after, entry.key, ctx)).doc
        assertEquals(RefState.LIVE, Resolution.resolve(back, region)!!.state)

        val p = PurgeOps.purge(after, entry.key, ctx).doc
        assertNull(p.maps[K.MAP_LOWER_DISTRICT])
        assertNull(p.index.pin(K.PIN_ASH_MARKET))
        assertTrue(DerivedMarks.isTopLevelMap(p, K.MAP_ASH_MARKET))
        assertTrue(p.deletions.entries.any { it.key == K.MAP_LOWER_DISTRICT && it.kind == "map" })
        // The places themselves are untouched.
        assertTrue(p.liveness.isPlaceLive(K.PLACE_ASH_MARKET))
    }

    @Test
    fun `timeline event delete keeps world-event anchors and purge orphans them`() {
        val doc = Fx.doc()
        val event = ItemId.event(K.EVENT_BLACKOUT)
        val impact = Impact.of(doc, event)
        assertEquals(2, impact.count(RefKind.EVENT_ANCHOR))
        assertEquals(1, impact.count(RefKind.FLASHBACK))
        assertEquals(1, impact.count(RefKind.CANVAS_NODE))
        val result = TrashOps.delete(doc, event, ctx)
        val after = result.doc
        assertTrue(result.relocations.isEmpty())
        for (key in listOf(K.W1, K.W2)) {
            val record = after.index.record(key)!!
            assertEquals(Anchor.WorldEvent(K.EVENT_BLACKOUT), record.anchor)
            assertEquals(AnchorState.NORMAL, record.anchorState.known)
            assertTrue(after.liveness.isLive(record))
        }
        val r = row(after, result.entries.single().key)
        assertEquals("대정전", r.title)
        assertEquals("사건 · 잿빛 새벽의 등불", r.sub)
        assertEquals(listOf(TrashFilter.ALL, TrashFilter.WORLD), filtersOf(r))
        assertEquals(RefState.TRASHED, Resolution.resolve(after, RefTargetKind.EVENT, K.EVENT_BLACKOUT).state)

        val p = PurgeOps.purge(after, result.entries.single().key, ctx).doc
        assertNull(p.index.event(K.EVENT_BLACKOUT))
        for (key in listOf(K.W1, K.W2)) {
            val record = p.index.record(key)!!
            assertEquals(Anchor.WorldEvent(K.EVENT_BLACKOUT), record.anchor)
            assertEquals(AnchorState.ORPHANED, record.anchorState.known)
            assertEquals(Anchor.WorldEvent(K.EVENT_BLACKOUT), record.originalAnchor)
        }
        assertEquals(RefState.MISSING, Resolution.resolve(p, RefTargetKind.EVENT, K.EVENT_BLACKOUT).state)
    }

    @Test
    fun `page delete takes its links out of the counts`() {
        val doc = Fx.doc()
        val page = ItemId.page(K.PAGE_LOWER_LIGHT)
        val termLinks = Impact.of(doc, ItemId.term(K.TERM_ORIGINIUM_LANTERN)).count(RefKind.LINK)
        val noahLinks = Impact.of(doc, ItemId.character(K.NOAH)).count(RefKind.LINK)
        assertFalse(Impact.of(doc, page).needsSheet)
        val result = TrashOps.delete(doc, page, ctx)
        val after = result.doc
        // References inside a non-live container are not counted (CST-41).
        assertEquals(termLinks - 1, Impact.of(after, ItemId.term(K.TERM_ORIGINIUM_LANTERN)).count(RefKind.LINK))
        assertEquals(noahLinks - 1, Impact.of(after, ItemId.character(K.NOAH)).count(RefKind.LINK))
        val r = row(after, result.entries.single().key)
        assertEquals("하층 구역의 빛", r.title)
        assertEquals("페이지 · 잿빛 새벽의 등불", r.sub)
        assertEquals(listOf(TrashFilter.ALL), filtersOf(r))
        // Plain text inside it is untouched.
        assertEquals(doc.pages.getValue(K.PAGE_LOWER_LIGHT).blocks, after.pages.getValue(K.PAGE_LOWER_LIGHT).blocks)

        val purged = PurgeOps.purge(after, result.entries.single().key, ctx)
        assertNull(purged.doc.pages[K.PAGE_LOWER_LIGHT])
        assertTrue(purged.nameTags.isEmpty())
    }

    @Test
    fun `canvas delete takes its nodes along`() {
        val doc = Fx.doc()
        val canvas = ItemId.canvas(K.CANVAS_CHAPTER_1)
        val parts = doc.canvases.getValue(K.CANVAS_CHAPTER_1).let { it.nodes.size + it.edges.size }
        assertEquals(parts, Impact.of(doc, canvas).owned(OwnedKind.CANVAS_PART))
        assertEquals(1, Impact.of(doc, ItemId.character(K.RIEN)).count(RefKind.CANVAS_NODE))
        val result = TrashOps.delete(doc, canvas, ctx)
        assertEquals(0, Impact.of(result.doc, ItemId.character(K.RIEN)).count(RefKind.CANVAS_NODE))
        val r = row(result.doc, result.entries.single().key)
        assertEquals("1장 구상", r.title)
        assertEquals(listOf(TrashFilter.ALL), filtersOf(r))
        assertNull(PurgeOps.purge(result.doc, result.entries.single().key, ctx).doc.canvases[K.CANVAS_CHAPTER_1])
    }

    @Test
    fun `scene purge removes its blocks and keeps the counter`() {
        val doc = Fx.doc()
        val blocks = Fx.blocksOfScene(doc, K.SCENE_1_6_5).map { it.key }
        assertEquals(3, blocks.size)
        val counter = doc.index.unit(K.EPISODE_1_6)!!.nextBlockNumber
        val purged = PurgeOps.purge(doc, K.TRASH_SCENE_1_6_5, ctx)
        val p = purged.doc
        assertNull(p.index.unit(K.SCENE_1_6_5))
        assertTrue(blocks.none { p.index.block(it) != null })
        assertEquals(counter, p.index.unit(K.EPISODE_1_6)!!.nextBlockNumber)
        assertTrue(purged.removedKeys.containsAll(blocks + K.SCENE_1_6_5))
        val logged = p.deletions.entries.filter { it.deletedAt == ctx.now }
        assertEquals(setOf(K.SCENE_1_6_5) + blocks, logged.map { it.key }.toSet())
        assertEquals("unit", logged.single { it.key == K.SCENE_1_6_5 }.kind)
        assertNull(p.index.trashEntry(K.TRASH_SCENE_1_6_5))
        assertNotNull(p.index.trashEntry(K.TRASH_NOAH))
        // A unit leaves no name tag; a recorder link to it gets the code and title.
        assertTrue(purged.nameTags.isEmpty())
        assertEquals("1-6 씬 5 · 시장의 밤", purged.purgedTargets.single { it.key == K.SCENE_1_6_5 }.label)
    }

    @Test
    fun `block group purge keeps numbers unissued`() {
        val doc = Fx.doc()
        val counter = doc.index.unit(K.EPISODE_1_5)!!.nextBlockNumber
        val p = PurgeOps.purge(doc, K.TRASH_BLOCKS_1_5_2, ctx).doc
        assertNull(p.index.block(K.block(1, 5, 8)))
        assertNull(p.index.block(K.block(1, 5, 9)))
        assertEquals(counter, p.index.unit(K.EPISODE_1_5)!!.nextBlockNumber)
        assertTrue(p.deletions.entries.any { it.key == K.block(1, 5, 8) && it.kind == "block" })
    }

    @Test
    fun `change record purge`() {
        val p = PurgeOps.purge(Fx.doc(), K.TRASH_RECORD, ctx).doc
        assertNull(p.index.record(K.RECORD_TRASHED))
        assertNull(p.index.trashEntry(K.TRASH_RECORD))
        assertTrue(p.deletions.entries.any { it.key == K.RECORD_TRASHED && it.kind == "change" })
    }

    @Test
    fun `undo puts back exactly what the delete changed`() {
        val live = Fx.noahLive()
        val noah = TrashOps.delete(live, ItemId.character(K.NOAH), ctx)
        assertEquals(live, TrashOps.undo(noah.doc, noah))

        val doc = Fx.doc()
        val empty = TrashOps.delete(doc, ItemId.block(K.BLOCK_1_8_001), ctx)
        assertEquals(doc, TrashOps.undo(empty.doc, empty))

        val scene = TrashOps.delete(doc, ItemId.unit(K.SCENE_1_7_3), ctx)
        assertTrue(scene.relocations.isNotEmpty())
        assertEquals(doc, TrashOps.undo(scene.doc, scene))
    }

    @Test
    fun `undo keeps later edits of other rows`() {
        val live = Fx.noahLive()
        val noah = TrashOps.delete(live, ItemId.character(K.NOAH), ctx)
        val edited = Fx.editBlock(noah.doc, K.BLOCK_036) { it.copy(runs = listOf(TextRun.Plain("불은 꺼졌다."))) }
        val undone = TrashOps.undo(edited, noah)
        assertTrue(undone.liveness.isCharacterLive(K.NOAH))
        assertEquals(listOf(TextRun.Plain("불은 꺼졌다.")), undone.index.block(K.BLOCK_036)!!.runs)
        assertNull(undone.index.trashEntry(noah.entries.single().key))
    }

    @Test
    fun `deleting what is already in the trash is skipped`() {
        val doc = Fx.doc()
        val result = TrashOps.delete(doc, listOf(ItemId.character(K.NOAH), ItemId.unit(K.SCENE_1_6_5), ItemId.block(K.block(1, 6, 30))), ctx)
        assertEquals(3, result.skipped.size)
        assertTrue(result.entries.isEmpty())
        assertEquals(doc.trash, result.doc.trash)
    }

    @Test
    fun `world items delete with their owned rows`() {
        val doc = Fx.doc()
        val faction = Impact.of(doc, ItemId.faction(K.FACTION_UPPER_GUARD))
        // 상층 경비대's 수장 value goes with it; the region on 세계 지도 stays as a dotted link.
        assertEquals(1, faction.owned(OwnedKind.FIELD_VALUE))
        assertTrue(faction.references.any { it.kind == RefKind.MAP_LINK && it.ownerKey == K.REGION_UPPER_GUARD })
        val term = Impact.of(doc, ItemId.term(K.TERM_BLACKOUT))
        assertEquals(1, term.owned(OwnedKind.FIRST_REVEAL))
        assertTrue(term.references.any { it.kind == RefKind.EVENT_TERM && it.ownerKey == K.EVENT_BLACKOUT })
        val after = TrashOps.delete(doc, ItemId.term(K.TERM_BLACKOUT), ctx).doc
        assertFalse(after.liveness.isLive(after.index.record(K.REVEAL_BLACKOUT)!!))
        assertEquals(
            listOf(K.REVEAL_BLACKOUT),
            DerivedMarks.targetDeleted(after, K.BLOCK_B41).map { it.key },
        )
        assertTrue(after.changes.records.filter { it.target is ChangeTarget.TermReveal }.all { it.deletedAt == null })
    }
}
