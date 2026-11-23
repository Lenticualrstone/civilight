package io.github.lenticualrstone.civilight.feature.map.common

import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.WorldMap

/**
 * (0.2.0, S-3, WLD-75, D11) What a map screen offers. A new map is one image plus place pins. Maps of
 * every other base kind a 0.1.0 project holds stay listed and open **view-only** (base, pins, regions,
 * labels, layers). Distances (scale, routes, travel modes), character routes, generation and the
 * node / terrain / sketch tools are hidden, never deleted. Every map screen asks this object, so one
 * place decides what is reachable.
 */
object MapOffers {
    /** True for the one kind 0.2.0 edits: an `IMAGE` map. */
    fun editable(map: WorldMap): Boolean = map.baseKind.known == MapBaseKind.IMAGE

    /** `편집` on the view's bottom bar. */
    fun canEdit(map: WorldMap): Boolean = editable(map)

    /** `지도 이미지 바꾸기`: image maps only (sketch maps are view-only now). */
    fun canReplaceBase(map: WorldMap): Boolean = editable(map)

    /** `핀 옮기기` on the place sheet and side card. */
    fun canMovePin(map: WorldMap): Boolean = editable(map)

    /** The `그림 · 색면` style chip (generated maps are view-only, WLD-57 hidden). */
    @Suppress("UNUSED_PARAMETER")
    fun showsStyleChip(map: WorldMap): Boolean = false

    /** Scale bar, `축척 맞추기` (WLD-46 hidden). */
    @Suppress("UNUSED_PARAMETER")
    fun showsScale(map: WorldMap): Boolean = false

    /** Routes and their chips are not drawn (WLD-43 hidden); the stored routes are kept. */
    const val DRAWS_ROUTES: Boolean = false

    /** `인물 동선` (WLD-62 hidden). */
    const val OFFERS_TRACK: Boolean = false

    /** Format editors and `단계 다시 만들기` (WLD-56 … WLD-60 hidden). */
    const val OFFERS_GENERATION: Boolean = false

    /** The `새 지도` sheet's `지형 만들기 · 스케치 촬영 · 노드 지도` (WLD-36 superseded): a new map is an image. */
    const val NEW_MAP_IMAGE_ONLY: Boolean = true
}
