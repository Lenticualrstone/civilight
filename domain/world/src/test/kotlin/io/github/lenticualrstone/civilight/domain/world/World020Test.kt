package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.key
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateA
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateB
import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.NewWorldItem
import io.github.lenticualrstone.civilight.domain.world.item.WorldCreate
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapNames
import io.github.lenticualrstone.civilight.domain.world.term.CandidateKind
import io.github.lenticualrstone.civilight.domain.world.term.RevealKind
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.StaleRejections
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermCheckRow
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermReveals
import io.github.lenticualrstone.civilight.domain.world.term.isAuto
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.docText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD §4.9: the 0.2.0 cases of the world domain (WLD-T73 … WLD-T89). */
class World020Test {
    private val b12 = K.BLOCK_B12
    private val b42 = K.BLOCK_B42
    private val b61 = K.BLOCK_B61
    private val b7344 = K.BLOCK_044

    /** State A0: state A without T1's stored `◇ 첫 공개` record. */
    private fun stateA0(): ProjectDoc = withoutStoredReveal(stateA(), T1)

    private fun withoutStoredReveal(doc: ProjectDoc, termKey: String): ProjectDoc = doc.copy(
        changes = doc.changes.copy(records = doc.changes.records.filterNot { (it.target as? ChangeTarget.TermReveal)?.termKey == termKey }),
    )

    private fun storedReveals(doc: ProjectDoc, termKey: String) =
        doc.changes.records.filter { (it.target as? ChangeTarget.TermReveal)?.termKey == termKey }

    private fun candidatesOff(doc: ProjectDoc): ProjectDoc = doc.copy(project = doc.project.copy(termCandidates = false))

    private fun blockOf(spot: Any?): String? = when (spot) {
        is ScriptSpot.BlockText -> spot.blockKey
        is ScriptSpot.OptionText -> spot.blockKey
        else -> null
    }

    // --- WLD-72 automatic reveal ----------------------------------------------------------------

    @Test
    fun `WLD-T73 automatic reveal at the first manuscript link`() {
        val doc = stateA0()
        val effective = TermReveals.effective(doc, T1)!!
        assertEquals(RevealKind.AUTO, effective.kind)
        assertEquals(Anchor.AtBlock(b42), effective.anchor)
        val q = queries(doc)
        val reveal = q.reveal(T1)!!
        assertTrue(reveal.isAuto)
        assertTrue(q.isAutoReveal(T1))
        assertNull(q.storedReveal(T1))
        assertEquals(b42, reveal.blockKey)
        assertTrue("no link before the automatic reveal", q.usage(T1).none(q::isBeforeReveal))
        val card = q.card(T1)!!
        assertEquals("쓰인 곳 · 2 · 후보 1", card.header)
        assertTrue(card.revealValue.startsWith("1-4 씬 2 · "))
        val row = q.list().revealed.single { it.termKey == T1 }
        assertEquals("첫 공개 1-4 · ◇ 후보 1", row.line1)
        assertNull(row.line2)
        val t1Rows = q.checks().rows.filter {
            when (it) {
                is TermCheckRow.BeforeReveal -> it.link.termKey == T1
                is TermCheckRow.CandidateBeforeReveal -> it.candidate.termKey == T1
                is TermCheckRow.NoReveal -> it.termKey == T1
                else -> false
            }
        }
        val only = t1Rows.single() as TermCheckRow.CandidateBeforeReveal
        assertEquals(b12, blockOf(only.candidate.spot))
        assertTrue(only.text.endsWith("첫 공개 1-4"))
        assertTrue("the automatic reveal is never stored", storedReveals(doc, T1).isEmpty())
    }

    @Test
    fun `WLD-T74 the automatic reveal follows the links`() {
        val unlinked = WorldFixture.withBlock(stateA0(), b42) { b ->
            b.copy(runs = b.runs.map { r -> if (r is TextRun.Link && r.target.key == T1) TextRun.Plain(r.text) else r })
        }
        val q = queries(unlinked)
        val reveal = q.reveal(T1)!!
        assertTrue(reveal.isAuto)
        assertEquals(b7344, reveal.blockKey)
        assertFalse("no 근거 지워짐 for an automatic reveal", reveal.orphaned)
        assertTrue(q.preRevealCandidates(T1).any { blockOf(it.spot) == b42 && it.kind == CandidateKind.FULL })
        assertTrue(storedReveals(unlinked, T1).isEmpty())
    }

    @Test
    fun `WLD-T74 trashing the reveal block moves the automatic reveal, restoring brings it back`() {
        val trashed = WorldFixture.trashBlock(stateA0(), b42)
        assertEquals(b7344, queries(trashed).reveal(T1)!!.blockKey)
        assertEquals(Anchor.AtBlock(b7344), TermReveals.effective(trashed, T1)!!.anchor)
        assertFalse(queries(trashed).reveal(T1)!!.orphaned)
        val restored = WorldFixture.withBlock(trashed, b42) { it.copy(deletedAt = null) }
        assertEquals(b42, queries(restored).reveal(T1)!!.blockKey)
        assertTrue(storedReveals(trashed, T1).isEmpty())
        assertTrue(storedReveals(restored, T1).isEmpty())
    }

    @Test
    fun `WLD-72 the domain effective reveal and the queries agree in every case`() {
        val t = key(790)
        val base = WorldFixture.addTerm(stateA0(), t, "검은 문장")
        val memoFirst = WorldFixture.addBlockAfter(
            WorldFixture.withBlock(base, b42) { it.copy(runs = it.runs + TextRun.Plain(" ") + WorldFixture.link(key(791), t, "검은 문장")) },
            b12, key(792), listOf(WorldFixture.link(key(793), t, "검은 문장")), kind = BlockKind.MEMO,
        )
        val trashedFirst = WorldFixture.trashBlock(
            WorldFixture.withBlock(memoFirst, b12) { it.copy(runs = it.runs + TextRun.Plain(" ") + WorldFixture.link(key(794), t, "검은 문장")) },
            b12,
        )
        val sceneKey = base.index.block(b42)!!.sceneKey
        val hiddenScene = memoFirst.copy(story = memoFirst.story.copy(units = memoFirst.story.units.map { if (it.key == sceneKey) it.copy(hidden = true) else it }))
        for ((name, doc) in listOf("memo first" to memoFirst, "trashed first" to trashedFirst, "hidden scene" to hiddenScene)) {
            val domain = TermReveals.effective(doc, t)
            for (open in listOf(false, true)) {
                val q = queries(doc, TermAccess(hiddenUnlocked = open))
                assertEquals("$name, unlocked=$open", (domain?.anchor as? Anchor.AtBlock)?.blockKey, q.reveal(t)?.blockKey)
                assertEquals(name, b42, q.reveal(t)?.blockKey)
            }
            assertEquals(name, RevealKind.AUTO, domain!!.kind)
        }
    }

    @Test
    fun `WLD-72 moving away from the automatic reveal writes exactly one stored record`() {
        val doc = stateA0()
        val applied = TermEdits.setReveal(doc, T1, io.github.lenticualrstone.civilight.domain.world.term.RevealTarget.Unit(doc.index.block(b7344)!!.sceneKey), ctx())
            as io.github.lenticualrstone.civilight.domain.world.term.RevealEdit.Applied
        val stored = storedReveals(applied.doc, T1)
        assertEquals(1, stored.size)
        assertFalse(stored.single().key.startsWith(TermReveals.AUTO_KEY_PREFIX))
        assertTrue(applied.doc.changes.records.none { it.key.startsWith(TermReveals.AUTO_KEY_PREFIX) })
        assertFalse(queries(applied.doc).isAutoReveal(T1))
    }

    @Test
    fun `WLD-73 nothing waits for a scan while 용어 후보 is off`() {
        val off = candidatesOff(stateA())
        val pending = io.github.lenticualrstone.civilight.domain.world.term.TermQueries(off, io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex.pending())
        assertFalse(pending.scanPending)
        assertFalse(pending.card(T1)!!.candidatesPending)
        assertFalse(pending.checks().scanning)
        val on = io.github.lenticualrstone.civilight.domain.world.term.TermQueries(stateA(), io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex.pending())
        assertTrue(on.scanPending)
    }

    @Test
    fun `WLD-T75 a stored record wins over the automatic reveal`() {
        val q = queries(stateA())
        val reveal = q.reveal(T1)!!
        assertFalse(reveal.isAuto)
        assertEquals(RevealKind.STORED, TermReveals.effective(stateA(), T1)!!.kind)
        assertEquals(b7344, reveal.blockKey)
        assertEquals(listOf(b42), q.activeWarnings(T1).map { blockOf(it.spot) })
    }

    @Test
    fun `WLD-T77 first link in a choice line reveals the choice block`() {
        val t10 = key(770)
        var doc = WorldFixture.addTerm(stateA0(), t10, "은빛 열쇠")
        doc = WorldFixture.withBlock(doc, K.BLOCK_1_5_CHOICE) { b ->
            val opt = b.options.last()
            b.copy(options = b.options.map { if (it.key == opt.key) it.copy(runs = it.runs + TextRun.Plain(" ") + WorldFixture.link(key(771), t10, "은빛 열쇠")) else it })
        }
        assertEquals(BlockKind.CHOICE, doc.index.block(K.BLOCK_1_5_CHOICE)!!.kind.known)
        val effective = TermReveals.effective(doc, t10)!!
        assertEquals(Anchor.AtBlock(K.BLOCK_1_5_CHOICE), effective.anchor)
        val q = queries(doc)
        assertEquals(K.BLOCK_1_5_CHOICE, q.reveal(t10)!!.blockKey)
        assertTrue(q.usage(t10).none(q::isBeforeReveal))
    }

    @Test
    fun `WLD-T78 links only in a memo never reveal`() {
        val t11 = key(780)
        var doc = WorldFixture.addTerm(stateA0(), t11, "잿빛 문장")
        doc = WorldFixture.addBlockAfter(doc, b42, key(781), listOf(WorldFixture.link(key(782), t11, "잿빛 문장")), kind = BlockKind.MEMO)
        assertNull(TermReveals.effective(doc, t11))
        val q = queries(doc)
        assertNull(q.reveal(t11))
        val list = q.list()
        assertEquals("첫 공개 없음", list.pending.single { it.termKey == t11 }.line1)
        assertTrue(q.checks().rows.none { it is TermCheckRow.NoReveal && it.termKey == t11 })
        assertTrue(q.card(t11)!!.notices.none { it.startsWith("첫 공개 없음") })
    }

    @Test
    fun `WLD-T78 links only in a direction block never reveal`() {
        val t12 = key(785)
        var doc = WorldFixture.addTerm(stateA0(), t12, "잿빛 등잔")
        doc = WorldFixture.addBlockAfter(doc, b42, key(786), listOf(WorldFixture.link(key(787), t12, "잿빛 등잔")), kind = BlockKind.DIRECTION)
        assertNull(TermReveals.effective(doc, t12))
        val q = queries(doc)
        assertNull(q.reveal(t12))
        assertTrue(q.usage(t12).isEmpty())
        assertEquals("첫 공개 없음", q.list().pending.single { it.termKey == t12 }.line1)
    }

    @Test
    fun `WLD-T87 game-text links are listed and counted only with the game tools switch on`() {
        val journal = io.github.lenticualrstone.civilight.model.sample.ExampleKeys.ARCHIVE_RIEN_JOURNAL
        fun withArchiveLink(doc: ProjectDoc): ProjectDoc = doc.copy(
            cast = doc.cast.copy(
                archiveDocs = doc.cast.archiveDocs.map { a ->
                    if (a.key != journal) a else a.copy(body = a.body.mapIndexed { i, p -> if (i == 0) p.copy(runs = p.runs + TextRun.Plain(" ") + WorldFixture.link(key(795), T1, "오리지늄")) else p })
                },
            ),
        )
        val on = withArchiveLink(stateA()).let { it.copy(project = it.project.copy(gameTools = true)) }
        val off = on.copy(project = on.project.copy(gameTools = false))
        val archive = { d: ProjectDoc -> queries(d).outside(T1).count { it.spot is io.github.lenticualrstone.civilight.domain.world.term.OutsideSpot.ArchiveText } }
        assertEquals(1, archive(on))
        assertEquals(0, archive(off))
        assertEquals(queries(on).outside(T1).size - 1, queries(off).outside(T1).size)
        // The link itself is kept.
        assertEquals(queries(on).links(T1).size, queries(off).links(T1).size)
    }

    @Test
    fun `WLD-T84 a dateless world event sits after the dated world events, before the story`() {
        val base = stateA()
        val dateless = io.github.lenticualrstone.civilight.model.world.TimelineEvent(
            key = key(796), order = io.github.lenticualrstone.civilight.model.OrderKeys.FIRST, title = "결사 창립",
            kind = io.github.lenticualrstone.civilight.model.world.EventKind.WORLD.open(),
        )
        val doc = base.copy(world = base.world.copy(events = base.world.events + dateless))
        val rows = io.github.lenticualrstone.civilight.domain.world.time.TimelineBuilder(doc, io.github.lenticualrstone.civilight.domain.world.time.StoryEngine(doc)).build().rows
        val at = rows.indexOfFirst { (it as? io.github.lenticualrstone.civilight.domain.world.time.TimelineRow.EventRow)?.event?.key == dateless.key }
        val divider = rows.indexOfFirst { it is io.github.lenticualrstone.civilight.domain.world.time.TimelineRow.Divider }
        assertTrue(at >= 0 && divider >= 0)
        assertEquals("right above 이야기 시작", divider - 1, at)
    }

    @Test
    fun `WLD-T35 superseded - a term with links gets the automatic reveal and no suggestion chip`() {
        val t8 = key(350)
        var doc = WorldFixture.addTerm(stateA(), t8, "검은 등잔")
        doc = WorldFixture.withBlock(doc, b12) { it.copy(runs = it.runs + TextRun.Plain(" ") + WorldFixture.link(key(351), t8, "검은 등잔")) }
        val q = queries(doc)
        assertEquals(b12, q.reveal(t8)!!.blockKey)
        val card = q.card(t8)!!
        assertTrue(card.chips.none { it is io.github.lenticualrstone.civilight.domain.world.term.TermChip.SuggestReveal })
        assertTrue(q.checks().rows.none { it is TermCheckRow.NoReveal })
    }

    @Test
    fun `WLD-T89 automatic reveal in a hidden scene`() {
        val base = stateA0()
        val sceneKey = base.index.block(b42)!!.sceneKey
        val hidden = base.copy(story = base.story.copy(units = base.story.units.map { if (it.key == sceneKey) it.copy(hidden = true) else it }))
        val locked = queries(hidden, TermAccess(hiddenUnlocked = false))
        assertEquals(b42, locked.reveal(T1)!!.blockKey)
        assertTrue(locked.usage(T1).none(locked::isBeforeReveal))
        assertEquals("1-4 · 숨긴 씬", locked.card(T1)!!.revealValue)
        val open = queries(hidden, TermAccess(hiddenUnlocked = true))
        assertTrue(open.card(T1)!!.revealValue.startsWith("1-4 씬 2 · "))
    }

    // --- WLD-73 candidate check -----------------------------------------------------------------

    @Test
    fun `WLD-T79 candidates off hides every candidate surface and keeps the records`() {
        val on = stateA()
        val doc = candidatesOff(on)
        val q = queries(doc)
        val card = q.card(T1)!!
        assertEquals("쓰인 곳 · 2", card.header)
        assertNull(card.candidateCount)
        assertTrue(card.candidates.rows.isEmpty())
        assertTrue(card.notices.none { it.contains("글자 찾기") })
        val row = q.list().revealed.single { it.termKey == T1 }
        assertEquals("첫 공개 1-7", row.line1)
        assertEquals("▲ 앞에서 1회 사용", row.line2)
        assertTrue(q.checks().rows.none { it is TermCheckRow.CandidateBeforeReveal })
        assertTrue(q.checks().rows.any { it is TermCheckRow.BeforeReveal })
        assertTrue(q.candidateSpans(b12).isEmpty())
        assertNull(q.candidateSection())
        // `아님` records and `글자 찾기` are kept while off.
        val rejected = TermEdits.reject(on, queries(on).candidates(T1).first(), ctx())
        val rejectedOff = candidatesOff(rejected)
        assertSame(rejectedOff, StaleRejections.release(rejectedOff))
        assertSame(rejectedOff, TermEdits.releaseStaleRejections(rejectedOff, WorldFixture.index(rejectedOff)))
        assertEquals(on.index.term(T1)!!.textSearch, doc.index.term(T1)!!.textSearch)
    }

    @Test
    fun `WLD-T80 candidate section is the last section of the term tab`() {
        val q = queries(stateB())
        val section = q.candidateSection()!!
        assertEquals("용어 후보 · ${section.rows.size}", section.title)
        assertEquals(q.list().let { l -> (l.revealed + l.pending).map { it.termKey } }.filter { k -> section.rows.any { it.termKey == k } }, section.rows.map { it.termKey })
        val row = section.rows.single { it.termKey == T1 }
        assertEquals(T1, row.termKey)
        assertEquals("오리지늄 등불", row.name)
        assertEquals("후보 3", row.fragment)
        assertEquals("◇ 공개 전 후보 3", row.right)
    }

    @Test
    fun `WLD-T81 reference row data for one block`() {
        val doc = stateB()
        val q = queries(doc)
        val span = q.candidateSpans(b61).single()
        assertEquals(T1, span.termKey)
        assertEquals("오리지늄 등불", span.termName)
        assertEquals("빛돌", span.matchedText)
        assertEquals(CandidateKind.FULL, span.kind)
        assertEquals(5, span.start)
        assertEquals(7, span.end)
        assertNull(span.optionIndex)
        assertEquals("용어 후보 · 빛돌", q.referenceLine(span))
        val rejected = TermEdits.reject(doc, span.candidate, ctx())
        assertTrue(queries(rejected).candidateSpans(b61).isEmpty())
    }

    // --- WLD-70 create in place -----------------------------------------------------------------

    @Test
    fun `WLD-T82 create a term in place`() {
        val doc = stateB()
        val r = WorldCreate.create(doc, WorldKind.TERM, NewWorldItem("반딧돌", "작은 빛을 내는 돌", alias = "반디"), ctx()) as CreateResult.Created
        val term = r.doc.index.term(r.key)!!
        assertEquals("반딧돌", term.name)
        assertEquals(listOf("반디"), term.aliases)
        assertEquals("작은 빛을 내는 돌", term.description.docText)
        assertEquals(doc.world.terms.size + 1, r.doc.world.terms.size)
        val single = WorldCreate.create(doc, WorldKind.TERM, NewWorldItem("반딧돌", alias = "반디, 개똥"), ctx()) as CreateResult.Created
        assertEquals(listOf("반디, 개똥"), single.doc.index.term(single.key)!!.aliases)
        val same = WorldCreate.create(doc, WorldKind.TERM, NewWorldItem("반딧돌", alias = "반딧돌"), ctx()) as CreateResult.Created
        assertTrue(same.doc.index.term(same.key)!!.aliases.isEmpty())
        assertEquals(CreateResult.EmptyName, WorldCreate.create(doc, WorldKind.TERM, NewWorldItem("  "), ctx()))
        assertFalse(WorldCreate.canCreate(" "))
        assertTrue(WorldCreate.canCreate("반딧돌"))
    }

    @Test
    fun `U-15_7 create a place with a parent and a faction`() {
        val doc = stateB()
        val parent = doc.world.places.first { it.deletedAt == null }
        val place = WorldCreate.create(doc, WorldKind.PLACE, NewWorldItem("은빛 항구", "바닷가", parentPlaceKey = parent.key), ctx()) as CreateResult.Created
        assertEquals(parent.key, place.doc.index.place(place.key)!!.parentPlaceKey)
        val noParent = WorldCreate.create(doc, WorldKind.PLACE, NewWorldItem("은빛 항구", parentPlaceKey = "missing"), ctx()) as CreateResult.Created
        assertNull(noParent.doc.index.place(noParent.key)!!.parentPlaceKey)
        val faction = WorldCreate.create(doc, WorldKind.FACTION, NewWorldItem("은빛 상단", "상인 조합"), ctx()) as CreateResult.Created
        assertEquals("상인 조합", faction.doc.index.faction(faction.key)!!.description.docText)
    }

    @Test
    fun `WLD-T83 create a place from a picker`() {
        val doc = stateB()
        assertFalse(WorldCreate.placePickerMatched(doc, "은빛 항구"))
        assertTrue(WorldCreate.placePickerMatched(doc, ""))
        val (next, ref) = WorldCreate.createPlaceAndPick(doc, NewWorldItem("은빛 항구"), ctx())!!
        assertEquals(PlaceRefKind.PLACE, ref.kind.known)
        assertEquals("은빛 항구", next.index.place(ref.key)!!.name)
        assertTrue(next.maps.values.none { m -> m.pins.any { it.placeKey == ref.key } })
        assertNull(WorldCreate.createPlaceAndPick(doc, NewWorldItem(""), ctx()))
    }

    // --- WLD-75 maps ----------------------------------------------------------------------------

    @Test
    fun `WLD-75 a new map is named with the smallest free number`() {
        val doc = stateB()
        val c = ctx()
        val empty = doc.copy(maps = emptyMap())
        assertEquals("지도 1", MapNames.nextDefault(empty))
        val one = (MapEdits.newMap(empty, MapBaseKind.IMAGE, c, name = "지도 1") as io.github.lenticualrstone.civilight.domain.world.map.MapEdit.Applied).doc
        val three = (MapEdits.newMap(one, MapBaseKind.IMAGE, c, name = "지도 3") as io.github.lenticualrstone.civilight.domain.world.map.MapEdit.Applied).doc
        assertEquals("지도 2", MapNames.nextDefault(three))
        val trashedKey = three.maps.values.first { it.name == "지도 1" }.key
        val trashed = three.copy(maps = three.maps.mapValues { (k, m) -> if (k == trashedKey) m.copy(deletedAt = 1L) else m })
        assertEquals("지도 1", MapNames.nextDefault(trashed))
        assertNotNull(doc.maps)
    }
}
