package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.AvatarSize
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.ProgressBar
import io.github.lenticualrstone.civilight.designsystem.component.RowDivider
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.field.FieldRow
import io.github.lenticualrstone.civilight.domain.cast.relation.DeletedPartnerRow
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationBlock
import io.github.lenticualrstone.civilight.domain.cast.relation.StepStatus
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLineRow
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLineState
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.docText
import androidx.compose.ui.text.AnnotatedString

/** What the record's rows open (sheets of the record screen). */
class RecordActions(
    val editable: Boolean,
    val onField: (defKey: Key) -> Unit,
    val onFieldHistory: (defKey: Key) -> Unit,
    val onFixed: (FixedField) -> Unit,
    val onDisplayNameHistory: () -> Unit,
    val onToggleEmpty: () -> Unit,
    val onAddField: () -> Unit,
    val onMap: () -> Unit,
    val onAddRelation: () -> Unit,
)

/** A field row of the header table or the `항목` list with its planned row. */
@Composable
fun FieldRowItem(doc: ProjectDoc, row: FieldRow, actions: RecordActions) {
    val def = row.defKey?.let(doc.index::fieldDef)
    val numeric = def == null || def.type.known == FieldType.NUMBER || def.type.known == FieldType.CALC
    val key = row.defKey
    RecordFieldRow(
        label = row.label,
        value = row.value,
        since = row.since,
        hasHistory = row.hasHistory,
        numeric = numeric,
        mark = row.characterOnly,
        onClick = key?.let { { actions.onField(it) } },
        onHistory = key?.let { { actions.onFieldHistory(it) } },
    )
    row.planned?.let { (point, value) -> PlannedRow(point, value) }
}

/** `개요` (CST-08): fixed rows and the `항목` list, `빈 항목 N · 보기 | + 항목`, then the `긴 글` sections. */
@Composable
fun OverviewTab(doc: ProjectDoc, state: ProfileState, showEmpty: Boolean, actions: RecordActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        for (row in state.fixedRows) {
            RecordFieldRow(
                label = row.label,
                value = row.value,
                since = row.since,
                hasHistory = row.hasHistory,
                onClick = { actions.onFixed(row.field) },
                onHistory = if (row.field == FixedField.DISPLAY_NAME) actions.onDisplayNameHistory else null,
            )
            row.planned?.let { (point, value) -> PlannedRow(point, value) }
        }
        for (row in state.overview.items) FieldRowItem(doc, row, actions)
        for (row in state.emptyLongRows) FieldRowItem(doc, row, actions)
        Row(Modifier.fillMaxWidth().topHairline(colors.line), verticalAlignment = Alignment.CenterVertically) {
            TextAction(state.emptyLabel, actions.onToggleEmpty, color = if (showEmpty) colors.acc else colors.ink3, style = type.small, modifier = Modifier.padding(start = 0.dp))
            Spacer(Modifier.weight(1f))
            if (actions.editable) TextAction(state.overview.addLabel, actions.onAddField, color = colors.ink2, style = type.label)
        }
        for (section in state.overview.sections) {
            SectionLabel("${section.numeral} ${section.title}", Modifier.padding(top = 22.dp))
            CivilightText(
                section.body.docText,
                type.body,
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .pressable(onClick = { actions.onField(section.defKey) }, scaleOnPress = false, onClickLabel = section.title),
                color = colors.ink,
            )
        }
    }
}

/** `관계` (CST-28; `Relations`): one block per partner from the viewer's side, dotted and hidden rows, `+ 관계`. */
@Composable
fun RelationsTab(state: ProfileState, actions: RecordActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val tab = state.relations
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        SectionLabel(tab.header, trailing = tab.mapLink, onTrailing = actions.onMap, trailingAccent = false)
        Spacer(Modifier.padding(top = 6.dp))
        for (block in tab.blocks) RelationBlockView(block)
        for (row in tab.deletedRows) DeletedPartnerView(row)
        tab.hiddenRow?.let { text ->
            ListRow(text, compact = true, divider = RowDivider.DASHED)
        }
        if (actions.editable) {
            Row(Modifier.fillMaxWidth().topHairline(colors.line)) {
                TextAction(tab.addLabel, actions.onAddRelation, color = colors.ink2, style = type.label)
            }
        }
    }
}

@Composable
private fun RelationBlockView(block: RelationBlock) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(block.partnerName, size = AvatarSize.List)
            Column(Modifier.weight(1f)) {
                CivilightText(block.title, type.itemTitle, color = colors.ink, maxLines = 1)
                CivilightText(block.sub, type.small, color = colors.ink3, maxLines = 2)
            }
            if (block.hasRecords) CivilightText(io.github.lenticualrstone.civilight.domain.cast.CastCopy.CHANGE_MARK, type.label, color = colors.acc)
        }
        if (block.steps.isNotEmpty()) {
            Column(Modifier.padding(start = 48.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val stepStyle = type.small.merge(TextStyle(fontSize = 13.sp, lineHeight = 19.sp))
                for (step in block.steps) {
                    val ink = if (step.status == StepStatus.IN_EFFECT) colors.ink else colors.ink3
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CivilightText(step.point, type.number.merge(TextStyle(fontSize = 11.sp)), Modifier.width(36.dp), color = ink, maxLines = 1)
                        val body = step.text.removePrefix("${step.point} · ").removePrefix(step.point)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CivilightText(body, stepStyle, Modifier.weight(1f, fill = false), color = ink)
                            step.statusLabel?.let { CivilightText(CastUiCopy.stepStatus(it), stepStyle, color = colors.acc, maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}

/** Dotted row of a partner in the trash: `노아 ◇ 지운 인물 · 관계 1` (CST-41; its fix buttons come with CST-43). */
@Composable
private fun DeletedPartnerView(row: DeletedPartnerRow) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line, dashed = true)
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DecoratedText(
            AnnotatedString(row.name),
            type.itemTitleCompact,
            color = colors.ink3,
            decorations = listOf(DecorationRange(0, row.name.length, InlineDecoration.DELETED_REF)),
            maxLines = 1,
        )
        CivilightText(row.label, type.small, color = colors.ink3, maxLines = 1)
    }
}

/** A non-interactive tag (`Tag` style): 30 tall, radius 15, `edge` outline. */
@Composable
private fun TagLabel(text: String) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(15.dp)
    CivilightText(
        text,
        CivilightTheme.type.chip,
        Modifier
            .heightIn(min = 30.dp)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = colors.ink2,
        maxLines = 1,
    )
}

/** `말투` (read-only until CST-15): the prose, then the 호칭 chips read from the relations. */
@Composable
fun SpeechTab(body: String, chips: List<String>, hiddenRow: String?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(CastUiCopy.SPEECH)
        if (body.isNotBlank()) CivilightText(body, type.body, color = colors.ink)
        if (chips.isNotEmpty()) ChipFlow { chips.forEach { TagLabel(it) } }
        hiddenRow?.let { CivilightText(it, type.small, color = colors.ink3) }
    }
}

/** `게임 텍스트` (read-only until CST-16, CST-17): voice lines with progress, then archive documents. */
@Composable
fun GameTextTab(progressTitle: String, progress: Float, orderLabel: String, lines: List<VoiceLineRow>, archiveTitle: String, archive: List<Pair<String, String>>) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        SectionLabel(progressTitle, trailing = orderLabel, trailingAccent = false)
        ProgressBar(progress, Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp))
        for (line in lines) {
            Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CivilightText(line.number, type.number.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3)
                    CivilightText(line.title, type.label, Modifier.weight(1f), color = colors.ink, maxLines = 1)
                    val stateColor = when (line.state) {
                        VoiceLineState.DONE -> colors.ink2
                        VoiceLineState.WRITING -> colors.acc
                        VoiceLineState.EMPTY -> colors.ink3
                    }
                    CivilightText(line.state.label, type.small, color = stateColor, maxLines = 1)
                }
                CivilightText(line.text, type.body, Modifier.padding(top = 4.dp), color = if (line.state == VoiceLineState.EMPTY) colors.ink3 else colors.ink)
            }
        }
        SectionLabel(archiveTitle, Modifier.padding(top = 22.dp, bottom = 6.dp))
        for ((title, sub) in archive) ListRow(title, subtitle = sub, compact = true)
    }
}

/** `비밀` (CST-18 fields shown here only; the prose is read-only until CST-18). */
@Composable
fun SecretTab(doc: ProjectDoc, rows: List<FieldRow>, body: String, actions: RecordActions) {
    val colors = CivilightTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        for (row in rows) FieldRowItem(doc, row, actions)
        if (body.isNotBlank()) CivilightText(body, CivilightTheme.type.body, Modifier.padding(top = 18.dp), color = colors.ink)
    }
}
