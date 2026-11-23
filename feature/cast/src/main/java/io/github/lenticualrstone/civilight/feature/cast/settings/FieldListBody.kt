package io.github.lenticualrstone.civilight.feature.cast.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettingRow
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettingsScreen
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key

/**
 * The stateless field list of `인물 항목` (CST-19), shared by the project screen and `템플릿 고치기`
 * (CST-59): kind tabs, the `고정 항목` box, [afterFixed] (the project's header switches), the field
 * rows with grip drag and the `순서 바꾸기` mode (CST-54), `+ 항목`, then [trailing] rows. Order mode
 * ends when the tab changes or the screen is left.
 */
@Composable
internal fun FieldListBody(
    screen: FieldSettingsScreen,
    kindIndex: Int,
    onKindIndex: (Int) -> Unit,
    sectionTitle: String,
    editable: Boolean,
    padding: PaddingValues,
    onMove: (key: Key, from: Int, to: Int) -> Unit,
    onForward: (Key) -> Unit,
    onBackward: (Key) -> Unit,
    onRowClick: (Key) -> Unit,
    onAdd: (() -> Unit)?,
    modifier: Modifier = Modifier,
    afterFixed: LazyListScope.() -> Unit = {},
    trailing: LazyListScope.() -> Unit = {},
    header: LazyListScope.() -> Unit = {},
) {
    // Ends with the tab (keyed) and with the screen (state of this entry only).
    var orderMode by rememberSaveable(kindIndex) { mutableStateOf(false) }
    val ordering = orderMode && editable
    val listState = rememberLazyListState()
    val reorder = rememberFieldReorder(screen.rows.map { it.key }, listState) { key, from, to -> onMove(key, from, to) }
    val shownRows = remember(screen.rows, reorder.order) {
        val byKey = screen.rows.associateBy { it.key }
        (reorder.order ?: screen.rows.map { it.key }).mapNotNull { byKey[it] }
    }
    val orderRows = remember(screen) { FieldSettingsModel.orderRows(screen).associateBy { it.key } }
    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        header()
        item(key = "tabs") { CivilightTabs(screen.tabs, kindIndex, onKindIndex, civilightItem()) }
        item(key = "fixed-head") { SectionLabel(screen.fixedTitle, civilightItem().padding(top = 16.dp)) }
        item(key = "fixed") {
            CivilightText(
                CastCopy.joinAll(screen.fixedFields),
                CivilightTheme.type.small,
                civilightItem()
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CivilightTheme.colors.tint)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                color = CivilightTheme.colors.ink3,
            )
        }
        afterFixed()
        item(key = "project-head") {
            Column(civilightItem().padding(top = 20.dp, bottom = 6.dp)) {
                SectionLabel(
                    sectionTitle,
                    trailing = when {
                        !editable || screen.rows.isEmpty() -> null
                        ordering -> FieldOrderCopy.BTN_DONE
                        else -> screen.reorderAction
                    },
                    onTrailing = { orderMode = !orderMode },
                )
                if (ordering) CivilightText(screen.orderModeNote, CivilightTheme.type.small, Modifier.padding(top = 2.dp), color = CivilightTheme.colors.ink3)
            }
        }
        items(shownRows, key = { it.key }) { row ->
            val dragged = reorder.dragKey == row.key
            val order = orderRows[row.key]
            FieldSettingRowView(
                row = row,
                modifier = Modifier
                    // The dragged row is placed by the finger; its neighbours glide aside (P1).
                    .then(civilightItem(placement = if (dragged) null else CivilightMotion.standard))
                    .onSizeChanged { reorder.rowHeight = it.height.toFloat() }
                    .zIndex(if (dragged) 1f else 0f)
                    .graphicsLayer { translationY = if (dragged) reorder.offset else 0f },
                grip = if (!editable) {
                    Modifier
                } else {
                    Modifier
                        .semantics {
                            // CST-54: the same moves as the buttons, for anyone who cannot drag.
                            customActions = listOfNotNull(
                                order?.takeIf { it.canForward }?.let { CustomAccessibilityAction(FieldOrderCopy.BTN_FORWARD) { onForward(row.key); true } },
                                order?.takeIf { it.canBackward }?.let { CustomAccessibilityAction(FieldOrderCopy.BTN_BACKWARD) { onBackward(row.key); true } },
                            )
                        }
                        .pointerInput(row.key) {
                            detectVerticalDragGestures(
                                onDragStart = { reorder.start(row.key) },
                                onDragEnd = { reorder.release() },
                                onDragCancel = { reorder.cancel() },
                                onVerticalDrag = { change, delta ->
                                    change.consume()
                                    reorder.drag(delta)
                                },
                            )
                        }
                },
                order = if (ordering) order else null,
                onForward = { onForward(row.key) },
                onBackward = { onBackward(row.key) },
                onClick = if (ordering) null else ({ onRowClick(row.key) }),
            )
        }
        if (!ordering) {
            if (editable && onAdd != null) item(key = "add") { AddFieldRow(civilightItem(), onAdd) }
            trailing()
        }
    }
}

/** `출생 · 날짜 · 연도 · 머리`: grip, name 15 bold, sub with the type in `ink2`, chevron; in order mode `3/7` · `앞으로` · `뒤로`. */
@Composable
private fun FieldSettingRowView(
    row: FieldSettingRow,
    modifier: Modifier,
    grip: Modifier,
    order: OrderRow?,
    onForward: () -> Unit,
    onBackward: () -> Unit,
    onClick: (() -> Unit)?,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val typePart = row.sub.substringBefore(CastCopy.SEP)
    val rest = row.sub.removePrefix(typePart)
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .heightIn(min = 44.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            grip
                .size(44.dp)
                .semantics { contentDescription = row.gripLabel },
            contentAlignment = Alignment.Center,
        ) {
            CivilightIcon(IconId.Grip, contentDescription = null, tint = colors.ink3)
        }
        Column(
            Modifier
                .weight(1f)
                .then(if (onClick != null) Modifier.pressable(onClick = onClick, scaleOnPress = false, onClickLabel = row.name) else Modifier),
        ) {
            CivilightText(row.name, type.itemTitleCompact, color = if (row.hidden) colors.ink3 else colors.ink, maxLines = 1)
            val sub = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.ink2)) { append(typePart) }
                append(rest)
            }
            androidx.compose.material3.Text(sub, style = type.small, color = colors.ink3, maxLines = 1)
        }
        if (order != null) {
            CivilightText(order.position, type.number, color = colors.ink3, maxLines = 1)
            OrderButton(FieldOrderCopy.BTN_FORWARD, order.canForward, onForward)
            OrderButton(FieldOrderCopy.BTN_BACKWARD, order.canBackward, onBackward)
        } else {
            CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3)
        }
    }
}

/** `앞으로` · `뒤로`: a text button with a 44 hit, dimmed and inert at the ends. */
@Composable
private fun OrderButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Box(
        Modifier
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.BUTTON, scaleOnPress = false, onClickLabel = text)
            .heightIn(min = 44.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(text, CivilightTheme.type.label, color = if (enabled) colors.ink2 else colors.ink3.copy(alpha = 0.5f), maxLines = 1)
    }
}

/** `+ 항목` (plus icon, `항목` 600 14). */
@Composable
internal fun AddFieldRow(modifier: Modifier, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = onClick, scaleOnPress = false, onClickLabel = CastUiCopy.ADD_FIELD_TITLE)
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, tint = colors.ink2)
        CivilightText(CastUiCopy.ADD_FIELD_ROW, CivilightTheme.type.label, Modifier.weight(1f), color = colors.ink)
    }
}
