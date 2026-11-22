package io.github.lenticualrstone.civilight.feature.story.check

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.domain.checks.CheckListRow
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.feature.story.common.MiniMaps
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkModel
import io.github.lenticualrstone.civilight.navigation.BulkPlaceTime
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/**
 * 점검 센터 (STO-39 … STO-41, CNV-17 shell; cover `Check` / `TravelCheck` / `EmptyCheck`, inner list
 * pane of `TwoCheck` with the script on the right). No lens chip (R11 §3).
 */
@Composable
fun CheckCenterScreen(route: CheckCenter, deps: FeatureDeps) {
    val vm = entryViewModel { CheckCenterViewModel(route.projectKey, deps.storyServices(), deps.settings) }
    val load by vm.state.collectAsState()
    val report by vm.report.collectAsState()
    val ready = load as? ProjectLoad.Ready
    var filterName by rememberSaveable { mutableStateOf(CheckModel.filterOf(route.filter).name) }
    val filter = CheckModel.filterOf(filterName)
    val r = report
    val ui = remember(r, filter, ready?.doc) { r?.let { CheckModel.build(it, filter, stale = ready != null && it.doc !== ready.doc) } }
    val sheets = rememberSheetHost()
    val navigator = deps.navigator
    val p = route.projectKey
    val gate = rememberHiddenGate(p)
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }

    fun run(result: CheckResult, action: FixAction, members: List<CheckResult> = listOf(result)) {
        val doc = ready?.doc ?: return
        when (val effect = CheckFixes.effect(doc, p, result, action, vm.inputs, members)) {
            is FixEffect.Navigate -> if (effect.detail) navigator.openDetail(effect.route) else navigator.push(effect.route)
            is FixEffect.Edit -> vm.apply(effect.label, effect.transform)
            is FixEffect.Restore -> vm.restoreWithToast(effect.label, effect.step)
            is FixEffect.OpenSheet -> sheets.open(effect.sheet)
            is FixEffect.OpenBulk -> {
                deps.results.send(BulkModel.preselectKey(p), effect.sceneKeys)
                navigator.push(BulkPlaceTime(p))
            }
            is FixEffect.PickSpeaker -> sheets.open(PickSpeakerSheet(p, effect.blockKey))
            FixEffect.Unlock -> gate.unlock()
            FixEffect.Unavailable -> Unit
        }
    }

    CenterColumn {
        CivilightScreen(
            title = CheckCopy.SCREEN_TITLE,
            smallLine = ready?.doc?.project?.name,
            back = if (LocalPaneRole.current == PaneRole.DETAIL) null else IconAction(StoryCopy.BACK) { navigator.pop() },
            commandBar = {
                CommandBar(
                    hint = ui?.commandHint.orEmpty(),
                    onClick = { navigator.push(ProjectSearch(p)) },
                    onMic = { navigator.push(Recording(projectKey = p)) },
                )
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is PickSpeakerSheet -> { { PickSpeakerContent(key, deps) } }
                    else -> null
                }
            },
        ) { padding ->
            val doc = ready?.doc
            // `점검 중` -> list swaps on the kind, never on the data (motion audit, feature:story 7).
            ContentSwap(ui != null && doc != null, Modifier.fillMaxSize()) { loaded ->
                if (!loaded || ui == null || doc == null || ready == null) {
                    Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg)) {
                        CivilightText(CheckCopy.SCANNING, CivilightTheme.type.small, Modifier.padding(top = padding.calculateTopPadding() + 12.dp, start = 24.dp), color = CivilightTheme.colors.ink3)
                    }
                } else {
                    CheckList(
                        ui = ui,
                        doc = doc,
                        padding = padding,
                        expanded = expanded,
                        readOnly = ready.readOnly,
                        onFilter = { filterName = it.name },
                        onToggle = { id -> expanded = if (id in expanded) expanded - id else expanded + id },
                        onLocation = { key -> navigator.openDetail(CheckFixes.scriptAt(doc, p, key)) },
                        onButton = ::run,
                        onUnlock = gate.unlock,
                        onManuscript = { navigator.openDetail(CheckModel.manuscriptRoute(doc)) },
                        buttonsOf = { result, actions, members -> CheckFixes.shown(doc, p, result, actions, vm.inputs, members, ready.readOnly).map { it.first } },
                        selectedKey = CheckModel.selectedKey(navigator.backStack, p),
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckList(
    ui: CheckUi,
    doc: ProjectDoc,
    padding: PaddingValues,
    expanded: List<String>,
    readOnly: Boolean,
    onFilter: (CheckFilter) -> Unit,
    onToggle: (String) -> Unit,
    onLocation: (String) -> Unit,
    onButton: (CheckResult, FixAction, List<CheckResult>) -> Unit,
    onUnlock: () -> Unit,
    onManuscript: () -> Unit,
    buttonsOf: (CheckResult, List<FixAction>, List<CheckResult>) -> List<FixAction>,
    selectedKey: String?,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val lines = remember(ui.rows, expanded) { CheckLines.flatten(ui.rows, expanded) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        item(key = "intro") {
            CivilightText(ui.header.intro, type.small, civilightItem(), color = colors.ink3)
        }
        item(key = "chips") {
            Row(
                civilightItem().fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ui.header.chips.forEach { chip ->
                    // `점검 중` dims the chips; the dim fades (animatedAlpha) instead of jumping.
                    val dim by animatedAlpha(if (chip.dimmed) DIMMED else 1f)
                    FilterChip(chip.label, chip.filter == ui.filter, { onFilter(chip.filter) }, Modifier.graphicsLayer { alpha = dim })
                }
                FoldRevealValue(ui.header.scanningLabel, horizontal = true) {
                    CivilightText(it, type.small, Modifier.padding(start = 6.dp), color = colors.ink3, maxLines = 1)
                }
            }
        }
        ui.empty?.let { empty ->
            item(key = "empty") {
                EmptyBlock(
                    modifier = civilightItem(),
                    title = empty.title,
                    subtitle = empty.line,
                    primaryLabel = empty.buttons[0],
                    onPrimary = onManuscript,
                    secondaryLabel = empty.buttons[1],
                    onSecondary = { onFilter(CheckFilter.ACKNOWLEDGED) },
                )
            }
        }
        items(lines, key = { it.key }) { line ->
            // Every line is its own keyed item; members of a group open and close on the fold tokens.
            val item = civilightItem()
            val fold = civilightItem(CivilightMotion.foldOpen, fadeOut = CivilightMotion.foldClose)
            when (line) {
                is CheckLine.Sub -> {
                    val subTarget = CheckModel.locationTarget(doc, line.result)
                    CheckItemRow(
                        result = line.result,
                        buttons = buttonsOf(line.result, line.result.actions, listOf(line.result)),
                        onLocation = subTarget?.let { { onLocation(it) } },
                        onButton = { onButton(line.result, it, listOf(line.result)) },
                        nested = true,
                        selected = subTarget != null && subTarget == selectedKey,
                        modifier = item,
                    )
                }
                is CheckLine.Member -> {
                    val member = line.result
                    val memberTarget = CheckModel.locationTarget(doc, member)
                    CheckItemRow(
                        result = member,
                        buttons = buttonsOf(member, member.actions, listOf(member)),
                        onLocation = memberTarget?.let { { onLocation(it) } },
                        onButton = { onButton(member, it, listOf(member)) },
                        nested = true,
                        selected = memberTarget != null && memberTarget == selectedKey,
                        modifier = fold,
                    )
                }
                is CheckLine.Top -> when (val row = line.row) {
                    is CheckListRow.HiddenGroup -> LockRow(title = row.text.substringBefore(" · ${CheckCopy.UNLOCK}"), actionLabel = row.action.label, onAction = onUnlock, modifier = item.padding(vertical = 8.dp))
                    is CheckListRow.Item -> {
                        val target = CheckModel.locationTarget(doc, row.result)
                        val buttons = buttonsOf(row.result, row.actions, listOf(row.result))
                        val selected = target != null && target == selectedKey
                        when (val look = TravelLooks.of(row.result, ui.filter)) {
                            is TravelLook.Card -> TravelCard(
                                title = row.result.title,
                                tag = row.result.tag,
                                routeLine = look.warning.routeLine,
                                warning = look.warning,
                                detail = look.detail,
                                chips = buttons.map { it.label },
                                onChip = { i -> onButton(row.result, buttons[i], listOf(row.result)) },
                                modifier = item.padding(vertical = 8.dp),
                                map = remember(doc, look.warning) { MiniMaps.travel(doc, look.warning) },
                                location = row.result.location,
                                onLocation = target?.let { { onLocation(it) } },
                            )
                            is TravelLook.Compact -> TravelCompactRow(
                                look,
                                buttons,
                                { onButton(row.result, it, listOf(row.result)) },
                                selected,
                                location = row.result.location,
                                onLocation = target?.let { { onLocation(it) } },
                                modifier = item,
                            )
                            TravelLook.Plain -> CheckItemRow(
                                result = row.result,
                                buttons = buttons,
                                onLocation = target?.let { { onLocation(it) } },
                                onButton = { onButton(row.result, it, listOf(row.result)) },
                                selected = selected,
                                modifier = item,
                            )
                        }
                    }
                    is CheckListRow.Group -> {
                        val id = row.group.id
                        val first = row.members.first()
                        val groupButtons = buttonsOf(first, row.actions, row.members)
                        Column(item) {
                            CheckGroupRow(row.title, row.members.any { it.marker == Marker.WARNING }, id in expanded) { onToggle(id) }
                            if (groupButtons.isNotEmpty()) ButtonsRow(groupButtons) { onButton(first, it, row.members) }
                        }
                    }
                }
            }
        }
    }
}

private const val DIMMED = 0.5f
