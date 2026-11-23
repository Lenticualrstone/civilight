package io.github.lenticualrstone.civilight.feature.library.newproject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.ChoiceCard
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.domain.cast.field.FieldBundles
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.feature.library.rating.RatingCells
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode

internal typealias FormUpdate = ((NewProjectForm) -> NewProjectForm) -> Unit

internal fun formatLabel(format: ProjectFormat): String = when (format) {
    ProjectFormat.SERIAL -> NewProjectCopy.FORMAT_SERIAL
    ProjectFormat.SHORT -> NewProjectCopy.FORMAT_SHORT
    ProjectFormat.SETTING_ONLY -> NewProjectCopy.FORMAT_SETTING
}

@Composable
private fun FieldCaption(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(text, type.small, Modifier.weight(1f), color = colors.ink3)
        if (trailing != null) CivilightText(trailing, type.small, color = colors.ink3)
    }
}

/** 1/4 `이름과 형식` (LIB-09; boards `NewName`, `NewNameR18`). */
@Composable
internal fun ColumnScope.NameStep(form: NewProjectForm, defaults: RatingDefaults, recoverySet: Boolean, update: FormUpdate) {
    CivilightTextField(value = form.name, onValueChange = { v -> update { it.copy(name = v) } }, label = NewProjectCopy.NAME)
    CivilightTextField(
        value = form.oneLiner,
        onValueChange = { v -> update { it.copy(oneLiner = v) } },
        label = NewProjectCopy.ONE_LINER,
        placeholder = NewProjectCopy.ONE_LINER_PLACEHOLDER,
    )
    FieldCaption(NewProjectCopy.FORMAT)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(ProjectFormat.SERIAL, NewProjectCopy.FORMAT_SERIAL, NewProjectCopy.FORMAT_SERIAL_SUB),
            Triple(ProjectFormat.SHORT, NewProjectCopy.FORMAT_SHORT, NewProjectCopy.FORMAT_SHORT_SUB),
            Triple(ProjectFormat.SETTING_ONLY, NewProjectCopy.FORMAT_SETTING, NewProjectCopy.FORMAT_SETTING_SUB),
        ).forEach { (format, title, sub) ->
            ChoiceCard(title, selected = form.format == format, onClick = { update { NewProjectRules.withFormat(it, format) } }, subtitle = sub)
        }
    }
    FieldCaption(NewProjectCopy.RATING, trailing = NewProjectCopy.RATING_NOTE)
    RatingCells(form.rating, onSelect = { r -> update { it.copy(rating = r) } })
    FoldRevealValue(NewProjectRules.r18Block(form, defaults, recoverySet)) { block ->
        val colors = CivilightTheme.colors
        val type = CivilightTheme.type
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.sel).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CivilightText(block.header, type.small, color = colors.ink3)
            block.lines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CivilightIcon(IconId.Check, contentDescription = null, size = 16.dp, tint = colors.acc)
                    CivilightText(line, type.command.merge(TextStyle(fontSize = 13.sp, lineHeight = 18.sp)), color = colors.ink)
                }
            }
            block.warning?.let { warning ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CivilightText("◇", type.command, color = colors.ink2)
                    CivilightText(warning, type.command.merge(TextStyle(fontSize = 13.sp, lineHeight = 18.sp)), color = colors.ink)
                }
            }
        }
    }
}

/**
 * The `미리 보기` tree: top level bold, scene level `ink2`, indented by depth (`NewStructure`). The box stays
 * put and the lines crossfade, its height following on `standard`, when [swapKey] changes (another structure,
 * a level added or removed); renaming a level updates the text in place. The whole box folds away while
 * [lines] is empty (a custom level still unnamed).
 */
@Composable
internal fun PreviewTree(lines: List<Pair<Int, String>>, modifier: Modifier = Modifier, swapKey: Any? = NewProjectRules.previewSwapKey(null, lines)) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    FoldRevealValue(PreviewLines(swapKey, lines).takeIf { lines.isNotEmpty() }, label = "previewTree") { shown ->
        // Boards `NewStructure` / `NewWide`: box padding 14 · 16, each line 4 dp above and below, 18 dp per level.
        Box(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.tint).padding(horizontal = 16.dp, vertical = 14.dp)) {
            ContentSwap(shown, Modifier.fillMaxWidth(), contentKey = { it.key }, label = "previewLines") { state ->
                val deepest = state.lines.maxOfOrNull { it.first } ?: 0
                Column(Modifier.fillMaxWidth()) {
                    state.lines.forEach { (depth, text) ->
                        val style = type.lead.merge(TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Normal))
                        CivilightText(
                            text,
                            style,
                            Modifier.padding(start = (depth * 18).dp, top = 4.dp, bottom = 4.dp),
                            color = if (depth == deepest && depth > 0) colors.ink2 else colors.ink,
                        )
                    }
                }
            }
        }
    }
}

/** The preview lines with the key that decides whether a change swaps (see [NewProjectRules.previewSwapKey]). */
private data class PreviewLines(val key: Any?, val lines: List<Pair<Int, String>>)

/** 2/4 `구조` (LIB-10; boards `NewStructure`, `NewWide`). */
@Composable
internal fun ColumnScope.StructureStep(form: NewProjectForm, wide: Boolean, showPreview: Boolean, update: FormUpdate) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    if (wide) {
        // `NewWide`: the name from step 1, shown read-only (a plain filled box, not a dimmed field).
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldCaption(NewProjectCopy.NAME)
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape).padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                CivilightText(form.name.trim(), type.field, color = colors.ink, maxLines = 1)
            }
        }
        FieldCaption(NewProjectCopy.STRUCTURE_HOW, Modifier.padding(top = 4.dp))
    } else {
        CivilightText(NewProjectCopy.STRUCTURE_DESC, type.lead.merge(TextStyle(fontSize = 15.sp, lineHeight = 25.sp)), color = colors.ink2)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(StructurePreset.CHAPTER_EPISODE_SCENE, NewProjectCopy.PRESET_CHAPTER, NewProjectCopy.PRESET_CHAPTER_SUB),
            Triple(StructurePreset.ACT_SCENE, NewProjectCopy.PRESET_ACT, NewProjectCopy.PRESET_ACT_SUB),
            Triple(StructurePreset.CUSTOM, NewProjectCopy.PRESET_CUSTOM, if (wide) NewProjectCopy.PRESET_CUSTOM_SUB_WIDE else NewProjectCopy.PRESET_CUSTOM_SUB),
        ).forEach { (preset, title, sub) ->
            ChoiceCard(title, selected = form.structurePreset == preset, onClick = { update { NewProjectRules.withPreset(it, preset) } }, subtitle = sub)
        }
    }
    FoldReveal(form.structurePreset == StructurePreset.CUSTOM) { LevelEditor(form, update) }
    FoldReveal(showPreview) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLabel(NewProjectCopy.PREVIEW, Modifier.padding(top = 8.dp))
            PreviewTree(NewProjectRules.preview(form), swapKey = NewProjectRules.previewSwapKey(form))
        }
    }
}

/** `직접 정하기`: 1–4 level names, top first, with add and remove (layout `ED`). */
@Composable
private fun LevelEditor(form: NewProjectForm, update: FormUpdate) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        form.customLevels.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CivilightTextField(
                    value = name,
                    onValueChange = { v -> update { NewProjectRules.setLevel(it, index, v) } },
                    modifier = Modifier.weight(1f),
                )
                CivilightIconButton(
                    IconId.X,
                    NewProjectCopy.REMOVE_LEVEL,
                    onClick = { update { NewProjectRules.removeLevel(it, index) } },
                    enabled = NewProjectRules.canRemoveLevel(form),
                )
            }
        }
        FoldReveal(NewProjectRules.canAddLevel(form)) {
            CivilightIconButton(IconId.Plus, NewProjectCopy.ADD_LEVEL, onClick = { update(NewProjectRules::addLevel) })
        }
    }
}

/** 3/4 `시간` (LIB-11; board `NewTime`). */
@Composable
internal fun ColumnScope.TimeStep(form: NewProjectForm, update: FormUpdate) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(WorldTimeMode.YEAR_ONLY, NewProjectCopy.TIME_YEAR, NewProjectCopy.TIME_YEAR_SUB),
            Triple(WorldTimeMode.CALENDAR, NewProjectCopy.TIME_CALENDAR, NewProjectCopy.TIME_CALENDAR_SUB),
            Triple(WorldTimeMode.NONE, NewProjectCopy.TIME_NONE, NewProjectCopy.TIME_NONE_SUB),
        ).forEach { (mode, title, sub) ->
            ChoiceCard(title, selected = form.timeMode == mode, onClick = { update { it.copy(timeMode = mode) } }, subtitle = sub)
        }
    }
    FoldReveal(form.timeMode != WorldTimeMode.NONE) {
        CivilightTextField(
            value = form.startYear,
            onValueChange = { v -> update { NewProjectRules.withYear(it, v) } },
            label = NewProjectCopy.START_YEAR,
            placeholder = NewProjectCopy.START_YEAR_PLACEHOLDER,
            hint = NewProjectCopy.START_YEAR_HINT,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

/** 4/4 `시작 재료` (LIB-12; board `NewStart`). */
@Composable
internal fun ColumnScope.StartStep(
    form: NewProjectForm,
    hideSwitch: Boolean,
    inputs: StartInputs,
    update: FormUpdate,
    onPickBundle: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CivilightTextField(value = form.protagonist, onValueChange = { v -> update { it.copy(protagonist = v) } }, label = NewProjectCopy.PROTAGONIST)
    FoldReveal(form.format != ProjectFormat.SETTING_ONLY) {
        CivilightTextField(
            value = form.firstSceneTitle,
            onValueChange = { v -> update { it.copy(firstSceneTitle = v) } },
            label = NewProjectCopy.FIRST_SCENE,
            placeholder = NewProjectCopy.FIRST_SCENE_PLACEHOLDER,
        )
    }
    FoldRevealValue(inputs.records.takeIf { it.isNotEmpty() }) { records ->
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val chosen = form.linkedRecordKeys.count { key -> records.any { it.recordKey == key } }
        FieldCaption(RecorderHooks.startMaterialHeader(records.size, chosen))
        Column {
            records.forEach { record ->
                val checked = record.recordKey in form.linkedRecordKeys
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressable(PressAction.Toggle(checked) { update { NewProjectRules.toggleRecord(it, record.recordKey) } }, role = Role.Checkbox, scaleOnPress = false)
                        .heightIn(min = 44.dp)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckboxMark(checked)
                    Column(Modifier.weight(1f)) {
                        CivilightText(record.meta, type.small, color = colors.ink3)
                        CivilightText(record.firstLine, type.lead.merge(TextStyle(fontSize = 15.sp, lineHeight = 24.sp)), color = colors.ink, maxLines = 1)
                    }
                }
            }
        }
      }
    }
    val (title, sub, value) = FieldBundles.newProjectRow(form.bundle)
    val sourceName = form.bundleSourceKey?.let { key -> inputs.bundleSources.firstOrNull { it.key == key }?.name }
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = onPickBundle, role = Role.Button, scaleOnPress = false, onClickLabel = title)
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(title, type.itemTitleCompact, color = colors.ink)
            val shownSub = if (form.bundle == FieldBundle.FROM_PROJECT) sourceName else sub.ifEmpty { null }
            if (shownSub != null) CivilightText(shownSub, type.small, color = colors.ink3, maxLines = 1)
        }
        CivilightText(value, type.label, color = colors.ink2, maxLines = 1)
    }
    SwitchRow(
        title = NewProjectCopy.HIDE,
        checked = hideSwitch,
        onCheckedChange = { on -> update { it.copy(step4Hide = on) } },
        subtitle = NewProjectCopy.HIDE_SUB,
    )
}
