package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.component.HoloMath
import io.github.lenticualrstone.civilight.designsystem.component.HoloPose
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** The board's holo foil (`scr_cards.py` `.hfoil` / `.hsheen`, `gen_holo.py`) as numbers (holo port, 0.2.1). */
class HoloMathTest {
    private val w = 300f
    private val h = 430f

    /** CSS gradient-line length of a [bw] × [bh] box at [deg]. */
    private fun cssLength(bw: Float, bh: Float, deg: Double): Float {
        val a = Math.toRadians(deg)
        return (abs(bw * Math.sin(a)) + abs(bh * Math.cos(a))).toFloat()
    }

    @Test
    fun tintBandRunsAt115DegreesWithA24PercentPeriodOfThe260PercentTile() {
        val line = HoloMath.tintLine(w, h, HoloPose.ZERO)
        val len = hypot(line.dx, line.dy)
        assertEquals(0.24f * cssLength(2.6f * w, 2.6f * h, 115.0), len, 0.01f)
        // CSS 115deg points right and down: screen angle atan2(dy, dx) = 115 − 90 = 25°.
        assertEquals(25.0, Math.toDegrees(atan2(line.dy.toDouble(), line.dx.toDouble())), 0.01)
    }

    @Test
    fun silverBandHasA26PercentPeriod() {
        val line = HoloMath.silverLine(w, h, HoloPose.ZERO)
        assertEquals(0.26f * cssLength(2.6f * w, 2.6f * h, 115.0), hypot(line.dx, line.dy), 0.01f)
    }

    @Test
    fun centredBackgroundPutsTheGradientMidpointOnTheBoxCentre() {
        // background-position 50 % 50 %: the tile's centre (its gradient's 50 % point) is the box centre.
        val line = HoloMath.bandLine(w, h, 115f, 2.6f, 0.5f, 0.5f, 1f)
        assertEquals(w / 2f, (line.startX + line.endX) / 2f, 0.01f)
        assertEquals(h / 2f, (line.startY + line.endY) / 2f, 0.01f)
    }

    @Test
    fun tiltMovesTintFasterThanSilverAndTheShiftMatchesTheLines() {
        val pose = HoloPose(0.6f, -0.4f)
        val t0 = HoloMath.tintLine(w, h, HoloPose.ZERO)
        val t1 = HoloMath.tintLine(w, h, pose)
        val s0 = HoloMath.silverLine(w, h, HoloPose.ZERO)
        val s1 = HoloMath.silverLine(w, h, pose)
        // calc(50% + tx * 50%) on a 2.6 × tile: the tile moves by (w − 2.6 w) × 0.5 × tx.
        assertEquals(-1.6f * w * 0.5f * 0.6f, t1.startX - t0.startX, 0.01f)
        assertEquals(HoloMath.bandShift(w, HoloMath.TINT_TRAVEL, 0.6f), t1.startX - t0.startX, 0.01f)
        assertEquals(HoloMath.bandShift(h, HoloMath.TINT_TRAVEL, -0.4f), t1.startY - t0.startY, 0.01f)
        assertEquals(HoloMath.bandShift(w, HoloMath.SILVER_TRAVEL, 0.6f), s1.startX - s0.startX, 0.01f)
        assertTrue(abs(t1.startX - t0.startX) > abs(s1.startX - s0.startX))
        // The period vector never changes with the pose (the brushes are built once and only translated).
        assertEquals(t0.dx, t1.dx, 0.001f)
        assertEquals(t0.dy, t1.dy, 0.001f)
    }

    @Test
    fun visibleBoxStaysInsideTheTileAtFullTilt() {
        // background-position 0 … 100 % keeps the box inside the 2.6 × tile: no seam at ±1.
        for (t in listOf(-1f, 1f)) {
            val shift = HoloMath.bandShift(w, HoloMath.TINT_TRAVEL, t)
            val tileLeft = (w - 2.6f * w) * 0.5f + shift
            assertTrue(tileLeft <= 0f && tileLeft + 2.6f * w >= w)
        }
    }

    @Test
    fun sheenIsA24PercentBandOfThe140PercentBoxAndSlidesThirtyPercent() {
        val line = HoloMath.sheenLine(w, h, HoloPose.ZERO)
        assertEquals(cssLength(1.4f * w, 1.4f * h, 105.0), hypot(line.dx, line.dy), 0.01f)
        assertEquals(0.24f, HoloMath.SHEEN_TO - HoloMath.SHEEN_FROM, 0.0001f)
        val moved = HoloMath.sheenLine(w, h, HoloPose(0f, 0f, 1f))
        assertEquals(0.3f * 1.4f * w, moved.startX - line.startX, 0.01f)
        assertEquals(HoloMath.sheenShift(w, 1f), moved.startX - line.startX, 0.01f)
    }

    @Test
    fun periodOffsetRepeats() {
        val line = HoloMath.tintLine(w, h, HoloPose.ZERO)
        val a = line.at(10f, 20f)
        val b = line.at(10f + line.dx, 20f + line.dy)
        assertEquals(a, b, 0.0005f)
        assertTrue(a in 0f..1f)
    }

    @Test
    fun idleSwayFollowsTheBoardKeyframes() {
        val ease: (Float) -> Float = { CivilightMotion.holoIdle.easing.transform(it) }
        // hFoil 0 % = 100 %: background-position 20 % 30 % → tx −0.6, ty −0.4; 50 %: 80 % 70 % → +0.6, +0.4.
        val start = HoloMath.idlePose(0f, ease)
        val mid = HoloMath.idlePose(0.5f, ease)
        val end = HoloMath.idlePose(0.999999f, ease)
        assertEquals(-0.6f, start.x, 0.001f)
        assertEquals(-0.4f, start.y, 0.001f)
        assertEquals(0.6f, mid.x, 0.001f)
        assertEquals(0.4f, mid.y, 0.001f)
        assertEquals(-0.6f, end.x, 0.01f)
        // hSheen translateX ∓25 % of the 1.4 × box = 0.833 in tilt units at 30 % travel.
        assertEquals(0.25f / 0.3f, mid.sheen, 0.001f)
        assertEquals(7000, CivilightMotion.holoIdleMs)
    }

    @Test
    fun sweepGoesLeftToRightAndSettlesIntoTheLivePose() {
        assertEquals(-1f, HoloMath.sweepPose(0f).x, 0f)
        assertEquals(1f, HoloMath.sweepPose(1f).x, 0f)
        val live = HoloPose(0.2f, 0.1f)
        val settled = HoloMath.blend(HoloMath.sweepPose(1f), live, 1f)
        assertEquals(live.x, settled.x, 0.0001f)
        assertEquals(live.y, settled.y, 0.0001f)
        assertEquals(live.sheen, settled.sheen, 0.0001f)
        assertEquals(HoloMath.sweepPose(1f), HoloMath.blend(HoloMath.sweepPose(1f), live, 0f))
    }

    @Test
    fun livePoseAddsSensorDragAndWeightedIdleClamped() {
        val idle = HoloPose(0.6f, 0.4f, 0.83f)
        assertEquals(HoloPose(0.6f, 0.4f, 0.83f), HoloMath.live(0f, 0f, 0f, 0f, idle, 1f))
        // A finger holds the foil: the idle part goes away.
        assertEquals(HoloPose(0.3f, -0.2f, 0.3f), HoloMath.live(0f, 0f, 0.3f, -0.2f, idle, 0f))
        val full = HoloMath.live(0.9f, 0.9f, 0.9f, 0.9f, idle, 1f)
        assertEquals(1f, full.x, 0f)
        assertEquals(1f, full.y, 0f)
    }

    @Test
    fun dragAcrossTheSurfaceIsAFullTurn() {
        val (x, y) = HoloMath.drag(0f, 0f, 150f, -215f, w, h)
        assertEquals(1f, x, 0.0001f)
        assertEquals(-1f, y, 0.0001f)
    }

    @Test
    fun foilAndTintAlphasAreTheBoardsAtIntensityOne() {
        assertEquals(0.7f, HoloMath.foilAlpha(1f, 1f), 0.0001f)
        assertEquals(0.5f, HoloMath.tintAlpha(1f), 0.0001f)
        assertEquals(0f, HoloMath.foilAlpha(1f, 0f), 0f)
        assertTrue(HoloMath.foilAlpha(5f, 1f) <= 1f)
        // Resting pose under reduced motion is the board's `--tx .35, --ty -.2`.
        assertEquals(HoloPose(0.35f, -0.2f), HoloPose.STILL)
    }

    @Test
    fun boardStopsAndColours() {
        assertEquals(listOf(0f, 6f / 26f, 12f / 26f, 18f / 26f, 1f), HoloMath.SILVER_STOPS.toList())
        assertEquals(listOf(0x7E848C, 0xE9ECEF, 0xA3A9B1, 0xF6F7F9, 0x7E848C), HoloMath.SILVER_RGB.toList())
        assertEquals(listOf(0xFF96C8, 0x8CD2FF, 0xBEA0FF), HoloMath.TINT_RGB.toList())
    }

    @Test
    fun holoSourcesUseMotionTokensOnly() {
        val dir = File("src/main/java/io/github/lenticualrstone/civilight/designsystem/component")
        val issues = listOf("HoloSurface.kt", "HoloMath.kt").flatMap { name ->
            MotionLint.lint(name, File(dir, name).readText(), allowEasingDefinitions = true)
        }
        assertEquals("", issues.joinToString("\n"))
    }

    @Test
    fun shadersDeclareEveryUniformTheRendererSets() {
        val src = File("src/main/java/io/github/lenticualrstone/civilight/designsystem/component/HoloSurface.kt").readText()
        for (u in listOf("uniform float2 uTS;", "uniform float2 uTV;", "uniform float2 uSS;", "uniform float2 uSV;", "uniform float uTintA;", "uniform float2 uTilt;", "uniform float uCell;")) {
            assertTrue(u, src.contains(u))
        }
        for (u in listOf("\"uTS\"", "\"uTV\"", "\"uSS\"", "\"uSV\"", "\"uTintA\"", "\"uTilt\"", "\"uCell\"")) {
            assertTrue(u, src.contains(u))
        }
    }
}

/** 0.2.1: the iridescent band and the tilting holo card. */
class HoloIrisTest {
    @Test
    fun irisMovesWithTheTiltAndFadesWithTheFoil() {
        assertEquals(0f, HoloMath.irisPhase(0f, 0f), 0f)
        assertEquals(HoloMath.IRIS_TRAVEL, HoloMath.irisPhase(1f, 0f), 1e-6f)
        assertTrue(HoloMath.irisPhase(0f, 1f) > 0f)
        assertEquals(HoloMath.IRIS_ALPHA, HoloMath.irisAlpha(1f, 1f), 1e-6f)
        assertEquals(0f, HoloMath.irisAlpha(1f, 0f), 0f)
        assertEquals(500f * HoloMath.IRIS_PERIOD, HoloMath.irisPeriod(300f, 400f), 1e-3f)
    }

    @Test
    fun holoCardTurnsAsTheBoardsCard() {
        assertEquals(14f, HoloMath.CARD_TURN_X, 0f)
        assertEquals(16f, HoloMath.CARD_TURN_Y, 0f)
        assertEquals(900f, HoloMath.CARD_PERSPECTIVE_DP, 0f)
    }
}
