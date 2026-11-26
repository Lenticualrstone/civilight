package io.github.lenticualrstone.civilight.feature.world.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.world.item.OrderList
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.DescriptionSheet
import io.github.lenticualrstone.civilight.feature.world.common.FieldSection
import io.github.lenticualrstone.civilight.feature.world.common.FixedRow
import io.github.lenticualrstone.civilight.feature.world.common.ItemMenuSheet
import io.github.lenticualrstone.civilight.feature.world.common.LensBanner
import io.github.lenticualrstone.civilight.feature.world.common.LockedCard
import io.github.lenticualrstone.civilight.feature.world.common.MergePickSheet
import io.github.lenticualrstone.civilight.feature.world.common.SameNameLine
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldGoneCard
import io.github.lenticualrstone.civilight.feature.world.common.cardPadding
import io.github.lenticualrstone.civilight.feature.world.common.dimmed
import io.github.lenticualrstone.civilight.feature.world.common.itemSheet
import io.github.lenticualrstone.civilight.feature.world.common.openFromWorld
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.AffiliationBasisSettings
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.DeleteImpactSheet
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.PlaceCard
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
import io.github.lenticualrstone.civilight.ui.reorder.ReorderBackHandler
import io.github.lenticualrstone.civilight.ui.reorder.rememberReorderState
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
 * 장소 카드 (WLD-09) on the immersive scaffold (boards `Immersive` kind place, `ImmContent` place branch,
 * `ImmPlaceRules`): hero image or colour field, title = name, subtitle = `상위 지역 X · 하위 지역 N`; header lens
 * chip and `⋯`; bottom tabs `개요 · 소속 · 등장`. No tags on places. The summary shape on the map is feature:map's.
 */
@Composable
fun PlaceCardScreen(route: PlaceCardRoute, deps: FeatureDeps) {
    val vm = entryViewModel(key = "place:${route.placeKey}") { PlaceCardViewModel(route.projectKey, route.placeKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    var tab by rememberSaveable(route.placeKey) { mutableIntStateOf(PlaceCardUi.tabIndex(route.tab)) }
    var showEmpty by rememberSaveable(route.placeKey) { mutableStateOf(false) }
    var expanded by rememberSaveable(route.placeKey) { mutableStateOf(listOf<String>()) }
    val reorder = rememberReorderState(route.placeKey)
    ReorderBackHandler(reorder)
    val scope = rememberCoroutineScope()
    val card = ui
    val open = card?.access == CardAccess.Open
    val p = route.projectKey
    val back = worldBack(route, deps)

    fun move(list: OrderList, key: Key, to: Int) {
        scope.launch { vm.move(list, key, to)?.let { label -> deps.toasts.showFor(p, label) { vm.undoLast() } } }
    }

    if (card != null && !open) {
        CenterColumn {
            CivilightScreen(
                title = card.title,
                smallLine = WorldCopy.TAB_PLACES,
                back = back,
                sheets = sheets,
            ) { padding ->
                LazyColumn(Modifier.fillMaxSize(), contentPadding = cardPadding(padding)) {
                    when (val access = card.access) {
                        is CardAccess.Locked -> item(key = "locked") { Box(civilightItem()) { LockedCard(access.text, gate.unlock) } }
                        is CardAccess.Gone -> item(key = "gone") { Box(civilightItem()) { WorldGoneCard(access, doc, vm, WorldKind.PLACE, route.placeKey, readOnly, gate) } }
                        CardAccess.Open -> Unit
                    }
                }
            }
        }
        return
    }

    WorldImmersiveFrame(
        itemKey = route.placeKey,
        title = card?.title.orEmpty(),
        subtitle = card?.subtitle,
        image = card?.image,
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is ParentPlaceSheet -> doc?.let { d -> { ParentPlaceContent(key.placeKey, d, gate, readOnly, vm) } }
                else -> itemSheet(key, doc, vm, readOnly, gate)
            }
        },
        onBack = { back?.onClick?.invoke() ?: deps.navigator.pop() },
        tabs = if (open) card!!.tabs else emptyList(),
        tab = tab,
        onTab = {
            reorder.exit()
            tab = it
        },
        onMore = if (open) ({ sheets.open(ItemMenuSheet(WorldKind.PLACE.name, route.placeKey)) }) else null,
        actions = { doc?.let { d -> WorldHeaderLens(lensChipLabel(d)) { sheets.open(LensSheet(p)) } } },
        onHeroClick = if (open && !readOnly) ({ sheets.open(io.github.lenticualrstone.civilight.feature.world.common.CardImages.pickSheet(p, WorldKind.PLACE, route.placeKey)) }) else null,
    ) {
        WorldLensBanner(doc?.let { LensBanner.of(it, readOnly) }, vm::backToLatest)
        if (card == null) return@WorldImmersiveFrame
        when (val access = card.access) {
            is CardAccess.Locked -> LockedCard(access.text, gate.unlock)
            is CardAccess.Gone -> WorldGoneCard(access, doc, vm, WorldKind.PLACE, route.placeKey, readOnly, gate)
            CardAccess.Open -> ContentSwap(tab, Modifier.fillMaxWidth(), label = "placeTab") { shownTab ->
                when (shownTab) {
                    PlaceCardUi.TAB_AFFILIATION -> {
                        val aff = card.affiliation
                        if (aff != null) {
                            AffiliationSectionView(
                                doc = doc,
                                ui = aff,
                                expanded = expanded.toSet(),
                                onExpand = { k, on -> expanded = if (on) expanded + k else expanded - k },
                                reorder = reorder,
                                editable = !readOnly,
                                actions = AffiliationActionsUi(
                                    onFaction = { f ->
                                        if (f.deleted) sheets.open(DeleteImpactSheet(p, EntityKind.FACTION, f.key))
                                        else deps.navigator.openFromWorld(FactionCard(p, f.key))
                                    },
                                    onMember = { m ->
                                        if (m.deleted) sheets.open(DeleteImpactSheet(p, EntityKind.CHARACTER, m.key))
                                        else deps.navigator.push(CharacterProfile(p, m.key))
                                    },
                                    onLoose = { m ->
                                        if (m.deleted) sheets.open(DeleteImpactSheet(p, EntityKind.CHARACTER, m.key))
                                        else deps.navigator.push(CharacterProfile(p, m.key))
                                    },
                                    onIncludeSubPlaces = vm::setIncludeSubPlaces,
                                    onPickBasis = { deps.navigator.push(AffiliationBasisSettings(p)) },
                                    onUnlock = gate.unlock,
                                    onMoveFaction = { k, to -> move(OrderList.Factions(route.placeKey), k, to) },
                                    onMoveMember = { f, k, to -> move(OrderList.Members(f), k, to) },
                                    onMoveLoose = { k, to -> move(OrderList.Loose(route.placeKey), k, to) },
                                ),
                            )
                        }
                    }
                    PlaceCardUi.TAB_APPEARANCE -> AppearanceTab(card) { deps.navigator.openFromWorld(it) }
                    else -> OverviewTab(
                        card = card,
                        doc = doc,
                        readOnly = readOnly,
                        showEmpty = showEmpty,
                        onShowEmpty = { showEmpty = true },
                        onDescription = { sheets.open(DescriptionSheet(WorldKind.PLACE.name, route.placeKey)) },
                        onParent = { sheets.open(ParentPlaceSheet(route.placeKey)) },
                        onField = { def -> sheets.open(FieldValueEditSheet(p, EntityKind.PLACE, route.placeKey, def)) },
                        onAddField = { sheets.open(AddFieldSheet(p, EntityKind.PLACE, route.placeKey)) },
                        onSubPlace = { deps.navigator.openFromWorld(PlaceCard(p, it)) },
                        onUnlock = gate.unlock,
                        hiddenOpen = gate.open,
                        onMerge = { sheets.open(MergePickSheet(WorldKind.PLACE.name, route.placeKey)) },
                    )
                }
            }
        }
    }
}

/** `개요` (board `ImmContent` place branch): `설명`, `세부 항목 · N` with `지역` and `+ 항목`, `하위 지역 · N` chips. */
@Composable
private fun OverviewTab(
    card: PlaceCardUi,
    doc: ProjectDoc?,
    readOnly: Boolean,
    showEmpty: Boolean,
    onShowEmpty: () -> Unit,
    onDescription: () -> Unit,
    onParent: () -> Unit,
    onField: (Key) -> Unit,
    onAddField: () -> Unit,
    onSubPlace: (Key) -> Unit,
    onUnlock: () -> Unit,
    hiddenOpen: Boolean,
    onMerge: () -> Unit,
) {
    val head = card.head ?: return
    val colors = CivilightTheme.colors
    Column(Modifier.fillMaxWidth()) {
        WorldSection(
            PlaceCopy.SEC_DESCRIPTION,
            first = true,
            trailing = if (readOnly || card.descriptionLocked) null else WorldCopy.EDIT_DESCRIPTION,
            onTrailing = onDescription,
        )
        if (head.description.isNotEmpty()) {
            LeadDescription(head.description)
        } else {
            CivilightText(WorldCopy.EMPTY_VALUE, CivilightTheme.type.lead, color = colors.ink3)
        }
        SameNameLine(doc, WorldKind.PLACE, card.placeKey, readOnly, hiddenOpen, onMerge)
        WorldSection(PlaceCopy.fieldsTitle(card.fields.filled.size))
        card.parent?.let { FixedRow(it, onParent) }
        FieldSection(
            section = card.fields,
            showEmpty = showEmpty,
            readOnly = readOnly,
            onShowEmpty = onShowEmpty,
            onEdit = { onField(it.fieldDefKey) },
            onAdd = onAddField,
        )
        if (card.subPlaces.isNotEmpty() || card.hiddenSubPlaces > 0) {
            WorldSection(PlaceCopy.subPlacesTitle(card.subPlaces.size))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                card.subPlaces.forEach { sub -> FilterChip(label = sub.name, selected = false, onClick = { onSubPlace(sub.key) }) }
                if (card.hiddenSubPlaces > 0) {
                    FilterChip(label = WorldCopy.hiddenRow(WorldKind.PLACE, card.hiddenSubPlaces), selected = false, onClick = onUnlock)
                }
            }
        }
    }
}

/** `등장`: `등장 · 배경 에셋` rows, `이곳의 시간` and the maps holding a pin of the place. */
@Composable
private fun AppearanceTab(card: PlaceCardUi, onOpen: (io.github.lenticualrstone.civilight.navigation.Route) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        card.appearance?.let { FixedRow(it) {} }
        card.assets?.let { FixedRow(it) {} }
        if (card.timeRows.isNotEmpty()) {
            WorldSection(PlaceCopy.SEC_TIME_HERE)
            card.timeRows.forEach { row ->
                when (row) {
                    is PlaceTimeItemUi.Row -> PlaceTimeRowView(row, row.target?.let { { onOpen(it) } })
                    is PlaceTimeItemUi.Divider -> PlaceTimeDivider(row.text)
                }
            }
        }
        if (card.maps.isNotEmpty()) {
            WorldSection(PlaceCopy.SEC_MAPS)
            card.maps.forEach { row ->
                ListRow(title = row.title, subtitle = row.sub, onClick = row.target?.let { { onOpen(it) } })
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
