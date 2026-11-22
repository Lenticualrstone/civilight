package io.github.lenticualrstone.civilight.feature.library.projectsettings

import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.domain.story.structure.StructureEdit
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.StructureLevels
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

/**
 * `단계 이름` editing (LIB-69 `구조` › `단계 이름` › `바꾸기`, STO-02). Renaming keeps every key,
 * order, code and datum (`domain:story` `StructureEdit.renameLevel`). The number of levels is not
 * changed here: a project made by `만들기` already has units below the top level, which locks the count
 * (`StructureEdit.canChangeLevelCount`).
 */
object StructureLevelsForm {
    /** `바꾸기` is on when some level changed and none is blank. */
    fun canSave(levels: List<String>, draft: List<String>): Boolean =
        draft.size == levels.size && draft.all { it.isNotBlank() } && draft.indices.any { draft[it].trim() != levels[it] }

    /** Every changed level renamed, as one document (one undo step). */
    fun apply(doc: ProjectDoc, draft: List<String>): ProjectDoc =
        draft.foldIndexed(doc) { index, acc, name ->
            if (name.trim() == acc.project.structure.levels.getOrNull(index)) acc else StructureEdit.renameLevel(acc, index, name)
        }
}

internal class StructureLevelsViewModel(projectKey: Key, services: LibraryServices) : ProjectViewModel(projectKey, services.deps.projects) {
    /** Level names, read-only flag and project name once loaded. */
    val levels: StateFlow<Triple<List<String>, Boolean, String>?> = state
        .map { load -> (load as? ProjectLoad.Ready)?.let { Triple(it.doc.project.structure.levels, it.readOnly, it.doc.project.name) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun save(draft: List<String>, done: () -> Unit) {
        viewModelScope.launch {
            update(ProjectSettingsCopy.LEVELS) { StructureLevelsForm.apply(it, draft) }
            done()
        }
    }
}

@Composable
internal fun StructureLevelsScreen(route: StructureLevels, services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { StructureLevelsViewModel(route.projectKey, services) }
    val loaded by vm.levels.collectAsState()
    var draft by rememberSaveable { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(loaded) { if (draft == null) loaded?.let { draft = it.first } }
    CivilightScreen(
        title = ProjectSettingsCopy.LEVELS,
        smallLine = loaded?.third,
        back = IconAction(BACK_LABEL) { navigator.pop() },
    ) { padding ->
      ContentSwap(loaded != null && draft != null, Modifier.fillMaxSize(), label = "structureLevels") { ready ->
        val current = loaded
        val names = draft
        if (!ready || current == null || names == null) return@ContentSwap
        val (levels, readOnly) = current
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                names.forEachIndexed { index, name ->
                    CivilightTextField(
                        value = name,
                        onValueChange = { v -> draft = names.mapIndexed { i, old -> if (i == index) v else old } },
                        enabled = !readOnly,
                    )
                }
                FoldReveal(!readOnly) {
                    CivilightButton(
                        text = ProjectSettingsCopy.CHANGE,
                        onClick = { vm.save(names) { navigator.pop() } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = StructureLevelsForm.canSave(levels, names),
                    )
                }
            }
        }
      }
    }
}
