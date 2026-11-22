package io.github.lenticualrstone.civilight.feature.script.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy

/**
 * `모듈` (SCR-35, boards `CSheetMods`, `WideMods`): `tint` cards (radius 14, 12 / 14 padding, 6 gap)
 * in the stored order, then the footer `카드 순서 · 추가는 ⋯`.
 */
@Composable
fun ModsTabView(model: ModsTabModel, actions: PanelActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        model.cards.forEach { card ->
            when (card) {
                is ModCard.Today -> ModCardBox(ScriptCopy.MOD_TODAY) {
                    CivilightText(card.written, type.number.merge(TextStyle(fontSize = 18.sp)), color = colors.ink)
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.line)) {
                        Box(Modifier.fillMaxWidth(card.progress).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.acc))
                    }
                    CivilightText(card.goal, type.small, color = colors.ink3)
                }
                is ModCard.SceneCount -> ModCardBox(ScriptCopy.MOD_SCENE) {
                    CivilightText(card.text.ifEmpty { ScriptCopy.MODS_SCENE_EMPTY }, type.command.merge(TextStyle(fontSize = 13.sp, lineHeight = 20.sp)), color = colors.ink)
                }
                is ModCard.Checks -> ModCardBox(
                    ScriptCopy.MOD_CHECKS,
                    Modifier.pressable(PressAction.Click(actions.openChecks), role = Role.Button, scaleOnPress = false),
                ) {
                    val text = card.text
                    CivilightText(
                        text ?: ScriptCopy.MODS_NO_CHECKS,
                        type.command.merge(TextStyle(fontSize = 13.sp, lineHeight = 20.sp, fontWeight = if (text != null) FontWeight.SemiBold else FontWeight.Normal)),
                        color = if (text != null) colors.warn else colors.ink3,
                    )
                }
                is ModCard.Memo -> MemoCard(card, actions)
            }
        }
        CivilightText(ScriptCopy.MODS_FOOTER, type.small.merge(TextStyle(textAlign = TextAlign.Center)), Modifier.fillMaxWidth(), color = colors.ink3)
    }
}

@Composable
private fun ModCardBox(
    label: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.tint)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(label, CivilightTheme.type.sec, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            trailing?.invoke()
        }
        content()
    }
}

/**
 * `씬 메모` (serif 14 / 22), typed in place (SCR-35 `[PROPOSED]`). The field holds the text while it
 * is edited (never re-seeded from the store while the scene stays the same), and each change is
 * written after a pause. `레코더 · 정리 안 됨 n ›` sits at the right (LRC-24).
 */
@Composable
private fun MemoCard(card: ModCard.Memo, actions: PanelActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    ModCardBox(
        ScriptCopy.MOD_MEMO,
        trailing = card.recorderLine?.let { line ->
            {
                CivilightText(
                    line, type.small,
                    Modifier.pressable(PressAction.Click({ actions.openRecorderInbox(card.sceneKey) }), role = Role.Button, hitTarget = HitTarget.SMALL, scaleOnPress = false),
                    color = colors.acc, maxLines = 1,
                )
            }
        },
    ) {
        val style = type.line.merge(TextStyle(fontSize = 14.sp, lineHeight = 22.sp, color = colors.ink))
        if (!card.editable) {
            if (card.text.isNotEmpty()) CivilightText(card.text, style, color = colors.ink)
            return@ModCardBox
        }
        var text by rememberSaveable(card.sceneKey) { mutableStateOf(card.text) }
        var focused by remember { mutableStateOf(false) }
        // While the field is not being typed into, it follows the store (undo, a merge, the recorder's
        // `메모 칸에 넣기`), so the next keystroke never writes over a newer memo.
        LaunchedEffect(card.text) { if (!focused) text = card.text }
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                actions.editMemo(card.sceneKey, it)
            },
            textStyle = style,
            cursorBrush = SolidColor(colors.acc),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = ScriptCopy.MOD_MEMO },
        )
    }
}
