package io.github.lenticualrstone.civilight.ui.lens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore

/**
 * STO-16 hook the shell runs around the script screen: entering stores the lens as it is (a stale
 * `렌즈를 이 씬으로 맞추기` is dropped); leaving the script (back, or going on to another screen)
 * applies the scene point if `렌즈를 이 씬으로 맞추기` was used meanwhile.
 */
@Composable
fun ScriptLensEffect() {
    val deps = LocalFeatureDeps.current
    val store = currentProjectStore
    DisposableEffect(store) {
        val lens = deps.lens(store)
        lens.enterScript()
        onDispose { lens.leaveScript() }
    }
}
