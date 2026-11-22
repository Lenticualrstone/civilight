package io.github.lenticualrstone.civilight.domain.world.gen

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.map.HexCell
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.DrawnLine
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.model.map.LineOrigin
import io.github.lenticualrstone.civilight.model.map.PolyCell
import io.github.lenticualrstone.civilight.model.map.PolyMap
import io.github.lenticualrstone.civilight.model.map.PolyProvince
import io.github.lenticualrstone.civilight.model.map.Stamp
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.open

/**
 * The same world drawn per format (WLD-53, WLD-57 … WLD-60): hex cells, polygon cells grouped into
 * provinces, detailed-terrain rivers, roads and stamps. Province names are left empty (names come
 * only from project records).
 */
object GenFormats {
    /** Hex cells (32 × 24, pointy, 24 km by default): the terrain under each cell centre. */
    fun hex(world: GeneratedWorld, cols: Int = 32, rows: Int = 24, orientation: HexOrientation = HexOrientation.POINTY, cellKm: Double = 24.0): HexGrid {
        val grid = HexGrid(cellKm = cellKm, orientation = orientation.open(), cols = cols, rows = rows)
        val cells = (0 until rows * cols).map { i ->
            val p = HexMaps.center(grid, HexMaps.cellOf(grid, i))
            world.terrain(world.cellAt(p.x.toDouble(), p.y.toDouble())).open()
        }
        return grid.copy(cells = cells)
    }

    /**
     * Polygon cells: [sites] Lloyd-relaxed (2 rounds) Voronoi cells on the world grid, grouped into
     * [provinces] provinces grown from spaced seeds over neighbouring land cells.
     */
    fun poly(world: GeneratedWorld, seed: Int, ctx: EditContext, sites: Int = 96, provinces: Int = 8): PolyMap {
        val grid = Grid(world.w, world.h)
        val rng = Rng(Hash.stream(seed, 7))
        val cols = 12
        val rowsN = (sites + cols - 1) / cols
        var points = (0 until sites).map { k ->
            val c = k % cols
            val r = k / cols
            doubleArrayOf((c + 0.2 + rng.nextDouble() * 0.6) / cols, (r + 0.2 + rng.nextDouble() * 0.6) / rowsN)
        }
        fun owners(pts: List<DoubleArray>): IntArray = IntArray(grid.size) { i ->
            val x = grid.x(i)
            val y = grid.y(i)
            var best = 0
            var bestD = Double.MAX_VALUE
            for ((k, p) in pts.withIndex()) {
                val d = (x - p[0]) * (x - p[0]) + (y - p[1]) * (y - p[1])
                if (d < bestD) {
                    bestD = d
                    best = k
                }
            }
            best
        }
        repeat(2) {
            val own = owners(points)
            val sx = DoubleArray(sites)
            val sy = DoubleArray(sites)
            val n = IntArray(sites)
            for (i in 0 until grid.size) {
                sx[own[i]] += grid.x(i)
                sy[own[i]] += grid.y(i)
                n[own[i]]++
            }
            points = points.mapIndexed { k, p -> if (n[k] == 0) p else doubleArrayOf(sx[k] / n[k], sy[k] / n[k]) }
        }
        val own = owners(points)
        val land = BooleanArray(sites) { k -> world.isLand(world.cellAt(points[k][0], points[k][1])) }
        val adjacency = Array(sites) { sortedSetOf<Int>() }
        for (i in 0 until grid.size) for (n in grid.neighbours4(i)) if (own[n] != own[i]) {
            adjacency[own[i]] += own[n]
            adjacency[own[n]] += own[i]
        }
        // Spaced province seeds: farthest-point sampling over land sites.
        val landSites = (0 until sites).filter { land[it] }
        val seeds = ArrayList<Int>()
        if (landSites.isNotEmpty()) seeds += landSites[rng.nextInt(landSites.size)]
        while (seeds.size < minOf(provinces, landSites.size)) {
            val next = landSites.filter { it !in seeds }.maxWithOrNull(compareBy<Int> { s ->
                seeds.minOf { t -> (points[s][0] - points[t][0]).let { it * it } + (points[s][1] - points[t][1]).let { it * it } }
            }.thenByDescending { it }) ?: break
            seeds += next
        }
        val province = IntArray(sites) { -1 }
        val queue = ArrayDeque<Int>()
        seeds.forEachIndexed { p, s -> province[s] = p; queue += s }
        while (queue.isNotEmpty()) {
            val s = queue.removeFirst()
            for (n in adjacency[s]) if (land[n] && province[n] < 0) {
                province[n] = province[s]
                queue += n
            }
        }
        // Land sites on other landmasses join the nearest province.
        for (s in landSites) if (province[s] < 0 && seeds.isNotEmpty()) {
            province[s] = seeds.indices.minWith(compareBy<Int> { p ->
                val t = seeds[p]
                (points[s][0] - points[t][0]).let { it * it } + (points[s][1] - points[t][1]).let { it * it }
            })
        }
        val keys = seeds.map { ctx.newKey() }
        return PolyMap(
            cells = points.mapIndexed { k, p -> PolyCell(Point(p[0].toFloat(), p[1].toFloat()), land[k]) },
            cellProvince = (0 until sites).filter { province[it] >= 0 }.associateWith { keys[province[it]] },
            provinces = keys.mapIndexed { p, key -> PolyProvince(key, name = "", color = "map-art-${p % 5 + 1}") },
        )
    }

    private fun points(line: List<GenPoint>): List<Point> = line.map { Point(it.x.toFloat(), it.y.toFloat()) }

    /** Generated rivers and roads of a detailed-terrain map. */
    fun drawnLines(world: GeneratedWorld, ctx: EditContext, kinds: Set<DrawnLineKind> = setOf(DrawnLineKind.RIVER, DrawnLineKind.ROAD)): List<DrawnLine> {
        val now = ctx.now()
        val rivers = if (DrawnLineKind.RIVER in kinds) world.rivers.map {
            DrawnLine(ctx.newKey(), DrawnLineKind.RIVER.open(), points(it), LineOrigin.GENERATED.open(), updatedAt = now, updatedBy = ctx.device)
        } else emptyList()
        val roads = if (DrawnLineKind.ROAD in kinds) world.roads.map {
            DrawnLine(ctx.newKey(), DrawnLineKind.ROAD.open(), points(it), LineOrigin.GENERATED.open(), updatedAt = now, updatedBy = ctx.device)
        } else emptyList()
        return rivers + roads
    }

    /** Stamps of a detailed-terrain map: peaks and mountains along ridges, forest marks, towns and ports. */
    fun stamps(world: GeneratedWorld, ctx: EditContext): List<Stamp> {
        val grid = Grid(world.w, world.h)
        val now = ctx.now()
        val out = ArrayList<Stamp>()
        fun stamp(kind: StampKind, i: Int, size: Int) {
            out += Stamp(
                key = ctx.newKey(), kind = kind.open(), x = GenPoint.round4(grid.x(i)).toFloat(), y = GenPoint.round4(grid.y(i)).toFloat(),
                size = size, updatedAt = now, updatedBy = ctx.device,
            )
        }
        // Every 4th cell of a lattice keeps marks sparse and stable.
        for (i in 0 until grid.size) {
            if (grid.col(i) % 4 != 1 || grid.row(i) % 4 != 1) continue
            when (world.terrain(i)) {
                Terrain.PEAK -> stamp(StampKind.MOUNTAIN, i, 3)
                Terrain.MOUNTAIN -> stamp(StampKind.MOUNTAIN, i, 2)
                Terrain.HILL -> stamp(StampKind.HILL, i, 1)
                Terrain.FOREST -> stamp(StampKind.FOREST, i, 1)
                else -> Unit
            }
        }
        for (t in world.towns) stamp(if (t.port) StampKind.PORT else StampKind.TOWN, t.cell, 2)
        return out
    }

    /** The world-grid cell under a hex cell (for tests and the `고르기` info). */
    fun worldCellOf(world: GeneratedWorld, grid: HexGrid, cell: HexCell): Int {
        val p = HexMaps.center(grid, cell)
        return world.cellAt(p.x.toDouble(), p.y.toDouble())
    }
}
