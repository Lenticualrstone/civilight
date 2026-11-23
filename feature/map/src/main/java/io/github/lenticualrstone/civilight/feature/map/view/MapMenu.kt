package io.github.lenticualrstone.civilight.feature.map.view

import io.github.lenticualrstone.civilight.feature.map.common.MapOffers
import io.github.lenticualrstone.civilight.domain.world.gen.TerrainGenerator
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.WorldMap

/** What a row of the map menu `⋯` does. */
enum class MapMenuAction {
    /** `이름` (WLD-36: maps are named by the author). */
    RENAME,

    /** The format editor: `칸 칠하기` (WLD-58), `다각형 · 지역 나누기` (WLD-59), `상세 지형 · 편집` (WLD-60). */
    FORMAT_EDIT,

    /** `단계 다시 만들기` (WLD-56). */
    REROLL,

    /** `인물 동선` (WLD-62). */
    TRACK,

    /** `지우기` (WLD-36 Left item: into the trash, `되돌리기` in the toast). */
    DELETE,
}

data class MapMenuRow(val action: MapMenuAction, val title: String, val subtitle: String? = null, val enabled: Boolean = true)

/** Which screen shows the menu. */
enum class MapMenuScreen { VIEW, EDIT, NODE }

/**
 * The rows the map menu adds above `축척 맞추기 · 시점별 공개 · 지도 내보내기`. The format editors,
 * re-roll and 인물 동선 have no other way in, so each one is a visible row here (`[PROPOSED entry:
 * edit ⋯]` for WLD-56). A generated map from another generator version shows `단계 다시 만들기`
 * disabled, so the refusal is visible.
 */
object MapMenu {
    fun formatEditTitle(map: WorldMap): String? = when (map.format?.known) {
        MapFormat.HEX -> map.hex?.let { MapCopy.HEX_TITLE }
        MapFormat.POLY -> map.poly?.let { MapCopy.POLY_TITLE }
        MapFormat.DETAIL -> MapCopy.DETAIL_TITLE
        else -> null
    }

    fun canReroll(map: WorldMap): Boolean = map.generation?.generatorVersion == TerrainGenerator.VERSION

    fun rows(map: WorldMap, screen: MapMenuScreen): List<MapMenuRow> {
        val out = ArrayList<MapMenuRow>()
        out += MapMenuRow(MapMenuAction.RENAME, MapCopy.MAP_NAME, map.name)
        // (0.2.0, WLD-75) Format editors, re-roll and 인물 동선 are hidden; their data stays.
        if (MapOffers.OFFERS_GENERATION && screen != MapMenuScreen.NODE) {
            formatEditTitle(map)?.let { out += MapMenuRow(MapMenuAction.FORMAT_EDIT, it) }
            if (map.baseKind.known == MapBaseKind.GENERATED && map.generation != null) {
                val ok = canReroll(map)
                out += MapMenuRow(MapMenuAction.REROLL, MapCopy.REROLL_TITLE, if (ok) null else MapCopy.REROLL_OTHER_VERSION, ok)
            }
        }
        if (MapOffers.OFFERS_TRACK && screen == MapMenuScreen.VIEW && !MapScales.isNodeMap(map)) out += MapMenuRow(MapMenuAction.TRACK, MapCopy.TRACK_TITLE)
        if (screen != MapMenuScreen.EDIT) out += MapMenuRow(MapMenuAction.DELETE, MapCopy.BTN_REMOVE)
        return out
    }
}
