package io.github.lenticualrstone.civilight.feature.world.place

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.AffiliationActions
import io.github.lenticualrstone.civilight.domain.world.item.BasisRefusal
import io.github.lenticualrstone.civilight.domain.world.item.BasisSlot
import io.github.lenticualrstone.civilight.domain.world.item.OrderList
import io.github.lenticualrstone.civilight.feature.world.faction.FactionCardState
import io.github.lenticualrstone.civilight.feature.world.faction.FactionCardUi
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
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Place card `소속`, faction `소속 인물 · 거점`, `묶음 기준` and the reorder wiring (board `ImmPlaceRules`). */
class AffiliationUiTest {
    private val f = Fixture

    private fun place(d: ProjectDoc = f.doc(), include: Boolean = true, readOnly: Boolean = false, open: Boolean = false) =
        AffiliationState.place(d, f.PA, LensPoint.Latest, include, open, readOnly)!!

    @Test
    fun `section rows - factions with 거점 and count, people with one-liner and 2곳, 무소속 outside`() {
        val ui = place()
        assertEquals("소속 세력 및 인물", ui.title)
        assertEquals("세력 2 · 인물 5", ui.header)
        assertTrue(ui.includeSubPlaces)
        assertEquals("시점 최신 · 1-7 · 소속 기준 ‘소속’", ui.basisLine)
        assertNull(ui.pickBasis)
        assertEquals(listOf("등불 결사", "상층 경비대"), ui.factions.map { it.name })
        assertEquals(listOf("거점 오리지아", "거점 하층 거리"), ui.factions.map { it.base })
        assertEquals(listOf("인물 2", "인물 2"), ui.factions.map { it.count })
        val lantern = ui.factions[0]
        assertTrue(lantern.expandable)
        assertEquals(listOf("리엔", "세라"), lantern.members.map { it.name })
        assertEquals("서고 관리인", lantern.members[0].role)
        assertEquals("2곳", lantern.members[0].multi)
        assertNull(lantern.members[1].multi)
        assertEquals("무소속 · 2", ui.looseTitle)
        assertEquals(listOf("국적 오리지아 · 거주 골목", "국적 오리지아"), ui.loose.map { it.line })
        assertEquals("숨긴 인물 1", ui.lockLine)
        assertTrue(ui.canReorder)
        assertFalse("read-only has no 순서 바꾸기", place(readOnly = true).canReorder)
    }

    @Test
    fun `하위 지역 포함 off keeps this place only`() {
        val ui = place(include = false)
        assertFalse(ui.includeSubPlaces)
        assertEquals(listOf("등불 결사"), ui.factions.map { it.name })
        assertTrue(ui.factions[0].members.all { it.multi == null })
    }

    @Test
    fun `without a 소속 basis - factions by 거점 only, no count, 소속 기준 고르기 line, no 무소속 without 거주 and 국적`() {
        val ui = place(f.withBasis(f.doc()) { AffiliationBasis() })
        assertEquals("소속 기준 고르기", ui.pickBasis)
        assertNull(ui.basisLine)
        assertEquals(2, ui.factions.size)
        assertTrue(ui.factions.all { it.count == null && !it.expandable })
        assertNull(ui.looseTitle)
        assertTrue(ui.loose.isEmpty())
    }

    @Test
    fun `rule 20 - trashed person and trashed base place stay dotted`() {
        var d = f.character(f.doc(), f.C2) { it.copy(deletedAt = ExampleClock.NOW) }
        d = f.placeEdit(d, f.PB) { it.copy(deletedAt = ExampleClock.NOW) }
        val ui = place(d)
        val sera = ui.factions[0].members.single { it.key == f.C2 }
        assertTrue(sera.deleted)
        val guard = ui.factions.single { it.key == f.F2 }
        assertTrue(guard.baseDeleted)
    }

    @Test
    fun `rule 19 - hidden people are counted in the lock line and open with the gate`() {
        assertEquals("숨긴 인물 1", place().lockLine)
        val open = place(open = true)
        assertNull(open.lockLine)
        assertTrue(open.factions[0].members.any { it.key == f.C8 })
    }

    @Test
    fun `subtitle and 하위 지역 chips come from the structure, hidden children are only counted`() {
        assertEquals("상위 지역 오리지아 · 하위 지역 2", PlaceStructure.subtitle("오리지아", 2))
        assertEquals("하위 지역 1", PlaceStructure.subtitle(null, 1))
        assertEquals("상위 지역 X", PlaceStructure.subtitle("X", 0))
        assertNull(PlaceStructure.subtitle(null, 0))
        val d = f.placeEdit(f.doc(), f.PE) { it.copy(hidden = true) }
        assertEquals(listOf("하층 거리"), PlaceStructure.subPlaces(d, f.PA, hiddenOpen = false).map { it.name })
        assertEquals(1, PlaceStructure.hiddenSubPlaces(d, f.PA, hiddenOpen = false))
        assertEquals(listOf("하층 거리", "비밀 구역"), PlaceStructure.subPlaces(d, f.PA, hiddenOpen = true).map { it.name })
        assertEquals(0, PlaceStructure.hiddenSubPlaces(d, f.PA, hiddenOpen = true))
        val card = PlaceCardState.build(d, "P", f.PA, hiddenOpen = false, readOnly = false)
        assertEquals("하위 지역 1", card.subtitle)
        assertEquals(PlaceCopy.TABS, card.tabs)
    }

    @Test
    fun `reorder wiring - each list moves within itself on the shown section, unchanged is the same document`() {
        val ctx = ActionContext(now = ExampleClock.NOW, device = "t")
        val d = f.doc()
        val moved = AffiliationMoves.move(d, f.PA, OrderList.Factions(f.PA), f.F2, 0, includeSubPlaces = true, hiddenOpen = false) { doc, s, k, to ->
            AffiliationActions.moveFaction(doc, s, k, to, ctx)
        }
        assertEquals(listOf("상층 경비대", "등불 결사"), place(moved).factions.map { it.name })
        val members = AffiliationMoves.move(d, f.PA, OrderList.Members(f.F1), f.C2, 0, true, false) { doc, s, k, to ->
            AffiliationActions.moveMember(doc, s, f.F1, k, to, ctx)
        }
        assertEquals(listOf("세라", "리엔"), place(members).factions[0].members.map { it.name })
        // The person order is the faction's: the faction card follows.
        assertEquals(listOf("세라", "리엔"), AffiliationState.faction(members, f.F1, LensPoint.Latest, false, false)!!.members.map { it.name })
        val loose = AffiliationMoves.move(d, f.PA, OrderList.Loose(f.PA), f.C5, 0, true, false) { doc, s, k, to ->
            AffiliationActions.moveLoose(doc, s, k, to, ctx)
        }
        assertEquals(listOf("마르타", "노아"), place(loose).loose.map { it.name })
        val same = AffiliationMoves.move(d, f.PA, OrderList.Factions(f.PA), f.F1, 0, true, false) { doc, s, k, to ->
            AffiliationActions.moveFaction(doc, s, k, to, ctx)
        }
        assertSame(d, same)
        assertEquals("세력 순서 바꿈", AffiliationActions.undoLabel(OrderList.Factions(f.PA)))
    }

    @Test
    fun `faction card - 거점 subtitle and row, 소속 인물 with the same rows, tabs 개요 · 소속`() {
        val ui = FactionCardState.build(f.doc(), "P", f.F2, hiddenOpen = false)
        assertEquals("거점 하층 거리", ui.subtitle)
        assertEquals("거점", ui.base!!.label)
        assertEquals("하층 거리", ui.base!!.value)
        assertEquals(f.PB, ui.baseKey)
        assertEquals(FactionCardUi.TABS, ui.tabs)
        assertEquals(listOf("리엔", "카일"), ui.members!!.members.map { it.name })
        assertEquals("2곳", ui.members!!.members[0].multi)
        assertTrue(ui.members!!.canReorder)
        val noBase = FactionCardState.build(f.factionEdit(f.doc(), f.F2) { it.copy(baseKey = null) }, "P", f.F2, hiddenOpen = false)
        assertNull(noBase.subtitle)
        assertEquals("—", noBase.base!!.value)
        val noBasis = FactionCardState.build(f.withBasis(f.doc()) { AffiliationBasis() }, "P", f.F2, hiddenOpen = false)
        assertEquals("소속 기준 고르기", noBasis.members!!.pickBasis)
    }

    @Test
    fun `묶음 기준 - rows show the field or 고르기, a stored field no longer usable shows one line, choices per slot`() {
        val d = f.doc()
        assertEquals(
            listOf("소속" to "소속", "거주" to "거주", "국적" to "국적"),
            BasisSettingsState.rows(d).map { it.title to it.value },
        )
        assertTrue(BasisSettingsState.rows(d).all { it.picked && it.note == null })
        assertEquals("소속 소속 · 거주 거주 · 국적 국적", BasisSettingsState.summary(d))

        val cleared = f.withBasis(d) { it.copy(residenceFieldKey = null) }
        val res = BasisSettingsState.rows(cleared).single { it.slot == BasisSlot.RESIDENCE }
        assertEquals("고르기", res.value)
        assertFalse(res.picked)

        val trashed = d.copy(project = d.project.copy(fieldDefs = d.project.fieldDefs.map { if (it.key == f.D_NAT) it.copy(deletedAt = ExampleClock.NOW) else it }))
        val natRow = BasisSettingsState.rows(trashed).single { it.slot == BasisSlot.NATIONALITY }
        assertEquals("고르기", natRow.value)
        assertEquals("휴지통", natRow.note)

        val aff = BasisSettingsState.choices(d, BasisSlot.AFFILIATION)
        assertTrue(aff.any { it.key == f.D_AFF && it.selected })
        assertTrue("소속 takes 세력 links only", aff.none { it.key == f.D_RES || it.key == f.D_NAT })
        val nat = BasisSettingsState.choices(d, BasisSlot.NATIONALITY)
        assertTrue(nat.single { it.key == f.D_NAT }.selected)
        assertFalse(nat.single { it.key == f.D_RES }.selected)
        assertEquals("장소 연결 아님", PlaceCopy.refusal(BasisRefusal.WRONG_TARGET, BasisSlot.RESIDENCE))
        assertEquals("세력 연결 항목 없음", PlaceCopy.noChoices(BasisSlot.AFFILIATION))
    }

    @Test
    fun `place copy passes the lint`() {
        val texts = listOf(
            PlaceCopy.LOOSE_NOTE, PlaceCopy.GRIP, PlaceCopy.BASIS_TITLE, PlaceCopy.BASIS_PICK, PlaceCopy.BASIS_NOTE,
            PlaceCopy.FIELD_SETTINGS, PlaceCopy.PICK_IMAGE, PlaceCopy.SEC_TIME_HERE, PlaceCopy.SEC_MAPS,
            PlaceCopy.subPlacesTitle(3), PlaceCopy.fieldsTitle(3), PlaceCopy.subPlaces(3), PlaceCopy.parent("오리지아"),
            PlaceCopy.base("하층 거리"), PlaceCopy.looseTitle(2), PlaceCopy.expandLabel("등불 결사", true),
            PlaceCopy.expandLabel("등불 결사", false), PlaceCopy.noChoices(BasisSlot.RESIDENCE),
        ) + PlaceCopy.TABS + BasisRefusal.entries.map { PlaceCopy.refusal(it, BasisSlot.AFFILIATION) }
        val failures = texts.mapNotNull { t -> CopyLint.lint(t).takeIf { it.isNotEmpty() }?.let { "$t: $it" } } +
            listOf(PlaceCopy.BTN_CLEAR).mapNotNull { t -> CopyLint.lint(t, isButton = true).takeIf { it.isNotEmpty() }?.let { "$t: $it" } }
        assertEquals(emptyList<String>(), failures)
    }

    /** Places 오리지아 › 하층 거리 › 골목, 오리지아 › 비밀 구역; factions based there; basis 소속 · 거주 · 국적. */
    private object Fixture {
        private fun k(tag: String, n: Int): Key = ExampleKeys.TIME + tag + n.toString().padStart(12, '0')

        val PA = k("UPLC", 1)
        val PB = k("UPLC", 2)
        val PC = k("UPLC", 3)
        val PE = k("UPLC", 5)
        val F1 = k("UFAC", 1)
        val F2 = k("UFAC", 2)
        val D_AFF = k("UFDF", 1)
        val D_RES = k("UFDF", 2)
        val D_NAT = k("UFDF", 3)
        val C1 = k("UCHR", 1)
        val C2 = k("UCHR", 2)
        val C3 = k("UCHR", 3)
        val C4 = k("UCHR", 4)
        val C5 = k("UCHR", 5)
        val C8 = k("UCHR", 8)

        private fun link(vararg keys: Key) = FieldValue.of(FieldPayload.Link(keys.toList()))

        fun doc(): ProjectDoc {
            val base = ExampleProject.build()
            val o = OrderKeys.evenlySpaced(9)
            val defOrder = OrderKeys.after(base.project.fieldDefs.maxOf { it.order })
            fun def(key: Key, name: String, target: FieldLinkTargetKind, n: Int) = FieldDef(
                key = key, entityKind = EntityKind.CHARACTER.open(), name = name, type = FieldType.LINK.open(),
                order = defOrder + n.toString(), linkTargetKind = target.open(),
            )
            val defs = listOf(
                def(D_AFF, "소속", FieldLinkTargetKind.FACTION, 1),
                def(D_RES, "거주", FieldLinkTargetKind.PLACE, 2),
                def(D_NAT, "국적", FieldLinkTargetKind.PLACE, 3),
            )
            val places = listOf(
                Place(key = PA, order = o[0], name = "오리지아"),
                Place(key = PB, order = o[1], name = "하층 거리", parentPlaceKey = PA),
                Place(key = PC, order = o[2], name = "골목", parentPlaceKey = PB),
                Place(key = PE, order = o[3], name = "비밀 구역", parentPlaceKey = PA),
            )
            val factions = listOf(
                Faction(key = F1, order = o[0], name = "등불 결사", baseKey = PA),
                Faction(key = F2, order = o[1], name = "상층 경비대", baseKey = PB),
            )
            fun c(n: Int, key: Key, name: String, fields: Map<Key, FieldValue>, hidden: Boolean = false, oneLiner: String? = null) =
                Character(key = key, recordNo = 200 + n, name = name, order = o[n - 1], fields = fields, hidden = hidden, oneLiner = oneLiner)
            val people = listOf(
                c(1, C1, "리엔", mapOf(D_AFF to link(F1, F2)), oneLiner = "서고 관리인"),
                c(2, C2, "세라", mapOf(D_AFF to link(F1))),
                c(3, C3, "카일", mapOf(D_AFF to link(F2), D_RES to link(PB))),
                c(4, C4, "노아", mapOf(D_RES to link(PC), D_NAT to link(PA))),
                c(5, C5, "마르타", mapOf(D_NAT to link(PA))),
                c(8, C8, "숨은이", mapOf(D_AFF to link(F1)), hidden = true),
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

        fun placeEdit(doc: ProjectDoc, key: Key, f: (Place) -> Place) =
            doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == key) f(it) else it }))

        fun factionEdit(doc: ProjectDoc, key: Key, f: (Faction) -> Faction) =
            doc.copy(world = doc.world.copy(factions = doc.world.factions.map { if (it.key == key) f(it) else it }))

        fun character(doc: ProjectDoc, key: Key, f: (Character) -> Character) =
            doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == key) f(it) else it }))
    }
}
