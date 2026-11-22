package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.StoryStructure
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 60-library §4.8 trash cases T-01 … T-15 (T-11 is in [Rule20MergeTest]). */
class LibraryTrashTest {
    private fun rowOf(doc: ProjectDoc, entryKey: String, ctx: TrashContext = Fx.ctx()) =
        TrashListing.projectItemRows(doc, ctx, recorder = Fx.recorder()).single { it.entryKey == entryKey }

    @Test
    fun `T-01 project deleted yesterday`() {
        val key = Fx.key("PRJT", 2)
        val orders = OrderKeys.evenlySpaced(6)
        val glass = ProjectDoc(
            project = Project(key = key, name = "유리 정원", structure = StoryStructure(StructurePreset.CUSTOM.open(), listOf("씬"))),
            story = StoryIndexFile(units = (1..6).map { StoryUnit(key = Fx.key("SCNE", 200 + it), level = 0, order = orders[it - 1]) }),
            cast = CastFile(characters = (1..4).map { Character(key = Fx.key("CHAR", 200 + it), recordNo = it, name = "인물 $it", order = orders[it - 1]) }),
        )
        val library = LibraryIndexFile(projects = listOf(LibraryEntry(key = key, name = "유리 정원")))
        val state = ProjectTrash.delete(ProjectTrashState(library, emptyList()), key, Fx.ctxAt("2026-11-21T10:00"))
        val row = TrashListing.projectRows(state.entries, state.library, mapOf(key to glass), Fx.ctx()).single()
        assertEquals("유리 정원", row.title)
        assertEquals("29일 남음", row.daysLeftLabel)
        assertEquals("프로젝트 · 씬 6개 · 인물 4명", row.sub)
        assertTrue(TrashListing.list(listOf(row), TrashFilter.PROJECT).isNotEmpty())
    }

    @Test
    fun `T-02 four blocks of one scene deleted today`() {
        val ctx = Fx.ctx()
        val blocks = listOf(K.BLOCK_036, K.BLOCK_037, K.BLOCK_038, K.BLOCK_039).map(ItemId::block)
        val result = TrashOps.delete(Fx.doc(), blocks, ctx)
        val entry = result.entries.single()
        val row = rowOf(result.doc, entry.key, ctx)
        assertEquals("1-7 씬 3 · 블록 4개", row.title)
        assertEquals("블록 · 잿빛 새벽의 등불 · 오늘 지움", row.sub)
        assertEquals("30일 남음", row.daysLeftLabel)
        assertEquals(4, row.lines.size)
        assertEquals(listOf(36, 37, 38, 39), row.lines.map { it.number })
    }

    @Test
    fun `T-03 blocks of one scene deleted today and yesterday`() {
        val yesterday = Fx.ctxAt("2026-11-21T10:00")
        val today = Fx.ctx()
        val first = TrashOps.delete(Fx.doc(), listOf(K.BLOCK_036, K.BLOCK_037).map(ItemId::block), yesterday)
        val second = TrashOps.delete(first.doc, listOf(K.BLOCK_038, K.BLOCK_039).map(ItemId::block), today)
        val third = TrashOps.delete(second.doc, listOf(K.BLOCK_040, K.BLOCK_041).map(ItemId::block), today)
        val groups = third.doc.trash.entries.filter { it.kind.known == TrashKind.BLOCK_GROUP && it.groupSceneKey == K.SCENE_1_7_3 }
        assertEquals(2, groups.size)
        val rows = TrashListing.list(TrashListing.projectItemRows(third.doc, today), TrashFilter.BLOCK).filter { it.title.startsWith("1-7 씬 3") }
        assertEquals(listOf("1-7 씬 3 · 블록 4개", "1-7 씬 3 · 블록 2개"), rows.map { it.title })
        assertEquals("블록 · 잿빛 새벽의 등불 · 어제 지움", rows.last().sub)
    }

    @Test
    fun `T-04 an empty block deleted`() {
        val doc = Fx.doc()
        val result = TrashOps.delete(doc, ItemId.block(K.BLOCK_1_8_001), Fx.ctx())
        assertTrue(result.entries.isEmpty())
        assertEquals(listOf(K.BLOCK_1_8_001), result.hardDeleted)
        assertNull(result.doc.index.block(K.BLOCK_1_8_001))
        assertNull(Messages.blockDeleteToast(result))
        assertEquals(doc.trash, result.doc.trash)
        assertEquals(doc.index.unit(K.EPISODE_1_8)!!.nextBlockNumber, result.doc.index.unit(K.EPISODE_1_8)!!.nextBlockNumber)
    }

    @Test
    fun `T-05 scene in the trash`() {
        val row = rowOf(Fx.doc(), K.TRASH_SCENE_1_6_5)
        assertEquals("1-6 씬 5 · 시장의 밤", row.title)
        assertEquals("12일 남음", row.daysLeftLabel)
        assertEquals("씬 · 잿빛 새벽의 등불", row.sub)
        assertTrue(TrashListing.list(listOf(row), TrashFilter.SCENE).isNotEmpty())
    }

    @Test
    fun `T-06 character in the trash`() {
        val row = rowOf(Fx.doc(), K.TRASH_NOAH)
        assertEquals("노아", row.title)
        assertEquals("3일 남음", row.daysLeftLabel)
        assertEquals("인물 · 잿빛 새벽의 등불 · 대사 14 · 관계 1", row.sub)
        assertEquals(listOf("되살리기", "영구 삭제"), row.actions)
    }

    @Test
    fun `T-07 recorder record in the trash`() {
        val row = TrashListing.recorderRows(ExampleRecorder.library(), ExampleRecorder.trashEntries(), Fx.ctx()).single()
        assertEquals("음성 0:12", row.title)
        assertEquals("라이프 레코더", row.sub)
        assertEquals("1일 남음", row.daysLeftLabel)
        assertTrue(TrashListing.list(listOf(row), TrashFilter.RECORDER).isNotEmpty())
    }

    @Test
    fun `T-08 entry purged on its 30th day`() {
        val deleted = TrashOps.delete(Fx.doc(), ItemId.character(K.GORDON), Fx.ctxAt("2026-10-23T10:00"))
        val entry = deleted.entries.single()
        val ctx = Fx.ctx()
        assertEquals(0, ctx.daysLeft(entry.deletedAt))
        assertTrue(ctx.isDue(entry))
        val purged = PurgeOps.purgeDue(deleted.doc, ctx)
        assertEquals(listOf(entry.key), purged.purgedEntries.map { it.key })
        assertNull(purged.doc.index.character(K.GORDON))
        assertEquals("고든", purged.doc.index.nameTag(K.GORDON)!!.name)
        // 노아 (3일 남음) and the other entries stay.
        assertTrue(purged.doc.index.trashEntry(K.TRASH_NOAH) != null)
        assertEquals(4, purged.doc.trash.entries.size)
    }

    @Test
    fun `T-09 filter 세계`() {
        val ctx = Fx.ctx()
        val a = TrashOps.delete(Fx.doc(), listOf(ItemId.term(K.TERM_ORIGINIUM_LANTERN), ItemId.place(K.PLACE_ASH_MARKET)), ctx)
        val rows = TrashListing.projectItemRows(a.doc, ctx)
        assertEquals(setOf("오리지늄 등불", "잿더미 시장"), TrashListing.list(rows, TrashFilter.WORLD).map { it.title }.toSet())
        assertTrue(TrashListing.list(rows, TrashFilter.CHARACTER).none { it.title == "오리지늄 등불" || it.title == "잿더미 시장" })
    }

    @Test
    fun `T-10 filter chips`() {
        val rows = TrashListing.projectItemRows(Fx.doc(), Fx.ctx())
        assertEquals(listOf("전체 4", "프로젝트", "씬", "블록", "인물", "세계", "변화", "레코더"), TrashListing.chips(rows))
        assertEquals(listOf("전체", "프로젝트", "씬", "블록", "인물", "세계", "변화", "레코더"), TrashFilter.entries.map { it.label })
    }

    @Test
    fun `T-12 permanent delete of 노아`() {
        val purged = PurgeOps.purge(Fx.doc(), K.TRASH_NOAH, Fx.ctx())
        val tag = purged.nameTags.single()
        assertEquals("노아", tag.name)
        assertEquals(NameTagKind.CHARACTER, tag.kind.known)
        assertEquals("NOA", tag.voiceCode)
        assertEquals("영구 삭제 · 노아", Resolution.resolve(purged.doc, RefTargetKind.CHARACTER, K.NOAH).label)
        assertEquals("NOA_0106_007", purged.doc.index.block(K.BLOCK_1_6_007)!!.voice!!.fixedId)
    }

    @Test
    fun `T-13 permanent delete of a hidden character`() {
        val ctx = Fx.ctx()
        val base = Fx.doc()
        val doc = base.copy(cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == K.NOAH) it.copy(hidden = true) else it }))
        val purged = PurgeOps.purge(doc, K.TRASH_NOAH, ctx)
        assertTrue(purged.nameTags.single().hidden)
        assertEquals("영구 삭제 · 숨긴 항목", Resolution.resolve(purged.doc, RefTargetKind.CHARACTER, K.NOAH).label)
    }

    @Test
    fun `T-14 restore a block whose scene is in the trash`() {
        val ctx = Fx.ctx()
        val block = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        val scene = TrashOps.delete(block.doc, ItemId.unit(K.SCENE_1_7_3), ctx)
        val outcome = RestoreOps.restore(scene.doc, block.entries.single().key, ctx)
        assertEquals(RestoreOutcome.NeedsSceneChoice(K.SCENE_1_7_3, listOf(K.BLOCK_043)), outcome)
        assertEquals(listOf("씬도 되살리기", "다른 씬 끝에"), listOf(TrashCopy.SCENE_TOO, TrashCopy.OTHER_SCENE_END))
        // 씬도 되살리기: the scene and the block both come back, #043 in its place.
        val both = Fx.restored(RestoreOps.restore(scene.doc, block.entries.single().key, ctx, sceneChoice = SceneChoice.RestoreScene)).doc
        assertTrue(both.liveness.isUnitLive(K.SCENE_1_7_3))
        val live = both.manuscript.liveBlocks.map { it.key }
        assertEquals(live.indexOf(K.BLOCK_042) + 1, live.indexOf(K.BLOCK_043))
        assertTrue(both.trash.entries.none { it.key == scene.entries.single().key })
    }

    @Test
    fun `T-15 restore block #043 with a fixed voice ID`() {
        val ctx = Fx.ctx()
        val doc = Fx.fixVoice(Fx.doc(), K.BLOCK_043, "HRT_0107_043")
        val deleted = TrashOps.delete(doc, ItemId.block(K.BLOCK_043), ctx)
        assertEquals(listOf("HRT_0107_043"), deleted.discardedVoiceIds.map { it.id })
        assertEquals(DiscardReason.DELETED, deleted.discardedVoiceIds.single().reason.known)
        val orphan = deleted.doc.index.record(K.R4)!!
        assertEquals(AnchorState.ORPHANED, orphan.anchorState.known)
        assertEquals(Anchor.AtBlock(K.BLOCK_044), orphan.anchor)
        val back = Fx.restored(RestoreOps.restore(deleted.doc, deleted.entries.single().key, ctx))
        val block = back.doc.index.block(K.BLOCK_043)!!
        assertEquals(43, block.number)
        assertEquals(VoiceMode.FIXED, block.voice!!.mode.known)
        assertEquals("HRT_0107_043", block.voice!!.fixedId)
        assertTrue(back.doc.index.allDiscardedVoiceIds.none { it.blockKey == K.BLOCK_043 })
        for (key in listOf(K.R4, K.R5, K.R5A)) {
            val record = back.doc.index.record(key)!!
            assertEquals(Anchor.AtBlock(K.BLOCK_043), record.anchor)
            assertEquals(AnchorState.NORMAL, record.anchorState.known)
            assertNull(record.originalAnchor)
        }
        assertEquals(setOf(K.R4, K.R5, K.R5A), back.report.reattached.toSet())
    }
}
