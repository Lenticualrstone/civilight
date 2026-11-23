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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
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
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettingRow
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.feature.cast.templates.SaveTemplateSheet
import io.github.lenticualrstone.civilight.feature.cast.templates.SaveTemplateSheetContent
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateCopy
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

    /** CST-54 `앞으로`: one place up, one undo step `순서 바꾸기`. */
    fun forward(kind: EntityKind, defKey: Key) {
        act(CastUiCopy.REORDER) { doc, ctx -> FieldSettingsModel.forward(doc, kind, defKey, ctx) }
    }

    /** CST-54 `뒤로`: one place down, one undo step `순서 바꾸기`. */
    fun backward(kind: EntityKind, defKey: Key) {
        act(CastUiCopy.REORDER) { doc, ctx -> FieldSettingsModel.backward(doc, kind, defKey, ctx) }
    }

    fun showInHeader(which: HeaderFixed, show: Boolean) {
        act(which.label) { doc, ctx -> FieldSettingsModel.setShowInHeader(doc, which, show, ctx) }
    }
}

/**
 * `RCFields`: kind tabs, fixed fields, project fields with grip handles and the `순서 바꾸기` mode
 * (CST-54), `+ 항목`, the start bundle row and `템플릿으로 저장` (CST-57).
 */
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
                    is SaveTemplateSheet -> { { SaveTemplateSheetContent(key, ready.doc) } }
                    else -> null
                }
            },
        ) { padding ->
            FieldListBody(
                screen = screen,
                kindIndex = kindIndex,
                onKindIndex = { kindIndex = it },
                sectionTitle = screen.projectTitle,
                editable = editable,
                padding = padding,
                onMove = { key, from, to -> vm.move(kind, key, from, to) },
                onForward = { key -> vm.forward(kind, key) },
                onBackward = { key -> vm.backward(kind, key) },
                onRowClick = { key -> deps.navigator.push(FieldDefEdit(route.projectKey, key)) },
                onAdd = { sheets.open(AddFieldSheet(route.projectKey, kind)) },
                afterFixed = {
                    if (kind == EntityKind.CHARACTER) {
                        items(HeaderFixed.entries, key = { "switch-${it.name}" }) { which ->
                            SwitchRow(
                                which.label,
                                FieldSettingsModel.showsInHeader(ready.doc, which),
                                { vm.showInHeader(which, it) },
                                modifier = civilightItem(),
                                subtitle = CastUiCopy.PLACE_HEADER,
                                enabled = editable,
                            )
                        }
                    }
                },
                trailing = {
                    item(key = "bundle") {
                        ListRow(
                            screen.bundleTitle,
                            civilightItem().padding(top = 4.dp),
                            subtitle = screen.bundleSub,
                            compact = true,
                            trailing = { CivilightText(screen.bundleAction, CivilightTheme.type.small, color = CivilightTheme.colors.ink2, maxLines = 1) },
                            onClick = { sheets.open(BundleSheet(route.projectKey, kind)) },
                        )
                    }
                    // CST-57: hidden in the read-only example project.
                    if (editable) {
                        item(key = "save-template") {
                            ListRow(
                                TemplateCopy.SAVE_ROW,
                                civilightItem(),
                                subtitle = TemplateCopy.SAVE_SUB,
                                compact = true,
                                trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = CivilightTheme.colors.ink3) },
                                onClick = { sheets.open(SaveTemplateSheet(route.projectKey, kind)) },
                            )
                        }
                    }
                },
            )
        }
    }
}

/**
 * The live reorder of the project field rows (CST-19): while a grip is dragged the rows reorder in a local
 * list at once (the lazy list animates the neighbours aside), the dragged row stays under the finger, and on
 * release it settles into its slot before the move is written once. [order] is null while the list follows
 * the document; after a drop it stays until the document shows the same order (or a second passes).
 */
@Stable
internal class FieldReorder(
    private val keys: () -> List<Key>,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val list: LazyListState,
    private val motion: () -> io.github.lenticualrstone.civilight.designsystem.theme.Motion,
    private val onMove: (Key, Int, Int) -> Unit,
) {
    var order: List<Key>? by mutableStateOf(null)
    var dragKey: Key? by mutableStateOf(null)
    var offset: Float by mutableFloatStateOf(0f)
    var rowHeight: Float = 0f
    private var startIndex = 0
    private var job: kotlinx.coroutines.Job? = null

    fun start(key: Key) {
        job?.cancel()
        val now = order ?: keys()
        dragKey = key
        startIndex = now.indexOf(key)
        order = now
        offset = 0f
    }

    fun drag(delta: Float) {
        val key = dragKey ?: return
        val now = order ?: keys()
        val step = FieldSettingsModel.dragStep(now, key, offset + delta, rowHeight)
        // The lazy list keeps its first visible item by key; a swap of that very row must not scroll the list.
        if (step.order != now) list.requestScrollToItem(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
        order = step.order
        offset = step.offsetPx
    }

    fun release() {
        val key = dragKey ?: return
        job = scope.launch {
            androidx.compose.animation.core.animate(offset, 0f, animationSpec = motion().move(CivilightMotion.standard)) { value, _ -> offset = value }
            val to = order?.indexOf(key) ?: startIndex
            dragKey = null
            if (to != startIndex) {
                onMove(key, startIndex, to)
                // Keep the local order until the document shows it, so nothing flashes back.
                motion().delayMs(SETTLE_WAIT_MS)
            }
            order = null
        }
    }

    fun cancel() {
        job?.cancel()
        dragKey = null
        offset = 0f
        order = null
    }

    /** The document now shows the dropped order: follow it again. */
    fun documentCaughtUp() {
        if (dragKey == null && order != null && order == keys()) order = null
    }

    private companion object {
        const val SETTLE_WAIT_MS = 1000
    }
}

@Composable
internal fun rememberFieldReorder(documentKeys: List<Key>, list: LazyListState, onMove: (Key, Int, Int) -> Unit): FieldReorder {
    val scope = rememberCoroutineScope()
    // Read through states, so a reduce-motion change or a new document is honoured by a state that lives on.
    val motion by rememberUpdatedState(CivilightTheme.motion)
    val keys by rememberUpdatedState(documentKeys)
    val move by rememberUpdatedState(onMove)
    val state = remember(scope, list) { FieldReorder({ keys }, scope, list, { motion }) { k, f, t -> move(k, f, t) } }
    LaunchedEffect(documentKeys) { state.documentCaughtUp() }
    return state
}
