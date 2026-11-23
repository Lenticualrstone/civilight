package io.github.lenticualrstone.civilight.ui.nav

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.navigation.OverlayRoute
import io.github.lenticualrstone.civilight.ui.scaffold.CommandBarHost
import io.github.lenticualrstone.civilight.ui.scaffold.CommandFrameBar
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScopeEvent
import io.github.lenticualrstone.civilight.ui.scaffold.LocalCommandBarHost
import io.github.lenticualrstone.civilight.ui.scaffold.LocalRelocked
import io.github.lenticualrstone.civilight.ui.window.EmptyDetailPane
import io.github.lenticualrstone.civilight.ui.window.ListDetailPanes

/** Which pane an entry is drawn in; lets a screen drop its own back button in the detail pane. */
enum class PaneRole { SINGLE, LIST, DETAIL }

val LocalPaneRole = androidx.compose.runtime.staticCompositionLocalOf { PaneRole.SINGLE }

/**
 * `목록 + 상세` on inner screens (CNV-33): when the top two routes are a [ListDetailPairs] pair, or
 * the top route is a list module alone, both panes share the screen.
 *
 * The scene key stays the list entry's, on purpose: choosing another item keeps the same scene, so the
 * list pane never takes part in a transition, and only the detail pane swaps inside the scene (motion
 * audit S3, [DetailSwap]). Keying the scene on the detail would make NavDisplay crossfade two scenes that
 * share the list entry, and Nav3 draws a shared entry only in the top scene, so the list would fade too.
 * The panes split by the module's CNV-47 ratio ([ListDetailPairs.moduleOf]).
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
        // DS-65: one command bar for the frame. The panes declare theirs to [host]; the scope follows the detail
        // opening, being replaced or closing (and the user's pick on the scope switch), never a touch.
        val detailKey = detail?.contentKey
        val host = remember { CommandBarHost(if (detailKey != null) CommandScope.DETAIL else CommandScope.LIST) }
        val seen = remember { DetailSeen(detailKey) }
        SideEffect {
            CommandScopeEvent.ofDetail(seen.key, detailKey)?.let(host::onEvent)
            seen.key = detailKey
        }
        // A detail with its own frame (script, cast profile) keeps one bar per pane until it registers (Requests).
        val frameHost = host.takeUnless { ListDetailPairs.detailDrawsOwnBar(detail?.route) }
        CompositionLocalProvider(LocalCommandBarHost provides frameHost) {
            ListDetailPanes(
                list = { CompositionLocalProvider(LocalPaneRole provides PaneRole.LIST) { list.Content() } },
                detail = { DetailSwap(detail) },
                module = ListDetailPairs.moduleOf(list.route),
                bottomBar = if (frameHost != null) ({ left -> CommandFrameBar(frameHost, left, hideTitles = LocalRelocked.current) }) else null,
            )
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ListDetailScene && other.list == list && other.detail == detail && other.previousEntries == previousEntries

    override fun hashCode(): Int = (list.hashCode() * 31 + detail.hashCode()) * 31 + previousEntries.hashCode()
}

/** The detail key the frame last saw (a plain holder, read and written only in its side effect). */
private class DetailSeen(var key: Any?)

/**
 * The detail pane's swap (S3): another item crossfades in place (`standard` in, `exit` out); the first
 * detail beside a list slides in 48 dp from the right on `foldOpen` and a closed detail fades on
 * `foldClose` (CNV-34). Reduced motion: fades only (inside the helpers). The outgoing entry stays composed
 * while it fades, so Nav3 pops it (view model, saved state) only after it has left. With no detail the pane
 * shows [EmptyDetailPane] (Q-LY-02): it fades out on `exit` under the first detail and back in on `standard`
 * when the detail closes.
 */
@Composable
private fun DetailSwap(detail: NavEntry<NavKey>?) {
    val motion = LocalMotion.current
    AnimatedContent(
        targetState = detail,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
        contentKey = { it?.contentKey },
        transitionSpec = {
            when {
                initialState == null -> ContentTransform(motion.panelEnter(), fadeOut(motion.spec(CivilightMotion.exit)), sizeTransform = null)
                targetState == null -> ContentTransform(fadeIn(motion.spec(CivilightMotion.standard)), motion.panelExit(), sizeTransform = null)
                else -> ContentTransform(
                    fadeIn(motion.spec(CivilightMotion.standard)),
                    fadeOut(motion.spec(CivilightMotion.exit)),
                    sizeTransform = null,
                )
            }
        },
        label = "detailSwap",
    ) { entry ->
        if (entry != null) {
            CompositionLocalProvider(LocalPaneRole provides PaneRole.DETAIL) { entry.Content() }
        } else {
            EmptyDetailPane()
        }
    }
}

/**
 * How strongly the screens under glass overlays are blurred and dimmed (0 none … 1 full): the largest
 * progress of the overlays on screen. The shell passes it to NavDisplay's modifier with
 * [overlayBackdrop], which wraps only the base scenes, never the overlays themselves.
 */
@Stable
class OverlayBackdrop {
    private val layers = mutableStateListOf<Animatable<Float, AnimationVector1D>>()

    /** Current amount; read it in a draw or layer block. */
    val amount: Float get() = layers.maxOfOrNull { it.value } ?: 0f

    /** Whether the full glass look is on (used for reduced motion, which does not ramp the blur). */
    val active: Boolean get() = layers.isNotEmpty()

    internal fun add(layer: Animatable<Float, AnimationVector1D>) {
        if (layer !in layers) layers.add(layer)
    }

    internal fun remove(layer: Animatable<Float, AnimationVector1D>) {
        layers.remove(layer)
    }
}

/**
 * Blurs (API 31+, 14 dp) and dims (to 60 %) the base screen under glass overlays by [backdrop]'s amount
 * (S2). Reduced motion: the blur does not ramp, only the dim fades.
 */
fun Modifier.overlayBackdrop(backdrop: OverlayBackdrop, reduced: () -> Boolean): Modifier = graphicsLayer {
    val p = backdrop.amount.coerceIn(0f, 1f)
    alpha = 1f - (1f - OVERLAY_DIM) * p
    val blurP = if (reduced()) (if (p > 0f) 1f else 0f) else p
    val r = OVERLAY_BLUR.toPx() * blurP
    renderEffect = if (Build.VERSION.SDK_INT >= 31 && r > 0f) BlurEffect(r, r, TileMode.Clamp) else null
}

/**
 * Full-window glass routes ([OverlayRoute]: search, recording) as Nav3 overlay scenes (S2): the screens
 * below stay composed in their own scene (no blank frame during the fade), the overlay fades in on `enter`
 * and out on `exit` before it leaves composition, and the base blur and dim follow the same progress
 * through [OverlayBackdrop]. Back inside the overlay pops only the overlay (NavDisplay's own handler counts
 * from the base scene).
 */
class OverlaySceneStrategy(
    private val backdrop: OverlayBackdrop = OverlayBackdrop(),
    private val motion: () -> Motion = { Motion(reduced = false) },
) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val last = entries.lastOrNull() ?: return null
        if (last.route !is OverlayRoute) return null
        if (entries.size < 2) return null
        return GlassOverlayScene(last, entries.dropLast(1), backdrop, motion, onBack)
    }
}

private class GlassOverlayScene(
    private val top: NavEntry<NavKey>,
    override val overlaidEntries: List<NavEntry<NavKey>>,
    private val backdrop: OverlayBackdrop,
    private val motion: () -> Motion,
    private val onBack: () -> Unit,
) : OverlayScene<NavKey> {
    override val key: Any = top.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(top)
    override val previousEntries: List<NavEntry<NavKey>> = overlaidEntries

    private val progress = Animatable(0f)
    private var removing by mutableStateOf(false)

    override val content: @Composable () -> Unit = {
        DisposableEffect(Unit) {
            backdrop.add(progress)
            onDispose { backdrop.remove(progress) }
        }
        LaunchedEffect(Unit) {
            if (!removing) progress.animateTo(1f, motion().spec(CivilightMotion.enter))
        }
        LocalBackHandler.current(!removing, onBack)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress.value }) { top.Content() }
    }

    override suspend fun onRemove() {
        removing = true
        progress.animateTo(0f, motion().spec(CivilightMotion.exit))
    }

    override fun equals(other: Any?): Boolean =
        other is GlassOverlayScene && other.top == top && other.overlaidEntries == overlaidEntries

    override fun hashCode(): Int = top.hashCode() * 31 + overlaidEntries.hashCode()
}

private const val OVERLAY_DIM = 0.6f
private val OVERLAY_BLUR = 14.dp
