package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.picker.FactionPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.TermPickerSheet
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.github.lenticualrstone.civilight.domain.trash.MergeChoices as TrashMergeChoices

/** `지우기 · 영향` of a world item (WLD-14; feature-local until cast's `DeleteImpactSheet` serves world kinds). */
@Serializable
data class DeleteSheet(val kind: String, val key: Key) : SheetKey

/** `합치기`, step 1: the item to merge into (the list picker of the kind, WLD-15). */
@Serializable
data class MergePickSheet(val kind: String, val key: Key) : SheetKey

/** `합치기`, step 2: the comparison sheet (feature-local until cast's `MergeItemsSheet` serves world kinds). */
@Serializable
data class MergeSheet(val kind: String, val mergedKey: Key, val targetKey: Key) : SheetKey

/** The `⋯ › 지우기` row: the impact sheet when something would be affected, else at once with the toast (TC-20-03). */
@Composable
fun rememberDeleteAction(vm: WorldViewModel, kind: WorldKind, key: Key): (SheetScope) -> Unit {
    val deps = LocalFeatureDeps.current
    val scope = rememberCoroutineScope()
    return { sheet ->
        scope.launch {
            val impact = vm.impact(ItemLifecycle.itemId(kind, key)) ?: return@launch
            if (impact.needsSheet) {
                sheet.close()
                sheet.open(DeleteSheet(kind.name, key))
            } else {
                // Delete first, then close: the menu's scope ends with the sheet.
                val deleted = vm.deleteItem(ItemLifecycle.itemId(kind, key))
                sheet.close()
                deleted?.let { deps.toasts.show(it.toast.message, actionLabel = it.toast.action) { vm.undoLast() } }
            }
        }
    }
}

/**
 * The impact sheet (board `RCDeleteImpact`): title `‘잿더미 시장’ 지우기`, sub `장소 · 참조 9`,
 * `같이 휴지통으로` rows, `제자리에 남음 · 지운 장소로 보임` rows, `점검 센터`, `중복이면 · 지우지 않고
 * 합치기` with `다른 장소와 합치기 ›`, footnote, `취소 · 휴지통으로`.
 */
@Composable
fun SheetScope.DeleteSheetContent(key: DeleteSheet, vm: WorldViewModel, readOnly: Boolean) {
    val deps = LocalFeatureDeps.current
    val kind = WorldKind.valueOf(key.kind)
    val item = ItemLifecycle.itemId(kind, key.key)
    val impact by produceState<io.github.lenticualrstone.civilight.domain.trash.DeleteImpact?>(null, key) { value = vm.impact(item) }
    val scope = rememberCoroutineScope()
    val sheet = this
    val ui = impact?.let(MergePlan::deleteSheet) ?: return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(title = ui.title, onClose = ::close, subtitle = ui.sub)
    SheetScrollBody {
        if (ui.owned.isNotEmpty()) {
            SectionLabel(ui.ownedTitle, Modifier.padding(top = 14.dp, bottom = 2.dp))
            ui.owned.forEach { FieldTableRow(it.label.substringBeforeLast(' '), it.label.substringAfterLast(' ') + (it.sub?.let { s -> WorldCopy.SEP + s } ?: "")) }
        }
        if (ui.staying.isNotEmpty()) {
            SectionLabel(ui.stayingTitle, Modifier.padding(top = 14.dp, bottom = 2.dp))
            ui.staying.forEach { FieldTableRow(it.label.substringBeforeLast(' '), it.label.substringAfterLast(' ') + (it.sub?.let { s -> WorldCopy.SEP + s } ?: "")) }
            FieldTableRow(ui.checkRow.first, ui.checkRow.second)
        }
        FieldTableRow(ui.duplicateRow.first, ui.duplicateRow.second)
        ui.mergeAction?.let { label ->
            TextAction(label, onClick = { sheet.close(); sheet.open(MergePickSheet(key.kind, key.key)) }, color = colors.acc, style = type.small)
        }
        CivilightText(ui.footnote, type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
    }
    SheetActions {
        CivilightButton(text = ui.cancel, onClick = { sheet.close() }, modifier = Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            text = if (readOnly) WorldCopy.READ_ONLY else ui.confirm,
            onClick = {
                scope.launch {
                    val deleted = vm.deleteItem(item) ?: return@launch
                    sheet.close()
                    deps.toasts.show(deleted.toast.message, actionLabel = deleted.toast.action) { vm.undoLast() }
                }
            },
            modifier = Modifier.weight(1f),
            variant = ButtonVariant.DANGER,
            enabled = !readOnly,
        )
    }
}

/** Step 1 of `합치기`: the kind's list picker; the item itself cannot be picked. */
@Composable
fun SheetScope.MergePickContent(key: MergePickSheet, doc: ProjectDoc, gate: HiddenGate) {
    val kind = WorldKind.valueOf(key.kind)
    val sheet = this
    val pick: (Key) -> Unit = { target ->
        if (target != key.key && MergePlan.canMerge(doc, kind, key.key, target)) {
            sheet.close()
            sheet.open(MergeSheet(key.kind, key.key, target))
        }
    }
    when (kind) {
        WorldKind.TERM -> TermPickerSheet(doc, gate, WorldCopy.MENU_MERGE, onPick = { target, _ -> pick(target) }, onClose = ::close)
        WorldKind.PLACE -> PlacePickerSheet(
            doc, gate, WorldCopy.MENU_MERGE,
            onPick = { ref -> if (ref.kind.known == PlaceRefKind.PLACE) pick(ref.key) },
            onClose = ::close,
            includeRegions = false,
        )
        WorldKind.FACTION -> FactionPickerSheet(doc, gate, WorldCopy.MENU_MERGE, onPick = pick, onClose = ::close)
    }
}

/**
 * Step 2 (board `RCMerge`): `<A> → <B> · 기본은 <B> 값`, `옮길 참조 · N`, `다른 칸 · N` with a choice per
 * row (target by default), the term `첫 공개` row, `경로 끝 N · <B> 핀으로`, `빈 칸 채움 · N`, the footer
 * `<A> · ‘합침 → <B>’로 휴지통`, `취소 · 합치기`. After merging, the target's card opens.
 */
@Composable
fun SheetScope.MergeSheetContent(key: MergeSheet, doc: ProjectDoc, vm: WorldViewModel, readOnly: Boolean) {
    val deps = LocalFeatureDeps.current
    val kind = WorldKind.valueOf(key.kind)
    var fromMerged by rememberSaveable(key) { mutableStateOf(listOf<String>()) }
    var revealFromMerged by rememberSaveable(key) { mutableStateOf(false) }
    val choices = TrashMergeChoices(fieldsFromMerged = fromMerged.toSet(), revealFromMerged = revealFromMerged)
    val ui = remember(doc, key, choices) { MergePlan.mergeSheet(doc, kind, key.mergedKey, key.targetKey, choices) }
    val scope = rememberCoroutineScope()
    val sheet = this
    if (ui == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(title = ui.title, onClose = ::close, subtitle = ui.sub, closeLabel = WorldCopy.BTN_CANCEL)
    SheetScrollBody {
        SectionLabel(ui.refsLine, Modifier.padding(top = 14.dp))
        FoldRevealValue(ui.refsParts.takeIf { it.isNotEmpty() }) { parts -> CivilightText(parts, type.small, Modifier.padding(top = 2.dp), color = colors.ink3) }
        ui.conflictsLine?.let { line ->
            SectionLabel(line, Modifier.padding(top = 16.dp, bottom = 2.dp))
            (ui.conflicts + listOfNotNull(ui.reveal)).forEach { row ->
                Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    CivilightText(row.field, type.small, color = colors.ink3)
                    RadioRow(
                        title = row.mergedValue,
                        subtitle = ui.mergedHead,
                        selected = row.fromMerged,
                        onClick = {
                            val next = MergePlan.toggle(choices, row, fromMerged = true)
                            fromMerged = next.fieldsFromMerged.toList(); revealFromMerged = next.revealFromMerged
                        },
                        enabled = !readOnly,
                    )
                    RadioRow(
                        title = row.targetValue,
                        subtitle = ui.targetHead,
                        selected = !row.fromMerged,
                        onClick = {
                            val next = MergePlan.toggle(choices, row, fromMerged = false)
                            fromMerged = next.fieldsFromMerged.toList(); revealFromMerged = next.revealFromMerged
                        },
                        enabled = !readOnly,
                    )
                }
            }
        }
        FoldRevealValue(ui.routeEnds) { line -> CivilightText(line, type.small, Modifier.padding(top = 12.dp), color = colors.ink2) }
        FoldRevealValue(ui.filledLine) { line -> CivilightText(line, type.small, Modifier.padding(top = 8.dp), color = colors.ink2) }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Start) {
            CivilightText(ui.footer, type.small, color = colors.ink3)
        }
    }
    SheetActions {
        CivilightButton(text = ui.cancel, onClick = { sheet.close() }, modifier = Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            text = if (readOnly) WorldCopy.READ_ONLY else ui.confirm,
            onClick = {
                scope.launch {
                    val merged = vm.merge(kind, key.mergedKey, key.targetKey, choices) ?: return@launch
                    sheet.close()
                    deps.toasts.show(merged.toast.message, actionLabel = merged.toast.action) { merged.undo() }
                    deps.navigator.openDetail(WorldTargets.card(doc.key, kind, key.targetKey))
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !readOnly,
        )
    }
}
