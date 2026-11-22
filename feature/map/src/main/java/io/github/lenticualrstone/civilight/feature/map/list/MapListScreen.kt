package io.github.lenticualrstone.civilight.feature.map.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapCanvas
import io.github.lenticualrstone.civilight.feature.map.canvas.Viewport
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapNav
import io.github.lenticualrstone.civilight.feature.map.common.mapBack
import io.github.lenticualrstone.civilight.navigation.MapList
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost

/**
 * 세계 › 지도 (WLD-36, board `MapNew`): header `<project> · 세계` / `지도` / `+`, section `지도 · N`,
 * a row of map cards (200 × 150 thumbnail, name, small line), and the `새 지도` sheet
 * ([rememberNewMapFlow]: system pickers and camera, WLD-38, WLD-39; `펴기` is `SketchStraighten`).
 */
@Composable
fun MapListScreen(route: MapList, deps: FeatureDeps) {
    val vm = entryViewModel { MapListViewModel(route.projectKey, MapEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val newMap = rememberNewMapFlow(route.projectKey, deps, vm, sheets)

    CivilightScreen(
        title = MapCopy.LIST_TITLE,
        smallLine = ui?.small,
        back = mapBack(deps),
        actions = {
            CivilightIconButton(IconId.Plus, MapCopy.NEW_MAP, { newMap.open() }, enabled = !readOnly)
        },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is NewMapSheet -> { { newMap.content(this, key) } }
                else -> null
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
            val list = ui ?: return@Column
            SectionLabel(list.section, Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp))
            if (list.cards.isEmpty()) {
                EmptyBlock(
                    title = list.section,
                    modifier = Modifier.padding(top = 48.dp),
                    primaryLabel = if (readOnly) null else MapCopy.NEW_MAP,
                    onPrimary = if (readOnly) null else { { newMap.open() } },
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(list.cards, key = { it.key }) { card ->
                        MapCard(card) { MapNav.open(deps.navigator, doc, route.projectKey, card.key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapCard(card: MapCardUi, onOpen: () -> Unit) {
    val type = CivilightTheme.type
    Column(Modifier.width(200.dp).pressable(onClick = onOpen, role = Role.Button)) {
        Box(Modifier.size(200.dp, 150.dp).clip(RoundedCornerShape(16.dp))) {
            MapCanvas(base = card.base, scene = card.scene, viewport = Viewport(), onViewportChange = {}, interactive = false, showRouteChips = false)
        }
        CivilightText(card.name, type.itemTitle, Modifier.padding(top = 8.dp), maxLines = 1)
        CivilightText(card.line, type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
    }
}
