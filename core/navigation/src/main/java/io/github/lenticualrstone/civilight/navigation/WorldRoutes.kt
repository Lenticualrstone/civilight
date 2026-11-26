package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import kotlinx.serialization.Serializable

/**
 * 세계: 용어 · 장소 · 세력 lists (WLD-01 … WLD-05) on [tab]. [createTerm] = open the new-term form
 * on arrival (LIB-13 `세계 용어 하나 적기`). The world screen consumes it once with an entry-scoped
 * `rememberSaveable` "handled" flag (do not replace the route: another route value is another entry,
 * which would reset the screen's view model, saved state and sheets).
 */
@Serializable
data class World(
    override val projectKey: Key,
    val tab: WorldTab = WorldTab.TERMS,
    val createTerm: Boolean = false,
) : WorldRoute, ProjectRoute, LensScreen, ListPaneRoute

@Serializable
enum class WorldTab { TERMS, PLACES, FACTIONS }

/** 용어 카드 (WLD-08, WLD-16, WLD-19 … WLD-25). */
@Serializable
data class TermCard(override val projectKey: Key, val termKey: Key) : WorldRoute, ProjectRoute, LensScreen, DetailPaneRoute

/** 장소 카드 with tabs `개요 · 이곳의 시간 · 지도` (WLD-09, WLD-63). */
@Serializable
data class PlaceCard(
    override val projectKey: Key,
    val placeKey: Key,
    val tab: PlaceTab = PlaceTab.OVERVIEW,
) : WorldRoute, ProjectRoute, LensScreen, DetailPaneRoute

@Serializable
enum class PlaceTab { OVERVIEW, TIME_HERE, MAP }

/** 세력 카드 (WLD-10). */
@Serializable
data class FactionCard(override val projectKey: Key, val factionKey: Key) : WorldRoute, ProjectRoute, LensScreen, DetailPaneRoute

/** 연표 (WLD-31, WLD-34, WLD-35). */
@Serializable
data class Timeline(override val projectKey: Key) : WorldRoute, ProjectRoute, LensScreen, ListPaneRoute

/** 사건 상세 (WLD-33). */
@Serializable
data class EventDetail(override val projectKey: Key, val eventKey: Key) : WorldRoute, ProjectRoute, LensScreen, DetailPaneRoute

/** 연표 + 지도 (WLD-64, WLD-65). */
@Serializable
data class TimelineMap(override val projectKey: Key) : WorldRoute, ProjectRoute, LensScreen

/** 프로젝트 설정 › 세계 시간 and the world-calendar editor (WLD-29, LIB-11). */
@Serializable
data class WorldCalendar(override val projectKey: Key) : WorldRoute, ProjectRoute

/** 프로젝트 설정 › 이동 수단 (WLD-30, STO-28). */
@Serializable
data class TravelModes(override val projectKey: Key) : WorldRoute, ProjectRoute

/** (2026-11-26) 프로젝트 설정 › 묶음 기준 (board `ImmPlaceRules`): the character fields read as 소속 · 거주 · 국적. */
@Serializable
data class AffiliationBasisSettings(override val projectKey: Key) : WorldRoute, ProjectRoute
