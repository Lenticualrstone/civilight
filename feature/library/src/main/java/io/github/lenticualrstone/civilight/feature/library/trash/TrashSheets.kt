package io.github.lenticualrstone.civilight.feature.library.trash

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.trash.SceneChoice
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.rememberProjectDoc
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import kotlinx.serialization.Serializable

/** `씬도 되살리기` · `다른 씬 끝에` for blocks whose scene is in the trash (T-14, SCR-39). */
@Serializable
data class SceneChoiceSheet(val projectKey: Key) : SheetKey

/** `다른 씬 끝에`: which live scene receives the blocks. */
@Serializable
data class ScenePickSheet(val projectKey: Key) : SheetKey

/**
 * The two choices of T-14. The waiting restore lives in the view model; when it is gone (answered,
 * or lost with the process) the sheet closes itself.
 */
@Composable
internal fun SheetScope.SceneChoiceContent(vm: TrashViewModel, waiting: Boolean) {
    val scope = this
    LaunchedEffect(waiting) { if (!waiting) scope.close() }
    val colors = CivilightTheme.colors
    SheetHeader(
        title = TrashScreenCopy.RESTORE,
        // Clearing the waiting restore closes the sheet (the effect above), exactly once.
        onClose = vm::dismissSceneChoice,
    )
    ListRow(
        title = TrashScreenCopy.SCENE_TOO,
        leading = { CivilightIcon(IconId.Undo, contentDescription = null, tint = colors.ink2) },
        onClick = { vm.chooseScene(SceneChoice.RestoreScene) },
        modifier = Modifier.padding(top = 8.dp),
    )
    ListRow(
        title = TrashScreenCopy.OTHER_SCENE_END,
        leading = { CivilightIcon(IconId.Move, contentDescription = null, tint = colors.ink2) },
        trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) },
        onClick = { currentProject(vm)?.let { scope.open(ScenePickSheet(it)) } },
    )
}

private fun currentProject(vm: TrashViewModel): Key? = vm.sceneChoice.value?.projectKey

/** The scene picker of `다른 씬 끝에` (scene level only, live scenes; hidden ones behind the gate). */
@Composable
internal fun SheetScope.ScenePickContent(projectKey: Key, services: LibraryServices, vm: TrashViewModel, waiting: Boolean) {
    val scope = this
    // The waiting restore is gone (answered, dismissed, a re-lock, a restart): nothing to pick for.
    LaunchedEffect(waiting) { if (!waiting) scope.close() }
    val doc by rememberProjectDoc(services.deps.projects, projectKey)
    val gate = rememberHiddenGate(projectKey)
    val current = doc ?: return
    if (!waiting) return
    // Scene titles of a project shown outside its own routes (LIB-26, LIB-29).
    ProtectWindow(WindowProtection.forProject(current.project.protections))
    val sceneLevel = current.project.structure.levels.lastIndex
    UnitPickerSheet(
        doc = current,
        gate = gate,
        title = TrashScreenCopy.OTHER_SCENE_END,
        // Answering clears the waiting restore; both sheets then close through their effects.
        onPick = { sceneKey -> vm.chooseScene(SceneChoice.OtherSceneEnd(sceneKey)) },
        onClose = scope::close,
        levels = setOf(sceneLevel),
    )
}
