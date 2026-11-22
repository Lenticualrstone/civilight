package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import kotlinx.serialization.Serializable

/**
 * App lock (LIB-34 `AppLock`, LIB-35). Never pushed on the back stack: the shell draws lock screens
 * as an overlay above the navigation so the stack under them is kept. Listed here so every screen of
 * the specs has a key (logs, placeholders, tests).
 */
@Serializable
data object AppLock : ShellRoute

/** Hidden-project lock (LIB-34 `RHLockMark`); drawn by the shell in place of [projectKey]'s content. */
@Serializable
data class HiddenProjectLock(override val projectKey: Key) : ShellRoute, ProjectRoute
