package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/**
 * Interval arithmetic on stored base chains, used only to rewrite intervals when the script
 * splits, merges or deletes scenes (SCR-50/51/52) so absolute times stay. It never resolves a
 * time for display (that is the place & time area's engine).
 */
internal object TimeChains {
    /** A day offset range; [approx] when any link on the way is `대략`. */
    data class Offset(val min: Int, val max: Int, val approx: Boolean) {
        operator fun plus(o: Offset) = Offset(min + o.min, max + o.max, approx || o.approx)
        operator fun minus(o: Offset) = Offset(min - o.max, max - o.min, approx || o.approx)

        companion object {
            val ZERO = Offset(0, 0, false)
        }
    }

    fun offsetOf(t: SceneTime.Relative): Offset =
        Offset(t.dayMin, t.dayMax, t.precision.known == TimePrecision.APPROX || t.dayMin != t.dayMax)

    /** Base chain of a scene: (scene, its relative time) pairs down to the first non-relative scene. */
    private fun chain(doc: ProjectDoc, sceneKey: Key): List<Pair<Key, SceneTime>> {
        val out = mutableListOf<Pair<Key, SceneTime>>()
        val seen = mutableSetOf<Key>()
        var key: Key? = sceneKey
        while (key != null && seen.add(key)) {
            val time = doc.index.unit(key)?.time ?: break
            out += key to time
            key = (time as? SceneTime.Relative)?.baseKey
        }
        return out
    }

    /**
     * `day(to) − day(from)` from stored chains, cancelling the shared part of both chains; null when
     * the chains have no common scene and do not both end in a story-day fixed point.
     */
    fun offset(doc: ProjectDoc, from: Key, to: Key): Offset? {
        if (from == to) return Offset.ZERO
        val a = chain(doc, from)
        val b = chain(doc, to)
        val aKeys = a.map { it.first }
        val common = b.firstOrNull { it.first in aKeys }?.first
        fun sumUntil(c: List<Pair<Key, SceneTime>>, stop: Key?): Offset? {
            var acc = Offset.ZERO
            for ((k, t) in c) {
                if (k == stop) return acc
                acc = when (t) {
                    is SceneTime.Relative -> acc + offsetOf(t)
                    is SceneTime.Direct -> return if (stop == null && t.storyDay != null) acc + Offset(t.storyDay!!, t.storyDay!!, false) else null
                    else -> return null
                }
            }
            return if (stop == null) null else acc
        }
        val bOff = sumUntil(b, common) ?: return null
        val aOff = sumUntil(a, common) ?: return null
        return bOff - aOff
    }

    /** [time] re-based on [newBase] after adding [shift] to its interval (precision widened when uncertain). */
    fun rebased(time: SceneTime.Relative, newBase: Key, shift: Offset, rebasedFrom: Key?): SceneTime.Relative {
        val o = offsetOf(time) + shift
        return time.copy(
            baseKey = newBase, dayMin = o.min, dayMax = o.max,
            precision = (if (o.approx) TimePrecision.APPROX else TimePrecision.EXACT).open(),
            rebasedFromKey = rebasedFrom,
        )
    }

    /** Same time as [base] (`{base} 기준 · 같은 때`): interval 0, its precision, band and clock. */
    fun sameTimeAs(doc: ProjectDoc, baseKey: Key): SceneTime {
        return when (val t = doc.index.unit(baseKey)?.time ?: SceneTime.None) {
            is SceneTime.Relative -> SceneTime.Relative(baseKey, 0, 0, t.precision, t.band, t.clock)
            is SceneTime.Direct -> SceneTime.Relative(baseKey, 0, 0, TimePrecision.EXACT.open(), t.band, t.clock)
            is SceneTime.Flashback -> t
            else -> SceneTime.None
        }
    }

    fun isTimed(time: SceneTime): Boolean = time is SceneTime.Relative || time is SceneTime.Direct || time is SceneTime.Flashback
}
