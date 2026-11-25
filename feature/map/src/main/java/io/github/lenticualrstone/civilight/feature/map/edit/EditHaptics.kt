package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.Stamp

/**
 * When the map editor plays the DS-15 `틱` (boards `MapRegionDraw`, `MapRouteDraw`, `MapStampPlace`): a vertex,
 * route point or 강 · 길 point placed; a stamp dropped; a stamp's rotation reaching another 15° detent; a stamp's
 * size reaching another step `1 · 2 · 3`. Nothing else ticks. Pure, so it is JVM-tested.
 */
object EditHaptics {
    /** Points placed so far in the draft being drawn (region, route or line). */
    fun draftPoints(s: EditorState): Int = (s.region?.vertices?.size ?: 0) + (s.route?.size ?: 0) + (s.line?.size ?: 0)

    /** A tap that placed a point ticks; an undo, a finish or a tool change does not. */
    fun placedPoint(before: EditorState, after: EditorState): Boolean = draftPoints(after) > draftPoints(before)

    /** A drag of the rotate handle toward [to] ticks when it lands on another detent. */
    fun rotationDetent(stamp: Stamp, to: Point): Boolean = StampHandles.rotationToward(Point(stamp.x, stamp.y), to) != stamp.rotation

    /** A drag of the size handle toward [to] ticks when it lands on another size step. */
    fun sizeStep(stamp: Stamp, to: Point): Boolean = StampHandles.sizeToward(Point(stamp.x, stamp.y), to) != stamp.size

    /** A size picked from the palette or segmented control ticks when it changes. */
    fun sizePicked(old: Int, new: Int): Boolean = old != new
}
