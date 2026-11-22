package io.github.lenticualrstone.civilight.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.designsystem.component.FullScreenError
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
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
 *   group is locked; a cancelled authentication goes to 서재 with the group locked (L-05);
 * - shows an error with `서재로` when the project cannot be opened (a restored stack may name a
 *   project that is gone).
 */
@Composable
fun ProjectEntryFrame(projectKey: Key, deps: FeatureDeps, content: @Composable () -> Unit) {
    var load by remember(projectKey) {
        mutableStateOf<FrameLoad>(deps.projects.peek(projectKey)?.let { FrameLoad.Ready(it) } ?: FrameLoad.Loading)
    }
    LaunchedEffect(projectKey) {
        val store = try {
            deps.projects.acquire(projectKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            load = FrameLoad.Failed(e)
            return@LaunchedEffect
        }
        try {
            load = FrameLoad.Ready(store)
            awaitCancellation()
        } finally {
            deps.projects.release(projectKey)
        }
    }
    when (val l = load) {
        FrameLoad.Loading -> Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg))
        is FrameLoad.Failed -> FullScreenError(
            icon = IconId.Warn,
            title = PROJECT_OPEN_FAILED,
            primaryLabel = LockCopy.TO_LIBRARY,
            onPrimary = { deps.navigator.resetTo(Library) },
        )
        is FrameLoad.Ready -> ReadyFrame(l.store, deps, content)
    }
}

@Composable
private fun ReadyFrame(store: ProjectStore, deps: FeatureDeps, content: @Composable () -> Unit) {
    val doc by store.doc.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    val protections = doc.project.protections
    ProtectWindow(WindowProtection.forProject(protections))
    if (protections.hidden && !unlock.hiddenGroupOpen) {
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
    CompositionLocalProvider(LocalProjectStore provides store) { content() }
}

/** No source gives copy for a project that cannot be opened (library spec rule: TODO copy, raised in shell.md). */
const val PROJECT_OPEN_FAILED = "TODO_COPY_SHELL_PROJECT_OPEN_FAILED"
