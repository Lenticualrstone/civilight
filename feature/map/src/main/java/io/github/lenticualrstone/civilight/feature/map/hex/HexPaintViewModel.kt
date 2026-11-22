package io.github.lenticualrstone.civilight.feature.map.hex

import io.github.lenticualrstone.civilight.domain.world.map.HexCell
import io.github.lenticualrstone.civilight.domain.world.map.HexEdits
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.model.map.Terrain
import kotlinx.coroutines.Job

/** 칸 칠하기 (WLD-58): every paint, fill, line and setting is one undo step `칸 칠하기 · O09`. */
class HexPaintViewModel(projectKey: Key, val mapKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    private fun label(cell: HexCell?): String = MapCopy.step(MapCopy.HEX_TITLE, cell?.let(HexMaps::label))

    fun apply(commit: HexCommit): Job {
        val ctx = editContext()
        return when (commit) {
            is HexCommit.Paint -> commit(label(commit.cell)) { d -> HexMaps.paint(d, mapKey, commit.cell, commit.terrain, ctx) }
            is HexCommit.Fill -> commit(label(commit.cell)) { d -> HexMaps.fill(d, mapKey, commit.cell, commit.terrain, ctx)?.first }
            is HexCommit.Line -> commit(label(commit.cells.firstOrNull())) { d -> HexEdits.paintCells(d, mapKey, commit.cells, commit.terrain, ctx) }
        }
    }

    /** `장소 만들기`: a place with a pin at the cell centre, one step `핀 · <name>`. */
    fun createPlace(cell: HexCell, name: String): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.PIN, name.trim())) { d -> (HexMaps.createPlaceAt(d, mapKey, cell, name, ctx) as? MapEdit.Applied)?.doc }
    }

    fun setCellKm(km: Double): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.CELL_SIZE, MapCopy.km(io.github.lenticualrstone.civilight.domain.world.map.Travel.number(km)))) { d ->
            if (km <= 0.0 || !km.isFinite()) null else HexMaps.setCellKm(d, mapKey, km, ctx)
        }
    }

    fun setOrientation(orientation: HexOrientation): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.ORIENTATION, MapCopy.ORIENTATIONS[orientation.ordinal])) { d -> HexEdits.setOrientation(d, mapKey, orientation, ctx) }
    }

    fun setCoordEvery(every: Int): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.COORDS, HexPaintUiBuilder.coordsLabel(every))) { d -> HexEdits.setCoordEvery(d, mapKey, every, ctx) }
    }

    companion object {
        /** `24` or `24.5` from the `칸 크기` field; null when it is not a positive number. */
        fun parseKm(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }

        val PALETTE: List<Terrain> get() = HexPaintState.PALETTE
    }
}
