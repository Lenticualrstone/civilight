package io.github.lenticualrstone.civilight.feature.world.faction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.actions.AffiliationActions
import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.item.BaseResult
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.DescriptionSheet
import io.github.lenticualrstone.civilight.feature.world.common.FieldSection
import io.github.lenticualrstone.civilight.feature.world.common.ItemMenuSheet
import io.github.lenticualrstone.civilight.feature.world.common.LensBanner
import io.github.lenticualrstone.civilight.feature.world.common.LockedCard
import io.github.lenticualrstone.civilight.feature.world.common.MergePickSheet
import io.github.lenticualrstone.civilight.feature.world.common.SameNameLine
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldGoneCard
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.feature.world.common.cardPadding
import io.github.lenticualrstone.civilight.feature.world.common.itemSheet
import io.github.lenticualrstone.civilight.feature.world.common.openFromWorld
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.feature.world.place.LeadDescription
import io.github.lenticualrstone.civilight.feature.world.place.PersonRow
import io.github.lenticualrstone.civilight.feature.world.place.PlaceCopy
import io.github.lenticualrstone.civilight.feature.world.place.RefText
import io.github.lenticualrstone.civilight.feature.world.place.WorldHeaderLens
import io.github.lenticualrstone.civilight.feature.world.place.WorldImmersiveFrame
import io.github.lenticualrstone.civilight.feature.world.place.WorldLensBanner
import io.github.lenticualrstone.civilight.feature.world.place.WorldSection
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.AffiliationBasisSettings
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.DeleteImpactSheet
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.ItemPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.reorder.ReorderBackHandler
import io.github.lenticualrstone.civilight.ui.reorder.ReorderList
import io.github.lenticualrstone.civilight.ui.reorder.ReorderState
import io.github.lenticualrstone.civilight.ui.reorder.rememberReorderState
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.github.lenticualrstone.civilight.navigation.FactionCard as FactionCardRoute

/** `거점` of a faction: a live place, or `비우기` (feature-local). */
@Serializable
data class FactionBaseSheet(val factionKey: Key) : SheetKey

/** The faction card (WLD-10): `거점` and `소속 인물` (board `ImmPlaceRules`). */
@OptIn(ExperimentalCoroutinesApi::class)
class FactionCardViewModel(projectKey: Key, val factionKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    val ui: StateFlow<FactionCardUi?> = combine(doc.filterNotNull(), hiddenOpen, readOnly) { d, h, r -> Triple(d, h, r) }
        .mapLatest { (d, h, r) -> FactionCardState.build(d, projectKey, factionKey, h, r) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** `거점`: a live place, or none. A refusal (trashed place, trashed faction) changes nothing. */
    suspend fun setBase(placeKey: Key?): EditResult {
        val ctx = actionContext()
        return apply(AffiliationActions.UNDO_BASE) { d ->
            when (val r = AffiliationActions.setBase(d, factionKey, placeKey, ctx)) {
                is BaseResult.Done -> r.doc
                is BaseResult.Refused -> d
            }
        }
    }

    /** One move in `소속 인물` (one undo step, `Faction.memberOrder`); the undo label when something moved. */
    suspend fun moveMember(characterKey: Key, toIndex: Int): String? {
        val ctx = actionContext()
        val open = hiddenOpen.value
        var moved = false
        val r = apply(AffiliationActions.UNDO_MEMBER_ORDER) { d ->
            val lens = LensRules.effective(d, d.app.view.lens.point)
            val members = AffiliationActions.factionMembers(d, factionKey, lens, open) ?: return@apply d
            AffiliationActions.moveMember(d, members, characterKey, toIndex, ctx).also { moved = it !== d }
        }
        return AffiliationActions.UNDO_MEMBER_ORDER.takeIf { r == EditResult.APPLIED && moved }
    }
}

/**
 * 세력 카드 (WLD-10) on the immersive scaffold: hero image or colour field, title = name, subtitle `거점 <place>`;
 * header lens chip and `⋯`; tabs `개요 · 소속`. `개요`: 설명, `거점` (fixed row, place picker, `비우기`), 세부 항목, map
 * areas and events of the faction. `소속`: `소속 인물` with the shared `순서 바꾸기` rows.
 */
@Composable
fun FactionCardScreen(route: FactionCardRoute, deps: FeatureDeps) {
    val vm = entryViewModel(key = "faction:${route.factionKey}") { FactionCardViewModel(route.projectKey, route.factionKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    var showEmpty by rememberSaveable(route.factionKey) { mutableStateOf(false) }
    var tab by rememberSaveable(route.factionKey) { mutableIntStateOf(FactionCardUi.TAB_OVERVIEW) }
    val reorder = rememberReorderState(route.factionKey)
    ReorderBackHandler(reorder)
    val scope = rememberCoroutineScope()
    val card = ui
    val open = card?.access == CardAccess.Open
    val p = route.projectKey
    val back = worldBack(route, deps)

    if (card != null && !open) {
        CenterColumn {
            CivilightScreen(
                title = card.title,
                smallLine = WorldCopy.TAB_FACTIONS,
                back = back,
                sheets = sheets,
            ) { padding ->
                LazyColumn(Modifier.fillMaxSize(), contentPadding = cardPadding(padding)) {
                    when (val access = card.access) {
                        is CardAccess.Locked -> item(key = "locked") { Box(civilightItem()) { LockedCard(access.text, gate.unlock) } }
                        is CardAccess.Gone -> item(key = "gone") { Box(civilightItem()) { WorldGoneCard(access, doc, vm, WorldKind.FACTION, route.factionKey, readOnly, gate) } }
                        CardAccess.Open -> Unit
                    }
                }
            }
        }
        return
    }

    WorldImmersiveFrame(
        itemKey = route.factionKey,
        title = card?.title.orEmpty(),
        subtitle = card?.subtitle,
        image = card?.image,
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is FactionBaseSheet -> doc?.let { d -> { FactionBaseContent(key.factionKey, d, gate, readOnly, vm) } }
                else -> itemSheet(key, doc, vm as WorldViewModel, readOnly, gate)
            }
        },
        onBack = { back?.onClick?.invoke() ?: deps.navigator.pop() },
        tabs = if (open) card!!.tabs else emptyList(),
        tab = tab,
        onTab = {
            reorder.exit()
            tab = it
        },
        onMore = if (open) ({ sheets.open(ItemMenuSheet(WorldKind.FACTION.name, route.factionKey)) }) else null,
        actions = { doc?.let { d -> WorldHeaderLens(lensChipLabel(d)) { sheets.open(LensSheet(p)) } } },
        onHeroClick = if (open && !readOnly) ({ sheets.open(io.github.lenticualrstone.civilight.feature.world.common.CardImages.pickSheet(p, WorldKind.FACTION, route.factionKey)) }) else null,
    ) {
        WorldLensBanner(doc?.let { LensBanner.of(it, readOnly) }, vm::backToLatest)
        if (card == null) return@WorldImmersiveFrame
        when (val access = card.access) {
            is CardAccess.Locked -> LockedCard(access.text, gate.unlock)
            is CardAccess.Gone -> WorldGoneCard(access, doc, vm, WorldKind.FACTION, route.factionKey, readOnly, gate)
            CardAccess.Open -> ContentSwap(tab, Modifier.fillMaxWidth(), label = "factionTab") { shownTab ->
                if (shownTab == FactionCardUi.TAB_MEMBERS) {
                    MembersTab(
                        doc = doc,
                        card = card,
                        reorder = reorder,
                        editable = !readOnly,
                        onMember = { key, deleted ->
                            if (deleted) sheets.open(DeleteImpactSheet(p, EntityKind.CHARACTER, key))
                            else deps.navigator.push(CharacterProfile(p, key))
                        },
                        onMove = { key, to ->
                            scope.launch { vm.moveMember(key, to)?.let { label -> deps.toasts.showFor(p, label) { vm.undoLast() } } }
                        },
                        onPickBasis = { deps.navigator.push(AffiliationBasisSettings(p)) },
                        onUnlock = gate.unlock,
                    )
                } else {
                    Column(Modifier.fillMaxWidth()) {
                        val head = card.head
                        WorldSection(
                            PlaceCopy.SEC_DESCRIPTION,
                            first = true,
                            trailing = if (readOnly || card.descriptionLocked) null else WorldCopy.EDIT_DESCRIPTION,
                            onTrailing = { sheets.open(DescriptionSheet(WorldKind.FACTION.name, route.factionKey)) },
                        )
                        if (head != null && head.description.isNotEmpty()) {
                            LeadDescription(head.description)
                        } else {
                            CivilightText(WorldCopy.EMPTY_VALUE, CivilightTheme.type.lead, color = CivilightTheme.colors.ink3)
                        }
                        SameNameLine(doc, WorldKind.FACTION, route.factionKey, readOnly, gate.open) { sheets.open(MergePickSheet(WorldKind.FACTION.name, route.factionKey)) }
                        WorldSection(PlaceCopy.fieldsTitle(card.fields.filled.size))
                        BaseRow(
                            card = card,
                            onClick = when {
                                card.baseDeleted && card.baseKey != null -> ({ sheets.open(DeleteImpactSheet(p, EntityKind.PLACE, card.baseKey)) })
                                !readOnly -> ({ sheets.open(FactionBaseSheet(route.factionKey)) })
                                else -> null
                            },
                        )
                        FieldSection(
                            section = card.fields,
                            showEmpty = showEmpty,
                            readOnly = readOnly,
                            onShowEmpty = { showEmpty = true },
                            onEdit = { sheets.open(FieldValueEditSheet(p, EntityKind.FACTION, route.factionKey, it.fieldDefKey)) },
                            onAdd = { sheets.open(AddFieldSheet(p, EntityKind.FACTION, route.factionKey)) },
                        )
                        card.sections.forEach { section ->
                            WorldSection(section.title)
                            section.rows.forEach { row ->
                                ListRow(
                                    title = row.title,
                                    compact = true,
                                    onClick = row.target?.let { target -> { deps.navigator.openFromWorld(target) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** `거점 | 하층 거리` (a fixed row; dotted when the place is deleted, rule 20). */
@Composable
private fun BaseRow(card: FactionCardUi, onClick: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val row = card.base ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (onClick != null) Modifier.pressable(PressAction.Click(onClick), role = Role.Button, scaleOnPress = false) else Modifier)
            .heightIn(min = 44.dp)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(row.label, type.command, color = colors.ink3, maxLines = 1)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            RefText(row.value, type.command, card.baseDeleted)
        }
    }
}

/** `소속 인물` (the place card's calculation for one faction) with the shared reorder rows. */
@Composable
private fun MembersTab(
    doc: ProjectDoc?,
    card: FactionCardUi,
    reorder: ReorderState,
    editable: Boolean,
    onMember: (Key, Boolean) -> Unit,
    onMove: (Key, Int) -> Unit,
    onPickBasis: () -> Unit,
    onUnlock: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val m = card.members ?: return
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            CivilightText(PlaceCopy.SEC_MEMBERS, type.heading, Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1)
            FoldReveal((m.canReorder && editable) || reorder.ordering, horizontal = true) {
                TextAction(
                    if (reorder.ordering) Copy.Affiliation.REORDER_DONE else Copy.Affiliation.REORDER,
                    { reorder.toggle() },
                    color = if (reorder.ordering) colors.ink else colors.acc,
                )
            }
        }
        ReorderList(
            state = reorder,
            list = MEMBERS_LIST,
            keys = m.members.map { it.key },
            labels = PlaceCopy.REORDER_LABELS,
            enabled = editable,
            onMove = onMove,
        ) { key, _ ->
            val member = m.members.first { it.key == key }
            PersonRow(
                doc = doc,
                key = member.key,
                name = member.name,
                role = member.role,
                deleted = member.deleted,
                trailing = { member.multi?.let { CivilightText(it, type.micro, color = colors.ink2, maxLines = 1) } },
                onClick = if (reorder.ordering) null else ({ onMember(member.key, member.deleted) }),
                indent = false,
                divider = true,
            )
        }
        m.pickBasis?.let { label ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) { CivilightButton(label, onPickBasis, variant = ButtonVariant.GHOST) }
        }
        m.lockLine?.let { line -> LockRow(title = line, actionLabel = WorldCopy.UNLOCK, onAction = onUnlock, modifier = Modifier.padding(top = 10.dp)) }
    }
}

private const val MEMBERS_LIST = "members"

@Composable
private fun SheetScope.FactionBaseContent(factionKey: Key, doc: ProjectDoc, gate: HiddenGate, readOnly: Boolean, vm: FactionCardViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val model = remember(doc, query, gate.open) { PickerModels.places(doc, query, gate.open, includeRegions = false) }
    val current = doc.index.faction(factionKey)?.baseKey
    val scope = rememberCoroutineScope()
    val sheet = this
    ItemPickerSheet(
        title = PlaceCopy.BASE,
        model = model,
        query = query,
        onQueryChange = { query = it },
        onPick = { item ->
            if (!readOnly) scope.launch { if (vm.setBase(item.key) == EditResult.APPLIED) sheet.close() }
        },
        onClose = ::close,
        selectedKey = current,
        onUnlockHidden = gate.unlock,
    )
    if (current != null && !readOnly) {
        SheetActions {
            CivilightButton(
                text = PlaceCopy.BTN_CLEAR,
                onClick = { scope.launch { if (vm.setBase(null) == EditResult.APPLIED) sheet.close() } },
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.GHOST,
            )
        }
    }
}
