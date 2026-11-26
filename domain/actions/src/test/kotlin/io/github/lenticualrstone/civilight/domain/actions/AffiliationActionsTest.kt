package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.trash.DeletedItems
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.MergeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.RefKind
import io.github.lenticualrstone.civilight.domain.trash.ReferenceScanner
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationSection
import io.github.lenticualrstone.civilight.domain.world.item.BasisResult
import io.github.lenticualrstone.civilight.domain.world.item.BasisSlot
import io.github.lenticualrstone.civilight.domain.world.item.OrderList
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.AffiliationBasis
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rule 20 for `거점` and the place card orders, through the trash engine; the action wrappers. */
class AffiliationActionsTest {
    private fun k(tag: String, n: Int): Key = ExampleKeys.TIME + tag + n.toString().padStart(12, '0')
    private val pa = k("APLC", 1)
    private val pb = k("APLC", 2)
    private val f1 = k("AFAC", 1)
    private val f2 = k("AFAC", 2)
    private val dAff = k("AFDF", 1)
    private val dRes = k("AFDF", 2)
    private val c1 = k("ACHR", 1)
    private val c2 = k("ACHR", 2)
    private val c3 = k("ACHR", 3)
    private val c4 = k("ACHR", 4)
    private val c5 = k("ACHR", 5)

    private fun link(vararg keys: Key) = FieldValue.of(FieldPayload.Link(keys.toList()))

    /** 오리지아 › 하층 거리; 등불 결사 (거점 오리지아), 경비대 (거점 하층 거리); 리엔 · 세라 · 카일 in factions, 노아 · 하나 living here. */
    private fun doc(): ProjectDoc {
        val base = Fx.doc()
        val o = OrderKeys.evenlySpaced(5)
        val last = base.project.fieldDefs.maxOf { it.order }
        fun def(key: Key, name: String, target: FieldLinkTargetKind, n: Int) = FieldDef(
            key = key, entityKind = EntityKind.CHARACTER.open(), name = name, type = FieldType.LINK.open(),
            order = last + n, linkTargetKind = target.open(),
        )
        fun c(n: Int, key: Key, name: String, fields: Map<Key, FieldValue>) = Character(key = key, recordNo = 200 + n, name = name, order = o[n - 1], fields = fields)
        return base.copy(
            project = base.project.copy(
                fieldDefs = base.project.fieldDefs + def(dAff, "소속", FieldLinkTargetKind.FACTION, 1) + def(dRes, "거주", FieldLinkTargetKind.PLACE, 2),
                cast = base.project.cast.copy(affiliationBasis = AffiliationBasis(affiliationFieldKey = dAff, residenceFieldKey = dRes)),
            ),
            cast = base.cast.copy(
                characters = listOf(
                    c(1, c1, "리엔", mapOf(dAff to link(f1, f2))),
                    c(2, c2, "세라", mapOf(dAff to link(f1))),
                    c(3, c3, "카일", mapOf(dAff to link(f2))),
                    c(4, c4, "노아", mapOf(dRes to link(pa))),
                    c(5, c5, "하나", mapOf(dRes to link(pb))),
                ),
                relations = emptyList(),
            ),
            world = base.world.copy(
                places = listOf(Place(key = pa, order = o[0], name = "오리지아"), Place(key = pb, order = o[1], name = "하층 거리", parentPlaceKey = pa)),
                factions = listOf(Faction(key = f1, order = o[0], name = "등불 결사", baseKey = pa), Faction(key = f2, order = o[1], name = "경비대", baseKey = pb)),
            ),
        )
    }

    private fun section(d: ProjectDoc, include: Boolean = true): AffiliationSection =
        AffiliationActions.section(d, pa, LensPoint.Latest, include)!!

    @Test
    fun `place merge moves the base to the target and undo brings it back`() {
        val ctx = Fx.ctx()
        val d = doc()
        assertTrue(ReferenceScanner(d).referencesTo(ItemId(ItemKind.PLACE, pb)).any { it.kind == RefKind.BASE_PLACE && it.ownerKey == f2 })
        val result = MergeOps.merge(d, ItemKind.PLACE, pb, pa, ctx.trash)
        assertEquals(pa, result.doc.index.faction(f2)!!.baseKey)
        assertTrue(result.record.movedRefs.any { it.ownerKey == f2 && it.field == "baseKey" })
        assertEquals(listOf(f1, f2), section(result.doc, include = false).factions.map { it.factionKey })
        assertEquals(pb, MergeOps.undo(result.doc, result).index.faction(f2)!!.baseKey)
    }

    @Test
    fun `faction and character merges move order entries to the target`() {
        val ctx = Fx.ctx()
        var d = doc()
        d = d.copy(world = d.world.copy(
            places = d.world.places.map { if (it.key == pa) it.copy(factionOrder = listOf(f2, f1), looseOrder = listOf(c5, c4)) else it },
            factions = d.world.factions.map { if (it.key == f1) it.copy(memberOrder = listOf(c2, c1)) else it },
        ))
        val factions = MergeOps.merge(d, ItemKind.FACTION, f2, f1, ctx.trash).doc
        assertEquals(listOf(f1), factions.index.place(pa)!!.factionOrder)
        assertEquals(listOf(listOf(c2, c1, c3)), section(factions).factions.map { g -> g.members.map { it.characterKey } })

        val people = MergeOps.merge(d, ItemKind.CHARACTER, c2, c1, ctx.trash).doc
        assertEquals(listOf(c1), people.index.faction(f1)!!.memberOrder)
        // The merged person is no dotted ghost beside the target (only a plain delete shows dotted).
        assertFalse(section(people).factions.any { g -> g.members.any { it.characterKey == c2 } })
        val loose = MergeOps.merge(d, ItemKind.CHARACTER, c5, c4, ctx.trash).doc
        assertEquals(listOf(c4), loose.index.place(pa)!!.looseOrder)
        assertFalse(section(loose).loose.any { it.characterKey == c5 })
        assertTrue(section(loose).loose.any { it.characterKey == c4 })
    }

    @Test
    fun `a merged faction's base and member order and a merged place's orders carry over, undo takes them back`() {
        val ctx = Fx.ctx()
        var d = doc()
        // 경비대 (거점 하층 거리, members 카일 · 리엔 + a purged key) merges into 등불 결사 (no 거점, members 세라 · 리엔).
        d = d.copy(world = d.world.copy(factions = d.world.factions.map {
            when (it.key) {
                f1 -> it.copy(baseKey = null, memberOrder = listOf(c2, c1))
                f2 -> it.copy(memberOrder = listOf(c3, c1, "purged"))
                else -> it
            }
        }))
        val result = MergeOps.merge(d, ItemKind.FACTION, f2, f1, ctx.trash)
        val target = result.doc.index.faction(f1)!!
        assertEquals(pb, target.baseKey)
        assertEquals(listOf(c2, c1, c3), target.memberOrder)
        assertEquals(ctx.trash.now, target.updatedAt)
        val undone = MergeOps.undo(result.doc, result).index.faction(f1)!!
        assertNull(undone.baseKey)
        assertEquals(listOf(c2, c1), undone.memberOrder)

        // A target with its own 거점 keeps it.
        val kept = MergeOps.merge(doc(), ItemKind.FACTION, f2, f1, ctx.trash).doc.index.faction(f1)!!
        assertEquals(pa, kept.baseKey)

        // 하층 거리 (orders 경비대 · 등불 결사, 하나 · 노아) merges into 오리지아 (orders 등불 결사, 노아).
        var p = doc()
        p = p.copy(world = p.world.copy(places = p.world.places.map {
            when (it.key) {
                pa -> it.copy(factionOrder = listOf(f1), looseOrder = listOf(c4))
                pb -> it.copy(factionOrder = listOf(f2, f1, "gone"), looseOrder = listOf(c5, c4, "gone"))
                else -> it
            }
        }))
        val placeMerge = MergeOps.merge(p, ItemKind.PLACE, pb, pa, ctx.trash)
        val place = placeMerge.doc.index.place(pa)!!
        assertEquals(listOf(f1, f2), place.factionOrder)
        assertEquals(listOf(c4, c5), place.looseOrder)
        val placeUndone = MergeOps.undo(placeMerge.doc, placeMerge).index.place(pa)!!
        assertEquals(listOf(f1), placeUndone.factionOrder)
        assertEquals(listOf(c4), placeUndone.looseOrder)
    }

    @Test
    fun `a purged place's children move up to its parent, the base keeps the name tag`() {
        val ctx = Fx.ctx()
        val pc = k("APLC", 3)
        val pd = k("APLC", 4)
        var d = doc()
        // 오리지아 › 하층 거리 › 골목 › 막다른 곳.
        d = d.copy(world = d.world.copy(places = d.world.places + Place(key = pc, order = "y", name = "골목", parentPlaceKey = pb) +
            Place(key = pd, order = "z", name = "막다른 곳", parentPlaceKey = pc)))
        val del = TrashOps.delete(d, ItemId(ItemKind.PLACE, pb), ctx.trash)
        val purged = PurgeOps.purge(del.doc, del.entries.single().key, ctx.trash).doc
        assertEquals(pa, purged.index.place(pc)!!.parentPlaceKey)
        assertEquals(ctx.trash.now, purged.index.place(pc)!!.updatedAt)
        assertEquals(pc, purged.index.place(pd)!!.parentPlaceKey)
        assertEquals(setOf(pc, pd), io.github.lenticualrstone.civilight.domain.world.item.Affiliations(purged).subPlaces(pa))
        assertEquals(pb, purged.index.faction(f2)!!.baseKey)

        // A top-level place: its children have no parent afterwards.
        val delTop = TrashOps.delete(d, ItemId(ItemKind.PLACE, pa), ctx.trash)
        val purgedTop = PurgeOps.purge(delTop.doc, delTop.entries.single().key, ctx.trash).doc
        assertNull(purgedTop.index.place(pb)!!.parentPlaceKey)
        assertEquals(pb, purgedTop.index.place(pc)!!.parentPlaceKey)
    }

    @Test
    fun `trash and restore keep every key, the trashed faction shows dotted meanwhile`() {
        val ctx = Fx.ctx()
        val d = doc().let { it.copy(world = it.world.copy(places = it.world.places.map { p -> if (p.key == pa) p.copy(factionOrder = listOf(f2, f1)) else p })) }
        val deleted = TrashOps.delete(d, ItemId(ItemKind.FACTION, f2), ctx.trash)
        val trashed = section(deleted.doc)
        assertEquals(listOf(true, false), trashed.factions.map { it.deleted })
        assertEquals(1, trashed.factionCount)
        assertEquals(listOf(f2, f1), deleted.doc.index.place(pa)!!.factionOrder)

        val placeGone = TrashOps.delete(d, ItemId(ItemKind.PLACE, pb), ctx.trash).doc
        assertEquals(pb, placeGone.index.faction(f2)!!.baseKey)
        assertTrue(section(placeGone).factions.single { it.factionKey == f2 }.base!!.deleted)
        // The deleted-item row counts the 거점 and offers 되살리기 · 이대로 두기; 이대로 두기 settles it.
        val row = DeletedItems.rows(placeGone, ctx.trash).single { it.item == ItemId(ItemKind.PLACE, pb) }
        assertTrue(row.body.contains("거점 1"))
        assertTrue(row.actions.containsAll(listOf("되살리기", "이대로 두기")))
        val kept = Fixes.keep(placeGone, pb, ctx.trash).doc
        assertTrue(DeletedItems.rows(kept, ctx.trash).single { it.item == ItemId(ItemKind.PLACE, pb) }.kept)

        val restored = RestoreOps.restore(deleted.doc, deleted.entries.single().key, ctx.trash) as RestoreOutcome.Restored
        assertEquals(listOf(false, false), section(restored.doc).factions.map { it.deleted })
        assertEquals(listOf(f2, f1), section(restored.doc).factions.map { it.factionKey })
    }

    @Test
    fun `purge drops order entries and clears a basis slot whose field is gone`() {
        val ctx = Fx.ctx()
        var d = doc()
        d = d.copy(world = d.world.copy(factions = d.world.factions.map { if (it.key == f1) it.copy(memberOrder = listOf(c2, c1)) else it }))
        val del = TrashOps.delete(d, ItemId(ItemKind.CHARACTER, c2), ctx.trash)
        val purged = PurgeOps.purge(del.doc, del.entries.single().key, ctx.trash).doc
        assertEquals(listOf(c1), purged.index.faction(f1)!!.memberOrder)

        val delField = TrashOps.delete(d, ItemId(ItemKind.FIELD_DEF, dRes), ctx.trash)
        // Trashed: the key stays (a restore uses it again) but the slot is not used.
        assertEquals(dRes, delField.doc.project.cast.affiliationBasis.residenceFieldKey)
        assertFalse(section(delField.doc).looseAvailable)
        val fieldPurged = PurgeOps.purge(delField.doc, delField.entries.single().key, ctx.trash).doc
        assertNull(fieldPurged.project.cast.affiliationBasis.residenceFieldKey)
        assertEquals(dAff, fieldPurged.project.cast.affiliationBasis.affiliationFieldKey)

        // A purged base place: the faction keeps the key and shows the name tag, dotted.
        val placeDel = TrashOps.delete(d, ItemId(ItemKind.PLACE, pb), ctx.trash)
        val placePurged = PurgeOps.purge(placeDel.doc, placeDel.entries.single().key, ctx.trash).doc
        val base = AffiliationActions.factionMembers(placePurged, f2, LensPoint.Latest)!!.base!!
        assertEquals("하층 거리" to true, base.name to base.deleted)
    }

    @Test
    fun `action wrappers - one step each with its undo label, no-ops return the same doc`() {
        val ctx = Fx.ctx()
        val d = doc()
        val s = section(d)
        val moved = AffiliationActions.moveFaction(d, s, f2, 0, ctx)
        assertEquals(listOf(f2, f1), section(moved).factions.map { it.factionKey })
        assertEquals("세력 순서 바꿈", AffiliationActions.undoLabel(OrderList.Factions(pa)))
        assertEquals("인물 순서 바꿈", AffiliationActions.undoLabel(OrderList.Members(f1)))
        assertEquals("무소속 순서 바꿈", AffiliationActions.undoLabel(OrderList.Loose(pa)))
        assertSame(d, AffiliationActions.moveUp(d, OrderList.Loose(pa), s.loose.map { it.characterKey }, s.loose.first().characterKey, ctx))
        val down = AffiliationActions.moveDown(d, OrderList.Loose(pa), s.loose.map { it.characterKey }, c4, ctx)
        assertEquals(listOf(c5, c4), section(down).loose.map { it.characterKey })

        val cleared = (AffiliationActions.setBasis(d, BasisSlot.AFFILIATION, null, ctx) as BasisResult.Done).doc
        assertFalse(section(cleared).membersAvailable)
        assertEquals(ctx.now, cleared.project.updatedAt)
    }
}
