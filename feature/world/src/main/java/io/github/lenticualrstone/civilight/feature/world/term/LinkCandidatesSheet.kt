package io.github.lenticualrstone.civilight.feature.world.term

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.term.BulkMode
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.StaleRejections
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.TermIndexes
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.LinkCandidatesSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * The card's own key for the bulk sheet: it carries the tapped word as the initial mode (WLD-23).
 * The shared [LinkCandidatesSheet] has no mode argument (see Requests) and opens in `모두 잇기`.
 */
@Serializable
data class BulkCandidatesSheet(val termKey: Key, val mode: BulkMode) : SheetKey

/**
 * Writes of the bulk sheet, one per host screen and project. It holds the project's shared
 * candidate index itself, so the sheet works over any screen of the project.
 */
class LinkCandidatesViewModel(projectKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    private val handle = TermIndexes.acquire(projectKey, env.appScope)

    val index: StateFlow<CandidateIndex> get() = handle.index

    init {
        combine(doc.filterNotNull(), hiddenOpen) { d, h -> d to h }
            .onEach { (d, h) -> handle.submit(d, TermAccess(h)) }
            .launchIn(viewModelScope)
    }

    /** `넣기 · N개`: one undo step; stale `아님` records of the touched blocks are released with it. */
    suspend fun apply(termKey: Key, mode: BulkMode, unchecked: Set<String>): BulkApply {
        var outcome: BulkApply = BulkApply.Nothing
        val ctx = editContext()
        val open = hiddenOpen.value
        val index = handle.index.value
        val label = if (mode == BulkMode.LINK_ALL) WorldCopy.BULK_LINK_ALL else WorldCopy.BULK_REJECT_ALL
        val result = apply(label) { d: ProjectDoc ->
            when (val r = LinkCandidatesState.apply(d, termKey, index, open, mode, unchecked, ctx)) {
                is BulkApply.Done -> StaleRejections.release(r.doc, TermAccess(open)).also { outcome = r }
                else -> d.also { outcome = r }
            }
        }
        return if (result == EditResult.APPLIED) outcome else BulkApply.Nothing
    }

    override fun onCleared() {
        handle.release()
        super.onCleared()
    }
}

@Composable
private fun rememberLinkCandidates(projectKey: Key): LinkCandidatesViewModel {
    val deps = LocalFeatureDeps.current
    return entryViewModel(key = "world.linkCandidates.$projectKey") { LinkCandidatesViewModel(projectKey, WorldEnv.of(deps)) }
}

/** The card's local key, or null for other keys. */
fun bulkSheet(key: SheetKey): (@Composable SheetScope.() -> Unit)? = when (key) {
    is BulkCandidatesSheet -> { { LinkCandidatesContent(key.termKey, key.mode) } }
    else -> null
}

/** Content of the shared [LinkCandidatesSheet] (owner WORLD), opened by other areas over their screens. */
@Composable
fun SheetScope.LinkCandidatesSheetContent(sheet: LinkCandidatesSheet) {
    LinkCandidatesContent(sheet.termKey, BulkMode.LINK_ALL)
}

/**
 * `후보 N · <용어>` + `취소`; segmented `모두 잇기 | 모두 아님`; rows with a checkbox each; footer;
 * `넣기 · N개`; `넣은 뒤 · 한 단계로 되돌리기`. `넣기` closes the sheet and shows the shell's undo toast.
 */
@Composable
fun SheetScope.LinkCandidatesContent(termKey: Key, initialMode: BulkMode) {
    val store = currentProjectStore
    val deps = LocalFeatureDeps.current
    val vm = rememberLinkCandidates(store.key)
    val doc by store.doc.collectAsState()
    val index by vm.index.collectAsState()
    val open by vm.hiddenOpen.collectAsState()
    val readOnly = store.readOnly
    var modeName by rememberSaveable(termKey) { mutableStateOf(initialMode.name) }
    var unchecked by rememberSaveable(termKey) { mutableStateOf(emptyList<String>()) }
    val mode = BulkMode.valueOf(modeName)
    val state = LinkCandidatesState.build(doc, termKey, index, open, mode, unchecked.toSet())
    val scope = rememberCoroutineScope()
    val sheet = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type

    val title = (state as? BulkSheetState.Ready)?.ui?.title ?: doc.index.term(termKey)?.takeIf { open || !it.hidden }?.name.orEmpty()
    SheetHeader(title = title, onClose = ::close, closeLabel = WorldCopy.BTN_CANCEL)
    SegmentedControl(
        options = LinkCandidatesState.modes.map(WorldCopy::bulkMode),
        selectedIndex = LinkCandidatesState.modes.indexOf(mode),
        onSelect = { modeName = LinkCandidatesState.modes[it].name },
        modifier = Modifier.padding(top = 12.dp),
    )
    when (state) {
        BulkSheetState.Pending -> CivilightText(WorldCopy.SCANNING, type.small, Modifier.padding(top = 16.dp), color = colors.ink3)
        BulkSheetState.Empty -> CivilightText(WorldCopy.TODO_COPY_WORLD_NO_CANDIDATES, type.small, Modifier.padding(top = 16.dp), color = colors.ink3)
        is BulkSheetState.Ready -> {
            val ui = state.ui
            SheetScrollBody(Modifier.padding(top = 12.dp)) {
                ui.rows.forEach { row ->
                    BulkRow(row) { checked ->
                        unchecked = if (checked) unchecked - row.id else (unchecked + row.id).distinct()
                    }
                }
            }
            ui.footer?.let { footer ->
                val warnAt = footer.indexOf('▲')
                Row(Modifier.fillMaxWidth().topHairline(colors.line).padding(top = 10.dp)) {
                    if (warnAt > 0) {
                        CivilightText(footer.substring(0, warnAt), type.small, color = colors.ink3)
                        CivilightText(footer.substring(warnAt), type.small, color = colors.warn)
                    } else {
                        CivilightText(footer, type.small, color = colors.ink3)
                    }
                }
            }
            SheetActions {
                CivilightButton(
                    text = ui.button,
                    modifier = Modifier.weight(1f),
                    enabled = !readOnly && ui.checkedCount > 0,
                    onClick = {
                        scope.launch {
                            when (val r = vm.apply(termKey, mode, unchecked.toSet())) {
                                is BulkApply.Done -> {
                                    sheet.close()
                                    deps.toasts.show(WorldCopy.bulkDone(mode, r.count)) { vm.undoLast() }
                                }
                                // The text changed under the sheet: rows are rebuilt from the new index.
                                BulkApply.Stale, BulkApply.Nothing -> Unit
                            }
                        }
                    },
                )
            }
            CivilightText(
                if (readOnly) WorldCopy.READ_ONLY else ui.note,
                type.small,
                Modifier.fillMaxWidth().padding(top = 8.dp),
                color = colors.ink3,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One row: the checkbox (22 dp mark, 44 dp target) and location + sentence with the box inline. */
@Composable
private fun BulkRow(row: BulkRowUi, onChecked: (Boolean) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(PressAction.Toggle(row.checked, onChecked), role = Role.Checkbox, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckboxMark(checked = row.checked)
        Column(Modifier.weight(1f)) {
            CivilightText(row.location, type.small, color = colors.ink3)
            DecoratedText(
                text = AnnotatedString(row.text),
                style = type.lead,
                color = colors.ink,
                decorations = if (row.boxEnd > row.boxStart) listOf(DecorationRange(row.boxStart, row.boxEnd, InlineDecoration.SEARCH_HIT)) else emptyList(),
            )
        }
    }
}
