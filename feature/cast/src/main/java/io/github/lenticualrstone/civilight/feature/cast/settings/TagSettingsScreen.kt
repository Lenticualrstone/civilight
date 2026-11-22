package io.github.lenticualrstone.civilight.feature.cast.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TagSettings
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.serialization.Serializable

class TagSettingsViewModel(projectKey: Key, sessions: ProjectSessions, contextOf: () -> EditContext) : CastViewModel(projectKey, sessions, contextOf)

/** One tag's name (feature-local; not drawn); [tagKey] null = a new tag. */
@Serializable
data class TagNameSheet(val projectKey: Key, val tagKey: Key? = null) : SheetKey

/**
 * `TagSettings` (CST-12; no board): the project tags in order with how many characters carry each
 * (`주연 · 3`); a tag opens its name sheet (`이름`, `이 값 고치기`), `+ 태그` adds one (`넣기`). Names
 * stay unique per project. Deleting and reordering are not offered (no source defines them).
 */
@Composable
fun TagSettingsScreen(route: TagSettings, deps: FeatureDeps) {
    val vm = entryViewModel { TagSettingsViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val gate = rememberHiddenGate(route.projectKey)
    val rows = remember(ready.doc, gate.open) { TagSettingsModel.rows(ready.doc, gate.open) }
    val sheets = rememberSheetHost()
    val editable = !ready.readOnly
    CenterColumn {
        CivilightScreen(
            title = CastUiCopy.TAGS,
            smallLine = FieldSettings.HEAD,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is TagNameSheet -> { { TagNameContent(key, ready.doc, vm) } }
                    else -> null
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                for (row in rows) {
                    ListRow(row.title, compact = true, onClick = if (editable) ({ sheets.open(TagNameSheet(route.projectKey, row.key)) }) else null)
                }
                if (editable) AddChip(CastUiCopy.TAGS, { sheets.open(TagNameSheet(route.projectKey)) }, Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun SheetScope.TagNameContent(key: TagNameSheet, doc: ProjectDoc, vm: TagSettingsViewModel) {
    val current = key.tagKey?.let { k -> doc.project.tags.firstOrNull { it.key == k } }
    var name by rememberSaveable(key) { mutableStateOf(current?.name.orEmpty()) }
    val usable = TagSettingsModel.canName(doc, name, except = key.tagKey) && name.trim() != current?.name
    val scope = this
    SheetHeader(current?.name ?: CastUiCopy.TAGS, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    CivilightTextField(name, { name = it }, Modifier.fillMaxWidth().padding(top = 14.dp), label = CastUiCopy.NAME)
    SheetActions {
        val label = if (current != null) CastUiCopy.BTN_CORRECT else CastUiCopy.BTN_INSERT
        CivilightButton(
            label,
            {
                val typed = name
                val tagKey = key.tagKey
                vm.act(label) { d, ctx -> (if (tagKey != null) TagSettingsModel.rename(d, tagKey, typed, ctx) else TagSettingsModel.add(d, typed, ctx)) ?: d }
                scope.close()
            },
            Modifier.weight(1f),
            enabled = usable,
        )
    }
}
