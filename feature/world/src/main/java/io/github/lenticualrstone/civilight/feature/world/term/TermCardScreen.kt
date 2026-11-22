package io.github.lenticualrstone.civilight.feature.world.term

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermChip
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.CardArt
import io.github.lenticualrstone.civilight.feature.world.common.CardHead
import io.github.lenticualrstone.civilight.feature.world.common.FieldSection
import io.github.lenticualrstone.civilight.feature.world.common.FixedRow
import io.github.lenticualrstone.civilight.feature.world.common.GoneCard
import io.github.lenticualrstone.civilight.feature.world.common.ItemMenuSheet
import io.github.lenticualrstone.civilight.feature.world.common.LensBanner
import io.github.lenticualrstone.civilight.feature.world.common.lensBannerSlot
import io.github.lenticualrstone.civilight.feature.world.common.LockedCard
import io.github.lenticualrstone.civilight.feature.world.common.NoticeLine
import io.github.lenticualrstone.civilight.feature.world.common.WorldCommandBar
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.cardPadding
import io.github.lenticualrstone.civilight.feature.world.common.itemSheet
import io.github.lenticualrstone.civilight.feature.world.common.openFromWorld
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.launch
import io.github.lenticualrstone.civilight.navigation.TermCard as TermCardRoute

/**
 * 용어 카드 (WLD-08, board `Term`, right pane of `TwoWorld`): header `세계 · 용어` · name · lens
 * chip · `⋯`; image slot, eyebrow, name, description; field table `첫 공개 · 별칭 · 관련` + project
 * fields; notices and chips; `쓰인 곳` with its groups (WLD-19).
 */
@Composable
fun TermCardScreen(route: TermCardRoute, deps: FeatureDeps) {
    val vm = entryViewModel { TermCardViewModel(route.projectKey, route.termKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val scope = rememberCoroutineScope()
    var showEmpty by rememberSaveable(route.termKey) { mutableStateOf(false) }
    val card = ui

    fun choose(target: RevealTarget) {
        scope.launch {
            when (val outcome = vm.setReveal(target)) {
                is EditOutcome.Done -> {
                    sheets.closeAll()
                    outcome.toast?.let { deps.toasts.show(it) { vm.undoLast() } }
                }
                is EditOutcome.Refused -> deps.toasts.show(outcome.reason, actionLabel = null)
                EditOutcome.ReadOnly -> Unit
            }
        }
    }

    CivilightScreen(
        title = card?.title.orEmpty(),
        smallLine = WorldCopy.SMALL_TERM,
        back = worldBack(route, deps),
        lens = doc?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        banner = lensBannerSlot(doc?.let { LensBanner.of(it, readOnly) }, vm::backToLatest),
        actions = {
            val name = card?.head?.name
            if (card?.access == CardAccess.Open && name != null) {
                CivilightIconButton(
                    icon = IconId.More,
                    contentDescription = WorldCopy.menuLabel(name),
                    onClick = { sheets.open(ItemMenuSheet(WorldKind.TERM.name, route.termKey)) },
                )
            }
        },
        commandBar = {
            WorldCommandBar(deps, route.projectKey, WorldCopy.SEARCH_CARD, EntrySurface.TERM, RecorderTargetKind.TERM, route.termKey)
        },
        sheets = sheets,
        localSheets = { key ->
            termSheet(key, doc, readOnly, gate, ::choose, vm::toggleRelated) ?: itemSheet(key, doc, vm, readOnly, gate)
        },
    ) { padding ->
        CenterColumn {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = cardPadding(padding)) {
                if (card == null) return@LazyColumn
                when (val access = card.access) {
                    is CardAccess.Locked -> item(key = "locked") { LockedCard(access.text, gate.unlock) }
                    is CardAccess.Gone -> item(key = "gone") { GoneCard(access.text, access.dotted) }
                    CardAccess.Open -> termBody(
                        card = card,
                        readOnly = readOnly,
                        showEmpty = showEmpty,
                        onShowEmpty = { showEmpty = true },
                        onReveal = { sheets.open(RevealSheet(route.termKey)) },
                        onRelated = { sheets.open(RelatedSheet(route.termKey)) },
                        onField = { def -> sheets.open(FieldValueEditSheet(route.projectKey, EntityKind.TERM, route.termKey, def)) },
                        onAddField = { sheets.open(AddFieldSheet(route.projectKey, EntityKind.TERM, route.termKey)) },
                        onChip = { chip ->
                            when (chip) {
                                is TermChip.SuggestReveal -> choose(chip.suggestion.target)
                                is TermChip.TurnOffSearch -> vm.setTextSearch(false)
                            }
                        },
                        onSearchOn = { vm.setTextSearch(true) },
                        onOpen = { row -> row.target?.let { deps.navigator.openFromWorld(it) } },
                        onLink = { candidate ->
                            scope.launch {
                                if (vm.linkCandidate(candidate)) deps.toasts.show(WorldCopy.TODO_COPY_WORLD_LINKED_TOAST) { vm.undoLast() }
                            }
                        },
                        onReject = { candidate ->
                            scope.launch {
                                if (vm.rejectCandidate(candidate)) deps.toasts.show(WorldCopy.TODO_COPY_WORLD_REJECTED_TOAST) { vm.undoLast() }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.termBody(
    card: TermCardUi,
    readOnly: Boolean,
    showEmpty: Boolean,
    onShowEmpty: () -> Unit,
    onReveal: () -> Unit,
    onRelated: () -> Unit,
    onField: (String) -> Unit,
    onAddField: () -> Unit,
    onChip: (TermChip) -> Unit,
    onSearchOn: () -> Unit,
    onOpen: (UsageRowUi) -> Unit,
    onLink: (TermCandidate) -> Unit,
    onReject: (TermCandidate) -> Unit,
) {
    val head = card.head ?: return
    item(key = "art") { CardArt(head.name, WorldCopy.IMAGE_LABEL) }
    item(key = "head") { CardHead(head) }
    item(key = "fixed") {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            card.reveal?.let { FixedRow(it, onReveal) }
            card.aliases?.let { FixedRow(it) {} }
            card.related?.let { FixedRow(it, onRelated) }
            FieldSection(
                section = card.fields,
                showEmpty = showEmpty,
                readOnly = readOnly,
                onShowEmpty = onShowEmpty,
                onEdit = { onField(it.fieldDefKey) },
                onAdd = onAddField,
            )
        }
    }
    if (card.notices.isNotEmpty() || card.searchOff || card.chips.isNotEmpty()) {
        item(key = "notices") { TermNotices(card, readOnly, onChip, onSearchOn) }
    }
    item(key = "header") { SectionLabel(card.header, Modifier.padding(top = 22.dp, bottom = 2.dp)) }
    card.groups.forEach { group ->
        // With one group the header already reads `쓰인 곳 · N`.
        if (card.groups.size > 1) {
            item(key = "group:${group.id}") { SectionLabel(group.title, Modifier.padding(top = 14.dp, bottom = 2.dp)) }
        }
        items(group.rows, key = { it.id }) { row ->
            UsageRowView(
                row = row,
                onOpen = row.target?.let { { onOpen(row) } },
                readOnly = readOnly,
                onLink = { row.candidate?.let(onLink) },
                onReject = { row.candidate?.let(onReject) },
            )
        }
    }
}

@Composable
private fun TermNotices(card: TermCardUi, readOnly: Boolean, onChip: (TermChip) -> Unit, onSearchOn: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        card.notices.forEach { NoticeLine(it) }
        if (card.searchOff) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                CivilightText("${WorldCopy.NOTICE_SEARCH_OFF}${WorldCopy.SEP}", type.small, color = colors.ink3)
                TextAction(WorldCopy.BTN_SEARCH_ON, onSearchOn, color = colors.acc, style = type.small)
            }
        }
        if (card.chips.isNotEmpty()) {
            ChipFlow(Modifier.padding(top = 8.dp)) {
                card.chips.forEach { chip ->
                    FilterChip(label = chip.label, selected = false, onClick = { onChip(chip) }, enabled = !readOnly)
                }
            }
        }
    }
}
