package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.image.DeviceAngles
import io.github.lenticualrstone.civilight.feature.canvas.image.HoloRules
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** U-03: holo tilt from gravity (smooth when held upright) and the gallery's sweep trigger (GAL-17). */
class HoloTiltRulesTest {
    private fun deg(r: Double) = Math.toDegrees(r).toFloat()

    @Test
    fun `flat device reads no lean and no pitch`() {
        val a = HoloRules.anglesFromGravity(0f, 0f, 1f)
        assertEquals(0f, a.roll, 0.0001f)
        assertEquals(0f, a.pitch, 0.0001f)
    }

    @Test
    fun `near flat the angles equal getOrientation's roll and pitch (same signs as before)`() {
        val x = 0.1f
        val z = sqrt(1f - x * x)
        // getOrientation: roll = atan2(-R6, R8), pitch = asin(-R7).
        assertEquals(deg(atan2(-x.toDouble(), z.toDouble())), HoloRules.anglesFromGravity(x, 0f, z).roll, 0.01f)
        assertEquals(deg(asin(-x.toDouble())), HoloRules.anglesFromGravity(0f, x, z).pitch, 0.01f)
    }

    @Test
    fun `held upright a side lean of 10 degrees reads 0_4, not a jump to the end`() {
        val base = HoloRules.anglesFromGravity(0f, 1f, 0f)
        val r = Math.toRadians(10.0)
        val now = HoloRules.anglesFromGravity(sin(r).toFloat(), cos(r).toFloat(), 0f)
        val t = HoloRules.fromAngles(now, base)
        assertEquals(-0.4f, t.x, 0.001f)
        assertEquals(0f, t.y, 0.001f)
    }

    @Test
    fun `held upright tipping back 10 degrees reads on the pitch axis`() {
        val base = HoloRules.anglesFromGravity(0f, 1f, 0f)
        val r = Math.toRadians(10.0)
        val now = HoloRules.anglesFromGravity(0f, cos(r).toFloat(), sin(r).toFloat())
        val t = HoloRules.fromAngles(now, base)
        assertEquals(0f, t.x, 0.001f)
        assertEquals(0.4f, t.y, 0.001f)
    }

    @Test
    fun `display rotation turns device axes into screen axes`() {
        assertEquals(1f to 2f, HoloRules.screenAxes(1f, 2f, 0))
        assertEquals(2f to -1f, HoloRules.screenAxes(1f, 2f, 1))
        assertEquals(-1f to -2f, HoloRules.screenAxes(1f, 2f, 2))
        assertEquals(-2f to 1f, HoloRules.screenAxes(1f, 2f, 3))
    }

    @Test
    fun `angle deltas wrap and tilt clamps`() {
        assertEquals(20f, HoloRules.angleDelta(170f, -170f), 0.001f)
        assertEquals(-20f, HoloRules.angleDelta(-170f, 170f), 0.001f)
        val t = HoloRules.fromAngles(DeviceAngles(pitch = 80f, roll = -80f), DeviceAngles(0f, 0f))
        assertEquals(-1f, t.x, 0f)
        assertEquals(1f, t.y, 0f)
    }

    @Test
    fun `sweep trigger rises only on off to on`() {
        assertEquals(1, HoloRules.toggleTrigger(wasOn = false, on = true, trigger = 0))
        assertEquals(1, HoloRules.toggleTrigger(wasOn = true, on = true, trigger = 1))
        assertEquals(1, HoloRules.toggleTrigger(wasOn = true, on = false, trigger = 1))
        assertEquals(2, HoloRules.toggleTrigger(wasOn = false, on = false, trigger = 2))
        // Shown first with holo on: no rise (the caller starts wasOn at the item's value).
        assertEquals(0, HoloRules.toggleTrigger(wasOn = true, on = true, trigger = 0))
        // Another item takes the place (viewer 이전 · 다음, a re-sort): the trigger stays, so no sweep fires.
        assertEquals(1, HoloRules.toggleTrigger(wasOn = false, on = true, trigger = 1, sameItem = false))
    }
}
