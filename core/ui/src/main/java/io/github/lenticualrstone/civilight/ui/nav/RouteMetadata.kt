package io.github.lenticualrstone.civilight.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.OverlayRoute
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
 * is private).
 */
object RouteMetadata {
    const val ROUTE = "civilight.route"

    private const val OVERLAY_FADE_MS = 220

    /** [ROUTE] plus, for overlay routes, a fade instead of the push slide. */
    fun of(key: NavKey): Map<String, Any> {
        val transitions = if (key is OverlayRoute) {
            val fade: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform =
                { fadeIn(tween(OVERLAY_FADE_MS)) togetherWith fadeOut(tween(OVERLAY_FADE_MS)) }
            metadata {
                put(NavDisplay.TransitionKey, fade)
                put(NavDisplay.PopTransitionKey, fade)
            }
        } else {
            emptyMap()
        }
        return transitions + (ROUTE to key)
    }
}

/** The route of an entry made by the shell. */
val NavEntry<*>.route: NavKey? get() = metadata[RouteMetadata.ROUTE] as? NavKey

/**
 * The list | detail pairs of CNV-33 (`Two*` boards): 서재 | 프로젝트 홈 · 홈 | 스토리 구조 ·
 * 인물 목록 | 인물 · 용어 목록 | 용어 카드 · 연표 | 사건 · 기록함 | 기록 · 점검 | 원고 위치.
 * Only these pairs share the screen; any other stack shows one screen.
 */
object ListDetailPairs {
    fun isList(key: NavKey?): Boolean =
        key == Library || key is ProjectHome || key is CastList || key is World || key is Timeline || key is RecorderInbox || key is CheckCenter

    fun isPair(list: NavKey?, detail: NavKey?): Boolean = when (detail) {
        is ProjectHome -> list == Library
        is StoryStructure -> list is ProjectHome && list.projectKey == detail.projectKey
        is CharacterProfile -> list is CastList && list.projectKey == detail.projectKey
        is TermCard -> list is World && list.projectKey == detail.projectKey
        is PlaceCard -> list is World && list.projectKey == detail.projectKey
        is FactionCard -> list is World && list.projectKey == detail.projectKey
        is EventDetail -> list is Timeline && list.projectKey == detail.projectKey
        is RecorderRecordDetail -> list is RecorderInbox
        is Script -> list is CheckCenter && list.projectKey == detail.projectKey
        else -> false
    }
}
