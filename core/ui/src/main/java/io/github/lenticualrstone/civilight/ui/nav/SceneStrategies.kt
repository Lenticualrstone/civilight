package io.github.lenticualrstone.civilight.ui.nav

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.github.lenticualrstone.civilight.navigation.OverlayRoute
import io.github.lenticualrstone.civilight.ui.window.ListDetailPanes

/** Which pane an entry is drawn in; lets a screen drop its own back button in the detail pane. */
enum class PaneRole { SINGLE, LIST, DETAIL }

val LocalPaneRole = androidx.compose.runtime.staticCompositionLocalOf { PaneRole.SINGLE }

/**
 * `목록 + 상세` on inner screens (CNV-33): when the top two routes are a [ListDetailPairs] pair, or
 * the top route is a list module alone, both panes share the screen. The scene key is the list
 * entry's, so choosing another item only swaps the right pane (no screen transition).
 */
class ListDetailSceneStrategy(private val enabled: () -> Boolean) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        if (!enabled()) return null
        val last = entries.lastOrNull() ?: return null
        val before = entries.getOrNull(entries.lastIndex - 1)
        return when {
            before != null && ListDetailPairs.isPair(before.route, last.route) ->
                ListDetailScene(before, last, previousEntries = entries.dropLast(1))
            ListDetailPairs.isList(last.route) ->
                ListDetailScene(last, null, previousEntries = entries.dropLast(1))
            else -> null
        }
    }
}

private class ListDetailScene(
    private val list: NavEntry<NavKey>,
    private val detail: NavEntry<NavKey>?,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val key: Any = list.contentKey
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(list, detail)
    override val content: @Composable () -> Unit = {
        ListDetailPanes(
            list = { CompositionLocalProvider(LocalPaneRole provides PaneRole.LIST) { list.Content() } },
            detail = detail?.let { d -> { CompositionLocalProvider(LocalPaneRole provides PaneRole.DETAIL) { d.Content() } } },
        )
    }

    override fun equals(other: Any?): Boolean =
        other is ListDetailScene && other.list == list && other.detail == detail && other.previousEntries == previousEntries

    override fun hashCode(): Int = (list.hashCode() * 31 + detail.hashCode()) * 31 + previousEntries.hashCode()
}

/**
 * Full-window glass routes ([OverlayRoute]: search, recording) drawn over the previous screen, which
 * stays composed underneath, blurred (API 31+) and dimmed.
 */
class OverlaySceneStrategy : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val last = entries.lastOrNull() ?: return null
        if (last.route !is OverlayRoute) return null
        val below = entries.getOrNull(entries.lastIndex - 1) ?: return null
        return GlassOverlayScene(below, last, entries.dropLast(1))
    }
}

private class GlassOverlayScene(
    private val below: NavEntry<NavKey>,
    private val top: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val key: Any = top.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(below, top)
    override val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            val behind = if (Build.VERSION.SDK_INT >= 31) Modifier.blur(OVERLAY_BLUR) else Modifier
            Box(Modifier.fillMaxSize().then(behind).graphicsLayer { alpha = 0.6f }) { below.Content() }
            top.Content()
        }
    }

    override fun equals(other: Any?): Boolean =
        other is GlassOverlayScene && other.below == below && other.top == top && other.previousEntries == previousEntries

    override fun hashCode(): Int = (below.hashCode() * 31 + top.hashCode()) * 31 + previousEntries.hashCode()

    private companion object {
        val OVERLAY_BLUR = 14.dp
    }
}
