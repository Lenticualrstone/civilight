package io.github.lenticualrstone.civilight.feature.map.common

import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.WorldMap

/**
 * What a map screen offers. (0.3.0) Map making is back (r3 pages `지도 만들기`, `지도 생성 · 양식`): every base
 * kind is made and edited again (지형 만들기, 이미지, 스케치, 노드), with pins, regions, routes, labels, layers,
 * stamps and the per-point 공개. Node maps open in the node editor, every other map in 지도 보기 / 지도 편집.
 *
 * (0.3.0) Distances are back too (r3 `지도 ↔ 연표`, `세계 · 연표`): the scale bar and `축척 맞추기` on every map but a
 * node map, the `그림 · 색면` chip where a style applies (`MapViewState.styleChip`), route distances with travel
 * modes, and `인물 동선`. Data stored by 0.1.x / 0.2.x loads as before.
 */
object MapOffers {
    /** `편집` on the view's bottom bar: every map 지도 보기 shows (a node map opens in its own editor). */
    fun canEdit(map: WorldMap): Boolean = !MapScales.isNodeMap(map)

    /** `지도 이미지 바꾸기`: image and sketch maps (a generated or node map has no picture to replace). */
    fun canReplaceBase(map: WorldMap): Boolean = map.baseKind.known == MapBaseKind.IMAGE || map.baseKind.known == MapBaseKind.SKETCH

    /** `핀 옮기기` on the place sheet and side card: every map. */
    @Suppress("UNUSED_PARAMETER")
    fun canMovePin(map: WorldMap): Boolean = true

    /** `새 장소` on an empty spot of 지도에서 고르기: every map that can be picked on. */
    @Suppress("UNUSED_PARAMETER")
    fun canDropPin(map: WorldMap): Boolean = true

    /** The `그림 · 색면` style chip (WLD-57): every map whose style applies (the chip itself is null elsewhere). */
    fun showsStyleChip(map: WorldMap): Boolean = !MapScales.isNodeMap(map)

    /** Scale bar, `축척 맞추기` (WLD-46): every map but a node map (0.1.0). */
    fun showsScale(map: WorldMap): Boolean = !MapScales.isNodeMap(map)

    /** Routes are drawn and edited (WLD-43). */
    const val DRAWS_ROUTES: Boolean = true

    /** `인물 동선` (WLD-62). */
    const val OFFERS_TRACK: Boolean = true

    /** Format editors (`칸 칠하기 · 다각형 · 상세 지형`) and `단계 다시 만들기` (WLD-56 … WLD-60). */
    const val OFFERS_GENERATION: Boolean = true

    /**
     * `지형 만들기 · 글로 설명` (WLD-61): on device only. The keyword reader reads the text here; the `기기 안 · 클라우드`
     * choice is not offered (the cloud reader has no model and sends nothing), so the 설정 `글로 설명` engine row
     * stays hidden too.
     */
    const val OFFERS_TEXT_TERRAIN: Boolean = true
}
