package io.github.lenticualrstone.civilight.feature.script.scenes

import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.feature.script.ui.SheetStep
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.story.scene.MergeSide
import io.github.lenticualrstone.civilight.domain.story.scene.SilentCastChoice
import io.github.lenticualrstone.civilight.domain.story.scene.TransitionOnMerge
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.navigation.SceneMergeSheet
import io.github.lenticualrstone.civilight.navigation.SceneSplitSheet
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** The action context of a shared sheet (no screen view model behind it). */
internal fun FeatureDeps.actionContext(): ActionContext =
    ActionContext(now = clock.now(), zone = clock.zone, device = stores.deviceId.value, newKey = Keys::newKey)

private val sheetJson = Json { ignoreUnknownKeys = true }

private val splitSaver = Saver<SplitChoices, String>(
    save = { sheetJson.encodeToString(SplitChoices.serializer(), it) },
    restore = { runCatching { sheetJson.decodeFromString(SplitChoices.serializer(), it) }.getOrNull() },
)

private val mergeSaver = Saver<MergeChoices, String>(
    save = { sheetJson.encodeToString(MergeChoices.serializer(), it) },
    restore = { runCatching { sheetJson.decodeFromString(MergeChoices.serializer(), it) }.getOrNull() },
)

/**
 * `씬 나누기` (SCR-50, board `RSSplit`): the shared `SceneSplitSheet`, opened by `/씬` and `새 씬` in
 * the script. Part cards, the new scene's `장소 · 때 · 제목`, the silent cast, `회상` / `숨김`,
 * `바뀌는 것`, primary `나누기`. Nothing changes before `나누기`, which is one undo step written
 * through the project's store.
 */
@Composable
fun SheetScope.SceneSplitSheetContent(sheet: SceneSplitSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(sheet.projectKey)
    var choices by rememberSaveable(stateSaver = splitSaver) { mutableStateOf(SplitChoices()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    SheetStep(
        picking = picking,
        picker = {
        PlacePickerSheet(
            doc = doc,
            gate = gate,
            title = SceneCopy.PICK_PLACE,
            onPick = { ref ->
                choices = choices.pick(ref)
                picking = false
            },
            onClose = { picking = false },
            selected = choices.picked,
            creator = io.github.lenticualrstone.civilight.feature.script.ui.scriptPickerCreator(io.github.lenticualrstone.civilight.designsystem.component.CreateKind.PLACE),
        )
        },
    ) {
    val model = remember(doc, sheet.blockKey, choices, gate.open, store.readOnly) {
        SplitModel.of(doc, sheet.blockKey, choices, gate.open, store.readOnly, deps.actionContext())
    }
    SheetHeader(title = model?.title ?: SceneCopy.SPLIT_TITLE, onClose = ::close, subtitle = model?.sub)
    if (model == null) return@SheetStep
    var editingTime by rememberSaveable { mutableStateOf(false) }
    var editingTitle by rememberSaveable { mutableStateOf(false) }
    var titleField by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(choices.title, TextRange(choices.title.length))) }
    SheetScrollBody(Modifier.padding(top = 10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { model.parts.forEach { PartCardView(it) } }
        Column(Modifier.padding(top = 10.dp)) {
            FieldRow(SceneCopy.FIELD_PLACE, model.place, SceneCopy.CHANGE.takeIf { model.canApply }) { picking = true }
            FieldRow(SceneCopy.FIELD_TIME, model.time, SceneCopy.CHANGE.takeIf { model.canApply && model.timeChoice }) { editingTime = !editingTime }
            FoldReveal(editingTime && model.timeChoice) {
                SegmentedControl(
                    options = listOf(SceneCopy.SAME_TIME, SceneCopy.NO_TIME),
                    selectedIndex = if (choices.sameTime) 0 else 1,
                    onSelect = { choices = choices.copy(sameTime = it == 0) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            FieldRow(SceneCopy.FIELD_TITLE, model.titleLine, SceneCopy.WRITE.takeIf { model.canApply }) { editingTitle = !editingTitle }
            FoldReveal(editingTitle) {
                CivilightTextField(
                    value = titleField,
                    onValueChange = {
                        titleField = it
                        choices = choices.copy(title = it.text)
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    placeholder = SceneCopy.UNTITLED,
                )
            }
        }
        model.silentRows.forEach { row ->
            Column(Modifier.fillMaxWidth().topHairline(CivilightTheme.colors.line).padding(top = 8.dp, bottom = 4.dp)) {
                CivilightText(row.label, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                SegmentedControl(
                    options = row.chips,
                    selectedIndex = row.selected,
                    onSelect = { i ->
                        val side = when (i) {
                            0 -> SilentCastChoice.FIRST
                            1 -> SilentCastChoice.NEW
                            else -> SilentCastChoice.BOTH
                        }
                        choices = choices.copy(silentCast = choices.silentCast + (row.characterKey to side))
                    },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        if (model.flashbackRow) {
            SwitchRow(SceneCopy.FLASHBACK, choices.flashback, { choices = choices.copy(flashback = it) }, enabled = model.canApply)
        }
        if (model.hiddenRow) {
            SwitchRow(SceneCopy.HIDDEN, choices.hidden, { choices = choices.copy(hidden = it) }, enabled = model.canApply)
        }
        ChangeRows(model.changes)
    }
    SheetActions {
        CivilightButton(
            text = SceneCopy.SPLIT,
            onClick = {
                val chosen = choices
                deps.appScope.launch {
                    try {
                        store.update(SceneCopy.SPLIT_TITLE) { d -> SplitModel.apply(d, sheet.blockKey, chosen, deps.actionContext()) }
                    } catch (e: ReadOnlyProjectException) {
                        // The example project: nothing changes.
                    }
                }
                host.close()
            },
            modifier = Modifier.weight(1f),
            subText = if (!model.canApply) SceneCopy.READ_ONLY else null,
            enabled = model.canApply,
        )
    }
    }
}

/**
 * `씬 합치기` (SCR-51, board `RSMerge`): the shared `SceneMergeSheet`, opened from the head ⋯ and the
 * story list's ⋯ `앞 씬과 합치기`. Blocked merges show the reason and no button; nothing changes before
 * `합치기`, which is one undo step (`되돌리기` restores both scenes).
 */
@Composable
fun SheetScope.SceneMergeSheetContent(sheet: SceneMergeSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    var choices by rememberSaveable(stateSaver = mergeSaver) { mutableStateOf(MergeChoices()) }
    val model = remember(doc, sheet.sceneKey, choices, store.readOnly) {
        MergeModel.of(doc, sheet.sceneKey, choices, store.readOnly, deps.actionContext())
    }
    when (model) {
        MergeSheetModel.NotOffered -> SheetHeader(title = model.title, onClose = ::close)
        is MergeSheetModel.Blocked -> {
            SheetHeader(title = model.title, onClose = ::close)
            CivilightText(model.reason, CivilightTheme.type.command, Modifier.padding(top = 12.dp, bottom = 8.dp), color = CivilightTheme.colors.warn)
        }
        is MergeSheetModel.Ready -> {
            SheetHeader(title = model.title, onClose = ::close, subtitle = model.sub)
            SheetScrollBody(Modifier.padding(top = 10.dp)) { MergeBody(model, onChoices = { choices = it(choices) }) }
            SheetActions {
                CivilightButton(
                    text = SceneCopy.MERGE,
                    onClick = {
                        val chosen = choices
                        deps.appScope.launch {
                            try {
                                store.update(SceneCopy.MERGE_TITLE) { d -> MergeModel.apply(d, sheet.sceneKey, chosen, deps.actionContext()) }
                            } catch (e: ReadOnlyProjectException) {
                                // The example project: nothing changes.
                            }
                        }
                        host.close()
                    },
                    modifier = Modifier.weight(1f),
                    subText = if (!model.canApply) SceneCopy.READ_ONLY else null,
                    enabled = model.canApply,
                )
            }
        }
    }
}

@Composable
private fun MergeBody(model: MergeSheetModel.Ready, onChoices: ((MergeChoices) -> MergeChoices) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    model.place?.let { FieldRow(SceneCopy.FIELD_PLACE, it, action = null) {} }
    if (model.placeRadios.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(top = 10.dp)) {
            CivilightText(SceneCopy.FIELD_PLACE, type.small, color = colors.ink3)
        }
        model.placeRadios.forEachIndexed { i, r ->
            RadioRow(r.title, r.selected, { onChoices { it.copy(place = if (i == 0) MergeSide.FIRST else MergeSide.SECOND) } }, subtitle = r.sub, divider = false)
        }
    }
    model.time?.let { FieldRow(SceneCopy.FIELD_TIME, it, action = null) {} }
    model.timeHeader?.let { header ->
        Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(top = 10.dp)) { CivilightText(header, type.small, color = colors.ink3) }
    }
    model.timeRadios.forEachIndexed { i, r ->
        RadioRow(r.title, r.selected, { onChoices { it.copy(time = if (i == 0) MergeSide.FIRST else MergeSide.SECOND) } }, subtitle = r.sub, divider = false)
    }
    model.timeNote?.let { CivilightText(it, type.small, Modifier.padding(top = 2.dp, bottom = 10.dp), color = colors.ink3) }
    model.fixedPointNote?.let { CivilightText(it, type.small, Modifier.padding(bottom = 10.dp), color = colors.ink3) }
    if (model.transitionLine != null) {
        Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(top = 10.dp)) {
            CivilightText(model.transitionLine, type.small, color = colors.ink3)
            model.transitionText?.let { CivilightText(it, serif(14, 20), Modifier.padding(top = 2.dp), color = colors.ink) }
        }
        SegmentedControl(
            options = listOf(SceneCopy.KEEP_AS_SYSTEM, SceneCopy.DISCARD),
            selectedIndex = model.transitionIndex,
            onSelect = { i -> onChoices { it.copy(transition = if (i == 0) TransitionOnMerge.KEEP_AS_SYSTEM_TEXT else TransitionOnMerge.DISCARD) } },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    ChangeRows(model.changes)
}

/** A part card: 2 dp bar (`ink3`, `acc` for the new scene), serif title, mono count, serif range. */
@Composable
private fun PartCardView(card: PartCard) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val bar = if (card.isNew) colors.acc else colors.ink3
    Column(Modifier.fillMaxWidth().leftBar(bar).padding(start = 12.dp, top = 3.dp, bottom = 3.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightText(card.title, type.sceneHead, color = colors.ink, maxLines = 1)
            CivilightText(card.count, type.number.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3, maxLines = 1)
        }
        if (card.range.isNotEmpty()) {
            CivilightText(card.range, serif(13, 19), color = colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** `장소 · 결사의 문 · 씬 3과 같음 · 바꾸기 ›` (min 44, top hairline, label 40 wide). */
@Composable
private fun FieldRow(label: String, line: FieldLine, action: String?, onAction: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CivilightText(label, type.small, Modifier.width(40.dp), color = colors.ink3, maxLines = 1)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CivilightText(line.value, type.command.merge(TextStyle(fontSize = 14.sp, lineHeight = 20.sp)), Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            line.note?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
        }
        if (action != null) TextAction(action, onAction, style = type.label.merge(TextStyle(fontSize = 12.sp)))
    }
}

/** `바뀌는 것` and its rows (`.rk`: label 64 wide in `ink3` 12, value 13 / 19). */
@Composable
private fun ChangeRows(rows: List<ChangeLine>) {
    if (rows.isEmpty()) return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CivilightText(SceneCopy.CHANGES, type.sec, Modifier.padding(top = 14.dp, bottom = 4.dp), color = colors.ink3)
    rows.forEach { row ->
        Row(
            Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CivilightText(row.label, type.small.merge(TextStyle(fontSize = 12.sp)), Modifier.width(64.dp), color = colors.ink3)
            CivilightText(row.value, type.command.merge(TextStyle(fontSize = 13.sp, lineHeight = 19.sp)), Modifier.weight(1f), color = colors.ink)
        }
    }
}

@Composable
private fun serif(size: Int, line: Int): TextStyle =
    CivilightTheme.type.sceneHead.merge(TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight.Normal))

/** The part card's 2 dp bar at the start (`box-shadow: inset 2px 0 0`). */
private fun Modifier.leftBar(color: Color): Modifier = drawBehind {
    drawRect(color, topLeft = Offset.Zero, size = Size(2.dp.toPx(), size.height))
}
