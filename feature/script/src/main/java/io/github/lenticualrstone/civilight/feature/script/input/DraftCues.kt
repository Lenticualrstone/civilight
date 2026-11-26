package io.github.lenticualrstone.civilight.feature.script.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.script.DirectionCues
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.state.CueEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/*
 * (0.2.0, SCR-08, board `U14CueGame`) The draft's cue chips in the input field, with `게임 제작 도구` on only.
 * The chips edit the draft; the block takes them on the send (or the `고치기` send). With the switch off they
 * are not drawn and never sent, so stored cues stay as they are (scope §8).
 */

/** Text of one cue chip: `{kind} {name}` (`효과음 바람`, `화면 효과 암전`; a background reads `배경 …`). */
fun cueChipText(doc: ProjectDoc, cue: DirectionCue): String = DirectionCues.cueLine(doc, cue).removePrefix("▸ ").let { line ->
    if (cue.kind.known == CueKind.BGM) "${DirectionCues.kindWord(CueKind.BGM)} ${line.removePrefix("♪ ")}" else line
}

/** The cue chips above the text: each `{kind} {name}` with `✕` (`큐 빼기`), then a round `＋` (`큐 넣기`). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DraftCueChips(doc: ProjectDoc, cues: List<DirectionCue>, onRemove: (Key) -> Unit, onAdd: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        cues.forEach { cue ->
            val shape = RoundedCornerShape(10.dp)
            // No clip on the chip: the `✕` touch area reaches 44 × 44 past its 30 × 36 drawing (DS-11).
            Row(
                Modifier.height(36.dp).background(colors.tint, shape).border(1.dp, colors.line, shape).padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CivilightText(cueChipText(doc, cue), type.number, color = colors.ink2, maxLines = 1)
                Box(
                    Modifier
                        .pressable(
                            PressAction.Click({ onRemove(cue.key) }),
                            role = Role.Button,
                            hitTarget = HitTarget.ICON_BUTTON,
                            shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                            scaleOnPress = false,
                        )
                        .width(30.dp)
                        .height(36.dp)
                        .semantics { contentDescription = WriteCopy.CUE_REMOVE },
                    contentAlignment = Alignment.Center,
                ) { CivilightIcon(IconId.X, null, size = 14.dp, tint = colors.ink3) }
            }
        }
        Box(
            Modifier
                .pressable(PressAction.Click(onAdd), role = Role.Button, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape, scaleOnPress = false)
                .size(36.dp)
                .border(1.dp, colors.acc, CircleShape)
                .semantics { contentDescription = WriteCopy.CUE_ADD },
            contentAlignment = Alignment.Center,
        ) { CivilightIcon(IconId.Plus, null, size = 16.dp, tint = colors.acc) }
    }
}

/** The `큐 넣기` menu of the input (feature-local key). */
@Serializable
data class DraftCueSheetKey(val projectKey: Key, val episodeKey: Key? = null) : SheetKey

/**
 * `큐 넣기` (SCR-08): rows `배경 · BGM · 효과음 · CG` (subline `에셋 고르기`) and `화면 효과` (subline `페이드 · 암전`);
 * a row opens that kind's picker (the asset list filtered to the kind, SCR-61, or the two effects). A pick adds
 * a cue chip to the draft. [onNewAsset] opens the 0.1.0 asset form of that kind (the new asset is then listed).
 */
@Composable
fun SheetScope.DraftCueSheet(
    key: DraftCueSheetKey,
    doc: ProjectDoc,
    onPick: (CueKind, assetKey: Key?, effect: String?) -> Unit,
    onNewAsset: (CueKind) -> Unit,
) {
    val sheet = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    var openKind by rememberSaveable { mutableStateOf<CueKind?>(null) }
    val kind = openKind
    SheetHeader(
        title = kind?.let(DirectionCues::kindWord) ?: WriteCopy.CUE_ADD,
        onClose = { if (kind != null) openKind = null else sheet.close() },
    )
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        if (kind == null) {
            DirectionCues.KINDS.forEach { k ->
                ListRow(
                    title = DirectionCues.kindWord(k),
                    subtitle = if (k == CueKind.SCREEN_EFFECT) WriteCopy.CUE_EFFECT_SUB else WriteCopy.CUE_ASSET_SUB,
                    compact = true,
                    onClick = { openKind = k },
                )
            }
            return@SheetScrollBody
        }
        val picks = CueEditor.picks(doc, kind, key.episodeKey)
        if (picks.isEmpty()) CivilightText(ScriptCopy.NO_ASSETS, type.small, Modifier.padding(vertical = 12.dp), color = colors.ink3)
        Column {
            picks.forEach { pick ->
                ListRow(
                    title = pick.title,
                    subtitle = pick.subtitle,
                    compact = true,
                    trailing = pick.trailing?.let { t -> { CivilightText(t, type.number, color = colors.ink3, maxLines = 1) } },
                    onClick = {
                        sheet.close()
                        onPick(kind, pick.assetKey, pick.effect)
                    },
                )
            }
        }
        if (kind != CueKind.SCREEN_EFFECT) {
            TextAction(io.github.lenticualrstone.civilight.feature.script.assets.AssetsCopy.ADD_FOR_CUE, onClick = { onNewAsset(kind) }, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
