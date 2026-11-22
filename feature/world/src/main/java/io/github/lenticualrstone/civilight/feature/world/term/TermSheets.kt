package io.github.lenticualrstone.civilight.feature.world.term

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.RelatedOptions
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PickerCopy
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import kotlinx.serialization.Serializable

/** `공개 시점 옮기기` (WLD-18), opened from the `첫 공개` row. */
@Serializable
data class RevealSheet(val termKey: Key) : SheetKey

/** `원고에서 고르기`, step 1: the scene (the script has no block-pick mode yet, see Requests). */
@Serializable
data class RevealScenePickSheet(val termKey: Key) : SheetKey

/** `원고에서 고르기`, step 2: a block of [sceneKey]. */
@Serializable
data class RevealBlockPickSheet(val termKey: Key, val sceneKey: Key) : SheetKey

/** `에피소드 · 장으로`: a chapter or episode point (`공개 예정 2-5`). */
@Serializable
data class RevealPointPickSheet(val termKey: Key) : SheetKey

/** `관련` (term fixed field): tick terms, places and factions. */
@Serializable
data class RelatedSheet(val termKey: Key) : SheetKey

/** The term card's own sheets; null for other keys. [choose] applies a reveal and closes the sheets. */
fun termSheet(
    key: SheetKey,
    doc: ProjectDoc?,
    readOnly: Boolean,
    gate: HiddenGate,
    choose: (RevealTarget) -> Unit,
    toggleRelated: (io.github.lenticualrstone.civilight.model.world.ItemRef) -> Unit,
): (@Composable SheetScope.() -> Unit)? {
    if (doc == null) return null
    return when (key) {
        is RevealSheet -> { { RevealContent(key.termKey, doc, readOnly, gate, choose) } }
        is RevealScenePickSheet -> {
            {
                val sheet = this
                UnitPickerSheet(
                    doc = doc,
                    gate = gate,
                    title = WorldCopy.REVEAL_FROM_SCRIPT,
                    onPick = { sceneKey ->
                        sheet.close()
                        sheet.open(RevealBlockPickSheet(key.termKey, sceneKey))
                    },
                    onClose = ::close,
                    levels = RevealOptions.sceneLevels(doc),
                )
            }
        }
        is RevealBlockPickSheet -> { { BlockPickContent(key, doc, readOnly, choose) } }
        is RevealPointPickSheet -> {
            {
                val current = io.github.lenticualrstone.civilight.domain.world.term.TermReveals.record(doc, key.termKey)?.anchor
                UnitPickerSheet(
                    doc = doc,
                    gate = gate,
                    title = WorldCopy.REVEAL_TO_UNIT,
                    onPick = { unitKey -> if (!readOnly) choose(RevealTarget.Unit(unitKey)) },
                    onClose = ::close,
                    selectedKey = (current as? io.github.lenticualrstone.civilight.model.change.Anchor.UnitStart)?.unitKey,
                    levels = RevealOptions.pointLevels(doc),
                )
            }
        }
        is RelatedSheet -> { { RelatedContent(key.termKey, doc, readOnly, gate, toggleRelated) } }
        else -> null
    }
}

@Composable
private fun SheetScope.RevealContent(termKey: Key, doc: ProjectDoc, readOnly: Boolean, gate: HiddenGate, choose: (RevealTarget) -> Unit) {
    val links = remember(doc, gate.open) { RevealOptions.links(doc, termKey, gate.open) }
    val sheet = this
    SheetHeader(title = WorldCopy.REVEAL_TITLE, onClose = ::close)
    SheetScrollBody {
        if (links.isNotEmpty()) {
            SectionLabel(WorldCopy.REVEAL_LINKS, Modifier.padding(top = 16.dp, bottom = 4.dp))
            links.forEach { option ->
                ListRow(
                    title = option.label,
                    subtitle = option.refusal ?: if (readOnly) WorldCopy.READ_ONLY else null,
                    onClick = if (option.refusal == null && !readOnly && !option.current) ({ choose(option.target) }) else null,
                    selected = option.current,
                    compact = true,
                )
            }
        }
        SettingsRow(
            title = WorldCopy.REVEAL_FROM_SCRIPT,
            onClick = { sheet.open(RevealScenePickSheet(termKey)) },
            modifier = Modifier.padding(top = 12.dp),
        )
        SettingsRow(title = WorldCopy.REVEAL_TO_UNIT, onClick = { sheet.open(RevealPointPickSheet(termKey)) })
    }
}

@Composable
private fun SheetScope.BlockPickContent(key: RevealBlockPickSheet, doc: ProjectDoc, readOnly: Boolean, choose: (RevealTarget) -> Unit) {
    val options = remember(doc, key) { RevealOptions.blocks(doc, key.termKey, key.sceneKey) }
    val scene = remember(doc, key) { UnitLabels(doc).reference(key.sceneKey) }
    SheetHeader(title = WorldCopy.REVEAL_FROM_SCRIPT, onClose = ::close, subtitle = scene)
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        items(options, key = { it.id }) { option ->
            ListRow(
                title = option.label,
                subtitle = if (readOnly) WorldCopy.READ_ONLY else null,
                onClick = if (!readOnly && !option.current) ({ choose(option.target) }) else null,
                selected = option.current,
                compact = true,
            )
        }
    }
}

@Composable
private fun SheetScope.RelatedContent(
    termKey: Key,
    doc: ProjectDoc,
    readOnly: Boolean,
    gate: HiddenGate,
    toggle: (io.github.lenticualrstone.civilight.model.world.ItemRef) -> Unit,
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val ui = remember(doc, field.text, gate.open) { RelatedOptions.build(doc, termKey, field.text, gate.open) }
    SheetHeader(title = WorldCopy.ROW_RELATED, onClose = ::close)
    SearchField(
        value = field,
        onValueChange = { field = it },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        placeholder = PickerCopy.SEARCH_PLACEHOLDER,
    )
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        ui.sections.forEachIndexed { i, section ->
            item(key = "sec:$i") { SectionLabel(section.title, Modifier.padding(top = 12.dp)) }
            items(section.options, key = { "${it.ref.kind.raw}:${it.ref.key}" }) { option ->
                CheckboxRow(
                    label = option.title,
                    checked = option.checked,
                    onCheckedChange = { toggle(option.ref) },
                    enabled = !readOnly,
                )
            }
        }
        if (ui.hiddenCount > 0) {
            item(key = "hidden") {
                LockRow(
                    title = PickerCopy.hiddenRow(PickerCopy.KIND_ITEM, ui.hiddenCount),
                    actionLabel = WorldCopy.UNLOCK,
                    onAction = gate.unlock,
                )
            }
        }
    }
}
