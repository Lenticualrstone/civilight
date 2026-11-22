package io.github.lenticualrstone.civilight.feature.canvas.label

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.domain.cast.relation.LineSlot
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.common.canvasEditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.LineLabelSheet
import io.github.lenticualrstone.civilight.navigation.RelationKindSettings
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import kotlinx.serialization.Serializable

/** `시작 시점` picker over a line-label surface (feature-local key, registered so it works over any screen). */
@Serializable
data class LineStartSheet(val projectKey: Key, val canvasKey: Key, val edgeKey: Key) : SheetKey

/** The entry's [LineLabelEditor]; every host in one entry gets the same one. */
@Composable
fun rememberLineLabelEditor(projectKey: Key): LineLabelEditor {
    val deps = LocalFeatureDeps.current
    return entryViewModel(key = "${LineLabelEditor.KEY}-$projectKey") { LineLabelEditor(projectKey, deps.projects, deps::canvasEditContext) }
}

/** Everything one surface needs: the view, the current draft and the actions. */
class LineLabelState(
    val view: LineLabelView,
    val draft: LineLabelDraft,
    val doc: ProjectDoc,
    private val editor: LineLabelEditor,
    private val canvasKey: Key,
    private val edgeKey: Key,
) {
    fun text(value: String) = editor.put(edgeKey, draft.copy(text = value))

    fun slot(slot: LineSlot) = editor.put(edgeKey, LineLabelModel.withSlot(doc, draft, slot))

    fun kind(key: Key) = editor.put(edgeKey, draft.copy(kindKey = key))

    fun start(unitKey: Key) = editor.put(edgeKey, draft.copy(startUnitKey = unitKey))

    fun discard() = editor.discard(edgeKey)

    fun keep(then: (EditResult) -> Unit) = editor.keep(canvasKey, edgeKey, draft, then)

    fun make(then: (EditResult) -> Unit) = editor.make(canvasKey, edgeKey, draft, then)

    fun remove(then: (EditResult) -> Unit) = editor.remove(canvasKey, edgeKey, then)
}

/** The state of the `선 라벨` surface for a canvas line, or null while loading or when the line is gone. */
@Composable
fun rememberLineLabelState(projectKey: Key, canvasKey: Key, edgeKey: Key): LineLabelState? {
    val editor = rememberLineLabelEditor(projectKey)
    val load by editor.state.collectAsState()
    val gate = rememberHiddenGate(projectKey)
    val ready = load as? ProjectLoad.Ready ?: return null
    val doc = ready.doc
    val ends = remember(doc, canvasKey, edgeKey, gate.open) { LineLabelModel.ends(doc, canvasKey, edgeKey, gate.open) } ?: return null
    val edge = doc.canvases[canvasKey]?.edges?.firstOrNull { it.key == edgeKey } ?: return null
    val lens = LensRules.effective(doc, doc.app.view.lens.point)
    val draft = editor.draft(edgeKey) ?: LineLabelModel.initialDraft(doc, edge.labelText, ends.first, ends.second, lens)
    val view = remember(doc, ends, draft, ready.readOnly) { LineLabelModel.build(doc, ends.first, ends.second, draft, ready.readOnly) }
    return LineLabelState(view, draft, doc, editor, canvasKey, edgeKey)
}

/**
 * Rows of the `선 라벨` surface, top to bottom (CNV-08): label field; `무엇으로 적을지` chips; `종류`
 * chips + `+ 새 종류`; `시작 시점` row; the two equal buttons. [compact] is the inner popover's
 * tighter spacing (board `Canvas`).
 */
@Composable
fun ColumnScope.LineLabelRows(
    state: LineLabelState,
    compact: Boolean,
    onNewKind: () -> Unit,
    onPickStart: () -> Unit,
    onDone: () -> Unit,
) {
    val view = state.view
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val deps = LocalFeatureDeps.current
    val after: (EditResult) -> Unit = { r ->
        if (r == EditResult.READ_ONLY) deps.toasts.show(CanvasCopy.READ_ONLY, actionLabel = null) else onDone()
    }
    CivilightTextField(
        value = view.text,
        onValueChange = state::text,
        modifier = Modifier.padding(top = if (compact) 8.dp else 12.dp),
        enabled = !view.readOnly,
    )
    // The relation rows fold in and out with the line's kind of ends (P2); the kind chips swap with the chosen
    // `무엇으로 적을지` (P3); the start value swaps like a lens value when it is picked (P4).
    FoldReveal(view.relational) {
        Column {
            SectionLabel(CanvasCopy.WHAT_TO_RECORD, Modifier.padding(top = if (compact) 10.dp else 18.dp))
            ChipFlow(Modifier.padding(top = if (compact) 6.dp else 8.dp)) {
                for (chip in view.slotChips) FilterChip(chip.label, chip.selected, { state.slot(chip.slot) })
            }
            CivilightText(CanvasCopy.KIND, type.small, Modifier.padding(top = if (compact) 8.dp else 12.dp), color = colors.ink3)
            ContentSwap(view.slotChips.firstOrNull { it.selected }?.slot to view.kindChips, contentKey = { it.first }, label = "lineKindChips") { (_, kinds) ->
                ChipFlow(Modifier.padding(top = if (compact) 6.dp else 8.dp)) {
                    for (chip in kinds) FilterChip(chip.label, chip.selected, { state.kind(chip.key) })
                    FilterChip(CanvasCopy.BTN_NEW_KIND, false, onNewKind, enabled = !view.readOnly)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = if (compact) 8.dp else 14.dp)
                    .topHairline(colors.line)
                    .pressable(onClick = onPickStart, hitTarget = HitTarget.BUTTON)
                    .heightIn(min = 44.dp)
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CivilightText(CanvasCopy.START_POINT, type.label, color = colors.ink3)
                ValueSwap(view.startValue.orEmpty(), type.number, color = colors.ink, contentAlignment = Alignment.CenterEnd)
            }
        }
    }
    FoldReveal(view.readOnly) { CivilightText(CanvasCopy.READ_ONLY, type.small, Modifier.padding(top = 10.dp), color = colors.ink3) }
    SheetActions(Modifier.padding(top = if (compact) 0.dp else 2.dp)) {
        CivilightButton(
            text = view.keepLabel,
            onClick = { state.keep(after) },
            modifier = Modifier.weight(1f),
            variant = if (compact) ButtonVariant.COMPACT_GHOST else ButtonVariant.GHOST,
            enabled = !view.readOnly,
        )
        view.makeLabel?.let { make ->
            CivilightButton(
                text = make,
                onClick = { state.make(after) },
                modifier = Modifier.weight(1f),
                variant = if (compact) ButtonVariant.COMPACT_PRIMARY else ButtonVariant.PRIMARY,
                enabled = view.makeEnabled,
            )
        }
    }
    // Take the line off the canvas (the relation, if any, stays; Q-CV-13). Copy proposed, see Requests.
    TextAction(
        CanvasCopy.BTN_REMOVE_LINE,
        { state.remove(after) },
        Modifier.align(Alignment.End).padding(top = 4.dp),
        color = if (view.readOnly) colors.ink3 else colors.ink2,
    )
}

/**
 * The shared `LineLabelSheet` content (CANVAS owner, shell.md §5): cover bottom sheet `CanvasCLabel`;
 * other areas get the same content in their sheet host.
 */
@Composable
fun SheetScope.LineLabelSheetContent(sheet: LineLabelSheet) {
    val canvasKey = sheet.canvasKey
    if (canvasKey == null) {
        // `LineLabelSheet.forCharacters` (relationship-map line, core pass): no lane opens it yet, and a
        // pair without a canvas edge needs a cast write path that is not `CanvasRelations` (see status file).
        SheetHeader(title = CanvasCopy.LINE_LABEL_TITLE, onClose = ::close, closeLabel = CanvasCopy.BTN_CLOSE)
        return
    }
    val deps = LocalFeatureDeps.current
    val state = rememberLineLabelState(sheet.projectKey, canvasKey, sheet.edgeKey)
    if (state == null) {
        SheetHeader(title = CanvasCopy.LINE_LABEL_TITLE, onClose = ::close, closeLabel = CanvasCopy.BTN_CLOSE)
        return
    }
    SheetHeader(
        title = state.view.title,
        subtitle = state.view.sub,
        closeLabel = CanvasCopy.BTN_CLOSE,
        onClose = {
            state.discard()
            close()
        },
    )
    val scope = this
    SheetScrollBody {
        LineLabelRows(
            state = state,
            compact = false,
            onNewKind = { deps.navigator.push(RelationKindSettings(sheet.projectKey)) },
            onPickStart = { if (!state.view.readOnly) scope.open(LineStartSheet(sheet.projectKey, canvasKey, sheet.edgeKey)) },
            onDone = scope::close,
        )
    }
}

/** `시작 시점` (Q-CV-10 default: an episode or a scene; the record applies from the start of that unit). */
@Composable
fun SheetScope.LineStartSheetContent(sheet: LineStartSheet) {
    val state = rememberLineLabelState(sheet.projectKey, sheet.canvasKey, sheet.edgeKey)
    val gate = rememberHiddenGate(sheet.projectKey)
    if (state == null) {
        SheetHeader(title = CanvasCopy.START_POINT, onClose = ::close, closeLabel = CanvasCopy.BTN_CLOSE)
        return
    }
    val structure = state.doc.project.structure
    val levels = remember(structure) { setOfNotNull(structure.episodeLevel, structure.sceneLevel) }
    UnitPickerSheet(
        doc = state.doc,
        gate = gate,
        title = CanvasCopy.START_POINT,
        onPick = { key ->
            state.start(key)
            close()
        },
        onClose = ::close,
        selectedKey = state.draft.startUnitKey,
        levels = levels,
    )
}

/** Column wrapper used by the inner popover card. */
@Composable
fun LineLabelPopoverBody(
    state: LineLabelState,
    onNewKind: () -> Unit,
    onPickStart: () -> Unit,
    onDone: () -> Unit,
) {
    Column {
        SectionLabel(CanvasCopy.LINE_LABEL_TITLE)
        LineLabelRows(state, compact = true, onNewKind = onNewKind, onPickStart = onPickStart, onDone = onDone)
    }
}
