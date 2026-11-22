package io.github.lenticualrstone.civilight.feature.story.where

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.RemovableChip
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.TransitionMode

/**
 * `장소 · 지역으로 바꾸기`, the place value (serif; tap = the list picker), `지도에서 고르기 ›`. A deleted
 * place reads `지운 장소 · …` in `ink3`, dotted (R20 §2), with `장소 고르기 · 되살리기 · 이대로 두기`; a
 * hidden place while locked reads `숨긴 항목` (R19 §6).
 */
@Composable
fun WherePlaceSection(ui: WhereUi, onPickList: (regions: Boolean) -> Unit, onPickMap: () -> Unit, onRestore: () -> Unit, onKeep: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val enabled = !ui.readOnly
    val state = ui.placeState
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CivilightText("${WhereCopy.FIELD_PLACE} · ", type.small, color = colors.ink3)
            if (enabled) TextAction(ui.sheet.placeToggle, { onPickList(!ui.isRegion) }, color = colors.acc)
        }
        val name = when {
            state.masked -> WhereCopy.HIDDEN_ITEM
            else -> ui.sheet.place ?: WhereCopy.NO_PLACE
        }
        val dotted = ui.sheet.placeDeleted && !state.masked
        val color = if (ui.sheet.place == null || dotted || state.masked) colors.ink3 else colors.ink
        val nameModifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = { onPickList(ui.isRegion) }, enabled = enabled, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(vertical = 8.dp)
        if (dotted) DottedText(name, type.sceneHead, nameModifier, color) else CivilightText(name, type.sceneHead, nameModifier, color = color, maxLines = 1)
        ChipFlow {
            // The deleted-place actions fold in and out when the place goes or comes back (P2).
            FoldReveal(dotted && enabled, horizontal = true) { TextAction(WhereCopy.PICK_PLACE, { onPickList(false) }) }
            FoldReveal(dotted && enabled && state.restorable, horizontal = true) { TextAction(WhereCopy.RESTORE, onRestore, color = colors.acc) }
            FoldReveal(dotted && enabled && state.keepable, horizontal = true) { TextAction(WhereCopy.KEEP, onKeep) }
            if (enabled) TextAction(WhereCopy.PICK_ON_MAP, onPickMap)
        }
    }
}

/** `회상 | 연쇄 · 이동 점검에서 빠짐` (`RTFlashback`); the form below it is [WhereFlashbackForm] (STO-24). */
@Composable
fun WhereFlashbackRow(ui: WhereUi, onChange: (Boolean) -> Unit) {
    SwitchRow(
        title = WhereCopy.FLASHBACK,
        checked = ui.sheet.flashback,
        onCheckedChange = onChange,
        subtitle = WhereCopy.FLASHBACK_SUB,
        enabled = !ui.readOnly,
    )
}

/**
 * `등장 | 리엔 · 하르트 | (대사) | + 세라` (STO-26): the speakers follow the blocks and are not edited
 * here; silent members are chips with `빼기`; `+` opens the speaker picker. A locked hidden member reads
 * `숨긴 인물`; a deleted one is dotted, and a deleted silent one gets `빼기 · 되살리기 · 이대로 두기`
 * (R20 §2, STO-11).
 */
@Composable
fun WhereCastRow(ui: WhereUi, onAdd: () -> Unit, onRemove: (Key) -> Unit, onRestore: (Key) -> Unit, onKeep: (Key) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val enabled = !ui.readOnly
    val row = ui.cast
    // No `spacedBy`: a folded-away block would still add its gap, so each folding block carries its own 6 dp.
    Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightText(WhereCopy.CAST, type.small, color = colors.ink3, maxLines = 1)
            if (row.speakers.isNotEmpty()) {
                Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                    row.speakers.forEachIndexed { i, m ->
                        if (i > 0) CivilightText(" · ", type.command, color = colors.ink3, maxLines = 1)
                        MemberName(m, type.command)
                    }
                }
                CivilightText(WhereCopy.SPOKEN, type.small, color = colors.ink3, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            if (enabled) CivilightIconButton(IconId.Plus, WhereCopy.CAST, onAdd)
        }
        // The silent members and the deleted ones fold in and out as a block, keeping the last list while they leave.
        FoldRevealValue(row.silent.filter { !it.dotted }.takeIf { it.isNotEmpty() }) { live ->
            ChipFlow(Modifier.padding(top = 6.dp)) {
                live.forEach { m ->
                    if (enabled && m.look == MemberLook.LIVE) {
                        RemovableChip(label = m.name, onRemove = { onRemove(m.key) }, removeLabel = "${m.name} ${WhereCopy.REMOVE}")
                    } else {
                        FilterChip(m.name, false, {}, enabled = false)
                    }
                }
            }
        }
        FoldRevealValue(row.silent.filter { it.dotted }.takeIf { it.isNotEmpty() }) { gone ->
            Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                gone.forEach { m ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CivilightText(WhereCopy.DELETED_MARK, type.small, color = colors.ink3)
                            MemberName(m, type.command)
                        }
                        if (enabled) {
                            ChipFlow {
                                TextAction(WhereCopy.REMOVE, { onRemove(m.key) })
                                if (m.look == MemberLook.DELETED) {
                                    TextAction(WhereCopy.RESTORE, { onRestore(m.key) }, color = colors.acc)
                                    TextAction(WhereCopy.KEEP, { onKeep(m.key) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A member name: dotted in `ink3` when deleted, `ink3` for `숨긴 인물`, else `ink`. */
@Composable
private fun MemberName(m: CastMember, style: TextStyle) {
    val colors = CivilightTheme.colors
    when {
        m.dotted -> DottedText(m.name, style, Modifier, colors.ink3)
        else -> CivilightText(m.name, style, color = if (m.look == MemberLook.HIDDEN) colors.ink3 else colors.ink, maxLines = 1)
    }
}

/** One line with the deleted-reference dotted underline (DS-18). */
@Composable
fun DottedText(text: String, style: TextStyle, modifier: Modifier, color: Color) {
    DecoratedText(
        AnnotatedString(text),
        style,
        modifier,
        color = color,
        decorations = listOf(DecorationRange(0, text.length, InlineDecoration.DELETED_REF)),
        maxLines = 1,
    )
}

/**
 * `전환 문구 | 자동 · 직접 · 없음` (STO-27 row; SCR-48 owns storage). `자동` needs a place; `직접`
 * reveals the text field, saved as the author types (one undo step per typing run).
 */
@Composable
fun WhereTransitionSection(ui: WhereUi, onMode: (TransitionMode) -> Unit, onDirectText: (String) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val row = ui.sheet.transition
    val enabled = !ui.readOnly
    // No `spacedBy`: the swap below always emits a node (empty when there is no preview line), which
    // would keep a gap. The chips and the swap carry their own 6 dp, the swap only when it has content.
    Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 10.dp)) {
        CivilightText(WhereCopy.TRANSITION, type.small, color = colors.ink3, maxLines = 1)
        ChipFlow(Modifier.padding(top = 6.dp)) {
            FilterChip(WhereCopy.MODE_AUTO, row.mode == TransitionMode.AUTO, { onMode(TransitionMode.AUTO) }, enabled = enabled && row.autoEnabled)
            FilterChip(WhereCopy.MODE_DIRECT, row.mode == TransitionMode.DIRECT, { onMode(TransitionMode.DIRECT) }, enabled = enabled)
            FilterChip(WhereCopy.MODE_NONE, row.mode == TransitionMode.NONE, { onMode(TransitionMode.NONE) }, enabled = enabled)
        }
        // The field and the preview line swap on the mode kind only, never while typing (P3).
        ContentSwap(row.mode == TransitionMode.DIRECT, Modifier.fillMaxWidth().padding(top = if (row.mode == TransitionMode.DIRECT || row.preview != null) 6.dp else 0.dp)) { direct ->
            if (direct) {
                // Seeded once per scene: the field is the source of truth while typing (Hangul composition).
                var draft by rememberSaveable(ui.sheet.sceneKey) { mutableStateOf(row.directText.orEmpty()) }
                CivilightTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        onDirectText(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                )
            } else {
                row.preview?.let { CivilightText(it, type.small, color = colors.ink2, maxLines = 2) }
            }
        }
    }
}
