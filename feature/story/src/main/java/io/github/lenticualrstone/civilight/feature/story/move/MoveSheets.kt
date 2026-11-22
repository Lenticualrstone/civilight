package io.github.lenticualrstone.civilight.feature.story.move

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.feature.story.menu.storyActions
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SceneMoveSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable

/**
 * `옮기기` destination picker (feature-local, STO-08): first the parent unit (the shared unit picker,
 * one level up), then the slot `맨 앞` · `씬 3 뒤` …. A scene goes on to the `SceneMoveSheet`
 * preview; an episode or chapter moves at once (one undo step). Top-level units skip the parent step.
 */
@Serializable
data class MoveTargetSheet(
    val projectKey: Key,
    val unitKey: Key,
    val parentKey: Key? = null,
    val parentPicked: Boolean = false,
) : SheetKey

@Composable
fun SheetScope.MoveTargetContent(sheet: MoveTargetSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val actions = storyActions(sheet.projectKey, deps)
    if (!MoveModel.movable(doc, sheet.unitKey)) {
        SheetHeader(title = MoveCopy.MOVE, onClose = ::close)
        return
    }
    val parentLevel = MoveModel.parentLevel(doc, sheet.unitKey)
    if (parentLevel != null && !sheet.parentPicked) {
        UnitPickerSheet(
            doc = doc,
            gate = rememberHiddenGate(sheet.projectKey),
            title = MoveCopy.MOVE,
            onPick = { parent -> swap(sheet.copy(parentKey = parent, parentPicked = true)) },
            onClose = ::close,
            selectedKey = doc.index.unit(sheet.unitKey)?.parentKey,
            levels = setOf(parentLevel),
        )
        return
    }
    val slots = remember(doc, sheet) { MoveModel.slots(doc, sheet.unitKey, sheet.parentKey) }
    val parentLabel = sheet.parentKey?.let { UnitCodes(doc).unitLabel(it) }
    SheetHeader(title = MoveCopy.MOVE, onClose = ::close, subtitle = parentLabel)
    SheetScrollBody {
        slots.forEach { slot ->
            SlotRow(slot.label, enabled = !slot.current && !store.readOnly) {
                val parent = slot.parentKey
                if (MoveModel.isScene(doc, sheet.unitKey) && parent != null) {
                    host.swap(SceneMoveSheet(sheet.projectKey, sheet.unitKey, parent, slot.afterKey))
                } else {
                    actions.apply(MoveCopy.MOVE) { d, ctx -> MoveModel.moveUnit(d, sheet.unitKey, parent, slot.afterKey, ctx) }
                    host.close()
                }
            }
        }
    }
}

/**
 * `SceneMoveSheet` content (shared, owner STORY; SCR-54 layout, board `RSSceneMove`): `씬 옮기기` ·
 * `미리 보기 · 옮기기 전에는 그대로`, the from | to line, `바뀌는 것` with `기록 · 블록 번호 · 보이스 ID ·
 * 때`, and `취소` · `옮기기`. Nothing changes before `옮기기` (R12 §3); the move is one undo step.
 */
@Composable
fun SheetScope.SceneMoveContent(sheet: SceneMoveSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val actions = storyActions(sheet.projectKey, deps)
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val previewCtx = remember { deps.storyContext() }
    val preview = remember(doc, sheet) { MoveModel.preview(doc, sheet.sceneKey, sheet.targetParentKey, sheet.afterKey, previewCtx) }
    if (preview == null) {
        SheetHeader(title = "", onClose = ::close)
        return
    }
    SheetHeader(title = preview.title, onClose = ::close, subtitle = preview.sub)
    SheetScrollBody(Modifier.padding(top = 10.dp)) {
        listOf(preview.fromLine, preview.toLine).forEach { line ->
            val parts = line.split(" | ")
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CivilightText(parts.getOrElse(0) { "" }, type.small, Modifier.width(96.dp), color = colors.ink3, maxLines = 1)
                CivilightText(parts.getOrElse(1) { "" }, type.itemTitleCompact, Modifier.weight(1f), color = colors.ink, maxLines = 1)
            }
        }
        SectionLabel(MoveCopy.CHANGES, Modifier.padding(top = 12.dp))
        listOf(
            MoveCopy.ROW_RECORDS to preview.recordsRow,
            MoveCopy.ROW_BLOCKS to preview.blocksRow,
            MoveCopy.ROW_VOICE to preview.voiceRow,
            MoveCopy.ROW_TIME to preview.timeRow,
        ).forEach { (key, value) ->
            Row(Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CivilightText(key, type.small, Modifier.width(72.dp), color = colors.ink3, maxLines = 1)
                CivilightText(value, type.command, Modifier.weight(1f), color = colors.ink, maxLines = 2)
            }
        }
    }
    SheetActions {
        CivilightButton(MoveCopy.CANCEL, { host.close() }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            MoveCopy.MOVE,
            {
                actions.apply(preview.title) { d, ctx -> MoveModel.moveScene(d, sheet.sceneKey, sheet.targetParentKey, sheet.afterKey, ctx) }
                host.close()
            },
            Modifier.weight(2f),
            subText = if (store.readOnly) StoryCopy.READ_ONLY else null,
            enabled = !store.readOnly,
        )
    }
}

@Composable
private fun SlotRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val alpha by animatedAlpha(if (enabled) 1f else DISABLED)
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = onClick, enabled = enabled, scaleOnPress = false)
            .graphicsLayer { this.alpha = alpha }
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(label, CivilightTheme.type.itemTitleCompact, color = colors.ink, maxLines = 1)
    }
}

private fun SheetScope.swap(next: SheetKey) {
    close()
    open(next)
}

/** A preview-only action context (keys minted for the preview are discarded; the move recomputes). */
private fun FeatureDeps.storyContext() = storyServices().actionContext()

private const val DISABLED = 0.38f
