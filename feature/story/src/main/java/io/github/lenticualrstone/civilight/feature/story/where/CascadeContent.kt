package io.github.lenticualrstone.civilight.feature.story.where

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.time.TimeChangePreview
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.menu.storyActions
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable

/**
 * `때 바꾸기` preview as its own sheet (feature-local), for changes started outside the `장소와 때`
 * sheet: the 점검 센터 travel time chip (`씬 3 · +2일로`). [pendingTime] is the new stored 때 as JSON.
 */
@Serializable
data class CascadeSheet(val projectKey: Key, val sceneKey: Key, val pendingTime: String) : SheetKey

/** [CascadeSheet] content: the preview recomputed on the current doc; nothing changes before `바꾸기`. */
@Composable
fun SheetScope.CascadeSheetContent(sheet: CascadeSheet, deps: FeatureDeps) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val actions = storyActions(sheet.projectKey, deps)
    val time = remember(sheet.pendingTime) { WhereModel.decode(sheet.pendingTime) }
    val step = remember(doc, sheet.sceneKey, time) { WhereModel.step(doc, sheet.sceneKey, time) }
    when (step) {
        is TimeStep.Preview -> CascadeBody(step.preview, store.readOnly, onCancel = ::close) { mode ->
            actions.apply(WhereCopy.UNDO_LABEL) { d, ctx -> WhereModel.setTime(d, sheet.sceneKey, step.time, mode, ctx) }
            close()
        }
        is TimeStep.Direct -> {
            // No scene depends on it any more: the change is stored as is (R11 §4), after one confirm.
            val preview = remember(doc, step) { io.github.lenticualrstone.civilight.domain.story.time.TimeChange.preview(doc, sheet.sceneKey, step.time) }
            CascadeBody(preview, store.readOnly, onCancel = ::close) { mode ->
                actions.apply(WhereCopy.UNDO_LABEL) { d, ctx -> WhereModel.setTime(d, sheet.sceneKey, step.time, mode, ctx) }
                close()
            }
        }
        TimeStep.NoChange -> SheetHeader(title = "", onClose = ::close)
    }
}

/**
 * `RTCascade` body: title `씬 1 · 때 바꾸기`, scene line, before → after with `+2일 · 이야기 날`, the
 * summary, up to four rows, `외 n개 …`, the fixed-point row, the two radios and `취소` · `바꾸기`.
 */
@Composable
fun ColumnScope.CascadeBody(preview: TimeChangePreview, readOnly: Boolean, onCancel: () -> Unit, onApply: (CascadeMode) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    var mode by rememberSaveable(preview.sceneKey) { mutableStateOf(CascadeMode.WITH_FOLLOWING) }
    SheetHeader(title = preview.title, onClose = onCancel, subtitle = preview.sceneLine)
    SheetScrollBody(Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            preview.before?.let { CivilightText("$it ${WhereCopy.ARROW}", type.itemTitle, color = colors.ink3, maxLines = 1) }
            preview.after?.let { CivilightText(it, type.itemTitle.merge(TextStyle(fontWeight = FontWeight.Bold)), color = colors.ink, maxLines = 1) }
        }
        preview.deltaLine?.let { CivilightText(it, type.small, color = colors.acc, maxLines = 1) }
        preview.summary?.let { CivilightText(it, type.command, Modifier.padding(top = 12.dp), color = colors.ink) }
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            preview.rows.forEach { row -> CascadeLine(row.sceneRef, row.before, row.after, last = null) }
            preview.restLine?.let {
                CivilightText(it, type.small, Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 10.dp), color = colors.ink3)
            }
            preview.fixedPointRow?.split(" | ")?.let { parts ->
                CascadeLine(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, null, last = parts.getOrNull(2))
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            RadioRow(
                title = WhereCopy.WITH_FOLLOWING,
                selected = mode == CascadeMode.WITH_FOLLOWING,
                onClick = { mode = CascadeMode.WITH_FOLLOWING },
                subtitle = preview.withFollowingLine.removePrefix("${WhereCopy.WITH_FOLLOWING} · "),
            )
            RadioRow(
                title = WhereCopy.THIS_ONLY,
                selected = mode == CascadeMode.THIS_ONLY,
                onClick = { mode = CascadeMode.THIS_ONLY },
                subtitle = preview.thisOnlyLine.removePrefix("${WhereCopy.THIS_ONLY} · "),
            )
        }
    }
    SheetActions {
        CivilightButton(WhereCopy.CANCEL, onCancel, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            WhereCopy.APPLY,
            { onApply(mode) },
            Modifier.weight(2f),
            subText = if (readOnly) StoryCopy.READ_ONLY else null,
            enabled = !readOnly,
        )
    }
}

/** `1-7 씬 2 | 약 12일째 저녁 | → | 약 14일째 저녁`, or the fixed point `1-9 씬 2 | 직접 · 그대로 | 고정점`. */
@Composable
private fun CascadeLine(scene: String, before: String, after: String?, last: String?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 40.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CivilightText(scene, type.small, Modifier.width(72.dp), color = colors.ink3, maxLines = 1)
        CivilightText(before, type.small, Modifier.weight(1f), color = colors.ink2, maxLines = 1)
        if (after != null) {
            CivilightText(WhereCopy.ARROW, type.small, color = colors.ink3, maxLines = 1)
            CivilightText(after, type.small, Modifier.weight(1f), color = colors.ink, maxLines = 1)
        }
        last?.let { CivilightText(it, type.small, color = colors.acc, maxLines = 1) }
    }
}
