package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormat
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.VoiceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Voice-ID format, the `◇` rule, manual mode, re-fixing and Q-07 collisions (SCR-23/24/25). */
class VoiceIdsTest {
    private val ctx = testCtx()
    private fun b(n: Int) = F1.b(n)
    private val pattern = VoiceIdFormat.DEFAULT_PATTERN

    @Test
    fun `pattern tokens and pad widths`() {
        val parts = VoiceIds.EpisodeParts(1, 7)
        assertEquals("HRT_0107_043", VoiceIds.format(pattern, "HRT", parts, 43))
        assertEquals("HRT_0107_045_1", VoiceIds.format(pattern, "HRT", parts, 45, optionIndex = 1))
        assertEquals("HRT_0107_1043", VoiceIds.format(pattern, "HRT", parts, 1043))
        assertEquals("HRT-1-7-43", VoiceIds.format("{code}-{chapter}-{episode}-{number}", "HRT", parts, 43))
    }

    @Test
    fun `automatic IDs need a speaker with a code and a dialogue block`() {
        val doc = F1.build()
        assertEquals("HRT_0107_043", VoiceIds.autoId(doc, doc.block(b(43))))
        assertNull(VoiceIds.autoId(doc, doc.block(b(44))))
        assertNull(VoiceIds.autoId(doc, doc.block(K.BLOCK_1_7_027)))
        assertEquals("RIE_0102_003", VoiceIds.currentId(doc, doc.block(K.BLOCK_B12)))
    }

    @Test
    fun `the number part never raises the format notice but speaker and episode do`() {
        val fixedDoc = F1.build().withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }
        assertFalse(VoiceIds.differsFromFormat(fixedDoc, fixedDoc.block(b(43))))
        val renumbered = fixedDoc.withBlock(b(43)) { it.copy(number = 7) }
        assertFalse(VoiceIds.differsFromFormat(renumbered, renumbered.block(b(43))))
        val otherSpeaker = BlockOps.changeSpeaker(fixedDoc, b(43), K.SERA, ctx)
        assertTrue(VoiceIds.differsFromFormat(otherSpeaker, otherSpeaker.block(b(43))))
        assertEquals("◇ 보이스 ID가 화자 · 에피소드와 다름", ScriptChecks.voiceFormatDiffers(otherSpeaker).single().title)
        assertEquals(listOf("새 ID로", "그대로"), ScriptChecks.voiceFormatDiffers(otherSpeaker).single().buttons)
    }

    @Test
    fun `그대로 stored with the item's fingerprint silences it`() {
        val doc = BlockOps.changeSpeaker(F1.build().withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }, b(43), K.SERA, ctx)
        val fp = ScriptChecks.voiceFormatFingerprint(doc, b(43))!!
        val ack = io.github.lenticualrstone.civilight.model.checks.Acknowledgement(
            key = "01JZ000000ACKS000000000099",
            checkType = io.github.lenticualrstone.civilight.model.checks.CheckType.VOICE_FORMAT_DIFFERS,
            subjectKeys = listOf(b(43)), fingerprint = fp,
            kind = io.github.lenticualrstone.civilight.model.checks.AckKind.KEEP.open(),
        )
        val acked = doc.copy(checks = doc.checks.copy(acknowledgements = doc.checks.acknowledgements + ack))
        assertTrue(ScriptChecks.voiceFormatDiffers(acked).isEmpty())
    }

    @Test
    fun `manual format uses typed IDs only`() {
        val base = F1.build()
        val manual = base.copy(project = base.project.copy(script = base.project.script.copy(voiceIdFormat = VoiceIdFormat(VoiceIdFormatMode.MANUAL.open()))))
            .withBlock(b(43)) { it.copy(voice = VoiceState(manualId = "HRT_A_01")) }
        assertEquals("HRT_A_01", VoiceIds.currentId(manual, manual.block(b(43))))
        assertNull(VoiceIds.currentId(manual, manual.block(b(47))))
        assertTrue(ScriptChecks.voiceFormatDiffers(manual).isEmpty())
    }

    @Test
    fun `T-12-23 manual IDs are typed in the 속성 tab, cleared when blank, never over a fixed ID`() {
        val base = F1.build()
        val manual = base.copy(project = base.project.copy(script = base.project.script.copy(voiceIdFormat = VoiceIdFormat(VoiceIdFormatMode.MANUAL.open()))))
        val typed = VoiceIds.setManualId(manual, b(43), " HRT_A_01 ", ctx)
        assertEquals("HRT_A_01", VoiceIds.currentId(typed, typed.block(b(43))))
        val both = VoiceIds.setManualId(typed, b(47), "HRT_A_01", ctx)
        assertEquals("▲ 보이스 ID 겹침 1", ScriptChecks.voiceDuplicates(both).single().title)
        val cleared = VoiceIds.setManualId(typed, b(43), "  ", ctx)
        assertNull(cleared.block(b(43)).voice)
        val fixedDoc = manual.withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }
        assertEquals(fixedDoc, VoiceIds.setManualId(fixedDoc, b(43), "X", ctx))
        assertEquals(typed, VoiceIds.setManualId(typed, b(44), "X", ctx))
    }

    @Test
    fun `voice IDs off hide the field but keep the data`() {
        val base = F1.build().withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }
        val off = base.copy(project = base.project.copy(script = base.project.script.copy(voiceIdEnabled = false)))
        assertNull(Panels.voiceField(off, off.block(b(43)), ctx.zone))
        assertEquals("HRT_0107_043", off.block(b(43)).voice?.fixedId)
        assertEquals("HRT_0107_043 · 고정 · 11월 20일 내보냄", Panels.voiceField(base, base.block(b(43)), ctx.zone))
    }

    @Test
    fun `fixed IDs again in a new format become discarded strings`() {
        val doc = F1.build().withBlock(b(43)) { it.copy(voice = fixed("HRT_0107_043")) }
        val format = VoiceIdFormat(pattern = "{code}-{chapter}-{episode}-{number}")
        val out = VoiceIds.refixAll(doc, format, ctx)
        assertEquals("HRT-1-7-43", VoiceIds.currentId(out, out.block(b(43))))
        assertTrue(VoiceIds.isFixed(out.block(b(43)).voice))
        val discarded = out.index.allDiscardedVoiceIds.single { it.id == "HRT_0107_043" }
        assertEquals(DiscardReason.FORMAT_REFIXED, discarded.reason.known)
        assertEquals("RIE-1-7-42", VoiceIds.currentId(out, out.block(b(42))))
        assertEquals(format, out.project.script.voiceIdFormat)
    }

    @Test
    fun `Q-07 an automatic ID equal to a discarded one raises the duplicate check`() {
        var doc = F1.build().withBlock(b(42)) { it.copy(voice = fixed("RIE_0107_042")) }
        doc = BlockTrash.delete(doc, listOf(b(42)), ctx).doc
        doc = BlockOps.changeSpeaker(doc, b(43), K.RIEN, ctx)
        assertTrue(ScriptChecks.voiceDuplicates(doc).isEmpty())
        val renumbered = Numbering.renumber(doc, F1.E17, ctx)
        assertEquals(42, renumbered.block(b(43)).number)
        val item = ScriptChecks.voiceDuplicates(renumbered).single()
        assertEquals("▲ 보이스 ID 겹침 1", item.title)
        assertEquals(listOf("RIE_0107_042"), item.details)
        assertEquals(58, renumbered.unit(F1.E17).nextBlockNumber)
    }

    @Test
    fun `spoken options get suffixed IDs and are discarded with their block`() {
        val doc = F3.build()
        val choice = doc.block(b(45))
        val options = BlockOps.orderedOptions(choice)
        assertNull(VoiceIds.currentIdOfOption(doc, choice, options[1]))
        val fixedOption = doc.withBlock(b(45)) { blk ->
            blk.copy(options = blk.options.map { if (it.key == F3.OPTION_1) it.copy(voice = fixed("HRT_0107_045_1")) else it })
        }
        val deleted = BlockTrash.delete(fixedOption, listOf(b(45)), ctx).doc
        val d = deleted.index.allDiscardedVoiceIds.single { it.id == "HRT_0107_045_1" }
        assertEquals(F3.OPTION_1, d.optionKey)
        assertEquals(DiscardReason.DELETED, d.reason.known)
    }
}
