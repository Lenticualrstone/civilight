package io.github.lenticualrstone.civilight.feature.recorder.inbox

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.feature.recorder.audio.PlayerRow
import io.github.lenticualrstone.civilight.feature.recorder.link.PresenceItems
import io.github.lenticualrstone.civilight.feature.recorder.link.chipKey

/**
 * One inbox row (LRC-25, board `LrInbox`): header `음성 0:24` · `오늘 08:41` · state in `acc`; the
 * text (manuscript 16/27, 3 lines); the link tags after a link icon (labels only, LRC-21); the
 * actions `원고에 넣기` · `카드에 넣기` · `페이지로 만들기`, `+ 연결` on a row without links, and the
 * visible `⋯` button (the menu is never behind a hidden gesture). A tap on the text opens the record.
 * A voice record with kept audio has the player row under its text (LRC-14). [selected] marks the
 * record open in the right pane (LRC-31). [textModifier] draws on the text paragraph only (the
 * hidden rows' reveal, LRC-41).
 */
@Composable
fun InboxRowItem(view: RowView, actions: InboxActions, modifier: Modifier = Modifier, selected: Boolean = false, textModifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val row = view.row
    val head = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold).let { type.label.merge(it) }
    val motion = CivilightTheme.motion
    Column(
        modifier
            .fillMaxWidth()
            // A row's content changes (links, state, actions) move the rows below on `standard`.
            .animateContentSize(motion.move())
            .topHairline(colors.line)
            // The record shown in the right pane (LRC-31 `목록 + 상세`).
            .selectedRowBackground(selected, colors.sel, colors.acc)
            .padding(vertical = 14.dp),
    ) {
        // Header and text together open the record (a voice record without text still has its header).
        Column(Modifier.fillMaxWidth().pressable(PressAction.Click(onClick = { actions.openRecord(row.recordKey) }), scaleOnPress = false)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightText(row.kindLabel, head, color = colors.ink, maxLines = 1)
            CivilightText(row.meta, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            val unhide = row.unhideAction
            if (unhide != null) {
                FilterChip(unhide, selected = false, onClick = { actions.unhide(row.recordKey) })
            } else {
                CivilightText(row.stateLabel, head, color = colors.acc, maxLines = 1)
            }
        }
        if (row.text.isNotEmpty()) {
            CivilightText(
                row.text,
                type.line.merge(TextStyle(fontSize = 16.sp, lineHeight = 27.sp)),
                Modifier.padding(top = 6.dp).fillMaxWidth().then(textModifier),
                color = colors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
        }
        view.audio?.let { audio -> PlayerRow(row.recordKey, audio, actions.player, Modifier.padding(top = 8.dp)) }
        FoldRevealValue(row.linkTags.takeIf { it.isNotEmpty() }) { tags ->
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CivilightIcon(IconId.Link, contentDescription = null, size = 14.dp, tint = colors.ink2)
                ChipFlow { PresenceItems(tags, key = ::chipKey) { tag -> LinkTag(tag) } }
            }
        }
        ChipFlow(Modifier.padding(top = 10.dp)) {
            for (label in row.actions) {
                when (label) {
                    RecorderCopy.PUT_MANUSCRIPT -> ActionTag(label, view.actions.putManuscript) { actions.put(row.recordKey, PutKind.MANUSCRIPT) }
                    RecorderCopy.PUT_CARD -> ActionTag(label, view.actions.putCard) { actions.put(row.recordKey, PutKind.CARD) }
                    RecorderCopy.MAKE_PAGE -> ActionTag(label, view.actions.makePage) { actions.put(row.recordKey, PutKind.PAGE) }
                    RecorderCopy.ADD_LINK -> ActionTag(label, true) { actions.addLink(row.recordKey) }
                    else -> ActionTag(label, false) {}
                }
            }
            CivilightIconButton(IconId.More, ShownCopy.MENU, onClick = { actions.menu(row.recordKey) }, iconSize = 18.dp)
        }
    }
}

/** An action tag; a disabled one is drawn at the DS disabled 38 % (the chip itself does not dim). */
@Composable
internal fun ActionTag(label: String, enabled: Boolean, onClick: () -> Unit) {
    // The disabled look comes and goes on `standard` (a put becomes available, a record gets text).
    val alpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "actionTag")
    FilterChip(label, selected = false, onClick = onClick, modifier = Modifier.graphicsLayer { this.alpha = alpha.value }, enabled = enabled)
}

/** DS disabled opacity (DS-30). */
const val DISABLED_ALPHA = 0.38f

/**
 * A link tag of an inbox row (`tag on`: `pri` fill, `onpri` text, labels only). A tag of a target
 * in the trash is drawn without fill and with the dotted outline, a deleted card adds ` ◇`.
 */
@Composable
private fun LinkTag(chip: Chip) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(15.dp)
    val label = if (chip.diamond) "${chip.label} ${RecorderCopy.DIAMOND}" else chip.label
    if (chip.plain || chip.dotted) {
        Box(
            Modifier
                .heightIn(min = 30.dp)
                .then(if (chip.dotted) Modifier.insetOutline(colors.edge, 1.dp, shape, dashed = true) else Modifier)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            CivilightText(label, CivilightTheme.type.chip, color = if (chip.plain) colors.ink3 else colors.ink2, maxLines = 1)
        }
    } else {
        Box(
            Modifier.heightIn(min = 30.dp).clip(shape).background(colors.pri).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            CivilightText(label, CivilightTheme.type.chip, color = colors.onpri, maxLines = 1)
        }
    }
}
