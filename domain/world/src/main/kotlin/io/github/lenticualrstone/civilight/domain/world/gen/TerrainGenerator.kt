package io.github.lenticualrstone.civilight.domain.world.gen

import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.TextCondition
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The one terrain generator (WLD-53), a pure function of [GenInput]: same version, seeds and choices
 * give the same [GeneratedWorld]. Steps run in order `해안선 → 산 → 강 → 숲 · 기후 → 마을 · 길`; each
 * step reads only the layers above it, its own seed and its own choices, so a locked step keeps its
 * layer when a later step is re-rolled (WLD-T61). Text conditions bias the height and moisture
 * inputs; they never draw (WLD-61). No place names are generated.
 *
 * Simplifications against the reference (`mapgeo.py`, informative only): a 64 × 48 grid, ridges as
 * gaussian bumps along seeded polylines, rivers on an 8-neighbour priority-flood, climate from
 * moisture quantiles without a temperature model, towns from coast and lowland only (not from
 * rivers, so re-rolling `강` changes only the river layer), roads as straight minimum-spanning-tree
 * edges per landmass.
 */
object TerrainGenerator {
    const val VERSION = "gen-1"

    fun generate(input: GenInput): GeneratedWorld {
        val grid = Grid(input.w, input.h)
        val coast = coast(input, grid)
        val mountains = mountains(input, grid, coast)
        val rivers = rivers(input, grid, coast, mountains)
        val climate = climate(input, grid, coast, mountains)
        val towns = towns(input, grid, coast, mountains, climate)
        return GeneratedWorld(
            version = VERSION, w = input.w, h = input.h,
            land = String(CharArray(grid.size) { if (coast.land[it]) '1' else '0' }),
            coastline = coast.coastline,
            heights = mountains.heights.toList(),
            relief = String(mountains.relief),
            ridges = mountains.ridges,
            rivers = rivers.lines,
            lakes = rivers.lakes,
            climate = String(climate),
            towns = towns.first,
            roads = towns.second,
        )
    }

    // --- Conditions -------------------------------------------------------------------------------

    private class Bias(val kind: ConditionKind, val area: Set<Int>)

    private fun biases(conditions: List<TextCondition>): List<Bias> =
        conditions.mapNotNull { c -> ConditionKind.of(c.what)?.let { Bias(it, TextConditions.area(c)) } }

    private val LAND_KINDS = setOf(
        ConditionKind.MOUNTAINS, ConditionKind.DESERT, ConditionKind.FOREST, ConditionKind.SWAMP,
        ConditionKind.SNOW, ConditionKind.PLAIN, ConditionKind.TOWN,
    )

    /** Centre of a 3 × 3 area. */
    private fun areaCenter(a: Int): Pair<Double, Double> = ((a % 3) + 0.5) / 3 to ((a / 3) + 0.5) / 3

    // --- 해안선 ------------------------------------------------------------------------------------

    internal class Coast(val land: BooleanArray, val base: DoubleArray, val coastline: List<List<GenPoint>>)

    private fun landFraction(ratio: LandRatio): Double = when (ratio) {
        LandRatio.SEA_MORE -> 0.35
        LandRatio.HALF -> 0.5
        LandRatio.LAND_MORE -> 0.68
    }

    private fun coast(input: GenInput, grid: Grid): Coast {
        val seed = input.seed(GenerationStepKind.COAST)
        val noiseSeed = Hash.stream(seed, 1)
        val rng = Rng(Hash.stream(seed, 2))
        val jx = (rng.nextDouble() - 0.5) * 0.1
        val jy = (rng.nextDouble() - 0.5) * 0.1
        val jx2 = (rng.nextDouble() - 0.5) * 0.1
        val jy2 = (rng.nextDouble() - 0.5) * 0.1
        val biases = biases(input.conditions)
        val height = DoubleArray(grid.size) { i ->
            val x = grid.x(i)
            val y = grid.y(i)
            fun ellipse(cx: Double, cy: Double, rx: Double, ry: Double): Double {
                val dx = (x - cx) / rx
                val dy = (y - cy) / ry
                return 1 - (dx * dx + dy * dy)
            }
            var h = when (input.kind) {
                GeneratedMapKind.CONTINENT -> 0.6 * ValueNoise.fbm(noiseSeed, x, y) + 0.6 * ellipse(0.5 + jx, 0.5 + jy, 0.38, 0.34)
                GeneratedMapKind.TWIN -> 0.6 * ValueNoise.fbm(noiseSeed, x, y) +
                    0.6 * maxOf(ellipse(0.28 + jx, 0.5 + jy, 0.2, 0.3), ellipse(0.72 + jx2, 0.5 + jy2, 0.2, 0.3))
                GeneratedMapKind.ISLANDS -> {
                    val dx = x - 0.5
                    val dy = y - 0.5
                    ValueNoise.fbm(noiseSeed, x, y, scale = 2) - 0.5 * sqrt(dx * dx + dy * dy)
                }
                GeneratedMapKind.REGION -> ValueNoise.fbm(noiseSeed, x, y)
            }
            val area = grid.area(i)
            for (b in biases) {
                if (area !in b.area) continue
                when (b.kind) {
                    in LAND_KINDS -> h += 0.35
                    ConditionKind.SEA, ConditionKind.PORT -> {
                        // Sea on the outer side of the area, land on the inner side.
                        val (cx, cy) = areaCenter(area)
                        val ox = cx - 0.5
                        val oy = cy - 0.5
                        val len = sqrt(ox * ox + oy * oy)
                        val t = if (len == 0.0) 0.0 else ((x - cx) * ox + (y - cy) * oy) / (len * (1.0 / 6))
                        h += 0.25 - 0.55 * t.coerceIn(-1.0, 1.0) - (if (len == 0.0) 0.3 else 0.0)
                    }
                    ConditionKind.LAKE -> {
                        val (cx, cy) = areaCenter(area)
                        val d = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
                        h += 0.2 - 0.9 * StrictMath.exp(-(d * d) / (0.06 * 0.06))
                    }
                    else -> Unit
                }
            }
            h
        }
        val sea = quantile(height.toList(), 1 - landFraction(input.landRatio))
        val land = BooleanArray(grid.size) { height[it] >= sea }
        val top = (0 until grid.size).filter { land[it] }.maxOfOrNull { height[it] } ?: sea
        val span = (top - sea).takeIf { it > 0 } ?: 1.0
        val base = DoubleArray(grid.size) { if (land[it]) (height[it] - sea) / span else 0.0 }
        return Coast(land, base, Contours.trace(grid, land))
    }

    // --- 산 ----------------------------------------------------------------------------------------

    internal class Mountains(val heights: IntArray, val relief: CharArray, val ridges: List<List<GenPoint>>)

    private fun mountains(input: GenInput, grid: Grid, coast: Coast): Mountains {
        val seed = input.seed(GenerationStepKind.MOUNTAIN)
        val rng = Rng(Hash.stream(seed, 3))
        val ranges = input.choice(GenerationStepKind.MOUNTAIN, StepChoices.RANGES) ?: StepChoices.RANGES_DEFAULT
        val amp = when (input.choice(GenerationStepKind.MOUNTAIN, StepChoices.HEIGHT) ?: StepChoices.HEIGHT_DEFAULT) {
            "낮게" -> 0.25
            "높게" -> 0.55
            else -> 0.4
        }
        val landCells = (0 until grid.size).filter { coast.land[it] }
        val ridges = ArrayList<List<GenPoint>>()
        fun ridge(length: Double, points: Int): List<GenPoint>? {
            if (landCells.isEmpty()) return null
            val start = landCells[rng.nextInt(landCells.size)]
            var x = grid.x(start)
            var y = grid.y(start)
            var angle = rng.nextDouble() * Math.PI * 2
            val step = length / (points - 1)
            val out = ArrayList<GenPoint>()
            for (k in 0 until points) {
                out += GenPoint.of(x, y)
                angle += (rng.nextDouble() - 0.5) * 0.6
                x = (x + StrictMath.cos(angle) * step).coerceIn(0.02, 0.98)
                y = (y + StrictMath.sin(angle) * step).coerceIn(0.02, 0.98)
            }
            return out
        }
        val scattered = ranges == "흩어짐"
        val count = when (ranges) {
            "1" -> 1
            "3" -> 3
            "흩어짐" -> 7
            else -> 2
        }
        val ridgeAmp = ArrayList<Double>()
        repeat(count) {
            val r = if (scattered) ridge(0.08 + rng.nextDouble() * 0.06, 3) else ridge(0.25 + rng.nextDouble() * 0.25, 6)
            if (r != null) {
                ridges += r
                ridgeAmp += if (scattered) amp * 0.7 else amp
            }
        }
        // Conditions: a ridge across each `산맥` area, always.
        for (b in biases(input.conditions).filter { it.kind == ConditionKind.MOUNTAINS }) {
            val cells = b.area.sorted()
            val (x0, y0) = areaCenter(cells.first())
            val (x1, y1) = areaCenter(cells.last())
            val pts = if (cells.size == 1) {
                listOf(GenPoint.of(x0 - 0.12, y0), GenPoint.of(x0 + 0.12, y0))
            } else {
                // Stretch along the band (east-west for a north/south band, north-south for east/west).
                val horizontal = abs(x1 - x0) >= abs(y1 - y0)
                if (horizontal) listOf(GenPoint.of(0.06, y0), GenPoint.of(0.5, y0 + 0.02), GenPoint.of(0.94, y1))
                else listOf(GenPoint.of(x0, 0.06), GenPoint.of(x0 + 0.02, 0.5), GenPoint.of(x1, 0.94))
            }
            ridges += pts
            ridgeAmp += amp * 1.3
        }
        val width = 0.045
        val heights = DoubleArray(grid.size) { i ->
            if (!coast.land[i]) return@DoubleArray 0.0
            var h = coast.base[i] * 0.6
            val p = GenPoint(grid.x(i), grid.y(i))
            for ((k, r) in ridges.withIndex()) {
                val d = Contours.distanceToPolyline(p, r)
                h += ridgeAmp[k] * StrictMath.exp(-(d * d) / (width * width))
            }
            h
        }
        val landHeights = landCells.map { heights[it] }
        val hill = quantile(landHeights, 0.72)
        val mountain = quantile(landHeights, 0.88)
        val peak = quantile(landHeights, 0.96)
        val top = landHeights.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val relief = CharArray(grid.size) { i ->
            when {
                !coast.land[i] -> '.'
                heights[i] >= peak && ridges.isNotEmpty() -> 'p'
                heights[i] >= mountain && ridges.isNotEmpty() -> 'm'
                heights[i] >= hill -> 'h'
                else -> '.'
            }
        }
        val ints = IntArray(grid.size) { if (coast.land[it]) (heights[it] / top * 1000).toInt().coerceIn(1, 1000) else 0 }
        return Mountains(ints, relief, ridges)
    }

    // --- 강 ----------------------------------------------------------------------------------------

    internal class Rivers(val lines: List<List<GenPoint>>, val lakes: List<Int>)

    private fun rivers(input: GenInput, grid: Grid, coast: Coast, m: Mountains): Rivers {
        val seed = input.seed(GenerationStepKind.RIVER)
        val rng = Rng(Hash.stream(seed, 4))
        val count = when (input.choice(GenerationStepKind.RIVER, StepChoices.COUNT) ?: StepChoices.COUNT_DEFAULT) {
            "적게" -> 2
            "많이" -> 7
            else -> 4
        }
        val meander = when (input.choice(GenerationStepKind.RIVER, StepChoices.BENDS) ?: StepChoices.BENDS_DEFAULT) {
            "곧게" -> 0.15
            "많이" -> 0.6
            else -> 0.35
        }
        val lakesOn = (input.choice(GenerationStepKind.RIVER, StepChoices.LAKES) ?: StepChoices.LAKES_DEFAULT) == StepChoices.ON
        val outletAtBorder = input.kind == GeneratedMapKind.REGION

        // Priority flood: every land cell drains to the sea (or, for 한 지역, off the map edge).
        val filled = IntArray(grid.size) { Int.MAX_VALUE }
        val queue = PriorityQueue<Long>()
        fun push(i: Int, h: Int) {
            filled[i] = h
            queue += (h.toLong() shl 32) or i.toLong()
        }
        for (i in 0 until grid.size) {
            if (!coast.land[i]) push(i, 0) else if (outletAtBorder && grid.isBorder(i)) push(i, m.heights[i] * SCALE)
        }
        val downstream = IntArray(grid.size) { -1 }
        while (queue.isNotEmpty()) {
            val packed = queue.poll()
            val i = (packed and 0xFFFFFFFFL).toInt()
            val h = (packed ushr 32).toInt()
            if (h != filled[i]) continue
            for (n in grid.neighbours8(i)) {
                if (filled[n] != Int.MAX_VALUE || !coast.land[n]) continue
                downstream[n] = i
                push(n, maxOf(m.heights[n] * SCALE, h + 1))
            }
        }
        val landCells = (0 until grid.size).filter { coast.land[it] }
        val highCut = quantile(landCells.map { m.heights[it].toDouble() }, 0.7)
        val candidates = rng.shuffled(landCells.filter { m.heights[it] >= highCut })
        val onRiver = HashSet<Int>()
        val sources = ArrayList<Int>()
        // Conditions `강`: one source in each named area first (the highest cell, ties by index).
        for (b in biases(input.conditions).filter { it.kind == ConditionKind.RIVER }) {
            landCells.filter { grid.area(it) in b.area && it !in sources }
                .maxWithOrNull(compareBy<Int> { m.heights[it] }.thenByDescending { it })?.let { sources += it }
        }
        val wanted = count + sources.size
        for (c in candidates) {
            if (sources.size >= wanted) break
            if (sources.none { s -> abs(grid.col(s) - grid.col(c)) + abs(grid.row(s) - grid.row(c)) < MIN_SOURCE_SPACING }) sources += c
        }
        val lines = ArrayList<List<GenPoint>>()
        val lakes = LinkedHashSet<Int>()
        for (s in sources) {
            if (s in onRiver) continue
            val path = ArrayList<Int>()
            var cur = s
            while (cur >= 0 && coast.land[cur] && path.size < grid.size) {
                path += cur
                if (cur in onRiver && path.size > 1) break
                cur = downstream[cur]
            }
            if (cur >= 0 && !coast.land[cur]) path += cur
            if (path.size < MIN_RIVER_CELLS) continue
            onRiver += path
            if (lakesOn) for (c in path) if (coast.land[c] && filled[c] - m.heights[c] * SCALE > LAKE_DEPTH * SCALE) lakes += c
            lines += path.mapIndexed { k, c ->
                if (k == 0 || k == path.size - 1) {
                    GenPoint.of(grid.x(c), grid.y(c))
                } else {
                    val prev = path[k - 1]
                    val next = path[k + 1]
                    val dx = grid.x(next) - grid.x(prev)
                    val dy = grid.y(next) - grid.y(prev)
                    val len = sqrt(dx * dx + dy * dy).takeIf { it > 0 } ?: 1.0
                    val off = (rng.nextDouble() - 0.5) * meander / grid.w
                    GenPoint.of(grid.x(c) - dy / len * off, grid.y(c) + dx / len * off)
                }
            }
        }
        return Rivers(lines, lakes.toList())
    }

    // --- 숲 · 기후 -----------------------------------------------------------------------------------

    private fun climate(input: GenInput, grid: Grid, coast: Coast, m: Mountains): CharArray {
        val seed = input.seed(GenerationStepKind.FOREST_CLIMATE)
        val noiseSeed = Hash.stream(seed, 5)
        val seaDist = grid.distanceTo { !coast.land[it] }
        val biases = biases(input.conditions)
        val moisture = DoubleArray(grid.size) { i ->
            if (!coast.land[i]) return@DoubleArray 0.0
            var v = ValueNoise.fbm(noiseSeed, grid.x(i), grid.y(i)) + 0.25 * StrictMath.exp(-seaDist[i] / 4.0)
            val area = grid.area(i)
            for (b in biases) if (area in b.area) when (b.kind) {
                ConditionKind.DESERT -> v -= 0.8
                ConditionKind.FOREST -> v += 0.6
                ConditionKind.SWAMP -> v += 0.9
                else -> Unit
            }
            v
        }
        val land = (0 until grid.size).filter { coast.land[it] }
        val values = land.map { moisture[it] }
        val dry = quantile(values, 0.18)
        val wet = quantile(values, 0.62)
        val soaked = quantile(values, 0.95)
        val low = quantile(land.map { m.heights[it].toDouble() }, 0.25)
        val out = CharArray(grid.size) { i ->
            if (!coast.land[i]) return@CharArray '.'
            val rugged = m.relief[i] == 'm' || m.relief[i] == 'p'
            when {
                rugged -> 'g'
                moisture[i] < dry -> 'd'
                moisture[i] >= soaked && m.heights[i] <= low -> 's'
                moisture[i] >= wet -> 'f'
                else -> 'g'
            }
        }
        // Condition areas take their class where the cell allows it (land, not rugged).
        for (b in biases) {
            val cells = land.filter { grid.area(it) in b.area }
            if (cells.isEmpty()) continue
            val mid = quantile(cells.map { moisture[it] }, 0.5)
            for (i in cells) {
                val rugged = m.relief[i] == 'm' || m.relief[i] == 'p'
                when (b.kind) {
                    ConditionKind.SNOW -> out[i] = 'n'
                    ConditionKind.DESERT -> if (!rugged && moisture[i] <= mid) out[i] = 'd'
                    ConditionKind.FOREST -> if (!rugged && moisture[i] >= mid) out[i] = 'f'
                    ConditionKind.SWAMP -> if (!rugged && m.relief[i] == '.') out[i] = 's'
                    ConditionKind.PLAIN -> if (!rugged) out[i] = 'g'
                    else -> Unit
                }
            }
        }
        return out
    }

    // --- 마을 · 길 -----------------------------------------------------------------------------------

    private fun towns(input: GenInput, grid: Grid, coast: Coast, m: Mountains, climate: CharArray): Pair<List<GenTown>, List<List<GenPoint>>> {
        val seed = input.seed(GenerationStepKind.TOWN_ROAD)
        val rng = Rng(Hash.stream(seed, 6))
        val seaDist = grid.distanceTo { !coast.land[it] }
        val biases = biases(input.conditions)
        val score = DoubleArray(grid.size)
        val candidates = ArrayList<Int>()
        for (i in 0 until grid.size) {
            val noise = rng.nextDouble()
            if (!coast.land[i] || m.relief[i] != '.' || climate[i] == 'd' || climate[i] == 's' || climate[i] == 'n') continue
            var s = noise * 0.3 + (1 - m.heights[i] / 1000.0) * 0.3 + (if (seaDist[i] <= 2) 0.5 else 0.0)
            val area = grid.area(i)
            for (b in biases) if (area in b.area) when (b.kind) {
                ConditionKind.TOWN -> s += 2.0
                ConditionKind.PORT -> if (seaDist[i] <= 1) s += 3.0
                else -> Unit
            }
            score[i] = s
            candidates += i
        }
        val target = when (input.kind) {
            GeneratedMapKind.CONTINENT -> 6
            GeneratedMapKind.TWIN -> 7
            GeneratedMapKind.ISLANDS -> 6
            GeneratedMapKind.REGION -> 5
        } + biases.count { it.kind == ConditionKind.TOWN || it.kind == ConditionKind.PORT }
        val picked = ArrayList<Int>()
        for (c in candidates.sortedWith(compareByDescending<Int> { score[it] }.thenBy { it })) {
            if (picked.size >= target) break
            if (picked.none { p -> abs(grid.col(p) - grid.col(c)) + abs(grid.row(p) - grid.row(c)) < MIN_TOWN_SPACING }) picked += c
        }
        val towns = picked.map { GenTown(GenPoint.round4(grid.x(it)), GenPoint.round4(grid.y(it)), it, port = seaDist[it] <= 1) }
        // Roads: a minimum spanning tree per landmass (Prim; ties by index).
        val mass = IntArray(grid.size) { -1 }
        var id = 0
        for (i in 0 until grid.size) {
            if (!coast.land[i] || mass[i] >= 0) continue
            val queue = ArrayDeque(listOf(i))
            mass[i] = id
            while (queue.isNotEmpty()) {
                val c = queue.removeFirst()
                for (n in grid.neighbours8(c)) if (coast.land[n] && mass[n] < 0) {
                    mass[n] = id
                    queue += n
                }
            }
            id++
        }
        val roads = ArrayList<List<GenPoint>>()
        for ((_, group) in towns.groupBy { mass[it.cell] }.toSortedMap()) {
            if (group.size < 2) continue
            val inTree = BooleanArray(group.size).also { it[0] = true }
            repeat(group.size - 1) {
                var best: Pair<Int, Int>? = null
                var bestD = Double.MAX_VALUE
                for (a in group.indices) if (inTree[a]) for (b in group.indices) if (!inTree[b]) {
                    val dx = group[a].x - group[b].x
                    val dy = group[a].y - group[b].y
                    val d = dx * dx + dy * dy
                    if (d < bestD) {
                        bestD = d
                        best = a to b
                    }
                }
                val (a, b) = best ?: return@repeat
                inTree[b] = true
                roads += listOf(GenPoint.of(group[a].x, group[a].y), GenPoint.of(group[b].x, group[b].y))
            }
        }
        return towns to roads
    }

    // --- Helpers -----------------------------------------------------------------------------------

    /** The [q] quantile (nearest rank) of [values]. */
    internal fun quantile(values: List<Double>, q: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        return sorted[((sorted.size - 1) * q).toInt().coerceIn(0, sorted.size - 1)]
    }

    private const val SCALE = 1000
    private const val LAKE_DEPTH = 40
    private const val MIN_SOURCE_SPACING = 8
    private const val MIN_RIVER_CELLS = 4
    private const val MIN_TOWN_SPACING = 7
}
