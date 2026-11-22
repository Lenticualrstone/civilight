package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import kotlinx.serialization.Serializable

/** 지도 목록 · 새 지도 (WLD-36). */
@Serializable
data class MapList(override val projectKey: Key) : MapRoute, ProjectRoute

/** 지도 보기 (WLD-37, WLD-38, WLD-41, WLD-48 breadcrumb), optionally centred on [focusPinKey]. */
@Serializable
data class MapView(override val projectKey: Key, val mapKey: Key, val focusPinKey: Key? = null) : MapRoute, ProjectRoute, LensScreen

/** 지도 편집 (WLD-40 … WLD-45) incl. 경로 sheet. */
@Serializable
data class MapEdit(override val projectKey: Key, val mapKey: Key) : MapRoute, ProjectRoute

/** 축척 맞추기 (WLD-46). */
@Serializable
data class MapScale(override val projectKey: Key, val mapKey: Key) : MapRoute, ProjectRoute

/** 시점별 공개 (WLD-49). */
@Serializable
data class MapReveal(override val projectKey: Key, val mapKey: Key) : MapRoute, ProjectRoute, LensScreen

/** 노드 지도 (WLD-50). */
@Serializable
data class NodeMap(override val projectKey: Key, val mapKey: Key) : MapRoute, ProjectRoute

/** 양식 고르기 (WLD-54, WLD-57). */
@Serializable
data class MapFormatPicker(override val projectKey: Key) : MapRoute, ProjectRoute

/** 단계별 만들기 (WLD-54 … WLD-56) for the chosen format id. */
@Serializable
data class MapGenerate(override val projectKey: Key, val format: String) : MapRoute, ProjectRoute

/** 육각 칸 칠하기 (WLD-58). */
@Serializable
data class HexPaint(override val projectKey: Key, val mapKey: Key) : MapRoute, ProjectRoute

/** 다각형 지역 (WLD-59). */
@Serializable
data class PolygonRegions(override val projectKey: Key, val mapKey: Key) : MapRoute, ProjectRoute

/** 상세 지형 도장 (WLD-60). */
@Serializable
data class DetailStamps(override val projectKey: Key, val mapKey: Key) : MapRoute, ProjectRoute

/** 글로 설명 · 결과 (WLD-61); [mapKey] null = a new map. */
@Serializable
data class TextMap(override val projectKey: Key, val mapKey: Key? = null) : MapRoute, ProjectRoute

/** 인물 동선 (WLD-62). */
@Serializable
data class CharacterTrack(override val projectKey: Key, val characterKey: Key? = null, val mapKey: Key? = null) : MapRoute, ProjectRoute, LensScreen

/**
 * `지도에서 고르기 ›` (WLD-52): picks a place or region on a map and returns it through
 * `NavResults` under [requestKey]; [sceneKey] is the scene being placed, if any.
 */
@Serializable
data class PickOnMap(override val projectKey: Key, val requestKey: String, val sceneKey: Key? = null) : MapRoute, ProjectRoute
