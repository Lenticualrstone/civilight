package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse

/** A tilt, each axis −1 … 1 (the DS `holoFoil` input). */
data class Tilt(val x: Float, val y: Float) {
    companion object {
        val ZERO = Tilt(0f, 0f)

        /** DS `HoloDefaultTilt`: the still foil under reduced motion. */
        val STILL = Tilt(0.35f, -0.2f)
    }
}

/**
 * Holo rules (CNV-30): where it may show, its starting value, the turn-on sweep and how tilt is read.
 * Pure, so the sensor lifecycle and the reduce rule are tested on the JVM (T-CNV-34, T-CNV-55).
 */
object HoloRules {
    /** The sweep when holo is switched on: one pass left → right, decelerate 480 ms (CNV-45). */
    val SWEEP: MotionToken = CivilightMotion.holoSweep

    /** Device tilt that reads as a full turn of the foil: ±25° (pitch and roll). */
    const val FULL_TILT_DEG = 25f

    /**
     * Released drag (board `Holo`: `손을 떼면 천천히 흔들림`): a damped spring back to rest, so the system
     * animator scale applies. A period of 1.4 s and a decay of 0.9 s give a stiffness of (2π / 1.4 s)² ≈ 20 and a
     * damping ratio of about 0.25.
     */
    const val SWAY_DAMPING = CivilightMotion.holoSwayDamping
    const val SWAY_STIFFNESS = CivilightMotion.holoSwayStiffness

    /** The sweep fires on the toggle's edge only: [trigger] moved while holo is [on] (not on restore or a tile change). */
    fun sweepEdge(previousTrigger: Int, trigger: Int, on: Boolean): Boolean = on && trigger != previousTrigger

    /** A point [fraction] of the way from [from] to [to]: the sweep's end settles into the live tilt on `standard`. */
    fun blend(from: Tilt, to: Tilt, fraction: Float): Tilt {
        val f = fraction.coerceIn(0f, 1f)
        return Tilt(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f)
    }

    /** The holo row shows only for slots that may carry holo (T-CNV-34). */
    fun showToggle(slot: ImageSlot): Boolean = ImageSlots.holoAllowed(slot)

    /**
     * The toggle's starting value: the current image's holo, or for a new image `홀로그램 효과 기본`
     * (CNV-19); always off where holo is not allowed.
     */
    fun initial(slot: ImageSlot, current: ImageUse?, holoDefaultForNewImages: Boolean): Boolean =
        ImageSlots.holoAllowed(slot) && (current?.holo ?: holoDefaultForNewImages)

    /** Whether the slot's image draws holo now. */
    fun shows(use: ImageUse?): Boolean = use != null && use.holo && use.slot.known?.let(ImageSlots::holoAllowed) == true

    /** The sweep to play when holo turns on, or null under reduced motion (static foil, no sweep). */
    fun sweepOnTurnOn(reduced: Boolean): MotionToken? = if (reduced) null else SWEEP

    /** The sweep's tilt at [fraction] 0 … 1 of its (eased) progress: x from −1 to 1. */
    fun sweepTilt(fraction: Float): Tilt = Tilt(-1f + 2f * fraction.coerceIn(0f, 1f), 0f)

    /** Tilt from the device's pitch and roll (degrees) relative to where it was held at the start. */
    fun fromDevice(pitchDeg: Float, rollDeg: Float, basePitchDeg: Float, baseRollDeg: Float): Tilt = Tilt(
        ((rollDeg - baseRollDeg) / FULL_TILT_DEG).coerceIn(-1f, 1f),
        ((pitchDeg - basePitchDeg) / FULL_TILT_DEG).coerceIn(-1f, 1f),
    )

    /** Tilt from a drag of ([dx], [dy]) px over a surface of [width] × [height] (no gyro). */
    fun fromDrag(start: Tilt, dx: Float, dy: Float, width: Float, height: Float): Tilt = Tilt(
        (start.x + 2f * dx / width.coerceAtLeast(1f)).coerceIn(-1f, 1f),
        (start.y + 2f * dy / height.coerceAtLeast(1f)).coerceIn(-1f, 1f),
    )

}

/** The platform side of the tilt sensor; faked in tests. */
interface TiltSensorPort {
    /** False when the device has no rotation sensor (drag to tilt instead). */
    val available: Boolean

    fun register()

    fun unregister()
}

/**
 * Keeps the rotation sensor registered only while a holo image is visible and motion is not reduced
 * (CNV-30 `화면 밖이면 기울기 센서 끔`, T-CNV-55). Each visible holo surface calls [shown] / [hidden].
 */
class HoloSensorGate(private val port: TiltSensorPort) {
    private var visible = 0
    private var reduced = false
    var registered: Boolean = false
        private set

    fun shown() {
        visible++
        sync()
    }

    fun hidden() {
        visible = (visible - 1).coerceAtLeast(0)
        sync()
    }

    fun reducedMotion(value: Boolean) {
        reduced = value
        sync()
    }

    /** True when holo surfaces read the sensor; false = drag to tilt (no gyro) or still (reduced). */
    val usesSensor: Boolean get() = port.available && !reduced

    private fun sync() {
        val want = visible > 0 && !reduced && port.available
        if (want && !registered) {
            port.register()
            registered = true
        } else if (!want && registered) {
            port.unregister()
            registered = false
        }
    }
}
