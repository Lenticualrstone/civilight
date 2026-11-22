package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import kotlinx.serialization.Serializable

/**
 * 프로젝트 홈 (STO-01, LIB-13 checklist). Inner `TwoHome`: home is the list pane and
 * [StoryStructure] the detail pane; it is also the detail pane of [Library] (`TwoLibrary`).
 */
@Serializable
data class ProjectHome(override val projectKey: Key) : StoryRoute, ProjectRoute, LensScreen, ListPaneRoute, DetailPaneRoute

/** 스토리 구조 (STO-04 … STO-11); no lens chip. [focusUnitKey] = episode to expand or scene to show. */
@Serializable
data class StoryStructure(override val projectKey: Key, val focusUnitKey: Key? = null) : StoryRoute, ProjectRoute, DetailPaneRoute

/** 점검 센터 (STO-39 … STO-42, CNV-17); no lens chip. [filter] = selected chip id. */
@Serializable
data class CheckCenter(override val projectKey: Key, val filter: String? = null) : StoryRoute, ProjectRoute, ListPaneRoute

/** 검색 · 명령 in a project (STO-36 … STO-38), glass over the blurred screen. */
@Serializable
data class ProjectSearch(override val projectKey: Key, val query: String = "") : StoryRoute, ProjectRoute, OverlayRoute

/** 모든 프로젝트에서 찾기 from the 서재 command bar (LIB-07 scope, STO-37 engine). */
@Serializable
data class LibrarySearch(val query: String = "") : StoryRoute, OverlayRoute

/** 장소 · 때 한꺼번에 (STO-31 … STO-35); its 장소 · 때 · 제안 sheets are local. */
@Serializable
data class BulkPlaceTime(override val projectKey: Key) : StoryRoute, ProjectRoute

/** 프로젝트 설정 › 때 표기 (STO-17). */
@Serializable
data class SceneNotationSettings(override val projectKey: Key) : StoryRoute, ProjectRoute

/** 프로젝트 설정 › 하루 · 시간대 (STO-19). */
@Serializable
data class DayBandSettings(override val projectKey: Key) : StoryRoute, ProjectRoute
