package io.github.lenticualrstone.civilight.feature.world.faction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.CardArt
import io.github.lenticualrstone.civilight.feature.world.common.CardHead
import io.github.lenticualrstone.civilight.feature.world.common.FieldSection
import io.github.lenticualrstone.civilight.feature.world.common.GoneCard
import io.github.lenticualrstone.civilight.feature.world.common.ItemMenuSheet
import io.github.lenticualrstone.civilight.feature.world.common.LensBanner
import io.github.lenticualrstone.civilight.feature.world.common.lensBannerSlot
import io.github.lenticualrstone.civilight.feature.world.common.LockedCard
import io.github.lenticualrstone.civilight.feature.world.common.WorldCommandBar
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.feature.world.common.cardPadding
import io.github.lenticualrstone.civilight.feature.world.common.itemSheet
import io.github.lenticualrstone.civilight.feature.world.common.openFromWorld
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.model.Key
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import io.github.lenticualrstone.civilight.navigation.FactionCard as FactionCardRoute

/** The faction card (WLD-10). */
@OptIn(ExperimentalCoroutinesApi::class)
class FactionCardViewModel(projectKey: Key, val factionKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    val ui: StateFlow<FactionCardUi?> = combine(doc.filterNotNull(), hiddenOpen) { d, h -> d to h }
        .mapLatest { (d, h) -> FactionCardState.build(d, projectKey, factionKey, h) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * 세력 카드 (WLD-10, not drawn; the term card's layout): header `세계 · 세력` · name · lens chip ·
 * `⋯`; image slot, eyebrow `세력`, name, description, project fields (`수장 · 규모`), then
 * `이 항목으로 잇는 인물`, map areas and events.
 */
@Composable
fun FactionCardScreen(route: FactionCardRoute, deps: FeatureDeps) {
    val vm = entryViewModel { FactionCardViewModel(route.projectKey, route.factionKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    var showEmpty by rememberSaveable(route.factionKey) { mutableStateOf(false) }
    val card = ui

    CivilightScreen(
        title = card?.title.orEmpty(),
        smallLine = WorldCopy.SMALL_FACTION,
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
                    onClick = { sheets.open(ItemMenuSheet(WorldKind.FACTION.name, route.factionKey)) },
                )
            }
        },
        commandBar = {
            WorldCommandBar(deps, route.projectKey, WorldCopy.SEARCH_CARD, EntrySurface.FACTION, RecorderTargetKind.FACTION, route.factionKey)
        },
        sheets = sheets,
        localSheets = { key -> itemSheet(key, doc, vm as WorldViewModel, readOnly, gate) },
    ) { padding ->
        CenterColumn {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = cardPadding(padding)) {
                if (card == null) return@LazyColumn
                when (val access = card.access) {
                    is CardAccess.Locked -> item(key = "locked") { LockedCard(access.text, gate.unlock) }
                    is CardAccess.Gone -> item(key = "gone") { GoneCard(access.text, access.dotted) }
                    CardAccess.Open -> {
                        val head = card.head ?: return@LazyColumn
                        item(key = "art") { CardArt(head.name, label = null) }
                        item(key = "head") { CardHead(head) }
                        item(key = "fields") {
                            Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                                FieldSection(
                                    section = card.fields,
                                    showEmpty = showEmpty,
                                    readOnly = readOnly,
                                    onShowEmpty = { showEmpty = true },
                                    onEdit = { sheets.open(FieldValueEditSheet(route.projectKey, EntityKind.FACTION, route.factionKey, it.fieldDefKey)) },
                                    onAdd = { sheets.open(AddFieldSheet(route.projectKey, EntityKind.FACTION, route.factionKey)) },
                                )
                            }
                        }
                        card.sections.forEach { section ->
                            item(key = "sec:${section.id}") { SectionLabel(section.title, Modifier.padding(top = 22.dp, bottom = 2.dp)) }
                            items(section.rows, key = { it.id }) { row ->
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
