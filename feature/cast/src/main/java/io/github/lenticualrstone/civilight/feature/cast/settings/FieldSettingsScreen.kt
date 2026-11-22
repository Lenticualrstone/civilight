package io.github.lenticualrstone.civilight.feature.cast.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTabs
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettingRow
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.navigation.FieldDefEdit
import io.github.lenticualrstone.civilight.navigation.FieldSettings as FieldSettingsRoute
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/** `프로젝트 설정 › 인물 항목` (CST-19): reorder writes go through here; adding is the shared sheet. */
class FieldSettingsViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
) : CastViewModel(projectKey, sessions, contextOf) {
    fun move(kind: EntityKind, defKey: Key, from: Int, to: Int) {
        if (from == to) return
        act(CastUiCopy.REORDER) { doc, ctx -> FieldSettingsModel.move(doc, kind, defKey, from, to, ctx) }
    }

    fun showInHeader(which: HeaderFixed, show: Boolean) {
        act(which.label) { doc, ctx -> FieldSettingsModel.setShowInHeader(doc, which, show, ctx) }
    }
}

/** `RCFields`: kind tabs, fixed fields, project fields with grip handles, `+ 항목`, the start bundle row. */
@Composable
fun FieldSettingsScreen(route: FieldSettingsRoute, deps: FeatureDeps) {
    val vm = entryViewModel { FieldSettingsViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    var kindIndex by rememberSaveable { mutableIntStateOf(FIELD_KINDS.indexOf(route.kind).coerceAtLeast(0)) }
    val kind = FIELD_KINDS[kindIndex]
    val screen = remember(ready.doc, kind) { FieldSettingsModel.screen(ready.doc, kind) }
    val sheets = rememberSheetHost()
    val editable = !ready.readOnly
    CenterColumn {
        CivilightScreen(
            title = screen.title,
            smallLine = screen.head,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is BundleSheet -> { { BundleSheetContent(key, ready.doc, ready.readOnly) { label, change -> vm.act(label) { d, ctx -> change(d, ctx) } } } }
                    else -> null
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                CivilightTabs(screen.tabs, kindIndex, { kindIndex = it })
                SectionLabel(screen.fixedTitle, Modifier.padding(top = 16.dp))
                CivilightText(
                    CastCopy.joinAll(screen.fixedFields),
                    CivilightTheme.type.small,
                    Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CivilightTheme.colors.tint)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    color = CivilightTheme.colors.ink3,
                )
                if (kind == EntityKind.CHARACTER) {
                    for (which in HeaderFixed.entries) {
                        SwitchRow(
                            which.label,
                            FieldSettingsModel.showsInHeader(ready.doc, which),
                            { vm.showInHeader(which, it) },
                            subtitle = CastUiCopy.PLACE_HEADER,
                            enabled = editable,
                        )
                    }
                }
                SectionLabel(screen.projectTitle, Modifier.padding(top = 20.dp, bottom = 6.dp), trailing = screen.reorderNote, trailingAccent = false)
                ReorderableRows(
                    rows = screen.rows,
                    editable = editable,
                    onOpen = { deps.navigator.push(FieldDefEdit(route.projectKey, it)) },
                    onMove = { key, from, to -> vm.move(kind, key, from, to) },
                )
                if (editable) AddRow { sheets.open(AddFieldSheet(route.projectKey, kind)) }
                ListRow(
                    screen.bundleTitle,
                    Modifier.padding(top = 4.dp),
                    subtitle = screen.bundleSub,
                    compact = true,
                    trailing = { CivilightText(screen.bundleAction, CivilightTheme.type.small, color = CivilightTheme.colors.ink2, maxLines = 1) },
                    onClick = { sheets.open(BundleSheet(route.projectKey, kind)) },
                )
            }
        }
    }
}

/** Project field rows with a visible grip (`순서 바꾸기`): dragging the grip moves the row (CST-19). */
@Composable
private fun ReorderableRows(rows: List<FieldSettingRow>, editable: Boolean, onOpen: (Key) -> Unit, onMove: (Key, Int, Int) -> Unit) {
    var dragging by remember { mutableStateOf<Int?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    Column {
        rows.forEachIndexed { index, row ->
            val isDragged = dragging == index
            FieldSettingRowView(
                row = row,
                modifier = Modifier
                    .onSizeChanged { rowHeight = it.height.toFloat() }
                    .graphicsLayer { translationY = if (isDragged) offset else 0f },
                grip = if (!editable) Modifier else Modifier.pointerInput(row.key, index) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragging = index
                            offset = 0f
                        },
                        onDragEnd = {
                            val to = FieldSettingsModel.dropIndex(index, offset, rowHeight, rows.size)
                            dragging = null
                            offset = 0f
                            onMove(row.key, index, to)
                        },
                        onDragCancel = {
                            dragging = null
                            offset = 0f
                        },
                        onVerticalDrag = { change, delta ->
                            change.consume()
                            offset += delta
                        },
                    )
                },
                onClick = { onOpen(row.key) },
            )
        }
    }
}

/** `출생 · 날짜 · 연도 · 머리`: grip, name 15 bold, sub with the type in `ink2`, chevron. */
@Composable
private fun FieldSettingRowView(row: FieldSettingRow, modifier: Modifier, grip: Modifier, onClick: () -> Unit) {
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
                .pressable(onClick = onClick, scaleOnPress = false, onClickLabel = row.name),
        ) {
            CivilightText(row.name, type.itemTitleCompact, color = if (row.hidden) colors.ink3 else colors.ink, maxLines = 1)
            val sub = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.ink2)) { append(typePart) }
                append(rest)
            }
            androidx.compose.material3.Text(sub, style = type.small, color = colors.ink3, maxLines = 1)
        }
        CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3)
    }
}

/** `+ 항목` (plus icon, `항목` 600 14). */
@Composable
private fun AddRow(onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        Modifier
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
