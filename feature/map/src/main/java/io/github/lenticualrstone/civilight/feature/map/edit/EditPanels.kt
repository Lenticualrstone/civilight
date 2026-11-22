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
    fun setRouteMode(routeKey: Key, modeKey: Key?)
    fun setScale()
    fun movePin(pinKey: Key)
    fun openPlaceCard(placeKey: Key)
    fun setCurve(labelKey: Key, on: Boolean)
}

/** `속성` of the selection or the draft (inner side panel tab; cover bottom card). */
@Composable
fun ColumnScope.PropsPanel(props: PropsUi, readOnly: Boolean, actions: PanelActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    when (props) {
        is PropsUi.PinProps -> {
            CardHeader(props.title, actions::deselect)
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = { actions.openReveal(props.key) })
            CardRow(MapCopy.LAYER, props.layer, onClick = { actions.openLayer(props.key) })
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
                props.modes.forEach { m -> FilterChip(m.label, m.selected, { actions.setRouteMode(props.key, m.modeKey) }, enabled = !readOnly || m.modeKey == null) }
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
private fun ColorRow(current: String, onPick: (String) -> Unit) {
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

/** `레이어` tab: visibility per layer (WLD-45 adds rename, order and points). */
@Composable
fun ColumnScope.LayersPanel(layers: List<LayerRowUi>, readOnly: Boolean, onVisible: (Key, Boolean) -> Unit) {
    if (layers.isEmpty()) CivilightText(MapCopy.NONE, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
    layers.forEach { l -> SwitchRow(l.name, l.visible, { onVisible(l.key, it) }, enabled = !readOnly) }
}

/** `목록` tab: every overlay (`핀 · 결사의 문`), tap to select. */
@Composable
fun ColumnScope.ItemsPanel(items: List<ItemRowUi>, onSelect: (Key) -> Unit) {
    if (items.isEmpty()) CivilightText(MapCopy.NONE, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
    Column {
        items.forEach { item -> ListRow(item.text, onClick = { onSelect(item.key) }, selected = item.selected, compact = true) }
    }
}
