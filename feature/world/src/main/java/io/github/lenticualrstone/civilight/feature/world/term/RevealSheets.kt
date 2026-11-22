package io.github.lenticualrstone.civilight.feature.world.term

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.TermReveals
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** `공개 시점 옮기기` (WLD-18, and the check center's button of the same name, WLD-28). */
@Serializable
data class RevealSheet(val termKey: Key) : SheetKey

/** `원고에서 고르기`, step 1: the scene (the script has no block-pick mode yet, see Requests). */
@Serializable
data class RevealScenePickSheet(val termKey: Key) : SheetKey

/** `원고에서 고르기`, step 2: a block of [sceneKey]. */
@Serializable
data class RevealBlockPickSheet(val termKey: Key, val sceneKey: Key) : SheetKey

/** `에피소드 · 장으로`: a chapter or episode point (`공개 예정 2-5`). */
@Serializable
data class RevealPointPickSheet(val termKey: Key) : SheetKey

/** Writes `첫 공개` for the reveal sheets; one per host screen and project (works over any screen of the project). */
class RevealEditorViewModel(projectKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    /** Sets or moves the reveal of [termKey] to [target] at once (WLD-18), one undo step. */
    suspend fun setReveal(termKey: Key, target: RevealTarget): EditOutcome {
        var outcome: EditOutcome = EditOutcome.Done(null)
        val ctx = editContext()
        val result = apply(WorldCopy.REVEAL_TITLE) { d ->
            when (val r = RevealOptions.apply(d, termKey, target, ctx)) {
                is RevealApply.Refused -> d.also { outcome = EditOutcome.Refused(r.reason) }
                is RevealApply.Applied -> r.doc.also { outcome = EditOutcome.Done(r.toast) }
            }
        }
        return if (result == EditResult.READ_ONLY) EditOutcome.ReadOnly else outcome
    }
}

@Composable
private fun rememberRevealEditor(projectKey: Key): RevealEditorViewModel {
    val deps = LocalFeatureDeps.current
    return entryViewModel(key = "world.revealEditor.$projectKey") { RevealEditorViewModel(projectKey, WorldEnv.of(deps)) }
}

/**
 * The reveal sheets' content, self-contained (it reads the open project store, so any screen of
 * the project can show it): null for other keys. Each step replaces the one before, so choosing
 * closes the flow; the toast `◇ 첫 공개 1-7 → 1-8 · ▲ 새로 1` carries `되돌리기`.
 */
fun revealSheet(key: SheetKey): (@Composable SheetScope.() -> Unit)? = when (key) {
    is RevealSheet -> { { RevealContent(key.termKey) } }
    is RevealScenePickSheet -> { { ScenePickContent(key.termKey) } }
    is RevealBlockPickSheet -> { { BlockPickContent(key) } }
    is RevealPointPickSheet -> { { PointPickContent(key.termKey) } }
    else -> null
}

/** Registers the reveal sheets with the shell, so a sheet opened from another area's screen shows. */
fun registerRevealSheets(deps: FeatureDeps) {
    deps.sheets.register<RevealSheet> { RevealContent(it.termKey) }
    deps.sheets.register<RevealScenePickSheet> { ScenePickContent(it.termKey) }
    deps.sheets.register<RevealBlockPickSheet> { BlockPickContent(it) }
    deps.sheets.register<RevealPointPickSheet> { PointPickContent(it.termKey) }
}

/** Applies [target] for [termKey]: closes the sheet and shows the toast, or the refusal. */
@Composable
private fun SheetScope.rememberChoose(termKey: Key): (RevealTarget) -> Unit {
    val store = currentProjectStore
    val deps = LocalFeatureDeps.current
    val editor = rememberRevealEditor(store.key)
    val scope = rememberCoroutineScope()
    val sheet = this
    return { target ->
        scope.launch {
            when (val outcome = editor.setReveal(termKey, target)) {
                is EditOutcome.Done -> {
                    sheet.close()
                    outcome.toast?.let { deps.toasts.show(it) { editor.undoLast() } }
                }
                is EditOutcome.Refused -> deps.toasts.show(outcome.reason, actionLabel = null)
                EditOutcome.ReadOnly -> Unit
            }
        }
    }
}

@Composable
private fun SheetScope.RevealContent(termKey: Key) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(store.key)
    val readOnly = store.readOnly
    val choose = rememberChoose(termKey)
    val links = remember(doc, gate.open) { RevealOptions.links(doc, termKey, gate.open) }
    val sheet = this
    SheetHeader(title = WorldCopy.REVEAL_TITLE, onClose = ::close, subtitle = doc.index.term(termKey)?.takeIf { gate.open || !it.hidden }?.name)
    SheetScrollBody {
        if (links.isNotEmpty()) {
            SectionLabel(WorldCopy.REVEAL_LINKS, Modifier.padding(top = 16.dp, bottom = 4.dp))
            links.forEach { option ->
                ListRow(
                    title = option.label,
                    subtitle = option.refusal ?: if (readOnly) WorldCopy.READ_ONLY else null,
                    onClick = if (option.refusal == null && !readOnly && !option.current) ({ choose(option.target) }) else null,
                    selected = option.current,
                    compact = true,
                )
            }
        }
        SettingsRow(
            title = WorldCopy.REVEAL_FROM_SCRIPT,
            onClick = { sheet.close(); sheet.open(RevealScenePickSheet(termKey)) },
            modifier = Modifier.padding(top = 12.dp),
        )
        SettingsRow(title = WorldCopy.REVEAL_TO_UNIT, onClick = { sheet.close(); sheet.open(RevealPointPickSheet(termKey)) })
    }
}

@Composable
private fun SheetScope.ScenePickContent(termKey: Key) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(store.key)
    val sheet = this
    UnitPickerSheet(
        doc = doc,
        gate = gate,
        title = WorldCopy.REVEAL_FROM_SCRIPT,
        onPick = { sceneKey ->
            sheet.close()
            sheet.open(RevealBlockPickSheet(termKey, sceneKey))
        },
        onClose = ::close,
        levels = RevealOptions.sceneLevels(doc),
    )
}

@Composable
private fun SheetScope.BlockPickContent(key: RevealBlockPickSheet) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val readOnly = store.readOnly
    val choose = rememberChoose(key.termKey)
    val options = remember(doc, key) { RevealOptions.blocks(doc, key.termKey, key.sceneKey) }
    val scene = remember(doc, key) { UnitLabels(doc).reference(key.sceneKey) }
    SheetHeader(title = WorldCopy.REVEAL_FROM_SCRIPT, onClose = ::close, subtitle = scene)
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        items(options, key = { it.id }) { option ->
            ListRow(
                title = option.label,
                subtitle = if (readOnly) WorldCopy.READ_ONLY else null,
                onClick = if (!readOnly && !option.current) ({ choose(option.target) }) else null,
                selected = option.current,
                compact = true,
            )
        }
    }
}

@Composable
private fun SheetScope.PointPickContent(termKey: Key) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(store.key)
    val readOnly = store.readOnly
    val choose = rememberChoose(termKey)
    val current = TermReveals.record(doc, termKey)?.anchor
    UnitPickerSheet(
        doc = doc,
        gate = gate,
        title = WorldCopy.REVEAL_TO_UNIT,
        onPick = { unitKey -> if (!readOnly) choose(RevealTarget.Unit(unitKey)) },
        onClose = ::close,
        selectedKey = (current as? Anchor.UnitStart)?.unitKey,
        levels = RevealOptions.pointLevels(doc),
    )
}
