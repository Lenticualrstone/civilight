package io.github.lenticualrstone.civilight.feature.script.panel

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetTabsHeader
import io.github.lenticualrstone.civilight.designsystem.component.SidePanel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.motion.ScriptMotion
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/**
 * The inner side panel (SCR-03, SCR-31): DS `SidePanel` (290, glass, left hairline) with the built
 * tabs and `닫기`. System back does not close it (the screen's own back steps come first); a closed
 * panel comes back through the swipe buttons (SCR-30) or when the script is opened again.
 */
@Composable
fun ScriptSidePanel(
    tab: PanelTab,
    onSelectTab: (PanelTab) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** The tab's scroll, shared with the cover sheet (SCR-66); null = this panel's own. */
    scrollOf: ((PanelTab) -> ScrollState)? = null,
    content: @Composable ColumnScope.(PanelTab) -> Unit,
) {
    val tabs = PanelTab.BUILT
    val shown = tab.takeIf { it in tabs } ?: tabs.first()
    val reduced = CivilightTheme.reducedMotion
    CompositionLocalProvider(LocalBackHandler provides { _, _ -> }) {
        SidePanel(
            onClose = onClose,
            modifier = modifier,
            tabs = tabs.map { it.label },
            selectedTab = tabs.indexOf(shown),
            onSelectTab = { onSelectTab(tabs[it]) },
            closeLabel = ScriptCopy.CLOSE,
        ) {
            // SCR-67: a tab change crossfades over 200 ms (reduced motion 120 ms).
            Crossfade(shown, animationSpec = tween(ScriptMotion.tabMs(reduced)), label = "panelTab") { t ->
                val scroll = scrollOf?.invoke(t) ?: rememberScrollState()
                Column(
                    Modifier.fillMaxWidth().verticalScroll(scroll).padding(top = 18.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) { content(t) }
            }
        }
    }
}

/** The cover panel sheet (SCR-31): tabs `시간 · 참고 · 속성` and `닫기`, over the manuscript. */
@Serializable
data class PanelSheetKey(val projectKey: Key) : SheetKey

/**
 * The cover panel sheet's content (boards `CSheetTime`, `CSheetRef`, `CSheetProps`): the same tab
 * bodies as the side panel; switching tabs keeps the sheet and the selected block.
 */
@Composable
fun SheetScope.PanelSheet(
    tab: PanelTab,
    onSelectTab: (PanelTab) -> Unit,
    /** The tab's scroll, shared with the side panel (SCR-66); null = this sheet's own. */
    scrollOf: ((PanelTab) -> ScrollState)? = null,
    content: @Composable ColumnScope.(PanelTab) -> Unit,
) {
    val tabs = PanelTab.BUILT
    val shown = tab.takeIf { it in tabs } ?: tabs.first()
    val reduced = CivilightTheme.reducedMotion
    SheetTabsHeader(
        tabs = tabs.map { it.label },
        selectedIndex = tabs.indexOf(shown),
        onSelect = { onSelectTab(tabs[it]) },
        onClose = ::close,
        closeLabel = ScriptCopy.CLOSE,
    )
    Crossfade(shown, Modifier.weight(1f, fill = false), animationSpec = tween(ScriptMotion.tabMs(reduced)), label = "panelTab") { t ->
        val scroll = scrollOf?.invoke(t) ?: rememberScrollState()
        Column(
            Modifier.verticalScroll(scroll).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content(t) }
    }
}

/**
 * `속성` (SCR-34, boards `CSheetProps`, `WideProps`): `고른 블록 · #043`, then the fields that apply
 * to the block: `종류` (SCR-13), `화자` (or the deleted speaker with `화자 바꾸기 · 되살리기`, SCR-57),
 * `표시 이름` with its reveal note, `무대 위치` (SCR-25), `보이스 ID` (SCR-23), `변화 연결` (tap →
 * `시간`), and a recorder memo's `출처`. Without a selected block: `고른 블록 없음`.
 */
@Composable
fun PropsTab(model: PropsModel, actions: PanelActions, wide: Boolean) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val block = model.blockKey
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // The cover sheet (`CSheetProps`) starts with the fields; the inner panel (`WideProps`) names the block.
        if (wide || block == null) CivilightText(model.title, type.sec, color = colors.ink3, maxLines = 1)
        if (block == null) return@Column
        model.kind?.let { PropFieldView(it) { actions.changeKind(block) } }
        model.speaker?.let { PropFieldView(it) { actions.changeSpeaker(block) } }
        model.deletedSpeaker?.let { deleted ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TextAction(ScriptCopy.CHANGE_SPEAKER, { actions.changeSpeaker(block) }, color = colors.ink)
                if (deleted.canRestore) TextAction(ScriptCopy.RESTORE, { actions.restoreSpeaker(deleted.characterKey) })
                if (deleted.canKeep) TextAction(ScriptCopy.KEEP, { actions.keepSpeaker(deleted.characterKey, block) })
            }
        }
        model.displayName?.let { field ->
            // SCR-34 `[PROPOSED]`: the 표시 이름 history lives in the character's record.
            val owner = model.displayNameOwner
            val tap = owner?.let { Modifier.pressable(PressAction.Click({ actions.editRecord(RecordEdit.Profile(it)) }), role = Role.Button, scaleOnPress = false) } ?: Modifier
            Box(tap) { PropFieldView(if (wide && field.noteWide != null) field.copy(note = field.noteWide) else field) {} }
        }
        model.stage?.let { stage ->
            FieldLabel(ScriptCopy.STAGE_FIELD) {
                val options = StageField.OPTIONS
                SegmentedControl(
                    options = options.map { it.second },
                    selectedIndex = options.indexOfFirst { it.first == stage.selected },
                    onSelect = { i -> if (stage.enabled) actions.setStage(block, options[i].first) },
                )
            }
        }
        model.voice?.let { voice ->
            if (voice.editable) ManualVoiceField(block, voice.id.orEmpty()) { actions.setManualVoice(block, it) }
            else PropFieldView(PropField(ScriptCopy.VOICE_FIELD, voice.id.orEmpty(), note = voice.state, mono = true)) {}
        }
        model.optionVoices.forEach { option ->
            PropFieldView(PropField(ScriptCopy.optionVoiceLabel(option.number), option.field.id.orEmpty(), note = option.field.state, mono = true)) {}
        }
        if (model.changes.isNotEmpty()) {
            FieldLabel(ScriptCopy.CHANGES_FIELD) {
                model.changes.forEach { line ->
                    CivilightText(
                        line, type.command.merge(TextStyle(fontSize = 13.sp)),
                        Modifier.pressable(PressAction.Click({ actions.showTab(PanelTab.TIME) }), role = Role.Button, hitTarget = HitTarget.SMALL, scaleOnPress = false),
                        color = colors.acc,
                    )
                }
            }
        }
        model.source?.let { field ->
            // SCR-59: tapping the source opens the record in the recorder (audio plays there).
            val record = model.sourceRecordKey
            val tap = record?.let { Modifier.pressable(PressAction.Click({ actions.openRecord(it) }), role = Role.Button, scaleOnPress = false) } ?: Modifier
            Box(tap) { PropFieldView(field) {} }
        }
        // SCR-36: the visible way into `블록 고르기` (the spec draws no entry, Q-17).
        if (!model.readOnly) actions.selectMode?.let { enter ->
            TextAction(io.github.lenticualrstone.civilight.feature.script.select.MoveCopy.TITLE, { enter(block) }, color = colors.ink)
        }
    }
}

@Composable
private fun FieldLabel(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CivilightText(label, CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
        content()
    }
}

/** Label 12 `ink3` above a 44–46 dp field box (`tint`, `edge` outline); a right note and `바꾸기`. */
@Composable
private fun PropFieldView(field: PropField, onChange: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(12.dp)
    FieldLabel(field.label) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .insetOutline(if (field.dotted) colors.ink3 else colors.edge, 1.dp, shape, dotted = field.dotted)
                .padding(start = 12.dp, end = if (field.canChange) 2.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            field.avatar?.let { Avatar(it, size = 26.dp) }
            val style = if (field.mono) type.number.merge(TextStyle(fontSize = 13.sp)) else type.command
            CivilightText(field.value, style, Modifier.weight(1f), color = if (field.dotted) colors.ink2 else colors.ink, maxLines = 1)
            field.note?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
            if (field.canChange) TextAction(ScriptCopy.CHANGE, onChange)
        }
    }
}

/** `보이스 ID` with the format `직접` (SCR-23): typed IDs are written when the field is left or done. */
@Composable
private fun ManualVoiceField(blockKey: Key, stored: String, onCommit: (String) -> Unit) {
    var text by rememberSaveable(blockKey, stored) { mutableStateOf(stored) }
    CivilightTextField(
        value = text,
        onValueChange = { text = it },
        label = ScriptCopy.VOICE_FIELD,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (text != stored) onCommit(text) }),
        textStyle = CivilightTheme.type.number.merge(TextStyle(fontSize = 13.sp)),
        modifier = Modifier.onFocusChanged { if (!it.hasFocus && text != stored) onCommit(text) },
    )
}
