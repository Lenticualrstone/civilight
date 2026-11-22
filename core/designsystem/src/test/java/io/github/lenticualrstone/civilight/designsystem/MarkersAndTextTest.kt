package io.github.lenticualrstone.civilight.designsystem

import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.HitRun
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.text.LABEL_ANCHOR_DELETED
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind.BGM
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind.CHANGE
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind.DIRECTION
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind.MEMO
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind.NOTICE
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind.WARNING
import io.github.lenticualrstone.civilight.designsystem.text.MarkerLine
import io.github.lenticualrstone.civilight.designsystem.text.PartUnderline
import io.github.lenticualrstone.civilight.designsystem.text.SpeakerMarker
import io.github.lenticualrstone.civilight.designsystem.text.SpeakerPart
import io.github.lenticualrstone.civilight.designsystem.text.decorationStyle
import io.github.lenticualrstone.civilight.designsystem.text.markerSpokenText
import io.github.lenticualrstone.civilight.designsystem.text.markerStyle
import io.github.lenticualrstone.civilight.designsystem.text.searchHitBoxStyle
import io.github.lenticualrstone.civilight.designsystem.text.searchHitDecorations
import io.github.lenticualrstone.civilight.designsystem.text.searchHitRuns
import io.github.lenticualrstone.civilight.designsystem.text.speakerLabelParts
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import io.github.lenticualrstone.civilight.designsystem.theme.TypeRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkersAndTextTest {

    private val themes = listOf(CivilightColors.dark, CivilightColors.light)

    @Test
    fun `T-DS-12 marker colours`() {
        themes.forEach { c ->
            assertEquals(c.acc, markerStyle(CHANGE, colors = c).color)
            assertEquals(c.ink3, markerStyle(NOTICE, colors = c).color)
            assertEquals(c.warn, markerStyle(WARNING, colors = c).color)
            listOf(DIRECTION, BGM, MEMO).forEach { assertEquals(c.ink3, markerStyle(it, colors = c).color) }
            assertNotEquals(c.acc, markerStyle(WARNING, colors = c).color)
            assertNotEquals(c.warn, markerStyle(CHANGE, colors = c).color)
        }
        assertTrue(markerStyle(CHANGE, colors = CivilightColors.dark).filled)
        assertFalse(markerStyle(NOTICE, colors = CivilightColors.dark).filled)
        assertEquals(listOf('◆', '◇', '▲', '▸', '♪', '⌁'), MarkerKind.entries.map { it.symbol })
    }

    @Test
    fun `T-DS-13 dotted reference`() {
        val trashed = speakerLabelParts("노아", "노아", selected = false, blockNo = null, markers = emptyList(), deleted = true)
        assertEquals(SpeakerPart("노아", TypeRole.SPEAKER_NAME, ColorRole.INK2, underline = PartUnderline.DOTTED), trashed[0])
        assertEquals("◇", trashed[1].text)
        assertEquals(ColorRole.INK3, trashed[1].color)
        assertFalse(trashed[1].alignEnd)
        assertEquals(2, trashed.size)

        val restored = speakerLabelParts("노아", "노아", selected = false, blockNo = null, markers = emptyList(), deleted = false)
        assertEquals(listOf(SpeakerPart("노아", TypeRole.SPEAKER_NAME, ColorRole.INK2)), restored)

        val c = CivilightColors.dark
        val deleted = decorationStyle(InlineDecoration.DELETED_REF, c, c.ink2).underline!!
        assertTrue(deleted.dotted)
        assertEquals(c.ink2, deleted.color)
        assertEquals(1.dp, deleted.thickness)
        assertEquals(4.dp, deleted.offset)
    }

    @Test
    fun `T-DS-14 dotted mark`() {
        themes.forEach { c ->
            val s = markerStyle(CHANGE, MarkerLine.DOTTED, c)
            assertFalse(s.filled)
            assertTrue(s.dashed)
            assertEquals(1.5f, s.strokeUnits, 0f)
            assertEquals(2f, s.dashOnUnits, 0f)
            assertEquals(1.8f, s.dashOffUnits, 0f)
            assertEquals(c.acc, s.color)
            assertEquals(LABEL_ANCHOR_DELETED, s.accessibilityLabel)
            assertEquals("근거 지워짐", s.accessibilityLabel)
            val ownReveal = markerStyle(NOTICE, MarkerLine.SOLID, c)
            assertFalse(ownReveal.dashed)
            val restored = markerStyle(CHANGE, MarkerLine.SOLID, c)
            assertTrue(restored.filled)
            assertFalse(restored.dashed)
        }
    }

    @Test
    fun `T-DS-19 speaker line parts`() {
        val wide = speakerLabelParts("???", "하르트", selected = true, blockNo = "#043", markers = listOf(SpeakerMarker(CHANGE)))
        assertEquals(3, wide.size)
        assertEquals(SpeakerPart("???", TypeRole.SPEAKER_NAME, ColorRole.INK), wide[0])
        assertEquals(SpeakerPart("하르트 · #043", TypeRole.SPEAKER, ColorRole.INK3), wide[1])
        assertEquals("◆", wide[2].text)
        assertEquals(ColorRole.ACC, wide[2].color)
        assertTrue(wide[2].alignEnd)

        val plain = speakerLabelParts("리엔", "리엔", selected = false, blockNo = "#042", markers = emptyList())
        assertEquals(listOf(SpeakerPart("리엔", TypeRole.SPEAKER_NAME, ColorRole.INK2)), plain)
    }

    @Test
    fun `T-DS-20 search hit stays inline`() {
        val sentence = "“오리지늄은 타지 않아. 빛만 남지.”"
        val runs = searchHitRuns(sentence, listOf(1 until 4))
        assertEquals(
            listOf(HitRun("“", false), HitRun("오리지", true), HitRun("늄은 타지 않아. 빛만 남지.”", false)),
            runs,
        )
        assertEquals(sentence, runs.joinToString("") { it.text })
        assertFalse(runs.any { '\n' in it.text })
        assertEquals(listOf(DecorationRange(1, 4, InlineDecoration.SEARCH_HIT)), searchHitDecorations(sentence, listOf(1 until 4)))
        themes.forEach { c ->
            val box = searchHitBoxStyle(c)
            assertEquals(c.hl, box.fill)
            assertEquals(c.acc, box.outline)
            assertEquals(1.dp, box.outlineWidth)
            assertEquals(4.dp, box.radius)
            assertEquals(c.ink, decorationStyle(InlineDecoration.SEARCH_HIT, c, c.ink2).textColor)
        }
    }

    @Test
    fun `search runs clamp and merge`() {
        val runs = searchHitRuns("abcdef", listOf(4 until 9, 0 until 2, 1 until 3))
        assertEquals(listOf(HitRun("abc", true), HitRun("d", false), HitRun("ef", true)), runs)
        assertEquals(listOf(HitRun("abc", false)), searchHitRuns("abc", emptyList()))
    }

    @Test
    fun `DS-16 spoken markers`() {
        assertEquals("변화 관계 · 리엔의 마음", markerSpokenText("◆ 관계 · 리엔의 마음"))
        assertEquals("경고 공개 전 사용", markerSpokenText("▲ 공개 전 사용"))
        assertEquals("배경 하층 거리(새벽)", markerSpokenText("▸ 배경 하층 거리(새벽)"))
    }

    @Test
    fun `DS-19 link and candidate decorations`() {
        val c = CivilightColors.light
        val link = decorationStyle(InlineDecoration.LINK, c, c.ink)
        assertEquals(c.ink3, link.underline!!.color)
        assertFalse(link.underline!!.dotted)
        assertEquals(2.dp, link.underline!!.offset)
        val cand = decorationStyle(InlineDecoration.CANDIDATE, c, c.ink).underline!!
        assertTrue(cand.dotted)
        assertEquals(2.dp, cand.thickness)
        assertEquals(6.dp, cand.offset)
        val on = decorationStyle(InlineDecoration.CANDIDATE_SELECTED, c, c.ink)
        assertEquals(c.acc, on.underline!!.color)
        assertEquals(c.sel, on.box!!.fill)
        assertNull(decorationStyle(InlineDecoration.PAREN, c, c.ink).underline)
        assertEquals(c.ink3, decorationStyle(InlineDecoration.PAREN, c, c.ink).textColor)
    }
}
