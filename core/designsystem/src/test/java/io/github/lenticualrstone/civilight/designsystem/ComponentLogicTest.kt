package io.github.lenticualrstone.civilight.designsystem

import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.GLOW_MAX_SCALE
import io.github.lenticualrstone.civilight.designsystem.component.GLOW_MIN_SCALE
import io.github.lenticualrstone.civilight.designsystem.component.StepState
import io.github.lenticualrstone.civilight.designsystem.component.fieldOutline
import io.github.lenticualrstone.civilight.designsystem.component.glowBandScale
import io.github.lenticualrstone.civilight.designsystem.component.sliderStepFor
import io.github.lenticualrstone.civilight.designsystem.component.stepState
import io.github.lenticualrstone.civilight.designsystem.component.warningSpan
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcons
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.icon.MarkGeometry
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.DirectionCue
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.text.ScriptRun
import io.github.lenticualrstone.civilight.designsystem.text.directionLineText
import io.github.lenticualrstone.civilight.designsystem.text.manuscriptText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.TextScale
import io.github.lenticualrstone.civilight.designsystem.theme.hapticConstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentLogicTest {

    @Test
    fun `DS-19 manuscript runs become one paragraph with decorations`() {
        val runs = listOf(
            ScriptRun.Paren("(숨을 삼키며) "),
            ScriptRun.Plain("“"),
            ScriptRun.Link("오리지늄 등불"),
            ScriptRun.Plain("은 "),
            ScriptRun.Candidate("잿더미 시장"),
            ScriptRun.Plain(" · "),
            ScriptRun.DeletedRef("노아"),
            ScriptRun.HiddenLink("숨긴 인물"),
        )
        val all = manuscriptText(runs, showParens = true, showCandidates = true)
        assertEquals("(숨을 삼키며) “오리지늄 등불은 잿더미 시장 · 노아숨긴 인물", all.text)
        assertEquals(
            listOf(
                DecorationRange(0, 9, InlineDecoration.PAREN),
                DecorationRange(10, 17, InlineDecoration.LINK),
                DecorationRange(19, 25, InlineDecoration.CANDIDATE),
                DecorationRange(28, 30, InlineDecoration.DELETED_REF),
                DecorationRange(30, 35, InlineDecoration.HIDDEN_LINK),
            ),
            all.decorations,
        )
        val layersOff = manuscriptText(runs, showParens = false, showCandidates = false)
        assertEquals("“오리지늄 등불은 잿더미 시장 · 노아숨긴 인물", layersOff.text)
        assertTrue(layersOff.decorations.none { it.kind == InlineDecoration.CANDIDATE || it.kind == InlineDecoration.PAREN })
        val tapped = manuscriptText(listOf(ScriptRun.Candidate("잿더미 시장", selected = true)), showCandidates = false)
        assertEquals(InlineDecoration.CANDIDATE_SELECTED, tapped.decorations.single().kind)
    }

    @Test
    fun `DS-42 direction line spacing`() {
        val text = directionLineText(listOf(DirectionCue(MarkerKind.DIRECTION, "배경", "하층 거리(새벽)"), DirectionCue(MarkerKind.BGM, null, "잔불")))
        assertEquals("▸ 배경  하층 거리(새벽)   ♪ 잔불", text)
        assertEquals("⌁ 작가 메모  여기서", directionLineText(listOf(DirectionCue(MarkerKind.MEMO, "작가 메모", "여기서"))))
    }

    @Test
    fun `DS-34 slider snaps to the nearest step`() {
        assertEquals(0, sliderStepFor(0f, 5))
        assertEquals(2, sliderStepFor(0.5f, 5))
        assertEquals(4, sliderStepFor(1.3f, 5))
        assertEquals(1, sliderStepFor(0.3f, 5))
        assertEquals(0, sliderStepFor(0.7f, 1))
        assertEquals(5, TextScale.StepsSp.size)
        assertEquals(22f, TextScale.forStep(99).manuscriptBaseSp, 0f)
    }

    @Test
    fun `DS-61 glow bands stay between 0_72 and 1_08`() {
        for (i in 0 until 7) {
            var t = 0f
            while (t < 5f) {
                listOf(0f, 0.5f, 1f).forEach { level ->
                    val s = glowBandScale(i, t, level)
                    assertTrue("band $i t $t: $s", s in GLOW_MIN_SCALE..GLOW_MAX_SCALE)
                }
                t += 0.07f
            }
        }
        assertEquals(GLOW_MIN_SCALE, glowBandScale(0, 0f, 0f), 1e-4f)
        assertEquals(GLOW_MAX_SCALE, glowBandScale(0, 1.6f, 1f), 1e-4f)
    }

    @Test
    fun `DS-02 warning text is warn from the triangle to the next separator`() {
        val c = CivilightColors.dark
        val s = warningSpan("▲ 이동 시간 부족 · 경로 38 km · 도보 15 km/일", c.warn)
        assertEquals(1, s.spanStyles.size)
        assertEquals(0, s.spanStyles[0].start)
        assertEquals("▲ 이동 시간 부족".length, s.spanStyles[0].end)
        assertEquals(SpanStyle(color = c.warn), s.spanStyles[0].item)
        val tail = warningSpan("첫 공개 1-7 · 잇기 시 ▲ 공개 전 사용", c.warn)
        assertEquals("첫 공개 1-7 · 잇기 시 ▲ 공개 전 사용".length, tail.spanStyles[0].end)
        assertTrue(warningSpan("1-7 · 씬 3", c.warn).spanStyles.isEmpty())
    }

    @Test
    fun `DS-48 step states`() {
        assertEquals(StepState.DONE, stepState(0, 2))
        assertEquals(StepState.CURRENT, stepState(2, 2))
        assertEquals(StepState.UPCOMING, stepState(3, 2))
    }

    @Test
    fun `DS-35 field outline`() {
        val c = CivilightColors.light
        assertEquals(c.edge, fieldOutline(focused = false, error = false, c).color)
        assertEquals(1.dp, fieldOutline(focused = false, error = false, c).width)
        assertEquals(c.acc, fieldOutline(focused = true, error = false, c).color)
        assertEquals(1.5.dp, fieldOutline(focused = true, error = false, c).width)
        assertEquals(c.r18, fieldOutline(focused = true, error = true, c).color)
    }

    @Test
    fun `DS-12 every icon path parses`() {
        IconId.entries.forEach { id ->
            val v = CivilightIcons.get(id)
            assertEquals(24f, v.viewportWidth, 0f)
            assertEquals(id.name, v.name)
        }
        assertEquals("CheckBold", CivilightIcons.checkBold.name)
    }

    @Test
    fun `DS-13 mark geometry`() {
        val m = MarkGeometry.Mark96
        assertEquals(listOf(72f, 60f, 40f), m.bars.map { it.width })
        assertEquals(listOf(22f, 44f, 66f), m.bars.map { it.y })
        assertEquals(58f, m.caret.x, 0f)
        val s = MarkGeometry.Static64
        assertEquals(listOf(48f, 40f, 26f), s.bars.map { it.width })
        assertEquals(12f, s.caret.height, 0f)
    }

    @Test
    fun `DS-15 haptic mapping`() {
        assertEquals(HapticFeedbackConstants.SEGMENT_TICK, hapticConstant(HapticToken.TICK, 34))
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, hapticConstant(HapticToken.TICK, 33))
        assertEquals(HapticFeedbackConstants.CONFIRM, hapticConstant(HapticToken.CONFIRM, 30))
        assertEquals(HapticFeedbackConstants.VIRTUAL_KEY, hapticConstant(HapticToken.CONFIRM, 29))
    }
}
