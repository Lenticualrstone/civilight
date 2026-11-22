package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormat
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.TrashPlacement
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 10-script.md §4.2 · rule 12 (blocks and change records), on the F1 overlay of the example project. */
class Rule12BlocksTest {
    private val ctx = testCtx()
    private fun b(n: Int) = F1.b(n)

    private fun deleted43() = BlockTrash.delete(F1.build(), listOf(b(43)), ctx)

    @Test
    fun `T-12-01 deleting an anchor block relocates the record`() {
        val r = deleted43()
        val b43 = r.doc.block(b(43))
        assertNotNull(b43.deletedAt)
        assertEquals(TrashPlacement(prevKey = b(42), nextKey = b(44), ordinal = 7), b43.trashPlacement)
        val r1 = r.doc.record(F1.R1)
        assertEquals(Anchor.AtBlock(b(44)), r1.anchor)
        assertEquals(AnchorState.ORPHANED, r1.anchorState.known)
        assertEquals(Anchor.AtBlock(b(43)), r1.originalAnchor)
        assertTrue(Records.marksOn(r.doc, b(44)).single { it.recordKey == F1.R1 }.dotted)
        assertEquals("블록 #043 지움 · ◆ 1개는 #044 자리로", r.toast)
        assertEquals(Toasts.UNDO, "되돌리기")
        assertEquals(listOf("◇ 근거 지워진 변화 1"), ScriptChecks.anchorDeleted(r.doc).map { it.title })
        assertTrue(r.discarded.isEmpty())
        assertEquals(F1.build().index.allDiscardedVoiceIds, r.doc.index.allDiscardedVoiceIds)
        val entry = r.doc.trash.entries.single { b(43) in it.targetKeys }
        assertEquals(TrashKind.BLOCK_GROUP, entry.kind.known)
        assertEquals(F1.S3, entry.groupSceneKey)
    }

    private fun relinked(): Triple<io.github.lenticualrstone.civilight.model.doc.ProjectDoc, String, List<String>> {
        val ins = BlockOps.insert(deleted43().doc, BlockPosition.After(b(42)), BlockKind.DIALOGUE, ctx, speakerKey = K.HART)
        return Triple(ins.doc, ins.blockKey, Anchors.relinkCandidates(ins.doc, F1.R1).map { it.key })
    }

    @Test
    fun `T-12-02 re-anchoring to a rewritten line takes two taps`() {
        val (doc, newKey, candidates) = relinked()
        assertEquals(58, doc.block(newKey).number)
        assertEquals(59, doc.unit(F1.E17).nextBlockNumber)
        val numbers = doc.liveNumbers(F1.S3)
        assertEquals(listOf(42, 58, 44), numbers.subList(numbers.indexOf(42), numbers.indexOf(42) + 3))
        assertEquals(newKey, candidates.first())
        val after = Anchors.anchorToBlock(doc, F1.R1, newKey, ctx)
        val r1 = after.record(F1.R1)
        assertEquals(Anchor.AtBlock(newKey), r1.anchor)
        assertNull(r1.originalAnchor)
        assertEquals(AnchorState.NORMAL, r1.anchorState.known)
        assertFalse(Records.marksOn(after, newKey).single { it.recordKey == F1.R1 }.dotted)
    }

    @Test
    fun `T-12-03 undo re-attaches`() {
        val r = deleted43()
        val undone = BlockTrash.undoDelete(r.doc, r, ctx)
        val numbers = undone.liveNumbers(F1.S3)
        assertEquals(listOf(42, 43, 44), numbers.subList(numbers.indexOf(42), numbers.indexOf(42) + 3))
        val r1 = undone.record(F1.R1)
        assertEquals(Anchor.AtBlock(b(43)), r1.anchor)
        assertNull(r1.originalAnchor)
        assertEquals(AnchorState.NORMAL, r1.anchorState.known)
        assertTrue(ScriptChecks.anchorDeleted(undone).isEmpty())
        assertTrue(undone.trash.entries.none { b(43) in it.targetKeys })
    }

    @Test
    fun `T-12-04 restoring keeps a choice the user made`() {
        val (doc, newKey, _) = relinked()
        val chosen = Anchors.anchorToBlock(doc, F1.R1, newKey, ctx)
        val restored = (BlockTrash.restore(chosen, b(43), ctx) as BlockTrash.RestoreResult.Restored).doc
        assertEquals(Anchor.AtBlock(newKey), restored.record(F1.R1).anchor)
        assertTrue(restored.manuscript.isLiveBlock(b(43)))
    }

    @Test
    fun `T-12-05 기록 지우기`() {
        val doc = Anchors.trashRecord(deleted43().doc, F1.R1, ctx)
        val r1 = doc.record(F1.R1)
        assertNotNull(r1.deletedAt)
        val entry = doc.trash.entries.single { F1.R1 in it.targetKeys }
        assertEquals(TrashKind.CHANGE, entry.kind.known)
        assertTrue(Records.marksOn(doc, b(44)).none { it.glyph == "◆" })
        assertTrue(ScriptChecks.anchorDeleted(doc).isEmpty())
    }

    @Test
    fun `T-12-06a relocation goes to the first block of the next scene`() {
        val r6 = "01JZ000000CHGR000000000096"
        val doc = F1.build().withRecords(record(r6, Anchor.AtBlock(b(48))))
        val after = BlockTrash.delete(doc, listOf(b(48)), ctx).doc
        assertEquals(Anchor.AtBlock(b(49)), after.record(r6).anchor)
    }

    @Test
    fun `T-12-06b relocation goes to the first block of the next episode`() {
        val r7 = "01JZ000000CHGR000000000097"
        val doc = F1.build().withRecords(record(r7, Anchor.AtBlock(b(53))))
        val after = BlockTrash.delete(doc, listOf(b(53)), ctx).doc
        assertEquals(Anchor.AtBlock(K.BLOCK_1_8_001), after.record(r7).anchor)
    }

    @Test
    fun `T-12-06c no surviving block makes the record planned`() {
        val r8 = "01JZ000000CHGR000000000098"
        val last = F2.blockAt(48)
        val doc = F2.build().withRecords(record(r8, Anchor.AtBlock(last), ChangeTarget.DisplayName(F2.HART)))
        val after = BlockTrash.delete(doc, listOf(last), ctx).doc
        val r = after.record(r8)
        assertEquals(Anchor.Unplaced, r.anchor)
        assertTrue(after.isPlanned(r))
        assertEquals(Anchor.AtBlock(last), r.originalAnchor)
        assertEquals("블록 #058 지움 · ◆ 1개는 예정으로", BlockTrash.delete(doc, listOf(last), ctx).toast)
    }

    @Test
    fun `T-12-07 deleting a scene`() {
        val d = SceneLifecycle.delete(F1.build(), F1.S3, ctx)!!
        val doc = d.doc
        assertNotNull(doc.unit(F1.S3).deletedAt)
        assertEquals(TrashKind.SCENE, doc.trash.entries.single { F1.S3 in it.targetKeys }.kind.known)
        for (key in listOf(F1.R1, F1.R4, F1.R5, F1.REVEAL)) {
            val r = doc.record(key)
            assertEquals(Anchor.AtBlock(b(49)), r.anchor)
            assertNotNull(r.originalAnchor)
            assertEquals(AnchorState.ORPHANED, r.anchorState.known)
        }
        assertEquals("씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로", d.toast)
        assertEquals(3, Labels.sceneOrdinal(doc, F1.S4))
        val t = doc.unit(F1.S4).time as io.github.lenticualrstone.civilight.model.story.SceneTime.Relative
        assertEquals(F1.S2, t.baseKey)
        assertEquals(1 to 1, t.dayMin to t.dayMax)
        assertEquals("06:10", t.clock.toString())
        assertEquals(F1.S3, t.rebasedFromKey)
        assertEquals("2일째 아침 06:10", SceneText.headTime(StoryTimes.source(doc).timeOf(F1.S4)))
        assertEquals(StoryTimes.source(F1.build()).timeOf(F1.S4), StoryTimes.source(doc).timeOf(F1.S4))
    }

    @Test
    fun `T-12-08 backspace merge moves records without flagging`() {
        val r = BlockOps.backspace(F1.build(), b(43), atStart = true, ctx) as BlockOps.BackspaceResult.Merged
        val b42 = r.doc.block(b(42))
        assertEquals("(작게) 저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요? 멈추지 마. 저건 등불이 아니야.", Runs.displayText(b42.runs))
        assertEquals(42, b42.number)
        assertEquals(K.RIEN, b42.speakerKey)
        assertNull(r.doc.index.block(b(43)))
        assertTrue(r.doc.trash.entries.none { b(43) in it.targetKeys })
        val r1 = r.doc.record(F1.R1)
        assertEquals(Anchor.AtBlock(b(42)), r1.anchor)
        assertNull(r1.originalAnchor)
        assertEquals(AnchorState.NORMAL, r1.anchorState.known)
        assertEquals("◆ 1개 #042로", r.toast)
    }

    @Test
    fun `T-12-08 variant fixed voice of the later block is discarded`() {
        val doc = F1.build().withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }
        val r = BlockOps.backspace(doc, b(43), atStart = true, ctx)
        val d = r.doc.index.allDiscardedVoiceIds.single { it.id == "HRT_0107_043" }
        assertEquals(DiscardReason.MERGED, d.reason.known)
    }

    @Test
    fun `T-12-09 split keeps records on the front block`() {
        val r6 = "01JZ000000CHGR000000000096"
        val doc = F1.build().withRecords(record(r6, Anchor.AtBlock(b(47))))
        val r = BlockOps.enter(doc, b(47), "뒤돌아보지 말고".length, ctx) as BlockOps.EnterResult.Split
        assertEquals("뒤돌아보지 말고", Runs.displayText(r.doc.block(b(47)).runs))
        assertEquals(Anchor.AtBlock(b(47)), r.doc.record(r6).anchor)
        val nb = r.doc.block(r.newBlockKey)
        assertEquals(58, nb.number)
        assertEquals(K.HART, nb.speakerKey)
        assertEquals("따라와.", Runs.displayText(nb.runs))
        val numbers = r.doc.liveNumbers(F1.S3)
        assertEquals(numbers.indexOf(47) + 1, numbers.indexOf(58))
        assertEquals("HRT_0107_058 · 자동", VoiceIds.stateLabel(r.doc, nb, ctx.zone))
    }

    @Test
    fun `T-12-10 copy does not copy records`() {
        val doc = F1.build()
        val r = BlockMove.paste(doc, BlockMove.copy(doc, listOf(b(43))), BlockPosition.After(b(48)), ctx)
        val copy = r.doc.block(r.blockKeys.single())
        assertEquals(58, copy.number)
        assertEquals(K.HART, copy.speakerKey)
        assertEquals(doc.block(b(43)).runs, copy.runs)
        assertEquals("HRT_0107_058 · 자동", VoiceIds.stateLabel(r.doc, copy, ctx.zone))
        assertTrue(r.doc.recordsAnchoredOn(copy.key).isEmpty())
        assertEquals(Anchor.AtBlock(b(43)), r.doc.record(F1.R1).anchor)
        assertEquals("◆는 복사되지 않음", r.toast)
    }

    @Test
    fun `T-12-11 move to another scene`() {
        val r = BlockMove.move(F1.build(), listOf(b(43)), BlockPosition.After(b(50)), ctx)
        val moved = r.doc.block(b(43))
        assertEquals(F1.S4, moved.sceneKey)
        assertEquals(43, moved.number)
        assertEquals("HRT_0107_043", VoiceIds.currentId(r.doc, moved))
        assertEquals(Anchor.AtBlock(b(43)), r.doc.record(F1.R1).anchor)
        assertEquals("◆ 1개 씬 3 → 씬 4", r.toast)
    }

    @Test
    fun `T-12-12 move to another episode, automatic voice`() {
        val r = BlockMove.move(F1.build(), listOf(b(43)), BlockPosition.SceneStart(F1.S18_1), ctx)
        val moved = r.doc.block(b(43))
        assertEquals(61, moved.number)
        assertEquals(62, r.doc.unit(F1.E18).nextBlockNumber)
        assertEquals("HRT_0108_061 · 자동", VoiceIds.stateLabel(r.doc, moved, ctx.zone))
        assertEquals("◆ 1개 1-7 → 1-8", r.toast)
        assertEquals(F1.E18, r.doc.index.episodeGroupOfBlock(b(43)))
        assertEquals(b(43), r.doc.manuscript.liveBlocksOf(F1.S18_1).first().key)
    }

    @Test
    fun `T-12-13 move to another episode, fixed voice`() {
        val doc = F1.build().withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }
        val r = BlockMove.move(doc, listOf(b(43)), BlockPosition.SceneStart(F1.S18_1), ctx)
        val moved = r.doc.block(b(43))
        assertEquals(61, moved.number)
        assertEquals("HRT_0107_043", VoiceIds.currentId(r.doc, moved))
        val item = ScriptChecks.voiceFormatDiffers(r.doc).single()
        assertEquals("보이스", item.filter)
        assertEquals(listOf("형식과 다른 ID 1"), item.details)
    }

    @Test
    fun `T-12-14 moving the first reveal behind its uses`() {
        val doc = F1.build()
            .withBlock(b(47)) { it.copy(runs = it.runs + TextRun.Plain(" ") + F1.lanternLink("01JZ000000HREF000000000047")) }
            .withBlock(b(48)) { it.copy(runs = it.runs + TextRun.Plain(" ") + F1.lanternLink("01JZ000000HREF000000000048")) }
        val r = BlockMove.move(doc, listOf(b(44)), BlockPosition.SceneStart(F1.S18_1), ctx)
        assertTrue(r.toast!!.contains("◇ 첫 공개 1-7 → 1-8 · ▲ 새로 2"))
    }

    @Test
    fun `T-12-15 scene-move preview`() {
        var doc = F1.build()
        for (n in listOf(42, 43, 45, 47)) doc = doc.withBlock(b(n)) { it.copy(voice = fixed(VoiceIds.autoId(doc, it)!!)) }
        val p = SceneLifecycle.movePreview(doc, F1.S3, F1.E18, afterSiblingKey = null, ctx = ctx)!!
        assertEquals("씬 옮기기", p.title)
        assertEquals("미리 보기 · 옮기기 전에는 그대로", p.sub)
        assertEquals("1-7 · 씬 3" to "꺼진 등불", p.from)
        assertEquals("1-8" to "맨 앞 · 씬 1", p.to)
        assertEquals(
            listOf(
                "기록 · ◆ 3 · ◇ 1 · 시점 1-7 → 1-8",
                "블록 번호 · 블록 13개 · 1-8 새 번호",
                "보이스 ID · 고정 4개 그대로 · ◇ 에피소드 부분 다름",
                "때 · 그대로 · ◇ 1 순서와 어긋남",
            ),
            p.rows,
        )
        assertEquals(listOf("취소", "옮기기"), p.buttons)
        val moved = SceneLifecycle.move(doc, F1.S3, F1.E18, null, ctx)
        assertEquals((61..73).toList(), moved.liveNumbers(F1.S3))
        val timeBefore = StoryTimes.source(doc).timeOf(F1.S3)!!
        val timeAfter = StoryTimes.source(moved).timeOf(F1.S3)!!
        assertEquals(timeBefore.storyDay to timeBefore.clock, timeAfter.storyDay to timeAfter.clock)
        assertEquals(1, StoryTimes.orderMismatches(moved))
        assertEquals(F1.S3, moved.manuscript.liveScenes.first { moved.numberingUnitOfScene(it.key) == F1.E18 }.key)
    }

    @Test
    fun `T-12-16 choice blocks`() {
        val doc = F3.build()
        val choice = doc.block(b(45))
        val options = BlockOps.orderedOptions(choice)
        assertEquals(listOf("#045-1", "#045-2"), options.indices.map { Labels.optionNumber(choice.number, it + 1) })
        assertEquals("HRT_0107_045_1", VoiceIds.currentIdOfOption(doc, choice, options[0]))
        assertEquals(b(46), GameJsonExport.rejoinOf(doc, choice))
        val created = Records.createAtBlock(doc, b(45), ChangeTarget.DisplayName(K.SERA), null, ctx)!!
        assertEquals(Anchor.AtBlock(b(45)), created.doc.record(created.recordKey).anchor)
    }

    @Test
    fun `T-12-17 renumber preview and apply`() {
        val doc = F2.build()
        val p = Numbering.renumberPreview(doc, F2.episode(7))
        assertEquals("번호 다시 매기기 · 1-7", p.title)
        assertEquals("원고 순서대로 #001부터", p.subtitle)
        assertEquals(listOf("48" to "블록", "7" to "바뀌는 번호", "41" to "그대로"), p.figures)
        val rows = p.rows.associate { it.change to it.where }
        assertEquals("씬 1 · 나레이션", rows["#056 → #020"])
        assertEquals("씬 2 · 나레이션", rows["#057 → #031"])
        assertEquals("씬 4 · 하르트", rows["#051 → #045"])
        assertEquals("씬 4 · 하르트", rows["#058 → #048"])
        assertEquals("보이스 HRT_0107_051 · 고정 · 그대로", p.rows.single { it.oldNumber == 51 }.voiceLine)
        assertEquals("보이스 HRT_0107_058 · 고정 · 그대로", p.rows.single { it.oldNumber == 58 }.voiceLine)
        assertEquals(listOf("고정 보이스 ID 4개는 그대로", "번호만 다름 · ◇ 없음 · 변화 기록 · 블록 ID 그대로"), p.footer)
        assertEquals(listOf("취소", "다시 매기기"), p.buttons)

        val after = Numbering.renumber(doc, F2.episode(7), ctx)
        for (pos in 1..48) assertEquals(pos, after.block(F2.blockAt(pos)).number)
        assertEquals(doc.changes, after.changes)
        assertEquals("HRT_0107_051", VoiceIds.currentId(after, after.block(F2.blockAt(45))))
        assertEquals("HRT_0107_058", VoiceIds.currentId(after, after.block(F2.blockAt(48))))
        assertTrue(ScriptChecks.voiceFormatDiffers(after).isEmpty())
        assertEquals("RIE_0107_038", VoiceIds.currentId(after, after.block(F2.blockAt(38))))
        assertEquals(59, after.unit(F2.episode(7)).nextBlockNumber)
    }

    @Ignore("spec conflict: T-12-17 lists #056, #057, #051, #058 one by one and #055, #052, #053 in the 외 line; no stated rule (manuscript order, number change, fixed IDs) yields that split. Implemented: the first 4 changed rows in manuscript order.")
    @Test
    fun `T-12-17 renumber rows as drawn on the board`() {
        val p = Numbering.renumberPreview(F2.build(), F2.episode(7))
        assertEquals(listOf("#056 → #020", "#057 → #031", "#051 → #045", "#058 → #048"), p.listedRows.map { it.change })
        assertEquals("외 3개 · #055 → #038 · #052 → #046 고정 · #053 → #047 고정", p.restLine)
    }

    @Test
    fun `T-12-17 renumber rows follow manuscript order (implemented rule)`() {
        val p = Numbering.renumberPreview(F2.build(), F2.episode(7))
        assertEquals(listOf("#056 → #020", "#057 → #031", "#055 → #038", "#051 → #045"), p.listedRows.map { it.change })
        assertEquals("외 3개 · #052 → #046 고정 · #053 → #047 고정 · #058 → #048 고정", p.restLine)
        assertEquals("바뀌는 번호 · 7", p.sectionTitle)
    }

    @Test
    fun `T-12-18 voice ID events`() {
        val base = F1.build()
        // Speaker changed, automatic.
        val a = BlockOps.changeSpeaker(base, b(43), K.RIEN, ctx)
        assertEquals("RIE_0107_043 · 자동", VoiceIds.stateLabel(a, a.block(b(43)), ctx.zone))
        // Speaker changed, fixed; then 새 ID로.
        val fixedDoc = base.withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }
        val f = BlockOps.changeSpeaker(fixedDoc, b(43), K.RIEN, ctx)
        assertEquals("HRT_0107_043", VoiceIds.currentId(f, f.block(b(43))))
        assertEquals(listOf("형식과 다른 ID 1"), ScriptChecks.voiceFormatDiffers(f).single().details)
        val replaced = VoiceIds.replaceWithNew(f, b(43), ctx = ctx)
        assertEquals("RIE_0107_043 · 자동", VoiceIds.stateLabel(replaced, replaced.block(b(43)), ctx.zone))
        assertEquals(DiscardReason.REPLACED, replaced.index.allDiscardedVoiceIds.single { it.id == "HRT_0107_043" }.reason.known)
        // Deleted.
        val del = BlockTrash.delete(fixedDoc, listOf(b(43)), ctx).doc
        assertEquals(DiscardReason.DELETED, del.index.allDiscardedVoiceIds.single { it.id == "HRT_0107_043" }.reason.known)
        // Kind change dialogue → narration.
        val kind = BlockOps.changeKind(fixedDoc, b(43), BlockKind.NARRATION, ctx)
        assertEquals(DiscardReason.KIND_CHANGED, kind.index.allDiscardedVoiceIds.single { it.id == "HRT_0107_043" }.reason.known)
        assertEquals(Anchor.AtBlock(b(43)), kind.record(F1.R1).anchor)
        // Export fixes.
        assertEquals("HRT_0107_043 · 자동", VoiceIds.stateLabel(base, base.block(b(43)), ctx.zone))
        val at = ExampleClock.at("2026-11-20T03:00:00Z")
        val exported = GameJsonExport.export(base, ExportScope(listOf(F1.S3)), F1.time, at).doc
        val voice = exported.block(b(43)).voice!!
        assertEquals(VoiceMode.FIXED, voice.mode.known)
        assertEquals("HRT_0107_043", voice.fixedId)
        assertEquals(at, voice.fixedAt)
        assertEquals("HRT_0107_043 · 고정 · 11월 20일 내보냄", VoiceIds.stateLabel(exported, exported.block(b(43)), ctx.zone))
    }

    @Test
    fun `T-12-18 format change notice counts fixed IDs`() {
        var doc = F2.build()
        val first = doc.block(F2.blockAt(1))
        val extra = (1..214).map { i ->
            first.copy(
                key = "01JZ000000BKNM0000000" + (100 + i).toString().padStart(5, '0'), order = "x" + i.toString().padStart(3, '0'),
                number = 100 + i, kind = BlockKind.DIALOGUE.open(), speakerKey = F2.HART, voice = fixed("HRT_0107_" + (100 + i)),
            )
        }
        doc = doc.mapBlocks { it.copy(voice = null) }.addBlocks(extra)
        assertEquals(listOf("고정 ID 214개는 그대로", "고정 ID도 새 형식으로"), VoiceIds.formatChangeNotice(doc))
    }

    @Test
    fun `T-12-19 deletion toasts and trash grouping`() {
        val r9 = "01JZ000000CHGR000000000099"
        val doc = F1.build().withRecords(record(r9, Anchor.AtBlock(b(40))))
        val r = BlockTrash.delete(doc, (38..42).map(::b), ctx)
        assertEquals("블록 5개 지움 · ◆ 2개 다음 자리로", r.toast)
        assertEquals(Anchor.AtBlock(b(43)), r.doc.record(F1.R4).anchor)
        assertEquals(Anchor.AtBlock(b(43)), r.doc.record(r9).anchor)
    }

    @Test
    fun `T-12-19 four blocks deleted today form one trash row`() {
        val first = BlockTrash.delete(F1.build(), listOf(b(36), b(37)), ctx).doc
        val second = BlockTrash.delete(first, listOf(b(38), b(39)), ctx).doc
        val row = BlockTrash.groupRows(second, ctx.today, ctx.zone).single { it.blockKeys.contains(b(36)) }
        assertEquals("1-7 씬 3 · 블록 4개", row.title)
        assertEquals("블록 · 잿빛 새벽의 등불 · 오늘 지움", row.sub)
        assertEquals("30일 남음", row.remaining)
        assertEquals(listOf("되살리기", "영구 삭제"), row.buttons)
    }

    @Test
    fun `T-12-19 an empty block is removed with no toast and no trash`() {
        val ins = BlockOps.insert(F1.build(), BlockPosition.After(b(48)), BlockKind.DIALOGUE, ctx)
        val counter = ins.doc.unit(F1.E17).nextBlockNumber
        val r = BlockTrash.delete(ins.doc, listOf(ins.blockKey), ctx)
        assertNull(r.toast)
        assertNull(r.doc.index.block(ins.blockKey))
        assertEquals(ins.doc.trash, r.doc.trash)
        assertEquals(counter, r.doc.unit(F1.E17).nextBlockNumber)
    }

    @Test
    fun `T-12-20 an emptied block that anchors a record is not empty`() {
        val doc = F1.build().withBlock(b(43)) { it.copy(runs = emptyList()) }
        val r = BlockOps.backspace(doc, b(43), atStart = true, ctx) as BlockOps.BackspaceResult.Deleted
        assertNotNull(r.doc.block(b(43)).deletedAt)
        val r1 = r.doc.record(F1.R1)
        assertEquals(Anchor.AtBlock(b(44)), r1.anchor)
        assertEquals(AnchorState.ORPHANED, r1.anchorState.known)
        assertEquals("블록 #043 지움 · ◆ 1개는 #044 자리로", r.result.toast)
    }

    @Test
    fun `T-12-21 moving across the writing line needs no re-linking`() {
        val r = BlockMove.move(F1.build(), listOf(b(43)), BlockPosition.SceneStart(K.SCENE_1_9_1), ctx)
        val moved = r.doc.block(b(43))
        assertEquals(1, moved.number)
        assertEquals(K.EPISODE_1_9, r.doc.numberingUnitOf(moved))
        assertEquals(Anchor.AtBlock(b(43)), r.doc.record(F1.R1).anchor)
        assertEquals(K.SCENE_1_9_1, r.doc.manuscript.writingLineSceneKey)
        assertFalse(r.doc.isPlanned(r.doc.record(F1.R1)))
    }

    @Test
    fun `T-12-22 cut and paste is a move`() {
        val doc = F1.build()
        val r = BlockMove.paste(doc, BlockMove.cut(doc, listOf(b(43))), BlockPosition.After(b(50)), ctx)
        assertEquals(listOf(b(43)), r.blockKeys)
        assertEquals(43, r.doc.block(b(43)).number)
        assertEquals(F1.S4, r.doc.block(b(43)).sceneKey)
        assertEquals(Anchor.AtBlock(b(43)), r.doc.record(F1.R1).anchor)
        assertEquals("◆ 1개 씬 3 → 씬 4", r.toast)
    }

    @Test
    fun `T-12-23 duplicate manual voice IDs`() {
        val base = F1.build()
        val manual = base.copy(project = base.project.copy(script = base.project.script.copy(voiceIdFormat = VoiceIdFormat(VoiceIdFormatMode.MANUAL.open()))))
            .withBlock(b(43)) { it.copy(voice = VoiceState(manualId = "HRT_A_01")) }
            .withBlock(b(47)) { it.copy(voice = VoiceState(manualId = "HRT_A_01")) }
        val item = ScriptChecks.voiceDuplicates(manual).single()
        assertEquals("▲ 보이스 ID 겹침 1", item.title)
        assertEquals(setOf(b(43), b(47)), item.subjectKeys.toSet())
        assertTrue(ScriptChecks.voiceDuplicates(base).isEmpty())
    }

    @Test
    fun `T-12-24a restore placement and preserved state`() {
        val doc = F1.build().withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }
        val d1 = BlockTrash.delete(doc, listOf(b(43)), ctx).doc
        val d2 = BlockTrash.delete(d1, listOf(b(42), b(44)), ctx).doc
        val restored = (BlockTrash.restore(d2, b(43), ctx) as BlockTrash.RestoreResult.Restored).doc
        assertEquals(listOf(36, 37, 38, 39, 40, 41, 45, 43, 46, 47, 48), restored.liveNumbers(F1.S3))
        val b43 = restored.block(b(43))
        assertEquals(43, b43.number)
        assertEquals("HRT_0107_043", b43.voice?.fixedId)
        assertEquals(VoiceMode.FIXED, b43.voice?.mode?.known)
        assertTrue(restored.index.allDiscardedVoiceIds.none { it.id == "HRT_0107_043" })
    }

    @Test
    fun `T-12-24b restore when the scene is in the trash`() {
        val d1 = BlockTrash.delete(F1.build(), listOf(b(43)), ctx).doc
        val d2 = SceneLifecycle.delete(d1, F1.S3, ctx)!!.doc
        val ask = BlockTrash.restore(d2, b(43), ctx) as BlockTrash.RestoreResult.NeedsSceneChoice
        assertEquals(listOf("씬도 되살리기", "다른 씬 끝에"), ask.options)
        val restored = (BlockTrash.restore(d2, b(43), ctx, BlockTrash.SceneChoice.AppendTo(F1.S4)) as BlockTrash.RestoreResult.Restored).doc
        assertEquals(b(43), restored.manuscript.liveBlocksOf(F1.S4).last().key)
        assertEquals(43, restored.block(b(43)).number)
    }

    @Test
    fun `T-12-25 point seat shown in the panel, not the manuscript`() {
        val p1 = "01JZ000000CHGR000000000095"
        val target = ChangeTarget.Field(io.github.lenticualrstone.civilight.model.cast.EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION)
        val doc = F1.build().withRecords(record(p1, Anchor.UnitStart(F1.E17), target))
        val lines = Records.pointLines(doc, F1.S1)
        assertEquals(listOf("◆ 1-7부터 · 근거 블록 없음"), lines.map { it.text })
        assertEquals("블록에 잇기", lines.single().button)
        assertFalse(lines.single().atEnd)
        val s1Blocks = doc.manuscript.liveBlocksOf(F1.S1)
        assertTrue(s1Blocks.none { blk -> Records.marksOn(doc, blk.key).any { it.recordKey == p1 } })
        // W1 (world event) appears nowhere in the script.
        for (s in doc.manuscript.liveScenes) assertTrue(Records.pointLines(doc, s.key).none { it.recordKey == K.W1 })
        assertTrue(doc.manuscript.liveBlocks.none { blk -> Records.marksOn(doc, blk.key).any { it.recordKey == K.W1 } })
        assertTrue(Records.pointLines(doc, F1.S3).isEmpty())
        assertTrue(Records.pointLines(doc, F1.S1, io.github.lenticualrstone.civilight.model.script.ScriptLayers(marks = false)).isEmpty())
        val candidates = Anchors.relinkCandidates(doc, p1)
        assertEquals(s1Blocks.map { it.key }, candidates.map { it.key })
        val second = s1Blocks[1].key
        val linked = Anchors.anchorToBlock(doc, p1, second, ctx)
        assertTrue(Records.pointLines(linked, F1.S1).isEmpty())
        assertTrue(Records.marksOn(linked, second).any { it.recordKey == p1 && it.glyph == "◆" })
        assertTrue(linked.recordsHere(second).any { it.key == p1 })
    }
}
