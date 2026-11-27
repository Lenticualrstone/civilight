package io.github.lenticualrstone.civilight.ui.nav

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.Timeline
import io.github.lenticualrstone.civilight.navigation.World

/**
 * Metadata the shell adds to every entry, so scene strategies can see the route (a `NavEntry`'s key
 * is private). Overlay routes need no transition of their own: they are Nav3 overlay scenes that fade
 * themselves (`OverlaySceneStrategy`, motion audit S2).
 */
object RouteMetadata {
    const val ROUTE = "civilight.route"

    /** [ROUTE] for [key]. */
    fun of(key: NavKey): Map<String, Any> = mapOf(ROUTE to key)
}

/** The route of an entry made by the shell. */
val NavEntry<*>.route: NavKey? get() = metadata[RouteMetadata.ROUTE] as? NavKey

/**
 * The list | detail pairs of CNV-33 (`Two*` boards): 인물 목록 | 인물 · 용어/장소/세력 목록 | 카드 ·
 * 연표 | 사건 · 기록함 | 기록 · 점검 | 원고 위치.
 * Library and ProjectHome stay standalone full-screen views on unfolded displays (avoiding cramped splits
 * and jarring rail/pane rebuilds on forward navigation).
 */
object ListDetailPairs {
    fun isList(key: NavKey?): Boolean = moduleOf(key) != null

    /** The CNV-47 ratio id (`PaneSplit.Modules`) of a list module, or null for any other route. */
    fun moduleOf(list: NavKey?): String? = when {
        list is CastList -> PaneSplit.Modules.CAST
        list is World -> PaneSplit.Modules.WORLD
        list is Timeline -> PaneSplit.Modules.TIMELINE
        list is RecorderInbox -> PaneSplit.Modules.RECORDER
        list is CheckCenter -> PaneSplit.Modules.CHECKS
        else -> null
    }

    fun isPair(list: NavKey?, detail: NavKey?): Boolean = when (detail) {
        is CharacterProfile -> list is CastList && list.projectKey == detail.projectKey
        is TermCard -> list is World && list.projectKey == detail.projectKey
        is PlaceCard -> list is World && list.projectKey == detail.projectKey
        is FactionCard -> list is World && list.projectKey == detail.projectKey
        is EventDetail -> list is Timeline && list.projectKey == detail.projectKey
        is RecorderRecordDetail -> list is RecorderInbox
        is Script -> list is CheckCenter && list.projectKey == detail.projectKey
        else -> false
    }

    /**
     * (0.4.0) Only Script draws its own specialized input bar; CharacterProfile shares the unified
     * List-Detail bottom command bar in unfolded two-pane mode (eliminating duplicate bottom bars).
     */
    fun detailDrawsOwnBar(detail: NavKey?): Boolean = detail is Script
}
