package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.checks.Fx.withBlock
import io.github.lenticualrstone.civilight.domain.script.ScriptChecks
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** Rows 4, 6, 7 and 8 (SCR-62) against small edits of the example project. */
class ScriptChecksTest {
    private val doc = Fx.doc()
    private var keys = 0
    private val trashCtx = TrashContext(ExampleClock.NOW, ZoneId.of("Asia/Seoul"), "test", { "01JZ000000TRSH5" + (++keys).toString().padStart(11, '0') })

    @Test
    fun speakerMissingFollowsTheSpeaker() {
        val fixed = doc.withBlock(K.BLOCK_1_7_027) { it.copy(speakerKey = K.KAIL) }
        assertTrue(Fx.run(fixed).ofType(CheckType.SPEAKER_MISSING).isEmpty())

        val two = doc.withBlock(K.block(1, 7, 43)) { it.copy(speakerKey = null) }
        val items = Fx.run(two).ofType(CheckType.SPEAKER_MISSING)
        assertEquals(listOf("1-7 씬 2", "1-7 씬 3"), items.map { it.location })
        assertEquals("“멈추지 마. 저건 등불이 아니야.”", items[1].body)
        assertEquals(CheckFilter.SPEAKER, items[1].filter)
        assertEquals(2, Fx.run(two).count(CheckFilter.SPEAKER))
    }

    @Test
    fun speakerMissingCannotBeAcknowledged() {
        val item = Fx.run(doc).ofType(CheckType.SPEAKER_MISSING).single()
        assertSame(doc, CheckAcks.acknowledge(doc, item, Fx.ctx()))
    }

    @Test
    fun fixedVoiceIdThatDiffersFromTheFormat() {
        val (changed, block) = withForeignFixedId(doc)
        val report = Fx.run(changed)
        val item = report.ofType(CheckType.VOICE_FORMAT_DIFFERS).single()
        assertEquals(listOf(block.key), item.subjectKeys)
        assertEquals(ScriptChecks.voiceFormatFingerprint(changed, block.key), item.fingerprint)
        assertEquals("ZZZ_0101_${block.number.toString().padStart(3, '0')} → ${VoiceIds.autoId(changed, block)}", item.body)
        assertEquals(listOf("새 ID로", "그대로"), item.actions.map { it.label })
        assertEquals(CheckFilter.VOICE, item.filter)
        val group = CheckCenterView.rows(report).filterIsInstance<CheckListRow.Group>().single()
        assertEquals("◇ 형식과 다른 ID 1", group.title)

        // `그대로` stores a match on the block; the item moves to `맞다고 표시한 것`.
        val acked = CheckAcks.acknowledge(changed, item, Fx.ctx())
        assertEquals(AckKind.MATCH, acked.checks.acknowledgements.last().kind.known)
        val after = Fx.run(acked)
        assertTrue(after.ofType(CheckType.VOICE_FORMAT_DIFFERS).all(after::isAcknowledged))
        assertEquals(1, after.count(CheckFilter.VOICE)) // only `▲ 보이스 비어 있음` stays open

        // Another fixed ID voids it; the next evaluation prunes the void acknowledgement.
        val again = acked.withBlock(block.key) { it.copy(voice = it.voice!!.copy(fixedId = "YYY_0101_001")) }
        val reopened = Fx.run(again)
        assertTrue(reopened.ofType(CheckType.VOICE_FORMAT_DIFFERS).none(reopened::isAcknowledged))
        assertEquals(1, CheckAcks.voided(reopened).size)
        assertEquals(doc.checks.acknowledgements.size, CheckAcks.prune(reopened).checks.acknowledgements.size)
    }

    /** STO CHK-05 / CNV T-CNV-26: an acknowledgement on a block goes with the block. */
    @Test
    fun chk05_blockAcknowledgementFollowsTheBlock() {
        val (changed, block) = withForeignFixedId(doc)
        val item = Fx.run(changed).ofType(CheckType.VOICE_FORMAT_DIFFERS).single()
        val acked = CheckAcks.acknowledge(changed, item, Fx.ctx())
        val ackKey = acked.checks.acknowledgements.last().key

        val deleted = TrashOps.delete(acked, ItemId.block(block.key), trashCtx)
        assertTrue(deleted.doc.checks.acknowledgements.none { it.key == ackKey })
        assertTrue(Fx.run(deleted.doc).ofType(CheckType.VOICE_FORMAT_DIFFERS).isEmpty())

        val undone = TrashOps.undo(deleted.doc, deleted)
        assertTrue(undone.checks.acknowledgements.any { it.key == ackKey })
        val undoneReport = Fx.run(undone)
        assertTrue(undoneReport.ofType(CheckType.VOICE_FORMAT_DIFFERS).single().let(undoneReport::isAcknowledged))

        val restored = RestoreOps.restore(deleted.doc, deleted.entries.single().key, trashCtx) as RestoreOutcome.Restored
        val fresh = Fx.run(restored.doc)
        assertEquals(1, fresh.ofType(CheckType.VOICE_FORMAT_DIFFERS).size)
        assertTrue(fresh.ofType(CheckType.VOICE_FORMAT_DIFFERS).none(fresh::isAcknowledged))
    }

    @Test
    fun sharedVoiceIdRaisesOneDuplicateRow() {
        val fixedBlocks = doc.manuscript.liveBlocks.filter { VoiceIds.isFixed(it.voice) }.take(2)
        val shared = fixedBlocks[0].voice!!.fixedId!!
        val dup = doc.withBlock(fixedBlocks[1].key) { it.copy(voice = it.voice!!.copy(fixedId = shared)) }
        val item = Fx.run(dup).ofType(CheckType.VOICE_DUPLICATE).single()
        assertEquals("▲ 보이스 ID 겹침 1", item.title)
        assertEquals(fixedBlocks.map { it.key }, item.subjectKeys)
        assertEquals(shared, item.body)
        assertSame(dup, CheckAcks.acknowledge(dup, item, Fx.ctx()))
    }

    /**
     * STO CHK-04 with a real delete: removing #043 relocates R4, R5 and R5A to #044 (`근거 지워짐`).
     * `이대로 두기` keeps one; `되돌리기` reopens it.
     */
    @Test
    fun chk04_anchorDeletedKeepAndRevert() {
        val deletion = TrashOps.delete(doc, ItemId.block(K.block(1, 7, 43)), trashCtx)
        val deleted = deletion.doc
        val report = Fx.run(deleted)
        val items = report.ofType(CheckType.CHANGE_ANCHOR_DELETED)
        assertEquals(setOf(K.R4, K.R5, K.R5A), items.map { it.subjectKeys.single() }.toSet())
        assertEquals(CheckFilter.CHANGE, items.first().filter)
        val r4 = items.single { it.subjectKeys == listOf(K.R4) }
        assertEquals("◆ 관계 리엔 → 하르트 · 근거 #043 지워짐", r4.body)
        assertEquals("1-7 씬 3 · #044", r4.location)
        assertEquals(listOf("다른 블록에 잇기", "이대로 두기", "기록 지우기"), r4.actions.map { it.label })

        val group = CheckCenterView.rows(report).filterIsInstance<CheckListRow.Group>().single()
        assertEquals("◇ 근거 지워진 변화 3", group.title)
        assertEquals(listOf("모두 지우기"), group.actions.map { it.label })
        assertEquals(3, report.count(CheckFilter.CHANGE))

        val kept = CheckAcks.acknowledge(deleted, r4, Fx.ctx())
        val ack = kept.checks.acknowledgements.last()
        assertEquals(AckKind.KEEP, ack.kind.known)
        assertEquals(listOf(K.R4), ack.subjectKeys)
        assertEquals(AnchorState.KEPT, kept.index.record(K.R4)!!.anchorState.known)
        val afterKeep = Fx.run(kept)
        assertEquals(2, afterKeep.count(CheckFilter.CHANGE))
        val ackRow = CheckCenterView.rows(afterKeep, CheckFilter.ACKNOWLEDGED).filterIsInstance<CheckListRow.Item>()
            .single { it.result.checkType == CheckType.CHANGE_ANCHOR_DELETED }
        assertEquals(FixKind.REVERT, ackRow.actions.single().kind)

        val reverted = CheckAcks.revert(kept, ackRow.actions.single().target!!, Fx.ctx())
        val r4After = reverted.index.record(K.R4)!!
        assertEquals(AnchorState.ORPHANED, r4After.anchorState.known)
        // `originalAnchor` is non-null iff orphaned: the pre-relocation anchor comes back from the ack.
        assertEquals(Anchor.AtBlock(K.block(1, 7, 43)), r4After.originalAnchor)
        assertEquals(3, Fx.run(reverted).count(CheckFilter.CHANGE))

        // Restoring #043 afterwards re-attaches all three records; nothing is left to check.
        val restored = RestoreOps.restore(reverted, deletion.entries.single().key, trashCtx) as RestoreOutcome.Restored
        assertEquals(Anchor.AtBlock(K.block(1, 7, 43)), restored.doc.index.record(K.R4)!!.anchor)
        assertEquals(AnchorState.NORMAL, restored.doc.index.record(K.R4)!!.anchorState.known)
        assertTrue(Fx.run(restored.doc).ofType(CheckType.CHANGE_ANCHOR_DELETED).isEmpty())
    }

    /** A fixed ID on the first fixed dialogue block of 1-1 whose speaker code part is `ZZZ`. */
    private fun withForeignFixedId(doc: ProjectDoc): Pair<ProjectDoc, Block> {
        val block = doc.manuscript.liveBlocks.first { VoiceIds.isFixed(it.voice) && doc.index.unit(it.sceneKey)?.parentKey == K.EPISODE_1_1 }
        val id = "ZZZ_0101_${block.number.toString().padStart(3, '0')}"
        val changed = doc.withBlock(block.key) { it.copy(voice = it.voice!!.copy(fixedId = id)) }
        return changed to changed.index.block(block.key)!!
    }
}
