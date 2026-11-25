package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.FixtureR.blk
import io.github.lenticualrstone.civilight.model.FixtureR.ep
import io.github.lenticualrstone.civilight.model.FixtureR.sc
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.CardStyle
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportRecord
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.app.ShownView
import io.github.lenticualrstone.civilight.model.app.Tilt
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import io.github.lenticualrstone.civilight.model.cast.VoiceLine
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.game.GameData
import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.model.game.GameToolsUpgrade
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.marks.Foreshadow
import io.github.lenticualrstone.civilight.model.marks.MarkAnchor
import io.github.lenticualrstone.civilight.model.marks.MarkRole
import io.github.lenticualrstone.civilight.model.marks.MarkState
import io.github.lenticualrstone.civilight.model.marks.Marks
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.BlockTodo
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.ScriptAsset
import io.github.lenticualrstone.civilight.model.script.StagePosition
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.version.EpisodeVersion
import io.github.lenticualrstone.civilight.model.version.EpisodeVersionsFile
import io.github.lenticualrstone.civilight.model.version.VersionBlock
import io.github.lenticualrstone.civilight.model.version.VersionOrigin
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.0 model rules (00 §13): G-3 game data, view settings, marks, versions, additive encoding. */
class Model020Test {
    private fun base(): ProjectDoc = ProjectDoc(
        project = Project(FixtureR.PROJECT, "등불"),
        story = StoryIndexFile(units = FixtureR.units()),
        episodes = FixtureR.episodesWithBlocks(),
    )

    private fun ProjectDoc.editBlock(key: String, f: (Block) -> Block) =
        copy(episodes = episodes.mapValues { (_, e) -> e.copy(blocks = e.blocks.map { if (it.key == key) f(it) else it }) })

    private fun ProjectDoc.trashBlock(key: String) = editBlock(key) { it.copy(deletedAt = 5L, trashEntryKey = "TE") }

    // ---- DM-0.2-01 GameData.present ----

    @Test
    fun `game data present follows D6`() {
        val doc = base()
        assertFalse("empty project", GameData.present(doc))
        val fixed = doc.editBlock(blk(43)) { it.copy(voice = VoiceState(mode = VoiceMode.FIXED.open(), fixedId = "HRT_0107_043")) }
        assertTrue("fixed voice ID", GameData.present(fixed))
        assertTrue("fixed voice ID on a trashed block", GameData.present(fixed.trashBlock(blk(43))))
        val auto = doc.editBlock(blk(43)) { it.copy(voice = VoiceState()) }
        assertFalse("auto IDs alone", GameData.present(auto))
        assertTrue("manual ID", GameData.present(doc.editBlock(blk(43)) { it.copy(voice = VoiceState(manualId = "X")) }))
        assertTrue("stage", GameData.present(doc.editBlock(blk(43)) { it.copy(stage = StagePosition.LEFT.open()) }))
        val direction = doc.editBlock(blk(44)) {
            it.copy(kind = BlockKind.DIRECTION.open(), cues = listOf(DirectionCue("CUE", CueKind.SCREEN_EFFECT.open(), effect = "fade")))
        }
        assertTrue("direction cue", GameData.present(direction))
        val discarded = doc.copy(
            episodes = doc.episodes + (ep(1, 7) to doc.episodes.getValue(ep(1, 7)).copy(
                discardedVoiceIds = listOf(DiscardedVoiceId("HRT_1", blk(43), discardedAt = 1L, reason = DiscardReason.DELETED.open())),
            )),
        )
        assertTrue("discarded ID", GameData.present(discarded))
        val asset = doc.copy(assets = doc.assets.copy(scriptAssets = listOf(ScriptAsset(key = "A1", kind = ScriptAssetKind.BGM.open(), name = "잔불", fileName = "bgm"))))
        assertTrue("script asset", GameData.present(asset))
        val line = doc.copy(cast = doc.cast.copy(voiceLines = listOf(VoiceLine("VL", FixtureR.RIEN, "V", "인사"))))
        assertTrue("voice line", GameData.present(line))
        assertTrue("CSV export record", GameData.present(doc, listOf(ExportRecord(1L, ExportFormat.CSV))))
        assertFalse("PDF export record", GameData.present(doc, listOf(ExportRecord(1L, ExportFormat.PDF))))
        assertTrue("the example project", GameData.present(ExampleProject.build()))
    }

    @Test
    fun `the upgrade step runs only while the switch is absent`() {
        val fixed = base().editBlock(blk(43)) { it.copy(voice = VoiceState(mode = VoiceMode.FIXED.open(), fixedId = "A")) }
        assertTrue(GameToolsUpgrade.needed(fixed))
        assertEquals(true, GameToolsUpgrade.resolve(fixed).project.gameTools)
        assertEquals(false, GameToolsUpgrade.resolve(base()).project.gameTools)
        val off = fixed.copy(project = fixed.project.copy(gameTools = false))
        assertTrue("an explicit off stays off", GameToolsUpgrade.resolve(off) === off)
        assertFalse(GameTools.on(off))
        assertFalse("absent reads off", GameTools.on(fixed))
    }

    // ---- additive encoding ----

    @Test
    fun `new fields are not written while at their defaults`() {
        val project = CivilightJson.encodeElement(Project.serializer(), Project("P", "x")).jsonObject
        assertFalse("gameTools absent until decided", "gameTools" in project)
        assertFalse("termCandidates default", "termCandidates" in project)
        assertEquals(false, CivilightJson.encodeElement(Project.serializer(), Project("P", "x", gameTools = false)).jsonObject["gameTools"]?.toString()?.toBoolean())
        val story = CivilightJson.encodeElement(StoryIndexFile.serializer(), StoryIndexFile(units = FixtureR.units())).jsonObject
        assertFalse("foreshadows" in story)
        val block = CivilightJson.encodeElement(Block.serializer(), base().index.block(blk(43))!!).jsonObject
        assertFalse("displayNameOverride" in block)
        assertFalse("todo" in block)
        val settings = CivilightJson.encodeElement(AppSettings.serializer(), AppSettings()).jsonObject
        assertEquals("defaults write nothing", 0, settings.size)
    }

    @Test
    fun `direction text counts as content`() {
        val b = base().index.block(blk(44))!!.copy(kind = BlockKind.DIRECTION.open(), runs = listOf(TextRun.Plain("배경 하층 거리")))
        assertTrue(b.hasContent())
        assertFalse(b.copy(runs = emptyList()).hasContent())
    }

    // ---- DM-0.2-05, DM-0.2-06 ----

    @Test
    fun `shown view and toggle`() {
        assertEquals(LibraryStyle.CD_JEWEL_CASE, ShownView.of(LibraryStyle.CD_JEWEL_CASE, null))
        assertEquals(LibraryStyle.LIST, ShownView.of(LibraryStyle.CD_JEWEL_CASE, ViewMode.LIST))
        assertEquals(LibraryStyle.LIST, ShownView.of(LibraryStyle.LIST, ViewMode.CARD))
        assertFalse(ShownView.showsToggle(LibraryStyle.LIST))
        assertEquals(CardStyle.TAROT, ShownView.of(CardStyle.TAROT, ViewMode.GRID))
        val unknown = CivilightJson.decode(AppSettings.serializer(), """{"libraryStyle":"hologram","castCardStyle":"x"}""")
        assertEquals(LibraryStyle.CD_JEWEL_CASE, unknown.libraryStyle)
        assertEquals(CardStyle.FAN_DECK, unknown.castCardStyle)
        val old = CivilightJson.decode(AppSettings.serializer(), """{"viewByScreen":{"library":"list","cast":"card"}}""")
        assertEquals(LibraryStyle.CD_JEWEL_CASE, ShownView.library(old))
        assertEquals(CardStyle.FAN_DECK, ShownView.cast(old))
        assertEquals(mapOf("library" to ViewMode.LIST, "cast" to ViewMode.CARD), old.viewByScreen)
        assertFalse("S-2: travel check off by default", old.checkTravel)
    }

    @Test
    fun `tilt follows only with motion`() {
        assertFalse(Tilt.follows(true, AnimationPref.FOLLOW_SYSTEM, systemReduceMotion = true))
        assertTrue(Tilt.follows(true, AnimationPref.FOLLOW_SYSTEM, systemReduceMotion = false))
        assertFalse(Tilt.follows(true, AnimationPref.ALWAYS_REDUCE, systemReduceMotion = false))
        assertTrue(Tilt.follows(true, AnimationPref.NEVER_REDUCE, systemReduceMotion = true))
        for (a in AnimationPref.entries) for (r in listOf(true, false)) assertFalse(Tilt.follows(false, a, r))
    }

    @Test
    fun `a project opens at the latest point and keeps the stored lens`() {
        val stored = io.github.lenticualrstone.civilight.model.app.ProjectAppState(
            view = io.github.lenticualrstone.civilight.model.app.ProjectViewFile(
                lens = io.github.lenticualrstone.civilight.model.app.LensState(point = io.github.lenticualrstone.civilight.model.position.LensPoint.UnitEnd(ep(1, 7))),
            ),
        )
        val open = io.github.lenticualrstone.civilight.model.app.LensOnOpen.open(stored)
        assertEquals(io.github.lenticualrstone.civilight.model.position.LensPoint.Latest, open.view.lens.point)
        assertEquals("written back unchanged while at 최신", stored.view, io.github.lenticualrstone.civilight.model.app.LensOnOpen.toStore(open))
        val picked = open.copy(view = open.view.copy(lens = io.github.lenticualrstone.civilight.model.app.LensState(point = io.github.lenticualrstone.civilight.model.position.LensPoint.UnitEnd(ep(1, 3)))))
        assertEquals(picked.view, io.github.lenticualrstone.civilight.model.app.LensOnOpen.toStore(picked))
        val latest = io.github.lenticualrstone.civilight.model.app.ProjectAppState()
        assertTrue(io.github.lenticualrstone.civilight.model.app.LensOnOpen.open(latest) === latest)
    }

    // ---- N-41 marks ----

    private fun withMarks(): ProjectDoc {
        val doc = base().editBlock("B1-7-1") { it.copy(kind = BlockKind.MEMO.open(), todo = BlockTodo()) }
            .editBlock("B1-7-2") { it.copy(kind = BlockKind.MEMO.open(), todo = BlockTodo(done = true)) }
        val f1 = Foreshadow("F1", "불빛", plant = MarkAnchor(blk(43)), mentions = listOf(MarkAnchor(blk(44))))
        val f2 = Foreshadow("F2", "등불", plant = MarkAnchor(blk(42)), payoff = MarkAnchor(blk(45)))
        val f3 = Foreshadow("F3", "지운 것", plant = MarkAnchor(blk(42)), deletedAt = 3L, trashEntryKey = "TE3")
        return doc.copy(story = doc.story.copy(foreshadows = listOf(f1, f2, f3)))
    }

    @Test
    fun `filter counts and states`() {
        val doc = withMarks()
        assertEquals(mapOf(MarkState.OPEN to 2, MarkState.PAID_OFF to 1, MarkState.DONE to 1), Marks.counts(doc))
        assertEquals(listOf("F1", "B1-7-1"), Marks.filter(doc, MarkState.OPEN).map { it.key })
        // A to-do goes with its memo block.
        assertEquals(1, Marks.counts(doc.trashBlock("B1-7-1"))[MarkState.OPEN])
    }

    @Test
    fun `a mark on a deleted block moves to the next block and comes back on restore`() {
        val doc = withMarks()
        val deleted = doc.trashBlock(blk(43))
        val r = Marks.resolve(deleted, MarkAnchor(blk(43)))
        assertEquals(blk(44), r.blockKey)
        assertTrue(r.moved)
        assertEquals(listOf(MarkRole.PLANT to true, MarkRole.MENTION to false), Marks.on(deleted, blk(44)).map { it.role to it.moved })
        assertEquals("stored key unchanged", blk(43), deleted.story.foreshadows.first().plant.blockKey)
        assertEquals(blk(43), Marks.resolve(doc, MarkAnchor(blk(43))).blockKey)
        // Nothing live after the last block → unplaced.
        assertNull(Marks.resolve(doc.trashBlock(blk(45)), MarkAnchor(blk(45))).blockKey)
        // Purge pins the anchor to where it was drawn; a merge moves it to the kept block.
        val settled = Marks.settleBeforePurge(deleted, setOf(blk(43)))
        assertEquals(blk(44), settled.story.foreshadows.first().plant.blockKey)
        assertEquals(blk(42), Marks.moveAnchors(doc, blk(43), blk(42)).story.foreshadows.first().plant.blockKey)
        assertTrue(Marks.carriesMark(doc, blk(44)))
    }

    // ---- files: marks and versions round-trip, trash mirror ----

    @Test
    fun `foreshadows and versions survive the file split`() {
        val version = EpisodeVersion(
            key = "V1", name = "11월 23일 · 초고", origin = VersionOrigin.EXPORT.open(), createdAt = 9L,
            blocks = listOf(VersionBlock(blk(43), sc(1, 7, 3), 43, BlockKind.DIALOGUE.open(), FixtureR.HART, listOf(TextRun.Plain("멈춰.")))),
        )
        val doc = withMarks().copy(versions = mapOf(ep(1, 7) to EpisodeVersionsFile(ep(1, 7), listOf(version))))
        val set = ProjectFileSet.split(doc)
        assertEquals(listOf("F1", "F2"), set.live.storyIndex.foreshadows.map { it.key })
        assertEquals(listOf("F3"), set.trash!!.units.storyIndex.foreshadows.map { it.key })
        val files = set.encode()
        assertTrue("versions/${ep(1, 7)}.json" in files)
        assertTrue("trash/story/index.json" in files)
        val back = ProjectFileSet.decode(files)
        assertEquals(emptyList<String>(), back.unknownPaths)
        val again = back.files.toDoc()
        assertEquals(doc.story.foreshadows.map { it.key }.toSet(), again.story.foreshadows.map { it.key }.toSet())
        assertEquals(doc.versions, again.versions)
        assertEquals(doc.allBlocks.toSet(), again.allBlocks.toSet())
    }

    @Test
    fun `a 0_1_0 shaped project writes no new file`() {
        val files = ProjectFileSet.split(base()).encode()
        assertTrue(files.keys.none { it.startsWith("versions/") })
        assertFalse("foreshadows" in files.getValue("story/index.json").jsonObject)
    }
}
