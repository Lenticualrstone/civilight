package io.github.lenticualrstone.civilight.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FullScreenError
import io.github.lenticualrstone.civilight.designsystem.icon.AppMark
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.rememberUnhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.resetTo
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import io.github.lenticualrstone.civilight.ui.security.HiddenProjectLockScreen
import io.github.lenticualrstone.civilight.ui.security.LockCopy
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The open store of the project whose screen this is (inside [ProjectEntryFrame]). */
val LocalProjectStore = staticCompositionLocalOf<ProjectStore?> { null }

/** The store of the current project screen; fails outside a project route. */
val currentProjectStore: ProjectStore
    @Composable get() = checkNotNull(LocalProjectStore.current) { "not inside a project route" }

private sealed interface FrameLoad {
    data object Loading : FrameLoad
    data class Ready(val store: ProjectStore) : FrameLoad
    data class Failed(val error: Throwable) : FrameLoad
}

/**
 * Wraps every project route (the shell applies it; features do not):
 * - opens the project through [FeatureDeps.projects] and provides [LocalProjectStore];
 * - keeps the window protected while its content is on screen (LIB-26, LIB-29) — set before the
 *   content is first drawn, since nothing is drawn until the store is open;
 * - shows the hidden-project lock (LIB-34 `RHLockMark`) instead of the content while the hidden
 *   group is locked, unless the project is kept open (`UnlockController.keepOpen`, "지금은 열린 채");
 *   a cancelled authentication goes to 서재 with the group locked (L-05);
 * - shows an error with `서재로` when the project cannot be opened (a restored stack may name a
 *   project that is gone).
 */
@Composable
fun ProjectEntryFrame(projectKey: Key, deps: FeatureDeps, content: @Composable () -> Unit) {
    val motion = CivilightTheme.motion
    val entry = deps.stores.library.state.collectAsState().value.entry(projectKey)
    val projectTitle = entry?.name?.takeUnless { entry.hidden } ?: ""
    // A store that is already open (another route of the same project) shows at once: no loading frame.
    var load by remember(projectKey) {
        mutableStateOf<FrameLoad>(deps.projects.peek(projectKey)?.let { FrameLoad.Ready(it) } ?: FrameLoad.Loading)
    }
    LaunchedEffect(projectKey) {
        val wasOpen = deps.projects.peek(projectKey) != null
        val started = System.currentTimeMillis()
        val store = try {
            deps.projects.acquire(projectKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            load = FrameLoad.Failed(e)
            return@LaunchedEffect
        }
        // Released however the effect ends, including a cancel during the opening beat.
        try {
            // (0.4.0) Opening beat on the first open of a project only, so opening feels tactile.
            val wait = ProjectOpenBeat.remainingMs(
                wasOpen = wasOpen,
                reduced = motion.reduced,
                beatMs = motion.delayMs(OPEN_BEAT_MS),
                elapsedMs = System.currentTimeMillis() - started,
            )
            if (wait > 0) delay(wait)
            load = FrameLoad.Ready(store)
            awaitCancellation()
        } finally {
            deps.projects.release(projectKey)
        }
    }
    // S9: loading → content → error crossfade (contentKey by kind, so a new store does not re-animate).
    ContentSwap(load, contentKey = { it::class }, label = "projectFrame") { l ->
        when (l) {
            FrameLoad.Loading -> Box(
                Modifier.fillMaxSize().background(CivilightTheme.colors.bg),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppMark(size = 44.dp, barProgress = 1f, showCaret = true)
                    if (projectTitle.isNotEmpty()) {
                        CivilightText(projectTitle, CivilightTheme.type.heading, color = CivilightTheme.colors.ink2, maxLines = 1)
                    }
                }
            }
            is FrameLoad.Failed -> FullScreenError(
                icon = IconId.Warn,
                title = PROJECT_OPEN_FAILED,
                primaryLabel = LockCopy.TO_LIBRARY,
                onPrimary = { deps.navigator.resetTo(Library) },
            )
            is FrameLoad.Ready -> ReadyFrame(l.store, deps, content)
        }
    }
}

@Composable
private fun ReadyFrame(store: ProjectStore, deps: FeatureDeps, content: @Composable () -> Unit) {
    val doc by store.doc.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    val protections = doc.project.protections
    ProtectWindow(WindowProtection.forProject(protections))
    val locked = protections.hidden && !unlock.projectOpen(store.key)
    // CNV-43: the lock shows at once; unlocking plays the unhide (blur 7 → 0, .55 → 1) on the content while
    // the lock screen fades out over it. Locked content is never composed.
    val unhide = rememberUnhide(revealed = !locked)
    if (locked) {
        val scope = rememberCoroutineScope()
        fun ask(credentialOnly: Boolean) {
            scope.launch {
                when (deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS, credentialOnly = credentialOnly))) {
                    AuthResult.SUCCESS -> deps.unlock.unlockHiddenGroup()
                    else -> {
                        deps.unlock.lockHiddenGroup()
                        deps.navigator.resetTo(Library)
                    }
                }
            }
        }
        HiddenProjectLockScreen(
            onBiometric = { ask(credentialOnly = false) },
            onPin = { ask(credentialOnly = true) },
            onLibrary = { deps.navigator.resetTo(Library) },
        )
        return
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().unhide(unhide)) {
            CompositionLocalProvider(LocalProjectStore provides store) { content() }
        }
        if (unhide.value < 1f && protections.hidden) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - unhide.value }) {
                HiddenProjectLockScreen(onBiometric = {}, onPin = {}, onLibrary = {}, interactive = false)
            }
        }
    }
}

/** Minimum opening beat (ms, before animator scaling) when a project is first opened. */
private const val OPEN_BEAT_MS = 260

/** How long the opening beat still waits after the store is open (pure; see `ProjectOpenBeatTest`). */
object ProjectOpenBeat {
    /**
     * 0 when the project was already open ([wasOpen]: its content shows at once) or motion is reduced;
     * otherwise what is left of [beatMs] after [elapsedMs] spent opening.
     */
    fun remainingMs(wasOpen: Boolean, reduced: Boolean, beatMs: Long, elapsedMs: Long): Long =
        if (wasOpen || reduced) 0L else (beatMs - elapsedMs).coerceAtLeast(0L)
}

/**
 * A project that cannot be opened (a restored stack may name a project that is gone). No source gives this
 * line; it follows the `… 열 수 없음` pattern of `RHRecoveryPw` (60 §5), raised in shell.md for review.
 */
const val PROJECT_OPEN_FAILED = "프로젝트 열 수 없음"
