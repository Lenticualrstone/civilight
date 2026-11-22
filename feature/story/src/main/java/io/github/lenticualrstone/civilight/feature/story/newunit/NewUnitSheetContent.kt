package io.github.lenticualrstone.civilight.feature.story.newunit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.StoryViewModel
import io.github.lenticualrstone.civilight.feature.story.menu.storyActions
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.Job

/** `만들기` (STO-05): one undo step `새 씬`; a stale sheet creates nothing. */
fun StoryViewModel.createScene(sheet: NewUnitSheet, form: NewSceneForm): Job =
    apply(NewUnitCopy.UNDO_LABEL) { doc, ctx -> NewSceneForms.create(doc, sheet, form, ctx)?.first ?: doc }

/**
 * `NewUnitSheet` content (shared, owner STORY): `다음 씬 만들기` (after a scene, or at the end of an
 * episode) and `앞에 새 씬` (before a scene). Fields 제목 · 장소 · 때 · 전환 문구, all skippable;
 * 장소 and 때 are pre-filled with `비우기` (R22 §4). `바꾸기 ›` on 장소 swaps the sheet body to the
 * place picker in place, so the form keeps its state. Nothing is created before `만들기`.
 */
@Composable
fun SheetScope.NewUnitContent(sheet: NewUnitSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val actions = storyActions(sheet.projectKey, deps)
    val setup = remember(doc, sheet) { NewSceneForms.setup(doc, sheet) }

    var title by rememberSaveable { mutableStateOf("") }
    var titleOpen by rememberSaveable { mutableStateOf(false) }
    var placeMode by rememberSaveable { mutableStateOf(PLACE_PREFILLED) }
    var placeKind by rememberSaveable { mutableStateOf<String?>(null) }
    var placeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var timeCleared by rememberSaveable { mutableStateOf(false) }
    var dayChip by rememberSaveable { mutableStateOf<Int?>(null) }
    var band by rememberSaveable { mutableStateOf<String?>(null) }
    var mode by rememberSaveable { mutableStateOf<String?>(null) }
    var directText by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf(false) }

    val form = NewSceneForm(
        title = title,
        place = when (placeMode) {
            PLACE_CLEARED -> PlaceChoice.Cleared
            PLACE_PICKED -> {
                val kind = placeKind?.let { k -> PlaceRefKind.entries.firstOrNull { it.name == k } }
                val key = placeKey
                if (kind != null && key != null) PlaceChoice.Picked(PlaceRef(kind, key)) else PlaceChoice.Prefilled
            }
            else -> PlaceChoice.Prefilled
        },
        timeCleared = timeCleared,
        dayChip = dayChip,
        band = band,
        transition = mode?.let { m -> TransitionMode.entries.firstOrNull { it.name == m } },
        directText = directText,
    )

    // The form and the place picker swap in place on the body kind, so the form keeps its state (P3).
    ContentSwap(picking && setup != null, Modifier.fillMaxWidth()) { pickerBody ->
        Column(Modifier.fillMaxWidth()) {
            if (pickerBody && setup != null) {
                PlacePickerSheet(
                    doc = doc,
                    gate = rememberHiddenGate(sheet.projectKey),
                    title = NewUnitCopy.PICK_PLACE,
                    onPick = { ref ->
                        placeMode = PLACE_PICKED
                        placeKind = ref.kind.known?.name
                        placeKey = ref.key
                        picking = false
                    },
                    onClose = { picking = false },
                    selected = NewSceneForms.place(setup, form),
                )
            } else {
                SheetHeader(title = setup?.title ?: NewUnitCopy.TITLE_AFTER, onClose = host::close, subtitle = setup?.subtitle?.ifEmpty { null })
                if (setup != null) {
                    val lines = NewSceneForms.lines(doc, setup, form)
                    val readOnly = store.readOnly
                    SheetScrollBody(Modifier.padding(top = 10.dp)) {
                        FieldRow(NewUnitCopy.FIELD_TITLE, lines.title) {
                            if (!titleOpen) TextAction("${NewUnitCopy.WRITE} ›", { titleOpen = true }, style = actionStyle())
                        }
                        FoldReveal(titleOpen) {
                            CivilightTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        }
                        FieldRow(NewUnitCopy.FIELD_PLACE, lines.place) {
                            if (lines.placeClearable) TextAction(NewUnitCopy.CLEAR, { placeMode = PLACE_CLEARED }, style = actionStyle())
                            TextAction("${NewUnitCopy.CHANGE} ›", { picking = true }, style = actionStyle())
                        }
                        FieldRow(NewUnitCopy.FIELD_TIME, lines.time) {
                            if (lines.timeClearable) TextAction(NewUnitCopy.CLEAR, { timeCleared = true }, style = actionStyle())
                        }
                        FoldReveal(NewSceneForms.baseKey(doc, setup) != null) {
                            val selectedDay = NewSceneForms.dayChipOf(doc, setup, form)
                            val selectedBand = NewSceneForms.bandOf(doc, setup, form)
                            ChipFlow(Modifier.padding(bottom = 10.dp)) {
                                listOf(0 to NewUnitCopy.SAME_DAY, 1 to NewUnitCopy.PLUS_ONE, 2 to NewUnitCopy.PLUS_TWO).forEach { (n, word) ->
                                    FilterChip(word, !timeCleared && selectedDay == n, {
                                        timeCleared = false
                                        dayChip = n
                                    })
                                }
                                Band.entries.forEach { b ->
                                    FilterChip(b.word, !timeCleared && selectedBand == b, {
                                        timeCleared = false
                                        band = b.name
                                    })
                                }
                            }
                        }
                        TransitionRow(lines.transition, lines.autoEnabled) { mode = it.name }
                        FoldReveal(lines.transition == TransitionMode.DIRECT) {
                            CivilightTextField(value = directText, onValueChange = { directText = it }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    SheetActions {
                        CivilightButton(
                            text = NewUnitCopy.CREATE,
                            onClick = {
                                actions.createScene(sheet, form)
                                host.close()
                            },
                            modifier = Modifier.weight(1f),
                            subText = if (readOnly) StoryCopy.READ_ONLY else null,
                            enabled = !readOnly,
                        )
                    }
                }
            }
        }
    }
}

/** A field row as `RSSplit` draws it: key `small` 40 wide, value 14 / 20, then the row's text actions. */
@Composable
private fun FieldRow(key: String, value: String, actions: @Composable () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CivilightText(key, type.small, Modifier.width(40.dp), color = colors.ink3, maxLines = 1)
        CivilightText(value, type.command, Modifier.weight(1f), color = colors.ink, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

/** `전환 문구 | 자동 · 직접 · 없음` (SCR-48 chips); `자동` needs a place. */
@Composable
private fun TransitionRow(selected: TransitionMode, autoEnabled: Boolean, onSelect: (TransitionMode) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CivilightText(NewUnitCopy.FIELD_TRANSITION, type.small, color = colors.ink3, maxLines = 1)
        ChipFlow {
            FilterChip(NewUnitCopy.MODE_AUTO, selected == TransitionMode.AUTO, { onSelect(TransitionMode.AUTO) }, enabled = autoEnabled)
            FilterChip(NewUnitCopy.MODE_DIRECT, selected == TransitionMode.DIRECT, { onSelect(TransitionMode.DIRECT) })
            FilterChip(NewUnitCopy.MODE_NONE, selected == TransitionMode.NONE, { onSelect(TransitionMode.NONE) })
        }
    }
}

@Composable
private fun actionStyle(): TextStyle = CivilightTheme.type.label.merge(TextStyle(fontSize = 12.sp))

private const val PLACE_PREFILLED = "prefilled"
private const val PLACE_CLEARED = "cleared"
private const val PLACE_PICKED = "picked"
