package io.github.lenticualrstone.civilight.domain.world.gen

import io.github.lenticualrstone.civilight.model.map.GenerationStep
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.TextCondition
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.open
import kotlin.math.roundToLong

/** A point in the map's 0–1 space, rounded to 4 decimals so stored results compare byte for byte. */
data class GenPoint(val x: Double, val y: Double) {
    companion object {
        fun of(x: Double, y: Double) = GenPoint(round4(x.coerceIn(0.0, 1.0)), round4(y.coerceIn(0.0, 1.0)))

        fun round4(v: Double): Double = (v * 10_000).roundToLong() / 10_000.0
    }
}

/** A generated town (no name: names come only from project records). */
data class GenTown(val x: Double, val y: Double, val cell: Int, val port: Boolean)

/**
 * One generated world: the same world for every format (WLD-53). Each step owns its layer:
 * `해안선` [land] + [coastline]; `산` [heights] + [relief] + [ridges]; `강` [rivers] + [lakes];
 * `숲 · 기후` [climate]; `마을 · 길` [towns] + [roads]. Cells are row-major on a [w] × [h] grid.
 *
 * [relief]: `.` none, `h` hill, `m` mountain, `p` peak. [climate]: `.` water, `g` plain, `f` forest,
 * `d` desert, `s` swamp, `n` snow. [heights] are 0–1000 on land (0 at sea).
 */
data class GeneratedWorld(
    val version: String,
    val w: Int,
    val h: Int,
    val land: String,
    val coastline: List<List<GenPoint>>,
    val heights: List<Int>,
    val relief: String,
    val ridges: List<List<GenPoint>>,
    val rivers: List<List<GenPoint>>,
    val lakes: List<Int>,
    val climate: String,
    val towns: List<GenTown>,
    val roads: List<List<GenPoint>>,
) {
    fun isLand(i: Int): Boolean = land[i] == '1' && i !in lakeSet

    private val lakeSet: Set<Int> by lazy { lakes.toSet() }
    private val seaDistance: IntArray by lazy { Grid(w, h).distanceTo { land[it] != '1' || it in lakeSet } }
    private val landDistance: IntArray by lazy { Grid(w, h).distanceTo { land[it] == '1' && it !in lakeSet } }

    /** Cells (grid steps) to the nearest water. */
    fun distanceToWater(i: Int): Int = seaDistance[i]

    /** The terrain class of a cell (hex cells, polygon colours, legend). */
    fun terrain(i: Int): Terrain = when {
        !isLand(i) -> if (landDistance[i] > DEEP_DISTANCE) Terrain.DEEP else Terrain.SEA
        climate[i] == 'n' -> Terrain.SNOW
        relief[i] == 'p' -> Terrain.PEAK
        relief[i] == 'm' -> Terrain.MOUNTAIN
        relief[i] == 'h' -> Terrain.HILL
        climate[i] == 'd' -> Terrain.DESERT
        climate[i] == 's' -> Terrain.SWAMP
        climate[i] == 'f' -> Terrain.FOREST
        else -> Terrain.PLAIN
    }

    /** The cell holding a 0–1 point. */
    fun cellAt(x: Double, y: Double): Int = Grid(w, h).cellAt(x, y)

    companion object {
        const val DEEP_DISTANCE = 3
    }
}

/** What the generator reads: base choices, the five steps (seed, lock, choices) and text conditions. */
data class GenInput(
    val kind: GeneratedMapKind,
    val landRatio: LandRatio,
    val steps: List<GenerationStep>,
    val conditions: List<TextCondition> = emptyList(),
    val w: Int = DEFAULT_W,
    val h: Int = DEFAULT_H,
) {
    fun step(kind: GenerationStepKind): GenerationStep =
        steps.firstOrNull { it.step.known == kind } ?: GenerationStep(kind.open(), seed = 0)

    fun seed(kind: GenerationStepKind): Int = step(kind).seed

    fun choice(kind: GenerationStepKind, key: String): String? = step(kind).choices[key]

    companion object {
        const val DEFAULT_W = 64
        const val DEFAULT_H = 48
    }
}

/** Grid helpers: cell centres, neighbours, multi-source distances. */
internal class Grid(val w: Int, val h: Int) {
    val size: Int get() = w * h

    fun x(i: Int): Double = (i % w + 0.5) / w
    fun y(i: Int): Double = (i / w + 0.5) / h
    fun col(i: Int): Int = i % w
    fun row(i: Int): Int = i / w

    fun cellAt(x: Double, y: Double): Int {
        val c = (x * w).toInt().coerceIn(0, w - 1)
        val r = (y * h).toInt().coerceIn(0, h - 1)
        return r * w + c
    }

    /** 3 × 3 area index of a cell (0 = north-west … 8 = south-east). */
    fun area(i: Int): Int = minOf(2, row(i) * 3 / h) * 3 + minOf(2, col(i) * 3 / w)

    fun isBorder(i: Int): Boolean = col(i) == 0 || row(i) == 0 || col(i) == w - 1 || row(i) == h - 1

    /** 8-neighbours in a fixed order. */
    fun neighbours8(i: Int): List<Int> {
        val c = col(i)
        val r = row(i)
        val out = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until h && nc in 0 until w) out += nr * w + nc
        }
        return out
    }

    fun neighbours4(i: Int): List<Int> {
        val c = col(i)
        val r = row(i)
        return buildList {
            if (r > 0) add(i - w)
            if (c > 0) add(i - 1)
            if (c < w - 1) add(i + 1)
            if (r < h - 1) add(i + w)
        }
    }

    /** Grid steps (8-connected) from every cell to the nearest cell where [target] holds; large when none. */
    fun distanceTo(target: (Int) -> Boolean): IntArray {
        val dist = IntArray(size) { Int.MAX_VALUE / 2 }
        val queue = ArrayDeque<Int>()
        for (i in 0 until size) if (target(i)) {
            dist[i] = 0
            queue += i
        }
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            for (n in neighbours8(i)) if (dist[n] > dist[i] + 1) {
                dist[n] = dist[i] + 1
                queue += n
            }
        }
        return dist
    }
}
