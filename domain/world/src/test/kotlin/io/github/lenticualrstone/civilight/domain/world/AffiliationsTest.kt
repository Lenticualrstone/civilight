package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationOrder
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationSection
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationSettings
import io.github.lenticualrstone.civilight.domain.world.item.Affiliations
import io.github.lenticualrstone.civilight.domain.world.item.BaseRefusal
import io.github.lenticualrstone.civilight.domain.world.item.BaseResult
import io.github.lenticualrstone.civilight.domain.world.item.BasisRefusal
import io.github.lenticualrstone.civilight.domain.world.item.BasisResult
import io.github.lenticualrstone.civilight.domain.world.item.BasisSlot
import io.github.lenticualrstone.civilight.domain.world.item.OrderList
import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.AffiliationBasis
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Place card `소속 세력 및 인물` and faction card `소속 인물 · 거점` (board `ImmPlaceRules`). */
class AffiliationsTest {
    private val f = AffiliationFixture

    private fun section(
        d: ProjectDoc = f.doc(),
        lens: LensPoint = LensPoint.Latest,
        include: Boolean = true,
        unlocked: Boolean = false,
        place: Key = f.PA,
    ): AffiliationSection = Affiliations(d, unlocked).section(place, lens, include)!!

    private fun AffiliationSection.groups() = factions.map { g -> g.factionKey to g.members.map { it.characterKey } }

    @Test
    fun `factions by base with sub-places, multi-faction people counted once`() {
        val s = section()
        assertEquals("소속 세력 및 인물", s.title)
        assertEquals(listOf(f.F1 to listOf(f.C1, f.C2), f.F2 to listOf(f.C1, f.C3)), s.groups())
        assertEquals(listOf(2, 1), s.factions[0].members.map { it.multi })
        assertEquals("2곳", s.factions[1].members[0].multiLabel)
        assertNull(s.factions[0].members[1].multiLabel)
        assertEquals("인물 2", s.factions[0].countLabel)
        assertEquals("하층 거리", s.factions[1].base?.name)
        assertEquals(listOf(f.C4, f.C5, f.C7), s.loose.map { it.characterKey })
        assertEquals("세력 2 · 인물 6", s.header)
        assertEquals("숨긴 세력 1 · 숨긴 인물 1", s.lockLine)
        assertTrue(s.membersAvailable && s.looseAvailable)
        assertNull(s.pickBasisLine)
        assertEquals("시점 최신 · 1-7 · 소속 기준 ‘소속’", s.basisLine)
    }

    @Test
    fun `sub-places off keeps this place only`() {
        val s = section(include = false)
        assertEquals(listOf(f.F1 to listOf(f.C1, f.C2)), s.groups())
        assertEquals(1, s.factions[0].members[0].multi)
        // 노아 lives in 골목 (under 하층 거리): elsewhere now, so out even though his nationality is here.
        assertEquals(listOf(f.C5, f.C7), s.loose.map { it.characterKey })
        assertEquals("세력 1 · 인물 4", s.header)
    }

    @Test
    fun `loose rules - residence here, nationality only, residence elsewhere excluded`() {
        val s = section()
        val byKey = s.loose.associateBy { it.characterKey }
        assertEquals("국적 오리지아 · 거주 골목", byKey.getValue(f.C4).line)
        assertEquals("국적 오리지아", byKey.getValue(f.C5).line)
        // 하나 is in 바깥 세력 (based elsewhere): not a faction here, so she is 무소속 here by residence.
        assertEquals("거주 오리지아", byKey.getValue(f.C7).line)
        assertFalse(f.C6 in byKey)
        // 유리 shows on the card of the place she lives in.
        assertEquals(listOf(f.C6), section(place = f.PD).loose.map { it.characterKey })
    }

    @Test
    fun `missing basis lists factions by base only and never hides the section`() {
        val noAff = f.withBasis(f.doc()) { it.copy(affiliationFieldKey = null) }
        val s = section(noAff)
        assertFalse(s.membersAvailable)
        assertEquals("소속 기준 고르기", s.pickBasisLine)
        assertEquals(listOf(f.F1 to emptyList<Key>(), f.F2 to emptyList()), s.groups())
        assertNull(s.basisLine)

        val none = f.withBasis(f.doc()) { AffiliationBasis() }
        val empty = section(none)
        assertFalse(empty.looseAvailable)
        assertTrue(empty.loose.isEmpty())
        assertEquals(2, empty.factions.size)
        assertEquals("세력 2 · 인물 0", empty.header)

        // A stored key that is no longer a usable field reads as unset.
        val wrong = f.withBasis(f.doc()) { it.copy(affiliationFieldKey = f.D_TEXT) }
        assertFalse(section(wrong).membersAvailable)
    }

    @Test
    fun `lens change moves a person between groups and out of loose`() {
        var d = f.doc()
        d = f.record(d, 1, f.C3, f.D_AFF, f.link(f.F1), ExampleKeys.EPISODE_1_5)
        d = f.record(d, 2, f.C5, f.D_RES, f.link(f.PD), ExampleKeys.EPISODE_1_5)
        val before = section(d, LensPoint.UnitEnd(ExampleKeys.EPISODE_1_4))
        assertEquals(listOf(f.F1 to listOf(f.C1, f.C2), f.F2 to listOf(f.C1, f.C3)), before.groups())
        assertTrue(before.loose.any { it.characterKey == f.C5 })
        val after = section(d, LensPoint.UnitEnd(ExampleKeys.EPISODE_1_6))
        assertEquals(listOf(f.F1 to listOf(f.C1, f.C2, f.C3), f.F2 to listOf(f.C1)), after.groups())
        assertFalse(after.loose.any { it.characterKey == f.C5 })
        assertTrue(after.basisLine!!.startsWith("시점 1-6"))
    }

    @Test
    fun `hidden factions and people are masked while locked and excluded from counts`() {
        val unlocked = section(unlocked = true)
        assertEquals(
            listOf(f.F1 to listOf(f.C1, f.C2, f.C8), f.F2 to listOf(f.C1, f.C3), f.F4 to listOf(f.C9)),
            unlocked.groups(),
        )
        assertEquals(2, unlocked.factions[0].members[0].multi)
        assertNull(unlocked.lockLine)
        assertEquals("세력 3 · 인물 8", unlocked.header)
        // Locked: 비밀's only faction is hidden, so he is neither listed nor 무소속.
        val locked = section()
        assertFalse(locked.loose.any { it.characterKey == f.C9 })
        assertFalse(locked.factions.any { g -> g.members.any { it.characterKey == f.C8 } })
    }

    @Test
    fun `secret affiliation field of a locked tab is not read`() {
        var d = f.doc()
        d = d.copy(project = d.project.copy(fieldDefs = d.project.fieldDefs.map { if (it.key == f.D_AFF) it.copy(isSecret = true) else it }))
        d = f.character(d, f.C2) { it.copy(secretTabLocked = true) }
        assertEquals(listOf(f.C1), section(d).factions[0].members.map { it.characterKey })
        // 세라 has no readable value pointing here (no 거주, no 국적): neither 무소속 nor counted.
        assertFalse(section(d).loose.any { it.characterKey == f.C2 })
        assertEquals("숨긴 세력 1 · 숨긴 인물 1", section(d).lockLine)
        // 하나 (소속 locked, lives here): her faction cannot be read, so she is not 무소속 but counted.
        d = f.character(d, f.C7) { it.copy(secretTabLocked = true) }
        val s = section(d)
        assertFalse(s.loose.any { it.characterKey == f.C7 })
        assertEquals("숨긴 세력 1 · 숨긴 인물 2", s.lockLine)
        assertEquals("세력 2 · 인물 4", s.header)
    }

    @Test
    fun `secret residence field of a locked tab gives no nationality fallback`() {
        var d = f.doc()
        d = d.copy(project = d.project.copy(fieldDefs = d.project.fieldDefs.map { if (it.key == f.D_RES) it.copy(isSecret = true) else it }))
        // 노아 lives in 골목 (unreadable now), 유리 lives in 다른 나라 (unreadable now); both have 국적 오리지아.
        d = f.character(d, f.C4) { it.copy(secretTabLocked = true) }
        d = f.character(d, f.C6) { it.copy(secretTabLocked = true) }
        val s = section(d)
        assertEquals(listOf(f.C5, f.C7), s.loose.map { it.characterKey })
        assertEquals("숨긴 세력 1 · 숨긴 인물 3", s.lockLine)
        // 다른 나라: 유리's residence cannot be read and her nationality is elsewhere, so nothing here.
        val pd = section(d, place = f.PD)
        assertTrue(pd.loose.isEmpty())
        assertNull(pd.lockLine)
        // A trashed person with an unreadable field is neither listed nor counted.
        val trashed = f.character(d, f.C4) { it.copy(deletedAt = 1L) }
        assertEquals("숨긴 세력 1 · 숨긴 인물 2", section(trashed).lockLine)
    }

    @Test
    fun `a hidden faction in the trash stays masked while locked`() {
        val d = f.faction(f.doc(), f.F4) { it.copy(deletedAt = 1L) } // still hidden
        val locked = section(d)
        assertFalse(locked.factions.any { it.factionKey == f.F4 })
        assertFalse(locked.factions.any { it.name == "숨은 결사" })
        // 비밀 is masked with it: not listed, not 무소속, not counted; the trashed faction is not counted either.
        assertFalse(locked.loose.any { it.characterKey == f.C9 })
        assertFalse(locked.factions.any { g -> g.members.any { it.characterKey == f.C9 } })
        assertEquals("숨긴 인물 1", locked.lockLine)
        val open = section(d, unlocked = true).factions.single { it.factionKey == f.F4 }
        assertTrue(open.deleted)
        assertEquals(listOf(f.C9), open.members.map { it.characterKey })
    }

    @Test
    fun `a person merged into another is no dotted ghost`() {
        fun mergedAway(doc: ProjectDoc, key: Key, entryKey: Key): ProjectDoc {
            val entry = TrashEntry(
                key = entryKey, kind = TrashKind.CHARACTER.open(), projectKey = doc.key, targetKeys = listOf(key),
                reason = TrashReason.MERGED.open(), mergedIntoKey = f.C1, deletedAt = 1L, purgeOn = TrashEntry.purgeDateFor(LocalDate.of(2026, 1, 1)),
            )
            val d = f.character(doc, key) { it.copy(deletedAt = 1L, trashEntryKey = entryKey) }
            return d.copy(trash = d.trash.copy(entries = d.trash.entries + entry))
        }
        var d = mergedAway(f.doc(), f.C2, "merge-entry-2")
        d = mergedAway(d, f.C5, "merge-entry-5")
        val s = section(d)
        assertEquals(listOf(f.C1), s.factions[0].members.map { it.characterKey })
        assertFalse(s.loose.any { it.characterKey == f.C5 })
        assertEquals(listOf(f.C1), Affiliations(d).factionMembers(f.F1)!!.members.map { it.characterKey })
        // A plainly deleted person still shows dotted.
        val deleted = f.character(f.doc(), f.C2) { it.copy(deletedAt = 1L) }
        assertTrue(section(deleted).factions[0].members.single { it.characterKey == f.C2 }.deleted)
    }

    @Test
    fun `default order, stored order, stale keys`() {
        var d = f.doc()
        d = f.place(d, f.PA) { it.copy(factionOrder = listOf("stale", f.F2), looseOrder = listOf(f.C7, "gone")) }
        d = f.faction(d, f.F1) { it.copy(memberOrder = listOf(f.C2, "stale")) }
        val s = section(d)
        assertEquals(listOf(f.F2 to listOf(f.C1, f.C3), f.F1 to listOf(f.C2, f.C1)), s.groups())
        assertEquals(listOf(f.C7, f.C4, f.C5), s.loose.map { it.characterKey })
        // The member order is the faction's, so the faction card follows it too.
        assertEquals(listOf(f.C2, f.C1), WorldCards(d).factionCard(f.F1)!!.affiliation!!.members.map { it.characterKey })
    }

    @Test
    fun `reorder ops are one pure step each and undo restores the order`() {
        val ctx = ctx()
        val before = f.doc()
        val s = section(before)
        val moved = AffiliationOrder.moveFaction(before, s, f.F2, 0, ctx)
        assertNotSame(before, moved)
        assertEquals(listOf(f.F2, f.F1), section(moved).factions.map { it.factionKey })
        assertEquals(listOf(f.F2, f.F1), moved.index.place(f.PA)!!.factionOrder)
        // Undo = the document before the step.
        assertEquals(listOf(f.F1, f.F2), section(before).factions.map { it.factionKey })

        val loose = AffiliationOrder.moveLoose(before, s, f.C7, 0, ctx)
        assertEquals(listOf(f.C7, f.C4, f.C5), section(loose).loose.map { it.characterKey })

        val members = AffiliationOrder.moveMember(before, s, f.F1, f.C2, 0, ctx)
        assertEquals(listOf(f.C2, f.C1), section(members).factions[0].members.map { it.characterKey })
        // The same order on the faction card and on another place's card (unlocked view: the hidden one follows).
        assertEquals(listOf(f.C2, f.C1, f.C8), section(members, unlocked = true).factions[0].members.map { it.characterKey })

        // No-op moves return the same document (no empty undo step).
        assertSame(before, AffiliationOrder.moveFaction(before, s, f.F1, 0, ctx))
        assertSame(before, AffiliationOrder.moveFaction(before, s, "not shown", 0, ctx))
        val shown = s.factions.map { it.factionKey }
        assertSame(before, AffiliationOrder.moveUp(before, OrderList.Factions(f.PA), shown, f.F1, ctx))
        assertSame(before, AffiliationOrder.moveDown(before, OrderList.Factions(f.PA), shown, f.F2, ctx))
        val down = AffiliationOrder.moveDown(before, OrderList.Factions(f.PA), shown, f.F1, ctx)
        assertEquals(listOf(f.F2, f.F1), section(down).factions.map { it.factionKey })
        val up = AffiliationOrder.moveUp(down, OrderList.Factions(f.PA), listOf(f.F2, f.F1), f.F1, ctx)
        assertEquals(listOf(f.F1, f.F2), section(up).factions.map { it.factionKey })
    }

    @Test
    fun `a move rewrites only shown slots, keys not shown keep their place`() {
        val ctx = ctx()
        val d = f.faction(f.doc(), f.F1) { it.copy(memberOrder = listOf(f.C1, f.C8, f.C2)) }
        val s = section(d) // locked: shows 리엔, 세라
        assertEquals(listOf(f.C1, f.C2), s.factions[0].members.map { it.characterKey })
        val moved = AffiliationOrder.moveMember(d, s, f.F1, f.C2, 0, ctx)
        assertEquals(listOf(f.C2, f.C8, f.C1), moved.index.faction(f.F1)!!.memberOrder)
        // Already last (index clamped): nothing moves.
        assertNull(AffiliationOrder.reorder(listOf("a", "x"), listOf("a", "b"), "b", 5))
        // Stored a, x (x not shown) + unlisted b: only the shown slots change.
        assertEquals(listOf("b", "x", "a"), AffiliationOrder.reorder(listOf("a", "x"), listOf("a", "b"), "b", 0))
    }

    @Test
    fun `deleted faction stays dotted while people link to it, trashed base place stays dotted`() {
        var d = f.faction(f.doc(), f.F2) { it.copy(deletedAt = 1L) }
        d = f.place(d, f.PB) { it.copy(deletedAt = 1L) }
        val s = section(d)
        val g = s.factions.single { it.factionKey == f.F2 }
        assertTrue(g.deleted)
        assertEquals(listOf(f.C1, f.C3), g.members.map { it.characterKey })
        assertTrue(g.base!!.deleted)
        assertEquals("하층 거리", g.base!!.name)
        assertEquals(1, s.factionCount)
        // 노아 still lives under the trashed 하층 거리 (reference kept); 카일 (lives there too) stays under the dotted group.
        assertTrue(s.loose.any { it.characterKey == f.C4 })
        assertFalse(s.loose.any { it.characterKey == f.C3 })

        // Trashed character: shown dotted, not counted.
        val noSera = f.character(f.doc(), f.C2) { it.copy(deletedAt = 1L) }
        val sera = section(noSera).factions[0].members
        assertEquals(listOf(f.C1 to false, f.C2 to true), sera.map { it.characterKey to it.deleted })
        assertEquals("세력 2 · 인물 5", section(noSera).header)
        val noNoah = f.character(f.doc(), f.C4) { it.copy(deletedAt = 1L) }
        assertTrue(section(noNoah).loose.single { it.characterKey == f.C4 }.deleted)

        // Trashed faction nobody links to any more: not shown.
        val empty = f.faction(f.doc(), f.F4) { it.copy(deletedAt = 1L, hidden = false) }
        assertTrue(section(empty).factions.single { it.factionKey == f.F4 }.deleted)
        val unlinked = f.character(empty, f.C9) { it.copy(fields = emptyMap()) }
        assertFalse(section(unlinked).factions.any { it.factionKey == f.F4 })

        // A purged base place keeps its name tag.
        val purged = f.doc().let { doc ->
            doc.copy(
                world = doc.world.copy(places = doc.world.places.filter { it.key != f.PB }),
                tombstones = doc.tombstones.copy(
                    nameTags = doc.tombstones.nameTags + io.github.lenticualrstone.civilight.model.trash.NameTag(
                        key = f.PB, kind = io.github.lenticualrstone.civilight.model.trash.NameTagKind.PLACE.open(), name = "하층 거리",
                        state = io.github.lenticualrstone.civilight.model.trash.NameTagState.PURGED.open(),
                    ),
                ),
            )
        }
        val base = Affiliations(purged).factionMembers(f.F2)!!.base!!
        assertEquals("하층 거리" to true, base.name to base.deleted)
    }

    @Test
    fun `faction card shows base and members`() {
        val card = WorldCards(f.doc()).factionCard(f.F1)!!
        val a = card.affiliation!!
        assertEquals("오리지아", a.base?.name)
        assertEquals(listOf(f.C1, f.C2), a.members.map { it.characterKey })
        assertEquals(listOf(2, 1), a.members.map { it.multi })
        assertEquals("숨긴 인물 1", a.lockLine)
        val noBasis = WorldCards(f.withBasis(f.doc()) { AffiliationBasis() }).factionCard(f.F1)!!.affiliation!!
        assertEquals("소속 기준 고르기", noBasis.pickBasisLine)
    }

    @Test
    fun `basis and base settings refuse wrong fields and places`() {
        val ctx = ctx()
        val d = f.withBasis(f.doc()) { AffiliationBasis() }
        fun refused(slot: BasisSlot, key: Key) = (AffiliationSettings.setBasis(d, slot, key, ctx) as BasisResult.Refused).reason
        assertEquals(BasisRefusal.NOT_LINK, refused(BasisSlot.AFFILIATION, f.D_TEXT))
        assertEquals(BasisRefusal.WRONG_TARGET, refused(BasisSlot.AFFILIATION, f.D_RES))
        assertEquals(BasisRefusal.WRONG_TARGET, refused(BasisSlot.RESIDENCE, f.D_AFF))
        assertEquals(BasisRefusal.NOT_CHARACTER, refused(BasisSlot.NATIONALITY, f.D_PLACE_ON_PLACE))
        assertEquals(BasisRefusal.NO_FIELD, refused(BasisSlot.NATIONALITY, "missing"))
        val trashed = d.copy(project = d.project.copy(fieldDefs = d.project.fieldDefs.map { if (it.key == f.D_NAT) it.copy(deletedAt = 1L) else it }))
        assertEquals(BasisRefusal.TRASHED, (AffiliationSettings.setBasis(trashed, BasisSlot.NATIONALITY, f.D_NAT, ctx) as BasisResult.Refused).reason)
        val oneOnly = d.copy(project = d.project.copy(fieldDefs = d.project.fieldDefs.map { if (it.key == f.D_NAT) it.copy(scopeCharacterKey = f.C1) else it }))
        assertEquals(BasisRefusal.ONE_CHARACTER, (AffiliationSettings.setBasis(oneOnly, BasisSlot.NATIONALITY, f.D_NAT, ctx) as BasisResult.Refused).reason)

        val set = (AffiliationSettings.setBasis(d, BasisSlot.AFFILIATION, f.D_AFF, ctx) as BasisResult.Done).doc
        assertEquals(f.D_AFF, set.project.cast.affiliationBasis.affiliationFieldKey)
        assertEquals(listOf(f.D_AFF), AffiliationSettings.choices(set, BasisSlot.AFFILIATION).map { it.key })
        assertSame(set, (AffiliationSettings.setBasis(set, BasisSlot.AFFILIATION, f.D_AFF, ctx) as BasisResult.Done).doc)
        val cleared = (AffiliationSettings.setBasis(set, BasisSlot.AFFILIATION, null, ctx) as BasisResult.Done).doc
        assertNull(cleared.project.cast.affiliationBasis.affiliationFieldKey)

        val base = (AffiliationSettings.setBase(f.doc(), f.F3, f.PA, ctx) as BaseResult.Done).doc
        assertEquals(f.PA, base.index.faction(f.F3)!!.baseKey)
        assertTrue(section(base).factions.any { it.factionKey == f.F3 })
        val trashedPlace = f.place(f.doc(), f.PD) { it.copy(deletedAt = 1L) }
        assertEquals(BaseRefusal.PLACE_TRASHED, (AffiliationSettings.setBase(trashedPlace, f.F1, f.PD, ctx) as BaseResult.Refused).reason)
        assertEquals(BaseRefusal.NO_PLACE, (AffiliationSettings.setBase(f.doc(), f.F1, "missing", ctx) as BaseResult.Refused).reason)
        assertEquals(BaseRefusal.NO_FACTION, (AffiliationSettings.setBase(f.doc(), "missing", f.PA, ctx) as BaseResult.Refused).reason)
        val clearedBase = (AffiliationSettings.setBase(f.doc(), f.F1, null, ctx) as BaseResult.Done).doc
        assertNull(clearedBase.index.faction(f.F1)!!.baseKey)
    }

    @Test
    fun `sub-place walk survives a parent cycle`() {
        val d = f.place(f.doc(), f.PA) { it.copy(parentPlaceKey = f.PC) }
        assertEquals(setOf(f.PB, f.PC), Affiliations(d).subPlaces(f.PA))
    }
}

/** Places 오리지아 › 하층 거리 › 골목 and 다른 나라; factions, three basis fields and nine people on the example project. */
object AffiliationFixture {
    private fun k(tag: String, n: Int): Key = ExampleKeys.TIME + tag + n.toString().padStart(12, '0')

    val PA = k("APLC", 1)
    val PB = k("APLC", 2)
    val PC = k("APLC", 3)
    val PD = k("APLC", 4)
    val F1 = k("AFAC", 1)
    val F2 = k("AFAC", 2)
    val F3 = k("AFAC", 3)
    val F4 = k("AFAC", 4)
    val D_AFF = k("AFDF", 1)
    val D_RES = k("AFDF", 2)
    val D_NAT = k("AFDF", 3)
    val D_TEXT = k("AFDF", 4)
    val D_PLACE_ON_PLACE = k("AFDF", 5)
    val C1 = k("ACHR", 1)
    val C2 = k("ACHR", 2)
    val C3 = k("ACHR", 3)
    val C4 = k("ACHR", 4)
    val C5 = k("ACHR", 5)
    val C6 = k("ACHR", 6)
    val C7 = k("ACHR", 7)
    val C8 = k("ACHR", 8)
    val C9 = k("ACHR", 9)

    fun link(vararg keys: Key) = FieldValue.of(FieldPayload.Link(keys.toList()))

    fun doc(): ProjectDoc {
        val base = ExampleProject.build()
        val o = OrderKeys.evenlySpaced(9)
        val defOrder = OrderKeys.after(base.project.fieldDefs.maxOf { it.order })
        fun def(key: Key, name: String, type: FieldType, target: FieldLinkTargetKind?, kind: EntityKind = EntityKind.CHARACTER, n: Int) = FieldDef(
            key = key, entityKind = kind.open(), name = name, type = type.open(), order = defOrder + n.toString(),
            linkTargetKind = target?.open(),
        )
        val defs = listOf(
            def(D_AFF, "소속", FieldType.LINK, FieldLinkTargetKind.FACTION, n = 1),
            def(D_RES, "거주", FieldType.LINK, FieldLinkTargetKind.PLACE, n = 2),
            def(D_NAT, "국적", FieldType.LINK, FieldLinkTargetKind.PLACE, n = 3),
            def(D_TEXT, "메모", FieldType.SHORT_TEXT, null, n = 4),
            def(D_PLACE_ON_PLACE, "이웃", FieldType.LINK, FieldLinkTargetKind.PLACE, EntityKind.PLACE, n = 5),
        )
        val places = listOf(
            Place(key = PA, order = o[0], name = "오리지아"),
            Place(key = PB, order = o[1], name = "하층 거리", parentPlaceKey = PA),
            Place(key = PC, order = o[2], name = "골목", parentPlaceKey = PB),
            Place(key = PD, order = o[3], name = "다른 나라"),
        )
        val factions = listOf(
            Faction(key = F1, order = o[0], name = "등불 결사", baseKey = PA),
            Faction(key = F2, order = o[1], name = "상층 경비대", baseKey = PB),
            Faction(key = F3, order = o[2], name = "바깥 세력", baseKey = PD),
            Faction(key = F4, order = o[3], name = "숨은 결사", baseKey = PA, hidden = true),
        )
        fun c(n: Int, key: Key, name: String, fields: Map<Key, FieldValue>, hidden: Boolean = false) =
            Character(key = key, recordNo = 100 + n, name = name, order = o[n - 1], fields = fields, hidden = hidden)
        val people = listOf(
            c(1, C1, "리엔", mapOf(D_AFF to link(F1, F2))),
            c(2, C2, "세라", mapOf(D_AFF to link(F1))),
            c(3, C3, "카일", mapOf(D_AFF to link(F2), D_RES to link(PB))),
            c(4, C4, "노아", mapOf(D_RES to link(PC), D_NAT to link(PA))),
            c(5, C5, "마르타", mapOf(D_NAT to link(PA))),
            c(6, C6, "유리", mapOf(D_RES to link(PD), D_NAT to link(PA))),
            c(7, C7, "하나", mapOf(D_AFF to link(F3), D_RES to link(PA))),
            c(8, C8, "숨은이", mapOf(D_AFF to link(F1)), hidden = true),
            c(9, C9, "비밀", mapOf(D_AFF to link(F4), D_RES to link(PA))),
        )
        return base.copy(
            project = base.project.copy(
                fieldDefs = base.project.fieldDefs + defs,
                cast = base.project.cast.copy(affiliationBasis = AffiliationBasis(D_AFF, D_RES, D_NAT)),
            ),
            cast = base.cast.copy(characters = people),
            world = base.world.copy(places = places, factions = factions),
        )
    }

    fun withBasis(doc: ProjectDoc, f: (AffiliationBasis) -> AffiliationBasis): ProjectDoc =
        doc.copy(project = doc.project.copy(cast = doc.project.cast.copy(affiliationBasis = f(doc.project.cast.affiliationBasis))))

    fun place(doc: ProjectDoc, key: Key, f: (Place) -> Place) =
        doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == key) f(it) else it }))

    fun faction(doc: ProjectDoc, key: Key, f: (Faction) -> Faction) =
        doc.copy(world = doc.world.copy(factions = doc.world.factions.map { if (it.key == key) f(it) else it }))

    fun character(doc: ProjectDoc, key: Key, f: (Character) -> Character) =
        doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == key) f(it) else it }))

    /** A `◆` from the start of [unitKey]. */
    fun record(doc: ProjectDoc, n: Int, characterKey: Key, defKey: Key, value: FieldValue, unitKey: Key): ProjectDoc {
        val r = ChangeRecord(
            key = k("AREC", n),
            target = ChangeTarget.Field(EntityKind.CHARACTER.open(), characterKey, defKey),
            value = ChangeRecord.encodeValue(FieldValue.serializer(), value),
            anchor = Anchor.UnitStart(unitKey),
            createdAt = n.toLong(),
        )
        return doc.copy(changes = doc.changes.copy(records = doc.changes.records + r))
    }
}
