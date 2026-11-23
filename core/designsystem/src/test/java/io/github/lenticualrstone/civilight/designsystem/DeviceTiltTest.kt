package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.card.TiltMath
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-DS-52 (DS-74): the shared tilt source of the 서재 views and the character card views. */
class DeviceTiltTest {
    @Test
    fun `T-DS-52 offset is tilt times depth, depth capped at 8`() {
        assertEquals(4f, TiltMath.offset(0.5f, 8f), 0f)
        assertEquals(-8f, TiltMath.offset(-1f, 8f), 0f)
        assertEquals(8f, TiltMath.offset(1f, 20f), 0f)
        assertEquals(-8f, TiltMath.offset(-3f, 8f), 0f)
    }

    @Test
    fun `T-DS-52 a 20 degree pitch is clamped to full tilt, relative to the starting pose`() {
        assertEquals(1f, TiltMath.normalized(20f, 0f), 0f)
        assertEquals(-1f, TiltMath.normalized(-20f, 0f), 0f)
        assertEquals(0f, TiltMath.normalized(30f, 30f), 0f)
        assertEquals(0.5f, TiltMath.normalized(36f, 30f), 0.001f)
        // Wraps across ±180.
        assertEquals(0.5f, TiltMath.normalized(-178f, 176f), 0.001f)
    }

    @Test
    fun `T-DS-52 low-pass reaches about 63 percent after one time constant`() {
        assertEquals(0.2f, TiltMath.lowPass(0.2f, 1f, 0f), 0f)
        val one = TiltMath.lowPass(0f, 1f, TiltMath.TAU_MS)
        assertEquals(0.632f, one, 0.01f)
        var v = 0f
        repeat(60) { v = TiltMath.lowPass(v, 1f, 16f) }
        assertTrue(v > 0.99f)
    }

    @Test
    fun `T-DS-52 display rotation maps gravity to screen axes`() {
        assertEquals(0f, TiltMath.lean(TiltMath.screenX(0f, 1f, 0)), 0.01f)
        assertEquals(-1f, TiltMath.screenX(0f, 1f, 1), 0f)
        assertEquals(1f, TiltMath.screenY(1f, 0f, 1), 0f)
    }

    @Test
    fun `T-DS-52 disabled tilt is still at zero`() {
        assertEquals(0f, DeviceTilt.Still.x, 0f)
        assertEquals(0f, DeviceTilt.Still.y, 0f)
    }

    @Test
    fun `3D view tokens keep their wave 3 values`() {
        assertEquals(90_000L, CivilightMotion.starOrbitPeriodMs)
        assertEquals(CivilightMotion.holoTiltStiffness, CivilightMotion.viewSettleStiffness, 0f)
        assertEquals(1f, CivilightMotion.viewSettleDamping, 0f)
    }
}
