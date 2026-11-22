package io.github.lenticualrstone.civilight.feature.script.ui

import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.assets.AssetRowView
import io.github.lenticualrstone.civilight.feature.script.assets.AssetsCopy
import io.github.lenticualrstone.civilight.feature.script.assets.AssetsModel
import io.github.lenticualrstone.civilight.feature.script.state.CueEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** The cue sheet of a `연출 지시` block (SCR-08 `[PROPOSED]` editor). */
@Serializable
data class DirectionSheetKey(val projectKey: Key, val blockKey: Key) : SheetKey

/** What the cue sheet does with a pick. */
class CueActions(
    val add: (CueKind, assetKey: Key?, effect: String?) -> Unit,
    val replace: (cueKey: Key, assetKey: Key?, effect: String?) -> Unit,
    val remove: (cueKey: Key) -> Unit,
    /** `+ 에셋` under an asset kind: the asset form of that kind (SCR-61); null hides it. */
    val addAsset: ((CueKind) -> Unit)? = null,
)

/**
 * `연출 지시 · #041` (SCR-08 `[PROPOSED]`, not drawn): the block's cues in order, each with `바꾸기 ·
 * 빼기`; `+ 연출` with the cue-kind chips `배경 · BGM · 효과음 · CG · 화면 효과`. A chip lists that
 * kind's assets (the asset list filtered to the kind, SCR-61) or the screen effects; a pick adds a
 * cue, or changes the cue whose `바꾸기` was tapped. Read-only projects show the cues only. The sheet
 * closes by itself when the block is gone (the last `빼기` removes the empty block).
 */
@Composable
fun SheetScope.DirectionSheet(key: DirectionSheetKey, doc: ProjectDoc, readOnly: Boolean, actions: CueActions) {
    val model = CueEditor.of(doc, key.blockKey)
    // The screen's document lags the store by a frame or two: a block just made by `/연` may not be
    // in it yet. Close only once the block was seen and then went away (the last `빼기`).
    var seen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(model != null) {
        if (model != null) seen = true else if (seen) close()
    }
    if (model == null) {
        SheetHeader(title = ScriptCopy.DIRECTION_TITLE, onClose = ::close)
        return
    }
    // The open kind chip, and the cue being changed (null = a pick adds a cue).
    var openKind by rememberSaveable { mutableStateOf<CueKind?>(null) }
    var replacing by rememberSaveable { mutableStateOf<Key?>(null) }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(title = model.title, onClose = ::close, subtitle = model.number)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        model.cues.forEach { cue ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .topHairline(colors.line)
                    .selectedRowBackground(replacing == cue.key, colors.sel, colors.acc)
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CivilightText(cue.line, type.dir, Modifier.weight(1f), color = colors.ink2, maxLines = 2)
                if (!readOnly) {
                    TextAction(ScriptCopy.CHANGE, onClick = {
                        replacing = cue.key
                        openKind = cue.kind
                    })
                    TextAction(ScriptCopy.REMOVE_CUE, onClick = {
                        if (replacing == cue.key) replacing = null
                        actions.remove(cue.key)
                    })
                }
            }
        }
        if (readOnly) return@SheetScrollBody
        SectionLabel(ScriptCopy.ADD_CUE, Modifier.padding(top = 16.dp, bottom = 8.dp))
        ChipFlow {
            model.kinds.forEach { (kind, word) ->
                FilterChip(word, selected = openKind == kind, onClick = {
                    if (openKind == kind) {
                        openKind = null
                        replacing = null
                    } else {
                        openKind = kind
                        // A chip of another kind adds; changing keeps the cue's own kind.
                        if (replacing != null && model.cues.firstOrNull { it.key == replacing }?.kind != kind) replacing = null
                    }
                })
            }
        }
        // Motion audit P3: the open kind's picker folds in under the chips and swaps when another kind opens.
        ContentSwap(openKind, label = "cuePicker") { kind ->
        Column(Modifier.fillMaxWidth()) {
        if (kind == null) return@Column
        fun pick(assetKey: Key?, effect: String?) {
            val cue = replacing
            if (cue != null) actions.replace(cue, assetKey, effect) else actions.add(kind, assetKey, effect)
            replacing = null
            openKind = null
        }
        // SCR-61: the asset list filtered to this kind is the picker (same rows, sublines for this
        // block's episode); `+ 에셋` adds one of the kind.
        if (kind != CueKind.SCREEN_EFFECT) {
            val episode = doc.index.block(key.blockKey)?.let { doc.index.numberingUnitKeyOfScene(it.sceneKey) }
            val rows = AssetsModel.pickerRows(doc, kind, episode)
            if (rows.isEmpty()) CivilightText(ScriptCopy.NO_ASSETS, type.small, Modifier.padding(top = 12.dp), color = colors.ink3)
            rows.forEach { row -> AssetRowView(row) { pick(row.key, null) } }
            actions.addAsset?.let { add -> TextAction(AssetsCopy.ADD_FOR_CUE, onClick = { add(kind) }, modifier = Modifier.padding(top = 4.dp)) }
            return@Column
        }
        CueEditor.picks(doc, kind).forEach { effect ->
            ListRow(
                title = effect.title,
                subtitle = effect.subtitle,
                compact = true,
                trailing = effect.trailing?.let { t -> { CivilightText(t, type.number, color = colors.ink3, maxLines = 1) } },
                onClick = { pick(effect.assetKey, effect.effect) },
            )
        }
        }
        }
    }
}
