package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.PolyMap
import io.github.lenticualrstone.civilight.model.map.PolyProvince

/**
 * Polygon (province) editing (WLD-59): `칸 넣기`, `합치기`, `나누기`, `땅 · 바다`. A province is
 * 이름 · 색 · 연결 세력; the list shows `<이름> | 칸 N`.
 */
object PolyMaps {
    private fun withPoly(doc: ProjectDoc, mapKey: Key, ctx: EditContext, f: (PolyMap) -> PolyMap?): ProjectDoc? {
        val poly = doc.maps[mapKey]?.poly ?: return null
        val next = f(poly) ?: return null
        return MapEdits.updateMap(doc, mapKey, ctx) { it.copy(poly = next) }
    }

    /** Number of cells of a province (`칸 37`). */
    fun cellCount(poly: PolyMap, provinceKey: Key): Int = poly.cellProvince.values.count { it == provinceKey }

    /** `<이름> | 칸 N` rows in province order. */
    fun rows(poly: PolyMap): List<Pair<String, String>> = poly.provinces.map { it.name to "칸 ${cellCount(poly, it.key)}" }

    /** `새 지역`. */
    fun addProvince(doc: ProjectDoc, mapKey: Key, name: String, color: String, ctx: EditContext, factionKey: Key? = null): Pair<ProjectDoc, Key>? {
        val key = ctx.newKey()
        val next = withPoly(doc, mapKey, ctx) { it.copy(provinces = it.provinces + PolyProvince(key, name.trim(), color, factionKey)) } ?: return null
        return next to key
    }

    /** `칸 넣기`: the cells join [provinceKey] (floating `칸 1 → 하층 구역`). */
    fun assign(doc: ProjectDoc, mapKey: Key, cells: Collection<Int>, provinceKey: Key, ctx: EditContext): ProjectDoc? = withPoly(doc, mapKey, ctx) { p ->
        if (p.provinces.none { it.key == provinceKey }) return@withPoly null
        p.copy(cellProvince = p.cellProvince + cells.filter { it in p.cells.indices }.associateWith { provinceKey })
    }

    /** `합치기`: [from] joins [into]; [from] is removed. */
    fun merge(doc: ProjectDoc, mapKey: Key, from: Key, into: Key, ctx: EditContext): ProjectDoc? = withPoly(doc, mapKey, ctx) { p ->
        if (from == into || p.provinces.none { it.key == into }) return@withPoly null
        p.copy(
            cellProvince = p.cellProvince.mapValues { (_, k) -> if (k == from) into else k },
            provinces = p.provinces.filterNot { it.key == from },
        )
    }

    /** `나누기`: the chosen cells of a province form a new province (WLD-59 default). */
    fun split(doc: ProjectDoc, mapKey: Key, provinceKey: Key, cells: Collection<Int>, name: String, color: String, ctx: EditContext): Pair<ProjectDoc, Key>? {
        val poly = doc.maps[mapKey]?.poly ?: return null
        val chosen = cells.filter { poly.cellProvince[it] == provinceKey }
        if (chosen.isEmpty()) return null
        val (added, key) = addProvince(doc, mapKey, name, color, ctx) ?: return null
        val assigned = assign(added, mapKey, chosen, key, ctx) ?: return null
        return assigned to key
    }

    /** `땅 · 바다`: flips one cell. */
    fun toggleLand(doc: ProjectDoc, mapKey: Key, cell: Int, ctx: EditContext): ProjectDoc? = withPoly(doc, mapKey, ctx) { p ->
        if (cell !in p.cells.indices) return@withPoly null
        p.copy(cells = p.cells.mapIndexed { i, c -> if (i == cell) c.copy(land = !c.land) else c })
    }

    fun updateProvince(doc: ProjectDoc, mapKey: Key, provinceKey: Key, ctx: EditContext, f: (PolyProvince) -> PolyProvince): ProjectDoc? =
        withPoly(doc, mapKey, ctx) { p -> p.copy(provinces = p.provinces.map { if (it.key == provinceKey) f(it) else it }) }
}
