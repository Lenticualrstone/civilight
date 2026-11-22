package io.github.lenticualrstone.civilight.feature.world.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.feature.world.common.CardBodySwap
import io.github.lenticualrstone.civilight.feature.world.common.dimmed
import io.github.lenticualrstone.civilight.feature.world.common.stageOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.CardImageSlot
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.feature.world.common.MergePickSheet
import io.github.lenticualrstone.civilight.feature.world.common.SameNameLine
import io.github.lenticualrstone.civilight.feature.world.common.WorldGoneCard
import io.github.lenticualrstone.civilight.feature.world.common.CardDescription
import io.github.lenticualrstone.civilight.feature.world.common.CardHead
import io.github.lenticualrstone.civilight.feature.world.common.FieldSection
import io.github.lenticualrstone.civilight.feature.world.common.FixedRow
import io.github.lenticualrstone.civilight.feature.world.common.ItemMenuSheet
import io.github.lenticualrstone.civilight.feature.world.common.LensBanner
import io.github.lenticualrstone.civilight.feature.world.common.lensBannerSlot
import io.github.lenticualrstone.civilight.feature.world.common.LockedCard
import io.github.lenticualrstone.civilight.feature.world.common.WorldCommandBar
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.cardPadding
import io.github.lenticualrstone.civilight.feature.world.common.itemSheet
import io.github.lenticualrstone.civilight.feature.world.common.openFromWorld
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.ItemPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.github.lenticualrstone.civilight.navigation.PlaceCard as PlaceCardRoute

/** `지역` of a place: another place, or `비우기` (feature-local). */
@Serializable
data class ParentPlaceSheet(val placeKey: Key) : SheetKey

/**
 * 장소 카드, full shape (WLD-09, board `PlaceTime`): header `세계 · 장소` · name · lens chip · `⋯`;
 * image slot, eyebrow `장소 · <지역>`, name, tabs `개요 · 이곳의 시간 · 지도`. The summary shape on
 * the map (sheet / side card) is feature:map's.
 */
@Composable
fun PlaceCardScreen(route: PlaceCardRoute, deps: FeatureDeps) {
    val vm = entryViewModel { PlaceCardViewModel(route.projectKey, route.placeKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    var tab by rememberSaveable(route.placeKey) { mutableIntStateOf(PlaceCardUi.tabIndex(route.tab)) }
    var showEmpty by rememberSaveable(route.placeKey) { mutableStateOf(false) }
    val card = ui

    CivilightScreen(
        title = card?.title.orEmpty(),
        smallLine = WorldCopy.SMALL_PLACE,
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
                    onClick = { sheets.open(ItemMenuSheet(WorldKind.PLACE.name, route.placeKey)) },
                )
            }
        },
        commandBar = {
            WorldCommandBar(deps, route.projectKey, WorldCopy.SEARCH_CARD, EntrySurface.PLACE, RecorderTargetKind.PLACE, route.placeKey)
        },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is ParentPlaceSheet -> doc?.let { d -> { ParentPlaceContent(key.placeKey, d, gate, readOnly, vm) } }
                else -> itemSheet(key, doc, vm, readOnly, gate)
            }
        },
    ) { padding ->
        CenterColumn {
            CardBodySwap(card, { stageOf(it?.access) }) { shown ->
                val card = shown
                LazyColumn(Modifier.fillMaxSize(), contentPadding = cardPadding(padding)) {
                    if (card == null) return@LazyColumn
                    when (val access = card.access) {
                        is CardAccess.Locked -> item(key = "locked") { Box(civilightItem()) { LockedCard(access.text, gate.unlock) } }
                        is CardAccess.Gone -> item(key = "gone") { Box(civilightItem()) { WorldGoneCard(access, doc, vm, WorldKind.PLACE, route.placeKey, readOnly, gate) } }
                        CardAccess.Open -> placeBody(
                            card = card,
                            tab = tab,
                            onTab = { tab = it },
                            readOnly = readOnly,
                            showEmpty = showEmpty,
                            onShowEmpty = { showEmpty = true },
                            onParent = { sheets.open(ParentPlaceSheet(route.placeKey)) },
                            onField = { def -> sheets.open(FieldValueEditSheet(route.projectKey, EntityKind.PLACE, route.placeKey, def)) },
                            onAddField = { sheets.open(AddFieldSheet(route.projectKey, EntityKind.PLACE, route.placeKey)) },
                            onOpen = { deps.navigator.openFromWorld(it) },
                            art = { name ->
                                doc?.let { d ->
                                    CardImageSlot(d, WorldKind.PLACE, route.placeKey, name, label = null, readOnly = readOnly) {
                                        sheets.open(CardImages.pickSheet(route.projectKey, WorldKind.PLACE, route.placeKey))
                                    }
                                }
                            },
                            sameName = { SameNameLine(doc, WorldKind.PLACE, route.placeKey, readOnly) { sheets.open(MergePickSheet(WorldKind.PLACE.name, route.placeKey)) } },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.placeBody(
    card: PlaceCardUi,
    tab: Int,
    onTab: (Int) -> Unit,
    readOnly: Boolean,
    showEmpty: Boolean,
    onShowEmpty: () -> Unit,
    onParent: () -> Unit,
    onField: (Key) -> Unit,
    onAddField: () -> Unit,
    onOpen: (io.github.lenticualrstone.civilight.navigation.Route) -> Unit,
    art: @Composable (String) -> Unit,
    sameName: @Composable () -> Unit,
) {
    val head = card.head ?: return
    item(key = "art") { Box(civilightItem()) { art(head.name) } }
    item(key = "head") {
        Column(civilightItem()) {
            CardHead(head.copy(description = emptyList()))
            sameName()
            CivilightTabs(tabs = card.tabs, selectedIndex = tab, onSelect = onTab, modifier = Modifier.padding(top = 12.dp))
        }
    }
    // One swap for the three tabs (the lists are short): the body crossfades and its size follows.
    item(key = "tabBody") {
        ContentSwap(tab, civilightItem().fillMaxWidth(), label = "placeTab") { shownTab ->
            when (PlaceCardUi.tabOf(shownTab)) {
                io.github.lenticualrstone.civilight.navigation.PlaceTab.OVERVIEW -> Column(Modifier.fillMaxWidth()) {
                    CardDescription(head.description, Modifier.padding(top = 6.dp))
                    Column(Modifier.padding(top = 14.dp)) {
                        card.appearance?.let { FixedRow(it) {} }
                        card.parent?.let { FixedRow(it, onParent) }
                        card.assets?.let { FixedRow(it) {} }
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
                io.github.lenticualrstone.civilight.navigation.PlaceTab.TIME_HERE -> Column(Modifier.fillMaxWidth()) {
                    card.timeRows.forEach { row ->
                        when (row) {
                            is PlaceTimeItemUi.Row -> PlaceTimeRowView(row, row.target?.let { { onOpen(it) } })
                            is PlaceTimeItemUi.Divider -> PlaceTimeDivider(row.text)
                        }
                    }
                }
                io.github.lenticualrstone.civilight.navigation.PlaceTab.MAP -> Column(Modifier.fillMaxWidth()) {
                    card.maps.forEach { row ->
                        ListRow(
                            title = row.title,
                            subtitle = row.sub,
                            onClick = row.target?.let { { onOpen(it) } },
                        )
                    }
                }
            }
        }
    }
}

/** `1195 | 대정전` over `세계 사건 · …` (board `PlaceTime`): mono time label 64 wide, title, sub. */
@Composable
private fun PlaceTimeRowView(row: PlaceTimeItemUi.Row, onOpen: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (onOpen != null) Modifier.pressable(PressAction.Click(onOpen), role = Role.Button, scaleOnPress = false) else Modifier)
            .heightIn(min = 44.dp)
            .dimmed(row.dimmed)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CivilightText(row.time, type.number, Modifier.width(64.dp), color = colors.ink3)
        Column(Modifier.weight(1f)) {
            CivilightText(row.title, type.itemTitle, color = colors.ink)
            if (row.sub.isNotEmpty()) CivilightText(row.sub, type.small, color = colors.ink3)
        }
    }
}

/** `시점 1-7 · 이 아래는 아직` in `acc` over a dashed rule. */
@Composable
private fun PlaceTimeDivider(text: String) {
    val colors = CivilightTheme.colors
    CivilightText(
        text,
        CivilightTheme.type.small,
        Modifier
            .fillMaxWidth()
            .topHairline(colors.acc, dashed = true)
            .padding(vertical = 8.dp),
        color = colors.acc,
    )
}

@Composable
private fun SheetScope.ParentPlaceContent(placeKey: Key, doc: ProjectDoc, gate: HiddenGate, readOnly: Boolean, vm: PlaceCardViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val excluded = remember(doc, placeKey) { ParentChoices.excluded(doc, placeKey) }
    val model = remember(doc, query, gate.open, excluded) {
        val all = PickerModels.places(doc, query, gate.open, includeRegions = false)
        all.copy(sections = all.sections.map { s -> s.copy(items = s.items.filter { it.key !in excluded }) }.filter { it.items.isNotEmpty() })
    }
    val current = doc.index.place(placeKey)?.parentPlaceKey
    val scope = rememberCoroutineScope()
    val sheet = this
    ItemPickerSheet(
        title = WorldCopy.ROW_PARENT,
        model = model,
        query = query,
        onQueryChange = { query = it },
        onPick = { item ->
            if (!readOnly) scope.launch { if (vm.setParent(item.key) == EditResult.APPLIED) sheet.close() }
        },
        onClose = ::close,
        selectedKey = current,
        onUnlockHidden = gate.unlock,
    )
    if (current != null && !readOnly) {
        SheetActions {
            CivilightButton(
                text = WorldCopy.BTN_CLEAR,
                onClick = { scope.launch { if (vm.setParent(null) == EditResult.APPLIED) sheet.close() } },
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.GHOST,
            )
        }
    }
}
