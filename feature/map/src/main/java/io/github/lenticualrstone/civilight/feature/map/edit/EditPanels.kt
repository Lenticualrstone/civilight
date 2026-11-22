package io.github.lenticualrstone.civilight.feature.map.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import androidx.compose.ui.draw.rotate
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapColors
import io.github.lenticualrstone.civilight.feature.map.canvas.regionColor
import io.github.lenticualrstone.civilight.feature.map.common.CardHeader
import io.github.lenticualrstone.civilight.feature.map.common.CardRow
import io.github.lenticualrstone.civilight.model.Key

/** What the property panels ask for; the screen opens sheets and calls the view model. */
interface PanelActions {
    fun deselect()
    fun openReveal(target: Key?)
    fun openLayer(target: Key?)
    fun openFaction(target: Key?)
    fun openKind(target: Key?)
    fun openName(target: Key?)
    fun openLabelText(labelKey: Key)
    fun setColor(target: Key?, token: String)
    fun undoVertex()
    fun closeRegion()
    fun undoRoutePoint()
    fun finishRoute()
    /** A mode chip by its index in `Travel.modes` order; null = `직접` (the mode editor). */
    fun setRouteMode(routeKey: Key, index: Int?)
    fun setScale()
    fun movePin(pinKey: Key)
    fun openPlaceCard(placeKey: Key)
    fun setCurve(labelKey: Key, on: Boolean)
    fun openChildMap(mapKey: Key)

    /** `하위 지도 · 만들기`: the `새 지도` sheet with this pin as the parent. */
    fun createChildMap(pinKey: Key)

    /** `하위 지도 · 고르기`: an existing map becomes this pin's child map. */
    fun pickChildMap(pinKey: Key)

    // --- WLD-60 detailed terrain (no-ops where the screen has none) ---
    fun setStampSize(stampKey: Key, size: Int) {}
    fun openStampPlace(stampKey: Key) {}
    fun moveStamp() {}
    fun undoLinePoint() {}
    fun finishLine() {}

    // --- WLD-65 ---
    fun toggleCompare(index: Int) {}
}

/** What the `레이어` tab asks for (WLD-45). */
interface LayerActions {
    fun setVisible(layerKey: Key, visible: Boolean)
    fun rename(layerKey: Key)
    fun reveal(layerKey: Key)
    fun move(layerKey: Key, up: Boolean)
    fun add()
}

/** `속성` of the selection or the draft (inner side panel tab; cover bottom card). */
@Composable
fun ColumnScope.PropsPanel(props: PropsUi, readOnly: Boolean, actions: PanelActions, compareIndex: Int? = null) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    when (props) {
        is PropsUi.PinProps -> {
            CardHeader(props.title, actions::deselect)
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = { actions.openReveal(props.key) })
            CardRow(MapCopy.LAYER, props.layer, onClick = { actions.openLayer(props.key) })
            props.childMaps.forEach { (key, name) -> CardRow(MapCopy.CHILD_MAP, name, onClick = { actions.openChildMap(key) }) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText(MapCopy.CHILD_MAP, type.label, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
                ButtonRow {
                    CivilightButton(MapCopy.BTN_CHILD_CREATE, { actions.createChildMap(props.key) }, variant = ButtonVariant.COMPACT_GHOST, enabled = !readOnly)
                    CivilightButton(MapCopy.BTN_CHILD_PICK, { actions.pickChildMap(props.key) }, variant = ButtonVariant.COMPACT_GHOST, enabled = !readOnly)
                }
            }
            ButtonRow(Modifier.padding(top = 4.dp)) {
                CivilightButton(MapCopy.BTN_MOVE_PIN, { actions.movePin(props.key) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = !readOnly)
                CivilightButton(MapCopy.BTN_PLACE_CARD, { actions.openPlaceCard(props.placeKey) }, Modifier.weight(1f))
            }
        }
        is PropsUi.RegionDraftProps -> {
            CivilightText(props.title, type.sec, color = colors.ink3)
            CardRow(MapCopy.REGION_NAME, props.name.ifEmpty { MapCopy.NONE }, onClick = { actions.openName(null) })
            CardRow(MapCopy.KIND, props.kind, onClick = { actions.openKind(null) })
            CardRow(MapCopy.LINK, props.faction, onClick = { actions.openFaction(null) })
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = { actions.openReveal(null) })
            CardRow(MapCopy.LAYER, props.layer, onClick = { actions.openLayer(null) })
            ColorRow(props.color) { actions.setColor(null, it) }
            VertexBar(props.vertexBar, props.canClose, actions)
        }
        is PropsUi.RegionProps -> {
            CardHeader(props.title, actions::deselect)
            CardRow(MapCopy.REGION_NAME, props.title, onClick = { actions.openName(props.key) })
            CardRow(MapCopy.KIND, props.kind, onClick = { actions.openKind(props.key) })
            CardRow(MapCopy.LINK, props.faction, onClick = { actions.openFaction(props.key) })
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = { actions.openReveal(props.key) })
            CardRow(MapCopy.LAYER, props.layer, onClick = { actions.openLayer(props.key) })
            ColorRow(props.color) { actions.setColor(props.key, it) }
            if (props.shapes.isNotEmpty()) {
                // WLD-65: the shape has `◆` history; each chip draws that shape over the one in force.
                CardRow(MapCopy.SHAPE, props.shapes.joinToString(MapCopy.SEP) { it.first })
                ChipFlow {
                    props.shapes.forEachIndexed { i, (label, _) -> FilterChip("$label${MapCopy.SEP}${MapCopy.COMPARE}", compareIndex == i, { actions.toggleCompare(i) }) }
                }
            }
        }
        is PropsUi.StampProps -> {
            CardHeader(props.title, actions::deselect)
            CivilightText(props.data.chip, type.small, color = colors.ink3, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CivilightText(MapCopy.STAMP_SIZE, type.label, color = colors.ink3)
                SegmentedControl(MapCopy.STAMP_SIZES, props.data.size - 1, { if (!readOnly) actions.setStampSize(props.data.key, it + 1) }, Modifier.weight(1f))
            }
            CardRow(MapCopy.PLACE_LINK, props.data.place, onClick = if (readOnly) null else ({ actions.openStampPlace(props.data.key) }))
            CardRow(MapCopy.REVEAL_POINT, props.data.reveal, onClick = { actions.openReveal(props.data.key) })
            CardRow(MapCopy.LAYER, props.data.layer, onClick = { actions.openLayer(props.data.key) })
            CivilightButton(MapCopy.TOOL_MOVE, actions::moveStamp, Modifier.fillMaxWidth(), variant = ButtonVariant.GHOST, enabled = !readOnly)
        }
        is PropsUi.LineProps -> {
            CardHeader(props.title, actions::deselect)
            CivilightText(MapCopy.routeDraftBar(props.points.size, null), type.small, color = colors.ink3)
        }
        is PropsUi.LineDraftProps -> {
            CivilightText(props.title, type.sec, color = colors.ink3)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText(props.bar, type.label, Modifier.weight(1f), maxLines = 1)
                ButtonRow {
                    CivilightButton(MapCopy.BTN_UNDO, actions::undoLinePoint, variant = ButtonVariant.COMPACT_GHOST)
                    CivilightButton(MapCopy.BTN_DONE, actions::finishLine, variant = ButtonVariant.COMPACT_PRIMARY, enabled = props.canFinish && !readOnly)
                }
            }
        }
        is PropsUi.RouteDraftProps -> {
            CivilightText(props.title, type.sec, color = colors.ink3)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText(props.bar, type.label, Modifier.weight(1f), maxLines = 1)
                ButtonRow {
                    CivilightButton(MapCopy.BTN_UNDO, actions::undoRoutePoint, variant = ButtonVariant.COMPACT_GHOST)
                    CivilightButton(MapCopy.BTN_DONE, actions::finishRoute, variant = ButtonVariant.COMPACT_PRIMARY, enabled = props.canFinish)
                }
            }
        }
        is PropsUi.RouteProps -> {
            CardHeader(MapCopy.ROUTE, actions::deselect)
            CivilightText(props.title, type.itemTitle, maxLines = 1)
            props.sheet.duration?.let { CivilightText(it, type.stat) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText(props.sheet.detail, type.small, Modifier.weight(1f), color = colors.ink3)
                props.sheet.action?.let { CivilightButton(it, actions::setScale, variant = ButtonVariant.COMPACT_GHOST) }
            }
            ChipFlow {
                props.modes.forEach { m -> FilterChip(m.label, m.selected, { actions.setRouteMode(props.key, m.index) }, enabled = !readOnly || m.index == null) }
            }
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = { actions.openReveal(props.key) })
            CardRow(MapCopy.LAYER, props.layer, onClick = { actions.openLayer(props.key) })
        }
        is PropsUi.LabelProps -> {
            CardHeader(props.title, actions::deselect)
            CardRow(MapCopy.LABEL, props.title, onClick = { actions.openLabelText(props.key) })
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = { actions.openReveal(props.key) })
            CardRow(MapCopy.LAYER, props.layer, onClick = { actions.openLayer(props.key) })
            if (props.canCurve) {
                SwitchRow(MapCopy.CURVE_LABEL, props.curve != null, { actions.setCurve(props.key, it) }, enabled = !readOnly)
            }
        }
    }
}

/** `꼭짓점 N · 되돌리기 · 닫기` (`닫기` is a button only; no auto-close near the first point). */
@Composable
private fun VertexBar(text: String, canClose: Boolean, actions: PanelActions) {
    val count = text.substringBefore(MapCopy.SEP)
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(count, CivilightTheme.type.label, Modifier.weight(1f), maxLines = 1)
        ButtonRow {
            CivilightButton(MapCopy.BTN_VERTEX_UNDO, actions::undoVertex, variant = ButtonVariant.COMPACT_GHOST)
            CivilightButton(MapCopy.BTN_VERTEX_CLOSE, actions::closeRegion, variant = ButtonVariant.COMPACT_PRIMARY, enabled = canClose)
        }
    }
}

/** `색`: swatches from `map-art-1 … 5`. */
@Composable
internal fun ColorRow(current: String, onPick: (String) -> Unit) {
    val colors = CivilightTheme.colors
    val art = FixedColors.mapArt(if (colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CivilightText(MapCopy.COLOR, CivilightTheme.type.label, color = colors.ink3)
        MapColors.REGION_TOKENS.forEachIndexed { i, token ->
            val on = token == current
            Box(
                Modifier
                    .pressable(onClick = { onPick(token) }, role = Role.RadioButton, onClickLabel = "${MapCopy.COLOR} ${i + 1}")
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(regionColor(token, art))
                    .then(if (on) Modifier.insetOutline(colors.acc, 2.dp, CircleShape) else Modifier),
            )
        }
    }
}

/**
 * `레이어` tab (WLD-45): per layer its name (tap to rename), shown switch, `보이는 시점` and order
 * buttons; `+ 레이어` adds one. Order is by visible buttons, never drag.
 */
@Composable
fun ColumnScope.LayersPanel(layers: List<LayerRowUi>, readOnly: Boolean, actions: LayerActions) {
    val colors = CivilightTheme.colors
    if (layers.isEmpty()) CivilightText(MapCopy.NONE, CivilightTheme.type.small, color = colors.ink3)
    // A keyed lazy list, so a layer that is added, removed or moved by the order buttons glides (motion audit row 13).
    // The height is bounded because the panel sits in a scrolling column.
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = PANEL_LIST_MAX_HEIGHT)) {
        items(layers, key = { it.key }) { l ->
            Column(civilightItem().padding(bottom = 6.dp)) {
                SwitchRow(l.name, l.visible, { actions.setVisible(l.key, it) }, enabled = !readOnly)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardRow(MapCopy.NAME, l.name, Modifier.weight(1f), onClick = if (readOnly) null else ({ actions.rename(l.key) }))
                    CivilightIconButton(IconId.Back, MapCopy.LAYER_UP, { actions.move(l.key, true) }, Modifier.rotate(90f), enabled = !readOnly && l.canUp)
                    CivilightIconButton(IconId.Back, MapCopy.LAYER_DOWN, { actions.move(l.key, false) }, Modifier.rotate(-90f), enabled = !readOnly && l.canDown)
                }
                CardRow(MapCopy.REVEAL_POINT, l.reveal, onClick = if (readOnly) null else ({ actions.reveal(l.key) }))
            }
        }
        if (!readOnly) item(key = "add") { AddChip(MapCopy.LAYER, actions::add, civilightItem().padding(top = 4.dp)) }
    }
}

/** The tallest a layer or item list grows inside a panel before it scrolls on its own. */
private val PANEL_LIST_MAX_HEIGHT = 320.dp

/** `목록` tab: every overlay (`핀 · 결사의 문`), tap to select. */
@Composable
fun ColumnScope.ItemsPanel(items: List<ItemRowUi>, onSelect: (Key) -> Unit) {
    if (items.isEmpty()) CivilightText(MapCopy.NONE, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = PANEL_LIST_MAX_HEIGHT)) {
        items(items, key = { it.key }) { item -> ListRow(item.text, onClick = { onSelect(item.key) }, selected = item.selected, compact = true, modifier = civilightItem()) }
    }
}
