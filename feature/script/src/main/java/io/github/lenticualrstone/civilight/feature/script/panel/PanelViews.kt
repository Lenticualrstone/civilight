package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.PanelCard
import io.github.lenticualrstone.civilight.designsystem.component.RowDivider
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy

/** The shown tab of the panel (side panel and cover sheet alike). */
@Composable
fun PanelTabBody(content: PanelContent, wide: Boolean, actions: PanelActions) {
    // Motion audit P4: another selected block crossfades the body (and its height follows); the same
    // block's data refreshes in place, so typing never cross-fades the panel.
    ContentSwap(content, Modifier.fillMaxWidth(), contentKey = { it.tab to it.selected }, label = "panelBody") { c ->
        when (c.tab) {
            PanelTab.TIME -> c.time?.let { TimeTabView(it, wide, actions) }
            PanelTab.REF -> c.ref?.let { RefTabView(it) }
            PanelTab.PROPS -> c.props?.let { PropsTab(it, actions, wide) }
            PanelTab.MODS -> c.mods?.let { ModsTabView(it, actions) }
        }
    }
}

// --- 시간 (SCR-32, boards `CSheetTime`, `Wide`) ---

@Composable
fun TimeTabView(model: TimeTabModel, wide: Boolean, actions: PanelActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val title = if (wide) model.blockTitleWide else model.blockTitle
        if (title != null) {
            SectionLabel(title)
            if (model.cards.isEmpty()) CivilightText(ScriptCopy.NO_CHANGE, type.small, color = colors.ink3)
            model.cards.forEach { RecordCardView(it, wide, model.editable, actions) }
            model.trashNotes.forEach { note -> TrashNoteView(note, model.editable, actions) }
            val block = model.blockKey
            if (model.canRecord && block != null) RecordChangeRow { actions.recordChange(block) }
        }
        model.scene?.let { scene ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText(ScriptCopy.SCENE_TIME_SECTION, type.sec, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
                CivilightText("${scene.transitionState} · ", type.small, color = colors.ink3, maxLines = 1)
                TextAction(ScriptCopy.CHANGE_MORE, { actions.openSceneWhere(scene.sceneKey) }, style = type.small)
            }
            val head = buildAnnotatedString {
                append(scene.headline)
                scene.place?.let { place ->
                    withStyle(SpanStyle(fontSize = type.small.fontSize, fontWeight = FontWeight.Normal, color = colors.ink3)) { append(" · $place") }
                }
            }
            BasicText(head, style = type.itemTitleCompact.merge(TextStyle(color = colors.ink)))
            scene.detail?.let { CivilightText(it, type.small, color = colors.ink3) }
            scene.travel.forEach { line ->
                CivilightText(
                    line, type.small,
                    Modifier.pressable(PressAction.Click(actions.openTravelChecks), role = Role.Button, hitTarget = HitTarget.SMALL, scaleOnPress = false),
                    color = colors.warn,
                )
            }
        }
        if (model.inScene.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            SectionLabel(ScriptCopy.IN_SCENE)
            Column { model.inScene.forEach { InSceneRowView(it, wide, model.editable, actions) } }
        }
    }
}

/** `#043 · 휴지통 · 30일 · 되살리면 다시 이어짐` (board `RSOrphan`) with `되살리기` (SCR-39, `ED`). */
@Composable
private fun TrashNoteView(note: TrashNote, editable: Boolean, actions: PanelActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(note.text, type.small, Modifier.weight(1f), color = colors.ink3)
        if (editable) TextAction(ScriptCopy.RESTORE, { actions.restoreBlock(note.blockKey) }, style = type.small)
    }
}

/** `◆ 변화 기록` (SCR-41 `[PROPOSED]` second entry): opens the change sheet for the selected block. */
@Composable
private fun RecordChangeRow(onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(PressAction.Click(onClick), role = Role.Button, scaleOnPress = false)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(ScriptCopy.RECORD_CHANGE_ROW, CivilightTheme.type.command, color = colors.acc)
    }
}

@Composable
private fun RecordCardView(card: RecordCard, wide: Boolean, editable: Boolean, actions: PanelActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val titleColor = if (card.reveal || card.dotted) colors.ink3 else colors.acc
    val dotted = if (card.dotted) Modifier.insetOutline(colors.ink3, 1.dp, androidx.compose.foundation.shape.RoundedCornerShape(14.dp), dotted = true) else Modifier
    PanelCard(dotted) {
        CivilightText(card.title, type.command.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = titleColor)
        CivilightText(card.line, type.small, color = colors.ink2)
        // SCR-43: `다른 블록에 잇기 · 이대로 두기 · 기록 지우기` while the record is flagged.
        if (card.orphan && editable) {
            // Three buttons wrap inside the 290 dp panel.
            io.github.lenticualrstone.civilight.designsystem.component.ChipFlow(gap = 4.dp) {
                TextAction(ScriptCopy.RELINK, { actions.relink(card.recordKey) }, color = colors.ink)
                TextAction(ScriptCopy.KEEP, { actions.keepRecord(card.recordKey) })
                TextAction(ScriptCopy.TRASH_RECORD, { actions.trashRecord(card.recordKey) }, color = colors.warn)
            }
        }
        // SCR-57: a record about a deleted item (`대상 지워짐`).
        val subject = card.deletedSubject
        if (subject != null && editable) {
            io.github.lenticualrstone.civilight.designsystem.component.ChipFlow(gap = 4.dp) {
                if (subject.canRestore) TextAction(ScriptCopy.RESTORE, { actions.restoreSubject(subject.kind, subject.itemKey) }, color = colors.ink)
                if (!subject.kept) TextAction(ScriptCopy.KEEP, { actions.keepSubject(subject.itemKey, card.recordKey) })
                TextAction(ScriptCopy.TRASH_RECORD, { actions.trashRecord(card.recordKey) }, color = colors.warn)
            }
        }
        // Inner adds `고치기 · 관계도에서 보기` (SCR-32).
        if (wide && !card.dotted && (card.edit != null || card.mapFocus != null)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                card.edit?.let { edit -> TextAction(ScriptCopy.EDIT_RECORD, { actions.editRecord(edit) }, color = colors.ink) }
                card.mapFocus?.let { focus -> TextAction(ScriptCopy.SHOW_IN_MAP, { actions.showInMap(focus) }) }
            }
        }
    }
}

@Composable
private fun InSceneRowView(row: InSceneRow, wide: Boolean, editable: Boolean, actions: PanelActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val tap = row.blockKey?.let { key ->
        Modifier.pressable(PressAction.Click({ actions.selectBlock(key) }), role = Role.Button, scaleOnPress = false)
    } ?: Modifier
    Row(
        tap
            .fillMaxWidth()
            .selectedRowBackground(row.selected, colors.sel, colors.acc)
            .topHairline(colors.line)
            .heightIn(min = 44.dp)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val left = buildAnnotatedString {
            val glyph = row.glyph
            if (glyph != null && row.left.startsWith(glyph)) {
                withStyle(SpanStyle(color = if (glyph == "◆") colors.acc else colors.ink3)) { append(glyph) }
                append(row.left.removePrefix(glyph))
            } else append(row.left)
        }
        BasicText(left, Modifier.weight(1f), style = type.command.merge(TextStyle(color = if (row.selected) colors.ink else colors.ink3)))
        val right = if (wide) row.rightWide else row.right
        if (right.isNotEmpty()) CivilightText(right, type.command, color = colors.ink2, maxLines = 1)
        // SCR-42: a point-seat line converts to a block seat through the candidate list.
        val record = row.recordKey
        if (record != null && editable) TextAction(ScriptCopy.LINK_TO_BLOCK, { actions.relink(record) }, style = type.small)
    }
}

// --- 참고 (SCR-33, boards `CSheetRef`, `WideRef`) ---

@Composable
fun RefTabView(model: RefTabModel) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        model.message?.let {
            CivilightText(it, type.small, color = colors.ink3)
            return@Column
        }
        model.title?.let { SectionLabel(it) }
        Column { model.rows.forEach { RefRowView(it) } }
        if (model.recent.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SectionLabel(ScriptCopy.RECENT_LINES)
            model.recent.forEach { line ->
                val text = buildAnnotatedString {
                    append(line.text)
                    withStyle(SpanStyle(fontSize = type.small.fontSize, color = colors.ink3)) { append("  ${line.location}") }
                }
                BasicText(text, style = type.lead.merge(TextStyle(color = colors.ink)))
            }
        }
        if (model.relations.isNotEmpty() || model.hiddenRelations != null) {
            Spacer(Modifier.height(10.dp))
            SectionLabel(ScriptCopy.RELATIONS_NOW)
            Column {
                model.relations.forEach { RefRowView(it) }
                model.hiddenRelations?.let { RefRowView(RefRow(it, "")) }
            }
        }
    }
}

@Composable
private fun RefRowView(row: RefRow) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    if (row.dotted) {
        // Deleted partners: one row with the dashed divider and dotted title (rule 20, DS-18).
        ListRow(title = row.left, compact = true, divider = RowDivider.DASHED, titleDotted = true)
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .padding(vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            CivilightText(row.left, type.command, Modifier.weight(1f), color = colors.ink3)
            if (row.right.isNotEmpty()) CivilightText(row.right, type.command, color = colors.ink)
        }
        row.sub?.let { CivilightText(it, type.small, Modifier.align(Alignment.End), color = colors.ink3) }
    }
}
