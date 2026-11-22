package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.FixtureR.HART
import io.github.lenticualrstone.civilight.model.FixtureR.ORIGINIUM
import io.github.lenticualrstone.civilight.model.FixtureR.RIEN
import io.github.lenticualrstone.civilight.model.FixtureR.affiliation
import io.github.lenticualrstone.civilight.model.FixtureR.between
import io.github.lenticualrstone.civilight.model.FixtureR.blk
import io.github.lenticualrstone.civilight.model.FixtureR.ep
import io.github.lenticualrstone.civilight.model.FixtureR.hartAddress
import io.github.lenticualrstone.civilight.model.FixtureR.hartMind
import io.github.lenticualrstone.civilight.model.FixtureR.hartName
import io.github.lenticualrstone.civilight.model.FixtureR.narration
import io.github.lenticualrstone.civilight.model.FixtureR.record
import io.github.lenticualrstone.civilight.model.FixtureR.rienAddress
import io.github.lenticualrstone.civilight.model.FixtureR.rienMind
import io.github.lenticualrstone.civilight.model.FixtureR.sc
import io.github.lenticualrstone.civilight.model.FixtureR.withBlock
import io.github.lenticualrstone.civilight.model.FixtureR.withBlockChanged
import io.github.lenticualrstone.civilight.model.FixtureR.withRecord
import io.github.lenticualrstone.civilight.model.FixtureR.withUnit
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.doc.recordBeforeBlock
import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.ManuscriptIndex
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.project.StoryStructure
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.TimelineEvent
import io.github.lenticualrstone.civilight.model.world.WorldDate
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PositionTest {
    private val doc = FixtureR.doc()

    private fun ProjectDoc.valueAt(target: io.github.lenticualrstone.civilight.model.change.ChangeTarget, lens: LensPoint): String? =
        recordAt(target, lens)?.let { (it.value as? JsonPrimitive)?.content }

    private fun ProjectDoc.recordKeyAt(target: io.github.lenticualrstone.civilight.model.change.ChangeTarget, lens: LensPoint): String? =
        recordAt(target, lens)?.key

    private fun ProjectDoc.planned(key: String) = isPlanned(changes.records.single { it.key == key })

    private fun lensAt(unitKey: String) = LensPoint.UnitEnd(unitKey)

    // --- 20-story-time §4.2 Lens and 집필 선 ---

    @Test
    fun lens01WritingLineIsEndOfScene3Of17() {
        assertEquals(sc(1, 7, 3), doc.manuscript.writingLineSceneKey)
        assertEquals(doc.manuscript.rankOf(Position.End(sc(1, 7, 3))), doc.manuscript.writingLineRank)
        assertEquals(doc.manuscript.writingLineRank, doc.manuscript.rankOf(LensPoint.Latest))
    }

    @Test
    fun lens02AtLatest() {
        val latest = LensPoint.Latest
        assertEquals("경계", doc.valueAt(rienMind, latest))
        assertEquals("보호", doc.valueAt(hartMind, latest))
        assertEquals("꼬마", doc.valueAt(hartAddress, latest))
        assertNull("display name still ???", doc.recordAt(hartName, latest))
        assertEquals("등불 결사", doc.valueAt(affiliation, latest))
        assertTrue(doc.planned("R3"))
        assertTrue(doc.planned("R6"))
        assertTrue(doc.planned("R7"))
        assertTrue(doc.planned("R8"))
        assertFalse(doc.planned("R4"))
        assertFalse(doc.planned("R2"))
    }

    @Test
    fun lens03At16RelationNotMetYet() {
        val lens = lensAt(ep(1, 6))
        assertNull(doc.recordAt(rienMind, lens))
        assertNull(doc.recordAt(hartMind, lens))
        assertNull(doc.recordAt(between, lens))
        assertNull(doc.recordAt(hartName, lens))
    }

    @Test
    fun lens04And05At17And18() {
        for (lens in listOf(lensAt(ep(1, 7)), lensAt(ep(1, 8)))) {
            assertEquals("경계", doc.valueAt(rienMind, lens))
            assertEquals("보호", doc.valueAt(hartMind, lens))
            assertEquals("꼬마", doc.valueAt(hartAddress, lens))
            assertNull(doc.recordAt(hartName, lens))
        }
    }

    @Test
    fun lens06At19UnwrittenButSelectable() {
        val lens = lensAt(ep(1, 9))
        assertEquals("동료", doc.valueAt(between, lens))
        assertEquals("신뢰", doc.valueAt(rienMind, lens))
        assertEquals("하르트", doc.valueAt(rienAddress, lens))
        assertEquals("하르트", doc.valueAt(hartName, lens))
    }

    @Test
    fun lens07ScriptPanelReadsBlockLevelState() {
        assertNull(doc.recordBeforeBlock(rienMind, blk(42)))
        assertNull(doc.recordBeforeBlock(rienMind, blk(43)))
        assertEquals(listOf("R4", "R5", "R5a"), doc.recordsHere(blk(43)).map { it.key }.sorted())
        assertEquals("R4", doc.recordBeforeBlock(rienMind, blk(44))?.key)
        assertTrue(doc.recordsHere(blk(42)).isEmpty())
    }

    @Test
    fun lens08AndT1102PreRevealByBlockOrder() {
        assertTrue(doc.isLinkBeforeReveal(ORIGINIUM, blk(42)))
        assertTrue(doc.isLinkBeforeReveal(ORIGINIUM, blk(43)))
        assertFalse("inside the reveal block counts as after", doc.isLinkBeforeReveal(ORIGINIUM, blk(44)))
        assertFalse(doc.isLinkBeforeReveal(ORIGINIUM, blk(45)))
        // No reveal record → no ▲.
        val noReveal = doc.copy(changes = doc.changes.copy(records = doc.changes.records.filter { it.key != "R9" }))
        assertFalse(noReveal.isLinkBeforeReveal(ORIGINIUM, blk(42)))
    }

    private fun withContentIn19(): ProjectDoc = doc.withBlock(narration("B1-9-1", sc(1, 9, 1), OrderKeys.FIRST, 1))

    @Test
    fun lens09ContentIn19MovesTheWritingLine() {
        val d = withContentIn19()
        assertEquals(sc(1, 9, 1), d.manuscript.writingLineSceneKey)
        assertFalse(d.planned("R6"))
        assertFalse(d.planned("R7"))
        assertFalse(d.planned("R8"))
        assertEquals("하르트", d.valueAt(hartName, LensPoint.Latest))
        assertEquals("동료", d.valueAt(between, LensPoint.Latest))
    }

    @Test
    fun lens10NoContentMeansEverythingApplies() {
        val empty = doc.copy(episodes = doc.episodes.mapValues { (k, _) -> EpisodeFile(k) })
        assertNull(empty.manuscript.writingLineSceneKey)
        assertEquals(Rank.UNPLACED, empty.manuscript.rankOf(LensPoint.Latest))
        assertEquals("등불지기장", empty.valueAt(affiliation, LensPoint.Latest))
        assertTrue(empty.changes.records.none { empty.isPlanned(it) })
    }

    @Test
    fun lens11WritingIn25AppliesR3() {
        val d = doc.withBlock(narration("B2-5-1", sc(2, 5, 1), OrderKeys.FIRST, 1))
        assertEquals(sc(2, 5, 1), d.manuscript.writingLineSceneKey)
        assertEquals("등불지기장", d.valueAt(affiliation, LensPoint.Latest))
        assertFalse(d.planned("R3"))
    }

    @Test
    fun lens12TrashingThe19SceneBringsTheLineBack() {
        val d = withContentIn19().withUnit(sc(1, 9, 1)) { it.copy(deletedAt = 5L, trashEntryKey = "TE") }
        assertEquals(sc(1, 7, 3), d.manuscript.writingLineSceneKey)
        assertTrue(d.planned("R6"))
        assertTrue(d.planned("R7"))
        assertTrue(d.planned("R8"))
        assertEquals(doc.changes.records.size, d.changes.records.size)
    }

    @Test
    fun hiddenScenesCountForTheWritingLine() {
        val d = withContentIn19().withUnit(sc(1, 9, 1)) { it.copy(hidden = true) }
        assertEquals(sc(1, 9, 1), d.manuscript.writingLineSceneKey)
    }

    @Test
    fun emptyBlocksDoNotMoveTheWritingLine() {
        val empty = Block(key = "B1-9-x", sceneKey = sc(1, 9, 1), order = "V", number = 1, kind = BlockKind.DIALOGUE.open(),
            speakerKey = RIEN, runs = listOf(TextRun.Plain("  "), TextRun.Break))
        assertEquals(sc(1, 7, 3), doc.withBlock(empty).manuscript.writingLineSceneKey)
    }

    // --- §4.3 Editing under the lens ---

    @Test
    fun edit02UnitEndAtLatest() {
        val newRecord = record("RN", affiliation, Anchor.UnitEnd(sc(1, 7, 3)), "등불 결사 · 견습", createdAt = 9L)
        var d = doc.withRecord(newRecord)
        assertEquals("등불 결사 · 견습", d.valueAt(affiliation, LensPoint.Latest))
        assertEquals("R2", d.recordBeforeBlock(affiliation, blk(45))?.key)
        assertFalse(d.isPlanned(newRecord))
        // A block appended to the same scene later does not see it; a block in 1-8 does.
        d = d.withBlock(narration(blk(46), sc(1, 7, 3), "z", 46))
            .withBlock(narration("B1-8-1", sc(1, 8, 1), OrderKeys.FIRST, 1))
        assertEquals("R2", d.recordBeforeBlock(affiliation, blk(46))?.key)
        assertEquals("RN", d.recordBeforeBlock(affiliation, "B1-8-1")?.key)
    }

    @Test
    fun edit03UnitStartOf15() {
        val d = doc.withRecord(record("RS", affiliation, Anchor.UnitStart(ep(1, 5)), "등불 결사 · 견습", createdAt = 9L))
        assertEquals("RS", d.recordKeyAt(affiliation, lensAt(ep(1, 5))))
        assertEquals("R2", d.recordKeyAt(affiliation, lensAt(ep(1, 4))))
    }

    @Test
    fun tiesGoToTheLaterCreatedAtThenKey() {
        val a = record("RA", affiliation, Anchor.UnitStart(ep(1, 4)), "A", createdAt = 10L)
        val b = record("RB", affiliation, Anchor.UnitStart(ep(1, 4)), "B", createdAt = 20L)
        val c = record("RC", affiliation, Anchor.UnitStart(ep(1, 4)), "C", createdAt = 20L)
        val d = doc.withRecord(a).withRecord(c).withRecord(b)
        assertEquals("RC", d.recordKeyAt(affiliation, lensAt(ep(1, 4))))
    }

    @Test
    fun trashedRecordsAndRecordsOfTrashedTargetsDoNotApply() {
        val trashedRecord = doc.copy(changes = doc.changes.copy(records = doc.changes.records.map {
            if (it.key == "R4") it.copy(deletedAt = 1L) else it
        }))
        assertNull(trashedRecord.recordAt(rienMind, lensAt(ep(1, 8))))
        val trashedTarget = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map {
            if (it.key == HART) it.copy(deletedAt = 1L) else it
        }))
        // Relation not live (one end trashed) → its records are not live (TC-20-23 analogue).
        assertNull(trashedTarget.recordAt(rienMind, lensAt(ep(1, 8))))
        assertNull(trashedTarget.recordAt(hartName, lensAt(ep(1, 9))))
    }

    @Test
    fun unknownTargetsAreIgnoredAndUnknownAnchorsSortAsUnplaced() {
        val unknownAnchor = record("RU", affiliation, Anchor.Unknown(kotlinx.serialization.json.buildJsonObject {
            put("kind", JsonPrimitive("sceneMiddle"))
        }), "X", createdAt = 99L)
        val d = doc.withRecord(unknownAnchor)
        assertEquals(Rank.UNPLACED, d.manuscript.rankOf(unknownAnchor.anchor))
        assertTrue(d.isPlanned(unknownAnchor))
        assertEquals("R2", d.recordKeyAt(affiliation, LensPoint.Latest))
    }

    // --- 10-script §4.2 relocation targets (T-12-06, T-12-07) ---

    @Test
    fun t1206RelocationOrder() {
        val m = doc.withBlock(narration("B1-7-4", sc(1, 7, 4), OrderKeys.FIRST, 46))
            .withBlock(narration("B1-8-1", sc(1, 8, 1), OrderKeys.FIRST, 1))
        // (a) last block of a scene → first block of the next scene.
        val a = m.withBlockChanged(blk(45)) { it.copy(deletedAt = 1L) }
        assertEquals(Anchor.AtBlock("B1-7-4"), a.manuscript.relocationTarget(Anchor.AtBlock(blk(45))))
        assertEquals(a.manuscript.rankOf(Position.AtBlock("B1-7-4")), a.manuscript.rankOf(Anchor.AtBlock(blk(45))))
        // (b) last block of an episode → first block of the next episode.
        val b = m.withBlockChanged("B1-7-4") { it.copy(deletedAt = 1L) }
        assertEquals(Anchor.AtBlock("B1-8-1"), b.manuscript.relocationTarget(Anchor.AtBlock("B1-7-4")))
        // (c) the last block of the project → unplaced, which is 예정 while a 집필 선 exists.
        val c = b.withBlockChanged("B1-8-1") { it.copy(deletedAt = 1L) }
        assertEquals(Anchor.Unplaced, c.manuscript.relocationTarget(Anchor.AtBlock("B1-8-1")))
        assertEquals(Rank.UNPLACED, c.manuscript.rankOf(Anchor.AtBlock("B1-8-1")))
        assertTrue(c.manuscript.isPlanned(Anchor.Unplaced))
        // Without any 집필 선, unplaced is not 예정.
        val empty = doc.copy(episodes = doc.episodes.mapValues { (k, _) -> EpisodeFile(k) })
        assertFalse(empty.manuscript.isPlanned(Anchor.Unplaced))
    }

    @Test
    fun t1207DeletingASceneResolvesItsAnchorsToTheNextScene() {
        val m = doc.withBlock(narration("B1-7-4", sc(1, 7, 4), OrderKeys.FIRST, 46))
            .withUnit(sc(1, 7, 3)) { it.copy(deletedAt = 1L) }
        for (key in listOf(blk(43), blk(44))) {
            assertEquals(Anchor.AtBlock("B1-7-4"), m.manuscript.relocationTarget(Anchor.AtBlock(key)))
        }
        assertEquals(Anchor.AtBlock("B1-7-4"), m.manuscript.relocationTarget(Anchor.UnitEnd(sc(1, 7, 3))))
        // Reads before relocation agree with reads after it.
        assertEquals(m.manuscript.rankOf(Anchor.AtBlock("B1-7-4")), m.manuscript.rankOf(Anchor.AtBlock(blk(43))))
        assertFalse(m.manuscript.isLiveBlock(blk(42)))
        assertEquals(listOf(sc(1, 7, 1), sc(1, 7, 2), sc(1, 7, 4)), m.manuscript.liveScenes.filter { it.parentKey == ep(1, 7) }.map { it.key })
    }

    @Test
    fun anchorsToMissingReferents() {
        assertEquals(Rank.UNPLACED, doc.manuscript.rankOf(Anchor.AtBlock("NOPE")))
        assertEquals(Rank.UNPLACED, doc.manuscript.rankOf(Anchor.UnitStart("NOPE")))
        assertNull(doc.manuscript.rankOf(Position.AtBlock("NOPE")))
    }

    // --- 10-script §4.2: moves keep anchors (T-12-11, T-12-21) ---

    @Test
    fun t1211MovingABlockToAnotherSceneCarriesItsRecords() {
        val moved = doc.withBlock(narration("B1-7-4", sc(1, 7, 4), OrderKeys.FIRST, 46))
            .withBlockChanged(blk(43)) { it.copy(sceneKey = sc(1, 7, 4), order = OrderKeys.after(OrderKeys.FIRST)) }
        assertEquals(moved.manuscript.rankOf(Position.AtBlock(blk(43))), moved.manuscript.rankOf(Anchor.AtBlock(blk(43))))
        // R4 now starts in 씬 4, after the remaining 씬 3 blocks: the script panel of #044 no longer sees it.
        assertNull(moved.recordBeforeBlock(rienMind, blk(44)))
        assertTrue(moved.manuscript.compare(Position.AtBlock("B1-7-4"), Position.AtBlock(blk(43))) < 0)
    }

    @Test
    fun t1221MovingAcrossTheWritingLineNeedsNoRelinking() {
        val moved = doc.withBlockChanged(blk(43)) { it.copy(sceneKey = sc(1, 9, 1), number = 1) }.regroupBlocks()
        assertEquals(ep(1, 9), moved.index.episodeGroupOfBlock(blk(43)))
        assertEquals(sc(1, 9, 1), moved.manuscript.writingLineSceneKey)
        assertFalse(moved.planned("R4"))
        // #043 now sits inside 1-9 씬 1, after R7's `unitStart(1-9)`, so R4 is the latest mind value.
        assertEquals("R4", moved.recordKeyAt(rienMind, LensPoint.Latest))
        // At 1-8 neither the moved R4 nor R7 applies yet: 만나기 전.
        assertNull(moved.recordAt(rienMind, lensAt(ep(1, 8))))
        assertTrue(moved.manuscript.compare(Position.End(sc(1, 7, 3)), Position.AtBlock(blk(43))) < 0)
    }

    @Test
    fun t1219DeletingSeveralBlocksResolvesTheirAnchorsToTheNextBlock() {
        val d = doc.withBlockChanged(blk(42)) { it.copy(deletedAt = 1L) }.withBlockChanged(blk(43)) { it.copy(deletedAt = 1L) }
        assertEquals(Anchor.AtBlock(blk(44)), d.manuscript.relocationTarget(Anchor.AtBlock(blk(42))))
        assertEquals(Anchor.AtBlock(blk(44)), d.manuscript.relocationTarget(Anchor.AtBlock(blk(43))))
        assertEquals(d.manuscript.rankOf(Position.AtBlock(blk(44))), d.manuscript.rankOf(Anchor.AtBlock(blk(43))))
        // Before relocation is written, the record already reads from #044 on.
        assertEquals("R4", d.recordBeforeBlock(rienMind, blk(45))?.key)
        assertNull(d.recordBeforeBlock(rienMind, blk(44)))
    }

    @Test
    fun t1214MovingTheRevealBehindItsUsesMakesThemPreReveal() {
        val withLink = doc.withBlockChanged(blk(45)) { it.copy(runs = it.runs + FixtureR.originiumLink("L45")) }
        assertFalse(withLink.isLinkBeforeReveal(ORIGINIUM, blk(45)))
        val moved = withLink.withBlockChanged(blk(44)) { it.copy(sceneKey = sc(1, 8, 1)) }.regroupBlocks()
        assertTrue(moved.isLinkBeforeReveal(ORIGINIUM, blk(45)))
    }

    @Test
    fun aRecordWithANullValueIsARecordNotTheBaseValue() {
        val reveal = record("RN", hartName, Anchor.UnitStart(ep(1, 2)), null, createdAt = 5L)
        val d = doc.withRecord(reveal)
        val found = d.recordAt(hartName, lensAt(ep(1, 3)))
        assertEquals("RN", found?.key)
        assertNull(found?.value)
        assertNull(d.recordAt(hartName, lensAt(ep(1, 1))))
    }

    // --- §4.3 total order ---

    @Test
    fun positionOrderInsideAScene() {
        val m = doc.manuscript
        val order = listOf(
            Position.Start(ep(1, 7)), Position.Start(sc(1, 7, 3)), Position.Transition(sc(1, 7, 3)),
            Position.AtBlock(blk(42)), Position.AtBlock(blk(43)), Position.AtBlock(blk(44)), Position.AtBlock(blk(45)),
            Position.End(sc(1, 7, 3)), Position.Start(sc(1, 7, 4)), Position.End(sc(1, 7, 4)), Position.End(ep(1, 7)),
            Position.Start(ep(1, 8)), Position.End(FixtureR.ch(1)), Position.Start(FixtureR.ch(2)), Position.Unplaced,
        )
        for (i in 0 until order.size - 1) {
            assertTrue("${order[i]} < ${order[i + 1]}", m.compare(order[i], order[i + 1]) < 0)
        }
    }

    @Test
    fun worldEventsSortBeforeTheStoryLatestDateLast() {
        val events = listOf(
            TimelineEvent(key = "EV_1195", order = "V", kind = EventKind.WORLD.open(), title = "대정전", worldDate = WorldDate(1195, 11)),
            TimelineEvent(key = "EV_1100", order = "V", kind = EventKind.WORLD.open(), title = "건국", worldDate = WorldDate(1100)),
            TimelineEvent(key = "EV_1195b", order = "V", kind = EventKind.WORLD.open(), title = "같은 해", worldDate = WorldDate(1195)),
            TimelineEvent(key = "EV_1195c", order = "k", kind = EventKind.WORLD.open(), title = "같은 해 2", worldDate = WorldDate(1195)),
        )
        val d = doc.copy(world = WorldFile(events = events))
        val m = d.manuscript
        val ranks = listOf("EV_1100", "EV_1195b", "EV_1195c", "EV_1195").map { m.rankOf(Anchor.WorldEvent(it)) }
        assertEquals(ranks.sorted(), ranks)
        assertEquals(Rank(-1), m.rankOf(Anchor.WorldEvent("EV_1195")))
        assertTrue(ranks.last() < m.rankOf(Anchor.UnitStart(FixtureR.ch(1))))
        // A purged event's anchor sorts before every event.
        assertTrue(m.rankOf(Anchor.WorldEvent("GONE")) < ranks.first())
        // A trashed event still sorts by its date.
        val trashed = d.copy(world = WorldFile(events = events.map { if (it.key == "EV_1100") it.copy(deletedAt = 1L) else it }))
        assertEquals(m.rankOf(Anchor.WorldEvent("EV_1100")), trashed.manuscript.rankOf(Anchor.WorldEvent("EV_1100")))
        // Event records apply at every lens until a later record takes over.
        val withRecord = d.withRecord(record("RE", hartName, Anchor.WorldEvent("EV_1100"), "옛 이름"))
        assertEquals("RE", withRecord.recordKeyAt(hartName, lensAt(ep(1, 1))))
        assertEquals("R8", withRecord.recordKeyAt(hartName, lensAt(ep(1, 9))))
    }

    // --- §4.6 PointRef ---

    @Test
    fun pointRefVisibility() {
        val m = doc.manuscript
        val from19 = PointRef.unitStart(ep(1, 9))
        assertTrue(m.isVisible(from19, lensAt(ep(1, 9))))
        assertFalse(m.isVisible(from19, lensAt(ep(1, 8))))
        assertFalse("1-9 is after the 집필 선", m.isVisible(from19, LensPoint.Latest))
        val after17 = PointRef.unitEnd(ep(1, 7))
        assertTrue(m.isVisible(after17, lensAt(ep(1, 7))))
        assertFalse(m.isVisible(after17, lensAt(ep(1, 6))))
        assertTrue(m.isVisible(null, lensAt(ep(1, 1))))
    }

    @Test
    fun pointRefToANonLiveUnitResolvesToTheNextLiveUnitStart() {
        val d = doc.withUnit(ep(1, 8)) { it.copy(deletedAt = 1L) }
        val m = d.manuscript
        val from18 = PointRef.unitStart(ep(1, 8))
        assertEquals(m.rankOf(Position.Start(ep(1, 9))), m.rankOf(from18))
        assertFalse(m.isVisible(from18, lensAt(ep(1, 7))))
        assertTrue(m.isVisible(from18, lensAt(ep(1, 9))))
        // The last scene of 1-7 trashed: the next live unit is 1-8 (not 1-7's end).
        val d2 = doc.withUnit(sc(1, 7, 4)) { it.copy(deletedAt = 1L) }
        assertFalse(d2.manuscript.isVisible(PointRef.unitStart(sc(1, 7, 4)), lensAt(ep(1, 7))))
        assertTrue(d2.manuscript.isVisible(PointRef.unitStart(sc(1, 7, 4)), lensAt(ep(1, 8))))
        // Restored → normal again.
        assertTrue(doc.manuscript.isVisible(PointRef.unitStart(sc(1, 7, 4)), lensAt(ep(1, 7))))
        // Nothing after → +∞; unknown unit → +∞.
        val d3 = doc.withUnit(FixtureR.ch(2)) { it.copy(deletedAt = 1L) }
        assertEquals(Rank.UNPLACED, d3.manuscript.rankOf(PointRef.unitStart(ep(2, 5))))
        assertEquals(Rank.UNPLACED, doc.manuscript.rankOf(PointRef.unitStart("NOPE")))
    }

    // --- §4.1 numbering unit ---

    @Test
    fun numberingUnits() {
        assertEquals(ep(1, 7), doc.index.numberingUnitKeyOfScene(sc(1, 7, 3)))
        val oneLevel = ProjectDoc(
            project = doc.project.copy(structure = StoryStructure(levels = listOf("씬"))),
            story = StoryIndexFile(listOf(StoryUnit(key = "S1", level = 0, order = "V"))),
        )
        assertEquals(FixtureR.PROJECT, oneLevel.index.numberingUnitKeyOfScene("S1"))
        val twoLevel = ProjectDoc(
            project = doc.project.copy(structure = StoryStructure(levels = StoryStructure.ACT_SCENE_LEVELS)),
            story = StoryIndexFile(listOf(
                StoryUnit(key = "A1", level = 0, order = "V"),
                StoryUnit(key = "A1S1", parentKey = "A1", level = 1, order = "V"),
            )),
        )
        assertEquals("A1", twoLevel.index.numberingUnitKeyOfScene("A1S1"))
        assertEquals(0, twoLevel.project.structure.episodeLevel)
        assertNull(oneLevel.project.structure.episodeLevel)
    }

    // --- §4.3 rank order equals the sort-key order ---

    @Test
    fun rankOrderMatchesTheSortKeyDefinition() {
        val random = Random(11)
        repeat(20) {
            val structure = StoryStructure()
            val units = mutableListOf<StoryUnit>()
            val blocks = mutableListOf<Block>()
            fun randomOrders(n: Int) = List(n) { OrderKeys.evenlySpaced(200)[random.nextInt(200)] }.distinct()
            for ((ci, co) in randomOrders(3).withIndex()) {
                val c = "C$ci"
                units += StoryUnit(key = c, level = 0, order = co)
                for ((ei, eo) in randomOrders(3).withIndex()) {
                    val e = "$c-E$ei"
                    units += StoryUnit(key = e, parentKey = c, level = 1, order = eo)
                    for ((si, so) in randomOrders(3).withIndex()) {
                        val s = "$e-S$si"
                        units += StoryUnit(key = s, parentKey = e, level = 2, order = so)
                        for ((bi, bo) in randomOrders(4).withIndex()) {
                            blocks += Block(key = "$s-B$bi", sceneKey = s, order = bo, number = bi + 1, kind = BlockKind.NARRATION.open())
                        }
                    }
                }
            }
            val m = ManuscriptIndex.build(structure, units.shuffled(random), blocks.shuffled(random), emptyList())
            val byKey = units.associateBy { it.key }
            fun path(unitKey: String): List<String> {
                val out = mutableListOf<String>()
                var u = byKey[unitKey]
                while (u != null) {
                    out.add(0, u.order)
                    u = u.parentKey?.let(byKey::get)
                }
                return out
            }
            // Sort key elements: START < TRANSITION < order keys < END.
            val positions = units.flatMap { u ->
                listOf(Position.Start(u.key) to path(u.key) + "\u0000", Position.End(u.key) to path(u.key) + "￿") +
                    if (u.level == 2) listOf(Position.Transition(u.key) to path(u.key) + "\u0001") else emptyList()
            } + blocks.map { b -> Position.AtBlock(b.key) to path(b.sceneKey) + b.order }
            val bySortKey = positions.sortedWith { a, b -> compareKeys(a.second, b.second) }.map { it.first }
            val byRank = positions.map { it.first }.sortedBy { m.rankOf(it)!!.value }
            assertEquals(bySortKey, byRank)
        }
    }

    private fun compareKeys(a: List<String>, b: List<String>): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val c = a[i].compareTo(b[i])
            if (c != 0) return c
        }
        return a.size.compareTo(b.size)
    }

    @Test
    fun characterLookups() {
        assertEquals("하르트", doc.index.character(HART)?.name)
        assertEquals(listOf(blk(42), blk(43), blk(44), blk(45)), doc.index.blocksOfScene(sc(1, 7, 3)).map { it.key })
        assertEquals(sc(1, 7, 3), doc.manuscript.liveScenes[doc.manuscript.sceneIndex(sc(1, 7, 3))!!].key)
        assertEquals(ep(1, 7), doc.index.episodeGroupOfBlock(blk(43)))
    }
}
