package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S7
import io.github.lenticualrstone.civilight.domain.story.Fx.S8
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.clock
import io.github.lenticualrstone.civilight.domain.story.Fx.rel
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.checks.AckSubject
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.time.OrderChecks
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeChange
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `맞음` · `이대로 두기` semantics (STO-41) on the items this area emits; ORD-03, CHK-04, CHK-05, CHK-07. */
class AckTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    /** ORD-01's variant: P = 같은 날 20:00, X = 같은 날 11:00 after it. */
    private val ord = doc.withTime(S8, rel(S7, 0, clock = clock(20, 0))).withTime(S9, rel(S8, 0, clock = clock(11, 0)))

    @Test
    fun ord03_acknowledgementVoidsWhenTheTimeChanges() {
        val notice = OrderChecks.notice(SceneTimes(ord), S9)!!
        assertEquals(CheckType.TIME_ORDER, notice.checkType)
        assertEquals(listOf(S9, S8), notice.subjectKeys)
        val acked = Acks.acknowledge(ord, notice, ctx)
        assertTrue(Acks.isAcknowledged(acked, notice))
        assertEquals(AckKind.MATCH, Acks.find(acked, notice)!!.kind.known)
        // S10 (06:10, same day as S9) is now earlier than S9 too; only S9's item is acknowledged.
        assertEquals(listOf(Fx.S10), Acks.open(acked, OrderChecks.notices(SceneTimes(acked))).map { it.sceneKey })
        assertEquals(listOf(notice), Acks.acknowledged(acked, OrderChecks.notices(SceneTimes(acked))))

        val moved = acked.withTime(S9, rel(S8, 0, clock = clock(10, 0)))
        val again = OrderChecks.notice(SceneTimes(moved), S9)!!
        assertFalse(Acks.isAcknowledged(moved, again))
        assertEquals(listOf(S9, Fx.S10), Acks.open(moved, OrderChecks.notices(SceneTimes(moved))).map { it.sceneKey })
        assertEquals("직전 씬보다 앞 · 10시간 전", again.line)
    }

    @Test
    fun orderAcknowledgementSurvivesAShiftOfBoth() {
        val notice = OrderChecks.notice(SceneTimes(ord), S9)!!
        val acked = Acks.acknowledge(ord, notice, ctx)
        val shifted = TimeChange.apply(acked, S8, rel(S7, 2, clock = clock(20, 0)), CascadeMode.WITH_FOLLOWING, ctx)
        val after = OrderChecks.notice(SceneTimes(shifted), S9)!!
        assertTrue(Acks.isAcknowledged(shifted, after))
    }

    private data class Item(override val checkType: String, override val subjectKeys: List<Key>, override val fingerprint: String) : AckSubject

    @Test
    fun chk04_keepAndRevert() {
        val item = Item(CheckType.CHANGE_ANCHOR_DELETED, listOf(K.R4), Fingerprints.of("orphan", K.R4))
        val kept = Acks.acknowledge(doc, item, ctx)
        val ack = Acks.find(kept, item)!!
        assertEquals(AckKind.KEEP, ack.kind.known)
        assertEquals(listOf(item), Acks.acknowledged(kept, listOf(item)))
        val reverted = Acks.revert(kept, ack.key)
        assertNull(Acks.find(reverted, item))
        assertEquals(listOf(item), Acks.open(reverted, listOf(item)))
    }

    @Test
    fun chk05_ackGoesWithItsSubject() {
        val item = Item(CheckType.SPEAKER_MISSING, listOf(K.BLOCK_043), "fp")
        val acked = Acks.acknowledge(doc, item, ctx)
        val deleted = Acks.dropForSubjects(acked, listOf(K.BLOCK_043))
        assertNull(Acks.find(deleted, item))
        // Undo = the previous doc, which still holds the acknowledgement.
        assertTrue(Acks.isAcknowledged(acked, item))
        // Other acknowledgements stay.
        assertEquals(doc.checks.acknowledgements, deleted.checks.acknowledgements)
    }

    @Test
    fun acknowledgingAgainReplacesAVoidOne() {
        val notice = OrderChecks.notice(SceneTimes(ord), S9)!!
        val acked = Acks.acknowledge(ord, notice, ctx)
        val moved = acked.withTime(S9, rel(S8, 0, clock = clock(10, 0)))
        val again = OrderChecks.notice(SceneTimes(moved), S9)!!
        val reAcked = Acks.acknowledge(moved, again, ctx)
        assertEquals(acked.checks.acknowledgements.size, reAcked.checks.acknowledgements.size)
        assertTrue(Acks.isAcknowledged(reAcked, again))
    }

    @Test
    fun chk07_acknowledgementsSurviveExportAndImport() {
        val notice = OrderChecks.notice(SceneTimes(ord), S9)!!
        val acked = Acks.acknowledge(ord, notice, ctx)
        val files: Map<String, JsonElement> = ProjectFileSet.split(acked).encode()
            .mapValues { (_, v) -> CivilightJson.json.parseToJsonElement(v.toString()) }
        val decoded = ProjectFileSet.decode(files)
        val back = decoded.files.toDoc(app = acked.app)
        assertEquals(acked.checks.acknowledgements, back.checks.acknowledgements)
        assertTrue(Acks.isAcknowledged(back, OrderChecks.notice(SceneTimes(back), S9)!!))
    }

    @Test
    fun clockBandNoticeForBand04() {
        val d = doc.withTime(S9, rel(S8, 1, Band.DAWN, clock(7, 0)))
        val notices = OrderChecks.clockBandNotices(SceneTimes(d), UnitCodes(d))
        val mine = notices.single { it.sceneKey == S9 }
        assertEquals("1-7 씬 3 · 새벽 07:00", mine.line)
        assertEquals(CheckType.TIME_CLOCK_BAND_MISMATCH, mine.checkType)
        // The fixture's 1-5 씬 1 (새벽 + 06:20) is the other one, acknowledged (`ACK_CLOCK_BAND`).
        assertEquals(setOf(S9, K.SCENE_1_5_1), notices.map { it.sceneKey }.toSet())
        assertEquals(Band.DAWN.open(), (d.index.unit(S9)!!.time as io.github.lenticualrstone.civilight.model.story.SceneTime.Relative).band)
    }
}
