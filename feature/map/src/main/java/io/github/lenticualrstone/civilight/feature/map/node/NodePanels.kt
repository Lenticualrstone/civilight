package io.github.lenticualrstone.civilight.feature.map.node

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.CardHeader
import io.github.lenticualrstone.civilight.feature.map.common.CardRow
import io.github.lenticualrstone.civilight.model.Key

/** What the node property card asks for; the screen opens sheets and calls the view model. */
interface NodeActions {
    fun deselect()
    fun editNode(nodeKey: Key)
    fun openPlace(nodeKey: Key)
    fun openPlaceCard(placeKey: Key)
    fun startMove(nodeKey: Key)
    fun startLink(nodeKey: Key)
    fun openReveal(key: Key)
    fun openLinkLabel(linkKey: Key)
    fun openLabelText(labelKey: Key)
    fun openLayer(key: Key)
}

/**
 * `속성` of the selection on a node map (inner side panel; cover bottom card): a node with `이름`,
 * its sub line, `장소`, `보이는 시점` and the buttons `이동 · 연결` (and `장소 카드` with a place); a
 * connection with `라벨` and `보이는 시점`; a label with `라벨`, `보이는 시점` and `레이어`.
 */
@Composable
fun ColumnScope.NodePropsPanel(props: NodePropsUi, readOnly: Boolean, actions: NodeActions) {
    val edit: ((() -> Unit) -> (() -> Unit)?) = { f -> if (readOnly) null else f }
    when (props) {
        is NodePropsUi.NodeProps -> {
            CardHeader(props.title, actions::deselect)
            CardRow(MapCopy.NAME, props.title, onClick = edit { actions.editNode(props.key) })
            CardRow(MapCopy.TODO_COPY_MAP_NODE_SUB, props.sub, onClick = edit { actions.editNode(props.key) })
            CardRow(MapCopy.PLACE, props.place, onClick = edit { actions.openPlace(props.key) })
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = edit { actions.openReveal(props.key) })
            ButtonRow(Modifier.padding(top = 4.dp)) {
                CivilightButton(MapCopy.TOOL_MOVE, { actions.startMove(props.key) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = !readOnly)
                CivilightButton(MapCopy.TOOL_LINK, { actions.startLink(props.key) }, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = !readOnly)
                props.placeKey?.let { place ->
                    CivilightButton(MapCopy.BTN_PLACE_CARD, { actions.openPlaceCard(place) }, Modifier.weight(1f))
                }
            }
        }
        is NodePropsUi.LinkProps -> {
            CardHeader(MapCopy.TOOL_LINK, actions::deselect)
            CivilightText(props.title, CivilightTheme.type.itemTitle, maxLines = 1)
            CardRow(MapCopy.LABEL, props.label, onClick = edit { actions.openLinkLabel(props.key) })
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = edit { actions.openReveal(props.key) })
        }
        is NodePropsUi.LabelProps -> {
            CardHeader(props.title, actions::deselect)
            CardRow(MapCopy.LABEL, props.title, onClick = edit { actions.openLabelText(props.key) })
            CardRow(MapCopy.REVEAL_POINT, props.reveal, onClick = edit { actions.openReveal(props.key) })
            CardRow(MapCopy.LAYER, props.layer, onClick = edit { actions.openLayer(props.key) })
        }
    }
}

/**
 * The bar of a two-step action: `연결 · 시장 입구` while the second node is chosen, `이동 · 시장 입구`
 * while the next tap places the node; `취소` leaves it.
 */
@Composable
fun NodeStepBar(text: String, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CivilightText(text, CivilightTheme.type.label, Modifier.weight(1f), maxLines = 1)
        CivilightButton(MapCopy.BTN_CANCEL, onCancel, variant = ButtonVariant.COMPACT_GHOST)
    }
}
