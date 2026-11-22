package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.foundation.layout.PaddingValues
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.navigation.WorldCalendar
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.launch

/**
 * `프로젝트 설정 › 세계 시간` (WLD-29, boards `NewTime` step `시간`, `ProjSettings`): the three modes
 * `연도만 · 세계 달력 만들기 · 시간 없이` as radio rows, `이야기가 시작되는 해`, and the note
 * `생일이 있으면 나이 자동 계산`. Changes apply at once (one undo step each; typing the year is one).
 * In `세계 달력` mode the month and weekday editor (WLD-30) follows; it saves with `저장`.
 */
@Composable
fun WorldTimeScreen(route: WorldCalendar, deps: FeatureDeps) {
    val vm = entryViewModel { WorldTimeViewModel(route.projectKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val state = ui
    var year by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state?.startYear) { if (year == null && state != null) year = state.startYear }
    // The editor's draft is seeded once from the stored calendar and saved with `저장` only (WLD-30).
    var calendarDraft by rememberSaveable(stateSaver = CalendarDraft.Saver) { mutableStateOf(CalendarDraft()) }
    var calendarSeeded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state != null) {
        if (!calendarSeeded && state != null) {
            calendarDraft = CalendarDraft.of(state.calendar)
            calendarSeeded = true
        }
    }
    val scope = rememberCoroutineScope()

    CivilightScreen(
        title = WorldCopy.WORLD_TIME,
        smallLine = WorldCopy.PROJECT_SETTINGS,
        back = IconAction(WorldCopy.BACK) { deps.navigator.pop() },
    ) { padding ->
        CenterColumn {
            if (state == null) return@CenterColumn
            val colors = CivilightTheme.colors
            val type = CivilightTheme.type
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                item(key = "title") { SectionLabel(WorldCopy.WORLD_TIME, civilightItem().padding(top = 12.dp, bottom = 4.dp)) }
                items(state.options, key = { it.mode.name }) { option ->
                    RadioRow(
                        modifier = civilightItem(),
                        title = option.title,
                        subtitle = option.subtitle,
                        selected = option.selected,
                        onClick = { vm.setMode(option.mode) },
                        enabled = !state.readOnly,
                    )
                }
                state.calendarLine?.let { line ->
                    item(key = "calendar") { CivilightText(line, type.small, civilightItem().padding(top = 8.dp), color = colors.ink3) }
                }
                if (state.showCalendarEditor) {
                    item(key = "calendarEditor") {
                        CalendarEditor(
                            modifier = civilightItem(),
                            draft = calendarDraft,
                            readOnly = state.readOnly,
                            onChange = { calendarDraft = it },
                            onSave = {
                                scope.launch {
                                    if (vm.saveCalendar(calendarDraft)) deps.toasts.show(WorldCopy.CALENDAR_SAVED_TOAST) { vm.undoLast() }
                                }
                            },
                        )
                    }
                }
                if (state.showStartYear) {
                    item(key = "year") {
                        CivilightTextField(
                            value = year ?: state.startYear,
                            onValueChange = { text ->
                                year = text
                                vm.setStartYear(text)
                            },
                            label = WorldCopy.STORY_START_YEAR,
                            modifier = civilightItem().fillMaxWidth().padding(top = 18.dp),
                            enabled = !state.readOnly,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                item(key = "note") { CivilightText(WorldCopy.AGES_NOTE, type.small, civilightItem().padding(top = 10.dp), color = colors.ink3) }
                if (state.readOnly) {
                    item(key = "ro") { CivilightText(WorldCopy.READ_ONLY, type.small, civilightItem().padding(top = 10.dp), color = colors.ink3) }
                }
            }
        }
    }
}
