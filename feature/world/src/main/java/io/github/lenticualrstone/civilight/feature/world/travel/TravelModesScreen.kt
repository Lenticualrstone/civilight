package io.github.lenticualrstone.civilight.feature.world.travel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TravelModes
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The mode form (feature-local): [index] null = `직접` (a new mode), else that row. */
@Serializable
data class TravelModeSheet(val index: Int? = null) : SheetKey

/** `프로젝트 설정 › 이동 수단` (WLD-47). */
class TravelModesViewModel(projectKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    val ui: StateFlow<TravelModesUi?> = combine(doc.filterNotNull(), readOnly) { d, r -> TravelModesState.build(d, r) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** `저장`: one undo step; returns the refusal line, or null when saved. */
    suspend fun save(index: Int?, draft: TravelModeDraft): String? {
        var refusal: String? = null
        val ctx = editContext()
        val result = apply(WorldCopy.TRAVEL_MODES) { d ->
            when (val r = TravelModesState.save(d, index, draft, ctx)) {
                is TravelModeSave.Saved -> r.doc
                is TravelModeSave.Refused -> d.also { refusal = r.line }
            }
        }
        return refusal ?: if (result == EditResult.READ_ONLY) WorldCopy.READ_ONLY else null
    }
}

private val json = Json { ignoreUnknownKeys = true }

private val DraftSaver: Saver<TravelModeDraft, String> = Saver(
    save = { json.encodeToString(TravelModeDraft.serializer(), it) },
    restore = { runCatching { json.decodeFromString(TravelModeDraft.serializer(), it) }.getOrNull() ?: TravelModeDraft() },
)

/**
 * 이동 수단 (WLD-47, board `ProjSettings` row `이동 하루 거리`; the editor is not drawn): one row
 * per mode `도보 | 15 km · 8시간` (tap → form), then `직접` to add one. The map lane's route sheet
 * pushes this screen from its `직접` chip.
 */
@Composable
fun TravelModesScreen(route: TravelModes, deps: FeatureDeps) {
    val vm = entryViewModel { TravelModesViewModel(route.projectKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val sheets = rememberSheetHost()
    val state = ui

    CivilightScreen(
        title = WorldCopy.TRAVEL_MODES,
        smallLine = WorldCopy.PROJECT_SETTINGS,
        back = IconAction(WorldCopy.BACK) { deps.navigator.pop() },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is TravelModeSheet -> doc?.let { d ->
                    { TravelModeForm(key, key.index?.let { TravelModesState.draftOf(d, it) } ?: TravelModeDraft(), vm) }
                }
                else -> null
            }
        },
    ) { padding ->
        CenterColumn {
            if (state == null) return@CenterColumn
            val colors = CivilightTheme.colors
            val type = CivilightTheme.type
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                item(key = "title") { SectionLabel(WorldCopy.TRAVEL_MODES, Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                items(state.rows, key = { it.index }) { row ->
                    ListRow(
                        title = row.name,
                        subtitle = row.line,
                        compact = true,
                        onClick = if (state.readOnly) null else ({ sheets.open(TravelModeSheet(row.index)) }),
                    )
                }
                item(key = "add") {
                    if (state.readOnly) {
                        CivilightText(WorldCopy.READ_ONLY, type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
                    } else {
                        TextAction(WorldCopy.TRAVEL_ADD, { sheets.open(TravelModeSheet(null)) }, Modifier.padding(top = 8.dp), color = colors.acc, style = type.small)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetScope.TravelModeForm(key: TravelModeSheet, initial: TravelModeDraft, vm: TravelModesViewModel) {
    var draft by rememberSaveable(key, stateSaver = DraftSaver) { mutableStateOf(initial) }
    var refusal by rememberSaveable(key) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sheet = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(title = if (key.index == null) WorldCopy.TRAVEL_ADD else initial.name, onClose = ::close, closeLabel = WorldCopy.BTN_CANCEL)
    SheetScrollBody(Modifier.padding(top = 12.dp)) {
        CivilightTextField(
            value = draft.name,
            onValueChange = { draft = draft.copy(name = it); refusal = null },
            label = WorldCopy.NAME,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightTextField(
                value = draft.km,
                onValueChange = { draft = draft.copy(km = it); refusal = null },
                label = WorldCopy.TRAVEL_DISTANCE,
                unit = WorldCopy.UNIT_KM,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            CivilightTextField(
                value = draft.hours,
                onValueChange = { draft = draft.copy(hours = it); refusal = null },
                label = WorldCopy.TODO_COPY_WORLD_TRAVEL_HOURS,
                unit = WorldCopy.UNIT_HOURS,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        refusal?.let { CivilightText(it, type.small, Modifier.padding(top = 6.dp), color = colors.warn) }
    }
    SheetActions {
        CivilightButton(
            text = WorldCopy.BTN_SAVE,
            modifier = Modifier.weight(1f),
            onClick = {
                scope.launch {
                    val line = vm.save(key.index, draft)
                    if (line == null) sheet.close() else refusal = line
                }
            },
        )
    }
}
