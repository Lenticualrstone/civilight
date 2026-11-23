package io.github.lenticualrstone.civilight.feature.map.list

import io.github.lenticualrstone.civilight.designsystem.component.CreateRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.SnapCardRow
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
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
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is NewMapSheet -> { { newMap.content(this, key) } }
                else -> null
            }
        },
    ) { padding ->
        val inner = LocalWindowLayout.current.isInner
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
            // (0.2.0, WLD-75, U-15.1) The list's first row `＋ 새 지도` (class b) replaces the head `+`; it opens the
            // image picker at once. Not drawn in a read-only project (U-15.10).
            if (!readOnly) CreateRow(MapCopy.NEW_MAP, { newMap.open() }, Modifier.padding(horizontal = 8.dp))
            // Loading, empty and filled swap with a crossfade; only a change of kind animates, so adding a map does not.
            ContentSwap(ui, contentKey = { list -> if (list == null) "loading" else if (list.cards.isEmpty()) "empty" else "cards" }) { list ->
                when {
                    list == null -> Unit
                    list.cards.isEmpty() -> EmptyBlock(
                        // The block shows the heading (`지도 · 0`) once; a second section label above it repeated it (QA 8).
                        title = list.section,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp),
                    )
                    else -> SnapCardRow(
                        items = list.cards,
                        key = { it.key },
                        kind = CardKind.RECENT,
                        inner = inner,
                        title = list.section,
                        showPager = false,
                        modifier = Modifier.padding(top = 4.dp),
                    ) { card, imageModifier ->
                        MapCard(card, imageModifier) { MapNav.open(deps.navigator, doc, route.projectKey, card.key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapCard(card: MapCardUi, imageModifier: Modifier, onOpen: () -> Unit) {
    val type = CivilightTheme.type
    Column(Modifier.width(200.dp).pressable(onClick = onOpen, role = Role.Button)) {
        Box(Modifier.size(200.dp, 150.dp).clip(RoundedCornerShape(16.dp))) {
            MapCanvas(base = card.base, scene = card.scene, viewport = Viewport(), onViewportChange = {}, modifier = imageModifier, interactive = false, showRouteChips = false)
        }
        CivilightText(card.name, type.itemTitle, Modifier.padding(top = 8.dp), maxLines = 1)
        CivilightText(card.line, type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
    }
}
