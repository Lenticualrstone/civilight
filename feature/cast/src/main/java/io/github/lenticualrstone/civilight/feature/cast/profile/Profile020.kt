package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.actions.CharacterFieldActions
import io.github.lenticualrstone.civilight.domain.cast.AppearanceBars
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** `한 줄 요약` (CST-72): one line, `닫기` and `저장` (no history, U-10). Feature-local sheet. */
@Serializable
data class SpeechSummarySheet(val projectKey: Key, val characterKey: Key) : SheetKey

/** What the `한 줄 요약` sheet saves: NFC, trimmed, line breaks removed; unchanged text cannot be saved. */
object SpeechSummaryModel {
    fun canSave(typed: String, current: String?): Boolean =
        CharacterFieldActions.normalizeSummary(typed) != CharacterFieldActions.normalizeSummary(current)
}

@Composable
fun SheetScope.SpeechSummaryContent(sheet: SpeechSummarySheet) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val c = doc.index.character(sheet.characterKey)
    if (c == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    var text by rememberSaveable { mutableStateOf(c.speechSummary.orEmpty()) }
    val readOnly = store.readOnly
    SheetHeader(CastUiCopy.SPEECH_SUMMARY, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    CivilightTextField(text, { text = it.replace("\n", " ") }, Modifier.fillMaxWidth().padding(top = 14.dp), singleLine = true, enabled = !readOnly)
    if (!readOnly) {
        SheetActions {
            CivilightButton(CastUiCopy.BTN_SAVE, {
                val typed = text
                editor.act(CastUiCopy.BTN_SAVE) { d, ctx -> CharacterFieldActions.setSpeechSummary(d, sheet.characterKey, typed, ctx.toAction()) }
                host.close()
            }, Modifier.weight(1f), enabled = SpeechSummaryModel.canSave(text, c.speechSummary))
        }
    }
}

/** `한 줄 요약 | 반말 · 짧게 끊음`, or `한 줄 요약 | —` while empty; tapping it opens the sheet. */
@Composable
fun SpeechSummaryRow(summary: String?, onEdit: (() -> Unit)?) {
    RecordFieldRow(
        label = CastUiCopy.SPEECH_SUMMARY,
        value = summary?.takeIf { it.isNotBlank() } ?: VoiceLines.EMPTY_TEXT,
        onClick = onEdit,
    )
}

/**
 * `이 탭 잠그기` (CST-73) at the head of the 비밀 tab. Drawn only when the device can authenticate or the
 * lock is already on (then disabled when the device has no lock, so a set lock is never hidden).
 */
@Composable
fun SecretLockRow(locked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    SwitchRow(CastUiCopy.SECRET_LOCK, locked, onChange, Modifier.fillMaxWidth(), enabled = enabled)
}

/**
 * Appearance bars and `마지막 등장` (CST-71), under the field table on `개요`. One bar per live episode,
 * height proportional to this character's largest value; the row scrolls sideways inside itself. The
 * bars are not tappable. The section label has no source copy yet, so none is drawn; the bars are
 * decorative for accessibility (the `마지막 등장` row is read).
 */
@Composable
fun AppearanceSection(bars: AppearanceBars, modifier: Modifier = Modifier) {
    if (bars.bars.isEmpty()) return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (bar in bars.bars) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.width(18.dp).height(BAR_HEIGHT.dp).clip(RoundedCornerShape(4.dp)).background(colors.line), contentAlignment = Alignment.BottomCenter) {
                        val share = bars.share(bar)
                        if (share > 0f) Box(Modifier.fillMaxWidth().fillMaxHeight(share).background(colors.acc))
                    }
                    CivilightText(bar.label, type.small, color = colors.ink3, maxLines = 1)
                }
            }
        }
        RecordFieldRow(label = CastUiCopy.LAST_SEEN, value = bars.last?.episodeLabel ?: VoiceLines.EMPTY_TEXT)
    }
}

private const val BAR_HEIGHT = 40
