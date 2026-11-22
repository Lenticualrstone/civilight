package io.github.lenticualrstone.civilight.feature.recorder.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip

/**
 * The link row of the capture screens and the memo (LRC-21, boards `LrRecord` · `LrProcess` ·
 * `LrText`): one chip per link in creation order (the first reads `연결 · …`), then `+ 연결`. Chips
 * 32 high, 6 apart, wrapping. A chip with × removes its link(s) at once ([onRemove]); tapping a
 * removable chip replaces its target ([onReplace], LRC-20). An `@` chip (`세라 @`) has no × and does
 * not open the picker: it leaves with its text (rule 23 §5). A purged target is plain text.
 */
@Composable
fun LinkChipRow(
    chips: List<Chip>,
    onAdd: () -> Unit,
    onReplace: (Chip) -> Unit,
    onRemove: (Chip) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipFlow(modifier) {
        for (chip in chips) {
            if (chip.plain) {
                Box(Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.Center) {
                    CivilightText(chip.text, CivilightTheme.type.chip, color = CivilightTheme.colors.ink3, maxLines = 1)
                }
            } else {
                LinkChip(
                    chip = chip,
                    onClick = if (chip.removable) ({ onReplace(chip) }) else null,
                    onRemove = if (chip.removable) ({ onRemove(chip) }) else null,
                )
            }
        }
        AddLinkChip(RecorderCopy.ADD_LINK, onAdd)
    }
}

/** One link chip: link icon 14, the label in `ink`, ` ◇` for a deleted card, the small `@` mark, ×; dotted while the target is in the trash. */
@Composable
fun LinkChip(chip: Chip, onClick: (() -> Unit)?, onRemove: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape) else Modifier)
            .heightIn(min = 32.dp)
            .insetOutline(colors.edge, 1.dp, shape, dashed = chip.dotted)
            .padding(start = 12.dp, end = if (onRemove != null) 2.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightIcon(IconId.Link, contentDescription = null, size = 14.dp, tint = colors.ink2)
        val label = buildString {
            if (chip.prefixed) append(RecorderCopy.LINK_PREFIX)
            append(chip.label)
            if (chip.diamond) append(' ').append(RecorderCopy.DIAMOND)
        }
        CivilightText(label, CivilightTheme.type.chip, color = colors.ink, maxLines = 1)
        if (chip.atForm) {
            CivilightText(RecorderCopy.AT_MARK, CivilightTheme.type.number.copy(fontSize = 11.sp, lineHeight = 14.sp), color = colors.ink3, maxLines = 1)
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .pressable(onClick = onRemove, hitTarget = HitTarget.SMALL)
                    .heightIn(min = 28.dp)
                    .padding(horizontal = 6.dp)
                    .semantics { contentDescription = RecorderCopy.REMOVE_CHIP },
                contentAlignment = Alignment.Center,
            ) {
                CivilightIcon(IconId.X, contentDescription = null, size = 14.dp, tint = colors.ink2)
            }
        }
    }
}

/** `+ 연결` as drawn: the chip shape with the text only (the `+` is part of the copy). */
@Composable
fun AddLinkChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape)
            .heightIn(min = 32.dp)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(label, CivilightTheme.type.chip, color = colors.ink, maxLines = 1)
    }
}
